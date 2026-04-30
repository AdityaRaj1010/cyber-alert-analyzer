package ui;

import dao.AlertDAO;
import model.SecurityAlert;
import model.SecurityAlert.Severity;
import util.AlertFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Random;

/**
 * Network Monitor window - simulates traffic and raises NETWORK_INTRUSION,
 * DDOS or DATA_EXFILTRATION alerts when thresholds are exceeded.
 *
 * Demonstrates a mix of:
 *   - {@link ActionListener} WITHOUT lambda (anonymous class on btnStart)
 *   - {@link ActionListener} WITH lambda    (btnStop, simulators)
 *   - JSpinner, JProgressBar
 *   - Background simulation thread
 */
public class NetworkAlertFrame extends JFrame {

    private final JSpinner    spnThreshold = new JSpinner(new SpinnerNumberModel(1000, 100, 100000, 100));
    private final JProgressBar bar         = new JProgressBar(0, 5000);
    private final JTextArea   console      = new JTextArea(20, 80);

    private volatile boolean running;
    private Thread sim;

    public NetworkAlertFrame() {
        super("Network Intrusion Monitor");
        Theme.styleFrame(this);
        buildUI();
        setSize(1100, 640);
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        add(Theme.headerBar("NETWORK  MONITOR  ::  Detect intrusions, DDoS and exfiltration",
                Theme.CRITICAL), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBackground(Theme.BG);
        center.setBorder(new EmptyBorder(14, 16, 14, 16));

        // ---- Row 1: threshold + Start/Stop ----
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        topRow.setOpaque(false);
        JLabel l = Theme.body("DDoS req/sec threshold:"); l.setFont(Theme.BODY_BOLD);
        topRow.add(l);

        spnThreshold.setFont(Theme.BODY);
        ((JSpinner.DefaultEditor) spnThreshold.getEditor()).getTextField().setBackground(Theme.SURFACE);
        ((JSpinner.DefaultEditor) spnThreshold.getEditor()).getTextField().setForeground(Theme.TEXT);
        topRow.add(spnThreshold);

        JButton btnStart = Theme.primary("Start Monitor");
        JButton btnStop  = Theme.button("Stop", Theme.WARN);
        JButton btnPing  = Theme.danger("Simulate Port Scan");
        JButton btnDdos  = Theme.danger("Simulate DDoS");
        JButton btnExfil = Theme.danger("Simulate Data Exfiltration");

        // ActionListener WITHOUT lambda (anonymous inner class)
        btnStart.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { start(); }
        });
        // ActionListener WITH lambda
        btnStop .addActionListener(e -> stop());
        btnPing .addActionListener(e -> raise("NETWORK_INTRUSION", Severity.CRITICAL,
                "ALERT: Port scan detected on TCP/22 from external host"));
        btnDdos .addActionListener(e -> raise("DDOS", Severity.CRITICAL,
                "ALERT: DDoS-like traffic burst 50000 req/sec on /login"));
        btnExfil.addActionListener(e -> raise("DATA_EXFILTRATION", Severity.CRITICAL,
                "ALERT: 2.4GB outbound transfer to 203.0.113.99"));

        topRow.add(btnStart);
        topRow.add(btnStop);

        // ---- Row 2: simulation buttons ----
        JPanel simRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        simRow.setOpaque(false);
        simRow.add(btnPing);
        simRow.add(btnDdos);
        simRow.add(btnExfil);

        // BoxLayout so each row keeps its full preferred height.
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);
        topRow.setAlignmentX(LEFT_ALIGNMENT);
        simRow.setAlignmentX(LEFT_ALIGNMENT);
        north.add(topRow);
        north.add(simRow);

        center.add(north, BorderLayout.NORTH);

        // ---- console ----
        console.setEditable(false);
        console.setFont(Theme.CONSOLE);
        console.setBackground(new Color(0x07, 0x10, 0x1F));
        console.setForeground(new Color(0xB8, 0xF7, 0xC8));
        console.setCaretColor(Theme.ACCENT);
        console.setBorder(new EmptyBorder(10, 12, 10, 12));
        JScrollPane sp = new JScrollPane(console);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        center.add(sp, BorderLayout.CENTER);

        // ---- progress bar ----
        bar.setStringPainted(true);
        bar.setForeground(Theme.DANGER);
        bar.setBackground(Theme.SURFACE);
        bar.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        bar.setFont(Theme.BODY_BOLD);
        bar.setPreferredSize(new Dimension(0, 28));
        center.add(bar, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
    }

    private void start() {
        if (running) return;
        running = true;
        console.append("=== Monitoring started, threshold "
                + spnThreshold.getValue() + " req/s ===\n");
        sim = new Thread(() -> {
            Random rnd = new Random();
            while (running) {
                int reqs = rnd.nextInt(5000);
                bar.setValue(reqs);
                bar.setString(reqs + " req/sec");
                int threshold = (Integer) spnThreshold.getValue();
                if (reqs > threshold) {
                    raise("DDOS", Severity.CRITICAL,
                            "ALERT: DDoS-like traffic burst " + reqs + " req/sec");
                }
                try { Thread.sleep(800); } catch (InterruptedException ie) { return; }
            }
        }, "NetMonitor");
        sim.setDaemon(true);
        sim.start();
    }

    private void stop() {
        running = false;
        if (sim != null) sim.interrupt();
        console.append("=== Monitoring stopped ===\n");
    }

    private void raise(String type, Severity sev, String message) {
        SecurityAlert a = new SecurityAlert(0, type, message, sev,
                "203.0.113." + new Random().nextInt(254), "n/a");
        try { new AlertDAO().insert(a); } catch (SQLException ignore) {}
        console.append("!! " + AlertFormatter.formatLine(a) + "\n");
        console.setCaretPosition(console.getDocument().getLength());

        if (sev == Severity.CRITICAL) {
            JOptionPane.showMessageDialog(this,
                    AlertFormatter.formatDetail(a),
                    "CRITICAL ALERT", JOptionPane.ERROR_MESSAGE);
        }
    }
}
