package dev.bkclient.systems.modules.modules.bkmods;
 
 import java.util.ArrayList;
 import java.util.List;
 import dev.bkclient.BKClientInit;
 import dev.bkclient.systems.event.EventListener;
 import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
 import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
 import dev.bkclient.systems.localization.Localizator;
 import dev.bkclient.systems.modules.api.ModuleCategory;
 import dev.bkclient.systems.modules.api.ModuleInfo;
 import dev.bkclient.systems.modules.impl.BaseModule;
 import dev.bkclient.systems.notifications.NotificationType;
 import dev.bkclient.systems.setting.SettingsContainer;
 import dev.bkclient.systems.setting.settings.ButtonSetting;
 import dev.bkclient.systems.setting.settings.ModeSetting;
 import dev.bkclient.systems.setting.settings.SliderSetting;
 import dev.bkclient.utility.inventory.InventoryUtility;
 import dev.bkclient.utility.time.Timer;
 import net.minecraft.block.BarrelBlock;
 import net.minecraft.block.Block;
 import net.minecraft.block.ChestBlock;
 import net.minecraft.block.TrappedChestBlock;
 import net.minecraft.block.entity.BlockEntity;
 import net.minecraft.block.entity.BrewingStandBlockEntity;
 import net.minecraft.client.gui.screen.ingame.BrewingStandScreen;
 import net.minecraft.component.DataComponentTypes;
 import net.minecraft.component.type.PotionContentsComponent;
 import net.minecraft.entity.player.PlayerEntity;
 import net.minecraft.item.Item;
 import net.minecraft.item.ItemStack;
 import net.minecraft.item.Items;
 import net.minecraft.potion.Potion;
 import net.minecraft.potion.Potions;
 import net.minecraft.registry.entry.RegistryEntry;
 import net.minecraft.screen.BrewingStandScreenHandler;
 import net.minecraft.screen.GenericContainerScreenHandler;
 import net.minecraft.screen.ScreenHandler;
 import net.minecraft.screen.slot.Slot;
 import net.minecraft.screen.slot.SlotActionType;
 import net.minecraft.util.Hand;
 import net.minecraft.util.hit.BlockHitResult;
 import net.minecraft.util.hit.HitResult;
 import net.minecraft.util.math.BlockPos;
 import net.minecraft.util.math.Direction;
 import net.minecraft.util.math.Position;
 import net.minecraft.util.math.Vec3d;
 
 @ModuleInfo(name = "Auto Potions", category = ModuleCategory.PLAYER, desc = "Automatically brews potions")
 public class AutoPotions
         extends BaseModule {
     private static final double INTERACTION_RANGE_SQ = 20.25;
     private final ModeSetting potions = new ModeSetting(this, "modules.settings.auto_potions.potion_type");
     private final ModeSetting.Value strength = new ModeSetting.Value(this.potions, "modules.settings.auto_potions.strength").select();
     private final ModeSetting.Value speed = new ModeSetting.Value(this.potions, "modules.settings.auto_potions.speed");
     private final ModeSetting.Value fire = new ModeSetting.Value(this.potions, "modules.settings.auto_potions.fire_resistance");
     private final ModeSetting.Value invisibility = new ModeSetting.Value(this.potions, "modules.settings.auto_potions.invisibility");
 
     private final ModeSetting enhancement = new ModeSetting(this, "modules.settings.auto_potions.enhancement");
     private final ModeSetting.Value none = new ModeSetting.Value(this.enhancement, "modules.settings.auto_potions.none").select();
     private final ModeSetting.Value level2 = new ModeSetting.Value(this.enhancement, "modules.settings.auto_potions.level2");
     private final ModeSetting.Value extended = new ModeSetting.Value(this.enhancement, "modules.settings.auto_potions.extended");
 
     private final SliderSetting delay = new SliderSetting((SettingsContainer) this, "modules.settings.auto_potions.delay",
             "modules.settings.auto_potions.delay.description")
             .step(10.0f).min(100.0f).max(1000.0f).currentValue(100.0f);
     private final ButtonSetting setChest = new ButtonSetting(this, "modules.settings.auto_potions.set_chest").action(this::setChestFromCrosshair);
     private final ButtonSetting clearChest = new ButtonSetting(this, "modules.settings.auto_potions.clear_chest").action(() -> {
         this.chestPos = null;
         this.notifyKey(NotificationType.INFO, "modules.notifications.auto_potions.chest_cleared");
     });
     private final Timer timer = new Timer();
     private State state = State.IDLE;
     private final Timer actionTimer = new Timer();
     private BrewingStandBlockEntity currentBrewer;
     private BlockPos chestPos;
     private boolean expectedScreenClose;
     private final List<BlockPos> processedBrewers = new ArrayList<BlockPos>();
     private List<BrewingStandBlockEntity> brewersQueue = new ArrayList<BrewingStandBlockEntity>();
     private final EventListener<WorldChangeEvent> onWorldChange = event -> this.resetState();
     private final EventListener<ClientPlayerTickEvent> onClientPlayerTickEvent = event -> {
         switch (this.state) {
             case IDLE: {
                 this.handleIdleState();
                 break;
             }
             case OPENING_BREWER: {
                 this.handleOpeningState();
                 break;
             }
             case PROCESSING: {
                 this.handleProcessingState();
                 break;
             }
             case OPENING_CHEST: {
                 this.handleOpeningChestState();
                 break;
             }
             case DEPOSITING: {
                 this.handleDepositingState();
                 break;
             }
             case CLOSING: {
                 this.handleClosingState();
             }
         }
     };

     private void resetState() {
         this.state = State.IDLE;
         this.currentBrewer = null;
         this.expectedScreenClose = false;
         this.processedBrewers.clear();
         this.brewersQueue.clear();
     }

     @Override
     public void onDisable() {
         this.resetState();
         super.onDisable();
     }
 
     private void handleOpeningState() {
         if (mc.currentScreen instanceof BrewingStandScreen) {
             this.state = State.PROCESSING;
             return;
         }
         if (this.actionTimer.finished(500L)) {
             BlockPos pos = this.currentBrewer.getPos();
             Vec3d vec = new Vec3d((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5);
             BlockHitResult hit = new BlockHitResult(vec, Direction.UP, pos, false);
             mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
             this.actionTimer.reset();
         }
     }
 
     private void handleProcessingState() {
         ScreenHandler screenHandler = mc.player.currentScreenHandler;
         if (!(screenHandler instanceof BrewingStandScreenHandler)) {
             if (!this.consumeExpectedScreenClose()) {
                 this.stopFromManualClose();
                 return;
             }
             this.state = State.IDLE;
             return;
         }
         BrewingStandScreenHandler brew = (BrewingStandScreenHandler) screenHandler;
 
         // Fuel management
         if (brew.getSlot(4).getStack().isEmpty() && brew.getFuel() == 0) {
             if (this.findIngredient(Items.BLAZE_POWDER) != -1) {
                 this.swapOneItem(Items.BLAZE_POWDER, 4);
                 return;
             }
         }
 
         // Bottle management
         for (int i = 0; i < 3; ++i) {
             if (brew.getSlot(i).getStack().isEmpty()) {
                 int bottleSlot = this.findWaterBottle(brew);
                 if (bottleSlot != -1) {
                     InventoryUtility.quickMove(bottleSlot);
                     return;
                 }
             }
         }
 
         // Brewing logic
         if (brew.getSlot(3).getStack().isEmpty() && brew.getFuel() > 0) {
             PotionRecipe recipe = getCurrentRecipe();
             if (recipe == null) return;
 
             RegistryEntry<Potion> currentType = getCommonPotion(brew);
             if (currentType == null) return;
 
             // 1. Water -> Awkward
             if (currentType.matchesKey(Potions.WATER.getKey().get())) {
                 handleIngredient(Items.NETHER_WART, 3);
                 return;
             }
 
             // 2. Base Potion Brewing
             if (currentType.matchesKey(Potions.AWKWARD.getKey().get())) {
                 handleIngredient(recipe.baseIngredient, 3);
                 return;
             }
 
             // Special case: Night Vision -> Invisibility
             if (this.invisibility.isSelected() && currentType.matchesKey(Potions.NIGHT_VISION.getKey().get())) {
                 handleIngredient(Items.FERMENTED_SPIDER_EYE, 3);
                 return;
             }
 
             // 3. Enhancement
             if (currentType.matchesKey(recipe.standardResult.getKey().get())) {
                 if (this.none.isSelected()) {
                     lootAndFinish(brew);
                     return;
                 }
 
                 if (this.level2.isSelected()) {
                     if (recipe.strongResult != null) {
                         handleIngredient(Items.GLOWSTONE_DUST, 3);
                     } else {
                         notifyError("modules.notifications.auto_potions.no_strong");
                         this.state = State.IDLE;
                     }
                     return;
                 }
 
                 if (this.extended.isSelected()) {
                     if (recipe.longResult != null) {
                         handleIngredient(Items.REDSTONE, 3);
                     } else {
                         notifyError("modules.notifications.auto_potions.no_extended");
                         this.state = State.IDLE;
                     }
                     return;
                 }
             }
 
             // 4. Finish if already enhanced or doesn't need more
             if ((recipe.strongResult != null && currentType.matchesKey(recipe.strongResult.getKey().get())) ||
                 (recipe.longResult != null && currentType.matchesKey(recipe.longResult.getKey().get()))) {
                 lootAndFinish(brew);
             }
         }
     }
 
     private PotionRecipe getCurrentRecipe() {
         if (this.strength.isSelected()) return PotionRecipe.STRENGTH;
         if (this.speed.isSelected()) return PotionRecipe.SPEED;
         if (this.fire.isSelected()) return PotionRecipe.FIRE_RES;
         if (this.invisibility.isSelected()) return PotionRecipe.INVIS;
         return null;
     }
 
     private RegistryEntry<Potion> getCommonPotion(BrewingStandScreenHandler brew) {
         RegistryEntry<Potion> lastType = null;
         for (int i = 0; i < 3; i++) {
             ItemStack stack = brew.getSlot(i).getStack();
             if (stack.isEmpty()) continue;
             if (stack.getItem() != Items.POTION) return null; // Not a standard potion
 
             PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
             if (contents == null || contents.potion().isEmpty()) return null;
 
             RegistryEntry<Potion> type = contents.potion().get();
             if (lastType != null && !lastType.equals(type)) return null; // Mixed types
             lastType = type;
         }
         return lastType;
     }
 
     private void lootAndFinish(BrewingStandScreenHandler brew) {
         this.lootPotions(brew);
         if (this.chestPos == null) {
             this.state = State.CLOSING;
         } else {
             this.expectedScreenClose = true;
             mc.player.closeHandledScreen();
             this.state = State.OPENING_CHEST;
         }
         this.actionTimer.reset();
     }
 
     private void notifyError(String message) {
         BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.ERROR,
                 Localizator.translate("modules.notifications.auto_potions.title"), Localizator.translate(message));
     }
 
     private void notify(NotificationType type, String title, String desc) {
         BKClientInit.getInstance().getNotificationManager().addNotificationOther(type, title, desc, 4000L);
     }
 
     private void notifyKey(NotificationType type, String descKey, Object... args) {
         this.notify(type, Localizator.translate("modules.notifications.auto_potions.title"),
                 Localizator.translate(descKey, args));
     }
 
     private static class PotionRecipe {
         Item baseIngredient;
         RegistryEntry<Potion> standardResult;
         RegistryEntry<Potion> strongResult;
         RegistryEntry<Potion> longResult;
 
         PotionRecipe(Item ing, RegistryEntry<Potion> std, RegistryEntry<Potion> str, RegistryEntry<Potion> lng) {
             this.baseIngredient = ing;
             this.standardResult = std;
             this.strongResult = str;
             this.longResult = lng;
         }
 
         static final PotionRecipe STRENGTH = new PotionRecipe(Items.BLAZE_POWDER, Potions.STRENGTH, Potions.STRONG_STRENGTH, Potions.LONG_STRENGTH);
         static final PotionRecipe SPEED = new PotionRecipe(Items.SUGAR, Potions.SWIFTNESS, Potions.STRONG_SWIFTNESS, Potions.LONG_SWIFTNESS);
         static final PotionRecipe FIRE_RES = new PotionRecipe(Items.MAGMA_CREAM, Potions.FIRE_RESISTANCE, null, Potions.LONG_FIRE_RESISTANCE);
         static final PotionRecipe INVIS = new PotionRecipe(Items.GOLDEN_CARROT, Potions.INVISIBILITY, null, Potions.LONG_INVISIBILITY);
     }
 
     private void handleIdleState() {
         if (this.actionTimer.finished(1000L)) {
             if (this.brewersQueue.isEmpty()) {
                 this.brewersQueue = this.findBrewers();
             }
             if (!this.brewersQueue.isEmpty()) {
                 this.currentBrewer = this.brewersQueue.removeFirst();
                 this.state = State.OPENING_BREWER;
                 this.actionTimer.reset();
             }
         }
     }
 
     private void handleIngredient(Item item, int slot) {
         if (this.findIngredient(item) == -1) {
             BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.ERROR,
                     Localizator.translate("modules.notifications.auto_potions.title"),
                     Localizator.translate("modules.notifications.auto_potions.item_not_found", item.getName().getString()));
             this.toggle();
             return;
         }
         this.swapOneItem(item, slot);
         this.expectedScreenClose = true;
         mc.player.closeHandledScreen();
     }
 
     private void handleDepositingState() {
         ScreenHandler screenHandler = mc.player.currentScreenHandler;
         if (!(screenHandler instanceof GenericContainerScreenHandler handler)) {
             if (!this.consumeExpectedScreenClose()) {
                 this.stopFromManualClose();
                 return;
             }
             this.state = State.OPENING_CHEST;
             this.actionTimer.reset();
             return;
         }
         if (!this.actionTimer.finished((long) this.delay.getCurrentValue())) {
             return;
         }

         int containerSlots = handler.getInventory().size();
         for (int i = containerSlots; i < handler.slots.size(); ++i) {
             Slot slot = handler.getSlot(i);
             ItemStack stack = slot.getStack();
             if (!this.shouldDepositPotion(stack)) {
                 continue;
             }
             mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, (PlayerEntity) mc.player);
             this.actionTimer.reset();
             return;
         }

         this.state = State.CLOSING;
         this.actionTimer.reset();
     }
 
     private void handleClosingState() {
         if (this.actionTimer.finished(500L)) {
             this.expectedScreenClose = true;
             mc.player.closeHandledScreen();
             if (this.currentBrewer != null) {
                 this.processedBrewers.add(this.currentBrewer.getPos());
             }
             this.state = State.IDLE;
             this.currentBrewer = null;
             this.actionTimer.reset();
         }
     }
 
     private List<BrewingStandBlockEntity> findBrewers() {
         ArrayList<BrewingStandBlockEntity> brewers = new ArrayList<BrewingStandBlockEntity>();
         int range = 10;
         BlockPos playerPos = BlockPos.ofFloored((Position) mc.player.getPos());
         for (int x = -range; x <= range; ++x) {
             for (int y = -range; y <= range; ++y) {
                 for (int z = -range; z <= range; ++z) {
                     BlockPos pos = playerPos.add(x, y, z);
                     BlockEntity blockEntity = mc.world.getBlockEntity(pos);
                     if (!(blockEntity instanceof BrewingStandBlockEntity))
                         continue;
                     BrewingStandBlockEntity brewer = (BrewingStandBlockEntity) blockEntity;
                     brewers.add(brewer);
                 }
             }
         }
         return brewers;
     }
 
     private boolean isPotion(ItemStack stack) {
         return stack.getItem() == Items.POTION || stack.getItem() == Items.SPLASH_POTION
                 || stack.getItem() == Items.LINGERING_POTION;
     }
 
     private boolean shouldDepositPotion(ItemStack stack) {
         if (!this.isPotion(stack)) {
             return false;
         }
         RegistryEntry<Potion> target = this.getTargetResultPotion();
         if (target == null) {
             return false;
         }
         PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
         return contents != null && contents.potion().isPresent() && contents.potion().get().equals(target);
     }
 
     private RegistryEntry<Potion> getTargetResultPotion() {
         PotionRecipe recipe = this.getCurrentRecipe();
         if (recipe == null) {
             return null;
         }
         if (this.level2.isSelected()) {
             return recipe.strongResult;
         }
         if (this.extended.isSelected()) {
             return recipe.longResult;
         }
         return recipe.standardResult;
     }
 
     private void lootPotions(BrewingStandScreenHandler brew) {
         for (int i = 0; i < 3; ++i) {
             if (!this.shouldDepositPotion(brew.getSlot(i).getStack()))
                 continue;
             InventoryUtility.quickMove(i);
         }
     }
 
     private void swapOneItem(Item item, int to) {
         int slot;
         if (this.timer.finished((long) (this.delay.getCurrentValue() * 2.0f))
                 && (slot = this.findIngredient(item)) != -1) {
             InventoryUtility.swapOneItem(slot, to);
             this.timer.reset();
         }
     }
 
     private int findIngredient(Item item) {
         for (int i = 5; i < 41; ++i) {
             if (((Slot) mc.player.currentScreenHandler.slots.get(i)).getStack().getItem() != item)
                 continue;
             return i;
         }
         return -1;
     }
 
     private int findWaterBottle(BrewingStandScreenHandler brew) {
         for (int i = 5; i < 41; ++i) {
             ItemStack stack = ((Slot) brew.slots.get(i)).getStack();
             if (stack.getItem() != Items.POTION
                     || !((PotionContentsComponent) stack.get(DataComponentTypes.POTION_CONTENTS)).potion().isPresent()
                     || ((RegistryEntry) ((PotionContentsComponent) stack.get(DataComponentTypes.POTION_CONTENTS))
                             .potion().get()).value() != Potions.WATER.value())
                 continue;
             return i;
         }
         return -1;
     }
 
     private void handleOpeningChestState() {
         if (this.chestPos == null) {
             this.state = State.CLOSING;
             return;
         }
         if (!this.isInInteractionRange(this.chestPos)) {
             this.notifyKey(NotificationType.ERROR, "modules.notifications.auto_potions.chest_too_far");
             this.state = State.CLOSING;
             this.actionTimer.reset();
             return;
         }
         if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
             this.state = State.DEPOSITING;
             this.actionTimer.reset();
             return;
         }
         if (!this.actionTimer.finished(500L)) {
             return;
         }
 
         Vec3d hitVec = this.chestPos.toCenterPos();
         BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, this.chestPos, false);
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
         mc.player.swingHand(Hand.MAIN_HAND);
         this.actionTimer.reset();
     }
 
     private void setChestFromCrosshair() {
         if (mc.world == null || mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
             this.notifyKey(NotificationType.ERROR, "modules.notifications.auto_potions.look_at_chest");
             return;
         }
 
         BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
         Block block = mc.world.getBlockState(pos).getBlock();
         if (!(block instanceof ChestBlock) && !(block instanceof TrappedChestBlock) && !(block instanceof BarrelBlock)) {
             this.notifyKey(NotificationType.ERROR, "modules.notifications.auto_potions.not_chest");
             return;
         }
 
         this.chestPos = pos.toImmutable();
         this.notifyKey(NotificationType.SUCCESS, "modules.notifications.auto_potions.chest_set",
                 pos.getX(), pos.getY(), pos.getZ());
     }
 
     private boolean isInInteractionRange(BlockPos pos) {
         return mc.player != null && mc.player.squaredDistanceTo(pos.toCenterPos()) <= INTERACTION_RANGE_SQ;
     }
 
     private boolean consumeExpectedScreenClose() {
         if (!this.expectedScreenClose) {
             return false;
         }
         this.expectedScreenClose = false;
         return true;
     }
 
     private void stopFromManualClose() {
         this.notifyKey(NotificationType.INFO, "modules.notifications.auto_potions.manual_stop");
         this.toggle();
     }
 
     private static enum State {
         IDLE,
         OPENING_BREWER,
         PROCESSING,
         OPENING_CHEST,
         DEPOSITING,
         CLOSING;
 
     }
 }


