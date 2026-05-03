/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: Registration.java
 * Purpose:
 *   Domain entity representing a student’s place for a specific event.
 *
 * Security & Design Notes:
 *   • All fields are private and final; no public mutators for identity.
 *   • Status transitions are controlled by the service layer (RBAC + rules).
 *   • Immutable to callers after construction; repositories reconstruct with
 *     assigned ids when persisting.
 *   • Timestamp is captured once at creation.
 *
 * Design Note:
 *   Ticket types are not used in the current system; each event has a single
 *   capacity pool. The model can be extended in future if needed.
 */
package uk.ac.aru.campusevents.domain;

import uk.ac.aru.campusevents.domain.enums.RegStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable value object representing a student's registration for a given event.
 * Status transitions (REGISTERED, WAITLISTED, APPROVED, CANCELLED) are governed
 * by business rules and service-layer authorization.
 */
@SuppressWarnings("unused")
public final class Registration {

    /** Database identifier; 0 indicates unsaved/new registration. */
    private final int id;

    /** Identifier of the event the student registered for. */
    private final int eventId;

    /** Identifier of the student user who registered. */
    private final int studentId;

    /** Time the registration was created. */
    private final LocalDateTime registeredAt;

    /** Registration lifecycle status. */
    private final RegStatus status;

    /**
     * Constructs an immutable registration record.
     *
     * @param id           database id (0 for new)
     * @param eventId      must be > 0
     * @param studentId    must be > 0
     * @param status       required non-null registration status
     * @param registeredAt creation timestamp; null → now
     * @throws IllegalArgumentException if ids are invalid
     * @throws NullPointerException     if status is null
     */
    public Registration(int id,
                        int eventId,
                        int studentId,
                        RegStatus status,
                        LocalDateTime registeredAt) {

        if (eventId <= 0) {
            throw new IllegalArgumentException("Event Id must be greater than 0");
        }
        if (studentId <= 0) {
            throw new IllegalArgumentException("Student Id must be greater than 0");
        }

        this.id = id;
        this.eventId = eventId;
        this.studentId = studentId;
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.registeredAt = (registeredAt == null ? LocalDateTime.now() : registeredAt);
    }

    /** Factory for creating a new registration with timestamp = now. */
    public static Registration newRegistration(int eventId,
                                               int studentId,
                                               RegStatus status) {
        return new Registration(0, eventId, studentId, status, LocalDateTime.now());
    }

    /* ---------- Getters ---------- */

    public int getId() {
        return id;
    }

    public int getEventId() {
        return eventId;
    }

    public int getStudentId() {
        return studentId;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public RegStatus getStatus() {
        return status;
    }

    /* ---------- Equality & Hashing ---------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Registration other)) return false;

        // Compare by ID if assigned
        if (this.id != 0 && other.id != 0) {
            return this.id == other.id;
        }

        // Otherwise compare core identifying fields
        return eventId == other.eventId
                && studentId == other.studentId
                && status == other.status
                && Objects.equals(registeredAt, other.registeredAt);
    }

    @Override
    public int hashCode() {
        if (id != 0) return Integer.hashCode(id);
        return Objects.hash(eventId, studentId, status, registeredAt);
    }

    /* ---------- Debug ---------- */

    @Override
    public String toString() {
        return "Registration{" +
                "id=" + id +
                ", eventId=" + eventId +
                ", studentId=" + studentId +
                ", status=" + status +
                ", registeredAt=" + registeredAt +
                '}';
    }
}
