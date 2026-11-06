package dev.bkclient.compat.runtime;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import dev.bkclient.BKClientInit;
import dev.bkclient.combatengine.api.events.implement.EventUpdate;
import dev.bkclient.combatengine.bkengine.module.impl.combat.AttackAura;
import dev.bkclient.combatengine.bridge.AuraRuntime;
import dev.bkclient.systems.config.ConfigFile;
import dev.bkclient.systems.modules.modules.combat.Aura;
import dev.bkclient.systems.modules.modules.visuals.TargetESP;
import dev.bkclient.systems.setting.settings.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;
import static org.lwjgl.opengl.GL33C.*;

/** Separate local test mod: never included in the client artifact. */
public final class AuraTargetProbe implements ClientModInitializer {
    static int tick,idle,checks; static boolean done; static Aura aura; static TargetESP esp;
    static OtherClientPlayerEntity near,weak,aim; static Object scan,cubes;
    static final String SOURCE="modules.settings.target_esp.target_source", PREFIX="modules.settings.target_esp.source.";
    static final String[] STYLES={"souls","crystals","marker","circles","ring","ring2","ghosts","glassGhosts","cubes","fragScan"};
    public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(AuraTargetProbe::tick);}
    static Object field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static Object call(Object o,String name)throws Exception{var m=o.getClass().getDeclaredMethod(name);m.setAccessible(true);return m.invoke(o);}
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("TARGET_CHECK "+label);}
    static Path dir(MinecraftClient mc){return mc.runDirectory.toPath().resolve("target-proof");}
    static ModeSetting setting(dev.bkclient.systems.modules.Module module,String name){return (ModeSetting)module.getSettings().stream().filter(s->s.getName().equals(name)).findFirst().orElseThrow();}
    static void select(ModeSetting setting,String name){setting.getValues().stream().filter(v->v.getName().equals(name)).findFirst().orElseThrow().select();}
    static void priority(String name){select(setting(aura,"Target priority"),name);aura.syncSourceSettings();}
    static void source(String name){select(setting(esp,SOURCE),PREFIX+name);}
    static void step(){aura.syncSourceSettings();AttackAura.INSTANCE.onEvent(new EventUpdate());}
    static void acquire(String name){priority(name);AttackAura.target=null;step();}
    static void same(LivingEntity actual,LivingEntity expected,String label){check(actual==expected,label+" ["+(actual==null?"none":actual.getName().getString())+"]");}
    static LivingEntity espTarget()throws Exception{return (LivingEntity)call(esp,"getTarget");}
    static void move(OtherClientPlayerEntity p,double x,double z){p.setPosition(x,100,z);p.prevX=x;p.prevY=100;p.prevZ=z;}
    static OtherClientPlayerEntity player(MinecraftClient mc,int id,String name,double x,double z,float hp){
        var p=new OtherClientPlayerEntity(mc.world,new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()),name));
        p.setId(id);move(p,x,z);p.setHealth(hp);p.setYaw(180);p.prevYaw=180;p.bodyYaw=p.prevBodyYaw=p.headYaw=p.prevHeadYaw=180;
        p.setNoGravity(true);p.setOnGround(true);mc.world.addEntity(p);return p;
    }
    static void fixture(MinecraftClient mc){
        move(near,1.2,2);move(weak,-1.5,3);move(aim,0,5);near.setHealth(20);weak.setHealth(3);aim.setHealth(20);
        mc.player.setYaw(0);mc.player.setPitch(8);mc.player.setStackInHand(Hand.MAIN_HAND,ItemStack.EMPTY);mc.player.setStackInHand(Hand.OFF_HAND,ItemStack.EMPTY);
    }
    static LivingEntity first(MinecraftClient mc,LivingEntity... candidates){
        for(Entity e:mc.world.getEntities())for(var c:candidates)if(e==c)return c;
        throw new AssertionError("fixture not in world");
    }
    static void configChecks()throws Exception{
        ConfigFile config=new ConfigFile("aura-target-fixture");
        check(setting(aura,"Target priority").getValue().getName().equals("Distance"),"default Distance");
        check(setting(esp,SOURCE).getValue().getName().equals(PREFIX+"crosshair"),"default crosshair");
        JsonObject old=JsonParser.parseString(config.saveToString()).getAsJsonObject();
        for(var value:old.getAsJsonArray("modules")){
            var m=value.getAsJsonObject();
            if(m.get("name").getAsString().equals("Aura"))m.getAsJsonObject("settings").remove("Target priority");
            if(m.get("name").getAsString().equals("Target ESP"))m.getAsJsonObject("settings").remove(SOURCE);
        }
        config.loadFromJson(old);
        check(setting(aura,"Target priority").getValue().getName().equals("Distance"),"old profile without priority loads");
        check(setting(esp,SOURCE).getValue().getName().equals(PREFIX+"crosshair"),"old ESP profile retains crosshair default");
        for(String value:new String[]{"Distance","Health","Angle / FOV","None"})for(String src:new String[]{"crosshair","aura"}){
            priority(value);source(src);JsonObject saved=JsonParser.parseString(config.saveToString()).getAsJsonObject();
            priority(value.equals("Health")?"None":"Health");source(src.equals("aura")?"crosshair":"aura");
            config.loadFromJson(saved);aura.syncSourceSettings();
            check(AttackAura.INSTANCE.targetPriority.getCurrent().equals(value),"full config restores priority "+value+" / "+src);
            check(setting(esp,SOURCE).getValue().getName().equals(PREFIX+src),"full config restores source "+src);
        }
        priority("None");source("aura");check(config.save(),"write profile to disk");
        priority("Distance");source("crosshair");
        check(new ConfigFile("aura-target-fixture").load(),"read profile from disk");aura.syncSourceSettings();
        check(AttackAura.INSTANCE.targetPriority.is("None")&&setting(esp,SOURCE).getValue().getName().equals(PREFIX+"aura"),"priority and source survive disk save/load");
        priority("Distance");source("crosshair");
    }
    static void selectionChecks(MinecraftClient mc)throws Exception{
        fixture(mc);acquire("Distance");same(aura.getTarget(),near,"distance chooses nearest");
        priority("Health");step();same(aura.getTarget(),weak,"live Health change reacquires");
        near.setHealth(1);step();same(aura.getTarget(),weak,"holds valid target when another loses health");
        weak.setHealth(0);step();same(aura.getTarget(),near,"death reacquires by health");fixture(mc);
        weak.setAbsorptionAmount(20);acquire("Health");same(aura.getTarget(),weak,"Health uses HP independently of absorption");weak.setAbsorptionAmount(0);
        priority("Angle / FOV");step();same(aura.getTarget(),aim,"live Angle chooses look direction");
        mc.player.setYaw(90);step();same(aura.getTarget(),aim,"camera movement keeps valid target");mc.player.setYaw(0);
        priority("None");step();same(aura.getTarget(),first(mc,near,weak,aim),"None uses first valid world entity");
        near.setHealth(10);weak.setHealth(10);acquire("Health");same(aura.getTarget(),first(mc,near,weak),"equal health preserves world order");fixture(mc);
        acquire("Distance");move(near,200,0);step();same(aura.getTarget(),weak,"out-of-range target reacquires");fixture(mc);
        weak.setHealth(0);acquire("Health");check(aura.getTarget()!=weak,"dead entity excluded before sorting");fixture(mc);
        var removed=player(mc,-9504,"RemovedTarget",0,1,1);acquire("Distance");same(aura.getTarget(),removed,"temporary target acquired");
        mc.world.removeEntity(removed.getId(),Entity.RemovalReason.DISCARDED);step();same(aura.getTarget(),near,"removed target is released");
        move(near,200,1);move(weak,200,3);move(aim,200,5);step();same(aura.getTarget(),null,"no valid targets yields null");fixture(mc);acquire("Distance");
        source("aura");same(espTarget(),near,"Aura source follows combat target outside crosshair");
        ((BooleanSetting)field(esp,"players")).setEnabled(false);same(espTarget(),near,"Aura source follows Aura filters");
        check(!((BooleanSetting)field(esp,"players")).isVisible(),"crosshair filters hidden in Aura mode");
        ((BooleanSetting)field(esp,"players")).setEnabled(true);
        source("crosshair");same(espTarget(),aim,"crosshair selects independently of Aura");
        check(((BooleanSetting)field(esp,"players")).isVisible(),"crosshair filters restored");
        BKClientInit.getInstance().getTargetManager().setCurrentTarget(near);esp.tick();
        same(BKClientInit.getInstance().getTargetManager().getLivingTarget(),near,"ESP does not overwrite Aura shared target");
        source("aura");aura.setEnabled(false,true);same(espTarget(),null,"disabled Aura has no crosshair fallback");
        aura.setEnabled(true,true);acquire("Distance");
    }
    static void shot(MinecraftClient mc,String name)throws Exception{
        var f=mc.getFramebuffer();int w=f.textureWidth,h=f.textureHeight,old=glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        var data=org.lwjgl.BufferUtils.createByteBuffer(w*h*4);
        com.mojang.blaze3d.platform.GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER,f.fbo);
        glReadPixels(0,0,w,h,GL_RGBA,GL_UNSIGNED_BYTE,data);
        com.mojang.blaze3d.platform.GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER,old);
        var image=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=((h-y-1)*w+x)*4;image.setRGB(x,y,(data.get(i)&255)<<16|(data.get(i+1)&255)<<8|data.get(i+2)&255);}
        javax.imageio.ImageIO.write(image,"png",dir(mc).resolve(name+".png").toFile());check(glGetError()==0,"clean GL "+name);
    }
    static void tick(MinecraftClient mc){if(done)return;try{
        GLFW.glfwHideWindow(mc.getWindow().getHandle());mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.getOverlay()!=null){if(mc.getOverlay()==null&&mc.getServer()==null&&++idle==40)mc.createIntegratedServerLoader().start("TestWorld",()->{});return;}
        tick++;
        if(tick==1){
            Files.createDirectories(dir(mc));for(var m:BKClientInit.getInstance().getModuleManager().getModules())m.setEnabled(false,true);
            aura=BKClientInit.getInstance().getModuleManager().getModule(Aura.class);esp=BKClientInit.getInstance().getModuleManager().getModule(TargetESP.class);
            scan=field(esp,"scanRenderer");cubes=field(esp,"cubeRenderer");configChecks();
            ((BooleanSetting)aura.getSettings().stream().filter(s->s.getName().equals("Only weapon")).findFirst().orElseThrow()).setEnabled(true);
            ((SliderSetting)aura.getSettings().stream().filter(s->s.getName().equals("Extra detection range")).findFirst().orElseThrow()).setCurrentValue(4);
            mc.options.hudHidden=true;mc.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
            mc.getServer().execute(()->{for(String c:new String[]{"gamemode creative @a","weather clear","gamerule doDaylightCycle false","time set 6000","fill -12 99 -8 12 99 18 minecraft:smooth_quartz","fill -12 100 -8 12 108 18 air","tp @a 0 100 0 0 8"})mc.getServer().getCommandManager().executeWithPrefix(mc.getServer().getCommandSource().withSilent(),c);});
        }
        if(tick==25){near=player(mc,-9501,"NearestTarget",1.2,2,20);weak=player(mc,-9502,"LowestHP",-1.5,3,3);aim=player(mc,-9503,"AimTarget",0,5,20);}
        if(tick==35){aura.setEnabled(true,true);selectionChecks(mc);((ModeSetting.Value)field(esp,"fragScan")).select();esp.setEnabled(true,true);}
        if(tick==55){same((LivingEntity)field(scan,"retained"),near,"rendered scan follows Aura");shot(mc,"01-aura-source");source("crosshair");}
        if(tick==75){same((LivingEntity)field(scan,"retained"),aim,"rendered scan follows crosshair");shot(mc,"02-crosshair-source");source("aura");aura.setEnabled(false,true);}
        if(tick==90){same((LivingEntity)field(scan,"retained"),null,"switching source clears old scan with Aura disabled");check(field(scan,"mask")==null,"Aura off releases scan resources");shot(mc,"03-aura-off");}
        if(tick>=100&&tick<300){
            int i=(tick-100)/20,phase=(tick-100)%20;String style=STYLES[i];
            if(phase==0){aura.setEnabled(true,true);acquire("Distance");source("aura");((ModeSetting.Value)field(esp,style)).select();}
            if(phase==8){
                LivingEntity drawn=style.equals("fragScan")?(LivingEntity)field(scan,"retained"):style.equals("cubes")?(LivingEntity)field(cubes,"target"):(LivingEntity)field(esp,"prevTarget");
                same(drawn,near,"Aura source renders "+style);check(glGetError()==0,"clean GL "+style);aura.setEnabled(false,true);
            }
            if(phase==15){same(espTarget(),null,"no fallback "+style);check(field(esp,"prevTarget")==null&&field(scan,"retained")==null&&field(cubes,"target")==null,"no retained target after Aura off "+style);}
        }
        if(tick==300){checks+=ClientResourceChecks.run(mc);esp.onWorldChanged();check(field(esp,"prevTarget")==null&&field(scan,"mask")==null,"world cleanup");esp.setEnabled(false,true);Files.writeString(dir(mc).resolve("result.txt"),"PASS "+checks);done=true;mc.scheduleStop();}
    }catch(Throwable e){e.printStackTrace();try{Files.createDirectories(dir(mc));Files.writeString(dir(mc).resolve("result.txt"),"FAIL "+e);}catch(Exception ignored){}done=true;mc.scheduleStop();}}
}
