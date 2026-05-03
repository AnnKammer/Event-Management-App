/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: InMemoryOrganizationRepository.java
 * Purpose:
 *   Thread-safe in-memory implementation of OrganizationRepository for development/testing.
 * Security & Design Notes:
 *   • ConcurrentHashMap + AtomicInteger ensure thread safety.
 *   • Identity is assigned here (reconstructs Organization with id); no external mutation.
 *   • No PII logged; lookups by id or normalized name only.
 *   • Enforces unique organization names (case-insensitive, trimmed).
 */
package uk.ac.aru.campusevents.repository.memory;

import uk.ac.aru.campusevents.domain.Organization;
import uk.ac.aru.campusevents.repository.OrganizationRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public final class InMemoryOrganizationRepository implements OrganizationRepository {

    private final AtomicInteger seq = new AtomicInteger(1);
    private final Map<Integer, Organization> byId = new ConcurrentHashMap<>();
    // normalized name (trimmed, lower-cased) -> id
    private final Map<String, Integer> idByName = new ConcurrentHashMap<>();

    @Override
    public int create(Organization org) {
        Objects.requireNonNull(org, "organization cannot be empty");
        final String norm = normalize(org.getName());
        if (norm.isEmpty()) throw new IllegalArgumentException("Please enter a valid organization name");

        // Reserve name first to enforce uniqueness atomically
        final int newId = seq.getAndIncrement();
        Integer existing = idByName.putIfAbsent(norm, newId);
        if (existing != null) {
            throw new IllegalStateException("Organization name already exists: " + org.getName());
        }

        Organization withId = new Organization(newId, org.getName(), org.getType());
        byId.put(newId, withId);
        return newId;
    }

    @Override
    public Optional<Organization> findById(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Organization> findByName(String name) {
        if (name == null) return Optional.empty();
        Integer id = idByName.get(normalize(name));
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Organization> findAll() {
        return byId.values().stream()
                .sorted(Comparator.comparing(Organization::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /* ---------- Helpers ---------- */

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
