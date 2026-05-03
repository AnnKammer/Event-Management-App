package uk.ac.aru.campusevents.repository.jdbc;

import uk.ac.aru.campusevents.database.DB;
import uk.ac.aru.campusevents.domain.Organization;
import uk.ac.aru.campusevents.repository.OrganizationRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class JdbcOrganizationRepository implements OrganizationRepository {

    private final DB db;

    public JdbcOrganizationRepository(DB db) {
        this.db = Objects.requireNonNull(db);
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public int create(Organization org) {
        Objects.requireNonNull(org, "organization cannot be null");

        Object[] params = {
                org.getName(),
                org.getType(),
                ""  // notes is empty by default
        };

        try (ResultSet rs = db.prepSQLQuery(
                "INSERT INTO organization (name, type, notes) " +
                        "VALUES (?,?,?) " +
                        "RETURNING organization_id",
                params,
                true)) {

            if (rs != null && rs.next()) {
                return rs.getInt("organization_id");
            } else {
                throw new IllegalStateException("Insert succeeded but no ID returned.");
            }

        } catch (SQLException ex) {
            System.err.println("Error inserting organization: " + ex.getMessage());
            throw new RuntimeException("Failed to create organization", ex);
        }
    }

    // -------------------------------------------------------------------------
    // FIND BY ID
    // -------------------------------------------------------------------------

    @Override
    public Optional<Organization> findById(int id) {
        Object[] params = { id };

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT organization_id, name, type, notes " +
                        "FROM organization WHERE organization_id = ?",
                params,
                true)) {

            if (rs != null && rs.next()) {
                return Optional.of(mapRow(rs));
            }

        } catch (SQLException ex) {
            System.err.println("Error fetching organization by id: " + ex.getMessage());
        }

        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // FIND BY NAME
    // -------------------------------------------------------------------------

    @Override
    public Optional<Organization> findByName(String name) {
        if (name == null) return Optional.empty();

        Object[] params = { name };

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT organization_id, name, type, notes " +
                        "FROM organization WHERE name = ?",
                params,
                true)) {

            if (rs != null && rs.next()) {
                return Optional.of(mapRow(rs));
            }

        } catch (SQLException ex) {
            System.err.println("Error fetching organization by name: " + ex.getMessage());
        }

        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // FIND ALL
    // -------------------------------------------------------------------------

    @Override
    public List<Organization> findAll() {
        List<Organization> result = new ArrayList<>();

        try (ResultSet rs = db.runSQLQuery(
                "SELECT organization_id, name, type, notes " +
                        "FROM organization ORDER BY LOWER(name)",
                true)) {

            if (rs == null) return List.of();
            while (rs.next()) result.add(mapRow(rs));

        } catch (SQLException ex) {
            System.err.println("Error fetching all organizations: " + ex.getMessage());
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Helper: map row to domain object
    // -------------------------------------------------------------------------

    private Organization mapRow(ResultSet rs) throws SQLException {
        return new Organization(
                rs.getInt("organization_id"),
                rs.getString("name"),
                rs.getString("type")
        );
    }
}
