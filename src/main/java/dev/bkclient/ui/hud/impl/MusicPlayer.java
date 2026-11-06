package dev.bkclient.ui.hud.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.framework.objects.gradient.impl.VerticalGradient;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.ui.hud.HudList;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.sounds.LyricsFetcher;
import dev.bkclient.utility.sounds.MusicTracker;
import dev.bkclient.utility.sounds.SyncedLyrics;
import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.Identifier;

/** Music preview content rendered with both Compact and Extended (Big with Live Lyrics) modes. */
public class MusicPlayer extends HudList {
   private static final Identifier NO_IMAGE_ICON = BKClientInit.id("icons/music/no_image.png");
   private static final Identifier PREVIOUS_ICON = BKClientInit.id("icons/music/previous.png");
   private static final Identifier NEXT_ICON = BKClientInit.id("icons/music/next.png");
   private static final Identifier PAUSE_ICON = BKClientInit.id("icons/music/pause.png");
   private static final Identifier PLAY_ICON = BKClientInit.id("icons/music/play.png");
   /** Maps the play/pause path to its constant id; any other path is resolved as before. */
   private static Identifier controlIcon(String path) {
      return "icons/music/pause.png".equals(path) ? PAUSE_ICON : "icons/music/play.png".equals(path) ? PLAY_ICON : BKClientInit.id(path);
   }
   private static final float WIDTH = 116.0F;
   private static final float HEIGHT = 76.0F;
   private static final float HEADER_HEIGHT = 22.0F;
   private static final float INFO_HEIGHT = 25.0F;
   private static final float PROGRESS_HEIGHT = 13.0F;
   private static final float CONTROLS_HEIGHT = 16.0F;
   private static final float SIDE_PADDING = 7.0F;

   private static final String PREVIEW_TITLE = "Cinema - Skrillex Remix";
   private static final String PREVIEW_ARTIST = "Benny Benassi";

   private final ModeSetting mode = new ModeSetting(this, "hud.music.mode");
   private final ModeSetting.Value modeCompact = new ModeSetting.Value(this.mode, "hud.music.mode.compact").select();
   private final ModeSetting.Value modeBig = new ModeSetting.Value(this.mode, "hud.music.mode.big");
   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");

   private final ModeSetting coverPosition = new ModeSetting(this, "hud.music.cover_position", () -> modeBig.isSelected());
   private final ModeSetting.Value coverLeft = new ModeSetting.Value(coverPosition, "hud.editor.left").select();
   private final ModeSetting.Value coverRight = new ModeSetting.Value(coverPosition, "hud.editor.right");
   private final BooleanSetting showArtist = new BooleanSetting(this, "hud.music.artist").enable();
   private final BooleanSetting showControls = new BooleanSetting(this, "hud.music.controls").enable();
   private final BooleanSetting scrollTitle = new BooleanSetting(this, "hud.music.scroll_title");
   private String marqueeTitle = "";
   private long marqueeStart;
   private String lastLyricText = "";
   private String prevLyricText = "";
   private float lyricFade = 1.0F;
   private float displayedProgress;
   private long progressFrameNanos;
   private String progressTrack = "";

   public MusicPlayer() {
      super("hud.music", "icons/music/play.png");
   }

   @Override
   public void update(UIContext context) {
      if (this.modeBig.isSelected()) {
         this.width = 146.0F;
         this.height = 208.0F;
      } else {
         this.width = HudTheme.isOld() ? 90.0F : WIDTH;
         this.height = HudTheme.isOld() ? 46.0F : HEIGHT;
      }
      if (!showControls.isEnabled()) this.height -= modeBig.isSelected() ? 22 : HudTheme.isOld() ? 12 : CONTROLS_HEIGHT;
      if (this.lyricFade < 1.0F) {
         this.lyricFade = Math.min(1.0F, this.lyricFade + 0.09F);
      }
      super.update(context);
   }

   @Override
   protected void renderContent(UIContext context) {
      if (this.modeBig.isSelected()) {
         this.renderBigContent(context);
         return;
      }

      if (HudTheme.isOld()) {
         this.renderOldContent(context);
         return;
      }

      // Compact Liquid frame
      super.renderContent(context);
      renderTrackInfo(context, this.y + HEADER_HEIGHT);
      renderProgress(context, this.y + HEADER_HEIGHT + INFO_HEIGHT);
      if (showControls.isEnabled()) renderControls(context, this.y + HEADER_HEIGHT + INFO_HEIGHT + PROGRESS_HEIGHT);
   }

   private void renderBigContent(UIContext context) {
      HudTheme.drawPanel(context, this.x, this.y, this.width, this.height, this.animation.getValue(), this.dragAnim.getValue(), 14.0F);

      MediaInfo media = media();
      String rawTitle = media == null ? PREVIEW_TITLE : media.getTitle();
      String rawArtist = media == null ? PREVIEW_ARTIST : media.getArtist();
      LyricsFetcher.ParsedTrack parsed = LyricsFetcher.parseTrackInfo(rawArtist, rawTitle);
      String title = !parsed.title().isEmpty() ? parsed.title() : (rawTitle == null || rawTitle.isBlank() ? PREVIEW_TITLE : cleanTitle(rawTitle));
      String artist = !parsed.artist().isEmpty() ? parsed.artist() : (rawArtist == null || rawArtist.isBlank() ? PREVIEW_ARTIST : rawArtist);

      long positionMs = tracker().getSmoothPositionMs();
      long durationSec = media == null ? 307L : Math.max(0L, media.getDuration());
      long positionSec = positionMs / 1000L;
      float progress = durationSec <= 0L ? 0.0F : Math.min(1.0F, (float)positionMs / (durationSec * 1000.0F));

      float pad = 8.0F;
      float artW = this.width - pad * 2.0F;
      float artH = 130.0F;
      float artX = this.x + pad;
      float artY = this.y + pad;
      BorderRadius artRadius = BorderRadius.all(10.0F);

      // 1. Cover Art
      Identifier artwork = tracker().getImage();
      if (artwork != null) {
         context.drawRoundedTexture(artwork, artX, artY, artW, artH, artRadius);
      } else {
         context.drawRoundedRect(artX, artY, artW, artH, artRadius, Colors.getAccent().withAlpha(65.0F));
         context.drawTextureSmooth(NO_IMAGE_ICON, artX + (artW - 28.0F) / 2.0F, artY + (artH - 28.0F) / 2.0F, 28.0F, 28.0F, HudTheme.textColor());
      }

      // Dark gradient overlay at bottom of cover for crisp text contrast
      context.drawRoundedRect(
         artX, artY + artH * 0.40F, artW, artH * 0.60F,
         BorderRadius.bottom(10.0F),
         new VerticalGradient(ColorRGBA.BLACK.withAlpha(0.0F), ColorRGBA.BLACK.withAlpha(225.0F))
      );

      // 2. Title & Artist overlaid on bottom of cover art
      Font titleFont = Fonts.SEMIBOLD.getFont(7.2F);
      Font artistFont = Fonts.MEDIUM.getFont(5.6F);
      float metadataWidth = artW - 16.0F;
      String visibleTitle = ellipsize(titleFont, title, metadataWidth);
      String visibleArtist = ellipsize(artistFont, artist, metadataWidth);
      drawTitle(context, titleFont, title, artX + 8, artY + artH - (showArtist.isEnabled() ? 23 : 17), metadataWidth, ColorRGBA.WHITE, true);
      if (showArtist.isEnabled()) context.drawCenteredText(artistFont, visibleArtist, artX + artW / 2.0F, artY + artH - 12.0F, ColorRGBA.WHITE.withAlpha(165.0F));

      // 3. Live Synchronized Lyrics Line
      float lyricsY = artY + artH + 8.5F;
      SyncedLyrics syncedLyrics = tracker().getSyncedLyrics();
      int activeIdx = syncedLyrics != null ? syncedLyrics.getActiveIndex(positionMs) : -1;
      String lyricText = "";
      if (syncedLyrics != null && !syncedLyrics.isEmpty() && activeIdx >= 0) {
         if (syncedLyrics.isLineActive(activeIdx, positionMs)) {
            lyricText = syncedLyrics.getLines().get(activeIdx).text();
         }
      } else if (media == null) {
         lyricText = "You are a cinema I could watch you forever";
      }

      if (!lyricText.equals(this.lastLyricText)) {
         this.prevLyricText = this.lastLyricText;
         this.lastLyricText = lyricText;
         this.lyricFade = 0.0F;
      }

      float ease = (float)(1.0 - Math.pow(1.0 - this.lyricFade, 3));
      float centerX = artX + artW / 2.0F;

      if (this.lyricFade < 1.0F && !this.prevLyricText.isBlank()) {
         float prevAlpha = (1.0F - ease);
         float prevY = lyricsY - ease * 4.0F;
         Font prevFont = getFittedFont(this.prevLyricText, artW);
         String visiblePreviousLyric = ellipsize(prevFont, this.prevLyricText, artW - 8.0F);
         context.drawCenteredText(prevFont, visiblePreviousLyric, centerX + 0.5F, prevY + 0.5F, ColorRGBA.BLACK.withAlpha(90.0F * prevAlpha));
         context.drawCenteredText(prevFont, visiblePreviousLyric, centerX, prevY, ColorRGBA.WHITE.withAlpha(255.0F * prevAlpha));
      }

      if (!lyricText.isBlank()) {
         float curAlpha = ease;
         float curY = lyricsY + (1.0F - ease) * 4.0F;
         Font curFont = getFittedFont(lyricText, artW);
         String visibleLyric = ellipsize(curFont, lyricText, artW - 8.0F);
         context.drawCenteredText(curFont, visibleLyric, centerX + 0.5F, curY + 0.5F, ColorRGBA.BLACK.withAlpha(100.0F * curAlpha));
         context.drawCenteredText(curFont, visibleLyric, centerX, curY, ColorRGBA.WHITE.withAlpha(255.0F * curAlpha));
      }

      // 4. Progress bar & Timestamps
      float barY = lyricsY + 15.0F;
      Font timeFont = Fonts.MEDIUM.getFont(5.2F);
      String elapsed = formatTime(positionSec);
      String total = durationSec <= 0L ? "Live" : formatTime(durationSec);
      context.drawText(timeFont, elapsed, artX, barY - 1.0F, HudTheme.textColor().withAlpha(190.0F));
      context.drawRightText(timeFont, total, artX + artW, barY - 1.0F, HudTheme.textColor().withAlpha(190.0F));

      float barLeft = artX + 22.0F;
      float barWidth = artW - 44.0F;
      float barCenterY = barY + 1.5F;
      drawPlaybackBar(context, barLeft, barCenterY, barWidth, progress, media);

      if (!showControls.isEnabled()) return;
      // 5. Media Controls (Previous, Play/Pause, Next)
      float controlsY = barY + 13.0F;
      float center = this.x + this.width / 2.0F;
      float iconSize = 8.5F;

      // Previous button
      context.drawTexture(PREVIOUS_ICON, center - 24.0F, controlsY,
         iconSize, iconSize, HudTheme.textColor().withAlpha(220.0F));

      // Play/Pause button
      float playBox = 16.0F;
      float playX = center - playBox / 2.0F;
      float playY = controlsY - 3.5F;
      context.drawRoundedRect(playX, playY, playBox, playBox, BorderRadius.all(8.0F),
         Colors.getAccent().withAlpha(200.0F));
      String playIcon = (media == null || media.isPlaying()) ? "icons/music/pause.png" : "icons/music/play.png";
      context.drawTexture(controlIcon(playIcon), center - 4.5F,
         playY + 3.5F, 9.0F, 9.0F, HudTheme.strongTextColor());

      // Next button
      context.drawTexture(NEXT_ICON, center + 16.0F, controlsY,
         iconSize, iconSize, HudTheme.textColor().withAlpha(220.0F));
   }

   private void renderOldContent(UIContext context) {
      HudTheme.drawPanel(context, this.x, this.y, this.width, this.height, this.animation.getValue(), this.dragAnim.getValue(), 8.0F);
      MediaInfo media = media();
      String rawTitle = media == null ? PREVIEW_TITLE : media.getTitle();
      String rawArtist = media == null ? PREVIEW_ARTIST : media.getArtist();
      LyricsFetcher.ParsedTrack parsed = LyricsFetcher.parseTrackInfo(rawArtist, rawTitle);
      String title = !parsed.title().isEmpty() ? parsed.title() : (rawTitle == null || rawTitle.isBlank() ? PREVIEW_TITLE : cleanTitle(rawTitle));
      String artist = !parsed.artist().isEmpty() ? parsed.artist() : (rawArtist == null || rawArtist.isBlank() ? PREVIEW_ARTIST : rawArtist);
      long position = media == null ? 74L : Math.max(0L, media.getPosition());
      long duration = media == null ? 172L : Math.max(0L, media.getDuration());
      float progress = duration <= 0L ? 0.0F : Math.min(1.0F, (float)position / duration);

      float artX = coverRight.isSelected() ? this.x + this.width - 22 : this.x + 6;
      Identifier artwork = tracker().getImage();
      if (artwork != null) {
         context.drawRoundedTexture(artwork, artX, this.y + 6, 16, 16, BorderRadius.all(5));
      } else {
         context.drawRoundedRect(artX, this.y + 6, 16, 16, BorderRadius.all(5), Colors.getAccent().withAlpha(100));
         context.drawTextureSmooth(NO_IMAGE_ICON, artX + 4, this.y + 10, 8, 8, HudTheme.textColor());
      }
      Font textFont = Fonts.MEDIUM.getFont(5.5F);
      float textX = coverRight.isSelected() ? this.x + 6 : this.x + 26;
      float textWidth = this.width - 30;
      drawTitle(context, textFont, title, textX, this.y + (showArtist.isEnabled() ? 7.5F : 11.5F), textWidth, HudTheme.textColor(), false);
      if (showArtist.isEnabled()) context.drawFadeoutText(textFont, ellipsize(textFont, artist, textWidth), textX, this.y + 15.5F, HudTheme.textColor().withAlpha(64), .8F, 1, textWidth);

      Font timeFont = Fonts.BOLD.getFont(5.0F);
      String elapsed = formatTime(position);
      String total = duration <= 0L ? "Live" : formatTime(duration);
      context.drawText(timeFont, elapsed, this.x + 4.0F, this.y + 27.0F, HudTheme.textColor());
      context.drawRightText(timeFont, total, this.x + 88.5F, this.y + 27.0F, HudTheme.textColor());
      drawPlaybackBar(context, this.x + 18.5F, this.y + 28.0F, 56.0F, progress, media);

      if (!showControls.isEnabled()) return;
      float iconY = this.y + 34.0F;
      context.drawTexture(PREVIOUS_ICON, this.x + 31.0F, iconY, 8.5F, 8.5F, Colors.getAccent());
      String playIcon = media == null || media.isPlaying() ? "icons/music/pause.png" : "icons/music/play.png";
      context.drawTexture(controlIcon(playIcon), this.x + 42.0F, iconY, 8.5F, 8.5F, Colors.getAccent());
      context.drawTexture(NEXT_ICON, this.x + 53.0F, iconY, 8.5F, 8.5F, Colors.getAccent());
   }

   private void renderTrackInfo(UIContext context, float y) {
      MediaInfo media = media();
      String rawTitle = media == null ? PREVIEW_TITLE : media.getTitle();
      String rawArtist = media == null ? PREVIEW_ARTIST : media.getArtist();
      LyricsFetcher.ParsedTrack parsed = LyricsFetcher.parseTrackInfo(rawArtist, rawTitle);
      String title = !parsed.title().isEmpty() ? parsed.title() : (rawTitle == null || rawTitle.isBlank() ? PREVIEW_TITLE : cleanTitle(rawTitle));
      String artist = !parsed.artist().isEmpty() ? parsed.artist() : (rawArtist == null || rawArtist.isBlank() ? PREVIEW_ARTIST : rawArtist);
      float artX = coverRight.isSelected() ? this.x + this.width - SIDE_PADDING - 17 : this.x + SIDE_PADDING;
      float artY = y + 4.0F;
      float artSize = 17.0F;
      Identifier artwork = tracker().getImage();
      if (artwork != null) {
         context.drawRoundedTexture(artwork, artX, artY, artSize, artSize, BorderRadius.all(5.0F));
      } else {
         context.drawRoundedRect(artX, artY, artSize, artSize, BorderRadius.all(5.0F), Colors.getAccent().withAlpha(65.0F));
         context.drawTextureSmooth(NO_IMAGE_ICON, artX + 4.5F, artY + 4.5F,
            8.0F, 8.0F, HudTheme.textColor());
      }

      Font titleFont = Fonts.MEDIUM.getFont(7.0F);
      Font artistFont = Fonts.REGULAR.getFont(6.0F);
      float textX = coverRight.isSelected() ? this.x + SIDE_PADDING : artX + artSize + 6;
      float maxWidth = this.width - SIDE_PADDING * 2 - artSize - 6;
      drawTitle(context, titleFont, title, textX, y + (showArtist.isEnabled() ? 4.5F : 9), maxWidth, HudTheme.textColor(), false);
      if (showArtist.isEnabled()) context.drawFadeoutText(artistFont, ellipsize(artistFont, artist, maxWidth), textX, y + 14,
         HudTheme.textColor().withAlpha(140), .8F, 1, maxWidth);
   }

   private void renderProgress(UIContext context, float y) {
      MediaInfo media = media();
      long position = media == null ? 74L : Math.max(0L, media.getPosition());
      long duration = media == null ? 172L : Math.max(0L, media.getDuration());
      float progress = duration <= 0L ? 0.0F : Math.min(1.0F, (float)position / duration);
      context.drawRect(this.x, y, this.width, 0.5F, HudTheme.textColor().withAlpha(5.1F));
      Font timeFont = Fonts.MEDIUM.getFont(5.0F);
      float textY = y + GuiUtility.getMiddleOfBox(timeFont.height(), PROGRESS_HEIGHT);
      context.drawText(timeFont, formatTime(position), this.x + SIDE_PADDING, textY, HudTheme.textColor());
      context.drawRightText(timeFont, duration <= 0L ? "Live" : formatTime(duration), this.x + this.width - SIDE_PADDING, textY, HudTheme.textColor());

      float barX = this.x + 25.0F;
      float barWidth = this.width - 50.0F;
      float barY = y + 5.5F;
      drawPlaybackBar(context, barX, barY, barWidth, progress, media);
   }

   private void drawPlaybackBar(UIContext context, float x, float y, float width, float previewProgress, MediaInfo media) {
      String track = media == null ? "preview" : tracker().getLastTrack();
      float target = media == null ? previewProgress : media.getDuration() > 0L
         ? Math.min(1.0F, tracker().getSmoothPositionMs() / (media.getDuration() * 1000.0F)) : 0.0F;
      long now = System.nanoTime();
      float delta = this.progressFrameNanos == 0L ? 0.0F : Math.min(0.1F, (now - this.progressFrameNanos) / 1_000_000_000.0F);
      if (!track.equals(this.progressTrack) || this.progressFrameNanos == 0L || Math.abs(target - this.displayedProgress) > 0.04F) {
         this.displayedProgress = target;
      } else {
         this.displayedProgress += (target - this.displayedProgress) * (1.0F - (float)Math.exp(-delta * 14.0F));
      }
      this.progressTrack = track;
      this.progressFrameNanos = now;

      dev.bkclient.ui.components.MusicSlider.draw(context, x, y, width, 0, this.displayedProgress,
         false, media == null || media.getDuration() > 0, 1);

   }

   private void renderControls(UIContext context, float y) {
      context.drawRect(this.x, y, this.width, 0.5F, HudTheme.textColor().withAlpha(5.1F));
      float iconSize = 7.0F;
      float iconY = y + GuiUtility.getMiddleOfBox(iconSize, CONTROLS_HEIGHT);
      float center = this.x + this.width / 2.0F;

      context.drawTexture(PREVIOUS_ICON, center - 22.0F, iconY,
         iconSize, iconSize, HudTheme.textColor());

      float playBox = 12.0F;
      float playX = center - playBox / 2.0F;
      float playY = y + GuiUtility.getMiddleOfBox(playBox, CONTROLS_HEIGHT);
      context.drawRoundedRect(playX, playY, playBox, playBox, BorderRadius.all(5.0F),
         Colors.getAccent().withAlpha(190.0F));
      MediaInfo media = media();
      String playIcon = media == null || media.isPlaying() ? "icons/music/pause.png" : "icons/music/play.png";
      context.drawTexture(controlIcon(playIcon), center - 3.5F,
         playY + 2.5F, iconSize, iconSize, HudTheme.strongTextColor());

      context.drawTexture(NEXT_ICON, center + 15.0F, iconY,
         iconSize, iconSize, HudTheme.textColor());
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      IMediaSession session = tracker().getSession();
      if (button == MouseButton.LEFT && showControls.isEnabled()) {
         float localX = localMouseX(mouseX);
         float localY = localMouseY(mouseY);

         if (this.modeBig.isSelected()) {
            float pad = 8.0F;
            float artW = this.width - pad * 2.0F;
            float artH = 130.0F;
            float artX = this.x + pad;
            float artY = this.y + pad;
            float lyricsY = artY + artH + 8.5F;
            float barY = lyricsY + 15.0F;
            float controlsY = barY + 13.0F;
            float center = this.x + this.width / 2.0F;

            // 1. Controls buttons (Previous, Play/Pause, Next)
            if (localY >= controlsY - 5.0F && localY <= controlsY + 16.0F) {
               if (localX >= center - 32.0F && localX <= center - 14.0F) {
                  if (session != null) session.previous();
                  return;
               }
               if (localX >= center - 12.0F && localX <= center + 12.0F) {
                  if (session != null) session.playPause();
                  return;
               }
               if (localX >= center + 14.0F && localX <= center + 32.0F) {
                  if (session != null) session.next();
                  return;
               }
            }

            // 2. Cover Art clicks (Left = Prev, Center = Play/Pause, Right = Next)
            if (session != null && localX >= artX && localX <= artX + artW && localY >= artY && localY <= artY + artH) {
               float relX = localX - artX;
               if (relX < artW * 0.30F) {
                  session.previous();
                  return;
               } else if (relX > artW * 0.70F) {
                  session.next();
                  return;
               } else {
                  session.playPause();
                  return;
               }
            }
         } else {
            if (session != null) {
               if (HudTheme.isOld() && localY >= this.y + 33.0F && localY <= this.y + 44.0F) {
                  if (localX >= this.x + 28.0F && localX <= this.x + 40.0F) {
                     session.previous();
                     return;
                  }
                  if (localX >= this.x + 40.0F && localX <= this.x + 52.0F) {
                     session.playPause();
                     return;
                  }
                  if (localX >= this.x + 52.0F && localX <= this.x + 64.0F) {
                     session.next();
                     return;
                  }
               }
               float controlsY = this.y + HEADER_HEIGHT + INFO_HEIGHT + PROGRESS_HEIGHT;
               float center = this.x + this.width / 2.0F;
               if (localY >= controlsY && localY <= controlsY + CONTROLS_HEIGHT) {
                  if (localX >= center - 25.0F && localX <= center - 12.0F) {
                     session.previous();
                     return;
                  }
                  if (localX >= center - 8.0F && localX <= center + 8.0F) {
                     session.playPause();
                     return;
                  }
                  if (localX >= center + 12.0F && localX <= center + 25.0F) {
                     session.next();
                     return;
                  }
               }
            }
         }
      }
      super.onMouseClicked(mouseX, mouseY, button);
   }

   private float localMouseX(double mouseX) {
      float scale = Math.max(0.001F, this.getScale());
      float center = this.x + this.width / 2.0F;
      return center + ((float)mouseX - center) / scale;
   }

   private float localMouseY(double mouseY) {
      float scale = Math.max(0.001F, this.getScale());
      float center = this.y + this.height / 2.0F;
      return center + ((float)mouseY - center) / scale;
   }

   private MusicTracker tracker() {
      return BKClientInit.getInstance().getMusicTracker();
   }

   private MediaInfo media() {
      IMediaSession session = tracker().getSession();
      return session == null ? null : session.getMedia();
   }

   private void drawTitle(UIContext context, Font font, String title, float x, float y, float width, ColorRGBA color, boolean centered) {
      if (title == null) title = "";
      float overflow = font.width(title) - width;
      if (!scrollTitle.isEnabled() || overflow <= 0) {
         String text = ellipsize(font, title, width);
         context.drawText(font, text, centered ? x + (width - font.width(text)) / 2 : x, y, color);
         return;
      }
      if (!title.equals(marqueeTitle)) { marqueeTitle = title; marqueeStart = System.nanoTime(); }
      double seconds = Math.max(0, (System.nanoTime() - marqueeStart) / 1_000_000_000.0 - 1);
      float offset = overflow * (float)(.5 - .5 * Math.cos(seconds * Math.PI / Math.max(3, overflow / 12)));
      dev.bkclient.utility.render.ScissorUtility.push(context.getMatrices(), x, y - 1, width, font.height() + 3);
      try { context.drawText(font, title, x - offset, y, color); context.draw(); }
      finally { dev.bkclient.utility.render.ScissorUtility.pop(); }
   }

   /** Last cleanTitle input and result as one array, so a reader never sees a mismatched pair. */
   private static volatile String[] cleanTitleCache;
   private static String cleanTitle(String title) {
      String[] cached = cleanTitleCache;
      if (cached != null && cached[0].equals(title)) return cached[1];
      String cleaned = title.replaceAll("^\\(\\d+\\)\\s*", "").replaceAll("^\\[\\d+\\]\\s*", "").trim();
      String result = cleaned.isBlank() ? "Unknown" : cleaned;
      cleanTitleCache = new String[]{title, result};
      return result;
   }

   private static Font getFittedFont(String text, float maxWidth) {
      float baseSize = 6.2F;
      Font font = Fonts.SEMIBOLD.getFont(baseSize);
      float textW = font.getFont().getWidth(text, font.getSize());
      if (textW > maxWidth && textW > 0.0F) {
         float scale = Math.clamp(maxWidth / textW, 0.70F, 1.0F);
         return Fonts.SEMIBOLD.getFont(Math.max(4.5F, baseSize * scale));
      }
      return font;
   }

   private static String ellipsize(Font font, String text, float maxWidth) {
      if (text == null || text.isBlank() || maxWidth <= 0.0F) {
         return "";
      }
      String value = text.trim();
      if (font.width(value) <= maxWidth) {
         return value;
      }

      String suffix = "...";
      if (font.width(suffix) > maxWidth) {
         return "";
      }

      int low = 0;
      int high = value.length();
      while (low < high) {
         int middle = (low + high + 1) >>> 1;
         if (font.width(value.substring(0, middle).stripTrailing() + suffix) <= maxWidth) {
            low = middle;
         } else {
            high = middle - 1;
         }
      }
      return value.substring(0, low).stripTrailing() + suffix;
   }

   /** String.format results per displayed second; dropped if the default format locale changes. */
   private static final java.util.Map<Long, String> TIME_TEXT = new java.util.HashMap<>();
   private static java.util.Locale timeTextLocale;
   private static String formatTime(long seconds) {
      java.util.Locale locale = java.util.Locale.getDefault(java.util.Locale.Category.FORMAT);
      if (!locale.equals(timeTextLocale) || TIME_TEXT.size() > 512) {
         timeTextLocale = locale;
         TIME_TEXT.clear();
      }
      String text = TIME_TEXT.get(seconds);
      if (text == null) {
         text = String.format("%d:%02d", seconds / 60L, seconds % 60L);
         TIME_TEXT.put(seconds, text);
      }
      return text;
   }

   @Override
   public boolean show() {
      return tracker().haveActiveSession() || mc.currentScreen instanceof ChatScreen || this.alwaysDisplay.isEnabled();
   }
}
