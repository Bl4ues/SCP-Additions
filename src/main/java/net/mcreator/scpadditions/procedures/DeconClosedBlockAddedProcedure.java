package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.state.Property;
import net.minecraft.server.MinecraftServer;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.block.BlockState;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;

import net.mcreator.scpadditions.world.DeconCheckpointGameRule;
import net.mcreator.scpadditions.block.DeconOpenReloadBlock;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.Comparator;

public class DeconClosedBlockAddedProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure DeconClosedBlockAdded!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency x for procedure DeconClosedBlockAdded!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency y for procedure DeconClosedBlockAdded!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency z for procedure DeconClosedBlockAdded!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		double x = dependencies.get("x") instanceof Integer ? (int) dependencies.get("x") : (double) dependencies.get("x");
		double y = dependencies.get("y") instanceof Integer ? (int) dependencies.get("y") : (double) dependencies.get("y");
		double z = dependencies.get("z") instanceof Integer ? (int) dependencies.get("z") : (double) dependencies.get("z");
		{
			List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
					new AxisAlignedBB((x - 0.5) - (2.5 / 2d), y - (2.5 / 2d), z - (2.5 / 2d), (x - 0.5) + (2.5 / 2d), y + (2.5 / 2d), z + (2.5 / 2d)),
					null).stream().sorted(new Object() {
						Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
							return Comparator.comparing((Function<Entity, Double>) (_entcnd -> _entcnd.getDistanceSq(_x, _y, _z)));
						}
					}.compareDistOf((x - 0.5), y, z)).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (world instanceof World && !world.isRemote()) {
					((World) world)
							.playSound(null, new BlockPos(entityiterator.getPosX(), entityiterator.getPosY() + 1, entityiterator.getPosZ()),
									(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
											.getValue(new ResourceLocation("scp_additions:decontamination")),
									SoundCategory.NEUTRAL, (float) 1, (float) 1);
				} else {
					((World) world).playSound((entityiterator.getPosX()), (entityiterator.getPosY() + 1), (entityiterator.getPosZ()),
							(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
									.getValue(new ResourceLocation("scp_additions:decontamination")),
							SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
				}
				if (world instanceof ServerWorld) {
					((ServerWorld) world).spawnParticle(ParticleTypes.CLOUD, (entityiterator.getPosX()), (entityiterator.getPosY() + 1),
							(entityiterator.getPosZ()), (int) 100, 1, 1, 1, 1);
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
						if (world instanceof ServerWorld) {
							((ServerWorld) world).spawnParticle(ParticleTypes.CLOUD, (entityiterator.getPosX()), (entityiterator.getPosY() + 1),
									(entityiterator.getPosZ()), (int) 100, 1, 1, 1, 1);
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
								if (world instanceof ServerWorld) {
									((ServerWorld) world).spawnParticle(ParticleTypes.CLOUD, (entityiterator.getPosX()),
											(entityiterator.getPosY() + 1), (entityiterator.getPosZ()), (int) 100, 1, 1, 1, 1);
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
										if (world instanceof ServerWorld) {
											((ServerWorld) world).spawnParticle(ParticleTypes.CLOUD, (entityiterator.getPosX()),
													(entityiterator.getPosY() + 1), (entityiterator.getPosZ()), (int) 100, 1, 1, 1, 1);
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
												if (world instanceof ServerWorld) {
													((ServerWorld) world).spawnParticle(ParticleTypes.CLOUD, (entityiterator.getPosX()),
															(entityiterator.getPosY() + 1), (entityiterator.getPosZ()), (int) 100, 1, 1, 1, 1);
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
														if (world instanceof ServerWorld) {
															((ServerWorld) world).spawnParticle(ParticleTypes.CLOUD, (entityiterator.getPosX()),
																	(entityiterator.getPosY() + 1), (entityiterator.getPosZ()), (int) 100, 1, 1, 1,
																	1);
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
																if (world instanceof ServerWorld) {
																	((ServerWorld) world).spawnParticle(ParticleTypes.CLOUD,
																			(entityiterator.getPosX()), (entityiterator.getPosY() + 1),
																			(entityiterator.getPosZ()), (int) 100, 1, 1, 1, 1);
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
																		if (world instanceof ServerWorld) {
																			((ServerWorld) world).spawnParticle(ParticleTypes.CLOUD,
																					(entityiterator.getPosX()), (entityiterator.getPosY() + 1),
																					(entityiterator.getPosZ()), (int) 100, 1, 1, 1, 1);
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
																				if (world instanceof ServerWorld) {
																					((ServerWorld) world).spawnParticle(ParticleTypes.CLOUD,
																							(entityiterator.getPosX()),
																							(entityiterator.getPosY() + 1),
																							(entityiterator.getPosZ()), (int) 100, 1, 1, 1, 1);
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
																						if (world instanceof ServerWorld) {
																							((ServerWorld) world).spawnParticle(ParticleTypes.CLOUD,
																									(entityiterator.getPosX()),
																									(entityiterator.getPosY() + 1),
																									(entityiterator.getPosZ()), (int) 100, 1, 1, 1,
																									1);
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
																								if (world instanceof ServerWorld) {
																									((ServerWorld) world).spawnParticle(
																											ParticleTypes.CLOUD,
																											(entityiterator.getPosX()),
																											(entityiterator.getPosY() + 1),
																											(entityiterator.getPosZ()), (int) 100, 1,
																											1, 1, 1);
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
																									public void tick(
																											TickEvent.ServerTickEvent event) {
																										if (event.phase == TickEvent.Phase.END) {
																											this.ticks += 1;
																											if (this.ticks >= this.waitTicks)
																												run();
																										}
																									}

																									private void run() {
																										if (world instanceof ServerWorld) {
																											((ServerWorld) world).spawnParticle(
																													ParticleTypes.CLOUD,
																													(entityiterator.getPosX()),
																													(entityiterator.getPosY() + 1),
																													(entityiterator.getPosZ()),
																													(int) 100, 1, 1, 1, 1);
																										}
																										new Object() {
																											private int ticks = 0;
																											private float waitTicks;
																											private IWorld world;

																											public void start(IWorld world,
																													int waitTicks) {
																												this.waitTicks = waitTicks;
																												MinecraftForge.EVENT_BUS
																														.register(this);
																												this.world = world;
																											}

																											@SubscribeEvent
																											public void tick(
																													TickEvent.ServerTickEvent event) {
																												if (event.phase == TickEvent.Phase.END) {
																													this.ticks += 1;
																													if (this.ticks >= this.waitTicks)
																														run();
																												}
																											}

																											private void run() {
																												if (world instanceof ServerWorld) {
																													((ServerWorld) world)
																															.spawnParticle(
																																	ParticleTypes.CLOUD,
																																	(entityiterator
																																			.getPosX()),
																																	(entityiterator
																																			.getPosY()
																																			+ 1),
																																	(entityiterator
																																			.getPosZ()),
																																	(int) 100, 1, 1,
																																	1, 1);
																												}
																												new Object() {
																													private int ticks = 0;
																													private float waitTicks;
																													private IWorld world;

																													public void start(IWorld world,
																															int waitTicks) {
																														this.waitTicks = waitTicks;
																														MinecraftForge.EVENT_BUS
																																.register(this);
																														this.world = world;
																													}

																													@SubscribeEvent
																													public void tick(
																															TickEvent.ServerTickEvent event) {
																														if (event.phase == TickEvent.Phase.END) {
																															this.ticks += 1;
																															if (this.ticks >= this.waitTicks)
																																run();
																														}
																													}

																													private void run() {
																														if (world instanceof ServerWorld) {
																															((ServerWorld) world)
																																	.spawnParticle(
																																			ParticleTypes.CLOUD,
																																			(entityiterator
																																					.getPosX()),
																																			(entityiterator
																																					.getPosY()
																																					+ 1),
																																			(entityiterator
																																					.getPosZ()),
																																			(int) 100,
																																			1, 1, 1,
																																			1);
																														}
																														new Object() {
																															private int ticks = 0;
																															private float waitTicks;
																															private IWorld world;

																															public void start(
																																	IWorld world,
																																	int waitTicks) {
																																this.waitTicks = waitTicks;
																																MinecraftForge.EVENT_BUS
																																		.register(
																																				this);
																																this.world = world;
																															}

																															@SubscribeEvent
																															public void tick(
																																	TickEvent.ServerTickEvent event) {
																																if (event.phase == TickEvent.Phase.END) {
																																	this.ticks += 1;
																																	if (this.ticks >= this.waitTicks)
																																		run();
																																}
																															}

																															private void run() {
																																if (world instanceof ServerWorld) {
																																	((ServerWorld) world)
																																			.spawnParticle(
																																					ParticleTypes.CLOUD,
																																					(entityiterator
																																							.getPosX()),
																																					(entityiterator
																																							.getPosY()
																																							+ 1),
																																					(entityiterator
																																							.getPosZ()),
																																					(int) 100,
																																					1,
																																					1,
																																					1,
																																					1);
																																}
																																new Object() {
																																	private int ticks = 0;
																																	private float waitTicks;
																																	private IWorld world;

																																	public void start(
																																			IWorld world,
																																			int waitTicks) {
																																		this.waitTicks = waitTicks;
																																		MinecraftForge.EVENT_BUS
																																				.register(
																																						this);
																																		this.world = world;
																																	}

																																	@SubscribeEvent
																																	public void tick(
																																			TickEvent.ServerTickEvent event) {
																																		if (event.phase == TickEvent.Phase.END) {
																																			this.ticks += 1;
																																			if (this.ticks >= this.waitTicks)
																																				run();
																																		}
																																	}

																																	private void run() {
																																		if (world instanceof ServerWorld) {
																																			((ServerWorld) world)
																																					.spawnParticle(
																																							ParticleTypes.CLOUD,
																																							(entityiterator
																																									.getPosX()),
																																							(entityiterator
																																									.getPosY()
																																									+ 1),
																																							(entityiterator
																																									.getPosZ()),
																																							(int) 100,
																																							1,
																																							1,
																																							1,
																																							1);
																																		}
																																		new Object() {
																																			private int ticks = 0;
																																			private float waitTicks;
																																			private IWorld world;

																																			public void start(
																																					IWorld world,
																																					int waitTicks) {
																																				this.waitTicks = waitTicks;
																																				MinecraftForge.EVENT_BUS
																																						.register(
																																								this);
																																				this.world = world;
																																			}

																																			@SubscribeEvent
																																			public void tick(
																																					TickEvent.ServerTickEvent event) {
																																				if (event.phase == TickEvent.Phase.END) {
																																					this.ticks += 1;
																																					if (this.ticks >= this.waitTicks)
																																						run();
																																				}
																																			}

																																			private void run() {
																																				if (world instanceof ServerWorld) {
																																					((ServerWorld) world)
																																							.spawnParticle(
																																									ParticleTypes.CLOUD,
																																									(entityiterator
																																											.getPosX()),
																																									(entityiterator
																																											.getPosY()
																																											+ 1),
																																									(entityiterator
																																											.getPosZ()),
																																									(int) 100,
																																									1,
																																									1,
																																									1,
																																									1);
																																				}
																																				new Object() {
																																					private int ticks = 0;
																																					private float waitTicks;
																																					private IWorld world;

																																					public void start(
																																							IWorld world,
																																							int waitTicks) {
																																						this.waitTicks = waitTicks;
																																						MinecraftForge.EVENT_BUS
																																								.register(
																																										this);
																																						this.world = world;
																																					}

																																					@SubscribeEvent
																																					public void tick(
																																							TickEvent.ServerTickEvent event) {
																																						if (event.phase == TickEvent.Phase.END) {
																																							this.ticks += 1;
																																							if (this.ticks >= this.waitTicks)
																																								run();
																																						}
																																					}

																																					private void run() {
																																						if (world instanceof ServerWorld) {
																																							((ServerWorld) world)
																																									.spawnParticle(
																																											ParticleTypes.CLOUD,
																																											(entityiterator
																																													.getPosX()),
																																											(entityiterator
																																													.getPosY()
																																													+ 1),
																																											(entityiterator
																																													.getPosZ()),
																																											(int) 100,
																																											1,
																																											1,
																																											1,
																																											1);
																																						}
																																						new Object() {
																																							private int ticks = 0;
																																							private float waitTicks;
																																							private IWorld world;

																																							public void start(
																																									IWorld world,
																																									int waitTicks) {
																																								this.waitTicks = waitTicks;
																																								MinecraftForge.EVENT_BUS
																																										.register(
																																												this);
																																								this.world = world;
																																							}

																																							@SubscribeEvent
																																							public void tick(
																																									TickEvent.ServerTickEvent event) {
																																								if (event.phase == TickEvent.Phase.END) {
																																									this.ticks += 1;
																																									if (this.ticks >= this.waitTicks)
																																										run();
																																								}
																																							}

																																							private void run() {
																																								if (world instanceof ServerWorld) {
																																									((ServerWorld) world)
																																											.spawnParticle(
																																													ParticleTypes.CLOUD,
																																													(entityiterator
																																															.getPosX()),
																																													(entityiterator
																																															.getPosY()
																																															+ 1),
																																													(entityiterator
																																															.getPosZ()),
																																													(int) 100,
																																													1,
																																													1,
																																													1,
																																													1);
																																								}
																																								new Object() {
																																									private int ticks = 0;
																																									private float waitTicks;
																																									private IWorld world;

																																									public void start(
																																											IWorld world,
																																											int waitTicks) {
																																										this.waitTicks = waitTicks;
																																										MinecraftForge.EVENT_BUS
																																												.register(
																																														this);
																																										this.world = world;
																																									}

																																									@SubscribeEvent
																																									public void tick(
																																											TickEvent.ServerTickEvent event) {
																																										if (event.phase == TickEvent.Phase.END) {
																																											this.ticks += 1;
																																											if (this.ticks >= this.waitTicks)
																																												run();
																																										}
																																									}

																																									private void run() {
																																										if (world instanceof ServerWorld) {
																																											((ServerWorld) world)
																																													.spawnParticle(
																																															ParticleTypes.CLOUD,
																																															(entityiterator
																																																	.getPosX()),
																																															(entityiterator
																																																	.getPosY()
																																																	+ 1),
																																															(entityiterator
																																																	.getPosZ()),
																																															(int) 100,
																																															1,
																																															1,
																																															1,
																																															1);
																																										}
																																										MinecraftForge.EVENT_BUS
																																												.unregister(
																																														this);
																																									}
																																								}.start(world,
																																										(int) 5);
																																								MinecraftForge.EVENT_BUS
																																										.unregister(
																																												this);
																																							}
																																						}.start(world,
																																								(int) 5);
																																						MinecraftForge.EVENT_BUS
																																								.unregister(
																																										this);
																																					}
																																				}.start(world,
																																						(int) 5);
																																				MinecraftForge.EVENT_BUS
																																						.unregister(
																																								this);
																																			}
																																		}.start(world,
																																				(int) 5);
																																		MinecraftForge.EVENT_BUS
																																				.unregister(
																																						this);
																																	}
																																}.start(world,
																																		(int) 5);
																																MinecraftForge.EVENT_BUS
																																		.unregister(
																																				this);
																															}
																														}.start(world, (int) 5);
																														MinecraftForge.EVENT_BUS
																																.unregister(this);
																													}
																												}.start(world, (int) 5);
																												MinecraftForge.EVENT_BUS
																														.unregister(this);
																											}
																										}.start(world, (int) 5);
																										MinecraftForge.EVENT_BUS.unregister(this);
																									}
																								}.start(world, (int) 5);
																								MinecraftForge.EVENT_BUS.unregister(this);
																							}
																						}.start(world, (int) 5);
																						MinecraftForge.EVENT_BUS.unregister(this);
																					}
																				}.start(world, (int) 5);
																				MinecraftForge.EVENT_BUS.unregister(this);
																			}
																		}.start(world, (int) 5);
																		MinecraftForge.EVENT_BUS.unregister(this);
																	}
																}.start(world, (int) 5);
																MinecraftForge.EVENT_BUS.unregister(this);
															}
														}.start(world, (int) 5);
														MinecraftForge.EVENT_BUS.unregister(this);
													}
												}.start(world, (int) 5);
												MinecraftForge.EVENT_BUS.unregister(this);
											}
										}.start(world, (int) 5);
										MinecraftForge.EVENT_BUS.unregister(this);
									}
								}.start(world, (int) 5);
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 5);
						MinecraftForge.EVENT_BUS.unregister(this);
					}
				}.start(world, (int) 5);
				if (entityiterator instanceof LivingEntity)
					((LivingEntity) entityiterator).clearActivePotions();
				if (world.getWorldInfo().getGameRulesInstance().getBoolean(DeconCheckpointGameRule.gamerule)) {
					if (entityiterator instanceof ServerPlayerEntity)
						((ServerPlayerEntity) entityiterator).func_242111_a(((ServerPlayerEntity) entityiterator).world.getDimensionKey(),
								new BlockPos(entityiterator.getPosX(), entityiterator.getPosY(), entityiterator.getPosZ()), 0, true, false);
				}
				if (entityiterator instanceof ServerPlayerEntity) {
					Advancement _adv = ((MinecraftServer) ((ServerPlayerEntity) entityiterator).server).getAdvancementManager()
							.getAdvancement(new ResourceLocation("scp_additions:decon_achievement"));
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
					((World) world).playSound(null, new BlockPos(x - 0.5, y, z + 1),
							(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:dooropen")),
							SoundCategory.NEUTRAL, (float) 1, (float) 1);
				} else {
					((World) world).playSound((x - 0.5), y, (z + 1),
							(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:dooropen")),
							SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
				}
				if (world instanceof World && !world.isRemote()) {
					((World) world).playSound(null, new BlockPos(x - 0.5, y, z - 1),
							(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:dooropen")),
							SoundCategory.NEUTRAL, (float) 1, (float) 1);
				} else {
					((World) world).playSound((x - 0.5), y, (z - 1),
							(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:dooropen")),
							SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
				}
				{
					BlockPos _bp = new BlockPos(x, y, z);
					BlockState _bs = DeconOpenReloadBlock.block.getDefaultState();
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
				MinecraftForge.EVENT_BUS.unregister(this);
			}
		}.start(world, (int) 100);
	}
}
