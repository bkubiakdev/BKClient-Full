package dev.bkclient.ui.clicksys;
import dev.bkclient.ui.clicksys.port.util.OptionSearchIndex;
import dev.bkclient.ui.clicksys.port.setting.SearchableListSetting;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class SearchableOptionsTest {
    static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    public static void main(String[] args){
        var index=new OptionSearchIndex(List.of("minecraft:gold_ore Ruda złota", "minecraft:diamond_ore Ruda diamentu", "Sword of Light"));
        check(Arrays.equals(index.filter("ZLOTA"),new int[]{0}),"Polish accents and case");
        check(Arrays.equals(index.filter("minecraft:diamond"),new int[]{1}),"registry ID search");
        check(Arrays.equals(index.filter("ore diamentu"),new int[]{1}),"multiple terms");
        check(index.filter("unknown").length==0,"no results");
        int[] all=index.filter(""); check(all.length==3 && all==index.filter(""),"unchanged query reuses results");
        check(index.filter("sword")[0]==2,"weapon name");
        var many=new OptionSearchIndex(java.util.stream.IntStream.range(0,10000).mapToObj(i->"block"+i).toList());
        check(many.filter("").length==10000,"large catalog");
        for(int scroll:new int[]{0,140,28000,279860}) {
            int first=OptionSearchIndex.firstVisible(scroll,28),end=OptionSearchIndex.endVisible(scroll,140,28,10000);
            check(end-first<=9 && first>=0 && end<=10000,"bounded viewport visits");
        }
        AtomicInteger labels=new AtomicInteger(),version=new AtomicInteger();
        var setting=new SearchableListSetting("Blocks","",List.of("a","b","c"),List.of("b"),true,0,
                key->{labels.incrementAndGet();return key+version.get();},version::get);
        check(labels.get()==0,"no eager index");
        setting.filter("b0");check(labels.get()==3,"index built once");
        for(int i=0;i<1000;i++)setting.filter("b0");check(labels.get()==3,"no per-frame rebuild");
        setting.onInt(0);check(setting.isInt(0)&&setting.isInt(1),"multi-select original indices");
        setting.filter("c0");check(setting.isInt(1),"filter preserves selections");
        version.incrementAndGet();check(setting.filter("b1")[0]==1&&labels.get()==6,"language invalidation");
        SearchableListSetting.invalidateResources();setting.filter("");check(labels.get()==9,"resource invalidation");
        var single=new SearchableListSetting("Weapon","",List.of("a","b"),List.of("a"),false,1,k->k,()->0);
        single.onInt2(1);check(single.getInt()==1&&single.getString2().equals("b"),"weapon single selection");
        var blocks=new SearchableListSetting("Blocks","",List.of("diamond_ore"),List.of(),true,0,
                k->"Ruda diamentu",()->0,k->"minecraft:"+k);
        check(blocks.filter("minecraft:diamond").length==1,"namespaced ID metadata without changing config key");
        blocks.onInt(0);check(blocks.getString2().equals("diamond_ore"),"stable block config key");
        System.out.println("Search normalization, selection, lazy indexing, invalidation and 10k-option virtualization passed");
    }
}
