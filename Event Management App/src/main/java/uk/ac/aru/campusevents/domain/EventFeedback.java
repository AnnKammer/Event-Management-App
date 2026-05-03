/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: EventFeedback.java
 * Purpose:
 *   Domain entity capturing student feedback (1–5 rating plus optional comment) for an event.
 * Security & Design Notes:
 *   • Immutable; repository assigns id on create.
 *   • Contains no personally identifiable information beyond userId.
 *   • UI layer should sanitize the comment before construction.
 *   • One feedback per (event,user) enforced in service logic.
 *   • Rating validation (1–5) and id sanity checks enforced in constructor.
 * Note:
 *   Some accessors may not yet be used directly by services but are retained for domain completeness.
 */
package uk.ac.aru.campusevents.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable value object representing feedback left by a student for a specific event.
 * Each feedback record holds a numeric rating (1–5) and an optional textual comment.
 */

@SuppressWarnings("unused")
public final class EventFeedback {

    /** Database identifier; 0 indicates unsaved/new feedback. */
    private final int id;

    /** Identifier of the event this feedback relates to. */
    private final int eventId;

    /** Identifier of the user who submitted the feedback. */
    private final int userId;

    /** Rating value, constrained to the range 1 – 5. */
    private final int rating;

    /** Optional free-text comment; sanitized by the UI layer. */
    private final String comment;

    /** Timestamp when the feedback was created. */
    private final LocalDateTime createdAt;

    /**
     * Constructs an immutable feedback record.
     *
     * @param id         database id (0 for new)
     * @param eventId    must be > 0
     * @param userId     must be > 0
     * @param rating     integer 1–5
     * @param comment    optional text; null converted → empty string
     * @param createdAt  creation timestamp; null → now
     * @throws IllegalArgumentException if ids or rating are invalid
     */
    public EventFeedback(int id, int eventId, int userId, int rating,
                         String comment, LocalDateTime createdAt) {
        if (eventId <= 0 || userId <= 0)
            throw new IllegalArgumentException("ids must be > 0");
        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("rating must be 1..5");

        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment == null ? "" : comment.trim();
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    /** Factory for creating new feedback with the current timestamp. */
    public static EventFeedback newFeedback(int eventId, int userId, int rating, String comment) {
        return new EventFeedback(0, eventId, userId, rating, comment, LocalDateTime.now());
    }

    /* ---------- Getters ---------- */

    public int getId() { return id; }
    public int getEventId() { return eventId; }
    public int getUserId() { return userId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /* ---------- Equality & Hashing ---------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventFeedback other)) return false;

        // If both have IDs, compare only by ID
        if (this.id != 0 && other.id != 0) {
            return this.id == other.id;
        }

        // Otherwise compare structural fields
        return eventId == other.eventId &&
                userId == other.userId &&
                rating == other.rating &&
                Objects.equals(comment, other.comment) &&
                Objects.equals(createdAt, other.createdAt);
    }

    @Override
    public int hashCode() {
        if (id != 0) return Integer.hashCode(id);
        return Objects.hash(eventId, userId, rating, comment, createdAt);
    }

    /* ---------- Debug ---------- */

    @Override
    public String toString() {
        return "EventFeedback{" +
                "id=" + id +
                ", eventId=" + eventId +
                ", userId=" + userId +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}


