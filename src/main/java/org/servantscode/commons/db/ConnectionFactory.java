package org.servantscode.commons.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class ConnectionFactory {
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
}
