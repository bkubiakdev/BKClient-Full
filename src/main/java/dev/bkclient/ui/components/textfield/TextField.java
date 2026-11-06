package dev.bkclient.ui.components.textfield;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Generated;
import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.systems.emoji.EmojiManager;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.math.MathUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.sounds.ClientSounds;
import dev.bkclient.utility.time.Timer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;

public class TextField extends CustomComponent implements IMinecraft {
   public static TextField LAST_FIELD;
   private static final java.util.Set<TextField> FOCUSED_FIELDS = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
   private net.minecraft.client.gui.screen.Screen focusScreen;

   public static boolean hasFocusedField() {
      var screen = net.minecraft.client.MinecraftClient.getInstance().currentScreen;
      return screen != null && FOCUSED_FIELDS.stream().anyMatch(field -> field.focused && field.focusScreen == screen);
   }

   public static void releaseMovementKeys() {
      var options = net.minecraft.client.MinecraftClient.getInstance().options;
      for (var key : new net.minecraft.client.option.KeyBinding[]{options.forwardKey, options.backKey,
         options.leftKey, options.rightKey, options.jumpKey, options.sneakKey, options.sprintKey}) key.setPressed(false);
   }
   private final HashMap<Character, Float> charSoundCache = new HashMap<>();
   private final List<TextField.TypedText> texts = new ArrayList<>();
   private final Font font;
   private String lastBuilt = "";
   private String builtText = "";
   private final Animation focusing = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private boolean focused;
   private final Animation cursorAnim = new Animation(300L, 0.0F, Easing.BAKEK);
   private int cursor;
   private TextField.Selection selection;
   private float startX = 0.0F;
   private float endX = 0.0F;
   private int drag = -1;
   private long lastClickTime = 0L;
   private int clickCount = 0;
   private final Timer typingTimer = new Timer();
   private String preview = "";
   private String icon = "";
   private Map<String, FieldAction> append = new HashMap<>();
   private String appending = "";
   private float xPos;
   private final Timer moveTimer = new Timer();
   private float alpha = 1.0F;
   private ColorRGBA textColor = ColorRGBA.WHITE;
    public static final float EMOJI_WIDTH = 10.0F;

   @Override
   protected void renderComponent(UIContext context) {
      float offset = 0.0F;
      float cleanOffset = 0.0F;
      float cursorOffset = 0.0F;
      float fontOffset = this.height / 2.0F - this.font.height() / 2.0F;
      float cursorWidth = this.font.height() / 8.0F;
      this.texts.removeIf(textxx -> textxx.showing.getValue() == 0.0F && textxx.removing);
      this.focusing.update(this.focused);
      if (this.selection != null && this.selection.getStart() == this.selection.getEnd()) {
         this.selection = null;
      }

      if (this.drag != -1) {
         this.typingTimer.reset();
         int current = -1;
         float v = 0.0F;

         for (int idx = 0; idx < this.texts.size(); idx++) {
            TextField.TypedText typedText = this.texts.get(idx);
            float charW = typedTextWidth(typedText);
            if (context.getMouseX() < this.x + fontOffset + this.xPos + v + charW + charW / 2.0F) {
               current = idx;
               break;
            }

            v += charW;
         }

         if (current == -1) {
            current = this.texts.size();
         }

         if (current != this.drag) {
            this.selection = new TextField.Selection(current > this.drag, Math.min(this.drag, current), Math.max(this.drag, current));
            this.cursor = current;
         } else {
            if (this.selection != null) {
               this.cursor = this.selection.getStart();
            }

            this.selection = null;
         }
      }

      if (this.isHovered(context)) {
         CursorUtility.set(CursorType.TEXT);
      }

      this.updateAppend();
      ScissorUtility.push(context.getMatrices(), this.x, this.y, this.width, this.height);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
      context.drawRect(
         this.x + this.xPos + fontOffset + this.startX,
         this.y + fontOffset - 1.0F,
         this.endX - this.startX,
         this.font.height() + 2.0F,
         ColorRGBA.BLUE.mix(new ColorRGBA(76.0F, 99.0F, 122.0F), 0.7F).withAlpha(255.0F * this.focusing.getValue())
      );
      this.startX = 0.0F;
      this.endX = 0.0F;
      if (this.texts.isEmpty()) {
         context.drawText(
            this.font,
            this.preview,
            this.x + offset + fontOffset,
            this.y + fontOffset - 2.0F * this.focusing.getValue(),
            this.textColor.mulAlpha(0.75F * (1.0F - this.focusing.getValue()))
         );
      }

      if (!this.appending.isEmpty() && this.appending.toLowerCase().startsWith(this.builtText.toLowerCase()) && !this.builtText.isEmpty()) {
         context.drawText(
            this.font,
            this.builtText + this.appending.substring(this.builtText.length()),
            this.x + offset + fontOffset,
            this.y + fontOffset,
            this.textColor.withAlpha(150.0F * this.focusing.getValue())
         );
      }

      for (int idx = 0; idx < this.texts.size(); idx++) {
         TextField.TypedText typedText = this.texts.get(idx);
         typedText.showing.setDuration(200L);
         typedText.showing.update(!typedText.removing);
         float charW = typedTextWidth(typedText);
          if (typedText.emoji != null) {
             float textY = this.y + fontOffset + 2.0F - 2.0F * typedText.showing.getValue();
             float emojiY = textY + this.font.height() * 0.35F - EMOJI_WIDTH / 2.0F;
              context.drawTexture(
                 typedText.emoji.texture(),
                 this.x + offset + fontOffset + this.xPos,
                 emojiY,
                 EMOJI_WIDTH, EMOJI_WIDTH,
                 ColorRGBA.WHITE.withAlpha(255.0F * typedText.showing.getValue())
              );
          } else {
             context.drawText(
                this.font,
                String.valueOf(typedText.type),
                this.x + offset + fontOffset + this.xPos,
                this.y + fontOffset + 2.0F - 2.0F * typedText.showing.getValue(),
                this.textColor.withAlpha(255.0F * typedText.showing.getValue())
             );
          }
         offset += charW * typedText.showing.getValue();
         cleanOffset += charW;
         if (idx == this.cursor - 1) {
            cursorOffset = cleanOffset;
         }

         if (this.selection != null) {
            if (idx == this.selection.getStart() - 1) {
               this.startX = cleanOffset;
            }

            if (idx == this.selection.getEnd() - 1) {
               this.endX = cleanOffset;
            }
         }
      }

      cursorOffset += this.cursor == this.texts.size() ? 1.0F : 0.0F;
      float textEndX = cursorOffset + fontOffset + this.xPos;
      if (textEndX > this.width - 5.0F) {
         this.xPos -= textEndX - (this.width - 5.0F);
      }

      if (cursorOffset + fontOffset + this.xPos < 5.0F) {
         this.xPos = 5.0F - cursorOffset - fontOffset;
      }

      float actualWidth = 0.0F;
      for (TextField.TypedText t : this.texts) {
         actualWidth += typedTextWidth(t);
      }
      if (actualWidth + fontOffset < this.width - 10.0F) {
         this.xPos = 0.0F;
      }

      this.cursorAnim.setEasing(Easing.BAKEK_SMALLER);
      this.cursorAnim.update(cursorOffset);
      RenderUtility.rotate(
         context.getMatrices(),
         this.x + fontOffset + this.xPos + this.cursorAnim.getValue() + cursorWidth / 2.0F,
         this.y + fontOffset - 1.0F,
         Math.clamp(cursorOffset - this.cursorAnim.getValue(), -20.0F, 20.0F)
      );
      context.drawRect(
         this.x + fontOffset + this.cursorAnim.getValue() + this.xPos,
         this.y + fontOffset - 1.0F,
         cursorWidth,
         this.font.height() + 2.0F,
         this.textColor
            .withAlpha(
               (float)(
                  200.0F * this.focusing.getValue()
                     * (!this.typingTimer.finished(300L) ? 3.0 : MathUtility.sin(System.currentTimeMillis() / 200.0) + 2.0)
                     / 3.0
               )
            )
      );
      RenderUtility.end(context.getMatrices());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      ScissorUtility.pop();
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY)) {
         if (button == MouseButton.LEFT) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastClickTime < 500L) {
               this.clickCount++;
            } else {
               this.clickCount = 1;
            }

            this.lastClickTime = currentTime;
            this.setFocused(true);
            float offset = 0.0F;
            int newCursor = this.texts.size();

            float hitOffset = this.height / 2.0F - this.font.height() / 2.0F;
            for (TextField.TypedText typedText : this.texts) {
               float charW = typedTextWidth(typedText);
               if (mouseX < this.x + hitOffset + this.xPos + offset + charW + charW / 2.0F) {
                  newCursor = this.texts.indexOf(typedText);
                  break;
               }

               offset += charW;
            }

            this.cursor = newCursor;
            if (this.clickCount == 2) {
               this.selectWordAtCursor();
               this.drag = -1;
            } else {
               this.selection = null;
               this.drag = this.cursor;
            }
         }
      } else {
         this.focused = false;
      }
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.drag = -1;
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.focused) {
         if ((keyCode == 259 || keyCode == 261) && this.selection != null) {
            this.clearSelection();
             ClientSounds.CHAT_REMOVE.play(0.3F, 1.2F);
             this.moveCursor(0);
          } else if (keyCode == 259 && this.cursor > 0) {
            int offset = Screen.hasControlDown() ? Math.max(1, this.getWordSize(false)) : 1;

            for (int i = 0; i < offset; i++) {
               this.moveCursor(-1);
               TextField.TypedText last = null;

               for (TextField.TypedText text : this.texts) {
                  if (!text.removing) {
                     last = text;
                  }

                  if (this.texts.indexOf(text) == this.cursor) {
                     break;
                  }
               }

               if (last != null) {
                  last.removing = true;
               }
            }

                ClientSounds.CHAT_REMOVE.play(0.3F, 1.2F);
             } else if (keyCode == 261 && this.cursor < this.texts.size()) {
            int offset = Screen.hasControlDown() ? Math.max(1, this.getWordSize(true)) : 1;

            for (int i = 0; i < offset && this.cursor < this.texts.size(); i++) {
               for (int j = this.cursor; j < this.texts.size(); j++) {
                  TextField.TypedText text = this.texts.get(j);
                  if (!text.removing) {
                     text.removing = true;
                     break;
                  }
               }
            }

            this.moveCursor(0);
            ClientSounds.CHAT_REMOVE.play(0.3F, 1.2F);
         } else if (keyCode == 263) {
            ClientSounds.TYPING.play(0.3F, 1.3F);
            int offset = Screen.hasControlDown() ? Math.max(1, this.getWordSize(false)) : 1;
            if (Screen.hasShiftDown()) {
               this.select(-offset);
            } else if (this.selection != null) {
               this.cursor = this.selection.getStart();
               this.selection = null;
               return;
            }

            this.moveCursor(-offset);
         } else if (keyCode == 262) {
            ClientSounds.TYPING.play(0.3F, 1.3F);
            int offset = Screen.hasControlDown() ? Math.max(1, this.getWordSize(true)) : 1;
            if (Screen.hasShiftDown()) {
               this.select(offset);
            } else if (this.selection != null) {
               this.cursor = this.selection.getEnd();
               this.selection = null;
               return;
            }

            this.moveCursor(offset);
          } else if ((keyCode == 65 && (modifiers & 2) != 0) || (keyCode == 65 && (modifiers & 8) != 0)) {
            this.selection = new TextField.Selection(true, 0, this.texts.size());
         } else if (Screen.isCopy(keyCode)) {
            if (this.selection != null) {
               mc.keyboard.setClipboard(this.getSelectedText());
               return;
            }

            mc.keyboard.setClipboard(this.builtText);
         } else if (Screen.isCut(keyCode)) {
            if (this.selection != null) {
               mc.keyboard.setClipboard(this.getSelectedText());
               this.clearSelection();
               this.moveCursor(0);
               return;
            }

            mc.keyboard.setClipboard(this.builtText);

            for (TextField.TypedText text : this.texts) {
               text.removing = true;
            }

            this.builtText = "";
         } else if (Screen.isPaste(keyCode)) {
            this.paste(mc.keyboard.getClipboard());
         } else if (keyCode == 258 || keyCode == 257) {
            for (Entry<String, FieldAction> sugg : this.append.entrySet()) {
               if (sugg.getKey().toLowerCase().startsWith(this.builtText.toLowerCase()) && !this.builtText.isEmpty() && sugg.getValue() != null) {
                  this.clear();
                  if (keyCode == 257) {
                     sugg.getValue().getEnter().run();
                  } else {
                     sugg.getValue().getTab().run();
                  }

                  this.focused = false;
                  return;
               }
            }

            if (keyCode == 257) {
               this.focused = false;
            }
         } else if (keyCode == 259 && this.texts.isEmpty()) {
            this.focused = false;
         }
      }
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (!this.focused) {
         return false;
      }

      if (chr == ' ') {
         ClientSounds.TYPING.play(0.3F, 0.8F);
      } else {
         if (!this.charSoundCache.containsKey(chr)) {
            this.charSoundCache.put(chr, MathUtility.random(0.8, 1.2));
         }

         ClientSounds.TYPING.play(0.3F, this.charSoundCache.get(chr));
      }

      this.typeChar(chr);
      return true;
   }

    private float typedTextWidth(TextField.TypedText tt) {
       if (tt.emoji != null) return EMOJI_WIDTH;
       float w = this.font.width(String.valueOf(tt.type));
       return w > 0.0F ? w : this.font.height();
    }

   private void updateAppend() {
      this.lastBuilt = this.builtText;
      StringBuilder builder = new StringBuilder();

      for (TextField.TypedText text : this.texts) {
         if (text.emoji != null) {
            builder.append("[").append(text.emoji.code()).append("]");
         } else {
            builder.append(text.type);
         }
      }

      this.builtText = builder.toString();
      if (!this.lastBuilt.equals(this.builtText)) {
         this.appending = "";

         for (String string : this.append.keySet()) {
            if (string.toLowerCase().startsWith(this.builtText.toLowerCase()) && !this.builtText.isEmpty()) {
               this.appending = string;
            }
         }
      }
   }

   public int getWordSize(boolean forward) {
      int counter = 0;
      if (forward) {
         for (int i = this.cursor; i < this.texts.size(); i++) {
            TextField.TypedText text = this.texts.get(i);
            if (text.removing || text.emoji != null || text.type == ' ') {
               break;
            }

            counter++;
         }
      } else {
         for (int i = this.cursor - 1; i >= 0; i--) {
            TextField.TypedText text = this.texts.get(i);
            if (text.removing || text.emoji != null || text.type == ' ') {
               break;
            }

            counter++;
         }
      }

      return counter;
   }

   public void paste(String paste) {
      if (paste == null) {
         return;
      }
      int pos = 0;
      while (pos < paste.length()) {
         if (paste.charAt(pos) == '[') {
            int close = paste.indexOf(']', pos);
            if (close != -1) {
               String code = paste.substring(pos + 1, close);
               EmojiManager.EmojiData emoji = EmojiManager.getByCode(code);
               if (emoji != null) {
                  insertEmoji(emoji);
                  pos = close + 1;
                  continue;
               }
            }
         }
         this.typeChar(paste.charAt(pos));
         pos++;
      }
   }

   public void typeChar(char c) {
      this.clearSelection();
      this.texts.add(Math.clamp(this.cursor, 0, Math.max(0, this.texts.size())), new TextField.TypedText(c));
      this.moveCursor(1);
      this.drag = -1;
      LAST_FIELD = this;
   }

   public void insertEmoji(EmojiManager.EmojiData emoji) {
      this.clearSelection();
      this.texts.add(Math.clamp(this.cursor, 0, Math.max(0, this.texts.size())), new TextField.TypedText(emoji));
      this.moveCursor(1);
      this.drag = -1;
      LAST_FIELD = this;
   }

   private void moveCursor(int offset) {
      this.cursor = MathHelper.clamp(this.cursor + offset, 0, this.texts.size());
      this.typingTimer.reset();
   }

   public void clear() {
      this.texts.clear();
      this.builtText = "";
   }

   public void setText(String text) {
      this.clear();
      this.cursor = 0;
      this.xPos = 0.0F;
      this.selection = null;
      this.drag = -1;
      this.paste(text);
      this.updateAppend();
   }

   public void pasteInitial(String text) {
      this.setText(text);
   }

   public String getText() {
      StringBuilder builder = new StringBuilder();
      for (TextField.TypedText text : this.texts) {
         if (text.removing) continue;
         if (text.emoji != null) {
            builder.append('[').append(text.emoji.code()).append(']');
         } else {
            builder.append(text.type);
         }
      }
      return builder.toString();
   }

   public void scrollToEndAnimated() {
      this.cursor = this.texts.size();
      this.moveTimer.reset();
      this.typingTimer.reset();
   }

   private void selectWordAtCursor() {
      if (!this.texts.isEmpty()) {
         int wordStart = this.cursor;
         int wordEnd = this.cursor;

         for (int i = this.cursor - 1; i >= 0; wordStart = i--) {
            TextField.TypedText text = this.texts.get(i);
            if (text.removing || text.emoji != null || text.type == ' ' || !Character.isLetterOrDigit(text.type)) {
               break;
            }
         }

         for (int ix = this.cursor; ix < this.texts.size(); ix++) {
            TextField.TypedText text = this.texts.get(ix);
            if (text.removing || text.emoji != null || text.type == ' ' || !Character.isLetterOrDigit(text.type)) {
               break;
            }

            wordEnd = ix + 1;
         }

         if (wordStart != wordEnd) {
            this.selection = new TextField.Selection(true, wordStart, wordEnd);
            this.cursor = wordEnd;
         }
      }
   }

   private void clearSelection() {
      if (this.selection != null) {
         for (TextField.TypedText text : this.getSelected()) {
            text.removing = true;
         }

         this.cursor = this.selection.getStart();
         this.selection = null;
      }
   }

   private List<TextField.TypedText> getSelected() {
      List<TextField.TypedText> typedTexts = new ArrayList<>();
      boolean inSelection = false;

      for (TextField.TypedText text : this.texts) {
         if (this.texts.indexOf(text) == this.selection.getStart()) {
            inSelection = true;
         }

         if (this.texts.indexOf(text) == this.selection.getEnd()) {
            inSelection = false;
         }

         if (inSelection) {
            typedTexts.add(text);
         }
      }

      return typedTexts;
   }

   private String getSelectedText() {
      StringBuilder builder = new StringBuilder();
      boolean inSelection = false;

      for (TextField.TypedText text : this.texts) {
         if (this.texts.indexOf(text) == this.selection.getStart()) {
            inSelection = true;
         }

         if (this.texts.indexOf(text) == this.selection.getEnd()) {
            inSelection = false;
         }

         if (inSelection) {
            if (text.emoji != null) {
               builder.append("[").append(text.emoji.code()).append("]");
            } else {
               builder.append(text.type);
            }
         }
      }

      return builder.toString();
   }

   private void select(int offset) {
      if (this.selection == null) {
         this.selection = new TextField.Selection(offset > 0, this.cursor, this.cursor);
      }

      if (!this.selection.forward) {
         this.selection.start = MathHelper.clamp(this.selection.getStart() + offset, 0, this.texts.size());
      } else {
         this.selection.end = MathHelper.clamp(this.selection.getEnd() + offset, 0, this.texts.size());
      }
   }

   @Generated
   public TextField(Font font) {
      this.font = font;
   }

    @Generated
    public String getBuiltText() {
        return this.builtText;
    }

    public float getXPos() {
        return this.xPos;
    }

   @Generated
   public boolean isFocused() {
      return this.focused;
   }

   @Generated
   public void setFocused(boolean focused) {
      this.focused = focused;
      if (focused) {
         this.focusScreen = net.minecraft.client.MinecraftClient.getInstance().currentScreen;
         FOCUSED_FIELDS.add(this);
         LAST_FIELD = this;
         if (this.focusScreen != null) releaseMovementKeys();
      } else {
         FOCUSED_FIELDS.remove(this);
         this.focusScreen = null;
      }
   }

   @Generated
   public String getPreview() {
      return this.preview;
   }

   @Generated
   public String getIcon() {
      return this.icon;
   }

   @Generated
   public void setPreview(String preview) {
      this.preview = preview;
   }

   @Generated
   public void setIcon(String icon) {
      this.icon = icon;
   }

   @Generated
   public void setAppend(Map<String, FieldAction> append) {
      this.append = append;
   }

   @Generated
   public String getAppending() {
      return this.appending;
   }

   @Generated
   public void setAlpha(float alpha) {
      this.alpha = alpha;
   }

   @Generated
   public ColorRGBA getTextColor() {
      return this.textColor;
   }

   @Generated
   public void setTextColor(ColorRGBA textColor) {
      this.textColor = textColor;
   }

   static class Selection {
      final boolean forward;
      int start;
      int end;

      int getStart() {
         return Math.min(this.end, this.start);
      }

      int getEnd() {
         return Math.max(this.end, this.start);
      }

      @Generated
      public Selection(boolean forward, int start, int end) {
         this.forward = forward;
         this.start = start;
         this.end = end;
      }
   }

    static class TypedText {
       final Animation showing = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
       boolean removing;
       final char type;
       final EmojiManager.EmojiData emoji;

       @Generated
       public TypedText(char type) {
          this.type = type;
          this.emoji = null;
       }

       public TypedText(EmojiManager.EmojiData emoji) {
          this.type = '\0';
          this.emoji = emoji;
       }
    }
}
