/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AuthServiceImpl.java
 * Purpose:
 *   BCrypt-backed authentication service (register + login) with multi-role support.
 * Security & Design Notes:
 *   • Registers users with salted BCrypt hashes; never stores plaintext.
 *   • Verifies passwords using BCrypt's constant-time verifier.
 *   • Normalizes emails to lowercase/trimmed to prevent duplicate accounts.
 *   • Does not log credentials or hashes; wipes password char[] after use.
 */
package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.User;
import uk.ac.aru.campusevents.domain.UserOrganization;
import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.dto.UserSession;
import uk.ac.aru.campusevents.repository.OrganizationApprovalRepository;
import uk.ac.aru.campusevents.repository.UserOrganizationRepository;
import uk.ac.aru.campusevents.repository.UserRepository;
import uk.ac.aru.campusevents.security.PasswordHasher;
import uk.ac.aru.campusevents.service.AuthService;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class AuthServiceImpl implements AuthService {

    private static final int MIN_PASSWORD_LEN = 8;

    private final UserRepository userRepo;
    private final UserOrganizationRepository userOrgRepo;
    private final OrganizationApprovalRepository orgApprovalRepo;

    public AuthServiceImpl(UserRepository userRepo,
                           UserOrganizationRepository userOrgRepo,
                           OrganizationApprovalRepository orgApprovalRepo) {
        this.userRepo = Objects.requireNonNull(userRepo);
        this.userOrgRepo = Objects.requireNonNull(userOrgRepo);
        this.orgApprovalRepo = Objects.requireNonNull(orgApprovalRepo);
    }

    @Override
    public int registerUser(String firstName,
                            String lastName,
                            String email,
                            char[] rawPassword,
                            Set<Role> roles) {
        if (firstName == null || firstName.isBlank()) throw new IllegalArgumentException("First name required");
        if (lastName  == null || lastName.isBlank())  throw new IllegalArgumentException("Last name required");
        if (email     == null || email.isBlank())     throw new IllegalArgumentException("Email required");
        if (rawPassword == null || rawPassword.length < MIN_PASSWORD_LEN) {
            throw new IllegalArgumentException("Password too short");
        }
        if (roles == null || roles.isEmpty()) throw new IllegalArgumentException("At least one role required");

        final String canonicalEmail = email.trim().toLowerCase();

        // Early uniqueness check
        userRepo.findByEmail(canonicalEmail).ifPresent(u -> {
            throw new IllegalStateException("Email already registered");
        });

        String hash;
        try {
            hash = PasswordHasher.hash(rawPassword); // salted BCrypt
        } finally {
            Arrays.fill(rawPassword, '\0'); // wipe regardless of success/failure
        }

        var safeRoles = EnumSet.copyOf(roles); // defensive copy
        var user = User.newUser(
                firstName.trim(),
                lastName.trim(),
                canonicalEmail,
                hash,
                safeRoles
        );

        // Persist user
        int newUserId = userRepo.create(user);

        // After creation: check if this email has any org approvals and create memberships
        var approvals = orgApprovalRepo.findByEmail(canonicalEmail);
        for (var a : approvals) {
            userOrgRepo.add(new UserOrganization(
                    newUserId,
                    a.getOrganizationId(),
                    a.getOrgRole()
            ));
        }

        return newUserId;
    }

    @Override
    public UserSession login(String email, char[] rawPassword) {
        if (email == null || rawPassword == null) throw new IllegalArgumentException("Credentials required");
        final String canonicalEmail = email.trim().toLowerCase();

        var user = userRepo.findByEmail(canonicalEmail)
                .orElseThrow(() -> new SecurityException("Invalid credentials"));

        boolean ok;
        try {
            ok = PasswordHasher.verify(rawPassword, user.getPasswordHash());
        } finally {
            Arrays.fill(rawPassword, '\0');
        }
        if (!ok) throw new SecurityException("Invalid credentials");

        String displayName = user.getFirstName() + " " + user.getLastName();
        return new UserSession(user.getId(), displayName, user.getEmail(), user.getRoles());
    }
}
