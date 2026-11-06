/*
 * CsBindRow — BindSetting: nazwa po lewej, kwadrat klawisza po prawej.
 * Klik → nasłuch ("..."); następna klawisza/mysz bindluje; Esc/Del = brak.
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.TextUtility;

public class CsBindRow implements CsSetting {

    private static final float HEIGHT = 14.0f;
    private static final float CHIP_H = 8.0f;      // Figma: 16
    private static final float ICON = 7.5f;        // ikona klawiatury 'i'
    private static final float PAD = 3.0f;         // wewnętrzny odstęp chipa
    private static final float GAP = 3.0f;         // ikona ↔ tekst
    private static final float TRASH_BOX = 10.0f;   // Figma: 16
    private static final float TRASH_ICON = 6.5f;
    private static final float TRASH_GAP = 3.0f;   // kosz ↔ chip

    private final BindSetting setting;
    private boolean listening;
    private final CsNameLabel nameLabel = new CsNameLabel();
    private float chipX, chipY, chipW;
    private float trashX, trashY;
    private boolean trashShown;

    public CsBindRow(BindSetting setting) {
        this.setting = setting;
    }

    @Override
    public Setting getSetting() {
        return this.setting;
    }

    @Override
    public float getHeight() {
        return HEIGHT;
    }

    @Override
    public boolean isCapturingInput() {
        return this.listening;
    }

    @Override
    public void render(UIContext context, float x, float y, float w, float alpha) {
        float centerY = y + HEIGHT / 2.0f;

        boolean bound = this.setting.getKey() != -1;
        Font keyFont = Fonts.SEMIBOLD.getFont(6.5f);
        String label = this.listening ? "..." : (bound ? TextUtility.getKeyName(this.setting.getKey()) : "n / a");

        float textW = keyFont.width(label);
        this.chipW = PAD + ICON + GAP + textW + PAD;
        this.chipX = x + w - this.chipW;
        this.chipY = centerY - CHIP_H / 2.0f;

        this.trashShown = bound && !this.listening;
        float leftmost = this.chipX;
        if (this.trashShown) {
            this.trashX = this.chipX - TRASH_GAP - TRASH_BOX;
            this.trashY = centerY - TRASH_BOX / 2.0f;
            leftmost = this.trashX;
        }

        Font font = Fonts.SEMIBOLD.getFont(CsTheme.Slider.NAME_FONT + 1.0f);
        boolean hovered = context.getMouseX() >= x && context.getMouseX() <= x + w
                && context.getMouseY() >= y && context.getMouseY() <= y + HEIGHT;
        this.nameLabel.render(context, font, Localizator.translate(this.setting.getName()), x, centerY,
                leftmost - x - 5.0f, hovered, CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));

        BorderRadius r = BorderRadius.all(2.0f);  // Figma: 4
        ColorRGBA chipBg = bound ? CsTheme.Color.accent() : CsTheme.Color.SUB_BORDER; // ON: czerwony, OFF: białe 5%
        context.drawRoundedRect(this.chipX, this.chipY, this.chipW, CHIP_H, r,
                chipBg.withAlpha(chipBg.getAlpha() * alpha));

        ColorRGBA content = bound ? CsTheme.Color.BLACK_75
                : (this.listening ? CsTheme.Color.accent() : CsTheme.Color.TEXT_25);
        float contentAlpha = bound ? 0.75f : (this.listening ? 1.0f : 0.25f);

        context.drawIcon(Fonts.ICONS, 'i', this.chipX + PAD, centerY - ICON / 2.0f, ICON,
                content.withAlpha(255.0f * contentAlpha * alpha));
        context.drawText(keyFont, label, this.chipX + PAD + ICON + GAP, centerY - keyFont.height() / 2.0f,
                content.withAlpha(255.0f * contentAlpha * alpha));

        if (this.trashShown) {
            context.drawRoundedRect(this.trashX, this.trashY, TRASH_BOX, TRASH_BOX, r,
                    CsTheme.Color.SUB_BORDER.withAlpha(CsTheme.Color.SUB_BORDER.getAlpha() * alpha));
            context.drawIcon(Fonts.ICONS, '(', this.trashX + (TRASH_BOX - TRASH_ICON) / 2.0f,
                    centerY - TRASH_ICON / 2.0f, TRASH_ICON,
                    CsTheme.Color.TEXT_25.withAlpha(255.0f * 0.25f * alpha));
        }
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (this.listening) {
            if (button == MouseButton.LEFT) {
                this.listening = false; // anuluj
            } else {
                this.setting.setKey(button.getButtonIndex());
                this.listening = false;
            }
            return true;
        }
        if (this.trashShown && button == MouseButton.LEFT
                && mouseX >= this.trashX && mouseX <= this.trashX + TRASH_BOX
                && mouseY >= this.trashY && mouseY <= this.trashY + TRASH_BOX) {
            this.setting.setKey(-1);
            return true;
        }
        if (button == MouseButton.LEFT) {
            this.listening = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.listening) {
            return false;
        }
        if (keyCode == 256 || keyCode == 261) {
            this.setting.setKey(-1);
        } else {
            this.setting.setKey(keyCode);
        }
        this.listening = false;
        return true;
    }

    @Override
    public void unfocus() {
        this.listening = false;
    }
}
