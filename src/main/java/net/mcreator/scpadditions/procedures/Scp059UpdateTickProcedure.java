package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Hand;
import net.minecraft.state.Property;
import net.minecraft.server.MinecraftServer;
import net.minecraft.potion.EffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.block.BlockState;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;

import net.mcreator.scpadditions.potion.DeltaRadiationPotionEffect;
import net.mcreator.scpadditions.item.HazmatSuitItem;
import net.mcreator.scpadditions.item.Geiger3Item;
import net.mcreator.scpadditions.item.Geiger2Item;
import net.mcreator.scpadditions.item.Geiger1Item;
import net.mcreator.scpadditions.block.Scp0591Block;
import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Random;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.Comparator;

public class Scp059UpdateTickProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure Scp059UpdateTick!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency x for procedure Scp059UpdateTick!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency y for procedure Scp059UpdateTick!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency z for procedure Scp059UpdateTick!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		double x = dependencies.get("x") instanceof Integer ? (int) dependencies.get("x") : (double) dependencies.get("x");
		double y = dependencies.get("y") instanceof Integer ? (int) dependencies.get("y") : (double) dependencies.get("y");
		double z = dependencies.get("z") instanceof Integer ? (int) dependencies.get("z") : (double) dependencies.get("z");
		{
			List<Entity> _entfound = world
					.getEntitiesWithinAABB(Entity.class,
							new AxisAlignedBB(x - (20 / 2d), y - (20 / 2d), z - (20 / 2d), x + (20 / 2d), y + (20 / 2d), z + (20 / 2d)), null)
					.stream().sorted(new Object() {
						Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
							return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
						}
					}.compareDistOf(x, y, z)).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(HazmatSuitItem.boots == ((entityiterator instanceof LivingEntity)
						? ((LivingEntity) entityiterator).getItemStackFromSlot(EquipmentSlotType.FEET)
						: ItemStack.EMPTY).getItem()
						&& HazmatSuitItem.legs == ((entityiterator instanceof LivingEntity)
								? ((LivingEntity) entityiterator).getItemStackFromSlot(EquipmentSlotType.LEGS)
								: ItemStack.EMPTY).getItem()
						&& HazmatSuitItem.body == ((entityiterator instanceof LivingEntity)
								? ((LivingEntity) entityiterator).getItemStackFromSlot(EquipmentSlotType.CHEST)
								: ItemStack.EMPTY).getItem()
						&& HazmatSuitItem.helmet == ((entityiterator instanceof LivingEntity)
								? ((LivingEntity) entityiterator).getItemStackFromSlot(EquipmentSlotType.HEAD)
								: ItemStack.EMPTY).getItem())) {
					if (entityiterator instanceof LivingEntity)
						((LivingEntity) entityiterator)
								.addPotionEffect(new EffectInstance(DeltaRadiationPotionEffect.potion, (int) 1200, (int) 1, (false), (false)));
					if (entityiterator instanceof ServerPlayerEntity) {
						Advancement _adv = ((MinecraftServer) ((ServerPlayerEntity) entityiterator).server).getAdvancementManager()
								.getAdvancement(new ResourceLocation("scp_additions:scp_059_adv"));
						AdvancementProgress _ap = ((ServerPlayerEntity) entityiterator).getAdvancements().getProgress(_adv);
						if (!_ap.isDone()) {
							Iterator _iterator = _ap.getRemaningCriteria().iterator();
							while (_iterator.hasNext()) {
								String _criterion = (String) _iterator.next();
								((ServerPlayerEntity) entityiterator).getAdvancements().grantCriterion(_adv, _criterion);
							}
						}
					}
				}
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
				ScpAdditionsModVariables.MapVariables.get(world).RandomX = (x + MathHelper.nextDouble(new Random(), -10, 10));
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
				ScpAdditionsModVariables.MapVariables.get(world).RandomY = (y + MathHelper.nextDouble(new Random(), -10, 10));
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
				ScpAdditionsModVariables.MapVariables.get(world).RandomZ = (z + MathHelper.nextDouble(new Random(), -10, 10));
				ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
				if (world
						.getBlockState(new BlockPos(ScpAdditionsModVariables.MapVariables.get(world).RandomX,
								ScpAdditionsModVariables.MapVariables.get(world).RandomY, ScpAdditionsModVariables.MapVariables.get(world).RandomZ))
						.isSolid()) {
					if (world.getBlockState(new BlockPos(ScpAdditionsModVariables.MapVariables.get(world).RandomX,
							ScpAdditionsModVariables.MapVariables.get(world).RandomY, ScpAdditionsModVariables.MapVariables.get(world).RandomZ))
							.getBlockHardness(world,
									new BlockPos(ScpAdditionsModVariables.MapVariables.get(world).RandomX,
											ScpAdditionsModVariables.MapVariables.get(world).RandomY,
											ScpAdditionsModVariables.MapVariables.get(world).RandomZ)) > 0
							&& world.getBlockState(new BlockPos(ScpAdditionsModVariables.MapVariables.get(world).RandomX,
									ScpAdditionsModVariables.MapVariables.get(world).RandomY,
									ScpAdditionsModVariables.MapVariables.get(world).RandomZ))
									.getBlockHardness(world,
											new BlockPos(ScpAdditionsModVariables.MapVariables.get(world).RandomX,
													ScpAdditionsModVariables.MapVariables.get(world).RandomY,
													ScpAdditionsModVariables.MapVariables.get(world).RandomZ)) < 25) {
						{
							BlockPos _bp = new BlockPos(ScpAdditionsModVariables.MapVariables.get(world).RandomX,
									ScpAdditionsModVariables.MapVariables.get(world).RandomY,
									ScpAdditionsModVariables.MapVariables.get(world).RandomZ);
							BlockState _bs = Scp0591Block.block.getDefaultState();
							BlockState _bso = world.getBlockState(_bp);
							for (Map.Entry<Property<?>, Comparable<?>> entry : _bso.getValues().entrySet()) {
								Property _property = _bs.getBlock().getStateContainer().getProperty(entry.getKey().getName());
								if (_property != null && _bs.get(_property) != null)
									try {
										_bs = _bs.with(_property, (Comparable) entry.getValue());
									} catch (Exception e) {
									}
							}
							world.setBlockState(_bp, _bs, 3);
						}
					}
				}
				MinecraftForge.EVENT_BUS.unregister(this);
			}
		}.start(world, (int) 18000);
		{
			List<Entity> _entfound = world
					.getEntitiesWithinAABB(Entity.class,
							new AxisAlignedBB(x - (100 / 2d), y - (100 / 2d), z - (100 / 2d), x + (100 / 2d), y + (100 / 2d), z + (100 / 2d)), null)
					.stream().sorted(new Object() {
						Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
							return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
						}
					}.compareDistOf(x, y, z)).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (((Entity) world
						.getEntitiesWithinAABB(LivingEntity.class,
								new AxisAlignedBB(x - (30 / 2d), y - (30 / 2d), z - (30 / 2d), x + (30 / 2d), y + (30 / 2d), z + (30 / 2d)), null)
						.stream().sorted(new Object() {
							Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
								return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
							}
						}.compareDistOf(x, y, z)).findFirst().orElse(null)) != null) {
					if (Geiger3Item.block == ((entityiterator instanceof LivingEntity)
							? ((LivingEntity) entityiterator).getHeldItemMainhand()
							: ItemStack.EMPTY).getItem()
							|| Geiger3Item.block == ((entityiterator instanceof LivingEntity)
									? ((LivingEntity) entityiterator).getHeldItemOffhand()
									: ItemStack.EMPTY).getItem()) {
						if (world instanceof World && !world.isRemote()) {
							((World) world)
									.playSound(null, new BlockPos(entityiterator.getPosX(), entityiterator.getPosY(), entityiterator.getPosZ()),
											(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
													.getValue(new ResourceLocation("scp_additions:geiger3")),
											SoundCategory.NEUTRAL, (float) 1, (float) 1);
						} else {
							((World) world).playSound((entityiterator.getPosX()), (entityiterator.getPosY()), (entityiterator.getPosZ()),
									(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
											.getValue(new ResourceLocation("scp_additions:geiger3")),
									SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
						}
					}
					if (Geiger1Item.block == ((entityiterator instanceof LivingEntity)
							? ((LivingEntity) entityiterator).getHeldItemMainhand()
							: ItemStack.EMPTY).getItem()) {
						if (entityiterator instanceof LivingEntity) {
							ItemStack _setstack = new ItemStack(Geiger3Item.block);
							_setstack.setCount((int) 1);
							((LivingEntity) entityiterator).setHeldItem(Hand.MAIN_HAND, _setstack);
							if (entityiterator instanceof ServerPlayerEntity)
								((ServerPlayerEntity) entityiterator).inventory.markDirty();
						}
					}
					if (Geiger1Item.block == ((entityiterator instanceof LivingEntity)
							? ((LivingEntity) entityiterator).getHeldItemOffhand()
							: ItemStack.EMPTY).getItem()) {
						if (entityiterator instanceof LivingEntity) {
							ItemStack _setstack = new ItemStack(Geiger3Item.block);
							_setstack.setCount((int) 1);
							((LivingEntity) entityiterator).setHeldItem(Hand.OFF_HAND, _setstack);
							if (entityiterator instanceof ServerPlayerEntity)
								((ServerPlayerEntity) entityiterator).inventory.markDirty();
						}
					}
					if (Geiger2Item.block == ((entityiterator instanceof LivingEntity)
							? ((LivingEntity) entityiterator).getHeldItemMainhand()
							: ItemStack.EMPTY).getItem()) {
						if (entityiterator instanceof LivingEntity) {
							ItemStack _setstack = new ItemStack(Geiger3Item.block);
							_setstack.setCount((int) 1);
							((LivingEntity) entityiterator).setHeldItem(Hand.MAIN_HAND, _setstack);
							if (entityiterator instanceof ServerPlayerEntity)
								((ServerPlayerEntity) entityiterator).inventory.markDirty();
						}
					}
					if (Geiger2Item.block == ((entityiterator instanceof LivingEntity)
							? ((LivingEntity) entityiterator).getHeldItemOffhand()
							: ItemStack.EMPTY).getItem()) {
						if (entityiterator instanceof LivingEntity) {
							ItemStack _setstack = new ItemStack(Geiger3Item.block);
							_setstack.setCount((int) 1);
							((LivingEntity) entityiterator).setHeldItem(Hand.OFF_HAND, _setstack);
							if (entityiterator instanceof ServerPlayerEntity)
								((ServerPlayerEntity) entityiterator).inventory.markDirty();
						}
					}
				} else {
					if (((Entity) world
							.getEntitiesWithinAABB(LivingEntity.class,
									new AxisAlignedBB(x - (60 / 2d), y - (60 / 2d), z - (60 / 2d), x + (60 / 2d), y + (60 / 2d), z + (60 / 2d)), null)
							.stream().sorted(new Object() {
								Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
									return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
								}
							}.compareDistOf(x, y, z)).findFirst().orElse(null)) != null) {
						if (Geiger2Item.block == ((entityiterator instanceof LivingEntity)
								? ((LivingEntity) entityiterator).getHeldItemMainhand()
								: ItemStack.EMPTY).getItem()
								|| Geiger2Item.block == ((entityiterator instanceof LivingEntity)
										? ((LivingEntity) entityiterator).getHeldItemOffhand()
										: ItemStack.EMPTY).getItem()) {
							if (world instanceof World && !world.isRemote()) {
								((World) world).playSound(null,
										new BlockPos(entityiterator.getPosX(), entityiterator.getPosY(), entityiterator.getPosZ()),
										(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
												.getValue(new ResourceLocation("scp_additions:geiger2")),
										SoundCategory.NEUTRAL, (float) 1, (float) 1);
							} else {
								((World) world).playSound((entityiterator.getPosX()), (entityiterator.getPosY()), (entityiterator.getPosZ()),
										(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
												.getValue(new ResourceLocation("scp_additions:geiger2")),
										SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
							}
						}
						if (Geiger1Item.block == ((entityiterator instanceof LivingEntity)
								? ((LivingEntity) entityiterator).getHeldItemMainhand()
								: ItemStack.EMPTY).getItem()) {
							if (entityiterator instanceof LivingEntity) {
								ItemStack _setstack = new ItemStack(Geiger2Item.block);
								_setstack.setCount((int) 1);
								((LivingEntity) entityiterator).setHeldItem(Hand.MAIN_HAND, _setstack);
								if (entityiterator instanceof ServerPlayerEntity)
									((ServerPlayerEntity) entityiterator).inventory.markDirty();
							}
						}
						if (Geiger1Item.block == ((entityiterator instanceof LivingEntity)
								? ((LivingEntity) entityiterator).getHeldItemOffhand()
								: ItemStack.EMPTY).getItem()) {
							if (entityiterator instanceof LivingEntity) {
								ItemStack _setstack = new ItemStack(Geiger2Item.block);
								_setstack.setCount((int) 1);
								((LivingEntity) entityiterator).setHeldItem(Hand.OFF_HAND, _setstack);
								if (entityiterator instanceof ServerPlayerEntity)
									((ServerPlayerEntity) entityiterator).inventory.markDirty();
							}
						}
						if (Geiger3Item.block == ((entityiterator instanceof LivingEntity)
								? ((LivingEntity) entityiterator).getHeldItemMainhand()
								: ItemStack.EMPTY).getItem()) {
							if (entityiterator instanceof LivingEntity) {
								ItemStack _setstack = new ItemStack(Geiger2Item.block);
								_setstack.setCount((int) 1);
								((LivingEntity) entityiterator).setHeldItem(Hand.MAIN_HAND, _setstack);
								if (entityiterator instanceof ServerPlayerEntity)
									((ServerPlayerEntity) entityiterator).inventory.markDirty();
							}
						}
						if (Geiger3Item.block == ((entityiterator instanceof LivingEntity)
								? ((LivingEntity) entityiterator).getHeldItemOffhand()
								: ItemStack.EMPTY).getItem()) {
							if (entityiterator instanceof LivingEntity) {
								ItemStack _setstack = new ItemStack(Geiger2Item.block);
								_setstack.setCount((int) 1);
								((LivingEntity) entityiterator).setHeldItem(Hand.OFF_HAND, _setstack);
								if (entityiterator instanceof ServerPlayerEntity)
									((ServerPlayerEntity) entityiterator).inventory.markDirty();
							}
						}
					} else {
						if (((Entity) world.getEntitiesWithinAABB(LivingEntity.class,
								new AxisAlignedBB(x - (100 / 2d), y - (100 / 2d), z - (100 / 2d), x + (100 / 2d), y + (100 / 2d), z + (100 / 2d)),
								null).stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
									}
								}.compareDistOf(x, y, z)).findFirst().orElse(null)) != null) {
							if (Geiger1Item.block == ((entityiterator instanceof LivingEntity)
									? ((LivingEntity) entityiterator).getHeldItemMainhand()
									: ItemStack.EMPTY).getItem()
									|| Geiger1Item.block == ((entityiterator instanceof LivingEntity)
											? ((LivingEntity) entityiterator).getHeldItemOffhand()
											: ItemStack.EMPTY).getItem()) {
								if (world instanceof World && !world.isRemote()) {
									((World) world).playSound(null,
											new BlockPos(entityiterator.getPosX(), entityiterator.getPosY(), entityiterator.getPosZ()),
											(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
													.getValue(new ResourceLocation("scp_additions:geiger1")),
											SoundCategory.NEUTRAL, (float) 1, (float) 1);
								} else {
									((World) world).playSound((entityiterator.getPosX()), (entityiterator.getPosY()), (entityiterator.getPosZ()),
											(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
													.getValue(new ResourceLocation("scp_additions:geiger1")),
											SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
								}
							}
							if (Geiger2Item.block == ((entityiterator instanceof LivingEntity)
									? ((LivingEntity) entityiterator).getHeldItemMainhand()
									: ItemStack.EMPTY).getItem()) {
								if (entityiterator instanceof LivingEntity) {
									ItemStack _setstack = new ItemStack(Geiger1Item.block);
									_setstack.setCount((int) 1);
									((LivingEntity) entityiterator).setHeldItem(Hand.MAIN_HAND, _setstack);
									if (entityiterator instanceof ServerPlayerEntity)
										((ServerPlayerEntity) entityiterator).inventory.markDirty();
								}
							}
							if (Geiger2Item.block == ((entityiterator instanceof LivingEntity)
									? ((LivingEntity) entityiterator).getHeldItemOffhand()
									: ItemStack.EMPTY).getItem()) {
								if (entityiterator instanceof LivingEntity) {
									ItemStack _setstack = new ItemStack(Geiger1Item.block);
									_setstack.setCount((int) 1);
									((LivingEntity) entityiterator).setHeldItem(Hand.OFF_HAND, _setstack);
									if (entityiterator instanceof ServerPlayerEntity)
										((ServerPlayerEntity) entityiterator).inventory.markDirty();
								}
							}
							if (Geiger3Item.block == ((entityiterator instanceof LivingEntity)
									? ((LivingEntity) entityiterator).getHeldItemMainhand()
									: ItemStack.EMPTY).getItem()) {
								if (entityiterator instanceof LivingEntity) {
									ItemStack _setstack = new ItemStack(Geiger1Item.block);
									_setstack.setCount((int) 1);
									((LivingEntity) entityiterator).setHeldItem(Hand.MAIN_HAND, _setstack);
									if (entityiterator instanceof ServerPlayerEntity)
										((ServerPlayerEntity) entityiterator).inventory.markDirty();
								}
							}
							if (Geiger3Item.block == ((entityiterator instanceof LivingEntity)
									? ((LivingEntity) entityiterator).getHeldItemOffhand()
									: ItemStack.EMPTY).getItem()) {
								if (entityiterator instanceof LivingEntity) {
									ItemStack _setstack = new ItemStack(Geiger1Item.block);
									_setstack.setCount((int) 1);
									((LivingEntity) entityiterator).setHeldItem(Hand.OFF_HAND, _setstack);
									if (entityiterator instanceof ServerPlayerEntity)
										((ServerPlayerEntity) entityiterator).inventory.markDirty();
								}
							}
						}
					}
				}
			}
		}
	}
}
