package org.servantscode.commons.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public abstract class SqlBuilder {
    private static Logger LOG = LogManager.getLogger(SqlBuilder.class);

    protected List<Object> values = new LinkedList<>();
    protected abstract String getSql();


    public PreparedStatement prepareStatement(Connection conn) throws SQLException {
        return prepareStatement(conn, false);
    }

    public PreparedStatement prepareStatement(Connection conn, boolean returnNewKeys) throws SQLException {
        //Stmt cannot be opened in a try structure, else it will auto close instead of returning.
        //So we need to open it outselves, and be wary of connection leaks.

        //Error here will not leak as higher level owns the connection and prepareStatement shouldn't leak on error.
        //We are relying on the underlying prepareStatement() but this seems reasonable.
//        LOG.trace("generated sql: " + getSql());

        PreparedStatement stmt = returnNewKeys?
                conn.prepareStatement(getSql(), RETURN_GENERATED_KEYS):
                conn.prepareStatement(getSql());
        try {
            //Error here would leak connection, so catch, close and re-throw.
            fillStatement(stmt);
        } catch (Throwable t) {
            LOG.error("Failed to populate generated sql: " + getSql());
            stmt.close();
            throw t;
        }
        return stmt;
    }

    protected void fillStatement(PreparedStatement stmt) {
        fillStatement(stmt, new AtomicInteger(1));
    }

    protected void fillStatement(PreparedStatement stmt, AtomicInteger pos) {
        values.forEach(value -> {
            try {
                if(value instanceof SqlBuilder)
                    ((SqlBuilder) value).fillStatement(stmt, pos);
                else
                    stmt.setObject(pos.getAndIncrement(), sqlize(value));
            } catch (SQLException e) {
                throw new RuntimeException(String.format("Could not populate sql with value: %s at pos: %d\nsql: %s", value, pos.get() - 1, getSql()), e);
            }
        });
    }

    private Object sqlize(Object value) {
        if(value instanceof LocalDate)
            return DBAccess.convert((LocalDate)value);
        if(value instanceof ZonedDateTime)
            return DBAccess.convert((ZonedDateTime) value);
        if(value instanceof Enum)
            return value.toString();
        if(value instanceof Integer && value.equals(0))
            return null;
        return value;
    }
}
