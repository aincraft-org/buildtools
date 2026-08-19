package dev.mintychochip.buildtools.paper.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaperBlockStatesTest {
    @Test
    void parsesPlainAndStatefulKeysIntoApiOwnedValues() {
        BlockState stone = PaperBlockStates.parse("minecraft:stone");
        assertEquals("minecraft:stone", stone.namespacedKey());
        assertTrue(stone.properties().isEmpty());

        BlockState stairs = PaperBlockStates.parse("minecraft:oak_stairs[facing=east,half=bottom]");
        assertEquals("minecraft:oak_stairs", stairs.namespacedKey());
        assertEquals(Map.of("facing", "east", "half", "bottom"), stairs.properties());
        assertEquals("minecraft:oak_stairs[facing=east,half=bottom]", PaperBlockStates.toBukkitString(stairs));
    }

    @Test
    void rejectsBlankKeys() {
        assertThrows(IllegalArgumentException.class, () -> PaperBlockStates.parse(" "));
    }
}
