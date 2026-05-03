/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: UserOrganization.java
 * Purpose:
 *   Membership link between a user and an organization.
 * Security:
     • Immutable; repository adds or removes entire memberships.
     • RoleInOrg determines permission (OWNER, MANAGER).
     • Simplifies RBAC for org-owned events.
 */
package uk.ac.aru.campusevents.domain;

import java.util.Objects;

public final class UserOrganization {
    private final int userId;
    private final int organizationId;
    private final String roleInOrg; // "OWNER","MANAGER"

    public UserOrganization(int userId, int organizationId, String roleInOrg) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.roleInOrg = Objects.requireNonNull(roleInOrg);
    }
    public int getUserId() { return userId; }
    public int getOrganizationId() { return organizationId; }
    public String getRoleInOrg() { return roleInOrg; }
}

