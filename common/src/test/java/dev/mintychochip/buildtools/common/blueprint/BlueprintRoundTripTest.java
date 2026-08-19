package dev.mintychochip.buildtools.common.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.command.CommandResult;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import dev.mintychochip.buildtools.common.support.TestHarness;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BlueprintRoundTripTest {
    @Test
    void saveLoadPasteRoundTripsBlockStatesAndDeleteRemovesFromList() {
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(1, 65, 0);
        BlockState stairs = new BlockState("minecraft:oak_stairs", Map.of("facing", "west", "half", "top"));
        harness.world.put(a, BlockState.of("minecraft:diamond_block"));
        harness.world.put(harness.pos(1, 64, 0), stairs);
        harness.world.put(harness.pos(0, 65, 0), BlockState.of("minecraft:glass"));
        harness.world.put(b, BlockState.of("minecraft:air"));
        select(harness, a, b);

        CommandResult save = harness.commands.execute(harness.command(a, a, "blueprint", "save", "keep"));
        assertTrue(save.success(), save.message());
        CommandResult list = harness.commands.execute(harness.command(a, a, "blueprint", "list"));
        assertTrue(list.message().contains("keep"), list.message());

        harness.sessions.session(TestHarness.ACTOR).setClipboard(null);
        CommandResult load = harness.commands.execute(harness.command(a, a, "blueprint", "load", "keep"));
        assertTrue(load.success(), load.message());

        BlockPosition dest = harness.pos(20, 70, 20);
        harness.sessions.session(TestHarness.ACTOR).setPos1(dest);
        harness.survival.give(TestHarness.ACTOR, "minecraft:diamond_block", 8);
        harness.survival.give(TestHarness.ACTOR, "minecraft:oak_stairs", 8);
        harness.survival.give(TestHarness.ACTOR, "minecraft:glass", 8);

        CommandResult paste = harness.commands.execute(harness.command(dest, dest, "paste"));
        assertTrue(paste.success(), paste.message());
        assertEquals(BlockState.of("minecraft:diamond_block"), harness.world.getBlock(dest));
        assertEquals(stairs, harness.world.getBlock(dest.offset(1, 0, 0)));
        assertEquals(BlockState.of("minecraft:glass"), harness.world.getBlock(dest.offset(0, 1, 0)));

        CommandResult delete = harness.commands.execute(harness.command(a, a, "blueprint", "delete", "keep"));
        assertTrue(delete.success(), delete.message());
        CommandResult listed = harness.commands.execute(harness.command(a, a, "blueprint", "list"));
        assertEquals("No blueprints", listed.message());
        assertFalse(harness.commands.execute(harness.command(a, a, "blueprint", "load", "keep")).success());
    }

    @Test
    void fileStoreSchematicPreservesBlockState(@TempDir Path temp) {
        FileBlueprintStore store = new FileBlueprintStore(temp);
        TestHarness harness = new TestHarness();
        BlockPosition a = harness.pos(0, 10, 0);
        BlockPosition b = harness.pos(1, 11, 0);
        BlockState stairs = new BlockState("minecraft:brick_stairs", Map.of("facing", "south", "half", "bottom"));
        harness.world.put(a, stairs);
        harness.world.put(harness.pos(1, 10, 0), BlockState.of("minecraft:emerald_block"));
        harness.world.put(harness.pos(0, 11, 0), BlockState.AIR);
        harness.world.put(b, BlockState.of("minecraft:gold_block"));
        select(harness, a, b);
        assertTrue(harness.commands.execute(harness.command(a, a, "copy")).success());

        store.save(TestHarness.ACTOR, "house", harness.sessions.clipboard(TestHarness.ACTOR).orElseThrow());
        harness.sessions.session(TestHarness.ACTOR).setClipboard(null);
        var loaded = store.load(TestHarness.ACTOR, "house").orElseThrow();
        assertEquals(4, loaded.size());
        assertEquals(stairs, loaded.blocks().values().stream()
                .filter(state -> state.namespacedKey().equals("minecraft:brick_stairs"))
                .findFirst()
                .orElseThrow());
        assertTrue(
                loaded.blocks().values().stream().anyMatch(BlockState::isAir),
                "decoded schematic must keep air cells so paste can clear holes");

        harness.sessions.setClipboard(TestHarness.ACTOR, loaded);
        BlockPosition dest = harness.pos(30, 20, 30);
        harness.world.fill(dest, dest.offset(1, 1, 0), BlockState.of("minecraft:dirt"));
        harness.sessions.session(TestHarness.ACTOR).setPos1(dest);
        harness.survival.give(TestHarness.ACTOR, "minecraft:brick_stairs", 4);
        harness.survival.give(TestHarness.ACTOR, "minecraft:emerald_block", 4);
        harness.survival.give(TestHarness.ACTOR, "minecraft:gold_block", 4);

        CommandResult pasted = harness.commands.execute(harness.command(dest, dest, "paste"));
        assertTrue(pasted.success(), pasted.message());
        assertEquals(stairs, harness.world.getBlock(dest));
        assertEquals(BlockState.of("minecraft:emerald_block"), harness.world.getBlock(dest.offset(1, 0, 0)));
        assertEquals(BlockState.AIR, harness.world.getBlock(dest.offset(0, 1, 0)));
        assertEquals(BlockState.of("minecraft:gold_block"), harness.world.getBlock(dest.offset(1, 1, 0)));

        assertEquals(1, store.list(TestHarness.ACTOR).size());
        assertTrue(store.delete(TestHarness.ACTOR, "house"));
        assertTrue(store.list(TestHarness.ACTOR).isEmpty());
    }

    private static void select(TestHarness harness, BlockPosition a, BlockPosition b) {
        assertTrue(harness.commands.execute(harness.command(a, a, "pos1")).success());
        assertTrue(harness.commands.execute(harness.command(b, b, "pos2")).success());
    }
}
