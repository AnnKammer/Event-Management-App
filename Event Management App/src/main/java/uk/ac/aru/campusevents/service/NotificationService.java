/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: NotificationService.java
 * Purpose:
 *   Defines application-facing use-cases for creating and managing user notifications.
 * Security & Design Notes:
 *   • No PII beyond user identifiers; messages should remain short and generic.
 *   • Read/unread tracking is maintained server-side — clients must not modify directly.
 *   • Notifications are immutable after creation; only the read-flag can change.
 *   • Safe for use with in-memory or database-backed repositories.
 */
package uk.ac.aru.campusevents.service;

import uk.ac.aru.campusevents.domain.Notification;

import java.util.List;

/**
 * Service boundary for managing {@link Notification} entities.
 * Implementations handle:
 *   • Creating new in-app notifications for users (via repository).
 *   • Retrieving unread notifications for a recipient.
 *   • Marking notifications as read in bulk (per user).
 * Notifications are intended to be brief, non-sensitive system messages.
 */
@SuppressWarnings("unused")
public interface NotificationService {

    /**
     * Sends (creates) a new notification for the specified recipient.
     *
     * @param recipientUserId the target user's identifier
     * @param message         short, non-sensitive message text
     * @return the generated notification identifier
     * @throws IllegalArgumentException if message is null or blank
     */
    int send(int recipientUserId, String message);

    /**
     * Retrieves all unread notifications for a given user.
     * Typically used to populate in-app notification panels or dashboards.
     *
     * @param recipientUserId the target user's identifier
     * @return a list of unread notifications (possibly empty)
     */
    List<Notification> listUnread(int recipientUserId);

    /**
     * Marks all notifications for a given user as read.
     * This updates the read flag on all corresponding notifications.
     *
     * @param recipientUserId the target user's identifier
     */
    void markAllRead(int recipientUserId);
}


