package org.servantscode.commons.search;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SearchParserTest {
    private SearchParser parser;

    private static class TestClass {
        private String name;
        private boolean male;
        private LocalDate date;
        // ----- Accessors -----
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean isMale() { return male; }
        public void setMale(boolean male) { this.male = male; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }

    public SearchParserTest() {
        this.parser = new SearchParser(TestClass.class);
    }

    @Test
    public void testSearchParse() {
        String[] clauses = parser.parseText("name");
        assertEquals("Incorrect clause count.", 1, clauses.length);
        assertEquals("Incorrect clause", "name", clauses[0]);
    }

    @Test
    public void testSearchParseWithField() {
        String[] clauses = parser.parseText("name:foo");
        assertEquals("Incorrect clause count.", 1, clauses.length);
        assertEquals("Incorrect clause", "name:foo", clauses[0]);
    }

    @Test
    public void testSearchParseMultiClause() {
        String[] clauses = parser.parseText("name:foo male:true date:1990-01-01");
        assertEquals("Incorrect clause count.", 3, clauses.length);
        assertEquals("Incorrect clause", "name:foo", clauses[0]);
        assertEquals("Incorrect clause", "male:true", clauses[1]);
        assertEquals("Incorrect clause", "date:1990-01-01", clauses[2]);
    }

    @Test
    public void testSearchParseTextAndOrGroups() {
        String[] clauses = parser.parseText("(name:foo OR male:true) AND date:1990-01-01");
        assertEquals("Incorrect clause count.", 7, clauses.length);
        assertEquals("Incorrect clause", "(", clauses[0]);
        assertEquals("Incorrect clause", "name:foo", clauses[1]);
        assertEquals("Incorrect clause", "OR", clauses[2]);
        assertEquals("Incorrect clause", "male:true", clauses[3]);
        assertEquals("Incorrect clause", ")", clauses[4]);
        assertEquals("Incorrect clause", "AND", clauses[5]);
        assertEquals("Incorrect clause", "date:1990-01-01", clauses[6]);
    }

    @Test
    public void testSearchParseTextNextedGroups() {
        String[] clauses = parser.parseText("((name:foo OR male:true))");
        assertEquals("Incorrect clause count.", 7, clauses.length);
        assertEquals("Incorrect clause", "(", clauses[0]);
        assertEquals("Incorrect clause", "(", clauses[1]);
        assertEquals("Incorrect clause", "name:foo", clauses[2]);
        assertEquals("Incorrect clause", "OR", clauses[3]);
        assertEquals("Incorrect clause", "male:true", clauses[4]);
        assertEquals("Incorrect clause", ")", clauses[5]);
        assertEquals("Incorrect clause", ")", clauses[6]);
    }

    @Test
    public void testSearchParseQueryAndOrGroups() {
        Search search = parser.parse("(name:foo OR male:true) AND date:1990-01-01");
        List<Search.SearchClause> clauses = search.getClauses();
        assertEquals("Should have top level and clause", 1, clauses.size());
        Search.SearchClause topAnd = clauses.get(0);
        assertEquals("Should have top level and clause", Search.CompoundClause.class, topAnd.getClass());
        assertEquals("sql not correct", "((name ILIKE ? OR male = ?) AND (date >= ? AND date <= ?))", topAnd.getSql());
    }

    @Test
    public void testSearchParseQueryAndOrGroups2() {
        Search search = parser.parse("(name:foo AND male:true) OR date:1990-01-01");
        List<Search.SearchClause> clauses = search.getClauses();
        assertEquals("Should have top level and clause", 1, clauses.size());
        Search.SearchClause topAnd = clauses.get(0);
        assertEquals("Should have top level and clause", Search.CompoundClause.class, topAnd.getClass());
        assertEquals("sql not correct", "((name ILIKE ? AND male = ?) OR (date >= ? AND date <= ?))", topAnd.getSql());
    }

    @Test
    public void testSearchParseQueryDefaultGroup() {
        Search search = parser.parse("name:foo AND male:true");
        List<Search.SearchClause> clauses = search.getClauses();
        assertEquals("Should have top level and clause", 1, clauses.size());
        Search.SearchClause topAnd = clauses.get(0);
        assertEquals("Should have top level and clause", Search.CompoundClause.class, topAnd.getClass());
        assertEquals("sql not correct", "(name ILIKE ? AND male = ?)", topAnd.getSql());
    }

    @Test
    public void testSearchParseQueryNestedGroups() {
        Search search = parser.parse("(((name:foo AND male:true)))");
        List<Search.SearchClause> clauses = search.getClauses();
        assertEquals("Should have top level and clause", 1, clauses.size());
        Search.SearchClause topAnd = clauses.get(0);
        assertEquals("Should have top level and clause", Search.CompoundClause.class, topAnd.getClass());
        assertEquals("sql not correct", "(name ILIKE ? AND male = ?)", topAnd.getSql());
    }

    @Test
    public void testSearchParseQueryLotsOfUnnessesaryGroups() {
        Search search = parser.parse("((((name:foo AND male:true))) OR (date:1990-01-01))");
        List<Search.SearchClause> clauses = search.getClauses();
        assertEquals("Should have top level and clause", 1, clauses.size());
        Search.SearchClause topAnd = clauses.get(0);
        assertEquals("Should have top level and clause", Search.CompoundClause.class, topAnd.getClass());
        assertEquals("sql not correct", "((name ILIKE ? AND male = ?) OR (date >= ? AND date <= ?))", topAnd.getSql());
    }

    @Test
    public void testSearchParseQueryMixedAndOrs() {
        Search search = parser.parse("name:foo AND male:true OR name:bar AND male:false OR date:1990-01-01");
        List<Search.SearchClause> clauses = search.getClauses();
        assertEquals("Should have top level and clause", 1, clauses.size());
        Search.SearchClause topAnd = clauses.get(0);
        assertEquals("Should have top level and clause", Search.CompoundClause.class, topAnd.getClass());
        assertEquals("sql not correct", "((name ILIKE ? AND male = ?) OR (name ILIKE ? AND male = ?) OR (date >= ? AND date <= ?))", topAnd.getSql());
    }

    @Test
    public void testSearchParseQueryMixedAndOrsMax() {
        Search search = parser.parse("name:foo AND (male:true) OR name:bar AND (male:false OR date:1990-01-01)");
        List<Search.SearchClause> clauses = search.getClauses();
        assertEquals("Should have top level and clause", 1, clauses.size());
        Search.SearchClause topAnd = clauses.get(0);
        assertEquals("Should have top level and clause", Search.CompoundClause.class, topAnd.getClass());
        assertEquals("sql not correct", "((name ILIKE ? AND male = ?) OR (name ILIKE ? AND (male = ? OR (date >= ? AND date <= ?))))", topAnd.getSql());
    }

    @Test
    public void testSearchParseExtraSpaces() {
        String[] clauses = parser.parseText(" name:foo  male:true   date:1990-01-01 ");
        assertEquals("Incorrect clause count.", 3, clauses.length);
        assertEquals("Incorrect clause", "name:foo", clauses[0]);
        assertEquals("Incorrect clause", "male:true", clauses[1]);
        assertEquals("Incorrect clause", "date:1990-01-01", clauses[2]);
    }

    @Test
    public void testSearchParseWithQuotes() {
        String[] clauses = parser.parseText("name:\"Greg Leitheiser\" male:true date:1990-01-01");
        assertEquals("Incorrect clause count.", 3, clauses.length);
        assertEquals("Incorrect clause", "name:\"Greg Leitheiser\"", clauses[0]);
        assertEquals("Incorrect clause", "male:true", clauses[1]);
        assertEquals("Incorrect clause", "date:1990-01-01", clauses[2]);
    }

    @Test
    public void testSearchParseWithBraceInQuotes() {
        String[] clauses = parser.parseText("name:\"Greg [Leitheiser\" male:true date:1990-01-01");
        assertEquals("Incorrect clause count.", 3, clauses.length);
        assertEquals("Incorrect clause", "name:\"Greg [Leitheiser\"", clauses[0]);
        assertEquals("Incorrect clause", "male:true", clauses[1]);
        assertEquals("Incorrect clause", "date:1990-01-01", clauses[2]);
    }

    @Test
    public void testSearchParseWithCloseBraceInQuotes() {
        String[] clauses = parser.parseText("name:\"Greg ]Leitheiser\" male:true date:1990-01-01");
        assertEquals("Incorrect clause count.", 3, clauses.length);
        assertEquals("Incorrect clause", "name:\"Greg ]Leitheiser\"", clauses[0]);
        assertEquals("Incorrect clause", "male:true", clauses[1]);
        assertEquals("Incorrect clause", "date:1990-01-01", clauses[2]);
    }


    @Test
    public void testSearchParseWithRange() {
        String[] clauses = parser.parseText("name:Greg male:true date:[1990-01-01 TO 2000-01-01]");
        assertEquals("Incorrect clause count.", 3, clauses.length);
        assertEquals("Incorrect clause", "name:Greg", clauses[0]);
        assertEquals("Incorrect clause", "male:true", clauses[1]);
        assertEquals("Incorrect clause", "date:[1990-01-01 TO 2000-01-01]", clauses[2]);
    }

    @Test(expected = RuntimeException.class)
    public void testSearchParserFailsWithUnmatchedQuote() {
        String[] clauses = parser.parseText("name:\"Greg male:true date:1990-01-01");
    }

    @Test(expected = RuntimeException.class)
    public void testSearchParserFailsWithUnmatchedRange() {
        String[] clauses = parser.parseText("name:Greg male:true date:[1990-01-01 TO 2000-01-01");
    }

    @Test(expected = RuntimeException.class)
    public void testSearchParserFailsWithDoubleRangeOpen() {
        String[] clauses = parser.parseText("name:Greg male:true date:[[1990-01-01 TO 2000-01-01]");
    }

    @Test(expected = RuntimeException.class)
    public void testSearchParserFailsWithMissingRangeOpen() {
        String[] clauses = parser.parseText("name:Greg male:true date:1990-01-01 TO 2000-01-01]");
    }

    @Test(expected = RuntimeException.class)
    public void testSearchParseWithQuotesHidingBrace() {
        String[] clauses = parser.parseText("name:\"Greg Leitheiser male:true date:[1990-01-01 \"TO 2000-01-01]");
    }
}
