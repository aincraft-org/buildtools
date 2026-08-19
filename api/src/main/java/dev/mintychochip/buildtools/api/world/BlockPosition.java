package dev.mintychochip.buildtools.api.world;

import java.util.Objects;

/**
 * A block coordinate in a named world.
 *
 * @param worldId world name (Paper {@code World#getName()})
 * @param x block X
 * @param y block Y
 * @param z block Z
 */
public record BlockPosition(String worldId, int x, int y, int z) {
    /**
     * @throws NullPointerException if {@code worldId} is {@code null}
     */
    public BlockPosition {
        Objects.requireNonNull(worldId, "worldId");
    }

    /**
     * Returns a new position offset by the given block deltas.
     *
     * @param dx X delta
     * @param dy Y delta
     * @param dz Z delta
     * @return position in the same world
     */
    public BlockPosition offset(int dx, int dy, int dz) {
        return new BlockPosition(worldId, x + dx, y + dy, z + dz);
    }

    /**
     * Euclidean distance between block centers (used for interaction reach).
     *
     * @param other other position
     * @return distance in blocks
     * @throws IllegalArgumentException if the positions are in different worlds
     */
    public double centerDistance(BlockPosition other) {
        Objects.requireNonNull(other, "other");
        if (!worldId.equals(other.worldId)) {
            throw new IllegalArgumentException("Positions must share a world");
        }
        double dx = (x + 0.5) - (other.x + 0.5);
        double dy = (y + 0.5) - (other.y + 0.5);
        double dz = (z + 0.5) - (other.z + 0.5);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
