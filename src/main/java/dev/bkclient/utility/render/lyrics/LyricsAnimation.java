package dev.bkclient.utility.render.lyrics;

/** Pure, bounded transforms driven by elapsed time, never by frame count. */
public final class LyricsAnimation {
    private LyricsAnimation() { }
    public record Effect(String name, float milliseconds) { }
    /** {@code shatter} is the progress (0..1) of the per-glyph Disintegrate exit; the renderer staggers it by glyph. */
    public record Transform(float alpha, float scale, float y, float flip, float roll, float shatter) {
        public static final Transform IDENTITY=new Transform(1,1,0,0,0);
        public Transform(float alpha, float scale, float y, float flip, float roll) { this(alpha,scale,y,flip,roll,0); }
    }
    public static float ease(float t) { t=Math.clamp(t,0,1); return 1-(1-t)*(1-t)*(1-t); }
    public static float duration(java.util.List<Effect> effects) {
        float duration=1;
        for(var effect:effects)duration=Math.max(duration,effect.milliseconds);
        return duration;
    }
    /** All selected effects start together; short cues shorten their times proportionally. */
    public static Transform stack(String stage,java.util.List<Effect> effects,float elapsedMs,float budgetMs,float strength) {
        if(effects.isEmpty())return stage.equals("exit")?exit("none",elapsedMs/Math.max(1,budgetMs),strength):Transform.IDENTITY;
        float duration=duration(effects),compression=Math.min(1,Math.max(1,budgetMs)/duration);
        boolean explicitFade=effects.stream().anyMatch(e->e.name.equals("fade"));
        float alpha=1,scale=1,y=0,flip=0,roll=0,shatter=0;
        for(var effect:effects) {
            float t=Math.clamp(elapsedMs/Math.max(1,effect.milliseconds*compression),0,1);
            Transform transform=switch(stage) {
                case "exit" -> exit(effect.name,t,strength);
                case "word" -> word(effect.name,t,strength);
                default -> enter(effect.name,t,strength);
            };
            // An explicitly selected Fade owns opacity; geometry effects keep their own clocks.
            if(!explicitFade||effect.name.equals("fade"))alpha=Math.min(alpha,transform.alpha);
            scale*=transform.scale;y+=transform.y;flip+=transform.flip;roll+=transform.roll;shatter=Math.max(shatter,transform.shatter);
        }
        return new Transform(alpha,Math.clamp(scale,.01f,4),y,flip,roll,shatter);
    }
    public static Transform word(String mode,float progress,float strength) {
        if(!mode.equals("pop"))return enter(mode,progress,strength);
        // Keep the original word entrance (108% -> 100%) for old profiles.
        float e=ease(progress),k=Math.clamp(strength,0,2);
        return new Transform(.35f+.65f*e,1+.08f*(1-e)*k,2.5f*(1-e)*k,0,0);
    }
    public static Transform enter(String mode,float progress,float strength) {
        float t=Math.clamp(progress,0,1), e=ease(t), k=Math.clamp(strength,0,2);
        return switch(mode) {
            case "none" -> Transform.IDENTITY;
            case "slide" -> new Transform(e,1,8*(1-e)*k,0,0);
            case "pop" -> { float q=t-1; float back=1+2.70158f*q*q*q+1.70158f*q*q; yield new Transform(e,Math.max(.1f,1+.25f*(back-1)*k),2.5f*(1-e)*k,0,0); }
            case "zoom" -> new Transform(e,Math.max(.1f,1-.6f*(1-e)*k),0,0,0);
            case "flip" -> new Transform(e,1,0,(float)Math.toRadians(75)*(1-e)*k,0);
            case "wave" -> new Transform(e,1,(float)Math.sin(t*Math.PI*2)*3*(1-t)*k,0,(float)Math.sin(t*Math.PI)*.12f*k);
            default -> new Transform(e,1,0,0,0);
        };
    }
    public static Transform exit(String mode,float progress,float strength) {
        float e=ease(progress),k=Math.clamp(strength,0,2);
        return switch(mode) {
            case "none" -> new Transform(progress>=1?0:1,1,0,0,0);
            case "float" -> new Transform(1-e,1,-12*e*k,0,0);
            case "shrink" -> new Transform(1-e,Math.max(.1f,1-.6f*e*k),0,0,0);
            // Glyphs fly apart and fade one after another; the renderer fades each glyph, so the verse keeps alpha 1.
            case "disintegrate" -> new Transform(1,1,0,0,0,Math.clamp(progress,0,1));
            default -> new Transform(1-e,1,0,0,0);
        };
    }
}
