package dev.bkclient;

import java.util.ArrayList;
import java.util.List;

import lombok.Generated;
import dev.bkclient.framework.shader.GlProgram;
import dev.bkclient.systems.commands.CommandRegistry;
import dev.bkclient.systems.config.ConfigDropHandler;
import dev.bkclient.systems.config.ConfigManager;
import dev.bkclient.systems.discord.DiscordManager;
import dev.bkclient.systems.event.EventIntegration;
import dev.bkclient.systems.event.EventManager;
import dev.bkclient.systems.event.handlers.ServerConnectionHandler;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.friends.FriendManager;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.supabase.SupabaseService;
import dev.bkclient.systems.modules.ModuleManager;
import dev.bkclient.systems.modules.constructions.swinganim.SwingManager;
import dev.bkclient.systems.modules.constructions.swinganim.presets.SwingPresetManager;
import dev.bkclient.systems.modules.listeners.ModuleTickListener;
import dev.bkclient.systems.modules.listeners.ModuleWidgetRenderer;
import dev.bkclient.systems.notifications.NotificationManager;
import dev.bkclient.systems.poshalko.PoshalkoHandler;
import dev.bkclient.systems.target.TargetManager;
import dev.bkclient.systems.theme.ThemeManager;
import dev.bkclient.systems.waypoints.WayPointsManager;
import dev.bkclient.ui.hud.Hud;
import dev.bkclient.ui.mainmenu.AccountManager;
import dev.bkclient.ui.mainmenu.MenuBackground;
import dev.bkclient.ui.menu.MenuScreen;
import dev.bkclient.utility.game.TitleBarHelper;
import dev.bkclient.utility.concurrent.ClientAsyncExecutor;
import dev.bkclient.utility.game.server.TPSHandler;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.math.calculator.ChatListener;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.sprite.SpriteAnimationAtlas;
import dev.bkclient.systems.emoji.EmojiTextureLoader;
import dev.bkclient.utility.rotations.RotationHandler;
import dev.bkclient.utility.rotations.RotationUpdateListener;
import dev.bkclient.utility.sounds.MusicTracker;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum BKClientInit implements IMinecraft {
   INSTANCE;

   public static final String NAME = "BK-Client";
   public static final String BUILD_TYPE = "PreBeta";
   public static final String VERSION = "3.0.8";
   public static final String MOD_ID = "bkclient";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
   private EventManager eventManager;
   private ThemeManager themeManager;
   private ModuleManager moduleManager;
   private CommandRegistry commandManager;
   private FriendManager friendManager;
   private DiscordManager discordManager;
   private RotationHandler rotationHandler;
   private TargetManager targetManager;
   private MusicTracker musicTracker;
   private FileManager fileManager;
   private NotificationManager notificationManager;
   private ConfigManager configManager;
   private SwingManager swingManager;
   private TPSHandler tpsHandler;
   private Hud hud;
   private ServerConnectionHandler serverConnectionHandler;
   private PoshalkoHandler poshalkoHandler;
   private WayPointsManager wayPointsManager;
   private SwingPresetManager swingPresetManager;
   private MenuScreen menuScreen;
    private ChatListener chatListener;
    private List<AccountManager.Account> accounts = new ArrayList<>();
    private String selectedMenuBackground = MenuBackground.CINEMATIC.getUniqueId();
    private volatile boolean panic;
    private boolean shuttingDown;

   public void initialize() {
      LOGGER.info("Initializing {}...", NAME);
      dev.bkclient.utility.sounds.ClientSoundInstance.initializePlayback();
      this.musicTracker = new MusicTracker();
      this.wayPointsManager = new WayPointsManager();
      this.eventManager = new EventManager();
      this.friendManager = new FriendManager();
      this.themeManager = new ThemeManager();
      this.discordManager = new DiscordManager();
      this.rotationHandler = new RotationHandler(new RotationUpdateListener());
      this.targetManager = new TargetManager();
      this.fileManager = new FileManager();
      this.moduleManager = new ModuleManager(new ModuleTickListener(), new ModuleWidgetRenderer());
      this.hud = new Hud();
      this.tpsHandler = new TPSHandler();
      this.notificationManager = new NotificationManager();
      this.fileManager.registerClientFiles();
      this.moduleManager.registerModules();
      this.moduleManager.enableModules();
      this.configManager = new ConfigManager();
      this.configManager.handle();
      this.moduleManager.getModule(dev.bkclient.systems.modules.modules.bkmods.AutoMarket.class).finishInitialization();
      this.commandManager = new CommandRegistry();
      this.commandManager.initCommands();
      this.swingManager = new SwingManager();
      this.swingPresetManager = new SwingPresetManager();
      this.swingPresetManager.handle();
      this.fileManager.loadClientFiles();
      dev.bkclient.utility.game.SupportNotice.initialize();
      dev.bkclient.utility.game.ClientTelemetryUtility.start();
      dev.bkclient.utility.game.AutoMarketRemote.start();
      ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         public Identifier getFabricId() {
            return BKClientInit.id("after_shader_load");
         }

         public void reload(ResourceManager manager) {
            GlProgram.loadAndSetupPrograms();
         }
      });
      DrawUtility.initializeShaders();
      Localizator.loadTranslations();
      this.chatListener = new ChatListener();
      new dev.bkclient.systems.modules.modules.bkmods.AutoMarketHistoryManager();
      this.serverConnectionHandler = new ServerConnectionHandler();
      this.poshalkoHandler = new PoshalkoHandler();
      String osName = System.getProperty("os.name");
      if (osName.toLowerCase().contains("windows")) {
         this.discordManager.connect();
      }

      ConfigDropHandler.init();
      net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
         dev.bkclient.ui.clicksys.GuiWarmup::tick);
      net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
         dev.bkclient.systems.modules.modules.player.FreecamUtil::tick);
      net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
         dev.bkclient.ui.mainmenu.InGameAccountSwitcher::tick);
      net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.LAST.register(
         dev.bkclient.systems.modules.modules.player.FreecamUtil::renderGhost);
      ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
         dev.bkclient.systems.modules.modules.other.tinytask.TinyTaskBridge.recordChatOrCommand(command, true);
         return true;
      });
      ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
         dev.bkclient.systems.modules.modules.other.tinytask.TinyTaskBridge.recordChatOrCommand(message, false);
         return true;
      });
      TitleBarHelper.setDarkTitleBar();
      new EventIntegration();
      this.createAvatar();
      LOGGER.info("{} initialized", NAME);
   }

   public synchronized void shutdown() {
      if (this.shuttingDown) return;
      this.shuttingDown = true;
      LOGGER.info("Shutting down...");
      dev.bkclient.utility.concurrent.ShutdownTasks.run(
         error -> LOGGER.error("Shutdown step failed; continuing cleanup", error),
         () -> {
            if (this.fileManager != null) {
               this.fileManager.saveClientFiles();
            }
         }, () -> {
            if (!this.isPanic() && this.configManager != null) {
               // Always update the real autosave file. getAutoSaveConfig() may return the
               // active named config, which would leave autosave stale or missing.
               var config = this.configManager.getConfig("autosave", true);
               if (config == null) config = new dev.bkclient.systems.config.ConfigFile("autosave");
               config.save();
            }
         }, () -> {
            if (!this.isPanic() && this.swingPresetManager != null) {
               var preset = this.swingPresetManager.getAutoSavePreset();
               if (preset != null) preset.save();
            }
         }, () -> {
            if (this.moduleManager != null) {
               this.moduleManager.shutdownModules();
            }
         }, () -> {
            if (this.musicTracker != null) {
               this.musicTracker.shutdown();
            }
         }, () -> {
            if (this.discordManager != null) {
               this.discordManager.shutdown();
            }
         }, () -> {
            if (this.moduleManager != null) {
               var xray = this.moduleManager.getModule(dev.bkclient.systems.modules.modules.visuals.XRay.class);
               if (xray != null) xray.shutdownScanner();
            }
         },
         SupabaseService::stopIfInitialized,
         dev.bkclient.utility.game.ClientTelemetryUtility::shutdown,
         dev.bkclient.utility.game.AutoMarketRemote::shutdown,
         dev.bkclient.ui.mainmenu.CustomBackgroundLoader::shutdown,
         dev.bkclient.utility.render.AnimatedCape::shutdown,
         dev.bkclient.utility.render.CustomCape::shutdown,
         dev.bkclient.utility.chunkanimator.ChunkAnimator::clear,
         dev.bkclient.utility.game.prediction.ElytraPredictionSystem::clear,
         ClientAsyncExecutor::shutdown,
         EmojiTextureLoader::clear,
         dev.bkclient.ui.clicksys.port.render.TextShader::releaseFonts,
         dev.bkclient.ui.clicksys.port.render.ItemIconCache::update,
         dev.bkclient.ui.clicksys.port.render.MipmapCapture::release,
         dev.bkclient.ui.clicksys.port.render.BlurEffect::update2,
         dev.bkclient.ui.clicksys.port.render.GuiQuad::close,
         SpriteAnimationAtlas::clearAllAtlases,
         dev.bkclient.utility.render.DrawUtility::shutdown
      );
      // Panic remains latched until the process exits.
   }

   public void releaseWorldResources() {
      if (moduleManager == null) return;
      dev.bkclient.utility.concurrent.ShutdownTasks.run(
         error -> LOGGER.error("World resource cleanup failed", error),
         dev.bkclient.utility.render.DrawUtility::releaseWorldTargets,
         dev.bkclient.utility.BlockEntityTracker::clear,
         dev.bkclient.utility.chunkanimator.ChunkAnimator::clear,
         dev.bkclient.utility.game.prediction.ElytraPredictionSystem::clear,
         () -> moduleManager.getModule(dev.bkclient.systems.modules.modules.visuals.Particles.class).onWorldChanged(),
         () -> moduleManager.getModule(dev.bkclient.systems.modules.modules.visuals.World.class).onWorldChanged(),
         () -> moduleManager.getModule(dev.bkclient.systems.modules.modules.visuals.Sky.class).onWorldChanged(),
         () -> moduleManager.getModule(dev.bkclient.systems.modules.modules.visuals.Lyrics3D.class).onWorldChanged(),
         () -> moduleManager.getModule(dev.bkclient.systems.modules.modules.visuals.ArmorGlass.class).renderer.close(),
         () -> moduleManager.getModule(dev.bkclient.systems.modules.modules.visuals.TargetESP.class).onWorldChanged(),
         () -> moduleManager.getModule(dev.bkclient.systems.modules.modules.visuals.ItemInspect.class).onWorldChanged(),
         () -> moduleManager.getModule(dev.bkclient.systems.modules.modules.visuals.ESP.class).onWorldChanged(),
         () -> moduleManager.getModule(dev.bkclient.systems.modules.modules.visuals.XRay.class).onWorldChanged(),
         () -> moduleManager.getModule(dev.bkclient.systems.modules.modules.combat.BackTrack.class).onWorldChanged(),
         () -> moduleManager.getModule(dev.bkclient.systems.modules.modules.player.Blink.class).onWorldChanged(),
         dev.bkclient.utility.render.BKEngineShaderEspRenderer::release
      );
   }

   public static BKClientInit getInstance() {
      return INSTANCE;
   }

   public static Identifier id(String path) {
      return Identifier.of(MOD_ID, path);
   }

   private void createAvatar() {
   }

   @Generated
   public EventManager getEventManager() {
      return this.eventManager;
   }

   @Generated
   public ThemeManager getThemeManager() {
      return this.themeManager;
   }

   @Generated
   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   @Generated
   public CommandRegistry getCommandManager() {
      return this.commandManager;
   }

   @Generated
   public FriendManager getFriendManager() {
      return this.friendManager;
   }

   @Generated
   public DiscordManager getDiscordManager() {
      return this.discordManager;
   }

   @Generated
   public RotationHandler getRotationHandler() {
      return this.rotationHandler;
   }

   @Generated
   public TargetManager getTargetManager() {
      return this.targetManager;
   }

   @Generated
   public MusicTracker getMusicTracker() {
      return this.musicTracker;
   }

   @Generated
   public FileManager getFileManager() {
      return this.fileManager;
   }

   @Generated
   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }

   @Generated
   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   @Generated
   public SwingManager getSwingManager() {
      return this.swingManager;
   }

   @Generated
   public TPSHandler getTpsHandler() {
      return this.tpsHandler;
   }

   @Generated
   public Hud getHud() {
      return this.hud;
   }

   @Generated
   public ServerConnectionHandler getServerConnectionHandler() {
      return this.serverConnectionHandler;
   }

   @Generated
   public PoshalkoHandler getPoshalkoHandler() {
      return this.poshalkoHandler;
   }

   @Generated
   public WayPointsManager getWayPointsManager() {
      return this.wayPointsManager;
   }

   @Generated
   public SwingPresetManager getSwingPresetManager() {
      return this.swingPresetManager;
   }

   @Generated
   public MenuScreen getMenuScreen() {
      return this.menuScreen;
   }

   @Generated
   public ChatListener getChatListener() {
      return this.chatListener;
   }

    @Generated
    public List<AccountManager.Account> getAccounts() {
        return this.accounts;
    }

    @Generated
    public void setAccounts(List<AccountManager.Account> accounts) {
        this.accounts = accounts;
    }

    @Generated
    public String getSelectedMenuBackground() {
        return this.selectedMenuBackground;
    }

    @Generated
    public void setSelectedMenuBackground(String selectedMenuBackground) {
        this.selectedMenuBackground = selectedMenuBackground;
    }


    @Generated
    public boolean isPanic() {
        return this.panic;
    }

   @Generated
   public void setMenuScreen(MenuScreen menuScreen) {
      this.menuScreen = menuScreen;
   }

   @Generated
   public void setPanic(boolean panic) {
      this.panic |= panic;
   }
}
