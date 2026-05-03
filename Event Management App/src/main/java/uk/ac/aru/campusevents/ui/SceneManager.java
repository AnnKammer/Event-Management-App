/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: SceneManager.java
 * Purpose:
 *   Utility class that centralizes JavaFX scene switching for FXML-based views.
 *
 * Responsibilities:
 *   • Load FXML resources relative to HelloApplication
 *   • Replace the current Scene on the active Stage
 *   • Update the window title when the Scene is changed
 *
 * Why this exists:
 *   Several controllers need to navigate between views (for example, from the
 *   Student dashboard to the Upcoming Events view). Instead of duplicating
 *   FXML loading and Stage handling logic in each controller, this helper
 *   provides a single, reusable method for consistent navigation behavior.
 *
 * Notes:
 *   • This class only deals with presentation concerns and does not access
 *     repositories, services, or the database.
 *   • All FXML paths must be valid resources on the JavaFX classpath.
 */

package uk.ac.aru.campusevents.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public final class SceneManager {

    /**
     * Switches the current JavaFX Scene to the view defined by the given FXML file.
     * The Stage is resolved from the source Node, so controllers can call this
     * method from any control that is already attached to a Scene.
     *
     * @param source   the UI node that triggered the navigation (for example, a Button)
     * @param fxmlPath the FXML resource path. If it does not start with '/',
     *                 it is resolved under "/ui/views/" for convenience.
     * @param title    the window title to display for the new Scene
     */
    public static void switchScene(Node source, String fxmlPath, String title) {
        if (source == null || source.getScene() == null) {
            // Defensive check to avoid NullPointerException if the node is not attached.
            return;
        }

        try {
            // Normalize the path: if the caller passed just "StudentDashboard-view.fxml",
            String resolvedPath = fxmlPath.startsWith("/")
                    ? fxmlPath
                    : "/ui/views/" + fxmlPath;

            URL resource = HelloApplication.class.getResource(resolvedPath);
            if (resource == null) {
                // Helpful message if the FXML cannot be found on the classpath.
                throw new IllegalStateException(
                        "FXML not found on classpath: " + resolvedPath
                );
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) source.getScene().getWindow();

            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Private constructor to prevent instantiation of this utility class.
    private SceneManager() {
    }
}
