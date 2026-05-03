/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: Event.java
 *
 * Purpose:
 *   Domain entity representing an event in the Campus Event Management System.
 *   Each event is owned by exactly one principal:
 *     • a single organizer user (organizerUserId), OR
 *     • an organization (organizationId).
 *
 *   The entity models:
 *     • identity and ownership (immutable)
 *     • editable metadata (title, description, category, location)
 *     • scheduling (start/end)
 *     • registration capacity (used by RegistrationServiceImpl)
 *
 * Design & Security Notes:
 *   • Ownership XOR rule is enforced in the constructor and factory methods.
 *   • Identity fields (id, owner IDs) are final and never mutated.
 *   • Title and capacity are validated in the constructor.
 *   • Category is a restricted enum aligned with database values.
 *   • Controlled setters restrict modification to allowed fields only.
 *   • Status is editable but validated (never null).
 */

package uk.ac.aru.campusevents.domain;

import uk.ac.aru.campusevents.domain.enums.EventCategory;
import uk.ac.aru.campusevents.domain.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.Objects;

@SuppressWarnings("unused")
public final class Event {

    /* ----------------------------------------------------------------------
       Identity & Ownership (immutable)
       ---------------------------------------------------------------------- */

    private final int id;                  // 0 when newly created, assigned later by repository
    private final Integer organizerUserId; // XOR with organizationId
    private final Integer organizationId;  // XOR with organizerUserId

    /* ----------------------------------------------------------------------
       Mutable fields (editable by owner or admin)
       ---------------------------------------------------------------------- */

    private String title;
    private String description;
    private EventCategory category;          // Controlled enum
    private String categoryOtherDescription; // Only used if category == OTHER
    private EventStatus status = EventStatus.DRAFT;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String location;

    /** Capacity for registration logic. Must be > 0. */
    private int capacity;

    /* ----------------------------------------------------------------------
       Constructor (private – use factories)
       ---------------------------------------------------------------------- */

    private Event(int id,
                  Integer organizerUserId,
                  Integer organizationId,
                  String title,
                  String description,
                  EventCategory category,
                  String categoryOtherDescription,
                  EventStatus status,
                  LocalDateTime startDateTime,
                  LocalDateTime endDateTime,
                  String location,
                  int capacity) {

        // XOR ownership rule
        if ((organizerUserId == null && organizationId == null) ||
                (organizerUserId != null && organizationId != null)) {
            throw new IllegalArgumentException(
                    "Event must be owned by exactly one principal: user OR organization"
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title required");
        }

        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }

        this.id = id;
        this.organizerUserId = organizerUserId;
        this.organizationId = organizationId;

        this.title = title.trim();
        this.description = (description == null) ? "" : description.trim();
        this.category = (category == null) ? EventCategory.OTHER : category;
        this.categoryOtherDescription =
                (categoryOtherDescription == null) ? "" : categoryOtherDescription.trim();

        if (status != null) {
            this.status = status;
        }

        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.location = (location == null) ? "" : location.trim();
        this.capacity = capacity;
    }

    /* ----------------------------------------------------------------------
       Factory Methods (preferred creation API)
       ---------------------------------------------------------------------- */

    public static Event newPersonalEvent(int organizerUserId,
                                         String title,
                                         String description,
                                         EventCategory category,
                                         String categoryOtherDescription,
                                         int capacity) {
        return new Event(
                0,                          // new event (id assigned by repository)
                organizerUserId,
                null,                       // XOR enforced
                title,
                description,
                category,
                categoryOtherDescription,
                EventStatus.DRAFT,
                null,
                null,
                "",
                capacity
        );
    }

    public static Event newOrgEvent(int organizationId,
                                    String title,
                                    String description,
                                    EventCategory category,
                                    String categoryOtherDescription,
                                    int capacity) {
        return new Event(
                0,
                null,
                organizationId,             // XOR enforced
                title,
                description,
                category,
                categoryOtherDescription,
                EventStatus.DRAFT,
                null,
                null,
                "",
                capacity
        );
    }

    /**
     * Creates a copy of this event with a newly assigned ID.
     * Identity is immutable; updates require rebuilding the object.
     */
    public Event withId(int newId) {
        return new Event(
                newId,
                organizerUserId,
                organizationId,
                title,
                description,
                category,
                categoryOtherDescription,
                status,
                startDateTime,
                endDateTime,
                location,
                capacity
        );
    }

    /* ----------------------------------------------------------------------
       Getters
       ---------------------------------------------------------------------- */

    public int getId() { return id; }
    public Integer getOrganizerUserId() { return organizerUserId; }
    public Integer getOrganizationId() { return organizationId; }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public EventCategory getCategory() { return category; }
    public String getCategoryOtherDescription() { return categoryOtherDescription; }
    public EventStatus getStatus() { return status; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public String getLocation() { return location; }
    public int getCapacity() { return capacity; }

    /* ----------------------------------------------------------------------
       Controlled Mutability — editable fields only
       ---------------------------------------------------------------------- */

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title).trim();
    }

    public void setDescription(String description) {
        this.description = (description == null) ? "" : description.trim();
    }

    public void setCategory(EventCategory category) {
        this.category = (category == null) ? EventCategory.OTHER : category;
    }

    public void setCategoryOtherDescription(String text) {
        this.categoryOtherDescription = (text == null) ? "" : text.trim();
    }

    public void setStatus(EventStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public void setStartDateTime(LocalDateTime start) {
        this.startDateTime = start;
    }

    public void setEndDateTime(LocalDateTime end) {
        this.endDateTime = end;
    }

    public void setLocation(String location) {
        this.location = (location == null) ? "" : location.trim();
    }

    public void setCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }
        this.capacity = capacity;
    }
}
