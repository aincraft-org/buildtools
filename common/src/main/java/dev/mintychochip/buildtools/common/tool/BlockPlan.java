package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.operation.BlockChange;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.List;

/**
 * Shared plan used by mutating tools so preview positions equal execute diffs.
 *
 * @param region bounding cuboid
 * @param changes ordered mutations
 */
public record BlockPlan(CuboidSelection region, List<BlockChange> changes) {
    /** @return positions of {@link #changes()} */
    public List<BlockPosition> affectedPositions() {
        return changes.stream().map(BlockChange::position).toList();
    }
}
