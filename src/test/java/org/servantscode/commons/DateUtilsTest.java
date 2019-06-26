package org.servantscode.commons;

import org.junit.Test;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Iterator;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DateUtilsTest {
    @Test
    public void parseNormalTest() {
        ZonedDateTime zd = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]");
        assertEquals("Incorrect parsing", zd, DateUtils.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"));
    }

    @Test
    public void parseFailedNoDefaultTest() {
        assertNull("Returned wrong value", DateUtils.parse("This is not a date"));
    }

    @Test
    public void parseFailedWithPassedDefaultTest() {
        ZonedDateTime zd = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]");
        assertEquals("Returned Wrong value", zd, DateUtils.parse("This is not a date", zd));
    }

    @Test
    public void parseNullValuesTest() {
        assertNull("Returned not Null", DateUtils.parse(null, (ZonedDateTime) null));
    }

    @Test
    public void parseLoopedTest() {
        Iterator<String> iter = ZoneId.getAvailableZoneIds().iterator();
        ZonedDateTime zd;
        Random numGen = new Random(170174656L);
        while (iter.hasNext()) {
            String id = iter.next();
            for (int i = 0; i < 10 && iter.hasNext(); i++) {
                zd = ZonedDateTime.of(LocalDateTime.of(LocalDate.of(numGen.nextInt(1000)+1990,numGen.nextInt(11)+1,1), LocalTime.of(numGen.nextInt(24),numGen.nextInt(60))), ZoneId.of(id));
                zd = zd.plusDays(numGen.nextInt(30));
                assertEquals("Wrong parsed date", zd, DateUtils.parse(zd.toString()));
            }
        }
    }

    // Could give a false failure if run at close to midnight on new years eve,
    // as the method being tested uses ZonedDateTime.now, which could be different
    // from here in the test to in the method called.
    @Test
    public void parseTemporalAdjustedTest() {
        ZonedDateTime expected = ZonedDateTime.now().with(TemporalAdjusters.firstDayOfNextYear());
        ZonedDateTime actual = DateUtils.parse("This is not a date", TemporalAdjusters.firstDayOfNextYear());
        assertEquals("Wrong Date with temporal adjuster", expected.toLocalDate(), actual.toLocalDate());
    }

    @Test
    public void toUTCTest() {
        ZonedDateTime zd = ZonedDateTime.now();
        assertEquals("Wrong date when converting to UTC", ZonedDateTime.ofInstant(zd.toInstant(), ZoneId.of("Z")), DateUtils.toUTC(zd));
    }
}
