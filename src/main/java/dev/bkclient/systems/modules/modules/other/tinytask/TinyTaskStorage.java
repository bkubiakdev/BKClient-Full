package dev.bkclient.systems.modules.modules.other.tinytask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.bkclient.systems.file.FileManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public final class TinyTaskStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TinyTaskStorage() {
    }

    public static void save(TinyTaskRecording recording) throws IOException {
        Files.writeString(file(recording.name), GSON.toJson(recording), StandardCharsets.UTF_8);
    }

    public static TinyTaskRecording load(String name) throws IOException {
        return GSON.fromJson(Files.readString(file(name), StandardCharsets.UTF_8), TinyTaskRecording.class);
    }

    public static List<String> listNames() {
        List<String> names = new ArrayList<>();

        try (Stream<Path> stream = Files.list(directory())) {
            stream.filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> names.add(path.getFileName().toString().replace(".json", "")));
        } catch (IOException ignored) {
        }

        Collections.sort(names);
        return names;
    }

    public static String latestName() {
        List<String> names = listNames();
        return names.isEmpty() ? "" : names.get(names.size() - 1);
    }

    private static Path file(String name) throws IOException {
        return directory().resolve(sanitize(name) + ".json");
    }

    private static Path directory() throws IOException {
        Path path = FileManager.DIRECTORY.toPath().resolve("TinyTask");
        Files.createDirectories(path);
        migrateLegacyDirectory(path);
        return path;
    }

    private static void migrateLegacyDirectory(Path targetPath) {
        Path legacyPath = FabricLoader.getInstance().getGameDir().resolve("BKClient").resolve("TinyTask");
        if (!Files.isDirectory(legacyPath)) {
            return;
        }
        try (Stream<Path> stream = Files.list(legacyPath)) {
            stream.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                Path targetFile = targetPath.resolve(path.getFileName());
                if (Files.exists(targetFile)) {
                    return;
                }
                try {
                    Files.copy(path, targetFile);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    public static String sanitize(String name) {
        String sanitized = name == null ? "" : name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return sanitized.isEmpty() ? "tinytask_" + System.currentTimeMillis() : sanitized;
    }
}
