import db.DBConnection;
import ui.LoginFrame;
import ui.Theme;
import util.AlertExtractor;
import util.AlertFormatter;
import model.SecurityAlert;
import model.SecurityAlert.Severity;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the desktop side of the Cybersecurity Alert Analyzer.
 *
 * On startup we:
 *   1. Run a quick smoke-test demonstrating the brief's required tasks
 *      (substring extraction, SecurityAlert class, list storage,
 *       critical-alert filtering, StringBuffer formatting).
 *   2. Confirm the JDBC connection.
 *   3. Open the Swing LoginFrame.
 */
public class Main {

    public static void main(String[] args) {
        Theme.install();   // dark theme + enlarged fonts (Theme.java)

        runStartupDemo();

        System.out.println("[DB] connection check: " + (DBConnection.ping() ? "OK" : "OFFLINE"));

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    /** Demonstrates each of the five tasks from the spec on startup. */
    private static void runStartupDemo() {
        System.out.println("===== Cybersecurity Alert Analyzer Startup Demo =====");

        // Task 1 - extract alert type via substring()
        String raw = "ALERT: Suspicious login attempt detected";
        String type = AlertExtractor.extractAlertType(raw);
        System.out.println("Task 1) substring -> alertType = " + type);

        // Task 2 - SecurityAlert class with alertId / alertType / message
        SecurityAlert a1 = new SecurityAlert(1, AlertExtractor.classifyAlertType(raw), raw,
                Severity.HIGH, "192.168.1.45", "raj");
        SecurityAlert a2 = new SecurityAlert(2, "MALWARE",
                "ALERT: Trojan.Gen detected", Severity.CRITICAL, "10.0.0.2", "system");
        SecurityAlert a3 = new SecurityAlert(3, "FILE_ACCESS",
                "ALERT: Unauthorized read on /etc/shadow", Severity.MEDIUM, "10.0.0.5", "guest");

        // Task 3 - store alerts in a suitable data structure (List)
        List<SecurityAlert> alerts = new ArrayList<>();
        alerts.add(a1); alerts.add(a2); alerts.add(a3);
        System.out.println("Task 3) stored " + alerts.size() + " alerts in ArrayList");

        // Task 4 - display critical alerts
        System.out.println("Task 4) critical alerts:");
        for (SecurityAlert a : alerts) {
            if (a.isCritical()) System.out.println("   * " + AlertFormatter.formatLine(a));
        }

        // Task 5 - StringBuffer formatted detail (formatDetail uses StringBuffer internally)
        System.out.println("Task 5) StringBuffer-formatted alert:");
        System.out.println(AlertFormatter.formatDetail(a1));
        System.out.println("=====================================================\n");
    }
}
