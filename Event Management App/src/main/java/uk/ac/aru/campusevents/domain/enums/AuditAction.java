/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AuditAction.java
 * Purpose:
 *   Canonical list of auditable actions written to the audit log.
 *
 * Security notes:
 *   • Enumerated values prevent arbitrary/unexpected action names.
 *   • Designed for stable reporting; extend cautiously.
 *
 * Notes:
 *   • Manual organizer approval is not part of the current workflow.
 *   • PROMOTE is used exclusively for automatic waitlist promotion
 *     (WAITLISTED → APPROVED).
 */
package uk.ac.aru.campusevents.domain.enums;

public enum AuditAction {

    // ----- Lifecycle / CRUD -----
    CREATE,
    UPDATE,
    DELETE,

    // ----- Registration workflow -----
    /** Student registered (REGISTERED or WAITLISTED depending on capacity). */
    REGISTER,

    /** Student cancelled their registration (capacity freed). */
    CANCEL,

    /**
     * System automatically promoted a WAITLISTED student to APPROVED.
     * (No manual approval exists; this is fully automated.)
     */
    PROMOTE,

    // ----- Feedback -----
    FEEDBACK_SUBMIT,

    // ----- Messaging / exports / authentication -----
    NOTIFY,
    EXPORT,
    LOGIN_OK,
    LOGIN_FAIL
}
