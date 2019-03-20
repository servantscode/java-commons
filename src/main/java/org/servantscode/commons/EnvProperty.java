package org.servantscode.commons;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.servantscode.commons.StringUtils.isEmpty;

public class EnvProperty {
    private static final Logger LOG = LogManager.getLogger(EnvProperty.class);

    public static String get(String propName, String defaultValue) {
        String lookup = get(propName);
        LOG.debug(String.format("Got env property %s value: %s", propName, lookup));
        return !isEmpty(lookup)? lookup: defaultValue;
    }

    public static String get(String propName) {
        String value = System.getenv(propName);
        return (value == null)? value: value.trim();
    }
}
