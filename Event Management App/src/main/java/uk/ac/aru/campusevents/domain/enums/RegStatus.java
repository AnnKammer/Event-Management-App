/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: RegStatus.java
 * Purpose:
 *   Registration lifecycle states for student–event relationships.
 *   Supports capacity enforcement, waitlisting, and automatic promotion.
 *
 * Notes:
 *   • All status transitions are managed by RegistrationService.
 *   • No manual organizer approval exists in the current workflow.
 *   • APPROVED is reserved exclusively for automatic waitlist promotion.
 */

package uk.ac.aru.campusevents.domain.enums;

/**
 * Lifecycle of a student's registration for an event.
 * Drives capacity accounting, waitlist behaviour, and notifications.
 */
public enum RegStatus {

    /**
     * The student holds a confirmed seat that counts toward event capacity.
     * Assigned immediately when capacity is available at registration time.
     */
    REGISTERED,

    /**
     * The student cancelled their registration (or was removed).
     * CANCELLED entries do NOT count toward capacity.
     */
    CANCELLED,

    /**
     * The event is full; the student is queued for possible promotion.
     * Ordered by registration timestamp (oldest promoted first).
     */
    WAITLISTED,

    /**
     * The student was automatically promoted from the waitlist
     * when capacity freed up (e.g., another attendee cancelled).
     *
     * This state is set ONLY by system logic — no manual approval workflow.
     */
    APPROVED
}
