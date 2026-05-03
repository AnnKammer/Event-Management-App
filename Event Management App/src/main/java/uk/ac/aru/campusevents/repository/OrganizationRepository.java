/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: OrganizationRepository.java
 * Purpose:
 *   Defines the persistence port (DAO) for storing and retrieving organizations
 *   such as societies, NGOs, companies, and university departments.
 * Security & Design Notes:
 *   • Interface hides persistence details behind a stable abstraction.
 *   • Callers use IDs rather than names to avoid ambiguity and minimize PII in logs.
 *   • Safe for both in-memory and database-backed implementations.
 *   • Implementations should normalize organization names (trim, case-insensitive).
 */
package uk.ac.aru.campusevents.repository;

import uk.ac.aru.campusevents.domain.Organization;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link Organization} entities.
 * Provides CRUD-style access to organization records and lookup methods
 * by identifier or name. The interface remains implementation-agnostic,
 * enabling easy substitution of persistence technologies.
 */
@SuppressWarnings("unused")
public interface OrganizationRepository {

    /**
     * Persists a new organization record.
     *
     * @param org the organization to create (must satisfy domain invariants)
     * @return the generated identifier
     */
    int create(Organization org);

    /**
     * Finds an organization by its unique identifier.
     *
     * @param id organization ID
     * @return an {@link Optional} containing the organization if found, or empty if not
     */
    Optional<Organization> findById(int id);

    /**
     * Finds an organization by its unique name.
     * Primarily intended for seeding or administrative screens.
     *
     * @param name case-insensitive organization name
     * @return an {@link Optional} containing the organization if found, or empty otherwise
     */
    Optional<Organization> findByName(String name);

    /**
     * Retrieves all organizations in the system.
     * May be limited or paginated in database-backed implementations.
     *
     * @return a list of all organizations (possibly empty)
     */
    List<Organization> findAll();
}



