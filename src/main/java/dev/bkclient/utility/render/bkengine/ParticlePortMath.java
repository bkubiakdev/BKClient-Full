package dev.bkclient.utility.render.bkengine;
import java.util.concurrent.ThreadLocalRandom;
public final class ParticlePortMath {
    public static double randomValue(double min,double max){return min+ThreadLocalRandom.current().nextDouble()*(max-min);}
    public static float randomValue(float min,float max){return min+ThreadLocalRandom.current().nextFloat()*(max-min);}
    public static int randomInt(int min,int max){return ThreadLocalRandom.current().nextInt(min,max+1);}
    public static double step(double value,double step){return Math.round(value/step)*step;}
}
