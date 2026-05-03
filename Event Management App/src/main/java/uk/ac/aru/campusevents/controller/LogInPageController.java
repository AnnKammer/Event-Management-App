/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: LogInPageController.java
 * Purpose:
 *   JavaFX controller for the Login screen.
 *
 *   Responsibilities:
 *     • Read email + password from GUI controls defined in LogInPage-view.fxml
 *     • Delegate authentication to AuthService
 *     • Store the authenticated UserSession in GuiContext
 *     • Navigate to the Home Page (dashboard selector) on successful login
 *     • Navigate to the Create Account screen when requested
 *     • Display user-friendly error messages inside the login form
 *
 * Why this exists:
 *   FXML controllers are constructed by the JavaFX framework. To keep the GUI
 *   thin and focused on presentation logic, domain logic remains in services.
 *   The HelloApplication class provides AuthService to this controller through
 *   a custom controller factory, so the controller only handles UI events and
 *   scene transitions.
 *
 * Security Notes:
 *   • Passwords are read from the PasswordField and passed as char[] to
 *     AuthService for secure handling.
 *   • Password values are never logged or printed.
 *   • Error messages are generic and do not reveal whether the email exists.
 */

package uk.ac.aru.campusevents.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import uk.ac.aru.campusevents.dto.UserSession;
import uk.ac.aru.campusevents.service.AuthService;
import uk.ac.aru.campusevents.ui.GuiContext;

import java.io.IOException;

public class LogInPageController {

    // ------------------------ Dependencies ------------------------

    /**
     * Authentication service used to verify credentials and create UserSession.
     * This is injected by HelloApplication via FXMLLoader's controller factory.
     */
    private final AuthService authService;


    // ------------------------ FXML-Injected UI Controls ------------------------

    /** Text field for the user's email address. */
    @FXML
    private TextField emailField;

    /** Password field for the user's password input. */
    @FXML
    private PasswordField passwordField;

    /**
     * Label used to display validation errors or login failures.
     * Defined in FXML with fx:id="errorLabel".
     */
    @FXML
    private Label errorLabel;


    // ------------------------ Construction ------------------------

    /**
     * Constructs the controller with the required AuthService.
     *
     * @param authService AuthService implementation provided at runtime.
     */
    public LogInPageController(AuthService authService) {
        this.authService = authService;
    }


    // ------------------------ Event Handlers ------------------------

    /**
     * Handles clicks on the "Log In" button.
     * FXML mapping: onAction="#Login"
     *
     * @param event ActionEvent fired by the button (not used directly).
     */
    @FXML
    private void Login(ActionEvent event) {
        // Clear any previous error message
        errorLabel.setText("");

        // Read values from the GUI
        String email = emailField.getText();
        String password = passwordField.getText();

        // Basic validation before hitting the service
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            errorLabel.setText("Please enter both email and password.");
            return;
        }

        try {
            // 1) Authenticate user using the AuthService
            UserSession session = authService.login(email, password.toCharArray());

            // 2) Store session in GuiContext for the rest of the GUI
            GuiContext.setCurrentUser(session);

            // 3) Navigate to the Home Page (dashboard selector)
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/views/HomePage-view.fxml")
            );

            Scene homeScene = new Scene(loader.load(), 900, 600);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(homeScene);
            stage.setTitle("Campus Events – Home");
            // Stage is reused; no need to call show() again.

        } catch (IllegalArgumentException | SecurityException ex) {
            // Most likely exceptions for invalid credentials.
            errorLabel.setText("Invalid email or password.");
        } catch (IOException ioEx) {
            // Problem loading the home page FXML.
            errorLabel.setText("Login succeeded, but the Home Page could not be loaded.");
        } catch (Exception ex) {
            // Fallback for any unexpected error — keep message generic.
            errorLabel.setText("An unexpected error occurred during login.");
        }
    }

    /**
     * Handles clicks on the "Create Account" button.
     * FXML mapping: onAction="#handleCreateAccount"
     *
     * @param event ActionEvent fired by the button (not used directly).
     */
    @FXML
    private void handleCreateAccount(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/views/CreateAccountPage-view.fxml")
            );

            Scene registerScene = new Scene(loader.load(), 900, 600);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(registerScene);
            stage.setTitle("Campus Events – Create Account");

        } catch (IOException ioEx) {
            errorLabel.setText("Unable to open the Create Account page.");
        } catch (Exception ex) {
            errorLabel.setText("An unexpected error occurred opening the Create Account page.");
        }
    }
}
