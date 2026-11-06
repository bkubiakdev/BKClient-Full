package dev.bkclient.ui.menu.csgui.panel;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.ScissorUtility;

/**
 * Responsive, wheel-driven selector for the CS GUI scale.
 */
public final class CsScalePicker implements IMinecraft {
    private static final float[] VALUES = {
            0.0f, 60.0f, 70.0f, 80.0f, 90.0f, 100.0f,
            110.0f, 120.0f, 130.0f
    };

    private static final ColorRGBA BACKDROP = new ColorRGBA(0.0f, 0.0f, 0.0f, 105.0f);
    private static final ColorRGBA FILL = new ColorRGBA(17.0f, 18.0f, 22.0f, 247.0f);
    private static final ColorRGBA GLASS = new ColorRGBA(17.0f, 18.0f, 22.0f, 180.0f);
    private static final ColorRGBA BORDER = new ColorRGBA(255.0f, 255.0f, 255.0f, 31.0f);
    private static final ColorRGBA MUTED = new ColorRGBA(255.0f, 255.0f, 255.0f, 82.0f);

    private final SliderSetting setting;
    private final Animation showAnim = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
    private final Animation indexAnim = new Animation(170L, Easing.FIGMA_EASE_IN_OUT);

    private int selectedIndex;
    private double wheelRemainder;
    private boolean closing;

    public CsScalePicker(SliderSetting setting) {
        this.setting = setting;
        this.selectedIndex = nearestIndex(setting.getCurrentValue());
        this.indexAnim.setValue(this.selectedIndex);
    }

    public boolean isDead() {
        return this.closing && this.showAnim.getValue() <= 0.01f;
    }

    public void close() {
        this.closing = true;
    }

    public void render(UIContext context, float screenAlpha) {
        this.showAnim.setDuration(Interface.animDuration(220L));
        float open = this.showAnim.update(this.closing ? 0.0f : 1.0f);
        float alpha = Math.clamp(open * screenAlpha, 0.0f, 1.0f);
        if (alpha <= 0.01f) {
            return;
        }

        Layout layout = layout();
        float visualIndex = this.indexAnim.update(this.selectedIndex);
        float scale = 0.90f + 0.10f * open;
        float centerX = layout.x + layout.width / 2.0f;
        float centerY = layout.y + layout.height / 2.0f;

        context.drawRect(0.0f, 0.0f, layout.screenWidth, layout.screenHeight,
                BACKDROP.withAlpha(BACKDROP.getAlpha() * alpha));

        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0.0f);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-centerX, -centerY + (1.0f - open) * 5.0f, 0.0f);

        BorderRadius radius = BorderRadius.all(layout.radius);
        if (Interface.shadow()) {
            context.drawShadow(layout.x - 4.0f, layout.y - 4.0f,
                    layout.width + 8.0f, layout.height + 8.0f,
                    13.0f, radius, ColorRGBA.BLACK.withAlpha(145.0f * alpha));
        }
        if (Interface.blurGui()) {
            context.drawBlurredRect(layout.x, layout.y, layout.width, layout.height,
                    18.0f, 2.0f, radius, GLASS.withAlpha(GLASS.getAlpha() * alpha));
        }
        context.drawRoundedRect(layout.x, layout.y, layout.width, layout.height,
                radius, FILL.withAlpha(FILL.getAlpha() * alpha));
        context.drawRoundedBorder(layout.x, layout.y, layout.width, layout.height,
                0.55f, radius, BORDER.withAlpha(BORDER.getAlpha() * alpha));

        Font titleFont = Fonts.SEMIBOLD.getFont(layout.titleFont);
        context.drawCenteredText(titleFont, Localizator.translate("gui.scale_picker.title"),
                centerX, layout.y + (layout.headerHeight - titleFont.height()) / 2.0f,
                ColorRGBA.WHITE.withAlpha(205.0f * alpha));

        float selectedY = layout.listY + layout.listHeight / 2.0f - layout.rowHeight / 2.0f;
        ColorRGBA accent = CsTheme.Color.accent();
        context.drawRoundedRect(layout.x + 5.0f, selectedY, layout.width - 10.0f,
                layout.rowHeight, BorderRadius.all(5.0f),
                accent.withAlpha(42.0f * alpha));
        context.drawRoundedBorder(layout.x + 5.0f, selectedY, layout.width - 10.0f,
                layout.rowHeight, 0.45f, BorderRadius.all(5.0f),
                accent.withAlpha(105.0f * alpha));

        ScissorUtility.push(context.getMatrices(), layout.x + 3.0f, layout.listY,
                layout.width - 6.0f, layout.listHeight);
        for (int i = 0; i < VALUES.length; i++) {
            float offset = i - visualIndex;
            if (Math.abs(offset) > layout.visibleRows / 2.0f + 1.2f) {
                continue;
            }

            float rowCenter = layout.listY + layout.listHeight / 2.0f + offset * layout.rowHeight;
            float distance = Math.abs(offset);
            float focus = 1.0f - Math.clamp(distance, 0.0f, 1.0f);
            float edgeFade = 1.0f - Math.clamp(
                    (distance - 0.75f) / Math.max(1.0f, layout.visibleRows / 2.0f),
                    0.0f, 0.82f);

            Font valueFont = Fonts.SEMIBOLD.getFont(layout.valueFont + focus * 1.5f);
            String label = label(i);
            ColorRGBA textColor = focus > 0.45f ? ColorRGBA.WHITE : MUTED;
            float textAlpha = (focus > 0.45f ? 255.0f : MUTED.getAlpha()) * edgeFade * alpha;
            context.drawCenteredText(valueFont, label, centerX,
                    rowCenter - valueFont.height() / 2.0f,
                    textColor.withAlpha(textAlpha));
        }
        ScissorUtility.pop();

        Font hintFont = Fonts.MEDIUM.getFont(layout.hintFont);
        context.drawCenteredText(hintFont, Localizator.translate("gui.scale_picker.hint"),
                centerX, layout.y + layout.height - layout.footerHeight
                        + (layout.footerHeight - hintFont.height()) / 2.0f,
                MUTED.withAlpha(MUTED.getAlpha() * alpha));

        context.getMatrices().pop();
    }

    public boolean onClick(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.RIGHT) {
            this.close();
            return true;
        }
        if (button != MouseButton.LEFT) {
            return true;
        }

        Layout layout = layout();
        if (!layout.contains(mouseX, mouseY)) {
            this.close();
            return true;
        }
        if (mouseY >= layout.listY && mouseY <= layout.listY + layout.listHeight) {
            float centerY = layout.listY + layout.listHeight / 2.0f;
            int clicked = Math.round(this.indexAnim.getValue()
                    + ((float) mouseY - centerY) / layout.rowHeight);
            this.select(Math.clamp(clicked, 0, VALUES.length - 1));
        }
        return true;
    }

    public boolean onScroll(double verticalAmount) {
        if (verticalAmount == 0.0) {
            return true;
        }
        this.wheelRemainder -= verticalAmount;
        int steps = (int) this.wheelRemainder;
        if (steps != 0) {
            this.wheelRemainder -= steps;
            this.select(Math.clamp(this.selectedIndex + steps, 0, VALUES.length - 1));
        }
        return true;
    }

    public boolean onKeyPressed(int keyCode) {
        if (keyCode == 256) {
            this.close();
            return true;
        }
        if (keyCode == 264 || keyCode == 267) {
            this.select(Math.min(VALUES.length - 1, this.selectedIndex + 1));
            return true;
        }
        if (keyCode == 265 || keyCode == 266) {
            this.select(Math.max(0, this.selectedIndex - 1));
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            this.close();
            return true;
        }
        return false;
    }

    private void select(int index) {
        if (index == this.selectedIndex) {
            return;
        }
        this.selectedIndex = index;
        this.setting.setCurrentValueAndSave(VALUES[index]);
    }

    private static String label(int index) {
        return VALUES[index] <= 0.0f
                ? Localizator.translate("gui.scale_picker.auto")
                : Math.round(VALUES[index]) + "%";
    }

    private static int nearestIndex(float value) {
        int best = 0;
        float distance = Float.MAX_VALUE;
        for (int i = 0; i < VALUES.length; i++) {
            float current = Math.abs(VALUES[i] - value);
            if (current < distance) {
                distance = current;
                best = i;
            }
        }
        return best;
    }

    private static Layout layout() {
        float sw = Math.max(1.0f, mc.getWindow().getScaledWidth());
        float sh = Math.max(1.0f, mc.getWindow().getScaledHeight());
        int visibleRows = sh < 260.0f ? 3 : 5;
        float rowHeight = Math.clamp(sh * 0.038f, 14.0f, 18.0f);
        float width = Math.clamp(sw * 0.145f, 86.0f, 116.0f);
        width = Math.min(width, Math.max(64.0f, sw - 20.0f));
        float headerHeight = Math.clamp(rowHeight * 1.05f, 15.0f, 19.0f);
        float footerHeight = Math.clamp(rowHeight * 0.88f, 13.0f, 16.0f);
        float listHeight = visibleRows * rowHeight;
        float height = Math.min(headerHeight + listHeight + footerHeight,
                Math.max(70.0f, sh - 20.0f));
        float x = (sw - width) / 2.0f;
        float y = (sh - height) / 2.0f;
        return new Layout(sw, sh, x, y, width, height, headerHeight,
                footerHeight, y + headerHeight, listHeight, rowHeight,
                visibleRows, Math.clamp(width * 0.075f, 6.0f, 8.5f),
                Math.clamp(width * 0.061f, 5.5f, 7.0f),
                Math.clamp(width * 0.049f, 4.5f, 5.5f),
                Math.clamp(width * 0.085f, 6.0f, 9.0f));
    }

    private record Layout(
            float screenWidth,
            float screenHeight,
            float x,
            float y,
            float width,
            float height,
            float headerHeight,
            float footerHeight,
            float listY,
            float listHeight,
            float rowHeight,
            int visibleRows,
            float radius,
            float titleFont,
            float hintFont,
            float valueFont) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX <= this.x + this.width
                    && mouseY >= this.y && mouseY <= this.y + this.height;
        }
    }
}
