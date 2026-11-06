package dev.bkclient.ui.menu.dropdown.components.settings.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.render.ScissorUtility;
import java.util.List;
import java.util.Locale;

public class ModeSettingComponent extends MenuSettingComponent<ModeSetting> {
   private static final float HORIZONTAL_PADDING = 8.0F;
   private static final float MENU_HORIZONTAL_PADDING = 6.0F;
   private static final float CIRCLE_MARGIN = 4.0F;
   private static final float CIRCLE_SIZE = 11.0F;
   private static final float VALUE_GAP = 2.0F;
   private static final float LIST_ROUNDING = 7.0F;
   private static final float LIST_PADDING = 3.0F;
   private static final float LIST_ITEM_HEIGHT = 20.0F;
   private static final float COMPONENT_HEIGHT = 29.0F;
   private static final long DROPDOWN_DURATION = 250L;
   private static final long PRESS_DURATION = 200L;

   /** Below this many entries the dropdown behaves exactly as before: no search, no cap. */
   private static final int SEARCH_THRESHOLD = 12;
   private static final float SEARCH_HEIGHT = 16.0F;
   private static final float SEARCH_GAP = 3.0F;
   private static final float MAX_LIST_HEIGHT = 160.0F;

   private final Animation dropdownAnimation = new Animation(DROPDOWN_DURATION, Easing.QUINTIC_OUT);
   private final Animation circleHoverAnimation = new Animation(200L, Easing.FIGMA_EASE_IN_OUT);
   private boolean dropdownOpen;
   private long pressTime;
   private float dropdownY;
   private long closeTime;

   // Fonts are resource-backed and unavailable while modules are still being
   // registered, so the field is built lazily on the first render.
   private TextField searchField;
   private final ScrollHandler scroll = new ScrollHandler();

   private float getMenuWidth() {
      Font font = Fonts.REGULAR.getFont(7.0F);
      float maxW = 60.0F;
      for (ModeSetting.Value v : this.setting.getValues()) {
         if (!v.isHidden()) {
            maxW = Math.max(maxW, font.width(Localizator.translate(v.getName())) + MENU_HORIZONTAL_PADDING * 2.0F);
         }
      }
      return maxW;
   }

   public ModeSettingComponent(ModeSetting setting, CustomComponent parent) {
      super(setting, parent);
      this.scroll.setSpeed(14.0);
   }

   @Override
   protected void renderComponent(UIContext context) {
      float mx = context.getMouseX();
      float my = context.getMouseY();
      Font font = Fonts.REGULAR.getFont(7.0F);

      float leftEdge = this.x + HORIZONTAL_PADDING;
      float rightEdge = this.x + this.width - HORIZONTAL_PADDING;
      float circleX = rightEdge - CIRCLE_SIZE;
      float circleY = this.y + (COMPONENT_HEIGHT - CIRCLE_SIZE) / 2.0F + 0.7F;
      float circleCenterX = circleX + CIRCLE_SIZE / 2.0F;
      float circleCenterY = circleY + CIRCLE_SIZE / 2.0F;

      this.hoverAnimation.update(this.isHovered(mx, my));

      String settingName = Localizator.translate(this.setting.getName());
      String currentValue = Localizator.translate(this.setting.getValue().getName());
      float valueWidth = font.width(currentValue);
      float valueX = circleX - VALUE_GAP - valueWidth;
      float labelEndX = valueX - CIRCLE_MARGIN;
      float maxLabelWidth = Math.max(labelEndX - leftEdge, 20.0F);
      this.drawSettingName(
         context, font, settingName,
         leftEdge,
         this.y + GuiUtility.getMiddleOfBox(font.height(), COMPONENT_HEIGHT),
         Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue())),
         maxLabelWidth
      );

      float valueY = this.y + GuiUtility.getMiddleOfBox(font.height(), COMPONENT_HEIGHT);
      context.drawText(font, currentValue, valueX, valueY,
         Colors.getTextColor().withAlpha(255.0F * 0.6F));

      boolean insideCircle = this.isTriggerHovered(mx, my);
      this.circleHoverAnimation.update(insideCircle);

      boolean isDark = ClickGuiV2Theme.active() ? ClickGuiV2Theme.current().dark : BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
      ColorRGBA circleBg = isDark
         ? new ColorRGBA(72, 72, 72)
         : new ColorRGBA(160, 160, 160);
      float hoverBlend = this.circleHoverAnimation.getValue();
      ColorRGBA hoverTint = isDark ? ColorRGBA.WHITE : ColorRGBA.BLACK;
      circleBg = circleBg.mix(hoverTint, hoverBlend * 0.15F);

      context.pushMatrix();
      long elapsed = System.currentTimeMillis() - this.pressTime;
      float pressProgress = Math.min(1.0F, (float)elapsed / (float)PRESS_DURATION);
      float scaleEased = pressProgress < 0.5F
         ? pressProgress * 2.0F
         : 2.0F - pressProgress * 2.0F;
      float scale = 1.0F - 0.04F * scaleEased;
      context.getMatrices().translate(circleCenterX, circleCenterY, 0.0F);
      context.getMatrices().scale(scale, scale, scale);
      context.getMatrices().translate(-circleCenterX, -circleCenterY, 0.0F);

      context.drawSquircle(circleX, circleY, CIRCLE_SIZE, CIRCLE_SIZE, 7.0F,
         BorderRadius.all(CIRCLE_SIZE / 2.0F), circleBg.withAlpha(180));

      float iconInset = 3.0F;
      float iconArea = CIRCLE_SIZE - iconInset * 2.0F;
      context.drawTexture(
         BKClientInit.id("textures/modesetting/mode.png"),
         circleX + iconInset, circleY + iconInset,
         iconArea, iconArea,
         Colors.getIconsColor()
      );
      context.popMatrix();

      if (insideCircle) {
         CursorUtility.set(CursorType.HAND);
      }

      this.dropdownAnimation.update(this.dropdownOpen ? 1.0F : 0.0F);
   }

   private void ensureSearchField() {
      if (this.searchField != null) return;
      this.searchField = new TextField(Fonts.REGULAR.getFont(6.3F));
      this.searchField.setPreview("Search...");
   }

   /** True once the list is long enough that dropping it whole would run off the screen. */
   private boolean isSearchVisible() {
      int total = 0;
      for (ModeSetting.Value v : this.setting.getValues()) {
         if (!v.isHidden()) total++;
      }
      return total > SEARCH_THRESHOLD;
   }

   private static String normalize(String text) {
      return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
   }

   /** Entries actually shown: never hidden, and matching the query when the search is up. */
   private List<ModeSetting.Value> visibleValues() {
      String query = normalize(!this.isSearchVisible() || this.searchField == null
         ? "" : this.searchField.getBuiltText());
      return this.setting.getValues().stream()
         .filter(v -> !v.isHidden())
         .filter(v -> query.isEmpty()
            || normalize(Localizator.translate(v.getName())).contains(query)
            || normalize(v.getName()).contains(query))
         .toList();
   }

   private float itemHeight() {
      return Fonts.REGULAR.getFont(7.0F).height() + LIST_PADDING * 2.0F;
   }

   private float menuX() {
      return this.x + this.width - HORIZONTAL_PADDING - getMenuWidth();
   }

   private float menuY() {
      return this.dropdownY + COMPONENT_HEIGHT + 0.7F;
   }

   private float listY() {
      return menuY() + LIST_PADDING + (isSearchVisible() ? SEARCH_HEIGHT + SEARCH_GAP : 0.0F);
   }

   private float listHeight(int count) {
      float raw = count * itemHeight();
      return isSearchVisible() ? Math.min(MAX_LIST_HEIGHT, raw) : raw;
   }

   private void renderSearch(UIContext context, float x, float y, float width) {
      BorderRadius radius = BorderRadius.all(5.0F);
      boolean hovered = GuiUtility.isHovered(x, y, width, SEARCH_HEIGHT, context);
      context.drawRoundedRect(x, y, width, SEARCH_HEIGHT, radius,
         Colors.getBackgroundColor().withAlpha(hovered || this.searchField.isFocused() ? 120.0F : 85.0F));
      context.drawRoundedBorder(x, y, width, SEARCH_HEIGHT, 0.05F, radius,
         (this.searchField.isFocused() ? Colors.getAccent() : Colors.getTextColor())
            .withAlpha(this.searchField.isFocused() ? 85.0F : 13.0F));
      context.drawIcon(Fonts.ICONS, 'v', x + 5.0F, y + 4.0F, 7.5F,
         Colors.getTextColor().withAlpha(this.searchField.isFocused() ? 190.0F : 100.0F));
      this.searchField.setTextColor(Colors.getTextColor());
      this.searchField.set(x + 13.0F, y + 1.0F, width - 16.0F, SEARCH_HEIGHT - 2.0F);
      this.searchField.render(context);
   }

   public void renderOverlay(UIContext context) {
      if (this.dropdownAnimation.getValue() <= 0.0F) return;
      ensureSearchField();

      float mx = context.getMouseX();
      float my = context.getMouseY();
      Font font = Fonts.REGULAR.getFont(7.0F);

      boolean searchVisible = isSearchVisible();
      List<ModeSetting.Value> values = visibleValues();

      float menuW = getMenuWidth();
      float menuX = menuX();
      float menuY = menuY();
      float itemH = itemHeight();
      float listH = listHeight(values.size());
      float a = this.dropdownAnimation.getValue();

      float chromeH = LIST_PADDING * 2.0F + (searchVisible ? SEARCH_HEIGHT + SEARCH_GAP : 0.0F);
      float menuH = (chromeH + listH) * a;

      context.drawRoundedRect(menuX, menuY, menuW, menuH,
         BorderRadius.all(LIST_ROUNDING),
         Colors.getAdditionalColor().withAlpha(245));

      if (a <= 0.95F) return;

      if (searchVisible) {
         renderSearch(context, menuX + LIST_PADDING, menuY + LIST_PADDING, menuW - LIST_PADDING * 2.0F);
      }

      float listY = listY();
      float contentH = values.size() * itemH;
      this.scroll.setMax(Math.min(0.0, listH - contentH));
      this.scroll.update();
      float scrollOffset = searchVisible ? (float) this.scroll.getValue() : 0.0F;

      ScissorUtility.push(context.getMatrices(), menuX, listY, menuW, listH);
      if (values.isEmpty()) {
         Font emptyFont = Fonts.REGULAR.getFont(6.0F);
         context.drawCenteredText(emptyFont, "No matching entries", menuX + menuW / 2.0F,
            listY + (listH - emptyFont.height()) / 2.0F, Colors.getTextColor().withAlpha(95.0F));
      } else {
         for (int i = 0; i < values.size(); i++) {
            ModeSetting.Value value = values.get(i);
            float itemY = listY + i * itemH - scrollOffset;
            if (itemY + itemH < listY || itemY > listY + listH) continue;

            boolean itemHovered = GuiUtility.isHovered(menuX, itemY, menuW, itemH, mx, my)
               && my >= listY && my <= listY + listH;

            value.getHoverAnimation().update(itemHovered);
            value.getActiveAnimation().update(value.isSelected());

            if (itemHovered) {
               float hoverAlpha = 10.0F * value.getHoverAnimation().getValue();
               context.drawRoundedRect(menuX + 0.7F, itemY, menuW - 4.0F, itemH,
                  BorderRadius.all(5.0F),
                  Colors.getTextColor().withAlpha(hoverAlpha * a));
               CursorUtility.set(CursorType.HAND);
            }

            String valName = Localizator.translate(value.getName());
            float textAlpha = value.isSelected() ? 1.0F : 0.75F + 0.25F * value.getHoverAnimation().getValue();
            context.drawText(font, valName, menuX + MENU_HORIZONTAL_PADDING, itemY + LIST_PADDING,
               Colors.getTextColor().withAlpha(255.0F * textAlpha * a));
         }
      }
      ScissorUtility.pop();
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (button != MouseButton.LEFT) return;
      if (this.isTriggerHovered(mouseX, mouseY)) {
         this.dropdownOpen = !this.dropdownOpen;
         this.pressTime = System.currentTimeMillis();
         if (this.dropdownOpen) {
            this.dropdownY = this.y;
            this.scroll.reset();
            if (this.searchField != null) {
               this.searchField.clear();
               this.searchField.setFocused(false);
            }
         }
         return;
      }

      if (this.dropdownOpen) {
         if (this.dropdownAnimation.getValue() > 0.95F) {
            ensureSearchField();
            boolean searchVisible = isSearchVisible();
            float menuW = getMenuWidth();
            float menuX = menuX();
            float menuY = menuY();
            float itemH = itemHeight();

            if (searchVisible && GuiUtility.isHovered(menuX + LIST_PADDING, menuY + LIST_PADDING,
                  menuW - LIST_PADDING * 2.0F, SEARCH_HEIGHT, mouseX, mouseY)) {
               this.searchField.onMouseClicked(mouseX, mouseY, button);
               return;
            }
            this.searchField.setFocused(false);

            List<ModeSetting.Value> values = visibleValues();
            float listY = listY();
            float listH = listHeight(values.size());
            float scrollOffset = searchVisible ? (float) this.scroll.getValue() : 0.0F;

            for (int i = 0; i < values.size(); i++) {
               ModeSetting.Value value = values.get(i);
               float itemY = listY + i * itemH - scrollOffset;
               if (itemY + itemH < listY || itemY > listY + listH) continue;
               if (mouseY >= listY && mouseY <= listY + listH
                     && GuiUtility.isHovered(menuX, itemY, menuW, itemH, mouseX, mouseY)) {
                  value.select();
                  this.dropdownOpen = false;
                  this.closeTime = System.currentTimeMillis();
                  return;
               }
            }

            float chromeH = LIST_PADDING * 2.0F + (searchVisible ? SEARCH_HEIGHT + SEARCH_GAP : 0.0F);
            if (GuiUtility.isHovered(menuX, menuY, menuW, chromeH + listH, mouseX, mouseY)) {
               return;
            }
         }
         this.dropdownOpen = false;
         this.closeTime = System.currentTimeMillis();
         return;
      }

      if (System.currentTimeMillis() - this.closeTime < 1000L) {
         return;
      }
   }

   private boolean isTriggerHovered(double mouseX, double mouseY) {
      return GuiUtility.isHovered(this.x, this.y, this.width, COMPONENT_HEIGHT, mouseX, mouseY);
   }

   /** True while the pointer is over the open scrollable list. */
   public boolean isScrollAreaHovered(double mouseX, double mouseY) {
      if (!this.dropdownOpen || !isSearchVisible()) return false;
      return GuiUtility.isHovered(menuX(), listY(), getMenuWidth(),
         listHeight(visibleValues().size()), mouseX, mouseY);
   }

   @Override
   public void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (isScrollAreaHovered(mouseX, mouseY)) {
         this.scroll.scroll(verticalAmount);
         return;
      }
      super.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.dropdownOpen && isSearchVisible() && this.searchField != null) {
         this.searchField.onKeyPressed(keyCode, scanCode, modifiers);
      }
      super.onKeyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.dropdownOpen && isSearchVisible() && this.searchField != null
         && this.searchField.charTyped(chr, modifiers)) {
         return true;
      }
      return super.charTyped(chr, modifiers);
   }

   public boolean isDropdownVisible() {
      return this.dropdownOpen;
   }
   public boolean isOverlayHovered(double mouseX, double mouseY) {
      if (!this.dropdownOpen) return false;
      float chrome = LIST_PADDING * 2 + (isSearchVisible() ? SEARCH_HEIGHT + SEARCH_GAP : 0);
      return GuiUtility.isHovered(menuX(), menuY(), getMenuWidth(), chrome + listHeight(visibleValues().size()), mouseX, mouseY);
   }

   @Override
   public float getHeight() {
      this.height = COMPONENT_HEIGHT;
      return this.height;
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight,
         Colors.getTextColor().withAlpha(5.1F));
   }
}
