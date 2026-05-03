/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: InMemoryNotificationRepository.java
 * Purpose:
 *   In-memory implementation of the NotificationRepository interface.
 *   Provides creation and read-marking of Notification entities for
 *   development and testing without database connectivity.
 * Security & Design Notes:
 *   • Thread-safe: ConcurrentHashMap + AtomicInteger.
 *   • Identity immutability: repository reconstructs instances on create/markRead.
 *   • No message content logging; all retrievals scoped by recipient user ID.
 *   • Suitable for demonstration/testing only — no persistence beyond runtime.
 */
package uk.ac.aru.campusevents.repository.memory;

import uk.ac.aru.campusevents.domain.Notification;
import uk.ac.aru.campusevents.repository.NotificationRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory adapter for {@link NotificationRepository}.
 * Stores notifications keyed by numeric ID and supports
 * creating, listing unread items, and marking all as read.
 */
@SuppressWarnings("unused")
public final class InMemoryNotificationRepository implements NotificationRepository {

    /** Sequence generator for notification IDs. */
    private final AtomicInteger seq = new AtomicInteger(1);

    /** Thread-safe map of id → Notification. */
    private final Map<Integer, Notification> byId = new ConcurrentHashMap<>();

    /**
     * Persists a new notification instance.
     *
     * @param n the notification to create (must contain valid recipient ID)
     * @return the generated identifier
     */
    @Override
    public int create(Notification n) {
        Objects.requireNonNull(n, "notification cannot be empty");
        int id = seq.getAndIncrement();
        Notification withId = new Notification(
                id,
                n.getRecipientUserId(),
                n.getMessage(),
                n.getCreatedAt(),
                n.isRead()
        );
        byId.put(id, withId);
        return id;
    }

    /**
     * Retrieves all unread notifications for the given recipient,
     * sorted by creation time (oldest first).
     *
     * @param recipientUserId the user identifier
     * @return list of unread notifications (possibly empty)
     */
    @Override
    public List<Notification> listUnread(int recipientUserId) {
        return byId.values().stream()
                .filter(n -> n.getRecipientUserId() == recipientUserId && !n.isRead())
                .sorted(Comparator.comparing(Notification::getCreatedAt))
                .toList();
    }

    /**
     * Marks all notifications for the specified user as read.
     * Replaces each matching instance with a new {@link Notification}
     * object whose {@code read} flag is true.
     *
     * @param recipientUserId the user whose notifications should be marked read
     */
    @Override
    public void markAllRead(int recipientUserId) {
        byId.replaceAll((id, old) ->
                (old.getRecipientUserId() == recipientUserId && !old.isRead())
                        ? old.asRead()
                        : old
        );
    }
}
