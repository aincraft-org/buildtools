package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.operation.BlockChange;
import dev.mintychochip.buildtools.api.operation.OperationRecord;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.tool.Tool;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import dev.mintychochip.buildtools.api.tool.ToolRequest;
import dev.mintychochip.buildtools.api.tool.ValidationResult;
import dev.mintychochip.buildtools.common.operation.ResourceCosts;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base for fill, replace, and paste. Preview and execute share {@link #plan}; a cancelled
 * {@code setBlock} rolls the world back and returns an empty change list.
 */
abstract class MutatingTool implements Tool {
    @Override
    public final ToolPreview preview(ToolRequest request, WorldAccess world) {
        BlockPlan plan = plan(request, world);
        return new ToolPreview(plan.region(), plan.affectedPositions(), ResourceCosts.placementCost(plan.changes()));
    }

    @Override
    public ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        if (request.selection() == null) {
            return ValidationResult.invalid("A valid selection is required");
        }
        return ValidationResult.passed();
    }

    @Override
    public final OperationRecord execute(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        BlockPlan plan = plan(request, world);
        List<BlockChange> applied = new ArrayList<>();
        for (BlockChange change : plan.changes()) {
            if (!world.setBlock(request.actorId(), change.position(), change.after())) {
                for (int i = applied.size() - 1; i >= 0; i--) {
                    BlockChange undo = applied.get(i);
                    world.setBlock(request.actorId(), undo.position(), undo.before());
                }
                applied.clear();
                break;
            }
            applied.add(change);
        }
        ResourceCost cost = ResourceCosts.placementCost(applied);
        ResourceCost harvest = ResourceCosts.harvest(applied);
        return new OperationRecord(UUID.randomUUID(), request.actorId(), name(), applied, cost, harvest);
    }

    @Override
    public final void undo(OperationRecord record, WorldAccess world, SurvivalTransaction survival) {
        List<BlockChange> changes = record.changes();
        for (int i = changes.size() - 1; i >= 0; i--) {
            BlockChange change = changes.get(i);
            world.setBlock(record.actorId(), change.position(), change.before());
        }
    }

    /**
     * Builds the exact diffs this invocation would apply.
     *
     * @param request request
     * @param world world
     * @return plan whose positions become the preview affected set
     */
    protected abstract BlockPlan plan(ToolRequest request, WorldAccess world);

    /**
     * @param request request
     * @return acting player
     */
    protected static ActorId actor(ToolRequest request) {
        return request.actorId();
    }
}
