package ui;

import dao.AlertDAO;
import model.SecurityAlert;
import util.AlertFormatter;
import util.SecurityAlertComparator;
import util.SecurityAlertComparator.Direction;
import util.SecurityAlertComparator.Field;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Main dashboard shown after a successful login.
 *
 * Demonstrates:
 *   - Java Swing complex layout (BorderLayout + custom toolbar)
 *   - {@link java.awt.event.ActionListener} WITH lambda
 *   - {@link java.awt.event.ItemListener} WITH lambda (JCheckBox + JComboBox)
 *   - JDBC reads through {@link AlertDAO}
 *   - Launching specialized frames for individual alert categories
 */
public class DashboardFrame extends JFrame {

    private final String username;
    private final String role;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Time", "Severity", "Type", "Message", "Source IP", "User"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    private final JCheckBox chkOnlyCritical = new JCheckBox("Show only HIGH / CRITICAL");
    private final JComboBox<String> cmbType = new JComboBox<>(new String[]{
            "ALL", "INVALID_LOGIN", "SUSPICIOUS_LOGIN", "MALWARE", "NETWORK_INTRUSION",
            "FILE_ACCESS", "PRIVILEGE_ESCALATION", "DATA_EXFILTRATION", "DDOS"
    });
    private final JLabel lblCount = new JLabel(" ");

    // Sorting controls + the custom Comparator instance used by reload().
    private final JComboBox<Field> cmbSortField = new JComboBox<>(Field.values());
    private final JButton btnSortDir = Theme.button("↓  DESC", Theme.ACCENT_DARK);
    private final SecurityAlertComparator sorter =
            new SecurityAlertComparator(Field.TIME, Direction.DESC);

    public DashboardFrame(String username, String role) {
        super("CyberSecurity Alert Analyzer  -  Dashboard");
        this.username = username;
        this.role     = role;

        Theme.styleFrame(this);
        buildUI();
        reload();

        setSize(1280, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // ================== HEADER BAR ==================
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x0E, 0x2A, 0x47),
                        getWidth(), getHeight(), Theme.SURFACE_2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 70));
        header.setBorder(new EmptyBorder(10, 22, 10, 22));

        JLabel title = new JLabel("CYBER  SHIELD  ::  Security Operations Center");
        title.setFont(Theme.HEADING);
        title.setForeground(Theme.ACCENT);
        header.add(title, BorderLayout.WEST);

        JLabel who = new JLabel("● " + username + "  (" + role + ")   ");
        who.setFont(Theme.BODY_BOLD);
        who.setForeground(Theme.OK);
        header.add(who, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ================== TOOLBAR ==================
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Theme.SURFACE);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        JButton btnReload   = Theme.primary("Reload");
        JButton btnFileScan = Theme.button("File Scan",         Theme.ACCENT_DARK);
        JButton btnNetwork  = Theme.button("Network Monitor",   Theme.ACCENT_DARK);
        JButton btnMalware  = Theme.button("Malware Scanner",   Theme.ACCENT_DARK);
        JButton btnAwt      = Theme.button("AWT Live Monitor",  Theme.ACCENT_DARK);
        JButton btnCritical = Theme.danger("Critical Only");
        JButton btnDetails  = Theme.button("View Details",      Theme.WARN);
        JButton btnLogout   = Theme.danger("Logout");

        // ActionListener WITH lambda
        btnReload  .addActionListener(e -> reload());
        btnFileScan.addActionListener(e -> new FileScanFrame().setVisible(true));
        btnNetwork .addActionListener(e -> new NetworkAlertFrame().setVisible(true));
        btnMalware .addActionListener(e -> new MalwareScanFrame().setVisible(true));
        btnAwt     .addActionListener(e -> new AlertMonitorFrame().setVisible(true));
        btnCritical.addActionListener(e -> new CriticalAlertsFrame().setVisible(true));
        btnDetails .addActionListener(e -> showSelectedDetails());
        btnLogout  .addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });

        toolbar.add(btnReload);
        toolbar.add(makeSep());
        toolbar.add(btnFileScan);
        toolbar.add(btnNetwork);
        toolbar.add(btnMalware);
        toolbar.add(btnAwt);
        toolbar.add(makeSep());
        toolbar.add(btnCritical);
        toolbar.add(btnDetails);
        toolbar.add(makeSep());
        toolbar.add(btnLogout);

        add(toolbar, BorderLayout.BEFORE_FIRST_LINE.equals("") ? BorderLayout.SOUTH : BorderLayout.SOUTH);
        // (we'll attach toolbar properly in a wrapper below)
        remove(toolbar);

        // ================== CENTER (toolbar + table) ==================
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Theme.BG);
        center.setBorder(new EmptyBorder(8, 14, 8, 14));

        center.add(toolbar, BorderLayout.NORTH);

        Theme.styleTable(table);
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(Theme.SURFACE);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        center.add(sp, BorderLayout.CENTER);

        // ================== FILTER STRIP (south) ==================
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        south.setBackground(Theme.SURFACE_2);
        south.setBorder(new EmptyBorder(8, 16, 8, 16));

        JLabel filterLbl = Theme.body("Filter type:"); filterLbl.setFont(Theme.BODY_BOLD);
        cmbType.setFont(Theme.BODY);
        cmbType.setBackground(Theme.SURFACE);
        cmbType.setForeground(Theme.TEXT);

        chkOnlyCritical.setOpaque(false);
        chkOnlyCritical.setForeground(Theme.TEXT);
        chkOnlyCritical.setFont(Theme.BODY_BOLD);

        lblCount.setFont(Theme.BODY_BOLD);
        lblCount.setForeground(Theme.ACCENT);

        south.add(filterLbl);
        south.add(cmbType);
        south.add(chkOnlyCritical);

        // ----- sort controls -----
        JLabel sep = new JLabel(" │ "); sep.setForeground(Theme.BORDER); sep.setFont(Theme.HEADING);
        south.add(sep);
        JLabel sortLbl = Theme.body("Sort by:"); sortLbl.setFont(Theme.BODY_BOLD);
        cmbSortField.setSelectedItem(Field.TIME);
        cmbSortField.setFont(Theme.BODY);
        cmbSortField.setBackground(Theme.SURFACE);
        cmbSortField.setForeground(Theme.TEXT);
        south.add(sortLbl);
        south.add(cmbSortField);
        south.add(btnSortDir);

        south.add(Box.createHorizontalStrut(40));
        south.add(lblCount);

        // ItemListener WITH lambda - filter combo
        cmbType.addItemListener(ev -> {
            if (ev.getStateChange() == java.awt.event.ItemEvent.SELECTED) reload();
        });
        // ItemListener WITH lambda - checkbox
        chkOnlyCritical.addItemListener(ev -> reload());

        // ItemListener WITH lambda - sort field combo
        cmbSortField.addItemListener(ev -> {
            if (ev.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                sorter.setField((Field) cmbSortField.getSelectedItem());
                reload();
            }
        });
        // ActionListener WITH lambda - direction toggle button
        btnSortDir.addActionListener(e -> {
            sorter.toggleDirection();
            btnSortDir.setText(sorter.getDirection() == Direction.ASC ? "↑  ASC" : "↓  DESC");
            reload();
        });

        center.add(south, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
    }

    private JComponent makeSep() {
        JComponent sep = new JPanel();
        sep.setPreferredSize(new Dimension(1, 28));
        sep.setBackground(Theme.BORDER);
        return sep;
    }

    private void reload() {
        model.setRowCount(0);
        try {
            AlertDAO dao = new AlertDAO();
            List<SecurityAlert> data = chkOnlyCritical.isSelected()
                    ? dao.findCritical()
                    : dao.findAll();

            // ===== custom Comparator applied here =====
            data.sort(sorter);

            String typeFilter = (String) cmbType.getSelectedItem();
            int shown = 0;
            for (SecurityAlert a : data) {
                if (typeFilter != null && !"ALL".equals(typeFilter)
                        && !typeFilter.equalsIgnoreCase(a.getAlertType())) continue;
                model.addRow(new Object[]{
                        a.getAlertId(), a.getCreatedAt(), a.getSeverity(),
                        a.getAlertType(), a.getMessage(), a.getSourceIp(), a.getUsername()
                });
                shown++;
            }
            lblCount.setText("Showing " + shown + " alert(s)  ::  sorted by "
                    + sorter.getField() + " " + sorter.getDirection());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database unreachable: " + ex.getMessage(),
                    "JDBC Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showSelectedDetails() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.");
            return;
        }
        SecurityAlert a = new SecurityAlert();
        a.setAlertId   ((Integer) model.getValueAt(row, 0));
        a.setMessage   (String.valueOf(model.getValueAt(row, 4)));
        a.setAlertType (String.valueOf(model.getValueAt(row, 3)));
        a.setSourceIp  (String.valueOf(model.getValueAt(row, 5)));
        a.setUsername  (String.valueOf(model.getValueAt(row, 6)));
        try { a.setSeverity(SecurityAlert.Severity.valueOf(String.valueOf(model.getValueAt(row, 2)))); }
        catch (Exception ignore) {}
        JOptionPane.showMessageDialog(this,
                AlertFormatter.formatDetail(a),
                "Alert Details", JOptionPane.INFORMATION_MESSAGE);
    }
}
