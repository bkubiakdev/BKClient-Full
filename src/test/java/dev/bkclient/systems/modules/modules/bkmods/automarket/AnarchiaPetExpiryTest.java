package dev.bkclient.systems.modules.modules.bkmods.automarket;
public final class AnarchiaPetExpiryTest {
    private static void expect(String text,Integer days){
        if(!java.util.Objects.equals(AnarchiaPetExpiry.daysLeft(text),days))throw new AssertionError(text);
    }
    public static void main(String[] args){
        expect("Pet wygaśnie za: 5d 2h",5);
        expect("§7Pet wygaśnie za: §f5d §f2h",5);
        expect("PET WYGASNIE ZA: 18d",18);
        expect("Pet wygaśnie za: 17d 23h 59m 59s",17);
        expect("Pet wygaśnie za: 23h 59m",0);
        expect("Pet wygaśnie za: 2h",0);
        expect("Pet wygaśnie za: 0s",0);
        expect("Pet wygaśnie za: 5d2h",5);
        expect("Pet wygaśnie za:\u00a05d\u00a02h".replace("\u00a0"," "),5);
        expect("Pet wygaśnie za: 999999999999999999999d",null);
        expect("Pet wygaśnie za: 5d ???",null);
        expect("Pet wygaśnie za: -2d",null);
        expect("Pet wygaśnie za: ",null);
        expect("Kostium wygaśnie za: 5d 2h",null);
        expect("Oferta wygaśnie za: 30d",null);
        expect("Pet wygaśnie: 12-10-2026",null);
        if(AnarchiaPetExpiry.daysLeft("Pet wygaśnie za: 5d 2h")>=18)throw new AssertionError("18-day filter must reject screenshot pet");
        var scanner=new AutoMarketScanner();
        var screenshot=java.util.List.<net.minecraft.text.Text>of(net.minecraft.text.Text.literal("Pet wygaśnie za: 5d 2h"));
        if(scanner.remainingDays(screenshot)!=5)throw new AssertionError("scanner path must read screenshot");
        if(AnarchiaPetExpiry.meetsMinimum(scanner.remainingDays(screenshot),18))throw new AssertionError("debug/purchase shared decision");
        if(!AnarchiaPetExpiry.meetsMinimum(scanner.remainingDays(screenshot),5))throw new AssertionError("five-day minimum accepted");
        if(AnarchiaPetExpiry.meetsMinimum(null,18))throw new AssertionError("unknown fails closed");
        if(!AnarchiaPetExpiry.meetsMinimum(null,0))throw new AssertionError("zero disables filter");
        if(scanner.remainingDays(java.util.List.of(net.minecraft.text.Text.literal("Pet wygaśnie: 31-12-2099")))==null)throw new AssertionError("legacy absolute date retained");
        if(scanner.remainingDays(java.util.List.of(net.minecraft.text.Text.literal("Kostium wygasa: 31.12.2099 12:00:00")))==null)throw new AssertionError("Minestar date retained");
        System.out.println("Anarchia pet expiry: screenshot 5d 2h, 18-day threshold, exact boundaries, formatting and malformed/other lore passed");
    }
}
