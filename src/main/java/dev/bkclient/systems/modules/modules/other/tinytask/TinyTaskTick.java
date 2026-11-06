package dev.bkclient.systems.modules.modules.other.tinytask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TinyTaskTick {
    public long tickIndex;
    public boolean forward;
    public boolean back;
    public boolean left;
    public boolean right;
    public boolean jump;
    public boolean sneak;
    public boolean sprint;
    public boolean attack;
    public boolean use;
    public float yaw;
    public float pitch;
    public int selectedSlot;
    public List<String> pressedKeys = new ArrayList<>();
    public Map<String, Integer> timesPressed = new HashMap<>();
    public List<String> modifiers = new ArrayList<>();
}
