# Deploying to Render (SQLite edition)

This guide takes you from "I have the source code" to "the app is live
on the internet" in ~10 minutes. **No MySQL install, no cloud DB signup.**

The SQLite database file is created automatically inside the container
on first request and is seeded with the sample users and alerts.

> **Note about hosting:** Render is a *web-server* host. The Swing/AWT
> desktop frames (LoginFrame, DashboardFrame, AlertMonitorFrame, etc.)
> only run locally on your PC because they need a display. Render hosts
> the **servlet half** (LoginServlet + AlertServlet + LogoutServlet).

---

## Step 0 - Prerequisites
- A GitHub account
- Git installed locally
- Docker Desktop is **not** required (Render builds the image for you)

That's it. You don't need Java, Maven, MySQL, or Tomcat installed
locally just to deploy. (You only need them if you also want to run
the desktop GUI from your PC.)

---

## Step 1 - Push the project to GitHub

```bash
cd "CyberSecurity Alert Analyzer"
git init
git add .
git commit -m "Cybersecurity Alert Analyzer (SQLite)"
git branch -M main
git remote add origin https://github.com/<your-username>/cyber-alert-analyzer.git
git push -u origin main
```

---

## Step 2 - Deploy on Render

1. Go to https://dashboard.render.com → **New + → Web Service**.
2. Click **"Connect"** next to your GitHub repo.
3. Render auto-detects the **Dockerfile**. Confirm:
   - **Runtime:** Docker
   - **Plan:** Free
   - **Region:** any (Oregon by default)
4. Click **Create Web Service**.
5. Wait ~3-5 min for the first build. When the status badge says
   **Live**, open the URL Render shows (e.g.
   `https://cyber-alert-analyzer.onrender.com/login`).

That's the entire deployment.

### Demo logins (auto-created on first start)

| Username | Password   | Role     |
|----------|------------|----------|
| admin    | admin123   | ADMIN    |
| raj      | raj@123    | USER     |
| analyst  | sec@2026   | ANALYST  |

Try a wrong password to see the **INVALID_LOGIN** SecurityAlert get
generated and stored in SQLite. After logging in, the dashboard at
`/alerts` reads from SQLite via JDBC and renders rows using
StringBuffer-built HTML.

---

## Step 3 (optional) - Persistent data on Render

The free tier wipes the container's disk on every deploy. Sample data
re-seeds automatically, but anything you add through the app is lost.

To keep data permanently:
1. Upgrade the service to the **Starter** plan ($7/mo).
2. Open the **Disks** tab → **Add Disk**:
   - Mount path: `/var/data`
   - Size: 1 GB
3. Open the **Environment** tab → add variable
   - `DB_FILE` = `/var/data/cyber_alerts.db`
4. Save → service redeploys → DB now lives on the persistent disk.

(Or uncomment the `disk:` and `envVars:` block in `render.yaml` and
push.)

---

## Step 4 - Run the desktop GUI locally (Swing + AWT half)

The desktop GUIs are not on Render - run them from your PC:

### Option A - using Maven
```bash
cd "CyberSecurity Alert Analyzer"
mvn clean package
java -cp "target/CyberAlertAnalyzer/WEB-INF/classes;target/CyberAlertAnalyzer/WEB-INF/lib/*" Main
```
On Linux/macOS replace `;` with `:`.

### Option B - manual jar download (no Maven)
1. Download `sqlite-jdbc-3.45.3.0.jar`
   from https://github.com/xerial/sqlite-jdbc/releases
   and put it in `lib/`.
2. Compile and run:
   ```bash
   javac -d out -cp "lib/sqlite-jdbc-3.45.3.0.jar" src/model/*.java src/util/*.java src/db/*.java src/dao/*.java src/ui/*.java src/servlet/*.java src/Main.java
   java  -cp "out;lib/sqlite-jdbc-3.45.3.0.jar" Main
   ```

The desktop app creates / uses `cyber_alerts.db` in whatever folder you
launch it from. The first run prints "Schema created with sample data."

### Connecting the desktop app to the SAME database as Render?
Not really practical - Render's container disk is private. Two options:
- Run the desktop app standalone with its own local SQLite file (recommended for the demo).
- Or upgrade Render to a paid plan with a persistent disk and use a remote MySQL/Postgres instead - but then you'd switch back from SQLite.

---

## What runs where

| Component                              | Local desktop | Render web |
|----------------------------------------|:-------------:|:----------:|
| LoginFrame, DashboardFrame             |      yes      |     no     |
| AlertMonitorFrame (pure AWT)           |      yes      |     no     |
| FileScan / Network / Malware frames    |      yes      |     no     |
| LoginServlet / AlertServlet / Logout   |  yes (Tomcat) |    yes     |
| SQLite DB (`cyber_alerts.db`)          |      yes      |    yes     |

All assignment requirements - Swing, AWT, Events, ActionListener
with/without lambda, ItemListener with/without lambda, JDBC, Servlet -
are covered. Render simply hosts the servlet portion; the desktop
portion you run locally for screenshots / demo videos.
