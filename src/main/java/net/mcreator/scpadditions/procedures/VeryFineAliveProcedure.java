package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.world.Explosion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.DamageSource;
import net.minecraft.potion.Effects;
import net.minecraft.potion.EffectInstance;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.horse.ZombieHorseEntity;
import net.minecraft.entity.passive.horse.TraderLlamaEntity;
import net.minecraft.entity.passive.horse.SkeletonHorseEntity;
import net.minecraft.entity.passive.horse.MuleEntity;
import net.minecraft.entity.passive.horse.LlamaEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.passive.horse.DonkeyEntity;
import net.minecraft.entity.passive.fish.TropicalFishEntity;
import net.minecraft.entity.passive.fish.SalmonEntity;
import net.minecraft.entity.passive.fish.PufferfishEntity;
import net.minecraft.entity.passive.fish.CodEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.monster.piglin.PiglinEntity;
import net.minecraft.entity.monster.piglin.PiglinBruteEntity;
import net.minecraft.entity.monster.ZombifiedPiglinEntity;
import net.minecraft.entity.monster.ZombieVillagerEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.monster.ZoglinEntity;
import net.minecraft.entity.monster.WitherSkeletonEntity;
import net.minecraft.entity.monster.WitchEntity;
import net.minecraft.entity.monster.VindicatorEntity;
import net.minecraft.entity.monster.VexEntity;
import net.minecraft.entity.monster.StrayEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SilverfishEntity;
import net.minecraft.entity.monster.ShulkerEntity;
import net.minecraft.entity.monster.RavagerEntity;
import net.minecraft.entity.monster.PillagerEntity;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.monster.MagmaCubeEntity;
import net.minecraft.entity.monster.IllusionerEntity;
import net.minecraft.entity.monster.HuskEntity;
import net.minecraft.entity.monster.HoglinEntity;
import net.minecraft.entity.monster.GuardianEntity;
import net.minecraft.entity.monster.GhastEntity;
import net.minecraft.entity.monster.EvokerEntity;
import net.minecraft.entity.monster.EndermiteEntity;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.ElderGuardianEntity;
import net.minecraft.entity.monster.DrownedEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.CaveSpiderEntity;
import net.minecraft.entity.monster.BlazeEntity;
import net.minecraft.entity.merchant.villager.WanderingTraderEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;

import net.mcreator.scpadditions.entity.Scp0591infected3Entity;
import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Map;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;

public class VeryFineAliveProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure VeryFineAlive!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency x for procedure VeryFineAlive!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency y for procedure VeryFineAlive!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency z for procedure VeryFineAlive!");
			return;
		}
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure VeryFineAlive!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		double x = dependencies.get("x") instanceof Integer ? (int) dependencies.get("x") : (double) dependencies.get("x");
		double y = dependencies.get("y") instanceof Integer ? (int) dependencies.get("y") : (double) dependencies.get("y");
		double z = dependencies.get("z") instanceof Integer ? (int) dependencies.get("z") : (double) dependencies.get("z");
		Entity entity = (Entity) dependencies.get("entity");
		if (((Entity) world.getEntitiesWithinAABB(PlayerEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			if (world instanceof World && !world.isRemote()) {
				((World) world).playSound(null, new BlockPos(x - 4, entity.getPosY(), entity.getPosZ()),
						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914inside")),
						SoundCategory.NEUTRAL, (float) 1, (float) 1);
			} else {
				((World) world).playSound((x - 4), (entity.getPosY()), (entity.getPosZ()),
						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914inside")),
						SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
			}
			new Object() {
				private int ticks = 0;
				private float waitTicks;
				private IWorld world;

				public void start(IWorld world, int waitTicks) {
					this.waitTicks = waitTicks;
					MinecraftForge.EVENT_BUS.register(this);
					this.world = world;
				}

				@SubscribeEvent
				public void tick(TickEvent.ServerTickEvent event) {
					if (event.phase == TickEvent.Phase.END) {
						this.ticks += 1;
						if (this.ticks >= this.waitTicks)
							run();
					}
				}

				private void run() {
					{
						Entity _ent = entity;
						_ent.setPositionAndUpdate((x + 4), (entity.getPosY()), (entity.getPosZ()));
						if (_ent instanceof ServerPlayerEntity) {
							((ServerPlayerEntity) _ent).connection.setPlayerLocation((x + 4), (entity.getPosY()), (entity.getPosZ()),
									_ent.rotationYaw, _ent.rotationPitch, Collections.emptySet());
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
					if (entity instanceof LivingEntity)
						((LivingEntity) entity).addPotionEffect(new EffectInstance(Effects.SPEED, (int) 300, (int) 5, (false), (false)));
					if (entity instanceof LivingEntity)
						((LivingEntity) entity).addPotionEffect(new EffectInstance(Effects.JUMP_BOOST, (int) 300, (int) 1, (false), (false)));
					if (entity instanceof LivingEntity)
						((LivingEntity) entity).addPotionEffect(new EffectInstance(Effects.HEALTH_BOOST, (int) 300, (int) 7, (false), (false)));
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (entity instanceof LivingEntity) {
								((LivingEntity) entity).attackEntityFrom(new DamageSource("scp914veryfine").setDamageBypassesArmor(), (float) 80);
							}
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 300);
					MinecraftForge.EVENT_BUS.unregister(this);
				}
			}.start(world, (int) 160);
		}
		if (((Entity) world.getEntitiesWithinAABB(BatEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new PhantomEntity(EntityType.PHANTOM, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(BeeEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new BeeEntity(EntityType.BEE, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(BlazeEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new BlazeEntity(EntityType.BLAZE, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(BoatEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new BoatEntity(EntityType.BOAT, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(CatEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new OcelotEntity(EntityType.OCELOT, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(CaveSpiderEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new CaveSpiderEntity(EntityType.CAVE_SPIDER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(ChickenEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new ParrotEntity(EntityType.PARROT, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(CodEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new GuardianEntity(EntityType.GUARDIAN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(CowEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new MooshroomEntity(EntityType.MOOSHROOM, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(CreeperEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof World && !((World) world).isRemote) {
								((World) world).createExplosion(null, (int) (x + 4), (int) y, (int) (z - 3), (float) 6, Explosion.Mode.NONE);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(DolphinEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new ElderGuardianEntity(EntityType.ELDER_GUARDIAN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(DonkeyEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new HorseEntity(EntityType.HORSE, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(EnderDragonEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new EnderDragonEntity(EntityType.ENDER_DRAGON, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(DrownedEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new DrownedEntity(EntityType.DROWNED, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(ElderGuardianEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new ElderGuardianEntity(EntityType.ELDER_GUARDIAN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(EndermanEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new EndermanEntity(EntityType.ENDERMAN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(EndermiteEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new EndermanEntity(EntityType.ENDERMAN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(EvokerEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new EvokerEntity(EntityType.EVOKER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(FoxEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new FoxEntity(EntityType.FOX, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(GhastEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new GhastEntity(EntityType.GHAST, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(GuardianEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new ElderGuardianEntity(EntityType.ELDER_GUARDIAN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(HoglinEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new HoglinEntity(EntityType.HOGLIN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(HorseEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new HorseEntity(EntityType.HORSE, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.JUMP_BOOST, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(HuskEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new HuskEntity(EntityType.HUSK, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(IllusionerEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new IllusionerEntity(EntityType.ILLUSIONER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(IronGolemEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new IronGolemEntity(EntityType.IRON_GOLEM, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 2, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.RESISTANCE, (int) 10000, (int) 2, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 2, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(LlamaEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new LlamaEntity(EntityType.LLAMA, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(MagmaCubeEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new MagmaCubeEntity(EntityType.MAGMA_CUBE, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(MooshroomEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new MooshroomEntity(EntityType.MOOSHROOM, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(MuleEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new MuleEntity(EntityType.MULE, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(OcelotEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new OcelotEntity(EntityType.OCELOT, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(PandaEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new PandaEntity(EntityType.PANDA, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(ParrotEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new ParrotEntity(EntityType.PARROT, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(PhantomEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new PhantomEntity(EntityType.PHANTOM, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.INVISIBILITY, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(PigEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new PiglinEntity(EntityType.PIGLIN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(PiglinEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new PiglinEntity(EntityType.PIGLIN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(PiglinBruteEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new PiglinBruteEntity(EntityType.field_242287_aj, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(PillagerEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new PillagerEntity(EntityType.PILLAGER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(PolarBearEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new PolarBearEntity(EntityType.POLAR_BEAR, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(PufferfishEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new GuardianEntity(EntityType.GUARDIAN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(RabbitEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new RabbitEntity(EntityType.RABBIT, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(RavagerEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new RavagerEntity(EntityType.RAVAGER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(SalmonEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new GuardianEntity(EntityType.GUARDIAN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(SheepEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new SheepEntity(EntityType.SHEEP, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(ShulkerEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new ShulkerEntity(EntityType.SHULKER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(SilverfishEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new SilverfishEntity(EntityType.SILVERFISH, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(SkeletonEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new WitherSkeletonEntity(EntityType.WITHER_SKELETON, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(SkeletonHorseEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new SkeletonHorseEntity(EntityType.SKELETON_HORSE, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(SlimeEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new MagmaCubeEntity(EntityType.MAGMA_CUBE, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(SnowGolemEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new SnowGolemEntity(EntityType.SNOW_GOLEM, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(SpiderEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new SpiderEntity(EntityType.SPIDER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(SquidEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new SquidEntity(EntityType.SQUID, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(StrayEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new StrayEntity(EntityType.STRAY, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(StriderEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new StriderEntity(EntityType.STRIDER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(TraderLlamaEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new TraderLlamaEntity(EntityType.TRADER_LLAMA, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(TropicalFishEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new GuardianEntity(EntityType.GUARDIAN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(TurtleEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new TurtleEntity(EntityType.TURTLE, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(VexEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new VexEntity(EntityType.VEX, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(VillagerEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new VillagerEntity(EntityType.VILLAGER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(VindicatorEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new VindicatorEntity(EntityType.VINDICATOR, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(WanderingTraderEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new WanderingTraderEntity(EntityType.WANDERING_TRADER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(WitchEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new WitchEntity(EntityType.WITCH, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(WitherEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new WitherEntity(EntityType.WITHER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(WitherSkeletonEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new WitherSkeletonEntity(EntityType.WITHER_SKELETON, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(WolfEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new WolfEntity(EntityType.WOLF, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(ZoglinEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new ZoglinEntity(EntityType.ZOGLIN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(ZombieEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new ZombieEntity(EntityType.ZOMBIE, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(ZombieHorseEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new ZombieHorseEntity(EntityType.ZOMBIE_HORSE, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(ZombieVillagerEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new ZombieVillagerEntity(EntityType.ZOMBIE_VILLAGER, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(ZombifiedPiglinEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new ZombifiedPiglinEntity(EntityType.ZOMBIFIED_PIGLIN, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
		if (((Entity) world.getEntitiesWithinAABB(Scp0591infected3Entity.CustomEntity.class,
				new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			{
				List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
						new AxisAlignedBB((x - 4) - (4 / 2d), y - (4 / 2d), (z - 3) - (4 / 2d), (x - 4) + (4 / 2d), y + (4 / 2d), (z - 3) + (4 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!entityiterator.world.isRemote())
						entityiterator.remove();
					new Object() {
						private int ticks = 0;
						private float waitTicks;
						private IWorld world;

						public void start(IWorld world, int waitTicks) {
							this.waitTicks = waitTicks;
							MinecraftForge.EVENT_BUS.register(this);
							this.world = world;
						}

						@SubscribeEvent
						public void tick(TickEvent.ServerTickEvent event) {
							if (event.phase == TickEvent.Phase.END) {
								this.ticks += 1;
								if (this.ticks >= this.waitTicks)
									run();
							}
						}

						private void run() {
							if (world instanceof ServerWorld) {
								Entity entityToSpawn = new Scp0591infected3Entity.CustomEntity(Scp0591infected3Entity.entity, (World) world);
								entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
								entityToSpawn.setRenderYawOffset((float) 0);
								entityToSpawn.setRotationYawHead((float) 0);
								entityToSpawn.setMotion(0, 0, 0);
								if (entityToSpawn instanceof MobEntity)
									((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
											world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED,
											(ILivingEntityData) null, (CompoundNBT) null);
								world.addEntity(entityToSpawn);
							}
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.SPEED, (int) 10000, (int) 1, (false), (false)));
							if (entityiterator instanceof LivingEntity)
								((LivingEntity) entityiterator)
										.addPotionEffect(new EffectInstance(Effects.STRENGTH, (int) 10000, (int) 1, (false), (false)));
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
			}
		}
	}
}
