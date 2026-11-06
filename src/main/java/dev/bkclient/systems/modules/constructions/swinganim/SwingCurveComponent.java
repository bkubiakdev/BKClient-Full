package dev.bkclient.systems.modules.constructions.swinganim;

import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.BezierSetting;
import dev.bkclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.render.ScissorUtility;
import net.minecraft.util.math.Vec2f;

/** Editable easing curve styled by the Swing screen's ClickSys palette. */
final class SwingCurveComponent extends MenuSettingComponent<BezierSetting> {
   private final Animation startX, startY, endX, endY;
   private boolean dragStart, dragEnd;
   private float minX, minY, maxX = 1, maxY = 1;
   private static final float HANDLE_MARGIN = 4;

   SwingCurveComponent(BezierSetting setting, CustomComponent parent) {
      super(setting, parent);
      startX = animation(setting.start().x);
      startY = animation(setting.start().y);
      endX = animation(setting.end().x);
      endY = animation(setting.end().y);
      fitRange();
   }

   private static Animation animation(float value) {
      return new Animation(500L, value, Easing.BAKEK_PAGES);
   }

   private float boxX() { return x + 11; }
   private float boxY() { return y + 12 + Fonts.REGULAR.getFont(8).height(); }
   private float boxSize() { return Math.max(1, width - 22); }
   private float inset() { return boxSize() * 0.17F; }
   private float plotSize() { return Math.max(1, boxSize() - inset() * 2); }
   private float graphLeft() { return boxX() + inset() + HANDLE_MARGIN; }
   private float graphTop() { return boxY() + inset() + HANDLE_MARGIN; }
   private float graphSize() { return Math.max(1, plotSize() - HANDLE_MARGIN * 2); }
   private Vec2f point(float px, float py) {
      return new Vec2f(graphLeft() + (px - minX) / (maxX - minX) * graphSize(),
         graphTop() + (py - minY) / (maxY - minY) * graphSize());
   }

   private void fitRange() {
      // Fit both the saved preset and animated handles; negative control values are valid easing.
      // Including the anchors keeps the entire cubic inside the frame by its convex-hull property.
      minX = Math.min(0, Math.min(Math.min(startX.getValue(), endX.getValue()), Math.min(setting.start().x, setting.end().x)));
      minY = Math.min(0, Math.min(Math.min(startY.getValue(), endY.getValue()), Math.min(setting.start().y, setting.end().y)));
      maxX = Math.max(1, Math.max(Math.max(startX.getValue(), endX.getValue()), Math.max(setting.start().x, setting.end().x)));
      maxY = Math.max(1, Math.max(Math.max(startY.getValue(), endY.getValue()), Math.max(setting.start().y, setting.end().y)));
   }
   private boolean overGraph(double mx, double my) {
      return GuiUtility.isHovered(boxX(), boxY(), boxSize(), boxSize(), mx, my);
   }

   @Override protected void renderComponent(UIContext context) {
      boolean hovered = overGraph(context.getMouseX(), context.getMouseY());
      hoverAnimation.update(isHovered(context));
      if (dragStart || dragEnd) movePoint(context.getMouseX(), context.getMouseY());
      startX.update(setting.start().x);
      startY.update(setting.start().y);
      endX.update(setting.end().x);
      endY.update(setting.end().y);
      if (dragStart || dragEnd) {
         // Direct handle tracking avoids animation overshoot and keeps the drag transform stable.
         startX.setValue(setting.start().x);
         startY.setValue(setting.start().y);
         endX.setValue(setting.end().x);
         endY.setValue(setting.end().y);
      } else {
         fitRange();
      }
      if (dragStart || dragEnd) CursorUtility.set(CursorType.CROSSHAIR);
      else if (hovered) CursorUtility.set(CursorType.HAND);

      var font = Fonts.REGULAR.getFont(8);
      drawSettingName(context, font, Localizator.translate(setting.getName()), boxX(), y + 6,
         Colors.getTextColor().mulAlpha(0.75F + 0.25F * hoverAnimation.getValue()), boxSize());
      context.drawRoundedRect(boxX(), boxY(), boxSize(), boxSize(), BorderRadius.all(6),
         Colors.getBackgroundColor());
      ScissorUtility.push(context.getMatrices(), boxX(), boxY(), boxSize(), boxSize());
      try {
         Vec2f a = point(0, 1), b = point(startX.getValue(), startY.getValue());
         Vec2f c = point(endX.getValue(), endY.getValue()), d = point(1, 0);
         SwingGizmoRenderer.dashedBorder(context.getMatrices(), boxX() + inset(), boxY() + inset(),
            plotSize(), 6, Colors.getTextColor().mulAlpha(0.25F));
         SwingGizmoRenderer.line(context.getMatrices(), a, b, Colors.getTextColor().mulAlpha(0.45F), 1);
         SwingGizmoRenderer.line(context.getMatrices(), d, c, Colors.getTextColor().mulAlpha(0.45F), 1);
         SwingGizmoRenderer.bezier(context.getMatrices(), a, b, c, d, Colors.getTextColor());
         context.drawRoundedBorder(a.x - 3, a.y - 3, 6, 6, 1, BorderRadius.all(3), Colors.getTextColor());
         dot(context, d, 2);
         dot(context, b, 3);
         dot(context, c, 3);
      } finally {
         ScissorUtility.pop();
      }
      context.drawRoundedBorder(boxX(), boxY(), boxSize(), boxSize(), 0.5F, BorderRadius.all(6),
         Colors.getTextColor().mulAlpha(0.08F));
   }

   private void dot(UIContext context, Vec2f point, float radius) {
      context.drawRoundedRect(point.x - radius, point.y - radius, radius * 2, radius * 2,
         BorderRadius.all(radius), Colors.getTextColor());
   }

   @Override public void onMouseClicked(double mx, double my, MouseButton button) {
      if (button != MouseButton.LEFT || !overGraph(mx, my)) return;
      Vec2f start = point(startX.getValue(), startY.getValue());
      Vec2f end = point(endX.getValue(), endY.getValue());
      dragStart = Math.hypot(mx - start.x, my - start.y) <= Math.hypot(mx - end.x, my - end.y);
      dragEnd = !dragStart;
      movePoint(mx, my);
   }

   private void movePoint(double mx, double my) {
      // Use the same fitted coordinates for drawing, hit testing and dragging.
      float px = Math.clamp(((float) mx - graphLeft()) / graphSize(), 0, 1);
      float py = Math.clamp(((float) my - graphTop()) / graphSize(), 0, 1);
      Vec2f value = new Vec2f(minX + px * (maxX - minX), minY + py * (maxY - minY));
      if (dragStart) setting.start(value);
      else if (dragEnd) setting.end(value);
   }

   @Override public void onMouseReleased(double mx, double my, MouseButton button) {
      dragStart = dragEnd = false;
   }

   @Override public float getHeight() {
      return height = 12 + Fonts.REGULAR.getFont(8).height() + boxSize() + 6;
   }
}
