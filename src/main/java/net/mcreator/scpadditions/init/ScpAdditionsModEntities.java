package net.mcreator.scpadditions.init;

import java.util.function.Supplier;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.AbstractScp131Entity;
import net.mcreator.scpadditions.entity.Scp131AEntity;
import net.mcreator.scpadditions.entity.Scp131BEntity;
import net.mcreator.scpadditions.entity.Scp173Entity;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScpAdditionsModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ScpAdditionsMod.MODID);

	public static final Supplier<EntityType<Scp131AEntity>> SCP_131_A = REGISTRY.register("scp_131_a",
			() -> EntityType.Builder.of(Scp131AEntity::new, MobCategory.CREATURE)
					.sized(0.70F, 1.00F).clientTrackingRange(10).updateInterval(2).build("scp_131_a"));

	public static final Supplier<EntityType<Scp131BEntity>> SCP_131_B = REGISTRY.register("scp_131_b",
			() -> EntityType.Builder.of(Scp131BEntity::new, MobCategory.CREATURE)
					.sized(0.70F, 1.00F).clientTrackingRange(10).updateInterval(2).build("scp_131_b"));

	public static final Supplier<EntityType<Scp173Entity>> SCP_173 = REGISTRY.register("scp_173",
			() -> EntityType.Builder.of(Scp173Entity::new, MobCategory.MONSTER)
					.sized(0.85F, 1.95F).clientTrackingRange(12).updateInterval(1).build("scp_173"));

	@SubscribeEvent
	public static void createAttributes(EntityAttributeCreationEvent event) {
		event.put(SCP_131_A.get(), AbstractScp131Entity.createAttributes().build());
		event.put(SCP_131_B.get(), AbstractScp131Entity.createAttributes().build());
		event.put(SCP_173.get(), Scp173Entity.createAttributes()
				.add(Attributes.ARMOR, 80.0D)
				.add(Attributes.ARMOR_TOUGHNESS, 40.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
				.build());
	}
}
