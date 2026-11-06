package dev.bkclient.ui.menu.spotlight.components;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.ui.menu.spotlight.SpotlightScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;

import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.ModeSettingComponent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.math.MathUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.ScissorUtility;

public class SpotlightSettings extends CustomComponent {
   private static final float SETTINGS_RADIUS = 16.0F;

   private final SpotlightModule module;
   private final dev.bkclient.ui.menu.MenuScreen hostScreen;
   private List<MenuSettingComponent> components = new ArrayList<>();
   private final Animation animation = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   private boolean showing;
   private final ScrollHandler scrollHandler = new ScrollHandler();
   private float dragX;
   private float dragY;
   private boolean drag;
   private final Animation enableAnimation = new Animation(250L, Easing.BAKEK);
   private final Animation circleOpacityAnimation = new Animation(250L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private boolean pinned;
   private Setting searchSetting;
   private boolean searchScrollPending;
   private long searchHighlightUntil;

   public void focusSearchSetting(Setting setting) {
      this.searchSetting = setting;
      this.searchScrollPending = true;
      this.searchHighlightUntil = System.currentTimeMillis() + 2500;
   }

   private float searchOffset(MenuSettingComponent component) {
      if (this.searchSetting == null) return -1;
      if (component instanceof dev.bkclient.ui.menu.clickgui.components.TargetStylingSection styling) return styling.searchOffset(this.searchSetting);
      return component.getSetting() == this.searchSetting ? 0 : -1;
   }
   private boolean toggleDrag;
   private float toggleDragProgress;
   private float toggleDragStartX;
   private final Animation toggleDragAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   public SpotlightSettings(SpotlightModule module, float x, float y, float width) {
      this(module, x, y, width, null);
   }

   protected SpotlightSettings(SpotlightModule module, float x, float y, float width, dev.bkclient.ui.menu.MenuScreen hostScreen) {
      this.hostScreen = hostScreen;
      this.module = module;
      this.x = x;
      this.y = y;
      this.width = width;
      this.showing = true;

      for (Setting setting : module.getModule().getDisplaySettings()) {
         MenuSettingComponent settingComponent = GuiUtility.settinge(setting, this);
         if (settingComponent != null) {
            this.components.add(settingComponent);
         }
      }
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.animation.setDuration(this.showing ? 400L : 300L);
      var ss = this.module.getScreen();
      this.animation.update(this.showing && (this.hostScreen != null ? !this.hostScreen.isClosing()
         : mc.currentScreen instanceof SpotlightScreen || ss != null && !ss.isClosing()));
      this.scrollHandler.update();
      if (this.drag) {
         this.x = context.getMouseX() - this.dragX;
         this.y = context.getMouseY() - this.dragY;
      }

      float alpha = Math.min(1.0F, this.animation.getValue());
      var menuScreen = BKClientInit.getInstance().getMenuScreen();
      boolean check = menuScreen == null || menuScreen.getMenuAnimation().getValue()
         == menuScreen.getMenuAnimation().getTargetValue();
      boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();

      this.animation.setEasing(this.showing ? Easing.QUARTIC_OUT : Easing.BAKEK_BACK);

      float x = MathUtility.interpolate(this.module.getX() + this.module.getWidth() + 10.0F, this.x, alpha);
      float y = MathUtility.interpolate(this.module.getY(), this.y, alpha);
      float width = MathUtility.interpolate(152.0F, this.width, alpha);
      float height = MathUtility.interpolate(this.module.getHeight(), this.height, alpha);
      if (!this.showing || !check || !(mc.currentScreen instanceof SpotlightScreen) && mc.currentScreen != this.hostScreen) {
         x = this.x;
         y = this.y;
         width = this.width;
         height = this.height;
      }

      if (!this.showing || !check) {
         RenderUtility.scale(context.getMatrices(), x + width / 2.0F, y + height / 2.0F, 0.5F + 0.5F * this.animation.getValue());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      }

      if (dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.active()) {
         context.drawClientRect(x, y, width, height, alpha, 0, SETTINGS_RADIUS);
      } else {
      context.drawBlurredRect(
         x, y, width, height,
         25.0F, SETTINGS_RADIUS,
         BorderRadius.all(SETTINGS_RADIUS),
         new ColorRGBA(0, 0, 0, (int)(80 * alpha))
      );

      if (Interface.showGlass()) {
         context.drawLiquidGlass(
            x, y, width, height,
            2.0F, 0.08F,
            BorderRadius.all(SETTINGS_RADIUS),
            ColorRGBA.WHITE.withAlpha((int)(255 * alpha * Interface.glass()))
         );
         context.drawRoundedRect(
            x, y, width, height,
            BorderRadius.all(SETTINGS_RADIUS),
            Colors.getBackgroundColor().withAlpha((int)(255 * (0.8F - 0.3F * Interface.glass()) * alpha))
         );
      } else {
         context.drawBlurredRect(
            x, y, width, height,
            45.0F, SETTINGS_RADIUS,
            BorderRadius.all(SETTINGS_RADIUS),
            ColorRGBA.WHITE.withAlpha((int)(255 * alpha * Interface.minimalizm()))
         );
         context.drawRoundedRect(
            x, y, width, height,
            BorderRadius.all(SETTINGS_RADIUS),
            Colors.getBackgroundColor().withAlpha((int)(200 * alpha))
         );
      }

      }

      if (this.showing) {
         context.drawText(
            Fonts.MEDIUM.getFont(7.0F + 2.0F * alpha),
            this.module.getModule().getName(),
            x + 8.0F + 2.0F * alpha,
            y + 8.0F + 2.0F * alpha,
            Colors.getTextColor()
         );
      } else {
         context.drawText(Fonts.MEDIUM.getFont(9.0F), this.module.getModule().getName(), x + 10.0F, y + 10.0F, Colors.getTextColor());
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

      float pinX = x + width - 36.0F;
      float pinY = y + 8.0F;
      boolean pinHovered = GuiUtility.isHovered(pinX, pinY, 10.0F, 10.0F, context);
      ColorRGBA pinBg = pinHovered
         ? new ColorRGBA(255, 255, 255, 30)
         : new ColorRGBA(0, 0, 0, 0);
      context.drawSquircle(pinX - 3.0F, pinY - 3.0F, 16.0F, 16.0F, 3.0F, BorderRadius.all(8.0F), pinBg);
      context.drawText(
         Fonts.MEDIUM.getFont(6.0F), this.pinned ? "\u25C9" : "\u25CB",
         pinX + 2.0F, pinY,
         Colors.getAccent().withAlpha(pinHovered ? (int)(255 * alpha) : (int)(180 * alpha))
      );
      if (pinHovered) CursorUtility.set(CursorType.HAND);

      float closeX = x + width - 18.0F;
      float closeY = y + 8.0F;
      boolean closeHovered = GuiUtility.isHovered(closeX, closeY, 10.0F, 10.0F, context);
      ColorRGBA closeBg = closeHovered
         ? new ColorRGBA(255, 255, 255, 30)
         : new ColorRGBA(0, 0, 0, 0);
      context.drawSquircle(closeX - 3.0F, closeY - 3.0F, 16.0F, 16.0F, 3.0F, BorderRadius.all(8.0F), closeBg);
      context.drawText(
         Fonts.MEDIUM.getFont(6.0F), "\u2715",
         closeX, closeY,
         Colors.getTextColor().mulAlpha(closeHovered ? 0.8F * alpha : 0.5F * alpha)
      );
      if (closeHovered) CursorUtility.set(CursorType.HAND);

      if (Interface.showMinimalizm()) {
         context.drawRect(x, y + 24.0F, width, 4.0F, Colors.getSeparatorColor().withAlpha(Colors.getSeparatorColor().getAlpha() * Interface.minimalizm()));
      }

      float settingsY = 28.0F;
      float offset = 0.0F;
      ScissorUtility.push(context.getMatrices(), x, y + 28.0F, width, Math.max(0, height - 28.0F - 5.0F));

      this.circleOpacityAnimation.update(this.module.getModule().isEnabled() ? 1.0F : 0.75F);
      if (!this.toggleDrag) {
         this.enableAnimation.update(this.module.getModule().isEnabled() ? 1.0F : 0.0F);
      }

      float settingY = (float)(y + 28.0F - this.scrollHandler.getValue());
      float checkWidth = 26.0F;
      float checkHeight = 10.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float headerHeight = 19.0F;
      String name = Localizator.translate("enabled");

      context.drawFadeoutText(
         nameFont,
         name.substring(0, 1).toUpperCase() + name.substring(1),
         x + leftPadding,
         settingY + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight) - 0.5F,
         Colors.getTextColor().withAlpha((int)(255 * (0.75F + 0.25F * this.enableAnimation.getValue()))),
         0.7F, 0.99F,
         width - checkWidth - 20.0F
      );

      this.toggleDragAnim.update(this.toggleDrag ? 1.0F : 0.0F);
      float trackX2 = x + width - checkWidth - 10.0F;
      dev.bkclient.ui.components.SettingControls.toggle(context, trackX2, settingY + 3.0F,
         this.toggleDrag ? this.toggleDragProgress : this.enableAnimation.getValue(), this.toggleDragAnim.getValue(), alpha);

      if (this.toggleDrag) {
         this.toggleDragProgress = (float)Math.max(0.0F, Math.min(1.0F, (context.getMouseX() - trackX2) / checkWidth));
      }

      float separatorHeight = 0.5F;
      boolean drawSep = BKClientInit.getInstance().getThemeManager().getCurrentTheme() == null
         || BKClientInit.getInstance().getThemeManager().getCurrentTheme().isSeparators();
      if (drawSep) {
         context.drawRect(x, settingY + 18.0F, width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
      }

      offset += 18.0F;

      for (MenuSettingComponent settingComponent : this.components) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, settingComponent.getOpacity() * alpha);
         settingComponent.getVisibilityAnimation().update(settingComponent.getSetting().isVisible() ? 1.0F : 0.0F);
         settingComponent.setX(x);
         settingComponent.setY((float)(y + settingsY + offset - this.scrollHandler.getValue()));
         settingComponent.setWidth(width);
         if (GuiUtility.isHovered(
            x, y - settingComponent.getHeight(), width, height + settingComponent.getHeight(), settingComponent.getX(), settingComponent.getY()
         )) {
            context.pushMatrix();
            context.getMatrices()
               .translate(0.0F, (-settingComponent.getHeight() + settingComponent.getHeight() * settingComponent.getOpacity()) / 2.0F, 0.0F);
            float found = searchOffset(settingComponent);
            float fade = found >= 0 ? Math.clamp((this.searchHighlightUntil - System.currentTimeMillis()) / 600F, 0, 1) : 0;
            settingComponent.setSearchLabelHighlight(this.searchSetting, fade);
            settingComponent.render(context);
            context.popMatrix();
         }

         offset += settingComponent.getHeight() * settingComponent.getOpacity();
      }

      ScissorUtility.pop();

      for (MenuSettingComponent settingComponent : this.components) {
         if (settingComponent instanceof ModeSettingComponent modeSetting && settingComponent.getOpacity() > 0.0F) {
            // A hidden setting at the end of the body leaves the shader alpha at zero.
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * settingComponent.getOpacity());
            modeSetting.renderOverlay(context);
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
      this.height = Math.min(this.maxPanelHeight(), offset + 28.0F + 5.0F);
      this.scrollHandler.setMax(-offset + height - 24.0F - 4.0F - 5.0F);
      if (this.searchScrollPending && alpha >= .99F) {
         float targetOffset = 18;
         for (MenuSettingComponent component : this.components) {
            if (!component.getSetting().isVisible()) continue;
            float found = searchOffset(component);
            if (found >= 0) {
               this.scrollHandler.setTargetValue(-Math.min(targetOffset + found, Math.max(0, -this.scrollHandler.getMax())));
               this.searchScrollPending = false;
               break;
            }
            targetOffset += component.getHeight() * component.getOpacity();
         }
      }
      if (!this.showing || !check) {
         RenderUtility.end(context.getMatrices());
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public boolean hasOpenDropdown() {
      for (MenuSettingComponent component : this.components) {
         if (component instanceof ModeSettingComponent ms && ms.isDropdownVisible()) {
            return true;
         }
      }
      return false;
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      // The overlay owns the click, including clicks outside it used to dismiss it.
      for (MenuSettingComponent component : this.components) {
         if (component instanceof ModeSettingComponent mode && mode.isDropdownVisible()) {
            mode.onMouseClicked(mouseX, mouseY, button);
            return;
         }
      }
      boolean bodyHovered = GuiUtility.isHovered(this.x, this.y + 28, this.width, Math.max(0, this.height - 33), mouseX, mouseY);
      boolean pass = this.isHovered(mouseX, mouseY) || button != MouseButton.LEFT || this.hasOpenDropdown();
      for (MenuSettingComponent component : this.components) {
         boolean acceptsClick = this.hostScreen == null ? pass : bodyHovered
            || component instanceof ModeSettingComponent mode && mode.isDropdownVisible();
         if (component.getOpacity() != 0.0F && acceptsClick) {
            component.onMouseClicked(mouseX, mouseY, button);
         }
      }

      if ((this.hostScreen == null || button == MouseButton.LEFT) && GuiUtility.isHovered(this.x + this.width - 36.0F, this.y + 8.0F, 10.0, 10.0, mouseX, mouseY)) {
         this.pinned = !this.pinned;
         return;
      }

      if ((this.hostScreen == null || button == MouseButton.LEFT) && GuiUtility.isHovered(this.x + this.width - 18.0F, this.y + 8.0F, 10.0, 10.0, mouseX, mouseY)) {
         this.showing = false;
         return;
      }

      if ((this.hostScreen == null || button == MouseButton.LEFT) && GuiUtility.isHovered(this.x, this.y, this.width, 24.0, mouseX, mouseY)) {
         this.drag = true;
         this.dragX = (float)(mouseX - this.x);
         this.dragY = (float)(mouseY - this.y);
      }

      float toggleH = 10.0F;
      float toggleX = this.x + this.width - (this.hostScreen == null ? 22.0F : 26.0F) - 10.0F;
      float settingY = (float)(this.y + 28.0F - this.scrollHandler.getValue());
      if ((this.hostScreen == null || bodyHovered) && GuiUtility.isHovered(toggleX, settingY + 3.0F, 26.0F, toggleH, mouseX, mouseY) && button == MouseButton.LEFT) {
         this.toggleDrag = true;
         this.toggleDragProgress = this.module.getModule().isEnabled() ? 1.0F : 0.0F;
         this.toggleDragStartX = (float) mouseX;
         return;
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      for (MenuSettingComponent component : this.components) {
         if (component.getOpacity() != 0.0F) {
            component.onMouseReleased(mouseX, mouseY, button);
         }
      }
      this.drag = false;
      if (this.toggleDrag) {
         if (button == MouseButton.LEFT) {
            boolean dragged = Math.abs((float) mouseX - this.toggleDragStartX) > 3.0F;
            if (dragged) {
                this.module.getModule().setEnabled(this.toggleDragProgress > 0.5F, false);
                this.enableAnimation.reset(this.module.getModule().isEnabled() ? 1.0F : 0.0F);
            } else {
               this.module.getModule().toggle();
            }
         }
         this.toggleDrag = false;
      }
      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.hostScreen != null && this.hasOpenDropdown()) {
         for (MenuSettingComponent component : this.components) {
            if (component instanceof ModeSettingComponent mode && mode.isDropdownVisible()) mode.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
         }
         return;
      }
      for (MenuSettingComponent component : this.components) {
         if (component.getOpacity() != 0.0F) {
            component.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
         }
      }
      if (this.isHovered(mouseX, mouseY)) {
         this.scrollHandler.scroll(verticalAmount);
      }
      super.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      for (MenuSettingComponent component : this.components) {
         if (component.getOpacity() != 0.0F) {
            component.onKeyPressed(keyCode, scanCode, modifiers);
         }
      }
      super.onKeyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      boolean consumed = false;
      for (MenuSettingComponent component : this.components) {
         if (component.getOpacity() != 0.0F) consumed |= component.charTyped(chr, modifiers);
      }
      return this.hostScreen != null ? consumed : super.charTyped(chr, modifiers);
   }

   @Override
   public float getHeight() {
      float offset = 18.0F;
      for (MenuSettingComponent settingComponent : this.components) {
         offset += settingComponent.getHeight() * settingComponent.getOpacity();
      }
      this.height = Math.min(this.maxPanelHeight(), offset + 28.0F + 5.0F);
      return this.height;
   }

   protected float maxPanelHeight() { return 200.0F; }

   public SpotlightModule getModule() { return this.module; }
   public List<MenuSettingComponent> getComponents() { return this.components; }
   public Animation getAnimation() { return this.animation; }
   public boolean isShowing() { return this.showing; }
   public void setShowing(boolean showing) { this.showing = showing; }
   public boolean isPinned() { return this.pinned; }
}
