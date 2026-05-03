/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: HomePageController.java
 *
 * Purpose:
 *   JavaFX controller for the Home page after a successful login.
 *
 * Responsibilities:
 *   • Display navigation buttons for the main dashboards.
 *   • Show a personalized welcome message and footer based on UserSession.
 *   • Provide a Log Out action that returns the user to the Log In screen.
 *   • Display ALL events (homepage = global event overview).
 *   • Provide a Calendar button (students only) that opens the calendar screen.
 *   • Allow students to register for events from the Upcoming Events table.
 */

package uk.ac.aru.campusevents.controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.EventStatus;
import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.dto.EventSearchCriteria;
import uk.ac.aru.campusevents.dto.UserSession;
import uk.ac.aru.campusevents.exceptions.ForbiddenException;
import uk.ac.aru.campusevents.ui.Authorization;
import uk.ac.aru.campusevents.ui.GuiContext;
import uk.ac.aru.campusevents.ui.SceneManager;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class HomePageController {

    // -------------------------------------------------------------------------
    // Header / navigation
    // -------------------------------------------------------------------------

    @FXML private Button UpcomingEvents;        // "Upcoming" button
    @FXML private Button StudentDashboard;
    @FXML private Button OrganizerDashboard;
    @FXML private Button AdminDashboard;
    @FXML private Button logoutButton;
    @FXML private Button NotificationsButton;

    @FXML private Label DashboardName;
    @FXML private Label welcomeLabel;
    @FXML private Label footerLabel;

    // -------------------------------------------------------------------------
    // Upcoming Events table (ALL events for everyone)
    // -------------------------------------------------------------------------

    // NOTE: fx:id in FXML must be "UpcomingEventsTable"
    @FXML private TableView<Event> UpcomingEventsTable;
    @FXML private TableColumn<Event, String> eventNameCol;
    @FXML private TableColumn<Event, String> eventStatusCol;
    @FXML private TableColumn<Event, String> eventOrganizerCol;
    @FXML private TableColumn<Event, String> eventDateCol;
    @FXML private TableColumn<Event, String> eventLocationCol;

    private final ObservableList<Event> allEvents = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    @FXML
    private void initialize() {
        if (DashboardName != null) {
            DashboardName.setText("Upcoming Events");
        }

        applyRoleBasedVisibility();
        updateUserTexts();
        configureUpcomingEventsTable();
        loadAllEvents();           // load all events immediately on home
        checkUnreadNotificationsOnHome();
    }

    private void configureUpcomingEventsTable() {
        if (UpcomingEventsTable == null) {
            return;
        }

        // Column resize policy to avoid cut-off columns
        UpcomingEventsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        // Column bindings
        eventNameCol.setCellValueFactory(cd ->
                Bindings.createStringBinding(
                        () -> cd.getValue().getTitle() == null ? "" : cd.getValue().getTitle()
                )
        );

        eventStatusCol.setCellValueFactory(cd ->
                Bindings.createStringBinding(
                        () -> {
                            EventStatus s = cd.getValue().getStatus();
                            return (s == null) ? "" : s.name();
                        }
                )
        );

        eventOrganizerCol.setCellValueFactory(cd ->
                Bindings.createStringBinding(
                        () -> GuiContext.eventService().resolveOrganizerName(cd.getValue())
                )
        );


        eventDateCol.setCellValueFactory(cd ->
                Bindings.createStringBinding(
                        () -> {
                            var dt = cd.getValue().getStartDateTime();
                            return (dt == null) ? "" : dt.format(DATE_FMT);
                        }
                )
        );

        eventLocationCol.setCellValueFactory(cd ->
                Bindings.createStringBinding(
                        () -> {
                            String loc = cd.getValue().getLocation();
                            return loc == null ? "" : loc;
                        }
                )
        );

        UpcomingEventsTable.setItems(allEvents);

        // Double-click → register for event (students only; backend enforces it)
        UpcomingEventsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Event ev = UpcomingEventsTable.getSelectionModel().getSelectedItem();
                if (ev != null) {
                    handleRegisterFromHome(ev);
                }
            }
        });
    }

    /**
     * Loads ALL events (no filtering) and sorts them newest first.
     */
    private void loadAllEvents() {
        allEvents.clear();

        List<Event> events = GuiContext.eventService().search(
                new EventSearchCriteria(
                        null,  // titleContains
                        null,  // category
                        null,  // status
                        null,  // fromDateTime
                        null   // toDateTime
                )
        );

        events.stream()
                .sorted(Comparator.comparing(
                        Event::getStartDateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())) // newest first
                )
                .forEach(allEvents::add);
    }

    // -------------------------------------------------------------------------
    // Registration from Home (Upcoming table)
    // -------------------------------------------------------------------------

    /**
     * Handles registration from the home page Upcoming Events table.
     * Double-click on a row → confirmation dialog → call RegistrationService.register.
     */
    private void handleRegisterFromHome(Event event) {
        UserSession session = GuiContext.getCurrentUser();
        if (session == null) {
            showError("Please log in as a student to register for events.");
            return;
        }

        // Confirm with the user
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Do you want to register for:\n\n" + event.getTitle() + "?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setTitle("Register for Event");
        confirm.setHeaderText(null);

        var result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        try {
            GuiContext.registrationService().register(session.userId(), event.getId());
            showInfo("Registration successful.\n\n" +
                    "Check your Student Dashboard → My Events for status.");
            loadAllEvents(); // refresh in case status / capacity changed

        } catch (ForbiddenException ex) {
            // This is "Student role required" (or similar RBAC error)
            showError("Only students can register for events.");
        } catch (IllegalStateException ex) {
            // Date rule, not OPEN, already registered, etc.
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Could not register for this event: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // "Upcoming" button (just reloads all events)
    // -------------------------------------------------------------------------

    @FXML
    private void handleUpcomingEvents() {
        if (DashboardName != null) {
            DashboardName.setText("Upcoming Events");
        }
        loadAllEvents();
    }

    // -------------------------------------------------------------------------
    // Calendar navigation (students only)
    // -------------------------------------------------------------------------

    @FXML
    private void openCalendar() {
        UserSession session = GuiContext.getCurrentUser();
        if (!Authorization.isStudent(session)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Event Calendar");
            alert.setHeaderText("Students only");
            alert.setContentText("Only students can access the event calendar.");
            alert.showAndWait();
            return;
        }

        if (UpcomingEventsTable != null) {
            SceneManager.switchScene(
                    UpcomingEventsTable,
                    "StudentCalendar-view.fxml",
                    "Event Calendar"
            );
        } else {
            navigateTo("/ui/views/StudentCalendar-view.fxml",
                    "Campus Events – Event Calendar");
        }
    }

    // -------------------------------------------------------------------------
    // Notifications
    // -------------------------------------------------------------------------

    @FXML
    private void openNotifications() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/views/Notifications-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 600, 400);

            Stage stage = new Stage();
            stage.setTitle("Notifications");
            stage.setScene(scene);
            stage.initOwner(resolveStage());
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void checkUnreadNotificationsOnHome() {
        var session = GuiContext.getCurrentUser();
        if (session == null) {
            return;
        }

        int userId = session.userId();
        int unreadCount = GuiContext.notificationService()
                .listUnread(userId)
                .size();

        if (unreadCount > 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Notifications");
            alert.setHeaderText("You have " + unreadCount + " new notification"
                    + (unreadCount == 1 ? "" : "s") + ".");
            alert.setContentText("Click the Notifications button to view them.");
            alert.show();
        }
    }

    // -------------------------------------------------------------------------
    // Role-based button visibility
    // -------------------------------------------------------------------------

    private void applyRoleBasedVisibility() {
        UserSession session = GuiContext.getCurrentUser();

        boolean isStudent   = Authorization.isStudent(session);
        boolean isOrganizer = Authorization.isOrganizer(session);
        boolean isAdmin     = Authorization.isAdmin(session);

        if (StudentDashboard != null) {
            StudentDashboard.setDisable(!isStudent);
            StudentDashboard.setVisible(isStudent);
        }
        if (OrganizerDashboard != null) {
            OrganizerDashboard.setDisable(!isOrganizer);
            OrganizerDashboard.setVisible(isOrganizer);
        }
        if (AdminDashboard != null) {
            AdminDashboard.setDisable(!isAdmin);
            AdminDashboard.setVisible(isAdmin);
        }
    }

    // -------------------------------------------------------------------------
    // Welcome/footer text
    // -------------------------------------------------------------------------

    private void updateUserTexts() {
        UserSession session = GuiContext.getCurrentUser();

        if (session != null) {
            String name  = session.name();
            String email = session.email();
            Set<Role> roles = session.roles();

            String rolesText;
            if (roles == null || roles.isEmpty()) {
                rolesText = "";
            } else {
                String joined = roles.stream()
                        .map(Role::name)
                        .sorted()
                        .collect(Collectors.joining(", "));
                rolesText = " (" + joined + ")";
            }

            if (welcomeLabel != null) {
                welcomeLabel.setText("Welcome, " + name + rolesText);
            }

            if (footerLabel != null) {
                if (rolesText.isEmpty()) {
                    footerLabel.setText("Logged in as " + email);
                } else {
                    // rolesText = " (A, B)" → "A, B"
                    String rolesPlain = rolesText.substring(2, rolesText.length() - 1);
                    footerLabel.setText("Logged in as " + email + " | Roles: " + rolesPlain);
                }
            }
        } else {
            if (welcomeLabel != null) {
                welcomeLabel.setText("Welcome to Campus Events");
            }
            if (footerLabel != null) {
                footerLabel.setText("");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Dashboard navigation
    // -------------------------------------------------------------------------

    @FXML
    private void openStudentDashboard() {
        UserSession session = GuiContext.getCurrentUser();
        if (!Authorization.isStudent(session)) {
            return;
        }
        navigateTo("/ui/views/StudentDashboard-view.fxml",
                "Campus Events – Student Dashboard");
    }

    @FXML
    private void openOrganizerDashboard() {
        UserSession session = GuiContext.getCurrentUser();
        if (!Authorization.isOrganizer(session)) {
            return;
        }
        navigateTo("/ui/views/OrganizerDashboard-view.fxml",
                "Campus Events – Organizer Dashboard");
    }

    @FXML
    private void openAdminDashboard() {
        UserSession session = GuiContext.getCurrentUser();
        if (!Authorization.isAdmin(session)) {
            return;
        }
        navigateTo("/ui/views/AdminDashboard-view.fxml",
                "Campus Events – Admin Dashboard");
    }

    @FXML
    private void openAuditLogs() {
        UserSession session = GuiContext.getCurrentUser();
        if (!Authorization.isAdmin(session)) {
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
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @FXML
    private void handleLogout() {
        try {
            GuiContext.setCurrentUser(null);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/views/LogInPage-view.fxml")
            );

            loader.setControllerFactory(type -> {
                if (type == LogInPageController.class) {
                    return new LogInPageController(GuiContext.authService());
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Unable to construct controller: " + type, e);
                }
            });

            Scene loginScene = new Scene(loader.load(), 900, 600);

            Stage stage = resolveStage();
            stage.setScene(loginScene);
            stage.setTitle("Campus Events – Log In");

        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath)
            );

            Scene scene = new Scene(loader.load(), 900, 600);

            Stage stage = resolveStage();
            stage.setScene(scene);
            stage.setTitle(title);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private Stage resolveStage() {
        if (logoutButton != null && logoutButton.getScene() != null) {
            return (Stage) logoutButton.getScene().getWindow();
        }
        if (DashboardName != null && DashboardName.getScene() != null) {
            return (Stage) DashboardName.getScene().getWindow();
        }
        throw new IllegalStateException("Cannot resolve Stage from HomePageController.");
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Error");
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("Information");
        a.showAndWait();
    }

    // -------------------------------------------------------------------------
    // Optional: Event details dialog (currently unused, but kept for later)
    // -------------------------------------------------------------------------

    private void openDetails(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    uk.ac.aru.campusevents.ui.HelloApplication.class
                            .getResource("EventDetails-view.fxml")
            );
            Scene scene = new Scene(loader.load());

            EventDetailsController controller = loader.getController();
            controller.setEvent(event);

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Event Details");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
