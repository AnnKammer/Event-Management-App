/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: CreateEventOrganizerController.java
 * Purpose:
 *   Controller for the "Create New Event" screen for organizers.
 *
 * Responsibilities:
 *   • Collect event details from form fields.
 *   • Decide whether the event is personal or organization-owned:
 *       – If organizationId is empty → personal event.
 *       – If organizationId is provided → org-owned event, guarded by RBAC.
 *   • Call EventService.createEvent(...) and show clear messages.
 *   • Navigate back to the Organizer Dashboard on success or cancel.
 */

package uk.ac.aru.campusevents.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.EventCategory;
import uk.ac.aru.campusevents.exceptions.ForbiddenException;
import uk.ac.aru.campusevents.ui.GuiContext;
import uk.ac.aru.campusevents.ui.SceneManager;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CreateEventOrganizerController {

    @FXML private TextField nameField;
    @FXML private TextField statusField;
    @FXML private TextField organizerField;
    @FXML private TextField dateField;
    @FXML private TextField locationField;
    @FXML private ComboBox<EventCategory> categoryBox;
    @FXML private TextField capacityField;
    @FXML private TextField organizationIdField;

    @FXML
    private void initialize() {
        // Populate categories from enum
        categoryBox.getItems().addAll(EventCategory.values());

        int userId = GuiContext.getCurrentUser().userId();
        organizerField.setText("You (Organizer #" + userId + ")");
        organizerField.setEditable(false);

        // Status is managed by EventService; show "OPEN" as a hint, read-only
        statusField.setText("OPEN");
        statusField.setEditable(false);
    }

    @FXML
    private void handleCreate(ActionEvent eventAction) {
        String title        = safeText(nameField);
        String location     = safeText(locationField);
        EventCategory category = categoryBox.getValue();
        String capacityText = safeText(capacityField);
        String dateText     = safeText(dateField);
        String orgIdText    = safeText(organizationIdField);

        if (title.isEmpty()) {
            showError("Title is required.");
            return;
        }

        if (capacityText.isEmpty()) {
            showError("Capacity is required.");
            return;
        }

        int capacity;
        try {
            capacity = Integer.parseInt(capacityText);
            if (capacity <= 0) {
                showError("Capacity must be a positive number.");
                return;
            }
        } catch (NumberFormatException ex) {
            showError("Capacity must be a valid integer.");
            return;
        }

        // --- Date: required, at least 1 day in the future ---
        if (dateText.isEmpty()) {
            showError("Date is required.");
            return;
        }

        LocalDateTime start = null;
        try {
            LocalDate d = LocalDate.parse(dateText); // ISO format yyyy-MM-dd

            // Enforce: event must be at least one full day in the future
            LocalDate today = LocalDate.now();
            if (!d.isAfter(today)) {
                showError("Events must be scheduled at least one day in advance.");
                return;
            }

            // Default time (you can change this if you like)
            start = d.atTime(9, 0);
        } catch (Exception ex) {
            showError("Date must be in format yyyy-MM-dd (for example, 2025-12-08).");
            return;
        }

        int actorUserId = GuiContext.getCurrentUser().userId();

        // Decide personal vs organization-owned event
        Event draft;
        if (orgIdText.isEmpty()) {
            // Personal event (always allowed for ORGANIZER role)
            draft = Event.newPersonalEvent(
                    actorUserId,
                    title,
                    "",          // description omitted for now
                    category,
                    "",
                    capacity
            );
        } else {
            int orgId;
            try {
                orgId = Integer.parseInt(orgIdText);
            } catch (NumberFormatException ex) {
                showError("Organization ID must be a number.");
                return;
            }

            // Org-owned event (RBAC + OWNER/MANAGER check in EventServiceImpl.requireCanManageOrg)
            draft = Event.newOrgEvent(
                    orgId,
                    title,
                    "",
                    category,
                    "",
                    capacity
            );
        }

        draft.setLocation(location);
        if (start != null) {
            draft.setStartDateTime(start);
        }

        try {
            GuiContext.eventService().createEvent(actorUserId, draft);
            showInfo("Event created successfully.");
            SceneManager.switchScene(
                    (javafx.scene.Node) eventAction.getSource(),
                    "OrganizerDashboard-view.fxml",
                    "Organizer Dashboard"
            );
        } catch (ForbiddenException | IllegalArgumentException ex) {
            // Messages from EventServiceImpl, e.g.:
            //  - "Organisation id 3 does not exist."
            //  - "You are not registered as OWNER/MANAGER for this organisation."
            //  - "Event must be scheduled at least one day in advance."
            showError(ex.getMessage());
        } catch (Exception ex) {
            // Fallback for anything unexpected
            showError("Failed to create event.");
        }
    }


    @FXML
    private void handleBack(ActionEvent eventAction) {
        SceneManager.switchScene(
                (javafx.scene.Node) eventAction.getSource(),
                "OrganizerDashboard-view.fxml",
                "Organizer Dashboard"
        );
    }

    // ---------------- Helpers ----------------

    private static String safeText(TextField field) {
        String t = field.getText();
        return t == null ? "" : t.trim();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.showAndWait();
    }
}
