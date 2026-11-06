package dev.bkclient.ui.components.popup;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.ui.components.popup.list.Button;
import dev.bkclient.ui.components.popup.list.CheckBox;
import dev.bkclient.ui.components.popup.list.Separator;
import dev.bkclient.ui.components.popup.list.Text;
import dev.bkclient.ui.components.popup.list.Title;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.ui.menu.dropdown.components.settings.impl.ModeSettingComponent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.batching.Batching;
import dev.bkclient.utility.render.batching.impl.FontBatching;
import net.minecraft.client.render.VertexFormats;

public class Popup extends CustomComponent {
   private final Animation animation = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation blurAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final List<CustomComponent> components = new ArrayList<>();
   private boolean showing;
   private final float offsetFactor;
   private Runnable onClose = () -> {};
   private boolean closed;
   private boolean hudSettings;
   private boolean compactSettings;
   public Popup compactSettings(boolean enabled) { this.compactSettings = enabled; return this; }
   private float contentHeight;
   private float scrollOffset;
   private boolean clickHandled;
   public boolean wasClickHandled() { return clickHandled; }
   public boolean hasOpenOverlay() {
      return components.stream().anyMatch(component -> component instanceof ModeSettingComponent mode && mode.isDropdownVisible());
   }


   public Popup(float x, float y) {
      this(x, y, 90.0F);
   }

   public Popup(float x, float y, float width) {
      this(x, y, width, 2.0F);
   }

   public Popup(float x, float y, float width, float offsetFactor) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.offsetFactor = offsetFactor;
      this.showing = true;
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.animation.setEasing(this.showing ? Easing.BAKEK : Easing.BAKEK_BACK);
      this.animation.update(this.showing);
      this.blurAnim.update(this.animation.getValue() >= 0.6F);
      this.height = 0.0F;

      for (CustomComponent component : this.components) {
         if (component instanceof MenuSettingComponent) {
            component.set(this.x - 2.0F, this.y + this.height - this.scrollOffset, this.width + 4.0F, 0.0F);
         } else {
            component.set(this.x, this.y + this.height - this.scrollOffset, this.width, 0.0F);
         }

         this.height = this.height
            + (component.getHeight() + 0.5F) * (component instanceof MenuSettingComponent settingComponent ? settingComponent.getOpacity() : 1.0F);
      }

      this.height += 2.0F;
      this.contentHeight = this.height;
      this.height = Math.min(this.height, Math.max(20, Math.min(this.compactSettings ? 180 : Float.MAX_VALUE, this.viewportHeight() - 8)));
      this.scrollOffset = Math.clamp(this.scrollOffset, 0, Math.max(0, this.contentHeight - this.height));

      // Context menus are often opened next to HUD elements placed on the
      // right or bottom edge. Keep the complete menu inside the logical GUI
      // viewport so the last actions/settings never become unreachable.
      float margin = 4.0F;
      float screenW = this.viewportWidth();
      float screenH = this.viewportHeight();
      float clampedX = Math.max(margin, Math.min(this.x, screenW - this.width - margin));
      float clampedY = Math.max(margin, Math.min(this.y, screenH - this.height - margin));
      if (clampedX != this.x || clampedY != this.y) {
         this.x = clampedX;
         this.y = clampedY;
         this.height = 0.0F;
         for (CustomComponent component : this.components) {
            if (component instanceof MenuSettingComponent) {
               component.set(this.x - 2.0F, this.y + this.height - this.scrollOffset, this.width + 4.0F, 0.0F);
            } else {
               component.set(this.x, this.y + this.height - this.scrollOffset, this.width, 0.0F);
            }
            this.height += (component.getHeight() + 0.5F)
               * (component instanceof MenuSettingComponent settingComponent ? settingComponent.getOpacity() : 1.0F);
         }
         this.height += 2.0F;
         this.height = Math.min(this.height, Math.max(20, Math.min(this.compactSettings ? 180 : Float.MAX_VALUE, screenH - 8)));
      }
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, this.animation.getValue()));
      RenderUtility.scale(
         context.getMatrices(), this.x + this.width / this.offsetFactor, this.y + this.height / this.offsetFactor, 0.5F + this.animation.getValue() * 0.5F
      );
      context.drawClientRect(this.x, this.y, this.width, this.height, this.animation.getValue(), 0.0F, 7.0F);
      context.draw();
      dev.bkclient.utility.render.ScissorUtility.push(context.getMatrices(), this.x - 2, this.y, this.width + 4, this.height);

      for (CustomComponent component : this.components) {
         if (component instanceof MenuSettingComponent settingComponent) {
            settingComponent.getVisibilityAnimation().update(settingComponent.getSetting().isVisible() ? 1.0F : 0.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, settingComponent.getOpacity() * this.animation.getValue());
         }

         int index = this.components.indexOf(component);
         if (!this.compactSettings && index != 0 && !(component instanceof Separator) && !(this.components.get(index - 1) instanceof Separator)) {
            float separatorHeight = 0.5F;
            context.drawRect(this.x, component.getY() - 1.0F, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
         }

         component.render(context);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.animation.getValue());
      }

      for (CustomComponent component : this.components) {
         if (component instanceof MenuSettingComponent comp) {
            Batching font = new FontBatching(VertexFormats.POSITION_TEXTURE_COLOR, Fonts.REGULAR);
            comp.drawRegular8(context);
            font.draw();
         }
      }

      context.draw();
      dev.bkclient.utility.render.ScissorUtility.pop();
      if (this.contentHeight > this.height) {
         float thumb = Math.max(8, this.height * this.height / this.contentHeight);
         float top = (this.height - thumb) * this.scrollOffset / (this.contentHeight - this.height);
         context.drawRect(this.x + this.width - 1, this.y + top, 1, thumb, Colors.getTextColor().withAlpha(100));
      }
      RenderUtility.end(context.getMatrices());

      for (CustomComponent component : this.components) {
         if (component instanceof ModeSettingComponent msComp) {
            msComp.renderOverlay(context);
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   protected float viewportWidth() { return IScaledResolution.sr.getScaledWidth(); }
   protected float viewportHeight() { return IScaledResolution.sr.getScaledHeight(); }

   @Override public boolean isHovered(double mouseX, double mouseY) {
      if (super.isHovered(mouseX, mouseY)) return true;
      for (CustomComponent component : this.components)
         if (component instanceof ModeSettingComponent mode && mode.isOverlayHovered(mouseX, mouseY)) return true;
      return false;
   }
   @Override public boolean isHovered(float mouseX, float mouseY) {
      return this.isHovered((double)mouseX, (double)mouseY);
   }

   public Popup setting(Setting setting) {
      MenuSettingComponent settingComponent = this.compactSettings
         && (setting instanceof dev.bkclient.systems.setting.settings.BooleanSetting || setting instanceof dev.bkclient.systems.setting.settings.SliderSetting)
         ? new CompactHudSettingRow(setting, this) : GuiUtility.settinge(setting, this);
      if (settingComponent != null) {
         this.components.add(settingComponent);
      }

      return this;
   }

   public Popup hudSettings() {
      this.hudSettings = true;
      return this;
   }

   public Popup add(CustomComponent component) {
      this.components.add(component);
      return this;
   }

   public Popup rawTitle(String text) {
      this.add(new Title(text, false));
      return this;
   }

   public Popup text(String text) {
      this.components.add(new Text(text));
      return this;
   }

   public Popup title(String text) {
      this.components.add(new Title(text));
      return this;
   }

   public Popup separator() {
      this.components.add(new Separator());
      return this;
   }

   public Popup checkbox(String text, boolean enabled) {
      this.components.add(new CheckBox(text).enabled(enabled));
      return this;
   }

   public Popup checkbox(String text, boolean enabled, CheckBoxAction action) {
      this.components.add(new CheckBox(text).enabled(enabled).action(action));
      return this;
   }

   public Popup button(String text, dev.bkclient.ui.components.LucideIcons icon, PopupAction runnable) {
      this.add(new Button(this, text, icon, runnable));
      return this;
   }

   public Popup button(String text, String icon, PopupAction runnable) {
      this.components.add(new Button(this, text, icon, runnable));
      return this;
   }

   public Popup onClose(Runnable onClose) {
      this.onClose = onClose;
      return this;
   }

   public void setShowing(boolean showing) {
      this.showing = showing;
      if (!showing && !this.closed) {
         this.onClose.run();
         this.closed = true;
      }
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      this.clickHandled = false;
      for (CustomComponent component : this.components) {
         if (component instanceof ModeSettingComponent mode && mode.isDropdownVisible()) {
            mode.onMouseClicked(mouseX, mouseY, button);
            this.clickHandled = true;
            return;
         }
      }
      if (!GuiUtility.isHovered(this.x, this.y, this.width, this.height, mouseX, mouseY)) {
         return;
      }
      this.clickHandled = true;
      for (CustomComponent component : this.components) {
         if (!(component instanceof MenuSettingComponent settingComponent && settingComponent.getOpacity() == 0.0F)) {
            component.onMouseClicked(mouseX, mouseY, button);
         }
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      for (CustomComponent component : this.components) {
         if (!(component instanceof MenuSettingComponent settingComponent && settingComponent.getOpacity() == 0.0F)) {
            component.onMouseReleased(mouseX, mouseY, button);
         }
      }

      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      for (CustomComponent component : this.components) {
         if (component instanceof ModeSettingComponent mode && mode.isDropdownVisible()) {
            mode.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
            return;
         }
      }
      if (this.contentHeight > this.height && GuiUtility.isHovered(this.x, this.y, this.width, this.height, mouseX, mouseY)) {
         this.scrollOffset = Math.clamp(this.scrollOffset - (float)verticalAmount * 18, 0, this.contentHeight - this.height);
         return;
      }
      for (CustomComponent component : this.components) {
         if (!(component instanceof MenuSettingComponent settingComponent && settingComponent.getOpacity() == 0.0F)) {
            component.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
         }
      }

      super.onScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      for (CustomComponent component : this.components) {
         if (!(component instanceof MenuSettingComponent settingComponent && settingComponent.getOpacity() == 0.0F)) {
            component.onKeyPressed(keyCode, scanCode, modifiers);
         }
      }

      super.onKeyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      for (CustomComponent component : this.components) {
         if (!(component instanceof MenuSettingComponent settingComponent && settingComponent.getOpacity() == 0.0F)) {
            component.charTyped(chr, modifiers);
         }
      }

      return super.charTyped(chr, modifiers);
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }

   @Generated
   public boolean isShowing() {
      return this.showing;
   }

   @Generated
   public boolean isHudSettings() {
      return this.hudSettings;
   }
}
