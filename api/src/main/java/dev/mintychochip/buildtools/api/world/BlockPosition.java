package dev.mintychochip.buildtools.api.world;

import java.util.Objects;

public record BlockPosition(String worldId, int x, int y, int z) {
    public BlockPosition {
        Objects.requireNonNull(worldId, "worldId");
    }

    public BlockPosition offset(int dx, int dy, int dz) {
        return new BlockPosition(worldId, x + dx, y + dy, z + dz);
    }

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
