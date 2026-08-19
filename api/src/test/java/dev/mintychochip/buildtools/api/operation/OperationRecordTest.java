package dev.mintychochip.buildtools.api.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Tests that {@link OperationRecord} copies the change list and rejects mutation. */
class OperationRecordTest {
    @Test
    void preservesOrderedBlockChangesAndRejectsMutation() {
        BlockChange first = new BlockChange(
                new BlockPosition("world", 0, 64, 0),
                BlockState.of("minecraft:dirt"),
                BlockState.of("minecraft:stone"));
        BlockChange second = new BlockChange(
                new BlockPosition("world", 1, 64, 0),
                BlockState.of("minecraft:grass_block"),
                BlockState.of("minecraft:air"));
        List<BlockChange> changes = new ArrayList<>();
        changes.add(first);
        changes.add(second);

        OperationRecord record = new OperationRecord(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                new ActorId(UUID.fromString("00000000-0000-0000-0000-0000000000aa")),
                "replace",
                changes,
                new ResourceCost(java.util.Map.of("minecraft:stone", 1)));

        changes.clear();
        assertEquals(List.of(first, second), record.changes());
        assertThrows(UnsupportedOperationException.class, () -> record.changes().add(first));
    }
}
