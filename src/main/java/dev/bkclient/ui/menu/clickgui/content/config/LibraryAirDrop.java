package dev.bkclient.ui.menu.clickgui.content.config;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.config.ConfigFile;
import dev.bkclient.systems.supabase.SupabaseService;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;

/** Private one-shot transfers, managed entirely within the config library. */
public final class LibraryAirDrop implements IMinecraft {
    private ConfigFile config;
    private boolean open, sending, checking, receive = true;
    private final TextField recipient = new TextField(Fonts.MEDIUM.getFont(6.5F));
    private float x, y, w, h;
    private String status = "Enter the recipient's code", checkedCode = "";
    private long revision, verifiedUntil, refreshAt, copiedUntil;
    public boolean isOpen() { return dev.bkclient.systems.DeferredFeatures.AIRDROP && open; }
    public void open(ConfigFile config) {
        if (!dev.bkclient.systems.DeferredFeatures.AIRDROP || sending) return;
        this.config = config; open = true; receive = false;
        recipient.setPreview("12-character receive code"); recipient.setFocused(true);
    }
    public void openReceive() { if (!dev.bkclient.systems.DeferredFeatures.AIRDROP) return; open = true; receive = true; recipient.setFocused(false); }
    public void close() { open = false; recipient.setFocused(false); invalidate(); }
    private void invalidate() {
        revision++; checking = false; checkedCode = ""; verifiedUntil = refreshAt = 0;
        status = "Enter the recipient's code";
    }
    private String code() { return SupabaseService.normalizeCode(recipient.getBuiltText()); }
    private boolean verified() { return !checkedCode.isEmpty() && checkedCode.equals(code()) && System.nanoTime() < verifiedUntil; }
    private boolean ready() { return open && !receive && config != null && !sending && !checking && verified(); }
    public void render(UIContext c, float x, float y, float w, float h, float alpha) {
        this.x=x; this.y=y; this.w=w; this.h=h;
        if (!checkedCode.isEmpty() && !checkedCode.equals(code())) invalidate();
        if (!receive && verified() && !checking && !sending && System.nanoTime() >= refreshAt) check();
        c.drawClientRect(x,y,w,h,alpha,0,9);
        text(c,"AirDrop",13,8,true);
        c.drawTextureSmooth(BKClientInit.id("icons/close.png"),x+w-20,y+12,8,8,Colors.getTextColor());
        for (int i=0;i<2;i++) {
            float tx=x+8+i*(w-16)/2;
            c.drawRoundedRect(tx,y+26,(w-20)/2,19,BorderRadius.all(5),Colors.getAccent().withAlpha(receive==(i==0)?85:20));
            c.drawCenteredText(Fonts.MEDIUM.getFont(6.5F),i==0?"Receive":"Send",tx+(w-20)/4,y+32,Colors.getTextColor());
        }
        var service=SupabaseService.getInstance();
        if (receive) {
            if (service.hasPendingTransfer()) {
                text(c,"Config received",53,7,true);
                text(c,service.getPendingConfigName(),73,6.5F,false);
                text(c,"Accept to apply and save it.",94,6,false);
                button(c,y+109,"Reject config",true);
                button(c,y+h-28,"Accept config",true);
            } else {
                text(c,service.isActive()?"Waiting for sender":"Receiving off",53,7,true);
                text(c,service.getReceiveCode().isEmpty()?"---":service.getReceiveCode(),72,9,true);
                text(c,service.isActive()?service.remainingSeconds()+"s left - one config":"Code lasts 5 minutes",91,6,false);
                button(c,y+106,System.nanoTime()<copiedUntil?"Copied":"Copy code",!service.getReceiveCode().isEmpty());
                text(c,service.getStatus(),131,5.5F,false);
                button(c,y+h-28,service.isActive()?"Stop receiving":"Create receive code",true);
            }
        } else {
            c.drawRoundedRect(x+8,y+50,w-16,21,BorderRadius.all(6),Colors.getTextColor().withAlpha(12));
            recipient.set(x+13,y+54,w-26,13);recipient.setTextColor(Colors.getTextColor());recipient.render(c);
            button(c,y+76,checking?"Connecting...":verified()?"Reconnect":"Connect",!checking&&!sending&&SupabaseService.validCode(code()));
            text(c,verified()?"Connected - ready to send":checkedCode.isEmpty()?status:"Disconnected - code expired",102,6,false);
            button(c,y+118,config==null?"< Select config >":"< Config: "+config.getDisplayName()+" >",!sending);
            button(c,y+h-28,sending?"Sending...":"Send selected config",ready());
        }
    }
    private void text(UIContext c,String label,float dy,float size,boolean accent) {
        c.drawFadeoutText(Fonts.MEDIUM.getFont(size),label,x+10,y+dy,accent?Colors.getAccent():Colors.getTextColor(),0.8F,1,w-20);
    }
    private void button(UIContext c,float by,String label,boolean enabled) {
        c.drawRoundedRect(x+8,by,w-16,20,BorderRadius.all(6),Colors.getAccent().withAlpha(enabled?90:25));
        c.drawFadeoutText(Fonts.MEDIUM.getFont(6.5F),label,x+13,by+6,Colors.getTextColor().withAlpha(enabled?255:100),0.8F,1,w-26);
    }
    private boolean hit(double mx,double my,float by) { return GuiUtility.isHovered(x+8,by,w-16,20,mx,my); }
    public boolean click(double mx,double my,MouseButton b) {
        if (!open) return false;
        if (!GuiUtility.isHovered(x,y,w,h,mx,my)) { recipient.setFocused(false); return false; }
        if (b!=MouseButton.LEFT) return true;
        if (GuiUtility.isHovered(x+w-26,y+5,23,25,mx,my)) {close();return true;}
        if (sending) return true;
        if (GuiUtility.isHovered(x+8,y+26,w-16,19,mx,my)) {
            receive=mx<x+w/2;recipient.setFocused(!receive);
            if (!receive) recipient.setPreview("12-character receive code");
            return true;
        }
        var service=SupabaseService.getInstance();
        if (receive) {
            if (service.hasPendingTransfer()) {
                if(hit(mx,my,y+109))service.rejectPendingTransfer();
                else if(hit(mx,my,y+h-28))service.acceptPendingTransfer();
            } else {
                if(hit(mx,my,y+106)&&!service.getReceiveCode().isEmpty()){service.copyReceiveCode();copiedUntil=System.nanoTime()+2_000_000_000L;}
                if(hit(mx,my,y+h-28)){if(service.isActive())service.stopPolling();else service.startPolling();}
            }
        } else {
            recipient.onMouseClicked(mx,my,b);
            if(hit(mx,my,y+76))check();
            else if(hit(mx,my,y+118))selectConfig(mx<x+w/2?-1:1);
            else if(hit(mx,my,y+h-28))send();
        }
        return true;
    }
    private void selectConfig(int direction) {
        var manager=BKClientInit.getInstance().getConfigManager();manager.refresh();
        var configs=manager.getConfigFiles().stream().filter(f->f.getFile().isFile()).toList();
        if(configs.isEmpty()){config=null;status="Save a config first";return;}
        int index=config==null?-1:java.util.stream.IntStream.range(0,configs.size()).filter(i->configs.get(i).getFileName().equals(config.getFileName())).findFirst().orElse(-1);
        config=configs.get(index<0?0:Math.floorMod(index+direction,configs.size()));
    }
    private void check() {
        if(checking||sending||!SupabaseService.validCode(code()))return;
        long current=++revision;String requested=code();checking=true;status="Checking receiver...";
        SupabaseService.getInstance().checkReceiveCode(requested,result->{
            if(current!=revision)return;
            checking=false;
            if(!requested.equals(code())){invalidate();return;}
            status=result.message();checkedCode=result.ready()?requested:"";
            verifiedUntil=result.ready()?System.nanoTime()+result.ttlSeconds()*1_000_000_000L:0;
            refreshAt=System.nanoTime()+10_000_000_000L;
        });
    }
    private void send() {
        if(!ready())return;long current=revision;
        try {
            if(!config.getFile().isFile()){status="Config no longer exists";checkedCode="";return;}
            if(java.nio.file.Files.size(config.getFile().toPath())>262144){status="Config too large (max 256 KiB)";checkedCode="";return;}
            String data=java.nio.file.Files.readString(config.getFile().toPath());sending=true;status="Sending...";
            SupabaseService.getInstance().sendAirdrop(code(),config.getFileName(),data,ok->{
                sending=false;if(current!=revision)return;
                checkedCode="";verifiedUntil=0;
                status=ok?"Sent - code used":"Send failed - check code again";
            });
        }catch(Exception e){sending=false;checkedCode="";status="Could not read config";}
    }
    public boolean key(int key,int scan,int mods) {
        if(!open)return false;if(key==256){close();return true;}
        if(receive||!recipient.isFocused())return false;
        if(!sending){if(key==257)check();else{recipient.onKeyPressed(key,scan,mods);if(!checkedCode.equals(code()))invalidate();}}
        return true;
    }
    public boolean character(char chr,int mods) {
        if(!open||receive||!recipient.isFocused())return false;
        if(sending)return true;boolean handled=recipient.charTyped(chr,mods);if(handled)invalidate();return handled;
    }
    public void release(){recipient.onMouseReleased(0,0,MouseButton.LEFT);}
    public boolean scroll(double mx,double my,double amount){return open&&GuiUtility.isHovered(x,y,w,h,mx,my);}
}
