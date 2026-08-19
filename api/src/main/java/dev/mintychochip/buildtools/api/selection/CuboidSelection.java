package dev.mintychochip.buildtools.api.selection;

import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Inclusive axis-aligned cuboid defined by two corners in the same world.
 *
 * @param pos1 first corner (typically {@code /bt pos1})
 * @param pos2 second corner (typically {@code /bt pos2})
 */
public record CuboidSelection(BlockPosition pos1, BlockPosition pos2) {
    /**
     * @throws NullPointerException if either corner is {@code null}
     * @throws IllegalArgumentException if the corners are in different worlds
     */
    public CuboidSelection {
        Objects.requireNonNull(pos1, "pos1");
        Objects.requireNonNull(pos2, "pos2");
        if (!pos1.worldId().equals(pos2.worldId())) {
            throw new IllegalArgumentException("Selection corners must share a world");
        }
    }

    /** @return world id shared by both corners */
    public String worldId() {
        return pos1.worldId();
    }

    /** @return inclusive size along X */
    public int width() {
        return Math.abs(pos1.x() - pos2.x()) + 1;
    }

    /** @return inclusive size along Y */
    public int height() {
        return Math.abs(pos1.y() - pos2.y()) + 1;
    }

    /** @return inclusive size along Z */
    public int depth() {
        return Math.abs(pos1.z() - pos2.z()) + 1;
    }

    /** @return {@code width * height * depth} */
    public int volume() {
        return width() * height() * depth();
    }

    /**
     * Longest inclusive edge; compared against {@code selection_extent}.
     *
     * @return max of width, height, and depth
     */
    public int extent() {
        return Math.max(width(), Math.max(height(), depth()));
    }

    /** @return inclusive minimum corner */
    public BlockPosition min() {
        return new BlockPosition(
                worldId(),
                Math.min(pos1.x(), pos2.x()),
                Math.min(pos1.y(), pos2.y()),
                Math.min(pos1.z(), pos2.z()));
    }

    /** @return inclusive maximum corner */
    public BlockPosition max() {
        return new BlockPosition(
                worldId(),
                Math.max(pos1.x(), pos2.x()),
                Math.max(pos1.y(), pos2.y()),
                Math.max(pos1.z(), pos2.z()));
    }

    /**
     * @param position candidate block
     * @return {@code true} if {@code position} is inside this cuboid
     */
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

    /**
     * Every block in the cuboid, in X-then-Y-then-Z order.
     *
     * @return immutable list of size {@link #volume()}
     */
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
