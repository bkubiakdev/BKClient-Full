package dev.bkclient.systems.modules.modules.visuals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.utility.concurrent.ClientAsyncExecutor;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@ModuleInfo(name = "SkinChanger", category = ModuleCategory.VISUALS, desc = "Apply another player's skin by nickname.")
public class SkinChanger extends BaseModule {
    private static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;

    public final StringSetting nick = new StringSetting(this, "Nick")
            .text("");

    public final ButtonSetting apply = new ButtonSetting(this, "Apply")
            .action(this::fetchAndApply);

    private volatile Identifier skinIdentifier = null;
    private volatile SkinTextures.Model skinModel = SkinTextures.Model.WIDE;
    private volatile boolean loading = false;
    private final AtomicInteger requestGeneration = new AtomicInteger();

    @Override
    public void onDisable() {
        requestGeneration.incrementAndGet();
        loading = false;
        releaseTexture();
        super.onDisable();
    }

    public Identifier getSkinIdentifier() {
        return skinIdentifier;
    }

    public SkinTextures.Model getSkinModel() {
        return skinModel;
    }

    private void fetchAndApply() {
        String name = nick.getText().trim();
        if (name.isEmpty() || loading) return;
        loading = true;
        int generation = requestGeneration.incrementAndGet();
        releaseTexture();

        ClientAsyncExecutor.supply(() -> {
            loadSkin(name, generation);
            return null;
        }).whenComplete((ignored, failure) -> BKClientInit.mc.execute(() -> {
            if (generation != requestGeneration.get()) return;
            loading = false;
            if (failure != null) pushNotification("SkinChanger: request could not be started, try again");
        }));
    }

    private void loadSkin(String name, int generation) {
        try {
            String uuid = resolveUuid(name);
            if (uuid == null) {
                if (generation == requestGeneration.get()) pushNotification("SkinChanger: nick not found");
                return;
            }
            String skinUrl = resolveSkinUrl(uuid);
            if (skinUrl == null) {
                if (generation == requestGeneration.get()) pushNotification("SkinChanger: no skin data");
                return;
            }
            byte[] data = downloadBytes(skinUrl);
            if (data == null || data.length == 0) {
                if (generation == requestGeneration.get()) pushNotification("SkinChanger: download failed");
                return;
            }
            SkinTextures.Model model = detectModel(data);
            Identifier id = Identifier.of("bkclient", "skin/" + UUID.randomUUID());
            BKClientInit.mc.execute(() -> {
                if (generation != requestGeneration.get() || !isEnabled()) return;
                if (registerSkinTexture(id, data)) {
                    skinModel = model;
                    skinIdentifier = id;
                }
            });
        } catch (Exception e) {
            if (generation == requestGeneration.get()) pushNotification("SkinChanger: error");
        }
    }

    private void releaseTexture() {
        Identifier old = skinIdentifier;
        skinIdentifier = null;
        if (old != null) {
            BKClientInit.mc.execute(() -> BKClientInit.mc.getTextureManager().destroyTexture(old));
        }
    }

    private boolean registerSkinTexture(Identifier id, byte[] data) {
        NativeImage image = null;
        try {
            image = NativeImage.read(new ByteArrayInputStream(data));
            BKClientInit.mc.getTextureManager().registerTexture(id, (AbstractTexture) new NativeImageBackedTexture(image));
            return true;
        } catch (Exception ignored) {
            if (image != null) image.close();
            return false;
        }
    }

    private String resolveUuid(String username) {
        try {
            String json = fetchString("https://api.mojang.com/users/profiles/minecraft/" + username);
            if (json == null) return null;
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String raw = obj.get("id").getAsString();
            return raw.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5");
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveSkinUrl(String uuid) {
        try {
            String json = fetchString("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.replace("-", ""));
            if (json == null) return null;
            JsonObject profile = JsonParser.parseString(json).getAsJsonObject();
            String b64 = profile.getAsJsonArray("properties")
                    .get(0).getAsJsonObject()
                    .get("value").getAsString();
            String decoded = new String(Base64.getDecoder().decode(b64));
            JsonObject textures = JsonParser.parseString(decoded)
                    .getAsJsonObject()
                    .getAsJsonObject("textures");
            if (!textures.has("SKIN")) return null;
            return textures.getAsJsonObject("SKIN").get("url").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private SkinTextures.Model detectModel(byte[] data) {
        try (NativeImage img = NativeImage.read(new ByteArrayInputStream(data))) {
            int w = img.getWidth();
            int h = img.getHeight();
            if (w == h) {
                int r = Math.max(w / 64, 1);
                // pixel at (55,20) is transparent in SLIM skins
                int pixel = img.getColorArgb(55 * r, 20 * r);
                int alpha = (pixel >> 24) & 0xFF;
                if (alpha == 0) return SkinTextures.Model.SLIM;
            }
        } catch (Exception ignored) {
        }
        return SkinTextures.Model.WIDE;
    }

    private String fetchString(String url) {
        byte[] data = downloadBytes(url);
        return data == null ? null : new String(data);
    }

    private byte[] downloadBytes(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            try {
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.connect();
                if (conn.getResponseCode() != 200) return null;
                if (conn.getContentLengthLong() > MAX_RESPONSE_BYTES) return null;
                try (InputStream in = new BufferedInputStream(conn.getInputStream());
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        if (n > MAX_RESPONSE_BYTES - out.size()) return null;
                        out.write(buf, 0, n);
                    }
                    return out.toByteArray();
                }
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void pushNotification(String msg) {
        BKClientInit.mc.execute(() ->
                BKClientInit.getInstance().getNotificationManager()
                        .addNotification(NotificationType.ERROR, msg));
    }
}
