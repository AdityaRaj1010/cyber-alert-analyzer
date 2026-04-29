package ui;

import dao.AlertDAO;
import model.SecurityAlert;
import model.SecurityAlert.Severity;
import util.AlertExtractor;
import util.AlertFormatter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.Random;

/**
 * Heritage-AWT (Abstract Window Toolkit) live alert simulator.
 *
 * Demonstrates:
 *   - Pure Java AWT (Frame, Panel, Button, Choice, Checkbox, TextArea)
 *   - {@link ActionListener} WITHOUT lambda (named class)
 *   - {@link ItemListener}   WITHOUT lambda (named class)
 *   - {@link WindowAdapter}  for clean shutdown
 *   - Background Thread that fires fake alerts
 */
public class AlertMonitorFrame extends Frame {

    private static final Color BG       = new Color(0x0B, 0x14, 0x24);
    private static final Color SURFACE  = new Color(0x14, 0x1F, 0x36);
    private static final Color ACCENT   = new Color(0x22, 0xD3, 0xEE);
    private static final Color TEXT     = new Color(0xE6, 0xED, 0xF7);
    private static final Color CONSOLE_BG = new Color(0x07, 0x10, 0x1F);
    private static final Color CONSOLE_FG = new Color(0xB8, 0xF7, 0xC8);
    private static final Font  TITLE   = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font  BOLD    = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font  PLAIN   = new Font("Segoe UI", Font.PLAIN, 16);
    private static final Font  MONO    = new Font("Consolas", Font.PLAIN, 15);

    private final TextArea  console     = new TextArea(20, 90);
    private final Choice    chType      = new Choice();
    private final Checkbox  chkAuto     = new Checkbox("Auto-generate alerts every 3s", false);
    private final Button    btnGenerate = new Button("Generate Random Alert");
    private final Button    btnClear    = new Button("Clear");
    private final Button    btnClose    = new Button("Close");

    private volatile boolean autoOn = false;
    private Thread autoThread;

    public AlertMonitorFrame() {
        super("AWT  Live  Alert  Monitor");
        setLayout(new BorderLayout(8, 8));
        setBackground(BG);
        setForeground(TEXT);

        // ---------- HEADER ----------
        Panel header = new Panel(new BorderLayout());
        header.setBackground(SURFACE);
        Label title = new Label("  AWT  LIVE  MONITOR  ::  Background alert generator");
        title.setFont(TITLE);
        title.setForeground(ACCENT);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ---------- CONTROL ROW ----------
        Panel controls = new Panel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        controls.setBackground(SURFACE);

        Label lblType = new Label("Type:");
        lblType.setFont(BOLD); lblType.setForeground(TEXT);
        controls.add(lblType);

        chType.setFont(PLAIN);
        chType.setBackground(BG); chType.setForeground(TEXT);
        chType.add("INVALID_LOGIN");
        chType.add("MALWARE");
        chType.add("NETWORK_INTRUSION");
        chType.add("FILE_ACCESS");
        chType.add("PRIVILEGE_ESCALATION");
        chType.add("DATA_EXFILTRATION");
        chType.add("DDOS");
        controls.add(chType);

        styleButton(btnGenerate, ACCENT);
        styleButton(btnClear,    new Color(0xF5, 0x9E, 0x0B));
        styleButton(btnClose,    new Color(0xEF, 0x44, 0x44));

        chkAuto.setFont(BOLD);
        chkAuto.setBackground(SURFACE);
        chkAuto.setForeground(TEXT);

        controls.add(btnGenerate);
        controls.add(chkAuto);
        controls.add(btnClear);
        controls.add(btnClose);
        add(controls, BorderLayout.SOUTH);

        // ---------- CONSOLE ----------
        console.setEditable(false);
        console.setBackground(CONSOLE_BG);
        console.setForeground(CONSOLE_FG);
        console.setFont(MONO);
        add(console, BorderLayout.CENTER);

        // ---- ActionListener WITHOUT lambda - named inner classes ----
        btnGenerate.addActionListener(new GenerateAction());
        btnClear   .addActionListener(new ClearAction());
        btnClose   .addActionListener(new CloseAction());

        // ---- ItemListener WITHOUT lambda - named inner classes ----
        chkAuto.addItemListener(new AutoToggle());
        chType .addItemListener(new TypeChange());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                stopAuto();
                dispose();
            }
        });

        setSize(960, 580);
        setLocationRelativeTo(null);
    }

    private void styleButton(Button b, Color accent) {
        b.setFont(BOLD);
        b.setBackground(accent);
        b.setForeground(Color.BLACK);
    }

    // ----------------- listener classes (NOT lambdas) -----------------
    private class GenerateAction implements ActionListener {
        @Override public void actionPerformed(ActionEvent e) { fireRandomAlert(); }
    }
    private class ClearAction implements ActionListener {
        @Override public void actionPerformed(ActionEvent e) { console.setText(""); }
    }
    private class CloseAction implements ActionListener {
        @Override public void actionPerformed(ActionEvent e) { stopAuto(); dispose(); }
    }
    private class AutoToggle implements ItemListener {
        @Override public void itemStateChanged(ItemEvent e) {
            autoOn = (e.getStateChange() == ItemEvent.SELECTED);
            if (autoOn) startAuto(); else stopAuto();
        }
    }
    private class TypeChange implements ItemListener {
        @Override public void itemStateChanged(ItemEvent e) {
            console.append("[INFO] Now generating alerts of type "
                    + chType.getSelectedItem() + "\n");
        }
    }

    private void startAuto() {
        if (autoThread != null && autoThread.isAlive()) return;
        autoThread = new Thread(() -> {
            while (autoOn) {
                fireRandomAlert();
                try { Thread.sleep(3000); } catch (InterruptedException ignore) { return; }
            }
        }, "AlertSimulator");
        autoThread.setDaemon(true);
        autoThread.start();
    }
    private void stopAuto() {
        autoOn = false;
        if (autoThread != null) autoThread.interrupt();
    }

    private void fireRandomAlert() {
        String[] templates = {
            "ALERT: Suspicious login attempt detected",
            "ALERT: Malware Trojan.Gen identified in payload",
            "ALERT: Port scan from external host",
            "ALERT: Unauthorized file access on /etc/passwd",
            "ALERT: User attempted privilege escalation via sudo",
            "ALERT: Data exfiltration outbound to unknown IP",
            "ALERT: DDoS flood traffic on port 443"
        };
        Random rnd = new Random();
        String raw = templates[rnd.nextInt(templates.length)];

        String type = AlertExtractor.classifyAlertType(raw);
        Severity sev = type.equals("INVALID_LOGIN")          ? Severity.HIGH
                     : type.equals("MALWARE")                ? Severity.CRITICAL
                     : type.equals("DDOS")                   ? Severity.CRITICAL
                     : type.equals("NETWORK_INTRUSION")      ? Severity.CRITICAL
                     : type.equals("DATA_EXFILTRATION")      ? Severity.CRITICAL
                     : type.equals("PRIVILEGE_ESCALATION")   ? Severity.HIGH
                     : Severity.MEDIUM;

        SecurityAlert a = new SecurityAlert(0, type, raw, sev,
                "10.0.0." + rnd.nextInt(254), "system");

        try { new AlertDAO().insert(a); } catch (SQLException ignore) {}
        console.append(AlertFormatter.formatLine(a) + "\n");
    }
}
