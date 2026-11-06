package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.utility.render.lyrics.*;
import dev.bkclient.utility.sounds.*;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.util.*;

@ModuleInfo(name="Lyrics 3D",category=ModuleCategory.VISUALS,desc="modules.descriptions.lyrics_3d")
public final class Lyrics3D extends BaseModule {
    public final ButtonSetting editor=new ButtonSetting(this,"modules.settings.lyrics_3d.editor")
        .action(()->mc.setScreen(new dev.bkclient.ui.menu.lyrics.LyricsEditorScreen(this,mc.currentScreen)));
    // Keep the module panel compact; the editor and profiles use the full settings list.
    @Override public List<dev.bkclient.systems.setting.Setting> getDisplaySettings(){return List.of(editor);}
    public final LyricsOptions options=new LyricsOptions(this,this::replay);
    // Public aliases and serialized keys retained for previous configurations/integrations.
    public final ModeSetting displayMode=options.display;
    public final ModeSetting.Value fullVerse=displayMode.getValues().get(0),wordByWord=displayMode.getValues().get(1);
    public final SliderSetting distance=options.distance,height=options.height,size=options.size,opacity=options.opacity,offset=options.offset,animation=options.animation;
    public final ColorSetting color=options.color;
    public final BooleanSetting shadow=options.shadow,throughWalls=options.throughWalls,approximate=options.approximate,preview=options.preview;
    private static final SyncedLyrics PREVIEW=SyncedLyrics.parseLrc(
        "[00:00.00]Muzyka zostawia ślad\n[00:04.00]Słowa płyną przez świat\n[00:08.00]Zatrzymaj się na chwilę\n[00:12.00]To jest podgląd napisów");
    private final LyricsRenderer renderer=new LyricsRenderer();
    private final Deque<LyricsVerse> history=new ArrayDeque<>();
    private ClientWorld world;
    private String track="";
    private LyricsTimeline.Cue cue;
    private LyricsVerse current,previous;
    private long previewStart,lastNanos,lastPosition,pauseStart;
    private boolean wasPreview,failed;
    private float entryDelay;
    private volatile boolean resourcesDirty;
    private int currentEnd=Integer.MAX_VALUE;
    private final EventListener<Render3DEvent> render=this::renderLyrics;

    public Lyrics3D() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            public Identifier getFabricId(){return Identifier.of("bkclient","lyrics_fonts");}
            public void reload(ResourceManager manager){resourcesDirty=true;LyricsRenderer.resourcesReloaded();}
        });
    }
    @Override public void onEnable(){clear();failed=false;}
    @Override public void onDisable(){onWorldChanged();}
    public void onWorldChanged(){clear();world=null;renderer.close();}
    public void replay(){clear();failed=false;}
    private void clear(){current=previous=null;history.clear();pieces.clear();pending.clear();pieceLine=null;comets.reset();cue=null;track="";previewStart=lastNanos=lastPosition=pauseStart=0;entryDelay=0;}
    private void renderLyrics(Render3DEvent event) {
        if(failed||mc.player==null||mc.world==null)return;
        try { renderFrame(event); }
        catch(RuntimeException error){failed=true;BKClientInit.LOGGER.error("Lyrics 3D renderer disabled; toggle module to retry",error);onWorldChanged();}
    }
    private void renderFrame(Render3DEvent event) {
        if(world!=mc.world){onWorldChanged();world=mc.world;}
        if(resourcesDirty){renderer.close();if(current!=null)current.layoutKey=null;if(previous!=null)previous.layoutKey=null;for(var v:history)v.layoutKey=null;resourcesDirty=false;}
        long now=System.nanoTime();double dt=lastNanos==0?0:(now-lastNanos)/1e9;
        if(wasPreview!=preview.isEnabled()){clear();wasPreview=preview.isEnabled();}
        MusicTracker.LyricsPlayback playback;
        if(preview.isEnabled()) {
            if(previewStart==0)previewStart=now;
            playback=new MusicTracker.LyricsPlayback("BK:preview",PREVIEW,(now-previewStart)/1_000_000%16000,16000,true);
        } else {
            var tracker=BKClientInit.getInstance().getMusicTracker();if(tracker==null){clear();return;}
            tracker.setApproximateLyrics(approximate.isEnabled());playback=tracker.getLyricsPlayback();

        }
        boolean seek=lastNanos!=0&&(playback.positionMs()<lastPosition-500||playback.positionMs()-lastPosition>(now-lastNanos)/1_000_000+1500);
        boolean reset=!track.equals(playback.track())||seek;
        if(reset){current=previous=null;history.clear();cue=null;pauseStart=0;track=playback.track();}
        lastPosition=playback.positionMs();lastNanos=now;
        if(options.comets.isEnabled())comets.render(event,Math.round(options.cometCount.getCurrentValue()),options.cometColor.getColor(),options.cometSize.getCurrentValue()/100,options.opacity.getCurrentValue()/100);
        else comets.reset();
        if(options.scattered()){current=previous=null;history.clear();cue=null;renderPieces(event,playback,now,reset);return;}
        if(!pieces.isEmpty()||!pending.isEmpty()){pieces.clear();pending.clear();pieceLine=null;}
        LyricsTimeline.Cue next=playback.playing()?LyricsTimeline.at(playback.lyrics(),playback.positionMs(),(long)offset.getCurrentValue(),playback.durationMs()):null;
        if(next==null) {
            if(pauseStart==0)pauseStart=now;
            if(now-pauseStart>=300_000_000L){current=previous=null;history.clear();cue=null;}
        } else {
            pauseStart=0;
            if(!Objects.equals(cue,next)) {
                previous=null;entryDelay=0;
                if(current!=null) {
                    current.retired=now;
                    current.retiredAlpha=options.entryAt((now-current.born)/1e6f-entryDelay,current.entranceMs).alpha();
                    current.settledEnd=current.visibleEnd;current.wordProgress=1;
                    if(options.historyCount.getCurrentValue()>0)history.addFirst(current);
                    else {previous=current;entryDelay=Math.min(options.exitDurationMs(),Math.min(options.entryDurationMs()*.32f,(next.endMs()-next.startMs())*.15f));}
                }
                cue=next;current=place(event,next,now);
            }
        }
        while(history.size()>(int)options.historyCount.getCurrentValue())history.removeLast();
        history.removeIf(v->(now-v.retired)/1e9>=options.historyTime.getCurrentValue());
        var camera=event.getCamera().getPos();Vector3d eye=new Vector3d(camera.x,camera.y,camera.z);
        if(current!=null) {
            layout(current);current.scale=current.baseScale*size.getCurrentValue()/100;
            if(pauseStart==0) {
                if(options.follow.isEnabled())current.pose.follow(target(event,current.layout),true,dt,options.smoothing.getCurrentValue());
                current.pose.face(eye,options.face.isEnabled());
                var f=LyricsWordReveal.frame(current.wordEnds,current.cue.wordMs(),current.cue.startMs(),current.cue.endMs(),playback.positionMs()+(long)offset.getCurrentValue(),options.revealPercent.getCurrentValue()/100.,options.wordGap.getCurrentValue(),options.wordDurationMs());
                // Word by word and Scattered both reveal words progressively.
                boolean reveal=!fullVerse.isSelected();
                current.visibleEnd=reveal?f.visibleEnd():Integer.MAX_VALUE;
                current.settledEnd=reveal?f.settledEnd():Integer.MAX_VALUE;
                current.wordProgress=reveal?1-(float)Math.cbrt(1-f.progress()):1;
                currentEnd=current.visibleEnd;
            }
        }
        List<LyricsRenderer.Draw> draws=new ArrayList<>(4);
        float pause=pauseStart==0?1:1-LyricsAnimation.ease((now-pauseStart)/300_000_000f);
        int rank=history.size();
        for(var it=history.descendingIterator();it.hasNext();) {
            var v=it.next();layout(v);v.scale=v.baseScale*size.getCurrentValue()/100;v.pose.face(eye,options.face.isEnabled());
            float age=(now-v.retired)/1e6f,life=options.historyTime.getCurrentValue()*1000;
            float exitMs=Math.min(options.exitDurationMs(),life);
            var effect=options.exitAt(Math.max(0,age-(life-exitMs)),exitMs);
            draws.add(new LyricsRenderer.Draw(v,effect,pause*v.retiredAlpha*options.historyAlpha.getCurrentValue()/100,rank--*options.historyOffset.getCurrentValue()*LyricsAnimation.ease(age/400)));
        }
        if(previous!=null) {
            layout(previous);previous.pose.face(eye,options.face.isEnabled());
            float elapsed=(now-previous.retired)/1e6f;
            if(elapsed>=entryDelay)previous=null;
            else draws.add(new LyricsRenderer.Draw(previous,options.exitAt(elapsed,entryDelay),pause*previous.retiredAlpha,0));
        }
        if(current!=null&&previous==null) {
            draws.add(new LyricsRenderer.Draw(current,options.entryAt((now-current.born)/1e6f-entryDelay,current.entranceMs),pause,0));
        }
        renderer.render(event,draws,options,now/1e9);
    }
    private void layout(LyricsVerse v) { v.ensureLayout(options,renderer.fonts.metrics(LyricsOptions.id(options.font))); }

    // Scattered: each piece of the line gets its own spot in the world, at most two stay on screen.
    private static final float[][] SLOTS={{.28f,.36f},{.72f,.32f},{.38f,.64f},{.64f,.58f},{.24f,.52f},{.56f,.42f}};
    private static final int MAX_PIECES=10;
    private static final long PIECE_HOLD_MS=350,WORD_LEAD_MS=250;
    private static final float WORD_REVEAL_MS=360,BOB=.06f,BOB_PERIOD=.9f;
    private final List<LyricsVerse> pieces=new ArrayList<>();
    private final ArrayDeque<LyricsPieces.Piece> pending=new ArrayDeque<>();
    private final Random random=new Random();
    private final LyricsComets comets=new LyricsComets();
    private LyricsTimeline.Cue pieceLine;
    private int lastSlot=-1;

    private void renderPieces(Render3DEvent event,MusicTracker.LyricsPlayback playback,long now,boolean reset) {
        long position=playback.positionMs()+(long)offset.getCurrentValue();
        float exitMs=Math.max(100,options.exitDurationMs());
        if(reset){pending.clear();pieceLine=null;leaveAll(now);}
        LyricsTimeline.Cue line=playback.playing()?LyricsTimeline.at(playback.lyrics(),playback.positionMs(),(long)offset.getCurrentValue(),playback.durationMs()):null;
        if(line==null) {
            if(pauseStart==0)pauseStart=now;
            if(now-pauseStart>=300_000_000L){pending.clear();pieceLine=null;leaveAll(now);}
        } else {
            pauseStart=0;
            if(!Objects.equals(line,pieceLine)) {
                pieceLine=line;pending.clear();
                pending.addAll(LyricsPieces.plan(line.text(),line.startMs(),line.endMs(),Math.round(options.scatterWords.getCurrentValue()),line.wordMs()));
            }
            while(!pending.isEmpty()&&pending.peekFirst().spawnMs()<=position)spawn(event,pending.pollFirst(),line,now);
        }
        for(var v:pieces) {
            if(v.retired==0&&line!=null&&position>v.lineEndMs+PIECE_HOLD_MS)v.retired=now;
            if(pauseStart==0)reveal(v,position);
        }
        pieces.removeIf(v->v.retired!=0&&(now-v.retired)/1e6f>=exitMs);
        float pause=pauseStart==0?1:1-LyricsAnimation.ease((now-pauseStart)/300_000_000f);
        double seconds=now/1e9;
        List<LyricsRenderer.Draw> draws=new ArrayList<>(pieces.size());
        for(var v:pieces) {
            if(v.anchorEye!=null&&v.anchorDistance>0) {
                // Distance moves visible pieces along their view ray at once (closer = bigger on screen).
                float k=distance.getCurrentValue()/v.anchorDistance;
                v.pose.position.set(v.anchorOffset).mul(k).add(v.anchorEye);v.baseScale=v.anchorScale;
            }
            layout(v);v.scale=v.baseScale*size.getCurrentValue()/100;
            var effect=v.retired==0?new LyricsAnimation.Transform(1,1,0,0,0):options.exitAt((now-v.retired)/1e6f,exitMs);
            draws.add(new LyricsRenderer.Draw(v,effect,pause,(float)Math.sin(seconds/BOB_PERIOD+v.bobPhase)*BOB));
        }
        renderer.render(event,draws,options,seconds);
    }
    private void leaveAll(long now){for(var v:pieces)if(v.retired==0)v.retired=now;}
    /** Words of a piece appear one by one at their song times. */
    private static void reveal(LyricsVerse v,long position) {
        int shown=-1;
        for(int i=0;i<v.wordRevealMs.length;i++)if(position+WORD_LEAD_MS>=v.wordRevealMs[i])shown=i;
        int words=Math.min(v.wordRevealMs.length,v.wordEnds.length);
        if(shown<0||words==0){v.visibleEnd=v.settledEnd=-1;v.wordProgress=0;return;}
        shown=Math.min(shown,words-1);
        float progress=Math.clamp((position+WORD_LEAD_MS-v.wordRevealMs[shown])/WORD_REVEAL_MS,0,1);
        v.visibleEnd=v.wordEnds[shown];
        v.settledEnd=progress>=1?v.visibleEnd:shown>0?v.wordEnds[shown-1]:0;
        v.wordProgress=progress;
    }
    private void spawn(Render3DEvent event,LyricsPieces.Piece piece,LyricsTimeline.Cue line,long now) {
        var v=new LyricsVerse(new LyricsTimeline.Cue(line.index(),piece.text(),piece.reveal()[0],line.endMs()),new LyricsPose(new Vector3d(),new Quaternionf()),now);
        v.piece=true;v.emphasisWord=piece.emphasisWord();v.wordRevealMs=piece.reveal();v.lineEndMs=line.endMs();
        v.bobPhase=random.nextFloat()*(float)(Math.PI*2);v.visibleEnd=v.settledEnd=-1;
        layout(v);
        float spread=Math.clamp(options.scatterSpread.getCurrentValue()/100*.7f,0,1.75f);
        int slot=random.nextInt(SLOTS.length);
        if(slot==lastSlot)slot=(slot+1+random.nextInt(SLOTS.length-1))%SLOTS.length;
        lastSlot=slot;
        float sx=.5f+(SLOTS[slot][0]-.5f)*spread+(random.nextFloat()*2-1)*.04f,sy=.5f+(SLOTS[slot][1]-.5f)*spread+(random.nextFloat()*2-1)*.04f;
        var projection=event.getProjectionMatrix();Quaternionf camera=event.getCamera().getRotation();
        Vector3f direction=new Vector3f((sx*2-1)/projection.m00(),(1-sy*2)/projection.m11(),-1).rotate(camera).normalize();
        Vec3d eye=event.getCamera().getPos();
        float d=Math.max(1.8f,distance.getCurrentValue()+(random.nextFloat()*2-1)*options.scatterDepth.getCurrentValue());
        if(!throughWalls.isEnabled()) {
            Vec3d end=eye.add(direction.x*(d+.6),direction.y*(d+.6),direction.z*(d+.6));
            var hit=mc.world.raycast(new RaycastContext(eye,end,RaycastContext.ShapeType.VISUAL,RaycastContext.FluidHandling.NONE,mc.player));
            if(hit.getType()!=HitResult.Type.MISS)d=(float)Math.max(1.8,eye.distanceTo(hit.getPos())-.6);
        }
        Vector3d origin=new Vector3d(eye.x,eye.y,eye.z);
        v.pose.position.set(eye.x+direction.x*d,eye.y+direction.y*d,eye.z+direction.z*d);
        v.pose.face(origin,true);
        float tilt=(float)Math.toRadians(options.scatterTilt.getCurrentValue());
        v.pose.rotation.rotateY((random.nextFloat()*2-1)*tilt).rotateX((random.nextFloat()*2-1)*tilt*.43f).rotateZ((random.nextFloat()*2-1)*tilt*.25f);
        // One line (ten layout units) is 0.6 m tall in the world, so a closer Distance shows bigger words.
        v.baseScale=.06f;
        Vector3f forward=new Vector3f(0,0,-1).rotate(camera);
        Vector3d center=new Vector3d(eye.x+forward.x*d,eye.y+forward.y*d,eye.z+forward.z*d);
        for(int i=0;i<8&&!fits(v,origin,camera,projection);i++) {
            if(i<5)v.pose.position.lerp(center,.3);else v.baseScale*=.82f;
        }
        v.anchorEye=origin;v.anchorOffset=new Vector3d(v.pose.position).sub(origin);
        v.anchorDistance=distance.getCurrentValue();v.anchorScale=v.baseScale;
        // The newest older piece stays, everything before it starts to leave: two pieces on screen.
        int live=0;
        for(int i=pieces.size()-1;i>=0;i--){var p=pieces.get(i);if(p.retired==0&&++live>=2)p.retired=now;}
        pieces.add(v);
        while(pieces.size()>MAX_PIECES)pieces.removeFirst();
    }
    /** All four corners of the piece inside the safe screen area. */
    private boolean fits(LyricsVerse v,Vector3d eye,Quaternionf camera,org.joml.Matrix4f projection) {
        float half=v.scatter!=null&&v.scatter.chunks.length>0?v.scatter.chunks[0].halfWidth():v.layout.width()/2;
        float bottom=10*(v.emphasisWord>=0&&options.emphasis.isEnabled()?options.emphasisSize.getCurrentValue()/100:1);
        float s=v.baseScale*size.getCurrentValue()/100;
        Quaternionf inverse=new Quaternionf(camera).conjugate();
        for(int i=0;i<4;i++) {
            Vector3f corner=new Vector3f((i%2==0?-half:half)*s,-(i<2?0:bottom)*s,0).rotate(v.pose.rotation);
            Vector3f view=new Vector3f((float)(v.pose.position.x-eye.x)+corner.x,(float)(v.pose.position.y-eye.y)+corner.y,(float)(v.pose.position.z-eye.z)+corner.z).rotate(inverse);
            if(view.z>-.05f)return false;
            float x=view.x*projection.m00()/-view.z,y=view.y*projection.m11()/-view.z;
            if(Math.abs(x)>.84f||Math.abs(y)>.8f)return false;
        }
        return true;
    }
    private LyricsVerse place(Render3DEvent event,LyricsTimeline.Cue cue,long now) {
        var v=new LyricsVerse(cue,new LyricsPose(new Vector3d(),new Quaternionf()),now);layout(v);
        var p=target(event,v.layout);v.pose.position.set(p);
        var camera=event.getCamera().getPos();v.pose.face(new Vector3d(camera.x,camera.y,camera.z),true);
        // Fit only at birth. Movement never rescales a verse anchored in the world.
        float d=distance.getCurrentValue();float clearance=(float)(p.distance(camera.x,camera.y,camera.z)/Math.max(.001,d));
        // Scattered verses spread around the anchor, so fit the expected cloud width instead of one long row.
        float fitWidth=options.scattered()?260*options.scatterSpread.getCurrentValue()/100+60:v.layout.width();
        v.baseScale=Math.min(.025f,Math.min(1.2f*d/(Math.abs(event.getProjectionMatrix().m00())*fitWidth),.42f*d/(Math.abs(event.getProjectionMatrix().m11())*v.layout.height())))*Math.min(1,clearance);
        v.scale=v.baseScale*size.getCurrentValue()/100;
        v.entranceMs=Math.max(40,(cue.endMs()-cue.startMs())*.45f);
        return v;
    }
    private Vector3d target(Render3DEvent event,LyricsLayout layout) {
        float d=distance.getCurrentValue(),h=Math.clamp(height.getCurrentValue(),-d/Math.abs(event.getProjectionMatrix().m11())*.55f,d/Math.abs(event.getProjectionMatrix().m11())*.55f);
        Vector3f anchor=new Vector3f(0,h,-d).rotate(event.getCamera().getRotation());Vec3d eye=event.getCamera().getPos();float clearance=1;
        if(!throughWalls.isEnabled()) {
            float width=options.scattered()?260*options.scatterSpread.getCurrentValue()/100+60:layout.width();
            float halfW=(width*.5f+2)*.025f*size.getCurrentValue()/100,halfH=(layout.height()*.5f+2)*.025f*size.getCurrentValue()/100;
            for(int i=0;i<5;i++) {
                Vector3f ray=i==4?new Vector3f(anchor):new Vector3f((i%2==0?-1:1)*halfW,(i<2?-1:1)*halfH,0).rotate(event.getCamera().getRotation()).add(anchor);
                Vec3d end=eye.add(ray.x,ray.y,ray.z);var hit=mc.world.raycast(new RaycastContext(eye,end,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,mc.player));
                if(hit.getType()!=HitResult.Type.MISS)clearance=Math.min(clearance,(float)(eye.distanceTo(hit.getPos())/eye.distanceTo(end))*.8f);
            }
        }
        anchor.mul(Math.max(.02f,clearance));return new Vector3d(eye.x+anchor.x,eye.y+anchor.y,eye.z+anchor.z);
    }
}
