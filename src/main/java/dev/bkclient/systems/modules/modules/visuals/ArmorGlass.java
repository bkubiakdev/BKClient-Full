package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.ArmorGlassRenderer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

@ModuleInfo(name="Armor Glass", category=ModuleCategory.VISUALS, desc="Experimental liquid glass material for worn netherite armor")
public final class ArmorGlass extends BaseModule {
    private static ArmorGlass instance;
    private static final String K="modules.settings.armor_glass.";
    public final ColorSetting tint=new ColorSetting(this,K+"tint").color(new ColorRGBA(209,199,255)).alpha(false);
    public final SliderSetting opacity=new SliderSetting(this,K+"opacity").min(0).max(100).currentValue(38).step(1).suffix("%");
    public final SliderSetting refraction=new SliderSetting(this,K+"refraction").min(0).max(20).currentValue(6).step(.5f);
    public final SliderSetting shine=new SliderSetting(this,K+"shine").min(0).max(3).currentValue(1.2f).step(.05f);
    public final SliderSetting edges=new SliderSetting(this,K+"edges").min(0).max(3).currentValue(1).step(.05f);
    public final SliderSetting speed=new SliderSetting(this,K+"speed").min(0).max(3).currentValue(.6f).step(.05f);
    public final ArmorGlassRenderer renderer=new ArmorGlassRenderer();

    public ArmorGlass(){
        instance=this;
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener(){
            public Identifier getFabricId(){return Identifier.of("bkclient","armor_glass");}
            public void reload(ResourceManager manager){renderer.invalidate();}
        });
    }
    public static ArmorGlass getInstance(){return instance;}
    public static boolean supports(ItemStack stack,RegistryKey<EquipmentAsset> asset,EquipmentModel.LayerType type){
        return (type==EquipmentModel.LayerType.HUMANOID||type==EquipmentModel.LayerType.HUMANOID_LEGGINGS)
            &&asset.getValue().equals(Identifier.ofVanilla("netherite"))
            &&(stack.isOf(Items.NETHERITE_HELMET)||stack.isOf(Items.NETHERITE_CHESTPLATE)
                ||stack.isOf(Items.NETHERITE_LEGGINGS)||stack.isOf(Items.NETHERITE_BOOTS));
    }
    @Override public void onDisable(){renderer.close();super.onDisable();}
}
