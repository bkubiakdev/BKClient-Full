package dev.bkclient.ui.mainmenu;

import com.google.gson.JsonObject;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

/** Remembers the last multiplayer server for the menu's "Continue" card; stored with the menu preferences. */
final class LastPlayedServer {
    private static final LastPlayedServer INSTANCE = new LastPlayedServer();
    private static boolean subscribed;
    private static boolean dirty;
    private static String name = "";
    private static String address = "";
    private static long playedAt;
    private static long savedAt;

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        MinecraftClient mc = MinecraftClient.getInstance();
        ServerInfo entry = mc.getCurrentServerEntry();
        if (entry == null || mc.isInSingleplayer() || entry.address == null || entry.address.isBlank()) return;
        boolean changed = !entry.address.equals(address) || !entry.name.equals(name);
        name = entry.name;
        address = entry.address;
        playedAt = System.currentTimeMillis();
        // Written as soon as a new server is joined, so closing the game while connected still keeps it.
        // The timestamp is refreshed in memory and saved when the menu opens after leaving.
        if (changed) {
            dirty = true;
            flush();
        }
    };

    private LastPlayedServer() {
    }

    static void ensureTracking() {
        if (subscribed) return;
        subscribed = true;
        BKClientInit.getInstance().getEventManager().subscribe(INSTANCE);
    }

    /** Called when the menu opens: persists the session that just ended. */
    static void flush() {
        if (!dirty && playedAt == savedAt) return;
        dirty = false;
        savedAt = playedAt;
        BKClientInit.getInstance().getFileManager().writeFile("client");
    }

    static boolean known() {
        return !address.isBlank();
    }

    static String name() {
        return name.isBlank() ? address : name;
    }

    static String address() {
        return address;
    }

    static long playedAt() {
        return playedAt;
    }

    static ServerInfo toServerInfo() {
        return new ServerInfo(name(), address, ServerInfo.ServerType.OTHER);
    }

    static void save(JsonObject out) {
        if (!known()) return;
        out.addProperty("LAST_SERVER_NAME", name);
        out.addProperty("LAST_SERVER_ADDRESS", address);
        out.addProperty("LAST_SERVER_TIME", playedAt);
    }

    static void load(JsonObject data) {
        try {
            if (data.has("LAST_SERVER_ADDRESS")) address = data.get("LAST_SERVER_ADDRESS").getAsString();
            if (data.has("LAST_SERVER_NAME")) name = data.get("LAST_SERVER_NAME").getAsString();
            if (data.has("LAST_SERVER_TIME")) playedAt = savedAt = data.get("LAST_SERVER_TIME").getAsLong();
        } catch (RuntimeException ignored) {
            name = address = "";
            playedAt = 0;
        }
    }
}
