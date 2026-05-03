/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: InMemoryEventRepository.java
 * Purpose:
 *   In-memory implementation of the EventRepository interface for development and testing.
 *   Provides CRUD-style storage of Event entities without database connectivity.
 *
 * Security & Design Notes:
 *   • Thread-safe: ConcurrentHashMap + AtomicInteger for id sequence.
 *   • Encapsulation: identity assigned here via Event.withId(...); callers cannot re-parent events.
 *   • No persistence beyond runtime: suitable for demos/tests only.
 *   • No PII logged; search is performed in-memory on already-sanitized fields.
 *   • Event capacity is stored on the Event entity itself and used by services
 *     (e.g. RegistrationService) for registration and waitlist decisions.
 */

package uk.ac.aru.campusevents.repository.memory;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.EventCategory;
import uk.ac.aru.campusevents.dto.EventSearchCriteria;
import uk.ac.aru.campusevents.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public final class InMemoryEventRepository implements EventRepository {

    private final AtomicInteger seq = new AtomicInteger(1);
    private final Map<Integer, Event> store = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public int create(Event e) {
        int id = seq.getAndIncrement();
        // Preserve ownership invariants (XOR organizer/organization) via domain factory
        Event withId = e.withId(id);
        store.put(id, withId);
        return id;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    public void update(Event e) {
        if (e.getId() <= 0) {
            throw new IllegalArgumentException("Event id required");
        }
        if (!store.containsKey(e.getId())) {
            throw new NoSuchElementException("Event not found: id=" + e.getId());
        }
        store.put(e.getId(), e);
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    public void delete(int id) {
        store.remove(id);
    }

    // -------------------------------------------------------------------------
    // FIND BY ID
    // -------------------------------------------------------------------------

    @Override
    public Optional<Event> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    // -------------------------------------------------------------------------
    // SEARCH
    // -------------------------------------------------------------------------

    @Override
    public List<Event> search(EventSearchCriteria c) {
        // Allow null criteria → return all, sorted by startDateTime (nulls last)
        final EventCategory cat = (c == null) ? null : c.category();
        final String text       = (c == null || c.text()     == null) ? null : c.text().trim();
        final String loc        = (c == null || c.location() == null) ? null : c.location().trim();
        final var from          = (c == null) ? null : c.startFrom();
        final var to            = (c == null) ? null : c.startTo();

        return store.values().stream()
                .filter(ev ->
                        // Category match if provided
                        (cat == null || ev.getCategory() == cat)
                                // Text contains across title/description/category/location (case-insensitive)
                                && (isBlank(text) || containsIgnoreCase(
                                ev.getTitle()
                                        + " " + safe(ev.getDescription())
                                        + " " + ev.getCategory().name()
                                        + " " + safe(ev.getLocation()),
                                text))
                                // Location contains (case-insensitive)
                                && (isBlank(loc) || containsIgnoreCase(safe(ev.getLocation()), loc))
                                // Start date range (inclusive) if event has a start
                                && (from == null || (ev.getStartDateTime() != null
                                && !ev.getStartDateTime().toLocalDate().isBefore(from)))
                                && (to == null || (ev.getStartDateTime() != null
                                && !ev.getStartDateTime().toLocalDate().isAfter(to)))
                )
                .sorted(Comparator.comparing(
                        InMemoryEventRepository::startOrMax // null starts sorted last
                ))
                .toList();
    }


    // -------------------------------------------------------------------------
    // FIND BY ORGANIZER
    // -------------------------------------------------------------------------

    @Override
    public List<Event> findByOrganizer(int organizerUserId) {
        return store.values().stream()
                .filter(ev -> ev.getOrganizerUserId() != null
                        && ev.getOrganizerUserId() == organizerUserId)
                .sorted(Comparator.comparing(Event::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null
                && needle != null
                && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private static LocalDateTime startOrMax(Event e) {
        LocalDateTime dt = e.getStartDateTime();
        return dt == null ? LocalDateTime.MAX : dt;
    }
}
