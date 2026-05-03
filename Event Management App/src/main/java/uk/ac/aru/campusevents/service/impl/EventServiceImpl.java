/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: EventServiceImpl.java
 * Purpose:
 *   RBAC-enforced event management service supporting events owned by
 *   either a single organizer user or an organization, with attendee/waitlist
 *   notifications on important updates and deletions.
 *
 * Security notes:
 *   • Requires ORGANIZER or ADMIN role for all mutating operations.
 *   • Ownership rules:
 *       - User-owned event: only the owning organizer user may mutate.
 *       - Org-owned event: only users who can manage that org may mutate.
 *   • Ownership is immutable (service never re-parents an event).
 *   • Capacity is stored on Event; RegistrationService enforces it at registration time.
 *   • Notifications:
 *       - updateEvent(): notifies attendees + waitlist when time/location/title/status change.
 *       - deleteEvent(): notifies all registered/waitlisted users about cancellation.
 */

package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.Registration;
import uk.ac.aru.campusevents.domain.enums.EventStatus;
import uk.ac.aru.campusevents.domain.enums.RegStatus;
import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.dto.EventSearchCriteria;
import uk.ac.aru.campusevents.exceptions.ForbiddenException;
import uk.ac.aru.campusevents.repository.EventRepository;
import uk.ac.aru.campusevents.repository.OrganizationRepository;
import uk.ac.aru.campusevents.repository.RegistrationRepository;
import uk.ac.aru.campusevents.repository.UserOrganizationRepository;
import uk.ac.aru.campusevents.repository.UserRepository;
import uk.ac.aru.campusevents.repository.OrganizationApprovalRepository;
import uk.ac.aru.campusevents.service.EventService;
import uk.ac.aru.campusevents.service.NotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class EventServiceImpl implements EventService {

    private final EventRepository eventRepo;
    private final UserRepository userRepo;
    private final OrganizationRepository orgRepo;
    private final UserOrganizationRepository userOrgRepo;  // kept for possible future use
    private final OrganizationApprovalRepository orgApprovalRepo;
    private final RegistrationRepository regRepo;          // for attendee/waitlist lookups
    private final NotificationService notifications;       // for user messages

    public EventServiceImpl(EventRepository eventRepo,
                            UserRepository userRepo,
                            OrganizationRepository orgRepo,
                            UserOrganizationRepository userOrgRepo,
                            OrganizationApprovalRepository orgApprovalRepo,
                            RegistrationRepository regRepo,
                            NotificationService notifications) {
        this.eventRepo = Objects.requireNonNull(eventRepo);
        this.userRepo = Objects.requireNonNull(userRepo);
        this.orgRepo = Objects.requireNonNull(orgRepo);
        this.userOrgRepo = Objects.requireNonNull(userOrgRepo);
        this.orgApprovalRepo = Objects.requireNonNull(orgApprovalRepo);
        this.regRepo = Objects.requireNonNull(regRepo);
        this.notifications = Objects.requireNonNull(notifications);
    }

    // ----------------------------- RBAC helpers ------------------------------

    private void requireOrganizerRole(int actorUserId) {
        var user = userRepo.findById(actorUserId)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        if (!(user.getRoles().contains(Role.ORGANIZER) || user.getRoles().contains(Role.ADMIN))) {
            throw new ForbiddenException("Organizer or Admin role required");
        }
    }

    private boolean isAdmin(int userId) {
        return userRepo.findById(userId)
                .map(u -> u.getRoles().contains(Role.ADMIN))
                .orElse(false);
    }

    /**
     * Returns true if the given user is allowed to manage the organisation:
     *   • Admins always return true
     *   • Otherwise, require that organisation_id exists in organization_approval
     *     and that the user's email is listed as OWNER/MANAGER.
     */
    private boolean canManageOrg(int actorUserId, int orgId) {
        if (orgId <= 0) {
            return false;
        }

        // Admins can manage any organisation
        if (isAdmin(actorUserId)) {
            return true;
        }

        // Check if org has any approvals at all
        var approvals = orgApprovalRepo.findByOrganization(orgId);
        if (approvals.isEmpty()) {
            return false;
        }

        var userOpt = userRepo.findById(actorUserId);
        if (userOpt.isEmpty()) {
            return false;
        }

        String email = userOpt.get().getEmail();
        if (email == null || email.isBlank()) {
            return false;
        }

        // Case-insensitive exists check
        return orgApprovalRepo.exists(orgId, email);
    }

    /**
     * Enforces that:
     *   1) The organisation id is known (has at least one approval row).
     *   2) The current user's email is approved (OWNER or MANAGER) for that org.
     *
     * If no approvals exist for that organisation id:
     *   → IllegalArgumentException("Organisation id X does not exist.")
     *
     * If approvals exist but NOT for this user:
     *   → ForbiddenException("You are not registered as OWNER/MANAGER for this organisation.")
     */
    private void requireCanManageOrg(int actorUserId, int orgId) {
        if (orgId <= 0) {
            throw new IllegalArgumentException("Organisation id must be > 0.");
        }

        // Ensure org id is known in approval table
        var approvals = orgApprovalRepo.findByOrganization(orgId);
        if (approvals.isEmpty()) {
            throw new IllegalArgumentException(
                    "Organisation id " + orgId + " does not exist."
            );
        }

        if (!canManageOrg(actorUserId, orgId)) {
            throw new ForbiddenException(
                    "You are not registered as OWNER/MANAGER for this organisation."
            );
        }
    }

    // ------------------------------- Mutations -------------------------------

    @Override
    public int createEvent(int actorUserId, Event input) {
        requireOrganizerRole(actorUserId);
        if (input == null) {
            throw new IllegalArgumentException("Event required");
        }

        // Basic validation (domain also enforces this, but we keep a clear service-level rule)
        if (input.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }

        final Event created;
        if (input.getOrganizationId() != null) {
            // Org-owned event
            int orgId = input.getOrganizationId();
            requireCanManageOrg(actorUserId, orgId);

            created = Event.newOrgEvent(
                    orgId,
                    input.getTitle(),
                    input.getDescription(),
                    input.getCategory(),                 // enum, passed straight through
                    input.getCategoryOtherDescription(),
                    input.getCapacity()
            );
        } else {
            // User-owned personal event
            created = Event.newPersonalEvent(
                    actorUserId,
                    input.getTitle(),
                    input.getDescription(),
                    input.getCategory(),                 // enum here too
                    input.getCategoryOtherDescription(),
                    input.getCapacity()
            );
        }

        // Copy optional fields if provided
        if (input.getStartDateTime() != null) {
            created.setStartDateTime(input.getStartDateTime());
        }
        if (input.getEndDateTime() != null) {
            created.setEndDateTime(input.getEndDateTime());
        }
        if (input.getLocation() != null) {
            created.setLocation(input.getLocation());
        }

        // --- Date rule: events must be scheduled at least one day in advance ---
        LocalDateTime start = created.getStartDateTime();
        if (start != null) {
            LocalDate today    = LocalDate.now();
            LocalDate eventDay = start.toLocalDate();

            // Only allow if the event day is strictly after today
            if (!eventDay.isAfter(today)) {
                throw new IllegalArgumentException(
                        "Event must be scheduled at least one day in advance."
                );
            }
        }

        // New events start OPEN in the app
        created.setStatus(EventStatus.OPEN);

        // Finally persist
        return eventRepo.create(created);
    }

    @Override
    public void updateEvent(int actorUserId, Event updated) {
        requireOrganizerRole(actorUserId);
        if (updated == null || updated.getId() <= 0) {
            throw new IllegalArgumentException("Valid event id required");
        }

        var existing = eventRepo.findById(updated.getId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        // Capacity validation:
        //  • must be > 0
        //  • cannot be lower than current number of confirmed attendees
        if (updated.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }

        long confirmedCount = regRepo.findByEvent(updated.getId()).stream()
                .filter(r -> r.getStatus() == RegStatus.REGISTERED
                        || r.getStatus() == RegStatus.APPROVED)
                .count();

        if (updated.getCapacity() < confirmedCount) {
            throw new IllegalArgumentException(
                    "Capacity cannot be lower than the current number of attendees (" + confirmedCount + "). " +
                            "If you need to reduce numbers further, cancel the event instead."
            );
        }

        // Ownership RBAC
        if (existing.getOrganizationId() != null) {
            requireCanManageOrg(actorUserId, existing.getOrganizationId());
        } else if (!Objects.equals(existing.getOrganizerUserId(), actorUserId) && !isAdmin(actorUserId)) {
            throw new ForbiddenException("Only the owning organizer (or admin) can update this event");
        }

        // -------- Detect user-visible changes BEFORE saving --------
        boolean timeChanged =
                !Objects.equals(existing.getStartDateTime(), updated.getStartDateTime()) ||
                        !Objects.equals(existing.getEndDateTime(),   updated.getEndDateTime());

        boolean locationChanged =
                !Objects.equals(ns(existing.getLocation()), ns(updated.getLocation()));

        boolean titleChanged =
                !Objects.equals(ns(existing.getTitle()), ns(updated.getTitle()));

        boolean statusChanged =
                !Objects.equals(existing.getStatus(), updated.getStatus());

        // Persist
        eventRepo.update(updated);

        // -------- Notify impacted users (attendees + waitlisted) --------
        if (timeChanged || locationChanged || titleChanged || statusChanged) {
            var attendees = regRepo.findByEvent(updated.getId()).stream()
                    .map(Registration::getStudentId)
                    .distinct()
                    .toList();

            var waitlisted = regRepo.findAllByEvent(updated.getId()).stream()
                    .filter(r -> r.getStatus() == RegStatus.WAITLISTED)
                    .map(Registration::getStudentId)
                    .distinct()
                    .toList();

            String message = buildUpdateMessage(
                    updated.getTitle(),
                    timeChanged,
                    locationChanged,
                    titleChanged,
                    statusChanged,
                    updated.getStatus()
            );

            for (int uid : attendees) {
                notifications.send(uid, message);
            }
            for (int uid : waitlisted) {
                notifications.send(uid, message);
            }
        }
    }

    @Override
    public void deleteEvent(int actorUserId, int eventId) {
        requireOrganizerRole(actorUserId);
        var existing = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        // Ownership RBAC
        if (existing.getOrganizationId() != null) {
            requireCanManageOrg(actorUserId, existing.getOrganizationId());
        } else if (!Objects.equals(existing.getOrganizerUserId(), actorUserId) && !isAdmin(actorUserId)) {
            throw new ForbiddenException("Only the owning organizer can delete this event");
        }

        // Notify all registered + waitlisted BEFORE deletion
        var confirmed = regRepo.findByEvent(eventId).stream()
                .map(Registration::getStudentId)
                .distinct()
                .toList();

        var waitlisted = regRepo.findAllByEvent(eventId).stream()
                .filter(r -> r.getStatus() == RegStatus.WAITLISTED)
                .map(Registration::getStudentId)
                .distinct()
                .toList();

        String cancelMsg = "The event you registered for has been cancelled.";
        for (int uid : confirmed) {
            notifications.send(uid, cancelMsg);
        }
        for (int uid : waitlisted) {
            notifications.send(uid, cancelMsg);
        }

        // Now delete the event
        eventRepo.delete(eventId);
    }

    // -------------------------------- Queries --------------------------------

    @Override
    public List<Event> search(EventSearchCriteria c) {
        List<Event> events = eventRepo.search(c);

        // Auto-complete past events (including "today")
        LocalDate today = LocalDate.now();
        for (Event e : events) {
            autoCompleteIfPast(e, today);
        }

        return events;
    }

    /**
     * Returns all events that the given user can manage as an organiser:
     *   • Personal events where organizer_user_id == actorUserId
     *   • Org-owned events for organisations they can manage (OWNER/MANAGER)
     */
    @Override
    public List<Event> listOrganizerEvents(int actorUserId) {
        var all = eventRepo.search(
                new EventSearchCriteria(
                        null,  // title/category/etc. are all-null: no filtering at repo level
                        null,
                        null,
                        null,
                        null
                )
        );

        LocalDate today = LocalDate.now();

        return all.stream()
                .peek(e -> autoCompleteIfPast(e, today))
                .filter(e -> {
                    if (e.getOrganizerUserId() != null &&
                            e.getOrganizerUserId() == actorUserId) {
                        return true;
                    }
                    if (e.getOrganizationId() != null) {
                        return canManageOrg(actorUserId, e.getOrganizationId());
                    }
                    return false;
                })
                .sorted(Comparator.comparing(
                        Event::getStartDateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    @Override
    public String resolveOrganizerName(Event e) {
        if (e == null) {
            return "Unknown organizer";
        }
        // Personal / user-owned event
        if (e.getOrganizerUserId() != null) {
            return userRepo.findById(e.getOrganizerUserId())
                    .map(u -> (ns(u.getFirstName()) + " " + ns(u.getLastName())).trim())
                    .filter(s -> !s.isBlank())
                    .orElse("Organizer #" + e.getOrganizerUserId());
        }
        // Organization-owned event
        if (e.getOrganizationId() != null) {
            return orgRepo.findById(e.getOrganizationId())
                    .map(org -> ns(org.getName()))
                    .filter(s -> !s.isBlank())
                    .orElse("Organization #" + e.getOrganizationId());
        }
        return "Unknown organizer";
    }

    // ------------------------------ Utilities --------------------------------

    private static String ns(String s) {
        return s == null ? "" : s;
    }

    private static String buildUpdateMessage(String title,
                                             boolean timeChanged,
                                             boolean locationChanged,
                                             boolean titleChanged,
                                             boolean statusChanged,
                                             EventStatus newStatus) {
        StringBuilder msg = new StringBuilder("Event update: ")
                .append(ns(title))
                .append(" — ");
        boolean first = true;
        if (timeChanged) {
            msg.append(first ? "" : "; ").append("time changed");
            first = false;
        }
        if (locationChanged) {
            msg.append(first ? "" : "; ").append("location changed");
            first = false;
        }
        if (titleChanged) {
            msg.append(first ? "" : "; ").append("title changed");
            first = false;
        }
        if (statusChanged) {
            msg.append(first ? "" : "; ").append("status changed to ").append(newStatus);
        }
        return msg.toString();
    }

    /**
     * If the event date is on or before today and the status is still
     * OPEN or FULL, automatically mark it as COMPLETED and persist.
     *
     * Returns true if an update was performed.
     */
    private boolean autoCompleteIfPast(Event e, LocalDate today) {
        if (e == null || e.getStartDateTime() == null) {
            return false;
        }

        LocalDate eventDay = e.getStartDateTime().toLocalDate();

        // If event day is today or earlier -> mark as COMPLETED
        if (!eventDay.isAfter(today)) {
            EventStatus status = e.getStatus();
            if (status == EventStatus.OPEN || status == EventStatus.FULL) {
                e.setStatus(EventStatus.COMPLETED);
                eventRepo.update(e);  // persist the change
                return true;
            }
        }
        return false;
    }
}
