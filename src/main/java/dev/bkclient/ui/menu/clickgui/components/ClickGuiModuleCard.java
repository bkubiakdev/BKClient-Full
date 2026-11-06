package dev.bkclient.ui.menu.clickgui.components;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.ModeSettingComponent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.render.ScissorUtility;

import java.util.ArrayList;
import java.util.List;

/** A BKClient-styled module card used by the BK-shaped ClickGUI layout. */
public class ClickGuiModuleCard extends CustomComponent {
   private static final float HEADER_HEIGHT = 24.0F;
   private static final float CARD_RADIUS = 8.0F;
   private static final float PADDING = 7.0F;
   private static final float SETTINGS_BOTTOM_PADDING = 5.0F;

   private final Module module;
   private final java.util.function.Consumer<ClickGuiModuleCard> settingsOpener;
   private boolean externalSettingsShowing;
   private final List<MenuSettingComponent<?>> settings = new ArrayList<>();
   private final Animation hoverAnimation = new Animation(150L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation enabledAnimation = new Animation(250L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation expandAnimation = new Animation(280L, Easing.BAKEK_SMALLER);
   private final Animation clickAnimation = new Animation(80L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation dragAnimation = new Animation(200L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation favoriteAnimation = new Animation(220L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation favoriteHover = new Animation(140L, Easing.QUARTIC_OUT);
   private final Animation favoritePulse = new Animation(280L, Easing.QUARTIC_OUT);
   private boolean deferHeaderDecorations;
   private boolean expanded;
   private boolean binding;
   private boolean toggleDrag;
   private float toggleDragProgress;
   private float toggleDragStartX;
   private float toggleX;
   private float toggleY;

   public ClickGuiModuleCard(Module module) { this(module, null); }

   public ClickGuiModuleCard(Module module, java.util.function.Consumer<ClickGuiModuleCard> settingsOpener) {
      this.settingsOpener = settingsOpener;
      this.module = module;
      this.favoriteAnimation.setValue(dev.bkclient.systems.modules.ModuleFavorites.contains(module.getName()) ? 1.0F : 0.0F);
      this.height = HEADER_HEIGHT;
      if (settingsOpener == null) for (Setting setting : module.getDisplaySettings()) {
         MenuSettingComponent<?> component = GuiUtility.settinge(setting, this);
         if (component != null) {
            this.settings.add(component);
         }
      }
   }

   /** The screen submits matching tracks, thumbs and badges in separate batches. */
   public void renderForBatch(UIContext context) {
      this.deferHeaderDecorations = true;
      try {
         this.render(context);
      } finally {
         this.deferHeaderDecorations = false;
      }
   }

   @Override
   protected void renderComponent(UIContext context) {
      boolean headerHovered = GuiUtility.isHovered(this.x, this.y, this.width, HEADER_HEIGHT, context);
      this.hoverAnimation.update(headerHovered);
      this.enabledAnimation.setEasing(Easing.QUARTIC_OUT);
      if (!this.toggleDrag) {
         this.enabledAnimation.update(this.module.isEnabled());
      }
      this.expandAnimation.update(this.expanded);

      float settingsHeight = this.updateSettingsLayout();
      this.height = HEADER_HEIGHT + settingsHeight * this.expandAnimation.getValue();
      float open = this.expandAnimation.getValue();

      boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
         && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
      float clickScale = 1.0F - 0.015F * this.clickAnimation.getValue();
      if (clickScale < 0.999F) {
         dev.bkclient.utility.render.RenderUtility.scale(
            context.getMatrices(), this.x + this.width / 2.0F, this.y + this.height / 2.0F, clickScale
         );
      }
      float hover = this.hoverAnimation.getValue();
      ColorRGBA cardColor = dark
         ? new ColorRGBA(255.0F, 255.0F, 255.0F, 12.0F * hover)
         : new ColorRGBA(0.0F, 0.0F, 0.0F, 5.0F * hover);
      if ((cardColor.getRGB() >>> 24) != 0) {
         context.drawSquircle(this.x, this.y, this.width, this.height, 4.0F, BorderRadius.all(CARD_RADIUS), cardColor);
      }
      if (clickScale < 0.999F) {
         dev.bkclient.utility.render.RenderUtility.end(context.getMatrices());
      }

      float centerY = this.y + HEADER_HEIGHT / 2.0F;
      this.toggleX = this.x + PADDING;
      this.toggleY = centerY - 6.0F;
      this.dragAnimation.update(this.toggleDrag);
      if (!this.deferHeaderDecorations) {
         this.drawToggle(context, this.toggleX, this.toggleY, this.enabledAnimation.getValue());
      }

      Font nameFont = Fonts.MEDIUM.getFont(7.0F);
      float nameX = this.toggleX + 26.0F + 7.0F;
      float availableNameWidth = this.width - (nameX - this.x) - 34.0F;
      String keyName = this.module.getKey() == -1 ? "" : TextUtility.getKeyName(this.module.getKey());
      float keyWidth = keyName.isEmpty() ? 0.0F : Math.max(12.0F, Fonts.MEDIUM.getFont(6.0F).width(keyName) + 7.0F);
      if (keyWidth > 0.0F) {
         availableNameWidth -= keyWidth + 6.0F;
      }

      float safeNameWidth = Math.max(8.0F, availableNameWidth);
      ScissorUtility.push(context.getMatrices(), nameX, this.y, safeNameWidth, HEADER_HEIGHT);
      context.drawText(
         nameFont,
         this.binding ? Localizator.translate("menu.binding") : this.module.getName(),
         nameX,
         centerY - nameFont.height() / 2.0F,
         Colors.getTextColor().withAlpha(255.0F * (0.7F + 0.3F * hover))
      );
      ScissorUtility.pop();

      if (!this.deferHeaderDecorations) {
         this.renderKeyBadgeBackground(context);
         this.renderHeaderBadges(context);
         this.renderFavoriteBadge(context);
      }

      if (headerHovered) {
         CursorUtility.set(CursorType.HAND);
      }

      if (open > 0.01F && settingsHeight > 0.0F) {
         float separatorY = this.y + HEADER_HEIGHT - 0.5F;
         context.drawRect(
            this.x + PADDING,
            separatorY,
            this.width - PADDING * 2.0F,
            0.5F,
            Colors.getTextColor().withAlpha(5.1F * open)
         );

         ScissorUtility.push(
            context.getMatrices(),
            this.x,
            this.y + HEADER_HEIGHT,
            this.width,
            Math.max(0.0F, this.height - HEADER_HEIGHT)
         );
         for (MenuSettingComponent<?> component : this.settings) {
            if (component.getOpacity() > 0.01F) {
               component.render(context);
            }
         }
         ScissorUtility.pop();
      }
   }

   public void renderKeyBadgeBackground(UIContext context) {
      if (this.module.getKey() == -1) return;
      String key = TextUtility.getKeyName(this.module.getKey());
      if (key.isEmpty()) return;
      float badgeWidth = Math.max(12.0F, Fonts.MEDIUM.getFont(6.0F).width(key) + 7.0F);
      context.drawRoundedRect(this.x + this.width - 34.0F - badgeWidth,
         this.y + HEADER_HEIGHT / 2.0F - 6.0F, badgeWidth, 12.0F, BorderRadius.all(6.0F),
         Colors.getTextColor().withAlpha(25.0F + 10.0F * this.hoverAnimation.getValue()));
   }

   public void renderHeaderBadges(UIContext context) {
      float centerY = this.y + HEADER_HEIGHT / 2.0F;
      float hover = this.hoverAnimation.getValue();
      float open = this.expandAnimation.getValue();
      String keyName = this.module.getKey() == -1 ? "" : TextUtility.getKeyName(this.module.getKey());
      float keyWidth = keyName.isEmpty() ? 0.0F : Math.max(12.0F, Fonts.MEDIUM.getFont(6.0F).width(keyName) + 7.0F);
      if (keyWidth > 0.0F) {
         dev.bkclient.ui.components.BadgeText.draw(context,
            Fonts.MEDIUM.getFont(6.0F), keyName, this.x + this.width - 34.0F - keyWidth / 2.0F,
            centerY,
            Colors.getTextColor().mulAlpha(0.85F + 0.15F * hover)
         );
      }

      Font arrowFont = Fonts.MEDIUM.getFont(9.0F);
      context.drawCenteredText(
         arrowFont,
         (this.expanded || this.externalSettingsShowing) ? "-" : "+",
         this.x + this.width - 10.0F,
         centerY - arrowFont.height() / 2.0F,
         ((this.expanded || this.externalSettingsShowing) ? Colors.getAccent() : Colors.getTextColor()).mulAlpha(0.6F + 0.4F * open)
      );

   }

   public void renderFavoriteBadge(UIContext context) {
      float centerX = this.x + this.width - 26.0F;
      float centerY = this.y + HEADER_HEIGHT / 2.0F;
      boolean hovered = GuiUtility.isHovered(centerX - 8, this.y, 16, HEADER_HEIGHT, context);
      this.favoriteHover.update(hovered);
      this.favoriteAnimation.update(dev.bkclient.systems.modules.ModuleFavorites.contains(this.module.getName()));
      float selected = this.favoriteAnimation.getValue();
      float hover = this.favoriteHover.getValue();
      float pulse = this.favoritePulse.update(0.0F);
      float size = 10.0F + 1.0F * hover + 1.8F * pulse;
      ColorRGBA gold = new ColorRGBA(255, 203, 82);
      ColorRGBA tint = Colors.getTextColor().mix(gold, selected);
      if (hovered) CursorUtility.set(CursorType.HAND);
      context.drawTexture(BKClientInit.id("textures/gui/favorite_star.png"),
         centerX - size / 2, centerY - size / 2, size, size,
         tint.mulAlpha((0.6F + 0.4F * hover) * (1.0F - selected)));
      context.drawTexture(BKClientInit.id("textures/gui/favorite_star_fill.png"),
         centerX - size / 2, centerY - size / 2, size, size, gold.mulAlpha(selected));
   }

   private float updateSettingsLayout() {
      float offset = 2.0F;
      for (MenuSettingComponent<?> component : this.settings) {
         component.getVisibilityAnimation().update(component.getSetting().isVisible());
         component.setX(this.x);
         component.setY(this.y + HEADER_HEIGHT + offset);
         component.setWidth(this.width);
         offset += component.getHeight() * component.getOpacity();
      }
      return this.settings.isEmpty() ? 0.0F : offset + SETTINGS_BOTTOM_PADDING;
   }

   private void drawToggle(UIContext context, float x, float y, float value) {
      this.renderToggleTrack(context);
      this.renderToggleThumb(context);
   }

   public boolean hasGlassThumb() {
      return this.toggleDrag || this.dragAnimation.getValue() > 0.0F;
   }

   public void renderToggleTrack(UIContext context) {
      dev.bkclient.ui.components.SettingControls.toggleTrack(context, this.toggleX, this.toggleY,
         this.toggleDrag ? this.toggleDragProgress : this.enabledAnimation.getValue(), 1.0F);
   }

   public void renderToggleThumb(UIContext context) {
      dev.bkclient.ui.components.SettingControls.toggleThumb(context, this.toggleX, this.toggleY,
         this.toggleDrag ? this.toggleDragProgress : this.enabledAnimation.getValue(), this.dragAnimation.getValue(), 1.0F);
      if (this.toggleDrag) {
         this.toggleDragProgress = (float)Math.max(0.0F, Math.min(1.0F, (context.getMouseX() - this.toggleX) / 26.0F));
      }
   }

   public void renderOverlay(UIContext context) {
      if (this.expandAnimation.getValue() <= 0.01F) {
         return;
      }
      for (MenuSettingComponent<?> component : this.settings) {
         if (component instanceof ModeSettingComponent mode && component.getOpacity() > 0.01F) {
            mode.renderOverlay(context);
         }
      }
   }

   public boolean hasOpenDropdown() {
      for (MenuSettingComponent<?> component : this.settings) {
         if (component instanceof ModeSettingComponent mode && mode.isDropdownVisible()) {
            return true;
         }
      }
      return false;
   }

   public void onCardMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.expandAnimation.getValue() > 0.01F) {
         for (MenuSettingComponent<?> component : this.settings) {
            if (component.getOpacity() > 0.01F) {
               component.onMouseClicked(mouseX, mouseY, button);
            }
         }
      }

      if (!GuiUtility.isHovered(this.x, this.y, this.width, HEADER_HEIGHT, mouseX, mouseY)) {
         return;
      }
      if (button == MouseButton.LEFT && GuiUtility.isHovered(this.x + this.width - 34, this.y, 16, HEADER_HEIGHT, mouseX, mouseY)) {
         this.favoritePulse.setValue(1.0F);
         dev.bkclient.systems.modules.ModuleFavorites.toggle(this.module.getName());
         BKClientInit.getInstance().getFileManager().writeFile("client");
         return;
      }
      this.clickAnimation.reset(0.0F);
      this.clickAnimation.setTargetValue(1.0F);
      if (button == MouseButton.MIDDLE) {
         this.binding = true;
      } else if (button == MouseButton.RIGHT) {
         this.openSettings();
      } else if (button == MouseButton.LEFT) {
         if (GuiUtility.isHovered(this.toggleX, this.toggleY, 26.0F, 10.0F, mouseX, mouseY)) {
            this.toggleDrag = true;
            this.toggleDragProgress = this.module.isEnabled() ? 1.0F : 0.0F;
            this.toggleDragStartX = (float)mouseX;
         } else if (mouseX >= this.x + this.width - 18.0F) {
            this.openSettings();
         } else {
            this.module.toggle();
         }
      }
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      for (MenuSettingComponent<?> component : this.settings) {
         if (component.getOpacity() > 0.01F) {
            component.onMouseReleased(mouseX, mouseY, button);
         }
      }
      if (this.toggleDrag && button == MouseButton.LEFT) {
         boolean dragged = Math.abs((float)mouseX - this.toggleDragStartX) > 3.0F;
         if (dragged) {
            this.module.setEnabled(this.toggleDragProgress > 0.5F, false);
         } else {
            this.module.toggle();
         }
         this.toggleDrag = false;
         this.enabledAnimation.setValue(this.toggleDragProgress);
      }
   }

   @Override
   public void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      for (MenuSettingComponent<?> component : this.settings) {
         if (component.getOpacity() > 0.01F) {
            component.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
         }
      }
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.binding) {
         this.module.setKey(keyCode == 256 || keyCode == 261 ? -1 : keyCode);
         this.binding = false;
         return;
      }
      for (MenuSettingComponent<?> component : this.settings) {
         if (component.getOpacity() > 0.01F) {
            component.onKeyPressed(keyCode, scanCode, modifiers);
         }
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      for (MenuSettingComponent<?> component : this.settings) {
         if (component.getOpacity() > 0.01F && component.charTyped(chr, modifiers)) {
            return true;
         }
      }
      return false;
   }

   private void openSettings() {
      if (this.settingsOpener != null) this.settingsOpener.accept(this);
      else this.expanded = !this.expanded;
   }

   public void setExternalSettingsShowing(boolean showing) { this.externalSettingsShowing = showing; }

   public Module getModule() {
      return this.module;
   }

   public boolean isBinding() {
      return this.binding;
   }

   public void clearBinding() {
      this.binding = false;
   }

   public boolean hasOpenPlayerCardPanel() { return false; }

   public void renderPlayerCardPanel(UIContext context, float x, float y, float width, float height, float screenAlpha) {  }

   public boolean isPlayerCardPanelHovered(double mouseX, double mouseY) { return false; }

   public void onPlayerCardPanelClicked(double mouseX, double mouseY, MouseButton button) {  }

   public void onPlayerCardPanelReleased(double mouseX, double mouseY, MouseButton button) {  }
}
