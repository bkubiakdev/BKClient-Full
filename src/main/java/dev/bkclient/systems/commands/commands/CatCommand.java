package dev.bkclient.systems.commands.commands;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.UUID;
import javax.imageio.ImageIO;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.PreHudRenderEvent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.concurrent.ClientAsyncExecutor;
import dev.bkclient.utility.game.WebUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class CatCommand implements IMinecraft {
   private final Animation animation = new Animation(1000L, Easing.CUBIC_IN_OUT);
   private boolean removing = false;
   private Identifier catTexture = null;
   private boolean loading = false;
   private final EventListener<PreHudRenderEvent> onPreHudRender = event -> {
      if (this.catTexture != null) {
         if (this.animation.getValue() == 1.0 && !this.removing) {
            this.removing = true;
         }

         this.animation.update(this.removing ? 0.0F : 1.0F);
         if (this.removing && this.animation.getValue() == 0.0F) {
            dev.bkclient.utility.render.DrawUtility.releaseTexture(this.catTexture);
            this.catTexture = null;
            this.removing = false;
            return;
         }
         if (this.animation.getValue() != 0.0F || !this.removing) {
            float textureScale = 200.0F;
            float textureX = (mc.getWindow().getScaledWidth() - textureScale) / 2.0F;
            float textureY = (mc.getWindow().getScaledHeight() - textureScale) / 2.0F;
            event.getContext()
               .drawTexture(this.catTexture, textureX, textureY, textureScale, textureScale, Colors.WHITE.withAlpha(255.0F * this.animation.getValue()));
         }
      }
   };

   public CatCommand() {
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }

   public Command command() {
      return CommandBuilder.begin("cat", b -> b.aliases("kitty").desc("commands.cat.description").handler(ctx -> this.loadRandomCatImage())).build();
   }

   private void loadRandomCatImage() {
      if (!this.loading) {
         this.loading = true;
         ClientAsyncExecutor.execute(() -> {
            NativeImage nativeImage = null;
            try {
               String json = WebUtility.fetchJson("https://api.thecatapi.com/v1/images/search");
               String imageUrl = WebUtility.extractImageUrl(json);
               if (imageUrl != null) {
                  java.net.URLConnection connection = new URL(imageUrl).openConnection();
                  connection.setConnectTimeout(5000);
                  connection.setReadTimeout(10000);
                  BufferedImage bufferedImage;
                  try (java.io.InputStream input = connection.getInputStream()) {
                     bufferedImage = ImageIO.read(input);
                  }
                  if (bufferedImage != null) {
                     try { nativeImage = WebUtility.bufferedImageToNativeImage(bufferedImage, false); }
                     finally { bufferedImage.flush(); }
                  }
               }
            } catch (Exception var3) {
               BKClientInit.LOGGER.error("Failed to download cat image", var3);
            }
            NativeImage downloadedImage = nativeImage;
            try {
               mc.execute(() -> {
                  NativeImageBackedTexture texture = null;
                  boolean transferred = false;
                  try {
                     if (downloadedImage == null) return;
                     if (BKClientInit.getInstance().isPanic()) return;
                     if (this.catTexture != null) {
                        dev.bkclient.utility.render.DrawUtility.releaseTexture(this.catTexture);
                        this.catTexture = null;
                     }
                     Identifier id = BKClientInit.id("temp/cat/" + UUID.randomUUID());
                     texture = new NativeImageBackedTexture(downloadedImage);
                     mc.getTextureManager().registerTexture(id, texture);
                     this.catTexture = id;
                     transferred = true;
                     this.animation.update(1.0F);
                     this.removing = false;
                  } catch (RuntimeException failure) {
                     BKClientInit.LOGGER.warn("Cat texture creation failed", failure);
                  } finally {
                     if (!transferred && downloadedImage != null) {
                        if (texture != null) texture.close(); else downloadedImage.close();
                     }
                     this.loading = false;
                  }
               });
            } catch (RuntimeException failure) {
               if (downloadedImage != null) downloadedImage.close();
               this.loading = false;
               BKClientInit.LOGGER.warn("Cat image upload failed", failure);
            }
         });
      }
   }
}
