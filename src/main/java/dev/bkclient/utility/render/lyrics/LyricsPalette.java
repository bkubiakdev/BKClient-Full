package dev.bkclient.utility.render.lyrics;

public record LyricsPalette(int first,int second,int third,int stops,float angle,double phase,boolean flowing) {
    public int at(float x,float y,float width,float height,float alpha) {
        double rad=Math.toRadians(angle),c=Math.cos(rad),s=Math.sin(rad);
        double span=Math.max(1e-5,Math.abs(c)*width+Math.abs(s)*height);
        double p=Math.clamp(.5+(x*c+y*s)/span,0,1);
        if(flowing) p=.5-.5*Math.cos(2*Math.PI*(p*.5+phase));
        int rgb=stops<=1?first:stops==2?mix(first,second,p):p<.5?mix(first,second,p*2):mix(second,third,(p-.5)*2);
        return (Math.clamp(Math.round((rgb>>>24)*alpha),0,255)<<24)|(rgb&0xffffff);
    }
    private static int mix(int a,int b,double t) {
        int result=0;
        for(int shift=0;shift<=24;shift+=8) result|=((int)Math.round(((a>>>shift)&255)*(1-t)+((b>>>shift)&255)*t))<<shift;
        return result;
    }
}
