package com.whim.starcommand.engine;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests for the minimal JSON reader. */
public class JsonTest {

    @SuppressWarnings("unchecked")
    @Test
    public void parsesObjectsArraysAndScalars() {
        Object v = Json.parse("{ \"a\": 1, \"b\": [true, false, null], \"c\": \"hi\" }");
        Map<String, Object> m = (Map<String, Object>) v;
        assertEquals(1.0, (Double) m.get("a"), 0.0001);
        List<Object> b = (List<Object>) m.get("b");
        assertEquals(Boolean.TRUE, b.get(0));
        assertEquals(Boolean.FALSE, b.get(1));
        assertTrue(b.get(2) == null);
        assertEquals("hi", m.get("c"));
    }

    @Test
    public void parsesNegativeAndDecimalNumbers() {
        Object v = Json.parse("[-3, 4.5, 1e2]");
        @SuppressWarnings("unchecked")
        List<Object> l = (List<Object>) v;
        assertEquals(-3.0, (Double) l.get(0), 0.0001);
        assertEquals(4.5, (Double) l.get(1), 0.0001);
        assertEquals(100.0, (Double) l.get(2), 0.0001);
    }

    @Test
    public void handlesEscapesInStrings() {
        Object v = Json.parse("\"line\\nbreak \\\"q\\\"\"");
        assertEquals("line\nbreak \"q\"", v);
    }

    @Test
    public void emptyContainers() {
        assertTrue(((Map<?, ?>) Json.parse("{}")).isEmpty());
        assertTrue(((List<?>) Json.parse("[]")).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTrailingGarbage() {
        Json.parse("{} oops");
    }
}
