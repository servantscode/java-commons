package org.servantscode.commons;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReflectionUtilsTest {

    private class DeepTestClass {
        String level1 = "foo";
        ShallowTestClass shallow = new ShallowTestClass();

        public String getLevel1() { return level1; }
        public void setLevel1(String level1) { this.level1 = level1; }

        public ShallowTestClass getShallow() { return shallow; }
        public void setShallow(ShallowTestClass shallow) { this.shallow = shallow; }

        private class ShallowTestClass {
            String level2 = "bar";
            int number2 = 2;

            public String getLevel2() { return level2; }
            public void setLevel2(String level2) { this.level2 = level2; }

            public int getNumber2() { return number2; }
            public void setNumber2(int number2) { this.number2 = number2; }
        }
    }

    @Test
    public void testFieldType() {
        DeepTestClass testObj = new DeepTestClass();
        assertEquals("Could not get proper class type", String.class, ReflectionUtils.getDeepFieldType(DeepTestClass.class, "level1"));
        assertEquals("Could not get proper class type", String.class, ReflectionUtils.getDeepFieldType(DeepTestClass.class, "shallow.level2"));
        assertEquals("Could not get proper class type", int.class, ReflectionUtils.getDeepFieldType(DeepTestClass.class, "shallow.number2"));
    }
}
