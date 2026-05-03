/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: GuiContext.java
 * Purpose:
 *   Central GUI context for sharing:
 *     • Application-wide services (Auth, Events, Registration, etc.)
 *     • Logged-in user session
 *     • Runtime access to AppConfig from any controller
 *
 * Why this exists:
 *   JavaFX controllers are instantiated by the FXML loader and therefore
 *   cannot directly receive constructor-injected dependencies.
 *   GuiContext provides a safe, minimal, global access point for:
 *     – Services configured in AppConfig
 *     – The current authenticated user (UserSession)
 *
 * Security Notes:
 *   • Only non-sensitive UserSession data is stored (ID, roles, email).
 *   • Passwords are never stored in GUI memory; only char[] is passed
 *     transiently to AuthServiceImpl.
 *   • This class does not persist data — it is wiped when the application closes.
 */
package uk.ac.aru.campusevents.ui;

import uk.ac.aru.campusevents.service.*;
import uk.ac.aru.campusevents.dto.UserSession;

public final class GuiContext {

    // ------------------------ Internal State ------------------------

    /** Reference to the global AppConfig created at application startup. */
    private static AppConfig config;

    /** Stores the currently authenticated user (null when logged out). */
    private static UserSession currentUser;

    /**
     * Initializes the global context.
     * Called once during HelloApplication.start().
     *
     * @param appConfig Fully constructed AppConfig containing all repositories
     *                  and services.
     */
    public static void init(AppConfig appConfig) {
        config = appConfig;
    }

    /**
     * Provides access to the application-wide {@link AuditService}.
     * Used by admin-only screens (e.g., Audit Logs) to view recorded actions.
     */
    public static AuditService auditService() {
        return config.auditService;
    }



    // ------------------------ User Session ------------------------

    public static UserSession getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(UserSession session) {
        currentUser = session;
    }


    // ------------------------ Service Getters ------------------------

    public static AuthService authService() {
        return config.authService;
    }

    public static EventService eventService() {
        return config.eventService;
    }

    public static RegistrationService registrationService() {
        return config.registrationService;
    }

    public static FeedbackService feedbackService() {
        return config.feedbackService;
    }

    public static ReportingService reportingService() {
        return config.reportingService;
    }

    public static CsvExportService csvExportService() {
        return config.csvExportService;
    }

    public static NotificationService notificationService() {
        return config.notificationService;
    }

    private GuiContext() {}
}
