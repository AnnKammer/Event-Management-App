/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AppConfig.java
 * Purpose:
 *   Central configuration class wiring repositories and services
 *   for JDBC/PostgreSQL persistence and GUI integration.
 *
 * Security Notes:
 *   • No plaintext passwords — AuthServiceImpl uses BCrypt hashing.
 *   • RBAC is enforced at service level.
 *   • JDBC repositories use parameterised SQL via the DB helper.
 */

package uk.ac.aru.campusevents.ui;

import uk.ac.aru.campusevents.export.CsvExportServiceImpl;

// Service interfaces
import uk.ac.aru.campusevents.service.AuthService;
import uk.ac.aru.campusevents.service.AuditService;
import uk.ac.aru.campusevents.service.CsvExportService;
import uk.ac.aru.campusevents.service.EventService;
import uk.ac.aru.campusevents.service.FeedbackService;
import uk.ac.aru.campusevents.service.NotificationService;
import uk.ac.aru.campusevents.service.RegistrationService;
import uk.ac.aru.campusevents.service.ReportingService;

// Core implementations
import uk.ac.aru.campusevents.service.impl.AuthServiceImpl;
import uk.ac.aru.campusevents.service.impl.EventServiceImpl;
import uk.ac.aru.campusevents.service.impl.FeedbackServiceImpl;
import uk.ac.aru.campusevents.service.impl.NotificationServiceImpl;
import uk.ac.aru.campusevents.service.impl.RegistrationServiceImpl;
import uk.ac.aru.campusevents.service.impl.ReportingServiceImpl;

// Auditing service + decorators
import uk.ac.aru.campusevents.service.impl.AuditServiceImpl;
import uk.ac.aru.campusevents.service.impl.AuditingAuthService;
import uk.ac.aru.campusevents.service.impl.AuditingEventService;
import uk.ac.aru.campusevents.service.impl.AuditingRegistrationService;
import uk.ac.aru.campusevents.service.impl.NoOpAuditService;

// DB helper
import uk.ac.aru.campusevents.database.DB;

// JDBC repositories
import uk.ac.aru.campusevents.repository.jdbc.JdbcUserRepository;
import uk.ac.aru.campusevents.repository.jdbc.JdbcEventRepository;
import uk.ac.aru.campusevents.repository.jdbc.JdbcRegistrationRepository;
import uk.ac.aru.campusevents.repository.jdbc.JdbcNotificationRepository;
import uk.ac.aru.campusevents.repository.jdbc.JdbcOrganizationRepository;
import uk.ac.aru.campusevents.repository.jdbc.JdbcUserOrganizationRepository;
import uk.ac.aru.campusevents.repository.jdbc.JdbcFeedbackRepository;
import uk.ac.aru.campusevents.repository.jdbc.JdbcOrganizationApprovalRepository;
import uk.ac.aru.campusevents.repository.jdbc.JdbcAuditRepository;

// Repository interfaces (ports)
import uk.ac.aru.campusevents.repository.AuditRepository;
import uk.ac.aru.campusevents.repository.EventRepository;
import uk.ac.aru.campusevents.repository.FeedbackRepository;
import uk.ac.aru.campusevents.repository.NotificationRepository;
import uk.ac.aru.campusevents.repository.OrganizationApprovalRepository;
import uk.ac.aru.campusevents.repository.OrganizationRepository;
import uk.ac.aru.campusevents.repository.RegistrationRepository;
import uk.ac.aru.campusevents.repository.UserOrganizationRepository;
import uk.ac.aru.campusevents.repository.UserRepository;

public final class AppConfig {

    /**
     * Toggle auditing behaviour:
     *  true  = use real AuditService with JdbcAuditRepository
     *  false = use NoOpAuditService (no auditing)
     */
    public static final boolean USE_AUDIT = true;

    // ---------------- DB helper ----------------

    /**
     * Single DB helper instance shared by all JDBC repositories.
     *
     * NOTE: if your DB class is a singleton (e.g. DB.getInstance()) or
     * requires URL/credentials, adjust this line accordingly.////////////////////////////////////
     */
    private final DB db = new DB();

    // ---------------- Repositories (all JDBC) ----------------

    public final UserRepository userRepo                = new JdbcUserRepository(db);
    public final EventRepository eventRepo              = new JdbcEventRepository(db);
    public final RegistrationRepository regRepo         = new JdbcRegistrationRepository(db);
    public final NotificationRepository notifRepo       = new JdbcNotificationRepository(db);
    public final OrganizationRepository orgRepo         = new JdbcOrganizationRepository(db);
    public final UserOrganizationRepository userOrgRepo = new JdbcUserOrganizationRepository(db);
    public final FeedbackRepository feedbackRepo        = new JdbcFeedbackRepository(db);
    public final OrganizationApprovalRepository orgApprovalRepo =
            new JdbcOrganizationApprovalRepository(db);
    public final AuditRepository auditRepo              =
            USE_AUDIT ? new JdbcAuditRepository(db) : null;

    // ---------------- Core services (non-audited) --------------

    public final NotificationService notificationService =
            new NotificationServiceImpl(notifRepo);

    // AuthServiceImpl is storage-agnostic; now wired to JDBC-backed repos
    public final AuthService baseAuthService =
            new AuthServiceImpl(userRepo, userOrgRepo, orgApprovalRepo);

    public final EventService baseEventService =
            new EventServiceImpl(
                    eventRepo,
                    userRepo,
                    orgRepo,
                    userOrgRepo,
                    orgApprovalRepo,
                    regRepo,
                    notificationService
            );

    public final RegistrationService baseRegistrationService =
            new RegistrationServiceImpl(
                    userRepo,
                    eventRepo,
                    regRepo,
                    notificationService
            );

    public final CsvExportService csvExportService =
            new CsvExportServiceImpl(eventRepo, regRepo, userRepo);

    public final FeedbackService feedbackService =
            new FeedbackServiceImpl(feedbackRepo, regRepo, eventRepo, userRepo);

    // ---------------- Audit service ----------------------------

    public final AuditService auditService = USE_AUDIT
            ? new AuditServiceImpl(auditRepo, eventRepo, userRepo)
            : new NoOpAuditService();

    // ---------------- Decorated (audited) services --------------

    public final AuthService authService =
            new AuditingAuthService(baseAuthService, auditService, userRepo);

    public final EventService eventService =
            new AuditingEventService(baseEventService, auditService, eventRepo);

    public final RegistrationService registrationService =
            new AuditingRegistrationService(baseRegistrationService, auditService, regRepo, eventRepo);

    public final ReportingService reportingService =
            new ReportingServiceImpl(eventRepo, regRepo, feedbackRepo);
}
