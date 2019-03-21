package org.servantscode.commons;

public class StringUtils {
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isSet(String s) {
        return s != null && !s.isEmpty();
    }
}
