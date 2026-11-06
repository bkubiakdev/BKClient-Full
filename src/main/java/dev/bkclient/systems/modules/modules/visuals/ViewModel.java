package dev.bkclient.systems.modules.modules.visuals;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.HandRenderEvent;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.event.impl.window.MouseEvent;
import dev.bkclient.systems.event.impl.window.MouseScrollEvent;
import dev.bkclient.systems.file.ClientFile;
import dev.bkclient.systems.file.api.FileInfo;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.shader.HandShaderHelper;
import dev.bkclient.utility.render.shader.Shader;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.opengl.GL13C.*;

@ModuleInfo(name = "View Model", category = ModuleCategory.VISUALS, desc = "modules.descriptions.view_model")
public class ViewModel extends BaseModule implements IMinecraft {
   private static ViewModel instance;
   private static final long OUTLINE_MS = 220L;
   private static final long DOUBLE_CLICK_MS = 300L;
   private static final float SCALE_STEP = 1.08F;
   private static final float SCALE_MIN = 0.2F;
   private static final float SCALE_MAX = 5.0F;
   private static final float FOLLOW = 0.45F;

   private float mainOffsetX;
   private float mainOffsetY;
   private float offOffsetX;
   private float offOffsetY;

   private float mainTargetX;
   private float mainTargetY;
   private float offTargetX;
   private float offTargetY;

   private float mainScale = 1.0F;
   private float offScale = 1.0F;
   private final Animation mainScaleAnim = new Animation(200L, 1.0F, Easing.CUBIC_OUT);
   private final Animation offScaleAnim = new Animation(200L, 1.0F, Easing.CUBIC_OUT);

   private Hand activeHand;
   private boolean holding;
   private long lastClickMs;
   private float grabCursorX;
   private float grabCursorY;
   private float grabTargetX;
   private float grabTargetY;
   private float[] grabJacobian;
   private int grabWidth, grabHeight;
   private double grabGuiScale;
   private Hand lastClickHand;
   private float lastClickX, lastClickY;
   private net.minecraft.client.gui.screen.Screen editingScreen;
   private final float[] mainBounds = new float[4];
   private final float[] offBounds = new float[4];

   private Hand outlineHand;
   private final Animation outlineAnim = new Animation(OUTLINE_MS, 0.0F, Easing.CUBIC_OUT);

   private boolean loaded;

   private Matrix4f mainA;
   private Matrix4f offA;
   private float mainPivotX;
   private float mainPivotY;
   private float offPivotX;
   private float offPivotY;
   private boolean mainPivotValid;
   private boolean offPivotValid;

   public ViewModel() {
      instance = this;
   }

   public void beginHandFrame() {
      this.resetFrame();
   }

   public static ViewModel getInstance() {
      return instance;
   }

   private static boolean inChat() {
      return mc.currentScreen instanceof ChatScreen;
   }

   private void load() {
      if (loaded) {
         return;
      }
      loaded = true;
      ViewModelFile file = (ViewModelFile) BKClientInit.getInstance().getFileManager().getClientFile("viewmodel");
      if (file == null) {
         return;
      }
      this.applyFileValues(file);
   }

   private void applyFileValues(ViewModelFile file) {
      this.mainOffsetX = this.mainTargetX = finiteOffset(file.mainX);
      this.mainOffsetY = this.mainTargetY = finiteOffset(file.mainY);
      this.offOffsetX = this.offTargetX = finiteOffset(file.offX);
      this.offOffsetY = this.offTargetY = finiteOffset(file.offY);
      this.mainScale = clamp(file.mainScale, SCALE_MIN, SCALE_MAX);
      this.offScale = clamp(file.offScale, SCALE_MIN, SCALE_MAX);
      this.mainScaleAnim.setValue(this.mainScale);
      this.offScaleAnim.setValue(this.offScale);
   }

   private static float finiteOffset(float value) { return Float.isFinite(value) ? Math.clamp(value, -10F, 10F) : 0F; }

   private void save() {
      BKClientInit.getInstance().getFileManager().writeFile("viewmodel");
   }

   private static float clamp(float f, float min, float max) {
      return Float.isFinite(f) ? Math.max(min, Math.min(max, f)) : Math.max(min, Math.min(max, 1.0F));
   }

   @Override
   public void onEnable() {
      this.lastClickMs = 0L;
      this.lastClickHand = null;
      this.editingScreen = null;
      this.resetFrame();
      HandShaderHelper.retryAfterFailure();
      this.load();
      this.mainScaleAnim.setValue(this.mainScale);
      this.offScaleAnim.setValue(this.offScale);
      this.outlineAnim.setValue(0.0F);
   }

   @Override
   public void onDisable() {
      this.finishEdit();
      this.resetFrame();
      this.outlineHand = null;
      this.outlineAnim.setValue(0F);
   }

   private void finishEdit() {
      boolean changed = this.holding;
      this.activeHand = null;
      this.holding = false;
      this.grabJacobian = null;
      if (changed) this.save();
   }

   public void resetLayout() {
      this.loaded = true;
      this.lastClickMs = 0L;
      this.lastClickHand = null;
      this.grabJacobian = null;
      this.holding = false;
      this.activeHand = null;
      this.mainOffsetX = this.mainOffsetY = this.mainTargetX = this.mainTargetY = 0.0F;
      this.offOffsetX = this.offOffsetY = this.offTargetX = this.offTargetY = 0.0F;
      this.mainScale = 1.0F;
      this.offScale = 1.0F;
      this.mainScaleAnim.setValue(1.0F);
      this.offScaleAnim.setValue(1.0F);
      this.save();
   }

   public static float outlineAlpha(Hand hand) {
      ViewModel vm = getInstance();
      if (vm == null || vm.outlineHand != hand) {
         return 0.0F;
      }
      return vm.outlineAnim.getValue() / 1.0F;
   }

   private void resetFrame() {
      this.mainPivotValid = false;
      this.offPivotValid = false;
   }

   private final EventListener<HandRenderEvent> onHandRender = event -> applyTransform(event.getArm(), event.getMatrices());

   /** Applies the ordinary hand/item layout; authored cosmetic rigs bypass this pass. */
   public void applyTransform(Arm arm, MatrixStack matrices) {
      this.load();
      this.captureTransform(arm, matrices.peek().getPositionMatrix());
      if (!this.isEnabled()) {
         return;
      }

      boolean isMain = mc.player != null && arm == mc.player.getMainArm();
      float dir = arm == Arm.RIGHT ? 1.0F : -1.0F;
      float scale = isMain ? this.mainScaleAnim.getValue() : this.offScaleAnim.getValue();

      float x = isMain ? this.mainOffsetX : this.offOffsetX;
      float y = isMain ? this.mainOffsetY : this.offOffsetY;
      matrices.translate(x, y, 0.0F);
      Matrix4f projected = new Matrix4f(RenderSystem.getProjectionMatrix())
         .mul(RenderSystem.getModelViewMatrix()).mul(matrices.peek().getPositionMatrix());
      if (scale != 1.0F) {
         // Resize the projected image, preserving its depth and proportions. Scaling
         // XYZ clips through the camera; scaling just local XY flattens angled items.
         Vector4f pivot = projected.transform(new Vector4f(dir * .56F, -.52F, -.72F, 1F));
         if (Float.isFinite(pivot.x) && Float.isFinite(pivot.y) && Float.isFinite(pivot.w)
               && pivot.w > 1.0E-5F && Float.isFinite(projected.determinant()) && projected.determinant() != 0F) {
            Matrix4f screenScale = new Matrix4f()
               .translate((1F-scale)*pivot.x/pivot.w, (1F-scale)*pivot.y/pivot.w, 0F)
               .scale(scale, scale, 1F);
            Matrix4f localScale = new Matrix4f(projected).invert().mul(screenScale).mul(projected);
            matrices.multiplyPositionMatrix(localScale);
            projected.mul(localScale);
         }
      }
      this.captureBounds(projected, isMain, dir);
   }

   private void captureTransform(Arm arm, Matrix4f positionMatrix) {
      Window window = mc.getWindow();
      if (window == null) {
         return;
      }
      float w = window.getFramebufferWidth();
      float h = window.getFramebufferHeight();
      if (w < 1.0F || h < 1.0F) {
         return;
      }
      Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
      Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix());
      Matrix4f m = projection.mul(mv).mul(positionMatrix);
      boolean isMain = mc.player != null && arm == mc.player.getMainArm();
      if (isMain) {
         this.mainA = m;
      } else {
         this.offA = m;
      }
   }

   private void captureBounds(Matrix4f m, boolean isMain, float dir) {
      float w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
      float[] bounds = isMain ? mainBounds : offBounds;
      float[] out = new float[2];
      bounds[0] = bounds[1] = Float.POSITIVE_INFINITY;
      bounds[2] = bounds[3] = Float.NEGATIVE_INFINITY;
      // A bounded hand/item editing region, transformed with the visible model.
      for (float x : new float[]{dir * .56F - .28F, dir * .56F + .28F}) {
         for (float y : new float[]{-.75F, .10F}) {
            if (!project(m, x, y, -.72F, w, h, out)) return;
            bounds[0] = Math.min(bounds[0], out[0]); bounds[1] = Math.min(bounds[1], out[1]);
            bounds[2] = Math.max(bounds[2], out[0]); bounds[3] = Math.max(bounds[3], out[1]);
         }
      }
      if (!project(m, dir * .56F, -.52F, -.72F, w, h, out)) return;
      if (isMain) { mainPivotX=out[0]; mainPivotY=out[1]; mainPivotValid=true; }
      else { offPivotX=out[0]; offPivotY=out[1]; offPivotValid=true; }
   }

   private static boolean project(Matrix4f m, float x, float y, float z, float w, float h, float[] out) {
      if (m == null) return false;
      Vector4f v = m.transform(new Vector4f(x, y, z, 1.0F));
      if (!Float.isFinite(v.w) || v.w <= 1.0E-5F) return false;
      out[0] = (v.x / v.w * .5F + .5F) * w;
      out[1] = (1F - (v.y / v.w * .5F + .5F)) * h;
      return Float.isFinite(out[0]) && Float.isFinite(out[1]);
   }

   private static float dist2(float x1, float y1, float x2, float y2) {
      float dx = x1 - x2;
      float dy = y1 - y2;
      return dx * dx + dy * dy;
   }

   private final EventListener<HudRenderEvent> onHud = event -> {
      if (!this.isEnabled() || !inChat() || this.editingScreen != mc.currentScreen
            || this.holding && (grabWidth != mc.getWindow().getScaledWidth()
               || grabHeight != mc.getWindow().getScaledHeight() || grabGuiScale != mc.getWindow().getScaleFactor())) {
         this.finishEdit();
         this.lastClickMs = 0L;
         this.lastClickHand = null;
         this.editingScreen = mc.currentScreen;
      }

      boolean holdingActive = this.holding && this.activeHand != null;
      if (this.isEnabled() && holdingActive) {
         this.outlineHand = this.activeHand;
      }

      this.outlineAnim.update(holdingActive ? 1.0F : 0.0F);
      this.mainScaleAnim.update(this.mainScale);
      this.offScaleAnim.update(this.offScale);

      if (this.isEnabled() && holdingActive) {
         this.dragTowardsCursor();
      }

      boolean isMain = this.activeHand == Hand.MAIN_HAND;
      if (holdingActive && isMain) {
         this.mainOffsetX = this.mainTargetX;
         this.mainOffsetY = this.mainTargetY;
      } else if (holdingActive) {
         this.offOffsetX = this.offTargetX;
         this.offOffsetY = this.offTargetY;
      } else {
         this.mainOffsetX += (this.mainTargetX - this.mainOffsetX) * FOLLOW;
         this.mainOffsetY += (this.mainTargetY - this.mainOffsetY) * FOLLOW;
         this.offOffsetX += (this.offTargetX - this.offOffsetX) * FOLLOW;
         this.offOffsetY += (this.offTargetY - this.offOffsetY) * FOLLOW;
      }
   };

   private void dragTowardsCursor() {
      if (grabJacobian == null) { finishEdit(); return; }
      float mx = mouseX(), my = mouseY();
      float[] j = grabJacobian;
      float det = j[0] * j[3] - j[2] * j[1];
      float dx = mx - grabCursorX, dy = my - grabCursorY;
      float nx = finiteOffset(grabTargetX + (j[3] * dx - j[2] * dy) / det);
      float ny = finiteOffset(grabTargetY + (-j[1] * dx + j[0] * dy) / det);
      if (activeHand == Hand.MAIN_HAND) { mainTargetX=nx; mainTargetY=ny; }
      else { offTargetX=nx; offTargetY=ny; }
   }

   private float[] dragJacobian(Hand hand) {
      boolean main = hand == Hand.MAIN_HAND;
      Matrix4f m = main ? mainA : offA;
      float dir = (main ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite()) == Arm.RIGHT ? 1F : -1F;
      float x = dir * .56F + (main ? mainOffsetX : offOffsetX);
      float y = -.52F + (main ? mainOffsetY : offOffsetY);
      float w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
      float[] p = new float[2], px = new float[2], py = new float[2];
      if (!project(m,x,y,-.72F,w,h,p) || !project(m,x+.01F,y,-.72F,w,h,px)
            || !project(m,x,y+.01F,-.72F,w,h,py)) return null;
      float[] j = {(px[0]-p[0])*100F,(px[1]-p[1])*100F,(py[0]-p[0])*100F,(py[1]-p[1])*100F};
      float det=j[0]*j[3]-j[2]*j[1];
      return Float.isFinite(det) && Math.abs(det)>1F ? j : null;
   }

   private static float mouseX() { return (float)(mc.mouse.getX()*mc.getWindow().getScaledWidth()/mc.getWindow().getWidth()); }
   private static float mouseY() { return (float)(mc.mouse.getY()*mc.getWindow().getScaledHeight()/mc.getWindow().getHeight()); }

   private boolean blockedByUi(float x, float y) {
      var hud = BKClientInit.getInstance().getHud();
      return y >= mc.getWindow().getScaledHeight()-16 || mc.options.hudHidden || mc.player == null
         || !mc.options.getPerspective().isFirstPerson()
         || dev.bkclient.ui.hud.impl.SuggestionPopup.isOpen()
         || hud != null && hud.blocksViewModelEdit(x,y);
   }

   /**
    * The HUD, so the depth buffer still contains the held item. Applies a
    * depth-based outline/glow only on the side of the currently grabbed hand.
    */
   public void renderHandOutline() {
      if (!this.isEnabled() || this.outlineHand == null) {
         return;
      }
      float alpha = this.outlineAnim.getValue();
      if (alpha <= 0.001F) {
         return;
      }
      if (!HandShaderHelper.isInitialized()) {
         HandShaderHelper.initIfNeeded();
         if (!HandShaderHelper.isInitialized()) return;
      }
      HandShaderHelper.checkFramebuffers();
      Shader shader = HandShaderHelper.getViewModelOutline();
      if (shader == null) {
         return;
      }

      try {
         Framebuffer mainFbo = mc.getFramebuffer();
         Framebuffer copyFbo = HandShaderHelper.getCopyFbo();
         if (mainFbo == null || copyFbo == null) {
            return;
         }

         // Copy current frame (scene + hand) with a GPU blit instead of another
         // full-screen textured draw.
         HandShaderHelper.blitColorExact(mainFbo, copyFbo);
         copyFbo.copyDepthFrom(mainFbo);

         mainFbo.beginWrite(true);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableDepthTest();

         RenderSystem.activeTexture(GL_TEXTURE0);
         RenderSystem.bindTexture(copyFbo.getColorAttachment());
         RenderSystem.activeTexture(GL_TEXTURE1);
         RenderSystem.bindTexture(copyFbo.getDepthAttachment());

         boolean isMain = this.outlineHand == Hand.MAIN_HAND;
         float side = mc.player != null && (isMain ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite()) == Arm.RIGHT ? 1.0F : -1.0F;
         ColorRGBA color = new ColorRGBA(100.0F, 200.0F, 255.0F, 255.0F);

         Vector3f rgb = new Vector3f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);

         shader.bind();
         shader.setUniform1i("ColorTexture", 0);
         shader.setUniform1i("DepthTexture", 1);
         shader.setUniform1f("uAlpha", alpha);
         shader.setUniform1f("uSide", side);
         shader.setUniform3f("uColor", rgb);
         HandShaderHelper.drawFullScreenQuad();
         shader.unbind();

         RenderSystem.activeTexture(GL_TEXTURE0);
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
         mc.getFramebuffer().beginWrite(true);
      } catch (Exception ignored) {
      }
   }

   private final EventListener<MouseEvent> onMouse = event -> {
      if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
      if (event.getAction() == GLFW.GLFW_RELEASE) {
         if (holding) { finishEdit(); event.cancel(); }
         return;
      }
      if (!isEnabled() || !inChat() || event.isCancelled() || event.getAction() != GLFW.GLFW_PRESS) return;
      float mx=mouseX(), my=mouseY();
      if (blockedByUi(mx,my)) { lastClickMs=0L; lastClickHand=null; finishEdit(); return; }
      Hand picked=pickHand(mx,my);
      if (picked==null) { lastClickMs=0L; lastClickHand=null; return; }
      long now=System.currentTimeMillis();
      boolean doubleClick=editingScreen==mc.currentScreen && picked==lastClickHand
         && now-lastClickMs<=DOUBLE_CLICK_MS && dist2(mx,my,lastClickX,lastClickY)<=36F;
      editingScreen=mc.currentScreen;
      lastClickMs=now; lastClickHand=picked; lastClickX=mx; lastClickY=my;
      if (!doubleClick) return;
      grabJacobian=dragJacobian(picked);
      if (grabJacobian==null) return;
      activeHand=picked; holding=true; lastClickMs=0L;
      grabCursorX=mx; grabCursorY=my;
      boolean main=picked==Hand.MAIN_HAND;
      grabTargetX=main?mainOffsetX:offOffsetX; grabTargetY=main?mainOffsetY:offOffsetY;
      grabWidth=mc.getWindow().getScaledWidth(); grabHeight=mc.getWindow().getScaledHeight();
      grabGuiScale=mc.getWindow().getScaleFactor();
      event.cancel();
   };

   private static boolean inside(float[] bounds,float x,float y) {
      return x>=bounds[0] && x<=bounds[2] && y>=bounds[1] && y<=bounds[3];
   }

   private Hand pickHand(float mx,float my) {
      boolean main=mainPivotValid && inside(mainBounds,mx,my);
      boolean off=offPivotValid && inside(offBounds,mx,my);
      if (main && off) return dist2(mx,my,mainPivotX,mainPivotY)<=dist2(mx,my,offPivotX,offPivotY)?Hand.MAIN_HAND:Hand.OFF_HAND;
      return main?Hand.MAIN_HAND:off?Hand.OFF_HAND:null;
   }

   private final EventListener<MouseScrollEvent> onScroll = event -> {
      if (!isEnabled() || !inChat() || !holding || activeHand==null || event.isCancelled()
            || blockedByUi(mouseX(),mouseY())) return;
      double amount=event.getVerticalAmount();
      if (!Double.isFinite(amount) || amount==0.0) return;
      // Equal proportional steps, including fractional touchpad input, with bounded bursts.
      float factor=(float)Math.pow(SCALE_STEP,Math.clamp(amount,-4.0,4.0));
      if (activeHand==Hand.MAIN_HAND) mainScale=clamp(mainScale*factor,SCALE_MIN,SCALE_MAX);
      else offScale=clamp(offScale*factor,SCALE_MIN,SCALE_MAX);
      // Persist once on release/close instead of writing a file for every wheel event.
      event.cancel();
   };

   @FileInfo(name = "viewmodel")
   public static class ViewModelFile extends ClientFile {
      public float mainX;
      public float mainY;
      public float offX;
      public float offY;
      public float mainScale = 1.0F;
      public float offScale = 1.0F;

      @Override
      public void write() {
         ViewModel vm = getInstance();
         if (vm == null) {
            vm = new ViewModel();
         }
         vm.load();
         JsonObject json = new JsonObject();
         json.addProperty("mainX", vm.mainTargetX);
         json.addProperty("mainY", vm.mainTargetY);
         json.addProperty("offX", vm.offTargetX);
         json.addProperty("offY", vm.offTargetY);
         json.addProperty("mainScale", vm.mainScale);
         json.addProperty("offScale", vm.offScale);
         try {
            dev.bkclient.systems.file.SafeJsonFile.write(this.file.toPath(), json);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      @Override
      public void read() {
         try {
            JsonObject json = dev.bkclient.systems.file.SafeJsonFile.read(this.file.toPath());
            if (json == null) {
               return;
            }
            this.mainX = json.has("mainX") ? json.get("mainX").getAsFloat() : 0.0F;
            this.mainY = json.has("mainY") ? json.get("mainY").getAsFloat() : 0.0F;
            this.offX = json.has("offX") ? json.get("offX").getAsFloat() : 0.0F;
            this.offY = json.has("offY") ? json.get("offY").getAsFloat() : 0.0F;
            this.mainScale = json.has("mainScale") ? json.get("mainScale").getAsFloat() : 1.0F;
            this.offScale = json.has("offScale") ? json.get("offScale").getAsFloat() : 1.0F;

            // Client files are loaded after modules are enabled. ViewModel.load()
            // can therefore already be marked as loaded when this file is read;
            // apply the freshly parsed values to the live module as well.
            ViewModel vm = getInstance();
            if (vm != null) {
               vm.loaded = true;
               vm.applyFileValues(this);
            }
         } catch (Exception e) {
            // Missing file: fall back to defaults.
         }
      }
   }
}
