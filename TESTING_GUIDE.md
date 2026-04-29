# Testing & Demo Guide
## How to demonstrate every alert type to your professor

This guide gives you a step-by-step walkthrough you can follow live.
For each alert type it tells you:
- which window to open,
- exactly which buttons / inputs to use,
- what the professor will see,
- which Java concept (substring, StringBuffer, Listener, JDBC, etc.)
  is being demonstrated.

The 5 spec tasks (substring extraction, SecurityAlert class, list
storage, critical filter, StringBuffer formatting) are all printed
to the **console** at startup before the GUI opens — keep the
terminal visible so your professor can see them too.

---

## How to launch

```
mvn clean package
java -cp "target/CyberAlertAnalyzer/WEB-INF/classes;target/CyberAlertAnalyzer/WEB-INF/lib/*" Main
```

You'll see in the console:
```
===== Cybersecurity Alert Analyzer Startup Demo =====
Task 1) substring -> alertType = SUSPICIOUS
Task 3) stored 3 alerts in ArrayList
Task 4) critical alerts:
   * [#1] ... HIGH SUSPICIOUS_LOGIN :: ALERT: Suspicious login attempt detected ...
   * [#2] ... CRITICAL MALWARE :: ALERT: Trojan.Gen detected ...
Task 5) StringBuffer-formatted alert:
==================== SECURITY ALERT ====================
 Alert ID   : 1
 ...
========================================================
[DB] connection check: OK
```
**Show this terminal output first** — it covers tasks 1, 2, 3, 4, 5
of the brief at a glance.

The login window then opens.

---

## DEMO 1 — INVALID_LOGIN  (wrong-password alert)

**Window:** Login page (the one that opens at startup).

**Steps:**
1. Type username `wronguser`, password `wrongpass` → click **LOG IN**.
2. A red **SECURITY ALERT — Invalid Login** popup appears showing the
   `SecurityAlert` formatted with `StringBuffer` (Alert ID, Timestamp,
   Severity, Type, User, Source IP, Message).
3. The status text under the form turns red: "Invalid credentials.
   Attempt #1 of 3".
4. Repeat with another wrong password → "Attempt #2 of 3".
5. Third wrong attempt → another popup says "Account locked after
   3 failed attempts" and the program exits.

**What's being shown:**
- ActionListener **WITHOUT lambda** (named inner class `LoginAction`)
- KeyListener via `KeyAdapter` (Enter submits the form)
- WindowListener via `WindowAdapter` (close confirmation)
- JDBC `SELECT` against the `users` table to validate
- JDBC `INSERT` writing the alert into the `alerts` table
- `StringBuffer` building the popup message
- `SecurityAlert` class with `alertId`, `alertType`, `message`

**To finish this demo:** restart and log in correctly with
`admin / admin123` to enter the dashboard.

---

## DEMO 2 — Dashboard view  (JDBC + Filters)

**Window:** Dashboard (opens after successful login).

**Steps:**
1. Show the table — every row was just read from SQLite via
   `AlertDAO.findAll()` (JDBC).
2. Click **Reload** → demonstrates ActionListener WITH lambda.
3. Drop down **Filter type** → pick `MALWARE` → only malware rows
   remain. (ItemListener WITH lambda on JComboBox.)
4. Tick **Show only HIGH / CRITICAL** → table filters again.
   (ItemListener WITH lambda on JCheckBox.)
5. Click any row → **View Details** → shows StringBuffer-formatted
   detail popup.

---

## DEMO 3 — Critical Alerts window

**Window:** Toolbar → **Critical Only**

**Steps:**
1. The window shows only HIGH + CRITICAL alerts (red header).
2. **Double-click** any row → MouseListener fires → detail dialog.
3. Click **Refresh** to re-query JDBC.

**What's being shown:**
- `MouseListener` via `MouseAdapter`
- `ActionListener` WITH lambda (Refresh / View Selected / Close)
- `List<SecurityAlert>` iteration (the spec's "store in a suitable
  data structure" + "display critical alerts")

---

## DEMO 4 — File Scanner  (FILE_ACCESS + MALWARE)

**Window:** Toolbar → **File Scan**

**Pre-step (one-time setup, do this BEFORE the demo):**
Create a test folder somewhere on your PC, e.g. `C:\demo_test\`,
and put these dummy files in it (empty files are fine):
```
notes.txt
report.docx
secret_token.txt        <- triggers FILE_ACCESS alert
shadow.bak              <- triggers FILE_ACCESS alert
update.exe              <- triggers MALWARE alert
install.bat             <- triggers MALWARE alert
script.ps1              <- triggers MALWARE alert
```

You can create them in PowerShell:
```powershell
mkdir C:\demo_test
cd C:\demo_test
ni notes.txt, report.docx, secret_token.txt, shadow.bak, update.exe, install.bat, script.ps1
```

**Demo steps:**
1. Click **Browse…** → pick `C:\demo_test`.
2. Pick **Scan Type:** Full Scan (ItemListener WITH lambda fires).
3. Tick **Deep Scan (recursive)** (ItemListener WITH lambda fires).
4. Click **Start Scan**.
5. The console fills with `ok` lines for safe files, **MALWARE** lines
   for `.exe/.bat/.ps1`, and **FILE_ACCESS** lines for files containing
   `passwd / shadow / secret / token`.
6. Switch to the Dashboard, click **Reload** — the new MALWARE and
   FILE_ACCESS rows are now in the database.

---

## DEMO 5 — Network Monitor  (DDOS + NETWORK_INTRUSION + DATA_EXFILTRATION)

**Window:** Toolbar → **Network Monitor**

**Steps:**
1. **Click "Simulate Port Scan"** → instantly raises a CRITICAL
   `NETWORK_INTRUSION` alert + popup. (ActionListener WITH lambda.)
2. **Click "Simulate Data Exfiltration"** → CRITICAL `DATA_EXFILTRATION`
   alert + popup.
3. Set **threshold** to `200`, click **Start Monitor**.
   - Background thread fires fake traffic 0-5000 req/sec every 800ms.
   - Whenever it goes over 200, a CRITICAL `DDOS` alert is raised
     and a popup pops up.
   - The progress bar shows current req/sec.
4. Click **Stop**.

**What's being shown:**
- ActionListener **WITHOUT lambda** (anonymous class on Start button)
- ActionListener WITH lambda (Stop, Port Scan, Exfiltration buttons)
- Background `Thread` for live monitoring
- JDBC `INSERT` for every alert raised
- `StringBuffer` formatting in the popup

---

## DEMO 6 — Malware Scanner  (MALWARE)

**Window:** Toolbar → **Malware Scanner**

**Steps:**
1. Pick **Quick / Full / Rootkit** radio → each click prints a line.
   (ItemListener WITHOUT lambda — single named-class instance shared
    by all three radios.)
2. Toggle **Auto-quarantine** → prints `[i] Auto-quarantine = ...`
   (ItemListener WITH lambda.)
3. Click **Start Scan**.
   - SwingWorker runs in the background, progress bar advances.
   - Random "threat detected" lines appear (Trojan.Generic,
     Worm.AutoRun.A, Backdoor.Mirai, Ransomware.Locky, etc.).
   - Each detection writes a CRITICAL MALWARE alert via JDBC.

---

## DEMO 7 — AWT Live Monitor  (all alert types, AWT-only)

**Window:** Toolbar → **AWT Live Monitor**

This window uses **pure java.awt** (Frame, Panel, Button, Choice,
Checkbox, TextArea — no Swing).

**Steps:**
1. Click **Generate Random Alert** several times.
   - Each click picks a random alert template, runs `AlertExtractor.
     classifyAlertType` (which uses `substring()`), creates a
     `SecurityAlert`, inserts via JDBC, prints the StringBuffer-
     formatted line in the green console.
   - Hits all 7 alert types randomly.
2. Tick **Auto-generate alerts every 3s** → background thread fires
   one alert every 3 seconds. (ItemListener WITHOUT lambda.)
3. Change the **Type** Choice dropdown → "[INFO] Now generating ..."
   line printed. (ItemListener WITHOUT lambda.)
4. Untick the auto checkbox → thread stops.

**What's being shown:**
- Pure Java AWT
- ActionListener WITHOUT lambda (named inner classes
  `GenerateAction`, `ClearAction`, `CloseAction`)
- ItemListener WITHOUT lambda (named inner classes `AutoToggle`,
  `TypeChange`)
- WindowListener via `WindowAdapter`
- `substring()` extraction in `AlertExtractor`

---

## DEMO 8 — Servlet (web side)  ★ optional but impressive

If you also want to show the servlet/JSP side:

1. Build the WAR: `mvn clean package`.
2. Drop `target/CyberAlertAnalyzer.war` into Tomcat 9's `webapps/`.
3. Start Tomcat. Browse to
   `http://localhost:8080/CyberAlertAnalyzer/login`.
4. Log in as `admin / admin123` → see the same alerts in the
   browser, served by `AlertServlet`, formatted with StringBuffer
   via `AlertFormatter.formatHtml`.
5. Click filter pills (Critical only / Malware / Network / DDoS).
6. Type a wrong password to show the web side also raises an
   `INVALID_LOGIN` alert.
7. Click **Simulator** in the header (see Demo 9) to trigger every
   other alert type from the browser.
8. Then go back to the desktop dashboard, click **Reload**, and your
   professor will see the same alerts that came in from the browser
   appear in the desktop table — proving both UIs share the same
   SQLite DB via JDBC.

---

## DEMO 9 — Web Simulator  (every alert type, in the browser)

**Page:** click **Simulator** in the top nav (or visit `/simulate`).

This is the web equivalent of the desktop Network Monitor + File Scan
+ Malware Scanner + AWT Live Monitor frames. Each card calls
`AlertDAO.insert()` through JDBC and writes a row to the same SQLite
table that the dashboard reads from.

**Quick triggers (single alert per click):**
1. Click **Trigger** under "Suspicious Login" → SUSPICIOUS_LOGIN row
2. Click **Trigger** under "Malware Detected"  → MALWARE row
3. Click **Trigger** under "Port Scan"          → NETWORK_INTRUSION row
4. Click **Trigger** under "File Access Violation" → FILE_ACCESS row
5. Click **Trigger** under "Privilege Escalation" → PRIVILEGE_ESCALATION
6. Click **Trigger** under "Data Exfiltration"  → DATA_EXFILTRATION row
7. Click **Trigger** under "DDoS Burst"         → DDOS row

After each click a green flash bar appears with a **View alerts →**
link. Click it to jump to the dashboard and confirm the new row.

**Compound simulators (multiple alerts per click):**
- **File Scanner** card – type a path like `C:\\update.exe` → MALWARE,
  or `secret_token.txt` → FILE_ACCESS. Plain `notes.txt` → "clean".
- **Network Monitor Burst** card – pick a count (e.g. 10) → that many
  random DDoS / intrusion / exfiltration alerts inserted at once.
- **Malware Scanner** card – pick Quick / Full / Rootkit → 3 / 5 / 7
  random malware detections inserted.
- **Random Alert Stream** card – one click → 8 mixed alerts spanning
  every category (best for showing the dashboard's filters in action).

This is the demo path you should use **on the deployed Render site**
since the desktop frames don't run in a browser.

---

## Quick "everything in 60 seconds" demo script

If your professor only wants a fast tour, do this in order:

1. Show terminal — points out tasks 1–5 from the brief.
2. Login window → wrong password → SECURITY ALERT popup.
3. Login as admin → dashboard, point at JDBC-loaded table.
4. Toggle "Show only HIGH/CRITICAL" → table filters.
5. **Network Monitor** → click "Simulate Port Scan" → popup.
6. **Network Monitor** → click "Simulate Data Exfiltration" → popup.
7. **AWT Live Monitor** → click "Generate Random Alert" 4-5 times
   → 4-5 alert lines in the console.
8. **Critical Only** window → double-click a row → detail popup.
9. Back to Dashboard → **Reload** → all the alerts you just
   generated are now in the table (JDBC works, persistence works).

That's every alert type and every required Java concept in
under two minutes.

---

## Mapping: brief requirement → where it's demonstrated

| Requirement                                | Where to look                                            |
|--------------------------------------------|----------------------------------------------------------|
| `substring()` to extract alert type        | `AlertExtractor.extractAlertType` (called by AWT Monitor)|
| `SecurityAlert` class (id/type/message)    | `model/SecurityAlert.java`                               |
| Stored in suitable data structure          | `ArrayList` in `Main`, `List` returned by `AlertDAO`     |
| Display critical alerts                    | `CriticalAlertsFrame` + dashboard "Show only HIGH/CRIT"  |
| `StringBuffer` for formatting              | `AlertFormatter` (every method)                          |
| ActionListener WITHOUT lambda              | `LoginFrame.LoginAction`, `AlertMonitorFrame.GenerateAction`, `NetworkAlertFrame` start button |
| ActionListener WITH lambda                 | `DashboardFrame`, `FileScanFrame`, `CriticalAlertsFrame` |
| ItemListener WITHOUT lambda                | `AlertMonitorFrame.AutoToggle`/`TypeChange`, `MalwareScanFrame` radios |
| ItemListener WITH lambda                   | `DashboardFrame` (combo + checkbox), `FileScanFrame`     |
| Java AWT                                   | `AlertMonitorFrame` (Frame, Panel, Button, Choice…)      |
| Java Swing                                 | every other UI frame                                     |
| Java Events                                | listeners + `WindowAdapter`, `KeyAdapter`, `MouseAdapter`|
| Java JDBC                                  | `db/DBConnection.java`, `dao/AlertDAO.java`              |
| Java Servlet                               | `servlet/LoginServlet.java`, `AlertServlet.java`, `LogoutServlet.java` |
| Login page raises alert on wrong creds     | `LoginFrame` and `LoginServlet`                          |
