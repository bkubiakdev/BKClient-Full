package dev.bkclient.ui.hud.inline;

import lombok.Generated;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.time.Timer;

public class InlineValue extends SelectSetting.Value {
   private String text = "?";
   private String copy = "";
   private String suffix;
   private String icon = "";
   private String[] graySegments;
   private boolean copied;
   private final Timer copyTimer = new Timer();
   private final Animation copyAnim = new Animation(300L, 0.0F, Easing.BAKEK);
   private final Animation successAnim = new Animation(500L, 0.0F, Easing.BAKEK_SIZE);

   public InlineValue(SelectSetting parent, String name, String suffix) {
      super(parent, name);
      this.select();
      this.suffix = " " + suffix;
   }

   public InlineValue(SelectSetting parent, String name) {
      super(parent, name);
      this.select();
      this.suffix = "";
   }

   public void update(String text, String copy) {
      if (this.isSelected()) {
         this.text = text;
         this.copy = copy;
      }
   }

   public void update(String text) {
      if (this.isSelected()) {
         this.text = text;
      }
   }

   @Generated
   public String text() {
      return this.text;
   }

   @Generated
   public String copy() {
      return this.copy;
   }

   @Generated
   public String suffix() {
      return this.suffix;
   }

   @Generated
   public boolean copied() {
      return this.copied;
   }

   @Generated
   public Timer copyTimer() {
      return this.copyTimer;
   }

   @Generated
   public Animation copyAnim() {
      return this.copyAnim;
   }

   @Generated
   public Animation successAnim() {
      return this.successAnim;
   }

   @Generated
   public InlineValue copied(boolean copied) {
      this.copied = copied;
      return this;
   }

   public String texture() {
      return this.icon;
   }

   public InlineValue texture(String icon) {
      this.icon = icon;
      return this;
   }

   public InlineValue suffix(String suffix) {
      this.suffix = suffix;
      return this;
   }

   public String[] graySegments() {
      return this.graySegments;
   }

   public InlineValue graySegments(String... segments) {
      this.graySegments = segments;
      return this;
   }
}
