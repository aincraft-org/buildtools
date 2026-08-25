package dev.mintychochip.masonry.common.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.world.BlockState;
import org.junit.jupiter.api.Test;

class BlockStatesTest {

    @Test
    void acceptsNamespacedUnnamespacedAndProperties() {
        assertEquals(BlockState.of("minecraft:stone"), BlockStates.parse("stone"));
        assertEquals(BlockState.of("minecraft:stone"), BlockStates.parse("minecraft:stone"));

        BlockState withProps = BlockStates.parse("oak_stairs[facing=east]");
        assertEquals("minecraft:oak_stairs", withProps.namespacedKey());
        assertEquals("east", withProps.properties().get("facing"));

        BlockState withNamespaceProps = BlockStates.parse("minecraft:oak_stairs[facing=east,half=top]");
        assertEquals("minecraft:oak_stairs", withNamespaceProps.namespacedKey());
        assertEquals("east", withNamespaceProps.properties().get("facing"));
        assertEquals("top", withNamespaceProps.properties().get("half"));
    }

    @Test
    void rejectsBlankOrMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> BlockStates.parse(""));
        assertThrows(IllegalArgumentException.class, () -> BlockStates.parse("   "));
        assertThrows(IllegalArgumentException.class, () -> BlockStates.parse("stone["));
        assertThrows(IllegalArgumentException.class, () -> BlockStates.parse("stone[facing]"));
    }

    @Test
    void matchesPatternWithOrWithoutProperties() {
        BlockState actual = new BlockState("minecraft:stone", java.util.Map.of());
        BlockState pattern = BlockState.of("minecraft:stone");
        assertTrue(BlockStates.matches(actual, pattern));

        BlockState stairs = new BlockState("minecraft:oak_stairs", java.util.Map.of("facing", "east", "half", "bottom"));
        BlockState anyStairs = BlockState.of("minecraft:oak_stairs");
        assertTrue(BlockStates.matches(stairs, anyStairs));

        BlockState eastStairs = new BlockState("minecraft:oak_stairs", java.util.Map.of("facing", "east"));
        assertTrue(BlockStates.matches(stairs, eastStairs));

        BlockState westStairs = new BlockState("minecraft:oak_stairs", java.util.Map.of("facing", "west"));
        assertFalse(BlockStates.matches(stairs, westStairs));

        BlockState cobble = BlockState.of("minecraft:cobblestone");
        assertFalse(BlockStates.matches(stairs, cobble));
    }
}
