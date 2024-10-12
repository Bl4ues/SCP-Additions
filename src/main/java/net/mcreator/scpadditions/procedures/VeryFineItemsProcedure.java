package net.mcreator.scpadditions.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Comparator;

public class VeryFineItemsProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.LEVEL_1_KEYCARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.05) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.LEVEL_6_KEYCARD.get()));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.CREDIT_CARD.get()));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.LEVEL_2_KEYCARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.1) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.LEVEL_6_KEYCARD.get()));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.CREDIT_CARD.get()));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.LEVEL_3_KEYCARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.2) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.LEVEL_6_KEYCARD.get()));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.CREDIT_CARD.get()));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.LEVEL_4_KEYCARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.3) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.LEVEL_6_KEYCARD.get()));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.CREDIT_CARD.get()));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.LEVEL_5_KEYCARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.4) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.LEVEL_6_KEYCARD.get()));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.CREDIT_CARD.get()));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.LEVEL_6_KEYCARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.CREDIT_CARD.get()));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.CREDIT_CARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.LEVEL_2_KEYCARD.get()));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.PLAYING_CARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.LEVEL_2_KEYCARD.get()));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.PIECES_OF_PAPER.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.4) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.LEVEL_2_KEYCARD.get()));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.PAPER));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.COIN.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.LEVEL_2_KEYCARD.get()));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STONE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STONE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.GRANITE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_GRANITE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_GRANITE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.GRANITE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.GRANITE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_GRANITE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.GRANITE_WALL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DIORITE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DIORITE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DIORITE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DIORITE_WALL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_DIORITE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_DIORITE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_DIORITE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ANDESITE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ANDESITE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ANDESITE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ANDESITE_WALL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_ANDESITE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_ANDESITE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_ANDESITE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SMOOTH_STONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_STONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.GRASS_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_NYLIUM));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DIRT_PATH.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_NYLIUM));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.MYCELIUM.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_NYLIUM));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DIRT.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERRACK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.COARSE_DIRT.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERRACK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.PODZOL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_NYLIUM));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.COBBLESTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_COBBLESTONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.MOSSY_COBBLESTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_COBBLESTONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.MOSSY_COBBLESTONE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_COBBLESTONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.MOSSY_COBBLESTONE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.INFESTED_COBBLESTONE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.OAK_PLANKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SPRUCE_PLANKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.BIRCH_PLANKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.JUNGLE_PLANKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ACACIA_PLANKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DARK_OAK_PLANKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_PLANKS));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CRIMSON_PLANKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.WARPED_PLANKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.OAK_SAPLING.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SPRUCE_SAPLING.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.BIRCH_SAPLING.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.JUNGLE_SAPLING.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ACACIA_SAPLING.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DARK_OAK_SAPLING.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SUGAR_CANE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BOOKSHELF));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SAND.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SAND));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.RED_SAND.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SAND));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.GRAVEL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SAND));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.MAGMA_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.OBSIDIAN.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRYING_OBSIDIAN));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CRYING_OBSIDIAN.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.BLACKSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.BLACKSTONE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.BLACKSTONE_WALL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.BLACKSTONE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_BLACKSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_BLACKSTONE_BRICKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CHISELED_POLISHED_BLACKSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_BLACKSTONE_BRICK_WALL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_BLACKSTONE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_BLACKSTONE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POLISHED_BLACKSTONE_WALL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.COAL_ORE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.COAL_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.IRON_ORE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.IRON_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.REDSTONE_ORE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.REDSTONE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.GOLD_ORE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.GOLD_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.LAPIS_ORE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.LAPIS_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DIAMOND_ORE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.DIAMOND_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.EMERALD_ORE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.EMERALD_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.NETHER_QUARTZ_ORE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.QUARTZ_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.NETHER_GOLD_ORE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.GOLD_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.GILDED_BLACKSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.GOLD_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.COAL_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof Level _level && !_level.isClientSide())
					_level.explode(null, (x + 4), y, (z - 3), 4, Level.ExplosionInteraction.NONE);
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.IRON_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.REDSTONE_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.COMMAND_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.GOLD_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.TOTEM_OF_UNDYING));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.LAPIS_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.SCP_330_BLUE_CANDY.get()));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DIAMOND_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BEACON));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.EMERALD_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.ENDER_EYE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.NETHERITE_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.DRAGON_EGG));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SMOOTH_QUARTZ.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CHISELED_QUARTZ_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.QUARTZ_PILLAR.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.QUARTZ_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.QUARTZ_BRICKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.QUARTZ_BRICKS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SMOOTH_QUARTZ_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SMOOTH_QUARTZ_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BONE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SLIME_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					Entity entityToSpawn = EntityType.SLIME.spawn(_level, BlockPos.containing(x + 4, y, z - 3), MobSpawnType.MOB_SUMMONED);
					if (entityToSpawn != null) {
						entityToSpawn.setDeltaMovement(0, 0, 0);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ANCIENT_DEBRIS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.NETHERITE_BLOCK));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.OAK_LOG.asItem()
				|| (((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ACACIA_LOG.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_OAK_LOG.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_SPRUCE_LOG.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_BIRCH_LOG.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_JUNGLE_LOG.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_ACACIA_LOG.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_DARK_OAK_LOG.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_STEM));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_WARPED_STEM.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_CRIMSON_STEM.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.OAK_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SPRUCE_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.BIRCH_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.JUNGLE_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ACACIA_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DARK_OAK_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.WARPED_HYPHAE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_NYLIUM));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CRIMSON_HYPHAE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_NYLIUM));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_OAK_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_SPRUCE_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_BIRCH_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_JUNGLE_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_ACACIA_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_DARK_OAK_WOOD.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (Math.random() < 0.5) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_WARPED_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				} else {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.STRIPPED_CRIMSON_HYPHAE));
						entityToSpawn.setPickUpDelay(10);
						entityToSpawn.setUnlimitedLifetime();
						_level.addFreshEntity(entityToSpawn);
					}
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_WARPED_HYPHAE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WARPED_HYPHAE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.STRIPPED_CRIMSON_HYPHAE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CRIMSON_HYPHAE));
					entityToSpawn.setPickUpDelay(10);
					entityToSpawn.setUnlimitedLifetime();
					_level.addFreshEntity(entityToSpawn);
				}
				ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = false;
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			});
		}
		VeryFineItems2Procedure.execute(world, x, y, z);
	}
}
