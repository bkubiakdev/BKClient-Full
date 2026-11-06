package dev.bkclient.ui.hud.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.ui.hud.HudList;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.ui.hud.components.HudValueRow;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.batching.Batching;
import dev.bkclient.utility.render.batching.impl.RoundedRectBatching;
import net.minecraft.item.ItemStack;

/** Live inventory slots 9–35, using the shared BK HUD panel and typography. */
public final class InventoryHud extends HudList {
   private final dev.bkclient.systems.setting.settings.BooleanSetting slotBackgrounds = new dev.bkclient.systems.setting.settings.BooleanSetting(this,"hud.inventory.slot_backgrounds").enable();
   private final dev.bkclient.systems.setting.settings.BooleanSetting itemCounts = new dev.bkclient.systems.setting.settings.BooleanSetting(this,"hud.inventory.item_counts").enable();
   private final dev.bkclient.systems.setting.settings.BooleanSetting overlays = new dev.bkclient.systems.setting.settings.BooleanSetting(this,"hud.inventory.overlays").enable();
   private static final int COLUMNS = 9;
   private static final int ROWS = 3;
   private static final float PADDING = 6.0F;
   private static final float SLOT_SIZE = 16.0F;
   private static final float GAP = 2.0F;
   private static final float ITEM_SIZE = 12.0F;
   private static final BorderRadius SLOT_RADIUS = BorderRadius.all(3.0F);

   public InventoryHud() {
      super("hud.inventory", "icons/hud/hotbar.png");
   }

   @Override
   public void update(UIContext context) {
      this.width = PADDING * 2 + COLUMNS * SLOT_SIZE + (COLUMNS - 1) * GAP;
      this.height = listTop(HudValueRow.contentTop()) + ROWS * SLOT_SIZE + (ROWS - 1) * listStride(GAP) + PADDING;
      super.update(context);
   }

   @Override
   protected void renderContent(UIContext context) {
      super.renderContent(context);
      if (mc.player == null) return;

      float alpha = Math.clamp(this.animation.getValue() * this.visible.getValue(), 0.0F, 1.0F);
      float top = this.y + listTop(HudValueRow.contentTop());
      ColorRGBA slotColor = HudTheme.surfaceColor().withAlpha(110.0F * alpha);
      if (slotBackgrounds.isEnabled()) drawSlotBackgrounds(context, top, slotColor);

      for (int row = 0; row < ROWS; row++) {
         for (int column = 0; column < COLUMNS; column++) {
            ItemStack stack = mc.player.getInventory().getStack(9 + row * COLUMNS + column);
            if (stack.isEmpty()) continue;
            float slotX = this.x + PADDING + column * (SLOT_SIZE + GAP);
            float slotY = top + row * listStride(SLOT_SIZE + GAP);
            drawStack(context, stack, slotX, slotY, alpha);
         }
      }
   }

   private void drawSlotBackgrounds(UIContext context, float top, ColorRGBA color) {
      // All 27 slots share size, radius and shader uniforms. Flush their batch
      // before item rendering, which uses different render layers and buffers.
      if (Batching.getActive() != null) {
         drawSlotGrid(context, top, color);
         return;
      }
      boolean cull = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
      RoundedRectBatching batch = new RoundedRectBatching();
      try {
         drawSlotGrid(context, top, color);
      } finally {
         try { batch.draw(); }
         finally { if (!cull) RenderSystem.disableCull(); }
      }
   }

   private void drawSlotGrid(UIContext context, float top, ColorRGBA color) {
      float stride = listStride(SLOT_SIZE + GAP);
      for (int row = 0; row < ROWS; row++) {
         for (int column = 0; column < COLUMNS; column++) {
            context.drawRoundedRect(this.x + PADDING + column * (SLOT_SIZE + GAP),
               top + row * stride, SLOT_SIZE, SLOT_SIZE, SLOT_RADIUS, color);
         }
      }
   }

   private void drawStack(UIContext context, ItemStack stack, float slotX, float slotY, float alpha) {
      float inset = (SLOT_SIZE - ITEM_SIZE) * 0.5F;
      float[] shaderColor = RenderSystem.getShaderColor().clone();
      try {
         // Use the same item path as Armor HUD, including uniform XYZ scaling.
         // Liquid shaders and earlier HUD entries may leave a tinted shader color.
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         context.drawItem(stack, slotX + inset, slotY + inset, ITEM_SIZE / 16.0F);
         context.getMatrices().push();
         try {
            context.getMatrices().translate(slotX + inset, slotY + inset, 0.0F);
            context.getMatrices().scale(ITEM_SIZE / 16.0F, ITEM_SIZE / 16.0F, ITEM_SIZE / 16.0F);
            // Vanilla supplies durability and cooldown overlays; the count uses our HUD font below.
            if (overlays.isEnabled()) context.drawStackOverlay(mc.textRenderer, stack, 0, 0, "");
            context.draw();
         } finally {
            context.getMatrices().pop();
         }
      } finally {
         RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
      }
      if (itemCounts.isEnabled() && stack.getCount() > 1) {
         // Item render layers change depth state; counts belong above the models.
         RenderSystem.disableDepthTest();
         var font = HudValueRow.valueFont();
         String count = Integer.toString(stack.getCount());
         float countX = slotX + SLOT_SIZE - font.width(count) - 0.5F;
         float countY = slotY + SLOT_SIZE - font.height() - 0.5F;
         context.drawText(font, count, countX + 0.5F, countY + 0.5F, ColorRGBA.BLACK.withAlpha(180.0F * alpha));
         context.drawText(font, count, countX, countY, HudTheme.strongTextColor().mulAlpha(alpha));
      }
   }

   @Override
   public boolean show() {
      return mc.player != null;
   }
}
