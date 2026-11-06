package dev.bkclient.systems.modules.modules.bkmods.automarket;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

/**
 * Manages automatic reconnection after kicks or crashes.
 * Ported from bk-rynek's ReconnectController.
 * Fully tick-based \u2014 detects DisconnectedScreen and manages countdown.
 */
public final class ReconnectController {

    private String lastServerAddress;
    private ServerInfo lastServerInfo;
    private long reconnectAt = 0;
    private boolean pending = false;
    private boolean reconnecting = false;
    private Screen lastDisconnectScreen = null;
    private boolean cancelled = false;
    private Object autoLeaveConnection;

    private int delaySeconds = 8;

    public ReconnectController() {
    }


    public void setDelaySeconds(int seconds) {
        this.delaySeconds = Math.max(0, seconds);
    }

    public boolean isReconnecting() {
        return reconnecting;
    }

    public void setReconnecting(boolean val) {
        this.reconnecting = val;
    }


    /**
     * Called every tick from AutoMarket.tick().
     * Detects DisconnectedScreen and manages reconnection timing.
     */
    public void globalTick(boolean autoReconnectEnabled) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null)
            return;

        if (autoLeaveConnection != null) {
            // A manual join creates a different handler and restores normal reconnect behavior.
            if (client.player != null && client.getNetworkHandler() != null && client.getNetworkHandler() != autoLeaveConnection) {
                autoLeaveConnection = null;
            } else {
                cancel();
                return;
            }
        }

        // Keep the last multiplayer target independently of the AutoMarket module state.
        // Minecraft may clear the world before DisconnectedScreen is initialized, while the
        // current server entry can still be available for that short transition.
        rememberCurrentServer(client);

        if (!autoReconnectEnabled)
            return;

        // 2. Detect DisconnectedScreen by instance
        Screen currentScreen = client.currentScreen;
        if (currentScreen instanceof DisconnectedScreen) {
            onDisconnectScreenInit(currentScreen);
        } else {
            lastDisconnectScreen = null;
        }

        // 3. Reconnect when timer expires
        if (pending && (currentScreen instanceof DisconnectedScreen) && !cancelled
                && System.currentTimeMillis() >= reconnectAt) {
            reconnect();
        }
    }

    /**
     * Called when a DisconnectedScreen is initialized or detected.
     * Starts countdown once per screen instance.
     */
    public void onDisconnectScreenInit(Screen screen) {
        if (isAutoLeavePaused()) return;
        rememberCurrentServer(MinecraftClient.getInstance());
        if (screen != lastDisconnectScreen) {
            lastDisconnectScreen = screen;
            startCountdown();
        }
    }

    private void rememberCurrentServer(MinecraftClient client) {
        if (client == null) return;
        ServerInfo currentEntry = client.getCurrentServerEntry();
        if (currentEntry != null && currentEntry.address != null && !currentEntry.address.isBlank()) {
            lastServerInfo = currentEntry;
            lastServerAddress = currentEntry.address;
        }
    }

    /** Save server info (backup, also called from JOIN event). */
    public void setLastServer(ServerInfo info) {
        if (info != null) {
            lastServerInfo = info;
            lastServerAddress = info.address;
        }
    }


    private void startCountdown() {
        if (lastServerAddress == null && lastServerInfo == null) {
            return;
        }
        cancelled = false;
        reconnectAt = System.currentTimeMillis() + (delaySeconds * 1000L);
        pending = true;
    }

    public boolean isPending() {
        return pending;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public int getSecondsUntilReconnect() {
        long remaining = Math.max(0L, reconnectAt - System.currentTimeMillis());
        return (int) Math.ceil(remaining / 1000.0);
    }


    public void reconnect() {
        autoLeaveConnection = null;
        pending = false;
        cancelled = false;
        reconnecting = true;
        MinecraftClient client = MinecraftClient.getInstance();

        if (lastServerInfo != null) {
            String addrStr = lastServerAddress != null ? lastServerAddress : lastServerInfo.address;
            ConnectScreen.connect(client.currentScreen, client,
                    ServerAddress.parse(addrStr), lastServerInfo, false, null);
        } else if (lastServerAddress != null) {
            ServerInfo info = new ServerInfo("Auto-Reconnect", lastServerAddress,
                    ServerInfo.ServerType.OTHER);
            ConnectScreen.connect(client.currentScreen, client,
                    ServerAddress.parse(lastServerAddress), info, false, null);
        }
    }

    public void pauseAfterAutoLeave() {
        autoLeaveConnection = MinecraftClient.getInstance().getNetworkHandler();
        reconnecting = false;
        cancel();
    }

    public boolean isAutoLeavePaused() {
        return autoLeaveConnection != null;
    }

    public void cancel() {
        pending = false;
        cancelled = true;
    }

    public void reset() {
        pending = false;
        cancelled = false;
        reconnecting = false;
        lastDisconnectScreen = null;
    }

    public String getLastServerAddress() {
        return lastServerAddress;
    }
}

