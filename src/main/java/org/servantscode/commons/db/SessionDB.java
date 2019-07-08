package org.servantscode.commons.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.Session;
import org.servantscode.commons.search.QueryBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class SessionDB extends DBAccess {
    private static Logger LOG = LogManager.getLogger(SessionDB.class);

    public List<Session> getSessions(int personId, int orgId) {
        QueryBuilder query = selectAll().from("sessions").where("person_id=?", personId).where("org_id=?", orgId);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            return processResults(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve sessions: ", e);
        }
    }

    public Session getSessionByToken(String token) {
        QueryBuilder query = selectAll().from("sessions").where("token=?", token);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            return firstOrNull(processResults(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve sessions: ", e);
        }
    }

    public void createSession(Session session) {
        String sql = "INSERT INTO sessions (person_id, org_id, token, expiration, ip) VALUES (?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, session.getPersonId());
            stmt.setInt(2, session.getOrgId());
            stmt.setString(3, session.getToken());
            stmt.setTimestamp(4, convert(session.getExpiration()));
            stmt.setString(5, session.getIp());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Could not create session: ", e);
        }
    }

    public void updateCallingIp(Session session) {
        String sql = "UPDATE sessions SET ip=? WHERE token=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, session.getIp());
            stmt.setString(2, session.getToken());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Could not create session: ", e);
        }
    }

    public void deleteSession(Session session) {
        String sql = "DELETE FROM sessions WHERE token=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, session.getToken());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not clean stale sessions.", e);
        }
    }

    public void deleteAllSessions(int personId) {
        String sql = "DELETE FROM sessions WHERE person_id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, personId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not clean stale sessions.", e);
        }
    }

    public void clearExpiredTokens() {
        String sql = "DELETE FROM sessions WHERE expiration < now() - INTERVAL '1 day'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            LOG.info("Cleaned up %d stale sessions.", stmt.executeQuery());
        } catch (SQLException e) {
            throw new RuntimeException("Could not clean stale sessions.", e);
        }
    }

    // ----- Private -----
    private List<Session> processResults(ResultSet rs) throws SQLException {
        List<Session> results = new LinkedList<>();
        while(rs.next()) {
            Session s = new Session();
            s.setPersonId(rs.getInt("person_id"));
            s.setOrgId(rs.getInt("org_id"));
            s.setToken(rs.getString("token"));
            s.setExpiration(convert(rs.getTimestamp("expiration")));
            s.setIp(rs.getString("ip"));
            results.add(s);
        }
        return results;
    }
}

