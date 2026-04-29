# Cybersecurity Alert Analyzer

A complete Java project demonstrating a Cybersecurity Alert Analyzer system that monitors,
classifies and reports various security alerts (failed logins, suspicious file access,
network intrusions, malware events, privilege escalation etc.).

## Technologies Demonstrated
- Core Java (`substring()`, `StringBuffer`, Collections)
- Java AWT (Frame, Button, Choice, etc.)
- Java Swing (JFrame, JButton, JTable, JComboBox, JCheckBox)
- Java Events
  - `ActionListener` WITH lambda
  - `ActionListener` WITHOUT lambda (anonymous inner class / named class)
  - `ItemListener` WITH lambda
  - `ItemListener` WITHOUT lambda
  - `WindowListener`, `MouseListener`, `KeyListener`
- Java JDBC (SQLite — zero-install, single-file database)
- Java Servlets (HttpServlet, web.xml)

## Folder Structure
```
CyberSecurity Alert Analyzer/
├── README.md
├── sql/
│   └── schema.sql
├── webapp/
│   └── WEB-INF/
│       └── web.xml
└── src/
    ├── Main.java
    ├── model/
    │   └── SecurityAlert.java
    ├── util/
    │   ├── AlertExtractor.java
    │   └── AlertFormatter.java
    ├── db/
    │   └── DBConnection.java
    ├── dao/
    │   └── AlertDAO.java
    ├── ui/
    │   ├── LoginFrame.java
    │   ├── DashboardFrame.java
    │   ├── AlertMonitorFrame.java
    │   ├── FileScanFrame.java
    │   ├── NetworkAlertFrame.java
    │   ├── MalwareScanFrame.java
    │   └── CriticalAlertsFrame.java
    └── servlet/
        ├── LoginServlet.java
        └── AlertServlet.java
```

## How to Run

### Option 1 — Maven (recommended)
```
mvn clean package
java -cp "target/CyberAlertAnalyzer/WEB-INF/classes;target/CyberAlertAnalyzer/WEB-INF/lib/*" Main
```
A `cyber_alerts.db` SQLite file is auto-created in the working directory
on first run and seeded with sample users and alerts.

### Option 2 — Manual JAR (no Maven)
See `lib/README.txt` — drop `sqlite-jdbc-*.jar` into `lib/` and compile.

### Servlet portion
- Local: drop `target/CyberAlertAnalyzer.war` into Tomcat 9's `webapps/`
- Cloud: see `DEPLOY_RENDER.md` for one-click deploy to Render via Docker.

## Default Login (demo)
- Username: `admin`
- Password: `admin123`

Wrong credentials trigger an `INVALID_LOGIN` SecurityAlert and pop up a dialog.

## Alerts Included
- INVALID_LOGIN (wrong username/password)
- SUSPICIOUS_LOGIN (unusual time / location)
- FILE_ACCESS (unauthorized file read)
- MALWARE (antivirus detection)
- NETWORK_INTRUSION (port scan / brute force)
- PRIVILEGE_ESCALATION (sudo/admin abuse)
- DATA_EXFILTRATION (large outbound transfer)
- DDOS (high-frequency request flood)
