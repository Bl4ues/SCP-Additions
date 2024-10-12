package net.mcreator.scpadditions.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;

public class Scp426OnBlockRightClickedProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof ServerPlayer _player) {
			Advancement _adv = _player.server.getAdvancements().getAdvancement(new ResourceLocation("scp_additions:scp_426_achievement"));
			AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
			if (!_ap.isDone()) {
				for (String criteria : _ap.getRemainingCriteria())
					_player.getAdvancements().award(_adv, criteria);
			}
		}
		if (Math.random() < 0.3) {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("I'm not sure why, but I suddenly have this urge to make toast."), true);
		} else {
			if (Math.random() < 0.3) {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("I can't help but feel a sense of camaraderie with me, as if we share a common thread in the tapestry of existence."), true);
			} else {
				if (Math.random() < 0.3) {
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("I can almost taste the anticipation, the sizzle, and that delightful aroma when the bread turns golden brown."), true);
				} else {
					if (Math.random() < 0.3) {
						if (entity instanceof Player _player && !_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("The surface here is a bit dusty. I could use a good wipe-down to shine brightly again."), true);
					} else {
						if (Math.random() < 0.3) {
							if (entity instanceof Player _player && !_player.level().isClientSide())
								_player.displayClientMessage(Component.literal("I keep feeling this urge to make everyone's mornings better. A warm, crisp start to the day, you know?"), true);
						} else {
							if (Math.random() < 0.3) {
								if (entity instanceof Player _player && !_player.level().isClientSide())
									_player.displayClientMessage(Component.literal("Sometimes, I feel like I have a hidden purpose, like I'm meant to bring joy in a simple, unassuming way."), true);
							} else {
								if (Math.random() < 0.3) {
									if (entity instanceof Player _player && !_player.level().isClientSide())
										_player.displayClientMessage(Component.literal("I seem to have developed a fondness for the smell of burnt toast. It's oddly nostalgic."), true);
								} else {
									if (Math.random() < 0.3) {
										if (entity instanceof Player _player && !_player.level().isClientSide())
											_player.displayClientMessage(Component.literal("I keep noticing the warmth around me. It's like I'm radiating something special."), true);
									} else {
										if (Math.random() < 0.3) {
											if (entity instanceof Player _player && !_player.level().isClientSide())
												_player.displayClientMessage(Component.literal("I've been trying to recall memories of people enjoying toast. It's almost like they're my own memories."), true);
										} else {
											if (Math.random() < 0.3) {
												if (entity instanceof Player _player && !_player.level().isClientSide())
													_player.displayClientMessage(Component.literal("The taste of burnt crumbs seems oddly familiar. It's like a distant but cherished memory."), true);
											} else {
												if (Math.random() < 0.3) {
													if (entity instanceof Player _player && !_player.level().isClientSide())
														_player.displayClientMessage(Component.literal("Sometimes, I feel like I'm a catalyst for change, like I have the power to turn something mundane into a delight."), true);
												} else {
													if (Math.random() < 0.3) {
														if (entity instanceof Player _player && !_player.level().isClientSide())
															_player.displayClientMessage(Component.literal("I've been trying to recall the feeling of satisfaction that comes with serving up the perfect toast."), true);
													} else {
														if (Math.random() < 0.3) {
															if (entity instanceof Player _player && !_player.level().isClientSide())
																_player.displayClientMessage(Component.literal("The toaster before me is a sleek, stainless steel model, with a lever that seems almost eager to pop up."), true);
														} else {
															if (Math.random() < 0.3) {
																if (entity instanceof Player _player && !_player.level().isClientSide())
																	_player.displayClientMessage(Component.literal("There's a certain elegance in my simplicity,"), true);
															} else {
																if (Math.random() < 0.3) {
																	if (entity instanceof Player _player && !_player.level().isClientSide())
																		_player.displayClientMessage(
																				Component.literal("As I examine me closely, I notice my intricate design details, each contributing to my seemingly ordinary yet captivating allure."), true);
																} else {
																	if (entity instanceof Player _player && !_player.level().isClientSide())
																		_player.displayClientMessage(Component.literal("It's me."), true);
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
