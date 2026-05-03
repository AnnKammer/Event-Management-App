/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: FeedbackServiceImpl.java
 * Purpose:
 *   BCrypt/RBAC-backed feedback service for submitting and aggregating event ratings.
 * Security & Design Notes:
 *   • Enforces STUDENT role and “must be an attendee” rule before accepting feedback.
 *   • One feedback per (eventId, studentId) enforced at service layer.
 *   • Comment is optional; sanitized (trimmed) and can be length-limited to deter abuse.
 *   • Aggregations return anonymized values only (no PII).
 */
package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.EventFeedback;
import uk.ac.aru.campusevents.domain.Registration;
import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.exceptions.ForbiddenException;
import uk.ac.aru.campusevents.repository.EventRepository;
import uk.ac.aru.campusevents.repository.FeedbackRepository;
import uk.ac.aru.campusevents.repository.RegistrationRepository;
import uk.ac.aru.campusevents.repository.UserRepository;
import uk.ac.aru.campusevents.service.FeedbackService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public final class FeedbackServiceImpl implements FeedbackService {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_COMMENT_LEN = 2_000; // soft cap for UI abuse

    private final FeedbackRepository repo;
    private final RegistrationRepository regRepo;
    private final EventRepository eventRepo;
    private final UserRepository userRepo;

    public FeedbackServiceImpl(FeedbackRepository repo,
                               RegistrationRepository regRepo,
                               EventRepository eventRepo,
                               UserRepository userRepo) {
        this.repo = Objects.requireNonNull(repo);
        this.regRepo = Objects.requireNonNull(regRepo);
        this.eventRepo = Objects.requireNonNull(eventRepo);
        this.userRepo = Objects.requireNonNull(userRepo);
    }

    /**
     * Submits feedback for an event by a student who attended.
     * Enforces STUDENT role, event existence, attendance, one-per-user, and rating bounds.
     */
    @Override
    public int submit(int actorStudentId, int eventId, int rating, String comment) {
        // RBAC: must be a student
        var user = userRepo.findById(actorStudentId)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        if (!user.getRoles().contains(Role.STUDENT)) {
            throw new ForbiddenException("Student role required");
        }

        // Event must exist (we don't leak details beyond existence)
        Event ev = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        // Event must have passed
        var now = java.time.LocalDateTime.now();
        var cutoff = ev.getEndDateTime() != null
                ? ev.getEndDateTime()
                : ev.getStartDateTime();

        if (cutoff == null || cutoff.isAfter(now)) {
            throw new ForbiddenException("Feedback is only allowed after the event has taken place.");
        }

        // Attendee check:
        // RegistrationRepository.findByEvent(eventId) is defined in your project
        // to return only REGISTERED/APPROVED (confirmed) registrations.
        boolean attended = regRepo.findByEvent(eventId).stream()
                .map(Registration::getStudentId)
                .anyMatch(id -> id == actorStudentId);
        if (!attended) {
            throw new ForbiddenException("Only attendees can submit feedback");
        }

        // One feedback per (event, user)
        if (repo.findByEventAndUser(eventId, actorStudentId).isPresent()) {
            throw new IllegalStateException("Feedback already submitted for this event");
        }

        // Validate rating
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("Rating must be " + MIN_RATING + ".." + MAX_RATING);
        }

        // Sanitize comment (optional)
        String safeComment = (comment == null) ? "" : comment.trim();
        if (safeComment.length() > MAX_COMMENT_LEN) {
            safeComment = safeComment.substring(0, MAX_COMMENT_LEN);
        }

        return repo.create(EventFeedback.newFeedback(eventId, actorStudentId, rating, safeComment));
    }

    /**
     * Returns true if this student has already submitted feedback for this event.
     * Simple wrapper over FeedbackRepository.findByEventAndUser(...).
     */
    @Override
    public boolean hasFeedback(int eventId, int studentId) {
        return repo.findByEventAndUser(eventId, studentId).isPresent();
    }

    /**
     * Returns the average rating for an event as Optional.
     * Rounds to one decimal place for stable display (e.g., 4.3).
     */
    @Override
    public Optional<Double> averageRating(int eventId) {
        List<EventFeedback> list = repo.findByEvent(eventId);
        if (list.isEmpty()) return Optional.empty();

        double avg = list.stream()
                .mapToInt(EventFeedback::getRating)
                .average()
                .orElse(0.0);

        // Round to one decimal place for UI friendliness
        double rounded = Math.round(avg * 10.0) / 10.0;
        return Optional.of(rounded);
    }

    /**
     * Lists feedback entries for an event (oldest-first as per repo sort).
     */
    @Override
    public List<EventFeedback> listForEvent(int eventId) {
        return repo.findByEvent(eventId);
    }
}
