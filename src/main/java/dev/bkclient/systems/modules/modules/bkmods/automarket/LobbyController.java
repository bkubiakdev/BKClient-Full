package dev.bkclient.systems.modules.modules.bkmods.automarket;

import dev.bkclient.utility.concurrent.ClientAsyncExecutor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.List;

/**
 * Lobby automation controller \u2014 handles automated movement and server
 * selection.
 * Ported from bk-rynek's LobbyAutomationController.
 * Hardcoded for anarchia (red dye / "anarchia" keyword).
 */
public final class LobbyController {

    private boolean guiScanPending = false;
    private long lastActionMs = 0;
    private boolean flowRunning = false;
    public boolean isAutoReconnectedContext = false;

    private MoveState currentState = MoveState.IDLE;
    private int stateTicks = 0;
    private float initialYaw = 0;
    private float initialPitch = 0;

    // Callback to start scanning after lobby flow completes
    private Runnable onFlowComplete;

    private enum MoveState {
        IDLE, START_WAIT, WALK_SHORT, STOP_WAIT, WALK_LONG, CLICK_COMPASS
    }

    public LobbyController() {
    }

    public void setOnFlowComplete(Runnable callback) {
        this.onFlowComplete = callback;
    }


    public void startLobbyFlow() {
        if (flowRunning)
            return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            initialYaw = client.player.getYaw();
            initialPitch = client.player.getPitch();
        }
        flowRunning = true;
        currentState = MoveState.START_WAIT;
        stateTicks = 20; // 1s wait
    }

    /**
     * Starts a fresh verification sequence once reconnect has produced a player.
     * This also closes the race where the action-bar prompt arrives before the
     * next AutoMarket module tick.
     */
    public void onAutoReconnectComplete() {
        stopAll();
        isAutoReconnectedContext = true;
        startLobbyFlow();
    }

    public void stopAll() {
        flowRunning = false;
        currentState = MoveState.IDLE;
        stateTicks = 0;
        guiScanPending = false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            client.options.forwardKey.setPressed(false);
        }
    }

    public boolean isFlowRunning() {
        return flowRunning;
    }


    public void onTick() {
        if (!flowRunning)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) {
            if (client.options != null) {
                client.options.forwardKey.setPressed(false);
            }
            return;
        }

        if (stateTicks > 0) {
            stateTicks--;

            if (currentState == MoveState.WALK_SHORT || currentState == MoveState.WALK_LONG) {
                client.options.forwardKey.setPressed(true);

                // Head wobble only in WALK_LONG (anti-AFK)
                if (currentState == MoveState.WALK_LONG) {
                    float yawOffset = (float) Math.sin(stateTicks * 0.2) * 10.0f;
                    float pitchOffset = (float) Math.cos(stateTicks * 0.1) * 5.0f;
                    client.player.setYaw(initialYaw + yawOffset);
                    client.player.setPitch(initialPitch + pitchOffset);
                }
            } else {
                client.options.forwardKey.setPressed(false);
            }

            // State transitions
            if (stateTicks == 0) {
                switch (currentState) {
                    case START_WAIT:
                        currentState = MoveState.WALK_SHORT;
                        stateTicks = 20; // 1s walk
                        break;
                    case WALK_SHORT:
                        currentState = MoveState.STOP_WAIT;
                        stateTicks = 20; // 1s stop
                        break;
                    case STOP_WAIT:
                        if (client.player != null) {
                            initialYaw = client.player.getYaw();
                            initialPitch = client.player.getPitch();
                        }
                        currentState = MoveState.WALK_LONG;
                        stateTicks = 80; // 4s walk + head wobble
                        break;
                    case WALK_LONG:
                        client.options.forwardKey.setPressed(false);
                        if (client.player != null) {
                            client.player.setYaw(initialYaw);
                            client.player.setPitch(initialPitch);
                        }
                        currentState = MoveState.IDLE;
                        break;
                    case CLICK_COMPASS:
                        if (client.player != null && client.interactionManager != null) {
                            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                            client.player.swingHand(Hand.MAIN_HAND);
                        }
                        guiScanPending = true;
                        lastActionMs = System.currentTimeMillis();
                        currentState = MoveState.IDLE;
                        break;
                    default:
                        break;
                }
            }
        }
    }


    public void onChatMessage(Text message, boolean overlay) {
        String text = message.getString().toLowerCase();

        // 1. Action Bar verification prompt
        if (overlay && (text.contains("weryfikacja pozycji") || text.contains("zacznij")
                || text.contains("porusza"))) {
            if (!isAutoReconnectedContext)
                return;
            startLobbyFlow();
            return;
        }

        if (!flowRunning)
            return;

        if (text.contains("trwa przenoszenie")) {
            stopAll();
            return;
        }

        if (text.contains("wyboru trybu") || text.contains("wybierz tryb")
                || text.contains("wybierz serwer") || text.contains("zapraszam do wyboru")
                || text.contains("zweryfikowano czynnik zaufania")) {
            useCompass();
        }
    }


    private void useCompass() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        // Stop movement but keep flow alive for GUI scan
        currentState = MoveState.IDLE;
        stateTicks = 0;
        if (client.options != null)
            client.options.forwardKey.setPressed(false);

        client.execute(() -> {
            int compassSlot = -1;
            for (int i = 0; i < 9; i++) {
                if (client.player.getInventory().getStack(i).isOf(Items.COMPASS)) {
                    compassSlot = i;
                    break;
                }
            }
            if (compassSlot != -1) {
                client.player.getInventory().selectedSlot = compassSlot;
                currentState = MoveState.CLICK_COMPASS;
                stateTicks = 10;
            }
        });
    }


    /**
     * Called from HandledScreenMixin's onRender.
     * Scans GUI for the anarchia server item and clicks it.
     */
    public void onScreenRender(Text title, ScreenHandler handler, List<Slot> slots) {
        // Allow clicking server selection GUI if: lobby flow active OR auto-reconnect context
        boolean canClick = (flowRunning && guiScanPending) || isAutoReconnectedContext;
        if (!canClick || System.currentTimeMillis() - lastActionMs < 500)
            return;

        for (Slot slot : slots) {
            if (slot.id > 53)
                continue;
            if (!slot.hasStack())
                continue;
            ItemStack stack = slot.getStack();
            String name = stack.getName().getString().toLowerCase();
            // Anarchia server detection: red dye item OR name contains "anarchia"
            if (stack.isOf(Items.RED_DYE) || name.contains("anarchia")) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.interactionManager.clickSlot(handler.syncId, slot.id, 0,
                        SlotActionType.PICKUP, client.player);
                guiScanPending = false;
                flowRunning = false;
                isAutoReconnectedContext = false;
                lastActionMs = System.currentTimeMillis();

                // After joining server \u2014 start market scanning after delay
                ClientAsyncExecutor.execute(() -> {
                    try {
                        Thread.sleep(8000);
                        client.execute(() -> {
                            if (client.player != null && onFlowComplete != null) {
                                onFlowComplete.run();
                            }
                        });
                    } catch (InterruptedException ignored) {
                    }
                });
                break;
            }
        }
    }

    /**
     * Returns true when lobby flow is actively walking.
     * Used by KeyboardInputMixin to force forward movement input.
     */
    public boolean shouldForceForward() {
        return flowRunning
                && (currentState == MoveState.WALK_SHORT || currentState == MoveState.WALK_LONG)
                && stateTicks > 0;
    }

    public void reset() {
        stopAll();
        isAutoReconnectedContext = false;
    }
}
