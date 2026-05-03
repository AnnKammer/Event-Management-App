/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: ReminderServiceImpl.java
 * Purpose:
 *   Send post-event feedback reminders to attendees who haven't rated yet.
 * Security & Design Notes:
 *   • Reads attendee list from RegistrationRepository (REGISTERED/APPROVED).
 *   • Checks FeedbackRepository to avoid duplicate reminders.
 *   • Uses NotificationService to deliver in-app messages.
 *   • Message includes token [FEEDBACK:<eventId>] for GUI deep-linking.
 */
package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.Registration;
import uk.ac.aru.campusevents.repository.EventRepository;
import uk.ac.aru.campusevents.repository.FeedbackRepository;
import uk.ac.aru.campusevents.repository.RegistrationRepository;
import uk.ac.aru.campusevents.service.NotificationService;
import uk.ac.aru.campusevents.service.ReminderService;

import java.util.Objects;

public final class ReminderServiceImpl implements ReminderService {
    private final EventRepository eventRepo;
    private final RegistrationRepository regRepo;
    private final FeedbackRepository feedbackRepo;
    private final NotificationService notifications;

    public ReminderServiceImpl(EventRepository eventRepo,
                               RegistrationRepository regRepo,
                               FeedbackRepository feedbackRepo,
                               NotificationService notifications) {
        this.eventRepo = Objects.requireNonNull(eventRepo);
        this.regRepo = Objects.requireNonNull(regRepo);
        this.feedbackRepo = Objects.requireNonNull(feedbackRepo);
        this.notifications = Objects.requireNonNull(notifications);
    }

    @Override
    public void sendFeedbackRemindersForEvent(int eventId) {
        // Validate event exists
        Event ev = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        // All confirmed attendees (REGISTERED/APPROVED)
        var attendees = regRepo.findByEvent(eventId).stream()
                .map(Registration::getStudentId)
                .distinct()
                .toList();

        // Notify only users who have NOT submitted feedback yet
        for (int userId : attendees) {
            boolean alreadyRated = feedbackRepo.findByEventAndUser(eventId, userId).isPresent();
            if (alreadyRated) continue;

            // GUI hook: [FEEDBACK:<eventId>] token lets the UI deep-link to the feedback form
            String msg = "Reminder: please rate \"" + ev.getTitle() + "\". Open feedback: [FEEDBACK:" + ev.getId() + "]";
            notifications.send(userId, msg);
        }
    }
}

