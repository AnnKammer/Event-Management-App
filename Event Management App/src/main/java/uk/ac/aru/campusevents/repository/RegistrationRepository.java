/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: RegistrationRepository.java
 * Purpose:
 *   Defines the persistence port (DAO) for storing and querying student registrations.
 * Security & Design Notes:
 *   • Interface hides persistence details behind a stable abstraction.
 *   • Methods avoid null returns (Optional/List) to reduce NPE risk.
 *   • Status transitions are enforced by the service layer (RBAC + business rules);
 *     the repository remains passive and does not apply authorization logic.
 *   • Safe to implement as in-memory or database-backed storage.
 */
package uk.ac.aru.campusevents.repository;

import uk.ac.aru.campusevents.domain.Registration;
import uk.ac.aru.campusevents.domain.enums.RegStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link Registration} entities.
 * Supports creation, replacement (immutable record update), status changes,
 * and a set of targeted queries used by services and exports.
 */
@SuppressWarnings("unused")
public interface RegistrationRepository {

    /**
     * Persists a new registration and returns the generated identifier.
     *
     * @param r the registration to create (must satisfy domain validation)
     * @return the generated database identifier
     */
    int create(Registration r);

    /**
     * Replaces an existing registration row by ID.
     * Intended for immutable model updates where a full record is rewritten.
     *
     * @param r the complete registration object with a valid ID
     */
    void replace(Registration r);

    /**
     * Updates only the status for a student's registration in a given event.
     * Transition validity (e.g., WAITLISTED → APPROVED) is checked in services.
     *
     * @param eventId the event identifier
     * @param studentId the student identifier
     * @param status the new {@link RegStatus}
     */
    void updateStatus(int eventId, int studentId, RegStatus status);

    /**
     * Finds the active (non-cancelled) registration for a student and event.
     *
     * @param eventId the event identifier
     * @param studentId the student identifier
     * @return an {@link Optional} containing the active registration if present
     */
    Optional<Registration> findActive(int eventId, int studentId);

    /**
     * Retrieves only confirmed/approved registrations for an event.
     * Intended for attendee lists and exports.
     *
     * @param eventId the event identifier
     * @return a list of confirmed registrations (possibly empty)
     */
    List<Registration> findByEvent(int eventId);

    /**
     * Retrieves all registrations for an event, including WAITLISTED and CANCELLED.
     * Intended for organizer/admin views and auditing.
     *
     * @param eventId the event identifier
     * @return a list of all registrations (possibly empty)
     */
    List<Registration> findAllByEvent(int eventId);

    /**
     * Retrieves registrations made by a specific student across events.
     *
     * @param studentId the student identifier
     * @return a list of registrations (possibly empty)
     */
    List<Registration> findByStudent(int studentId);

    /**
     * Counts confirmed registrations in a given event for capacity checks.
     * Implementations should count only seats that consume capacity
     * (typically REGISTERED and APPROVED, excluding WAITLISTED and CANCELLED).
     *
     * @param eventId the event identifier
     * @return number of confirmed registrations
     */
    long countByEvent(int eventId);
}

