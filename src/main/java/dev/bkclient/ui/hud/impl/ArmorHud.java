package dev.bkclient.ui.hud.impl;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.ui.hud.HudElement;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.RingRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

/** BK-Client ArmorHUD layout rendered with the Liquid theme. */
public class ArmorHud extends HudElement {
   private static final float PANEL_WIDTH = 40.0F;
   private static final float PANEL_HEIGHT = 20.0F;
   private static final float PANEL_GAP = 4.0F;

   private static final float RING_RADIUS = 3.5F;
   private static final float RING_THICKNESS = 0.9F;
   private static final float RING_CENTER_X = 11.0F;

   private static final float DIVIDER_WIDTH = 1.0F;
   private static final float DIVIDER_HEIGHT = 5.0F;
   private static final float DIVIDER_X = 19.0F;

   private static final float ITEM_SIZE = 11.0F;
   private static final float ITEM_X = 23.5F;

   private static final ColorRGBA RING_HIGH = new ColorRGBA(160.0F, 220.0F, 45.0F, 255.0F);
   private static final ColorRGBA RING_MID = new ColorRGBA(255.0F, 150.0F, 40.0F, 255.0F);
   private static final ColorRGBA RING_LOW = new ColorRGBA(240.0F, 55.0F, 55.0F, 255.0F);

   private static final EquipmentSlot[] SLOTS = {
      EquipmentSlot.HEAD,
      EquipmentSlot.CHEST,
      EquipmentSlot.LEGS,
      EquipmentSlot.FEET
   };
   private static final List<ItemStack> PREVIEW_STACKS = List.of(
      Items.NETHERITE_HELMET.getDefaultStack(),
      Items.NETHERITE_CHESTPLATE.getDefaultStack(),
      Items.NETHERITE_LEGGINGS.getDefaultStack(),
      Items.NETHERITE_BOOTS.getDefaultStack()
   );
   private static final float[] PREVIEW_DURABILITY = {0.84F, 0.61F, 0.38F, 0.18F};

   private final List<ItemStack> armorStacks = new ArrayList<>(4);
   private boolean preview;
   private final ModeSetting durabilityDisplay = new ModeSetting(this, "hud.armor.durability_display");
   private final ModeSetting.Value rings = new ModeSetting.Value(durabilityDisplay, "hud.editor.rings").select();
   private final ModeSetting.Value percent = new ModeSetting.Value(durabilityDisplay, "hud.armor.percent");
   private final ModeSetting.Value ringsAndPercent = new ModeSetting.Value(durabilityDisplay, "hud.armor.rings_percent");
   private final dev.bkclient.systems.setting.settings.BooleanSetting horizontal = new dev.bkclient.systems.setting.settings.BooleanSetting(this,"hud.armor.horizontal");
   private final dev.bkclient.systems.setting.settings.BooleanSetting accentRings = new dev.bkclient.systems.setting.settings.BooleanSetting(this,"hud.armor.accent_rings");
   private final dev.bkclient.systems.setting.settings.BooleanSetting testPreview = new dev.bkclient.systems.setting.settings.BooleanSetting(this,"hud.cooldowns2.preview");
   private final dev.bkclient.systems.setting.settings.SliderSetting spacing = new dev.bkclient.systems.setting.settings.SliderSetting(this,"hud.editor.spacing").min(0).max(10).step(1).currentValue(4);

   public ArmorHud() {
      super("hud.armor", "icons/hud/armor.png");
   }

   private float panelWidth() { return ringsAndPercent.isSelected() ? 60 : PANEL_WIDTH; }

   private void refreshArmor() {
      this.armorStacks.clear();
      if (mc.player != null) {
         for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
               this.armorStacks.add(stack);
            }
         }
      }

      this.preview = testPreview.isEnabled() || this.armorStacks.isEmpty() && mc.currentScreen instanceof ChatScreen;
      if (this.preview) {
         this.armorStacks.clear();
         this.armorStacks.addAll(PREVIEW_STACKS);
      }
   }

   @Override
   public void update(UIContext context) {
      this.refreshArmor();
      int count = this.armorStacks.size();
      this.width = panelWidth();
      this.height = horizontal.isEnabled() || count == 0 ? PANEL_HEIGHT : count * PANEL_HEIGHT + (count - 1) * spacing.getCurrentValue();
      if (horizontal.isEnabled()) this.width = Math.max(1,count)*panelWidth() + Math.max(0,count-1)*spacing.getCurrentValue();
      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      float panelY = this.y;
      for (int index = 0; index < this.armorStacks.size(); index++) {
         ItemStack stack = this.armorStacks.get(index);
         float durability = this.preview ? PREVIEW_DURABILITY[index % PREVIEW_DURABILITY.length] : durabilityFraction(stack);
         this.renderPanel(context, this.x + (horizontal.isEnabled()?index*(panelWidth()+spacing.getCurrentValue()):0), panelY, stack, durability);
         if (!horizontal.isEnabled()) panelY += PANEL_HEIGHT + spacing.getCurrentValue();
      }
   }

   private void renderPanel(UIContext context, float panelX, float panelY, ItemStack stack, float durability) {
      float elementAlpha = Math.min(1.0F, this.animation.getValue() * this.visible.getValue());
      HudTheme.drawPanel(
         context,
         panelX,
         panelY,
         panelWidth(),
         PANEL_HEIGHT,
         elementAlpha,
         this.dragAnim.getValue(),
         3.0F
      );

      float ringCenterX = panelX + RING_CENTER_X;
      float ringCenterY = panelY + PANEL_HEIGHT / 2.0F;
      ColorRGBA ringColor = this.ringColor(durability).withAlpha(255.0F * elementAlpha);
      if (!percent.isSelected()) RingRenderer.progress(
         context.getMatrices(),
         ringCenterX,
         ringCenterY,
         RING_RADIUS,
         RING_THICKNESS,
         durability,
         0.0F,
         HudTheme.separatorColor().withAlpha(72.0F * elementAlpha),
         ringColor
      );

      float extraWidth = panelWidth() - PANEL_WIDTH;
      if (!rings.isSelected()) {
         Font font = Fonts.MEDIUM.getFont(6);
         String label = Math.round(durability * 100) + "%";
         float centerX = panelX + (ringsAndPercent.isSelected() ? 28 : RING_CENTER_X);
         context.drawText(font, label, centerX - font.width(label) / 2,
            ringCenterY - font.height() / 2, ringColor);
      }

      context.drawRoundedRect(
         panelX + DIVIDER_X + extraWidth,
         ringCenterY - DIVIDER_HEIGHT / 2.0F,
         DIVIDER_WIDTH,
         DIVIDER_HEIGHT,
         BorderRadius.all(0.5F),
         HudTheme.separatorColor().withAlpha(96.0F * elementAlpha)
      );

      float itemY = panelY + (PANEL_HEIGHT - ITEM_SIZE) / 2.0F;
      context.drawItem(stack, panelX + ITEM_X + extraWidth, itemY, ITEM_SIZE / 16.0F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static float durabilityFraction(ItemStack stack) {
      if (!stack.isDamageable() || stack.getMaxDamage() <= 0) {
         return 1.0F;
      }
      float remaining = stack.getMaxDamage() - stack.getDamage();
      return MathHelper.clamp(remaining / stack.getMaxDamage(), 0.0F, 1.0F);
   }

   private ColorRGBA ringColor(float durability) {
      if (accentRings.isEnabled()) return dev.bkclient.utility.colors.Colors.getAccent();
      return durabilityColor(durability);
   }

   public static ColorRGBA durabilityColor(float durability) {
      if (durability > 0.5F) {
         return RING_HIGH;
      }
      if (durability > 0.25F) {
         return RING_MID;
      }
      return RING_LOW;
   }

   @Override
   public boolean show() {
      return !this.armorStacks.isEmpty();
   }
}
