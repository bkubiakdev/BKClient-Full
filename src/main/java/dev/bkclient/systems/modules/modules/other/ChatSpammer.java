package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.utility.time.Timer;

@ModuleInfo(name = "Chat Spammer", category = ModuleCategory.OTHER, desc = "Sends a configured message at fixed intervals")
public class ChatSpammer extends BaseModule {
    public final StringSetting message = new StringSetting(this, "modules.settings.chat_spammer.message").text("");
    public final SliderSetting interval = new SliderSetting(this, "modules.settings.chat_spammer.interval")
        .min(5.0F).max(60.0F).step(1.0F).currentValue(5.0F).suffix("s");
    private static final String[] SUFFIXES = {"!", "!!", "!!!", "!!!!", "!!!!!"};
    private final Timer timer = new Timer();
    private int suffixIndex;

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (mc.player == null || mc.player.networkHandler == null) return;
        if (!timer.finished((long) (interval.getCurrentValue() * 1000.0F))) return;
        timer.reset();
        String text = message.getText();
        if (text == null || text.isBlank()) return;
        String outgoing = text.trim() + " " + SUFFIXES[suffixIndex++ % SUFFIXES.length];
        mc.player.networkHandler.sendChatMessage(outgoing.substring(0, Math.min(outgoing.length(), 255)));
    };

    @Override
    public void onDisable() {
        suffixIndex = 0;
        timer.reset();
        super.onDisable();
    }
}
