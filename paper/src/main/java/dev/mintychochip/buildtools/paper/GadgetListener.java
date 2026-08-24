package dev.mintychochip.buildtools.paper;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.command.CommandContext;
import dev.mintychochip.buildtools.api.command.CommandResult;
import dev.mintychochip.buildtools.api.limits.OperationLimits;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.common.command.BuildToolsCommands;
import dev.mintychochip.buildtools.common.session.PlayerSessionStore;
import dev.mintychochip.buildtools.common.session.ToolMode;
import dev.mintychochip.buildtools.paper.adapter.GadgetItem;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

public final class GadgetListener implements Listener {
    private final JavaPlugin plugin;
    private final BuildToolsCommands commands;
    private final PlayerSessionStore sessions;
    private final OperationLimits limits;
    private final WorldAccess world;
    private final SurvivalTransaction survival;

    public GadgetListener(
            JavaPlugin plugin,
            BuildToolsCommands commands,
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
        if (!player.hasPermission("buildtools.command")) {
            return;
        }

        event.setCancelled(true);
        Action action = event.getAction();
        boolean sneaking = player.isSneaking();

        if (sneaking && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            cycleMode(player);
            return;
        }

        if (sneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            handleShiftRightClick(player, targetOf(player));
            return;
        }

        if (!sneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            handleRightClick(player, targetOf(player));
        }
    }

    private void cycleMode(Player player) {
        ToolMode next = sessions.session(actorOf(player)).mode().next();
        sessions.session(actorOf(player)).setMode(next);
        player.sendActionBar(Component.text("BuildTools mode: " + next.name().toLowerCase(), NamedTextColor.GREEN));
    }

    private void handleShiftRightClick(Player player, BlockPosition target) {
        if (target == null) {
            player.sendActionBar(Component.text("No target block", NamedTextColor.RED));
            return;
        }
        ToolMode mode = sessions.session(actorOf(player)).mode();
        if (mode == ToolMode.PASTE) {
            sessions.session(actorOf(player)).setPos1(target);
            dispatch(player, target, List.of("paste"));
            return;
        }
        dispatch(player, target, List.of("pos1"));
    }

    private void handleRightClick(Player player, BlockPosition target) {
        if (target == null) {
            player.sendActionBar(Component.text("No target block", NamedTextColor.RED));
            return;
        }
        ToolMode mode = sessions.session(actorOf(player)).mode();
        switch (mode) {
            case FILL -> handleFill(player, target);
            case REPLACE -> handleReplace(player, target);
            case COPY -> dispatch(player, target, List.of("pos2"), List.of("copy"));
            default -> player.sendActionBar(Component.text("Use shift-right-click to set pos1", NamedTextColor.RED));
        }
    }

    private void handleFill(Player player, BlockPosition target) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType().isAir() || !offhand.getType().isBlock()) {
            player.sendActionBar(Component.text("Hold a block in your offhand to fill", NamedTextColor.RED));
            return;
        }
        if (sessions.session(actorOf(player)).pos1().isEmpty()) {
            player.sendActionBar(Component.text("Set pos1 with shift-right-click first", NamedTextColor.RED));
            return;
        }
        String material = "minecraft:" + offhand.getType().getKey().getKey();
        dispatch(player, target, List.of("pos2"), List.of("survival_fill", material));
    }

    private void handleReplace(Player player, BlockPosition target) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType().isAir() || !offhand.getType().isBlock()) {
            player.sendActionBar(Component.text("Hold a block in your offhand for replace-to", NamedTextColor.RED));
            return;
        }
        if (sessions.session(actorOf(player)).pos1().isEmpty()) {
            player.sendActionBar(Component.text("Set pos1 with shift-right-click first", NamedTextColor.RED));
            return;
        }
        BlockPosition pos1 = sessions.session(actorOf(player)).pos1().orElseThrow();
        String from = world.getBlock(pos1).namespacedKey();
        String to = "minecraft:" + offhand.getType().getKey().getKey();
        dispatch(player, target, List.of("pos2"), List.of("replace", from, to));
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
