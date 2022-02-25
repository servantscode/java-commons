package org.servantscode.commons;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {
    @Test
    public void isEmptyTestEmptyString() {
        assertTrue("Incorrect boolean", StringUtils.isEmpty(""));
    }

    @Test
    public void isEmptyTestNullString() {
        assertTrue("Incorrect boolean", StringUtils.isEmpty(null));
    }

    @Test
    public void isEmptyTestNonEmptyString() {
        assertFalse("Incorrect boolean", StringUtils.isEmpty("Hello"));
    }

    @Test
    public void isEmptyTestSingleSpaceString() {
        assertTrue("Incorrect boolean", StringUtils.isEmpty(" "));
    }

    @Test
    public void isEmptyTestEscapedString() {
        assertFalse("Incorrect boolean", StringUtils.isEmpty("\\"));
    }

    @Test
    public void isSetTestEmptyString() {
        assertFalse("Incorrect boolean", StringUtils.isSet(""));
    }

    @Test
    public void isSetTestNullString() {
        assertFalse("Incorrect boolean", StringUtils.isSet(null));
    }

    @Test
    public void isSetTestNonEmptyString() {
        assertTrue("Incorrect boolean", StringUtils.isSet("Hello"));
    }

    @Test
    public void isSetTestSingleSpaceString() {
        assertFalse("Incorrect boolean", StringUtils.isSet(" "));
    }

    @Test
    public void isSetTestEscapedString() {
        assertTrue("Incorrect boolean", StringUtils.isSet("\\"));
    }



}
