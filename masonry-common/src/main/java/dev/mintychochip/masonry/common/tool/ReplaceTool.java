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
 * Replaces blocks matching {@code from} with {@code to}. Only matching cells are previewed
 * and executed.
 */
public final class ReplaceTool extends MutatingTool {
    @Override
    public String name() {
        return "replace";
    }

    @Override
    public ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        ValidationResult base = super.validate(request, world, survival);
        if (!base.valid()) {
            return base;
        }
        if (parse(request, "from") == null || parse(request, "to") == null) {
            return ValidationResult.invalid("Usage: /masonry replace <from> <to>");
        }
        return ValidationResult.passed();
    }

    @Override
    protected BlockPlan plan(ToolRequest request, WorldAccess world) {
        BlockState from = parse(request, "from");
        BlockState to = parse(request, "to");
        if (from == null || to == null || request.selection() == null) {
            return new BlockPlan(request.selection(), List.of());
        }
        List<BlockChange> changes = new ArrayList<>();
        for (BlockPosition position : request.selection().positions()) {
            if (request.isExcluded(position)) {
                continue;
            }
            BlockState before = world.getBlock(position);
            if (BlockStates.matches(before, from) && !before.equals(to)) {
                changes.add(new BlockChange(position, before, to));
            }
        }
        return new BlockPlan(request.selection(), List.copyOf(changes));
    }

    private static BlockState parse(ToolRequest request, String key) {
        String raw = request.argument(key);
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
