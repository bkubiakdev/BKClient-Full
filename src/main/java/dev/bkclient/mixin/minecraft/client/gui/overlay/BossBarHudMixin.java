package dev.bkclient.mixin.minecraft.client.gui.overlay;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Removals;
import dev.bkclient.utility.game.server.ServerUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarHudMixin implements IMinecraft {
   @Shadow
   @Final
   private Map<UUID, ClientBossBar> field_2060;
   @Unique
   private static final Pattern PVP_TIME_PATTERN = Pattern.compile("(\\d+)\\s*[сc][еe][кk](?=$|\\s|\\p{Punct})", 66);
   private static final String FILTERED_TEXT = "둅ꈣꈃ둄ꈣꈅ";
   @Unique
   private ClientBossBar bkclient$lastBar;
   @Unique
   private net.minecraft.text.Text bkclient$lastBarName;
   @Unique
   private int bkclient$lastBarResult = -1;

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"))
   private void onRenderHead(DrawContext context, CallbackInfo ci) {
      int ctTimer = 0;

      for (ClientBossBar bossBar : this.field_2060.values()) {
         net.minecraft.text.Text barName = bossBar.getName();
         if (barName != null) {
            // Same Text instance → same answer; the string work is redone only when the server changes the name.
            int parsed;
            if (bossBar == this.bkclient$lastBar && barName == this.bkclient$lastBarName) {
               parsed = this.bkclient$lastBarResult;
            } else {
               parsed = -1;
               String name = barName.getString().toLowerCase();
               if (name.contains("бой") || name.contains("pvp")) {
                  parsed = 0;
                  Matcher matcher = PVP_TIME_PATTERN.matcher(barName.getString());
                  if (matcher.find()) {
                     parsed = Integer.parseInt(matcher.group(1));
                  }
               }
               this.bkclient$lastBar = bossBar;
               this.bkclient$lastBarName = barName;
               this.bkclient$lastBarResult = parsed;
            }
            if (parsed >= 0) {
               ctTimer = parsed;
               break;
            }
         }
      }

      ServerUtility.setHasCT(ctTimer > 0);
      ServerUtility.setCtTime(ctTimer);
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      if ((!removals.isEnabled() || !removals.getBossBar().isSelected())
         && BKClientInit.getInstance().getHud().getIsland().isShowing()
         && !this.field_2060.isEmpty()
         && (!removals.isEnabled() || !removals.getBossBar().isSelected())
         && !ServerUtility.isCM()) {
         context.getMatrices().push();
         context.getMatrices().translate(0.0F, BKClientInit.getInstance().getHud().getIsland().getSize().height + 7.0F, 0.0F);
      }
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true)
   private void render(CallbackInfo ci) {
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      if (removals.isEnabled() && removals.getBossBar().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("RETURN"))
   private void onRenderReturn(DrawContext context, CallbackInfo ci) {
      int j = 19 * this.field_2060.size();
      Removals removals = BKClientInit.getInstance().getModuleManager().getModule(Removals.class);
      if ((!removals.isEnabled() || !removals.getBossBar().isSelected())
         && BKClientInit.getInstance().getHud().getIsland().isShowing()
         && !this.field_2060.isEmpty()
         && (!removals.isEnabled() || !removals.getBossBar().isSelected())
         && !ServerUtility.isCM()) {
         context.getMatrices().pop();
      }
   }
}
