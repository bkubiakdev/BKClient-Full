package dev.bkclient.utility.render.bkengine;
import dev.bkclient.utility.colors.Colors;
public final class ParticlePortColor {
    public static int getColor(int r,int g,int b){return 0xff000000|(r<<16)|(g<<8)|b;}
    public static int replAlpha(int c,int a){return (c&0xffffff)|(Math.clamp(a,0,255)<<24);}
    public static int fade(int index){
        var c=Colors.getAccent();int angle=(int)((System.currentTimeMillis()/10+index)%360);
        angle=angle>=180?360-angle:angle;float f=1-.25F*angle/180;
        return getColor((int)(c.getRed()*f),(int)(c.getGreen()*f),(int)(c.getBlue()*f));
    }
}
