package dev.mintychochip.masonry.common.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.tool.ToolRequest;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import dev.mintychochip.masonry.common.support.TestHarness;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurvivalFillToolTest {
    @Test
    void fillOnlyReplacesAirAndReplaceableBlocks() {
        TestHarness harness = new TestHarness();
        harness.registry.register(new SurvivalFillTool());

        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(1, 65, 0);

        harness.world.put(a, BlockState.of("minecraft:air"));
        harness.world.put(b, BlockState.of("minecraft:stone"));
        harness.world.withReplaceable("minecraft:air");
        harness.survival.give(TestHarness.ACTOR, "minecraft:stone", 16);

        CuboidSelection selection = new CuboidSelection(a, b);
        ToolRequest request = new ToolRequest(
                TestHarness.ACTOR, "survival_fill", selection, Map.of("block", "minecraft:stone"));

        var record = harness.executor.execute(request, harness.world, harness.survival);
        assertTrue(record.isPresent(), record.toString());
        assertEquals(3, record.get().changes().size(), "only the three air positions should change");
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(a));
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(new BlockPosition(TestHarness.WORLD, 1, 64, 0)));
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(new BlockPosition(TestHarness.WORLD, 0, 65, 0)));
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(b));
        assertEquals(13, harness.survival.count(TestHarness.ACTOR, "minecraft:stone"), "16 stone minus 3 placed");
    }
}
