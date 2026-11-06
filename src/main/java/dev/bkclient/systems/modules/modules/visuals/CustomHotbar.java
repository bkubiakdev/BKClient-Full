package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.utility.render.HotbarRendered;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

@ModuleInfo(name = "Custom Hotbar", category = ModuleCategory.VISUALS)
public final class CustomHotbar extends BaseModule {
    private final ColorSetting backgroundColor = new ColorSetting(this, "modules.settings.custom_hotbar.background_color")
        .alpha(false).color(new ColorRGBA(30, 33, 39));
    private final HotbarRendered renderer = new HotbarRendered();
    private final BooleanSetting customHearts = new BooleanSetting(this, "modules.settings.custom_hotbar.hearts");
    private final BooleanSetting customFood = new BooleanSetting(this, "modules.settings.custom_hotbar.food");
    private final BooleanSetting customArmor = new BooleanSetting(this, "modules.settings.custom_hotbar.armor");

    public static boolean replacesHearts() { return replacesHud() && get().customHearts.isEnabled(); }
    public static boolean replacesFood() { return replacesHud() && get().customFood.isEnabled(); }
    public static boolean replacesArmor() { return replacesHud() && get().customArmor.isEnabled(); }

    public static CustomHotbar get() {
        if (BKClientInit.getInstance() == null || BKClientInit.getInstance().getModuleManager() == null) return null;
        return BKClientInit.getInstance().getModuleManager().getModule(CustomHotbar.class);
    }

    public static boolean active() {
        CustomHotbar module = get();
        return module != null && module.isEnabled();
    }

    public static boolean replacesHud() {
        return active() && mc.player != null && mc.interactionManager != null && !mc.player.isSpectator()
            && mc.getCameraEntity() == mc.player;
    }

    public void renderHotbar(DrawContext context, RenderTickCounter counter) {
        renderer.render(context, counter, backgroundColor.getColor(), customHearts.isEnabled(), customFood.isEnabled(), customArmor.isEnabled());
    }

    @Override public void onEnable() { renderer.reset(); }
    @Override public void onDisable() { renderer.reset(); }
}
