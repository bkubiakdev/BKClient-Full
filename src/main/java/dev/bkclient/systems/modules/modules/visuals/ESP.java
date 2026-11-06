package dev.bkclient.systems.modules.modules.visuals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.gradient.impl.VerticalGradient;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.GameTickEvent;
import dev.bkclient.systems.modules.modules.visuals.esp.SimpleNametags;
import dev.bkclient.systems.event.impl.render.PreHudRenderEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.modules.combat.AntiBot;
import dev.bkclient.utility.game.FakePlayerEntity;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.LinkedModeSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.systems.modules.modules.visuals.esp.GradientBoxRenderer;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.BKClientInit;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.render.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "ESP", category = ModuleCategory.VISUALS, desc = "modules.descriptions.esp")
public class ESP extends BaseModule implements IMinecraft, IScaledResolution {
    private static final float BAR_WIDTH = 1.0F;
    private static final float BORDER = 0.3F;
    private static final float BAR_GAP = 1.0F;
    private static final ColorRGBA BAR_BORDER = new ColorRGBA(0, 0, 0, 180);
    private static final ColorRGBA BAR_BG = new ColorRGBA(0, 0, 0, 120);
    private static final ColorRGBA BAR_OVERLAY = new ColorRGBA(0, 0, 0, 210);

    private final dev.bkclient.systems.setting.settings.ButtonSetting openEditor =
        new dev.bkclient.systems.setting.settings.ButtonSetting(this, "modules.settings.esp.open_menu")
            .action(() -> mc.setScreen(new dev.bkclient.ui.menu.esp.EspEditorScreen(this, mc.currentScreen)));

    @Override public List<dev.bkclient.systems.setting.Setting> getDisplaySettings() { return List.of(openEditor); }
    public SimpleNametags getSimpleNametags() { return simpleNametags; }
    public ColorRGBA previewBoxColor(int tab) { var p=profile(tab);return p==null?(boxThemeSync.isEnabled()?Colors.getAccent():boxColor.getColor()):p.color(); }
    public boolean previewBoxEnabled(int tab) { var p=profile(tab);return p==null?box.isEnabled():p.box.isEnabled(); }
    public boolean previewFillEnabled(int tab) { var p=profile(tab);return p==null?gradientFill.isSelected():p.fill.isSelected(); }
    public boolean previewOutlineEnabled(int tab) { var p=profile(tab);return p==null?gradientOutline.isSelected():p.outline.isSelected(); }
    public boolean previewGlowEnabled(int tab) { var p=profile(tab);return p==null?glow.isEnabled():p.glow.isEnabled(); }
    public ColorRGBA previewGlowColor(int tab) { var p=profile(tab);return p==null?glowColor.getColor():p.glowColor.getColor(); }
    public float previewNameSize(int tab) { var p=profile(tab);return p==null?nameSize.getCurrentValue():p.nameSize.getCurrentValue(); }
    public float previewLabelScale(int tab) { var p=profile(tab);return p==null?espScale.getCurrentValue():p.scale.getCurrentValue(); }
    public boolean previewNameEnabled(int tab) { var p=profile(tab);return p==null?name.isEnabled():p.name.isEnabled(); }
    private SelectSetting.Value target(int tab){return switch(tab){case 1->targetMobs;case 2->targetAnimals;case 3->targetItems;default->targetPlayers;};}
    public boolean targetEnabled(int tab){return target(tab).isSelected();}
    public void setTargetEnabled(int tab,boolean enabled){if(target(tab).isSelected()!=enabled)target(tab).toggle();}
    public List<dev.bkclient.systems.setting.Setting> editorSettings(int tab) {
        if(tab!=0){var result=new ArrayList<>(profile(tab).settings);if(tab==3)result.add(itemDisplayMode);return result;}
        return getSettings().stream().filter(setting->setting!=openEditor&&setting!=objects&&setting!=itemDisplayMode
            && !setting.getName().startsWith("esp.mobs.")&&!setting.getName().startsWith("esp.animals.")&&!setting.getName().startsWith("esp.drops.")).toList();
    }

    private final SelectSetting objects = new SelectSetting(this, "objects");

    private final BooleanSetting box = new BooleanSetting(this, "esp.box").enable();
    private final ModeSetting boxMode = new ModeSetting(this, "esp.box.mode", () -> !this.box.isEnabled());
    private final ModeSetting.Value modeBoxes = new ModeSetting.Value(this.boxMode, "esp.box.mode.boxes").select();
    private final BooleanSetting boxThemeSync = new BooleanSetting(this, "esp.box.theme_sync",
        () -> !this.box.isEnabled() || !this.modeBoxes.isSelected()).enable();
    private final SelectSetting boxGradient = new SelectSetting(this, "esp.box.gradient",
        () -> !this.box.isEnabled() || !this.modeBoxes.isSelected());
    private final SelectSetting.Value gradientFill = new SelectSetting.Value(this.boxGradient, "esp.boxes.mode.fill").select();
    private final SelectSetting.Value gradientOutline = new SelectSetting.Value(this.boxGradient, "esp.boxes.mode.outline").select();
    private final ColorSetting boxColor = new ColorSetting(this, "esp.box.color",
        () -> !this.box.isEnabled() || this.modeBoxes.isSelected() && this.boxThemeSync.isEnabled())
       .color(new ColorRGBA(154.0F, 93.0F, 255.0F));
    private final ModeSetting equipmentStyle = new ModeSetting(this, "esp.equipment.style");
    private final ModeSetting.Value equipmentDetailed = new ModeSetting.Value(this.equipmentStyle, "esp.equipment.detailed").select();
    private final ModeSetting.Value equipmentSimple = new ModeSetting.Value(this.equipmentStyle, "esp.equipment.simple");
    private final SliderSetting espScale = new SliderSetting(this, "esp.scale", () -> this.equipmentSimple.isSelected())
        .min(0.5F).max(2.0F).step(0.05F).currentValue(1.0F);
    private final BooleanSetting name = new BooleanSetting(this, "esp.name").enable();
    public boolean isNameEnabled() { return name.isEnabled(); }
    private final SliderSetting nameSize = new SliderSetting(this, "esp.name.size", () -> !this.name.isEnabled() || this.equipmentSimple.isSelected())
        .min(0.03F).max(0.30F).step(0.01F).currentValue(0.10F);
    private final ModeSetting itemDisplayMode = new ModeSetting(this, "esp.item.mode");
    private final ModeSetting.Value itemIndividual = new ModeSetting.Value(this.itemDisplayMode, "esp.item.mode.individual").select();
    private final ModeSetting.Value itemCount = new ModeSetting.Value(this.itemDisplayMode, "esp.item.mode.count");
    private final ModeSetting.Value itemColumn = new ModeSetting.Value(this.itemDisplayMode, "esp.item.mode.column");
    private final BooleanSetting hp = new BooleanSetting(this, "esp.hp", () -> this.equipmentSimple.isSelected()).enable();
    private final BooleanSetting items = new BooleanSetting(this, "esp.items", () -> this.equipmentSimple.isSelected());
    private final BooleanSetting armor = new BooleanSetting(this, "esp.armor", () -> this.equipmentSimple.isSelected());
    private final BooleanSetting itemCountLabel = new BooleanSetting(this, "esp.item_count", () -> this.equipmentSimple.isSelected() || !this.armor.isEnabled() && !this.items.isEnabled()).enable();
    private final BooleanSetting durability = new BooleanSetting(this, "esp.durability", () -> this.equipmentSimple.isSelected() || !this.armor.isEnabled() && !this.items.isEnabled());
    private final ModeSetting durabilityStyle = new ModeSetting(this, "esp.durability.style",
        () -> this.equipmentSimple.isSelected() || !this.durability.isEnabled() || !this.armor.isEnabled() && !this.items.isEnabled());
    private final ModeSetting.Value durabilityPercent = new ModeSetting.Value(this.durabilityStyle, "esp.durability.percent").select();
    private final ModeSetting.Value durabilityBar = new ModeSetting.Value(this.durabilityStyle, "esp.durability.bar");
    private final BooleanSetting enchantments = new BooleanSetting(this, "esp.enchantments", () -> this.equipmentSimple.isSelected() || !this.armor.isEnabled() && !this.items.isEnabled());
    private final BooleanSetting glow = new BooleanSetting(this, "esp.glow");
    private final ColorSetting glowColor = new ColorSetting(this, "esp.glow.color", () -> !this.glow.isEnabled())
        .color(new ColorRGBA(0, 255, 0));
    private final SliderSetting maxDistance = new SliderSetting(this, "esp.max_distance")
        .min(8.0F).max(192.0F).step(1.0F).currentValue(96.0F);

    private final SelectSetting.Value targetPlayers;
    private final SelectSetting.Value targetAnimals;
    private final SelectSetting.Value targetMobs;
    private final SelectSetting.Value targetItems;

    /** Separate serialized controls for each non-player target category. */
    private final class TargetOptions {
        final List<dev.bkclient.systems.setting.Setting> settings;
        final BooleanSetting box,theme,name,glow;
        final ColorSetting boxColor,glowColor;
        final SliderSetting range,nameSize,scale;
        final SelectSetting gradient;
        final SelectSetting.Value fill,outline;
        TargetOptions(String key) {
            int start=getSettings().size();
            box=new BooleanSetting(ESP.this,key+"box").enable();
            theme=new BooleanSetting(ESP.this,key+"box.theme_sync",()->!box.isEnabled()).enable();
            LinkedModeSetting.toggle(ESP.this,key+"box.source",theme,"Theme","Custom");
            boxColor=new ColorSetting(ESP.this,key+"box.color",()->!box.isEnabled()||theme.isEnabled()).color(new ColorRGBA(154,93,255));
            gradient=new SelectSetting(ESP.this,key+"box.gradient",()->!box.isEnabled());
            fill=new SelectSetting.Value(gradient,"esp.boxes.mode.fill").select();
            outline=new SelectSetting.Value(gradient,"esp.boxes.mode.outline").select();
            glow=new BooleanSetting(ESP.this,key+"glow");
            glowColor=new ColorSetting(ESP.this,key+"glow.color",()->!glow.isEnabled()).color(new ColorRGBA(0,255,0));
            range=new SliderSetting(ESP.this,key+"max_distance").min(8).max(192).step(1).currentValue(96);
            name=new BooleanSetting(ESP.this,key+"name").enable();
            nameSize=new SliderSetting(ESP.this,key+"name.size",()->!name.isEnabled()).min(.03F).max(.3F).step(.01F).currentValue(.1F);
            scale=new SliderSetting(ESP.this,key+"scale").min(.5F).max(2).step(.05F).currentValue(1);
            settings=List.copyOf(getSettings().subList(start,getSettings().size()));
        }
        ColorRGBA color(){return theme.isEnabled()?Colors.getAccent():boxColor.getColor();}
    }
    private final TargetOptions mobOptions=new TargetOptions("esp.mobs.");
    private final TargetOptions animalOptions=new TargetOptions("esp.animals.");
    private final TargetOptions itemOptions=new TargetOptions("esp.drops.");
    private TargetOptions profile(int tab){return switch(tab){case 1->mobOptions;case 2->animalOptions;case 3->itemOptions;default->null;};}
    private int tab(Entity entity){return entity instanceof PlayerEntity?0:entity instanceof ItemEntity?3:entity instanceof AnimalEntity?2:1;}

    private final SimpleNametags simpleNametags = new SimpleNametags(this);
    private final EventListener<GameTickEvent> onTick = event -> this.simpleNametags.tick();

    private record NameplateHit(UUID player, float left, float top, float right, float bottom) {}
    private final List<NameplateHit> nameplateHits = new ArrayList<>();
    private long nameplateFrame;
    private int nameplateWidth, nameplateHeight;

    private final Set<UUID> bkClientUsers = new HashSet<>();

    private final float[] projectScratch = new float[2];
    private final List<Entity> frameTargets = new ArrayList<>();
    private boolean frameTargetsValid;
    @SuppressWarnings("unchecked")
    private final List<Entity>[] categoryTargets = new List[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
    private final java.util.Map<net.minecraft.entity.EntityType<?>, ItemStack> spawnEggs = new java.util.IdentityHashMap<>();

    /** Same egg as {@code new ItemStack(SpawnEggItem.forEntity(type))}, created once per entity type. */
    private ItemStack spawnEgg(net.minecraft.entity.EntityType<?> type) {
        return spawnEggs.computeIfAbsent(type, t -> {
            SpawnEggItem egg = SpawnEggItem.forEntity(t);
            return egg != null ? new ItemStack(egg) : ItemStack.EMPTY;
        });
    }

    private final EventListener<Render3DEvent> onRender3D = event -> {
        nameplateHits.clear();
        nameplateFrame = System.nanoTime();
        nameplateWidth = mc.getWindow().getScaledWidth();
        nameplateHeight = mc.getWindow().getScaledHeight();
        frameTargetsValid = false;
        if (mc.player == null || mc.world == null) return;
        if (isSimpleNametagActive()) this.simpleNametags.render(event);
        List<Entity> targets=collectTargets();
        // The world pass and the HUD labels of the same frame see the same world state and
        // tick delta, so the HUD pass reuses this list instead of scanning all entities again.
        frameTargets.clear();
        frameTargets.addAll(targets);
        frameTargetsValid = true;
        // One partition pass instead of four stream filters; per-category order is unchanged.
        for (List<Entity> group : categoryTargets) group.clear();
        for (Entity entity : targets) categoryTargets[tab(entity)].add(entity);
        for(int category=0;category<4;category++) {
            if(!previewBoxEnabled(category))continue;
            List<Entity> group=categoryTargets[category];
            if(!group.isEmpty())GradientBoxRenderer.render(event,group,previewBoxColor(category),previewFillEnabled(category),previewOutlineEnabled(category));
        }
    };

    public boolean isSimpleNametagStyle() { return this.equipmentSimple.isSelected(); }

    public boolean isSimpleNametagActive() {
        return this.isEnabled() && isSimpleNametagStyle() && this.name.isEnabled();
    }

    public boolean isTargetEntity(Entity entity) { return isTarget(entity); }
    public float getTargetRange() { return maxDistance.getCurrentValue(); }

    public static boolean hidesVanillaName(PlayerEntity player) {
        if (instance == null || !instance.isEnabled() || !instance.name.isEnabled()
                || !instance.isTarget(player)) return false;
        return !instance.equipmentSimple.isSelected() || instance.simpleNametags.accepts(player);
    }

    public List<Entity> getNametagEntities() {
        if (!isEnabled() || !name.isEnabled()) return List.of();
        return equipmentSimple.isSelected() ? simpleNametags.getEntityList() : collectTargets();
    }

    @Override
    public void onDisable() { onWorldChanged(); }

    public void onWorldChanged() {
        nameplateHits.clear();
        nameplateFrame = 0;
        frameTargets.clear();
        frameTargetsValid = false;
        for (List<Entity> group : categoryTargets) group.clear();
        simpleNametags.clearCaches();
        bkClientUsers.clear();
    }

    /**
     * Mob/animal labels (background, spawn-egg icon, name) are queued during the label pass and
     * drawn as three batched draws after it instead of three draws per label. In profiling the
     * per-label draws were the main ESP cost. Identical except where two labels overlap: the
     * later label's background no longer covers the earlier label's icon and name.
     */
    private final dev.bkclient.systems.modules.modules.visuals.esp.EspLabelBatch labelBatch =
        new dev.bkclient.systems.modules.modules.visuals.esp.EspLabelBatch();

    private final EventListener<PreHudRenderEvent> onPreHud = event -> {
        if (mc.player == null || mc.world == null) return;

        List<Entity> targets = frameTargetsValid ? frameTargets : this.collectTargets();
        frameTargetsValid = false;
        List<PlayerEntity> playerTargets = new ArrayList<>();
        List<ItemEntity> itemTargets = new ArrayList<>();
        List<Entity> otherTargets = new ArrayList<>();

        for (Entity entity : targets) {
            if (entity instanceof PlayerEntity player) {
                playerTargets.add(player);
            } else if (entity instanceof ItemEntity item) {
                itemTargets.add(item);
            } else {
                otherTargets.add(entity);
            }
        }

        for (PlayerEntity player : playerTargets) {
            this.renderPlayerEsp(event, player);
        }

        List<List<ItemEntity>> itemGroups = this.groupEntities(itemTargets, 9.0F);
        for (List<ItemEntity> group : itemGroups) {
            if (group.size() == 1 || this.itemDisplayMode.is(this.itemIndividual)) {
                for (ItemEntity item : group) {
                    this.renderItemEsp(event, item);
                }
            } else if (this.itemDisplayMode.is(this.itemCount)) {
                this.renderItemGroupCount(event, group);
            } else if (this.itemDisplayMode.is(this.itemColumn)) {
                this.renderItemGroupColumn(event, group);
            }
        }

        labelBatch.begin();
        try {
            for (Entity entity : otherTargets) {
                this.renderEntityEsp(event, entity);
            }
        } finally {
            labelBatch.flush(event.getContext());
        }
    };

    /** Recorded spawn-egg models (Sodium only); null without Sodium. Same output bytes as the item path. */
    private final dev.bkclient.systems.modules.modules.visuals.esp.EspEggReplay eggReplay =
        net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("sodium")
            ? new dev.bkclient.systems.modules.modules.visuals.esp.EspEggReplay() : null;

    private void drawEgg(PreHudRenderEvent event, ItemStack egg, float x, float y, float scale) {
        if (labelBatch.active()) {
            if (eggReplay == null || !eggReplay.draw(event.getContext(), egg, x, y, scale))
                event.getContext().drawBatchItem(egg, x, y, scale);
        } else event.getContext().drawItem(egg, x, y, scale);
    }

    /** Pill label background (radius = height / 2), batched during the mob/animal label pass. */
    private void drawLabelBackground(PreHudRenderEvent event, float x, float y, float width, float height, ColorRGBA color) {
        if (labelBatch.active())
            labelBatch.pill(event.getContext().getMatrices().peek().getPositionMatrix(), x, y, width, height, color);
        else event.getContext().drawRoundedRect(x, y, width, height, BorderRadius.all(height / 2.0F), color);
    }

    private void drawLabelText(PreHudRenderEvent event, Font font, String text, float x, float y, ColorRGBA color) {
        if (!labelBatch.active()
                || !labelBatch.text(event.getContext().getMatrices().peek().getPositionMatrix(), font, text, x, y, color.getRGB()))
            event.getContext().drawText(font, text, x, y, color);
    }

    /** Configure the standard renderer for the explicit fakeplayer ESP preview command. */
    public void enableEquipmentPreview() {
        this.box.enable();
        this.name.enable();
        this.hp.enable();
        this.armor.enable();
        this.items.enable();
        this.itemCountLabel.enable();
        this.durability.enable();
        this.enchantments.enable();
        this.targetPlayers.select();
        this.enable();
    }

    private boolean isTarget(Entity entity) {
        if (mc.player == null || mc.world == null || entity == mc.player || entity.isSpectator() || entity.isRemoved()) return false;
        float distance = entity instanceof PlayerEntity ? maxDistance.getCurrentValue() : profile(tab(entity)).range.getCurrentValue();
        if (mc.player.squaredDistanceTo(entity) > distance * distance) return false;
        if (entity instanceof PlayerEntity player) {
            if (!this.targetPlayers.isSelected() || player instanceof FakePlayerEntity) return false;
            String name = player.getGameProfile().getName();
            if (name.startsWith("-") || name.contains("@") || name.contains(".")) return false;
            AntiBot antiBot = BKClientInit.getInstance().getModuleManager().getModule(AntiBot.class);
            return antiBot == null || !antiBot.isRWBot(player);
        }
        // MobEntity includes every current animal and mob, without a species whitelist.
        if (entity instanceof MobEntity)
            return entity instanceof AnimalEntity ? this.targetAnimals.isSelected() : this.targetMobs.isSelected();
        // Item entities remain valid targets while flying, bouncing or being picked up.
        // Restricting this to onGround made item ESP disappear for the exact moment it was
        // most useful (a thrown drop, a moving stack or an item coming from a container).
        return entity instanceof ItemEntity && this.targetItems.isSelected();
    }

    private List<Entity> collectTargets() {
        if (mc.player == null || mc.world == null) return List.of();
        List<Entity> list = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) if (isTarget(entity)) list.add(entity);
        return list;
    }
    private static ESP instance;

    public ESP() {
        LinkedModeSetting.toggle(this, "color.source.box", this.boxThemeSync, "Theme", "Custom");

        this.targetPlayers = new SelectSetting.Value(this.objects, "players").select();
        this.targetAnimals = new SelectSetting.Value(this.objects, "animals");
        this.targetMobs = new SelectSetting.Value(this.objects, "mobs").select();
        this.targetItems = new SelectSetting.Value(this.objects, "items");
        instance = this;
    }

    public static boolean shouldGlow(Entity entity) {
        return instance != null && instance.isEnabled() && instance.previewGlowEnabled(instance.tab(entity)) && instance.isTarget(entity);
    }

    public static int getGlowColor(Entity entity) {
        if (instance == null) return 0x00FF00;
        ColorRGBA c = instance.previewGlowColor(instance.tab(entity));
        if (c == null) return 0x00FF00;
        int r = Math.round(c.getRed()) & 0xFF;
        int g = Math.round(c.getGreen()) & 0xFF;
        int b = Math.round(c.getBlue()) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    public void markBKClientUser(UUID uuid) {
        this.bkClientUsers.add(uuid);
    }

    public boolean isBKClientUser(PlayerEntity player) {
        return this.bkClientUsers.contains(player.getUuid());
    }

    private void renderEntityEsp(PreHudRenderEvent event, Entity entity) {
        if (entity instanceof MobEntity mob) {
            if (!previewNameEnabled(tab(entity))) return;
        }
        if (entity instanceof PlayerEntity player) {
            this.renderPlayerEsp(event, player);
        } else if (entity instanceof AnimalEntity animal) {
            this.renderAnimalEsp(event, animal);
        } else if (entity instanceof MobEntity mob) {
            this.renderMobEsp(event, mob);
        } else if (entity instanceof ItemEntity item) {
            this.renderItemEsp(event, item);
        }
    }

    /** Only the rendered nameplate is clickable; the player's body is not a UI control. */
    public PlayerEntity getPlayerAt(double mouseX, double mouseY) {
        if (!isEnabled() || !name.isEnabled() || mc.options.hudHidden || mc.world == null || mc.player == null
                || !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)
                || System.nanoTime() - nameplateFrame > 250_000_000L
                || nameplateWidth != mc.getWindow().getScaledWidth() || nameplateHeight != mc.getWindow().getScaledHeight()) return null;
        PlayerEntity selected = null;
        double nearest = Double.MAX_VALUE;
        for (NameplateHit hit : nameplateHits) {
            if (mouseX < hit.left || mouseX > hit.right || mouseY < hit.top || mouseY > hit.bottom) continue;
            PlayerEntity player = mc.world.getPlayerByUuid(hit.player);
            if (player == null || !isTarget(player) || !player.isAlive()) continue;
            if (equipmentSimple.isSelected() && !simpleNametags.accepts(player)) continue;
            double distance = mc.player.squaredDistanceTo(player);
            if (distance < nearest) { nearest = distance; selected = player; }
        }
        return selected;
    }

    private void recordNameplate(PlayerEntity player, float left, float top, float right, float bottom) {
        if (!(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) return;
        if (!Float.isFinite(left + top + right + bottom) || left >= right || top >= bottom) return;
        nameplateHits.add(new NameplateHit(player.getUuid(), left, top, right, bottom));
    }

    /** Project the same local rectangle and matrices used to draw the Simple nameplate. */
    public static void recordSimpleNameplate(PlayerEntity player, org.joml.Matrix4f model, float width, float bottomEdge) {
        if (instance == null || !instance.isSimpleNametagActive()
                || !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) return;
        var matrix = new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix())
            .mul(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix()).mul(model);
        float left = Float.POSITIVE_INFINITY, top = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY, bottom = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            var corner = matrix.transform(new org.joml.Vector4f((i & 1) == 0 ? -width / 2 : width / 2,
                (i & 2) == 0 ? -16 : bottomEdge, 0, 1));
            if (corner.w <= 0 || !Float.isFinite(corner.w)) return;
            float x = (corner.x / corner.w + 1) * mc.getWindow().getScaledWidth() / 2F;
            float y = (1 - corner.y / corner.w) * mc.getWindow().getScaledHeight() / 2F;
            left = Math.min(left, x); right = Math.max(right, x);
            top = Math.min(top, y); bottom = Math.max(bottom, y);
        }
        instance.recordNameplate(player, left, top, right, bottom);
    }

    private float[] computeScreenBounds(Entity entity, float tickDelta) {
        double px = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double py = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double pz = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
        float w = entity.getWidth();
        float h = entity.getHeight();
        float hw = w * 0.5F;

        float boxMinX = Float.MAX_VALUE, boxMinY = Float.MAX_VALUE;
        float boxMaxX = -Float.MAX_VALUE, boxMaxY = -Float.MAX_VALUE;

        // Same eight corners in the same order as before (bottom ring, then top ring), projected
        // without allocating a Vec3d/Vec2f per corner.
        float[] screen = this.projectScratch;
        for (int i = 0; i < 8; i++) {
            double cx = px + ((i & 1) == 0 ? -hw : hw);
            double cy = py + (i < 4 ? 0 : h);
            double cz = pz + ((i & 2) == 0 ? -hw : hw);
            if (!Utils.worldToScreen(cx, cy, cz, screen)) continue;
            boxMinX = Math.min(boxMinX, screen[0]);
            boxMinY = Math.min(boxMinY, screen[1]);
            boxMaxX = Math.max(boxMaxX, screen[0]);
            boxMaxY = Math.max(boxMaxY, screen[1]);
        }

        if (boxMinX >= boxMaxX || boxMinY >= boxMaxY) return null;
        if (boxMaxX < 0 || boxMinX > sr.getScaledWidth() || boxMaxY < 0 || boxMinY > sr.getScaledHeight()) return null;

        return new float[]{boxMinX, boxMinY, boxMaxX, boxMaxY, boxMaxX - boxMinX, boxMaxY - boxMinY};
    }

    private <T extends Entity> List<List<T>> groupEntities(List<T> entities, float squaredDistance) {
        if (entities.isEmpty()) return List.of();
        
        List<List<T>> groups = new ArrayList<>();
        Set<T> processed = new HashSet<>();
        List<T> unsorted = new ArrayList<>(entities);

        for (T entity : unsorted) {
            if (processed.contains(entity)) continue;

            List<T> group = new ArrayList<>();
            group.add(entity);
            processed.add(entity);

            boolean expanded;
            do {
                expanded = false;
                for (T candidate : entities) {
                    if (processed.contains(candidate)) continue;
                    boolean merged = false;
                    for (T member : group) {
                        float distance = this.getGroupingDistance(candidate, member);
                        if (distance <= squaredDistance) {
                            group.add(candidate);
                            processed.add(candidate);
                            expanded = true;
                            merged = true;
                            break;
                        }
                    }
                    if (merged) break;
                }
            } while (expanded);

            groups.add(group);
        }

        return groups;
    }
    
    private float getGroupingDistance(Entity a, Entity b) {
        if (a == b) return 0.0F;
        if (a instanceof PlayerEntity && b instanceof PlayerEntity) {
            return (float) a.squaredDistanceTo(b);
        }
        if (a instanceof ItemEntity && b instanceof ItemEntity) {
            return (float) a.squaredDistanceTo(b);
        }
        if (a instanceof PlayerEntity && b instanceof ItemEntity) {
            PlayerEntity player = (PlayerEntity) a;
            ItemEntity item = (ItemEntity) b;
            float inventoryMargin = 30.0F;
            if (player.getEyePos().distanceTo(item.getEyePos()) < inventoryMargin) {
                return 0.0F;
            }
            ItemStack mainHand = player.getMainHandStack();
            ItemStack offHand = player.getOffHandStack();
            if (mainHand.getItem() == item.getStack().getItem() || 
                offHand.getItem() == item.getStack().getItem()) {
                return (float) player.squaredDistanceTo(item);
            }
            return Float.MAX_VALUE / 2.0F;
        }
        if (a instanceof ItemEntity && b instanceof PlayerEntity) {
            return this.getGroupingDistance(b, a);
        }
        return (float) a.squaredDistanceTo(b);
    }

    private Vec3d groupCenter(List<? extends Entity> group) {
        double cx = 0, cy = 0, cz = 0;
        for (Entity e : group) {
            cx += e.getX();
            cy += e.getY() + e.getHeight() / 2.0;
            cz += e.getZ();
        }
        int n = group.size();
        return new Vec3d(cx / n, cy / n, cz / n);
    }

    private void renderItemGroupCount(PreHudRenderEvent event, List<ItemEntity> group) {
        if(!itemOptions.name.isEnabled())return;
        Vec3d center = this.groupCenter(group);
        Vec2f screen = Utils.worldToScreen(center);
        if (screen == null) return;

        float[] bounds = this.computeScreenBounds(group.get(0), event.getTickDelta());
        if (bounds == null) return;
        float boxH = bounds[5];

        boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
        float nameScale = itemOptions.nameSize.getCurrentValue();
        float fontSize = MathHelper.clamp(boxH * nameScale * 4.0F, 5.5F, 12.0F) * itemOptions.scale.getCurrentValue();
        Font font = Fonts.MEDIUM.getFont(fontSize);

        int totalCount = group.stream().mapToInt(e -> e.getStack().getCount()).sum();
        String countStr = String.valueOf(totalCount);
        Font countFont = Fonts.MEDIUM.getFont(fontSize);
        float countTextWidth = countFont.width(countStr);
        float countPadding = fontSize * 0.3F;
        float countRectWidth = countTextWidth + countPadding * 2.0F;
        float countRectHeight = fontSize + fontSize * 0.2F;
        float countRectRadius = countRectHeight * 0.25F;

        String firstName = group.get(0).getStack().getName().getString();
        boolean allSame = group.stream().allMatch(e -> e.getStack().getItem() == group.get(0).getStack().getItem());
        String label = allSame ? firstName : Localizator.translate("items");
        float textWidth = font.width(label);

        float gap = fontSize * 0.4F;
        float padding = fontSize * 0.35F;
        float bgHeight = fontSize + padding * 2.0F;
        float radius = bgHeight / 2.0F;
        float bgY = screen.y - bgHeight - 2.0F;

        ColorRGBA bgColor = dark ? new ColorRGBA(0, 0, 0, 140) : new ColorRGBA(255, 255, 255, 160);
        ColorRGBA textColor = dark ? ColorRGBA.WHITE : new ColorRGBA(0, 0, 0);

        float totalW = textWidth + gap + countRectWidth;
        float textX = screen.x - totalW / 2.0F;
        float countRectX = textX + textWidth + gap;
        float bgX = textX - padding;
        float bgWidth = padding + totalW + padding;

        event.getContext().drawRoundedRect(bgX, bgY, bgWidth, bgHeight, BorderRadius.all(radius), bgColor);
        float textY = bgY + (bgHeight - font.height()) / 2.0F;
        event.getContext().drawText(font, label, textX, textY, textColor);

        float countRectY = bgY + (bgHeight - countRectHeight) / 2.0F;
        event.getContext().drawRoundedRect(countRectX, countRectY, countRectWidth, countRectHeight,
            BorderRadius.all(countRectRadius), new ColorRGBA(60, 60, 230, 230));
        float countTextX = countRectX + (countRectWidth - countTextWidth) / 2.0F;
        float countTextY = bgY + (bgHeight - countFont.height()) / 2.0F;
        event.getContext().drawText(countFont, countStr, countTextX, countTextY, ColorRGBA.WHITE);
    }

    private void renderItemGroupColumn(PreHudRenderEvent event, List<ItemEntity> group) {
        if(!itemOptions.name.isEnabled())return;
        Vec3d center = this.groupCenter(group);
        Vec2f screen = Utils.worldToScreen(center);
        if (screen == null) return;

        float[] bounds = this.computeScreenBounds(group.get(0), event.getTickDelta());
        if (bounds == null) return;
        float boxH = bounds[5];

        boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
        float nameScale = itemOptions.nameSize.getCurrentValue();
        float fontSize = MathHelper.clamp(boxH * nameScale * 4.0F, 5.5F, 12.0F) * itemOptions.scale.getCurrentValue();
        Font font = Fonts.MEDIUM.getFont(fontSize);
        float lineHeight = font.height() + 2.0F;

        float maxWidth = 0;
        java.util.Map<String, Integer> itemCounts = new java.util.LinkedHashMap<>();
        for (ItemEntity item : group) {
            String name = item.getStack().getName().getString();
            itemCounts.merge(name, item.getStack().getCount(), Integer::sum);
        }
        List<String> lines = new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            String line = entry.getKey() + " " + entry.getValue() + "x";
            lines.add(line);
            maxWidth = Math.max(maxWidth, font.width(line));
        }

        float padding = fontSize * 0.35F;
        float bgHeight = lines.size() * lineHeight + padding * 2.0F;
        float radius = 4.0F;
        float bgY = screen.y - bgHeight - 2.0F;

        ColorRGBA bgColor = dark ? new ColorRGBA(0, 0, 0, 160) : new ColorRGBA(200, 200, 200, 180);
        ColorRGBA textColor = dark ? ColorRGBA.WHITE : new ColorRGBA(0, 0, 0);

        float bgX = screen.x - maxWidth / 2.0F - padding;
        float bgWidth = maxWidth + padding * 2.0F;

        event.getContext().drawRoundedRect(bgX, bgY, bgWidth, bgHeight, BorderRadius.all(radius), bgColor);

        float textY = bgY + padding;
        for (String line : lines) {
            float textX = screen.x - font.width(line) / 2.0F;
            event.getContext().drawText(font, line, textX, textY, textColor);
            textY += lineHeight;
        }
    }

    private void renderPlayerEsp(PreHudRenderEvent event, PlayerEntity player) {
        if (this.equipmentSimple.isSelected()) return;
        float[] bounds = this.computeScreenBounds(player, event.getTickDelta());
        if (bounds == null) return;
        Vec3d pos = Utils.getInterpolatedPos(player, event.getTickDelta());
        drawDetailedOverlay(event, player, bounds, Utils.worldToScreen(pos.add(0, player.getHeight() + 0.25, 0)));
    }

    /** Shares the actual Detailed nameplate, equipment and health drawing with the editor. */
    public void drawDetailedOverlay(PreHudRenderEvent event, PlayerEntity player, float[] bounds, Vec2f headScreen) {
        float boxMinX = bounds[0], boxMinY = bounds[1];
        float boxMaxX = bounds[2], boxMaxY = bounds[3];
        float boxW = bounds[4], boxH = bounds[5];

        if (this.hp.isEnabled()) {
            float scale = this.espScale.getCurrentValue();
            float width = BAR_WIDTH * scale;
            float border = BORDER * scale;
            float barX = boxMaxX + BAR_GAP * scale;
            float barY = boxMinY;
            float barH = boxH;

            float health = player.getHealth();
            float maxHealth = player.getMaxHealth();
            float ratio = MathHelper.clamp(health / maxHealth, 0.0F, 1.0F);

            event.getContext().drawRect(barX - border, barY - border, width + border * 2, barH + border * 2, BAR_BORDER);
            event.getContext().drawRect(barX, barY, width, barH, BAR_BG);

            if (ratio > 0.001F) {
                event.getContext().drawRoundedRect(barX, barY, width, barH, BorderRadius.ZERO,
                    new VerticalGradient(new ColorRGBA(0, 255, 0), new ColorRGBA(255, 0, 0)));
                float emptyH = barH * (1.0F - ratio);
                if (emptyH > 0.5F) {
                    event.getContext().drawRect(barX, barY, width, emptyH, BAR_OVERLAY);
                }
            }
        }

        if (headScreen == null) return;
        var matrices = event.getContext().getMatrices();
        float scale = this.espScale.getCurrentValue();
        matrices.push();
        try {
            matrices.translate(headScreen.x, headScreen.y, 0);
            matrices.scale(scale, scale, 1);
            matrices.translate(-headScreen.x, -headScreen.y, 0);
        float iconSize = MathHelper.clamp(boxH * 0.16F, 9.0F, 16.0F);

        float nametagTop = Float.NaN;
        if (headScreen != null && this.name.isEnabled()) {
            boolean isBKClient = this.isBKClientUser(player);
            boolean isFriend = BKClientInit.getInstance().getFriendManager().isFriend(player.getGameProfile().getName());
            String displayNameStr = isFriend ? player.getDisplayName().getString()
                    : isBKClient ? "BK-Client" : player.getDisplayName().getString();
            boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();

            float nameScale = this.nameSize.getCurrentValue();
            float fontSize = MathHelper.clamp(boxH * nameScale, 5.5F, 12.0F);
            Font font = Fonts.MEDIUM.getFont(fontSize);
            float textWidth = font.width(displayNameStr);

            int hp = MathHelper.ceil(player.getHealth());
            String hpText = String.valueOf(hp);
            float hpFontSize = fontSize * 0.9F;
            Font hpFont = Fonts.MEDIUM.getFont(hpFontSize);
            float hpTextWidth = hpFont.width(hpText);

            float hpPadding = fontSize * 0.20F;
            float hpRectWidth = hpTextWidth + hpPadding * 2.0F;
            float hpRectHeight = hpFontSize + fontSize * 0.2F;
            float hpRectRadius = hpRectHeight * 0.25F;

            float padding = fontSize * 0.18F;
            float dotSize = fontSize * 0.5F;
            float gap = fontSize * 0.2F;
            float hpGap = fontSize * 0.25F;

            float bgHeight = fontSize + padding * 2.0F;
            float radius = bgHeight / 2.0F;

            float bgY = headScreen.y - bgHeight - 2.0F;
            nametagTop = bgY;

            ColorRGBA bgColor = dark ? new ColorRGBA(0, 0, 0, 140) : new ColorRGBA(255, 255, 255, 160);
            ColorRGBA nameColor = dark ? ColorRGBA.WHITE : new ColorRGBA(0, 0, 0);

            ColorRGBA dotColor = nameColor;
            Team team = player.getScoreboardTeam();
            if (team != null) {
                Formatting formatting = team.getColor();
                if (formatting != null && formatting.getColorValue() != null) {
                    dotColor = ColorRGBA.fromInt(0xFF000000 | formatting.getColorValue());
                }
            }
            if (isBKClient) {
                dotColor = new ColorRGBA(0, 200, 255);
                bgColor = dark ? new ColorRGBA(0, 40, 60, 180) : new ColorRGBA(0, 180, 255, 160);
                nameColor = new ColorRGBA(0, 200, 255);
            }

            if (isFriend) {
                dotColor = new ColorRGBA(50, 220, 100);
                nameColor = dotColor;
            }

            float totalW = dotSize + gap + textWidth + hpGap + hpRectWidth;
            float startX = headScreen.x - totalW / 2.0F;
            float dotX = startX;
            float textX = dotX + dotSize + gap;
            float hpRectX = textX + textWidth + hpGap;
            float bgX = dotX - padding;
            float bgWidth = totalW + padding * 2.0F;
            recordNameplate(player,
                headScreen.x + (bgX - headScreen.x) * scale,
                headScreen.y + (bgY - headScreen.y) * scale,
                headScreen.x + (bgX + bgWidth - headScreen.x) * scale,
                headScreen.y + (bgY + bgHeight - headScreen.y) * scale);

            event.getContext().drawRoundedRect(bgX, bgY, bgWidth, bgHeight, BorderRadius.all(radius), bgColor);

            float dotY = bgY + (bgHeight - dotSize) / 2.0F;
            event.getContext().drawRoundedRect(dotX, dotY, dotSize, dotSize, BorderRadius.all(dotSize / 2.0F), dotColor);
            float textY = bgY + (bgHeight - font.height()) / 2.0F;
            event.getContext().drawText(font, displayNameStr, textX, textY, nameColor);

            float hpRectY = bgY + (bgHeight - hpRectHeight) / 2.0F;
            event.getContext().drawRoundedRect(hpRectX, hpRectY, hpRectWidth, hpRectHeight, BorderRadius.all(hpRectRadius), new ColorRGBA(230, 60, 60, 230));
            float hpTextX = hpRectX + (hpRectWidth - hpTextWidth) / 2.0F;
            float hpTextY = bgY + (bgHeight - hpFont.height()) / 2.0F;
            event.getContext().drawText(hpFont, hpText, hpTextX, hpTextY, ColorRGBA.WHITE);
        }

        boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
        float bottom = Float.isNaN(nametagTop) ? headScreen.y - 1 : nametagTop - 3;
        List<ItemStack> equipment = new ArrayList<>();
        if(items.isEnabled()&&!player.getMainHandStack().isEmpty())equipment.add(player.getMainHandStack());
        if(armor.isEnabled())for(int slot=3;slot>=0;slot--){var stack=player.getInventory().armor.get(slot);if(!stack.isEmpty())equipment.add(stack);}
        if(items.isEnabled()&&!player.getOffHandStack().isEmpty())equipment.add(player.getOffHandStack());
        renderDetailedEquipment(event,equipment,headScreen.x,bottom,iconSize,dark);
        } finally {
            matrices.pop();
        }
    }

    private void renderDetailedEquipment(PreHudRenderEvent event,List<ItemStack> stacks,float cx,float bottom,float size,boolean dark) {
        if(stacks.isEmpty())return;
        var c=event.getContext();
        Font font=Fonts.MEDIUM.getFont(6F);
        float line=Math.max(7.5F,font.height()+1.5F),cell=size+4;
        var labels=new ArrayList<List<String>>();
        for(var stack:stacks){
            var text=new ArrayList<String>();
            if(enchantments.isEnabled()){
                var entries=new ArrayList<>(stack.getEnchantments().getEnchantmentEntries());
                entries.sort(java.util.Comparator.comparing(e->e.getKey().getKey().map(k->k.getValue().toString()).orElse("")));
                for(var entry:entries){String id=entry.getKey().getKey().map(k->k.getValue().getPath()).orElse("enchant");text.add(shortEnchantment(id)+" "+entry.getIntValue());}
                if(text.size()>4){int extra=text.size()-3;text=new ArrayList<>(text.subList(0,3));text.add("+"+extra);}
            }
            for(String label:text)cell=Math.max(cell,font.width(label)+4);
            labels.add(text);
        }
        boolean percent=durabilityStyle.is(durabilityPercent);
        boolean info=durability.isEnabled()&&stacks.stream().anyMatch(ItemStack::isDamageable)
            ||itemCountLabel.isEnabled()&&stacks.stream().anyMatch(stack->stack.getCount()>1);
        float infoH=info?line+1:0,height=size+infoH+3,width=stacks.size()*cell+4;
        float top=bottom-height,left=cx-width/2;
        ColorRGBA ink=dark?ColorRGBA.WHITE:new ColorRGBA(25,28,30);
        c.drawRoundedRect(left,top,width,height,BorderRadius.all(3),dark?new ColorRGBA(12,15,17,190):new ColorRGBA(245,247,249,190));
        for(int i=0;i<stacks.size();i++){
            ItemStack stack=stacks.get(i);float center=left+2+(i+.5F)*cell;
            var text=labels.get(i);float ty=top-2-text.size()*line;
            for(String label:text){c.drawCenteredText(font,label,center,ty,ink);ty+=line;}
            c.drawItem(stack,center-size/2,top+1,size/16);
            float infoY=top+size+2;
            if(durability.isEnabled()&&stack.isDamageable()){
                float remaining=MathHelper.clamp(1-stack.getDamage()/(float)stack.getMaxDamage(),0,1);
                if(percent)c.drawCenteredText(font,Math.round(remaining*100)+"%",center,infoY,ink);
                else{
                    ColorRGBA color=remaining<.25F?new ColorRGBA(240,85,90):remaining<.5F?new ColorRGBA(240,183,65):new ColorRGBA(65,204,146);
                    float barWidth=size-4,barX=center-barWidth/2,barY=infoY+1;
                    dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(c.getMatrices(),barX,barY,barWidth,1,BorderRadius.all(.25F),ink.mulAlpha(.22F));
                    if(remaining>0)dev.bkclient.utility.render.DrawUtility.drawRoundedRectScreenAA(c.getMatrices(),barX,barY,barWidth*remaining,1,BorderRadius.all(Math.min(.25F,barWidth*remaining/2)),color);
                }
            }
            if(itemCountLabel.isEnabled()&&stack.getCount()>1){
                String count=Integer.toString(stack.getCount());float w=font.width(count)+3;
                float cy=stack.isDamageable()&&durability.isEnabled()?top+size-line:infoY;
                c.drawRoundedRect(center-w/2,cy-.5F,w,line,BorderRadius.all(2),new ColorRGBA(12,15,17,230));
                c.drawCenteredText(font,count,center,cy,ColorRGBA.WHITE);
            }
        }
    }

    private static String shortEnchantment(String id) {
        return switch (id) {
            case "protection" -> "Prot";
            case "projectile_protection" -> "Proj";
            case "blast_protection" -> "Blast";
            case "fire_protection" -> "FProt";
            case "unbreaking" -> "Unb";
            case "mending" -> "Mend";
            case "sharpness" -> "Sharp";
            case "fire_aspect" -> "Fire";
            case "feather_falling" -> "Fall";
            case "depth_strider" -> "Depth";
            case "binding_curse", "vanishing_curse" -> "Curse";
            default -> {
                String label = id.replace('_', ' ');
                yield Character.toUpperCase(label.charAt(0)) + label.substring(1, Math.min(label.length(), 6));
            }
        };
    }

    private void renderAnimalEsp(PreHudRenderEvent event, AnimalEntity animal) {
        if(!animalOptions.name.isEnabled())return;
        float tickDelta = event.getTickDelta();
        Vec3d pos = Utils.getInterpolatedPos(animal, tickDelta);
        float h = animal.getHeight();

        Vec3d headPos = pos.add(0, h + 0.25, 0);
        Vec2f headScreen = Utils.worldToScreen(headPos);
        if (headScreen == null) return;

        float[] bounds = this.computeScreenBounds(animal, tickDelta);
        if (bounds == null) return;
        float boxH = bounds[5];

        boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
        float fontSize = MathHelper.clamp(boxH * animalOptions.nameSize.getCurrentValue(), 5.5F, 12.0F) * animalOptions.scale.getCurrentValue();
        Font font = Fonts.MEDIUM.getFont(fontSize);
        String nameStr = animal.hasCustomName() ? animal.getCustomName().getString() : animal.getDisplayName().getString();
        float textWidth = font.width(nameStr);

        float iconSize = fontSize * 1.2F;
        float gap = fontSize * 0.25F;
        float padding = fontSize * 0.35F;
        float bgHeight = fontSize + padding * 2.0F;
        float radius = bgHeight / 2.0F;

        float bgY = headScreen.y - bgHeight - 2.0F;

        ColorRGBA bgColor = dark ? new ColorRGBA(0, 0, 0, 140) : new ColorRGBA(255, 255, 255, 160);
        ColorRGBA nameColor = dark ? ColorRGBA.WHITE : new ColorRGBA(0, 0, 0);

        float totalW = iconSize + gap + textWidth;
        float startX = headScreen.x - totalW / 2.0F;
        float iconX = startX;
        float textX = iconX + iconSize + gap;
        float bgX = iconX - padding;
        float bgWidth = totalW + padding * 2.0F;

        this.drawLabelBackground(event, bgX, bgY, bgWidth, bgHeight, bgColor);

        ItemStack egg = this.spawnEgg(animal.getType());
        if (!egg.isEmpty()) {
            float iconY = bgY + (bgHeight - iconSize) / 2.0F;
            this.drawEgg(event, egg, iconX, iconY, iconSize / 16.0F);
        }

        float textY = bgY + (bgHeight - font.height()) / 2.0F;
        this.drawLabelText(event, font, nameStr, textX, textY, nameColor);
    }

    private void renderMobEsp(PreHudRenderEvent event, MobEntity mob) {
        if(!mobOptions.name.isEnabled())return;
        float tickDelta = event.getTickDelta();
        Vec3d pos = Utils.getInterpolatedPos(mob, tickDelta);
        float h = mob.getHeight();

        Vec3d headPos = pos.add(0, h + 0.25, 0);
        Vec2f headScreen = Utils.worldToScreen(headPos);
        if (headScreen == null) return;

        float[] bounds = this.computeScreenBounds(mob, tickDelta);
        if (bounds == null) return;
        float boxH = bounds[5];

        boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
        float fontSize = MathHelper.clamp(boxH * mobOptions.nameSize.getCurrentValue(), 5.5F, 12.0F) * mobOptions.scale.getCurrentValue();
        Font font = Fonts.MEDIUM.getFont(fontSize);
        String nameStr = mob.hasCustomName() ? mob.getCustomName().getString() : mob.getDisplayName().getString();
        float textWidth = font.width(nameStr);

        float iconSize = fontSize * 1.2F;
        float gap = fontSize * 0.25F;
        float padding = fontSize * 0.35F;
        float bgHeight = fontSize + padding * 2.0F;
        float radius = bgHeight / 2.0F;

        float bgY = headScreen.y - bgHeight - 2.0F;

        ColorRGBA bgColor = dark ? new ColorRGBA(0, 0, 0, 140) : new ColorRGBA(255, 255, 255, 160);
        ColorRGBA nameColor = dark ? ColorRGBA.WHITE : new ColorRGBA(0, 0, 0);

        float textX = headScreen.x - textWidth / 2.0F;
        float iconX = textX - gap - iconSize;
        float bgX = iconX - padding;
        float bgWidth = padding + iconSize + gap + textWidth + padding;

        this.drawLabelBackground(event, bgX, bgY, bgWidth, bgHeight, bgColor);

        ItemStack egg = this.spawnEgg(mob.getType());
        if (!egg.isEmpty()) {
            float iconY = bgY + (bgHeight - iconSize) / 2.0F;
            this.drawEgg(event, egg, iconX, iconY, iconSize / 16.0F);
        }

        float textY = bgY + (bgHeight - font.height()) / 2.0F;
        this.drawLabelText(event, font, nameStr, textX, textY, nameColor);
    }

    private void renderItemEsp(PreHudRenderEvent event, ItemEntity item) {
        if(!itemOptions.name.isEnabled())return;
        if (shouldSkipItemRendering(item)) return;

        float tickDelta = event.getTickDelta();
        Vec3d pos = Utils.getInterpolatedPos(item, tickDelta);
        float h = item.getHeight();

        float[] bounds = this.computeScreenBounds(item, tickDelta);
        if (bounds == null) return;
        float boxH = bounds[5];

        Vec3d headPos = pos.add(0, h + 0.25, 0);
        Vec2f screen = Utils.worldToScreen(headPos);
        if (screen == null) return;

        ItemStack stack = item.getStack();

        boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();
        float nameScale = itemOptions.nameSize.getCurrentValue();
        float fontSize = MathHelper.clamp(boxH * nameScale * 4.0F, 5.5F, 12.0F) * itemOptions.scale.getCurrentValue();
        Font font = Fonts.MEDIUM.getFont(fontSize);

        String nameStr = stack.getName().getString();
        float textWidth = font.width(nameStr);

        String countStr = String.valueOf(stack.getCount());
        Font countFont = Fonts.MEDIUM.getFont(fontSize);
        float countTextWidth = countFont.width(countStr);

        float iconSize = fontSize * 1.2F;
        float gap = fontSize * 0.25F;
        float countPadding = fontSize * 0.3F;
        float countRectWidth = countTextWidth + countPadding * 2.0F;
        float countRectHeight = fontSize + fontSize * 0.2F;
        float countRectRadius = countRectHeight * 0.25F;
        float padding = fontSize * 0.35F;
        float bgHeight = fontSize + padding * 2.0F;
        float radius = bgHeight / 2.0F;
        float bgY = screen.y - bgHeight - 2.0F;

        ColorRGBA bgColor = dark ? new ColorRGBA(0, 0, 0, 140) : new ColorRGBA(255, 255, 255, 160);
        ColorRGBA nameColor = dark ? ColorRGBA.WHITE : new ColorRGBA(0, 0, 0);

        float countGap = fontSize * 0.4F;
        float totalW = iconSize + gap + textWidth + countGap + countRectWidth;
        float startX = screen.x - totalW / 2.0F;
        float iconX = startX;
        float nameX = iconX + iconSize + gap;
        float bgX = startX - padding;
        float bgWidth = totalW + padding * 2.0F;

        event.getContext().drawRoundedRect(bgX, bgY, bgWidth, bgHeight, BorderRadius.all(radius), bgColor);

        float iconY = bgY + (bgHeight - iconSize) / 2.0F;
        event.getContext().drawItem(stack, iconX, iconY, iconSize / 16.0F);

        float textY = bgY + (bgHeight - font.height()) / 2.0F;
        event.getContext().drawText(font, nameStr, nameX, textY, nameColor);

        float countRectX = nameX + textWidth + countGap;
        float countRectY = bgY + (bgHeight - countRectHeight) / 2.0F;
        event.getContext().drawRoundedRect(countRectX, countRectY, countRectWidth, countRectHeight, BorderRadius.all(countRectRadius), new ColorRGBA(60, 60, 230, 230));
        float countTextX = countRectX + (countRectWidth - countTextWidth) / 2.0F;
        float countTextY = bgY + (bgHeight - countFont.height()) / 2.0F;
        event.getContext().drawText(countFont, countStr, countTextX, countTextY, ColorRGBA.WHITE);
    }
    
    private boolean shouldSkipItemRendering(ItemEntity item) {
        if (mc.player == null) return true;
        
        if (item.getStack().getCount() <= 1 && isPlayerHoldingItem(item)) {
            return true;
        }
        
        return false;
    }
    
    private boolean isPlayerHoldingItem(ItemEntity item) {
        if (mc.player == null) return false;
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();
        return (mainHand.getItem() == item.getStack().getItem() && 
               mc.player.squaredDistanceTo(item) < 2.0F);
    }
    
}
