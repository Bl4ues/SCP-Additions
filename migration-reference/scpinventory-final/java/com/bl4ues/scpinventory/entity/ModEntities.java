package com.bl4ues.scpinventory.entity;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ScpInventoryMod.MODID);

    public static final RegistryObject<EntityType<Scp173Entity>> SCP_173 = ENTITY_TYPES.register(
            "scp_173",
            () -> EntityType.Builder.of(Scp173Entity::new, MobCategory.MONSTER)
                    .sized(0.85F, 1.95F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("scp_173")
    );

    public static final RegistryObject<EntityType<Scp131AEntity>> SCP_131_A = ENTITY_TYPES.register(
            "scp_131_a",
            () -> EntityType.Builder.of(Scp131AEntity::new, MobCategory.CREATURE)
                    .sized(0.70F, 1.00F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("scp_131_a")
    );

    public static final RegistryObject<EntityType<Scp131BEntity>> SCP_131_B = ENTITY_TYPES.register(
            "scp_131_b",
            () -> EntityType.Builder.of(Scp131BEntity::new, MobCategory.CREATURE)
                    .sized(0.70F, 1.00F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("scp_131_b")
    );

    private ModEntities() {
    }
}
