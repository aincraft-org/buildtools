package dev.mintychochip.buildtools.common.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.operation.OperationRecord;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperationHistoryTest {
    private static final ActorId ACTOR = new ActorId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));

    @Test
    void undoThenRedoRestoresRecordAndNewMutationClearsRedo() {
        OperationHistory history = new OperationHistory(8);
        OperationRecord first = record("one");
        OperationRecord second = record("two");

        history.record(ACTOR, first);
        history.record(ACTOR, second);

        assertEquals(second, history.undo(ACTOR).orElseThrow());
        assertEquals(second, history.redo(ACTOR).orElseThrow());
        assertEquals(second, history.undo(ACTOR).orElseThrow());

        history.record(ACTOR, record("three"));
        assertTrue(history.redo(ACTOR).isEmpty());
    }

    @Test
    void evictsOldestRecordsWhenMaxSizeExceeded() {
        OperationHistory history = new OperationHistory(2);
        OperationRecord first = record("one");
        OperationRecord second = record("two");
        OperationRecord third = record("three");

        history.record(ACTOR, first);
        history.record(ACTOR, second);
        history.record(ACTOR, third);

        assertEquals(third, history.undo(ACTOR).orElseThrow());
        assertEquals(second, history.undo(ACTOR).orElseThrow());
        assertTrue(history.undo(ACTOR).isEmpty());
    }

    private static OperationRecord record(String toolName) {
        return new OperationRecord(UUID.randomUUID(), ACTOR, toolName, List.of(), ResourceCost.none());
    }
}
