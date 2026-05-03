/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: JdbcOrganizationApprovalRepository.java
 * Purpose:
 *   JDBC-backed implementation of OrganizationApprovalRepository using the
 *   organization_approval table.
 *
 * Uses the organization_approval table:
 *   organization_id  INT NOT NULL REFERENCES organization(organization_id)
 *   email            VARCHAR(254) NOT NULL
 *   org_role         TEXT NOT NULL CHECK (org_role IN ('OWNER','MANAGER'))
 *   created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
 *   PRIMARY KEY (organization_id, email)
 */
package uk.ac.aru.campusevents.repository.jdbc;

import uk.ac.aru.campusevents.database.DB;
import uk.ac.aru.campusevents.domain.OrganizationApproval;
import uk.ac.aru.campusevents.repository.OrganizationApprovalRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcOrganizationApprovalRepository implements OrganizationApprovalRepository {

    private final DB db;

    public JdbcOrganizationApprovalRepository(DB db) {
        this.db = Objects.requireNonNull(db);
    }

    /* ----------------------------------------------------------------------
       UPSERT
       ---------------------------------------------------------------------- */

    @Override
    public void upsert(OrganizationApproval approval) {
        Objects.requireNonNull(approval, "approval cannot be null");

        int orgId = approval.getOrganizationId();
        String email = canonicalEmail(approval.getEmail());

        if (orgId <= 0) {
            throw new IllegalArgumentException("organizationId must be > 0");
        }
        if (email.isEmpty()) {
            throw new IllegalArgumentException("email must not be blank");
        }

        Object[] params = {
                orgId,
                email,
                approval.getOrgRole()
        };

        db.prepSQLUpdate(
                "INSERT INTO organization_approval (organization_id, email, org_role) " +
                        "VALUES (?,?,?) " +
                        "ON CONFLICT (organization_id, email) DO UPDATE " +
                        "SET org_role = EXCLUDED.org_role",
                params,
                true
        );
    }

    /* ----------------------------------------------------------------------
       QUERIES
       ---------------------------------------------------------------------- */

    @Override
    public List<OrganizationApproval> findByEmail(String email) {
        String norm = canonicalEmail(email);
        if (norm.isEmpty()) return List.of();

        Object[] params = { norm };

        List<OrganizationApproval> result = new ArrayList<>();
        try (ResultSet rs = db.prepSQLQuery(
                "SELECT organization_id, email, org_role, created_at " +
                        "FROM organization_approval " +
                        "WHERE LOWER(email) = ? " +
                        "ORDER BY organization_id",
                params,
                true)) {

            if (rs == null) return List.of();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error querying organization_approval by email: " + ex.getMessage());
        }
        return result;
    }

    @Override
    public List<OrganizationApproval> findByOrganization(int organizationId) {
        if (organizationId <= 0) return List.of();

        Object[] params = { organizationId };

        List<OrganizationApproval> result = new ArrayList<>();
        try (ResultSet rs = db.prepSQLQuery(
                "SELECT organization_id, email, org_role, created_at " +
                        "FROM organization_approval " +
                        "WHERE organization_id = ? " +
                        "ORDER BY email",
                params,
                true)) {

            if (rs == null) return List.of();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error querying organization_approval by org: " + ex.getMessage());
        }
        return result;
    }

    @Override
    public boolean exists(int organizationId, String email) {
        if (organizationId <= 0) return false;
        String norm = canonicalEmail(email);
        if (norm.isEmpty()) return false;

        Object[] params = { organizationId, norm };

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT 1 FROM organization_approval " +
                        "WHERE organization_id = ? AND LOWER(email) = ? " +
                        "LIMIT 1",
                params,
                true)) {

            return rs != null && rs.next();
        } catch (SQLException ex) {
            System.err.println("Error checking organization_approval existence: " + ex.getMessage());
            return false;
        }
    }

    /** Optional helper if your interface includes it. */
    @Override
    public Optional<OrganizationApproval> findOne(int organizationId, String email) {
        if (organizationId <= 0) return Optional.empty();
        String norm = canonicalEmail(email);
        if (norm.isEmpty()) return Optional.empty();

        Object[] params = { organizationId, norm };

        try (ResultSet rs = db.prepSQLQuery(
                "SELECT organization_id, email, org_role, created_at " +
                        "FROM organization_approval " +
                        "WHERE organization_id = ? AND LOWER(email) = ? " +
                        "LIMIT 1",
                params,
                true)) {

            if (rs != null && rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Error querying single organization_approval: " + ex.getMessage());
        }
        return Optional.empty();
    }

    /* ----------------------------------------------------------------------
       MAPPING + HELPERS
       ---------------------------------------------------------------------- */

    private OrganizationApproval mapRow(ResultSet rs) throws SQLException {
        int orgId = rs.getInt("organization_id");
        String email = rs.getString("email");
        String role = rs.getString("org_role");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        return new OrganizationApproval(orgId, email, role, createdAt);
    }

    private static String canonicalEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
