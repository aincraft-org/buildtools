package dev.mintychochip.masonry.paper.integration;

import dev.mintychochip.masonry.api.ActorId;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

/**
 * Region-protection veto used before any world write. Implementations must be safe to load
 * when their backing plugin is absent (see {@link WorldGuardRegionGuard}); callers use the
 * no-op default in that case.
 */
public interface RegionGuard {
    /** No-op guard used when no region plugin is available. */
    RegionGuard NONE = new RegionGuard() {
        @Override
        public boolean canBreak(ActorId actor, Block block) {
            return true;
        }

        @Override
        public boolean canPlace(ActorId actor, Block block, BlockData newData) {
            return true;
        }
    };

    /**
     * @param actor acting player
     * @param block block being replaced or broken
     * @return {@code true} if the region allows breaking the block
     */
    boolean canBreak(ActorId actor, Block block);

    /**
     * @param actor acting player
     * @param block block being placed
     * @param newData new block data
     * @return {@code true} if the region allows placing the block
     */
    boolean canPlace(ActorId actor, Block block, BlockData newData);
}