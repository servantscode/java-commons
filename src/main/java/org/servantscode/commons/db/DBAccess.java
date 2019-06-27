package org.servantscode.commons.db;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnvProperty;
import org.servantscode.commons.search.QueryBuilder;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.servantscode.commons.StringUtils.isEmpty;

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

    protected QueryBuilder select(String... selections) { return new QueryBuilder().select(selections); }
    protected QueryBuilder selectAll() { return new QueryBuilder().select("*"); }
    protected QueryBuilder count() { return new QueryBuilder().select("count(1)"); }

    protected <T> T firstOrNull(List<T> items) { return items.isEmpty()? null: items.get(0); }

    public static Timestamp convert(ZonedDateTime input) {
        //Translate zone to UTC then save
        return input != null? Timestamp.valueOf(input.withZoneSameInstant(ZoneId.of("Z")).toLocalDateTime()): null;
    }

    public static ZonedDateTime convert(Timestamp input) {
        //Set zone to UTC
        return input != null? ZonedDateTime.ofInstant(input.toInstant(), ZoneId.of("Z")): null;
    };

    public static Date convert(LocalDate date) { return date != null? Date.valueOf(date): null; }

    public static LocalDate convert(Date date) { return date != null? date.toLocalDate(): null; }

    public static List<String> parseList(String dataString) {
        if(isEmpty(dataString))
            return emptyList();

        String[] values = dataString.split("\\|");
        return Arrays.asList(values);
    }

    public static String storeList(List<String> dates) {
        if(dates == null || dates.isEmpty())
            return "";

        return String.join("|", dates);
    }

    public String stringify(Enum<?> value) { return value == null? null: value.toString(); }
}
