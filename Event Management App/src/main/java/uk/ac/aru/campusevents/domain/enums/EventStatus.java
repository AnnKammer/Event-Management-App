/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: EventStatus.java
 * Purpose: Event lifecycle states used by services/GUI to gate valid actions.
 * Security notes: Services must restrict actions by status (e.g., no registration on ARCHIVED or DRAFT).
 * References: Element 010 brief — event creation/management, capacity update, student registration.
 */

package uk.ac.aru.campusevents.domain.enums;

/**
 * Lifecycle of an event from creation to archival. Used to validate
 * student/organizer actions and drive UI state (buttons, menus).
 */
public enum EventStatus {
    /** Organizer is editing; not visible for student registration. */
    DRAFT,
    /** Open for registration; capacity not yet reached. */
    OPEN,
    /** Capacity reached; new registrants must waitlist if enabled. */
    FULL,
    /** Occurrence has passed and attendance can be reviewed/rated. */
    COMPLETED,
    /** No longer active; hidden from regular listings and immutable. */
    ARCHIVED,
    /** No longer active; hidden from regular listings and immutable. */
    CANCELLED,
}

