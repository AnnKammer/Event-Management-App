/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: Notification.java
 * Purpose:
 *   Domain entity for in-app notifications delivered to a single recipient.
 * Security & Design Notes:
 *   • Immutable to callers; repository assigns id on create.
 *   • Contains no PII beyond recipient id and short message text.
 *   • Timestamp captured once at creation.
 *   • Read flag can be toggled only through a controlled domain transformation.
 * Note:
 *   Some accessors may not yet be used directly by services but are retained for domain completeness.
 */
package uk.ac.aru.campusevents.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable value object representing an in-app notification to one user.
 * Notifications carry a short, non-PII message and a timestamp.
 * The {@link #read} flag indicates whether the user has seen the message.
 * To mark a notification as read, use {@link #asRead()}, which returns a new instance.
 */

@SuppressWarnings("unused")
public final class Notification {

    /** Database identifier; 0 indicates unsaved/new notification. */
    private final int id;

    /** Target user who receives this notification. */
    private final int recipientUserId;

    /** Message text (non-PII). */
    private final String message;

    /** Timestamp when the notification was created. */
    private final LocalDateTime createdAt;

    /** Read/unread flag. */
    private final boolean read;

    /**
     * Constructs a notification instance.
     *
     * @param id               database id (0 for new)
     * @param recipientUserId  must be > 0
     * @param message          non-null short text
     * @param createdAt        creation timestamp; null → now
     * @param read             initial read/unread state
     * @throws IllegalArgumentException if recipientUserId ≤ 0 or message is null
     */
    public Notification(int id, int recipientUserId, String message,
                        LocalDateTime createdAt, boolean read) {
        if (recipientUserId <= 0)
            throw new IllegalArgumentException("recipientUserId must be > 0");
        this.id = id;
        this.recipientUserId = recipientUserId;
        this.message = Objects.requireNonNull(message, "message must not be null").trim();
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.read = read;
    }

    /** Factory method for a new unread notification. */
    public static Notification unread(int recipientUserId, String message) {
        return new Notification(0, recipientUserId, message, LocalDateTime.now(), false);
    }

    /* ---------- Getters ---------- */

    public int getId() { return id; }
    public int getRecipientUserId() { return recipientUserId; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }

    /* ---------- Domain Transformation ---------- */

    /**
     * Returns a copy of this notification marked as read.
     * The original instance remains unchanged (immutability preserved).
     */
    public Notification asRead() {
        return new Notification(id, recipientUserId, message, createdAt, true);
    }

    /* ---------- Equality & Hashing ---------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification other)) return false;

        // Compare by ID if assigned
        if (this.id != 0 && other.id != 0) {
            return this.id == other.id;
        }

        // Otherwise compare structurally
        return recipientUserId == other.recipientUserId
                && Objects.equals(message, other.message)
                && Objects.equals(createdAt, other.createdAt)
                && read == other.read;
    }

    @Override
    public int hashCode() {
        if (id != 0) return Integer.hashCode(id);
        return Objects.hash(recipientUserId, message, createdAt, read);
    }

    /* ---------- Debug ---------- */

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", recipientUserId=" + recipientUserId +
                ", message='" + message + '\'' +
                ", createdAt=" + createdAt +
                ", read=" + read +
                '}';
    }
}

