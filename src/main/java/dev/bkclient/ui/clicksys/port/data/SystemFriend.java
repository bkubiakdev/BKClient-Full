package dev.bkclient.ui.clicksys.port.data;
import java.util.*;
import dev.bkclient.BKClientInit;
public final class SystemFriend {
 private static final SystemFriend INSTANCE=new SystemFriend();
 private final FriendLabels labels;
 private SystemFriend(){
  var path=dev.bkclient.systems.file.FileManager.DIRECTORY.toPath().resolve("clicksys-friends.json");
  FriendLabels loaded=null;
  try {loaded=new FriendLabels(path);} catch(Exception e){BKClientInit.LOGGER.warn("Cannot load friend labels; keeping file intact",e);}
  labels=loaded;
 }
 public static SystemFriend getInstance(){return INSTANCE;}
 public Set<String> getSet(){return new LinkedHashSet<>(BKClientInit.getInstance().getFriendManager().listFriends());}
 public void addString2(String name){
  if(name==null||name.isBlank())return;name=name.trim();
  var manager=BKClientInit.getInstance().getFriendManager();boolean existed=manager.isFriend(name);manager.add(name);
  if(!existed&&manager.isFriend(name)) {if(labels!=null)labels.added(name,dev.bkclient.ui.clicksys.port.util.DateUtil.getString());BKClientInit.getInstance().getFileManager().writeFile("client");save();}
 }
 public void removeString(String name){BKClientInit.getInstance().getFriendManager().remove(name);if(labels!=null)labels.remove(name);save();}
 public String getStringByString(String name){return labels==null?"Friend":labels.group(name);}
 public String getStringByString2(String name){return labels==null?"":labels.date(name);}
 public String getStringByString4(String name){return labels==null?null:labels.alias(name);}
 public void onStringString3(String name,String value){if(labels==null||!exists(name))return;labels.group(name,value);save();}
 public void onStringString5(String name,String value){if(labels==null||!exists(name))return;labels.alias(name,value);save();}
 private boolean exists(String name){return BKClientInit.getInstance().getFriendManager().isFriend(name);}
 private void save(){try{if(labels!=null)labels.save();}catch(Exception e){BKClientInit.LOGGER.warn("Could not save friend labels",e);}}
}
