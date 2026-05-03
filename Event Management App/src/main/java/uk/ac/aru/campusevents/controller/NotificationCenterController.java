/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: NotificationCenterController.java
 *
 * Purpose:
 *   JavaFX controller for the notification center window.
 *
 * Responsibilities:
 *   • Load unread notifications for the current user via NotificationService.
 *   • Display date/time and message in a table, newest first.
 *   • Allow the user to mark all notifications as read.
 *
 * Design Notes:
 *   This is a pure read-model over NotificationService. Messages already
 *   contain any event-related context in their text (e.g. event title).
 */

package uk.ac.aru.campusevents.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import uk.ac.aru.campusevents.domain.Notification;
import uk.ac.aru.campusevents.ui.GuiContext;
import uk.ac.aru.campusevents.service.NotificationService;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public final class NotificationCenterController {

    @FXML private TableView<NotificationRow> notificationsTable;
    @FXML private TableColumn<NotificationRow, String> dateCol;
    @FXML private TableColumn<NotificationRow, String> messageCol;

    private final ObservableList<NotificationRow> rows = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        if (notificationsTable != null) {
            notificationsTable.setItems(rows);
            notificationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        if (dateCol != null) {
            dateCol.setCellValueFactory(cell -> cell.getValue().createdAtProperty());
        }
        if (messageCol != null) {
            messageCol.setCellValueFactory(cell -> cell.getValue().messageProperty());
        }

        loadUnreadNotifications();
    }

    private void loadUnreadNotifications() {
        var session = GuiContext.getCurrentUser();
        if (session == null) {
            return;
        }
        int userId = session.userId();

        List<Notification> unread =
                GuiContext.notificationService().listUnread(userId);

        rows.clear();
        unread.stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .forEach(n -> rows.add(new NotificationRow(
                        n.getCreatedAt().format(DATE_FMT),
                        n.getMessage()
                )));
    }

    /**
     * Marks all unread notifications for the current user as read.
     * This action updates the persistent notification records via the
     * {@link NotificationService}, clearing the unread state for all messages
     * associated with the authenticated user. After the update completes, the
     * table view is refreshed to reflect an empty notification list.
     * A confirmation dialog is displayed to provide immediate user feedback.
     * The handler does not close the window; users may continue viewing
     * cleared notifications or dismiss the window manually.
     */
    @FXML
    private void handleMarkAllRead() {
        var session = GuiContext.getCurrentUser();
        if (session == null) {
            return;
        }

        GuiContext.notificationService().markAllRead(session.userId());
        rows.clear();

        Alert a = new Alert(Alert.AlertType.INFORMATION, "All notifications marked as read.", ButtonType.OK);
        a.showAndWait();
    }

    /**
     * Closes the Notification Center window.
     * No notification state is modified by this action; unread notifications
     * remain unread unless explicitly marked as read.
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) notificationsTable.getScene().getWindow();
        stage.close();
    }

    // ---------------------------------------------------------------------
    // Row model
    // ---------------------------------------------------------------------

    /**
     * View-model wrapper for presenting Notification data inside the
     * Notification Center's TableView.
     * This class exposes JavaFX {@link SimpleStringProperty} values for
     * display-ready fields:
     *   • createdAt  – formatted timestamp string (dd/MM/yyyy HH:mm)
     *   • message    – the notification's text
     * Domain objects ({@link Notification}) remain immutable. The purpose of
     * this row model is strictly to provide JavaFX-friendly observable
     * properties for table binding.
     */
    public static final class NotificationRow {

        private final SimpleStringProperty createdAt =
                new SimpleStringProperty();
        private final SimpleStringProperty message =
                new SimpleStringProperty();

        public NotificationRow(String createdAt, String message) {
            this.createdAt.set(createdAt == null ? "" : createdAt);
            this.message.set(message == null ? "" : message);
        }

        public SimpleStringProperty createdAtProperty() {
            return createdAt;
        }

        public SimpleStringProperty messageProperty() {
            return message;
        }
    }
}
