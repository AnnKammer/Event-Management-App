/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: NoOpAuditService.java
 * Purpose:
 *   No-op (null-object) implementation of AuditService.
 * Usage:
 *   • Use in dev/demo builds when audit storage/query is not required.
 *   • Keeps service constructors stable while disabling audit side effects.
 * Security:
 *   • Returns empty results for queries (leaks nothing).
 *   • Does not throw on record(); callers never need null checks.
 */
package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.AuditLog;
import uk.ac.aru.campusevents.domain.enums.AuditAction;
import uk.ac.aru.campusevents.domain.enums.AuditEntity;
import uk.ac.aru.campusevents.dto.AuditFilter;
import uk.ac.aru.campusevents.service.AuditService;

import java.util.List;

public final class NoOpAuditService implements AuditService {

    @Override
    public void record(AuditAction action, AuditEntity entity, Integer entityId,
                       Integer actorUserId, String detailsJson) {
        // Intentionally no-op.
        // Keep silent to avoid spamming console in demos/tests.
    }

    @Override
    public List<AuditLog> find(AuditFilter filter, int actorUserId) {
        // No storage = no results.
        return List.of();
    }
}


