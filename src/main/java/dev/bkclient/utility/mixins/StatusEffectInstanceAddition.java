package dev.bkclient.utility.mixins;

import dev.bkclient.ui.components.animated.AnimatedNumber;
import dev.bkclient.utility.animation.base.Animation;

public interface StatusEffectInstanceAddition {
   Animation bkclient$getAnimPotion();

   AnimatedNumber bkclient$getTimeAnimation();
}
