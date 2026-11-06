package dev.bkclient.ui.hud.impl.island.impl;

import java.util.List;

import dev.bkclient.BKClientInit;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.impl.win.WindowsMediaSession;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.window.MouseScrollEvent;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.hud.impl.island.DynamicDock;
import dev.bkclient.ui.hud.impl.island.ExtandableStatus;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.math.MathUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.render.obj.Rect;
import dev.bkclient.utility.sounds.MusicTracker;
import dev.bkclient.utility.sounds.SyncedLyrics;
import net.minecraft.util.Identifier;

public class MusicStatus extends ExtandableStatus implements IMinecraft {
   private final Animation[] waveAnims = new Animation[4];
   private final Animation pausingAnim = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation hoverPrevious = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation hoverPause = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation hoverNext = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation hoverLyrics = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private boolean showLyrics = false;
   private int lyricsOffset = 0;
   private String compactText = "";
   private String previousCompactText = "";
   private String compactTrack = "";
   private long compactChangedAt;
   private final EventListener<MouseScrollEvent> onMouseScroll = event -> {
      if (this.showLyrics) {
         DynamicDock island = BKClientInit.getInstance().getHud().getIsland();
         if (island.active() == this && island.isExtended()) {
            if (event.getVerticalAmount() < 0.0) {
               this.lyricsOffset++;
            } else {
               if (!(event.getVerticalAmount() > 0.0)) {
                  return;
               }

               this.lyricsOffset--;
            }

            SyncedLyrics synced = BKClientInit.getInstance().getMusicTracker().getSyncedLyrics();
            if (synced != null && !synced.isEmpty()) {
               int lineCount = synced.getLines().size();
               int maxOffset = Math.max(0, lineCount - 6);
               this.lyricsOffset = Math.min(Math.max(0, this.lyricsOffset), maxOffset);
            }
         }
      }
   };

   public MusicStatus(SelectSetting setting) {
      super(setting, "music");

      for (int i = 0; i < this.waveAnims.length; i++) {
         this.waveAnims[i] = new Animation(400L, 0.0F, Easing.LINEAR);
      }

      BKClientInit.getInstance().getEventManager().subscribe(this);
   }

   @Override
   public void draw(CustomDrawContext context) {
      DynamicDock island = BKClientInit.getInstance().getHud().getIsland();
      float x = sr.getScaledWidth() / 2.0F - island.getSize().width / 2.0F;
      float y = 0.0F;
      MusicTracker tracker = BKClientInit.getInstance().getMusicTracker();
      ColorRGBA textColor = ColorRGBA.WHITE;
      var session = tracker.getSession();
      if (session != null) {
         MediaInfo media = session.getMedia();
         if (media == null) return;
         SyncedLyrics synced = tracker.getSyncedLyrics();
         long positionMs = tracker.getSmoothPositionMs();
         if (!island.isMusicLyricsEnabled()) {
            this.showLyrics = false;
            this.previousCompactText = "";
         }
         int lyricIndex = island.isMusicLyricsEnabled() && synced.isSynced() ? synced.getActiveIndex(positionMs) : -1;
         boolean hasActiveLyric = lyricIndex >= 0 && synced.isLineActive(lyricIndex, positionMs);
         String line = hasActiveLyric ? synced.getLines().get(lyricIndex).text() : media.getTitle();
         if (line == null || line.isBlank()) line = media.getArtist() == null ? "Music" : media.getArtist();
         String track = tracker.getLastTrack();
         if (!track.equals(this.compactTrack)) {
            this.compactTrack = track;
            this.compactText = "";
            this.previousCompactText = "";
         }
         if (!line.equals(this.compactText)) {
            this.previousCompactText = island.isMusicLyricsEnabled() ? this.compactText : "";
            this.compactText = line;
            this.compactChangedAt = System.nanoTime();
         }
         float expWidth = 164.0F;
         float expHeight = this.showLyrics ? 125.0F : 80.0F;
         float maxWidth = Math.min(170.0F, sr.getScaledWidth() - 24.0F);
         float defaultWidth = Math.max(100.0F, 36.0F + Fonts.MEDIUM.getFont(7.0F).width(this.compactText));
         float width = this.size.width = island.isExtended() ? expWidth : Math.min(defaultWidth, maxWidth);
         float height = this.size.height = island.isExtended() ? expHeight : 15.0F;
         float extending = island.getExtendingAnim().getValue();
         float imageMargin = 4.0F + 6.0F * extending;
         float imageSize = 7.0F + 19.0F * extending;
         Identifier cachedImage = tracker.getImage();
         Identifier trackImage = cachedImage != null ? cachedImage : BKClientInit.id("icons/music/no_image.png");
         float imageY = y + (island.isExtended() ? imageMargin : GuiUtility.getMiddleOfBox(imageSize, island.getSize().height));
         context.drawRoundedTexture(
            trackImage, x + imageMargin - 10.0F + 10.0F * this.animation.getValue(), imageY, imageSize, imageSize, BorderRadius.all(1.0F + 5.0F * extending)
         );
         if (extending < 1.0F) {
            float lyricX = x + 15.0F;
            float lyricWidth = Math.max(0.0F, island.getSize().width - 34.0F);
            float elapsed = (System.nanoTime() - this.compactChangedAt) / 1_000_000_000.0F;
            float transition = Math.min(1.0F, elapsed / 0.32F);
            float visibility = (1.0F - extending) * this.animation.getValue();
            ScissorUtility.push(context.getMatrices(), lyricX, y + 2.0F, lyricWidth, 11.0F);
            try {
               if (transition < 1.0F && !this.previousCompactText.isEmpty()) {
                  context.drawText(Fonts.MEDIUM.getFont(7.0F), this.previousCompactText,
                     lyricX, y + 4.5F - transition * 7.0F,
                     textColor.withAlpha(255.0F * visibility * (1.0F - transition)));
               }
               float textWidth = Fonts.MEDIUM.getFont(7.0F).width(this.compactText);
               float lineAge = hasActiveLyric ? Math.max(0L, positionMs - synced.getLines().get(lyricIndex).timeMs()) / 1000.0F : 0.0F;
               float scroll = hasActiveLyric ? Math.min(Math.max(0.0F, textWidth - lyricWidth), Math.max(0.0F, lineAge - 0.8F) * 24.0F) : 0.0F;
               float wordX = lyricX - scroll;
               String[] words = this.compactText.split("\\s+");
               for (int i = 0; i < words.length; i++) {
                  float progress = Math.max(0.0F, Math.min(1.0F, (elapsed - Math.min(i, 10) * 0.025F) / 0.30F));
                  float eased = 1.0F - (float)Math.pow(1.0F - progress, 3);
                  context.drawText(Fonts.MEDIUM.getFont(7.0F), words[i], wordX,
                     y + 4.5F + (1.0F - eased) * 6.0F,
                     textColor.withAlpha(255.0F * visibility * eased));
                  wordX += Fonts.MEDIUM.getFont(7.0F).width(words[i] + " ");
               }
            } finally {
               ScissorUtility.pop();
            }
         }
          if (extending > 0.0F) {
             context.drawFadeoutText(
                Fonts.MEDIUM.getFont(7.0F),
                media.getTitle(),
                x - 5.0F + 10.0F * this.animation.getValue() + 10.0F * this.animation.getValue() + 29.0F * extending,
                y + 5.0F + 11.0F * extending,
                textColor,
                0.3F,
                0.7F,
                island.isExtended() ? expWidth - 30.0F : maxWidth + 5.0F
             );
          }
         if (extending != 0.0F) {
            context.drawFadeoutText(
               Fonts.REGULAR.getFont(7.0F),
               media.getArtist(),
               x + 20.0F + 24.0F * extending,
               y + 5.0F + 19.0F * extending,
               textColor.withAlpha(178.5F * extending),
               0.3F,
               0.7F,
               island.isExtended() ? expWidth - 30.0F : maxWidth + 5.0F
            );
            context.drawText(
               Fonts.REGULAR.getFont(5.0F),
               formatTime(media.getPosition()),
               sr.getScaledWidth() / 2.0F - expWidth / 2.0F + 11.0F * extending,
               y + 43.0F * extending,
               textColor.withAlpha(255.0F)
            );
            context.drawText(
               Fonts.REGULAR.getFont(5.0F),
               formatTime(media.getDuration()),
               sr.getScaledWidth() / 2.0F + expWidth / 2.0F - (9.5F + Fonts.REGULAR.getFont(5.0F).width(formatTime(media.getDuration())) * extending),
               y + 43.0F * extending,
               textColor.withAlpha(255.0F)
            );
            float barWidth = 116.0F;
            float barX = sr.getScaledWidth() / 2.0F - barWidth / 2.0F;
            context.drawRoundedRect(
               barX, y + expHeight - (this.showLyrics ? 45 : 0) - 36.5F * extending, barWidth, 3.0F, BorderRadius.all(0.5F), textColor.withAlpha(63.75F)
            );
            float progressWidth = barWidth * (media.getDuration() > 0L ? Math.min(1.0F, (float)media.getPosition() / (float)media.getDuration()) : 0.0F);
            context.drawRoundedRect(
               barX, y + expHeight - (this.showLyrics ? 45 : 0) - 36.5F * extending, progressWidth, 3.0F, BorderRadius.all(0.5F), textColor.withAlpha(150.0F)
            );
            this.pausingAnim.setDuration(600L);
            this.pausingAnim.update(media.isPlaying() ? 1.0F : 0.0F);
            if (extending > 0.7F) {
               float controlY = y + expHeight - 25.0F * extending;
               double mouseX = GuiUtility.getMouse().getX();
               double mouseY = GuiUtility.getMouse().getY();
               Rect previous = new Rect(sr.getScaledWidth() / 2.0F - 40.0F, controlY, 16.0F, 16.0F);
               Rect pause = new Rect(sr.getScaledWidth() / 2.0F - 8.0F, controlY, 16.0F, 16.0F);
               Rect next = new Rect(sr.getScaledWidth() / 2.0F + 24.0F, controlY, 16.0F, 16.0F);
               if (previous.hovered(mouseX, mouseY) || pause.hovered(mouseX, mouseY) || next.hovered(mouseX, mouseY)) {
                  CursorUtility.set(CursorType.HAND);
               }

               this.hoverPrevious.update(previous.hovered(mouseX, mouseY));
               this.hoverPause.update(pause.hovered(mouseX, mouseY));
               this.hoverNext.update(next.hovered(mouseX, mouseY));
               context.drawTexture(BKClientInit.id("icons/music/previous.png"), previous, textColor.withAlpha(255.0F - 100.0F * this.hoverPrevious.getValue()));
               float anim = this.pausingAnim.getValue();
               float centerX = pause.getX() + pause.getWidth() / 2.0F;
               float centerY = pause.getY() + pause.getHeight() / 2.0F;
               RenderUtility.rotate(context.getMatrices(), centerX, centerY, 90.0F * anim);
               RenderUtility.scale(context.getMatrices(), centerX, centerY, 1.0F - anim);
               context.drawTexture(
                  BKClientInit.id("icons/music/play.png"), pause, textColor.withAlpha(255.0F * (1.0F - anim) - 100.0F * this.hoverPause.getValue())
               );
               RenderUtility.end(context.getMatrices());
               RenderUtility.end(context.getMatrices());
               RenderUtility.rotate(context.getMatrices(), centerX, centerY, -90.0F + 90.0F * anim);
               RenderUtility.scale(context.getMatrices(), centerX, centerY, anim);
               context.drawTexture(BKClientInit.id("icons/music/pause.png"), pause, textColor.withAlpha(255.0F * anim - 100.0F * this.hoverPause.getValue()));
               RenderUtility.end(context.getMatrices());
               RenderUtility.end(context.getMatrices());
               context.drawTexture(BKClientInit.id("icons/music/next.png"), next, textColor.withAlpha(255.0F - 100.0F * this.hoverNext.getValue()));
            }

            String owner = null;
            String sessionOwner = session.getOwner() == null ? "" : session.getOwner().toLowerCase(java.util.Locale.ROOT);
            if (sessionOwner.contains("yandex") || sessionOwner.contains("яндекс")) {
               owner = "yandex_music";
            } else if (sessionOwner.contains("spotify")) {
               owner = "spotify";
            } else if (sessionOwner.contains("soundcloud")) {
               owner = "soundcloud";
            } else if (sessionOwner.contains("youtube")) {
               owner = "youtube";
            } else if (sessionOwner.contains("telegram")) {
               owner = "telegram";
            } else if (sessionOwner.contains("edge")) {
               owner = "edge";
            }

            if (owner != null) {
               context.drawTextureSmooth(BKClientInit.id("icons/media/" + owner + ".png"), x + expWidth - 24.0F, y + expHeight - 23.0F, 12.0F, 12.0F, ColorRGBA.WHITE);
            }

            switch (WindowsMediaSession.getCycle()) {
               case 0:
                  context.drawTexture(BKClientInit.id("icons/music/repeat.png"), x + 14.0F, y + expHeight - 21.0F, 8.0F, 8.0F, textColor.withAlpha(150.0F));
                  break;
               case 1:
                  context.drawTexture(BKClientInit.id("icons/music/repeat.png"), x + 14.0F, y + expHeight - 21.0F, 8.0F, 8.0F, textColor);
                  break;
               case 2:
                  context.drawTexture(BKClientInit.id("icons/music/repeat1.png"), x + 14.0F, y + expHeight - 21.0F, 8.0F, 8.0F, textColor);
            }

            if (!this.hasLyrics(tracker)) {
               this.showLyrics = false;
            }
            if (this.hasLyrics(tracker)) {
               Rect lyricsRect = lyricsButton(x, y, expWidth, expHeight);
               boolean hovered = GuiUtility.isHovered(lyricsRect.getX(), lyricsRect.getY(), lyricsRect.getWidth(), lyricsRect.getHeight(),
                  GuiUtility.getMouse().getX(), GuiUtility.getMouse().getY());
               this.hoverLyrics.update(hovered);
               if (hovered) CursorUtility.set(CursorType.HAND);
               context.drawText(Fonts.MEDIUM.getFont(6.0F), "TXT", lyricsRect.getX(), lyricsRect.getY() + 1.0F,
                  textColor.withAlpha(this.showLyrics ? 255.0F : 150.0F));
            }

            // Plynny, zsynchronizowany tekst piosenki (dynamic island):
            // pokazuj aktualna linijke na podstawie pozycji utworu.
            if (this.showLyrics && this.hasLyrics(tracker)) {
               List<SyncedLyrics.LyricLine> lyricLines = tracker.getSyncedLyrics().getLines();
               int activeIndex = synced.isSynced() ? synced.getActiveIndex(positionMs) : -1;
               int from = Math.max(0, Math.min(synced.isSynced() ? activeIndex : this.lyricsOffset, Math.max(0, lyricLines.size() - 6)));
               int maxLines = Math.min(6, lyricLines.size());
               for (int i = 0; i < maxLines && from + i < lyricLines.size(); i++) {
                  int idx = from + i;
                  boolean isCurrent = idx == activeIndex;
                  context.drawFadeoutText(
                     Fonts.REGULAR.getFont(isCurrent ? 6.4F : 6.0F),
                     lyricLines.get(idx).text(),
                     x + 10.0F,
                     y + 55.0F + i * 7,
                     isCurrent ? ColorRGBA.WHITE : textColor.withAlpha(150.0F * extending),
                     0.91F,
                     1.0F,
                     expWidth - 20.0F
                  );
               }
            }
         }

         for (int i = 0; i < this.waveAnims.length; i++) {
            float phase = (float)media.getPosition() * 8.0F + i * 0.7F;
            float size = media.isPlaying() ? (float)(2.0 + Math.abs(MathUtility.sin(phase)) * 8.0) : 3.0F;
            this.waveAnims[i].update(size);
            this.waveAnims[i].setDuration(1000L);
            context.drawRoundedRect(
               x
                  + MathUtility.interpolate(island.getSize().width, expWidth - 10.0F, extending)
                  - 2.0F
                  - 10.0F * this.animation.getValue()
                  + i * (2.0F + extending),
               y + MathUtility.interpolate(4.25, 14.0, extending) + (7.0F - this.waveAnims[i].getValue()) / 2.0F,
               1.0F + extending,
               this.waveAnims[i].getValue(),
               BorderRadius.all(0.5F),
               tracker.getMediaColor()
            );
         }
      }
   }

   @Override
   public void click(float mouseX, float mouseY, int button) {
      DynamicDock island = BKClientInit.getInstance().getHud().getIsland();
      float x = sr.getScaledWidth() / 2.0F - island.getSize().width / 2.0F;
      float y = 0.0F;
      float width = this.size.width;
      float height = this.size.height;
      MusicTracker tracker = BKClientInit.getInstance().getMusicTracker();
      var session = tracker.getSession();
      if (session != null) {
         if (GuiUtility.isHovered(x + width / 2.0F - 40.0F, y + height - 9.0F - 16.0F, 16.0, 16.0, mouseX, mouseY)) {
            session.previous();
         }

         if (GuiUtility.isHovered(x + width / 2.0F - 8.0F, y + height - 9.0F - 16.0F, 16.0, 16.0, mouseX, mouseY)) {
            session.playPause();
         }

         if (GuiUtility.isHovered(x + width / 2.0F + 24.0F, y + height - 9.0F - 16.0F, 16.0, 16.0, mouseX, mouseY)) {
            session.next();
         }

         if (GuiUtility.isHovered(x + 14.0F, y + height - 21.0F, 8.0, 8.0, mouseX, mouseY)) {
            session.swapCycle();
            WindowsMediaSession.setCycle(session.getCycleType());
         }

          Rect lyricsRect = lyricsButton(x, y, width, height);
          if (this.hasLyrics(tracker)
             && GuiUtility.isHovered(lyricsRect.getX(), lyricsRect.getY(), lyricsRect.getWidth(), lyricsRect.getHeight(), mouseX, mouseY)) {
             this.showLyrics = !this.showLyrics;
             if (this.showLyrics) {
                this.lyricsOffset = 0;
             }
          }
      }
   }

   @Override
   public boolean canShow() {
      var session = BKClientInit.getInstance().getMusicTracker().getSession();
      return session != null && session.getMedia() != null
         && (session.getOwner() == null || !session.getOwner().toLowerCase(java.util.Locale.ROOT).contains("gram"));
   }

   private boolean hasLyrics(MusicTracker tracker) {
      SyncedLyrics synced = tracker == null ? null : tracker.getSyncedLyrics();
      return BKClientInit.getInstance().getHud().getIsland().isMusicLyricsEnabled()
         && synced != null && !synced.isEmpty() && synced.getLines() != null && !synced.getLines().isEmpty();
   }

   private static Rect lyricsButton(float x, float y, float width, float height) {
      // Separate from the media provider badge at width - 22, and from Next.
      return new Rect(x + width - 40.0F, y + height - 21.0F, 14.0F, 8.0F);
   }

   public static String formatTime(long totalSeconds) {
      long minutes = totalSeconds / 60L;
      long seconds = totalSeconds % 60L;
      return String.format("%d:%02d", minutes, seconds);
   }

   @Override
   public ColorRGBA getColor() {
      return ColorRGBA.BLACK;
   }
}
