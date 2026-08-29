package dev.mintychochip.masonry.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.masonry.api.clipboard.BlockOffset;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

class GadgetListenerTest {

    @Test
    void topFaceUsesDominantHorizontalPlayerToTargetDirection() {
        assertEquals(
                new BlockOffset(0, 0, 1),
                GadgetListener.extensionDirection(BlockFace.UP, 0, 3, 0.8, -0.9));
    }

    @Test
    void sideFaceUsesTheClickedFaceDirection() {
        assertEquals(
                new BlockOffset(0, 0, -1),
                GadgetListener.extensionDirection(BlockFace.NORTH, -4, 2, -0.2, 0.9));
    }
}
