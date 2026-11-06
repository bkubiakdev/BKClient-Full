package dev.bkclient.systems.modules.modules.bkmods.automarket;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Relative pet lifetime from Anarchia lore; unknown/malformed lifetimes fail closed. */
public final class AnarchiaPetExpiry {
    // Precompiled regex literals (same semantics as String.replaceAll/matches/split).
    private static final java.util.regex.Pattern BK_RE_0 = java.util.regex.Pattern.compile("(?i)\u00a7[0-9a-fk-orx]");
    private static final java.util.regex.Pattern BK_RE_1 = java.util.regex.Pattern.compile("\\p{M}+");
    private static final Pattern HEADER=Pattern.compile("(?:^|\\W)pet\\s+wygasnie\\s+za\\s*:");
    private static final Pattern DURATION=Pattern.compile("(?:(\\d+)\\s*d\\s*)?(?:(\\d+)\\s*h\\s*)?(?:(\\d+)\\s*m\\s*)?(?:(\\d+)\\s*s\\s*)?");
    private AnarchiaPetExpiry() {}
    private static String normalize(String line) {
        if(line==null)return "";
        return BK_RE_1.matcher(Normalizer.normalize(BK_RE_0.matcher(line).replaceAll(""),Normalizer.Form.NFD)).replaceAll("").toLowerCase(Locale.ROOT).replace('\u00a0',' ').trim();
    }
    public static boolean isExpiryLine(String line) { return HEADER.matcher(normalize(line)).find(); }
    public static boolean meetsMinimum(Integer days,Integer minimum) {
        return minimum==null || minimum<=0 || days!=null && days>=minimum;
    }
    public static Integer daysLeft(String line) {
        String normalized=normalize(line);
        var header=HEADER.matcher(normalized);
        if(!header.find())return null;
        var duration=DURATION.matcher(normalized.substring(header.end()).trim());
        if(!duration.matches())return null;
        long seconds=0; boolean found=false;
        long[] units={86400,3600,60,1};
        try {
            for(int i=0;i<4;i++)if(duration.group(i+1)!=null){
                found=true;
                seconds=Math.addExact(seconds,Math.multiplyExact(Long.parseLong(duration.group(i+1)),units[i]));
            }
            long days=seconds/86400;
            return found && days<=Integer.MAX_VALUE ? (int)days : null;
        } catch(ArithmeticException|NumberFormatException invalid) {return null;}
    }
}
