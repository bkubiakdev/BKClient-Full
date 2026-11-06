package dev.bkclient.systems.modules.modules.other;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.modules.modules.other.AnticheatPingClassifier.Detection;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.game.MessageUtility;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.ProfilelessChatMessageS2CPacket;
import net.minecraft.text.Text;

/**
 * Guesses the server anticheat. Ping (transaction) packets are numbered differently by each anticheat,
 * so the ids right after joining act as a fingerprint; the server brand and chat/tab texts add hints.
 * Pings that match no known pattern are reported as Custom. Purely passive: nothing is sent to the server.
 */
@ModuleInfo(name = "Anticheat Detect", category = ModuleCategory.OTHER, desc = "modules.descriptions.anticheat_detect")
public class AnticheatDetect extends BaseModule {
    private static final int MAX_SAMPLES = 16;
    private static final int BRAND_CONFIDENCE = 90;
    private static final int TEXT_CONFIDENCE = 60;
    private static final int CUSTOM_CONFIDENCE = 50;
    private static final int CUSTOM_AFTER_PINGS = 40;
    private static final String[][] KEYWORDS = {
        {"grim", "Grim"}, {"vulcan", "Vulcan"}, {"matrix", "Matrix"}, {"intave", "Intave"},
        {"polar", "Polar"}, {"verus", "Verus"}, {"karhu", "Karhu"}, {"spartan", "Spartan"},
        {"themis", "Themis"}, {"grizzly", "Grizzly"}, {"negativity", "Negativity"},
        {"nocheatplus", "NoCheatPlus"}, {"ncp", "NoCheatPlus"}, {"aac", "AAC"}
    };

    private final SliderSetting samples = new SliderSetting(this, "modules.settings.anticheat_detect.samples")
        .min(5.0F).max(MAX_SAMPLES).currentValue(8.0F).step(1.0F);
    private final BooleanSetting brand = new BooleanSetting(this, "modules.settings.anticheat_detect.brand").enabled(true);
    private final BooleanSetting textHints = new BooleanSetting(this, "modules.settings.anticheat_detect.text_hints").enabled(true);
    private final BooleanSetting notify = new BooleanSetting(this, "modules.settings.anticheat_detect.notify").enabled(true);

    // Packets arrive on the network thread and are guarded by pingLock; results are reported on the client tick.
    private final Object pingLock = new Object();
    // First ids after joining: the known start ranges only apply to these.
    private final List<Integer> startIds = new ArrayList<>();
    // Latest ids: a single irregular id (e.g. a jump) must not block detection for the whole session.
    private final List<Integer> recentIds = new ArrayList<>();
    private int pingCount;
    private boolean newPings;
    private volatile String pendingHint;
    private boolean brandChecked;
    private int confidence;

    private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
        if (event.getPacket() instanceof GameJoinS2CPacket) {
            this.reset();
        } else if (event.getPacket() instanceof CommonPingS2CPacket ping) {
            int size = this.windowSize();
            synchronized (this.pingLock) {
                if (this.startIds.size() < size) this.startIds.add(ping.getParameter());
                this.recentIds.add(ping.getParameter());
                while (this.recentIds.size() > size) this.recentIds.remove(0);
                this.pingCount++;
                this.newPings = true;
            }
        } else if (this.textHints.isEnabled()) {
            if (event.getPacket() instanceof GameMessageS2CPacket packet) {
                this.scanText(packet.content());
            } else if (event.getPacket() instanceof ProfilelessChatMessageS2CPacket packet) {
                this.scanText(packet.message());
            } else if (event.getPacket() instanceof PlayerListHeaderS2CPacket packet) {
                this.scanText(packet.header());
                this.scanText(packet.footer());
            }
        }
    };

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (this.brand.isEnabled() && !this.brandChecked && mc.getNetworkHandler().getBrand() != null) {
            this.brandChecked = true;
            String name = findKeyword(mc.getNetworkHandler().getBrand());
            if (name != null) this.report(name, BRAND_CONFIDENCE);
        }

        String hint = this.pendingHint;
        if (hint != null) {
            this.pendingHint = null;
            this.report(hint, TEXT_CONFIDENCE);
        }

        int[] start;
        int[] recent;
        int count;
        synchronized (this.pingLock) {
            if (!this.newPings) return;
            this.newPings = false;
            start = toArray(this.startIds);
            recent = toArray(this.recentIds);
            count = this.pingCount;
        }
        this.report(AnticheatPingClassifier.classify(start));
        this.report(AnticheatPingClassifier.classify(recent));
        // The server keeps pinging, yet no known pattern showed up: a private (custom) anticheat.
        if (count >= CUSTOM_AFTER_PINGS && this.confidence == 0) this.report(AnticheatPingClassifier.CUSTOM, CUSTOM_CONFIDENCE);
    };

    @Override
    public void onEnable() {
        this.reset();
        super.onEnable();
    }

    private int windowSize() {
        return Math.round(this.samples.getCurrentValue());
    }

    private void reset() {
        synchronized (this.pingLock) {
            this.startIds.clear();
            this.recentIds.clear();
            this.pingCount = 0;
            this.newPings = false;
        }
        this.pendingHint = null;
        this.brandChecked = false;
        this.confidence = 0;
    }

    private static int[] toArray(List<Integer> ids) {
        return ids.stream().mapToInt(Integer::intValue).toArray();
    }

    private void scanText(Text text) {
        if (text == null) return;
        String name = findKeyword(text.getString());
        if (name != null) this.pendingHint = name;
    }

    private static String findKeyword(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String[] keyword : KEYWORDS) {
            // Whole words only, so e.g. "aac" does not match inside other words.
            if (Pattern.compile("\\b" + keyword[0] + "\\b").matcher(lower).find()) return keyword[1];
        }
        return null;
    }

    private void report(Detection detection) {
        if (detection != null) this.report(detection.name(), detection.confidence());
    }

    private void report(String name, int confidence) {
        // Windows are re-checked on every ping, so only a more confident result replaces the shown one.
        if (confidence <= this.confidence) return;
        this.confidence = confidence;
        String result = name + " (" + confidence + "%)";
        MessageUtility.info(Text.of(Localizator.translate("modules.anticheat_detect.detected", result)));
        if (this.notify.isEnabled()) {
            BKClientInit.getInstance().getNotificationManager().addThemedNotification(NotificationType.INFO,
                Localizator.translate("modules.anticheat_detect.notification.title"), result, 4000L);
        }
    }
}
