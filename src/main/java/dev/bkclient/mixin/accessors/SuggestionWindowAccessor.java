package dev.bkclient.mixin.accessors;

import com.mojang.brigadier.suggestion.Suggestion;
import java.util.List;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.util.math.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Opens up the vanilla suggestion popup so the client can draw its own in place of it.
 *
 * <p>Only reading happens through here. Vanilla keeps owning the hard parts — parsing the line,
 * producing the suggestions, moving the selection with the arrow keys, completing on tab — and
 * this exposes just enough of the result to paint it in the client's style instead of Mojang's
 * grey box. Reimplementing the behaviour would mean maintaining a second, worse copy of it.
 */
@Mixin(ChatInputSuggestor.SuggestionWindow.class)
public interface SuggestionWindowAccessor {

    @Accessor("suggestions")
    List<Suggestion> bkclient$getSuggestions();

    /** Index of the highlighted row, absolute within the full suggestion list. */
    @Accessor("selection")
    int bkclient$getSelection();

    /** First row currently scrolled into view; the window shows a fixed number from here. */
    @Accessor("inWindowIndex")
    int bkclient$getInWindowIndex();

    /** Vanilla's own placement, which already accounts for the chat field and screen edges. */
    @Accessor("area")
    Rect2i bkclient$getArea();
}
