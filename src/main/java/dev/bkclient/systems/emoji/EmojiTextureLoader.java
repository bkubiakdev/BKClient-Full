package dev.bkclient.systems.emoji;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class EmojiTextureLoader {
    private static final Map<Identifier, Identifier> STANDALONE_TEXTURES = new HashMap<>();

    public static Identifier getStandaloneTexture(Identifier original) {
        return STANDALONE_TEXTURES.computeIfAbsent(original, EmojiTextureLoader::createStandalone);
    }

    private static Identifier createStandalone(Identifier original) {
        Identifier standaloneId = Identifier.of("bkclient", "emoji_standalone/" + original.getPath().replace("/", "_"));

        try (InputStream stream = MinecraftClient.getInstance().getResourceManager().open(original)) {
            NativeImage image = NativeImage.read(stream);

            MinecraftClient.getInstance().getTextureManager().registerTexture(standaloneId, new NativeImageBackedTexture(image));

            MinecraftClient.getInstance().execute(() -> {
                var tex = MinecraftClient.getInstance().getTextureManager().getTexture(standaloneId);
                if (tex != null) {
                    int glId = tex.getGlId();
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to load emoji texture: " + original, e);
        }

        return standaloneId;
    }

    public static void clear() {
        MinecraftClient client = MinecraftClient.getInstance();
        STANDALONE_TEXTURES.values().forEach(client.getTextureManager()::destroyTexture);
        STANDALONE_TEXTURES.clear();
    }
}
