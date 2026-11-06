package dev.bkclient.ui.mainmenu;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import java.util.*;

/** Saved-server/world carousel with background favicon refresh. */
final class ServerCarousel {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private record Entry(String label, ServerInfo server, net.minecraft.world.level.storage.LevelSummary world) {}
    private final List<Entry> servers = new ArrayList<>();
    private boolean worlds, loading;
    private int generation;
    private String loadError;
    private final Map<Integer,Identifier> icons = new HashMap<>();
    private final Map<Integer,byte[]> iconBytes = new HashMap<>();
    private final ServerIconLoader iconLoader = new ServerIconLoader();
    void preloadServers(){iconLoader.start();}
    void tick(){iconLoader.tick();}
    private final List<Hit> hits = new ArrayList<>();
    private final Map<Integer,dev.bkclient.utility.animation.base.Animation> hoverAnimations = new HashMap<>();
    private boolean open;
    private int selected;
    private float position, alpha, velocity;
    private long frame;
    private float cx, cy, width;
    private record Hit(int index,float x,float y,float size,float angle) {
        boolean contains(double mx,double my){
            double dx=mx-x-size/2,dy=my-y-size/2,c=Math.cos(angle),s=Math.sin(angle);
            return Math.abs(dx*c+dy*s)<=size/2 && Math.abs(-dx*s+dy*c)<=size/2;
        }
    }
    boolean isOpen(){return open || alpha>.01f;}
    float visibility(){return alpha;}
    void open(){open(false);}
    void open(boolean worlds){
        close();clearTextures();this.worlds=worlds;servers.clear();loadError=null;loading=false;
        selected=0;position=0;velocity=0;alpha=0;frame=System.nanoTime();open=true;
        if(!worlds){
            iconLoader.start();
            var list=new ServerList(mc);list.loadFile();
            for(int i=0;i<list.size();i++){var server=list.get(i);servers.add(new Entry(server.address,server,null));}
            return;
        }
        loading=true;int request=++generation;
        // Vanilla's summary reader handles versions, locks and corrupt saves; no world is opened here.
        java.util.concurrent.CompletableFuture.supplyAsync(()->{
            try{return mc.getLevelStorage().getLevelList();}
            catch(Exception e){throw new java.util.concurrent.CompletionException(e);}
        }).thenCompose(list->mc.getLevelStorage().loadSummaries(list)).whenComplete((list,error)->mc.execute(()->{
            if(request!=generation||!open)return;
            loading=false;
            if(error!=null){loadError="Unable to read worlds";return;}
            list.stream().sorted().forEach(world->servers.add(new Entry(world.getDisplayName(),null,world)));
        }));
    }
    void close(){generation++;loading=false;open=false;hits.clear();}
    void dispose(){
        close();iconLoader.stop();clearTextures();
    }
    private void clearTextures(){
        alpha=0;hoverAnimations.clear();iconBytes.clear();for(var id:icons.values())if(id!=null)mc.getTextureManager().destroyTexture(id);icons.clear();
    }
    private Identifier icon(int index){
        var current=servers.get(index);
        byte[] fresh=current.server()!=null?iconLoader.favicon(current.server()):null;
        if(icons.containsKey(index) && (current.server()==null || Arrays.equals(iconBytes.get(index),fresh)))return icons.get(index);
        var previous=icons.remove(index);if(previous!=null)mc.getTextureManager().destroyTexture(previous);
        iconBytes.put(index,fresh);
        Identifier id=null;
        byte[] png=null;
        try{
            var entry=servers.get(index);
            if(entry.server()!=null)png=fresh;
            else {
                var original = entry.world().getIconPath();
                var preview = original.resolveSibling(WorldCarouselPreview.FILE_NAME);
                var path = java.nio.file.Files.isRegularFile(preview) && !java.nio.file.Files.isSymbolicLink(preview)
                    && java.nio.file.Files.size(preview) <= 4*1024*1024 ? preview : original;
                if (java.nio.file.Files.isRegularFile(path) && !java.nio.file.Files.isSymbolicLink(path)
                        && java.nio.file.Files.size(path) <= 4*1024*1024)
                    png = java.nio.file.Files.readAllBytes(path);
            }
            if(png==null){icons.put(index,null);return null;}

            var image=NativeImage.read(new java.io.ByteArrayInputStream(png));
            if(entry.server()!=null ? image.getWidth()!=64||image.getHeight()!=64
                    : image.getWidth()<1||image.getWidth()>2048||image.getHeight()<1||image.getHeight()>2048){image.close();throw new IllegalArgumentException("Invalid server icon");}
            id=Identifier.of("bkclient","server-carousel/"+index);
            mc.getTextureManager().registerTexture(id,new NativeImageBackedTexture(image));
        }catch(Exception ignored){id=null;}
        icons.put(index,id);return id;
    }
    void draw(UIContext ctx,int screenWidth,int screenHeight){
        if(!isOpen())return;
        long now=System.nanoTime();double dt=Math.min(.1,(now-frame)/1e9);frame=now;
        alpha+=((open?1:0)-alpha)*(float)(1-Math.exp(-dt*(open?10:15)));
        // Integrate a damped spring in small steps so fast wheel changes keep their momentum.
        int steps=Math.max(1,(int)Math.ceil(dt*240));float step=(float)dt/steps;
        for(int i=0;i<steps;i++){
            velocity+=((selected-position)*225-velocity*25)*step;
            position+=velocity*step;
        }
        cx=screenWidth/2f;cy=screenHeight*.79f;width=Math.min(580,screenWidth-24);
        // Floating artwork, with no card frames or modal panel covering the menu background.
        ctx.drawRoundedRect(0,0,screenWidth,screenHeight,BorderRadius.all(0),new ColorRGBA(0,0,0).withAlpha(25*alpha));
        hits.clear();
        if(servers.isEmpty()){
            ctx.drawCenteredText(Fonts.MEDIUM.getFont(10),loadError!=null?loadError:loading?"Loading worlds...":worlds?"No saved worlds":"Add servers in Multiplayer first",cx,cy-5,ColorRGBA.WHITE.mulAlpha(alpha));return;
        }
        int count=Math.min(3,servers.size()),center=Math.round(position);
        var order=new ArrayList<Integer>();
        for(int offset=-(count-1)/2;offset<=count/2;offset++)order.add(center+offset);
        order.sort(Comparator.comparingDouble((Integer i)->Math.abs(i-position)).reversed());
        float unit=Math.min(1,width/580),spacing=84*unit;
        for(int virtual:order){
            int index=Math.floorMod(virtual,servers.size());
            float offset=virtual-position,distance=Math.abs(offset);
            float scale=(float)Math.exp(-distance*.36),size=70*unit*scale*(.65f+.35f*alpha);
            float midX=cx+offset*spacing;
            float x=midX-size/2,y=cy-size/2+(1-alpha)*(30+distance*9);
            float opacity=alpha*(float)Math.exp(-distance*.55)*Math.clamp(4-distance,0,1);
            // Room between individual glass tiles, including rotated side cards.
            for(int layer=3;layer>0;layer--){
                float spread=layer*3*unit;
                ctx.drawRoundedRect(x-spread,y+size-6,size+spread*2,8+spread,BorderRadius.all(8),new ColorRGBA(0,0,0).withAlpha(11*opacity));
            }
            float angle=0;
            float padding=5*unit*scale;
            ctx.draw();ctx.getMatrices().push();
            ctx.getMatrices().peek().getPositionMatrix().translate(midX,y+size/2,0)
                .rotateZ(angle)
                .translate(-midX,-y-size/2,0);
            float tileX=x-padding,tileY=y-padding,tileSize=size+padding*2;
            boolean hovered=open && new Hit(virtual,tileX,tileY,tileSize,0).contains(ctx.getMouseX(),ctx.getMouseY());
            var hover=hoverAnimations.computeIfAbsent(index,key->new dev.bkclient.utility.animation.base.Animation(
                300L,0.0F,dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT));
            hover.update(hovered);
            if(hovered)dev.bkclient.utility.game.cursor.CursorUtility.set(dev.bkclient.utility.game.cursor.CursorType.HAND);
            CustomButton.drawMenuSurface(ctx,tileX,tileY,tileSize,tileSize,BorderRadius.all(11*unit*scale),opacity,hover.getValue()*opacity);
            var texture=icon(index);
            if(texture!=null)ctx.drawRoundedTexture(texture,x,y,size,size,BorderRadius.all(7*unit*scale),ColorRGBA.WHITE.mulAlpha(opacity));
            else ctx.drawCenteredText(Fonts.BOLD.getFont(26*scale*unit),servers.get(index).label().isBlank()?"?":servers.get(index).label().substring(0,1),midX,y+size/2-11*scale*unit,ColorRGBA.WHITE.mulAlpha(opacity));
            ctx.draw();ctx.getMatrices().pop();
            float nameAlpha=alpha*(float)Math.exp(-distance*5);
            if(nameAlpha>.01f){
                String name=servers.get(index).label();
                if(name.length()>28)name=name.substring(0,25)+"...";
                ctx.drawCenteredText(Fonts.MEDIUM.getFont(9*unit),name,midX,y-20*unit,ColorRGBA.WHITE.mulAlpha(nameAlpha));
            }
            hits.add(new Hit(virtual,x-padding,y-padding,size+padding*2,angle));
        }
    }
    void scroll(double amount){
        if(open&&amount!=0&&servers.size()>1)selected+=amount>0?-1:1;
    }
    void click(double x,double y,int button,Screen parent){
        if(!open)return;
        if(button==1){close();return;}
        if(button!=0)return;
        for(int i=hits.size()-1;i>=0;i--){var hit=hits.get(i);if(hit.contains(x,y)){
            if(hit.index()==selected && Math.abs(position-selected)<.12f)join(parent);else selected=hit.index();return;
        }}
        if(x<cx-width/2||x>cx+width/2||y<cy-85||y>cy+90)close();
    }
    boolean key(int key,Screen parent){
        if(key==256)close();else if(key==263)scroll(1);else if(key==262)scroll(-1);else if(key==257)join(parent);
        return true;
    }
    private void join(Screen parent){
        if(servers.isEmpty())return;
        var entry=servers.get(Math.floorMod(selected,servers.size()));
        if(entry.world()!=null){
            if(!entry.world().isSelectable()){
                close();mc.setScreen(new net.minecraft.client.gui.screen.world.SelectWorldScreen(parent));return;
            }
            close();mc.createIntegratedServerLoader().start(entry.world().getName(),()->mc.setScreen(parent));
        }else{
            var server=entry.server();close();
            ConnectScreen.connect(parent,mc,ServerAddress.parse(server.address),server,false,null);
        }
    }
}
