/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: EventService.java
 * Purpose:
 *   Organizer-facing use-cases for managing events.
 *
 * Security & Design Notes:
 *   • All mutating methods require actorUserId; implementations must enforce
 *     ORGANIZER (or ADMIN) role and ownership.
 *   • No persistence details are exposed; repositories are used behind this interface.
 *   • Public search is read-only; it does not require an actor.
 */

package uk.ac.aru.campusevents.service;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.dto.EventSearchCriteria;

import java.util.List;

/**
 * Service boundary for organizer operations on {@link Event}.
 * Implementations should:
 *   • Validate input (titles, dates, capacity > 0, etc.).
 *   • Enforce RBAC and ownership (actor must be allowed to manage the event/org).
 *   • Guard status transitions according to business rules.
 *   • Delegate persistence to repository interfaces.
 */
@SuppressWarnings("unused")
public interface EventService {

    /**
     * Creates a new event owned by the requesting organizer (or their organization).
     *
     * @param actorUserId authenticated user performing the action
     * @param e           event draft to create (ownership invariants enforced in domain)
     * @return generated event identifier
     * @throws SecurityException        if the actor lacks permission
     * @throws IllegalArgumentException if validation fails
     */
    int createEvent(int actorUserId, Event e);

    /**
     * Updates an existing event (non-ownership fields).
     *
     * @param actorUserId authenticated user performing the action
     * @param e           event with updated fields (must contain a valid id)
     * @throws SecurityException        if the actor lacks permission or ownership
     * @throws IllegalArgumentException if validation fails or id is missing
     */
    void updateEvent(int actorUserId, Event e);

    /**
     * Deletes an event the actor is allowed to manage.
     *
     * @param actorUserId authenticated user performing the action
     * @param eventId     identifier of the event to delete
     * @throws SecurityException if the actor lacks permission or ownership
     */
    void deleteEvent(int actorUserId, int eventId);

    /**
     * Searches for events based on immutable criteria.
     * This is a read-only, public operation (no actor required).
     *
     * @param c search criteria (category/text/date range/location)
     * @return matching events (possibly empty), typically sorted by start date
     */
    List<Event> search(EventSearchCriteria c);

    /**
     * Returns all events that the given user can manage as an organizer:
     *   • Personal events where organizer_user_id == actorUserId
     *   • Org-owned events for organisations they can manage (OWNER/MANAGER)
     *
     * Implementations may also apply additional ordering (e.g., newest first).
     *
     * @param actorUserId authenticated organizer
     * @return manageable events for that organizer (possibly empty)
     */
    List<Event> listOrganizerEvents(int actorUserId);

    /**
     * Returns a human-readable organizer name for an event:
     *   • "Firstname Lastname" for user-owned events
     *   • organization.name for organization-owned events
     *   • "Unknown organizer" as a fallback
     *
     * Implementations (e.g. EventServiceImpl) may override this to resolve
     * real names from UserRepository / OrganizationRepository. The default
     * implementation uses simple fallback strings.
     *
     * @param e the event (may be null)
     * @return a display-friendly organizer name
     */
    default String resolveOrganizerName(Event e) {
        if (e == null) return "Unknown organizer";
        if (e.getOrganizerUserId() != null) {
            return "Organizer #" + e.getOrganizerUserId();
        }
        if (e.getOrganizationId() != null) {
            return "Organization #" + e.getOrganizationId();
        }
        return "Unknown organizer";
    }
}
