package dev.bkclient.utility.render;

import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/** Custom hotbar layout, animations and status rendering for BKClient 1.21.4. */
public final class HotbarRendered implements IMinecraft {
    private static final int HOTBAR_HEIGHT = 29;
    private static final int STATUS_BAR_GAP = 10;

    public static int statusBarY(int screenHeight) {
        return screenHeight - HOTBAR_HEIGHT - STATUS_BAR_GAP - 9;
    }

    private static final Identifier internalField0354 = Identifier.ofVanilla("hud/air");
    private static final Identifier internalField0355 = Identifier.ofVanilla("hud/air_bursting");
    private static final Identifier internalField1133 = Identifier.ofVanilla("hud/air_empty");
    private final Random internalField0721 = Random.create();
    private int internalField0227, internalField0228, internalField1053, internalField1055;
    private long internalField0229 = -1L, internalField0230, internalField1059;
    private final Animation[] internalField0556 = new Animation[40];
    private final int[] internalField0618 = new int[40];
    // ScriptInternal106: independent selected/adjacent tracks, including the selected slot.
    private final Animation[] selected = new Animation[9];
    private final Animation[] adjacent = new Animation[9];
    private final ItemStack[] stacks = new ItemStack[9];
    private final float[] slotX = new float[9], slotY = new float[9], expansion = new float[9];
    private float y;
    private ColorRGBA backgroundColor = new ColorRGBA(30, 33, 39);
    private ColorRGBA textColor = ColorRGBA.WHITE;

    public HotbarRendered() {
        for (int i = 0; i < 9; i++) {
            selected[i] = new Animation(300L, Easing.BAKEK);
            adjacent[i] = new Animation(300L, Easing.BAKEK);
        }
        for (int i = 0; i < 40; i++) internalField0556[i] = new Animation(700L, 0, Easing.QUAD_OUT);
    }

    public void reset() {
        for (int i = 0; i < 9; i++) { selected[i].reset(); adjacent[i].reset(); }
        internalField0229 = -1;
    }

    public void render(DrawContext context, RenderTickCounter counter, ColorRGBA backgroundColor,
                       boolean hearts, boolean food, boolean armor) {
        this.backgroundColor = backgroundColor;
        float luminance = .2126F * backgroundColor.getRed() + .7152F * backgroundColor.getGreen() + .0722F * backgroundColor.getBlue();
        this.textColor = luminance > 150 ? ColorRGBA.BLACK : ColorRGBA.WHITE;
        if (mc.player == null || mc.world == null) return;
        context.draw();
        DrawUtility.blurProgram.drawHotbar();
        var c = CustomDrawContext.of(context);
        float x = (int)(context.getScaledWindowWidth() / 2F - 100);
        y = context.getScaledWindowHeight() - HOTBAR_HEIGHT;
        if (internalField0229 != mc.world.getTime()) { internalField0229 = mc.world.getTime(); internalField0227++; }
        if (mc.interactionManager.hasStatusBars()) renderStatusBars(c, hearts, food, armor);
        if (mc.player.getJumpingMount() == null && mc.interactionManager.hasExperienceBar() && mc.player.experienceLevel > 0) {
            String level = Integer.toString(mc.player.experienceLevel);
            var font = Fonts.REGULAR.getFont(8);
            float tx = (context.getScaledWindowWidth() - font.width(level)) / 2F;
            c.drawText(font, level, tx + 1, y - 8, ColorRGBA.BLACK.mulAlpha(.5F));
            c.drawText(font, level, tx - 1, y - 8, ColorRGBA.BLACK.mulAlpha(.5F));
            c.drawText(font, level, tx, y - 7, ColorRGBA.BLACK.mulAlpha(.5F));
            c.drawText(font, level, tx, y - 9, ColorRGBA.BLACK.mulAlpha(.5F));
            c.drawText(font, level, tx, y - 8, new ColorRGBA(126, 252, 32, 255));
        }
        ItemStack offhand = mc.player.getOffHandStack();
        boolean left = !offhand.isEmpty() && mc.player.getMainArm().getOpposite() == Arm.LEFT;
        drawBackground(c, x - (left ? 30 : 0), offhand.isEmpty() ? 200 : 230);
        int index = mc.player.getInventory().selectedSlot;
        for (int i = 0; i < 9; i++) {
            selected[i].update(index == i);
            adjacent[i].update(Math.abs(index - i) <= 1);
            expansion[i] = .1F * adjacent[i].getValue() + .25F * selected[i].getValue();
            slotX[i] = x + 6 + i * 21.5F;
            slotY[i] = y + 5.5F - 12 * expansion[i];
            stacks[i] = mc.player.getInventory().getStack(i);
            drawSlotModel(c, stacks[i], slotX[i], slotY[i], expansion[i]);
        }
        float ox = left ? x - 24 : x + 208;
        if (!offhand.isEmpty()) drawSlotModel(c, offhand, ox, y + 5.5F, 0);
        // Finish all 1.21.4 item consumers before drawing the GUI overlays.
        c.draw();
        for (int i = 0; i < 9; i++) {
            float select = selected[i].getValue();
            if (select > .001F) c.drawRoundedRect(slotX[i] + 7.5F, slotY[i] + 21 - select, 2, 2,
                BorderRadius.all(.5F), textColor.mulAlpha(select));
        }
        for (int i = 0; i < 9; i++)
            drawSlotOverlay(c, stacks[i], slotX[i], slotY[i], expansion[i], i, counter);
        if (!offhand.isEmpty()) {
            drawSlotOverlay(c, offhand, ox, y + 5.5F, 0, -1, counter);
            c.drawRoundedRect(ox + (left ? 22.5F : -7.5F), y + 5.5F, .5F, 16,
                BorderRadius.ZERO, textColor.mulAlpha(.5F));
        }
        context.draw();
    }

    private void drawBackground(CustomDrawContext c, float x, float width) {
        BorderRadius radius = BorderRadius.all(8);
        // GUI themes supply RGB only; keep the hotbar's glass and opacity stable.
        DrawUtility.drawLiquidGlass(c.getMatrices(), x, y, width, 27, BorderRadius.all(28),
            ColorRGBA.WHITE, 1, 26, ColorRGBA.WHITE, 1, true, 0, .3F, 7, false,
            dev.bkclient.framework.shader.impl.BlurProgram.getHotbarTexture());
        c.drawSquircle(x, y, width, 27, 7, radius, backgroundColor.withAlpha(102));
    }

    private void drawSlotModel(CustomDrawContext c, ItemStack stack, float x, float y, float expand) {
        if (stack.isEmpty()) return;
        // Preserve fractional item positions using the 1.21.4 MatrixStack API.
        c.getMatrices().push();
        c.getMatrices().translate(x + 8, y + 8, 0);
        c.getMatrices().scale(1 + expand, 1 + expand, 1);
        c.getMatrices().translate(-8, -8, 0);
        try {
            // Vanilla item rendering keeps the correct GUI lighting for block models.
            c.drawItem(stack, 0, 0);
        } finally { c.getMatrices().pop(); }
    }

    private void drawSlotOverlay(CustomDrawContext c, ItemStack stack, float x, float y, float expand, int index, RenderTickCounter counter) {
        RenderUtility.scale(c.getMatrices(), x + 8, y + 8, 1 + expand);
        try {
            if (stack.isEmpty()) {
                if (index >= 0) c.drawCenteredText(Fonts.ROUND_BOLD.getFont(10), Integer.toString(index + 1),
                    x + 8.5F, y + 4.5F, textColor.mulAlpha(.5F + expand * 1.5F));
                return;
            }
            if (stack.isItemBarVisible()) {
                float durability = stack.isDamageable() && stack.getMaxDamage() > 0
                    ? MathHelper.clamp((stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage(), 0, 1)
                    : MathHelper.clamp(stack.getItemBarStep() / 13.0F, 0, 1);
                dev.bkclient.ui.components.MusicSlider.drawRail(c, x + 2, y + 13.75F, 13, 0, durability,
                    dev.bkclient.ui.hud.impl.ArmorHud.durabilityColor(durability), 1);

            }
            if (stack.getCount() != 1) {
                var font = Fonts.ROUND_BOLD.getFont(8);
                String count = Integer.toString(stack.getCount());
                c.drawText(font, count, x + 17.5F - font.width(count), y + 9.5F, ColorRGBA.BLACK.mulAlpha(.4F));
                c.drawText(font, count, x + 17 - font.width(count), y + 9, ColorRGBA.WHITE);
            }
            float cooldown = mc.player.getItemCooldownManager().getCooldownProgress(stack, counter.getTickDelta(true));
            if (cooldown > 0) c.drawRoundedRect(x, y + MathHelper.floor(16 * (1 - cooldown)), 16,
                MathHelper.ceil(16 * cooldown), BorderRadius.all(.5F), ColorRGBA.WHITE.mulAlpha(.35F));
        } finally { RenderUtility.end(c.getMatrices()); }
    }
   private void renderStatusBars(CustomDrawContext localValue1, boolean hearts, boolean food, boolean armor) {
      PlayerEntity localValue2 = this.internalMethod03874();
      if (localValue2 != null) {
         int localValue3 = MathHelper.ceil(localValue2.getHealth());
         boolean localValue4 = this.internalField1059 > this.internalField0227 && (this.internalField1059 - this.internalField0227) / 3L % 2L == 1L;
         long localValue5 = Util.getMeasuringTimeMs();
         if (localValue3 < this.internalField0228 && localValue2.timeUntilRegen > 0) {
            this.internalField0230 = localValue5;
            this.internalField1059 = this.internalField0227 + 20;
         } else if (localValue3 > this.internalField0228 && localValue2.timeUntilRegen > 0) {
            this.internalField0230 = localValue5;
            this.internalField1059 = this.internalField0227 + 10;
         }

         if (localValue5 - this.internalField0230 > 1000L) {
            this.internalField1053 = localValue3;
            this.internalField0230 = localValue5;
         }

         if (localValue3 < this.internalField0228) {
            for (int localValue7 = 0; localValue7 < 40 && localValue7 * 2 < this.internalField0228; localValue7++) {
               int localValue8 = MathHelper.clamp(this.internalField0228 - localValue7 * 2, 0, 2);
               int localValue9 = MathHelper.clamp(localValue3 - localValue7 * 2, 0, 2);
               if (localValue8 > localValue9) {
                  this.internalField0618[localValue7] = localValue8 - localValue9 == 2 ? 3 : (localValue8 == 2 ? 2 : 1);
                  this.internalField0556[localValue7].setValue(1.0F);
                  this.internalField0556[localValue7].update(0.0F);
               }
            }
         } else if (localValue3 > this.internalField0228) {
            for (int localValue19 = 0; localValue19 < 40 && localValue19 * 2 < localValue3; localValue19++) {
               this.internalField0556[localValue19].setValue(0.0F);
               this.internalField0618[localValue19] = 0;
            }
         }

         this.internalField0228 = localValue3;
         int localValue20 = this.internalField1053;
         this.internalField0721.setSeed(this.internalField0227 * 312871);
         int localValue21 = localValue1.getScaledWindowWidth() / 2 - 91;
         int localValue22 = localValue1.getScaledWindowWidth() / 2 + 91;
         int localValue10 = statusBarY(localValue1.getScaledWindowHeight());
         float localValue11 = Math.max((float)localValue2.getAttributeValue(EntityAttributes.MAX_HEALTH), (float)Math.max(localValue20, localValue3));
         int localValue12 = MathHelper.ceil(localValue2.getAbsorptionAmount());
         int localValue13 = MathHelper.ceil((localValue11 + localValue12) / 2.0F / 10.0F);
         int localValue14 = Math.max(10 - (localValue13 - 2), 3);
         int localValue15 = localValue10 - 10;
         int localValue16 = -1;
         if (localValue2.hasStatusEffect(StatusEffects.REGENERATION)) {
            localValue16 = this.internalField0227 % MathHelper.ceil(localValue11 + 5.0F);
         }

         if (armor) internalMethod02071(localValue1, localValue2, localValue10, localValue13, localValue14, localValue21);
         if (hearts) this.internalMethod03714(localValue1, localValue2, localValue21, localValue10, localValue14, localValue16, localValue11, localValue3, localValue20, localValue12, localValue4);
         LivingEntity localValue17 = this.internalMethod04421();
         int localValue18 = this.internalMethod05438(localValue17);
         if (food && localValue18 == 0) {
            this.internalMethod00037(localValue1, localValue2, localValue10, localValue22);
            localValue15 -= 10;
         }

         // Vanilla retains air bubbles and mount status independently of the hotbar style.
      }
   }

   private static void internalMethod02071(CustomDrawContext localValue0, PlayerEntity localValue1, int localValue2, int localValue3, int localValue4, int localValue5) {
      ColorRGBA localValue6 = new ColorRGBA(223.0F, 223.0F, 223.0F);
      int localValue7 = localValue1.getArmor();
      if (localValue7 > 0) {
         int localValue8 = localValue2 - (localValue3 - 1) * localValue4 - 10;

         for (int localValue9 = 0; localValue9 < 10; localValue9++) {
            int localValue10 = localValue5 + localValue9 * 9;
            if (localValue9 * 2 + 1 < localValue7) {
               localValue0.drawRoundedRect(localValue10, localValue8, 8.0F, 8.0F, BorderRadius.all(1.5F), localValue6);
            }

            if (localValue9 * 2 + 1 > localValue7) {
               localValue0.drawRoundedRect(
                  localValue10,
                  localValue8,
                  8.0F,
                  8.0F,
                  BorderRadius.all(1.5F),
                  Colors.getBackgroundColor().withAlpha(170)
               );
            }
         }

         for (int localValue11 = 0; localValue11 < 10; localValue11++) {
            int localValue12 = localValue5 + localValue11 * 9;
            if (localValue11 * 2 + 1 == localValue7) {
               localValue0.drawRoundedRect(
                  localValue12 + 4,
                  localValue8,
                  4.0F,
                  8.0F,
                  new BorderRadius(0.0F, 1.5F, 1.5F, 0.0F),
                  Colors.getBackgroundColor().withAlpha(170)
               );
               localValue0.drawRoundedRect(localValue12, localValue8, 4.0F, 8.0F, new BorderRadius(1.5F, 0.0F, 0.0F, 1.5F), localValue6);
            }
         }
      }
   }

   private void internalMethod03714(CustomDrawContext localValue1, PlayerEntity localValue2, int localValue3, int localValue4, int localValue5, int localValue6, float localValue7, int localValue8, int localValue9, int localValue10, boolean localValue11) {
      HotbarRendered.InternalType0285 localValue12 = HotbarRendered.InternalType0285.internalMethod05872(localValue2);
      boolean localValue13 = localValue2.getWorld().getLevelProperties().isHardcore();
      int localValue14 = MathHelper.ceil(localValue7 / 2.0);
      int localValue15 = MathHelper.ceil(localValue10 / 2.0);
      int localValue16 = localValue14 * 2;

      for (int localValue17 = localValue14 + localValue15 - 1; localValue17 >= 0; localValue17--) {
         int localValue18 = localValue17 / 10;
         int localValue19 = localValue17 % 10;
         int localValue20 = localValue3 + localValue19 * 9;
         int localValue21 = localValue4 - localValue18 * localValue5;
         if (localValue8 + localValue10 <= 4) {
            localValue21 += this.internalField0721.nextInt(2);
         }

         if (localValue17 < localValue14 && localValue17 == localValue6) {
            localValue21 -= 2;
         }

         this.internalMethod07283(localValue1, HotbarRendered.InternalType0285.internalField0108, localValue20, localValue21, false);
         int localValue22 = localValue17 * 2;
         boolean localValue23 = localValue17 >= localValue14;
         if (localValue23) {
            int localValue24 = localValue22 - localValue16;
            if (localValue24 < localValue10) {
               boolean localValue25 = localValue24 + 1 == localValue10;
               this.internalMethod07283(
                  localValue1,
                  localValue12 == HotbarRendered.InternalType0285.internalField1000 ? localValue12 : HotbarRendered.InternalType0285.internalField0998,
                  localValue20,
                  localValue21,
                  localValue25
               );
            }
         }

         if (localValue22 < localValue8) {
            this.internalMethod07283(localValue1, localValue12, localValue20, localValue21, localValue22 + 1 == localValue8);
         }

         if (localValue17 < 40) {
            float localValue27 = this.internalField0556[localValue17].update(0.0F);
            if (localValue27 > 0.001F) {
               float localValue28 = 1.0F + (1.0F - localValue27);
               RenderUtility.scale(localValue1.getMatrices(), localValue20 + 4.0F, localValue21 + 4.0F, localValue28);
               ColorRGBA localValue26 = ColorRGBA.WHITE.mulAlpha(localValue27);
               if (this.internalField0618[localValue17] == 1) {
                  localValue1.drawRoundedRect(localValue20, localValue21, 4.0F, 8.0F, new BorderRadius(2.0F, 0.0F, 0.0F, 2.0F), localValue26);
               } else if (this.internalField0618[localValue17] == 2) {
                  localValue1.drawRoundedRect(localValue20 + 4, localValue21, 4.0F, 8.0F, new BorderRadius(0.0F, 2.0F, 2.0F, 0.0F), localValue26);
               } else {
                  localValue1.drawRoundedRect(localValue20, localValue21, 8.0F, 8.0F, BorderRadius.all(2.0F), localValue26);
               }

               RenderUtility.end(localValue1.getMatrices());
            }
         }
      }
   }

   private void internalMethod07283(CustomDrawContext localValue1, HotbarRendered.InternalType0285 localValue2, int localValue3, int localValue4, boolean localValue5) {
      localValue2.internalMethod06973(localValue1, localValue3, localValue4, 8.0F, 8.0F, localValue5);
   }

   private void internalMethod05073(CustomDrawContext localValue1, PlayerEntity localValue2, int localValue3, int localValue4, int localValue5) {
      int localValue6 = localValue2.getMaxAir();
      int localValue7 = Math.clamp((long)localValue2.getAir(), 0, localValue6);
      boolean localValue8 = localValue2.isSubmergedIn(FluidTags.WATER);
      if (localValue8 || localValue7 < localValue6) {
         localValue4 = this.internalMethod03982(localValue3, localValue4);
         int localValue9 = internalMethod07239(localValue7, localValue6, -2);
         int localValue10 = internalMethod07239(localValue7, localValue6, 0);
         int localValue11 = 10 - internalMethod07239(localValue7, localValue6, internalMethod03983(localValue7, localValue8));
         boolean localValue12 = localValue9 != localValue10;
         if (!localValue8) {
            this.internalField1055 = 0;
         }

         for (int localValue13 = 1; localValue13 <= 10; localValue13++) {
            int localValue14 = localValue5 - (localValue13 - 1) * 9 - 10;
            if (localValue13 <= localValue9) {
               localValue1.drawGuiTexture(RenderLayer::getGuiTextured, internalField0354, localValue14, localValue4, 9, 9);
            } else if (localValue12 && localValue13 == localValue10 && localValue8) {
               localValue1.drawGuiTexture(RenderLayer::getGuiTextured, internalField0355, localValue14, localValue4, 9, 9);
               this.internalMethod07071(localValue13, localValue2, localValue11);
            } else if (localValue13 > 10 - localValue11) {
               int localValue15 = localValue11 == 10 && this.internalField0227 % 2 == 0 ? this.internalField0721.nextInt(2) : 0;
               localValue1.drawGuiTexture(RenderLayer::getGuiTextured, internalField1133, localValue14, localValue4 + localValue15, 9, 9);
            }
         }
      }
   }

   private int internalMethod03982(int localValue1, int localValue2) {
      return localValue2 - (this.internalMethod00616(localValue1) - 1) * 10;
   }

   private static int internalMethod07239(int localValue0, int localValue1, int localValue2) {
      return MathHelper.ceil((float)((localValue0 + localValue2) * 10) / localValue1);
   }

   private static int internalMethod03983(int localValue0, boolean localValue1) {
      return localValue0 != 0 && localValue1 ? 1 : 0;
   }

   private void internalMethod07071(int localValue1, PlayerEntity localValue2, int localValue3) {
      if (this.internalField1055 != localValue1) {
         float localValue4 = 0.5F + 0.1F * Math.max(0, localValue3 - 3 + 1);
         float localValue5 = 1.0F + 0.1F * Math.max(0, localValue3 - 5 + 1);
         localValue2.playSound(SoundEvents.UI_HUD_BUBBLE_POP, localValue4, localValue5);
         this.internalField1055 = localValue1;
      }
   }

   private void internalMethod00037(CustomDrawContext localValue1, PlayerEntity localValue2, int localValue3, int localValue4) {
      ColorRGBA localValue5 = new ColorRGBA(184.0F, 132.0F, 88.0F);
      HungerManager localValue6 = localValue2.getHungerManager();
      int localValue7 = localValue6.getFoodLevel();
      int[] localValue8 = new int[10];

      for (int localValue9 = 0; localValue9 < 10; localValue9++) {
         int localValue10 = localValue3;
         if (localValue6.getSaturationLevel() <= 0.0F && this.internalField0227 % (localValue7 * 3 + 1) == 0) {
            localValue10 = localValue3 + (this.internalField0721.nextInt(3) - 1);
         }

         localValue8[localValue9] = localValue10;
      }

      for (int localValue14 = 0; localValue14 < 10; localValue14++) {
         int localValue18 = localValue4 - localValue14 * 9 - 10;
         localValue1.drawRoundedRect(
            localValue18,
            localValue8[localValue14],
            8.0F,
            8.0F,
            BorderRadius.all(1.5F),
            Colors.getBackgroundColor().withAlpha(170)
         );
      }

      for (int localValue15 = 0; localValue15 < 10; localValue15++) {
         if (localValue15 * 2 + 1 < localValue7) {
            int localValue19 = localValue4 - localValue15 * 9 - 10;
            localValue1.drawRoundedRect(localValue19, localValue8[localValue15], 8.0F, 8.0F, BorderRadius.all(1.5F), localValue5);
         }
      }

      for (int localValue16 = 0; localValue16 < 10; localValue16++) {
         if (localValue16 * 2 + 1 == localValue7) {
            int localValue20 = localValue4 - localValue16 * 9 - 10;
            localValue1.drawRoundedRect(localValue20 + 4, localValue8[localValue16], 4.0F, 8.0F, new BorderRadius(0.0F, 1.5F, 1.5F, 0.0F), localValue5);
         }
      }

      localValue5 = new ColorRGBA(251.0F, 170.0F, 56.0F);
      int localValue17 = (int)localValue6.getSaturationLevel();
      int[] localValue21 = new int[10];

      for (int localValue11 = 0; localValue11 < 10; localValue11++) {
         int localValue12 = localValue3;
         if (localValue6.getSaturationLevel() <= 0.0F && this.internalField0227 % (localValue17 * 3 + 1) == 0) {
            localValue12 = localValue3 + (this.internalField0721.nextInt(3) - 1);
         }

         localValue21[localValue11] = localValue12;
      }

      for (int localValue22 = 0; localValue22 < 10; localValue22++) {
         int localValue24 = localValue4 - localValue22 * 9 - 10;
         if (localValue22 * 2 + 1 < localValue17) {
            localValue1.drawRoundedRect(localValue24, localValue21[localValue22], 8.0F, 8.0F, BorderRadius.all(1.5F), localValue5);
         }
      }

      for (int localValue23 = 0; localValue23 < 10; localValue23++) {
         if (localValue23 * 2 + 1 == localValue17) {
            int localValue25 = localValue4 - localValue23 * 9 - 10;
            localValue1.drawRoundedRect(localValue25 + 4, localValue21[localValue23], 4.0F, 8.0F, new BorderRadius(0.0F, 1.5F, 1.5F, 0.0F), localValue5);
         }
      }
   }

   @Nullable
   private PlayerEntity internalMethod03874() {
      return mc.getCameraEntity() instanceof PlayerEntity localValue2 ? localValue2 : null;
   }

   @Nullable
   private LivingEntity internalMethod04421() {
      PlayerEntity localValue1 = this.internalMethod03874();
      if (localValue1 == null) {
         return null;
      } else {
         return localValue1.getVehicle() instanceof LivingEntity localValue3 ? localValue3 : null;
      }
   }

   private int internalMethod05438(@Nullable LivingEntity localValue1) {
      if (localValue1 != null && localValue1.isLiving()) {
         int localValue2 = (int)(localValue1.getMaxHealth() + 0.5F) / 2;
         return Math.min(localValue2, 30);
      } else {
         return 0;
      }
   }

   private int internalMethod00616(int localValue1) {
      return (int)Math.ceil(localValue1 / 10.0);
   }

   static enum InternalType0285 {
      internalField0108(new ColorRGBA(200.0F, 200.0F, 200.0F, 200.0F)),
      internalField0109(new ColorRGBA(255.0F, 81.0F, 81.0F)),
      internalField0999(new ColorRGBA(169.0F, 202.0F, 23.0F)),
      internalField1000(new ColorRGBA(66.0F, 66.0F, 66.0F)),
      internalField0998(new ColorRGBA(251.0F, 170.0F, 56.0F)),
      internalField0997(new ColorRGBA(163.0F, 246.0F, 255.0F));

      private final ColorRGBA internalField0777;

      public void internalMethod06973(CustomDrawContext localValue1, float localValue2, float localValue3, float localValue4, float localValue5, boolean localValue6) {
         if (localValue6) {
            localValue1.drawRoundedRect(
               localValue2,
               localValue3,
               localValue4 / 2.0F,
               localValue5,
               new BorderRadius(2.0F, 0.0F, 0.0F, 2.0F),
               this == internalField0108
                  ? Colors.getBackgroundColor().withAlpha(170)
                  : this.internalField0777
            );
         } else {
            localValue1.drawRoundedRect(
               localValue2,
               localValue3,
               localValue4,
               localValue5,
               BorderRadius.all(2.0F),
               this == internalField0108
                  ? Colors.getBackgroundColor().withAlpha(170)
                  : this.internalField0777
            );
         }
      }

      static HotbarRendered.InternalType0285 internalMethod05872(PlayerEntity localValue0) {
         if (localValue0.hasStatusEffect(StatusEffects.POISON)) {
            return internalField0999;
         } else if (localValue0.hasStatusEffect(StatusEffects.WITHER)) {
            return internalField1000;
         } else {
            return localValue0.isFrozen() ? internalField0997 : internalField0109;
         }
      }

      
      private InternalType0285(ColorRGBA localValue3) {
         this.internalField0777 = localValue3;
      }
   }

}
