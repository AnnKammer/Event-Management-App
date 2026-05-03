/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AuthService.java
 * Purpose:
 *   Defines authentication use-cases (user registration and login).
 * Security & Design Notes:
 *   • Accepts raw passwords as char[] to limit heap retention.
 *   • Hashing and verification are delegated to PasswordHasher (BCrypt).
 *   • Never returns plaintext passwords or hashes; only a safe UserSession snapshot.
 *   • Email handling is case-insensitive and trimmed at the service boundary.
 */
package uk.ac.aru.campusevents.service;

import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.dto.UserSession;

import java.util.Set;

/**
 * Service boundary for user authentication flows.
 * Implementations are responsible for:
 *   • Validating input (non-empty name/email/password, at least one role).
 *   • Normalizing email (trim + lowercase) and enforcing uniqueness on register.
 *   • Hashing passwords on register; verifying hashes on login (BCrypt).
 *   • Returning a minimal, non-sensitive {@link UserSession} on successful login.
 * Typical failure signals:
 *   {@code IllegalArgumentException} — invalid input (blank email/password, etc.).
 *   {@code IllegalStateException} — email already exists on register.
 *   {@code SecurityException} — invalid credentials on login.
 */
@SuppressWarnings("unused")
public interface AuthService {
    /**
     * Register a new user (BCrypt-hashed password).
     * @param firstName required, non-blank
     * @param lastName  required, non-blank
     * @param email     required, canonicalized to lowercase
     * @param rawPassword min length enforced; wiped after hashing
     * @param roles     at least one role required
     * @return assigned user id
     */
    int registerUser(String firstName, String lastName, String email, char[] rawPassword, Set<Role> roles);

    /**
     * Authenticate and return a non-sensitive session snapshot.
     * @throws SecurityException on invalid credentials
     */
    UserSession login(String email, char[] rawPassword);


}

