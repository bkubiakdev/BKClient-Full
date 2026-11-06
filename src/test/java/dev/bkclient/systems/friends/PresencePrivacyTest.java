package dev.bkclient.systems.friends;
import com.google.gson.JsonParser;
public final class PresencePrivacyTest {
 private static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  PresencePrivacy.load(null);
  check(PresencePrivacy.isEnabled()&&!PresencePrivacy.isHidden());
  check(PresencePrivacy.publishedServer(()->"example.test").equals("example.test"));
  PresencePrivacy.load(JsonParser.parseString("{\"enabled\":false,\"shareServer\":false}"));
  check(PresencePrivacy.isEnabled()&&PresencePrivacy.isHidden());
  long revision=PresencePrivacy.revision();
  PresencePrivacy.setHidden(true);check(!PresencePrivacy.allows(revision));
  check(PresencePrivacy.publishedServer(()->{throw new AssertionError();}).equals("Hidden"));
  PresencePrivacy.setEnabled(false);
  var saved=PresencePrivacy.save();PresencePrivacy.load(null);PresencePrivacy.load(saved);
  check(!PresencePrivacy.isEnabled()&&PresencePrivacy.isHidden());
  PresencePrivacy.setEnabled(true);PresencePrivacy.setHidden(false);
  saved=PresencePrivacy.save();PresencePrivacy.load(saved);
  check(PresencePrivacy.isEnabled()&&!PresencePrivacy.isHidden());
  System.out.println("Legacy Friends defaults, v2 migration, saved controls and stale request guard passed");
 }
}
