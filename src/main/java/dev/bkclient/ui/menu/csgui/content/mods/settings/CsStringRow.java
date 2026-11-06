package dev.bkclient.ui.menu.csgui.content.mods.settings;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.colors.ColorRGBA;

public class CsStringRow implements CsSetting {

    private static final float HEIGHT = 14.0f;
    private static final float FIELD_W = 56.0f;
    private static final float FIELD_H = 8.0f;

    private final StringSetting setting;
    private final TextField textField;
    private final CsNameLabel nameLabel = new CsNameLabel();
    private String syncedText;

    private float fx, fy, fw;

    public CsStringRow(StringSetting setting) {
        this.setting = setting;
        this.syncedText = setting.getText() == null ? "" : setting.getText();
        this.textField = new TextField(Fonts.SEMIBOLD.getFont(6.0f));
        this.textField.pasteInitial(this.syncedText);
        this.textField.setPreview("Text..");
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
        return this.textField.isFocused();
    }

    @Override
    public void render(UIContext context, float x, float y, float w, float alpha) {
        float centerY = y + HEIGHT / 2.0f;
        Font nameFont = Fonts.SEMIBOLD.getFont(CsTheme.Slider.NAME_FONT + 1.0f);

        this.fw = Math.min(FIELD_W, Math.max(34.0f, w * 0.48f));
        this.fx = x + w - this.fw;
        this.fy = centerY - FIELD_H / 2.0f;

        boolean hovered = context.getMouseX() >= x && context.getMouseX() <= x + w
                && context.getMouseY() >= y && context.getMouseY() <= y + HEIGHT;
        float nameMaxW = this.fx - x - 5.0f;
        this.nameLabel.render(context, nameFont, Localizator.translate(this.setting.getName()), x, centerY,
                nameMaxW, hovered, CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));

        context.drawRoundedRect(this.fx, this.fy, this.fw, FIELD_H, BorderRadius.all(2.0f),
                new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.05f).withAlpha(255.0f * 0.05f * alpha));

        float iconSize = 7.0f;
        context.drawIcon(Fonts.ICONS, 'n', this.fx + 2.5f, centerY - iconSize / 2.0f, iconSize,
                CsTheme.Color.TEXT_25.withAlpha(255.0f * 0.25f * alpha));

        float textLeft = this.fx + iconSize + 3.0f;
        float textRight = this.fx + this.fw - 3.5f;
        this.textField.set(textLeft - 1.0f, this.fy, textRight - textLeft + 1.0f, FIELD_H);
        this.textField.setAlpha(alpha);
        this.textField.setTextColor(CsTheme.Color.TEXT);
        if (!this.textField.isFocused()) {
            String current = this.setting.getText() == null ? "" : this.setting.getText();
            if (!current.equals(this.syncedText) || !current.equals(this.textField.getText())) {
                this.syncedText = current;
                this.textField.setText(current);
            }
        }
        this.textField.render(context);
        if (this.textField.isFocused()) {
            this.syncedText = this.textField.getText();
            this.setting.text(this.syncedText);
        }
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return false;
        }
        boolean inside = mouseX >= this.fx && mouseX <= this.fx + this.fw
                && mouseY >= this.fy - 2.0f && mouseY <= this.fy + FIELD_H + 2.0f;
        if (inside) {
            this.textField.onMouseClicked(mouseX, mouseY, button);
            this.textField.setFocused(true);
            return true;
        }
        if (this.textField.isFocused()) {
            this.commit();
        }
        return false;
    }

    @Override
    public void handleRelease() {
        this.textField.onMouseReleased(0.0, 0.0, MouseButton.LEFT);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.textField.isFocused()) {
            return false;
        }
        this.textField.onKeyPressed(keyCode, scanCode, modifiers);
        if (keyCode == 257 || keyCode == 256 || !this.textField.isFocused()) {
            this.commit();
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr) {
        if (!this.textField.isFocused()) {
            return false;
        }
        return this.textField.charTyped(chr, 0);
    }

    @Override
    public void unfocus() {
        if (this.textField.isFocused()) {
            this.commit();
        }
    }

    private void commit() {
        this.syncedText = this.textField.getText();
        this.setting.setText(this.syncedText);
        AbstractSetting.saveConfig();
        this.textField.setFocused(false);
    }
}
