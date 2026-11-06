package dev.bkclient.ui.hud.impl.island;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.ui.hud.HudElement;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.ui.hud.impl.island.impl.BlinkStatus;
import dev.bkclient.ui.hud.impl.island.impl.ConfigStatus;
import dev.bkclient.ui.hud.impl.island.impl.DefaultStatus;
import dev.bkclient.ui.hud.impl.island.impl.MusicStatus;
import dev.bkclient.ui.hud.impl.island.impl.NotificationStatus;
import dev.bkclient.ui.hud.impl.island.impl.TinyTaskStatus;
import dev.bkclient.ui.hud.impl.island.impl.XrayStatus;
import dev.bkclient.ui.menu.dropdown.DropDownScreen;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.animation.types.ColorAnimation;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.time.Timer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import net.minecraft.client.gui.screen.ChatScreen;

/**
 * BK-Client Dynamic Island behaviour rendered with the BKClient Liquid theme.
 * The class name stays DynamicDock to preserve configs and existing integrations.
 */
public class DynamicDock extends HudElement implements IMinecraft, IScaledResolution {
   private static final float DOCK_Y = 7.0F;
   private static final float COMPACT_SCALE = 0.75F;
   private static final int[] PING_THRESHOLDS = {450, 300, 150, 75};

   private final SelectSetting statuses = new SelectSetting(this, "hud.dynamic_dock.statuses").draggable();
   private final BooleanSetting clockVisible = new BooleanSetting(this, "hud.dock.clock").enable();
   private final BooleanSetting connectionVisible = new BooleanSetting(this, "hud.dock.connection").enable();
   private final BooleanSetting swapSides = new BooleanSetting(this, "hud.dock.swap_sides");
   private final BooleanSetting musicLyrics = new BooleanSetting(this, "hud.dynamic_dock.music_lyrics").enabled(true);
   private final BooleanSetting liquidBubble = new BooleanSetting(this, "hud.dynamic_dock.liquid_bubble").enabled(true);
   private final Animation bubbleAnimation = new Animation(650L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private float bubbleOffset;
   private final IslandSize size = new IslandSize(48.0F, 15.0F);
   private boolean extended;
   private final Animation extendingAnim = new Animation(200L, 0.0F, Easing.LINEAR);
   private final Animation widthAnim = new Animation(500L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation heightAnim = new Animation(500L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation showPing = new Animation(500L, 0.0F, Easing.BAKEK);
   private final ColorAnimation backgroundColor = new ColorAnimation(300L, Colors.getBackgroundColor(), Easing.FIGMA_EASE_IN_OUT);
   private final ColorAnimation adaptColor = new ColorAnimation(300L, ColorRGBA.WHITE, Easing.LINEAR);
   private final Timer timer = new Timer();
   private boolean dark;
   private boolean useDark;
   private IslandStatus last;
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
   };

   public DynamicDock() {
      super("hud.dynamic_dock", "icons/hud/dock.png");
      for (var setting : this.getSettings()) {
         if (setting instanceof dev.bkclient.systems.setting.settings.SliderSetting slider
               && "hud.scale".equals(slider.getName())) {
            slider.min(1.0F).max(2.0F).currentValue(1.0F);
         }
      }
      // Temporarily disabled: FriendMessageStatus (restore with friend popups).
      // new dev.bkclient.ui.hud.impl.island.impl.FriendMessageStatus(this.statuses);
      // Temporarily disabled: FriendRequestStatus (restore with friend popups).
      // new dev.bkclient.ui.hud.impl.island.impl.FriendRequestStatus(this.statuses);
      new TinyTaskStatus(this.statuses);
      new ConfigStatus(this.statuses);
      new NotificationStatus(this.statuses);
      new dev.bkclient.ui.hud.impl.island.impl.DisplacementStatus(this.statuses).alwaysEnabled();
      new BlinkStatus(this.statuses);
      new dev.bkclient.ui.hud.impl.island.impl.PVPStatus(this.statuses);
      new XrayStatus(this.statuses);
      new MusicStatus(this.statuses);
      new DefaultStatus(this.statuses).alwaysEnabled();
      BKClientInit.getInstance().getFileManager().readFile("client");
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }

   @Override
   public void update(UIContext context) {
      super.update(context);
      this.currentStretchX = 1.0F;
      this.currentStretchY = 1.0F;
   }

   @Override
   public float getScale() {
      // Display 1..2 in settings while using the former 1.5..2 scale range.
      // Rendering and pointer coordinates both use this same mapping.
      return 1.0F + 0.5F * Math.clamp(super.getScale(), 1.0F, 2.0F);
   }

   @Override
   public void render(UIContext context) {
      if (!this.showing && this.animation.getValue() <= 0.001F) {
         return;
      }

      this.update(context);
      float anim = this.animation.getValue() * this.visible.getValue();
      if (anim <= 0.0F) {
         return;
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, anim));
      float scale = (0.5F + anim * 0.5F - 0.05F * this.selecting.getValue())
         * this.pressAnim.getValue()
         * this.getScale()
         * COMPACT_SCALE;
      float bounceValue = this.bounceAnim.getValue();
      float bounce = 1.0F + 0.08F * (float)Math.sin(bounceValue * Math.PI * 2.5F) * (1.0F - bounceValue);
      float centerX = sr.getScaledWidth() / 2.0F;
      float centerY = DOCK_Y + this.height / 2.0F;
      context.getMatrices().push();
      context.getMatrices().translate(centerX, centerY, 0.0F);
      context.getMatrices().scale(scale * bounce, scale * bounce, 1.0F);
      context.getMatrices().translate(-centerX, -centerY, 0.0F);
      this.renderComponent(context);
      context.getMatrices().pop();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   @Override
   protected void renderComponent(UIContext context) {
      IslandStatus status = this.active();
      if (status == null) {
         return;
      }
      if (status != this.last) {
         this.extended = false;
      }

      this.width = this.size.width;
      this.height = this.size.height;
      this.x = sr.getScaledWidth() / 2.0F - this.width / 2.0F;
      this.y = DOCK_Y;

      updateAdaptiveColor();
      boolean displacement = status instanceof dev.bkclient.ui.hud.impl.island.impl.DisplacementStatus;
      this.bubbleAnimation.update(this.liquidBubble.isEnabled() && !(status instanceof DefaultStatus)
         && !displacement && !this.extended);
      float bubbleProgress = displacement ? 0.0F : Math.clamp(this.bubbleAnimation.getValue(), 0.0F, 1.0F);
      float bubbleX = this.width - 7.5F + 19.0F * bubbleProgress;
      this.bubbleOffset = 19.0F * bubbleProgress;

      this.backgroundColor.update(status.getColor());
      float alpha = Math.min(1.0F, this.animation.getValue() * this.visible.getValue());
      boolean accentGlow = !(status instanceof NotificationStatus alerts && alerts.hasNotification());
      if (bubbleProgress > 0.001F) {
         HudTheme.drawIslandPanel(context, this.x, this.y, this.width, this.height, bubbleX,
            alpha, this.dragAnim.getValue(), accentGlow);
      } else {
         HudTheme.drawPanel(context, this.x, this.y, this.width, this.height, alpha,
            this.dragAnim.getValue(), 3.0F, accentGlow);
      }
      drawClockAndConnection(context);
      // Reveal the logo only once the bubble clears the capsule's text area.
      float logoAlpha = Math.clamp((bubbleProgress - 0.7F) / 0.3F, 0.0F, 1.0F);
      if (logoAlpha > 0.0F) {
         context.drawIcon(Fonts.ICONS, 'D', this.x + bubbleX - 6.0F,
            this.y + this.height / 2.0F - 5.7F, 11.0F, HudTheme.strongTextColor().withAlpha(255.0F * logoAlpha * alpha));
      }

      for (SelectSetting.Value value : this.statuses.getValues()) {
         IslandStatus islandStatus = (IslandStatus)value;
         islandStatus.getAnimation().update(status == islandStatus ? 1.0F : 0.0F);
      }

      context.getMatrices().push();
      context.getMatrices().translate(0.0F, DOCK_Y, 0.0F);
      ScissorUtility.push(context.getMatrices(), this.x, 0.0F, this.width, this.height);
      if (status.getAnimation().getValue() == 1.0F) {
         status.draw(context);
      } else {
         for (SelectSetting.Value value : this.statuses.getValues()) {
            IslandStatus islandStatus = (IslandStatus)value;
            if (islandStatus.getAnimation().getValue() > 0.0F) {
               islandStatus.drawWithAlpha(context);
            }
         }
      }
      ScissorUtility.pop();
      context.getMatrices().pop();

      this.widthAnim.update(status.size.width);
      this.heightAnim.update(status.size.height);
      this.size.width = this.widthAnim.getValue();
      this.size.height = this.heightAnim.getValue();
      this.extendingAnim.update(this.extended ? 1.0F : 0.0F);
      this.last = status;

      if (!(status instanceof ExtandableStatus)) {
         this.extended = false;
      }
      if (!(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof DropDownScreen) && mc.player != null || this.select) {
         this.extended = false;
      }
      if (!this.extended
         && status instanceof ExtandableStatus
         && GuiUtility.isHovered(this.x, this.y, this.width, this.height, GuiUtility.getMouse().getX(), GuiUtility.getMouse().getY())) {
         CursorUtility.set(CursorType.HAND);
      }
   }

   private void updateAdaptiveColor() {
      if (this.timer.finished(500L)) {
         // Sample the world under the island: the main framebuffer, not whatever the HUD draws into
         // (its cache target is transparent), at the island's position in framebuffer pixels.
         var main = mc.getFramebuffer();
         float scale = (float) mc.getWindow().getScaleFactor();
         float pixelX = main.textureWidth / 2.0F;
         float pixelY = main.textureHeight - (this.y + 5.0F) * scale;
         ColorRGBA pixel = ColorRGBA.fromFramebufferPixel(main, pixelX, pixelY);
         this.useDark = (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3.0F > 70.0F;
         this.timer.reset();
      }
      this.adaptColor.update(this.useDark ? ColorRGBA.BLACK : ColorRGBA.WHITE);
      this.dark = this.useDark;
   }

   private static final net.minecraft.util.Identifier AIRPLANE_ICON = BKClientInit.id("icons/airplane.png");
   /** "HH:mm" only changes between seconds, the ping label only with the latency value. */
   private long clockSecond = Long.MIN_VALUE;
   private String clockText;
   private int pingTextValue = Integer.MIN_VALUE;
   private String pingText;

   private String clockText() {
      long second = Math.floorDiv(System.currentTimeMillis(), 1000L);
      if (this.clockText == null || second != this.clockSecond) {
         this.clockSecond = second;
         this.clockText = TextUtility.getCurrentTime();
      }
      return this.clockText;
   }

   private void drawClockAndConnection(UIContext context) {
      if (mc.player == null) {
         return;
      }

      float sideAlpha = 1.0F - this.extendingAnim.getValue();
      ColorRGBA elementColor = this.adaptColor.getColor().withAlpha(255.0F * sideAlpha);
      String time = clockText();
      float clockX = swapSides.isEnabled() ? this.x + this.width + this.bubbleOffset + 7
         : this.x - Fonts.MEDIUM.getFont(7.0F).width(time) - 4;
      if (clockVisible.isEnabled()) context.drawText(Fonts.MEDIUM.getFont(7.0F), time, clockX, this.y + 5.0F, elementColor);
      if (!connectionVisible.isEnabled()) return;
      float connectionX = swapSides.isEnabled() ? this.x - 42 : this.x + this.width + this.bubbleOffset;


      if (mc.isInSingleplayer() || mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()) == null) {
         context.drawTextureSmooth(AIRPLANE_ICON, connectionX + 7.0F, this.y + 2.5F, 10.0F, 10.0F, elementColor);
         return;
      }

      float baseX = connectionX + 4.0F;
      this.showPing.update(GuiUtility.isHovered(baseX + 4.0F * this.showPing.getValue(), this.y + 5.0F, 12.8F, 7.0F, context));
      int ping = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()).getLatency();
      float reveal = this.showPing.getValue();
      if (this.pingText == null || ping != this.pingTextValue) {
         this.pingTextValue = ping;
         this.pingText = ping + " ms";
      }
      context.drawText(Fonts.MEDIUM.getFont(7.0F), this.pingText, baseX + 4.0F * reveal, this.y + 5.0F, elementColor.mulAlpha(reveal));

      for (int i = 0; i < 4; i++) {
         float barX = connectionX + 9.0F + i * 2.7F + 4.0F * reveal;
         float barY = this.y + 8.0F - i;
         context.drawRoundedRect(barX, barY, 2.0F, 3.0F + i, BorderRadius.all(0.1F), elementColor.withAlpha(elementColor.getAlpha() * 0.2F * (1.0F - reveal)));
         if (ping < PING_THRESHOLDS[i]) {
            context.drawRoundedRect(barX, barY, 2.0F, 3.0F + i, BorderRadius.all(0.1F), elementColor.mulAlpha(1.0F - reveal));
         }
      }
   }

   @Override
   public boolean isHovered(float mouseX, float mouseY) {
      float scale = this.currentScale();
      float scaledWidth = this.size.width * scale;
      float scaledHeight = this.size.height * scale;
      float pillX = sr.getScaledWidth() / 2.0F - scaledWidth / 2.0F;
      float pillY = DOCK_Y + (this.size.height - scaledHeight) / 2.0F;
      return GuiUtility.isHovered(pillX, pillY, scaledWidth, scaledHeight, mouseX, mouseY);
   }

   @Override
   public boolean isHovered(double mouseX, double mouseY) {
      return this.isHovered((float)mouseX, (float)mouseY);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      super.onMouseReleased(mouseX, mouseY, button);
      this.handleClick((float)mouseX, (float)mouseY, button.getButtonIndex());
   }

   public boolean handleClick(float mouseX, float mouseY, int button) {
      boolean hovered = this.isHovered(mouseX, mouseY);
      IslandStatus active = this.active();

      if (this.extended) {
         if (!hovered) {
            this.extended = false;
            if (mc.currentScreen instanceof ChatScreen) {
               mc.setScreen(null);
            }
         } else if (active != null) {
            float scale = Math.max(0.001F, this.currentScale());
            float centerX = sr.getScaledWidth() / 2.0F;
            float centerY = DOCK_Y + this.size.height / 2.0F;
            float localX = centerX + (mouseX - centerX) / scale;
            float localY = centerY + (mouseY - centerY) / scale;
            active.click(localX, localY - DOCK_Y, button);
         }
         return true;
      }

      if (!hovered || !(active instanceof ExtandableStatus)) {
         return false;
      }

      this.extended = true;
      if (button == MouseButton.LEFT.getButtonIndex() && mc.currentScreen == null) {
         mc.setScreen(new ChatScreen(""));
      }
      return true;
   }

   public IslandStatus active() {
      List<IslandStatus> activeStatuses = this.statuses();
      return activeStatuses.isEmpty() ? null : activeStatuses.getFirst();
   }

   public List<IslandStatus> statuses() {
      for (SelectSetting.Value value : this.statuses.getValues()) {
         if (value instanceof dev.bkclient.ui.hud.impl.island.impl.PVPStatus pvp && pvp.isPreviewActive()) {
            return List.of(pvp);
         }
         if (value instanceof dev.bkclient.ui.hud.impl.island.impl.FriendMessageStatus friend && friend.isPreviewActive()) {
            return List.of(friend);
         }
         if (value instanceof dev.bkclient.ui.hud.impl.island.impl.FriendRequestStatus request
            && request.canShow()) {
            return List.of(request);
         }
      }
      // Plain loops, same evaluation order as the former filter/map/findFirst streams.
      List<IslandStatus> activeStatuses = new ArrayList<>();
      for (SelectSetting.Value value : this.statuses.getValues()) {
         if (value.isSelected() && ((IslandStatus)value).canShow()) activeStatuses.add((IslandStatus)value);
      }

      if (activeStatuses.size() > 1) {
         activeStatuses.removeIf(status -> status instanceof DefaultStatus);
      }
      // Keep short notifications ahead of the persistent combat direction indicator.
      IslandStatus displacement = null;
      for (IslandStatus status : activeStatuses) {
         if (status instanceof dev.bkclient.ui.hud.impl.island.impl.DisplacementStatus) { displacement = status; break; }
      }
      if (displacement != null) {
         activeStatuses.remove(displacement);
         int index = 0;
         while (index < activeStatuses.size() && (activeStatuses.get(index) instanceof NotificationStatus
            || activeStatuses.get(index) instanceof ConfigStatus
            || activeStatuses.get(index) instanceof TinyTaskStatus
            || activeStatuses.get(index) instanceof dev.bkclient.ui.hud.impl.island.impl.FriendMessageStatus
            || activeStatuses.get(index) instanceof dev.bkclient.ui.hud.impl.island.impl.FriendRequestStatus)) index++;
         activeStatuses.add(index, displacement);
      }
      return activeStatuses;
   }

   public boolean isMusicLyricsEnabled() {
      return this.musicLyrics.isEnabled();
   }

   private float currentScale() {
      float anim = this.animation.getValue() * this.visible.getValue();
      return (0.5F + anim * 0.5F - 0.05F * this.selecting.getValue())
         * this.pressAnim.getValue()
         * this.getScale()
         * COMPACT_SCALE;
   }

   @Generated
   public SelectSetting getStatuses() {
      return this.statuses;
   }

   @Generated
   public IslandSize getSize() {
      return this.size;
   }

   @Generated
   public boolean isExtended() {
      return this.extended;
   }

   @Generated
   public Animation getExtendingAnim() {
      return this.extendingAnim;
   }

   @Generated
   @Override
   public Animation getWidthAnim() {
      return this.widthAnim;
   }

   @Generated
   @Override
   public Animation getHeightAnim() {
      return this.heightAnim;
   }

   @Generated
   public Animation getShowPing() {
      return this.showPing;
   }

   @Generated
   public ColorAnimation getBackgroundColor() {
      return this.backgroundColor;
   }

   @Generated
   public ColorAnimation getAdaptColor() {
      return this.adaptColor;
   }

   @Generated
   public Timer getTimer() {
      return this.timer;
   }

   @Generated
   public boolean isDark() {
      return this.dark;
   }

   @Generated
   public boolean isUseDark() {
      return this.useDark;
   }

   @Generated
   public IslandStatus getLast() {
      return this.last;
   }

   @Generated
   public EventListener<ClientPlayerTickEvent> getOnTick() {
      return this.onTick;
   }
}
