package net.mcreator.scpadditions.procedures;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.Entity;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.block.Blocks;

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

import java.util.function.Function;
import java.util.Map;
import java.util.Comparator;

public class OneToOneItemsProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure OneToOneItems!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency x for procedure OneToOneItems!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency y for procedure OneToOneItems!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency z for procedure OneToOneItems!");
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
					if (world instanceof World && !world.isRemote()) {
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(PlayingCardItem.block));
						entityToSpawn.setPickupDelay((int) 10);
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
					if (world instanceof World && !world.isRemote()) {
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(PlayingCardItem.block));
						entityToSpawn.setPickupDelay((int) 10);
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
					if (world instanceof World && !world.isRemote()) {
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(PlayingCardItem.block));
						entityToSpawn.setPickupDelay((int) 10);
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
					if (world instanceof World && !world.isRemote()) {
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(PlayingCardItem.block));
						entityToSpawn.setPickupDelay((int) 10);
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
					if (world instanceof World && !world.isRemote()) {
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(PlayingCardItem.block));
						entityToSpawn.setPickupDelay((int) 10);
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
					if (Math.random() < 0.5) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(PlayingCardItem.block));
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level1KeycardItem.block));
						entityToSpawn.setPickupDelay((int) 10);
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level1KeycardItem.block));
						entityToSpawn.setPickupDelay((int) 10);
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
					if (world instanceof World && !world.isRemote()) {
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Items.PAPER));
						entityToSpawn.setPickupDelay((int) 10);
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
						ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Level1KeycardItem.block));
						entityToSpawn.setPickupDelay((int) 10);
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
					if (Math.random() < 0.3) {
						if (world instanceof World && !world.isRemote()) {
							ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.GRANITE));
							entityToSpawn.setPickupDelay((int) 10);
							entityToSpawn.setNoDespawn();
							world.addEntity(entityToSpawn);
						}
					} else {
						if (Math.random() < 0.3) {
							if (world instanceof World && !world.isRemote()) {
								ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.DIORITE));
								entityToSpawn.setPickupDelay((int) 10);
								entityToSpawn.setNoDespawn();
								world.addEntity(entityToSpawn);
							}
						} else {
							if (Math.random() < 0.3) {
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.ANDESITE));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
							} else {
								if (world instanceof World && !world.isRemote()) {
									ItemEntity entityToSpawn = new ItemEntity((World) world, (x + 4), y, (z - 3), new ItemStack(Blocks.BLACKSTONE));
									entityToSpawn.setPickupDelay((int) 10);
									entityToSpawn.setNoDespawn();
									world.addEntity(entityToSpawn);
								}
							}
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
					MinecraftForge.EVENT_BUS.unregister(this);
				}
			}.start(world, (int) 160);
		}
	}
}
