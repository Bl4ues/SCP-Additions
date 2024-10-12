package net.mcreator.scpadditions.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
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
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Comparator;

public class VeryFineItems2Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if ((((Entity) world.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(new Vec3((x - 4), y, (z - 3)), 3, 3, 3), e -> true).stream().sorted(new Object() {
			Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
				return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
			}
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.OAK_LEAVES.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES_PLANT));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SPRUCE_LEAVES.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES_PLANT));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.BIRCH_LEAVES.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES_PLANT));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.JUNGLE_LEAVES.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES_PLANT));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ACACIA_LEAVES.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES_PLANT));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ACACIA_LEAVES.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES_PLANT));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DARK_OAK_LEAVES.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.TWISTING_VINES_PLANT));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SPONGE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.HORN_CORAL_BLOCK));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.WET_SPONGE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.HORN_CORAL_BLOCK));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SANDSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CHISELED_SANDSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CUT_SANDSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SANDSTONE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SMOOTH_SANDSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SMOOTH_SANDSTONE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SMOOTH_SANDSTONE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CUT_SANDSTONE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SANDSTONE_WALL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.RED_SANDSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CHISELED_RED_SANDSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CUT_RED_SANDSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.RED_SANDSTONE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SMOOTH_RED_SANDSTONE.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SMOOTH_RED_SANDSTONE_STAIRS.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.SMOOTH_RED_SANDSTONE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CUT_RED_SANDSTONE_SLAB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.RED_SANDSTONE_WALL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.SOUL_SOIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.NOTE_BLOCK.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.JUKEBOX));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.RAIL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.POWERED_RAIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.POWERED_RAIL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.DETECTOR_RAIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DETECTOR_RAIL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.ACTIVATOR_RAIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ACTIVATOR_RAIL.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.RAIL));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.WHITE_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.WHITE_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.ORANGE_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.ORANGE_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.MAGENTA_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.MAGENTA_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.LIGHT_BLUE_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.LIGHT_BLUE_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.YELLOW_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.YELLOW_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.LIME_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.LIME_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.PINK_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.PINK_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.GRAY_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.GRAY_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.LIGHT_GRAY_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.LIGHT_GRAY_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.CYAN_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.CYAN_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.PURPLE_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.PURPLE_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.BLUE_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BLUE_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.BROWN_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BROWN_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.GREEN_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.GREEN_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.RED_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.RED_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.BLACK_BED.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.BLACK_BED));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.COBWEB.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					Entity entityToSpawn = EntityType.SPIDER.spawn(_level, BlockPos.containing(x + 4, y, z - 3), MobSpawnType.MOB_SUMMONED);
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DEAD_BUSH.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.POTTED_DEAD_BUSH));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == Blocks.DEAD_BUSH.asItem()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.POTTED_DEAD_BUSH));
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
