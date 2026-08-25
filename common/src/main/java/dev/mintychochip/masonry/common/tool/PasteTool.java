package dev.mintychochip.masonry.common.tool;

import dev.mintychochip.masonry.api.clipboard.Clipboard;
import dev.mintychochip.masonry.api.operation.BlockChange;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.service.SurvivalTransaction;
import dev.mintychochip.masonry.api.service.WorldAccess;
import dev.mintychochip.masonry.api.tool.ToolRequest;
import dev.mintychochip.masonry.api.tool.ValidationResult;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pastes the request clipboard at the selection origin (pos1). Air cells clear destination
 * blocks. Placement cost is charged for non-air after states.
 */
public final class PasteTool extends MutatingTool {
    @Override
    public String name() {
        return "paste";
    }

    @Override
    public ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        if (request.clipboard() == null || request.clipboard().isEmpty()) {
            return ValidationResult.invalid("Clipboard is empty");
        }
        if (request.selection() == null) {
            return ValidationResult.invalid("A paste origin is required");
        }
        return ValidationResult.passed();
    }

    @Override
    protected BlockPlan plan(ToolRequest request, WorldAccess world) {
        Clipboard clipboard = request.clipboard();
        BlockPosition origin = request.selection().pos1();
        if (clipboard == null || clipboard.isEmpty()) {
            return new BlockPlan(request.selection(), List.of());
        }
        Map<BlockPosition, BlockState> placed = clipboard.placedAt(origin);
        List<BlockChange> changes = new ArrayList<>();
        BlockPosition min = origin;
        BlockPosition max = origin;
        for (Map.Entry<BlockPosition, BlockState> entry : placed.entrySet()) {
            BlockPosition position = entry.getKey();
            min = new BlockPosition(
                    origin.worldId(),
                    Math.min(min.x(), position.x()),
                    Math.min(min.y(), position.y()),
                    Math.min(min.z(), position.z()));
            max = new BlockPosition(
                    origin.worldId(),
                    Math.max(max.x(), position.x()),
                    Math.max(max.y(), position.y()),
                    Math.max(max.z(), position.z()));
            BlockState before = world.getBlock(position);
            BlockState after = entry.getValue();
            if (!before.equals(after)) {
                changes.add(new BlockChange(position, before, after));
            }
        }
        return new BlockPlan(new CuboidSelection(min, max), List.copyOf(changes));
    }
}
