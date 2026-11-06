package dev.bkclient.ui.menu.clickgui.components;

import dev.bkclient.BKClientInit;
import dev.bkclient.ui.menu.clickgui.GuiProfiler;
import static dev.bkclient.ui.menu.clickgui.GuiProfiler.Mode.*;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.ModuleFavorites;
import dev.bkclient.ui.components.BadgeText;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.gui.GuiUtility;
import java.util.function.Consumer;

/** Compact, two-line light card for the screenshot-based ClickGUIv2. */
public final class ReferenceModuleCard extends ClickGuiModuleCard {
    private final Consumer<ClickGuiModuleCard> opener;
    private final Animation enabled = new Animation(150L, Easing.QUARTIC_OUT);
    private final Animation hover = new Animation(160L, Easing.QUARTIC_OUT);
    private boolean binding;
    private boolean grouped;
    private final LucideIcons categoryIcon;

    public void renderGrouped(UIContext context) {
        grouped = true;
        try { render(context); } finally { grouped = false; }
    }

    public void renderCardBackground(UIContext context) {
        this.hover.update(this.isHovered(context));
        if (ClickGuiV2Theme.current().liquid) {
            context.drawClientRect(this.x, this.y, this.width, this.height, 1, 0, 7);
            ColorRGBA tint = ClickGuiV2Theme.current().text.mulAlpha(0.04F * this.hover.getValue());
            if ((tint.getRGB() >>> 24) != 0) {
                context.drawRoundedRect(this.x, this.y, this.width, this.height, BorderRadius.all(7), tint);
            }
        } else {
            context.drawRoundedRect(this.x, this.y, this.width, 30, BorderRadius.all(5),
                ClickGuiV2Theme.current().surface.mix(ClickGuiV2Theme.current().hover, this.hover.getValue()));
        }
    }

    public void renderCardToggle(UIContext context, boolean thumb) {
        float x = this.x + this.width - 33, y = this.y + 6;
        if (thumb) dev.bkclient.ui.components.SettingControls.toggleThumb(context, x, y, this.enabled.getValue(), 0, 1);
        else dev.bkclient.ui.components.SettingControls.toggleTrack(context, x, y, this.enabled.getValue(), 1);
    }

    private String keyLabel() {
        return this.binding ? "..." : this.getModule().getKey() < 0 ? "" : TextUtility.getKeyName(this.getModule().getKey());
    }

    public void renderCardText(UIContext context, boolean description) {
        if (description) {
            context.drawFadeoutText(Fonts.REGULAR.getFont(6), this.getModule().getDescription(), this.x + 22, this.y + 17,
                ClickGuiV2Theme.current().muted, 0.85F, 1, Math.max(0, this.width - 71));
        } else if (keyLabel().isEmpty()) {
            context.drawFadeoutText(Fonts.MEDIUM.getFont(7), this.getModule().getName(), this.x + 22, this.y + 7,
                ClickGuiV2Theme.current().text, 0.85F, 1, Math.max(8, this.width - 75));
        }
    }

    public void renderCardIcons(UIContext context) {
        if (categoryIcon != null) categoryIcon.draw(context, this.x + 6, this.y + 9, 12, ClickGuiV2Theme.current().text);
        boolean favorite = ModuleFavorites.contains(this.getModule().getName());
        (favorite ? LucideIcons.STAR_FILLED : LucideIcons.STAR).draw(context,
            this.x + this.width - 46, this.y + 7, 8,
            favorite ? new ColorRGBA(214, 164, 62) : ClickGuiV2Theme.current().muted);
    }

    public ReferenceModuleCard(Module module, Consumer<ClickGuiModuleCard> opener) {
        super(module, opener);
        this.opener = opener;
        LucideIcons icon = null;
        for (var category : dev.bkclient.ui.menu.api.MenuCategory.values()) {
            if (category.getCategory() == module.getCategory()) { icon = LucideIcons.category(category); break; }
        }
        this.categoryIcon = icon;
        this.height = 30;
        this.enabled.setValue(module.isEnabled() ? 1 : 0);
    }

    @Override protected void renderComponent(UIContext context) {
        this.height = 30;
        if (!grouped) this.hover.update(this.isHovered(context));
        this.enabled.update(this.getModule().isEnabled());
        if (!grouped && !GuiProfiler.hides(NO_BACKGROUNDS)) renderCardBackground(context);
        ColorRGBA ink = ClickGuiV2Theme.current().text;
        if (!grouped && !GuiProfiler.hides(NO_ICONS) && categoryIcon != null) categoryIcon.draw(context, this.x + 6, this.y + 9, 12, ink);
        String key = keyLabel();
        float keyWidth = key.isEmpty() ? 0 : Math.min(48, Math.max(12, Fonts.MEDIUM.getFont(5).width(key) + 5));
        float nameWidth = this.width - 75 - keyWidth;
        float favoriteX = this.x + this.width - 46;
        float descriptionWidth = Math.max(0, favoriteX - 3 - (this.x + 22));
        if (!GuiProfiler.hides(NO_TEXT)) {
        if (!grouped || !key.isEmpty()) context.drawFadeoutText(Fonts.MEDIUM.getFont(7), this.getModule().getName(), this.x + 22, this.y + 7,
            ink, 0.85F, 1, Math.max(8, nameWidth));
        if (!grouped) context.drawFadeoutText(Fonts.REGULAR.getFont(6), this.getModule().getDescription(), this.x + 22, this.y + 17,
            ClickGuiV2Theme.current().muted, 0.85F, 1, descriptionWidth);
        }
        if (!key.isEmpty()) {
            float bx = this.x + this.width - 50 - keyWidth;
            if (!GuiProfiler.hides(NO_BACKGROUNDS)) context.drawRoundedRect(bx, this.y + 6, keyWidth, 10, BorderRadius.all(3), ClickGuiV2Theme.current().field);
            if (!GuiProfiler.hides(NO_TEXT)) BadgeText.draw(context, Fonts.MEDIUM.getFont(5), key, bx + keyWidth / 2, this.y + 11, ink);
        }
        boolean favorite = ModuleFavorites.contains(this.getModule().getName());
        if (!grouped && !GuiProfiler.hides(NO_ICONS)) (favorite ? LucideIcons.STAR_FILLED : LucideIcons.STAR).draw(context,
            favoriteX, this.y + 7, 8, favorite ? new ColorRGBA(214, 164, 62) : ClickGuiV2Theme.current().muted);
        if (!grouped && !GuiProfiler.hides(NO_TOGGLES)) dev.bkclient.ui.components.SettingControls.toggle(context, this.x + this.width - 33, this.y + 6,
            this.enabled.getValue(), 0, 1.0F);
    }

    @Override public void onCardMouseClicked(double x, double y, MouseButton button) {
        if (!this.isHovered(x, y)) return;
        if (button == MouseButton.MIDDLE) { this.binding = true; return; }
        if (button == MouseButton.RIGHT) { this.opener.accept(this); return; }
        if (button != MouseButton.LEFT) return;
        if (GuiUtility.isHovered(this.x + this.width - 48, this.y + 4, 12, 14, x, y)) {
            ModuleFavorites.toggle(this.getModule().getName());
            BKClientInit.getInstance().getFileManager().writeFile("client");
        } else if (GuiUtility.isHovered(this.x + this.width - 33, this.y + 4, 26, 14, x, y)) {
            this.getModule().toggle();
        } else {
            this.opener.accept(this);
        }
    }
    @Override public void onMouseReleased(double x, double y, MouseButton button) {
        // State changes only on click; releasing or moving the pointer has no effect.
    }
    @Override public boolean isBinding() { return this.binding; }
    @Override public void clearBinding() { this.binding = false; }
    @Override public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.binding) {
            this.getModule().setKey(keyCode == 256 || keyCode == 261 ? -1 : keyCode);
            this.binding = false;
        }
    }
}
