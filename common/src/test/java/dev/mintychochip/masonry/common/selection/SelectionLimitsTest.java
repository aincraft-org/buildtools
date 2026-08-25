package dev.mintychochip.masonry.common.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.command.CommandResult;
import dev.mintychochip.masonry.api.limits.OperationLimits;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import dev.mintychochip.masonry.common.support.TestHarness;
import org.junit.jupiter.api.Test;

/**
 * Drives {@code /masonry pos1}/{@code pos2} and fill to prove the three limits are independent.
 */
class SelectionLimitsTest {
    @Test
    void twoPointCuboidVolumeAndBoundsMatchCorners() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(2, 64, -1);
        BlockPosition b = harness.pos(5, 66, 1);

        CommandResult pos1 = harness.commands.execute(harness.command(a, a, "pos1"));
        CommandResult pos2 = harness.commands.execute(harness.command(b, b, "pos2"));

        assertTrue(pos1.success(), pos1.message());
        assertTrue(pos2.success(), pos2.message());
        CuboidSelection selection = harness.sessions.session(TestHarness.ACTOR).selection().orElseThrow();
        assertEquals(4, selection.width());
        assertEquals(3, selection.height());
        assertEquals(3, selection.depth());
        assertEquals(36, selection.volume());
        assertEquals(a, selection.pos1());
        assertEquals(b, selection.pos2());
        assertEquals(1, harness.previews.selections().size());
        assertEquals(selection, harness.previews.selections().getFirst());
    }

    @Test
    void targetBeyondInteractionDistanceIsRejectedWhileValidSelectionMayExceedIt() {
        TestHarness harness = new TestHarness(new OperationLimits(6, 64, 32_768));
        BlockPosition origin = harness.pos(0, 64, 0);
        assertFalse(harness.commands.execute(harness.command(origin, harness.pos(7, 64, 0), "pos1")).success());

        assertTrue(harness.commands.execute(harness.command(origin, harness.pos(3, 64, 0), "pos1")).success());
        BlockPosition laterOrigin = harness.pos(0, 64, 50);
        assertTrue(harness.commands.execute(harness.command(laterOrigin, harness.pos(3, 64, 53), "pos2")).success());

        CuboidSelection selection = harness.sessions.session(TestHarness.ACTOR).selection().orElseThrow();
        assertEquals(54, selection.extent());
        assertTrue(selection.extent() > harness.limits.interactionDistance());
        assertTrue(selection.extent() <= harness.limits.selectionExtent());
    }

    @Test
    void operationOverMaxBlocksIsRejected() {
        TestHarness harness = new TestHarness(new OperationLimits(6, 64, 8));
        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(2, 64, 2);
        harness.world.fill(a, b, BlockState.of("minecraft:dirt"));
        harness.survival.give(TestHarness.ACTOR, "minecraft:stone", 64);
        assertTrue(harness.commands.execute(harness.command(a, a, "pos1")).success());
        assertTrue(harness.commands.execute(harness.command(b, b, "pos2")).success());

        CommandResult fill = harness.commands.execute(harness.command(a, a, "fill", "minecraft:stone"));
        assertFalse(fill.success(), fill.message());
        assertTrue(fill.message().toLowerCase().contains("maximum")
                || fill.validation().firstError().toLowerCase().contains("maximum"));
        assertEquals(BlockState.of("minecraft:dirt"), harness.world.getBlock(a));
    }
}
