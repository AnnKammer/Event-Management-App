/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: EventDetailsController.java
 * Purpose:
 *   JavaFX controller for the student-facing Event Details pop-up.
 *
 * Responsibilities:
 *   • Display selected event metadata (title, organizer, date, location, status)
 *   • Show the student's current registration status
 *   • Allow the student to register or cancel registration
 *   • Allow the student to submit feedback after attending the event
 *   • Display the average rating for the event
 *   • Close the details window on request
 *
 * Why this exists:
 *   Students need a focused screen to understand an event and manage
 *   their registration and feedback. The heavy lifting is delegated to the
 *   RegistrationService, FeedbackService, and EventService; this controller
 *   only handles presentation and button actions.
 */

package uk.ac.aru.campusevents.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.EventStatus;
import uk.ac.aru.campusevents.ui.EventStatusHelper;
import uk.ac.aru.campusevents.ui.GuiContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public final class EventDetailsController {

    // -------------------------------------------------------------------------
    // FXML: Event metadata + registration controls
    // -------------------------------------------------------------------------

    @FXML private Label nameLabel;
    @FXML private Label organizerLabel;
    @FXML private Label dateLabel;
    @FXML private Label locationLabel;
    @FXML private Label statusLabel;

    @FXML private Button registerButton;
    @FXML private Button unregisterButton;

    // -------------------------------------------------------------------------
    // FXML: Feedback controls
    // -------------------------------------------------------------------------

    @FXML private Label averageRatingLabel;
    @FXML private Label feedbackInfoLabel;
    @FXML private ComboBox<Integer> feedbackRatingBox;
    @FXML private TextArea feedbackCommentArea;
    @FXML private Button submitFeedbackButton;

    // -------------------------------------------------------------------------
    // Internal state
    // -------------------------------------------------------------------------

    /** The event currently displayed in this dialog. */
    private Event event;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * JavaFX lifecycle hook.
     * Called automatically after FXML has been loaded and fields injected.
     * Initializes the feedback rating combo box.
     */
    @FXML
    private void initialize() {
        if (feedbackRatingBox != null) {
            feedbackRatingBox.getItems().setAll(1, 2, 3, 4, 5);
        }
    }

    /**
     * Called by the StudentDashboardController (or HomePageController)
     * after FXML load.
     *
     * @param event the event to display
     */
    public void setEvent(Event event) {
        this.event = Objects.requireNonNull(event, "event must not be null");
        populateFields();
        refreshButtons();
        loadAverageRating();
        updateFeedbackAvailability();
    }

    // -------------------------------------------------------------------------
    // UI population
    // -------------------------------------------------------------------------

    private void populateFields() {
        if (event == null) {
            showError("No event was provided to display.");
            return;
        }

        nameLabel.setText(event.getTitle());

        String organizerText;
        if (event.getOrganizerUserId() != null) {
            organizerText = "Organizer #" + event.getOrganizerUserId();
        } else if (event.getOrganizationId() != null) {
            organizerText = "Organization #" + event.getOrganizationId();
        } else {
            organizerText = "Unknown organizer";
        }
        organizerLabel.setText(organizerText);

        if (event.getStartDateTime() != null) {
            dateLabel.setText(event.getStartDateTime().format(DATE_FMT));
        } else {
            dateLabel.setText("No start date set");
        }

        String location = event.getLocation();
        locationLabel.setText((location == null || location.isBlank())
                ? "No location set"
                : location);

        // Show "runtime" status (auto COMPLETED after end time, etc.)
        EventStatus effective = EventStatusHelper.computeRuntimeStatus(event);
        statusLabel.setText(effective == null ? "UNKNOWN" : effective.name());
    }

    /**
     * Updates which buttons are enabled based on the student's registration status
     * and whether registration is still open for this event.
     * Also updates the status label with a per-student note.
     */
    private void refreshButtons() {
        if (event == null) {
            registerButton.setDisable(true);
            unregisterButton.setDisable(true);
            return;
        }

        var session = GuiContext.getCurrentUser();
        if (session == null) {
            registerButton.setDisable(true);
            unregisterButton.setDisable(true);
            return;
        }

        int studentId = session.userId();

        // Base status text from the "runtime" status helper
        EventStatus effective = EventStatusHelper.computeRuntimeStatus(event);
        String baseStatus     = (effective == null) ? "UNKNOWN" : effective.name();

        var myEvents = GuiContext.registrationService().listMyEvents(studentId);
        boolean registeredForThisEvent = myEvents.stream()
                .anyMatch(e -> e.getId() == event.getId());

        // Is registration allowed at all (day-before rule + status OPEN)?
        boolean registrationOpen = EventStatusHelper.isRegistrationOpen(event);

        if (!registeredForThisEvent) {
            // Not registered at all
            unregisterButton.setDisable(true);

            if (registrationOpen) {
                // Can still register
                registerButton.setDisable(false);
                statusLabel.setText(baseStatus + " — You are not registered");
            } else {
                // Too late to register
                registerButton.setDisable(true);
                statusLabel.setText(baseStatus + " — Registration is closed");
            }
            return;
        }

        // Student has some active registration; check if confirmed attendee or waitlisted.
        List<Integer> attendees =
                GuiContext.registrationService().listAttendeeUserIds(event.getId());
        boolean isConfirmed = attendees.contains(studentId);

        // If confirmed or waitlisted, allow cancel; no more registering.
        registerButton.setDisable(true);
        unregisterButton.setDisable(false);

        if (isConfirmed) {
            statusLabel.setText(baseStatus + " — You are REGISTERED");
        } else {
            statusLabel.setText(baseStatus + " — You are on the WAITLIST");
        }
    }

    // -------------------------------------------------------------------------
    // Feedback helpers
    // -------------------------------------------------------------------------

    /**
     * Loads the average rating for this event and updates the average rating label.
     * If no feedback exists, displays "N/A".
     */
    private void loadAverageRating() {
        if (averageRatingLabel == null || event == null) {
            return;
        }

        var avgOpt = GuiContext.feedbackService().averageRating(event.getId());
        if (avgOpt.isPresent()) {
            double value = avgOpt.get();
            averageRatingLabel.setText("Average rating: " + value);
        } else {
            averageRatingLabel.setText("Average rating: N/A");
        }
    }

    /**
     * Enables or disables the feedback form depending on:
     *   • whether the event has passed,
     *   • whether the current user is a confirmed attendee (REGISTERED/APPROVED),
     *   • whether the current user has *already submitted* feedback.
     *
     * The service layer also enforces the same rules, so this method is a
     * user-friendly guard rather than a security boundary.
     */
    private void updateFeedbackAvailability() {
        if (event == null || feedbackRatingBox == null || submitFeedbackButton == null) {
            return;
        }

        var session = GuiContext.getCurrentUser();
        if (session == null) {
            disableFeedback("You must be logged in to submit feedback.");
            return;
        }

        int studentId = session.userId();

        // Determine if event has passed (use endDateTime if present, otherwise startDateTime)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = event.getEndDateTime() != null
                ? event.getEndDateTime()
                : event.getStartDateTime();

        boolean eventHasPassed = cutoff != null && !cutoff.isAfter(now);

        if (!eventHasPassed) {
            disableFeedback("Feedback will be available after the event has taken place.");
            return;
        }

        // Check if the current user is a confirmed attendee (REGISTERED/APPROVED)
        var attendees = GuiContext.registrationService().listAttendeeUserIds(event.getId());
        boolean isConfirmedAttendee = attendees.contains(studentId);

        if (!isConfirmedAttendee) {
            disableFeedback("Only confirmed attendees can submit feedback for this event.");
            return;
        }

        // NEW: Check if this student has ALREADY submitted feedback
        boolean alreadySubmitted =
                GuiContext.feedbackService().hasFeedback(event.getId(), studentId);

        if (alreadySubmitted) {
            disableFeedback("You have already submitted feedback for this event.");
            return;
        }

        // All checks passed: enable the feedback form
        feedbackRatingBox.setDisable(false);
        if (feedbackCommentArea != null) {
            feedbackCommentArea.setDisable(false);
        }
        submitFeedbackButton.setDisable(false);

        if (feedbackInfoLabel != null) {
            feedbackInfoLabel.setText("");
        }
    }

    private void disableFeedback(String reason) {
        feedbackRatingBox.setDisable(true);
        if (feedbackCommentArea != null) {
            feedbackCommentArea.setDisable(true);
        }
        submitFeedbackButton.setDisable(true);
        if (feedbackInfoLabel != null) {
            feedbackInfoLabel.setText(reason);
        }
    }

    // -------------------------------------------------------------------------
    // Button handlers — registration
    // -------------------------------------------------------------------------

    @FXML
    private void handleRegister(ActionEvent actionEvent) {
        if (event == null) return;

        var session = GuiContext.getCurrentUser();
        if (session == null) {
            showError("You must be logged in to register.");
            return;
        }

        int studentId = session.userId();

        try {
            GuiContext.registrationService().register(studentId, event.getId());
            showInfo("You have been registered (or waitlisted) for this event.");
            refreshButtons();
            updateFeedbackAvailability();
        } catch (Exception ex) {
            // Friendly UI message for date rule
            String msg = ex.getMessage();

            if (msg != null && msg.contains("Registration is closed")) {
                showError("Registration for this event is closed. You must register at least 1 day before the event starts.");
            } else {
                showError("Could not register: " + msg);
            }
        }
    }

    @FXML
    private void handleUnregister(ActionEvent actionEvent) {
        if (event == null) return;

        var session = GuiContext.getCurrentUser();
        if (session == null) {
            showError("You must be logged in to cancel your registration.");
            return;
        }

        int studentId = session.userId();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you really want to cancel your registration?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm cancellation");
        confirm.setHeaderText(null);
        confirm.showAndWait();

        if (confirm.getResult() != ButtonType.YES) {
            return;
        }

        try {
            GuiContext.registrationService().cancel(studentId, event.getId());
            showInfo("Your registration has been cancelled.");
            refreshButtons();
            updateFeedbackAvailability();
        } catch (Exception ex) {
            showError("Could not cancel registration: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Button handler — feedback submission
    // -------------------------------------------------------------------------

    /**
     * Handles the "Submit Feedback" action for the current event.
     * Delegates to FeedbackService.submit(...) and displays a user-friendly
     * alert on success or failure. All business rules (STUDENT role,
     * attendance, one-per-user, rating bounds, event has passed) are enforced
     * in the service layer.
     */
    @FXML
    private void handleSubmitFeedback() {
        if (event == null || feedbackRatingBox == null) {
            return;
        }

        Integer rating = feedbackRatingBox.getValue();
        if (rating == null) {
            showError("Please select a rating between 1 and 5.");
            return;
        }

        var session = GuiContext.getCurrentUser();
        if (session == null) {
            showError("You must be logged in as a student to submit feedback.");
            return;
        }

        int studentId = session.userId();
        String comment = (feedbackCommentArea == null)
                ? ""
                : feedbackCommentArea.getText();

        try {
            GuiContext.feedbackService().submit(studentId, event.getId(), rating, comment);

            showInfo("Thank you, your feedback has been submitted.");

            // Optional: clear controls
            feedbackRatingBox.setValue(null);
            if (feedbackCommentArea != null) {
                feedbackCommentArea.clear();
            }

            // Refresh average rating + availability (now greys out)
            loadAverageRating();
            updateFeedbackAvailability();

        } catch (Exception ex) {
            showError("Could not submit feedback: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Button handler — close window
    // -------------------------------------------------------------------------

    @FXML
    private void closeWindow(ActionEvent actionEvent) {
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        stage.close();
    }

    // -------------------------------------------------------------------------
    // Alert helpers
    // -------------------------------------------------------------------------

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
