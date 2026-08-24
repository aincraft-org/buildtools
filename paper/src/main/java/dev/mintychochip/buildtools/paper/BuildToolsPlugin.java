package dev.mintychochip.buildtools.paper;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.limits.OperationLimits;
import dev.mintychochip.buildtools.common.blueprint.BlueprintManager;
import dev.mintychochip.buildtools.common.blueprint.FileBlueprintStore;
import dev.mintychochip.buildtools.common.command.BuildToolsCommands;
import dev.mintychochip.buildtools.common.operation.OperationGuard;
import dev.mintychochip.buildtools.common.operation.OperationHistory;
import dev.mintychochip.buildtools.common.session.PlayerSessionStore;
import dev.mintychochip.buildtools.common.tool.CopyTool;
import dev.mintychochip.buildtools.common.tool.FillTool;
import dev.mintychochip.buildtools.common.tool.PasteTool;
import dev.mintychochip.buildtools.common.tool.ReplaceTool;
import dev.mintychochip.buildtools.common.tool.SurvivalFillTool;
import dev.mintychochip.buildtools.common.tool.ToolExecutor;
import dev.mintychochip.buildtools.common.tool.ToolRegistry;
import dev.mintychochip.buildtools.paper.adapter.PaperPermissionService;
import dev.mintychochip.buildtools.paper.adapter.PaperPreviewRenderer;
import dev.mintychochip.buildtools.paper.adapter.PaperSurvivalTransaction;
import dev.mintychochip.buildtools.paper.adapter.PaperTaskScheduler;
import dev.mintychochip.buildtools.paper.adapter.PaperWorldAccess;
import dev.mintychochip.buildtools.paper.command.BuildToolsBrigadierCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper entry point. Wires {@code api}/{@code common} ports, registers replace/fill/copy/paste,
 * and binds {@code /bt} via Brigadier.
 */
public final class BuildToolsPlugin extends JavaPlugin implements Listener {
    private ToolRegistry toolRegistry;
    private ToolExecutor toolExecutor;
    private OperationHistory history;
    private PaperPreviewRenderer previewRenderer;
    private PaperWorldAccess worldAccess;
    private PaperSurvivalTransaction survivalTransaction;
    private PaperTaskScheduler taskScheduler;
    private PlayerSessionStore sessions;

    @Override
    public void onEnable() {
        OperationLimits limits = OperationLimits.defaults();
        this.sessions = new PlayerSessionStore();
        this.toolRegistry = new ToolRegistry();
        this.toolRegistry.register(new ReplaceTool());
        this.toolRegistry.register(new FillTool());
        this.toolRegistry.register(new SurvivalFillTool());
        this.toolRegistry.register(new CopyTool(sessions));
        this.toolRegistry.register(new PasteTool());
        this.history = new OperationHistory(20);
        this.toolExecutor = new ToolExecutor(
                toolRegistry, history, new OperationGuard(limits), new PaperPermissionService(getServer()));
        this.previewRenderer = new PaperPreviewRenderer(this, sessions);
        this.worldAccess = new PaperWorldAccess(getServer());
        this.survivalTransaction = new PaperSurvivalTransaction(getServer());
        this.taskScheduler = new PaperTaskScheduler(this);
        BlueprintManager blueprints =
                new BlueprintManager(new FileBlueprintStore(getDataFolder().toPath().resolve("blueprints")), sessions);
        BuildToolsCommands commands = new BuildToolsCommands(
                sessions,
                new OperationGuard(limits),
                toolExecutor,
                blueprints,
                previewRenderer,
                worldAccess,
                survivalTransaction);

        LifecycleEventManager<org.bukkit.plugin.Plugin> manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            LiteralCommandNode<CommandSourceStack> node = new BuildToolsBrigadierCommand(commands, limits, this).tree();
            event.registrar().register(node, "BuildTools command root", List.of());
        });

        GadgetListener gadgetListener = new GadgetListener(
                this, commands, sessions, limits, worldAccess, survivalTransaction);
        getServer().getPluginManager().registerEvents(gadgetListener, this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BuildTools loaded.");
    }

    @Override
    public void onDisable() {
        this.toolRegistry = null;
        this.toolExecutor = null;
        this.history = null;
        this.previewRenderer = null;
        this.worldAccess = null;
        this.survivalTransaction = null;
        this.taskScheduler = null;
        this.sessions = null;
        getLogger().info("BuildTools disabled.");
    }

    /**
     * Clears preview entities and session state so logout cannot leak display entities.
     *
     * @param event quit
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ActorId actor = new ActorId(event.getPlayer().getUniqueId());
        if (previewRenderer != null) {
            previewRenderer.clear(actor);
        }
        if (sessions != null) {
            sessions.remove(actor);
        }
    }
}
