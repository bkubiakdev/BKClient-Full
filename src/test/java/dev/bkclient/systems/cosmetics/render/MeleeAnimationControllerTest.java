package dev.bkclient.systems.cosmetics.render;
import com.google.gson.JsonParser;
import java.util.*;
public final class MeleeAnimationControllerTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void near(float a,float b,String m){check(Math.abs(a-b)<.001,m+": "+a+" != "+b);}
 static final class Audio implements MeleeAnimationController.Audio {
  int starts,stops; String last;
  public void start(int track,BedrockAnimation.Clip c,boolean loop){starts++;last=c.name();}
  public void update(int track,float seconds){} public void stop(int track){stops++;}
 }
 public static void main(String[] args){
  CosmeticAnimationClockTest.main(args);
  var box=BedrockModel.parse(JsonParser.parseString("{\"minecraft:geometry\":[{\"description\":{},\"bones\":[{\"name\":\"cube\",\"cubes\":[{\"origin\":[0,0,0],\"size\":[2,3,4],\"uv\":[0,0]}]}]}]}").getAsJsonObject());
  for(var f:box.bones.get(0).cubes().get(0).faces()){
   var v=f.vertices();float ax=-(v[3]-v[0]),ay=v[4]-v[1],az=v[5]-v[2],bx=-(v[6]-v[0]),by=v[7]-v[1],bz=v[8]-v[2];
   float dot=(ay*bz-az*by)*-f.nx()+(az*bx-ax*bz)*f.ny()+(ax*by-ay*bx)*f.nz();
   check(dot>0,"face winding agrees with outward normal after Bedrock X conversion: "+f.nx()+","+f.ny()+","+f.nz());
  }

  var clips=BedrockAnimation.parse(JsonParser.parseString("""
   {"animations":{
    "static_idle":{"animation_length":1,"bones":{"hand":{"position":[2,3,4],"rotation":[1,2,3]}}},
    "draw":{"animation_length":0.4,"bones":{"hand":{"rotation":[10,20,30]}}},
    "inspect":{"animation_length":1,"bones":{"hand":{"rotation":[40,50,60]}}},
    "inspect_1":{"animation_length":1,"bones":{"hand":{"rotation":[50,60,70]}}},
    "inspect_2":{"animation_length":1,"bones":{"hand":{"rotation":[60,70,80]}}},
    "melee_1":{"animation_length":0.5,"bones":{"hand":{"rotation":[70,80,90]}}},
    "melee_2":{"animation_length":0.5,"bones":{"hand":{"rotation":[80,90,100]}}},
    "melee_3":{"animation_length":0.5,"bones":{"hand":{"rotation":[90,100,110]}}},
    "run_start":{"animation_length":0.2,"bones":{"hand":{"position":[0,1,0]}}},
    "run":{"animation_length":0.8,"bones":{"hand":{"position":[0,2,0]}}},
    "run_end":{"animation_length":0.2,"bones":{"hand":{"position":[0,0,0]}}}
   }}
   """).getAsJsonObject());
  Audio a=new Audio();var c=new MeleeAnimationController(clips,List.of("hand"),"",new Random(7),a);
  check(c.actionName().equals("draw"),"draw on bind");
  near(c.pose("hand").position()[0],2,"missing channel retains base");
  c.tick(.2f,false,true,0);check(c.actionName().equals("draw"),"draw before end");
  c.tick(.21f,false,true,0);check(c.actionName().isEmpty(),"draw completion");
  c.attack(true);check(c.actionName().equals("melee_1"),"primary1");
  c.attack(true);check(c.actionName().equals("melee_2"),"primary2 interruption");
  c.attack(false);check(c.actionName().equals("melee_3"),"secondary distinct");
  c.tick(.2f,false,true,0);near(c.pose("hand").rotation()[0],90,"fade reaches action");
  c.tick(.4f,false,true,0);c.tick(.2f,true,true,0);check(c.movementName().equals("run_start"),"run enters");
  c.tick(.21f,true,true,.2f);check(c.movementName().equals("run"),"run loops");
  c.tick(.21f,true,true,.4f);near(c.pose("hand").position()[1],5,"additive running pose");
  c.tick(.01f,false,true,.4f);check(c.movementName().equals("run_end"),"run exits");
  var seen=new HashSet<String>();for(int i=0;i<30;i++){c.inspect();seen.add(c.actionName());}
  check(seen.containsAll(List.of("inspect","inspect_1","inspect_2")),"all inspect variants reachable");
  int starts=a.starts;c.tick(0,false,true,0);check(a.starts==starts,"paused tick no retrigger");
  check(a.stops>0,"interruption stops old audio");c.close();check(c.actionName().isEmpty(),"close resets");
  Audio repeatedAudio=new Audio();var repeated=new MeleeAnimationController(clips,List.of("hand"),"",new Random(3),repeatedAudio);
  repeated.attack(false);int firstSecondary=repeatedAudio.starts;
  for(int i=0;i<8;i++){repeated.tick(.05f,false,true,0);repeated.attack(false);}
  check(repeatedAudio.starts==firstSecondary,"held secondary must not restart the active clip/audio");
  repeated.tick(.11f,false,true,0);check(repeated.actionName().isEmpty(),"secondary reaches completion while held");
  var continuityClips=BedrockAnimation.parse(JsonParser.parseString("""
  {"animations":{
   "static_idle":{"animation_length":1,"bones":{"hand":{"rotation":[0,350,0]}}},
   "inspect":{"animation_length":2,"bones":{"hand":{"rotation":[0,10,0]}}},
   "run":{"animation_length":1,"bones":{"hand":{"position":{"0":[0,0,0],"1":[1,0,0]}}}}
  }}
  """).getAsJsonObject());
  var continuity=new MeleeAnimationController(continuityClips,List.of("hand"),"",new Random(1),new Audio());
  continuity.inspect();continuity.tick(.1f,false,true,0);
  near((float)Math.cos(Math.toRadians(continuity.pose("hand").rotation()[1])),1,"transition crosses angular seam by short path");
  continuity.tick(1,false,true,0);continuity.tick(1,false,true,0);
  continuity.tick(.01f,true,true,0);continuity.tick(.3f,true,true,.6f);
  continuity.tick(.1f,true,false,.6f);float beforeLanding=continuity.pose("hand").position()[0];
  continuity.tick(.01f,true,true,.6f);
  near(continuity.pose("hand").position()[0],beforeLanding,"landing must not rewind running phase");
  System.out.println("PASS: layered pose, attack routing, all variants, sprint transitions, pause and cleanup");
 }
}
