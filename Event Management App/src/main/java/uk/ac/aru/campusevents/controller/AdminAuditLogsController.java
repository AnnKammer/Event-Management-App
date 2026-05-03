/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AdminAuditLogsController.java
 * Purpose:
 *   Simple read-only view of audit_log with CSV export.
 *   • No filters – always “latest N” rows.
 *   • Shows: timestamp, action, entity, entityId, actorUserId, details_json.
 */
package uk.ac.aru.campusevents.controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import uk.ac.aru.campusevents.domain.AuditLog;
import uk.ac.aru.campusevents.dto.AuditFilter;
import uk.ac.aru.campusevents.ui.GuiContext;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class AdminAuditLogsController {

    @FXML private TableView<AuditLog> auditTable;
    @FXML private TableColumn<AuditLog, String> tsCol;
    @FXML private TableColumn<AuditLog, String> actionCol;
    @FXML private TableColumn<AuditLog, String> entityCol;
    @FXML private TableColumn<AuditLog, String> entityIdCol;
    @FXML private TableColumn<AuditLog, String> actorCol;
    @FXML private TableColumn<AuditLog, String> detailsCol;

    @FXML private Button refreshBtn;
    @FXML private Button exportCsvBtn;
    @FXML private Button closeBtn;

    @FXML private TextArea csvArea;

    private final ObservableList<AuditLog> logs = FXCollections.observableArrayList();

    // Zone-aware formatter so it can handle Instant from the DB
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    @FXML
    private void initialize() {
        auditTable.setItems(logs);
        auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tsCol.setCellValueFactory(cd ->
                Bindings.createStringBinding(() -> {
                    if (cd.getValue().getTs() == null) return "";
                    return TS_FMT.format(cd.getValue().getTs());
                }));

        actionCol.setCellValueFactory(cd ->
                Bindings.createStringBinding(() -> {
                    var a = cd.getValue().getAction();
                    return a == null ? "" : a.name();
                }));

        entityCol.setCellValueFactory(cd ->
                Bindings.createStringBinding(() -> {
                    var e = cd.getValue().getEntity();
                    return e == null ? "" : e.name();
                }));

        entityIdCol.setCellValueFactory(cd ->
                Bindings.createStringBinding(() -> {
                    Integer id = cd.getValue().getEntityId();
                    return id == null ? "" : String.valueOf(id);
                }));

        actorCol.setCellValueFactory(cd ->
                Bindings.createStringBinding(() -> {
                    Integer id = cd.getValue().getActorUserId();
                    return id == null ? "" : String.valueOf(id);
                }));

        detailsCol.setCellValueFactory(cd ->
                Bindings.createStringBinding(() -> {
                    String d = cd.getValue().getDetailsJson();
                    return d == null ? "" : d;
                }));

        loadLatest();
    }

    /** Loads the latest audit rows (no filters, just limit N). */
    private void loadLatest() {
        logs.clear();

        // Build filter object
        AuditFilter filter = new AuditFilter(
                null,   // entity
                null,   // entityId
                null,   // actorUserId
                null,   // action
                null,   // from
                null,   // to
                200     // limit
        );

        var session = GuiContext.getCurrentUser();
        if (session == null) {
            return;
        }

        int actorUserId = session.userId();

        List<AuditLog> result = GuiContext.auditService().find(filter, actorUserId);
        logs.addAll(result);
    }

    @FXML
    private void handleRefresh() {
        loadLatest();
    }

    @FXML
    private void handleExportCsv() {
        if (logs.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "There are no audit log entries to export.",
                    ButtonType.OK);
            a.setHeaderText(null);
            a.setTitle("Export Audit Logs");
            a.showAndWait();
            return;
        }

        // Build CSV in memory
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,action,entity,entity_id,actor_user_id,details_json\n");

        for (AuditLog log : logs) {
            String ts          = log.getTs() == null ? "" : TS_FMT.format(log.getTs());
            String action      = log.getAction() == null ? "" : log.getAction().name();
            String entity      = log.getEntity() == null ? "" : log.getEntity().name();
            String entityId    = log.getEntityId() == null ? "" : String.valueOf(log.getEntityId());
            String actorUserId = log.getActorUserId() == null ? "" : String.valueOf(log.getActorUserId());
            String details     = log.getDetailsJson() == null ? "" : log.getDetailsJson();

            sb.append(csv(ts)).append(',')
                    .append(csv(action)).append(',')
                    .append(csv(entity)).append(',')
                    .append(csv(entityId)).append(',')
                    .append(csv(actorUserId)).append(',')
                    .append(csv(details)).append('\n');
        }

        String csv = sb.toString();

        // Keep the preview in the TextArea
        csvArea.setText(csv);

        // And actually export to file
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Audit Logs CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv")
        );
        chooser.setInitialFileName("audit-logs.csv");

        Stage stage = (Stage) exportCsvBtn.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return; // user cancelled
        }

        try {
            Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "Export saved to:\n" + file.getAbsolutePath(),
                    ButtonType.OK);
            a.setHeaderText(null);
            a.setTitle("Export Audit Logs");
            a.showAndWait();
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR,
                    "Failed to save file: " + ex.getMessage(),
                    ButtonType.OK);
            a.setHeaderText(null);
            a.setTitle("Export Audit Logs");
            a.showAndWait();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    /** Minimal CSV escaping. */
    private static String csv(String raw) {
        if (raw == null) return "";
        String v = raw;
        boolean needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n");
        v = v.replace("\"", "\"\"");
        return needsQuotes ? "\"" + v + "\"" : v;
    }
}
