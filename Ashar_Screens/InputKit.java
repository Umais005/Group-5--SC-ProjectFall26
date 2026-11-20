import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Shared input-design helpers for all Eco-Drive screens (Lecture 17).
 * Field rules come from the Data Dictionary; the patterns are applied to the WHOLE text after every
 * keystroke, so a keystroke that would break a rule is rejected and invalid data can never be typed.
 */
final class InputKit {
    private InputKit() { }

    static final String NAME_50 = "^(?! )(?!.*  )[A-Za-z0-9 \\-]{0,50}$";
    static final String ITEM_NAME_30 = "^(?! )(?!.*  )[A-Za-z0-9 \\-]{0,30}$";
    static final String VENUE_60 = "^(?! )(?!.*  )[A-Za-z0-9 ,.()\\-]{0,60}$";
    static final String TEXT_100 = "^(?! )[\\x20-\\x7E]{0,100}$";
    static final String TEXT_300 = "^(?! )[\\x20-\\x7E]{0,300}$";
    static final String EMAIL_50 = "^(?!.*@.*@)[A-Za-z0-9+_.@\\-]{0,50}$";
    static final String PHONE_13 = "^(\\+[0-9]{0,12}|[0-9]{0,13})$";
    static final String DIGITS_16 = "^[0-9]{0,16}$";
    static final String DIGITS_3 = "^[0-9]{0,3}$";

    static final Pattern EMAIL_FINAL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    static final Pattern PHONE_FINAL = Pattern.compile("^\\+?[0-9]{10,13}$");

    static class RegexFilter extends DocumentFilter {
        private final Pattern allowed;

        RegexFilter(String regex) {
            this.allowed = Pattern.compile(regex);
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            Document doc = fb.getDocument();
            String current = doc.getText(0, doc.getLength());
            String candidate = current.substring(0, offset) + (text == null ? "" : text) + current.substring(offset + length);
            if (allowed.matcher(candidate).matches()) {
                super.replace(fb, offset, length, text, attrs);
            } else {
                Toolkit.getDefaultToolkit().beep();         // keystroke rejected
            }
        }
    }

    static JTextField filteredField(int columns, String regex) {
        JTextField f = new JTextField(columns);
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new RegexFilter(regex));
        return f;
    }

    static JTextArea filteredArea(int rows, int cols, String regex) {
        JTextArea a = new JTextArea(rows, cols);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        ((AbstractDocument) a.getDocument()).setDocumentFilter(new RegexFilter(regex));
        return a;
    }

    static JTextField readOnly(String text, int columns) {
        JTextField f = new JTextField(text, columns);
        f.setEditable(false);
        f.setBackground(new Color(235, 235, 235));
        return f;
    }

    static void onChange(JTextComponent c, Runnable r) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    static JLabel counter(JTextComponent c, int max) {
        JLabel l = new JLabel();
        Runnable r = () -> {
            int len = c.getText().trim().length();
            l.setText("Characters: " + len + " / " + max);
        };
        onChange(c, r);
        r.run();
        return l;
    }

    static String clean(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    static boolean luhnOk(String number) {
        int sum = 0;
        boolean alt = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int d = number.charAt(i) - '0';
            if (alt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /** Enables or disables a component; typed fields are also greyed so the state is visible. */
    static void enable(Component c, boolean on) {
        c.setEnabled(on);
        if (c instanceof JTextComponent && ((JTextComponent) c).isEditable()) {
            c.setBackground(on ? Color.WHITE : new Color(235, 235, 235));
        }
    }

    static void setEnabledRecursive(Component c, boolean enabled) {
        enable(c, enabled);
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                setEnabledRecursive(child, enabled);
            }
        }
    }

    static void addRow(JPanel p, GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        p.add(l, gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        p.add(field, gbc);
    }

    static GridBagConstraints newGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 8, 3, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    static JLabel noteLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        return l;
    }

    /** Keeps a form at the top of its tab instead of stretching it over the free space. */
    static JScrollPane topAnchored(JPanel form) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(form, BorderLayout.NORTH);
        return new JScrollPane(wrap);
    }

}
