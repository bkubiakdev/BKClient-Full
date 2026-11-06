package dev.bkclient.ui.menu.clickgui.components;

import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.ui.components.ColorPicker;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.components.SettingControls;
import dev.bkclient.ui.menu.clickgui.ClickGuiV3Screen;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import java.util.ArrayList;
import java.util.List;

/** Compact visual-effect controls, backed by the original module settings. */
public final class TargetStylingSection extends MenuSettingComponent<Setting> {
    private final List<Setting> settings;
    private final boolean genericLabels;
    private final ClickGuiV3Screen host;
    private final List<Row> rows = new ArrayList<>();
    private final java.util.Map<Setting, dev.bkclient.utility.animation.base.Animation> checks = new java.util.HashMap<>();
    private SliderSetting dragging;
    private float sliderX, sliderWidth;
    private ColorPicker picker;
    private ColorSetting pickerSetting;

    public TargetStylingSection(Setting alwaysVisible, List<Setting> settings, CustomComponent parent, ClickGuiV3Screen host) {
        this(alwaysVisible, settings, parent, host, false);
    }
    public TargetStylingSection(Setting alwaysVisible, List<Setting> settings, CustomComponent parent, ClickGuiV3Screen host, boolean genericLabels) {
        super(alwaysVisible, parent);
        this.genericLabels = genericLabels;
        this.settings = List.copyOf(settings);
        this.host = host;
    }
    @Override public float getHeight() {
        return 35 + (int)this.settings.stream().filter(Setting::isVisible).count() * 19;
    }
    @Override protected void renderComponent(UIContext context) {
        this.height = getHeight();
        this.rows.clear();
        if (this.picker != null) {
            this.pickerSetting.setColor(this.picker.built());
            if (!this.picker.isShowing()) {
                AbstractSetting.saveConfig();
                this.picker = null;
            }
        }
        if (this.dragging != null) {
            if (!this.dragging.isVisible() || org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1) finishDrag();
            else updateSlider(context.getMouseX());
        }
        float left = this.x + 8, w = this.width - 16;
        ColorRGBA text = Colors.getTextColor();
        LucideIcons.PALETTE.draw(context, left, this.y + 4, 10, text);
        context.drawText(Fonts.MEDIUM.getFont(8), "Styling", left + 14, this.y + 5, text);
        context.drawText(Fonts.REGULAR.getFont(6), this.genericLabels ? "Fine-tune the visual effect" : "Fine-tune the target effect", left, this.y + 18, text.mulAlpha(.5F));
        boolean dark = ClickGuiV2Theme.current().dark;
        float rowY = this.y + 31;
        for (Setting setting : this.settings) {
            if (!setting.isVisible()) continue;
            this.rows.add(new Row(setting, left, rowY, w));
            boolean hover = GuiUtility.isHovered(left, rowY, w, 17, context);
            context.drawRoundedRect(left, rowY, w, 17, BorderRadius.all(4),
                (dark ? ColorRGBA.BLACK : ColorRGBA.WHITE).mulAlpha(hover ? .6F : .35F));
            icon(setting).draw(context, left + 4, rowY + 4, 9, text.mulAlpha(.7F));
            float available = setting instanceof SliderSetting ? w - 91 : w - 49;
            this.drawSettingName(context, Fonts.MEDIUM.getFont(6.5F), label(setting), left + 17, rowY + 5.5F, text, available);
            if (setting instanceof SliderSetting slider) {
                float sx = left + w - 70, sw = 42;
                float percent = (slider.getCurrentValue() - slider.getMin()) / Math.max(.0001F, slider.getMax() - slider.getMin());
                SettingControls.cleanSlider(context, sx, rowY + 8, sw, 0, percent, false, 1);
                context.drawRightText(Fonts.REGULAR.getFont(6), number(slider.getCurrentValue()), left + w - 5, rowY + 6, text.mulAlpha(.6F));
            } else if (setting instanceof BooleanSetting value) {
                var animation = this.checks.computeIfAbsent(value, key -> new dev.bkclient.utility.animation.base.Animation(
                    220L, value.isEnabled() ? 1 : 0, dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT));
                SettingControls.option(context, left + w - 31, rowY + 3.5F, animation.update(value.isEnabled() ? 1 : 0), 0, 1);
            } else if (setting instanceof ColorSetting value) {
                dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(),
                    left + w - 14, rowY + 4.5F, 8, 8, BorderRadius.all(4), value.getColor().withAlpha(255));
            }
            rowY += 19;
        }
    }
    @Override protected boolean isSearchLabel(String text) {
        return this.searchLabelTarget != null && this.settings.contains(this.searchLabelTarget)
            && label(this.searchLabelTarget).equals(text);
    }

    public float searchOffset(Setting target) {
        float offset = 31;
        for (Setting setting : this.settings) {
            if (!setting.isVisible()) continue;
            if (setting == target) return offset;
            offset += 19;
        }
        return -1;
    }

    private static String number(float value) {
        return new java.math.BigDecimal(Float.toString(value)).setScale(3, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
    private LucideIcons icon(Setting setting) {
        String name = setting.getName();
        if (genericLabels) {
            String key = name.toLowerCase(java.util.Locale.ROOT);
            if (setting instanceof ColorSetting || key.contains("color") || key.contains("theme")) return LucideIcons.PALETTE;
            if (key.contains("speed") || key.contains("time") || key.contains("duration")) return LucideIcons.GAUGE;
            if (key.contains("size") || key.contains("radius") || key.contains("scale") || key.contains("distance")) return LucideIcons.EXPAND;
            if (key.contains("width") || key.contains("thickness") || key.contains("height")) return LucideIcons.RULER;
            if (key.contains("alpha") || key.contains("opacity") || key.contains("fill")) return LucideIcons.DROPLETS;
            if (key.contains("animation") || key.contains("wave") || key.contains("smooth")) return LucideIcons.WAVES;
            if (key.contains("light") || key.contains("glow") || key.contains("bloom")) return LucideIcons.SPARKLES;
            return setting instanceof BooleanSetting ? LucideIcons.EYE : LucideIcons.PRISM;
        }
        if (name.endsWith("distortion_strength")) return LucideIcons.PRISM;
        if (name.endsWith("distortion")) return LucideIcons.WAVES;
        if (name.endsWith("lighting")) return LucideIcons.SPARKLES;
        if (name.endsWith("speed")) return LucideIcons.GAUGE;
        if (name.endsWith("size")) return LucideIcons.EXPAND;
        if (name.endsWith("particle_thickness")) return LucideIcons.RULER;
        if (name.endsWith("change_color_on_damage")) return LucideIcons.DROPLETS;
        return LucideIcons.PALETTE;
    }

    private String label(Setting setting) {
        String name = setting.getName();
        if (genericLabels) {
            // Display aliases only: preserve setting identifiers used by saved configurations.
            if (name.equals("Second Color Picker") && setting instanceof ColorSetting) return "Second Color";
            if (name.equals("Second Color") && setting instanceof BooleanSetting) return "Use Second Color";
            return dev.bkclient.systems.localization.Localizator.translate(name);
        }
        if (name.endsWith("distortion_strength")) return "Refraction";
        if (name.endsWith("distortion")) return "Distortion";
        if (name.endsWith("lighting")) return "Highlights";
        if (name.endsWith("particle_thickness")) return "Thickness";
        if (name.endsWith("change_color_on_damage")) return "Damage tint";
        if (name.endsWith("speed")) return "Speed";
        if (name.endsWith("size")) return "Size";
        if (setting instanceof ColorSetting) return "Tint colour";
        return dev.bkclient.systems.localization.Localizator.translate(name);
    }
    @Override public void onMouseClicked(double mx, double my, MouseButton button) {
        if (button != MouseButton.LEFT) return;
        for (Row row : this.rows) {
            if (!row.setting.isVisible() || !GuiUtility.isHovered(row.x, row.y, row.width, 17, mx, my)) continue;
            if (row.setting instanceof SliderSetting slider) {
                this.sliderX = row.x + row.width - 70;
                this.sliderWidth = 42;
                if (GuiUtility.isHovered(this.sliderX - 4, row.y, this.sliderWidth + 8, 17, mx, my)) {
                    this.dragging = slider;
                    updateSlider(mx);
                }
            } else if (row.setting instanceof BooleanSetting value) {
                value.toggle();
                AbstractSetting.saveConfig();
            } else if (row.setting instanceof ColorSetting value) {
                if (this.picker != null) this.picker.setShowing(false);
                this.pickerSetting = value;
                this.picker = new ColorPicker((float)mx, (float)my, 6, value.isAlpha(), value.getColor(), label(value));
                this.host.getColorPickers().add(this.picker);
            }
            return;
        }
    }
    private void updateSlider(double mouseX) {
        float fraction = Math.clamp(((float)mouseX - this.sliderX) / this.sliderWidth, 0, 1);
        this.dragging.setCurrentValue(this.dragging.getMin() + fraction * (this.dragging.getMax() - this.dragging.getMin()));
    }
    private void finishDrag() {
        if (this.dragging != null) { this.dragging = null; AbstractSetting.saveConfig(); }
    }
    @Override public void onMouseReleased(double x, double y, MouseButton button) {
        if (button == MouseButton.LEFT) finishDrag();
    }
    private record Row(Setting setting, float x, float y, float width) { }
}
