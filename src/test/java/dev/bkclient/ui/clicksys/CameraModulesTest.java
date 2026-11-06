package dev.bkclient.ui.clicksys;
import dev.bkclient.systems.modules.modules.visuals.FreeLook;
import dev.bkclient.systems.modules.modules.visuals.Zoom;
import com.google.gson.JsonObject;
public final class CameraModulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  check(MenuKeybind.accepts(0,0),"legacy menu key works without modifiers");
  int chord=MenuKeybind.normalizeModifiers(75,3);
  check(!MenuKeybind.accepts(chord,0)&&!MenuKeybind.accepts(chord,2)&&MenuKeybind.accepts(chord,3),"menu chord requires all selected modifiers");
  check(MenuKeybind.normalizeModifiers(344,1)==0,"standalone Shift does not require itself");
  check(MenuKeybind.normalizeModifiers(341,3)==1,"Ctrl key removes only its own modifier");
  check(MenuKeybind.normalizeModifiers(-1,7)==0,"cleared binding clears modifiers");
  check(MenuKeybind.format("CTRL + SHIFT + K",true).equals("^ + K"),"symbol mode changes binding text");
  check(MenuKeybind.format("CTRL + K",false).equals("CTRL + K"),"text mode preserves readable modifier names");
  FreeLook look=new FreeLook();Zoom zoom=new Zoom();
  check(look.lookKey.getKey()==342 && look.autoThirdPerson.isEnabled(),"source left Alt translated from VK_LMENU to GLFW");
  check(zoom.zoomKey.getKey()==-1,"source unbound default");
  check(zoom.zoomFov.getMin()==1&&zoom.zoomFov.getMax()==110&&zoom.zoomFov.getCurrentValue()==30,"source FOV range/default");
  check(zoom.speed.getMin()==.1F&&zoom.speed.getMax()==10&&zoom.speed.getCurrentValue()==1,"source speed range/default");
  check(zoom.scrollStep.getHideCondition().getAsBoolean(),"scroll step dependency");
  zoom.scrollControl.setEnabled(true);zoom.zoomKey.setKey(67);zoom.zoomFov.setCurrentValue(12);zoom.speed.setCurrentValue(2);zoom.scrollStep.setCurrentValue(3);
  JsonObject saved=new JsonObject();for(var setting:zoom.getSettings())saved.add(setting.getName(),setting.save());
  Zoom loaded=new Zoom();for(var setting:loaded.getSettings())setting.load(saved.get(setting.getName()));
  check(loaded.zoomKey.getKey()==67&&loaded.zoomFov.getCurrentValue()==12&&loaded.speed.getCurrentValue()==2&&loaded.scrollControl.isEnabled()&&loaded.scrollStep.getCurrentValue()==3,"all zoom settings roundtrip");
  for(double speed:new double[]{.1,1,10}) {
   double one=Zoom.interpolate(90,30,speed,1),many=90;
   for(int i=0;i<240;i++)many=Zoom.interpolate(many,30,speed,1.0/240);
   check(Math.abs(one-many)<.000001,"frame rate independent smoothing");
   check(one>=30&&one<=90,"no overshoot");
   check(Zoom.interpolate(30,90,speed,1)>30,"release returns toward original FOV");
  }
  check(!FreeLook.active(),"new module does not seize camera");
  System.out.println("Camera settings: source defaults/ranges, scroll dependency, persistence, frame-rate-independent Zoom and inactive FreeLook passed.");
 }
}
