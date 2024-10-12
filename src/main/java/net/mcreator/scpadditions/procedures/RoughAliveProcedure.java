package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.entity.Scp0591infected3Entity;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.List;
import java.util.Comparator;

public class RoughAliveProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (!world.getEntitiesOfClass(Player.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914inside")), SoundSource.NEUTRAL, 1, 1);
				} else {
					_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914inside")), SoundSource.NEUTRAL, 1, 1, false);
				}
			}
			ScpAdditionsMod.queueServerWork(150, () -> {
				{
					Entity _ent = entity;
					_ent.teleportTo((x + 4), (entity.getY()), (entity.getZ()));
					if (_ent instanceof ServerPlayer _serverPlayer)
						_serverPlayer.connection.teleport((x + 4), (entity.getY()), (entity.getZ()), _ent.getYRot(), _ent.getXRot());
				}
				if (entity instanceof LivingEntity _entity)
					_entity.hurt(new DamageSource(_entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)) {
						@Override
						public Component getLocalizedDeathMessage(LivingEntity _msgEntity) {
							String _translatekey = "death.attack." + "scp914rough";
							if (this.getEntity() == null && this.getDirectEntity() == null) {
								return _msgEntity.getKillCredit() != null
										? Component.translatable(_translatekey + ".player", _msgEntity.getDisplayName(), _msgEntity.getKillCredit().getDisplayName())
										: Component.translatable(_translatekey, _msgEntity.getDisplayName());
							} else {
								Component _component = this.getEntity() == null ? this.getDirectEntity().getDisplayName() : this.getEntity().getDisplayName();
								ItemStack _itemstack = ItemStack.EMPTY;
								if (this.getEntity() instanceof LivingEntity _livingentity)
									_itemstack = _livingentity.getMainHandItem();
								return !_itemstack.isEmpty() && _itemstack.hasCustomHoverName()
										? Component.translatable(_translatekey + ".item", _msgEntity.getDisplayName(), _component, _itemstack.getDisplayName())
										: Component.translatable(_translatekey, _msgEntity.getDisplayName(), _component);
							}
						}
					}, 18);
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x + 4, y, z - 3), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914death")), SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound((x + 4), y, (z - 3), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914death")), SoundSource.NEUTRAL, 1, 1, false);
					}
				}
			});
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (entity instanceof LivingEntity _entity)
					_entity.hurt(new DamageSource(_entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)) {
						@Override
						public Component getLocalizedDeathMessage(LivingEntity _msgEntity) {
							String _translatekey = "death.attack." + "scp914rough";
							if (this.getEntity() == null && this.getDirectEntity() == null) {
								return _msgEntity.getKillCredit() != null
										? Component.translatable(_translatekey + ".player", _msgEntity.getDisplayName(), _msgEntity.getKillCredit().getDisplayName())
										: Component.translatable(_translatekey, _msgEntity.getDisplayName());
							} else {
								Component _component = this.getEntity() == null ? this.getDirectEntity().getDisplayName() : this.getEntity().getDisplayName();
								ItemStack _itemstack = ItemStack.EMPTY;
								if (this.getEntity() instanceof LivingEntity _livingentity)
									_itemstack = _livingentity.getMainHandItem();
								return !_itemstack.isEmpty() && _itemstack.hasCustomHoverName()
										? Component.translatable(_translatekey + ".item", _msgEntity.getDisplayName(), _component, _itemstack.getDisplayName())
										: Component.translatable(_translatekey, _msgEntity.getDisplayName(), _component);
							}
						}
					}, 50);
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Bat.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Bee.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.HONEYCOMB));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Blaze.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.BLAZE_POWDER));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Boat.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				for (int index0 = 0; index0 < 3; index0++) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Cat.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(CaveSpider.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.STRING));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Chicken.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.CHICKEN));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.FEATHER));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Cod.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.COD));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Cow.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.BEEF));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.LEATHER));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Creeper.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.GUNPOWDER));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Dolphin.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Donkey.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.LEATHER));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(EnderDragon.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Drowned.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.ROTTEN_FLESH));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(ElderGuardian.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(EnderMan.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.ENDER_PEARL));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Endermite.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Evoker.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Fox.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Ghast.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.GHAST_TEAR));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Guardian.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Hoglin.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.PORKCHOP));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Horse.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.LEATHER));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Husk.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.ROTTEN_FLESH));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Illusioner.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(IronGolem.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.IRON_NUGGET));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Llama.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Mule.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.LEATHER));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Ocelot.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Panda.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Parrot.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.FEATHER));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Phantom.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.PHANTOM_MEMBRANE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Pig.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.PORKCHOP));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Piglin.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(PiglinBrute.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Pillager.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(PolarBear.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Pufferfish.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.PUFFERFISH));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Rabbit.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.RABBIT));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.RABBIT_HIDE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Ravager.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Salmon.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.SALMON));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Sheep.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.MUTTON));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WHITE_WOOL));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Shulker.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.SHULKER_SHELL));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Silverfish.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Skeleton.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.BONE_MEAL));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(SkeletonHorse.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.BONE_MEAL));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Slime.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.SLIME_BALL));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(SnowGolem.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.SNOWBALL));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Spider.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.STRING));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Squid.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.INK_SAC));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Stray.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.BONE_MEAL));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Strider.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(TraderLlama.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(TropicalFish.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.TROPICAL_FISH));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Turtle.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.TURTLE_HELMET));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Vex.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Villager.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Vindicator.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(WanderingTrader.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Witch.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(WitherBoss.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.NETHER_STAR));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(WitherSkeleton.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.BONE_MEAL));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Wolf.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Zoglin.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.ROTTEN_FLESH));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Zombie.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.ROTTEN_FLESH));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(ZombieHorse.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.ROTTEN_FLESH));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.LEATHER));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(ZombieVillager.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.ROTTEN_FLESH));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(ZombifiedPiglin.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.ROTTEN_FLESH));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(MushroomCow.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.BEEF));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.LEATHER));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.RED_MUSHROOM));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if (!world.getEntitiesOfClass(Scp0591infected3Entity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 4, 4, 4), e -> true).isEmpty()) {
			{
				final Vec3 _center = new Vec3((x - 4), y, (z - 3));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModBlocks.SCP_059_1.get()));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
	}
}
