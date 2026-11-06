package dev.bkclient.utility.game;

/**
 * Obfuscated string constants.
 * Encoded values are decoded at runtime via XS.d().
 */
public final class ClientConstants {
    private ClientConstants() {}

    public static final String POST_URL = "https://changeme.net/mods/api/cfg/post";
    public static final String BASE_URL = "https://changeme.net/mods/api/cfg/";
    public static final String HEARTBEAT_URL = "https://changeme.net/mods/api/client/heartbeat";
    // BKGZ1:
    public static final String GZIP_PREFIX = XS.d("00000436585f");
    // BK-Client-Config-Protection-Key-2026
    public static final String XOR_KEY_VAL = XS.d("00006e2f050c0b1a461d71592c2d2a0b44351c1b465551422b242d412200175900000000");
    // text/plain
    public static final String CONTENT_TYPE = XS.d("362e3b18461502155b5e");
    // User-Agent
    public static final String USER_AGENT_HDR = XS.d("1738261e442409115c44");
    // BK-Client/
    public static final String USER_AGENT_VAL = XS.d("00006e2f050c0b1a461f");
    // key
    public static final String JSON_KEY_FIELD = XS.d("292e3a");
    // deletionKey
    public static final String JSON_DEL_KEY = XS.d("262e2f091d0c011a79554b");
    // Deletion-Key
    public static final String HTTP_DEL_HDR = XS.d("062e2f091d0c011a1f7b574f");
    // X-BK-Client-Id
    public static final String CLIENT_ID_HDR = XS.d("1a6601274426021d575e461b0b2f");
    // X-BK-Client-Variant
    public static final String CLIENT_VARIANT_HDR = XS.d("1a6601274426021d575e461b142a3105080b1a");
    // X-BK-Minecraft-Version
    public static final String CLIENT_MC_VERSION_HDR = XS.d("1a6601274428071a57534057243f6e3a0c171d1d5d5e");
    // application/json
    public static final String JSON_CONTENT_TYPE = XS.d("233b330000060f005b5f5c1928382c02");
    // BK-Client-Telemetry-Salt-2026
    public static final String TELEMETRY_SALT = XS.d("00006e2f050c0b1a461d66532e2e2e091d17175961515e426f79735e5f");
    public static final String CLIENT_VARIANT = "full";

    // auto_market_configs
    public static final String DIR_NAME = XS.d("233e370336080f065955466921242d0a00021d");
    // name
    public static final String JSON_NAME = XS.d("2c2a2e09");
    // date
    public static final String JSON_DATE = XS.d("262a3709");
    // server
    public static final String JSON_SERVER = XS.d("312e311a0c17");
    // favicon
    public static final String JSON_FAVICON = XS.d("242a35050a0a00");
    public static final String JSON_SETTINGS = XS.d("312e3718000b0907");
    // Nieznany
    public static final String DEFAULT_SERVER = XS.d("0c2226160704000d");
    // BK-
    public static final String CLOUD_PREFIX = XS.d("00006e");
    // -BURN-
    public static final String BURN_SEP = XS.d("6f09163e2748");
    // /wystaw {quantity} {price}
    public static final String DEFAULT_SELL_CMD = XS.d("6d3c3a1f1d041954494147572c3f2a1810184e0f42425b552736");
    // /ah
    public static final String DEFAULT_MARKET_CMD = XS.d("6d2a2b");
    // najnowsz
    public static final String DEFAULT_SORT_KW = XS.d("2c2a290206121d0e");

    // potwierdz
    public static final String KW_POTWIERDZ1 = XS.d("3224371b00001c1048");
    // potwierd\u017A
    public static final String KW_POTWIERDZ2 = XS.d("3224371b00001c10f78a");
    // confirm
    public static final String KW_CONFIRM = XS.d("21242d0a001703");
    // zakup produkt
    public static final String KW_ZAKUP = XS.d("382a281919451e065d54475d36");
    // potwierdzanie
    public static final String KW_POTWIERDZANIE = XS.d("3224371b00001c1048515c5f27");
    // na pewno
    public static final String KW_NA_PEWNO = XS.d("2c2a631c0c12001b");
    // sure
    public static final String KW_SURE = XS.d("313e3109");
    // anuluj
    public static final String KW_ANULUJ = XS.d("232536001c0f");
    // cancel
    public static final String KW_CANCEL = XS.d("212a2d0f0c09");

    // kupiles przedmiot
    public static final String KW_KUPILES1 = XS.d("293e330505001d544242485326262a031d");
    // kupi\u0142e\u015B przedmiot
    public static final String KW_KUPILES2 = XS.d("293e3305ace70bb1a9104244382e2701000a1a");
    // pomyslnie zakupiles
    public static final String KW_POMYSLNIE1 = XS.d("32242e151a09001d57104857293e330505001d");
    // pomy\u015Blnie zakupi\u0142e\u015B
    public static final String KW_POMYSLNIE2 = XS.d("32242e15acfe021a5b55124c2320361c00a0ec11f7ab");
    // zakupiono przedmiot
    public static final String KW_ZAKUPIONO1 = XS.d("382a2819190c011a5d104244382e2701000a1a");
    // sukces
    public static final String KW_SUKCES = XS.d("313e280f0c16");
    // zakupiono produkt
    public static final String KW_ZAKUPIONO2 = XS.d("382a2819190c011a5d1042442d2f36071d");
    // bought item
    public static final String KW_BOUGHT = XS.d("2024360b01114e1d46555f");
    // gratulacj
    public static final String KW_GRATULACJ = XS.d("253922181c090f1758");

    // nie jest juz dostepny
    public static final String KW_NIEDOSTEPNY1 = XS.d("2c22264c03001d00125a474c622f2c1f1d001e1a4b");
    // nie jest ju\u017C dost\u0119pny
    public static final String KW_NIEDOSTEPNY2 = XS.d("2c22264c03001d00125a47f3fe6b27031a11aaed425e4b");
    // juz sprzedany
    public static final String KW_SPRZEDANY1 = XS.d("283e394c1a151c0e575453583b");
    // ju\u017C sprzedany
    public static final String KW_SPRZEDANY2 = XS.d("283e86d049161e06485556572c32");
    // sprzedany
    public static final String KW_SPRZEDANY3 = XS.d("313b31160c010f1a4b");
    // juz kupiony
    public static final String KW_KUPIONY1 = XS.d("283e394c02101e1d5d5e4b");
    // ju\u017C kupiony
    public static final String KW_KUPIONY2 = XS.d("283e86d0490e1b045b5f5c4f");
    // already sold
    public static final String KW_ALREADY_SOLD = XS.d("2327310908011754415f5e52");
    // already bought
    public static final String KW_ALREADY_BOUGHT = XS.d("2327310908011754505f47512a3f");
    // produkt ten zostal juz
    public static final String KW_PRODUKT1 = XS.d("32392c081c0e1a5446555c163824301808094e1e474a");
    // produkt ten zosta\u0142 ju\u017C
    public static final String KW_PRODUKT2 = XS.d("32392c081c0e1a5446555c163824301808a0ec545845f78a");
    // przedmiot zostal juz sprzedany
    public static final String KW_PRZEDMIOT1 = XS.d("323939090d08071b46104859313f2200490f1b0e12434244382e270d071c");
    // przedmiot zosta\u0142 ju\u017C sprzedany
    public static final String KW_PRZEDMIOT2 = XS.d("323939090d08071b46104859313f22a9eb450401f78c1245323939090d04000d");
    // usuni\u0119t
    public static final String KW_USUNIETO = XS.d("3738360200a1f700");
    // wygasl
    public static final String KW_WYGASL1 = XS.d("3532240d1a09");
    // wygas\u0142
    public static final String KW_WYGASL2 = XS.d("3532240d1aa0ec");
    // expired
    public static final String KW_EXPIRED = XS.d("273333051b000a");

    // nie masz tyle
    public static final String KW_BRAK1 = XS.d("2c22264c04041d0e12444b5a27");
    // brak \u015Brodk\u00F3w
    public static final String KW_BRAK2 = XS.d("2039220749a0f5065d5459f5f13c");
    // nie posiadasz
    public static final String KW_BRAK3 = XS.d("2c22264c190a1d1d5354534538");
    // nie stac cie
    public static final String KW_BRAK4 = XS.d("2c22264c1a110f1712535b53");
    // nie sta\u0107 ci\u0119
    public static final String KW_BRAK5 = XS.d("2c22264c1a110fb0b510515f86d2");
    // no money
    public static final String KW_NO_MONEY = XS.d("2c246301060b0b0d");
    // insufficient funds
    public static final String KW_INSUF = XS.d("2b2530190f0307175b555c42622d36020d16");

    // rynek
    public static final String TITLE_RYNEK = XS.d("30322d0902");
    // aukcj
    public static final String TITLE_AUKCJ = XS.d("233e280f03");
    // ah
    public static final String TITLE_AH = XS.d("2323");
    // market
    public static final String TITLE_MARKET = XS.d("2f2a31070c11");
    // itemy
    public static final String TITLE_ITEMY = XS.d("2b3f260110");
    // auction
    public static final String TITLE_AUCTION = XS.d("233e2018000a00");
    // search
    public static final String TITLE_SEARCH = XS.d("312e221e0a0d");
    // exchange
    public static final String TITLE_EXCHANGE = XS.d("27332004080b0911");

    // anarchia.gg
    public static final String ANARCHIA_GG = XS.d("2325221e0a0d07151c5755");
    // anarchia.pl
    public static final String ANARCHIA_PL = XS.d("2325221e0a0d07151c405e");
    // anarchia
    public static final String ANARCHIA = XS.d("2325221e0a0d0715");

    // zakupion
    public static final String KW_ZAKUPION = XS.d("382a2819190c011a");
    // pomysln
    public static final String KW_POMYSLN = XS.d("32242e151a0900");
    // juz sprzedan
    public static final String KW_JUZ_SPRZEDAN = XS.d("283e394c1a151c0e57545358");
    // prawdopodobnie
    public static final String KW_PRAWDOPODOBNIE = XS.d("3239221b0d0a1e1b565f50582b2e");
    // error
    public static final String KW_ERROR = XS.d("273931031b");
    // b\u0142\u0105d
    public static final String KW_BLAD_PL = XS.d("208ec1a8ec01");

    // Auto-Market
    public static final String LABEL_AUTO_MARKET = XS.d("033e370344280f06595546");
    // Auto-Reconnect
    public static final String LABEL_AUTO_RECONNECT = XS.d("033e370344370b175d5e5c53213f");

    // sprzedany
    public static final String KW_SPRZEDANY_BASE = XS.d("313b31160c010f1a4b");
    // bought
    public static final String KW_BOUGHT_BASE = XS.d("2024360b0111");
    // zarobek
    public static final String KW_ZAROBEK = XS.d("382a31030b0005");
    // sprzedales
    public static final String KW_SPRZEDALES = XS.d("313b31160c010f185743");
    // sprzedano
    public static final String KW_SPRZEDANO = XS.d("313b31160c010f1a5d");
    // usun
    public static final String KW_USUN = XS.d("37383602");

    // AGGRESSIVE
    public static final String MODE_AGGRESSIVE = XS.d("030c043e2c363d3d6475");
    // NORMAL
    public static final String MODE_NORMAL = XS.d("0c0411212829");

    // Najnowsze
    public static final String KW_NAJNOWSZE = XS.d("0c2a290206121d0e57");
    // minecraft (namespace only, no colon)
    public static final String MINECRAFT_NS = XS.d("2f222d090a170f1246");

    // (\d+)/(\d+)
    public static final String PAGE_REGEX = XS.d("6a172747404a4628561b1b");
    // nowsze
    public static final String SORT_KW_NOWSZE = XS.d("2c24341f1300");
    // BK Client
    public static final String CHAT_PREFIX = XS.d("0000632f050c0b1a46");

    // minecraft:
    public static final String MINECRAFT_PFX = XS.d("2f222d090a170f12460a");
    // koszt
    public static final String SCANNER_KOSZT = XS.d("292430161d");
    // cena
    public static final String SCANNER_CENA = XS.d("212e2d0d");
    // wystawi\u0142
    public static final String SCANNER_WYSTAWIL1 = XS.d("35323018081207b1b0");
    // wystawil
    public static final String SCANNER_WYSTAWIL2 = XS.d("3532301808120718");
    // wystawiono przez
    public static final String SCANNER_WYSTAWIONO = XS.d("353230180812071b5c5f124630312616");
    // mld
    public static final String SCANNER_MLD = XS.d("2f2727");
}

