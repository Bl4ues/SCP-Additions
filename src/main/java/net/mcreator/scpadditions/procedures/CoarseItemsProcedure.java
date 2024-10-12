package net.mcreator.scpadditions.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Comparator;

public class CoarseItemsProcedure {
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
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.PIECES_OF_PAPER.get()));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.LEVEL_2_KEYCARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.PIECES_OF_PAPER.get()));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.LEVEL_3_KEYCARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.PIECES_OF_PAPER.get()));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.LEVEL_4_KEYCARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.PIECES_OF_PAPER.get()));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.LEVEL_5_KEYCARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.PIECES_OF_PAPER.get()));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.LEVEL_6_KEYCARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.PIECES_OF_PAPER.get()));
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
				for (int index0 = 0; index0 < 4; index0++) {
					if (world instanceof ServerLevel _level) {
						ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.COIN.get()));
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
		}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) instanceof ItemEntity _itemEnt ? _itemEnt.getItem() : ItemStack.EMPTY).getItem() == ScpAdditionsModItems.PLAYING_CARD.get()) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((x - 4), y, (z - 3)), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"/kill @e[distance=..3]");
			ScpAdditionsMod.queueServerWork(160, () -> {
				if (world instanceof ServerLevel _level) {
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(ScpAdditionsModItems.PIECES_OF_PAPER.get()));
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
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Items.IRON_NUGGET));
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
					ItemEntity entityToSpawn = new ItemEntity(_level, (x + 4), y, (z - 3), new ItemStack(Blocks.COBBLESTONE));
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
