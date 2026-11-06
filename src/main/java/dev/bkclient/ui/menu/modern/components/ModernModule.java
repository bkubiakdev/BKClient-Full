package dev.bkclient.ui.menu.modern.components;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import lombok.Generated;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.modules.other.Sounds;
import dev.bkclient.ui.menu.dropdown.DropDownScreen;
import dev.bkclient.ui.menu.modern.ModernCategory;
import dev.bkclient.ui.menu.modern.ModernScreen;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.animation.types.ColorAnimation;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.render.obj.Rect;
import dev.bkclient.utility.sounds.ClientSounds;

public class ModernModule extends CustomComponent {
   private final Animation visible = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation offset = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation hoverAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation enableAnimation = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Module module;
   private final ModernCategory category;
   private boolean bindingMode;
   private final Animation shakeAnimation = new Animation(100L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation blockingAnimation = new Animation(500L, Easing.FIGMA_EASE_IN_OUT);
   private final ColorAnimation blockingColorAnimation = new ColorAnimation(500L, ColorRGBA.WHITE, Easing.FIGMA_EASE_IN_OUT);
   private boolean blocking;
   private boolean shakeValue;

   @Override
   protected void renderComponent(UIContext context) {
      this.enableAnimation.setEasing(Easing.QUARTIC_OUT);
      this.enableAnimation.update(this.module.isEnabled());
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      this.blockingAnimation.update(this.blocking);
      this.blockingColorAnimation.update(this.blocking ? new ColorRGBA(255.0F, 150.0F, 150.0F) : BKClientInit.getInstance().getThemeManager().getTextColor());
      this.shakeAnimation.update(this.blocking ? (this.shakeValue ? 1.0F : -1.0F) : 0.0F);
      if (this.blockingAnimation.getValue() == 1.0F) {
         this.blocking = false;
      }

      if (this.shakeAnimation.getValue() == 1.0F) {
         this.shakeValue = false;
      }

      if (this.shakeAnimation.getValue() == -1.0F) {
         this.shakeValue = true;
      }

      boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
      context.drawSquircle(
         this.x,
         this.y,
         this.width,
         this.height,
         5.0F,
         BorderRadius.all(6.0F),
         (!dark ? Colors.getAdditionalColor().mulAlpha(0.3F) : Colors.getBackgroundColor().mulAlpha(0.3F)).mulAlpha(this.visible.getValue())
      );
   }

   public void renderRounds(UIContext context) {
      context.drawRoundedRect(
         this.x + this.width - 25.0F,
         this.y + 10.5F,
         14.5F,
         7.0F,
         BorderRadius.all(2.75F),
         Colors.getAdditionalColor().mix(Colors.getAccent(), this.enableAnimation.getValue()).mulAlpha(this.visible.getValue())
      );
   }

   public void renderInto(UIContext context) {
      context.drawRoundedRect(
         this.x + this.width - 25.0F + 1.0F + 5.0F * this.enableAnimation.getValue(),
         this.y + 11.5F,
         7.5F,
         5.0F,
         BorderRadius.all(1.75F),
         Colors.WHITE.mulAlpha(this.visible.getValue())
      );
   }

   public void renderMedium(UIContext context) {
      int key = this.module.getKey();
      Object bindingText = key == -1 ? Localizator.translate("menu.binding") : Localizator.translate("key") + ": " + TextUtility.getKeyName(key);
      context.drawText(
         Fonts.MEDIUM.getFont(7.0F),
         (String)(this.bindingMode ? bindingText : this.module.getName()),
         this.x + 7.0F + this.shakeAnimation.getValue(),
         this.y + 8.0F,
         this.blockingColorAnimation
            .getColor()
            .mulAlpha(RenderSystem.getShaderColor()[3] * 0.75F + 0.25F * this.enableAnimation.getValue() + 0.25F * this.hoverAnimation.getValue())
            .mulAlpha(this.visible.getValue())
      );
   }

   public void renderRegular(UIContext context) {
      context.drawText(
         Fonts.REGULAR.getFont(6.0F),
         this.module.getDescription(),
         this.x + 7.0F,
         this.y + 16.0F,
         Colors.getTextColor().mulAlpha(0.5F * this.visible.getValue())
      );
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.bindingMode && button != MouseButton.LEFT && button != MouseButton.RIGHT) {
         this.module.setKey(button.getButtonIndex());
         this.bindingMode = false;
      } else {
         switch (button) {
            case LEFT:
               this.module.toggle();
               break;
            case MIDDLE:
               for (ModernModule comp : this.category.getModules()) {
                  comp.setBindingMode(false);
               }

               this.bindingMode = true;
               break;
            case RIGHT:
               this.open();
         }
      }
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.bindingMode) {
         if (keyCode != 256 && keyCode != 261) {
            this.module.setKey(keyCode);
         } else {
            this.module.setKey(-1);
         }

         this.bindingMode = false;
         if (BKClientInit.getInstance().getMenuScreen() instanceof DropDownScreen dropDownScreen) {
            dropDownScreen.getSearchField().setFocused(false);
         }
      }

      super.onKeyPressed(keyCode, scanCode, modifiers);
   }

   public void open() {
      if (this.module.getDisplaySettings().isEmpty()) {
         if (BKClientInit.getInstance().getModuleManager().getModule(Sounds.class).isEnabled() && !this.blocking) {
            ClientSounds.CRITICAL.play(1.0F, 1.0F);
         }

         this.blocking = true;
         this.shakeValue = true;
      } else {
         ModernScreen modernScreen = (ModernScreen) BKClientInit.getInstance().getMenuScreen();
         Rect win = modernScreen.getMenuWindow();
         List<ModernSettings> windows = modernScreen.getWindows();
         float x = win.getX() + win.getWidth() + 10.0F;
         float y = win.getY();
         float width = 152.0F;
         if (!windows.isEmpty()) {
            float h = ((ModernSettings)windows.getLast()).getY() + ((ModernSettings)windows.getLast()).getHeight();
            if (h < win.getY() + win.getHeight()) {
               y = h + 10.0F;
               x = ((ModernSettings)windows.getLast()).getX();
            } else {
               x = ((ModernSettings)windows.getLast()).getX() + ((ModernSettings)windows.getLast()).getWidth() + 10.0F;
            }
         }

         for (ModernSettings window : windows) {
            if (window.getModule() == this) {
               return;
            }
         }

         if ((windows.isEmpty() || !(x + width > IScaledResolution.sr.getScaledWidth())) && windows.size() <= 4) {
            windows.add(new ModernSettings(this, x, y, width));
         } else {
            x = ((ModernSettings)windows.getFirst()).getX();
            y = ((ModernSettings)windows.getFirst()).getY();
            ((ModernSettings)windows.getFirst()).setShowing(false);
            ModernSettings newWindow = new ModernSettings(this, x, y, width);
            windows.addFirst(newWindow);
            float offset = y + newWindow.getHeight() + 10.0F;

            for (ModernSettings window : windows) {
               if (window.getX() == x && window.getModule() != this) {
                  window.setY(offset);
                  offset += window.getHeight() + 10.0F;
               }
            }
         }

         this.visible.setValue(0.0F);
      }
   }

   public boolean isBinding() {
      return false;
   }

   @Generated
   public Animation getEnableAnimation() {
      return this.enableAnimation;
   }

   @Generated
   public ModernModule(Module module, ModernCategory category) {
      this.module = module;
      this.category = category;
   }

   @Generated
   public Animation getVisible() {
      return this.visible;
   }

   @Generated
   public Animation getOffset() {
      return this.offset;
   }

   @Generated
   public Module getModule() {
      return this.module;
   }

   @Generated
   public void setBindingMode(boolean bindingMode) {
      this.bindingMode = bindingMode;
   }
}
