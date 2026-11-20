import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** UC 3 - Validate and review events (Site Admin). */
public class ReviewEventsScreen extends JPanel {
    private final AppContext ctx = new AppContext();   // own data: not shared with the other screens

    public ReviewEventsScreen() {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        ctx.setUser(ctx.db.admin);
                
        DefaultTableModel model = new DefaultTableModel(new String[]{"Report ID", "Event", "Group", "Submitted by", "Date", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new TitledBorder("UC 3: Reports tab - select an event report (system retrieved, read-only)"));
        tableScroll.setPreferredSize(new Dimension(300, 150));

        JTextArea detail = new JTextArea(5, 50);
        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setBackground(new Color(245, 245, 245));
        detail.setBorder(new TitledBorder("Report details (read-only)"));

        JRadioButton rbApprove = new JRadioButton("Approve report and add relevant information (MSS steps 3-4, status: Validated)", true);
        JRadioButton rbRequest = new JRadioButton("Ask Group Leader for additional information (Alt Flow 3a, status: Returned)");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbApprove);
        bg.add(rbRequest);
        JTextArea notes = InputKit.filteredArea(4, 40, InputKit.TEXT_300);
        JLabel lblCount = InputKit.counter(notes, 300);
        JLabel lblHint = InputKit.noteLabel();
        JButton btnSubmit = new JButton("Submit review decision");
        JPanel decision = new JPanel(new GridBagLayout());
        decision.setBorder(new TitledBorder("Admin decision (notes: 10-300 characters)"));
        GridBagConstraints gbc = InputKit.newGbc();
        int r = 0;
        InputKit.addRow(decision, gbc, r++, "Decision*:", rbApprove);
        InputKit.addRow(decision, gbc, r++, "", rbRequest);
        InputKit.addRow(decision, gbc, r++, "Relevant information / what is missing*:", new JScrollPane(notes));
        InputKit.addRow(decision, gbc, r++, "", lblCount);

        final List<EventReport> shown = new ArrayList<>();

        Runnable update = () -> {
            int row = table.getSelectedRow();
            EventReport sel = row >= 0 && row < shown.size() ? shown.get(row) : null;
            boolean admin = ctx.user() instanceof SiteAdmin;
            boolean canReview = admin && sel != null && sel.isReviewable() && Status.HELD.equals(sel.event.eventStatus);
            InputKit.setEnabledRecursive(decision, canReview);
            String hint;
            if (!admin) hint = "Only a Site Admin can validate event reports.";
            else if (sel == null) hint = "Select a report from the list.";
            else if (!canReview) hint = "Report #" + sel.eReportID + " is '" + sel.eReportStatus + "' - nothing to review.";
            else if (InputKit.clean(notes.getText()).length() < 10) hint = "Next: add notes (at least 10 characters).";
            else hint = null;
            btnSubmit.setEnabled(canReview && hint == null);
            lblHint.setForeground(hint == null ? new Color(27, 94, 32) : new Color(160, 60, 0));
            lblHint.setText(hint == null ? "Ready: click 'Submit review decision'." : hint);
        };

        Runnable loadTable = () -> {
            model.setRowCount(0);
            shown.clear();
            for (EventReport er : ctx.facade.getEventReports()) {
                shown.add(er);
                model.addRow(new Object[]{er.eReportID, er.event.eventName, er.event.group.groupName, er.author.name,
                        er.eReportDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), er.eReportStatus});
            }
            detail.setText("");
            update.run();
        };

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = table.getSelectedRow();
            if (row >= 0 && row < shown.size()) {
                EventReport er = shown.get(row);
                detail.setText("Report ID: " + er.eReportID + " | Status: " + er.eReportStatus + "\n"
                        + "Event: " + er.event.eventName + " (ID " + er.event.eventID + ", " + er.event.eventType + ", " + er.event.eventDate + ")\n"
                        + "Group: " + er.event.group.groupName + " | Leader: " + er.author.name + "\n"
                        + "Content: " + er.content
                        + (er.feedback.isEmpty() ? "" : "\nPrevious admin notes: " + er.feedback));
            }
            update.run();
        });
        InputKit.onChange(notes, update);
        rbApprove.addActionListener(e -> update.run());
        rbRequest.addActionListener(e -> update.run());

        btnSubmit.addActionListener(e -> {
            try {
                int row = table.getSelectedRow();
                EventReport sel = row >= 0 && row < shown.size() ? shown.get(row) : null;
                String action = rbApprove.isSelected() ? "APPROVE" : "REQUEST_INFO";
                ValidationResult vr = InputBarricade.validateReview(ctx.user(), sel, action, notes.getText());
                if (!vr.valid) {
                    JOptionPane.showMessageDialog(this, vr.errorMessage, "Barricade Rejected Review (UC 3)", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                SiteAdmin admin = (SiteAdmin) ctx.user();
                String outcome;
                if ("APPROVE".equals(action)) {
                    String rank = ctx.facade.validateEventReport(admin, sel, InputKit.clean(notes.getText()));
                    outcome = "Report #" + sel.eReportID + " validated and saved for ranking; sent back to the Group Leader.\n" + rank;
                } else {
                    ctx.facade.requestAdditionalInfo(admin, sel, InputKit.clean(notes.getText()));
                    outcome = "Alt Flow 3a: Additional information requested from " + sel.author.name + ". Report status: Returned.";
                }
                notes.setText("");
                loadTable.run();
                JOptionPane.showMessageDialog(this, outcome, "Review Recorded", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "System error: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel centre = new JPanel(new BorderLayout(6, 6));
        centre.add(new JScrollPane(detail), BorderLayout.NORTH);
        centre.add(decision, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(lblHint, BorderLayout.CENTER);
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.add(btnSubmit);
        south.add(bp, BorderLayout.EAST);

        add(tableScroll, BorderLayout.NORTH);
        add(centre, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        ctx.addRefresher(loadTable);
        ctx.refreshScreens();   // fill the screen for its acting user
    }

    User actingUser() {
        return ctx.user();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ReviewEventsScreen screen = new ReviewEventsScreen();
            ScreenWindow.show(screen, screen.actingUser(), "Eco-Drive - UC 3 - Validate and review events");
        });
    }
}
