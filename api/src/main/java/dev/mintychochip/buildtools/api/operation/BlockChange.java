package dev.mintychochip.buildtools.api.operation;

import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.Objects;

/**
 * One recorded mutation: previous state, new state, and position.
 *
 * @param position world coordinate
 * @param before state before the operation
 * @param after state after the operation
 */
public record BlockChange(BlockPosition position, BlockState before, BlockState after) {
    /**
     * @throws NullPointerException if any component is {@code null}
     */
    public BlockChange {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
    }

    /** @return {@code true} if before and after are equal */
    public boolean isNoOp() {
        return before.equals(after);
    }
}
