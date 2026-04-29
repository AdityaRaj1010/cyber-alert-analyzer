package servlet;

import dao.AlertDAO;
import model.SecurityAlert;
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
import java.util.List;

/**
 * Renders an HTML dashboard of security alerts pulled from SQLite via
 * {@link AlertDAO}.
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
                + "  border-bottom:1px solid #33466B;}"
                + "header h1{margin:0;font-size:26px;color:#22D3EE;letter-spacing:1.5px;}"
                + "header .who{font-size:16px;color:#A1AEC4;}"
                + "header a{color:#FFC4C4;text-decoration:none;font-weight:bold;margin-left:8px;}"
                + "main{padding:24px 32px;}"
                + ".filters{margin-bottom:18px;}"
                + ".filters a{display:inline-block;background:#141F36;color:#22D3EE;"
                + "  padding:9px 16px;border-radius:20px;margin:4px 6px 4px 0;text-decoration:none;"
                + "  font-size:15px;font-weight:bold;border:1px solid #33466B;}"
                + ".filters a:hover{background:#0691A8;color:#fff;}"
                + "table{width:100%;border-collapse:collapse;background:#141F36;border-radius:8px;overflow:hidden;}"
                + "th,td{padding:12px 14px;text-align:left;font-size:15px;border-bottom:1px solid #33466B;}"
                + "th{background:#1B2944;color:#22D3EE;font-size:16px;letter-spacing:.5px;text-transform:uppercase;}"
                + "tr:hover td{background:#1B2944;}"
                + ".sev-CRITICAL{color:#DC2626;font-weight:bold;}"
                + ".sev-HIGH    {color:#EF4444;font-weight:bold;}"
                + ".sev-MEDIUM  {color:#F59E0B;font-weight:bold;}"
                + ".sev-LOW     {color:#10B981;font-weight:bold;}"
                + ".empty{color:#A1AEC4;font-style:italic;padding:30px;text-align:center;font-size:16px;}"
                + "</style></head><body>");
        w.println("<header><h1>CYBER SHIELD :: Security Operations Center</h1>"
                + "<div class='who'>Logged in as <b style='color:#10B981'>"
                + escape((String) s.getAttribute("user")) + "</b> ("
                + escape((String) s.getAttribute("role")) + ")"
                + " <a href='logout'>Logout</a></div></header>");
        w.println("<main>");
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

        w.println("<table><thead><tr>"
                + "<th>ID</th><th>Time</th><th>Severity</th><th>Type</th>"
                + "<th>Message</th><th>Source IP</th><th>User</th>"
                + "</tr></thead><tbody>");

        try {
            AlertDAO dao = new AlertDAO();
            List<SecurityAlert> data;
            if (onlyCritical)              data = dao.findCritical();
            else if (type != null && !type.isEmpty()) data = dao.findByType(type);
            else                            data = dao.findAll();

            for (SecurityAlert a : data) {
                w.println(AlertFormatter.formatHtml(a));
            }
            if (data.isEmpty())
                w.println("<tr><td colspan='7' class='empty'>No alerts found.</td></tr>");
        } catch (SQLException ex) {
            w.println("<tr><td colspan='7' class='empty'>DB error: "
                    + escape(ex.getMessage()) + "</td></tr>");
        }

        w.println("</tbody></table></main></body></html>");
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("<","&lt;").replace(">","&gt;");
    }
}
