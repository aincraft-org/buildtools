package dev.mintychochip.masonry.paper.integration;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.operation.BlockChange;
import dev.mintychochip.masonry.api.service.WorldAccess;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import dev.mintychochip.masonry.paper.adapter.PaperBlockStates;
import dev.mintychochip.masonry.paper.adapter.PaperWorldAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * {@link WorldAccess} used by the plugin. Reads and single-block writes delegate to
 * {@link PaperWorldAccess}; batch writes preflight per-position Bukkit events and WorldGuard
 * synchronously, then apply the committed diff set through FastAsyncWorldEdit when available.
 *
 * <p>Semantics preserved from the base contract: every write fires standard place/break
 * events and honors cancellation; a cancelled or refused batch applies nothing.
 */
public final class MasonryWorldAccess implements WorldAccess {
    private final PaperWorldAccess paper;
    private final Server server;
    private final RegionGuard regions;
    private final FaweBatchWriter fawe;
    private final Logger logger;

    /**
     * Builds the access with auto-detection of WorldGuard and FastAsyncWorldEdit.
     *
     * @param server running server
     * @param logger plugin logger
     */
    public MasonryWorldAccess(Server server, Logger logger) {
        this.server = Objects.requireNonNull(server, "server");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.paper = new PaperWorldAccess(server);
        this.regions = WorldGuardRegionGuard.resolve(logger, server);
        this.fawe = FaweBatchWriter.resolve(logger, server);
    }

    /** Visible for tests: injects collaborators directly. */
    MasonryWorldAccess(PaperWorldAccess paper, Server server, RegionGuard regions, FaweBatchWriter fawe) {
        this.paper = Objects.requireNonNull(paper, "paper");
        this.server = Objects.requireNonNull(server, "server");
        this.regions = Objects.requireNonNull(regions, "regions");
        this.fawe = fawe; // null when FAWE is unavailable
        this.logger = Logger.getLogger("Masonry");
    }

    @Override
    public BlockState getBlock(BlockPosition position) {
        return paper.getBlock(position);
    }

    @Override
    public boolean setBlock(ActorId actor, BlockPosition position, BlockState state) {
        Block block = blockAt(position);
        if (!preflight(actor, block, state)) {
            return false;
        }
        block.setBlockData(toBukkit(block, state), false);
        return true;
    }

    @Override
    public boolean setBlocks(ActorId actor, List<BlockChange> changes) {
        List<BlockChange> allowed = new ArrayList<>(changes.size());
        for (BlockChange change : changes) {
            if (change.isNoOp()) {
                continue;
            }
            Block block = blockAt(change.position());
            if (!preflight(actor, block, change.after())) {
                return false;
            }
            allowed.add(change);
        }
        if (allowed.isEmpty()) {
            return true;
        }
        if (fawe != null) {
            return fawe.write(actor, allowed);
        }
        return applyBukkit(actor, allowed);
    }

    @Override
    public boolean isLoaded(BlockPosition position) {
        return paper.isLoaded(position);
    }

    @Override
    public boolean isReplaceable(BlockPosition position) {
        return paper.isReplaceable(position);
    }

    private boolean preflight(ActorId actor, Block block, BlockState state) {
        if (!regions.canBreak(actor, block)) {
            return false;
        }
        if (!regions.canPlace(actor, block, toBukkit(block, state))) {
            return false;
        }
        Player player = server.getPlayer(actor.value());
        if (player == null) {
            return true;
        }
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
                    new ItemStack(toBukkit(block, state).getMaterial()),
                    player,
                    true,
                    EquipmentSlot.HAND);
            server.getPluginManager().callEvent(placeEvent);
            if (placeEvent.isCancelled()) {
                return false;
            }
        }
        return true;
    }

    private boolean applyBukkit(ActorId actor, List<BlockChange> changes) {
        List<BlockChange> applied = new ArrayList<>();
        for (BlockChange change : changes) {
            try {
                Block block = blockAt(change.position());
                block.setBlockData(toBukkit(block, change.after()), false);
                applied.add(change);
            } catch (RuntimeException e) {
                logger.warning("Masonry: Bukkit batch write failed at " + change.position() + ", rolling back.");
                for (int i = applied.size() - 1; i >= 0; i--) {
                    BlockChange undo = applied.get(i);
                    Block block = blockAt(undo.position());
                    block.setBlockData(toBukkit(block, undo.before()), false);
                }
                return false;
            }
        }
        return true;
    }

    private org.bukkit.block.data.BlockData toBukkit(Block block, BlockState state) {
        return server.createBlockData(PaperBlockStates.toBukkitString(state));
    }

    private Block blockAt(BlockPosition position) {
        World world = server.getWorld(position.worldId());
        if (world == null) {
            throw new IllegalArgumentException("Unknown world: " + position.worldId());
        }
        return world.getBlockAt(position.x(), position.y(), position.z());
    }
}