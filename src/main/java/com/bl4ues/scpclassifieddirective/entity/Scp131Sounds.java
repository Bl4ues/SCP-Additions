package com.bl4ues.scpclassifieddirective.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/**
 * SCP-131 sound events registered by the canonical SCP: Classified Directive mod while
 * continuing to use audio files from the migrated scp_classified_directive resource pack.
 */
public final class Scp131Sounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ScpClassifiedDirectiveMod.MODID);

	public static final RegistryObject<SoundEvent> EYE_POD_VOICE = REGISTRY.register("eye_pod_voice", () ->
			SoundEvent.createVariableRangeEvent(new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "eye_pod_voice")));
	public static final RegistryObject<SoundEvent> EYE_POD_IDLE = REGISTRY.register("eye_pod_idle", () ->
			SoundEvent.createVariableRangeEvent(new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "eye_pod_idle")));
	public static final RegistryObject<SoundEvent> EYE_POD_MOVE = REGISTRY.register("eye_pod_move", () ->
			SoundEvent.createVariableRangeEvent(new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "eye_pod_move")));

	private Scp131Sounds() {
	}
}
