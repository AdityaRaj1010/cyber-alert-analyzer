package servlet;

import dao.AlertDAO;
import model.SecurityAlert;
import model.SecurityAlert.Severity;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Random;

/**
 * Web-based replacement for the desktop simulators (NetworkAlertFrame,
 * MalwareScanFrame, FileScanFrame, AlertMonitorFrame).
 *
 * GET  /simulate          -> renders the control panel
 * POST /simulate?...      -> creates one or more SecurityAlerts via JDBC
 *                            and redirects back with a flash message.
 *
 * Every action below ultimately calls {@link AlertDAO#insert} just like
 * the desktop frames do, so the database, the dashboard table and the
 * /alerts servlet all see the new rows immediately.
 */
@WebServlet("/simulate")
public class SimulateAlertServlet extends HttpServlet {

    private static final Random R = new Random();
    private static final String[] THREATS = {
            "Trojan.Generic", "Worm.AutoRun.A", "Backdoor.Mirai",
            "Ransomware.Locky", "Spyware.Keylog", "Adware.Bundle",
            "Rootkit.Necurs",  "Trojan.Emotet"
    };

    /* ============================== GET ============================== */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect("login"); return;
        }

        String msg  = req.getParameter("msg");
        String user = (String) s.getAttribute("user");
        String role = (String) s.getAttribute("role");

        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter w = resp.getWriter();

        w.println("<!doctype html><html><head><title>Cyber Shield - Simulator</title>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "*{box-sizing:border-box}"
                + "body{font-family:'Segoe UI',Arial,sans-serif;font-size:17px;"
                + "  background:#0B1424;color:#E6EDF7;margin:0;min-height:100vh;}"
                + "header{background:linear-gradient(90deg,#0E2A47,#1B2944);"
                + "  padding:22px 32px;display:flex;justify-content:space-between;align-items:center;"
                + "  border-bottom:1px solid #33466B;}"
                + "header h1{margin:0;font-size:26px;color:#22D3EE;letter-spacing:1.5px;}"
                + "nav a{color:#22D3EE;text-decoration:none;font-weight:bold;margin:0 12px;font-size:16px;}"
                + "nav a:hover{color:#fff;}"
                + "header .who{font-size:16px;color:#A1AEC4;}"
                + "header .who a{color:#FFC4C4;text-decoration:none;font-weight:bold;margin-left:12px;}"
                + "main{padding:24px 32px;max-width:1200px;margin:auto;}"
                + ".flash{background:rgba(16,185,129,.18);border-left:4px solid #10B981;"
                + "  padding:14px 18px;margin-bottom:24px;border-radius:6px;font-size:17px;}"
                + ".flash a{color:#22D3EE;font-weight:bold;text-decoration:none;margin-left:14px;}"
                + ".intro{color:#A1AEC4;margin-bottom:26px;font-size:16px;line-height:1.5;}"
                + ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:18px;}"
                + ".card{background:#141F36;border:1px solid #33466B;border-radius:10px;"
                + "  padding:22px;display:flex;flex-direction:column;}"
                + ".card .icon{font-size:34px;margin-bottom:10px;}"
                + ".card h3{margin:0 0 6px;font-size:20px;color:#22D3EE;letter-spacing:.5px;}"
                + ".card .desc{color:#A1AEC4;font-size:15px;flex:1;margin-bottom:14px;line-height:1.45;}"
                + ".card form{display:flex;gap:8px;flex-wrap:wrap;}"
                + ".card input{padding:9px 12px;font-size:15px;border-radius:6px;"
                + "  border:1px solid #33466B;background:#0B1424;color:#E6EDF7;flex:1;min-width:0;}"
                + ".card select{padding:9px 12px;font-size:15px;border-radius:6px;"
                + "  border:1px solid #33466B;background:#0B1424;color:#E6EDF7;flex:1;}"
                + ".btn{padding:10px 16px;font-size:15px;font-weight:bold;letter-spacing:.5px;"
                + "  border:0;border-radius:6px;cursor:pointer;color:#fff;background:#0691A8;}"
                + ".btn:hover{background:#22D3EE;color:#0B1424;}"
                + ".btn.danger{background:#DC2626;}"
                + ".btn.danger:hover{background:#EF4444;}"
                + ".btn.warn{background:#D97706;}"
                + ".btn.warn:hover{background:#F59E0B;}"
                + ".section-title{margin-top:34px;margin-bottom:14px;font-size:20px;color:#E6EDF7;"
                + "  letter-spacing:.5px;border-left:4px solid #22D3EE;padding-left:12px;}"
                + ".stripe-CRIT{border-top:3px solid #DC2626;}"
                + ".stripe-HIGH{border-top:3px solid #EF4444;}"
                + ".stripe-MED {border-top:3px solid #F59E0B;}"
                + "</style></head><body>");

        // header
        w.println("<header><h1>CYBER SHIELD :: Simulator</h1>"
                + "<nav><a href='alerts'>Dashboard</a>"
                + "<a href='simulate'>Simulator</a></nav>"
                + "<div class='who'>" + escape(user) + " (" + escape(role) + ")"
                + " <a href='logout'>Logout</a></div></header>");
        w.println("<main>");

        if (msg != null) {
            w.println("<div class='flash'>" + escape(msg)
                    + "<a href='alerts'>View alerts &rarr;</a></div>");
        }

        w.println("<p class='intro'>This control panel triggers every alert type "
                + "the desktop application can generate. Each action calls "
                + "<code>AlertDAO.insert()</code> through JDBC and writes a row "
                + "into the SQLite <code>alerts</code> table; the dashboard at "
                + "<code>/alerts</code> reads from the same table and shows the new row.</p>");

        // ---------- Quick single-shot triggers ----------
        w.println("<div class='section-title'>Quick triggers · single alert</div>");
        w.println("<div class='grid'>");
        card(w, "🔓", "Suspicious Login", "Login from unusual IP / time-of-day. Severity HIGH.",
                "type=SUSPICIOUS_LOGIN", "warn", "stripe-HIGH");
        card(w, "🦠", "Malware Detected", "AV engine flags a known signature. Severity CRITICAL.",
                "type=MALWARE", "danger", "stripe-CRIT");
        card(w, "🛰️", "Port Scan", "Sequential probe across TCP ports. Severity CRITICAL.",
                "type=NETWORK_INTRUSION", "danger", "stripe-CRIT");
        card(w, "📂", "File Access Violation", "Read on /etc/shadow by unauthorized user. Severity HIGH.",
                "type=FILE_ACCESS", "warn", "stripe-HIGH");
        card(w, "👑", "Privilege Escalation", "User attempted sudo without rights. Severity HIGH.",
                "type=PRIVILEGE_ESCALATION", "warn", "stripe-HIGH");
        card(w, "📤", "Data Exfiltration", "Large outbound transfer to unknown IP. Severity CRITICAL.",
                "type=DATA_EXFILTRATION", "danger", "stripe-CRIT");
        card(w, "🌊", "DDoS Burst", "Request flood exceeding threshold. Severity CRITICAL.",
                "type=DDOS", "danger", "stripe-CRIT");
        w.println("</div>");

        // ---------- Compound simulators ----------
        w.println("<div class='section-title'>Compound simulators · multiple alerts</div>");
        w.println("<div class='grid'>");

        // File scanner card
        w.println("<div class='card stripe-HIGH'>"
                + "<div class='icon'>🗂️</div><h3>File Scanner</h3>"
                + "<div class='desc'>Mimics the desktop File Scan window. "
                + "Type a file path - <code>.exe/.bat/.ps1/.vbs</code> raises MALWARE, "
                + "names containing <code>passwd / shadow / secret / token</code> raise FILE_ACCESS.</div>"
                + "<form method='POST' action='simulate'>"
                + "<input type='hidden' name='action' value='filescan'/>"
                + "<input name='path' placeholder='C:\\\\users\\\\x\\\\update.exe' required/>"
                + "<button class='btn warn' type='submit'>Scan</button></form></div>");

        // Network monitor card
        w.println("<div class='card stripe-CRIT'>"
                + "<div class='icon'>🌐</div><h3>Network Monitor Burst</h3>"
                + "<div class='desc'>Simulates the live network monitor. Generates the chosen number of "
                + "DDOS / NETWORK_INTRUSION / DATA_EXFILTRATION alerts in one shot.</div>"
                + "<form method='POST' action='simulate'>"
                + "<input type='hidden' name='action' value='netmon'/>"
                + "<input name='count' type='number' min='1' max='25' value='5' required/>"
                + "<button class='btn danger' type='submit'>Burst</button></form></div>");

        // Malware scanner card
        w.println("<div class='card stripe-CRIT'>"
                + "<div class='icon'>🛡️</div><h3>Malware Scanner</h3>"
                + "<div class='desc'>Mimics the desktop Malware Scanner. Picks N random threats "
                + "(Trojan, Worm, Ransomware, Spyware…) and inserts a MALWARE alert for each.</div>"
                + "<form method='POST' action='simulate'>"
                + "<input type='hidden' name='action' value='malware'/>"
                + "<select name='mode'>"
                + "<option value='quick'>Quick Scan (3 detections)</option>"
                + "<option value='full'>Full Scan (5 detections)</option>"
                + "<option value='rootkit'>Rootkit Deep-Scan (7 detections)</option>"
                + "</select>"
                + "<button class='btn danger' type='submit'>Run Scan</button></form></div>");

        // Auto-monitor (one-click batch)
        w.println("<div class='card stripe-MED'>"
                + "<div class='icon'>🎲</div><h3>Random Alert Stream</h3>"
                + "<div class='desc'>Mimics the AWT Live Monitor. Generates 8 random alerts "
                + "spanning every category - good for a quick demo of the dashboard's filters.</div>"
                + "<form method='POST' action='simulate'>"
                + "<input type='hidden' name='action' value='random'/>"
                + "<button class='btn' type='submit'>Generate 8 random alerts</button></form></div>");

        w.println("</div></main></body></html>");
    }

    private void card(PrintWriter w, String icon, String title, String desc,
                      String hidden, String btnClass, String stripe) {
        w.println("<div class='card " + stripe + "'>"
                + "<div class='icon'>" + icon + "</div>"
                + "<h3>" + title + "</h3>"
                + "<div class='desc'>" + desc + "</div>"
                + "<form method='POST' action='simulate'>"
                + "<input type='hidden' name='action' value='quick'/>"
                + "<input type='hidden' name='" + hidden.split("=")[0] + "' value='" + hidden.split("=")[1] + "'/>"
                + "<button class='btn " + btnClass + "' type='submit'>Trigger</button>"
                + "</form></div>");
    }

    /* ============================== POST ============================== */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect("login"); return;
        }
        String user   = (String) s.getAttribute("user");
        String action = req.getParameter("action");
        String ip     = req.getRemoteAddr();
        AlertDAO dao  = new AlertDAO();
        String msg;

        try {
            if ("quick".equals(action)) {
                String type = req.getParameter("type");
                SecurityAlert a = buildAlertForType(type, user, ip);
                dao.insert(a);
                msg = "Triggered  " + type + "  alert  (#" + a.getAlertId() + ")";
            }
            else if ("filescan".equals(action)) {
                String path = req.getParameter("path");
                msg = doFileScan(dao, path, user);
            }
            else if ("netmon".equals(action)) {
                int n = parseInt(req.getParameter("count"), 5);
                msg = doNetMon(dao, n, user);
            }
            else if ("malware".equals(action)) {
                String mode = req.getParameter("mode");
                int n = "rootkit".equals(mode) ? 7 : "full".equals(mode) ? 5 : 3;
                msg = doMalwareScan(dao, n, user);
            }
            else if ("random".equals(action)) {
                msg = doRandomStream(dao, 8, user);
            }
            else msg = "Unknown action.";
        } catch (SQLException ex) {
            msg = "Database error: " + ex.getMessage();
        }

        resp.sendRedirect("simulate?msg=" + java.net.URLEncoder.encode(msg, "UTF-8"));
    }

    /* ============================== alert builders ============================== */

    private SecurityAlert buildAlertForType(String type, String user, String ip) {
        Severity sev;
        String message;
        String src = randomIp();

        switch (type == null ? "" : type) {
            case "SUSPICIOUS_LOGIN":
                sev = Severity.HIGH;
                message = "ALERT: Suspicious login attempt detected from " + src
                        + " (geo-anomaly)";
                break;
            case "MALWARE":
                sev = Severity.CRITICAL;
                message = "ALERT: " + THREATS[R.nextInt(THREATS.length)]
                        + " detected on host workstation";
                break;
            case "NETWORK_INTRUSION":
                sev = Severity.CRITICAL;
                message = "ALERT: Port scan detected on TCP/22 from external host " + src;
                break;
            case "FILE_ACCESS":
                sev = Severity.HIGH;
                message = "ALERT: Unauthorized read on /etc/shadow by user " + user;
                break;
            case "PRIVILEGE_ESCALATION":
                sev = Severity.HIGH;
                message = "ALERT: User '" + user + "' attempted sudo without rights";
                break;
            case "DATA_EXFILTRATION":
                sev = Severity.CRITICAL;
                message = "ALERT: " + (1 + R.nextInt(5)) + "GB outbound transfer to " + src;
                break;
            case "DDOS":
                sev = Severity.CRITICAL;
                message = "ALERT: DDoS-like traffic burst "
                        + (5000 + R.nextInt(50000)) + " req/sec on /login";
                break;
            default:
                sev = Severity.LOW;
                type = "INFO";
                message = "ALERT: Unknown event";
        }
        return new SecurityAlert(0, type, message, sev, src, user);
    }

    private String doFileScan(AlertDAO dao, String path, String user) throws SQLException {
        if (path == null || path.isEmpty()) return "Please provide a path.";
        String low = path.toLowerCase();
        boolean malware = low.endsWith(".exe") || low.endsWith(".bat")
                       || low.endsWith(".ps1") || low.endsWith(".vbs");
        boolean sensitive = low.contains("passwd") || low.contains("shadow")
                         || low.contains("secret") || low.contains("token");

        if (malware) {
            SecurityAlert a = new SecurityAlert(0, "MALWARE",
                    "ALERT: Suspicious executable detected at " + path,
                    Severity.CRITICAL, "127.0.0.1", user);
            dao.insert(a);
            return "File scan -> MALWARE alert raised for " + path;
        }
        if (sensitive) {
            SecurityAlert a = new SecurityAlert(0, "FILE_ACCESS",
                    "ALERT: Sensitive file scanned " + path,
                    Severity.HIGH, "127.0.0.1", user);
            dao.insert(a);
            return "File scan -> FILE_ACCESS alert raised for " + path;
        }
        return "File scan -> '" + path + "' is clean (no alert raised).";
    }

    private String doNetMon(AlertDAO dao, int n, String user) throws SQLException {
        n = Math.max(1, Math.min(n, 25));
        String[] types = { "DDOS", "NETWORK_INTRUSION", "DATA_EXFILTRATION" };
        for (int i = 0; i < n; i++) {
            String t = types[R.nextInt(types.length)];
            dao.insert(buildAlertForType(t, user, "10.0.0." + R.nextInt(254)));
        }
        return "Network burst -> " + n + " critical network alerts inserted.";
    }

    private String doMalwareScan(AlertDAO dao, int n, String user) throws SQLException {
        for (int i = 0; i < n; i++) {
            dao.insert(buildAlertForType("MALWARE", user, "127.0.0.1"));
        }
        return "Malware scan complete -> " + n + " threats detected and logged.";
    }

    private String doRandomStream(AlertDAO dao, int n, String user) throws SQLException {
        String[] all = { "SUSPICIOUS_LOGIN", "MALWARE", "NETWORK_INTRUSION", "FILE_ACCESS",
                         "PRIVILEGE_ESCALATION", "DATA_EXFILTRATION", "DDOS" };
        for (int i = 0; i < n; i++) {
            dao.insert(buildAlertForType(all[R.nextInt(all.length)], user, randomIp()));
        }
        return "Random stream -> " + n + " mixed alerts inserted.";
    }

    /* ============================== helpers ============================== */
    private static String randomIp() {
        return R.nextInt(255) + "." + R.nextInt(255) + "." + R.nextInt(255) + "." + R.nextInt(255);
    }
    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception ex) { return def; }
    }
    private static String escape(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
