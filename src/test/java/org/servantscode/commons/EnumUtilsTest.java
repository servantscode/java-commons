package org.servantscode.commons;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class EnumUtilsTest {
    public enum PledgeFrequency {WEEKLY, MONTHLY, QUARTERLY, ANNUALLY};

    @Test
    public void testListValues() {
        List<String> enumItems = EnumUtils.listValues(PledgeFrequency.class);

        assertEquals("Incorrect enum value (1)", "WEEKLY", enumItems.get(0));
        assertEquals("Incorrect enum value (2)", "MONTHLY", enumItems.get(1));
        assertEquals("Incorrect enum value (3)", "QUARTERLY", enumItems.get(2));
        assertEquals("Incorrect enum value (4)", "ANNUALLY", enumItems.get(3));
    }
}
