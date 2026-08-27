package dev.mintychochip.masonry.paper;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.limits.OperationLimits;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.common.session.PlayerSession;
import dev.mintychochip.masonry.common.session.PlayerSessionStore;
import dev.mintychochip.masonry.paper.adapter.PaperPreviewRenderer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import dev.mintychochip.masonry.paper.adapter.GadgetItem;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

/**
 * Live aim preview: while a player holds the gadget, the cuboid between their stored
 * {@code pos1} and the block they are looking at is outlined continuously. This driver is
 * the single owner of hover preview lifetime — it renders on region change and clears when
 * the gadget is lowered, no corner is set, or the target leaves reach. A signature map
 * prevents respawning entities while the player merely stands still.
 */
public final class HoverPreviewDriver implements Runnable {
    private static final long DELAY_TICKS = 8L;
    private static final long PERIOD_TICKS = 4L;

    private final JavaPlugin plugin;
    private final PlayerSessionStore sessions;
    private final OperationLimits limits;
    private final PaperPreviewRenderer previews;
    private final Map<UUID, String> shownRegions = new HashMap<>();

    public HoverPreviewDriver(
            JavaPlugin plugin,
            PlayerSessionStore sessions,
            OperationLimits limits,
            PaperPreviewRenderer previews) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.previews = Objects.requireNonNull(previews, "previews");
    }

    /** Starts the driver on the Bukkit scheduler. */
    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, DELAY_TICKS, PERIOD_TICKS);
    }

    /** Drops hover state for a quitting player so nothing lingers. */
    public void forget(UUID playerId) {
        if (shownRegions.remove(playerId) != null) {
            previews.clear(new ActorId(playerId));
        }
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ActorId actor = new ActorId(player.getUniqueId());
            if (!GadgetItem.isGadget(plugin, player.getInventory().getItemInMainHand())
                    || !player.hasPermission("masonry.command")) {
                hide(actor);
                continue;
            }
            PlayerSession session = sessions.session(actor);
            BlockPosition pos1 = session.pos1().orElse(null);
            BlockPosition target = targetOf(player);
            BlockPosition placement = placementOf(player);
            // In paste/move, show the held clipboard as a ghost at the placement cell; the
            // selection outline is meaningless for a paste origin.
            boolean pasteOrMove = session.mode() == dev.mintychochip.masonry.common.session.ToolMode.PASTE
                    || session.mode() == dev.mintychochip.masonry.common.session.ToolMode.MOVE
                    || session.mode() == dev.mintychochip.masonry.common.session.ToolMode.COPY
                    || session.mode() == dev.mintychochip.masonry.common.session.ToolMode.CUT;
            if (pasteOrMove && session.clipboard().isPresent() && placement != null) {
                String signature = session.mode() + "|ghost|" + placement + "|" + session.previewAnimation();
                if (signature.equals(shownRegions.put(player.getUniqueId(), signature))) {
                    continue;
                }
                previews.showGhost(actor, session.clipboard().orElseThrow(), placement);
                continue;
            }
            if (pos1 == null
                    || target == null
                    || !pos1.worldId().equals(target.worldId())) {
                hide(actor);
                continue;
            }
            CuboidSelection region = new CuboidSelection(pos1, target);
            String signature = session.mode() + "|" + region.min() + "|" + region.max() + "|" + session.previewAnimation();
            if (signature.equals(shownRegions.put(player.getUniqueId(), signature))) {
                continue;
            }
            previews.showSelection(actor, region);
        }
    }

    private void hide(ActorId actor) {
        if (shownRegions.remove(actor.value()) != null) {
            previews.clear(actor);
        }
    }

    private BlockPosition targetOf(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(limits.interactionDistance());
        Block block = hit != null ? hit.getHitBlock() : null;
        if (block == null) {
            return null;
        }
        return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
    private BlockPosition placementOf(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(limits.interactionDistance());
        Block block = hit != null ? hit.getHitBlock() : null;
        if (block == null) {
            return null;
        }
        BlockFace face = hit.getHitBlockFace();
        int dx = face == null ? 0 : face.getModX();
        int dy = face == null ? 0 : face.getModY();
        int dz = face == null ? 0 : face.getModZ();
        return new BlockPosition(block.getWorld().getName(), block.getX() + dx, block.getY() + dy, block.getZ() + dz);
    }
}
