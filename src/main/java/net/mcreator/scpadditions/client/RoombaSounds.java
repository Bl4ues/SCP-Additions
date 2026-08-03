package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** Client-local sound event used by the continuously tracked Roomba loop. */
public final class RoombaSounds {
    public static final SoundEvent LOOP = SoundEvent.createVariableRangeEvent(
            new ResourceLocation("scp_additions_roomba", "loop"));

    private RoombaSounds() {
    }
}
