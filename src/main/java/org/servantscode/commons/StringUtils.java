package org.servantscode.commons;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class StringUtils {
    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean isSet(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public static String trim(String s) {
        return s == null? s: s.trim();
    }

    public static boolean areEqual(String s1, String s2) { return (s1 == null)? s2 == null: s1.equals(s2); }

    public static String stripQuotes(String value) {
        if (value.startsWith("\""))
            value = value.substring(1);
        if (value.endsWith("\""))
            value = value.substring(0, value.length() - 1);
        return value;
    }

    public static String stripAllTags(String input) {
        return isSet(input)? Jsoup.clean(input, Safelist.none()): input;
    }

    public static String stripUnsafeTags(String input) {
        return isSet(input)? Jsoup.clean(input, Safelist.relaxed()): input;
    }
}
