package dev.bkclient.combatengine.bkengine.module.impl.combat.aura;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import dev.bkclient.combatengine.bkengine.module.impl.combat.aura.rotation.*;

@Getter
@RequiredArgsConstructor
public enum RotationType {

    SPOOKYTIME("SpookyTime", new SpookyTimeRotation()),
    MATRIX("Default", new MatrixRotation()),
    SNAP("Snap", new SnapRotation()),
    HVH("Ana", new HvHRotation());


    private final String name;
    private final RotationAura rotation;

}
