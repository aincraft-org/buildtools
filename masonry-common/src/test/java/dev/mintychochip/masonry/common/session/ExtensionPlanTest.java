package dev.mintychochip.masonry.common.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.mintychochip.masonry.api.clipboard.BlockOffset;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import org.junit.jupiter.api.Test;

/** Tests the horizontal-plane extension geometry. */
class ExtensionPlanTest {

    @Test
    void endpointAndSelectionStartAfterTheAnchor() {
        BlockPosition anchor = new BlockPosition("world", 0, 64, 0);
        ExtensionPlan plan = new ExtensionPlan(
                anchor, new BlockOffset(1, 0, 0), BlockState.of("minecraft:bricks"), 3, 0);

        assertEquals(new BlockPosition("world", 3, 64, 0), plan.endpoint());
        assertEquals(
                new CuboidSelection(
                        new BlockPosition("world", 1, 64, 0),
                        new BlockPosition("world", 3, 64, 0)),
                plan.selection());
    }

    @Test
    void widthSpansPerpendicularAndKeepsEndpoint() {
        BlockPosition anchor = new BlockPosition("world", 0, 64, 5);
        ExtensionPlan plan = new ExtensionPlan(
                anchor, new BlockOffset(1, 0, 0), BlockState.of("minecraft:bricks"), 3, 3, 0);

        assertEquals(new BlockPosition("world", 3, 64, 5), plan.endpoint());
        assertEquals(
                new CuboidSelection(
                        new BlockPosition("world", 1, 64, 4),
                        new BlockPosition("world", 3, 64, 6)),
                plan.selection());
    }

    @Test
    void widthAlongZDirectionSpansX() {
        BlockPosition anchor = new BlockPosition("world", 4, 64, 0);
        ExtensionPlan plan = new ExtensionPlan(
                anchor, new BlockOffset(0, 0, 1), BlockState.of("minecraft:bricks"), 2, 3, 0);

        assertEquals(
                new CuboidSelection(
                        new BlockPosition("world", 3, 64, 1),
                        new BlockPosition("world", 5, 64, 2)),
                plan.selection());
    }

    @Test
    void defaultWidthIsOne() {
        BlockPosition anchor = new BlockPosition("world", 0, 64, 0);
        ExtensionPlan plan = new ExtensionPlan(
                anchor, new BlockOffset(1, 0, 0), BlockState.of("minecraft:bricks"), 1, 0);

        assertEquals(1, plan.width());
    }

    @Test
    void widthMustBePositive() {
        BlockPosition anchor = new BlockPosition("world", 0, 64, 0);
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExtensionPlan(anchor, new BlockOffset(1, 0, 0), BlockState.of("minecraft:bricks"), 1, 0, 0));
    }
}