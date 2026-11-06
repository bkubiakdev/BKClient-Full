package dev.bkclient.ui.emotes;

import dev.bkclient.framework.base.CustomScreen;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.emotes.EmoteManager;
import dev.bkclient.systems.emotes.EmoteRegistry;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * The emote wheel: eight emotes on a circle, more on further pages (mouse wheel). Click an emote, or let go of the key
 * that opened the wheel while pointing at it. Choosing the emote that is playing stops it.
 */
public final class EmoteWheelScreen extends CustomScreen {
   private static final int SLOTS = 8;
   private static final float RADIUS = 92.0F;
   private static final float TILE = 46.0F;
   private static final float DEAD_ZONE = 30.0F;

   private final int openKey;
   private final long openedAt = System.currentTimeMillis();
   private int page;
   private int hovered = -1;
   private float appear;

   public EmoteWheelScreen(int openKey) {
      super();
      this.openKey = openKey;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private static int pages() {
      return (EmoteRegistry.ALL.size() + SLOTS - 1) / SLOTS;
   }

   private EmoteRegistry emoteAt(int slot) {
      int index = this.page * SLOTS + slot;
      return index < EmoteRegistry.ALL.size() ? EmoteRegistry.ALL.get(index) : null;
   }

   @Override
   public void render(UIContext ctx) {
      this.appear = Math.min(1.0F, this.appear + 0.12F);
      float cx = this.width / 2.0F;
      float cy = this.height / 2.0F;
      float scale = 0.6F + 0.4F * this.appear;

      // Pointing at a slot: by angle from the centre, ignoring the middle.
      double dx = ctx.getMouseX() - cx;
      double dy = ctx.getMouseY() - cy;
      this.hovered = -1;
      if (Math.sqrt(dx * dx + dy * dy) > DEAD_ZONE) {
         double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0 + 180.0 / SLOTS;
         this.hovered = (int)(((angle % 360.0) + 360.0) % 360.0 / (360.0 / SLOTS)) % SLOTS;
         if (this.emoteAt(this.hovered) == null) this.hovered = -1;
      }

      // Hold-to-select: letting go of the opening key picks the emote under the pointer.
      if (this.openKey > 0 && System.currentTimeMillis() - this.openedAt > 250L && !keyDown(this.openKey)) {
         this.choose(this.hovered);
         return;
      }

      float alpha = this.appear;
      ctx.drawRoundedRect(0.0F, 0.0F, this.width, this.height, BorderRadius.ZERO, ColorRGBA.BLACK.withAlpha(95.0F * alpha));
      ctx.drawRoundedRect(cx - RADIUS * scale - 38.0F, cy - RADIUS * scale - 38.0F, (RADIUS * scale + 38.0F) * 2.0F, (RADIUS * scale + 38.0F) * 2.0F,
         BorderRadius.all(RADIUS * scale + 38.0F), new ColorRGBA(14.0F, 15.0F, 20.0F, 150.0F * alpha));

      EmoteRegistry playing = EmoteManager.current();
      for (int slot = 0; slot < SLOTS; slot++) {
         EmoteRegistry emote = this.emoteAt(slot);
         if (emote == null) continue;
         double a = Math.toRadians(-90.0 + slot * 360.0 / SLOTS);
         float x = cx + (float)Math.cos(a) * RADIUS * scale - TILE / 2.0F;
         float y = cy + (float)Math.sin(a) * RADIUS * scale - TILE / 2.0F;
         boolean hot = slot == this.hovered;
         boolean active = emote == playing;
         float grow = hot ? 4.0F : 0.0F;
         ColorRGBA plate = active ? new ColorRGBA(236.0F, 196.0F, 72.0F, 120.0F * alpha)
            : hot ? new ColorRGBA(255.0F, 255.0F, 255.0F, 70.0F * alpha) : new ColorRGBA(255.0F, 255.0F, 255.0F, 26.0F * alpha);
         ctx.drawRoundedRect(x - grow, y - grow, TILE + grow * 2.0F, TILE + grow * 2.0F, BorderRadius.all(12.0F), plate);
         ctx.drawRoundedTexture(emote.icon(), x + 5.0F - grow / 2.0F, y + 5.0F - grow / 2.0F, TILE - 10.0F + grow, TILE - 10.0F + grow,
            BorderRadius.all(8.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha * (hot || active ? 1.0F : 0.82F)));
      }

      Font title = Fonts.SEMIBOLD.getFont(9.0F);
      Font small = Fonts.REGULAR.getFont(6.5F);
      EmoteRegistry shown = this.hovered >= 0 ? this.emoteAt(this.hovered) : playing;
      String label = shown != null ? shown.title() : "Emotki";
      ctx.drawCenteredText(title, label, cx, cy - title.height() / 2.0F - 4.0F, ColorRGBA.WHITE.withAlpha(245.0F * alpha));
      String hint = shown == null ? "Wybierz emotkę" : shown == playing ? "Kliknij, aby zatrzymać" : pages() > 1 ? "Kółko myszy: strona " + (this.page + 1) + "/" + pages() : "";
      ctx.drawCenteredText(small, hint, cx, cy + 8.0F, new ColorRGBA(160.0F, 160.0F, 168.0F, 220.0F * alpha));
   }

   private static boolean keyDown(int key) {
      long window = MinecraftClient.getInstance().getWindow().getHandle();
      return key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
   }

   private void choose(int slot) {
      EmoteRegistry emote = slot >= 0 ? this.emoteAt(slot) : null;
      this.close();
      if (emote != null) EmoteManager.toggle(emote);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (button == MouseButton.LEFT) this.choose(this.hovered);
      else if (button == MouseButton.RIGHT) this.close();
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (verticalAmount != 0.0 && pages() > 1) {
         this.page = Math.floorMod(this.page + (verticalAmount < 0.0 ? 1 : -1), pages());
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }
}
