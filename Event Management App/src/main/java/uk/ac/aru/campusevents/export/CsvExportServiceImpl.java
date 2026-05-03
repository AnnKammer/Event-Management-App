/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: CsvExportServiceImpl.java
 * Purpose:
 *   Composes repositories to generate CSV exports:
 *     • Attendees for a given event (confirmed only)
 *     • A student's own registered events
 * Security & Design Notes:
 *   • Exports only REGISTERED/APPROVED registrations.
 *   • CSV fields are escaped and formula-neutralized to prevent injection.
 *   • No sensitive fields (e.g., password hashes) are ever read or exported.
 */
package uk.ac.aru.campusevents.export;

import uk.ac.aru.campusevents.domain.Event;
import uk.ac.aru.campusevents.domain.Registration;
import uk.ac.aru.campusevents.repository.EventRepository;
import uk.ac.aru.campusevents.repository.RegistrationRepository;
import uk.ac.aru.campusevents.repository.UserRepository;
import uk.ac.aru.campusevents.service.CsvExportService;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CsvExportServiceImpl implements CsvExportService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final EventRepository eventRepo;
    private final RegistrationRepository regRepo;
    private final UserRepository userRepo;

    public CsvExportServiceImpl(EventRepository eventRepo,
                                RegistrationRepository regRepo,
                                UserRepository userRepo) {
        this.eventRepo = Objects.requireNonNull(eventRepo);
        this.regRepo = Objects.requireNonNull(regRepo);
        this.userRepo = Objects.requireNonNull(userRepo);
    }

    /**
     * Export confirmed attendees for a single event.
     *
     * Columns:
     *   event_id,event_title,student_id,student_name,student_email
     */
    @Override
    public String exportAttendeesCsv(int eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        // Repo contract: findByEvent(eventId) already returns confirmed attendees
        // Make a mutable copy so we can sort safely.
        List<Registration> regs = new ArrayList<>(regRepo.findByEvent(eventId));

        if (regs.isEmpty()) {
            // Still return a header-only CSV – easier for the caller to handle.
            return "event_id,event_title,student_id,student_name,student_email\n";
        }

        // Load users once; map by id
        var userIds = regs.stream()
                .map(Registration::getStudentId)
                .distinct()
                .toList();

        var users = userRepo.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), Function.identity()));

        // Sort by "First Last" (case-insensitive), then by user id for stability
        regs.sort(Comparator
                .comparing((Registration r) -> {
                    var u = users.get(r.getStudentId());
                    return (u != null)
                            ? (u.getFirstName() + " " + u.getLastName())
                            : "";
                }, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Registration::getStudentId));

        StringBuilder sb = new StringBuilder();
        sb.append("event_id,event_title,student_id,student_name,student_email\n");

        for (var r : regs) {
            var u = users.get(r.getStudentId());
            if (u == null) {
                // Defensive: skip registrations whose user record is missing.
                continue;
            }

            appendCsv(sb, event.getId());                       sb.append(',');
            appendCsv(sb, event.getTitle());                    sb.append(',');
            appendCsv(sb, u.getId());                           sb.append(',');
            appendCsv(sb, u.getFirstName() + " " + u.getLastName()); sb.append(',');
            appendCsv(sb, u.getEmail());                        sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Export all events a student is registered/approved for.
     *
     * Columns:
     *   event_id,event_title,start,end,location,status
     */
    @Override
    public String exportMyEventsCsv(int studentId) {
        // Repo contract: findByStudent(studentId) returns REGISTERED/APPROVED only
        List<Registration> regs = new ArrayList<>(regRepo.findByStudent(studentId));

        // Sort by event start time (nulls last), then by event id for stability
        regs.sort(Comparator.comparing(
                        (Registration r) -> eventRepo.findById(r.getEventId())
                                .map(Event::getStartDateTime)
                                .orElse(null),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Registration::getEventId));

        StringBuilder sb = new StringBuilder();
        sb.append("event_id,event_title,start,end,location,status\n");

        for (var r : regs) {
            var evOpt = eventRepo.findById(r.getEventId());
            if (evOpt.isEmpty()) continue;
            var e = evOpt.get();

            appendCsv(sb, e.getId());                           sb.append(',');
            appendCsv(sb, e.getTitle());                        sb.append(',');
            appendCsv(sb, e.getStartDateTime() == null ? null : TS.format(e.getStartDateTime())); sb.append(',');
            appendCsv(sb, e.getEndDateTime() == null ? null : TS.format(e.getEndDateTime()));     sb.append(',');
            appendCsv(sb, e.getLocation());                     sb.append(',');
            appendCsv(sb, r.getStatus());                       sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Appends a value to the CSV, with escaping and formula neutralization.
     *
     * Rules:
     *   • Null   → empty cell (no characters written; caller handles commas)
     *   • Quotes inside are doubled (") → ("")
     *   • If the value:
     *        - contains comma, quote, newline, OR
     *        - starts with = + - @ or tab (spreadsheet formulas),
     *     then we wrap it in quotes and, if dangerous, prefix it with a single quote.
     */
    private static void appendCsv(StringBuilder sb, Object value) {
        if (value == null) {
            // Empty cell, no characters written. The caller already writes commas
            // between cells, so this becomes ,, as expected.
            return;
        }

        String s = String.valueOf(value);
        boolean dangerousStart = !s.isEmpty() && "=+-@\t".indexOf(s.charAt(0)) >= 0;
        boolean needsQuotes = dangerousStart
                || s.contains(",")
                || s.contains("\"")
                || s.contains("\n");

        String escaped = s.replace("\"", "\"\"");

        if (needsQuotes) {
            sb.append('"');
            if (dangerousStart) {
                // Neutralize formulas in spreadsheets (e.g., =CMD(), +1+1, etc.).
                sb.append('\'');
            }
            sb.append(escaped).append('"');
        } else {
            sb.append(escaped);
        }
    }
}
