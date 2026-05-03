package uk.ac.aru.campusevents.database;

/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: TableCreator.java
 * This class initialises the PostgreSQL database schema for development:
 *   - Creates required ENUM types (role, event_category, event_status, reg_status, audit_action, audit_entity)
 *   - Creates all core tables (app_user, organization, event, registration, notification, feedback, audit_log, etc.)
 *
 * IMPORTANT:
 *   This is aligned with the main campusevent schema:
 *     • audit_log.created_at (NOT ts)
 *     • organization_id (not org_id)
 *     • JSONB/TIMESTAMPTZ where expected
 */
public class TableCreator {

    /**
     * Creates an ENUM type if it does not already exist.
     */
    public static boolean createEnumType(DB db, String enumTypeName, String[] enumOptions) {
        String mainStatement = "CREATE TYPE " + enumTypeName + " AS ENUM(";
        for (int i = 0; i < enumOptions.length; i++) {
            mainStatement += "'" + enumOptions[i] + "'";
            if (i < enumOptions.length - 1) {
                mainStatement += ",";
            }
        }
        mainStatement += ");";

        String fullStatement = """
                DO $$
                BEGIN
                """ + mainStatement + """
                EXCEPTION
                    WHEN duplicate_object THEN
                        NULL;
                END$$;""";

        return db.runSQLUpdate(fullStatement, true);
    }

    public static boolean createSchema(DB db, boolean silent) {
        if (!silent) {
            System.out.println("Initialising database schema...");
        }

        // ---------------------------------------------------------------------
        // ENUM TYPES
        // ---------------------------------------------------------------------
        createEnumType(db, "role", new String[]{"STUDENT", "ORGANIZER", "ADMIN"});

        createEnumType(db, "event_category", new String[]{
                "WORKSHOP", "LECTURE", "SOCIAL", "SPORTS",
                "ENVIRONMENT", "ACADEMIC", "OTHER"
        });

        createEnumType(db, "event_status", new String[]{
                "DRAFT", "OPEN", "FULL", "COMPLETED", "ARCHIVED"
        });

        createEnumType(db, "reg_status", new String[]{
                "REGISTERED", "CANCELLED", "WAITLISTED", "APPROVED"
        });

        createEnumType(db, "audit_action", new String[]{
                "CREATE",
                "UPDATE",
                "DELETE",
                "REGISTER",
                "CANCEL",
                "PROMOTE",
                "FEEDBACK_SUBMIT",
                "NOTIFY",
                "EXPORT",
                "LOGIN_OK",
                "LOGIN_FAIL"
        });

        createEnumType(db, "audit_entity", new String[]{
                "USER",
                "EVENT",
                "REGISTRATION",
                "ORGANIZATION"
        });

        // ---------------------------------------------------------------------
        // TABLE CREATION
        // --------------------------------------------------------------------
        String[] tableStatements = {

                // USERS
                "CREATE TABLE IF NOT EXISTS app_user (" +
                        "user_id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                        "first_name    VARCHAR(100) NOT NULL, " +
                        "last_name     VARCHAR(100) NOT NULL, " +
                        "email         VARCHAR(254) NOT NULL UNIQUE, " +
                        "password_hash TEXT NOT NULL, " +
                        "created_at    TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ");",

                // GLOBAL USER ROLES
                "CREATE TABLE IF NOT EXISTS user_role (" +
                        "user_id INT  NOT NULL, " +
                        "role    role NOT NULL, " +
                        "PRIMARY KEY (user_id, role), " +
                        "CONSTRAINT fk_user_role_user " +
                        "  FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE" +
                        ");",

                // ORGANIZATIONS
                "CREATE TABLE IF NOT EXISTS organization (" +
                        "organization_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                        "name            VARCHAR(100) NOT NULL UNIQUE, " +
                        "type            VARCHAR(50)  NOT NULL, " +
                        "notes           TEXT NOT NULL DEFAULT ''" +
                        ");",

                // ORGANIZATION APPROVAL
                "CREATE TABLE IF NOT EXISTS organization_approval (" +
                        "organization_id INT NOT NULL, " +
                        "email           VARCHAR(254) NOT NULL, " +
                        "org_role        TEXT NOT NULL CHECK (org_role IN ('OWNER','MANAGER')), " +
                        "created_at      TIMESTAMPTZ NOT NULL DEFAULT now(), " +
                        "PRIMARY KEY (organization_id, email), " +
                        "CONSTRAINT fk_org_approval_org " +
                        "  FOREIGN KEY (organization_id) REFERENCES organization(organization_id) ON DELETE CASCADE" +
                        ");",

                // USER–ORGANIZATION MEMBERSHIPS
                "CREATE TABLE IF NOT EXISTS user_organization (" +
                        "user_id         INT NOT NULL, " +
                        "organization_id INT NOT NULL, " +
                        "org_role        TEXT NOT NULL CHECK (org_role IN ('OWNER','MANAGER')), " +
                        "PRIMARY KEY (user_id, organization_id), " +
                        "CONSTRAINT fk_user_org_user " +
                        "  FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE, " +
                        "CONSTRAINT fk_user_org_org " +
                        "  FOREIGN KEY (organization_id) REFERENCES organization(organization_id) ON DELETE CASCADE" +
                        ");",

                // EVENTS
                "CREATE TABLE IF NOT EXISTS event (" +
                        "event_id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                        "organizer_user_id   INT, " +
                        "organization_id     INT, " +
                        "title               VARCHAR(200) NOT NULL, " +
                        "description         TEXT NOT NULL DEFAULT '', " +
                        "category            event_category NOT NULL, " +
                        "category_other_desc TEXT NOT NULL DEFAULT '', " +
                        "status              event_status NOT NULL DEFAULT 'DRAFT', " +
                        "start_at            TIMESTAMPTZ, " +
                        "end_at              TIMESTAMPTZ, " +
                        "location            VARCHAR(200), " +
                        "capacity            INT NOT NULL CHECK (capacity > 0), " +
                        "created_at          TIMESTAMPTZ NOT NULL DEFAULT now(), " +
                        "updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(), " +

                        "CONSTRAINT fk_event_organizer " +
                        "  FOREIGN KEY (organizer_user_id) REFERENCES app_user(user_id) ON DELETE SET NULL, " +
                        "CONSTRAINT fk_event_organization " +
                        "  FOREIGN KEY (organization_id) REFERENCES organization(organization_id) ON DELETE SET NULL, " +

                        "CONSTRAINT event_owner_xor " +
                        "  CHECK ((organizer_user_id IS NOT NULL AND organization_id IS NULL) " +
                        "      OR (organizer_user_id IS NULL AND organization_id IS NOT NULL))" +
                        ");",

                // REGISTRATIONS
                "CREATE TABLE IF NOT EXISTS registration (" +
                        "registration_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                        "event_id        INT NOT NULL, " +
                        "student_id      INT NOT NULL, " +
                        "status          reg_status NOT NULL, " +
                        "registered_at   TIMESTAMPTZ NOT NULL DEFAULT now(), " +
                        "CONSTRAINT fk_reg_event " +
                        "  FOREIGN KEY (event_id)   REFERENCES event(event_id) ON DELETE CASCADE, " +
                        "CONSTRAINT fk_reg_student " +
                        "  FOREIGN KEY (student_id) REFERENCES app_user(user_id) ON DELETE CASCADE" +
                        ");",

                // NOTIFICATIONS
                "CREATE TABLE IF NOT EXISTS notification (" +
                        "notification_id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                        "recipient_user_id INT NOT NULL, " +
                        "message           VARCHAR(300) NOT NULL, " +
                        "created_at        TIMESTAMPTZ NOT NULL DEFAULT now(), " +
                        "is_read           BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "CONSTRAINT fk_notif_user " +
                        "  FOREIGN KEY (recipient_user_id) REFERENCES app_user(user_id) ON DELETE CASCADE" +
                        ");",

                // FEEDBACK
                "CREATE TABLE IF NOT EXISTS feedback (" +
                        "feedback_id  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                        "event_id     INT NOT NULL, " +
                        "student_id   INT NOT NULL, " +
                        "rating       SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5), " +
                        "comment      TEXT NOT NULL DEFAULT '', " +
                        "created_at   TIMESTAMPTZ NOT NULL DEFAULT now(), " +
                        "CONSTRAINT fk_feedback_event " +
                        "  FOREIGN KEY (event_id)   REFERENCES event(event_id) ON DELETE CASCADE, " +
                        "CONSTRAINT fk_feedback_user " +
                        "  FOREIGN KEY (student_id) REFERENCES app_user(user_id) ON DELETE CASCADE, " +
                        "CONSTRAINT uniq_feedback_once UNIQUE (event_id, student_id)" +
                        ");",

                // AUDIT LOGS
                "CREATE TABLE IF NOT EXISTS audit_log (" +
                        "audit_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                        "created_at    TIMESTAMPTZ NOT NULL DEFAULT now(), " +   // <-- FIXED
                        "action        audit_action NOT NULL, " +
                        "entity        audit_entity NOT NULL, " +
                        "entity_id     INT, " +
                        "actor_user_id INT REFERENCES app_user(user_id), " +
                        "details_json  JSONB NOT NULL DEFAULT '{}'::jsonb" +     // JSONB per schema
                        ");"
        };

        for (String sql : tableStatements) {
            if (!db.runSQLUpdate(sql)) {
                System.err.println("Failed to create table with SQL: " + sql);
                return false;
            }
        }

        if (!silent) {
            System.out.println("Database schema initialised successfully.");
        }
        return true;
    }

    public static boolean createSchema(DB db) {
        return createSchema(db, true);
    }
}
