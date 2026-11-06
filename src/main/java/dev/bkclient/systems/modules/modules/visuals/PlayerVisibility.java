package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.settings.StringSetting;
import net.minecraft.entity.Entity;

@ModuleInfo(name = "Player Visibility", category = ModuleCategory.VISUALS, desc = "modules.descriptions.player_visibility")
public class PlayerVisibility extends BaseModule {
    private final StringSetting keywords = new StringSetting(this, "modules.settings.player_visibility.keywords").text("UPDATE, KOWAL, KLIKNIJ");

    @Override
    public void onEnable() {
        BKClientInit.getInstance().getNotificationManager().addNotificationOther(
            NotificationType.SUCCESS, "Player Visibility", "Player filtering enabled");
        super.onEnable();
    }

    @Override
    public void onDisable() {
        BKClientInit.getInstance().getNotificationManager().addNotificationOther(
            NotificationType.ERROR, "Player Visibility", "Player filtering disabled");
        super.onDisable();
    }

    public static boolean isNpc(Entity entity) {
        if (entity.getUuid().version() == 2) return true;
        PlayerVisibility module = BKClientInit.getInstance().getModuleManager().getModule(PlayerVisibility.class);
        String[] normalized = module.parsedKeywords();
        if (normalized.length == 0) return false;
        String name = entity.getName().getString().toUpperCase();
        for (String keyword : normalized) {
            if (name.contains(keyword)) return true;
        }
        return false;
    }

    /** Raw setting text and its split/trim/upper-cased non-empty keywords, in original order. */
    private record ParsedKeywords(String source, String[] keywords) {}
    private volatile ParsedKeywords parsed;

    private String[] parsedKeywords() {
        String text = this.keywords.getText();
        ParsedKeywords current = this.parsed;
        if (current != null && current.source().equals(text)) return current.keywords();
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String keyword : text.split(",")) {
            String normalized = keyword.trim().toUpperCase();
            if (!normalized.isEmpty()) list.add(normalized);
        }
        String[] result = list.toArray(new String[0]);
        this.parsed = new ParsedKeywords(text, result);
        return result;
    }
}
