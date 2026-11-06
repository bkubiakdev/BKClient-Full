package dev.bkclient.ui.clicksys.port.module;
import java.util.*;
import dev.bkclient.ui.clicksys.port.setting.*;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.utility.colors.ColorRGBA;
/** Live adapter: original BKClient widgets edit the existing BKClient settings. */
public class Module extends Feature {
 private final dev.bkclient.systems.modules.Module delegate;
 private final String name; private final Category category;
 private final List<Setting> settings=new ArrayList<>(); private final List<Runnable> refresh=new ArrayList<>();
 private final KeybindSetting keybind; private boolean syncing,enabled;
 public Module(String name,Category category){this.delegate=null;this.name=name;this.category=category;keybind=new KeybindSetting("Bind","",-1,this::toggle);settings.add(keybind);}
 public Module(dev.bkclient.systems.modules.Module m){this(m,m.getDisplaySettings());}
 public Module(dev.bkclient.systems.modules.Module m, List<dev.bkclient.systems.setting.Setting> source){
  delegate=m;name=m.getName();category=switch(m.getCategory()){case COMBAT->Category.COMBAT;case MOVEMENT->Category.MOVEMENT;case PLAYER->Category.PLAYER;case VISUALS->Category.VISUAL;case OTHER->Category.CLIENT;};
  keybind=new KeybindSetting("Bind","",m.getKey(),m::toggle);settings.add(keybind);
  keybind.setOnChange(()->{if(!syncing){m.setKey(keybind.getValue());AbstractSetting.saveConfig();}});
  refresh.add(()->keybind.setInt(m.getKey()));
  var paired = new HashSet<dev.bkclient.systems.setting.Setting>();
  for(var setting:source){
   if (paired.contains(setting)) continue;
   boolean enchantRange = setting.getName().equals("modules.settings.auto_zmianka.from");
   if (setting instanceof dev.bkclient.systems.setting.settings.SliderSetting low
       && (setting.getName().equals("modules.settings.target_pearl.min_landing") || enchantRange)) {
    String highKey = enchantRange ? "modules.settings.auto_zmianka.to" : "modules.settings.target_pearl.max_landing";
    var high = source.stream().filter(candidate -> candidate.getName().equals(highKey))
        .filter(dev.bkclient.systems.setting.settings.SliderSetting.class::isInstance)
        .map(dev.bkclient.systems.setting.settings.SliderSetting.class::cast).findFirst().orElse(null);
    if (high != null) {
     String rangeKey = enchantRange ? "modules.settings.auto_zmianka.damage_range" : "modules.settings.target_pearl.landing_distance";
     var range = new RangeSliderSetting(rangeKey, enchantRange ? rangeKey + ".description" : "",
         low.getCurrentValue(), high.getCurrentValue(), low.getMin(), high.getMax(), low.getStep());
     range.setText(enchantRange ? "%" : " m");
     bind(range,low,()->range.setDouble(low.getCurrentValue()),()->{
       low.setCurrentValue(Math.min((float)range.getValue(),high.getCurrentValue()));
       range.setDouble(low.getCurrentValue());
     });
     bind(range.upper,high,()->range.upper.setDouble(high.getCurrentValue()),()->{
       high.setCurrentValue(Math.max((float)range.upper.getValue(),low.getCurrentValue()));
       range.upper.setDouble(high.getCurrentValue());
     });
     range.setVisibleWhen(()->low.isVisible() && high.isVisible());
     settings.add(range);paired.add(high);continue;
    }
   }
   Setting adapter=adapt(setting);if(adapter!=null)settings.add(adapter);
  }
 }
 public String getName(){return name;} public Category getCategory(){return category;}
 public boolean isEnabled(){return delegate!=null?delegate.isEnabled():enabled;}
 public void setEnabled(boolean b){if(delegate!=null){delegate.setEnabled(b, false);AbstractSetting.saveConfig();}else {enabled=b;if(b)onEnable();else onDisable();}}
 public void toggle(){setEnabled(!isEnabled());}public void onEnable(){}public void onDisable(){}
 public KeybindSetting getKeybindSetting(){return keybind;}
 public List<Setting> getSettings(){return settings;}public List<Setting> getSettingsCopy(){return new ArrayList<>(settings);}
 public List<Setting> getVisibleSettings(){return settings.stream().filter(Setting::isVisible).toList();}
 public void addSettings(Setting...values){settings.addAll(Arrays.asList(values));}
 public void addSetting(Setting s){settings.add(s);}
 public void sync(){syncing=true;try{refresh.forEach(Runnable::run);}finally{syncing=false;}}
 private void bind(Setting out,dev.bkclient.systems.setting.Setting in,Runnable read,Runnable write){
  out.setVisibleWhen(in::isVisible);out.setOnChange(()->{if(!syncing){write.run();AbstractSetting.saveConfig();}});refresh.add(read);
 }
 private String text(String s){return s==null?"":Localizator.translate(s);}
 /** {@code getSuffix()} depends only on the value, the suffix function and the language. */
 private static final class SuffixCache {
  private int bits;private Object function,language;private String suffix;
  String get(dev.bkclient.systems.setting.settings.SliderSetting b){
   int currentBits=Float.floatToIntBits(b.getCurrentValue());Object currentFunction=b.getSuffixFunction(),currentLanguage=Localizator.getCurrentLanguage();
   if(suffix==null||bits!=currentBits||function!=currentFunction||language!=currentLanguage){
    String next=b.getSuffix();bits=currentBits;function=currentFunction;language=currentLanguage;suffix=next;
   }
   return suffix;
  }
 }
 /**
  * Identity fingerprint of everything a list read depends on: the source objects it reads, their
  * flags, the language used by {@link #text} and the adapter's own {@link ListSetting#getRevision()}.
  * When nothing changed, the read would rebuild the same lists and leave the adapter untouched, so it
  * is skipped. Any change (including the user editing the adapter) runs the unchanged original read.
  */
 private static final class ReadSnapshot {
  private static final Object SEPARATOR=new Object();
  private Object[] stored=new Object[8],next=new Object[8];
  private int storedSize=-1,size,revision;
  void begin(){size=0;}
  void add(Object o){if(size==next.length)next=Arrays.copyOf(next,size*2);next[size++]=o;}
  void add(boolean flag){add(flag?Boolean.TRUE:Boolean.FALSE);}
  void separator(){add(SEPARATOR);}
  boolean unchanged(ListSetting out){
   if(storedSize!=size||revision!=out.getRevision())return false;
   for(int i=0;i<size;i++)if(stored[i]!=next[i])return false;
   return true;
  }
  void commit(ListSetting out){
   Object[] swap=stored;stored=next;next=swap;
   storedSize=size;revision=out.getRevision();
  }
 }
 private Setting adapt(dev.bkclient.systems.setting.Setting input){
  String n=input.getName(),d=input.getDescription()==null?"":input.getDescription();
  if(d.endsWith(".description") && Localizator.translate(d).equals(d))d="";
  if(input instanceof dev.bkclient.systems.setting.settings.BooleanSetting b){BooleanSetting out=new BooleanSetting(n,d,b.isEnabled());bind(out,b,()->out.setBoolean(b.isEnabled()),()->b.setEnabled(out.isFlag()));return out;}
  if(input instanceof dev.bkclient.systems.setting.settings.SliderSetting b){
   SliderSetting out=new SliderSetting(n,d,b.getCurrentValue(),b.getMin(),b.getMax(),b.getStep());out.setText(b.getSuffix());
   Setting result=out;
   if(b.getMaxMultiplier()>1){
    SliderSetting multiplier=new SliderSetting("Multiplier","",b.getMultiplier(),1,b.getMaxMultiplier(),1);
    result=new CompactGroupSetting(n,d,out,multiplier);result.setVisibleWhen(b::isVisible);
    bind(multiplier,b,()->multiplier.setDouble(b.getMultiplier()),()->{int target=(int)Math.round(multiplier.getValue());while(b.getMultiplier()!=target)b.cycleMultiplier();});
   }
   SuffixCache suffix=new SuffixCache();
   bind(out,b,()->{out.setDouble(b.getCurrentValue());out.setText(suffix.get(b));},
    ()->b.setCurrentValue((float)out.getValue()));return result;
  }
  if(input instanceof dev.bkclient.systems.setting.settings.RangeSetting b){
   var out=new dev.bkclient.ui.clicksys.port.setting.RangeSliderSetting(n,d,b.getFirstValue(),b.getSecondValue(),b.getMin(),b.getMax(),b.getStep());
   bind(out,b,()->out.setDouble(Math.min(b.getFirstValue(),b.getSecondValue())),()->b.setFirstValue((float)out.getValue()));
   bind(out.upper,b,()->out.upper.setDouble(Math.max(b.getFirstValue(),b.getSecondValue())),()->b.setSecondValue((float)out.upper.getValue()));
   out.setVisibleWhen(b::isVisible);return out;
  }
  if(delegate instanceof dev.bkclient.systems.modules.modules.visuals.CustomSwords
      && input instanceof dev.bkclient.systems.setting.settings.ModeSetting b && input.getName().equals("Weapon")) {
   var values=List.copyOf(b.getValues()); var keys=values.stream().map(v->v.getName()).toList();
   SearchableListSetting out=new SearchableListSetting(n,d,keys,List.of(b.getValue().getName()),false,1,
       this::text,()->Localizator.getCurrentLanguage());
   ReadSnapshot snapshot=new ReadSnapshot();
   bind(out,b,()->{snapshot.begin();snapshot.add(b.getValue());if(snapshot.unchanged(out))return;
       if(!Objects.equals(out.getString2(),b.getValue().getName()))out.addList(List.of(b.getValue().getName()));snapshot.commit(out);},
       ()->values.get(out.getInt2()).select()); return out;
  }
  if(input instanceof dev.bkclient.systems.setting.settings.SelectSetting b
      && (delegate instanceof dev.bkclient.systems.modules.modules.visuals.XRay && input.getName().equals("modules.settings.xray.blocks")
          || delegate instanceof dev.bkclient.systems.modules.modules.combat.AutoTrap && input.getName().equals("modules.settings.auto_trap.blocks"))) {
   var values=List.copyOf(b.getValues()); var keys=values.stream().map(v->v.getName()).toList();
   SearchableListSetting out=new SearchableListSetting(n,d,keys,b.getSelectedValues().stream().map(v->v.getName()).toList(),true,b.getMin(),
       key->net.minecraft.registry.Registries.BLOCK.get(net.minecraft.util.Identifier.of(key)).getName().getString(),
       ()->net.minecraft.util.Language.getInstance(),key->net.minecraft.util.Identifier.of(key).toString());
   // isSelected() of every value and the rebuilt list follow from the selected values in order.
   ReadSnapshot snapshot=new ReadSnapshot();
   bind(out,b,()->{snapshot.begin();for(var v:b.getSelectedValues())snapshot.add(v);if(snapshot.unchanged(out))return;
       for(int i=0;i<values.size();i++)if(out.isInt(i)!=values.get(i).isSelected()){
       out.addList(b.getSelectedValues().stream().map(v->v.getName()).toList());break;}snapshot.commit(out);},
       ()->{for(int i=0;i<values.size();i++){var v=values.get(i);boolean selected=v.isAlwaysEnabled()||out.isInt(i);if(v.isSelected()!=selected)v.toggle();}});
   return out;
  }
  if(input instanceof dev.bkclient.systems.setting.settings.ModeSetting b){
   var names=b.getValues().stream().filter(v->!v.isHidden()).map(v->text(v.getName())).toList();
   ListSetting out=new ListSetting(n,d,names.isEmpty()?List.of("—"):names,List.of(text(b.getValue().getName())),false,1,1);
   ReadSnapshot snapshot=new ReadSnapshot();
   bind(out,b,()->{snapshot.begin();snapshot.add(Localizator.getCurrentLanguage());snapshot.add(b.getValue());
     for(var v:b.getValues()){snapshot.add(v);snapshot.add(v.isHidden());}if(snapshot.unchanged(out))return;
     var next=b.getValues().stream().filter(v->!v.isHidden()).map(v->text(v.getName())).toList();if(!next.isEmpty()&&!out.getList2().equals(next))out.addList2(next);out.addList(List.of(text(b.getValue().getName())));snapshot.commit(out);},
    ()->{for(var v:b.getValues())if(text(v.getName()).equals(out.getString2())){v.select();
      if(delegate instanceof dev.bkclient.systems.modules.modules.visuals.Interface ui && b==ui.getThemeMode()) {
       dev.bkclient.ui.clicksys.port.data.ThemeConfig.setThemePalette(ui.dark.isSelected()
        ?dev.bkclient.ui.clicksys.port.enums.ThemePalette.INSTANCE2:dev.bkclient.ui.clicksys.port.enums.ThemePalette.INSTANCE);
      }
      break;}});return out;
  }
  if(input instanceof dev.bkclient.systems.setting.settings.SelectSetting b){
   var names=b.getValues().stream().filter(v->!v.isHidden()).map(v->text(v.getName())).toList();
   ListSetting out=new ListSetting(n,d,names.isEmpty()?List.of("—"):names,b.getSelectedValues().stream().map(v->text(v.getName())).toList(),true,Math.max(1,names.size()),b.getMin());
   ReadSnapshot snapshot=new ReadSnapshot();
   bind(out,b,()->{snapshot.begin();snapshot.add(Localizator.getCurrentLanguage());
     for(var v:b.getValues()){snapshot.add(v);snapshot.add(v.isHidden());}snapshot.separator();
     for(var v:b.getSelectedValues())snapshot.add(v);if(snapshot.unchanged(out))return;
     var next=b.getValues().stream().filter(v->!v.isHidden()).map(v->text(v.getName())).toList();if(!next.isEmpty()&&!out.getList2().equals(next))out.addList2(next);out.addList(b.getSelectedValues().stream().map(v->text(v.getName())).toList());snapshot.commit(out);},
    ()->{
     // Compute the user's change before applying it: selecting an exclusive option
     // can deselect its peer, which must not then be toggled back on by this loop.
     var changed=b.getValues().stream().filter(v->!v.isHidden() && v.isSelected()!=(v.isAlwaysEnabled()||out.isString(text(v.getName())))).toList();
     for(var v:changed)if(v.isSelected())v.toggle();
     for(var v:changed)if(v.isAlwaysEnabled()||out.isString(text(v.getName())))v.select();
     sync();
    });return out;
  }
  if(input instanceof dev.bkclient.systems.setting.settings.StringSetting b){InputSetting out=new InputSetting(n,d,b.getText(),"Type...");bind(out,b,()->out.setString(b.getText()),()->b.setText(out.getText()));return out;}
  if(input instanceof dev.bkclient.systems.setting.settings.ColorSetting b){ColorSetting out=new ColorSetting(n,d,b.getColor().getRGB(),b.isAlpha());bind(out,b,()->out.setInt(b.getColor().getRGB()),()->b.setColor(ColorRGBA.fromInt(out.getInt2())));return out;}
  if(input instanceof dev.bkclient.systems.setting.settings.BindSetting b){KeybindSetting out=new KeybindSetting(n,d,b.getKey());bind(out,b,()->out.setInt(b.getKey()),()->b.setKey(out.getValue()));return out;}
  if(input instanceof dev.bkclient.systems.setting.settings.ButtonSetting b){ActionSetting out=new ActionSetting(n,d);out.setVisibleWhen(b::isVisible);out.setRunnable(()->{if(b.getAction()!=null)b.getAction().run();});return out;}
  if(input instanceof dev.bkclient.systems.setting.settings.NumberSetting b){
   InputSetting out=new InputSetting(n,d,Double.toString(b.getValue()),"Number",true,0,0);
   bind(out,b,()->out.setDouble(b.getValue()),()->{double value=out.getDouble2();if(Double.isFinite(value))b.setValue(b.isInteger()?Math.rint(value):value);});return out;
  }
  if(input instanceof dev.bkclient.systems.setting.settings.ItemListSetting b){
   ActionSetting out=new ActionSetting(n,d);out.setVisibleWhen(b::isVisible);out.setRunnable(() -> {
    if (net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof dev.bkclient.ui.clicksys.ClickSysScreen screen) screen.openAutoMarket();
    else dev.bkclient.ui.menu.automarket.AutoMarketScreen.open();
   });return out;
  }
  if(input instanceof dev.bkclient.systems.setting.settings.BezierSetting b){
   SliderSetting sx=new SliderSetting("Start X",d,b.start().x,0,1,.01),sy=new SliderSetting("Start Y",d,b.start().y,0,1,.01);
   SliderSetting ex=new SliderSetting("End X",d,b.end().x,0,1,.01),ey=new SliderSetting("End Y",d,b.end().y,0,1,.01);
   CompactGroupSetting out=new CompactGroupSetting(n,d,sx,sy,ex,ey);
   bind(sx,b,()->sx.setDouble(b.start().x),()->b.start((float)sx.getValue(),b.start().y));
   bind(sy,b,()->sy.setDouble(b.start().y),()->b.start(b.start().x,(float)sy.getValue()));
   bind(ex,b,()->ex.setDouble(b.end().x),()->b.end((float)ex.getValue(),b.end().y));
   bind(ey,b,()->ey.setDouble(b.end().y),()->b.end(b.end().x,(float)ey.getValue()));
   out.setVisibleWhen(b::isVisible);return out;
  }
  // Some modules register invisible serialization-only state, without a GUI control.
  if (!input.isVisible()) return null;
  throw new IllegalArgumentException("Unsupported clicksys setting: "+input.getClass().getName());
 }
}
