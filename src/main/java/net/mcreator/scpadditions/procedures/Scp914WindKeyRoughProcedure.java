package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.horse.DonkeyEntity;
import net.minecraft.entity.passive.fish.CodEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.monster.GhastEntity;
import net.minecraft.entity.monster.EvokerEntity;
import net.minecraft.entity.monster.EndermiteEntity;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.ElderGuardianEntity;
import net.minecraft.entity.monster.DrownedEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.CaveSpiderEntity;
import net.minecraft.entity.monster.BlazeEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.block.Blocks;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;

import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

public class Scp914WindKeyRoughProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure Scp914WindKeyRough!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency x for procedure Scp914WindKeyRough!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency y for procedure Scp914WindKeyRough!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency z for procedure Scp914WindKeyRough!");
			return;
		}
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure Scp914WindKeyRough!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		double x = dependencies.get("x") instanceof Integer ? (int) dependencies.get("x") : (double) dependencies.get("x");
		double y = dependencies.get("y") instanceof Integer ? (int) dependencies.get("y") : (double) dependencies.get("y");
		double z = dependencies.get("z") instanceof Integer ? (int) dependencies.get("z") : (double) dependencies.get("z");
		Entity entity = (Entity) dependencies.get("entity");
		if (entity instanceof ServerPlayerEntity) {
			Advancement _adv = ((MinecraftServer) ((ServerPlayerEntity) entity).server).getAdvancementManager()
					.getAdvancement(new ResourceLocation("scp_additions:scp_914_achievement"));
			AdvancementProgress _ap = ((ServerPlayerEntity) entity).getAdvancements().getProgress(_adv);
			if (!_ap.isDone()) {
				Iterator _iterator = _ap.getRemaningCriteria().iterator();
				while (_iterator.hasNext()) {
					String _criterion = (String) _iterator.next();
					((ServerPlayerEntity) entity).getAdvancements().grantCriterion(_adv, _criterion);
				}
			}
		}
		if (world instanceof World && !world.isRemote()) {
			((World) world).playSound(null, new BlockPos(x, y, z),
					(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914refining")),
					SoundCategory.NEUTRAL, (float) 1, (float) 1);
		} else {
			((World) world).playSound(x, y, z,
					(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914refining")),
					SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
		}
		ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		if (((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
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
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STONE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STONE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRANITE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_GRANITE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRANITE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_GRANITE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRANITE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_GRANITE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRANITE_WALL.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIORITE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIORITE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIORITE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIORITE_WALL.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_DIORITE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIORITE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIORITE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIORITE_WALL.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_DIORITE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_DIORITE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_DIORITE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ANDESITE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ANDESITE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ANDESITE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ANDESITE_WALL.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_ANDESITE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_ANDESITE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_ANDESITE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SMOOTH_STONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.COBBLESTONE));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRASS_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRASS_PATH.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.MYCELIUM.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIRT.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.COARSE_DIRT.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.PODZOL.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.COBBLESTONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.MOSSY_COBBLESTONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.VINE));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.MOSSY_COBBLESTONE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.VINE));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.MOSSY_COBBLESTONE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.VINE));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.OAK_PLANKS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SPRUCE_PLANKS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BIRCH_PLANKS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.JUNGLE_PLANKS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ACACIA_PLANKS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DARK_OAK_PLANKS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CRIMSON_PLANKS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Blocks.CRIMSON_FUNGUS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.WARPED_PLANKS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Blocks.WARPED_FUNGUS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.OAK_SAPLING.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SPRUCE_SAPLING.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BIRCH_SAPLING.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.JUNGLE_SAPLING.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ACACIA_SAPLING.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DARK_OAK_SAPLING.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SUGAR_CANE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.SUGAR));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SAND.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.DEAD_BUSH));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.RED_SAND.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.DEAD_BUSH));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRAVEL.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.MAGMA_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.BLAZE_POWDER));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.OBSIDIAN.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.BLACK_DYE));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CRYING_OBSIDIAN.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.BLACK_DYE));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BLACKSTONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BLACKSTONE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BLACKSTONE_WALL.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BLACKSTONE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_BLACKSTONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null))))
							.getItem() == Blocks.POLISHED_BLACKSTONE_BRICKS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null))))
							.getItem() == Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null))))
							.getItem() == Blocks.CHISELED_POLISHED_BLACKSTONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null))))
							.getItem() == Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null))))
							.getItem() == Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null))))
							.getItem() == Blocks.POLISHED_BLACKSTONE_BRICK_WALL.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null))))
							.getItem() == Blocks.POLISHED_BLACKSTONE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_BLACKSTONE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_BLACKSTONE_WALL.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.COAL_ORE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.COAL));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.IRON_ORE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.IRON_NUGGET));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.REDSTONE_ORE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.REDSTONE));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GOLD_ORE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.GOLD_NUGGET));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.LAPIS_ORE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.LAPIS_LAZULI));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIAMOND_ORE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.DIAMOND));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.EMERALD_ORE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.EMERALD));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.NETHER_QUARTZ_ORE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.QUARTZ));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.NETHER_GOLD_ORE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.GOLD_NUGGET));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.NETHER_WART));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GILDED_BLACKSTONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.GOLD_NUGGET));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FLINT));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.COAL_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.COAL));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.IRON_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.IRON_NUGGET));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.REDSTONE_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.REDSTONE));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GOLD_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.GOLD_NUGGET));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.LAPIS_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.LAPIS_LAZULI));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIAMOND_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.DIAMOND));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.EMERALD_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.EMERALD));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.NETHERITE_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Items.NETHERITE_SCRAP));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SMOOTH_QUARTZ.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.QUARTZ));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CHISELED_QUARTZ_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.QUARTZ));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.QUARTZ_PILLAR.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.QUARTZ));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.QUARTZ_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.QUARTZ));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.QUARTZ_BRICKS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.QUARTZ));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SMOOTH_QUARTZ_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.QUARTZ));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SMOOTH_QUARTZ_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.QUARTZ));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SLIME_BLOCK.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.SLIME_BALL));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ANCIENT_DEBRIS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Items.NETHERITE_SCRAP));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.OAK_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SPRUCE_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BIRCH_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.JUNGLE_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ACACIA_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DARK_OAK_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.WARPED_STEM.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Blocks.WARPED_FUNGUS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CRIMSON_STEM.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Blocks.CRIMSON_FUNGUS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_OAK_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_SPRUCE_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_BIRCH_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_JUNGLE_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_ACACIA_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_DARK_OAK_LOG.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_WARPED_STEM.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Blocks.WARPED_FUNGUS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_CRIMSON_STEM.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Blocks.CRIMSON_FUNGUS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.OAK_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SPRUCE_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BIRCH_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.JUNGLE_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ACACIA_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DARK_OAK_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.WARPED_HYPHAE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Blocks.WARPED_FUNGUS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CRIMSON_HYPHAE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Blocks.CRIMSON_FUNGUS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_OAK_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_SPRUCE_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_BIRCH_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_JUNGLE_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_ACACIA_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_DARK_OAK_WOOD.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_WARPED_HYPHAE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Blocks.WARPED_FUNGUS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_CRIMSON_HYPHAE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STICK));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
											new ItemStack(Blocks.CRIMSON_FUNGUS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.OAK_LEAVES.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SPRUCE_LEAVES.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BIRCH_LEAVES.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.JUNGLE_LEAVES.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ACACIA_LEAVES.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ACACIA_LEAVES.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DARK_OAK_LEAVES.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.WHEAT_SEEDS));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SPONGE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.HORN_CORAL));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.WET_SPONGE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.HORN_CORAL));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SANDSTONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SAND));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CHISELED_SANDSTONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SAND));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CUT_SANDSTONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SAND));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SANDSTONE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SAND));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SMOOTH_SANDSTONE.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SAND));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SMOOTH_SANDSTONE_STAIRS.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SAND));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SMOOTH_SANDSTONE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SAND));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CUT_SANDSTONE_SLAB.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SAND));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					if ((new Object() {
						public ItemStack entityToItem(Entity _ent) {
							if (_ent instanceof ItemEntity) {
								return ((ItemEntity) _ent).getItem();
							}
							return ItemStack.EMPTY;
						}
					}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
							(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SANDSTONE_WALL.asItem()) {
						if (world instanceof ServerWorld) {
							((World) world).getServer().getCommandManager().handleCommand(
									new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4,
											"", new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
									"/kill @e[distance=..3]");
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
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SAND));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
								ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
								ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 160);
					}
					MinecraftForge.EVENT_BUS.unregister(this);
				}
			}.start(world, (int) 30);
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
				if (((Entity) world.getEntitiesWithinAABB(PlayerEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					if (world instanceof World && !world.isRemote()) {
						((World) world)
								.playSound(null, new BlockPos(x - 4, entity.getPosY(), entity.getPosZ()),
										(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
												.getValue(new ResourceLocation("scp_additions:scp914inside")),
										SoundCategory.NEUTRAL, (float) 1, (float) 1);
					} else {
						((World) world).playSound((x - 4), (entity.getPosY()), (entity.getPosZ()),
								(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
										.getValue(new ResourceLocation("scp_additions:scp914inside")),
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
							if (entity instanceof LivingEntity) {
								((LivingEntity) entity).attackEntityFrom(new DamageSource("scp914rough").setDamageBypassesArmor(), (float) 18);
							}
							if (world instanceof World && !world.isRemote()) {
								((World) world).playSound(null, new BlockPos(x + 4, y, z - 3),
										(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
												.getValue(new ResourceLocation("scp_additions:scp914death")),
										SoundCategory.NEUTRAL, (float) 1, (float) 1);
							} else {
								((World) world).playSound((x + 4), y, (z - 3),
										(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
												.getValue(new ResourceLocation("scp_additions:scp914death")),
										SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
							}
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 150);
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
								((LivingEntity) entity).attackEntityFrom(new DamageSource("scp914rough").setDamageBypassesArmor(), (float) 50);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(BatEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
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
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(BeeEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(BlazeEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.BLAZE_POWDER));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(CatEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(CaveSpiderEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.STRING));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(ChickenEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.CHICKEN));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.FEATHER));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(CodEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.COD));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(CowEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.BEEF));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.LEATHER));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(CreeperEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.GUNPOWDER));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(DolphinEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					if (world instanceof World && !world.isRemote()) {
						((World) world)
								.playSound(null, new BlockPos(x, y, z),
										(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
												.getValue(new ResourceLocation("scp_additions:scp914refining")),
										SoundCategory.NEUTRAL, (float) 1, (float) 1);
					} else {
						((World) world).playSound(x, y, z,
								(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
										.getValue(new ResourceLocation("scp_additions:scp914refining")),
								SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(DonkeyEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.LEATHER));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(EnderDragonEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(DrownedEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.ROTTEN_FLESH));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(ElderGuardianEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(EndermanEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.ENDER_PEARL));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(EndermiteEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(EvokerEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(FoxEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				if (((Entity) world.getEntitiesWithinAABB(GhastEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
					{
						List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d),
								(z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf((x - 4), y, (z - 3))).collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!entityiterator.world.isRemote())
								entityiterator.remove();
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (true);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
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
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.GHAST_TEAR));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
							ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
							ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 160);
				}
				MinecraftForge.EVENT_BUS.unregister(this);
			}
		}.start(world, (int) 30);
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
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
				MinecraftForge.EVENT_BUS.unregister(this);
			}
		}.start(world, (int) 190);
	}
}
