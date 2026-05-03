/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AuditFilter.java
 * Purpose:
 *   Filter object for querying audit logs.
 * Security & Design Notes:
 *   • Immutable record → prevents tampering between layers.
 *   • Optional fields allow flexible querying.
 *   • Uses Instant for precise, timezone-agnostic range filtering.
 */
package uk.ac.aru.campusevents.dto;

import uk.ac.aru.campusevents.domain.enums.AuditAction;
import uk.ac.aru.campusevents.domain.enums.AuditEntity;
import java.time.Instant;

public record AuditFilter(
        AuditEntity entity,      // EVENT, REGISTRATION, USER, etc.
        Integer entityId,        // e.g. specific event_id
        Integer actorUserId,     // who performed the action
        AuditAction action,      // CREATE, UPDATE, DELETE, etc.
        Instant from,            // start of time range (nullable)
        Instant to,              // end of time range (nullable)
        Integer limit            // max rows, e.g. 200
) { }

