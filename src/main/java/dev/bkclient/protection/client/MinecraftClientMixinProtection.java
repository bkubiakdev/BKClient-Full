package dev.bkclient.protection.client;

import dev.bkclient.BKClientInit;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class MinecraftClientMixinProtection {
   public static void init() {
      BKClientInit.INSTANCE.initialize();
   }

   public static void shutdown() {
      BKClientInit.INSTANCE.shutdown();
   }

   public static void updateTitle(CallbackInfoReturnable<String> cir) {
      if (!BKClientInit.INSTANCE.isPanic()) {
         cir.setReturnValue("BKClient " + BKClientInit.VERSION);
      }
   }
}
