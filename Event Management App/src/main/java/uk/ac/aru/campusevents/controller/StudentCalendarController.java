/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: StudentCalendarController.java
 * Purpose:
 *   JavaFX controller for the student-facing calendar view of events.
 *
 * Responsibilities:
 *   • Display a month-view calendar (GridPane) of days.
 *   • Load events for the visible month via EventService.
 *   • When a day is clicked:
 *       – Show all events on that date in a ListView.
 *       – Show the student's registration status for each event.
 *   • On double-click of an event:
 *       – Open the standard Event Details window (EventDetailsController).
 *   • Provide Previous/Next month navigation and a Back button.
 *
 * Why this exists:
 *   The calendar provides a visual overview of when events are happening,
 *   complementing the Student Dashboard's tabular view. It is a pure UI
 *   layer over the existing EventService and RegistrationService.
 */
package uk.ac.aru.campusevents.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.EventCategory;
import uk.ac.aru.campusevents.dto.EventSearchCriteria;
import uk.ac.aru.campusevents.ui.GuiContext;
import uk.ac.aru.campusevents.ui.SceneManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class StudentCalendarController {

    @FXML private DatePicker dateFilter;
    @FXML private ComboBox<EventCategory> categoryFilter;
    @FXML private TextField locationFilter;

    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> titleCol;
    @FXML private TableColumn<Event, String> categoryCol;
    @FXML private TableColumn<Event, String> dateCol;
    @FXML private TableColumn<Event, String> locationCol;
    @FXML private TableColumn<Event, String> statusCol;

    private final ObservableList<Event> masterData   = FXCollections.observableArrayList();
    private final ObservableList<Event> filteredData = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        // Table column bindings
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        categoryCol.setCellValueFactory(cell ->
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> {
                            EventCategory c = cell.getValue().getCategory();
                            return c == null ? "" : c.name();
                        }
                )
        );

        dateCol.setCellValueFactory(cell ->
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> {
                            var dt = cell.getValue().getStartDateTime();
                            return dt == null ? "" : dt.format(DATE_FMT);
                        }
                )
        );

        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));

        statusCol.setCellValueFactory(cell ->
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> {
                            var s = cell.getValue().getStatus();
                            return s == null ? "" : s.name();
                        }
                )
        );

        eventsTable.setItems(filteredData);
        eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Filters
        categoryFilter.getItems().add(null); // "All"
        categoryFilter.getItems().addAll(EventCategory.values());
        categoryFilter.setValue(null);

        dateFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, o, n) -> applyFilters());
        locationFilter.textProperty().addListener((obs, o, n) -> applyFilters());

        // Double-click: open event details (read-only)
        eventsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Event ev = eventsTable.getSelectionModel().getSelectedItem();
                if (ev != null) {
                    openDetails(ev);
                }
            }
        });

        loadEvents();
        applyFilters();
    }

    private void loadEvents() {
        masterData.clear();

        // All events in the system
        List<Event> events = GuiContext.eventService().search(
                new EventSearchCriteria(
                        null,   // text
                        null,   // category
                        null,   // from
                        null,   // to
                        null    // location
                )
        );

        // Sort by newest first (latest start date on top)
        events = events.stream()
                .sorted(Comparator.comparing(
                        Event::getStartDateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        masterData.addAll(events);
    }

    private void applyFilters() {
        filteredData.clear();

        LocalDate date = dateFilter.getValue();
        EventCategory category = categoryFilter.getValue();
        String locationText = (locationFilter.getText() == null)
                ? ""
                : locationFilter.getText().trim().toLowerCase();

        var result = masterData.stream()
                .filter(ev -> {
                    // Date filter
                    if (date != null && ev.getStartDateTime() != null) {
                        LocalDate evDate = ev.getStartDateTime().toLocalDate();
                        if (!evDate.equals(date)) return false;
                    }

                    // Category filter
                    if (category != null && ev.getCategory() != category) {
                        return false;
                    }

                    // Location filter
                    if (!locationText.isEmpty()) {
                        String loc = (ev.getLocation() == null ? "" : ev.getLocation()).toLowerCase();
                        if (!loc.contains(locationText)) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        filteredData.addAll(result);
    }

    @FXML
    private void clearFilters() {
        dateFilter.setValue(null);
        categoryFilter.setValue(null);
        locationFilter.clear();
        applyFilters();
    }

    // ------------ Navigation ------------

    @FXML
    private void goHome() {
        SceneManager.switchScene(
                eventsTable,
                "HomePage-view.fxml",
                "Campus Events – Home"
        );
    }

    @FXML
    private void goStudentDashboard() {
        SceneManager.switchScene(
                eventsTable,
                "StudentDashboard-view.fxml",
                "Student Dashboard"
        );
    }

    @FXML
    private void goOrganizerDashboard() {
        SceneManager.switchScene(
                eventsTable,
                "OrganizerDashboard-view.fxml",
                "Organizer Dashboard"
        );
    }

    @FXML
    private void goAdminDashboard() {
        SceneManager.switchScene(
                eventsTable,
                "AdminDashboard-view.fxml",
                "Admin Dashboard"
        );
    }

    // ------------ Details dialog (reuses your existing EventDetails) ------------

    private void openDetails(Event event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    uk.ac.aru.campusevents.ui.HelloApplication.class
                            .getResource("EventDetails-view.fxml")
            );
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());

            EventDetailsController controller = loader.getController();
            controller.setEvent(event);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setScene(scene);
            stage.setTitle("Event Details");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
