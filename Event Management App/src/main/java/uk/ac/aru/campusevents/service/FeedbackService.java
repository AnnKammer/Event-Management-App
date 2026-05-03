/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: FeedbackService.java
 * Purpose:
 *   Defines use-cases for student feedback submission and event rating retrieval.
 * Security & Design Notes:
 *   • Accepts actorStudentId to enforce STUDENT role and participation checks.
 *   • One feedback per (event, user) enforced in service layer.
 *   • No PII beyond numeric identifiers and optional comments.
 *   • Aggregations (e.g., averageRating) return only anonymized values.
 */
package uk.ac.aru.campusevents.service;

import uk.ac.aru.campusevents.domain.EventFeedback;

import java.util.List;
import java.util.Optional;

/**
 * Service boundary for managing {@link EventFeedback}.
 * Implementations handle:
 *   • Validation (rating range 1–5, event existence, participation check).
 *   • Enforcing one feedback per (event, student) rule.
 *   • Delegating persistence to {@code FeedbackRepository}.
 *   • Providing aggregate metrics (e.g., average rating).
 */
@SuppressWarnings("unused")
public interface FeedbackService {

    /**
     * Submits a feedback entry from a student for a given event.
     *
     * @param actorStudentId the student submitting feedback (must be registered attendee)
     * @param eventId        the event identifier
     * @param rating         the rating value (1–5 inclusive)
     * @param comment        optional comment (may be null or blank)
     * @return the generated feedback identifier
     * @throws SecurityException        if the actor is not permitted (e.g., not registered)
     * @throws IllegalArgumentException if validation fails (invalid rating, etc.)
     */
    int submit(int actorStudentId, int eventId, int rating, String comment);

    /**
     * Returns true if this student has already submitted feedback for this event.
     * Used by the UI to disable/grey out the feedback controls.
     *
     * @param eventId   the event identifier
     * @param studentId the student identifier
     * @return true if a feedback row exists for (eventId, studentId)
     */
    boolean hasFeedback(int eventId, int studentId);

    /**
     * Calculates the average rating for a specific event.
     *
     * @param eventId the event identifier
     * @return an {@link Optional} containing the average rating (1–5),
     *         or empty if no feedback has been submitted
     */
    Optional<Double> averageRating(int eventId);

    /**
     * Retrieves all feedback entries associated with an event.
     *
     * @param eventId the event identifier
     * @return a list of feedback objects (possibly empty), sorted by creation time
     */
    List<EventFeedback> listForEvent(int eventId);
}
