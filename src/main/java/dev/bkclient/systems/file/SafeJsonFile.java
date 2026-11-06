package dev.bkclient.systems.file;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.Predicate;

/** Serialized, UTF-8 JSON replacement with a last valid backup. */
public final class SafeJsonFile {
    private SafeJsonFile() {}

    public static Path backup(Path file) {
        return file.resolveSibling(file.getFileName() + ".bak");
    }

    public static synchronized JsonObject read(Path file) throws IOException {
        return read(file, json -> true);
    }

    public static synchronized JsonObject read(Path file, Predicate<JsonObject> validator) throws IOException {
        try {
            return parse(file, validator);
        } catch (IOException original) {
            JsonObject recovered;
            try {
                recovered = parse(backup(file), validator);
            } catch (IOException invalidBackup) {
                original.addSuppressed(invalidBackup);
                throw original;
            }
            // Keep the damaged input for diagnosis; never replace the good backup with it.
            if (Files.exists(file)) {
                Path damaged = Files.createTempFile(file.toAbsolutePath().getParent(), file.getFileName() + ".corrupt-", ".damaged");
                Files.copy(file, damaged, StandardCopyOption.REPLACE_EXISTING);
            }
            replace(file, recovered.toString());
            return recovered;
        }
    }

    public static synchronized void write(Path file, JsonObject json) throws IOException {
        write(file, json, value -> true);
    }

    /** Validate the incoming file before touching the destination or its backup. */
    public static synchronized void importFile(Path source, Path destination, Predicate<JsonObject> validator) throws IOException {
        JsonObject incoming = parse(source, validator);
        if (Files.exists(destination) && Files.isSameFile(source, destination)) return;
        write(destination, incoming, validator);
    }

    public static synchronized void write(Path file, JsonObject json, Predicate<JsonObject> validator) throws IOException {
        if (json == null || !validator.test(json)) throw new IOException("Invalid JSON structure: " + file);
        String data = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json);
        if (Files.exists(file)) {
            JsonObject previous = null;
            try {
                previous = parse(file, validator);
            } catch (IOException invalid) {
                // Do not silently overwrite damaged data or discard its backup.
                read(file, validator);
                previous = parse(file, validator);
            }
            replace(backup(file), previous.toString());
        }
        replace(file, data);
    }

    private static JsonObject parse(Path file, Predicate<JsonObject> validator) throws IOException {
        try {
            var parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject() || !validator.test(parsed.getAsJsonObject())) {
                throw new IOException("Invalid JSON structure: " + file);
            }
            return parsed.getAsJsonObject();
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid JSON: " + file, invalid);
        }
    }

    private static void replace(Path file, String data) throws IOException {
        Path target = file.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName() + ".", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer bytes = StandardCharsets.UTF_8.encode(data);
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            // Fail safely if this filesystem cannot replace atomically.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
