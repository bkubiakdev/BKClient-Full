package dev.bkclient.mixin.accessors;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the suggestion popup that belongs to the vanilla chat screen.
 *
 * <p>The client's own chat cancels the vanilla screen render, which takes the suggestion window
 * down with it even though the machinery behind it is still running and still producing results.
 * Reaching the object lets the replacement chat draw it itself.
 */
@Mixin(ChatScreen.class)
public interface ChatScreenAccessor {

    @Accessor("messageHistoryIndex")
    void bkclient$setMessageHistoryIndex(int size);

    @Accessor("chatField")
    net.minecraft.client.gui.widget.TextFieldWidget bkclient$getChatField();

    @Accessor("chatInputSuggestor")
    ChatInputSuggestor bkclient$getSuggestor();
}
