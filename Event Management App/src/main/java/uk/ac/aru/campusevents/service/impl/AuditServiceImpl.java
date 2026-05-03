/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: AuditServiceImpl.java
 * Purpose:
 *   Records audit logs and enforces RBAC for queries.
 * RBAC:
 *   • ADMIN  → full access to all audit entries.
 *   • ORGANIZER → can query only audit entries for their own personal events.
 *   • STUDENT → no audit access.
 * Notes:
 *   • Uses AuditRepository.save/find.
 *   • Builds AuditLog entries via AuditLog.newEntry(...).
 *   • Uses getEntityId() (not "getEntryid").
 */
package uk.ac.aru.campusevents.service.impl;

import uk.ac.aru.campusevents.domain.AuditLog;
import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.enums.AuditAction;
import uk.ac.aru.campusevents.domain.enums.AuditEntity;
import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.dto.AuditFilter;
import uk.ac.aru.campusevents.exceptions.ForbiddenException;
import uk.ac.aru.campusevents.repository.AuditRepository;
import uk.ac.aru.campusevents.repository.EventRepository;
import uk.ac.aru.campusevents.repository.UserRepository;
import uk.ac.aru.campusevents.service.AuditService;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuditServiceImpl implements AuditService {
    private final AuditRepository repo;
    private final EventRepository eventRepo;
    private final UserRepository userRepo;

    public AuditServiceImpl(AuditRepository repo, EventRepository eventRepo, UserRepository userRepo) {
        this.repo = Objects.requireNonNull(repo);
        this.eventRepo = Objects.requireNonNull(eventRepo);
        this.userRepo = Objects.requireNonNull(userRepo);
    }

    @Override
    public void record(AuditAction action, AuditEntity entity, Integer entityId,
                       Integer actorUserId, String detailsJson) {
        // Build an immutable log entry and persist it
        AuditLog entry = AuditLog.newEntry(action, entity, entityId, actorUserId, detailsJson);
        repo.save(entry);
    }

    @Override
    public List<AuditLog> find(AuditFilter filter, int actorUserId) {
        var user = userRepo.findById(actorUserId)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        Set<Role> roles = user.getRoles();

        // ADMIN → unrestricted
        if (roles.contains(Role.ADMIN)) {
            return repo.find(filter);
        }

        // ORGANIZER → only their own personal events' logs
        if (roles.contains(Role.ORGANIZER)) {
            var myEventIds = eventRepo.findByOrganizer(actorUserId).stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());

            // If the filter pinpoints a specific event, allow only if it's one of mine
            if (filter.entity() == AuditEntity.EVENT && filter.entityId() != null) {
                if (!myEventIds.contains(filter.entityId())) {
                    throw new ForbiddenException("Audit access denied for this event");
                }
                return repo.find(filter);
            }

            // Otherwise scope to my events: constrain to EVENT entries and post-filter by entityId
            var scoped = new AuditFilter(
                    AuditEntity.EVENT,
                    null,                        // all of my events
                    filter.actorUserId(),
                    filter.action(),
                    filter.from(),
                    filter.to(),
                    filter.limit()
            );

            return repo.find(scoped).stream()
                    .filter(l -> myEventIds.contains(l.getEntityId()))
                    .toList();
        }

        // STUDENT (or others) → no access
        throw new ForbiddenException("Audit access denied");
    }
}



