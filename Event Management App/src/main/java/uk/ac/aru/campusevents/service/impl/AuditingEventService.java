/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AuditingEventService.java
 * Purpose:
 *   EventService decorator that records audit logs for key mutations.
 * Security & Design Notes:
 *   • Logs CREATE/UPDATE/DELETE for EVENT.
 *   • Update writes a compact diff (only changed fields).
 *   • Avoids PII; stores titles/categories/locations only.
 *   • Delegates all business logic + RBAC to the wrapped EventService.
 */
package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.AuditAction;
import uk.ac.aru.campusevents.domain.enums.AuditEntity;
import uk.ac.aru.campusevents.dto.EventSearchCriteria;
import uk.ac.aru.campusevents.repository.EventRepository;
import uk.ac.aru.campusevents.service.AuditService;
import uk.ac.aru.campusevents.service.EventService;

import java.util.List;
import java.util.Objects;

public final class AuditingEventService implements EventService {

    private final EventService delegate;
    private final AuditService audit;
    private final EventRepository eventRepo;

    /**
     * @param delegate  real EventService (e.g., EventServiceImpl)
     * @param audit     AuditService (real or NoOp)
     * @param eventRepo EventRepository (used to compute diffs on update/delete)
     */
    public AuditingEventService(EventService delegate,
                                AuditService audit,
                                EventRepository eventRepo) {
        this.delegate = Objects.requireNonNull(delegate);
        this.audit = Objects.requireNonNull(audit);
        this.eventRepo = Objects.requireNonNull(eventRepo);
    }

    // ------------------------------ Mutations (audited) ------------------------------

    @Override
    public int createEvent(int actorUserId, Event e) {
        int id = delegate.createEvent(actorUserId, e);
        // Re-read to enrich details (title/category/status/location)
        eventRepo.findById(id).ifPresent(created -> {
            String details = json(
                    "title", nz(created.getTitle()),
                    "category", enumToString(created.getCategory()),
                    "status", String.valueOf(created.getStatus()),
                    "location", nz(created.getLocation())
            );
            audit.record(AuditAction.CREATE, AuditEntity.EVENT, id, actorUserId, details);
        });
        return id;
    }

    @Override
    public void updateEvent(int actorUserId, Event updated) {
        // Fetch before snapshot (best-effort for diff)
        Event before = eventRepo.findById(updated.getId()).orElse(null);

        delegate.updateEvent(actorUserId, updated);

        // Fetch after snapshot
        Event after = eventRepo.findById(updated.getId()).orElse(null);

        String details = (before != null && after != null)
                ? diffEvent(before, after)
                : json("note", "no-diff-available"); // fallback

        audit.record(AuditAction.UPDATE, AuditEntity.EVENT, updated.getId(), actorUserId, details);
    }

    @Override
    public void deleteEvent(int actorUserId, int eventId) {
        // Capture small context before deletion
        String details = eventRepo.findById(eventId)
                .map(ev -> json(
                        "title", nz(ev.getTitle()),
                        "category", enumToString(ev.getCategory()),
                        "status", String.valueOf(ev.getStatus())
                ))
                .orElse(json("note", "event-not-found-pre-delete"));

        delegate.deleteEvent(actorUserId, eventId);

        audit.record(AuditAction.DELETE, AuditEntity.EVENT, eventId, actorUserId, details);
    }

    // ------------------------------ Queries (pass-through) ----------------------------

    @Override
    public List<Event> search(EventSearchCriteria c) {
        return delegate.search(c);
    }

    @Override
    public List<Event> listOrganizerEvents(int actorUserId) {
        return delegate.listOrganizerEvents(actorUserId);
    }

    @Override
    public String resolveOrganizerName(Event e) {
        return delegate.resolveOrganizerName(e);
    }

    // ------------------------------ Helpers ------------------------------------------

    /** Build a compact JSON diff of changed fields only. */
    private static String diffEvent(Event before, Event after) {
        JsonBuilder jb = new JsonBuilder().begin();
        // Only include keys for values that changed (null-safe)
        putIfChanged(jb, "title", before.getTitle(), after.getTitle());
        putIfChanged(jb, "description", before.getDescription(), after.getDescription());
        putIfChanged(jb, "category",
                enumToString(before.getCategory()), enumToString(after.getCategory()));
        putIfChanged(jb, "status",
                String.valueOf(before.getStatus()), String.valueOf(after.getStatus()));
        putIfChanged(jb, "startDateTime",
                String.valueOf(before.getStartDateTime()), String.valueOf(after.getStartDateTime()));
        putIfChanged(jb, "endDateTime",
                String.valueOf(before.getEndDateTime()), String.valueOf(after.getEndDateTime()));
        putIfChanged(jb, "location", before.getLocation(), after.getLocation());
        return jb.end();
    }

    private static void putIfChanged(JsonBuilder jb, String key, String a, String b) {
        if (!Objects.equals(a, b)) {
            jb.kv(key, nz(a) + " → " + nz(b));
        }
    }

    private static String nz(String s) {
        return (s == null) ? "" : s;
    }

    private static String enumToString(Enum<?> e) {
        return (e == null) ? "" : e.name();
    }

    /** Minimal JSON helpers (flat objects, string values only). */
    private static String json(Object... kv) {
        JsonBuilder jb = new JsonBuilder().begin();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            jb.kv(String.valueOf(kv[i]), String.valueOf(kv[i + 1]));
        }
        return jb.end();
    }

    /** Tiny builder to avoid external JSON libs. */
    private static final class JsonBuilder {
        private final StringBuilder sb = new StringBuilder();
        private boolean first;

        JsonBuilder begin() { sb.append('{'); first = true; return this; }

        JsonBuilder kv(String key, String val) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(esc(key)).append('"').append(':')
                    .append('"').append(esc(val)).append('"');
            return this;
        }

        String end() { return sb.append('}').toString(); }

        private static String esc(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
