package org.servantscode.commons.search;

import jdk.vm.ci.meta.Local;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class Search {
    private static final Logger LOG = LogManager.getLogger(Search.class);

    private List<SearchClause> clauses;

    public Search() {
        this.clauses = new ArrayList<>();
    }

    public void addClause(SearchClause clause) {
        this.clauses.add(clause);
    }

    public String getDBQueryString() {
        String clause = clauses.stream().map(SearchClause::getQuery).collect(Collectors.joining(" AND "));
        LOG.trace("Parsed search is: " + clause);
        return clause ;
    }

    public List<SearchClause> getClauses() {
        return clauses;
    }

    public static abstract class SearchClause {
        abstract String getQuery();
        abstract String getSql();
        abstract List<Object> getValues();
    }

    public static class TextClause extends SearchClause {
        private final String field;
        private final String value;

        public TextClause(String field, String value) {
            this.field = field;
            this.value = value;
        }

        @Override
        public String getQuery() { return String.format("%s ILIKE '%%%s%%'", field, value.replace("'", "''")); }

        @Override
        public String getSql() { return String.format("%s ILIKE ?", field); }

        @Override
        public List<Object> getValues() { return asList(String.format("%%%s%%", value)); }
    }

    public static class IntegerClause extends SearchClause {
        private final String field;
        private final int value;

        public IntegerClause(String field, int value) {
            this.field = field;
            this.value = value;
        }

        @Override
        String getQuery() { return String.format("%s = %d", field, value); }

        @Override
        public String getSql() { return String.format("%s = ?", field); }

        @Override
        public List<Object> getValues() { return asList(value); }
    }

    public static class IntegerRangeClause extends SearchClause {
        private final String field;
        private final int startValue;
        private final int endValue;

        public IntegerRangeClause(String field, int startValue, int endValue) {
            this.field = field;
            this.startValue = startValue;
            this.endValue = endValue;
        }

        @Override
        String getQuery() { return String.format("%s >= '%s' AND %s <= '%s'", field, startValue, field, endValue); }

        @Override
        public String getSql() { return String.format("%s >= ? AND %s <= ?", field, field); }

        @Override
        public List<Object> getValues() { return asList(startValue, endValue); }
    }


    public static class BooleanClause extends SearchClause {
        private final String field;
        private final boolean value;

        public BooleanClause(String field, boolean value) {
            this.field = field;
            this.value = value;
        }

        @Override
        String getQuery() {
            return String.format("%s=%s", field, Boolean.toString(value));
        }

        @Override
        public String getSql() { return String.format("%s = ?", field); }

        @Override
        public List<Object> getValues() { return asList(value); }
    }

    public static class DateClause extends SearchClause {
        private final String field;
        private final LocalDate value;

        public DateClause(String field, LocalDate value) {
            this.field = field;
            this.value = value;
        }

        @Override
        String getQuery() {
            return String.format("%s > '%s' AND %s < '%s'",
                    field, value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00")),
                    field, value.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00")));
        }

        @Override
        public String getSql() { return String.format("%s > ? AND %s < ?", field, field); }

        @Override
        public List<Object> getValues() { return asList(value, value.plusDays(1)); }
    }

    public static class DateRangeClause extends SearchClause {
        private final String field;
        private final LocalDate start;
        private final LocalDate end;

        public DateRangeClause(String field, LocalDate start, LocalDate end) {
            this.field = field;
            this.start = start;
            this.end = end;
        }

        @Override
        String getQuery() {
            return String.format("%s > '%s' AND %s < '%s'",
                    field, start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00")),
                    field, end.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00")));
        }

        @Override
        public String getSql() { return String.format("%s > ? AND %s < ?", field, field); }

        @Override
        public List<Object> getValues() { return asList(start, end); }

        protected Date convert(LocalDate date) {
            return date == null? null: Date.valueOf(date);
        }
    }

    public static class TimeRangeClause extends SearchClause {
        private final String field;
        private final ZonedDateTime start;
        private final ZonedDateTime end;

        public TimeRangeClause(String field, ZonedDateTime start, ZonedDateTime end) {
            this.field = field;
            this.start = start;
            this.end = end;
        }

        @Override
        String getQuery() {
            String query = (start != null)? String.format("%s > '%s'", field, start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))): "";
            query += (start != null && end != null)? " AND ": "";
            query +=  (end != null)? String.format("%s < '%s'", field, end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))): "";
            return query;
        }

        @Override
        public String getSql() {
            String query = (start != null)? String.format("%s > ?", field): "";
            query += (start != null && end != null)? " AND ": "";
            query +=  (end != null)? String.format("%s < ?", field): "";
            return query;
        }

        @Override
        public List<Object> getValues() {
            List<Object> values = new ArrayList<>(2);
            if(start != null) values.add(convert(start));
            if(end != null) values.add(convert(end));
            return values;
        }

        private static Timestamp convert(ZonedDateTime input) {
            //Translate zone to UTC then save
            return input != null? Timestamp.valueOf(input.withZoneSameInstant(ZoneId.of("Z")).toLocalDateTime()): null;
        }
    }
}
