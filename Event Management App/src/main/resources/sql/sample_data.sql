/* Team Lentil — Campus Event Management System (MOD004881, Element 010) */

/* ---------- 0) CLEAR EXISTING DATA ---------- */

TRUNCATE audit_log, feedback, notification, registration, event,
    user_organization, organization_approval, user_role,
    organization, app_user
    RESTART IDENTITY CASCADE;

/* ---------- 1) USERS ---------- */

INSERT INTO app_user (first_name, last_name, email, password_hash)
VALUES
    ('Alice',   'Student',   'alice@student.uni.ac.uk',   'pw_hash_alice'),
    ('Bob',     'Organizer', 'bob@events.uni.ac.uk',      'pw_hash_bob'),
    ('Carol',   'Admin',     'carol.admin@uni.ac.uk',    'pw_hash_carol'),
    ('David',   'Smith',     'david.smith@uni.ac.uk',     'pw_hash_david'),
    ('Eva',     'Jones',     'eva.jones@uni.ac.uk',       'pw_hash_eva'),
    ('Frank',   'Miller',    'frank.miller@uni.ac.uk',    'pw_hash_frank'),
    ('Grace',   'Nguyen',    'grace.nguyen@uni.ac.uk',    'pw_hash_grace');

/* ---------- 2) GLOBAL USER ROLES ---------- */

INSERT INTO user_role (user_id, role)
VALUES
    (1, 'STUDENT'),
    (2, 'ORGANIZER'),
    (3, 'ADMIN'),
    (3, 'ORGANIZER'),
    (4, 'STUDENT'),
    (5, 'STUDENT'),
    (6, 'ORGANIZER'),
    (7, 'STUDENT');

/* ---------- 3) ORGANIZATIONS ---------- */

INSERT INTO organization (name, type, notes)
VALUES
    ('Computer Science Society', 'STUDENT_SOCIETY', 'CS-focused events and talks'),
    ('Green Campus Initiative',  'STUDENT_SOCIETY', 'Environment & sustainability projects'),
    ('Sports Union',             'UNION',           'Umbrella for all sports clubs'),
    ('Board Games Club',         'STUDENT_SOCIETY', 'Casual social gaming evenings');

/* ---------- 4) ORGANIZATION APPROVAL REQUESTS ---------- */

INSERT INTO organization_approval (organization_id, email, org_role)
VALUES
    (1, 'bob@events.uni.ac.uk',    'OWNER'),
    (1, 'carol.admin@uni.ac.uk',   'MANAGER'),
    (2, 'grace.nguyen@uni.ac.uk',  'MANAGER'),
    (3, 'frank.miller@uni.ac.uk',  'OWNER'),
    (4, 'alice@student.uni.ac.uk', 'MANAGER');

/* ---------- 5) USER–ORGANIZATION MEMBERSHIPS ---------- */

INSERT INTO user_organization (user_id, organization_id, org_role)
VALUES
    (2, 1, 'OWNER'),
    (3, 1, 'MANAGER'),
    (7, 2, 'MANAGER'),
    (6, 3, 'OWNER'),
    (1, 4, 'MANAGER');

/* ---------- 6) EVENTS ---------- */

-- 6.1 Personal event by Bob (OPEN)
INSERT INTO event
(organizer_user_id, organization_id, title, description, category, category_other_desc,
 status, start_at, end_at, location, capacity)
VALUES
    (2, NULL,
     'Intro to Java Workshop',
     'Hands-on Java basics for first-years.',
     'WORKSHOP', '',
     'OPEN',
     now() + INTERVAL '7 days',
     now() + INTERVAL '7 days 2 hours',
     'Lab 3.12',
     30);

-- 6.2 CS Society event (OPEN)
INSERT INTO event (
    organizer_user_id,
    organization_id,
    title,
    description,
    category,
    category_other_desc,
    status,
    start_at,
    end_at,
    location,
    capacity
) VALUES (
             NULL,
             1,
             'Algorithms Guest Lecture',
             'Visiting speaker discussing real-world algorithms.',
             'LECTURE',
             '',
             'OPEN',
             now() + INTERVAL '5 days',
             now() + INTERVAL '5 days 2 hours',
             'Main Lecture Theatre',
             100
         );

-- 6.3 Green Campus event (FULL)
INSERT INTO event (
    organizer_user_id,
    organization_id,
    title,
    description,
    category,
    category_other_desc,
    status,
    start_at,
    end_at,
    location,
    capacity
) VALUES (
             NULL,
             2,
             'Campus Cleanup Day',
             'Join us to help clean the campus and park areas.',
             'ENVIRONMENT',
             '',
             'FULL',
             now() + INTERVAL '10 days',
             now() + INTERVAL '10 days 4 hours',
             'Main Quad',
             50
         );


-- 6.4 Sports Union event (OPEN)
INSERT INTO event (
    organizer_user_id,
    organization_id,
    title,
    description,
    category,
    category_other_desc,
    status,
    start_at,
    end_at,
    location,
    capacity
) VALUES (
             NULL,
             3,
             'Intermural Football Tournament',
             '5-a-side tournament between halls.',
             'SPORTS',
             '',
             'OPEN',
             now() + INTERVAL '14 days',
             now() + INTERVAL '14 days 6 hours',
             'Sports Field A',
             80
         );


-- 6.5 Board Games Night (DRAFT)
INSERT INTO event (
    organizer_user_id,
    organization_id,
    title,
    description,
    category,
    category_other_desc,
    status,
    start_at,
    end_at,
    location,
    capacity
) VALUES (
             NULL,
             4,
             'Board Games Night',
             'Casual evening of games and snacks.',
             'SOCIAL',
             '',
             'DRAFT',
             now() + INTERVAL '3 days',
             now() + INTERVAL '3 days 3 hours',
             'Student Union Room B',
             25
         );


-- 6.6 Past completed event
INSERT INTO event (
    organizer_user_id,
    organization_id,
    title,
    description,
    category,
    category_other_desc,
    status,
    start_at,
    end_at,
    location,
    capacity
) VALUES (
             2,
             NULL,
             'Exam Revision Session',
             'Tips and Q&A before exams.',
             'ACADEMIC',
             '',
             'COMPLETED',
             now() - INTERVAL '5 days',
             now() - INTERVAL '5 days' + INTERVAL '2 hours',
             'Library Study Room 2',
             40
         );


/* ---------- 7) REGISTRATIONS ---------- */

INSERT INTO registration (event_id, student_id, status)
VALUES
    (1, 1, 'REGISTERED'),
    (2, 1, 'REGISTERED'),
    (3, 1, 'WAITLISTED'),
    (6, 1, 'APPROVED'),          -- has attended past event

    (1, 4, 'REGISTERED'),
    (2, 4, 'REGISTERED'),
    (3, 4, 'CANCELLED'),
    (4, 4, 'REGISTERED'),
    (6, 4, 'APPROVED'),

    (1, 5, 'CANCELLED'),
    (2, 5, 'REGISTERED'),
    (3, 5, 'WAITLISTED'),
    (4, 5, 'REGISTERED'),

    (2, 7, 'REGISTERED'),
    (3, 7, 'REGISTERED'),        -- Cleanup Day FULL → 7 has a seat
    (4, 7, 'REGISTERED');

/* ---------- 8) NOTIFICATIONS ---------- */

INSERT INTO notification (recipient_user_id, message, is_read)
VALUES
    (1, 'You are registered for "Intro to Java Workshop".', FALSE),
    (1, 'You are on the waitlist for "Campus Cleanup Day".', FALSE),
    (4, 'Your registration for "Campus Cleanup Day" was cancelled.', TRUE),
    (5, 'You are registered for "Algorithms Guest Lecture".', FALSE),
    (7, 'You are registered for "Campus Cleanup Day".', TRUE),
    (2, 'Your event "Intro to Java Workshop" has new registrations.', FALSE),
    (3, 'You have been added as manager for "Computer Science Society".', TRUE);

/* ---------- 9) FEEDBACK ---------- */

INSERT INTO feedback (event_id, student_id, rating, comment)
VALUES
    (6, 1, 5, 'Very helpful session.'),
    (6, 4, 4, 'Could have included more examples.'),
    (6, 5, 5, 'Excellent revision help.');

/* ---------- 10) AUDIT LOG ---------- */

INSERT INTO audit_log (action, entity, entity_id, actor_user_id, details_json)
VALUES
    -- Event creation logs
    ('CREATE', 'EVENT', 1, 2, '{"title":"Intro to Java Workshop","status":"OPEN"}'),
    ('CREATE', 'EVENT', 2, 3, '{"title":"Algorithms Guest Lecture","status":"OPEN"}'),
    ('CREATE', 'EVENT', 3, 7, '{"title":"Campus Cleanup Day","status":"FULL"}'),

    -- Registrations
    ('REGISTER', 'REGISTRATION', 1, 1, '{"event_id":1,"student_id":1,"status":"REGISTERED"}'),
    ('REGISTER', 'REGISTRATION', 2, 1, '{"event_id":2,"student_id":1,"status":"REGISTERED"}'),

    -- Promotion (system-triggered)
    ('PROMOTE', 'REGISTRATION', 3, NULL,
     '{"event_id":3,"student_id":7,"from":"WAITLISTED","to":"APPROVED","auto":true}'),

    -- Cancellation
    ('CANCEL', 'REGISTRATION', 7, 5,
     '{"event_id":1,"student_id":5,"prev_status":"REGISTERED","new_status":"CANCELLED"}'),

    -- Feedback submissions
    ('FEEDBACK_SUBMIT', 'REGISTRATION', 10, 1,
     '{"event_id":6,"rating":5}'),

    -- Login logs
    ('LOGIN_OK',   'USER', 1, 1, '{"email":"alice@student.uni.ac.uk"}'),
    ('LOGIN_OK',   'USER', 2, 2, '{"email":"bob@events.uni.ac.uk"}'),
    ('LOGIN_FAIL', 'USER', NULL, NULL, '{"email":"unknown@uni.ac.uk"}');
