package dev.mintychochip.masonry.paper;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.clipboard.BlockOffset;
import dev.mintychochip.masonry.api.command.CommandContext;
import dev.mintychochip.masonry.api.command.CommandResult;
import dev.mintychochip.masonry.api.limits.OperationLimits;
import dev.mintychochip.masonry.api.service.SurvivalTransaction;
import dev.mintychochip.masonry.api.service.WorldAccess;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.common.command.MasonryCommands;
import dev.mintychochip.masonry.common.session.ExtensionPlan;
import dev.mintychochip.masonry.common.session.PlayerSession;
import dev.mintychochip.masonry.common.session.PlayerSessionStore;
import dev.mintychochip.masonry.common.session.ToolMode;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
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
import dev.mintychochip.masonry.api.world.BlockState;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
     * Uses the ordinary brick item as an extension-mode token. The aimed block supplies the
     * material; matching placeable items are charged from the player's inventory.
     */
    @EventHandler
    public void onBrickInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("masonry.tool.extend")
                || !GadgetItem.isExtensionToken(player.getInventory().getItemInMainHand())) {
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            event.setCancelled(true);
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        long now = player.getWorld().getFullTime();
        PlayerSession session = sessions.session(actorOf(player));
        BlockFace clickedFace =
                event.getClickedBlock() == null ? null : event.getBlockFace();
        ExtensionPlan plan = currentExtensionPlan(player, session);
        if (player.isSneaking()) {
            if (plan != null) {
                ExtensionPlan clickedPlan = createExtensionPlan(player, plan.length(), now, clickedFace);
                if (clickedPlan != null) {
                    plan = clickedPlan.withWidth(plan.width(), now);
                }
            }
            commitPlan(player, plan);
            event.setCancelled(true);
            return;
        }
        if (plan == null) {
            plan = createExtensionPlan(player, 1, now, clickedFace);
            if (plan == null) {
                event.setCancelled(true);
                return;
            }
            session.setExtensionPlan(plan);
            player.sendActionBar(Component.text("Extension: 1 x 1", NamedTextColor.AQUA));
        } else {
            // Retarget to the clicked face before growing by one row.
            ExtensionPlan clickedPlan = createExtensionPlan(player, plan.length(), now, clickedFace);
            if (clickedPlan != null) {
                plan = clickedPlan.withWidth(plan.width(), now);
            }
            plan = plan.withLength(
                    Math.min(limits.selectionExtent(), plan.length() + 1), now);
            session.setExtensionPlan(plan);
            player.sendActionBar(Component.text(
                    "Extension: " + plan.length() + " x " + plan.width(), NamedTextColor.AQUA));
        }
        event.setCancelled(true);
    }

    private void commitPlan(Player player, ExtensionPlan plan) {
        if (plan == null) {
            return;
        }
        CommandResult result = commands.execute(extensionContext(player, plan));
        if (result.success()) {
            sessions.session(actorOf(player)).clearExtensionPlan();
        } else {
            sessions.session(actorOf(player)).clearExtensionPlan();
            player.sendActionBar(Component.text(result.message(), NamedTextColor.RED));
        }
    }

    /**
     * Holding the brick selects extension mode. Sneak-scroll adjusts extension length; normal
     * scrolling remains vanilla hotbar selection. Larger jumps are treated as number-key slot
     * changes and fall through.
     */
    @EventHandler
    public void onExtensionScroll(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("masonry.tool.extend")) {
            return;
        }
        ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack nextItem = player.getInventory().getItem(event.getNewSlot());
        boolean previousBrick = GadgetItem.isExtensionToken(previousItem);
        boolean nextBrick = GadgetItem.isExtensionToken(nextItem);
        long now = player.getWorld().getFullTime();
        PlayerSession session = sessions.session(actorOf(player));

        if (!previousBrick) {
            if (nextBrick) {
                armExtensionPlan(player, now);
            }
            return;
        }

        int delta = slotDelta(event.getPreviousSlot(), event.getNewSlot());
        if (!player.isSneaking() || delta == 0) {
            if (nextBrick) {
                armExtensionPlan(player, now);
            } else {
                session.clearExtensionPlan();
            }
            return;
        }

        ExtensionPlan plan = session.extensionPlan().orElse(null);
        if (plan == null) {
            plan = createExtensionPlan(player, 1, now);
            if (plan == null) {
                return;
            }
        }
        plan = plan.withLengthDelta(delta, limits.selectionExtent(), now);
        session.setExtensionPlan(plan);
        event.setCancelled(true);
        player.sendActionBar(Component.text(
                "Extension: " + plan.length() + " x " + plan.width(), NamedTextColor.AQUA));
    }

    @EventHandler
    public void onExtensionJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("masonry.tool.extend")
                && GadgetItem.isExtensionToken(player.getInventory().getItemInMainHand())) {
            armExtensionPlan(player, player.getWorld().getFullTime());
        }
    }

    private void armExtensionPlan(Player player, long now) {
        ExtensionPlan plan = createExtensionPlan(player, 1, now);
        if (plan == null) {
            return;
        }
        sessions.session(actorOf(player)).setExtensionPlan(plan);
        player.sendActionBar(Component.text("Extension: 1 x 1", NamedTextColor.AQUA));
    }
    private ExtensionPlan currentExtensionPlan(Player player, PlayerSession session) {
        ExtensionPlan plan = session.extensionPlan().orElse(null);
        if (plan == null) {
            return null;
        }
        if (!matchesPlayer(player, plan)) {
            session.clearExtensionPlan();
            return null;
        }
        return plan;
    }

    private ExtensionPlan createExtensionPlan(Player player, int length, long now) {
        return createExtensionPlan(player, length, now, null);
    }

    private ExtensionPlan createExtensionPlan(
            Player player, int length, long now, BlockFace clickedFace) {
        RayTraceResult hit = player.rayTraceBlocks(limits.interactionDistance());
        Block block = hit != null ? hit.getHitBlock() : null;
        if (block == null) {
            return null;
        }
        BlockPosition aimed =
                new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        BlockState state = world.getBlock(aimed);
        if (state == null || state.isAir()) {
            return null;
        }
        BlockFace face = clickedFace != null ? clickedFace : hit.getHitBlockFace();
        var view = player.getLocation().getDirection();
        BlockOffset direction = extensionDirection(face, view.getX(), view.getZ());
        return new ExtensionPlan(aimed, direction, state, length, now);
    }

    static BlockOffset extensionDirection(BlockFace clickedFace, double viewX, double viewZ) {
        if (clickedFace != null
                && clickedFace.getModY() == 0
                && (clickedFace.getModX() != 0 || clickedFace.getModZ() != 0)) {
            return new BlockOffset(clickedFace.getModX(), 0, clickedFace.getModZ());
        }
        if (Math.abs(viewX) >= Math.abs(viewZ) && viewX != 0.0) {
            return new BlockOffset(Double.compare(viewX, 0.0), 0, 0);
        }
        if (viewZ != 0.0) {
            return new BlockOffset(0, 0, Double.compare(viewZ, 0.0));
        }
        return new BlockOffset(1, 0, 0);
    }

    private boolean matchesPlayer(Player player, ExtensionPlan plan) {
        return GadgetItem.isExtensionToken(player.getInventory().getItemInMainHand())
                && plan.anchor().worldId().equals(player.getWorld().getName());
    }

    private static int slotDelta(int previousSlot, int newSlot) {
        int delta = newSlot - previousSlot;
        if (delta > 4) {
            delta -= 9;
        } else if (delta < -4) {
            delta += 9;
        }
        return Math.abs(delta) == 1 ? delta : 0;
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
    private CommandContext extensionContext(Player player, ExtensionPlan plan) {
        BlockPosition body = new BlockPosition(
                player.getWorld().getName(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
        CuboidSelection region = plan.selection();
        return new CommandContext(
                actorOf(player),
                player.getWorld().getName(),
                plan.anchor(),
                region.max(),
                List.of("extend", plan.block().namespacedKey(),
                        Integer.toString(plan.length()), Integer.toString(plan.width()),
                        Integer.toString(plan.direction().x()),
                        Integer.toString(plan.direction().z())),
                Set.of(body, body.offset(0, 1, 0)));
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
