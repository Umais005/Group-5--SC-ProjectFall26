import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * BARRICADE: every value coming from the GUI is treated as untrusted.
 * Existence, limit/range, combination and transaction checks (Lecture 17).
 */
final class InputBarricade {

    // ---- UC 1 ----
    static ValidationResult validateFramework(User user, String name, String type, String venue,
                                              LocalDate date, String description, List<String[]> items,
                                              BigDecimal fee) {
        // transaction checks (pre-conditions)
        if (user == null) return ValidationResult.fail("Transaction Error: Group Leader must be logged in.");
        if (!(user instanceof GroupLeader)) return ValidationResult.fail("Transaction Rejected: Only a Group Leader can establish an event framework.");
        Group g = ((GroupLeader) user).group;
        if (g == null) return ValidationResult.fail("Pre-condition Failed: A group must be established in your area first.");
        if (!g.isActive()) return ValidationResult.fail("Transaction Rejected: Your group is '" + g.groupStatus + "' and cannot create frameworks.");
        // existence tests
        String n = InputKit.clean(name);
        if (n.isEmpty()) return ValidationResult.fail("Existence Test Failed: Event name is required.");
        if (type == null || type.isEmpty()) return ValidationResult.fail("Existence Test Failed: Event type is required.");
        if (InputKit.clean(venue).isEmpty()) return ValidationResult.fail("Existence Test Failed: Event location (venue) is required.");
        if (date == null) return ValidationResult.fail("Existence Test Failed: Event date is required.");
        // limit / range tests
        if (n.length() < 3 || n.length() > 50) return ValidationResult.fail("Limit/Range Test Failed: Event name must be 3-50 characters.");
        if (InputKit.clean(venue).length() < 3 || InputKit.clean(venue).length() > 60) return ValidationResult.fail("Limit/Range Test Failed: Venue must be 3-60 characters.");
        if (!date.isAfter(LocalDate.now())) return ValidationResult.fail("Limit/Range Test Failed: Event date must be after today's date.");
        if (date.isAfter(LocalDate.now().plusYears(2))) return ValidationResult.fail("Limit/Range Test Failed: Event date cannot be more than 2 years ahead.");
        String d = InputKit.clean(description);
        if (d.length() < 10 || d.length() > 300) return ValidationResult.fail("Limit/Range Test Failed: Description must be 10-300 characters.");
        if (fee == null || fee.signum() < 0 || fee.compareTo(new BigDecimal(100000)) > 0) return ValidationResult.fail("Limit/Range Test Failed: Fee must be between Rs. 0 and Rs. 100,000.");
        // combination tests
        if (items == null || items.isEmpty()) return ValidationResult.fail("Combination Test Failed: At least one event item is required.");
        if (items.size() > 10) return ValidationResult.fail("Limit/Range Test Failed: At most 10 event items are allowed.");
        List<String> seen = new ArrayList<>();
        for (String[] it : items) {
            String in = InputKit.clean(it[0]);
            if (in.isEmpty() || in.length() > 30) return ValidationResult.fail("Limit/Range Test Failed: Each item name must be 1-30 characters.");
            if (seen.contains(in.toLowerCase())) return ValidationResult.fail("Combination Test Failed: Item '" + in + "' is listed twice.");
            seen.add(in.toLowerCase());
        }
        return ValidationResult.ok();
    }

    // ---- UC 2 ----
    static ValidationResult validateScheduleView(User user, Group group, Event event) {
        if (user == null) return ValidationResult.fail("Transaction Error: You must be logged in.");
        if (!(user instanceof GroupMember)) return ValidationResult.fail("Transaction Rejected: Only group members, team members and leaders can view event schedules.");
        GroupMember m = (GroupMember) user;
        if (group == null || !m.isInGroup(group)) return ValidationResult.fail("Pre-condition Failed: You must be in the relevant group.");
        if (event == null) return ValidationResult.fail("Existence Test Failed: Please select an event.");
        if (event.group != group) return ValidationResult.fail("Transaction Rejected: The event does not belong to the selected group.");
        if (event.hasEnded()) return ValidationResult.fail("Exception 2a: The event has ended, so its schedule is no longer available.");
        if (event.schedules.isEmpty()) return ValidationResult.fail("Pre-condition Failed: No schedule has been created for this event yet.");
        if (event.getActiveSchedules().isEmpty()) return ValidationResult.fail("Exception 3a: The schedule of this event has been deleted.");
        return ValidationResult.ok();
    }

    // ---- UC 3 ----
    static ValidationResult validateReview(User user, EventReport report, String action, String notes) {
        if (user == null) return ValidationResult.fail("Transaction Error: Site Admin must be logged in.");
        if (!(user instanceof SiteAdmin)) return ValidationResult.fail("Transaction Rejected: Only a Site Admin can validate event reports.");
        if (report == null) return ValidationResult.fail("Existence Test Failed: Please select an event report.");
        if (!report.isReviewable()) return ValidationResult.fail("Transaction Rejected: Report #" + report.eReportID + " is already '" + report.eReportStatus + "'.");
        if (!Status.HELD.equals(report.event.eventStatus)) return ValidationResult.fail("Pre-condition Failed: The event must have been held.");
        if (action == null || (!action.equals("APPROVE") && !action.equals("REQUEST_INFO"))) return ValidationResult.fail("Existence Test Failed: Choose a review decision.");
        String n = InputKit.clean(notes);
        if (n.length() > 300) return ValidationResult.fail("Limit/Range Test Failed: Notes cannot exceed 300 characters.");
        if (n.length() < 10) {
            return ValidationResult.fail("APPROVE".equals(action)
                    ? "Combination Test Failed (MSS step 3): Add relevant information (at least 10 characters) before approving."
                    : "Combination Test Failed (Alt Flow 3a): State what information is missing (at least 10 characters).");
        }
        return ValidationResult.ok();
    }

    // ---- UC 10 ----
    static ValidationResult validateRegistration(User user, Event event, String method, String cardNumber,
                                                 YearMonth expiry, String cvv, String email, String phone) {
        if (user == null) return ValidationResult.fail("Transaction Error: You must be logged in.");
        if (!(user instanceof GroupMember)) return ValidationResult.fail("Transaction Rejected: Only group members can register for events.");
        GroupMember m = (GroupMember) user;
        if (event == null) return ValidationResult.fail("Existence Test Failed: Select an event.");
        if (!m.isInGroup(event.group)) return ValidationResult.fail("Pre-condition Failed: You must be a member of the organising group.");
        if (!event.canRegister()) return ValidationResult.fail("Exception 1a: Registration for this event is closed.");
        if (event.isRegisteredBy(m)) return ValidationResult.fail("Transaction Rejected: You are already registered for this event.");
        String e = email == null ? "" : email.trim();
        String p = phone == null ? "" : phone.trim();
        if (e.isEmpty() && p.isEmpty()) return ValidationResult.fail("Combination Test Failed: Provide an email or a phone number for the confirmation.");
        if (!e.isEmpty() && (e.length() > 50 || !InputKit.EMAIL_FINAL.matcher(e).matches())) return ValidationResult.fail("Format Test Failed: Enter a valid email address.");
        if (!p.isEmpty() && !InputKit.PHONE_FINAL.matcher(p).matches()) return ValidationResult.fail("Format Test Failed: Phone must be 10-13 digits with an optional leading '+'.");
        if (event.eventFee.signum() > 0) {
            if (method == null || (!method.equals(Status.CARD) && !method.equals(Status.CASH))) return ValidationResult.fail("Existence Test Failed: Select a payment method.");
            if (Status.CARD.equals(method)) {
                if (cardNumber == null || !cardNumber.matches("[0-9]{16}")) return ValidationResult.fail("Limit/Range Test Failed: Card number must be exactly 16 digits.");
                if (!InputKit.luhnOk(cardNumber)) return ValidationResult.fail("Format Test Failed: Card number fails the check-digit test.");
                if (expiry == null || expiry.isBefore(YearMonth.now())) return ValidationResult.fail("Limit/Range Test Failed: The card has expired.");
                if (cvv == null || !cvv.matches("[0-9]{3}")) return ValidationResult.fail("Limit/Range Test Failed: CVV must be exactly 3 digits.");
            }
        }
        return ValidationResult.ok();
    }
}
