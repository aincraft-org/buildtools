package dev.mintychochip.masonry.common.support;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.command.CommandContext;
import dev.mintychochip.masonry.api.limits.OperationLimits;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.common.blueprint.BlueprintManager;
import dev.mintychochip.masonry.common.blueprint.MemoryBlueprintStore;
import dev.mintychochip.masonry.common.command.MasonryCommands;
import dev.mintychochip.masonry.api.service.TaskScheduler;
import dev.mintychochip.masonry.common.operation.OperationGuard;
import dev.mintychochip.masonry.common.operation.OperationHistory;
import dev.mintychochip.masonry.common.session.PlayerSessionStore;
import dev.mintychochip.masonry.common.tool.CopyTool;
import dev.mintychochip.masonry.common.tool.FillTool;
import dev.mintychochip.masonry.common.tool.PasteTool;
import dev.mintychochip.masonry.common.tool.ReplaceTool;
import dev.mintychochip.masonry.common.tool.ToolExecutor;
import dev.mintychochip.masonry.common.tool.ToolRegistry;
import java.util.Arrays;
import java.util.UUID;

/**
 * Wires the shipped command dispatcher, tools, and in-memory ports for JVM tests.
 */
public final class TestHarness {
    /** Default test actor. */
    public static final ActorId ACTOR = new ActorId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
    /** World id used by {@link #pos(int, int, int)}. */
    public static final String WORLD = "world";

    public final PlayerSessionStore sessions = new PlayerSessionStore();
    public final InMemoryWorldAccess world = new InMemoryWorldAccess();
    public final InMemorySurvival survival = new InMemorySurvival();
    public final MapPermissions permissions = new MapPermissions();
    public final RecordingPreviewRenderer previews = new RecordingPreviewRenderer();
    public final MemoryBlueprintStore store = new MemoryBlueprintStore();
    public final OperationLimits limits;
    public final OperationGuard guard;
    public final ToolRegistry registry = new ToolRegistry();
    public final OperationHistory history;
    public final ToolExecutor executor;
    public final BlueprintManager blueprints;
    public final MasonryCommands commands;

    /** Uses {@link OperationLimits#defaults()}. */
    public TestHarness() {
        this(OperationLimits.defaults());
    }

    /**
     * @param limits limits for the guard
     */
    public TestHarness(OperationLimits limits) {
        this.limits = limits;
        this.guard = new OperationGuard(limits);
        this.history = new OperationHistory(32);
        this.registry.register(new ReplaceTool());
        this.registry.register(new FillTool());
        this.registry.register(new CopyTool(sessions));
        this.registry.register(new PasteTool());
        this.executor = new ToolExecutor(registry, history, guard, permissions);
        this.blueprints = new BlueprintManager(store, sessions);
        this.commands = new MasonryCommands(sessions, guard, executor, blueprints, previews,
                world, survival);
    }

    /**
     * @param origin player origin
     * @param target look target
     * @param args including the subcommand
     * @return context for {@link #ACTOR}
     */
    public CommandContext command(BlockPosition origin, BlockPosition target, String... args) {
        return new CommandContext(ACTOR, WORLD, origin, target, Arrays.asList(args));
    }

    /**
     * @param x X
     * @param y Y
     * @param z Z
     * @return position in {@link #WORLD}
     */
    public BlockPosition pos(int x, int y, int z) {
        return new BlockPosition(WORLD, x, y, z);
    }
}
