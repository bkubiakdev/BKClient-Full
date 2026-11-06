package dev.bkclient.mixin.accessors;

import net.minecraft.client.gui.screen.ChatInputSuggestor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reaches the popup the suggestor is currently showing, or null when there is nothing to show. */
@Mixin(ChatInputSuggestor.class)
public interface ChatInputSuggestorAccessor {

    @Accessor("window")
    ChatInputSuggestor.SuggestionWindow bkclient$getWindow();

    /** How many rows vanilla decided fit on screen at once. */
    @Accessor("maxSuggestionSize")
    int bkclient$getMaxSuggestionSize();
}
