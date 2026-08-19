package dev.mintychochip.buildtools.api.operation;

import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.Objects;

public record BlockChange(BlockPosition position, BlockState before, BlockState after) {
    public BlockChange {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
    }

    public boolean isNoOp() {
        return before.equals(after);
    }
}
