package org.servantscode.commons;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;

public class AutoCompleteComparator implements Comparator<String> {
    private final String search;

    public AutoCompleteComparator(String search) {
        this.search = search;
    }

    @Override
    public int compare(String str1, String str2) {
        return calculatePriority(str2)-calculatePriority(str1);
    }

    private int calculatePriority(String str) {
        return firstNameStartsWith(str)? 1: anyNameStartsWith(str)? 0: -1;
    }

    private boolean firstNameStartsWith(String str) {
        return str.matches(String.format("(?i:\\A%s.*)", search));
    }

    private boolean anyNameStartsWith(String str) {
        return str.matches(String.format("(?i:.*\\s%s.*)", search));
    }
}
