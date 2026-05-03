/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: InMemoryFeedbackRepository.java
 * Purpose:
 *   In-memory implementation of the FeedbackRepository for development/testing.
 *   Stores EventFeedback without a database and supports lookups needed by services.
 * Security & Design Notes:
 *   • Thread-safe maps (ConcurrentHashMap) and atomic id sequence.
 *   • Enforces one feedback per (eventId, userId) to surface logic errors early.
 *   • No persistence beyond runtime; suitable for demos/tests only.
 *   • No PII beyond userId is stored in this adapter.
 */
package uk.ac.aru.campusevents.repository.memory;

import uk.ac.aru.campusevents.domain.EventFeedback;
import uk.ac.aru.campusevents.repository.FeedbackRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory adapter for {@link FeedbackRepository}.
 * Uses two maps:
 *  • byId: id -> feedback
 *  • idByEvUser: "eventId:userId" -> id (enforces uniqueness)
 */
@SuppressWarnings("unused")
public final class InMemoryFeedbackRepository implements FeedbackRepository {

    private final AtomicInteger seq = new AtomicInteger(1);
    private final Map<Integer, EventFeedback> byId = new ConcurrentHashMap<>();
    // key = "eventId:userId" to enforce one feedback per (event,user)
    private final Map<String, Integer> idByEvUser = new ConcurrentHashMap<>();

    private static String key(int eventId, int userId) { return eventId + ":" + userId; }

    @Override
    public int create(EventFeedback f) {
        Objects.requireNonNull(f, "feedback cannot be empty");
        final String k = key(f.getEventId(), f.getUserId());

        // Enforce uniqueness atomically on the (eventId,userId) index first.
        final int id = seq.getAndIncrement();
        final Integer prev = idByEvUser.putIfAbsent(k, id);
        if (prev != null) {
            throw new IllegalStateException("Feedback already exists for eventId=" +
                    f.getEventId() + ", userId=" + f.getUserId());
        }

        // Now publish the feedback by id.
        var withId = new EventFeedback(
                id, f.getEventId(), f.getUserId(), f.getRating(), f.getComment(), f.getCreatedAt()
        );
        byId.put(id, withId);
        return id;
    }

    @Override
    public Optional<EventFeedback> findByEventAndUser(int eventId, int userId) {
        Integer id = idByEvUser.get(key(eventId, userId));
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<EventFeedback> findByEvent(int eventId) {
        return byId.values().stream()
                .filter(fb -> fb.getEventId() == eventId)
                .sorted(Comparator.comparing(EventFeedback::getCreatedAt)
                        .thenComparing(EventFeedback::getUserId))
                .toList();
    }
}
