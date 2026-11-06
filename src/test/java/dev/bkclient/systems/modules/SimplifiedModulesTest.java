package dev.bkclient.systems.modules;
import dev.bkclient.systems.modules.modules.combat.BackTrack;
import dev.bkclient.systems.modules.modules.visuals.Trails;
import dev.bkclient.systems.modules.modules.visuals.CustomSwords;
import dev.bkclient.systems.setting.settings.ModeSetting;
public final class SimplifiedModulesTest {
    static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    public static void main(String[] args) throws Exception {
        var utils=new dev.bkclient.systems.modules.modules.player.PlayerUtils();
        var drop=(dev.bkclient.systems.setting.settings.BooleanSetting)utils.getSettings().stream().filter(s->s.getName().equals("Drop everything")).findFirst().orElseThrow();
        check(!drop.isEnabled()&&!utils.showDropEverything(),"drop button hidden by default");
        drop.setEnabled(true);
        check(!utils.showDropEverything(),"disabled Player Utils cannot show drop button");
        var enabledField=dev.bkclient.systems.modules.impl.BaseModule.class.getDeclaredField("enabled");
        enabledField.setAccessible(true);enabledField.setBoolean(utils,true);
        check(utils.showDropEverything(),"enabled module and opted-in setting show button");
        var savedDrop=drop.save();drop.setEnabled(false);
        check(!utils.showDropEverything(),"switch hides button even when module enabled");
        drop.load(savedDrop);check(utils.showDropEverything(),"drop preference survives serialization");
        check(dev.bkclient.systems.modules.modules.player.Blink.holdMillisFor(50)==50,"minimum hold matches slider");
        check(dev.bkclient.systems.modules.modules.player.Blink.holdMillisFor(200)==200,"default hold matches slider");
        check(dev.bkclient.systems.modules.modules.player.Blink.holdMillisFor(400)==400,"maximum hold matches slider");
        for (Class<?> type:java.util.List.of(net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.class,
                net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket.class, net.minecraft.network.packet.c2s.play.HandSwingC2SPacket.class,
                net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket.class, net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket.class,
                net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket.class))
            check(dev.bkclient.systems.modules.modules.player.Blink.isReleaseOnInteractPacket(type),"Nemesis interaction releases queue");
        check(!dev.bkclient.systems.modules.modules.player.Blink.isReleaseOnInteractPacket(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class),"movement remains queued");
        check(dev.bkclient.systems.modules.modules.player.Blink.isTimingReply(net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket.class),"preserve keepalive across correction");
        check(dev.bkclient.systems.modules.modules.player.Blink.isTimingReply(net.minecraft.network.packet.c2s.common.CommonPongC2SPacket.class),"preserve timing reply across correction");
        check(!dev.bkclient.systems.modules.modules.player.Blink.isTimingReply(net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.class),"discard stale movement on correction");
        var classic=new dev.bkclient.utility.network.FakeLagBatch<String>(8);
        classic.add("move1",0,200);classic.add("timing-reply",100_000_000,200);classic.add("move2",199_000_000,200);
        check(classic.drainBatchDue(199_999_999).isEmpty(),"Classic holds the whole batch until deadline");
        check(classic.drainBatchDue(200_000_000).equals(java.util.List.of("move1","timing-reply","move2")),"Classic releases all packets together, including newest, in order");
        check(classic.drainBatchDue(200_000_000).isEmpty(),"no duplicate release");
        classic.add("next-cycle",210_000_000,200);
        check(classic.drainBatchDue(409_999_999).isEmpty(),"new cycle starts with first new packet");
        check(classic.drainBatchDue(410_000_000).equals(java.util.List.of("next-cycle")),"second pulse repeats delay");
        System.out.println("FakeLag Classic: full FIFO pulses, repeated cycle and six Nemesis interaction boundaries passed");
        var batch=new dev.bkclient.utility.network.FakeLagBatch<String>(3);
        batch.add("move",0,200);batch.add("attack",100_000_000,200);
        check(batch.drainDue(199_999_999).isEmpty(),"no early sends");
        check(batch.drainDue(200_000_000).equals(java.util.List.of("move")),"later packets are not released with first packet");
        check(batch.drainDue(300_000_000).equals(java.util.List.of("attack")),"own deadline respected");
        batch.add("slow",400_000_000,400);batch.add("fast",450_000_000,50);
        check(batch.drainDue(500_000_000).isEmpty(),"slider decrease cannot reorder packets");
        check(batch.drainDue(800_000_000).equals(java.util.List.of("slow","fast")),"movement and actions retain FIFO order");
        check(batch.isEmpty()&&batch.remainingFraction(800_000_000)==0,"drain resets Island progress");
        batch.add("new-session",500_000_000,400);batch.clear();
        check(batch.drain().isEmpty(),"reset discards old session/correction packets");
        batch.add("a",0,200);batch.add("b",0,200);batch.add("c",0,200);
        check(batch.isFull(),"bounded batch");
        check(batch.drain().size()==3,"disable flushes entire batch, not just latest movement");
        dev.bkclient.ui.mainmenu.MenuPreferences.load(null);
        check(dev.bkclient.ui.mainmenu.MenuPreferences.Option.DATE.enabled,"old configs keep menu defaults");
        dev.bkclient.ui.mainmenu.MenuPreferences.Option.DATE.enabled=false;
        dev.bkclient.ui.mainmenu.MenuPreferences.Option.MUSIC.enabled=false;
        var menuSaved=dev.bkclient.ui.mainmenu.MenuPreferences.save();menuSaved.addProperty("futureOption",17);
        dev.bkclient.ui.mainmenu.MenuPreferences.load(menuSaved);
        check(!dev.bkclient.ui.mainmenu.MenuPreferences.Option.DATE.enabled&&!dev.bkclient.ui.mainmenu.MenuPreferences.Option.MUSIC.enabled,"menu preferences round trip");
        check(dev.bkclient.ui.mainmenu.MenuPreferences.Option.CLOCK.enabled,"unrelated menu option retained");
        check(dev.bkclient.ui.mainmenu.MenuPreferences.save().get("futureOption").getAsInt()==17,"unknown preference preserved");
        check(dev.bkclient.ui.mainmenu.MenuPreferences.Option.SERVER_CAROUSEL.enabled&&!dev.bkclient.ui.mainmenu.MenuPreferences.Option.WORLD_CAROUSEL.enabled,"server carousel defaults on and world carousel defaults off");
        dev.bkclient.ui.mainmenu.MenuPreferences.Option.SERVER_CAROUSEL.enabled=false;
        dev.bkclient.ui.mainmenu.MenuPreferences.Option.WORLD_CAROUSEL.enabled=true;
        dev.bkclient.ui.mainmenu.MenuPreferences.load(dev.bkclient.ui.mainmenu.MenuPreferences.save());
        check(!dev.bkclient.ui.mainmenu.MenuPreferences.Option.SERVER_CAROUSEL.enabled&&dev.bkclient.ui.mainmenu.MenuPreferences.Option.WORLD_CAROUSEL.enabled,"carousel switches persist independently");
        var back=new BackTrack();
        ModeSetting mode=(ModeSetting)back.getSettings().stream().filter(s->s.getName().equals("Mode")).findFirst().orElseThrow();
        check(mode.getValues().size()==2&&mode.getValue().getName().equals("Distance"),"BackTrack modes");
        check(mode.getValues().stream().anyMatch(v->v.getName().equals("Hit")),"Hit available");
        check(back.getSettings().stream().anyMatch(s->s.getName().equals("Range")),"Range available");
        check(back.getSettings().stream().noneMatch(s->s.getName().equals("Delay")||s.getName().equals("Nemesis Range")),"target-only settings gone");
        var trails=new Trails();check(trails.style.getValues().size()==1&&trails.style.getValue()==trails.sDash,"Dash only");
        check(trails.getSettings().stream().noneMatch(s->s.getName().equals("color.source.ribbon")||s.getName().endsWith(".color2")),"Ribbon controls gone");
        var swords=new CustomSwords();check(swords.weapon.getValues().size()>100,"weapon catalog retained");
        System.out.println("BackTrack, Trails and CustomSwords setting contracts passed");
    }
}
