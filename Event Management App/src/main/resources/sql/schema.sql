/* Team Lentil — Campus Event Management System (MOD004881, Element 010) */

/* ---------- ENUM TYPES ---------- */

CREATE TYPE role AS ENUM ('STUDENT','ORGANIZER','ADMIN');

CREATE TYPE org_role AS ENUM ('MANAGER', 'OWNER');

CREATE TYPE reg_status AS ENUM ('REGISTERED','CANCELLED','WAITLISTED','APPROVED');

CREATE TYPE event_category AS ENUM
    ('WORKSHOP','LECTURE','SOCIAL','SPORTS','ENVIRONMENT','ACADEMIC','OTHER');

CREATE TYPE event_status AS ENUM ('DRAFT','OPEN','FULL','COMPLETED','ARCHIVED');

CREATE TYPE audit_action AS ENUM
    ('CREATE',
        'UPDATE',
        'DELETE',
        'REGISTER',
        'CANCEL',
        'PROMOTE',
        'FEEDBACK_SUBMIT',
        'NOTIFY',
        'EXPORT',
        'LOGIN_OK',
        'LOGIN_FAIL');

CREATE TYPE audit_entity AS ENUM ('USER','EVENT','REGISTRATION','ORGANIZATION');


/* ---------- TABLES ---------- */

CREATE TABLE app_user (
                          user_id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          first_name    VARCHAR(100) NOT NULL,
                          last_name     VARCHAR(100) NOT NULL,
                          email         VARCHAR(254) NOT NULL UNIQUE,
                          password_hash TEXT NOT NULL,
                          created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE organization (
                              organization_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              name            VARCHAR(100) NOT NULL UNIQUE,
                              type            VARCHAR(50)  NOT NULL,
                              notes           TEXT NOT NULL DEFAULT ''
);

CREATE TABLE user_role (
                           user_id INT  NOT NULL,
                           role    role NOT NULL,
                           PRIMARY KEY (user_id, role),
                           CONSTRAINT fk_user_role_user
                               FOREIGN KEY (user_id)
                                   REFERENCES app_user(user_id) ON DELETE CASCADE
);

CREATE TABLE organization_approval (
                                       organization_id INT NOT NULL,
                                       email           VARCHAR(254) NOT NULL,
                                       org_role        TEXT NOT NULL CHECK (org_role IN ('OWNER','MANAGER')),
                                       created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       PRIMARY KEY (organization_id, email),
                                       CONSTRAINT fk_org_approval_org
                                           FOREIGN KEY (organization_id) REFERENCES organization(organization_id) ON DELETE CASCADE
);

CREATE TABLE user_organization (
                                   user_id         INT NOT NULL,
                                   organization_id INT NOT NULL,
                                   org_role        TEXT NOT NULL CHECK (org_role IN ('OWNER','MANAGER')),
                                   PRIMARY KEY (user_id, organization_id),
                                   CONSTRAINT fk_user_org_user
                                       FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
                                   CONSTRAINT fk_user_org_org
                                       FOREIGN KEY (organization_id) REFERENCES organization(organization_id) ON DELETE CASCADE
);

CREATE TABLE event (
                       event_id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       organizer_user_id   INT,
                       organization_id     INT,
                       title               VARCHAR(200) NOT NULL,
                       description         TEXT NOT NULL DEFAULT '',
                       category            event_category NOT NULL,
                       category_other_desc TEXT NOT NULL DEFAULT '',
                       status              event_status NOT NULL DEFAULT 'DRAFT',
                       start_at            TIMESTAMPTZ,
                       end_at              TIMESTAMPTZ,
                       location            VARCHAR(200),
                       capacity            INT NOT NULL CHECK (capacity > 0),
                       created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

                       CONSTRAINT fk_event_organizer
                           FOREIGN KEY (organizer_user_id) REFERENCES app_user(user_id) ON DELETE SET NULL,

                       CONSTRAINT fk_event_organization
                           FOREIGN KEY (organization_id) REFERENCES organization(organization_id) ON DELETE SET NULL,

    -- Exactly one owner: either personal OR organizational
                       CONSTRAINT event_owner_xor
                           CHECK (
                               (organizer_user_id IS NOT NULL AND organization_id IS NULL) OR
                               (organizer_user_id IS NULL AND organization_id IS NOT NULL)
                               )
);

CREATE OR REPLACE FUNCTION tg_set_event_updated_at()
    RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END$$;

CREATE TRIGGER set_event_updated_at
    BEFORE UPDATE ON event
    FOR EACH ROW
EXECUTE FUNCTION tg_set_event_updated_at();

CREATE INDEX idx_event_start_at ON event(start_at);
CREATE INDEX idx_event_status   ON event(status);
CREATE INDEX idx_event_category ON event(category);


CREATE TABLE registration (
                              registration_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              event_id        INT NOT NULL,
                              student_id      INT NOT NULL,
                              status          reg_status NOT NULL,
                              registered_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

                              CONSTRAINT fk_reg_event
                                  FOREIGN KEY (event_id)   REFERENCES event(event_id) ON DELETE CASCADE,
                              CONSTRAINT fk_reg_student
                                  FOREIGN KEY (student_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uniq_current_reg
    ON registration(event_id, student_id)
    WHERE status <> 'CANCELLED';

CREATE INDEX idx_reg_event   ON registration(event_id);
CREATE INDEX idx_reg_student ON registration(student_id);
CREATE INDEX idx_reg_status  ON registration(status);


CREATE TABLE notification (
                              notification_id   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              recipient_user_id INT NOT NULL,
                              message           VARCHAR(300) NOT NULL,
                              created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                              is_read           BOOLEAN NOT NULL DEFAULT FALSE,
                              CONSTRAINT fk_notif_user
                                  FOREIGN KEY (recipient_user_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_unread_by_user
    ON notification(recipient_user_id)
    WHERE is_read = FALSE;


CREATE TABLE feedback (
                          feedback_id  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          event_id     INT NOT NULL,
                          student_id   INT NOT NULL,
                          rating       SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                          comment      TEXT NOT NULL DEFAULT '',
                          created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
                          CONSTRAINT fk_feedback_event
                              FOREIGN KEY (event_id) REFERENCES event(event_id) ON DELETE CASCADE,
                          CONSTRAINT fk_feedback_user
                              FOREIGN KEY (student_id) REFERENCES app_user(user_id) ON DELETE CASCADE,
                          CONSTRAINT uniq_feedback_once UNIQUE (event_id, student_id)
);

CREATE TABLE audit_log (
                           audit_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                           action        audit_action NOT NULL,
                           entity        audit_entity NOT NULL,
                           entity_id     INT,
                           actor_user_id INT,
                           details_json  JSONB NOT NULL DEFAULT '{}'::jsonb,
                           CONSTRAINT fk_audit_actor
                               FOREIGN KEY (actor_user_id) REFERENCES app_user(user_id)
);

CREATE INDEX idx_audit_created ON audit_log(created_at DESC);
CREATE INDEX idx_audit_entity  ON audit_log(entity, entity_id);
CREATE INDEX idx_audit_action  ON audit_log(action);
