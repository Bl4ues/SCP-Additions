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

public class Scp294drinkGiveProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, HashMap guistate) {
		if (entity == null || guistate == null)
			return;
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294enter")), SoundSource.NEUTRAL, 1, 1);
			} else {
				_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294enter")), SoundSource.NEUTRAL, 1, 1, false);
			}
		}
		if (ScpAdditionsModItems.COIN.get() == (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(0)).getItem() : ItemStack.EMPTY).getItem()) {
			if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("air")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Air")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("nothing")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Nothing")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("hl3")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("HL3")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("half life 3")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Half Life 3")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("emptiness")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Emptiness")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("vacuum")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Vacuum")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("cup")
					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Cup")) {
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294emptycup")), SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294emptycup")), SoundSource.NEUTRAL, 1, 1, false);
					}
				}
				if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
					((Slot) _slots.get(0)).remove(1);
					_player.containerMenu.broadcastChanges();
				}
				if (entity instanceof Player _player) {
					ItemStack _setstack = new ItemStack(ScpAdditionsModItems.EMPTY_CUP.get());
					_setstack.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
				}
				ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
				ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
			} else {
				if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Coffee")
						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("coffee")
						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Black coffee")
						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("black coffee")) {
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
							ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CUP_OF_COFFEE.get());
							_setstack.setCount(1);
							ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
						}
					});
					ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
					ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
				} else {
					if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("alcohol")
							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Alcohol")) {
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
								ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CUP_OF_ALCOHOL.get());
								_setstack.setCount(1);
								ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
							}
						});
						ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
						ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
					} else {
						if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("ethanol")
								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Ethanol")
								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("ethanol liquid")
								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Ethanol liquid")) {
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
									ItemStack _setstack = new ItemStack(ScpAdditionsModItems.ETHANOL.get());
									_setstack.setCount(1);
									ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
								}
							});
							ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
							ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
						} else {
							if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("spirit")
									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Spirit")) {
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
										ItemStack _setstack = new ItemStack(ScpAdditionsModItems.SPIRIT.get());
										_setstack.setCount(1);
										ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
									}
								});
								ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
								ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
							} else {
								if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("vodka")
										|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Vodka")) {
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
											ItemStack _setstack = new ItemStack(ScpAdditionsModItems.VODKA.get());
											_setstack.setCount(1);
											ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
										}
									});
									ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
									ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
								} else {
									if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("aloe vera")
											|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Aloe Vera")) {
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
												ItemStack _setstack = new ItemStack(ScpAdditionsModItems.ALOE.get());
												_setstack.setCount(1);
												ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
											}
										});
										ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
										ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
									} else {
										if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("cactus")
												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Cactus")) {
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
													ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CACTUS.get());
													_setstack.setCount(1);
													ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
												}
											});
											ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
											ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
										} else {
											if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("amnesia")
													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Amnesia")) {
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
														ItemStack _setstack = new ItemStack(ScpAdditionsModItems.AMNESIA.get());
														_setstack.setCount(1);
														ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
													}
												});
												ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
												ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
											} else {
												if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("anti-energy")
														|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Anti-Energy")
														|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("anti energy")
														|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Anti Energy")) {
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
															ItemStack _setstack = new ItemStack(ScpAdditionsModItems.ANTI_ENERGY.get());
															_setstack.setCount(1);
															ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
														}
													});
													ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
													ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
												} else {
													if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("antimatter")
															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Antimatter")
															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("anti-matter")
															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Anti-matter")
															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("void")
															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Void")) {
														if (world instanceof Level _level) {
															if (!_level.isClientSide()) {
																_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294emptycup")), SoundSource.NEUTRAL, 1, 1);
															} else {
																_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294emptycup")), SoundSource.NEUTRAL, 1, 1, false);
															}
														}
														if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
															((Slot) _slots.get(0)).remove(1);
															_player.containerMenu.broadcastChanges();
														}
														ScpAdditionsMod.queueServerWork(30, () -> {
															if (world instanceof Level _level && !_level.isClientSide())
																_level.explode(null, x, y, z, 10, Level.ExplosionInteraction.NONE);
															{
																final Vec3 _center = new Vec3(x, y, z);
																List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(10 / 2d), e -> true).stream()
																		.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
																for (Entity entityiterator : _entfound) {
																	if (entityiterator instanceof LivingEntity _entity)
																		_entity.hurt(new DamageSource(_entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)) {
																			@Override
																			public Component getLocalizedDeathMessage(LivingEntity _msgEntity) {
																				String _translatekey = "death.attack." + "void";
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
																}
															}
														});
														ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
														ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
													} else {
														if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("aqua regia")
																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Aqua Regia")) {
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
																	ItemStack _setstack = new ItemStack(ScpAdditionsModItems.AQUA_REGIA.get());
																	_setstack.setCount(1);
																	ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																}
															});
															ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
															ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
														} else {
															if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("atomic")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Atomic")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("nuclear")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Nuclear")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("nuclear fusion")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Nuclear Fusion")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("nuclear fission")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Nuclear Fission")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("nuclear warhead")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Nuclear Warhead")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("oppenheimer")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Oppenheimer")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("nuclear reaction")
																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Nuclear Reaction")) {
																if (world instanceof Level _level) {
																	if (!_level.isClientSide()) {
																		_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294emptycup")), SoundSource.NEUTRAL, 1, 1);
																	} else {
																		_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294emptycup")), SoundSource.NEUTRAL, 1, 1, false);
																	}
																}
																if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
																	((Slot) _slots.get(0)).remove(1);
																	_player.containerMenu.broadcastChanges();
																}
																{
																	final Vec3 _center = new Vec3(x, y, z);
																	List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(30 / 2d), e -> true).stream()
																			.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
																	for (Entity entityiterator : _entfound) {
																		if (world instanceof Level _level) {
																			if (!_level.isClientSide()) {
																				_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:nuclear")), SoundSource.NEUTRAL, 1, 1);
																			} else {
																				_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:nuclear")), SoundSource.NEUTRAL, 1, 1, false);
																			}
																		}
																		{
																			boolean _setval = true;
																			entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
																				capability.nuclear = _setval;
																				capability.syncPlayerVariables(entity);
																			});
																		}
																		ScpAdditionsMod.queueServerWork(40, () -> {
																			if (entityiterator instanceof LivingEntity _entity)
																				_entity.hurt(new DamageSource(_entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)) {
																					@Override
																					public Component getLocalizedDeathMessage(LivingEntity _msgEntity) {
																						String _translatekey = "death.attack." + "nuclear";
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
																if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("beer")
																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Beer")) {
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
																			ItemStack _setstack = new ItemStack(ScpAdditionsModItems.BEER.get());
																			_setstack.setCount(1);
																			ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																		}
																	});
																	ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																	ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																} else {
																	if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("lager")
																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Lager")) {
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
																				ItemStack _setstack = new ItemStack(ScpAdditionsModItems.LAGER.get());
																				_setstack.setCount(1);
																				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																			}
																		});
																		ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																		ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																	} else {
																		if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("black corrosive liquid")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Black corrosive liquid")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("scp-106")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("SCP-106")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("scp 106")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("SCP 106")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("old man")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Old man")
																				|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("106")) {
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
																					ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CORROSIVE_BLACK.get());
																					_setstack.setCount(1);
																					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																				}
																			});
																			ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																			ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																		} else {
																			if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("bleach")
																					|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Bleach")) {
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
																						ItemStack _setstack = new ItemStack(ScpAdditionsModItems.BLEACH.get());
																						_setstack.setCount(1);
																						ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																					}
																				});
																				ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																				ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																			} else {
																				if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("blood")
																						|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Blood")) {
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
																							ItemStack _setstack = new ItemStack(ScpAdditionsModItems.BLOOD.get());
																							_setstack.setCount(1);
																							ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																						}
																					});
																					ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																					ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																				} else {
																					if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("blood of christ")
																							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Blood of Christ")
																							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("blood of jesus")
																							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Blood of Jesus")
																							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("blood of jesus christ")
																							|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Blood of Jesus Christ")) {
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
																							if (entity instanceof Player _player && !_player.level().isClientSide())
																								_player.displayClientMessage(Component.literal("SCP-294: \"Hic est enim Calix S\u00E1nguinis mei\""), false);
																							if (entity instanceof Player _player) {
																								ItemStack _setstack = new ItemStack(ScpAdditionsModItems.BLOOD_OF_CHRIST.get());
																								_setstack.setCount(1);
																								ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																							}
																						});
																						ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																						ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																					} else {
																						if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("grimace")
																								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Grimace")
																								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("grimace shake")
																								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Grimace Shake")
																								|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Grimace shake")) {
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
																									ItemStack _setstack = new ItemStack(ScpAdditionsModItems.GRIMACE_SHAKE.get());
																									_setstack.setCount(1);
																									ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																								}
																							});
																							ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																							ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																						} else {
																							if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("bose-einstein condensate")
																									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Bose-Einstein Condensate")
																									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("bose einstein condensate")
																									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Bose Einstein Condensate")
																									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("quantum gas")
																									|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Quantum Gas")) {
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
																										ItemStack _setstack = new ItemStack(ScpAdditionsModItems.QUANTUM.get());
																										_setstack.setCount(1);
																										ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																									}
																								});
																								ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																								ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																							} else {
																								if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("carbon")
																										|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Carbon")) {
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
																											ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CARBON.get());
																											_setstack.setCount(1);
																											ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																										}
																									});
																									ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																									ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																								} else {
																									if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("cassis fanta")
																											|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Cassis Fanta")) {
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
																												ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CASSIS_FANTA.get());
																												_setstack.setCount(1);
																												ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																											}
																										});
																										ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																										ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																									} else {
																										if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("carrot juice")
																												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Carrot juice")
																												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("carrot")
																												|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Carrot")) {
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
																													ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CARROT.get());
																													_setstack.setCount(1);
																													ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																												}
																											});
																											ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																											ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																										} else {
																											if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("champagne")
																													|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Champagne")) {
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
																														ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CHAMPAGNE.get());
																														_setstack.setCount(1);
																														ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																													}
																												});
																												ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																												ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																											} else {
																												if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("chim")
																														|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("Chim")) {
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
																															ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CHIM.get());
																															_setstack.setCount(1);
																															ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																														}
																													});
																													ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																													ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																												} else {
																													if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "").equals("cider")
																															|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																	.equals("Cider")) {
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
																																ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CIDER.get());
																																_setstack.setCount(1);
																																ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																															}
																														});
																														ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock
																																+ 1;
																														ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																													} else {
																														if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																.equals("apple cider")
																																|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																		.equals("Apple Cider")) {
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
																																	ItemStack _setstack = new ItemStack(ScpAdditionsModItems.APPLE_CIDER.get());
																																	_setstack.setCount(1);
																																	ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																																}
																															});
																															ScpAdditionsModVariables.WorldVariables
																																	.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																															ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																														} else {
																															if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																	.equals("pear cider")
																																	|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																			.equals("Pear Cider")) {
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
																																		ItemStack _setstack = new ItemStack(ScpAdditionsModItems.PEAR_CIDER.get());
																																		_setstack.setCount(1);
																																		ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																																	}
																																});
																																ScpAdditionsModVariables.WorldVariables
																																		.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																																ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																															} else {
																																if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																		.equals("chocolate")
																																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																				.equals("Chocolate")
																																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																				.equals("cocoa")
																																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																				.equals("Cocoa")
																																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																				.equals("hot chocolate")
																																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																				.equals("Hot chocolate")
																																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																				.equals("hot cocoa")
																																		|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																				.equals("Hot cocoa")) {
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
																																			ItemStack _setstack = new ItemStack(ScpAdditionsModItems.CHOCOLATE.get());
																																			_setstack.setCount(1);
																																			ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																																		}
																																	});
																																	ScpAdditionsModVariables.WorldVariables
																																			.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																																	ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																																} else {
																																	if ((guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																			.equals("cocaine")
																																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																					.equals("Cocaine")
																																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																					.equals("cocaine drink")
																																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																					.equals("Cocaine Drink")
																																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																					.equals("cocaine energy drink")
																																			|| (guistate.containsKey("text:scp294input") ? ((EditBox) guistate.get("text:scp294input")).getValue() : "")
																																					.equals("Cocaine Energy Drink")) {
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
																																				ItemStack _setstack = new ItemStack(ScpAdditionsModItems.COCAINE.get());
																																				_setstack.setCount(1);
																																				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
																																			}
																																		});
																																		ScpAdditionsModVariables.WorldVariables
																																				.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
																																		ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																																	} else {
																																		Scp294drinkGive2Procedure.execute(world, x, y, z, entity, guistate);
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
		}
	}
}
