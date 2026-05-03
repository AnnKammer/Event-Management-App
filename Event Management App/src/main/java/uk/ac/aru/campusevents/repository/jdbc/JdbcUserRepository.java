/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: JdbcUserRepository.java
 * Purpose:
 *   JDBC-backed implementation of {@link UserRepository} that persists and
 *   retrieves {@link User} entities from the PostgreSQL database.
 *
 * Design & Security Notes:
 *   • Uses the DB helper to run parameterised SQL (PreparedStatement) via JDBC.
 *   • Reads from the app_user and user_role tables to fully hydrate User objects
 *     (id, name, email, password hash, roles, createdAt).
 *   • Assumes password hashing is handled by the service layer (e.g. AuthService),
 *     so this repository never deals with plaintext passwords directly.
 *   • Safe to swap with InMemoryUserRepository without changing service code,
 *     preserving the hexagonal / ports-and-adapters architecture.
 *
 * Uses the app_user table:
 *   user_id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
 *   first_name    VARCHAR(100) NOT NULL
 *   last_name     VARCHAR(100) NOT NULL
 *   email         VARCHAR(254) NOT NULL UNIQUE
 *   password_hash TEXT NOT NULL
 *   created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
 */

package uk.ac.aru.campusevents.repository.jdbc;

import uk.ac.aru.campusevents.database.DB;
import uk.ac.aru.campusevents.domain.User;
import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.repository.UserRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JDBC adapter for user persistence.
 * Bridges the domain {@link UserRepository} interface to the PostgreSQL schema.
 */
public class JdbcUserRepository implements UserRepository {

    private final DB db;

    public JdbcUserRepository(DB db) {
        this.db = Objects.requireNonNull(db);
    }

    // -------------------------------------------------------------------------
    // CREATE USER
    // -------------------------------------------------------------------------

    @Override
    public int create(User user) {
        Objects.requireNonNull(user, "user cannot be null");

        Object[] params = {
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPasswordHash()
        };

        int id;

        // Insert user and get generated ID directly
        try (ResultSet rs = db.prepSQLQuery(
                "INSERT INTO app_user (first_name, last_name, email, password_hash) " +
                        "VALUES (?,?,?,?) " +
                        "RETURNING user_id",
                params,
                true)) {

            if (rs != null && rs.next()) {
                id = rs.getInt("user_id");
            } else {
                throw new IllegalStateException("User insert succeeded but no ID returned");
            }

        } catch (SQLException e) {
            // 23505 = unique_violation (e.g. duplicate email)
            if ("23505".equals(e.getSQLState())) {
                throw new IllegalStateException(
                        "A user with email '" + user.getEmail() + "' already exists.", e);
            }
            System.err.println("Error inserting user: " + e.getMessage());
            throw new RuntimeException("Failed to create user", e);
        }

        // Insert user roles
        for (Role r : user.getRoles()) {
            boolean ok = db.prepSQLUpdate(
                    "INSERT INTO user_role (user_id, role) VALUES (?, ?::role)",
                    new Object[]{id, r.name()},
                    true
            );
            if (!ok) {
                System.err.println("Warning: failed to insert role " + r + " for user " + id);
            }
        }

        return id;
    }

    // -------------------------------------------------------------------------
    // FIND BY ID
    // -------------------------------------------------------------------------

    @Override
    public Optional<User> findById(int id) {
        if (id <= 0) return Optional.empty();

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT user_id, first_name, last_name, email, password_hash, created_at " +
                        "FROM app_user WHERE user_id = ?",
                new Object[]{id},
                true)) {

            if (rs != null && rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error querying user by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // FIND BY EMAIL
    // -------------------------------------------------------------------------

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT user_id, first_name, last_name, email, password_hash, created_at " +
                        "FROM app_user WHERE email = ?",
                new Object[]{email},
                true)) {

            if (rs != null && rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error querying user by email: " + e.getMessage());
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // FIND ALL BY IDS
    // -------------------------------------------------------------------------

    @Override
    public List<User> findAllByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        List<Integer> idList = new ArrayList<>(ids);

        String placeholders = String.join(",", Collections.nCopies(idList.size(), "?"));
        String sql =
                "SELECT user_id, first_name, last_name, email, password_hash, created_at " +
                        "FROM app_user WHERE user_id IN (" + placeholders + ")";

        Object[] params = idList.toArray();

        List<User> result = new ArrayList<>();
        try (ResultSet rs = db.prepSQLQuery(sql, params, true)) {
            if (rs == null) return List.of();
            while (rs.next()) {
                result.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error querying users by ids: " + e.getMessage());
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // MAPPING: SQL row → User domain object
    // -------------------------------------------------------------------------

    private User mapRowToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String first = rs.getString("first_name");
        String last = rs.getString("last_name");
        String email = rs.getString("email");
        String hash = rs.getString("password_hash");
        LocalDateTime createdAt =
                rs.getTimestamp("created_at").toLocalDateTime();

        // Load roles for this user
        Set<Role> roles = loadRolesFor(id);

        return new User(
                id,
                first,
                last,
                email,
                hash,
                roles,
                createdAt
        );
    }

    private Set<Role> loadRolesFor(int userId) {
        Set<Role> roles = new HashSet<>();

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT role FROM user_role WHERE user_id = ?",
                new Object[]{userId},
                true)) {

            if (rs == null) return roles;

            while (rs.next()) {
                String roleString = rs.getString("role");
                if (roleString != null) {
                    roles.add(Role.valueOf(roleString.toUpperCase()));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error loading user roles: " + e.getMessage());
        }
        return roles;
    }
}
