package org.servantscode.commons;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class AutoCompleteComparatorTest {

    @Test
    public void testSort() {
        String search = "An";
        ArrayList<String> names = new ArrayList<>();
        names.add("Stephanie Leitheiser");
        names.add("Joshua Anberline");
        names.add("Anneliese Leitheiser");
        names.add("Joshua\tAnberline");

        names.sort(new AutoCompleteComparator(search));

        ArrayList<String> expectedNames = new ArrayList<>(4);
        expectedNames.add("Anneliese Leitheiser");
        expectedNames.add("Joshua Anberline");
        expectedNames.add("Joshua\tAnberline");
        expectedNames.add("Stephanie Leitheiser");

        Assert.assertArrayEquals("names not sorted correctly", expectedNames.toArray(), names.toArray());
    }
}
