/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: JdbcEventRepository.java
 * Purpose:
 *   JDBC-backed implementation of {@link EventRepository} that persists and
 *   retrieves {@link Event} entities from the PostgreSQL database.
 *
 * Design & Security Notes:
 *   • Uses the DB helper to run parameterised SQL (PreparedStatement) via JDBC.
 *   • Reads from and writes to the event table, mapping to domain {@link Event}.
 *   • Ownership is enforced at the database level using the CHECK constraint:
 *        exactly one of (organizer_user_id, organization_id) must be non-null.
 *   • Assumes that higher-level services enforce RBAC and validate that the
 *     current user is allowed to modify/delete a given event.
 *
 * Uses the event table:
 *   event_id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
 *   organizer_user_id   INT REFERENCES app_user(user_id)
 *   organization_id     INT REFERENCES organization(organization_id)
 *   title               VARCHAR(200) NOT NULL
 *   description         TEXT NOT NULL DEFAULT ''
 *   category            event_category NOT NULL
 *   category_other_desc TEXT NOT NULL DEFAULT ''
 *   status              event_status NOT NULL DEFAULT 'DRAFT'
 *   start_at            TIMESTAMPTZ
 *   end_at              TIMESTAMPTZ
 *   location            VARCHAR(200)
 *   capacity            INT NOT NULL DEFAULT 1
 *   created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
 *   updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
 *   CHECK (
 *     (organizer_user_id IS NOT NULL AND organization_id IS NULL) OR
 *     (organizer_user_id IS NULL AND organization_id IS NOT NULL)
 *   )
 */
package uk.ac.aru.campusevents.repository.jdbc;

import uk.ac.aru.campusevents.database.DB;
import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.EventCategory;
import uk.ac.aru.campusevents.domain.enums.EventStatus;
import uk.ac.aru.campusevents.dto.EventSearchCriteria;
import uk.ac.aru.campusevents.repository.EventRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class JdbcEventRepository implements EventRepository {

    private final DB db;

    public JdbcEventRepository(DB db) {
        this.db = Objects.requireNonNull(db);
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public int create(Event e) {
        Object[] params = {
                e.getOrganizerUserId(),                // 1
                e.getOrganizationId(),                 // 2
                e.getTitle(),                          // 3
                e.getDescription(),                    // 4
                mapCategoryToDb(e.getCategory()),      // 5
                e.getCategoryOtherDescription(),       // 6 category_other_desc (never null in domain)
                mapStatusToDb(e.getStatus()),          // 7
                e.getStartDateTime(),                  // 8
                e.getEndDateTime(),                    // 9
                e.getLocation(),                       // 10
                e.getCapacity()                        // 11
        };

        try (ResultSet rs = db.prepSQLQuery(
                "INSERT INTO event (" +
                        "organizer_user_id, organization_id, title, description, " +
                        "category, category_other_desc, status, start_at, end_at, " +
                        "location, capacity" +
                        ") VALUES (" +
                        "?, ?, ?, ?, " +
                        "?::event_category, ?, " +
                        "?::event_status, " +
                        "?, ?, ?, ?" +
                        ") RETURNING event_id",
                params,
                true)) {

            if (rs != null && rs.next()) {
                return rs.getInt("event_id");
            } else {
                throw new IllegalStateException("Event insert succeeded but no ID returned");
            }

        } catch (SQLException ex) {
            System.err.println("Error inserting event: " + ex.getMessage());
            throw new RuntimeException("Failed to create event", ex);
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    public void update(Event e) {
        if (e.getId() <= 0) {
            throw new IllegalArgumentException("Event must have a valid ID to be updated");
        }

        Object[] params = {
                e.getOrganizerUserId(),                // 1
                e.getOrganizationId(),                 // 2
                e.getTitle(),                          // 3
                e.getDescription(),                    // 4
                mapCategoryToDb(e.getCategory()),      // 5
                e.getCategoryOtherDescription(),       // 6
                mapStatusToDb(e.getStatus()),          // 7
                e.getStartDateTime(),                  // 8
                e.getEndDateTime(),                    // 9
                e.getLocation(),                       // 10
                e.getCapacity(),                       // 11
                e.getId()                              // 12 WHERE
        };

        boolean ok = db.prepSQLUpdate(
                "UPDATE event SET " +
                        "organizer_user_id   = ?, " +
                        "organization_id     = ?, " +
                        "title               = ?, " +
                        "description         = ?, " +
                        "category            = ?::event_category, " +
                        "category_other_desc = ?, " +
                        "status              = ?::event_status, " +
                        "start_at            = ?, " +
                        "end_at              = ?, " +
                        "location            = ?, " +
                        "capacity            = ?, " +
                        "updated_at          = CURRENT_TIMESTAMP " +
                        "WHERE event_id      = ?",
                params,
                true);

        if (!ok) {
            throw new RuntimeException("Failed to update event with id " + e.getId());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    public void delete(int id) {
        boolean ok = db.prepSQLUpdate(
                "DELETE FROM event WHERE event_id = ?",
                new Object[]{id},
                true);

        if (!ok) {
            System.err.println("Warning: delete event(" + id + ") may have failed.");
        }
    }

    // -------------------------------------------------------------------------
    // FIND BY ID
    // -------------------------------------------------------------------------

    @Override
    public Optional<Event> findById(int id) {
        try (ResultSet rs = db.prepSQLQuery(
                "SELECT " +
                        "event_id, organizer_user_id, organization_id, " +
                        "title, description, category, category_other_desc, status, " +
                        "start_at, end_at, location, capacity, " +
                        "created_at, updated_at " +
                        "FROM event WHERE event_id = ?",
                new Object[]{id},
                true)) {

            if (rs != null && rs.next()) {
                return Optional.of(mapRowToEvent(rs));
            }

        } catch (SQLException ex) {
            System.err.println("Error querying event by id: " + ex.getMessage());
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // SEARCH
    // -------------------------------------------------------------------------

    @Override
    public List<Event> search(EventSearchCriteria c) {
        StringBuilder sql = new StringBuilder(
                "SELECT " +
                        "event_id, organizer_user_id, organization_id, " +
                        "title, description, category, category_other_desc, status, " +
                        "start_at, end_at, location, capacity, " +
                        "created_at, updated_at " +
                        "FROM event WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (c != null) {
            // category (enum in criteria)
            if (c.category() != null) {
                sql.append(" AND category = ?::event_category");
                params.add(mapCategoryToDb(c.category()));
            }

            // free-text in title/description
            if (c.text() != null && !c.text().isBlank()) {
                sql.append(" AND (LOWER(title) LIKE ? OR LOWER(description) LIKE ?)");
                String pattern = "%" + c.text().toLowerCase() + "%";
                params.add(pattern);
                params.add(pattern);
            }

            // date range on start_at (using LocalDate from criteria)
            if (c.startFrom() != null) {
                sql.append(" AND start_at >= ?");
                params.add(c.startFrom().atStartOfDay());
            }
            if (c.startTo() != null) {
                sql.append(" AND start_at <= ?");
                params.add(c.startTo().atTime(23, 59, 59));
            }

            // location substring
            if (c.location() != null && !c.location().isBlank()) {
                sql.append(" AND LOWER(location) LIKE ?");
                params.add("%" + c.location().toLowerCase() + "%");
            }
        }

        sql.append(" ORDER BY start_at NULLS LAST, title ASC");

        List<Event> events = new ArrayList<>();
        try (ResultSet rs = db.prepSQLQuery(sql.toString(), params.toArray(), true)) {
            if (rs == null) return List.of();
            while (rs.next()) {
                events.add(mapRowToEvent(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error running event search: " + ex.getMessage());
        }
        return events;
    }

    // -------------------------------------------------------------------------
    // FIND BY ORGANIZER
    // -------------------------------------------------------------------------

    @Override
    public List<Event> findByOrganizer(int organizerId) {
        List<Event> events = new ArrayList<>();
        try (ResultSet rs = db.prepSQLQuery(
                "SELECT " +
                        "event_id, organizer_user_id, organization_id, " +
                        "title, description, category, category_other_desc, status, " +
                        "start_at, end_at, location, capacity, " +
                        "created_at, updated_at " +
                        "FROM event WHERE organizer_user_id = ?",
                new Object[]{organizerId},
                true)) {

            if (rs == null) return List.of();
            while (rs.next()) {
                events.add(mapRowToEvent(rs));
            }

        } catch (SQLException ex) {
            System.err.println("Error querying events by organizer: " + ex.getMessage());
        }
        return events;
    }

    // -------------------------------------------------------------------------
    // MAPPING HELPERS
    // -------------------------------------------------------------------------

    private Event mapRowToEvent(ResultSet rs) throws SQLException {
        int id                  = rs.getInt("event_id");
        Integer organizerUserId = (Integer) rs.getObject("organizer_user_id");
        Integer organizationId  = (Integer) rs.getObject("organization_id");
        String title            = rs.getString("title");
        String description      = rs.getString("description");

        // Still read as String — correct behaviour for JDBC
        String categoryDb       = rs.getString("category");
        String categoryOther    = rs.getString("category_other_desc");

        String statusDb         = rs.getString("status");
        int capacity            = rs.getInt("capacity");

        LocalDateTime startAt   = rs.getTimestamp("start_at") != null
                ? rs.getTimestamp("start_at").toLocalDateTime()
                : null;

        LocalDateTime endAt     = rs.getTimestamp("end_at") != null
                ? rs.getTimestamp("end_at").toLocalDateTime()
                : null;

        String location         = rs.getString("location");

        // Convert DB values → enums
        EventStatus status       = mapStatusFromDb(statusDb);
        EventCategory category   = mapCategoryFromDb(categoryDb);

        String categoryOtherDesc = (categoryOther == null) ? "" : categoryOther;

        Event base;
        if (organizerUserId != null) {
            base = Event.newPersonalEvent(
                    organizerUserId,
                    title,
                    description,
                    category,
                    categoryOtherDesc,
                    capacity
            );
        } else if (organizationId != null) {
            base = Event.newOrgEvent(
                    organizationId,
                    title,
                    description,
                    category,
                    categoryOtherDesc,
                    capacity
            );
        } else {
            throw new IllegalStateException("Event row violates ownership XOR constraint");
        }

        base.setStatus(status);
        base.setStartDateTime(startAt);
        base.setEndDateTime(endAt);
        base.setLocation(location);

        return base.withId(id);
    }

    private String mapCategoryToDb(EventCategory category) {
        if (category == null) {
            return EventCategory.OTHER.name();
        }
        return category.name();
    }

    private String mapStatusToDb(EventStatus status) {
        if (status == null) return EventStatus.DRAFT.name();
        return status.name();
    }

    private EventCategory mapCategoryFromDb(String dbVal) {
        if (dbVal == null) return EventCategory.OTHER;
        try {
            return EventCategory.valueOf(dbVal);
        } catch (IllegalArgumentException ex) {
            // Safety fallback if DB somehow contains an unknown value
            return EventCategory.OTHER;
        }
    }

    private EventStatus mapStatusFromDb(String dbVal) {
        if (dbVal == null) return EventStatus.DRAFT;
        return EventStatus.valueOf(dbVal);
    }
}
