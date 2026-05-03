/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AuditingAuthService.java
 * Purpose:
 *   AuthService decorator that records audit logs for register/login.
 * Security & Design Notes:
 *   • Writes CREATE (on register), LOGIN_OK / LOGIN_FAIL (on login).
 *   • Minimizes PII in detailsJson: masks email local-part (e.g., a***@aru.ac.uk).
 *   • Uses actorUserId where possible (user id on success; best-effort on failure).
 *   • Delegates all business logic to the wrapped AuthService.
 */
package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.enums.AuditAction;
import uk.ac.aru.campusevents.domain.enums.AuditEntity;
import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.dto.UserSession;
import uk.ac.aru.campusevents.repository.UserRepository;
import uk.ac.aru.campusevents.service.AuditService;
import uk.ac.aru.campusevents.service.AuthService;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuditingAuthService implements AuthService {
    private final AuthService delegate;
    private final AuditService audit;
    private final UserRepository userRepo;

    /**
     * @param delegate the real AuthService implementation (e.g., AuthServiceImpl)
     * @param audit    the AuditService to record entries
     * @param userRepo used to map email→user id for best-effort LOGIN_FAIL auditing
     */
    public AuditingAuthService(AuthService delegate, AuditService audit, UserRepository userRepo) {
        this.delegate = Objects.requireNonNull(delegate);
        this.audit = Objects.requireNonNull(audit);
        this.userRepo = Objects.requireNonNull(userRepo);
    }

    @Override
    public int registerUser(String firstName,
                            String lastName,
                            String email,
                            char[] rawPassword,
                            Set<Role> roles) {
        int newUserId = delegate.registerUser(firstName, lastName, email, rawPassword, roles);

        // Build minimal details JSON (masked email + roles + non-sensitive name)
        String details = json(
                "event", "register",
                "email", maskEmail(email),
                "firstName", safeTruncate(firstName, 60),
                "lastName", safeTruncate(lastName, 60),
                "roles", rolesAsCsv(roles)
        );
        audit.record(AuditAction.CREATE, AuditEntity.USER, newUserId, newUserId, details);
        return newUserId;
    }

    @Override
    public UserSession login(String email, char[] rawPassword) {
        String canonicalEmail = (email == null ? "" : email.trim().toLowerCase());

        try {
            // Delegate performs the actual authentication (BCrypt verify, etc.)
            UserSession session = delegate.login(canonicalEmail, rawPassword);

            String detailsOk = json(
                    "event", "login",
                    "email", maskEmail(canonicalEmail),
                    "roles", rolesAsCsv(session.roles()),
                    "result", "ok"
            );
            audit.record(AuditAction.LOGIN_OK, AuditEntity.USER,
                    session.userId(), session.userId(), detailsOk);
            return session;

        } catch (RuntimeException ex) {
            // Best-effort: try to look up user id by email for the audit record
            Integer userId = userRepo.findByEmail(canonicalEmail)
                    .map(u -> u.getId())
                    .orElse(null);

            String detailsFail = json(
                    "event", "login",
                    "email", maskEmail(canonicalEmail),
                    "result", "fail",
                    "reason", safeTruncate(ex.getMessage(), 120)
            );
            audit.record(AuditAction.LOGIN_FAIL, AuditEntity.USER, userId, userId, detailsFail);

            // Preserve original behavior (AuthServiceImpl throws SecurityException)
            throw ex;
        }
    }

    /* ---------- helpers (no external deps; simple/minimal JSON) ---------- */

    private static String rolesAsCsv(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) return "";
        return roles.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    /**
     * Masks an email by replacing most of the local-part with '*'.
     * Example: "alice@aru.ac.uk" → "a***@aru.ac.uk"
     */
    private static String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) return "";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 1) return "*@" + domain;
        return local.charAt(0) + "***@" + domain;
    }

    private static String safeTruncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    /**
     * Tiny helper to build flat JSON without pulling a library.
     * Pairs must be even: key1, val1, key2, val2, ...
     * Values are string-escaped minimally for quotes and backslashes.
     */
    private static String json(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append(',');
            String key = String.valueOf(kv[i]);
            String val = (i + 1 < kv.length) ? String.valueOf(kv[i + 1]) : "";
            sb.append('"').append(escape(key)).append('"').append(':')
                    .append('"').append(escape(val)).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}


