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

/** Places a horizontal plane (length by width) into replaceable cells. */
public final class ExtendTool extends MutatingTool {
    @Override
    public String name() {
        return "extend";
    }

    @Override
    public ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        ValidationResult base = super.validate(request, world, survival);
        if (!base.valid()) {
            return base;
        }
        BlockState target = parseTarget(request);
        if (target == null || target.isAir()) {
            return ValidationResult.invalid("Usage: /masonry extend <block>");
        }
        if (!isHorizontalPlane(request.selection())) {
            return ValidationResult.invalid("Extension must be a horizontal plane");
        }
        for (BlockPosition position : request.selection().positions()) {
            if (request.isExcluded(position)) {
                continue;
            }
            BlockState before = world.getBlock(position);
            if (!before.equals(target) && !world.isReplaceable(position)) {
                return ValidationResult.invalid("Extension is blocked");
            }
            if (!world.isLoaded(position)) {
                return ValidationResult.invalid("Extension enters an unloaded area");
            }
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
            if (!before.equals(target) && world.isReplaceable(position)) {
                changes.add(new BlockChange(position, before, target));
            }
        }
        return new BlockPlan(request.selection(), List.copyOf(changes));
    }

    private static boolean isHorizontalPlane(dev.mintychochip.masonry.api.selection.CuboidSelection selection) {
        return selection != null && selection.height() == 1;
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
