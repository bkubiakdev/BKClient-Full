package dev.bkclient.systems.setting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
public final class SliderDefaultsTest {
 public static void main(String[] args){
  SettingsContainer owner=()->new ArrayList<>();
  for(float[] row:new float[][]{{0,255,180,1},{10,300,75,1},{.1f,3,1.5f,.1f},{.01f,.12f,.045f,.005f},{-3,3,-.2f,.01f},{-1,1,0,.05f}}){
   var early=new SliderSetting(owner,"early").min(row[0]).max(row[1]).step(row[3]).currentValue(row[2]);
   var late=new SliderSetting(owner,"late").min(row[0]).max(row[1]).currentValue(row[2]).step(row[3]);
   check(Math.abs(early.getCurrentValue()-late.getCurrentValue())<.00001,"builder order");check(Math.abs(late.getCurrentValue()-row[2])<.00001,"declared default retained");
  }
  var s=new SliderSetting(owner,"saved").min(0).max(1).currentValue(.5f).step(.1f);
  s.load(new JsonPrimitive(0));check(s.getCurrentValue()==0,"explicit saved zero preserved");s.load(new JsonPrimitive(.73f));check(Math.abs(s.getCurrentValue()-.7f)<.00001,"load snaps to step");s.load(new JsonPrimitive(9));check(s.getCurrentValue()==1,"clamped to range");s.setCurrentValue(Float.NaN);check(s.getCurrentValue()==1,"invalid input cannot poison value");
  System.out.println("Slider defaults: order-independent values, precision, intentional zero, config load and bounds passed");
 }
 private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
}
