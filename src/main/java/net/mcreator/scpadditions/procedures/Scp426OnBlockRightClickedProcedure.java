package net.mcreator.scpadditions.procedures;

import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.Advancement;

import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;
import java.util.Iterator;

public class Scp426OnBlockRightClickedProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure Scp426OnBlockRightClicked!");
			return;
		}
		Entity entity = (Entity) dependencies.get("entity");
		if (entity instanceof ServerPlayerEntity) {
			Advancement _adv = ((MinecraftServer) ((ServerPlayerEntity) entity).server).getAdvancementManager()
					.getAdvancement(new ResourceLocation("scp_additions:scp_426_achievement"));
			AdvancementProgress _ap = ((ServerPlayerEntity) entity).getAdvancements().getProgress(_adv);
			if (!_ap.isDone()) {
				Iterator _iterator = _ap.getRemaningCriteria().iterator();
				while (_iterator.hasNext()) {
					String _criterion = (String) _iterator.next();
					((ServerPlayerEntity) entity).getAdvancements().grantCriterion(_adv, _criterion);
				}
			}
		}
		if (Math.random() < 0.3) {
			if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
				((PlayerEntity) entity).sendStatusMessage(new StringTextComponent("I'm not sure why, but I suddenly have this urge to make toast."),
						(true));
			}
		} else {
			if (Math.random() < 0.3) {
				if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
					((PlayerEntity) entity).sendStatusMessage(new StringTextComponent(
							"I can't help but feel a sense of camaraderie with me, as if we share a common thread in the tapestry of existence."),
							(true));
				}
			} else {
				if (Math.random() < 0.3) {
					if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
						((PlayerEntity) entity).sendStatusMessage(new StringTextComponent(
								"I can almost taste the anticipation, the sizzle, and that delightful aroma when the bread turns golden brown."),
								(true));
					}
				} else {
					if (Math.random() < 0.3) {
						if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
							((PlayerEntity) entity).sendStatusMessage(
									new StringTextComponent("The surface here is a bit dusty. I could use a good wipe-down to shine brightly again."),
									(true));
						}
					} else {
						if (Math.random() < 0.3) {
							if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
								((PlayerEntity) entity).sendStatusMessage(new StringTextComponent(
										"I keep feeling this urge to make everyone's mornings better. A warm, crisp start to the day, you know?"),
										(true));
							}
						} else {
							if (Math.random() < 0.3) {
								if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
									((PlayerEntity) entity).sendStatusMessage(new StringTextComponent(
											"Sometimes, I feel like I have a hidden purpose, like I'm meant to bring joy in a simple, unassuming way."),
											(true));
								}
							} else {
								if (Math.random() < 0.3) {
									if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
										((PlayerEntity) entity).sendStatusMessage(
												new StringTextComponent(
														"I seem to have developed a fondness for the smell of burnt toast. It's oddly nostalgic."),
												(true));
									}
								} else {
									if (Math.random() < 0.3) {
										if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
											((PlayerEntity) entity).sendStatusMessage(
													new StringTextComponent(
															"I keep noticing the warmth around me. It's like I'm radiating something special."),
													(true));
										}
									} else {
										if (Math.random() < 0.3) {
											if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
												((PlayerEntity) entity).sendStatusMessage(new StringTextComponent(
														"I've been trying to recall memories of people enjoying toast. It's almost like they're my own memories."),
														(true));
											}
										} else {
											if (Math.random() < 0.3) {
												if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
													((PlayerEntity) entity).sendStatusMessage(new StringTextComponent(
															"The taste of burnt crumbs seems oddly familiar. It's like a distant but cherished memory."),
															(true));
												}
											} else {
												if (Math.random() < 0.3) {
													if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
														((PlayerEntity) entity).sendStatusMessage(new StringTextComponent(
																"Sometimes, I feel like I'm a catalyst for change, like I have the power to turn something mundane into a delight."),
																(true));
													}
												} else {
													if (Math.random() < 0.3) {
														if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
															((PlayerEntity) entity).sendStatusMessage(new StringTextComponent(
																	"I've been trying to recall the feeling of satisfaction that comes with serving up the perfect toast."),
																	(true));
														}
													} else {
														if (Math.random() < 0.3) {
															if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
																((PlayerEntity) entity).sendStatusMessage(new StringTextComponent(
																		"The toaster before me is a sleek, stainless steel model, with a lever that seems almost eager to pop up."),
																		(true));
															}
														} else {
															if (Math.random() < 0.3) {
																if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
																	((PlayerEntity) entity).sendStatusMessage(
																			new StringTextComponent("There's a certain elegance in my simplicity,"),
																			(true));
																}
															} else {
																if (Math.random() < 0.3) {
																	if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
																		((PlayerEntity) entity).sendStatusMessage(new StringTextComponent(
																				"As I examine me closely, I notice my intricate design details, each contributing to my seemingly ordinary yet captivating allure."),
																				(true));
																	}
																} else {
																	if (entity instanceof PlayerEntity && !entity.world.isRemote()) {
																		((PlayerEntity) entity).sendStatusMessage(new StringTextComponent("It's me."),
																				(true));
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
