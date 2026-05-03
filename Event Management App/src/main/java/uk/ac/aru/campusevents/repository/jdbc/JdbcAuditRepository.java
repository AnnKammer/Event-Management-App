/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: JdbcAuditRepository.java
 *
 * Purpose:
 *   JDBC-backed implementation of the AuditRepository using PostgreSQL
 *   ENUM types (audit_action, audit_entity) and JSONB for audit details.
 */

package uk.ac.aru.campusevents.repository.jdbc;

import uk.ac.aru.campusevents.database.DB;
import uk.ac.aru.campusevents.domain.AuditLog;
import uk.ac.aru.campusevents.domain.enums.AuditAction;
import uk.ac.aru.campusevents.domain.enums.AuditEntity;
import uk.ac.aru.campusevents.dto.AuditFilter;
import uk.ac.aru.campusevents.repository.AuditRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public final class JdbcAuditRepository implements AuditRepository {

    private final DB db;

    public JdbcAuditRepository(DB db) {
        this.db = Objects.requireNonNull(db);
    }

    // -------------------------------------------------------------------------
    // SAVE (INSERT)
    // -------------------------------------------------------------------------

    @Override
    public int save(AuditLog log) {
        Objects.requireNonNull(log, "audit log cannot be null");

        Object[] params = {
                Timestamp.from(log.getTs()),   // 1: created_at
                log.getAction().name(),        // 2: action -> cast to ::audit_action
                log.getEntity().name(),        // 3: entity -> cast to ::audit_entity
                log.getEntityId(),             // 4: entity_id
                log.getActorUserId(),          // 5: actor_user_id
                log.getDetailsJson()           // 6: details_json -> cast to ::jsonb
        };

        String sql =
                "INSERT INTO audit_log (" +
                        "created_at, action, entity, entity_id, actor_user_id, details_json" +
                        ") VALUES (" +
                        "?," +                     // created_at
                        "?::audit_action," +       // action ENUM
                        "?::audit_entity," +       // entity ENUM
                        "?," +                     // entity_id
                        "?," +                     // actor_user_id
                        "?::jsonb" +               // JSONB
                        ") RETURNING audit_id";

        try (ResultSet rs = db.prepSQLQuery(sql, params, true)) {
            if (rs != null && rs.next()) {
                return rs.getInt("audit_id");
            }
            throw new IllegalStateException(
                    "Audit insert succeeded but RETURNING did not return an ID");
        } catch (SQLException ex) {
            System.err.println("Error inserting audit log: " + ex.getMessage());
            throw new RuntimeException("Failed to save audit log", ex);
        }
    }

    // -------------------------------------------------------------------------
    // FIND WITH FILTER
    // -------------------------------------------------------------------------

    @Override
    public List<AuditLog> find(AuditFilter f) {

        StringBuilder sql = new StringBuilder(
                "SELECT audit_id, created_at, action, entity, entity_id, actor_user_id, details_json " +
                        "FROM audit_log WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        // ----- Filter: entity -----
        if (f.entity() != null) {
            sql.append(" AND entity = ?::audit_entity");
            params.add(f.entity().name());
        }

        // ----- Filter: entityId -----
        if (f.entityId() != null) {
            sql.append(" AND entity_id = ?");
            params.add(f.entityId());
        }

        // ----- Filter: actorUserId -----
        if (f.actorUserId() != null) {
            sql.append(" AND actor_user_id = ?");
            params.add(f.actorUserId());
        }

        // ----- Filter: action -----
        if (f.action() != null) {
            sql.append(" AND action = ?::audit_action");
            params.add(f.action().name());
        }

        // ----- Filter: time range -----
        if (f.from() != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.from(f.from()));
        }
        if (f.to() != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.from(f.to()));
        }

        // Order newest first
        sql.append(" ORDER BY created_at DESC");

        // Limit results if specified
        if (f.limit() != null && f.limit() > 0) {
            sql.append(" LIMIT ?");
            params.add(f.limit());
        }

        List<AuditLog> result = new ArrayList<>();

        try (ResultSet rs = db.prepSQLQuery(sql.toString(), params.toArray(), true)) {
            if (rs == null) return List.of();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error querying audit logs: " + ex.getMessage());
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // ROW MAPPING HELPERS
    // -------------------------------------------------------------------------

    private AuditLog mapRow(ResultSet rs) throws SQLException {

        int id = rs.getInt("audit_id");

        Instant ts = rs.getTimestamp("created_at").toInstant();

        Integer actorUserId = (Integer) rs.getObject("actor_user_id"); // nullable
        Integer entityId    = (Integer) rs.getObject("entity_id");     // nullable

        String actionStr = rs.getString("action");
        String entityStr = rs.getString("entity");
        String details   = rs.getString("details_json");

        // Convert DB strings to Java enums
        AuditAction action = AuditAction.valueOf(actionStr);
        AuditEntity entity = AuditEntity.valueOf(entityStr);

        return new AuditLog(
                id,
                ts,
                actorUserId,
                action,
                entity,
                entityId,
                details
        );
    }
}
