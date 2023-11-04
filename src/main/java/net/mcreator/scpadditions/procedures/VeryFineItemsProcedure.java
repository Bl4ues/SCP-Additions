package net.mcreator.scpadditions.procedures;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.world.Explosion;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.block.Blocks;

import net.mcreator.scpadditions.item.Scp330BlueCandyItem;
import net.mcreator.scpadditions.item.PlayingCardItem;
import net.mcreator.scpadditions.item.PiecesOfPaperItem;
import net.mcreator.scpadditions.item.Level6KeycardItem;
import net.mcreator.scpadditions.item.Level5KeycardItem;
import net.mcreator.scpadditions.item.Level4KeycardItem;
import net.mcreator.scpadditions.item.Level3KeycardItem;
import net.mcreator.scpadditions.item.Level2KeycardItem;
import net.mcreator.scpadditions.item.Level1KeycardItem;
import net.mcreator.scpadditions.item.CreditCardItem;
import net.mcreator.scpadditions.item.CoinItem;
import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.stream.Stream;
import java.util.function.Function;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.AbstractMap;

public class VeryFineItemsProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure VeryFineItems!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency x for procedure VeryFineItems!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency y for procedure VeryFineItems!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency z for procedure VeryFineItems!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		double x = dependencies.get("x") instanceof Integer ? (int) dependencies.get("x") : (double) dependencies.get("x");
		double y = dependencies.get("y") instanceof Integer ? (int) dependencies.get("y") : (double) dependencies.get("y");
		double z = dependencies.get("z") instanceof Integer ? (int) dependencies.get("z") : (double) dependencies.get("z");
		if ((new Object() {
			public ItemStack entityToItem(Entity _ent) {
				if (_ent instanceof ItemEntity) {
					return ((ItemEntity) _ent).getItem();
				}
				return ItemStack.EMPTY;
			}
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Level1KeycardItem.block) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.05) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level6KeycardItem.block));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(CreditCardItem.block));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Level2KeycardItem.block) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.1) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level6KeycardItem.block));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(CreditCardItem.block));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Level3KeycardItem.block) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.2) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level6KeycardItem.block));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(CreditCardItem.block));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Level4KeycardItem.block) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.3) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level6KeycardItem.block));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(CreditCardItem.block));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Level5KeycardItem.block) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.4) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level6KeycardItem.block));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(CreditCardItem.block));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Level6KeycardItem.block) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(CreditCardItem.block));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == CreditCardItem.block) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level2KeycardItem.block));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == PlayingCardItem.block) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level2KeycardItem.block));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == PiecesOfPaperItem.block) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.4) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level2KeycardItem.block));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.PAPER));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == CoinItem.block) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level2KeycardItem.block));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STONE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STONE_STAIRS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STONE_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRANITE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_GRANITE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_GRANITE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRANITE_STAIRS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRANITE_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_GRANITE_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRANITE_WALL.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIORITE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIORITE_STAIRS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIORITE_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIORITE_WALL.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_DIORITE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_DIORITE_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_DIORITE_STAIRS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ANDESITE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ANDESITE_STAIRS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ANDESITE_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ANDESITE_WALL.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_ANDESITE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_ANDESITE_STAIRS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_ANDESITE_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SMOOTH_STONE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRASS_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_NYLIUM));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRASS_PATH.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_NYLIUM));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.MYCELIUM.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_NYLIUM));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIRT.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERRACK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.COARSE_DIRT.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERRACK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.PODZOL.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_NYLIUM));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.COBBLESTONE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_COBBLESTONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.MOSSY_COBBLESTONE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_COBBLESTONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.MOSSY_COBBLESTONE_STAIRS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_COBBLESTONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.MOSSY_COBBLESTONE_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_COBBLESTONE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.OAK_PLANKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SPRUCE_PLANKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BIRCH_PLANKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.JUNGLE_PLANKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ACACIA_PLANKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DARK_OAK_PLANKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CRIMSON_PLANKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.WARPED_PLANKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.OAK_SAPLING.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SPRUCE_SAPLING.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BIRCH_SAPLING.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.JUNGLE_SAPLING.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ACACIA_SAPLING.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DARK_OAK_SAPLING.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SUGAR_CANE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.BOOKSHELF));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SAND.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SAND));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.RED_SAND.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SAND));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GRAVEL.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SAND));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.MAGMA_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.OBSIDIAN.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRYING_OBSIDIAN));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CRYING_OBSIDIAN.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BLACKSTONE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BLACKSTONE_STAIRS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BLACKSTONE_WALL.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BLACKSTONE_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_BLACKSTONE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_BLACKSTONE_BRICKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CHISELED_POLISHED_BLACKSTONE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_BLACKSTONE_BRICK_WALL.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_BLACKSTONE_STAIRS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_BLACKSTONE_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.POLISHED_BLACKSTONE_WALL.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.COAL_ORE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.COAL_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.IRON_ORE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.IRON_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.REDSTONE_ORE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.REDSTONE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GOLD_ORE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.GOLD_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.LAPIS_ORE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.LAPIS_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIAMOND_ORE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.DIAMOND_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.EMERALD_ORE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.EMERALD_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.NETHER_QUARTZ_ORE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.QUARTZ_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.NETHER_GOLD_ORE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.GOLD_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GILDED_BLACKSTONE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.GOLD_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.COAL_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (world instanceof World && !((World) world).isRemote) {
						((World) world).createExplosion(null, (int) (x + 4), (int) y, (int) (z - 3), (float) 4, Explosion.Mode.NONE);
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.IRON_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.REDSTONE_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.COMMAND_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.GOLD_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.TOTEM_OF_UNDYING));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.LAPIS_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Scp330BlueCandyItem.block));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DIAMOND_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.BEACON));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.EMERALD_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.ENDER_EYE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.NETHERITE_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.DRAGON_EGG));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SMOOTH_QUARTZ.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CHISELED_QUARTZ_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.QUARTZ_PILLAR.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.QUARTZ_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.QUARTZ_BRICKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.QUARTZ_BRICKS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SMOOTH_QUARTZ_STAIRS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SMOOTH_QUARTZ_SLAB.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SLIME_BLOCK.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (world instanceof ServerWorld) {
						Entity entityToSpawn = new SlimeEntity(EntityType.SLIME, (World) world);
						entityToSpawn.setLocationAndAngles((x + 4), y, (z - 3), (float) 0, (float) 0);
						entityToSpawn.setRenderYawOffset((float) 0);
						entityToSpawn.setRotationYawHead((float) 0);
						entityToSpawn.setMotion(0, 0, 0);
						if (entityToSpawn instanceof MobEntity)
							((MobEntity) entityToSpawn).onInitialSpawn((ServerWorld) world,
									world.getDifficultyForLocation(entityToSpawn.getPosition()), SpawnReason.MOB_SUMMONED, (ILivingEntityData) null,
									(CompoundNBT) null);
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ANCIENT_DEBRIS.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.OAK_LOG.asItem() || (new Object() {
					public ItemStack entityToItem(Entity _ent) {
						if (_ent instanceof ItemEntity) {
							return ((ItemEntity) _ent).getItem();
						}
						return ItemStack.EMPTY;
					}
				}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
						new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)),
						null).stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ACACIA_LOG.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_OAK_LOG.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_SPRUCE_LOG.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_BIRCH_LOG.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_JUNGLE_LOG.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_ACACIA_LOG.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_DARK_OAK_LOG.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_WARPED_STEM.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_CRIMSON_STEM.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.OAK_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.SPRUCE_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.BIRCH_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.JUNGLE_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.ACACIA_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.DARK_OAK_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.WARPED_HYPHAE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_NYLIUM));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.CRIMSON_HYPHAE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_NYLIUM));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_OAK_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_SPRUCE_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_BIRCH_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_JUNGLE_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_ACACIA_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_DARK_OAK_WOOD.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3),
									new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_WARPED_HYPHAE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
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
		}.entityToItem(((Entity) world.getEntitiesWithinAABB(ItemEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)))).getItem() == Blocks.STRIPPED_CRIMSON_HYPHAE.asItem()) {
			if (world instanceof ServerWorld) {
				((World) world).getServer().getCommandManager()
						.handleCommand(
								new CommandSource(ICommandSource.DUMMY, new Vector3d((x - 4), y, (z - 3)), Vector2f.ZERO, (ServerWorld) world, 4, "",
										new StringTextComponent(""), ((World) world).getServer(), null).withFeedbackDisabled(),
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
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
		VeryFineItems2Procedure
				.executeProcedure(Stream
						.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x),
								new AbstractMap.SimpleEntry<>("y", y), new AbstractMap.SimpleEntry<>("z", z))
						.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
	}
}
