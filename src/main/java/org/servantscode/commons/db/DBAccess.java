package org.servantscode.commons.db;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnvProperty;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.lang.String.format;

public class DBAccess {
    private static Logger LOG = LogManager.getLogger(DBAccess.class);

    private static HikariDataSource source;

    private static final String DB_HOST = EnvProperty.get("DB_HOST", "postgres");
    private static final String DB_PORT = EnvProperty.get("DB_PORT","5432");
    private static final String DB_NAME = EnvProperty.get("DB_NAME","servantscode");
    private static final String DB_USER = EnvProperty.get("DB_USER","servant1");
    private static final String DB_PASSWORD = EnvProperty.get("DB_PASSWORD");

    static {
        try {
            //Ensure driver is loaded into local context.
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Database driver not present.", e);
        }

        source = new HikariDataSource();
        String jdbcUrl = format("jdbc:postgresql://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME);
        LOG.debug("Connecting to: " + jdbcUrl);
        source.setJdbcUrl(jdbcUrl);
        source.setUsername(DB_USER);
        source.setPassword(DB_PASSWORD);
    }

    protected static Connection getConnection() {
        try {
            return source.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to database.", e);
        }
    }

    protected static Timestamp convert(ZonedDateTime input) {
        //Translate zone to UTC then save
        return input != null? Timestamp.valueOf(input.withZoneSameInstant(ZoneId.of("Z")).toLocalDateTime()): null;
    }

    protected static ZonedDateTime convert(Timestamp input) {
        //Set zone to UTC
        return input != null? ZonedDateTime.ofInstant(input.toInstant(), ZoneId.of("Z")): null;
    };
}
