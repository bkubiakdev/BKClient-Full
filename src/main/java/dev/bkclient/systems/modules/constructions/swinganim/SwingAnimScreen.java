package dev.bkclient.systems.modules.constructions.swinganim;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomScreen;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.constructions.swinganim.presets.PresetComponent;
import dev.bkclient.systems.modules.constructions.swinganim.presets.SwingPresetManager;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.ui.components.popup.Popup;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/** Swing editor with a render-scoped, flat ClickSys theme. */
public class SwingAnimScreen extends CustomScreen implements IScaledResolution, IMinecraft {
   private static final float CONTENT_WIDTH = 570.0F;
   private static final float EDITOR_HEIGHT = 230.0F;
   private final Popup presets = new SwingPopup().title("presets");
   private final Popup shared = new SwingPopup().title("shared");
   private final SwingEditor editor = new SwingEditor();
   private final Popup[] panels = {presets, shared};
   private float screenScale = 1.0F;

   public SwingAnimScreen() {
      SwingManager manager = BKClientInit.getInstance().getSwingManager();
      BKClientInit.getInstance().getSwingPresetManager().refresh();
      presets.add(new PresetComponent());
      for (Setting setting : manager.getSharedSettings().settings) {
         if (setting == manager.getBezier()) shared.add(new SwingCurveComponent(manager.getBezier(), shared));
         else shared.setting(setting);
      }
   }

   @Override
   public void render(UIContext context) {
      float contentHeight = Math.max(EDITOR_HEIGHT, Math.max(presets.getHeight(), shared.getHeight()));
      screenScale = Math.max(0.01F, Math.min(1.0F, Math.min(
         (sr.getScaledWidth() - 24.0F) / CONTENT_WIDTH,
         (sr.getScaledHeight() - 24.0F) / contentHeight)));
      float x = (sr.getScaledWidth() / screenScale - CONTENT_WIDTH) / 2.0F;
      float y = (sr.getScaledHeight() / screenScale - contentHeight) / 2.0F;
      UIContext scaled = context.withMouse((int)(context.getMouseX() / screenScale), (int)(context.getMouseY() / screenScale));
      context.getMatrices().push();
      try (ClickGuiV2Theme theme = ClickGuiV2Theme.openClickSys()) {
         context.getMatrices().scale(screenScale, screenScale, 1.0F);
         presets.set(x, y, 170.0F, presets.getHeight());
         shared.set(x + 180.0F, y, 170.0F, shared.getHeight());
         editor.set(x + 360.0F, y, 210.0F, EDITOR_HEIGHT);
         presets.render(scaled);
         shared.render(scaled);
         editor.render(scaled);
         for (Popup panel : panels) {
            scaled.drawRoundedBorder(panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight(),
               0.5F, BorderRadius.all(7), theme.border);
         }
         scaled.draw();
      } finally {
         context.getMatrices().pop();
      }
   }

   @Override
   public void onMouseClicked(double x, double y, MouseButton button) {
      for (Popup panel : panels) panel.onMouseClicked(x / screenScale, y / screenScale, button);
      editor.onMouseClicked(x / screenScale, y / screenScale, button);
   }

   @Override
   public void onMouseReleased(double x, double y, MouseButton button) {
      for (Popup panel : panels) panel.onMouseReleased(x / screenScale, y / screenScale, button);
      editor.onMouseReleased(x / screenScale, y / screenScale, button);
   }

   @Override
   public void onMouseDragged(double x, double y, MouseButton button, double dx, double dy) {
      editor.onMouseDragged(x / screenScale, y / screenScale, button, dx / screenScale, dy / screenScale);
   }

   @Override
   public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      for (Popup panel : panels) {
         if (panel.isHovered(x / screenScale, y / screenScale)) {
            panel.onScroll(x / screenScale, y / screenScale, horizontal, vertical);
         }
      }
      editor.onScroll(x / screenScale, y / screenScale, horizontal, vertical);
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean typing = TextField.LAST_FIELD != null && TextField.LAST_FIELD.isFocused();
      for (Popup panel : panels) panel.onKeyPressed(keyCode, scanCode, modifiers);
      if (!typing) {
         editor.onKeyPressed(keyCode, scanCode, modifiers);
         if (hasControlDown() && (keyCode == GLFW.GLFW_KEY_Z || keyCode == GLFW.GLFW_KEY_Y)) return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      for (Popup panel : panels) panel.charTyped(chr, modifiers);
      return super.charTyped(chr, modifiers);
   }

   private final class SwingPopup extends Popup {
      private SwingPopup() { super(0, 0); }
      @Override protected float viewportWidth() { return sr.getScaledWidth() / screenScale; }
      @Override protected float viewportHeight() { return sr.getScaledHeight() / screenScale; }
   }

   @Override public boolean shouldPause() { return false; }
   @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }

   @Override
   public void close() {
      SwingPresetManager manager = BKClientInit.getInstance().getSwingPresetManager();
      if (manager.getCurrent() != null) manager.getCurrent().save();
      var autosave = manager.getAutoSavePreset();
      if (autosave != null) autosave.save();
      if (TextField.LAST_FIELD != null) TextField.LAST_FIELD.setFocused(false);
      super.close();
      mc.setScreen(BKClientInit.getInstance().getMenuScreen());
   }
}
