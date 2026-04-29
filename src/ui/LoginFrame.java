package ui;

import dao.AlertDAO;
import model.SecurityAlert;
import model.SecurityAlert.Severity;
import util.AlertFormatter;

import javax.swing.*;
// import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;

/**
 * Login window of the Cybersecurity Alert Analyzer.
 *
 * Demonstrates:
 *   - Java Swing layout (GridBagLayout)
 *   - {@link ActionListener} WITHOUT lambda (named inner class + anonymous)
 *   - WindowListener via {@link WindowAdapter}
 *   - KeyListener via {@link KeyAdapter}
 *   - JDBC validation through {@link AlertDAO}
 *   - Generation of an INVALID_LOGIN SecurityAlert when credentials fail
 */
public class LoginFrame extends JFrame {

    private final JTextField     txtUser   = Theme.textField(20);
    private final JPasswordField txtPass   = Theme.passwordField(20);
    private final JLabel         lblStatus = new JLabel(" ");
    private int wrongAttempts = 0;

    public LoginFrame() {
        super("CyberSecurity Alert Analyzer  -  Secure Login");
        Theme.styleFrame(this);
        buildUI();

        // Window event - WindowListener WITHOUT lambda (anonymous class)
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int r = JOptionPane.showConfirmDialog(LoginFrame.this,
                        "Exit application?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) System.exit(0);
            }
        });

        setSize(640, 540);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void buildUI() {
        // Painted background panel with subtle radial-style gradient
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x0A, 0x18, 0x2C),
                        getWidth(), getHeight(), Theme.BG);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setOpaque(false);
        setContentPane(root);

        // Card
        JPanel card = Theme.card(new GridBagLayout());
        card.setPreferredSize(new Dimension(420, 420));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        JLabel title = Theme.title("CYBER  SHIELD");
        JLabel sub   = new JLabel("Security Operations Center · Sign In");
        sub.setFont(Theme.SUBHEAD); sub.setForeground(Theme.TEXT_MUTED);

        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; g.anchor = GridBagConstraints.CENTER;
        card.add(title, g);
        g.gridy = 1; card.add(sub, g);

        g.gridwidth = 1; g.anchor = GridBagConstraints.WEST;

        JLabel lUser = Theme.body("Username"); lUser.setFont(Theme.BODY_BOLD);
        JLabel lPass = Theme.body("Password"); lPass.setFont(Theme.BODY_BOLD);

        g.gridx = 0; g.gridy = 2; card.add(lUser, g);
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2; card.add(txtUser, g);

        g.gridwidth = 1;
        g.gridx = 0; g.gridy = 4; card.add(lPass, g);
        g.gridx = 0; g.gridy = 5; g.gridwidth = 2; card.add(txtPass, g);

        JButton btnLogin  = Theme.primary("LOG IN");
        JButton btnCancel = Theme.button("CLEAR", Theme.WARN);

        // ActionListener WITHOUT lambda - named inner class
        btnLogin.addActionListener(new LoginAction());

        // ActionListener WITHOUT lambda - anonymous inner class
        btnCancel.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                txtUser.setText("");
                txtPass.setText("");
                lblStatus.setForeground(Theme.TEXT_MUTED);
                lblStatus.setText("Cleared.");
            }
        });

        JPanel btns = new JPanel(new GridLayout(1, 2, 12, 0));
        btns.setOpaque(false);
        btns.add(btnCancel);
        btns.add(btnLogin);
        g.gridx = 0; g.gridy = 6; g.gridwidth = 2;
        g.insets = new Insets(18, 8, 8, 8);
        card.add(btns, g);

        lblStatus.setFont(Theme.BODY);
        lblStatus.setForeground(new Color(0xFF, 0xB4, 0xB4));
        g.gridy = 7; g.insets = new Insets(8, 8, 0, 8);
        card.add(lblStatus, g);

        JLabel hint = new JLabel("Demo:  admin / admin123");
        hint.setFont(Theme.BODY);
        hint.setForeground(Theme.TEXT_MUTED);
        g.gridy = 8;
        card.add(hint, g);

        // ENTER submits (KeyListener without lambda - anonymous class)
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) btnLogin.doClick();
            }
        };
        txtUser.addKeyListener(enter);
        txtPass.addKeyListener(enter);

        root.add(card);
    }

    /**
     * Named inner class implementing {@link ActionListener} WITHOUT lambda.
     * Performs login validation and raises an INVALID_LOGIN alert on failure.
     */
    private class LoginAction implements ActionListener {
        @Override public void actionPerformed(ActionEvent e) {
            String user = txtUser.getText().trim();
            String pass = new String(txtPass.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                lblStatus.setText("Username and password are required.");
                return;
            }

            String role = null;
            AlertDAO dao = new AlertDAO();
            try {
                role = dao.validateLogin(user, pass);
            } catch (SQLException ex) {
                if ("admin".equals(user) && "admin123".equals(pass)) role = "ADMIN";
            }

            if (role == null) {
                wrongAttempts++;
                String raw = "ALERT: Suspicious login attempt detected for user '" + user + "'";
                SecurityAlert alert = new SecurityAlert(
                        wrongAttempts, "INVALID_LOGIN", raw,
                        wrongAttempts >= 3 ? Severity.CRITICAL : Severity.HIGH,
                        "127.0.0.1", user);

                try { dao.insert(alert); } catch (SQLException ignore) {}

                JOptionPane.showMessageDialog(LoginFrame.this,
                        AlertFormatter.formatDetail(alert),
                        "SECURITY ALERT  -  Invalid Login",
                        JOptionPane.ERROR_MESSAGE);

                lblStatus.setForeground(new Color(0xFF, 0x8A, 0x8A));
                lblStatus.setText("Invalid credentials.  Attempt #" + wrongAttempts + " of 3.");

                if (wrongAttempts >= 3) {
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Account locked after 3 failed attempts.\nApplication will exit.",
                            "Account Locked", JOptionPane.WARNING_MESSAGE);
                    System.exit(0);
                }
                return;
            }

            lblStatus.setForeground(Theme.OK);
            lblStatus.setText("Welcome, " + user + " (" + role + ")");
            DashboardFrame dash = new DashboardFrame(user, role);
            dash.setVisible(true);
            dispose();
        }
    }
}
