package dev.mintychochip.buildtools.api.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests valid/invalid factories and unmodifiable error lists. */
class ValidationResultTest {
    @Test
    void validHasNoErrors() {
        ValidationResult result = ValidationResult.passed();
        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void invalidRequiresAtLeastOneError() {
        ValidationResult result = ValidationResult.invalid("too large");
        assertFalse(result.valid());
        assertEquals(List.of("too large"), result.errors());
        assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid(List.of()));
    }

    @Test
    void errorsAreUnmodifiable() {
        ValidationResult result = ValidationResult.invalid("denied");
        assertThrows(UnsupportedOperationException.class, () -> result.errors().add("other"));
    }
}
