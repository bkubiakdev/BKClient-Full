package dev.bkclient.systems.modules.modules.player;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.StartBreakBlockEvent;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.SettingsContainer;

import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.inventory.InventoryUtility;
import dev.bkclient.utility.inventory.slots.HotbarSlot;
import dev.bkclient.utility.render.Draw3DUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.rotations.MoveCorrection;
import dev.bkclient.utility.rotations.Rotation;
import dev.bkclient.utility.rotations.RotationPriority;
import dev.bkclient.utility.time.Timer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.text.Text;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.systems.setting.settings.ButtonSetting;

@ModuleInfo(name = "Mine Helper", category = ModuleCategory.PLAYER, desc = "Assistant in the mine")
public class MineHelper extends BaseModule {
    private static final int MAX_SELECTION_POSITIONS = 20;
    private static final int MAX_SELECTION_REGIONS = MAX_SELECTION_POSITIONS / 2;
    private static final long CHANNEL_WORLD_LOAD_DELAY_MS = 3000L;
    private static final long CHANNEL_SCAN_ONLY_MS = 4000L;
    private static final long CHANNEL_INPUT_RELEASE_AFTER_MS = 350L;

    /**
     * Marks that a chunk has received at least one full ChunkDataS2CPacket, keyed by packed
     * chunk x/z (mixin-fed, independent of the XRay module's enabled state). isChunkLoaded()
     * alone can't be trusted right after (re)connect or after /ch: the client can report a
     * stale cached chunk from before a channel swap, or the player can briefly be in a
     * lobby/hub world far from the selection with a perfectly "loaded" but irrelevant chunk.
     * Whether the region is actually empty is still decided by a fresh getBlockState() scan
     * (see findNearestBlockInRegion) - this set only answers "has real data arrived yet".
     */
    private static final java.util.Set<Long> scannedChunks = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void recordChunkDataPacket(net.minecraft.world.chunk.WorldChunk chunk, int chunkX, int chunkZ) {
        scannedChunks.add(net.minecraft.util.math.ChunkPos.toLong(chunkX, chunkZ));
    }

    public final SliderSetting percent = new SliderSetting(this, "Durability").step(1.0f).min(1.0f).max(70.0f)
            .currentValue(10.0f).suffix("%");
    public final BooleanSetting autoCaptcha = new BooleanSetting(this, "Auto Captcha",
            "Read the nearby mining challenge, submit its code and resume after server confirmation.",
            () -> !this.isAreaMode()).enable();
    private final dev.bkclient.utility.text.MiningChallengeGate miningChallenge = new dev.bkclient.utility.text.MiningChallengeGate();
    private final java.util.Map<java.util.UUID, String> captchaCodes = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Text> captchaHolograms = new java.util.HashMap<>();
    private net.minecraft.client.world.ClientWorld captchaDebugWorld;
    private long nextCaptchaHologramScan;

    private void scanCaptchaHolograms() {
        if (!autoCaptcha.isEnabled() || mc.world == null || mc.player == null) {
            captchaHolograms.clear();
            captchaCodes.clear();
            miningChallenge.reset();
            captchaDebugWorld = null;
            nextCaptchaHologramScan = 0;
            return;
        }
        if (captchaDebugWorld != mc.world) {
            captchaHolograms.clear();
            captchaCodes.clear();
            miningChallenge.reset();
            captchaDebugWorld = mc.world;
            nextCaptchaHologramScan = 0;
        }
        long now = System.nanoTime();
        if (now < nextCaptchaHologramScan) return;
        nextCaptchaHologramScan = now + 250_000_000L;
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
        java.util.Set<String> challengeCodes = new java.util.HashSet<>();
        boolean challengePresent = false;
        for (Entity entity : mc.world.getEntitiesByClass(Entity.class,
                mc.player.getBoundingBox().expand(8), e -> !e.isRemoved()
                        && (e instanceof TextDisplayEntity || e instanceof net.minecraft.entity.decoration.ArmorStandEntity))) {
            Text text = entity instanceof TextDisplayEntity display ? display.getText() : entity.getCustomName();
            if (text == null || text.getString().isBlank()) continue;
            String plain = text.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "").toLowerCase(java.util.Locale.ROOT);
            if (!plain.contains("/kopanie") || !plain.contains("aby kopać dalej")) continue;
            seen.add(entity.getUuid());
            if (!text.equals(captchaHolograms.get(entity.getUuid()))) {
                captchaHolograms.put(entity.getUuid(), text.copy());
                captchaCodes.remove(entity.getUuid());
                if (text.getString().contains("/kopanie")) {
                    String code = readHologramCode(text);
                    if (code != null) captchaCodes.put(entity.getUuid(), code);
                }
            }
            if (autoCaptcha.isEnabled() && entity.squaredDistanceTo(mc.player) <= 64
                    && plain.contains("/kopanie") && plain.contains("aby kopać dalej")) {
                challengePresent = true;
                String code = captchaCodes.get(entity.getUuid());
                if (code != null) challengeCodes.add(code);
            }
        }
        captchaHolograms.keySet().retainAll(seen);
        captchaCodes.keySet().retainAll(seen);
        if (!autoCaptcha.isEnabled()) { miningChallenge.reset(); return; }
        boolean canSend = mc.getOverlay() == null && mc.player.networkHandler != null;
        String command = miningChallenge.observe(challengePresent,
                canSend ? challengeCodes : java.util.Set.of(), now / 1_000_000L);
        if (command != null) {
            mc.player.networkHandler.sendChatCommand(command);
            BKClientInit.LOGGER.info("[MineHelper] Submitted mining challenge; waiting for server confirmation");
        }
    }

    private String readHologramCode(Text text) {
        // Read the live resource stack each time a new challenge arrives, not an old download.
        java.util.Map<Integer, Character> digits = new java.util.HashMap<>();
        try {
            for (var resource : mc.getResourceManager().getAllResources(
                    net.minecraft.util.Identifier.of("minecraft", "font/default.json"))) {
                try (var reader = resource.getReader()) {
                    dev.bkclient.utility.text.FontDigitReader.apply(
                            com.google.gson.JsonParser.parseReader(reader).getAsJsonObject(), digits);
                }
            }
            var code = dev.bkclient.utility.text.FontDigitReader.read(text.getString(), digits);
            return code.orElse(null);
        } catch (java.io.IOException | RuntimeException error) {
            BKClientInit.LOGGER.warn("[MineHelper] Could not read active digit font: {}", error.toString());
            return null;
        }
    }

    public final BooleanSetting autoReplace = new BooleanSetting((SettingsContainer) this, "Auto Replace",
            "Automatically changes pickaxe at low durability");
    public final BooleanSetting autoRepair = new BooleanSetting((SettingsContainer) this, "Auto Repair",
            "Sends the configured repair command at low durability");
    public final StringSetting repairCommand = new StringSetting(this, "Repair Command",
            () -> !this.autoRepair.isEnabled()).text("/repair");

    public final SliderSetting blockLimit = new SliderSetting(this, "Block Limit",
            () -> this.isAreaMode())
            .min(0.0f).max(100.0f).step(1.0f).currentValue(6.0f);
    public final ModeSetting mineMode = new ModeSetting(this, "Mine Mode");
    public final ModeSetting.Value squareMode = new ModeSetting.Value(mineMode, "Square").select();
    public final ModeSetting.Value lineMode = new ModeSetting.Value(mineMode, "Line");
    // Area: mine WorldEdit-style selected regions (.mine pos1..pos20) in order.
    public final ModeSetting.Value areaMode = new ModeSetting.Value(mineMode, "Area");
    public final SliderSetting areaPatternWidth = new SliderSetting(this, "Area Width",
            () -> !this.isAreaMode())
            .min(1.0f).max(5.0f).step(1.0f).currentValue(2.0f);
    public final SliderSetting areaPatternHeight = new SliderSetting(this, "Area Height",
            () -> !this.isAreaMode())
            .min(1.0f).max(5.0f).step(1.0f).currentValue(2.0f);
    public final BooleanSetting autoChannelSwitch = new BooleanSetting(this, "Auto Channel",
            "Changes channel with /ch in Area", () -> !this.isChannelMode()).enabled(true);
    public final BooleanSetting areaAntiStuck = new BooleanSetting(this, "Area Anti Stuck",
            "Tries a short left/right sidestep when Area movement gets blocked",
            () -> !this.isAreaMode()).enabled(true);
    public final BooleanSetting autoDrop = new BooleanSetting(this, "Auto Drop");
    public final SliderSetting dropInterval = new SliderSetting(this, "Drop Interval")
            .min(1.0f).max(60.0f).step(1.0f).currentValue(1.0f).suffix("min");
    public final int[] slotStates = new int[36]; // 0 = off, 1 = green border, 2 = blue bg
    // Hidden setting that persists slotStates to config
    public final dev.bkclient.systems.setting.impl.AbstractSetting slotStatesSetting = new dev.bkclient.systems.setting.impl.AbstractSetting(this, "SlotStates", () -> true) {
        @Override
        public com.google.gson.JsonElement save() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < slotStates.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(slotStates[i]);
            }
            return new com.google.gson.JsonPrimitive(sb.toString());
        }
        @Override
        public void load(com.google.gson.JsonElement element) {
            try {
                String[] parts = element.getAsString().split(",");
                for (int i = 0; i < Math.min(parts.length, slotStates.length); i++) {
                    slotStates[i] = Integer.parseInt(parts[i].trim());
                }
            } catch (Exception ignored) {}
        }
    };
    public final StringSetting command1 = new StringSetting(this, "Komenda 1").text("");
    public final SliderSetting interval1 = new SliderSetting(this, "Co ile 1")
            .min(1.0f).max(60.0f).step(1.0f).currentValue(5.0f).suffix("min");
    public final StringSetting command2 = new StringSetting(this, "Komenda 2").text("");
    public final SliderSetting interval2 = new SliderSetting(this, "Co ile 2")
            .min(1.0f).max(60.0f).step(1.0f).currentValue(10.0f).suffix("min");
    public final StringSetting command3 = new StringSetting(this, "Komenda 3").text("");
    public final SliderSetting interval3 = new SliderSetting(this, "Co ile 3")
            .min(1.0f).max(60.0f).step(1.0f).currentValue(30.0f).suffix("min");

    public final BooleanSetting autoCraft = new BooleanSetting((SettingsContainer) this, "Auto Craft",
            "Sends /cx when 9 full stacks of cobblestone");

    public final BooleanSetting autoEat = new BooleanSetting((SettingsContainer) this, "Auto Eat",
            "Automatically eats food");
    public final SliderSetting hungerThreshold = new SliderSetting(this, "Hunger Threshold").min(0.0f).max(20.0f)
            .step(1.0f).currentValue(14.0f);

    public final BooleanSetting selectionRenderEnabled = new BooleanSetting(this, "Show Selection",
            "Renders the selected mining area", () -> !this.isAreaMode()).enabled(true);
    public final BooleanSetting selectionFillEnabled = new BooleanSetting(this, "Fill Selection",
            "Renders a translucent fill in addition to the outline", () -> !this.isAreaMode()).enabled(true);
    public final ColorSetting selectionColor = new ColorSetting(this, "Selection Color",
            () -> !this.isAreaMode()).color(Colors.ACCENT);
    private final java.util.Map<Integer, BlockPos> selectionPositions = new java.util.HashMap<>();
    public final dev.bkclient.systems.setting.impl.AbstractSetting selectionPositionsSetting = new dev.bkclient.systems.setting.impl.AbstractSetting(this, "AreaSelectionPositions", () -> true) {
        @Override
        public com.google.gson.JsonElement save() {
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            for (int id = 1; id <= MAX_SELECTION_POSITIONS; id++) {
                BlockPos pos = selectionPositions.get(id);
                if (pos == null) {
                    continue;
                }
                com.google.gson.JsonObject object = new com.google.gson.JsonObject();
                object.addProperty("id", id);
                object.addProperty("x", pos.getX());
                object.addProperty("y", pos.getY());
                object.addProperty("z", pos.getZ());
                array.add(object);
            }
            return array;
        }

        @Override
        public void load(com.google.gson.JsonElement element) {
            selectionPositions.clear();
            try {
                if (element == null || !element.isJsonArray()) {
                    activeRegionIndex = 0;
                    resetAreaScan();
                    return;
                }
                for (com.google.gson.JsonElement entry : element.getAsJsonArray()) {
                    if (!entry.isJsonObject()) {
                        continue;
                    }
                    com.google.gson.JsonObject object = entry.getAsJsonObject();
                    int id = object.get("id").getAsInt();
                    if (id < 1 || id > MAX_SELECTION_POSITIONS) {
                        continue;
                    }
                    int x = object.get("x").getAsInt();
                    int y = object.get("y").getAsInt();
                    int z = object.get("z").getAsInt();
                    selectionPositions.put(id, new BlockPos(x, y, z));
                }
            } catch (Exception ignored) {
                selectionPositions.clear();
            }
            if (!hasRegionSelection(activeRegionIndex)) {
                int firstRegion = findFirstSelectionRegion();
                activeRegionIndex = firstRegion >= 0 ? firstRegion : 0;
            }
            resetAreaScan();
        }
    };
    private int activeRegionIndex = 0;

    private BlockPos areaTargetPos = null;
    private int areaCurLayerY = Integer.MIN_VALUE;
    private int areaCurStripX = Integer.MIN_VALUE;
    private int areaCurZ = Integer.MIN_VALUE;
    private boolean areaReversedPass = false;
    private float areaCurrentYaw;
    private float areaCurrentPitch;
    private boolean areaFirstTick = true;
    private long areaTargetStartTime = 0L;
    private long areaLastHitResetTime = 0L;
    private int areaSkipTicks = 0;
    private Vec3d areaLastMoveTarget = null;
    private Vec3d areaLastPlayerPos = null;
    private double areaLastMoveDistanceSq = Double.MAX_VALUE;
    private int areaBlockedTicks = 0;
    private int areaCollisionTicks = 0;
    private int areaAvoidanceTicks = 0;
    private int areaAvoidanceCooldown = 0;
    private int areaAvoidanceDirection = 1;
    private int areaAvoidanceAttempts = 0;
    private long areaZoneBoundaryCooldownUntil = 0L;
    private final java.util.Random areaAvoidanceRandom = new java.util.Random();

    /**
     * ReceivePacketEvent fires on the Netty network thread, not the render/game thread. The
     * old code called finishChannelSwitch()/retryChannelSwitch() directly from there, and
     * finishChannelSwitch() closes the current screen (touches GL state) - that throws
     * "RenderSystem called from wrong thread" and aborts finishChannelSwitch() halfway through,
     * leaving channelResumeMiningAt/channelScanOnlyUntil unset and the module stuck. Queue the
     * normalized message instead and drain it from onUpdateEvent, which does run on the game
     * thread.
     */
    private final java.util.concurrent.ConcurrentLinkedQueue<String> pendingChannelMessages =
            new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final dev.bkclient.utility.time.MiningCommandTimer commandSchedule = new dev.bkclient.utility.time.MiningCommandTimer();
    private final java.util.Random channelRandom = new java.util.Random();
    private long lastCommandGlobalTime = 0;
    private record PendingMineCommand(String text, int slot, net.minecraft.client.network.ClientPlayNetworkHandler connection) {}
    private PendingMineCommand pendingMineCommand;
    private int commandQuietTicks;
    private int commandResumeTicks;

    private int movementTicks = 0;
    private float ticksPerBlock = 4.5f;
    private boolean positionInitialized = false;
    private int totalCycles = 0;
    private int loopStep = 0;
    private int direction = 1;

    private final dev.bkclient.utility.inventory.AutoEating eating = new dev.bkclient.utility.inventory.AutoEating();
    private final EventListener<dev.bkclient.systems.event.impl.game.GameTickEvent> onEatingLifecycle = event ->
            eating.validate(mc, autoEat.isEnabled());

    private boolean isDropping = false;
    private int dropState = 0;
    private int dropTimer = 0;
    private long lastDropTime = 0;

    private long globalTickCounter = 0;
    private net.minecraft.util.math.BlockPos currentBreakingPos = null;
    private boolean channelSwitching = false;
    private boolean channelClicked = false;
    private long channelNextActionTime = 0L;
    private long channelCommandSentTime = 0L;
    private long channelClickTime = 0L;
    private int channelAttempt = 0;
    private long channelResumeMiningAt = 0L;
    private long channelScanOnlyUntil = 0L;
    private long channelInputReleaseUntil = 0L;


    private final Timer timer = new Timer();
    private final Timer repairTimer = new Timer();
    private final Timer autoCraftTimer = new Timer();
    private boolean rotate;

    public MineHelper() {
        this.getSettings().clear();
        this.getSettings().add(this.autoCaptcha);
        this.getSettings().add(this.percent);
        this.getSettings().add(this.autoReplace);
        this.getSettings().add(this.autoRepair);
        this.getSettings().add(this.repairCommand);
        this.getSettings().add(this.autoDrop);
        this.getSettings().add(this.autoCraft);
        this.getSettings().add(this.autoEat);
        this.getSettings().add(this.hungerThreshold);
        this.getSettings().add(this.blockLimit);
        this.getSettings().add(this.mineMode);
        this.getSettings().add(this.areaPatternWidth);
        this.getSettings().add(this.areaPatternHeight);
        this.getSettings().add(this.autoChannelSwitch);
        this.getSettings().add(this.areaAntiStuck);
        this.getSettings().add(this.dropInterval);
        this.getSettings().add(this.command1);
        this.getSettings().add(this.interval1);
        this.getSettings().add(this.command2);
        this.getSettings().add(this.interval2);
        this.getSettings().add(this.command3);
        this.getSettings().add(this.interval3);
        this.getSettings().add(this.selectionRenderEnabled);
        this.getSettings().add(this.selectionFillEnabled);
        this.getSettings().add(this.selectionColor);
        this.getSettings().add(this.slotStatesSetting);
        this.getSettings().add(this.selectionPositionsSetting);
        // Mine AFK GUI nie zalezy od baritone \u2014 przycisk zawsze widoczny.
        // (wczesniej hideCondition chowal go u osob bez baritone)
        new ButtonSetting(this, "Mine AFK GUI").action(() -> {
            if (mc.player != null) {
                mc.setScreen(new dev.bkclient.ui.menu.csgui.CsMineAfkScreen(this));
            }
        });
    }


    /** id is 1-20 (matching ".mine pos1".."pos20"). Region index = (id-1)/2. */
    public boolean isAreaMode() { return this.mineMode.is(this.areaMode); }

    public boolean isChannelMode() { return this.isAreaMode(); }

    public void setSelectionPos(int id, BlockPos pos) {
        if (id < 1 || id > MAX_SELECTION_POSITIONS || pos == null) {
            return;
        }
        int regionIndex = (id - 1) / 2;
        this.selectionPositions.put(id, pos.toImmutable());
        if (!this.hasRegionSelection(this.activeRegionIndex)) {
            this.activeRegionIndex = regionIndex;
        }
        this.resetAreaScan();
        dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();
    }

    public BlockPos getSelectionPos(int id) {
        return this.selectionPositions.get(id);
    }

    public void clearSelection() {
        this.selectionPositions.clear();
        this.activeRegionIndex = 0;
        this.resetAreaScan();
        dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();
    }

    /** Restarts the boustrophedon scan from the selection's corner on the next findNextAreaBlock() call. */
    private void resetAreaScan() {
        this.areaTargetPos = null;
        this.areaCurLayerY = Integer.MIN_VALUE;
        this.areaCurStripX = Integer.MIN_VALUE;
        this.areaCurZ = Integer.MIN_VALUE;
        this.areaReversedPass = false;
        this.areaTargetStartTime = 0L;
        this.areaLastHitResetTime = 0L;
        this.areaSkipTicks = 0;
        this.resetAreaAvoidance();
    }

    private void resetAreaAvoidance() {
        this.areaLastMoveTarget = null;
        this.areaLastPlayerPos = null;
        this.areaLastMoveDistanceSq = Double.MAX_VALUE;
        this.areaBlockedTicks = 0;
        this.areaCollisionTicks = 0;
        this.areaAvoidanceTicks = 0;
        this.areaAvoidanceCooldown = 0;
        this.areaAvoidanceDirection = 1;
        this.areaAvoidanceAttempts = 0;
        this.areaZoneBoundaryCooldownUntil = 0L;
    }

    public boolean hasSelection() {
        return this.findFirstSelectionRegion() >= 0;
    }

    public int getMaxSelectionPositions() {
        return MAX_SELECTION_POSITIONS;
    }

    /**
     * Returns the exact block the crosshair is aimed at, falling back to the player's feet
     * if nothing is hit. Uses its own long-range raycast (200 blocks) instead of vanilla's
     * {@code mc.crosshairTarget}, since that one is capped to the player's short interaction
     * reach (~3-6 blocks) and would silently fall back to "feet" for anything further away,
     * which looked like "wrong" selection.
     */
    public BlockPos getTargetedOrFeetPos() {
        if (mc.player == null || mc.world == null) {
            return null;
        }
        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(200.0));
        HitResult hit = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE, (Entity) mc.player));
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos();
        }
        return mc.player.getBlockPos();
    }

    private BlockPos[] getRegionPositions(int regionIndex) {
        int id1 = regionIndex * 2 + 1;
        int id2 = regionIndex * 2 + 2;
        return new BlockPos[]{this.selectionPositions.get(id1), this.selectionPositions.get(id2)};
    }

    private boolean hasRegionSelection(int regionIndex) {
        if (regionIndex < 0 || regionIndex >= MAX_SELECTION_REGIONS) {
            return false;
        }
        BlockPos[] region = this.getRegionPositions(regionIndex);
        return region[0] != null && region[1] != null;
    }

    private int findFirstSelectionRegion() {
        return this.findNextSelectionRegion(0);
    }

    private int findNextSelectionRegion(int startRegion) {
        for (int regionIndex = Math.max(0, startRegion); regionIndex < MAX_SELECTION_REGIONS; regionIndex++) {
            if (this.hasRegionSelection(regionIndex)) {
                return regionIndex;
            }
        }
        return -1;
    }

    /**
     * True while at least one selected region still has chunks we haven't received a scan
     * result for yet. Callers use this to tell "genuinely empty" apart from "don't know yet"
     * (fresh join, mid-/ch, lobby/hub world before teleport, etc.) without guessing from player
     * position or isChunkLoaded().
     */
    private boolean anyRegionScanUnknown() {
        for (int regionIndex = 0; regionIndex < MAX_SELECTION_REGIONS; regionIndex++) {
            BlockPos[] region = this.getRegionPositions(regionIndex);
            if (region[0] == null || region[1] == null) {
                continue;
            }
            int minX = Math.min(region[0].getX(), region[1].getX());
            int maxX = Math.max(region[0].getX(), region[1].getX());
            int minZ = Math.min(region[0].getZ(), region[1].getZ());
            int maxZ = Math.max(region[0].getZ(), region[1].getZ());
            if (!this.isRegionScanReady(minX, maxX, minZ, maxZ)) {
                return true;
            }
        }
        return false;
    }

    private boolean ensureActiveRegionSelection() {
        if (this.hasRegionSelection(this.activeRegionIndex)) {
            return true;
        }
        int firstRegion = this.findFirstSelectionRegion();
        if (firstRegion < 0) {
            return false;
        }
        this.activeRegionIndex = firstRegion;
        this.resetAreaScan();
        return true;
    }

    private boolean switchToNearestFreeSelectionRegion() {
        int nextRegion = this.findNearestFreeSelectionRegion();
        if (nextRegion < 0) {
            return false;
        }
        this.activeRegionIndex = nextRegion;
        this.resetAreaScan();
        this.stopAreaMovement();
        if (mc.interactionManager != null && mc.player != null && mc.world != null && this.currentBreakingPos != null) {
            mc.interactionManager.cancelBlockBreaking();
            this.currentBreakingPos = null;
        }
        return true;
    }

    private int findNearestFreeSelectionRegion() {
        if (mc.player == null || mc.world == null) {
            return -1;
        }
        Vec3d eye = mc.player.getEyePos();
        int bestRegion = -1;
        double bestScore = Double.MAX_VALUE;
        for (int regionIndex = 0; regionIndex < MAX_SELECTION_REGIONS; regionIndex++) {
            BlockPos nearestBlock = this.findNearestBlockInRegion(regionIndex);
            if (nearestBlock == null) {
                continue;
            }
            // Don't skip the whole region just because another player is somewhere near it -
            // that made the bot abandon a region with plenty of blocks left and fire /ch
            // instead, even though only a small part of the region was actually blocked. The
            // per-block scan (findNearestBlockInRegion/findNextAreaBlock) and normal digging
            // already can't target a block a player is standing in, so region-wide avoidance
            // was pure loss with no upside.
            Vec3d center = Vec3d.ofCenter(nearestBlock);
            double dx = center.x - eye.x;
            double dy = center.y - eye.y;
            double dz = center.z - eye.z;
            double score = dx * dx + dy * dy + dz * dz;
            if (score < bestScore) {
                bestScore = score;
                bestRegion = regionIndex;
            }
        }
        return bestRegion;
    }

    private Box getSelectionBox(int regionIndex) {
        BlockPos[] region = this.getRegionPositions(regionIndex);
        if (region[0] == null || region[1] == null) {
            return null;
        }
        int minX = Math.min(region[0].getX(), region[1].getX());
        int minY = Math.min(region[0].getY(), region[1].getY());
        int minZ = Math.min(region[0].getZ(), region[1].getZ());
        int maxX = Math.max(region[0].getX(), region[1].getX()) + 1;
        int maxY = Math.max(region[0].getY(), region[1].getY()) + 1;
        int maxZ = Math.max(region[0].getZ(), region[1].getZ()) + 1;
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private java.util.List<Box> getSelectionBoxes() {
        java.util.List<Box> boxes = new java.util.ArrayList<>();
        for (int regionIndex = 0; regionIndex < MAX_SELECTION_REGIONS; regionIndex++) {
            Box box = this.getSelectionBox(regionIndex);
            if (box != null) {
                boxes.add(box);
            }
        }
        return boxes;
    }

    @SuppressWarnings("unused")
    private final EventListener<Render3DEvent> onSelectionRender3D = event -> {
        if (!this.isAreaMode() || !this.selectionRenderEnabled.isEnabled()) {
            return;
        }
        java.util.List<Box> boxes = this.getSelectionBoxes();
        if (boxes.isEmpty()) {
            return;
        }

        MatrixStack matrices = event.getMatrices();
        Camera camera = event.getCamera();
        ColorRGBA color = this.selectionColor.getColor();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        matrices.push();
        RenderUtility.prepareMatrices(matrices);

        BufferBuilder outlineBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES,
                VertexFormats.POSITION_COLOR);
        for (Box box : boxes) {
            Draw3DUtility.renderOutlinedBox(matrices, outlineBuffer, box, color.withAlpha(255.0f));
        }
        BuiltBuffer builtOutline = outlineBuffer.endNullable();
        if (builtOutline != null) {
            BufferRenderer.drawWithGlobalProgram(builtOutline);
        }

        if (this.selectionFillEnabled.isEnabled()) {
            BufferBuilder fillBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_COLOR);
            for (Box box : boxes) {
                Draw3DUtility.renderFilledBox(matrices, fillBuffer, box, color.withAlpha(40.0f));
            }
            BuiltBuffer builtFill = fillBuffer.endNullable();
            if (builtFill != null) {
                BufferRenderer.drawWithGlobalProgram(builtFill);
            }
        }

        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    };

    @Override
    public java.util.List<dev.bkclient.systems.setting.Setting> getDisplaySettings() {
        return this.getSettings().stream()
                .filter(s -> s != blockLimit && s != mineMode
                          && s != dropInterval
                          && s != command1 && s != interval1
                          && s != command2 && s != interval2
                          && s != command3 && s != interval3)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        
        this.positionInitialized = false;
        this.lastDropTime = System.currentTimeMillis();
        this.totalCycles = 0;
        this.globalTickCounter = 0;
        this.currentBreakingPos = null;
        this.resetChannelSwitchState();
        // Give the module a scan-only grace period on enable too, same as after a /ch switch -
        // otherwise enabling Area right after (re)joining the server (or right when a fresh
        // selection is made) can hit a selection that reads as "empty" for a tick or two before
        // the world/chunks are fully settled, and fire an unnecessary /ch immediately.
        long enableNow = System.currentTimeMillis();
        this.channelResumeMiningAt = 0L;
        this.channelScanOnlyUntil = enableNow + CHANNEL_SCAN_ONLY_MS;
        this.channelInputReleaseUntil = 0L;
        this.resetAreaScan();
        this.areaFirstTick = true;
        
        // Prevent commands from firing immediately on enable
        long now = System.currentTimeMillis();
        pendingMineCommand=null;commandQuietTicks=0;commandResumeTicks=0;
        commandSchedule.reset(now, command1.getText(), command2.getText(), command3.getText());
        lastCommandGlobalTime = now;
    }

    @Override
    public void onDisable() {
        stopEating();
        pendingMineCommand=null;commandQuietTicks=0;commandResumeTicks=0;
        miningChallenge.reset();
        captchaCodes.clear();
        captchaHolograms.clear();
        captchaDebugWorld = null;
        nextCaptchaHologramScan = 0;
        super.onDisable();
        // cancel any ongoing block break so the server doesn't keep a half-broken (shadow) block
        if (mc.interactionManager != null && mc.player != null && mc.world != null && this.currentBreakingPos != null) {
            mc.interactionManager.cancelBlockBreaking();
        }
        this.currentBreakingPos = null;
        this.resetChannelSwitchState();
        this.channelResumeMiningAt = 0L;
        this.channelScanOnlyUntil = 0L;
        this.channelInputReleaseUntil = 0L;
        this.resetAreaAvoidance();
        if (mc.options != null) {
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.attackKey.setPressed(false);
            mc.options.useKey.setPressed(false);
        }
    }

    @SuppressWarnings("unused")
    private final EventListener<StartBreakBlockEvent> onStartBreakBlockEvent = event -> {
        if (MineHelper.mc.player == null) {
            return;
        }
        ItemStack currentStack = MineHelper.mc.player.getMainHandStack();
        if (!this.isMiningTool(currentStack)) {
            return;
        }
        double durabilityPercent = this.getDurabilityPercent(currentStack);
        if (durabilityPercent >= (double) this.percent.getCurrentValue()) {
            return;
        }
        event.cancel();
        this.handleLowDurability(currentStack);
    };

    @SuppressWarnings("unused")
    private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
        Packet<?> packet = event.getPacket();
        if (packet instanceof GameMessageS2CPacket messagePacket) {
            if (dev.bkclient.utility.text.MiningChallengeGate.isSuccessMessage(messagePacket.content().getString())) {
                var connection = mc.getNetworkHandler();
                mc.execute(() -> {
                    if (mc.getNetworkHandler() == connection && this.isEnabled() && autoCaptcha.isEnabled()
                            && miningChallenge.confirm()) {
                        BKClientInit.LOGGER.info("[MineHelper] Mining challenge accepted; resuming mining");
                    }
                });
            }
            this.handleChannelMessage(messagePacket.content());
        }
    };

    @SuppressWarnings("unused")
    private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            stopEating();
            return;
        }
        this.scanCaptchaHolograms();
        if (autoCaptcha.isEnabled() && miningChallenge.waiting() && mc.player != null) {
            stopEating();
            this.stopMovement();
            this.stopAreaMovement();
            if (mc.interactionManager != null) mc.interactionManager.cancelBlockBreaking();
            this.currentBreakingPos = null;
            return;
        }
        if (MineHelper.mc.player == null) {
            return;
        }

        this.drainPendingChannelMessages();

        if (this.rotate) {
            BKClientInit.getInstance().getRotationHandler().rotate(new Rotation(MineHelper.mc.player.getYaw(), 90.0f),
                    MoveCorrection.SILENT, 180.0f, 180.0f, 180.0f, RotationPriority.USE_ITEM);
        }

        if (this.channelSwitching) {
            stopEating();
            this.handleChannelSwitchTick();
            return;
        }

        if (this.handleEating()) return;
        if (this.handlePendingMineCommand()) return;
        this.handleCommands();
        this.handleAutoCraft();
        if (this.handlePendingMineCommand()) return;

        this.handleDropping();
        if (this.isDropping)
            return;

        this.handleMovement();
        this.handleDigging();
    };

    private void handleAutoCraft() {
        if (!autoCraft.isEnabled() || pendingMineCommand!=null) return;
        if (!autoCraftTimer.finished(5000L)) return;

        long now=System.currentTimeMillis();
        if (now-lastCommandGlobalTime<3000L || mc.player.networkHandler==null) return;
        if (hasCraftableCobblestone(mc.player.getInventory().main)) {
            queueMineCommand("/cx",-1);
        }
    }

    public static boolean hasCraftableCobblestone(Iterable<ItemStack> inventory) {
        int fullStacks=0;
        for(ItemStack stack:inventory) {
            if(stack.isOf(Items.COBBLESTONE)&&stack.getCount()==64&&++fullStacks>=9)return true;
        }
        return false;
    }

    private void handleCommands() {
        long now = System.currentTimeMillis();
        checkAndRunCommand(0, command1, interval1, now);
        checkAndRunCommand(1, command2, interval2, now);
        checkAndRunCommand(2, command3, interval3, now);
    }

    private void checkAndRunCommand(int slot, StringSetting cmd, SliderSetting interval, long now) {
        if (pendingMineCommand!=null) return;
        String text = cmd.getText().strip();
        if (text.isEmpty() || interval.getCurrentValue() <= 0)
            return;

        if (commandSchedule.due(slot, text, (long)(interval.getCurrentValue() * 60000L), now)) {
            if (now - lastCommandGlobalTime < 3000L)
                return;

            queueMineCommand(text,slot);
        }
    }

    private void queueMineCommand(String text,int slot) {
        pendingMineCommand=new PendingMineCommand(text,slot,mc.player.networkHandler);
        commandQuietTicks=4;
    }

    /** Reproduce the short mining pause of opening a screen, without opening any GUI. */
    private boolean handlePendingMineCommand() {
        if(pendingMineCommand==null && commandResumeTicks==0)return false;
        stopMovement();stopAreaMovement();
        mc.options.attackKey.setPressed(false);
        if(mc.interactionManager!=null)mc.interactionManager.cancelBlockBreaking();
        currentBreakingPos=null;
        if(pendingMineCommand==null){commandResumeTicks--;return true;}
        var pending=pendingMineCommand;
        if(mc.player.networkHandler!=pending.connection()){
            pendingMineCommand=null;commandQuietTicks=0;return true;
        }
        if(commandQuietTicks-->0)return true;
        pendingMineCommand=null;
        if(pending.slot()<0 && (!autoCraft.isEnabled() || !hasCraftableCobblestone(mc.player.getInventory().main)))return true;
        if(pending.slot()>=0){
            String current=switch(pending.slot()){case 0->command1.getText();case 1->command2.getText();default->command3.getText();};
            if(!current.strip().equals(pending.text()))return true;
        }
        if(pending.text().startsWith("/"))mc.player.networkHandler.sendChatCommand(pending.text().substring(1));
        else mc.player.networkHandler.sendChatMessage(pending.text());
        long now=System.currentTimeMillis();
        if(pending.slot()<0){
            autoCraftTimer.reset();
            BKClientInit.LOGGER.info("[MineHelper] Auto Craft: sent /cx with at least 9 full cobblestone stacks");
        }else{
            commandSchedule.sent(pending.slot(),now);
            BKClientInit.LOGGER.info("[MineHelper] Sent scheduled command from row {}",pending.slot()+1);
        }
        lastCommandGlobalTime=now;commandResumeTicks=3;
        return true;
    }

    public void stopEating() { eating.stop(mc); }

    private boolean handleEating() {
        AutoEat standalone = BKClientInit.getInstance().getModuleManager().getModule(AutoEat.class);
        boolean active;
        if (standalone != null && standalone.isEnabled()) {
            stopEating();
            active = standalone.tickEating();
        } else {
            active = eating.tick(mc, autoEat.isEnabled(), (int) hungerThreshold.getCurrentValue());
        }
        if (active) {
            stopMovement();
            stopAreaMovement();
            mc.options.attackKey.setPressed(false);
            currentBreakingPos = null;
        }
        return active;
    }

    private void handleDropping() {
        if (!isDropping) {
            if (autoDrop.isEnabled()) {
                long now = System.currentTimeMillis();
                if (now - lastDropTime >= (long) dropInterval.getCurrentValue() * 60000L) {
                    isDropping = true;
                    dropState = 0;
                    dropTimer = 0;
                    stopMovement();
                }
            }
            return;
        }

        dropTimer++;
        switch (dropState) {
            case 0:
                if (dropTimer >= 40) {
                    dropState = 1;
                    dropTimer = 0;
                }
                break;
            case 1:
                if (dropTimer >= 4) {
                    boolean droppedAny = false;
                    for (int i = 0; i < 36; i++) {
                        if (slotStates[i] > 0) {
                            ItemStack stack = mc.player.getInventory().getStack(i);
                            if (!stack.isEmpty()) {
                                int slotId = i < 9 ? i + 36 : i;
                                if (slotStates[i] == 1) {
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slotId, 1,
                                            net.minecraft.screen.slot.SlotActionType.THROW, mc.player);
                                    droppedAny = true;
                                    dropTimer = 0;
                                    break;
                                } else if (slotStates[i] == 2 && stack.getCount() > 1) {
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slotId, 0,
                                            net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slotId, 1,
                                            net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
                                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, -999, 0,
                                            net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
                                    droppedAny = true;
                                    dropTimer = 0;
                                    break;
                                }
                            }
                        }
                    }
                    if (!droppedAny) {
                        dropState = 2;
                        dropTimer = 0;
                    }
                }
                break;
            case 2:
                if (dropTimer >= 20) {
                    isDropping = false;
                    lastDropTime = System.currentTimeMillis();
                }
                break;
        }
    }

    private void handleChannelMessage(Text text) {
        if (text == null) {
            return;
        }
        String message = this.normalizeServerMessage(text.getString());
        if (message.isEmpty()) {
            return;
        }
        // This runs on the Netty network thread (ReceivePacketEvent), not the game thread -
        // finishChannelSwitch()/retryChannelSwitch() touch GL state (closeHandledScreen) and
        // must not run here. Queue the message and process it from onUpdateEvent instead.
        this.pendingChannelMessages.add(message);
    }

    private void drainPendingChannelMessages() {
        String message;
        while ((message = this.pendingChannelMessages.poll()) != null) {
            if (this.isAreaZonePermissionMessage(message)) {
                
                this.handleAreaZonePermissionMessage();
                continue;
            }
            if (!this.channelSwitching) {
                continue;
            }
            if (message.contains("laczenie z kanalem") || message.contains("lacznie z kanalem")) {
                this.finishChannelSwitch();
                continue;
            }
            if (message.contains("ten kanal jest pelny") || message.contains("whitelist")
                    || message.contains("wybierz inny") || message.contains("kanal jest pelny")) {
                this.retryChannelSwitch(1200L);
            }
        }
    }

    private boolean isAreaZonePermissionMessage(String message) {
        return message.contains("nie posiadasz uprawnien do tej strefy");
    }

    private void handleAreaZonePermissionMessage() {
        if (!this.mineMode.getValue().equals(this.areaMode) || !this.areaAntiStuck.isEnabled()
                || mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.areaZoneBoundaryCooldownUntil) {
            return;
        }
        if (mc.interactionManager != null && mc.player != null && mc.world != null && this.currentBreakingPos != null) {
            mc.interactionManager.cancelBlockBreaking();
            this.currentBreakingPos = null;
        }
        this.areaAvoidanceDirection = this.areaAvoidanceDirection < 0 ? 1 : -1;
        this.areaAvoidanceTicks = 100;
        this.areaAvoidanceCooldown = 0;
        this.areaBlockedTicks = 0;
        this.areaCollisionTicks = 0;
        this.areaAvoidanceAttempts = Math.max(this.areaAvoidanceAttempts, 1);
        this.areaLastPlayerPos = mc.player.getPos();
        this.areaLastMoveDistanceSq = Double.MAX_VALUE;
        this.areaZoneBoundaryCooldownUntil = now + 1200L;
    }

    private String normalizeServerMessage(String message) {
        if (message == null) {
            return "";
        }
        String normalized = message
                .replaceAll("(?i)\u00a7[0-9a-fk-orx]", "")
                .replace('\u00a0', ' ')
                .replace('\u202f', ' ');
        normalized = java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized
                .replace('\u0142', 'l')
                .replace('\u0141', 'l')
                .replaceAll("\\s+", " ")
                .toLowerCase(java.util.Locale.ROOT)
                .trim();
    }

    private boolean startChannelSwitch() {
        if (!this.autoChannelSwitch.isEnabled() || this.channelSwitching || mc.player == null
                || mc.player.networkHandler == null) {
            return false;
        }
        this.resetChannelSwitchState();
        this.channelSwitching = true;
        this.channelNextActionTime = 0L;
        this.releaseChannelInputs();
        if (mc.interactionManager != null && mc.player != null && mc.world != null && this.currentBreakingPos != null) {
            mc.interactionManager.cancelBlockBreaking();
            this.currentBreakingPos = null;
        }
        return this.sendChannelCommand();
    }

    private void handleChannelSwitchTick() {
        this.releaseChannelInputs();
        if (mc.player == null || mc.player.networkHandler == null || mc.interactionManager == null) {
            this.resetChannelSwitchState();
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.channelNextActionTime) {
            return;
        }
        if (!this.channelClicked) {
            ScreenHandler handler = mc.player.currentScreenHandler;
            if (this.isChannelScreen(handler)) {
                if (!this.clickRandomChannelItem(handler)) {
                    this.retryChannelSwitch(1500L);
                }
                return;
            }
            if (this.channelCommandSentTime == 0L || now - this.channelCommandSentTime > 3500L) {
                this.sendChannelCommand();
            }
            return;
        }
        if (now - this.channelClickTime > 5500L) {
            this.retryChannelSwitch(1200L);
        }
    }

    private boolean sendChannelCommand() {
        if (mc.player == null || mc.player.networkHandler == null) {
            this.resetChannelSwitchState();
            return false;
        }
        if (this.channelAttempt >= 10) {
            this.resetChannelSwitchState();
            this.channelResumeMiningAt = System.currentTimeMillis() + 7000L;
            return false;
        }
        this.releaseChannelInputs();
        this.closeCurrentHandledScreen();
        long now = System.currentTimeMillis();
        mc.player.networkHandler.sendChatCommand("ch");
        this.channelAttempt++;
        this.channelClicked = false;
        this.channelCommandSentTime = now;
        this.channelNextActionTime = now + 1000L;
        this.lastCommandGlobalTime = now;
        return true;
    }

    private boolean isChannelScreen(ScreenHandler handler) {
        if (mc.currentScreen == null || mc.player == null || handler == null || handler == mc.player.playerScreenHandler) {
            return false;
        }
        String title = this.normalizeServerMessage(mc.currentScreen.getTitle().getString());
        return title.contains("zmiana kanalu")
                || (title.contains("zmiana") && title.contains("kanal"))
                || handler instanceof GenericContainerScreenHandler;
    }

    private boolean clickRandomChannelItem(ScreenHandler handler) {
        int containerSlots = this.getChannelContainerSlotCount(handler);
        if (containerSlots <= 0) {
            return false;
        }
        java.util.List<Integer> clickableSlots = new java.util.ArrayList<>();
        java.util.List<Integer> fallbackSlots = new java.util.ArrayList<>();
        for (int slotId = 0; slotId < containerSlots && slotId < handler.slots.size(); slotId++) {
            Slot slot = handler.getSlot(slotId);
            if (slot == null || slot.getStack().isEmpty()) {
                continue;
            }
            fallbackSlots.add(slot.id);
            if (this.isClickableChannelItem(slot.getStack())) {
                clickableSlots.add(slot.id);
            }
        }
        if (clickableSlots.isEmpty()) {
            clickableSlots.addAll(fallbackSlots);
        }
        if (clickableSlots.isEmpty()) {
            return false;
        }
        int slotId = clickableSlots.get(this.channelRandom.nextInt(clickableSlots.size()));
        this.releaseChannelInputs();
        mc.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, mc.player);
        long now = System.currentTimeMillis();
        this.channelClicked = true;
        this.channelClickTime = now;
        this.channelNextActionTime = now + 1200L;
        return true;
    }

    private int getChannelContainerSlotCount(ScreenHandler handler) {
        if (handler instanceof GenericContainerScreenHandler genericHandler) {
            return genericHandler.getRows() * 9;
        }
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot != null && slot.inventory instanceof PlayerInventory) {
                return i;
            }
        }
        return Math.max(0, handler.slots.size() - 36);
    }

    private boolean isClickableChannelItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && !stack.hasGlint();
    }

    private void retryChannelSwitch(long delayMs) {
        this.releaseChannelInputs();
        this.channelClicked = false;
        this.channelCommandSentTime = 0L;
        this.channelClickTime = 0L;
        this.closeCurrentHandledScreen();
        this.channelNextActionTime = System.currentTimeMillis() + delayMs;
    }

    private void finishChannelSwitch() {
        this.releaseChannelInputs();
        this.resetChannelSwitchState();
        this.closeCurrentHandledScreen();
        long now = System.currentTimeMillis();
        this.channelResumeMiningAt = now + CHANNEL_WORLD_LOAD_DELAY_MS;
        this.channelScanOnlyUntil = this.channelResumeMiningAt + CHANNEL_SCAN_ONLY_MS;
        this.channelInputReleaseUntil = this.channelResumeMiningAt + CHANNEL_INPUT_RELEASE_AFTER_MS;
        int firstRegion = this.findFirstSelectionRegion();
        this.activeRegionIndex = firstRegion >= 0 ? firstRegion : 0;
        this.resetAreaScan();
        this.areaFirstTick = true;
        this.releaseChannelInputs();
    }

    private void resetChannelSwitchState() {
        this.channelSwitching = false;
        this.channelClicked = false;
        this.channelNextActionTime = 0L;
        this.channelCommandSentTime = 0L;
        this.channelClickTime = 0L;
        this.channelAttempt = 0;
    }

    private void closeCurrentHandledScreen() {
        if (mc.player != null && mc.currentScreen != null && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            mc.player.closeHandledScreen();
            mc.setScreen(null);
        }
    }

    private void handleMovement() {
        
        if (mc.currentScreen != null) {
            stopMovement();
            return;
        }
        if (mineMode.getValue().equals(areaMode)) {
            this.handleAreaMining();
            return;
        }
        // Block Limit = 0 -> no horizontal movement at all (just mine in place)
        if (blockLimit.getCurrentValue() <= 0.0f) {
            stopMovement();
            return;
        }
        if (mineMode.getValue().equals(squareMode)) {
            if (!positionInitialized) {
                movementTicks = 0;
                loopStep = 0;
                positionInitialized = true;
            }
            movementTicks++;
            float ticksForStep = (loopStep == 0 || loopStep == 2) ? 4.5f : ticksPerBlock * blockLimit.getCurrentValue();
            if (movementTicks >= ticksForStep) {
                loopStep = (loopStep + 1) % 4;
                movementTicks = 0;
            }
            mc.options.forwardKey.setPressed(loopStep == 0);
            mc.options.rightKey.setPressed(loopStep == 1);
            mc.options.backKey.setPressed(loopStep == 2);
            mc.options.leftKey.setPressed(loopStep == 3);
        } else {
            if (!positionInitialized) {
                movementTicks = 0;
                positionInitialized = true;
                direction = 1;
            }
            movementTicks++;
            if (movementTicks >= ticksPerBlock * blockLimit.getCurrentValue()) {
                direction *= -1;
                movementTicks = 0;
            }
            mc.options.rightKey.setPressed(direction == 1);
            mc.options.leftKey.setPressed(direction == -1);
        }
    }

    private void handleDigging() {
        globalTickCounter++;

        // Area mode drives its own look/move/dig cycle entirely inside handleAreaMining()
        // (called from handleMovement()), since it needs to target a specific block inside
        // the selection rather than whatever the crosshair happens to be on.
        if (mineMode.getValue().equals(areaMode)) {
            this.handleAreaPromoMessage();
            return;
        }
        this.handleDefaultPromoMessage();

        // keep attackKey RELEASED and drive the break manually via updateBlockBreakingProgress.
        // A held attackKey would make vanilla's handleBlockBreaking advance the break a SECOND
        // time each tick (double progress = fastbreak / shadow blocks). Crucially, when the
        // crosshair moves to a new block (the bot strafes while mining) we cancelBlockBreaking
        // first \u2014 otherwise the server keeps the old block half-broken => shadow blocks.
        mc.options.attackKey.setPressed(false);

        if (mc.currentScreen != null) {
            return;
        }
        if (mc.interactionManager == null || mc.player == null || mc.world == null) {
            return;
        }

        net.minecraft.util.hit.HitResult hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            net.minecraft.util.hit.BlockHitResult blockHit = (net.minecraft.util.hit.BlockHitResult) hit;
            net.minecraft.util.math.BlockPos pos = blockHit.getBlockPos();
            if (!mc.world.getBlockState(pos).isAir()) {
                // durability guard: stop mining and swap/repair when the pickaxe is too low
                ItemStack stack = mc.player.getMainHandStack();
                if (this.isMiningTool(stack) && this.getDurabilityPercent(stack) < this.percent.getCurrentValue()) {
                    if (this.currentBreakingPos != null) {
                        mc.interactionManager.cancelBlockBreaking();
                        this.currentBreakingPos = null;
                    }
                    this.handleLowDurability(stack);
                    return;
                }
                if (this.currentBreakingPos != null && !this.currentBreakingPos.equals(pos)) {
                    mc.interactionManager.cancelBlockBreaking();
                }
                this.currentBreakingPos = pos;
                mc.interactionManager.updateBlockBreakingProgress(pos, blockHit.getSide());
                mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            } else if (this.currentBreakingPos != null) {
                mc.interactionManager.cancelBlockBreaking();
                this.currentBreakingPos = null;
            }
        } else if (this.currentBreakingPos != null) {
            mc.interactionManager.cancelBlockBreaking();
            this.currentBreakingPos = null;
        }
    }

    private void handleAreaPromoMessage() {
        // Promo chat messages are disabled in this build.
    }

    private void handleDefaultPromoMessage() {
        // Promo chat messages are disabled in this build.
    }

    private void stopMovement() {
        if (mc.options != null) {
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.attackKey.setPressed(false);
        }
    }

    private void releaseChannelInputs() {
        if (mc.options == null) {
            return;
        }
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
        mc.options.useKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        if (mc.interactionManager != null && mc.player != null && mc.world != null && this.currentBreakingPos != null) {
            mc.interactionManager.cancelBlockBreaking();
            this.currentBreakingPos = null;
        }
    }

    private void handleLowDurability(ItemStack currentStack) {
        // autoRepair: send the configured command (cooldown 10s to avoid spam).
        String command = this.repairCommand.getText().strip();
        if (command.startsWith("/")) command = command.substring(1).strip();
        if (this.autoRepair.isEnabled() && !command.isEmpty() && this.repairTimer.finished(10000L)) {
            mc.player.networkHandler.sendChatCommand(command);
            BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.INFO,
                    "Auto Repair", "Sent /" + command);
            this.repairTimer.reset();
            return;
        }
        // autoReplace: switch to better pickaxe from hotbar or inventory
        if (this.autoReplace.isEnabled() && this.trySwitchPickaxe(currentStack)) {
            return;
        }
        if (this.timer.finished(800L)) {
            BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.ERROR,
                    "Mining tool almost broken!", "No replacement available");
            this.timer.reset();
        }
    }

    private boolean trySwitchPickaxe(ItemStack currentStack) {
        double currentDurability = this.getDurabilityPercent(currentStack);
        int currentHotbarSlot = mc.player.getInventory().selectedSlot;

        for (int i = 0; i < 9; i++) {
            if (i == currentHotbarSlot) continue;
            HotbarSlot slot = InventoryUtility.getHotbarSlot(i);
            ItemStack stack = slot.itemStack();
            if (!this.isMiningTool(stack) || (stack.getItem() instanceof net.minecraft.item.AxeItem)
                    != (currentStack.getItem() instanceof net.minecraft.item.AxeItem)) continue;
            if (this.getDurabilityPercent(stack) > currentDurability) {
                InventoryUtility.selectHotbarSlot(slot);
                if (this.timer.finished(800L)) {
                    BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS,
                            "Mining tool replaced",
                            String.format("%.1f%% \u2192 %.1f%%", currentDurability, this.getDurabilityPercent(stack)));
                    this.timer.reset();
                }
                return true;
            }
        }

        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!this.isMiningTool(stack) || (stack.getItem() instanceof net.minecraft.item.AxeItem)
                    != (currentStack.getItem() instanceof net.minecraft.item.AxeItem)) continue;
            if (this.getDurabilityPercent(stack) > currentDurability) {
                InventoryUtility.moveToHotbar(InventoryUtility.getInventorySlot(i), currentHotbarSlot);
                if (this.timer.finished(800L)) {
                    BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS,
                            "Mining tool replaced from inventory",
                            String.format("%.1f%% \u2192 %.1f%%", currentDurability, this.getDurabilityPercent(stack)));
                    this.timer.reset();
                }
                return true;
            }
        }

        return false;
    }

    private boolean isMiningTool(ItemStack stack) {
        return stack != null && stack.isDamageable()
                && (stack.getItem() instanceof PickaxeItem || stack.getItem() instanceof net.minecraft.item.AxeItem);
    }

    private boolean isValidPickaxe(ItemStack stack) {
        return stack != null && stack.isDamageable() && stack.getItem() instanceof PickaxeItem;
    }

    private double getDurabilityPercent(ItemStack stack) {
        return (double) (stack.getMaxDamage() - stack.getDamage()) / (double) stack.getMaxDamage() * 100.0;
    }

    // --- Area mining: WorldEdit-style selection-driven mining, ported from MinestarMiner's
    // MiningManager tunnel-scan algorithm, kept 1:1 with Minestar's Area flow.

    private void handleAreaMining() {
        if (mc.interactionManager == null || mc.player == null || mc.world == null) {
            stopMovement();
            return;
        }
        // Right after (re)connecting, mid-/ch, or while routed through a lobby/hub world before
        // being teleported into the real game world, chunk data for the selection may not have
        // arrived (or been rescanned) yet. Player position and isChunkLoaded() can't tell "empty"
        // apart from "not synced yet" in those cases, so wait for a real per-chunk scan result
        // instead of ever treating "unknown" as "empty" (which used to fire an unwanted /ch).
        if (this.hasSelection() && this.anyRegionScanUnknown()) {
            stopMovement();
            this.resetAreaScan();
            this.areaFirstTick = true;
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.channelResumeMiningAt) {
            this.releaseChannelInputs();
            this.resetAreaScan();
            this.areaFirstTick = true;
            return;
        }
        if (now < this.channelInputReleaseUntil) {
            this.releaseChannelInputs();
            this.resetAreaScan();
            this.areaFirstTick = true;
            return;
        }
        if (!this.hasSelection()) {
            stopMovement();
            this.areaFirstTick = true;
            if (this.currentBreakingPos != null) {
                mc.interactionManager.cancelBlockBreaking();
                this.currentBreakingPos = null;
            }
            return;
        }
        if (this.areaFirstTick && this.areaTargetPos == null && !this.switchToNearestFreeSelectionRegion()) {
            if (now < this.channelScanOnlyUntil) {
                this.stopMovement();
                this.resetAreaScan();
                this.areaFirstTick = true;
                return;
            }
            if (this.startChannelSwitch()) {
                return;
            }
            stopMovement();
            if (this.currentBreakingPos != null) {
                mc.interactionManager.cancelBlockBreaking();
                this.currentBreakingPos = null;
            }
            return;
        }
        if (!this.ensureActiveRegionSelection()) {
            stopMovement();
            this.areaFirstTick = true;
            if (this.currentBreakingPos != null) {
                mc.interactionManager.cancelBlockBreaking();
                this.currentBreakingPos = null;
            }
            return;
        }
        if (this.areaSkipTicks > 0) {
            this.areaSkipTicks--;
            mc.options.attackKey.setPressed(false);
            return;
        }

        if (this.areaFirstTick) {
            this.areaCurrentYaw = mc.player.getYaw();
            this.areaCurrentPitch = mc.player.getPitch();
            this.areaFirstTick = false;
        }

        // durability guard (same behaviour as the crosshair-driven digging path)
        ItemStack stack = mc.player.getMainHandStack();
        if (this.isMiningTool(stack) && this.getDurabilityPercent(stack) < this.percent.getCurrentValue()) {
            this.stopMovement();
            if (this.currentBreakingPos != null) {
                mc.interactionManager.cancelBlockBreaking();
                this.currentBreakingPos = null;
            }
            this.handleLowDurability(stack);
            return;
        }

        BlockPos oldTargetPos = this.areaTargetPos;

        // Priority 1: whatever's immediately at the player's feet/head/facing-wall inside the
        // selection takes precedence over the scan result \u2014 this is what makes the bot clear
        // the space it's actually standing in first, rather than beelining across the region
        // to the scan's next strip and leaving stray blocks behind it (which read as "teleporty").
        BlockPos myFeet = mc.player.getBlockPos();
        BlockPos myHead = myFeet.up();
        if (!mc.world.getBlockState(myFeet).isAir() && this.isInSelection(myFeet)) {
            this.areaTargetPos = myFeet;
        } else if (!mc.world.getBlockState(myHead).isAir() && this.isInSelection(myHead)) {
            this.areaTargetPos = myHead;
        }
        if (mc.player.horizontalCollision) {
            net.minecraft.util.math.Direction facing = mc.player.getHorizontalFacing();
            BlockPos legLevel = myFeet.offset(facing);
            BlockPos eyeLevel = legLevel.up();
            if (!mc.world.getBlockState(legLevel).isAir() && this.isInSelection(legLevel)) {
                this.areaTargetPos = legLevel;
            } else if (!mc.world.getBlockState(eyeLevel).isAir() && this.isInSelection(eyeLevel)) {
                this.areaTargetPos = eyeLevel;
            }
        }

        // Priority 2: correct the movement/look target to whatever is actually in the way.
        // (that mismatch is what makes bots look "off"). Also remember the real hit side here \u2014
        if (this.areaTargetPos != null) {
            Vec3d start = mc.player.getEyePos();
            Vec3d end = Vec3d.ofCenter(this.areaTargetPos);
            HitResult hit = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE, (Entity) mc.player));
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = blockHit.getBlockPos();
                if (!hitPos.equals(this.areaTargetPos) && this.isInSelection(hitPos) && !mc.world.getBlockState(hitPos).isAir()) {
                    this.areaTargetPos = hitPos;
                }
            }
        }

        if (this.areaTargetPos != null && !this.areaTargetPos.equals(oldTargetPos)) {
            this.areaTargetStartTime = System.currentTimeMillis();
            this.areaLastHitResetTime = this.areaTargetStartTime;
        }

        BlockPos visibleTarget = this.findVisibleAreaBlockInReach();
        if (visibleTarget != null) {
            this.areaTargetPos = visibleTarget;
        }

        // Timeout/stall handling: drop a target that's been air for a while, and if we haven't
        // registered a hit in 2s, briefly release attack + pause 2 ticks to resync (mirrors
        // MinestarMiner exactly) instead of holding attack forever on a block that never breaks.
        if (this.areaTargetPos != null) {
            if (mc.world.getBlockState(this.areaTargetPos).isAir()) {
                this.areaTargetPos = null;
            } else {
                long now2 = System.currentTimeMillis();
                if (now2 - this.areaTargetStartTime > 7000L) {
                    this.areaTargetPos = null;
                } else if (now2 - this.areaLastHitResetTime > 2000L) {
                    mc.options.attackKey.setPressed(false);
                    this.areaLastHitResetTime = now2;
                    this.areaSkipTicks = 2;
                }
            }
        }

        if (this.areaTargetPos == null) {
            this.areaTargetPos = this.findNextAreaBlock();
            if (this.areaTargetPos != null && !this.areaTargetPos.equals(oldTargetPos)) {
                this.areaTargetStartTime = System.currentTimeMillis();
                this.areaLastHitResetTime = this.areaTargetStartTime;
            }
            if (this.areaTargetPos == null) {
                if (this.isAreaEmpty()) {
                    if (this.switchToNearestFreeSelectionRegion()) {
                        return;
                    }
                    if (now < this.channelScanOnlyUntil) {
                        this.stopMovement();
                        this.resetAreaScan();
                        this.areaFirstTick = true;
                        return;
                    }
                    if (this.startChannelSwitch()) {
                        return;
                    }
                    // whole selection scanned and empty: stop cleanly, keep selection for review
                    this.stopMovement();
                    if (this.currentBreakingPos != null) {
                        mc.interactionManager.cancelBlockBreaking();
                        this.currentBreakingPos = null;
                    }
                    return;
                }
                this.areaReversedPass = !this.areaReversedPass;
                this.areaCurLayerY = Integer.MIN_VALUE;
                this.areaTargetPos = this.findNextAreaBlock();
                if (this.areaTargetPos != null && !this.areaTargetPos.equals(oldTargetPos)) {
                    this.areaTargetStartTime = System.currentTimeMillis();
                    this.areaLastHitResetTime = this.areaTargetStartTime;
                }
            }
        }

        if (this.areaTargetPos == null) {
            this.stopMovement();
            if (this.currentBreakingPos != null) {
                mc.interactionManager.cancelBlockBreaking();
                this.currentBreakingPos = null;
            }
            return;
        }

        Vec3d blockCenter = Vec3d.ofCenter(this.areaTargetPos);
        this.areaLookAtSmoothly(blockCenter);

        double dist = mc.player.getEyePos().distanceTo(blockCenter);
        if (dist > 3.5) {
            this.areaMoveTo(blockCenter);
            mc.options.attackKey.setPressed(false);
            if (this.currentBreakingPos != null) {
                mc.interactionManager.cancelBlockBreaking();
                this.currentBreakingPos = null;
            }
        } else {
            this.stopAreaMovement();
            this.performAreaDigging();
        }
    }

    private void performAreaDigging() {
        mc.options.attackKey.setPressed(false);
        HitResult hit = this.raycastAreaCrosshair();
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            if (this.isInSelection(pos) && !mc.world.getBlockState(pos).isAir()) {
                ItemStack stack = mc.player.getMainHandStack();
                if (this.isMiningTool(stack) && this.getDurabilityPercent(stack) < this.percent.getCurrentValue()) {
                    if (this.currentBreakingPos != null) {
                        mc.interactionManager.cancelBlockBreaking();
                        this.currentBreakingPos = null;
                    }
                    this.handleLowDurability(stack);
                    return;
                }
                if (this.currentBreakingPos != null && !this.currentBreakingPos.equals(pos)) {
                    mc.interactionManager.cancelBlockBreaking();
                }
                this.currentBreakingPos = pos;
                mc.interactionManager.updateBlockBreakingProgress(pos, blockHit.getSide());
                mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            } else if (this.currentBreakingPos != null) {
                mc.interactionManager.cancelBlockBreaking();
                this.currentBreakingPos = null;
            }
        } else if (this.currentBreakingPos != null) {
            mc.interactionManager.cancelBlockBreaking();
            this.currentBreakingPos = null;
        }
    }

    private HitResult raycastAreaCrosshair() {
        Vec3d start = mc.player.getEyePos();
        Vec3d end = start.add(mc.player.getRotationVec(1.0f).multiply(4.5));
        return mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE, (Entity) mc.player));
    }

    private void areaLookAtSmoothly(Vec3d target) {
        Vec3d eyePos = mc.player.getEyePos();
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = this.areaCurrentYaw;
        if (horizontalDist > 0.01) {
            targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        }
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.max(horizontalDist, 0.001))));
        float yawDelta = ((targetYaw - this.areaCurrentYaw) % 360.0f + 540.0f) % 360.0f - 180.0f;
        float pitchDelta = ((targetPitch - this.areaCurrentPitch) % 360.0f + 540.0f) % 360.0f - 180.0f;
        this.areaCurrentYaw += yawDelta * 0.3f;
        this.areaCurrentPitch += pitchDelta * 0.3f;
        mc.player.setYaw(this.areaCurrentYaw);
        mc.player.setPitch(this.areaCurrentPitch);
    }

    private void areaMoveTo(Vec3d target) {
        Vec3d playerPos = mc.player.getPos();
        double dx = target.x - playerPos.x;
        double dz = target.z - playerPos.z;
        double distSq = dx * dx + dz * dz;
        if (distSq < 0.05) {
            mc.options.forwardKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            this.resetAreaAvoidance();
        } else {
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
            float yawDelta = ((yaw - mc.player.getYaw()) % 360.0f + 540.0f) % 360.0f - 180.0f;
            mc.player.setYaw(mc.player.getYaw() + yawDelta * 0.3f);
            boolean avoiding = this.handleAreaAvoidance(target, distSq);
            if (avoiding) {
                mc.options.forwardKey.setPressed(this.areaAvoidanceTicks <= 8);
                mc.options.leftKey.setPressed(this.areaAvoidanceDirection < 0);
                mc.options.rightKey.setPressed(this.areaAvoidanceDirection > 0);
                mc.options.sprintKey.setPressed(true);
            } else {
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.forwardKey.setPressed(true);
                mc.options.sprintKey.setPressed(true);
            }
            mc.options.jumpKey.setPressed(!this.areaAntiStuck.isEnabled()
                    && !avoiding
                    && mc.player.horizontalCollision
                    && target.y > mc.player.getY() + 1.0);
        }
    }

    private boolean handleAreaAvoidance(Vec3d target, double distSq) {
        if (!this.areaAntiStuck.isEnabled()) {
            this.resetAreaAvoidance();
            return false;
        }
        if (this.currentBreakingPos != null) {
            this.areaLastPlayerPos = mc.player.getPos();
            this.areaBlockedTicks = 0;
            this.areaCollisionTicks = 0;
            return false;
        }

        if (this.areaLastMoveTarget == null || this.areaLastMoveTarget.squaredDistanceTo(target) > 0.25) {
            this.areaLastMoveTarget = target;
            this.areaLastPlayerPos = mc.player.getPos();
            this.areaLastMoveDistanceSq = distSq;
            this.areaBlockedTicks = 0;
            this.areaCollisionTicks = 0;
            this.areaAvoidanceTicks = 0;
            this.areaAvoidanceCooldown = 0;
            this.areaAvoidanceAttempts = 0;
            return false;
        }

        if (this.areaAvoidanceTicks > 0) {
            this.areaAvoidanceTicks--;
            if (this.areaAvoidanceTicks == 0) {
                this.areaAvoidanceCooldown = 3;
            }
            return true;
        }

        if (this.areaAvoidanceCooldown > 0) {
            this.areaAvoidanceCooldown--;
        }

        if (mc.player.horizontalCollision && this.areaAvoidanceCooldown == 0 && this.areaAvoidanceAttempts < 3) {
            if (this.areaAvoidanceAttempts == 0) {
                this.areaAvoidanceDirection = this.areaAvoidanceRandom.nextBoolean() ? 1 : -1;
            } else {
                this.areaAvoidanceDirection *= -1;
            }
            this.areaAvoidanceTicks = 80;
            this.areaAvoidanceAttempts++;
            this.areaBlockedTicks = 0;
            this.areaCollisionTicks = 0;
            return true;
        }

        Vec3d currentPlayerPos = mc.player.getPos();
        if (this.areaLastPlayerPos == null) {
            this.areaLastPlayerPos = currentPlayerPos;
            return false;
        }

        double movedX = currentPlayerPos.x - this.areaLastPlayerPos.x;
        double movedZ = currentPlayerPos.z - this.areaLastPlayerPos.z;
        double movedSq = movedX * movedX + movedZ * movedZ;
        this.areaLastPlayerPos = currentPlayerPos;

        boolean madeProgress = distSq < this.areaLastMoveDistanceSq - 0.08;
        boolean movingEnough = movedSq >= 0.0064;
        if (madeProgress || movingEnough) {
            this.areaLastMoveDistanceSq = distSq;
            this.areaBlockedTicks = 0;
            this.areaCollisionTicks = 0;
            this.areaAvoidanceAttempts = 0;
            return false;
        }

        this.areaBlockedTicks++;
        if (this.areaAvoidanceCooldown == 0
                && this.areaBlockedTicks >= 6
                && this.areaAvoidanceAttempts < 3) {
            if (this.areaAvoidanceAttempts == 0) {
                this.areaAvoidanceDirection = this.areaAvoidanceRandom.nextBoolean() ? 1 : -1;
            } else {
                this.areaAvoidanceDirection *= -1;
            }
            this.areaAvoidanceTicks = 80;
            this.areaAvoidanceAttempts++;
            this.areaBlockedTicks = 0;
            return true;
        }

        if (this.areaAvoidanceAttempts >= 3 && this.areaBlockedTicks >= 14) {
            this.areaAvoidanceCooldown = 40;
            this.areaBlockedTicks = 0;
        }

        return false;
    }

    private BlockPos findVisibleAreaBlockInReach() {
        Vec3d eye = mc.player.getEyePos();
        BlockPos playerBlock = mc.player.getBlockPos();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterateOutwards(playerBlock, 4, 3, 4)) {
            if (!this.isInSelection(pos) || mc.world.getBlockState(pos).isAir()) {
                continue;
            }
            Vec3d center = Vec3d.ofCenter(pos);
            double distance = eye.distanceTo(center);
            if (distance > 4.35) {
                continue;
            }
            HitResult hit = mc.world.raycast(new RaycastContext(eye, center, RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE, (Entity) mc.player));
            if (!(hit instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(pos)) {
                continue;
            }
            Vec3d look = mc.player.getRotationVec(1.0f).normalize();
            Vec3d toBlock = center.subtract(eye).normalize();
            double anglePenalty = 1.0 - look.dotProduct(toBlock);
            double score = distance + anglePenalty * 2.0;
            if (score < bestScore) {
                bestScore = score;
                best = pos.toImmutable();
            }
        }
        return best;
    }

    private void stopAreaMovement() {
        mc.options.forwardKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        this.resetAreaAvoidance();
    }

    private boolean isInSelection(BlockPos pos) {
        BlockPos[] region = this.getRegionPositions(this.activeRegionIndex);
        if (region[0] == null || region[1] == null) {
            return false;
        }
        int minX = Math.min(region[0].getX(), region[1].getX());
        int maxX = Math.max(region[0].getX(), region[1].getX());
        int minY = Math.min(region[0].getY(), region[1].getY());
        int maxY = Math.max(region[0].getY(), region[1].getY());
        int minZ = Math.min(region[0].getZ(), region[1].getZ());
        int maxZ = Math.max(region[0].getZ(), region[1].getZ());
        return pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    private boolean isAreaEmpty() {
        return this.findNearestBlockInRegion(this.activeRegionIndex) == null;
    }

    /**
     * True once every chunk touching the region is either currently loaded (getBlockState()
     * reflects real, live data) or has received at least one mixin-fed scan since connecting
     * (recordChunkDataPacket). Requiring only "currently loaded" would be enough on its own for
     * normal play, but relying solely on the historical scannedChunks set was too strict: a
     * chunk on the edge of render distance may never get re-sent, so a region straddling it
     * would report "not ready" forever even though its blocks are perfectly visible and
     * minable right now. Prefer isChunkLoaded() as the live signal; scannedChunks only helps
     * the fresh-join/post-/ch case where a chunk hasn't loaded at all yet.
     */
    private boolean isRegionScanReady(int minX, int maxX, int minZ, int maxZ) {
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (mc.world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                if (!scannedChunks.contains(net.minecraft.util.math.ChunkPos.toLong(cx, cz))) {
                    return false;
                }
            }
        }
        return true;
    }

    private BlockPos findNearestBlockInRegion(int regionIndex) {
        if (mc.player == null || mc.world == null) {
            return null;
        }
        BlockPos[] region = this.getRegionPositions(regionIndex);
        if (region[0] == null || region[1] == null) {
            return null;
        }
        int minX = Math.min(region[0].getX(), region[1].getX());
        int maxX = Math.max(region[0].getX(), region[1].getX());
        int minY = Math.min(region[0].getY(), region[1].getY());
        int maxY = Math.max(region[0].getY(), region[1].getY());
        int minZ = Math.min(region[0].getZ(), region[1].getZ());
        int maxZ = Math.max(region[0].getZ(), region[1].getZ());
        if (!this.isRegionScanReady(minX, maxX, minZ, maxZ)) {
            return null;
        }
        Vec3d eye = mc.player.getEyePos();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mc.world.getBlockState(pos).isAir()) {
                        continue;
                    }
                    Vec3d center = Vec3d.ofCenter(pos);
                    double dx = center.x - eye.x;
                    double dy = center.y - eye.y;
                    double dz = center.z - eye.z;
                    double score = dx * dx + dy * dy + dz * dz;
                    if (score < bestScore) {
                        bestScore = score;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private BlockPos findBlockInVerticalSlice(int x1, int x2, int y1, int y2, int z1, int z2) {
        for (int y = y1; y <= y2; ++y) {
            for (int x = x1; x <= x2; ++x) {
                for (int z = z1; z <= z2; ++z) {
                    BlockPos bp = new BlockPos(x, y, z);
                    if (!mc.world.getBlockState(bp).isAir()) {
                        return bp;
                    }
                }
            }
        }
        return null;
    }

    /** Boustrophedon (back-and-forth strip) scan of the selection, 1:1 with Minestar. */
    private BlockPos findNextAreaBlock() {
        BlockPos[] region = this.getRegionPositions(this.activeRegionIndex);
        if (region[0] == null || region[1] == null) {
            return null;
        }
        int minX = Math.min(region[0].getX(), region[1].getX());
        int maxX = Math.max(region[0].getX(), region[1].getX());
        int minY = Math.min(region[0].getY(), region[1].getY());
        int maxY = Math.max(region[0].getY(), region[1].getY());
        int minZ = Math.min(region[0].getZ(), region[1].getZ());
        int maxZ = Math.max(region[0].getZ(), region[1].getZ());
        int w = Math.max(1, Math.round(this.areaPatternWidth.getCurrentValue()));
        int h = Math.max(1, Math.round(this.areaPatternHeight.getCurrentValue()));

        if (this.areaCurLayerY == Integer.MIN_VALUE) {
            if (!this.areaReversedPass) {
                this.areaCurLayerY = minY;
                this.areaCurStripX = minX;
                this.areaCurZ = minZ;
            } else {
                this.areaCurLayerY = minY + (maxY - minY) / h * h;
                this.areaCurStripX = minX + (maxX - minX) / w * w;
                this.areaCurZ = maxZ;
            }
        }

        if (!this.areaReversedPass) {
            for (int ly = this.areaCurLayerY; ly <= maxY; ly += h) {
                int layerMinY = ly;
                int layerMaxY = Math.min(maxY, ly + h - 1);
                int startStripX = ly == this.areaCurLayerY ? this.areaCurStripX : minX;
                for (int tx = startStripX; tx <= maxX; tx += w) {
                    int stripMaxX = Math.min(maxX, tx + w - 1);
                    int stripIdx = (tx - minX) / w;
                    boolean forwardZ = stripIdx % 2 == 0;
                    BlockPos found;
                    if (forwardZ) {
                        int startZ = ly == this.areaCurLayerY && tx == this.areaCurStripX ? this.areaCurZ : minZ;
                        for (int tz = startZ; tz <= maxZ; ++tz) {
                            found = this.findBlockInVerticalSlice(tx, stripMaxX, layerMinY, layerMaxY, tz, tz);
                            if (found != null) {
                                this.areaCurLayerY = ly;
                                this.areaCurStripX = tx;
                                this.areaCurZ = tz;
                                return found;
                            }
                        }
                    } else {
                        int startZ = ly == this.areaCurLayerY && tx == this.areaCurStripX ? this.areaCurZ : maxZ;
                        for (int tz = startZ; tz >= minZ; --tz) {
                            found = this.findBlockInVerticalSlice(tx, stripMaxX, layerMinY, layerMaxY, tz, tz);
                            if (found == null) continue;
                            this.areaCurLayerY = ly;
                            this.areaCurStripX = tx;
                            this.areaCurZ = tz;
                            return found;
                        }
                    }
                }
            }
        } else {
            for (int ly = this.areaCurLayerY; ly >= minY; ly -= h) {
                int layerMinY = ly;
                int layerMaxY = Math.min(maxY, ly + h - 1);
                int startStripX = ly == this.areaCurLayerY ? this.areaCurStripX : minX + (maxX - minX) / w * w;
                for (int tx = startStripX; tx >= minX; tx -= w) {
                    int stripMaxX = Math.min(maxX, tx + w - 1);
                    int stripIdx = (tx - minX) / w;
                    boolean forwardZ = stripIdx % 2 == 0;
                    BlockPos found;
                    if (forwardZ) {
                        int startZ = ly == this.areaCurLayerY && tx == this.areaCurStripX ? this.areaCurZ : minZ;
                        for (int tz = startZ; tz <= maxZ; ++tz) {
                            found = this.findBlockInVerticalSlice(tx, stripMaxX, layerMinY, layerMaxY, tz, tz);
                            if (found == null) continue;
                            this.areaCurLayerY = ly;
                            this.areaCurStripX = tx;
                            this.areaCurZ = tz;
                            return found;
                        }
                        continue;
                    }
                    int startZ2 = ly == this.areaCurLayerY && tx == this.areaCurStripX ? this.areaCurZ : maxZ;
                    for (int tz = startZ2; tz >= minZ; --tz) {
                        found = this.findBlockInVerticalSlice(tx, stripMaxX, layerMinY, layerMaxY, tz, tz);
                        if (found == null) continue;
                        this.areaCurLayerY = ly;
                        this.areaCurStripX = tx;
                        this.areaCurZ = tz;
                        return found;
                    }
                }
            }
        }
        return null;
    }

}
