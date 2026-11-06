package dev.bkclient.combatengine;
import dev.bkclient.combatengine.bkengine.CriticalWindow;
public final class CriticalWindowTest {
 public static void main(String[] args) {
  if(CriticalWindow.charged(.8F)||CriticalWindow.charged(.9F)||!CriticalWindow.charged(.95F)) throw new AssertionError("Vanilla critical cooldown");
  if(!CriticalWindow.falling(false,.1F,-.08,false)) throw new AssertionError("Descending jump");
  if(CriticalWindow.falling(true,.1F,-.08,false)||CriticalWindow.falling(false,0,-.08,false)||CriticalWindow.falling(false,.1F,.08,false)||CriticalWindow.falling(false,.1F,-.08,true)) throw new AssertionError("Invalid critical state");
  System.out.println("Critical window: cooldown, falling, ground, ascent and riding passed");
 }
}
