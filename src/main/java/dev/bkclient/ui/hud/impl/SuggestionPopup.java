package dev.bkclient.ui.hud.impl;

import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.mixin.accessors.ChatInputSuggestorAccessor;
import dev.bkclient.mixin.accessors.ChatScreenAccessor;
import dev.bkclient.mixin.accessors.SuggestionWindowAccessor;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;

/**
 * Draws the command suggestions as a glass card instead of Mojang's grey box.
 *
 * <p>Purely a repaint. Vanilla still parses the line, produces the list, moves the selection and
 * completes on tab; this reads the result and renders it in the client's own language — the same
 * panel, rounding and accent the rest of the interface uses.
 */
public final class SuggestionPopup {

    private static final float ROW_HEIGHT = 11.0F;
    private static final float PADDING_X = 6.0F;
    private static final float PADDING_Y = 4.0F;
    private static final float TEXT_SIZE = 7.5F;
    private static final float MIN_WIDTH = 60.0F;

    /** Grows the card in from slightly small, the way a sheet appears rather than blinks on. */
    private static final Animation APPEAR = new Animation(180L, 0.0F, Easing.BAKEK_SIZE);

    /**
     * When the popup was last actually showing.
     *
     * <p>Vanilla drops its window for a frame or two whenever it re-parses the line, so treating
     * every gap as "closed" restarted the grow animation constantly and made the card blink.
     */
    private static long lastVisibleMs;

    private SuggestionPopup() {
    }

    /**
     * Whether the suggestion list is currently up.
     *
     * <p>Callers use this to leave the arrow keys alone: with suggestions open the arrows belong
     * to the list, and only otherwise do they mean command history.
     */
    public static boolean isOpen() {
        if (!(MinecraftClient.getInstance().currentScreen instanceof ChatScreen screen)) {
            return false;
        }
        ChatInputSuggestor suggestor = ((ChatScreenAccessor) screen).bkclient$getSuggestor();
        if (suggestor == null) {
            return false;
        }
        ChatInputSuggestor.SuggestionWindow window = ((ChatInputSuggestorAccessor) suggestor).bkclient$getWindow();
        if (window == null) {
            return false;
        }
        List<Suggestion> suggestions = ((SuggestionWindowAccessor) window).bkclient$getSuggestions();
        return suggestions != null && !suggestions.isEmpty();
    }

    /** Fades the card out only once the gap looks deliberate rather than a re-parse. */
    private static boolean closed() {
        if (System.currentTimeMillis() - lastVisibleMs > 150L) {
            APPEAR.setValue(0.0F);
        }
        return false;
    }

    /**
     * Renders the popup if one is open.
     *
     * @return false when the suggestor has nothing showing, so the caller knows nothing was drawn
     */
    public static boolean render(CustomDrawContext context, ChatInputSuggestor suggestor) {
        if (suggestor == null) {
            return closed();
        }
        ChatInputSuggestor.SuggestionWindow window = ((ChatInputSuggestorAccessor) suggestor).bkclient$getWindow();
        if (window == null) {
            return closed();
        }

        SuggestionWindowAccessor access = (SuggestionWindowAccessor) window;
        List<Suggestion> suggestions = access.bkclient$getSuggestions();
        if (suggestions == null || suggestions.isEmpty()) {
            return closed();
        }

        int visibleRows = Math.min(suggestions.size(), ((ChatInputSuggestorAccessor) suggestor).bkclient$getMaxSuggestionSize());
        int firstRow = Math.max(0, Math.min(access.bkclient$getInWindowIndex(), suggestions.size() - visibleRows));

        Font font = Fonts.MEDIUM.getFont(TEXT_SIZE);
        float widest = MIN_WIDTH;
        for (int i = 0; i < visibleRows; i++) {
            widest = Math.max(widest, font.width(suggestions.get(firstRow + i).getText()));
        }

        float width = widest + PADDING_X * 2.0F;
        float height = visibleRows * ROW_HEIGHT + PADDING_Y * 2.0F;

        // Vanilla already worked out somewhere sensible for this, clear of the chat field and
        // inside the screen, so its rectangle is reused rather than second-guessed.
        float x = access.bkclient$getArea().getX() - PADDING_X;
        float y = access.bkclient$getArea().getY() - PADDING_Y;

        // The custom chat's field sits at the bottom of the screen, exactly where vanilla would
        // lay its popup. That let the suggestion card cover the line being typed. Lift it clear of
        // the input so the text stays readable while the list is open.
        MinecraftClient client = MinecraftClient.getInstance();
        int scaledHeight = client.getWindow().getScaledHeight();
        float inputClearance = 28.0F;
        float maxBottom = scaledHeight - inputClearance;
        if (y + height > maxBottom) {
            y = maxBottom - height;
        }
        if (y < 4.0F) {
            y = 4.0F;
        }

        lastVisibleMs = System.currentTimeMillis();
        float appear = APPEAR.update(1.0F);
        if (appear <= 0.01F) {
            return true;
        }

        context.pushMatrix();
        // Scale about the bottom left, the corner nearest the text being typed, so the card
        // unfolds away from the caret rather than out of its own middle.
        context.getMatrices().translate(x, y + height, 0.0F);
        context.getMatrices().scale(appear, appear, 1.0F);
        context.getMatrices().translate(-x, -(y + height), 0.0F);

        context.drawClientRect(x, y, width, height, appear, 0.0F, 1.0F);

        // Selection reads as brightness alone. A filled highlight behind the row was louder than
        // anything else on screen, and the card already separates itself from the world well
        // enough that the row does not need its own background to be found.
        int selected = access.bkclient$getSelection();
        for (int i = 0; i < visibleRows; i++) {
            int index = firstRow + i;
            context.drawText(
                    font,
                    suggestions.get(index).getText(),
                    x + PADDING_X,
                    y + PADDING_Y + i * ROW_HEIGHT + 1.5F,
                    index == selected
                            ? Colors.WHITE.withAlpha(255.0F * appear)
                            : HudTheme.textColor().withAlpha(255.0F * 0.45F * appear)
            );
        }

        context.popMatrix();
        return true;
    }
}
