package dev.bkclient.mixin.minecraft.client.gui.screen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.systems.modules.modules.visuals.Beautifully;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class BeautifulInventoryMixin {
   @Shadow protected int x;
   @Shadow protected int y;
   @Shadow protected int backgroundWidth;
   @Shadow protected int backgroundHeight;
   @Unique private long bk$openedAt;
   @Inject(method = "init", at = @At("TAIL"))
   private void bk$open(CallbackInfo ci) { bk$openedAt = System.currentTimeMillis(); }
   @Unique private float bk$progress() {
      return Beautifully.inventoryAnimation() ? Beautifully.progress(bk$openedAt) : 1;
   }
   @Unique private float bk$ease(float p) { return Beautifully.OPEN.ease(p, 0, 1, 1); }
   @Unique private float bk$scale(float p) { return 0.88F + 0.12F * bk$ease(p); }
   @Unique private double bk$mouseX(double mx, float p) {
      float cx = x + backgroundWidth / 2.0F;
      return cx + (mx - cx) / bk$scale(p);
   }
   @Unique private double bk$mouseY(double my, float p) {
      float cy = y + backgroundHeight / 2.0F;
      return cy + (my - cy - (1 - bk$ease(p)) * 10) / bk$scale(p);
   }
   @Unique private void bk$draw(DrawContext context, float p, Runnable render) {
      if (p >= 1) { render.run(); return; }
      context.draw();
      float[] color = RenderSystem.getShaderColor().clone();
      context.getMatrices().push();
      try {
         float cx = x + backgroundWidth / 2.0F, cy = y + backgroundHeight / 2.0F;
         context.getMatrices().translate(cx, cy + (1 - bk$ease(p)) * 10, 0);
         context.getMatrices().scale(bk$scale(p), bk$scale(p), 1);
         context.getMatrices().translate(-cx, -cy, 0);
         RenderSystem.setShaderColor(1, 1, 1, Math.clamp(p * 2, 0, 1));
         render.run();
         context.draw();
      } finally {
         context.getMatrices().pop();
         RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
      }
   }
   @WrapOperation(method = "renderBackground", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V"))
   private void bk$background(HandledScreen<?> screen, DrawContext context, float delta, int mx, int my, Operation<Void> original) {
      float p = bk$progress();
      bk$draw(context, p, () -> original.call(screen, context, delta, (int)bk$mouseX(mx, p), (int)bk$mouseY(my, p)));
   }
   @WrapMethod(method = "render")
   private void bk$content(DrawContext context, int mx, int my, float delta, Operation<Void> original) {
      float p = bk$progress();
      bk$draw(context, p, () -> original.call(context, (int)bk$mouseX(mx, p), (int)bk$mouseY(my, p), delta));
   }
   @WrapMethod(method = "mouseClicked")
   private boolean bk$click(double mx, double my, int button, Operation<Boolean> original) {
      float p = bk$progress();
      return original.call(bk$mouseX(mx, p), bk$mouseY(my, p), button);
   }
   @WrapMethod(method = "mouseReleased")
   private boolean bk$release(double mx, double my, int button, Operation<Boolean> original) {
      float p = bk$progress();
      return original.call(bk$mouseX(mx, p), bk$mouseY(my, p), button);
   }
   @WrapMethod(method = "mouseDragged")
   private boolean bk$drag(double mx, double my, int button, double dx, double dy, Operation<Boolean> original) {
      float p = bk$progress();
      return original.call(bk$mouseX(mx, p), bk$mouseY(my, p), button, dx / bk$scale(p), dy / bk$scale(p));
   }
}
