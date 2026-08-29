package dev.mintychochip.masonry.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.masonry.api.clipboard.BlockOffset;
import dev.mintychochip.masonry.api.world.BlockPosition;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

class GadgetListenerTest {

    @Test
    void topFaceUsesHorizontalAimBeforeTargetDirection() {
        assertEquals(
                new BlockOffset(1, 0, 0),
                GadgetListener.extensionDirection(BlockFace.UP, 0, 3, 0.9, -0.2));
    }

    @Test
    void topFaceFallsBackToTargetDirectionWhenHorizontalAimIsZero() {
        assertEquals(
                new BlockOffset(0, 0, -1),
                GadgetListener.extensionDirection(BlockFace.UP, 0, -3, 0.0, 0.0));
    }

    @Test
    void sideFaceUsesTheClickedFaceDirection() {
        assertEquals(
                new BlockOffset(0, 0, -1),
                GadgetListener.extensionDirection(BlockFace.NORTH, -4, 2, -0.2, 0.9));
    }

    @Test
    void holdCycleUsesSelectedLengthUntilReachLimit() {
        assertEquals(3, GadgetListener.extensionCycleLength(3, 0, 6));
        assertEquals(2, GadgetListener.extensionCycleLength(3, 4, 6));
        assertEquals(0, GadgetListener.extensionCycleLength(3, 6, 6));
        assertEquals(0, GadgetListener.extensionCycleLength(3, 0, 0));
    }

    @Test
    void extensionMaxLengthUsesReachFromAnchorToTip() {
        BlockPosition origin = new BlockPosition("world", 0, 64, 0);
        BlockOffset direction = new BlockOffset(0, 0, 1);

        assertEquals(
                5,
                GadgetListener.extensionMaxLength(
                        origin, new BlockPosition("world", 0, 64, 1), direction, 6.0, 64));
        assertEquals(
                1,
                GadgetListener.extensionMaxLength(
                        origin, new BlockPosition("world", 0, 64, 5), direction, 6.0, 64));
    }
}
