/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: OrganizationApproval.java
 * Purpose:
 *   Immutable whitelist entry mapping an email address to an organization
 *   and an organization-level role (OWNER/MANAGER).
 * Security & Design Notes:
 *   • No user_id here: approvals are granted to emails before or after
 *     user registration.
 *   • Existence of a row = approval; there is no separate approved flag.
 *   • Repository is responsible for enforcing the primary key
 *     (organization_id, email) uniqueness.
 */
package uk.ac.aru.campusevents.domain;

import java.time.LocalDateTime;
import java.util.Objects;

@SuppressWarnings("unused")
public final class OrganizationApproval {

    private final int organizationId;
    private final String email;      // canonicalized (lowercase, trimmed)
    private final String orgRole;    // "OWNER" or "MANAGER"
    private final LocalDateTime createdAt;

    public OrganizationApproval(int organizationId,
                                String email,
                                String orgRole,
                                LocalDateTime createdAt) {
        if (organizationId <= 0) {
            throw new IllegalArgumentException("organizationId must be > 0");
        }
        this.organizationId = organizationId;
        this.email = Objects.requireNonNull(email, "email").trim().toLowerCase();
        this.orgRole = Objects.requireNonNull(orgRole, "orgRole").trim().toUpperCase();
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    /** Convenience factory for new approvals (createdAt = now). */
    public static OrganizationApproval newApproval(int organizationId,
                                                   String email,
                                                   String orgRole) {
        return new OrganizationApproval(organizationId, email, orgRole, LocalDateTime.now());
    }

    public int getOrganizationId() { return organizationId; }
    public String getEmail() { return email; }
    public String getOrgRole() { return orgRole; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
