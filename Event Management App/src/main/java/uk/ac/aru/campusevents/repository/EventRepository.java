/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: EventRepository.java
 * Purpose:
 *   Defines the persistence port for managing Event entities.
 *   Acts as an abstraction between the domain/service layer and the underlying data source.
 * Security & Design Notes:
 *   • No raw SQL defined here — JDBC or ORM adapters implement prepared statements.
 *   • Only full Event objects are exposed to services, which enforce RBAC and validation.
 *   • Enables substitution of InMemory or Database-backed implementations without changing services.
 */
package uk.ac.aru.campusevents.repository;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.dto.EventSearchCriteria;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for persisting and retrieving {@link Event} entities.
 * Implementations may store data in-memory, via JDBC, or through an ORM.
 * Services depend on this interface rather than concrete persistence logic.
 */
@SuppressWarnings("unused")
public interface EventRepository {

    /**
     * Persists a new event and returns the generated identifier.
     *
     * @param e the event to create (must satisfy domain invariants)
     * @return the generated database identifier
     */
    int create(Event e);

    /**
     * Updates an existing event record.
     *
     * @param e the modified event object (must have a valid ID)
     */
    void update(Event e);

    /**
     * Deletes the event with the specified identifier.
     *
     * @param id the event ID to delete
     */
    void delete(int id);

    /**
     * Finds an event by its unique identifier.
     *
     * @param id event identifier
     * @return an {@link Optional} containing the event, or empty if not found
     */
    Optional<Event> findById(int id);

    /**
     * Searches for events that match the given criteria.
     *
     * @param c immutable filter criteria
     * @return a list of matching events (possibly empty)
     */
    List<Event> search(EventSearchCriteria c);

    /**
     * Retrieves all events owned by a specific organizer user.
     *
     * @param organizerId the organizer’s user ID
     * @return a list of events owned by the specified user
     */
    List<Event> findByOrganizer(int organizerId);
}


