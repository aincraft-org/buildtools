package dev.mintychochip.masonry.common.session;

import dev.mintychochip.masonry.api.clipboard.BlockOffset;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import java.util.Objects;

/** Immutable player intent for a horizontal extension preview. */
public record ExtensionPlan(
        BlockPosition anchor,
        BlockOffset direction,
        BlockState block,
        int length,
        int width,
        long lastInputTick) {
    /** Convenience for a one-block-wide line. */
    public ExtensionPlan(
            BlockPosition anchor,
            BlockOffset direction,
            BlockState block,
            int length,
            long lastInputTick) {
        this(anchor, direction, block, length, 1, lastInputTick);
    }

    public ExtensionPlan {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(block, "block");
        if (Math.abs(direction.x()) + Math.abs(direction.z()) != 1 || direction.y() != 0) {
            throw new IllegalArgumentException("direction must be a horizontal unit offset");
        }
        if (length < 1) {
            throw new IllegalArgumentException("length must be positive");
        }
        if (width < 1) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (lastInputTick < 0) {
            throw new IllegalArgumentException("lastInputTick must not be negative");
        }
    }

    /** @return final planned block position (one past the anchor along length) */
    public BlockPosition endpoint() {
        return anchor.offset(direction.x() * length, 0, direction.z() * length);
    }

    /**
     * @return horizontal plane (length by width) one cell past the anchor, centered on the
     *     direction axis
     */
    public CuboidSelection selection() {
        // Perpendicular spread: for an X direction the width spans Z, and vice versa.
        int spreadZ = direction.x() != 0 ? (width - 1) / 2 : 0;
        int spreadX = direction.z() != 0 ? (width - 1) / 2 : 0;
        int lowZ = -spreadZ;
        int highZ = spreadZ;
        int lowX = -spreadX;
        int highX = spreadX;
        if (width % 2 == 0) {
            if (direction.x() != 0) {
                highZ = spreadZ + 1;
            } else {
                highX = spreadX + 1;
            }
        }
        BlockPosition start = anchor.offset(direction.x() + lowX, 0, direction.z() + lowZ);
        BlockPosition end = endpoint().offset(highX, 0, highZ);
        return new CuboidSelection(start, end);
    }

    /** @param newLength new positive length @param inputTick input time @return updated plan */
    public ExtensionPlan withLength(int newLength, long inputTick) {
        return new ExtensionPlan(anchor, direction, block, newLength, width, inputTick);
    }

    /** @param newWidth new positive width @param inputTick input time @return updated plan */
    public ExtensionPlan withWidth(int newWidth, long inputTick) {
        return new ExtensionPlan(anchor, direction, block, length, newWidth, inputTick);
    }
}
