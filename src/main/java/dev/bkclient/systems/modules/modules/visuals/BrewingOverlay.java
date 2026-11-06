package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.event.impl.render.PreHudRenderEvent;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.Utils;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.brewing.BrewingClock;
import net.minecraft.block.Blocks;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import java.util.*;

@ModuleInfo(name = "Brewing Overlay", category = ModuleCategory.VISUALS, desc = "modules.descriptions.brewing_overlay")
public final class BrewingOverlay extends BaseModule {
    private final SliderSetting range = new SliderSetting(this,"modules.settings.brewing_overlay.range").min(4).max(32).step(1).currentValue(16);
    private final SliderSetting scale = new SliderSetting(this,"modules.settings.brewing_overlay.scale").min(0.5f).max(1.5f).step(0.05f).currentValue(0.8f);
    private final SliderSetting speed = new SliderSetting(this,"modules.settings.brewing_overlay.speed").min(0).max(2).step(0.1f).currentValue(0.6f);
    private final BooleanSetting connections = new BooleanSetting(this,"modules.settings.brewing_overlay.connections").enable();
    private final Map<BlockPos, Contents> known = new HashMap<>();
    private final List<BlockPos> nearby = new ArrayList<>();
    private BlockPos pendingPos;
    private long pendingTick;
    private BlockPos openPos;
    private BrewingStandScreenHandler openHandler;
    private long ticks;
    private double orbit;
    private long lastFrame;
    private long generation;
    private boolean localReadPending;

    private final EventListener<WorldChangeEvent> onWorldChange = event -> clear();
    private final EventListener<PreHudRenderEvent> onHud = event -> render(event.getContext(),event.getTickDelta());

    /** Associates the next server container with the stand actually clicked by the player. */
    public void noteInteraction(BlockPos pos) {
        if (!isEnabled() || mc.world == null) return;
        pendingPos = mc.world.getBlockState(pos).isOf(Blocks.BREWING_STAND) ? pos.toImmutable() : null;
        pendingTick = ticks;
    }

    @Override public void tick() {
        if(mc.world == null || mc.player == null) { clear(); return; }
        ticks++;
        if(ticks%5==0) readLocalServer();
        if(mc.player.currentScreenHandler instanceof BrewingStandScreenHandler handler) {
            if(handler != openHandler) {
                openHandler = handler;
                openPos = pendingPos != null && ticks-pendingTick <= 40 ? pendingPos : null;
                pendingPos = null;
            }
            if(openPos != null && mc.world.getBlockState(openPos).isOf(Blocks.BREWING_STAND)) {
                Contents data = known.computeIfAbsent(openPos, ignored -> new Contents());
                for(int i=0;i<3;i++) data.bottles[i] = handler.getSlot(i).getStack().copy();
                data.ingredient = handler.getSlot(3).getStack().copy();
                data.clock.observe(handler.getBrewTime(),ticks);
            }
        } else { openHandler=null; openPos=null; }
        if(ticks%10 == 1) {
            nearby.clear();
            collectStands();
            nearby.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(mc.player.getPos())));
            if(nearby.size()>32) nearby.subList(32,nearby.size()).clear();
            known.entrySet().removeIf(entry -> ticks-entry.getValue().clock.observedAt()>1200
                || !mc.world.isChunkLoaded(entry.getKey().getX()>>4,entry.getKey().getZ()>>4)
                || !mc.world.getBlockState(entry.getKey()).isOf(Blocks.BREWING_STAND));
        }
    }

    @Override public void onEnable() {
        if(mc.world != null && mc.player != null) { nearby.clear(); collectStands(); readLocalServer(); }
    }

    private void collectStands() {
        int radius=(int)Math.ceil(range.getCurrentValue());
        BlockPos player=mc.player.getBlockPos();
        for(int cx=(player.getX()-radius)>>4;cx<=(player.getX()+radius)>>4;cx++) {
            for(int cz=(player.getZ()-radius)>>4;cz<=(player.getZ()+radius)>>4;cz++) {
                var chunk=mc.world.getChunk(cx,cz,net.minecraft.world.chunk.ChunkStatus.FULL,false);
                if(!(chunk instanceof net.minecraft.world.chunk.WorldChunk loaded)) continue;
                for(var entity:loaded.getBlockEntities().values()) {
                    if(entity instanceof BrewingStandBlockEntity && entity.getPos().getSquaredDistance(mc.player.getPos()) <= radius*radius)
                        nearby.add(entity.getPos().toImmutable());
                }
            }
        }
    }

    // Read the integrated server on its own thread: its inventories and brew timer are authoritative.
    private void readLocalServer() {
        var server=mc.getServer();
        if(server==null || localReadPending || nearby.isEmpty()) return;
        var world=mc.world;
        var dimension=world.getRegistryKey();
        var positions=List.copyOf(nearby);
        long requestGeneration=generation;
        localReadPending=true;
        server.execute(() -> {
            Map<BlockPos,Contents> samples=new HashMap<>();
            var serverWorld=server.getWorld(dimension);
            if(serverWorld!=null) for(BlockPos pos:positions) {
                if(!serverWorld.isChunkLoaded(pos.getX()>>4,pos.getZ()>>4)) continue;
                if(serverWorld.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand) {
                    Contents data=new Contents();
                    for(int i=0;i<3;i++) data.bottles[i]=stand.getStack(i).copy();
                    data.ingredient=stand.getStack(3).copy();
                    data.clock.observe(((dev.bkclient.mixin.accessors.BrewingStandAccessor)stand).bk$getBrewTime(),0);
                    data.authoritative=true;
                    samples.put(pos,data);
                }
            }
            mc.execute(() -> {
                if(generation!=requestGeneration || mc.world!=world || !isEnabled()) return;
                localReadPending=false;
                samples.forEach((pos,data)-> { data.clock.observe(data.clock.sampleTicks(),ticks); known.put(pos,data); });
            });
        });
    }

    private void render(CustomDrawContext original,float tickDelta) {
        UIContext context=UIContext.of(original,0,0,tickDelta);
        if(mc.world == null || mc.player == null) return;
        long now=System.nanoTime();
        if(lastFrame!=0 && !mc.isPaused()) orbit += Math.min((now-lastFrame)/1e9,0.1)*speed.getCurrentValue();
        lastFrame=now;
        if(nearby.isEmpty()) return;
        // Pre-HUD rendering runs before the normal HUD refreshes the glass background.
        if(DrawUtility.blurProgram!=null) DrawUtility.blurProgram.draw();
        List<BlockPos> ordered=new ArrayList<>(nearby);
        Vec3d camera=mc.gameRenderer.getCamera().getPos();
        ordered.sort(Comparator.comparingDouble((BlockPos pos) -> pos.getSquaredDistance(camera)).reversed());
        for(BlockPos pos:ordered) {
            if(!mc.world.getBlockState(pos).isOf(Blocks.BREWING_STAND)) continue;
            Vec3d center=Vec3d.ofCenter(pos);
            double distance=camera.distanceTo(center);
            if(distance>range.getCurrentValue()) continue;
            var hit=mc.world.raycast(new RaycastContext(camera,center.add(0,0.5,0),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,mc.player));
            if(hit.getType()!=HitResult.Type.MISS && !hit.getBlockPos().equals(pos)) continue;
            drawStand(context,pos,distance,tickDelta);
        }
    }

    private void drawStand(UIContext context,BlockPos pos,double distance,float tickDelta) {
        Contents data=known.get(pos);
        // A zero estimate does not confirm completion; refresh by opening the stand again.
        boolean live=pos.equals(openPos) || data!=null && data.authoritative;
        if(data!=null && !live && ticks-data.clock.observedAt()>data.clock.sampleTicks()+200) data=null;
        var state=mc.world.getBlockState(pos);
        List<Orb> orbs=new ArrayList<>();
        Vec3d center=Vec3d.ofCenter(pos);
        List<ItemStack> outputs=new ArrayList<>();
        boolean predicted=false;
        for(int i=0;i<3;i++) {
            if(data==null && !state.get(BrewingStandBlock.BOTTLE_PROPERTIES[i])) continue;
            ItemStack item=data==null ? new ItemStack(Items.GLASS_BOTTLE) : data.bottles[i];
            if(item.isEmpty()) continue;
            if(data!=null) {
                ItemStack result=mc.world.getBrewingRecipeRegistry().craft(data.ingredient,item);
                predicted |= !ItemStack.areItemsAndComponentsEqual(item,result);
                item=result;
                ItemStack candidate=item;
                if(outputs.stream().noneMatch(stack -> ItemStack.areItemsAndComponentsEqual(stack,candidate))) outputs.add(item);
            }
            double angle=orbit+i*Math.PI*2/3;
            Vec3d point=center.add(Math.cos(angle)*0.7,0.52+Math.sin(angle*2+orbit)*0.065,Math.sin(angle)*0.7);
            orbs.add(new Orb(point,item));
        }

        Vec3d upper=center.add(0,1.5+Math.sin(orbit*1.4)*0.06,0);
        Vec2f upperScreen=Utils.worldToScreen(upper);
        if(upperScreen==null) return;
        float size=MathHelper.clamp(5f/(float)Math.max(distance,1),0.3f,1.2f)*scale.getCurrentValue();
        ColorRGBA accent=Colors.getAccent();
        if(connections.isEnabled()) for(Orb orb:orbs) {
            Vec2f screen=Utils.worldToScreen(orb.position());
            if(screen!=null) context.drawLine(screen,upperScreen,accent.withAlpha(45));
        }
        orbs.sort(Comparator.comparingDouble((Orb orb)->orb.position().squaredDistanceTo(mc.gameRenderer.getCamera().getPos())).reversed());
        for(Orb orb:orbs) drawOrb(context,orb.position(),orb.item(),size,accent);
        if(data!=null && !data.ingredient.isEmpty()) drawOrb(context,upper,data.ingredient,size,accent);
        String text;
        if(data==null) text=Localizator.translate("modules.brewing_overlay.open");
        else if(data.clock.sampleTicks()==0) text=live ? Localizator.translate("modules.brewing_overlay.idle") : Localizator.translate("modules.brewing_overlay.refresh");
        else {
            double remaining=data.clock.remaining(ticks+(mc.isPaused()?0:tickDelta),live);
            text=String.format(Locale.ROOT,"%s%d%% · %.1fs",live?"":"~",(int)Math.floor((1-remaining/400)*100),remaining/20);
        }
        var matrices=context.getMatrices();
        matrices.push();
        try {
            matrices.translate(upperScreen.x,upperScreen.y-22*size,0);
            matrices.scale(size,size,1);
            var font=Fonts.MEDIUM.getFont(8);
            float width=Math.max(76,font.width(text)+16);
            for(ItemStack output:outputs) width=Math.max(width,font.width(output.getName().getString())+34);
            float height=outputs.isEmpty()?23:28+outputs.size()*18;
            HudTheme.drawPanel(context,-width/2,-height,width,height,1,0,3);
            if(!outputs.isEmpty()) {
                String heading=Localizator.translate(predicted?"modules.brewing_overlay.result":"modules.brewing_overlay.contents");
                context.drawText(Fonts.MEDIUM.getFont(6),heading,-width/2+8,-height+6,HudTheme.textColor());
                float row=-height+17;
                for(ItemStack output:outputs) {
                    context.drawItem(output,-width/2+7,row-2,0.75f);
                    context.drawText(font,output.getName().getString(),-width/2+23,row,HudTheme.strongTextColor());
                    row+=18;
                }
            }
            context.drawText(font,text,-font.width(text)/2,-14,accent);
        } finally { matrices.pop(); }
    }

    private void drawOrb(UIContext context,Vec3d position,ItemStack item,float scale,ColorRGBA accent) {
        Vec2f screen=Utils.worldToScreen(position);
        if(screen==null) return;
        var matrices=context.getMatrices();
        matrices.push();
        try {
            matrices.translate(screen.x,screen.y,0);
            matrices.scale(scale,scale,1);
            HudTheme.drawPanel(context,-12,-12,24,24,1,0,3);
            context.drawItem(item,-7,-7,0.875f);
        } finally { matrices.pop(); }
    }

    private void clear() {
        generation++; localReadPending=false;
        known.clear(); nearby.clear(); pendingPos=null; openPos=null; openHandler=null; ticks=0; lastFrame=0; orbit=0;
    }
    @Override public void onDisable() { clear(); }
    private record Orb(Vec3d position,ItemStack item) {}
    private static final class Contents {
        final ItemStack[] bottles={ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};
        ItemStack ingredient=ItemStack.EMPTY;
        boolean authoritative;
        final BrewingClock clock=new BrewingClock();
    }
}
