package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;

import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;
import java.util.List;
import java.util.Comparator;

public class DeconClosedBlockAddedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		{
			final Vec3 _center = new Vec3((x - 0.5), y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2.5 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(entityiterator.getX(), entityiterator.getY() + 1, entityiterator.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:decontamination")),
								SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound((entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:decontamination")), SoundSource.NEUTRAL, 1, 1,
								false);
					}
				}
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
				ScpAdditionsMod.queueServerWork(5, () -> {
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
					ScpAdditionsMod.queueServerWork(5, () -> {
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
						ScpAdditionsMod.queueServerWork(5, () -> {
							if (world instanceof ServerLevel _level)
								_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
							ScpAdditionsMod.queueServerWork(5, () -> {
								if (world instanceof ServerLevel _level)
									_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
								ScpAdditionsMod.queueServerWork(5, () -> {
									if (world instanceof ServerLevel _level)
										_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
									ScpAdditionsMod.queueServerWork(5, () -> {
										if (world instanceof ServerLevel _level)
											_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
										ScpAdditionsMod.queueServerWork(5, () -> {
											if (world instanceof ServerLevel _level)
												_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
											ScpAdditionsMod.queueServerWork(5, () -> {
												if (world instanceof ServerLevel _level)
													_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
												ScpAdditionsMod.queueServerWork(5, () -> {
													if (world instanceof ServerLevel _level)
														_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
													ScpAdditionsMod.queueServerWork(5, () -> {
														if (world instanceof ServerLevel _level)
															_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
														ScpAdditionsMod.queueServerWork(5, () -> {
															if (world instanceof ServerLevel _level)
																_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
															ScpAdditionsMod.queueServerWork(5, () -> {
																if (world instanceof ServerLevel _level)
																	_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
																ScpAdditionsMod.queueServerWork(5, () -> {
																	if (world instanceof ServerLevel _level)
																		_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
																	ScpAdditionsMod.queueServerWork(5, () -> {
																		if (world instanceof ServerLevel _level)
																			_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
																		ScpAdditionsMod.queueServerWork(5, () -> {
																			if (world instanceof ServerLevel _level)
																				_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
																			ScpAdditionsMod.queueServerWork(5, () -> {
																				if (world instanceof ServerLevel _level)
																					_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
																				ScpAdditionsMod.queueServerWork(5, () -> {
																					if (world instanceof ServerLevel _level)
																						_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
																					ScpAdditionsMod.queueServerWork(5, () -> {
																						if (world instanceof ServerLevel _level)
																							_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
																						ScpAdditionsMod.queueServerWork(5, () -> {
																							if (world instanceof ServerLevel _level)
																								_level.sendParticles(ParticleTypes.CLOUD, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 100, 1, 1, 1, 1);
																						});
																					});
																				});
																			});
																		});
																	});
																});
															});
														});
													});
												});
											});
										});
									});
								});
							});
						});
					});
				});
				if (entityiterator instanceof LivingEntity _entity)
					_entity.removeAllEffects();
				if (world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.DECONCHECKPOINT)) {
					if (entityiterator instanceof ServerPlayer _serverPlayer)
						_serverPlayer.setRespawnPosition(_serverPlayer.level().dimension(), BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), _serverPlayer.getYRot(), true, false);
				}
				if (entityiterator instanceof ServerPlayer _player) {
					Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("scp_additions:decon_achievement"));
					AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
					if (!_ap.isDone()) {
						for (String criteria : _ap.getRemainingCriteria())
							_player.getAdvancements().award(_adv, criteria);
					}
				}
			}
		}
		ScpAdditionsMod.queueServerWork(100, () -> {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x - 0.5, y, z + 1), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:dooropen")), SoundSource.NEUTRAL, 1, 1);
				} else {
					_level.playLocalSound((x - 0.5), y, (z + 1), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:dooropen")), SoundSource.NEUTRAL, 1, 1, false);
				}
			}
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x - 0.5, y, z - 1), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:dooropen")), SoundSource.NEUTRAL, 1, 1);
				} else {
					_level.playLocalSound((x - 0.5), y, (z - 1), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:dooropen")), SoundSource.NEUTRAL, 1, 1, false);
				}
			}
			{
				BlockPos _bp = BlockPos.containing(x, y, z);
				BlockState _bs = ScpAdditionsModBlocks.DECON_OPEN_RELOAD.get().defaultBlockState();
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
		});
	}
}
