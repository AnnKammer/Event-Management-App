/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: InMemoryUserRepository.java
 * Purpose:
 *   In-memory implementation of UserRepository for fast development/testing.
 * Security & Design Notes:
 *   • Not for production — no persistence beyond runtime.
 *   • Thread-safe maps; ids assigned atomically.
 *   • Emails are normalized (trim/lowercase) and enforced unique.
 *   • No plaintext passwords stored anywhere; only hashed values in User.
 */
package uk.ac.aru.campusevents.repository.memory;

import uk.ac.aru.campusevents.domain.User;
import uk.ac.aru.campusevents.repository.UserRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public final class InMemoryUserRepository implements UserRepository {

    /** Auto-incrementing id sequence. */
    private final AtomicInteger seq = new AtomicInteger(1);

    /** Primary store: id -> User. */
    private final Map<Integer, User> store = new ConcurrentHashMap<>();

    /** Secondary index: normalizedEmail -> id (unique). */
    private final Map<String, Integer> idByEmail = new ConcurrentHashMap<>();

    @Override
    public int create(User user) {
        Objects.requireNonNull(user, "User cannot be empty");
        final String normEmail = normalize(user.getEmail());
        if (normEmail.isEmpty()) throw new IllegalArgumentException("Email cannot be empty");

        // Reserve email first to enforce uniqueness atomically
        final int newId = seq.getAndIncrement();
        Integer previous = idByEmail.putIfAbsent(normEmail, newId);
        if (previous != null) {
            throw new IllegalStateException("Email already exists: " + user.getEmail());
        }

        // Reconstruct with assigned id (immutability of identity)
        User withId = new User(
                newId,
                Objects.requireNonNull(user.getFirstName(), "First name cannot be empty"),
                Objects.requireNonNull(user.getLastName(), "Last name cannot be empty"),
                normEmail, // already normalized
                Objects.requireNonNull(user.getPasswordHash(), "Password cannot be empty"),
                Objects.requireNonNull(user.getRoles(), "Roles cannot be empty"),
                user.getCreatedAt()
        );
        store.put(newId, withId);
        return newId;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        Integer id = idByEmail.get(normalize(email));
        return id == null ? Optional.empty() : Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<User> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<User> findAllByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<User> out = new ArrayList<>(ids.size());
        for (int id : ids) {
            User u = store.get(id);
            if (u != null) out.add(u);
        }
        return out;
    }

    /* ---------- Helpers ---------- */

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
