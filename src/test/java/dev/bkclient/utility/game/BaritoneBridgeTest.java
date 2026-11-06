package dev.bkclient.utility.game;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;

/** Exercises optional dependency discovery in isolated mod class loaders. */
public final class BaritoneBridgeTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class ModLoader extends URLClassLoader {
        int apiLookups;
        ModLoader(Path fixtures) throws Exception {
            super(new URL[]{BaritoneBridge.class.getProtectionDomain().getCodeSource().getLocation(),
                    fixtures.toUri().toURL()}, BaritoneBridgeTest.class.getClassLoader());
        }
        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("dev.bkclient.utility.game.BaritoneBridge")
                    && !name.startsWith("baritone.api.")) return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    if (name.equals("baritone.api.BaritoneAPI")) apiLookups++;
                    loaded = findClass(name);
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("bkclient-baritone-test-");
        Path missing = Files.createDirectory(root.resolve("absent"));
        try (var loader = new ModLoader(missing)) {
            Class<?> bridge = loader.loadClass("dev.bkclient.utility.game.BaritoneBridge");
            var available = bridge.getMethod("isAvailable");
            for (int i = 0; i < 10_000; i++) check(!(boolean) available.invoke(null), "absent mod hidden");
            check(loader.apiLookups == 1, "absent API must be searched only once");
            check(((String) bridge.getMethod("getLastError").invoke(null)).contains("ClassNotFoundException"), "missing mod diagnostic");
            check(!(boolean) bridge.getMethod("stop").invoke(null), "absent mod action fails safely");
            check(loader.apiLookups == 1, "actions reuse absent API result");
        }

        Path present = Files.createDirectory(root.resolve("present"));
        Path source = present.resolve("BaritoneAPI.java");
        Files.writeString(source, """
            package baritone.api;
            public final class BaritoneAPI {
                public static boolean ready;
                public static int calls;
                public static Object getProvider() {
                    calls++;
                    if (!ready) throw new IllegalStateException("Not ready yet");
                    return new Provider();
                }
                public static class Provider {
                    public Object getPrimaryBaritone() { return new Object(); }
                }
            }
            """);
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", present.toString(), source.toString());
        check(result == 0, "compile optional mod fixture");
        try (var loader = new ModLoader(present)) {
            Class<?> bridge = loader.loadClass("dev.bkclient.utility.game.BaritoneBridge");
            var available = bridge.getMethod("isAvailable");
            check(!(boolean) available.invoke(null), "provider may not be ready initially");
            Class<?> api = loader.loadClass("baritone.api.BaritoneAPI");
            api.getField("ready").setBoolean(null, true);
            check((boolean) available.invoke(null), "provider readiness must recover without restart");
            api.getField("ready").setBoolean(null, false);
            check(!(boolean) available.invoke(null), "provider failure remains visible after success");
            api.getField("ready").setBoolean(null, true);
            check((boolean) available.invoke(null), "provider can recover again");
            check(loader.apiLookups == 1, "present API class resolved once");
            check(api.getField("calls").getInt(null) == 4, "provider remains live");
        }
        try (var files = Files.walk(root)) {
            for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(file);
        }
        System.out.println("Baritone absence caching and live provider recovery passed");
    }
}
