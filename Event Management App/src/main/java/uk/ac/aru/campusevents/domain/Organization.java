/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: Organization.java
 * Purpose:
 *   Domain entity representing a legal or campus organization that can own events.
 * Security & Design Notes:
 *   • Immutable: all fields are final and set at construction.
 *   • Contains no secret or sensitive data.
 *   • Repository assigns id on creation (0 for new instances).
 * Note:
 *   Some accessors may not yet be used directly by services but are retained for domain completeness.
 */
package uk.ac.aru.campusevents.domain;

import java.util.Objects;

/**
 * Immutable representation of an organization that may own one or more events.
 * Typical types include "Society", "NGO", "Company", or "Department".
 */

@SuppressWarnings("unused")
public final class Organization {

    /** Database identifier; 0 indicates unsaved/new organization. */
    private final int id;

    /** Organization display name (required). */
    private final String name;

    /** Organization type label (e.g., "Society", "Company"). */
    private final String type;

    /**
     * Constructs a new organization instance.
     *
     * @param id   database id (0 for new)
     * @param name required, non-null, trimmed
     * @param type optional; defaults to "Society" if null
     * @throws NullPointerException if name is null
     */
    public Organization(int id, String name, String type) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Organization name cannot be empty").trim();
        this.type = (type == null ? "Society" : type).trim();
    }

    /** Factory for creating a new organization with id = 0. */
    public static Organization newOrg(String name, String type) {
        return new Organization(0, name, type);
    }

    /* ---------- Getters ---------- */

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }

    /* ---------- Equality & Hashing ---------- */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Organization other)) return false;

        // If both have IDs, compare by ID only
        if (this.id != 0 && other.id != 0) {
            return this.id == other.id;
        }

        // Otherwise compare by name and type
        return Objects.equals(name, other.name) &&
                Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        if (id != 0) return Integer.hashCode(id);
        return Objects.hash(name, type);
    }

    /* ---------- Debug ---------- */

    @Override
    public String toString() {
        return "Organization{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}

