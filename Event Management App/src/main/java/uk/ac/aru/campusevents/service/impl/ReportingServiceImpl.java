/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: ReportingServiceImpl.java
 * Purpose:
 *   Provides textual summaries for events and organizer portfolios.
 *   Used for reporting and analytics features.
 * Security notes:
 *   • Read-only: no data mutation occurs here.
 *   • No PII: only event titles, IDs, and aggregate feedback/registration stats.
 *   • Does not expose internal repository identifiers.
 */
package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.RegStatus;
import uk.ac.aru.campusevents.repository.EventRepository;
import uk.ac.aru.campusevents.repository.FeedbackRepository;
import uk.ac.aru.campusevents.repository.RegistrationRepository;
import uk.ac.aru.campusevents.service.ReportingService;

import java.util.Objects;

public final class ReportingServiceImpl implements ReportingService {
    private final EventRepository eventRepo;
    private final RegistrationRepository regRepo;
    private final FeedbackRepository feedbackRepo;

    public ReportingServiceImpl(EventRepository eventRepo,
                                RegistrationRepository regRepo,
                                FeedbackRepository feedbackRepo) {
        this.eventRepo = Objects.requireNonNull(eventRepo);
        this.regRepo = Objects.requireNonNull(regRepo);
        this.feedbackRepo = Objects.requireNonNull(feedbackRepo);
    }

    /**
     * Generates a single-event textual summary:
     *  - Counts confirmed and cancelled registrations.
     *  - Calculates average rating and number of feedback entries.
     */
    @Override
    public String eventSummary(int eventId) {
        Event e = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        var regs = regRepo.findByEvent(eventId);
        long confirmed = regs.stream()
                .filter(r -> r.getStatus() == RegStatus.REGISTERED || r.getStatus() == RegStatus.APPROVED)
                .count();

        long cancelled = regRepo.findAllByEvent(eventId).stream()
                .filter(r -> r.getStatus() == RegStatus.CANCELLED)
                .count();

        var feedback = feedbackRepo.findByEvent(eventId);
        double avg = feedback.isEmpty()
                ? 0
                : feedback.stream().mapToInt(f -> f.getRating()).average().orElse(0);

        String titleLine = "Event #" + e.getId() + " – " + e.getTitle();
        return titleLine + System.lineSeparator() +
                "Confirmed: " + confirmed + ", Cancelled: " + cancelled + System.lineSeparator() +
                "Feedback count: " + feedback.size() +
                ", Average rating: " + String.format("%.2f", avg);
    }

    /**
     * Lists all events created by the given organizer.
     * Useful for dashboards and administrative reports.
     */
    @Override
    public String organizerPortfolio(int organizerUserId) {
        var events = eventRepo.findByOrganizer(organizerUserId);
        StringBuilder sb = new StringBuilder(
                "Organizer portfolio for userId=" + organizerUserId + System.lineSeparator());
        for (var e : events) {
            sb.append("- ").append(e.getTitle())
                    .append(" (#").append(e.getId()).append(")")
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }
}

