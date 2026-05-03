/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: CsvExportService.java
 * Purpose:
 *   Defines the public API for exporting attendee and participation data as CSV.
 * Security & Design Notes:
 *   • Returns only sanitized CSV text; caller decides where to store or transmit it.
 *   • Exports only confirmed (REGISTERED/APPROVED) attendees — no waitlist or sensitive PII.
 *   • No credentials, hashes, or internal identifiers are exposed.
 *   • Output encoding is UTF-8; all CSV fields must be properly escaped to prevent injection.
 */
package uk.ac.aru.campusevents.service;

/**
 * Service interface for generating CSV exports of event participation data.
 * Implementations may join data from multiple repositories (Event, Registration, User)
 * to produce sanitized, formula-safe CSV text suitable for download or reporting.
 */
@SuppressWarnings("unused")
public interface CsvExportService {

    /**
     * Exports a CSV list of confirmed attendees for a specific event.
     * Columns (UTF-8, comma-separated):
     * {@code event_id, event_title, student_id, student_name, student_email}
     *
     * @param eventId the event identifier
     * @return a CSV-formatted string representing confirmed/approved attendees
     * @throws IllegalArgumentException if the event does not exist
     */
    String exportAttendeesCsv(int eventId);

    /**
     * Exports a CSV list of events the specified student is registered for.
     * Columns (UTF-8, comma-separated):
     * {@code event_id, event_title, start, end, location, status}
     *
     * @param studentId the student identifier
     * @return a CSV-formatted string representing the student's registered events
     */
    String exportMyEventsCsv(int studentId);
}


