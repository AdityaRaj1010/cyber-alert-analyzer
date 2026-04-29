package servlet;

import dao.AlertDAO;
import model.SecurityAlert;
import model.SecurityAlert.Severity;
import util.AlertFormatter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Web Security Operations Center.
 *
 * Replicates ALL the desktop tool frames inside a single JS-driven page:
 *   - File Scanner  (FileScanFrame.java)        : path, scan type, deep, save-to-DB, start
 *   - Network Mon.  (NetworkAlertFrame.java)    : threshold, start/stop, port scan, DDoS, exfil, progress bar
 *   - Malware Scn.  (MalwareScanFrame.java)     : Quick/Full/Rootkit, auto-quarantine, progress bar
 *   - Live Monitor  (AlertMonitorFrame.java)    : type chooser, generate, auto-3s
 *   - Critical view (CriticalAlertsFrame.java)  : link to /alerts?onlyCritical=true
 *
 * Architecture:
 *   GET  /simulate          -> renders the SPA (sidebar + tools + console + modal)
 *   POST /simulate          -> JSON endpoint that creates alerts via AlertDAO
 *                              and returns the rendered console lines + details.
 *
 * No external libraries - vanilla JS + fetch().
 */
@WebServlet("/simulate")
public class SimulateAlertServlet extends HttpServlet {

    private static final Random R = new Random();
    private static final String[] THREATS = {
            "Trojan.Generic", "Worm.AutoRun.A", "Backdoor.Mirai", "Ransomware.Locky",
            "Spyware.Keylog", "Adware.Bundle", "Rootkit.Necurs", "Trojan.Emotet"
    };

    /* =================================================================== */
    /*                                GET                                  */
    /* =================================================================== */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) { resp.sendRedirect("login"); return; }
        String user = (String) s.getAttribute("user");
        String role = (String) s.getAttribute("role");

        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter w = resp.getWriter();
        renderPage(w, user, role);
    }

    /* =================================================================== */
    /*                                POST                                 */
    /* =================================================================== */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        resp.setContentType("application/json;charset=UTF-8");
        if (s == null || s.getAttribute("user") == null) {
            resp.setStatus(401);
            resp.getWriter().write("{\"ok\":false,\"error\":\"Not logged in\"}");
            return;
        }

        String user   = (String) s.getAttribute("user");
        String action = req.getParameter("action");
        AlertDAO dao  = new AlertDAO();
        List<SecurityAlert> created = new ArrayList<>();
        List<String> info = new ArrayList<>();

        try {
            switch (action == null ? "" : action) {

                case "quick": {
                    String type = req.getParameter("type");
                    SecurityAlert a = buildAlertForType(type, user, randomIp());
                    dao.insert(a);
                    created.add(a);
                    break;
                }

                case "filescan": {
                    String path = req.getParameter("path");
                    boolean deep = "true".equals(req.getParameter("deep"));
                    String scanType = req.getParameter("scanType");
                    info.add(">>> " + (scanType == null ? "Quick Scan" : scanType)
                            + " on " + path + (deep ? "  (deep)" : ""));
                    SecurityAlert a = scanFile(path, user);
                    if (a != null) { dao.insert(a); created.add(a); }
                    else info.add("  ok  " + path + "  (no threats found)");
                    info.add("<<< Scan completed.");
                    break;
                }

                case "netscan_portscan": {
                    SecurityAlert a = buildAlertForType("NETWORK_INTRUSION", user, randomIp());
                    dao.insert(a); created.add(a);
                    break;
                }
                case "netscan_ddos": {
                    SecurityAlert a = buildAlertForType("DDOS", user, randomIp());
                    dao.insert(a); created.add(a);
                    break;
                }
                case "netscan_exfil": {
                    SecurityAlert a = buildAlertForType("DATA_EXFILTRATION", user, randomIp());
                    dao.insert(a); created.add(a);
                    break;
                }
                case "netscan_tick": {
                    int reqs      = R.nextInt(5000);
                    int threshold = parseInt(req.getParameter("threshold"), 1000);
                    info.add("[net] " + reqs + " req/sec");
                    if (reqs > threshold) {
                        SecurityAlert a = new SecurityAlert(0, "DDOS",
                                "ALERT: DDoS-like traffic burst " + reqs + " req/sec",
                                Severity.CRITICAL, randomIp(), user);
                        dao.insert(a); created.add(a);
                    }
                    // also publish the live req/sec value for the JS to render the bar
                    info.add("__BAR__" + reqs);
                    break;
                }

                case "malware": {
                    String mode = req.getParameter("mode");
                    boolean quarantine = "true".equals(req.getParameter("quarantine"));
                    int detections = "rootkit".equals(mode) ? 7
                                   : "full".equals(mode)    ? 5 : 3;
                    info.add(">>> " + (mode == null ? "Quick" : mode) + " scan started");
                    for (int i = 0; i < detections; i++) {
                        SecurityAlert a = buildAlertForType("MALWARE", user, "127.0.0.1");
                        dao.insert(a); created.add(a);
                        if (quarantine) info.add("   -> moved to /quarantine/");
                    }
                    info.add("<<< Scan complete.  " + detections + " threats found.");
                    break;
                }

                case "live_random": {
                    String[] all = {"SUSPICIOUS_LOGIN","MALWARE","NETWORK_INTRUSION","FILE_ACCESS",
                                    "PRIVILEGE_ESCALATION","DATA_EXFILTRATION","DDOS"};
                    String type = req.getParameter("type");
                    if (type == null || type.isEmpty() || "RANDOM".equals(type)) {
                        type = all[R.nextInt(all.length)];
                    }
                    SecurityAlert a = buildAlertForType(type, user, randomIp());
                    dao.insert(a); created.add(a);
                    break;
                }

                case "detail": {
                    int id = parseInt(req.getParameter("id"), -1);
                    SecurityAlert a = dao.findById(id);
                    StringBuffer sb = new StringBuffer();
                    sb.append("{\"ok\":true,\"detail\":\"")
                      .append(a == null ? "Alert not found." : escapeJson(AlertFormatter.formatDetail(a)))
                      .append("\"}");
                    resp.getWriter().write(sb.toString());
                    return;
                }

                default:
                    resp.setStatus(400);
                    resp.getWriter().write("{\"ok\":false,\"error\":\"Unknown action\"}");
                    return;
            }

            resp.getWriter().write(buildJson(created, info));

        } catch (SQLException ex) {
            resp.setStatus(500);
            resp.getWriter().write("{\"ok\":false,\"error\":\""
                    + escapeJson(ex.getMessage()) + "\"}");
        }
    }

    /* =================================================================== */
    /*                       Alert-construction logic                      */
    /* =================================================================== */
    private SecurityAlert buildAlertForType(String type, String user, String src) {
        Severity sev;  String message;
        if (type == null) type = "INFO";
        switch (type) {
            case "SUSPICIOUS_LOGIN":
                sev = Severity.HIGH;
                message = "ALERT: Suspicious login attempt detected from " + src + " (geo-anomaly)"; break;
            case "INVALID_LOGIN":
                sev = Severity.HIGH;
                message = "ALERT: Suspicious login attempt detected for user '" + user + "'"; break;
            case "MALWARE":
                sev = Severity.CRITICAL;
                message = "ALERT: " + THREATS[R.nextInt(THREATS.length)] + " detected on host workstation"; break;
            case "NETWORK_INTRUSION":
                sev = Severity.CRITICAL;
                message = "ALERT: Port scan detected on TCP/22 from external host " + src; break;
            case "FILE_ACCESS":
                sev = Severity.HIGH;
                message = "ALERT: Unauthorized read on /etc/shadow by user " + user; break;
            case "PRIVILEGE_ESCALATION":
                sev = Severity.HIGH;
                message = "ALERT: User '" + user + "' attempted sudo without rights"; break;
            case "DATA_EXFILTRATION":
                sev = Severity.CRITICAL;
                message = "ALERT: " + (1 + R.nextInt(5)) + "GB outbound transfer to " + src; break;
            case "DDOS":
                sev = Severity.CRITICAL;
                message = "ALERT: DDoS-like traffic burst " + (5000 + R.nextInt(50000)) + " req/sec on /login"; break;
            default:
                sev = Severity.LOW; type = "INFO";
                message = "ALERT: Unknown event";
        }
        return new SecurityAlert(0, type, message, sev, src, user);
    }

    /** Mirror of FileScanFrame's classification rules. */
    private SecurityAlert scanFile(String path, String user) {
        if (path == null || path.isEmpty()) return null;
        String low = path.toLowerCase();
        boolean malware = low.endsWith(".exe") || low.endsWith(".bat")
                       || low.endsWith(".ps1") || low.endsWith(".vbs");
        boolean sensitive = low.contains("passwd") || low.contains("shadow")
                         || low.contains("secret") || low.contains("token");
        if (malware) return new SecurityAlert(0, "MALWARE",
                "ALERT: Suspicious executable detected at " + path,
                Severity.CRITICAL, "127.0.0.1", user);
        if (sensitive) return new SecurityAlert(0, "FILE_ACCESS",
                "ALERT: Sensitive file scanned " + path,
                Severity.HIGH, "127.0.0.1", user);
        return null;
    }

    /* =================================================================== */
    /*                              JSON                                   */
    /* =================================================================== */
    private String buildJson(List<SecurityAlert> created, List<String> info) {
        StringBuffer sb = new StringBuffer();
        sb.append("{\"ok\":true,\"alerts\":[");
        for (int i = 0; i < created.size(); i++) {
            if (i > 0) sb.append(',');
            SecurityAlert a = created.get(i);
            sb.append("{\"id\":").append(a.getAlertId())
              .append(",\"type\":\"").append(escapeJson(a.getAlertType())).append('"')
              .append(",\"severity\":\"").append(a.getSeverity()).append('"')
              .append(",\"line\":\"").append(escapeJson(AlertFormatter.formatLine(a))).append('"')
              .append(",\"detail\":\"").append(escapeJson(AlertFormatter.formatDetail(a))).append('"')
              .append('}');
        }
        sb.append("],\"info\":[");
        for (int i = 0; i < info.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escapeJson(info.get(i))).append('"');
        }
        sb.append("]}");
        return sb.toString();
    }

    /* =================================================================== */
    /*                          HTML rendering                             */
    /* =================================================================== */
    private void renderPage(PrintWriter w, String user, String role) {
        w.println("<!doctype html><html><head><meta charset='UTF-8'>");
        w.println("<title>Cyber Shield - SOC Console</title>");
        w.println("<meta name='viewport' content='width=device-width,initial-scale=1'>");
        w.println("<style>" + CSS + "</style></head><body>");

        // Header
        w.println("<header class='topbar'>"
                + "<div class='brand'>CYBER  SHIELD<span> :: SOC Console</span></div>"
                + "<div class='who'>● <b>" + escape(user) + "</b> "
                + "<span class='muted'>(" + escape(role) + ")</span> "
                + "<a href='logout' class='logout'>Logout</a></div>"
                + "</header>");

        w.println("<div class='layout'>");
        // Sidebar
        w.println("<aside class='sidebar'>"
                + "<a href='#overview' class='nav active' data-tool='overview'><span class='ic'>▣</span>Overview</a>"
                + "<a href='#filescan' class='nav'        data-tool='filescan'><span class='ic'>📂</span>File Scanner</a>"
                + "<a href='#netmon'   class='nav'        data-tool='netmon'  ><span class='ic'>🌐</span>Network Monitor</a>"
                + "<a href='#malware'  class='nav'        data-tool='malware' ><span class='ic'>🛡</span>Malware Scanner</a>"
                + "<a href='#live'     class='nav'        data-tool='live'    ><span class='ic'>🎲</span>Live Monitor</a>"
                + "<div class='nav-sep'></div>"
                + "<a href='alerts'                 class='nav'><span class='ic'>📋</span>Alert Dashboard</a>"
                + "<a href='alerts?onlyCritical=true' class='nav crit'><span class='ic'>⚠</span>Critical Alerts</a>"
                + "</aside>");

        // Main column - all tools live here, only one shown at a time
        w.println("<main class='main'>");
        renderOverview(w);
        renderFileScanner(w);
        renderNetMon(w);
        renderMalware(w);
        renderLive(w);

        // Shared console + status
        w.println("<section class='console-wrap'>"
                + "<div class='console-head'>"
                + "  <span class='dot dot-cyan'></span>LIVE  ALERT  CONSOLE"
                + "  <span class='spacer'></span>"
                + "  <button class='btn-ghost' id='btnClear'>Clear</button>"
                + "</div>"
                + "<div id='console' class='console'>"
                + "  <div class='line muted'>[ready]  Click any action above to generate alerts.  Click any line for full details.</div>"
                + "</div>"
                + "</section>");

        w.println("</main></div>");

        // Modal for alert details
        w.println("<div id='modal' class='modal'>"
                + "<div class='modal-card'>"
                + "<div class='modal-head'>SECURITY ALERT  ::  Detail<span class='spacer'></span>"
                + "<button class='btn-ghost' id='modalClose'>Close</button></div>"
                + "<pre id='modalBody' class='modal-body'></pre>"
                + "</div></div>");

        w.println("<script>" + JS + "</script>");
        w.println("</body></html>");
    }

    private void renderOverview(PrintWriter w) {
        w.println("<section id='tool-overview' class='tool active'>"
                + "<h2 class='tool-title'>SOC Overview</h2>"
                + "<p class='tool-sub'>Web equivalents of every desktop frame.  Each tool calls "
                + "<code>AlertDAO.insert()</code> over JDBC and writes a row to the same SQLite "
                + "table the dashboard reads from.</p>"
                + "<div class='cards'>"
                + card("📂", "File Scanner", "Path / scan type / deep / save-to-DB.  Mirrors FileScanFrame.", "filescan")
                + card("🌐", "Network Monitor", "Threshold, live req/sec bar, port scan / DDoS / exfil triggers.  Mirrors NetworkAlertFrame.", "netmon")
                + card("🛡", "Malware Scanner", "Quick / Full / Rootkit + auto-quarantine + progress bar.  Mirrors MalwareScanFrame.", "malware")
                + card("🎲", "Live Monitor", "Random alert generator + auto-fire toggle.  Mirrors AlertMonitorFrame.", "live")
                + "</div></section>");
    }

    private String card(String icon, String title, String desc, String tool) {
        return "<a href='#" + tool + "' data-tool='" + tool + "' class='ov-card'>"
                + "<div class='ic'>" + icon + "</div>"
                + "<div class='t'>" + title + "</div>"
                + "<div class='d'>" + desc + "</div>"
                + "<div class='go'>Open  &rarr;</div>"
                + "</a>";
    }

    private void renderFileScanner(PrintWriter w) {
        w.println("<section id='tool-filescan' class='tool'>"
                + "<h2 class='tool-title'>File / Folder Scanner</h2>"
                + "<p class='tool-sub'>Detect malware (.exe / .bat / .ps1 / .vbs) and unauthorized "
                + "file access (passwd / shadow / secret / token).  Mirrors the desktop FileScanFrame.</p>"
                + "<div class='panel'>"
                + "  <div class='row'>"
                + "    <label>Path</label>"
                + "    <input id='fsPath' type='text' placeholder='C:\\\\users\\\\admin\\\\update.exe' style='flex:1'/>"
                + "    <button class='btn warn' id='fsBrowse' type='button'>Examples ▼</button>"
                + "  </div>"
                + "  <div class='row'>"
                + "    <label>Scan Type</label>"
                + "    <select id='fsType'>"
                + "      <option>Quick Scan</option><option>Full Scan</option>"
                + "      <option>Heuristic</option><option>Signature-Based</option>"
                + "    </select>"
                + "    <label class='chk'><input type='checkbox' id='fsDeep'/>Deep Scan (recursive)</label>"
                + "    <label class='chk'><input type='checkbox' id='fsSave' checked/>Save alerts to DB</label>"
                + "  </div>"
                + "  <div class='row'>"
                + "    <button class='btn primary'  id='fsScan'>▶  Start Scan</button>"
                + "    <button class='btn ghost'    id='fsStop'>■  Stop</button>"
                + "  </div>"
                + "  <div id='fsExamples' class='examples' style='display:none'>"
                + "    <span>Click an example to fill the path:</span>"
                + "    <a data-ex='C:\\\\users\\\\admin\\\\update.exe'>update.exe</a>"
                + "    <a data-ex='C:\\\\install.bat'>install.bat</a>"
                + "    <a data-ex='/home/raj/script.ps1'>script.ps1</a>"
                + "    <a data-ex='/etc/shadow'>/etc/shadow</a>"
                + "    <a data-ex='secret_token.txt'>secret_token.txt</a>"
                + "    <a data-ex='notes.txt'>notes.txt  (clean)</a>"
                + "  </div>"
                + "</div></section>");
    }

    private void renderNetMon(PrintWriter w) {
        w.println("<section id='tool-netmon' class='tool'>"
                + "<h2 class='tool-title'>Network Intrusion Monitor</h2>"
                + "<p class='tool-sub'>Detect intrusions, DDoS bursts and data exfiltration.  "
                + "When live monitoring is on, traffic is sampled every 800 ms; values above the "
                + "threshold raise a CRITICAL DDOS alert automatically.</p>"
                + "<div class='panel'>"
                + "  <div class='row'>"
                + "    <label>DDoS req/sec threshold</label>"
                + "    <input id='nmThreshold' type='number' min='100' max='100000' step='100' value='1000' style='width:120px'/>"
                + "    <button class='btn primary' id='nmStart'>▶  Start Monitor</button>"
                + "    <button class='btn ghost'   id='nmStop'>■  Stop</button>"
                + "  </div>"
                + "  <div class='row'>"
                + "    <button class='btn danger' id='nmPort'>Simulate Port Scan</button>"
                + "    <button class='btn danger' id='nmDdos'>Simulate DDoS</button>"
                + "    <button class='btn danger' id='nmExfil'>Simulate Data Exfiltration</button>"
                + "  </div>"
                + "  <div class='bar-wrap'>"
                + "    <div id='nmBarFill' class='bar-fill' style='width:0%'></div>"
                + "    <div id='nmBarText' class='bar-text'>0 req/sec</div>"
                + "  </div>"
                + "</div></section>");
    }

    private void renderMalware(PrintWriter w) {
        w.println("<section id='tool-malware' class='tool'>"
                + "<h2 class='tool-title'>Malware Scanner</h2>"
                + "<p class='tool-sub'>Heuristic + signature scan.  Quick mode produces 3 detections, "
                + "Full produces 5, Rootkit Deep-Scan produces 7.  Each detection inserts a CRITICAL MALWARE "
                + "alert via JDBC.</p>"
                + "<div class='panel'>"
                + "  <fieldset class='group'><legend>Scan Mode</legend>"
                + "    <label class='radio'><input type='radio' name='mwMode' value='quick' checked/>Quick (memory only)</label>"
                + "    <label class='radio'><input type='radio' name='mwMode' value='full'/>Full disk</label>"
                + "    <label class='radio'><input type='radio' name='mwMode' value='rootkit'/>Rootkit deep-scan</label>"
                + "  </fieldset>"
                + "  <div class='row'>"
                + "    <label class='chk'><input type='checkbox' id='mwQuarantine' checked/>Auto-quarantine on detection</label>"
                + "    <button class='btn primary' id='mwScan'>▶  Start Scan</button>"
                + "  </div>"
                + "  <div class='bar-wrap'>"
                + "    <div id='mwBarFill' class='bar-fill green' style='width:0%'></div>"
                + "    <div id='mwBarText' class='bar-text'>0 %</div>"
                + "  </div>"
                + "</div></section>");
    }

    private void renderLive(PrintWriter w) {
        w.println("<section id='tool-live' class='tool'>"
                + "<h2 class='tool-title'>AWT-Style Live Monitor</h2>"
                + "<p class='tool-sub'>Random alert generator.  Each click fires one alert of the chosen "
                + "type (or random).  Toggle &quot;Auto-generate every 3s&quot; to mimic the desktop AWT background thread.</p>"
                + "<div class='panel'>"
                + "  <div class='row'>"
                + "    <label>Type</label>"
                + "    <select id='lvType'>"
                + "      <option value='RANDOM'>RANDOM (any)</option>"
                + "      <option>SUSPICIOUS_LOGIN</option><option>MALWARE</option>"
                + "      <option>NETWORK_INTRUSION</option><option>FILE_ACCESS</option>"
                + "      <option>PRIVILEGE_ESCALATION</option><option>DATA_EXFILTRATION</option>"
                + "      <option>DDOS</option>"
                + "    </select>"
                + "    <button class='btn primary' id='lvGen'>Generate Random Alert</button>"
                + "    <label class='chk'><input type='checkbox' id='lvAuto'/>Auto-generate every 3s</label>"
                + "  </div>"
                + "</div></section>");
    }

    /* =================================================================== */
    /*                          helpers                                    */
    /* =================================================================== */
    private static String randomIp() {
        return R.nextInt(255) + "." + R.nextInt(255) + "." + R.nextInt(255) + "." + R.nextInt(255);
    }
    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception ex) { return def; }
    }
    private static String escape(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    /* =================================================================== */
    /*                          CSS  +  JS                                 */
    /* =================================================================== */
    private static final String CSS =
        "*{box-sizing:border-box}"
      + "body{margin:0;font-family:'Segoe UI',Arial,sans-serif;font-size:16px;"
      + "  background:#0B1424;color:#E6EDF7;min-height:100vh;}"
      + "code{background:#0E2A47;padding:1px 6px;border-radius:4px;font-size:14px;color:#22D3EE;}"
      + ".topbar{background:linear-gradient(90deg,#0E2A47,#1B2944);padding:18px 28px;"
      + "  display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #33466B;}"
      + ".topbar .brand{font-size:24px;font-weight:bold;color:#22D3EE;letter-spacing:2px;}"
      + ".topbar .brand span{color:#A1AEC4;font-weight:normal;letter-spacing:1px;font-size:18px;}"
      + ".topbar .who{font-size:15px;color:#A1AEC4;}"
      + ".topbar .who b{color:#10B981;}"
      + ".topbar .logout{margin-left:14px;color:#FFC4C4;text-decoration:none;font-weight:bold;}"
      + ".layout{display:flex;min-height:calc(100vh - 68px);}"
      + ".sidebar{width:240px;background:#0F1B30;border-right:1px solid #33466B;padding:18px 0;flex-shrink:0;}"
      + ".sidebar .nav{display:flex;align-items:center;gap:12px;padding:12px 22px;color:#A1AEC4;"
      + "  text-decoration:none;font-size:15.5px;font-weight:600;border-left:4px solid transparent;}"
      + ".sidebar .nav .ic{width:22px;text-align:center;font-size:18px;}"
      + ".sidebar .nav:hover{color:#fff;background:#141F36;}"
      + ".sidebar .nav.active{color:#22D3EE;background:#141F36;border-left-color:#22D3EE;}"
      + ".sidebar .nav.crit{color:#FFC4C4;}"
      + ".nav-sep{height:1px;background:#33466B;margin:14px 22px;}"
      + ".main{flex:1;padding:24px 28px;display:flex;flex-direction:column;min-width:0;}"
      + ".tool{display:none;animation:fade .25s ease;}"
      + ".tool.active{display:block;}"
      + "@keyframes fade{from{opacity:0;transform:translateY(4px)}to{opacity:1;transform:none}}"
      + ".tool-title{margin:0 0 6px;font-size:24px;color:#22D3EE;letter-spacing:1px;"
      + "  border-left:4px solid #22D3EE;padding-left:12px;}"
      + ".tool-sub{color:#A1AEC4;font-size:15px;line-height:1.5;margin:0 0 18px 16px;max-width:880px;}"
      + ".panel{background:#141F36;border:1px solid #33466B;border-radius:10px;padding:20px;}"
      + ".panel .row{display:flex;align-items:center;gap:14px;margin-bottom:12px;flex-wrap:wrap;}"
      + ".panel label{font-size:15px;font-weight:bold;color:#E6EDF7;min-width:84px;}"
      + ".panel input[type=text],.panel input[type=number],.panel select{"
      + "  padding:9px 12px;font-size:15px;border-radius:6px;border:1px solid #33466B;"
      + "  background:#0B1424;color:#E6EDF7;}"
      + ".chk{display:flex;align-items:center;gap:7px;font-size:15px;font-weight:600;color:#E6EDF7;min-width:0!important;}"
      + ".chk input,.radio input{accent-color:#22D3EE;width:16px;height:16px;}"
      + ".radio{display:flex;align-items:center;gap:7px;font-size:15px;color:#E6EDF7;margin-right:18px;}"
      + ".group{border:1px solid #33466B;border-radius:8px;padding:12px 16px;margin-bottom:12px;}"
      + ".group legend{padding:0 8px;color:#22D3EE;font-weight:bold;}"
      + ".btn{padding:10px 18px;font-size:15px;font-weight:bold;letter-spacing:.5px;"
      + "  border:0;border-radius:6px;cursor:pointer;color:#fff;background:#0691A8;transition:all .15s;}"
      + ".btn:hover{background:#22D3EE;color:#0B1424;}"
      + ".btn.primary{background:#0691A8;}"
      + ".btn.danger{background:#DC2626;}.btn.danger:hover{background:#EF4444;color:#fff;}"
      + ".btn.warn  {background:#D97706;}.btn.warn:hover  {background:#F59E0B;color:#0B1424;}"
      + ".btn.ghost {background:#1B2944;}.btn.ghost:hover {background:#33466B;}"
      + ".btn-ghost{background:transparent;border:1px solid #33466B;color:#A1AEC4;padding:5px 11px;"
      + "  font-size:13px;border-radius:5px;cursor:pointer;font-weight:bold;}"
      + ".btn-ghost:hover{color:#fff;border-color:#22D3EE;}"
      + ".examples{margin-top:8px;display:flex;flex-wrap:wrap;gap:8px;align-items:center;font-size:14px;color:#A1AEC4;}"
      + ".examples a{background:#1B2944;color:#22D3EE;padding:5px 11px;border-radius:14px;"
      + "  cursor:pointer;text-decoration:none;}"
      + ".examples a:hover{background:#0691A8;color:#fff;}"
      + ".bar-wrap{margin-top:14px;background:#0B1424;border:1px solid #33466B;border-radius:6px;"
      + "  height:32px;position:relative;overflow:hidden;}"
      + ".bar-fill{position:absolute;left:0;top:0;height:100%;background:#DC2626;transition:width .25s;}"
      + ".bar-fill.green{background:#10B981;}"
      + ".bar-text{position:absolute;width:100%;text-align:center;line-height:32px;font-weight:bold;"
      + "  font-size:14px;color:#fff;text-shadow:0 1px 2px rgba(0,0,0,.6);}"
      + ".cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:16px;}"
      + ".ov-card{display:flex;flex-direction:column;background:#141F36;border:1px solid #33466B;"
      + "  border-radius:10px;padding:20px;text-decoration:none;color:inherit;transition:all .15s;}"
      + ".ov-card:hover{border-color:#22D3EE;transform:translateY(-2px);}"
      + ".ov-card .ic{font-size:34px;margin-bottom:10px;}"
      + ".ov-card .t{font-size:18px;font-weight:bold;color:#22D3EE;margin-bottom:6px;}"
      + ".ov-card .d{font-size:14px;color:#A1AEC4;flex:1;line-height:1.45;}"
      + ".ov-card .go{font-size:14px;font-weight:bold;color:#10B981;margin-top:10px;}"
      // Console
      + ".console-wrap{margin-top:18px;background:#141F36;border:1px solid #33466B;border-radius:10px;"
      + "  display:flex;flex-direction:column;flex:1;min-height:300px;}"
      + ".console-head{display:flex;align-items:center;gap:10px;padding:10px 16px;"
      + "  border-bottom:1px solid #33466B;color:#22D3EE;font-weight:bold;letter-spacing:1px;}"
      + ".console-head .spacer{flex:1;}"
      + ".dot{width:10px;height:10px;border-radius:50%;display:inline-block;}"
      + ".dot-cyan{background:#22D3EE;box-shadow:0 0 8px #22D3EE;}"
      + ".console{font-family:Consolas,Monaco,monospace;font-size:14px;background:#07101F;"
      + "  padding:14px 16px;flex:1;overflow-y:auto;max-height:360px;}"
      + ".console .line{padding:4px 8px;border-radius:3px;margin-bottom:2px;cursor:pointer;"
      + "  white-space:pre-wrap;word-break:break-all;}"
      + ".console .line:hover{background:#1B2944;}"
      + ".console .line.muted{color:#6B7280;cursor:default;}"
      + ".console .line.info{color:#A1AEC4;cursor:default;}"
      + ".console .line.CRITICAL{color:#FCA5A5;}"
      + ".console .line.HIGH    {color:#FCD34D;}"
      + ".console .line.MEDIUM  {color:#FBBF24;}"
      + ".console .line.LOW     {color:#86EFAC;}"
      // Modal
      + ".modal{display:none;position:fixed;inset:0;background:rgba(0,0,0,.7);align-items:center;"
      + "  justify-content:center;z-index:99;}"
      + ".modal.open{display:flex;}"
      + ".modal-card{width:min(680px,92%);background:#141F36;border:1px solid #DC2626;border-radius:10px;"
      + "  box-shadow:0 12px 40px rgba(0,0,0,.7);}"
      + ".modal-head{display:flex;align-items:center;padding:14px 18px;background:#1B2944;"
      + "  border-radius:10px 10px 0 0;color:#FCA5A5;font-weight:bold;letter-spacing:1px;}"
      + ".modal-head .spacer{flex:1;}"
      + ".modal-body{padding:20px;color:#E6EDF7;font-family:Consolas,monospace;font-size:14px;"
      + "  margin:0;white-space:pre;line-height:1.55;}"
      ;

    private static final String JS =
        "(()=>{"
      // ----- helpers -----
      + "const $=q=>document.querySelector(q),$$=q=>document.querySelectorAll(q);"
      + "const consoleEl=$('#console');"
      + "function append(text,cls){const d=document.createElement('div');d.className='line '+(cls||'');"
      + "  d.textContent=text;d.dataset.detail=text;consoleEl.appendChild(d);"
      + "  consoleEl.scrollTop=consoleEl.scrollHeight;return d;}"
      + "function showModal(text){$('#modalBody').textContent=text;$('#modal').classList.add('open');}"
      + "$('#modalClose').onclick=()=>$('#modal').classList.remove('open');"
      + "$('#modal').addEventListener('click',e=>{if(e.target.id==='modal')$('#modal').classList.remove('open');});"
      // ----- console click -> detail (fetched if id is present) -----
      + "consoleEl.addEventListener('click',async e=>{"
      + "  const li=e.target.closest('.line');if(!li||li.classList.contains('muted')||li.classList.contains('info'))return;"
      + "  if(li.dataset.full){showModal(li.dataset.full);return;}"
      + "  if(li.dataset.id){"
      + "    const fd=new FormData();fd.append('action','detail');fd.append('id',li.dataset.id);"
      + "    const r=await fetch('simulate',{method:'POST',body:fd});const d=await r.json();"
      + "    if(d.ok){li.dataset.full=d.detail;showModal(d.detail);}"
      + "  } else {showModal(li.dataset.detail||li.textContent);}"
      + "});"
      + "$('#btnClear').onclick=()=>{consoleEl.innerHTML='';append('[cleared]','muted');};"
      // ----- sidebar nav -----
      + "function showTool(name){"
      + "  $$('.tool').forEach(t=>t.classList.remove('active'));"
      + "  const sec=$('#tool-'+name);if(sec)sec.classList.add('active');"
      + "  $$('.sidebar .nav').forEach(a=>a.classList.toggle('active',a.dataset.tool===name));"
      + "  if(history.replaceState)history.replaceState(null,'','#'+name);"
      + "}"
      + "$$('[data-tool]').forEach(a=>a.addEventListener('click',e=>{"
      + "  if(a.tagName==='A'&&a.getAttribute('href')&&a.getAttribute('href').startsWith('#')){"
      + "    e.preventDefault();showTool(a.dataset.tool);}"
      + "}));"
      + "if(location.hash){const t=location.hash.slice(1);if($('#tool-'+t))showTool(t);}"
      // ----- generic action -----
      + "async function call(fd){"
      + "  try{const r=await fetch('simulate',{method:'POST',body:fd});const d=await r.json();"
      + "    if(!d.ok){append('[error] '+(d.error||'unknown'),'CRITICAL');return null;}"
      + "    (d.info||[]).forEach(s=>{"
      + "      if(s.startsWith('__BAR__')){const v=parseInt(s.slice(7))||0;updateNetBar(v);return;}"
      + "      append(s,'info');"
      + "    });"
      + "    (d.alerts||[]).forEach(a=>{"
      + "      const li=append(a.line,a.severity);"
      + "      li.dataset.id=a.id;li.dataset.full=a.detail;"
      + "    });"
      + "    return d;"
      + "  }catch(e){append('[network error] '+e,'CRITICAL');return null;}"
      + "}"
      // ----- File Scanner -----
      + "$('#fsBrowse').onclick=()=>$('#fsExamples').style.display=$('#fsExamples').style.display==='none'?'flex':'none';"
      + "$$('#fsExamples a').forEach(a=>a.onclick=()=>{$('#fsPath').value=a.dataset.ex;$('#fsExamples').style.display='none';});"
      + "let fsRunning=false;"
      + "$('#fsScan').onclick=async()=>{"
      + "  if(fsRunning)return;fsRunning=true;$('#fsScan').disabled=true;"
      + "  const fd=new FormData();fd.append('action','filescan');"
      + "  fd.append('path',$('#fsPath').value||'');"
      + "  fd.append('deep',$('#fsDeep').checked);fd.append('scanType',$('#fsType').value);"
      + "  await call(fd);fsRunning=false;$('#fsScan').disabled=false;"
      + "};"
      + "$('#fsStop').onclick=()=>{fsRunning=false;append('[file scan] stop requested','info');};"
      // ----- Network Monitor -----
      + "let nmTimer=null;"
      + "function updateNetBar(v){const max=5000;const pct=Math.min(100,(v/max)*100);"
      + "  $('#nmBarFill').style.width=pct+'%';$('#nmBarText').textContent=v+' req/sec';}"
      + "$('#nmStart').onclick=()=>{"
      + "  if(nmTimer)return;append('=== Network Monitor started ===','info');"
      + "  nmTimer=setInterval(async()=>{"
      + "    const fd=new FormData();fd.append('action','netscan_tick');"
      + "    fd.append('threshold',$('#nmThreshold').value||1000);"
      + "    await call(fd);"
      + "  },800);"
      + "};"
      + "$('#nmStop').onclick=()=>{if(nmTimer){clearInterval(nmTimer);nmTimer=null;"
      + "  append('=== Network Monitor stopped ===','info');updateNetBar(0);}};"
      + "$('#nmPort').onclick =()=>{const fd=new FormData();fd.append('action','netscan_portscan');call(fd);};"
      + "$('#nmDdos').onclick =()=>{const fd=new FormData();fd.append('action','netscan_ddos');    call(fd);};"
      + "$('#nmExfil').onclick=()=>{const fd=new FormData();fd.append('action','netscan_exfil');   call(fd);};"
      // ----- Malware Scanner -----
      + "$('#mwScan').onclick=async()=>{"
      + "  $('#mwScan').disabled=true;$('#mwBarFill').style.width='0%';$('#mwBarText').textContent='0 %';"
      + "  const mode=document.querySelector('input[name=\"mwMode\"]:checked').value;"
      + "  const total=mode==='rootkit'?100:mode==='full'?80:30;"
      + "  for(let p=0;p<=total;p+=4){"
      + "    $('#mwBarFill').style.width=(p/total*100)+'%';"
      + "    $('#mwBarText').textContent=Math.round(p/total*100)+' %';"
      + "    await new Promise(r=>setTimeout(r,40));"
      + "  }"
      + "  const fd=new FormData();fd.append('action','malware');fd.append('mode',mode);"
      + "  fd.append('quarantine',$('#mwQuarantine').checked);"
      + "  await call(fd);"
      + "  $('#mwBarFill').style.width='100%';$('#mwBarText').textContent='100 %';"
      + "  $('#mwScan').disabled=false;"
      + "};"
      // ----- Live Monitor -----
      + "let lvTimer=null;"
      + "$('#lvGen').onclick=()=>{const fd=new FormData();fd.append('action','live_random');"
      + "  fd.append('type',$('#lvType').value);call(fd);};"
      + "$('#lvAuto').onchange=()=>{"
      + "  if($('#lvAuto').checked){"
      + "    append('[live] auto-fire enabled (every 3s)','info');"
      + "    lvTimer=setInterval(()=>$('#lvGen').click(),3000);"
      + "  }else{if(lvTimer){clearInterval(lvTimer);lvTimer=null;append('[live] auto-fire stopped','info');}}"
      + "};"
      + "})();";
}
