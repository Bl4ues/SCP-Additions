package net.mcreator.scpadditions.entity;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class Scp173Sounds {
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ScpAdditionsMod.MODID);

    public static final Supplier<SoundEvent> SCARE = register("scare");
    public static final Supplier<SoundEvent> HORROR = register("horror");
    public static final Supplier<SoundEvent> STATUE_DEATH = register("statue_death");
    public static final Supplier<SoundEvent> STONE_SCRAP = register("stone_scrap");
    public static final Supplier<SoundEvent> NECK_SNAP = register("neck_snap");
    public static final Supplier<SoundEvent> RATTLE = register("rattle");

    private Scp173Sounds() {
    }

    private static Supplier<SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ScpAdditionsMod.MODID, name);
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
