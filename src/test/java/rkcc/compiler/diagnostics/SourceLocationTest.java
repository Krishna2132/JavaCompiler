package rkcc.compiler.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SourceLocation}.
 */
class SourceLocationTest {

    @Test
    void createsValidSourceLocationAndExposesCoordinates() {
        SourceLocation location = new SourceLocation(12, 8, 245);

        assertEquals(12, location.line());
        assertEquals(8, location.column());
        assertEquals(245, location.offset());
        assertEquals("12:8", location.formatCompact());
        assertEquals("SourceLocation[line=12, column=8, offset=245]", location.toString());

        assertEquals(1, SourceLocation.START.line());
        assertEquals(1, SourceLocation.START.column());
        assertEquals(0, SourceLocation.START.offset());
    }

    @Test
    void rejectsInvalidLineColumnAndOffsetArguments() {
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation(0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation(-3, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation(1, -2, 0));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation(1, 1, -1));
    }

    @Test
    void verifiesEqualityHashCodeAndImmutability() {
        SourceLocation first = new SourceLocation(3, 15, 42);
        SourceLocation equalCopy = new SourceLocation(3, 15, 42);
        SourceLocation differentLine = new SourceLocation(4, 15, 42);
        SourceLocation differentOffset = new SourceLocation(3, 15, 43);

        assertEquals(first, equalCopy);
        assertEquals(first.hashCode(), equalCopy.hashCode());
        assertNotEquals(first, differentLine);
        assertNotEquals(first, differentOffset);
        assertTrue(SourceLocation.class.isRecord());
    }
}
