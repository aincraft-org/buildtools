package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.operation.BlockChange;
import dev.mintychochip.buildtools.api.operation.OperationRecord;
import dev.mintychochip.buildtools.api.service.PermissionService;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.tool.Tool;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import dev.mintychochip.buildtools.api.tool.ToolRequest;
import dev.mintychochip.buildtools.api.tool.ValidationResult;
import dev.mintychochip.buildtools.common.operation.OperationGuard;
import dev.mintychochip.buildtools.common.operation.OperationHistory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ToolExecutor {
    private final ToolRegistry registry;
    private final OperationHistory history;
    private final OperationGuard guard;
    private final PermissionService permissions;

    public ToolExecutor(
            ToolRegistry registry,
            OperationHistory history,
            OperationGuard guard,
            PermissionService permissions) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.history = Objects.requireNonNull(history, "history");
        this.guard = Objects.requireNonNull(guard, "guard");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
    }

    public ToolPreview preview(ToolRequest request, WorldAccess world) {
        return registry.require(request.toolName()).preview(request, world);
    }

    public ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        Objects.requireNonNull(request, "request");
        Optional<Tool> tool = registry.find(request.toolName());
        if (tool.isEmpty()) {
            return ValidationResult.invalid("Unknown tool: " + request.toolName());
        }
        String node = "buildtools.tool." + request.toolName();
        if (!permissions.has(request.actorId(), node)) {
            return ValidationResult.invalid("Missing permission " + node);
        }
        if (request.selection() != null) {
            ValidationResult selection = guard.validateSelection(request.selection());
            if (!selection.valid()) {
                return selection;
            }
        }
        ToolPreview preview = tool.get().preview(request, world);
        ValidationResult previewLimit = guard.validatePreview(preview);
        if (!previewLimit.valid()) {
            return previewLimit;
        }
        if (!survival.bypassesCost(request.actorId())
                && !survival.canAfford(request.actorId(), preview.estimatedCost())) {
            return ValidationResult.invalid("Insufficient blocks for operation");
        }
        return tool.get().validate(request, world, survival);
    }

    public Optional<OperationRecord> execute(
            ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        ValidationResult validation = validate(request, world, survival);
        if (!validation.valid()) {
            return Optional.empty();
        }
        Tool tool = registry.require(request.toolName());
        ToolPreview preview = tool.preview(request, world);
        boolean bypass = survival.bypassesCost(request.actorId());
        if (!bypass) {
            survival.charge(request.actorId(), preview.estimatedCost());
        }
        OperationRecord record = tool.execute(request, world, survival);
        List<dev.mintychochip.buildtools.api.world.BlockPosition> executed = record.changes().stream()
                .map(BlockChange::position)
                .toList();
        if (!record.changes().isEmpty() && !preview.affectedPositions().equals(executed)) {
            rollback(request.actorId(), record, world, survival, bypass, preview);
            return Optional.empty();
        }
        if (record.changes().isEmpty()) {
            if (!preview.affectedPositions().isEmpty()) {
                if (!bypass) {
                    survival.refund(request.actorId(), preview.estimatedCost());
                }
                return Optional.empty();
            }
            return Optional.of(record);
        }
        if (!bypass && !record.harvest().isEmpty()) {
            survival.refund(request.actorId(), record.harvest());
        }
        history.record(request.actorId(), record);
        return Optional.of(record);
    }

    public Optional<OperationRecord> undo(ActorId actor, WorldAccess world, SurvivalTransaction survival) {
        Optional<OperationRecord> record = history.undo(actor);
        record.ifPresent(value -> applyUndo(actor, value, world, survival));
        return record;
    }

    public Optional<OperationRecord> redo(ActorId actor, WorldAccess world, SurvivalTransaction survival) {
        Optional<OperationRecord> record = history.redo(actor);
        if (record.isEmpty()) {
            return Optional.empty();
        }
        OperationRecord value = record.get();
        boolean bypass = survival.bypassesCost(actor);
        if (!bypass && !survival.canAfford(actor, value.cost())) {
            history.undo(actor);
            return Optional.empty();
        }
        if (!bypass) {
            survival.charge(actor, value.cost());
        }
        List<BlockChange> applied = new ArrayList<>();
        for (BlockChange change : value.changes()) {
            if (!world.setBlock(actor, change.position(), change.after())) {
                for (int i = applied.size() - 1; i >= 0; i--) {
                    BlockChange undone = applied.get(i);
                    world.setBlock(actor, undone.position(), undone.before());
                }
                if (!bypass) {
                    survival.refund(actor, value.cost());
                }
                history.undo(actor);
                return Optional.empty();
            }
            applied.add(change);
        }
        if (!bypass && !value.harvest().isEmpty()) {
            survival.refund(actor, value.harvest());
        }
        return Optional.of(value);
    }

    private void applyUndo(ActorId actor, OperationRecord value, WorldAccess world, SurvivalTransaction survival) {
        registry.require(value.toolName()).undo(value, world, survival);
        if (survival.bypassesCost(actor)) {
            return;
        }
        if (!value.harvest().isEmpty()) {
            if (survival.canAfford(actor, value.harvest())) {
                survival.charge(actor, value.harvest());
            }
        }
        survival.refund(actor, value.cost());
    }

    private void rollback(
            ActorId actor,
            OperationRecord record,
            WorldAccess world,
            SurvivalTransaction survival,
            boolean bypass,
            ToolPreview preview) {
        for (BlockChange change : record.changes()) {
            world.setBlock(actor, change.position(), change.before());
        }
        if (!bypass) {
            survival.refund(actor, preview.estimatedCost());
        }
    }
}
