/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: StudentDashboardController.java
 * Purpose:
 *   Controller for the Student Dashboard UI (read-only version, no registration actions).
 *
 * Responsibilities:
 *   • Load all of the student's registered events using RegistrationService
 *   • Convert domain events into StudentEventRow view models
 *   • Provide filtering (date, category, location)
 *   • Double-click row opens event details
 *   • Export "My Events" as CSV
 *   • Navigate between dashboards with role-based visibility
 *
 * NOTE:
 *   Registration and cancellation functionality has been fully removed.
 */

package uk.ac.aru.campusevents.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.EventCategory;
import uk.ac.aru.campusevents.dto.UserSession;
import uk.ac.aru.campusevents.ui.Authorization;
import uk.ac.aru.campusevents.ui.GuiContext;
import uk.ac.aru.campusevents.ui.SceneManager;
import uk.ac.aru.campusevents.ui.StudentEventRow;
import uk.ac.aru.campusevents.ui.HelloApplication;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public final class StudentDashboardController {

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

    @FXML private TableView<StudentEventRow> eventsTable;

    @FXML private TableColumn<StudentEventRow,String> titleCol;
    @FXML private TableColumn<StudentEventRow,String> categoryCol;
    @FXML private TableColumn<StudentEventRow,String> organizerCol;
    @FXML private TableColumn<StudentEventRow,String> dateCol;
    @FXML private TableColumn<StudentEventRow,String> locationCol;
    @FXML private TableColumn<StudentEventRow,String> statusCol;
    @FXML private TableColumn<StudentEventRow,String> regStatusCol;

    @FXML private Button exportMyEventsButton;

    @FXML private DatePicker dateFilter;
    @FXML private ComboBox<EventCategory> categoryFilter;
    @FXML private TextField locationFilter;

    private final ObservableList<StudentEventRow> masterData   = FXCollections.observableArrayList();
    private final ObservableList<StudentEventRow> filteredData = FXCollections.observableArrayList();

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    @FXML
    private void initialize() {
        applyRoleBasedVisibility();

        // Column bindings
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        organizerCol.setCellValueFactory(new PropertyValueFactory<>("organizer"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        regStatusCol.setCellValueFactory(new PropertyValueFactory<>("registrationStatus"));

        // Filters
        categoryFilter.getItems().add(null); // "All"
        categoryFilter.getItems().addAll(EventCategory.values());
        categoryFilter.setValue(null);

        dateFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        locationFilter.textProperty().addListener((obs, o, n) -> applyFilters());

        // Load student's events
        loadEvents();
        applyFilters();

        // Double-click to open event details
        eventsTable.setRowFactory(tv -> {
            TableRow<StudentEventRow> row = new TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (evt.getClickCount() == 2 && !row.isEmpty()) {
                    openDetails(row.getItem().getEvent());
                }
            });
            return row;
        });
    }

    private void applyRoleBasedVisibility() {
        UserSession session = GuiContext.getCurrentUser();

        boolean isStudent   = Authorization.isStudent(session);
        boolean isOrganizer = Authorization.isOrganizer(session);
        boolean isAdmin     = Authorization.isAdmin(session);

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
    // Load student events (read-only)
    // -------------------------------------------------------------------------

    private void loadEvents() {
        masterData.clear();

        var session = GuiContext.getCurrentUser();
        if (session == null) {
            showError("You must be logged in.");
            return;
        }

        int studentId = session.userId();

        // Already filtered to "my events" in the service
        List<Event> events = GuiContext.registrationService().listMyEvents(studentId);

        events = events.stream()
                .sorted((e1, e2) -> {
                    var d1 = e1.getStartDateTime();
                    var d2 = e2.getStartDateTime();
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1);
                })
                .toList();

        for (Event e : events) {
            String regStatus = computeRegistrationStatus(studentId, e.getId());
            String organizer = computeOrganizerName(e);
            masterData.add(new StudentEventRow(e, organizer, regStatus));
        }

        applyFilters();
    }

    private String computeRegistrationStatus(int studentId, int eventId) {
        var myEvents = GuiContext.registrationService().listMyEvents(studentId);

        for (Event e : myEvents) {
            if (e.getId() == eventId) {
                List<Integer> attendees =
                        GuiContext.registrationService().listAttendeeUserIds(eventId);

                if (attendees.contains(studentId)) return "Registered";
                return "Waitlisted";
            }
        }
        return "Not Registered";
    }

    private String computeOrganizerName(Event e) {
        return GuiContext.eventService().resolveOrganizerName(e);
    }



    // -------------------------------------------------------------------------
    // Filtering (unchanged)
    // -------------------------------------------------------------------------

    private void applyFilters() {
        filteredData.clear();

        LocalDate date = dateFilter.getValue();
        EventCategory category = categoryFilter.getValue();
        String locationText = locationFilter.getText() == null
                ? ""
                : locationFilter.getText().trim().toLowerCase();

        var result = masterData.stream()
                .filter(row -> {
                    if (date != null && row.getEvent().getStartDateTime() != null) {
                        if (!row.getEvent().getStartDateTime().toLocalDate().equals(date))
                            return false;
                    }
                    if (category != null && row.getEvent().getCategory() != category)
                        return false;
                    if (!locationText.isEmpty()) {
                        if (!row.getLocation().toLowerCase().contains(locationText))
                            return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        filteredData.addAll(result);
        eventsTable.setItems(filteredData);
    }

    @FXML
    private void clearFilters() {
        dateFilter.setValue(null);
        categoryFilter.setValue(null);
        locationFilter.clear();
        applyFilters();
    }

    // -------------------------------------------------------------------------
    // CSV Export
    // -------------------------------------------------------------------------

    @FXML
    private void handleExportMyEvents() {
        var session = GuiContext.getCurrentUser();
        if (session == null) {
            showError("You must be logged in to export your events.");
            return;
        }

        int studentId = session.userId();
        final String csv;

        try {
            csv = GuiContext.csvExportService().exportMyEventsCsv(studentId);
            if (csv == null || csv.isBlank()) {
                showInfo("You have no active event registrations.");
                return;
            }
        } catch (Exception ex) {
            showError("Could not generate CSV: " + ex.getMessage());
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save My Events CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv")
        );
        chooser.setInitialFileName("my-events.csv");

        Stage stage = (Stage) eventsTable.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try {
            Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);
            showInfo("Export saved to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            showError("Failed to save file: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Event details popup
    // -------------------------------------------------------------------------

    private void openDetails(Event event) {
        if (event == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/ui/views/EventDetails-view.fxml")
            );

            Scene scene = new Scene(loader.load());
            EventDetailsController controller = loader.getController();
            controller.setEvent(event);

            Stage stage = new Stage();
            stage.setTitle("Event Details");
            stage.setScene(scene);

            if (eventsTable.getScene() != null)
                stage.initOwner(eventsTable.getScene().getWindow());

            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Could not open event details: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    @FXML
    private void goUpcoming() {
        SceneManager.switchScene(
                eventsTable,
                "HomePage-view.fxml",
                "Campus Events – Upcoming Events"
        );
    }

    @FXML
    private void goStudent() {
        // Already here
    }

    @FXML
    private void goOrganizer() {
        if (Authorization.isOrganizer(GuiContext.getCurrentUser())) {
            SceneManager.switchScene(
                    eventsTable,
                    "OrganizerDashboard-view.fxml",
                    "Organizer Dashboard"
            );
        }
    }

    @FXML
    private void goAdmin() {
        if (Authorization.isAdmin(GuiContext.getCurrentUser())) {
            SceneManager.switchScene(
                    eventsTable,
                    "AdminDashboard-view.fxml",
                    "Admin Dashboard"
            );
        }
    }

    // -------------------------------------------------------------------------
    // Alerts
    // -------------------------------------------------------------------------

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Error");
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Student Dashboard");
        a.showAndWait();
    }
}
