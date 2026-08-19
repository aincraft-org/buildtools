package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.operation.BlockChange;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.List;

public record BlockPlan(CuboidSelection region, List<BlockChange> changes) {
    public List<BlockPosition> affectedPositions() {
        return changes.stream().map(BlockChange::position).toList();
    }
}
