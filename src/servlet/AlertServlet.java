package servlet;

import dao.AlertDAO;
import model.SecurityAlert;
import util.AlertFormatter;
import util.SecurityAlertComparator;
import util.SecurityAlertComparator.Direction;
import util.SecurityAlertComparator.Field;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/**
 * Renders an HTML dashboard of security alerts pulled from SQLite via
 * {@link AlertDAO}.  Each row is clickable - opens a modal that shows
 * the full StringBuffer-formatted detail (same as the desktop "View
 * Details" button on the dashboard).
 */
@WebServlet("/alerts")
public class AlertServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user") == null) {
            resp.sendRedirect("login");
            return;
        }

        boolean onlyCritical = "true".equalsIgnoreCase(req.getParameter("onlyCritical"));
        String  type         = req.getParameter("type");

        // ----- sort params (default = TIME / DESC, newest first) -----
        Field     sortField = parseField(req.getParameter("sort"));
        Direction sortDir   = parseDir  (req.getParameter("dir"));
        SecurityAlertComparator sorter = new SecurityAlertComparator(sortField, sortDir);

        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter w = resp.getWriter();

        w.println("<!doctype html><html><head><title>Cyber Shield - Alerts</title>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "*{box-sizing:border-box}"
                + "body{font-family:'Segoe UI',Arial,sans-serif;font-size:17px;"
                + "  background:#0B1424;color:#E6EDF7;margin:0;min-height:100vh;}"
                + "header{background:linear-gradient(90deg,#0E2A47,#1B2944);"
                + "  padding:22px 32px;display:flex;justify-content:space-between;align-items:center;"
                + "  border-bottom:1px solid #33466B;flex-wrap:wrap;gap:14px;}"
                + "header h1{margin:0;font-size:24px;color:#22D3EE;letter-spacing:1.5px;}"
                + "nav{display:flex;gap:18px;}"
                + "nav a{color:#22D3EE;text-decoration:none;font-weight:bold;font-size:16px;}"
                + "nav a:hover{color:#fff;}"
                + "header .who{font-size:16px;color:#A1AEC4;}"
                + "header .who a{color:#FFC4C4;text-decoration:none;font-weight:bold;margin-left:8px;}"
                + "main{padding:24px 32px;}"
                + ".cta{background:#141F36;border:1px solid #33466B;border-left:4px solid #22D3EE;"
                + "  padding:14px 18px;margin-bottom:20px;border-radius:6px;font-size:16px;}"
                + ".cta a{color:#22D3EE;font-weight:bold;text-decoration:none;}"
                + ".filters{margin-bottom:18px;}"
                + ".filters a{display:inline-block;background:#141F36;color:#22D3EE;"
                + "  padding:9px 16px;border-radius:20px;margin:4px 6px 4px 0;text-decoration:none;"
                + "  font-size:15px;font-weight:bold;border:1px solid #33466B;}"
                + ".filters a:hover{background:#0691A8;color:#fff;}"
                + "table{width:100%;border-collapse:collapse;background:#141F36;border-radius:8px;overflow:hidden;}"
                + "th,td{padding:12px 14px;text-align:left;font-size:15px;border-bottom:1px solid #33466B;}"
                + "th{background:#1B2944;color:#22D3EE;font-size:16px;letter-spacing:.5px;text-transform:uppercase;}"
                + "tbody tr{cursor:pointer;}"
                + "tbody tr:hover td{background:#1B2944;}"
                + ".sev-CRITICAL{color:#DC2626;font-weight:bold;}"
                + ".sev-HIGH    {color:#EF4444;font-weight:bold;}"
                + ".sev-MEDIUM  {color:#F59E0B;font-weight:bold;}"
                + ".sev-LOW     {color:#10B981;font-weight:bold;}"
                + ".empty{color:#A1AEC4;font-style:italic;padding:30px;text-align:center;font-size:16px;}"
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
                + ".btn-ghost{background:transparent;border:1px solid #33466B;color:#A1AEC4;padding:5px 11px;"
                + "  font-size:13px;border-radius:5px;cursor:pointer;font-weight:bold;}"
                + ".btn-ghost:hover{color:#fff;border-color:#22D3EE;}"
                + "</style></head><body>");

        w.println("<header><h1>CYBER SHIELD :: SOC Dashboard</h1>"
                + "<nav>"
                + "<a href='alerts'>Dashboard</a>"
                + "<a href='simulate'>Simulator</a>"
                + "</nav>"
                + "<div class='who'>Logged in as <b style='color:#10B981'>"
                + escape((String) s.getAttribute("user")) + "</b> ("
                + escape((String) s.getAttribute("role")) + ")"
                + " <a href='logout'>Logout</a></div></header>");
        w.println("<main>");
        w.println("<div class='cta'>Need to demo every alert type? Open the "
                + "<a href='simulate'>Simulator &rarr;</a> "
                + "for full File Scanner / Network Monitor / Malware Scanner / Live Monitor controls."
                + "  <span style='color:#A1AEC4;margin-left:14px'>Tip: click any row below for full details.</span></div>");
        w.println("<div class='filters'>"
                + "<a href='alerts'>All</a>"
                + "<a href='alerts?onlyCritical=true'>Critical only</a>"
                + "<a href='alerts?type=INVALID_LOGIN'>Invalid logins</a>"
                + "<a href='alerts?type=MALWARE'>Malware</a>"
                + "<a href='alerts?type=NETWORK_INTRUSION'>Network</a>"
                + "<a href='alerts?type=DDOS'>DDoS</a>"
                + "<a href='alerts?type=DATA_EXFILTRATION'>Exfiltration</a>"
                + "<a href='alerts?type=FILE_ACCESS'>File access</a>"
                + "<a href='alerts?type=PRIVILEGE_ESCALATION'>Privilege</a>"
                + "</div>");

        // Build clickable, direction-toggling column headers using the
        // same custom Comparator the desktop dashboard uses.
        w.println("<table id='alertsTable'><thead><tr>"
                + sortHeader("ID",        Field.ID,       sortField, sortDir, type, onlyCritical)
                + sortHeader("Time",      Field.TIME,     sortField, sortDir, type, onlyCritical)
                + sortHeader("Severity",  Field.SEVERITY, sortField, sortDir, type, onlyCritical)
                + sortHeader("Type",      Field.TYPE,     sortField, sortDir, type, onlyCritical)
                + sortHeader("Message",   Field.MESSAGE,  sortField, sortDir, type, onlyCritical)
                + "<th>Source IP</th>"
                + sortHeader("User",      Field.USER,     sortField, sortDir, type, onlyCritical)
                + "</tr></thead><tbody>");

        try {
            AlertDAO dao = new AlertDAO();
            List<SecurityAlert> data;
            if (onlyCritical)              data = dao.findCritical();
            else if (type != null && !type.isEmpty()) data = dao.findByType(type);
            else                            data = dao.findAll();

            // ===== custom Comparator applied here =====
            data.sort(sorter);

            for (SecurityAlert a : data) {
                String detail = AlertFormatter.formatDetail(a);
                w.println("<tr data-detail=\"" + htmlAttr(detail) + "\">"
                        + "<td>" + a.getAlertId() + "</td>"
                        + "<td>" + (a.getCreatedAt() == null ? "" : a.getCreatedAt()) + "</td>"
                        + "<td class='sev-" + a.getSeverity() + "'>" + a.getSeverity() + "</td>"
                        + "<td>" + escape(a.getAlertType()) + "</td>"
                        + "<td>" + escape(a.getMessage()) + "</td>"
                        + "<td>" + escape(a.getSourceIp()) + "</td>"
                        + "<td>" + escape(a.getUsername()) + "</td>"
                        + "</tr>");
            }
            if (data.isEmpty())
                w.println("<tr><td colspan='7' class='empty'>No alerts found.</td></tr>");
        } catch (SQLException ex) {
            w.println("<tr><td colspan='7' class='empty'>DB error: "
                    + escape(ex.getMessage()) + "</td></tr>");
        }

        w.println("</tbody></table></main>");

        // Modal + click handler for row -> details
        w.println("<div id='modal' class='modal'>"
                + "<div class='modal-card'>"
                + "<div class='modal-head'>SECURITY ALERT  ::  Detail<span class='spacer'></span>"
                + "<button class='btn-ghost' id='modalClose'>Close</button></div>"
                + "<pre id='modalBody' class='modal-body'></pre>"
                + "</div></div>");

        w.println("<script>(()=>{"
                + "const m=document.getElementById('modal'),b=document.getElementById('modalBody');"
                + "document.querySelectorAll('#alertsTable tbody tr').forEach(tr=>{"
                + "  if(!tr.dataset.detail)return;"
                + "  tr.addEventListener('click',()=>{b.textContent=tr.dataset.detail;m.classList.add('open');});"
                + "});"
                + "document.getElementById('modalClose').onclick=()=>m.classList.remove('open');"
                + "m.addEventListener('click',e=>{if(e.target===m)m.classList.remove('open');});"
                + "})();</script>");

        w.println("</body></html>");
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
    private static String htmlAttr(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("\"","&quot;")
                .replace("<","&lt;").replace(">","&gt;").replace("\n","&#10;");
    }

    /**
     * Build a clickable {@code <th>} that links to the same page with the
     * sort field set. Clicking the *currently active* column flips the
     * direction; clicking another column resets to ASC for that column.
     * Active column is shown with an arrow indicator.
     */
    private static String sortHeader(String label, Field col,
                                     Field active, Direction dir,
                                     String type, boolean onlyCritical) {
        boolean isActive = (col == active);
        Direction nextDir;
        if (isActive) nextDir = (dir == Direction.ASC) ? Direction.DESC : Direction.ASC;
        else          nextDir = Direction.ASC;

        StringBuilder href = new StringBuilder("alerts?sort=").append(col)
                                       .append("&dir=").append(nextDir);
        if (onlyCritical) href.append("&onlyCritical=true");
        if (type != null && !type.isEmpty()) href.append("&type=").append(type);

        String arrow = isActive ? (dir == Direction.ASC ? " ▲" : " ▼") : "";
        String style = isActive ? "color:#fff;" : "";

        return "<th><a href='" + href + "' "
             + "style='color:#22D3EE;text-decoration:none;display:block;" + style + "'>"
             + label + arrow + "</a></th>";
    }

    private static Field parseField(String s) {
        if (s == null || s.isEmpty()) return Field.TIME;
        try { return Field.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException ex) { return Field.TIME; }
    }
    private static Direction parseDir(String s) {
        if (s == null || s.isEmpty()) return Direction.DESC;
        try { return Direction.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException ex) { return Direction.DESC; }
    }
}
