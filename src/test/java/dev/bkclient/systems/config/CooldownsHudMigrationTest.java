package dev.bkclient.systems.config;
import com.google.gson.*;
public final class CooldownsHudMigrationTest {
 public static void main(String[] args) {
  JsonObject input=JsonParser.parseString("""
   {"hudElements":[
    {"name":"hud.effects","x":9},
    {"name":"hud.cooldowns","x":10,"y":20,"showing":true,"settings":{"scale":1.2,"hud.editor.alignment":"hud.editor.left"}},
    {"name":"hud.cooldowns2","x":110,"y":120,"showing":true,"settings":{"hud.editor.joined":true,"hud.editor.alignment":"hud.editor.center"}}]}
   """).getAsJsonObject();
  JsonObject result=CooldownsHudMigration.migrate(input);
  JsonObject hud=result.getAsJsonArray("hudElements").get(1).getAsJsonObject();
  check(result.getAsJsonArray("hudElements").size()==2);
  check(hud.get("name").getAsString().equals("hud.cooldowns") && hud.get("x").getAsInt()==110);
  check(hud.getAsJsonObject("settings").get("scale").getAsFloat()==1.2f);
  check(hud.getAsJsonObject("settings").get("hud.editor.alignment").getAsString().equals("hud.editor.center"));
  check(result.equals(CooldownsHudMigration.migrate(result)) && input.getAsJsonArray("hudElements").size()==3);
  input.getAsJsonArray("hudElements").get(2).getAsJsonObject().addProperty("showing",false);
  hud=CooldownsHudMigration.migrate(input).getAsJsonArray("hudElements").get(1).getAsJsonObject();
  check(hud.get("x").getAsInt()==10 && hud.get("showing").getAsBoolean());
  input.getAsJsonArray("hudElements").remove(1);
  hud=CooldownsHudMigration.migrate(input).getAsJsonArray("hudElements").get(1).getAsJsonObject();
  check(hud.get("x").getAsInt()==110 && !hud.get("showing").getAsBoolean());
  System.out.println("Cooldowns HUD migration passed: visibility, geometry, merged settings, single widget and idempotence");
 }
 private static void check(boolean ok){if(!ok)throw new AssertionError("Cooldown HUD migration");}
}
