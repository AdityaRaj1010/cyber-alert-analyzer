package dao;

import db.DBConnection;
import model.SecurityAlert;
import model.SecurityAlert.Severity;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the {@code alerts} table.
 *
 * Demonstrates JDBC: PreparedStatement, ResultSet, transaction handling
 * and proper resource management with try-with-resources.
 */
public class AlertDAO {

    /** Insert a new alert; returns the generated alert_id. */
    public int insert(SecurityAlert a) throws SQLException {
        String sql = "INSERT INTO alerts(alert_type, severity, message, source_ip, username) "
                   + "VALUES (?,?,?,?,?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getAlertType());
            ps.setString(2, a.getSeverity().name());
            ps.setString(3, a.getMessage());
            ps.setString(4, a.getSourceIp());
            ps.setString(5, a.getUsername());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    a.setAlertId(rs.getInt(1));
                    return a.getAlertId();
                }
            }
        }
        return -1;
    }

    /** Returns every alert, newest first. */
    public List<SecurityAlert> findAll() throws SQLException {
        String sql = "SELECT alert_id, alert_type, severity, message, source_ip, username, created_at "
                   + "FROM alerts ORDER BY created_at DESC";
        return query(sql);
    }

    /** Returns only HIGH or CRITICAL alerts. */
    public List<SecurityAlert> findCritical() throws SQLException {
        String sql = "SELECT alert_id, alert_type, severity, message, source_ip, username, created_at "
                   + "FROM alerts WHERE severity IN ('HIGH','CRITICAL') "
                   + "ORDER BY created_at DESC";
        return query(sql);
    }

    /** Returns alerts of a particular type. */
    public List<SecurityAlert> findByType(String type) throws SQLException {
        String sql = "SELECT alert_id, alert_type, severity, message, source_ip, username, created_at "
                   + "FROM alerts WHERE alert_type=? ORDER BY created_at DESC";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        }
    }

    /** Look up a single alert by id. Returns null if not found. */
    public SecurityAlert findById(int id) throws SQLException {
        String sql = "SELECT alert_id, alert_type, severity, message, source_ip, username, created_at "
                   + "FROM alerts WHERE alert_id=?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<SecurityAlert> list = mapAll(rs);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    /** Validate user credentials. Returns the user's role or null. */
    public String validateLogin(String username, String password) throws SQLException {
        String sql = "SELECT role FROM users WHERE username=? AND password=?";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("role") : null;
            }
        }
    }

    // ---------------- private helpers ----------------
    private List<SecurityAlert> query(String sql) throws SQLException {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapAll(rs);
        }
    }

    private static LocalDateTime parseSqliteTs(String s) {
        if (s == null || s.isEmpty()) return LocalDateTime.now();
        try { return LocalDateTime.parse(s.replace(' ', 'T')); }
        catch (Exception e1) {
            try { return LocalDateTime.parse(s,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
            catch (Exception e2) { return LocalDateTime.now(); }
        }
    }

    private List<SecurityAlert> mapAll(ResultSet rs) throws SQLException {
        List<SecurityAlert> out = new ArrayList<>();
        while (rs.next()) {
            SecurityAlert a = new SecurityAlert();
            a.setAlertId(rs.getInt("alert_id"));
            a.setAlertType(rs.getString("alert_type"));
            a.setMessage(rs.getString("message"));
            try {
                a.setSeverity(Severity.valueOf(rs.getString("severity")));
            } catch (IllegalArgumentException ex) {
                a.setSeverity(Severity.LOW);
            }
            a.setSourceIp(rs.getString("source_ip"));
            a.setUsername(rs.getString("username"));
            // SQLite stores TIMESTAMP as TEXT; getTimestamp() may throw,
            // so fall back to parsing the string ourselves.
            try {
                Timestamp t = rs.getTimestamp("created_at");
                a.setCreatedAt(t == null ? LocalDateTime.now() : t.toLocalDateTime());
            } catch (SQLException sqle) {
                String s = rs.getString("created_at");
                a.setCreatedAt(parseSqliteTs(s));
            }
            out.add(a);
        }
        return out;
    }
}
