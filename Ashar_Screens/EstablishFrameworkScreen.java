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

/** UC 1 - Establish a framework for an event (Group Leader). */
public class EstablishFrameworkScreen extends JPanel {
    private final AppContext ctx = new AppContext();   // own data: not shared with the other screens
    private static final String[] EVENT_TYPES = {"Tree Plantation", "Cleanup Drive", "Recycling Drive", "Awareness Walk", "Park Restoration"};

    public EstablishFrameworkScreen() {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        ctx.setUser(ctx.db.leader);
                        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("UC 1: Establish a framework for an event  (actor: Group Leader)"));
        GridBagConstraints gbc = InputKit.newGbc();

        // retrieved by the system - not typed
        JTextField txtLeader = InputKit.readOnly("", 28), txtGroup = InputKit.readOnly("", 28), txtArea = InputKit.readOnly("", 28);

        // typed or chosen inputs
        JTextField txtName = InputKit.filteredField(28, InputKit.NAME_50);
        JLabel lblName = InputKit.counter(txtName, 50);
        JComboBox<String> comboType = new JComboBox<>(EVENT_TYPES);
        JTextField txtVenue = InputKit.filteredField(28, InputKit.VENUE_60);
        JLabel lblVenue = InputKit.counter(txtVenue, 60);

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        SpinnerDateModel dateModel = new SpinnerDateModel(InputKit.toDate(tomorrow), InputKit.toDate(tomorrow),
                InputKit.toDate(tomorrow.plusYears(2)), Calendar.DAY_OF_MONTH);
        JSpinner spDate = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(spDate, "dd/MM/yyyy");
        spDate.setEditor(dateEditor);
        dateEditor.getTextField().setEditable(false);

        JSpinner spFee = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 50));
        JSpinner.NumberEditor feeEditor = new JSpinner.NumberEditor(spFee, "#,##0");
        spFee.setEditor(feeEditor);
        ((DefaultFormatter) feeEditor.getTextField().getFormatter()).setAllowsInvalid(false);
        ((DefaultFormatter) feeEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);

        JTextArea txtDesc = InputKit.filteredArea(2, 28, InputKit.TEXT_300);
        JLabel lblDesc = InputKit.counter(txtDesc, 300);

        // items of interest
        DefaultTableModel itemsModel = new DefaultTableModel(new String[]{"Item name", "Description"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable itemsTable = new JTable(itemsModel);
        itemsTable.setRowHeight(22);
        JTextField txtItemName = InputKit.filteredField(12, InputKit.ITEM_NAME_30);
        JTextField txtItemDesc = InputKit.filteredField(16, InputKit.TEXT_100);
        JButton btnAddItem = new JButton("Add");
        JButton btnRemoveItem = new JButton("Remove");
        final boolean[] suggested = {false};

        JLabel lblHint = InputKit.noteLabel();
        JButton btnConfirm = new JButton("Confirm establishment");
        JButton btnCancel = new JButton("Cancel");
        JTextArea output = new JTextArea(4, 50);
        output.setEditable(false);
        output.setBorder(new TitledBorder("Output: Event Framework"));
        final boolean[] allowed = {false};

        Runnable update = new Runnable() {
            public void run() {
                String itemName = InputKit.clean(txtItemName.getText());
                boolean dup = false;
                for (int i = 0; i < itemsModel.getRowCount(); i++) {
                    if (itemsModel.getValueAt(i, 0).toString().equalsIgnoreCase(itemName)) dup = true;
                }
                btnAddItem.setEnabled(allowed[0] && !itemName.isEmpty() && !dup && itemsModel.getRowCount() < 10);
                btnRemoveItem.setEnabled(allowed[0] && itemsTable.getSelectedRow() >= 0);
                String hint = null;
                if (!allowed[0]) hint = "Only a Group Leader with an active group can use this screen.";
                else if (InputKit.clean(txtName.getText()).length() < 3) hint = "Next: enter the event name (3-50 characters).";
                else if (InputKit.clean(txtVenue.getText()).length() < 3) hint = "Next: enter the event location / venue (3-60 characters).";
                else if (InputKit.clean(txtDesc.getText()).length() < 10) hint = "Next: enter a description (10-300 characters).";
                else if (itemsModel.getRowCount() == 0) hint = "Next: add at least one event item.";
                btnConfirm.setEnabled(allowed[0] && hint == null);
                btnCancel.setEnabled(allowed[0]);
                lblHint.setForeground(hint == null ? new Color(27, 94, 32) : new Color(160, 60, 0));
                lblHint.setText(hint == null ? "All inputs are valid. Click 'Confirm establishment'." : hint);
            }
        };

        Runnable loadSuggestions = () -> {
            itemsModel.setRowCount(0);
            String type = (String) comboType.getSelectedItem();
            String[][] s;
            switch (type) {
                case "Tree Plantation": s = new String[][]{{"Type of tree", "Species planted"}, {"Total trees", "Number of trees planted"}}; break;
                case "Cleanup Drive": s = new String[][]{{"Bags collected", "Number of waste bags"}}; break;
                case "Recycling Drive": s = new String[][]{{"Recyclables collected", "Weight in kilograms"}}; break;
                case "Awareness Walk": s = new String[][]{{"Participants", "Number of walkers"}}; break;
                default: s = new String[][]{{"Area restored", "Size of restored area"}};
            }
            for (String[] row : s) itemsModel.addRow(row);
            suggested[0] = true;
        };
        comboType.addActionListener(e -> {
            if (suggested[0] || itemsModel.getRowCount() == 0) loadSuggestions.run();
            update.run();
        });

        btnAddItem.addActionListener(e -> {
            itemsModel.addRow(new Object[]{InputKit.clean(txtItemName.getText()), InputKit.clean(txtItemDesc.getText())});
            txtItemName.setText("");
            txtItemDesc.setText("");
            suggested[0] = false;
            update.run();
        });
        btnRemoveItem.addActionListener(e -> {
            int r = itemsTable.getSelectedRow();
            if (r >= 0) itemsModel.removeRow(r);
            suggested[0] = false;
            update.run();
        });
        itemsTable.getSelectionModel().addListSelectionListener(e -> update.run());
        InputKit.onChange(txtName, update);
        InputKit.onChange(txtVenue, update);
        InputKit.onChange(txtDesc, update);
        InputKit.onChange(txtItemName, update);

        Runnable resetForm = () -> {
            txtName.setText("");
            txtVenue.setText("");
            txtDesc.setText("");
            comboType.setSelectedIndex(0);
            spDate.setValue(InputKit.toDate(tomorrow));
            spFee.setValue(0);
            txtItemName.setText("");
            txtItemDesc.setText("");
            loadSuggestions.run();
            update.run();
        };

        btnConfirm.addActionListener(e -> {
            try {
                String name = InputKit.clean(txtName.getText());
                String type = (String) comboType.getSelectedItem();
                String venue = InputKit.clean(txtVenue.getText());
                LocalDate date = InputKit.toLocalDate((Date) spDate.getValue());
                String desc = InputKit.clean(txtDesc.getText());
                BigDecimal fee = new BigDecimal(((Number) spFee.getValue()).intValue());
                List<String[]> items = new ArrayList<>();
                for (int i = 0; i < itemsModel.getRowCount(); i++) {
                    items.add(new String[]{itemsModel.getValueAt(i, 0).toString(), itemsModel.getValueAt(i, 1).toString()});
                }
                ValidationResult vr = InputBarricade.validateFramework(ctx.user(), name, type, venue, date, desc, items, fee);
                if (!vr.valid) {
                    JOptionPane.showMessageDialog(this, vr.errorMessage, "Barricade Rejected Input (UC 1)", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                GroupLeader leader = (GroupLeader) ctx.user();
                if (ctx.facade.frameworkExists(leader.group, name, type, date, venue)) {
                    int c = JOptionPane.showOptionDialog(this,
                            "Alt Flow 2a: A framework with the same details already exists in your group.\nRe-enter the details or cancel the establishment?",
                            "Duplicate Framework", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                            new Object[]{"Re-enter details", "Cancel establishment"}, "Re-enter details");
                    if (c == 1) {
                        resetForm.run();
                        output.setText("Exception 3a: Group Leader cancelled the framework establishment.");
                    }
                    return;
                }
                EventBuilder builder = new EventBuilder().name(name).type(type).venue(venue).date(date)
                        .description(desc).fee(fee);
                for (String[] it : items) builder.addItem(it[0], it[1]);
                Event created = ctx.facade.establishFramework(leader, builder);

                StringBuilder sb = new StringBuilder();
                sb.append("EVENT FRAMEWORK ESTABLISHED\n")
                  .append("Event ID  : ").append(created.eventID).append('\n')
                  .append("Name      : ").append(created.eventName).append(" [").append(created.eventType).append("]\n")
                  .append("Location  : ").append(created.venue).append(", ").append(created.location.areaName).append('\n')
                  .append("Date      : ").append(created.eventDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append('\n')
                  .append("Fee       : Rs. ").append(created.eventFee.toPlainString()).append('\n')
                  .append("Items     : ");
                for (EventItem it : created.items) sb.append(it.itemName).append("; ");
                output.setText(sb.toString());
                ctx.refreshScreens();
                JOptionPane.showMessageDialog(this, "Event framework '" + created.eventName + "' established successfully.\nGroup members have been alerted.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                resetForm.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "System error: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this, "Discard this framework and clear the form?", "Cancel establishment", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                resetForm.run();
                output.setText("Exception 3a: Group Leader cancelled the framework establishment.");
            }
        });

        // layout
        int r = 0;
        InputKit.addRow(form, gbc, r++, "Group Leader [system retrieved]:", txtLeader);
        InputKit.addRow(form, gbc, r++, "Group [system retrieved]:", txtGroup);
        InputKit.addRow(form, gbc, r++, "Area [system retrieved]:", txtArea);
        JPanel namePanel = new JPanel(new BorderLayout(6, 0));
        namePanel.add(txtName, BorderLayout.CENTER);
        namePanel.add(lblName, BorderLayout.EAST);
        InputKit.addRow(form, gbc, r++, "Event name* (letters, digits, space, -):", namePanel);
        InputKit.addRow(form, gbc, r++, "Event type*:", comboType);
        JPanel venuePanel = new JPanel(new BorderLayout(6, 0));
        venuePanel.add(txtVenue, BorderLayout.CENTER);
        venuePanel.add(lblVenue, BorderLayout.EAST);
        InputKit.addRow(form, gbc, r++, "Event location (venue)*:", venuePanel);
        InputKit.addRow(form, gbc, r++, "Event date* (after today):", spDate);
        InputKit.addRow(form, gbc, r++, "Registration fee (Rs.):", spFee);
        InputKit.addRow(form, gbc, r++, "Description*:", new JScrollPane(txtDesc));
        InputKit.addRow(form, gbc, r++, "", lblDesc);

        JPanel itemEntry = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        itemEntry.add(new JLabel("Item:"));
        itemEntry.add(txtItemName);
        itemEntry.add(new JLabel("Details:"));
        itemEntry.add(txtItemDesc);
        itemEntry.add(btnAddItem);
        itemEntry.add(btnRemoveItem);
        InputKit.addRow(form, gbc, r++, "Event items* (suggested for the type):", itemEntry);
        JScrollPane itemScroll = new JScrollPane(itemsTable);
        itemScroll.setPreferredSize(new Dimension(300, 66));
        InputKit.addRow(form, gbc, r++, "", itemScroll);

        JPanel south = new JPanel(new BorderLayout(6, 6));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(btnCancel);
        btns.add(btnConfirm);
        south.add(lblHint, BorderLayout.NORTH);
        south.add(btns, BorderLayout.CENTER);
        south.add(new JScrollPane(output), BorderLayout.SOUTH);

        add(InputKit.topAnchored(form), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        ctx.addRefresher(() -> {
            boolean isLeader = ctx.user() instanceof GroupLeader && ((GroupLeader) ctx.user()).group != null
                    && ((GroupLeader) ctx.user()).group.isActive();
            allowed[0] = isLeader;
            if (ctx.user() instanceof GroupLeader) {
                GroupLeader gl = (GroupLeader) ctx.user();
                txtLeader.setText(gl.name + " (ID " + gl.userID + ")");
                txtGroup.setText(gl.group == null ? "None" : gl.group.groupName + " (ID " + gl.group.groupID + ", " + gl.group.groupStatus + ")");
                txtArea.setText(gl.group == null ? "None" : gl.group.area.areaName);
            } else {
                txtLeader.setText(ctx.user().name + " - not a Group Leader");
                txtGroup.setText("-");
                txtArea.setText("-");
            }
            for (Component c : form.getComponents()) {
                if (c != txtLeader && c != txtGroup && c != txtArea) InputKit.setEnabledRecursive(c, isLeader);
            }
            if (itemsModel.getRowCount() == 0) loadSuggestions.run();
            update.run();
        });
        ctx.refreshScreens();   // fill the screen for its acting user
    }

    User actingUser() {
        return ctx.user();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EstablishFrameworkScreen screen = new EstablishFrameworkScreen();
            ScreenWindow.show(screen, screen.actingUser(), "Eco-Drive - UC 1 - Establish a framework for an event");
        });
    }
}
