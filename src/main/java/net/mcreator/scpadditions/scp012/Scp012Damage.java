package net.mcreator.scpadditions.scp012;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class Scp012Damage {
    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ScpAdditionsMod.MODID, "scp_012"));

    private Scp012Damage() {
    }

    public static DamageSource source(ServerLevel level) {
        return new DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TYPE)) {
            @Override
            public Component getLocalizedDeathMessage(LivingEntity entity) {
                return Component.literal(entity.getDisplayName().getString()
                        + " could not complete SCP-012");
            }
        };
    }
}
