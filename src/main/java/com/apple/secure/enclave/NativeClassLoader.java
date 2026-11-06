package com.apple.secure.enclave;

import java.io.*;
import java.nio.file.*;

public class NativeClassLoader {

    private static boolean loaded = false;

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        
        System.err.println("[NativeLoader] OS=" + os + " Arch=" + arch);
        
        if (os.contains("mac") || os.contains("darwin")) {
            try {
                boolean isArm64 = arch.contains("aarch64") || arch.contains("arm");
                String libName = isArm64 ? "libbkclientloader_arm64.dylib" : "libbkclientloader.dylib";
                String libPath = "META-INF/native/macos/";

                if (!arch.contains("64") && !isArm64) {
                    throw new UnsupportedOperationException("Only 64-bit supported");
                }

                System.err.println("[NativeLoader] Loading: " + libName + " from " + libPath);
                String resourcePath = libPath + libName;
                System.err.println("[NativeLoader] Resource: " + resourcePath);
                InputStream in = NativeClassLoader.class.getClassLoader().getResourceAsStream(resourcePath);
                System.err.println("[NativeLoader] InputStream: " + (in != null ? "found" : "NULL"));
                if (in == null) {
                    // Fallback: try the other arch
                    libName = isArm64 ? "libbkclientloader.dylib" : "libbkclientloader_arm64.dylib";
                    resourcePath = libPath + libName;
                    in = NativeClassLoader.class.getClassLoader().getResourceAsStream(resourcePath);
                }
                if (in == null) {
                    in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
                }
                if (in == null) {
                    System.err.println("[NativeLoader] WARNING: Native library not found: " + resourcePath);
                    System.err.println("Continuing without native class loader...");
                    loaded = true;
                    return;
                }

                Path tempDir = Files.createTempDirectory("bkclientloader_");
                Path tempFile = tempDir.resolve(libName);
                try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
                    in.transferTo(out);
                }
                in.close();

                try {
                    System.load(tempFile.toAbsolutePath().toString());
                } catch (UnsatisfiedLinkError e) {
                    System.err.println("Warning: Failed to load native library " + libName + ": " + e.getMessage());
                    System.err.println("The library may not be available on this platform.");
                    System.err.println("Continuing without native class loader...");
                    loaded = true;
                    return;
                }

                tempFile.toFile().deleteOnExit();
                tempDir.toFile().deleteOnExit();

                loaded = true;
            } catch (Exception e) {
                System.err.println("Error loading native class loader on macOS: " + e.getMessage());
                e.printStackTrace();
                System.err.println("Continuing without native class loader...");
                loaded = true;
            }
        } else if (os.contains("windows")) {
            try {
                String libName = "bkclientloader.dll";
                String libPath = "META-INF/native/win/";

                if (!arch.contains("64")) {
                    throw new UnsupportedOperationException("Only 64-bit supported");
                }

                System.err.println("[NativeLoader] Loading: " + libName + " from " + libPath);
                String resourcePath = libPath + libName;
                System.err.println("[NativeLoader] Resource: " + resourcePath);
                InputStream in = NativeClassLoader.class.getClassLoader().getResourceAsStream(resourcePath);
                System.err.println("[NativeLoader] InputStream: " + (in != null ? "found" : "NULL"));
                if (in == null) {
                    in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
                }
                if (in == null) {
                    System.err.println("[NativeLoader] WARNING: Native library not found: " + resourcePath);
                    System.err.println("Continuing without native class loader...");
                    loaded = true;
                    return;
                }

                Path tempDir = Files.createTempDirectory("bkclientloader_");
                Path tempFile = tempDir.resolve(libName);
                try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
                    in.transferTo(out);
                }
                in.close();

                System.err.println("[NativeLoader] Loading DLL: " + tempFile.toAbsolutePath());
                try {
                    System.load(tempFile.toAbsolutePath().toString());
                    System.err.println("[NativeLoader] DLL loaded OK");
                } catch (UnsatisfiedLinkError e) {
                    System.err.println("[NativeLoader] FAILED: " + e.getMessage());
                    System.err.println("The library may not be available on this platform.");
                    System.err.println("Continuing without native class loader...");
                    loaded = true;
                    return;
                }

                tempFile.toFile().deleteOnExit();
                tempDir.toFile().deleteOnExit();

                loaded = true;
            } catch (Exception e) {
                System.err.println("Error loading native class loader on Windows: " + e.getMessage());
                e.printStackTrace();
                System.err.println("Continuing without native class loader...");
                loaded = true;
            }
        } else if (os.contains("linux")) {
            try {
                String libName = "libbkclientloader.so";
                String libPath = "META-INF/native/linux/";

                if (!arch.contains("64")) {
                    throw new UnsupportedOperationException("Only 64-bit supported");
                }

                String resourcePath = libPath + libName;
                InputStream in = NativeClassLoader.class.getClassLoader().getResourceAsStream(resourcePath);
                if (in == null) {
                    in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
                }
                if (in == null) {
                    System.err.println("Warning: Native library not found in JAR: " + resourcePath);
                    System.err.println("Continuing without native class loader...");
                    loaded = true;
                    return;
                }

                Path tempDir = Files.createTempDirectory("bkclientloader_");
                Path tempFile = tempDir.resolve(libName);
                try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
                    in.transferTo(out);
                }
                in.close();

                try {
                    System.load(tempFile.toAbsolutePath().toString());
                } catch (UnsatisfiedLinkError e) {
                    System.err.println("Warning: Failed to load native library " + libName + ": " + e.getMessage());
                    System.err.println("Continuing without native class loader...");
                    loaded = true;
                    return;
                }

                tempFile.toFile().deleteOnExit();
                tempDir.toFile().deleteOnExit();

                loaded = true;
            } catch (Exception e) {
                System.err.println("Error loading native class loader on Linux: " + e.getMessage());
                e.printStackTrace();
                System.err.println("Continuing without native class loader...");
                loaded = true;
            }
        } else {
            System.err.println("Warning: Unsupported OS " + os);
            System.err.println("Continuing without native class loader...");
            loaded = true;
        }
    }

    public static Class<?> defineClass(String className, byte[] encryptedData, byte[] keyData, ClassLoader loader) throws ClassNotFoundException {
        ensureLoaded();
        try {
            // Decrypt via native RC4
            byte[] decrypted = decryptNative(encryptedData, keyData, className);
            if (decrypted == null || decrypted.length < 4) {
                throw new ClassNotFoundException("Decryption failed for: " + className);
            }
            // Verify it's a valid class file (magic = 0xCAFEBABE)
            if (decrypted[0] != (byte)0xCA || decrypted[1] != (byte)0xFE ||
                decrypted[2] != (byte)0xBA || decrypted[3] != (byte)0xBE) {
                throw new ClassNotFoundException("Invalid class data for: " + className);
            }
            // Define class via reflection
            java.lang.reflect.Method defineClassMethod = java.lang.ClassLoader.class.getDeclaredMethod(
                "defineClass", String.class, byte[].class, int.class, int.class, java.security.ProtectionDomain.class);
            defineClassMethod.setAccessible(true);
            // Pass null as class name — JVM reads the actual name from bytecode.
            // The obfuscator renames the class internally, so the original name
            // (used for key derivation in decryptNative) won't match the bytecode name.
            Class<?> cls = (Class<?>) defineClassMethod.invoke(loader, null, decrypted, 0, decrypted.length, null);
            return cls;
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ClassNotFoundException("Failed to define class: " + className, e);
        }
    }

    private static native byte[] decryptNative(byte[] encryptedData, byte[] keyData, String className);
}