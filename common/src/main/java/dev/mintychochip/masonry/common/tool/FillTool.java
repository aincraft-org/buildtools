package dev.mintychochip.masonry.common.tool;

import dev.mintychochip.masonry.api.operation.BlockChange;
import dev.mintychochip.masonry.api.service.SurvivalTransaction;
import dev.mintychochip.masonry.api.service.WorldAccess;
import dev.mintychochip.masonry.api.tool.ToolRequest;
import dev.mintychochip.masonry.api.tool.ValidationResult;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes one target state across the selection. Preview lists every cell that is not already
 * that state.
 */
public final class FillTool extends MutatingTool {
    @Override
    public String name() {
        return "fill";
    }

    @Override
    public ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        ValidationResult base = super.validate(request, world, survival);
        if (!base.valid()) {
            return base;
        }
        if (parseTarget(request) == null) {
            return ValidationResult.invalid("Usage: /masonry fill <block>");
        }
        return ValidationResult.passed();
    }

    @Override
    protected BlockPlan plan(ToolRequest request, WorldAccess world) {
        BlockState target = parseTarget(request);
        if (target == null || request.selection() == null) {
            return new BlockPlan(request.selection(), List.of());
        }
        List<BlockChange> changes = new ArrayList<>();
        for (BlockPosition position : request.selection().positions()) {
            if (request.isExcluded(position)) {
                continue;
            }
            BlockState before = world.getBlock(position);
            if (!before.equals(target)) {
                changes.add(new BlockChange(position, before, target));
            }
        }
        return new BlockPlan(request.selection(), List.copyOf(changes));
    }

    private static BlockState parseTarget(ToolRequest request) {
        String raw = request.argument("block");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return BlockStates.parse(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
