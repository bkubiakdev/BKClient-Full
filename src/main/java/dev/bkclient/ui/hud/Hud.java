package dev.bkclient.ui.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.utility.render.CustomRenderTarget;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.ChatRenderEvent;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.event.impl.window.ChatClickEvent;
import dev.bkclient.systems.event.impl.window.ChatKeyPressEvent;
import dev.bkclient.systems.event.impl.window.ChatReleaseEvent;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.systems.event.impl.window.MouseEvent;
import dev.bkclient.systems.event.impl.window.MouseScrollEvent;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.visuals.ESP;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.ui.components.animated.AnimatedText;
import dev.bkclient.ui.components.popup.Popup;
import dev.bkclient.ui.hud.impl.Effects;
// import dev.bkclient.ui.hud.impl.RadarHud; // Temporarily removed from the HUD catalog.
import dev.bkclient.ui.hud.impl.CustomChat;
import dev.bkclient.ui.hud.impl.KeyBinds;
import dev.bkclient.ui.hud.impl.TargetHud;
import dev.bkclient.ui.hud.impl.ArmorHud;
import dev.bkclient.ui.hud.impl.InventoryHud;
import dev.bkclient.ui.hud.impl.AutoMarketHud;
import dev.bkclient.ui.hud.impl.Cooldowns;
import dev.bkclient.ui.hud.impl.FriendsElement;
import dev.bkclient.ui.hud.impl.MusicPlayer;
import dev.bkclient.ui.hud.impl.ShowLoreElement;
import dev.bkclient.ui.hud.impl.island.DynamicDock;
import dev.bkclient.ui.hud.inline.impl.PlayerElement;
import dev.bkclient.ui.hud.inline.impl.WorldElement;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.time.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;

public class Hud implements IMinecraft, IScaledResolution {
   private final List<HudElement> elements = new ArrayList<>();
   private final List<Popup> popups = new ArrayList<>();
   private Popup playerPopup;
   private final EventListener<dev.bkclient.systems.event.impl.game.GameTickEvent> onPlayerPopupTick = event -> {
      if (playerPopup == null) return;
      var esp = BKClientInit.getInstance().getModuleManager().getModule(ESP.class);
      if (!(mc.currentScreen instanceof ChatScreen) || mc.world == null || esp == null || !esp.isEnabled()) {
         popups.remove(playerPopup);
         playerPopup = null;
      } else if (!popups.contains(playerPopup)) playerPopup = null;
   };

   /** The menu is drawn above chat, so it must receive input before chat as well. */
   public boolean clickPlayerPopup(double x, double y, int button) {
      if (playerPopup == null || !playerPopup.isShowing() || !playerPopup.isHovered(x, y)) return false;
      playerPopup.onMouseClicked(x, y, MouseButton.fromButtonIndex(button));
      return true;
   }
   public DynamicDock island;
   private final HudHistoryManager historyManager = new HudHistoryManager();
   private final Grid grid = new Grid();
   private String desc = "";
   /** Chat (HUD editor) input: the HUD renders live until this time after mouse movement or any input. */
   private static final long CHAT_LIVE_MS = 400L;
   private long chatLiveUntil;
   private double chatMouseX = Double.NaN, chatMouseY = Double.NaN;
   /** {@link #desc} as set by the cached elements during the last cache refresh. */
   private String cacheDesc = "";
   private AnimatedText descText;
   private final Timer timer = new Timer();
   private static final long CACHE_INTERVAL_MS = 16L;
   /** BK_HUD_CACHE=0 keeps the live path for every frame (comparison and diagnostics). */
   private static final boolean CACHE_ENABLED = !"0".equals(System.getenv("BK_HUD_CACHE"));
   private static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
   /** An element that rebound the framebuffer while rendering stays live until this time, then retries the cache. */
   private static final long LIVE_RETRY_MS = 2000L;
   private final CustomRenderTarget cache = new CustomRenderTarget(true);
   private final Timer cacheTimer = new Timer();
   private boolean cacheValid;
   private final Map<HudElement, Long> liveUntil = new IdentityHashMap<>();
   private final Map<HudElement, Boolean> liveReported = new IdentityHashMap<>();
   private boolean cacheUnavailableReported;

   /** The mod's draw code resets the blend function per element (alpha ONE, ZERO), which leaves the cache's
    *  alpha channel equal to the last primitive. Exordium's GlStateManager hook forces alpha-accumulating
    *  blending while set; the cache uses the same hook and stays off when it is absent. */
   private static final class BlendForce {
      static final MethodHandle IS, SET, CORRECT;
      static {
         MethodHandle is = null, set = null, correct = null;
         try {
            Class<?> base = Class.forName("dev.tr7zw.exordium.ExordiumModBase");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            is = lookup.findStatic(base, "isForceBlend", MethodType.methodType(boolean.class));
            set = lookup.findStatic(base, "setForceBlend", MethodType.methodType(void.class, boolean.class));
            correct = lookup.findStatic(base, "correctBlendMode", MethodType.methodType(void.class));
         } catch (Throwable ignored) {
            is = null; set = null; correct = null;
         }
         IS = is; SET = set; CORRECT = correct;
      }
      static boolean available() { return SET != null; }
      static boolean forced() {
         try { return (boolean) IS.invoke(); } catch (Throwable t) { return false; }
      }
      /** Returns whether the hook accepted the state. */
      static boolean force(boolean on) {
         try { SET.invoke(on); if (on) CORRECT.invoke(); return true; } catch (Throwable t) { return false; }
      }
   }

   /** True while this HUD captures its cache with the forced (alpha accumulating) blending above. */
   private static volatile boolean captureBlendForced;

   public static boolean captureBlendForced() {
      return captureBlendForced;
   }

   /** Draws an item with its own blending. The forced blending replaces every blend function, so the
    *  additive enchantment glint became an opaque layer over the item (purple icons, only in the cache).
    *  Item layers accumulate alpha by themselves and the glint keeps the target alpha. */
   public static void drawItemUnforced(Runnable draw) {
      if (!captureBlendForced) { draw.run(); return; }
      BlendForce.force(false);
      try { draw.run(); } finally { BlendForce.force(true); }
   }

   /** Icon atlases and captures return to "the main framebuffer" through mc.getFramebuffer(); while the
    *  cache is captured, that field points at the cache, so those elements stay inside it and keep their
    *  draw order. Found by type so the overlay works in every mapping namespace. */
   private static final java.lang.reflect.Field MAIN_FRAMEBUFFER = findMainFramebufferField();
   private static java.lang.reflect.Field findMainFramebufferField() {
      try {
         for (java.lang.reflect.Field field : MinecraftClient.class.getDeclaredFields()) {
            if (field.getType() == net.minecraft.client.gl.Framebuffer.class
                  && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
               field.setAccessible(true);
               return field;
            }
         }
      } catch (Throwable ignored) {}
      return null;
   }
   /** Returns the previous value, or null when the field could not be redirected. */
   private Object redirectMainFramebuffer(Object target) {
      if (MAIN_FRAMEBUFFER == null) return null;
      try {
         Object previous = MAIN_FRAMEBUFFER.get(mc);
         if (previous == null) return null;
         MAIN_FRAMEBUFFER.set(mc, target);
         return previous;
      } catch (Throwable t) { return null; }
   }
   private void restoreMainFramebuffer(Object previous) {
      if (previous == null) return;
      try { MAIN_FRAMEBUFFER.set(mc, previous); } catch (Throwable ignored) {}
   }

   private boolean cacheUsable() {
      if (!CACHE_ENABLED) return false;
      if (BlendForce.available()) return true;
      if (!this.cacheUnavailableReported) {
         this.cacheUnavailableReported = true;
         BKClientInit.LOGGER.info("HUD cache: no blend-forcing hook (Exordium) in this game, elements render live");
      }
      return false;
   }

   /** Called on the render thread when the client stops. */
   private void releaseCache() {
      // Invalidate first: a failed deletion must never leave an old frame reusable on resume.
      this.cacheValid = false;
      this.cache.delete();
   }


   private void renderElement(HudElement element, UIContext context) {
      element.updateSelectionSweep();
      HudVisualScope.begin(element);
      try { element.render(context); }
      finally { context.draw(); HudVisualScope.end(); }
   }

   private void renderElements(UIContext context) {
      for (HudElement element : this.elements) this.renderElement(element, context);
   }

   /** Renders the cacheable elements into the bound cache target. An element that leaves the target
    *  (item icon atlas, captures, own render targets rebind the main framebuffer) is kept live for
    *  LIVE_RETRY_MS and the cache is rebound for the next element. */
   private void renderCacheable(UIContext context, long now) {
      for (HudElement element : this.elements) {
         Long until = this.liveUntil.get(element);
         if (until != null) {
            if (until > now) continue;
            this.liveUntil.remove(element);
         }
         this.renderElement(element, context);
         if (GlStateManager._getInteger(GL_DRAW_FRAMEBUFFER_BINDING) != this.cache.fbo) {
            this.liveUntil.put(element, now + LIVE_RETRY_MS);
            if (this.liveReported.put(element, Boolean.TRUE) == null) {
               BKClientInit.LOGGER.info("HUD cache: element {} rebinds the framebuffer while rendering, drawn live", element.getName());
            }
            this.cache.beginWrite(false);
         }
      }
   }

   private void renderLive(UIContext context, long now) {
      for (HudElement element : this.elements) {
         Long until = this.liveUntil.get(element);
         if (until != null && until > now) this.renderElement(element, context);
      }
   }

   /** Returns whether the cache holds a complete frame of the cacheable elements. */
   private boolean refreshCache(UIContext context, long now) {
      boolean rendered = false, forced = false, previous = false;
      Object mainTarget = null;
      context.draw();
      try {
         this.cache.setFixedSize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
         this.cache.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                  // glClear honours the depth/colour masks and the scissor box: reset them so the depth
         // buffer really is cleared (3D previews depend on it) before the target is bound.
         boolean depthMask = GlStateManager._getInteger(0x0B72) != 0; // GL_DEPTH_WRITEMASK
         RenderSystem.disableScissor();
         RenderSystem.colorMask(true, true, true, true);
         RenderSystem.depthMask(true);
         this.cache.setup(true);
         RenderSystem.depthMask(depthMask);
         rendered = true;
         mainTarget = this.redirectMainFramebuffer(this.cache);
         previous = BlendForce.forced();
         forced = BlendForce.force(true);
         captureBlendForced = forced && !previous;
         RenderSystem.enableBlend();
         this.renderCacheable(context, now);
      } finally {
         if (rendered) context.draw();
         captureBlendForced = false;
         if (forced && !previous) BlendForce.force(false);
         // The real target must be back before stop() rebinds it.
         this.restoreMainFramebuffer(mainTarget);
         if (rendered) this.cache.stop();
      }
      return rendered && forced;
   }

   private void drawCache(UIContext context) {
      context.draw();
      RenderSystem.enableBlend();
      // The cache holds premultiplied colour with accumulated alpha (forced blending above).
      RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
         GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, this.cache.getColorAttachment());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
      float width = sr.getScaledWidth(), height = sr.getScaledHeight();
      BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix, 0.0F, 0.0F, 0.0F).texture(0.0F, 1.0F).color(-1);
      builder.vertex(matrix, 0.0F, height, 0.0F).texture(0.0F, 0.0F).color(-1);
      builder.vertex(matrix, width, height, 0.0F).texture(1.0F, 0.0F).color(-1);
      builder.vertex(matrix, width, 0.0F, 0.0F).texture(1.0F, 1.0F).color(-1);
      BufferRenderer.drawWithGlobalProgram(builder.end());
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.defaultBlendFunc();
   }
   private final EventListener<HudRenderEvent> onHud = event -> {
      UIContext context = UIContext.of(
         event.getContext(),
         mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().getX(),
         mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().getY(),
         MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false)
      );
      if (this.descText == null) {
         this.descText = new AnimatedText(Fonts.REGULAR.getFont(10.0F), 10.0F, 300L, Easing.BAKEK).centered();
      }

      this.desc = "";
      boolean dragging = false;
      for (HudElement element : this.elements) {
         if (element.isDragging()) {
            dragging = true;
            break;
         }
      }
      if (dragging) {
         this.grid.draw(context);
         this.grid.update();
      } else {
         this.grid.clear();
      }

      // HUD frame cache: with no screen, drag or popup, the elements are
      // rendered into an own framebuffer at most every CACHE_INTERVAL_MS and blitted in between.
      // Animations keep their timing (they run on wall-clock time); input paths stay live.
      // Under the click GUI the HUD is static (no hover, drag or popups there), so it stays cached.
      // ClickSysScreen is the menu wrapper around the same ClickGuiScreen and counts as the click GUI.
      // Inventories and containers are the same: HUD hover, drag and popups only exist in ChatScreen.
      // In chat (the HUD editor) hover, drag and popups are live: the HUD renders live while the mouse
      // moves or any input arrives and for CHAT_LIVE_MS after it; with a still mouse the hover state
      // cannot change, so the cache (refreshed every CACHE_INTERVAL_MS) shows the same picture.
      boolean chatIdle = false;
      if (mc.currentScreen instanceof ChatScreen) {
         double mouseX = GuiUtility.getMouse().getX(), mouseY = GuiUtility.getMouse().getY();
         if (mouseX != this.chatMouseX || mouseY != this.chatMouseY) {
            this.chatMouseX = mouseX;
            this.chatMouseY = mouseY;
            this.markChatInput();
         }
         chatIdle = System.currentTimeMillis() >= this.chatLiveUntil;
      } else {
         this.chatMouseX = this.chatMouseY = Double.NaN;
      }
      boolean screenLive = mc.currentScreen != null && !chatIdle
         && !(mc.currentScreen instanceof dev.bkclient.ui.clicksys.port.gui.screen.ClickGuiScreen)
         && !(mc.currentScreen instanceof dev.bkclient.ui.clicksys.ClickSysScreen)
         && !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?>);
      boolean live = !this.cacheUsable() || dragging || screenLive || !this.popups.isEmpty()
         || mc.getWindow().getFramebufferWidth() <= 0 || mc.getWindow().getFramebufferHeight() <= 0;
      if (live) {
         this.cacheValid = false;
         this.renderElements(context);
      } else {
         long now = System.currentTimeMillis();
         if (!this.cacheValid || this.cacheTimer.finished(CACHE_INTERVAL_MS)) {
            this.cacheValid = this.refreshCache(context, now);
            this.cacheTimer.reset();
            this.cacheDesc = this.desc;
         } else {
            // Hover descriptions (StaffList under a screen) are set while an element renders; between
            // refreshes the cached elements do not render, so keep what the last refresh set.
            this.desc = this.cacheDesc;
         }
         if (this.cacheValid) {
            this.drawCache(context);
            this.renderLive(context, now);
         } else {
            this.renderElements(context);
         }
      }

      this.descText.pos(sr.getScaledWidth() / 2.0F, 30.0F);
      if (!this.desc.isEmpty() && !this.desc.contains(".description")) {
         this.descText.update(this.desc);
         this.descText.render(context);
      }

      if (!(mc.currentScreen instanceof ChatScreen)) {
         CursorUtility.set(CursorType.DEFAULT);
      }

      if (mc.currentScreen == null) {
         context.getMatrices().push();
         context.getMatrices().translate(0.0F, 0.0F, 2000.0F);

         for (Popup popup : this.popups) {
            if (popup.getY() + popup.getHeight() > sr.getScaledHeight()) {
               popup.setY(sr.getScaledHeight() - 10.0F - popup.getHeight());
            }

            popup.render(context);
         }

         context.getMatrices().pop();
      }

      this.popups.removeIf(popupx -> popupx.getAnimation().getValue() == 0.0F && !popupx.isShowing());
   };
   private final EventListener<ChatRenderEvent> onPostHud = event -> {
      UIContext context = UIContext.of(
         event.getContext(),
         mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().getX(),
         mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().getY(),
         MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false)
      );
      context.getMatrices().push();
      context.getMatrices().translate(0.0F, 0.0F, 2000.0F);

      for (Popup popup : this.popups) {
         if (popup.getY() + popup.getHeight() > sr.getScaledHeight()) {
            popup.setY(sr.getScaledHeight() - 10.0F - popup.getHeight());
         }

         popup.render(context);
      }

      context.getMatrices().pop();
   };
   private final EventListener<ChatKeyPressEvent> onKeyPress = event -> {
      this.markChatInput();
      int modifiers = event.getModifiers();
      int keyCode = event.getKeyCode();
      if (keyCode == 90 && (modifiers & 2) != 0) {
         BKClientInit.getInstance().getHud().getHistoryManager().undo();
      } else if (keyCode == 89 && (modifiers & 2) != 0) {
         BKClientInit.getInstance().getHud().getHistoryManager().redo();
      }
   };
   private final EventListener<MouseScrollEvent> onPopupScroll = event -> {
      this.markChatInput();
      if (!(mc.currentScreen instanceof ChatScreen)) return;
      double mouseX = GuiUtility.getMouse().getX();
      double mouseY = GuiUtility.getMouse().getY();
      for (int i = this.popups.size() - 1; i >= 0; i--) {
         Popup popup = this.popups.get(i);
         if (popup.isShowing() && popup.isHovered(mouseX, mouseY)) {
            popup.onScroll(mouseX, mouseY, 0.0, event.getVerticalAmount());
            event.cancel();
            return;
         }
      }
   };
   /** HUD controls own the pointer before the hand editor sees a raw mouse event. */
   public boolean blocksViewModelEdit(float x, float y) {
      if (CustomChat.active() && dev.bkclient.ui.hud.impl.DirectMessageChat.isHovered(x,y)) return true;
      for (Popup popup : popups) if (popup.isShowing()) return true;
      for (HudElement element : elements) {
         if (element instanceof CustomChat && !CustomChat.active()) continue;
         if (element.isDragging() || element.isShowing() && element.isHovered(x,y)) return true;
      }
      ESP esp = BKClientInit.getInstance().getModuleManager().getModule(ESP.class);
      return esp != null && esp.getPlayerAt(x, y) != null;
   }

   private final EventListener<ChatClickEvent> onClick = event -> {
      this.markChatInput();
      if (!(mc.currentScreen instanceof ChatScreen)) return;
      for (Popup popup : this.popups) {
         if (!popup.isShowing()) continue;
         popup.onMouseClicked(event.getX(), event.getY(), MouseButton.fromButtonIndex(event.getButton()));
         if (popup.wasClickHandled() || popup.isHovered(event.getX(), event.getY())) {
            event.consume();
            return;
         }

         popup.setShowing(false);
      }

      for (HudElement element : this.elements) {
         if (element instanceof CustomChat && !CustomChat.active()) continue;
         element.onMouseClicked(event.getX(), event.getY(), MouseButton.fromButtonIndex(event.getButton()));
         if (element.isHovered(event.getX(), event.getY()) && element.isShowing() || element.isDragging()) {
            event.consume();
            return;
         }
      }

      if (event.getButton() == 0 || event.getButton() == 1) {
         ESP esp = BKClientInit.getInstance().getModuleManager().getModule(ESP.class);
         var player = esp == null ? null : esp.getPlayerAt(event.getX(), event.getY());
         if (player != null) {
            // Capture only the account name, never retain an entity/world in popup callbacks.
            openPlayerMenu(player.getGameProfile().getName(), event.getX(), event.getY());
            event.consume();
            return;
         }
      }

      if (event.getButton() == 1 && !this.disabledElements().isEmpty()) {
         Popup popup = new Popup(event.getX(), event.getY(), 90.0F, 6.0F).title(Localizator.translate("whatadd")).separator();

                  for (HudElement elementx : this.disabledElements()) {
            popup.button(Localizator.translate(elementx.getName()), elementx.getIcon(), popup1 -> {
               elementx.setShowing(true);
               popup1.setShowing(false);
               BKClientInit.getInstance().getFileManager().writeFile("client");
            });
         }

         this.popups.add(popup);
      } else if (event.getButton() == 1 && this.disabledElements().isEmpty() && this.timer.finished(600L)) {




         BKClientInit.getInstance()
            .getNotificationManager()
            .addNotificationOther(NotificationType.ERROR, "No elements", "There are no more HUD elements to add");
         this.timer.reset();
      }
   };
    private final EventListener<ChatReleaseEvent> onRelease = event -> {
      this.markChatInput();
       for (Popup popup : this.popups) {
          popup.onMouseReleased(event.getX(), event.getY(), MouseButton.fromButtonIndex(event.getButton()));
          if (popup.isHovered(event.getX(), event.getY())) {
             return;
          }
       }

       for (HudElement element : this.elements) {
          element.onMouseReleased(event.getX(), event.getY(), MouseButton.fromButtonIndex(event.getButton()));
       }
    };

      private final EventListener<MouseEvent> onGameMouse = event -> {
         // HUD editing is handled by ChatScreen events. While the game owns the mouse, a normal
         // attack/use click must never start a HUD drag. We only accept a release here so a drag
         // that began in the editor is safely terminated if the editor closes first.
         if (mc.currentScreen != null || event.getAction() != 0) return;

         float mouseX = (float)GuiUtility.getMouse().getX();
         float mouseY = (float)GuiUtility.getMouse().getY();
         MouseButton btn = MouseButton.fromButtonIndex(event.getButton());
         for (Popup popup : this.popups) {
            popup.onMouseReleased(mouseX, mouseY, btn);
         }
         for (HudElement element : this.elements) {
            element.onMouseReleased(mouseX, mouseY, btn);
         }
      };

     private final EventListener<KeyPressEvent> onKey = event -> {
        if (event.getAction() != 1) return;
        if (event.getKey() != 256) return;
        for (Popup popup : this.popups) {
           popup.setShowing(false);
        }
     };

     private void initialize() {
       BKClientInit.getInstance().getEventManager().subscribe(this);
       Effects effects = new Effects();
       KeyBinds keyBinds = new KeyBinds();
       TargetHud targetHud = new TargetHud();
       this.island = new DynamicDock();
       this.island.setShowing(true);
       WorldElement worldElement = new WorldElement();
       PlayerElement playerElement = new PlayerElement();
       CustomChat customChat = new CustomChat();
       Cooldowns cooldowns = new Cooldowns();
       ArmorHud armorHud = new ArmorHud();
       InventoryHud inventoryHud = new InventoryHud();
       inventoryHud.pos(400.0F, 180.0F);
       inventoryHud.setShowing(false);
       ShowLoreElement showLore = new ShowLoreElement();
       MusicPlayer musicPlayer = new MusicPlayer();
       FriendsElement friends = new FriendsElement();
       var staffList = new dev.bkclient.ui.hud.impl.StaffList();
       staffList.pos(232.0F, 120.0F);
       staffList.setShowing(false);
       AutoMarketHud autoMarket = new AutoMarketHud();
       autoMarket.setShowing(true);
       // RadarHud radar = new RadarHud();
       effects.pos(10.0F, 60.0F);
       effects.setShowing(true);
       keyBinds.pos(10.0F, 120.0F);
       keyBinds.setShowing(true);
       targetHud.pos(10.0F, 200.0F);
       targetHud.setShowing(true);
       worldElement.pos(10.0F, 10.0F);
       worldElement.setShowing(true);
       playerElement.pos(10.0F, 30.0F);
       playerElement.setShowing(true);
       customChat.setShowing(true);
       cooldowns.pos(120.0F, 60.0F);
       armorHud.pos(232.0F, 60.0F);
       showLore.pos(280.0F, 60.0F);
       musicPlayer.pos(280.0F, 162.0F);
       friends.pos(120.0F, 160.0F);
       autoMarket.pos(120.0F, 238.0F);
       // radar.pos(400.0F, 60.0F);
       // radar.setShowing(true);
       this.elements.addAll(List.of(
          effects, keyBinds, cooldowns, targetHud, showLore, this.island, worldElement, playerElement,
           autoMarket, friends, staffList, armorHud, inventoryHud, musicPlayer, /* radar, */ customChat
       ));
   }

   public Hud() {
      this.initialize();
      net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> this.releaseCache());
   }

   public List<HudElement> enabledElements() {
      return this.elements.stream().filter(HudElement::isShowing).toList();
   }

   public List<HudElement> disabledElements() {
      return this.elements.stream().filter(element -> !element.isShowing()).toList();
   }

   public <T extends HudElement> T getElementByName(String name) {
      return (T)this.elements.stream().filter(element -> element.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   private void openPlayerMenu(String name, float x, float y) {
      var friends = BKClientInit.getInstance().getFriendManager();
      if (playerPopup != null) popups.remove(playerPopup);
      Popup popup = new Popup(x, y, 150.0F).rawTitle(name).separator();
      playerPopup = popup;
      popup.button(Localizator.translate("esp.player.copy"), dev.bkclient.ui.components.LucideIcons.COPY, menu -> {
         mc.keyboard.setClipboard(name);
         menu.setShowing(false);
      });
      if (!friends.isFriend(name)) {
         popup.button(Localizator.translate("esp.player.friend"), dev.bkclient.ui.components.LucideIcons.USERS, menu -> {
            friends.add(name);
            menu.setShowing(false);
         });
      } else {
         popup.text(Localizator.translate("esp.player.already_friend"));
      }
      this.popups.add(popup);
   }

   @Generated
   public List<HudElement> getElements() {
      return this.elements;
   }

   @Generated
   public List<Popup> getPopups() {
      return this.popups;
   }

   @Generated
   public DynamicDock getIsland() {
      return this.island;
   }

   @Generated
   public HudHistoryManager getHistoryManager() {
      return this.historyManager;
   }

   @Generated
   public Grid getGrid() {
      return this.grid;
   }

   @Generated
   public String getDesc() {
      return this.desc;
   }

   @Generated
   public AnimatedText getDescText() {
      return this.descText;
   }

   @Generated
   public Timer getTimer() {
      return this.timer;
   }

   @Generated
   public EventListener<HudRenderEvent> getOnHud() {
      return this.onHud;
   }

   @Generated
   public EventListener<ChatRenderEvent> getOnPostHud() {
      return this.onPostHud;
   }

   @Generated
   public EventListener<ChatKeyPressEvent> getOnKeyPress() {
      return this.onKeyPress;
   }

   @Generated
   public EventListener<ChatClickEvent> getOnClick() {
      return this.onClick;
   }

   @Generated
   public EventListener<ChatReleaseEvent> getOnRelease() {
      return this.onRelease;
   }

   @Generated
   private void markChatInput() {
      this.chatLiveUntil = System.currentTimeMillis() + CHAT_LIVE_MS;
   }

   public void setDesc(String desc) {
      this.desc = desc;
   }
}
