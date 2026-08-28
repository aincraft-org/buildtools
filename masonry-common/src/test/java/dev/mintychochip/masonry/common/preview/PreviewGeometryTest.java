package dev.mintychochip.masonry.common.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.world.BlockPosition;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Preview geometry contracts: sparse outlines remain capped, while complete surfaces include
 * every outer-face block and reject an insufficient packet budget.
 */
class PreviewGeometryTest {

    private static BlockPosition pos(String world, int x, int y, int z) {
        return new BlockPosition(world, x, y, z);
    }

    @Test
    void topFaceInteriorIsSampledForSmallBoxes() {
        CuboidSelection selection = new CuboidSelection(
                pos("world", 0, 64, 0), pos("world", 9, 69, 9));
        var points = new HashSet<>(PreviewGeometry.outline(selection));

        // Center of the 10x10 top face must be rendered, not just the border ring.
        assertTrue(points.contains(pos("world", 4, 69, 4)),
                "top-face interior missing: " + points.size() + " points");
        assertTrue(points.contains(pos("world", 5, 69, 5)));
    }

    @Test
    void sideAndBottomFacesAreSampledForSmallBoxes() {
        CuboidSelection selection = new CuboidSelection(
                pos("world", 0, 64, 0), pos("world", 9, 69, 9));
        var points = new HashSet<>(PreviewGeometry.outline(selection));

        // Interior of a side face (constant x) and the bottom face (constant y) must render.
        assertTrue(points.contains(pos("world", 0, 66, 4)),
                "side-face interior missing: " + points.size() + " points");
        assertTrue(points.contains(pos("world", 4, 64, 4)),
                "bottom-face interior missing: " + points.size() + " points");
    }

    @Test
    void allCornersPresentAndCapRespectedForMaxBox() {
        CuboidSelection selection = new CuboidSelection(
                pos("world", 0, 64, 0), pos("world", 63, 71, 63));
        var points = new HashSet<>(PreviewGeometry.outline(selection));
        Set<BlockPosition> corners = Set.of(
                pos("world", 0, 64, 0), pos("world", 63, 64, 0),
                pos("world", 0, 64, 63), pos("world", 63, 64, 63),
                pos("world", 0, 71, 0), pos("world", 63, 71, 0),
                pos("world", 0, 71, 63), pos("world", 63, 71, 63));
        assertTrue(points.containsAll(corners), "corners missing");
        assertEquals(points.size(), PreviewGeometry.outline(selection).size(), "duplicates");
        assertTrue(points.size() <= PreviewGeometry.DEFAULT_MAX_DISPLAYS,
                "cap exceeded: " + points.size());
    }

    @Test
    void completeSurfaceIncludesEveryOuterFaceCell() {
        CuboidSelection selection = new CuboidSelection(
                pos("world", 0, 64, 0), pos("world", 2, 66, 2));
        var points = new HashSet<>(PreviewGeometry.surface(selection, 100));

        assertEquals(26, points.size());
        assertTrue(points.contains(pos("world", 1, 64, 1)));
        assertTrue(points.contains(pos("world", 0, 65, 1)));
        assertTrue(points.contains(pos("world", 1, 65, 0)));
        assertTrue(points.contains(pos("world", 2, 65, 2)));
        assertFalse(points.contains(pos("world", 1, 65, 1)));
    }

    @Test
    void completeSurfaceRejectsAnInsufficientPacketBudget() {
        CuboidSelection selection = new CuboidSelection(
                pos("world", 0, 64, 0), pos("world", 9, 69, 9));

        assertThrows(
                IllegalArgumentException.class,
                () -> PreviewGeometry.surface(selection, 100));
    }
}
