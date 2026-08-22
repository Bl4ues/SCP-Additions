package com.bl4ues.scpclassifieddirective.acoustics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.Set;

/**
 * Bridges authoritative world sounds whose gameplay meaning is clearer at the
 * moment the sound actually plays than at an earlier input event.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldSoundAcousticEvents {
    private static final Set<String> FACILITY_DOOR_SOUNDS = Set.of(
            "unity_door_opening",
            "unity_door_closing",
            "unity_door_open",
            "unity_door_close",
            "unity_bath_open",
            "unity_bath_close",
            "unity_office_open",
            "unity_office_close");

    private WorldSoundAcousticEvents() {
    }

    @SubscribeEvent
    public static void onPositionSound(PlayLevelSoundEvent.AtPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.getSound() == null) {
            return;
        }

        ResourceLocation soundId = event.getSound().value().getLocation();
        if (!ScpClassifiedDirectiveMod.MODID.equals(soundId.getNamespace())
                || !FACILITY_DOOR_SOUNDS.contains(soundId.getPath())) {
            return;
        }

        // Trigger from the actual door transition, not merely from a click that
        // might fail or be cancelled. Source entity is intentionally null: the
        // evidence is the door itself, at its physical location.
        AcousticStimulusSystem.emit(level, event.getPosition(),
                AcousticCategory.DOOR,
                Math.max(0.65F, Math.min(1.35F, event.getNewVolume())), null);
    }
}
