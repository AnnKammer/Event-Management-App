/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: PasswordHasher.java
 * Purpose:
 *   Centralized password hashing and verification utility using BCrypt.
 * Security & Design Notes:
 *   • BCrypt provides per-hash random salts and adaptive cost factor.
 *   • Hash format embeds version, cost, and salt; no plaintext passwords stored or logged.
 *   • Stateless, single-responsibility utility (easy to audit and test).
 */
package uk.ac.aru.campusevents.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility class providing secure password hashing and verification
 * using the BCrypt algorithm. Designed for use in authentication
 * and registration workflows.
 */
@SuppressWarnings("unused")
public final class PasswordHasher {

    /** Adaptive work factor (cost). Increase for production environments. */
    private static final int COST = 12;

    /** Private constructor to prevent instantiation. */
    private PasswordHasher() {}

    /**
     * Hashes a plaintext password using BCrypt.
     * BCrypt automatically generates a random salt internally and embeds it
     * along with the cost factor in the returned hash string.
     *
     * @param rawPassword the plaintext password (char[] for memory safety)
     * @return the BCrypt-formatted hash string
     */
    public static String hash(char[] rawPassword) {
        if (rawPassword == null || rawPassword.length == 0)
            throw new IllegalArgumentException("rawPassword cannot be empty");
        return BCrypt.withDefaults().hashToString(COST, rawPassword);
    }

    /**
     * Verifies a plaintext password against a stored BCrypt hash.
     *
     * @param rawPassword the plaintext password attempt
     * @param storedHash  the previously stored BCrypt hash
     * @return {@code true} if the password matches; otherwise {@code false}
     */
    public static boolean verify(char[] rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null)
            return false;
        return BCrypt.verifyer().verify(rawPassword, storedHash).verified;
    }
}


