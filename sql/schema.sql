-- ============================================================
-- Cybersecurity Alert Analyzer - SQLite Schema
-- ============================================================
-- This script is auto-applied by db.DBConnection on first startup
-- if the database file does not yet contain the 'users' table.
-- You can also run it manually with the sqlite3 CLI:
--     sqlite3 cyber_alerts.db < sql/schema.sql
-- ============================================================

DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    username    TEXT NOT NULL UNIQUE,
    password    TEXT NOT NULL,
    role        TEXT NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (username, password, role) VALUES
    ('admin',   'admin123', 'ADMIN'),
    ('raj',     'raj@123',  'USER'),
    ('analyst', 'sec@2026', 'ANALYST');

DROP TABLE IF EXISTS alerts;
CREATE TABLE alerts (
    alert_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    alert_type  TEXT NOT NULL,
    severity    TEXT NOT NULL,
    message     TEXT NOT NULL,
    source_ip   TEXT,
    username    TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO alerts (alert_type, severity, message, source_ip, username) VALUES
 ('INVALID_LOGIN',        'HIGH',     'ALERT: Suspicious login attempt detected', '192.168.1.45', 'unknown'),
 ('MALWARE',              'CRITICAL', 'ALERT: Trojan.Gen detected in C:\temp\x.exe', '10.0.0.2',     'raj'),
 ('NETWORK_INTRUSION',    'CRITICAL', 'ALERT: Port scan from external host',         '203.0.113.7',  'n/a'),
 ('FILE_ACCESS',          'MEDIUM',   'ALERT: Unauthorized read on /etc/shadow',     '10.0.0.5',     'guest'),
 ('PRIVILEGE_ESCALATION', 'HIGH',     'ALERT: User attempted sudo without rights',   '10.0.0.8',     'analyst'),
 ('DATA_EXFILTRATION',    'CRITICAL', 'ALERT: 2GB outbound transfer to unknown IP',  '10.0.0.3',     'raj'),
 ('DDOS',                 'CRITICAL', 'ALERT: 50k req/sec on /login from botnet',    '0.0.0.0',      'n/a');
