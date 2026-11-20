import javax.swing.*;
import java.awt.*;

/** Opens one use-case screen in its own window (no tabs, no role switching). */
final class ScreenWindow {
    private static int opened = 0;

    private ScreenWindow() { }

    static JFrame show(JPanel screen, User user, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        String group = (user instanceof GroupMember && ((GroupMember) user).group != null)
                ? ((GroupMember) user).group.toString() : "None";
        JLabel banner = new JLabel("  Logged in (system retrieved) -> " + user.name + " | UserID: " + user.userID
                + " | Role: " + user.role + " | Group: " + group);
        banner.setFont(new Font("SansSerif", Font.BOLD, 12));
        banner.setOpaque(true);
        banner.setBackground(new Color(225, 240, 228));
        banner.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(46, 125, 50)));
        banner.setPreferredSize(new Dimension(0, 30));

        frame.add(banner, BorderLayout.NORTH);
        frame.add(screen, BorderLayout.CENTER);
        frame.setSize(1100, 780);
        int offset = (opened++ % 4) * 45;
        frame.setLocation(40 + offset, 30 + offset);
        frame.setVisible(true);
        return frame;
    }
}
