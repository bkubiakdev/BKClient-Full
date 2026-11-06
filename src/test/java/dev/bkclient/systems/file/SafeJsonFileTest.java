package dev.bkclient.systems.file;

import com.google.gson.JsonObject;
import java.nio.file.*;
import java.util.concurrent.*;

public final class SafeJsonFileTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("bk-config-test-");
        try {
            checkImports(directory);
            Path file = directory.resolve("config.bk");
            SafeJsonFile.write(file, value("Zażółć gęślą"));
            check(SafeJsonFile.read(file).get("value").getAsString().equals("Zażółć gęślą"), "UTF-8");
            SafeJsonFile.write(file, value("second"));
            Files.writeString(file, "{broken");
            check(SafeJsonFile.read(file).get("value").getAsString().equals("Zażółć gęślą"), "backup recovery");
            check(Files.readString(file).contains("Zażółć"), "repaired primary");
            Files.writeString(file, "[]");
            check(SafeJsonFile.read(file).has("value"), "invalid structure recovery");
            Files.delete(file);
            check(SafeJsonFile.read(file).has("value"), "missing primary recovery");
            Path corrupt = directory.resolve("bad.bk");
            Files.writeString(corrupt, "{broken");
            try {
                SafeJsonFile.write(corrupt, value("replacement"));
                throw new AssertionError("overwrote unrecoverable config");
            } catch (java.io.IOException expected) {
                check(Files.readString(corrupt).equals("{broken"), "original preserved");
            }
            try {
                SafeJsonFile.write(file, new JsonObject(), json -> json.has("value"));
                throw new AssertionError("accepted invalid structure");
            } catch (java.io.IOException expected) { }
            try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
                var tasks = new java.util.ArrayList<Future<?>>();
                for (int i = 0; i < 24; i++) {
                    int id = i;
                    tasks.add(pool.submit(() -> {
                        try { SafeJsonFile.write(file, value("writer-" + id)); }
                        catch (Exception error) { throw new RuntimeException(error); }
                    }));
                }
                for (Future<?> task : tasks) task.get();
            }
            check(SafeJsonFile.read(file).get("value").getAsString().startsWith("writer-"), "concurrent primary");
            check(SafeJsonFile.read(SafeJsonFile.backup(file)).has("value"), "concurrent backup");
            try (var files = Files.list(directory)) {
                check(files.noneMatch(path -> path.toString().endsWith(".tmp")), "temporary cleanup");
            }
            System.out.println("Safe JSON config checks passed");
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
    private static JsonObject value(String text) {
        JsonObject json = new JsonObject();
        json.addProperty("value", text);
        json.addProperty("unknown_preserved", true);
        return json;
    }
    private static void checkImports(Path directory) throws Exception {
        Path target = directory.resolve("import-target.bk");
        Path source = directory.resolve("incoming.sw");
        var validator = (java.util.function.Predicate<JsonObject>) json -> json.has("value");
        SafeJsonFile.write(target, value("older"), validator);
        SafeJsonFile.write(target, value("latest settings"), validator);
        byte[] original = Files.readAllBytes(target);
        byte[] originalBackup = Files.readAllBytes(SafeJsonFile.backup(target));
        for (String invalid : new String[]{"{broken", "[]", "{}"}) {
            Files.writeString(source, invalid);
            try {
                SafeJsonFile.importFile(source, target, validator);
                throw new AssertionError("accepted invalid import: " + invalid);
            } catch (java.io.IOException expected) {
                check(java.util.Arrays.equals(original, Files.readAllBytes(target)), "invalid import preserves primary");
                check(java.util.Arrays.equals(originalBackup, Files.readAllBytes(SafeJsonFile.backup(target))), "invalid import preserves backup");
            }
        }
        Files.writeString(source, value("nowe ustawienia ąęł").toString());
        SafeJsonFile.importFile(source, target, validator);
        check(SafeJsonFile.read(target).get("value").getAsString().equals("nowe ustawienia ąęł"), "valid UTF-8 import");
        check(SafeJsonFile.read(SafeJsonFile.backup(target)).get("value").getAsString().equals("latest settings"), "import backs up latest settings");
        byte[] backupBeforeSelfImport = Files.readAllBytes(SafeJsonFile.backup(target));
        SafeJsonFile.importFile(target, target, validator);
        check(java.util.Arrays.equals(backupBeforeSelfImport, Files.readAllBytes(SafeJsonFile.backup(target))), "same-file import preserves backup");
        Path blocked = Files.createDirectory(directory.resolve("blocked.bk"));
        Path marker = blocked.resolve("keep.txt");
        Files.writeString(marker, "keep");
        try {
            SafeJsonFile.importFile(source, blocked, validator);
            throw new AssertionError("reported successful import to unwritable destination");
        } catch (java.io.IOException expected) {
            check(Files.readString(marker).equals("keep"), "write failure preserves destination");
        }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
