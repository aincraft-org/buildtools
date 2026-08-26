package dev.mintychochip.masonry.common.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.command.CommandResult;
import dev.mintychochip.masonry.api.operation.BlockChange;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import dev.mintychochip.masonry.common.support.TestHarness;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped replace, fill, copy, and paste through {@code MasonryCommands}.
 */
class ToolsLifecycleTest {
    @Test
    void replaceChangesOnlyMatchingBlocksAndPreviewEqualsExecute() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(2, 64, 0);
        harness.world.put(a, BlockState.of("minecraft:dirt"));
        harness.world.put(harness.pos(1, 64, 0), BlockState.of("minecraft:stone"));
        harness.world.put(b, BlockState.of("minecraft:dirt"));
        harness.survival.give(TestHarness.ACTOR, "minecraft:oak_planks", 8);
        select(harness, a, b);

        CommandResult result = harness.commands.execute(
                harness.command(a, a, "replace", "minecraft:dirt", "minecraft:oak_planks"));

        assertTrue(result.success(), result.message());
        assertEquals(
                result.preview().affectedPositions(),
                result.record().changes().stream().map(BlockChange::position).toList());
        assertEquals(2, result.preview().affectedCount());
        assertEquals(BlockState.of("minecraft:oak_planks"), harness.world.getBlock(a));
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(harness.pos(1, 64, 0)));
        assertEquals(BlockState.of("minecraft:oak_planks"), harness.world.getBlock(b));
    }

    @Test
    void fillWritesTargetAcrossSelection() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 70, 0);
        BlockPosition b = harness.pos(1, 70, 1);
        harness.world.fill(a, b, BlockState.of("minecraft:air"));
        harness.survival.give(TestHarness.ACTOR, "minecraft:cobblestone", 16);
        select(harness, a, b);

        CommandResult result = harness.commands.execute(harness.command(a, a, "fill", "minecraft:cobblestone"));

        assertTrue(result.success(), result.message());
        assertEquals(4, result.preview().affectedCount());
        assertEquals(
                result.preview().affectedPositions(),
                result.record().changes().stream().map(BlockChange::position).toList());
        for (BlockPosition position : List.of(a, harness.pos(1, 70, 0), harness.pos(0, 70, 1), b)) {
            assertEquals(BlockState.of("minecraft:cobblestone"), harness.world.getBlock(position));
        }
    }

    @Test
    void copyThenPasteReproducesCopiedStates() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(1, 64, 0);
        BlockState stairs = new BlockState("minecraft:oak_stairs", java.util.Map.of("facing", "east", "half", "bottom"));
        harness.world.put(a, BlockState.of("minecraft:gold_block"));
        harness.world.put(b, stairs);
        select(harness, a, b);

        CommandResult copy = harness.commands.execute(harness.command(a, a, "copy"));
        assertTrue(copy.success(), copy.message());

        BlockPosition dest = harness.pos(10, 64, 10);
        harness.sessions.session(TestHarness.ACTOR).setPos1(dest);
        harness.survival.give(TestHarness.ACTOR, "minecraft:gold_block", 4);
        harness.survival.give(TestHarness.ACTOR, "minecraft:oak_stairs", 4);

        CommandResult paste = harness.commands.execute(harness.command(dest, dest, "paste"));
        assertTrue(paste.success(), paste.message());
        assertEquals(
                paste.preview().affectedPositions(),
                paste.record().changes().stream().map(BlockChange::position).toList());
        assertEquals(BlockState.of("minecraft:gold_block"), harness.world.getBlock(dest));
        assertEquals(stairs, harness.world.getBlock(dest.offset(1, 0, 0)));
        assertEquals(BlockState.of("minecraft:gold_block"), harness.world.getBlock(a));
    }

    @Test
    void mutatingToolWithoutSelectionIsRefused() {
        TestHarness harness = new TestHarness();
        harness.survival.give(TestHarness.ACTOR, "minecraft:stone", 8);
        CommandResult result = harness.commands.execute(
                harness.command(harness.pos(0, 64, 0), harness.pos(0, 64, 0), "fill", "minecraft:stone"));
        assertFalse(result.success());
        assertEquals("A valid selection is required", result.message());
    }

    private static void select(TestHarness harness, BlockPosition a, BlockPosition b) {
        assertTrue(harness.commands.execute(harness.command(a, a, "pos1")).success());
        assertTrue(harness.commands.execute(harness.command(b, b, "pos2")).success());
    }
}
