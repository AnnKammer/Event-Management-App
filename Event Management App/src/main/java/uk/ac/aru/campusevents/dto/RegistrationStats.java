/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: RegistrationStats.java
 * Purpose:
 *   Immutable DTO representing aggregate registration statistics for a single event.
 */
package uk.ac.aru.campusevents.dto;

public record RegistrationStats(
        int registeredOrApproved,
        int waitlisted,
        int cancelled
) {}

