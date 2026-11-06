package dev.bkclient.utility.time;
public final class MiningCommandTimerTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        var timer=new MiningCommandTimer();timer.reset(1000," /cx ","/cx","");
        check(!timer.due(0,"/cx",60000,60999),"not before full minute");
        check(timer.due(0,"/cx",60000,61000),"due at minute even without any mining ticks");
        timer.sent(0,61000);
        check(!timer.due(0,"/cx",60000,61001),"no duplicate send");
        check(timer.due(1,"/cx",60000,64000),"same command in another row has independent timer");
        check(timer.due(0,"/cx",60000,600000),"long pause leaves command due");
        timer.sent(0,600000);
        check(!timer.due(0,"/repair",60000,601000),"editing starts a fresh interval");
        check(timer.due(0,"/repair",60000,661000),"edited command becomes due");
        check(!timer.due(2," ",60000,900000),"empty row never sends");
        check(!timer.due(0,"/repair",60000,500),"backward clock restarts safely");
        System.out.println("Mining command schedules: idle timing, exact minute, independent rows, edits and clock rollback passed");
    }
}
