package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;

import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.function.Function;
import java.util.Map;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

public class OnetoOneAliveProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure OnetoOneAlive!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency x for procedure OnetoOneAlive!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency y for procedure OnetoOneAlive!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency z for procedure OnetoOneAlive!");
			return;
		}
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure OnetoOneAlive!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		double x = dependencies.get("x") instanceof Integer ? (int) dependencies.get("x") : (double) dependencies.get("x");
		double y = dependencies.get("y") instanceof Integer ? (int) dependencies.get("y") : (double) dependencies.get("y");
		double z = dependencies.get("z") instanceof Integer ? (int) dependencies.get("z") : (double) dependencies.get("z");
		Entity entity = (Entity) dependencies.get("entity");
		if (((Entity) world.getEntitiesWithinAABB(PlayerEntity.class,
				new AxisAlignedBB((x - 4) - (3 / 2d), y - (3 / 2d), (z - 3) - (3 / 2d), (x - 4) + (3 / 2d), y + (3 / 2d), (z - 3) + (3 / 2d)), null)
				.stream().sorted(new Object() {
					Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
						return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
					}
				}.compareDistOf((x - 4), y, (z - 3))).findFirst().orElse(null)) != null) {
			if (world instanceof World && !world.isRemote()) {
				((World) world).playSound(null, new BlockPos(x - 4, entity.getPosY(), entity.getPosZ()),
						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914refining")),
						SoundCategory.NEUTRAL, (float) 1, (float) 1);
			} else {
				((World) world).playSound((x - 4), (entity.getPosY()), (entity.getPosZ()),
						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp914refining")),
						SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
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
					{
						Entity _ent = entity;
						_ent.setPositionAndUpdate((x + 4), (entity.getPosY()), (entity.getPosZ()));
						if (_ent instanceof ServerPlayerEntity) {
							((ServerPlayerEntity) _ent).connection.setPlayerLocation((x + 4), (entity.getPosY()), (entity.getPosZ()),
									_ent.rotationYaw, _ent.rotationPitch, Collections.emptySet());
						}
					}
					ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = (false);
					ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
					if (entity instanceof ServerPlayerEntity) {
						Advancement _adv = ((MinecraftServer) ((ServerPlayerEntity) entity).server).getAdvancementManager()
								.getAdvancement(new ResourceLocation("scp_additions:scp_914_metamorphosis"));
						AdvancementProgress _ap = ((ServerPlayerEntity) entity).getAdvancements().getProgress(_adv);
						if (!_ap.isDone()) {
							Iterator _iterator = _ap.getRemaningCriteria().iterator();
							while (_iterator.hasNext()) {
								String _criterion = (String) _iterator.next();
								((ServerPlayerEntity) entity).getAdvancements().grantCriterion(_adv, _criterion);
							}
						}
					}
					if (Math.random() < 0.25) {
						{
							boolean _setval = (true);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.PlayerOn1to1 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						{
							boolean _setval = (false);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.PlayerOn1to1_2 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						{
							boolean _setval = (false);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.PlayerOn1to1_3 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						{
							boolean _setval = (false);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.PlayerOn1to1_4 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						{
							boolean _setval = (false);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.PlayerOn1to1_5 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						{
							boolean _setval = (false);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.PlayerOn1to1_6 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						{
							boolean _setval = (false);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.PlayerOn1to1_7 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						{
							boolean _setval = (false);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.PlayerOn1to1_8 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						{
							boolean _setval = (false);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.PlayerOn1to1_9 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						{
							boolean _setval = (false);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.PlayerOn1to1_10 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						{
							boolean _setval = (false);
							entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.PlayerOn1to1_11 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					} else {
						if (Math.random() < 0.25) {
							{
								boolean _setval = (false);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.PlayerOn1to1 = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
							{
								boolean _setval = (true);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.PlayerOn1to1_2 = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
							{
								boolean _setval = (false);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.PlayerOn1to1_3 = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
							{
								boolean _setval = (false);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.PlayerOn1to1_4 = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
							{
								boolean _setval = (false);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.PlayerOn1to1_5 = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
							{
								boolean _setval = (false);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.PlayerOn1to1_6 = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
							{
								boolean _setval = (false);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.PlayerOn1to1_7 = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
							{
								boolean _setval = (false);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.PlayerOn1to1_8 = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
							{
								boolean _setval = (false);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.PlayerOn1to1_9 = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
							{
								boolean _setval = (false);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.PlayerOn1to1_10 = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
							{
								boolean _setval = (false);
								entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.PlayerOn1to1_11 = _setval;
									capability.syncPlayerVariables(entity);
								});
							}
						} else {
							if (Math.random() < 0.25) {
								{
									boolean _setval = (false);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.PlayerOn1to1 = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
								{
									boolean _setval = (false);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.PlayerOn1to1_2 = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
								{
									boolean _setval = (true);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.PlayerOn1to1_3 = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
								{
									boolean _setval = (false);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.PlayerOn1to1_4 = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
								{
									boolean _setval = (false);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.PlayerOn1to1_5 = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
								{
									boolean _setval = (false);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.PlayerOn1to1_6 = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
								{
									boolean _setval = (false);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.PlayerOn1to1_7 = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
								{
									boolean _setval = (false);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.PlayerOn1to1_8 = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
								{
									boolean _setval = (false);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.PlayerOn1to1_9 = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
								{
									boolean _setval = (false);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.PlayerOn1to1_10 = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
								{
									boolean _setval = (false);
									entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.PlayerOn1to1_11 = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
							} else {
								if (Math.random() < 0.25) {
									{
										boolean _setval = (false);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.PlayerOn1to1 = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
									{
										boolean _setval = (false);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.PlayerOn1to1_2 = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
									{
										boolean _setval = (false);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.PlayerOn1to1_3 = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
									{
										boolean _setval = (true);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.PlayerOn1to1_4 = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
									{
										boolean _setval = (false);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.PlayerOn1to1_5 = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
									{
										boolean _setval = (false);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.PlayerOn1to1_6 = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
									{
										boolean _setval = (false);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.PlayerOn1to1_7 = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
									{
										boolean _setval = (false);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.PlayerOn1to1_8 = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
									{
										boolean _setval = (false);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.PlayerOn1to1_9 = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
									{
										boolean _setval = (false);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.PlayerOn1to1_10 = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
									{
										boolean _setval = (false);
										entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.PlayerOn1to1_11 = _setval;
											capability.syncPlayerVariables(entity);
										});
									}
								} else {
									if (Math.random() < 0.25) {
										{
											boolean _setval = (false);
											entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.PlayerOn1to1 = _setval;
												capability.syncPlayerVariables(entity);
											});
										}
										{
											boolean _setval = (false);
											entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.PlayerOn1to1_2 = _setval;
												capability.syncPlayerVariables(entity);
											});
										}
										{
											boolean _setval = (false);
											entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.PlayerOn1to1_3 = _setval;
												capability.syncPlayerVariables(entity);
											});
										}
										{
											boolean _setval = (false);
											entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.PlayerOn1to1_4 = _setval;
												capability.syncPlayerVariables(entity);
											});
										}
										{
											boolean _setval = (true);
											entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.PlayerOn1to1_5 = _setval;
												capability.syncPlayerVariables(entity);
											});
										}
										{
											boolean _setval = (false);
											entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.PlayerOn1to1_6 = _setval;
												capability.syncPlayerVariables(entity);
											});
										}
										{
											boolean _setval = (false);
											entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.PlayerOn1to1_7 = _setval;
												capability.syncPlayerVariables(entity);
											});
										}
										{
											boolean _setval = (false);
											entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.PlayerOn1to1_8 = _setval;
												capability.syncPlayerVariables(entity);
											});
										}
										{
											boolean _setval = (false);
											entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.PlayerOn1to1_9 = _setval;
												capability.syncPlayerVariables(entity);
											});
										}
										{
											boolean _setval = (false);
											entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.PlayerOn1to1_10 = _setval;
												capability.syncPlayerVariables(entity);
											});
										}
										{
											boolean _setval = (false);
											entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
												capability.PlayerOn1to1_11 = _setval;
												capability.syncPlayerVariables(entity);
											});
										}
									} else {
										if (Math.random() < 0.25) {
											{
												boolean _setval = (false);
												entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
														.ifPresent(capability -> {
															capability.PlayerOn1to1 = _setval;
															capability.syncPlayerVariables(entity);
														});
											}
											{
												boolean _setval = (false);
												entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
														.ifPresent(capability -> {
															capability.PlayerOn1to1_2 = _setval;
															capability.syncPlayerVariables(entity);
														});
											}
											{
												boolean _setval = (false);
												entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
														.ifPresent(capability -> {
															capability.PlayerOn1to1_3 = _setval;
															capability.syncPlayerVariables(entity);
														});
											}
											{
												boolean _setval = (false);
												entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
														.ifPresent(capability -> {
															capability.PlayerOn1to1_4 = _setval;
															capability.syncPlayerVariables(entity);
														});
											}
											{
												boolean _setval = (false);
												entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
														.ifPresent(capability -> {
															capability.PlayerOn1to1_5 = _setval;
															capability.syncPlayerVariables(entity);
														});
											}
											{
												boolean _setval = (true);
												entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
														.ifPresent(capability -> {
															capability.PlayerOn1to1_6 = _setval;
															capability.syncPlayerVariables(entity);
														});
											}
											{
												boolean _setval = (false);
												entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
														.ifPresent(capability -> {
															capability.PlayerOn1to1_7 = _setval;
															capability.syncPlayerVariables(entity);
														});
											}
											{
												boolean _setval = (false);
												entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
														.ifPresent(capability -> {
															capability.PlayerOn1to1_8 = _setval;
															capability.syncPlayerVariables(entity);
														});
											}
											{
												boolean _setval = (false);
												entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
														.ifPresent(capability -> {
															capability.PlayerOn1to1_9 = _setval;
															capability.syncPlayerVariables(entity);
														});
											}
											{
												boolean _setval = (false);
												entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
														.ifPresent(capability -> {
															capability.PlayerOn1to1_10 = _setval;
															capability.syncPlayerVariables(entity);
														});
											}
											{
												boolean _setval = (false);
												entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
														.ifPresent(capability -> {
															capability.PlayerOn1to1_11 = _setval;
															capability.syncPlayerVariables(entity);
														});
											}
										} else {
											if (Math.random() < 0.25) {
												{
													boolean _setval = (false);
													entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
															.ifPresent(capability -> {
																capability.PlayerOn1to1 = _setval;
																capability.syncPlayerVariables(entity);
															});
												}
												{
													boolean _setval = (false);
													entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
															.ifPresent(capability -> {
																capability.PlayerOn1to1_2 = _setval;
																capability.syncPlayerVariables(entity);
															});
												}
												{
													boolean _setval = (false);
													entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
															.ifPresent(capability -> {
																capability.PlayerOn1to1_3 = _setval;
																capability.syncPlayerVariables(entity);
															});
												}
												{
													boolean _setval = (false);
													entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
															.ifPresent(capability -> {
																capability.PlayerOn1to1_4 = _setval;
																capability.syncPlayerVariables(entity);
															});
												}
												{
													boolean _setval = (false);
													entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
															.ifPresent(capability -> {
																capability.PlayerOn1to1_5 = _setval;
																capability.syncPlayerVariables(entity);
															});
												}
												{
													boolean _setval = (false);
													entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
															.ifPresent(capability -> {
																capability.PlayerOn1to1_6 = _setval;
																capability.syncPlayerVariables(entity);
															});
												}
												{
													boolean _setval = (true);
													entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
															.ifPresent(capability -> {
																capability.PlayerOn1to1_7 = _setval;
																capability.syncPlayerVariables(entity);
															});
												}
												{
													boolean _setval = (false);
													entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
															.ifPresent(capability -> {
																capability.PlayerOn1to1_8 = _setval;
																capability.syncPlayerVariables(entity);
															});
												}
												{
													boolean _setval = (false);
													entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
															.ifPresent(capability -> {
																capability.PlayerOn1to1_9 = _setval;
																capability.syncPlayerVariables(entity);
															});
												}
												{
													boolean _setval = (false);
													entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
															.ifPresent(capability -> {
																capability.PlayerOn1to1_10 = _setval;
																capability.syncPlayerVariables(entity);
															});
												}
												{
													boolean _setval = (false);
													entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
															.ifPresent(capability -> {
																capability.PlayerOn1to1_11 = _setval;
																capability.syncPlayerVariables(entity);
															});
												}
											} else {
												if (Math.random() < 0.25) {
													{
														boolean _setval = (false);
														entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																.ifPresent(capability -> {
																	capability.PlayerOn1to1 = _setval;
																	capability.syncPlayerVariables(entity);
																});
													}
													{
														boolean _setval = (false);
														entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																.ifPresent(capability -> {
																	capability.PlayerOn1to1_2 = _setval;
																	capability.syncPlayerVariables(entity);
																});
													}
													{
														boolean _setval = (false);
														entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																.ifPresent(capability -> {
																	capability.PlayerOn1to1_3 = _setval;
																	capability.syncPlayerVariables(entity);
																});
													}
													{
														boolean _setval = (false);
														entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																.ifPresent(capability -> {
																	capability.PlayerOn1to1_4 = _setval;
																	capability.syncPlayerVariables(entity);
																});
													}
													{
														boolean _setval = (false);
														entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																.ifPresent(capability -> {
																	capability.PlayerOn1to1_5 = _setval;
																	capability.syncPlayerVariables(entity);
																});
													}
													{
														boolean _setval = (false);
														entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																.ifPresent(capability -> {
																	capability.PlayerOn1to1_6 = _setval;
																	capability.syncPlayerVariables(entity);
																});
													}
													{
														boolean _setval = (false);
														entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																.ifPresent(capability -> {
																	capability.PlayerOn1to1_7 = _setval;
																	capability.syncPlayerVariables(entity);
																});
													}
													{
														boolean _setval = (true);
														entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																.ifPresent(capability -> {
																	capability.PlayerOn1to1_8 = _setval;
																	capability.syncPlayerVariables(entity);
																});
													}
													{
														boolean _setval = (false);
														entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																.ifPresent(capability -> {
																	capability.PlayerOn1to1_9 = _setval;
																	capability.syncPlayerVariables(entity);
																});
													}
													{
														boolean _setval = (false);
														entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																.ifPresent(capability -> {
																	capability.PlayerOn1to1_10 = _setval;
																	capability.syncPlayerVariables(entity);
																});
													}
													{
														boolean _setval = (false);
														entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																.ifPresent(capability -> {
																	capability.PlayerOn1to1_11 = _setval;
																	capability.syncPlayerVariables(entity);
																});
													}
												} else {
													if (Math.random() < 0.25) {
														{
															boolean _setval = (false);
															entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																	.ifPresent(capability -> {
																		capability.PlayerOn1to1 = _setval;
																		capability.syncPlayerVariables(entity);
																	});
														}
														{
															boolean _setval = (false);
															entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																	.ifPresent(capability -> {
																		capability.PlayerOn1to1_2 = _setval;
																		capability.syncPlayerVariables(entity);
																	});
														}
														{
															boolean _setval = (false);
															entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																	.ifPresent(capability -> {
																		capability.PlayerOn1to1_3 = _setval;
																		capability.syncPlayerVariables(entity);
																	});
														}
														{
															boolean _setval = (false);
															entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																	.ifPresent(capability -> {
																		capability.PlayerOn1to1_4 = _setval;
																		capability.syncPlayerVariables(entity);
																	});
														}
														{
															boolean _setval = (false);
															entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																	.ifPresent(capability -> {
																		capability.PlayerOn1to1_5 = _setval;
																		capability.syncPlayerVariables(entity);
																	});
														}
														{
															boolean _setval = (false);
															entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																	.ifPresent(capability -> {
																		capability.PlayerOn1to1_6 = _setval;
																		capability.syncPlayerVariables(entity);
																	});
														}
														{
															boolean _setval = (false);
															entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																	.ifPresent(capability -> {
																		capability.PlayerOn1to1_7 = _setval;
																		capability.syncPlayerVariables(entity);
																	});
														}
														{
															boolean _setval = (false);
															entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																	.ifPresent(capability -> {
																		capability.PlayerOn1to1_8 = _setval;
																		capability.syncPlayerVariables(entity);
																	});
														}
														{
															boolean _setval = (true);
															entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																	.ifPresent(capability -> {
																		capability.PlayerOn1to1_9 = _setval;
																		capability.syncPlayerVariables(entity);
																	});
														}
														{
															boolean _setval = (false);
															entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																	.ifPresent(capability -> {
																		capability.PlayerOn1to1_10 = _setval;
																		capability.syncPlayerVariables(entity);
																	});
														}
														{
															boolean _setval = (false);
															entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																	.ifPresent(capability -> {
																		capability.PlayerOn1to1_11 = _setval;
																		capability.syncPlayerVariables(entity);
																	});
														}
													} else {
														if (Math.random() < 0.25) {
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_2 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_3 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_4 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_5 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_6 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_7 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_8 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_9 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (true);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_10 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_11 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
														} else {
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_2 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_3 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_4 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_5 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_6 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_7 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_8 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_9 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (false);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_10 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
															}
															{
																boolean _setval = (true);
																entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
																		.ifPresent(capability -> {
																			capability.PlayerOn1to1_11 = _setval;
																			capability.syncPlayerVariables(entity);
																		});
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
					MinecraftForge.EVENT_BUS.unregister(this);
				}
			}.start(world, (int) 160);
		}
	}
}
