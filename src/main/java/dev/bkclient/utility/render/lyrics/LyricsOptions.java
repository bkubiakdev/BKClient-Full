package dev.bkclient.utility.render.lyrics;

import com.google.gson.JsonObject;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.utility.colors.ColorRGBA;
import java.util.function.BooleanSupplier;

/** Existing setting keys remain stable; sections affect presentation only. */
public final class LyricsOptions {
    private static final String K="modules.settings.lyrics_3d.";
    private final BaseModule parent;
    private final JsonObject defaults=new JsonObject();
    public final ModeSetting section,font,colors,align,display,entrance,exit,wordAnimation;
    public final SelectSetting entryEffects,exitEffects,wordEffects;
    public final java.util.Map<String,SliderSetting> entryTimes=new java.util.LinkedHashMap<>(),exitTimes=new java.util.LinkedHashMap<>(),wordTimes=new java.util.LinkedHashMap<>();
    public final ColorSetting color,color2,color3,outlineColor,glowColor,cometColor;
    public final SliderSetting distance,height,size,opacity,offset,animation,exitTime,strength,wordTime,revealPercent,wordGap;
    public final SliderSetting wrap,tracking,leading,angle,period,outlineWidth,glowRadius,glowPower,smoothing;
    public final SliderSetting historyCount,historyTime,historyAlpha,historyOffset;
    public final SliderSetting scatterSpread,scatterDepth,scatterTilt,scatterArc,scatterWords,emphasisSize,cometCount,cometSize;
    public final BooleanSetting shadow,throughWalls,approximate,preview,follow,face,flow,outline,glow,glowInherit,mixEffects,emphasis,comets;
    public LyricsOptions(BaseModule parent,Runnable restart) {
        this.parent=parent;
        section=mode("section",()->false,"appearance","movement","animations","history","synchronization");
        new ButtonSetting(parent,K+"classic").action(()->{preset(false);dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();});
        new ButtonSetting(parent,K+"neon").action(()->{preset(true);dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();});
        new ButtonSetting(parent,K+"preset_scattered").action(()->{preset("scattered");dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();});
        preview=bool("preview",()->false,false);
        new ButtonSetting(parent,K+"replay",()->!preview.isEnabled()).action(restart);
        font=mode("font",hide("appearance"),"minecraft","regular","medium","semibold","bold","roundbold");
        colors=mode("color_mode",hide("appearance"),"solid","gradient2","gradient3","theme");
        color=new ColorSetting(parent,"color",()->hidden("appearance")||is(colors,"theme")).color(ColorRGBA.WHITE);
        color2=new ColorSetting(parent,K+"color2",()->hidden("appearance")||!secondaryColorUsed()).color(new ColorRGBA(236,132,255));
        color3=new ColorSetting(parent,K+"color3",()->hidden("appearance")||!is(colors,"gradient3")).color(new ColorRGBA(128,220,255));
        angle=slider("gradient_angle",()->hidden("appearance")||!gradient(),0,360,5,0,"°");
        flow=bool("gradient_flow",()->hidden("appearance")||!gradient(),false);
        period=slider("gradient_period",()->hidden("appearance")||!gradient()||!flow.isEnabled(),1,20,.5f,5," s");
        size=slider("size",hide("appearance"),50,200,5,100,"%");
        opacity=slider("opacity",hide("appearance"),10,100,5,100,"%");
        wrap=slider("wrap",hide("appearance"),80,600,10,280," px");
        tracking=slider("tracking",hide("appearance"),-.5f,3,.1f,0," px");
        leading=slider("leading",hide("appearance"),0,12,.5f,1," px");
        align=mode("alignment",hide("appearance"),"center","left","right");
        shadow=bool("shadow",hide("appearance"),true);
        outline=bool("outline",hide("appearance"),false);
        outlineColor=new ColorSetting(parent,K+"outline_color",()->hidden("appearance")||!outline.isEnabled()).color(new ColorRGBA(25,12,42));
        outlineWidth=slider("outline_width",()->hidden("appearance")||!outline.isEnabled(),.1f,1.5f,.1f,.5f," px");
        glow=bool("glow",hide("appearance"),false);
        glowInherit=bool("glow_inherit",()->hidden("appearance")||!glow.isEnabled(),true);
        glowColor=new ColorSetting(parent,K+"glow_color",()->hidden("appearance")||!glow.isEnabled()||glowInherit.isEnabled()).color(new ColorRGBA(187,126,255));
        glowRadius=slider("glow_radius",()->hidden("appearance")||!glow.isEnabled(),.5f,3,.1f,1.5f," px");
        glowPower=slider("glow_power",()->hidden("appearance")||!glow.isEnabled(),0,100,5,35,"%");
        // Comets: short bright streaks crossing the sky in front of the player while the module is on.
        comets=bool("comets",hide("appearance"),false);
        cometCount=slider("comet_count",()->hidden("appearance")||!comets.isEnabled(),1,LyricsComets.LIMIT,1,8,"");
        cometSize=slider("comet_size",()->hidden("appearance")||!comets.isEnabled(),50,200,5,100,"%");
        cometColor=new ColorSetting(parent,K+"comet_color",()->hidden("appearance")||!comets.isEnabled()).color(new ColorRGBA(222,64,255));
        distance=slider("distance",hide("movement"),2,14,.5f,6," m");
        height=slider("height",hide("movement"),-2,4,.1f,.6f," m");
        follow=bool("follow_position",hide("movement"),false);
        face=bool("face_player",hide("movement"),true);
        smoothing=slider("follow_smoothing",()->hidden("movement")||!follow.isEnabled(),0,500,10,90," ms");
        throughWalls=bool("through_walls",hide("movement"),false);
        display=mode("display_mode",hide("animations"),"full_verse","word_by_word","scattered");
        // Scattered: each short piece of the line gets its own spot around the view (spread), distance jitter (depth)
        // and tilt; at most two pieces stay on screen.
        scatterSpread=slider("scatter_spread",hide("animations",true),20,250,5,100,"%");
        scatterDepth=slider("scatter_depth",hide("animations",true),0,3,.1f,.8f," m");
        scatterTilt=slider("scatter_tilt",hide("animations",true),0,45,1,18,"°");
        // Kept for saved configurations; pieces are flat since they are placed one by one.
        scatterArc=slider("scatter_arc",()->true,0,100,5,35,"%");
        scatterWords=slider("scatter_words",hide("animations",true),1,4,1,2,"");
        emphasis=bool("emphasis",hide("animations",true),true);
        emphasisSize=slider("emphasis_size",()->hidden("animations")||!scattered()||!emphasis.isEnabled(),100,250,5,170,"%");
        mixEffects=bool("mix_effects",hide("animations"),false);
        entrance=mode("entrance",()->hidden("animations")||mixEffects.isEnabled(),"fade","none","slide","pop","zoom","flip");
        exit=mode("exit",()->hidden("animations")||mixEffects.isEnabled(),"fade","none","float","shrink","disintegrate");
        wordAnimation=mode("word_animation",()->hidden("animations")||mixEffects.isEnabled()||is(display,"full_verse"),"pop","none","fade","slide","wave");
        animation=slider("animation",()->hidden("animations")||mixEffects.isEnabled(),100,1400,50,700," ms");
        exitTime=slider("exit_time",()->hidden("animations")||mixEffects.isEnabled(),100,1400,50,300," ms");
        wordTime=slider("word_time",()->hidden("animations")||mixEffects.isEnabled()||is(display,"full_verse"),50,500,10,160," ms");
        entryEffects=effects("entry_effects",false,"fade","slide","pop","zoom","flip");
        effectTimes("entry",entryEffects,entryTimes,.7f,false);
        exitEffects=effects("exit_effects",false,"fade","float","shrink","disintegrate");
        effectTimes("exit",exitEffects,exitTimes,.3f,false);
        wordEffects=effects("word_effects",true,"pop","fade","slide","wave");
        effectTimes("word",wordEffects,wordTimes,.16f,true);
        strength=slider("strength",hide("animations"),0,200,5,100,"%");
        revealPercent=slider("reveal_percent",()->hidden("animations")||is(display,"full_verse"),20,100,5,45,"%");
        wordGap=slider("word_gap",()->hidden("animations")||is(display,"full_verse"),100,650,50,650," ms");
        historyCount=slider("history_count",hide("history"),0,3,1,0,"");
        historyTime=slider("history_time",()->hidden("history")||historyCount.getCurrentValue()==0,.5f,8,.5f,3," s");
        historyAlpha=slider("history_alpha",()->hidden("history")||historyCount.getCurrentValue()==0,10,100,5,45,"%");
        historyOffset=slider("history_offset",()->hidden("history")||historyCount.getCurrentValue()==0,0,2,.1f,.5f," m");
        offset=slider("offset",hide("synchronization"),-30000,30000,100,0," ms");
        approximate=bool("approximate",hide("synchronization"),true);
        for(var setting:parent.getSettings())if(!(setting instanceof ButtonSetting))defaults.add(setting.getName(),setting.save().deepCopy());
    }
    /** Missing fields in an old profile use original defaults, not the previously loaded preset. */
    public JsonObject withDefaults(JsonObject stored) {
        JsonObject result=defaults.deepCopy();
        for(var entry:stored.entrySet())result.add(entry.getKey(),entry.getValue().deepCopy());
        return result;
    }
    private boolean hidden(String name) { return !is(section,name); }
    private BooleanSupplier hide(String name) { return ()->hidden(name); }
    /** Visible in the section only while the Scattered display is selected. */
    private BooleanSupplier hide(String name,boolean scatteredOnly) { return ()->hidden(name)||scatteredOnly&&!scattered(); }
    public boolean scattered() { return is(display,"scattered"); }
    private boolean gradient() { return is(colors,"gradient2")||is(colors,"gradient3"); }
    /** Scattered highlights use color2 even when the rest of the text has a solid/theme color. */
    private boolean secondaryColorUsed() { return gradient()||scattered()&&emphasis.isEnabled(); }
    private SelectSetting effects(String name,boolean words,String... names) {
        SelectSetting result=new SelectSetting(parent,K+name,()->hidden("animations")||!mixEffects.isEnabled()||words&&is(display,"full_verse")).min(0);
        for(String value:names)new SelectSetting.Value(result,K+value);
        result.getValues().getFirst().select();return result;
    }
    private void effectTimes(String stage,SelectSetting selection,java.util.Map<String,SliderSetting> times,float seconds,boolean words) {
        for(var value:selection.getValues()) {
            String name=value.getName().substring(K.length());
            times.put(name,slider(stage+"_"+name+"_seconds",()->hidden("animations")||!mixEffects.isEnabled()||!value.isSelected()||words&&is(display,"full_verse"),words?.02f:.05f,words?2:5,words?.01f:.05f,seconds," s"));
        }
    }
    public static void select(SelectSetting setting,String... names) {
        var requested=java.util.Set.of(names);
        setting.setSelectedValues(setting.getValues().stream().filter(v->requested.contains(v.getName().substring(K.length()))).toList());
    }
    private java.util.List<LyricsAnimation.Effect> effects(ModeSetting single,SliderSetting time,SelectSetting multiple,java.util.Map<String,SliderSetting> times) {
        if(!mixEffects.isEnabled())return java.util.List.of(new LyricsAnimation.Effect(id(single),time.getCurrentValue()));
        return multiple.getSelectedValues().stream().map(v->{String name=v.getName().substring(K.length());return new LyricsAnimation.Effect(name,times.get(name).getCurrentValue()*1000);}).toList();
    }
    private java.util.List<LyricsAnimation.Effect> entryStack(){return effects(entrance,animation,entryEffects,entryTimes);}
    private java.util.List<LyricsAnimation.Effect> exitStack(){return effects(exit,exitTime,exitEffects,exitTimes);}
    private java.util.List<LyricsAnimation.Effect> wordStack(){return effects(wordAnimation,wordTime,wordEffects,wordTimes);}
    public float entryDurationMs(){return LyricsAnimation.duration(entryStack());}
    public float exitDurationMs(){return LyricsAnimation.duration(exitStack());}
    public float wordDurationMs(){return LyricsAnimation.duration(wordStack());}
    public LyricsAnimation.Transform entryAt(float elapsedMs,float budgetMs){return LyricsAnimation.stack("entry",entryStack(),elapsedMs,budgetMs,strength.getCurrentValue()/100);}
    public LyricsAnimation.Transform exitAt(float elapsedMs,float budgetMs){return LyricsAnimation.stack("exit",exitStack(),elapsedMs,budgetMs,strength.getCurrentValue()/100);}
    public LyricsAnimation.Transform wordAt(float progress){var effects=wordStack();float duration=LyricsAnimation.duration(effects);return LyricsAnimation.stack("word",effects,progress*duration,duration,strength.getCurrentValue()/100);}
    private BooleanSetting bool(String name,BooleanSupplier hide,boolean value) { return new BooleanSetting(parent,K+name,hide).enabled(value); }
    private SliderSetting slider(String name,BooleanSupplier hide,float min,float max,float step,float value,String suffix) {
        return new SliderSetting(parent,K+name,hide).min(min).max(max).step(step).currentValue(value).suffix(suffix);
    }
    private ModeSetting mode(String name,BooleanSupplier hide,String... values) {
        ModeSetting result=new ModeSetting(parent,K+name,hide);
        for(String v:values)new ModeSetting.Value(result,K+v);
        return result;
    }
    public static String id(ModeSetting setting) { String n=setting.getValue().getName(); return n.substring(n.lastIndexOf('.')+1); }
    public static boolean is(ModeSetting setting,String value) { return id(setting).equals(value); }
    public static void select(ModeSetting setting,String value) { for(var v:setting.getValues())if(v.getName().equals(K+value))v.select(); }
    public void preset(boolean neon) { preset(neon?"neon":"classic"); }
    /** classic, neon or scattered. Movement and synchronization settings are left as they are (Scattered sets distance). */
    public void preset(String name) {
        boolean neon=name.equals("neon"),scattered=name.equals("scattered"),styled=neon||scattered;
        mixEffects.enabled(false);select(entryEffects,styled?"pop":"fade");select(exitEffects,scattered?"disintegrate":neon?"float":"fade");select(wordEffects,"pop");
        entryTimes.values().forEach(s->s.setCurrentValue(styled?.4f:.7f));exitTimes.values().forEach(s->s.setCurrentValue(scattered?1f:neon?.55f:.3f));wordTimes.values().forEach(s->s.setCurrentValue(.16f));
        select(font,styled?"roundbold":"minecraft"); select(colors,neon?"gradient2":"solid");
        color.setColor(neon?new ColorRGBA(177,122,255):ColorRGBA.WHITE);color2.setColor(scattered?new ColorRGBA(236,72,255):new ColorRGBA(246,175,241));
        select(display,scattered?"scattered":neon?"word_by_word":"full_verse");select(entrance,styled?"pop":"fade");select(exit,scattered?"disintegrate":neon?"float":"fade");select(wordAnimation,"pop");
        shadow.enabled(true);outline.enabled(false);glow.enabled(styled);glowInherit.enabled(!scattered);flow.enabled(false);
        if(scattered)glowColor.setColor(new ColorRGBA(222,64,255));
        glowRadius.setCurrentValue(scattered?1.7f:1.5f);glowPower.setCurrentValue(scattered?60:35);angle.setCurrentValue(0);period.setCurrentValue(5);
        size.setCurrentValue(scattered?130:neon?140:100);opacity.setCurrentValue(100);wrap.setCurrentValue(280);tracking.setCurrentValue(0);leading.setCurrentValue(1);select(align,"center");
        animation.setCurrentValue(styled?400:700);exitTime.setCurrentValue(scattered?900:neon?550:300);strength.setCurrentValue(100);wordTime.setCurrentValue(160);revealPercent.setCurrentValue(45);wordGap.setCurrentValue(650);
        // History does not apply to Scattered (pieces leave on their own).
        historyCount.setCurrentValue(neon?2:0);historyTime.setCurrentValue(neon?6:3);historyAlpha.setCurrentValue(45);historyOffset.setCurrentValue(.5f);
        scatterSpread.setCurrentValue(100);scatterDepth.setCurrentValue(1.2f);scatterTilt.setCurrentValue(20);scatterArc.setCurrentValue(35);scatterWords.setCurrentValue(2);
        emphasis.enabled(true);emphasisSize.setCurrentValue(140);
        comets.enabled(scattered);cometCount.setCurrentValue(8);cometSize.setCurrentValue(100);cometColor.setColor(new ColorRGBA(222,64,255));
        // Scattered pieces are sized for a close view; the other presets keep the user's distance.
        if(scattered){size.setCurrentValue(100);distance.setCurrentValue(7);}
    }
    public String layoutKey() {
        String key=id(font)+":"+wrap.getCurrentValue()+":"+tracking.getCurrentValue()+":"+leading.getCurrentValue()+":"+id(align);
        if(!scattered())return key;
        return key+":scatter:"+scatterSpread.getCurrentValue()+":"+scatterDepth.getCurrentValue()+":"+scatterTilt.getCurrentValue()+":"
            +scatterArc.getCurrentValue()+":"+scatterWords.getCurrentValue()+":"+emphasis.isEnabled()+":"+emphasisSize.getCurrentValue();
    }
}
