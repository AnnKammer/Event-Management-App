/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: User.java
 * Purpose:
 *   Domain entity representing a system user with one or more roles.
 * Security & Design Notes:
 *   • All fields are private → encapsulation prevents accidental mutation.
 *   • Password hash is immutable and must be assigned via constructor only.
 *   • Roles stored as an immutable copy to prevent external modification.
 *   • Email kept as plain String; validated at service/validation layer.
 *   • Repository assigns the database ID internally.
 * Note:
 *   Some accessors may not yet be used directly by services but are retained for domain completeness.
 */
package uk.ac.aru.campusevents.domain;

import uk.ac.aru.campusevents.domain.enums.Role;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unused")
public final class User {

    /** Database identifier; assigned by repository. */
    private int id;

    /** Person name (split). */
    private final String firstName;
    private final String lastName;

    /** Email address (validated externally). */
    private final String email;

    /** Hashed password string (e.g., BCrypt). */
    private final String passwordHash;

    /** Immutable set of user roles. */
    private final Set<Role> roles;

    /** Account creation timestamp. */
    private final LocalDateTime createdAt;

    /**
     * Constructs a new user.
     *
     * @param id            database id (0 for new)
     * @param firstName     required, non-null/non-blank
     * @param lastName      required, non-null/non-blank
     * @param email         required, validated externally
     * @param passwordHash  required hashed password (BCrypt)
     * @param roles         roles for this user (may be empty for legacy rows)
     * @param createdAt     timestamp; null → now
     * @throws NullPointerException if required fields are null
     */
    public User(int id,
                String firstName,
                String lastName,
                String email,
                String passwordHash,
                Set<Role> roles,
                LocalDateTime createdAt) {
        this.id = id;
        this.firstName = Objects.requireNonNull(firstName, "firstName cannot be empty").trim();
        this.lastName  = Objects.requireNonNull(lastName,  "lastName cannot be empty").trim();
        this.email = Objects.requireNonNull(email, "email cannot be empty").trim().toLowerCase();
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash cannot be empty");

        // Safe handling of roles: allow empty set for legacy DB rows
        Set<Role> safeRoles;
        if (roles == null || roles.isEmpty()) {
            safeRoles = EnumSet.noneOf(Role.class);
        } else {
            safeRoles = EnumSet.copyOf(roles);
        }
        this.roles = Collections.unmodifiableSet(safeRoles);

        this.createdAt = (createdAt == null ? LocalDateTime.now() : createdAt);
    }

    /** Factory for new users (id assigned later by repository). */
    public static User newUser(String firstName,
                               String lastName,
                               String email,
                               String passwordHash,
                               Set<Role> roles) {
        return new User(0, firstName, lastName, email, passwordHash, roles, LocalDateTime.now());
    }

    /* ---------- Getters ---------- */

    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
    public String getFullName()  { return firstName + " " + lastName; }

    /** Backwards-compat alias for older code that used getName(). */
    @Deprecated
    public String getName() { return getFullName(); }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Set<Role> getRoles() { return roles; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /* ---------- Internal Repository Accessor ---------- */

    /** Package-private setter for repository use only. */
    void setIdInternal(int id) { this.id = id; }

    /* ---------- Equality & Hashing ---------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;

        // Compare by ID if both assigned
        if (this.id != 0 && other.id != 0) {
            return this.id == other.id;
        }

        // Otherwise compare core identity fields (email is unique)
        return Objects.equals(email, other.email)
                && Objects.equals(firstName, other.firstName)
                && Objects.equals(lastName, other.lastName)
                && Objects.equals(roles, other.roles);
    }

    @Override
    public int hashCode() {
        if (id != 0) return Integer.hashCode(id);
        return Objects.hash(email, firstName, lastName, roles);
    }

    /* ---------- Debug ---------- */

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                ", createdAt=" + createdAt +
                '}';
    }
}
