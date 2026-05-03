/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: EventStatusHelper.java
 * Purpose:
 *   Helper utilities for computing "runtime" event status and
 *   registration availability based on dates.
 *
 * Design Notes:
 *   • Does NOT modify the stored Event.status; it only derives
 *     a view of the status for display / validation.
 *   • Keeps all date logic in one place so UI and services can
 *     share the same rule set.
 */
package uk.ac.aru.campusevents.ui;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.EventStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class EventStatusHelper {

    private EventStatusHelper() { }

    /**
     * Computes an "effective" status for display:
     *   • If the event has ended → COMPLETED
     *   • Otherwise → stored status
     *
     * This does not write anything back to the database.
     */
    public static EventStatus computeRuntimeStatus(Event event) {
        if (event == null) return null;

        EventStatus stored = event.getStatus();
        LocalDateTime now  = LocalDateTime.now();

        // If we have an end time and it's in the past → treat as COMPLETED
        LocalDateTime end = event.getEndDateTime();
        if (end != null && end.isBefore(now)) {
            return EventStatus.COMPLETED;
        }

        return stored;
    }

    /**
     * Returns true if registration is allowed for this event *right now*.
     *
     * Rules:
     *   • Event must be OPEN (stored status) – CANCELLED / ARCHIVED / FULL are blocked.
     *   • startDate must be strictly after today (you can only register
     *     up to the day BEFORE the event).
     *   • If the event has no startDate, this method returns false
     *     (defensive choice).
     */
    public static boolean isRegistrationOpen(Event event) {
        if (event == null) return false;

        EventStatus stored = event.getStatus();
        if (stored == null ||
                stored == EventStatus.CANCELLED ||
                stored == EventStatus.ARCHIVED ||
                stored == EventStatus.FULL) {
            return false;
        }

        LocalDateTime start = event.getStartDateTime();
        if (start == null) {
            // No date → be strict and disallow registration.
            return false;
        }

        LocalDate today    = LocalDate.now();
        LocalDate eventDay = start.toLocalDate();

        // Must be at least one full day in the future
        return eventDay.isAfter(today);
    }
}

