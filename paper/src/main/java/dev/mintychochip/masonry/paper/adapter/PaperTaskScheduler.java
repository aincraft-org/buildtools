package dev.mintychochip.masonry.paper.adapter;

import dev.mintychochip.masonry.api.service.TaskScheduler;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bukkit scheduler adapter that runs immediately when already on the primary thread.
 */
public final class PaperTaskScheduler implements TaskScheduler {
    private final JavaPlugin plugin;

    /**
     * @param plugin owning plugin
     */
    public PaperTaskScheduler(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void runOnMain(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (plugin.getServer().isPrimaryThread()) {
            task.run();
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public void runLater(Runnable task, long delayTicks) {
        Objects.requireNonNull(task, "task");
        plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
    }
}
