package dev.bkclient.ui.menu.clickgui;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.ui.components.ColorPicker;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.MenuScreen;
import dev.bkclient.ui.menu.api.MenuCategory;
import dev.bkclient.ui.menu.clickgui.components.ClickGuiModuleCard;
import dev.bkclient.ui.menu.clickgui.components.ReferenceModuleCard;
import dev.bkclient.utility.render.batching.impl.RoundedRectBatching;
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

/** Independent light ClickGUI reconstructed from the supplied reference image. */
public class ClickGuiV2Screen extends MenuScreen implements IMinecraft {
   private static final float WINDOW_WIDTH = 500.0F;
   private float windowHeight() { return this.liquidStyle() ? 335 : 260; }
   private static final float WINDOW_RADIUS = 16.0F;
   private static final float TOP_BAR_HEIGHT = 63.0F;
   private static final float BOTTOM_BAR_HEIGHT = 12.0F;
   private static final float CONTENT_PADDING = 10.0F;
   private static final float COLUMN_GAP = 8.0F;
   private static final float CARD_GAP = 7.0F;
   private static final float PLAYER_PANEL_WIDTH = 125.0F;
   private static final float PLAYER_PANEL_HEIGHT = 172.0F;
   private static final float PLAYER_PANEL_OVERLAP = 5.0F;
   private static final int COLUMNS = 2;
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
   private final dev.bkclient.ui.menu.clickgui.components.V2AppearanceView themeSettings = new dev.bkclient.ui.menu.clickgui.components.V2AppearanceView(this);
   private final ThemeSettingsView liquidThemeSettings = new ThemeSettingsView();
   protected boolean liquidStyle() { return false; }

   private final AutoMarketContent autoMarketContent = new AutoMarketContent();
   private final ConfigsContent configsContent = new ConfigsContent();
   private final boolean openAutoMarketInitially;
   protected TextField searchField;
   private final dev.bkclient.ui.menu.clickgui.components.GuiSearchOverlay globalSearch = new dev.bkclient.ui.menu.clickgui.components.GuiSearchOverlay();
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

   public ClickGuiV2Screen() {
      this(false);
   }

   public ClickGuiV2Screen(boolean openAutoMarketInitially) {
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
         .map(module -> this.liquidStyle()
            ? new dev.bkclient.ui.menu.clickgui.components.ReferenceModuleCard(module, this::openSettingsPanel)
            : new dev.bkclient.ui.menu.clickgui.components.FinderModuleRow(module, this::openSettingsPanel))
         .forEach(this.cards::add);
      this.searchField = new TextField(Fonts.MEDIUM.getFont(9.0F));
      this.globalSearch.invalidate();
      this.searchField.setPreview("Search...  Ctrl+F");
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
      GuiProfiler.begin();
      try {
      if (GuiProfiler.hides(GuiProfiler.Mode.NO_GUI)) return;
      if (!this.liquidStyle()) this.themeSettings.updatePicker();
      MenuModule menu = BKClientInit.getInstance().getModuleManager().getModule(MenuModule.class);
      try (ClickGuiV2Theme ignored = this.liquidStyle() ? ClickGuiV2Theme.openLiquid()
            : ClickGuiV2Theme.open(menu.isV2Dark(), menu.getV2Accent().getColor())) {
         this.renderThemed(context);
      }
      } finally { GuiProfiler.end(context); }
   }

   private void renderThemed(UIContext context) {
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
      this.windowY = (this.height - windowHeight()) / 2.0F;

      if (this.sideSpace > 0.01F) this.windowX -= this.sideSpace * this.fittedMenuScale() / 2;
      this.contentY = this.windowY + (this.liquidStyle() ? TOP_BAR_HEIGHT : 31);
      this.contentHeight = windowHeight() - (this.liquidStyle() ? TOP_BAR_HEIGHT : 31) - BOTTOM_BAR_HEIGHT;

      float pivotX = this.windowX + WINDOW_WIDTH / 2.0F;
      float pivotY = this.windowY + windowHeight() / 2.0F;
      int rawMouseX = context.getMouseX();
      int rawMouseY = context.getMouseY();
      context.setMouse((int)this.logicalMouseX(rawMouseX), (int)this.logicalMouseY(rawMouseY));
      context.getMatrices().push();
      if (!this.applyMenuTransform(context.getMatrices(), pivotX, pivotY, progress)) {
         context.getMatrices().pop();
         context.setMouse(rawMouseX, rawMouseY);
         return;
      }

      if (!GuiProfiler.hides(GuiProfiler.Mode.NO_FRAME)) {
         if (!GuiProfiler.hides(GuiProfiler.Mode.NO_BACKGROUNDS)) this.drawWindow(context, alpha);
         this.drawTopBar(context, alpha);
      }
      List<ClickGuiModuleCard> visibleCards = this.themeView || this.autoMarketView || this.configsView ? List.of() : this.visibleCards();
      if (this.autoMarketView) {
         ScissorUtility.push(context.getMatrices(), this.windowX + 98, this.contentY, WINDOW_WIDTH - 98, this.contentHeight);
         this.autoMarketContent.render(context, this.windowX + 98, this.contentY, WINDOW_WIDTH - 98, this.contentHeight, alpha);
         ScissorUtility.pop();
      } else if (this.configsView) {
         ScissorUtility.push(context.getMatrices(), this.windowX + 98, this.contentY, WINDOW_WIDTH - 98, this.contentHeight);
         this.configsContent.render(context, this.windowX + 98, this.contentY, WINDOW_WIDTH - 98, this.contentHeight, alpha);
         ScissorUtility.pop();
      } else if (this.themeView) {
         ScissorUtility.push(context.getMatrices(), this.windowX + 98, this.contentY, WINDOW_WIDTH - 98, this.contentHeight);
         if (this.liquidStyle()) {
            this.liquidThemeSettings.render(context, this.windowX + 98 + CONTENT_PADDING, this.contentY + CONTENT_PADDING,
               WINDOW_WIDTH - 98 - CONTENT_PADDING * 2, this.contentHeight - CONTENT_PADDING * 2, alpha);
         } else this.themeSettings.render(
            context,
            this.windowX + 98 + CONTENT_PADDING,
            this.contentY + CONTENT_PADDING,
            WINDOW_WIDTH - 98 - CONTENT_PADDING * 2.0F,
            this.contentHeight - CONTENT_PADDING * 2.0F,
            alpha
         );
         ScissorUtility.pop();
      } else {
         if (!GuiProfiler.hides(GuiProfiler.Mode.NO_CARDS)) this.drawCards(context, alpha, visibleCards);
      }
      if (!GuiProfiler.hides(GuiProfiler.Mode.NO_FRAME)) this.drawBottomBar(context, alpha);

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
         float panelY = this.windowY + (windowHeight() - PLAYER_PANEL_HEIGHT) / 2.0F;
         playerPanel.renderPlayerCardPanel(
            context, panelX, panelY, PLAYER_PANEL_WIDTH, PLAYER_PANEL_HEIGHT, alpha
         );
      }

      this.layoutSettingsPanels();
      for (ClickGuiSettingsPanel panel : this.settingsPanels) {
         panel.render(context);
      }
      this.settingsPanels.removeIf(panel -> !panel.isShowing() && panel.getAnimation().getValue() <= 0.001F);
      for (ColorPicker picker : this.colorPickers) {
         picker.render(context);
      }
      this.colorPickers.removeIf(picker -> picker.getAnimation().getValue() == 0.0F && !picker.isShowing());


      this.globalSearch.render(context, this.searchField, this.cards, this.windowX, this.windowY, WINDOW_WIDTH, windowHeight(), alpha);
      context.getMatrices().pop();
      context.setMouse(rawMouseX, rawMouseY);
      if (!this.closing) {
         this.cornerSettings.renderScalePicker(context, alpha);
      }
   }

   private float fittedMenuScale() {
      
      return Math.min(Interface.menuScale(), Math.min((this.width - 20.0F) / (WINDOW_WIDTH + this.sideSpace),
         (this.height - 20.0F) / windowHeight()));
   }

   private void openSearchResult(dev.bkclient.ui.menu.clickgui.components.GuiSearchOverlay.Result result) {
      this.searchField.setFocused(false);
      this.searchField.clear();
      this.globalSearch.invalidate();
      this.themeView = this.autoMarketView = this.configsView = false;
      for (MenuCategory category : MenuCategory.values()) {
         if (category.getCategory() == result.card().getModule().getCategory()) this.selectedCategory = category;
      }
      this.scroll.reset();
      var visible = this.visibleCards();
      int index = visible.indexOf(result.card());
      float rowHeight = result.card().getHeight() + CARD_GAP;
      this.scroll.setMax(Math.min(0, this.contentHeight - CONTENT_PADDING * 2 - (float)Math.ceil(visible.size() / (double)COLUMNS) * rowHeight));
      this.scroll.setTargetValue(-Math.max(0, index / COLUMNS * rowHeight));
      if (result.card().getModule().getDisplaySettings().stream().noneMatch(dev.bkclient.systems.setting.Setting::isVisible)) {
         for (ClickGuiSettingsPanel existing : this.settingsPanels) existing.setShowing(false);
         this.focusedSettingsPanel = null;
         return;
      }
      ClickGuiSettingsPanel panel = this.settingsPanels.stream()
         .filter(p -> p.isShowing() && p.getCard() == result.card()).findFirst().orElse(null);
      if (panel == null) {
         this.openSettingsPanel(result.card());
         panel = this.settingsPanels.stream().filter(p -> p.isShowing() && p.getCard() == result.card()).findFirst().orElse(null);
      }
      if (panel != null) {
         this.focusedSettingsPanel = panel;
         if (result.setting() != null) panel.focusSearchSetting(result.setting());
      }
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
         nextY += Math.min(windowHeight(), panel.contentHeight()) + 10.0F;
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

   private float navY(int index) { return this.liquidStyle()
      ? this.windowY + 47 + index * 24 + (index >= 5 ? 17 : 0)
      : this.windowY + 33 + index * 18 + (index >= 5 ? 12 : 0); }

   protected void drawWindow(UIContext context, float alpha) {
      if (!this.liquidStyle()) { this.drawFinderWindow(context, alpha); return; }
      if (this.liquidStyle()) {

      BorderRadius radius = BorderRadius.all(WINDOW_RADIUS);
      if (Interface.backgroundBlurEnabled()) {
         context.drawBlurredRect(
            this.windowX,
            this.windowY,
            WINDOW_WIDTH,
            windowHeight(),
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
            windowHeight(),
            2.0F,
            0.08F,
            radius,
            ColorRGBA.WHITE.withAlpha(255.0F * Interface.glass() * alpha)
         );
         context.drawRoundedRect(
            this.windowX, this.windowY, WINDOW_WIDTH, windowHeight(), radius,
            Colors.getBackgroundColor().withAlpha(255.0F * (0.8F - 0.3F * Interface.glass()) * alpha)
         );
      } else {
         if (Interface.backgroundBlurEnabled()) {
            context.drawBlurredRect(
               this.windowX, this.windowY, WINDOW_WIDTH, windowHeight(),
               45.0F, WINDOW_RADIUS, radius,
               ColorRGBA.WHITE.withAlpha(255.0F * alpha * Interface.minimalizm())
            );
         }
         context.drawRoundedRect(
            this.windowX, this.windowY, WINDOW_WIDTH, windowHeight(), radius,
            Colors.getBackgroundColor().withAlpha(200.0F * alpha)
         );
      }
            return;
      }
      dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(context.getMatrices(), this.windowX, this.windowY, WINDOW_WIDTH, windowHeight(),
         BorderRadius.all(15), ClickGuiV2Theme.current().window.mulAlpha(alpha));
   }

   private void drawFinderWindow(UIContext c, float alpha) {
      boolean dark = ClickGuiV2Theme.current().dark;
      ColorRGBA border = new ColorRGBA(dark ? 54 : 180, dark ? 54 : 180, dark ? 54 : 180);
      ColorRGBA body = new ColorRGBA(dark ? 26 : 241, dark ? 26 : 240, dark ? 28 : 243);
      ColorRGBA sidebar = new ColorRGBA(dark ? 33 : 247, dark ? 33 : 247, dark ? 35 : 249);
      c.drawShadow(windowX, windowY, WINDOW_WIDTH, windowHeight(), 12, BorderRadius.all(10), ColorRGBA.BLACK.mulAlpha(.22F * alpha));
      c.drawRoundedRect(windowX, windowY, WINDOW_WIDTH, windowHeight(), BorderRadius.all(12), border.mulAlpha(.35F*alpha));
      c.drawRoundedRect(windowX+1, windowY+1, WINDOW_WIDTH-2, windowHeight()-2, BorderRadius.all(11), body.mulAlpha(alpha));
      c.drawRoundedRect(windowX+3, windowY+3, 94, windowHeight()-6, BorderRadius.all(10), sidebar.mulAlpha(alpha));
   }

   private void drawFinderTopBar(UIContext c, float alpha) {
      var palette = ClickGuiV2Theme.current();
      ColorRGBA ink = palette.text.mulAlpha(alpha), muted = palette.muted.mulAlpha(.75F*alpha);
      for (int i=0;i<3;i++) {
         ColorRGBA color = i==0 ? new ColorRGBA(255,95,87) : i==1 ? new ColorRGBA(255,189,46) : new ColorRGBA(40,200,64);
         c.drawRoundedRect(windowX+10+i*10, windowY+10, 6, 6, BorderRadius.all(3), color.mulAlpha(alpha));
      }
      c.drawText(Fonts.MEDIUM.getFont(6), "BKClient", windowX+47, windowY+10, ink);
      c.drawText(Fonts.MEDIUM.getFont(5.5F), "Categories", windowX+10, windowY+24, muted);
      tabHeight=17;
      for (MenuCategory category:MenuCategory.values()) {
         int index=category.ordinal(); tabX[index]=windowX+7; tabWidth[index]=84;
         drawFinderNavigation(c,category.getName(),index,!themeView&&!configsView&&!autoMarketView&&selectedCategory==category,alpha);
      }
      c.drawText(Fonts.MEDIUM.getFont(5.5F), "Client", windowX+10, windowY+124, muted);
      autoMarketTabX=configsTabX=themeTabX=windowX+7;
      autoMarketTabWidth=configsTabWidth=themeTabWidth=84;
      drawFinderNavigation(c,"AutoMarket",5,autoMarketView,alpha);
      drawFinderNavigation(c,"Configs",6,configsView,alpha);
      drawFinderNavigation(c,"Color",7,themeView,alpha);
      String title=themeView?"Color":configsView?"Configs":autoMarketView?"AutoMarket":selectedCategory.getName();
      LucideIcons icon=themeView?LucideIcons.PALETTE:configsView?LucideIcons.COPY:autoMarketView?LucideIcons.CART:LucideIcons.category(selectedCategory);
      icon.draw(c,windowX+108,windowY+10,8,muted);
      c.drawText(Fonts.MEDIUM.getFont(7),title,windowX+121,windowY+11,ink);
      c.drawText(Fonts.REGULAR.getFont(5.5F),"BKClient  /  1.21.4",windowX+10,windowY+windowHeight()-15,muted);
   }

   private void drawFinderNavigation(UIContext c,String label,int index,boolean selected,float alpha) {
      float y=navY(index); var palette=ClickGuiV2Theme.current();
      if(selected)c.drawRoundedRect(windowX+7,y,84,17,BorderRadius.all(4),palette.text.mulAlpha(.09F*alpha));
      ColorRGBA ink=(selected?palette.accent:palette.text).mulAlpha(alpha);
      LucideIcons icon=index<5?LucideIcons.category(MenuCategory.values()[index]):index==5?LucideIcons.CART:index==6?LucideIcons.COPY:LucideIcons.PALETTE;
      icon.draw(c,windowX+11,y+4,8,ink);
      c.drawText(Fonts.MEDIUM.getFont(6),label,windowX+24,y+5,ink);
   }

   private void drawFinderRows(UIContext c,float alpha,List<ClickGuiModuleCard> visible) {
      final float gap = 5, tileHeight = dev.bkclient.ui.menu.clickgui.components.FinderModuleRow.HEIGHT;
      final float tileWidth = (WINDOW_WIDTH-114-gap)/2;
      int rowCount = (visible.size()+1)/2;
      categoryAnimation.update(1);
      scroll.setMax(Math.min(0, contentHeight-4-rowCount*(tileHeight+gap)+gap));
      scroll.update();
      ScissorUtility.push(c.getMatrices(),windowX+103,contentY,WINDOW_WIDTH-110,contentHeight);
      try {
         for(int i=0;i<visible.size();i++) {
            var card=visible.get(i);
            float cx=106+(i%2)*(tileWidth+gap), cy=2+(i/2)*(tileHeight+gap);
            card.set(windowX+cx,contentY+cy-(float)scroll.getValue(),tileWidth,tileHeight);
            cardPositions.put(card,new Vec2f(cx,cy));
            if(isInContentViewport(card))card.render(c);
         }
      } finally { ScissorUtility.pop(); }
   }

   private void drawTopBar(UIContext context, float alpha) {
      if (!this.liquidStyle()) { this.drawFinderTopBar(context, alpha); return; }
      ColorRGBA text = Colors.getTextColor().mulAlpha(alpha);
      ColorRGBA muted = ClickGuiV2Theme.current().muted.mulAlpha(alpha);
      for (int i = 0; i < 3; i++) {
         ColorRGBA color = i == 0 ? new ColorRGBA(244, 101, 98) : i == 1 ? new ColorRGBA(243, 196, 75) : new ColorRGBA(85, 189, 118);
         context.drawRoundedRect(this.windowX + 12 + i * 13, this.windowY + 11, 8, 8, BorderRadius.all(4), color.mulAlpha(alpha));
      }
      context.drawText(Fonts.MEDIUM.getFont(6), "General", this.windowX + 12, this.windowY + 33, muted);
      this.tabHeight = 21;
      for (MenuCategory category : MenuCategory.values()) {
         int index = category.ordinal();
         this.tabX[index] = this.windowX + 9;
         this.tabWidth[index] = 78;
         drawNavigation(context, category.getName(), index,
            !this.themeView && !this.autoMarketView && !this.configsView && this.selectedCategory == category, alpha);
      }
      context.drawText(Fonts.MEDIUM.getFont(6), "Client", this.windowX + 12, this.windowY + 174, muted);
      this.autoMarketTabX = this.configsTabX = this.themeTabX = this.windowX + 9;
      this.autoMarketTabWidth = this.configsTabWidth = this.themeTabWidth = 78;
      drawNavigation(context, "AutoMarket", 5, this.autoMarketView, alpha);
      drawNavigation(context, "Configs", 6, this.configsView, alpha);
      drawNavigation(context, "Color", 7, this.themeView, alpha);
      dev.bkclient.utility.render.DrawUtility.drawTextureMinified(context.getMatrices(), BKClientInit.id("textures/gui/bk-logo.png"), this.windowX + 12, this.windowY + windowHeight() - 29, 17, 17, Colors.getTextColor().mulAlpha(alpha));
      context.drawText(Fonts.MEDIUM.getFont(7), "BKClient", this.windowX + 34, this.windowY + windowHeight() - 26, text);
      context.drawText(Fonts.REGULAR.getFont(5), "1.21.4", this.windowX + 34, this.windowY + windowHeight() - 16, muted);
      String title = this.themeView ? "Color" : this.configsView ? "Configs" : this.autoMarketView ? "AutoMarket" : this.selectedCategory.getName();
      context.drawText(Fonts.MEDIUM.getFont(8), title, this.windowX + 110, this.windowY + 14, text);
      if (!this.themeView && !this.configsView && !this.autoMarketView) {
         LucideIcons.category(this.selectedCategory).draw(context, this.windowX + 110, this.windowY + 41, 11, text);
      } else {
         LucideIcons icon = this.autoMarketView ? LucideIcons.CART : this.configsView ? LucideIcons.COPY : LucideIcons.PALETTE;
         icon.draw(context, this.windowX + 110, this.windowY + 41, 11, text);
      }
      context.drawText(Fonts.MEDIUM.getFont(12), this.themeView || this.configsView || this.autoMarketView ? title : "Modules",
         this.windowX + 127, this.windowY + 42, text);
   }

   private void drawNavigation(UIContext context, String label, int index, boolean selected, float alpha) {
      float y = navY(index);
      if (selected) context.drawRoundedRect(this.windowX + 8, y, 80, 21, BorderRadius.all(5), (this.liquidStyle() ? Colors.getAccent().withAlpha(45) : ClickGuiV2Theme.current().selection).mulAlpha(alpha));
      ColorRGBA color = Colors.getTextColor().mulAlpha(alpha);
      if (index < MenuCategory.values().length) {
         LucideIcons.category(MenuCategory.values()[index]).draw(context, this.windowX + 12, y + 5, 11, color);
      } else {
         LucideIcons icon = index == 5 ? LucideIcons.CART : index == 6 ? LucideIcons.COPY : LucideIcons.PALETTE;
         icon.draw(context, this.windowX + 12, y + 5, 11, color);
      }
      context.drawText(Fonts.MEDIUM.getFont(7), label, this.windowX + 26, y + 7, color);
   }

   private void drawCards(UIContext context, float alpha, List<ClickGuiModuleCard> visible) {
      if (!this.liquidStyle()) { this.drawFinderRows(context, alpha, visible); return; }
      this.categoryAnimation.update(1.0F);
      float columnWidth = (WINDOW_WIDTH - 98 - CONTENT_PADDING * 2.0F - COLUMN_GAP * (COLUMNS - 1)) / COLUMNS;
      java.util.Arrays.fill(this.columnHeights, 0.0F);
      float scrollOffset = (float)this.scroll.getValue();
      float reorder = this.reorderAnimation.update(1.0F);

      ScissorUtility.push(context.getMatrices(), this.windowX + 98, this.contentY, WINDOW_WIDTH - 98, this.contentHeight);
      for (int i = 0; i < visible.size(); ++i) {
         ClickGuiModuleCard card = visible.get(i);
         int column = i % COLUMNS;
         float localX = 98 + CONTENT_PADDING + column * (columnWidth + COLUMN_GAP);
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
         this.columnHeights[column] += card.getHeight() + CARD_GAP;
      }
      try {
         // At rest these equal-sized cards do not overlap. During reorder, preserve painter order.
         boolean grouped = reorder >= 1.0F
            && visible.stream().allMatch(ReferenceModuleCard.class::isInstance);
         if (grouped) {
            if (!GuiProfiler.hides(GuiProfiler.Mode.NO_BACKGROUNDS)) {
            if (this.liquidStyle()) {
               // Glass samples the shared blur and keeps its original per-card composition.
               for (ClickGuiModuleCard card : visible) if (this.isInContentViewport(card)) {
                  ((ReferenceModuleCard)card).renderCardBackground(context);
               }
            } else {
            RoundedRectBatching backgrounds = new RoundedRectBatching();
            try {
               for (ClickGuiModuleCard card : visible) if (this.isInContentViewport(card)) {
                  ((ReferenceModuleCard)card).renderCardBackground(context);
               }
            } finally { backgrounds.draw(); }
            }
            }
            for (ClickGuiModuleCard card : visible) if (this.isInContentViewport(card)) {
               ((ReferenceModuleCard)card).renderGrouped(context);
            }
            if (!GuiProfiler.hides(GuiProfiler.Mode.NO_TEXT)) {
               // Same-column labels share their horizontal fade. Key-bound names keep individual widths.
               for (int column = 0; column < COLUMNS && column < visible.size(); column++) {
                  float textX = visible.get(column).getX() + 22;
                  for (int layer = 0; layer < 2; layer++) {
                     boolean description = layer == 1;
                     var font = description ? Fonts.REGULAR.getFont(6) : Fonts.MEDIUM.getFont(7);
                     float fadeWidth = description ? Math.max(0, columnWidth - 71) : Math.max(8, columnWidth - 75);
                     var text = new dev.bkclient.utility.render.batching.impl.FadedFontBatching(font.getFont(), textX, fadeWidth);
                     try {
                        for (int i = column; i < visible.size(); i += COLUMNS) {
                           var card = visible.get(i);
                           if (this.isInContentViewport(card)) ((ReferenceModuleCard)card).renderCardText(context, description);
                        }
                     } finally { text.draw(); }
                  }
               }
            }
            if (!GuiProfiler.hides(GuiProfiler.Mode.NO_TOGGLES)) {
               // Each pass uses one shape size/radius; only color and position vary.
               for (int layer = 0; layer < 2; layer++) {
                  var toggles = new dev.bkclient.utility.render.batching.impl.ScreenRectBatching();
                  try {
                     for (ClickGuiModuleCard card : visible) if (this.isInContentViewport(card)) {
                        ((ReferenceModuleCard)card).renderCardToggle(context, layer == 1);
                     }
                  } finally { toggles.draw(); }
               }
            }
            if (!GuiProfiler.hides(GuiProfiler.Mode.NO_ICONS)) LucideIcons.drawBatch(() -> {
               for (ClickGuiModuleCard card : visible) if (this.isInContentViewport(card)) {
                  ((ReferenceModuleCard)card).renderCardIcons(context);
               }
            });
         } else {
            for (ClickGuiModuleCard card : visible) if (this.isInContentViewport(card)) card.renderForBatch(context);
         }
      } finally { ScissorUtility.pop(); }

      float tallest = 0.0F;
      for (float columnHeight : this.columnHeights) {
         tallest = Math.max(tallest, columnHeight);
      }
      this.scroll.setMax(Math.min(0.0F, this.contentHeight - CONTENT_PADDING * 2.0F - tallest));
      this.scroll.update();
   }

   private void drawBottomBar(UIContext context, float alpha) {
      if (this.searchField.isFocused()) return;
      if (!this.liquidStyle()) {
         var palette = ClickGuiV2Theme.current();
         float sx = windowX + WINDOW_WIDTH - 110, sy = windowY + 8;
         boolean hovered = GuiUtility.isHovered(sx, sy, 100, 18, context);
         ColorRGBA fill = palette.text.mulAlpha((hovered ? .075F : .035F) * alpha);
         context.drawRoundedRect(sx, sy, 100, 18, BorderRadius.all(5), fill);
         ColorRGBA ink = palette.text.mulAlpha(.75F * alpha);
         // Thin outlined magnifier; the old font glyph rendered as a solid blob.
         context.drawRoundedRect(sx+7, sy+5, 6, 6, BorderRadius.all(3), ink);
         ColorRGBA interior = (palette.dark ? new ColorRGBA(26,26,28) : new ColorRGBA(241,240,243))
            .mix(palette.text, hovered ? .075F : .035F).mulAlpha(alpha);
         context.drawRoundedRect(sx+8, sy+6, 4, 4, BorderRadius.all(2), interior);
         context.drawLine(new Vec2f(sx+12,sy+10),new Vec2f(sx+15,sy+13),ink);
         context.drawText(Fonts.REGULAR.getFont(6.5F), "Search", sx+21, sy+6, ink);
         context.drawRoundedRect(sx+68, sy+4, 27, 10, BorderRadius.all(3), palette.text.mulAlpha(.055F*alpha));
         context.drawText(Fonts.REGULAR.getFont(5), "Ctrl F", sx+73, sy+7, palette.muted.mulAlpha(alpha));
         return;
      }
      if (this.liquidStyle()) {
         context.drawClientRect(this.windowX + WINDOW_WIDTH - 110, this.windowY + 9, 100, 18, alpha, 0, 7);
      } else {
      context.drawRoundedRect(this.windowX + WINDOW_WIDTH - 110, this.windowY + 9, 100, 18,
         BorderRadius.all(11), ClickGuiV2Theme.current().surface.mulAlpha(alpha));
      }
      ColorRGBA ink = Colors.getTextColor();
      context.drawIcon(Fonts.ICONS, 'v', this.windowX + WINDOW_WIDTH - 103, this.windowY + 14, 7, ink.mulAlpha(.55F * alpha));
      context.drawText(Fonts.REGULAR.getFont(7), "Search", this.windowX + WINDOW_WIDTH - 91, this.windowY + 14, ink.mulAlpha(.6F * alpha));
      context.drawRightText(Fonts.REGULAR.getFont(5.5F), "Ctrl+F", this.windowX + WINDOW_WIDTH - 17, this.windowY + 15, ink.mulAlpha(.35F * alpha));
   }

   private List<ClickGuiModuleCard> visibleCards() {
      String query = "";
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
      if (this.searchField.isFocused()) {
         if (this.globalSearch.click(mouseX, mouseY, button, this.searchField, this::openSearchResult)) return;
         if (!GuiUtility.isHovered(this.windowX + WINDOW_WIDTH - 110, this.windowY + 9, 100, 18, mouseX, mouseY)) {
            this.searchField.setFocused(false);
            return;
         }
      }
      for (ColorPicker picker : new ArrayList<>(this.colorPickers)) {
         boolean picking = picker.isPick();
         picker.onMouseClicked(mouseX, mouseY, button);
         if (picking || picker.isHovered(mouseX, mouseY)) {
            return;
         }
         picker.setShowing(false);
      }

      if (button == MouseButton.LEFT && GuiUtility.isHovered(this.windowX + 9, this.windowY + 8, 14, 14, mouseX, mouseY)) { this.close(); return; }
      if (button == MouseButton.LEFT && GuiUtility.isHovered(this.windowX + 22, this.windowY + 8, 14, 14, mouseX, mouseY)) {
         for (ClickGuiSettingsPanel panel : this.settingsPanels) panel.setShowing(false);
         return;
      }
      if (button == MouseButton.LEFT && GuiUtility.isHovered(this.windowX + 35, this.windowY + 8, 14, 14, mouseX, mouseY)) {
         if (TextField.LAST_FIELD != null) TextField.LAST_FIELD.setFocused(false);
         this.searchField.setFocused(true);
         this.globalSearch.invalidate();
         return;
      }
      if (GuiUtility.isHovered(this.windowX + WINDOW_WIDTH - 110, this.windowY + 9, 100, 18, mouseX, mouseY)) {
         if (TextField.LAST_FIELD != null && TextField.LAST_FIELD != this.searchField) TextField.LAST_FIELD.setFocused(false);
         this.focusedSettingsPanel = null;
         this.globalSearch.invalidate();
         this.searchField.setFocused(true);
         return;
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
      if (this.themeView && (this.liquidStyle() ? this.liquidThemeSettings.onClick(mouseX, mouseY, button) : this.themeSettings.onClick(mouseX, mouseY, button))) {
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
            if (GuiUtility.isHovered(this.tabX[index], navY(index), this.tabWidth[index], this.tabHeight, mouseX, mouseY)) {
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
         if (GuiUtility.isHovered(this.autoMarketTabX, navY(5), this.autoMarketTabWidth, this.tabHeight, mouseX, mouseY)) {
            if (this.liquidStyle()) {
               dev.bkclient.ui.menu.automarket.AutoMarketScreen.open();
               return;
            }
            this.autoMarketView = true;
            this.themeView = false;
            this.configsView = false;
            this.searchField.setFocused(false);
            this.scroll.reset();
            return;
         }
         if (GuiUtility.isHovered(this.configsTabX, navY(6), this.configsTabWidth, this.tabHeight, mouseX, mouseY)) {
            this.configsView = true;
            this.autoMarketView = false;
            this.themeView = false;
            this.searchField.setFocused(false);
            this.scroll.reset();
            return;
         }
         if (GuiUtility.isHovered(this.themeTabX, navY(7), this.themeTabWidth, this.tabHeight, mouseX, mouseY)) {
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
      if (!GuiUtility.isHovered(this.windowX + 98, this.contentY, WINDOW_WIDTH - 98, this.contentHeight, mouseX, mouseY)) return;
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
         if (this.liquidStyle()) this.liquidThemeSettings.onRelease(button);
         else this.themeSettings.onRelease(button);
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
      if (this.searchField.isFocused()) { this.globalSearch.scroll(verticalAmount); return true; }
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
      if (GuiUtility.isHovered(this.windowX + 98, this.contentY, WINDOW_WIDTH - 98, this.contentHeight, mouseX, mouseY)) {
         for (ClickGuiModuleCard card : this.visibleCards()) {
            if (this.isInContentViewport(card)) card.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
         }
      }
      if (GuiUtility.isHovered(this.windowX + 98, this.contentY, WINDOW_WIDTH - 98, this.contentHeight, mouseX, mouseY)) {
         this.scroll.scroll(verticalAmount);
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   /** Picker positions use the same scaled coordinate space as GUI input and rendering. */
   public void keepColorPickerVisible(ColorPicker picker) {
      float left = (float)this.logicalMouseX(5);
      float top = (float)this.logicalMouseY(5);
      float right = (float)this.logicalMouseX(this.width - 5);
      float bottom = (float)this.logicalMouseY(this.height - 5);
      picker.pos(Math.max(left, Math.min(picker.getX(), right - picker.getWidth())),
         Math.max(top, Math.min(picker.getY(), bottom - picker.getHeight())));
   }

   private double logicalMouseX(double mouseX) {
      float pivot = this.windowX + WINDOW_WIDTH / 2.0F;
      return pivot + (mouseX - pivot) / Math.max(0.01F, this.currentRenderScale);
   }

   private double logicalMouseY(double mouseY) {
      float pivot = this.windowY + windowHeight() / 2.0F;
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
      if (GuiProfiler.key(keyCode)) return true;
      if (this.closing) {
         return true;
      }
      if (this.cornerSettings.hasScalePicker()) {
         return this.cornerSettings.onScalePickerKeyPressed(keyCode);
      }
      for (int i = this.colorPickers.size() - 1; i >= 0; i--) {
         ColorPicker picker = this.colorPickers.get(i);
         if (picker.isShowing()) { picker.onKeyPressed(keyCode, scanCode, modifiers); return true; }
      }
      if (Screen.hasControlDown() && keyCode == 70) {
         if (TextField.LAST_FIELD != null) TextField.LAST_FIELD.setFocused(false);
         this.searchField.setFocused(true);
         this.globalSearch.invalidate();
         return true;
      }
      if (this.searchField.isFocused()) {
         if (!this.globalSearch.key(keyCode, this.searchField, this.cards, this::openSearchResult)) this.searchField.onKeyPressed(keyCode, scanCode, modifiers);
         return true;
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
      for (int i = this.colorPickers.size() - 1; i >= 0; i--) {
         ColorPicker picker = this.colorPickers.get(i);
         if (picker.isShowing()) { picker.charTyped(chr, modifiers); return true; }
      }
      if (this.searchField.isFocused()) { this.searchField.charTyped(chr, modifiers); return true; }
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
   public void removed() {
      GuiProfiler.stop();
      super.removed();
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
      if (this.liquidStyle()) {
         dev.bkclient.ui.menu.automarket.AutoMarketScreen.open();
         return;
      }
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
