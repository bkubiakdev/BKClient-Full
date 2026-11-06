package dev.bkclient.ui.menu.waypoints;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.modules.visuals.Waypoints;
import dev.bkclient.systems.waypoints.WaypointStore.Point;
import dev.bkclient.ui.components.ColorPicker;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

/** Name-row layout adapted from the older client's WaypointComponent, without coordinate editing. */
public final class WaypointScreen extends Screen implements IMinecraft {
   private final Waypoints module;
   private final Screen parent;
   private final String scope = Waypoints.scope(), dimension = Waypoints.dimension();
   private final Vec3d position = mc.player.getPos();
   private final TextField name = new TextField(Fonts.REGULAR.getFont(7));
   private ColorRGBA color = Colors.getAccentColor();
   private ColorPicker picker;
   private float x, y;
   private String error = "";
   private boolean saved;

   public WaypointScreen(Waypoints module, Screen parent) {
      super(Text.literal("Waypoint"));
      this.module = module;
      this.parent = parent;
      name.setPreview("Waypoint name…");
      TextField.releaseMovementKeys();
   }

   @Override protected void init() {
      if (picker != null) color = picker.built();
      picker = null;
      x = (width-180)/2.0F; y = (height-28)/2.0F;
      name.set(x+7, y+7, 132, 14);
      name.setFocused(true);
   }

   @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      UIContext c = UIContext.of(context, mouseX, mouseY, delta);
      TextField.releaseMovementKeys();
      c.drawClientRect(x, y, 180, 28, 1, 0, 2);
      name.set(x+7, y+7, 132, 14);
      name.setTextColor(Colors.getTextColor());
      name.render(c);
      c.drawRoundedRect(x+148, y+10.5F, 7, 7, BorderRadius.all(3), color);
      c.drawTextureSmooth(BKClientInit.id("icons/colorpicker/plus.png"), x+162, y+9, 10, 10, Colors.getTextColor().withAlpha(210));
      if (!error.isEmpty()) c.drawFadeoutText(Fonts.REGULAR.getFont(6), error, x, y+33,
         Colors.getTextColor(), 0.8F, 1, 180);
      if (picker != null) {
         picker.render(c); color = picker.built();
         if (!picker.isShowing()) { picker = null; name.setFocused(true); }
      }
   }

   private void save() {
      if (saved) return;
      if (!scope.equals(Waypoints.scope()) || !dimension.equals(Waypoints.dimension())) {
         error = "World changed — reopen Waypoints"; return;
      }
      try {
         String label = name.getBuiltText().strip();
         if (label.isEmpty()) label = "Waypoint";
         module.store().put(new Point(UUID.randomUUID().toString(), scope, dimension, label,
            position.x, position.y, position.z, color.getRGB(), true));
         saved = true;
         module.refresh();
         if (!module.isEnabled()) module.enable();
         mc.setScreen(null);
      } catch (IllegalArgumentException invalid) { error = invalid.getMessage(); }
      catch (Exception failure) { error = "Could not save waypoint"; }
   }

   @Override public boolean mouseClicked(double mx, double my, int buttonIndex) {
      MouseButton button = MouseButton.fromButtonIndex(buttonIndex);
      // The picker only owns its own bounds (or the eyedropper interaction).
      if (picker != null && (picker.isHovered(mx, my) || picker.isPick())) {
         picker.onMouseClicked(mx, my, button); color = picker.built(); return true;
      }
      boolean wasOpen = picker != null;
      if (wasOpen) { color = picker.built(); picker = null; }
      if (mx >= x && mx <= x+180 && my >= y && my <= y+28) {
         if (button == MouseButton.LEFT && mx >= x+159) save();
         else if (button == MouseButton.LEFT && mx >= x+142) {
            if (wasOpen) { name.setFocused(true); return true; }
            name.setFocused(false);
            float pickerY = y+33+136 <= height-4 ? y+33 : Math.max(4, y-141);
            picker = new ColorPicker(Math.max(4, Math.min(x+80, width-147)), pickerY, 2, false, color, "Waypoint color");
         }
         else {
            name.onMouseClicked(mx, my, button);
            if (button == MouseButton.LEFT) name.setFocused(true);
         }
         return true;
      }
      name.setFocused(false);
      return true;
   }
   @Override public boolean mouseReleased(double mx, double my, int buttonIndex) {
      MouseButton button = MouseButton.fromButtonIndex(buttonIndex);
      name.onMouseReleased(mx, my, button);
      if (picker != null) picker.onMouseReleased(mx, my, button);
      return true;
   }
   @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return true; }
   @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) { return true; }
   @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }
   @Override public boolean keyPressed(int key, int scan, int mods) {
      if (picker != null) {
         if (key == 256 || key == 257 || key == 335) { color = picker.built(); picker = null; name.setFocused(true); }
         else picker.onKeyPressed(key, scan, mods);
         return true;
      }
      if (key == 256) { close(); return true; }
      if (key == 257 || key == 335) { save(); return true; }
      if (key == 258) { name.setFocused(!name.isFocused()); return true; }
      if (name.isFocused()) name.onKeyPressed(key, scan, mods);
      return true;
   }
   @Override public boolean charTyped(char chr, int mods) {
      if (name.isFocused()) name.charTyped(chr, mods);
      return true;
   }
   @Override public void removed() {
      name.setFocused(false); TextField.releaseMovementKeys(); super.removed();
   }
   @Override public void close() { mc.setScreen(parent); }
   @Override public boolean shouldPause() { return false; }
}
