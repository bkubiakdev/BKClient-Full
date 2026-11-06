package dev.bkclient.ui.hud;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.ui.components.popup.Popup;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;

public abstract class HudElement implements SettingsContainer, IMinecraft {
   protected float x;
   protected float y;
   protected float width;
   protected float height;
   protected final Animation animation = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   protected final Animation visible = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   protected final Animation selecting = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   protected final Animation dragAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation blurAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation loadingAnim = new Animation(700L, 0.0F, Easing.SMOOTH_STEP);
   private final Animation widthAnim = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation heightAnim = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation scaleAnim = new Animation(300L, 1.0F, Easing.BAKEK_SIZE);
   protected final Animation bounceAnim = new Animation(1200L, 0.0F, Easing.generate(0.34F, 1.56F, 0.64F, 1.0F));
   protected final Animation pressAnim = new Animation(150L, 1.0F, Easing.FIGMA_EASE_IN_OUT);
   private float prevDragMouseX;
   private float prevDragMouseY;
   private boolean firstDragFrame = true;
   protected float currentStretchX = 1.0F;
   protected float currentStretchY = 1.0F;
   protected float dragAngle = 0.0F;
   protected boolean showing;
   protected boolean select;
   private List<Setting> settings = new ArrayList<>();
   private final SliderSetting scaleSetting = new SliderSetting(this, "hud.scale")
      .min(0.5F)
      .max(2.0F)
      .step(0.1F)
      .currentValue(1.0F);
   private boolean dragging;
   private final dev.bkclient.systems.setting.settings.BooleanSetting locked =
      new dev.bkclient.systems.setting.settings.BooleanSetting(this, "hud.editor.locked");
   private final dev.bkclient.systems.setting.settings.BooleanSetting selectionEffect =
      new dev.bkclient.systems.setting.settings.BooleanSetting(this, "hud.editor.selection_effect").enable();
   private final SliderSetting selectionDuration = new SliderSetting(this, "hud.editor.selection_duration")
      .min(.5F).max(3).step(.1F).currentValue(1.5F);
   private final SliderSetting panelOpacity = new SliderSetting(this, "hud.editor.panel_opacity")
      .min(.1F).max(1).step(.05F).currentValue(1);
   public float selectionStrength() { return selectionEffect.isEnabled() ? Math.clamp(selecting.getValue(),0,1) : 0; }
   private final dev.bkclient.systems.setting.settings.BooleanSetting interfaceIcons =
      new dev.bkclient.systems.setting.settings.BooleanSetting(this, "hud.editor.interface_icons").enable();
   public boolean interfaceIcons() { return interfaceIcons.isEnabled(); }
   public float panelOpacity() { return panelOpacity.getCurrentValue(); }
   public void updateSelectionSweep() {
      if (selectionStrength()>.001F) {
         loadingAnim.setDuration((long)(selectionDuration.getCurrentValue()*1000));
         loadingAnim.update(1);
         if (loadingAnim.getValue()>=1) loadingAnim.setValue(0);
      }
   }
   private float dragX;
   private float dragY;
   private float startDragX;
   private float startDragY;
   protected final String name;
   protected final String icon;

   public HudElement(String name, String icon) {
      this.name = name;
      this.icon = icon;
   }

   public void render(UIContext context) {
      if (!this.showing && this.animation.getValue() <= 0.001F) {
         return;
      }
      this.update(context);
      float anim = this.animation.getValue() * this.visible.getValue();
      if (anim != 0.0F) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, anim));
         float press = this.pressAnim.getValue();
         float scale = (0.5F + anim * 0.5F - 0.05F * this.selecting.getValue()) * press * this.getScale();
         float bounceVal = this.bounceAnim.getValue();
         float bounce = 1.0F + 0.08F * (float)Math.sin(bounceVal * Math.PI * 2.5F) * (1.0F - bounceVal);
         float sx = this.currentStretchX;
         float sy = this.currentStretchY;
         float cx = this.x + this.width / 2.0F;
         float cy = this.y + this.height / 2.0F;
         context.getMatrices().push();
         context.getMatrices().translate(cx, cy, 0.0F);
         float angleDeg = (float)Math.toDegrees(this.dragAngle);
         context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(angleDeg));
         context.getMatrices().scale(scale * bounce * sx, scale * bounce * sy, 1.0F);
         context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(-angleDeg));
         context.getMatrices().translate(-cx, -cy, 0.0F);
         this.renderComponent(context);
         context.getMatrices().pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   protected abstract void renderComponent(UIContext var1);

   /** Seed geometry before a whole-element entrance instead of growing from a stale layout. */
   protected final void snapLayoutSize(float targetWidth, float targetHeight) {
      widthAnim.setValue(targetWidth);
      heightAnim.setValue(targetHeight);
   }

   public void update(UIContext context) {
      // A single non-finite value fed into an Animation poisons it permanently: the eased
      // interpolation keeps mixing NaN into every future frame, so the element renders at
      // NaN scale (= invisible) forever even though isShowing() stays true. That makes it
      // vanish from the screen without ever showing up in the "add element" list, since that
      // list only offers elements that are actually turned off. Heal both the source setting
      // and the animation every frame so a corrupted scale can never get stuck.
      if (!Float.isFinite(this.scaleSetting.getCurrentValue())) {
         this.scaleSetting.setCurrentValue(1.0F);
      }
      this.scaleAnim.update(this.scaleSetting.getCurrentValue());
      if (!Float.isFinite(this.scaleAnim.getValue()) || this.scaleAnim.getValue() <= 0.0F) {
         this.scaleAnim.setValue(this.scaleSetting.getCurrentValue());
      }
      // Same poisoning risk applies to widthAnim/heightAnim below: they must never be fed a
      // non-finite size, or they latch onto NaN exactly like scaleAnim did above.
      if (!Float.isFinite(this.width) || this.width <= 0.0F) this.width = 20.0F;
      if (!Float.isFinite(this.height) || this.height <= 0.0F) this.height = 18.0F;
      float oldWidth = this.widthAnim.getValue();
      this.widthAnim.update(this.width);
      float newWidth = this.widthAnim.getValue();
      float widthDelta = newWidth - oldWidth;
      boolean isLeftSide = this.x + this.width / 2.0F < IScaledResolution.sr.getScaledWidth() / 2.0F;
      boolean repositionOnResize = this.repositionOnResize();
      if (repositionOnResize && !isLeftSide) {
         this.x -= widthDelta;
      }

      if (repositionOnResize && widthDelta != 0.0F) {
         for (HudElement otherElement : BKClientInit.getInstance().getHud().getElements()) {
            if (otherElement != this && otherElement.isShowing()) {
               float verticalOverlap = Math.min(this.y + this.height, otherElement.y + otherElement.height) - Math.max(this.y, otherElement.y);
               if (!(verticalOverlap <= 0.0F)) {
                  if (isLeftSide) {
                     float rightEdge = this.x + newWidth;
                     float distanceToOther = otherElement.x - rightEdge;
                     if (distanceToOther >= -5.0F && distanceToOther <= 25.0F) {
                        otherElement.x += widthDelta;
                        otherElement.x = Math.max(0.0F, Math.min(otherElement.x, IScaledResolution.sr.getScaledWidth() - otherElement.width));
                     }
                  } else {
                     float leftEdge = this.x;
                     float distanceToOther = leftEdge - (otherElement.x + otherElement.width);
                     if (distanceToOther >= -5.0F && distanceToOther <= 25.0F) {
                        otherElement.x -= widthDelta;
                        otherElement.x = Math.max(0.0F, Math.min(otherElement.x, IScaledResolution.sr.getScaledWidth() - otherElement.width));
                     }
                  }
               }
            }
         }
      }

      this.width = newWidth;
      this.heightAnim.update(this.height);
      this.height = this.heightAnim.getValue();
      this.dragAnim.update(this.dragging);
      this.pressAnim.update(this.dragging ? 0.97F : 1.0F);
      this.animation.setEasing(this.showing ? Easing.BAKEK : Easing.BAKEK_BACK);
      this.animation.update(this.showing);
      boolean shouldShow = this.show();
      this.visible.setEasing(shouldShow ? Easing.BAKEK : Easing.BAKEK_BACK);
      this.visible.update(shouldShow);
      this.selecting.update(this.select);
      this.blurAnim.update(this.animation.getValue() >= 0.6F);
      if (this.dragging) {
         // The release event can be missed by the game's input pipeline, which then leaves the
         // element glued to the cursor forever. Poll the real left-button state so a drag always
         // ends the moment the button is up, regardless of what events were delivered.
         long windowHandle = mc.getWindow().getHandle();
         if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(windowHandle, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) != org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            this.dragging = false;
            BKClientInit.getInstance().getFileManager().writeFile("client");
         }
      }

      if (this.dragging) {
         float mouseX = (float)GuiUtility.getMouse().getX();
         float mouseY = (float)GuiUtility.getMouse().getY();

         float screenWidth = IScaledResolution.sr.getScaledWidth();
         float screenHeight = IScaledResolution.sr.getScaledHeight();

         this.x = Math.max(0.0F, Math.min(screenWidth - this.width, mouseX - this.dragX));
         this.y = Math.max(0.0F, Math.min(screenHeight - this.height, mouseY - this.dragY));

         for (GridLine line : BKClientInit.getInstance().getHud().getGrid().getLines()) {
            if (line.getType() == GridLine.Type.VERTICAL) {
               this.x = this.snapToLine(line, this.x, List.of(0.0F, this.width, this.width / 2.0F),
                  List.of(0.0F, -this.width, -this.width / 2.0F));
            } else {
               this.y = this.snapToLine(line, this.y, List.of(0.0F, this.height, this.height / 2.0F),
                  List.of(0.0F, -this.height, -this.height / 2.0F));
            }
         }
      }

      this.keepInsideViewport();

      if (this.isHovered(context) && this.animation.getValue() >= 1.0F) {
         CursorUtility.set(CursorType.HAND);
      }
   }

   /**
    * Whether a changing component size may move its own anchor or nearby HUD elements.
    * Dynamic info rows override this because their text changes continuously.
    */
   protected boolean repositionOnResize() {
      return true;
   }

   /** Keeps the complete scaled HUD element visible after loading or resizing. */
   private void keepInsideViewport() {
      float screenWidth = IScaledResolution.sr.getScaledWidth();
      float screenHeight = IScaledResolution.sr.getScaledHeight();
      if (screenWidth <= 0.0F || screenHeight <= 0.0F) return;

      float margin = 4.0F;
      if (!Float.isFinite(this.x)) this.x = margin;
      if (!Float.isFinite(this.y)) this.y = margin;
      // Math.max does not filter NaN (Math.max(x, NaN) == NaN), so a corrupted scale used to
      // pass straight through here and poison x/y as well, making the element unrecoverable.
      float rawScale = this.getScale();
      float scale = Float.isFinite(rawScale) && rawScale > 0.0F ? Math.max(0.1F, rawScale) : 1.0F;
      float extraX = Math.max(0.0F, (this.width * scale - this.width) / 2.0F);
      float extraY = Math.max(0.0F, (this.height * scale - this.height) / 2.0F);
      float minX = margin + extraX;
      float minY = margin + extraY;
      float maxX = screenWidth - margin - this.width - extraX;
      float maxY = screenHeight - margin - this.height - extraY;

      this.x = maxX < minX ? Math.max(margin, (screenWidth - this.width) / 2.0F) : Math.clamp(this.x, minX, maxX);
      this.y = maxY < minY ? Math.max(margin, (screenHeight - this.height) / 2.0F) : Math.clamp(this.y, minY, maxY);
   }

   protected float getContextMenuWidth() {
      return 170.0F;
   }

   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && this.showing) {
         if (button == MouseButton.LEFT && !locked.isEnabled()) {
            this.dragging = true;
            this.dragX = (float)(mouseX - this.x);
            this.dragY = (float)(mouseY - this.y);
            this.startDragX = this.x;
            this.startDragY = this.y;
          } else if (button == MouseButton.RIGHT) {
             this.openSettings(mouseX, mouseY);
          }
      }
   }

   protected boolean compactSettings() { return true; }

   private void openSettings(double mouseX, double mouseY) {
      this.openSettings(mouseX, mouseY, false);
   }

   private boolean isBasicSetting(Setting setting) {
      String key = setting.getName();
      if (key.startsWith("hud.editor.")) {
         return key.equals("hud.editor.panel_opacity") || key.equals("hud.editor.header")
            || key.equals("hud.editor.joined");
      }
      if (key.startsWith("hud.header.")) return false;
      return switch (key) {
         case "hud.binds.icons", "hud.binds.preview", "hud.binds.sort_key", "hud.binds.sort_active",
            "hud.list.limit", "hud.list.sort", "hud.armor.accent_rings", "hud.cooldowns2.preview" -> false;
         default -> true;
      };
   }

   private void openSettings(double mouseX, double mouseY, boolean advanced) {
      this.select = true;
      this.loadingAnim.setValue(0.0F);
      Popup popup = new Popup((float)mouseX, (float)mouseY, this.getContextMenuWidth(), 6.0F)
         .hudSettings()
         .compactSettings(this.compactSettings())
         .title(Localizator.translate(advanced ? "hud.editor.advanced" : this.settings.isEmpty() ? "actions" : "settings"))
         .separator();
      for (Setting setting : this.settings) {
         if (advanced || this.isBasicSetting(setting)) popup.setting(setting);
      }
      popup.separator().button(Localizator.translate(advanced ? "hud.editor.basic" : "hud.editor.advanced"),
         "icons/arrow.png", menu -> {
            menu.setShowing(false);
            BKClientInit.getInstance().getHud().getPopups().remove(menu);
            this.openSettings(menu.getX(), menu.getY(), !advanced);
         });
      if (advanced) {
         popup.button(Localizator.translate("hud.editor.center_element"), "icons/hud/center.png", menu -> {
            float previousX = this.x, previousY = this.y;
            this.pos((IScaledResolution.sr.getScaledWidth() - this.width) / 2,
               (IScaledResolution.sr.getScaledHeight() - this.height) / 2);
            BKClientInit.getInstance().getHud().getHistoryManager().registerMove(this, previousX, previousY, this.x, this.y);
            BKClientInit.getInstance().getFileManager().writeFile("client");
         });
         popup.button(Localizator.translate("hud.editor.reset_scale"), "icons/hud/reset.png", menu -> {
            this.scaleSetting.setCurrentValue(1);
            BKClientInit.getInstance().getFileManager().writeFile("client");
         });
      }
      popup.button(Localizator.translate("remove"), "icons/hud/trash.png", popup1 -> {
         this.showing = false;
         popup1.setShowing(false);
         BKClientInit.getInstance().getFileManager().writeFile("client");
      }).onClose(() -> {
         this.select = false;
         BKClientInit.getInstance().getFileManager().writeFile("client");
      });
      BKClientInit.getInstance().getHud().getPopups().add(popup);
   }

   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (this.dragging && button == MouseButton.LEFT) {
         this.dragging = false;
         if (this.x != this.startDragX || this.y != this.startDragY) {
            BKClientInit.getInstance().getHud().getHistoryManager().registerMove(this, this.startDragX, this.startDragY, this.x, this.y);
         }

         BKClientInit.getInstance().getFileManager().writeFile("client");
      }
   }

   private float snapToLine(GridLine line, float pos, List<Float> offsets, List<Float> adjustments) {
      for (int i = 0; i < offsets.size(); i++) {
         float distance = Math.abs(pos + offsets.get(i) - line.getPos());
         if (distance < 25.0F) {
            line.setActive(true);
         }

         if (distance < 5.0F) {
            pos = line.getPos() + adjustments.get(i);
         }
      }

      return pos;
   }

   public boolean show() {
      return true;
   }

   public boolean isHovered(float mouseX, float mouseY) {
      float anim = this.animation.getValue() * this.visible.getValue();
      float scale = (0.5F + anim * 0.5F - 0.05F * this.selecting.getValue()) * this.getScale();
      float scaledWidth = this.width * scale;
      float scaledHeight = this.height * scale;
      float scaledX = this.x + (this.width - scaledWidth) / 2.0F;
      float scaledY = this.y + (this.height - scaledHeight) / 2.0F;
      return GuiUtility.isHovered(scaledX, scaledY, scaledWidth, scaledHeight, mouseX, mouseY);
   }

   public boolean isHovered(double mouseX, double mouseY) {
      return this.isHovered((float)mouseX, (float)mouseY);
   }

   public boolean isHovered(UIContext context) {
      return this.isHovered(context.getMouseX(), context.getMouseY());
   }

   public void pos(float x, float y) {
      this.x = x;
      this.y = y;
   }

   @Generated
   public float getX() {
      return this.x;
   }

   @Generated
   public float getY() {
      return this.y;
   }

   @Generated
   public float getWidth() {
      return this.width;
   }

   @Generated
   public float getHeight() {
      return this.height;
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }

   @Generated
   public Animation getVisible() {
      return this.visible;
   }

   @Generated
   public Animation getSelecting() {
      return this.selecting;
   }

   @Generated
   public Animation getDragAnim() {
      return this.dragAnim;
   }

   @Generated
   public Animation getBlurAnim() {
      return this.blurAnim;
   }

   @Generated
   public Animation getLoadingAnim() {
      return this.loadingAnim;
   }

   @Generated
   public Animation getWidthAnim() {
      return this.widthAnim;
   }

   @Generated
   public Animation getHeightAnim() {
      return this.heightAnim;
   }

   @Generated
   public Animation getScaleAnim() {
      return this.scaleAnim;
   }

   @Generated
   public boolean isShowing() {
      return this.showing;
   }

   @Generated
   public boolean isSelect() {
      return this.select;
   }

   @Generated
   @Override
   public List<Setting> getSettings() {
      return this.settings;
   }

   @Generated
   public boolean isDragging() {
      return this.dragging;
   }

   @Generated
   public float getDragX() {
      return this.dragX;
   }

   @Generated
   public float getDragY() {
      return this.dragY;
   }

   @Generated
   public float getStartDragX() {
      return this.startDragX;
   }

   @Generated
   public float getStartDragY() {
      return this.startDragY;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public String getIcon() {
      return this.icon;
   }

   @Generated
   public void setX(float x) {
      this.x = x;
   }

   @Generated
   public void setY(float y) {
      this.y = y;
   }

   @Generated
   public void setWidth(float width) {
      this.width = width;
   }

   @Generated
   public void setHeight(float height) {
      this.height = height;
   }

   @Generated
   public void setShowing(boolean showing) {
      this.showing = showing;
   }

   @Generated
   public void setSelect(boolean select) {
      this.select = select;
   }

   @Generated
   public void setSettings(List<Setting> settings) {
      this.settings = settings;
   }

   @Generated
   public void setDragging(boolean dragging) {
      this.dragging = dragging;
   }

   @Generated
   public void setDragX(float dragX) {
      this.dragX = dragX;
   }

   @Generated
   public void setDragY(float dragY) {
      this.dragY = dragY;
   }

   @Generated
   public void setStartDragX(float startDragX) {
      this.startDragX = startDragX;
   }

   @Generated
   public void setStartDragY(float startDragY) {
      this.startDragY = startDragY;
   }

   public float getScale() {
      return this.scaleAnim.getValue();
   }
}
