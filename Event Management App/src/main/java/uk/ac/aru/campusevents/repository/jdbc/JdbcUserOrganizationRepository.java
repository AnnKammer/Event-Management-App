/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: JdbcUserOrganizationRepository.java
 * Purpose:
 *   JDBC-backed implementation of {@link UserOrganizationRepository} that manages
 *   membership links between users and organizations in PostgreSQL.
 *
 * Design & Security Notes:
 *   • Uses the DB helper to execute parameterised SQL via JDBC.
 *   • Enforces one membership per (user_id, organization_id) using the PK.
 *   • org_role is stored as upper-case text ('OWNER','MANAGER') and interpreted
 *     for RBAC (canManage) checks.
 *   • No PII beyond numeric IDs is stored in this table.
 *
 * Uses the user_organization table:
 *   user_id         INT NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE
 *   organization_id INT NOT NULL REFERENCES organization(organization_id) ON DELETE CASCADE
 *   org_role        TEXT NOT NULL CHECK (org_role IN ('OWNER','MANAGER'))
 *   PRIMARY KEY (user_id, organization_id)
 */
package uk.ac.aru.campusevents.repository.jdbc;

import uk.ac.aru.campusevents.database.DB;
import uk.ac.aru.campusevents.domain.UserOrganization;
import uk.ac.aru.campusevents.repository.UserOrganizationRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class JdbcUserOrganizationRepository implements UserOrganizationRepository {

    private final DB db;

    public JdbcUserOrganizationRepository(DB db) {
        this.db = Objects.requireNonNull(db);
    }

    // -------------------------------------------------------------------------
    // ADD / UPSERT MEMBERSHIP
    // -------------------------------------------------------------------------

    @Override
    public void add(UserOrganization membership) {
        Objects.requireNonNull(membership, "membership cannot be null");

        int userId = membership.getUserId();
        int orgId  = membership.getOrganizationId();
        String role = normalizeRole(membership.getRoleInOrg()); // "OWNER" / "MANAGER"

        Object[] params = { userId, orgId, role };

        // If a link already exists, just update the role.
        boolean ok = db.prepSQLUpdate(
                "INSERT INTO user_organization (user_id, organization_id, org_role) " +
                        "VALUES (?,?,?) " +
                        "ON CONFLICT (user_id, organization_id) " +
                        "DO UPDATE SET org_role = EXCLUDED.org_role",
                params,
                true
        );

        if (!ok) {
            throw new RuntimeException("Failed to save user_organization membership for user=" +
                    userId + ", org=" + orgId);
        }
    }

    // -------------------------------------------------------------------------
    // PROBES
    // -------------------------------------------------------------------------

    @Override
    public boolean isMember(int userId, int orgId) {
        Object[] params = { userId, orgId };

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT 1 FROM user_organization " +
                        "WHERE user_id = ? AND organization_id = ? " +
                        "LIMIT 1",
                params,
                true)) {

            return rs != null && rs.next();
        } catch (SQLException ex) {
            System.err.println("Error checking membership: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean canManage(int userId, int orgId) {
        Object[] params = { userId, orgId };

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT org_role FROM user_organization " +
                        "WHERE user_id = ? AND organization_id = ? " +
                        "LIMIT 1",
                params,
                true)) {

            if (rs == null || !rs.next()) return false;
            String role = rs.getString("org_role");
            return isManagerRole(role);

        } catch (SQLException ex) {
            System.err.println("Error checking manage permission: " + ex.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // LIST MEMBERSHIPS FOR USER
    // -------------------------------------------------------------------------

    @Override
    public List<UserOrganization> membershipsForUser(int userId) {
        Object[] params = { userId };
        List<UserOrganization> result = new ArrayList<>();

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT user_id, organization_id, org_role " +
                        "FROM user_organization " +
                        "WHERE user_id = ? " +
                        "ORDER BY organization_id ASC",
                params,
                true)) {

            if (rs == null) return List.of();
            while (rs.next()) {
                int uid      = rs.getInt("user_id");
                int orgId    = rs.getInt("organization_id");
                String role  = rs.getString("org_role");
                result.add(new UserOrganization(uid, orgId, role));
            }

        } catch (SQLException ex) {
            System.err.println("Error listing memberships for user " + userId + ": " + ex.getMessage());
        }

        return List.copyOf(result);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private static String normalizeRole(String roleInOrg) {
        if (roleInOrg == null) {
            throw new IllegalArgumentException("org_role cannot be null");
        }
        return roleInOrg.trim().toUpperCase(); // e.g. "owner" → "OWNER"
    }

    private static boolean isManagerRole(String role) {
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return "MANAGER".equals(r) || "OWNER".equals(r);
    }
}
