package org.servantscode.commons.search;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchParserTest {
    private SearchParser parser;

    public SearchParserTest() {
        this.parser = new SearchParser(null);
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
