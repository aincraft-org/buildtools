package dev.mintychochip.masonry.common.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.command.CommandResult;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import dev.mintychochip.masonry.common.support.TestHarness;
import org.junit.jupiter.api.Test;

/** Tests the extension operation through the common command and tool lifecycle. */
class ExtendToolTest {
    @Test
    void extendPlacesLineFromStandingBlockAndRecordsUndoableChanges() {
        TestHarness harness = new TestHarness();
        BlockPosition feet = harness.pos(0, 65, 0);
        BlockPosition anchor = harness.pos(0, 64, 0);
        BlockState bricks = BlockState.of("minecraft:bricks");
        harness.world.put(anchor, bricks);
        harness.survival.give(TestHarness.ACTOR, bricks.itemKey(), 3);

        CommandResult result = harness.commands.execute(
                harness.command(feet, harness.pos(3, 64, 0), "extend", bricks.namespacedKey()));

        assertTrue(result.success(), result.message());
        assertEquals("extend", result.record().toolName());
        assertEquals(3, result.preview().affectedCount());
        assertEquals(bricks, harness.world.getBlock(harness.pos(1, 64, 0)));
        assertEquals(bricks, harness.world.getBlock(harness.pos(2, 64, 0)));
        assertEquals(bricks, harness.world.getBlock(harness.pos(3, 64, 0)));

        harness.commands.execute(harness.command(feet, null, "undo"));

        assertEquals(BlockState.AIR, harness.world.getBlock(harness.pos(1, 64, 0)));
        assertEquals(BlockState.AIR, harness.world.getBlock(harness.pos(2, 64, 0)));
        assertEquals(BlockState.AIR, harness.world.getBlock(harness.pos(3, 64, 0)));
    }

    @Test
    void solidObstacleRejectsTheWholeExtensionBeforeMutation() {
        TestHarness harness = new TestHarness();
        BlockPosition feet = harness.pos(0, 65, 0);
        BlockPosition anchor = harness.pos(0, 64, 0);
        BlockPosition obstacle = harness.pos(2, 64, 0);
        BlockState bricks = BlockState.of("minecraft:bricks");
        BlockState stone = BlockState.of("minecraft:stone");
        harness.world.put(anchor, bricks).put(obstacle, stone);
        harness.survival.give(TestHarness.ACTOR, bricks.itemKey(), 3);

        CommandResult result = harness.commands.execute(
                harness.command(feet, harness.pos(3, 64, 0), "extend", bricks.namespacedKey()));

        assertFalse(result.success());
        assertEquals("Extension is blocked", result.message());
        assertEquals(BlockState.AIR, harness.world.getBlock(harness.pos(1, 64, 0)));
        assertEquals(stone, harness.world.getBlock(obstacle));
        assertEquals(3, harness.survival.count(TestHarness.ACTOR, bricks.itemKey()));
    }

    @Test
    void extensionRespectsSelectionExtent() {
        TestHarness harness = new TestHarness(new dev.mintychochip.masonry.api.limits.OperationLimits(6, 2, 32_768));
        BlockPosition feet = harness.pos(0, 65, 0);
        BlockState bricks = BlockState.of("minecraft:bricks");
        harness.world.put(harness.pos(0, 64, 0), bricks);
        harness.survival.give(TestHarness.ACTOR, bricks.itemKey(), 3);

        CommandResult result = harness.commands.execute(
                harness.command(feet, harness.pos(3, 64, 0), "extend", bricks.namespacedKey()));

        assertFalse(result.success());
        assertEquals("Selection exceeds maximum extent", result.message());
    }

    @Test
    void existingTargetCellsAreNotChargedOrRecorded() {
        TestHarness harness = new TestHarness();
        BlockPosition feet = harness.pos(0, 65, 0);
        BlockState bricks = BlockState.of("minecraft:bricks");
        harness.world.put(harness.pos(0, 64, 0), bricks);
        harness.world.put(harness.pos(2, 64, 0), bricks);
        harness.survival.give(TestHarness.ACTOR, bricks.itemKey(), 2);

        CommandResult result = harness.commands.execute(
                harness.command(feet, harness.pos(3, 64, 0), "extend", bricks.namespacedKey()));

        assertTrue(result.success(), result.message());
        assertEquals(2, result.preview().affectedCount());
        assertEquals(0, harness.survival.count(TestHarness.ACTOR, bricks.itemKey()));
        assertEquals(2, result.record().changes().size());
    }

    @Test
    void extensionRequiresMatchingSupportBlock() {
        TestHarness harness = new TestHarness();
        BlockPosition feet = harness.pos(0, 65, 0);
        BlockState stone = BlockState.of("minecraft:stone");
        BlockState bricks = BlockState.of("minecraft:bricks");
        harness.world.put(harness.pos(0, 64, 0), stone);
        harness.survival.give(TestHarness.ACTOR, bricks.itemKey(), 1);

        CommandResult result = harness.commands.execute(
                harness.command(feet, harness.pos(1, 64, 0), "extend", bricks.namespacedKey()));

        assertFalse(result.success());
        assertEquals("Extension must start on a matching block", result.message());
    }
}
