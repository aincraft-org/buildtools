package dev.mintychochip.masonry.common.command;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.blueprint.BlueprintMeta;
import dev.mintychochip.masonry.api.command.CommandContext;
import dev.mintychochip.masonry.api.command.CommandResult;
import dev.mintychochip.masonry.api.operation.OperationRecord;
import dev.mintychochip.masonry.api.preview.PreviewMode;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.service.PreviewRenderer;
import dev.mintychochip.masonry.api.service.SurvivalTransaction;
import dev.mintychochip.masonry.api.service.WorldAccess;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import dev.mintychochip.masonry.api.tool.ToolRequest;
import dev.mintychochip.masonry.api.tool.ValidationResult;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.common.blueprint.BlueprintManager;
import dev.mintychochip.masonry.common.operation.OperationGuard;
import dev.mintychochip.masonry.common.session.PlayerSession;
import dev.mintychochip.masonry.common.session.PlayerSessionStore;
import dev.mintychochip.masonry.common.tool.ToolExecutor;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
/**
 * Platform-neutral {@code /masonry} dispatcher. Paper's {@code MasonryBrigadierCommand} builds a
 * {@link CommandContext} and calls {@link #execute(CommandContext)}.
 */
public final class MasonryCommands {
    private final PlayerSessionStore sessions;
    private final OperationGuard guard;
    private final ToolExecutor executor;
    private final BlueprintManager blueprints;
    private final PreviewRenderer previews;
    private final WorldAccess world;
    private final SurvivalTransaction survival;

    /**
     * @param world world
     * @param survival inventory
     */
    public MasonryCommands(
            PlayerSessionStore sessions,
            OperationGuard guard,
            ToolExecutor executor,
            BlueprintManager blueprints,
            PreviewRenderer previews,
            WorldAccess world,
            SurvivalTransaction survival) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.guard = Objects.requireNonNull(guard, "guard");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.blueprints = Objects.requireNonNull(blueprints, "blueprints");
        this.previews = Objects.requireNonNull(previews, "previews");
        this.world = Objects.requireNonNull(world, "world");
        this.survival = Objects.requireNonNull(survival, "survival");
    }

    /**
     * Dispatches {@code pos1}, {@code pos2}, {@code replace}, {@code fill}, {@code copy},
     * {@code paste}, {@code undo}, {@code redo}, and {@code blueprint}.
     *
     * @param context resolved invocation
     * @return player-facing result (validation and execute content, not merely a handler stub)
     */
    public CommandResult execute(CommandContext context) {
        Objects.requireNonNull(context, "context");
        if (context.arguments().isEmpty()) {
            return CommandResult.fail("Usage: /masonry <pos1|pos2|replace|fill|survival_fill|copy|paste|undo|redo|blueprint|wand|previewmode|animation>");
        }
        String command = context.argument(0).toLowerCase();
        return switch (command) {
            case "pos1" -> setCorner(context, 1);
            case "pos2" -> setCorner(context, 2);
            case "replace" -> replace(context);
            case "fill" -> fill(context, false);
            case "survival_fill" -> fill(context, true);
            case "copy" -> copy(context);
            case "paste" -> paste(context);
            case "cut" -> runTool(context, "cut", Map.of());
            case "move" -> runTool(context, "move", Map.of());
            case "repeat" -> repeat(context);
            case "undo" -> undo(context);
            case "redo" -> redo(context);
            case "blueprint", "bp" -> blueprint(context);
            case "previewmode" -> previewMode(context);
            case "animation", "anim" -> previewAnimation(context);
            default -> CommandResult.fail("Unknown subcommand: " + command);
        };
    }

    private CommandResult setCorner(CommandContext context, int corner) {
        BlockPosition target = resolveCoordinate(context, 1);
        if (target == null) {
            target = context.target();
        }
        if (target == null) {
            return CommandResult.fail("No target block in range");
        }
        ValidationResult interaction = guard.validateInteraction(context.origin(), target);
        if (!interaction.valid()) {
            return CommandResult.invalid(interaction);
        }
        PlayerSession session = sessions.session(context.actorId());
        if (corner == 1) {
            session.setPos1(target);
        } else {
            session.setPos2(target);
        }
        Optional<CuboidSelection> selection = session.selection();
        if (selection.isPresent()) {
            ValidationResult extent = guard.validateSelection(selection.get());
            if (!extent.valid()) {
                return CommandResult.invalid(extent);
            }
            previews.showSelection(context.actorId(), selection.get());
            return CommandResult.ok("Set pos" + corner + " to " + format(target)
                    + " (" + selection.get().volume() + " blocks)");
        }
        return CommandResult.ok("Set pos" + corner + " to " + format(target));
    }

    private CommandResult replace(CommandContext context) {
        if (context.arguments().size() < 3) {
            return CommandResult.fail("Usage: /masonry replace <from> <to>");
        }
        return runTool(context, "replace", Map.of(
                "from", context.argument(1),
                "to", context.argument(2)));
    }
    private CommandResult fill(CommandContext context, boolean soft) {
        if (context.arguments().size() < 2) {
            return CommandResult.fail("Usage: /masonry fill <block>");
        }
        return runTool(context, soft ? "survival_fill" : "fill", Map.of("block", context.argument(1)));
    }

    private CommandResult copy(CommandContext context) {
        return runTool(context, "copy", Map.of());
    }

    /**
     * Re-runs the last executed tool with its recorded arguments against the current
     * selection. Nothing to repeat if no tool has run for this actor yet.
     *
     * @param context invocation with an active selection
     * @return result of the re-run, or a clear refusal
     */
    private CommandResult repeat(CommandContext context) {
        PlayerSession session = sessions.session(context.actorId());
        String toolName = session.lastTool();
        if (toolName == null) {
            return CommandResult.fail("Nothing to repeat");
        }
        Optional<CuboidSelection> selection = session.selection();
        if (selection.isEmpty()) {
            return CommandResult.fail("A valid selection is required");
        }
        return runTool(context, toolName, session.lastArgs());
    }

    private CommandResult paste(CommandContext context) {
        PlayerSession session = sessions.session(context.actorId());
        if (session.clipboard().isEmpty()) {
            return CommandResult.fail("Clipboard is empty");
        }
        BlockPosition origin = session.pos1().orElse(context.target());
        if (origin == null) {
            origin = context.origin();
        }
        CuboidSelection pasteOrigin = new CuboidSelection(origin, origin);
        ToolRequest request = new ToolRequest(
                context.actorId(),
                "paste",
                pasteOrigin,
                Map.of(),
                session.clipboard().orElseThrow(),
                context.excludedPositions());
        return dispatch(request, "Pasted " + session.clipboard().orElseThrow().size() + " blocks");
    }

    private CommandResult undo(CommandContext context) {
        Optional<OperationRecord> undone = executor.undo(context.actorId(), world, survival);
        if (undone.isEmpty()) {
            return CommandResult.fail("Nothing to undo");
        }
        return CommandResult.executed(
                "Undid " + undone.get().toolName() + " (" + undone.get().changes().size() + " blocks)",
                null,
                undone.get());
    }

    private CommandResult redo(CommandContext context) {
        Optional<OperationRecord> redone = executor.redo(context.actorId(), world, survival);
        if (redone.isEmpty()) {
            return CommandResult.fail("Nothing to redo");
        }
        return CommandResult.executed(
                "Redid " + redone.get().toolName() + " (" + redone.get().changes().size() + " blocks)",
                null,
                redone.get());
    }

    private CommandResult blueprint(CommandContext context) {
        if (context.arguments().size() < 2) {
            return CommandResult.fail("Usage: /masonry blueprint <save|load|list|delete>");
        }
        String action = context.argument(1).toLowerCase();
        return switch (action) {
            case "save" -> saveBlueprint(context);
            case "load" -> loadBlueprint(context);
            case "list" -> listBlueprints(context);
            case "delete" -> deleteBlueprint(context);
            default -> CommandResult.fail("Usage: /masonry blueprint <save|load|list|delete>");
        };
    }

    private CommandResult saveBlueprint(CommandContext context) {
        if (context.arguments().size() < 3) {
            return CommandResult.fail("Usage: /masonry blueprint save <name>");
        }
        String name = context.argument(2);
        PlayerSession session = sessions.session(context.actorId());
        if (session.clipboard().isEmpty()) {
            CommandResult copied = copy(context);
            if (!copied.success()) {
                return copied;
            }
        }
        if (session.clipboard().isEmpty()) {
            return CommandResult.fail("Nothing to save");
        }
        try {
            blueprints.save(context.actorId(), name, session.clipboard().orElseThrow());
        } catch (IllegalArgumentException exception) {
            return CommandResult.fail(exception.getMessage());
        }
        return CommandResult.ok("Saved blueprint '" + name + "'");
    }

    private CommandResult loadBlueprint(CommandContext context) {
        if (context.arguments().size() < 3) {
            return CommandResult.fail("Usage: /masonry blueprint load <name>");
        }
        String name = context.argument(2);
        if (blueprints.loadToClipboard(context.actorId(), name).isEmpty()) {
            return CommandResult.fail("Unknown blueprint '" + name + "'");
        }
        return CommandResult.ok("Loaded blueprint '" + name + "'");
    }

    private CommandResult listBlueprints(CommandContext context) {
        List<BlueprintMeta> metas = blueprints.list(context.actorId());
        if (metas.isEmpty()) {
            return CommandResult.ok("No blueprints");
        }
        String names = metas.stream().map(BlueprintMeta::name).collect(Collectors.joining(", "));
        return CommandResult.ok("Blueprints: " + names);
    }

    private CommandResult deleteBlueprint(CommandContext context) {
        if (context.arguments().size() < 3) {
            return CommandResult.fail("Usage: /masonry blueprint delete <name>");
        }
        String name = context.argument(2);
        if (!blueprints.delete(context.actorId(), name)) {
            return CommandResult.fail("Unknown blueprint '" + name + "'");
        }
        return CommandResult.ok("Deleted blueprint '" + name + "'");
    }

    private CommandResult runTool(CommandContext context, String toolName, Map<String, String> arguments) {
        Optional<CuboidSelection> selection = sessions.session(context.actorId()).selection();
        if (selection.isEmpty()) {
            return CommandResult.fail("A valid selection is required");
        }
        ToolRequest request = new ToolRequest(
                context.actorId(),
                toolName,
                selection.get(),
                arguments,
                sessions.clipboard(context.actorId()).orElse(null),
                context.excludedPositions());
        String ok = switch (toolName) {
            case "copy" -> "Copied " + selection.get().volume() + " blocks";
            case "fill" -> "Filled";
            case "survival_fill" -> "Soft-filled";
            case "replace" -> "Replaced matching blocks";
            default -> "Ran " + toolName;
        };
        return dispatch(request, ok);
    }

    private CommandResult dispatch(ToolRequest request, String successMessage) {
        ValidationResult validation = executor.validate(request, world, survival);
        if (!validation.valid()) {
            return CommandResult.invalid(validation);
        }
        ToolPreview preview = executor.preview(request, world);
        previews.show(request.actorId(), preview);
        Optional<OperationRecord> record = executor.execute(request, world, survival);
        if (record.isEmpty()) {
            return CommandResult.fail("Execute refused");
        }
        String detail = switch (request.toolName()) {
            case "fill", "survival_fill", "replace" -> " (" + preview.affectedCount()
                    + " of " + request.selection().volume() + " cells)";
            default -> " (" + preview.affectedCount() + " affected)";
        };
        // The action is done; drop the selection corners and its preview so the player must
        // pick fresh pos1/pos2 for the next operation.
        PlayerSession session = sessions.session(request.actorId());
        session.setLastTool(request.toolName(), request.arguments());
        session.clearSelection();
        previews.clear(request.actorId());
        return CommandResult.executed(successMessage + detail, preview, record.get());
    }

    private static BlockPosition resolveCoordinate(CommandContext context, int startIndex) {
        if (context.arguments().size() < startIndex + 3) {
            return null;
        }
        try {
            int x = Integer.parseInt(context.argument(startIndex));
            int y = Integer.parseInt(context.argument(startIndex + 1));
            int z = Integer.parseInt(context.argument(startIndex + 2));
            return new BlockPosition(context.worldId(), x, y, z);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String format(BlockPosition position) {
        return position.x() + "," + position.y() + "," + position.z();
    }

    private CommandResult previewMode(CommandContext context) {
        if (context.arguments().size() < 2) {
            return CommandResult.fail("Usage: /masonry previewmode <" + java.util.Arrays.stream(PreviewMode.values())
                    .map(m -> m.name().toLowerCase(java.util.Locale.ROOT))
                    .collect(Collectors.joining("|")) + ">");
        }
        String name = context.argument(1).toUpperCase(java.util.Locale.ROOT);
        PreviewMode mode;
        try {
            mode = PreviewMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return CommandResult.fail("Unknown preview mode: " + context.argument(1));
        }
        PlayerSession session = sessions.session(context.actorId());
        session.setPreviewMode(mode);
        previews.clear(context.actorId());
        session.selection().ifPresent(selection -> previews.showSelection(context.actorId(), selection));
        return CommandResult.ok("Preview mode: " + mode.name().toLowerCase(java.util.Locale.ROOT));
    }

    private CommandResult previewAnimation(CommandContext context) {
        PlayerSession session = sessions.session(context.actorId());
        if (context.arguments().size() < 2) {
            session.setPreviewAnimation(!session.previewAnimation());
        } else {
            String arg = context.argument(1).toLowerCase(java.util.Locale.ROOT);
            switch (arg) {
                case "on", "true", "enabled", "1" -> session.setPreviewAnimation(true);
                case "off", "false", "disabled", "0" -> session.setPreviewAnimation(false);
                case "toggle" -> session.setPreviewAnimation(!session.previewAnimation());
                default -> { return CommandResult.fail("Usage: /masonry animation [on|off|toggle]"); }
            }
        }
        previews.clear(context.actorId());
        session.selection().ifPresent(selection -> previews.showSelection(context.actorId(), selection));
        return CommandResult.ok("Preview animation: " + (session.previewAnimation() ? "on" : "off"));
    }
}
