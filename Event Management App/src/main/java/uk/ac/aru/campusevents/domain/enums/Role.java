/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: Role.java
 * Purpose: RBAC roles for access control across controllers/services.
 * Security notes: Use enums (not strings) for compile-time safety in RBAC checks.
 * References: Element 010 brief — separate Student vs. Organizer capabilities; Admin maintenance.
 */
package uk.ac.aru.campusevents.domain.enums;

/**
 * System roles used by authentication/authorization to gate privileged operations.
 */
public enum Role {
    /** Can browse/search/register for events and receive notifications. */
    STUDENT,
    /** Can create/update/delete events and manage attendees. */
    ORGANIZER,
    /** Can perform administrative tasks (e.g., user maintenance, audits). */
    ADMIN
}


