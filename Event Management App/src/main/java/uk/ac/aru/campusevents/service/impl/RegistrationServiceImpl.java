/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: RegistrationServiceImpl.java
 * Purpose:
 *   Implements registration workflows for students, including:
 *     • Event registration with per-event capacity control
 *     • Automatic waitlisting when events are full
 *     • Cancellation with automatic waitlist promotion
 *
 * Design & Security Notes:
 *   • RBAC is enforced via role checks (STUDENT for self-registration/cancel).
 *   • Capacity is modelled on the Event itself (event.getCapacity());
 *     registration counting is backed by the repository.
 *   • No separate waitlist store: WAITLISTED rows in the registration table
 *     *are* the waitlist, ordered by registered_at.
 *   • Services never bypass domain validation – {@link Registration} enforces
 *     required invariants (ids > 0, non-null status).
 */
package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.Registration;
import uk.ac.aru.campusevents.domain.enums.RegStatus;
import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.domain.enums.EventStatus;
import uk.ac.aru.campusevents.exceptions.ForbiddenException;
import uk.ac.aru.campusevents.repository.EventRepository;
import uk.ac.aru.campusevents.repository.RegistrationRepository;
import uk.ac.aru.campusevents.repository.UserRepository;
import uk.ac.aru.campusevents.service.NotificationService;
import uk.ac.aru.campusevents.service.RegistrationService;
import uk.ac.aru.campusevents.dto.RegistrationStats;

import java.time.LocalDate;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RegistrationServiceImpl implements RegistrationService {

    // Dependencies (repositories + notifications)
    private final UserRepository userRepo;
    private final EventRepository eventRepo;
    private final RegistrationRepository regRepo;
    private final NotificationService notifications;

    public RegistrationServiceImpl(UserRepository userRepo,
                                   EventRepository eventRepo,
                                   RegistrationRepository regRepo,
                                   NotificationService notifications) {
        this.userRepo = Objects.requireNonNull(userRepo);
        this.eventRepo = Objects.requireNonNull(eventRepo);
        this.regRepo = Objects.requireNonNull(regRepo);
        this.notifications = Objects.requireNonNull(notifications);
    }

    /* ----------------------------------------------------------------------
       STUDENT OPERATIONS
       ---------------------------------------------------------------------- */

    @Override
    public void register(int actorStudentId, int eventId) {
        requireStudent(actorStudentId);

        // Verify the event exists and read its capacity
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        // --------- NEW: status + date rules ---------

        // 1) Event must be OPEN for registration
        if (event.getStatus() != EventStatus.OPEN) {
            throw new IllegalStateException("This event is not open for registration.");
        }

        // 2) Registration allowed only up to the day BEFORE the event
        if (event.getStartDateTime() != null) {
            LocalDate today    = LocalDate.now();
            LocalDate eventDay = event.getStartDateTime().toLocalDate();

            // eventDay.isAfter(today)  → OK (future)
            // eventDay.equals(today)   → same day → block
            // eventDay.isBefore(today) → past → block
            if (!eventDay.isAfter(today)) {
                throw new IllegalStateException(
                        "Registration is closed for this event. " +
                                "You can only register up to the day before the event."
                );
            }
        }

        int capacity = event.getCapacity();
        if (capacity <= 0) {
            // Should not happen if domain validation is correct
            throw new IllegalStateException("Event capacity must be positive");
        }

        // Prevent duplicate active registration for same user/event
        regRepo.findActive(eventId, actorStudentId)
                .ifPresent(r -> {
                    throw new IllegalStateException("Already registered or waitlisted for this event");
                });

        // Capacity check: per-event pool, using Event.capacity
        long used = regRepo.countByEvent(eventId);

        if (used < capacity) {
            // Seat available → directly registered
            var confirmed = Registration.newRegistration(eventId, actorStudentId, RegStatus.REGISTERED);
            regRepo.create(confirmed);
            notifications.send(actorStudentId, "You are registered for the event.");
        } else {
            // Full → WAITLISTED registration
            var wait = Registration.newRegistration(eventId, actorStudentId, RegStatus.WAITLISTED);
            regRepo.create(wait);
            notifications.send(actorStudentId, "Event full: you were added to the waitlist.");
        }
    }

    @Override
    public RegistrationStats getEventStats(int eventId) {
        var all = regRepo.findAllByEvent(eventId); // includes all statuses

        int registeredOrApproved = 0;
        int waitlisted = 0;
        int cancelled = 0;

        for (Registration r : all) {
            switch (r.getStatus()) {
                case REGISTERED, APPROVED -> registeredOrApproved++;
                case WAITLISTED -> waitlisted++;
                case CANCELLED -> cancelled++;
            }
        }
        return new RegistrationStats(registeredOrApproved, waitlisted, cancelled);
    }




    @Override
    public void cancel(int actorStudentId, int eventId) {
        requireStudent(actorStudentId);

        Registration current = regRepo.findActive(eventId, actorStudentId)
                .orElseThrow(() -> new IllegalArgumentException("No active registration found"));

        // Cancel registration
        regRepo.updateStatus(eventId, actorStudentId, RegStatus.CANCELLED);
        notifications.send(actorStudentId, "You cancelled your registration.");

        // Attempt to promote next waitlisted attendee
        promoteNextIfCapacity(eventId);
    }

    /* ----------------------------------------------------------------------
       QUERY METHODS
       ---------------------------------------------------------------------- */

    @Override
    public List<Integer> listAttendeeUserIds(int eventId) {
        // Only confirmed attendees (REGISTERED or APPROVED)
        return regRepo.findByEvent(eventId).stream()
                .filter(r -> r.getStatus() == RegStatus.REGISTERED
                        || r.getStatus() == RegStatus.APPROVED)
                .map(Registration::getStudentId)
                .toList();
    }

    @Override
    public List<Event> listMyEvents(int studentId) {
        // Active registrations for the student (not CANCELLED)
        var regs = regRepo.findByStudent(studentId).stream()
                .filter(r -> r.getStatus() != RegStatus.CANCELLED)
                .toList();

        LocalDate today = LocalDate.now();

        return regs.stream()
                .map(r -> eventRepo.findById(r.getEventId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(e -> autoCompleteIfPast(e, today))   // <--- NEW LINE
                .sorted(Comparator.comparing(
                        Event::getStartDateTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }


    /**
     * Exports the student's active event registrations as CSV text.
     * Columns: Title,StartDateTime,Location,Status
     */
    @Override
    public String exportMyEventsCsv(int studentId) {
        List<Event> events = listMyEvents(studentId);

        StringBuilder sb = new StringBuilder();
        sb.append("Title,StartDateTime,Location,Status\n");

        for (Event e : events) {
            String title     = nullSafe(e.getTitle());
            String location  = nullSafe(e.getLocation());
            String status    = (e.getStatus() == null) ? "" : e.getStatus().name();
            String startTime = (e.getStartDateTime() == null)
                    ? ""
                    : e.getStartDateTime().toString(); // ISO-8601 is fine for CSV

            sb.append(asCsvField(title)).append(',')
                    .append(asCsvField(startTime)).append(',')
                    .append(asCsvField(location)).append(',')
                    .append(asCsvField(status)).append('\n');
        }

        return sb.toString();
    }

    /* ----------------------------------------------------------------------
       INTERNAL HELPERS
       ---------------------------------------------------------------------- */

    // Promote next user from waitlist if capacity allows
    private void promoteNextIfCapacity(int eventId) {
        if (!hasCapacity(eventId)) {
            return;
        }

        // Look for earliest WAITLISTED registration
        var nextOpt = regRepo.findAllByEvent(eventId).stream()
                .filter(r -> r.getStatus() == RegStatus.WAITLISTED)
                .sorted(Comparator.comparing(Registration::getRegisteredAt))
                .findFirst();

        if (nextOpt.isEmpty()) {
            return;
        }

        var next = nextOpt.get();
        regRepo.updateStatus(eventId, next.getStudentId(), RegStatus.APPROVED);
        notifications.send(next.getStudentId(), "Good news! You have a seat for the event.");
    }

    /**
     * Local helper for RegistrationServiceImpl:
     * auto-mark an event as COMPLETED if its date is today or in the past
     * and the status is still OPEN or FULL.
     */
    private void autoCompleteIfPast(Event e, LocalDate today) {
        if (e == null || e.getStartDateTime() == null) {
            return;
        }

        LocalDate eventDay = e.getStartDateTime().toLocalDate();
        if (!eventDay.isAfter(today)) {
            EventStatus status = e.getStatus();
            if (status == EventStatus.OPEN || status == EventStatus.FULL) {
                e.setStatus(EventStatus.COMPLETED);
                eventRepo.update(e);
            }
        }
    }


    // Capacity helper: per-event pool using Event.capacity
    private boolean hasCapacity(int eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        int capacity = event.getCapacity();
        if (capacity <= 0) {
            // Consistent with register(): invalid domain state
            throw new IllegalStateException("Event capacity must be positive");
        }

        long used = regRepo.countByEvent(eventId);
        return used < capacity;
    }

    // RBAC guard: must be a STUDENT
    private void requireStudent(int actorUserId) {
        var u = userRepo.findById(actorUserId)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        if (!u.getRoles().contains(Role.STUDENT)) {
            throw new ForbiddenException("Student role required");
        }
    }

    // Null-safe text
    private static String nullSafe(String value) {
        return (value == null) ? "" : value;
    }

    // Minimal CSV escaping: wrap in quotes if it contains comma/quote/newline
    private static String asCsvField(String raw) {
        String value = nullSafe(raw);
        if (value.contains("\"") || value.contains(",") || value.contains("\n") || value.contains("\r")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }
}
