package util;

/**
 * Utility class that pulls the "alert type" out of a raw alert string
 * using {@link String#substring(int, int)} as required by the brief.
 *
 * Example raw alerts:
 *   "ALERT: Suspicious login attempt detected"
 *   "ALERT: Malware found on host WIN-DEV-01"
 *
 * The portion after "ALERT: " up to the next whitespace boundary is
 * treated as the type keyword. The function also classifies the alert
 * into a {@code SecurityAlert.Severity} based on simple keyword rules.
 */
public final class AlertExtractor {

    private AlertExtractor() { /* static utility */ }

    private static final String PREFIX = "ALERT:";

    /**
     * Extract the alert type from a raw alert message.
     *
     * Implementation uses {@code substring(int)} and {@code substring(int,int)}
     * exactly as the assignment requires. We do NOT use regex/split here
     * because the brief specifically asks for {@code substring()}.
     */
    public static String extractAlertType(String rawAlert) {
        if (rawAlert == null) return "UNKNOWN";

        String s = rawAlert.trim();

        // 1. If the string starts with "ALERT:" strip the prefix.
        int idx = s.toUpperCase().indexOf(PREFIX);
        if (idx >= 0) {
            s = s.substring(idx + PREFIX.length()).trim();   // <-- substring(int)
        }

        // 2. Take everything up to the first whitespace as the "type word".
        int space = s.indexOf(' ');
        String firstWord = (space < 0) ? s : s.substring(0, space); // <-- substring(int,int)

        return firstWord.toUpperCase();
    }

    /**
     * Map common keywords to a normalized alertType code that we store
     * in the database / display in the table.
     */
    public static String classifyAlertType(String rawAlert) {
        String upper = rawAlert == null ? "" : rawAlert.toUpperCase();

        if (upper.contains("LOGIN") && upper.contains("SUSPICIOUS")) return "SUSPICIOUS_LOGIN";
        if (upper.contains("LOGIN") && upper.contains("INVALID"))    return "INVALID_LOGIN";
        if (upper.contains("LOGIN"))                                  return "LOGIN_EVENT";
        if (upper.contains("MALWARE") || upper.contains("VIRUS")
                || upper.contains("TROJAN"))                          return "MALWARE";
        if (upper.contains("PORT SCAN")
                || upper.contains("INTRUSION"))                       return "NETWORK_INTRUSION";
        if (upper.contains("FILE") || upper.contains("UNAUTHORIZED")) return "FILE_ACCESS";
        if (upper.contains("SUDO") || upper.contains("PRIVILEGE"))    return "PRIVILEGE_ESCALATION";
        if (upper.contains("EXFILTRATION") || upper.contains("OUTBOUND"))
                                                                      return "DATA_EXFILTRATION";
        if (upper.contains("DDOS") || upper.contains("FLOOD"))        return "DDOS";

        // Fallback to the substring-based extraction
        return extractAlertType(rawAlert);
    }
}
