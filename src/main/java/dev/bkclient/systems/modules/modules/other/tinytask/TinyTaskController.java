package dev.bkclient.systems.modules.modules.other.tinytask;

import dev.bkclient.BKClientInit;
import dev.bkclient.mixin.accessors.KeyBindingAccessor;
import dev.bkclient.mixin.minecraft.client.input.InputAccessor;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.Input;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.PlayerInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TinyTaskController {
    private static final int GUI_EVENT_WAIT_TICKS = 40;
    private static final int GUI_SETTLE_TICKS = 3;
    private static final int SCREEN_CLOSE_WAIT_TICKS = 20;

    private final BaseModule module;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final BooleanSetting loop;
    private final StringSetting macroName;
    private final ButtonSetting startRecording;
    private final ButtonSetting stopRecording;
    private final ButtonSetting playLast;
    private final ButtonSetting stopPlayback;

    private TinyTaskRecording recording;
    private TinyTaskRecording playback;
    private Map<Long, List<TinyTaskEvent>> playbackEvents = Map.of();
    private long recordingTick = 0L;
    private int playbackIndex = 0;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private boolean transitioning = false;
    private int transitionTicks = 0;
    private int totalTransitionTicks = 0;
    private float startYaw = 0.0f;
    private float startPitch = 0.0f;
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private List<String> currentModifiers = List.of();
    private TinyTaskTick currentPlaybackTick = null;
    private TinyTaskEvent pendingEvent = null;
    private int pendingEventTicks = 0;
    private int playbackWaitTicks = 0;
    private int screenCloseWaitTicks = 0;
    private int worldUseWaitPlaybackIndex = -1;

    public TinyTaskController(BaseModule module) {
        this.module = module;
        this.loop = new BooleanSetting(module, "TinyTask Loop").enabled(false);
        this.macroName = new StringSetting(module, "TinyTask Name").text("kopacz");
        this.startRecording = new ButtonSetting(module, "TinyTask Start Recording").action(this::startRecording);
        this.stopRecording = new ButtonSetting(module, "TinyTask Stop + Save").action(this::stopRecordingAndSave);
        this.playLast = new ButtonSetting(module, "TinyTask Play").action(this::playSelectedOrLatest);
        this.stopPlayback = new ButtonSetting(module, "TinyTask Stop").action(this::stop);
        TinyTaskBridge.register(this);
    }

    public boolean tick() {
        if (!module.isEnabled()) {
            if (isRecording || isPlaying) {
                stopAll();
            }
            return false;
        }
        if (mc.player == null) {
            stop(false);
            return false;
        }

        if (isPlaying) {
            tickPlayback();
            return true;
        }

        if (isRecording) {
            recordTick();
        }

        return false;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public String statusLabel() {
        if (isRecording) {
            return "REC";
        }
        if (isPlaying) {
            return loop.isEnabled() ? "PLAY LOOP" : "PLAY";
        }
        return "";
    }

    public int tickCount() {
        if (isRecording) {
            return (int) Math.min(Integer.MAX_VALUE, recordingTick);
        }
        if (isPlaying && playback != null) {
            return playbackIndex;
        }
        return 0;
    }

    public float progress() {
        if (isPlaying && playback != null && !playback.ticks.isEmpty()) {
            return Math.clamp((float) playbackIndex / (float) playback.ticks.size(), 0.0f, 1.0f);
        }
        if (isRecording) {
            return (recordingTick % 200L) / 200.0f;
        }
        return 0.0f;
    }

    public void stopAll() {
        isRecording = false;
        stop(false);
        releaseAllKeys();
        currentModifiers = List.of();
        transitioning = false;
        currentPlaybackTick = null;
        clearPlaybackWaits();
    }

    public boolean hasModifier(String modifier) {
        return isPlaying && currentModifiers.contains(modifier);
    }

    public void recordChatOrCommand(String text, boolean command) {
        if (!isRecording || recording == null || text == null || text.isEmpty()) {
            return;
        }

        TinyTaskEvent event = new TinyTaskEvent();
        event.type = command ? TinyTaskEvent.Type.COMMAND : TinyTaskEvent.Type.CHAT;
        event.tickIndex = recordingTick;
        event.text = text;
        recording.events.add(event);
    }

    public void recordGuiClick(String screenClass, double mouseX, double mouseY, int button) {
        if (!isRecording || recording == null || screenClass == null) {
            return;
        }

        TinyTaskEvent event = new TinyTaskEvent();
        event.type = TinyTaskEvent.Type.GUI_CLICK;
        event.tickIndex = recordingTick;
        event.screenClass = screenClass;
        event.mouseX = mouseX;
        event.mouseY = mouseY;
        event.button = button;
        recording.events.add(event);
    }

    private void startRecording() {
        if (mc.player == null) {
            notify(NotificationType.ERROR, "TinyTask: wejdz do swiata.");
            return;
        }
        if (!module.isEnabled()) {
            module.enable();
        }

        if (isPlaying) {
            stop(false);
        }
        recording = new TinyTaskRecording();
        recording.name = TinyTaskStorage.sanitize(macroName.getText());
        recordingTick = 0L;
        isRecording = true;
        notify(NotificationType.SUCCESS, "TinyTask: nagrywanie start.");
    }

    private void stopRecordingAndSave() {
        if (!isRecording || recording == null) {
            notify(NotificationType.ERROR, "TinyTask: nic nie nagrywasz.");
            return;
        }

        isRecording = false;
        recording.name = TinyTaskStorage.sanitize(macroName.getText());

        try {
            TinyTaskStorage.save(recording);
            notify(NotificationType.SUCCESS, "TinyTask: zapisano " + recording.name + " (" + recording.ticks.size() + " tickow).");
        } catch (IOException e) {
            notify(NotificationType.ERROR, "TinyTask: blad zapisu.");
            e.printStackTrace();
        }
    }

    private void playSelectedOrLatest() {
        if (mc.player == null) {
            notify(NotificationType.ERROR, "TinyTask: wejdz do swiata.");
            return;
        }
        if (!module.isEnabled()) {
            module.enable();
        }

        String name = TinyTaskStorage.sanitize(macroName.getText());
        TinyTaskRecording loaded = null;

        try {
            loaded = TinyTaskStorage.load(name);
        } catch (IOException ignored) {
        }

        if (loaded == null) {
            String latest = TinyTaskStorage.latestName();
            if (!latest.isEmpty()) {
                try {
                    loaded = TinyTaskStorage.load(latest);
                    macroName.text(latest);
                } catch (IOException ignored) {
                }
            }
        }

        if (loaded == null || loaded.ticks.isEmpty()) {
            notify(NotificationType.ERROR, "TinyTask: brak makra do odtworzenia.");
            return;
        }

        playback = loaded;
        playbackEvents = indexEvents(loaded);
        playbackIndex = 0;
        clearPlaybackWaits();
        setupTransition(loaded);
        isRecording = false;
        isPlaying = true;
        notify(NotificationType.SUCCESS, "TinyTask: odtwarzam " + loaded.name + ".");
    }

    private void stop() {
        stop(true);
    }

    private void stop(boolean notify) {
        if (!isPlaying && !notify) {
            return;
        }

        isPlaying = false;
        transitioning = false;
        playback = null;
        playbackEvents = Map.of();
        playbackIndex = 0;
        currentModifiers = List.of();
        currentPlaybackTick = null;
        clearPlaybackWaits();
        releaseAllKeys();

        if (notify) {
            notify(NotificationType.ERROR, "TinyTask: stop.");
        }
    }

    private void recordTick() {
        TinyTaskTick tick = new TinyTaskTick();
        tick.tickIndex = recordingTick++;
        PlayerInput playerInput = mc.player.input == null ? PlayerInput.DEFAULT : mc.player.input.playerInput;
        tick.forward = mc.options.forwardKey.isPressed() || playerInput.forward();
        tick.back = mc.options.backKey.isPressed() || playerInput.backward();
        tick.left = mc.options.leftKey.isPressed() || playerInput.left();
        tick.right = mc.options.rightKey.isPressed() || playerInput.right();
        tick.jump = mc.options.jumpKey.isPressed() || playerInput.jump();
        tick.sneak = mc.options.sneakKey.isPressed() || playerInput.sneak();
        tick.sprint = mc.options.sprintKey.isPressed() || playerInput.sprint();
        tick.attack = mc.options.attackKey.isPressed();
        tick.use = mc.options.useKey.isPressed();
        tick.yaw = mc.player.getYaw();
        tick.pitch = mc.player.getPitch();
        tick.selectedSlot = mc.player.getInventory().selectedSlot;
        for (KeyBinding key : mc.options.allKeys) {
            if (key.isPressed()) {
                tick.pressedKeys.add(key.getTranslationKey());
            }

            int count = ((KeyBindingAccessor) key).bkclient$getTimesPressed();
            if (count > 0) {
                tick.timesPressed.put(key.getTranslationKey(), count);
            }
        }
        if (Screen.hasControlDown()) {
            tick.modifiers.add("CTRL");
        }
        if (Screen.hasShiftDown()) {
            tick.modifiers.add("SHIFT");
        }
        if (Screen.hasAltDown()) {
            tick.modifiers.add("ALT");
        }
        recording.ticks.add(tick);
    }

    private void tickPlayback() {
        if (playback == null || playback.ticks.isEmpty()) {
            stop(false);
            return;
        }

        if (transitioning) {
            tickTransition();
            return;
        }

        if (playbackWaitTicks > 0) {
            playbackWaitTicks--;
            currentPlaybackTick = null;
            releaseAllKeys();
            return;
        }

        if (screenCloseWaitTicks > 0) {
            currentPlaybackTick = null;
            releaseAllKeys();
            if (isScreenClosed()) {
                screenCloseWaitTicks = 0;
                playbackWaitTicks = GUI_SETTLE_TICKS;
            } else {
                screenCloseWaitTicks--;
            }
            return;
        }

        if (pendingEvent != null) {
            currentPlaybackTick = null;
            releaseAllKeys();
            if (tryDispatchEvent(pendingEvent)) {
                pendingEvent = null;
                pendingEventTicks = 0;
                playbackWaitTicks = GUI_SETTLE_TICKS;
            } else if (--pendingEventTicks <= 0) {
                pendingEvent = null;
                pendingEventTicks = 0;
            }
            return;
        }

        if (playbackIndex >= playback.ticks.size()) {
            if (loop.isEnabled()) {
                playbackIndex = 0;
                clearPlaybackWaits();
                setupTransition(playback);
                return;
            } else {
                stop(false);
                notify(NotificationType.SUCCESS, "TinyTask: zakonczono.");
                return;
            }
        }

        TinyTaskTick tick = playback.ticks.get(playbackIndex);
        if (shouldWaitBeforeWorldUse(tick)) {
            screenCloseWaitTicks = SCREEN_CLOSE_WAIT_TICKS;
            currentPlaybackTick = null;
            releaseAllKeys();
            return;
        }

        boolean waitAfterTick = applyTick(tick);
        for (TinyTaskEvent event : playbackEvents.getOrDefault(tick.tickIndex, List.of())) {
            if (!tryDispatchEvent(event)) {
                pendingEvent = event;
                pendingEventTicks = GUI_EVENT_WAIT_TICKS;
                break;
            }
            if (event.type == TinyTaskEvent.Type.GUI_CLICK) {
                waitAfterTick = true;
            }
        }

        playbackIndex++;
        if (waitAfterTick) {
            playbackWaitTicks = Math.max(playbackWaitTicks, GUI_SETTLE_TICKS);
        }
    }

    private boolean applyTick(TinyTaskTick tick) {
        releaseAllKeys();
        currentPlaybackTick = tick;

        Map<String, KeyBinding> keysById = KeyBindingAccessor.bkclient$getKeysById();
        if (keysById != null && tick.pressedKeys != null && !tick.pressedKeys.isEmpty()) {
            for (String keyId : tick.pressedKeys) {
                KeyBinding key = keysById.get(keyId);
                if (key != null) {
                    key.setPressed(true);
                }
            }

            if (tick.timesPressed != null) {
                for (Map.Entry<String, Integer> entry : tick.timesPressed.entrySet()) {
                    KeyBinding key = keysById.get(entry.getKey());
                    if (key != null) {
                        ((KeyBindingAccessor) key).bkclient$setTimesPressed(entry.getValue());
                    }
                }
            }
        } else {
            mc.options.attackKey.setPressed(tick.attack);
            mc.options.useKey.setPressed(tick.use);
        }

        mc.options.forwardKey.setPressed(tick.forward);
        mc.options.backKey.setPressed(tick.back);
        mc.options.leftKey.setPressed(tick.left);
        mc.options.rightKey.setPressed(tick.right);
        mc.options.jumpKey.setPressed(tick.jump);
        mc.options.sneakKey.setPressed(tick.sneak);
        mc.options.sprintKey.setPressed(tick.sprint);
        mc.options.attackKey.setPressed(tick.attack);
        mc.options.useKey.setPressed(tick.use);
        currentModifiers = tick.modifiers == null ? List.of() : tick.modifiers;
        mc.player.setYaw(tick.yaw);
        mc.player.setPitch(tick.pitch);
        mc.player.setBodyYaw(tick.yaw);
        mc.player.setHeadYaw(tick.yaw);
        mc.player.getInventory().selectedSlot = tick.selectedSlot;
        return closeHandledScreenFromTick(tick);
    }

    private boolean tryDispatchEvent(TinyTaskEvent event) {
        if (event.type == null) {
            return true;
        }

        switch (event.type) {
            case COMMAND -> {
                if (mc.player != null && event.text != null) {
                    mc.player.networkHandler.sendChatCommand(event.text.startsWith("/") ? event.text.substring(1) : event.text);
                }
                return true;
            }
            case CHAT -> {
                if (mc.player != null && event.text != null) {
                    mc.player.networkHandler.sendChatMessage(event.text);
                }
                return true;
            }
            case GUI_CLICK -> {
                Screen screen = mc.currentScreen;
                if (screen == null || event.screenClass == null || !screen.getClass().getName().equals(event.screenClass)) {
                    return false;
                }

                double mouseX = event.mouseX + screen.width / 2.0;
                double mouseY = event.mouseY + screen.height / 2.0;
                screen.mouseClicked(mouseX, mouseY, event.button);
                screen.mouseReleased(mouseX, mouseY, event.button);
                return true;
            }
        }
        return true;
    }

    private void setupTransition(TinyTaskRecording recording) {
        if (mc.player == null || recording == null || recording.ticks.isEmpty()) {
            transitioning = false;
            return;
        }

        TinyTaskTick firstTick = recording.ticks.get(0);
        startYaw = mc.player.getYaw();
        startPitch = mc.player.getPitch();
        targetYaw = firstTick.yaw;
        targetPitch = firstTick.pitch;
        transitionTicks = 0;

        float yawDiff = Math.abs(targetYaw - startYaw);
        float pitchDiff = Math.abs(targetPitch - startPitch);
        totalTransitionTicks = (int) Math.min(20.0f, Math.max(5.0f, (yawDiff + pitchDiff) / 10.0f));
        transitioning = totalTransitionTicks > 0;
    }

    private void tickTransition() {
        if (mc.player == null) {
            stop(false);
            return;
        }

        currentPlaybackTick = null;
        releaseAllKeys();
        transitionTicks++;
        float progress = Math.clamp((float) transitionTicks / (float) totalTransitionTicks, 0.0f, 1.0f);
        float smooth = progress * progress * (3.0f - 2.0f * progress);
        float yaw = startYaw + (targetYaw - startYaw) * smooth;
        float pitch = startPitch + (targetPitch - startPitch) * smooth;
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
        mc.player.setBodyYaw(yaw);
        mc.player.setHeadYaw(yaw);

        if (transitionTicks >= totalTransitionTicks) {
            transitioning = false;
        }
    }

    public void applyInputOverride(Input input) {
        if (!isPlaying || currentPlaybackTick == null || input == null) {
            return;
        }

        InputAccessor accessor = (InputAccessor) input;
        float movementForward = 0.0f;
        float movementSideways = 0.0f;
        if (currentPlaybackTick.forward) {
            movementForward += 1.0f;
        }
        if (currentPlaybackTick.back) {
            movementForward -= 1.0f;
        }
        if (currentPlaybackTick.left) {
            movementSideways += 1.0f;
        }
        if (currentPlaybackTick.right) {
            movementSideways -= 1.0f;
        }

        accessor.setMovementForward(movementForward);
        accessor.setMovementSideways(movementSideways);
        accessor.setInput(new PlayerInput(
                currentPlaybackTick.forward,
                currentPlaybackTick.back,
                currentPlaybackTick.left,
                currentPlaybackTick.right,
                currentPlaybackTick.jump,
                currentPlaybackTick.sneak,
                currentPlaybackTick.sprint));
    }

    private Map<Long, List<TinyTaskEvent>> indexEvents(TinyTaskRecording recording) {
        Map<Long, List<TinyTaskEvent>> result = new HashMap<>();

        for (TinyTaskEvent event : recording.events) {
            result.computeIfAbsent(event.tickIndex, ignored -> new ArrayList<>()).add(event);
        }

        return result;
    }

    private void releaseAllKeys() {
        if (mc.options == null) {
            return;
        }

        for (KeyBinding key : mc.options.allKeys) {
            key.setPressed(false);
        }
    }

    private boolean shouldWaitBeforeWorldUse(TinyTaskTick tick) {
        if (!tick.use || isScreenClosed() || worldUseWaitPlaybackIndex == playbackIndex) {
            return false;
        }

        worldUseWaitPlaybackIndex = playbackIndex;
        return true;
    }

    private boolean closeHandledScreenFromTick(TinyTaskTick tick) {
        if (mc.player == null || mc.options == null || mc.currentScreen == null || tick.timesPressed == null) {
            return false;
        }

        String inventoryKey = mc.options.inventoryKey.getTranslationKey();
        if (!tick.timesPressed.containsKey(inventoryKey)) {
            return false;
        }
        if (mc.player.currentScreenHandler == null || mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            return false;
        }

        mc.player.closeHandledScreen();
        screenCloseWaitTicks = SCREEN_CLOSE_WAIT_TICKS;
        return true;
    }

    private boolean isScreenClosed() {
        return mc.currentScreen == null
                && (mc.player == null || mc.player.currentScreenHandler == null
                        || mc.player.currentScreenHandler == mc.player.playerScreenHandler);
    }

    private void clearPlaybackWaits() {
        pendingEvent = null;
        pendingEventTicks = 0;
        playbackWaitTicks = 0;
        screenCloseWaitTicks = 0;
        worldUseWaitPlaybackIndex = -1;
    }

    private void notify(NotificationType type, String message) {
        try {
            BKClientInit.getInstance().getNotificationManager().addNotification(type, message);
        } catch (Exception ignored) {
        }
    }
}
