package org.servantscode.commons;

import org.junit.Test;

import java.nio.BufferUnderflowException;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ConfigUtilsTest {

    @Test
    public void encryptTestNormal() {
        assertThat(ConfigUtils.encryptConfig("Value"), not(equalTo("Value")));
    }

    @Test(expected = NullPointerException.class)
    public void encryptTestNull() {
        ConfigUtils.encryptConfig(null);
    }

    @Test
    public void encryptTestEmpty() {
        assertThat(ConfigUtils.encryptConfig(""), not(equalTo("")));
    }

    @Test
    public void encryptTestLong() {
        String value = "XsQex4afFuj9DdswHH2JBhKDyfuQHgE6Su8qBxhwzM2dZnxqXsQex4afFuj9ljkljkl;io;jhgyufr3w2qawesrdhfgbhjiok9o78ytydre45r6t7yuiokjlhbgvfhdtre56t789ioklnjbhvg5456fresdDdswHH2JBhKDyfuQHgE6Su8qBxhwzM2dZnxqhfdsUDhHF742jkF(f456#rr42*&REW&*gfs*(rw74ewrHFssdf";
        assertThat(ConfigUtils.encryptConfig(value), not(equalTo(value)));
    }

    @Test
    public void encryptTestRepeat() {
        String value = "Value";
        HashSet<String> set = new HashSet<>();
        set.add("Value");
        for (int i = 0; i < 2000; i++) {
            String encrypted = ConfigUtils.encryptConfig(value);
            if (set.contains(encrypted)) {
                assertEquals("Repeated encrypt value", 1, 0);
            } else {
                set.add(encrypted);
            }
        }
    }

    @Test
    public void decryptTestNormal() {
        String value = "Value";
        assertEquals("Incorrect string while decrypting.", value, ConfigUtils.decryptConfig(ConfigUtils.encryptConfig(value)));
    }

    @Test(expected = NullPointerException.class)
    public void decryptTestNull() {
        ConfigUtils.decryptConfig(null);
    }

    @Test(expected = BufferUnderflowException.class)
    public void decryptTestEmpty() {
        ConfigUtils.decryptConfig("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void decryptTestUnrelated() {
        ConfigUtils.decryptConfig("Value");
    }

    @Test
    public void decryptTestLong() {
        String value = "XsQex4afFuj9DdswHH2JBhKDyfuQHgE6Su8qBxhwzM2dZnxqXsQex4afFuj9ljkljkl;io;jhgyufr3w2qawesrdhfgbhjiok9o78ytydre45r6t7yuiokjlhbgvfhdtre56t789ioklnjbhvg5456fresdDdswHH2JBhKDyfuQHgE6Su8qBxhwzM2dZnxqhfdsUDhHF742jkF(f456#rr42*&REW&*gfs*(rw74ewrHFssdf";
        assertEquals("Incorrect string while decrypting a long string.", value, ConfigUtils.decryptConfig(ConfigUtils.encryptConfig(value)));
    }

    @Test
    public void decryptTestRepeatedNoOrder() {
        String[] values = new String[]{"Hello there,", "This is a message", "42", "Hello World", "Moo", "This is also a message", "GoodBye"};
        String[] encrypted = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            encrypted[i] = ConfigUtils.encryptConfig(values[i]);
        }
        assertEquals("Incorrect string when decrypting out of order", values[3], ConfigUtils.decryptConfig(encrypted[3]));
        assertEquals("Incorrect string when decrypting out of order", values[2], ConfigUtils.decryptConfig(encrypted[2]));
        assertEquals("Incorrect string when decrypting out of order", values[5], ConfigUtils.decryptConfig(encrypted[5]));
        assertEquals("Incorrect string when decrypting out of order", values[6], ConfigUtils.decryptConfig(encrypted[6]));
        assertEquals("Incorrect string when decrypting out of order", values[1], ConfigUtils.decryptConfig(encrypted[1]));
        assertEquals("Incorrect string when decrypting out of order", values[0], ConfigUtils.decryptConfig(encrypted[0]));
        assertEquals("Incorrect string when decrypting out of order", values[4], ConfigUtils.decryptConfig(encrypted[4]));
    }


}
