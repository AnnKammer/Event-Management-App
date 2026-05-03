/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: OrganizerEventDetailsController.java
 *
 * Purpose:
 *   JavaFX controller for the window that displays detailed event information
 *   to an event organizer. From here, organizers can:
 *     • View key metadata about their event
 *     • See live registration statistics (registered, approved, waitlisted)
 *     • See feedback summary (average rating + list)
 *     • Export the attendee list as a CSV file
 *     • Close the window when finished
 */

package uk.ac.aru.campusevents.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.dto.RegistrationStats;
import uk.ac.aru.campusevents.ui.GuiContext;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class OrganizerEventDetailsController {

    // -------------------------------------------------------------------------
    // FXML-bound labels displaying event metadata
    // -------------------------------------------------------------------------

    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label dateLabel;
    @FXML private Label locationLabel;
    @FXML private Label capacityLabel;
    @FXML private Label organiserLabel;

    // Registration statistics
    @FXML private Label registeredCountLabel;
    @FXML private Label waitlistCountLabel;

    // Feedback summary
    @FXML private Label avgRatingLabel;
    @FXML private Label feedbackCountLabel;
    @FXML private TextArea feedbackListArea;

    // Buttons
    @FXML private Button exportCsvButton;
    @FXML private Button closeButton;

    // The event currently being displayed
    private Event event;

    // Date formatting for display
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Called by the parent controller after constructing this details window.
     * Sets the event being inspected and populates both metadata and statistics.
     *
     * @param event the event to display (must not be null)
     */
    public void setEvent(Event event) {
        this.event = Objects.requireNonNull(event, "event");
        populateEventInfo();
        loadStats();
        loadFeedbackSummary();
    }

    /**
     * Populates all UI labels with the event's metadata (title, status, date,
     * location, capacity, and organizer identity).
     */
    private void populateEventInfo() {
        if (event == null) return;

        titleLabel.setText(ns(event.getTitle()));
        statusLabel.setText(event.getStatus() == null ? "" : event.getStatus().name());

        if (event.getStartDateTime() != null) {
            dateLabel.setText(event.getStartDateTime().format(DATE_FMT));
        } else {
            dateLabel.setText("");
        }

        locationLabel.setText(ns(event.getLocation()));
        capacityLabel.setText(String.valueOf(event.getCapacity()));

        if (event.getOrganizerUserId() != null) {
            organiserLabel.setText("Organizer #" + event.getOrganizerUserId());
        } else if (event.getOrganizationId() != null) {
            organiserLabel.setText("Organization #" + event.getOrganizationId());
        } else {
            organiserLabel.setText("Unknown");
        }
    }

    /**
     * Loads aggregated registration statistics for the event and displays them.
     * Only "registered/approved" and "waitlisted" counts are shown in this view.
     */
    private void loadStats() {
        if (event == null) return;

        RegistrationStats stats =
                GuiContext.registrationService().getEventStats(event.getId());

        registeredCountLabel.setText(String.valueOf(stats.registeredOrApproved()));
        waitlistCountLabel.setText(String.valueOf(stats.waitlisted()));
    }

    /**
     * Loads feedback summary (average rating + a simple list of entries)
     * and fills the feedback area.
     */
    private void loadFeedbackSummary() {
        if (event == null) return;

        var feedbackService = GuiContext.feedbackService();

        // Average rating
        var avgOpt = feedbackService.averageRating(event.getId());
        if (avgOpt.isPresent()) {
            double avg = avgOpt.get();
            avgRatingLabel.setText("Average rating: " + String.format("%.2f", avg));
        } else {
            avgRatingLabel.setText("Average rating: N/A");
        }

        // Individual feedbacks
        var feedbackList = feedbackService.listForEvent(event.getId());
        feedbackCountLabel.setText("Total feedback: " + feedbackList.size());

        if (feedbackListArea == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        feedbackList.forEach(fb -> {
            // Keep it generic to avoid mismatched getters; fb.toString() is safe.
            sb.append(fb.toString())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        });

        feedbackListArea.setText(sb.toString());
    }

    // -------------------------------------------------------------------------
    // CSV Export
    // -------------------------------------------------------------------------

    /**
     * Exports the attendee list for this event as a CSV file. Opens a file
     * chooser and uses CsvExportService to generate the data.
     */
    @FXML
    private void handleExportCsv() {
        if (event == null) {
            showError("No event selected.");
            return;
        }

        String csv;
        try {
            csv = GuiContext.csvExportService().exportAttendeesCsv(event.getId());
            if (csv == null || csv.isBlank()) {
                showInfo("There are no registrations to export for this event.");
                return;
            }
        } catch (Exception ex) {
            showError("Could not generate CSV: " + ex.getMessage());
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export registrations CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv")
        );

        String safeTitle = ns(event.getTitle()).isBlank() ? "event" : ns(event.getTitle());
        chooser.setInitialFileName(
                safeTitle.replaceAll("\\s+", "_").toLowerCase() + "_registrations.csv"
        );

        Stage stage = (Stage) exportCsvButton.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return; // user cancelled

        try {
            Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);
            showInfo("Export saved to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            showError("Failed to save file: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Window controls
    // -------------------------------------------------------------------------

    @FXML
    private void handleClose() {
        Stage s = (Stage) closeButton.getScene().getWindow();
        s.close();
    }

    // -------------------------------------------------------------------------
    // Helper utilities
    // -------------------------------------------------------------------------

    private static String ns(String s) {
        return s == null ? "" : s;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.setTitle("Event Details");
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText(null);
        a.setTitle("Event Details");
        a.showAndWait();
    }
}
