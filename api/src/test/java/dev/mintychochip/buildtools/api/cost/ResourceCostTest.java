package dev.mintychochip.buildtools.api.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests {@link ResourceCost} copy-on-write, non-negative counts, and immutability. */
class ResourceCostTest {
    @Test
    void copiesAndExposesItemCounts() {
        Map<String, Integer> input = new HashMap<>();
        input.put("minecraft:stone", 8);
        ResourceCost cost = new ResourceCost(input);

        input.put("minecraft:dirt", 1);
        assertEquals(Map.of("minecraft:stone", 8), cost.itemCounts());
        assertTrue(ResourceCost.none().isEmpty());
    }

    @Test
    void rejectsNegativeCounts() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceCost(Map.of("minecraft:stone", -1)));
    }

    @Test
    void itemCountsAreUnmodifiable() {
        ResourceCost cost = new ResourceCost(Map.of("minecraft:stone", 1));
        assertThrows(UnsupportedOperationException.class, () -> cost.itemCounts().put("minecraft:dirt", 1));
    }
}
