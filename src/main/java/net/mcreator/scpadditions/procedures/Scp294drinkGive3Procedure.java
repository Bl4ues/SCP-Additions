package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.container.Container;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.client.gui.widget.TextFieldWidget;

import net.mcreator.scpadditions.item.YogurtItem;
import net.mcreator.scpadditions.item.IronCItem;
import net.mcreator.scpadditions.item.IpecacItem;
import net.mcreator.scpadditions.item.InsulinItem;
import net.mcreator.scpadditions.item.InkItem;
import net.mcreator.scpadditions.item.FrozenYogurtItem;
import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class Scp294drinkGive3Procedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure Scp294drinkGive3!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency x for procedure Scp294drinkGive3!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency y for procedure Scp294drinkGive3!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency z for procedure Scp294drinkGive3!");
			return;
		}
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure Scp294drinkGive3!");
			return;
		}
		if (dependencies.get("guistate") == null) {
			if (!dependencies.containsKey("guistate"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency guistate for procedure Scp294drinkGive3!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		double x = dependencies.get("x") instanceof Integer ? (int) dependencies.get("x") : (double) dependencies.get("x");
		double y = dependencies.get("y") instanceof Integer ? (int) dependencies.get("y") : (double) dependencies.get("y");
		double z = dependencies.get("z") instanceof Integer ? (int) dependencies.get("z") : (double) dependencies.get("z");
		Entity entity = (Entity) dependencies.get("entity");
		HashMap guistate = (HashMap) dependencies.get("guistate");
		if ((new Object() {
			public String getText() {
				TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
				if (_tf != null) {
					return _tf.getText();
				}
				return "";
			}
		}.getText()).equals("frozen yogurt") || (new Object() {
			public String getText() {
				TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
				if (_tf != null) {
					return _tf.getText();
				}
				return "";
			}
		}.getText()).equals("Frozen Yogurt") || (new Object() {
			public String getText() {
				TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
				if (_tf != null) {
					return _tf.getText();
				}
				return "";
			}
		}.getText()).equals("Frozen yogurt")) {
			if (world instanceof World && !world.isRemote()) {
				((World) world).playSound(null, new BlockPos(x, y, z),
						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
						SoundCategory.NEUTRAL, (float) 1, (float) 1);
			} else {
				((World) world).playSound(x, y, z,
						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294pouring")),
						SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
			}
			{
				Entity _ent = entity;
				if (_ent instanceof ServerPlayerEntity) {
					Container _current = ((ServerPlayerEntity) _ent).openContainer;
					if (_current instanceof Supplier) {
						Object invobj = ((Supplier) _current).get();
						if (invobj instanceof Map) {
							((Slot) ((Map) invobj).get((int) (0))).decrStackSize((int) (1));
							_current.detectAndSendChanges();
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
					if (entity instanceof PlayerEntity) {
						ItemStack _setstack = new ItemStack(FrozenYogurtItem.block);
						_setstack.setCount((int) 1);
						ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
					}
					MinecraftForge.EVENT_BUS.unregister(this);
				}
			}.start(world, (int) 40);
			ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1);
			ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
		} else {
			if ((new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("yogurt") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Yogurt")) {
				if (world instanceof World && !world.isRemote()) {
					((World) world)
							.playSound(null, new BlockPos(x, y, z),
									(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
											.getValue(new ResourceLocation("scp_additions:scp294pouring")),
									SoundCategory.NEUTRAL, (float) 1, (float) 1);
				} else {
					((World) world).playSound(x, y, z,
							(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
									.getValue(new ResourceLocation("scp_additions:scp294pouring")),
							SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
				}
				{
					Entity _ent = entity;
					if (_ent instanceof ServerPlayerEntity) {
						Container _current = ((ServerPlayerEntity) _ent).openContainer;
						if (_current instanceof Supplier) {
							Object invobj = ((Supplier) _current).get();
							if (invobj instanceof Map) {
								((Slot) ((Map) invobj).get((int) (0))).decrStackSize((int) (1));
								_current.detectAndSendChanges();
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
						if (entity instanceof PlayerEntity) {
							ItemStack _setstack = new ItemStack(YogurtItem.block);
							_setstack.setCount((int) 1);
							ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
						}
						MinecraftForge.EVENT_BUS.unregister(this);
					}
				}.start(world, (int) 40);
				ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1);
				ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
			} else {
				if ((new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("ink") || (new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("Ink")) {
					if (world instanceof World && !world.isRemote()) {
						((World) world)
								.playSound(null, new BlockPos(x, y, z),
										(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
												.getValue(new ResourceLocation("scp_additions:scp294pouring")),
										SoundCategory.NEUTRAL, (float) 1, (float) 1);
					} else {
						((World) world).playSound(x, y, z,
								(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
										.getValue(new ResourceLocation("scp_additions:scp294pouring")),
								SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
					}
					{
						Entity _ent = entity;
						if (_ent instanceof ServerPlayerEntity) {
							Container _current = ((ServerPlayerEntity) _ent).openContainer;
							if (_current instanceof Supplier) {
								Object invobj = ((Supplier) _current).get();
								if (invobj instanceof Map) {
									((Slot) ((Map) invobj).get((int) (0))).decrStackSize((int) (1));
									_current.detectAndSendChanges();
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
							if (entity instanceof PlayerEntity) {
								ItemStack _setstack = new ItemStack(InkItem.block);
								_setstack.setCount((int) 1);
								ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
							}
							MinecraftForge.EVENT_BUS.unregister(this);
						}
					}.start(world, (int) 40);
					ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock
							+ 1);
					ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
				} else {
					if ((new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("insulin") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("Insulin") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("novorapid") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("NovoRapid") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("novo rapid") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("Novo Rapid") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("glargine") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("Glargine")) {
						if (world instanceof World && !world.isRemote()) {
							((World) world).playSound(null, new BlockPos(x, y, z),
									(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
											.getValue(new ResourceLocation("scp_additions:scp294pouring")),
									SoundCategory.NEUTRAL, (float) 1, (float) 1);
						} else {
							((World) world).playSound(x, y, z,
									(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
											.getValue(new ResourceLocation("scp_additions:scp294pouring")),
									SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
						}
						{
							Entity _ent = entity;
							if (_ent instanceof ServerPlayerEntity) {
								Container _current = ((ServerPlayerEntity) _ent).openContainer;
								if (_current instanceof Supplier) {
									Object invobj = ((Supplier) _current).get();
									if (invobj instanceof Map) {
										((Slot) ((Map) invobj).get((int) (0))).decrStackSize((int) (1));
										_current.detectAndSendChanges();
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
								if (entity instanceof PlayerEntity) {
									ItemStack _setstack = new ItemStack(InsulinItem.block);
									_setstack.setCount((int) 1);
									ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
								}
								MinecraftForge.EVENT_BUS.unregister(this);
							}
						}.start(world, (int) 40);
						ScpAdditionsModVariables.WorldVariables
								.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1);
						ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
					} else {
						if ((new Object() {
							public String getText() {
								TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
								if (_tf != null) {
									return _tf.getText();
								}
								return "";
							}
						}.getText()).equals("ipecac") || (new Object() {
							public String getText() {
								TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
								if (_tf != null) {
									return _tf.getText();
								}
								return "";
							}
						}.getText()).equals("Ipecac")) {
							if (world instanceof World && !world.isRemote()) {
								((World) world).playSound(null, new BlockPos(x, y, z),
										(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
												.getValue(new ResourceLocation("scp_additions:scp294pouring")),
										SoundCategory.NEUTRAL, (float) 1, (float) 1);
							} else {
								((World) world).playSound(x, y, z,
										(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
												.getValue(new ResourceLocation("scp_additions:scp294pouring")),
										SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
							}
							{
								Entity _ent = entity;
								if (_ent instanceof ServerPlayerEntity) {
									Container _current = ((ServerPlayerEntity) _ent).openContainer;
									if (_current instanceof Supplier) {
										Object invobj = ((Supplier) _current).get();
										if (invobj instanceof Map) {
											((Slot) ((Map) invobj).get((int) (0))).decrStackSize((int) (1));
											_current.detectAndSendChanges();
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
									if (entity instanceof PlayerEntity) {
										ItemStack _setstack = new ItemStack(IpecacItem.block);
										_setstack.setCount((int) 1);
										ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
									}
									MinecraftForge.EVENT_BUS.unregister(this);
								}
							}.start(world, (int) 40);
							ScpAdditionsModVariables.WorldVariables
									.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1);
							ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
						} else {
							if ((new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("iron") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("Iron") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("steel") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("Steel") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("metal") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("Metal") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("razor blades") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("Razor blades") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("razorblades") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("Razorblades")) {
								if (world instanceof World && !world.isRemote()) {
									((World) world).playSound(null, new BlockPos(x, y, z),
											(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
													.getValue(new ResourceLocation("scp_additions:scp294pouring")),
											SoundCategory.NEUTRAL, (float) 1, (float) 1);
								} else {
									((World) world).playSound(x, y, z,
											(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
													.getValue(new ResourceLocation("scp_additions:scp294pouring")),
											SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
								}
								{
									Entity _ent = entity;
									if (_ent instanceof ServerPlayerEntity) {
										Container _current = ((ServerPlayerEntity) _ent).openContainer;
										if (_current instanceof Supplier) {
											Object invobj = ((Supplier) _current).get();
											if (invobj instanceof Map) {
												((Slot) ((Map) invobj).get((int) (0))).decrStackSize((int) (1));
												_current.detectAndSendChanges();
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
										if (entity instanceof PlayerEntity) {
											ItemStack _setstack = new ItemStack(IronCItem.block);
											_setstack.setCount((int) 1);
											ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
										}
										MinecraftForge.EVENT_BUS.unregister(this);
									}
								}.start(world, (int) 40);
								ScpAdditionsModVariables.WorldVariables
										.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1);
								ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
							}
						}
					}
				}
			}
		}
	}
}
