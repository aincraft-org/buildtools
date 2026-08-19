package dev.mintychochip.buildtools.api.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BlockPositionTest {
    @Test
    void equalityIsWorldAndCoordinates() {
        BlockPosition left = new BlockPosition("world", 1, 64, -3);
        BlockPosition same = new BlockPosition("world", 1, 64, -3);
        BlockPosition otherWorld = new BlockPosition("nether", 1, 64, -3);
        BlockPosition otherCoord = new BlockPosition("world", 2, 64, -3);

        assertEquals(left, same);
        assertEquals(left.hashCode(), same.hashCode());
        assertNotEquals(left, otherWorld);
        assertNotEquals(left, otherCoord);
    }

    @Test
    void rejectsNullWorldId() {
        assertThrows(NullPointerException.class, () -> new BlockPosition(null, 0, 0, 0));
    }
}
