package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import dev.mintychochip.buildtools.api.operation.BlockChange;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.tool.ToolRequest;
import dev.mintychochip.buildtools.api.tool.ValidationResult;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
