package dev.bkclient.ui.hud.impl;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.window.MouseScrollEvent;
import dev.bkclient.systems.emoji.EmojiManager;
import dev.bkclient.systems.emoji.EmojiTextureLoader;
import dev.bkclient.systems.supabase.SupabaseService;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.hud.HudElement;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.sounds.ClientSounds;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import org.joml.Matrix4f;

public class CustomChat extends HudElement {
    private static final float MAX_WIDTH = 240.0F;
    private static final float MSG_PADDING_H = 6.0F;
    private static final float MSG_PADDING_V = 3.0F;
    private static final float MSG_GAP = 3.0F;
    private static final float INPUT_HEIGHT = 20.0F;
    private static final float INPUT_RADIUS = 10.0F;
    private static final float MSG_RADIUS = 6.0F;
    private static final float FONT_SIZE = 6.0F;
    private static final int MAX_MESSAGES = 100;

    private final List<ChatMessage> messages = new ArrayList<>();
    private float scrollOffset = 0.0F;
    private float targetScroll = 0.0F;
    private float contentHeight = 0.0F;
    private float scrollVelocity = 0.0F;
    private boolean shouldAutoScroll = true;
    private TextField searchField;
    private final Animation inputAnim = new Animation(300L, 0.0F, new Easing.BackOut(1.1F));
    private boolean wasChatOpen = false;
    private boolean emojiPickerOpen = false;
    private float emojiPickerScroll = 0.0F;
    private final Animation emojiPickerAnim = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
    private final List<ClickableRegion> clickableRegions = new ArrayList<>();
    /** Called from ChatHud after vanilla accepts any chat message, on the client thread. */
    public static void captureMessage(Text content) {
        if (BKClientInit.getInstance() == null || BKClientInit.getInstance().getHud() == null) return;
        for (HudElement element : BKClientInit.getInstance().getHud().getElements()) {
            if (element instanceof CustomChat chat) {
                String text = content.getString();
                if (text.isBlank()) return;
                chat.messages.add(new ChatMessage(text, chat.extractSender(text), System.currentTimeMillis(), chat.extractClickables(content)));
                while (chat.messages.size() > MAX_MESSAGES) chat.messages.remove(0);
                chat.shouldAutoScroll = true;
                return;
            }
        }
    }

    private final EventListener<MouseScrollEvent> onScroll = event -> {
        if (!this.showing || !Interface.customChatEnabled()) return;
        if (DirectMessageChat.scroll(event.getVerticalAmount())) { event.cancel(); return; }
        float mouseX = (float)mc.mouse.getX() * IScaledResolution.sr.getScaledWidth() / mc.getWindow().getWidth();
        float mouseY = (float)mc.mouse.getY() * IScaledResolution.sr.getScaledHeight() / mc.getWindow().getHeight();

        float totalH = this.contentHeight;
        float availableH = this.height;
        if (totalH > availableH) {
            if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height + INPUT_HEIGHT + 4.0F) {
                this.scrollVelocity += (float)(event.getVerticalAmount() * 8.0F);
                if (event.getVerticalAmount() > 0.0) {
                    this.shouldAutoScroll = false;
                }
            }
        }

        if (emojiPickerOpen) {
            float cellSize = 26.0F;
            float gridPad = 4.0F;
            int colsInt = 6;
            float pickerW = colsInt * cellSize + gridPad * 2;
            float pickerH = 130.0F;
            float pickerX = this.x + this.width - 67.0F;
            float pickerY = this.y + this.height - pickerH - 2.0F;
            pickerX = Math.max(2.0F, Math.min(IScaledResolution.sr.getScaledWidth() - pickerW - 2.0F, pickerX));
            pickerY = Math.max(2.0F, pickerY);

            if (mouseX >= pickerX && mouseX <= pickerX + pickerW && mouseY >= pickerY && mouseY <= pickerY + pickerH) {
                float totalEmojiH = ((EmojiManager.getAll().size() + colsInt - 1) / colsInt) * cellSize + gridPad * 2;
                float maxScroll = Math.max(0.0F, totalEmojiH - pickerH);
                if (maxScroll > 0.0F) {
                    emojiPickerScroll += (float)(event.getVerticalAmount() * 16.0F);
                    if (emojiPickerScroll < -maxScroll) emojiPickerScroll = -maxScroll;
                    if (emojiPickerScroll > 0.0F) emojiPickerScroll = 0.0F;
                }
            }
        }
    };

    /**
     * Shared activation check for rendering and keyboard hooks; both require
     * the chat module and HUD element to be enabled.
     */
    public static boolean active() {
        if (!Interface.customChatEnabled()
                || BKClientInit.getInstance() == null
                || BKClientInit.getInstance().getHud() == null) {
            return false;
        }
        for (HudElement element : BKClientInit.getInstance().getHud().getElements()) {
            if (element instanceof CustomChat && element.isShowing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Synchronizes vanilla history and completion edits into the custom input field.
     * Edits already mirrored from the custom field are ignored to avoid a feedback loop.
     */
    public static void syncInput(String text) {
        if (text == null || !active()) {
            return;
        }
        for (HudElement element : BKClientInit.getInstance().getHud().getElements()) {
            if (element instanceof CustomChat chat) {
                chat.adoptText(text);
                return;
            }
        }
    }

    private void adoptText(String text) {
        ensureSearchField();
        if (this.searchField.getText().equals(text)) {
            return;
        }
        this.searchField.setText(text);
        this.searchField.scrollToEndAnimated();
    }

    public CustomChat() {
        super("hud.custom_chat", "icons/hud/player.png");
        this.width = MAX_WIDTH;
        this.height = 150.0F;
        BKClientInit.getInstance().getEventManager().subscribe(this);
        DirectMessageChat.initialize();
    }

    private void handleAirdropClick(String cmd) {
        String[] parts = cmd.split(" ");
        if (parts.length >= 4) {
            String action = parts[2];
            String id = parts[3];
            if (action.equals("accept")) {
                if (parts.length >= 5) {
                    String configData = parts[4];
                    SupabaseService.getInstance().acceptAirdrop(id, configData);
                }
            } else if (action.equals("reject")) {
                SupabaseService.getInstance().rejectAirdrop(id);
            }
        }
    }

    private void ensureSearchField() {
        if (this.searchField == null) {
            this.searchField = new TextField(Fonts.MEDIUM.getFont(7.0F));
            this.searchField.setPreview(Localizator.translate("chat.placeholder"));
        }
    }

    @Override
    public void update(UIContext context) {
        this.width = Math.min(MAX_WIDTH, IScaledResolution.sr.getScaledWidth() * 0.7F);
        this.height = Math.min(150.0F, IScaledResolution.sr.getScaledHeight() * 0.35F);
        // Text layout needs the final viewport, not the HUD size animation starting at zero.
        this.getWidthAnim().setValue(this.width);
        this.getHeightAnim().setValue(this.height);
        this.x = 2.0F;
        this.y = IScaledResolution.sr.getScaledHeight() - this.height - INPUT_HEIGHT - 6.0F;

        float maxScroll = 0.0F;
        float minScroll = -(this.contentHeight - this.height);
        if (minScroll > 0.0F) minScroll = 0.0F;

        this.scrollVelocity *= 0.88F;
        if (Math.abs(this.scrollVelocity) < 0.5F) {
            this.scrollVelocity = 0.0F;
        }
        this.targetScroll += this.scrollVelocity;
        this.targetScroll = Math.max(minScroll, Math.min(maxScroll, this.targetScroll));

        this.scrollOffset += (this.targetScroll - this.scrollOffset) * 0.12F;
        if (Math.abs(this.scrollOffset - this.targetScroll) < 0.1F) {
            this.scrollOffset = this.targetScroll;
        }

        boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        if (chatOpen && !this.wasChatOpen) {
            ensureSearchField();
            var vanillaField = ((dev.bkclient.mixin.accessors.ChatScreenAccessor) mc.currentScreen).bkclient$getChatField();
            if (vanillaField != null) adoptText(vanillaField.getText());
            this.searchField.setFocused(true);
            TextField.LAST_FIELD = this.searchField;
        }
        this.wasChatOpen = chatOpen;
        if (dev.bkclient.systems.modules.modules.visuals.Beautifully.chatAnimation()) {
            this.inputAnim.setDuration(200L);
            this.inputAnim.setEasing(dev.bkclient.systems.modules.modules.visuals.Beautifully.OUT_CUBIC);
            this.inputAnim.update(chatOpen);
        } else {
            this.inputAnim.setValue(chatOpen ? 1 : 0);
        }
        this.emojiPickerAnim.update(this.emojiPickerOpen);

        super.update(context);
    }

    @Override
    public void render(UIContext context) {
        if (!this.showing || !Interface.customChatEnabled()) {
            this.clickableRegions.clear();
            this.emojiPickerOpen = false;
            this.wasChatOpen = false;
            this.inputAnim.setValue(0.0F);
            this.emojiPickerAnim.setValue(0.0F);
            if (this.searchField != null) {
                this.searchField.setFocused(false);
                if (TextField.LAST_FIELD == this.searchField) TextField.LAST_FIELD = null;
            }
            // Also release the private-message field; its enabled() follows CustomChat.active().
            DirectMessageChat.render(context);
            return;
        }
        this.update(context);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.getMatrices().push();
        this.renderComponent(context);
        DirectMessageChat.render(context);
        context.getMatrices().pop();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void renderComponent(UIContext context) {
        ensureSearchField();
        clickableRegions.clear();
        boolean dark = HudTheme.isOld() || BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
                && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();

        if (!this.messages.isEmpty()) {
            Font msgFont = Fonts.MEDIUM.getFont(FONT_SIZE);
            float maxTextW = this.width - MSG_PADDING_H * 2.0F - 10.0F;

            int messageCount = this.messages.size();
            float totalH = 0.0F;
            for (int i = 0; i < messageCount; i++) {
                ChatMessage msg = this.messages.get(i);
                float msgH = msg.getHeight(msgFont, maxTextW);
                totalH += msgH + MSG_GAP;
            }

            this.contentHeight = totalH;
            float availableH = this.height;
            if (totalH > availableH) {
                if (this.shouldAutoScroll) {
                    this.targetScroll = -(totalH - availableH);
                    this.scrollVelocity = 0.0F;
                    this.shouldAutoScroll = false;
                }
                this.targetScroll = Math.max(-(totalH - availableH), this.targetScroll);
                this.targetScroll = Math.min(0.0F, this.targetScroll);
            } else {
                this.targetScroll = 0.0F;
                this.shouldAutoScroll = true;
            }

            float scroll = this.scrollOffset;
            float drawY = this.y + scroll;

            float feather = 15.0F;

            ScissorUtility.push(context.getMatrices(), this.x, this.y - feather, this.width, this.height + feather * 2);

            // Same for every message of this frame; read once instead of once per visible message.
            String username = mc.getSession() != null ? mc.getSession().getUsername() : "";
            boolean animateChat = dev.bkclient.systems.modules.modules.visuals.Beautifully.chatAnimation();

            for (int i = 0; i < messageCount; i++) {
                ChatMessage msg = this.messages.get(i);
                float msgH = msg.getHeight(msgFont, maxTextW);

                if (drawY + msgH < this.y - feather || drawY > this.y + this.height + feather) {
                    drawY += msgH + MSG_GAP;
                    continue;
                }

                List<String> lines = msg.getLines(msgFont, maxTextW);
                float maxLineW = msg.getMaxLineW(msgFont, maxTextW);
                float bubbleW = maxLineW + MSG_PADDING_H * 2.0F;

                boolean isOwn = msg.sender.equals(username);
                if (!animateChat) msg.entryAnim.setValue(1);
                msg.entryAnim.setDuration(200L);
                msg.entryAnim.setEasing(dev.bkclient.systems.modules.modules.visuals.Beautifully.OUT_CUBIC);
                float p = msg.entryAnim.update(1.0F);
                float inv = 1.0F - p;
                float vRise = msgH * 0.25F * inv;
                float hSlide = isOwn ? (bubbleW + 8.0F) * inv : -(bubbleW + 8.0F) * inv;
                float pulse = 1.0F + 0.05F * (float)Math.sin(p * Math.PI * 2.5F) * (1.0F - p);
                float bubbleX = (isOwn ? this.x + this.width - bubbleW - 4.0F : this.x + 4.0F) + hSlide;
                float bubbleY = drawY + vRise;

                float fade = Math.clamp((bubbleY + msgH - (this.y - feather)) / (feather * 2), 0.0F, 1.0F);

                ColorRGBA bubbleBg = isOwn
                        ? Colors.getAccent().withAlpha((int)(200 * fade))
                        : dark
                            ? new ColorRGBA(40.0F, 40.0F, 45.0F, (int)(220 * fade))
                            : new ColorRGBA(230.0F, 230.0F, 235.0F, (int)(220 * fade));
                EmojiManager.EmojiData singleEmoji = isSingleEmojiData(msg.text);

                context.getMatrices().push();
                float pivotX = bubbleX + bubbleW / 2.0F;
                float pivotY = bubbleY + msgH / 2.0F;
                context.getMatrices().translate(pivotX, pivotY, 0.0F);
                context.getMatrices().scale(pulse, pulse, 1.0F);
                context.getMatrices().translate(-pivotX, -pivotY, 0.0F);

                ColorRGBA textColor = isOwn
                        ? ColorRGBA.WHITE.withAlpha((int)(230 * fade))
                        : dark
                            ? ColorRGBA.WHITE.withAlpha((int)(200 * fade))
                            : new ColorRGBA(30.0F, 30.0F, 30.0F).withAlpha((int)(230 * fade));

                if (singleEmoji != null) {
                    float eSize = 18.0F;
                    float eX = bubbleX + (bubbleW - eSize) / 2.0F;
                    float eY = bubbleY + (msgH - eSize) / 2.0F;
                    context.drawTextureSmooth(EmojiTextureLoader.getStandaloneTexture(singleEmoji.texture()), eX, eY, eSize, eSize, ColorRGBA.WHITE.withAlpha(textColor.getAlpha()));
                } else {
                    if (!HudTheme.isOld() && Interface.showGlass()) {
                        context.drawLiquidGlass(bubbleX, bubbleY, bubbleW, msgH, 2.0F, 0.05F, BorderRadius.all(MSG_RADIUS), ColorRGBA.WHITE.withAlpha((int)(36 * fade)));
                    }
                    context.drawRoundedRect(bubbleX, bubbleY, bubbleW, msgH, BorderRadius.all(MSG_RADIUS), bubbleBg.mulAlpha(dev.bkclient.ui.hud.HudVisualScope.opacity()));
                    dev.bkclient.ui.hud.HudVisualScope.panel(context,bubbleX,bubbleY,bubbleW,msgH,MSG_RADIUS,2,fade);
                }

                context.getMatrices().pop();

                if (singleEmoji == null) {
                    float textY = bubbleY + MSG_PADDING_V + 1.0F;
                    int lineOffset = 0;
                    for (String line : lines) {
                        int found = msg.text.indexOf(line, lineOffset);
                        if (found >= 0) lineOffset = found;
                        drawTextWithEmojis(context, msgFont, line, bubbleX + MSG_PADDING_H, textY, textColor, 8.0F, msg.clickables, lineOffset);
                        lineOffset += line.length();
                        boolean hasEmoji = line.contains("[") && line.contains("]");
                        textY += hasEmoji ? 9.0F : msgFont.height() + 2.0F;
                    }
                }

                drawY += msgH + MSG_GAP;
            }

            ScissorUtility.pop();
        }

        boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        float inputAnim = this.inputAnim.getValue();
        if (inputAnim > 0.0F) {
            float inputY = this.y + this.height + 4.0F + (1.0F - inputAnim) * 12.0F;
            float inputX = this.x;
            float inputW = this.width;

            context.getMatrices().push();
            context.getMatrices().translate(inputX + inputW / 2.0F, inputY + INPUT_HEIGHT / 2.0F, 0.0F);
            context.getMatrices().scale(inputAnim, inputAnim, 1.0F);
            context.getMatrices().translate(-(inputX + inputW / 2.0F), -(inputY + INPUT_HEIGHT / 2.0F), 0.0F);

            if (HudTheme.isOld()) {
                HudTheme.drawPanel(context, inputX, inputY, inputW, INPUT_HEIGHT, inputAnim, 0.0F, INPUT_RADIUS);
            } else if (Interface.showGlass()) {
                context.drawLiquidGlass(inputX, inputY, inputW, INPUT_HEIGHT, 2.0F, 0.08F, BorderRadius.all(INPUT_RADIUS), ColorRGBA.WHITE.withAlpha((int)(255 * Interface.glass() * inputAnim)));
                context.drawRoundedRect(inputX, inputY, inputW, INPUT_HEIGHT, BorderRadius.all(INPUT_RADIUS), Colors.getBackgroundColor().withAlpha((int)(255 * (0.8F - 0.3F * Interface.glass()) * inputAnim)));
            } else {
                ColorRGBA inputBg = dark
                        ? new ColorRGBA(30.0F, 30.0F, 35.0F, 220)
                        : new ColorRGBA(240.0F, 240.0F, 245.0F, 220);
                context.drawRoundedRect(inputX, inputY, inputW, INPUT_HEIGHT, BorderRadius.all(INPUT_RADIUS), inputBg.withAlpha((int)(inputBg.getAlpha() * inputAnim * dev.bkclient.ui.hud.HudVisualScope.opacity())));

            }
            if (!HudTheme.isOld()) dev.bkclient.ui.hud.HudVisualScope.panel(context,inputX,inputY,inputW,INPUT_HEIGHT,INPUT_RADIUS,2,inputAnim);

            context.getMatrices().pop();

            float emojiBtnW = 22.0F;
            float sendBtnW = 34.0F;
            float btnH = 16.0F;
            float fieldEndX = inputX + inputW - emojiBtnW - sendBtnW - 9.0F;
            this.searchField.set(inputX + 10.0F, inputY + 3.0F, fieldEndX - inputX - 10.0F, INPUT_HEIGHT - 6.0F);
            ColorRGBA textColor = dark ? HudTheme.strongTextColor() : new ColorRGBA(30.0F, 30.0F, 30.0F);
            this.searchField.setTextColor(textColor.mulAlpha(0.85F * inputAnim));
            this.searchField.render(context);

            float emojiBtnX = fieldEndX + 3.0F;
            float emojiBtnY = inputY + (INPUT_HEIGHT - btnH) / 2.0F;
            ColorRGBA emojiBtnBg = HudTheme.textColor().withAlpha((int)(15 * inputAnim));
            context.drawRoundedRect(emojiBtnX, emojiBtnY, emojiBtnW, btnH, BorderRadius.all(7.0F), emojiBtnBg);
            float emojiIconSize = 11.0F;
            context.drawTexture(BKClientInit.id("icons/hud/emoji.png"), emojiBtnX + (emojiBtnW - emojiIconSize) / 2.0F, emojiBtnY + (btnH - emojiIconSize) / 2.0F, emojiIconSize, emojiIconSize, HudTheme.textColor().withAlpha((int)(180 * inputAnim)));

            float sendBtnX = emojiBtnX + emojiBtnW + 3.0F;
            float sendBtnY = inputY + (INPUT_HEIGHT - btnH) / 2.0F;
            context.drawRoundedRect(sendBtnX, sendBtnY, sendBtnW, btnH, BorderRadius.all(7.0F), Colors.getAccent().withAlpha((int)(162 * inputAnim)));
            float sendIconSize = 10.0F;
            context.drawTexture(BKClientInit.id("sf_symbold/chat/send.png"), sendBtnX + (sendBtnW - sendIconSize) / 2.0F, sendBtnY + (btnH - sendIconSize) / 2.0F, sendIconSize, sendIconSize, ColorRGBA.WHITE.withAlpha((int)(200 * inputAnim)));

            if (this.emojiPickerAnim.getValue() > 0.0F) {
                renderEmojiPicker(context, inputX, inputY, inputAnim);
            }
        }
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        ensureSearchField();

        if (!(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) return;
        if (button == MouseButton.LEFT && mouseY >= this.y && mouseY <= this.y + this.height) {
            for (ClickableRegion region : clickableRegions) {
                if (region.contains((float) mouseX, (float) mouseY)) {
                    ClickEvent action = region.style.getClickEvent();
                    if (action != null && action.getAction() == ClickEvent.Action.RUN_COMMAND
                            && action.getValue().replaceFirst("^/", "").startsWith("bkclient airdrop ")) {
                        handleAirdropClick(action.getValue().replaceFirst("^/", ""));
                    } else mc.currentScreen.handleTextClick(region.style);
                    return;
                }
            }
        }

        float inputY = this.y + this.height + 4.0F;
        float inputX = this.x;
        float inputW = this.width;

        this.searchField.onMouseClicked(mouseX, mouseY, button);

        float emojiBtnW = 22.0F;
        float sendBtnW = 34.0F;
        float btnH = 16.0F;
        float fieldEndX = inputX + inputW - emojiBtnW - sendBtnW - 9.0F;

        float emojiBtnX = fieldEndX + 3.0F;
        float emojiBtnY = inputY + (INPUT_HEIGHT - btnH) / 2.0F;
        if (GuiUtility.isHovered(emojiBtnX, emojiBtnY, emojiBtnW, btnH, mouseX, mouseY)) {
            emojiPickerOpen = !emojiPickerOpen;
            emojiPickerScroll = 0.0F;
            return;
        }

        float sendBtnX = emojiBtnX + emojiBtnW + 3.0F;
        float sendBtnY = inputY + (INPUT_HEIGHT - btnH) / 2.0F;
        if (GuiUtility.isHovered(sendBtnX, sendBtnY, sendBtnW, btnH, mouseX, mouseY)) {
            sendCurrentMessage();
            return;
        }

        if (emojiPickerOpen) {
            float cellSize = 26.0F;
            float gridPad = 4.0F;
            int colsInt = 6;
            float pickerW = colsInt * cellSize + gridPad * 2;
            float pickerH = 130.0F;
            float pickerX = inputX + fieldEndX + 3.0F - inputX - 5.0F;
            float pickerY = inputY - pickerH - 6.0F;
            pickerX = Math.max(2.0F, Math.min(IScaledResolution.sr.getScaledWidth() - pickerW - 2.0F, pickerX));
            pickerY = Math.max(2.0F, pickerY);

            if (!GuiUtility.isHovered(pickerX, pickerY, pickerW, pickerH, mouseX, mouseY)) {
                emojiPickerOpen = false;
            } else if (button == MouseButton.LEFT) {
                float startX = pickerX + gridPad;
                float startY = pickerY + gridPad + emojiPickerScroll;

                int mx = (int) ((mouseX - startX) / cellSize);
                int my = (int) ((mouseY - startY) / cellSize);
                int clickedIndex = my * colsInt + mx;

                int emojiIndex = 0;
                for (EmojiManager.EmojiData emoji : EmojiManager.getAll().values()) {
                    if (emojiIndex == clickedIndex) {
                        ensureSearchField();
                        this.searchField.insertEmoji(emoji);
                        syncVanillaInput();
                        break;
                    }
                    emojiIndex++;
                }
            }
        }
    }

    public boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        ensureSearchField();
        if (!this.searchField.isFocused()) return false;
        // Vanilla owns history and completion; its field callback mirrors the result here.
        if (keyCode == 264 || keyCode == 265 || keyCode == 258 || keyCode == 256
                || ((keyCode == 257 || keyCode == 335) && SuggestionPopup.isOpen())) return false;
        if (keyCode == 257 || keyCode == 335) {
            sendCurrentMessage();
            mc.setScreen(null);
            return true;
        }
        this.searchField.onKeyPressed(keyCode, scanCode, modifiers);
        syncVanillaInput();
        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        ensureSearchField();
        if (!this.searchField.isFocused()) return false;
        this.searchField.charTyped(chr, modifiers);
        syncVanillaInput();
        return true;
    }

    private void syncVanillaInput() {
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen screen) {
            var field = ((dev.bkclient.mixin.accessors.ChatScreenAccessor) screen).bkclient$getChatField();
            String text = searchField.getText();
            if (!field.getText().equals(text)) field.setText(text);
            field.setFocused(true);
        }
    }

    private void sendCurrentMessage() {
        ensureSearchField();
        String text = StringHelper.stripInvalidChars(this.searchField.getText().trim().replace("\u00A7", ""));
        if (text.isEmpty() || mc.player == null) return;
        // The same history is used by vanilla, completion and the custom send button.
        mc.inGameHud.getChatHud().addToMessageHistory(text);
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen screen) {
            ((dev.bkclient.mixin.accessors.ChatScreenAccessor) screen).bkclient$setMessageHistoryIndex(mc.inGameHud.getChatHud().getMessageHistory().size());
        }
        if (!BKClientInit.getInstance().getCommandManager().dispatch(text)) {
            for (int i = 0; i < text.length(); i += 256) {
                String chunk = text.substring(i, Math.min(text.length(), i + 256));
                if (i == 0 && chunk.startsWith("/")) mc.player.networkHandler.sendCommand(chunk.substring(1));
                else mc.player.networkHandler.sendChatMessage(chunk);
            }
            ClientSounds.CHAT_SEND.play(0.2F);
        }
        // Incoming ChatHud messages provide the server's actual echo, without a duplicate bubble.
        this.searchField.clear();
        this.searchField.setFocused(true);
        syncVanillaInput();
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        float inputY = this.y + this.height + 4.0F;
        return GuiUtility.isHovered(this.x, this.y, this.width, this.height + INPUT_HEIGHT + 4.0F, mouseX, mouseY);
    }

    private static float visualWidthStatic(String text, Font font) {
        float emojiW = 9.0F;
        float w = 0.0F;
        int pos = 0;
        char[] singleChar = new char[1];
        while (pos < text.length()) {
            if (text.charAt(pos) == '[') {
                int close = text.indexOf(']', pos);
                if (close != -1) {
                    String code = text.substring(pos + 1, close);
                    if (EmojiManager.getByCode(code) != null) {
                        w += emojiW;
                        pos = close + 1;
                        continue;
                    }
                }
            }
            singleChar[0] = text.charAt(pos);
            w += font.width(new String(singleChar));
            pos++;
        }
        return w;
    }

    private static List<String> wrapText(String text, Font font, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        StringBuilder testBuf = new StringBuilder();
        for (String word : words) {
            if (visualWidthStatic(word, font) > maxWidth) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                }
                testBuf.setLength(0);
                for (int i = 0; i < word.length(); i++) {
                    testBuf.append(word.charAt(i));
                    if (visualWidthStatic(testBuf.toString(), font) > maxWidth && testBuf.length() > 1) {
                        lines.add(testBuf.substring(0, testBuf.length() - 1));
                        testBuf.setLength(0);
                        testBuf.append(word.charAt(i));
                    }
                }
                if (testBuf.length() > 0) currentLine.append(testBuf);
            } else {
                testBuf.setLength(0);
                if (currentLine.length() > 0) {
                    testBuf.append(currentLine).append(' ').append(word);
                } else {
                    testBuf.append(word);
                }
                if (visualWidthStatic(testBuf.toString(), font) > maxWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                    currentLine.append(word);
                } else {
                    currentLine.setLength(0);
                    currentLine.append(testBuf);
                }
            }
        }
        if (currentLine.length() > 0) lines.add(currentLine.toString());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private String extractSender(String text) {
        String username = mc.getSession() != null ? mc.getSession().getUsername() : "";
        if (text.startsWith("<" + username + ">")) return username;
        int bracketEnd = text.indexOf('>');
        if (bracketEnd > 0 && text.startsWith("<")) {
            return text.substring(1, bracketEnd);
        }
        return "";
    }

    private List<ClickSpan> extractClickables(Text text) {
        List<ClickSpan> spans = new ArrayList<>();
        int[] offset = {0};
        text.visit((style, part) -> {
            if (style.getClickEvent() != null) spans.add(new ClickSpan(offset[0], offset[0] + part.length(), style));
            offset[0] += part.length();
            return java.util.Optional.empty();
        }, net.minecraft.text.Style.EMPTY);
        return spans;
    }

    private record ClickSpan(int start, int end, net.minecraft.text.Style style) {}

    @Override
    public boolean show() {
        return Interface.customChatEnabled();
    }

    private EmojiManager.EmojiData isSingleEmojiData(String text) {
        if (text == null || text.isEmpty() || text.charAt(0) != '[') return null;
        int close = text.indexOf(']');
        if (close != text.length() - 1) return null;
        return EmojiManager.getByCode(text.substring(1, close));
    }

    private void renderEmojiPicker(UIContext context, float inputX, float inputY, float inputAnim) {
        float animValue = this.emojiPickerAnim.getValue();
        if (animValue <= 0.0F) return;

        float cellSize = 26.0F;
        float gridPad = 4.0F;
        int colsInt = 6;
        float pickerW = colsInt * cellSize + gridPad * 2;
        float pickerH = 130.0F;
        float emojiBtnW = 22.0F;
        float sendBtnW = 34.0F;
        float fieldEndX = inputX + this.width - emojiBtnW - sendBtnW - 9.0F;
        float emojiBtnX = fieldEndX + 3.0F;
        float pickerX = inputX + fieldEndX + 3.0F - inputX - 5.0F;
        float pickerY = inputY - pickerH - 6.0F;
        pickerX = Math.max(2.0F, Math.min(IScaledResolution.sr.getScaledWidth() - pickerW - 2.0F, pickerX));
        pickerY = Math.max(2.0F, pickerY);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, animValue));
        RenderUtility.scale(context.getMatrices(), pickerX + pickerW / 2.0F, pickerY + pickerH / 2.0F, 0.5F + animValue * 0.5F);
        HudTheme.drawPanel(context, pickerX, pickerY, pickerW, pickerH, animValue, 0.0F, 7.0F);

        ScissorUtility.push(context.getMatrices(), pickerX, pickerY, pickerW, pickerH);
        context.getMatrices().push();
        context.getMatrices().translate(0.0F, emojiPickerScroll, 0.0F);

        float startX = pickerX + gridPad;
        float startY = pickerY + gridPad;
        int i = 0;
        for (EmojiManager.EmojiData emoji : EmojiManager.getAll().values()) {
            int col = i % colsInt;
            int row = i / colsInt;
            float ex = startX + col * cellSize;
            float ey = startY + row * cellSize;
            float emojiSize = 18.0F;
            float eox = ex + (cellSize - emojiSize) / 2.0F;
            float eoy = ey + (cellSize - emojiSize) / 2.0F;
            context.drawTextureSmooth(EmojiTextureLoader.getStandaloneTexture(emoji.texture()), eox, eoy, emojiSize, emojiSize, ColorRGBA.WHITE.withAlpha((int)(255 * animValue)));
            i++;
        }

        context.getMatrices().pop();
        ScissorUtility.pop();

        float totalEmojiH = ((EmojiManager.getAll().size() + colsInt - 1) / colsInt) * cellSize + gridPad * 2;
        float maxScroll = Math.max(0.0F, totalEmojiH - pickerH);

        if (emojiPickerScroll < -maxScroll) emojiPickerScroll = -maxScroll;
        if (emojiPickerScroll > 0.0F) emojiPickerScroll = 0.0F;

        if (maxScroll > 0.0F) {
            float scrollbarWidth = 3.0F;
            float scrollbarX = pickerX + pickerW - scrollbarWidth - 3.0F;
            float scrollbarY = pickerY + gridPad;
            float scrollbarH = pickerH - gridPad * 2;

            float thumbH = scrollbarH * (pickerH / totalEmojiH);
            float thumbY = scrollbarY + (-emojiPickerScroll / maxScroll) * (scrollbarH - thumbH);

            boolean dark = HudTheme.isOld() || BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
                    && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
            ColorRGBA trackColor = dark
                    ? new ColorRGBA(255.0F, 255.0F, 255.0F, 8)
                    : new ColorRGBA(0.0F, 0.0F, 0.0F, 8);
            ColorRGBA thumbColor = dark
                    ? new ColorRGBA(255.0F, 255.0F, 255.0F, 40)
                    : new ColorRGBA(0.0F, 0.0F, 0.0F, 40);
            context.drawRoundedRect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarH, BorderRadius.all(scrollbarWidth / 2.0F), trackColor);
            context.drawRoundedRect(scrollbarX, thumbY, scrollbarWidth, thumbH, BorderRadius.all(scrollbarWidth / 2.0F), thumbColor);
        }

        float triSize = 6.0F;
        float triCenterX = emojiBtnX + emojiBtnW / 2.0F;
        float triY = pickerY + pickerH;
        boolean dark = HudTheme.isOld() || BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
                && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
        ColorRGBA triColor = dark
                ? new ColorRGBA(30.0F, 30.0F, 35.0F, (int)(240 * animValue))
                : new ColorRGBA(240.0F, 240.0F, 245.0F, (int)(240 * animValue));
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        buf.vertex(matrix, triCenterX, triY + triSize, 0.0F).color(triColor.getRGB());
        buf.vertex(matrix, triCenterX - triSize / 2.0F, triY, 0.0F).color(triColor.getRGB());
        buf.vertex(matrix, triCenterX + triSize / 2.0F, triY, 0.0F).color(triColor.getRGB());
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderUtility.end(context.getMatrices());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawTextWithEmojis(UIContext context, Font font, String text, float x, float y,
            ColorRGBA color, float emojiSize, List<ClickSpan> clickables, int lineOffset) {
        float currentX = x;
        for (int pos = 0; pos < text.length();) {
            int end = pos + 1;
            net.minecraft.text.Style style = net.minecraft.text.Style.EMPTY;
            for (ClickSpan span : clickables) {
                if (lineOffset + pos >= span.start && lineOffset + pos < span.end) { style = span.style; break; }
            }
            EmojiManager.EmojiData emoji = null;
            if (text.charAt(pos) == '[') {
                int close = text.indexOf(']', pos);
                if (close >= 0) {
                    emoji = EmojiManager.getByCode(text.substring(pos + 1, close));
                    if (emoji != null) end = close + 1;
                }
            }
            float width;
            if (emoji != null) {
                context.drawTextureSmooth(EmojiTextureLoader.getStandaloneTexture(emoji.texture()), currentX,
                        y + (font.height() - emojiSize) / 2 + 1.5F, emojiSize, emojiSize, ColorRGBA.WHITE.withAlpha(color.getAlpha()));
                width = emojiSize + 1;
            } else {
                // Keep whole runs together so glyph kerning and hit bounds agree.
                while (end < text.length() && text.charAt(end) != '[') {
                    boolean boundary = false;
                    for (ClickSpan span : clickables) if (span.start == lineOffset + end || span.end == lineOffset + end) { boundary = true; break; }
                    if (boundary) break;
                    end++;
                }
                String part = text.substring(pos, end);
                width = font.width(part);
                context.drawText(font, part, currentX, y, style.getClickEvent() == null ? color : Colors.getAccent().withAlpha(color.getAlpha()));
            }
            if (style.getClickEvent() != null) clickableRegions.add(new ClickableRegion(currentX, y, width, font.height() + 1, style));
            currentX += width;
            pos = end;
        }
    }

    private static class ChatMessage {
        final String text;
        final String sender;
        final long timestamp;
        final Animation entryAnim;
        final List<ClickSpan> clickables;
        List<String> cachedLines;
        float cachedHeight = -1.0F;
        float cachedMaxLineW = -1.0F;
        float cachedWrapWidth = Float.NaN;
        Font cachedFont;

        ChatMessage(String text, String sender, long timestamp, List<ClickSpan> clickables) {
            this.text = text;
            this.sender = sender;
            this.timestamp = timestamp;
            this.clickables = clickables;
            this.entryAnim = new Animation(400L, 0.0F, Easing.generate(0.4F, 1.12F, 0.6F, 1.0F));
        }

        List<String> getLines(Font font, float maxTextW) {
            float layoutWidth = Math.max(1.0F, maxTextW);
            if (cachedLines == null || Float.compare(cachedWrapWidth, layoutWidth) != 0 || cachedFont != font) {
                cachedLines = wrapText(text, font, layoutWidth);
                cachedWrapWidth = layoutWidth;
                cachedFont = font;
                cachedHeight = -1.0F;
                cachedMaxLineW = -1.0F;
            }
            return cachedLines;
        }

        float getHeight(Font font, float maxTextW) {
            List<String> lines = getLines(font, maxTextW);
            if (cachedHeight < 0.0F) {
                float emojiLineH = 8.0F + 2.0F;
                float msgH = 0.0F;
                for (String line : lines) {
                    boolean hasEmoji = line.contains("[") && line.contains("]");
                    msgH += hasEmoji ? emojiLineH : font.height() + 2.0F;
                }
                cachedHeight = Math.max(font.height() + MSG_PADDING_V * 2.0F, msgH + MSG_PADDING_V * 2.0F);
            }
            return cachedHeight;
        }

        float getMaxLineW(Font font, float maxTextW) {
            List<String> lines = getLines(font, maxTextW);
            if (cachedMaxLineW < 0.0F) {
                float maxW = 0.0F;
                for (String line : lines) {
                    maxW = Math.max(maxW, visualWidthStatic(line, font));
                }
                cachedMaxLineW = maxW;
            }
            return cachedMaxLineW;
        }
    }

    private static class ClickableRegion {
        final float x, y, width, height;
        final net.minecraft.text.Style style;

        ClickableRegion(float x, float y, float width, float height, net.minecraft.text.Style style) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.style = style;
        }

        boolean contains(float mx, float my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }
    }
}
