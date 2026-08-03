package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Client-local sound event used by the continuously tracked Roomba loop. */
public final class RoombaSounds {
    public static final SoundEvent LOOP = SoundEvent.createVariableRangeEvent(
            new ResourceLocation(ScpAdditionsMod.MODID, "roomba_loop"));

    private RoombaSounds() {
    }
}
