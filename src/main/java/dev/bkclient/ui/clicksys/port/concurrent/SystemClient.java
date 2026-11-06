package dev.bkclient.ui.clicksys.port.concurrent;
public final class SystemClient {
 private final ModuleRegistry modules=new ModuleRegistry();
 private final ConfigManager configs=new ConfigManager(modules);
 private final dev.bkclient.ui.clicksys.port.util.HashUtil saver=new dev.bkclient.ui.clicksys.port.util.HashUtil();
 private static class Holder {static final SystemClient INSTANCE=new SystemClient();}
 public static SystemClient getInstance(){return Holder.INSTANCE;}
 public ModuleRegistry getModuleRegistry(){return modules;}
 public ConfigManager getConfigManager(){return configs;}
 public dev.bkclient.ui.clicksys.port.util.HashUtil getHashUtil(){return saver;}
}