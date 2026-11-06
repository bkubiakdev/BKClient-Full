package dev.bkclient.ui.menu.spotlight;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.ui.components.ColorPicker;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.MenuScreen;
import dev.bkclient.ui.menu.api.MenuCategory;
import dev.bkclient.ui.menu.spotlight.components.SpotlightModule;
import dev.bkclient.ui.menu.spotlight.components.SpotlightSettings;
import dev.bkclient.systems.config.ConfigFile;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.time.Timer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.InputUtil;

public class SpotlightScreen extends MenuScreen implements IMinecraft, IScaledResolution {
    private static final float PILL_WIDTH = 320.0F;
    private static final float PILL_HEIGHT = 30.0F;
    private static final float PILL_RADIUS = 15.0F;
    private static final float MODULE_HEIGHT = 24.0F;
    private static final float MAX_RESULTS_HEIGHT = 180.0F;

   private float pillX;
   private float pillY;
   private boolean resultsVisible = false;
   private final ScrollHandler scrollHandler = new ScrollHandler();
   private MenuCategory currentCategory = null;
   private final List<SpotlightModule> allModules = new ArrayList<>();
   private final List<SpotlightSettings> windows = new LinkedList<>();
   private final TextField searchField;
   private boolean prevFocused;
    private final Animation clickAnimation = new Animation(100L, Easing.FIGMA_EASE_IN_OUT);
    private final Animation expandAnimation = new Animation(400L, 0.0F, Easing.BAKEK);
    private final List<ColorPicker> colorPickers = new ArrayList<>();
    private final Animation rightHoverAnim = new Animation(250L, Easing.BAKEK);
    private final Animation circlesRevealAnim = new Animation(300L, Easing.BAKEK);
    private boolean rightHovered = false;
    private static final String[] CIRCLE_LABELS = {"Fn", "Cfg", "Frd", "Sr"};
    private static final Identifier[] CIRCLE_ICONS = {
       BKClientInit.id("sf_symbold/spotlight/func.png"),
       BKClientInit.id("sf_symbold/spotlight/conf.png"),
       BKClientInit.id("sf_symbold/spotlight/fr.png"),
       BKClientInit.id("sf_symbold/spotlight/search.png")
    };
    private boolean circlesActive = false;
    private boolean expanded = false;
    private boolean showConfigs = false;
    private boolean showFriends = false;
    private final Animation resultsHeightFactor = new Animation(200L, 1.0F, Easing.QUARTIC_OUT);
    private final Animation blurFadeAnim = new Animation(600L, Easing.QUARTIC_OUT);
    private final Timer blurFadeTimer = new Timer();
    private boolean blurFadeStarted = false;
   public SpotlightScreen() {
      for (MenuCategory category : MenuCategory.values()) {
         BKClientInit.getInstance()
            .getModuleManager()
            .getModules()
            .stream()
            .sorted(Comparator.comparing(Module::getName))
            .filter(module -> module.getCategory().equals(category.getCategory()))
            .forEach(module -> {
               SpotlightModule sm = new SpotlightModule(module, category, this);
               this.allModules.add(sm);
            });
      }

      this.searchField = new TextField(Fonts.MEDIUM.getFont(10.0F));
      this.searchField.setPreview(Localizator.translate("spotlight.title"));
   }

   public void init() {
      this.closing = false;
      super.init();
   }

   public void tick() {
      this.handleMovementKeys();
      super.tick();
   }

   @Override
   public void render(UIContext context) {
      this.menuAnimation.update(this.closing ? 0.0F : 1.0F);
      this.menuAnimation.setEasing(!this.closing ? Easing.BAKEK : Easing.BAKEK_BACK);
      this.menuAnimation.setDuration(300L);
      this.scrollHandler.update();

      float openProgress = this.menuAnimation.getValue();
      if (openProgress < 0.01F) return;

      float bgAlpha = openProgress;

      float alpha = Math.min(1.0F, openProgress);
      float scale = 0.96F + 0.04F * openProgress;

      if (!this.closing) {
         this.clickAnimation.update(this.clickAnimation.getTargetValue() > 0.01F);
      }

      boolean hasText = this.searchField != null && !this.searchField.getBuiltText().isBlank();
      this.resultsVisible = hasText || this.currentCategory != null || this.expanded || this.showConfigs || this.showFriends;
      boolean showResults = this.resultsVisible;

      long expandDuration = showResults ? 550L : 250L;
      this.expandAnimation.setDuration(expandDuration);
      this.expandAnimation.setEasing(showResults ? Easing.generate(0.2F, 0.9F, 0.35F, 1.08F) : Easing.QUARTIC_OUT);
      this.expandAnimation.update(showResults ? 1.0F : 0.0F);
      float expandProgress = Math.max(0.0F, this.expandAnimation.getValue());

      int visibleCount = (int)this.allModules.stream().filter(m -> !this.searchCheck(m) && !this.categoryCheck(m)).count();
      float maxHeight = MAX_RESULTS_HEIGHT;
      float targetResultsHeight = maxHeight;
      if (this.showConfigs || this.showFriends) {
         int count = this.showConfigs ? this.getFilteredConfigCount() : this.getFilteredFriendCount();
         float contentH = count * (CONFIG_HEIGHT + 2.0F);
         targetResultsHeight = Math.min(Math.max(contentH + 4.0F, 30.0F), maxHeight);
      } else if (visibleCount <= 1) {
         targetResultsHeight = CATEGORY_BAR_HEIGHT + MODULE_HEIGHT + 2.0F;
      } else {
         float contentH = CATEGORY_BAR_HEIGHT + visibleCount * (MODULE_HEIGHT + 2.0F);
         targetResultsHeight = Math.min(contentH, maxHeight);
      }
      this.resultsHeightFactor.update(targetResultsHeight / maxHeight);
      float resultsHeight = maxHeight * this.resultsHeightFactor.getValue() * expandProgress;
      float totalHeight = PILL_HEIGHT + resultsHeight;

      this.pillX = (sr.getScaledWidth() - PILL_WIDTH) / 2.0F;
      this.pillY = sr.getScaledHeight() / 3.0F - PILL_HEIGHT / 2.0F;

      float clickScale = 1.0F - 0.005F * this.clickAnimation.getValue();
      float finalScale = scale * clickScale;

      boolean hasFocus = this.searchField.isFocused();
      boolean isTyping = hasText || hasFocus;

      boolean hoveringPill = GuiUtility.isHovered(this.pillX, this.pillY, PILL_WIDTH, PILL_HEIGHT, context.getMouseX(), context.getMouseY());
      boolean hoveringRight = hoveringPill && context.getMouseX() > this.pillX + PILL_WIDTH * 0.5F;

      if (isTyping || this.expanded || this.showConfigs || this.showFriends) {
         this.circlesActive = false;
      } else if (hoveringPill) {
         this.circlesActive = true;
      }

      boolean shouldSplit = (this.circlesActive || hoveringRight) && !isTyping && !this.expanded && !this.showConfigs && !this.showFriends;
      this.rightHoverAnim.update(shouldSplit ? 1.0F : 0.0F);
      float splitProgress = this.rightHoverAnim.getValue();
      float circlesProgress = this.circlesRevealAnim.getValue();

      if (this.circlesActive && splitProgress > 0.3F) {
         this.circlesRevealAnim.update(1.0F);
      } else if (!this.circlesActive) {
         this.circlesRevealAnim.update(0.0F);
      }
      circlesProgress = this.circlesRevealAnim.getValue();

      float pillDrawW = PILL_WIDTH;
      if (splitProgress > 0.01F) {
         pillDrawW = PILL_WIDTH * (1.0F - 0.45F * splitProgress);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      RenderUtility.scale(
         context.getMatrices(),
         this.pillX + PILL_WIDTH / 2.0F,
         this.pillY + totalHeight / 2.0F,
         finalScale
      );

      float searchFieldW = PILL_WIDTH - 28;
      if (splitProgress > 0.01F) {
         searchFieldW = (PILL_WIDTH - 28) * (1.0F - 0.45F * splitProgress);
      }
      context.drawBlurredRect(
         this.pillX, this.pillY, searchFieldW + 14, totalHeight,
         25.0F, PILL_RADIUS,
         BorderRadius.all(PILL_RADIUS),
         new ColorRGBA(0, 0, 0, (int)(80 * alpha))
      );

      if (Interface.showGlass()) {
         context.drawLiquidGlass(
            this.pillX, this.pillY, pillDrawW, totalHeight,
            2.0F, 0.08F,
            BorderRadius.all(PILL_RADIUS),
            ColorRGBA.WHITE.withAlpha((int)(255 * alpha * Interface.glass()))
         );
         context.drawRoundedRect(
            this.pillX, this.pillY, pillDrawW, totalHeight,
            BorderRadius.all(PILL_RADIUS),
            Colors.getBackgroundColor().withAlpha((int)(255 * (0.8F - 0.3F * Interface.glass()) * alpha))
         );
      } else {
         context.drawBlurredRect(
            this.pillX, this.pillY, pillDrawW, totalHeight,
            45.0F, PILL_RADIUS,
            BorderRadius.all(PILL_RADIUS),
            ColorRGBA.WHITE.withAlpha((int)(255 * alpha * Interface.minimalizm()))
         );
         context.drawRoundedRect(
            this.pillX, this.pillY, pillDrawW, totalHeight,
            BorderRadius.all(PILL_RADIUS),
            Colors.getBackgroundColor().withAlpha((int)(200 * alpha))
         );
      }

      this.renderSearchField(context, alpha, splitProgress);
      if (this.circlesActive) {
         this.renderHoverCircles(context, alpha, splitProgress);
      }

      if (this.circlesActive && circlesProgress > 0.99F && !this.blurFadeStarted) {
         this.blurFadeStarted = true;
         this.blurFadeTimer.reset();
      }
      if (!this.circlesActive) {
         this.blurFadeStarted = false;
         this.blurFadeAnim.reset(0.0F);
      }
      if (this.blurFadeStarted && this.blurFadeTimer.finished(1000L)) {
         this.blurFadeAnim.update(1.0F);
      }
      float blurKeep = 1.0F - this.blurFadeAnim.getValue();

      if (splitProgress > 0.01F && blurKeep > 0.01F) {
         float blurRightX = this.pillX + pillDrawW;
         float blurRightW = PILL_WIDTH - pillDrawW;
         ColorRGBA fadeColor = ColorRGBA.WHITE.withAlpha((int)(255 * blurKeep * splitProgress * alpha));

         DrawUtility.drawLiquidGlass(
            context.getMatrices(),
            blurRightX, this.pillY, blurRightW, PILL_HEIGHT,
            BorderRadius.all(PILL_RADIUS),
            fadeColor, // outer color (controls vertex alpha for overall fade)
            0.0F, // outer alpha uniform (0 = no white tint)
            35.0F * blurKeep, // blurRadius
            ColorRGBA.WHITE, // innerColor
            1.0F, // innerAlpha uniform (1 = fully visible blur effect)
            false, // noise
            0.0F, // noiseStrength
            0.0F, // distortion
            PILL_RADIUS, // squirt
            true // clean (reads current framebuffer)
         );
      }

      if (expandProgress > 0.01F) {
         float configOffset = (this.showConfigs || this.showFriends) ? 4.0F : 0.0F;
         float resY = this.pillY + PILL_HEIGHT + configOffset;
         float resH = Math.max(0.0F, resultsHeight - configOffset);
         ScissorUtility.push(context.getMatrices(), this.pillX, resY, PILL_WIDTH, resH);
         if (this.showConfigs) {
            this.renderConfigList(context, alpha, resH);
         } else if (this.showFriends) {
            this.renderFriendList(context, alpha, resH);
         } else {
            this.renderCategories(context, alpha);
            this.renderModuleList(context, alpha);
         }
         ScissorUtility.pop();
      }

      RenderUtility.end(context.getMatrices());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      for (SpotlightSettings window : this.windows) {
         window.render(context);
      }

      for (ColorPicker colorPicker : this.colorPickers) {
         colorPicker.render(context);
      }

      this.windows.removeIf(w -> w.getAnimation().getValue() == 0.0F && !w.isShowing());
      this.colorPickers.removeIf(c -> c.getAnimation().getValue() == 0.0F && !c.isShowing());
   }

    private int getFilteredConfigCount() {
       List<ConfigFile> configs = BKClientInit.getInstance().getConfigManager().getConfigFiles();
       String searchQuery = this.searchField != null ? this.searchField.getBuiltText().toLowerCase() : "";
       int count = 0;
       for (ConfigFile cf : configs) {
          if (cf.getFileName().equalsIgnoreCase("autosave")) continue;
          if (searchQuery.isEmpty() || matchesQuery(cf.getFileName(), searchQuery)) {
             count++;
          }
       }
       return count;
    }

      private float getTotalContentHeight() {
         if (this.showConfigs) {
            return this.getFilteredConfigCount() * (CONFIG_HEIGHT + 2.0F);
         }
         if (this.showFriends) {
            return this.getFilteredFriendCount() * (CONFIG_HEIGHT + 2.0F);
        }
       float h = CATEGORY_BAR_HEIGHT;
       for (SpotlightModule m : this.allModules) {
          if (!this.searchCheck(m) && !this.categoryCheck(m)) {
             h += MODULE_HEIGHT + 2.0F;
          }
       }
       return h;
   }

   private boolean categoryCheck(SpotlightModule module) {
      return this.currentCategory != null && module.getCategory() != this.currentCategory;
   }

    private void renderSearchField(UIContext context, float alpha, float splitProgress) {

       boolean searchDark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
          && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
       ColorRGBA searchIconColor = searchDark ? ColorRGBA.WHITE : new ColorRGBA(10, 10, 10);
       float w = (PILL_WIDTH - 28.0F) * (1.0F - 0.45F * splitProgress);

       float iconSize = 16.0F;

       RenderSystem.setShaderTexture(0, BKClientInit.id("sf_symbold/spotlight/search.png"));
       org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
       org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);

       context.drawTexture(
          BKClientInit.id("sf_symbold/spotlight/search.png"),
          this.pillX + 14.0F,
          this.pillY + (PILL_HEIGHT - iconSize) / 2.0F,
          iconSize,
          iconSize,
          searchIconColor.withAlpha((int)(255 * 0.75F * alpha))
       );

       this.searchField.set(this.pillX + 14.0F + iconSize + 2.0F, this.pillY + (PILL_HEIGHT - 18.0F) / 2.0F, w - iconSize - 2.0F, 18.0F);
       this.searchField.setTextColor(searchIconColor.mulAlpha(0.5F * alpha));
       this.searchField.render(context);

       boolean focused = this.searchField.isFocused();
       boolean hovered = GuiUtility.isHovered(this.pillX, this.pillY, PILL_WIDTH, PILL_HEIGHT, context.getMouseX(), context.getMouseY());
       if (hovered && !focused) {
          CursorUtility.set(CursorType.TEXT);
       }
       this.prevFocused = focused;
    }

   private void renderHoverCircles(UIContext context, float alpha, float splitProgress) {
      boolean circlesDark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
         && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
      ColorRGBA circlesIconColor = circlesDark ? ColorRGBA.WHITE : new ColorRGBA(10, 10, 10);
      float pillDrawW = PILL_WIDTH * (1.0F - 0.45F * splitProgress);
      float cAreaX = this.pillX + pillDrawW;
      float cAreaW = PILL_WIDTH - pillDrawW;
      float circleSize = PILL_HEIGHT;
      float spacing = 4.0F;
      float totalW = 4.0F * circleSize + 3.0F * spacing;
      float startX = cAreaX + (cAreaW - totalW) / 2.0F;
      float prog = this.circlesRevealAnim.getValue();

      for (int i = 0; i < 4; i++) {
         float cx = startX + i * (circleSize + spacing);
         float cy = this.pillY;
         float delay = i * 0.07F;
         float local = Math.max(0.0F, Math.min(1.0F, (prog - delay) / (1.0F - delay)));
         float sz = circleSize * local;
         float dx = cx + (circleSize - sz) / 2.0F;
         float dy = cy + (circleSize - sz) / 2.0F;

          if (Interface.showGlass()) {
             context.drawLiquidGlass(
                dx, dy, sz, sz,
                2.0F, 0.08F,
                BorderRadius.all(sz / 2.0F),
                ColorRGBA.WHITE.withAlpha((int)(255 * local * alpha * Interface.glass()))
             );
             context.drawRoundedRect(
                dx, dy, sz, sz,
                BorderRadius.all(sz / 2.0F),
                 Colors.getBackgroundColor().withAlpha((int)(255 * local * (0.8F - 0.3F * Interface.glass()) * alpha))
              );
           } else {
             context.drawBlurredRect(
                dx, dy, sz, sz,
                45.0F, 2.0F,
                BorderRadius.all(sz / 2.0F),
                ColorRGBA.WHITE.withAlpha((int)(255 * local * alpha * Interface.minimalizm()))
             );
             context.drawRoundedRect(
                dx, dy, sz, sz,
                BorderRadius.all(sz / 2.0F),
                Colors.getBackgroundColor().withAlpha((int)(200 * local * alpha))
             );
          }

         float iconSize = sz * 0.6F;

         RenderSystem.setShaderTexture(0, CIRCLE_ICONS[i]);
         org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
         org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);

          context.drawTexture(
             CIRCLE_ICONS[i],
             dx + (sz - iconSize) / 2.0F,
             dy + (sz - iconSize) / 2.0F,
             iconSize,
             iconSize,
              circlesIconColor.withAlpha((int)(255 * 0.45F * local * alpha))
          );

         if (GuiUtility.isHovered(cx, cy, circleSize, circleSize, context.getMouseX(), context.getMouseY())) {
            CursorUtility.set(CursorType.HAND);
         }
      }
   }

   private void renderCategories(UIContext context, float alpha) {
      boolean categoriesDark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
         && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
      ColorRGBA catTextColor = categoriesDark ? ColorRGBA.WHITE : new ColorRGBA(10, 10, 10);
      float cx = this.pillX + 10.0F;
      float cy = this.pillY + PILL_HEIGHT + 4.0F;
      float ch = 18.0F;

      for (MenuCategory cat : MenuCategory.values()) {
         boolean selected = cat == this.currentCategory;
         String localizedName = Localizator.translate("spotlight.category." + cat.getName().toLowerCase());
         float cw = Fonts.MEDIUM.getFont(6.0F).width(localizedName) + 12.0F;

         if (cx + cw > this.pillX + PILL_WIDTH - 10.0F) break;

         ColorRGBA bg = selected
             ? Colors.getAccent().withAlpha(120)
             : Colors.getBackgroundColor().withAlpha((int)(80 * alpha));
         context.drawLiquidGlass(cx, cy, cw, ch, 2.0F, 0.05F, BorderRadius.all(8.0F), bg);
         context.drawText(
            Fonts.MEDIUM.getFont(6.0F), localizedName,
             cx + cw / 2.0F - Fonts.MEDIUM.getFont(6.0F).width(localizedName) / 2.0F,
             cy + GuiUtility.getMiddleOfBox(Fonts.MEDIUM.getFont(6.0F).height(), ch) - 0.5F,
            selected ? Colors.WHITE : catTextColor.mulAlpha(0.6F * alpha)
         );

         cx += cw + 4.0F;
      }
   }

   private static final float CATEGORY_BAR_HEIGHT = 22.0F;

    private void renderModuleList(UIContext context, float alpha) {
       float listX = this.pillX + 6.0F;
       float listY = this.pillY + PILL_HEIGHT + CATEGORY_BAR_HEIGHT;
       float listW = PILL_WIDTH - 12.0F;
       float contentH = this.getTotalContentHeight();
       float currentExpand = Math.max(0.0F, this.expandAnimation.getValue());
       float availH = MAX_RESULTS_HEIGHT * this.resultsHeightFactor.getValue() * currentExpand - CATEGORY_BAR_HEIGHT;
       float maxH = Math.max(MODULE_HEIGHT + 2.0F, availH);
       float listH = Math.max(MODULE_HEIGHT + 2.0F, Math.min(contentH, maxH));

      if (contentH <= 0.0F) {
         ScissorUtility.pop();
         return;
      }

      ScissorUtility.push(context.getMatrices(), listX, listY, listW, listH);

      float scroll = (float)(-this.scrollHandler.getValue());
      float yOffset = scroll;

      for (SpotlightModule spotlightModule : this.allModules) {
         if (this.searchCheck(spotlightModule) || this.categoryCheck(spotlightModule)) continue;

         spotlightModule.set(listX, listY + yOffset, listW, MODULE_HEIGHT);
         spotlightModule.render(context);
         spotlightModule.renderText(context);
         spotlightModule.renderToggle(context);

         if (GuiUtility.isHovered(spotlightModule.getX(), spotlightModule.getY(), spotlightModule.getWidth(), spotlightModule.getHeight(), context)) {
            CursorUtility.set(CursorType.HAND);
         }

         yOffset += MODULE_HEIGHT + 2.0F;
      }

      float maxScroll = -Math.max(0.0F, yOffset - scroll - listH);
      this.scrollHandler.setMax(maxScroll);

      ScissorUtility.pop();
   }

     private boolean searchCheck(SpotlightModule module) {
        TextField search = this.searchField;
        if (search == null || search.getBuiltText().isBlank()) return false;
        String query = search.getBuiltText().toLowerCase();
        String name = module.getModule().getName().toLowerCase();
        String transliterated = transliterate(query);
        return !name.contains(query)
           && !name.contains(transliterated)
           && !name.replace(" ", "").contains(query);
     }

     private static String transliterate(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
           result.append(TRANSLIT_MAP.getOrDefault(c, String.valueOf(c)));
        }
        return result.toString();
     }

     private static final java.util.HashMap<Character, String> TRANSLIT_MAP = new java.util.HashMap<>();
     static {
        TRANSLIT_MAP.put('а', "a"); TRANSLIT_MAP.put('б', "b"); TRANSLIT_MAP.put('в', "v");
        TRANSLIT_MAP.put('г', "g"); TRANSLIT_MAP.put('д', "d"); TRANSLIT_MAP.put('е', "e");
        TRANSLIT_MAP.put('ё', "yo"); TRANSLIT_MAP.put('ж', "zh"); TRANSLIT_MAP.put('з', "z");
        TRANSLIT_MAP.put('и', "i"); TRANSLIT_MAP.put('й', "y"); TRANSLIT_MAP.put('к', "k");
        TRANSLIT_MAP.put('л', "l"); TRANSLIT_MAP.put('м', "m"); TRANSLIT_MAP.put('н', "n");
        TRANSLIT_MAP.put('о', "o"); TRANSLIT_MAP.put('п', "p"); TRANSLIT_MAP.put('р', "r");
        TRANSLIT_MAP.put('с', "s"); TRANSLIT_MAP.put('т', "t"); TRANSLIT_MAP.put('у', "u");
        TRANSLIT_MAP.put('ф', "f"); TRANSLIT_MAP.put('х', "kh"); TRANSLIT_MAP.put('ц', "ts");
        TRANSLIT_MAP.put('ч', "ch"); TRANSLIT_MAP.put('ш', "sh"); TRANSLIT_MAP.put('щ', "shch");
        TRANSLIT_MAP.put('ъ', ""); TRANSLIT_MAP.put('ы', "y"); TRANSLIT_MAP.put('ь', "");
        TRANSLIT_MAP.put('э', "e"); TRANSLIT_MAP.put('ю', "yu"); TRANSLIT_MAP.put('я', "ya");
     }

     private static boolean matchesQuery(String name, String query) {
        if (query == null || query.isEmpty()) return true;
        String lowerName = name.toLowerCase();
        String lowerQuery = query.toLowerCase();
        return lowerName.contains(lowerQuery)
           || lowerName.contains(transliterate(lowerQuery));
     }

     private static final float CONFIG_HEIGHT = 24.0F;
    private static final float CONFIG_BUTTON_W = 20.0F;

    private void renderConfigList(UIContext context, float alpha, float availH) {
       List<ConfigFile> configs = BKClientInit.getInstance().getConfigManager().getConfigFiles();
       String searchQuery = this.searchField != null ? this.searchField.getBuiltText().toLowerCase() : "";

       List<ConfigFile> filtered = new ArrayList<>();
       for (ConfigFile cf : configs) {
          if (cf.getFileName().equalsIgnoreCase("autosave")) continue;
          if (searchQuery.isEmpty() || matchesQuery(cf.getFileName(), searchQuery)) {
             filtered.add(cf);
          }
       }

       float listX = this.pillX + 6.0F;
       float listY = this.pillY + PILL_HEIGHT + 4.0F;
       float listW = PILL_WIDTH - 12.0F;
       float contentH = filtered.size() * (CONFIG_HEIGHT + 2.0F);

       if (contentH <= 0.0F) {
          context.drawText(
             Fonts.MEDIUM.getFont(7.0F),
             Localizator.translate("spotlight.configs.empty"),
             listX + listW / 2.0F - Fonts.MEDIUM.getFont(7.0F).width(Localizator.translate("spotlight.configs.empty")) / 2.0F,
             listY + 4.0F,
             Colors.getTextColor().mulAlpha(0.4F * alpha)
          );
          return;
       }

       float scroll = (float)(-this.scrollHandler.getValue());
       float yOffset = scroll;
       boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
          && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();

       for (int i = 0; i < filtered.size(); i++) {
          ConfigFile cf = filtered.get(i);
          float itemY = listY + yOffset;
          boolean hovered = GuiUtility.isHovered(listX, itemY, listW, CONFIG_HEIGHT, context.getMouseX(), context.getMouseY());

            ColorRGBA bg = dark
               ? new ColorRGBA(255, 255, 255, (int)(12 * (hovered ? 1 : 0) * alpha))
               : new ColorRGBA(0, 0, 0, (int)(5 * (hovered ? 1 : 0) * alpha));
            context.drawSquircle(listX, itemY, listW, CONFIG_HEIGHT, 4.0F, BorderRadius.all(12.0F), bg);

           boolean isCurrent = BKClientInit.getInstance().getConfigManager().getCurrent() == cf;

              ColorRGBA nameColor = isCurrent ? Colors.getAccent().withAlpha((int)(255 * alpha)) : Colors.getTextColor().mulAlpha(0.85F * alpha);
              context.drawText(
                 Fonts.MEDIUM.getFont(7.0F),
                 cf.getFileName(),
                 listX + 10.0F,
                 itemY + GuiUtility.getMiddleOfBox(Fonts.MEDIUM.getFont(7.0F).height(), CONFIG_HEIGHT) - 0.5F,
                 nameColor
              );

              float btnY = itemY + (CONFIG_HEIGHT - 14.0F) / 2.0F;
              float btnRight = listX + listW - 6.0F;

              boolean delHovered = GuiUtility.isHovered(btnRight - 14.0F, btnY, 14.0F, 14.0F, context.getMouseX(), context.getMouseY());
              ColorRGBA delColor = delHovered
                 ? new ColorRGBA(255, 80, 80, (int)(200 * alpha))
                 : Colors.getTextColor().mulAlpha(0.3F * alpha);
              context.drawText(
                 Fonts.MEDIUM.getFont(7.0F),
                 "X",
                 btnRight - 7.0F - Fonts.MEDIUM.getFont(7.0F).width("X") / 2.0F,
                 itemY + GuiUtility.getMiddleOfBox(Fonts.MEDIUM.getFont(7.0F).height(), CONFIG_HEIGHT) - 0.5F,
                 delColor
              );

              if (hovered) {
                 CursorUtility.set(CursorType.HAND);
              }

          yOffset += CONFIG_HEIGHT + 2.0F;
       }

       float maxScroll = -Math.max(0.0F, yOffset - scroll - availH);
       this.scrollHandler.setMax(maxScroll);
    }

    private int getFilteredFriendCount() {
       List<String> friends = BKClientInit.getInstance().getFriendManager().listFriends();
       String searchQuery = this.searchField != null ? this.searchField.getBuiltText().toLowerCase() : "";
       int count = 0;
       for (String name : friends) {
          if (searchQuery.isEmpty() || matchesQuery(name, searchQuery)) {
             count++;
          }
       }
       return count;
    }

    private void renderFriendList(UIContext context, float alpha, float availH) {
       List<String> friends = new ArrayList<>(BKClientInit.getInstance().getFriendManager().listFriends());
       String searchQuery = this.searchField != null ? this.searchField.getBuiltText().toLowerCase() : "";

       List<String> filtered = new ArrayList<>();
       for (String name : friends) {
          if (searchQuery.isEmpty() || matchesQuery(name, searchQuery)) {
             filtered.add(name);
          }
       }

       float listX = this.pillX + 6.0F;
       float listY = this.pillY + PILL_HEIGHT + 4.0F;
       float listW = PILL_WIDTH - 12.0F;
       float contentH = filtered.size() * (CONFIG_HEIGHT + 2.0F);

       if (contentH <= 0.0F) {
          String emptyText = Localizator.translate("spotlight.friends.empty");
          context.drawText(
             Fonts.MEDIUM.getFont(7.0F),
             emptyText,
             listX + listW / 2.0F - Fonts.MEDIUM.getFont(7.0F).width(emptyText) / 2.0F,
             listY + 4.0F,
             Colors.getTextColor().mulAlpha(0.4F * alpha)
          );
          return;
       }

       float scroll = (float)(-this.scrollHandler.getValue());
       float yOffset = scroll;
       boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
          && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();

       for (int i = 0; i < filtered.size(); i++) {
          String name = filtered.get(i);
          float itemY = listY + yOffset;
          boolean hovered = GuiUtility.isHovered(listX, itemY, listW, CONFIG_HEIGHT, context.getMouseX(), context.getMouseY());

          ColorRGBA bg = dark
             ? new ColorRGBA(255, 255, 255, (int)(12 * (hovered ? 1 : 0) * alpha))
             : new ColorRGBA(0, 0, 0, (int)(5 * (hovered ? 1 : 0) * alpha));
          context.drawSquircle(listX, itemY, listW, CONFIG_HEIGHT, 4.0F, BorderRadius.all(12.0F), bg);

          context.drawText(
             Fonts.MEDIUM.getFont(7.0F),
             name,
             listX + 10.0F,
             itemY + GuiUtility.getMiddleOfBox(Fonts.MEDIUM.getFont(7.0F).height(), CONFIG_HEIGHT) - 0.5F,
             Colors.getTextColor().mulAlpha(0.85F * alpha)
          );

          float btnRight = listX + listW - 6.0F;

          boolean delHovered = GuiUtility.isHovered(btnRight - 14.0F, itemY, 14.0F, CONFIG_HEIGHT, context.getMouseX(), context.getMouseY());
          ColorRGBA delColor = delHovered
             ? new ColorRGBA(255, 80, 80, (int)(200 * alpha))
             : Colors.getTextColor().mulAlpha(0.3F * alpha);
          context.drawText(
             Fonts.MEDIUM.getFont(7.0F),
             "X",
             btnRight - 7.0F - Fonts.MEDIUM.getFont(7.0F).width("X") / 2.0F,
             itemY + GuiUtility.getMiddleOfBox(Fonts.MEDIUM.getFont(7.0F).height(), CONFIG_HEIGHT) - 0.5F,
             delColor
          );

          if (hovered) {
             CursorUtility.set(CursorType.HAND);
          }

          yOffset += CONFIG_HEIGHT + 2.0F;
       }

       float maxScroll = -Math.max(0.0F, yOffset - scroll - availH);
       this.scrollHandler.setMax(maxScroll);
    }

    private boolean handleFriendClick(double mouseX, double mouseY) {
       if (!this.showFriends) return false;

       List<String> friends = new ArrayList<>(BKClientInit.getInstance().getFriendManager().listFriends());
       String searchQuery = this.searchField != null ? this.searchField.getBuiltText().toLowerCase() : "";

       List<String> filtered = new ArrayList<>();
       for (String name : friends) {
          if (searchQuery.isEmpty() || matchesQuery(name, searchQuery)) {
             filtered.add(name);
          }
       }

       float listX = this.pillX + 6.0F;
       float listY = this.pillY + PILL_HEIGHT + 4.0F;
       float listW = PILL_WIDTH - 12.0F;
       float scroll = (float)(-this.scrollHandler.getValue());
       float yOffset = scroll;

       for (String name : filtered) {
          float itemY = listY + yOffset;
          if (GuiUtility.isHovered(listX, itemY, listW, CONFIG_HEIGHT, mouseX, mouseY)) {
             float btnRight = listX + listW - 6.0F;

             if (GuiUtility.isHovered(btnRight - 14.0F, itemY, 14.0F, CONFIG_HEIGHT, mouseX, mouseY)) {
                BKClientInit.getInstance().getFriendManager().remove(name);
                return true;
             }
          }
          yOffset += CONFIG_HEIGHT + 2.0F;
       }
       return false;
    }

    private boolean handleConfigClick(double mouseX, double mouseY, MouseButton button) {
       if (!this.showConfigs) return false;

       List<ConfigFile> configs = BKClientInit.getInstance().getConfigManager().getConfigFiles();
       String searchQuery = this.searchField != null ? this.searchField.getBuiltText().toLowerCase() : "";

       List<ConfigFile> filtered = new ArrayList<>();
       for (ConfigFile cf : configs) {
          if (cf.getFileName().equalsIgnoreCase("autosave")) continue;
          if (searchQuery.isEmpty() || matchesQuery(cf.getFileName(), searchQuery)) {
             filtered.add(cf);
          }
       }

       float listX = this.pillX + 6.0F;
        float listY = this.pillY + PILL_HEIGHT + 4.0F;
        float listW = PILL_WIDTH - 12.0F;
        float scroll = (float)(-this.scrollHandler.getValue());
        float yOffset = scroll;

        for (ConfigFile cf : filtered) {
           float itemY = listY + yOffset;
           if (GuiUtility.isHovered(listX, itemY, listW, CONFIG_HEIGHT, mouseX, mouseY)) {
            float btnY = itemY + (CONFIG_HEIGHT - 14.0F) / 2.0F;
            float btnRight = listX + listW - 6.0F;

            if (GuiUtility.isHovered(btnRight - 12.0F, btnY, 12.0F, 14.0F, mouseX, mouseY)) {
                 cf.delete();
                 BKClientInit.getInstance().getConfigManager().refresh();
                 return true;
              }

              cf.load();
              return true;
          }
          yOffset += CONFIG_HEIGHT + 2.0F;
       }
       return false;
    }

   private void handleMovementKeys() {
      if (dev.bkclient.ui.components.textfield.TextField.hasFocusedField()) {
         dev.bkclient.ui.components.textfield.TextField.releaseMovementKeys(); return;
      }
      if (mc.player != null && !this.isTyping()) {
         long windowHandle = mc.getWindow().getHandle();
         for (KeyBinding key : new KeyBinding[]{
            mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey
         }) {
            int keyCode = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode();
            key.setPressed(InputUtil.isKeyPressed(windowHandle, keyCode));
         }
         if (mc.player.getAbilities().flying) {
            int keyCode = InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode();
            mc.options.sneakKey.setPressed(InputUtil.isKeyPressed(windowHandle, keyCode));
         }
      }
   }

   private boolean isTyping() {
      return this.searchField != null && this.searchField.isFocused();
   }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
       this.clickAnimation.setTargetValue(1.0F);
       this.clickAnimation.reset(0.0F);

       for (SpotlightSettings window : this.windows) {
          window.onMouseClicked(mouseX, mouseY, button);
       }
       for (ColorPicker colorPicker : this.colorPickers) {
          colorPicker.onMouseClicked(mouseX, mouseY, button);
       }

       if (button != MouseButton.LEFT) {
          this.handleClick(mouseX, mouseY, button);
       }
    }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (button == MouseButton.LEFT) {
         this.handleClick(mouseX, mouseY, button);
      }

      for (SpotlightModule spotlightModule : this.allModules) {
         spotlightModule.onMouseReleased(mouseX, mouseY, button);
      }
      for (SpotlightSettings window : this.windows) {
         window.onMouseReleased(mouseX, mouseY, button);
      }
      for (ColorPicker colorPicker : this.colorPickers) {
         colorPicker.onMouseReleased(mouseX, mouseY, button);
      }
      if (this.searchField.isFocused()) {
         this.searchField.onMouseReleased(mouseX, mouseY, button);
      }
      super.onMouseReleased(mouseX, mouseY, button);
   }

    private void handleClick(double mouseX, double mouseY, MouseButton button) {
       for (SpotlightSettings window : this.windows) {
          if (window.isHovered(mouseX, mouseY)) return;
       }
       for (ColorPicker colorPicker : this.colorPickers) {
          if (colorPicker.isHovered(mouseX, mouseY)) return;
       }

      float catBarArea = CATEGORY_BAR_HEIGHT + 10.0F;
      float totalH = PILL_HEIGHT + (this.resultsVisible ? Math.min(this.getTotalContentHeight() + 10.0F, MAX_RESULTS_HEIGHT) : catBarArea);

      if (GuiUtility.isHovered(this.pillX, this.pillY, PILL_WIDTH, totalH, mouseX, mouseY)) {
         if (!BKClientInit.getInstance().getHud().getIsland().handleClick((float)mouseX, (float)mouseY, button.getButtonIndex())) {

                float listX = this.pillX + 6.0F;
               float listY = this.pillY + PILL_HEIGHT + CATEGORY_BAR_HEIGHT;
               float scroll = (float)(-this.scrollHandler.getValue());
               float yOffset = scroll;

                 if (this.handleCircleClick(mouseX, mouseY, button)) return;

                 float clickProgress = this.expandAnimation.getValue();
                 boolean resultsClickable = clickProgress > 0.5F;

                  if (!this.resultsVisible || (!resultsClickable && !this.showConfigs && !this.expanded && !this.showFriends)) {
                    if (button != MouseButton.MIDDLE) {
                       this.searchField.onMouseClicked(mouseX, mouseY, button);
                    }
                    super.onMouseClicked(mouseX, mouseY, button);
                    return;
                 }

                 if (this.handleCategoryClick(mouseX, mouseY)) {
                    return;
                 }

                 if (this.handleConfigClick(mouseX, mouseY, button)) return;
                 if (this.handleFriendClick(mouseX, mouseY)) return;

                  if (!this.showConfigs && !this.showFriends) {
                     float sfX = this.pillX + 14.0F + 18.0F;
                     float sfY = this.pillY + (PILL_HEIGHT - 18.0F) / 2.0F;
                     float sfW = PILL_WIDTH - 28.0F - 18.0F;
                     if (this.isTyping() || GuiUtility.isHovered(sfX, sfY, sfW, 18.0F, mouseX, mouseY)) {
                        if (button != MouseButton.MIDDLE) {
                           this.searchField.onMouseClicked(mouseX, mouseY, button);
                        }
                        super.onMouseClicked(mouseX, mouseY, button);
                        return;
                     }
                     for (SpotlightModule spotlightModule : this.allModules) {
                       if (this.searchCheck(spotlightModule) || this.categoryCheck(spotlightModule)) {
                          continue;
                       }
                       if (mouseY >= listY && GuiUtility.isHovered(spotlightModule.getX(), spotlightModule.getY(), spotlightModule.getWidth(), spotlightModule.getHeight(), mouseX, mouseY)) {
                          spotlightModule.onMouseClicked(mouseX, mouseY, button);
                          return;
                       }
                       yOffset += MODULE_HEIGHT + 2.0F;
                    }
                 }

                  if (button != MouseButton.MIDDLE) {
                    this.searchField.onMouseClicked(mouseX, mouseY, button);
                 }

                 super.onMouseClicked(mouseX, mouseY, button);
               }
            }
         }

     private boolean handleCircleClick(double mouseX, double mouseY, MouseButton button) {
        if (!this.circlesActive) return false;

        float pillDrawW = PILL_WIDTH * (1.0F - 0.45F * this.rightHoverAnim.getValue());
        float cAreaX = this.pillX + pillDrawW;
        float cAreaW = PILL_WIDTH - pillDrawW;
        float circleSize = PILL_HEIGHT;
        float spacing = 4.0F;
        float totalW = 4.0F * circleSize + 3.0F * spacing;
        float startX = cAreaX + (cAreaW - totalW) / 2.0F;
        float cy = this.pillY;

        for (int i = 0; i < 4; i++) {
           float cX = startX + i * (circleSize + spacing);
           if (GuiUtility.isHovered(cX, cy, circleSize, circleSize, mouseX, mouseY)) {
                switch (i) {
                   case 0:
                      this.expanded = !this.expanded;
                      this.showConfigs = false;
                      this.showFriends = false;
                      this.searchField.setPreview(Localizator.translate("spotlight.title"));
                      this.scrollHandler.reset();
                      break;
                   case 1:
                      this.showConfigs = !this.showConfigs;
                      if (!this.showConfigs) {
                      }
                      this.expanded = false;
                      this.showFriends = false;
                      this.currentCategory = null;
                      this.searchField.setPreview(this.showConfigs ? Localizator.translate("spotlight.search.configs") : Localizator.translate("spotlight.title"));
                      this.searchField.clear();
                      this.scrollHandler.reset();
                      BKClientInit.getInstance().getConfigManager().refresh();
                      break;
                   case 2:
                      this.showFriends = !this.showFriends;
                      this.expanded = false;
                      this.showConfigs = false;
                      this.currentCategory = null;
                      this.searchField.setPreview(this.showFriends ? Localizator.translate("spotlight.search.friends") : Localizator.translate("spotlight.title"));
                      this.searchField.clear();
                      this.scrollHandler.reset();
                      break;
                   case 3:
                      this.searchField.setFocused(true);
                      break;
              }
             return true;
          }
       }
        return false;
     }

     private boolean handleCategoryClick(double mouseX, double mouseY) {
       if (!this.resultsVisible) return false;
       float cx = this.pillX + 10.0F;
      float cy = this.pillY + PILL_HEIGHT + 4.0F;
      float ch = 18.0F;
      for (MenuCategory cat : MenuCategory.values()) {
         String localizedName = Localizator.translate("spotlight.category." + cat.getName().toLowerCase());
         float cw = Fonts.MEDIUM.getFont(6.0F).width(localizedName) + 12.0F;
         if (cx + cw > this.pillX + PILL_WIDTH - 10.0F) break;
         if (GuiUtility.isHovered(cx, cy, cw, ch, mouseX, mouseY)) {
            this.currentCategory = this.currentCategory == cat ? null : cat;
            this.scrollHandler.reset();
            return true;
         }
         cx += cw + 4.0F;
      }
      return false;
   }

   private void clearSearchIfClicked(double mouseX, double mouseY) {}

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      for (SpotlightSettings window : this.windows) {
         window.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
      if (this.resultsVisible && this.expandAnimation.getValue() > 0.5F) {
         float listY = this.pillY + PILL_HEIGHT + CATEGORY_BAR_HEIGHT;
         float contentH = this.getTotalContentHeight();
         float currentExpand = Math.max(0.0F, this.expandAnimation.getValue());
         float availH = MAX_RESULTS_HEIGHT * currentExpand - CATEGORY_BAR_HEIGHT;
         float maxH = Math.max(0.0F, availH);
         float listH = Math.min(contentH, maxH);
         if (GuiUtility.isHovered(this.pillX + 6.0F, listY, PILL_WIDTH - 12.0F, listH, mouseX, mouseY)) {
            this.scrollHandler.scroll(verticalAmount);
         }
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.searchField.isFocused() && Screen.hasControlDown() && keyCode == 70) {
         this.searchField.setFocused(true);
      }

      this.scrollHandler.onKeyPressed(keyCode);

      for (SpotlightSettings window : this.windows) {
         window.onKeyPressed(keyCode, scanCode, modifiers);
      }

      for (ColorPicker colorPicker : this.colorPickers) {
         colorPicker.onKeyPressed(keyCode, scanCode, modifiers);
      }

       if (this.searchField.isFocused() && !this.isBindingModule()) {
          this.searchField.onKeyPressed(keyCode, scanCode, modifiers);
       }

      for (SpotlightModule module : this.allModules) {
         module.onKeyPressed(keyCode, scanCode, modifiers);
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

    public boolean charTyped(char chr, int modifiers) {
       if (this.searchField.isFocused() && !this.isBindingModule()) {
          this.searchField.charTyped(chr, modifiers);
       }

      for (SpotlightSettings window : this.windows) {
         window.charTyped(chr, modifiers);
      }
      for (SpotlightModule module : this.allModules) {
         module.charTyped(chr, modifiers);
      }
      return super.charTyped(chr, modifiers);
   }

   public void close() {
      this.closing = true;
      this.circlesActive = false;
      this.expanded = false;
      this.showConfigs = false;
      this.showFriends = false;
      this.searchField.clear();
      this.searchField.setPreview(Localizator.translate("spotlight.title"));
      this.currentCategory = null;
        this.windows.clear();
        BKClientInit.getInstance().getModuleManager().getModule(MenuModule.class).disable();
      BKClientInit.getInstance().getFileManager().writeFile("client");
      if (BKClientInit.getInstance().getConfigManager().getCurrent() != null) {
         BKClientInit.getInstance().getConfigManager().getCurrent().save();
      }
      BKClientInit.getInstance().getThemeManager().flushSave();
      if (TextField.LAST_FIELD != null) {
         TextField.LAST_FIELD.setFocused(false);
      }
      if (this.client != null) {
         super.close();
      }
   }

   public boolean isBindingModule() {
      return this.allModules.stream().anyMatch(SpotlightModule::isBinding);
   }

   public boolean shouldPause() {
      return false;
   }

   public void renderBackground(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public float getPillWidth() { return PILL_WIDTH; }
   public float getPillHeight() { return PILL_HEIGHT; }
   public ScrollHandler getScrollHandler() { return this.scrollHandler; }
   public MenuCategory getCurrentCategory() { return this.currentCategory; }
   public List<SpotlightModule> getAllModules() { return this.allModules; }
   public List<SpotlightSettings> getWindows() { return this.windows; }
   public TextField getSearchField() { return this.searchField; }
   public List<ColorPicker> getColorPickers() { return this.colorPickers; }
   public float getX() { return this.pillX; }
   public float getY() { return this.pillY; }
   public boolean isClosing() { return this.closing; }
}
