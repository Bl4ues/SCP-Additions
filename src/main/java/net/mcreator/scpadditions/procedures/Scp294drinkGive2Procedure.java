package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.EditBox;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.function.Supplier;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Comparator;

public class Scp294drinkGive2Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, HashMap guistate) {
		if (entity == null || guistate == null)
			return;
		if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("coconut")
				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Coconut")
				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("coconut milk")
				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Coconut milk")
				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("coconut water")
				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Coconut water")
				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("coconut juice")
				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Coconut juice")) {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
				} else {
					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
				}
			}
			if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
				((Slot) _slots.get(0)).remove(1);
				_player.containerMenu.broadcastChanges();
			}
			ScpAdditionsMod.queueServerWork(40, () -> {
				if (entity instanceof Player _player) {
					ItemStack _setstack = new ItemStack(ScpAdditionsModItems.COCONUT.get());
					_setstack.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
				}
			});
			ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
			ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
		} else {
			if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("cola")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Cola")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("coke")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Coke")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("coca-cola")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Coca-cola")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("coca cola")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Coca cola")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("pepsi")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Pepsi")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("soda")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Soda")) {
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
					}
				}
				if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
					((Slot) _slots.get(0)).remove(1);
					_player.containerMenu.broadcastChanges();
				}
				ScpAdditionsMod.queueServerWork(40, () -> {
					if (entity instanceof Player _player) {
						ItemStack _setstack = new ItemStack(ScpAdditionsModItems.COLA.get());
						_setstack.setCount(1);
						ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
					}
				});
				ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
				ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
			} else {
				if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("cold")
						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Cold")
						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("cool")
						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Cool")
						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("freezing")
						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Freezing")
						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("coldness")
						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Coldness")) {
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
						} else {
							_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
						}
					}
					if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
						((Slot) _slots.get(0)).remove(1);
						_player.containerMenu.broadcastChanges();
					}
					ScpAdditionsMod.queueServerWork(40, () -> {
						if (entity instanceof Player _player) {
							ItemStack _setstack = new ItemStack(ScpAdditionsModItems.COLD.get());
							_setstack.setCount(1);
							ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
						}
					});
					ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
					ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
				} else {
					if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("cosmopolitan")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Cosmopolitan")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("cocktail")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Cocktail")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("cosmopolitan cocktail")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Cosmopolitan Cocktail")) {
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
							} else {
								_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
							}
						}
						if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
							((Slot) _slots.get(0)).remove(1);
							_player.containerMenu.broadcastChanges();
						}
						ScpAdditionsMod.queueServerWork(40, () -> {
							if (entity instanceof Player _player) {
								ItemStack _setstack = new ItemStack(ScpAdditionsModItems.COSMOPOLITAN.get());
								_setstack.setCount(1);
								ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
							}
						});
						ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
						ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
					} else {
						if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("courage")
								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Courage")
								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("bravery")
								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Bravery")) {
							if (world instanceof Level _level) {
								if (!_level.isClientSide()) {
									_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
								} else {
									_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
								}
							}
							if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
								((Slot) _slots.get(0)).remove(1);
								_player.containerMenu.broadcastChanges();
							}
							ScpAdditionsMod.queueServerWork(40, () -> {
								if (entity instanceof Player _player) {
									ItemStack _setstack = new ItemStack(ScpAdditionsModItems.COURAGE.get());
									_setstack.setCount(1);
									ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
								}
							});
							ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
							ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
						} else {
							if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("curry")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Curry")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("masala")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Masala")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("curry sauce")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Curry sause")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("masala sause")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Masala sause")) {
								if (world instanceof Level _level) {
									if (!_level.isClientSide()) {
										_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
									} else {
										_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
									}
								}
								if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
									((Slot) _slots.get(0)).remove(1);
									_player.containerMenu.broadcastChanges();
								}
								ScpAdditionsMod.queueServerWork(40, () -> {
									if (entity instanceof Player _player) {
										ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CURRY.get());
										_setstack.setCount(1);
										ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
									}
								});
								ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
								ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
							} else {
								if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("death")
										|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Death")
										|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("game over")
										|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Game over")) {
									if (world instanceof Level _level) {
										if (!_level.isClientSide()) {
											_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
										} else {
											_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
										}
									}
									if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
										((Slot) _slots.get(0)).remove(1);
										_player.containerMenu.broadcastChanges();
									}
									ScpAdditionsMod.queueServerWork(40, () -> {
										if (entity instanceof Player _player) {
											ItemStack _setstack = new ItemStack(ScpAdditionsModItems.DEATH.get());
											_setstack.setCount(1);
											ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
										}
									});
									ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
									ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
								} else {
									if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("egg")
											|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Egg")
											|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("eggs")
											|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Eggs")) {
										if (world instanceof Level _level) {
											if (!_level.isClientSide()) {
												_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
											} else {
												_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
											}
										}
										if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
											((Slot) _slots.get(0)).remove(1);
											_player.containerMenu.broadcastChanges();
										}
										ScpAdditionsMod.queueServerWork(40, () -> {
											if (entity instanceof Player _player) {
												ItemStack _setstack = new ItemStack(ScpAdditionsModItems.EGGS.get());
												_setstack.setCount(1);
												ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
											}
										});
										ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
										ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
									} else {
										if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("element 0")
												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Element 0")
												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("element zero")
												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Element Zero")
												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("neutronium")
												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Neutronium")
												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("neutrium")
												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Neutrium")
												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("tetraneutron")
												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Tetraneutron")) {
											if (world instanceof Level _level) {
												if (!_level.isClientSide()) {
													_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
												} else {
													_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
												}
											}
											if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
												((Slot) _slots.get(0)).remove(1);
												_player.containerMenu.broadcastChanges();
											}
											ScpAdditionsMod.queueServerWork(40, () -> {
												if (entity instanceof Player _player) {
													ItemStack _setstack = new ItemStack(ScpAdditionsModItems.NEUTRONIUM.get());
													_setstack.setCount(1);
													ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
												}
											});
											ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
											ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
										} else {
											if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("es")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("ES")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("euroshopper")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Euroshopper")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("euroshopper energy drink")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Euroshopper Energy Drink")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("energy drink")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Energy Drink")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("red bull")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Red Bull")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("monster")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Monster")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("monster energy")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Monster Energy")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("monster energy drink")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Monster Energy Drink")) {
												if (world instanceof Level _level) {
													if (!_level.isClientSide()) {
														_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
													} else {
														_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
													}
												}
												if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
													((Slot) _slots.get(0)).remove(1);
													_player.containerMenu.broadcastChanges();
												}
												ScpAdditionsMod.queueServerWork(40, () -> {
													if (entity instanceof Player _player) {
														ItemStack _setstack = new ItemStack(ScpAdditionsModItems.NEUTRONIUM.get());
														_setstack.setCount(1);
														ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
													}
												});
												ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
												ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
											} else {
												if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("espresso")
														|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Espresso")) {
													if (world instanceof Level _level) {
														if (!_level.isClientSide()) {
															_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
														} else {
															_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
														}
													}
													if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
														((Slot) _slots.get(0)).remove(1);
														_player.containerMenu.broadcastChanges();
													}
													ScpAdditionsMod.queueServerWork(40, () -> {
														if (entity instanceof Player _player) {
															ItemStack _setstack = new ItemStack(ScpAdditionsModItems.ESPRESSO.get());
															_setstack.setCount(1);
															ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
														}
													});
													ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
													ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
												} else {
													if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("estus")
															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Estus")
															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("estus flesk")
															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Estus flesk")) {
														if (world instanceof Level _level) {
															if (!_level.isClientSide()) {
																_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
															} else {
																_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
															}
														}
														if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
															((Slot) _slots.get(0)).remove(1);
															_player.containerMenu.broadcastChanges();
														}
														ScpAdditionsMod.queueServerWork(40, () -> {
															if (entity instanceof Player _player) {
																ItemStack _setstack = new ItemStack(ScpAdditionsModItems.ESTUS.get());
																_setstack.setCount(1);
																ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
															}
														});
														ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
														ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
													} else {
														if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Exotic Matter")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("exotic matter")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Zero Point Energy")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("zero point energy")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Negative Matter")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("negative matter")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Gravitons")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("gravitons")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Higgs Boson")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("higgs boson")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("God Particles")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("god particles")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Black Hole")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("black hole")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Black Holes")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("black holes")) {
															if (world instanceof Level _level) {
																if (!_level.isClientSide()) {
																	_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294emptycup")), SoundSource.NEUTRAL, 1, 1);
																} else {
																	_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294emptycup")), SoundSource.NEUTRAL, 1, 1, false);
																}
															}
															if (world instanceof Level _level) {
																if (!_level.isClientSide()) {
																	_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:gravitons")), SoundSource.NEUTRAL, 1, 1);
																} else {
																	_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:gravitons")), SoundSource.NEUTRAL, 1, 1, false);
																}
															}
															if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																((Slot) _slots.get(0)).remove(1);
																_player.containerMenu.broadcastChanges();
															}
															{
																final Vec3 _center = new Vec3(x, y, z);
																List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(20 / 2d), e -> true).stream()
																		.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
																for (Entity entityiterator : _entfound) {
																	{
																		boolean _setval = true;
																		entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
																			capability.blackh = _setval;
																			capability.syncPlayerVariables(entity);
																		});
																	}
																	ScpAdditionsMod.queueServerWork(30, () -> {
																		if (entity instanceof LivingEntity _entity)
																			_entity.hurt(new DamageSource(_entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)) {
																				@Override
																				public Component getLocalizedDeathMessage(LivingEntity _msgEntity) {
																					String _translatekey = "death.attack." + "gravitons";
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
																			}, 100);
																	});
																}
															}
															ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
															ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
														} else {
															if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("eternal champion")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Eternal Champion")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("blood of eternal champion")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Blood of Eternal Champion")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("talin warhaft")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Talin Warhaft")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("champion")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Champion")) {
																if (world instanceof Level _level) {
																	if (!_level.isClientSide()) {
																		_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
																	} else {
																		_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
																	}
																}
																if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																	((Slot) _slots.get(0)).remove(1);
																	_player.containerMenu.broadcastChanges();
																}
																ScpAdditionsMod.queueServerWork(40, () -> {
																	if (entity instanceof Player _player) {
																		ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CHAMPION.get());
																		_setstack.setCount(1);
																		ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																	}
																});
																ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
															} else {
																if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("fear")
																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Fear")
																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("scare")
																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Scare")
																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("horror")
																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Horror")
																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("terror")
																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Terror")) {
																	if (world instanceof Level _level) {
																		if (!_level.isClientSide()) {
																			_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
																		} else {
																			_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
																		}
																	}
																	if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																		((Slot) _slots.get(0)).remove(1);
																		_player.containerMenu.broadcastChanges();
																	}
																	ScpAdditionsMod.queueServerWork(40, () -> {
																		if (entity instanceof Player _player) {
																			ItemStack _setstack = new ItemStack(ScpAdditionsModItems.FEAR.get());
																			_setstack.setCount(1);
																			ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																		}
																	});
																	ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																	ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																} else {
																	if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("feces")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Feces")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("fecal matter")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Fecal matter")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("shit")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Shit")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("crap")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Crap")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("poo")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Poo")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("poop")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Poop")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("dung")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Dung")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("scat")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Scat")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("turd")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Turd")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("bullshit")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Bullshit")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("horseshit")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Horseshit")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("diarrhea")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Diarrhea")) {
																		if (world instanceof Level _level) {
																			if (!_level.isClientSide()) {
																				_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1,
																						1);
																			} else {
																				_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
																			}
																		}
																		if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																			((Slot) _slots.get(0)).remove(1);
																			_player.containerMenu.broadcastChanges();
																		}
																		ScpAdditionsMod.queueServerWork(40, () -> {
																			if (entity instanceof Player _player) {
																				ItemStack _setstack = new ItemStack(ScpAdditionsModItems.FECES.get());
																				_setstack.setCount(1);
																				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																			}
																		});
																		ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																		ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																	} else {
																		if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("feces and blood")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Feces and blood")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("blood and feces")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Blood and feces")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Feces and Blood")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Blood and Feces")) {
																			if (world instanceof Level _level) {
																				if (!_level.isClientSide()) {
																					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL,
																							1, 1);
																				} else {
																					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
																				}
																			}
																			if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																				((Slot) _slots.get(0)).remove(1);
																				_player.containerMenu.broadcastChanges();
																			}
																			ScpAdditionsMod.queueServerWork(40, () -> {
																				if (entity instanceof Player _player) {
																					ItemStack _setstack = new ItemStack(ScpAdditionsModItems.FECES_AND_BLOOD.get());
																					_setstack.setCount(1);
																					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																				}
																			});
																			ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																			ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																		} else {
																			if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("gin")
																					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Gin")
																					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("gin and tonic")
																					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Gin and Tonic")
																					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("tonic and gin")
																					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Tonic and Gin")
																					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("gin & tonic")
																					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Gin & Tonic")
																					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("tonic & gin")
																					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Tonic & Gin")) {
																				if (world instanceof Level _level) {
																					if (!_level.isClientSide()) {
																						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																								SoundSource.NEUTRAL, 1, 1);
																					} else {
																						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
																					}
																				}
																				if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																					((Slot) _slots.get(0)).remove(1);
																					_player.containerMenu.broadcastChanges();
																				}
																				ScpAdditionsMod.queueServerWork(40, () -> {
																					if (entity instanceof Player _player) {
																						ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GIN.get());
																						_setstack.setCount(1);
																						ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																					}
																				});
																				ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																				ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																			} else {
																				if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("glass")
																						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Glass")) {
																					if (world instanceof Level _level) {
																						if (!_level.isClientSide()) {
																							_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																									SoundSource.NEUTRAL, 1, 1);
																						} else {
																							_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1, false);
																						}
																					}
																					if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																						((Slot) _slots.get(0)).remove(1);
																						_player.containerMenu.broadcastChanges();
																					}
																					ScpAdditionsMod.queueServerWork(40, () -> {
																						if (entity instanceof Player _player) {
																							ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GLASS.get());
																							_setstack.setCount(1);
																							ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																						}
																					});
																					ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																					ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																				} else {
																					if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("gold")
																							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Gold")) {
																						if (world instanceof Level _level) {
																							if (!_level.isClientSide()) {
																								_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																										SoundSource.NEUTRAL, 1, 1);
																							} else {
																								_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1,
																										false);
																							}
																						}
																						if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																							((Slot) _slots.get(0)).remove(1);
																							_player.containerMenu.broadcastChanges();
																						}
																						ScpAdditionsMod.queueServerWork(40, () -> {
																							if (entity instanceof Player _player) {
																								ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GOLD_C.get());
																								_setstack.setCount(1);
																								ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																							}
																						});
																						ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																						ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																					} else {
																						if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("grog")
																								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Grog")) {
																							if (world instanceof Level _level) {
																								if (!_level.isClientSide()) {
																									_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																											SoundSource.NEUTRAL, 1, 1);
																								} else {
																									_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1,
																											false);
																								}
																							}
																							if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																								((Slot) _slots.get(0)).remove(1);
																								_player.containerMenu.broadcastChanges();
																							}
																							ScpAdditionsMod.queueServerWork(40, () -> {
																								if (entity instanceof Player _player) {
																									ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GROG.get());
																									_setstack.setCount(1);
																									ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																								}
																							});
																							ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																							ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																						} else {
																							if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("happiness")
																									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Happiness")
																									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("cheerfulness")
																									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Cheerfulness")
																									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("joy")
																									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Joy")) {
																								if (world instanceof Level _level) {
																									if (!_level.isClientSide()) {
																										_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																												SoundSource.NEUTRAL, 1, 1);
																									} else {
																										_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1,
																												1, false);
																									}
																								}
																								if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																									((Slot) _slots.get(0)).remove(1);
																									_player.containerMenu.broadcastChanges();
																								}
																								ScpAdditionsMod.queueServerWork(40, () -> {
																									if (entity instanceof Player _player) {
																										ItemStack _setstack = new ItemStack(ScpAdditionsModItems.HAPPINESS.get());
																										_setstack.setCount(1);
																										ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																									}
																								});
																								ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																								ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																							} else {
																								if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("heroin")
																										|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Heroin")) {
																									if (world instanceof Level _level) {
																										if (!_level.isClientSide()) {
																											_level.playSound(null, BlockPos.containing(x, y, z),
																													ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
																										} else {
																											_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																													SoundSource.NEUTRAL, 1, 1, false);
																										}
																									}
																									if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																										((Slot) _slots.get(0)).remove(1);
																										_player.containerMenu.broadcastChanges();
																									}
																									ScpAdditionsMod.queueServerWork(40, () -> {
																										if (entity instanceof Player _player) {
																											ItemStack _setstack = new ItemStack(ScpAdditionsModItems.HEROIN.get());
																											_setstack.setCount(1);
																											ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																										}
																									});
																									ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																									ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																								} else {
																									if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("morphine")
																											|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Morphine")) {
																										if (world instanceof Level _level) {
																											if (!_level.isClientSide()) {
																												_level.playSound(null, BlockPos.containing(x, y, z),
																														ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
																											} else {
																												_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																														SoundSource.NEUTRAL, 1, 1, false);
																											}
																										}
																										if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																											((Slot) _slots.get(0)).remove(1);
																											_player.containerMenu.broadcastChanges();
																										}
																										ScpAdditionsMod.queueServerWork(40, () -> {
																											if (entity instanceof Player _player) {
																												ItemStack _setstack = new ItemStack(ScpAdditionsModItems.MORPHINE.get());
																												_setstack.setCount(1);
																												ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																											}
																										});
																										ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																										ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																									} else {
																										if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("honey")
																												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Honey")) {
																											if (world instanceof Level _level) {
																												if (!_level.isClientSide()) {
																													_level.playSound(null, BlockPos.containing(x, y, z),
																															ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
																												} else {
																													_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																															SoundSource.NEUTRAL, 1, 1, false);
																												}
																											}
																											if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																												((Slot) _slots.get(0)).remove(1);
																												_player.containerMenu.broadcastChanges();
																											}
																											ScpAdditionsMod.queueServerWork(40, () -> {
																												if (entity instanceof Player _player) {
																													ItemStack _setstack = new ItemStack(ScpAdditionsModItems.HONEY.get());
																													_setstack.setCount(1);
																													ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																												}
																											});
																											ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																											ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																										} else {
																											if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("hot")
																													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Hot")
																													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("warm")
																													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Warm")) {
																												if (world instanceof Level _level) {
																													if (!_level.isClientSide()) {
																														_level.playSound(null, BlockPos.containing(x, y, z),
																																ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1, 1);
																													} else {
																														_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																																SoundSource.NEUTRAL, 1, 1, false);
																													}
																												}
																												if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																													((Slot) _slots.get(0)).remove(1);
																													_player.containerMenu.broadcastChanges();
																												}
																												ScpAdditionsMod.queueServerWork(40, () -> {
																													if (entity instanceof Player _player) {
																														ItemStack _setstack = new ItemStack(ScpAdditionsModItems.HOT.get());
																														_setstack.setCount(1);
																														ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																													}
																												});
																												ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																												ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																											} else {
																												if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("tea")
																														|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Tea")
																														|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("green tea")
																														|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																.equals("Green tea")) {
																													if (world instanceof Level _level) {
																														if (!_level.isClientSide()) {
																															_level.playSound(null, BlockPos.containing(x, y, z),
																																	ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL, 1,
																																	1);
																														} else {
																															_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																																	SoundSource.NEUTRAL, 1, 1, false);
																														}
																													}
																													if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																														((Slot) _slots.get(0)).remove(1);
																														_player.containerMenu.broadcastChanges();
																													}
																													ScpAdditionsMod.queueServerWork(40, () -> {
																														if (entity instanceof Player _player) {
																															ItemStack _setstack = new ItemStack(ScpAdditionsModItems.TEA.get());
																															_setstack.setCount(1);
																															ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																														}
																													});
																													ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																													ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																												} else {
																													if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																															.equals("hydroflouric acid")
																															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																	.equals("Hydroflouric Acid")
																															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																	.equals("hydrochloric acid")
																															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																	.equals("Hydrochloric Acid")
																															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																	.equals("corrosive acid")
																															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																	.equals("Corrosive Acid")) {
																														if (world instanceof Level _level) {
																															if (!_level.isClientSide()) {
																																_level.playSound(null, BlockPos.containing(x, y, z),
																																		ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")), SoundSource.NEUTRAL,
																																		1, 1);
																															} else {
																																_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																																		SoundSource.NEUTRAL, 1, 1, false);
																															}
																														}
																														if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current
																																&& _current.get() instanceof Map _slots) {
																															((Slot) _slots.get(0)).remove(1);
																															_player.containerMenu.broadcastChanges();
																														}
																														ScpAdditionsMod.queueServerWork(40, () -> {
																															if (entity instanceof Player _player) {
																																ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CORROSIVE_ACID.get());
																																_setstack.setCount(1);
																																ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																															}
																														});
																														ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock
																																+ 1;
																														ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																													} else {
																														if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("ice cream")
																																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																		.equals("Ice Cream")) {
																															if (world instanceof Level _level) {
																																if (!_level.isClientSide()) {
																																	_level.playSound(null, BlockPos.containing(x, y, z),
																																			ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																																			SoundSource.NEUTRAL, 1, 1);
																																} else {
																																	_level.playLocalSound(x, y, z,
																																			ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
																																			SoundSource.NEUTRAL, 1, 1, false);
																																}
																															}
																															if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current
																																	&& _current.get() instanceof Map _slots) {
																																((Slot) _slots.get(0)).remove(1);
																																_player.containerMenu.broadcastChanges();
																															}
																															ScpAdditionsMod.queueServerWork(40, () -> {
																																if (entity instanceof Player _player) {
																																	ItemStack _setstack = new ItemStack(ScpAdditionsModItems.ICE_CREAM.get());
																																	_setstack.setCount(1);
																																	ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																																}
																															});
																															ScpAdditionsModVariables.WorldVariables
																																	.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																															ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																														} else {
																															Scp294drinkGive3Procedure.execute(world, x, y, z, entity, guistate);
																														}
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
