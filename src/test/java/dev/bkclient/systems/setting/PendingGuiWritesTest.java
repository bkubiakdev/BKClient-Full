package dev.bkclient.systems.setting;
import java.util.ArrayList;
public final class PendingGuiWritesTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        var pending = new PendingGuiWrites();
        var saved = new ArrayList<Integer>();
        for(int i=0;i<10000;i++) { int value=i; check(pending.defer("config",()->saved.add(value),true),"drag deferred"); }
        check(saved.isEmpty(),"continuous preview must not write");
        pending.defer("theme",()->saved.add(42),true);
        var released=pending.drain();
        check(released.size()==2,"one write per destination");
        released.forEach(Runnable::run);
        check(saved.equals(java.util.List.of(9999,42)),"latest state saved and independent targets preserved");
        check(pending.drain().isEmpty(),"release and close cannot duplicate writes");
        pending.defer("config",()->saved.add(-1),true);
        Runnable immediate=()->saved.add(7);
        if(!pending.defer("config",immediate,false))immediate.run();
        check(pending.drain().isEmpty() && saved.getLast()==7,"explicit release save supersedes stale pending save");
        pending.defer("theme",()->saved.add(8),true);
        pending.drain().forEach(Runnable::run);
        check(saved.getLast()==8,"pending edit can flush on close/focus loss/shutdown");
        var queueField=GuiSaveBatch.class.getDeclaredField("pending"); queueField.setAccessible(true);
        var liveQueue=(PendingGuiWrites)queueField.get(null);
        liveQueue.defer("shutdown",()->{
            if(!GuiSaveBatch.defer("shutdown",()->saved.add(9)))saved.add(9);
        },true);
        var flush=GuiSaveBatch.class.getDeclaredMethod("flush"); flush.setAccessible(true); flush.invoke(null);
        check(saved.getLast()==9 && liveQueue.drain().isEmpty(),"forced shutdown flush must execute rather than requeue callbacks");
        System.out.println("GUI persistence: 10000 edits coalesced, latest state, independent files, explicit release, close flush passed.");
    }
}
