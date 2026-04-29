package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Core domain object representing a single security alert raised
 * by the Cybersecurity Alert Analyzer.
 *
 * Required fields per spec:
 *   - alertId
 *   - alertType
 *   - message
 *
 * A few extra fields (severity, sourceIp, username, timestamp) are
 * included to make the dashboards more realistic.
 */
public class SecurityAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Severity ordinals so we can quickly check "is this critical?". */
    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }

    private int            alertId;
    private String         alertType;
    private String         message;
    private Severity       severity;
    private String         sourceIp;
    private String         username;
    private LocalDateTime  createdAt;

    public SecurityAlert() {
        this.createdAt = LocalDateTime.now();
        this.severity  = Severity.LOW;
    }

    public SecurityAlert(int alertId, String alertType, String message) {
        this();
        this.alertId   = alertId;
        this.alertType = alertType;
        this.message   = message;
    }

    public SecurityAlert(int alertId, String alertType, String message,
                         Severity severity, String sourceIp, String username) {
        this(alertId, alertType, message);
        this.severity = severity;
        this.sourceIp = sourceIp;
        this.username = username;
    }

    // -------------------- Getters / Setters --------------------
    public int getAlertId()                         { return alertId; }
    public void setAlertId(int alertId)             { this.alertId = alertId; }

    public String getAlertType()                    { return alertType; }
    public void setAlertType(String alertType)      { this.alertType = alertType; }

    public String getMessage()                      { return message; }
    public void setMessage(String message)          { this.message = message; }

    public Severity getSeverity()                   { return severity; }
    public void setSeverity(Severity severity)      { this.severity = severity; }

    public String getSourceIp()                     { return sourceIp; }
    public void setSourceIp(String sourceIp)        { this.sourceIp = sourceIp; }

    public String getUsername()                     { return username; }
    public void setUsername(String username)        { this.username = username; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime t)       { this.createdAt = t; }

    /** Convenience: is this alert critical and should be shown immediately? */
    public boolean isCritical() {
        return severity == Severity.CRITICAL || severity == Severity.HIGH;
    }

    @Override
    public String toString() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "[#" + alertId + "] " + (createdAt == null ? "" : createdAt.format(f))
             + " " + severity + " " + alertType + " :: " + message;
    }
}
