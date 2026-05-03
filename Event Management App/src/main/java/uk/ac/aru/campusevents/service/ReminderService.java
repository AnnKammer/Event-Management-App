/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: ReminderService.java
 * Purpose: Reminders for post-event actions (e.g., feedback prompts).
 * Security notes:
 *   • No PII beyond user ids in notifications.
 *   • Idempotent: safe to run multiple times; skips users who already submitted feedback.
 */
package uk.ac.aru.campusevents.service;

public interface ReminderService {
    /**
     * Sends feedback reminders to all attendees (REGISTERED/APPROVED)
     * of the given event who have not yet submitted feedback.
     */
    void sendFeedbackRemindersForEvent(int eventId);
}

