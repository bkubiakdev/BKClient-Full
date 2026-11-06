package dev.bkclient.ui.menu.dropdown.components.settings.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.components.animated.AnimatedNumber;
import dev.bkclient.ui.components.popup.Popup;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.render.sprite.SpriteAnimationPlayer;

/**
 * Searchable, bounded multi-select using the original compact BKClient selector rows.
 */
public class SelectSettingComponent extends MenuSettingComponent<SelectSetting> {
   private static final float HEADER_HEIGHT = 19.0F;
   private static final float CONTROL_HEIGHT = 16.0F;
   private static final float CONTROL_GAP = 4.0F;
   private static final float ROW_HEIGHT = 12.0F;
   private static final float MAX_LIST_HEIGHT = 120.0F;

   // Fonts are resource-backed and are unavailable while modules are still
   // being registered during MinecraftClient construction. Create the field
   // lazily on the first GUI render, after resource initialization.
   private TextField searchField;
   private final ScrollHandler scroll = new ScrollHandler();
   private AnimatedNumber numberAnim;
   private SelectSetting.Value dragging;
   private boolean initialized;
   private int maxVisibleRows;
   private boolean prefixCompletion;
   public SelectSettingComponent maxVisibleRows(int rows) { this.maxVisibleRows = Math.max(1, rows); return this; }
   public SelectSettingComponent prefixCompletion() { this.prefixCompletion = true; return this; }
   public boolean isSearchFocused() { return searchField != null && searchField.isFocused(); }
   public void clearFocus() { if (searchField != null) searchField.setFocused(false); }
   public boolean canScroll(double amount) {
      return amount < 0 ? scroll.getTargetValue() > scroll.getMax() : scroll.getTargetValue() < 0;
   }
   private SelectSetting.Value suggestion() {
      if (!prefixCompletion || searchField == null || searchField.getText().isBlank()) return null;
      String q = normalize(searchField.getText());
      return setting.getValues().stream().filter(v -> normalize(v.getName()).startsWith(q)).findFirst().orElse(null);
   }

   public SelectSettingComponent(SelectSetting setting, CustomComponent parent) {
      super(setting, parent);
      this.scroll.setSpeed(14.0);
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (!this.setting.isVisible()) return;
      ensureSearchField();
      initializeAnimations();
      if (prefixCompletion) {
         var suggestion = suggestion();
         searchField.setAppend(suggestion == null ? java.util.Map.of() : java.util.Map.of(suggestion.getName(),
            new dev.bkclient.ui.components.textfield.FieldAction(() -> {}, () -> {})));
      }

      float x = this.x + 9.0F;
      float y = this.y + 1.0F;
      float width = this.width - 18.0F;
      renderHeader(context, x, y, width);

      boolean searchVisible = this.isSearchVisible();
      float controlsY = y + HEADER_HEIGHT;
      if (searchVisible) renderSearch(context, x, controlsY, width);

      List<SelectSetting.Value> values = visibleValues();
      float listY = searchVisible ? controlsY + CONTROL_HEIGHT + CONTROL_GAP : controlsY;
      float listH = listHeight(values.size());
      context.drawRoundedRect(x - 1.0F, listY, width + 2.0F, listH, BorderRadius.all(6.0F),
         Colors.getBackgroundColor().withAlpha(76.5F));

      float contentH = values.size() * ROW_HEIGHT + 8.0F;
      this.scroll.setMax(Math.min(0.0, listH - contentH));
      this.scroll.update();
      float scrollOffset = (float)this.scroll.getValue();

      ScissorUtility.push(context.getMatrices(), x - 1.0F, listY, width + 2.0F, listH);
      if (values.isEmpty()) {
         Font emptyFont = Fonts.REGULAR.getFont(6.0F);
         context.drawCenteredText(emptyFont, "No matching entries", x + width / 2.0F,
            listY + (listH - emptyFont.height()) / 2.0F, Colors.getTextColor().withAlpha(95.0F));
      } else {
         for (int i = 0; i < values.size(); i++) {
            SelectSetting.Value value = values.get(i);
            float rowY = listY + 4.0F + i * ROW_HEIGHT - scrollOffset;
            if (this.dragging == value) {
               rowY = Math.clamp(context.getMouseY() - ROW_HEIGHT / 2.0F, listY + 2.0F, listY + listH - ROW_HEIGHT - 2.0F);
            }
            value.setYFactor(rowY);
            if (rowY + ROW_HEIGHT < listY || rowY > listY + listH) continue;
            renderRow(context, value, x, rowY, width, listY, listH);
         }
      }
      ScissorUtility.pop();
      if (contentH > listH) {
         float trackH = listH - 8.0F;
         float thumbH = Math.max(12.0F, trackH * listH / contentH);
         float progress = Math.clamp(scrollOffset / (contentH - listH), 0.0F, 1.0F);
         context.drawRoundedRect(x + width - 1.0F, listY + 4.0F + progress * (trackH - thumbH),
            1.5F, thumbH, BorderRadius.all(0.75F), Colors.getTextColor().withAlpha(95.0F));
      }
   }

   private void initializeAnimations() {
      if (this.initialized) return;
      for (SelectSetting.Value value : this.setting.getValues()) {
         value.setEnableSpriteAnimation(new SpriteAnimationPlayer(BKClientInit.id("animations/check_enable.bkanim")));
         value.setDisableSpriteAnimation(new SpriteAnimationPlayer(BKClientInit.id("animations/check_disable.bkanim")));
         value.setLastState(value.isSelected());
         value.setCurrentSpriteAnimation(value.isSelected() ? value.getEnableSpriteAnimation() : value.getDisableSpriteAnimation());
         if (value.isSelected()) value.getEnableSpriteAnimation().playOnce();
         else {
            value.getDisableSpriteAnimation().setFrame(0);
            value.getDisableSpriteAnimation().stop();
         }
      }
      this.initialized = true;
   }

   private void ensureSearchField() {
      if (this.searchField != null) return;
      this.searchField = new TextField(Fonts.REGULAR.getFont(6.3F));
      this.searchField.setPreview("Search...");
   }

   private void renderHeader(UIContext context, float x, float y, float width) {
      Font titleFont = Fonts.REGULAR.getFont(8.0F);
      Font countFont = Fonts.REGULAR.getFont(7.0F);
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      ColorRGBA text = Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()));
      String suffix = " of " + this.setting.getValues().stream().filter(v -> !v.isHidden()).count();
      if (this.numberAnim == null) this.numberAnim = new AnimatedNumber(Fonts.MEDIUM.getFont(7.0F), 5.0F, 500L, Easing.BAKEK);
      this.numberAnim.settings(false, text);
      this.numberAnim.update(this.setting.getSelectedValues().size());
      float suffixW = countFont.width(suffix);
      this.numberAnim.pos(x + width - suffixW - this.numberAnim.getWidth(), y + GuiUtility.getMiddleOfBox(countFont.height(), HEADER_HEIGHT));
      this.numberAnim.render(context);
      context.drawRightText(countFont, suffix, x + width, y + GuiUtility.getMiddleOfBox(countFont.height(), HEADER_HEIGHT), text);
      this.drawSettingName(context, titleFont, Localizator.translate(this.setting.getName()), x + 1.0F,
         y + GuiUtility.getMiddleOfBox(titleFont.height(), HEADER_HEIGHT), text,
         width - suffixW - this.numberAnim.getWidth() - 10.0F);
   }

   private void renderSearch(UIContext context, float x, float y, float width) {
      renderSearchControl(context, this.searchField, x, y, width);
   }

   public static void renderSearchControl(UIContext context, TextField field, float x, float y, float width) {
      BorderRadius radius = BorderRadius.all(5.0F);
      boolean hovered = GuiUtility.isHovered(x, y, width, CONTROL_HEIGHT, context);
      if (dev.bkclient.ui.components.SettingControls.cleanOptions()) {
         context.drawRoundedRect(x, y, width, CONTROL_HEIGHT, radius, Colors.getTextColor().withAlpha(hovered ? 24 : 14));
      } else {
      context.drawLiquidGlass(x, y, width, CONTROL_HEIGHT, 2.0F, 0.05F, radius,
         Colors.getBackgroundColor().withAlpha(hovered || field.isFocused() ? 120.0F : 85.0F));
      }
      context.drawRoundedBorder(x, y, width, CONTROL_HEIGHT, 0.05F, radius,
         (field.isFocused() ? Colors.getAccent() : Colors.getTextColor()).withAlpha(field.isFocused() ? 85.0F : 13.0F));
      context.drawIcon(Fonts.ICONS, 'v', x + 5.0F, y + 4.0F, 7.5F,
         Colors.getTextColor().withAlpha(field.isFocused() ? 190.0F : 100.0F));
      field.setTextColor(Colors.getTextColor());
      field.set(x + 13.0F, y + 1.0F, width - 16.0F, CONTROL_HEIGHT - 2.0F);
      field.render(context);
   }

   private void renderRow(UIContext context, SelectSetting.Value value, float x, float y, float width,
                          float listY, float listH) {
      boolean hover = GuiUtility.isHovered(x - 1.0F, y, width + 2.0F, ROW_HEIGHT, context)
         && context.getMouseY() >= listY && context.getMouseY() <= listY + listH;
      boolean currentState = value.isSelected();
      if (currentState != value.isLastState()) {
         value.setCurrentSpriteAnimation(currentState ? value.getEnableSpriteAnimation() : value.getDisableSpriteAnimation());
         value.getCurrentSpriteAnimation().playOnce();
         value.setLastState(currentState);
      }
      value.getCurrentSpriteAnimation().update();
      value.getHoverAnimation().update(hover);
      value.getActiveAnimation().update(value.isSelected());
      if (hover && !value.isAlwaysEnabled()) CursorUtility.set(CursorType.HAND);

      float active = value.getActiveAnimation().getValue();
      float hovered = value.getHoverAnimation().getValue();
      value.getYAnim().setEasing(Easing.BAKEK_SMALLER);
      value.getYAnim().update(y - listY + 3.0F);
      float animatedY = listY + value.getYAnim().getValue();

      if (this.setting.isDraggable()) {
         context.drawTexture(BKClientInit.id("icons/hud/drag.png"), x + 7.0F, animatedY, 6.0F, 6.0F,
            Colors.getTextColor());
         if (GuiUtility.isHovered(x, y + 1.0F, 17.0F, 10.0F, context) || value == this.dragging) {
            CursorUtility.set(CursorType.ARROW_VERTICAL);
         }
      }

      float textX = x + (this.setting.isDraggable() ? 18.0F : 7.0F);
      float textWidth = Math.max(12, width - (textX - x) - 12 - active * 10);
      ScissorUtility.push(context.getMatrices(), textX, animatedY - 1, textWidth, ROW_HEIGHT);
      context.drawText(Fonts.REGULAR.getFont(7), displayName(value), textX, animatedY + .5F,
         Colors.getTextColor().withAlpha(255 * Math.min(1, .75F + .25F * hovered + .25F * active)));
      ScissorUtility.pop();
      if (dev.bkclient.ui.components.SettingControls.cleanOptions()) {
         if (active > 0.001F) dev.bkclient.ui.components.LucideIcons.CHECK.draw(context,
            x + width - 15, animatedY - 0.5F, 9, Colors.getTextColor().mulAlpha(active));
      } else if (active > 0.0F || value.getCurrentSpriteAnimation().isPlaying()) {
         DrawUtility.drawAnimationSprite(
            context.getMatrices(),
            value.getCurrentSpriteAnimation().getCurrentSprite(),
            x + width - 11.0F - active * 2.0F,
            animatedY,
            6.0F,
            6.0F,
            Colors.getTextColor().mulAlpha(0.1F + 0.9F * active)
         );
      }
   }

   private List<SelectSetting.Value> visibleValues() {
      String query = normalize(!this.isSearchVisible() || this.searchField == null ? "" : this.searchField.getBuiltText());
      return this.setting.getValues().stream()
         .filter(v -> !v.isHidden())
         .filter(v -> query.isEmpty() || searchableText(v).contains(query))
         .toList();
   }

   private String searchableText(SelectSetting.Value value) {
      return normalize(value.getName() + " " + value.getDescription() + " " + displayName(value));
   }

   private String displayName(SelectSetting.Value value) {
      String translated = Localizator.translate(value.getName());
      if (translated != null && !translated.isBlank() && !translated.equals(value.getName())) return translated;
      String raw = value.getName();
      int namespace = raw.indexOf(':');
      if (namespace >= 0) raw = raw.substring(namespace + 1);
      String[] words = raw.replace('.', '_').split("_+");
      StringBuilder result = new StringBuilder();
      for (String word : words) {
         if (word.isBlank()) continue;
         if (!result.isEmpty()) result.append(' ');
         result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
      }
      return result.isEmpty() ? value.getName() : result.toString();
   }

   private String normalize(String text) {
      return text == null ? "" : text.toLowerCase(Locale.ROOT).replace('_', ' ').trim();
   }

   private float listHeight(int count) {
      float maxHeight = this.getParent() instanceof Popup popup && popup.isHudSettings() ? 144.0F : MAX_LIST_HEIGHT;
      if (maxVisibleRows > 0) maxHeight = Math.min(maxHeight, maxVisibleRows * ROW_HEIGHT + 8.0F);
      return Math.min(maxHeight, Math.max(20.0F, count * ROW_HEIGHT + 8.0F));
   }

   private float controlsX() { return this.x + 9.0F; }
   private float controlsY() { return this.y + 1.0F + HEADER_HEIGHT; }
   private float contentWidth() { return this.width - 18.0F; }
   private float searchWidth() { return contentWidth(); }
   private float listY() { return controlsY() + (this.isSearchVisible() ? CONTROL_HEIGHT + CONTROL_GAP : 0.0F); }
   private boolean isSearchVisible() {
      return !(this.getParent() instanceof Popup popup && popup.isHudSettings());
   }

   public boolean isScrollAreaHovered(double mouseX, double mouseY) {
      return GuiUtility.isHovered(controlsX(), listY(), contentWidth(), listHeight(visibleValues().size()), mouseX, mouseY);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (button != MouseButton.LEFT) return;
      ensureSearchField();
      float x = controlsX();
      float controlsY = controlsY();
      float searchW = searchWidth();
      if (this.isSearchVisible() && GuiUtility.isHovered(x, controlsY, searchW, CONTROL_HEIGHT, mouseX, mouseY)) {
         this.searchField.onMouseClicked(mouseX, mouseY, button);
         return;
      }
      this.searchField.setFocused(false);

      List<SelectSetting.Value> values = visibleValues();
      float listY = listY();
      float listH = listHeight(values.size());
      if (!GuiUtility.isHovered(x - 1.0F, listY, contentWidth() + 2.0F, listH, mouseX, mouseY)) return;
      float scrollOffset = (float)this.scroll.getValue();
      for (int i = 0; i < values.size(); i++) {
         SelectSetting.Value value = values.get(i);
         float rowY = listY + 4.0F + i * ROW_HEIGHT - scrollOffset;
         if (rowY + ROW_HEIGHT < listY || rowY > listY + listH) continue;
         if (GuiUtility.isHovered(x - 1.0F, rowY, contentWidth() + 2.0F, ROW_HEIGHT, mouseX, mouseY)) {
            if (this.setting.isDraggable() && mouseX <= x + 17.0F) this.dragging = value;
            else value.toggle();
            return;
         }
      }
      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (this.dragging != null && (this.searchField == null || this.searchField.getBuiltText().isBlank())) {
         List<SelectSetting.Value> sorted = new ArrayList<>(this.setting.getValues());
         sorted.sort(java.util.Comparator.comparingDouble(SelectSetting.Value::getYFactor));
         this.setting.getValues().clear();
         this.setting.getValues().addAll(sorted);
         BKClientInit.getInstance().getFileManager().writeFile("client");
      }
      this.dragging = null;
      if (this.searchField != null) this.searchField.onMouseReleased(mouseX, mouseY, button);
      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (isScrollAreaHovered(mouseX, mouseY)) this.scroll.scroll(verticalAmount);
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (isSearchFocused() && prefixCompletion && (keyCode == 258 || keyCode == 257)) {
         var suggestion = suggestion();
         if (suggestion != null) {
            if (keyCode == 257) {
               if (!suggestion.isSelected()) suggestion.toggle();
               searchField.setText("");
            } else searchField.setText(suggestion.getName());
            scroll.reset();
            return;
         }
      }
      if (this.isSearchVisible() && this.searchField != null) this.searchField.onKeyPressed(keyCode, scanCode, modifiers);
      if ((this.searchField == null || !this.searchField.isFocused())
         && isScrollAreaHovered(GuiUtility.getMouse().getX(), GuiUtility.getMouse().getY())) {
         this.scroll.onKeyPressed(keyCode);
      }
      super.onKeyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      return this.isSearchVisible() && this.searchField != null && this.searchField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
   }

   @Override
   public void drawSplit(UIContext context) {
      context.drawRect(this.x, this.y + this.height, this.width, 0.5F, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   public float getHeight() {
      if (!this.setting.isVisible()) return 0.0F;
      this.height = HEADER_HEIGHT + (this.isSearchVisible() ? CONTROL_HEIGHT + CONTROL_GAP : 0.0F) + listHeight(visibleValues().size()) + 4.0F;
      return this.height;
   }
}
