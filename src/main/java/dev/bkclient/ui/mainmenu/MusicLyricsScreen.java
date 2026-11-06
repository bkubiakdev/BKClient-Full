package dev.bkclient.ui.mainmenu;

import dev.bkclient.BKClientInit;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.framework.base.CustomScreen;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.framework.objects.gradient.impl.HorizontalGradient;
import dev.bkclient.framework.objects.gradient.impl.VerticalGradient;
import dev.bkclient.systems.localization.Language;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.sounds.LyricsFetcher;
import dev.bkclient.utility.sounds.MusicTracker;
import dev.bkclient.utility.sounds.SyncedLyrics;
import dev.bkclient.utility.sounds.SyncedLyrics.LyricLine;
import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Apple Music / Spotify live synchronized lyrics screen. */
public class MusicLyricsScreen extends CustomScreen implements IMinecraft {
   private final Screen parentScreen;
   private float scrollY = 0.0F;
   private float targetScrollY = 0.0F;
   private boolean userScrolled = false;
   private long lastUserScrollTime = 0L;
   private long openedAtNanos;

   private final Animation openAnim = new Animation(350L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation backHoverAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation prevHoverAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation playHoverAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation nextHoverAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   private float backBtnX, backBtnY, backBtnW, backBtnH;
   private float prevX, prevY, playX, playY, nextX, nextY, controlSize;
   private float barX, barY, barW, barH;
   private float lyricsAreaX, lyricsAreaY, lyricsAreaW, lyricsAreaH;

   public MusicLyricsScreen(Screen parentScreen) {
      this.parentScreen = parentScreen;
   }

   @Override
   protected void init() {
      this.openedAtNanos = System.nanoTime();
      this.openAnim.update(true);
      super.init();
   }

   @Override
   public void render(UIContext context) {
      this.openAnim.update(true);
      float alpha = this.openAnim.getValue();
      double time = (System.nanoTime() - this.openedAtNanos) / 1_000_000_000.0D;

      // 1. Menu background & ambient backdrop
      this.drawBackdrop(context, time);
      DrawUtility.blurProgram.draw();

      // 2. Dark ambient overlay with liquid gradient
      context.drawRoundedRect(
         0.0F, 0.0F, this.width, this.height, BorderRadius.ZERO,
         new VerticalGradient(new ColorRGBA(10.0F, 12.0F, 22.0F, 180.0F * alpha), new ColorRGBA(4.0F, 6.0F, 14.0F, 225.0F * alpha))
      );
      context.drawRoundedRect(
         0.0F, 0.0F, this.width * 0.45F, this.height, BorderRadius.ZERO,
         new HorizontalGradient(new ColorRGBA(4.0F, 7.0F, 16.0F, 120.0F * alpha), new ColorRGBA(4.0F, 7.0F, 16.0F, 0.0F))
      );

      // 3. Back navigation
      this.renderTopBar(context, alpha);

      // 4. Left side: Album Art, Track Info, Seek Bar, Media Controls
      this.renderLeftPanel(context, alpha);

      // 5. Right side: Synchronized Live Lyrics Stream
      this.renderLyricsStream(context, alpha);
   }

   private void drawBackdrop(UIContext context, double time) {
      MenuBackground background = MenuBackground.fromUniqueId(BKClientInit.getInstance().getSelectedMenuBackground(), MenuBackground.CINEMATIC);
      if (background.isVideo()) {
         background.getVideoPlayer().update();
      }
      float aspect = background.getAspect();
      float drawWidth = this.width;
      float drawHeight = drawWidth / aspect;
      if (drawHeight < this.height) {
         drawHeight = this.height;
         drawWidth = drawHeight * aspect;
      }
      float x = (this.width - drawWidth) / 2.0F;
      float y = (this.height - drawHeight) / 2.0F;
      context.drawTextureSmooth(background.id(), x, y, drawWidth, drawHeight, ColorRGBA.WHITE);
   }

   private void renderTopBar(UIContext context, float alpha) {
      Font font = Fonts.MEDIUM.getFont(9.0F);
      String label = Localizator.getCurrentLanguage() == Language.PL_PL ? "Wróć" : "Back";
      this.backBtnX = 16;
      this.backBtnY = 16;
      this.backBtnW = font.width(label) + 36;
      this.backBtnH = 26;
      boolean hovered = GuiUtility.isHovered(this.backBtnX, this.backBtnY, this.backBtnW, this.backBtnH, context.getMouseX(), context.getMouseY());
      this.backHoverAnim.update(hovered);
      if (hovered) CursorUtility.set(CursorType.HAND);
      float hover = this.backHoverAnim.getValue();
      context.drawRoundedRect(this.backBtnX, this.backBtnY, this.backBtnW, this.backBtnH,
         BorderRadius.all(7), ColorRGBA.WHITE.withAlpha((10 + 15 * hover) * alpha));
      ColorRGBA color = ColorRGBA.WHITE.withAlpha((205 + 50 * hover) * alpha);
      LucideIcons.MENU_BACK.draw(context, this.backBtnX + 7, this.backBtnY + 7, 12, color);
      context.drawText(font, label, this.backBtnX + 25,
         this.backBtnY + GuiUtility.getMiddleOfBox(font.height(), this.backBtnH), color);
   }

   private void renderLeftPanel(UIContext context, float alpha) {
      MediaInfo media = this.media();
      String rawTitle = media == null ? "Night Drive" : media.getTitle();
      String rawArtist = media == null ? "BK-Client Preview" : media.getArtist();
      LyricsFetcher.ParsedTrack parsed = LyricsFetcher.parseTrackInfo(rawArtist, rawTitle);
      String title = !parsed.title().isEmpty() ? parsed.title() : (rawTitle == null || rawTitle.isBlank() ? "Night Drive" : cleanTitle(rawTitle));
      String artist = !parsed.artist().isEmpty() ? parsed.artist() : (rawArtist == null || rawArtist.isBlank() ? "BK-Client Preview" : rawArtist);
      long position = media == null ? 74L : Math.max(0L, media.getPosition());
      long duration = media == null ? 172L : Math.max(0L, media.getDuration());
      float progress = duration <= 0L ? 0.0F : Math.min(1.0F, (float)position / duration);

      float leftCenterX = this.width * 0.26F;
      float artSize = Math.clamp(this.height * 0.38F, 110.0F, 160.0F);
      float leftPanelW = artSize + 20.0F;
      float totalHeight = artSize + 110.0F;
      float startY = Math.max(50.0F, (this.height - totalHeight) / 2.0F);

      float artX = leftCenterX - artSize / 2.0F;
      float artY = startY;

      // Album artwork with smooth rounded corners and glass container
      Identifier artwork = this.tracker().getImage();
      BorderRadius artRadius = BorderRadius.all(14.0F);
      if (artwork != null) {
         context.drawRoundedTexture(artwork, artX, artY, artSize, artSize, artRadius, ColorRGBA.WHITE.withAlpha(255.0F * alpha));
      } else {
         ColorRGBA base = new ColorRGBA(58.0F, 58.0F, 58.0F);
         if (Interface.showGlass()) {
            context.drawLiquidGlass(artX - 1.0F, artY - 1.0F, artSize + 2.0F, artSize + 2.0F, 7.0F, 0.08F, artRadius, ColorRGBA.WHITE.withAlpha(255.0F * alpha));
            context.drawRoundedRect(artX, artY, artSize, artSize, artRadius, base.withAlpha(255.0F * 0.2F * alpha));
         } else {
            context.drawRoundedRect(artX, artY, artSize, artSize, artRadius, base.withAlpha(255.0F * 0.35F * alpha));
         }
         float iconSize = artSize * 0.32F;
         context.drawTextureSmooth(
            BKClientInit.id("icons/music/no_image.png"), artX + (artSize - iconSize) / 2.0F, artY + (artSize - iconSize) / 2.0F,
            iconSize, iconSize, ColorRGBA.WHITE.withAlpha(180.0F * alpha)
         );
      }

      Font titleFont = Fonts.SEMIBOLD.getFont(12.5F);
      Font artistFont = Fonts.MEDIUM.getFont(9.0F);
      float textY = artY + artSize + 14.0F;
      context.drawFadeoutText(titleFont, title, artX, textY, ColorRGBA.WHITE.withAlpha(245.0F * alpha), 0.85F, 1.0F, artSize);
      context.drawFadeoutText(artistFont, artist, artX, textY + 16.0F, ColorRGBA.WHITE.withAlpha(150.0F * alpha), 0.85F, 1.0F, artSize);

      // Seek / Progress bar
      this.barX = artX;
      this.barW = artSize;
      this.barY = textY + 36.0F;
      this.barH = 3.0F;
      Font timeFont = Fonts.REGULAR.getFont(6.5F);
      context.drawText(timeFont, formatTime(position), this.barX, this.barY - 8.0F, ColorRGBA.WHITE.withAlpha(140.0F * alpha));
      context.drawRightText(timeFont, duration <= 0L ? "Live" : formatTime(duration), this.barX + this.barW, this.barY - 8.0F, ColorRGBA.WHITE.withAlpha(140.0F * alpha));

      boolean hoveredBar = GuiUtility.isHovered(this.barX, this.barY - 4.0F, this.barW, this.barH + 8.0F, context.getMouseX(), context.getMouseY());
      if (hoveredBar) {
         CursorUtility.set(CursorType.HAND);
      }

      context.drawRoundedRect(this.barX, this.barY, this.barW, this.barH, BorderRadius.all(1.5F), ColorRGBA.WHITE.withAlpha(35.0F * alpha));
      context.drawRoundedRect(this.barX, this.barY, this.barW * progress, this.barH, BorderRadius.all(1.5F), ColorRGBA.WHITE.withAlpha(235.0F * alpha));

      // Media controls
      this.controlSize = 13.0F;
      float controlsY = this.barY + 16.0F;
      this.prevY = controlsY;
      this.playY = controlsY;
      this.nextY = controlsY;
      float spacing = 36.0F;
      this.prevX = leftCenterX - spacing;
      this.playX = leftCenterX - this.controlSize / 2.0F;
      this.nextX = leftCenterX + spacing - this.controlSize;

      float hit = 20.0F;
      boolean prevHover = GuiUtility.isHovered(this.prevX - (hit - this.controlSize) / 2.0F, controlsY - (hit - this.controlSize) / 2.0F, hit, hit, context.getMouseX(), context.getMouseY());
      boolean playHover = GuiUtility.isHovered(this.playX - (hit - this.controlSize) / 2.0F, controlsY - (hit - this.controlSize) / 2.0F, hit, hit, context.getMouseX(), context.getMouseY());
      boolean nextHover = GuiUtility.isHovered(this.nextX - (hit - this.controlSize) / 2.0F, controlsY - (hit - this.controlSize) / 2.0F, hit, hit, context.getMouseX(), context.getMouseY());

      this.prevHoverAnim.update(prevHover);
      this.playHoverAnim.update(playHover);
      this.nextHoverAnim.update(nextHover);

      if (prevHover || playHover || nextHover) {
         CursorUtility.set(CursorType.HAND);
      }

      float prevA = 180.0F + 75.0F * this.prevHoverAnim.getValue();
      float playA = 220.0F + 35.0F * this.playHoverAnim.getValue();
      float nextA = 180.0F + 75.0F * this.nextHoverAnim.getValue();

      LucideIcons.MEDIA_PREVIOUS.draw(context, this.prevX, controlsY, this.controlSize, ColorRGBA.WHITE.withAlpha(prevA * alpha));
      LucideIcons playIcon = media == null || media.isPlaying() ? LucideIcons.MEDIA_PAUSE : LucideIcons.MEDIA_PLAY;
      playIcon.draw(context, this.playX - 1, controlsY - 1, this.controlSize + 2, ColorRGBA.WHITE.withAlpha(playA * alpha));
      LucideIcons.MEDIA_NEXT.draw(context, this.nextX, controlsY, this.controlSize, ColorRGBA.WHITE.withAlpha(nextA * alpha));
   }

   private void renderLyricsStream(UIContext context, float alpha) {
      this.lyricsAreaX = this.width * 0.50F;
      this.lyricsAreaY = 55.0F;
      this.lyricsAreaW = this.width * 0.45F;
      this.lyricsAreaH = this.height - 110.0F;

      SyncedLyrics lyrics = this.tracker().getSyncedLyrics();
      MediaInfo media = this.media();
      long positionMs = this.tracker().getSmoothPositionMs();

      if (this.tracker().isFetchingLyrics() && (lyrics == null || lyrics.isEmpty())) {
         Font loadingFont = Fonts.MEDIUM.getFont(10.0F);
         String msg = Localizator.getCurrentLanguage() == Language.PL_PL ? "Pobieranie tekstu utworu..." : "Fetching lyrics...";
         context.drawCenteredText(loadingFont, msg, this.lyricsAreaX + this.lyricsAreaW / 2.0F, this.lyricsAreaY + this.lyricsAreaH / 2.0F, ColorRGBA.WHITE.withAlpha(160.0F * alpha));
         return;
      }

      if (lyrics == null || lyrics.isEmpty()) {
         Font emptyFont = Fonts.MEDIUM.getFont(10.0F);
         String msg = Localizator.getCurrentLanguage() == Language.PL_PL ? "Brak tekstu dla tego utworu" : "No lyrics available for this song";
         context.drawCenteredText(emptyFont, msg, this.lyricsAreaX + this.lyricsAreaW / 2.0F, this.lyricsAreaY + this.lyricsAreaH / 2.0F, ColorRGBA.WHITE.withAlpha(120.0F * alpha));
         return;
      }

      List<LyricLine> lines = lyrics.getLines();
      int activeIndex = lyrics.getActiveIndex(positionMs);
      float lineHeight = 28.0F;

      // Auto-follow logic
      if (this.userScrolled && System.currentTimeMillis() - this.lastUserScrollTime > 3000L) {
         this.userScrolled = false;
      }
      if (!this.userScrolled && activeIndex >= 0) {
         this.targetScrollY = -(activeIndex * lineHeight) + (this.lyricsAreaH / 2.0F - lineHeight / 2.0F);
      }

      // Smooth scroll interpolation
      this.scrollY += (this.targetScrollY - this.scrollY) * 0.12F;

      ScissorUtility.push(context.getMatrices(), this.lyricsAreaX - 10.0F, this.lyricsAreaY, this.lyricsAreaW + 20.0F, this.lyricsAreaH);

      float centerY = this.lyricsAreaY + this.lyricsAreaH / 2.0F;
      float halfH = this.lyricsAreaH / 2.0F;

      for (int i = 0; i < lines.size(); i++) {
         LyricLine line = lines.get(i);
         float lineY = this.lyricsAreaY + this.scrollY + i * lineHeight;
         if (lineY + lineHeight < this.lyricsAreaY - 40.0F || lineY > this.lyricsAreaY + this.lyricsAreaH + 40.0F) {
            continue;
         }

         float itemCenter = lineY + lineHeight / 2.0F;
         float distCenter = Math.abs(itemCenter - centerY);
         float normDist = Math.clamp(distCenter / halfH, 0.0F, 1.0F);
         float edgeFade = Math.clamp(1.0F - normDist * normDist * 0.85F, 0.0F, 1.0F);

         boolean isActive = (i == activeIndex);
         int diffLines = Math.abs(i - activeIndex);

         boolean isHovered = GuiUtility.isHovered(this.lyricsAreaX, lineY, this.lyricsAreaW, lineHeight, context.getMouseX(), context.getMouseY());
         if (isHovered) {
            CursorUtility.set(CursorType.HAND);
         }

         if (isActive) {
            Font activeFont = Fonts.SEMIBOLD.getFont(12.0F);
            ColorRGBA activeColor = ColorRGBA.WHITE.withAlpha(255.0F * edgeFade * alpha);
            context.drawText(activeFont, line.text(), this.lyricsAreaX, lineY + (lineHeight - activeFont.height()) / 2.0F, activeColor);
         } else {
            Font inactiveFont = Fonts.MEDIUM.getFont(9.5F);
            float baseA = Math.max(0.18F, 0.65F - diffLines * 0.10F);
            if (isHovered) {
               baseA = Math.min(1.0F, baseA + 0.35F);
            }
            ColorRGBA inactiveColor = ColorRGBA.WHITE.withAlpha(255.0F * baseA * edgeFade * alpha);
            context.drawText(inactiveFont, line.text(), this.lyricsAreaX, lineY + (lineHeight - inactiveFont.height()) / 2.0F, inactiveColor);
         }
      }

      ScissorUtility.pop();


   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (button != MouseButton.LEFT) {
         return;
      }

      if (GuiUtility.isHovered(this.backBtnX, this.backBtnY, this.backBtnW, this.backBtnH, mouseX, mouseY)) {
         mc.setScreen(this.parentScreen);
         return;
      }

      // Media controls
      IMediaSession session = this.tracker().getSession();
      if (session != null) {
         float hit = 20.0F;
         if (GuiUtility.isHovered(this.prevX - (hit - this.controlSize) / 2.0F, this.prevY - (hit - this.controlSize) / 2.0F, hit, hit, mouseX, mouseY)) {
            session.previous();
            return;
         } else if (GuiUtility.isHovered(this.playX - (hit - this.controlSize) / 2.0F, this.playY - (hit - this.controlSize) / 2.0F, hit, hit, mouseX, mouseY)) {
            session.playPause();
            return;
         } else if (GuiUtility.isHovered(this.nextX - (hit - this.controlSize) / 2.0F, this.nextY - (hit - this.controlSize) / 2.0F, hit, hit, mouseX, mouseY)) {
            session.next();
            return;
         }
      }

      // Clicking on a lyric line centers it
      SyncedLyrics lyrics = this.tracker().getSyncedLyrics();
      if (lyrics != null && !lyrics.isEmpty() && GuiUtility.isHovered(this.lyricsAreaX, this.lyricsAreaY, this.lyricsAreaW, this.lyricsAreaH, mouseX, mouseY)) {
         float lineHeight = 28.0F;
         int clickedIndex = (int)((mouseY - this.lyricsAreaY - this.scrollY) / lineHeight);
         if (clickedIndex >= 0 && clickedIndex < lyrics.getLines().size()) {
            this.targetScrollY = -(clickedIndex * lineHeight) + (this.lyricsAreaH / 2.0F - lineHeight / 2.0F);
            this.userScrolled = true;
            this.lastUserScrollTime = System.currentTimeMillis();
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (GuiUtility.isHovered(this.lyricsAreaX, this.lyricsAreaY, this.lyricsAreaW, this.lyricsAreaH, mouseX, mouseY)) {
         this.targetScrollY += (float)verticalAmount * 36.0F;
         this.userScrolled = true;
         this.lastUserScrollTime = System.currentTimeMillis();
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == 256) {
         mc.setScreen(this.parentScreen);
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
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
