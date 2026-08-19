package dev.mintychochip.buildtools.api.operation;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record OperationRecord(
        UUID operationId,
        ActorId actorId,
        String toolName,
        List<BlockChange> changes,
        ResourceCost cost,
        ResourceCost harvest) {
    public OperationRecord {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(changes, "changes");
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(harvest, "harvest");
        if (toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must be present");
        }
        changes = List.copyOf(changes);
    }

    public OperationRecord(
            UUID operationId,
            ActorId actorId,
            String toolName,
            List<BlockChange> changes,
            ResourceCost cost) {
        this(operationId, actorId, toolName, changes, cost, ResourceCost.none());
    }
}
