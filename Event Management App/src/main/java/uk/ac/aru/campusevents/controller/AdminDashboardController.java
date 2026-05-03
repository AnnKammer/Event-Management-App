/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AdminDashboardController.java
 * Purpose:
 *   JavaFX controller for the Admin Dashboard screen.
 *
 * Responsibilities:
 *   • Load all events in the system via EventService.search()
 *   • Display events in a TableView with key metadata
 *   • Allow an ADMIN user to:
 *       – Change the status of an event (OPEN, FULL, COMPLETED, ARCHIVED, CANCELLED)
 *       – Delete events
 *   • Navigate between Home / Student / Organizer / Admin dashboards
 *
 * Why this exists:
 *   Administrators need a global overview of all events and the ability
 *   to enforce platform-wide rules (closing, cancelling, or removing
 *   events that violate policy or are no longer valid). This controller
 *   provides a thin UI layer over the RBAC-enforced EventService.
 */

package uk.ac.aru.campusevents.controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.EventCategory;
import uk.ac.aru.campusevents.domain.enums.EventStatus;
import uk.ac.aru.campusevents.dto.EventSearchCriteria;
import uk.ac.aru.campusevents.dto.UserSession;
import uk.ac.aru.campusevents.ui.Authorization;
import uk.ac.aru.campusevents.ui.GuiContext;
import uk.ac.aru.campusevents.ui.SceneManager;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public final class AdminDashboardController {

    // -------------------------------------------------------------------------
    // Navigation buttons (top bar)
    // -------------------------------------------------------------------------

    @FXML private Button upcomingBtn;
    @FXML private Button studentBtn;
    @FXML private Button organizerBtn;
    @FXML private Button adminBtn;

    // -------------------------------------------------------------------------
    // Table + filters
    // -------------------------------------------------------------------------

    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> nameCol;
    @FXML private TableColumn<Event, String> statusCol;
    @FXML private TableColumn<Event, String> organizerCol;
    @FXML private TableColumn<Event, String> dateCol;
    @FXML private TableColumn<Event, String> locationCol;

    @FXML private ComboBox<EventStatus> statusFilter;
    @FXML private ComboBox<EventCategory> categoryFilter;

    private final ObservableList<Event> masterData   = FXCollections.observableArrayList();
    private final ObservableList<Event> filteredData = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    @FXML
    private void initialize() {
        applyRoleBasedVisibility();

        // Column bindings
        nameCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        statusCol.setCellValueFactory(cellData ->
                Bindings.createStringBinding(
                        () -> {
                            EventStatus s = cellData.getValue().getStatus();
                            return s == null ? "" : s.name();
                        }
                ));

        organizerCol.setCellValueFactory(cellData ->
                Bindings.createStringBinding(
                        () -> {
                            Event e = cellData.getValue();
                            if (e.getOrganizerUserId() != null) {
                                return "Organizer #" + e.getOrganizerUserId();
                            }
                            if (e.getOrganizationId() != null) {
                                return "Organization #" + e.getOrganizationId();
                            }
                            return "Unknown";
                        }
                ));

        dateCol.setCellValueFactory(cellData ->
                Bindings.createStringBinding(
                        () -> {
                            var dt = cellData.getValue().getStartDateTime();
                            return dt == null ? "" : dt.format(DATE_FMT);
                        }
                ));

        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));

        // Filters
        statusFilter.getItems().add(null); // "All"
        statusFilter.getItems().addAll(EventStatus.values());
        statusFilter.setValue(null);

        categoryFilter.getItems().add(null); // "All"
        categoryFilter.getItems().addAll(EventCategory.values());
        categoryFilter.setValue(null);

        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        eventsTable.setItems(filteredData);

        loadAllEvents();
        applyFilters();
    }

    /**
     * Hides/disables navigation buttons for dashboards the current user
     * is not allowed to access. Presentation-only; real checks still
     * happen in the handlers.
     */
    private void applyRoleBasedVisibility() {
        UserSession session = GuiContext.getCurrentUser();

        boolean isStudent   = Authorization.isStudent(session);
        boolean isOrganizer = Authorization.isOrganizer(session);
        boolean isAdmin     = Authorization.isAdmin(session);

        // Upcoming (HomePage) is global → always visible
        if (upcomingBtn != null) {
            upcomingBtn.setVisible(true);
            upcomingBtn.setDisable(false);
        }

        if (studentBtn != null) {
            studentBtn.setVisible(isStudent);
            studentBtn.setDisable(!isStudent);
        }
        if (organizerBtn != null) {
            organizerBtn.setVisible(isOrganizer);
            organizerBtn.setDisable(!isOrganizer);
        }
        if (adminBtn != null) {
            adminBtn.setVisible(isAdmin);
            adminBtn.setDisable(!isAdmin);
        }
    }

    // -------------------------------------------------------------------------
    // Data loading + filtering
    // -------------------------------------------------------------------------

    private void loadAllEvents() {
        masterData.clear();

        List<Event> all = GuiContext.eventService().search(
                new EventSearchCriteria(
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        masterData.addAll(all);
    }

    private void applyFilters() {
        filteredData.clear();

        EventStatus status      = statusFilter.getValue();
        EventCategory category  = categoryFilter.getValue();

        masterData.stream()
                .filter(e -> {
                    if (status != null && e.getStatus() != status) {
                        return false;
                    }
                    if (category != null && e.getCategory() != category) {
                        return false;
                    }
                    return true;
                })
                .forEach(filteredData::add);
    }

    // -------------------------------------------------------------------------
    // Admin actions on events
    // -------------------------------------------------------------------------

    @FXML
    private void handleSetOpen() {
        setSelectedEventStatus(EventStatus.OPEN);
    }

    @FXML
    private void handleSetFull() {
        setSelectedEventStatus(EventStatus.FULL);
    }

    @FXML
    private void handleSetCompleted() {
        setSelectedEventStatus(EventStatus.COMPLETED);
    }

    @FXML
    private void handleSetArchived() {
        setSelectedEventStatus(EventStatus.ARCHIVED);
    }

    @FXML
    private void handleSetCancelled() {
        setSelectedEventStatus(EventStatus.CANCELLED);
    }

    private void setSelectedEventStatus(EventStatus newStatus) {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Please select an event first.");
            return;
        }

        int actorUserId = GuiContext.getCurrentUser().userId();

        try {
            selected.setStatus(newStatus);
            GuiContext.eventService().updateEvent(actorUserId, selected);
            showInfo("Status updated to " + newStatus + ".");
            loadAllEvents();
            applyFilters();
        } catch (Exception ex) {
            showError("Failed to update event: " + ex.getMessage());
        }
    }

    @FXML
    private void handleDeleteEvent() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Please select an event to delete.");
            return;
        }

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this event?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        int actorUserId = GuiContext.getCurrentUser().userId();

        try {
            GuiContext.eventService().deleteEvent(actorUserId, selected.getId());
            showInfo("Event deleted.");
            loadAllEvents();
            applyFilters();
        } catch (Exception ex) {
            showError("Failed to delete event: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Audit Logs navigation
    // -------------------------------------------------------------------------

    /**
     * Opens the Audit Logs screen in a separate window for administrators.
     * Uses AdminAuditLogs-view.fxml and enforces that only ADMIN users
     * can open this view.
     */
    @FXML
    private void openAuditLogs() {
        UserSession session = GuiContext.getCurrentUser();
        if (!Authorization.isAdmin(session)) {
            // Defensive check: in normal use the button is only visible for admins.
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/views/AdminAuditLogs-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 900, 500);

            Stage stage = new Stage();
            stage.setTitle("Campus Events – Audit Logs");
            stage.setScene(scene);

            if (eventsTable != null && eventsTable.getScene() != null) {
                stage.initOwner(eventsTable.getScene().getWindow());
            }

            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
            // For the coursework, a stack trace is acceptable here.
        }
    }

    // -------------------------------------------------------------------------
    // Navigation between dashboards
    // -------------------------------------------------------------------------

    @FXML
    private void goUpcoming() {
        // Back to shared home page
        SceneManager.switchScene(
                eventsTable,
                "HomePage-view.fxml",
                "Campus Events – Upcoming Events"
        );
    }

    @FXML
    private void goStudent() {
        UserSession session = GuiContext.getCurrentUser();
        if (!Authorization.isStudent(session)) {
            return;
        }
        SceneManager.switchScene(
                eventsTable,
                "StudentDashboard-view.fxml",
                "Student Dashboard"
        );
    }

    @FXML
    private void goOrganizer() {
        UserSession session = GuiContext.getCurrentUser();
        if (!Authorization.isOrganizer(session)) {
            return;
        }
        SceneManager.switchScene(
                eventsTable,
                "OrganizerDashboard-view.fxml",
                "Organizer Dashboard"
        );
    }

    @FXML
    private void goAdmin() {
        UserSession session = GuiContext.getCurrentUser();
        if (!Authorization.isAdmin(session)) {
            return;
        }
        // Already here — no navigation needed.
    }

    // -------------------------------------------------------------------------
    // Helper dialogs
    // -------------------------------------------------------------------------

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Admin Dashboard");
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Admin Dashboard");
        a.showAndWait();
    }
}
