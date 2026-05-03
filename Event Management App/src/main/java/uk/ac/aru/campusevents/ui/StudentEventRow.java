/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: StudentEventRow.java
 * Purpose:
 *   JavaFX ViewModel wrapper used by the Student Dashboard table.
 *   Converts the Event domain entity plus the student's registration status
 *   into simple, UI-friendly string properties.
 *
 * Responsibilities:
 *   • Provide read-only getters (title, category, organizerName, startDate, status)
 *   • Store per-student registration status (Registered / Waitlisted / Not Registered)
 *   • Expose the underlying Event object for actions (Register, Cancel, Details)
 *
 * Why this exists:
 *   TableView requires simple string/primitive getters or JavaFX properties.
 *   The Event domain object cannot be mutated for UI-specific formatting,
 *   so this wrapper isolates presentation formatting from domain logic.
 */
package uk.ac.aru.campusevents.ui;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.EventCategory;
import uk.ac.aru.campusevents.domain.enums.EventStatus;

import java.time.format.DateTimeFormatter;

public final class StudentEventRow {

    private final Event event;
    private final String organizerName;
    private final String registrationStatus; // "Registered", "Waitlisted", or "Not Registered"

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public StudentEventRow(Event event, String organizerName, String registrationStatus) {
        this.event = event;
        this.organizerName = organizerName;
        this.registrationStatus = registrationStatus;
    }

    public Event getEvent() {
        return event;
    }

    public String getTitle() {
        return event.getTitle();
    }

    public String getCategory() {
        EventCategory c = event.getCategory();
        return (c == null) ? "" : c.name();
    }

    public String getOrganizer() {
        return organizerName;
    }

    public String getStartDate() {
        return (event.getStartDateTime() == null)
                ? ""
                : event.getStartDateTime().format(DATE_FMT);
    }

    public String getLocation() {
        return event.getLocation() == null ? "" : event.getLocation();
    }

    public String getStatus() {
        EventStatus s = event.getStatus();
        return (s == null) ? "" : s.name();
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }
}