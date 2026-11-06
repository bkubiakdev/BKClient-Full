package dev.bkclient.ui.clicksys.port.setting;

import dev.bkclient.ui.clicksys.port.concurrent.Translations;
import dev.bkclient.ui.clicksys.port.util.StringParts;
import dev.bkclient.ui.clicksys.port.util.TextHash;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import java.util.function.Supplier;

public abstract class Setting {
   protected String[] nameParts;
   protected String[] descriptionParts;
   @Expose
   protected boolean visible = true;
   private transient String nameText;
   private transient String descriptionText;
   private transient String nameHash;
   private transient String descriptionHash;
   protected transient Runnable onChange;
   protected transient Runnable onUpdate;
   protected transient Supplier<Boolean> visibleWhen;

   public Setting(String text, String text2) {
      this.nameParts = StringParts.split(text);
      this.descriptionParts = StringParts.split(text2);
   }

   public String getNameHash() {
      String s = this.nameHash;
      if (s == null) {
         s = Long.toHexString(TextHash.getLongByStringArray(this.nameParts));
         this.nameHash = s;
      }

      return s;
   }

   public abstract String getTypeId();

   public Runnable getOnChange() {
      return this.onChange;
   }

   public void setOnChange(Runnable runnable) {
      this.onChange = runnable;
   }

   public String[] getDescriptionParts() {
      return this.descriptionParts;
   }

   public abstract void reset();

   public void setOnUpdate(Runnable runnable) {
      this.onUpdate = runnable;
   }

   public Runnable getOnUpdate() {
      return this.onUpdate;
   }

   public void setVisible(boolean flag) {
      this.visible = flag;
   }

   public String getDisplayDescription() {
      String s = Translations.getInstance().getStringByString(this.getDescriptionHash());
      return s != null ? s : dev.bkclient.systems.localization.Localizator.translate(this.getDescription());
   }

   public String getDescriptionHash() {
      String s = this.descriptionHash;
      if (s == null) {
         s = Long.toHexString(TextHash.getLongByStringArray(this.descriptionParts));
         this.descriptionHash = s;
      }

      return s;
   }

   public void fireOnChange() {
      if (this.onChange != null) {
         try {
            this.onChange.run();
         } catch (Exception exception) {
            System.err.println(exception.getMessage());
         }
      }
   }

   public boolean isVisible() {
      return this.visibleWhen != null ? this.visibleWhen.get() : this.visible;
   }

   public Supplier<Boolean> getVisibleWhen() {
      return this.visibleWhen;
   }

   public String[] getNameParts() {
      return this.nameParts;
   }

   public abstract void fromJson(JsonObject jsonObject);

   public void setVisibleWhen(Supplier<Boolean> supplier) {
      this.visibleWhen = supplier;
   }

   public abstract JsonObject toJson();

   public String getDescription() {
      if (descriptionText == null) descriptionText = StringParts.join(this.descriptionParts);
      return descriptionText;
   }

   public String getDisplayName() {
      String s = Translations.getInstance().getStringByString(this.getNameHash());
      return s != null ? s : dev.bkclient.systems.localization.Localizator.translate(this.getName());
   }

   public String getName() {
      if (nameText == null) nameText = StringParts.join(this.nameParts);
      return nameText;
   }

   public void setDescription(String text) {
      this.descriptionParts = StringParts.split(text);
      this.descriptionHash = null;
      this.descriptionText = null;
   }

   public void setName(String text) {
      this.nameParts = StringParts.split(text);
      this.nameHash = null;
      this.nameText = null;
   }
}
