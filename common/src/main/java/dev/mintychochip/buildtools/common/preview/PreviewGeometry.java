package dev.mintychochip.buildtools.common.preview;

import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PreviewGeometry {
    public static final int DEFAULT_MAX_DISPLAYS = 256;

    private PreviewGeometry() {}

    public static List<BlockPosition> outline(CuboidSelection selection) {
        return outline(selection, DEFAULT_MAX_DISPLAYS);
    }

    /**
     * Bounded aggregated cuboid outline. Samples the 12 edges; never one point per volume block.
     */
    public static List<BlockPosition> outline(CuboidSelection selection, int maxDisplays) {
        Objects.requireNonNull(selection, "selection");
        if (maxDisplays <= 0) {
            throw new IllegalArgumentException("maxDisplays must be positive");
        }
        BlockPosition min = selection.min();
        BlockPosition max = selection.max();
        Set<BlockPosition> points = new LinkedHashSet<>();
        int[][] edges = {
            {min.x(), min.y(), min.z(), max.x(), min.y(), min.z()},
            {min.x(), min.y(), max.z(), max.x(), min.y(), max.z()},
            {min.x(), max.y(), min.z(), max.x(), max.y(), min.z()},
            {min.x(), max.y(), max.z(), max.x(), max.y(), max.z()},
            {min.x(), min.y(), min.z(), min.x(), max.y(), min.z()},
            {max.x(), min.y(), min.z(), max.x(), max.y(), min.z()},
            {min.x(), min.y(), max.z(), min.x(), max.y(), max.z()},
            {max.x(), min.y(), max.z(), max.x(), max.y(), max.z()},
            {min.x(), min.y(), min.z(), min.x(), min.y(), max.z()},
            {max.x(), min.y(), min.z(), max.x(), min.y(), max.z()},
            {min.x(), max.y(), min.z(), min.x(), max.y(), max.z()},
            {max.x(), max.y(), min.z(), max.x(), max.y(), max.z()}
        };
        int perEdge = Math.max(2, maxDisplays / 12);
        for (int[] edge : edges) {
            sampleEdge(points, selection.worldId(), edge[0], edge[1], edge[2], edge[3], edge[4], edge[5], perEdge);
        }
        if (points.size() > maxDisplays) {
            List<BlockPosition> trimmed = new ArrayList<>(maxDisplays);
            int i = 0;
            for (BlockPosition point : points) {
                if (i++ >= maxDisplays) {
                    break;
                }
                trimmed.add(point);
            }
            return List.copyOf(trimmed);
        }
        return List.copyOf(points);
    }

    private static void sampleEdge(
            Set<BlockPosition> points,
            String worldId,
            int x0,
            int y0,
            int z0,
            int x1,
            int y1,
            int z1,
            int samples) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int dz = z1 - z0;
        int length = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        int steps = Math.max(1, Math.min(length, samples - 1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            int x = x0 + (int) Math.round(dx * t);
            int y = y0 + (int) Math.round(dy * t);
            int z = z0 + (int) Math.round(dz * t);
            points.add(new BlockPosition(worldId, x, y, z));
        }
    }
}
