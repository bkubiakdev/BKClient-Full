package dev.bkclient.combatengine.api.utils.input;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
public final class AuraMovement {
    private AuraMovement() {}
    public static Vec2f correct(float viewYaw,float bodyYaw,float forward,float strafe,boolean discrete) {
        if (forward==0 && strafe==0) return Vec2f.ZERO;
        double delta=Math.toRadians(MathHelper.wrapDegrees(viewYaw-bodyYaw));
        float f=(float)(forward*Math.cos(delta)+strafe*Math.sin(delta));
        float s=(float)(strafe*Math.cos(delta)-forward*Math.sin(delta));
        if(!discrete) return new Vec2f(Math.abs(f)<1e-6f?0:f,Math.abs(s)<1e-6f?0:s);
        double angle=Math.atan2(-s,f),best=Double.MAX_VALUE;float bf=0,bs=0;
        for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++)if(a!=0||b!=0){
            double error=Math.abs(MathHelper.wrapDegrees(Math.toDegrees(angle-Math.atan2(-b,a))));
            if(error<best){best=error;bf=a;bs=b;}
        }
        return new Vec2f(bf,bs);
    }
}
