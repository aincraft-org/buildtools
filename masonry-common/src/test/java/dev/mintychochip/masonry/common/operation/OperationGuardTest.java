package dev.mintychochip.masonry.common.operation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.cost.ResourceCost;
import dev.mintychochip.masonry.api.limits.OperationLimits;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import dev.mintychochip.masonry.api.world.BlockPosition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests interaction, extent, and preview-size checks independently. */
class OperationGuardTest {
    private final OperationGuard guard = new OperationGuard(new OperationLimits(6, 4, 8));

    @Test
    void interactionDistanceIsIndependentOfSelectionExtent() {
        BlockPosition origin = new BlockPosition("world", 0, 64, 0);
        assertTrue(guard.validateInteraction(origin, new BlockPosition("world", 4, 64, 0)).valid());
        assertFalse(guard.validateInteraction(origin, new BlockPosition("world", 7, 64, 0)).valid());
    }

    @Test
    void selectionExtentDoesNotUseInteractionDistance() {
        CuboidSelection allowed = new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("world", 3, 64, 0));
        CuboidSelection tooLong = new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("world", 4, 64, 0));

        assertTrue(guard.validateSelection(allowed).valid());
        assertFalse(guard.validateSelection(tooLong).valid());
    }

    @Test
    void previewCountIsCappedByMaxOperationBlocks() {
        CuboidSelection region = new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("world", 1, 64, 0));
        assertTrue(guard.validatePreview(new ToolPreview(region, positions(region, 8), ResourceCost.none())).valid());
        assertFalse(guard.validatePreview(new ToolPreview(region, positions(region, 9), ResourceCost.none())).valid());
    }

    private static List<BlockPosition> positions(CuboidSelection region, int count) {
        List<BlockPosition> positions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            positions.add(new BlockPosition(region.worldId(), i, 64, 0));
        }
        return positions;
    }
}
