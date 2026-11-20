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

/** UC 2 - View event schedule (Group Leader, Group Member, Team Member). */
public class ViewScheduleScreen extends JPanel {
    private final AppContext ctx = new AppContext();   // own data: not shared with the other screens

    public ViewScheduleScreen() {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        ctx.setUser(ctx.db.member);
                        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(new TitledBorder("UC 2: View event schedule  (inputs: none - choose from the lists)"));
        GridBagConstraints gbc = InputKit.newGbc();

        JTextField txtUser = InputKit.readOnly("", 30);
        DefaultComboBoxModel<Group> groupModel = new DefaultComboBoxModel<>();
        JComboBox<Group> comboGroup = new JComboBox<>(groupModel);
        DefaultComboBoxModel<Event> eventModel = new DefaultComboBoxModel<>();
        JComboBox<Event> comboEvent = new JComboBox<>(eventModel);
        JButton btnView = new JButton("View event schedule");
        JLabel lblStatus = InputKit.noteLabel();

        DefaultTableModel schedModel = new DefaultTableModel(new String[]{"Date", "Time", "Activity", "Description"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(schedModel);
        table.setRowHeight(24);

        Runnable update = () -> btnView.setEnabled(ctx.user() instanceof GroupMember
                && comboGroup.getSelectedItem() != null && comboEvent.getSelectedItem() != null);

        comboGroup.addActionListener(e -> {
            eventModel.removeAllElements();
            Group g = (Group) comboGroup.getSelectedItem();
            if (g != null) for (Event ev : ctx.facade.getEventsForGroup(g)) eventModel.addElement(ev);
            schedModel.setRowCount(0);
            update.run();
        });
        comboEvent.addActionListener(e -> {
            schedModel.setRowCount(0);
            lblStatus.setText(" ");
            update.run();
        });

        btnView.addActionListener(e -> {
            Group g = (Group) comboGroup.getSelectedItem();
            Event ev = (Event) comboEvent.getSelectedItem();
            ValidationResult vr = InputBarricade.validateScheduleView(ctx.user(), g, ev);
            schedModel.setRowCount(0);
            if (!vr.valid) {
                lblStatus.setForeground(new Color(160, 40, 40));
                lblStatus.setText(vr.errorMessage);
                JOptionPane.showMessageDialog(this, vr.errorMessage, "Schedule Not Available (UC 2)", JOptionPane.WARNING_MESSAGE);
                return;
            }
            List<Schedule> list = ctx.facade.viewSchedule((GroupMember) ctx.user(), ev);
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (Schedule s : list) {
                schedModel.addRow(new Object[]{s.scheduleDate.format(df), s.scheduleTime.toString(), s.activity, s.description});
            }
            lblStatus.setForeground(new Color(27, 94, 32));
            lblStatus.setText("Post-condition met: schedule of '" + ev.eventName + "' displayed (" + list.size() + " entries).");
        });

        int r = 0;
        InputKit.addRow(top, gbc, r++, "Logged in as [system retrieved]:", txtUser);
        InputKit.addRow(top, gbc, r++, "1. Open group*:", comboGroup);
        InputKit.addRow(top, gbc, r++, "2. Select event*:", comboEvent);
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bp.add(btnView);
        InputKit.addRow(top, gbc, r++, "3.", bp);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(lblStatus, BorderLayout.SOUTH);

        ctx.addRefresher(() -> {
            txtUser.setText(ctx.user().name + " - " + ctx.user().role);
            groupModel.removeAllElements();
            eventModel.removeAllElements();
            schedModel.setRowCount(0);
            lblStatus.setText(" ");
            boolean member = ctx.user() instanceof GroupMember && ((GroupMember) ctx.user()).group != null;
            if (member) groupModel.addElement(((GroupMember) ctx.user()).group);
            comboGroup.setEnabled(member);
            comboEvent.setEnabled(member);
            if (!member) {
                lblStatus.setForeground(new Color(160, 60, 0));
                lblStatus.setText("Only members of a group can view event schedules.");
            }
            update.run();
        });
        ctx.refreshScreens();   // fill the screen for its acting user
    }

    User actingUser() {
        return ctx.user();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ViewScheduleScreen screen = new ViewScheduleScreen();
            ScreenWindow.show(screen, screen.actingUser(), "Eco-Drive - UC 2 - View event schedule");
        });
    }
}
