package dev.mintychochip.masonry.common.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.command.CommandResult;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import dev.mintychochip.masonry.common.support.TestHarness;
import org.junit.jupiter.api.Test;

/**
 * Exercises the shipped {@code /masonry} surface and asserts validation/execute result content.
 */
class MasonryCommandsTest {
    @Test
    void commandSurfaceReturnsValidationAndExecuteContent() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(1, 64, 0);
        harness.world.put(a, BlockState.of("minecraft:dirt"));
        harness.world.put(b, BlockState.of("minecraft:dirt"));
        harness.survival.give(TestHarness.ACTOR, "minecraft:stone", 16);

        CommandResult pos1 = harness.commands.execute(harness.command(a, a, "pos1"));
        CommandResult pos2 = harness.commands.execute(harness.command(b, b, "pos2"));
        assertTrue(pos1.success(), pos1.message());
        assertTrue(pos1.message().contains("Set pos1"));
        assertTrue(pos2.success(), pos2.message());
        assertTrue(pos2.message().contains("2 blocks"));

        CommandResult replace = harness.commands.execute(
                harness.command(a, a, "replace", "minecraft:dirt", "minecraft:stone"));
        assertTrue(replace.success(), replace.message());
        assertTrue(replace.validation().valid());
        assertEquals(2, replace.preview().affectedCount());
        assertEquals("replace", replace.record().toolName());
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(a));
        assertTrue(harness.sessions.session(TestHarness.ACTOR).selection().isEmpty(),
                "selection must be cleared after a mutating execute");

        // The selection is dropped after each operation, so re-pick both corners.
        harness.commands.execute(harness.command(a, a, "pos1"));
        harness.commands.execute(harness.command(b, b, "pos2"));
        CommandResult fill = harness.commands.execute(harness.command(a, a, "fill", "minecraft:oak_planks"));
        assertFalse(fill.success());
        assertEquals("Insufficient blocks for operation", fill.message());

        harness.survival.give(TestHarness.ACTOR, "minecraft:oak_planks", 8);
        fill = harness.commands.execute(harness.command(a, a, "fill", "minecraft:oak_planks"));
        assertTrue(fill.success(), fill.message());
        assertEquals(2, fill.record().changes().size());
        assertTrue(harness.sessions.session(TestHarness.ACTOR).selection().isEmpty(),
                "selection must be cleared after a successful fill");

        harness.commands.execute(harness.command(a, a, "pos1"));
        harness.commands.execute(harness.command(b, b, "pos2"));
        CommandResult copy = harness.commands.execute(harness.command(a, a, "copy"));
        assertTrue(copy.success(), copy.message());
        assertTrue(copy.message().contains("Copied"));

        BlockPosition dest = harness.pos(8, 64, 0);
        harness.sessions.session(TestHarness.ACTOR).setPos1(dest);
        CommandResult paste = harness.commands.execute(harness.command(dest, dest, "paste"));
        assertTrue(paste.success(), paste.message());
        assertEquals(BlockState.of("minecraft:oak_planks"), harness.world.getBlock(dest));

        CommandResult save = harness.commands.execute(harness.command(a, a, "blueprint", "save", "hut"));
        assertTrue(save.success(), save.message());
        assertEquals("Saved blueprint 'hut'", save.message());
        CommandResult list = harness.commands.execute(harness.command(a, a, "blueprint", "list"));
        assertEquals("Blueprints: hut", list.message());
        CommandResult load = harness.commands.execute(harness.command(a, a, "blueprint", "load", "hut"));
        assertTrue(load.success());
        CommandResult delete = harness.commands.execute(harness.command(a, a, "blueprint", "delete", "hut"));
        assertTrue(delete.success());
        assertEquals("Deleted blueprint 'hut'", delete.message());
    }

    @Test
    void pasteWithoutClipboardIsRefused() {
        TestHarness harness = new TestHarness();
        CommandResult result = harness.commands.execute(
                harness.command(harness.pos(0, 64, 0), harness.pos(0, 64, 0), "paste"));
        assertFalse(result.success());
        assertEquals("Clipboard is empty", result.message());
    }
}
