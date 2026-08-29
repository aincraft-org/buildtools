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
 * Tests the extension operation through the common command and tool lifecycle. The
 * extend command is aim-based: the aimed block is the anchor, the extension occupies the
 * cells one past it (in the direction away from the player), shaped length x width. The
 * tool accepts any flat horizontal rectangle.
 */
class ExtendToolTest {

    @Test
    void extendPlacesLinePastAimedBlockAndRecordsUndoableChanges() {
        TestHarness harness = new TestHarness();
        BlockPosition feet = harness.pos(0, 65, 0);
        BlockPosition aimed = harness.pos(3, 64, 0);
        BlockState bricks = BlockState.of("minecraft:bricks");
        harness.survival.give(TestHarness.ACTOR, bricks.itemKey(), 3);

        CommandResult result = harness.commands.execute(
                harness.command(feet, aimed, "extend", bricks.namespacedKey()));

        assertTrue(result.success(), result.message());
        assertEquals("extend", result.record().toolName());
        assertEquals(3, result.preview().affectedCount());
        // The aimed block is the anchor; the line extends one cell past it, away from feet.
        assertEquals(bricks, harness.world.getBlock(harness.pos(4, 64, 0)));
        assertEquals(bricks, harness.world.getBlock(harness.pos(5, 64, 0)));
        assertEquals(bricks, harness.world.getBlock(harness.pos(6, 64, 0)));

        harness.commands.execute(harness.command(feet, null, "undo"));

        assertEquals(BlockState.AIR, harness.world.getBlock(harness.pos(4, 64, 0)));
        assertEquals(BlockState.AIR, harness.world.getBlock(harness.pos(5, 64, 0)));
        assertEquals(BlockState.AIR, harness.world.getBlock(harness.pos(6, 64, 0)));
    }

    @Test
    void extendPlacesHorizontalPlaneAndRecordsUndoableChanges() {
        TestHarness harness = new TestHarness();
        BlockPosition feet = harness.pos(0, 65, 0);
        BlockPosition aimed = harness.pos(2, 64, 1);
        BlockState bricks = BlockState.of("minecraft:bricks");
        harness.survival.give(TestHarness.ACTOR, bricks.itemKey(), 9);

        // A 3x3 plane one cell past the aimed block, dominant axis +X (|2|>|1|).
        CommandResult result = harness.commands.execute(
                harness.command(feet, aimed, "extend", bricks.namespacedKey(), "3", "3"));

        assertTrue(result.success(), result.message());
        assertEquals("extend", result.record().toolName());
        assertEquals(9, result.preview().affectedCount());
        for (int x = 3; x <= 5; x++) {
            for (int z = 0; z <= 2; z++) {
                assertEquals(bricks, harness.world.getBlock(harness.pos(x, 64, z)), "cell " + x + "," + z);
            }
        }

        harness.commands.execute(harness.command(feet, null, "undo"));

        for (int x = 3; x <= 5; x++) {
            for (int z = 0; z <= 2; z++) {
                assertEquals(BlockState.AIR, harness.world.getBlock(harness.pos(x, 64, z)), "cell " + x + "," + z);
            }
        }
    }

    @Test
    void solidObstacleRejectsTheWholeExtensionBeforeMutation() {
        TestHarness harness = new TestHarness();
        BlockPosition feet = harness.pos(0, 65, 0);
        BlockPosition aimed = harness.pos(3, 64, 0);
        BlockPosition obstacle = harness.pos(5, 64, 0);
        BlockState bricks = BlockState.of("minecraft:bricks");
        BlockState stone = BlockState.of("minecraft:stone");
        harness.world.put(obstacle, stone);
        harness.survival.give(TestHarness.ACTOR, bricks.itemKey(), 3);

        CommandResult result = harness.commands.execute(
                harness.command(feet, aimed, "extend", bricks.namespacedKey()));

        assertFalse(result.success());
        assertEquals("Extension is blocked", result.message());
        assertEquals(BlockState.AIR, harness.world.getBlock(harness.pos(4, 64, 0)));
        assertEquals(stone, harness.world.getBlock(obstacle));
        assertEquals(3, harness.survival.count(TestHarness.ACTOR, bricks.itemKey()));
    }

    @Test
    void extensionRespectsSelectionExtent() {
        TestHarness harness = new TestHarness(new dev.mintychochip.masonry.api.limits.OperationLimits(6, 2, 32_768));
        BlockPosition feet = harness.pos(0, 65, 0);
        BlockState bricks = BlockState.of("minecraft:bricks");
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
        harness.world.put(harness.pos(4, 64, 0), bricks);
        harness.world.put(harness.pos(5, 64, 0), bricks);
        harness.survival.give(TestHarness.ACTOR, bricks.itemKey(), 2);

        CommandResult result = harness.commands.execute(
                harness.command(feet, harness.pos(3, 64, 0), "extend", bricks.namespacedKey()));

        assertTrue(result.success(), result.message());
        // The aimed block (3,64,0) is the anchor; extension is 4..6. Cells 4 and 5 already
        // have bricks, only 6 is new → 1 affected, 1 charged, 1 record.
        assertEquals(1, result.preview().affectedCount());
        assertEquals(1, harness.survival.count(TestHarness.ACTOR, bricks.itemKey()));
        assertEquals(1, result.record().changes().size());
    }

    @Test
    void extendRejectsBlockedPlaneCell() {
        TestHarness harness = new TestHarness();
        BlockPosition feet = harness.pos(0, 65, 0);
        BlockPosition aimed = harness.pos(2, 64, 1);
        BlockPosition obstacle = harness.pos(4, 64, 1);
        BlockState bricks = BlockState.of("minecraft:bricks");
        BlockState stone = BlockState.of("minecraft:stone");
        harness.world.put(obstacle, stone);
        harness.survival.give(TestHarness.ACTOR, bricks.itemKey(), 9);

        CommandResult result = harness.commands.execute(
                harness.command(feet, aimed, "extend", bricks.namespacedKey(), "3", "3"));

        assertFalse(result.success());
        assertEquals("Extension is blocked", result.message());
        assertEquals(BlockState.AIR, harness.world.getBlock(harness.pos(3, 64, 0)));
    }
}