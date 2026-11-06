package dev.bkclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import lombok.Generated;
import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.systems.target.TargetSettings;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.Draw3DUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.Utils;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Tracers", category = ModuleCategory.VISUALS, desc = "modules.descriptions.tracers")
public class Tracers
                extends BaseModule {
        private final BooleanSetting lines = new BooleanSetting(this, "lines").enable();
        private final SelectSetting targets = new SelectSetting((SettingsContainer) this,
                        "modules.settings.tracers.targets", "modules.settings.tracers.targets.description");
        private final SelectSetting.Value players = new SelectSetting.Value(this.targets,
                        "modules.settings.tracers.targets.players").select();
        private final SelectSetting.Value animals = new SelectSetting.Value(this.targets,
                        "modules.settings.tracers.targets.animals");
        private final SelectSetting.Value mobs = new SelectSetting.Value(this.targets,
                        "modules.settings.tracers.targets.mobs");
        private final SelectSetting.Value invisibles = new SelectSetting.Value(this.targets,
                        "modules.settings.tracers.targets.invisibles").select();
        private final SelectSetting.Value nakedPlayers = new SelectSetting.Value(this.targets,
                        "modules.settings.tracers.targets.naked_players").select();
        private final SelectSetting.Value friends = new SelectSetting.Value(this.targets,
                        "modules.settings.tracers.targets.friends").select();

        private final ColorSetting color = new ColorSetting(this, "modules.settings.tracers.color")
                        .color(new ColorRGBA(255, 255, 255, 255));
        private final ColorSetting friendColor = new ColorSetting(this, "modules.settings.tracers.friend_color")
                        .color(new ColorRGBA(0, 255, 0, 255));
        private final BooleanSetting ignoreFriends = new BooleanSetting(this,
                        "modules.settings.tracers.ignore_friends");
        private final SliderSetting scale = new SliderSetting(this, "Scale")
                        .min(1.0f).max(5.0f).currentValue(1.0f).step(1.0f);

        private final Map<Entity, TracersAnimation> animations = new HashMap<Entity, TracersAnimation>();

        @Override
        public void onDisable() {
            animations.clear();
            super.onDisable();
        }

        private final EventListener<HudRenderEvent> onHud = event -> {
                if (Tracers.mc.player == null || Tracers.mc.world == null || this.lines.isEnabled()) {
                        if (this.lines.isEnabled() && !this.animations.isEmpty()) {
                                this.animations.clear();
                        }
                        return;
                }
                CustomDrawContext context = event.getContext();
                MatrixStack ms = context.getMatrices();
                TargetSettings targetSettings = new TargetSettings.Builder().targetPlayers(this.players.isSelected())
                                .targetAnimals(this.animals.isSelected()).targetInvisibles(this.invisibles.isSelected())
                                .targetFriends(this.friends.isSelected())
                                .targetNakedPlayers(this.nakedPlayers.isSelected())
                                .targetMobs(this.mobs.isSelected()).build();
                HashSet<Entity> toRemove = new HashSet<Entity>();
                for (Map.Entry<Entity, TracersAnimation> entry : this.animations.entrySet()) {
                        Entity entity = entry.getKey();
                        TracersAnimation animation = entry.getValue();
                        boolean shouldShow = Tracers.mc.world.hasEntity(entity) && entity instanceof LivingEntity
                                        && targetSettings
                                                        .isEntityValid(entity);
                        animation.showing.update(shouldShow);
                        animation.showing.setDuration(500L);
                        if (animation.showing.getValue() != 0.0f || shouldShow)
                                continue;
                        toRemove.add(entity);
                }
                for (Entity entity : Tracers.mc.world.getEntities()) {
                        if (!(entity instanceof LivingEntity)
                                        || !targetSettings
                                                        .isEntityValid(entity)
                                        || this.animations.containsKey(entity))
                                continue;
                        this.animations.put(entity, new TracersAnimation());
                }
                RenderSystem.enableBlend();
                RenderSystem.blendFunc((GlStateManager.SrcFactor) GlStateManager.SrcFactor.SRC_ALPHA,
                                (GlStateManager.DstFactor) GlStateManager.DstFactor.ONE);
                RenderSystem.disableCull();
                ms.push();
                ms.translate(mc.getWindow().getScaledWidth() / 2.0f, mc.getWindow().getScaledHeight() / 2.0f, 0.0f);
                for (Map.Entry<Entity, TracersAnimation> arrow : this.animations.entrySet()) {
                        if (!(arrow.getValue().showing.getValue() > 0.0f))
                                continue;
                        RenderUtility.rotate(ms, 0.0f, 0.0f, this.calculateAngle(arrow.getKey(), event.getTickDelta(),
                                        Tracers.mc.gameRenderer.getCamera()));
                        RenderUtility.scale(ms, 0.0f, 0.0f, 2.0f - arrow.getValue().showing.getValue());
                        float arrowSize = 20.0f * this.scale.getCurrentValue();
                        context.drawTexture(BKClientInit.id("textures/arrow.png"), -arrowSize / 2, 50 - arrowSize / 2, arrowSize, arrowSize,
                                        (BKClientInit.getInstance().getFriendManager()
                                                        .isFriend(arrow.getKey().getName().getString())
                                                                        ? Colors.GREEN
                                                                        : Colors.getAccent())
                                                        .mulAlpha(arrow.getValue().showing.getValue()));
                        RenderUtility.end(ms);
                        RenderUtility.end(ms);
                }
                for (Entity entity : toRemove) {
                        this.animations.remove(entity);
                }
                ms.pop();
                RenderSystem.depthMask((boolean) true);
                RenderSystem.setShaderTexture((int) 0, (int) 0);
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
                RenderSystem.enableCull();
                RenderSystem.enableDepthTest();
        };
        private final EventListener<Render3DEvent> on3DRender = event -> {
                if (Tracers.mc.player == null || Tracers.mc.world == null || !this.lines.isEnabled()) {
                        return;
                }
                MatrixStack matrices = event.getMatrices();
                TargetSettings targetSettings = new TargetSettings.Builder().targetPlayers(this.players.isSelected())
                                .targetAnimals(this.animals.isSelected()).targetInvisibles(this.invisibles.isSelected())
                                .targetFriends(this.friends.isSelected())
                                .targetNakedPlayers(this.nakedPlayers.isSelected())
                                .targetMobs(this.mobs.isSelected()).build();
                RenderUtility.setupRender3D(false);
                RenderSystem.lineWidth(Math.max(1.0f, this.scale.getCurrentValue()));
                RenderSystem.setShader((ShaderProgramKey) ShaderProgramKeys.RENDERTYPE_LINES);
                BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.LINES,
                                VertexFormats.LINES);
                for (Entity entity : Tracers.mc.world.getEntities()) {
                        if (!(entity instanceof LivingEntity)
                                        || !targetSettings
                                                        .isEntityValid(entity))
                                continue;
                        LivingEntity livingEntity = (LivingEntity) entity;
                        boolean isFriend = BKClientInit.getInstance().getFriendManager()
                                        .isFriend(livingEntity.getName().getString());

                        if (isFriend && this.ignoreFriends.isEnabled())
                                continue;

                        Vec3d entityPos = Utils.getInterpolatedPos((Entity) livingEntity, event.getTickDelta());
                        double heightOffset = 0.0;

                        Draw3DUtility.renderLineFromPlayer(matrices, builder,
                                        entityPos.add(0.0, heightOffset, 0.0),
                                        isFriend ? this.friendColor.getColor() : this.color.getColor());
                }
                RenderUtility.buildBuffer(builder);
                RenderSystem.lineWidth(1.0f);
                RenderUtility.endRender3D();
        };

        private float calculateAngle(Entity entity, float partialTicks, Camera camera) {
                Vec3d pos = Utils.getInterpolatedPos(entity, partialTicks)
                                .subtract(camera.getPos());
                double cos = MathHelper
                                .cos((float) ((float) ((double) camera.getYaw()
                                                * (Math.PI / 180))));
                double sin = MathHelper
                                .sin((float) ((float) ((double) camera.getYaw()
                                                * (Math.PI / 180))));
                double rotY = -(pos.z * cos - pos.x * sin);
                double rotX = -(pos.x * cos + pos.z * sin);
                return (float) (Math.atan2(rotY, rotX) * 180.0 / Math.PI - 90.0);
        }

        static class TracersAnimation {
                Animation showing = new Animation(300L, Easing.BAKEK);
                Animation rotating = new Animation(300L, Easing.BAKEK);

                TracersAnimation() {
                }
        }

        @Generated
        public EventListener<HudRenderEvent> getOnHud() {
                return this.onHud;
        }

        @Generated
        public EventListener<Render3DEvent> getOn3DRender() {
                return this.on3DRender;
        }
}
