package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Centralized JDBC connection factory for the Cybersecurity Alert Analyzer.
 *
 * Backed by SQLite - no separate database server required.
 *
 * Database file location is resolved in this order:
 *   1. Environment variable  DB_FILE  (e.g. /var/data/cyber_alerts.db)
 *   2. Environment variable  DB_URL   (full JDBC URL)
 *   3. Default               cyber_alerts.db (in working directory)
 *
 * The first call to {@link #get()} will automatically create the
 * database file and run the schema if it doesn't already exist.
 */
public final class DBConnection {

    private static final String DRIVER       = "org.sqlite.JDBC";
    private static final String DEFAULT_FILE = "cyber_alerts.db";

    private static volatile boolean initialized = false;

    private DBConnection() { /* utility */ }

    /** Pick env var, system property, or fallback. */
    private static String pick(String envKey, String sysKey, String fallback) {
        String v = System.getenv(envKey);
        if (v != null && !v.isEmpty()) return v;
        v = System.getProperty(sysKey);
        return (v != null && !v.isEmpty()) ? v : fallback;
    }

    private static String jdbcUrl() {
        String full = pick("DB_URL", "db.url", null);
        if (full != null) return full;
        String file = pick("DB_FILE", "db.file", DEFAULT_FILE);
        return "jdbc:sqlite:" + file;
    }

    /** Lazily registers the JDBC driver and opens a fresh connection. */
    public static Connection get() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver missing on classpath", e);
        }
        Connection c = DriverManager.getConnection(jdbcUrl());
        ensureSchema(c);
        return c;
    }

    /** Quick connectivity check called at startup. */
    public static boolean ping() {
        try (Connection c = get()) {
            return c != null && !c.isClosed();
        } catch (SQLException ex) {
            System.err.println("[DB] ping failed: " + ex.getMessage());
            return false;
        }
    }

    /**
     * If the database is empty (no 'users' table) run the embedded
     * schema. We embed the SQL inline so we don't depend on classpath
     * resource lookup, which can be fiddly inside a WAR.
     */
    private static synchronized void ensureSchema(Connection c) {
        if (initialized) return;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='users'")) {
            if (rs.next()) {
                initialized = true;
                return;
            }
        } catch (SQLException ex) {
            // fall through and try to create anyway
        }

        System.out.println("[DB] First-time setup: creating SQLite schema...");
        try (Statement st = c.createStatement()) {
            for (String sql : SCHEMA) {
                st.executeUpdate(sql);
            }
            initialized = true;
            System.out.println("[DB] Schema created with sample data.");
        } catch (SQLException ex) {
            System.err.println("[DB] Schema creation failed: " + ex.getMessage());
        }
    }

    /** SQLite DDL + seed data, mirroring sql/schema.sql. */
    private static final String[] SCHEMA = {
        "CREATE TABLE IF NOT EXISTS users (" +
        "  id         INTEGER PRIMARY KEY AUTOINCREMENT," +
        "  username   TEXT NOT NULL UNIQUE," +
        "  password   TEXT NOT NULL," +
        "  role       TEXT NOT NULL DEFAULT 'USER'," +
        "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",

        "CREATE TABLE IF NOT EXISTS alerts (" +
        "  alert_id   INTEGER PRIMARY KEY AUTOINCREMENT," +
        "  alert_type TEXT NOT NULL," +
        "  severity   TEXT NOT NULL," +
        "  message    TEXT NOT NULL," +
        "  source_ip  TEXT," +
        "  username   TEXT," +
        "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",

        "INSERT INTO users (username,password,role) VALUES " +
        "('admin','admin123','ADMIN')," +
        "('raj','raj@123','USER')," +
        "('analyst','sec@2026','ANALYST')",

        "INSERT INTO alerts (alert_type,severity,message,source_ip,username) VALUES " +
        "('INVALID_LOGIN','HIGH','ALERT: Suspicious login attempt detected','192.168.1.45','unknown')," +
        "('MALWARE','CRITICAL','ALERT: Trojan.Gen detected in C:\\temp\\x.exe','10.0.0.2','raj')," +
        "('NETWORK_INTRUSION','CRITICAL','ALERT: Port scan from external host','203.0.113.7','n/a')," +
        "('FILE_ACCESS','MEDIUM','ALERT: Unauthorized read on /etc/shadow','10.0.0.5','guest')," +
        "('PRIVILEGE_ESCALATION','HIGH','ALERT: User attempted sudo without rights','10.0.0.8','analyst')," +
        "('DATA_EXFILTRATION','CRITICAL','ALERT: 2GB outbound transfer to unknown IP','10.0.0.3','raj')," +
        "('DDOS','CRITICAL','ALERT: 50k req/sec on /login from botnet','0.0.0.0','n/a')"
    };
}
