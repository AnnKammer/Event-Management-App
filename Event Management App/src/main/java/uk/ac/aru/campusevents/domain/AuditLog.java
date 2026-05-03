/* Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AuditLog.java
 * Purpose:
 *   Immutable audit log entry (id assigned by repository).
 * Security:
 *   • Stores only actor id + structured details; no sensitive payloads.
 * Design:
 *   • Strongly-typed enums for action/entity (no free-text drift).
 *   • Uses Instant for timezone-agnostic timestamps.
 */
package uk.ac.aru.campusevents.domain;

import uk.ac.aru.campusevents.domain.enums.AuditAction;
import uk.ac.aru.campusevents.domain.enums.AuditEntity;

import java.time.Instant;
import java.util.Objects;

@SuppressWarnings("unused")
public final class AuditLog {
    private final int id;                 // 0 for new; repository assigns real id
    private final Instant ts;             // when it happened
    private final Integer actorUserId;    // null = system
    private final AuditAction action;     // enum
    private final AuditEntity entity;     // enum
    private final Integer entityId;       // null for system/global
    private final String detailsJson;     // optional JSON summary

    public AuditLog(int id,
                    Instant ts,
                    Integer actorUserId,
                    AuditAction action,
                    AuditEntity entity,
                    Integer entityId,
                    String detailsJson) {
        this.id = id;
        this.ts = ts == null ? Instant.now() : ts;
        this.actorUserId = actorUserId;
        this.action = Objects.requireNonNull(action, "action");
        this.entity = Objects.requireNonNull(entity, "entity");
        this.entityId = entityId;
        this.detailsJson = detailsJson == null ? "" : detailsJson;
    }

    /** Convenience factory for new entries (id=0, ts=now). */
    public static AuditLog newEntry(AuditAction action,
                                    AuditEntity entity,
                                    Integer entityId,
                                    Integer actorUserId,
                                    String detailsJson) {
        return new AuditLog(0, Instant.now(), actorUserId, action, entity, entityId, detailsJson);
    }

    /* ---------- Getters (used by repositories/services) ---------- */
    public int getId()             { return id; }
    public Instant getTs()         { return ts; }
    public Integer getActorUserId(){ return actorUserId; }
    public AuditAction getAction() { return action; }
    public AuditEntity getEntity() { return entity; }
    public Integer getEntityId()   { return entityId; }
    public String getDetailsJson() { return detailsJson; }

    /** Rebuild with assigned id (immutability preserved). */
    public AuditLog withId(int newId) {
        return new AuditLog(newId, ts, actorUserId, action, entity, entityId, detailsJson);
    }
}
