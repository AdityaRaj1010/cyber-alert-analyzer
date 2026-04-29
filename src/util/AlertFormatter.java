package util;

import model.SecurityAlert;
import java.time.format.DateTimeFormatter;

/**
 * Builds nicely-formatted alert messages using {@link StringBuffer},
 * exactly as the assignment requires. We deliberately use StringBuffer
 * (not StringBuilder) to satisfy the spec.
 */
public final class AlertFormatter {

    private AlertFormatter() { /* static utility */ }

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Compact one-line format, used in tables and lists. */
    public static String formatLine(SecurityAlert a) {
        StringBuffer sb = new StringBuffer();              // <-- StringBuffer per spec
        sb.append("[#").append(a.getAlertId()).append("] ")
          .append(a.getCreatedAt() == null ? "----" : a.getCreatedAt().format(TS))
          .append(" | ").append(a.getSeverity())
          .append(" | ").append(a.getAlertType())
          .append(" :: ").append(a.getMessage());
        if (a.getSourceIp() != null && !a.getSourceIp().isEmpty()) {
            sb.append(" (from ").append(a.getSourceIp()).append(")");
        }
        return sb.toString();
    }

    /** Multi-line format used in the "details" pane and dialog popups. */
    public static String formatDetail(SecurityAlert a) {
        StringBuffer sb = new StringBuffer();
        sb.append("==================== SECURITY ALERT ====================\n");
        sb.append(" Alert ID   : ").append(a.getAlertId()).append('\n');
        sb.append(" Timestamp  : ")
          .append(a.getCreatedAt() == null ? "n/a" : a.getCreatedAt().format(TS)).append('\n');
        sb.append(" Severity   : ").append(a.getSeverity()).append('\n');
        sb.append(" Type       : ").append(a.getAlertType()).append('\n');
        sb.append(" User       : ").append(safe(a.getUsername())).append('\n');
        sb.append(" Source IP  : ").append(safe(a.getSourceIp())).append('\n');
        sb.append("--------------------------------------------------------\n");
        sb.append(" Message    : ").append(a.getMessage()).append('\n');
        sb.append("========================================================");
        return sb.toString();
    }

    /** HTML format - used by the servlet output. */
    public static String formatHtml(SecurityAlert a) {
        StringBuffer sb = new StringBuffer();
        sb.append("<tr>")
          .append("<td>").append(a.getAlertId()).append("</td>")
          .append("<td>").append(a.getCreatedAt() == null ? "" : a.getCreatedAt().format(TS)).append("</td>")
          .append("<td class='sev-").append(a.getSeverity()).append("'>").append(a.getSeverity()).append("</td>")
          .append("<td>").append(a.getAlertType()).append("</td>")
          .append("<td>").append(escape(a.getMessage())).append("</td>")
          .append("<td>").append(safe(a.getSourceIp())).append("</td>")
          .append("<td>").append(safe(a.getUsername())).append("</td>")
          .append("</tr>");
        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
