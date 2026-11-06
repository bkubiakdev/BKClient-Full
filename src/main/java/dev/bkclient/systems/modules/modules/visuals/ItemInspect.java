package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.systems.event.impl.window.MouseEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.animation.InspectAnimation;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import java.util.EnumMap;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "Item Inspect", category = ModuleCategory.VISUALS, desc = "modules.descriptions.item_inspect")
public final class ItemInspect extends BaseModule {
    private static ItemInspect instance;
    public final BindSetting inspectKey = new BindSetting(this, "modules.settings.item_inspect.key").key(GLFW.GLFW_KEY_G);
    public final ModeSetting animation = new ModeSetting(this, "modules.settings.item_inspect.animation");
    private final ModeSetting.Value automatic = new ModeSetting.Value(animation, "modules.settings.item_inspect.auto").select();
    private final EnumMap<InspectAnimation.Style, ModeSetting.Value> styles = new EnumMap<>(InspectAnimation.Style.class);
    private final ModeSetting.Value cycle;
    private final ModeSetting.Value random;
    public final SliderSetting duration = new SliderSetting(this, "modules.settings.item_inspect.duration")
        .min(.6f).max(8).step(.1f).currentValue(2.8f).suffix("s");
    public final SliderSetting intensity = new SliderSetting(this, "modules.settings.item_inspect.intensity")
        .min(25).max(150).step(5).currentValue(100).suffix("%");
    public final ModeSetting hand = new ModeSetting(this, "modules.settings.item_inspect.hand");
    private final ModeSetting.Value mainHand = new ModeSetting.Value(hand, "modules.settings.item_inspect.main_hand").select();
    private final ModeSetting.Value offHand = new ModeSetting.Value(hand, "modules.settings.item_inspect.off_hand");

    private final InspectAnimation timeline = new InspectAnimation();
    private ClientWorld world;
    private ClientPlayerEntity player;
    private ItemStack inspected = ItemStack.EMPTY;
    private Hand activeHand = Hand.MAIN_HAND;
    private int slot, nextStyle;
    private InspectAnimation.Style lastStyle;

    public ItemInspect() {
        for (var style : InspectAnimation.Style.values())
            styles.put(style, new ModeSetting.Value(animation, "modules.settings.item_inspect." + style.name().toLowerCase(Locale.ROOT)));
        cycle = new ModeSetting.Value(animation, "modules.settings.item_inspect.cycle");
        random = new ModeSetting.Value(animation, "modules.settings.item_inspect.random");
        instance = this;
    }

    private final EventListener<KeyPressEvent> onKey = event -> {
        if (event.getAction() == GLFW.GLFW_PRESS && inspectKey.isKey(event.getKey())) inspect();
    };
    private final EventListener<MouseEvent> onMouse = event -> {
        if (event.getAction() == GLFW.GLFW_PRESS
            && (inspectKey.isKey(event.getButton()) || inspectKey.isKey(event.getButton() - 100))) inspect();
    };

    private boolean canInspect() {
        return isEnabled() && mc.player != null && mc.world != null && mc.currentScreen == null
            && mc.player.isAlive() && !mc.player.isSpectator() && mc.options.getPerspective().isFirstPerson()
            && mc.getCameraEntity() == mc.player && !dev.bkclient.systems.modules.modules.player.FreecamUtil.isActive();
    }

    private boolean interacting() {
        return mc.player.isUsingItem() || mc.player.handSwinging || mc.options.attackKey.isPressed()
            || mc.options.useKey.isPressed() || mc.interactionManager != null && mc.interactionManager.isBreakingBlock();
    }

    /** Starts only a local render animation; never changes inventory or sends an action packet. */
    public boolean inspect() {
        refresh();
        if (!canInspect() || mc.isPaused() || interacting()) return false;
        if (timeline.isActive()) { timeline.finish(System.nanoTime()); return false; }
        activeHand = offHand.isSelected() ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack stack = mc.player.getStackInHand(activeHand);
        // Maps have their own two-handed renderer and do not pass through the item hook.
        if (stack.isEmpty() || stack.isOf(Items.FILLED_MAP)) return false;
        InspectAnimation.Style style;
        if (cycle.isSelected()) {
            style = InspectAnimation.Style.values()[nextStyle];
            nextStyle = (nextStyle + 1) % InspectAnimation.Style.values().length;
        }
        else if (random.isSelected()) {
            var choices = InspectAnimation.Style.values();
            int index = ThreadLocalRandom.current().nextInt(choices.length - (lastStyle == null ? 0 : 1));
            if (lastStyle != null && index >= lastStyle.ordinal()) index++;
            style = choices[index];
        }
        else if (automatic.isSelected() && stack.getItem() instanceof SwordItem) style = InspectAnimation.Style.SPIN;
        else if (automatic.isSelected() && stack.getItem() instanceof MiningToolItem) style = InspectAnimation.Style.FLIP;
        else style = styles.entrySet().stream().filter(entry -> entry.getValue().isSelected())
            .map(java.util.Map.Entry::getKey).findFirst().orElse(InspectAnimation.Style.EXAMINE);
        inspected = stack.copy();
        world = mc.world;
        player = mc.player;
        slot = player.getInventory().selectedSlot;
        timeline.start(System.nanoTime(), style, duration.getCurrentValue(), intensity.getCurrentValue() / 100);
        lastStyle = style;
        return true;
    }

    @Override public void tick() { refresh(); }

    private void refresh() {
        if (!timeline.isActive()) return;
        if (!canInspect() || world != mc.world || player != mc.player || interacting()
            || (activeHand == Hand.MAIN_HAND && slot != player.getInventory().selectedSlot)
            || !ItemStack.areItemsAndComponentsEqual(inspected, player.getStackInHand(activeHand))) {
            onWorldChanged();
            return;
        }
        timeline.advance(System.nanoTime(), mc.isPaused());
        if (!timeline.isActive()) onWorldChanged();
    }

    public static boolean activeFor(Arm arm) {
        ItemInspect self = instance;
        if (self == null) return false;
        self.refresh();
        return self.timeline.isActive() && arm == (self.activeHand == Hand.MAIN_HAND
            ? self.player.getMainArm() : self.player.getMainArm().getOpposite());
    }

    public static InspectAnimation.Pose renderPose(LivingEntity entity, ItemStack stack, ModelTransformationMode mode) {
        if (mode != ModelTransformationMode.FIRST_PERSON_RIGHT_HAND && mode != ModelTransformationMode.FIRST_PERSON_LEFT_HAND)
            return null;
        Arm arm = mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND ? Arm.RIGHT : Arm.LEFT;
        if (!activeFor(arm) || entity != instance.player || !ItemStack.areItemsAndComponentsEqual(stack, instance.inspected))
            return null;
        return instance.timeline.pose(arm == Arm.LEFT);
    }

    public static void apply(MatrixStack matrices, InspectAnimation.Pose pose) {
        matrices.translate(pose.x(), pose.y(), pose.z());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(pose.yaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.pitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pose.roll()));
    }

    @Override public void onDisable() { onWorldChanged(); }
    public void onWorldChanged() { timeline.reset(); inspected = ItemStack.EMPTY; world = null; player = null; }
}
