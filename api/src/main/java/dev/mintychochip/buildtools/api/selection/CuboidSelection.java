package dev.mintychochip.buildtools.api.selection;

import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record CuboidSelection(BlockPosition pos1, BlockPosition pos2) {
    public CuboidSelection {
        Objects.requireNonNull(pos1, "pos1");
        Objects.requireNonNull(pos2, "pos2");
        if (!pos1.worldId().equals(pos2.worldId())) {
            throw new IllegalArgumentException("Selection corners must share a world");
        }
    }

    public String worldId() {
        return pos1.worldId();
    }

    public int width() {
        return Math.abs(pos1.x() - pos2.x()) + 1;
    }

    public int height() {
        return Math.abs(pos1.y() - pos2.y()) + 1;
    }

    public int depth() {
        return Math.abs(pos1.z() - pos2.z()) + 1;
    }

    public int volume() {
        return width() * height() * depth();
    }

    public int extent() {
        return Math.max(width(), Math.max(height(), depth()));
    }

    public BlockPosition min() {
        return new BlockPosition(
                worldId(),
                Math.min(pos1.x(), pos2.x()),
                Math.min(pos1.y(), pos2.y()),
                Math.min(pos1.z(), pos2.z()));
    }

    public BlockPosition max() {
        return new BlockPosition(
                worldId(),
                Math.max(pos1.x(), pos2.x()),
                Math.max(pos1.y(), pos2.y()),
                Math.max(pos1.z(), pos2.z()));
    }

    public boolean contains(BlockPosition position) {
        Objects.requireNonNull(position, "position");
        if (!worldId().equals(position.worldId())) {
            return false;
        }
        BlockPosition lo = min();
        BlockPosition hi = max();
        return position.x() >= lo.x()
                && position.x() <= hi.x()
                && position.y() >= lo.y()
                && position.y() <= hi.y()
                && position.z() >= lo.z()
                && position.z() <= hi.z();
    }

    public List<BlockPosition> positions() {
        BlockPosition lo = min();
        BlockPosition hi = max();
        List<BlockPosition> positions = new ArrayList<>(volume());
        for (int x = lo.x(); x <= hi.x(); x++) {
            for (int y = lo.y(); y <= hi.y(); y++) {
                for (int z = lo.z(); z <= hi.z(); z++) {
                    positions.add(new BlockPosition(worldId(), x, y, z));
                }
            }
        }
        return List.copyOf(positions);
    }
}
