package dev.bkclient.compat.runtime;

import com.google.gson.JsonObject;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.ItemPhysics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.*;

/** Offline integration checks, launched only through the separate probe mod. */
public final class ClientResourceChecks {
    private static int checks;
    private static final boolean BASELINE=Boolean.getBoolean("bk.audit.baseline");
    private static final JsonObject results=new JsonObject();
    static Object field(Class<?> type,Object instance,String name)throws Exception{
        var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(instance);
    }
    static Method method(Class<?> type,String name,Class<?>... args)throws Exception{
        var m=type.getDeclaredMethod(name,args);m.setAccessible(true);return m;
    }
    static void check(boolean ok,String label){
        System.out.println("RESOURCE_CHECK "+label+": "+ok);
        if(!ok&&!BASELINE)throw new AssertionError(label);checks++;
    }
    public static int run(MinecraftClient mc)throws Exception{
        results.addProperty("baseline",BASELINE);
        try { menuQueue(mc); itemPhysics(mc); worldBuffers(mc); skinRequests(mc); }
        finally { Files.writeString(mc.runDirectory.toPath().resolve("target-proof/resources.json"),results.toString()); }
        return checks;
    }
    private static void menuQueue(MinecraftClient mc)throws Exception{
        var type=Class.forName("dev.bkclient.ui.mainmenu.ServerIconLoader");
        var constructor=type.getDeclaredConstructor();constructor.setAccessible(true);
        var pool=(ThreadPoolExecutor)field(type,null,"WORKERS");
        var ready=new CountDownLatch(5);var release=new CountDownLatch(1);
        // Occupy all DNS workers without making a single network request.
        for(int i=0;i<5;i++)pool.execute(()->{ready.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
        if(!ready.await(3,TimeUnit.SECONDS))throw new AssertionError("workers did not start");
        var servers=new ServerList(mc);
        for(int i=0;i<5;i++)servers.add(new ServerInfo("Audit "+i,"127.0.0.1:"+(45000+i),ServerInfo.ServerType.OTHER),false);
        servers.saveFile();
        var start=method(type,"start");var tick=method(type,"tick");var stop=method(type,"stop");
        try{
            for(int i=0;i<100;i++){
                Object loader=constructor.newInstance();
                try{start.invoke(loader);tick.invoke(loader);}finally{stop.invoke(loader);}
            }
            int queued=pool.getQueue().size();results.addProperty("queuedAfter100MenuCloseCycles",queued);
            check(queued==0,"closing menu cancels and removes pending icon requests (queued="+queued+")");
            check(pool.getQueue().remainingCapacity()<1000,"icon worker queue has a hard capacity");
        }finally{
            release.countDown();
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            while(!pool.getQueue().isEmpty()&&System.nanoTime()<deadline)Thread.sleep(10);
        }
    }
    private static ItemEntity item(MinecraftClient mc){
        var item=new ItemEntity(mc.world,0,100,0,new ItemStack(Items.STONE));item.setVelocity(Vec3d.ZERO);return item;
    }
    private static void itemPhysics(MinecraftClient mc)throws Exception{
        var type=ItemEntityRenderer.class;
        var hold=(Map<?,?>)field(type,null,"bk$groundHold");
        var states=(Map<?,?>)field(type,null,"bk$groundState");
        var resolve=method(type,"bk$resolveGroundState",ItemEntity.class,boolean.class);
        var module=BKClientInit.getInstance().getModuleManager().getModule(ItemPhysics.class);
        module.setEnabled(true,true);
        for(int fps:new int[]{20,60,144}){
            hold.clear();var e=item(mc);e.age=100;resolve.invoke(null,e,true);
            int mismatch=0;
            for(int frame=0;frame<fps;frame++){
                int ticks=frame*20/fps;e.age=100+ticks;
                boolean grounded=(boolean)resolve.invoke(null,e,false);
                if(grounded!=(ticks<8))mismatch++;
            }
            results.addProperty("groundHoldMismatchesAt"+fps+"Fps",mismatch);
            check(mismatch==0,"ground hold lasts eight ticks at "+fps+" FPS");
        }
        hold.clear();var first=item(mc);first.age=100;resolve.invoke(null,first,true);
        var replacement=item(mc);replacement.setId(first.getId());replacement.age=0;
        boolean inherited=(boolean)resolve.invoke(null,replacement,false);
        results.addProperty("newEntityInheritsReusedIdState",inherited);
        check(!inherited,"new entity never inherits an old entity id's ground state");
        hold.clear();states.clear();module.setEnabled(false,true);
        for(int i=0;i<1000;i++){
            var e=item(mc);e.setOnGround(true);
            var renderer=(ItemEntityRenderer)mc.getEntityRenderDispatcher().getRenderer(e);
            renderer.updateRenderState(e,renderer.createRenderState(),0);
        }
        results.addProperty("groundCacheEntriesWhileDisabled",hold.size());
        check(hold.isEmpty()&&states.isEmpty(),"disabled Item Physics skips ground tracking");
        hold.clear();states.clear();
    }
    private static void worldBuffers(MinecraftClient mc)throws Exception{
        Object[] posts={dev.bkclient.utility.render.DrawUtility.targetLensPost,dev.bkclient.utility.render.DrawUtility.particleLightPost};
        var buffers=new java.util.ArrayList<net.minecraft.client.gl.Framebuffer>();
        for(Object post:posts){
            method(post.getClass(),"ensureFramebuffers",int.class,int.class).invoke(post,1920,1080);
            for(String name:new String[]{"copyFbo","outFbo"})buffers.add((net.minecraft.client.gl.Framebuffer)field(post.getClass(),post,name));
        }
        mc.getFramebuffer().beginWrite(true);
        int[] textures=buffers.stream().mapToInt(net.minecraft.client.gl.Framebuffer::getColorAttachment).toArray();
        int[] fbos=buffers.stream().mapToInt(f->f.fbo).toArray();
        BKClientInit.getInstance().releaseWorldResources();
        int retained=0;for(int id:textures)if(org.lwjgl.opengl.GL11.glIsTexture(id))retained++;
        results.addProperty("postTexturesRetainedAfterWorldCleanup",retained);
        results.addProperty("postColorBytesAt1080p",1920*1080*4*4);
        check(retained==0,"world cleanup releases four postprocessing textures");
        check(java.util.Arrays.stream(fbos).noneMatch(org.lwjgl.opengl.GL30::glIsFramebuffer),"world cleanup deletes post framebuffer objects");
        check(org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING)==mc.getFramebuffer().fbo,"world cleanup preserves main draw framebuffer");
        if(BASELINE)for(Object post:posts)method(post.getClass(),"shutdown").invoke(post);
        check(org.lwjgl.opengl.GL11.glGetError()==0,"world cleanup has no GL errors");
        if(!BASELINE){
            for(int cycle=0;cycle<40;cycle++){
                for(Object post:posts){
                    Object shader=field(post.getClass(),post,"shader");
                    int program=(int)field(shader.getClass(),shader,"programId");
                    method(post.getClass(),"ensureFramebuffers",int.class,int.class).invoke(post,320+cycle*8,180+cycle*4);
                    var copy=(net.minecraft.client.gl.Framebuffer)field(post.getClass(),post,"copyFbo");
                    var out=(net.minecraft.client.gl.Framebuffer)field(post.getClass(),post,"outFbo");
                    int copyId=copy.getColorAttachment(),outId=out.getColorAttachment();
                    mc.getFramebuffer().beginWrite(true);method(post.getClass(),"releaseTargets").invoke(post);
                    check(!org.lwjgl.opengl.GL11.glIsTexture(copyId)&&!org.lwjgl.opengl.GL11.glIsTexture(outId),"resize/release cycle "+cycle+" "+post.getClass().getSimpleName());
                    check(org.lwjgl.opengl.GL20.glIsProgram(program),"shader remains reusable "+cycle+" "+post.getClass().getSimpleName());
                }
            }
            results.addProperty("postResizeReleaseCycles",40);
            check(org.lwjgl.opengl.GL11.glGetError()==0,"all repeated allocation/release cycles have clean GL");
        }
    }
    private static void skinRequests(MinecraftClient mc)throws Exception{
        var skin=BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.SkinChanger.class);
        var pool=(ThreadPoolExecutor)field(dev.bkclient.utility.concurrent.ClientAsyncExecutor.class,null,"EXECUTOR");
        int oldCore=pool.getCorePoolSize();pool.setCorePoolSize(2);
        var ready=new CountDownLatch(2);var release=new CountDownLatch(1);
        try{
            for(int i=0;i<2;i++)pool.execute(()->{ready.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            if(!ready.await(3,TimeUnit.SECONDS))throw new AssertionError("client workers did not start");
            while(pool.getQueue().remainingCapacity()>0)pool.execute(()->{});
            skin.setEnabled(true,true);skin.nick.text("AuditFixture");method(skin.getClass(),"fetchAndApply").invoke(skin);
            boolean loading=(boolean)field(skin.getClass(),skin,"loading");
            results.addProperty("skinLoadingStuckAfterQueueRejection",loading);
            check(!loading,"rejected skin request clears loading without requiring a toggle");
        }finally{skin.setEnabled(false,true);release.countDown();pool.setCorePoolSize(oldCore);}
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        byte[] large=new byte[4*1024*1024+1];
        server.createContext("/small",exchange->{try(exchange){exchange.sendResponseHeaders(200,128);exchange.getResponseBody().write(new byte[128]);}});
        server.createContext("/large",exchange->{try(exchange){exchange.sendResponseHeaders(200,large.length);exchange.getResponseBody().write(large);}});
        server.createContext("/chunked",exchange->{try(exchange){exchange.sendResponseHeaders(200,0);exchange.getResponseBody().write(large);}});
        server.start();
        try{
            var download=method(skin.getClass(),"downloadBytes",String.class);String url="http://127.0.0.1:"+server.getAddress().getPort();
            check(((byte[])download.invoke(skin,url+"/small")).length==128,"small skin response is accepted");
            boolean bounded=download.invoke(skin,url+"/large")==null;
            results.addProperty("oversizedFixedSkinResponseRejected",bounded);check(bounded,"oversized fixed-length skin response is bounded");
            bounded=download.invoke(skin,url+"/chunked")==null;
            results.addProperty("oversizedChunkedSkinResponseRejected",bounded);check(bounded,"oversized chunked skin response is bounded");
        }finally{server.stop(0);}
    }
}
