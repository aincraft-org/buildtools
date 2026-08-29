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
        long lastInputTick) {
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
        if (lastInputTick < 0) {
            throw new IllegalArgumentException("lastInputTick must not be negative");
        }
    }

    /** @return final planned block position */
    public BlockPosition endpoint() {
        return anchor.offset(direction.x() * length, 0, direction.z() * length);
    }

    /** @return one-block-wide region excluding the existing anchor */
    public CuboidSelection selection() {
        return new CuboidSelection(
                anchor.offset(direction.x(), 0, direction.z()), endpoint());
    }

    /** @param newLength new positive length @param inputTick input time @return updated plan */
    public ExtensionPlan withLength(int newLength, long inputTick) {
        return new ExtensionPlan(anchor, direction, block, newLength, inputTick);
    }
}
