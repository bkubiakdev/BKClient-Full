package dev.bkclient.utility.render.ambience;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;


/** Periodic, multiscale density texture; CPU generation stays off the render thread. */
public final class FogNoiseTexture {
    private NativeImageBackedTexture texture;
    private volatile NativeImage pending;
    private boolean started;
    private volatile boolean closed;

    public int getTexture() {
        if (closed) return 0;
        if (this.texture != null) {
            return this.texture.getGlId();
        }
        NativeImage nativeImage = this.pending;
        if (nativeImage != null) {
            this.pending = null;
            this.texture = new NativeImageBackedTexture(nativeImage);
            this.texture.setFilter(true, false);
            return this.texture.getGlId();
        }
        if (!this.started) {
            this.started = true;
            Thread thread = new Thread(this::generate, "bk-fog-noise");
            thread.setDaemon(true);
            thread.start();
        }
        return 0;
    }

    private void generate() {
        NativeImage nativeImage = new NativeImage(512, 512, false);
        for (int i = 0; i < 512; ++i) {
            for (int j = 0; j < 512; ++j) {
                float f;
                float f2 = 0.0f;
                float f3 = 0.0f;
                float f4 = 0.0f;
                float f5 = 0.6f;
                int n = 16;
                for (int k = 0; k < 4; ++k) {
                    f = FogNoiseTexture.noise(j, i, n, k * 131);
                    f2 += f * f5;
                    f3 += (k < 2 ? f : 0.5f) * f5;
                    f4 += f5;
                    f5 *= 0.45f;
                    n <<= 1;
                }
                float f6 = 0.0f;
                f = 0.0f;
                f5 = 0.6f;
                n = 16;
                for (int k = 0; k < 3; ++k) {
                    f6 += FogNoiseTexture.noise(j, i, n, 7919 + k * 131) * f5;
                    f += f5;
                    f5 *= 0.45f;
                    n <<= 1;
                }
                nativeImage.setColorArgb(j, i, 0xFF000000 | FogNoiseTexture.channel(f2 / f4) << 16 | FogNoiseTexture.channel(f3 / f4) << 8 | FogNoiseTexture.channel(f6 / f));
            }
        }
        synchronized (this) {
            if (closed) nativeImage.close(); else this.pending = nativeImage;
        }
    }

    private static float noise(int n, int n2, int n3, int n4) {
        float f = (float)(n * n3) / 512.0f;
        float f2 = (float)(n2 * n3) / 512.0f;
        int n5 = (int)Math.floor(f);
        int n6 = (int)Math.floor(f2);
        float f3 = FogNoiseTexture.smooth(f - (float)n5);
        float f4 = FogNoiseTexture.smooth(f2 - (float)n6);
        int n7 = n3 - 1;
        int n8 = n5 & n7;
        int n9 = n5 + 1 & n7;
        int n10 = n6 & n7;
        int n11 = n6 + 1 & n7;
        float f5 = FogNoiseTexture.lerp(FogNoiseTexture.hash(n8, n10, n4), FogNoiseTexture.hash(n9, n10, n4), f3);
        float f6 = FogNoiseTexture.lerp(FogNoiseTexture.hash(n8, n11, n4), FogNoiseTexture.hash(n9, n11, n4), f3);
        return FogNoiseTexture.lerp(f5, f6, f4);
    }

    private static float hash(int n, int n2, int n3) {
        int n4 = n * 374761393 + n2 * 668265263 + n3 * 1274126177;
        n4 = (n4 ^ n4 >>> 13) * 1274126177;
        n4 ^= n4 >>> 16;
        return (float)(n4 & 0xFFFFFF) / 1.6777215E7f;
    }

    private static float smooth(float f) {
        return f * f * (3.0f - 2.0f * f);
    }

    private static float lerp(float f, float f2, float f3) {
        return f + (f2 - f) * f3;
    }

    private static int channel(float f) {
        return Math.max(0, Math.min(255, Math.round(f * 255.0f)));
    }
    public synchronized void close() {
        closed = true;
        if (texture != null) { texture.close(); texture = null; }
        if (pending != null) { pending.close(); pending = null; }
    }
}
