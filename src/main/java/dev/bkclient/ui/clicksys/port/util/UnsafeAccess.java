package dev.bkclient.ui.clicksys.port.util;
import dev.bkclient.ui.clicksys.port.module.Module;
import dev.bkclient.ui.clicksys.port.concurrent.SystemClient;
public final class UnsafeAccess<T extends Module> {
 private final Class<T> type; public UnsafeAccess(Class<T> type){this.type=type;}
 public T getModule(){return SystemClient.getInstance().getModuleRegistry().getModuleByClass(type);}
 public T getModule2(){T m=getModule();return m!=null&&m.isEnabled()?m:null;}
}