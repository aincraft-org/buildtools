package dev.mintychochip.masonry.common.operation;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.operation.OperationRecord;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Per-actor undo/redo stacks. Recording a new mutation clears redo. Oldest records evict first.
 */
public final class OperationHistory {
    /** V1 history bound (32 operations per actor). */
    public static final int DEFAULT_SIZE = 32;

    private final int maxSize;
    private final Map<ActorId, ArrayDeque<OperationRecord>> undoStacks = new HashMap<>();
    private final Map<ActorId, ArrayDeque<OperationRecord>> redoStacks = new HashMap<>();

    /**
     * @param maxSize positive undo depth
     */
    public OperationHistory(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.maxSize = maxSize;
    }

    /**
     * Pushes {@code record} and discards that actor's redo stack.
     *
     * @param actor owner
     * @param record executed operation
     */
    public void record(ActorId actor, OperationRecord record) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(record, "record");
        ArrayDeque<OperationRecord> undo = undoStacks.computeIfAbsent(actor, ignored -> new ArrayDeque<>());
        undo.addLast(record);
        while (undo.size() > maxSize) {
            undo.removeFirst();
        }
        redoStacks.remove(actor);
    }

    /**
     * Pops the latest undo onto redo.
     *
     * @param actor owner
     * @return popped record, if any
     */
    public Optional<OperationRecord> undo(ActorId actor) {
        ArrayDeque<OperationRecord> undo = undoStacks.get(actor);
        if (undo == null || undo.isEmpty()) {
            return Optional.empty();
        }
        OperationRecord record = undo.removeLast();
        redoStacks.computeIfAbsent(actor, ignored -> new ArrayDeque<>()).addLast(record);
        return Optional.of(record);
    }

    /**
     * Pops the latest redo onto undo.
     *
     * @param actor owner
     * @return popped record, if any
     */
    public Optional<OperationRecord> redo(ActorId actor) {
        ArrayDeque<OperationRecord> redo = redoStacks.get(actor);
        if (redo == null || redo.isEmpty()) {
            return Optional.empty();
        }
        OperationRecord record = redo.removeLast();
        undoStacks.computeIfAbsent(actor, ignored -> new ArrayDeque<>()).addLast(record);
        return Optional.of(record);
    }

    /**
     * Drops undo and redo for {@code actor} (logout).
     *
     * @param actor owner
     */
    public void clear(ActorId actor) {
        undoStacks.remove(actor);
        redoStacks.remove(actor);
    }
}
