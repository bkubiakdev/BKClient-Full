package dev.bkclient.ui.menu.dropdown.components.module;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.Module;
import dev.bkclient.systems.modules.modules.other.Sounds;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.ui.menu.dropdown.DropDownScreen;
import dev.bkclient.ui.menu.dropdown.components.MenuPanel;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.animation.types.ColorAnimation;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.sprite.SpriteAnimationPlayer;
import dev.bkclient.utility.sounds.ClientSounds;

public class ModuleComponent extends CustomComponent {
   private final Module module;
   private final MenuPanel parent;
   private final Animation hoverAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation enableAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation shakeAnimation = new Animation(100L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation blockingAnimation = new Animation(500L, Easing.FIGMA_EASE_IN_OUT);
   private final ColorAnimation blockingColorAnimation = new ColorAnimation(500L, ColorRGBA.WHITE, Easing.FIGMA_EASE_IN_OUT);
   private boolean blocking;
   private boolean shakeValue;
   private final SpriteAnimationPlayer enableSpriteAnimation;
   private final SpriteAnimationPlayer disableSpriteAnimation;
   private SpriteAnimationPlayer currentSpriteAnimation;
   private boolean lastModuleState;
   private Font nameFont;
   private float headerHeight;
   private final List<MenuSettingComponent<?>> settingComponents = new ArrayList<>();
   private boolean bindingMode;

   public ModuleComponent(Module module, MenuPanel parent) {
      this.module = module;
      this.parent = parent;
      this.enableSpriteAnimation = new SpriteAnimationPlayer(BKClientInit.id("animations/check_enable.bkanim"));
      this.disableSpriteAnimation = new SpriteAnimationPlayer(BKClientInit.id("animations/check_disable.bkanim"));
      this.lastModuleState = module.isEnabled();
      SpriteAnimationPlayer spriteAnimationPlayer = this.currentSpriteAnimation = this.lastModuleState ? this.enableSpriteAnimation : this.disableSpriteAnimation;
      if (this.lastModuleState) {
         this.enableSpriteAnimation.playOnce();
         this.currentSpriteAnimation = this.enableSpriteAnimation;
      } else {
         this.disableSpriteAnimation.setFrame(0);
         this.disableSpriteAnimation.stop();
         this.currentSpriteAnimation = this.disableSpriteAnimation;
      }
   }

   @Override
   public void onInit() {
      this.nameFont = Fonts.REGULAR.getFont(8.0F);
      this.headerHeight = 20.0F;
      this.settingComponents.clear();

      for (Setting setting : this.module.getDisplaySettings()) {
         MenuSettingComponent settingComponent = GuiUtility.settinge(setting, this);
         if (settingComponent != null) {
            this.settingComponents.add(settingComponent);
         }
      }

      this.settingComponents.forEach(MenuSettingComponent::onInit);

      for (MenuSettingComponent menuSettingComponent : this.settingComponents) {
         menuSettingComponent.getVisibilityAnimation().setValue(menuSettingComponent.getSetting().isVisible() ? 1.0F : 0.0F);
      }

      super.onInit();
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.enableAnimation.update(this.module.isEnabled() ? 1.0F : 0.0F);
      boolean currentState = this.module.isEnabled();
      if (currentState != this.lastModuleState) {
         this.currentSpriteAnimation = currentState ? this.enableSpriteAnimation : this.disableSpriteAnimation;
         this.currentSpriteAnimation.playOnce();
         this.lastModuleState = currentState;
      }

      this.currentSpriteAnimation.update();
      this.blockingAnimation.update(this.blocking);
      this.blockingColorAnimation.update(this.blocking ? new ColorRGBA(255.0F, 150.0F, 150.0F) : Colors.getTextColor());
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

      if (this.parent.isHovered(context) && this.isHovered(context)) {
         CursorUtility.set(CursorType.HAND);
         if (BKClientInit.getInstance().getMenuScreen() instanceof DropDownScreen dropDownScreen) {
            dropDownScreen.setDesc(this.module.getDescription());
         }
      }
   }

   public void drawRegular8(UIContext context) {
      float enabledOffset = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
         ? BKClientInit.getInstance().getThemeManager().getCurrentTheme().getEnabledOffset()
         : 2.0F;
      float nameLeftPadding = 10.0F + enabledOffset * this.enableAnimation.getValue();
      float nameHeight = this.nameFont.height();
      int key = this.module.getKey();
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      Object bindingText = key == -1 ? Localizator.translate("menu.binding") : Localizator.translate("key") + ": " + TextUtility.getKeyName(key);
      ColorRGBA nameColor;
      if (this.blockingAnimation.getValue() > 0.01F) {
         nameColor = this.blockingColorAnimation.getColor();
      } else {
         ColorRGBA off = Colors.getTextColor();
         ColorRGBA on = Colors.getEnabledModulesColor();
         float t = this.enableAnimation.getValue();
         nameColor = off.mix(on, t);
      }

      context.drawText(
         this.nameFont,
         (String)(this.bindingMode && this.parent.getSelectedModuleComponent() == null ? bindingText : this.module.getName()),
         this.x + nameLeftPadding + this.shakeAnimation.getValue(),
         this.y + GuiUtility.getMiddleOfBox(nameHeight, this.headerHeight) - 0.5F,
         nameColor.withAlpha(
            RenderSystem.getShaderColor()[3] * 255.0F * (0.75F + 0.25F * this.enableAnimation.getValue() + 0.25F * this.hoverAnimation.getValue())
         )
      );
   }

   public void drawIcons(UIContext context) {
      float enabledOffset = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
         ? BKClientInit.getInstance().getThemeManager().getCurrentTheme().getEnabledOffset()
         : 2.0F;
      float alpha = this.enableAnimation.getValue() * RenderSystem.getShaderColor()[3];
      if (this.enableAnimation.getValue() > 0.0F || this.currentSpriteAnimation.isPlaying()) {
         DrawUtility.drawAnimationSprite(
            context.getMatrices(),
            this.currentSpriteAnimation.getCurrentSprite(),
            this.x + this.width - 15.0F - this.enableAnimation.getValue() * enabledOffset,
            this.y + 7.0F,
            6.0F,
            6.0F,
            Colors.getIconsColor().mulAlpha(0.1F + 0.9F * alpha)
         );
      }
   }

   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(
         this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(RenderSystem.getShaderColor()[3] * 255.0F * 0.02F)
      );
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY)) {
         if (this.bindingMode && button != MouseButton.LEFT && button != MouseButton.RIGHT) {
            this.module.setKey(button.getButtonIndex());
            this.bindingMode = false;
         } else {
            switch (button) {
               case LEFT:
                  this.module.toggle();
                  break;
               case MIDDLE:
                  for (ModuleComponent comp : this.parent.getModuleComponents()) {
                     comp.setBindingMode(false);
                  }

                  this.bindingMode = true;
                  break;
               case RIGHT:
                  this.open();
            }

            super.onMouseClicked(mouseX, mouseY, button);
         }
      }
   }

   public void open() {
      if (this.module.getDisplaySettings().isEmpty()) {
         if (BKClientInit.getInstance().getModuleManager().getModule(Sounds.class).isEnabled() && !this.blocking) {
            ClientSounds.CRITICAL.play(1.0F, 1.0F);
         }

         this.blocking = true;
         this.shakeValue = true;
      } else {
         this.parent.setSelectedModuleComponent(this);
         this.onInit();
         if (BKClientInit.getInstance().getModuleManager().getModule(Sounds.class).isEnabled()) {
            ClientSounds.CLICKGUI_OPEN.play(0.8F, 1.3F);
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

   @Generated
   public Module getModule() {
      return this.module;
   }

   @Generated
   public MenuPanel getParent() {
      return this.parent;
   }

   @Generated
   public Animation getHoverAnimation() {
      return this.hoverAnimation;
   }

   @Generated
   public Animation getEnableAnimation() {
      return this.enableAnimation;
   }

   @Generated
   public Animation getShakeAnimation() {
      return this.shakeAnimation;
   }

   @Generated
   public Animation getBlockingAnimation() {
      return this.blockingAnimation;
   }

   @Generated
   public ColorAnimation getBlockingColorAnimation() {
      return this.blockingColorAnimation;
   }

   @Generated
   public boolean isBlocking() {
      return this.blocking;
   }

   @Generated
   public boolean isShakeValue() {
      return this.shakeValue;
   }

   @Generated
   public SpriteAnimationPlayer getEnableSpriteAnimation() {
      return this.enableSpriteAnimation;
   }

   @Generated
   public SpriteAnimationPlayer getDisableSpriteAnimation() {
      return this.disableSpriteAnimation;
   }

   @Generated
   public SpriteAnimationPlayer getCurrentSpriteAnimation() {
      return this.currentSpriteAnimation;
   }

   @Generated
   public boolean isLastModuleState() {
      return this.lastModuleState;
   }

   @Generated
   public Font getNameFont() {
      return this.nameFont;
   }

   @Generated
   public float getHeaderHeight() {
      return this.headerHeight;
   }

   @Generated
   public List<MenuSettingComponent<?>> getSettingComponents() {
      return this.settingComponents;
   }

   @Generated
   public boolean isBindingMode() {
      return this.bindingMode;
   }

   @Generated
   public void setBindingMode(boolean bindingMode) {
      this.bindingMode = bindingMode;
   }
}
