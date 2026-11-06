package dev.bkclient.systems.modules.constructions.swinganim;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.render.ScissorUtility;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/** Interactive first-person item editor, using the same transforms as the hand renderer. */
public final class SwingEditor extends CustomComponent {
   private enum Phase { START, END, PREVIEW }
   private enum Tool { POSITION, ROTATION, PIVOT }
   private static final float HEADER = 32, FOOTER = 28, AXIS_LENGTH = 0.45F, RING_RADIUS = 0.5F;
   private static final ColorRGBA[] AXIS_COLORS = {ColorRGBA.RED, ColorRGBA.GREEN, ColorRGBA.BLUE};
   private final SwingManager manager = BKClientInit.getInstance().getSwingManager();
   private final ItemStack previewItem = new ItemStack(Items.NETHERITE_SWORD);
   private final Matrix4f projection = new Matrix4f();
   private final Matrix4f uiInverse = new Matrix4f();
   private final Animation[] phaseHover = animations(180), phaseActive = animations(220);
   private final Animation[] toolHover = animations(180), toolActive = animations(220);
   private final Animation resetHover = new Animation(180, Easing.FIGMA_EASE_IN_OUT);
   private final float[] phaseX = new float[3], phaseWidth = new float[3];
   private static final String[] TOOL_ICONS = {"position", "rotation", "anchor"};

   private final Vec2f[] axisEnds = {Vec2f.ZERO, Vec2f.ZERO, Vec2f.ZERO};
   private final Vec2f[][] rings = new Vec2f[3][64];
   private final Deque<float[]> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
   private final Animation recenter = new Animation(260, Easing.FIGMA_EASE_IN_OUT);
   private final Vector3f cameraOffset = new Vector3f(), centerFrom = new Vector3f(), centerTo = new Vector3f();
   private final Vector3f rotationFocus = new Vector3f();
   private Vector3f modelCenter;
   private Vec2f gizmoCenter = Vec2f.ZERO;
   private Phase phase = Phase.START;
   private Tool tool = Tool.POSITION;
   private float yaw = 22, pitch = -12, zoom = 120, panX, panY, panFromX, panFromY;
   private boolean initialized, centering, orbiting, panning, gizmoReady, previewFailed;
   private int draggedAxis = -1;
   private double dragX, dragY;
   private float dragValue, previousAngle, accumulatedAngle;
   private Vec2f dragDirection, rotationCenter;
   private float[] dragPose, transitionPose, lastPose, lastSnapshot;
   private String lastPreset;

   public SwingEditor() {
      lastSnapshot = manager.snapshot();
      lastPreset = manager.getCurrent();
   }

   public SwingPhase selectedPhase() {
      return phase == Phase.END ? manager.getEndPhase() : manager.getStartPhase();
   }

   public boolean isEndPhase() { return phase == Phase.END; }

   @Override
   protected void renderComponent(UIContext context) {
      captureHistory();
      if (!initialized && mc.player != null) {
         try {
            measureModel();
         } catch (RuntimeException failure) {
            BKClientInit.LOGGER.warn("Could not measure the swing preview model; using its origin", failure);
         }
         focusImmediately();
         initialized = true;
      }
      float[] pose = selectedPhase().values();
      if (lastPose != null && !Arrays.equals(lastPose, pose) && draggedAxis < 0 && phase != Phase.PREVIEW) {
         beginRecenter();
      }
      lastPose = pose;
      if (centering) {
         recenter.update(true);
         float t = recenter.getValue();
         cameraOffset.set(centerFrom).lerp(centerTo, t);
         panX = panFromX * (1 - t);
         panY = panFromY * (1 - t);
         if (t >= 1) { centering = false; transitionPose = null; }
      }
      float[] display = displayedPose();
      if (draggedAxis >= 0 && tool == Tool.ROTATION) {
         cameraOffset.set(rotationFocus).sub(itemCenter(display));
      }

      context.drawClientRect(x, y, width, height, 1, 0, 7);
      context.drawRoundedBorder(x, y, width, height, 0.5F, BorderRadius.all(7), Colors.getOutlineColor());
      context.drawRect(x + 8, y + HEADER, width - 16, 0.5F, Colors.getOutlineColor());
      context.drawRect(x + 8, y + height - FOOTER, width - 16, 0.5F, Colors.getOutlineColor());
      drawHeader(context);
      gizmoReady = false;
      ScissorUtility.push(context.getMatrices(), x + 1, y + HEADER, width - 2, height - HEADER - FOOTER);
      try {
         if (mc.player != null && !previewFailed) {
            drawItem(context, display);
            if (phase != Phase.PREVIEW) drawGizmo(context, display);
         } else {
            context.drawText(Fonts.REGULAR.getFont(7), Localizator.translate("swing.preview_unavailable"), x + 10, y + HEADER + 15, Colors.getTextColor());
         }
      } finally {
         ScissorUtility.pop();
      }
      drawToolbar(context);
   }

   private static Animation[] animations(long duration) {
      return new Animation[]{new Animation(duration, Easing.FIGMA_EASE_IN_OUT),
         new Animation(duration, Easing.FIGMA_EASE_IN_OUT), new Animation(duration, Easing.FIGMA_EASE_IN_OUT)};
   }

   private static String phaseLabel(int i) { return new String[]{"swing.start", "swing.end", "swing.preview"}[i]; }

   private void drawHeader(UIContext context) {
      context.drawText(Fonts.SEMIBOLD.getFont(7), Localizator.translate("swing.editor"),
         x + 9, y + 8.5F, Colors.getTextColor());
      var font = Fonts.REGULAR.getFont(7);
      String[] labels = new String[3];
      float total = -8;
      for (int i = 0; i < 3; i++) {
         labels[i] = Localizator.translate(phaseLabel(i));
         phaseWidth[i] = font.width(labels[i]) + 8;
         total += phaseWidth[i];
      }
      float cursor = x + width / 2 - total / 2;
      for (int i = 0; i < 3; i++) {
         phaseX[i] = cursor - 4;
         boolean hover = GuiUtility.isHovered(phaseX[i], y + 20, phaseWidth[i], 11, context);
         phaseHover[i].update(hover);
         phaseActive[i].update(phase.ordinal() == i);
         context.drawRoundedRect(phaseX[i], y + 18, phaseWidth[i] - 1, 12, BorderRadius.all(3),
            Colors.getEnabledModulesColor().mulAlpha(phaseActive[i].getValue()));
         ColorRGBA color = Colors.getTextColor().mulAlpha(0.45F + 0.35F * phaseHover[i].getValue())
            .mix(Colors.getAccent(), phaseActive[i].getValue());
         context.drawText(font, labels[i], cursor, y + 20, color);
         if (hover) CursorUtility.set(CursorType.HAND);
         cursor += phaseWidth[i];
      }
   }

   private float toolbarY() { return y + height - FOOTER + 2; }
   private float toolX(int index) { return x + width / 2 - 30 + index * 22; }

   private void drawToolbar(UIContext context) {
      float resetX = x + width - 25;
      boolean hovered = GuiUtility.isHovered(resetX, toolbarY(), 16, 16, context);
      resetHover.update(hovered);
      drawToolIcon(context, "clean", resetX, Colors.getTextColor().mulAlpha(0.5F + 0.4F * resetHover.getValue()));
      if (hovered) CursorUtility.set(CursorType.HAND);
      if (phase == Phase.PREVIEW) return;
      for (int i = 0; i < 3; i++) {
         boolean hover = GuiUtility.isHovered(toolX(i), toolbarY(), 16, 16, context);
         toolHover[i].update(hover);
         toolActive[i].update(tool.ordinal() == i);
         context.drawRoundedRect(toolX(i), toolbarY(), 16, 16, BorderRadius.all(4),
            Colors.getEnabledModulesColor().mulAlpha(toolActive[i].getValue()));
         ColorRGBA color = Colors.getTextColor().mulAlpha(0.5F + 0.4F * toolHover[i].getValue())
            .mix(Colors.getAccent(), toolActive[i].getValue());
         drawToolIcon(context, TOOL_ICONS[i], toolX(i), color);
         if (hover) CursorUtility.set(CursorType.HAND);
      }
   }

   private void drawToolIcon(UIContext context, String name, float iconX, ColorRGBA color) {
      context.drawTextureSmooth(BKClientInit.id("icons/swing/" + name + ".png"),
         iconX + 3.5F, toolbarY() + 3.5F, 9, 9, color);
   }

   private void measureModel() {
      BoundsConsumer bounds = new BoundsConsumer();
      mc.getEntityRenderDispatcher().getHeldItemRenderer().renderItem(mc.player, previewItem,
         ModelTransformationMode.FIRST_PERSON_RIGHT_HAND, false, new MatrixStack(), layer -> bounds, 15728880);
      modelCenter = bounds.center();
   }

   private void drawItem(UIContext context, float[] pose) {
      MatrixStack matrices = context.getMatrices();
      boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
      boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
      uiInverse.set(matrices.peek().getPositionMatrix()).invert();
      matrices.push();
      try {
         matrices.translate(x + width / 2 + panX, y + HEADER + (height - HEADER - FOOTER) / 2 + panY, 250);
         matrices.scale(zoom, -zoom, zoom);
         matrices.multiply(new Quaternionf().rotateX((float)Math.toRadians(pitch)).rotateY((float)Math.toRadians(yaw)));
         matrices.translate(cameraOffset.x, cameraOffset.y, cameraOffset.z);
         projection.set(uiInverse).mul(matrices.peek().getPositionMatrix());
         SwingTransformations.fromValues(pose).apply(matrices, false);
         matrices.translate(0.56F, -0.52F, -0.72F);
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.disableCull();
         // Scissored to the preview; the surrounding UI keeps its depth buffer.
         GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
         var consumers = mc.getBufferBuilders().getEntityVertexConsumers();
         mc.getEntityRenderDispatcher().getHeldItemRenderer().renderItem(mc.player, previewItem,
            ModelTransformationMode.FIRST_PERSON_RIGHT_HAND, false, matrices, consumers, 15728880);
         consumers.draw();
      } catch (RuntimeException failure) {
         previewFailed = true;
         BKClientInit.LOGGER.error("Could not render the swing editor preview", failure);
      } finally {
         matrices.pop();
         if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
         if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
         if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
         RenderSystem.depthMask(depthMask);
      }
   }

   private Vector3f itemCenter(float[] pose) {
      Vector3f center = modelCenter == null ? new Vector3f() : new Vector3f(modelCenter);
      center.add(0.56F - pose[0], -0.52F - pose[1], -0.72F - pose[2]);
      rotation(pose).transform(center);
      return center.add(pose[0] + pose[3], pose[1] + pose[4], pose[2] + pose[5]);
   }

   private static Quaternionf rotation(float[] pose) {
      return new Quaternionf().rotationXYZ((float)Math.toRadians(pose[6]), (float)Math.toRadians(pose[7]), (float)Math.toRadians(pose[8]));
   }

   private Vector3f cameraTarget() {
      float[] pose = selectedPhase().values();
      Vector3f target = itemCenter(pose);
      if (tool == Tool.PIVOT && phase != Phase.PREVIEW) target.add(pose[0] + pose[3], pose[1] + pose[4], pose[2] + pose[5]).mul(0.5F);
      return target.negate();
   }

   private void focusImmediately() {
      cameraOffset.set(cameraTarget());
      panX = panY = 0;
      centering = false;
      transitionPose = null;
   }

   private void beginRecenter() {
      centerFrom.set(cameraOffset);
      centerTo.set(cameraTarget());
      panFromX = panX; panFromY = panY;
      recenter.setValue(0);
      centering = true;
   }

   private float[] displayedPose() {
      if (phase == Phase.PREVIEW) {
         long duration = (long)Math.max(100, 300 * manager.getSpeed().getCurrentValue());
         long elapsed = System.currentTimeMillis() % (duration + 500);
         return manager.transformations(elapsed >= duration ? 0 : (float)elapsed / duration).values();
      }
      float[] pose = selectedPhase().values();
      if (centering && transitionPose != null) {
         for (int i = 0; i < pose.length; i++) pose[i] = MathHelper.lerp(recenter.getValue(), transitionPose[i], pose[i]);
      }
      return pose;
   }

   private Vec2f project(Vector3f point) {
      Vector3f result = projection.transformPosition(new Vector3f(point));
      return new Vec2f(result.x, result.y);
   }

   private void drawGizmo(UIContext context, float[] pose) {
      Vector3f origin = tool == Tool.PIVOT ? new Vector3f(pose[0] + pose[3], pose[1] + pose[4], pose[2] + pose[5]) : itemCenter(pose);
      gizmoCenter = project(origin);
      for (int axis = 0; axis < 3; axis++) {
         Vector3f tip = new Vector3f(origin);
         tip.setComponent(axis, tip.get(axis) + AXIS_LENGTH);
         axisEnds[axis] = project(tip);
         for (int i = 0; i < rings[axis].length; i++) {
            double angle = 2 * Math.PI * i / rings[axis].length;
            Vector3f point = new Vector3f(origin);
            int u = (axis + 1) % 3, v = (axis + 2) % 3;
            point.setComponent(u, point.get(u) + RING_RADIUS * (float)Math.cos(angle));
            point.setComponent(v, point.get(v) + RING_RADIUS * (float)Math.sin(angle));
            rings[axis][i] = project(point);
         }
      }
      gizmoReady = true;
      int hover = draggedAxis >= 0 ? draggedAxis : hitAxis(context.getMouseX(), context.getMouseY());
      boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      RenderSystem.disableDepthTest();
      try {
         for (int axis = 0; axis < 3; axis++) {
            ColorRGBA color = AXIS_COLORS[axis].mulAlpha(hover == axis ? 1 : tool == Tool.ROTATION ? 0.6F : 0.7F);
            if (tool == Tool.ROTATION) {
               // Four cubic arcs keep the ring smooth without a draw call per segment.
               for (int quarter = 0; quarter < 4; quarter++) {
                  double a = quarter * Math.PI / 2, b = a + Math.PI / 2;
                  float tangent = RING_RADIUS * 0.5522848F;
                  Vector3f start = ringPoint(origin, axis, a), end = ringPoint(origin, axis, b);
                  Vector3f first = new Vector3f(start).add(ringTangent(axis, a).mul(tangent));
                  Vector3f second = new Vector3f(end).sub(ringTangent(axis, b).mul(tangent));
                  SwingGizmoRenderer.bezier(context.getMatrices(), project(start), project(first), project(second), project(end), color);
               }
            } else {
               Vec2f tip = axisEnds[axis];
               SwingGizmoRenderer.line(context.getMatrices(), gizmoCenter, tip, color);
               float angle = (float)Math.atan2(tip.y - gizmoCenter.y, tip.x - gizmoCenter.x);
               for (float sign : new float[]{-1, 1}) {
                  SwingGizmoRenderer.line(context.getMatrices(), tip, new Vec2f(tip.x - 4 * (float)Math.cos(angle + sign * 0.42F), tip.y - 4 * (float)Math.sin(angle + sign * 0.42F)), color);
               }
            }
         }
         float radius = tool == Tool.PIVOT ? 2.5F : 1.5F;
         context.drawRoundedRect(gizmoCenter.x - radius, gizmoCenter.y - radius, radius * 2, radius * 2,
            BorderRadius.all(radius), (tool == Tool.PIVOT ? Colors.getAccent() : ColorRGBA.WHITE).mulAlpha(0.9F));
      } finally {
         if (depth) RenderSystem.enableDepthTest();
      }
      if (hover >= 0) CursorUtility.set(CursorType.HAND);
   }

   private static Vector3f ringPoint(Vector3f origin, int axis, double angle) {
      int u = (axis + 1) % 3, v = (axis + 2) % 3;
      Vector3f point = new Vector3f(origin);
      return point.setComponent(u, point.get(u) + RING_RADIUS * (float)Math.cos(angle))
         .setComponent(v, point.get(v) + RING_RADIUS * (float)Math.sin(angle));
   }

   private static Vector3f ringTangent(int axis, double angle) {
      return new Vector3f().setComponent((axis + 1) % 3, -(float)Math.sin(angle))
         .setComponent((axis + 2) % 3, (float)Math.cos(angle));
   }

   private int hitAxis(double mx, double my) {
      if (!gizmoReady || !inViewport(mx, my)) return -1;
      int result = -1;
      float closest = 6;
      for (int axis = 0; axis < 3; axis++) {
         if (tool == Tool.ROTATION) {
            for (int i = 0; i < rings[axis].length; i++) {
               float d = segmentDistance(mx, my, rings[axis][i], rings[axis][(i + 1) % rings[axis].length]);
               if (d < closest) { closest = d; result = axis; }
            }
         } else {
            float d = segmentDistance(mx, my, gizmoCenter, axisEnds[axis]);
            if (d < closest) { closest = d; result = axis; }
         }
      }
      return result;
   }

   static float segmentDistance(double mx, double my, Vec2f a, Vec2f b) {
      double dx = b.x - a.x, dy = b.y - a.y, length = dx * dx + dy * dy;
      double t = length < 1E-5 ? 0 : MathHelper.clamp(((mx - a.x) * dx + (my - a.y) * dy) / length, 0, 1);
      return (float)Math.hypot(mx - a.x - t * dx, my - a.y - t * dy);
   }

   private boolean inViewport(double mx, double my) {
      return GuiUtility.isHovered(x, y + HEADER, width, height - HEADER - FOOTER, mx, my);
   }

   @Override
   public void onMouseClicked(double mx, double my, MouseButton button) {
      if (!isHovered(mx, my)) return;
      captureHistory();
      if (inViewport(mx, my)) {
         centering = false;
         transitionPose = null;
         if (button == MouseButton.MIDDLE || button == MouseButton.LEFT && GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            panning = true;
         } else if (button == MouseButton.LEFT) {
            draggedAxis = phase == Phase.PREVIEW ? -1 : hitAxis(mx, my);
            if (draggedAxis < 0) { orbiting = true; return; }
            dragX = mx; dragY = my;
            dragPose = selectedPhase().values();
            dragValue = axisSetting(draggedAxis).getCurrentValue();
            dragDirection = new Vec2f(axisEnds[draggedAxis].x - gizmoCenter.x, axisEnds[draggedAxis].y - gizmoCenter.y);
            rotationCenter = gizmoCenter;
            previousAngle = (float)Math.atan2(my - rotationCenter.y, mx - rotationCenter.x);
            accumulatedAngle = 0;
            rotationFocus.set(cameraOffset).add(itemCenter(dragPose));
         }
         return;
      }
      if (button != MouseButton.LEFT) return;
      for (int i = 0; i < 3; i++) {
         if (GuiUtility.isHovered(phaseX[i], y + 20, phaseWidth[i], 11, mx, my)) {
            float[] previous = displayedPose();
            phase = Phase.values()[i];
            beginRecenter();
            transitionPose = phase == Phase.PREVIEW ? null : previous;
            return;
         }
         if (phase != Phase.PREVIEW && GuiUtility.isHovered(toolX(i), toolbarY(), 16, 16, mx, my)) {
            selectTool(Tool.values()[i]);
            return;
         }
      }
      if (GuiUtility.isHovered(x + width - 25, toolbarY(), 16, 16, mx, my)) {
         manager.restore(new float[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0.5F,1,0.5F,0,1,2});
         captureHistory();
         beginRecenter();
      }
   }

   public void onMouseDragged(double mx, double my, MouseButton button, double dx, double dy) {
      if (panning) { panX += (float)dx; panY += (float)dy; }
      else if (orbiting) { yaw += (float)dx * 0.6F; pitch = MathHelper.clamp(pitch + (float)dy * 0.6F, -89, 89); }
      else if (draggedAxis >= 0) {
         if (tool == Tool.ROTATION) {
            float angle = (float)Math.atan2(my - rotationCenter.y, mx - rotationCenter.x);
            accumulatedAngle += MathHelper.wrapDegrees((float)Math.toDegrees(angle - previousAngle));
            previousAngle = angle;
            axisSetting(draggedAxis).setCurrentValue(dragValue + accumulatedAngle);
         } else {
            float lengthSquared = dragDirection.x * dragDirection.x + dragDirection.y * dragDirection.y;
            if (lengthSquared < 1E-5) return;
            float distance = (float)((mx - dragX) * dragDirection.x + (my - dragY) * dragDirection.y) * AXIS_LENGTH / lengthSquared;
            if (tool == Tool.POSITION) axisSetting(draggedAxis).setCurrentValue(dragValue + distance);
            else movePivot(distance);
         }
         if (Screen.hasShiftDown() || Screen.hasControlDown()) copyToolToOtherPhase();
      }
   }

   private void movePivot(float distance) {
      Vector3f worldDelta = new Vector3f().setComponent(draggedAxis, distance);
      Vector3f localDelta = rotation(dragPose).conjugate().transform(new Vector3f(worldDelta));
      SliderSetting[] sliders = selectedPhase().sliders();
      for (int i = 0; i < 3; i++) {
         sliders[i].setCurrentValue(dragPose[i] + localDelta.get(i));
         // Compensate the translation so moving the pivot does not move the item.
         sliders[i + 3].setCurrentValue(dragPose[i + 3] + worldDelta.get(i) - localDelta.get(i));
      }
   }

   private SliderSetting axisSetting(int axis) {
      return selectedPhase().sliders()[(tool == Tool.POSITION ? 3 : tool == Tool.ROTATION ? 6 : 0) + axis];
   }

   private void copyToolToOtherPhase() {
      SwingPhase other = phase == Phase.END ? manager.getStartPhase() : manager.getEndPhase();
      float[] source = selectedPhase().values(), target = other.values();
      int from = tool == Tool.POSITION ? 3 : tool == Tool.ROTATION ? 6 : 0;
      int count = tool == Tool.PIVOT ? 6 : 3;
      System.arraycopy(source, from, target, from, count);
      other.restore(target);
   }

   @Override
   public void onMouseReleased(double mx, double my, MouseButton button) {
      draggedAxis = -1; panning = orbiting = false;
      captureHistory();
   }

   @Override
   public void onScroll(double mx, double my, double horizontal, double vertical) {
      if (!inViewport(mx, my)) return;
      centering = false;
      float next = MathHelper.clamp(zoom * (1 + (float)vertical * 0.1F), 30, 280), ratio = next / zoom;
      panX += ((float)mx - (x + width / 2 + panX)) * (1 - ratio);
      panY += ((float)my - (y + HEADER + (height - HEADER - FOOTER) / 2 + panY)) * (1 - ratio);
      zoom = next;
   }

   @Override
   public void onKeyPressed(int key, int scanCode, int modifiers) {
      if (Screen.hasControlDown()) {
         if (key == GLFW.GLFW_KEY_Z) { if (Screen.hasShiftDown()) redo(); else undo(); }
         else if (key == GLFW.GLFW_KEY_Y) redo();
      } else {
         switch (key) {
            case GLFW.GLFW_KEY_F -> focusImmediately();
            case GLFW.GLFW_KEY_G -> selectTool(Tool.POSITION);
            case GLFW.GLFW_KEY_R -> selectTool(Tool.ROTATION);
            case GLFW.GLFW_KEY_T -> selectTool(Tool.PIVOT);
         }
      }
   }

   private void selectTool(Tool next) {
      if (tool != next) { tool = next; beginRecenter(); }
   }

   private void captureHistory() {
      if (draggedAxis >= 0 || mouseDown()) return;
      float[] snapshot = manager.snapshot();
      // History belongs to the selected file; undo must not overwrite another preset.
      if (!java.util.Objects.equals(lastPreset, manager.getCurrent())) {
         lastPreset = manager.getCurrent();
         lastSnapshot = snapshot;
         undo.clear(); redo.clear();
         return;
      }
      if (!Arrays.equals(snapshot, lastSnapshot)) {
         undo.push(lastSnapshot);
         if (undo.size() > 100) undo.removeLast();
         redo.clear();
         lastSnapshot = snapshot;
      }
   }

   private boolean mouseDown() {
      long window = mc.getWindow().getHandle();
      return GLFW.glfwGetMouseButton(window, 0) == GLFW.GLFW_PRESS
         || GLFW.glfwGetMouseButton(window, 1) == GLFW.GLFW_PRESS
         || GLFW.glfwGetMouseButton(window, 2) == GLFW.GLFW_PRESS;
   }

   private void undo() {
      captureHistory();
      if (!undo.isEmpty()) {
         redo.push(manager.snapshot());
         manager.restore(undo.pop());
         lastSnapshot = manager.snapshot();
         beginRecenter();
      }
   }

   private void redo() {
      captureHistory();
      if (!redo.isEmpty()) {
         undo.push(manager.snapshot());
         manager.restore(redo.pop());
         lastSnapshot = manager.snapshot();
         beginRecenter();
      }
   }

   private static final class BoundsConsumer implements VertexConsumer {
      private final Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE);
      @Override public VertexConsumer vertex(float x, float y, float z) { Vector3f p = new Vector3f(x, y, z); min.min(p); max.max(p); return this; }
      @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
      @Override public VertexConsumer texture(float u, float v) { return this; }
      @Override public VertexConsumer overlay(int u, int v) { return this; }
      @Override public VertexConsumer light(int u, int v) { return this; }
      @Override public VertexConsumer normal(float x, float y, float z) { return this; }
      Vector3f center() { return min.x > max.x ? new Vector3f() : new Vector3f(min).add(max).mul(0.5F); }
   }
}
