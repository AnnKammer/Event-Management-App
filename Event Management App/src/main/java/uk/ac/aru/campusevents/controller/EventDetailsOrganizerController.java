/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: EventDetailsOrganizerController.java
 * Purpose:
 *   JavaFX controller for the organizer-facing "Edit Event" screen.
 *
 * Responsibilities:
 *   • Display the selected event's metadata
 *   • Allow the organizer to change title, date, and location
 *   • Persist changes via EventService.updateEvent(...)
 *   • Delete the event via EventService.deleteEvent(...)
 *   • Navigate back by closing the window
 *
 * Why this exists:
 *   Organizers must be able to maintain their events after creation.
 *   This controller uses the existing RBAC-enforced EventService to
 *   ensure only allowed users modify or delete events.
 */
package uk.ac.aru.campusevents.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.EventStatus;
import uk.ac.aru.campusevents.ui.GuiContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class EventDetailsOrganizerController {

    @FXML private TextField nameField;
    @FXML private TextField statusField;
    @FXML private TextField organizerField;
    @FXML private TextField dateField;
    @FXML private TextField locationField;

    private Event event;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Called by the Organizer Dashboard when opening this screen.
     *
     * @param event the event to edit
     */
    public void setEvent(Event event) {
        this.event = event;
        populateFields();
    }

    @FXML
    private void initialize() {
        // Fields will be populated once setEvent(...) is called.
    }

    private void populateFields() {
        if (event == null) {
            showAlert(Alert.AlertType.ERROR, "No event selected", "No event was provided to edit.");
            return;
        }

        nameField.setText(event.getTitle());

        EventStatus status = event.getStatus();
        statusField.setText(status == null ? "" : status.name());
        statusField.setEditable(false); // status changes via business rules, not free text

        String organizerLabel;
        if (event.getOrganizerUserId() != null) {
            organizerLabel = "Organizer #" + event.getOrganizerUserId();
        } else if (event.getOrganizationId() != null) {
            organizerLabel = "Organization #" + event.getOrganizationId();
        } else {
            organizerLabel = "Unknown";
        }
        organizerField.setText(organizerLabel);
        organizerField.setEditable(false);

        if (event.getStartDateTime() != null) {
            dateField.setText(event.getStartDateTime().toLocalDate().format(DATE_FMT));
        } else {
            dateField.setText("");
        }

        locationField.setText(event.getLocation() == null ? "" : event.getLocation());
    }

    @FXML
    private void handleSave(ActionEvent actionEvent) {
        if (event == null) {
            showAlert(Alert.AlertType.ERROR, "No event", "There is no event to save.");
            return;
        }

        String title = nameField.getText() == null ? "" : nameField.getText().trim();
        String location = locationField.getText() == null ? "" : locationField.getText().trim();
        String dateText = dateField.getText() == null ? "" : dateField.getText().trim();

        if (title.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing data", "Please enter a title.");
            return;
        }

        // Apply changes to the domain object
        event.setTitle(title);
        event.setLocation(location);

        if (!dateText.isEmpty()) {
            try {
                LocalDate d = LocalDate.parse(dateText, DATE_FMT);
                LocalDateTime start = d.atTime(9, 0); // simple default time
                event.setStartDateTime(start);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.WARNING,
                        "Invalid date",
                        "Date must be in format yyyy-MM-dd (for example, 2025-12-08).");
                return;
            }
        }

        int actorUserId = GuiContext.getCurrentUser().userId();

        try {
            GuiContext.eventService().updateEvent(actorUserId, event);
            showAlert(Alert.AlertType.INFORMATION, "Saved", "Event details have been updated.");
            closeWindow();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update event: " + ex.getMessage());
        }
    }

    @FXML
    private void handleDelete(ActionEvent actionEvent) {
        if (event == null) {
            showAlert(Alert.AlertType.ERROR, "No event", "There is no event to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this event?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait();

        if (confirm.getResult() != ButtonType.YES) {
            return;
        }

        int actorUserId = GuiContext.getCurrentUser().userId();

        try {
            GuiContext.eventService().deleteEvent(actorUserId, event.getId());
            showAlert(Alert.AlertType.INFORMATION, "Deleted", "Event has been deleted.");
            closeWindow();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete event: " + ex.getMessage());
        }
    }

    @FXML
    private void handleBack(ActionEvent actionEvent) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
