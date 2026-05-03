/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: Authorization.java
 * Purpose:
 *   Centralized helper for evaluating GUI-level role permissions.
 *
 *   Provides readable methods for controllers to check:
 *     • Whether the current user has specific roles
 *     • Whether a GUI feature should be visible or accessible
 *
 * Why this exists:
 *   The service layer already enforces RBAC. However, the GUI needs a simple
 *   way to show/hide buttons, dashboards, or sections based on roles.
 *
 * Security Notes:
 *   • These checks are purely for UX (visibility / navigation).
 *   • Real access control remains in the service layer.
 */

package uk.ac.aru.campusevents.ui;

import uk.ac.aru.campusevents.domain.enums.Role;
import uk.ac.aru.campusevents.dto.UserSession;

public final class Authorization {

    // ------------------------ Public Role Checks ------------------------

    public static boolean hasRole(UserSession session, Role role) {
        return session != null && session.roles().contains(role);
    }

    public static boolean isStudent(UserSession session) {
        return hasRole(session, Role.STUDENT);
    }

    public static boolean isOrganizer(UserSession session) {
        return hasRole(session, Role.ORGANIZER);
    }

    public static boolean isAdmin(UserSession session) {
        return hasRole(session, Role.ADMIN);
    }

    private Authorization() {}
}
