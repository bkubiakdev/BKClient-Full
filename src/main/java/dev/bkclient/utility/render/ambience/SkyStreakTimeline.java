package dev.bkclient.utility.render.ambience;

import java.util.Arrays;

/** Finite world-space flights. Only a new flight adopts the camera's current area. */
public final class SkyStreakTimeline {
    public static final int LIMIT = 32;
    public static final double DX = .9278, DY = -.025, DZ = .3722337840658558;
    private final double[] origins = new double[LIMIT * 3];
    private final long[] cycles = new long[LIMIT];
    private double lastX, lastY, lastZ, lastTime;
    private boolean initialized;

    public SkyStreakTimeline() { reset(); }
    public void reset() { Arrays.fill(cycles, Long.MIN_VALUE); initialized = false; }
    private static double random(double seed) {
        double x = Math.sin(seed * 127.1 + 311.7) * 43758.5453;
        return x - Math.floor(x);
    }
    private static double smooth(double x) { x = Math.max(0, Math.min(1, x)); return x*x*(3-2*x); }

    /** Head/tail XYZ are camera-relative blocks; W holds opacity and head radius respectively. */
    public void sample(double seconds, int count, float length, float size,
                       double cameraX, double cameraY, double cameraZ, float[] heads, float[] tails) {
        seconds = Double.isFinite(seconds) ? Math.max(0, seconds) : 0;
        count = Math.max(0, Math.min(LIMIT, count));
        double dx = cameraX-lastX, dy = cameraY-lastY, dz = cameraZ-lastZ;
        if (initialized && (seconds < lastTime || dx*dx+dy*dy+dz*dz > 128*128)) reset();
        lastX=cameraX; lastY=cameraY; lastZ=cameraZ; lastTime=seconds; initialized=true;
        double trail = 1.5 + Math.max(10, Math.min(100, length)) * .12;
        for (int i=0; i<LIMIT; i++) {
            int p=i*4, o=i*3;
            if (i>=count) { heads[p+3]=0; cycles[i]=Long.MIN_VALUE; continue; }
            double period=8+random(i*17+1)*5;
            double clock=seconds/period+random(i*13+2), phase=clock-Math.floor(clock);
            long cycle=(long)Math.floor(clock);
            if (cycles[i]!=cycle) {
                cycles[i]=cycle;
                origins[o]=cameraX; origins[o+1]=cameraY; origins[o+2]=cameraZ;
            }
            double seed=i*231.7+cycle*31.9;
            // Stratified azimuth keeps the default count useful in every view direction.
            double az=(i*.61803398875+random(cycle*31.9+3)*.15)*Math.PI*2;
            double radius=12+random(seed+4)*32;
            double travel=(phase-.5)*48;
            double x=origins[o]+Math.cos(az)*radius+DX*travel-cameraX;
            double y=origins[o+1]+1+random(seed+5)*18+DY*travel-cameraY;
            double z=origins[o+2]+Math.sin(az)*radius+DZ*travel-cameraZ;
            double distance=Math.sqrt(x*x+y*y+z*z);
            heads[p]=(float)x; heads[p+1]=(float)y; heads[p+2]=(float)z;
            heads[p+3]=(float)(smooth(phase/.12)*(1-smooth((phase-.80)/.20))
                *smooth((distance-2)/5)*(1-smooth((distance-72)/24)));
            tails[p]=(float)(x-DX*trail); tails[p+1]=(float)(y-DY*trail); tails[p+2]=(float)(z-DZ*trail);
            tails[p+3]=(float)((.10+random(seed+8)*.07)*Math.max(.5,Math.min(2,size/100)));
        }
    }
}
