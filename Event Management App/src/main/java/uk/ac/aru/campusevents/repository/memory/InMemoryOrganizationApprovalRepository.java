/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: InMemoryOrganizationApprovalRepository.java
 * Purpose:
 *   In-memory implementation of OrganizationApprovalRepository for
 *   development and testing. Stores pending/approved organization-level
 *   email grants (OWNER/MANAGER) without requiring a database.
 *
 * Design & Security Notes:
 *   • Thread-safe: ConcurrentHashMap for all storage.
 *   • Keyed by (organizationId, canonicalEmail) where canonicalEmail
 *     is lowercased + trimmed to avoid duplicates.
 *   • Immutable domain model: repository never mutates OrganizationApproval,
 *     it only replaces entries on upsert().
 *   • No PII beyond email address; contents are lost on JVM shutdown.
 */
package uk.ac.aru.campusevents.repository.memory;

import uk.ac.aru.campusevents.domain.OrganizationApproval;
import uk.ac.aru.campusevents.repository.OrganizationApprovalRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unused")
public final class InMemoryOrganizationApprovalRepository implements OrganizationApprovalRepository {

    /**
     * Internal storage keyed by "orgId|canonicalEmail".
     */
    private final ConcurrentMap<String, OrganizationApproval> byOrgEmail = new ConcurrentHashMap<>();

    /* ----------------------------------------------------------------------
       Core operations
       ---------------------------------------------------------------------- */

    @Override
    public void upsert(OrganizationApproval approval) {
        Objects.requireNonNull(approval, "approval cannot be null");
        int orgId = approval.getOrganizationId();
        String canonicalEmail = canonicalEmail(approval.getEmail());

        if (orgId <= 0) {
            throw new IllegalArgumentException("organizationId must be > 0");
        }
        if (canonicalEmail.isEmpty()) {
            throw new IllegalArgumentException("email must not be blank");
        }

        String key = key(orgId, canonicalEmail);

        // Normalize email inside the stored object as well
        OrganizationApproval normalized = new OrganizationApproval(
                orgId,
                canonicalEmail,
                approval.getOrgRole(),
                approval.getCreatedAt()
        );

        byOrgEmail.put(key, normalized); // upsert semantics
    }

    @Override
    public List<OrganizationApproval> findByEmail(String email) {
        String canonical = canonicalEmail(email);
        if (canonical.isEmpty()) return List.of();

        List<OrganizationApproval> result = new ArrayList<>();
        for (OrganizationApproval a : byOrgEmail.values()) {
            if (canonical.equals(canonicalEmail(a.getEmail()))) {
                result.add(a);
            }
        }

        // Optional: sort for deterministic output (by orgId, then role)
        result.sort((a, b) -> {
            int cmp = Integer.compare(a.getOrganizationId(), b.getOrganizationId());
            if (cmp != 0) return cmp;
            return a.getOrgRole().compareToIgnoreCase(b.getOrgRole());
        });

        return List.copyOf(result);
    }

    @Override
    public List<OrganizationApproval> findByOrganization(int organizationId) {
        if (organizationId <= 0) return List.of();

        List<OrganizationApproval> result = new ArrayList<>();
        for (OrganizationApproval a : byOrgEmail.values()) {
            if (a.getOrganizationId() == organizationId) {
                result.add(a);
            }
        }

        // Optional: sort for deterministic output (by email, then role)
        result.sort((a, b) -> {
            int cmp = canonicalEmail(a.getEmail()).compareTo(canonicalEmail(b.getEmail()));
            if (cmp != 0) return cmp;
            return a.getOrgRole().compareToIgnoreCase(b.getOrgRole());
        });

        return List.copyOf(result);
    }

    @Override
    public boolean exists(int organizationId, String email) {
        if (organizationId <= 0) return false;
        String canonical = canonicalEmail(email);
        if (canonical.isEmpty()) return false;

        String key = key(organizationId, canonical);
        return byOrgEmail.containsKey(key);
    }

    @Override
    public Optional<OrganizationApproval> findOne(int organizationId, String email) {
        if (organizationId <= 0) return Optional.empty();
        String canonical = canonicalEmail(email);
        if (canonical.isEmpty()) return Optional.empty();

        String key = key(organizationId, canonical);
        return Optional.ofNullable(byOrgEmail.get(key));
    }

    /* ----------------------------------------------------------------------
       Helpers
       ---------------------------------------------------------------------- */

    private static String canonicalEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String key(int orgId, String canonicalEmail) {
        return orgId + "|" + canonicalEmail;
    }
}
