package org.servantscode.commons.db;

import org.servantscode.commons.search.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class EasyDB<T> extends DBAccess {

    protected SearchParser<T> searchParser;

    public EasyDB(Class<T> clazz, String defaultField)  {
        this(clazz, defaultField, Collections.emptyMap());
    }

    public EasyDB(Class<T> clazz, String defaultField, Map<String, String> fieldMap)  {
        this.searchParser = new SearchParser<>(clazz, defaultField, fieldMap);
    }

    protected int getCount(QueryBuilder query) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve item count.", e);
        }
        return 0;
    }

    protected List<T> get(QueryBuilder query) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)
        ) {
            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve items.", e);
        }
    }

    protected T getOne(QueryBuilder query) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
        ) {

            return firstOrNull(processResults(stmt));
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve item.", e);
        }
    }

    protected boolean create(InsertBuilder cmd) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = cmd.prepareStatement(conn)) {

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not create record.", e);
        }
    }

    protected int createAndReturnKey(InsertBuilder cmd) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = cmd.prepareStatement(conn, true)) {

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not create record.");

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (!rs.next())
                    throw new RuntimeException("No new key generated.");

                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not create record.", e);
        }
    }

    protected boolean update(UpdateBuilder cmd) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = cmd.prepareStatement(conn)) {

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update record.", e);
        }
    }

    protected boolean delete(DeleteBuilder cmd) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = cmd.prepareStatement(conn)) {

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete record.", e);
        }
    }

    protected List<T> processResults(PreparedStatement stmt) throws SQLException {
        try(ResultSet rs = stmt.executeQuery()) {
            List<T> sessions = new LinkedList<>();
            while (rs.next())
                sessions.add(processRow(rs));
            return sessions;
        }
    }

    protected abstract T processRow(ResultSet r) throws SQLException;
}
