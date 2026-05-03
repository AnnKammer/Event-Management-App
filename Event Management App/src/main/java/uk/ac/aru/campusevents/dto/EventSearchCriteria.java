/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: EventSearchCriteria.java
 * Purpose:
 *   Immutable filter object used to encapsulate search parameters for event queries.
 *
 * Security & Design Notes:
 *   • Record is immutable → prevents tampering between layers.
 *   • Contains no personally identifiable information (PII).
 *   • Safe to log or serialize for debugging.
 *
 * Usage:
 *   Passed from controller/UI to service layer to perform filtered event lookups.
 *   Any field may be null, meaning “do not filter on this value”.
 */

package uk.ac.aru.campusevents.dto;

import uk.ac.aru.campusevents.domain.enums.EventCategory;
import java.time.LocalDate;

/**
 * Immutable Data Transfer Object representing search filters for events.
 * All fields are optional (null = no filter).
 *
 * category    → fixed enum, matching the domain + database + search logic.
 * text        → free-text match on title, description, category name, or location.
 * startFrom   → inclusive lower bound on start date.
 * startTo     → inclusive upper bound on start date.
 * location    → substring match, case-insensitive.
 */
public record EventSearchCriteria(
        EventCategory category,
        String text,
        LocalDate startFrom,
        LocalDate startTo,
        String location
) { }
