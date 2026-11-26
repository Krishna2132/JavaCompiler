package rkcc.compiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Minimal JUnit 5 build verification test for Milestone 01.
 */
class BuildVerificationTest {

    @Test
    void verifiesMavenJUnitAndPackageSetup() {
        assertEquals("rkcc.compiler", BuildVerificationTest.class.getPackageName());
        assertTrue(Runtime.version().feature() >= 17,
                "Expected Java 17 or newer runtime");
    }
}
