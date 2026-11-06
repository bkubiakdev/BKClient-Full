package dev.bkclient.ui.mainmenu;

import java.util.ArrayList;
import java.util.List;
import dev.bkclient.BKClientInit;
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
import dev.bkclient.systems.modules.modules.other.Sounds;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.ui.menu.spotlight.SpotlightScreen;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.math.MathUtility;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.sounds.ClientSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

public class CustomTitleScreen extends CustomScreen implements IMinecraft {
   private static boolean welcomePlayed;
   private final MenuPreferencesPanel menuPreferences=new MenuPreferencesPanel();
   private final ServerCarousel serverCarousel = new ServerCarousel();
   private final ServerBrowser serverBrowser = new ServerBrowser();
   private final List<CustomButton> buttons = new ArrayList<>();
   private boolean active;
   private final Animation activeAnimation = new Animation(800L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private static final float COLON_RAISE = 4.5F;
   private boolean spotlightOpen;
   private SpotlightScreen spotlight;
   private AccountManager accountManager;
   private MenuBackgroundManager backgroundManager;
   private MainMenuMusicCard musicCard;
   private ModernMenuLayout modernLayout;
   /** True once the menu has been drawn at least once, i.e. startup loading is over and its resources are known to work. */
   private static boolean menuDrawnOnce;
   private final Animation customizeHoverAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private float customizeBtnX;
   private float customizeBtnY;
   private float customizeBtnW;
   private float customizeBtnH;
   private long openedAtNanos;

   protected void init() {
      String basePath = "image/mainmenu/icons/";
      menuPreferences.close();
      serverCarousel.close();
      if(MenuPreferences.Option.SERVER_CAROUSEL.enabled) serverCarousel.preloadServers();
      this.openedAtNanos = System.nanoTime();
      if (!welcomePlayed) {
         if (BKClientInit.getInstance().getModuleManager().getModule(Sounds.class).isEnabled()) {
            ClientSounds.WELCOME.play(BKClientInit.getInstance().getModuleManager().getModule(Sounds.class).getVolume().getCurrentValue());
         }
         welcomePlayed = true;
      }

      if (this.buttons.isEmpty()) {
         buttons.add(new CustomButton(basePath + "single.png", 12.0F, this::openSingleplayer));
         buttons.add(new CustomButton(basePath + "multi.png", 12.0F, this::openMultiplayer));
         buttons.add(new CustomButton(basePath + "settings.png", 12.0F, () -> mc.setScreen(new OptionsScreen(this, mc.options))));
         buttons.add(new CustomButton(basePath + "quit.png", 14.0F, () -> mc.stop()));
         buttons.add(new CustomButton(basePath + "accounts.png", 14.0F, this::openAccounts));
      }
      if (this.modernLayout == null) {
         this.modernLayout = new ModernMenuLayout(new ModernMenuLayout.Actions(
            this::openSingleplayer,
            this::openMultiplayer,
            () -> ConnectScreen.connect(this, mc, ServerAddress.parse(LastPlayedServer.address()), LastPlayedServer.toServerInfo(), false, null),
            () -> mc.setScreen(new OptionsScreen(this, mc.options)),
            () -> { if (this.backgroundManager != null) this.backgroundManager.open(); },
            this::openAccounts,
            mc::stop
         ));
      }
      this.modernLayout.reset();
      LastPlayedServer.ensureTracking();
      LastPlayedServer.flush();

      this.accountManager = new AccountManager();
      if (this.backgroundManager == null) this.backgroundManager = new MenuBackgroundManager();
      this.musicCard = new MainMenuMusicCard();
      this.spotlight = new SpotlightScreen();
      this.spotlight.init();
      super.init();
   }

   private boolean modern() {
      return this.modernLayout != null;
   }

   private void openSingleplayer() {
      if (!MenuPreferences.Option.WORLD_CAROUSEL.enabled) mc.setScreen(new SelectWorldScreen(this));
      else if (this.modern()) serverBrowser.open(true);
      else serverCarousel.open(true);
   }

   private void openMultiplayer() {
      if (!MenuPreferences.Option.SERVER_CAROUSEL.enabled) mc.setScreen(new MultiplayerScreen(this));
      else if (this.modern()) serverBrowser.open(false);
      else serverCarousel.open();
   }

   private void openAccounts() {
      if (InGameAccountSwitcher.open(this)) return;
      if (this.accountManager != null) this.accountManager.open();
   }

   @Override
   public void tick() {
      super.tick();
      if(MenuPreferences.Option.SERVER_CAROUSEL.enabled) serverCarousel.preloadServers();
      serverCarousel.tick();
      serverBrowser.tick();
      InGameAccountSwitcher.restoreLastAccount(this);
   }

   @Override
   public void render(UIContext context) {
      // SplashOverlay renders its underlying screen while resource loading/fading is in progress.
      // At startup do not sample menu textures or custom shaders until that overlay has gone away.
      boolean loading = mc.getOverlay() instanceof net.minecraft.client.gui.screen.SplashOverlay;
      if (loading && !menuDrawnOnce) {
         context.fill(0, 0, this.width, this.height, 0xFF101116);
         this.openedAtNanos = System.nanoTime();
         return;
      }
      if (!loading) {
         this.renderMenu(context);
         return;
      }
      // A later reload (for example when a server resource pack is dropped after leaving a server): the menu has already
      // worked, so keep drawing it instead of a dark screen. If a resource is not ready for a frame, fall back to the fill.
      try {
         this.renderMenu(context);
      } catch (RuntimeException e) {
         context.fill(0, 0, this.width, this.height, 0xFF101116);
      }
   }

   private void renderMenu(UIContext context) {
      this.activeAnimation.update(this.active);
      Font timeFont = Fonts.BOLD.getFont(65.0F);
      Font dateFont = Fonts.MEDIUM.getFont(16.0F);
      Font unlockFont = Fonts.REGULAR.getFont(10.0F);
      float timeOffset = MathUtility.interpolate(this.height / 2.0F - 155.0F, -30.0F, this.activeAnimation.getValue());
      double time = (System.nanoTime() - this.openedAtNanos) / 1_000_000_000.0D;

      this.drawMenuBackground(context, time);
      menuDrawnOnce = true;
      if (MenuPreferences.Option.DIM.enabled) {
      context.drawRoundedRect(
         0.0F,
         0.0F,
         this.width,
         this.height,
         BorderRadius.ZERO,
         new VerticalGradient(new ColorRGBA(4.0F, 7.0F, 15.0F, 70.0F), new ColorRGBA(2.0F, 4.0F, 10.0F, 154.0F))
      );
      context.drawRoundedRect(
         0.0F,
         0.0F,
         this.width * 0.35F,
         this.height,
         BorderRadius.ZERO,
         new HorizontalGradient(new ColorRGBA(2.0F, 4.0F, 10.0F, 112.0F), new ColorRGBA(2.0F, 4.0F, 10.0F, 0.0F))
      );
      }
      DrawUtility.blurProgram.draw();

      if (this.modernLayout != null) {
         this.renderModern(context, time);
         this.renderOverlays(context);
         return;
      }

      float clockAlpha = 255.0F * (1.0F - this.activeAnimation.getValue());
      if (clockAlpha > 1.0F) {
         String dateText = TextUtility.getFormattedDate();
         String timeText = TextUtility.getCurrentTime();
         float clockCenterX = this.width / 2.0F;
         ColorRGBA tint = new ColorRGBA(234.0F, 241.0F, 255.0F, clockAlpha);
         ColorRGBA shadow = ColorRGBA.BLACK.withAlpha(80.0F * (1.0F - this.activeAnimation.getValue()));

         if(MenuPreferences.Option.DATE.enabled){
         context.drawCenteredText(dateFont, dateText, clockCenterX, timeOffset - 21.8F, shadow);
         context.drawCenteredText(dateFont, dateText, clockCenterX, timeOffset - 23.0F, tint.withAlpha(clockAlpha * 0.82F));
         }
         if(MenuPreferences.Option.CLOCK.enabled){
         this.drawTimeWithRaisedColon(context, timeFont, timeText, clockCenterX, timeOffset + 1.4F, shadow);
         this.drawTimeWithRaisedColon(context, timeFont, timeText, clockCenterX, timeOffset, tint);

         }
         float hintPulse = 0.74F + 0.16F * (float)Math.sin(time * 1.7D);
         context.drawCenteredText(
            unlockFont,
            copy("Kliknij, aby otworzyć menu", "Click to open menu"),
            clockCenterX,
            timeOffset + timeFont.height() + 24.0F,
            ColorRGBA.WHITE.withAlpha(clockAlpha * 0.42F * hintPulse)
         );

         if (MenuPreferences.Option.MUSIC.enabled && this.musicCard != null && this.musicCard.shouldShow()) {
            float cardY = timeOffset + timeFont.height() + 24.0F + unlockFont.height() + 16.0F;
            this.musicCard.render(context, clockCenterX, cardY, clockAlpha / 255.0F);
         }
      }

      float userAlpha = 255.0F * this.activeAnimation.getValue();
      if (MenuPreferences.Option.USERNAME.enabled && userAlpha > 1.0F && this.height > 424) {
         String userName = mc.getSession() == null ? "BK-Client" : mc.getSession().getUsername();
         float baseY = this.height - 25;
         float offset = 5.0F * (1.0F - this.activeAnimation.getValue());
         float userY = baseY - offset;
         context.drawCenteredText(Fonts.REGULAR.getFont(11.0F), userName, this.width / 2.0F, userY, ColorRGBA.WHITE.withAlpha(userAlpha));
      }

      float offset = 0.0F;
      float totalWidth = buttons.size() * 30.0F + (buttons.size() - 1) * 6.0F;
      float startX = this.width / 2.0F - totalWidth / 2.0F;
      float yPos = this.height > 460 ? this.height / 2.0F + 20.0F : this.height / 1.25F;

      for (CustomButton button : buttons) {
         button.getActiveAnim().update(!serverCarousel.isOpen() && buttons.size() - buttons.indexOf(button) > (1.0F - this.activeAnimation.getValue()) * buttons.size() + 0.5F);
         float xPos = startX + offset;
         button.set(xPos, yPos - 5.0F - 10.0F * button.getActiveAnim().getValue(), 30.0F, 30.0F);
         offset += button.getWidth() + 6.0F;
         context.getMatrices().push();
         float carouselFade=1-serverCarousel.visibility();
         context.getMatrices().translate(0,serverCarousel.visibility()*35,0);
         if(carouselFade>.02f){
            // Existing button animation handles the fade while the server strip takes its place.
            button.draw(context);
         }
         context.getMatrices().pop();
      }

      float customizeAlpha = (1-serverCarousel.visibility()) * 255.0F * (buttons.isEmpty() ? 0.0F : buttons.get(buttons.size() - 1).getActiveAnim().getValue());
      if (customizeAlpha > 1.0F) {
         Font pillFont = Fonts.MEDIUM.getFont(9.0F);
         String label = copy("Personalizacja", "Customize");
         float textW = pillFont.getFont().getWidth(label, pillFont.getSize());
         this.customizeBtnW = textW + 26.0F;
         this.customizeBtnH = 22.0F;
         this.customizeBtnX = this.width / 2.0F - this.customizeBtnW / 2.0F;
         this.customizeBtnY = yPos + 30.0F + 12.0F;
         boolean hovered = GuiUtility.isHovered(this.customizeBtnX, this.customizeBtnY, this.customizeBtnW, this.customizeBtnH, context.getMouseX(), context.getMouseY());
         this.customizeHoverAnim.update(hovered);
         if (hovered) {
            CursorUtility.set(CursorType.HAND);
         }
         float hover = this.customizeHoverAnim.getValue();
         BorderRadius pillRadius = BorderRadius.all(this.customizeBtnH / 2.0F);
         ColorRGBA pillBase = new ColorRGBA(58.0F, 58.0F, 58.0F);
         if (Interface.showGlass()) {
            context.drawLiquidGlass(
               this.customizeBtnX - 1.0F, this.customizeBtnY - 1.0F, this.customizeBtnW + 2.0F, this.customizeBtnH + 2.0F,
               7.0F, 0.08F, pillRadius, ColorRGBA.WHITE.withAlpha(customizeAlpha)
            );
            context.drawRoundedRect(
               this.customizeBtnX, this.customizeBtnY, this.customizeBtnW, this.customizeBtnH, pillRadius,
               pillBase.withAlpha(customizeAlpha * (0.15F + 0.15F * hover))
            );
         } else {
            context.drawRoundedRect(
               this.customizeBtnX, this.customizeBtnY, this.customizeBtnW, this.customizeBtnH, pillRadius,
               pillBase.withAlpha(customizeAlpha * (0.33F + 0.2F * hover))
            );
         }
         context.drawCenteredText(
            pillFont, label, this.customizeBtnX + this.customizeBtnW / 2.0F,
            this.customizeBtnY + GuiUtility.getMiddleOfBox(pillFont.height(), this.customizeBtnH), ColorRGBA.WHITE.withAlpha(customizeAlpha)
         );
      } else {
         this.customizeBtnW = 0.0F;
      }

      this.renderOverlays(context);
   }

   private void renderModern(UIContext context, double time) {
      this.customizeBtnW = 0.0F;
      float visibility = 1.0F - Math.max(serverCarousel.visibility(), serverBrowser.visibility());
      this.modernLayout.render(context, this.width, this.height, time, visibility);
      serverBrowser.draw(context, this.width, this.height, this.modernLayout.clockBottom(), this.height - 44.0F);
      float musicY = this.modernLayout.musicCardY();
      if (MenuPreferences.Option.MUSIC.enabled && musicY >= 0.0F && visibility > 0.02F && this.musicCard != null && this.musicCard.shouldShow()) {
         this.musicCard.render(context, this.width / 2.0F, musicY, visibility);
      }
   }

   /** Panels drawn above either menu layout. */
   private void renderOverlays(UIContext context) {
      menuPreferences.draw(context,this.width,this.height);
      if (this.spotlightOpen && this.spotlight != null) {
         this.spotlight.render(context);
      }

      if (this.accountManager != null) {
         this.accountManager.render(context);
      }

      if (this.backgroundManager != null) {
         this.backgroundManager.render(context);
      }
      serverCarousel.draw(context,this.width,this.height);
   }

   @Override public void removed() { serverCarousel.dispose(); serverBrowser.dispose(); super.removed(); }

   private void drawMenuBackground(UIContext context, double time) {
      MenuBackground background = MenuBackground.fromUniqueId(BKClientInit.getInstance().getSelectedMenuBackground(), MenuBackground.CINEMATIC);
      if (background.isVideo()) {
         background.getVideoPlayer().update();
         if (!background.getVideoPlayer().hasFrame()) background = MenuBackground.CINEMATIC;
      }
      float aspect = background.getAspect();
      boolean animated = MenuPreferences.Option.MOTION.enabled && background.isAnimated();
      float motion = this.activeAnimation.getValue();
      float breathe = animated ? 1.075F + 0.008F * (float)Math.sin(time * 0.28D) + 0.025F * (1.0F - motion) : 1.0F;
      float drawWidth = this.width * breathe;
      float drawHeight = drawWidth / aspect;
      if (drawHeight < this.height * breathe) {
         drawHeight = this.height * breathe;
         drawWidth = drawHeight * aspect;
      }

      float mouseX = this.width == 0 ? 0.0F : context.getMouseX() / (float)this.width - 0.5F;
      float mouseY = this.height == 0 ? 0.0F : context.getMouseY() / (float)this.height - 0.5F;
      float driftX = animated ? (float)Math.sin(time * 0.07D) * 3.0F - mouseX * 5.0F : 0.0F;
      float driftY = animated ? (float)Math.cos(time * 0.055D) * 1.8F - mouseY * 3.0F : 0.0F;
      float x = (this.width - drawWidth) / 2.0F + driftX;
      float y = (this.height - drawHeight) / 2.0F + driftY;
      context.drawTextureSmooth(background.id(), x, y, drawWidth, drawHeight, ColorRGBA.WHITE);
   }

   private void drawTimeWithRaisedColon(UIContext context, Font font, String time, float centerX, float y, ColorRGBA color) {
      int colonIdx = time.indexOf(58);
      if (colonIdx < 0) {
         context.drawCenteredText(font, time, centerX, y, color);
      } else {
         String left = time.substring(0, colonIdx);
         String colon = ":";
         String right = time.substring(colonIdx + 1);
         float leftW = font.getFont().getWidth(left, font.getSize());
         float colonW = font.getFont().getWidth(colon, font.getSize());
         float rightW = font.getFont().getWidth(right, font.getSize());
         float totalW = leftW + colonW + rightW;
         float startX = centerX - totalW / 2.0F;
         context.drawText(font, left, startX, y, color);
         context.drawText(font, colon, startX + leftW, y - COLON_RAISE, color);
         context.drawText(font, right, startX + leftW + colonW, y, color);
      }
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (serverBrowser.isOpen()) {
         // The menu's dock and account chip stay usable around the browser.
         if (this.modern() && this.modernLayout.clickChrome(mouseX, mouseY, button.getButtonIndex())) serverBrowser.close();
         else serverBrowser.click(mouseX, mouseY, button, this);
         return;
      }
      if (serverCarousel.isOpen()) { serverCarousel.click(mouseX,mouseY,button.getButtonIndex(),this); return; }
      if (this.accountManager != null && this.accountManager.isOpen()) {
         this.accountManager.onMouseClicked(mouseX, mouseY, button);
         return;
      }

      if (this.backgroundManager != null && this.backgroundManager.isOpen()) {
         this.backgroundManager.onMouseClicked(mouseX, mouseY, button);
         return;
      }

      if (menuPreferences.click(mouseX,mouseY,button.getButtonIndex())) return;
      if (this.modernLayout != null) {
         if (MenuPreferences.Option.MUSIC.enabled && this.modernLayout.musicCardY() >= 0.0F && this.musicCard != null
            && this.musicCard.shouldShow() && this.musicCard.onMouseClicked(mouseX, mouseY, button)) return;
         this.modernLayout.click(mouseX, mouseY, button.getButtonIndex());
         if (this.spotlightOpen && this.spotlight != null) this.spotlight.onMouseClicked(mouseX, mouseY, button);
         return;
      }
      if (!this.active && this.activeAnimation.getValue() == 0.0F && MenuPreferences.Option.MUSIC.enabled && this.musicCard != null && this.musicCard.shouldShow() && this.musicCard.onMouseClicked(mouseX, mouseY, button)) {
         return;
      }

      for (CustomButton customButton : buttons) {
         if (customButton.hovered(mouseX, mouseY) && customButton.getActiveAnim().getValue() == 1.0F) {
            customButton.click(mouseX, mouseY, button.getButtonIndex());
            return;
         }
      }

      if (this.customizeBtnW > 0.0F && GuiUtility.isHovered(this.customizeBtnX, this.customizeBtnY, this.customizeBtnW, this.customizeBtnH, mouseX, mouseY)) {
         if (this.backgroundManager != null) {
            this.backgroundManager.open();
         }
         return;
      }

      float animProgress = this.activeAnimation.getValue();
      if (!(animProgress > 0.0F) || !(animProgress < 1.0F)) {
         this.active = !this.active;
         super.onMouseClicked(mouseX, mouseY, button);
      }

      if (this.spotlightOpen && this.spotlight != null) {
         this.spotlight.onMouseClicked(mouseX, mouseY, button);
      }
   }

   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (serverBrowser.isOpen()) {
         serverBrowser.mouseReleased(mouseX, mouseY, button);
         return;
      }
      if (this.backgroundManager != null && this.backgroundManager.isOpen()) {
         this.backgroundManager.onMouseReleased(mouseX, mouseY, button);
         return;
      }
      if (this.accountManager != null && this.accountManager.isOpen()) {
         this.accountManager.onMouseReleased(mouseX, mouseY, button);
         return;
      }
      if (this.spotlightOpen && this.spotlight != null) {
         this.spotlight.onMouseReleased(mouseX, mouseY, button);
      }
   }

   @Override
   public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {
      if (this.backgroundManager != null && this.backgroundManager.isOpen()) {
         this.backgroundManager.onMouseDragged(mouseX, mouseY, button);
         return;
      }
      super.onMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if(menuPreferences.isOpen())return true;
      if (serverBrowser.isOpen()) { serverBrowser.scroll(verticalAmount); return true; }
      if (serverCarousel.isOpen()) {serverCarousel.scroll(verticalAmount);return true;}
      if (this.backgroundManager != null && this.backgroundManager.isOpen()) {
         return this.backgroundManager.mouseScrolled(horizontalAmount, verticalAmount);
      }
      if (this.accountManager != null && this.accountManager.isOpen()) {
         return this.accountManager.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
      if (this.spotlightOpen && this.spotlight != null) {
         return this.spotlight.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean charTyped(char chr, int modifiers) {
      if (serverBrowser.isOpen()) return serverBrowser.charTyped(chr, modifiers);
      if (this.backgroundManager != null && this.backgroundManager.isOpen()) return true;
      if (this.accountManager != null && this.accountManager.isOpen()) {
         return this.accountManager.charTyped(chr, modifiers);
      }
      if (this.spotlightOpen && this.spotlight != null) {
         return this.spotlight.charTyped(chr, modifiers);
      }
      return super.charTyped(chr, modifiers);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if(menuPreferences.isOpen()){if(keyCode==256)menuPreferences.close();return true;}
      if (serverBrowser.isOpen()) return serverBrowser.key(keyCode, scanCode, modifiers, this);
      if (serverCarousel.isOpen()) return serverCarousel.key(keyCode,this);
      if (this.backgroundManager != null && this.backgroundManager.isOpen()) {
         return this.backgroundManager.keyPressed(keyCode, scanCode, modifiers);
      }
      if (Screen.hasControlDown() && keyCode == 82) {
         MinecraftClient.getInstance().setScreen(new MultiplayerScreen(this));
      }

      if (Screen.hasControlDown() && keyCode == 84) {
         MinecraftClient.getInstance().setScreen(new SelectWorldScreen(this));
      }

      if (keyCode == 344) {
         this.spotlightOpen = !this.spotlightOpen;
         if (this.spotlightOpen && this.spotlight != null) {
            this.spotlight.setClosing(false);
         }
         return true;
      }

      if (this.accountManager != null && this.accountManager.isOpen()) {
         return this.accountManager.keyPressed(keyCode, scanCode, modifiers);
      }

      if (this.backgroundManager != null && this.backgroundManager.isOpen()) {
         return this.backgroundManager.keyPressed(keyCode, scanCode, modifiers);
      }

      if (this.spotlightOpen && this.spotlight != null) {
         return this.spotlight.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private static String copy(String polish, String english) {
      return Localizator.getCurrentLanguage() == Language.PL_PL ? polish : english;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }
}
