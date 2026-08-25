package dev.mintychochip.masonry.api.service;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;

/**
 * Read/write access to a world. Paper implementations fire place/break events and honor cancel.
 */
public interface WorldAccess {
    /**
     * @param position block coordinate
     * @return current state, or air if unset
     */
    BlockState getBlock(BlockPosition position);

    /**
     * Applies a block mutation. Implementations must fire standard place/break events
     * when an actor is supplied and must honor cancellation.
     *
     * @param actor acting player
     * @param position block coordinate
     * @param state new state
     * @return {@code false} if the mutation was cancelled or otherwise refused
     */
    boolean setBlock(ActorId actor, BlockPosition position, BlockState state);

    /**
     * @param position block coordinate
     * @return {@code true} if the chunk is loaded
     */
    boolean isLoaded(BlockPosition position);

    /**
     * @param position block coordinate
     * @return {@code true} if the block can be replaced without being broken first
     */
    boolean isReplaceable(BlockPosition position);
}
