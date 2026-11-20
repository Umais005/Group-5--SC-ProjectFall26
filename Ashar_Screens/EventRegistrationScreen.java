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

/** UC 10 - Registration for an event (Group Member). */
public class EventRegistrationScreen extends JPanel {
    private final AppContext ctx = new AppContext();   // own data: not shared with the other screens

    public EventRegistrationScreen() {
        super(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        ctx.setUser(ctx.db.member);
                        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("UC 10: Registration for an event  (actor: Group Member)"));
        GridBagConstraints gbc = InputKit.newGbc();

        JTextField txtMember = InputKit.readOnly("", 28), txtGroup = InputKit.readOnly("", 28);
        DefaultComboBoxModel<Event> eventModel = new DefaultComboBoxModel<>();
        JComboBox<Event> comboEvent = new JComboBox<>(eventModel);
        comboEvent.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                String text = "";
                if (value instanceof Event) {
                    Event ev = (Event) value;
                    text = ev.eventID + " - " + ev.eventName + " (" + ev.eventDate + ")";
                    if (ctx.user() instanceof GroupMember && ev.isRegisteredBy((GroupMember) ctx.user())) text += " - already registered";
                    else if (!ev.canRegister()) text += " - registration closed";
                }
                return super.getListCellRendererComponent(list, text, index, sel, focus);
            }
        });
        JTextField txtFee = InputKit.readOnly("", 28);

        JTextField txtEmail = InputKit.filteredField(28, InputKit.EMAIL_50);
        JTextField txtPhone = InputKit.filteredField(28, InputKit.PHONE_13);
        JComboBox<String> comboMethod = new JComboBox<>(new String[]{Status.CARD, Status.CASH});

        JTextField txtCard = InputKit.filteredField(18, InputKit.DIGITS_16);
        JComboBox<Integer> comboYear = new JComboBox<>();
        JComboBox<Integer> comboMonth = new JComboBox<>();
        JPasswordField txtCvv = new JPasswordField(4);
        ((AbstractDocument) txtCvv.getDocument()).setDocumentFilter(new InputKit.RegexFilter(InputKit.DIGITS_3));
        int thisYear = YearMonth.now().getYear();
        for (int y = thisYear; y <= thisYear + 10; y++) comboYear.addItem(y);
        Runnable fillMonths = () -> {
            Integer y = (Integer) comboYear.getSelectedItem();
            Integer keep = (Integer) comboMonth.getSelectedItem();
            comboMonth.removeAllItems();
            int from = (y != null && y == thisYear) ? YearMonth.now().getMonthValue() : 1;
            for (int m = from; m <= 12; m++) comboMonth.addItem(m);
            if (keep != null && keep >= from) comboMonth.setSelectedItem(keep);
        };
        comboYear.addActionListener(e -> fillMonths.run());
        fillMonths.run();
        JPanel cardPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        cardPanel.add(new JLabel("Expiry month:"));
        cardPanel.add(comboMonth);
        cardPanel.add(new JLabel("year:"));
        cardPanel.add(comboYear);
        cardPanel.add(new JLabel("CVV:"));
        cardPanel.add(txtCvv);

        JLabel lblHint = InputKit.noteLabel();
        JButton btnRegister = new JButton("Register");
        btnRegister.setFont(new Font("SansSerif", Font.BOLD, 13));
        JTextArea output = new JTextArea(6, 50);
        output.setEditable(false);
        output.setBorder(new TitledBorder("Output: registration number, payment receipt, confirmation"));
        final boolean[] allowed = {false};

        // ----- state: which payment fields apply and whether the form is valid -----
        Runnable update = new Runnable() {
            public void run() {
                Event ev = (Event) comboEvent.getSelectedItem();
                boolean paid = ev != null && ev.eventFee.signum() > 0;
                boolean card = paid && Status.CARD.equals(comboMethod.getSelectedItem());
                txtFee.setText(ev == null ? "" : (paid ? "Rs. " + ev.eventFee.toPlainString() : "Free event - no payment needed"));
                InputKit.enable(comboMethod, allowed[0] && paid);
                InputKit.enable(txtCard, allowed[0] && card);
                InputKit.enable(comboMonth, allowed[0] && card);
                InputKit.enable(comboYear, allowed[0] && card);
                InputKit.enable(txtCvv, allowed[0] && card);
                btnRegister.setText(!paid ? "Register" : (card ? "Pay and register" : "Register (pay on arrival)"));

                String hint = null;
                String email = txtEmail.getText().trim(), phone = txtPhone.getText().trim();
                GroupMember m = ctx.user() instanceof GroupMember ? (GroupMember) ctx.user() : null;
                if (!allowed[0]) hint = "Only a Group Member of a group can register for events.";
                else if (ev == null) hint = "Next: select an event.";
                else if (m != null && ev.isRegisteredBy(m)) hint = "You are already registered for this event.";
                else if (!ev.canRegister()) hint = "Exception 1a: Registration for this event is closed.";
                else if (email.isEmpty() && phone.isEmpty()) hint = "Next: enter an email or a phone number for the confirmation.";
                else if (!email.isEmpty() && !InputKit.EMAIL_FINAL.matcher(email).matches()) hint = "Next: complete the email address (name@domain.com).";
                else if (!phone.isEmpty() && !InputKit.PHONE_FINAL.matcher(phone).matches()) hint = "Next: phone needs 10-13 digits (optional leading +).";
                else if (card) {
                    String num = txtCard.getText();
                    if (num.length() != 16) hint = "Next: enter the 16-digit card number.";
                    else if (!InputKit.luhnOk(num)) hint = "The card number fails the check-digit test (test card: 4242424242424242).";
                    else if (comboMonth.getSelectedItem() == null) hint = "Next: choose the card expiry.";
                    else if (txtCvv.getPassword().length != 3) hint = "Next: enter the 3-digit CVV.";
                }
                btnRegister.setEnabled(allowed[0] && hint == null);
                lblHint.setForeground(hint == null ? new Color(27, 94, 32) : new Color(160, 60, 0));
                lblHint.setText(hint == null ? "All inputs are valid. Click the button to register." : hint);
            }
        };

        comboEvent.addActionListener(e -> update.run());
        comboMethod.addActionListener(e -> update.run());
        comboMonth.addActionListener(e -> update.run());
        comboYear.addActionListener(e -> update.run());
        InputKit.onChange(txtEmail, update);
        InputKit.onChange(txtPhone, update);
        InputKit.onChange(txtCard, update);
        InputKit.onChange(txtCvv, update);

        btnRegister.addActionListener(e -> {
            try {
                Event ev = (Event) comboEvent.getSelectedItem();
                String method = ev != null && ev.eventFee.signum() > 0 ? (String) comboMethod.getSelectedItem() : null;
                String cvv = new String(txtCvv.getPassword());
                Integer month = (Integer) comboMonth.getSelectedItem();
                Integer year = (Integer) comboYear.getSelectedItem();
                YearMonth expiry = (month == null || year == null) ? null : YearMonth.of(year, month);
                ValidationResult vr = InputBarricade.validateRegistration(ctx.user(), ev, method, txtCard.getText(),
                        expiry, cvv, txtEmail.getText(), txtPhone.getText());
                if (!vr.valid) {
                    JOptionPane.showMessageDialog(this, vr.errorMessage, "Barricade Rejected Registration (UC 10)", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                CardDetails card = Status.CARD.equals(method)
                        ? new CardDetails(txtCard.getText(), expiry.toString(), cvv) : null;
                Registration reg;
                try {
                    reg = ctx.facade.registerForEvent((GroupMember) ctx.user(), ev, method, card,
                            txtEmail.getText().trim(), txtPhone.getText().trim());
                } catch (PaymentFailedException pf) {
                    output.setText("Exception 3a: " + pf.getMessage() + "\nRegistration was NOT completed. Try another card or choose cash on arrival.");
                    JOptionPane.showMessageDialog(this, pf.getMessage() + "\nRegistration was not completed.", "Payment Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                output.setText(reg.getReceipt() + "\nConfirmation sent via: " + reg.confirmationSentVia);
                txtCard.setText("");
                txtCvv.setText("");
                JOptionPane.showMessageDialog(this, "Registration successful.\nRegistration number: " + reg.registrationNo
                        + "\nConfirmation sent via " + reg.confirmationSentVia + ".", "Success", JOptionPane.INFORMATION_MESSAGE);
                comboEvent.repaint();
                update.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "System error: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        int r = 0;
        InputKit.addRow(form, gbc, r++, "Member [system retrieved]:", txtMember);
        InputKit.addRow(form, gbc, r++, "Group [system retrieved]:", txtGroup);
        InputKit.addRow(form, gbc, r++, "Event* (open for registration):", comboEvent);
        InputKit.addRow(form, gbc, r++, "Amount due [system calculated]:", txtFee);
        InputKit.addRow(form, gbc, r++, "Email (optional if phone given):", txtEmail);
        InputKit.addRow(form, gbc, r++, "Phone (optional if email given):", txtPhone);
        InputKit.addRow(form, gbc, r++, "Payment method*:", comboMethod);
        InputKit.addRow(form, gbc, r++, "Card number (16 digits):", txtCard);
        InputKit.addRow(form, gbc, r++, "Card expiry and security:", cardPanel);
        JLabel testNote = new JLabel("Test card: 4242424242424242 (any later expiry, any 3-digit CVV)");
        testNote.setFont(new Font("SansSerif", Font.ITALIC, 11));
        JCheckBox chkFunds = new JCheckBox("Simulate insufficient funds (Exception 3a)");
        chkFunds.addActionListener(e -> ThirdPartyPaymentGateway.simulateInsufficientFunds = chkFunds.isSelected());
        JPanel demoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        demoPanel.add(testNote);
        demoPanel.add(chkFunds);
        InputKit.addRow(form, gbc, r++, "Demo helpers:", demoPanel);

        JPanel south = new JPanel(new BorderLayout(6, 6));
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.add(btnRegister);
        south.add(lblHint, BorderLayout.NORTH);
        south.add(bp, BorderLayout.CENTER);
        south.add(new JScrollPane(output), BorderLayout.SOUTH);

        add(InputKit.topAnchored(form), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        ctx.addRefresher(() -> {
            boolean member = ctx.user() instanceof GroupMember && ((GroupMember) ctx.user()).group != null;
            allowed[0] = member;
            eventModel.removeAllElements();
            if (member) {
                GroupMember gm = (GroupMember) ctx.user();
                txtMember.setText(gm.name + " (ID " + gm.userID + ")");
                txtGroup.setText(gm.group.groupName);
                txtEmail.setText(gm.getEmail());          // pre-filled from the profile (avoid retyping)
                txtPhone.setText(gm.phoneNo);
                for (Event ev : ctx.facade.getEventsForGroup(gm.group)) {
                    if (!ev.hasEnded()) eventModel.addElement(ev);
                }
            } else {
                txtMember.setText(ctx.user().name + " - not a Group Member");
                txtGroup.setText("-");
                txtEmail.setText("");
                txtPhone.setText("");
            }
            comboEvent.setEnabled(member);
            InputKit.enable(txtEmail, member);
            InputKit.enable(txtPhone, member);
            output.setText("");
            update.run();
        });
        ctx.refreshScreens();   // fill the screen for its acting user
    }

    User actingUser() {
        return ctx.user();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EventRegistrationScreen screen = new EventRegistrationScreen();
            ScreenWindow.show(screen, screen.actingUser(), "Eco-Drive - UC 10 - Registration for an event");
        });
    }
}
