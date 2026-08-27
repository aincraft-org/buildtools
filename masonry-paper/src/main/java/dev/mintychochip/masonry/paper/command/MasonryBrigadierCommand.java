package dev.mintychochip.masonry.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.command.CommandResult;
import dev.mintychochip.masonry.api.limits.OperationLimits;
import dev.mintychochip.masonry.api.preview.PreviewMode;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.common.command.MasonryCommands;
import dev.mintychochip.masonry.paper.adapter.GadgetItem;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

/**
 * Brigadier command tree for {@code /masonry}. Parses typed arguments where safe,
 * preserves the common command layer's accepted string syntax for block states,
 * and provides tab completion for subcommands, block names, blueprint actions,
 * and preview modes.
 */
public final class MasonryBrigadierCommand {

    private final MasonryCommands commands;
    private final OperationLimits limits;
    private final JavaPlugin plugin;

    public MasonryBrigadierCommand(MasonryCommands commands, OperationLimits limits, JavaPlugin plugin) {
        this.commands = Objects.requireNonNull(commands, "commands");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public LiteralCommandNode<CommandSourceStack> tree() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("masonry")
                .requires(source -> source.getSender() instanceof Player && source.getSender().hasPermission("masonry.command"));

        root.then(Commands.literal("wand").executes(this::wand));
        root.then(posCorner("pos1", 1));
        root.then(posCorner("pos2", 2));
        root.then(replace());
        root.then(fill("fill"));
        root.then(fill("survival_fill"));
        root.then(Commands.literal("copy").executes(ctx -> execute(ctx, List.of("copy"))));
        root.then(Commands.literal("paste").executes(ctx -> execute(ctx, List.of("paste"))));
        root.then(Commands.literal("undo").executes(ctx -> execute(ctx, List.of("undo"))));
        root.then(Commands.literal("redo").executes(ctx -> execute(ctx, List.of("redo"))));
        root.then(blueprint("blueprint"));
        root.then(blueprint("bp"));
        root.then(previewMode());
        root.then(animation());

        return root.build();
    }

    private LiteralArgumentBuilder<CommandSourceStack> posCorner(String name, int corner) {
        return Commands.literal(name)
                .executes(ctx -> executeCorner(ctx, corner, null))
                .then(Commands.argument("pos", ArgumentTypes.blockPosition())
                        .executes(ctx -> executeCorner(ctx, corner, resolveBlockPosition(ctx, "pos"))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> replace() {
        return Commands.literal("replace")
                .then(Commands.argument("from", StringArgumentType.word())
                        .suggests(blockStateSuggestions())
                        .then(Commands.argument("to", StringArgumentType.word())
                                .suggests(blockStateSuggestions())
                                .executes(ctx -> execute(ctx, List.of(
                                        "replace",
                                        StringArgumentType.getString(ctx, "from"),
                                        StringArgumentType.getString(ctx, "to"))))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> fill(String name) {
        return Commands.literal(name)
                .then(Commands.argument("block", StringArgumentType.word())
                        .suggests(blockStateSuggestions())
                        .executes(ctx -> execute(ctx, List.of(name, StringArgumentType.getString(ctx, "block")))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> blueprint(String name) {
        return Commands.literal(name)
                .then(Commands.argument("action", StringArgumentType.word())
                        .suggests(blueprintActions())
                        .executes(ctx -> execute(ctx, List.of(name, StringArgumentType.getString(ctx, "action"))))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> execute(ctx, List.of(
                                        name,
                                        StringArgumentType.getString(ctx, "action"),
                                        StringArgumentType.getString(ctx, "name"))))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> previewMode() {
        return Commands.literal("previewmode")
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests(previewModeSuggestions())
                        .executes(ctx -> execute(ctx, List.of(
                                "previewmode",
                                StringArgumentType.getString(ctx, "mode")))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> animation() {
        return Commands.literal("animation")
                .executes(ctx -> execute(ctx, List.of("animation")))
                .then(Commands.argument("state", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            for (String s : List.of("on", "off", "toggle")) b.suggest(s);
                            return b.buildFuture();
                        })
                        .executes(ctx -> execute(ctx, List.of("animation", StringArgumentType.getString(ctx, "state")))));
    }

    private int execute(CommandContext<CommandSourceStack> ctx, List<String> args) {
        Player player = (Player) ctx.getSource().getSender();
        dev.mintychochip.masonry.api.command.CommandContext domain =
                domainContext(player, args, null);
        CommandResult result = commands.execute(domain);
        send(player, result);
        return result.success() ? Command.SINGLE_SUCCESS : 0;
    }

    private int executeCorner(CommandContext<CommandSourceStack> ctx, int corner, BlockPosition explicit) {
        Player player = (Player) ctx.getSource().getSender();
        BlockPosition target = explicit != null ? explicit : targetOf(player);
        List<String> args = explicit != null
                ? List.of("pos" + corner, String.valueOf(explicit.x()), String.valueOf(explicit.y()), String.valueOf(explicit.z()))
                : List.of("pos" + corner);
        dev.mintychochip.masonry.api.command.CommandContext domain =
                domainContext(player, args, target);
        CommandResult result = commands.execute(domain);
        send(player, result);
        return result.success() ? Command.SINGLE_SUCCESS : 0;
    }

    private int wand(CommandContext<CommandSourceStack> ctx) {
        Player player = (Player) ctx.getSource().getSender();
        player.getInventory().addItem(GadgetItem.create(plugin));
        player.sendMessage(Component.text("Given Masonry Gadget.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private dev.mintychochip.masonry.api.command.CommandContext domainContext(
            Player player, List<String> args, BlockPosition overrideTarget) {
        Location feet = player.getLocation();
        BlockPosition origin = new BlockPosition(
                player.getWorld().getName(),
                feet.getBlockX(),
                feet.getBlockY(),
                feet.getBlockZ());
        BlockPosition target = overrideTarget != null ? overrideTarget : targetOf(player);
        return new dev.mintychochip.masonry.api.command.CommandContext(
                new ActorId(player.getUniqueId()),
                player.getWorld().getName(),
                origin,
                target,
                args);
    }

    private BlockPosition targetOf(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(limits.interactionDistance());
        Block block = hit != null ? hit.getHitBlock() : null;
        if (block == null) {
            Location feet = player.getLocation();
            return new BlockPosition(
                    player.getWorld().getName(),
                    feet.getBlockX(),
                    feet.getBlockY(),
                    feet.getBlockZ());
        }
        return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private static BlockPosition resolveBlockPosition(CommandContext<CommandSourceStack> ctx, String name) {
        BlockPositionResolver resolver = ctx.getArgument(name, BlockPositionResolver.class);
        try {
            io.papermc.paper.math.BlockPosition pos = resolver.resolve(ctx.getSource());
            return new BlockPosition(worldOf(ctx), pos.blockX(), pos.blockY(), pos.blockZ());
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String worldOf(CommandContext<CommandSourceStack> ctx) {
        World world = ctx.getSource().getLocation().getWorld();
        return world != null ? world.getName() : "";
    }

    private static void send(Player player, CommandResult result) {
        player.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private static SuggestionProvider<CommandSourceStack> blueprintActions() {
        return (ctx, builder) -> {
            Stream.of("save", "load", "list", "delete").forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> previewModeSuggestions() {
        return (ctx, builder) -> {
            for (PreviewMode mode : PreviewMode.values()) {
                builder.suggest(mode.name().toLowerCase(Locale.ROOT));
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> blockStateSuggestions() {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            Registry.BLOCK.stream()
                    .map(BlockType::getKey)
                    .map(key -> key.toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(remaining) || name.contains(remaining))
                    .limit(64)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
