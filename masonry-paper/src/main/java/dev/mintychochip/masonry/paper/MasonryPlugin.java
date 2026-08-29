package dev.mintychochip.masonry.paper;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.limits.OperationLimits;
import dev.mintychochip.masonry.common.blueprint.BlueprintManager;
import dev.mintychochip.masonry.common.blueprint.FileBlueprintStore;
import dev.mintychochip.masonry.common.command.MasonryCommands;
import dev.mintychochip.masonry.common.operation.OperationGuard;
import dev.mintychochip.masonry.common.operation.OperationHistory;
import dev.mintychochip.masonry.common.session.PlayerSessionStore;
import dev.mintychochip.masonry.common.tool.CopyTool;
import dev.mintychochip.masonry.common.tool.CutTool;
import dev.mintychochip.masonry.common.tool.FillTool;
import dev.mintychochip.masonry.common.tool.ExtendTool;
import dev.mintychochip.masonry.common.tool.MoveTool;
import dev.mintychochip.masonry.common.tool.PasteTool;
import dev.mintychochip.masonry.common.tool.ReplaceTool;
import dev.mintychochip.masonry.common.tool.SurvivalFillTool;
import dev.mintychochip.masonry.common.tool.ToolExecutor;
import dev.mintychochip.masonry.common.tool.ToolRegistry;
import dev.mintychochip.masonry.paper.adapter.PaperPermissionService;
import dev.mintychochip.masonry.paper.adapter.PaperPreviewRenderer;
import dev.mintychochip.masonry.paper.adapter.PaperSurvivalTransaction;
import dev.mintychochip.masonry.paper.adapter.PaperTaskScheduler;
import dev.mintychochip.masonry.paper.command.MasonryBrigadierCommand;
import dev.mintychochip.masonry.paper.integration.MasonryWorldAccess;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Paper entry point. Wires {@code api}/{@code common} ports, registers replace/fill/extend/copy/paste,
 * and binds {@code /masonry} via Brigadier.
 */
public final class MasonryPlugin extends JavaPlugin implements Listener {
    private ToolRegistry toolRegistry;
    private ToolExecutor toolExecutor;
    private OperationHistory history;
    private PaperPreviewRenderer previewRenderer;
    private MasonryWorldAccess worldAccess;
    private PaperSurvivalTransaction survivalTransaction;
    private PaperTaskScheduler taskScheduler;
    private HoverPreviewDriver hoverPreviews;
    private PlayerSessionStore sessions;
    private Team previewTeam;

    @Override
    public void onEnable() {
        OperationLimits limits = OperationLimits.defaults();
        this.sessions = new PlayerSessionStore();
        this.toolRegistry = new ToolRegistry();
        this.toolRegistry.register(new ReplaceTool());
        this.toolRegistry.register(new FillTool());
        this.toolRegistry.register(new ExtendTool());
        this.toolRegistry.register(new SurvivalFillTool());
        this.toolRegistry.register(new CopyTool(sessions));
        this.toolRegistry.register(new CutTool(sessions));
        this.toolRegistry.register(new MoveTool(sessions));
        this.toolRegistry.register(new PasteTool());
        this.history = new OperationHistory(20);
        this.toolExecutor = new ToolExecutor(
                toolRegistry, history, new OperationGuard(limits), new PaperPermissionService(getServer()));

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String name = "masonry_preview";
        int i = 0;
        while (scoreboard.getTeam(name) != null && i < 10) {
            name = "masonry_preview" + i;
            i++;
        }
        while (scoreboard.getTeam(name) != null) {
            name = "masonry_" + Long.toHexString(System.nanoTime()).substring(0, 8);
        }
        this.previewTeam = scoreboard.registerNewTeam(name);
        this.previewTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        this.previewTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        this.previewTeam.setCanSeeFriendlyInvisibles(false);

        this.previewRenderer = new PaperPreviewRenderer(this, sessions, previewTeam);
        this.worldAccess = new MasonryWorldAccess(getServer(), getLogger());
        this.survivalTransaction = new PaperSurvivalTransaction(getServer());
        this.taskScheduler = new PaperTaskScheduler(this);
        this.hoverPreviews = new HoverPreviewDriver(this, sessions, limits, previewRenderer);
        this.hoverPreviews.start();
        BlueprintManager blueprints =
                new BlueprintManager(new FileBlueprintStore(getDataFolder().toPath().resolve("blueprints")), sessions);
        MasonryCommands commands = new MasonryCommands(
                sessions,
                new OperationGuard(limits),
                toolExecutor,
                blueprints,
                previewRenderer,
                worldAccess,
                survivalTransaction);

        LifecycleEventManager<org.bukkit.plugin.Plugin> manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            LiteralCommandNode<CommandSourceStack> node = new MasonryBrigadierCommand(commands, limits, this).tree();
            event.registrar().register(node, "Masonry command root", List.of());
        });

        GadgetListener gadgetListener = new GadgetListener(
                this, commands, sessions, limits, worldAccess, survivalTransaction);
        getServer().getPluginManager().registerEvents(gadgetListener, this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BuildTools loaded.");
    }

    @Override
    public void onDisable() {
        if (this.previewTeam != null) {
            this.previewTeam.unregister();
            this.previewTeam = null;
        }
        this.toolRegistry = null;
        this.toolExecutor = null;
        this.history = null;
        this.previewRenderer = null;
        this.worldAccess = null;
        this.survivalTransaction = null;
        this.taskScheduler = null;
        this.hoverPreviews = null;
        getLogger().info("BuildTools disabled.");
    }

    /**
     * Clears preview state and session state so logout cannot leak client-side previews.
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
        if (hoverPreviews != null) {
            hoverPreviews.forget(actor.value());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        schedulePreviewResend(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        schedulePreviewResend(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (previewRenderer == null) {
            return;
        }
        getServer().getScheduler().runTask(this, () -> {
            if (previewRenderer == null) {
                return;
            }
            for (Player player : getServer().getOnlinePlayers()) {
                previewRenderer.resendChunk(new ActorId(player.getUniqueId()), event.getChunk());
            }
        });
    }

    private void schedulePreviewResend(UUID playerId) {
        if (previewRenderer == null) {
            return;
        }
        getServer().getScheduler().runTask(this, () -> {
            if (previewRenderer != null) {
                previewRenderer.resend(new ActorId(playerId));
            }
        });
    }
}
