import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * ECO-DRIVE PLATFORM - domain model (Main.java)
 * Use cases: Ashar Jabbar (04072413021) - UC 1, 2, 3, 10
 *
 * RELATIONS
 *   Inheritance : User <- GroupMember <- TeamMember <- GroupLeader ; User <- SiteAdmin
 *                 Report <- MemberReport, EventReport
 *   Aggregation : Area o-- Group ;  Group o-- GroupMember ;  Group o-- Event
 *   Composition : Event *-- EventItem ;  Event *-- Schedule ;  Registration *-- Payment
 *   Association : Event -> Group, Event -> Area, Event -> GroupLeader,
 *                 Registration -> GroupMember / Event, EventReport -> Event / GroupLeader
 *
 * PATTERNS
 *   Observer : Event (subject) notifies GroupMember (observer)
 *   Builder  : EventBuilder builds an Event step by step
 *   Adapter  : PaymentGatewayAdapter / NotificationAdapter wrap third-party services
 *   Facade   : DBFacade (data access) and EcoDriveFacade (single entry point for the UI)
 */

// ============================================================================
// CONSTANTS AND ID GENERATION
// ============================================================================
final class Status {
    static final String ACTIVE = "Active", LOCKED = "Locked", RETIRED = "Retired";
    static final String FRAMEWORK = "Framework", SCHEDULED = "Scheduled", HELD = "Held", VALIDATED = "Validated";
    static final String SUBMITTED = "Submitted", RETURNED = "Returned";
    static final String PAID = "Paid", PENDING = "Pending", FAILED = "Failed";
    static final String CARD = "Card", CASH = "Cash on arrival";
    private Status() { }
}

final class IdSequence {
    private static int event = 500130, item = 9000, payment = 8000, registration = 4500, report = 7000;
    static Integer nextEventID() { return event++; }
    static Integer nextReportID() { return report++; }
    static Integer nextItemID() { return item++; }
    static Integer nextPaymentID() { return payment++; }
    static String nextRegistrationNo() { return "REG-" + String.format("%06d", registration++); }
    private IdSequence() { }
}

// ============================================================================
// OBSERVER PATTERN - observer interface (subject is Event)
// ============================================================================
interface EventObserver {
    void update(Event event, String message);
}

// ============================================================================
// USER HIERARCHY (inheritance)
// ============================================================================
class User {
    public Integer userID;
    public String name, phoneNo, role;
    private String email, password;

    User(Integer userID, String name, String email,
         String phoneNo, String role, String password) {
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.phoneNo = phoneNo;
        this.role = role;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public boolean hasEmail() {
        return email != null && !email.trim().isEmpty();
    }

    public void updateContact(String email, String phoneNo) {
        this.email = email == null ? "" : email;
        this.phoneNo = phoneNo == null ? "" : phoneNo;
    }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }

    public static boolean validateInputDetails(Component parentFrame, String inputEmail, String inputPassword, User registeredUser) {

        // Remove accidental spaces at the start/end of the email
        inputEmail = inputEmail.trim();

        // Condition 1: Check if either email OR password is empty
        if (inputEmail.isEmpty() || inputPassword.isEmpty()) {
            JOptionPane.showMessageDialog(
                parentFrame,
                "There is a missing input. Please enter both email and password.",
                "Missing Input",
                JOptionPane.WARNING_MESSAGE // Shows a yellow warning triangle icon
            );
            return false;
        }

        // Condition 2: Check if input matches the User object's email and password
        else if (inputEmail.equals(registeredUser.getEmail()) && inputPassword.equals(registeredUser.getPassword())) {
            JOptionPane.showMessageDialog(
                parentFrame,
                "Login Successful! Welcome to Eco-Drive.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE // Shows a blue info icon
            );
            return true;
        }

        // Condition 3: Email or password does not match
        else {
            JOptionPane.showMessageDialog(
                parentFrame,
                "User email or password is incorrect.",
                "Login Failed",
                JOptionPane.ERROR_MESSAGE // Shows a red error 'X' icon
            );
            return false;
        }
    }
}

/** Group Member: belongs to at most one Group and observes the events of that group. */
class GroupMember extends User implements EventObserver {
    Group group;                                            // association: member -> group (0..1)
    private final List<String> inbox = new ArrayList<>();   // alerts received through the Observer pattern

    GroupMember(Integer userID, String name, String email, String phoneNo, String password) {
        super(userID, name, email, phoneNo, "Group Member", password);
    }

    protected GroupMember(Integer userID, String name, String email, String phoneNo, String role, String password) {
        super(userID, name, email, phoneNo, role, password);
    }

    boolean isInGroup(Group g) {
        return group != null && g != null && group.groupID.equals(g.groupID);
    }

    void joinGroup(Group g) {
        g.addMember(this);
    }

    @Override
    public void update(Event event, String message) {
        inbox.add("[" + event.eventName + "] " + message);
    }

    List<String> getInbox() {
        return Collections.unmodifiableList(inbox);
    }
}

class TeamMember extends GroupMember {
    String teamName;
    final List<MemberReport> reports = new ArrayList<>();   // association: team member -> member reports

    TeamMember(Integer userID, String name, String email, String phoneNo, String password, String teamName) {
        super(userID, name, email, phoneNo, "Team Member", password);
        this.teamName = teamName;
    }

    protected TeamMember(Integer userID, String name, String email, String phoneNo, String role,
                         String password, String teamName) {
        super(userID, name, email, phoneNo, role, password);
        this.teamName = teamName;
    }

    MemberReport submitMemberReport(MemberReport report) {
        report.author = this;
        reports.add(report);
        return report;
    }
}

class GroupLeader extends TeamMember {

    GroupLeader(Integer userID, String name, String email, String phoneNo, String password) {
        super(userID, name, email, phoneNo, "Group Leader", password, "Leadership");
    }

    /** UC 1: establish an event framework using the Builder. */
    Event establishFramework(EventBuilder builder) {
        assert group != null : "Leader must belong to an established group";
        Event e = builder.group(group).leader(this).build();
        group.addEvent(e);
        return e;
    }

    /** Leader submits the report of a held event (input of UC 3). */
    EventReport submitEventReport(Event event, String content) {
        EventReport r = new EventReport(IdSequence.nextReportID(), LocalDate.now(), Status.SUBMITTED, event, this, content);
        event.report = r;
        return r;
    }
}

class SiteAdmin extends User {

    SiteAdmin(Integer userID, String name, String email, String phoneNo, String password) {
        super(userID, name, email, phoneNo, "Site Admin", password);
    }

    /** UC 3: approve a report (validated event is used for ranking). */
    void validateEventReport(EventReport report, String notes) {
        report.validate(notes);
    }

    /** UC 3 alternative flow: ask the leader for additional information. */
    void requestAdditionalInfo(EventReport report, String notes) {
        report.returnForInfo(notes);
    }
}

// ============================================================================
// GROUP AND AREA (aggregation)
// ============================================================================
class Group {
    Integer groupID, groupRank;
    String groupName, groupStatus;
    Area area;                                              // association: group -> area
    GroupLeader leader;                                     // association: group -> leader
    final List<GroupMember> members = new ArrayList<>();    // aggregation: group o-- members
    final List<Event> events = new ArrayList<>();           // aggregation: group o-- events

    Group(Integer groupID, String groupName,
          String groupStatus, Integer groupRank) {
        this.groupID = groupID;
        this.groupName = groupName;
        this.groupStatus = groupStatus;
        this.groupRank = groupRank;
    }

    boolean isActive() {
        return Status.ACTIVE.equals(groupStatus);
    }

    /** Adding a member also subscribes it (Observer) to every event of the group. */
    void addMember(GroupMember m) {
        if (!members.contains(m)) {
            members.add(m);
            m.group = this;
            for (Event e : events) {
                e.attach(m);
            }
        }
    }

    void removeMember(GroupMember m) {
        members.remove(m);
        m.group = null;
        for (Event e : events) {
            e.detach(m);
        }
    }

    /** Adding an event subscribes all current members and alerts them. */
    void addEvent(Event e) {
        events.add(e);
        e.group = this;
        for (GroupMember m : members) {
            e.attach(m);
        }
        e.notifyObservers("New event framework established for " + e.eventDate + " at " + e.venue + ".");
    }

    int getValidatedEventCount() {
        int n = 0;
        for (Event e : events) {
            if (Status.VALIDATED.equals(e.eventStatus)) {
                n++;
            }
        }
        return n;
    }

    int getScore() {
        return getValidatedEventCount() * 50 + members.size() * 5;
    }

    @Override
    public String toString() {
        return groupID + " - " + groupName;
    }
}

class Area {
    Integer areaID;
    String areaName, areaLocation;
    final List<Group> groups = new ArrayList<>();           // aggregation: area o-- groups

    Area(Integer areaID, String areaName, String areaLocation) {
        this.areaID = areaID;
        this.areaName = areaName;
        this.areaLocation = areaLocation;
    }

    void addGroup(Group g) {
        groups.add(g);
        g.area = this;
    }

    @Override
    public String toString() {
        return areaName;
    }
}

// ============================================================================
// EVENT (Observer subject) AND ITS COMPOSED PARTS
// ============================================================================
class EventItem {
    Integer itemID;
    String itemName, itemDescription;

    EventItem(Integer itemID, String itemName,
              String itemDescription) {
        this.itemID = itemID;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
    }
}

class Schedule {
    LocalDate scheduleDate;
    LocalTime scheduleTime;
    String activity, description;
    boolean deleted;

    Schedule(LocalDate scheduleDate, LocalTime scheduleTime, String activity, String description) {
        this.scheduleDate = scheduleDate;
        this.scheduleTime = scheduleTime;
        this.activity = activity;
        this.description = description;
    }
}

class Event {
    Integer eventID;
    String eventType, eventDescription, eventStatus;
    LocalDate eventDate;
    // refinements used by UC 1, 2, 10
    String eventName, venue;
    BigDecimal eventFee = BigDecimal.ZERO;
    boolean registrationOpen = true;
    // associations
    Group group;
    GroupLeader leader;
    Area location;
    EventReport report;
    // compositions: parts die with the event
    final List<EventItem> items = new ArrayList<>();
    final List<Schedule> schedules = new ArrayList<>();
    final List<Registration> registrations = new ArrayList<>();
    // Observer pattern: subject keeps its observers
    private final List<EventObserver> observers = new ArrayList<>();

    Event(Integer eventID, String eventType, LocalDate eventDate,
          String eventDescription, String eventStatus) {
        this.eventID = eventID;
        this.eventType = eventType;
        this.eventName = eventType;
        this.eventDate = eventDate;
        this.eventDescription = eventDescription;
        this.eventStatus = eventStatus;
    }

    // ---- Observer pattern ----
    void attach(EventObserver o) {
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }

    void detach(EventObserver o) {
        observers.remove(o);
    }

    void notifyObservers(String message) {
        for (EventObserver o : observers) {
            o.update(this, message);
        }
    }

    // ---- behaviour ----
    void addItem(EventItem item) {
        items.add(item);
    }

    void addSchedule(Schedule s) {
        schedules.add(s);
        if (Status.FRAMEWORK.equals(eventStatus)) {
            eventStatus = Status.SCHEDULED;
        }
        notifyObservers("Schedule added: " + s.activity + " on " + s.scheduleDate + " at " + s.scheduleTime + ".");
    }

    void changeStatus(String newStatus) {
        eventStatus = newStatus;
        notifyObservers("Event status changed to " + newStatus + ".");
    }

    boolean hasEnded() {
        return Status.HELD.equals(eventStatus) || Status.VALIDATED.equals(eventStatus)
                || eventDate.isBefore(LocalDate.now());
    }

    boolean canRegister() {
        return registrationOpen && !hasEnded();
    }

    List<Schedule> getActiveSchedules() {
        List<Schedule> active = new ArrayList<>();
        for (Schedule s : schedules) {
            if (!s.deleted) {
                active.add(s);
            }
        }
        return active;
    }

    boolean isRegisteredBy(GroupMember m) {
        for (Registration r : registrations) {
            if (r.member == m) {
                return true;
            }
        }
        return false;
    }

    void addRegistration(Registration r) {
        registrations.add(r);
    }

    @Override
    public String toString() {
        return eventID + " - " + eventName + " [" + eventStatus + ", " + eventDate + "]";
    }
}

// ============================================================================
// BUILDER PATTERN - builds an Event step by step (UC 1)
// ============================================================================
class EventBuilder {
    private String eventName, eventType, eventDescription, venue;
    private LocalDate eventDate;
    private BigDecimal fee = BigDecimal.ZERO;
    private Area location;
    private Group group;
    private GroupLeader leader;
    private final List<String[]> itemData = new ArrayList<>();

    EventBuilder name(String v) { this.eventName = v; return this; }
    EventBuilder type(String v) { this.eventType = v; return this; }
    EventBuilder description(String v) { this.eventDescription = v; return this; }
    EventBuilder venue(String v) { this.venue = v; return this; }
    EventBuilder date(LocalDate v) { this.eventDate = v; return this; }
    EventBuilder fee(BigDecimal v) { this.fee = v; return this; }
    EventBuilder location(Area v) { this.location = v; return this; }
    EventBuilder group(Group v) { this.group = v; return this; }
    EventBuilder leader(GroupLeader v) { this.leader = v; return this; }
    EventBuilder addItem(String itemName, String itemDescription) {
        itemData.add(new String[]{itemName, itemDescription});
        return this;
    }

    Event build() {
        if (eventName == null || eventType == null || eventDate == null || venue == null
                || location == null || itemData.isEmpty()) {
            throw new IllegalStateException("Event framework is incomplete: name, type, date, venue, location and items are required.");
        }
        Event e = new Event(IdSequence.nextEventID(), eventType, eventDate, eventDescription, Status.FRAMEWORK);
        e.eventName = eventName;
        e.venue = venue;
        e.eventFee = fee;
        e.location = location;
        e.group = group;
        e.leader = leader;
        for (String[] d : itemData) {
            e.addItem(new EventItem(IdSequence.nextItemID(), d[0], d[1]));
        }
        return e;
    }
}

// ============================================================================
// REGISTRATION AND PAYMENT (composition)
// ============================================================================
class Payment {
    Integer paymentID;
    BigDecimal paymentAmount;
    LocalDate paymentDate;
    String paymentMethod;
    String status = Status.PENDING;
    String receiptNo;

    Payment(Integer paymentID, BigDecimal paymentAmount,
            LocalDate paymentDate, String paymentMethod) {
        this.paymentID = paymentID;
        this.paymentAmount = paymentAmount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
    }
}

class Registration {
    String registrationNo;
    LocalDate registrationDate;
    String status;
    GroupMember member;                 // association
    Event event;                        // association
    Payment payment;                    // composition: payment belongs to the registration
    String confirmationSentVia = "";

    Registration(String registrationNo, LocalDate registrationDate, String status,
                 GroupMember member, Event event, Payment payment) {
        this.registrationNo = registrationNo;
        this.registrationDate = registrationDate;
        this.status = status;
        this.member = member;
        this.event = event;
        this.payment = payment;
    }

    String getReceipt() {
        return "RECEIPT " + payment.receiptNo + "\n"
                + "Registration No : " + registrationNo + "\n"
                + "Member          : " + member.name + "\n"
                + "Event           : " + event.eventName + " (" + event.eventDate + ")\n"
                + "Amount          : Rs. " + payment.paymentAmount.toPlainString() + "\n"
                + "Payment method  : " + payment.paymentMethod + "  [" + payment.status + "]\n"
                + "Date            : " + registrationDate;
    }
}

// ============================================================================
// REPORTS (inheritance)
// ============================================================================
abstract class Report {
    abstract Integer getID();
    abstract LocalDate getDate();
    abstract String getStatus();
}

class MemberReport extends Report {
    Integer mReportID;
    LocalDate mReportDate;
    String mReportType, mReportStatus;
    TeamMember author;                  // association

    MemberReport(Integer mReportID, LocalDate mReportDate,
                 String mReportType, String mReportStatus) {
        this.mReportID = mReportID;
        this.mReportDate = mReportDate;
        this.mReportType = mReportType;
        this.mReportStatus = mReportStatus;
    }

    @Override Integer getID() { return mReportID; }
    @Override LocalDate getDate() { return mReportDate; }
    @Override String getStatus() { return mReportStatus; }
}

class EventReport extends Report {
    Integer eReportID;
    LocalDate eReportDate;
    String eReportStatus;
    Event event;                        // association
    GroupLeader author;                 // association
    String content = "";
    String feedback = "";

    EventReport(Integer eReportID, LocalDate eReportDate,
                String eReportStatus) {
        this.eReportID = eReportID;
        this.eReportDate = eReportDate;
        this.eReportStatus = eReportStatus;
    }

    EventReport(Integer eReportID, LocalDate eReportDate, String eReportStatus,
                Event event, GroupLeader author, String content) {
        this(eReportID, eReportDate, eReportStatus);
        this.event = event;
        this.author = author;
        this.content = content;
    }

    boolean isReviewable() {
        return Status.SUBMITTED.equals(eReportStatus);
    }

    /** Admin approves: the event becomes Validated and the leader is told. */
    void validate(String notes) {
        eReportStatus = Status.VALIDATED;
        feedback = notes;
        event.changeStatus(Status.VALIDATED);
        author.update(event, "Your event report was validated. Admin notes: " + notes);
    }

    /** Admin asks for more information: status Returned and the leader is told. */
    void returnForInfo(String notes) {
        eReportStatus = Status.RETURNED;
        feedback = notes;
        author.update(event, "Additional information requested: " + notes);
    }

    @Override Integer getID() { return eReportID; }
    @Override LocalDate getDate() { return eReportDate; }
    @Override String getStatus() { return eReportStatus; }
}

class Request {
    Integer requestID;
    LocalDate requestDate, approvalDate;
    String requestStatus;

    Request(Integer requestID, LocalDate requestDate,
            LocalDate approvalDate, String requestStatus) {
        this.requestID = requestID;
        this.requestDate = requestDate;
        this.approvalDate = approvalDate;
        this.requestStatus = requestStatus;
    }
}

class Certificate {
    Integer certificateID;
    String certificateType;
    LocalDate issueDate;

    Certificate(Integer certificateID, String certificateType,
                LocalDate issueDate) {
        this.certificateID = certificateID;
        this.certificateType = certificateType;
        this.issueDate = issueDate;
    }
}

// ============================================================================
// ADAPTER PATTERN - third-party services wrapped behind our own interfaces
// ============================================================================
class CardDetails {
    final String number, expiry, cvv;
    CardDetails(String number, String expiry, String cvv) {
        this.number = number;
        this.expiry = expiry;
        this.cvv = cvv;
    }
}

class PaymentResult {
    final boolean success;
    final String message;
    PaymentResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}

/** Target interface the domain expects (IPaymentGatewayAdapter in the diagrams). */
interface PaymentGatewayAdapter {
    PaymentResult charge(BigDecimal amount, CardDetails card);
}

/** Third-party class that we cannot change: different method names and units. */
class ThirdPartyPaymentGateway {
    static boolean simulateInsufficientFunds = false;       // used by the demo screens (Exception 3a)

    int makeCharge(long amountInPaisa, String cardToken) {
        if (cardToken == null || cardToken.length() < 8) return 400;
        if (simulateInsufficientFunds) return 402;
        return 200;
    }
}

/** Adapter: translates our call into the gateway's call and its codes into a PaymentResult. */
class PaymentGatewayAdapterImpl implements PaymentGatewayAdapter {
    private final ThirdPartyPaymentGateway gateway = new ThirdPartyPaymentGateway();

    @Override
    public PaymentResult charge(BigDecimal amount, CardDetails card) {
        long paisa = amount.multiply(new BigDecimal(100)).longValue();
        String token = "tok_" + card.number;
        int code = gateway.makeCharge(paisa, token);
        switch (code) {
            case 200: return new PaymentResult(true, "Payment accepted.");
            case 402: return new PaymentResult(false, "Payment failed: insufficient funds.");
            default:  return new PaymentResult(false, "Payment failed: card was declined.");
        }
    }
}

interface NotificationAdapter {
    /** Sends by email, or by SMS when there is no email; returns where it was sent. */
    String send(String email, String phone, String message);
}

class ThirdPartyMessenger {
    final List<String> outbox = new ArrayList<>();
    void sendEmail(String to, String subject, String body) { outbox.add("EMAIL to " + to + ": " + subject); }
    void sendSms(String number, String text) { outbox.add("SMS to " + number + ": " + text); }
}

class EmailSmsAdapter implements NotificationAdapter {
    private final ThirdPartyMessenger messenger = new ThirdPartyMessenger();

    @Override
    public String send(String email, String phone, String message) {
        if (email != null && !email.trim().isEmpty()) {
            messenger.sendEmail(email, "Eco-Drive confirmation", message);
            return "email " + email;
        }
        messenger.sendSms(phone, message);
        return "SMS " + phone;
    }
}

class ServicesFactory {
    private static final PaymentGatewayAdapter PAYMENT = new PaymentGatewayAdapterImpl();
    private static final NotificationAdapter NOTIFIER = new EmailSmsAdapter();
    static PaymentGatewayAdapter getPaymentAdapter() { return PAYMENT; }
    static NotificationAdapter getNotificationAdapter() { return NOTIFIER; }
    private ServicesFactory() { }
}

// ============================================================================
// FACADE PATTERN 1 - DBFacade: one class hides data access (in-memory here)
// ============================================================================
class DBFacade {
    final List<Area> areas = new ArrayList<>();
    final List<Group> groups = new ArrayList<>();
    final List<User> users = new ArrayList<>();
    final List<Event> events = new ArrayList<>();
    // demo users used by the screens
    GroupLeader leader;
    GroupMember member;
    TeamMember teamMember;
    SiteAdmin admin;

    DBFacade() {
        seed();
    }

    List<Event> findEventsByGroup(Group g) {
        List<Event> result = new ArrayList<>();
        for (Event e : events) {
            if (e.group == g) {
                result.add(e);
            }
        }
        return result;
    }

    List<EventReport> findAllEventReports() {
        List<EventReport> result = new ArrayList<>();
        for (Event e : events) {
            if (e.report != null) {
                result.add(e.report);
            }
        }
        return result;
    }

    void save(Event e) {
        if (!events.contains(e)) {
            events.add(e);
        }
    }

    private Event addEvent(Group g, String name, String type, int dayOffset, String status,
                           int fee, boolean open, String venue, String... itemNames) {
        Event e = new Event(IdSequence.nextEventID(), type, LocalDate.now().plusDays(dayOffset),
                name + " organised by " + g.groupName + ".", status);
        e.eventName = name;
        e.venue = venue;
        e.eventFee = new BigDecimal(fee);
        e.registrationOpen = open;
        e.location = g.area;
        e.leader = g.leader;
        for (String n : itemNames) {
            e.addItem(new EventItem(IdSequence.nextItemID(), n, n + " to be recorded"));
        }
        g.addEvent(e);
        events.add(e);
        return e;
    }

    private void seed() {
        Area f10 = new Area(1012, "Sector F-10", "Sector F-10, Islamabad");
        Area g11 = new Area(1014, "Sector G-11", "Sector G-11, Islamabad");
        Area e7 = new Area(1015, "Sector E-7", "Sector E-7, Islamabad");
        areas.add(f10); areas.add(g11); areas.add(e7);

        Group green = new Group(30211, "Green Margalla", Status.ACTIVE, 0);
        Group rawal = new Group(30212, "Clean Rawal", Status.ACTIVE, 0);
        f10.addGroup(green); g11.addGroup(rawal);
        groups.add(green); groups.add(rawal);

        leader = new GroupLeader(210045, "Ali Khan", "ali.khan@example.com", "+923001234567", "pass1");
        member = new GroupMember(210046, "Sara Ahmed", "sara.ahmed@example.com", "+923011234567", "pass2");
        teamMember = new TeamMember(210088, "Zainab Tariq", "zainab.tariq@example.com", "+923021234567", "pass3",
                "Logistics Team");
        admin = new SiteAdmin(100001, "Hassan Raza", "hassan.raza@example.com", "+923031234567", "admin1");
        GroupLeader rawalLeader = new GroupLeader(210050, "Bilal Raza", "bilal.raza@example.com", "+923041234567", "pass4");
        users.add(leader); users.add(member); users.add(teamMember); users.add(admin); users.add(rawalLeader);

        green.leader = leader;
        rawal.leader = rawalLeader;
        leader.joinGroup(green);
        member.joinGroup(green);
        teamMember.joinGroup(green);
        rawalLeader.joinGroup(rawal);

        // events of Green Margalla (members are subscribed automatically)
        Event spring = addEvent(green, "Spring Plantation Drive", "Tree Plantation", 20, Status.SCHEDULED, 500, true,
                "Margalla Trail 3 entrance", "Type of tree", "Total trees");
        spring.addSchedule(new Schedule(spring.eventDate, LocalTime.of(9, 0), "Registration and briefing", "Meet at the main gate"));
        spring.addSchedule(new Schedule(spring.eventDate, LocalTime.of(10, 30), "Planting session", "Plant saplings in assigned rows"));

        Event cleanup = addEvent(green, "Margalla Trail Cleanup", "Cleanup Drive", -10, Status.HELD, 0, false,
                "Margalla Trail 3", "Bags collected");
        cleanup.addSchedule(new Schedule(cleanup.eventDate, LocalTime.of(8, 0), "Trail cleanup", "Collect litter along the trail"));
        leader.submitEventReport(cleanup, "Cleanup completed with 40 volunteers; 120 bags of waste collected along trail 3.");

        Event sapling = addEvent(green, "Park Sapling Care", "Tree Plantation", 30, Status.SCHEDULED, 200, false,
                "F-10 Markaz Park", "Saplings watered");
        Schedule removed = new Schedule(sapling.eventDate, LocalTime.of(16, 0), "Watering round", "Water new saplings");
        removed.deleted = true;
        sapling.schedules.add(removed);

        addEvent(green, "Neighborhood Recycling Drive", "Recycling Drive", 40, Status.FRAMEWORK, 0, true,
                "F-10 Community Centre", "Recyclables collected");

        Event lake = addEvent(green, "Rawal Lake Cleanup", "Cleanup Drive", -25, Status.HELD, 0, false,
                "Rawal Lake viewpoint", "Bags collected", "Volunteers present");
        lake.addSchedule(new Schedule(lake.eventDate, LocalTime.of(7, 30), "Lakeside cleanup", "Clean the viewpoint area"));
        leader.submitEventReport(lake, "Lake cleanup finished; 25 volunteers took part and the viewpoint area was cleared.");

        Event winter = addEvent(green, "Winter Cleanup", "Cleanup Drive", -50, Status.VALIDATED, 0, false,
                "F-10 Markaz Park", "Bags collected");
        EventReport done = leader.submitEventReport(winter, "Winter cleanup finished with 18 volunteers.");
        done.eReportStatus = Status.VALIDATED;
        done.feedback = "Validated earlier.";

        // an event of the other group, so that ranking has something to compare
        Event rawalEvent = addEvent(rawal, "Rawal Tree Drive", "Tree Plantation", -40, Status.VALIDATED, 0, false,
                "G-11 Park", "Total trees");
        rawalEvent.registrationOpen = false;
    }
}

// ============================================================================
// FACADE PATTERN 2 - EcoDriveFacade: single entry point used by the screens
// ============================================================================
class PaymentFailedException extends RuntimeException {
    PaymentFailedException(String message) {
        super(message);
    }
}

class EcoDriveFacade {
    private final DBFacade db;
    private final PaymentGatewayAdapter payment = ServicesFactory.getPaymentAdapter();
    private final NotificationAdapter notifier = ServicesFactory.getNotificationAdapter();

    EcoDriveFacade(DBFacade db) {
        this.db = db;
    }

    DBFacade db() {
        return db;
    }

    // ---------------- UC 1 ----------------
    boolean frameworkExists(Group g, String name, String type, LocalDate date, String venue) {
        for (Event e : db.findEventsByGroup(g)) {
            boolean sameName = e.eventName.equalsIgnoreCase(name);
            boolean sameDetails = e.eventType.equals(type) && e.eventDate.equals(date)
                    && e.venue.equalsIgnoreCase(venue);
            if (sameName || sameDetails) {
                return true;
            }
        }
        return false;
    }

    Event establishFramework(GroupLeader leader, EventBuilder builder) {
        assert leader != null && leader.group != null && leader.group.isActive() : "Leader needs an active group";
        builder.location(leader.group.area);
        Event e = leader.establishFramework(builder);   // builds, attaches observers, alerts members
        db.save(e);
        return e;
    }

    // ---------------- UC 2 ----------------
    List<Event> getEventsForGroup(Group g) {
        return db.findEventsByGroup(g);
    }

    List<Schedule> viewSchedule(GroupMember viewer, Event event) {
        assert viewer.isInGroup(event.group) : "Viewer must be in the event's group";
        assert !event.hasEnded() : "Event must not have ended";
        return event.getActiveSchedules();
    }

    // ---------------- UC 3 ----------------
    List<EventReport> getEventReports() {
        return db.findAllEventReports();
    }

    String validateEventReport(SiteAdmin admin, EventReport report, String notes) {
        assert report.isReviewable() : "Only submitted reports can be reviewed";
        admin.validateEventReport(report, notes);
        computeRanking();
        return "Group '" + report.event.group.groupName + "' is now ranked #" + report.event.group.groupRank + ".";
    }

    void requestAdditionalInfo(SiteAdmin admin, EventReport report, String notes) {
        assert report.isReviewable() : "Only submitted reports can be reviewed";
        admin.requestAdditionalInfo(report, notes);
    }

    List<Group> computeRanking() {
        List<Group> ranked = new ArrayList<>(db.groups);
        ranked.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        int rank = 1;
        for (Group g : ranked) {
            g.groupRank = rank++;
        }
        return ranked;
    }

    // ---------------- UC 10 ----------------
    Registration registerForEvent(GroupMember member, Event event, String method,
                                  CardDetails card, String email, String phone) {
        assert member.isInGroup(event.group) : "Member must belong to the event's group";
        assert event.canRegister() : "Registration must be open";
        member.updateContact(email, phone);

        Payment p = new Payment(IdSequence.nextPaymentID(), event.eventFee, LocalDate.now(), method);
        if (event.eventFee.signum() == 0) {
            p.paymentMethod = "None (free event)";
            p.status = Status.PAID;
        } else if (Status.CARD.equals(method)) {
            PaymentResult result = payment.charge(event.eventFee, card);
            if (!result.success) {
                p.status = Status.FAILED;
                throw new PaymentFailedException(result.message);
            }
            p.status = Status.PAID;
        } else {
            p.status = Status.PENDING;                  // cash on arrival
        }
        p.receiptNo = "RCPT-" + p.paymentID;

        Registration reg = new Registration(IdSequence.nextRegistrationNo(), LocalDate.now(),
                "Confirmed", member, event, p);
        event.addRegistration(reg);
        String text = "Registered for " + event.eventName + " on " + event.eventDate
                + ". Registration no. " + reg.registrationNo + ".";
        reg.confirmationSentVia = notifier.send(member.getEmail(), member.phoneNo, text);
        return reg;
    }
}

// ============================================================================
// ENTRY POINT
// ============================================================================
public class Main {
    public static void main(String[] args) {
        // small console demonstration of the patterns, then the four separate screens
        DBFacade db = new DBFacade();
        EcoDriveFacade facade = new EcoDriveFacade(db);

        EventBuilder builder = new EventBuilder()                       // Builder
                .name("Demo Plantation").type("Tree Plantation")
                .venue("F-10 Markaz Park").date(LocalDate.now().plusDays(7))
                .description("Demo framework").addItem("Type of tree", "Species planted");
        Event e = facade.establishFramework(db.leader, builder);        // Facade
        System.out.println("Created: " + e);
        for (String s : db.member.getInbox()) {                         // Observer
            System.out.println("Member alert: " + s);
        }

        // each use-case screen is an independent window with its own data
        EstablishFrameworkScreen.main(args);
        ViewScheduleScreen.main(args);
        ReviewEventsScreen.main(args);
        EventRegistrationScreen.main(args);
    }
}
