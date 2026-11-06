package dev.bkclient.ui.menu.esp;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.framework.base.CustomScreen;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.event.impl.render.PreHudRenderEvent;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.modules.visuals.ESP;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.ui.clicksys.port.util.NameTagCache;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.clicksys.port.gui.widget.SettingsInspector;
import dev.bkclient.ui.clicksys.port.gui.widget.WidgetState;
import dev.bkclient.ui.clicksys.port.gui.widget.ScissorStack;
import dev.bkclient.ui.clicksys.ClickSysMaterial;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.ScissorUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec2f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import java.util.UUID;

/** Two-column ESP editor with an isolated, interactive model preview. */
public final class EspEditorScreen extends CustomScreen {
    private static final float WIDTH=478, HEIGHT=268, PREVIEW_X=310, PREVIEW_W=168;
    private static final net.minecraft.client.util.SkinTextures PREVIEW_SKIN = new net.minecraft.client.util.SkinTextures(
        net.minecraft.util.Identifier.of("bkclient","textures/preview/bkubiakdev.png"), null, null, null,
        net.minecraft.client.util.SkinTextures.Model.WIDE, false);
    private static final String[] TARGETS={"players","mobs","animals","items"};
    private final ESP esp;
    private final Screen parent;
    private final NameTagCache previewCache=new NameTagCache();
    private final EspPreviewDiagnostics diagnostics=new EspPreviewDiagnostics();
    private SettingsInspector left, right;
    private dev.bkclient.ui.clicksys.port.module.Module adapter;
    private ClientWorld previewWorld;
    private Entity[] samples;
    private OtherClientPlayerEntity skinModel;
    private final net.minecraft.client.util.BufferAllocator previewAllocator=new net.minecraft.client.util.BufferAllocator(262144);
    private final java.util.SequencedMap<net.minecraft.client.render.RenderLayer,net.minecraft.client.util.BufferAllocator> previewLayers=createPreviewLayers();
    private final net.minecraft.client.render.VertexConsumerProvider.Immediate previewConsumers=net.minecraft.client.render.VertexConsumerProvider.immediate(previewLayers,previewAllocator);
    private static java.util.SequencedMap<net.minecraft.client.render.RenderLayer,net.minecraft.client.util.BufferAllocator> createPreviewLayers(){
        var layers=new java.util.LinkedHashMap<net.minecraft.client.render.RenderLayer,net.minecraft.client.util.BufferAllocator>();
        for(var layer:java.util.List.of(net.minecraft.client.render.RenderLayer.getArmorEntityGlint(),net.minecraft.client.render.RenderLayer.getGlintTranslucent(),net.minecraft.client.render.RenderLayer.getGlint(),net.minecraft.client.render.RenderLayer.getEntityGlint()))
            layers.put(layer,new net.minecraft.client.util.BufferAllocator(65536));
        return layers;
    }
    private final org.joml.Vector3f itemCenter=new org.joml.Vector3f();
    private float itemFit=1,itemYaw=0,itemPitch=0;
    private net.minecraft.util.math.Box itemBounds;
    private int target;
    private float scale=1, x, y, yaw=205, pitch=0, zoom=1;
    private boolean dragging;
    private boolean modelArmor;
    public EspEditorScreen(ESP esp, Screen parent) { this.esp=esp; this.parent=parent; }
    private static String tr(String key) { return Localizator.translate(key); }
    public float widgetScale() { return scale / 2; }
    @Override protected void init() { dragging=false; rebuildSettings(); }
    private void rebuildSettings() {
        WidgetState.setWidget2(null);
        if(left!=null)left.hide();if(right!=null)right.hide();
        adapter=new dev.bkclient.ui.clicksys.port.module.Module(esp,esp.editorSettings(target));
        var appearance=new dev.bkclient.ui.clicksys.port.module.Module("esp.editor.appearance",dev.bkclient.ui.clicksys.port.module.Category.VISUAL);
        var labels=new dev.bkclient.ui.clicksys.port.module.Module("esp.editor.labels",dev.bkclient.ui.clicksys.port.module.Category.VISUAL);
        var enabled=new dev.bkclient.ui.clicksys.port.setting.BooleanSetting("esp.editor.enabled","",esp.targetEnabled(target));
        enabled.setOnChange(()->{esp.setTargetEnabled(target,enabled.isFlag());dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();});
        appearance.addSetting(enabled);
        for(var setting:adapter.getSettings()) {
            if(setting==adapter.getKeybindSetting())continue;
            String key=setting.getName();
            boolean general=key.contains("box")||key.contains("glow")||key.endsWith("max_distance");
            (general?appearance:labels).addSetting(setting);
        }
        left=new EditorInspector();right=new EditorInspector();left.show(appearance);right.show(labels);
    }
    private static final class EditorInspector extends SettingsInspector {
        EditorInspector(){super(()->{});utilityPage=true;setValue237(284);}
    }
    @Override public void tick(){if(adapter!=null)adapter.sync();}
    private void drawSettings(UIContext c) {
        ClickSysMaterial.begin(c);
        dev.bkclient.ui.clicksys.port.gui.widget.UiContext.update();
        dev.bkclient.ui.clicksys.port.module.client.ThemeModule.setFlag2();
        var matrix=new org.joml.Matrix4f(c.getMatrices().peek().getPositionMatrix()).scale(.5F,.5F,1);
        try {
            left.position((x+7)*2,(y+66)*2,386);right.position((x+153)*2,(y+66)*2,386);
            left.onFloatFloatFloatMatrix4f(1,c.getMouseY()*2,c.getMouseX()*2,matrix);
            right.onFloatFloatFloatMatrix4f(1,c.getMouseY()*2,c.getMouseX()*2,matrix);
            WidgetState.onMatrix4fFloatFloatFloat(matrix,c.getMouseX()*2,c.getMouseY()*2,1);
            dev.bkclient.ui.clicksys.port.gui.widget.HeaderPainter.onMatrix4fFloat(matrix,1);
        } finally {
            ScissorStack.update2();
            dev.bkclient.ui.clicksys.port.module.client.ThemeModule.setFlag();
            ClickSysMaterial.end();
        }
    }
    private void layout() {
        scale=Math.max(.1F,Math.min(1.15F,.88F*Math.min((width-24F)/WIDTH,(height-24F)/HEIGHT)));
        x=(width/scale-WIDTH)/2; y=(height/scale-HEIGHT)/2;
    }
    @Override public void render(UIContext context) {
        layout();
        context.drawRect(0,0,width,height,new ColorRGBA(0,0,0,80));context.draw();
        UIContext c=context.withMouse((int)(context.getMouseX()/scale),(int)(context.getMouseY()/scale));
        var matrices=c.getMatrices(); matrices.push();
        try(ClickGuiV2Theme theme=ClickGuiV2Theme.openClickSys()) {
            matrices.scale(scale,scale,1);
            panel(c,x,y,302,HEIGHT,theme);
            panel(c,x+PREVIEW_X,y,PREVIEW_W,HEIGHT,theme);
            c.drawText(Fonts.SEMIBOLD.getFont(10),"ESP",x+12,y+12,theme.text);
            c.drawText(Fonts.REGULAR.getFont(7),"BKClient",x+39,y+14,theme.muted);
            c.drawText(Fonts.MEDIUM.getFont(8),tr("esp.editor.preview"),x+PREVIEW_X+12,y+13,theme.text);
            c.draw();
            LucideIcons.CROSS.draw(c,x+PREVIEW_X+PREVIEW_W-21,y+11,10,theme.muted);
            for(int i=0;i<4;i++) {
                float bx=x+8+i*72;
                c.drawRoundedRect(bx,y+33,69,18,BorderRadius.all(4),target==i?theme.selection:theme.field);
                String text=tr(TARGETS[i]);var font=Fonts.MEDIUM.getFont(7);
                c.drawText(font,text,bx+(69-font.width(text))/2,y+39,target==i?theme.accent:theme.muted);
            }
            c.drawText(Fonts.REGULAR.getFont(6),tr("esp.editor.shared"),x+10,y+57,theme.muted);
            c.draw();
            drawPreview(c,theme);
            drawSettings(c);
            c.draw();
        } finally { matrices.pop();diagnostics.finishFrame(); }
    }
    private void panel(UIContext c,float px,float py,float w,float h,ClickGuiV2Theme t) {
        c.drawShadow(px,py,w,h,t.dark?10:18,BorderRadius.all(8),ColorRGBA.BLACK.withAlpha(t.dark?35:65));
        c.drawRoundedRect(px,py,w,h,BorderRadius.all(8),t.window);
        c.drawRoundedBorder(px,py,w,h,t.dark?.25F:.5F,BorderRadius.all(8),t.dark?t.border.mulAlpha(.55F):t.border);
    }
    private void ensureSamples() {
        if(previewWorld==client.world && samples!=null)return;
        releaseSamples();previewWorld=client.world;
        if(previewWorld==null)return;
        OtherClientPlayerEntity player=createPreviewPlayer(previewWorld);
        skinModel=createPreviewPlayer(previewWorld);
        EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND};
        net.minecraft.item.Item[] items={Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS,Items.NETHERITE_SWORD};
        for(int i=0;i<slots.length;i++){ItemStack stack=new ItemStack(items[i]);stack.setDamage(Math.round(stack.getMaxDamage()*(.1F+i*.12F)));stack.addEnchantment(previewWorld.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
            .getOrThrow(i==4?net.minecraft.enchantment.Enchantments.SHARPNESS:net.minecraft.enchantment.Enchantments.PROTECTION),i==4?5:4);
            player.equipStack(slots[i],stack);}
        player.equipStack(EquipmentSlot.OFFHAND,new ItemStack(Items.GOLDEN_APPLE,16));player.setHealth(18);
        ItemEntity item=new ItemEntity(EntityType.ITEM,previewWorld);item.setStack(new ItemStack(Items.NETHERITE_SWORD));
        measureItem(item.getStack());
        skinModel.equipStack(EquipmentSlot.MAINHAND,player.getMainHandStack());
        skinModel.equipStack(EquipmentSlot.OFFHAND,player.getOffHandStack());
        samples=new Entity[]{player,new ZombieEntity(EntityType.ZOMBIE,previewWorld),new PigEntity(EntityType.PIG,previewWorld),item};
    }
    private static OtherClientPlayerEntity createPreviewPlayer(ClientWorld world) {
        return new OtherClientPlayerEntity(world,
            new GameProfile(UUID.fromString("66b11747-4274-4597-9d61-5a1caa78b5da"),"bkubiakdev")) {
                @Override public net.minecraft.client.util.SkinTextures getSkinTextures() {
                    return PREVIEW_SKIN;
                }
                @Override public boolean isPartVisible(net.minecraft.entity.player.PlayerModelPart part) { return true; }
            };
    }
    private void drawPreview(UIContext c,ClickGuiV2Theme theme) {
        float px=x+PREVIEW_X, cx=px+PREVIEW_W/2, top=y+37, bottom=y+226;
        c.drawRoundedRect(px+8,top,PREVIEW_W-16,bottom-top,BorderRadius.all(6),theme.surface);
        c.draw();ensureSamples();
        if(samples==null) {
            c.drawCenteredText(Fonts.REGULAR.getFont(7),tr("esp.editor.join_world"),cx,y+125,theme.muted);
        } else {
            Entity entity=samples[target];
            float modelScale=(target==3?128:target==2?62:63)*zoom;
            float base=y+211, modelHeight=(target==3?.5F:entity.getHeight())*modelScale;
            float bodyTop=base-modelHeight, bodyWidth=(target==3?.5F:entity.getWidth())*modelScale;
            ScissorUtility.push(c.getMatrices(),px+8,top,PREVIEW_W-16,bottom-top);
            c.getMatrices().push();
            try {
                // Leave headroom for both equipment rows, durability and the nameplate.
                c.getMatrices().translate(cx,base,0);
                c.getMatrices().scale(.8F,.8F,1);
                c.getMatrices().translate(-cx,-base,0);
                c.drawRoundedRect(cx-36,base-2,72,5,BorderRadius.all(3),theme.border.mulAlpha(.5F));c.draw();
                if(esp.previewGlowEnabled(target))c.drawShadow(cx-bodyWidth/2,bodyTop,bodyWidth,modelHeight,18,BorderRadius.all(8),esp.previewGlowColor(target).mulAlpha(.32F));
                c.draw();
                drawModel(c,entity,cx,base,modelScale);
                if(entity instanceof PlayerEntity player) {
                    if(esp.isSimpleNametagStyle()) {
                        if(esp.isNameEnabled())esp.getSimpleNametags().renderPreview(previewCache,c,player,cx,bodyTop-14,PREVIEW_W-20);
                    } else esp.drawDetailedOverlay(new PreHudRenderEvent(c,c.getDelta()),player,
                        new float[]{cx-bodyWidth/2,bodyTop,cx+bodyWidth/2,base,bodyWidth,modelHeight},new Vec2f(cx,bodyTop-8));
                } else if(esp.previewNameEnabled(target)) {
                    String label=entity instanceof ItemEntity item?item.getStack().getName().getString():entity.getName().getString();
                    float labelSize=Math.clamp(80*esp.previewNameSize(target),5.5F,12)*esp.previewLabelScale(target);
                    labelSize*=Math.min(1,(PREVIEW_W-24)/Fonts.MEDIUM.getFont(labelSize).width(label));
                    c.drawCenteredText(Fonts.MEDIUM.getFont(labelSize),label,cx,bodyTop-labelSize-7,theme.text);
                }
                c.draw();
            } finally {c.getMatrices().pop();ScissorUtility.pop();}
        }
        c.drawCenteredText(Fonts.REGULAR.getFont(6),diagnostics.active()?"ESP debug: recording...":tr("esp.editor.drag"),cx,y+233,theme.muted);
        c.drawRoundedRect(px+10,y+245,70,15,BorderRadius.all(4),theme.field);
        c.drawCenteredText(Fonts.MEDIUM.getFont(6),tr("esp.editor.reset"),px+45,y+250,theme.text);
        if(target==0){
            c.drawRoundedRect(px+86,y+245,72,15,BorderRadius.all(4),modelArmor?theme.selection:theme.field);
            c.drawCenteredText(Fonts.MEDIUM.getFont(6),tr(modelArmor?"esp.editor.armor_on":"esp.editor.armor_off"),px+122,y+250,theme.text);
        }
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private void drawModel(UIContext c,Entity entity,float cx,float base,float size) {
        diagnostics.before();
        var lights=dev.bkclient.mixin.accessors.RenderSystemLightsAccessor.bkclient$getShaderLights();
        var light0=new org.joml.Vector3f(lights[0]);
        var light1=new org.joml.Vector3f(lights[1]);
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int srcRgb=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dstRgb=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        double clearDepth=GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        var matrices=c.getMatrices();matrices.push();
        if(!modelArmor&&entity instanceof PlayerEntity)entity=skinModel;
        try {
            if(entity instanceof ItemEntity){
                // Same camera convention and held-item model as Swing Animation.
                // Orbit the measured centre, rather than the ground item's bottom edge.
                matrices.translate(cx,base-size*.25F,80);matrices.scale(size,-size,size);
                matrices.multiply(new Quaternionf().rotateX((float)Math.toRadians(itemPitch)).rotateY((float)Math.toRadians(itemYaw+90)));
            }else{
                matrices.translate(cx,base,80);matrices.scale(size,size,-size);
                matrices.multiply(new Quaternionf().rotateZ((float)Math.PI).rotateX((float)Math.toRadians(pitch)).rotateY((float)Math.toRadians(yaw)));
            }
            if(entity instanceof LivingEntity living){living.bodyYaw=0;living.prevBodyYaw=0;living.headYaw=0;living.prevHeadYaw=0;}
            var dispatcher=client.getEntityRenderDispatcher();var consumers=previewConsumers;
            // GUI and world draws share the framebuffer. An opaque panel only replaces
            // colour; stale depth can still reject parts of the model on the next draw.
            // drawPreview keeps this clear scissored to the model panel.
            RenderSystem.depthMask(true);
            GL11.glClearDepth(1);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.enableDepthTest();
            // World nametags/HUD item passes leave different diffuse light directions.
            // Fullbright only fixes the lightmap, not this directional shading.
            if(entity instanceof ItemEntity)net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();
            else net.minecraft.client.render.DiffuseLighting.method_34742();
            if(entity instanceof ItemEntity item){
                matrices.push();
                try {
                    matrices.scale(itemFit,itemFit,itemFit);matrices.translate(-itemCenter.x,-itemCenter.y,-itemCenter.z);
                    client.getEntityRenderDispatcher().getHeldItemRenderer().renderItem(client.player,item.getStack(),
                        net.minecraft.item.ModelTransformationMode.FIRST_PERSON_RIGHT_HAND,false,matrices,consumers,15728880);
                    consumers.draw();
                }
                finally {matrices.pop();}
            } else {
                // Render only the model. World dispatcher labels/shadows use the world camera,
                // producing a second, edge-on nameplate and world geometry in the GUI.
                net.minecraft.client.render.entity.EntityRenderer renderer=dispatcher.getRenderer(entity);
                var state=renderer.createRenderState();
                renderer.updateRenderState(entity,state,1);
                state.displayName=null;state.nameLabelPos=null;state.leashData=null;
                renderer.render(state,matrices,consumers,15728880);consumers.draw();
            }
            if(esp.previewBoxEnabled(target))dev.bkclient.systems.modules.modules.visuals.esp.GradientBoxRenderer.preview(
                matrices,entity instanceof ItemEntity?itemBounds:new net.minecraft.util.math.Box(-entity.getWidth()/2,0,-entity.getWidth()/2,entity.getWidth()/2,entity.getHeight(),entity.getWidth()/2),esp.previewBoxColor(target),esp.previewFillEnabled(target),esp.previewOutlineEnabled(target));
        } finally {
            try {previewConsumers.draw();} finally {
                matrices.pop();
                // Do not let model depth clip the nameplate, equipment or native popups.
                RenderSystem.depthMask(true);
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glClearDepth(clearDepth);
                RenderSystem.depthFunc(depthFunc);
                RenderSystem.depthMask(depthMask);
                if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();
                if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();
                RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);
                if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();
                RenderSystem.setShaderColor(1,1,1,1);
                RenderSystem.setShaderLights(light0,light1);
                diagnostics.after();
            }
        }
    }
    private boolean inside(double mx,double my,float bx,float by,float w,float h){return mx>=bx&&mx<=bx+w&&my>=by&&my<=by+h;}
    @Override public void onMouseClicked(double mx,double my,MouseButton button) {
        mx/=scale;my/=scale;
        if(WidgetState.isWidgetAsBoolean()){WidgetState.isIntDoubleDouble(button.getButtonIndex(),mx*2,my*2);return;}
        if(left.isIntDoubleDouble(button.getButtonIndex(),mx*2,my*2)||right.isIntDoubleDouble(button.getButtonIndex(),mx*2,my*2))return;
        if(button!=MouseButton.LEFT)return;
        if(inside(mx,my,x+PREVIEW_X+PREVIEW_W-25,y+7,20,20)){close();return;}
        for(int i=0;i<4;i++)if(inside(mx,my,x+8+i*72,y+33,69,18)){target=i;rebuildSettings();previewCache.update();diagnostics.modelChanged();dragging=false;return;}
        if(inside(mx,my,x+PREVIEW_X+10,y+245,70,15)){yaw=205;pitch=0;itemYaw=0;itemPitch=0;zoom=1;return;}
        if(target==0&&inside(mx,my,x+PREVIEW_X+86,y+245,72,15)){modelArmor=!modelArmor;return;}
        dragging=inside(mx,my,x+PREVIEW_X+8,y+37,PREVIEW_W-16,189);
    }
    @Override public void onMouseDragged(double mx,double my,MouseButton button,double dx,double dy){
        if(WidgetState.isDoubleIntDoubleDoubleDouble(my/widgetScale(),button.getButtonIndex(),dx/widgetScale(),dy/widgetScale(),mx/widgetScale()))return;
        left.isDoubleDoubleIntDoubleDouble(my/widgetScale(),mx/widgetScale(),button.getButtonIndex(),dx/widgetScale(),dy/widgetScale());
        right.isDoubleDoubleIntDoubleDouble(my/widgetScale(),mx/widgetScale(),button.getButtonIndex(),dx/widgetScale(),dy/widgetScale());
        if(dragging&&button==MouseButton.LEFT){
            if(target==3){itemYaw+=(float)dx/scale*1.5F;itemPitch=Math.clamp(itemPitch+(float)dy/scale*.3F,-85,85);}
            else{yaw+=(float)dx/scale*1.5F;pitch=Math.clamp(pitch+(float)dy/scale*.3F,-45,45);}
        }
    }
    @Override public void onMouseReleased(double mx,double my,MouseButton button){
        dragging=false;mx/=widgetScale();my/=widgetScale();
        WidgetState.isDoubleDoubleInt(mx,my,button.getButtonIndex());
        left.isDoubleDoubleInt(mx,my,button.getButtonIndex());right.isDoubleDoubleInt(mx,my,button.getButtonIndex());
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        if(WidgetState.isDoubleDoubleDouble(mx/widgetScale(),my/widgetScale(),vertical))return true;
        if(left.isDoubleDoubleDouble(vertical,mx/widgetScale(),my/widgetScale())||right.isDoubleDoubleDouble(vertical,mx/widgetScale(),my/widgetScale()))return true;
        mx/=scale;my/=scale;
        if(inside(mx,my,x+PREVIEW_X+8,y+37,PREVIEW_W-16,189))zoom=Math.clamp(zoom+(float)vertical*.08F,.65F,1.3F);
        return true;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_F8){diagnostics.start();return true;}
        if(WidgetState.isWidgetAsBoolean()){
            if(key==256)WidgetState.setWidget2(null);else WidgetState.isIntIntInt2(modifiers,key,scan);
            return true;
        }
        if(left.isIntIntInt2(scan,modifiers,key)||right.isIntIntInt2(scan,modifiers,key))return true;
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean keyReleased(int key,int scan,int modifiers){return WidgetState.isIntIntInt(key,modifiers,scan)||left.isIntIntInt(key,modifiers,scan)||right.isIntIntInt(key,modifiers,scan)||super.keyReleased(key,scan,modifiers);}
    @Override public boolean charTyped(char chr,int modifiers){return WidgetState.isCharInt(chr,modifiers)||left.isIntChar(modifiers,chr)||right.isIntChar(modifiers,chr)||super.charTyped(chr,modifiers);}
    private void releaseSamples(){itemBounds=null;samples=null;skinModel=null;previewWorld=null;previewCache.update();dragging=false;}
    private void measureItem(ItemStack stack){
        var bounds=new ItemBoundsConsumer();
        client.getEntityRenderDispatcher().getHeldItemRenderer().renderItem(client.player,stack,
            net.minecraft.item.ModelTransformationMode.FIRST_PERSON_RIGHT_HAND,false,new net.minecraft.client.util.math.MatrixStack(),layer->bounds,15728880);
        itemCenter.set(bounds.min).add(bounds.max).mul(.5F);
        var span=new org.joml.Vector3f(bounds.max).sub(bounds.min);
        itemFit=.5F/Math.max(.001F,Math.max(span.x,Math.max(span.y,span.z)));
        var half=span.mul(itemFit*.5F);
        itemBounds=new net.minecraft.util.math.Box(-half.x,-half.y,-half.z,half.x,half.y,half.z);
    }
    private static final class ItemBoundsConsumer implements net.minecraft.client.render.VertexConsumer {
        final org.joml.Vector3f min=new org.joml.Vector3f(Float.MAX_VALUE),max=new org.joml.Vector3f(-Float.MAX_VALUE);
        public net.minecraft.client.render.VertexConsumer vertex(float x,float y,float z){var p=new org.joml.Vector3f(x,y,z);min.min(p);max.max(p);return this;}
        public net.minecraft.client.render.VertexConsumer color(int r,int g,int b,int a){return this;}
        public net.minecraft.client.render.VertexConsumer texture(float u,float v){return this;}
        public net.minecraft.client.render.VertexConsumer overlay(int u,int v){return this;}
        public net.minecraft.client.render.VertexConsumer light(int u,int v){return this;}
        public net.minecraft.client.render.VertexConsumer normal(float x,float y,float z){return this;}
    }
    @Override public void removed(){diagnostics.close();previewConsumers.draw();previewAllocator.close();previewLayers.values().forEach(net.minecraft.client.util.BufferAllocator::close);releaseSamples();WidgetState.setWidget2(null);if(left!=null)left.hide();if(right!=null)right.hide();dev.bkclient.ui.clicksys.port.render.TextShader.clearBatches();super.removed();}
    @Override public void close(){releaseSamples();if(TextField.LAST_FIELD!=null)TextField.LAST_FIELD.setFocused(false);dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
    @Override public void renderBackground(DrawContext c,int mx,int my,float delta){}
}


