/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AuditingRegistrationService.java
 * Purpose:
 *   RegistrationService decorator that records audit logs for
 *   register and cancel operations.
 *
 * Security & Design Notes:
 *   • Logs REGISTER and CANCEL on the REGISTRATION entity.
 *   • Minimizes PII; records ids and simple status/metadata only.
 *   • Delegates all business logic + RBAC to the wrapped service.
 *   • Reads RegistrationRepository after mutations to capture final status/id.
 *   • Automatic waitlist promotion (WAITLISTED → APPROVED) is handled by the
 *     underlying RegistrationService implementation and may be audited there.
 */

package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.Registration;
import uk.ac.aru.campusevents.domain.enums.AuditAction;
import uk.ac.aru.campusevents.domain.enums.AuditEntity;
import uk.ac.aru.campusevents.repository.EventRepository;
import uk.ac.aru.campusevents.repository.RegistrationRepository;
import uk.ac.aru.campusevents.service.AuditService;
import uk.ac.aru.campusevents.service.RegistrationService;
import uk.ac.aru.campusevents.dto.RegistrationStats;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AuditingRegistrationService implements RegistrationService {

    private final RegistrationService delegate;
    private final AuditService audit;
    private final RegistrationRepository regRepo;
    private final EventRepository eventRepo; // optional context (title)

    /**
     * @param delegate  the real RegistrationService (e.g., RegistrationServiceImpl)
     * @param audit     AuditService (real or NoOp)
     * @param regRepo   to inspect resulting registration state for auditing
     * @param eventRepo optional context for event title in details
     */
    public AuditingRegistrationService(RegistrationService delegate,
                                       AuditService audit,
                                       RegistrationRepository regRepo,
                                       EventRepository eventRepo) {
        this.delegate = Objects.requireNonNull(delegate);
        this.audit = Objects.requireNonNull(audit);
        this.regRepo = Objects.requireNonNull(regRepo);
        this.eventRepo = Objects.requireNonNull(eventRepo);
    }

    // ------------------------------ Student APIs (audited) ------------------------------

    @Override
    public void register(int actorStudentId, int eventId) {
        delegate.register(actorStudentId, eventId);

        // After the call, try to find the (active) registration for this student+event.
        // It may be REGISTERED or WAITLISTED (based on capacity).
        Optional<Registration> regOpt = regRepo.findActive(eventId, actorStudentId);

        String title = eventRepo.findById(eventId).map(Event::getTitle).orElse("");
        if (regOpt.isPresent()) {
            Registration r = regOpt.get();
            String details = json(
                    "eventId", String.valueOf(eventId),
                    "eventTitle", nz(title),
                    "studentId", String.valueOf(actorStudentId),
                    "status", String.valueOf(r.getStatus())
            );
            audit.record(AuditAction.REGISTER, AuditEntity.REGISTRATION, r.getId(), actorStudentId, details);
        } else {
            // Fallback (should be rare): no active reg found, still log attempt
            String details = json(
                    "eventId", String.valueOf(eventId),
                    "eventTitle", nz(title),
                    "studentId", String.valueOf(actorStudentId),
                    "status", "unknown"
            );
            audit.record(AuditAction.REGISTER, AuditEntity.REGISTRATION, null, actorStudentId, details);
        }
    }

    @Override
    public RegistrationStats getEventStats(int eventId) {
        return delegate.getEventStats(eventId);
    }

    @Override
    public void cancel(int actorStudentId, int eventId) {
        // Capture current reg before cancelling so we have the reg id/status
        Optional<Registration> before = regRepo.findActive(eventId, actorStudentId);

        delegate.cancel(actorStudentId, eventId);

        String title = eventRepo.findById(eventId).map(Event::getTitle).orElse("");
        if (before.isPresent()) {
            Registration r = before.get();
            String details = json(
                    "eventId", String.valueOf(eventId),
                    "eventTitle", nz(title),
                    "studentId", String.valueOf(actorStudentId),
                    "prevStatus", String.valueOf(r.getStatus()),
                    "newStatus", "CANCELLED"
            );
            audit.record(AuditAction.CANCEL, AuditEntity.REGISTRATION, r.getId(), actorStudentId, details);
        } else {
            String details = json(
                    "eventId", String.valueOf(eventId),
                    "eventTitle", nz(title),
                    "studentId", String.valueOf(actorStudentId),
                    "note", "no-active-registration-found"
            );
            audit.record(AuditAction.CANCEL, AuditEntity.REGISTRATION, null, actorStudentId, details);
        }
    }

    // ------------------------------ Queries (pass-through) ------------------------------

    @Override
    public List<Integer> listAttendeeUserIds(int eventId) {
        return delegate.listAttendeeUserIds(eventId);
    }

    @Override
    public List<Event> listMyEvents(int studentId) {
        return delegate.listMyEvents(studentId);
    }

    @Override
    public String exportMyEventsCsv(int studentId) {
        // Read-only operation; we just delegate with no extra audit
        return delegate.exportMyEventsCsv(studentId);
    }

    // ------------------------------ Helpers --------------------------------------------

    private static String nz(String s) { return s == null ? "" : s; }

    /** Minimal JSON helper (flat object; string values only). */
    private static String json(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (i > 0) sb.append(',');
            String key = String.valueOf(kv[i]);
            String val = String.valueOf(kv[i + 1]);
            sb.append('"').append(esc(key)).append('"').append(':')
                    .append('"').append(esc(val)).append('"');
        }
        return sb.append('}').toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
