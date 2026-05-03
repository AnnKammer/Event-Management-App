/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: ForbiddenException.java
 * Purpose:
 *   Custom runtime exception thrown when a user lacks the required role or permission.
 * Security & Design Notes:
 *   • Allows controllers to return a generic error without revealing internal authorization details.
 *   • Contains no sensitive data; safe to log or display a generic message.
 */
package uk.ac.aru.campusevents.exceptions;

/**
 * Indicates that the current user attempted an action without sufficient privileges.
 * Controllers can catch this exception to produce a 403-style response or an
 * equivalent error message in the UI, without exposing internal access rules.
 */
public final class ForbiddenException extends RuntimeException {

    /**
     * Creates a new ForbiddenException with a message for logging or debugging.
     *
     * @param message brief description (safe for logs, not user-facing)
     */
    public ForbiddenException(String message) {
        super(message);
    }
}


