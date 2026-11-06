package dev.bkclient.ui.menu.csgui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.framework.base.CustomScreen;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.player.MineHelper;
import dev.bkclient.ui.components.popup.list.SlotSelectorComponent;
import dev.bkclient.ui.menu.csgui.content.mods.settings.CsSetting;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;

import java.util.ArrayList;
import java.util.List;

public class CsMineAfkScreen extends CustomScreen implements IScaledResolution {

    private static final float WINDOW_W = 540.0f;
    private static final float HEADER_H = 27.0f;
    private static final float PAD = 10.0f;
    private static final float GAP = 8.0f;
    private static final float CARD_PAD = 8.0f;
    private static final float TITLE_H = 11.0f;
    private static final float ROW_GAP = 3.0f;
    private static final float LABEL_GAP = 5.0f;

    private final net.minecraft.client.gui.screen.Screen parent = IMinecraft.mc.currentScreen;
    private final MineHelper module;
    private final List<CsSetting> generalRows = new ArrayList<>();
    private final List<CsSetting> droppingRows = new ArrayList<>();
    private final List<CsSetting> commandRows = new ArrayList<>();
    private final List<RowHit> rowHits = new ArrayList<>();
    private final List<dev.bkclient.ui.menu.dropdown.components.settings.impl.ModeSettingComponent> modeOverlays = new ArrayList<>();
    private final SlotSelectorComponent slotSelector;

    private float windowX;
    private float windowY;
    private float windowH;

    public CsMineAfkScreen(MineHelper module) {
        this.module = module;
        this.slotSelector = new SlotSelectorComponent(module);

        this.generalRows.add(nativeSetting(module.blockLimit));
        this.generalRows.add(nativeSetting(module.mineMode));

        this.droppingRows.add(nativeSetting(module.dropInterval));

        this.commandRows.add(nativeSetting(module.command1));
        this.commandRows.add(nativeSetting(module.interval1));
        this.commandRows.add(nativeSetting(module.command2));
        this.commandRows.add(nativeSetting(module.interval2));
        this.commandRows.add(nativeSetting(module.command3));
        this.commandRows.add(nativeSetting(module.interval3));
    }

    private float uiScale = 1;
    private double localX(double x) { double cx = width / 2.0; return cx + (x - cx) / uiScale; }
    private double localY(double y) { double cy = height / 2.0; return cy + (y - cy) / uiScale; }

    @Override
    public void render(UIContext context) {
        uiScale = Math.max(.1F, Math.min(dev.bkclient.systems.modules.modules.visuals.Interface.menuScale(),
                Math.min((width - 16F) / WINDOW_W, (height - 16F) / Math.max(280F, windowH))));
        context.getMatrices().push();
        try (var theme = dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.openClickSys()) {
            context.getMatrices().translate(width / 2F, height / 2F, 0);
            context.getMatrices().scale(uiScale, uiScale, 1);
            context.getMatrices().translate(-width / 2F, -height / 2F, 0);
            renderContent(context.withMouse((int)localX(context.getMouseX()), (int)localY(context.getMouseY())));
        } finally { context.getMatrices().pop(); }
    }

    private void renderContent(UIContext context) {
        if (IMinecraft.mc.player == null) {
            return;
        }

        this.rowHits.clear();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        float leftW = 150.0f;
        float midW = 178.0f;
        float rightW = WINDOW_W - PAD * 2.0f - GAP * 2.0f - leftW - midW;

        float generalH = sectionHeight(this.generalRows, 0.0f);
        float droppingH = sectionHeight(this.droppingRows, LABEL_GAP + 8.0f + this.slotSelector.getHeight());
        float commandsH = sectionHeight(this.commandRows, 0.0f);
        float contentH = Math.max(generalH, Math.max(droppingH, commandsH));
        this.windowH = HEADER_H + PAD + contentH + PAD;

        this.windowX = IScaledResolution.sr.getScaledWidth() / 2.0f - WINDOW_W / 2.0f;
        this.windowY = IScaledResolution.sr.getScaledHeight() / 2.0f - this.windowH / 2.0f;

        context.drawRoundedRect(this.windowX,this.windowY,WINDOW_W,this.windowH,BorderRadius.all(10),dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.current().window);

        renderHeader(context);

        float cardY = this.windowY + HEADER_H + PAD;
        float x = this.windowX + PAD;
        renderSection(context, "modules.minehelper.tab.general", x, cardY, leftW, contentH, this.generalRows, 1.0f);

        x += leftW + GAP;
        renderDroppingSection(context, x, cardY, midW, contentH);

        x += midW + GAP;
        renderSection(context, "modules.minehelper.tab.commands", x, cardY, rightW, contentH, this.commandRows, 1.0f);
        // Native mode controls draw their floating list in a separate pass.
        for (var mode : modeOverlays) {
            if (mode.getSetting().isVisible()) mode.renderOverlay(context);
        }
    }

    private void renderHeader(UIContext context) {
        Font title = Fonts.SEMIBOLD.getFont(9.0f);
        Font sub = Fonts.REGULAR.getFont(6.0f);
        float tx = this.windowX + PAD;
        float ty = this.windowY + 6.0f;

        context.drawText(title, "MineHelper", tx, ty, dev.bkclient.utility.colors.Colors.getTextColor().withAlpha(230.0f));
        context.drawText(sub, Localizator.translate(this.module.getName()), tx + title.width("MineHelper") + 5.0f,
                ty + 2.0f, dev.bkclient.utility.colors.Colors.getTextColor().withAlpha(150.0f));
        context.drawRoundedRect(this.windowX, this.windowY + HEADER_H - 0.5f, WINDOW_W, 0.5f, BorderRadius.ZERO,
                dev.bkclient.utility.colors.Colors.getOutlineColor().mulAlpha(.3F));
    }

    private void renderDroppingSection(UIContext context, float x, float y, float w, float h) {
        renderCard(context, x, y, w, h);
        float cy = y + CARD_PAD;
        renderSectionTitle(context, "modules.minehelper.tab.dropping", x + CARD_PAD, cy, w - CARD_PAD * 2.0f);
        cy += TITLE_H + LABEL_GAP;
        cy = renderRows(context, this.droppingRows, x + CARD_PAD, cy, w - CARD_PAD * 2.0f, 1.0f);
        cy += LABEL_GAP;

        Font label = Fonts.SEMIBOLD.getFont(6.5f);
        context.drawText(label, Localizator.translate("modules.minehelper.label.slot_selection"), x + CARD_PAD, cy,
                dev.bkclient.utility.colors.Colors.getTextColor().withAlpha(180.0f));
        cy += 8.0f;

        this.slotSelector.set(x + CARD_PAD, cy, w - CARD_PAD * 2.0f, this.slotSelector.getHeight());
        this.slotSelector.render(context);
    }

    private void renderSection(UIContext context, String title, float x, float y, float w, float h,
            List<CsSetting> rows, float alpha) {
        renderCard(context, x, y, w, h);
        float cy = y + CARD_PAD;
        renderSectionTitle(context, title, x + CARD_PAD, cy, w - CARD_PAD * 2.0f);
        cy += TITLE_H + LABEL_GAP;
        renderRows(context, rows, x + CARD_PAD, cy, w - CARD_PAD * 2.0f, alpha);
    }

    private float renderRows(UIContext context, List<CsSetting> rows, float x, float y, float w, float alpha) {
        float cy = y;
        for (CsSetting row : rows) {
            if (!row.isVisible()) {
                continue;
            }
            float rh = row.getHeight();
            row.render(context, x, cy, w, alpha);
            this.rowHits.add(new RowHit(row, x, cy, w, rh));
            cy += rh + ROW_GAP;
        }
        return cy;
    }

    private void renderCard(UIContext context, float x, float y, float w, float h) {
        var theme=dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.current();
        context.drawRoundedRect(x,y,w,h,BorderRadius.all(6),theme.surface);
        context.drawRoundedBorder(x,y,w,h,.5f,BorderRadius.all(6),theme.border);
    }

    private void renderSectionTitle(UIContext context, String key, float x, float y, float w) {
        Font font = Fonts.SEMIBOLD.getFont(7.0f);
        context.drawText(font, Localizator.translate(key), x, y, dev.bkclient.utility.colors.Colors.getTextColor().withAlpha(210.0f));
        context.drawRoundedRect(x, y + TITLE_H - 1.0f, w, 0.5f, BorderRadius.ZERO,
                dev.bkclient.utility.colors.Colors.getOutlineColor().mulAlpha(.3F).withAlpha(dev.bkclient.utility.colors.Colors.getOutlineColor().mulAlpha(.3F).getAlpha() * 0.8f));
    }

    private float sectionHeight(List<CsSetting> rows, float extra) {
        float h = CARD_PAD + TITLE_H + LABEL_GAP + extra + CARD_PAD;
        boolean first = true;
        for (CsSetting row : rows) {
            if (!row.isVisible()) {
                continue;
            }
            if (!first) {
                h += ROW_GAP;
            }
            h += row.getHeight();
            first = false;
        }
        return h;
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        mouseX = localX(mouseX); mouseY = localY(mouseY);
        if (button != MouseButton.LEFT && button != MouseButton.RIGHT) {
            return;
        }

        // The floating list owns clicks before the cards (including outside-click dismissal).
        for (var mode : modeOverlays) {
            if (mode.isDropdownVisible()) {
                mode.onMouseClicked(mouseX, mouseY, button);
                return;
            }
        }
        boolean inside = mouseX >= this.windowX && mouseX <= this.windowX + WINDOW_W
                && mouseY >= this.windowY && mouseY <= this.windowY + this.windowH;
        if (!inside) {
            close();
            return;
        }

        this.slotSelector.onMouseClicked(mouseX, mouseY, button);
        for (int i = this.rowHits.size() - 1; i >= 0; i--) {
            RowHit hit = this.rowHits.get(i);
            // Every native field sees outside clicks so focus cannot remain on two commands.
            hit.row.handleClick(mouseX, mouseY, button);
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        mouseX = localX(mouseX); mouseY = localY(mouseY);
        for (RowHit hit : this.rowHits) {
            hit.row.handleRelease();
        }
        this.slotSelector.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (RowHit hit : this.rowHits) {
            if (hit.row.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (keyCode == 256) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (RowHit hit : this.rowHits) {
            if (hit.row.charTyped(chr)) {
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    public void close() {
        for (RowHit hit : rowHits) { hit.row.handleRelease(); hit.row.unfocus(); }
        dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();
        IMinecraft.mc.setScreen(parent);
    }

    // Adapts existing ClickGUI controls; no separate slider, input or mode renderer.
    private CsSetting nativeSetting(dev.bkclient.systems.setting.Setting setting) {
        // Native settings read their containing row's bounds during rendering.
        var rowContainer = new dev.bkclient.framework.base.CustomComponent() {
            @Override protected void renderComponent(UIContext context) { }
        };
        dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent<?> component;
        if (setting instanceof dev.bkclient.systems.setting.settings.SliderSetting slider)
            component = new dev.bkclient.ui.menu.dropdown.components.settings.impl.SliderSettingComponent(slider, rowContainer);
        else if (setting instanceof dev.bkclient.systems.setting.settings.ModeSetting mode)
            component = new dev.bkclient.ui.menu.dropdown.components.settings.impl.ModeSettingComponent(mode, rowContainer);
        else if (setting instanceof dev.bkclient.systems.setting.settings.StringSetting text)
            component = new dev.bkclient.ui.menu.dropdown.components.settings.impl.StringSettingComponent(text, rowContainer);
        else throw new IllegalArgumentException("Unsupported MineHelper setting");
        component.onInit();
        if (component instanceof dev.bkclient.ui.menu.dropdown.components.settings.impl.ModeSettingComponent mode) {
            modeOverlays.add(mode);
        }
        return new CsSetting() {
            public dev.bkclient.systems.setting.Setting getSetting() { return setting; }
            public float getHeight() { return component.getHeight(); }
            public void render(UIContext c, float x, float y, float w, float alpha) {
                rowContainer.set(x - 8, y, w + 16, getHeight());
                component.set(x - 8, y, w + 16, getHeight());
                component.render(c);
            }
            public boolean handleClick(double x, double y, MouseButton button) {
                component.onMouseClicked(x, y, button); return component.isHovered(x, y);
            }
            public void handleRelease() { component.onMouseReleased(-1, -1, MouseButton.LEFT); }
            public boolean keyPressed(int key, int scan, int mods) { component.onKeyPressed(key, scan, mods); return false; }
            public boolean charTyped(char chr) { return component.charTyped(chr, 0); }
        };
    }

    private static final class RowHit {
        private final CsSetting row;
        private final float x;
        private final float y;
        private final float w;
        private final float h;

        private RowHit(CsSetting row, float x, float y, float w, float h) {
            this.row = row;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        private boolean contains(double mx, double my) {
            return mx >= this.x && mx <= this.x + this.w && my >= this.y && my <= this.y + this.h;
        }
    }

}
