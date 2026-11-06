package dev.bkclient.ui.hud.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.friends.PrivateMessageDetector;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.sounds.ClientSounds;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.StringHelper;
import java.util.ArrayList;
import java.util.List;

/** Session-only private conversations. Input is isolated from the public chat field. */
public final class DirectMessageChat implements IMinecraft, IScaledResolution {
   private static final net.minecraft.util.Identifier MESSAGE_ICON = BKClientInit.id("sf_symbold/chat/message.png");
   private static final net.minecraft.util.Identifier TRASH_ICON = BKClientInit.id("icons/trash.png");
   private static final net.minecraft.util.Identifier SEND_ICON = BKClientInit.id("sf_symbold/chat/send.png");
   private static final net.minecraft.util.Identifier TEST_SKIN = BKClientInit.id("icons/hud/bkubiakdev-skin.png");
   private static final DirectMessageChat INSTANCE = new DirectMessageChat();
   private final List<Conversation> conversations = new ArrayList<>();
   private Conversation active;
   private TextField input;
   private boolean expanded = true;
   private float x, y, width, height, scroll, maxScroll;
   private boolean registered;

   private static boolean enabled() {
      return CustomChat.active() && BKClientInit.getInstance().getModuleManager()
         .getModule(dev.bkclient.systems.modules.modules.visuals.CustomChatModule.class).areDirectMessagesEnabled();
   }

   private static final class Message {
      private final String body;
      private final boolean outgoing;
      private Font layoutFont;
      private float layoutWidth = Float.NaN;
      private List<String> layout;
      private List<String> widthLayout;
      private float layoutMaxWidth;
      Message(String body, boolean outgoing) { this.body = body; this.outgoing = outgoing; }
      /** Widest wrapped line of the current layout, recomputed only when the layout is rebuilt. */
      float maxLineWidth(Font font, List<String> lines) {
         if (widthLayout != lines) {
            layoutMaxWidth = (float)lines.stream().mapToDouble(font::width).max().orElse(0);
            widthLayout = lines;
         }
         return layoutMaxWidth;
      }
      String body() { return body; }
      boolean outgoing() { return outgoing; }
      List<String> lines(Font font, float width) {
         if (layout == null || layoutFont != font || Float.compare(layoutWidth, width) != 0) {
            layout = wrap(body, font, width); layoutFont = font; layoutWidth = width;
         }
         return layout;
      }
   }
   private static final class Conversation {
      final String name;
      final boolean simulated;
      final List<Message> messages = new ArrayList<>();
      String draft = "";
      int unread;
      long replyAt;
      int replies;
      Conversation(String name) { this(name, false); }
      Conversation(String name, boolean simulated) { this.name = name; this.simulated = simulated; }
   }

   public static void showPreview() {
      if (mc.player == null || !enabled()) {
         BKClientInit.getInstance().getNotificationManager().addNotificationOther(
            dev.bkclient.systems.notifications.NotificationType.INFO, "DM simulation", "Enter a world and enable Custom Chat > DM windows first.");
         return;
      }
      var dm = INSTANCE;
      Conversation preview = dm.conversations.stream().filter(c -> c.simulated).findFirst().orElse(null);
      if (preview == null) {
         if (dm.conversations.size() >= 6) dm.conversations.remove(dm.conversations.stream().filter(c -> c != dm.active).findFirst().orElseThrow());
         preview = new Conversation("bkubiakdev", true);
         dm.conversations.add(preview);
         dm.append(preview, new Message("Siema! Tak będzie wyglądała nasza rozmowa na priv :)", false));
         dm.append(preview, new Message("Napisz coś — odpowiem tutaj testowo.", false));
      }
      dm.select(preview);
      dm.expanded = true;
      mc.setScreen(new ChatScreen(""));
      dm.ensureInput();
      dm.input.setFocused(true);
   }

   public static void initialize() {
      if (INSTANCE.registered) return;
      INSTANCE.registered = true;
      ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
         if (!overlay) receiveText(message);
      });
      ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, parameters, receivedAt) -> {
         // The server's channel decoration distinguishes whispers from public message bodies.
         receiveText(parameters.applyChatDecoration(message));
      });
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
         INSTANCE.conversations.clear();
         INSTANCE.active = null;
         if (INSTANCE.input != null) { INSTANCE.input.clear(); INSTANCE.input.setFocused(false); }
      });
   }

   private static boolean receiveText(Text message) {
         if (mc.player == null || !enabled()) return false;
         String self = mc.player.getGameProfile().getName();
         PrivateMessageDetector.Incoming incoming = null;
         if (message.getContent() instanceof TranslatableTextContent content
             && content.getKey().equals("commands.message.display.incoming") && content.getArgs().length >= 2) {
            String sender = plain(content.getArgs()[0]);
            if (sender.matches("[A-Za-z0-9_]{1,16}") && !sender.equalsIgnoreCase(self)) {
               incoming = new PrivateMessageDetector.Incoming(sender, plain(content.getArgs()[1]));
            }
         } else incoming = PrivateMessageDetector.parseIncoming(message.getString(), self);
         if (incoming == null) return false;
         INSTANCE.receive(incoming);
         return true;
   }

   private static String plain(Object value) { return value instanceof Text text ? text.getString() : String.valueOf(value); }

   private void receive(PrivateMessageDetector.Incoming incoming) {
      Conversation conversation = conversations.stream().filter(c -> !c.simulated && c.name.equalsIgnoreCase(incoming.sender())).findFirst().orElse(null);
      if (conversation == null) {
         if (conversations.size() >= 6) {
            Conversation oldest = conversations.stream().filter(c -> c != active).findFirst().orElseThrow();
            conversations.remove(oldest);
         }
         conversation = new Conversation(incoming.sender());
         conversations.add(conversation);
      }
      append(conversation, new Message(incoming.body(), false));
      // Never change the destination while the user is composing a reply.
      if (active == null || (!active.simulated && !focused() && (input == null || input.getBuiltText().isBlank()))) select(conversation);
      if (conversation != active || !expanded || !(mc.currentScreen instanceof ChatScreen)) conversation.unread++;
      expanded = true;
   }

   private void append(Conversation conversation, Message message) {
      conversation.messages.add(message);
      if (conversation.messages.size() > 100) conversation.messages.removeFirst();
      if (conversation == active) scroll = 0;
   }

   private void select(Conversation conversation) {
      if (active != null && input != null) active.draft = input.getBuiltText();
      active = conversation;
      scroll = 0;
      if (input != null) input.setText(conversation.draft);
   }

   private void ensureInput() {
      if (input != null) return;
      input = new TextField(Fonts.MEDIUM.getFont(6.5F));
      input.setText(active == null ? "" : active.draft);
   }

   public static boolean focused() {
      return enabled() && mc.currentScreen instanceof ChatScreen && INSTANCE.expanded
         && INSTANCE.active != null && INSTANCE.input != null && INSTANCE.input.isFocused();
   }

   public static void render(UIContext context) {
      if (!enabled()) {
         if (INSTANCE.input != null) INSTANCE.input.setFocused(false);
         return;
      }
      INSTANCE.draw(context);
   }

   private void draw(UIContext context) {
      if (active == null) return;
      for (Conversation conversation : conversations) {
         if (conversation.simulated && conversation.replyAt != 0 && System.nanoTime() >= conversation.replyAt) {
            conversation.replyAt = 0;
            append(conversation, new Message("Dostałem wiadomość", false));
            conversation.unread++;
         }
      }
      ensureInput();
      boolean chatOpen = mc.currentScreen instanceof ChatScreen;
      if (!chatOpen) input.setFocused(false);
      width = Math.min(190, sr.getScaledWidth() - 12);
      boolean open = expanded && chatOpen;
      height = open ? Math.min(158, sr.getScaledHeight() - 12) : 25;
      x = sr.getScaledWidth() - width - 6;
      y = sr.getScaledHeight() - height - 6;
      context.drawClientRect(x, y, width, height, 1, 0, 7);
      var font = Fonts.MEDIUM.getFont(6.5F);
      drawHead(context, active, x + 7, y + 7, 10);
      context.drawText(font, active.name, x + 23, y + 5, Colors.getTextColor());
      context.drawText(Fonts.REGULAR.getFont(5.5F), active.simulated ? "TEST · local conversation" : "/msg " + active.name,
         x + 23, y + 14, Colors.getTextColor().withAlpha(110));
      int unread = 0;
      for (Conversation c : conversations) unread += c.unread;
      if (unread > 0) context.drawRightText(font, Integer.toString(unread), x + width - 46, y + 9, Colors.getAccent());
      context.drawTextureSmooth(MESSAGE_ICON, x + width - 38, y + 7, 11, 11,
         open ? Colors.getAccent() : Colors.getTextColor());
      context.drawTextureSmooth(TRASH_ICON, x + width - 18, y + 8, 9, 9,
         GuiUtility.isHovered(x + width - 23, y + 3, 20, 20, context) ? ColorRGBA.RED : Colors.getTextColor().withAlpha(150));
      String hint = null;
      if (GuiUtility.isHovered(x + width - 23, y + 3, 20, 20, context)) hint = "Remove conversation";
      else if (GuiUtility.isHovered(x + width - 44, y + 3, 21, 20, context)) hint = open ? "Minimize chat" : "Open chat";
      if (hint != null && chatOpen) drawHint(context, hint, x + width / 2, y - 15);
      if (!open) return;
      active.unread = 0;
      for (int i = 0; i < conversations.size(); i++) {
         Conversation c = conversations.get(i);
         float tabX = x + 7 + i * 23;
         context.drawRoundedRect(tabX, y + 27, 19, 18, BorderRadius.all(5),
            (c == active ? Colors.getAccent() : Colors.getTextColor()).withAlpha(c == active ? 48 : 12));
         drawHead(context, c, tabX + 5, y + 31, 9);
         if (c.unread > 0) context.drawRoundedRect(tabX + 14, y + 28, 3, 3, BorderRadius.all(1.5F), Colors.getAccent());
         if (GuiUtility.isHovered(tabX, y + 27, 19, 18, context)) {
            drawHint(context, c.name + (c.simulated ? " · TEST" : ""), tabX + 10, y - 15);
         }
      }
      float listY = y + 49, listH = height - 79;
      float contentH = 0;
      for (Message message : active.messages) contentH += message.lines(font, width - 30).size() * 9 + 8;
      maxScroll = Math.max(0, contentH - listH);
      scroll = Math.clamp(scroll, 0, maxScroll);
      float rowY = listY + listH - contentH + scroll;
      ScissorUtility.push(context.getMatrices(), x + 4, listY, width - 8, listH);
      for (int i = 0; i < active.messages.size(); i++) {
         Message message = active.messages.get(i);
         List<String> row = message.lines(font, width - 30);
         float rowH = row.size() * 9 + 5;
         if (rowY + rowH >= listY && rowY <= listY + listH) {
            float rowW = Math.min(width - 18, 10 + message.maxLineWidth(font, row));
            float rowX = message.outgoing() ? x + width - rowW - 7 : x + 7;
            context.drawRoundedRect(rowX, rowY, rowW, rowH, BorderRadius.all(4),
               (message.outgoing() ? Colors.getAccent() : Colors.getTextColor()).withAlpha(message.outgoing() ? 45 : 15));
            for (int j = 0; j < row.size(); j++) context.drawText(font, row.get(j), rowX + 5, rowY + 3 + j * 9, Colors.getTextColor());
         }
         rowY += rowH + 3;
      }
      ScissorUtility.pop();
      float inputY = y + height - 24;
      context.drawRoundedRect(x + 6, inputY, width - 12, 18, BorderRadius.all(6), Colors.getTextColor().withAlpha(12));
      input.setPreview("Message " + active.name + "…");
      input.set(x + 11, inputY + 3, width - 45, 12);
      input.setTextColor(Colors.getTextColor());
      input.render(context);
      context.drawTextureSmooth(SEND_ICON, x + width - 23, inputY + 4, 10, 10, Colors.getAccent());
   }

   private static List<String> wrap(String text, Font font, float width) {
      List<String> lines = new ArrayList<>();
      StringBuilder line = new StringBuilder();
      for (int i = 0; i < text.length();) {
         int cp = text.codePointAt(i);
         String next = new String(Character.toChars(cp));
         if (!line.isEmpty() && font.width(line + next) > width) { lines.add(line.toString()); line.setLength(0); }
         line.append(next);
         i += Character.charCount(cp);
      }
      if (!line.isEmpty()) lines.add(line.toString());
      return lines.isEmpty() ? List.of("") : lines;
   }

   private static void drawHint(UIContext context, String label, float centerX, float top) {
      var font = Fonts.MEDIUM.getFont(6);
      float w = font.width(label) + 12;
      float left = Math.clamp(centerX - w / 2, 3, Math.max(3, sr.getScaledWidth() - w - 3));
      top = Math.max(3, top);
      context.drawRoundedRect(left, top, w, 13, BorderRadius.all(4), Colors.getBackgroundColor().withAlpha(235));
      context.drawText(font, label, left + 6, top + 4, Colors.getTextColor());
   }

   private void removeActive() {
      int index = conversations.indexOf(active);
      if (index < 0) return;
      active.replyAt = 0;
      conversations.remove(index);
      active = null;
      input.clear();
      input.setFocused(false);
      scroll = 0;
      if (!conversations.isEmpty()) select(conversations.get(Math.min(index, conversations.size() - 1)));
   }

   private static void drawHead(UIContext context, Conversation conversation, float x, float y, float size) {
      var skin = conversation.simulated ? TEST_SKIN
         : dev.bkclient.utility.render.PlayerHeadSkins.get(conversation.name);
      DrawUtility.drawPlayerHeadWithRoundedShader(context.getMatrices(), skin, x, y, size, BorderRadius.all(2), ColorRGBA.WHITE);
      DrawUtility.drawPlayerHatLayerWithRoundedShader(context.getMatrices(), skin, x, y, size, BorderRadius.all(2), ColorRGBA.WHITE);
   }
   public static boolean isHovered(double x, double y) {
      return enabled() && INSTANCE.active != null && mc.currentScreen instanceof ChatScreen
         && GuiUtility.isHovered(INSTANCE.x,INSTANCE.y,INSTANCE.width,INSTANCE.height,x,y);
   }
   public static boolean click(double mouseX, double mouseY, int button) {
      if (!enabled() || INSTANCE.active == null || !(mc.currentScreen instanceof ChatScreen)) return false;
      var dm = INSTANCE;
      if (!GuiUtility.isHovered(dm.x, dm.y, dm.width, dm.height, mouseX, mouseY)) {
         if (dm.input != null) dm.input.setFocused(false);
         return false;
      }
      if (button != 0) return true;
      dm.ensureInput();
      if (GuiUtility.isHovered(dm.x + dm.width - 23, dm.y + 3, 20, 20, mouseX, mouseY)) {
         dm.removeActive();
         return true;
      }
      if (mouseY < dm.y + 25) { dm.expanded = !dm.expanded; dm.input.setFocused(dm.expanded); return true; }
      if (!dm.expanded) return true;
      for (int i = 0; i < dm.conversations.size(); i++) {
         if (GuiUtility.isHovered(dm.x + 7 + i * 23, dm.y + 27, 19, 18, mouseX, mouseY)) {
            dm.select(dm.conversations.get(i)); dm.input.setFocused(true); return true;
         }
      }
      if (GuiUtility.isHovered(dm.x + dm.width - 28, dm.y + dm.height - 24, 22, 18, mouseX, mouseY)) {
         dm.send(); dm.input.setFocused(true); return true;
      }
      dm.input.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
      dm.input.setFocused(true);
      return true;
   }

   public static boolean scroll(double amount) {
      var dm = INSTANCE;
      if (!enabled() || !(mc.currentScreen instanceof ChatScreen) || dm.active == null || !dm.expanded) return false;
      var mouse = GuiUtility.getMouse();
      if (!GuiUtility.isHovered(dm.x, dm.y, dm.width, dm.height, mouse.getX(), mouse.getY())) return false;
      dm.scroll = Math.clamp(dm.scroll + (float)amount * 16, 0, dm.maxScroll);
      return true;
   }

   public static void release() {
      if (INSTANCE.input != null) INSTANCE.input.onMouseReleased(0, 0, MouseButton.LEFT);
   }

   public static void key(int key, int scanCode, int modifiers) {
      if (key == 257 || key == 335) INSTANCE.send();
      else if (key == 256) { INSTANCE.expanded = false; INSTANCE.input.setFocused(false); }
      else INSTANCE.input.onKeyPressed(key, scanCode, modifiers);
   }

   public static void character(int codepoint, int modifiers) {
      for (char chr : Character.toChars(codepoint)) INSTANCE.input.charTyped(chr, modifiers);
   }

   private void send() {
      if (!enabled() || active == null || mc.player == null || !active.name.matches("[A-Za-z0-9_]{1,16}")) return;
      String body = StringHelper.stripInvalidChars(input.getBuiltText().replace("§", "")).strip();
      if (body.isEmpty()) return;
      if (active.simulated) {
         append(active, new Message(body, true));
         active.replyAt = System.nanoTime() + 900_000_000L;
         active.draft = "";
         input.clear();
         ClientSounds.CHAT_SEND.play(0.2F);
         return;
      }
      // Every chunk remains a private command, including text starting with a slash.
      for (String command : PrivateMessageDetector.replyCommands(active.name, body)) mc.player.networkHandler.sendChatCommand(command);
      append(active, new Message(body, true));
      active.draft = "";
      input.clear();
      ClientSounds.CHAT_SEND.play(0.2F);
   }
}
