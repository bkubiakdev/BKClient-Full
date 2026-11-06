package dev.bkclient.utility.render.sprite;

import net.minecraft.util.Identifier;

public record SpriteFrame(Identifier texture, float u1, float v1, float u2, float v2, int width, int height) {
}
