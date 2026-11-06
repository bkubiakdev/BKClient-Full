package dev.bkclient.systems.notifications;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.event.impl.window.ChatClickEvent;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;

public class ConfigSavedPopup implements IMinecraft {
    private static ConfigSavedPopup instance;

    public enum Mode {
        SAVE,
        ERROR_OVERWRITE
    }

    private boolean visible;
    private boolean hiding;
    private String configName;
    private Mode mode = Mode.SAVE;
    private Runnable overwriteAction;
    private final Animation anim = new Animation(350L, 0.0F, Easing.BAKEK_SIZE);

    private static final float POPUP_WIDTH = 180.0F;
    private static final float PADDING = 14.0F;
    private static final float TITLE_FONT_SIZE = 8.0F;
    private static final float DESC_FONT_SIZE = 6.0F;
    private static final float BUTTON_HEIGHT = 24.0F;
    private static final float BUTTON_RADIUS = 12.0F;
    private static final float POPUP_RADIUS = 12.0F;
    private static final float MAX_DESC_WIDTH = POPUP_WIDTH - PADDING * 2.0F;
    private static final float BTN_GAP = 8.0F;

    private float popupX, popupY, popupH;

    private final EventListener<HudRenderEvent> onHudRender = event -> {
        if (!visible && !hiding) return;
        draw(event.getContext());
    };

    private final EventListener<ChatClickEvent> onClick = event -> {
        if (!visible) return;
        float mx = event.getX();
        float my = event.getY();

        float btnX = popupX + PADDING;
        float btnW = POPUP_WIDTH - PADDING * 2.0F;

        if (mode == Mode.ERROR_OVERWRITE) {
            float overwriteBtnY = popupY + popupH - PADDING - BUTTON_HEIGHT * 2.0F - BTN_GAP;
            float cancelBtnY = popupY + popupH - PADDING - BUTTON_HEIGHT;

            if (mx >= btnX && mx <= btnX + btnW && my >= cancelBtnY && my <= cancelBtnY + BUTTON_HEIGHT) {
                hide();
            } else if (mx >= btnX && mx <= btnX + btnW && my >= overwriteBtnY && my <= overwriteBtnY + BUTTON_HEIGHT) {
                if (overwriteAction != null) {
                    overwriteAction.run();
                }
            }
        } else {
            float btnY = popupY + popupH - BUTTON_HEIGHT - PADDING;

            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + BUTTON_HEIGHT) {
                hide();
            }
        }
    };

    private ConfigSavedPopup() {
        BKClientInit.getInstance().getEventManager().subscribe(this);
    }

    public static ConfigSavedPopup getInstance() {
        if (instance == null) {
            instance = new ConfigSavedPopup();
        }
        return instance;
    }

    public void show(String configName) {
        this.configName = configName;
        this.mode = Mode.SAVE;
        this.visible = true;
        this.hiding = false;
        this.anim.setValue(0.0F);
        this.anim.setEasing(Easing.BAKEK);
        this.anim.setDuration(300L);
        recalculateLayout();
    }

    public void showOverwrite(String configName, Runnable onOverwrite) {
        this.configName = configName;
        this.mode = Mode.ERROR_OVERWRITE;
        this.overwriteAction = onOverwrite;
        this.visible = true;
        this.hiding = false;
        this.anim.setValue(0.0F);
        this.anim.setEasing(Easing.BAKEK);
        this.anim.setDuration(300L);
        recalculateLayout();
    }

    public void hide() {
        this.hiding = true;
        this.anim.setEasing(Easing.BAKEK_BACK);
        this.anim.setDuration(200L);
        this.anim.update(0.0F);
    }

    public void forceHide() {
        this.visible = false;
        this.configName = null;
        this.overwriteAction = null;
    }

    private void recalculateLayout() {
        Font descFont = Fonts.MEDIUM.getFont(DESC_FONT_SIZE);
        String descKey = mode == Mode.ERROR_OVERWRITE
                ? "commands.config.error.description"
                : "commands.config.saved.description";
        String descRaw = configName != null
                ? Localizator.translate(descKey, configName)
                : "";
        List<String> descLines = wrapText(descRaw, descFont, MAX_DESC_WIDTH);

        Font titleFont = Fonts.BOLD.getFont(TITLE_FONT_SIZE);
        float titleH = titleFont.height() + 4.0F;
        float descH = descLines.size() * (descFont.height() + 3.0F);
        float buttonsH = mode == Mode.ERROR_OVERWRITE
                ? BUTTON_HEIGHT * 2.0F + BTN_GAP
                : BUTTON_HEIGHT;
        popupH = PADDING * 2.0F + titleH + 8.0F + descH + 8.0F + buttonsH;

        float sw = IScaledResolution.sr.getScaledWidth();
        float sh = IScaledResolution.sr.getScaledHeight();
        popupX = (sw - POPUP_WIDTH) / 2.0F;
        popupY = (sh - popupH) / 2.0F;
    }

    private void draw(CustomDrawContext context) {
        anim.update(visible || hiding ? (hiding ? 0.0F : 1.0F) : 0.0F);
        float progress = anim.getValue();
        if (progress <= 0.0F) {
            if (hiding) forceHide();
            return;
        }

        recalculateLayout();

        Font titleFont = Fonts.BOLD.getFont(TITLE_FONT_SIZE);
        Font descFont = Fonts.MEDIUM.getFont(DESC_FONT_SIZE);

        String titleKey = mode == Mode.ERROR_OVERWRITE
                ? "commands.config.error.title"
                : "commands.config.saved.title";
        String descKey = mode == Mode.ERROR_OVERWRITE
                ? "commands.config.error.description"
                : "commands.config.saved.description";
        String title = Localizator.translate(titleKey);
        String descRaw = Localizator.translate(descKey, configName);
        List<String> descLines = wrapText(descRaw, descFont, MAX_DESC_WIDTH);

        float titleH = titleFont.height() + 4.0F;

        context.getMatrices().push();
        float pivotX = popupX + POPUP_WIDTH / 2.0F;
        float pivotY = popupY + popupH / 2.0F;
        context.getMatrices().translate(pivotX, pivotY, 0.0F);
        context.getMatrices().scale(0.85F + progress * 0.15F, 0.85F + progress * 0.15F, 1.0F);
        context.getMatrices().translate(-pivotX, -pivotY, 0.0F);

        int alpha = (int) (255.0F * progress);
        boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
                && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();

        context.drawLiquidGlass(popupX, popupY, POPUP_WIDTH, popupH,
                2.0F, 0.08F, BorderRadius.all(POPUP_RADIUS),
                ColorRGBA.WHITE.withAlpha(255));

        ColorRGBA bg = dark
                ? new ColorRGBA(25.0F, 25.0F, 30.0F, (int) (alpha * 0.35F))
                : new ColorRGBA(240.0F, 240.0F, 245.0F, (int) (alpha * 0.35F));
        context.drawRoundedRect(popupX, popupY, POPUP_WIDTH, popupH, BorderRadius.all(POPUP_RADIUS), bg);

        float titleY = popupY + PADDING;
        float titleX = popupX + (POPUP_WIDTH - titleFont.width(title)) / 2.0F;
        context.drawText(titleFont, title, titleX, titleY, Colors.getTextColor().withAlpha(alpha));

        float descY = titleY + titleH + 6.0F;
        ColorRGBA descColor = Colors.getTextColor().withAlpha((int) (alpha * 0.65F));
        for (String line : descLines) {
            float lineW = descFont.width(line);
            float lineX = popupX + (POPUP_WIDTH - lineW) / 2.0F;
            context.drawText(descFont, line, lineX, descY, descColor);
            descY += descFont.height() + 3.0F;
        }

        Font btnFont = Fonts.MEDIUM.getFont(7.0F);

        if (mode == Mode.ERROR_OVERWRITE) {
            float btnX = popupX + PADDING;
            float btnW = POPUP_WIDTH - PADDING * 2.0F;
            float textYBase = popupY + popupH - PADDING - BUTTON_HEIGHT;

            float overwriteY = textYBase - BUTTON_HEIGHT - BTN_GAP;
            ColorRGBA overwriteBg = new ColorRGBA(255.0F, 59.0F, 48.0F, (int) (alpha * 0.5F));
            context.drawRoundedRect(btnX, overwriteY, btnW, BUTTON_HEIGHT, BorderRadius.all(BUTTON_RADIUS), overwriteBg);
            String overwriteText = Localizator.translate("popup.overwrite");
            float overwriteTextX = btnX + (btnW - btnFont.width(overwriteText)) / 2.0F;
            float overwriteTextY = overwriteY + (BUTTON_HEIGHT - btnFont.height()) / 2.0F;
            context.drawText(btnFont, overwriteText, overwriteTextX, overwriteTextY, ColorRGBA.WHITE.withAlpha(alpha));

            float cancelY = textYBase;
            ColorRGBA cancelBg = new ColorRGBA(0.0F, 136.0F, 255.0F, (int) (alpha * 0.5F));
            context.drawRoundedRect(btnX, cancelY, btnW, BUTTON_HEIGHT, BorderRadius.all(BUTTON_RADIUS), cancelBg);
            String cancelText = Localizator.translate("popup.cancel");
            float cancelTextX = btnX + (btnW - btnFont.width(cancelText)) / 2.0F;
            float cancelTextY = cancelY + (BUTTON_HEIGHT - btnFont.height()) / 2.0F;
            context.drawText(btnFont, cancelText, cancelTextX, cancelTextY, ColorRGBA.WHITE.withAlpha(alpha));
        } else {
            float btnX = popupX + PADDING;
            float btnW = POPUP_WIDTH - PADDING * 2.0F;
            float btnY = popupY + popupH - BUTTON_HEIGHT - PADDING;
            ColorRGBA btnBg = dark
                    ? new ColorRGBA(140.0F, 140.0F, 150.0F, (int) (alpha * 0.5F))
                    : new ColorRGBA(220.0F, 220.0F, 225.0F, (int) (alpha * 0.5F));
            context.drawRoundedRect(btnX, btnY, btnW, BUTTON_HEIGHT, BorderRadius.all(BUTTON_RADIUS), btnBg);

            String okText = Localizator.translate("popup.ok");
            float okX = btnX + (btnW - btnFont.width(okText)) / 2.0F;
            float okY = btnY + (BUTTON_HEIGHT - btnFont.height()) / 2.0F;
            context.drawText(btnFont, okText, okX, okY, Colors.getTextColor().withAlpha(alpha));
        }

        context.getMatrices().pop();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public boolean isVisible() {
        return visible;
    }

    private static List<String> wrapText(String text, Font font, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String test = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.width(test) > maxWidth && !currentLine.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(test);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        if (lines.isEmpty()) {
            lines.add(text);
        }
        return lines;
    }
}
