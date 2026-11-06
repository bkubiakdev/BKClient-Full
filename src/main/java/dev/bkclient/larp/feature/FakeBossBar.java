package dev.bkclient.larp.feature;

import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import dev.bkclient.larp.mixin.BossBarHudAccessor;

final class FakeBossBar {
    private final UUID id;
    private final BossBar.Color color;
    private final BossBar.Style style;

    FakeBossBar(String name, BossBar.Color color, BossBar.Style style) {
        this.id = UUID.nameUUIDFromBytes(("easyeventowka:" + name).getBytes());
        this.color = color;
        this.style = style;
    }

    void show(Text title, float percent) {
        Map<UUID, ClientBossBar> bars = FakeBossBar.bars();
        if (bars == null) {
            return;
        }
        ClientBossBar bar = bars.get(this.id);
        if (bar == null) {
            bar = new ClientBossBar(this.id, title, percent, this.color, this.style, false, false, false);
            bars.put(this.id, bar);
        } else {
            bar.setName(title);
        }
        bar.setPercent(Math.max(0.0f, Math.min(1.0f, percent)));
    }

    void hide() {
        Map<UUID, ClientBossBar> bars = FakeBossBar.bars();
        if (bars != null) {
            bars.remove(this.id);
        }
    }

    private static Map<UUID, ClientBossBar> bars() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) {
            return null;
        }
        return ((BossBarHudAccessor)client.inGameHud.getBossBarHud()).easyeventowka$getBossBars();
    }
}
