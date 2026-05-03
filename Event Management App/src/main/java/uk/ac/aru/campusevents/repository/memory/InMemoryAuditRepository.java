/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: InMemoryAuditRepository.java
 * Purpose:
 *   Thread-safe in-memory implementation of AuditRepository
 *   for development and testing (no external database).
 * Security & Design Notes:
 *   • Thread-safe: uses ConcurrentHashMap + AtomicInteger.
 *   • Immutable model: AuditLog.withId() used for assigned IDs.
 *   • No PII stored beyond actorUserId and JSON summary text.
 *   • Not suitable for production — contents cleared on restart.
 */
package uk.ac.aru.campusevents.repository.memory;

import uk.ac.aru.campusevents.domain.AuditLog;
import uk.ac.aru.campusevents.dto.AuditFilter;
import uk.ac.aru.campusevents.repository.AuditRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class InMemoryAuditRepository implements AuditRepository {
    private final AtomicInteger seq = new AtomicInteger(1);
    private final Map<Integer, AuditLog> store = new ConcurrentHashMap<>();

    @Override
    public int save(AuditLog log) {
        int id = seq.getAndIncrement();
        store.put(id, log.withId(id));  // <-- requires withId()
        return id;
    }

    @Override
    public List<AuditLog> find(AuditFilter f) {
        var stream = store.values().stream();

        if (f.entity() != null)     stream = stream.filter(l -> l.getEntity() == f.entity());     // <-- getEntity()
        if (f.entityId() != null)   stream = stream.filter(l -> Objects.equals(l.getEntityId(), f.entityId()));
        if (f.actorUserId() != null)stream = stream.filter(l -> Objects.equals(l.getActorUserId(), f.actorUserId()));
        if (f.action() != null)     stream = stream.filter(l -> l.getAction() == f.action());     // enums, so ==
        if (f.from() != null)       stream = stream.filter(l -> !l.getTs().isBefore(f.from()));   // <-- getTs()
        if (f.to() != null)         stream = stream.filter(l -> !l.getTs().isAfter(f.to()));

        var result = stream
                .sorted(Comparator.comparing(AuditLog::getTs).reversed())
                .toList();

        if (f.limit() != null && f.limit() > 0 && result.size() > f.limit()) {
            return result.subList(0, f.limit());
        }
        return result;
    }
}


