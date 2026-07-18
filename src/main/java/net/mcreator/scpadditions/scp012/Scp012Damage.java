package net.mcreator.scpadditions.scp012;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class Scp012Damage {
    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ScpAdditionsMod.MODID, "scp_012"));

    private Scp012Damage() {
    }

    public static DamageSource source(ServerLevel level) {
        return new DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TYPE));
    }
}
