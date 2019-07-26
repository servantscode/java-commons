package org.servantscode.commons.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractDBUpgrade extends DBAccess implements ServletContextListener {
    private static final Logger LOG = LogManager.getLogger(AbstractDBUpgrade.class);

    //Do this automatically on service start up. Referenced in each services' web.xml.
    public void contextInitialized(ServletContextEvent arg0)
    {
        doUpgrade();
    }

    public void contextDestroyed(ServletContextEvent event) {}

    public abstract void doUpgrade();

    protected boolean tableExists(String tableName) {
        String sql = String.format("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = '%s')", tableName);
        return verifyExistence(sql, "Could not verify table existence:" + tableName);
    }

    protected boolean columnExists(String tableName, String columnName) {
        String sql = String.format("SELECT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = '%s' and column_name = '%s')", tableName, columnName);
        return verifyExistence(sql, "Could not verify column existence: " + tableName + "(" + columnName + ")");
    }

    protected boolean runSql(String sql) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("Failed to run command: " + sql, e);
            throw new RuntimeException("Failed to run command: " + sql, e);
        }
    }

    // ----- Private -----
    private boolean verifyExistence(String sql, String failureString) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() && rs.getBoolean(1);
        } catch (SQLException e) {
            LOG.error(failureString, e);
            throw new RuntimeException(failureString, e);
        }
    }
}
