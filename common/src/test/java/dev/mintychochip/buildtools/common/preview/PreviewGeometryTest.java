package dev.mintychochip.buildtools.common.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.List;
import org.junit.jupiter.api.Test;

class PreviewGeometryTest {
    @Test
    void outlineMatchesCuboidEdgesAndIsBounded() {
        CuboidSelection selection = new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("world", 63, 80, 63));
        List<BlockPosition> outline = PreviewGeometry.outline(selection, 256);
        assertTrue(outline.size() <= 256);
        assertTrue(outline.size() < selection.volume());
        assertTrue(outline.contains(selection.min()));
        assertTrue(outline.contains(selection.max()));
        for (BlockPosition point : outline) {
            boolean onFace = point.x() == 0
                    || point.x() == 63
                    || point.y() == 64
                    || point.y() == 80
                    || point.z() == 0
                    || point.z() == 63;
            assertTrue(onFace, "outline point left the cuboid surface: " + point);
        }
    }

    @Test
    void smallSelectionOutlineIncludesCorners() {
        CuboidSelection selection = new CuboidSelection(
                new BlockPosition("world", 5, 10, 5),
                new BlockPosition("world", 7, 12, 6));
        List<BlockPosition> outline = PreviewGeometry.outline(selection);
        assertEquals(selection.min(), outline.getFirst());
        assertTrue(outline.contains(new BlockPosition("world", 7, 10, 5)));
    }
}
