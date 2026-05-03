/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: EventCategory.java
 * Purpose:
 *   Canonical list of allowed event categories within the system.
 *   Mirrors the PostgreSQL enum type `event_category` for consistency.
 *
 * Design Notes:
 *   • Provides strong typing in the domain model.
 *   • Prevents free-text drift and invalid category values.
 *   • fromString() ensures safe parsing of user/UI inputs by normalising
 *     case and falling back to OTHER when invalid.
 *
 * Security Notes:
 *   • Limits category values to a fixed controlled set to prevent malformed
 *     or unexpected inputs from reaching the database.
 *   • Reduces injection risk by enforcing strict mapping rules.
 */
package uk.ac.aru.campusevents.domain.enums;

public enum EventCategory {
    WORKSHOP,
    LECTURE,
    SOCIAL,
    SPORTS,
    ENVIRONMENT,
    ACADEMIC,
    OTHER;

    /**
     * Safely parses a string into an EventCategory.
     * Normalises to uppercase and defaults to OTHER if the input
     * is null or does not match any known category.
     *
     * @param value category string to parse
     * @return matching EventCategory, or OTHER as fallback
     */
    public static EventCategory fromString(String value) {
        if (value == null) return OTHER;
        try {
            return EventCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return OTHER;
        }
    }
}

