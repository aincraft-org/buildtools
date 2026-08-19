package dev.mintychochip.buildtools.common.operation;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.operation.OperationRecord;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class OperationHistory {
    public static final int DEFAULT_SIZE = 32;

    private final int maxSize;
    private final Map<ActorId, ArrayDeque<OperationRecord>> undoStacks = new HashMap<>();
    private final Map<ActorId, ArrayDeque<OperationRecord>> redoStacks = new HashMap<>();

    public OperationHistory(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.maxSize = maxSize;
    }

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

    public Optional<OperationRecord> undo(ActorId actor) {
        ArrayDeque<OperationRecord> undo = undoStacks.get(actor);
        if (undo == null || undo.isEmpty()) {
            return Optional.empty();
        }
        OperationRecord record = undo.removeLast();
        redoStacks.computeIfAbsent(actor, ignored -> new ArrayDeque<>()).addLast(record);
        return Optional.of(record);
    }

    public Optional<OperationRecord> redo(ActorId actor) {
        ArrayDeque<OperationRecord> redo = redoStacks.get(actor);
        if (redo == null || redo.isEmpty()) {
            return Optional.empty();
        }
        OperationRecord record = redo.removeLast();
        undoStacks.computeIfAbsent(actor, ignored -> new ArrayDeque<>()).addLast(record);
        return Optional.of(record);
    }

    public void clear(ActorId actor) {
        undoStacks.remove(actor);
        redoStacks.remove(actor);
    }
}
