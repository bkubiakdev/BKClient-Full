package dev.bkclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.util.math.Direction;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Generated;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.modules.modules.player.AutoEat;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.utility.time.Timer;
import dev.bkclient.utility.game.EntityUtility;
import dev.bkclient.utility.game.BaritoneBridge;
import dev.bkclient.utility.inventory.InventoryUtility;
import dev.bkclient.utility.render.Draw3DUtility;
import dev.bkclient.utility.render.RenderUtility;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.registry.Registries;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

@ModuleInfo(name = "XRay", category = ModuleCategory.VISUALS, desc = "Highlights specific ores")
public class XRay
        extends BaseModule implements IMinecraft {
    private static final long BARITONE_GOTO_TIMEOUT_MS = 60000L;
    private static final long BARITONE_MINE_TIMEOUT_MS = 90000L;
    private static final int MAX_CACHED_BLOCKS = 12000;
    private static final int MAX_CACHED_BLOCKS_PER_TYPE = 4000;
    private static final int MAX_RENDERED_BLOCKS_PER_FRAME = 2500;
    private static final int BARITONE_PICKAXE_RESERVED_USES = 5;

    private final Set<BlockPos> cachedBlocks = ConcurrentHashMap.newKeySet();
    private final BooleanSetting legitMode = new BooleanSetting(this, "modules.settings.xray.exposed_only").enabled(false);
    private final SelectSetting blocks = new SelectSetting(this, "modules.settings.xray.blocks");
    private final SliderSetting range = new SliderSetting(this, "modules.settings.xray.range").min(16.0f).max(256.0f).currentValue(64.0f)
            .step(16.0f);
    private final BooleanSetting baritoneOnlyExposed = new BooleanSetting(this,
            "modules.settings.xray.mine_only_exposed", this::hideBaritoneSettings).enabled(false);
    // Baritone omija rudy przy wodzie/lawie — tam sie buguje (szarpany ruch, spam korekt pozycji).
    private final BooleanSetting baritoneAvoidWater = new BooleanSetting(this,
            "modules.settings.xray.avoid_water", this::hideBaritoneSettings).enabled(true);
    private final SliderSetting baritoneVeinRadius = new SliderSetting(this, "modules.settings.xray.cluster_radius",
            this::hideBaritoneSettings)
            .min(1.0f).max(8.0f).step(1.0f).currentValue(4.0f);
    private final BooleanSetting baritonePickupDrops = new BooleanSetting(this, "modules.settings.xray.pickup_drops",
            this::hideBaritoneSettings).enabled(true);
    private final SliderSetting baritoneQuantity = new SliderSetting(this, "modules.settings.xray.quantity",
            this::hideBaritoneSettings).min(0.0f).max(256.0f)
            .step(1.0f).currentValue(0.0f);
    private final SliderSetting baritonePickupTime = new SliderSetting(this, "modules.settings.xray.pickup_time",
            () -> this.hideBaritoneSettings() || !this.baritonePickupDrops.isEnabled())
            .min(0.5f).max(8.0f).step(0.5f).currentValue(3.0f)
            .suffix("s");
    private final ButtonSetting baritoneAutoMine = new ButtonSetting(this,
            "modules.settings.xray.automine_selected", this::hideBaritoneSettings)
            .action(this::startBaritoneAutoMine);
    private final ButtonSetting baritoneMineNearest = new ButtonSetting(this, "modules.settings.xray.mine_nearest",
            this::hideBaritoneSettings)
            .action(this::startBaritoneMineNearest);
    private final ButtonSetting baritoneStop = new ButtonSetting(this, "modules.settings.xray.stop_baritone",
            this::hideBaritoneSettings).action(() -> {
        this.stopBaritoneAutomation(true);
        this.notifyBaritone(NotificationType.INFO, "modules.notifications.xray.stopped");
    });
    // Auto-captcha: gdy serwer pokazuje w tytule "Wpisz: /captcha XXXX", wpisuje komendę automatycznie.
    private long lastCaptchaSentMs;
    private String lastCaptchaCode = "";
    private static final java.util.regex.Pattern CAPTCHA_PATTERN =
            java.util.regex.Pattern.compile("/?captcha\\s+([A-Za-z0-9]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
    private final BooleanSetting autoTrash = new BooleanSetting(this, "modules.settings.xray.auto_trash").enabled(false);
    private final StringSetting trashCommand = new StringSetting(this, "modules.settings.xray.trash_command",
            () -> !this.autoTrash.isEnabled()).text("/kosz");
    private final ButtonSetting trashStart = new ButtonSetting(this, "modules.settings.xray.trash_now")
            .action(() -> this.startTrashAutomation(false));
    // Trash state machine (osobne pola, nie kolidują ze sprzedażą)
    private boolean trashActive;
    private int trashStage;
    private int trashSlotIndex;
    private long trashStageStartedAt;
    private boolean trashClickedAny;
    private boolean trashAutoTriggered;
    private String trashPendingCommand = "";
    private long lastTrashAttemptAt;
    private final Map<String, Block> blockMap = new HashMap<>();
    private final Set<Block> enabledBlocksSet = new HashSet<>();
    private int diamonds = 0;
    private int ancient = 0;
    private int gold = 0;
    private int lapis = 0;
    private final Map<Block, Integer> blockCounts = new ConcurrentHashMap<>();
    private final java.util.concurrent.ThreadPoolExecutor scanExecutor = new java.util.concurrent.ThreadPoolExecutor(1, 1, 30, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.ArrayBlockingQueue<>(1), r -> {
        Thread t = new Thread(r, "xray-scanner");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong scanGeneration = new AtomicLong();
    private Future<List<ScanHit>> scanTask;
    private final java.util.LinkedHashSet<Long> pendingScanChunks = new java.util.LinkedHashSet<>();
    private net.minecraft.client.world.ClientWorld scanWorld;
    private List<ScanHit> pendingScanHits = List.of();
    private int pendingHitIndex;
    private record ScanHit(BlockPos pos, Block block) { }
    private record ScanSection(int bottomY, net.minecraft.world.chunk.PalettedContainer<BlockState> blocks) { }
    private final Timer settingsTimer = new Timer();
    private final Timer cacheMaintenanceTimer = new Timer();
    private boolean needsUpdate = false;
    private int lastSelectionHash = -1;
    private boolean lastLegitMode = false;
    private float lastRange = -1.0f;
    private boolean baritoneAutomationActive;
    private boolean baritoneSingleTarget;
    private BlockPos baritoneTarget;
    private String baritoneTargetName;
    private BlockPos baritoneClusterOrigin;
    private String baritoneClusterName;
    private List<BlockPos> baritoneClusterPositions = List.of();
    private BlockPos baritonePickupOrigin;
    private int baritoneStage;
    private long baritoneStageStartedAt;
    private boolean resumeBaritoneAfterTrash;
    private boolean resumeBaritoneSingleTargetAfterTrash;
    private long resumeBaritoneAfterTrashAt;
    private boolean baritonePausedForEating;
    private boolean baritonePausedForEatingSingleTarget;
    private volatile boolean scanLimitReached;

    public XRay() {
        for (Block block : Registries.BLOCK) {
            net.minecraft.util.Identifier id = Registries.BLOCK.getId(block);
            if (id == null) continue;
            String path = id.getPath();
            boolean useful = path.contains("ore") || path.equals("ancient_debris")
                    || path.endsWith("_raw_block") || path.equals("amethyst_cluster")
                    || path.equals("budding_amethyst") || path.equals("spawner");
            if (!useful || this.blockMap.containsKey(path))
                continue;
            this.blockMap.put(path, block);
            new SelectSetting.Value(this.blocks, path);
        }
    }

    private boolean hideBaritoneSettings() {
        return !BaritoneBridge.isAvailable();
    }



    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (XRay.mc.world == null || XRay.mc.player == null) {
            return;
        }
        MatrixStack matrices = event.getMatrices();
        Camera camera = event.getCamera();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.lineWidth(10.0f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        double maxDistSq = this.range.getCurrentValue() * this.range.getCurrentValue();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR);

        // Per block this used to push, translate by (center - camera) and pop, fetch the state twice and
        // compute the box centre three times. The translated matrix is now built in a scratch matrix from
        // the same base with the same translate((float) dx, ...) call (bit-identical), the state is fetched
        // once, the centre once, and the fill colour (a pure function of the block) is cached per block.
        Matrix4f base = matrices.peek().getPositionMatrix();
        Matrix4f translated = this.renderScratch;
        Vec3d cameraPos = XRay.mc.gameRenderer.getCamera().getPos();
        int renderedBlocks = 0;
        for (BlockPos pos : this.cachedBlocks) {
            if (renderedBlocks >= MAX_RENDERED_BLOCKS_PER_FRAME) {
                break;
            }
            if (XRay.mc.player.squaredDistanceTo(pos.toCenterPos()) > maxDistSq)
                continue;

            BlockState state = XRay.mc.world.getBlockState(pos);
            Box boundingBox = this.getBoundingBox(pos, state);
            Block block = state.getBlock();
            Vec3d center = boundingBox.getCenter();

            // RenderUtility.prepareMatrices(matrices, center): translate(center.subtract(cameraPos)).
            translated.set(base).translate((float) (center.x - cameraPos.x), (float) (center.y - cameraPos.y),
                    (float) (center.z - cameraPos.z));
            ColorRGBA fill = this.fillColorCache.get(block);
            if (fill == null) {
                fill = this.getBlockColor(block).withAlpha(30.0f);
                this.fillColorCache.put(block, fill);
            }
            Draw3DUtility.renderFilledBox(translated, buffer, boundingBox.offset(-center.x, -center.y, -center.z), fill);
            ++renderedBlocks;
        }

        BuiltBuffer builtBuffer = buffer.endNullable();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    };

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        this.tick();
    };

    private final EventListener<WorldChangeEvent> onWorldChange = event -> {
        this.cancelActiveScan();
        this.cachedBlocks.clear();
        this.blockCounts.clear();
    };

    // XRay sam czyta tytuy/subtitle serwera (niezalenie od AutoMarket) — pod auto-captch.
    private final EventListener<ReceivePacketEvent> onPacket = event -> {
        Packet<?> p = event.getPacket();
        if (p instanceof TitleS2CPacket titlePacket) {
            this.handleServerTitle(titlePacket.text());
        } else if (p instanceof SubtitleS2CPacket subtitlePacket) {
            this.handleServerTitle(subtitlePacket.text());
        }
    };

    private void handleServerTitle(net.minecraft.text.Text text) {
        if (text == null) {
            return;
        }
        String raw = text.getString().replaceAll("§[0-9a-fk-or]", "").trim();
        if (!raw.isEmpty()) {
            this.onServerTitle(raw);
        }
    }

    public void scanChunk(WorldChunk chunk) { scanChunkAsync(chunk); }

    /** Called by the client packet handler. Queue coordinates, never a live chunk/world. */
    public void scanChunkAsync(WorldChunk chunk) {
        if (chunk == null || !isEnabled() || scanExecutor.isShutdown()) return;
        if (!mc.isOnThread()) throw new IllegalStateException("Chunk snapshots require the client thread");
        if (scanWorld != mc.world) resetScanWorld();
        enqueueChunk(chunk.getPos().toLong());
    }

    private void enqueueChunk(long position) {
        if (pendingScanChunks.size() < 256) pendingScanChunks.add(position);
    }

    public void onWorldChanged() {
        cancelActiveScan(); cachedBlocks.clear(); blockCounts.clear();
    }

    private void resetScanWorld() {
        cancelActiveScan();
        cachedBlocks.clear();
        blockCounts.clear();
        scanWorld = mc.world;
    }

    /** Snapshot capture and bounded publication run on the client thread; workers see immutable copies. */
    private void advanceScans() {
        if (scanWorld != mc.world) resetScanWorld();
        if (scanWorld == null || !isEnabled() || scanExecutor.isShutdown()) return;
        if (scanTask != null) {
            if (!scanTask.isDone()) return;
            try { pendingScanHits = scanTask.get(); }
            catch (Exception ignored) { pendingScanHits = List.of(); }
            pendingHitIndex = 0;
            scanTask = null;
        }
        long deadline = System.nanoTime() + 1_000_000L;
        Map<Block, Integer> counts = new HashMap<>(blockCounts);
        int published = 0;
        while (pendingHitIndex < pendingScanHits.size()) {
            if (++published > 512 || System.nanoTime() >= deadline) return;
            ScanHit hit = pendingScanHits.get(pendingHitIndex++);
            BlockPos pos = hit.pos();
            if (!mc.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4) || !isInRenderDistance(pos)) continue;
            if (!isBlockEnabled(hit.block()) || !mc.world.getBlockState(pos).isOf(hit.block())) continue;
            if (legitMode.isEnabled() && !isExposed(pos)) continue;
            if (!cacheScannedBlock(pos, hit.block(), counts)) {
                pendingScanHits = List.of(); pendingHitIndex = 0; pendingScanChunks.clear(); return;
            }
        }
        pendingScanHits = List.of(); pendingHitIndex = 0;
        if (cachedBlocks.size() >= MAX_CACHED_BLOCKS || areSelectedBlockLimitsReached(counts)) {
            pendingScanChunks.clear(); return;
        }
        if (pendingScanChunks.isEmpty()) return;
        var iterator = pendingScanChunks.iterator();
        long packed = iterator.next(); iterator.remove();
        int cx = net.minecraft.util.math.ChunkPos.getPackedX(packed);
        int cz = net.minecraft.util.math.ChunkPos.getPackedZ(packed);
        if (!mc.world.isChunkLoaded(cx, cz)) return;
        WorldChunk chunk = mc.world.getChunk(cx, cz);
        Set<Block> selected = Set.copyOf(enabledBlocksSet);
        List<ScanSection> sections = new ArrayList<>();
        var liveSections = chunk.getSectionArray();
        for (int i = 0; i < liveSections.length; i++) {
            var section = liveSections[i];
            if (!section.isEmpty() && section.hasAny(state -> selected.contains(state.getBlock()))) {
                sections.add(new ScanSection(mc.world.getBottomY() + i * 16, section.getBlockStateContainer().copy()));
            }
        }
        long generation = scanGeneration.get();
        try {
            scanTask = scanExecutor.submit(() -> scanSnapshot(cx, cz, sections, selected, generation));
        } catch (RejectedExecutionException ignored) { }
    }

    private List<ScanHit> scanSnapshot(int cx, int cz, List<ScanSection> sections, Set<Block> selected, long generation) {
        List<ScanHit> result = new ArrayList<>();
        for (ScanSection section : sections) {
            for (int y = 0; y < 16; y++) {
                if (Thread.currentThread().isInterrupted() || generation != scanGeneration.get()) return List.of();
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    Block block = section.blocks().get(x, y, z).getBlock();
                    if (selected.contains(block)) {
                        result.add(new ScanHit(new BlockPos(cx * 16 + x, section.bottomY() + y, cz * 16 + z), block));
                        if (result.size() >= MAX_CACHED_BLOCKS) return result;
                    }
                }
            }
        }
        return result;
    }

    private boolean areSelectedBlockLimitsReached(Map<Block, Integer> scannedCounts) {
        if (this.enabledBlocksSet.isEmpty()) {
            return false;
        }

        for (Block block : this.enabledBlocksSet) {
            if (scannedCounts.getOrDefault(block, 0) < MAX_CACHED_BLOCKS_PER_TYPE) {
                return false;
            }
        }
        return true;
    }

    private boolean cacheScannedBlock(BlockPos pos, Block block, Map<Block, Integer> scannedCounts) {
        if (this.cachedBlocks.size() >= MAX_CACHED_BLOCKS) {
            this.scanLimitReached = true;
            return false;
        }

        int count = scannedCounts.getOrDefault(block, 0);
        if (count >= MAX_CACHED_BLOCKS_PER_TYPE) {
            this.scanLimitReached = true;
            return true;
        }

        if (this.cachedBlocks.add(pos)) {
            scannedCounts.put(block, count + 1);
            this.blockCounts.merge(block, 1, Integer::sum);
        }
        return true;
    }

    private boolean isExposed(BlockPos pos) {
        if (XRay.mc.world == null)
            return false;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.offset(direction);
            BlockState neighborState = XRay.mc.world.getBlockState(neighbor);
            if (neighborState.isAir() || neighborState.isOf(Blocks.WATER) || neighborState.isOf(Blocks.LAVA)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True jeśli blok ma wodę/lawę (lub waterlogged) w sąsiedztwie 3x3x3 (z diagonalami).
     * Baritone w wodzie buguje się — szarpany ruch, spam korekt pozycji, co flaguje anticheat —
     * więc takie rudy NIE mogą być celem baritone (render XRay nadal je pokazuje).
     */
    private boolean isNearWaterOrLava(BlockPos pos) {
        if (XRay.mc.world == null) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockState s = XRay.mc.world.getBlockState(pos.add(dx, dy, dz));
                    if (s.isOf(Blocks.WATER) || s.isOf(Blocks.LAVA)
                            || !s.getFluidState().isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Gdy avoid_water wlaczone, podbij kare za chodzenie po wodzie w baritone (3 -> 1000), zeby
     * trasowal WOKOL wody, nie przez nia. Bez tego baritone wchodzil do wody PO DRODZE do suchej
     * rudy i tam drgal/spamowal pakietami. Gdy wylaczone — przywroc domyslna kare (3).
     */
    private void applyWaterAvoidance() {
        boolean enabled = this.baritoneAvoidWater.isEnabled();
        BaritoneBridge.setWalkOnWaterOnePenalty(enabled ? 1000.0 : 3.0);
        BaritoneBridge.setAvoidWaterBlocks(enabled);
    }

    @Override
    public void onEnable() {
        if (!EntityUtility.isInGame()) {
            return;
        }

        this.lastSelectionHash = this.blocks.getSelectedValues().hashCode();
        this.lastLegitMode = this.legitMode.isEnabled();
        this.lastRange = this.range.getCurrentValue();
        this.enableAutoEat();
        this.updateEnabledBlocks();

        if (XRay.mc.worldRenderer != null) {
            XRay.mc.worldRenderer.reload();
        }
        this.fullScan();
        super.onEnable();
    }

    private void enableAutoEat() {
        AutoEat autoEat = BKClientInit.getInstance().getModuleManager().getModule(AutoEat.class);
        if (autoEat != null && !autoEat.isEnabled()) {
            autoEat.setEnabled(true, true);
        }
    }

    @Override
    public void onDisable() {
        this.cancelActiveScan();
        this.stopBaritoneAutomation(true);
        this.cachedBlocks.clear();
        this.blockCounts.clear();
        this.diamonds = 0;
        this.ancient = 0;
        this.gold = 0;
        this.lapis = 0;
        if (XRay.mc.worldRenderer != null) {
            XRay.mc.worldRenderer.reload();
        }
        super.onDisable();
    }

    public void fullScan() {
        resetScanWorld();
        scanLimitReached = false;
        if (mc.world == null || mc.player == null || scanExecutor.isShutdown()) return;
        int px = mc.player.getBlockX() >> 4, pz = mc.player.getBlockZ() >> 4;
        int requestedRadius = Math.max(1, (int)Math.ceil(range.getCurrentValue() / 16F));
        int radius = Math.min(requestedRadius, Math.max(1, mc.options.getViewDistance().getValue() + 1));
        List<long[]> coordinates = new ArrayList<>();
        for (int x = px - radius; x <= px + radius; x++) for (int z = pz - radius; z <= pz + radius; z++) {
            if (mc.world.isChunkLoaded(x, z)) coordinates.add(new long[]{x, z, (long)(x-px)*(x-px)+(long)(z-pz)*(z-pz)});
        }
        coordinates.sort(Comparator.comparingLong(point -> point[2]));
        for (int i = 0; i < Math.min(225, coordinates.size()); i++) {
            long[] point = coordinates.get(i);
            enqueueChunk(net.minecraft.util.math.ChunkPos.toLong((int)point[0], (int)point[1]));
        }
    }

    private void cancelActiveScan() {
        scanGeneration.incrementAndGet();
        if (scanTask != null) scanTask.cancel(true);
        scanExecutor.purge();
        scanTask = null;
        pendingScanChunks.clear();
        pendingScanHits = List.of(); pendingHitIndex = 0;
        scanWorld = null;
    }

    public void shutdownScanner() {
        cancelActiveScan();
        scanExecutor.shutdownNow();
        cachedBlocks.clear(); blockCounts.clear();
    }

    @Override
    public void tick() {
        advanceScans();
        int currentHash = this.blocks.getSelectedValues().hashCode();
        boolean currentLegit = this.legitMode.isEnabled();
        float currentRange = this.range.getCurrentValue();

        if (currentHash != this.lastSelectionHash || currentLegit != this.lastLegitMode
                || currentRange != this.lastRange) {
            this.lastSelectionHash = currentHash;
            this.lastLegitMode = currentLegit;
            this.lastRange = currentRange;
            this.settingsTimer.reset();
            this.needsUpdate = true;
        }

        if (this.needsUpdate && this.settingsTimer.finished(250L)) {
            this.updateEnabledBlocks();
            this.fullScan();
            if (XRay.mc.worldRenderer != null) {
                XRay.mc.worldRenderer.reload();
            }
            this.needsUpdate = false;
        }

        if (this.cacheMaintenanceTimer.finished(500L)) {
            this.removeInvalidBlocks();
            this.countBlocks();
            this.cacheMaintenanceTimer.reset();
        }
        this.updateBaritoneEatingPause();
        this.updateBaritoneAutomation();
        this.updateTrashTrigger();
        this.updateTrashAutomation();
        this.updateBaritoneResumeAfterTrash();
        super.tick();
    }







    /**
     * Podczas jedzenia (AutoEat) gracz musi stać — ruch przerywa jedzenie. Gdy AutoEat zaczyna jeść,
     * zatrzymujemy Baritone i zapamiętujemy tryb; gdy skończy, wznawiamy poprzedni tryb. Nie ruszamy
     * przy tym pauzy sprzedaży (resumeBaritoneAfterSell) — to osobny mechanizm.
     */
    private void updateBaritoneEatingPause() {
        AutoEat autoEat = BKClientInit.getInstance().getModuleManager().getModule(AutoEat.class);
        boolean eating = autoEat != null && autoEat.isEnabled() && autoEat.shouldPauseMovement();

        if (eating) {
            // Nie przerywaj sprzedaży — tam Baritone i tak jest zatrzymany i wznawiany osobno.
            if (this.baritoneAutomationActive && !this.baritonePausedForEating
                    && !this.trashActive) {
                this.baritonePausedForEating = true;
                this.baritonePausedForEatingSingleTarget = this.baritoneSingleTarget;
                this.stopBaritoneAutomation(true);
            }
            return;
        }

        // Skończył jeść — wznów Baritone jeśli to my go spauzowaliśmy.
        if (this.baritonePausedForEating) {
            this.baritonePausedForEating = false;
            boolean singleTarget = this.baritonePausedForEatingSingleTarget;
            this.baritonePausedForEatingSingleTarget = false;
            if (this.trashActive) {
                return; // sprzedaż przejmuje wznawianie Baritone
            }
            if (XRay.mc.player != null && XRay.mc.world != null && XRay.mc.currentScreen == null) {
                this.startBaritoneAutomation(singleTarget);
            }
        }
    }





    private int getContainerSlotCount(ScreenHandler handler) {
        return Math.max(0, handler.slots.size() - 36);
    }


    /**
     * Biaa lista (keep-list) po TYPACH itemw — wyrzucamy wszystko OPRCZ wartociowych rzeczy.
     * Zachowujemy: rudy/dropy z XRay (zaznaczone), wszystkie narzdzia/bronie/zbroj (po klasie
     * itemu, wic apie kady materia i modowane), jedzenie (komponent FOOD), wiadra, totemy,
     * enderpearle. Pozostałe przedmioty są odrzucane. Lista zachowywanych przedmiotów jest stała.
     */
    private boolean shouldKeep(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        Item item = stack.getItem();

        if (stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)
                || stack.hasEnchantments()
                || item == net.minecraft.item.Items.ENCHANTED_BOOK) {
            return true;
        }

        // 1. Rudy/dropy z XRay (to co aktualnie zaznaczone do podwietlania/kopania)
        if (this.getSelectedXrayDrops().contains(item)) {
            return true;
        }

        // 2. Narzdzia / bronie / zbroja — po typie, apie kady materia
        if (item instanceof net.minecraft.item.MiningToolItem
                || item instanceof net.minecraft.item.SwordItem
                || item instanceof net.minecraft.item.ShearsItem
                || item instanceof net.minecraft.item.ArmorItem
                || item instanceof net.minecraft.item.RangedWeaponItem
                || item instanceof net.minecraft.item.TridentItem
                || item instanceof net.minecraft.item.FishingRodItem
                || item instanceof net.minecraft.item.ShieldItem
                || item instanceof net.minecraft.item.FlintAndSteelItem) {
            return true;
        }

        // 3. Jedzenie (dowolny item z komponentem FOOD)
        if (stack.contains(net.minecraft.component.DataComponentTypes.FOOD)) {
            return true;
        }
        if (stack.contains(net.minecraft.component.DataComponentTypes.POTION_CONTENTS)) {
            return true;
        }

        // 4. Wiadra (woda/lawa/puste), totemy, enderpearle
        if (item instanceof net.minecraft.item.BucketItem
                || item == net.minecraft.item.Items.TOTEM_OF_UNDYING
                || item == net.minecraft.item.Items.ENDER_PEARL
                || item == net.minecraft.item.Items.COBWEB
                || item == net.minecraft.item.Items.ENDER_CHEST
                || item instanceof net.minecraft.item.BlockItem blockItem
                        && blockItem.getBlock() instanceof net.minecraft.block.ShulkerBoxBlock) {
            return true;
        }

        return false;
    }

    /** Item is trash when it is not protected by the whitelist. */
    private boolean isTrashItem(ItemStack stack) {
        return !stack.isEmpty() && !this.shouldKeep(stack);
    }

    /** Liczba wolnych slotów w plecaku gracza (0-35). */
    private int countFreeInventorySlots() {
        if (XRay.mc.player == null) {
            return 0;
        }
        int free = 0;
        for (int i = 0; i < 36; i++) {
            if (XRay.mc.player.getInventory().getStack(i).isEmpty()) {
                free++;
            }
        }
        return free;
    }

    /** True when the player inventory contains any item outside the whitelist. */
    private boolean hasTrashInInventory() {
        if (XRay.mc.player == null) {
            return false;
        }
        for (int i = 0; i < 36; i++) {
            if (this.isTrashItem(XRay.mc.player.getInventory().getStack(i))) {
                return true;
            }
        }
        return false;
    }

    private void updateTrashTrigger() {
        if (!this.autoTrash.isEnabled() || this.trashActive) {
            return;
        }
        if (XRay.mc.player == null || XRay.mc.player.networkHandler == null) {
            return;
        }
        if (XRay.mc.currentScreen != null
                || XRay.mc.player.currentScreenHandler != XRay.mc.player.playerScreenHandler) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastTrashAttemptAt < 8000L) {
            return;
        }
        // Czyść tylko gdy plecak prawie pełny ORAZ jest co wyrzucić.
        if (this.countFreeInventorySlots() > 1) {
            return;
        }
        if (!this.hasTrashInInventory()) {
            return;
        }
        this.lastTrashAttemptAt = now;
        this.startTrashAutomation(true);
    }

    private void startTrashAutomation(boolean autoTriggered) {
        if (XRay.mc.player == null || XRay.mc.player.networkHandler == null) {
            this.notifyTrash(NotificationType.ERROR, "modules.notifications.xray.sell_enter_world");
            return;
        }
        if (!this.hasTrashInInventory()) {
            this.notifyTrash(NotificationType.INFO, "modules.notifications.xray.trash_no_items");
            return;
        }
        String command = this.trashCommand.getText() == null ? "" : this.trashCommand.getText().trim();
        if (command.isEmpty()) {
            this.notifyTrash(NotificationType.ERROR, "modules.notifications.xray.sell_command_empty");
            return;
        }
        // Zatrzymaj Baritone na czas czyszczenia (gracz musi stać przy GUI), wznów jak po sprzedaży.
        this.pauseBaritoneForTrash();

        this.trashActive = true;
        this.trashStage = 0;
        this.trashSlotIndex = 0;
        this.trashStageStartedAt = System.currentTimeMillis();
        this.trashClickedAny = false;
        this.trashAutoTriggered = autoTriggered;

        String normalized = command.startsWith("/") ? command.substring(1) : command;
        this.trashPendingCommand = normalized;
    }

    private void updateTrashAutomation() {
        if (!this.trashActive) {
            return;
        }
        if (XRay.mc.player == null || XRay.mc.interactionManager == null) {
            this.stopTrashAutomation();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.trashStageStartedAt > 15000L) {
            this.notifyTrash(NotificationType.ERROR, "modules.notifications.xray.sell_timeout");
            this.stopTrashAutomation();
            return;
        }
        if (this.trashStage == 0) {
            if (now - this.trashStageStartedAt < 400L) {
                return;
            }
            if (XRay.mc.currentScreen != null
                    || XRay.mc.player.currentScreenHandler != XRay.mc.player.playerScreenHandler) {
                return;
            }
            XRay.mc.player.networkHandler.sendChatCommand(this.trashPendingCommand);
            this.notifyTrash(NotificationType.INFO, "modules.notifications.xray.trash_opening", this.trashPendingCommand);
            this.trashStage = 1;
            this.trashStageStartedAt = now;
            return;
        }
        ScreenHandler handler = XRay.mc.player.currentScreenHandler;
        if (handler == null || handler == XRay.mc.player.playerScreenHandler) {
            return; // czekaj aż /kosz otworzy GUI
        }

        // Sloty gracza wewnątrz GUI kosza zaczynają się za slotami kontenera.
        if (this.trashStage == 1) {
            this.trashStage = 2;
            this.trashStageStartedAt = now;
            return;
        }
        if (this.trashStage == 2 && now - this.trashStageStartedAt < 180L) {
            return;
        }

        int containerSlots = this.getContainerSlotCount(handler);
        int totalSlots = handler.slots.size();

        // Szybkie wyrzucanie: do 9 shift-klikw na tick zamiast jednego z opnieniem.
        int clicksThisTick = 0;
        while (this.trashSlotIndex < (totalSlots - containerSlots)) {
            int slot = containerSlots + this.trashSlotIndex;
            ItemStack stack = handler.getSlot(slot).getStack();
            if (this.isTrashItem(stack)) {
                if (!this.clickContainerSlot(handler, slot, 1, SlotActionType.QUICK_MOVE)) {
                    this.notifyTrash(NotificationType.ERROR, "modules.notifications.xray.sell_slot_missing", slot);
                    this.stopTrashAutomation();
                    return;
                }
                this.trashClickedAny = true;
                this.trashStageStartedAt = now;
                this.trashSlotIndex++;
                if (++clicksThisTick >= 3) {
                    return; // oddaj tick, reszta w nastpnym
                }
                continue;
            }
            this.trashSlotIndex++;
        }

        this.notifyTrash(this.trashClickedAny ? NotificationType.SUCCESS : NotificationType.INFO,
                this.trashClickedAny ? "modules.notifications.xray.trash_done" : "modules.notifications.xray.trash_no_items");
        this.stopTrashAutomation();
    }

    private void stopTrashAutomation() {
        if (XRay.mc.player != null && XRay.mc.player.currentScreenHandler != null
                && XRay.mc.player.currentScreenHandler != XRay.mc.player.playerScreenHandler) {
            XRay.mc.player.closeHandledScreen();
            XRay.mc.setScreen(null);
        }
        this.trashActive = false;
        this.trashStage = 0;
        this.trashSlotIndex = 0;
        this.trashStageStartedAt = 0L;
        this.trashClickedAny = false;
        this.trashAutoTriggered = false;
        this.trashPendingCommand = "";
        this.scheduleBaritoneResumeAfterTrash();
    }

    private void notifyTrash(NotificationType type, String descKey, Object... args) {
        this.notify(type, Localizator.translate("modules.notifications.xray.trash_title"),
                Localizator.translate(descKey, args));
    }

    private boolean hasPlayerInventoryItem(Item item) {
        if (XRay.mc.player == null || item == null) {
            return false;
        }
        for (int i = 0; i < XRay.mc.player.getInventory().main.size(); i++) {
            ItemStack stack = XRay.mc.player.getInventory().main.get(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }





    private Set<Item> getSelectedXrayDrops() {
        Set<Item> items = new HashSet<>();
        for (SelectSetting.Value value : this.blocks.getSelectedValues()) {
            Block block = this.blockMap.get(value.getName());
            Item item = this.getDropItemForBlock(block);
            if (item != null && item != Items.AIR) {
                items.add(item);
            }
        }
        return items;
    }

    private Item getDropItemForBlock(Block block) {
        if (block == null) {
            return null;
        }
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return Items.DIAMOND;
        }
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            return Items.EMERALD;
        }
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return Items.LAPIS_LAZULI;
        }
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            return Items.REDSTONE;
        }
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
            return Items.COAL;
        }
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return Items.RAW_IRON;
        }
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            return Items.RAW_GOLD;
        }
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            return Items.RAW_COPPER;
        }
        if (block == Blocks.NETHER_GOLD_ORE || block == Blocks.GILDED_BLACKSTONE) {
            return Items.GOLD_NUGGET;
        }
        if (block == Blocks.NETHER_QUARTZ_ORE) {
            return Items.QUARTZ;
        }
        if (block == Blocks.ANCIENT_DEBRIS) {
            return Items.ANCIENT_DEBRIS;
        }
        return block.asItem();
    }

    private boolean clickContainerSlot(ScreenHandler handler, int slot, int button, SlotActionType actionType) {
        if (slot < 0 || slot >= handler.slots.size()) {
            return false;
        }
        XRay.mc.interactionManager.clickSlot(handler.syncId, slot, button, actionType, XRay.mc.player);
        return true;
    }







    private void pauseBaritoneForTrash() {
        if (!this.baritoneAutomationActive) {
            this.resumeBaritoneAfterTrash = false;
            this.resumeBaritoneSingleTargetAfterTrash = false;
            this.resumeBaritoneAfterTrashAt = 0L;
            return;
        }
        this.resumeBaritoneAfterTrash = true;
        this.resumeBaritoneSingleTargetAfterTrash = this.baritoneSingleTarget;
        this.resumeBaritoneAfterTrashAt = 0L;
        this.stopBaritoneAutomation(true);
    }

    private void scheduleBaritoneResumeAfterTrash() {
        if (!this.resumeBaritoneAfterTrash) {
            return;
        }
        this.resumeBaritoneAfterTrashAt = System.currentTimeMillis() + 750L;
    }

    private void updateBaritoneResumeAfterTrash() {
        if (!this.resumeBaritoneAfterTrash || this.trashActive) {
            return;
        }
        if (XRay.mc.player == null || XRay.mc.world == null) {
            this.resumeBaritoneAfterTrash = false;
            return;
        }
        if (XRay.mc.currentScreen != null || XRay.mc.player.currentScreenHandler != XRay.mc.player.playerScreenHandler) {
            return;
        }
        if (System.currentTimeMillis() < this.resumeBaritoneAfterTrashAt) {
            return;
        }

        boolean singleTarget = this.resumeBaritoneSingleTargetAfterTrash;
        this.resumeBaritoneAfterTrash = false;
        this.resumeBaritoneSingleTargetAfterTrash = false;
        this.resumeBaritoneAfterTrashAt = 0L;
        this.startBaritoneAutomation(singleTarget);
    }





    private Integer parseSlotNumber(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void addSlotIfValid(List<Integer> slots, int slot) {
        if (slot >= 0 && !slots.contains(slot)) {
            slots.add(slot);
        }
    }

    private void startBaritoneAutoMine() {
        this.startBaritoneAutomation(false);
    }

    private void startBaritoneMineNearest() {
        this.startBaritoneAutomation(true);
    }

    private void startBaritoneAutomation(boolean singleTarget) {
        if (XRay.mc.world == null || XRay.mc.player == null) {
            this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.mine_enter_world");
            return;
        }
        if (!BaritoneBridge.isAvailable()) {
            this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.baritone_unavailable",
                    BaritoneBridge.getLastError());
            return;
        }

        this.updateEnabledBlocks();
        if (!singleTarget) {
            this.startDefaultBaritoneMine();
            return;
        }

        BlockPos target = this.findNearestXrayTarget();
        if (target == null) {
            this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.no_target");
            return;
        }

        this.baritoneAutomationActive = true;
        this.baritoneSingleTarget = singleTarget;
        this.startBaritoneCluster(target);
        this.moveBaritoneToTarget(target);
    }

    private void startDefaultBaritoneMine() {
        List<String> blockNames = this.getSelectedBaritoneBlockNames();
        if (blockNames.isEmpty()) {
            this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.no_target");
            return;
        }

        BaritoneBridge.setAllowOnlyExposedOres(this.baritoneOnlyExposed.isEnabled());
        BaritoneBridge.setMineScanDroppedItems(this.baritonePickupDrops.isEnabled());
        BaritoneBridge.setMineDropLoiterDuration((long) (this.baritonePickupTime.getCurrentValue() * 1000.0f));
        this.applyWaterAvoidance();
        if (this.requiresPickaxe(blockNames) && !this.ensureSafeBaritonePickaxe()) {
            return;
        }
        BaritoneBridge.stop();

        int quantity = Math.round(this.baritoneQuantity.getCurrentValue());
        if (!BaritoneBridge.mineByName(quantity, blockNames)) {
            this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.mine_fail",
                    BaritoneBridge.getLastError());
            this.stopBaritoneAutomation(true);
            return;
        }

        this.baritoneAutomationActive = true;
        this.baritoneSingleTarget = false;
        this.baritoneTarget = null;
        this.baritoneTargetName = null;
        this.baritoneClusterOrigin = null;
        this.baritoneClusterName = null;
        this.baritoneClusterPositions = List.of();
        this.baritonePickupOrigin = null;
        this.baritoneStage = 4;
        this.baritoneStageStartedAt = System.currentTimeMillis();
        this.notifyBaritone(NotificationType.INFO, "modules.notifications.xray.mining", String.join(", ", blockNames));
    }

    private List<String> getSelectedBaritoneBlockNames() {
        List<String> names = new ArrayList<>();
        for (SelectSetting.Value value : this.blocks.getSelectedValues()) {
            Block block = this.blockMap.get(value.getName());
            String name = this.getBlockName(block);
            if (name != null && !names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private void updateBaritoneAutomation() {
        if (!this.baritoneAutomationActive) {
            return;
        }
        if (XRay.mc.world == null || XRay.mc.player == null) {
            this.stopBaritoneAutomation(true);
            return;
        }
        if (this.baritoneStage == 2 && this.requiresPickaxe(this.baritoneTargetName)
                && this.hasReservedPickaxeInHotbar()) {
            BaritoneBridge.stop();
            if (!this.ensureSafeBaritonePickaxe()) {
                return;
            }
            this.startMiningCurrentBaritoneTarget();
            return;
        }
        if (this.baritoneStage == 4 && this.requiresPickaxe(this.getSelectedBaritoneBlockNames())
                && this.hasReservedPickaxeInHotbar()) {
            BaritoneBridge.stop();
            if (!this.ensureSafeBaritonePickaxe()) {
                return;
            }
            this.startDefaultBaritoneMine();
            return;
        }
        if (this.baritoneStage == 4) {
            if (this.findNearestXrayTarget() == null) {
                this.stopBaritoneAutomation(false);
                this.notifyBaritone(NotificationType.SUCCESS, "modules.notifications.xray.no_more_targets");
            }
            return;
        }
        long now = System.currentTimeMillis();
        long stageElapsed = now - this.baritoneStageStartedAt;
        if (this.baritoneStage == 1 && stageElapsed > BARITONE_GOTO_TIMEOUT_MS) {
            this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.baritone_stuck");
            BaritoneBridge.stop();
            this.advanceBaritoneTarget();
            return;
        }
        if (this.baritoneStage == 2 && stageElapsed > BARITONE_MINE_TIMEOUT_MS) {
            this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.baritone_stuck");
            BaritoneBridge.stop();
            if (this.baritonePickupDrops.isEnabled()) {
                this.startPickupPhase();
            } else {
                this.advanceBaritoneTarget();
            }
            return;
        }
        if (this.baritoneStage == 3) {
            long pickupMs = (long) (this.baritonePickupTime.getCurrentValue() * 1000.0f);
            long pickupElapsed = now - this.baritoneStageStartedAt;
            if (stageElapsed >= pickupMs
                    || (pickupElapsed >= 1000L && !this.hasNearbyDroppedItems(this.baritonePickupOrigin))) {
                this.advanceBaritoneTarget();
            }
            return;
        }

        if (this.baritoneTarget == null) {
            this.advanceBaritoneTarget();
            return;
        }

        if (!this.isValidBaritoneTarget(this.baritoneTarget)) {
            if (this.baritoneStage == 2) {
                BlockPos clusterTarget = this.findNearestBaritoneClusterTarget();
                if (clusterTarget != null) {
                    this.baritoneTarget = clusterTarget.toImmutable();
                    return;
                }
                if (this.baritonePickupDrops.isEnabled()) {
                    this.startPickupPhase();
                } else {
                    this.advanceBaritoneTarget();
                }
            } else {
                this.advanceBaritoneTarget();
            }
            return;
        }

        if (this.baritoneStage == 1 && XRay.mc.player.squaredDistanceTo(this.baritoneTarget.toCenterPos()) <= 25.0) {
            this.startMiningCurrentBaritoneTarget();
        }
    }

    private void advanceBaritoneTarget() {
        if (this.baritoneSingleTarget) {
            this.stopBaritoneAutomation(false);
            this.notifyBaritone(NotificationType.SUCCESS, "modules.notifications.xray.target_done");
            return;
        }

        BlockPos nextTarget = this.findNearestXrayTarget();
        if (nextTarget == null) {
            this.stopBaritoneAutomation(false);
            this.notifyBaritone(NotificationType.SUCCESS, "modules.notifications.xray.no_more_targets");
            return;
        }

        this.startBaritoneCluster(nextTarget);
        this.moveBaritoneToTarget(nextTarget);
    }

    private void startBaritoneCluster(BlockPos target) {
        Block block = XRay.mc.world.getBlockState(target).getBlock();
        String blockName = this.getBlockName(block);
        this.baritoneClusterOrigin = target.toImmutable();
        this.baritoneClusterName = blockName;
        this.baritoneClusterPositions = this.buildBaritoneClusterPositions(target, blockName);
    }

    private void moveBaritoneToTarget(BlockPos target) {
        Block block = XRay.mc.world.getBlockState(target).getBlock();
        String blockName = this.getBlockName(block);
        if (blockName == null) {
            this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.block_name_fail");
            this.stopBaritoneAutomation(false);
            return;
        }

        BaritoneBridge.setAllowOnlyExposedOres(this.baritoneOnlyExposed.isEnabled());
        BaritoneBridge.setMineScanDroppedItems(true);
        BaritoneBridge.setMineDropLoiterDuration((long) (this.baritonePickupTime.getCurrentValue() * 1000.0f));
        this.applyWaterAvoidance();
        // NIE wołamy stop() przed goto — setGoalAndPath płynnie przekierowuje istniejącą ścieżkę.
        // Twardy cancelEverything+restart powodował szarpnięcie i snap-rotację głowy (flag anticheata).
        if (!BaritoneBridge.gotoInteractableBlock(target)) {
            this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.goto_fail",
                    BaritoneBridge.getLastError());
            this.stopBaritoneAutomation(false);
            return;
        }

        this.baritoneTarget = target.toImmutable();
        this.baritoneTargetName = blockName;
        if (this.baritoneClusterOrigin == null || this.baritoneClusterName == null
                || !this.baritoneClusterName.equals(blockName)) {
            this.startBaritoneCluster(target);
        }
        this.baritonePickupOrigin = this.baritoneClusterOrigin;
        this.baritoneStage = 1;
        this.baritoneStageStartedAt = System.currentTimeMillis();
        this.notifyBaritone(NotificationType.INFO, "modules.notifications.xray.going_to",
                blockName, target.getX(), target.getY(), target.getZ());
    }

    private void startMiningCurrentBaritoneTarget() {
        if (this.baritoneTargetName == null) {
            this.advanceBaritoneTarget();
            return;
        }

        List<BlockPos> positions = new ArrayList<>(this.getValidBaritoneClusterPositions());
        if (this.baritoneTarget != null && this.isValidBaritoneTarget(this.baritoneTarget)
                && !positions.contains(this.baritoneTarget)) {
            positions.add(0, this.baritoneTarget.toImmutable());
        }
        if (positions.isEmpty()) {
            this.advanceBaritoneTarget();
            return;
        }
        this.sortBaritonePositionsByDistanceToPlayer(positions);
        this.baritoneClusterPositions = positions;
        int configuredQuantity = Math.round(this.baritoneQuantity.getCurrentValue());
        int quantity = configuredQuantity <= 0 ? positions.size() : Math.min(configuredQuantity, positions.size());

        BaritoneBridge.setAllowOnlyExposedOres(this.baritoneOnlyExposed.isEnabled());
        BaritoneBridge.setMineScanDroppedItems(true);
        BaritoneBridge.setMineDropLoiterDuration((long) (this.baritonePickupTime.getCurrentValue() * 1000.0f));
        if (!this.prepareToolForBaritoneTarget(this.baritoneTargetName)) {
            this.stopBaritoneAutomation(true);
            return;
        }

        boolean started = BaritoneBridge.mineAtPositions(quantity, List.of(this.baritoneTargetName), positions);
        if (!started && this.shouldFallbackToBaritoneMineByName()) {
            started = BaritoneBridge.mineByName(quantity, List.of(this.baritoneTargetName));
        }
        if (!started) {
            this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.mine_fail",
                    BaritoneBridge.getLastError());
            this.stopBaritoneAutomation(true);
            return;
        }

        this.baritoneStage = 2;
        this.baritoneStageStartedAt = System.currentTimeMillis();
        this.notifyBaritone(NotificationType.INFO, "modules.notifications.xray.mining", this.baritoneTargetName);
    }

    private boolean prepareToolForBaritoneTarget(String blockName) {
        if (XRay.mc.player == null || XRay.mc.interactionManager == null) {
            return false;
        }
        if (!"cobweb".equals(blockName)) {
            return !this.requiresPickaxe(blockName) || this.ensureSafeBaritonePickaxe();
        }
        if (InventoryUtility.selectItemInHotbar(Items.SHEARS)) {
            return true;
        }
        int selectedSlot = XRay.mc.player.getInventory().selectedSlot;
        for (int i = 9; i < XRay.mc.player.getInventory().main.size(); i++) {
            ItemStack stack = XRay.mc.player.getInventory().main.get(i);
            if (!stack.isEmpty() && stack.getItem() == Items.SHEARS) {
                XRay.mc.interactionManager.clickSlot(XRay.mc.player.playerScreenHandler.syncId,
                        this.inventoryIndexToScreenSlot(i), selectedSlot, SlotActionType.SWAP, XRay.mc.player);
                InventoryUtility.selectHotbarSlot(selectedSlot);
                return true;
            }
        }
        this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.no_shears");
        return false;
    }

    private boolean ensureSafeBaritonePickaxe() {
        if (XRay.mc.player == null || XRay.mc.interactionManager == null) {
            return false;
        }

        if (!this.moveReservedPickaxesOutOfHotbar()) {
            this.notifyNoSafePickaxe();
            this.stopBaritoneAutomation(true);
            return false;
        }

        ItemStack currentStack = XRay.mc.player.getMainHandStack();
        if (this.isUsableBaritonePickaxe(currentStack)) {
            return this.keepOnlySelectedPickaxeOnHotbar(XRay.mc.player.getInventory().selectedSlot);
        }

        int hotbarSlot = this.findUsablePickaxeHotbarSlot();
        if (hotbarSlot != -1) {
            InventoryUtility.selectHotbarSlot(hotbarSlot);
            return this.keepOnlySelectedPickaxeOnHotbar(hotbarSlot);
        }

        int inventoryIndex = this.findUsablePickaxeInventoryIndex();
        if (inventoryIndex != -1) {
            int selectedSlot = XRay.mc.player.getInventory().selectedSlot;
            XRay.mc.interactionManager.clickSlot(XRay.mc.player.playerScreenHandler.syncId,
                    this.inventoryIndexToScreenSlot(inventoryIndex), selectedSlot, SlotActionType.SWAP, XRay.mc.player);
            InventoryUtility.selectHotbarSlot(selectedSlot);
            return this.keepOnlySelectedPickaxeOnHotbar(selectedSlot);
        }

        this.notifyNoSafePickaxe();
        this.stopBaritoneAutomation(true);
        return false;
    }

    private boolean moveReservedPickaxesOutOfHotbar() {
        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack stack = XRay.mc.player.getInventory().main.get(hotbarSlot);
            if (!this.isReservedBaritonePickaxe(stack)) {
                continue;
            }
            int inventoryIndex = this.findStorageSlotForReservedPickaxe();
            if (inventoryIndex == -1) {
                return false;
            }
            XRay.mc.interactionManager.clickSlot(XRay.mc.player.playerScreenHandler.syncId,
                    this.inventoryIndexToScreenSlot(inventoryIndex), hotbarSlot, SlotActionType.SWAP, XRay.mc.player);
        }
        return true;
    }

    private int findStorageSlotForReservedPickaxe() {
        int usablePickaxeSlot = -1;
        int fallbackSlot = -1;
        for (int i = 9; i < XRay.mc.player.getInventory().main.size(); i++) {
            ItemStack stack = XRay.mc.player.getInventory().main.get(i);
            if (stack.isEmpty()) {
                return i;
            }
            if (usablePickaxeSlot == -1 && this.isUsableBaritonePickaxe(stack)) {
                usablePickaxeSlot = i;
            }
            if (fallbackSlot == -1 && !this.isReservedBaritonePickaxe(stack)) {
                fallbackSlot = i;
            }
        }
        return usablePickaxeSlot != -1 ? usablePickaxeSlot : fallbackSlot;
    }

    private boolean keepOnlySelectedPickaxeOnHotbar(int selectedSlot) {
        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            if (hotbarSlot == selectedSlot) {
                continue;
            }
            ItemStack stack = XRay.mc.player.getInventory().main.get(hotbarSlot);
            if (!this.isPickaxe(stack)) {
                continue;
            }
            int inventoryIndex = this.findStorageSlotForExtraHotbarPickaxe();
            if (inventoryIndex == -1) {
                this.notifyHotbarPickaxeCleanupFailed();
                this.stopBaritoneAutomation(true);
                return false;
            }
            XRay.mc.interactionManager.clickSlot(XRay.mc.player.playerScreenHandler.syncId,
                    this.inventoryIndexToScreenSlot(inventoryIndex), hotbarSlot, SlotActionType.SWAP, XRay.mc.player);
        }
        return true;
    }

    private int findStorageSlotForExtraHotbarPickaxe() {
        for (int i = 9; i < XRay.mc.player.getInventory().main.size(); i++) {
            if (XRay.mc.player.getInventory().main.get(i).isEmpty()) {
                return i;
            }
        }
        for (int i = 9; i < XRay.mc.player.getInventory().main.size(); i++) {
            if (!this.isPickaxe(XRay.mc.player.getInventory().main.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasReservedPickaxeInHotbar() {
        if (XRay.mc.player == null) {
            return false;
        }
        for (int i = 0; i < 9; i++) {
            if (this.isReservedBaritonePickaxe(XRay.mc.player.getInventory().main.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean requiresPickaxe(List<String> blockNames) {
        for (String blockName : blockNames) {
            if (this.requiresPickaxe(blockName)) {
                return true;
            }
        }
        return false;
    }

    private boolean requiresPickaxe(String blockName) {
        return blockName != null && !"cobweb".equals(blockName);
    }

    private int findUsablePickaxeHotbarSlot() {
        int bestSlot = -1;
        int bestRemaining = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = XRay.mc.player.getInventory().main.get(i);
            int remaining = this.remainingDurability(stack);
            if (this.isUsableBaritonePickaxe(stack) && remaining > bestRemaining) {
                bestRemaining = remaining;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private int findUsablePickaxeInventoryIndex() {
        int bestIndex = -1;
        int bestRemaining = -1;
        for (int i = 9; i < XRay.mc.player.getInventory().main.size(); i++) {
            ItemStack stack = XRay.mc.player.getInventory().main.get(i);
            int remaining = this.remainingDurability(stack);
            if (this.isUsableBaritonePickaxe(stack) && remaining > bestRemaining) {
                bestRemaining = remaining;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private boolean isUsableBaritonePickaxe(ItemStack stack) {
        return this.isPickaxe(stack) && this.remainingDurability(stack) > BARITONE_PICKAXE_RESERVED_USES;
    }

    private boolean isReservedBaritonePickaxe(ItemStack stack) {
        return this.isPickaxe(stack) && this.remainingDurability(stack) <= BARITONE_PICKAXE_RESERVED_USES;
    }

    private boolean isPickaxe(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof PickaxeItem;
    }

    private int remainingDurability(ItemStack stack) {
        if (!this.isPickaxe(stack) || !stack.isDamageable() || stack.getMaxDamage() <= 0) {
            return 0;
        }
        return stack.getMaxDamage() - stack.getDamage();
    }

    private void notifyNoSafePickaxe() {
        this.notify(NotificationType.ERROR, "XRay Baritone",
                "Brak kilofa z wiecej niz " + BARITONE_PICKAXE_RESERVED_USES + " uzyciami.");
    }

    private void notifyHotbarPickaxeCleanupFailed() {
        this.notify(NotificationType.ERROR, "XRay Baritone",
                "Nie moge odlozyc dodatkowych kilofow z hotbara.");
    }

    private int inventoryIndexToScreenSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
    }

    private void startPickupPhase() {
        this.baritonePickupOrigin = this.baritoneTarget == null ? this.baritonePickupOrigin : this.baritoneTarget;
        this.baritoneTarget = null;
        this.baritoneTargetName = null;
        this.baritoneStage = 3;
        this.baritoneStageStartedAt = System.currentTimeMillis();

        if (!BaritoneBridge.pickupAll()) {
            this.notifyBaritone(NotificationType.ERROR, "modules.notifications.xray.pickup_fail",
                    BaritoneBridge.getLastError());
            this.advanceBaritoneTarget();
            return;
        }

        this.notifyBaritone(NotificationType.INFO, "modules.notifications.xray.pickup_started");
    }

    private void stopBaritoneAutomation(boolean stopBaritone) {
        this.baritoneAutomationActive = false;
        this.baritoneSingleTarget = false;
        this.baritoneTarget = null;
        this.baritoneTargetName = null;
        this.baritoneClusterOrigin = null;
        this.baritoneClusterName = null;
        this.baritoneClusterPositions = List.of();
        this.baritonePickupOrigin = null;
        this.baritoneStage = 0;
        this.baritoneStageStartedAt = 0L;
        if (stopBaritone) {
            BaritoneBridge.stop();
        }
    }

    private BlockPos findNearestXrayTarget() {
        if (XRay.mc.player == null) {
            return null;
        }

        List<BlockPos> snapshot = new ArrayList<>(this.cachedBlocks);
        return snapshot.stream()
                .filter(this::isValidBaritoneTarget)
                .min(Comparator.comparingDouble(pos -> XRay.mc.player.squaredDistanceTo(pos.toCenterPos())))
                .orElse(null);
    }

    private List<BlockPos> buildBaritoneClusterPositions(BlockPos target, String blockName) {
        if (XRay.mc.world == null || target == null || blockName == null) {
            return List.of();
        }

        double radiusSq = this.baritoneVeinRadius.getCurrentValue() * this.baritoneVeinRadius.getCurrentValue();
        List<BlockPos> positions = new ArrayList<>(this.cachedBlocks);
        positions.removeIf(pos -> !this.isValidBaritoneTarget(pos)
                || target.getSquaredDistance(pos) > radiusSq
                || !blockName.equals(this.getBlockName(XRay.mc.world.getBlockState(pos).getBlock())));
        this.sortBaritonePositionsByDistanceToPlayer(positions);
        return positions.stream()
                .map(BlockPos::toImmutable)
                .toList();
    }

    private List<BlockPos> getValidBaritoneClusterPositions() {
        if (this.baritoneClusterPositions.isEmpty()) {
            return List.of();
        }

        List<BlockPos> positions = new ArrayList<>(this.baritoneClusterPositions);
        positions.removeIf(pos -> !this.isValidBaritoneTarget(pos)
                || (this.baritoneClusterName != null
                && !this.baritoneClusterName.equals(this.getBlockName(XRay.mc.world.getBlockState(pos).getBlock()))));
        this.sortBaritonePositionsByDistanceToPlayer(positions);
        return positions;
    }

    private void sortBaritonePositionsByDistanceToPlayer(List<BlockPos> positions) {
        if (XRay.mc.player == null) {
            return;
        }
        positions.sort(Comparator.comparingDouble(pos -> XRay.mc.player.squaredDistanceTo(pos.toCenterPos())));
    }

    private BlockPos findNearestBaritoneClusterTarget() {
        if (XRay.mc.player == null) {
            return null;
        }

        List<BlockPos> positions = this.getValidBaritoneClusterPositions();
        this.baritoneClusterPositions = positions;
        return positions.stream()
                .min(Comparator.comparingDouble(pos -> XRay.mc.player.squaredDistanceTo(pos.toCenterPos())))
                .orElse(null);
    }

    private boolean shouldFallbackToBaritoneMineByName() {
        String error = BaritoneBridge.getLastError();
        return error != null && error.contains("NoSuchMethodException") && error.contains("mineAtPositions");
    }

    private boolean isValidBaritoneTarget(BlockPos pos) {
        if (XRay.mc.world == null || XRay.mc.player == null || pos == null) {
            return false;
        }

        BlockState state = XRay.mc.world.getBlockState(pos);
        if (state.isAir() || !this.isBlockEnabled(state.getBlock())) {
            return false;
        }
        // Baritone omija rudy przy wodzie/lawie (gdy wlaczone) — tam sie buguje, szarpany ruch + spam pakietow.
        if (this.baritoneAvoidWater.isEnabled() && this.isNearWaterOrLava(pos)) {
            return false;
        }
        return !this.baritoneOnlyExposed.isEnabled() || this.isExposed(pos);
    }

    private String getBlockName(Block block) {
        net.minecraft.util.Identifier id = Registries.BLOCK.getId(block);
        if (id == null) {
            return null;
        }
        return id.getPath();
    }

    /**
     * Wywoływane z AutoMarket.handleTitleText dla każdego tytułu/podtytułu serwera (surowy tekst,
     * zachowana wielkość liter). Gdy auto-captcha jest włączone i tytuł zawiera "Wpisz: /captcha XXXX",
     * automatycznie wysyła komendę. Odstęp 3 s i pomijanie powtórzeń tego samego kodu ograniczają spam, gdy
     * tytuł pozostaje na ekranie przez wiele ticków.
     */
    public void onServerTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isEmpty() || !this.isEnabled()) {
            return;
        }
        if (XRay.mc.player == null || XRay.mc.player.networkHandler == null) {
            return;
        }
        java.util.regex.Matcher m = CAPTCHA_PATTERN.matcher(rawTitle);
        if (!m.find()) {
            return;
        }
        String code = m.group(1);
        long now = System.currentTimeMillis();
        // Ten sam kod wysany niedawno — nie powtarzaj (tytu wisi przez wiele klatek).
        if (code.equals(this.lastCaptchaCode) && now - this.lastCaptchaSentMs < 5000L) {
            return;
        }
        if (now - this.lastCaptchaSentMs < 3000L) {
            return;
        }
        this.lastCaptchaCode = code;
        this.lastCaptchaSentMs = now;
        XRay.mc.player.networkHandler.sendChatCommand("captcha " + code);
        this.notify(NotificationType.INFO, "XRay AutoCaptcha", "Wpisano kod: " + code);
    }

    private boolean hasNearbyDroppedItems(BlockPos origin) {
        if (XRay.mc.world == null || origin == null) {
            return false;
        }

        Box pickupBox = new Box(origin).expand(12.0);
        return !XRay.mc.world.getEntitiesByClass(ItemEntity.class, pickupBox, entity -> !entity.isRemoved()).isEmpty();
    }

    private void notify(NotificationType type, String title, String desc) {
        BKClientInit.getInstance().getNotificationManager().addNotificationOther(type, title, desc, 4000L);
    }

    private void notifyBaritone(NotificationType type, String descKey, Object... args) {
        this.notify(type, Localizator.translate("modules.notifications.xray.baritone_title"),
                Localizator.translate(descKey, args));
    }

















    private void updateEnabledBlocks() {
        this.enabledBlocksSet.clear();
        for (SelectSetting.Value value : this.blocks.getSelectedValues()) {
            Block block = this.blockMap.get(value.getName());
            if (block == null)
                continue;
            this.enabledBlocksSet.add(block);
        }
    }

    private void removeInvalidBlocks() {
        this.cachedBlocks.removeIf(pos -> !this.isInRenderDistance((BlockPos) pos));
    }

    private void countBlocks() {
        int d = 0;
        int a = 0;
        int g = 0;
        int l = 0;
        Set<BlockPos> set = this.cachedBlocks;
        synchronized (set) {
            for (BlockPos pos : this.cachedBlocks) {
                Block block = XRay.mc.world.getBlockState(pos).getBlock();
                if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
                    ++d;
                    continue;
                }
                if (block == Blocks.ANCIENT_DEBRIS) {
                    ++a;
                    continue;
                }
                if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
                    ++g;
                    continue;
                }
                if (block != Blocks.LAPIS_ORE && block != Blocks.DEEPSLATE_LAPIS_ORE)
                    continue;
                ++l;
            }
        }
        this.diamonds = d;
        this.ancient = a;
        this.gold = g;
        this.lapis = l;

        Map<Block, Integer> counts = new HashMap<>();
        synchronized (set) {
            for (BlockPos pos : this.cachedBlocks) {
                Block block = XRay.mc.world.getBlockState(pos).getBlock();
                if (this.isBlockEnabled(block)) {
                    counts.put(block, counts.getOrDefault(block, 0) + 1);
                }
            }
        }
        this.blockCounts.clear();
        this.blockCounts.putAll(counts);
    }

    public ColorRGBA getBlockColor(Block block) {
        if (block == Blocks.ANCIENT_DEBRIS) {
            return new ColorRGBA(255.0f, 131.0f, 54.0f);
        }
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return new ColorRGBA(121.0f, 54.0f, 255.0f);
        }
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            return new ColorRGBA(255.0f, 215.0f, 0.0f);
        }
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            return new ColorRGBA(0.0f, 71.0f, 179.0f);
        }
        int color = block.getDefaultMapColor().color;
        float r = (float) (color >> 16 & 0xFF);
        float g = (float) (color >> 8 & 0xFF);
        float b = (float) (color & 0xFF);
        return new ColorRGBA(r, g, b);
    }

    private boolean isInRenderDistance(BlockPos pos) {
        return XRay.mc.player != null && XRay.mc.player
                .squaredDistanceTo(pos.toCenterPos()) < this.range.getCurrentValue() * this.range.getCurrentValue();
    }

    // Render-thread scratch for onRender3D, and the per-block fill colour (getBlockColor(b).withAlpha(30)).
    private final Matrix4f renderScratch = new Matrix4f();
    private final Map<Block, ColorRGBA> fillColorCache = new java.util.IdentityHashMap<>();

    private Box getBoundingBox(BlockPos blockEntity, BlockState blockState) {
        VoxelShape shape = blockState.getOutlineShape((BlockView) XRay.mc.world, blockEntity);
        if (shape.isEmpty()) {
            return new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).offset(blockEntity);
        }
        return shape.getBoundingBox().offset(blockEntity);
    }

    public boolean isBlockEnabled(Block block) {
        return this.enabledBlocksSet.contains(block);
    }

    @Generated
    public Set<BlockPos> getCachedBlocks() {
        return this.cachedBlocks;
    }

    @Generated
    public int getDiamonds() {
        return this.diamonds;
    }

    @Generated
    public int getAncient() {
        return this.ancient;
    }

    @Generated
    public int getGold() {
        return this.gold;
    }

    @Generated
    public int getLapis() {
        return this.lapis;
    }

    public Map<Block, Integer> getBlockCounts() {
        return this.blockCounts;
    }
}
