package org.servantscode.commons.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.search.DeleteBuilder;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.UpdateBuilder;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.servantscode.commons.StringUtils.isEmpty;

public class DBAccess {
    private static Logger LOG = LogManager.getLogger(DBAccess.class);

    private static ConnectionFactory factory;

    public static ConnectionFactory getConnectionFactory() { return factory; }
    public static void setConnectionFactory(ConnectionFactory f) {
        factory = f;
    }

    private TransactionConnection transactionalConnection = null;

    protected Connection getConnection() {
        if(factory == null)
            defaultFactory();

        if(transactionalConnection != null) {
            transactionalConnection.addRef();
            return transactionalConnection;
        }
        return factory.getConnection();
    }

    protected Connection startTransaction() throws SQLException {
        if(transactionalConnection != null)
            throw new IllegalStateException("Transaction is already open.");

        transactionalConnection = new TransactionConnection(getConnection());
        transactionalConnection.setAutoCommit(false);

        return transactionalConnection;
    }

    private static synchronized void defaultFactory() {
        if(factory == null)
            factory = new PostgresConnectionFactory();
    }

    protected static QueryBuilder select(String... selections) { return new QueryBuilder().select(selections); }
    protected static QueryBuilder selectDistinct(String... selections) { return new QueryBuilder().distinct().select(selections); }
    protected static QueryBuilder selectAll() { return new QueryBuilder().select("*"); }
    protected static QueryBuilder count() { return new QueryBuilder().select("count(1)"); }
    protected static InsertBuilder insertInto(String table) { return new InsertBuilder().into(table); }
    protected static UpdateBuilder update(String table) { return new UpdateBuilder().update(table); }
    protected static DeleteBuilder deleteFrom(String table) { return new DeleteBuilder().delete(table); }

    protected static <T> T firstOrNull(List<T> items) { return items.isEmpty()? null: items.get(0); }

    public static Timestamp convert(ZonedDateTime input) {
        //Translate zone to UTC then save
        return input != null? Timestamp.from(input.withZoneSameInstant(ZoneId.of("Z")).toInstant()): null;
    }

    public static ZonedDateTime convert(Timestamp input) {
        //Set zone to UTC
        return input != null? ZonedDateTime.ofInstant(input.toInstant(), ZoneId.of("Z")): null;
    };

    public static Date convert(LocalDate date) { return date != null? Date.valueOf(date): null; }

    public static LocalDate convert(Date date) { return date != null? date.toLocalDate(): null; }

    public static <T extends Enum<T>> List<T> parseEnumList(Class<T> clazz, String valueString) {
        if(valueString == null || valueString.isEmpty())
            return emptyList();

        String[] values = valueString.split("\\|");
        return Arrays.stream(values).map(v -> Enum.valueOf(clazz, v)).collect(Collectors.toList());
    }

    public static String storeEnumList(List<? extends Enum<?>> values) {
        if(values == null || values.isEmpty())
            return "";

        return values.stream().map(Enum::toString).collect(Collectors.joining("|"));
    }

    public static String encodeDateList(List<LocalDate> dates) {
        return encodeList(dates, date->date.format(DateTimeFormatter.ISO_DATE));
    }

    public static List<LocalDate> decodeDateList(String dateString) {
        return decodeList(dateString, LocalDate::parse);
    }

    public static String encodeList(List<String> items) {
        return encodeList(items, i->i);
    }

    public static List<String> decodeList(String data) {
        return decodeList(data, i->i);
    }

    public static <T> String encodeList(List<T> items, Function<T, String> mapper) {
        if(items == null || items.isEmpty())
            return "";

        return items.stream().map(mapper).collect(Collectors.joining("|"));
    }

    public static <T> List<T> decodeList(String data, Function<String, T> mapper) {
        if(isEmpty(data))
            return emptyList();

        String[] dates = data.split("\\|");
        return Arrays.stream(dates).map(mapper).collect(Collectors.toList());
    }

    public static String stringify(Enum<?> value) { return value == null? null: value.toString(); }
    public static <T extends Enum<T>> T parse(Class<T> clazz, String value) { return value == null? null: Enum.valueOf(clazz, value); }

    private class TransactionConnection implements Connection {
        private final Connection ic;
        AtomicInteger refCount = new AtomicInteger(1);

        private TransactionConnection(Connection ic) {this.ic = ic;}

        public void addRef() {
            refCount.incrementAndGet();
        }

        @Override
        public void close() throws SQLException {
            if(refCount.decrementAndGet() == 0) {
                ic.close();
                transactionalConnection = null;
            }
        }

        @Override
        public Statement createStatement() throws SQLException {
            return ic.createStatement();
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return ic.prepareStatement(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return ic.prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return ic.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            ic.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return ic.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            ic.commit();
        }

        @Override
        public void rollback() throws SQLException {
            ic.rollback();
        }

        @Override
        public boolean isClosed() throws SQLException {
            return ic.isClosed();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return ic.getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            ic.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return ic.isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            ic.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return ic.getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            ic.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return ic.getTransactionIsolation();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return ic.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            ic.clearWarnings();
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return ic.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return ic.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return ic.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return ic.getTypeMap();
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            ic.setTypeMap(map);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            ic.setTransactionIsolation(holdability);
        }

        @Override
        public int getHoldability() throws SQLException {
            return ic.getHoldability();
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return ic.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return ic.setSavepoint(name);
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            ic.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            ic.releaseSavepoint(savepoint);
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return ic.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return ic.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return ic.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return ic.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return ic.prepareStatement(sql, columnNames);
        }

        @Override
        public Clob createClob() throws SQLException {
            return ic.createClob();
        }

        @Override
        public Blob createBlob() throws SQLException {
            return ic.createBlob();
        }

        @Override
        public NClob createNClob() throws SQLException {
            return ic.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return ic.isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            ic.setClientInfo(name, value);
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            ic.setClientInfo(properties);
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return ic.getClientInfo(name);
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return ic.getClientInfo();
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return ic.createArrayOf(typeName, elements);
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            ic.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return ic.getSchema();
        }

        @Override
        public void abort(Executor executor) throws SQLException {
            ic.abort(executor);
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            ic.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return ic.getNetworkTimeout();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return ic.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return ic.isWrapperFor(iface);
        }
    }
}
