package dev.mintychochip.buildtools.api.limits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Tests locked default limits and rejection of non-positive values. */
class OperationLimitsTest {
    @Test
    void defaultsMatchLockedValues() {
        OperationLimits limits = OperationLimits.defaults();
        assertEquals(6, limits.interactionDistance());
        assertEquals(64, limits.selectionExtent());
        assertEquals(32_768, limits.maxOperationBlocks());
    }

    @Test
    void rejectsNonPositiveLimits() {
        assertThrows(IllegalArgumentException.class, () -> new OperationLimits(0, 64, 1));
        assertThrows(IllegalArgumentException.class, () -> new OperationLimits(6, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new OperationLimits(6, 64, 0));
    }
}
