/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AuditEntity.java
 * Purpose: Canonical list of entity types that can appear in the audit log.
 * Security notes:
 *   • Enum prevents free-text entity names, aiding reliable filtering.
 *   • Keep aligned with domain model to avoid drift.
 */
package uk.ac.aru.campusevents.domain.enums;

public enum AuditEntity {
    USER,
    ORGANIZATION,
    EVENT,
    REGISTRATION,
    FEEDBACK,
    NOTIFICATION,
    SYSTEM
}

