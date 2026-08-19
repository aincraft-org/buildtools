package dev.mintychochip.buildtools.api.tool;

import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.List;
import java.util.Objects;

public record ToolPreview(
        CuboidSelection region, List<BlockPosition> affectedPositions, ResourceCost estimatedCost) {
    public ToolPreview {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(affectedPositions, "affectedPositions");
        Objects.requireNonNull(estimatedCost, "estimatedCost");
        affectedPositions = List.copyOf(affectedPositions);
    }

    public int affectedCount() {
        return affectedPositions.size();
    }
}
