package dev.bkclient.ui.menu.spotlight.components;

import java.util.List;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.modules.other.Sounds;
import dev.bkclient.ui.menu.api.MenuCategory;
import dev.bkclient.ui.menu.spotlight.SpotlightScreen;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.sounds.ClientSounds;

public class SpotlightModule extends CustomComponent {
   private static final float CARD_RADIUS = 12.0F;

   private final Module module;
   private final MenuCategory category;
   private final SpotlightScreen screen;
   private final Animation hoverAnimation = new Animation(150L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation enableAnimation = new Animation(250L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation clickAnimation = new Animation(80L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation shakeAnimation = new Animation(100L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation blockingAnimation = new Animation(500L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation dragAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private boolean blocking;
   private boolean shakeValue;
   private boolean bindingMode;
    private boolean drag;
    private float dragProgress;
    private float dragStartX;

   public SpotlightModule(Module module, MenuCategory category, SpotlightScreen screen) {
      this.module = module;
      this.category = category;
      this.screen = screen;
   }

   @Override
   protected void renderComponent(UIContext context) {
       this.enableAnimation.setEasing(Easing.QUARTIC_OUT);
       if (!this.drag) {
          this.enableAnimation.update(this.module.isEnabled());
       }
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      this.blockingAnimation.update(this.blocking);
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

      boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
         && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
      float hoverVal = this.hoverAnimation.getValue();

      float clickScale = 1.0F - 0.015F * this.clickAnimation.getValue();
      if (clickScale < 1.0F) {
         RenderUtility.scale(context.getMatrices(),
            this.x + this.width / 2.0F, this.y + this.height / 2.0F, clickScale);
      }

      ColorRGBA bgColor = dark
         ? new ColorRGBA(255, 255, 255, (int)(12 * hoverVal))
         : new ColorRGBA(0, 0, 0, (int)(5 * hoverVal));
      context.drawSquircle(
         this.x, this.y, this.width, this.height,
         4.0F, BorderRadius.all(CARD_RADIUS), bgColor
      );


      if (clickScale < 1.0F) {
         RenderUtility.end(context.getMatrices());
      }
   }

   public void renderText(UIContext context) {
      int key = this.module.getKey();
      int alpha = (int)(255 * (0.7F + 0.3F * this.hoverAnimation.getValue()));

      if (this.bindingMode) {
         String bindText = Localizator.translate("menu.binding");
         context.drawText(
            Fonts.MEDIUM.getFont(7.0F),
            bindText,
            this.x + 10.0F + this.shakeAnimation.getValue(),
            this.y + GuiUtility.getMiddleOfBox(Fonts.MEDIUM.getFont(7.0F).height(), this.height) - 0.5F,
            Colors.getTextColor().withAlpha(alpha)
         );
      } else {
         ColorRGBA nameColor;
         if (this.blockingAnimation.getValue() > 0.01F) {
            nameColor = Colors.getTextColor().mix(new ColorRGBA(255, 100, 100), 0.5F);
         } else {
            nameColor = Colors.getTextColor().mulAlpha(0.85F);
         }

         context.drawText(
            Fonts.MEDIUM.getFont(7.0F),
            this.module.getName(),
            this.x + 10.0F + this.shakeAnimation.getValue(),
            this.y + GuiUtility.getMiddleOfBox(Fonts.MEDIUM.getFont(7.0F).height(), this.height) - 0.5F,
            nameColor.withAlpha(alpha)
         );
      }

      if (key != -1) {
         float toggleW = 26.0F;
         float toggleX = this.x + this.width - toggleW - 12.0F;
         String keyName = TextUtility.getKeyName(key);
         Font keyFont = Fonts.MEDIUM.getFont(5.0F);
         float keyX = toggleX - 6.0F - keyFont.width(keyName);
         float keyY = this.y + GuiUtility.getMiddleOfBox(keyFont.height(), this.height) - 0.5F;
         int keyAlpha = (int)(255 * (0.5F + 0.5F * this.hoverAnimation.getValue()));
         context.drawText(keyFont, keyName, keyX, keyY,
            Colors.getTextColor().withAlpha(keyAlpha));
      }
   }

   public void renderToggle(UIContext context) {
        float toggleW = 26.0F;
        float toggleH = 10.0F;
        float toggleX = this.x + this.width - toggleW - 12.0F;
        float toggleY = this.y + GuiUtility.getMiddleOfBox(toggleH, this.height) - 1.0F;

      this.dragAnim.update(this.drag ? 1.0F : 0.0F);
      context.drawSquircle(
         toggleX, toggleY, toggleW, toggleH,
         3.0F, BorderRadius.all(4.0F),
         new ColorRGBA(173.0F, 173.0F, 173.0F).mix(new ColorRGBA(0.0F, 216.0F, 32.0F), this.enableAnimation.getValue()).mulAlpha(0.8F)
      );

      float thumbW = 14.0F;
      float thumbH = 7.0F;
      float trackPadding = 1.0F;
      float thumbVisual = this.drag ? this.dragProgress : this.enableAnimation.getValue();
      float thumbX = toggleX + trackPadding + (toggleW - thumbW - trackPadding * 2.0F) * thumbVisual;
      float thumbY = toggleY + (toggleH - thumbH) / 2.0F;
      float dragVal = this.dragAnim.getValue();
      float glassW = thumbW + 6.0F * dragVal;
      float glassH = thumbH + 4.0F * dragVal;
      float glassX = thumbX - (glassW - thumbW) / 2.0F;
      float glassY = thumbY - (glassH - thumbH) / 2.0F;
      if (this.drag || dragVal > 0.0F) {
         context.drawRoundedTexture(BKClientInit.id("textures/boolean/glassbutton.png"), glassX, glassY, glassW, glassH, BorderRadius.all(4.0F));
      } else {
         context.drawSquircle(thumbX, thumbY, thumbW, thumbH, 7.0F, BorderRadius.all(3.0F), Colors.WHITE);
      }

      if (this.drag) {
         this.dragProgress = (float)Math.max(0.0F, Math.min(1.0F, (context.getMouseX() - toggleX) / toggleW));
      }
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY)) {
         this.clickAnimation.reset(0.0F);
         this.clickAnimation.setTargetValue(1.0F);

         if (this.bindingMode && button != MouseButton.LEFT && button != MouseButton.RIGHT) {
            this.module.setKey(button.getButtonIndex());
            this.bindingMode = false;
         } else {
             float toggleW = 26.0F;
             float toggleH = 10.0F;
             float toggleX = this.x + this.width - toggleW - 12.0F;
             float toggleY = this.y + GuiUtility.getMiddleOfBox(toggleH, this.height) - 1.0F;
            boolean onToggle = mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= toggleY && mouseY <= toggleY + toggleH;
             if (button == MouseButton.LEFT) {
                if (onToggle) {
                   this.drag = true;
                   this.dragProgress = this.module.isEnabled() ? 1.0F : 0.0F;
                   this.dragStartX = (float)mouseX;
                } else {
                   this.module.toggle();
                }
             } else if (button == MouseButton.MIDDLE) {
                for (SpotlightModule comp : this.screen.getAllModules()) {
                   comp.bindingMode = false;
                }
                this.bindingMode = true;
             } else if (button == MouseButton.RIGHT) {
                this.open();
             }
         }
      }
   }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
       if (this.drag && button == MouseButton.LEFT) {
          boolean dragged = Math.abs((float) mouseX - this.dragStartX) > 3.0F;
          if (dragged) {
             this.module.setEnabled(this.dragProgress > 0.5F, false);
          } else {
             this.module.toggle();
          }
           this.drag = false;
           this.enableAnimation.setValue(this.dragProgress);
        }
       super.onMouseReleased(mouseX, mouseY, button);
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
         if (this.screen.getSearchField().isFocused()) {
            this.screen.getSearchField().setFocused(false);
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
         List<SpotlightSettings> windows = this.screen.getWindows();
         float x = this.screen.getX() + this.screen.getPillWidth() + 10.0F;
         float y = this.screen.getY();
         float width = 152.0F;
         if (!windows.isEmpty()) {
            SpotlightSettings last = windows.getLast();
            float h = last.getY() + last.getHeight();
            if (h < this.screen.getY() + this.screen.getPillHeight()) {
               y = h + 10.0F;
               x = last.getX();
            } else {
               x = last.getX() + last.getWidth() + 10.0F;
            }
         }

         for (SpotlightSettings window : windows) {
            if (window.getModule() == this) {
               window.setShowing(false);
               return;
            }
         }

          if ((windows.isEmpty() || !(x + width > mc.getWindow().getScaledWidth())) && windows.size() <= 4) {
             windows.add(new SpotlightSettings(this, x, y, width));
          } else {
             SpotlightSettings replace = null;
             for (SpotlightSettings w : windows) {
                if (!w.isPinned()) {
                   replace = w;
                   break;
                }
             }
             if (replace == null) return;
             x = replace.getX();
             y = replace.getY();
             replace.setShowing(false);
             SpotlightSettings newWindow = new SpotlightSettings(this, x, y, width);
             windows.addFirst(newWindow);
          }
      }
   }

   public boolean isBinding() {
      return this.bindingMode;
   }

   public Module getModule() { return this.module; }
   public MenuCategory getCategory() { return this.category; }
   public SpotlightScreen getScreen() { return this.screen; }
   public Animation getHoverAnimation() { return this.hoverAnimation; }
   public Animation getEnableAnimation() { return this.enableAnimation; }
}
