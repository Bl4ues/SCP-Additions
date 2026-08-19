package net.mcreator.scpadditions.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.AbstractScp131Entity;
import net.mcreator.scpadditions.entity.PlayerCorpseEntity;
import net.mcreator.scpadditions.entity.RoombaEntity;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.entity.Scp131AEntity;
import net.mcreator.scpadditions.entity.Scp131BEntity;
import net.mcreator.scpadditions.entity.Scp173Entity;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScpAdditionsModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,
                    ScpAdditionsMod.MODID);

    public static final RegistryObject<EntityType<Scp106Entity>> SCP_106 =
            REGISTRY.register("scp_106", () -> EntityType.Builder.of(
                    Scp106Entity::new, MobCategory.MONSTER)
                    .sized(0.90F, 2.00F).clientTrackingRange(12)
                    .updateInterval(2).build("scp_106"));

    public static final RegistryObject<EntityType<Scp131AEntity>> SCP_131_A =
            REGISTRY.register("scp_131_a", () -> EntityType.Builder.of(
                    Scp131AEntity::new, MobCategory.CREATURE)
                    .sized(0.70F, 1.00F).clientTrackingRange(10)
                    .updateInterval(2).build("scp_131_a"));

    public static final RegistryObject<EntityType<Scp131BEntity>> SCP_131_B =
            REGISTRY.register("scp_131_b", () -> EntityType.Builder.of(
                    Scp131BEntity::new, MobCategory.CREATURE)
                    .sized(0.70F, 1.00F).clientTrackingRange(10)
                    .updateInterval(2).build("scp_131_b"));

    public static final RegistryObject<EntityType<Scp173Entity>> SCP_173 =
            REGISTRY.register("scp_173", () -> EntityType.Builder.of(
                    Scp173Entity::new, MobCategory.MONSTER)
                    // Keep the rendered statue broad, but use a player-like
                    // collision footprint so slight path-node offsets cannot
                    // wedge it against one-block facility door frames.
                    .sized(0.70F, 1.90F).clientTrackingRange(12)
                    .updateInterval(1).build("scp_173"));

    public static final RegistryObject<EntityType<RoombaEntity>> ROOMBA =
            REGISTRY.register("roomba", () -> EntityType.Builder.of(
                    RoombaEntity::new, MobCategory.CREATURE)
                    .sized(0.62F, 0.22F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .fireImmune()
                    .build("roomba"));

    public static final RegistryObject<EntityType<PlayerCorpseEntity>> PLAYER_CORPSE =
            REGISTRY.register("player_corpse", () -> EntityType.Builder.of(
                    PlayerCorpseEntity::new, MobCategory.MISC)
                    .sized(0.72F, 0.42F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("player_corpse"));

    @SubscribeEvent
    public static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(SCP_106.get(), Scp106Entity.createAttributes().build());
        event.put(SCP_131_A.get(),
                AbstractScp131Entity.createAttributes().build());
        event.put(SCP_131_B.get(),
                AbstractScp131Entity.createAttributes().build());
        event.put(SCP_173.get(), Scp173Entity.createAttributes()
                .add(Attributes.ARMOR, 80.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .build());
        event.put(ROOMBA.get(), RoombaEntity.createAttributes().build());
        event.put(PLAYER_CORPSE.get(), PlayerCorpseEntity.createAttributes().build());
    }
}
