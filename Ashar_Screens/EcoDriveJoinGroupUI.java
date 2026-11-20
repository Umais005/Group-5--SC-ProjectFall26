import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.geom.Path2D;

public class EcoDriveJoinGroupUI extends JFrame {

    public EcoDriveJoinGroupUI() {
        setTitle("Eco-Drive - Join Group Screen (UI Mockup)");
        setSize(1020, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        EnvironmentalBackgroundPanel mainPanel = new EnvironmentalBackgroundPanel();
        mainPanel.setLayout(new BorderLayout(18, 18));
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        setContentPane(mainPanel);

        // 1. Top Header Bar
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Center Split Layout: Left = Area Groups List, Right = Role & Details Form
        JPanel centerContent = new JPanel(new GridLayout(1, 2, 20, 0));
        centerContent.setOpaque(false);

        centerContent.add(createGroupsListColumn());
        centerContent.add(createJoinFormColumn());

        mainPanel.add(centerContent, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = createTranslucentCard();
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(15, 22, 15, 22));

        JLabel title = new JLabel("Neighborhood Groups in Your Area: Sector F-10 (Area ID: 1012)");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(27, 94, 32));

        JLabel userBadge = new JLabel("Logged in: Ali Khan (User ID: 210045)");
        userBadge.setFont(new Font("SansSerif", Font.BOLD, 13));
        userBadge.setForeground(new Color(46, 125, 50));

        header.add(title, BorderLayout.WEST);
        header.add(userBadge, BorderLayout.EAST);
        return header;
    }

    // Left Column: Groups in the user's area
    private JPanel createGroupsListColumn() {
        JPanel container = createTranslucentCard();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel colTitle = new JLabel("Available Groups in Sector F-10");
        colTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        colTitle.setForeground(new Color(27, 94, 32));
        colTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        container.add(colTitle);
        container.add(Box.createRigidArea(new Dimension(0, 14)));

        container.add(createGroupItemCard(
            "Green Margalla (ID: 30211)",
            "Leader: Ashar Jabbar  |  Status: Active",
            "Native tree plantation drives and trail cleanups across Sector F-10.",
            true
        ));
        container.add(Box.createRigidArea(new Dimension(0, 12)));

        container.add(createGroupItemCard(
            "F-10 Eco Warriors (ID: 30212)",
            "Leader: Mohsi Mujahid  |  Status: Active",
            "Neighborhood recycling awareness and plastic-free community campaigns.",
            true
        ));
        container.add(Box.createRigidArea(new Dimension(0, 12)));

        container.add(createGroupItemCard(
            "F-10 Green Youth (ID: 30218)",
            "Leader: Bilal Tariq  |  Status: Locked",
            "Youth-led neighborhood conservation group.",
            false
        ));

        return container;
    }

    private JPanel createGroupItemCard(String name, String meta, String desc, boolean active) {
        JPanel item = new JPanel(new BorderLayout(10, 5));
        item.setBackground(Color.WHITE);
        item.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(165, 214, 167), 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setMaximumSize(new Dimension(500, 115));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        nameLbl.setForeground(new Color(27, 94, 32));

        JLabel metaLbl = new JLabel(meta);
        metaLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        metaLbl.setForeground(active ? new Color(56, 142, 60) : new Color(198, 40, 40));

        JLabel descLbl = new JLabel("<html><div style='width:240px;'>" + desc + "</div></html>");
        descLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descLbl.setForeground(new Color(80, 90, 80));

        textPanel.add(nameLbl);
        textPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        textPanel.add(metaLbl);
        textPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        textPanel.add(descLbl);

        JButton joinBtn = new JButton(active ? "Join" : "Locked");
        joinBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        joinBtn.setPreferredSize(new Dimension(90, 34));
        joinBtn.setBackground(active ? new Color(46, 125, 50) : new Color(210, 210, 210));
        joinBtn.setForeground(active ? Color.WHITE : new Color(110, 110, 110));
        joinBtn.setFocusPainted(false);
        joinBtn.setBorderPainted(false);
        joinBtn.setOpaque(true);
        joinBtn.setEnabled(active);

        JPanel btnWrapper = new JPanel(new GridBagLayout());
        btnWrapper.setOpaque(false);
        btnWrapper.add(joinBtn);

        item.add(textPanel, BorderLayout.CENTER);
        item.add(btnWrapper, BorderLayout.EAST);
        return item;
    }

    // Right Column: Role Selection & User Info Form (Use Case 5 Steps 2, 3, 4 & Alt Flow 2A)
    private JPanel createJoinFormColumn() {
        JPanel formCard = createTranslucentCard();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel formTitle = new JLabel("Join Selected Group: Green Margalla");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        formTitle.setForeground(new Color(27, 94, 32));
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Role Selection Box (Group Member vs Team Member)
        JPanel roleBox = new JPanel(new GridLayout(2, 1, 4, 4));
        roleBox.setOpaque(false);
        roleBox.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(129, 199, 132)),
            "Choose Role", TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12), new Color(46, 125, 50)
        ));
        roleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleBox.setMaximumSize(new Dimension(450, 75));

        JRadioButton groupMemberRadio = new JRadioButton("Group Member (Main Flow)", true);
        JRadioButton teamMemberRadio = new JRadioButton("Team Member (Alt Flow 2A - Schedules Interview)");
        groupMemberRadio.setOpaque(false);
        teamMemberRadio.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(groupMemberRadio);
        bg.add(teamMemberRadio);
        roleBox.add(groupMemberRadio);
        roleBox.add(teamMemberRadio);

        // Team Selection Dropdown (For Team Member Role)
        JLabel teamLabel = new JLabel("Select Team (If joining as Team Member):");
        teamLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        teamLabel.setForeground(new Color(46, 125, 50));
        teamLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComboBox<String> teamDropdown = new JComboBox<>(new String[]{
            "Logistics & Field Operations Team",
            "Media & Documentation Team",
            "Event Reporting Team"
        });
        teamDropdown.setMaximumSize(new Dimension(450, 32));
        teamDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Personal & Contact Details Inputs
        JPanel fieldsPanel = new JPanel(new GridLayout(6, 1, 0, 4));
        fieldsPanel.setOpaque(false);
        fieldsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldsPanel.setMaximumSize(new Dimension(450, 175));

        fieldsPanel.add(createFieldLabel("Full Name:"));
        fieldsPanel.add(new JTextField(""));
        fieldsPanel.add(createFieldLabel("Email Address (For confirmation / interview mail):"));
        fieldsPanel.add(new JTextField("ali.khan@example.com"));
        fieldsPanel.add(createFieldLabel("Phone Number:"));
        fieldsPanel.add(new JTextField("+923001234567"));

        // Submit Join Button
        JButton confirmBtn = new JButton("Confirm & Join Group");
        confirmBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        confirmBtn.setBackground(new Color(46, 125, 50));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFocusPainted(false);
        confirmBtn.setBorderPainted(false);
        confirmBtn.setOpaque(true);
        confirmBtn.setMaximumSize(new Dimension(450, 40));
        confirmBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        formCard.add(formTitle);
        formCard.add(Box.createRigidArea(new Dimension(0, 14)));
        formCard.add(roleBox);
        formCard.add(Box.createRigidArea(new Dimension(0, 10)));
        formCard.add(teamLabel);
        formCard.add(Box.createRigidArea(new Dimension(0, 4)));
        formCard.add(teamDropdown);
        formCard.add(Box.createRigidArea(new Dimension(0, 12)));
        formCard.add(fieldsPanel);
        formCard.add(Box.createRigidArea(new Dimension(0, 18)));
        formCard.add(confirmBtn);

        return formCard;
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(new Color(46, 125, 50));
        return lbl;
    }

    private JPanel createTranslucentCard() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    private static class EnvironmentalBackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g2.setPaint(new GradientPaint(0, 0, new Color(178, 223, 219), 0, h, new Color(232, 245, 233)));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(255, 249, 196, 180));
            g2.fillOval(w - 180, 40, 100, 100);

            drawHill(g2, w, h, 0.62, 70, new Color(129, 199, 132));
            drawHill(g2, w, h, 0.75, -50, new Color(76, 175, 80));
            drawHill(g2, w, h, 0.88, 40, new Color(46, 125, 50));
            g2.dispose();
        }

        private void drawHill(Graphics2D g2, int w, int h, double ratio, int offset, Color color) {
            Path2D path = new Path2D.Double();
            double base = h * ratio;
            path.moveTo(0, h);
            path.lineTo(0, base);
            path.curveTo(w * 0.33, base - offset, w * 0.66, base + offset, w, base - 20);
            path.lineTo(w, h);
            path.closePath();
            g2.setColor(color);
            g2.fill(path);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EcoDriveJoinGroupUI().setVisible(true));
    }
}