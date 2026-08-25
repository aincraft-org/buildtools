package dev.mintychochip.masonry.paper;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.command.CommandContext;
import dev.mintychochip.masonry.api.command.CommandResult;
import dev.mintychochip.masonry.api.limits.OperationLimits;
import dev.mintychochip.masonry.api.service.SurvivalTransaction;
import dev.mintychochip.masonry.api.service.WorldAccess;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.common.command.MasonryCommands;
import dev.mintychochip.masonry.common.session.PlayerSessionStore;
import dev.mintychochip.masonry.common.session.ToolMode;
import dev.mintychochip.masonry.paper.adapter.GadgetItem;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
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
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        if (!GadgetItem.isGadget(plugin, main)) {
            return;
        }
        if (!player.hasPermission("masonry.command")) {
            return;
        }

        Action action = event.getAction();
        boolean sneaking = player.isSneaking();
        boolean consumed = false;

        if (sneaking && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            consumed = cycleMode(player);
        } else if (sneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            consumed = handleShiftRightClick(player, targetOf(player));
        } else if (!sneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            consumed = handleRightClick(player, targetOf(player));
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

    private boolean handleShiftRightClick(Player player, BlockPosition target) {
        if (target == null) {
            player.sendActionBar(Component.text("No target block", NamedTextColor.RED));
            return true;
        }
        ToolMode mode = sessions.session(actorOf(player)).mode();
        if (mode == ToolMode.PASTE) {
            sessions.session(actorOf(player)).setPos1(target);
            dispatch(player, target, List.of("paste"));
            return true;
        }
        dispatch(player, target, List.of("pos1"));
        return true;
    }

    private boolean handleRightClick(Player player, BlockPosition target) {
        if (target == null) {
            player.sendActionBar(Component.text("No target block", NamedTextColor.RED));
            return true;
        }
        ToolMode mode = sessions.session(actorOf(player)).mode();
        return switch (mode) {
            case FILL -> handleFill(player, target);
            case REPLACE -> handleReplace(player, target);
            case COPY -> handleCopy(player, target);
            default -> false;
        };
    }

    private boolean handleFill(Player player, BlockPosition target) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType().isAir() || !offhand.getType().isBlock()) {
            player.sendActionBar(Component.text("Hold a block in your offhand to fill", NamedTextColor.RED));
            return true;
        }
        if (sessions.session(actorOf(player)).pos1().isEmpty()) {
            player.sendActionBar(Component.text("Set pos1 with shift-right-click first", NamedTextColor.RED));
            return true;
        }
        String material = "minecraft:" + offhand.getType().getKey().getKey();
        dispatch(player, target, List.of("pos2"), List.of("survival_fill", material));
        return true;
    }

    private boolean handleReplace(Player player, BlockPosition target) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType().isAir() || !offhand.getType().isBlock()) {
            player.sendActionBar(Component.text("Hold a block in your offhand for replace-to", NamedTextColor.RED));
            return true;
        }
        if (sessions.session(actorOf(player)).pos1().isEmpty()) {
            player.sendActionBar(Component.text("Set pos1 with shift-right-click first", NamedTextColor.RED));
            return true;
        }
        BlockPosition pos1 = sessions.session(actorOf(player)).pos1().orElseThrow();
        String from = world.getBlock(pos1).namespacedKey();
        String to = "minecraft:" + offhand.getType().getKey().getKey();
        dispatch(player, target, List.of("pos2"), List.of("replace", from, to));
        return true;
    }

    private boolean handleCopy(Player player, BlockPosition target) {
        if (sessions.session(actorOf(player)).pos1().isEmpty()) {
            player.sendActionBar(Component.text("Set pos1 with shift-right-click first", NamedTextColor.RED));
            return true;
        }
        dispatch(player, target, List.of("pos2"), List.of("copy"));
        return true;
    }

    private void dispatch(Player player, BlockPosition target, List<String> preArgs, List<String> toolArgs) {
        for (String pre : preArgs) {
            CommandResult result = commands.execute(context(player, target, List.of(pre)));
            if (!result.success()) {
                player.sendActionBar(Component.text(result.message(), NamedTextColor.RED));
                return;
            }
        }
        CommandResult result = commands.execute(context(player, target, toolArgs));
        player.sendActionBar(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void dispatch(Player player, BlockPosition target, List<String> args) {
        CommandResult result = commands.execute(context(player, target, args));
        player.sendActionBar(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private CommandContext context(Player player, BlockPosition target, List<String> args) {
        BlockPosition origin = new BlockPosition(
                player.getWorld().getName(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
        return new CommandContext(
                actorOf(player), player.getWorld().getName(), origin, target, args);
    }

    private BlockPosition targetOf(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(limits.interactionDistance());
        Block block = hit != null ? hit.getHitBlock() : null;
        if (block == null) {
            return null;
        }
        return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private static ActorId actorOf(Player player) {
        return new ActorId(player.getUniqueId());
    }
}
