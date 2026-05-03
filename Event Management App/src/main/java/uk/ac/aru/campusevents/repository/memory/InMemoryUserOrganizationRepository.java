/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: InMemoryUserOrganizationRepository.java
 * Purpose:
 *   In-memory implementation of UserOrganizationRepository to simulate
 *   organization membership and management privileges during development/tests.
 * Security & Design Notes:
 *   • Thread-safe: ConcurrentHashMap-based indexes; atomic compute operations.
 *   • RBAC probe methods (canManage) are O(1) due to composite indexes.
 *   • Uniqueness enforced: one link per (userId, orgId); re-adding replaces the existing link.
 *   • Returns are immutable snapshots; no external mutation of repository state.
 */
package uk.ac.aru.campusevents.repository.memory;

import uk.ac.aru.campusevents.domain.UserOrganization;
import uk.ac.aru.campusevents.repository.UserOrganizationRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unused")
public final class InMemoryUserOrganizationRepository implements UserOrganizationRepository {

    /** Index A: orgId -> (userId -> membership) */
    private final ConcurrentMap<Integer, ConcurrentMap<Integer, UserOrganization>> byOrgUser = new ConcurrentHashMap<>();

    /** Index B: userId -> (orgId -> membership) for fast reverse lookups */
    private final ConcurrentMap<Integer, ConcurrentMap<Integer, UserOrganization>> byUserOrg = new ConcurrentHashMap<>();

    @Override
    public void add(UserOrganization membership) {
        Objects.requireNonNull(membership, "membership cannot be empty");
        final int orgId = membership.getOrganizationId();
        final int userId = membership.getUserId();

        // Upsert atomically in both indexes
        byOrgUser.computeIfAbsent(orgId, k -> new ConcurrentHashMap<>())
                .put(userId, membership);

        byUserOrg.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(orgId, membership);
    }

    @Override
    public boolean isMember(int userId, int orgId) {
        var inner = byOrgUser.get(orgId);
        return inner != null && inner.containsKey(userId);
    }

    @Override
    public boolean canManage(int userId, int orgId) {
        var inner = byOrgUser.get(orgId);
        if (inner == null) return false;
        var uo = inner.get(userId);
        return uo != null && isManagerRole(uo.getRoleInOrg());
    }

    @Override
    public List<UserOrganization> membershipsForUser(int userId) {
        var inner = byUserOrg.get(userId);
        if (inner == null || inner.isEmpty()) return List.of();
        // Return a stable, immutable snapshot sorted by orgId
        var list = new ArrayList<>(inner.values());
        list.sort(Comparator.comparingInt(UserOrganization::getOrganizationId));
        return List.copyOf(list);
    }

    /* ---------- Helpers ---------- */

    private static boolean isManagerRole(String role) {
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return "MANAGER".equals(r) || "OWNER".equals(r);
    }
}
