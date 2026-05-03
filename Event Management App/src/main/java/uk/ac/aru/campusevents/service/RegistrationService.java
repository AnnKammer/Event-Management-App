/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: RegistrationService.java
 * Purpose:
 *   Public use-cases for student registrations.
 *
 * Security & Design Notes:
 *   • All mutating methods accept actor IDs; implementations must enforce RBAC
 *     (STUDENT for self-registration/cancel).
 *   • Capacity is enforced in the service layer (using per-event capacity).
 *   • No persistence details are exposed; repositories remain behind this interface.
 *   • Status of the Event governs whether registration is allowed.
 */

package uk.ac.aru.campusevents.service;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.dto.RegistrationStats;

import java.util.List;

/**
 * Service boundary for managing event registrations.
 * Implementations should:
 *   • Validate that the event exists and is open for registration.
 *   • Enforce capacity and per-user limit.
 *   • Ensure actor role/ownership (student self-actions only).
 *   • Integrate with waitlist logic when capacity changes (e.g. on cancel).
 */
@SuppressWarnings("unused")
public interface RegistrationService {

    // ---------- Student-facing ----------

    /**
     * Registers the authenticated student for an event.
     * Behavior:
     *   • If capacity is available, the student is confirmed as an attendee.
     *   • If capacity is full, the student is placed on the waitlist (status WAITLISTED).
     *
     * @param actorStudentId the student performing the action (must match the registration subject)
     * @param eventId        the target event
     * @throws SecurityException        if the actor is not a STUDENT or not permitted
     * @throws IllegalArgumentException if inputs are invalid (bad IDs, closed event, per-user limit exceeded)
     * @throws IllegalStateException    if already registered
     */
    void register(int actorStudentId, int eventId);

    /**
     * Cancels the student's registration (or waitlist entry) for the given event.
     * Behavior:
     *   • Marks the existing active registration as CANCELLED.
     *   • If this cancellation frees up capacity, the implementation must automatically
     *     promote the oldest WAITLISTED registration for the event to APPROVED.
     *
     * @param actorStudentId the student performing the action
     * @param eventId        the event identifier
     * @throws SecurityException     if the actor is not permitted
     * @throws IllegalStateException if no active registration exists
     */
    void cancel(int actorStudentId, int eventId);


    // ---------- Read models ----------

    /**
     * Returns user IDs of confirmed attendees (REGISTERED/APPROVED) for the event.
     * Intended for attendee lists, exports, and check-in screens.
     *
     * @param eventId the event identifier
     * @return list of student user IDs (possibly empty)
     */
    List<Integer> listAttendeeUserIds(int eventId);

    /**
     * Lists events for which the given student currently holds an active registration
     * (e.g. REGISTERED, APPROVED, or WAITLISTED but not CANCELLED).
     *
     * @param studentId the student identifier
     * @return list of events (possibly empty)
     */
    List<Event> listMyEvents(int studentId);

    /**
     * Exports the student's active event registrations as a CSV-formatted string.
     * Intended for use by the Student Dashboard, which displays this CSV text
     * directly in the UI (e.g. in a TextArea) as a simple read model.
     *
     * @param studentId the student identifier
     * @return CSV string (possibly an empty string if no events)
     */
    String exportMyEventsCsv(int studentId);

    RegistrationStats getEventStats(int eventId);
}
