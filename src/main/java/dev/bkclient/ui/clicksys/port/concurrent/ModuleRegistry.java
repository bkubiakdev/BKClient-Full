package dev.bkclient.ui.clicksys.port.concurrent;
import java.util.*;
import dev.bkclient.ui.clicksys.port.module.Module;
import dev.bkclient.ui.clicksys.port.module.Category;
import dev.bkclient.ui.clicksys.port.module.client.ThemeModule;
public final class ModuleRegistry {
 private final Map<dev.bkclient.systems.modules.Module,Module> adapters=new IdentityHashMap<>();
 private final ThemeModule theme=new ThemeModule();
 public List<Module> getList32(){
  List<Module> result=new ArrayList<>();
  for(var m:dev.bkclient.BKClientInit.getInstance().getModuleManager().getModules()) {

   result.add(adapters.computeIfAbsent(m,Module::new));
  }
  result.add(theme);
  return result;
 }
 public List<Module> getListByCategory2(Category c){return new ArrayList<>(getList32().stream().filter(m->m.getCategory()==c).toList());}
 public Module getModuleByString(String n){return getList32().stream().filter(m->m.getName().equals(n)).findFirst().orElse(null);}
 public <T extends Module>T getModuleByClass(Class<T> c){return c.isInstance(theme)?c.cast(theme):null;}
 public Module getInterfaceSettings(){
  var module=dev.bkclient.BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.visuals.Interface.class);
  return adapters.computeIfAbsent(module,Module::new);
 }
 public void sync(){for(Module m:adapters.values())m.sync();}
}