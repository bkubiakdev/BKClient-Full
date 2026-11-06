package dev.bkclient.utility.concurrent;

import java.util.ArrayList;
import java.util.List;

public final class ShutdownTasksTest {
    public static void main(String[] args) {
        var completed = new ArrayList<String>();
        var failures = new ArrayList<RuntimeException>();
        var saveFailure = new IllegalStateException("disk unavailable");
        var serviceFailure = new IllegalStateException("service cleanup failed");
        ShutdownTasks.run(failures::add,
            () -> { throw saveFailure; },
            () -> completed.add("modules"),
            () -> { throw serviceFailure; },
            () -> completed.add("workers"),
            () -> completed.add("render resources"));
        if (!completed.equals(List.of("modules", "workers", "render resources")))
            throw new AssertionError("Shutdown stopped early or changed cleanup order");
        if (!failures.equals(List.of(saveFailure, serviceFailure)))
            throw new AssertionError("Shutdown did not report the original failures");
        ShutdownTasks.run(failures::add, () -> completed.add("normal shutdown"));
        if (!completed.getLast().equals("normal shutdown") || failures.size() != 2)
            throw new AssertionError("Normal shutdown failed");
        System.out.println("Shutdown cleanup continues after save and service failures");
    }
}
