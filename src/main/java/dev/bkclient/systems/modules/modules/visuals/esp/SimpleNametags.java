package dev.bkclient.systems.modules.modules.visuals.esp;
import net.minecraft.util.math.MathHelper;

import dev.bkclient.systems.modules.modules.visuals.ESP;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.utility.interfaces.IMinecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.modules.other.NameProtect;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.ui.clicksys.port.data.IdName;
import dev.bkclient.ui.clicksys.port.data.TextSanitizer;
import dev.bkclient.ui.clicksys.port.enums.ServerMode;
import dev.bkclient.ui.clicksys.port.enums.VoiceIcon;
import dev.bkclient.ui.clicksys.port.render.DepthState;
import dev.bkclient.ui.clicksys.port.render.ItemIconCache;
import dev.bkclient.ui.clicksys.port.render.RotationBuffer;
import dev.bkclient.ui.clicksys.port.render.WorldRenderContext;
import dev.bkclient.ui.clicksys.port.util.HealthTracker;
import dev.bkclient.ui.clicksys.port.util.ItemIcons;
import dev.bkclient.ui.clicksys.port.util.ItemPickupMath;
import dev.bkclient.ui.clicksys.port.util.NameTagCache;
import dev.bkclient.ui.clicksys.port.util.NameTagEntry;
import dev.bkclient.ui.clicksys.port.util.VoicechatBridge;
import dev.bkclient.utility.game.EntityUtility;
import java.util.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/** BKClient NameTags renderer, with BK settings, friends and name protection. */
public final class SimpleNametags implements SettingsContainer, IMinecraft {
    private final List<Setting> settings = new ArrayList<>();
    private final ESP owner;

    public SimpleNametags(ESP owner) {
        this.owner = owner;
        owner.getSettings().addAll(settings);
    }

    @Override
    public List<Setting> getSettings() { return settings; }

    private boolean hidden() { return !owner.isSimpleNametagStyle(); }

    private final ModeSetting obhodHp = new ModeSetting(this, "HP source", this::hidden);
    private final ModeSetting.Value hpDefault = new ModeSetting.Value(obhodHp, "Default").select();
    private final ModeSetting.Value hpOff = new ModeSetting.Value(obhodHp, "Off");
    private final ModeSetting.Value hpMb = new ModeSetting.Value(obhodHp, "MB");
    private final ModeSetting.Value hpFt = new ModeSetting.Value(obhodHp, "FT");
    private final ModeSetting modeHp = new ModeSetting(this, "HP display", this::hidden);
    private final ModeSetting.Value hpPoints = new ModeSetting.Value(modeHp, "Health points").select();
    private final ModeSetting.Value hpHearts = new ModeSetting.Value(modeHp, "Hearts");
    private final SliderSetting size = new SliderSetting(this, "Size", this::hidden).min(0.01F).max(1F).step(0.05F).currentValue(0.35F);
    private final BooleanSetting backgroundPlashki = new BooleanSetting(this, "Background", this::hidden).enable();
    private final SliderSetting opacityPlashki = new SliderSetting(this, "Background opacity", () -> hidden() || !backgroundPlashki.isEnabled()).min(0).max(100).step(5).currentValue(100);
    private final SelectSetting targets = new SelectSetting(this, "Targets", this::hidden);
    private final SelectSetting.Value friends = new SelectSetting.Value(targets, "Friends").select();
    private final SelectSetting.Value enemies = new SelectSetting.Value(targets, "Enemies").select();
    private final SelectSetting.Value armoredEnemies = new SelectSetting.Value(targets, "Armored enemies only");
    private final SelectSetting chtoRenderit = new SelectSetting(this, "Elements", this::hidden);
    private final SelectSetting.Value prefix = new SelectSetting.Value(chtoRenderit, "Prefix").select();
    private final SelectSetting.Value armor = new SelectSetting.Value(chtoRenderit, "Armor").select();
    private final SelectSetting.Value leftHand = new SelectSetting.Value(chtoRenderit, "Left hand").select();
    private final SelectSetting.Value rightHand = new SelectSetting.Value(chtoRenderit, "Right hand");
    private final SelectSetting.Value coloredHp = new SelectSetting.Value(chtoRenderit, "Colored HP").select();
    private final BooleanSetting hidePustyeSloty = new BooleanSetting(this, "Hide empty slots", this::hidden);
    private final BooleanSetting itemDurability = new BooleanSetting(this, "esp.simple.durability", this::hidden).enable();
    private final BooleanSetting kolichestvoPredmetov = new BooleanSetting(this, "Item count", this::hidden).enable();
    private final BooleanSetting ikonkaVoicechat = new BooleanSetting(this, "Voicechat icon", this::hidden).enable();
    private final BooleanSetting uproschatVdali = new BooleanSetting(this, "Simplify at distance", this::hidden);
    private final SliderSetting rangeUproscheniya = new SliderSetting(this, "Simplify distance", () -> hidden() || !uproschatVdali.isEnabled()).min(8).max(128).step(2).currentValue(30);
    private final NameTagCache nameTagCache = new NameTagCache();
    private final Map<UUID, IdName> map = new HashMap<>();
    private final ArrayList<NameTagEntry> list = new ArrayList<>();
    private final ArrayList<NameTagEntry> list2 = new ArrayList<>();
    private final List<Entity> entityList = new ArrayList<>();
    private ClientWorld lastWorld;
    private static final Comparator<NameTagEntry> comparator = (a, b) -> Double.compare(b.value3, a.value3);

    public void tick() {
        if (lastWorld != null && (lastWorld != mc.world || !owner.isSimpleNametagActive())) {
            clearCaches();
        }
    }

    public void render(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (lastWorld != mc.world) {
            clearCaches();
            lastWorld = mc.world;
        }
        entityList.clear();
        RotationBuffer.onMatrix4fMatrix4f(event.getPositionMatrix(), event.getProjectionMatrix());
        try {
            render8(new WorldRenderContext(event.getMatrices(), event.getCamera(), mc.getRenderTickCounter(), mc.world));
        } finally {
            // Never retain player/world/item references in the reusable render entries.
            for (NameTagEntry entry : list) entry.update();
            list2.clear();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            if (RotationBuffer.isFlag()) RotationBuffer.setMinecraftClient(mc);
        }
    }

    public void clearCaches() {
        nameTagCache.update();
        HealthTracker.update();
        map.clear();
        for (NameTagEntry entry : list) entry.update();
        list.clear();
        list2.clear();
        entityList.clear();
        ItemIcons.update4();
        ItemIconCache.update();
        lastWorld = null;
    }


    public boolean isFriend(PlayerEntity player) {
        return BKClientInit.getInstance().getFriendManager().isFriend(player.getName().getString());
    }

    private static boolean hasArmor(PlayerEntity player) {
        for (ItemStack stack : player.getArmorItems()) if (!stack.isEmpty()) return true;
        return false;
    }

    public boolean accepts(PlayerEntity player) {
        if (!owner.isTargetEntity(player) || !player.isAlive()) return false;
        return isFriend(player) ? friends.isSelected()
                : enemies.isSelected() && (!armoredEnemies.isSelected() || hasArmor(player));
    }

    public List<Entity> getEntityList() { return entityList; }

    private NameTagEntry getNameTagEntryByInt(int count) {
        if (count == list.size()) list.add(new NameTagEntry());
        return list.get(count);
    }

   private void render8(WorldRenderContext worldRenderContext) {
      Camera camera = worldRenderContext.getCamera();
      Vec3d vec3d = camera.getPos();
      float f = worldRenderContext.getRenderTickCounter().getTickDelta(true);
      PlayerEntity playerentity = mc.player;
      Vec3d vec3d1 = playerentity.getPos();
      float f1 = (float)((Integer)mc.options.getFov().getValue()).intValue();

      boolean flag = prefix.isSelected();
      boolean flag1 = armor.isSelected();
      boolean flag2 = leftHand.isSelected();
      boolean flag3 = rightHand.isSelected();
      boolean flag4 = coloredHp.isSelected();
      NameProtect protect = BKClientInit.getInstance().getModuleManager().getModule(NameProtect.class);
      boolean flag5 = protect != null && protect.isEnabled();
      ServerMode servermode = HealthTracker.getServerModeByString(this.obhodHp.getValue().getName());
      boolean flag6 = hpHearts.isSelected();
      boolean flag7 = this.ikonkaVoicechat.isEnabled() && VoicechatBridge.isFlag();

      boolean flag8 = friends.isSelected();
      boolean flag9 = enemies.isSelected();
      boolean flag10 = armoredEnemies.isSelected();
      double d0 = owner.getTargetRange();
      this.list2.clear();
      int i = 0;

      for (PlayerEntity playerentity1 : mc.world.getPlayers()) {
         boolean flag14 = true;
         boolean flag13 = false;
         if (owner.isTargetEntity(playerentity1) && ItemPickupMath.isDoubleFloatVec3dBooleanCameraBooleanModulePlayerEntityPlayerEntity(
            d0, f1, vec3d1, flag14, camera, flag13, this, playerentity1, playerentity
         )) {
            boolean flag11 = this.isFriend(playerentity1);
            if (flag11 ? flag8 : flag9 && (!flag10 || hasArmor(playerentity1))) {
               double d1 = playerentity.distanceTo(playerentity1);
               Vec3d vec3d2 = DepthState.getVec3dByFloatEntityVec3d(f, playerentity1, vec3d);
               UUID uuid = playerentity1.getUuid();
               String s = playerentity1.getName().getString();
               String s1 = flag5 ? protect.patchName(s) : s;
               String s2 = "";
               if (flag) {
                  String s3 = playerentity1.getDisplayName().getString();
                  IdName idname = this.map.get(uuid);
                  int j = s3.hashCode();
                  if (idname == null) {
                     idname = new IdName();
                     idname.value = j;
                     idname.text = TextSanitizer.getStringByStringString(s3, s);
                     this.map.put(uuid, idname);
                  } else if (idname.value != j) {
                     idname.value = j;
                     idname.text = TextSanitizer.getStringByStringString(s3, s);
                  }

                  s2 = flag5 ? protect.patchName(idname.text) : idname.text;
               }

               // Default shares Detailed's entity health, without scoreboard overrides or absorption.
               float f5 = hpDefault.isSelected() ? playerentity1.getHealth()
                  : HealthTracker.getFloatByServerModePlayerEntity(servermode, playerentity1);
               float f6 = hpDefault.isSelected() ? playerentity1.getMaxHealth()
                  : HealthTracker.getFloatByPlayerEntityServerMode(playerentity1, servermode);
               String s4 = hpDefault.isSelected()
                  ? (flag6 ? MathHelper.ceil(f5 / 2F) + "/" + MathHelper.ceil(f6 / 2F) : MathHelper.ceil(f5) + "HP")
                  : (flag6 ? HealthTracker.getStringByFloatFloat(f5, f6) : HealthTracker.getStringByFloat2(f5));
               NameTagEntry nametagentry = this.getNameTagEntryByInt(i++);
               boolean flag12 = false;
               if (flag1) {
                  for (int k1 = 0; k1 < 4; k1++) {
                     ItemStack itemstack = playerentity1.getInventory().getStack(36 + k1);
                     nametagentry.itemStackArray[k1] = itemstack;
                     if (!itemstack.isEmpty()) {
                        flag12 = true;
                     }
                  }
               } else {
                  for (int k = 0; k < 4; k++) {
                     nametagentry.itemStackArray[k] = ItemStack.EMPTY;
                  }
               }

               VoiceIcon voiceicon = flag7 ? VoicechatBridge.getVoiceIconByUUID(uuid) : VoiceIcon.NONE;
               ItemStack itemstack2 = playerentity1.getMainHandStack();
               ItemStack itemstack1 = playerentity1.getOffHandStack();
               nametagentry.onVec3dFloatBooleanItemStackPlayerEntityItemStackStringDoubleFloatStringVoiceIconStringBoolean(
                  vec3d2, f6, flag11, itemstack2, playerentity1, itemstack1, s2, d1, f5, s1, voiceicon, s4, flag12
               );
               this.list2.add(nametagentry);
               this.entityList.add(playerentity1);
            }
         }
      }

      if (this.list.size() > i) this.list.subList(i, this.list.size()).clear();
      if (this.list2.isEmpty()) {
         this.nameTagCache.update();
         if (!this.map.isEmpty()) {
            this.map.clear();
         }
      } else {
         int l = this.list2.size();
         if (this.map.size() > l * 2) {
            HashSet hashset = new HashSet(l);

            for (int j1 = 0; j1 < l; j1++) {
               hashset.add(this.list2.get(j1).playerEntity.getUuid());
            }

            this.map.keySet().retainAll(hashset);
         }

         this.list2.sort(comparator);
         NameTagCache nametagcache = this.nameTagCache;
         nametagcache.setDurabilityEnabled(itemDurability.isEnabled());
         ArrayList arraylist1 = this.list2;
         float f7 = this.size.getCurrentValue();
         float f8 = this.backgroundPlashki.isEnabled() ? this.opacityPlashki.getCurrentValue() / 100.0F : 0.0F;
         boolean flag19 = this.backgroundPlashki.isEnabled();
         boolean flag20 = this.hidePustyeSloty.isEnabled();
         boolean flag21 = this.kolichestvoPredmetov.isEnabled();
         boolean flag22 = this.uproschatVdali.isEnabled();
         float f4 = this.rangeUproscheniya.getCurrentValue();
         boolean flag18 = flag22;
         boolean flag17 = flag21;
         boolean flag16 = flag20;
         boolean flag15 = flag19;
         float f3 = f8;
         float f2 = f7;
         ArrayList arraylist = arraylist1;
         nametagcache.onBooleanBooleanFloatBooleanListWorldRenderContextBooleanBooleanFloatBooleanBooleanFloatBoolean(
            flag1, flag3, f4, flag2, arraylist, worldRenderContext, flag15, flag18, f3, flag16, flag17, f2, flag4
         );

         for (int i1 = 0; i1 < l; i1++) {
            this.list2.get(i1).update();
         }
      }
   }


    /** A screen-owned cache keeps editor samples out of the live player cache. */
    public void renderPreview(NameTagCache cache, dev.bkclient.framework.base.UIContext context,
                              PlayerEntity player, float x, float y, float maxWidth) {
        NameTagEntry entry = new NameTagEntry();
        entry.playerEntity = player;
        entry.vec3d = Vec3d.ZERO;
        entry.text = prefix.isSelected() ? "[BK] " : "";
        entry.text2 = player.getGameProfile().getName();
        entry.text3 = hpOff.isSelected() ? "" : (hpHearts.isSelected() ? "9" : "18");
        entry.value = 18; entry.value2 = 20; entry.value3 = 4;
        entry.itemStack = player.getOffHandStack(); entry.itemStack2 = player.getMainHandStack();
        for (int i=0;i<4;i++) entry.itemStackArray[i]=player.getInventory().armor.get(i);
        float scale = Math.min(maxWidth / 290F, Math.max(.12F, size.getCurrentValue() * 1.5F));
        context.draw();
        cache.setDurabilityEnabled(itemDurability.isEnabled());
        cache.setPreviewTransform(new org.joml.Matrix4f(context.getMatrices().peek().getPositionMatrix())
            .translate(x,y,100).scale(scale,scale,1));
        try {
            cache.onBooleanBooleanFloatBooleanListWorldRenderContextBooleanBooleanFloatBooleanBooleanFloatBoolean(
                armor.isSelected(), rightHand.isSelected(), 32, leftHand.isSelected(), List.of(entry),
                new WorldRenderContext(context.getMatrices(), mc.gameRenderer.getCamera(), mc.getRenderTickCounter(), mc.world),
                backgroundPlashki.isEnabled(), false, opacityPlashki.getCurrentValue()/100F,
                hidePustyeSloty.isEnabled(), kolichestvoPredmetov.isEnabled(), size.getCurrentValue(), coloredHp.isSelected());
        } finally {
            cache.setPreviewTransform(null);
            entry.update();
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
        }
    }

    public static Text displayName(Entity entity) {
        float f;
        if (entity.getDisplayName() == null) {
            return Text.empty();
        }
        NameProtect nameProtectModule = BKClientInit.getInstance().getModuleManager().getModule(NameProtect.class);
        String displayName = nameProtectModule.isEnabled()
                ? nameProtectModule.patchName(entity.getDisplayName().getString())
                : entity.getDisplayName().getString();
        MutableText text = Text.of((String) displayName).copy();
        if (!(entity instanceof LivingEntity)) {
            return text;
        }
        LivingEntity living = (LivingEntity) entity;
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            f = EntityUtility.getHealth(player);
        } else {
            f = living.getHealth();
        }
        int health = (int) f;
        if (!text.getString().endsWith(" ")) {
            text.append(" ");
        }
        return text.append(
                (Text) Text.of((String) ("[" + String.valueOf(health == 1000 ? "?" : Integer.valueOf(health)) + "]"))
                        .copy().withColor(-2142128));
    }

}
