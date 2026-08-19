package dev.mintychochip.buildtools.paper.command;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.command.CommandContext;
import dev.mintychochip.buildtools.api.command.CommandResult;
import dev.mintychochip.buildtools.api.limits.OperationLimits;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.common.command.BuildToolsCommands;
import java.util.Arrays;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public final class BuildToolsCommand implements CommandExecutor {
    private final BuildToolsCommands commands;
    private final OperationLimits limits;

    public BuildToolsCommand(BuildToolsCommands commands, OperationLimits limits) {
        this.commands = Objects.requireNonNull(commands, "commands");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("BuildTools commands are player-only.", NamedTextColor.RED));
            return true;
        }
        CommandResult result = commands.execute(toContext(player, args));
        sender.sendMessage(Component.text(
                result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
        return true;
    }

    CommandContext toContext(Player player, String[] args) {
        BlockPosition origin = new BlockPosition(
                player.getWorld().getName(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
        BlockPosition target = targetOf(player);
        return new CommandContext(
                new ActorId(player.getUniqueId()),
                player.getWorld().getName(),
                origin,
                target,
                Arrays.asList(args));
    }

    private BlockPosition targetOf(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(limits.interactionDistance());
        Block block = hit != null ? hit.getHitBlock() : player.getLocation().getBlock();
        if (block == null) {
            return new BlockPosition(
                    player.getWorld().getName(),
                    player.getLocation().getBlockX(),
                    player.getLocation().getBlockY(),
                    player.getLocation().getBlockZ());
        }
        return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
}
