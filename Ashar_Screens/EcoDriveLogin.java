import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;

public class EcoDriveLogin extends JFrame {

    public EcoDriveLogin() {
        setTitle("Eco-Drive - Login");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Environmental background container
        EnvironmentalBackgroundPanel backgroundPanel = new EnvironmentalBackgroundPanel();
        backgroundPanel.setLayout(new GridBagLayout());
        setContentPane(backgroundPanel);

        // Central translucent login card
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(35, 45, 35, 45));
        card.setPreferredSize(new Dimension(440, 470));

        // 1. Welcome Heading
        JLabel titleLabel = new JLabel("Welcome to Eco Drive");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(new Color(27, 94, 32));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 2. Platform Description
        JLabel descLabel = new JLabel(
            "<html><div style='text-align: center; width: 280px;'>"
            + "Organize neighborhood tree plantations and cleanups, join local eco-groups, "
            + "and track your community's environmental impact."
            + "</div></html>"
        );
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        descLabel.setForeground(new Color(60, 80, 60));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 3. Input Fields (Email & Password)
        JPanel formPanel = new JPanel(new GridLayout(4, 1, 0, 6));
        formPanel.setOpaque(false);
        formPanel.setMaximumSize(new Dimension(350, 140));

        JLabel emailLabel = new JLabel("Email Address:");
        emailLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        emailLabel.setForeground(new Color(46, 125, 50));

        JTextField emailField = new JTextField();
        emailField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        emailField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(129, 199, 132), 1, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        passwordLabel.setForeground(new Color(46, 125, 50));

        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(129, 199, 132), 1, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        formPanel.add(emailLabel);
        formPanel.add(emailField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        // 4. Buttons (Visual Only - No ActionListeners)
        JButton loginButton = createButton("Login", new Color(46, 125, 50), Color.WHITE);
        loginButton.setMaximumSize(new Dimension(350, 42));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel secondaryButtonsPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        secondaryButtonsPanel.setOpaque(false);
        secondaryButtonsPanel.setMaximumSize(new Dimension(350, 38));

        JButton signupButton = createButton("Sign Up", new Color(102, 187, 106), Color.WHITE);
        JButton helpButton = createButton("Help", new Color(200, 230, 201), new Color(27, 94, 32));

        secondaryButtonsPanel.add(signupButton);
        secondaryButtonsPanel.add(helpButton);

        // Assemble Card
        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(descLabel);
        card.add(Box.createRigidArea(new Dimension(0, 25)));
        card.add(formPanel);
        card.add(Box.createRigidArea(new Dimension(0, 22)));
        card.add(loginButton);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(secondaryButtonsPanel);

        backgroundPanel.add(card);
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        return btn;
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
            g2.fillOval(w - 220, 60, 110, 110);

            drawHill(g2, w, h, 0.55, 80, new Color(129, 199, 132));
            drawHill(g2, w, h, 0.68, -60, new Color(76, 175, 80));
            drawHill(g2, w, h, 0.82, 50, new Color(46, 125, 50));
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
        SwingUtilities.invokeLater(() -> new EcoDriveLogin().setVisible(true));
    }
}