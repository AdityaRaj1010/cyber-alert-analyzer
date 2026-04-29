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

/**
 * HTTP servlet that handles login requests for the Cybersecurity Alert
 * Analyzer web front-end.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter w = resp.getWriter();
        w.println("<!doctype html><html><head><title>Cyber Shield - Login</title>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "*{box-sizing:border-box}"
                + "body{font-family:'Segoe UI',Arial,sans-serif;font-size:18px;"
                + "  background:radial-gradient(circle at top,#0E2A47 0%,#0B1424 60%);"
                + "  color:#E6EDF7;margin:0;min-height:100vh;display:flex;align-items:center;"
                + "  justify-content:center;}"
                + ".card{width:440px;background:#141F36;padding:38px 36px;"
                + "  border:1px solid #33466B;border-radius:12px;"
                + "  box-shadow:0 12px 40px rgba(0,0,0,.6);}"
                + "h1{font-size:32px;margin:0 0 4px;color:#22D3EE;letter-spacing:2px;}"
                + ".sub{color:#A1AEC4;margin-bottom:26px;font-size:16px;}"
                + "label{display:block;font-weight:bold;font-size:17px;margin:14px 0 6px;}"
                + "input{width:100%;padding:12px 14px;font-size:18px;border-radius:6px;"
                + "  border:1px solid #33466B;background:#0B1424;color:#E6EDF7;}"
                + "input:focus{outline:none;border-color:#22D3EE;}"
                + "button{width:100%;padding:14px;font-size:18px;font-weight:bold;"
                + "  background:#0691A8;color:#fff;border:0;border-radius:6px;cursor:pointer;"
                + "  margin-top:22px;letter-spacing:1px;}"
                + "button:hover{background:#22D3EE;color:#0B1424;}"
                + ".err{margin-top:18px;color:#FFC4C4;background:rgba(220,38,38,.18);"
                + "  padding:12px 14px;border-left:3px solid #DC2626;border-radius:4px;font-size:16px;}"
                + ".hint{margin-top:18px;font-size:15px;color:#A1AEC4;}"
                + "</style></head><body><div class='card'>"
                + "<h1>CYBER SHIELD</h1>"
                + "<div class='sub'>Security Operations Center · Sign In</div>"
                + "<form method='POST' action='login'>"
                + "<label>Username</label><input name='user' required autocomplete='username'/>"
                + "<label>Password</label><input name='pass' type='password' required autocomplete='current-password'/>"
                + "<button type='submit'>LOG IN</button></form>");
        String err = req.getParameter("err");
        if (err != null) w.println("<p class='err'>" + escape(err) + "</p>");
        w.println("<p class='hint'>Demo:  admin / admin123</p>");
        w.println("</div></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String user = req.getParameter("user");
        String pass = req.getParameter("pass");

        AlertDAO dao = new AlertDAO();
        String role = null;
        try { role = dao.validateLogin(user, pass); }
        catch (SQLException ex) {
            resp.sendRedirect("login?err=DB+unreachable");
            return;
        }

        if (role == null) {
            SecurityAlert a = new SecurityAlert(0, "INVALID_LOGIN",
                    "ALERT: Suspicious login attempt detected for user '" + user + "' via web",
                    Severity.HIGH, req.getRemoteAddr(), user);
            try { dao.insert(a); } catch (SQLException ignore) {}
            resp.sendRedirect("login?err=Invalid+username+or+password");
            return;
        }

        HttpSession s = req.getSession(true);
        s.setAttribute("user", user);
        s.setAttribute("role", role);
        resp.sendRedirect("alerts");
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("<","&lt;").replace(">","&gt;");
    }
}
