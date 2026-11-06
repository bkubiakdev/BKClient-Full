package dev.bkclient.ui.menu.clickgui;

import dev.bkclient.BKClientInit;
import dev.bkclient.utility.render.batching.impl.FontBatching;
import dev.bkclient.utility.render.batching.impl.SquircleBatching;
import net.minecraft.client.render.VertexFormats;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.ui.components.ColorPicker;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.MenuScreen;
import dev.bkclient.ui.menu.api.MenuCategory;
import dev.bkclient.ui.menu.clickgui.components.ClickGuiModuleCard;
import dev.bkclient.ui.menu.clickgui.components.ClickGuiSettingsPanel;
import dev.bkclient.ui.menu.clickgui.components.CornerSettingsPanel;
import dev.bkclient.ui.menu.clickgui.components.ThemeSettingsView;
import dev.bkclient.ui.menu.clickgui.content.AutoMarketContent;
import dev.bkclient.ui.menu.clickgui.content.ConfigsContent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.render.Utils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** BK-shaped ClickGUI using the shared Liquid/Glass interface style. */
public class ClickGuiScreen extends MenuScreen implements IMinecraft {
   private static final float WINDOW_WIDTH = 465.0F;
   private static final float WINDOW_HEIGHT = 320.0F;
   private static final float WINDOW_RADIUS = 16.0F;
   private static final float TOP_BAR_HEIGHT = 36.0F;
   private static final float BOTTOM_BAR_HEIGHT = 34.0F;
   private static final float CONTENT_PADDING = 10.0F;
   private static final float COLUMN_GAP = 8.0F;
   private static final float CARD_GAP = 7.0F;
   private static final float PLAYER_PANEL_WIDTH = 125.0F;
   private static final float PLAYER_PANEL_HEIGHT = 172.0F;
   private static final float PLAYER_PANEL_OVERLAP = 5.0F;
   private static final int COLUMNS = 3;
   private static final long CLOSE_ANIMATION_MS = 620L;
   private static final double CLOSE_ANCHOR_DISTANCE = 7.5D;

   private final List<ClickGuiModuleCard> cards = new ArrayList<>();
   private final List<ClickGuiModuleCard> visibleCardsCache = new ArrayList<>();
   private final List<ClickGuiSettingsPanel> settingsPanels = new ArrayList<>();
   private final Animation sideSpaceAnimation = new Animation(300L, Easing.QUARTIC_OUT);
   private ClickGuiSettingsPanel focusedSettingsPanel;
   private float sideSpace;
   private final List<ColorPicker> colorPickers = new ArrayList<>();
   private final float[] columnHeights = new float[3];
   protected final ScrollHandler scroll = new ScrollHandler();
   private final Animation categoryAnimation = new Animation(250L, Easing.FIGMA_EASE_IN_OUT);
   private final CornerSettingsPanel cornerSettings = new CornerSettingsPanel();
   private final ThemeSettingsView themeSettings = new ThemeSettingsView();
   private final AutoMarketContent autoMarketContent = new AutoMarketContent();
   private final ConfigsContent configsContent = new ConfigsContent();
   private final boolean openAutoMarketInitially;
   protected TextField searchField;
   protected MenuCategory selectedCategory = MenuCategory.COMBAT;
   protected float windowX;
   protected float windowY;
   private float contentY;
   private float contentHeight;
   private final float[] tabX = new float[MenuCategory.values().length];
   private final float[] tabWidth = new float[MenuCategory.values().length];
   private final java.util.Map<ClickGuiModuleCard, Vec2f> cardPositions = new java.util.HashMap<>();
   private final java.util.Map<ClickGuiModuleCard, Vec2f> reorderOrigins = new java.util.HashMap<>();
   private final Animation reorderAnimation = new Animation(360L, 1.0F, Easing.QUARTIC_OUT);
   private int cachedFavoriteCount;
   private long cachedFavoritesRevision = -1;
   private String cachedQuery;
   private MenuCategory cachedCategory;
   private float currentRenderScale = 1.0F;
   protected boolean themeView;
   protected boolean autoMarketView;
   protected boolean configsView;
   private float autoMarketTabX;
   private float autoMarketTabWidth;
   private float configsTabX;
   private float configsTabWidth;
   private float themeTabX;
   private float themeTabWidth;
   private float tabY;
   private float tabHeight;
   private Vec3d closeAnchor;
   private Vec3d closeRight;
   private float closeInitialRightPixels = 1.0F;
   private float closeInitialRightAngle;

   public ClickGuiScreen() {
      this(false);
   }

   public ClickGuiScreen(boolean openAutoMarketInitially) {
      this.openAutoMarketInitially = openAutoMarketInitially;
   }

   @Override
   protected void init() {
      super.init();
      this.closing = false;
      this.closeAnchor = null;
      this.closeRight = null;
      this.closeInitialRightPixels = 0.0F;
      this.closeInitialRightAngle = 0.0F;
      this.menuAnimation.setValue(0.0F);
      this.categoryAnimation.setValue(1.0F);
      this.cards.clear();
      this.cardPositions.clear();
      this.reorderOrigins.clear();
      this.reorderAnimation.setValue(1.0F);
      this.settingsPanels.clear();
      this.focusedSettingsPanel = null;
      this.sideSpaceAnimation.setValue(0);
      this.sideSpace = 0;
      BKClientInit.getInstance().getModuleManager().getModules().stream()
         .filter(module -> !module.isHidden())
         .sorted(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER))
         .map(module -> new ClickGuiModuleCard(module, this::openSettingsPanel))
         .forEach(this.cards::add);
      this.searchField = new TextField(Fonts.MEDIUM.getFont(10.0F));
      this.searchField.setPreview(Localizator.translate("spotlight.title"));
      this.searchField.setTextColor(Colors.getTextColor());
      this.scroll.reset();
      this.cornerSettings.reset();
      this.cachedQuery = null;
      this.cachedCategory = null;
      this.themeView = false;
      this.autoMarketView = this.openAutoMarketInitially;
      this.configsView = false;
   }

   @Override
   public void tick() {
      this.handleMovementKeys();
      super.tick();
   }

   private void handleMovementKeys() {
      if (TextField.hasFocusedField()) { TextField.releaseMovementKeys(); return; }
      if (mc.player == null || this.searchField != null && this.searchField.isFocused()) {
         return;
      }
      long windowHandle = mc.getWindow().getHandle();
      this.syncKey(mc.options.forwardKey, windowHandle);
      this.syncKey(mc.options.backKey, windowHandle);
      this.syncKey(mc.options.leftKey, windowHandle);
      this.syncKey(mc.options.rightKey, windowHandle);
      this.syncKey(mc.options.jumpKey, windowHandle);
      if (mc.player.getAbilities().flying) {
         int keyCode = InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode();
         mc.options.sneakKey.setPressed(InputUtil.isKeyPressed(windowHandle, keyCode));
      }
   }

   private void syncKey(KeyBinding key, long windowHandle) {
      int keyCode = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode();
      key.setPressed(InputUtil.isKeyPressed(windowHandle, keyCode));
   }

   @Override
   public void render(UIContext context) {
      this.menuAnimation.setDuration(this.closing ? CLOSE_ANIMATION_MS : 420L);
      this.menuAnimation.setEasing(this.closing ? Easing.LINEAR : Easing.BAKEK_SMALLER);
      float intro = this.menuAnimation.update(this.closing ? 0.0F : 1.0F);
      float progress = Math.max(0.0F, Math.min(1.0F, intro));
      if (progress <= 0.001F) {
         return;
      }
      // BoczekBK keeps the detached panel opaque while its world-space scale
      // collapses; only the opening animation fades the content in.
      float alpha = this.closing ? 1.0F : progress;

      {
         this.sideSpaceAnimation.update(this.settingsPanels.stream().anyMatch(ClickGuiSettingsPanel::isShowing));
         this.sideSpace = 180 * this.sideSpaceAnimation.getValue();
         for (ClickGuiModuleCard card : this.cards) card.setExternalSettingsShowing(false);
         for (ClickGuiSettingsPanel panel : this.settingsPanels) if (panel.isShowing()) panel.getCard().setExternalSettingsShowing(true);
      }
      this.windowX = (this.width - WINDOW_WIDTH) / 2.0F;
      this.windowY = (this.height - WINDOW_HEIGHT) / 2.0F;

      if (this.sideSpace > 0.01F) this.windowX -= this.sideSpace * this.fittedMenuScale() / 2;
      this.contentY = this.windowY + TOP_BAR_HEIGHT;
      this.contentHeight = WINDOW_HEIGHT - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT;

      float pivotX = this.windowX + WINDOW_WIDTH / 2.0F;
      float pivotY = this.windowY + WINDOW_HEIGHT / 2.0F;
      int rawMouseX = context.getMouseX();
      int rawMouseY = context.getMouseY();
      context.setMouse((int)this.logicalMouseX(rawMouseX), (int)this.logicalMouseY(rawMouseY));
      context.getMatrices().push();
      if (!this.applyMenuTransform(context.getMatrices(), pivotX, pivotY, progress)) {
         context.getMatrices().pop();
         context.setMouse(rawMouseX, rawMouseY);
         return;
      }

      this.drawWindow(context, alpha);
      this.drawTopBar(context, alpha);
      List<ClickGuiModuleCard> visibleCards = this.themeView || this.autoMarketView || this.configsView ? List.of() : this.visibleCards();
      if (this.autoMarketView) {
         ScissorUtility.push(context.getMatrices(), this.windowX, this.contentY, WINDOW_WIDTH, this.contentHeight);
         this.autoMarketContent.render(context, this.windowX, this.contentY, WINDOW_WIDTH, this.contentHeight, alpha);
         ScissorUtility.pop();
      } else if (this.configsView) {
         ScissorUtility.push(context.getMatrices(), this.windowX, this.contentY, WINDOW_WIDTH, this.contentHeight);
         this.configsContent.render(context, this.windowX, this.contentY, WINDOW_WIDTH, this.contentHeight, alpha);
         ScissorUtility.pop();
      } else if (this.themeView) {
         ScissorUtility.push(context.getMatrices(), this.windowX, this.contentY, WINDOW_WIDTH, this.contentHeight);
         this.themeSettings.render(
            context,
            this.windowX + CONTENT_PADDING,
            this.contentY + CONTENT_PADDING,
            WINDOW_WIDTH - CONTENT_PADDING * 2.0F,
            this.contentHeight - CONTENT_PADDING * 2.0F,
            alpha
         );
         ScissorUtility.pop();
      } else {
         this.drawCards(context, alpha, visibleCards);
      }
      this.drawBottomBar(context, alpha);

      for (ClickGuiModuleCard card : visibleCards) {
         if (this.isInContentViewport(card)) {
            card.renderOverlay(context);
         }
      }
      ClickGuiModuleCard playerPanel = this.activePlayerCardPanel();
      if (playerPanel != null) {
         float logicalRight = pivotX + (this.width - pivotX) / Math.max(0.01F, this.currentRenderScale);
         float preferredX = this.windowX + WINDOW_WIDTH - PLAYER_PANEL_OVERLAP
            - (this.sideSpace > 0.01F ? PLAYER_PANEL_WIDTH : 0);
         float panelX = Math.min(preferredX, logicalRight - PLAYER_PANEL_WIDTH - 4.0F);
         float panelY = this.windowY + (WINDOW_HEIGHT - PLAYER_PANEL_HEIGHT) / 2.0F;
         playerPanel.renderPlayerCardPanel(
            context, panelX, panelY, PLAYER_PANEL_WIDTH, PLAYER_PANEL_HEIGHT, alpha
         );
      }
      this.cornerSettings.render(context, this.windowX, this.windowY + WINDOW_HEIGHT - BOTTOM_BAR_HEIGHT, alpha);
      this.layoutSettingsPanels();
      for (ClickGuiSettingsPanel panel : this.settingsPanels) {
         panel.render(context);
      }
      this.settingsPanels.removeIf(panel -> !panel.isShowing() && panel.getAnimation().getValue() <= 0.001F);
      for (ColorPicker picker : this.colorPickers) {
         picker.render(context);
      }
      this.colorPickers.removeIf(picker -> picker.getAnimation().getValue() == 0.0F && !picker.isShowing());


      context.getMatrices().pop();
      context.setMouse(rawMouseX, rawMouseY);
      if (!this.closing) {
         this.cornerSettings.renderScalePicker(context, alpha);
      }
   }

   private float fittedMenuScale() {
      if (this.sideSpace <= 0.01F) return Interface.menuScale();
      return Math.min(Interface.menuScale(), Math.min((this.width - 20.0F) / (WINDOW_WIDTH + this.sideSpace),
         (this.height - 20.0F) / WINDOW_HEIGHT));
   }

   private void openSettingsPanel(ClickGuiModuleCard card) {
      if (card.getModule().getDisplaySettings().stream().noneMatch(dev.bkclient.systems.setting.Setting::isVisible)) return;
      for (ClickGuiSettingsPanel panel : this.settingsPanels) {
         if (panel.getCard() == card && panel.isShowing()) { panel.setShowing(false); return; }
      }
      ClickGuiSettingsPanel panel = new ClickGuiSettingsPanel(card, this, 0, this.windowX + WINDOW_WIDTH + 10, this.windowY);
      if (panel.getComponents().stream().noneMatch(component -> component.getSetting().isVisible())) return;
      boolean exclusive = panel.needsFullHeight()
         || this.settingsPanels.stream().anyMatch(existing -> existing.isShowing() && existing.needsFullHeight());
      if (exclusive) {
         for (ClickGuiSettingsPanel existing : this.settingsPanels) existing.setShowing(false);
      } else if (this.settingsPanels.stream().filter(ClickGuiSettingsPanel::isShowing).count() >= 2) {
         ClickGuiSettingsPanel replace = this.settingsPanels.stream()
            .filter(existing -> existing.isShowing() && !existing.isPinned()).findFirst().orElse(null);
         if (replace == null) return;
         replace.setShowing(false);
      }
      this.settingsPanels.add(panel);
      this.focusedSettingsPanel = panel;
      this.searchField.setFocused(false);
   }

   private void layoutSettingsPanels() {
      // Re-evaluate real visibility, not animation opacity, when dependent options expand.
      ClickGuiSettingsPanel expanded = this.settingsPanels.stream()
         .filter(panel -> panel.isShowing() && panel.needsFullHeight()).findFirst().orElse(null);
      if (expanded != null) {
         for (ClickGuiSettingsPanel panel : this.settingsPanels) {
            if (panel != expanded && panel.isShowing()) {
               panel.setShowing(false);
               if (this.focusedSettingsPanel == panel) this.focusedSettingsPanel = expanded;
            }
         }
      }
      float nextY = this.windowY;
      for (ClickGuiSettingsPanel panel : this.settingsPanels) {
         if (!panel.isShowing()) continue;
         panel.dock(this.windowX + WINDOW_WIDTH + 10, nextY);
         nextY += Math.min(WINDOW_HEIGHT, panel.contentHeight()) + 10.0F;
      }
   }

   private boolean clickSettingsPanel(double x, double y, MouseButton button) {
      for (int i = this.settingsPanels.size() - 1; i >= 0; i--) {
         ClickGuiSettingsPanel panel = this.settingsPanels.get(i);
         if (panel.isShowing() && (panel.isHovered(x, y) || panel.hasOpenDropdown())) {
            if (this.focusedSettingsPanel != panel && TextField.LAST_FIELD != null) TextField.LAST_FIELD.setFocused(false);
            this.focusedSettingsPanel = panel;
            this.searchField.setFocused(false);
            panel.onMouseClicked(x, y, button);
            this.settingsPanels.remove(i);
            this.settingsPanels.add(panel);
            return true;
         }
      }
      if (this.focusedSettingsPanel != null && TextField.LAST_FIELD != null) TextField.LAST_FIELD.setFocused(false);
      this.focusedSettingsPanel = null;
      return false;
   }

   protected void drawWindow(UIContext context, float alpha) {
      BorderRadius radius = BorderRadius.all(WINDOW_RADIUS);
      if (Interface.backgroundBlurEnabled()) {
         context.drawBlurredRect(
            this.windowX,
            this.windowY,
            WINDOW_WIDTH,
            WINDOW_HEIGHT,
            25.0F,
            WINDOW_RADIUS,
            radius,
            new ColorRGBA(0.0F, 0.0F, 0.0F, 80.0F * alpha)
         );
      }
      if (Interface.showGlass()) {
         context.drawLiquidGlass(
            this.windowX,
            this.windowY,
            WINDOW_WIDTH,
            WINDOW_HEIGHT,
            2.0F,
            0.08F,
            radius,
            ColorRGBA.WHITE.withAlpha(255.0F * Interface.glass() * alpha)
         );
         context.drawRoundedRect(
            this.windowX, this.windowY, WINDOW_WIDTH, WINDOW_HEIGHT, radius,
            Colors.getBackgroundColor().withAlpha(255.0F * (0.8F - 0.3F * Interface.glass()) * alpha)
         );
      } else {
         if (Interface.backgroundBlurEnabled()) {
            context.drawBlurredRect(
               this.windowX, this.windowY, WINDOW_WIDTH, WINDOW_HEIGHT,
               45.0F, WINDOW_RADIUS, radius,
               ColorRGBA.WHITE.withAlpha(255.0F * alpha * Interface.minimalizm())
            );
         }
         context.drawRoundedRect(
            this.windowX, this.windowY, WINDOW_WIDTH, WINDOW_HEIGHT, radius,
            Colors.getBackgroundColor().withAlpha(200.0F * alpha)
         );
      }
   }

   private void drawTopBar(UIContext context, float alpha) {
      BorderRadius topRadius = new BorderRadius(WINDOW_RADIUS, WINDOW_RADIUS, 0.0F, 0.0F);
      context.drawRoundedRect(
         this.windowX,
         this.windowY,
         WINDOW_WIDTH,
         TOP_BAR_HEIGHT,
         topRadius,
         Colors.getTextColor().withAlpha(7.0F * alpha)
      );
      context.drawRect(
         this.windowX,
         this.windowY + TOP_BAR_HEIGHT - 0.5F,
         WINDOW_WIDTH,
         0.5F,
         Colors.getTextColor().withAlpha(14.0F * alpha)
      );

      context.drawTextureSmooth(
         BKClientInit.id("textures/gui/bk-logo.png"),
         this.windowX + 10.0F,
         this.windowY + 7.0F,
         24.0F,
         24.0F,
         Colors.getTextColor().mulAlpha(alpha)
      );

      float navigationScale = Interface.navigationScale();
      Font tabFont = Fonts.MEDIUM.getFont(6.0F * navigationScale);
      float requiredNavigationWidth = 0.0F;
      for (MenuCategory category : MenuCategory.values()) {
         String name = Localizator.translate("spotlight.category." + category.getName().toLowerCase(Locale.ROOT));
         requiredNavigationWidth += tabFont.width(name) + 16.0F * navigationScale;
      }
      requiredNavigationWidth += tabFont.width("AutoMarket") + tabFont.width("Library") + tabFont.width("Theme")
         + 48.0F * navigationScale;
      float availableNavigationWidth = WINDOW_WIDTH - 67.0F - 43.0F;
      if (requiredNavigationWidth > availableNavigationWidth) {
         navigationScale *= availableNavigationWidth / requiredNavigationWidth;
         tabFont = Fonts.MEDIUM.getFont(6.0F * navigationScale);
      }
      this.tabHeight = 18.0F * navigationScale;
      this.tabY = this.windowY + (TOP_BAR_HEIGHT - this.tabHeight) / 2.0F;
      float cursor = this.windowX + 67.0F;
      for (MenuCategory category : MenuCategory.values()) {
         int index = category.ordinal();
         String categoryName = Localizator.translate("spotlight.category." + category.getName().toLowerCase(Locale.ROOT));
         float width = tabFont.width(categoryName) + 12.0F * navigationScale;
         this.tabX[index] = cursor;
         this.tabWidth[index] = width;
         cursor += width + 4.0F * navigationScale;
      }
      this.autoMarketTabX = cursor;
      this.autoMarketTabWidth = tabFont.width("AutoMarket") + 12.0F * navigationScale;
      cursor += this.autoMarketTabWidth + 4.0F * navigationScale;
      this.configsTabX = cursor;
      this.configsTabWidth = tabFont.width("Library") + 12.0F * navigationScale;
      cursor += this.configsTabWidth + 4.0F * navigationScale;
      this.themeTabX = cursor;
      this.themeTabWidth = tabFont.width("Theme") + 12.0F * navigationScale;

      boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
         && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
      ColorRGBA categoryText = dark ? ColorRGBA.WHITE : new ColorRGBA(10.0F, 10.0F, 10.0F);
      for (MenuCategory category : MenuCategory.values()) {
         int index = category.ordinal();
         boolean selected = !this.themeView && !this.autoMarketView && !this.configsView && category == this.selectedCategory;
         String categoryName = Localizator.translate("spotlight.category." + category.getName().toLowerCase(Locale.ROOT));
         ColorRGBA background = selected
            ? Colors.getAccent().withAlpha(120.0F * alpha)
            : Colors.getBackgroundColor().withAlpha(80.0F * alpha);
         context.drawLiquidGlass(
            this.tabX[index], this.tabY, this.tabWidth[index], this.tabHeight,
            2.0F, 0.05F, BorderRadius.all(8.0F), background
         );
         context.drawText(
            tabFont, categoryName,
            this.tabX[index] + this.tabWidth[index] / 2.0F - tabFont.width(categoryName) / 2.0F,
            this.tabY + GuiUtility.getMiddleOfBox(tabFont.height(), this.tabHeight) - 0.5F,
            selected ? Colors.WHITE : categoryText.mulAlpha(0.6F * alpha)
         );
      }

      ColorRGBA autoMarketBackground = this.autoMarketView
         ? Colors.getAccent().withAlpha(120.0F * alpha)
         : Colors.getBackgroundColor().withAlpha(80.0F * alpha);
      context.drawLiquidGlass(
         this.autoMarketTabX, this.tabY, this.autoMarketTabWidth, this.tabHeight,
         2.0F, 0.05F, BorderRadius.all(8.0F), autoMarketBackground
      );
      context.drawCenteredText(
         tabFont, "AutoMarket", this.autoMarketTabX + this.autoMarketTabWidth / 2.0F,
         this.tabY + GuiUtility.getMiddleOfBox(tabFont.height(), this.tabHeight) - 0.5F,
         this.autoMarketView ? Colors.WHITE : categoryText.mulAlpha(0.6F * alpha)
      );

      ColorRGBA configsBackground = this.configsView
         ? Colors.getAccent().withAlpha(120.0F * alpha)
         : Colors.getBackgroundColor().withAlpha(80.0F * alpha);
      context.drawLiquidGlass(
         this.configsTabX, this.tabY, this.configsTabWidth, this.tabHeight,
         2.0F, 0.05F, BorderRadius.all(8.0F), configsBackground
      );
      context.drawCenteredText(
         tabFont, "Library", this.configsTabX + this.configsTabWidth / 2.0F,
         this.tabY + GuiUtility.getMiddleOfBox(tabFont.height(), this.tabHeight) - 0.5F,
         this.configsView ? Colors.WHITE : categoryText.mulAlpha(0.6F * alpha)
      );

      ColorRGBA themeBackground = this.themeView
         ? Colors.getAccent().withAlpha(120.0F * alpha)
         : Colors.getBackgroundColor().withAlpha(80.0F * alpha);
      context.drawLiquidGlass(
         this.themeTabX, this.tabY, this.themeTabWidth, this.tabHeight,
         2.0F, 0.05F, BorderRadius.all(8.0F), themeBackground
      );
      context.drawCenteredText(
         tabFont, "Theme", this.themeTabX + this.themeTabWidth / 2.0F,
         this.tabY + GuiUtility.getMiddleOfBox(tabFont.height(), this.tabHeight) - 0.5F,
         this.themeView ? Colors.WHITE : categoryText.mulAlpha(0.6F * alpha)
      );

      int enabled = 0;
      for (Module module : BKClientInit.getInstance().getModuleManager().getModules()) {
         if (module.isEnabled()) {
            ++enabled;
         }
      }
      int total = BKClientInit.getInstance().getModuleManager().getModules().size();
      context.drawRightText(
         Fonts.MEDIUM.getFont(6.5F),
         enabled + " / " + total,
         this.windowX + WINDOW_WIDTH - 12.0F,
         this.windowY + 15.0F,
         Colors.getTextColor().mulAlpha(0.55F * alpha)
      );
   }











   private void drawCards(UIContext context, float alpha, List<ClickGuiModuleCard> visible) {
      this.categoryAnimation.update(1.0F);
      float columnWidth = (WINDOW_WIDTH - CONTENT_PADDING * 2.0F - COLUMN_GAP * (COLUMNS - 1)) / COLUMNS;
      java.util.Arrays.fill(this.columnHeights, 0.0F);
      float scrollOffset = (float)this.scroll.getValue();
      float reorder = this.reorderAnimation.update(1.0F);

      ScissorUtility.push(context.getMatrices(), this.windowX, this.contentY, WINDOW_WIDTH, this.contentHeight);
      for (int i = 0; i < visible.size(); ++i) {
         ClickGuiModuleCard card = visible.get(i);
         int column = i % COLUMNS;
         float localX = CONTENT_PADDING + column * (columnWidth + COLUMN_GAP);
         float localY = CONTENT_PADDING + this.columnHeights[column];
         Vec2f origin = this.reorderOrigins.get(card);
         if (origin != null && reorder < 1.0F) {
            localX = origin.x + (localX - origin.x) * reorder;
            localY = origin.y + (localY - origin.y) * reorder;
         }
         this.cardPositions.put(card, new Vec2f(localX, localY));
         float cardX = this.windowX + localX;
         float cardY = this.contentY + localY - scrollOffset;
         card.set(cardX, cardY, columnWidth, card.getHeight());
         if (this.isInContentViewport(card)) {
            card.renderForBatch(context);
         }
         this.columnHeights[column] += card.getHeight() + CARD_GAP;
      }
      SquircleBatching tracks = new SquircleBatching(3.0F);
      for (ClickGuiModuleCard card : visible) {
         if (this.isInContentViewport(card)) card.renderToggleTrack(context);
      }
      tracks.draw();
      SquircleBatching thumbs = new SquircleBatching(7.0F);
      for (ClickGuiModuleCard card : visible) {
         if (this.isInContentViewport(card) && !card.hasGlassThumb()) card.renderToggleThumb(context);
      }
      thumbs.draw();
      for (ClickGuiModuleCard card : visible) {
         if (this.isInContentViewport(card) && card.hasGlassThumb()) card.renderToggleThumb(context);
      }
      for (ClickGuiModuleCard card : visible) {
         if (this.isInContentViewport(card)) card.renderKeyBadgeBackground(context);
      }
      FontBatching badges = new FontBatching(VertexFormats.POSITION_TEXTURE_COLOR, Fonts.MEDIUM.getFont(9.0F).getFont());
      for (ClickGuiModuleCard card : visible) {
         if (this.isInContentViewport(card)) card.renderHeaderBadges(context);
      }
      badges.draw();
      // Texture icons render after the text batch has been flushed.
      for (ClickGuiModuleCard card : visible) {
         if (this.isInContentViewport(card)) card.renderFavoriteBadge(context);
      }
      ScissorUtility.pop();

      float tallest = 0.0F;
      for (float columnHeight : this.columnHeights) {
         tallest = Math.max(tallest, columnHeight);
      }
      this.scroll.setMax(Math.min(0.0F, this.contentHeight - CONTENT_PADDING * 2.0F - tallest));
      this.scroll.update();
   }

   private void drawBottomBar(UIContext context, float alpha) {
      float bottomY = this.windowY + WINDOW_HEIGHT - BOTTOM_BAR_HEIGHT;
      BorderRadius bottomRadius = new BorderRadius(0.0F, 0.0F, WINDOW_RADIUS, WINDOW_RADIUS);
      context.drawRoundedRect(
         this.windowX,
         bottomY,
         WINDOW_WIDTH,
         BOTTOM_BAR_HEIGHT,
         bottomRadius,
         Colors.getTextColor().withAlpha(7.0F * alpha)
      );
      context.drawRect(this.windowX, bottomY, WINDOW_WIDTH, 0.5F, Colors.getTextColor().withAlpha(14.0F * alpha));

      context.drawCenteredText(
         Fonts.MEDIUM.getFont(7.0F),
         this.autoMarketView ? "AutoMarket" : this.configsView ? "Library" : this.themeView ? "Theme" : this.selectedCategory.getName(),
         this.windowX + WINDOW_WIDTH / 2.0F,
         bottomY + 13.0F,
         Colors.getAccent().mulAlpha(alpha)
      );

      if (this.themeView || this.autoMarketView || this.configsView) {
         context.drawRightText(
            Fonts.MEDIUM.getFont(5.2F), this.autoMarketView ? "Registry & automation" : this.configsView ? "Local & cloud profiles" : "Live preview",
            this.windowX + WINDOW_WIDTH - 13.0F, bottomY + 14.0F,
            Colors.getTextColor().mulAlpha(0.35F * alpha)
         );
         return;
      }

      float searchWidth = 150.0F;
      float searchHeight = 26.0F;
      float searchX = this.windowX + WINDOW_WIDTH - searchWidth - 6.0F;
      float searchY = bottomY + (BOTTOM_BAR_HEIGHT - searchHeight) / 2.0F;
      BorderRadius searchRadius = BorderRadius.all(13.0F);
      context.drawBlurredRect(
         searchX, searchY, searchWidth, searchHeight, 25.0F, 13.0F, searchRadius,
         new ColorRGBA(0.0F, 0.0F, 0.0F, 80.0F * alpha)
      );
      if (Interface.showGlass()) {
         context.drawLiquidGlass(
            searchX, searchY, searchWidth, searchHeight, 2.0F, 0.08F, searchRadius,
            ColorRGBA.WHITE.withAlpha(255.0F * alpha * Interface.glass())
         );
         context.drawRoundedRect(
            searchX, searchY, searchWidth, searchHeight, searchRadius,
            Colors.getBackgroundColor().withAlpha(255.0F * (0.8F - 0.3F * Interface.glass()) * alpha)
         );
      }
      boolean searchDark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
         && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
      ColorRGBA searchColor = searchDark ? ColorRGBA.WHITE : new ColorRGBA(10.0F, 10.0F, 10.0F);
      float searchIconSize = 14.0F;
      context.drawTexture(
         BKClientInit.id("sf_symbold/spotlight/search.png"),
         searchX + 10.0F, searchY + (searchHeight - searchIconSize) / 2.0F,
         searchIconSize, searchIconSize, searchColor.mulAlpha(0.75F * alpha)
      );
      this.searchField.set(searchX + 26.0F, searchY + 4.0F, searchWidth - 34.0F, 18.0F);
      this.searchField.setAlpha(alpha);
      this.searchField.setTextColor(searchColor.mulAlpha(0.5F * alpha));
      this.searchField.render(context);
   }

   private List<ClickGuiModuleCard> visibleCards() {
      String query = this.searchField == null ? "" : this.searchField.getBuiltText().trim().toLowerCase(Locale.ROOT);
      if (query.equals(this.cachedQuery) && this.selectedCategory == this.cachedCategory
         && this.cachedFavoritesRevision == dev.bkclient.systems.modules.ModuleFavorites.revision()) {
         return this.visibleCardsCache;
      }

      int favoriteCount = (int)this.cards.stream()
         .filter(card -> dev.bkclient.systems.modules.ModuleFavorites.contains(card.getModule().getName())).count();
      boolean sameView = query.equals(this.cachedQuery) && this.selectedCategory == this.cachedCategory;
      this.reorderOrigins.clear();
      if (sameView && !this.cardPositions.isEmpty()) {
         // Start from the current animated positions, also when clicked again mid-transition.
         this.reorderOrigins.putAll(this.cardPositions);
         this.reorderAnimation.setValue(0.0F);
         if (favoriteCount > this.cachedFavoriteCount) this.scroll.setTargetValue(0.0);
      } else {
         this.cardPositions.clear();
         this.reorderAnimation.setValue(1.0F);
      }
      this.cachedFavoriteCount = favoriteCount;
      this.visibleCardsCache.clear();
      for (ClickGuiModuleCard card : this.cards) {
         boolean matches = query.isEmpty()
            ? card.getModule().getCategory() == this.selectedCategory.getCategory()
            : card.getModule().getName().toLowerCase(Locale.ROOT).contains(query);
         if (matches) {
            this.visibleCardsCache.add(card);
         }
      }
      this.visibleCardsCache.sort(Comparator
         .comparing((ClickGuiModuleCard card) -> !dev.bkclient.systems.modules.ModuleFavorites.contains(card.getModule().getName()))
         .thenComparing(card -> card.getModule().getName(), String.CASE_INSENSITIVE_ORDER));
      this.cachedFavoritesRevision = dev.bkclient.systems.modules.ModuleFavorites.revision();
      this.cachedQuery = query;
      this.cachedCategory = this.selectedCategory;
      return this.visibleCardsCache;
   }

   private boolean isInContentViewport(ClickGuiModuleCard card) {
      return card.getY() + card.getHeight() >= this.contentY
         && card.getY() <= this.contentY + this.contentHeight;
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.cornerSettings.hasScalePicker()) {
         this.cornerSettings.onScalePickerClicked(mouseX, mouseY, button);
         return;
      }
      mouseX = this.logicalMouseX(mouseX);
      mouseY = this.logicalMouseY(mouseY);
      for (ColorPicker picker : new ArrayList<>(this.colorPickers)) {
         boolean picking = picker.isPick();
         picker.onMouseClicked(mouseX, mouseY, button);
         if (picking || picker.isHovered(mouseX, mouseY)) {
            return;
         }
         picker.setShowing(false);
      }

      if (this.clickSettingsPanel(mouseX, mouseY, button)) return;
      if (this.cornerSettings.onMouseClicked(mouseX, mouseY, button)) {
         return;
      }
      if (this.autoMarketView && button == MouseButton.LEFT && this.autoMarketContent.handleClick(mouseX, mouseY)) {
         return;
      }
      if (this.configsView && this.configsContent.handleClick(mouseX, mouseY, button)) {
         return;
      }
      if (this.themeView && this.themeSettings.onClick(mouseX, mouseY, button)) {
         return;
      }

      ClickGuiModuleCard playerPanel = this.activePlayerCardPanel();
      if (playerPanel != null && playerPanel.isPlayerCardPanelHovered(mouseX, mouseY)) {
         playerPanel.onPlayerCardPanelClicked(mouseX, mouseY, button);
         return;
      }

      if (button == MouseButton.LEFT) {
         for (MenuCategory category : MenuCategory.values()) {
            int index = category.ordinal();
            if (GuiUtility.isHovered(this.tabX[index], this.tabY, this.tabWidth[index], this.tabHeight, mouseX, mouseY)) {
               this.themeView = false;
               this.autoMarketView = false;
               this.configsView = false;
               if (category != this.selectedCategory) {
                  this.selectedCategory = category;
                  this.scroll.reset();
                  this.categoryAnimation.reset(0.0F);
               }
               return;
            }
         }
         if (GuiUtility.isHovered(this.autoMarketTabX, this.tabY, this.autoMarketTabWidth, this.tabHeight, mouseX, mouseY)) {
            this.autoMarketView = true;
            this.themeView = false;
            this.configsView = false;
            this.searchField.setFocused(false);
            this.scroll.reset();
            return;
         }
         if (GuiUtility.isHovered(this.configsTabX, this.tabY, this.configsTabWidth, this.tabHeight, mouseX, mouseY)) {
            this.configsView = true;
            this.autoMarketView = false;
            this.themeView = false;
            this.searchField.setFocused(false);
            this.scroll.reset();
            return;
         }
         if (GuiUtility.isHovered(this.themeTabX, this.tabY, this.themeTabWidth, this.tabHeight, mouseX, mouseY)) {
            this.themeView = true;
            this.autoMarketView = false;
            this.configsView = false;
            this.searchField.setFocused(false);
            this.scroll.reset();
            return;
         }
      }

      if (this.themeView || this.autoMarketView || this.configsView) {
         return;
      }

      this.searchField.onMouseClicked(mouseX, mouseY, button);
      List<ClickGuiModuleCard> visibleCards = this.visibleCards();
      if (button == MouseButton.MIDDLE) {
         this.cards.forEach(ClickGuiModuleCard::clearBinding);
      }
      ClickGuiModuleCard dropdownCard = visibleCards.stream()
         .filter(ClickGuiModuleCard::hasOpenDropdown)
         .findFirst()
         .orElse(null);
      if (dropdownCard != null) {
         dropdownCard.onCardMouseClicked(mouseX, mouseY, button);
         return;
      }
      if (!GuiUtility.isHovered(this.windowX, this.contentY, WINDOW_WIDTH, this.contentHeight, mouseX, mouseY)) return;
      for (ClickGuiModuleCard card : visibleCards) {
         if (this.isInContentViewport(card) && card.isHovered(mouseX, mouseY)) {
            card.onCardMouseClicked(mouseX, mouseY, button);
            return;
         }
      }
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      mouseX = this.logicalMouseX(mouseX);
      mouseY = this.logicalMouseY(mouseY);
      for (ColorPicker picker : this.colorPickers) {
         picker.onMouseReleased(mouseX, mouseY, button);
      }
      for (ClickGuiSettingsPanel panel : this.settingsPanels) panel.onMouseReleased(mouseX, mouseY, button);
      if (this.autoMarketView) {
         this.autoMarketContent.handleRelease();
         return;
      }
      if (this.configsView) {
         this.configsContent.handleRelease();
         return;
      }
      if (this.themeView) {
         this.themeSettings.onRelease(button);
         return;
      }
      ClickGuiModuleCard playerPanel = this.activePlayerCardPanel();
      if (playerPanel != null) {
         playerPanel.onPlayerCardPanelReleased(mouseX, mouseY, button);
      }
      this.searchField.onMouseReleased(mouseX, mouseY, button);
      for (ClickGuiModuleCard card : this.visibleCards()) {
         card.onMouseReleased(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.cornerSettings.hasScalePicker()) {
         return this.cornerSettings.onScalePickerScroll(verticalAmount);
      }
      mouseX = this.logicalMouseX(mouseX);
      mouseY = this.logicalMouseY(mouseY);
      for (int i = this.settingsPanels.size() - 1; i >= 0; i--) {
         ClickGuiSettingsPanel panel = this.settingsPanels.get(i);
         if (panel.isShowing() && (panel.isHovered(mouseX, mouseY) || panel.hasOpenDropdown())) {
            panel.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
            return true;
         }
      }
      if (this.cornerSettings.onScroll(mouseX, mouseY, verticalAmount)) return true;
      if (this.autoMarketView) {
         return this.autoMarketContent.onScroll(mouseX, mouseY, verticalAmount);
      }
      if (this.configsView) {
         return this.configsContent.onScroll(mouseX, mouseY, verticalAmount);
      }
      if (this.themeView) {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
      for (ClickGuiModuleCard card : this.visibleCards()) {
         if (card.hasOpenDropdown()) {
            card.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
            return true;
         }
      }
      if (GuiUtility.isHovered(this.windowX, this.contentY, WINDOW_WIDTH, this.contentHeight, mouseX, mouseY)) {
         for (ClickGuiModuleCard card : this.visibleCards()) {
            if (this.isInContentViewport(card)) card.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
         }
      }
      if (GuiUtility.isHovered(this.windowX, this.contentY, WINDOW_WIDTH, this.contentHeight, mouseX, mouseY)) {
         this.scroll.scroll(verticalAmount);
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   private double logicalMouseX(double mouseX) {
      float pivot = this.windowX + WINDOW_WIDTH / 2.0F;
      return pivot + (mouseX - pivot) / Math.max(0.01F, this.currentRenderScale);
   }

   private double logicalMouseY(double mouseY) {
      float pivot = this.windowY + WINDOW_HEIGHT / 2.0F;
      return pivot + (mouseY - pivot) / Math.max(0.01F, this.currentRenderScale);
   }

   private boolean applyMenuTransform(MatrixStack matrices, float pivotX, float pivotY, float progress) {
      float targetX = pivotX;
      float targetY = pivotY;
      float rotation = Interface.popTiltMotion() ? -1.7F * (1.0F - progress) : 0.0F;
      float scale;

      if (this.closing) {
         scale = Math.max(0.001F, easeOutBack(progress));
         if (this.closeAnchor != null && this.closeRight != null) {
            Vec2f center = Utils.worldToScreen(this.closeAnchor);
            Vec2f right = Utils.worldToScreen(this.closeAnchor.add(this.closeRight));
            if (center == null || right == null) {
               return false;
            }

            targetX = center.x;
            targetY = center.y;
            float rightX = right.x - center.x;
            float rightY = right.y - center.y;
            float rightPixels = (float)Math.hypot(rightX, rightY);
            if (rightPixels > 0.001F) {
               float currentAngle = (float)Math.atan2(rightY, rightX);
               if (this.closeInitialRightPixels <= 0.001F) {
                  this.closeInitialRightPixels = rightPixels;
                  this.closeInitialRightAngle = currentAngle;
               }
               scale *= rightPixels / this.closeInitialRightPixels;
               rotation = (float)Math.toDegrees(normalizeRadians(currentAngle - this.closeInitialRightAngle));
            }
         }
      } else {
         scale = Interface.popTiltMotion()
            ? 0.90F + 0.10F * progress
            : 0.96F + 0.04F * progress;
      }

      scale *= this.fittedMenuScale();
      this.currentRenderScale = scale;
      matrices.translate(targetX, targetY, 0.0F);
      if (Math.abs(rotation) > 0.001F) {
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
      }
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-pivotX, -pivotY, 0.0F);
      return true;
   }

   private void captureCloseAnchor() {
      this.closeAnchor = null;
      this.closeRight = null;
      if (mc.world == null || mc.player == null || mc.gameRenderer == null) {
         return;
      }

      Camera camera = mc.gameRenderer.getCamera();
      double yaw = Math.toRadians(camera.getYaw());
      double pitch = Math.toRadians(camera.getPitch());
      double pitchCos = Math.cos(pitch);
      Vec3d look = new Vec3d(
         -Math.sin(yaw) * pitchCos,
         -Math.sin(pitch),
         Math.cos(yaw) * pitchCos
      );
      Vec3d rightVector = new Vec3d(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
      double distance = CLOSE_ANCHOR_DISTANCE * mc.getWindow().getHeight() / 1080.0D;
      Vec3d anchor = camera.getPos().add(look.multiply(distance));

      // The world position is the source of truth. Do not discard it when the projection
      // matrices happen to be unavailable during the exact input frame that closes the GUI.
      this.closeAnchor = anchor;
      this.closeRight = rightVector;
      this.closeInitialRightPixels = 0.0F;
      this.closeInitialRightAngle = 0.0F;

      Vec2f center = Utils.worldToScreen(anchor);
      Vec2f right = Utils.worldToScreen(anchor.add(rightVector));
      if (center == null || right == null) {
         return;
      }

      float rightX = right.x - center.x;
      float rightY = right.y - center.y;
      float rightPixels = (float)Math.hypot(rightX, rightY);
      if (rightPixels <= 0.001F) {
         return;
      }

      this.closeInitialRightPixels = rightPixels;
      this.closeInitialRightAngle = (float)Math.atan2(rightY, rightX);
   }

   private static float easeOutBack(float value) {
      float overshoot = 1.70158F;
      float shifted = value - 1.0F;
      return 1.0F + (overshoot + 1.0F) * shifted * shifted * shifted + overshoot * shifted * shifted;
   }

   private static float normalizeRadians(float value) {
      while (value > Math.PI) {
         value -= (float)(Math.PI * 2.0D);
      }
      while (value < -Math.PI) {
         value += (float)(Math.PI * 2.0D);
      }
      return value;
   }

   private ClickGuiModuleCard activePlayerCardPanel() {
      if (this.themeView || this.autoMarketView || this.configsView) {
         return null;
      }
      return this.visibleCards().stream()
         .filter(ClickGuiModuleCard::hasOpenPlayerCardPanel)
         .findFirst()
         .orElse(null);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.closing) {
         return true;
      }
      if (this.cornerSettings.hasScalePicker()) {
         return this.cornerSettings.onScalePickerKeyPressed(keyCode);
      }
      for (ColorPicker picker : this.colorPickers) {
         picker.onKeyPressed(keyCode, scanCode, modifiers);
      }
      if (this.focusedSettingsPanel != null && this.focusedSettingsPanel.isShowing() && !this.searchField.isFocused()
         && !(Screen.hasControlDown() && keyCode == 70)) {
         boolean editing = TextField.hasFocusedField();
         this.focusedSettingsPanel.onKeyPressed(keyCode, scanCode, modifiers);
         if (keyCode == 256 && !editing) this.focusedSettingsPanel.setShowing(false);
         return true;
      }
      if (this.autoMarketView && this.autoMarketContent.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      }
      if (this.configsView && this.configsContent.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      }
      if (this.themeView) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
      ClickGuiModuleCard bindingCard = this.cards.stream().filter(ClickGuiModuleCard::isBinding).findFirst().orElse(null);
      if (bindingCard != null) {
         bindingCard.onKeyPressed(keyCode, scanCode, modifiers);
         return true;
      }
      if (!this.searchField.isFocused() && Screen.hasControlDown() && keyCode == 70) {
         this.searchField.setFocused(true);
         return true;
      }
      if (this.searchField.isFocused()) {
         this.searchField.onKeyPressed(keyCode, scanCode, modifiers);
         this.scroll.reset();
         return true;
      }
      for (ClickGuiModuleCard card : this.visibleCards()) {
         card.onKeyPressed(keyCode, scanCode, modifiers);
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.focusedSettingsPanel != null && this.focusedSettingsPanel.isShowing() && !this.searchField.isFocused()) {
         return this.focusedSettingsPanel.charTyped(chr, modifiers);
      }
      if (this.autoMarketView && this.autoMarketContent.charTyped(chr, modifiers)) {
         return true;
      }
      if (this.configsView && this.configsContent.charTyped(chr, modifiers)) {
         return true;
      }
      if (this.themeView) {
         return super.charTyped(chr, modifiers);
      }
      if (this.searchField.isFocused()) {
         boolean typed = this.searchField.charTyped(chr, modifiers);
         this.scroll.reset();
         return typed;
      }
      for (ClickGuiModuleCard card : this.visibleCards()) {
         if (card.charTyped(chr, modifiers)) {
            return true;
         }
      }
      return super.charTyped(chr, modifiers);
   }

   @Override
   public void close() {
      if (this.closing) {
         return;
      }
      this.captureCloseAnchor();
      this.closing = true;
      this.menuAnimation.setDuration(CLOSE_ANIMATION_MS);
      this.menuAnimation.setEasing(Easing.LINEAR);
      BKClientInit.getInstance().getModuleManager().getModule(MenuModule.class).disable();
      BKClientInit.getInstance().getFileManager().writeFile("client");
      if (BKClientInit.getInstance().getConfigManager().getCurrent() != null) {
         BKClientInit.getInstance().getConfigManager().getCurrent().save();
      }
      if (TextField.LAST_FIELD != null) {
         TextField.LAST_FIELD.setFocused(false);
      }
      super.close();
   }

   @Override
   public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {
      if (this.autoMarketView && button == MouseButton.LEFT) {
         this.autoMarketContent.handleDrag(this.logicalMouseX(mouseX), this.logicalMouseY(mouseY));
      }
   }

   public void showAutoMarket() {
      this.autoMarketView = true;
      this.themeView = false;
      this.configsView = false;
      if (this.searchField != null) this.searchField.setFocused(false);
      this.scroll.reset();
   }

   public void showConfigs() {
      this.configsView = true;
      this.autoMarketView = false;
      this.themeView = false;
      if (this.searchField != null) this.searchField.setFocused(false);
      this.scroll.reset();
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public List<ColorPicker> getColorPickers() {
      return this.colorPickers;
   }
}
