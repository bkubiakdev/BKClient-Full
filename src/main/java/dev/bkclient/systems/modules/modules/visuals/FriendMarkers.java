package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.BKClientInit;
import lombok.Generated;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.CrystalRenderer;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.Utils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Friend Markers", desc = "Highlights friends", category = ModuleCategory.VISUALS)
public class FriendMarkers extends BaseModule {
   private final ModeSetting setting = new ModeSetting(this, "modules.settings.friends_markers.setting");
   private final ModeSetting.Value heads = new ModeSetting.Value(this.setting, "modules.settings.friends_markers.heads");
   private final ModeSetting.Value sims = new ModeSetting.Value(this.setting, "Sims");
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (this.sims.isSelected()) {
         RenderUtility.setupRender3D(true);
         MatrixStack ms = event.getMatrices();
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cameraPos = camera.getPos();
         ColorRGBA color = new ColorRGBA(52.0F, 199.0F, 89.0F);
         BufferBuilder builder = CrystalRenderer.createBuffer();

         for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (BKClientInit.getInstance().getFriendManager().isFriend(player.getName().getString())) {
               ms.push();
               RenderUtility.prepareMatrices(ms, Utils.getInterpolatedPos(player, event.getTickDelta()));
               float size = 0.1F;
               CrystalRenderer.render(ms, builder, 0.0F, player.getHeight() + 0.4F, 0.0F, size, color.withAlpha(255.0F));
               ms.pop();
            }
         }

         BuiltBuffer built = builder.endNullable();
         if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
         }

         RenderUtility.endRender3D();
      }
   };

   @Generated
   public ModeSetting.Value getHeads() {
      return this.heads;
   }
}
