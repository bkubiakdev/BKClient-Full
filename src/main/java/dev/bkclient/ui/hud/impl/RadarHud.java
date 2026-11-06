package dev.bkclient.ui.hud.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.ui.hud.HudElement;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.render.ScissorUtility;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class RadarHud extends HudElement {
   private static final float HEADER_HEIGHT = 18.0F;
   private static final float BODY_PADDING = 5.0F;
   private static final float HEAD_SIZE = 9.0F;
   private static final int MAX_TRACKED_PLAYERS = 24;
   private static final ColorRGBA ENEMY_COLOR = new ColorRGBA(255.0F, 82.0F, 82.0F);
   private static final ColorRGBA FRIEND_COLOR = new ColorRGBA(72.0F, 205.0F, 126.0F);

   private final SliderSetting radarSize = new SliderSetting(this, "hud.radar.size")
      .min(84.0F).max(140.0F).step(2.0F).currentValue(104.0F);
   private final SliderSetting range = new SliderSetting(this, "hud.radar.range")
      .min(16.0F).max(128.0F).step(8.0F).currentValue(64.0F).suffix("m");
   private final BooleanSetting names = new BooleanSetting(this, "hud.radar.names").enable();
   private final BooleanSetting distance = new BooleanSetting(this, "hud.radar.distance").enable();

   public RadarHud() {
      super("hud.radar", "icons/hud/world.png");
   }

   @Override
   public void update(UIContext context) {
      this.width = this.radarSize.getCurrentValue();
      this.height = this.width + HEADER_HEIGHT;
      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      float alpha = this.animation.getValue();
      HudTheme.drawPanel(context, this.x, this.y, this.width, this.height, alpha, this.dragAnim.getValue(), 8.0F);

      float bodyX = this.x + BODY_PADDING;
      float bodyY = this.y + HEADER_HEIGHT;
      float bodySize = this.width - BODY_PADDING * 2.0F;
      float centerX = bodyX + bodySize / 2.0F;
      float centerY = bodyY + bodySize / 2.0F;
      float radius = bodySize / 2.0F - HEAD_SIZE / 2.0F - 2.0F;
      BorderRadius bodyRadius = BorderRadius.all(7.0F);

      context.drawRoundedRect(
         bodyX, bodyY, bodySize, bodySize, bodyRadius,
         HudTheme.surfaceColor().withAlpha(72.0F * alpha)
      );
      this.drawGrid(context, bodyX, bodyY, bodySize, centerX, centerY, radius, alpha);

      ScissorUtility.push(context.getMatrices(), bodyX, bodyY, bodySize, bodySize);
      int tracked = this.drawPlayers(context, centerX, centerY, radius, alpha);
      ScissorUtility.pop();

      Font headerFont = Fonts.MEDIUM.getFont(7.0F);
      context.drawText(
         headerFont, "Radar", this.x + 8.0F,
         this.y + GuiUtility.getMiddleOfBox(headerFont.height(), HEADER_HEIGHT) + 0.5F,
         HudTheme.textColor().withAlpha(230.0F * alpha)
      );
      String summary = tracked + "  /  " + (int)this.range.getCurrentValue() + "m";
      context.drawRightText(
         Fonts.MEDIUM.getFont(5.5F), summary, this.x + this.width - 8.0F,
         this.y + GuiUtility.getMiddleOfBox(Fonts.MEDIUM.getFont(5.5F).height(), HEADER_HEIGHT) + 0.5F,
         HudTheme.textColor().withAlpha(145.0F * alpha)
      );
   }

   private void drawGrid(
      UIContext context, float bodyX, float bodyY, float bodySize,
      float centerX, float centerY, float radius, float alpha
   ) {
      ColorRGBA gridColor = HudTheme.textColor().withAlpha(25.0F * alpha);
      context.drawLine(new Vec2f(bodyX + 4.0F, centerY), new Vec2f(bodyX + bodySize - 4.0F, centerY), gridColor);
      context.drawLine(new Vec2f(centerX, bodyY + 4.0F), new Vec2f(centerX, bodyY + bodySize - 4.0F), gridColor);

      float ringOne = radius * 0.5F;
      context.drawRoundedBorder(
         centerX - ringOne, centerY - ringOne, ringOne * 2.0F, ringOne * 2.0F,
         0.5F, BorderRadius.all(ringOne), gridColor
      );
      context.drawRoundedBorder(
         centerX - radius, centerY - radius, radius * 2.0F, radius * 2.0F,
         0.5F, BorderRadius.all(radius), HudTheme.textColor().withAlpha(35.0F * alpha)
      );

      ColorRGBA forwardColor = Colors.getAccent().withAlpha(190.0F * alpha);
      context.drawLine(new Vec2f(centerX, centerY - 7.0F), new Vec2f(centerX, centerY - 15.0F), forwardColor);
      context.drawLine(new Vec2f(centerX, centerY - 15.0F), new Vec2f(centerX - 2.5F, centerY - 11.5F), forwardColor);
      context.drawLine(new Vec2f(centerX, centerY - 15.0F), new Vec2f(centerX + 2.5F, centerY - 11.5F), forwardColor);
   }

   private int drawPlayers(UIContext context, float centerX, float centerY, float radius, float alpha) {
      float tickDelta = context.getDelta();
      Vec3d ownPos = mc.player.getLerpedPos(tickDelta);
      float yaw = MathHelper.lerpAngleDegrees(tickDelta, mc.player.prevYaw, mc.player.getYaw());
      double yawRadians = Math.toRadians(yaw);
      double sin = Math.sin(yawRadians);
      double cos = Math.cos(yawRadians);
      float maxRange = this.range.getCurrentValue();
      float pixelsPerBlock = radius / maxRange;
      int tracked = 0;

      for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
         if (player == mc.player || !player.isAlive() || tracked >= MAX_TRACKED_PLAYERS) {
            continue;
         }

         Vec3d playerPos = player.getLerpedPos(tickDelta);
         double dx = playerPos.x - ownPos.x;
         double dz = playerPos.z - ownPos.z;
         double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
         if (horizontalDistance > maxRange) {
            continue;
         }

         double localX = dx * cos + dz * sin;
         double localForward = dz * cos - dx * sin;
         float markerX = centerX - (float)localX * pixelsPerBlock;
         float markerY = centerY - (float)localForward * pixelsPerBlock;
         float markerLeft = markerX - HEAD_SIZE / 2.0F;
         float markerTop = markerY - HEAD_SIZE / 2.0F;
         boolean friend = BKClientInit.getInstance().getFriendManager().isFriend(player.getName().getString());
         ColorRGBA markerColor = (friend ? FRIEND_COLOR : ENEMY_COLOR).withAlpha(230.0F * alpha);

         context.drawShadow(
            markerLeft, markerTop, HEAD_SIZE, HEAD_SIZE, 2.0F,
            BorderRadius.all(2.5F), markerColor.withAlpha(80.0F * alpha)
         );
         context.drawHead(player, markerLeft, markerTop, HEAD_SIZE, BorderRadius.all(2.5F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
         context.drawRoundedBorder(markerLeft, markerTop, HEAD_SIZE, HEAD_SIZE, 0.75F, BorderRadius.all(2.5F), markerColor);

         float health = MathHelper.clamp(player.getHealth() / player.getMaxHealth(), 0.0F, 1.0F);
         context.drawRoundedRect(markerLeft, markerTop + HEAD_SIZE + 1.0F, HEAD_SIZE, 1.25F, BorderRadius.all(0.6F), HudTheme.separatorColor());
         context.drawRoundedRect(markerLeft, markerTop + HEAD_SIZE + 1.0F, HEAD_SIZE * health, 1.25F, BorderRadius.all(0.6F), markerColor);

         if (this.names.isEnabled()) {
            String label = player.getName().getString();
            if (this.distance.isEnabled()) {
               label += " " + Math.round(horizontalDistance) + "m";
            }
            double heightDelta = playerPos.y - ownPos.y;
            if (heightDelta > 3.0) {
               label += " ↑";
            } else if (heightDelta < -3.0) {
               label += " ↓";
            }
            Font labelFont = Fonts.MEDIUM.getFont(4.75F);
            context.drawCenteredText(
               labelFont, label, markerX,
               markerTop + HEAD_SIZE + 3.0F,
               HudTheme.textColor().withAlpha(210.0F * alpha)
            );
         }
         ++tracked;
      }

      float ownLeft = centerX - HEAD_SIZE / 2.0F;
      float ownTop = centerY - HEAD_SIZE / 2.0F;
      context.drawHead(mc.player, ownLeft, ownTop, HEAD_SIZE, BorderRadius.all(2.5F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
      context.drawRoundedBorder(
         ownLeft, ownTop, HEAD_SIZE, HEAD_SIZE, 0.75F,
         BorderRadius.all(2.5F), Colors.getAccent().withAlpha(245.0F * alpha)
      );
      return tracked;
   }

   @Override
   public boolean show() {
      return mc.player != null && mc.world != null;
   }
}
