package dev.mintychochip.buildtools.api.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.world.BlockPosition;
import org.junit.jupiter.api.Test;

class CuboidSelectionTest {
    @Test
    void volumeAndExtentAreInclusive() {
        CuboidSelection selection = new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("world", 3, 65, 1));

        assertEquals(4, selection.width());
        assertEquals(2, selection.height());
        assertEquals(2, selection.depth());
        assertEquals(16, selection.volume());
        assertEquals(4, selection.extent());
        assertEquals("world", selection.worldId());
        assertEquals(new BlockPosition("world", 0, 64, 0), selection.min());
        assertEquals(new BlockPosition("world", 3, 65, 1), selection.max());
        assertEquals(16, selection.positions().size());
        assertTrue(selection.contains(new BlockPosition("world", 1, 64, 1)));
    }

    @Test
    void rejectsCornersInDifferentWorlds() {
        assertThrows(IllegalArgumentException.class, () -> new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("nether", 1, 64, 0)));
    }
}
