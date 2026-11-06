package dev.bkclient.systems.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

/** Copies old installations once; existing BK-Client files always take precedence. */
public final class LegacyDataMigration {
    private LegacyDataMigration() {}

    public static void migrate(Path runDirectory) throws IOException {
        Path target = runDirectory.resolve("BK-Client");
        Files.createDirectories(target);
        copyOnce(runDirectory.resolve("com.apple.core"), target, ".migrated-com-apple-core");
        copyOnce(runDirectory.resolve("bkclient/configs"), target.resolve("configs"), ".migrated-upload-configs");
    }

    private static void copyOnce(Path source, Path target, String markerName) throws IOException {
        Path marker = target.resolve(markerName);
        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS) || Files.exists(marker)) return;
        Files.createDirectories(target);
        List<Path> files;
        try (var walk = Files.walk(source)) {
            files = walk.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).sorted().toList();
        }
        // Copy canonical names first if the old directory contains both formats.
        for (boolean legacy : new boolean[]{false, true}) {
            for (Path file : files) {
                Path relative = source.relativize(file);
                Path renamed = canonicalPath(relative);
                if (relative.equals(renamed) == legacy) continue;
                Path destination = target.resolve(renamed);
                Files.createDirectories(destination.getParent());
                if (!Files.exists(destination)) Files.copy(file, destination);
            }
        }
        Files.writeString(marker, "Legacy files copied without replacing existing files. Originals retained.\n");
    }

    private static Path canonicalPath(Path relative) {
        String name = relative.getFileName().toString();
        String parent = relative.getParent() == null ? "" : relative.getParent().toString().replace('\\', '/');
        if ((parent.isEmpty() || parent.equals("configs") || parent.equals("presets/swing")) && name.endsWith(".sw")) {
            return relative.resolveSibling(name.substring(0, name.length() - 3) + ".bk");
        }
        if (parent.isEmpty() && name.endsWith(".rock")) {
            return relative.resolveSibling(name.substring(0, name.length() - 5) + ".bk");
        }
        return relative;
    }
}
