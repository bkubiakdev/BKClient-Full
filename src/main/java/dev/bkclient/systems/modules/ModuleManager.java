package dev.bkclient.systems.modules;

import java.util.List;
import lombok.Generated;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.systems.event.impl.window.MouseEvent;
import dev.bkclient.systems.modules.exception.UnknownModuleException;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.modules.modules.combat.AimAssist;
import dev.bkclient.systems.modules.modules.combat.AntiBot;
import dev.bkclient.systems.modules.modules.combat.Aura;
import dev.bkclient.systems.modules.modules.combat.AutoArmor;
import dev.bkclient.systems.modules.modules.combat.AutoDripstone;
import dev.bkclient.systems.modules.modules.combat.AutoGapple;
import dev.bkclient.systems.modules.modules.combat.AutoPotion;
import dev.bkclient.systems.modules.modules.combat.AutoTotem;
import dev.bkclient.systems.modules.modules.combat.AutoTrap;
import dev.bkclient.systems.modules.modules.combat.BackTrack;
import dev.bkclient.systems.modules.modules.combat.ErrorKiller;
import dev.bkclient.systems.modules.modules.combat.Hitboxes;
import dev.bkclient.systems.modules.modules.combat.TriggerBot;
import dev.bkclient.systems.modules.modules.combat.Velocity;
import dev.bkclient.systems.modules.modules.movement.AirStuck;
import dev.bkclient.systems.modules.modules.movement.AutoSprint;
import dev.bkclient.systems.modules.modules.movement.Flight;
import dev.bkclient.systems.modules.modules.movement.HighJump;
import dev.bkclient.systems.modules.modules.movement.NoSlow;
import dev.bkclient.systems.modules.modules.movement.NoWeb;
import dev.bkclient.systems.modules.modules.movement.Speed;
import dev.bkclient.systems.modules.modules.movement.Spider;
import dev.bkclient.systems.modules.modules.movement.Strafe;
import dev.bkclient.systems.modules.modules.movement.Timer;
import dev.bkclient.systems.modules.modules.movement.WaterSpeed;
import dev.bkclient.systems.modules.modules.movement.WindHop;
import dev.bkclient.systems.modules.modules.other.Assist;
import dev.bkclient.systems.modules.modules.other.AutoSign;
import dev.bkclient.systems.modules.modules.other.AutoTool;
import dev.bkclient.systems.modules.modules.other.AutoAccept;
import dev.bkclient.systems.modules.modules.other.AutoAuth;
import dev.bkclient.systems.modules.modules.other.AutoDuels;
import dev.bkclient.systems.modules.modules.other.AutoJoin;
import dev.bkclient.systems.modules.modules.other.AnticheatDetect;
import dev.bkclient.systems.modules.modules.other.DeathCords;
import dev.bkclient.systems.modules.modules.other.ChatSpammer;
import dev.bkclient.systems.modules.modules.other.EffectRemover;
import dev.bkclient.systems.modules.modules.other.FastItemUse;
import dev.bkclient.systems.modules.modules.other.FastPlace;
import dev.bkclient.systems.modules.modules.other.HitSound;
import dev.bkclient.systems.modules.modules.other.WindowScreen;
import dev.bkclient.systems.modules.modules.other.InventoryCleaner;
import dev.bkclient.systems.modules.modules.other.NameProtect;
import dev.bkclient.systems.modules.modules.other.StreamerMode;
import dev.bkclient.systems.modules.modules.other.Panic;
import dev.bkclient.systems.modules.modules.other.RussianRoulette;
import dev.bkclient.systems.modules.modules.other.Sounds;
import dev.bkclient.systems.modules.modules.other.TestModule;
import dev.bkclient.systems.modules.modules.other.TinyTask;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.modules.modules.bkmods.AutoPotions;
import dev.bkclient.systems.modules.modules.player.AutoEat;
import dev.bkclient.systems.modules.modules.player.AutoInvisible;
import dev.bkclient.systems.modules.modules.player.AutoLeave;
import dev.bkclient.systems.modules.modules.player.AutoSwap;
import dev.bkclient.systems.modules.modules.player.Blink;
import dev.bkclient.systems.modules.modules.player.ElytraUtils;
import dev.bkclient.systems.modules.modules.player.FreeCam;
import dev.bkclient.systems.modules.modules.player.GuiMove;
import dev.bkclient.systems.modules.modules.player.InvUtils;
import dev.bkclient.systems.modules.modules.player.InventorySet;
import dev.bkclient.systems.modules.modules.player.MiddleClick;
import dev.bkclient.systems.modules.modules.player.MineHelper;
import dev.bkclient.systems.modules.modules.player.NoDelay;
import dev.bkclient.systems.modules.modules.player.NoFall;
import dev.bkclient.systems.modules.modules.player.NoInteract;
import dev.bkclient.systems.modules.modules.player.NoPush;
import dev.bkclient.systems.modules.modules.player.NoRotate;
import dev.bkclient.systems.modules.modules.player.Nuker;
import dev.bkclient.systems.modules.modules.player.PlayerUtils;
import dev.bkclient.systems.modules.modules.player.Scaffold;
import dev.bkclient.systems.modules.modules.player.Stealer;
import dev.bkclient.systems.modules.modules.player.TargetPearl;
import dev.bkclient.systems.modules.modules.visuals.Ambience;
import dev.bkclient.systems.modules.modules.visuals.AntiInvisible;
import dev.bkclient.systems.modules.modules.visuals.BlockOutline;
import dev.bkclient.systems.modules.modules.visuals.Capes;
import dev.bkclient.systems.modules.modules.visuals.ChinaHat;
import dev.bkclient.systems.modules.modules.visuals.CustomFog;
import dev.bkclient.systems.modules.modules.visuals.CustomChatModule;
import dev.bkclient.systems.modules.modules.visuals.ESP;
import dev.bkclient.systems.modules.modules.visuals.FriendMarkers;
import dev.bkclient.systems.modules.modules.visuals.HandsShader;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.modules.modules.visuals.Crosshair;
import dev.bkclient.systems.modules.modules.visuals.CustomHotbar;
import dev.bkclient.systems.modules.modules.visuals.CustomSwords;
import dev.bkclient.systems.modules.modules.visuals.ItemPhysics;
import dev.bkclient.systems.modules.modules.visuals.KillEffects;
import dev.bkclient.systems.modules.modules.visuals.GlassVapor;
import dev.bkclient.systems.modules.modules.visuals.AspectRatio;
import dev.bkclient.systems.modules.modules.visuals.Particles;
import dev.bkclient.systems.modules.modules.visuals.JumpCircles;
import dev.bkclient.systems.modules.modules.visuals.LogoutSpot;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.systems.modules.modules.visuals.ObjectInfo;
import dev.bkclient.systems.modules.modules.visuals.Pet;
import dev.bkclient.systems.modules.modules.visuals.Prediction;
import dev.bkclient.systems.modules.modules.visuals.PlayerVisibility;
import dev.bkclient.systems.modules.modules.visuals.Removals;
import dev.bkclient.systems.modules.modules.visuals.ScoreboardTweaks;
import dev.bkclient.systems.modules.modules.visuals.SkinChanger;
import dev.bkclient.systems.modules.modules.visuals.ShowLore;
import dev.bkclient.systems.modules.modules.visuals.StorageESP;
import dev.bkclient.systems.modules.modules.visuals.SwingAnimation;
import dev.bkclient.systems.modules.modules.visuals.TNTTimer;
import dev.bkclient.systems.modules.modules.visuals.TargetESP;
import dev.bkclient.systems.modules.modules.visuals.Tracers;
import dev.bkclient.systems.modules.modules.visuals.Trails;
import dev.bkclient.systems.modules.modules.visuals.TrapESP;
import dev.bkclient.systems.modules.modules.visuals.ViewModel;
import dev.bkclient.systems.modules.modules.visuals.World;
import dev.bkclient.systems.modules.modules.visuals.XRay;
import net.minecraft.client.MinecraftClient;

public class ModuleManager {
   private final ClassLookupList<Module> modules = new ClassLookupList<>();
   private final EventListener<ClientPlayerTickEvent> tickListener;
   private final EventListener<HudRenderEvent> moduleWidgetRenderer;
   private final EventListener<KeyPressEvent> onKeyPress = event -> {
      if (MinecraftClient.getInstance().currentScreen == null) {
         for (Module module : this.getModules()) {
            if (module.getKey() == event.getKey() && module.getKey() != -1 && event.getAction() == 1) {
               if (module instanceof MenuModule menu && !menu.acceptsCurrentModifiers()) continue;
               module.toggle();
            }
         }
      }
   };
   private final EventListener<MouseEvent> onMouseButtonPress = event -> {
      if (MinecraftClient.getInstance().currentScreen == null) {
         for (Module module : this.getModules()) {
            if (module.getKey() == event.getButton() && module.getKey() != -1 && event.getAction() == 1) {
               if (module instanceof MenuModule menu && !menu.acceptsCurrentModifiers()) continue;
               module.toggle();
            }
         }
      }
   };

   public ModuleManager(EventListener<ClientPlayerTickEvent> tickListener, EventListener<HudRenderEvent> moduleWidgetRenderer) {
      this.tickListener = tickListener;
      this.moduleWidgetRenderer = moduleWidgetRenderer;
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }

   public void registerModules() {
      this.register(new Aura());

      this.register(new AutoTotem());
      this.register(new AutoTrap());
      this.register(new dev.bkclient.systems.modules.modules.combat.AutoWeb());
      this.register(new dev.bkclient.systems.modules.modules.combat.AutoCrystal());
      this.register(new dev.bkclient.systems.modules.modules.combat.AutoMace());
      this.register(new dev.bkclient.systems.modules.modules.combat.AnchorExploder());
      this.register(new dev.bkclient.systems.modules.modules.combat.PredictDoubleHand());
      this.register(new dev.bkclient.systems.modules.modules.combat.NoMissDelay());
      this.register(new dev.bkclient.systems.modules.modules.combat.GhostObby());
      this.register(new TriggerBot());
      this.register(new AutoGapple());

      this.register(new AimAssist());
      this.register(new AutoPotion());
      this.register(new AntiBot());
      this.register(new Velocity());
      this.register(new AutoArmor());
      this.register(new AutoDripstone());

      this.register(new ErrorKiller());
      this.register(new BackTrack());
      this.register(new dev.bkclient.systems.modules.modules.player.NoJumpDelay());
      this.register(new dev.bkclient.systems.modules.modules.combat.KnockbackDisplacement());
      this.register(new Hitboxes());
      this.register(new dev.bkclient.systems.modules.modules.combat.WTap());

      this.register(new AutoSprint());
      this.register(new WindHop());
      this.register(new NoWeb());
      this.register(new AirStuck());
      this.register(new Flight());
      this.register(new HighJump());

      this.register(new Strafe());

      this.register(new WaterSpeed());
      this.register(new Speed());
      this.register(new Timer());
      this.register(new NoSlow());
      this.register(new Spider());

      this.register(new MenuModule());
      this.register(new ESP());
      this.register(new dev.bkclient.systems.modules.modules.visuals.ShaderESP());
      this.register(new Removals());
      this.register(new dev.bkclient.systems.modules.modules.visuals.Beautifully());
      this.register(new Ambience());
      this.register(new dev.bkclient.systems.modules.modules.visuals.Sky());
      this.register(new SwingAnimation());
      this.register(new dev.bkclient.systems.modules.modules.visuals.ItemInspect());

      this.register(new ShowLore());
      this.register(new FriendMarkers());
      this.register(new Tracers());
      this.register(new Trails());
      this.register(new TNTTimer());
      this.register(new ViewModel());
      this.register(new TrapESP());
      this.register(new Blink());
      this.register(new Interface());
      this.register(new CustomChatModule());
      this.register(new TargetESP());
      this.register(new dev.bkclient.systems.modules.modules.visuals.PickBlowup());
      this.register(new StorageESP());
      this.register(new XRay());
      this.register(new AntiInvisible());
      this.register(new CustomFog());
      this.register(new dev.bkclient.systems.modules.modules.visuals.Blizzard());
      this.register(new World());
      this.register(new dev.bkclient.systems.modules.modules.visuals.Lyrics3D());
      this.register(new KillEffects());
      this.register(new Particles());
      this.register(new dev.bkclient.systems.modules.modules.visuals.PopChams());

      this.register(new AspectRatio());
      this.register(new dev.bkclient.systems.modules.modules.visuals.FreeLook());
      this.register(new dev.bkclient.systems.modules.modules.visuals.Zoom());
      this.register(new dev.bkclient.systems.modules.modules.visuals.SmoothCamera());
      this.register(new dev.bkclient.systems.modules.modules.visuals.Emotes());
      this.register(new Prediction());
      this.register(new BlockOutline());
      this.register(new ChinaHat());
      this.register(new JumpCircles());
      this.register(new GlassVapor());
      this.register(new dev.bkclient.systems.modules.modules.visuals.ArmorGlass());
      this.register(new Pet());
      this.register(new LogoutSpot());
      this.register(new ItemPhysics());
      this.register(new Crosshair());
      this.register(new CustomHotbar());
      this.register(new CustomSwords());
      this.register(new ScoreboardTweaks());
      this.register(new Capes());
      this.register(new SkinChanger());
      this.register(new HandsShader());
      this.register(new PlayerVisibility());
      this.register(new InventoryCleaner());
      this.register(new AutoInvisible());
      this.register(new MineHelper());
      this.register(new TargetPearl());
      this.register(new Stealer());
      this.register(new MiddleClick());
      this.register(new InvUtils());
      this.register(new InventorySet());
      this.register(new AutoEat());
      this.register(new FreeCam());
      this.register(new NoDelay());
      this.register(new PlayerUtils());
      this.register(new NoPush());

      this.register(new Scaffold());
      this.register(new ObjectInfo());
      this.register(new dev.bkclient.systems.modules.modules.visuals.Waypoints());
      this.register(new Nuker());
      this.register(new NoRotate());
      this.register(new NoInteract());
      this.register(new NoFall());
      this.register(new EffectRemover());
      this.register(new NameProtect());
      this.register(new StreamerMode());
      this.register(new ElytraUtils());
      this.register(new FastItemUse());
      this.register(new FastPlace());
      this.register(new AutoTool());
      this.register(new AutoSign());
      this.register(new ChatSpammer());
      this.register(new HitSound());

      this.register(new WindowScreen());
      this.register(new AutoMarket());
      this.register(new AutoPotions());
      this.register(new dev.bkclient.systems.modules.modules.bkmods.AutoZmianka());
      this.register(new Panic());

      this.register(new AutoAccept());
      this.register(new DeathCords());
      this.register(new AnticheatDetect());
      this.register(new AutoLeave());
      this.register(new AutoSwap());
      this.register(new RussianRoulette());
      this.register(new AutoDuels());
      this.register(new AutoAuth());
      this.register(new AutoJoin());
      this.register(new GuiMove());
      this.register(new Assist());
      this.register(new Sounds());
      this.register(new TestModule());
      this.register(new dev.bkclient.systems.modules.modules.visuals.LarpAna());
      this.register(new dev.bkclient.systems.modules.modules.other.PerformanceProfiler());
      // AirDrop is controlled exclusively from Configs; receiving is never a saved module toggle.
      this.register(new TinyTask());
   }

   public void enableModules() {
      for (Module module : this.modules) {
         if (module.getInfo().enabledByDefault()) {
            module.enable();
         }
      }
   }

   public void shutdownModules() {
      for (Module module : List.copyOf(this.modules)) {
         if (!module.isEnabled()) continue;
         try {
            module.setEnabled(false, true);
         } catch (Throwable error) {
            BKClientInit.LOGGER.error("Failed to shut down module {}", module.getName(), error);
         }
      }
      for (Module module : List.copyOf(this.modules)) {
         if (module instanceof AutoMarket autoMarket) {
            autoMarket.shutdown();
         }
      }
   }

   public void register(BaseModule module) {
      this.modules.add(module);
   }

   public <T extends Module> T getModule(String name) {
      if ("Auto-Zmianka".equalsIgnoreCase(name)) return (T)getModule(dev.bkclient.systems.modules.modules.bkmods.AutoZmianka.class);
      if ("Blink".equalsIgnoreCase(name)) return (T)getModule(Blink.class);
      if ("FreeCamera".equalsIgnoreCase(name.replace(" ", ""))) return (T)getModule(FreeCam.class);
      if ("ShaderESP".equalsIgnoreCase(name.replace(" ", ""))) {
         return (T)getModule(dev.bkclient.systems.modules.modules.visuals.ShaderESP.class);
      }
      return (T)this.modules
         .stream()
         .filter(module -> module.getName().replace(" ", "").equalsIgnoreCase(name) || module.getName().equalsIgnoreCase(name))
         .findFirst()
         .orElseThrow(() -> new UnknownModuleException(name));
   }

   public <T extends Module> T getModule(Class<T> clazz) {
      Module module = this.modules.firstOfClass(clazz);
      if (module == null) throw new UnknownModuleException(clazz.getSimpleName());
      return (T)module;
   }

   @Generated
   public List<Module> getModules() {
      return this.modules;
   }

   @Generated
   public EventListener<ClientPlayerTickEvent> getTickListener() {
      return this.tickListener;
   }

   @Generated
   public EventListener<HudRenderEvent> getModuleWidgetRenderer() {
      return this.moduleWidgetRenderer;
   }

   @Generated
   public EventListener<KeyPressEvent> getOnKeyPress() {
      return this.onKeyPress;
   }

   @Generated
   public EventListener<MouseEvent> getOnMouseButtonPress() {
      return this.onMouseButtonPress;
   }
}
