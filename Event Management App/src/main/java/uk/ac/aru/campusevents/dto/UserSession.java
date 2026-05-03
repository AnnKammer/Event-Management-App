/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: UserSession.java
 * Purpose:
 *   Represents an authenticated user snapshot shared with controllers and UI layers.
 * Security & Design Notes:
 *   • Immutable record — values cannot be altered once created.
 *   • Contains no password or hash; safe for client-side session handling.
 *   • Includes role information for RBAC (role-based access control) decisions.
 * Usage:
 *   Returned after successful authentication and stored in session context.
 */
package uk.ac.aru.campusevents.dto;

import uk.ac.aru.campusevents.domain.enums.Role;

import java.util.Set;

/**
 * Immutable representation of the currently authenticated user.
 * Contains only non-sensitive data required for authorization and UI personalization.
 * Passwords and hashes are never included. Controllers and views use this record
 * to determine what actions the current user is permitted to perform.
 */
public record UserSession(
        /* Unique user identifier from the database. */
        int userId,

        /* Display name for greeting/personalization. */
        String name,

        /*  User email (used for identification, not authentication). */
        String email,

        /*  Set of roles used for authorization checks. */
        Set<Role> roles
) { }

