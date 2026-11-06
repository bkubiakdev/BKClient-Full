/*
 * CsGuiScreen — alternatywny styl ClickGUI (CS-like).
 * Przeciągalne panele (Informacja, ThemeClient) rysowane ZA głównym oknem.
 */
package dev.bkclient.ui.menu.csgui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.ui.components.textfield.FieldAction;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.MenuScreen;
import dev.bkclient.ui.menu.csgui.panel.CsPanel;
import dev.bkclient.ui.menu.csgui.panel.InfoPanel;
import dev.bkclient.ui.menu.csgui.panel.ThemePanel;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.ScissorUtility;

public class CsGuiScreen
        extends MenuScreen
        implements IMinecraft {

    private static float WIDTH()  { return CsTheme.Window.WIDTH(); }
    private static float HEIGHT() { return CsTheme.Window.HEIGHT(); }
    private static final float CORNER_RADIUS = CsTheme.Window.RADIUS;
    private static final float BORDER_THICKNESS = CsTheme.Window.BORDER;

    private static final float TOP_BAR_H = CsTheme.Window.TOP_BAR_H;
    private static final float BOTTOM_BAR_H = CsTheme.Window.BOTTOM_BAR_H;
    private static final float PANEL_MARGIN = 10.0f;

    private static final ColorRGBA BACKGROUND = CsTheme.Color.WINDOW_FILL;
    private static final ColorRGBA BACKGROUND_GLASS = CsTheme.Color.WINDOW_GLASS;
    private static final ColorRGBA BORDER = CsTheme.Color.WINDOW_BORDER;
    private static final ColorRGBA BAR_FILL = CsTheme.Color.BAR_FILL;
    private static final ColorRGBA BAR_BORDER = CsTheme.Color.BAR_BORDER;
    private static final String MODS_URL = "https://changeme.net/mods";

    private final List<CsPanel> panels = new ArrayList<>();
    private InfoPanel infoPanel;
    private ThemePanel themePanel;
    private boolean infoPanelOpen;
    private final Animation infoPanelAnimation = new Animation(360L, Easing.BAKEK_SMALLER);
    private final dev.bkclient.ui.menu.csgui.content.AutoMarketContent autoMarketContent =
            new dev.bkclient.ui.menu.csgui.content.AutoMarketContent();
    private final dev.bkclient.ui.menu.csgui.content.ConfigContent configContent =
            new dev.bkclient.ui.menu.csgui.content.ConfigContent();
    private final dev.bkclient.ui.menu.csgui.content.ModsContent modsContent =
            new dev.bkclient.ui.menu.csgui.content.ModsContent();
    private boolean panelsInit = false;

    // Mapowanie tab → kategoria modułów (null = tab nie pokazuje listy modułów)
    private static final dev.bkclient.systems.modules.api.ModuleCategory[] TAB_CATEGORY = {
            dev.bkclient.systems.modules.api.ModuleCategory.COMBAT,   // Mods (Combat + Movement)
            dev.bkclient.systems.modules.api.ModuleCategory.VISUALS,  // Visuals
            dev.bkclient.systems.modules.api.ModuleCategory.PLAYER,   // Player
            dev.bkclient.systems.modules.api.ModuleCategory.OTHER,    // Misc
            null,                                                     // AutoMarket
            null,                                                     // Configs
    };

    private int selectedCategory = 0;

    /** Otwiera GUI od razu na danej zakładce (4 = AutoMarket, 5 = Configs). */
    public void setSelectedCategory(int category) {
        this.selectedCategory = category;
    }
    // animacja przejścia zawartości przy zmianie kategorii (0 = świeża zmiana → 1 = ustabilizowana)
    private final dev.bkclient.utility.animation.base.Animation catAnim =
            new dev.bkclient.utility.animation.base.Animation(240L,
                    dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT);
    private static final String[] CATEGORY_NAMES = { "Mods", "Visuals", "Player", "Misc", "AutoMarket", "Configs" };
    private static final char[] CATEGORY_ICONS = { 'u', 'r', 's', 't', 'A', 'U' }; // Glify ikon kategorii.
    private final float[] tabX = new float[16];
    private final float[] tabW = new float[16];
    private int tabCount = 0;
    private float tabsY, tabsH;

    private TextField searchField;
    private final dev.bkclient.utility.animation.base.Animation searchAnim =
            new dev.bkclient.utility.animation.base.Animation(300L,
                    dev.bkclient.utility.animation.base.Easing.BAKEK);
    private final dev.bkclient.utility.animation.base.Animation appendingAnim =
            new dev.bkclient.utility.animation.base.Animation(300L,
                    dev.bkclient.utility.animation.base.Easing.BAKEK);
    private float searchFieldX, searchFieldY, searchFieldW, searchFieldH;
    private float footerDotsX, footerDotsY, footerDotsW, footerDotsH;

    private float tabSlideX, tabSlideW;
    private boolean tabSlideInit;

    public CsGuiScreen() {
        this.closing = false;
    }

    private float guiScale() {
        return Interface.menuScaleMultiplier();
    }

    private double logicalMouseX(double mouseX) {
        float scale = this.guiScale();
        return this.width / 2.0 + (mouseX - this.width / 2.0) / scale;
    }

    private double logicalMouseY(double mouseY) {
        float scale = this.guiScale();
        return this.height / 2.0 + (mouseY - this.height / 2.0) / scale;
    }

    @Override
    protected void init() {
        super.init();
        this.closing = false;
        this.menuAnimation.setValue(0.0f);
        this.infoPanelAnimation.setValue(0.0f);
        this.infoPanelOpen = false;
        this.panelsInit = false;
    }

    private void initPanels() {
        float mainX = (this.width - WIDTH()) / 2.0f;
        float mainY = (this.height - HEIGHT()) / 2.0f;
        this.infoPanel = new InfoPanel(mainX - 130.0f, mainY + TOP_BAR_H);
        this.themePanel = new ThemePanel(mainX + WIDTH() - 5.0f, mainY + (HEIGHT() - 265.0f) / 2.0f);
        this.panels.clear();
        this.panels.add(this.themePanel);

        this.searchField = new TextField(Fonts.SEMIBOLD.getFont(7.0f));
        HashMap<String, FieldAction> append = new HashMap<>();
        for (Module module : BKClientInit.getInstance().getModuleManager().getModules()) {
            FieldAction action = new FieldAction(
                    module::toggle,
                    () -> { /* Tab — wypełnij nazwę, nie otwieraj panelu */ });
            append.put(module.getName(), action);
            append.put(module.getName().replace(" ", ""), action);
        }
        this.searchField.setAppend(append);

        this.panelsInit = true;
    }

    @Override
    public void render(UIContext context) {
        if (!this.panelsInit) {
            this.initPanels();
        }

        this.menuAnimation.setEasing(this.isClosing() ? Easing.FIGMA_EASE_IN_OUT : Easing.BAKEK_SMALLER);
        this.menuAnimation.setDuration(this.isClosing() ? 220L : 360L);
        float intro = this.menuAnimation.update(this.isClosing() ? 0.0f : 1.0f);

        float alpha = Math.clamp(intro, 0.0f, 1.0f);
        if (alpha <= 0.0f) {
            return;
        }

        UIContext screenContext = context;
        float guiScale = this.guiScale();
        int logicalMouseX = (int) Math.round(this.logicalMouseX(context.getMouseX()));
        int logicalMouseY = (int) Math.round(this.logicalMouseY(context.getMouseY()));
        context = context.withMouse(logicalMouseX, logicalMouseY);
        float guiPivotX = this.width / 2.0f;
        float guiPivotY = this.height / 2.0f;
        context.getMatrices().push();
        context.getMatrices().translate(guiPivotX, guiPivotY, 0.0f);
        context.getMatrices().scale(guiScale, guiScale, 1.0f);
        context.getMatrices().translate(-guiPivotX, -guiPivotY, 0.0f);

        float infoT = this.infoPanelAnimation.update(this.infoPanelOpen ? 1.0f : 0.0f);
        float infoVisible = Math.clamp(infoT, 0.0f, 1.0f);
        float slideOffset = 20.0f * (1.0f - alpha);
        if ((this.infoPanelOpen || infoVisible > 0.01f) && this.infoPanel != null) {
            float infoAlpha = alpha * infoVisible;
            float infoScale = 0.93f + 0.07f * Math.max(0.0f, infoT);
            float infoPivotX = this.infoPanel.getX() + this.infoPanel.getWidth() / 2.0f;
            float infoPivotY = this.infoPanel.getY() + this.infoPanel.getHeight() / 2.0f;
            context.getMatrices().push();
            context.getMatrices().translate(infoPivotX, infoPivotY, 0.0f);
            context.getMatrices().scale(infoScale, infoScale, 1.0f);
            context.getMatrices().translate(-infoPivotX - slideOffset - 24.0f * (1.0f - infoVisible),
                    -infoPivotY + 8.0f * (1.0f - infoVisible), 0.0f);
            this.infoPanel.render(context, infoAlpha);
            context.getMatrices().pop();
        }
        for (int i = 0; i < this.panels.size(); i++) {
            CsPanel panel = this.panels.get(i);
            context.getMatrices().push();
            context.getMatrices().translate(slideOffset, 0.0f, 0.0f);
            panel.render(context, alpha);
            context.getMatrices().pop();
        }

        float x = (this.width - WIDTH()) / 2.0f;
        float y = (this.height - HEIGHT()) / 2.0f;
        BorderRadius radius = BorderRadius.all(CORNER_RADIUS);

        float scale = 0.93f + 0.07f * Math.max(0.0f, intro);
        float introX = -24.0f * (1.0f - alpha);
        float introY = 8.0f * (1.0f - alpha);
        float pivotX = x + WIDTH() / 2.0f;
        float pivotY = y + HEIGHT() / 2.0f;
        context.getMatrices().push();
        context.getMatrices().translate(pivotX, pivotY, 0.0f);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-pivotX + introX, -pivotY + introY, 0.0f);

        if (dev.bkclient.systems.modules.modules.visuals.Interface.blurGui())
            context.drawBlurredRect(x, y, WIDTH(), HEIGHT(), 12.0f, 2.0f, radius,
                    BACKGROUND_GLASS.withAlpha(BACKGROUND_GLASS.getAlpha() * alpha));
        context.drawRoundedRect(x, y, WIDTH(), HEIGHT(), radius, BACKGROUND.withAlpha(BACKGROUND.getAlpha() * alpha));
        context.drawRoundedBorder(x, y, WIDTH(), HEIGHT(), BORDER_THICKNESS, radius,
                BORDER.withAlpha(BORDER.getAlpha() * alpha));

        BorderRadius topRadius = new BorderRadius(CORNER_RADIUS, CORNER_RADIUS, 0.0f, 0.0f);
        context.drawRoundedRect(x, y, WIDTH(), TOP_BAR_H, topRadius, BAR_FILL.withAlpha(BAR_FILL.getAlpha() * alpha));
        context.drawRoundedBorder(x, y, WIDTH(), TOP_BAR_H, BORDER_THICKNESS, topRadius,
                BAR_BORDER.withAlpha(BAR_BORDER.getAlpha() * alpha));

        java.util.List<dev.bkclient.systems.modules.Module> mods =
                BKClientInit.getInstance().getModuleManager().getModules();
        int total = mods.size();
        int enabled = (int) mods.stream().filter(dev.bkclient.systems.modules.Module::isEnabled).count();
        Font countFont = Fonts.SEMIBOLD.getFont(7.0f);
        String enabledStr = String.valueOf(enabled);
        String restStr = " / " + total;
        float countCenterY = y + TOP_BAR_H / 2.0f;
        float countRight = x + WIDTH() - 12.0f;
        context.drawRightText(countFont, restStr, countRight, countCenterY - countFont.height() / 2.0f,
                new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.5f).withAlpha(255.0f * 0.5f * alpha));
        float enabledRight = countRight - countFont.width(restStr);
        context.drawRightText(countFont, enabledStr , enabledRight, countCenterY - countFont.height() / 2.0f,
                new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.75f).withAlpha(255.0f * 0.75f * alpha));
        float countIconSize = 9.0f;
        float countIconX = enabledRight - countFont.width(enabledStr) - 3.0f - countIconSize;
        context.drawIcon(Fonts.ICONS, 'q', countIconX, countCenterY - countIconSize / 2.0f + 0.32f, countIconSize,
                CsTheme.Color.accent().withAlpha(CsTheme.Color.accent().getAlpha() * alpha));

        float logoW = 17.5f;
        float logoH = 9.0f;
        float logoX = x + 12.0f;
        float logoCenterY = y + TOP_BAR_H / 2.0f;

        context.drawIcon(Fonts.ICONS, 'D', logoX + (logoW - 8.0f) / 2.0f - 5, logoCenterY - 10.0f, 20.0f,
                CsTheme.Color.accent().withAlpha(CsTheme.Color.accent().getAlpha() * alpha));

        this.tabCount = CATEGORY_NAMES.length;
        Font tabFont = Fonts.SEMIBOLD.getFont(6.0f);
        float tabIconSize = 9.5f;
        float tabIconGap = 3.0f;
        float tabPadX = 6.0f;
        float tabH = 17.0f;
        this.tabsY = logoCenterY - tabH / 2.0f;
        this.tabsH = tabH;
        float tcursor = logoX + logoW + 10.0f;
        float selTabX = 0.0f, selTabW = 0.0f;
        for (int i = 0; i < CATEGORY_NAMES.length; ++i) {
            String name = CATEGORY_NAMES[i];
            float contentW = tabIconSize + tabIconGap + tabFont.width(name);
            float tabWidth = tabPadX * 2.0f + contentW;
            this.tabX[i] = tcursor;
            this.tabW[i] = tabWidth;
            if (i == this.selectedCategory) {
                selTabX = tcursor;
                selTabW = tabWidth;
            }
            tcursor += tabWidth + 4.0f;
        }

        if (!this.tabSlideInit) {
            this.tabSlideX = selTabX;
            this.tabSlideW = selTabW;
            this.tabSlideInit = true;
        } else {
            float speed = 0.30f;
            this.tabSlideX += (selTabX - this.tabSlideX) * speed;
            this.tabSlideW += (selTabW - this.tabSlideW) * speed;
            if (Math.abs(selTabX - this.tabSlideX) < 0.5f) this.tabSlideX = selTabX;
            if (Math.abs(selTabW - this.tabSlideW) < 0.5f) this.tabSlideW = selTabW;
        }
        context.drawRoundedRect(this.tabSlideX, this.tabsY, this.tabSlideW, tabH, BorderRadius.all(6.0f),
                CsTheme.Color.accent().withAlpha(CsTheme.Color.accent().getAlpha() * 0.15f * alpha));

        for (int i = 0; i < CATEGORY_NAMES.length; ++i) {
            String name = CATEGORY_NAMES[i];
            char icon = CATEGORY_ICONS[i % CATEGORY_ICONS.length];
            float tabWidth = this.tabW[i];
            float tabPosX = this.tabX[i];

            boolean selected = i == this.selectedCategory;
            boolean hover = context.getMouseX() >= tabPosX && context.getMouseX() <= tabPosX + tabWidth
                    && context.getMouseY() >= this.tabsY && context.getMouseY() <= this.tabsY + tabH;

            if (hover && !selected) {
                context.drawRoundedRect(tabPosX, this.tabsY, tabWidth, tabH, BorderRadius.all(6.0f),
                        CsTheme.Color.accent().withAlpha(CsTheme.Color.accent().getAlpha() * 0.08f * alpha));
            }
            ColorRGBA nameColor = selected
                    ? ColorRGBA.WHITE
                    : new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f);
            ColorRGBA iconColor = selected ? CsTheme.Color.accent()
                    : new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f);

            context.drawIcon(Fonts.ICONS, icon, tabPosX + tabPadX, logoCenterY - tabIconSize / 2.0f, tabIconSize,
                    iconColor.withAlpha(iconColor.getAlpha() * alpha));
            context.drawText(tabFont, name, tabPosX + tabPadX + tabIconSize + tabIconGap,
                    logoCenterY - tabFont.height() / 2.0f, nameColor.withAlpha(nameColor.getAlpha() * alpha));
        }

        float contentX = x;
        float contentY = y + TOP_BAR_H;
        float contentW = WIDTH();
        float contentH = HEIGHT() - TOP_BAR_H - BOTTOM_BAR_H;
        this.catAnim.update(1.0f);
        float catV = this.catAnim.getValue();
        float contentAlpha = alpha * catV;
        float slide = (1.0f - catV) * 8.0f;
        context.getMatrices().push();
        context.getMatrices().translate(0.0f, slide, 0.0f);
        boolean searching = this.modsContent.hasSearch();
        if (!searching && this.selectedCategory == 4) {
            this.autoMarketContent.render(context, contentX, contentY, contentW, contentH, contentAlpha);
        } else if (!searching && this.selectedCategory == 5) {
            this.configContent.render(context, contentX, contentY, contentW, contentH, contentAlpha);
        } else {
            dev.bkclient.systems.modules.api.ModuleCategory cat = TAB_CATEGORY[this.selectedCategory];
            if (cat == null) {
                cat = dev.bkclient.systems.modules.api.ModuleCategory.COMBAT;
            }
            this.modsContent.render(context, cat, contentX, contentY, contentW, contentH, contentAlpha);
        }
        context.getMatrices().pop();

        float bottomY = y + HEIGHT() - BOTTOM_BAR_H;
        BorderRadius bottomRadius = new BorderRadius(0.0f, 0.0f, CORNER_RADIUS, CORNER_RADIUS);
        context.drawRoundedRect(x, bottomY, WIDTH(), BOTTOM_BAR_H, bottomRadius,
                BAR_FILL.withAlpha(BAR_FILL.getAlpha() * alpha));
        context.drawRoundedBorder(x, bottomY, WIDTH(), BOTTOM_BAR_H, BORDER_THICKNESS, bottomRadius,
                BAR_BORDER.withAlpha(BAR_BORDER.getAlpha() * alpha));

        float avSize = 20.0f;
        float avX = x + 8.0f;
        float avY = bottomY + (BOTTOM_BAR_H - avSize) / 2.0f;
        if (mc.player != null) {
            context.drawHead(mc.player, avX, avY, avSize, BorderRadius.all(5.0f),
                    ColorRGBA.WHITE.withAlpha(255.0f * alpha));
        } else {
            context.drawRoundedTexture(BKClientInit.id("textures/gui/bk-logo.png"), avX, avY, avSize, avSize,
                    BorderRadius.all(5.0f), ColorRGBA.WHITE.withAlpha(255.0f * alpha));
        }

        Font linkFont = Fonts.SEMIBOLD.getFont(5.5f);
        Font dateFont = Fonts.MEDIUM.getFont(5.5f);
        float tx = avX + avSize + 7.0f;
        float linkY = bottomY + BOTTOM_BAR_H / 2.0f - 7.5f;
        String playerName = mc.player != null ? mc.player.getName().getString() : "Player";
        context.drawText(linkFont, playerName, tx, linkY + 1,
                new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.75f).withAlpha(255.0f * 0.75f * alpha));
        context.drawText(dateFont, MODS_URL, tx, linkY + 9.0f,
                CsTheme.Color.accent().withAlpha(CsTheme.Color.accent().getAlpha() * alpha));
        float dotsSize = 6.0f;
        float dotsX = tx + linkFont.width(playerName) + 7.0f;
        float dotsY = linkY + 1.0f + (linkFont.height() - dotsSize) / 2.0f;
        this.footerDotsX = dotsX - 4.0f;
        this.footerDotsY = dotsY - 4.0f;
        this.footerDotsW = dotsSize + 8.0f;
        this.footerDotsH = dotsSize + 8.0f;
        context.drawIcon(Fonts.ICONS, 'w', dotsX, dotsY, dotsSize,
                new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.5f).withAlpha(255.0f * 0.5f * alpha));

        float lineX = x + 130.0f;
        context.drawRect(lineX, bottomY + 0.5f, 0.5f, BOTTOM_BAR_H - 1.0f,
                new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.06f).withAlpha(255.0f * 0.06f * alpha));

        Font catFont = Fonts.SEMIBOLD.getFont(6.0f);
        String catName = CATEGORY_NAMES[this.selectedCategory];
        char catIcon = CATEGORY_ICONS[this.selectedCategory % CATEGORY_ICONS.length];
        float catIconSize = 11.0f;
        float catGap = 3.0f;
        float catTotalW = catIconSize + catGap + catFont.width(catName);
        float catX = x + WIDTH() / 2.0f - catTotalW / 2.0f;
        float catCenterY = bottomY + BOTTOM_BAR_H / 2.0f;
        context.drawIcon(Fonts.ICONS, catIcon, catX, catCenterY - catIconSize / 2.0f, catIconSize,
                CsTheme.Color.accent().withAlpha(CsTheme.Color.accent().getAlpha() * alpha));
        context.drawText(catFont, catName, catX + catIconSize + catGap, catCenterY - catFont.height() / 2.0f,
                ColorRGBA.WHITE.withAlpha(255.0f * alpha));

        float searchLineX = x + WIDTH() - 102.5f;
        context.drawRect(searchLineX, bottomY + 0.5f, 0.5f, BOTTOM_BAR_H - 1.0f,
                new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.06f).withAlpha(255.0f * 0.06f * alpha));

        float searchIconSize = 6.0f;
        float rightEdge = x + WIDTH() - 12.0f;
        float sIconX = rightEdge - searchIconSize;
        context.drawIcon(Fonts.ICONS, 'v', sIconX, catCenterY - searchIconSize / 2.0f - 2.5f, searchIconSize + 3,
                new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f).withAlpha(255.0f * 0.25f * alpha));
        float sDivX = sIconX - 6.0f;
        context.drawRoundedRect(sDivX, catCenterY - 4.0f, 1.0f, 8.0f, BorderRadius.all(0.5f),
                new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.1f).withAlpha(255.0f * 0.1f * alpha));

        float clipLeft = searchLineX + 6.0f;
        float textRight = sDivX - 6.0f;
        this.searchFieldX = clipLeft;
        this.searchFieldW = (sIconX + searchIconSize) - clipLeft;
        this.searchFieldH = 13.0f;
        this.searchFieldY = catCenterY - this.searchFieldH / 2.0f;

        boolean searchFocused = this.searchField != null && this.searchField.isFocused();
        float sFocus = this.searchAnim.update(searchFocused ? 1.0f : 0.0f);

        String appending = this.searchField != null ? this.searchField.getAppending() : "";
        float appF = this.appendingAnim.update(!appending.isBlank() ? 1.0f : 0.0f);
        if (appF > 0.0f) {
            Font tipFont = Fonts.MEDIUM.getFont(7.0f);
            float tipX = x + WIDTH() / 2.0f;
            context.drawCenteredText(tipFont, Localizator.translate("search.tooltip.tab"),
                    tipX, bottomY - 14.0f * appF,
                    ColorRGBA.WHITE.withAlpha(150.0f * sFocus * appF));
            context.drawCenteredText(tipFont, Localizator.translate("search.tooltip.enter"),
                    tipX, bottomY - 6.0f * appF,
                    ColorRGBA.WHITE.withAlpha(150.0f * sFocus * appF));
        }

        if (this.searchField != null) {
            String built = this.searchField.getBuiltText();
            if (built.isEmpty() && !searchFocused) {
                Font sf = Fonts.SEMIBOLD.getFont(7.0f);
                context.drawRightText(sf, "Search..",
                        textRight, catCenterY - sf.height() / 2.0f,
                        new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f * alpha));
            } else {
                this.searchField.setAlpha(alpha);
                this.searchField.setTextColor(ColorRGBA.WHITE);
                ScissorUtility.push(context.getMatrices(), clipLeft, this.searchFieldY,
                        textRight - clipLeft + 2.0f, this.searchFieldH);
                this.searchField.set(clipLeft, this.searchFieldY, textRight - clipLeft, this.searchFieldH);
                this.searchField.render(context);
                ScissorUtility.pop();
            }
        }

        context.getMatrices().pop();
        this.modsContent.renderOverlay(context, contentAlpha);
        if ((this.infoPanelOpen || infoVisible > 0.01f) && this.infoPanel != null) {
            this.infoPanel.renderOverlay(context, alpha * infoVisible);
        }
        context.getMatrices().pop();

        if ((this.infoPanelOpen || infoVisible > 0.01f) && this.infoPanel != null) {
            this.infoPanel.renderScalePicker(screenContext, alpha * infoVisible);
        }
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (this.infoPanelOpen && this.infoPanel != null && this.infoPanel.hasActiveScalePicker()) {
            if (this.infoPanel.scalePickerClick(mouseX, mouseY, button)) {
                return;
            }
        }

        mouseX = this.logicalMouseX(mouseX);
        mouseY = this.logicalMouseY(mouseY);

        if (mouseX >= this.footerDotsX && mouseX <= this.footerDotsX + this.footerDotsW
                && mouseY >= this.footerDotsY && mouseY <= this.footerDotsY + this.footerDotsH) {
            if (button == MouseButton.LEFT) {
                this.infoPanelOpen = !this.infoPanelOpen;
                if (!this.infoPanelOpen && this.infoPanel != null) {
                    this.infoPanel.closeTransientOverlays();
                }
                return;
            }
            if (button == MouseButton.RIGHT) {
                this.infoPanelOpen = false;
                if (this.infoPanel != null) {
                    this.infoPanel.closeTransientOverlays();
                }
                return;
            }
            return;
        }

        if (this.infoPanelOpen && this.infoPanel != null && this.infoPanel.hasActivePicker()) {
            if (this.infoPanel.pickerClick(mouseX, mouseY, button)) {
                return;
            }
        }

        if (this.modsContent.handleOverlayClick(mouseX, mouseY, button)) {
            return;
        }

        if (this.infoPanelOpen && this.infoPanel != null) {
            if (button == MouseButton.RIGHT && this.infoPanel.isHovered(mouseX, mouseY)) {
                this.infoPanelOpen = false;
                return;
            }
            if (button == MouseButton.LEFT && this.infoPanel.handleClick(mouseX, mouseY)) {
                return;
            }
            if (this.infoPanel.isHovered(mouseX, mouseY)) {
                this.infoPanel.onMouseClicked(mouseX, mouseY, button);
                return;
            }
        }

        if (this.searchField != null) {
            boolean inSearchBox = button == MouseButton.LEFT && mouseX >= this.searchFieldX
                    && mouseX <= this.searchFieldX + this.searchFieldW
                    && mouseY >= this.searchFieldY && mouseY <= this.searchFieldY + this.searchFieldH;
            // Klik POZA polem głównego searcha → zdejmij jego fokus, żeby klik (np. w search bloków
            // wewnątrz karty) mógł przejąć klawiaturę. Bez tego główny search nadal łapał wpisywanie.
            if (!inSearchBox && this.searchField.isFocused()) {
                this.searchField.setFocused(false);
            }
            this.searchField.onMouseClicked(mouseX, mouseY, button);
            if (this.searchField.isFocused()) {
                this.modsContent.setSearchQuery(this.searchField.getBuiltText());
                return;
            }
        }

        if (button == MouseButton.LEFT && mouseX >= this.searchFieldX
                && mouseX <= this.searchFieldX + this.searchFieldW
                && mouseY >= this.searchFieldY && mouseY <= this.searchFieldY + this.searchFieldH) {
            if (this.searchField != null) this.searchField.setFocused(true);
            return;
        }

        if (button == MouseButton.LEFT) {
            for (int i = 0; i < this.tabCount; ++i) {
                if (mouseX >= this.tabX[i] && mouseX <= this.tabX[i] + this.tabW[i]
                        && mouseY >= this.tabsY && mouseY <= this.tabsY + this.tabsH) {
                    if (this.selectedCategory != i) {
                        this.modsContent.resetScroll();
                        this.catAnim.setValue(0.0f);
                    }
                    this.selectedCategory = i;
                    return;
                }
            }
        }

        if (!this.modsContent.hasSearch() && this.selectedCategory == 5) {
            if (this.configContent.handleClick(mouseX, mouseY, button)) {
                return;
            }
        }

        if (!this.modsContent.hasSearch() && this.selectedCategory == 4 && button == MouseButton.LEFT) {
            if (this.autoMarketContent.handleClick(mouseX, mouseY)) {
                return;
            }
        }

        if (this.selectedCategory < 4 || this.modsContent.hasSearch()) {
            dev.bkclient.systems.modules.api.ModuleCategory cat =
                    this.selectedCategory < 4 ? TAB_CATEGORY[this.selectedCategory]
                            : dev.bkclient.systems.modules.api.ModuleCategory.COMBAT;
            if (cat != null && this.modsContent.handleClick(cat, mouseX, mouseY, button)) {
                return;
            }
        }

        for (int i = this.panels.size() - 1; i >= 0; --i) {
            CsPanel panel = this.panels.get(i);
            if (panel.isHovered(mouseX, mouseY)) {
                panel.onMouseClicked(mouseX, mouseY, button);
                return;
            }
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        mouseX = this.logicalMouseX(mouseX);
        mouseY = this.logicalMouseY(mouseY);
        if (this.infoPanelOpen && this.infoPanel != null) {
            this.infoPanel.pickerRelease();
            this.infoPanel.onMouseReleased(mouseX, mouseY, button);
        }
        if (this.searchField != null) {
            this.searchField.onMouseReleased(mouseX, mouseY, button);
        }
        this.autoMarketContent.handleRelease();
        this.configContent.handleRelease();
        this.modsContent.handleRelease();
        for (CsPanel panel : this.panels) {
            panel.onMouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {
        mouseX = this.logicalMouseX(mouseX);
        mouseY = this.logicalMouseY(mouseY);
        if (this.infoPanelOpen && this.infoPanel != null) {
            this.infoPanel.onMouseDragged(mouseX, mouseY);
        }
        if (!this.modsContent.hasSearch() && this.selectedCategory == 4
                && this.autoMarketContent.handleDrag(mouseX, mouseY)) {
            return;
        }
        for (CsPanel panel : this.panels) {
            panel.onMouseDragged(mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.infoPanelOpen && this.infoPanel != null && this.infoPanel.hasActiveScalePicker()
                && this.infoPanel.scalePickerKeyPressed(keyCode)) {
            return true;
        }

        // Search w karcie / otwarty dropdown ma pierwszeństwo nad głównym searchem — inaczej
        // gdy znajdziesz moduł przez główny search i otworzysz jego listę (np. XRay → bloki),
        // główny search pożerał wpisywanie i pole w karcie się "bugowało".
        if (this.selectedCategory < 4 && this.modsContent.isCapturing()
                && this.modsContent.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        boolean searchFocused = this.searchField != null && this.searchField.isFocused();

        if (searchFocused) {
            if (keyCode == 256) { // Esc — zdejmij fokus
                this.searchField.setFocused(false);
                return true;
            }
            this.searchField.onKeyPressed(keyCode, scanCode, modifiers);
            this.modsContent.setSearchQuery(this.searchField.getBuiltText());
            return true;
        }

        if (net.minecraft.client.gui.screen.Screen.hasControlDown() && keyCode == 70) {
            if (this.searchField != null) this.searchField.setFocused(true);
            return true;
        }

        if (!this.modsContent.hasSearch() && this.selectedCategory == 4
                && this.autoMarketContent.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (!this.modsContent.hasSearch() && this.selectedCategory == 5
                && this.configContent.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (this.selectedCategory < 4 && this.modsContent.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.selectedCategory < 4 && this.modsContent.isCapturing()
                && this.modsContent.charTyped(chr)) {
            return true;
        }

        boolean searchFocused = this.searchField != null && this.searchField.isFocused();

        if (searchFocused) {
            this.searchField.charTyped(chr, modifiers);
            this.modsContent.setSearchQuery(this.searchField.getBuiltText());
            return true;
        }
        if (!this.modsContent.hasSearch() && this.selectedCategory == 4
                && this.autoMarketContent.charTyped(chr, modifiers)) {
            return true;
        }
        if (!this.modsContent.hasSearch() && this.selectedCategory == 5
                && this.configContent.charTyped(chr, modifiers)) {
            return true;
        }
        if (this.selectedCategory < 4 && this.modsContent.charTyped(chr)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.infoPanelOpen && this.infoPanel != null && this.infoPanel.hasActiveScalePicker()
                && this.infoPanel.scalePickerScroll(verticalAmount)) {
            return true;
        }
        mouseX = this.logicalMouseX(mouseX);
        mouseY = this.logicalMouseY(mouseY);
        if (!this.modsContent.hasSearch() && this.selectedCategory == 4
                && this.autoMarketContent.onScroll(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        if (!this.modsContent.hasSearch() && this.selectedCategory == 5
                && this.configContent.onScroll(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        if (this.selectedCategory < 4 || this.modsContent.hasSearch()) {
            this.modsContent.onScroll(mouseX, mouseY, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
    }
}
