/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: JdbcFeedbackRepository.java
 * Purpose:
 *   JDBC-backed implementation of {@link FeedbackRepository} that persists and
 *   retrieves {@link EventFeedback} entities from the PostgreSQL database.
 *
 * Design & Security Notes:
 *   • Uses the DB helper to run parameterised SQL (PreparedStatement) via JDBC.
 *   • One feedback per (event_id, student_id) is enforced by the database
 *     (UNIQUE constraint) and surfaced to callers as an IllegalStateException.
 *   • No PII beyond student_id and free-text comment; comment should already be
 *     sanitized/validated in the UI/service layer.
 *   • Timestamps use TIMESTAMPTZ in PostgreSQL and {@link java.time.LocalDateTime}
 *     in the domain model.
 *
 * Uses the feedback table:
 *   feedback_id  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
 *   event_id     INT NOT NULL REFERENCES event(event_id) ON DELETE CASCADE
 *   student_id   INT NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE
 *   rating       SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5)
 *   comment      TEXT NOT NULL DEFAULT ''
 *   created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
 *   UNIQUE (event_id, student_id)
 */
package uk.ac.aru.campusevents.repository.jdbc;

import uk.ac.aru.campusevents.database.DB;
import uk.ac.aru.campusevents.domain.EventFeedback;
import uk.ac.aru.campusevents.repository.FeedbackRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

public final class JdbcFeedbackRepository implements FeedbackRepository {

    private final DB db;

    public JdbcFeedbackRepository(DB db) {
        this.db = Objects.requireNonNull(db);
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public int create(EventFeedback f) {
        Objects.requireNonNull(f, "feedback cannot be null");

        Object[] params = {
                f.getEventId(),                        // 1: event_id
                f.getUserId(),                         // 2: student_id
                f.getRating(),                         // 3: rating
                f.getComment(),                        // 4: comment
                Timestamp.valueOf(f.getCreatedAt())    // 5: created_at
        };

        try (ResultSet rs = db.prepSQLQuery(
                "INSERT INTO feedback (" +
                        "event_id, student_id, rating, comment, created_at" +
                        ") VALUES (?,?,?,?,?) " +
                        "RETURNING feedback_id",
                params,
                true)) {

            if (rs != null && rs.next()) {
                return rs.getInt("feedback_id");
            } else {
                throw new IllegalStateException("Feedback insert succeeded but no ID returned");
            }

        } catch (SQLException ex) {
            // 23505 = unique_violation (e.g. event_id + student_id already exist)
            if ("23505".equals(ex.getSQLState())) {
                throw new IllegalStateException(
                        "Feedback already exists for eventId=" + f.getEventId() +
                                ", userId=" + f.getUserId(), ex);
            }
            System.err.println("Error inserting feedback: " + ex.getMessage());
            throw new RuntimeException("Failed to create feedback", ex);
        }
    }

    // -------------------------------------------------------------------------
    // FIND BY (EVENT, USER)
    // -------------------------------------------------------------------------

    @Override
    public Optional<EventFeedback> findByEventAndUser(int eventId, int userId) {
        Object[] params = { eventId, userId };

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT feedback_id, event_id, student_id, rating, comment, created_at " +
                        "FROM feedback " +
                        "WHERE event_id = ? AND student_id = ?",
                params,
                true)) {

            if (rs != null && rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error querying feedback by event/user: " + ex.getMessage());
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // FIND BY EVENT (all feedback rows)
    // -------------------------------------------------------------------------

    @Override
    public List<EventFeedback> findByEvent(int eventId) {
        Object[] params = { eventId };
        List<EventFeedback> result = new ArrayList<>();

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT feedback_id, event_id, student_id, rating, comment, created_at " +
                        "FROM feedback " +
                        "WHERE event_id = ? " +
                        "ORDER BY created_at ASC, student_id ASC",
                params,
                true)) {

            if (rs == null) return List.of();
            while (rs.next()) {
                result.add(mapRow(rs));
            }

        } catch (SQLException ex) {
            System.err.println("Error querying feedback by event: " + ex.getMessage());
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // ROW MAPPING
    // -------------------------------------------------------------------------

    private EventFeedback mapRow(ResultSet rs) throws SQLException {
        int id        = rs.getInt("feedback_id");
        int eventId   = rs.getInt("event_id");
        int userId    = rs.getInt("student_id");
        int rating    = rs.getInt("rating");
        String comment = rs.getString("comment");
        Timestamp ts   = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts.toLocalDateTime();

        return new EventFeedback(
                id,
                eventId,
                userId,
                rating,
                comment,
                createdAt
        );
    }
}

