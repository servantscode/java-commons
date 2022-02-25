package org.servantscode.commons.db;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

public abstract class ConnectionFactory implements Closeable {
    private static final Logger LOG = LogManager.getLogger(ConnectionFactory.class);

    private DataSource source;

    public ConnectionFactory(){
        source = configureSource();
    }

    public abstract DataSource configureSource();

    protected Connection getConnection() {
        try {
            return source.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to database.", e);
        }
    }

    public void close() {
        if(source != null && source instanceof HikariDataSource)
            ((HikariDataSource)source).close();
        deregisterDrivers();
    }

    private void deregisterDrivers() {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                LOG.log(Level.INFO, String.format("Deregistering jdbc driver: %s", driver));
            } catch (SQLException e) {
                LOG.log(Level.WARN, String.format("Error deregistering driver %s", driver), e);
            }
        }
    }
}
