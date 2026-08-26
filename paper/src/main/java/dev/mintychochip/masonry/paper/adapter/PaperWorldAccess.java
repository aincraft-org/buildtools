package dev.mintychochip.masonry.paper.adapter;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.operation.BlockChange;
import dev.mintychochip.masonry.api.service.WorldAccess;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * {@link WorldAccess} over {@link Server}. {@link #setBlock} fires {@link BlockBreakEvent}
 * and {@link BlockPlaceEvent} and honors cancellation.
 */
public final class PaperWorldAccess implements WorldAccess {
    private final Server server;

    /**
     * @param server running server
     */
    public PaperWorldAccess(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public BlockState getBlock(BlockPosition position) {
        return PaperBlockStates.fromBukkit(blockAt(position).getBlockData());
    }

    @Override
    public boolean setBlock(ActorId actor, BlockPosition position, BlockState state) {
        Block block = blockAt(position);
        BlockData newData = server.createBlockData(PaperBlockStates.toBukkitString(state));
        Player player = server.getPlayer(actor.value());
        if (player != null) {
            if (!block.getType().isAir()) {
                BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
                server.getPluginManager().callEvent(breakEvent);
                if (breakEvent.isCancelled()) {
                    return false;
                }
            }
            if (!state.isAir()) {
                BlockPlaceEvent placeEvent = new BlockPlaceEvent(
                        block,
                        block.getState(),
                        block,
                        new ItemStack(newData.getMaterial()),
                        player,
                        true,
                        EquipmentSlot.HAND);
                server.getPluginManager().callEvent(placeEvent);
                if (placeEvent.isCancelled()) {
                    return false;
                }
            }
        }
        block.setBlockData(newData, false);
        return true;
    }

    @Override
    public boolean setBlocks(ActorId actor, List<BlockChange> changes) {
        Map<Long, List<BlockChange>> byChunk = new HashMap<>();
        for (BlockChange change : changes) {
            BlockPosition position = change.position();
            long key = chunkKey(position.x(), position.z());
            byChunk.computeIfAbsent(key, ignored -> new ArrayList<>()).add(change);
        }
        List<BlockChange> applied = new ArrayList<>();
        for (List<BlockChange> group : byChunk.values()) {
            for (BlockChange change : group) {
                if (!setBlock(actor, change.position(), change.after())) {
                    for (int i = applied.size() - 1; i >= 0; i--) {
                        BlockChange undo = applied.get(i);
                        setBlock(actor, undo.position(), undo.before());
                    }
                    return false;
                }
                applied.add(change);
            }
        }
        return true;
    }

    @Override
    public boolean isLoaded(BlockPosition position) {
        World world = world(position);
        return world.isChunkLoaded(Math.floorDiv(position.x(), 16), Math.floorDiv(position.z(), 16));
    }

    @Override
    public boolean isReplaceable(BlockPosition position) {
        return blockAt(position).getBlockData().isReplaceable();
    }

    private static long chunkKey(int x, int z) {
        return ((long) Math.floorDiv(x, 16) << 32) | (Math.floorDiv(z, 16) & 0xffffffffL);
    }

    private Block blockAt(BlockPosition position) {
        return world(position).getBlockAt(position.x(), position.y(), position.z());
    }

    private World world(BlockPosition position) {
        World world = server.getWorld(position.worldId());
        if (world == null) {
            throw new IllegalArgumentException("Unknown world: " + position.worldId());
        }
        return world;
    }
}
