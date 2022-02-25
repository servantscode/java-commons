package org.servantscode.commons;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.ConfigDB;

import java.time.*;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.servantscode.commons.StringUtils.isEmpty;

public class DateUtils {
    private static final Logger LOG = LogManager.getLogger(DateUtils.class);

    private static ConfigDB db = new ConfigDB();

    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("America/Chicago");

    public static ZoneId getTimeZone() {
        LOG.trace("Getting timezone");
        String zone;
        try {
            zone = db.getConfiguration("timezone");
        } catch (Throwable t) {
            LOG.error("Failed to get timezone configuration! ", t);
            throw t;
        }
        LOG.trace("got configuration: " + zone);
        if(isEmpty(zone)) return DEFAULT_TIMEZONE;

        try {
            LOG.trace("getting zoneId");
            return ZoneId.of(zone);
        } catch(DateTimeException e) {
            LOG.error("Invalid timezone string encountered: " + zone + ". Using US Central timezone as default.");
            return DEFAULT_TIMEZONE;
        }
    }

    public static ZonedDateTime parse(String input) {
        return parse(input, (ZonedDateTime) null);
    }

    public static ZonedDateTime parse(String input, TemporalAdjuster adjuster) {
        return parse(input, ZonedDateTime.now().with(adjuster));
    }

    public static ZonedDateTime parse(String input, ZonedDateTime defaultValue) {
        try {
            return ZonedDateTime.parse(input);
        } catch (Throwable t) {
            LOG.debug("Returning default date: " +
                    (defaultValue != null? defaultValue.format(ISO_OFFSET_DATE_TIME): "null"));
            return defaultValue;
        }
    }

    public static ZonedDateTime toUTC(ZonedDateTime input) {
        if(input == null)
            return null;
        return ZonedDateTime.ofInstant(input.toInstant(), ZoneId.of("Z"));
    }
}
