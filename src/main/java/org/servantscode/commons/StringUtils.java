package org.servantscode.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.util.Collection;
import java.util.stream.Collectors;

public class StringUtils {
    private static final Logger LOG = LogManager.getLogger(StringUtils.class);

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

    public static String join(Collection<Integer> ints) {
        return join(", ", ints);
    }

    public static String join(String delimiter, Collection<Integer> ints) {
        return ints.stream().map(i -> Integer.toString(i)).collect(Collectors.joining(delimiter));
    }

    public static String toJson(Object o) {
        try {
            return ObjectMapperFactory.getMapper().writeValueAsString(o);
        } catch (JsonProcessingException e) { LOG.error("Could not map object to json...", new Exception()); }

        return "**Unprintable object**";
    }
}
