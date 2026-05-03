/*
    ================================================================
    MOD004881 – Object Oriented Programming
    Element 010 – Team Project: Campus Event Management System
    Team Name: Team Lentil
    File: Main.java
    Purpose:
        End-to-end demo:

          • Database bootstrap (PostgreSQL, campusevent DB)
              - Connect via DB helper
              - Create enums & tables via TableCreator
              - Print existing users in app_user

          • Business logic demo (JDBC-based):
              - Auth (BCrypt) + RBAC
              - Personal & organization-owned events
              - Registration (simple capacity)
              - In-app notifications
              - Search by category/text/date/location
              - CSV export (attendees + “my events”)
              - Feedback + reporting
              - Event update notifications
              - Feedback reminders

    Security notes:
        • AuthServiceImpl stores BCrypt hashes for demo users created here.
        • Role checks in services; org-scoped RBAC for org-owned events.
        • Immutable identities; repos reconstruct on create.
        • CSV escaping to avoid injection issues.
        • Notifications never include sensitive data; GUI can deep-link via tokens.
    ================================================================
*/
package uk.ac.aru.campusevents;

import uk.ac.aru.campusevents.database.DB;
import uk.ac.aru.campusevents.database.TableCreator;
import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.Organization;
import uk.ac.aru.campusevents.domain.UserOrganization;
import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.domain.enums.EventCategory;
import uk.ac.aru.campusevents.dto.EventSearchCriteria;
import uk.ac.aru.campusevents.dto.UserSession;
import uk.ac.aru.campusevents.export.CsvExportServiceImpl;

// JDBC repositories
import uk.ac.aru.campusevents.repository.jdbc.*;
import uk.ac.aru.campusevents.repository.*;

// Services
import uk.ac.aru.campusevents.service.*;
import uk.ac.aru.campusevents.service.impl.*;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Team Lentil Campus Event Management Demo ===");

        // ---------------------------------------------------------------------
        // 0) DATABASE BOOTSTRAP (PostgreSQL: campusevent)
        // ---------------------------------------------------------------------
        DB myDB = new DB(); // uses defaults from DB class

        if (!myDB.connect(false)) {
            System.err.println("FATAL: Could not connect to database. Exiting.");
            return;
        }

        // Create enums + tables if they don't exist yet (idempotent).
        if (!TableCreator.createSchema(myDB, false)) {
            System.err.println("FATAL: Failed to initialise database schema. Exiting.");
            myDB.disconnect(false);
            return;
        }

        // Print whatever is already in app_user (from schema seed etc.)
        System.out.println("\n--- Database demo: existing contents of app_user ---");
        ResultSet rs = myDB.runSQLQuery(
                "SELECT user_id, first_name, last_name, email, created_at " +
                        "FROM app_user ORDER BY user_id;",
                false
        );
        myDB.printResultSet(rs);

        // ---------------------------------------------------------------------
        // 1) Repositories (JDBC)
        // ---------------------------------------------------------------------
        JdbcUserRepository userRepo                = new JdbcUserRepository(myDB);
        JdbcEventRepository eventRepo              = new JdbcEventRepository(myDB);
        JdbcRegistrationRepository regRepo         = new JdbcRegistrationRepository(myDB);
        JdbcNotificationRepository notifRepo       = new JdbcNotificationRepository(myDB);
        JdbcOrganizationRepository orgRepo         = new JdbcOrganizationRepository(myDB);
        JdbcUserOrganizationRepository userOrgRepo = new JdbcUserOrganizationRepository(myDB);
        JdbcFeedbackRepository feedbackRepo        = new JdbcFeedbackRepository(myDB);
        JdbcOrganizationApprovalRepository orgApprovalRepo =
                new JdbcOrganizationApprovalRepository(myDB);

        // ---------------------------------------------------------------------
        // 2) Services (wire all dependencies)
        // ---------------------------------------------------------------------
        AuthService authService          = new AuthServiceImpl(userRepo, userOrgRepo, orgApprovalRepo);
        NotificationService notifService = new NotificationServiceImpl(notifRepo);
        EventService eventService        = new EventServiceImpl(
                eventRepo, userRepo, orgRepo, userOrgRepo, orgApprovalRepo, regRepo, notifService);
        RegistrationService regs         = new RegistrationServiceImpl(
                userRepo, eventRepo, regRepo, notifService);
        CsvExportService csvExport       = new CsvExportServiceImpl(eventRepo, regRepo, userRepo);
        FeedbackService feedback         = new FeedbackServiceImpl(feedbackRepo, regRepo, eventRepo, userRepo);
        ReportingService reporting       = new ReportingServiceImpl(eventRepo, regRepo, feedbackRepo);
        ReminderService reminderService  = new ReminderServiceImpl(eventRepo, regRepo, feedbackRepo, notifService);

        // ---------------------------------------------------------------------
        // 3) Demo users (created via AuthService → BCrypt passwords)
        //    Use UNIQUE demo emails so we never clash with seeded users.
        // ---------------------------------------------------------------------
        Set<Role> organizerRoles = Set.of(Role.ORGANIZER);
        Set<Role> studentRoles   = Set.of(Role.STUDENT);

        int orgUserId = ensureUser(authService, userRepo,
                "Bob", "Builder", "demo-org@campusevents.local",
                "StrongPwd1!".toCharArray(), organizerRoles);

        int studentId = ensureUser(authService, userRepo,
                "Alice", "Wonderland", "demo-alice@campusevents.local",
                "StudyHard!".toCharArray(), studentRoles);

        int s2 = ensureUser(authService, userRepo,
                "Eve", "Edan", "demo-eve@campusevents.local",
                "EvePass1!".toCharArray(), studentRoles);

        int s3 = ensureUser(authService, userRepo,
                "Charlie", "Citrus", "demo-charlie@campusevents.local",
                "CharPwd1!".toCharArray(), studentRoles);

        // ---------------------------------------------------------------------
        // 4) Organization + membership
        // ---------------------------------------------------------------------
        int ngoId = ensureOrganization(orgRepo, "Green Earth NGO", "NGO");
        userOrgRepo.add(new UserOrganization(orgUserId, ngoId, "OWNER"));

        // ---------------------------------------------------------------------
        // 5) Login (organizer)
        // ---------------------------------------------------------------------
        UserSession orgSession = authService.login(
                "demo-org@campusevents.local",
                "StrongPwd1!".toCharArray()
        );
        System.out.println("Logged in as: " + orgSession.name() + " Roles: " + orgSession.roles());

        // ---------------------------------------------------------------------
// 6) Create events (personal + org-owned)
// ---------------------------------------------------------------------
        Event personalEvent = Event.newPersonalEvent(
                orgUserId,
                "Java Bootcamp",
                "Intro + labs",
                EventCategory.WORKSHOP,   // enum instead of string
                "",                       // categoryOtherDescription (only used for OTHER)
                30                        // capacity
        );
        int personalEventId = eventService.createEvent(orgUserId, personalEvent);

        Event orgEvent = Event.newOrgEvent(
                ngoId,
                "Tree Planting Day",
                "Campus-wide volunteer event",
                EventCategory.ENVIRONMENT,  // enum instead of string
                "",                         // categoryOtherDescription (not used)
                50                          // capacity
        );
        int orgEventId = eventService.createEvent(orgUserId, orgEvent);

// Set dates/locations (for search + notifications)
        var now = LocalDateTime.now();

        var pe = eventRepo.findById(personalEventId).orElseThrow();
        pe.setStartDateTime(now.plusDays(7));
        pe.setEndDateTime(now.plusDays(7).plusHours(2));
        pe.setLocation("Cambridge Campus, Lab 3.21");
        eventRepo.update(pe);

        var oe = eventRepo.findById(orgEventId).orElseThrow();
        oe.setStartDateTime(now.plusDays(3));
        oe.setEndDateTime(now.plusDays(3).plusHours(3));
        oe.setLocation("Cambridge Campus, Courtyard");
        eventRepo.update(oe);

        System.out.println("\nCreated events:");
        System.out.println("- Personal:  #" + personalEventId + "  (" + pe.getTitle() + ")");
        System.out.println("- Organization-owned: #" + orgEventId + " (" + oe.getTitle() + ")");

        // ---------------------------------------------------------------------
        // 7) Registrations + notifications
        // ---------------------------------------------------------------------
        regs.register(studentId, orgEventId);
        printAndClearNotifications(notifService, studentId, "Alice after register");

        regs.register(s2, orgEventId);
        printAndClearNotifications(notifService, s2, "Eve after register");

        regs.register(s3, orgEventId);
        printAndClearNotifications(notifService, s3, "Charlie after register");

        System.out.println("\nAttendees after registration: " + regs.listAttendeeUserIds(orgEventId));

        // Alice cancels → if we had a waitlist, someone could be promoted
        regs.cancel(studentId, orgEventId);
        printAndClearNotifications(notifService, studentId, "Alice after cancel");
        printAndClearNotifications(notifService, s3, "Charlie after possible promotion");

        System.out.println("Attendees after promotion: " + regs.listAttendeeUserIds(orgEventId));

        // ---------------------------------------------------------------------
        // 8) Search (category/text/date/location)
        // ---------------------------------------------------------------------
        var results = eventService.search(new EventSearchCriteria(
                null,
                "tree",
                now.toLocalDate(),
                now.toLocalDate().plusDays(10),
                "Cambridge"
        ));
        System.out.println("\n=== Search Results (tree within 10 days, Cambridge) ===");
        results.forEach(ev -> System.out.println(
                "- " + ev.getTitle()
                        + " (orgId=" + ev.getOrganizationId()
                        + "), when=" + ev.getStartDateTime()
                        + ", where=" + ev.getLocation()
        ));

        // ---------------------------------------------------------------------
        // 9) CSV Exports
        // ---------------------------------------------------------------------
        String attendeesCsv = csvExport.exportAttendeesCsv(orgEventId);
        System.out.println("\n=== CSV Export – Attendees (Tree Planting Day) ===\n" + attendeesCsv);

        String myCsv = csvExport.exportMyEventsCsv(s2);
        System.out.println("\n=== CSV Export – Eve's My Events ===\n" + myCsv);

        // ---------------------------------------------------------------------
        // 10) Feedback + Reporting
        // ---------------------------------------------------------------------
        feedback.submit(s2, orgEventId, 5, "Loved it!");
        feedback.submit(s3, orgEventId, 4, "Great team vibes.");
        System.out.println("\nAverage rating: " + feedback.averageRating(orgEventId).orElse(0.0));

        System.out.println("\n=== Reporting: Event Summary ===");
        System.out.println(reporting.eventSummary(orgEventId));

        System.out.println("\n=== Reporting: Organizer Portfolio ===");
        System.out.println(reporting.organizerPortfolio(orgUserId));

        // ---------------------------------------------------------------------
        // 11) Event update → notify attendees
        // ---------------------------------------------------------------------
        var toUpdate = eventRepo.findById(orgEventId).orElseThrow();
        toUpdate.setLocation("Cambridge Campus, Great Hall");         // locationChanged
        toUpdate.setStartDateTime(oe.getStartDateTime().plusDays(1)); // timeChanged
        eventService.updateEvent(orgUserId, toUpdate);
        printAndClearNotifications(notifService, s2, "Eve after event update");
        printAndClearNotifications(notifService, s3, "Charlie after event update");

        // ---------------------------------------------------------------------
        // 12) Feedback Reminders
        // ---------------------------------------------------------------------
        reminderService.sendFeedbackRemindersForEvent(orgEventId);
        printAndClearNotifications(notifService, s2, "Eve (feedback reminder)");
        printAndClearNotifications(notifService, s3, "Charlie (feedback reminder)");

        // ---------------------------------------------------------------------
        // 13) RBAC-protected delete (personal event)
        // ---------------------------------------------------------------------
        eventService.deleteEvent(orgUserId, personalEventId);
        System.out.println("Deleted personal event id " + personalEventId + " (RBAC OK)");

        // ---------------------------------------------------------------------
        // 14) Done
        // ---------------------------------------------------------------------
        System.out.println("\nDemo completed successfully.");

        myDB.disconnect(false);
    }

    /**
     * Helper: make demo user creation idempotent.
     * If the email already exists, reuse that user’s id instead of failing.
     */
    private static int ensureUser(AuthService authService,
                                  uk.ac.aru.campusevents.repository.UserRepository userRepo,
                                  String firstName,
                                  String lastName,
                                  String email,
                                  char[] password,
                                  Set<Role> roles) {
        try {
            return authService.registerUser(firstName, lastName, email, password, roles);
        } catch (IllegalStateException ex) {
            // Email already exists → reuse existing user
            return userRepo.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalStateException(
                            "Email exists but user cannot be loaded: " + email))
                    .getId();
        }
    }

    /**
     * Helper: make organization creation idempotent.
     * If an org with the same name already exists, reuse its id.
     */
    private static int ensureOrganization(OrganizationRepository orgRepo,
                                          String name,
                                          String type) {
        return orgRepo.findByName(name)
                .map(Organization::getId)
                .orElseGet(() -> orgRepo.create(new Organization(0, name, type)));
    }

    // Utility: display and clear unread notifications
    private static void printAndClearNotifications(NotificationService notifService, int userId, String label) {
        var unread = notifService.listUnread(userId);
        System.out.println("\n-- Notifications for " + label + " --");
        if (unread.isEmpty()) {
            System.out.println("(none)");
        } else {
            unread.forEach(n -> System.out.println("- " + n.getMessage()));
            notifService.markAllRead(userId);
        }
    }

}
