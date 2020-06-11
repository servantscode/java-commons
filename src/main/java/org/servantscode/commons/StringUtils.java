package org.servantscode.commons;

public class StringUtils {
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isSet(String s) {
        return s != null && !s.isEmpty();
    }

    public static boolean areEqual(String s1, String s2) { return (s1 == null)? s2 == null: s1.equals(s2); }
}
