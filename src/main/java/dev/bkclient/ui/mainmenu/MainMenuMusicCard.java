package dev.bkclient.ui.mainmenu;

import dev.bkclient.BKClientInit;
import dev.bkclient.ui.components.MusicSlider;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.sounds.LyricsFetcher;
import dev.bkclient.utility.sounds.MusicTracker;
import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import net.minecraft.util.Identifier;

/** Liquid glass "now playing" music card for the main menu, matching button style 1:1. */
public class MainMenuMusicCard implements IMinecraft {
   private static final float WIDTH = 215.0F;
   private static final float HEIGHT = 76.0F;
   private static final float PAD = 11.0F;
   private static final float ART_SIZE = 32.0F;
   private static final float CONTROL_SIZE = 10.0F;
   private static final boolean FORCE_PREVIEW = true;
   private static final String PREVIEW_TITLE = "Night Drive";
   private static final String PREVIEW_ARTIST = "BK-Client Preview";

   private final ColorRGBA backgroundColor = new ColorRGBA(58.0F, 58.0F, 58.0F);
   private final Animation cardHoverAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation prevHoverAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation playHoverAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation nextHoverAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   private float x;
   private float y;
   private float prevX;
   private float playX;
   private float nextX;
   private float controlsY;
   private float barX;
   private float barY;
   private float barW;

   public boolean shouldShow() {
      return FORCE_PREVIEW || this.tracker().haveActiveSession();
   }

   public void render(UIContext context, float centerX, float topY, float alpha) {
      if (alpha <= 0.001F) {
         return;
      }
      this.x = centerX - WIDTH / 2.0F;
      this.y = topY;

      boolean cardHovered = GuiUtility.isHovered(this.x, this.y, WIDTH, HEIGHT, context.getMouseX(), context.getMouseY());
      this.cardHoverAnim.update(cardHovered);
      float cardHover = this.cardHoverAnim.getValue();

      BorderRadius cardRadius = BorderRadius.all(16.0F);

      if (Interface.showGlass()) {
         context.drawLiquidGlass(
            this.x - 1.0F, this.y - 1.0F, WIDTH + 2.0F, HEIGHT + 2.0F, 7.0F, 0.08F, cardRadius, ColorRGBA.WHITE.withAlpha(255.0F * alpha)
         );
         context.drawRoundedRect(this.x, this.y, WIDTH, HEIGHT, cardRadius, this.backgroundColor.withAlpha(255.0F * (0.15F * alpha + 0.08F * cardHover * alpha)));
      } else {
         context.drawRoundedRect(this.x, this.y, WIDTH, HEIGHT, cardRadius, this.backgroundColor.withAlpha(255.0F * (0.33F * alpha + 0.15F * cardHover * alpha)));
      }

      MediaInfo media = this.media();
      String rawTitle = media == null ? PREVIEW_TITLE : media.getTitle();
      String rawArtist = media == null ? PREVIEW_ARTIST : media.getArtist();
      LyricsFetcher.ParsedTrack parsed = LyricsFetcher.parseTrackInfo(rawArtist, rawTitle);
      String title = !parsed.title().isEmpty() ? parsed.title() : (rawTitle == null || rawTitle.isBlank() ? PREVIEW_TITLE : cleanTitle(rawTitle));
      String artist = !parsed.artist().isEmpty() ? parsed.artist() : (rawArtist == null || rawArtist.isBlank() ? PREVIEW_ARTIST : rawArtist);
      long position = media == null ? 74L : Math.max(0L, media.getPosition());
      long duration = media == null ? 172L : Math.max(0L, media.getDuration());
      float progress = duration <= 0L ? 0.0F : Math.min(1.0F, (float)position / duration);

      float artX = this.x + PAD;
      float artY = this.y + 10.0F;
      Identifier artwork = this.tracker().getImage();
      if (artwork != null) {
         context.drawRoundedTexture(artwork, artX, artY, ART_SIZE, ART_SIZE, BorderRadius.all(8.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
      } else {
         context.drawRoundedRect(artX, artY, ART_SIZE, ART_SIZE, BorderRadius.all(8.0F), this.backgroundColor.withAlpha(255.0F * 0.35F * alpha));
         float iconSize = 14.0F;
         context.drawTextureSmooth(
            BKClientInit.id("icons/music/no_image.png"), artX + (ART_SIZE - iconSize) / 2.0F, artY + (ART_SIZE - iconSize) / 2.0F,
            iconSize, iconSize, ColorRGBA.WHITE.withAlpha(180.0F * alpha)
         );
      }

      Font titleFont = Fonts.MEDIUM.getFont(8.0F);
      Font artistFont = Fonts.REGULAR.getFont(7.0F);
      float textX = artX + ART_SIZE + 10.0F;
      float maxWidth = this.x + WIDTH - PAD - textX;
      context.drawFadeoutText(titleFont, title, textX, artY + 3.0F, ColorRGBA.WHITE.withAlpha(240.0F * alpha), 0.85F, 1.0F, maxWidth);
      context.drawFadeoutText(artistFont, artist, textX, artY + 16.0F, ColorRGBA.WHITE.withAlpha(140.0F * alpha), 0.85F, 1.0F, maxWidth);

      this.barY = artY + ART_SIZE + 10.0F;
      this.barX = this.x + PAD;
      this.barW = WIDTH - PAD * 2.0F;
      Font timeFont = Fonts.REGULAR.getFont(6.0F);
      context.drawText(timeFont, formatTime(position), this.barX, this.barY - 7.5F, ColorRGBA.WHITE.withAlpha(140.0F * alpha));
      context.drawRightText(timeFont, duration <= 0L ? "Live" : formatTime(duration), this.barX + this.barW, this.barY - 7.5F, ColorRGBA.WHITE.withAlpha(140.0F * alpha));
      MusicSlider.draw(context, this.barX, this.barY, this.barW, 0, progress,
         false, duration > 0L, alpha);

      this.controlsY = this.barY + 9.0F;
      float center = this.x + WIDTH / 2.0F;
      this.prevX = center - 26.0F;
      this.playX = center - CONTROL_SIZE / 2.0F;
      this.nextX = center + 16.0F;

      float hitBox = 16.0F;
      boolean prevHovered = GuiUtility.isHovered(this.prevX - (hitBox - CONTROL_SIZE) / 2.0F, this.controlsY - (hitBox - CONTROL_SIZE) / 2.0F, hitBox, hitBox, context.getMouseX(), context.getMouseY());
      boolean playHovered = GuiUtility.isHovered(this.playX - (hitBox - CONTROL_SIZE) / 2.0F, this.controlsY - (hitBox - CONTROL_SIZE) / 2.0F, hitBox, hitBox, context.getMouseX(), context.getMouseY());
      boolean nextHovered = GuiUtility.isHovered(this.nextX - (hitBox - CONTROL_SIZE) / 2.0F, this.controlsY - (hitBox - CONTROL_SIZE) / 2.0F, hitBox, hitBox, context.getMouseX(), context.getMouseY());

      this.prevHoverAnim.update(prevHovered);
      this.playHoverAnim.update(playHovered);
      this.nextHoverAnim.update(nextHovered);

      if (cardHovered || prevHovered || playHovered || nextHovered) {
         CursorUtility.set(CursorType.HAND);
      }

      float prevAlpha = 190.0F + 65.0F * this.prevHoverAnim.getValue();
      float playAlpha = 220.0F + 35.0F * this.playHoverAnim.getValue();
      float nextAlpha = 190.0F + 65.0F * this.nextHoverAnim.getValue();

      LucideIcons.MEDIA_PREVIOUS.draw(context, this.prevX, this.controlsY, CONTROL_SIZE, ColorRGBA.WHITE.withAlpha(prevAlpha * alpha));
      LucideIcons playIcon = media == null || media.isPlaying() ? LucideIcons.MEDIA_PAUSE : LucideIcons.MEDIA_PLAY;
      playIcon.draw(context, this.playX, this.controlsY, CONTROL_SIZE, ColorRGBA.WHITE.withAlpha(playAlpha * alpha));
      LucideIcons.MEDIA_NEXT.draw(context, this.nextX, this.controlsY, CONTROL_SIZE, ColorRGBA.WHITE.withAlpha(nextAlpha * alpha));
   }

   public boolean onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (button != MouseButton.LEFT || !GuiUtility.isHovered(this.x, this.y, WIDTH, HEIGHT, mouseX, mouseY)) {
         return false;
      }
      IMediaSession session = this.tracker().getSession();
      float hit = 16.0F;
      if (session != null) {
         if (GuiUtility.isHovered(this.prevX - (hit - CONTROL_SIZE) / 2.0F, this.controlsY - (hit - CONTROL_SIZE) / 2.0F, hit, hit, mouseX, mouseY)) {
            session.previous();
            return true;
         } else if (GuiUtility.isHovered(this.playX - (hit - CONTROL_SIZE) / 2.0F, this.controlsY - (hit - CONTROL_SIZE) / 2.0F, hit, hit, mouseX, mouseY)) {
            session.playPause();
            return true;
         } else if (GuiUtility.isHovered(this.nextX - (hit - CONTROL_SIZE) / 2.0F, this.controlsY - (hit - CONTROL_SIZE) / 2.0F, hit, hit, mouseX, mouseY)) {
            session.next();
            return true;
         }
      }

      // Open the Live Synced Lyrics screen
      mc.setScreen(new MusicLyricsScreen(mc.currentScreen));
      return true;
   }

   public float getWidth() {
      return WIDTH;
   }

   public float getHeight() {
      return HEIGHT;
   }

   private MusicTracker tracker() {
      return BKClientInit.getInstance().getMusicTracker();
   }

   private MediaInfo media() {
      IMediaSession session = this.tracker().getSession();
      return session == null ? null : session.getMedia();
   }

   private static String cleanTitle(String title) {
      String cleaned = title.replaceAll("^\\(\\d+\\)\\s*", "").replaceAll("^\\[\\d+\\]\\s*", "").trim();
      return cleaned.isBlank() ? "Unknown" : cleaned;
   }

   private static String formatTime(long seconds) {
      return String.format("%d:%02d", seconds / 60L, seconds % 60L);
   }
}
