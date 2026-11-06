package dev.bkclient.systems.modules.modules.other.tinytask;

import net.minecraft.client.input.Input;

public final class TinyTaskBridge {
    private static TinyTaskController controller;

    private TinyTaskBridge() {
    }

    public static void register(TinyTaskController controller) {
        TinyTaskBridge.controller = controller;
    }

    public static void tick() {
        if (controller != null) {
            controller.tick();
        }
    }

    public static boolean isPlaying() {
        return controller != null && controller.isPlaying();
    }

    public static boolean isRecording() {
        return controller != null && controller.isRecording();
    }

    public static boolean isActive() {
        return isRecording() || isPlaying();
    }

    public static String statusLabel() {
        return controller == null ? "" : controller.statusLabel();
    }

    public static float progress() {
        return controller == null ? 0.0f : controller.progress();
    }

    public static int tickCount() {
        return controller == null ? 0 : controller.tickCount();
    }

    public static boolean hasModifier(String modifier) {
        return controller != null && controller.hasModifier(modifier);
    }

    public static void applyInputOverride(Input input) {
        if (controller != null) {
            controller.applyInputOverride(input);
        }
    }

    public static void recordChatOrCommand(String text, boolean command) {
        if (controller != null) {
            controller.recordChatOrCommand(text, command);
        }
    }

    public static void recordGuiClick(String screenClass, double mouseX, double mouseY, int button) {
        if (controller != null) {
            controller.recordGuiClick(screenClass, mouseX, mouseY, button);
        }
    }
}
