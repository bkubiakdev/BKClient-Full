package dev.bkclient.ui.clicksys.port.concurrent;
import java.util.*;
import dev.bkclient.ui.clicksys.port.data.Waypoint;
import dev.bkclient.systems.modules.modules.visuals.Waypoints;
public final class WaypointStore {
 private static final WaypointStore INSTANCE=new WaypointStore();public static WaypointStore getInstance(){return INSTANCE;}
 private Waypoints owner(){return dev.bkclient.BKClientInit.getInstance().getModuleManager().getModule(Waypoints.class);}
 public static boolean check(){return !Waypoints.scope().isEmpty();}public static String getString2(){return Waypoints.scope();}
 public List<Waypoint> getList(){try{return owner().store().list(Waypoints.scope(),Waypoints.dimension()).stream().map(p->new Waypoint(p.id(),p.name(),(int)p.x(),(int)p.y(),(int)p.z(),p.scope(),p.visible(),"")).toList();}catch(Exception e){throw new IllegalStateException(e);}}
 private void edit(String id,java.util.function.UnaryOperator<dev.bkclient.systems.waypoints.WaypointStore.Point> fn){try{for(var p:owner().store().list(Waypoints.scope(),Waypoints.dimension()))if(p.id().equals(id)){owner().store().put(fn.apply(p));owner().refresh();return;}}catch(Exception e){throw new IllegalStateException(e);}}
 public void onStringString(String id,String name){edit(id,p->new dev.bkclient.systems.waypoints.WaypointStore.Point(p.id(),p.scope(),p.dimension(),name,p.x(),p.y(),p.z(),p.color(),p.visible()));}
 public void onStringIntIntInt(String id,int x,int y,int z){edit(id,p->new dev.bkclient.systems.waypoints.WaypointStore.Point(p.id(),p.scope(),p.dimension(),p.name(),x,y,z,p.color(),p.visible()));}
 public void onStringBoolean(String id,boolean visible){edit(id,p->p.visibility(visible));}
 public void removeString(String id){try{owner().store().remove(id);owner().refresh();}catch(Exception e){throw new IllegalStateException(e);}}
 public Waypoint getWaypointByStringIntIntIntString2(String name,int x,int y,int z,String server){try{String id=UUID.randomUUID().toString();owner().store().put(new dev.bkclient.systems.waypoints.WaypointStore.Point(id,Waypoints.scope(),Waypoints.dimension(),name,x,y,z,0xff86efac,true));owner().refresh();return new Waypoint(id,name,x,y,z,Waypoints.scope(),true,"");}catch(Exception e){throw new IllegalStateException(e);}}
}