package dev.mintychochip.masonry.api.service;

/**
 * Main-thread scheduling port. Paper implements this with the Bukkit scheduler.
 */
public interface TaskScheduler {
    /**
     * Runs {@code task} immediately if already on the primary thread, otherwise schedules it.
     *
     * @param task work
     */
    void runOnMain(Runnable task);

    /**
     * @param task work
     * @param delayTicks delay in server ticks
     */
    void runLater(Runnable task, long delayTicks);
}
