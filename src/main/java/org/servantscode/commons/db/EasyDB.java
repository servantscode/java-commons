package org.servantscode.commons.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.search.*;

import java.sql.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class EasyDB<T> extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(EasyDB.class);

    protected SearchParser<T> searchParser;

    public EasyDB(Class<T> clazz, String defaultField)  {
        this(clazz, defaultField, Collections.emptyMap());
    }

    public EasyDB(Class<T> clazz, String defaultField, Map<String, String> fieldMap)  {
        this.searchParser = new SearchParser<>(clazz, defaultField, fieldMap);
    }

    public EasyDB(Class<T> clazz, String defaultField, FieldTransformer transformer)  {
        this.searchParser = new SearchParser<>(clazz, defaultField, transformer);
    }

    public QueryBuilder select(List<String> fields) { return super.select(fields).setSearchParser(searchParser); }
    protected QueryBuilder selectDistinct(List<String> selections) { return super.selectDistinct(selections).setSearchParser(searchParser); }
    protected QueryBuilder selectAll() { return super.selectAll().setSearchParser(searchParser); }
    protected QueryBuilder selectCount() { return super.selectCount().setSearchParser(searchParser); }
    protected UpdateBuilder update(String table) { return super.update(table).setSearchParser(searchParser); }
    protected DeleteBuilder deleteFrom(String table) { return super.deleteFrom(table).setSearchParser(searchParser); }

    protected int getCount(QueryBuilder query) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = runQuery(query, stmt)) {

            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            LOG.error("SQL failed: " + query.getSql());
            throw new RuntimeException("Could not retrieve item count.", e);
        }
        return 0;
    }

    protected List<T> get(QueryBuilder query) {
        return processResults(query);
    }

    protected T getOne(QueryBuilder query) {
        return firstOrNull(processResults(query));
    }

    //Requires a count search
    protected boolean existsAny(QueryBuilder query) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = runQuery(query, stmt)) {

            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOG.error("SQL failed: " + query.getSql());
            throw new RuntimeException("Could not determine data existence.", e);
        }
    }

    protected boolean create(InsertBuilder cmd) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = cmd.prepareStatement(conn)) {

            return runUpdate(cmd, stmt) > 0;
        } catch (SQLException e) {
            LOG.error("SQL failed: " + cmd.getSql());
            throw new RuntimeException("Could not create record.", e);
        }
    }

    protected int createAndReturnKey(InsertBuilder cmd) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = cmd.prepareStatement(conn, true)) {

            if(runUpdate(cmd, stmt) == 0)
                throw new RuntimeException("Could not create record.");

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (!rs.next())
                    throw new RuntimeException("No new key generated.");

                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOG.error("SQL failed: " + cmd.getSql());
            throw new RuntimeException("Could not create record.", e);
        }
    }

    protected long createAndReturnLongKey(InsertBuilder cmd) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = cmd.prepareStatement(conn, true)) {

            if(runUpdate(cmd, stmt) == 0)
                throw new RuntimeException("Could not create record.");

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (!rs.next())
                    throw new RuntimeException("No new key generated.");

                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.error("SQL failed: " + cmd.getSql());
            throw new RuntimeException("Could not create record.", e);
        }
    }

    protected boolean update(UpdateBuilder cmd) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = cmd.prepareStatement(conn)) {

            return runUpdate(cmd, stmt) > 0;
        } catch (SQLException e) {
            LOG.error("SQL failed: " + cmd.getSql());
            throw new RuntimeException("Could not update record.", e);
        }
    }

    protected boolean delete(DeleteBuilder cmd) {
        if(logSql) LOG.trace("Executing: " + cmd.getSql());
        try (Connection conn = getConnection();
             PreparedStatement stmt = cmd.prepareStatement(conn)) {

            return runUpdate(cmd, stmt) > 0;
        } catch (SQLException e) {
            LOG.error("SQL failed: " + cmd.getSql());
            throw new RuntimeException("Could not delete record.", e);
        }
    }

    protected boolean executeUpdate(SqlBuilder cmd) {
        if(logSql) LOG.trace("Executing: " + cmd.getSql());
        try (Connection conn = getConnection();
             PreparedStatement stmt = cmd.prepareStatement(conn)) {

            return runUpdate(cmd, stmt) > 0;
        } catch (SQLException e) {
            LOG.error("SQL failed: " + cmd.getSql());
            throw new RuntimeException("Could not run command.", e);
        }
    }

    protected List<T> processResults(QueryBuilder query) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn);
              ResultSet rs = runQuery(query, stmt)) {

            List<T> sessions = new LinkedList<>();
            while (rs.next())
                sessions.add(processRow(rs));
            return sessions;
        } catch (SQLException e) {
            LOG.error("SQL failed: " + query.getSql());
            throw new RuntimeException("Could not retrieve items.", e);
        }
    }

    protected abstract T processRow(ResultSet r) throws SQLException;

    protected Search parse(String search) {
        return searchParser.parse(search);
    }

    protected String translate(String sort) {
        return searchParser.translateSort(sort);
    }

    protected boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();

        for(int x = 1; x <= columns; ++x) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }

        return false;
    }
}
