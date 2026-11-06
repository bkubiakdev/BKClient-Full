package dev.bkclient.combatengine;

import dev.bkclient.combatengine.client.modules.impl.combat.bkadaptive.BKAdaptiveRotations;
import dev.bkclient.combatengine.api.utils.rotate.Rotation;

public final class BKAdaptiveRotationsTest {
    private static final String[] MODES = {"Funtime","Smooth","Grim","SlothAC","HolyWorld","Bezier","NoRotate"};
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) {
        for (String mode : MODES) {
            BKAdaptiveRotations r = new BKAdaptiveRotations();
            Rotation equal = r.step(mode,new Rotation(10,20),new Rotation(10,20),false,false,false,2,0,900,3);
            check(equal.getYaw()==10 && equal.getPitch()==20, mode+" zero error");
            for(int i=0;i<500;i++) {
                Rotation out=r.step(mode,new Rotation(i*7-150, i%170-85),new Rotation(-i*11,85-i%170),
                    i%2==0,i%3==0,i%4==0,i,i, i%1200,3);
                check(Float.isFinite(out.getYaw()) && Float.isFinite(out.getPitch()),mode+" finite angles");
                check(Math.abs(out.getPitch())<=89,mode+" pitch limits");
            }
        }
        BKAdaptiveRotations bezier=new BKAdaptiveRotations();
        Rotation at=new Rotation(179,0), goal=new Rotation(-179,30);
        for(int i=0;i<4;i++) at=bezier.step("Bezier",at,goal,true,true,false,i,0,1000,3);
        check(Math.abs(at.getYaw()-181)<.001 && Math.abs(at.getPitch()-30)<.001,"Bezier completes and crosses yaw seam via shortest path");
        bezier.reset();
        at=new Rotation(-70,-20); goal=new Rotation(30,10);
        for(int i=0;i<4;i++) at=bezier.step("Bezier",at,goal,true,true,false,i,0,1000,3);
        check(Math.abs(at.getYaw()-30)<.001 && Math.abs(at.getPitch()-10)<.001,"Bezier reset forgets previous target");
        BKAdaptiveRotations funtime=new BKAdaptiveRotations();
        at=funtime.step("Funtime",new Rotation(0,0),new Rotation(60,30),true,true,false,0,0,10,3);
        check(Math.abs(at.getYaw()-60)<.001 && Math.abs(at.getPitch()-30)<.001,"Funtime locks on when hit is ready");
        System.out.println("Aura2 rotations: zero error, finite angles, pitch limits, Bezier continuity/reset/yaw seam, attack lock passed");
    }
}
