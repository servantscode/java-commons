package org.servantscode.commons;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

public class EnvProperty {
    private static final Logger LOG = LogManager.getLogger(EnvProperty.class);

    private static final HashMap<String, String> props;
    static {
        Map<String, String> env = System.getenv();
        props = new HashMap<>(env);
    }

    public static String get(String propName, String defaultValue) {
        String lookup = get(propName);
        if(isEmpty(lookup)) {
            LOG.warn(String.format("Could not retrieve env property %s. Using default value.", propName));
            return defaultValue;
        }
        return lookup;
    }

    public static String get(String propName) {
        String value = props.get(propName);
        if(isSet(value))
            LOG.debug(String.format("Successfully retrieved env property %s", propName));
        return (value == null)? value: value.trim();
    }


    public static void put(String propName, String value) {
        props.put(propName, value);
    }
}
