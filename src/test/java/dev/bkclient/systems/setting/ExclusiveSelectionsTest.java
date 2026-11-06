package dev.bkclient.systems.setting;
import com.google.gson.JsonParser;
import dev.bkclient.systems.setting.settings.SelectSetting;
import java.util.*;
public final class ExclusiveSelectionsTest {
 public static void main(String[] args){
  var settings=new ArrayList<Setting>();
  var checks=new SelectSetting(()->settings,"Checks").exclusive("Only critical hits","Smart criticals");
  var only=new SelectSetting.Value(checks,"Only critical hits").select();
  var smart=new SelectSetting.Value(checks,"Smart criticals");
  var walls=new SelectSetting.Value(checks,"Through walls").select();
  smart.toggle();require(smart.isSelected()&&!only.isSelected()&&walls.isSelected());
  only.toggle();require(only.isSelected()&&!smart.isSelected()&&walls.isSelected());
  only.toggle();require(!only.isSelected()&&!smart.isSelected()&&walls.isSelected());
  checks.load(JsonParser.parseString("[\"Only critical hits\",\"Smart criticals\",\"Through walls\"]"));
  require(smart.isSelected()&&!only.isSelected()&&walls.isSelected());
  var saved=checks.save();checks.load(saved);require(saved.equals(checks.save()));
  checks.setSelectedValues(List.of(smart,only,walls));require(only.isSelected()&&!smart.isSelected()&&walls.isSelected());
  System.out.println("Exclusive checks: both toggle directions, optional off, config conflict, stable save/load and unrelated checks passed");
 }
 private static void require(boolean b){if(!b)throw new AssertionError("Exclusive selection failed");}
}
