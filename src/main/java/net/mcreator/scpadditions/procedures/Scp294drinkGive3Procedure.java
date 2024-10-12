package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.EditBox;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class Scp294drinkGive3Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, HashMap guistate) {
		if (entity == null || guistate == null)
			return;
		if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("frozen yogurt")
				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Frozen Yogurt")
				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Frozen yogurt")) {
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
					ItemStack _setstack = new ItemStack(ScpAdditionsModItems.FROZEN_YOGURT.get());
					_setstack.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
				}
			});
			ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
			ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
		} else {
			if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("yogurt")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Yogurt")) {
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
						ItemStack _setstack = new ItemStack(ScpAdditionsModItems.YOGURT.get());
						_setstack.setCount(1);
						ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
					}
				});
				ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
				ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
			} else {
				if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("ink")
						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Ink")) {
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
							ItemStack _setstack = new ItemStack(ScpAdditionsModItems.INK.get());
							_setstack.setCount(1);
							ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
						}
					});
					ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
					ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
				} else {
					if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("insulin")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Insulin")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("novorapid")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("NovoRapid")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("novo rapid")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Novo Rapid")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("glargine")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Glargine")) {
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
								ItemStack _setstack = new ItemStack(ScpAdditionsModItems.INSULIN.get());
								_setstack.setCount(1);
								ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
							}
						});
						ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
						ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
					} else {
						if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("ipecac")
								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Ipecac")) {
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
									ItemStack _setstack = new ItemStack(ScpAdditionsModItems.IPECAC.get());
									_setstack.setCount(1);
									ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
								}
							});
							ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
							ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
						} else {
							if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("iron")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Iron")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("steel")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Steel")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("metal")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Metal")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("razor blades")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Razor blades")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("razorblades")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Razorblades")) {
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
										ItemStack _setstack = new ItemStack(ScpAdditionsModItems.IRON_C.get());
										_setstack.setCount(1);
										ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
									}
								});
								ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
								ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
							}
						}
					}
				}
			}
		}
	}
}
