package dev.mintychochip.masonry.paper;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.command.CommandContext;
import dev.mintychochip.masonry.api.command.CommandResult;
import dev.mintychochip.masonry.api.limits.OperationLimits;
import dev.mintychochip.masonry.api.service.SurvivalTransaction;
import dev.mintychochip.masonry.api.service.WorldAccess;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.common.command.MasonryCommands;
import dev.mintychochip.masonry.common.session.PlayerSession;
import dev.mintychochip.masonry.common.session.PlayerSessionStore;
import dev.mintychochip.masonry.common.session.ToolMode;
import dev.mintychochip.masonry.paper.adapter.GadgetItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

/**
 * Handles the Masonry gadget. Only consumes/cancels the specific gestures that are
 * explicitly recognized; unhandled clicks (e.g. a normal left-click) fall through to
 * vanilla behavior.
 */
public final class GadgetListener implements Listener {
    private final JavaPlugin plugin;
    private final MasonryCommands commands;
    private final PlayerSessionStore sessions;
    private final OperationLimits limits;
    private final WorldAccess world;
    private final SurvivalTransaction survival;
    private final Map<UUID, Long> lastRightClick = new HashMap<>();
    private static final long REPEAT_WINDOW_TICKS = 6L;

    public GadgetListener(
            JavaPlugin plugin,
            MasonryCommands commands,
            PlayerSessionStore sessions,
            OperationLimits limits,
            WorldAccess world,
            SurvivalTransaction survival) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.commands = Objects.requireNonNull(commands, "commands");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.world = Objects.requireNonNull(world, "world");
        this.survival = Objects.requireNonNull(survival, "survival");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        if (!GadgetItem.isGadget(plugin, main)) {
            return;
        }
        if (!player.hasPermission("masonry.command")) {
            return;
        }

        // Paper fires a second interact event for the off hand when a block is held there;
        // that event must be cancelled too or the offhand block still gets placed.
        Action action = event.getAction();
        if (event.getHand() == EquipmentSlot.OFF_HAND
                && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            return;
        }

        boolean sneaking = player.isSneaking();
        boolean consumed = false;

        if (sneaking && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            consumed = cycleMode(player);
        } else if (sneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            consumed = handleShiftRightClick(player, shiftRightTarget(player));
        } else if (!sneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            if (isRepeatClick(player.getUniqueId(), player.getWorld().getFullTime())) {
                consumed = repeat(player);
            } else {
                consumed = handleRightClick(player, rightClickTarget(player));
            }
        }

        if (consumed) {
            event.setCancelled(true);
        }
    }

    /**
     * Maps the swap-hands key while holding the gadget: F undoes the last operation,
     * sneak+F redoes. The swap itself is always cancelled so the item stays in hand.
     *
     * @param event swap
     */
    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!GadgetItem.isGadget(plugin, event.getMainHandItem())) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("masonry.command")) {
            return;
        }
        event.setCancelled(true);
        dispatch(player, targetOf(player), List.of(player.isSneaking() ? "redo" : "undo"));
    }

    private boolean cycleMode(Player player) {
        ToolMode next = sessions.session(actorOf(player)).mode().next();
        sessions.session(actorOf(player)).setMode(next);
        player.sendActionBar(Component.text("Masonry mode: " + next.name().toLowerCase(), NamedTextColor.GREEN));
        return true;
    }

    /**
     * Detects a double right-click: two right-clicks within {@value #REPEAT_WINDOW_TICKS}
     * game ticks. The first click of a pair records the time and runs the normal action; the
     * second click within the window re-runs the last executed tool instead.
     *
     * @param playerId actor
     * @param now current world time in ticks
     * @return {@code true} if this click is the repeat of a prior click in the window
     */
    private boolean isRepeatClick(UUID playerId, long now) {
        Long previous = lastRightClick.get(playerId);
        lastRightClick.put(playerId, now);
        return previous != null && now - previous <= REPEAT_WINDOW_TICKS;
    }

    /**
     * Re-runs the last executed tool against the current aim target by dispatching the
     * {@code repeat} subcommand. A repeat needs an active selection, so pos1 must still be
     * set; the repeat command re-applies last tool args to the region between pos1 and the
     * aimed block.
     *
     * @param player player
     * @return {@code true} to consume the click
     */
    private boolean repeat(Player player) {
        BlockPosition target = repeatTarget(player);
        if (target == null) {
            return true;
        }
        dispatch(player, target, List.of("pos2"), List.of("repeat"));
        return true;
    }

    private boolean handleShiftRightClick(Player player, BlockPosition target) {
        if (target == null) {
            return true;
        }
        ToolMode mode = sessions.session(actorOf(player)).mode();
        switch (mode) {
            // COPY/CUT: shift-right sets pos1 only; right-click sets pos2 and captures the
            // region, then auto-switches to PASTE so the next right-click places the image.
            case COPY, CUT, MOVE -> {
                dispatch(player, target, List.of("pos1"));
                return true;
            }
            default -> {
                dispatch(player, target, List.of("pos1"));
                return true;
            }
        }
    }

    private boolean handleRightClick(Player player, BlockPosition target) {
        if (target == null) {
            return true;
        }
        ToolMode mode = sessions.session(actorOf(player)).mode();
        PlayerSession session = sessions.session(actorOf(player));
        return switch (mode) {
            case SELECT -> {
                dispatch(player, target, List.of("pos2"));
                yield true;
            }
            case FILL -> handleFill(player, target);
            case REPLACE -> handleReplace(player, target);
            case COPY -> {
                if (session.pos1().isEmpty()) {
                    yield true;
                }
                dispatch(player, target, List.of("pos2"), List.of("copy"));
                session.setMode(ToolMode.PASTE);
                yield true;
            }
            case CUT -> {
                if (session.pos1().isEmpty()) {
                    yield true;
                }
                dispatch(player, target, List.of("pos2"), List.of("cut"));
                session.setMode(ToolMode.PASTE);
                yield true;
            }
            case MOVE -> {
                if (session.pos1().isEmpty()) {
                    yield true;
                }
                dispatch(player, target, List.of("pos2"), List.of("move"));
                session.setMode(ToolMode.PASTE);
                yield true;
            }
            // Once a clipboard exists, right-click pastes it at the aim target.
            case PASTE -> paste(player, target);
        };
    }

    private boolean handleFill(Player player, BlockPosition target) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType().isAir() || !offhand.getType().isBlock()) {
            return true;
        }
        if (sessions.session(actorOf(player)).pos1().isEmpty()) {
            return true;
        }
        String material = "minecraft:" + offhand.getType().getKey().getKey();
        dispatch(player, target, List.of("pos2"), List.of("survival_fill", material));
        return true;
    }

    private boolean handleReplace(Player player, BlockPosition target) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType().isAir() || !offhand.getType().isBlock()) {
            return true;
        }
        if (sessions.session(actorOf(player)).pos1().isEmpty()) {
            return true;
        }
        BlockPosition pos1 = sessions.session(actorOf(player)).pos1().orElseThrow();
        String from = world.getBlock(pos1).namespacedKey();
        String to = "minecraft:" + offhand.getType().getKey().getKey();
        dispatch(player, target, List.of("pos2"), List.of("replace", from, to));
        return true;
    }

    /**
     * Pastes the actor's clipboard at the aimed block. The clipboard origin is the aim
     * target, so COPY/CUT/PASTE/MOVE all place the held image where the player points.
     *
     * @param player player
     * @param target aimed block
     * @return {@code true} to consume the click
     */
    private boolean paste(Player player, BlockPosition target) {
        if (sessions.session(actorOf(player)).clipboard().isEmpty()) {
            return true;
        }
        // Set pos1 to the aim target so the paste command places the clipboard there.
        dispatch(player, target, List.of("pos1"), List.of("paste"));
        return true;
    }

    private void dispatch(Player player, BlockPosition target, List<String> preArgs, List<String> toolArgs) {
        for (String pre : preArgs) {
            CommandResult result = commands.execute(context(player, target, List.of(pre)));
            if (!result.success()) {
                return;
            }
        }
        commands.execute(context(player, target, toolArgs));
    }

    private void dispatch(Player player, BlockPosition target, List<String> args) {
        commands.execute(context(player, target, args));
    }

    private CommandContext context(Player player, BlockPosition target, List<String> args) {
        BlockPosition origin = new BlockPosition(
                player.getWorld().getName(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
        return new CommandContext(
                actorOf(player),
                player.getWorld().getName(),
                origin,
                target,
                args,
                Set.of(
                        origin,
                        new BlockPosition(origin.worldId(), origin.x(), origin.y() + 1, origin.z())));
    }

    /**
     * The block the player is looking at.
     *
     * @param player player
     * @return hit block, or {@code null} if nothing in range
     */
    private BlockPosition targetOf(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(limits.interactionDistance());
        Block block = hit != null ? hit.getHitBlock() : null;
        if (block == null) {
            return null;
        }
        return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    /**
     * The cell a vanilla block placement would occupy: the aimed block shifted one cell in the
     * direction of the hit face (placing on the adjacent face).
     *
     * @param player player
     * @return adjacent cell, or {@code null} if nothing in range
     */
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

    private BlockPosition shiftRightTarget(Player player) {
        ToolMode mode = sessions.session(actorOf(player)).mode();
        return mode == ToolMode.PASTE ? placementOf(player) : targetOf(player);
    }

    private BlockPosition rightClickTarget(Player player) {
        ToolMode mode = sessions.session(actorOf(player)).mode();
        return mode == ToolMode.PASTE ? placementOf(player) : targetOf(player);
    }

    private BlockPosition repeatTarget(Player player) {
        PlayerSession session = sessions.session(actorOf(player));
        return "paste".equals(session.lastTool()) ? placementOf(player) : targetOf(player);
    }

    private static ActorId actorOf(Player player) {
        return new ActorId(player.getUniqueId());
    }
}
