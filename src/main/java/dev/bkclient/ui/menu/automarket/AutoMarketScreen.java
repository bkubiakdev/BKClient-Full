package dev.bkclient.ui.menu.automarket;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.modules.bkmods.AutoMarket;
import dev.bkclient.systems.modules.modules.bkmods.config.AutoMarketConfigManager;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.setting.settings.rynek.PriceEntry;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.components.SettingControls;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.MenuScreen;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.ui.menu.clickgui.content.AutoMarketContent;
import dev.bkclient.ui.menu.clickgui.widget.CsSlider;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.inventory.ItemStackExtractor;
import dev.bkclient.utility.render.ScissorUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import java.util.*;

/** Dedicated market workspace; shares the real registry and existing editors. */
public final class AutoMarketScreen extends MenuScreen {
    private static final float W = 620, H = 350, SIDE = 184, MAIN = W - SIDE - 10;
    private final Screen parent;
    private final AutoMarket module;
    private AutoMarketContent editor = new AutoMarketContent();
    private final AutoMarketContent cards = new AutoMarketContent();
    private final TextField search = new TextField(Fonts.MEDIUM.getFont(7));
    private final TextField profileName = new TextField(Fonts.MEDIUM.getFont(7));
    private final TextField command = new TextField(Fonts.MEDIUM.getFont(7));
    private final TextField webhook = new TextField(Fonts.MEDIUM.getFont(7));
    private final CsSlider openDelay = new CsSlider("Confirm delay", 0, 1000, 173, v -> Math.round(v) + " ms");
    private final CsSlider retryDelay = new CsSlider("Retry delay", 0, 5000, 1500, v -> Math.round(v) + " ms");
    private final List<Hit> hits = new ArrayList<>();
    private final Set<PriceEntry> expandedItems = Collections.newSetFromMap(new IdentityHashMap<>());
    private List<AutoMarketConfigManager.ConfigInfo> profiles = List.of();
    private PriceEntry selected;
    private boolean settings, creating;
    private float scale = 1, ox, oy, scroll, maxScroll;
    private String message = "", activeProfile;
    private record Hit(float x, float y, float w, float h, Runnable action) {
        boolean contains(double mx, double my) { return mx >= x && mx <= x+w && my >= y && my <= y+h; }
    }
    public AutoMarketScreen(Screen parent) {
        this.parent = parent;
        module = BKClientInit.getInstance().getModuleManager().getModule(AutoMarket.class);
        closing = false;
        activeProfile = module.getActiveProfile();
        refreshProfiles();
    }
    public static void open() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof AutoMarketScreen)) mc.setScreen(new AutoMarketScreen(mc.currentScreen));
    }
    private void refreshProfiles() { module.configManager.refresh(); profiles = new ArrayList<>(module.configManager.getConfigs()); }
    private void resetEditor() { editor.finishEditing(); editor = new AutoMarketContent(); }
    private void select(PriceEntry entry) { resetEditor(); selected = entry; creating = settings = false; message = ""; }
    private void save() { editor.finishEditing(); module.syncControllersConfig(); module.configManager.save(module.getActiveProfile()); }
    private void switchProfile(String name) {
        save(); module.configName.setText(name); selected = null; settings = creating = false;
        editor = new AutoMarketContent(); activeProfile = module.getActiveProfile(); scroll = 0;
        dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();
    }
    private void beginCreate() {
        resetEditor(); selected = null; creating = true; settings = false; message = "";
        profileName.setText(""); command.setText(module.marketCommand.getText()); webhook.setText(module.discordWebhookUrl.getText());
        openDelay.setValue(module.confirmDelay.getCurrentValue()); retryDelay.setValue(module.restartDelay.getCurrentValue());
    }
    private void createProfile() {
        if (profileName.getText().isBlank()) { message = "Enter a profile name"; return; }
        String name = AutoMarketConfigManager.sanitizeProfileName(profileName.getText());
        if (module.configManager.profileExists(name) || name.equals(module.getActiveProfile())) { message = "This profile already exists"; return; }
        if (command.getText().isBlank()) { message = "Enter a market command"; return; }
        save();
        // Save a separate profile before switching, retaining the previous registry on disk.
        module.configManager.save(name);
        if (!module.configManager.profileExists(name)) { message = "Could not save profile"; return; }
        module.configName.setText(name);
        module.itemRegistry.getValue().clear(); module.itemRegistry.notifyChanged();
        module.marketCommand.setText(command.getText()); module.discordWebhookUrl.setText(webhook.getText());
        module.confirmDelay.setCurrentValue((float)openDelay.getValue()); module.restartDelay.setCurrentValue((float)retryDelay.getValue());
        module.syncControllersConfig(); module.configManager.save(name);
        activeProfile = name; creating = false; message = ""; scroll = 0;
        profileName.setFocused(false); command.setFocused(false); webhook.setFocused(false);
        refreshProfiles(); dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();
    }
    private void addHeldItem() {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.getMainHandStack().isEmpty()) { message = "Hold the item you want to add"; return; }
        PriceEntry entry = ItemStackExtractor.extract(mc.player.getMainHandStack());
        if (entry == null) return;
        module.itemRegistry.getValue().add(entry); module.itemRegistry.notifyChanged(); select(entry); save();
    }
    @Override public void render(UIContext context) {
        if (!Objects.equals(activeProfile, module.getActiveProfile())) {
            // A profile loaded elsewhere must never receive unfinished edits from the old one.
            editor = new AutoMarketContent(); selected = null; scroll = 0; activeProfile = module.getActiveProfile(); refreshProfiles();
        }
        if (selected != null && !module.itemRegistry.getValue().contains(selected)) { selected = null; editor = new AutoMarketContent(); }
        scale = .8625f * Math.max(.1f, Math.min(Interface.menuScale(), Math.min((width-16f)/W, (height-16f)/H)));
        ox = (width-W*scale)/2; oy = (height-H*scale)/2;
        int mx=context.getMouseX(), my=context.getMouseY();
        context.setMouse((int)((mx-ox)/scale), (int)((my-oy)/scale));
        context.getMatrices().push(); context.getMatrices().translate(ox,oy,0); context.getMatrices().scale(scale,scale,1);
        try (var theme = ClickGuiV2Theme.openLiquid()) { draw(context); }
        finally { context.getMatrices().pop(); context.setMouse(mx,my); }
    }
    private void draw(UIContext c) {
        hits.clear(); panel(c,0,0,MAIN,H); panel(c,MAIN+10,0,SIDE,H);
        LucideIcons.CART.draw(c,12,12,13,Colors.getAccent()); text(c,dev.bkclient.systems.localization.Localizator.translate("AutoMarket"),32,13,10,1);
        button(c,"Add held item",MAIN-92,9,82,19,this::addHeldItem);
        field(c,search,"Search items",12,40,MAIN-24);
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        List<PriceEntry> entries = module.itemRegistry.getValue().stream()
            .filter(e -> matchesSearch(e, query)).toList();
        float top=68, bottom=H-24, cw=(MAIN-34)/2, gap=10;
        float cy=top-scroll;
        ScissorUtility.push(c.getMatrices(),8,top,MAIN-16,bottom-top);
        for(int i=0;i<entries.size();i+=2) {
            float rowH=0;
            for(int j=0;j<2 && i+j<entries.size();j++) {
                PriceEntry e=entries.get(i+j); boolean expanded=expandedItems.contains(e);
                float ch=expanded?106:38, x=12+j*(cw+gap); rowH=Math.max(rowH,ch);
                if(cy+ch<top || cy>bottom) continue;
                c.drawClientRect(x,cy,cw,ch,1,0,2);
                if(selected==e) {
                    c.drawRoundedRect(x+2,cy+9,2,ch-18,BorderRadius.all(1),Colors.getAccent());
                }
                cards.renderItemCard(c,e,x,cy,cw,expanded);
                float yy=cy;
                hitClipped(x+cw-62,yy+8,28,20,top,bottom,()->{cards.toggleItem(e);save();});
                hitClipped(x+cw-25,yy+5,24,26,top,bottom,()->select(e));
                hitClipped(x,yy,cw-66,35,top,bottom,()->{if(!expandedItems.add(e))expandedItems.remove(e);});
                if(expanded)hitClipped(x,yy+36,cw,ch-36,top,bottom,()->select(e));
            }
            cy+=rowH+gap;
        }
        ScissorUtility.pop();
        maxScroll=Math.max(0,cy+scroll-bottom); scroll=Math.min(scroll,maxScroll);
        if(entries.isEmpty()) { text(c,dev.bkclient.systems.localization.Localizator.translate("No items in this view"),60,145,11,.85f); text(c,dev.bkclient.systems.localization.Localizator.translate("Hold an item and click Add held item"),60,163,7,.45f); }
        if(!message.isEmpty())text(c,dev.bkclient.systems.localization.Localizator.translate(message),12,H-14,6.5f,.85f);
        float sx=MAIN+10;
        text(c,creating?dev.bkclient.systems.localization.Localizator.translate("Create profile"):settings?dev.bkclient.systems.localization.Localizator.translate("Market settings"):selected!=null?dev.bkclient.systems.localization.Localizator.translate("Edit rule"):dev.bkclient.systems.localization.Localizator.translate("AutoMarket"),sx+12,14,10,1);
        LucideIcons.CROSS.draw(c,sx+SIDE-23,12,11,Colors.getTextColor().mulAlpha(.7f));
        hits.add(new Hit(sx+SIDE-28,6,25,25,()->{if(creating || settings || selected!=null){resetEditor();creating=settings=false;selected=null;}else close();}));
        if(creating) {
            text(c,dev.bkclient.systems.localization.Localizator.translate("Name"),sx+12,45,7,.6f); field(c,profileName,"Profile name",sx+12,58,SIDE-24);
            text(c,dev.bkclient.systems.localization.Localizator.translate("Open command"),sx+12,89,7,.6f);field(c,command,"/ah",sx+12,102,SIDE-24);
            openDelay.render(c,sx+12,137,SIDE-24,1);retryDelay.render(c,sx+12,178,SIDE-24,1);
            text(c,dev.bkclient.systems.localization.Localizator.translate("Webhook URL"),sx+12,226,7,.6f);field(c,webhook,"Optional",sx+12,239,SIDE-24);
            button(c,"Create profile",sx+12,H-30,SIDE-24,20,this::createProfile);
        } else {
            button(c,"Settings",sx+12,34,(SIDE-30)/2,18,()->{resetEditor();selected=null;settings=true;});
            if(selected!=null)button(c,"Remove item",sx+18+(SIDE-30)/2,34,(SIDE-30)/2,18,()->{PriceEntry old=selected;resetEditor();selected=null;module.itemRegistry.getValue().remove(old);module.itemRegistry.notifyChanged();save();});
            if(settings || selected!=null) {
                editor.renderSidebar(c,selected,settings,sx+2,60,SIDE-4,H-98);
                button(c,"Save",sx+12,H-30,SIDE-24,20,()->{save();message="Saved";});
            } else {
                text(c,dev.bkclient.systems.localization.Localizator.translate("Select an item to edit its rule."),sx+12,83,7,.55f);
                text(c,dev.bkclient.systems.localization.Localizator.translate("Changes use the active profile."),sx+12,98,7,.4f);
                button(c,"Create profile",sx+12,127,SIDE-24,20,this::beginCreate);
            }
        }
    }
    private static boolean matchesSearch(PriceEntry entry, String query) {
        if (query.isBlank()) return true;
        StringBuilder haystack = new StringBuilder(160);
        appendSearchText(haystack, entry.getName());
        appendSearchText(haystack, entry.getMaterial());
        appendSearchText(haystack, entry.getLore());
        appendSearchText(haystack, entry.getEnchants());
        appendSearchText(haystack, entry.getExcludedEnchants());
        if (entry.getEnchantments() != null) {
            entry.getEnchantments().forEach((id, level) -> {
                appendSearchText(haystack, id);
                haystack.append(' ').append(level);
            });
        }
        if (entry.getBannedEnchantsList() != null) {
            entry.getBannedEnchantsList().forEach(value -> appendSearchText(haystack, value));
        }
        String searchable = haystack.toString();
        for (String token : query.split("\\s+")) {
            if (!token.isBlank() && !searchable.contains(token)) return false;
        }
        return true;
    }

    private static void appendSearchText(StringBuilder target, String value) {
        if (value != null && !value.isBlank()) target.append(' ').append(value.toLowerCase(Locale.ROOT));
    }
    private static String trim(String s,float w){var f=Fonts.MEDIUM.getFont(7);while(s.length()>1&&f.width(s)>w)s=s.substring(0,s.length()-1);return s;}
    private void hitClipped(float x,float y,float w,float h,float top,float bottom,Runnable action){float a=Math.max(y,top),b=Math.min(y+h,bottom);if(b>a)hits.add(new Hit(x,a,w,b-a,action));}
    private void text(UIContext c,String s,float x,float y,float size,float alpha){c.drawText(Fonts.MEDIUM.getFont(size),s,x,y,Colors.getTextColor().mulAlpha(alpha));}
    private void panel(UIContext c,float x,float y,float w,float h){
        c.drawClientRect(x,y,w,h,1,0,2);
    }
    private void button(UIContext c,String label,float x,float y,float w,float h,Runnable action){
        boolean hovered=c.getMouseX()>=x && c.getMouseX()<=x+w && c.getMouseY()>=y && c.getMouseY()<=y+h;
        SettingControls.buttonBackground(c,x,y,w,h,hovered?1:0,1);
        String displayLabel = dev.bkclient.systems.localization.Localizator.translate(label);
        float textWidth = Fonts.MEDIUM.getFont(7).width(displayLabel);
        float labelX = x + (w - textWidth) / 2;
        if ("Save".equals(label)) {
            labelX += 7;
            LucideIcons.SAVE.draw(c,labelX-15,y+(h-10)/2,10,Colors.getTextColor().mulAlpha(.95f));
        }
        text(c,displayLabel,labelX,y+6,7,.95f);
        hits.add(new Hit(x,y,w,h,action));
    }
    private void field(UIContext c,TextField f,String placeholder,float x,float y,float w){
        c.drawClientRect(x,y,w,20,1,0,2);
        f.set(x+6,y,w-12,20);f.setPreview(dev.bkclient.systems.localization.Localizator.translate(placeholder));f.setTextColor(Colors.getTextColor());f.render(c);
    }
    private double lx(double x){return (x-ox)/scale;} private double ly(double y){return (y-oy)/scale;}
    @Override public void onMouseClicked(double x,double y,MouseButton b){
        if(b!=MouseButton.LEFT)return; x=lx(x);y=ly(y);
        for(Hit hit:new ArrayList<>(hits))if(hit.contains(x,y)){search.setFocused(false);profileName.setFocused(false);command.setFocused(false);webhook.setFocused(false);hit.action.run();return;}
        search.onMouseClicked(x,y,b);
        if(creating){profileName.onMouseClicked(x,y,b);command.onMouseClicked(x,y,b);webhook.onMouseClicked(x,y,b);openDelay.onClick(x,y);retryDelay.onClick(x,y);}
        else if(x>MAIN+10 && y>=60 && y<H-38 && (settings||selected!=null))editor.handleClick(x,y);
    }
    @Override public void onMouseDragged(double x,double y,MouseButton b,double dx,double dy){if(!creating)editor.handleDrag(lx(x),ly(y));}
    @Override public void onMouseReleased(double x,double y,MouseButton b){editor.handleRelease();openDelay.onRelease();retryDelay.onRelease();}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        x=lx(x);y=ly(y);
        if(x>=0&&x<MAIN&&y>=68&&y<H-24)scroll=Math.clamp(scroll-(float)vertical*22,0,maxScroll);
        else if(!creating&&x>MAIN+10&&y>=60&&y<H-38)editor.onScroll(x,y,vertical);
        return true;
    }
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==256){close();return true;}
        if(search.isFocused()){search.onKeyPressed(key,scan,mods);scroll=0;return true;}
        if(creating){for(TextField f:List.of(profileName,command,webhook))if(f.isFocused())f.onKeyPressed(key,scan,mods);return true;}
        return editor.keyPressed(key,scan,mods)||super.keyPressed(key,scan,mods);
    }
    @Override public boolean charTyped(char chr,int mods){
        if(search.isFocused()){scroll=0;return search.charTyped(chr,mods);}
        if(creating){boolean handled=false;for(TextField f:List.of(profileName,command,webhook))handled|=f.charTyped(chr,mods);return handled;}
        return editor.charTyped(chr,mods);
    }
    @Override public void close(){save();closing=true;if(parent instanceof MenuScreen menu)menu.setClosing(false);MinecraftClient.getInstance().setScreen(parent);}
    @Override public void removed(){save();search.setFocused(false);profileName.setFocused(false);command.setFocused(false);webhook.setFocused(false);super.removed();}
    @Override public boolean shouldPause(){return false;}
    @Override public void renderBackground(DrawContext c,int x,int y,float delta){}
}
