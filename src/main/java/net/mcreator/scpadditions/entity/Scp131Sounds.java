package net.mcreator.scpadditions.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * SCP-131 sound events registered by the canonical SCP Additions mod while
 * continuing to use audio files from the migrated scpinventory resource pack.
 */
public final class Scp131Sounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ScpAdditionsMod.MODID);

	public static final RegistryObject<SoundEvent> EYE_POD_VOICE = REGISTRY.register("eye_pod_voice", () ->
			SoundEvent.createVariableRangeEvent(new ResourceLocation(ScpAdditionsMod.MODID, "eye_pod_voice")));
	public static final RegistryObject<SoundEvent> EYE_POD_IDLE = REGISTRY.register("eye_pod_idle", () ->
			SoundEvent.createVariableRangeEvent(new ResourceLocation(ScpAdditionsMod.MODID, "eye_pod_idle")));
	public static final RegistryObject<SoundEvent> EYE_POD_MOVE = REGISTRY.register("eye_pod_move", () ->
			SoundEvent.createVariableRangeEvent(new ResourceLocation(ScpAdditionsMod.MODID, "eye_pod_move")));

	private Scp131Sounds() {
	}
}
