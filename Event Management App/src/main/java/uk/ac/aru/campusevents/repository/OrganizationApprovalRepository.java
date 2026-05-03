/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: OrganizationApprovalRepository.java
 * Purpose:
 *   Persistence port for organization-level email approvals.
 *   A record in organization_approval means a given email is allowed
 *   to act as OWNER or MANAGER for an organization.
 *
 * Design Notes:
 *   • Domain-centric interface (uses OrganizationApproval domain object).
 *   • Primary key is (organizationId, email).
 *   • “Upsert” semantics: inserting same pair replaces role.
 */
package uk.ac.aru.campusevents.repository;

import uk.ac.aru.campusevents.domain.OrganizationApproval;

import java.util.List;
import java.util.Optional;

public interface OrganizationApprovalRepository {

    /**
     * Inserts a new approval or updates an existing one.
     * Enforces uniqueness on (organizationId, email).
     */
    void upsert(OrganizationApproval approval);

    /**
     * Retrieves all approvals matching this email (case-insensitive).
     */
    List<OrganizationApproval> findByEmail(String email);

    /**
     * Retrieves all approvals belonging to the given organization.
     */
    List<OrganizationApproval> findByOrganization(int organizationId);

    /**
     * Returns true if the email has ANY approval role for this organization.
     */
    boolean exists(int organizationId, String email);

    /**
     * Optional direct lookup (organizationId + email).
     */
    Optional<OrganizationApproval> findOne(int organizationId, String email);
}
