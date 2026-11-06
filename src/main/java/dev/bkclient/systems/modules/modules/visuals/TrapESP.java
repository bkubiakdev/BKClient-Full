package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.event.impl.render.PreHudRenderEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.WorldUtility;
import dev.bkclient.utility.render.Utils;
import dev.bkclient.utility.time.Timer;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ModuleInfo(name = "Trap ESP", category = ModuleCategory.VISUALS)
public class TrapESP extends BaseModule {

    private final SliderSetting range = new SliderSetting(this,
            "modules.settings.trap_esp.range", "modules.settings.trap_esp.range.desc")
            .min(16).max(200).step(1).currentValue(100);

    private final SliderSetting minCount = new SliderSetting(this,
            "modules.settings.trap_esp.min_count", "modules.settings.trap_esp.min_count.desc")
            .min(1).max(30).step(1).currentValue(6);

    private final SelectSetting scanFor = new SelectSetting(this, "modules.settings.trap_esp.scan_for");
    private final SelectSetting.Value sPaintings     = new SelectSetting.Value(scanFor, "modules.settings.trap_esp.scan_for.paintings").select();
    private final SelectSetting.Value sSigns         = new SelectSetting.Value(scanFor, "modules.settings.trap_esp.scan_for.signs").select();
    private final SelectSetting.Value sButtons       = new SelectSetting.Value(scanFor, "modules.settings.trap_esp.scan_for.buttons");
    private final SelectSetting.Value sLevers        = new SelectSetting.Value(scanFor, "modules.settings.trap_esp.scan_for.levers");
    private final SelectSetting.Value sBanners       = new SelectSetting.Value(scanFor, "modules.settings.trap_esp.scan_for.banners").select();
    private final SelectSetting.Value sArmorStands   = new SelectSetting.Value(scanFor, "modules.settings.trap_esp.scan_for.armor_stands").select();
    private final SelectSetting.Value sMinecarts     = new SelectSetting.Value(scanFor, "modules.settings.trap_esp.scan_for.minecarts").select();
    private final SelectSetting.Value sFallingBlocks = new SelectSetting.Value(scanFor, "modules.settings.trap_esp.scan_for.falling_blocks").select();
    private final SelectSetting.Value sDecoPots      = new SelectSetting.Value(scanFor, "modules.settings.trap_esp.scan_for.decoration_pots");

    private final SliderSetting clusterRadius = new SliderSetting(this,
            "modules.settings.trap_esp.cluster_radius", "modules.settings.trap_esp.cluster_radius.desc")
            .min(1).max(64).step(1).currentValue(16);

    private volatile List<Cluster> clusters = Collections.emptyList();
    private final Timer scanTimer = new Timer();


    private final EventListener<PreHudRenderEvent> onHud = ev -> {
        if (mc.player == null || mc.world == null) return;

        Vec3d pp      = mc.player.getPos();
        if (scanTimer.finished(500L)) {
            float r = range.getCurrentValue();
            int minC = (int) minCount.getCurrentValue();
            int cr = (int) clusterRadius.getCurrentValue();
            List<Vec3d> hits = new ArrayList<>();

        // block entities — O(loaded BEs), typically <500 even in dense areas
            for (BlockEntity be : WorldUtility.getBlockEntities()) {
                Vec3d pos = Vec3d.ofCenter(be.getPos());
                if (pos.distanceTo(pp) > r) continue;
                if (matchesBlockEntity(be)) hits.add(pos);
            }

        // also check block state for buttons/levers (no block entity) — but only nearby chunks
        // to avoid iterating millions of blocks, we only check a small radius
            if (sButtons.isSelected() || sLevers.isSelected()) {
                int smallR = Math.min((int) r, 32);
                net.minecraft.util.math.BlockPos from = mc.player.getBlockPos().add(-smallR, -smallR, -smallR);
                net.minecraft.util.math.BlockPos to = mc.player.getBlockPos().add(smallR, smallR, smallR);
                if (mc.world.isDebugWorld()) {
                    // Debug-world chunks synthesize states outside their sections; keep the plain scan there.
                    for (net.minecraft.util.math.BlockPos bpos : net.minecraft.util.math.BlockPos.iterate(from, to)) {
                        BlockState state = mc.world.getBlockState(bpos);
                        if (state.isAir()) continue;
                        Block block = state.getBlock();
                        if ((sButtons.isSelected() && block instanceof ButtonBlock)
                                || (sLevers.isSelected() && block instanceof LeverBlock)) {
                            hits.add(Vec3d.ofCenter(bpos));
                        }
                    }
                } else {
                    scanSwitchBlocks(from, to, sButtons.isSelected(), sLevers.isSelected(), hits);
                }
            }

            Box box = new Box(pp.x - r, pp.y - r, pp.z - r, pp.x + r, pp.y + r, pp.z + r);
            for (Entity entity : mc.world.getEntitiesByClass(Entity.class, box, this::matchesEntity)) {
                hits.add(entity.getPos());
            }
            clusters = cluster(hits, cr, minC);
            scanTimer.reset();
        }

        if (clusters.isEmpty()) return;

        UIContext ctx = UIContext.of(ev.getContext(),
            mc.currentScreen == null ? -1 : (int) dev.bkclient.utility.gui.GuiUtility.getMouse().getX(),
            mc.currentScreen == null ? -1 : (int) dev.bkclient.utility.gui.GuiUtility.getMouse().getY(),
            net.minecraft.client.MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false));

        Font bold   = Fonts.BOLD.getFont(9.0f);
        Font medium = Fonts.MEDIUM.getFont(8.0f);
        Font small  = Fonts.SEMIBOLD.getFont(7.0f);

        for (Cluster cl : clusters) {
            Vec2f screen = Utils.worldToScreen(cl.center.add(0, 0.5, 0));
            if (screen == null) continue;

            int dist = (int) pp.distanceTo(cl.center);
            String titleStr = "Trap Detected";
            String distStr  = dist + "m";
            String objLabel = "OBJECTS";
            String countStr = String.valueOf(cl.count);
            String coordStr = cl.x + " " + cl.y + " " + cl.z;

            float panelW = Math.max(
                bold.width(titleStr) + small.width(distStr) + 20.0f,
                Math.max(small.width(objLabel) + medium.width(countStr) + 20.0f,
                         medium.width(coordStr) + 20.0f));

            float row1H  = bold.height()   + 8.0f;
            float row2H  = medium.height() + 10.0f;
            float row3H  = medium.height() + 8.0f;
            float totalH = row1H + row2H + row3H;

            ctx.getMatrices().push();
            ctx.getMatrices().translate(screen.x - panelW / 2.0f, screen.y - totalH - 4.0f, 0.0f);

            // row 1 — title + distance
            ctx.drawRect(0, 0, panelW, row1H, new ColorRGBA(15, 15, 15, 200));
            ctx.drawText(bold, titleStr, 6.0f, row1H / 2.0f - bold.height() / 2.0f, Colors.getTextColor());
            ctx.drawText(small, distStr,
                panelW - small.width(distStr) - 6.0f, row1H / 2.0f - small.height() / 2.0f,
                Colors.getTextColor().withAlpha(160.0f));

            // row 2 — OBJECTS count + bar
            float y2 = row1H;
            ctx.drawRect(0, y2, panelW, row2H, new ColorRGBA(10, 10, 10, 180));

            float textBlockW = small.width(objLabel) + 6.0f + medium.width(countStr);
            float textStartX = 6.0f;
            ctx.drawText(small, objLabel,
                textStartX, y2 + 3.0f,
                Colors.getAccent());
            ctx.drawText(medium, countStr,
                textStartX + small.width(objLabel) + 4.0f, y2 + 3.0f,
                Colors.getTextColor());

            // bar — full width, capped at 50 objects = full bar
            float barX = 6.0f;
            float barY = y2 + row2H - 4.5f;
            float barW = panelW - 12.0f;
            float barH = 2.0f;
            float fill = Math.min(cl.count / 50.0f, 1.0f);
            ctx.drawRect(barX, barY, barW, barH, Colors.getAdditionalColor().withAlpha(100.0f));
            ctx.drawRect(barX, barY, barW * fill, barH, Colors.getAccent());

            // row 3 — coords
            float y3 = row1H + row2H;
            ctx.drawRect(0, y3, panelW, row3H, new ColorRGBA(10, 10, 10, 160));
            ctx.drawText(small, "\u29BF",
                6.0f, y3 + row3H / 2.0f - small.height() / 2.0f,
                Colors.getAccent());
            ctx.drawText(medium, coordStr,
                6.0f + small.width("\u29BF") + 4.0f, y3 + row3H / 2.0f - medium.height() / 2.0f,
                Colors.getTextColor().withAlpha(200.0f));

            ctx.getMatrices().pop();
        }
    };

    private final EventListener<WorldChangeEvent> onWorldChange = event -> clusters = Collections.emptyList();

    @Override
    public void onDisable() {
        clusters = Collections.emptyList();
        super.onDisable();
    }

    /**
     * Same hits, in the same order, as iterating BlockPos.iterate(from, to) (x fastest, then y, then z) and
     * testing every state, but without touching the up to 274,625 positions one by one. World#getBlockState
     * resolves to chunk section (x>>4, y>>4, z>>4) and returns air for out-of-height positions, unloaded
     * (empty) chunks and empty sections, so any section that is out of range, empty, or whose palette has no
     * button/lever state (ChunkSection#hasAny is a superset test: stale palette entries only cause a scan)
     * cannot produce a hit and its x-run is skipped. Positions in candidate sections go through the exact
     * original test via World#getBlockState.
     */
    private static void scanSwitchBlocks(net.minecraft.util.math.BlockPos from, net.minecraft.util.math.BlockPos to,
                                         boolean buttons, boolean levers, List<Vec3d> hits) {
        int minX = Math.min(from.getX(), to.getX()), maxX = Math.max(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY()), maxY = Math.max(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ()), maxZ = Math.max(from.getZ(), to.getZ());
        int minCx = minX >> 4, minSy = minY >> 4, minCz = minZ >> 4;
        int sizeCx = (maxX >> 4) - minCx + 1, sizeSy = (maxY >> 4) - minSy + 1, sizeCz = (maxZ >> 4) - minCz + 1;

        java.util.function.Predicate<BlockState> target = state -> {
            Block block = state.getBlock();
            return (buttons && block instanceof ButtonBlock) || (levers && block instanceof LeverBlock);
        };
        // candidate[(cz * sizeSy + sy) * sizeCx + cx]: section may contain a target state.
        boolean[] candidate = new boolean[sizeCx * sizeSy * sizeCz];
        boolean any = false;
        for (int cz = 0; cz < sizeCz; cz++) {
            for (int cx = 0; cx < sizeCx; cx++) {
                net.minecraft.world.chunk.WorldChunk chunk = mc.world.getChunk(minCx + cx, minCz + cz);
                net.minecraft.world.chunk.ChunkSection[] sections = chunk.getSectionArray();
                int bottomSection = chunk.getBottomSectionCoord();
                for (int sy = 0; sy < sizeSy; sy++) {
                    int index = minSy + sy - bottomSection;
                    if (index < 0 || index >= sections.length) continue;
                    net.minecraft.world.chunk.ChunkSection section = sections[index];
                    if (section == null || section.isEmpty() || !section.hasAny(target)) continue;
                    candidate[(cz * sizeSy + sy) * sizeCx + cx] = true;
                    any = true;
                }
            }
        }
        if (!any) return;

        net.minecraft.util.math.BlockPos.Mutable bpos = new net.minecraft.util.math.BlockPos.Mutable();
        for (int z = minZ; z <= maxZ; z++) {
            int czRow = (z >> 4) - minCz;
            for (int y = minY; y <= maxY; y++) {
                int rowBase = (czRow * sizeSy + ((y >> 4) - minSy)) * sizeCx;
                int x = minX;
                while (x <= maxX) {
                    int sectionEnd = Math.min(maxX, ((x >> 4) << 4) + 15);
                    if (!candidate[rowBase + ((x >> 4) - minCx)]) {
                        x = sectionEnd + 1;
                        continue;
                    }
                    for (; x <= sectionEnd; x++) {
                        BlockState state = mc.world.getBlockState(bpos.set(x, y, z));
                        if (state.isAir()) continue;
                        Block block = state.getBlock();
                        if ((buttons && block instanceof ButtonBlock) || (levers && block instanceof LeverBlock)) {
                            hits.add(Vec3d.ofCenter(bpos));
                        }
                    }
                }
            }
        }
    }


    private boolean matchesBlockEntity(BlockEntity be) {
        if (sSigns.isSelected()  && be instanceof SignBlockEntity)          return true;
        if (sBanners.isSelected()&& be instanceof BannerBlockEntity)        return true;
        if (sDecoPots.isSelected()&& be instanceof DecoratedPotBlockEntity) return true;
        return false;
    }

    private boolean matchesEntity(Entity e) {
        if (sPaintings.isSelected()    && e instanceof PaintingEntity)         return true;
        if (sArmorStands.isSelected()  && e instanceof ArmorStandEntity)       return true;
        if (sMinecarts.isSelected()    && e instanceof AbstractMinecartEntity) return true;
        if (sFallingBlocks.isSelected()&& e instanceof FallingBlockEntity)     return true;
        return false;
    }


    private List<Cluster> cluster(List<Vec3d> hits, int radius, int minC) {
        // deduplicate hits that are on the same block
        List<Vec3d> deduped = new ArrayList<>();
        for (Vec3d h : hits) {
            boolean dup = false;
            for (Vec3d d : deduped) { if (d.distanceTo(h) < 0.5) { dup = true; break; } }
            if (!dup) deduped.add(h);
        }

        List<Vec3d> remaining = new ArrayList<>(deduped);
        List<List<Vec3d>> groups = new ArrayList<>();

        while (!remaining.isEmpty()) {
            Vec3d seed = remaining.remove(0);
            List<Vec3d> group = new ArrayList<>();
            group.add(seed);
            // collect all within radius (not just first pass — repeat until stable)
            boolean changed = true;
            while (changed) {
                changed = false;
                List<Vec3d> toRemove = new ArrayList<>();
                for (Vec3d p : remaining) {
                    boolean close = false;
                    for (Vec3d g : group) { if (g.distanceTo(p) <= radius) { close = true; break; } }
                    if (close) { group.add(p); toRemove.add(p); changed = true; }
                }
                remaining.removeAll(toRemove);
            }
            groups.add(group);
        }

        List<Cluster> result = new ArrayList<>();
        for (List<Vec3d> group : groups) {
            if (group.size() < minC) continue;
            double cx = group.stream().mapToDouble(v -> v.x).average().orElse(0);
            double cy = group.stream().mapToDouble(v -> v.y).average().orElse(0);
            double cz = group.stream().mapToDouble(v -> v.z).average().orElse(0);
            result.add(new Cluster(new Vec3d(cx, cy, cz), group.size(), (int)cx, (int)cy, (int)cz));
        }
        return result;
    }


    static class Cluster {
        Vec3d center; int count, x, y, z;
        Cluster(Vec3d c, int n, int x, int y, int z) { center=c; count=n; this.x=x; this.y=y; this.z=z; }
    }
}
