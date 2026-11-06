package dev.bkclient.compat;

import dev.bkclient.utility.render.ambience.SkyStreakTimeline;
import java.util.Arrays;

public final class SkyStreakTimelineTest {
    private static int checks;
    private static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;}
    private static void near(double a,double b,String text){check(Math.abs(a-b)<.00003,text);}
    public static void main(String[] args) {
        float[] h=new float[128],t=new float[128],before=new float[128],scratch=new float[128];
        var timeline=new SkyStreakTimeline();
        for(int fps:new int[]{20,60,144}) {
            timeline.reset();
            for(int frame=0;frame<fps*30;frame++) {
                timeline.sample((double)frame/fps,32,60,100,0,100,0,h,t);
                for(int p=0;p<128;p+=4) {
                    check(h[p+3]>=0&&h[p+3]<=1,"bounded opacity");
                    for(int c=0;c<4;c++) check(Float.isFinite(h[p+c])&&Float.isFinite(t[p+c]),"finite geometry");
                    check(Math.abs(h[p+1]-t[p+1])<Math.abs(h[p]-t[p])*.05,"flight along world, not vertical rain");
                    near(h[p]-t[p],SkyStreakTimeline.DX*8.7,"constant world trail length");
                }
            }
            timeline.sample(12.75,32,60,100,0,100,0,h,t);
            if(fps==20)before=h.clone(); else check(Arrays.equals(h,before),"FPS-independent flights");
        }
        // Camera translation must change relative coordinates, never pull a live particle along.
        timeline.reset();timeline.sample(2,32,60,100,0,100,0,before,scratch);
        timeline.sample(2,32,60,100,7,103,-5,h,t);
        for(int p=0;p<128;p+=4){near(h[p]+7,before[p],"world X anchored");near(h[p+1]+3,before[p+1],"world Y anchored");near(h[p+2]-5,before[p+2],"world Z anchored");}
        timeline.sample(2,32,10,50,7,103,-5,h,t);
        timeline.sample(2,32,100,200,7,103,-5,before,scratch);
        check(Arrays.equals(h,before),"size/length preserve head positions and clock");
        for(int p=0;p<128;p+=4){near(scratch[p+3],t[p+3]*4,"size multiplier");check(before[p]-scratch[p]>h[p]-t[p],"length slider extends trailing end");}
        // Reseeding is hidden, and each visible flight has a constant shared direction.
        timeline.reset();
        for(double time=0;time<30;time+=.01) {
            timeline.sample(time,32,60,100,0,100,0,before,scratch);
            timeline.sample(time+.001,32,60,100,0,100,0,h,t);
            for(int p=0;p<128;p+=4){double dx=h[p]-before[p],dy=h[p+1]-before[p+1],dz=h[p+2]-before[p+2];
                if(Math.abs(dx)+Math.abs(dy)+Math.abs(dz)>1)check(h[p+3]<.001&&before[p+3]<.001,"invisible respawn");
                else {check(dx>=0&&dz>=0,"shared direction");near(dy,dx*SkyStreakTimeline.DY/SkyStreakTimeline.DX,"flight slope");}}
        }
        timeline.reset();timeline.sample(2,32,60,100,29_999_900,100,-29_999_900,h,t);
        new SkyStreakTimeline().sample(2,32,60,100,0,100,0,before,scratch);
        for(int p=0;p<128;p++)near(h[p],before[p],"precision at world border");
        timeline.sample(2,32,60,100,0,100,0,h,t); // Teleport resets anchors.
        for(int p=0;p<128;p++)near(h[p],before[p],"teleport clears previous area");
        timeline.sample(2,0,60,100,0,100,0,h,t);for(int p=3;p<128;p+=4)check(h[p]==0,"zero count clears all slots");
        timeline.sample(Double.NaN,100,60,100,0,100,0,h,t);for(float v:h)check(Float.isFinite(v),"invalid time/count safe");
        System.out.println("SkyStreakTimelineTest PASS "+checks+": world anchors, parallax, horizontal flight, FPS, controls, reseed and teleport");
    }
}
