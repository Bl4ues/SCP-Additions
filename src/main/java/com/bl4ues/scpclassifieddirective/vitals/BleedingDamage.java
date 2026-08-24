package com.bl4ues.scpclassifieddirective.vitals;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/** Dedicated continuous-damage source for persistent blood loss. */
public final class BleedingDamage {
    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "bleeding"));

    private BleedingDamage() {
    }

    public static DamageSource source(ServerLevel level) {
        return new DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TYPE)) {
            @Override
            public Component getLocalizedDeathMessage(LivingEntity entity) {
                return Component.literal(entity.getDisplayName().getString()
                        + " bled out");
            }
        };
    }
}
