package dev.bkclient.utility.concurrent;

import java.util.function.Consumer;

/** Run shutdown steps in order even if saving or an earlier cleanup step fails. */
public final class ShutdownTasks {
    private ShutdownTasks() {}

    public static void run(Consumer<RuntimeException> onError, Runnable... tasks) {
        for (Runnable task : tasks) {
            try {
                task.run();
            } catch (RuntimeException error) {
                onError.accept(error);
            }
        }
    }
}
