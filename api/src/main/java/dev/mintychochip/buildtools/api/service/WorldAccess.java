package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;

public interface WorldAccess {
    BlockState getBlock(BlockPosition position);

    /**
     * Applies a block mutation. Implementations must fire standard place/break events
     * when an actor is supplied and must honor cancellation.
     *
     * @return {@code false} if the mutation was cancelled or otherwise refused
     */
    boolean setBlock(ActorId actor, BlockPosition position, BlockState state);

    boolean isLoaded(BlockPosition position);
}
