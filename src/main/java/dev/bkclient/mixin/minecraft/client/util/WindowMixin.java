package dev.bkclient.mixin.minecraft.client.util;

import java.io.InputStream;
import java.util.List;
import dev.bkclient.BKClientInit;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Window.class)
public class WindowMixin {
   @Redirect(
      method = "setIcon(Lnet/minecraft/resource/ResourcePack;Lnet/minecraft/client/util/Icons;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Icons;getIcons(Lnet/minecraft/resource/ResourcePack;)Ljava/util/List;")
   )
   public List<InputSupplier<InputStream>> setCustomIcon(Icons instance, ResourcePack resourcePack) {
      if (BKClientInit.getInstance().isPanic()) {
         try {
            return instance.getIcons(resourcePack);
         } catch (Exception var5) {
         }
      }

      return List.of(icon(16), icon(32), icon(48), icon(256));
   }

   private static InputSupplier<InputStream> icon(int size) {
      return () -> BKClientInit.class.getResourceAsStream(
         "/assets/%s/icons/window/icon%dx%d.png".formatted(BKClientInit.MOD_ID, size, size));
   }
}
