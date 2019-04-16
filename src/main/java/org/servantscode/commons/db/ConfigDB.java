package org.servantscode.commons.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConfigDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(ConfigDB.class);

    public String getConfiguration(String config) {
        try(Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT value FROM configuration WHERE config=?")) {

            stmt.setString(1, config);
            try(ResultSet rs = stmt.executeQuery()) {
                if(rs.next())
                    return rs.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve configuration property: " + config, e);
        }
        return null;
    }

    public Map<String, String> getConfigurations(String configPrefix) {
        try(Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM configuration WHERE config LIKE ?")) {

            stmt.setString(1, configPrefix + "%");
            try(ResultSet rs = stmt.executeQuery()) {
                Map<String, String> results = new HashMap<>();
                while(rs.next())
                    results.put(rs.getString("config"), rs.getString("value"));
                LOG.debug("Retrieved " + results.size() + " properties starting with " + configPrefix);
                return results;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve configuration properties: " + configPrefix, e);
        }
    }

    public void patchConfigurations(Map<String, String> configs) {
        try(Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO configuration VALUES(?, ?) " +
                                                               "ON CONFLICT (config) DO UPDATE SET VALUE=EXCLUDED.value"))
        {
            configs.forEach((config, value) -> {
                    try {
                        stmt.setString(1, config);
                        stmt.setString(2, value);
                        stmt.addBatch();
                    } catch (SQLException e) {
                        throw new RuntimeException("Could not patch configuration properties.", e);
                    }
                });

            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Could not patch configuration properties.", e);
        }
    }

    public void deleteConfigurations(Set<String> configs) {
        try(Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM configuration WHERE config=?"))
        {
            configs.forEach((config) -> {
                try {
                    stmt.setString(1, config);
                    stmt.addBatch();
                } catch (SQLException e) {
                    throw new RuntimeException("Could not patch configuration properties.", e);
                }
            });

            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Could not patch configuration properties.", e);
        }
    }
}
