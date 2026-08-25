package dev.mintychochip.masonry.api.tool;

import dev.mintychochip.masonry.api.cost.ResourceCost;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.world.BlockPosition;
import java.util.List;
import java.util.Objects;

/**
 * Exact planned mutation. For mutating tools the affected list must equal execute diffs.
 *
 * @param region bounding cuboid of the plan
 * @param affectedPositions blocks that will change (empty for non-mutating tools such as copy)
 * @param estimatedCost placement items the executor will charge
 */
public record ToolPreview(
        CuboidSelection region, List<BlockPosition> affectedPositions, ResourceCost estimatedCost) {
    /**
     * @throws NullPointerException if a component is {@code null}
     */
    public ToolPreview {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(affectedPositions, "affectedPositions");
        Objects.requireNonNull(estimatedCost, "estimatedCost");
        affectedPositions = List.copyOf(affectedPositions);
    }

    /** @return {@code affectedPositions.size()} */
    public int affectedCount() {
        return affectedPositions.size();
    }
}
