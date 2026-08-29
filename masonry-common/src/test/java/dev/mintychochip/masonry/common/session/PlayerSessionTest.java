package dev.mintychochip.masonry.common.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.clipboard.BlockOffset;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import org.junit.jupiter.api.Test;

class PlayerSessionTest {
    @Test
    void clearResetsModeToFill() {
        PlayerSession session = new PlayerSession();
        session.setMode(ToolMode.PASTE);
        session.clear();
        assertEquals(ToolMode.FILL, session.mode());
    }

    @Test
    void clearRemovesExtensionPreviewState() {
        PlayerSession session = new PlayerSession();
        session.setExtensionPlan(new ExtensionPlan(
                new BlockPosition("world", 0, 64, 0),
                new BlockOffset(1, 0, 0),
                BlockState.of("minecraft:bricks"),
                2,
                0));

        session.clear();

        assertTrue(session.extensionPlan().isEmpty());
    }
}
