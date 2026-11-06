package dev.bkclient.combatengine;
import dev.bkclient.combatengine.api.utils.input.AuraMovement;
import dev.bkclient.combatengine.client.modules.impl.combat.bkadaptive.MovementInputState;
import net.minecraft.util.math.*;
import net.minecraft.util.PlayerInput;
public final class AuraMovementTest {
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 private static Vec2f world(float yaw,float f,float s){double a=Math.toRadians(yaw);return new Vec2f((float)(s*Math.cos(a)-f*Math.sin(a)),(float)(f*Math.cos(a)+s*Math.sin(a)));}
 public static void main(String[] args){
  int samples=0;
  for(int view=-180;view<180;view+=3)for(int body=-180;body<180;body+=3)for(int f=-1;f<=1;f++)for(int s=-1;s<=1;s++){
   var exact=AuraMovement.correct(view,body,f,s,false);var desired=world(view,f,s);var actual=world(body,exact.x,exact.y);
   check(Math.hypot(desired.x-actual.x,desired.y-actual.y)<1e-5,"world direction preserved");
   check(Math.abs(exact.x*exact.x+exact.y*exact.y-f*f-s*s)<1e-5,"input magnitude preserved");
   var keys=AuraMovement.correct(view,body,f,s,true);
   if(f==0&&s==0)check(keys.x==0&&keys.y==0,"idle remains idle");
   else {var discrete=world(body,keys.x,keys.y);double angle=Math.toDegrees(Math.atan2(desired.y,desired.x)-Math.atan2(discrete.y,discrete.x));check(Math.abs(MathHelper.wrapDegrees(angle))<=22.5001,"nearest keyboard direction including seam");}
   samples++;
  }
  var seam=AuraMovement.correct(-180,-136,1,0,true);check(seam.x==1&&seam.y==1,"reported yaw seam regression");
  var correction=AuraMovement.correct(10,0,1,0,false);
  var keys=new PlayerInput(true,false,false,true,true,false,false);
  var state=MovementInputState.analog(keys,correction.x,correction.y);
  for(int tick=0;tick<8;tick++){state.update();check(state.movementForward==correction.x&&state.movementSideways==correction.y,"prediction keeps analog input");check(state.playerInput.jump(),"prediction preserves jump");}
  System.out.println("Aura movement: "+samples+" direction/magnitude/seam cases and analog prediction passed");
 }
}
