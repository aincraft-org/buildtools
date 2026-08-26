package dev.mintychochip.masonry.common.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.command.CommandResult;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import dev.mintychochip.masonry.common.support.TestHarness;
import org.junit.jupiter.api.Test;

/**
 * Exercises cut, move, and repeat through {@code MasonryCommands}.
 */
class CutMoveRepeatTest {
    private static void select(TestHarness harness, BlockPosition a, BlockPosition b) {
        assertTrue(harness.commands.execute(harness.command(a, a, "pos1")).success());
        assertTrue(harness.commands.execute(harness.command(b, b, "pos2")).success());
    }

    @Test
    void cutCopiesToClipboardAndClearsSelection() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(1, 64, 0);
        harness.world.put(a, BlockState.of("minecraft:stone"));
        harness.world.put(b, BlockState.of("minecraft:stone"));
        select(harness, a, b);

        CommandResult result = harness.commands.execute(harness.command(a, a, "cut"));
        assertTrue(result.success(), result.message());
        assertEquals(2, result.record().changes().size());
        assertEquals(BlockState.AIR, harness.world.getBlock(a));
        assertEquals(BlockState.AIR, harness.world.getBlock(b));
        assertEquals(2, harness.sessions.clipboard(TestHarness.ACTOR).orElseThrow().size());

        CommandResult undo = harness.commands.execute(harness.command(a, a, "undo"));
        assertTrue(undo.success(), undo.message());
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(a));
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(b));
    }

    @Test
    void cutSkipsExcludedCells() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(1, 64, 0);
        harness.world.put(a, BlockState.of("minecraft:stone"));
        harness.world.put(b, BlockState.of("minecraft:stone"));
        select(harness, a, b);

        // The gadget excludes the player's body cells; b is excluded so it must survive.
        var context = new dev.mintychochip.masonry.api.command.CommandContext(
                TestHarness.ACTOR,
                TestHarness.WORLD,
                a,
                a,
                java.util.List.of("cut"),
                java.util.Set.of(b));
        CommandResult result = harness.commands.execute(context);
        assertTrue(result.success(), result.message());
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(b));
        assertEquals(BlockState.AIR, harness.world.getBlock(a));
    }

    @Test
    void repeatReRunsLastToolAgainstCurrentSelection() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(1, 64, 0);
        harness.world.put(a, BlockState.of("minecraft:dirt"));
        harness.world.put(b, BlockState.of("minecraft:dirt"));
        harness.survival.give(TestHarness.ACTOR, "minecraft:stone", 16);
        select(harness, a, b);

        assertTrue(harness.commands.execute(
                harness.command(a, a, "fill", "minecraft:stone")).success());

        // Re-select a fresh region and repeat.
        BlockPosition c = harness.pos(4, 64, 0);
        BlockPosition d = harness.pos(5, 64, 0);
        harness.world.put(c, BlockState.of("minecraft:dirt"));
        harness.world.put(d, BlockState.of("minecraft:dirt"));
        select(harness, c, d);
        CommandResult repeated = harness.commands.execute(harness.command(c, c, "repeat"));
        assertTrue(repeated.success(), repeated.message());
        assertEquals("fill", repeated.record().toolName());
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(c));
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(d));
    }

    @Test
    void repeatWithoutLastToolIsRefused() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(1, 64, 0);
        select(harness, a, b);
        CommandResult result = harness.commands.execute(harness.command(a, a, "repeat"));
        assertFalse(result.success());
        assertEquals("Nothing to repeat", result.message());
    }
}
