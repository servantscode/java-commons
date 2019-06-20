package org.servantscode.commons.search;

import org.junit.Test;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

import static junit.framework.TestCase.assertEquals;

public class SearchTest {

    public SearchTest() {
    }

    @Test
    public void testSearchTextClause() {
        Search search = new Search();
        search.addClause(new Search.TextClause("field", "value"));
        Search.SearchClause clause = search.getClauses().get(0);
        assertEquals("Wrong Query.", "field ILIKE '%value%'", clause.getQuery());
        assertEquals("Wrong SQL.", "field ILIKE ?", clause.getSql());
        assertEquals("Wrong Values.", "[%value%]", clause.getValues().toString());

        clause = new Search.TextClause("Robert◙⌐⌐x╘'\");", "\\\\\\\\\"); \\\"");
        assertEquals("Wrong Query", "Robert◙⌐⌐x╘'\"); ILIKE '%\\\\\\\\\"); \\\"%'", clause.getQuery());
        assertEquals("Wrong SQL.", "Robert◙⌐⌐x╘'\"); ILIKE ?", clause.getSql());
        assertEquals("Wrong Values", "[%\\\\\\\\\"); \\\"%]", clause.getValues().toString());

        clause = new Search.TextClause(null, "");
        assertEquals("Wrong Query", "null ILIKE '%%'", clause.getQuery());
        assertEquals("Wrong SQL.", "null ILIKE ?", clause.getSql());
        assertEquals("Wrong Values", "[%%]", clause.getValues().toString());
    }

    @Test
    public void testSearchIntegerClause() {
        Search.SearchClause clause = new Search.IntegerClause("field", Integer.MAX_VALUE);
        assertEquals("Wrong Query.", "field = 2147483647", clause.getQuery());
        assertEquals("Wrong SQL.", "field = ?", clause.getSql());
        assertEquals("Wrong Values", "[2147483647]", clause.getValues().toString());

        clause = new Search.IntegerClause("field", Integer.MIN_VALUE);
        assertEquals("Wrong Query.", "field = -2147483648", clause.getQuery());
        assertEquals("Wrong SQL.", "field = ?", clause.getSql());
        assertEquals("Wrong Values", "[-2147483648]", clause.getValues().toString());

        for (int i = -100000; i < 100000; i++) {
            clause = new Search.IntegerClause("field", i);
            assertEquals("Wrong Query.", "field = " + i, clause.getQuery());
            assertEquals("Wrong SQL.", "field = ?", clause.getSql());
            assertEquals("Wrong Values", "[" + i + "]", clause.getValues().toString());
        }
    }

    @Test
    public void testIntegerRangeClause() {
        Search.SearchClause clause = new Search.IntegerRangeClause("field", Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals("Wrong Query.", "field >= '-2147483648' AND field <= '2147483647'", clause.getQuery());
        assertEquals("Wrong SQL.", "field >= ? AND field <= ?", clause.getSql());
        assertEquals("Wrong Values", "[-2147483648, 2147483647]", clause.getValues().toString());

        for (int i = -1000; i < 1000; i++) {
            for (int j = -1000; j < 1000; j++) {
                clause = new Search.IntegerRangeClause("field", i, j);
                assertEquals("Wrong Query.", "field >= '" + i + "' AND field <= '" + j + "'", clause.getQuery());
                assertEquals("Wrong SQL.", "field >= ? AND field <= ?", clause.getSql());
                assertEquals("Wrong Values", "[" + i + ", " + j + "]", clause.getValues().toString());
            }
        }
    }

    @Test
    public void testBooleanClause() {
        Search.SearchClause clause = new Search.BooleanClause("field", true);
        assertEquals("Wrong Query.", "field=true", clause.getQuery());
        assertEquals("Wrong SQL.", "field = ?", clause.getSql());
        assertEquals("Wrong Values", "[true]", clause.getValues().toString());


        clause = new Search.BooleanClause("field", false);
        assertEquals("Wrong Query.", "field=false", clause.getQuery());
        assertEquals("Wrong SQL.", "field = ?", clause.getSql());
        assertEquals("Wrong Values", "[false]", clause.getValues().toString());
    }

    @Test
    public void testDateClause() {
        Search.SearchClause clause;
        LocalDate local = LocalDate.of(-2000, 1, 1);
        while (local.isBefore(LocalDate.of(4000, 12, 31))) {
            clause = new Search.DateClause("field", local);
            assertEquals("Wrong Query.", String.format("field > '%s' AND field < '%s'",
                    local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00")),
                    local.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00"))),
                    clause.getQuery());
            assertEquals("Wrong SQL.", "field > ? AND field < ?", clause.getSql());
            assertEquals("Wrong Values", String.format("[%s, %s]",
                    local,
                    local.plusDays(1)),
                    clause.getValues().toString());
            local = local.plusDays(1);
        }
    }

    @Test
    public void testDateRangeClause() {
        Search.SearchClause clause;
        LocalDate first = LocalDate.of(2000, 1, 1);
        LocalDate second;
        while (first.isBefore(LocalDate.of(2100, 1, 1))) {
            second = LocalDate.of(2000, 1, 1);
            while (second.isBefore(LocalDate.of(2100, 1, 1))) {
                clause = new Search.DateRangeClause("field", first, second);
                assertEquals("Wrong Query.", String.format("field > '%s' AND field < '%s'",
                        first.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00")),
                        second.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00"))),
                        clause.getQuery());
                assertEquals("Wrong SQL.", "field > ? AND field < ?", clause.getSql());
                assertEquals("Wrong Values", String.format("[%s, %s]",
                        first,
                        second),
                        clause.getValues().toString());
                second = second.plusDays(17);
            }
            first = first.plusDays(13);
        }
    }

    @Test
    public void testTimeRangeClauseSimple() {
        LocalDate dateOne = LocalDate.of(0, 2, 1);
        LocalTime timeOne = LocalTime.of(0, 0, 0, 0);
        ZonedDateTime zd = ZonedDateTime.of(dateOne, timeOne, ZoneId.of("GMT"));
        Search.TimeRangeClause clause = new Search.TimeRangeClause(null, null, null);

        assertEquals("Wrong Query.", "", clause.getQuery());
        assertEquals("Wrong SQL.", "", clause.getSql());
        assertEquals("Wrong Values", "[]", clause.getValues().toString());

        clause = new Search.TimeRangeClause("field", zd, null);
        assertEquals("Wrong Query.", "field > '0001-02-01 00:00:00'", clause.getQuery());
        assertEquals("Wrong SQL.", "field > ?", clause.getSql());
        assertEquals("Wrong Values", "[0001-02-01 00:00:00.0]", clause.getValues().toString());


        clause = new Search.TimeRangeClause("field", null, zd);
        assertEquals("Wrong Query.", "field < '0001-02-01 00:00:00'", clause.getQuery());
        assertEquals("Wrong SQL.", "field < ?", clause.getSql());
        assertEquals("Wrong Values", "[0001-02-01 00:00:00.0]", clause.getValues().toString());


        clause = new Search.TimeRangeClause("field", zd, zd);
        assertEquals("Wrong Query.", "field > '0001-02-01 00:00:00' AND field < '0001-02-01 00:00:00'", clause.getQuery());
        assertEquals("Wrong SQL.", "field > ? AND field < ?", clause.getSql());
        assertEquals("Wrong Values", "[0001-02-01 00:00:00.0, 0001-02-01 00:00:00.0]", clause.getValues().toString());
    }

    @Test
    public void testTimeRangeClauseExtended() {
        Iterator<String> iter = ZoneId.getAvailableZoneIds().iterator();
        Search.SearchClause clause;
        ZoneId id;
        LocalDate dateOne = LocalDate.of(2000, 1, 1);
        LocalDate dateTwo = LocalDate.of(2000, 1, 1);
        while (iter.hasNext()) {
            id = ZoneId.of(iter.next());
            LocalTime timeOne = LocalTime.of(0, 0);
            while (timeOne.isBefore(LocalTime.of(23, 22))) {
                LocalTime timeTwo = LocalTime.of(0, 0);
                while (timeTwo.isBefore(LocalTime.of(23, 46))) {
                    ZonedDateTime one = ZonedDateTime.of(LocalDateTime.of(dateOne, timeOne), id);
                    ZonedDateTime two = ZonedDateTime.of(LocalDateTime.of(dateTwo, timeTwo), id);
                    clause = new Search.TimeRangeClause("field", one, two);
                    assertEquals("Wrong Query.", String.format("field > '%s' AND field < '%s'",
                            one.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            two.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
                            clause.getQuery());
                    assertEquals("Wrong SQL.", "field > ? AND field < ?", clause.getSql());
                    assertEquals("Wrong Values", String.format("[%s, %s]",
                            Timestamp.valueOf(one.withZoneSameInstant(ZoneId.of("Z")).toLocalDateTime()),
                            Timestamp.valueOf(two.withZoneSameInstant(ZoneId.of("Z")).toLocalDateTime())),
                            clause.getValues().toString());
                    timeTwo = timeTwo.plusMinutes(13);
                }
                timeOne = timeOne.plusMinutes(23);
            }
            dateOne = dateOne.plusDays(17);
            dateTwo = dateTwo.plusDays(23);
        }
    }

    @Test
    public void testListItemClause() {
        Search.ListItemClause clause = new Search.ListItemClause("field");
        assertEquals("Wrong Query.", "", clause.getQuery());
        assertEquals("Wrong SQL.", "", clause.getSql());
        assertEquals("Wrong Values", "[]", clause.getValues().toString());

        clause = new Search.ListItemClause(null, new String[]{null});
        assertEquals("Wrong Query.", "null ILIKE ?", clause.getQuery());
        assertEquals("Wrong SQL.", "null ILIKE ?", clause.getSql());
        assertEquals("Wrong Values", "[%null%]", clause.getValues().toString());

        clause = new Search.ListItemClause(null, null, null, null, null, null, null);
        assertEquals("Wrong Query.", "null ILIKE ? OR null ILIKE ? OR null ILIKE ? OR null ILIKE ? OR null ILIKE ? OR null ILIKE ?", clause.getQuery());
        assertEquals("Wrong SQL.", "null ILIKE ? OR null ILIKE ? OR null ILIKE ? OR null ILIKE ? OR null ILIKE ? OR null ILIKE ?", clause.getSql());
        assertEquals("Wrong Values", "[%null%, %null%, %null%, %null%, %null%, %null%]", clause.getValues().toString());

        clause = new Search.ListItemClause("field", "item");
        assertEquals("Wrong Query.", "field ILIKE ?", clause.getQuery());
        assertEquals("Wrong SQL.", "field ILIKE ?", clause.getSql());
        assertEquals("Wrong Values", "[%item%]", clause.getValues().toString());

        clause = new Search.ListItemClause("field", "aba\\\\%s\\\\\\\\)\\\\;\\\\\"", "abcd123", "321fds!@#$%^&*();", "[]{}-\234=_+,.<>", "::\"\4132", null);
        assertEquals("Wrong Query.", "field ILIKE ? OR field ILIKE ? OR field ILIKE ? OR field ILIKE ? OR field ILIKE ? OR field ILIKE ?", clause.getQuery());
        assertEquals("Wrong SQL.", "field ILIKE ? OR field ILIKE ? OR field ILIKE ? OR field ILIKE ? OR field ILIKE ? OR field ILIKE ?", clause.getSql());
        assertEquals("Wrong Values", "[%aba\\\\%s\\\\\\\\)\\\\;\\\\\"%, %abcd123%, %321fds!@#$%^&*();%, %[]{}-\u009C=_+,.<>%, %::\"!32%, %null%]", clause.getValues().toString());
    }


}
