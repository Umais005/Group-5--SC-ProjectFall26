import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/**
 * CS 322 Software Construction - Eco-Drive Platform
 * Use Cases Assigned to: Muhammad Umais (04072413005)
 * Implements Lecture 17 Defensive Programming & Input Design Rules
 */
public class EcoDriveUmaisScreens extends JFrame {

    // =========================================================================
    // 1. DOMAIN MODELS & SESSION CONTEXT (Simulated System State)
    // =========================================================================
    static class UserSession {
        int userId;
        String userName;
        String userEmail;
        String userPhone;
        String userRole; // "User", "Group Member", "Team Member", "Group Leader"
        int areaId;
        String areaName;
        Integer groupId; // null if not yet in a group

        public UserSession(int userId, String userName, String userEmail, String userPhone,
                           String userRole, int areaId, String areaName, Integer groupId) {
            this.userId = userId;
            this.userName = userName;
            this.userEmail = userEmail;
            this.userPhone = userPhone;
            this.userRole = userRole;
            this.areaId = areaId;
            this.areaName = areaName;
            this.groupId = groupId;
        }
    }

    static class GroupEntity {
        int groupId;
        String groupName;
        int areaId;
        String areaName;
        String status; // Active, Locked, Retired
        int eventsOrganized;
        int memberCount;

        public GroupEntity(int groupId, String groupName, int areaId, String areaName,
                           String status, int eventsOrganized, int memberCount) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.areaId = areaId;
            this.areaName = areaName;
            this.status = status;
            this.eventsOrganized = eventsOrganized;
            this.memberCount = memberCount;
        }

        // System-calculated score for ComputeRanking()
        public int getCalculatedScore() {
            return (eventsOrganized * 50) + (memberCount * 5);
        }

        @Override
        public String toString() {
            return groupId + " - " + groupName + " (" + areaName + ")";
        }
    }

    static class EventEntity {
        int eventId;
        String eventTitle;
        String eventType;
        String eventStatus; // Framework, Scheduled, Held, Validated
        int groupId;

        public EventEntity(int eventId, String eventTitle, String eventType, String eventStatus, int groupId) {
            this.eventId = eventId;
            this.eventTitle = eventTitle;
            this.eventType = eventType;
            this.eventStatus = eventStatus;
            this.groupId = groupId;
        }

        @Override
        public String toString() {
            return eventId + " - " + eventTitle + " [" + eventType + "]";
        }
    }

    static class MemberReportEntity {
        int reportId;
        int eventId;
        String eventTitle;
        int authorUserId;
        String authorName;
        String teamRole;
        String reportDate; // DD/MM/YYYY
        String content;    // Max 1000 chars
        String filePath;
        String status;     // Submitted, Reviewed, Approved, Returned
        String feedback;   // Max 300 chars

        public MemberReportEntity(int reportId, int eventId, String eventTitle, int authorUserId,
                                  String authorName, String teamRole, String reportDate,
                                  String content, String filePath, String status) {
            this.reportId = reportId;
            this.eventId = eventId;
            this.eventTitle = eventTitle;
            this.authorUserId = authorUserId;
            this.authorName = authorName;
            this.teamRole = teamRole;
            this.reportDate = reportDate;
            this.content = content;
            this.filePath = filePath;
            this.status = status;
            this.feedback = "";
        }

        @Override
        public String toString() {
            return "Report #" + reportId + " | Event: " + eventTitle + " | By: " + authorName + " [" + status + "]";
        }
    }

    // =========================================================================
    // 2. BARRICADE LAYER (Slide 12-16: Sterilizes Dirty UI Data at the Boundary)
    // =========================================================================
    static class ValidationResult {
        final boolean valid;
        final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult fail(String msg) {
            return new ValidationResult(false, msg);
        }
    }

    /**
     * BARRICADE CLASS: Assumes all data coming from the GUI is dirty and untrusted.
     * Enforces Existence, Limit/Range, Combination, and Transaction Checks per SRS Data Dictionary.
     */
    static class InputBarricade {
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        private static final Pattern PHONE_PATTERN =
                Pattern.compile("^\\+?[0-9]{10,13}$");
        private static final Set<String> ALLOWED_REPORT_EXTENSIONS =
                new HashSet<>(Arrays.asList("pdf", "docx", "txt"));

        // Barricade Validation for UC 4: View Leaderboard
        public static ValidationResult validateLeaderboardAccess(UserSession session, String selectedArea) {
            // Transaction Check: Pre-conditions from SRS UC 4
            if (session == null || session.userId <= 0) {
                return ValidationResult.fail("Transaction Error: User must be logged in to view the leaderboard.");
            }
            if (session.groupId == null) {
                return ValidationResult.fail("Pre-condition Failed: You must already be part of a group to view the leaderboard.");
            }
            // Existence Test
            if (selectedArea == null || selectedArea.trim().isEmpty()) {
                return ValidationResult.fail("Existence Test Failed: Please select a valid Neighborhood/Area filter.");
            }
            return ValidationResult.ok();
        }

        // Barricade Validation for UC 5: Join Group
        public static ValidationResult validateJoinGroupInput(
                UserSession session, GroupEntity selectedGroup, String fullName,
                String email, String phone, String chosenRole, String chosenSubTeam) {

            // 1. Transaction Check (Pre-conditions & Group state)
            if (session.groupId != null) {
                return ValidationResult.fail("Transaction Rejected: You have already joined a group (Group ID: "
                        + session.groupId + "). Users cannot join multiple groups simultaneously.");
            }
            if (selectedGroup == null) {
                return ValidationResult.fail("Existence Test Failed: Please select a group to join.");
            }
            if (!"Active".equalsIgnoreCase(selectedGroup.status)) {
                return ValidationResult.fail("Transaction Rejected: Selected group is currently '"
                        + selectedGroup.status + "' and cannot accept new members.");
            }

            // 2. Existence & Limit/Range Test on User Name (SRS Data Dictionary: String, Length 50, Required)
            String cleanName = (fullName == null) ? "" : fullName.trim();
            if (cleanName.isEmpty()) {
                return ValidationResult.fail("Existence Test Failed: Full Name is required.");
            }
            if (cleanName.length() < 2 || cleanName.length() > 50) {
                return ValidationResult.fail("Limit/Range Test Failed: Full Name must be between 2 and 50 characters (Current: "
                        + cleanName.length() + ").");
            }

            // 3. Combination Test + Limit/Range Test on Email (Length 50) & Phone (Length 13)
            // SRS Data Dictionary: Email optional if phone given; Phone required if no email.
            String cleanEmail = (email == null) ? "" : email.trim();
            String cleanPhone = (phone == null) ? "" : phone.trim();

            if (cleanEmail.isEmpty() && cleanPhone.isEmpty()) {
                return ValidationResult.fail("Combination Test Failed: You must provide at least an Email Address OR a Phone Number for confirmations.");
            }
            if (!cleanEmail.isEmpty()) {
                if (cleanEmail.length() > 50) {
                    return ValidationResult.fail("Limit/Range Test Failed: Email cannot exceed 50 characters.");
                }
                if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
                    return ValidationResult.fail("Format Test Failed: Please enter a valid Email Address (e.g., ali.khan@example.com).");
                }
            }
            if (!cleanPhone.isEmpty()) {
                if (cleanPhone.length() > 13) {
                    return ValidationResult.fail("Limit/Range Test Failed: Phone number cannot exceed 13 characters (e.g., +923001234567).");
                }
                if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
                    return ValidationResult.fail("Format Test Failed: Phone must be 10-13 digits (optional leading '+', e.g., +923001234567).");
                }
            }

            // 4. Existence & Combination Test on Role and Sub-Team (Alt Flow 2A)
            if (chosenRole == null || (!chosenRole.equals("Group Member") && !chosenRole.equals("Team Member"))) {
                return ValidationResult.fail("Existence Test Failed: Role must be either 'Group Member' or 'Team Member'.");
            }
            if ("Team Member".equals(chosenRole)) {
                if (chosenSubTeam == null || chosenSubTeam.trim().isEmpty() || "-- Select Specialty Team --".equals(chosenSubTeam)) {
                    return ValidationResult.fail("Combination Test Failed (Alt Flow 2A): When joining as a 'Team Member', you must select the specific team you wish to join.");
                }
                if (cleanEmail.isEmpty()) {
                    return ValidationResult.fail("Combination Test Failed (Alt Flow 2A): An Email Address is mandatory for Team Members so the interview date can be mailed.");
                }
            }

            return ValidationResult.ok();
        }

        // Barricade Validation for UC 6: Submit Member Report
        public static ValidationResult validateSubmitReportInput(
                UserSession session, EventEntity selectedEvent, String submissionMode,
                String reportSummary, File attachedFile, boolean hasExistingReport) {

            // 1. Transaction Check: Actor & Event Pre-conditions
            if (!"Team Member".equalsIgnoreCase(session.userRole)) {
                return ValidationResult.fail("Transaction Rejected: Only registered Team Members can submit member reports.");
            }
            if (selectedEvent == null) {
                return ValidationResult.fail("Existence Test Failed: Please select an event you participated in.");
            }
            if (!"Held".equalsIgnoreCase(selectedEvent.eventStatus)) {
                return ValidationResult.fail("Transaction Rejected: Reports can only be submitted for events with status 'Held'.");
            }

            // 2. Combination Test: New Submission vs Alt Flow 3A (Additional Info)
            if ("New Report Submission".equals(submissionMode) && hasExistingReport) {
                return ValidationResult.fail("Combination/Transaction Test Failed: You have already submitted a primary report for this event. Switch mode to 'Append Additional Information (Alt Flow 3A)'.");
            }
            if ("Append Additional Information (Alt Flow 3A)".equals(submissionMode) && !hasExistingReport) {
                return ValidationResult.fail("Combination/Transaction Test Failed: No prior report exists for this event. Please use 'New Report Submission'.");
            }

            // 3. Existence & Limit/Range Test on Report Content (SRS Data Dictionary: String, Length 1000, Required)
            String cleanContent = (reportSummary == null) ? "" : reportSummary.trim();
            if (cleanContent.isEmpty()) {
                return ValidationResult.fail("Existence Test Failed: Report Content / Summary cannot be empty.");
            }
            if (cleanContent.length() < 15 || cleanContent.length() > 1000) {
                return ValidationResult.fail("Limit/Range Test Failed: Report Content must be between 15 and 1000 characters (Current: "
                        + cleanContent.length() + ").");
            }

            // 4. Existence, Format & Limit Test on Report File (Exception 1: Invalid file format)
            if (attachedFile == null) {
                return ValidationResult.fail("Existence Test Failed: Please select a valid report file to upload.");
            }
            if (!attachedFile.exists() || !attachedFile.isFile()) {
                return ValidationResult.fail("Existence Test Failed: The selected report file does not exist on disk.");
            }
            String fileName = attachedFile.getName();
            int dotIndex = fileName.lastIndexOf('.');
            String ext = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1).toLowerCase();
            if (!ALLOWED_REPORT_EXTENSIONS.contains(ext)) {
                return ValidationResult.fail("Exception 1 (Invalid File Format): '" + fileName
                        + "' is not allowed. Only .pdf, .docx, or .txt report files are accepted.");
            }
            long maxFileSize = 10L * 1024L * 1024L; // 10 MB limit for document reports
            if (attachedFile.length() > maxFileSize) {
                return ValidationResult.fail("Limit/Range Test Failed: Report file size exceeds the 10 MB maximum limit.");
            }

            return ValidationResult.ok();
        }

        // Barricade Validation for UC 11: Review Team Member Report
        public static ValidationResult validateReviewReportInput(
                UserSession session, MemberReportEntity selectedReport,
                String reviewAction, String leaderComments) {

            // 1. Transaction Check: Actor & Report State
            if (!"Group Leader".equalsIgnoreCase(session.userRole)) {
                return ValidationResult.fail("Transaction Rejected: Only a Group Leader can review team member reports.");
            }
            if (selectedReport == null) {
                return ValidationResult.fail("Existence Test Failed: Please select a submitted team member report to review.");
            }
            if (!"Submitted".equalsIgnoreCase(selectedReport.status) && !"Returned".equalsIgnoreCase(selectedReport.status)) {
                return ValidationResult.fail("Transaction Rejected: Report #" + selectedReport.reportId
                        + " has already been finalized ('" + selectedReport.status + "').");
            }

            // 2. Existence Test on Action
            if (reviewAction == null || (!reviewAction.equals("APPROVE") && !reviewAction.equals("REQUEST_INFO"))) {
                return ValidationResult.fail("Existence Test Failed: Please choose a valid Review Decision.");
            }

            // 3. Combination Test & Limit/Range Test on Report Feedback (SRS Data Dictionary: String, Length 300)
            String cleanComments = (leaderComments == null) ? "" : leaderComments.trim();
            if (cleanComments.length() > 300) {
                return ValidationResult.fail("Limit/Range Test Failed: Leader Feedback/Information cannot exceed 300 characters (Current: "
                        + cleanComments.length() + ").");
            }

            // Combination Test: Main Success Scenario Step 4 ("Group leader adds relevant information")
            // and Alt Flow 3a ("Group leader asks for additional information from team member due to an incomplete report")
            if ("REQUEST_INFO".equals(reviewAction) && cleanComments.length() < 10) {
                return ValidationResult.fail("Combination Test Failed (Alt Flow 3a): When requesting additional information for an incomplete report, you must specify what is missing (at least 10 characters).");
            }
            if ("APPROVE".equals(reviewAction) && cleanComments.isEmpty()) {
                return ValidationResult.fail("Combination Test Failed (MSS Step 4): Please add relevant review notes/information before approving the report.");
            }

            return ValidationResult.ok();
        }
    }

    // =========================================================================
    // 3. INTERNAL OPERATING ROOM / SAFE ZONE (Slides 15, 18-19: Uses Assertions)
    // =========================================================================
    static class EcoDriveSafeService {
        private final List<GroupEntity> groups = new ArrayList<>();
        private final List<EventEntity> events = new ArrayList<>();
        private final List<MemberReportEntity> reports = new ArrayList<>();
        private int nextReportId = 7001;

        public EcoDriveSafeService() {
            // Seed sample groups for Islamabad neighborhoods
            groups.add(new GroupEntity(30211, "Green Margalla", 1012, "Sector F-10", "Active", 14, 62));
            groups.add(new GroupEntity(30212, "Clean Rawal", 1014, "Sector G-11", "Active", 11, 48));
            groups.add(new GroupEntity(30213, "Capital Canopy", 1012, "Sector F-10", "Active", 19, 85));
            groups.add(new GroupEntity(30214, "Eco Trailblazers", 1015, "Sector E-7", "Active", 8, 30));
            groups.add(new GroupEntity(30215, "Dormant Savers", 1012, "Sector F-10", "Locked", 2, 10));

            // Seed sample held events for UC 6 & UC 11
            events.add(new EventEntity(500120, "Spring Plantation Drive", "Tree plantation", "Held", 30211));
            events.add(new EventEntity(500121, "Margalla Trail 3 Cleanup", "Cleanup", "Held", 30211));
            events.add(new EventEntity(500122, "F-10 Park Sapling Care", "Tree plantation", "Scheduled", 30211));

            // Seed sample submitted member reports for UC 11
            reports.add(new MemberReportEntity(
                    7000, 500120, "Spring Plantation Drive", 210088, "Zainab Tariq",
                    "Logistics & Sapling Distribution Team", "08/11/2026",
                    "Planted 40 Neem and Chir Pine saplings across the northern ridge. All 15 volunteers received tools on time.",
                    "C:/EcoDrive/Reports/spring_plantation_logistics.pdf", "Submitted"));
        }

        public List<GroupEntity> getAllGroups() {
            return Collections.unmodifiableList(groups);
        }

        public List<EventEntity> getHeldEventsForGroup(int groupId) {
            List<EventEntity> result = new ArrayList<>();
            for (EventEntity e : events) {
                if (e.groupId == groupId && "Held".equalsIgnoreCase(e.eventStatus)) {
                    result.add(e);
                }
            }
            return result;
        }

        public List<MemberReportEntity> getReportsList() {
            return Collections.unmodifiableList(reports);
        }

        public boolean hasUserSubmittedReport(int userId, int eventId) {
            for (MemberReportEntity r : reports) {
                if (r.authorUserId == userId && r.eventId == eventId) {
                    return true;
                }
            }
            return false;
        }

        /**
         * UC 4 Internal Method: ComputeRanking() (SRS 3.3.5)
         * Inside the Barricade -> Uses Java Assertions (Slide 18-19)
         */
        public List<GroupEntity> computeRanking(int userId, Integer userGroupId, String areaFilter) {
            assert userId > 0 : "Internal Invariant Violated: userId must be positive inside safe zone.";
            assert userGroupId != null : "Internal Invariant Violated: userGroupId cannot be null inside safe zone.";
            assert areaFilter != null && !areaFilter.isEmpty() : "Internal Invariant Violated: areaFilter empty.";

            List<GroupEntity> filtered = new ArrayList<>();
            for (GroupEntity g : groups) {
                if ("All Neighborhoods".equals(areaFilter) || g.areaName.equalsIgnoreCase(areaFilter)) {
                    filtered.add(g);
                }
            }
            filtered.sort((a, b) -> Integer.compare(b.getCalculatedScore(), a.getCalculatedScore()));
            return filtered;
        }

        /**
         * UC 5 Internal Method: JoinGroup() (SRS 3.3.1)
         * Inside the Barricade -> Uses Java Assertions (Slide 18-19)
         */
        public String joinGroupInternal(UserSession session, GroupEntity group, String cleanName,
                                        String cleanEmail, String cleanPhone, String role, String subTeam) {
            assert session != null && session.groupId == null : "Invariant Violated: User already in a group.";
            assert group != null && "Active".equals(group.status) : "Invariant Violated: Group must be Active.";
            assert cleanName.length() <= 50 : "Invariant Violated: Name exceeds 50 chars.";
            assert cleanEmail.length() <= 50 : "Invariant Violated: Email exceeds 50 chars.";
            assert cleanPhone.length() <= 13 : "Invariant Violated: Phone exceeds 13 chars.";

            session.userName = cleanName;
            session.userEmail = cleanEmail;
            session.userPhone = cleanPhone;
            session.groupId = group.groupId;
            session.userRole = role;
            group.memberCount++;

            if ("Team Member".equals(role)) {
                String interviewDate = LocalDate.now().plusDays(3)
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                return "Alt Flow 2A Executed: Application recorded for '" + subTeam + "' in " + group.groupName
                        + ".\nAn interview date (" + interviewDate + " at 10:00 AM) has been mailed to " + cleanEmail + ".";
            } else {
                String contactTarget = !cleanEmail.isEmpty() ? cleanEmail : cleanPhone;
                return "Main Success Scenario: You have joined '" + group.groupName + "' as a Group Member!\n"
                        + "Confirmation dispatched to: " + contactTarget;
            }
        }

        /**
         * UC 6 Internal Method: SubmitReport() (SRS 3.3.4)
         * Inside the Barricade -> Uses Java Assertions (Slide 18-19)
         */
        public MemberReportEntity submitReportInternal(UserSession session, EventEntity event,
                                                       String teamRole, String cleanContent,
                                                       File cleanFile, boolean isAppendMode) {
            assert "Team Member".equals(session.userRole) : "Invariant Violated: Actor must be Team Member.";
            assert event != null && "Held".equals(event.eventStatus) : "Invariant Violated: Event must be Held.";
            assert cleanContent.length() >= 15 && cleanContent.length() <= 1000 : "Invariant Violated: Content length out of bounds.";
            assert cleanFile != null && cleanFile.exists() : "Invariant Violated: Report file missing.";

            String systemGeneratedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            if (isAppendMode) {
                for (MemberReportEntity existing : reports) {
                    if (existing.authorUserId == session.userId && existing.eventId == event.eventId) {
                        String merged = existing.content + "\n[Addendum " + systemGeneratedDate + "]: " + cleanContent;
                        if (merged.length() > 1000) {
                            merged = merged.substring(0, 1000);
                        }
                        existing.content = merged;
                        existing.filePath = cleanFile.getAbsolutePath();
                        existing.status = "Submitted";
                        return existing;
                    }
                }
            }

            MemberReportEntity newReport = new MemberReportEntity(
                    nextReportId++, event.eventId, event.eventTitle, session.userId,
                    session.userName, teamRole, systemGeneratedDate,
                    cleanContent, cleanFile.getAbsolutePath(), "Submitted"
            );
            reports.add(newReport);
            return newReport;
        }

        /**
         * UC 11 Internal Method: ReviewReport() & RequestAdditionalInfo() (SRS 3.3.4)
         * Inside the Barricade -> Uses Java Assertions (Slide 18-19)
         */
        public String reviewReportInternal(UserSession session, MemberReportEntity report,
                                           String action, String cleanFeedback) {
            assert "Group Leader".equals(session.userRole) : "Invariant Violated: Actor must be Group Leader.";
            assert report != null : "Invariant Violated: Report is null.";
            assert cleanFeedback.length() <= 300 : "Invariant Violated: Feedback exceeds 300 chars.";

            report.feedback = cleanFeedback;
            if ("APPROVE".equals(action)) {
                report.status = "Reviewed"; // SRS Post-condition: status changed from submitted to reviewed
                return "Report #" + report.reportId + " approved. Status updated from 'Submitted' to 'Reviewed'.";
            } else {
                report.status = "Returned"; // SRS Data Dictionary state for incomplete report returned
                return "Alt Flow 3a Executed: Additional information requested from " + report.authorName
                        + ". Report status marked as 'Returned'.";
            }
        }
    }

    // =========================================================================
    // 4. GUI APPLICATION & 4 USE CASE SCREENS
    // =========================================================================
    private final EcoDriveSafeService safeService = new EcoDriveSafeService();
    private final UserSession currentSession = new UserSession(
            210045, "Muhammad Umais", "umais@eco-drive.pk", "+923001234567",
            "Group Member", 1012, "Sector F-10", 30211
    );

    private JLabel sessionBannerLabel;

    public EcoDriveUmaisScreens() {
        setTitle("Eco-Drive Platform — Muhammad Umais (04072413005) — Defensive Input Screens");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 740);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        add(createSessionSimulationBar(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabbedPane.addTab("UC 4: View Leaderboard", createUC4LeaderboardScreen());
        tabbedPane.addTab("UC 5: Join Group", createUC5JoinGroupScreen());
        tabbedPane.addTab("UC 6: Submit Member Report", createUC6SubmitReportScreen());
        tabbedPane.addTab("UC 11: Review Team Member Report", createUC11ReviewReportScreen());

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Top bar allowing you to toggle pre-conditions (e.g., Role, Group Membership)
     * so you can demonstrate Barricade Pre-condition/Transaction checks live in class.
     */
    private JPanel createSessionSimulationBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bar.setBackground(new Color(225, 240, 228));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(46, 125, 50)));

        sessionBannerLabel = new JLabel();
        sessionBannerLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        updateSessionBannerText();

        JButton btnPresetUnjoined = new JButton("Preset: Unjoined User (For UC 5)");
        btnPresetUnjoined.addActionListener(e -> {
            currentSession.userRole = "User";
            currentSession.groupId = null;
            updateSessionBannerText();
        });

        JButton btnPresetTeamMember = new JButton("Preset: Team Member (For UC 4 & UC 6)");
        btnPresetTeamMember.addActionListener(e -> {
            currentSession.userRole = "Team Member";
            currentSession.groupId = 30211;
            updateSessionBannerText();
        });

        JButton btnPresetLeader = new JButton("Preset: Group Leader (For UC 4 & UC 11)");
        btnPresetLeader.addActionListener(e -> {
            currentSession.userRole = "Group Leader";
            currentSession.groupId = 30211;
            updateSessionBannerText();
        });

        bar.add(sessionBannerLabel);
        bar.add(btnPresetUnjoined);
        bar.add(btnPresetTeamMember);
        bar.add(btnPresetLeader);
        return bar;
    }

    private void updateSessionBannerText() {
        String grpText = (currentSession.groupId == null) ? "None (Not Joined)" : String.valueOf(currentSession.groupId);
        sessionBannerLabel.setText(String.format(
                "System Retrieved Session -> UserID: %d | Name: %s | Role: %s | Area: %s | GroupID: %s",
                currentSession.userId, currentSession.userName, currentSession.userRole,
                currentSession.areaName, grpText));
    }

    // -------------------------------------------------------------------------
    // SCREEN 1: Use Case 4 - View Leaderboard
    // -------------------------------------------------------------------------
    private JPanel createUC4LeaderboardScreen() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top Control Bar: Minimal Input (Data Capture Guideline: No manual entry of calculated/retrieved data)
        JPanel topControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        topControls.setBorder(new TitledBorder("Leaderboard Filter & System Status (Slide 9: System Retrieves Context & Calculates Rank)"));

        JComboBox<String> areaFilterCombo = new JComboBox<>(new String[]{
                "All Neighborhoods", "Sector F-10", "Sector G-11", "Sector E-7"
        });
        JCheckBox simulateOutageCheck = new JCheckBox("Simulate Exception 1 (System Outage)");
        JButton btnRefresh = new JButton("View / Refresh Leaderboard");

        topControls.add(new JLabel("Filter Neighborhood:"));
        topControls.add(areaFilterCombo);
        topControls.add(btnRefresh);
        topControls.add(simulateOutageCheck);

        // Read-Only Leaderboard Table
        String[] cols = {"Calculated Rank", "Group ID", "Group Name", "Neighborhood (Area)", "Status", "Events Held", "Members", "Computed Score"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // No Garbage Allowed In: Users cannot edit calculated rankings
            }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(26);

        JLabel standingLabel = new JLabel("Click 'View / Refresh Leaderboard' to assess where your group stands.");
        standingLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        standingLabel.setForeground(new Color(27, 94, 32));

        btnRefresh.addActionListener(e -> {
            // Error Handling Outside the Barricade (Slide 18 & 20: Robustness)
            try {
                if (simulateOutageCheck.isSelected()) {
                    throw new RuntimeException("Database connection timeout while retrieving neighborhood rankings.");
                }

                String selectedArea = (String) areaFilterCombo.getSelectedItem();
                ValidationResult vr = InputBarricade.validateLeaderboardAccess(currentSession, selectedArea);
                if (!vr.valid) {
                    JOptionPane.showMessageDialog(this, vr.errorMessage,
                            "Barricade Access Denied (UC 4)", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Crosses Barricade into Safe Zone
                List<GroupEntity> ranked = safeService.computeRanking(
                        currentSession.userId, currentSession.groupId, selectedArea);

                tableModel.setRowCount(0);
                int rank = 1;
                int userGroupRank = -1;
                for (GroupEntity g : ranked) {
                    if (g.groupId == currentSession.groupId) {
                        userGroupRank = rank;
                    }
                    tableModel.addRow(new Object[]{
                            "#" + rank, g.groupId, g.groupName, g.areaName,
                            g.status, g.eventsOrganized, g.memberCount, g.getCalculatedScore()
                    });
                    rank++;
                }

                if (userGroupRank != -1) {
                    standingLabel.setText("Post-Condition Met: Your Group (ID " + currentSession.groupId
                            + ") currently stands at Rank #" + userGroupRank + " in the selected view.");
                } else {
                    standingLabel.setText("Your Group (ID " + currentSession.groupId
                            + ") is in a different neighborhood filter.");
                }
            } catch (Exception ex) {
                // Exception 1 handled robustly without crashing the program
                JOptionPane.showMessageDialog(this,
                        "Exception 1 (System Outage/Error): " + ex.getMessage() + "\nThe application remains stable.",
                        "System Failure Handled Gracefully", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(topControls, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(standingLabel, BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // SCREEN 2: Use Case 5 - Join Group
    // -------------------------------------------------------------------------
    private JPanel createUC5JoinGroupScreen() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("UC 5: Join Neighborhood Group (Click 'Preset: Unjoined User' at top to test joining)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 1. System-Retrieved Read-Only Fields (Slide 9: Do not require entry of details system can retrieve)
        JTextField txtRetrievedUserId = new JTextField(String.valueOf(currentSession.userId), 20);
        txtRetrievedUserId.setEditable(false);
        txtRetrievedUserId.setBackground(new Color(235, 235, 235));

        JTextField txtRetrievedArea = new JTextField(currentSession.areaName + " (Area ID: " + currentSession.areaId + ")", 20);
        txtRetrievedArea.setEditable(false);
        txtRetrievedArea.setBackground(new Color(235, 235, 235));

        // 2. Identification Data: Select Group from Active Neighborhood Groups
        DefaultComboBoxModel<GroupEntity> groupComboModel = new DefaultComboBoxModel<>();
        for (GroupEntity g : safeService.getAllGroups()) {
            groupComboModel.addElement(g);
        }
        JComboBox<GroupEntity> comboGroups = new JComboBox<>(groupComboModel);

        // 3. Variable Data: Personal & Contact Details (Pre-populated from profile, editable within Data Dictionary limits)
        JTextField txtFullName = new JTextField(currentSession.userName, 25);
        JTextField txtEmail = new JTextField(currentSession.userEmail, 25);
        JTextField txtPhone = new JTextField(currentSession.userPhone, 25);

        // 4. Role Selection (Main Success Scenario vs Alt Flow 2A)
        JRadioButton rbGroupMember = new JRadioButton("Group Member (Main Success Scenario)", true);
        JRadioButton rbTeamMember = new JRadioButton("Team Member (Alt Flow 2A - Requires Team & Interview)");
        ButtonGroup roleGroup = new ButtonGroup();
        roleGroup.add(rbGroupMember);
        roleGroup.add(rbTeamMember);

        // 5. Conditional Sub-Team Selection (Only enabled when Team Member is selected - Avoiding Extra Steps)
        JComboBox<String> comboSubTeam = new JComboBox<>(new String[]{
                "-- Select Specialty Team --",
                "Logistics & Sapling Distribution Team",
                "Media & Documentation Team",
                "Site Cleanup & Waste Sorting Team",
                "Community Outreach Team"
        });
        comboSubTeam.setEnabled(false);

        rbGroupMember.addActionListener(e -> comboSubTeam.setEnabled(false));
        rbTeamMember.addActionListener(e -> comboSubTeam.setEnabled(true));

        // Layout Assembly
        int row = 0;
        addFormRow(form, gbc, row++, "User ID [System Retrieved]:", txtRetrievedUserId);
        addFormRow(form, gbc, row++, "Neighborhood [System Retrieved]:", txtRetrievedArea);
        addFormRow(form, gbc, row++, "Select Group to Join*:", comboGroups);
        addFormRow(form, gbc, row++, "Full Name* (Max 50 chars):", txtFullName);
        addFormRow(form, gbc, row++, "Email (Max 50 chars, req. for Team):", txtEmail);
        addFormRow(form, gbc, row++, "Phone (Max 13 chars, e.g. +923001234567):", txtPhone);

        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rolePanel.add(rbGroupMember);
        rolePanel.add(rbTeamMember);
        addFormRow(form, gbc, row++, "Desired Role*:", rolePanel);
        addFormRow(form, gbc, row++, "Specialty Team (Alt Flow 2A):", comboSubTeam);

        JTextArea outputArea = new JTextArea(5, 50);
        outputArea.setEditable(false);
        outputArea.setBorder(new TitledBorder("Transaction & Confirmation Output"));

        JButton btnJoin = new JButton("Join Selected Group");
        btnJoin.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnJoin.addActionListener(e -> {
            try {
                GroupEntity selectedGroup = (GroupEntity) comboGroups.getSelectedItem();
                String role = rbGroupMember.isSelected() ? "Group Member" : "Team Member";
                String subTeam = (String) comboSubTeam.getSelectedItem();

                // Step 1: Pass dirty GUI inputs to Barricade Class
                ValidationResult vr = InputBarricade.validateJoinGroupInput(
                        currentSession, selectedGroup, txtFullName.getText(),
                        txtEmail.getText(), txtPhone.getText(), role, subTeam
                );

                if (!vr.valid) {
                    outputArea.setText("BARRICADE REJECTED INPUT:\n" + vr.errorMessage);
                    JOptionPane.showMessageDialog(this, vr.errorMessage,
                            "Exception 1: Input Validation Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Step 2: Sanitize and pass into Safe Zone (Operating Room)
                String msg = safeService.joinGroupInternal(
                        currentSession, selectedGroup,
                        txtFullName.getText().trim(),
                        txtEmail.getText().trim(),
                        txtPhone.getText().trim(),
                        role, subTeam
                );

                updateSessionBannerText();
                outputArea.setText("SUCCESS:\n" + msg);
                JOptionPane.showMessageDialog(this, msg, "Group Joined Successfully", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Exception 2 (System Error): " + ex.getMessage(),
                        "System Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnJoin);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.add(form, BorderLayout.NORTH);
        centerWrapper.add(btnPanel, BorderLayout.CENTER);

        panel.add(centerWrapper, BorderLayout.CENTER);
        panel.add(new JScrollPane(outputArea), BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // SCREEN 3: Use Case 6 - Submit Member Report
    // -------------------------------------------------------------------------
    private JPanel createUC6SubmitReportScreen() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("UC 6: Submit Member Report (Click 'Preset: Team Member' at top to test)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // System-Generated & System-Retrieved Read-Only Fields (Slide 9)
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        JTextField txtAutoDate = new JTextField(todayStr + " (Auto-Generated by System | Initial Status: Submitted)", 30);
        txtAutoDate.setEditable(false);
        txtAutoDate.setBackground(new Color(235, 235, 235));

        // Identification Data: Held Events User Participated In
        DefaultComboBoxModel<EventEntity> eventComboModel = new DefaultComboBoxModel<>();
        for (EventEntity ev : safeService.getHeldEventsForGroup(30211)) {
            eventComboModel.addElement(ev);
        }
        JComboBox<EventEntity> comboEvents = new JComboBox<>(eventComboModel);

        // Submission Mode: Main Success Scenario vs Alt Flow 3A
        JComboBox<String> comboMode = new JComboBox<>(new String[]{
                "New Report Submission",
                "Append Additional Information (Alt Flow 3A)"
        });

        JComboBox<String> comboRoleInTeam = new JComboBox<>(new String[]{
                "Logistics & Sapling Distribution Team",
                "Media & Documentation Team",
                "Site Cleanup & Waste Sorting Team"
        });

        // Report Content Area with Live Character Counter (Limit/Range Test: Max 1000 chars)
        JTextArea txtReportContent = new JTextArea(6, 35);
        txtReportContent.setLineWrap(true);
        txtReportContent.setWrapStyleWord(true);
        JLabel lblCharCount = new JLabel("Characters: 0 / 1000 (Min 15 required)");
        txtReportContent.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void updateCount() {
                int len = txtReportContent.getText().trim().length();
                lblCharCount.setText("Characters: " + len + " / 1000 (Min 15 required)");
                lblCharCount.setForeground(len > 1000 ? Color.RED : new Color(46, 125, 50));
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCount(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCount(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCount(); }
        });

        // File Picker with Extension Filter (Exception 1 Prevention: Only .pdf, .docx, .txt)
        final File[] selectedFileHolder = new File[1];
        JTextField txtFilePath = new JTextField("No file selected (.pdf, .docx, .txt allowed)", 25);
        txtFilePath.setEditable(false);
        JButton btnBrowse = new JButton("Browse Report File...");
        JButton btnCreateSampleValidFile = new JButton("Generate Sample .txt File");
        JButton btnCreateSampleInvalidFile = new JButton("Test Exception 1 (.exe File)");

        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Valid Report Files (.pdf, .docx, .txt)", "pdf", "docx", "txt"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFileHolder[0] = chooser.getSelectedFile();
                txtFilePath.setText(selectedFileHolder[0].getName());
            }
        });

        btnCreateSampleValidFile.addActionListener(e -> {
            try {
                File temp = File.createTempFile("EcoDrive_MemberReport_", ".txt");
                temp.deleteOnExit();
                selectedFileHolder[0] = temp;
                txtFilePath.setText(temp.getName() + " (Valid .txt)");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not create temp file: " + ex.getMessage());
            }
        });

        btnCreateSampleInvalidFile.addActionListener(e -> {
            try {
                File badTemp = File.createTempFile("MaliciousScript_", ".exe");
                badTemp.deleteOnExit();
                selectedFileHolder[0] = badTemp;
                txtFilePath.setText(badTemp.getName() + " (Invalid .exe for testing Exception 1)");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not create temp file: " + ex.getMessage());
            }
        });

        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filePanel.add(txtFilePath);
        filePanel.add(btnBrowse);
        filePanel.add(btnCreateSampleValidFile);
        filePanel.add(btnCreateSampleInvalidFile);

        int row = 0;
        addFormRow(form, gbc, row++, "Report Metadata [System Generated]:", txtAutoDate);
        addFormRow(form, gbc, row++, "Select Participated Event*:", comboEvents);
        addFormRow(form, gbc, row++, "Submission Mode*:", comboMode);
        addFormRow(form, gbc, row++, "Your Team Role in Event*:", comboRoleInTeam);
        addFormRow(form, gbc, row++, "Report Summary / Content*:", new JScrollPane(txtReportContent));
        addFormRow(form, gbc, row++, "Length Range Check:", lblCharCount);
        addFormRow(form, gbc, row++, "Upload Report File*:", filePanel);

        JLabel lblSubmissionStatus = new JLabel("Submission Status: No report submitted in this session yet.");
        lblSubmissionStatus.setFont(new Font("SansSerif", Font.BOLD, 13));

        JButton btnSubmitReport = new JButton("Submit Member Report");
        btnSubmitReport.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSubmitReport.addActionListener(e -> {
            try {
                EventEntity selectedEvent = (EventEntity) comboEvents.getSelectedItem();
                String mode = (String) comboMode.getSelectedItem();
                boolean alreadyExists = (selectedEvent != null) &&
                        safeService.hasUserSubmittedReport(currentSession.userId, selectedEvent.eventId);

                // Barricade Check
                ValidationResult vr = InputBarricade.validateSubmitReportInput(
                        currentSession, selectedEvent, mode,
                        txtReportContent.getText(), selectedFileHolder[0], alreadyExists
                );

                if (!vr.valid) {
                    JOptionPane.showMessageDialog(this, vr.errorMessage,
                            "Barricade Rejected Report Submission (UC 6)", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                boolean isAppend = "Append Additional Information (Alt Flow 3A)".equals(mode);
                MemberReportEntity saved = safeService.submitReportInternal(
                        currentSession, selectedEvent,
                        (String) comboRoleInTeam.getSelectedItem(),
                        txtReportContent.getText().trim(),
                        selectedFileHolder[0], isAppend
                );

                lblSubmissionStatus.setText("Post-Condition Met -> Report #" + saved.reportId
                        + " Status: [" + saved.status + "] | Date: " + saved.reportDate);
                JOptionPane.showMessageDialog(this,
                        "Report #" + saved.reportId + " successfully recorded!\nCurrent Status: " + saved.status,
                        "Report Submitted", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Exception 2 (System Error): " + ex.getMessage(),
                        "System Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel southPanel = new JPanel(new BorderLayout(8, 8));
        southPanel.add(btnSubmitReport, BorderLayout.EAST);
        southPanel.add(lblSubmissionStatus, BorderLayout.WEST);

        panel.add(form, BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // SCREEN 4: Use Case 11 - Review Team Member Report
    // -------------------------------------------------------------------------
    private JPanel createUC11ReviewReportScreen() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topBar.setBorder(new TitledBorder("UC 11: Select Team Member Report to Review (Click 'Preset: Group Leader' at top to test)"));

        DefaultComboBoxModel<MemberReportEntity> reportComboModel = new DefaultComboBoxModel<>();
        JComboBox<MemberReportEntity> comboReports = new JComboBox<>(reportComboModel);
        JButton btnReloadList = new JButton("Refresh Submitted Reports");

        // Read-Only Details Display (Slide 9: System retrieves report details)
        JTextArea txtReadOnlyReportView = new JTextArea(8, 50);
        txtReadOnlyReportView.setEditable(false);
        txtReadOnlyReportView.setBackground(new Color(245, 245, 245));
        txtReadOnlyReportView.setBorder(new TitledBorder("System-Retrieved Team Member Report Details (Read-Only)"));

        Runnable refreshReportsDropdown = () -> {
            reportComboModel.removeAllElements();
            for (MemberReportEntity r : safeService.getReportsList()) {
                reportComboModel.addElement(r);
            }
        };

        comboReports.addActionListener(e -> {
            MemberReportEntity sel = (MemberReportEntity) comboReports.getSelectedItem();
            if (sel != null) {
                txtReadOnlyReportView.setText(
                        "Report ID: " + sel.reportId + " | Current Status: " + sel.status + "\n" +
                        "Event: " + sel.eventTitle + " (ID: " + sel.eventId + ")\n" +
                        "Submitted By: " + sel.authorName + " (User ID: " + sel.authorUserId + ") | Team: " + sel.teamRole + "\n" +
                        "Submission Date: " + sel.reportDate + "\n" +
                        "Attached File: " + sel.filePath + "\n" +
                        "-------------------------------------------------------------------------\n" +
                        "Report Content:\n" + sel.content + "\n" +
                        (sel.feedback.isEmpty() ? "" : "\nPrevious Leader Notes: " + sel.feedback)
                );
            }
        });

        btnReloadList.addActionListener(e -> refreshReportsDropdown.run());
        refreshReportsDropdown.run();

        topBar.add(new JLabel("Select Report:"));
        topBar.add(comboReports);
        topBar.add(btnReloadList);

        // Review Decision & Feedback Form
        JPanel reviewForm = new JPanel(new GridBagLayout());
        reviewForm.setBorder(new TitledBorder("Group Leader Review Decision & Feedback (Limit: Max 300 chars)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JRadioButton rbApprove = new JRadioButton("Approve Report & Add Relevant Info (MSS Steps 3-4 -> Status: 'Reviewed')", true);
        JRadioButton rbRequestInfo = new JRadioButton("Request Additional Information for Incomplete Report (Alt Flow 3a -> Status: 'Returned')");
        ButtonGroup decisionGroup = new ButtonGroup();
        decisionGroup.add(rbApprove);
        decisionGroup.add(rbRequestInfo);

        JTextArea txtLeaderFeedback = new JTextArea(4, 35);
        txtLeaderFeedback.setLineWrap(true);
        txtLeaderFeedback.setWrapStyleWord(true);
        JLabel lblFeedbackCount = new JLabel("Characters: 0 / 300");

        txtLeaderFeedback.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                int len = txtLeaderFeedback.getText().trim().length();
                lblFeedbackCount.setText("Characters: " + len + " / 300");
                lblFeedbackCount.setForeground(len > 300 ? Color.RED : new Color(46, 125, 50));
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        int row = 0;
        addFormRow(reviewForm, gbc, row++, "Review Decision*:", rbApprove);
        addFormRow(reviewForm, gbc, row++, "", rbRequestInfo);
        addFormRow(reviewForm, gbc, row++, "Relevant Info / Feedback*:", new JScrollPane(txtLeaderFeedback));
        addFormRow(reviewForm, gbc, row++, "Length Range Check:", lblFeedbackCount);

        JButton btnSubmitReview = new JButton("Submit Review Decision");
        btnSubmitReview.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSubmitReview.addActionListener(e -> {
            MemberReportEntity selectedReport = (MemberReportEntity) comboReports.getSelectedItem();
            String action = rbApprove.isSelected() ? "APPROVE" : "REQUEST_INFO";

            // Pass through Barricade
            ValidationResult vr = InputBarricade.validateReviewReportInput(
                    currentSession, selectedReport, action, txtLeaderFeedback.getText()
            );

            if (!vr.valid) {
                JOptionPane.showMessageDialog(this, vr.errorMessage,
                        "Barricade Rejected Review (UC 11)", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Execute inside Safe Zone
            String outcome = safeService.reviewReportInternal(
                    currentSession, selectedReport, action, txtLeaderFeedback.getText().trim()
            );

            comboReports.repaint();
            comboReports.getActionListeners()[0].actionPerformed(null);
            JOptionPane.showMessageDialog(this, outcome, "Review Recorded", JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel centerSplit = new JPanel(new GridLayout(2, 1, 8, 8));
        centerSplit.add(new JScrollPane(txtReadOnlyReportView));
        centerSplit.add(reviewForm);

        JPanel southBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southBar.add(btnSubmitReview);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(centerSplit, BorderLayout.CENTER);
        panel.add(southBar, BorderLayout.SOUTH);
        return panel;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.25;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.75;
        panel.add(field, gbc);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new EcoDriveUmaisScreens().setVisible(true);
        });
    }
}