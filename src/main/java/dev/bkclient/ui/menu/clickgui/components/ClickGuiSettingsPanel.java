package dev.bkclient.ui.menu.clickgui.components;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.ui.menu.spotlight.components.SpotlightModule;
import dev.bkclient.ui.menu.spotlight.components.SpotlightSettings;
import dev.bkclient.utility.gui.GuiUtility;

/** Reuses Spotlight's animated, scrollable Liquid settings window. */
public final class ClickGuiSettingsPanel extends SpotlightSettings {
    private final ClickGuiModuleCard card;
    private final int slot;
    private boolean detached;
    public ClickGuiSettingsPanel(ClickGuiModuleCard card, dev.bkclient.ui.menu.MenuScreen host, int slot, float x, float y) {
        super(new SpotlightModule(card.getModule(), null, null), x, y, 170, host);
        this.card = card;
        this.slot = slot;
        this.height = 150;
        if (host instanceof dev.bkclient.ui.menu.clickgui.ClickGuiV3Screen v3
                && card.getModule() instanceof dev.bkclient.systems.modules.modules.visuals.TargetESP) {
            java.util.List<dev.bkclient.systems.setting.Setting> styling = new java.util.ArrayList<>();
            dev.bkclient.systems.setting.Setting anchor = null;
            for (var component : this.getComponents()) {
                var setting = component.getSetting();
                String name = setting.getName();
                if (setting instanceof dev.bkclient.systems.setting.settings.SliderSetting
                        || setting instanceof dev.bkclient.systems.setting.settings.ColorSetting
                        || name.endsWith(".distortion") || name.endsWith(".lighting") || name.endsWith(".change_color_on_damage")) {
                    styling.add(setting);
                    if (name.endsWith(".speed")) anchor = setting;
                }
            }
            if (anchor != null) {
                this.getComponents().removeIf(component -> styling.contains(component.getSetting()));
                this.getComponents().add(new TargetStylingSection(anchor, styling, this, v3));
            }
        }
        if (host instanceof dev.bkclient.ui.menu.clickgui.ClickGuiV3Screen v3
                && visualStylingModule(card.getModule().getClass().getSimpleName())) {
            java.util.List<dev.bkclient.systems.setting.Setting> styling = new java.util.ArrayList<>();
            for (var component : this.getComponents()) {
                var setting = component.getSetting();
                if (setting instanceof dev.bkclient.systems.setting.settings.SliderSetting
                        || setting instanceof dev.bkclient.systems.setting.settings.ColorSetting
                        || setting instanceof dev.bkclient.systems.setting.settings.BooleanSetting) styling.add(setting);
            }
            if (!styling.isEmpty()) {
                // The section stays available when its first setting is hidden by another mode.
                var anchor = new dev.bkclient.systems.setting.settings.BooleanSetting(
                        () -> new java.util.ArrayList<>(), "Styling", () -> styling.stream().noneMatch(dev.bkclient.systems.setting.Setting::isVisible));
                this.getComponents().removeIf(component -> styling.contains(component.getSetting()));
                this.getComponents().add(new TargetStylingSection(anchor, styling, this, v3, true));
            }
        }
    }
    private static boolean visualStylingModule(String name) {
        return switch (name) {
            case "Ambience", "AspectRatio", "Beautifully", "BlockOutline", "ChinaHat", "Crosshair",
                    "CustomFog", "CustomHotbar", "CustomSwords", "ESP", "FriendMarkers", "GlassVapor",
                    "HandsShader", "ItemPhysics", "JumpCircles", "KillEffects",
                    "LogoutPos", "LogoutSpot", "Particles", "Pet", "PickBlowup",
                    "PopChams", "Prediction", "ShaderESP", "StorageESP",
                    "SwingAnimation", "TNTTimer", "Tracers", "Trails", "TrapESP", "ViewModel", "World" -> true;
            default -> false;
        };
    }
    public ClickGuiModuleCard getCard() { return this.card; }
    public int getSlot() { return this.slot; }
    public void dock(float x, float y) { if (!this.detached) this.pos(x, y); }
    public float contentHeight() {
        float total = 51.0F; // Title, module toggle and bottom padding.
        for (var component : this.getComponents()) {
            if (component.getSetting().isVisible()) total += component.getHeight();
        }
        return total;
    }
    public boolean needsFullHeight() { return this.contentHeight() > 155.0F; }
    @Override protected float maxPanelHeight() { return 320.0F; }
    @Override public void render(UIContext context) {
        this.getModule().set(this.card.getX(), this.card.getY(), this.card.getWidth(), 24);
        super.render(context);
    }
    @Override public void onMouseClicked(double x, double y, MouseButton button) {
        if (button == MouseButton.LEFT && GuiUtility.isHovered(this.x, this.y, this.width - 40, 24, x, y)) this.detached = true;
        super.onMouseClicked(x, y, button);
    }
}
