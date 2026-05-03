/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: HelloApplication.java
 *
 * Purpose:
 *   Primary JavaFX entry point. Responsible for:
 *     • Initializing the application-wide configuration (repositories and services).
 *     • Establishing the GUI context, including dependency injection for controllers.
 *     • Loading and presenting the initial user interface scene (Login Page).
 *
 * Design Notes:
 *   • The application employs explicit dependency injection to ensure that UI controllers
 *     receive the appropriate service implementations at runtime.
 *   • All service-level authorization and validation mechanisms are encapsulated within
 *     the business logic layer to preserve separation of concerns.
 */

package uk.ac.aru.campusevents.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import uk.ac.aru.campusevents.controller.LogInPageController;

public class HelloApplication extends Application {

    /** Global configuration container providing all repositories and services. */
    private static AppConfig config;

    /** Public accessor for components that require application-wide configuration. */
    public static AppConfig getConfig() {
        return config;
    }

    @Override
    public void start(Stage stage) {

        // ---------------------------------------------------------------------
        // 1. Instantiate the application configuration (backend initialization)
        // ---------------------------------------------------------------------
        config = new AppConfig();
        GuiContext.init(config);

        // ---------------------------------------------------------------------
        // 2. Prepare FXMLLoader for the initial UI view
        // ---------------------------------------------------------------------
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ui/views/LogInPage-view.fxml")
        );

        // ---------------------------------------------------------------------
        // 3. Configure dependency injection for UI controllers
        // ---------------------------------------------------------------------
        loader.setControllerFactory(type -> {
            if (type == LogInPageController.class) {
                return new LogInPageController(config.authService);
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Unable to instantiate controller: " + type.getName(), e
                );
            }
        });

        // ---------------------------------------------------------------------
        // 4. Load and display the primary scene
        // ---------------------------------------------------------------------
        try {
            Scene scene = new Scene(loader.load(), 900, 600);
            stage.setTitle("Team Lentil – Campus Events");
            stage.setScene(scene);
            stage.show();

        } catch (Exception ex) {
            // This block will reveal the actual FXML loading failure.
            System.err.println("Critical GUI initialization error:");
            ex.printStackTrace();
            return;
        }
    }
}
