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

import net.mcreator.scpadditions.item.EggsItem;
import net.mcreator.scpadditions.item.DeathItem;
import net.mcreator.scpadditions.item.CurryItem;
import net.mcreator.scpadditions.item.CourageItem;
import net.mcreator.scpadditions.item.CosmopolitanItem;
import net.mcreator.scpadditions.item.ColdItem;
import net.mcreator.scpadditions.item.ColaItem;
import net.mcreator.scpadditions.item.CoconutItem;
import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class Scp294drinkGive2Procedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure Scp294drinkGive2!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency x for procedure Scp294drinkGive2!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency y for procedure Scp294drinkGive2!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency z for procedure Scp294drinkGive2!");
			return;
		}
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure Scp294drinkGive2!");
			return;
		}
		if (dependencies.get("guistate") == null) {
			if (!dependencies.containsKey("guistate"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency guistate for procedure Scp294drinkGive2!");
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
		}.getText()).equals("coconut") || (new Object() {
			public String getText() {
				TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
				if (_tf != null) {
					return _tf.getText();
				}
				return "";
			}
		}.getText()).equals("Coconut") || (new Object() {
			public String getText() {
				TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
				if (_tf != null) {
					return _tf.getText();
				}
				return "";
			}
		}.getText()).equals("coconut milk") || (new Object() {
			public String getText() {
				TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
				if (_tf != null) {
					return _tf.getText();
				}
				return "";
			}
		}.getText()).equals("Coconut milk") || (new Object() {
			public String getText() {
				TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
				if (_tf != null) {
					return _tf.getText();
				}
				return "";
			}
		}.getText()).equals("coconut water") || (new Object() {
			public String getText() {
				TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
				if (_tf != null) {
					return _tf.getText();
				}
				return "";
			}
		}.getText()).equals("Coconut water") || (new Object() {
			public String getText() {
				TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
				if (_tf != null) {
					return _tf.getText();
				}
				return "";
			}
		}.getText()).equals("coconut juice") || (new Object() {
			public String getText() {
				TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
				if (_tf != null) {
					return _tf.getText();
				}
				return "";
			}
		}.getText()).equals("Coconut juice")) {
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
						ItemStack _setstack = new ItemStack(CoconutItem.block);
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
			}.getText()).equals("cola") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Cola") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("coke") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Coke") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("coca-cola") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Coca-cola") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("coca cola") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Coca cola") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("pepsi") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Pepsi") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("soda") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Soda")) {
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
							ItemStack _setstack = new ItemStack(ColaItem.block);
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
				}.getText()).equals("cold") || (new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("Cold") || (new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("cool") || (new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("Cool") || (new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("freezing") || (new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("Freezing") || (new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("coldness") || (new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("Coldness")) {
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
								ItemStack _setstack = new ItemStack(ColdItem.block);
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
					}.getText()).equals("cosmopolitan") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("Cosmopolitan") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("cocktail") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("Cocktail") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("cosmopolitan cocktail") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("Cosmopolitan Cocktail")) {
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
									ItemStack _setstack = new ItemStack(CosmopolitanItem.block);
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
						}.getText()).equals("courage") || (new Object() {
							public String getText() {
								TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
								if (_tf != null) {
									return _tf.getText();
								}
								return "";
							}
						}.getText()).equals("Courage") || (new Object() {
							public String getText() {
								TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
								if (_tf != null) {
									return _tf.getText();
								}
								return "";
							}
						}.getText()).equals("bravery") || (new Object() {
							public String getText() {
								TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
								if (_tf != null) {
									return _tf.getText();
								}
								return "";
							}
						}.getText()).equals("Bravery")) {
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
										ItemStack _setstack = new ItemStack(CourageItem.block);
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
							}.getText()).equals("curry") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("Curry") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("masala") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("Masala") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("curry sauce") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("Curry sause") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("masala sause") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("Masala sause")) {
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
											ItemStack _setstack = new ItemStack(CurryItem.block);
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
								}.getText()).equals("death") || (new Object() {
									public String getText() {
										TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
										if (_tf != null) {
											return _tf.getText();
										}
										return "";
									}
								}.getText()).equals("Death") || (new Object() {
									public String getText() {
										TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
										if (_tf != null) {
											return _tf.getText();
										}
										return "";
									}
								}.getText()).equals("game over") || (new Object() {
									public String getText() {
										TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
										if (_tf != null) {
											return _tf.getText();
										}
										return "";
									}
								}.getText()).equals("Game over")) {
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
												ItemStack _setstack = new ItemStack(DeathItem.block);
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
									}.getText()).equals("egg") || (new Object() {
										public String getText() {
											TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
											if (_tf != null) {
												return _tf.getText();
											}
											return "";
										}
									}.getText()).equals("Egg") || (new Object() {
										public String getText() {
											TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
											if (_tf != null) {
												return _tf.getText();
											}
											return "";
										}
									}.getText()).equals("eggs") || (new Object() {
										public String getText() {
											TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
											if (_tf != null) {
												return _tf.getText();
											}
											return "";
										}
									}.getText()).equals("Eggs")) {
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
													ItemStack _setstack = new ItemStack(EggsItem.block);
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
	}
}
