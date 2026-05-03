/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: InMemoryRegistrationRepository.java
 * Purpose:
 *   Simple in-memory implementation of {@link RegistrationRepository} for
 *   demos and unit testing. Stores all registrations in heap storage.
 *
 * Design Notes:
 *   • Mirrors the JDBC repository API, but stores everything in Java Lists.
 *   • Enforces no business rules — services handle RBAC and transitions.
 *   • Capacity is handled by service via countByEvent().
 *
 * Storage:
 *   List<Registration> acts as our fake table.
 */
package uk.ac.aru.campusevents.repository.memory;

import uk.ac.aru.campusevents.domain.Registration;
import uk.ac.aru.campusevents.domain.enums.RegStatus;
import uk.ac.aru.campusevents.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class InMemoryRegistrationRepository implements RegistrationRepository {

    private final List<Registration> data = new ArrayList<>();
    private final AtomicInteger idSeq = new AtomicInteger(1);

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public int create(Registration r) {
        int id = idSeq.getAndIncrement();
        Registration saved = new Registration(
                id,
                r.getEventId(),
                r.getStudentId(),
                r.getStatus(),
                r.getRegisteredAt() == null ? LocalDateTime.now() : r.getRegisteredAt()
        );
        data.add(saved);
        return id;
    }

    // -------------------------------------------------------------------------
    // REPLACE
    // -------------------------------------------------------------------------

    @Override
    public void replace(Registration r) {
        deleteById(r.getId());
        data.add(r);
    }

    private void deleteById(int id) {
        data.removeIf(x -> x.getId() == id);
    }

    // -------------------------------------------------------------------------
    // UPDATE STATUS
    // -------------------------------------------------------------------------

    @Override
    public void updateStatus(int eventId, int studentId, RegStatus status) {
        for (int i = 0; i < data.size(); i++) {
            Registration r = data.get(i);
            if (r.getEventId() == eventId && r.getStudentId() == studentId) {
                Registration updated = new Registration(
                        r.getId(),
                        r.getEventId(),
                        r.getStudentId(),
                        status,
                        r.getRegisteredAt()
                );
                data.set(i, updated);
            }
        }
    }

    // -------------------------------------------------------------------------
    // FIND ACTIVE
    // -------------------------------------------------------------------------

    @Override
    public Optional<Registration> findActive(int eventId, int studentId) {
        return data.stream()
                .filter(r -> r.getEventId() == eventId)
                .filter(r -> r.getStudentId() == studentId)
                .filter(r -> r.getStatus() != RegStatus.CANCELLED)
                .max(Comparator.comparing(Registration::getRegisteredAt));
    }

    // -------------------------------------------------------------------------
    // FIND CONFIRMED (REGISTERED/APPROVED)
    // -------------------------------------------------------------------------

    @Override
    public List<Registration> findByEvent(int eventId) {
        return data.stream()
                .filter(r -> r.getEventId() == eventId)
                .filter(r -> r.getStatus() == RegStatus.REGISTERED ||
                        r.getStatus() == RegStatus.APPROVED)
                .sorted(Comparator.comparing(Registration::getRegisteredAt))
                .toList();
    }

    // -------------------------------------------------------------------------
    // FIND ALL STATUSES
    // -------------------------------------------------------------------------

    @Override
    public List<Registration> findAllByEvent(int eventId) {
        return data.stream()
                .filter(r -> r.getEventId() == eventId)
                .sorted(Comparator.comparing(Registration::getRegisteredAt))
                .toList();
    }

    // -------------------------------------------------------------------------
    // FIND BY STUDENT
    // -------------------------------------------------------------------------

    @Override
    public List<Registration> findByStudent(int studentId) {
        return data.stream()
                .filter(r -> r.getStudentId() == studentId)
                .sorted(Comparator.comparing(Registration::getRegisteredAt).reversed())
                .toList();
    }

    // -------------------------------------------------------------------------
    // COUNT BY EVENT (counts only confirmed (REGISTERED/APPROVED) registrations for capacity checks)
    // -------------------------------------------------------------------------

    @Override
    public long countByEvent(int eventId) {
        return data.stream()
                .filter(r -> r.getEventId() == eventId)
                .filter(r -> r.getStatus() == RegStatus.REGISTERED ||
                        r.getStatus() == RegStatus.APPROVED)
                .count();
    }
}
