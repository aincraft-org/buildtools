package dev.mintychochip.masonry.common.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.masonry.api.clipboard.BlockOffset;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import org.junit.jupiter.api.Test;

class ExtensionPlanTest {
    @Test
    void endpointAndSelectionStartAfterTheAnchor() {
        BlockPosition anchor = new BlockPosition("world", 0, 64, 0);
        ExtensionPlan plan = new ExtensionPlan(
                anchor, new BlockOffset(1, 0, 0), BlockState.of("minecraft:bricks"), 3, 0);

        assertEquals(new BlockPosition("world", 3, 64, 0), plan.endpoint());
        assertEquals(new BlockPosition("world", 1, 64, 0), plan.selection().min());
        assertEquals(new BlockPosition("world", 3, 64, 0), plan.selection().max());
    }
}
