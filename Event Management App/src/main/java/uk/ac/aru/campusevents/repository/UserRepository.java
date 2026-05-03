/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: UserRepository.java
 * Purpose:
 *   Defines the persistence port (DAO) for storing and retrieving User entities.
 * Security & Design Notes:
 *   • Methods never expose plaintext passwords; only hashed values within full {@link User}.
 *   • Services control exposure of sensitive fields and enforce RBAC/validation.
 *   • All finders return Optional or List to prevent null-related errors.
 *   • Safe to implement using in-memory storage or database-backed persistence (e.g., JDBC/JPA).
 */
package uk.ac.aru.campusevents.repository;

import uk.ac.aru.campusevents.domain.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link User} entities.
 * Provides lookup and bulk-fetch operations for authentication,
 * authorization, and user management features.
 * Password hashing and verification are handled by the service layer.
 */
@SuppressWarnings("unused")
public interface UserRepository {

    /**
     * Persists a new user and returns the generated identifier.
     * Passwords must already be hashed before creation.
     *
     * @param user the user to create (must satisfy domain validation)
     * @return the generated database identifier
     */
    int create(User user);

    /**
     * Finds a user by their unique email address.
     * Used primarily for authentication (login) and duplicate-check validation.
     *
     * @param email the email to search for (case-insensitive)
     * @return an {@link Optional} containing the user if found, or empty if not
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their unique identifier.
     *
     * @param id user ID
     * @return an {@link Optional} containing the user if found, or empty if not
     */
    Optional<User> findById(int id);

    /**
     * Retrieves multiple users by their identifiers.
     * Used by exports, registration lookups, and event-related operations.
     *
     * @param ids a collection of user identifiers
     * @return a list of matching users (possibly empty)
     */
    List<User> findAllByIds(Collection<Integer> ids);
}


