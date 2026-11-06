package dev.bkclient.utility.game;

import dev.bkclient.BKClientInit;
import dev.bkclient.utility.concurrent.ClientAsyncExecutor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.net.URI;

public final class SupportNotice {
    private static final String DONATE_URL = "https://donate.bkubiak.dev";
    private static boolean initialized;
    private static boolean shown;
    private static int pendingTicks = -1;

    private SupportNotice() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ClientAsyncExecutor.execute(() -> {
            for (String url : new String[] { "https://bkubiak.dev/mods", "https://dc.bkubiak.dev" }) {
                try {
                    Util.getOperatingSystem().open(URI.create(url));
                } catch (RuntimeException error) {
                    BKClientInit.LOGGER.warn("Could not open startup page {}", url, error);
                }
            }
        }));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!shown) pendingTicks = 20;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> pendingTicks = -1);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (shown || pendingTicks < 0 || client.player == null || client.world == null) return;
            if (pendingTicks-- > 0) return;
            Text link = Text.literal(DONATE_URL).styled(style -> style
                    .withColor(Formatting.AQUA)
                    .withUnderline(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, DONATE_URL)));
            client.inGameHud.getChatHud().addMessage(Text.literal(
                    "[BKClient] Using this source code? You can support us: ").append(link));
            shown = true;
        });
    }
}
