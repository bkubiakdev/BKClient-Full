package dev.bkclient.mixin.minecraft.text;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.other.NameProtect;
import dev.bkclient.systems.modules.modules.other.StreamerMode;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
   @ModifyArg(
      method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
      index = 0,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
         ordinal = 0
      )
   )
   private static String patchName(String text) {
      NameProtect nameProtectModule = BKClientInit.getInstance().getModuleManager().getModule(NameProtect.class);
      if (nameProtectModule != null && nameProtectModule.isEnabled()) {
         text = nameProtectModule.patchName(text);
      }
      StreamerMode streamerMode = BKClientInit.getInstance().getModuleManager().getModule(StreamerMode.class);
      if (streamerMode != null && streamerMode.isEnabled()) {
         text = streamerMode.filterText(text);
      }
      return text;
   }
}
