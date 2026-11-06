package dev.bkclient.larp;

import dev.bkclient.framework.base.CustomScreen;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.gui.ModOption;
import dev.bkclient.larp.gui.NumberSetting;
import dev.bkclient.larp.gui.Status;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.ui.menu.csgui.widget.CsSlider;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.render.ScissorUtility;
import net.minecraft.client.gui.screen.Screen;
import java.util.ArrayList;
import java.util.List;

/** A standalone editor, using the same scoped flat palette as SwingAnimation. */
public final class LarpScreen extends CustomScreen {
    private static final float W = 630, H = 320;
    private final Screen parent;
    private final TextField search = new TextField(Fonts.MEDIUM.getFont(8));
    private final ScrollHandler listScroll = new ScrollHandler(), loreScroll = new ScrollHandler();
    private final List<Hit> hits = new ArrayList<>();
    private final List<CsSlider> sliders = new ArrayList<>();
    private final java.util.Map<String,dev.bkclient.ui.clicksys.ClickSysActionButton> buttons = new java.util.HashMap<>();
    private ModOption selected;
    private float scale = 1, x, y;
    private String query = "";
    private record Hit(float x, float y, float w, float h, Runnable action) {
        boolean contains(double mx, double my) { return mx >= x && mx < x+w && my >= y && my < y+h; }
    }

    public LarpScreen(Screen parent) { this.parent = parent; select(LarpCatalog.entries().getFirst()); }
    private void select(ModOption entry) {
        selected = entry; sliders.clear(); loreScroll.reset();
        for (var setting : entry.settings) if (setting instanceof NumberSetting number) {
            String label = number.id.equals("zabojstwa") ? "Kills" : "Extra damage (%)";
            sliders.add(new CsSlider(label, number.minimum(), number.maximum(), number.value, value -> Long.toString(Math.round(value))));
        }
    }
    private static boolean hover(double mx,double my,float x,float y,float w,float h) { return mx>=x && mx<x+w && my>=y && my<y+h; }
    private void panel(UIContext c, ClickGuiV2Theme t, float px,float py,float w,float h) {
        c.drawRoundedRect(px,py,w,h,BorderRadius.all(7),t.surface);
        c.drawRoundedBorder(px,py,w,h,.5f,BorderRadius.all(7),t.border);
    }
    private void text(UIContext c,String text,float px,float py,float size,ColorRGBA color) { c.drawText(Fonts.MEDIUM.getFont(size),text,px,py,color); }
    private void scrollbar(UIContext c, ClickGuiV2Theme t, ScrollHandler scroll, float px, float py, float height, float content) {
        if (content <= height) return;
        float thumb = Math.max(16, height * height / content);
        float offset = (float)Math.min(1, Math.max(0, scroll.getValue() / (content - height)));
        c.drawRoundedRect(px, py + offset * (height - thumb), 2, thumb, BorderRadius.all(1), t.muted);
    }
    // ClickSys actions use 32 units in its double-sized coordinate system.
    private static final float BUTTON_HEIGHT = 16;
    private void button(UIContext c,ClickGuiV2Theme t,String label,float px,float py,float w,Runnable action,boolean primary,boolean enabled) {
        actionButton(c,t,label,px,py,w,action,primary,enabled);
    }
    private void actionButton(UIContext c,ClickGuiV2Theme t,String label,float px,float py,float w,Runnable action,boolean primary,boolean enabled) {
        buttons.computeIfAbsent(label,dev.bkclient.ui.clicksys.ClickSysActionButton::new).render(c,px,py,w,1,enabled);
        if(enabled) hits.add(new Hit(px,py,w,BUTTON_HEIGHT,action));
    }
    @Override public void render(UIContext context) {
        scale=Math.max(.01f,Math.min(1,Math.min((width-24f)/W,(height-24f)/H)));
        x=(width/scale-W)/2; y=(height/scale-H)/2;
        var c=context.withMouse((int)(context.getMouseX()/scale),(int)(context.getMouseY()/scale));
        hits.clear();
        c.draw();
        c.getMatrices().push(); c.getMatrices().scale(scale,scale,1);
        try(var t=ClickGuiV2Theme.openClickSys()) {
            c.drawRoundedRect(x,y,W,H,BorderRadius.all(9),t.window);
            c.drawRoundedBorder(x,y,W,H,.5f,BorderRadius.all(9),t.border);
            text(c,"Larp Ana",x+15,y+12,15,t.text);
            text(c,"Local event item studio",x+15,y+32,8,t.muted);
            button(c,t,"Back",x+W-61,y+13,46,this::close,false,true);
            float top=y+55, bottom=y+H-31, ph=bottom-top;
            panel(c,t,x+10,top,166,ph); panel(c,t,x+184,top,244,ph); panel(c,t,x+436,top,184,ph);
            text(c,"ITEMS",x+21,top+12,8,t.muted);
            text(c,Integer.toString(LarpCatalog.entries().size()),x+149,top+12,8,t.muted);
            c.drawRoundedRect(x+18,top+29,150,21,BorderRadius.all(4),t.field);
            c.drawRoundedBorder(x+18,top+29,150,21,.5f,BorderRadius.all(4),search.isFocused()?t.accent:t.border);
            search.set(x+24,top+30,136,19); search.setPreview("Search items..."); search.setTextColor(t.text); search.setAlpha(1); search.render(c);
            if(!query.equals(search.getText())) { query=search.getText();listScroll.reset(); }
            var shown=LarpCatalog.entries().stream().filter(e->e.name.toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT))).toList();
            float ly=top+58, lh=bottom-ly-7;
            listScroll.setMax(Math.min(0,lh-shown.size()*27));listScroll.update();
            ScissorUtility.push(c.getMatrices(),x+16,ly,154,lh);
            try {
                float rowY=ly-(float)listScroll.getValue();
                for(var entry:shown) {
                    if(rowY+25>=ly && rowY<ly+lh) {
                        boolean over=hover(c.getMouseX(),c.getMouseY(),x+18,rowY,150,25);
                        if(entry==selected || over)c.drawRoundedRect(x+18,rowY,150,25,BorderRadius.all(4),entry==selected?t.selection:t.hover);
                        c.drawItem(Disguise.preview(entry.item.spec()),x+23,rowY+5,.9f);
                        c.drawFadeoutText(Fonts.MEDIUM.getFont(8),entry.name,x+43,rowY+8,t.text,entry.item.assigned()?105:119,10);
                        if(entry.item.assigned())c.drawRoundedRect(x+157,rowY+10,4,4,BorderRadius.all(2),t.accent);
                        float hy=Math.max(ly,rowY), hh=Math.min(ly+lh,rowY+25)-hy;
                        hits.add(new Hit(x+18,hy,150,hh,()->select(entry)));
                    }
                    rowY+=27;
                }
                if(shown.isEmpty())text(c,"No matching items",x+28,ly+14,8,t.muted);
                c.draw();
            } finally { ScissorUtility.pop(); }
            scrollbar(c,t,listScroll,x+170,ly,lh,shown.size()*27);
            var spec=selected.item.spec();
            c.drawFadeoutText(Fonts.SEMIBOLD.getFont(12),selected.name,x+198,top+13,t.text,211,10);
            text(c,selected.item.assigned()?"ASSIGNED":"PREVIEW",x+198,top+31,7,t.muted);
            c.drawRoundedRect(x+267,top+53,78,70,BorderRadius.all(8),t.field);
            c.drawItem(Disguise.preview(spec),x+282,top+64,3);
            float loreY=top+134, loreH=bottom-loreY-10;
            List<String> lore=new ArrayList<>();Font font=Fonts.MEDIUM.getFont(7.5f);
            for(var line:spec.lore()) {
                String remaining=line.getString();
                while(font.width(remaining)>212 && remaining.length()>1) {
                    int end=remaining.length();while(end>1 && font.width(remaining.substring(0,end))>212)end--;
                    int space=remaining.lastIndexOf(' ',end);if(space>0)end=space;
                    lore.add(remaining.substring(0,end));remaining=remaining.substring(end).stripLeading();
                }
                lore.add(remaining);
            }
            loreScroll.setMax(Math.min(0,loreH-lore.size()*11));loreScroll.update();
            ScissorUtility.push(c.getMatrices(),x+198,loreY,213,loreH);
            try {float py=loreY-(float)loreScroll.getValue();for(String line:lore){c.drawText(font,line,x+198,py,t.muted);py+=11;}c.draw();}
            finally {ScissorUtility.pop();}
            scrollbar(c,t,loreScroll,x+419,loreY,loreH,lore.size()*11);
            text(c,"CUSTOMIZE",x+448,top+13,8,t.muted);
            float sy=top+34;int index=0;
            for(var setting:selected.settings) if(setting instanceof NumberSetting number) {
                CsSlider slider=sliders.get(index++);slider.setValue(number.value);slider.render(c,x+448,sy,158,1);sy+=31;
            }
            boolean held=client!=null && client.player!=null && !client.player.getMainHandStack().isEmpty();
            actionButton(c,t,held?"Assign to held item":"Hold an item to assign",x+462,bottom-48,132,()->LarpCatalog.assign(selected),true,held);
            actionButton(c,t,"Remove",x+462,bottom-26,64,()->LarpCatalog.remove(selected),false,selected.item.assigned());
            actionButton(c,t,"Reset all",x+530,bottom-26,64,()->{LarpRuntime.reset();Status.info("All appearances and effects cleared.");},false,true);
            c.draw();
        } finally {c.getMatrices().pop();}
    }

    @Override public void onMouseClicked(double mx,double my,MouseButton button) {
        if(button!=MouseButton.LEFT)return;mx/=scale;my/=scale;
        search.onMouseClicked(mx,my,button);
        for(var slider:sliders)if(slider.onClick(mx,my)){updateSliders();return;}
        for(var hit:hits)if(hit.contains(mx,my)){hit.action.run();return;}
    }
    private void updateSliders() {int index=0;for(var setting:selected.settings)if(setting instanceof NumberSetting number){double value=sliders.get(index++).getValue();number.setValue(Math.round(value/number.increment())*number.increment());}}
    @Override public void onMouseDragged(double mx,double my,MouseButton button,double dx,double dy) {for(var slider:sliders)if(slider.onDrag(mx/scale))updateSliders();}
    @Override public void onMouseReleased(double mx,double my,MouseButton button) {for(var slider:sliders)slider.onRelease();LarpCatalog.save();search.onMouseReleased(mx/scale,my/scale,button);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double amount) {
        mx/=scale;my/=scale;
        if(hover(mx,my,x+16,y+113,154,H-150)){listScroll.scroll(amount);return true;}
        if(hover(mx,my,x+198,y+189,213,H-225)){loreScroll.scroll(amount);return true;}
        return false;
    }
    @Override public boolean charTyped(char character,int modifiers) {return search.charTyped(character,modifiers);}
    @Override public boolean keyPressed(int key,int scan,int modifiers) {if(key==256){if(search.isFocused())search.setFocused(false);else close();return true;}search.onKeyPressed(key,scan,modifiers);return search.isFocused();}
    @Override public void close() {LarpCatalog.save();client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
