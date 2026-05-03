/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AuditService.java
 * Purpose:
 *   Provides an application-level service for recording audit events.
 * Responsibilities:
 *   • Capturing user and system actions for accountability.
 *   • Delegating persistence to the AuditRepository (no direct database logic).
 *   • Ensuring consistent log format and minimizing PII in stored data.
 * Security Notes:
 *   • Exposes only safe, high-level methods to other services.
 *   • Never logs raw credentials or sensitive data.
 *   • Designed to support later replacement with a persistent database implementation.
 */
package uk.ac.aru.campusevents.service;

import uk.ac.aru.campusevents.domain.AuditLog;
import uk.ac.aru.campusevents.domain.enums.AuditAction;
import uk.ac.aru.campusevents.domain.enums.AuditEntity;
import uk.ac.aru.campusevents.dto.AuditFilter;

import java.util.List;

public interface AuditService {
    void record(AuditAction action, AuditEntity entity, Integer entityId, Integer actorUserId, String detailsJson);
    List<AuditLog> find(AuditFilter filter, int actorUserId);
}



