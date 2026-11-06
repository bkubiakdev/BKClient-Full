package dev.bkclient.larp.feature;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.EntityEffectParticleEffect;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public final class PoisonPencil {
    private static final int POISON_COLOR = 5149489;
    private static final int PARTICLES = 12;

    private PoisonPencil() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) {
                PoisonPencil.onClientAttack(entity);
            }
            return ActionResult.PASS;
        });
    }

    private static void onClientAttack(Entity target) {
        ClientWorld NarrationMessageBuilder;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || !((NarrationMessageBuilder = client.world) instanceof ClientWorld)) {
            return;
        }
        ClientWorld world = NarrationMessageBuilder;
        if (!(target instanceof LivingEntity) || target == player) {
            return;
        }
        if (!Disguise.hasAbility(player.getMainHandStack(), Disguise.Ability.POISON_PENCIL)) {
            return;
        }
        PoisonPencil.spawnPoison(world, target.getX(), target.getY(), target.getZ(), target.getWidth(), target.getHeight());
        Titles.playSound(SoundEvents.ENTITY_SPLASH_POTION_BREAK, 1.0f, 0.8f);
        PoisonPencil.showMessage(target);
    }

    private static void spawnPoison(ClientWorld world, double x, double y, double z, float width, float height) {
        EntityEffectParticleEffect poison = EntityEffectParticleEffect.create((ParticleType)ParticleTypes.ENTITY_EFFECT, (int)5149489);
        for (int i = 0; i < 12; ++i) {
            if (dev.bkclient.larp.LarpRuntime.effects()) world.addParticle((ParticleEffect)poison, x + (world.random.nextDouble() - 0.5) * (double)width, y + world.random.nextDouble() * (double)height, z + (world.random.nextDouble() - 0.5) * (double)width, 0.0, 0.0, 0.0);
        }
    }

    private static void showMessage(Entity target) {
        Style white = Style.EMPTY.withColor(Formatting.WHITE).withItalic(Boolean.valueOf(false));
        MutableText message = Text.empty().append(LegacyText.parse("&7Zatru\u0142e\u015b ")).append((Text)Text.empty().setStyle(white).append(target.getName())).append(LegacyText.parse(" &azatrutym o\u0142\u00f3wkiem&7!"));
        Titles.show((Text)Text.empty(), (Text)message);
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { }
}
