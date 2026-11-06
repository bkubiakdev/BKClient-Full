package dev.bkclient.ui.clicksys.port.api;

import dev.bkclient.ui.clicksys.port.data.Waypoint;

public interface ConfigChangeCallback {
   public void onChange(Waypoint waypoint, int count, int count2, int count3);
}
