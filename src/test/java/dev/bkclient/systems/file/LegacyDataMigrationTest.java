package dev.bkclient.systems.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class LegacyDataMigrationTest {
    public static void main(String[] args) throws Exception {
        Path run = Files.createTempDirectory("bk-storage-migration-test-");
        try {
            put(run, "com.apple.core/client.rock", "client: zażółć");
            put(run, "com.apple.core/viewmodel.rock", "viewmodel");
            put(run, "com.apple.core/configs/autosave.sw", "old autosave");
            put(run, "com.apple.core/configs/profile.sw", "profile");
            put(run, "com.apple.core/configs/both.sw", "old format");
            put(run, "com.apple.core/configs/both.bk", "new format");
            put(run, "com.apple.core/presets/swing/custom.sw", "swing");
            put(run, "com.apple.core/auto_market_configs/market.json", "market: żelazo");
            put(run, "com.apple.core/TinyTask/recording.json", "recording");
            put(run, "com.apple.core/backgrounds/test.sw", "not a config");
            put(run, "BK-Client/configs/autosave.bk", "Copy autosave");
            put(run, "bkclient/configs/upload.sw", "upload");

            LegacyDataMigration.migrate(run);
            expect(run, "BK-Client/client.bk", "client: zażółć");
            expect(run, "BK-Client/viewmodel.bk", "viewmodel");
            expect(run, "BK-Client/configs/autosave.bk", "Copy autosave");
            expect(run, "BK-Client/configs/profile.bk", "profile");
            expect(run, "BK-Client/configs/both.bk", "new format");
            expect(run, "BK-Client/presets/swing/custom.bk", "swing");
            expect(run, "BK-Client/auto_market_configs/market.json", "market: żelazo");
            expect(run, "BK-Client/TinyTask/recording.json", "recording");
            expect(run, "BK-Client/backgrounds/test.sw", "not a config");
            expect(run, "BK-Client/configs/upload.bk", "upload");
            expect(run, "com.apple.core/configs/profile.sw", "profile");

            Files.delete(run.resolve("BK-Client/configs/profile.bk"));
            LegacyDataMigration.migrate(run);
            if (Files.exists(run.resolve("BK-Client/configs/profile.bk"))) {
                throw new AssertionError("A deleted profile was resurrected on the next startup");
            }
            LegacyDataMigration.migrate(run.resolve("fresh"));
            if (!Files.isDirectory(run.resolve("fresh/BK-Client"))) throw new AssertionError("Fresh install failed");
            System.out.println("Legacy migration checks passed");
        } finally {
            // All paths are descendants of the unique temporary fixture directory.
            try (var files = Files.walk(run)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void put(Path root, String relative, String value) throws Exception {
        Path path = root.resolve(relative);
        Files.createDirectories(path.getParent());
        Files.writeString(path, value);
    }

    private static void expect(Path root, String relative, String value) throws Exception {
        if (!Files.readString(root.resolve(relative)).equals(value)) throw new AssertionError(relative);
    }
}
