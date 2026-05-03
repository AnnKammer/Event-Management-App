/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: CreateAccountPageController.java
 * Purpose:
 *   JavaFX controller for the "Create Account" screen.
 */

package uk.ac.aru.campusevents.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.service.AuthService;
import uk.ac.aru.campusevents.ui.GuiContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class CreateAccountPageController {

    @FXML private Button GoToLogIn;
    @FXML private Button ValidateLogIn;
    @FXML private TextField firstnameInput;
    @FXML private TextField surnameInput;
    @FXML private TextField emailInput;
    @FXML private ComboBox<String> studentInput;
    @FXML private ComboBox<String> organizerInput;
    @FXML private ComboBox<String> adminInput;
    @FXML private PasswordField createPasswordInput;

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------
    @FXML
    private void initialize() {
        if (studentInput != null) {
            studentInput.getItems().setAll("YES", "NO");
            studentInput.setValue("YES");
        }
        if (organizerInput != null) {
            organizerInput.getItems().setAll("YES", "NO");
            organizerInput.setValue("NO");
        }
        if (adminInput != null) {
            adminInput.getItems().setAll("YES", "NO");
            adminInput.setValue("NO");
        }
    }

    private AuthService authService() {
        return GuiContext.authService();
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------
    /**
     * Navigates back to the Log In page.
     * Invoked when the user clicks the "Log In" button or after successful registration.
     */
    @FXML
    private void openLogInPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/views/LogInPage-view.fxml")
            );

            // Inject AuthService into LogInPageController
            loader.setControllerFactory(type -> {
                if (type == uk.ac.aru.campusevents.controller.LogInPageController.class) {
                    return new uk.ac.aru.campusevents.controller.LogInPageController(
                            GuiContext.authService()
                    );
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Unable to construct controller: " + type, e);
                }
            });

            Scene loginScene = new Scene(loader.load(), 900, 600);

            // SAFELY resolve the current Stage from any known control
            Control anyControl =
                    firstnameInput != null ? firstnameInput :
                            surnameInput   != null ? surnameInput   :
                                    emailInput     != null ? emailInput     :
                                            GoToLogIn; // last fallback

            if (anyControl == null || anyControl.getScene() == null) {
                throw new IllegalStateException("Cannot resolve current window: no attached controls.");
            }

            Stage stage = (Stage) anyControl.getScene().getWindow();
            stage.setScene(loginScene);
            stage.setTitle("Campus Events – Log In");

        } catch (IOException ioEx) {
            showErrorAlert(
                    "Navigation Error",
                    "Unable to open the Log In page.",
                    "Technical details: " + ioEx.getMessage()
            );
        } catch (Exception ex) {
            showErrorAlert(
                    "Navigation Error",
                    "An unexpected error occurred while opening the Log In page.",
                    "Technical details: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
        }
    }

    // -------------------------------------------------------------------------
    // Registration logic
    // -------------------------------------------------------------------------
    @FXML
    private void ValidateAccountInfo() {
        String firstName = safeTrim(firstnameInput.getText());
        String lastName  = safeTrim(surnameInput.getText());
        String email     = safeTrim(emailInput.getText());
        String password  = createPasswordInput.getText();

        // Validate name
        if (firstName.isEmpty() || lastName.isEmpty()) {
            showErrorAlert("Validation Error", "Name missing.",
                    "Please enter both your first and last name.");
            return;
        }

        // Validate email
        if (!isValidEmail(email)) {
            showErrorAlert("Validation Error", "Invalid email.",
                    "Please enter a valid email such as user@example.com.");
            return;
        }

        // Simplified password rule: ONLY length >= 8
        if (password == null || password.trim().length() < 8) {
            showErrorAlert("Validation Error", "Password too short.",
                    "Password must be at least 8 characters long.");
            return;
        }

        // Validate roles
        Set<Role> roles = buildSelectedRoles();
        if (roles.isEmpty()) {
            showErrorAlert("Validation Error", "No role selected.",
                    "Please select at least one role.");
            return;
        }

        // Register
        char[] passwordChars = password.toCharArray();
        try {
            int newUserId = authService().registerUser(
                    firstName, lastName, email, passwordChars, roles
            );

            showInfoAlert("Registration Successful",
                    "Your account has been created.",
                    "Your user ID is: " + newUserId +
                            "\nYou can now log in using your email and password.");

            // Go back to Log In page
            openLogInPage();

        } catch (IllegalStateException dupEmailEx) {
            showErrorAlert("Registration Failed",
                    "Email already registered.",
                    "");
        } catch (Exception ex) {
            showErrorAlert("Registration Failed",
                    "An unexpected error occurred.",
                    ex.getMessage());
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private Set<Role> buildSelectedRoles() {
        Set<Role> roles = new HashSet<>();

        if ("YES".equalsIgnoreCase(valueOrEmpty(studentInput)))
            roles.add(Role.STUDENT);

        if ("YES".equalsIgnoreCase(valueOrEmpty(organizerInput)))
            roles.add(Role.ORGANIZER);

        if ("YES".equalsIgnoreCase(valueOrEmpty(adminInput)))
            roles.add(Role.ADMIN);

        return roles;
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private static String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static String valueOrEmpty(ComboBox<String> combo) {
        if (combo == null) return "";
        String val = combo.getValue();
        return (val == null) ? "" : val.trim();
    }

    private void showInfoAlert(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }
}
