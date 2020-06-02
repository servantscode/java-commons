package org.servantscode.commons.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.search.DeleteBuilder;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.UpdateBuilder;

import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.servantscode.commons.StringUtils.isEmpty;

public class DBAccess {
    private static Logger LOG = LogManager.getLogger(DBAccess.class);

    private static ConnectionFactory factory;
    static {
        factory = new PostgresConnectionFactory();
    }

    public static void setConnectionFactory(ConnectionFactory f) {
        factory = f;
    }

    protected Connection getConnection() {
        return factory.getConnection();
    }

    protected static QueryBuilder select(String... selections) { return new QueryBuilder().select(selections); }
    protected static QueryBuilder selectAll() { return new QueryBuilder().select("*"); }
    protected static QueryBuilder count() { return new QueryBuilder().select("count(1)"); }
    protected static InsertBuilder insertInto(String table) { return new InsertBuilder().into(table); }
    protected static UpdateBuilder update(String table) { return new UpdateBuilder().update(table); }
    protected static DeleteBuilder deleteFrom(String table) { return new DeleteBuilder().delete(table); }

    protected static <T> T firstOrNull(List<T> items) { return items.isEmpty()? null: items.get(0); }

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
}
