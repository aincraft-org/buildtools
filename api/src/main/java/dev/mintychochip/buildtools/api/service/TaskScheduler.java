package dev.mintychochip.buildtools.api.service;

public interface TaskScheduler {
    void runOnMain(Runnable task);

    void runLater(Runnable task, long delayTicks);
}
