package dev.mintychochip.buildtools.common.survival;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.command.CommandResult;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import dev.mintychochip.buildtools.common.support.TestHarness;
import org.junit.jupiter.api.Test;

/**
 * Drives charge, refund, permission, bypass, and cancelled {@code setBlock} through
 * {@code /bt fill} and undo.
 */
class SurvivalEconomicsTest {
    @Test
    void undoRestoresBlocksAndRefundsNoMoreThanCharged() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(1, 64, 0);
        harness.world.put(a, BlockState.of("minecraft:dirt"));
        harness.world.put(b, BlockState.of("minecraft:dirt"));
        harness.survival.give(TestHarness.ACTOR, "minecraft:stone", 10);
        select(harness, a, b);

        CommandResult fill = harness.commands.execute(harness.command(a, a, "fill", "minecraft:stone"));
        assertTrue(fill.success(), fill.message());
        assertEquals(8, harness.survival.count(TestHarness.ACTOR, "minecraft:stone"));
        assertEquals(1, harness.survival.charges().size());
        assertEquals(2, harness.survival.charges().getFirst().countOf("minecraft:stone"));

        CommandResult undo = harness.commands.execute(harness.command(a, a, "undo"));
        assertTrue(undo.success(), undo.message());
        assertEquals(BlockState.of("minecraft:dirt"), harness.world.getBlock(a));
        assertEquals(BlockState.of("minecraft:dirt"), harness.world.getBlock(b));
        int refundedStone = harness.survival.refunds().stream()
                .mapToInt(cost -> cost.countOf("minecraft:stone"))
                .sum();
        int chargedStone = harness.survival.charges().stream()
                .mapToInt(cost -> cost.countOf("minecraft:stone"))
                .sum();
        assertTrue(refundedStone <= chargedStone);
        assertEquals(10, harness.survival.count(TestHarness.ACTOR, "minecraft:stone"));

        CommandResult secondUndo = harness.commands.execute(harness.command(a, a, "undo"));
        assertFalse(secondUndo.success());
        assertEquals("Nothing to undo", secondUndo.message());
        assertEquals(10, harness.survival.count(TestHarness.ACTOR, "minecraft:stone"));
    }

    @Test
    void redoReappliesMutation() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 80, 0);
        harness.world.put(a, BlockState.of("minecraft:dirt"));
        harness.survival.give(TestHarness.ACTOR, "minecraft:stone", 4);
        select(harness, a, a);

        assertTrue(harness.commands.execute(harness.command(a, a, "fill", "minecraft:stone")).success());
        assertTrue(harness.commands.execute(harness.command(a, a, "undo")).success());
        CommandResult redo = harness.commands.execute(harness.command(a, a, "redo"));
        assertTrue(redo.success(), redo.message());
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(a));
    }

    @Test
    void missingPermissionBlocksExecuteAndMutatesNothing() {
        TestHarness harness = new TestHarness();
        harness.permissions.denyAll();
        BlockPosition a = harness.pos(0, 64, 0);
        harness.world.put(a, BlockState.of("minecraft:dirt"));
        harness.survival.give(TestHarness.ACTOR, "minecraft:stone", 8);
        select(harness, a, a);

        CommandResult result = harness.commands.execute(harness.command(a, a, "fill", "minecraft:stone"));
        assertFalse(result.success());
        assertTrue(result.message().contains("Missing permission buildtools.tool.fill"));
        assertEquals(BlockState.of("minecraft:dirt"), harness.world.getBlock(a));
        assertTrue(harness.survival.charges().isEmpty());
    }

    @Test
    void missingItemsBlockExecuteAndMutateNothing() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        harness.world.put(a, BlockState.of("minecraft:dirt"));
        select(harness, a, a);

        CommandResult result = harness.commands.execute(harness.command(a, a, "fill", "minecraft:stone"));
        assertFalse(result.success());
        assertEquals("Insufficient blocks for operation", result.message());
        assertEquals(BlockState.of("minecraft:dirt"), harness.world.getBlock(a));
    }

    @Test
    void creativeBypassSkipsCharge() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        harness.world.put(a, BlockState.of("minecraft:dirt"));
        harness.survival.creative(TestHarness.ACTOR);
        select(harness, a, a);

        CommandResult result = harness.commands.execute(harness.command(a, a, "fill", "minecraft:stone"));
        assertTrue(result.success(), result.message());
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(a));
        assertTrue(harness.survival.charges().isEmpty());
        assertEquals(0, harness.survival.count(TestHarness.ACTOR, "minecraft:stone"));
    }

    @Test
    void survivalBypassPermissionSkipsCharge() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(1, 64, 1);
        harness.world.put(a, BlockState.of("minecraft:dirt"));
        harness.survival.bypass(TestHarness.ACTOR);
        select(harness, a, a);

        CommandResult result = harness.commands.execute(harness.command(a, a, "fill", "minecraft:stone"));
        assertTrue(result.success(), result.message());
        assertTrue(harness.survival.charges().isEmpty());
    }

    @Test
    void cancelledMutationRefundsChargeAndLeavesWorldUnchanged() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        harness.world.put(a, BlockState.of("minecraft:dirt"));
        harness.world.cancel(a);
        harness.survival.give(TestHarness.ACTOR, "minecraft:stone", 4);
        select(harness, a, a);

        CommandResult result = harness.commands.execute(harness.command(a, a, "fill", "minecraft:stone"));

        assertFalse(result.success(), result.message());
        assertEquals(BlockState.of("minecraft:dirt"), harness.world.getBlock(a));
        assertEquals(4, harness.survival.count(TestHarness.ACTOR, "minecraft:stone"));
        int charged = harness.survival.charges().stream()
                .mapToInt(cost -> cost.countOf("minecraft:stone"))
                .sum();
        int refunded = harness.survival.refunds().stream()
                .mapToInt(cost -> cost.countOf("minecraft:stone"))
                .sum();
        assertEquals(charged, refunded);
        CommandResult undo = harness.commands.execute(harness.command(a, a, "undo"));
        assertFalse(undo.success());
        assertEquals("Nothing to undo", undo.message());
    }

    @Test
    void fullInventoryRefundDropsLeftovers() {
        TestHarness harness = new TestHarness();
        harness.survival.withCapacity(0);
        BlockPosition a = harness.pos(0, 64, 0);
        harness.world.put(a, BlockState.AIR);
        harness.survival.give(TestHarness.ACTOR, "minecraft:stone", 1);
        select(harness, a, a);

        assertTrue(harness.commands.execute(harness.command(a, a, "fill", "minecraft:stone")).success());
        assertEquals(0, harness.survival.count(TestHarness.ACTOR, "minecraft:stone"));
        assertTrue(harness.commands.execute(harness.command(a, a, "undo")).success());
        int refunded = harness.survival.refunds().stream()
                .mapToInt(cost -> cost.countOf("minecraft:stone"))
                .sum();
        int dropped = harness.survival.drops().stream()
                .mapToInt(cost -> cost.countOf("minecraft:stone"))
                .sum();
        assertEquals(1, refunded);
        assertEquals(1, dropped);
        assertEquals(0, harness.survival.count(TestHarness.ACTOR, "minecraft:stone"));
    }

    private static void select(TestHarness harness, BlockPosition a, BlockPosition b) {
        assertTrue(harness.commands.execute(harness.command(a, a, "pos1")).success());
        assertTrue(harness.commands.execute(harness.command(b, b, "pos2")).success());
    }
}
