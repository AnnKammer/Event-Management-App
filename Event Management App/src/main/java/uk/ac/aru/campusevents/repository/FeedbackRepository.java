/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: FeedbackRepository.java
 * Purpose:
 *   Defines the persistence port for storing and retrieving student feedback on events.
 *   Provides methods for creation, lookup, and event-based retrieval.
 * Security & Design Notes:
 *   • Only exposes sanitized {@link EventFeedback} domain objects.
 *   • No raw SQL defined here — JDBC adapter will use prepared statements.
 *   • Services enforce one-feedback-per-(event,user) rule and handle RBAC.
 *   • Safe to implement using in-memory or database-backed storage.
 */
package uk.ac.aru.campusevents.repository;

import uk.ac.aru.campusevents.domain.EventFeedback;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link EventFeedback} entities.
 * Enables retrieval of feedback records per event and per user,
 * supporting both reporting and student review features.
 */
@SuppressWarnings("unused")
public interface FeedbackRepository {

    /**
     * Persists a new feedback entry.
     *
     * @param f the feedback object to create (must satisfy domain validation)
     * @return the generated database identifier
     */
    int create(EventFeedback f);

    /**
     * Finds a specific feedback entry by event and user identifiers.
     *
     * @param eventId the event being reviewed
     * @param userId  the student who left the feedback
     * @return an {@link Optional} containing the feedback if found, or empty otherwise
     */
    Optional<EventFeedback> findByEventAndUser(int eventId, int userId);

    /**
     * Retrieves all feedback entries associated with a particular event.
     *
     * @param eventId the event identifier
     * @return a list of feedback objects (possibly empty)
     */
    List<EventFeedback> findByEvent(int eventId);
}


