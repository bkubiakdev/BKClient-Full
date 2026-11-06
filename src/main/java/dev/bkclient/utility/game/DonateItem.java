package dev.bkclient.utility.game;

import lombok.Generated;

public enum DonateItem {
   TAL_KRUSH("Crusher Talisman", new String[]{"tal-krush", "attribute-item-tkryshitela"}, 1, 15),
   TAL_KARATEL("Punisher Talisman", new String[]{"tal-karatelya", "attribute-item-tkaratela"}, 3, 14),
   TAL_GARMONIA("Harmony Talisman", new String[]{"tal-garmonii", "attribute-item-tgarmonii"}, 2, 12),
   TAL_DEDALA("Daedalus Talisman", new String[]{"tal-dedala", "attribute-item-tdedala"}, 4, 11),
   TAL_EHIDNA("Echidna Talisman", new String[]{"tal-ehidna", "attribute-item-texidni"}, 6, 10),
   TAL_GRAN("Edge Talisman", new String[]{"tal-grani", "attribute-item-tgrani"}, 5, 7),
   TAL_TRITON("Triton Talisman", new String[]{"tal-tritona", "attribute-item-ttritona"}, 8, 2),
   TAL_FENIX("Phoenix Talisman", new String[]{"tal-fenixa", "attribute-item-tfeniksa"}, 7, 1),
   SP_ANDROMEDA("Andromeda Sphere", new String[]{"sphere-andromeda", "attribute-item-sandromedi"}, 8, 13),
   SP_PANDORA("Pandora Sphere", new String[]{"sphere-pandora", "attribute-item-spandori"}, 9, 9),
   SP_HIMERA("Chimera Sphere", new String[]{"sphere-himeri", "attribute-item-shimeri"}, 10, 8),
   SP_TITAN("Titan Sphere", new String[]{"sphere-titana", "attribute-item-stitana"}, 11, 6),
   SP_ASTREYA("Astraea Sphere", new String[]{"sphere-astreya", "attribute-item-sastreya"}, 11, 5),
   SP_APOLLON("Apollo Sphere", new String[]{"sphere-apollona", "attribute-item-sapollona"}, 11, 4),
   SP_OSIRIS("Osiris Sphere", new String[]{"sphere-osirisa", "attribute-item-sosirisa"}, 11, 3),
   TAL_INFINITY("Infinity Talisman", new String[]{"Infinity"}, 1, 1),
   TAL_ETERNITY("Eternity Talisman", new String[]{"Eternity"}, 2, 1),
   TAL_STINGER("Stinger Talisman", new String[]{"Stinger"}, 3, 1),
   TAL_SATIRI("Satire Talisman", new String[]{"Satir"}, 4, 1),
   TAL_MIF("Mythical Talisman", new String[]{"MYTHICAL"}, 5, 1),
   TAL_LEG("Legendary Talisman", new String[]{"LEGENDARY"}, 6, 1),
   TAL_EPIC("Epic Talisman", new String[]{"EPIC"}, 7, 1),
   TAL_DEFAULT("Normal Talisman", new String[]{"NORMAL"}, 8, 1);

   private final String name;
   private final String[] nbt;
   private final int totem;
   private final int factor;

   DonateItem(String name, String[] nbt, int totem, int factor) {
      this.name = name;
      this.nbt = nbt;
      this.totem = totem;
      this.factor = factor;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public String[] getNbt() {
      return this.nbt;
   }

   @Generated
   public int getTotem() {
      return this.totem;
   }

   @Generated
   public int getFactor() {
      return this.factor;
   }
}
