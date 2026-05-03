/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: UserOrganizationRepository.java
 * Purpose:
 *   Defines the persistence port (DAO) for membership links between users and organizations,
 *   including management privileges.
 * Security & Design Notes:
 *   • Encapsulates membership and management permissions behind simple boolean probes.
 *   • Repository does not mutate User/Organization entities directly; it only manages links.
 *   • Implementations should enforce uniqueness (one link per (userId, orgId)) and atomic writes.
 *   • Safe to implement as in-memory or database-backed storage (e.g., UNIQUE(user_id, org_id)).
 */
package uk.ac.aru.campusevents.repository;

import uk.ac.aru.campusevents.domain.UserOrganization;

import java.util.List;

/**
 * Repository interface for managing user–organization membership links.
 * The service layer relies on this contract to enforce RBAC (e.g., allowing
 * organizers/managers to perform privileged actions on organization-owned events).
 */
@SuppressWarnings("unused")
public interface UserOrganizationRepository {

    /**
     * Creates a new membership link (or updates role flags if your domain supports it).
     * Implementations should enforce uniqueness on (userId, orgId).
     *
     * @param membership the membership link to persist
     */
    void add(UserOrganization membership);

    /**
     * Checks whether a user is a member of a given organization.
     *
     * @param userId user identifier
     * @param orgId  organization identifier
     * @return {@code true} if the user is a member; otherwise {@code false}
     */
    boolean isMember(int userId, int orgId);

    /**
     * Checks whether a user has management privileges for a given organization.
     * Implementations may derive this from role flags stored in {@link UserOrganization}
     * (e.g., member vs. manager).
     *
     * @param userId user identifier
     * @param orgId  organization identifier
     * @return {@code true} if the user can manage the organization; otherwise {@code false}
     */
    boolean canManage(int userId, int orgId);

    /**
     * Retrieves all membership links for a specific user.
     *
     * @param userId user identifier
     * @return a list of membership links (possibly empty)
     */
    List<UserOrganization> membershipsForUser(int userId);
}


