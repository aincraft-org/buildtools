package dev.mintychochip.buildtools.common.preview;

import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Bounded cuboid outline samples for BlockDisplay / particle previews.
 */
public final class PreviewGeometry {
    /** Default cap so large selections never spawn one entity per block. */
    public static final int DEFAULT_MAX_DISPLAYS = 256;

    private PreviewGeometry() {}

    /**
     * Outline using {@link #DEFAULT_MAX_DISPLAYS}.
     *
     * @param selection cuboid
     * @return unique edge samples, size at most the default cap
     */
    public static List<BlockPosition> outline(CuboidSelection selection) {
        return outline(selection, DEFAULT_MAX_DISPLAYS);
    }

    /**
     * Bounded cuboid preview. Samples the player-facing TOP face as a lattice first so
     * selections are not hollow from above, then distributes the remaining display budget
     * across the 12 edges proportionally to their length. Never one point per volume block.
     *
     * @param selection cuboid
     * @param maxDisplays hard cap on returned points
     * @return unique samples, size at most {@code maxDisplays}
     */
    public static List<BlockPosition> outline(CuboidSelection selection, int maxDisplays) {
        Objects.requireNonNull(selection, "selection");
        if (maxDisplays <= 0) {
            throw new IllegalArgumentException("maxDisplays must be positive");
        }
        BlockPosition min = selection.min();
        BlockPosition max = selection.max();
        Set<BlockPosition> points = new LinkedHashSet<>();

        int topBudget = Math.max(4, maxDisplays * 3 / 5);
        int stride = 1;
        while (samples(min.x(), max.x(), stride).size()
                * samples(min.z(), max.z(), stride).size() > topBudget) {
            stride++;
        }
        for (int x : samples(min.x(), max.x(), stride)) {
            for (int z : samples(min.z(), max.z(), stride)) {
                points.add(new BlockPosition(selection.worldId(), x, max.y(), z));
            }
        }

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
        int[] lengths = new int[edges.length];
        long totalLength = 0;
        for (int i = 0; i < edges.length; i++) {
            int[] edge = edges[i];
            lengths[i] = Math.max(Math.abs(edge[3] - edge[0]),
                    Math.max(Math.abs(edge[4] - edge[1]), Math.abs(edge[5] - edge[2])));
            totalLength += Math.max(1, lengths[i]);
        }
        int remaining = Math.max(0, maxDisplays - points.size());
        for (int i = 0; i < edges.length && remaining > 0; i++) {
            int[] edge = edges[i];
            int length = lengths[i];
            int steps = (int) Math.round((double) length * remaining / totalLength);
            steps = Math.max(1, Math.min(length, steps));
            sampleEdge(points, selection.worldId(),
                    edge[0], edge[1], edge[2], edge[3], edge[4], edge[5], steps + 1);
            remaining = maxDisplays - points.size();
        }
        if (points.size() > maxDisplays) {
            return List.copyOf(new ArrayList<>(points).subList(0, maxDisplays));
        }
        return List.copyOf(points);
    }

    /**
     * Inclusive axis samples from {@code from} to {@code to} at most every {@code stride},
     * always ending on {@code to}.
     */
    private static List<Integer> samples(int from, int to, int stride) {
        List<Integer> values = new ArrayList<>();
        for (int value = from; value < to; value += stride) {
            values.add(value);
        }
        values.add(to);
        return values;
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
