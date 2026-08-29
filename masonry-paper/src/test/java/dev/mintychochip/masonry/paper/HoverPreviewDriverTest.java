package dev.mintychochip.masonry.paper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HoverPreviewDriverTest {
    @Test
    void autoArmRequiresPermissionAndBrickWithNoExistingPlan() {
        assertTrue(HoverPreviewDriver.shouldAutoArmExtension(false, false, true, true));
        assertFalse(HoverPreviewDriver.shouldAutoArmExtension(true, false, true, true));
        assertFalse(HoverPreviewDriver.shouldAutoArmExtension(false, true, true, true));
        assertFalse(HoverPreviewDriver.shouldAutoArmExtension(false, false, false, true));
        assertFalse(HoverPreviewDriver.shouldAutoArmExtension(false, false, true, false));
    }
}
