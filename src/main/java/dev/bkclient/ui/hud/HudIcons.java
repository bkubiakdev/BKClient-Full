package dev.bkclient.ui.hud;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.utility.colors.ColorRGBA;
import net.minecraft.util.Identifier;

/** Semantic interface icons only. Item sprites, covers, skins and emoji images stay untouched. */
public final class HudIcons {
 private HudIcons() {}
 /** Resolved icon per texture id (NONE = not a semantic icon). Render thread only; ids are a finite
  *  texture set, the bound only guards against dynamically named textures. */
 private static final Object NONE=new Object();
 private static final java.util.HashMap<Identifier,Object> CACHE=new java.util.HashMap<>();
 public static boolean draw(CustomDrawContext c,Identifier id,float x,float y,float w,float h,ColorRGBA color){
  if (!id.getNamespace().equals("bkclient")) return false;
  Object cached=CACHE.get(id);
  if(cached==null){
   LucideIcons resolved=resolve(id.getPath());
   if(CACHE.size()>=1024)CACHE.clear();
   CACHE.put(id,cached=resolved==null?NONE:resolved);
  }
  if(cached==NONE)return false;
  LucideIcons icon=(LucideIcons)cached;
  if(!HudVisualScope.icons())return true;
  c.draw(); float size=Math.min(w,h); icon.draw(c,x+(w-size)/2,y+(h-size)/2,size,color); return true;
 }
 private static LucideIcons resolve(String path){
  if (!(path.startsWith("icons/hud/") || path.startsWith("icons/music/") || path.startsWith("icons/category/") || path.startsWith("sf_symbold/chat/") || path.startsWith("sf_symbold/sf/") || path.equals("icons/check.png") || path.equals("icons/trash.png") || path.equals("icons/airplane.png"))) return null;
  String name=path.substring(path.lastIndexOf('/')+1).replace(".png", "");
  return switch(name){
   case "check" -> LucideIcons.CHECK;
   case "message" -> LucideIcons.PANEL;
   case "bps","stats","tps","lowtps" -> LucideIcons.GAUGE;
   case "cord" -> LucideIcons.CENTER;
   case "server" -> LucideIcons.GLOBE;
   case "clock","timer" -> LucideIcons.CLOCK;
   case "sandtimer" -> LucideIcons.HOURGLASS;
   case "keyboard","keybinds","keybinds_new" -> LucideIcons.KEYBOARD;
   case "key","key1" -> LucideIcons.KEY;
   case "armor" -> LucideIcons.SHIELD;
   case "hotbar" -> LucideIcons.PACKAGE;
   case "potion","effect" -> LucideIcons.FLASK;
   case "enchant" -> LucideIcons.SPARKLES;
   case "world" -> LucideIcons.GLOBE;
   case "staff" -> LucideIcons.USERS;
   case "player","who","whodark","whoglass" -> LucideIcons.USER;
   case "target" -> LucideIcons.CROSSHAIR;
   case "dock","island" -> LucideIcons.PANEL;
   case "copy","copy2" -> LucideIcons.COPY;
   case "buy" -> LucideIcons.BUY;
   case "sell" -> LucideIcons.SELL;
   case "trash" -> LucideIcons.TRASH;
   case "center" -> LucideIcons.CENTER;
   case "reset" -> LucideIcons.RESET;
   case "drag","movement" -> LucideIcons.MOVE;
   case "combat" -> LucideIcons.SWORDS;
   case "visuals" -> LucideIcons.EYE;
   case "other" -> LucideIcons.MORE;
   case "emoji" -> LucideIcons.SMILE;
   case "send" -> LucideIcons.SEND;
   case "airplane" -> LucideIcons.PLANE;
   case "no_image" -> LucideIcons.MUSIC;
   case "previous" -> LucideIcons.PREVIOUS;
   case "next" -> LucideIcons.NEXT;
   case "play" -> LucideIcons.PLAY;
   case "pause" -> LucideIcons.PAUSE;
   case "repeat" -> LucideIcons.REPEAT;
   case "repeat1" -> LucideIcons.REPEAT_ONE;
   case "text" -> LucideIcons.BOOK;
   default -> null;
  };
 }
}
