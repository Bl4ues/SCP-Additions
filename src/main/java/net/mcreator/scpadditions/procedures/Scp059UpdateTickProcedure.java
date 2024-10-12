package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;
import java.util.List;
import java.util.Comparator;

public class Scp059UpdateTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(20 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if (!(ScpAdditionsModItems.HAZMAT_SUIT_BOOTS.get() == (entityiterator instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.FEET) : ItemStack.EMPTY).getItem()
						&& ScpAdditionsModItems.HAZMAT_SUIT_LEGGINGS.get() == (entityiterator instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.LEGS) : ItemStack.EMPTY).getItem()
						&& ScpAdditionsModItems.HAZMAT_SUIT_CHESTPLATE.get() == (entityiterator instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.CHEST) : ItemStack.EMPTY).getItem()
						&& ScpAdditionsModItems.HAZMAT_SUIT_HELMET.get() == (entityiterator instanceof LivingEntity _entGetArmor ? _entGetArmor.getItemBySlot(EquipmentSlot.HEAD) : ItemStack.EMPTY).getItem()
						|| (entityiterator.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ScpAdditionsModVariables.PlayerVariables())).scp059infected0
						|| (entityiterator.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ScpAdditionsModVariables.PlayerVariables())).scp059infected1)) {
					if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(ScpAdditionsModMobEffects.DELTA_RADIATION.get(), 1200, 1, false, false));
					if (entityiterator instanceof ServerPlayer _player) {
						Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("scp_additions:scp_059_adv"));
						AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
						if (!_ap.isDone()) {
							for (String criteria : _ap.getRemainingCriteria())
								_player.getAdvancements().award(_adv, criteria);
						}
					}
				}
			}
		}
		ScpAdditionsMod.queueServerWork(18000, () -> {
			ScpAdditionsModVariables.MapVariables.get(world).RandomX = x + Mth.nextDouble(RandomSource.create(), -10, 10);
			ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			ScpAdditionsModVariables.MapVariables.get(world).RandomY = y + Mth.nextDouble(RandomSource.create(), -10, 10);
			ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			ScpAdditionsModVariables.MapVariables.get(world).RandomZ = z + Mth.nextDouble(RandomSource.create(), -10, 10);
			ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
			if (world.getBlockState(BlockPos.containing(ScpAdditionsModVariables.MapVariables.get(world).RandomX, ScpAdditionsModVariables.MapVariables.get(world).RandomY, ScpAdditionsModVariables.MapVariables.get(world).RandomZ)).canOcclude()) {
				if (world.getBlockState(BlockPos.containing(ScpAdditionsModVariables.MapVariables.get(world).RandomX, ScpAdditionsModVariables.MapVariables.get(world).RandomY, ScpAdditionsModVariables.MapVariables.get(world).RandomZ))
						.getDestroySpeed(world, BlockPos.containing(ScpAdditionsModVariables.MapVariables.get(world).RandomX, ScpAdditionsModVariables.MapVariables.get(world).RandomY, ScpAdditionsModVariables.MapVariables.get(world).RandomZ)) > 0
						&& world.getBlockState(BlockPos.containing(ScpAdditionsModVariables.MapVariables.get(world).RandomX, ScpAdditionsModVariables.MapVariables.get(world).RandomY, ScpAdditionsModVariables.MapVariables.get(world).RandomZ))
								.getDestroySpeed(world,
										BlockPos.containing(ScpAdditionsModVariables.MapVariables.get(world).RandomX, ScpAdditionsModVariables.MapVariables.get(world).RandomY, ScpAdditionsModVariables.MapVariables.get(world).RandomZ)) < 25) {
					{
						BlockPos _bp = BlockPos.containing(ScpAdditionsModVariables.MapVariables.get(world).RandomX, ScpAdditionsModVariables.MapVariables.get(world).RandomY, ScpAdditionsModVariables.MapVariables.get(world).RandomZ);
						BlockState _bs = ScpAdditionsModBlocks.SCP_059_1.get().defaultBlockState();
						BlockState _bso = world.getBlockState(_bp);
						for (Map.Entry<Property<?>, Comparable<?>> entry : _bso.getValues().entrySet()) {
							Property _property = _bs.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
							if (_property != null && _bs.getValue(_property) != null)
								try {
									_bs = _bs.setValue(_property, (Comparable) entry.getValue());
								} catch (Exception e) {
								}
						}
						world.setBlock(_bp, _bs, 3);
					}
				}
			}
		});
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(100 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if (!world.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(x, y, z), 30, 30, 30), e -> true).isEmpty()) {
					if (ScpAdditionsModItems.GEIGER_3.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
							|| ScpAdditionsModItems.GEIGER_3.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()) {
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:geiger3")),
										SoundSource.NEUTRAL, 1, 1);
							} else {
								_level.playLocalSound((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:geiger3")), SoundSource.NEUTRAL, 1, 1, false);
							}
						}
					}
					if (ScpAdditionsModItems.GEIGER_1.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
						if (entityiterator instanceof LivingEntity _entity) {
							ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_3.get());
							_setstack.setCount(1);
							_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
							if (_entity instanceof Player _player)
								_player.getInventory().setChanged();
						}
					}
					if (ScpAdditionsModItems.GEIGER_1.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()) {
						if (entityiterator instanceof LivingEntity _entity) {
							ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_3.get());
							_setstack.setCount(1);
							_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
							if (_entity instanceof Player _player)
								_player.getInventory().setChanged();
						}
					}
					if (ScpAdditionsModItems.GEIGER_2.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
						if (entityiterator instanceof LivingEntity _entity) {
							ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_3.get());
							_setstack.setCount(1);
							_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
							if (_entity instanceof Player _player)
								_player.getInventory().setChanged();
						}
					}
					if (ScpAdditionsModItems.GEIGER_2.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()) {
						if (entityiterator instanceof LivingEntity _entity) {
							ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_3.get());
							_setstack.setCount(1);
							_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
							if (_entity instanceof Player _player)
								_player.getInventory().setChanged();
						}
					}
				} else {
					if (!world.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(x, y, z), 60, 60, 60), e -> true).isEmpty()) {
						if (ScpAdditionsModItems.GEIGER_2.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
								|| ScpAdditionsModItems.GEIGER_2.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()) {
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:geiger2")),
											SoundSource.NEUTRAL, 1, 1);
								} else {
									_level.playLocalSound((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:geiger2")), SoundSource.NEUTRAL, 1, 1,
											false);
								}
							}
						}
						if (ScpAdditionsModItems.GEIGER_1.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
							if (entityiterator instanceof LivingEntity _entity) {
								ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_2.get());
								_setstack.setCount(1);
								_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
								if (_entity instanceof Player _player)
									_player.getInventory().setChanged();
							}
						}
						if (ScpAdditionsModItems.GEIGER_1.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()) {
							if (entityiterator instanceof LivingEntity _entity) {
								ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_2.get());
								_setstack.setCount(1);
								_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
								if (_entity instanceof Player _player)
									_player.getInventory().setChanged();
							}
						}
						if (ScpAdditionsModItems.GEIGER_3.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
							if (entityiterator instanceof LivingEntity _entity) {
								ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_2.get());
								_setstack.setCount(1);
								_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
								if (_entity instanceof Player _player)
									_player.getInventory().setChanged();
							}
						}
						if (ScpAdditionsModItems.GEIGER_3.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()) {
							if (entityiterator instanceof LivingEntity _entity) {
								ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_2.get());
								_setstack.setCount(1);
								_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
								if (_entity instanceof Player _player)
									_player.getInventory().setChanged();
							}
						}
					} else {
						if (!world.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(x, y, z), 100, 100, 100), e -> true).isEmpty()) {
							if (ScpAdditionsModItems.GEIGER_1.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()
									|| ScpAdditionsModItems.GEIGER_1.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()) {
								if (world instanceof Level _level) {
									if (!_level.isClientSide()) {
										_level.playSound(null, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:geiger1")),
												SoundSource.NEUTRAL, 1, 1);
									} else {
										_level.playLocalSound((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:geiger1")), SoundSource.NEUTRAL, 1, 1,
												false);
									}
								}
							}
							if (ScpAdditionsModItems.GEIGER_2.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
								if (entityiterator instanceof LivingEntity _entity) {
									ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_1.get());
									_setstack.setCount(1);
									_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
									if (_entity instanceof Player _player)
										_player.getInventory().setChanged();
								}
							}
							if (ScpAdditionsModItems.GEIGER_2.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()) {
								if (entityiterator instanceof LivingEntity _entity) {
									ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_1.get());
									_setstack.setCount(1);
									_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
									if (_entity instanceof Player _player)
										_player.getInventory().setChanged();
								}
							}
							if (ScpAdditionsModItems.GEIGER_3.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem()) {
								if (entityiterator instanceof LivingEntity _entity) {
									ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_1.get());
									_setstack.setCount(1);
									_entity.setItemInHand(InteractionHand.MAIN_HAND, _setstack);
									if (_entity instanceof Player _player)
										_player.getInventory().setChanged();
								}
							}
							if (ScpAdditionsModItems.GEIGER_3.get() == (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem()) {
								if (entityiterator instanceof LivingEntity _entity) {
									ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GEIGER_1.get());
									_setstack.setCount(1);
									_entity.setItemInHand(InteractionHand.OFF_HAND, _setstack);
									if (_entity instanceof Player _player)
										_player.getInventory().setChanged();
								}
							}
						}
					}
				}
			}
		}
	}
}
