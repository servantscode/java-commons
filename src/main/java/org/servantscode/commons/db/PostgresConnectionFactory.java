package org.servantscode.commons.db;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnvProperty;

import javax.sql.DataSource;

import static java.lang.String.format;

public class PostgresConnectionFactory extends ConnectionFactory {
    private static Logger LOG = LogManager.getLogger(PostgresConnectionFactory.class);

    @Override
    public DataSource configureSource() {
        try {
            //Ensure driver is loaded into local context.
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Database driver not present.", e);
        }

        String DB_HOST = EnvProperty.get("DB_HOST", "postgres");
        String DB_PORT = EnvProperty.get("DB_PORT","5432");
        String DB_NAME = EnvProperty.get("DB_NAME","servantscode");
        String DB_USER = EnvProperty.get("DB_USER","servant1");
        String DB_PASSWORD = EnvProperty.get("DB_PASSWORD");

        HikariDataSource source = new HikariDataSource();
        String jdbcUrl = format("jdbc:postgresql://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME);
        LOG.debug("Connecting to: " + jdbcUrl);
        source.setJdbcUrl(jdbcUrl);
        source.setUsername(DB_USER);
        source.setPassword(DB_PASSWORD);

        return source;
    }
}
