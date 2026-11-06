package dev.bkclient;

import com.apple.secure.enclave.NativeClassLoader;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.launch.common.FabricLauncherBase;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MagicClassProposer implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        System.out.println("[BK-Client] Starting...");

        try {
            detectDebugger();

            ModContainer container = FabricLoader.getInstance().getModContainer("bkclient").orElse(null);
            if (container == null) return;

            loadNestedJars(container);

            ClassLoader cl = FabricLauncherBase.getLauncher().getTargetClassLoader();

            // Open the JAR that contains this class
            Path codeSource = getCodeSourcePath();
            if (!Files.isRegularFile(codeSource)) {
                System.out.println("[BK-Client] Development classes detected; protected-class scan skipped");
                return;
            }

            // Collect .enclv entries from the JAR
            List<String> classNames = new ArrayList<>();
            Map<String, byte[]> classData = new LinkedHashMap<>();
            try (JarFile jar = new JarFile(codeSource.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry je = entries.nextElement();
                    String name = je.getName();
                    if (!name.endsWith(".enclv")) continue;
                    String dotName = name.substring(0, name.length() - 6).replace('/', '.');
                    byte[] data;
                    try (var input = jar.getInputStream(je)) {
                        data = input.readAllBytes();
                    }
                    classNames.add(dotName);
                    classData.put(dotName, data);
                }
            }

            if (classData.isEmpty()) {
                System.out.println("[BK-Client] No .enclv files found in JAR");
                return;
            }
            System.out.println("[BK-Client] Found " + classData.size() + " .enclv files in JAR");

            int defined = 0;
            int failed = 0;
            int skipped = 0;
            int exceptions = 0;
            int logged = 0;

            for (int retry = 0; retry < 10; retry++) {
                Map<String, byte[]> nextRound = new LinkedHashMap<>();
                for (Map.Entry<String, byte[]> entry : classData.entrySet()) {
                    String dotName = entry.getKey();
                    byte[] raw = entry.getValue();
                    try {
                        if (raw.length < 2 || raw[0] != 0x01) { skipped++; continue; }

                        if (logged < 5) {
                            System.out.println("[BK-Client] Defining: " + dotName + " (" + raw.length + " bytes, first bytes: " 
                                + String.format("%02X %02X %02X %02X", raw[0] & 0xFF, raw[1] & 0xFF, raw[2] & 0xFF, raw[3] & 0xFF) + ")");
                            logged++;
                        }

                        try {
                            NativeClassLoader.defineClass(dotName, raw, new byte[0], cl);
                            defined++;
                            Arrays.fill(raw, (byte) 0);
                        } catch (ClassFormatError e) {
                            if (e.getMessage() != null && e.getMessage().contains("Incompatible magic value")) {
                                nextRound.put(dotName, raw);
                            } else {
                                failed++;
                                Arrays.fill(raw, (byte) 0);
                                System.out.println("[BK-Client] ClassFormatError: " + dotName + " - " + e.getMessage());
                            }
                        } catch (Throwable e) {
                            if (nextRound.isEmpty() && retry == 0) {
                                System.out.println("[BK-Client] defineClass error: " + dotName + " - " + e.getClass().getSimpleName() + " - " + e.getMessage());
                            }
                            nextRound.put(dotName, raw);
                        }

                    } catch (Throwable e) {
                        exceptions++;
                        if (exceptions <= 3) {
                            System.out.println("[BK-Client] Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        }
                    }
                }
                classData = nextRound;
                if (classData.isEmpty()) break;
            }

            System.out.println("[BK-Client] Result: defined=" + defined + " failed=" + failed
                + " skipped=" + skipped + " exceptions=" + exceptions);

            System.out.println("[BK-Client] Done");
        } catch (Throwable e) {
            System.out.println("[BK-Client] Fatal: " + e);
        }
    }

    private boolean detectDebugger() {
        try {
            String args = ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
            if (args.contains("-agentlib:jdwp") || args.contains("-Xrunjdwp") || args.contains("-Xdebug")) {
                System.out.println("[BK-Client] Debugger detected (non-fatal)");
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private void loadNestedJars(ModContainer container) throws Exception {
        Path codeSource = getCodeSourcePath();
        if (!Files.isRegularFile(codeSource)) return;

        try (JarFile jar = new JarFile(codeSource.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                String name = je.getName();
                if (name.startsWith("META-INF/jars/") && name.endsWith(".jar")) {
                    byte[] nestedData;
                    try (var input = jar.getInputStream(je)) {
                        nestedData = input.readAllBytes();
                    }
                    Path tempDirectory = Files.createTempDirectory("bk-client-nested-");
                    Path tempJar = Files.write(tempDirectory.resolve(Path.of(name).getFileName()), nestedData);
                    tempJar.toFile().deleteOnExit();
                    tempDirectory.toFile().deleteOnExit();
                    FabricLauncherBase.getLauncher().propose(tempJar.toUri().toURL());
                    System.out.println("[BK-Client] Loaded nested JAR: " + name);
                }
            }
        }
    }

    private Path getCodeSourcePath() throws Exception {
        URL location = MagicClassProposer.class.getProtectionDomain().getCodeSource().getLocation();
        URI uri = location.toURI();
        return Path.of(uri);
    }
}
