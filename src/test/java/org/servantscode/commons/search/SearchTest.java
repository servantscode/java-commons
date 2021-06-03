package org.servantscode.commons.search;

import org.junit.Test;

import java.sql.Timestamp;
import java.time.*;
import java.util.Arrays;
import java.util.Collections;
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
        assertEquals("Wrong SQL.", "field ILIKE ?", clause.getSql());
        assertEquals("Wrong Values.", "[%value%]", clause.getValues().toString());
    }

    @Test(expected = NullPointerException.class)
    public void testSearchTextClauseNull() {
        Search.SearchClause clause;
        clause = new Search.TextClause(null, "");
    }

    @Test
    public void testSearchTextClauseSpecial() {
        Search.SearchClause clause;
        clause = new Search.TextClause("Robert◙⌐⌐x╘'\");", "\\\\\\\\\"); \\\"");
        assertEquals("Wrong SQL.", "Robert◙⌐⌐x╘'\"); ILIKE ?", clause.getSql());
        assertEquals("Wrong Values", "[%\\\\\\\\\"); \\\"%]", clause.getValues().toString());
    }

    @Test
    public void testSearchEnumClause() {
        Search search = new Search();
        search.addClause(new Search.EnumClause("field", "value"));
        Search.SearchClause clause = search.getClauses().get(0);
        assertEquals("Wrong SQL.", "field=?", clause.getSql());
        assertEquals("Wrong Number of values.", 1, clause.getValues().size());
        assertEquals("Wrong Values.", "VALUE", clause.getValues().get(0));
    }

    @Test
    public void testSearchIntegerClauseLooped() {
        Search.SearchClause clause;
        for (int i = -1000; i < 1001; i+=1000) {
            clause = new Search.NumberClause("field", i);
            assertEquals("Wrong SQL.", "field = ?", clause.getSql());
            assertEquals("Wrong Values", "[" + i + "]", clause.getValues().toString());
        }
    }

    @Test
    public void testSearchFloatClause() {
        Search.SearchClause clause;
        clause = new Search.NumberClause("field", 1.12f);
        assertEquals("Wrong SQL.", "field = ?", clause.getSql());
        assertEquals("Wrong Values", "[1.12]", clause.getValues().toString());
    }

    @Test
    public void testSearchIntegerMin() {
        Search.SearchClause clause;
        clause = new Search.NumberClause("field", Integer.MIN_VALUE);
        assertEquals("Wrong SQL.", "field = ?", clause.getSql());
        assertEquals("Wrong Values", "[-2147483648]", clause.getValues().toString());
    }

    @Test
    public void testSearchIntegerClauseMax() {
        Search.SearchClause clause;
        clause = new Search.NumberClause("field", Integer.MAX_VALUE);
        assertEquals("Wrong SQL.", "field = ?", clause.getSql());
        assertEquals("Wrong Values", "[2147483647]", clause.getValues().toString());
    }

    @Test
    public void testIntegerRangeClauseExtreme() {
        Search.SearchClause clause;
        clause = new Search.NumberRangeClause("field", Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals("Wrong SQL.", "(field >= ? AND field <= ?)", clause.getSql());
        assertEquals("Wrong Values", "[-2147483648, 2147483647]", clause.getValues().toString());
    }

    @Test
    public void testIntegerRangeClauseLooped() {
        Search.SearchClause clause;
        for (int i = -100; i < 101; i += 100) {
            for (int j = -100; j < 101; j += 100) {
                clause = new Search.NumberRangeClause("field", i, j);
                assertEquals("Wrong SQL.", "(field >= ? AND field <= ?)", clause.getSql());
                assertEquals("Wrong Values", "[" + i + ", " + j + "]", clause.getValues().toString());
            }
        }
    }

    @Test
    public void testBooleanClauseTrue() {
        Search.SearchClause clause;
        clause = new Search.BooleanClause("field", true);
        assertEquals("Wrong SQL.", "field = ?", clause.getSql());
        assertEquals("Wrong Values", "[true]", clause.getValues().toString());
    }

    @Test
    public void testBooleanClauseFalse() {
        Search.SearchClause clause;
        clause = new Search.BooleanClause("field", false);
        assertEquals("Wrong SQL.", "field = ?", clause.getSql());
        assertEquals("Wrong Values", "[false]", clause.getValues().toString());
    }

    @Test
    public void testDateClauseLooped() {
        Search.SearchClause clause;
        LocalDate local = LocalDate.of(-500, 1, 1);
        while (local.isBefore(LocalDate.of(2500, 12, 31))) {
            clause = new Search.DateClause("field", local);
            assertEquals("Wrong SQL.", "(field >= ? AND field <= ?)", clause.getSql());
            assertEquals("Wrong Values", String.format("[%s, %s]",
                    local,
                    local.plusDays(1)),
                    clause.getValues().toString());
            local = local.plusDays(11);
        }
    }

    //Potentially has edge cases that are not tested for
    @Test
    public void testDateRangeClause() {
        Search.SearchClause clause;
        LocalDate first;
        LocalDate second;
        first = LocalDate.of(2000, 1, 1);
        while (first.isBefore(LocalDate.of(2030, 1, 1))) {
            second = LocalDate.of(2000, 1, 1);
            while (second.isBefore(LocalDate.of(2030, 1, 1))) {
                clause = new Search.DateRangeClause("field", first, second);
                assertEquals("Wrong SQL.", "(field >= ? AND field <= ?)", clause.getSql());
                assertEquals("Wrong Values", String.format("[%s, %s]",
                        first,
                        second),
                        clause.getValues().toString());
                second = second.plusDays(23);
            }
            first = first.plusDays(19);
        }
    }

    @Test
    public void testTimeRangeClauseSimple() {
        LocalDate dateOne = LocalDate.of(0, 2, 1);
        LocalTime timeOne = LocalTime.of(0, 0, 0, 0);
        ZonedDateTime zd = ZonedDateTime.of(dateOne, timeOne, ZoneId.of("GMT"));

        Search.TimeRangeClause clause;
        clause = new Search.TimeRangeClause("field", zd, zd);
        assertEquals("Wrong SQL.", "(field >= ? AND field <= ?)", clause.getSql());
        assertEquals("Wrong Values", "[0001-02-01 00:00:00.0, 0001-02-01 00:00:00.0]", clause.getValues().toString());
    }

    @Test(expected = NullPointerException.class)
    public void testTimeRangeClauseAllNull() {
        Search.TimeRangeClause clause;
        clause = new Search.TimeRangeClause(null, null, null);
    }

    @Test
    public void testTimeRangeClauseStartNull() {
        LocalDate dateOne = LocalDate.of(0, 2, 1);
        LocalTime timeOne = LocalTime.of(0, 0, 0, 0);
        ZonedDateTime zd = ZonedDateTime.of(dateOne, timeOne, ZoneId.of("GMT"));
        Search.TimeRangeClause clause = new Search.TimeRangeClause("field", null, zd);
        assertEquals("Wrong SQL.", "field <= ?", clause.getSql());
        assertEquals("Wrong Values", "[0001-02-01 00:00:00.0]", clause.getValues().toString());
    }

    @Test
    public void testTimeRangeClauseEndNull() {
        LocalDate dateOne = LocalDate.of(0, 2, 1);
        LocalTime timeOne = LocalTime.of(0, 0, 0, 0);
        ZonedDateTime zd = ZonedDateTime.of(dateOne, timeOne, ZoneId.of("GMT"));
        Search.TimeRangeClause clause = new Search.TimeRangeClause("field", zd, null);
        assertEquals("Wrong SQL.", "field >= ?", clause.getSql());
        assertEquals("Wrong Values", "[0001-02-01 00:00:00.0]", clause.getValues().toString());
    }

    //Potentially has edge cases that are not tested for
    @Test
    public void testTimeRangeClauseLooped() {
        Iterator<String> iter = ZoneId.getAvailableZoneIds().iterator();
        Search.SearchClause clause;
        ZoneId idOne;
        ZoneId idTwo;
        LocalDate dateOne = LocalDate.of(2000, 1, 1);
        LocalDate dateTwo = LocalDate.of(2000, 1, 1);
        while (iter.hasNext()) {
            idOne = ZoneId.of(iter.next());
            if (iter.hasNext()) {
                idTwo = ZoneId.of(iter.next());
            } else {
                idTwo = idOne;
            }
            LocalTime timeOne = LocalTime.of(0, 0);
            for (int i = 0; i < 60 * 24; i += 37) {
                LocalTime timeTwo = LocalTime.of(0, 0);
                for (int j = 0; j < 60 * 24; j += 49) {
                    ZonedDateTime one = ZonedDateTime.of(LocalDateTime.of(dateOne, timeOne), idOne);
                    ZonedDateTime two = ZonedDateTime.of(LocalDateTime.of(dateTwo, timeTwo), idTwo);
                    clause = new Search.TimeRangeClause("field", one, two);
                    assertEquals("Wrong SQL.", "(field >= ? AND field <= ?)", clause.getSql());
                    assertEquals("Wrong Values", String.format("[%s, %s]",
                            Timestamp.valueOf(one.withZoneSameInstant(ZoneId.of("Z")).toLocalDateTime()),
                            Timestamp.valueOf(two.withZoneSameInstant(ZoneId.of("Z")).toLocalDateTime())),
                            clause.getValues().toString());
                    timeTwo = timeTwo.plusMinutes(37);
                }
                timeOne = timeOne.plusMinutes(37);
                if (iter.hasNext()) {
                    idOne = ZoneId.of(iter.next());
                }
            }
            dateOne = dateOne.plusDays(17);
            dateTwo = dateTwo.plusDays(23);
        }
    }

    @Test
    public void testListItemClauseSimple() {
        Search.ListItemClause clause;
        clause = new Search.ListItemClause("field", Arrays.asList("Item"));
        assertEquals("Wrong SQL.", "(? = any(field))", clause.getSql());
        assertEquals("Wrong Values", "[Item]", clause.getValues().toString());
    }

    @Test(expected = NullPointerException.class)
    public void testListItemClauseNullList() {
        Search.ListItemClause clause;
        clause = new Search.ListItemClause("field", null);
    }

    @Test
    public void testListItemClauseSpecial() {
        Search.ListItemClause clause;
        clause = new Search.ListItemClause("field", Arrays.asList("aba\\\\%s\\\\\\\\)\\\\;\\\\\"", "abcd123", "321fds!@#$%^&*();", "[]{}-\234=_+,.<>", "::\"\4132", ""));
        assertEquals("Wrong SQL.", "(? = any(field) OR ? = any(field) OR ? = any(field) OR ? = any(field) OR ? = any(field) OR ? = any(field))", clause.getSql());
        assertEquals("Wrong Values", "[aba\\\\%s\\\\\\\\)\\\\;\\\\\", abcd123, 321fds!@#$%^&*();, []{}-\u009C=_+,.<>, ::\"!32, ]", clause.getValues().toString());
    }

    @Test(expected = NullPointerException.class)
    public void testListItemClauseListOfNull() {
        Search.ListItemClause clause;
        clause = new Search.ListItemClause(null, Arrays.asList(null, null, null, null, null, null));
    }

    @Test(expected = NullPointerException.class)
    public void testListItemClauseSingleListOfNull() {
        Search.ListItemClause clause;
        clause = new Search.ListItemClause(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testListItemClauseEmptyList() {
        Search.ListItemClause clause;
        clause = new Search.ListItemClause("field", Collections.emptyList());
    }
}
