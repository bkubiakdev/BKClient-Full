package dev.bkclient.utility.render.ambience;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import dev.bkclient.BKClientInit;
import dev.bkclient.utility.interfaces.IMinecraft;

/** Coarse terrain roof map, refreshed every 500 ms or after moving eight blocks. */
public final class RainHeightmap implements IMinecraft {
    private final Identifier id = BKClientInit.id("dynamic/rain_heightmap");
    private NativeImageBackedTexture texture;
    private boolean registered;
    private float originX;
    private float originZ;
    private float originY;
    private long updatedAt;
    private boolean ready;

    public Identifier id() {
        return this.id;
    }

    public boolean isReady() {
        return this.ready;
    }

    public int texture() {
        return this.texture == null ? 0 : this.texture.getGlId();
    }

    public float x() {
        return this.originX;
    }

    public float z() {
        return this.originZ;
    }

    public float y() {
        return this.originY;
    }

    public void update(double d, double d2, double d3) {
        ClientWorld clientWorld = RainHeightmap.mc.world;
        if (clientWorld == null) {
            return;
        }
        long l = System.currentTimeMillis();
        float f = (float)d - 96.0f;
        float f2 = (float)d3 - 96.0f;
        boolean moved = Math.abs(f - this.originX) > 8.0f || Math.abs(f2 - this.originZ) > 8.0f;
        if (this.ready && !moved && l - this.updatedAt < 500L) {
            return;
        }
        this.updatedAt = l;
        this.originX = f;
        this.originZ = f2;
        this.originY = (float)d2;
        this.ensureTexture();
        NativeImage nativeImage = this.texture.getImage();
        if (nativeImage == null) {
            return;
        }
        for (int i = 0; i < 96; ++i) {
            int n = (int)Math.floor(this.originZ + (float)i * 2.0f);
            for (int j = 0; j < 96; ++j) {
                int n2 = (int)Math.floor(this.originX + (float)j * 2.0f);
                int n3 = clientWorld.isChunkLoaded(n2 >> 4, n >> 4) ? clientWorld.getTopY(Heightmap.Type.MOTION_BLOCKING, n2, n) : (int)originY + 127;
                int n4 = MathHelper.clamp((int)(Math.round((float)n3 - this.originY) + 128), (int)0, (int)255);
                nativeImage.setColorArgb(j, i, 0xFF000000 | n4 << 16 | n4 << 8 | n4);
            }
        }
        this.texture.upload();
        this.ready = true;
    }

    private void ensureTexture() {
        if (this.texture == null) {
            this.texture = new NativeImageBackedTexture(96, 96, false);
        }
        if (!this.registered) {
            mc.getTextureManager().registerTexture(this.id, (AbstractTexture)this.texture);
            this.registered = true;
        }
    }

    public void close() {
        if (registered) mc.getTextureManager().destroyTexture(id);
        else if (texture != null) texture.close();
        texture = null;
        registered = false;
        ready = false;
    }

    public static int range() {
        return 128;
    }
}

