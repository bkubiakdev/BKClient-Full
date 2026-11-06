package dev.bkclient.systems.commands.commands;

import dev.bkclient.BKClientInit;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

import dev.bkclient.systems.commands.Command;
import dev.bkclient.systems.commands.CommandBuilder;
import dev.bkclient.systems.commands.CommandContext;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.AttackEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class FakePlayerCommand implements IMinecraft {
   private OtherClientPlayerEntity fakePlayer;
   private float moveForward = 0.0F;
   private float moveStrafe = 0.0F;
   private final EventListener<AttackEvent> onAttackEvent = event -> {
      if (this.fakePlayer != null && event.getEntity() == this.fakePlayer && this.fakePlayer.hurtTime == 0) {
         mc.world
            .playSound(
               mc.player,
               this.fakePlayer.getX(),
               this.fakePlayer.getY(),
               this.fakePlayer.getZ(),
               SoundEvents.ENTITY_PLAYER_HURT,
               SoundCategory.PLAYERS,
               1.0F,
               1.0F
            );
         if (mc.player.fallDistance > 0.0F) {
            mc.world
               .playSound(
                  mc.player,
                  this.fakePlayer.getX(),
                  this.fakePlayer.getY(),
                  this.fakePlayer.getZ(),
                  SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                  SoundCategory.PLAYERS,
                  1.0F,
                  1.0F
               );
         } else {
            mc.world
               .playSound(
                  mc.player,
                  this.fakePlayer.getX(),
                  this.fakePlayer.getY(),
                  this.fakePlayer.getZ(),
                  SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                  SoundCategory.PLAYERS,
                  1.0F,
                  1.0F
               );
         }

         this.fakePlayer.onDamaged(mc.world.getDamageSources().generic());
         this.fakePlayer.setHealth(this.fakePlayer.getHealth() + this.fakePlayer.getAbsorptionAmount() - 1.0F);
         if (this.fakePlayer.isDead()) {
            this.fakePlayer.setHealth(10.0F);
            new EntityStatusS2CPacket(this.fakePlayer, (byte)35).apply(mc.player.networkHandler);
         }
      }
   };
   private final EventListener<KeyPressEvent> onKeyPressEvent = event -> {
      if (this.fakePlayer != null && mc.currentScreen == null) {
         int key = event.getKey();
         int action = event.getAction();
         if (key == 265) {
            this.moveForward = action != 1 && action != 2 ? 0.0F : 1.0F;
         } else if (key == 264) {
            this.moveForward = action != 1 && action != 2 ? 0.0F : -1.0F;
         } else if (key == 263) {
            this.moveStrafe = action != 1 && action != 2 ? 0.0F : 1.0F;
         } else if (key == 262) {
            this.moveStrafe = action != 1 && action != 2 ? 0.0F : -1.0F;
         }
      }
   };
   private final EventListener<ClientPlayerTickEvent> onClientPlayerTickEvent = event -> {
      if (this.fakePlayer != null && mc.player != null) {
         if (this.moveForward == 0.0F && this.moveStrafe == 0.0F) {
            this.fakePlayer.setSprinting(false);
            this.fakePlayer.setVelocity(0.0, this.fakePlayer.getVelocity().y, 0.0);
            this.fakePlayer.limbAnimator.setSpeed(0.0F);
         } else {
            float yaw = mc.player.getYaw();
            double speed = 0.2;
            double motionX = this.moveStrafe * Math.cos(Math.toRadians(yaw)) - this.moveForward * Math.sin(Math.toRadians(yaw));
            double motionZ = this.moveForward * Math.cos(Math.toRadians(yaw)) + this.moveStrafe * Math.sin(Math.toRadians(yaw));
            Vec3d velocity = new Vec3d(motionX * speed, this.fakePlayer.getVelocity().y, motionZ * speed);
            this.fakePlayer.setVelocity(velocity);
            this.fakePlayer.move(MovementType.SELF, velocity);
            this.fakePlayer.setSprinting(true);
         }
      }
   };

   public FakePlayerCommand() {
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }

   public Command command() {
      return CommandBuilder.begin("fakeplayer")
         .aliases("fp")
         .desc("commands.fakeplayer.description")
         .param("action", p -> p.literal("add", "del", "esp"))
         .handler(this::handle)
         .build();
   }

   private void handle(CommandContext ctx) {
      String action = (String)ctx.arguments().getFirst();
      String var3 = action.toLowerCase();
      switch (var3) {
         case "add":
            this.add();
            break;
         case "esp":
            this.previewEsp();
            break;
         case "del":
            this.del();
      }
   }

   public void add() {
      if (mc.player == null || mc.world == null) return;
      this.moveForward = this.moveStrafe = 0.0F;
      if (this.fakePlayer != null) {
         this.fakePlayer.discard();
         this.fakePlayer = null;
      }

      this.fakePlayer = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.fromString("66123666-6666-6666-6666-666666666600"), "FakePlayer"));
      this.fakePlayer.copyPositionAndRotation(mc.player);
      for (net.minecraft.entity.EquipmentSlot slot : net.minecraft.entity.EquipmentSlot.values()) {
         if (slot.isArmorSlot()) this.fakePlayer.equipStack(slot, mc.player.getEquippedStack(slot).copy());
      }
      this.fakePlayer.setStackInHand(Hand.MAIN_HAND, mc.player.getMainHandStack().copy());
      this.fakePlayer.setStackInHand(Hand.OFF_HAND, mc.player.getOffHandStack().copy());
      this.fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 9999, 2));
      this.fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 9999, 4));
      this.fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 9999, 1));
      mc.world.addEntity(this.fakePlayer);
      BKClientInit.getInstance()
         .getNotificationManager()
         .addNotificationOther(
            NotificationType.SUCCESS, Localizator.translate("commands.fakeplayer.success"), Localizator.translate("commands.fakeplayer.added")
         );
   }

   public void previewEsp() {
      if (mc.player == null || mc.world == null) return;
      this.add();
      var forward = Vec3d.fromPolar(0, mc.player.getYaw()).multiply(4.0);
      this.fakePlayer.setPosition(mc.player.getPos().add(forward));
      this.fakePlayer.setYaw(mc.player.getYaw() + 180.0F);
      this.fakePlayer.setHeadYaw(this.fakePlayer.getYaw());
      this.fakePlayer.setBodyYaw(this.fakePlayer.getYaw());
      this.fakePlayer.clearStatusEffects();
      this.fakePlayer.setHealth(14.0F);
      this.fakePlayer.setAbsorptionAmount(4.0F);
      var slots = new net.minecraft.entity.EquipmentSlot[] {
         net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST,
         net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET
      };
      var gear = new net.minecraft.item.Item[] {
         net.minecraft.item.Items.NETHERITE_HELMET, net.minecraft.item.Items.NETHERITE_CHESTPLATE,
         net.minecraft.item.Items.NETHERITE_LEGGINGS, net.minecraft.item.Items.NETHERITE_BOOTS
      };
      for (int i = 0; i < slots.length; i++) {
         var stack = new net.minecraft.item.ItemStack(gear[i]);
         stack.setDamage(stack.getMaxDamage() * (i + 1) / 5);
         var registry = mc.world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
         stack.addEnchantment(registry.getOrThrow(net.minecraft.enchantment.Enchantments.PROTECTION), 4);
         stack.addEnchantment(registry.getOrThrow(net.minecraft.enchantment.Enchantments.UNBREAKING), 3);
         stack.addEnchantment(registry.getOrThrow(net.minecraft.enchantment.Enchantments.MENDING), 1);
         this.fakePlayer.equipStack(slots[i], stack);
      }
      this.fakePlayer.setStackInHand(Hand.MAIN_HAND, new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_SWORD));
      this.fakePlayer.setStackInHand(Hand.OFF_HAND, new net.minecraft.item.ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING, 16));
      var esp = BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.ESP.class);
      esp.enableEquipmentPreview();
   }

   public void del() {
      if (this.fakePlayer == null) {
         BKClientInit.getInstance()
            .getNotificationManager()
            .addNotificationOther(
               NotificationType.ERROR, Localizator.translate("commands.fakeplayer.error"), Localizator.translate("commands.fakeplayer.not_exists")
            );
      } else {
         this.fakePlayer.discard();
         this.fakePlayer = null;
         BKClientInit.getInstance()
            .getNotificationManager()
            .addNotificationOther(
               NotificationType.SUCCESS, Localizator.translate("commands.fakeplayer.success"), Localizator.translate("commands.fakeplayer.removed")
            );
      }
   }
}
