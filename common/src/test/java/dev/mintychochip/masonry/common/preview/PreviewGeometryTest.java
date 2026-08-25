package dev.mintychochip.masonry.common.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.world.BlockPosition;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Preview geometry contract: corners always present, display cap respected, and the
 * player-facing TOP face is sampled across its interior — an edges-only ring is what made
 * large selections look hollow from above.
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
}
