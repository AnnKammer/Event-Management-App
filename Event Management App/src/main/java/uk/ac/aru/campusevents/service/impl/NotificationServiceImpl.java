/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: NotificationServiceImpl.java
 * Purpose:
 *   Concrete implementation of in-app notifications.
 * Security & Design Notes:
 *   • Creates immutable Notification entities; repository assigns identifiers.
 *   • Does not allow arbitrary user mutation — only markAllRead() may alter state.
 *   • No PII or message metadata logged; messages remain short and generic.
 */
package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.Notification;
import uk.ac.aru.campusevents.repository.NotificationRepository;
import uk.ac.aru.campusevents.service.NotificationService;

import java.util.List;
import java.util.Objects;

/**
 * Default service for creating and managing user notifications.
 * Responsibilities:
 *   • Create new notifications (read flag initially false).
 *   • Return unread notifications for display.
 *   • Mark all user notifications as read in bulk.
 * All persistence details are delegated to {@link NotificationRepository}.
 */
@SuppressWarnings("unused")
public final class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repo;

    public NotificationServiceImpl(NotificationRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    /**
     * Sends a new notification to a specific user.
     *
     * @param recipientUserId target user ID
     * @param message         short, non-sensitive message text
     * @return the generated notification ID
     * @throws IllegalArgumentException if message is null or blank
     */
    @Override
    public int send(int recipientUserId, String message) {
        if (recipientUserId <= 0) throw new IllegalArgumentException("Invalid recipient user ID");
        if (message == null || message.isBlank())
            throw new IllegalArgumentException("Please write a message");
        return repo.create(Notification.unread(recipientUserId, message.trim()));
    }

    /**
     * Retrieves all unread notifications for the given user.
     *
     * @param recipientUserId target user ID
     * @return list of unread notifications (possibly empty)
     */
    @Override
    public List<Notification> listUnread(int recipientUserId) {
        return repo.listUnread(recipientUserId);
    }

    /**
     * Marks all notifications for the given user as read.
     *
     * @param recipientUserId target user ID
     */
    @Override
    public void markAllRead(int recipientUserId) {
        repo.markAllRead(recipientUserId);
    }
}

