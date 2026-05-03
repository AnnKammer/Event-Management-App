/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: NotificationRepository.java
 * Purpose:
 *   Defines the persistence port for storing and retrieving in-app notifications.
 *   Provides methods for creation, querying unread messages, and marking them as read.
 * Security & Design Notes:
 *   • Stores no PII beyond recipient ID and short message text.
 *   • Avoids null returns; uses Optional and List for clarity.
 *   • Implementations must ensure atomic updates when marking notifications as read.
 *   • Safe for both in-memory and database-backed persistence layers.
 */
package uk.ac.aru.campusevents.repository;

import uk.ac.aru.campusevents.domain.Notification;

import java.util.List;

/**
 * Repository interface for managing {@link Notification} entities.
 * Enables services to deliver and query notifications for individual users.
 * Implementations may persist data in memory, files, or relational databases.
 */
@SuppressWarnings("unused")
public interface NotificationRepository {

    /**
     * Persists a new notification entry.
     *
     * @param n the notification to create (must contain a valid recipient ID)
     * @return the generated identifier
     */
    int create(Notification n);

    /**
     * Retrieves all unread notifications for the given user.
     *
     * @param recipientUserId the recipient’s user ID
     * @return a list of unread notifications (possibly empty)
     */
    List<Notification> listUnread(int recipientUserId);

    /**
     * Marks all notifications for a user as read.
     *
     * @param recipientUserId the user whose notifications should be updated
     */
    void markAllRead(int recipientUserId);
}


