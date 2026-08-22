package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Client-local sound event used by the continuously tracked Roomba loop. */
public final class RoombaSounds {
    public static final SoundEvent LOOP = SoundEvent.createVariableRangeEvent(
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "roomba_loop"));

    private RoombaSounds() {
    }
}
