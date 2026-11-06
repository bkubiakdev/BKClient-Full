package dev.bkclient.utility.time;
/** Independent wall-clock schedules for MineHelper's three command rows. */
public final class MiningCommandTimer {
    private final String[] commands = {"", "", ""};
    private final long[] lastRun = new long[3];
    public void reset(long now, String... configured) {
        for(int i=0;i<3;i++){commands[i]=configured[i].strip();lastRun[i]=now;}
    }
    public boolean due(int slot,String text,long interval,long now) {
        text=text.strip();
        if(!text.equals(commands[slot])||now<lastRun[slot]){commands[slot]=text;lastRun[slot]=now;return false;}
        return !text.isEmpty()&&interval>0&&now-lastRun[slot]>=interval;
    }
    public void sent(int slot,long now){lastRun[slot]=now;}
}
