package ui;

import dao.AlertDAO;
import model.SecurityAlert;
import model.SecurityAlert.Severity;
import util.AlertFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;

/**
 * File Scan window - simulates an antivirus / file integrity scan and
 * raises FILE_ACCESS / MALWARE alerts on suspicious files.
 *
 * Demonstrates:
 *   - JFileChooser, JCheckBox, JComboBox
 *   - {@link java.awt.event.ActionListener} WITH lambda
 *   - {@link java.awt.event.ItemListener}  WITH lambda
 */
public class FileScanFrame extends JFrame {

    private final JTextField   txtPath = Theme.textField(40);
    private final JCheckBox    chkDeep = new JCheckBox("Deep Scan (recursive)");
    private final JCheckBox    chkLog  = new JCheckBox("Save alerts to DB", true);
    private final JComboBox<String> cmbScanType =
            new JComboBox<>(new String[]{"Quick Scan", "Full Scan", "Heuristic", "Signature-Based"});
    private final JTextArea    log     = new JTextArea(20, 70);

    public FileScanFrame() {
        super("File / Folder Scanner");
        Theme.styleFrame(this);
        buildUI();
        setSize(1000, 640);
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        add(Theme.headerBar("FILE  SCANNER  ::  Detect malware and unauthorized file access",
                Theme.ACCENT_DARK), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBackground(Theme.BG);
        center.setBorder(new EmptyBorder(14, 16, 14, 16));

        // ----- Row 1: Path + Browse -----
        JPanel pathRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        pathRow.setOpaque(false);
        JLabel lblPath = Theme.body("Path:"); lblPath.setFont(Theme.BODY_BOLD);
        pathRow.add(lblPath);
        pathRow.add(txtPath);
        JButton btnBrowse = Theme.button("Browse…", Theme.WARN);
        pathRow.add(btnBrowse);

        // ----- Row 2: Scan options + Start Scan -----
        JPanel optRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        optRow.setOpaque(false);
        JLabel lScanType = Theme.body("Scan Type:"); lScanType.setFont(Theme.BODY_BOLD);
        optRow.add(lScanType);
        cmbScanType.setBackground(Theme.SURFACE);
        cmbScanType.setForeground(Theme.TEXT);
        cmbScanType.setFont(Theme.BODY);
        optRow.add(cmbScanType);
        chkDeep.setOpaque(false); chkDeep.setForeground(Theme.TEXT); chkDeep.setFont(Theme.BODY_BOLD);
        chkLog .setOpaque(false); chkLog .setForeground(Theme.TEXT); chkLog .setFont(Theme.BODY_BOLD);
        optRow.add(chkDeep);
        optRow.add(chkLog);
        JButton btnScan = Theme.primary("Start Scan");
        optRow.add(btnScan);

        // BoxLayout so each row keeps its full preferred height
        // (GridLayout was forcing equal rows and clipping the wrapped button).
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        pathRow.setAlignmentX(LEFT_ALIGNMENT);
        optRow .setAlignmentX(LEFT_ALIGNMENT);
        north.add(pathRow);
        north.add(optRow);

        center.add(north, BorderLayout.NORTH);

        // ----- log area -----
        log.setEditable(false);
        log.setFont(Theme.CONSOLE);
        log.setBackground(new Color(0x07, 0x10, 0x1F));
        log.setForeground(new Color(0xB8, 0xF7, 0xC8));
        log.setCaretColor(Theme.ACCENT);
        log.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane sp = new JScrollPane(log);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        center.add(sp, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        // ActionListener WITH lambda
        btnBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtPath.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        btnScan.addActionListener(e -> performScan());

        // ItemListener WITH lambda
        chkDeep.addItemListener(ev ->
                appendLine("[i] Deep scan = " + chkDeep.isSelected(), Theme.ACCENT));
        cmbScanType.addItemListener(ev -> {
            if (ev.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                appendLine("[i] Mode switched -> " + cmbScanType.getSelectedItem(), Theme.ACCENT);
            }
        });
    }

    private void appendLine(String s, Color c) {
        log.append(s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void performScan() {
        String path = txtPath.getText().trim();
        if (path.isEmpty()) { JOptionPane.showMessageDialog(this, "Please pick a file or folder first."); return; }
        File root = new File(path);
        if (!root.exists()) {
            JOptionPane.showMessageDialog(this, "Path does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        log.append(">>> Starting " + cmbScanType.getSelectedItem() + " on " + root + "\n");
        scan(root, 0);
        log.append("<<< Scan completed.\n\n");
    }

    private void scan(File f, int depth) {
        if (depth > 6) return;
        String name = f.getName().toLowerCase();
        boolean malware = name.endsWith(".exe") || name.endsWith(".bat")
                       || name.endsWith(".ps1") || name.endsWith(".vbs");
        boolean sensitive = name.contains("passwd") || name.contains("shadow")
                         || name.contains("secret") || name.contains("token");

        if (malware) raise("MALWARE", Severity.CRITICAL,
                "ALERT: Suspicious executable detected at " + f.getAbsolutePath());
        else if (sensitive) raise("FILE_ACCESS", Severity.HIGH,
                "ALERT: Sensitive file scanned " + f.getAbsolutePath());
        else log.append("  ok  " + f.getAbsolutePath() + "\n");

        if (f.isDirectory() && (chkDeep.isSelected() || depth == 0)) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) scan(k, depth + 1);
        }
    }

    private void raise(String type, Severity sev, String message) {
        SecurityAlert a = new SecurityAlert(0, type, message, sev, "127.0.0.1", "scanner");
        log.append("** " + AlertFormatter.formatLine(a) + "\n");
        if (chkLog.isSelected()) {
            try { new AlertDAO().insert(a); } catch (SQLException ignore) {}
        }
    }
}
