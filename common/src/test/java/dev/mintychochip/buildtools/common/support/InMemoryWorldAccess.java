package dev.mintychochip.buildtools.common.support;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class InMemoryWorldAccess implements WorldAccess {
    private final Map<BlockPosition, BlockState> blocks = new HashMap<>();
    private final Set<BlockPosition> cancelled = new HashSet<>();

    public InMemoryWorldAccess put(BlockPosition position, BlockState state) {
        blocks.put(position, state);
        return this;
    }

    public InMemoryWorldAccess fill(
            BlockPosition min, BlockPosition max, BlockState state) {
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    blocks.put(new BlockPosition(min.worldId(), x, y, z), state);
                }
            }
        }
        return this;
    }

    public InMemoryWorldAccess cancel(BlockPosition position) {
        cancelled.add(position);
        return this;
    }

    @Override
    public BlockState getBlock(BlockPosition position) {
        return blocks.getOrDefault(position, BlockState.AIR);
    }

    @Override
    public boolean setBlock(ActorId actor, BlockPosition position, BlockState state) {
        if (cancelled.contains(position)) {
            return false;
        }
        blocks.put(position, state);
        return true;
    }

    @Override
    public boolean isLoaded(BlockPosition position) {
        return true;
    }
}
