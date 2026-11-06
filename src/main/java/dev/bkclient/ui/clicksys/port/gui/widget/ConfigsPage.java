package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.data.ConfigEntry;
import dev.bkclient.ui.clicksys.port.data.ConfigSync;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class ConfigsPage extends PageWidget<ConfigRow, ConfigImportForm> {
   private boolean marketTab;
   private final dev.bkclient.ui.clicksys.port.setting.ChoiceSetting configKind = new dev.bkclient.ui.clicksys.port.setting.ChoiceSetting("", "", new dev.bkclient.ui.clicksys.port.data.ChoiceOption("Client"), new dev.bkclient.ui.clicksys.port.data.ChoiceOption("AutoMarket"), false);
   private final ChoiceRow tabs = new ChoiceRow(configKind, true);
   private final TextField searchField=new TextField(210,28,"Search configs...",14,10,0);
   private String query="";
   private ConfigSync sync() { return marketTab ? dev.bkclient.ui.clicksys.port.data.MarketConfigSync.instance() : ConfigSync.getInstance(); }
   private final ConfigImportForm configImportForm = new ConfigImportForm();


   public ConfigsPage() {
      this.configImportForm.setConsumer(this::onString2);
      this.configImportForm.setCreateConsumer(name -> sync().onStringStringConsumer2(
          "Local configuration", name, result -> { this.configImportForm.complete(result != null); this.update3(); }));
      this.update3();
   }

   @Override public void onFloatFloatFloatMatrix4f(float alpha,float mouseY,float mouseX,org.joml.Matrix4f matrix) {
      float originalY=value236, originalHeight=value238;
      searchField.onFloatFloat2(originalY+10,value235+value237-226);
      searchField.onFloatFloatFloatMatrix4f(alpha,mouseY,mouseX,matrix);
      if(!query.equals(searchField.getString())){query=searchField.getString();list.clear();update3();}
      configKind.setBoolean(marketTab);
      tabs.setValue237(218);
      tabs.onFloatFloat2(originalY+8,value235+16);
      tabs.onFloatFloatFloatMatrix4f(alpha,mouseY,mouseX,matrix);
      value236+=44;value238-=44;
      try{super.onFloatFloatFloatMatrix4f(alpha,mouseY,mouseX,matrix);}finally{value236=originalY;value238=originalHeight;}
   }
   @Override public boolean isIntDoubleDouble(int button,double mx,double my) {
      if(searchField.isIntDoubleDouble(button,mx,my))return true;
      if(tabs.isIntDoubleDouble(button,mx,my))return true;
      if(my<value236+44)return false;
      return super.isIntDoubleDouble(button,mx,my);
   }
   @Override public boolean isDoubleDoubleInt(double mx,double my,int button) {
      if(tabs.isDoubleDoubleInt(mx,my,button)) {
         boolean next=configKind.isFlag3();
         if(next!=marketTab){marketTab=next;list.clear();value240=0;tween4.setFloat(0);flag4=true;update3();}
         return true;
      }
      return super.isDoubleDoubleInt(mx,my,button);
   }
   @Override public boolean isDoubleDoubleIntDoubleDouble(double my,double mx,int button,double dx,double dy) {
      return tabs.isDoubleDoubleIntDoubleDouble(my,mx,button,dx,dy) || super.isDoubleDoubleIntDoubleDouble(my,mx,button,dx,dy);
   }
   @Override public boolean isIntChar(int modifiers,char c){return searchField.isFlag4()?searchField.isIntChar(modifiers,c):super.isIntChar(modifiers,c);}
   @Override public boolean isIntIntInt2(int scan,int modifiers,int key){return searchField.isFlag4()?searchField.isIntIntInt2(scan,modifiers,key):super.isIntIntInt2(scan,modifiers,key);}

   private void onConfigEntry(ConfigEntry configEntry) {
      if (configEntry != null) {
         ConfigSync configsync = sync();
         String s1 = configEntry.getText();
         Runnable runnable = this::update3;
         String s = s1;
         configsync.onRunnableString(runnable, s);
      }
   }

   private String getString() { return "BKClient configuration"; }

   public void update3() {
      sync().setRunnable2(this::update4);
   }

   private void onConfigEntryString(ConfigEntry configEntry, String text) {
      if (configEntry != null) {
         ConfigSync configsync = sync();
         String s2 = configEntry.getText();
         String s3 = text == null ? "" : text;
         Runnable runnable = this::update3;
         String s1 = s3;
         String s = s2;
         configsync.onStringStringRunnable(s1, s, runnable);
      }
   }

   private void onConfigEntry3(ConfigEntry configEntry) {
      this.update3();
   }

   private void update4() {
      List<ConfigEntry> list = sync().getList().stream().filter(entry->entry.getText().toLowerCase(java.util.Locale.ROOT).contains(query.toLowerCase(java.util.Locale.ROOT))).toList();
      LinkedHashMap linkedhashmap = new LinkedHashMap();

      for (ConfigEntry configentry : (Iterable<ConfigEntry>)(list)) {
         if (configentry != null && configentry.getText() != null) {
            linkedhashmap.put(configentry.getText(), configentry);
         }
      }

      ArrayList arraylist = new ArrayList();
      HashSet hashset = new HashSet();

      for (ConfigRow configrow : this.list) {
         ConfigEntry configentry1 = configrow.getConfigEntry();
         if (configrow.isFlag4()) {
            arraylist.add(configrow);
         } else if (configentry1 != null && configentry1.getText() != null) {
            ConfigEntry configentry2 = (ConfigEntry)linkedhashmap.get(configentry1.getText());
            if (configentry2 != null) {
               configrow.setConfigEntry(configentry2);
               arraylist.add(configrow);
               hashset.add(configentry1.getText());
            } else {
               configrow.update3();
               arraylist.add(configrow);
            }
         }
      }

      for (Entry entry : (Iterable<Entry>)(linkedhashmap.entrySet())) {
         if (!hashset.contains(entry.getKey())) {
            arraylist.add(this.getConfigRowByConfigEntry((ConfigEntry)entry.getValue()));
         }
      }

      this.list.clear();
      this.list.addAll(arraylist);

   }

   private void onConfigEntry4(ConfigEntry configEntry) {
      sync().save(configEntry.getText());
      this.update3();
   }

   private void share(ConfigRow row, ConfigEntry entry, boolean privateMode) {
      if (entry == null) return;
      sync().shareCode(entry.getText(), privateMode, success -> {
         if (success) row.markCopied();
      });
   }

   private void update5() {}

   @Override
   protected void onFloat(float value) {
      this.configImportForm.setFloat(value);
   }

   private void onString2(String text) {
      ConfigSync configsync = sync();
      Consumer consumer = var1x -> {
         if (Boolean.FALSE.equals(var1x)) { this.configImportForm.cancelled(); return; }
         if (var1x instanceof ConfigSync.Imported imported) { marketTab=imported.kind()==dev.bkclient.systems.config.ConfigImport.Kind.MARKET;this.list.clear(); }
         if (var1x == null) {
            this.configImportForm.update4();
         } else {
            this.configImportForm.complete(true);
            this.update3();
         }
      };
      configsync.onConsumerString2(consumer, text);
   }

   @Override
   protected void onWidgetBooleanFloatFloat(Widget widget2, boolean flag, float value, float value2) {
      ConfigImportForm configimportform = (ConfigImportForm)widget2;
      this.onFloatBooleanConfigImportFormFloat(value2, flag, configimportform, value);
   }

   @Override
   protected Widget getWidget() {
      return this.getConfigImportForm();
   }

   @Override
   protected boolean check() {
      return this.configImportForm.hasFocus();
   }

   protected ConfigImportForm getConfigImportForm() {
      return this.configImportForm;
   }

   private void onConfigEntryString2(ConfigEntry configEntry, String text) {
      if (configEntry != null && text != null) {
         String s = configEntry.getText();
         if (s != null && !s.equals(text) && !text.isBlank()) {
            sync().onStringStringRunnable2(s, text, this::update3);
         }
      }
   }

   private void onConfigRowConfigEntry2(ConfigRow row, ConfigEntry entry) {
 if (marketTab) net.minecraft.util.Util.getOperatingSystem().open(dev.bkclient.systems.config.ConfigImport.directory(dev.bkclient.systems.config.ConfigImport.Kind.MARKET).toFile());
 else dev.bkclient.BKClientInit.getInstance().getConfigManager().directionConfig();
 }

   private ConfigRow getConfigRowByConfigEntry(ConfigEntry configEntry) {
      ConfigRow configrow = new ConfigRow(configEntry);
      configrow.setFloat(this.value244);
      ConfigSync source=sync();configrow.setSharingState(()->source.isSharing(configEntry.getText()));
      configrow.setConsumer(this::onConfigEntry7);
      configrow.setConsumer2(this::onConfigEntry3);
      configrow.setConsumer3(this::onConfigEntry4);
      configrow.setConsumer4(entry -> this.share(configrow, entry, false));
      configrow.setPrivateShare(entry -> this.share(configrow, entry, true));
      configrow.setConsumer5(this::onConfigEntry);
      configrow.setBiConsumer(this::onConfigEntryString2);
      configrow.setBiConsumer2(this::onConfigEntryString);
      configrow.setConsumer6(var2x -> this.onConfigRowConfigEntry2(configrow, var2x));
      return configrow;
   }

   private void update6() {
      ConfigSync configsync = sync();
      String s2 = this.getString2();
      String s3 = this.getString();
      Consumer consumer = var1x -> this.update3();
      String s1 = s3;
      String s = s2;
      configsync.onStringStringConsumer2(s1, s, consumer);
   }

   private String getString2() {
      String[] astring = new String[]{"config", "profile"};
      HashSet hashset = new HashSet();

      for (ConfigEntry configentry : sync().getList()) {
         if (configentry.getText() != null) {
            hashset.add(configentry.getText());
         }
      }

      Random random = new Random();

      for (int i = 0; i < 32; i++) {
         String s = astring[random.nextInt(astring.length)] + " " + (random.nextInt(9000) + 1000);
         if (!hashset.contains(s)) {
            return s;
         }
      }

      return "config " + Long.toString(System.currentTimeMillis(), 36);
   }

   @Override
   protected float getFloat3() {
      return 300.0F;
   }

   private void onConfigEntry7(ConfigEntry configEntry) {
      if (configEntry != null) {
         ConfigSync configsync = sync();
         String s1 = configEntry.getText();
         Runnable runnable = this::update3;
         String s = s1;
         configsync.onRunnableString2(runnable, s);
      }
   }

   protected void onFloatBooleanConfigImportFormFloat(float value, boolean flag, ConfigImportForm configImportForm, float value2) {
      if (flag) {
         configImportForm.onFloatFloat4(value, value2);
      } else {
         configImportForm.onFloatFloat2(value, value2);
      }
   }
}
