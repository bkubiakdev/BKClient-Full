package dev.bkclient.ui.menu.lyrics;

import dev.bkclient.framework.base.CustomScreen;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.visuals.Lyrics3D;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import dev.bkclient.ui.clicksys.ClickSysMaterial;
import dev.bkclient.ui.clicksys.port.gui.widget.*;
import dev.bkclient.ui.clicksys.port.render.ShapeShader;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.ScissorUtility;
import dev.bkclient.utility.render.lyrics.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.joml.Matrix4f;

/** Native settings beside an independent, continuously rendered Lyrics 3D sample. */
public final class LyricsEditorScreen extends CustomScreen {
    private static final String K="modules.settings.lyrics_3d.";
    private static final String[] SECTIONS={"appearance","movement","animations","history","synchronization"};
    private static final String[] PRESETS={"classic","neon","scattered"},PRESET_LABELS={"classic","neon","preset_scattered"};
    private static final float WIDTH=520,HEIGHT=308,LEFT=208,RIGHT=220,VIEW_W=288;
    private final Lyrics3D module;
    private final Screen parent;
    private final LyricsPreviewClock clock=new LyricsPreviewClock();
    private final LyricsPreviewScene scene=new LyricsPreviewScene();
    private SettingsInspector inspector;
    private dev.bkclient.ui.clicksys.port.module.Module adapter;
    private float scale=1,x,y,zoom=1,yaw,pitch;
    private boolean dragging;
    private long replayAfter;

    public LyricsEditorScreen(Lyrics3D module,Screen parent){this.module=module;this.parent=parent;}
    private static String tr(String key){return Localizator.translate(key);}
    public float widgetScale(){return scale/2;}
    @Override protected void init(){dragging=false;rebuildSettings();}
    private void rebuildSettings(){
        WidgetState.setWidget2(null);if(inspector!=null)inspector.hide();
        var o=module.options;
        var source=module.getSettings().stream().filter(s->!(s instanceof ButtonSetting)&&s!=o.section
            &&(s!=o.preview||LyricsOptions.is(o.section,"synchronization"))).toList();
        adapter=new dev.bkclient.ui.clicksys.port.module.Module(module,source);
        var group=new dev.bkclient.ui.clicksys.port.module.Module(K+LyricsOptions.id(o.section),dev.bkclient.ui.clicksys.port.module.Category.VISUAL);
        for(var setting:adapter.getSettings())if(setting!=adapter.getKeybindSetting()){
            Runnable apply=setting.getOnChange();
            setting.setOnChange(()->{if(apply!=null)apply.run();if(LyricsOptions.is(o.section,"animations"))replayAfter=System.nanoTime()+180_000_000;});
            group.addSetting(setting);
        }
        inspector=new EditorInspector();inspector.show(group);
    }
    private static final class EditorInspector extends SettingsInspector {
        EditorInspector(){super(()->{});utilityPage=true;setValue237((LEFT-12)*2);}
    }
    @Override public void tick(){if(adapter!=null)adapter.sync();}
    private void layout(){
        scale=Math.max(.1f,Math.min(1.15f,.94f*Math.min((width-20f)/WIDTH,(height-20f)/HEIGHT)));
        x=(width/scale-WIDTH)/2;y=(height/scale-HEIGHT)/2;
    }
    @Override public void render(UIContext context){
        layout();long now=System.nanoTime();clock.tick(now);
        if(replayAfter!=0&&now>=replayAfter){replayAfter=0;replay();}
        context.drawRect(0,0,width,height,new ColorRGBA(0,0,0,95));context.draw();
        UIContext c=context.withMouse((int)(context.getMouseX()/scale),(int)(context.getMouseY()/scale));
        var matrices=c.getMatrices();matrices.push();
        try(ClickGuiV2Theme theme=ClickGuiV2Theme.openClickSys()){
            matrices.scale(scale,scale,1);
            c.drawShadow(x,y,WIDTH,HEIGHT,12,BorderRadius.all(8),ColorRGBA.BLACK.withAlpha(45));
            c.drawRoundedRect(x,y,WIDTH,HEIGHT,BorderRadius.all(8),theme.window);
            c.drawText(Fonts.SEMIBOLD.getFont(11),"Lyrics 3D",x+12,y+12,theme.text);
            c.drawText(Fonts.REGULAR.getFont(7),tr("lyrics.editor.live"),x+78,y+15,theme.accent);
            var credit=Fonts.REGULAR.getFont(6);String creditText=tr("lyrics.editor.credit");
            c.drawText(credit,creditText,x+WIDTH-32-credit.width(creditText),y+15,theme.muted);
            c.draw();LucideIcons.CROSS.draw(c,x+WIDTH-23,y+10,11,theme.muted);
            for(int i=0;i<SECTIONS.length;i++){
                boolean selected=LyricsOptions.is(module.options.section,SECTIONS[i]);float bx=x+8+i*101;
                c.drawRoundedRect(bx,y+33,97,19,BorderRadius.all(4),selected?theme.selection:theme.field);
                c.drawCenteredText(Fonts.MEDIUM.getFont(7),tr(K+SECTIONS[i]),bx+48.5f,y+39,selected?theme.accent:theme.muted);
            }
            for(int i=0;i<PRESETS.length;i++)button(c,x+8+i*67,y+60,63,tr(K+PRESET_LABELS[i]),theme);
            c.drawText(Fonts.MEDIUM.getFont(8),tr("lyrics.editor.preview"),x+RIGHT,y+64,theme.text);
            drawPreview(c,theme);
            drawSettings(c);
            c.draw();
        }finally{matrices.pop();}
    }
    private void drawPreview(UIContext c,ClickGuiV2Theme theme){
        float px=x+RIGHT,top=y+82,h=170;
        c.drawRoundedRect(px,top,VIEW_W,h,BorderRadius.all(6),theme.surface);
        for(int i=1;i<8;i++)c.drawRect(px+i*VIEW_W/8,top,.4f,h,theme.border.mulAlpha(.28f));
        for(int i=1;i<5;i++)c.drawRect(px,top+i*h/5,VIEW_W,.4f,theme.border.mulAlpha(.28f));
        c.draw();
        ScissorUtility.push(c.getMatrices(),px+1,top+1,VIEW_W-2,h-2);
        try{
            var o=module.options;var lines=scene.frame(clock.positionMs(),o);
            // A scattered verse is a wide cloud, not one wrapped line; fit the cloud into the preview.
            float span=o.scattered()?260*o.scatterSpread.getCurrentValue()/100+60:o.wrap.getCurrentValue();
            float fit=Math.min(1.25f,(VIEW_W-30)/span)*o.size.getCurrentValue()/100*zoom;
            Matrix4f matrix=new Matrix4f(c.getMatrices().peek().getPositionMatrix()).translate(px+VIEW_W/2,top+h*.59f,100)
                .rotateX(pitch).rotateY(yaw).scale(fit);
            scene.renderer.renderPreview(matrix,lines,o,clock.positionMs()/1000.);
        }finally{ScissorUtility.pop();}
        button(c,px,y+260,81,tr(clock.playing()?"lyrics.editor.pause":"lyrics.editor.play"),theme);
        button(c,px+86,y+260,105,tr("lyrics.editor.replay"),theme);
        button(c,px+196,y+260,92,tr("lyrics.editor.reset_view"),theme);
        float progress=(clock.positionMs()%LyricsPreviewScene.VERSE_MS)/(float)LyricsPreviewScene.VERSE_MS;
        c.draw();
        Matrix4f trackMatrix=c.getMatrices().peek().getPositionMatrix();
        float trackHeight=3,filled=VIEW_W*progress;
        ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(trackHeight/2,px,theme.field.getRGB(),trackMatrix,trackHeight,VIEW_W,1,y+281.5f);
        if(filled>0)ShapeShader.onFloatFloatIntMatrix4fFloatFloatFloatFloat(Math.min(trackHeight/2,filled/2),px,theme.accent.getRGB(),trackMatrix,trackHeight,filled,1,y+281.5f);
        boolean worldTab=LyricsOptions.is(module.options.section,"movement")||LyricsOptions.is(module.options.section,"synchronization");
        c.drawCenteredText(Fonts.REGULAR.getFont(6),tr(worldTab?"lyrics.editor.world_hint":"lyrics.editor.drag"),px+VIEW_W/2,y+292,theme.muted);
        c.draw();
    }
    private void drawSettings(UIContext c){
        ClickSysMaterial.begin(c);UiContext.update();dev.bkclient.ui.clicksys.port.module.client.ThemeModule.setFlag2();
        Matrix4f matrix=new Matrix4f(c.getMatrices().peek().getPositionMatrix()).scale(.5f,.5f,1);
        try{
            inspector.position((x+6)*2,(y+82)*2,(HEIGHT-89)*2);
            inspector.onFloatFloatFloatMatrix4f(1,c.getMouseY()*2,c.getMouseX()*2,matrix);
            WidgetState.onMatrix4fFloatFloatFloat(matrix,c.getMouseX()*2,c.getMouseY()*2,1);
            HeaderPainter.onMatrix4fFloat(matrix,1);
        }finally{ScissorStack.update2();dev.bkclient.ui.clicksys.port.module.client.ThemeModule.setFlag();ClickSysMaterial.end();}
    }
    private void button(UIContext c,float bx,float by,float w,String title,ClickGuiV2Theme theme){
        c.drawRoundedRect(bx,by,w,17,BorderRadius.all(4),theme.field);
        c.drawCenteredText(Fonts.MEDIUM.getFont(6.5f),title,bx+w/2,by+5,theme.text);
    }
    private boolean inside(double mx,double my,float bx,float by,float w,float h){return mx>=bx&&mx<=bx+w&&my>=by&&my<=by+h;}
    private void replay(){clock.replay(System.nanoTime());scene.reset();}
    @Override public void onMouseClicked(double mx,double my,MouseButton button){
        mx/=scale;my/=scale;
        if(WidgetState.isWidgetAsBoolean()){WidgetState.isIntDoubleDouble(button.getButtonIndex(),mx*2,my*2);return;}
        if(inspector.isIntDoubleDouble(button.getButtonIndex(),mx*2,my*2))return;
        if(button!=MouseButton.LEFT)return;
        if(inside(mx,my,x+WIDTH-28,y+7,23,22)){close();return;}
        for(int i=0;i<SECTIONS.length;i++)if(inside(mx,my,x+8+i*101,y+33,97,19)){
            LyricsOptions.select(module.options.section,SECTIONS[i]);rebuildSettings();AbstractSetting.saveConfig();return;
        }
        for(int i=0;i<PRESETS.length;i++)if(inside(mx,my,x+8+i*67,y+60,63,17)){
            module.options.preset(PRESETS[i]);rebuildSettings();adapter.sync();replay();AbstractSetting.saveConfig();return;
        }
        if(inside(mx,my,x+RIGHT,y+260,81,17)){clock.toggle(System.nanoTime());return;}
        if(inside(mx,my,x+RIGHT+86,y+260,105,17)){replay();return;}
        if(inside(mx,my,x+RIGHT+196,y+260,92,17)){yaw=pitch=0;zoom=1;return;}
        dragging=inside(mx,my,x+RIGHT,y+82,VIEW_W,170);
    }
    @Override public void onMouseDragged(double mx,double my,MouseButton button,double dx,double dy){
        if(WidgetState.isDoubleIntDoubleDoubleDouble(my/widgetScale(),button.getButtonIndex(),dx/widgetScale(),dy/widgetScale(),mx/widgetScale()))return;
        if(inspector.isDoubleDoubleIntDoubleDouble(my/widgetScale(),mx/widgetScale(),button.getButtonIndex(),dx/widgetScale(),dy/widgetScale()))return;
        if(dragging&&button==MouseButton.LEFT){yaw=Math.clamp(yaw+(float)dx/scale*.008f,-1.2f,1.2f);pitch=Math.clamp(pitch-(float)dy/scale*.008f,-.8f,.8f);}
    }
    @Override public void onMouseReleased(double mx,double my,MouseButton button){
        dragging=false;WidgetState.isDoubleDoubleInt(mx/widgetScale(),my/widgetScale(),button.getButtonIndex());
        inspector.isDoubleDoubleInt(mx/widgetScale(),my/widgetScale(),button.getButtonIndex());
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        if(WidgetState.isDoubleDoubleDouble(mx/widgetScale(),my/widgetScale(),vertical)||inspector.isDoubleDoubleDouble(vertical,mx/widgetScale(),my/widgetScale()))return true;
        if(inside(mx/scale,my/scale,x+RIGHT,y+82,VIEW_W,170))zoom=Math.clamp(zoom+(float)vertical*.1f,.5f,2);
        return true;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(WidgetState.isWidgetAsBoolean()){if(key==256)WidgetState.setWidget2(null);else WidgetState.isIntIntInt2(modifiers,key,scan);return true;}
        return inspector.isIntIntInt2(scan,modifiers,key)||super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean keyReleased(int key,int scan,int modifiers){return WidgetState.isIntIntInt(key,modifiers,scan)||inspector.isIntIntInt(key,modifiers,scan)||super.keyReleased(key,scan,modifiers);}
    @Override public boolean charTyped(char chr,int modifiers){return WidgetState.isCharInt(chr,modifiers)||inspector.isIntChar(modifiers,chr)||super.charTyped(chr,modifiers);}
    @Override public void close(){if(TextField.LAST_FIELD!=null)TextField.LAST_FIELD.setFocused(false);AbstractSetting.saveConfig();client.setScreen(parent);}
    @Override public void removed(){scene.close();WidgetState.setWidget2(null);if(inspector!=null)inspector.hide();dev.bkclient.ui.clicksys.port.render.TextShader.clearBatches();super.removed();}
    @Override public boolean shouldPause(){return false;}
    @Override public void renderBackground(DrawContext context,int mx,int my,float delta){}
}
