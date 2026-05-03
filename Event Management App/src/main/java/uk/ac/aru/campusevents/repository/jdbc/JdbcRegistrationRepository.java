/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: JdbcRegistrationRepository.java
 * Purpose:
 *   JDBC-backed implementation of {@link RegistrationRepository} that persists and
 *   retrieves {@link Registration} entities from the PostgreSQL database.
 *
 * Design & Security Notes:
 *   • Uses the DB helper to run parameterised SQL (PreparedStatement) via JDBC.
 *   • Works with an immutable {@link Registration} model; updates are expressed as
 *     full replacements or targeted status changes.
 *   • Capacity-related queries only count confirmed registrations (REGISTERED/APPROVED),
 *     aligning with business rules enforced by services.
 *   • Assumes higher-level services enforce valid status transitions and RBAC.
 *
 * Uses the registration table:
 *   registration_id  SERIAL PRIMARY KEY
 *   event_id         INT NOT NULL REFERENCES event(event_id)
 *   student_id       INT NOT NULL REFERENCES app_user(user_id)
 *   status           reg_status NOT NULL
 *   registered_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
 */
package uk.ac.aru.campusevents.repository.jdbc;

import uk.ac.aru.campusevents.database.DB;
import uk.ac.aru.campusevents.domain.Registration;
import uk.ac.aru.campusevents.domain.enums.RegStatus;
import uk.ac.aru.campusevents.repository.RegistrationRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class JdbcRegistrationRepository implements RegistrationRepository {

    private final DB db;

    public JdbcRegistrationRepository(DB db) {
        this.db = Objects.requireNonNull(db);
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public int create(Registration r) {
        Object[] params = {
                r.getEventId(),               // 1
                r.getStudentId(),             // 2
                mapStatusToDb(r.getStatus()), // 3
                r.getRegisteredAt()           // 4
        };

        try (ResultSet rs = db.prepSQLQuery(
                "INSERT INTO registration (" +
                        "event_id, student_id, status, registered_at" +
                        ") VALUES (" +
                        "?, ?, ?::reg_status, ?" +
                        ") RETURNING registration_id",
                params,
                true)) {

            if (rs != null && rs.next()) {
                return rs.getInt("registration_id");
            } else {
                throw new IllegalStateException("Registration insert succeeded but no ID returned");
            }

        } catch (SQLException ex) {
            System.err.println("Error inserting registration: " + ex.getMessage());
            throw new RuntimeException("Failed to create registration", ex);
        }
    }

    // -------------------------------------------------------------------------
    // REPLACE (full row update by ID)
    // -------------------------------------------------------------------------

    @Override
    public void replace(Registration r) {
        if (r.getId() <= 0) {
            throw new IllegalArgumentException("Registration must have a valid ID to be replaced");
        }

        Object[] params = {
                r.getEventId(),               // 1
                r.getStudentId(),             // 2
                mapStatusToDb(r.getStatus()), // 3
                r.getRegisteredAt(),          // 4
                r.getId()                     // 5
        };

        boolean ok = db.prepSQLUpdate(
                "UPDATE registration SET " +
                        "event_id      = ?, " +
                        "student_id    = ?, " +
                        "status        = ?::reg_status, " +
                        "registered_at = ? " +
                        "WHERE registration_id = ?",
                params,
                true);

        if (!ok) {
            throw new RuntimeException("Failed to replace registration with id " + r.getId());
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE STATUS (by eventId + studentId)
    // -------------------------------------------------------------------------

    @Override
    public void updateStatus(int eventId, int studentId, RegStatus status) {
        Object[] params = {
                mapStatusToDb(status), // 1
                eventId,               // 2
                studentId              // 3
        };

        boolean ok = db.prepSQLUpdate(
                "UPDATE registration SET status = ?::reg_status " +
                        "WHERE event_id = ? AND student_id = ?",
                params,
                true);

        if (!ok) {
            System.err.println("Warning: status update may have failed for event " +
                    eventId + " / student " + studentId);
        }
    }

    // -------------------------------------------------------------------------
    // FIND ACTIVE (non-cancelled)
    // -------------------------------------------------------------------------

    @Override
    public Optional<Registration> findActive(int eventId, int studentId) {
        Object[] params = {
                eventId,
                studentId
        };

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT registration_id, event_id, student_id, status, registered_at " +
                        "FROM registration " +
                        "WHERE event_id = ? AND student_id = ? " +
                        "AND status <> 'CANCELLED'::reg_status " +
                        "ORDER BY registered_at DESC " +
                        "LIMIT 1",
                params,
                true)) {

            if (rs != null && rs.next()) {
                return Optional.of(mapRowToRegistration(rs));
            }

        } catch (SQLException ex) {
            System.err.println("Error querying active registration: " + ex.getMessage());
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // FIND CONFIRMED BY EVENT (REGISTERED / APPROVED)
    // -------------------------------------------------------------------------

    @Override
    public List<Registration> findByEvent(int eventId) {
        Object[] params = { eventId };

        List<Registration> result = new ArrayList<>();
        try (ResultSet rs = db.prepSQLQuery(
                "SELECT registration_id, event_id, student_id, status, registered_at " +
                        "FROM registration " +
                        "WHERE event_id = ? " +
                        "AND status IN ('REGISTERED'::reg_status,'APPROVED'::reg_status) " +
                        "ORDER BY registered_at ASC",
                params,
                true)) {

            if (rs == null) return List.of();
            while (rs.next()) {
                result.add(mapRowToRegistration(rs));
            }

        } catch (SQLException ex) {
            System.err.println("Error querying registrations by event: " + ex.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // FIND ALL BY EVENT (all statuses)
    // -------------------------------------------------------------------------

    @Override
    public List<Registration> findAllByEvent(int eventId) {
        Object[] params = { eventId };

        List<Registration> result = new ArrayList<>();
        try (ResultSet rs = db.prepSQLQuery(
                "SELECT registration_id, event_id, student_id, status, registered_at " +
                        "FROM registration " +
                        "WHERE event_id = ? " +
                        "ORDER BY registered_at ASC",
                params,
                true)) {

            if (rs == null) return List.of();
            while (rs.next()) {
                result.add(mapRowToRegistration(rs));
            }

        } catch (SQLException ex) {
            System.err.println("Error querying all registrations by event: " + ex.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // FIND BY STUDENT
    // -------------------------------------------------------------------------

    @Override
    public List<Registration> findByStudent(int studentId) {
        Object[] params = { studentId };

        List<Registration> result = new ArrayList<>();
        try (ResultSet rs = db.prepSQLQuery(
                "SELECT registration_id, event_id, student_id, status, registered_at " +
                        "FROM registration " +
                        "WHERE student_id = ? " +
                        "ORDER BY registered_at DESC",
                params,
                true)) {

            if (rs == null) return List.of();
            while (rs.next()) {
                result.add(mapRowToRegistration(rs));
            }

        } catch (SQLException ex) {
            System.err.println("Error querying registrations by student: " + ex.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // COUNT BY EVENT (capacity checks)
    // -------------------------------------------------------------------------

    @Override
    public long countByEvent(int eventId) {
        Object[] params = { eventId };

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT COUNT(*) FROM registration " +
                        "WHERE event_id = ? " +
                        "AND status IN ('REGISTERED'::reg_status,'APPROVED'::reg_status)",
                params,
                true)) {
            if (rs != null && rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException ex) {
            System.err.println("Error counting registrations by event: " + ex.getMessage());
        }
        return 0L;
    }

    // -------------------------------------------------------------------------
    // MAPPING HELPERS
    // -------------------------------------------------------------------------

    private Registration mapRowToRegistration(ResultSet rs) throws SQLException {
        int id           = rs.getInt("registration_id");
        int eventId      = rs.getInt("event_id");
        int studentId    = rs.getInt("student_id");
        String statusStr = rs.getString("status");
        LocalDateTime registeredAt =
                rs.getTimestamp("registered_at").toLocalDateTime();

        RegStatus status = mapStatusFromDb(statusStr);

        return new Registration(
                id,
                eventId,
                studentId,
                status,
                registeredAt
        );
    }

    private String mapStatusToDb(RegStatus status) {
        if (status == null) return "REGISTERED";
        return status.name();
    }

    private RegStatus mapStatusFromDb(String dbVal) {
        if (dbVal == null) return RegStatus.REGISTERED;
        return RegStatus.valueOf(dbVal);
    }
}
