package net.mcreator.scpadditions.entity;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * SCP-131 sound events registered by the canonical SCP Additions mod while
 * continuing to use audio files from the migrated scpinventory resource pack.
 */
public final class Scp131Sounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ScpAdditionsMod.MODID);

	public static final Supplier<SoundEvent> EYE_POD_VOICE = REGISTRY.register("eye_pod_voice", () ->
			SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ScpAdditionsMod.MODID, "eye_pod_voice")));
	public static final Supplier<SoundEvent> EYE_POD_IDLE = REGISTRY.register("eye_pod_idle", () ->
			SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ScpAdditionsMod.MODID, "eye_pod_idle")));
	public static final Supplier<SoundEvent> EYE_POD_MOVE = REGISTRY.register("eye_pod_move", () ->
			SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ScpAdditionsMod.MODID, "eye_pod_move")));

	private Scp131Sounds() {
	}
}
