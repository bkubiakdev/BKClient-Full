package dev.bkclient.systems.modules.constructions.swinganim.presets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.constructions.swinganim.SwingManager;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.render.ScissorUtility;

public class PresetComponent extends CustomComponent {
   private final Animation addAnim = new Animation(300L, Easing.BAKEK);
   private final ScrollHandler scrollHandler = new ScrollHandler();
   private final TextField textField;
   private boolean hovered;
   private final Animation heightAnim = new Animation(300L, Easing.BAKEK_SMALLER);

   public PresetComponent() {
      this.textField = new TextField(Fonts.REGULAR.getFont(8.0F));
      this.textField.setPreview(Localizator.translate("type_name"));
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.hovered = this.isHovered(context);
      SwingManager swingManager = BKClientInit.getInstance().getSwingManager();
      SwingPresetManager manager = BKClientInit.getInstance().getSwingPresetManager();
      List<SwingPresetFile> presets = manager.getSwingPresetFiles();
      float x = this.x + 8.0F;
      float y = this.y - 1.0F;
      float width = this.width - 16.0F;
      this.scrollHandler.update();
      context.drawRoundedRect(
         x - 1.0F, y + 7.0F, width + 2.0F, 8.0F + this.height - 46.0F, BorderRadius.all(6.0F), Colors.getBackgroundColor()
      );
      ScissorUtility.push(context.getMatrices(), x - 1.0F, y + 7.5F, width + 2.0F, 7.0F + this.height - 46.0F);
      float offset = 0.0F;

      for (SwingPreset value : BKClientInit.getInstance().getSwingManager().getPresets()) {
         float elmtY = (float)(y + 14.0F + offset - this.scrollHandler.getValue());
         boolean hover = GuiUtility.isHovered(x - 1.0F, y + 7.5F, width + 2.0F, 7.0F + this.height - 46.0F, context)
            && GuiUtility.isHovered(x - 1.0F, elmtY - 4.0F, width + 2.0F, 12.0, context.getMouseX(), context.getMouseY());
         value.getHoverAnimation().update(hover);
         value.getActiveAnimation().update(Objects.equals(value.getName(), swingManager.getCurrent()));
         drawRow(context, x, elmtY, width, value.getHoverAnimation().getValue(), value.getActiveAnimation().getValue());
         context.drawFadeoutText(
            Fonts.REGULAR.getFont(7.0F),
            Localizator.translate(value.getName()),
            x + 7.0F,
            elmtY + 0.5F,
            Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * value.getHoverAnimation().getValue() + 0.25F * value.getActiveAnimation().getValue())),
            0.8F,
            1.0F,
            width - 12.0F - value.getActiveAnimation().getValue() * 10.0F
         );
         if (hover) {
            CursorUtility.set(CursorType.HAND);
         }

         if (value.getActiveAnimation().getValue() >= 0.0F) {
            context.drawTexture(
               BKClientInit.id("icons/check.png"),
               x + width - 11.0F - value.getActiveAnimation().getValue() * 2.0F,
               elmtY,
               6.0F,
               6.0F,
               Colors.getTextColor().withAlpha(value.getActiveAnimation().getValue() * 255.0F)
            );
         }

         offset += 12.0F;
      }

      for (SwingPresetFile value : presets) {
         if (!value.getFileName().equals("autosave")) {
            float elmtYx = (float)(y + 14.0F + offset - this.scrollHandler.getValue());
            boolean hoverx = GuiUtility.isHovered(x - 1.0F, y + 7.5F, width + 2.0F, 7.0F + this.height - 46.0F, context)
               && GuiUtility.isHovered(x - 1.0F, elmtYx - 4.0F, width + 2.0F, 12.0, context.getMouseX(), context.getMouseY());
            value.getHoverAnimation().update(hoverx);
            value.getActiveAnimation().update(Objects.equals(value.getFileName(), swingManager.getCurrent()));
            drawRow(context, x, elmtYx, width, value.getHoverAnimation().getValue(), value.getActiveAnimation().getValue());
            context.drawFadeoutText(
               Fonts.REGULAR.getFont(7.0F),
               value.getFileName(),
               x + 7.0F + 10.0F * value.getHoverAnimation().getValue(),
               elmtYx + 0.5F,
               Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * value.getHoverAnimation().getValue() + 0.25F * value.getActiveAnimation().getValue())),
               0.8F,
               1.0F,
               width - 12.0F - value.getActiveAnimation().getValue() * 10.0F - 10.0F * value.getHoverAnimation().getValue()
            );
            if (hoverx) {
               CursorUtility.set(CursorType.HAND);
            }

            if (value.getHoverAnimation().getValue() >= 0.0F) {
               context.drawTexture(
                  BKClientInit.id("icons/trash.png"),
                  x + 7.0F * value.getHoverAnimation().getValue(),
                  elmtYx,
                  6.0F,
                  6.0F,
                  Colors.getTextColor().withAlpha(value.getHoverAnimation().getValue() * 255.0F)
               );
            }

            if (value.getActiveAnimation().getValue() >= 0.0F) {
               context.drawTexture(
                  BKClientInit.id("icons/check.png"),
                  x + width - 11.0F - value.getActiveAnimation().getValue() * 2.0F,
                  elmtYx,
                  6.0F,
                  6.0F,
                  Colors.getTextColor().withAlpha(value.getActiveAnimation().getValue() * 255.0F)
               );
            }

            offset += 12.0F;
         }
      }

      ScissorUtility.pop();
      context.drawRoundedRect(x - 1.0F, y + this.height - 25.0F, width + 2.0F, 20.0F, BorderRadius.all(6.0F), Colors.getBackgroundColor());
      context.drawRoundedBorder(x - 1.0F, y + this.height - 25.0F, width + 2.0F, 20.0F, 0.5F, BorderRadius.all(6.0F), Colors.getOutlineColor());
      context.drawTexture(
         BKClientInit.id("icons/add.png"),
         x + width - 2.0F * this.addAnim.getValue() - 10.0F,
         y + this.height - 25.0F + 6.0F,
         8.0F,
         8.0F,
         Colors.getTextColor().mulAlpha(this.addAnim.getValue())
      );
      this.textField.set(x - 1.0F, y + this.height - 25.0F, width + 2.0F - 12.0F, 20.0F);
      this.textField.setAlpha(1.0F);
      this.textField.setTextColor(Colors.getTextColor());
      this.textField.render(context);
      this.addAnim.update(!this.textField.getBuiltText().isBlank());
      if (GuiUtility.isHovered(x + width - 2.0F - 10.0F, y + this.height - 25.0F + 6.0F, 8.0, 8.0, context) && this.addAnim.getValue() > 0.0F) {
         CursorUtility.set(CursorType.HAND);
      }

      this.scrollHandler.setMax(-offset + this.height - 20.0F - 25.0F);
   }

   private void drawRow(UIContext context, float x, float y, float width, float hover, float active) {
      context.drawRoundedRect(x + 2, y - 3, width - 4, 12, BorderRadius.all(3),
         Colors.getFlatColor().mix(Colors.getEnabledModulesColor(), active).mulAlpha(Math.max(hover, active)));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      this.textField.onMouseClicked(mouseX, mouseY, button);
      SwingManager swingManager = BKClientInit.getInstance().getSwingManager();
      SwingPresetManager manager = BKClientInit.getInstance().getSwingPresetManager();
      List<SwingPresetFile> presets = manager.getSwingPresetFiles();
      float x = this.x + 8.0F;
      float y = this.y - 1.0F;
      float width = this.width - 16.0F;
      float offset = 0.0F;

      for (SwingPreset value : BKClientInit.getInstance().getSwingManager().getPresets()) {
         float elmtY = (float)(y + 14.0F + offset - this.scrollHandler.getValue());
         boolean hover = GuiUtility.isHovered(x - 1.0F, y + 7.5F, width + 2.0F, 7.0F + this.height - 46.0F, mouseX, mouseY)
            && GuiUtility.isHovered(x - 1.0F, elmtY - 4.0F, width + 2.0F, 12.0, mouseX, mouseY);
         if (hover && button == MouseButton.LEFT) {
            if (manager.getCurrent() != null) manager.getCurrent().save();
            swingManager.applyPreset(value);
         }

         offset += 12.0F;
      }

      for (SwingPresetFile value : new ArrayList<>(presets)) {
         if (!value.getFileName().equals("autosave")) {
            float elmtY = (float)(y + 14.0F + offset - this.scrollHandler.getValue());
            boolean hover = GuiUtility.isHovered(x - 1.0F, y + 7.5F, width + 2.0F, 7.0F + this.height - 46.0F, mouseX, mouseY)
               && GuiUtility.isHovered(x - 1.0F, elmtY - 4.0F, width + 2.0F, 12.0, mouseX, mouseY);
            if (hover && GuiUtility.isHovered(x + 7.0F, elmtY, 6.0, 6.0, mouseX, mouseY) && button == MouseButton.LEFT) {
               value.delete();
            } else if (hover && button == MouseButton.LEFT) {
               if (manager.getCurrent() != null) {
                  manager.getCurrent().save();
               }

               swingManager.setCurrent(value.getFileName());
               value.load();
            }

            offset += 12.0F;
         }
      }

      if (GuiUtility.isHovered(x + width - 2.0F - 10.0F, y + this.height - 25.0F + 6.0F, 8.0, 8.0, mouseX, mouseY) && !this.textField.getBuiltText().isBlank()) {
         this.create();
      }
   }

   private void create() {
      SwingPresetManager manager = BKClientInit.getInstance().getSwingPresetManager();
      SwingManager swingManager = BKClientInit.getInstance().getSwingManager();
      if (manager.getPreset(this.textField.getBuiltText()) != null) return;
      if (manager.getCurrent() != null) manager.getCurrent().save();
      // Save the animation being edited instead of clearing it when naming a preset.
      swingManager.setCurrent(this.textField.getBuiltText());
      manager.createPreset(this.textField.getBuiltText());
      manager.getPreset(this.textField.getBuiltText()).load();
      this.textField.clear();
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.textField.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 257 && this.textField.isFocused() && !this.textField.getBuiltText().isBlank()) {
         this.create();
      } else {
         this.textField.onKeyPressed(keyCode, scanCode, modifiers);
         if (this.hovered) {
            this.scrollHandler.onKeyPressed(keyCode);
         }
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      this.textField.charTyped(chr, modifiers);
      return super.charTyped(chr, modifiers);
   }

   @Override
   public void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (isHovered(mouseX, mouseY)) this.scrollHandler.scroll(verticalAmount);
   }

   @Override
   public float getHeight() {
      SwingPresetManager manager = BKClientInit.getInstance().getSwingPresetManager();
      List<SwingPresetFile> presets = manager.getSwingPresetFiles();
      return this.height = this.heightAnim
         .update(Math.min(presets.size() * 12 + BKClientInit.getInstance().getSwingManager().getPresets().size() * 12 - 12, 182) + 46);
   }
}
