package dev.bkclient.mixin.minecraft.client.network;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.game.PickupEvent;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.modules.modules.visuals.XRay;
import dev.bkclient.utility.BlockEntityTracker;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.rotations.Rotation;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler implements IMinecraft {
   @Unique
   private Rotation oldRotation = Rotation.ZERO;

   protected ClientPlayNetworkHandlerMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
      super(client, connection, connectionState);
   }

   @Inject(
      method = "onItemPickupAnimation(Lnet/minecraft/network/packet/s2c/play/ItemPickupAnimationS2CPacket;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getEntityById(I)Lnet/minecraft/entity/Entity;", ordinal = 0)
   )
   private void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo info) {
      Entity itemEntity = this.client.world.getEntityById(packet.getEntityId());
      Entity entity = this.client.world.getEntityById(packet.getCollectorEntityId());
      if (itemEntity instanceof ItemEntity && entity == this.client.player) {
         BKClientInit.getInstance().getEventManager().triggerEvent(new PickupEvent(((ItemEntity)itemEntity).getStack(), packet.getStackAmount()));
      }
   }

   @Inject(method = "onBlockEntityUpdate(Lnet/minecraft/network/packet/s2c/play/BlockEntityUpdateS2CPacket;)V", at = @At("TAIL"))
   private void onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet, CallbackInfo ci) {
      if (mc.world != null) {
         BlockPos pos = packet.getPos();
         BlockEntity blockEntity = mc.world.getBlockEntity(pos);
         BlockEntityTracker.put(blockEntity);
      }
   }

   @Inject(method = "onChunkData(Lnet/minecraft/network/packet/s2c/play/ChunkDataS2CPacket;)V", at = @At("TAIL"))
   private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
      if (ClientPlayNetworkHandlerMixin.mc.world != null) {
         WorldChunk chunk = ClientPlayNetworkHandlerMixin.mc.world.getChunk(packet.getChunkX(), packet.getChunkZ());
         BlockEntityTracker.putAll(chunk.getBlockEntities().values());
         XRay xray = BKClientInit.getInstance().getModuleManager().getModule(XRay.class);
         MinecraftClient mc = MinecraftClient.getInstance();
         if (xray != null && xray.isEnabled() && mc.world != null) {
            xray.scanChunkAsync(chunk);
         }
      }
   }

   @Inject(method = "onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V", at = @At("TAIL"))
   private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
      BlockEntityTracker.clear();
      BKClientInit.getInstance().getEventManager().triggerEvent(new WorldChangeEvent());
   }

   @Inject(method = "onPlayerPositionLook(Lnet/minecraft/network/packet/s2c/play/PlayerPositionLookS2CPacket;)V", at = @At("HEAD"))
   public void savePlayerRotation(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
      if (mc.player != null) {
         this.oldRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
      }
   }

   @Inject(method = "onPlayerPositionLook(Lnet/minecraft/network/packet/s2c/play/PlayerPositionLookS2CPacket;)V", at = @At("RETURN"))
   public void modifyPlayerRotation(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
      if (mc.player != null) {
         new Rotation(packet.change().yaw(), packet.change().pitch());
      }
   }
}
