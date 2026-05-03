/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: ReportingService.java
 * Purpose:
 *   Defines reporting and summary use-cases for events and organizers.
 * Security & Design Notes:
 *   • Provides read-only, aggregated information derived from repositories.
 *   • Returns preformatted text summaries (UTF-8); caller decides how to display/export.
 *   • Contains no PII or sensitive credentials — only public event and organizer metadata.
 *   • Safe for in-memory or database-backed implementations.
 */
package uk.ac.aru.campusevents.service;

/**
 * Service boundary for reporting and high-level data summaries.
 * Implementations aggregate data across repositories to generate
 * concise, human-readable overviews for organizers and administrators.
 */
@SuppressWarnings("unused")
public interface ReportingService {

    /**
     * Generates a text summary for a specific event.
     * The summary may include title, status,
     * number of confirmed attendees, and average rating (if feedback available).
     *
     * @param eventId the event identifier
     * @return a human-readable text summary (UTF-8)
     * @throws IllegalArgumentException if the event does not exist
     */
    String eventSummary(int eventId);

    /**
     * Generates an organizer portfolio summary showing their managed events.
     * May include counts of active/past events, total attendees, and average ratings.
     *
     * @param organizerUserId the organizer’s user identifier
     * @return a formatted textual portfolio report
     * @throws IllegalArgumentException if the organizer does not exist
     */
    String organizerPortfolio(int organizerUserId);
}


