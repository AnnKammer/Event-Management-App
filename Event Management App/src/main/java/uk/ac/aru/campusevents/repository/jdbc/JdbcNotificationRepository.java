/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: JdbcNotificationRepository.java
 * Purpose:
 *   JDBC-backed implementation of {@link NotificationRepository} that persists and
 *   retrieves {@link Notification} entities from the PostgreSQL database.
 *
 * Design & Security Notes:
 *   • Uses the DB helper to run parameterised SQL (PreparedStatement) via JDBC.
 *   • Stores only recipient id, short message text, timestamp, and read flag.
 *   • Does not log message bodies; all queries are scoped by recipient id.
 *   • Marking notifications read is done via a single UPDATE statement.
 *
 * Uses the notification table:
 *   notification_id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
 *   recipient_user_id INT NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE
 *   message           VARCHAR(300) NOT NULL
 *   created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
 *   is_read           BOOLEAN NOT NULL DEFAULT FALSE
 */
package uk.ac.aru.campusevents.repository.jdbc;

import uk.ac.aru.campusevents.database.DB;
import uk.ac.aru.campusevents.domain.Notification;
import uk.ac.aru.campusevents.repository.NotificationRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

public final class JdbcNotificationRepository implements NotificationRepository {

    private final DB db;

    public JdbcNotificationRepository(DB db) {
        this.db = Objects.requireNonNull(db);
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public int create(Notification n) {
        Objects.requireNonNull(n, "notification cannot be null");

        Object[] params = {
                n.getRecipientUserId(),                  // 1: recipient_user_id
                n.getMessage(),                          // 2: message
                Timestamp.valueOf(n.getCreatedAt()),     // 3: created_at
                n.isRead()                               // 4: is_read
        };

        try (ResultSet rs = db.prepSQLQuery(
                "INSERT INTO notification (" +
                        "recipient_user_id, message, created_at, is_read" +
                        ") VALUES (?,?,?,?) " +
                        "RETURNING notification_id",
                params,
                true)) {

            if (rs != null && rs.next()) {
                return rs.getInt("notification_id");
            } else {
                throw new IllegalStateException("Notification insert succeeded but no ID returned");
            }

        } catch (SQLException ex) {
            // 23503: foreign_key_violation (e.g. unknown recipient user)
            if ("23503".equals(ex.getSQLState())) {
                throw new IllegalArgumentException(
                        "Unknown recipient user id: " + n.getRecipientUserId(), ex);
            }
            System.err.println("Error inserting notification: " + ex.getMessage());
            throw new RuntimeException("Failed to create notification", ex);
        }
    }

    // -------------------------------------------------------------------------
    // LIST UNREAD
    // -------------------------------------------------------------------------

    @Override
    public List<Notification> listUnread(int recipientUserId) {
        Object[] params = { recipientUserId };
        List<Notification> result = new ArrayList<>();

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT notification_id, recipient_user_id, message, created_at, is_read " +
                        "FROM notification " +
                        "WHERE recipient_user_id = ? AND is_read = FALSE " +
                        "ORDER BY created_at ASC",
                params,
                true)) {

            if (rs == null) return List.of();
            while (rs.next()) {
                result.add(mapRow(rs));
            }

        } catch (SQLException ex) {
            System.err.println("Error querying unread notifications: " + ex.getMessage());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // MARK ALL READ
    // -------------------------------------------------------------------------

    @Override
    public void markAllRead(int recipientUserId) {
        Object[] params = { recipientUserId };

        boolean ok = db.prepSQLUpdate(
                "UPDATE notification " +
                        "SET is_read = TRUE " +
                        "WHERE recipient_user_id = ? AND is_read = FALSE",
                params,
                true);

        if (!ok) {
            // not fatal; just log – could be "no rows matched"
            System.err.println("Warning: markAllRead may have affected 0 rows for user " + recipientUserId);
        }
    }

    // -------------------------------------------------------------------------
    // ROW MAPPING
    // -------------------------------------------------------------------------

    private Notification mapRow(ResultSet rs) throws SQLException {
        int id               = rs.getInt("notification_id");
        int recipientUserId  = rs.getInt("recipient_user_id");
        String message       = rs.getString("message");
        Timestamp ts         = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts.toLocalDateTime();
        boolean isRead       = rs.getBoolean("is_read");

        return new Notification(
                id,
                recipientUserId,
                message,
                createdAt,
                isRead
        );
    }
}

