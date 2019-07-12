package org.servantscode.commons.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.Organization;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.security.OrganizationContext;

import javax.ws.rs.NotFoundException;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class OrganizationDB extends DBAccess {
    private static Logger LOG = LogManager.getLogger(OrganizationDB.class);

    private SearchParser<Organization> searchParser;
    static HashMap<String, String> FIELD_MAP = new HashMap<>(8);

    static {
        FIELD_MAP.put("hostName","host_name");
    }

    public OrganizationDB() {
        this.searchParser = new SearchParser<>(Organization.class, "name", FIELD_MAP);
    }

    public int getCount(String search) {
        QueryBuilder query = count().from("organizations").search(searchParser.parse(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next()? 0: rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve organizations: ", e);
        }
    }

    public List<Organization> getOrganizations(String search, String sortField, int start, int count) {
        QueryBuilder query = selectAll().from("organizations").search(searchParser.parse(search))
                .sort(sortField).limit(count).offset(start);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            return processResults(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve organizations: ", e);
        }
    }

    public Organization getOrganization(String hostName) {
        QueryBuilder query = selectAll().from("organizations").where("host_name=?", hostName);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            return firstOrNull(processResults(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve organization: ", e);
        }
    }

    public Organization getOrganization(int id) {
        QueryBuilder query = selectAll().from("organizations").withId(id);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            return firstOrNull(processResults(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve organization: ", e);
        }
    }

    public Organization create(Organization organization) {
        String sql = "INSERT INTO organizations (name, host_name) VALUES (?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, organization.getName());
            stmt.setString(2, organization.getHostName());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create organization: " + organization.getName());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    organization.setId(rs.getInt(1));
            }

            return organization;
        } catch (SQLException e) {
            throw new RuntimeException("Could not create organization: " + organization.getName(), e);
        }
    }

    public Organization update(Organization organization) {
        String sql = "UPDATE organizations SET name=?, host_name=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, organization.getName());
            stmt.setString(2, organization.getHostName());
            stmt.setInt(3, organization.getId());

            stmt.executeUpdate();

            return organization;
        } catch (SQLException e) {
            throw new RuntimeException("Could not create organization: ", e);
        }
    }

    public void delete(Organization organization) {
        String sql = "DELETE FROM organizations WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, organization.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not clean stale organizations.", e);
        }
    }

    public void attchPhoto(int id, String guid) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE organizations SET photo_guid=? WHERE id=?");
        ){
            stmt.setString(1, guid);
            stmt.setInt(2, id);

            if(stmt.executeUpdate() == 0)
                throw new NotFoundException("Could not attach photo to organization: " + id);

        } catch (SQLException e) {
            throw new RuntimeException("Could not attach photo to organization: " + id, e);
        }
    }

    // ----- Private -----
    private List<Organization> processResults(ResultSet rs) throws SQLException {
        List<Organization> results = new LinkedList<>();
        while(rs.next()) {
            Organization org = new Organization();
            org.setId(rs.getInt("id"));
            org.setName(rs.getString("name"));
            org.setHostName(rs.getString("host_name"));
            org.setPhotoGuid(rs.getString("photo_guid"));
            results.add(org);
        }
        return results;
    }
}
