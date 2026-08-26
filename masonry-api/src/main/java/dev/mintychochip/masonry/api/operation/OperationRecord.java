package dev.mintychochip.masonry.api.operation;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.cost.ResourceCost;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable history entry for one executed tool: diffs plus what was charged and harvested.
 *
 * @param operationId unique id for this execution
 * @param actorId player who ran the tool
 * @param toolName registered tool name ({@code replace}, {@code fill}, {@code paste}, …)
 * @param changes ordered block diffs actually applied
 * @param cost items charged for placed blocks
 * @param harvest items given for removed blocks
 */
public record OperationRecord(
        UUID operationId,
        ActorId actorId,
        String toolName,
        List<BlockChange> changes,
        ResourceCost cost,
        ResourceCost harvest) {
    /**
     * @throws NullPointerException if a required component is {@code null}
     * @throws IllegalArgumentException if {@code toolName} is blank
     */
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

    /**
     * Convenience constructor with an empty harvest.
     *
     * @param operationId unique id
     * @param actorId actor
     * @param toolName tool name
     * @param changes diffs
     * @param cost charged items
     */
    public OperationRecord(
            UUID operationId,
            ActorId actorId,
            String toolName,
            List<BlockChange> changes,
            ResourceCost cost) {
        this(operationId, actorId, toolName, changes, cost, ResourceCost.none());
    }
}
