package dev.mintychochip.buildtools.common.support;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.command.CommandContext;
import dev.mintychochip.buildtools.api.limits.OperationLimits;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.common.blueprint.BlueprintManager;
import dev.mintychochip.buildtools.common.blueprint.MemoryBlueprintStore;
import dev.mintychochip.buildtools.common.command.BuildToolsCommands;
import dev.mintychochip.buildtools.common.operation.OperationGuard;
import dev.mintychochip.buildtools.common.operation.OperationHistory;
import dev.mintychochip.buildtools.common.session.PlayerSessionStore;
import dev.mintychochip.buildtools.common.tool.CopyTool;
import dev.mintychochip.buildtools.common.tool.FillTool;
import dev.mintychochip.buildtools.common.tool.PasteTool;
import dev.mintychochip.buildtools.common.tool.ReplaceTool;
import dev.mintychochip.buildtools.common.tool.ToolExecutor;
import dev.mintychochip.buildtools.common.tool.ToolRegistry;
import java.util.Arrays;
import java.util.UUID;

public final class TestHarness {
    public static final ActorId ACTOR = new ActorId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
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
    public final BuildToolsCommands commands;

    public TestHarness() {
        this(OperationLimits.defaults());
    }

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
        this.commands = new BuildToolsCommands(sessions, guard, executor, blueprints, previews, world, survival);
    }

    public CommandContext command(BlockPosition origin, BlockPosition target, String... args) {
        return new CommandContext(ACTOR, WORLD, origin, target, Arrays.asList(args));
    }

    public BlockPosition pos(int x, int y, int z) {
        return new BlockPosition(WORLD, x, y, z);
    }
}
