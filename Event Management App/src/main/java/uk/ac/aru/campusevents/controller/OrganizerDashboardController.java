/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: OrganizerDashboardController.java
 * Purpose:
 *   JavaFX controller for the Organizer Dashboard screen.
 *
 * Responsibilities:
 *   • Load events owned by the current organizer from EventService
 *   • Display them in a TableView
 *   • Open a stats/CSV window for the selected event (double-click)
 *   • Navigate to the Create Event screen
 *   • Navigate between Student / Organizer / Admin dashboards
 *
 * RBAC + Navigation:
 *   • Only users with the ORGANIZER role should see/use Organizer functions.
 *   • Buttons for dashboards the user cannot access are hidden/disabled.
 *   • Everyone can always go back to the HomePage (Upcoming events).
 */

package uk.ac.aru.campusevents.controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.dto.UserSession;
import uk.ac.aru.campusevents.ui.Authorization;
import uk.ac.aru.campusevents.ui.GuiContext;
import uk.ac.aru.campusevents.ui.SceneManager;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class OrganizerDashboardController {

    // --- Nav buttons (top bar) ---
    @FXML private Button upcomingBtn;
    @FXML private Button studentBtn;
    @FXML private Button organizerBtn;
    @FXML private Button adminBtn;

    // --- Table ---
    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, String> nameCol;
    @FXML private TableColumn<Event, String> statusCol;
    @FXML private TableColumn<Event, String> organizerCol;
    @FXML private TableColumn<Event, String> dateCol;
    @FXML private TableColumn<Event, String> locationCol;

    private final ObservableList<Event> events = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        applyRoleBasedVisibility();

        // Column bindings
        nameCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        statusCol.setCellValueFactory(cellData ->
                Bindings.createStringBinding(
                        () -> cellData.getValue().getStatus() == null
                                ? ""
                                : cellData.getValue().getStatus().name()
                ));

        organizerCol.setCellValueFactory(cellData ->
                Bindings.createStringBinding(
                        () -> GuiContext.eventService().resolveOrganizerName(cellData.getValue())
                ));

        dateCol.setCellValueFactory(cellData ->
                Bindings.createStringBinding(
                        () -> {
                            var dt = cellData.getValue().getStartDateTime();
                            return dt == null ? "" : dt.format(DATE_FMT);
                        }
                ));

        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));

        // Table setup
        eventsTable.setItems(events);
        eventsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Load organiser's events
        loadOrganizerEvents();

        // Double-click → organizer details popup (stats + CSV export)
        eventsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Event selected = eventsTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openOrganizerEventDetails(selected);
                }
            }
        });
    }

    /**
     * Opens the organiser-facing event details window:
     *  • total registrations
     *  • waitlist count
     *  • CSV export of current registrations
     */
    private void openOrganizerEventDetails(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/views/OrganizerEventDetails-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 480, 320);

            OrganizerEventDetailsController controller = loader.getController();
            controller.setEvent(event);

            Stage stage = new Stage();
            stage.setTitle("Event Details – " + (event.getTitle() == null ? "" : event.getTitle()));
            stage.setScene(scene);
            stage.initOwner(eventsTable.getScene().getWindow());
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Hides/disables navigation buttons for dashboards the current user
     * is not allowed to access. Presentation only; real RBAC is enforced
     * by services and navigation handlers.
     */
    private void applyRoleBasedVisibility() {
        UserSession session = GuiContext.getCurrentUser();

        boolean isStudent   = Authorization.isStudent(session);
        boolean isOrganizer = Authorization.isOrganizer(session);
        boolean isAdmin     = Authorization.isAdmin(session);

        // Upcoming is the common home screen → always visible
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

    /**
     * Loads events owned by the current organizer using EventService.listOrganizerEvents().
     */
    private void loadOrganizerEvents() {
        events.clear();

        UserSession session = GuiContext.getCurrentUser();
        if (session == null) {
            return;
        }

        int userId = session.userId();

        List<Event> mine = GuiContext.eventService().listOrganizerEvents(userId);
        events.addAll(mine);
    }

    // ---------------- Navigation + actions ----------------

    @FXML
    private void handleCreateEvent() {
        SceneManager.switchScene(
                eventsTable,
                "CreateEventOrganizer-view.fxml",
                "Create New Event"
        );
    }

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
        // Already on Organizer dashboard; just guard the click.
        UserSession session = GuiContext.getCurrentUser();
        if (!Authorization.isOrganizer(session)) {
            return;
        }
        // no-op
    }

    @FXML
    private void goAdmin() {
        UserSession session = GuiContext.getCurrentUser();
        if (!Authorization.isAdmin(session)) {
            return;
        }
        SceneManager.switchScene(
                eventsTable,
                "AdminDashboard-view.fxml",
                "Admin Dashboard"
        );
    }
}
