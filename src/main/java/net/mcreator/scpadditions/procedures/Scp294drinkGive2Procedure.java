package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.world.Explosion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.container.Container;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.client.gui.widget.TextFieldWidget;

import net.mcreator.scpadditions.item.NeutroniumItem;
import net.mcreator.scpadditions.item.GrogItem;
import net.mcreator.scpadditions.item.GoldCItem;
import net.mcreator.scpadditions.item.GlassItem;
import net.mcreator.scpadditions.item.GinItem;
import net.mcreator.scpadditions.item.FecesItem;
import net.mcreator.scpadditions.item.FecesAndBloodItem;
import net.mcreator.scpadditions.item.FearItem;
import net.mcreator.scpadditions.item.EstusItem;
import net.mcreator.scpadditions.item.EspressoItem;
import net.mcreator.scpadditions.item.EggsItem;
import net.mcreator.scpadditions.item.DeathItem;
import net.mcreator.scpadditions.item.CurryItem;
import net.mcreator.scpadditions.item.CourageItem;
import net.mcreator.scpadditions.item.CosmopolitanItem;
import net.mcreator.scpadditions.item.ColdItem;
import net.mcreator.scpadditions.item.ColaItem;
import net.mcreator.scpadditions.item.CoconutItem;
import net.mcreator.scpadditions.item.ChampionItem;
import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.stream.Collectors;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Comparator;

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
									} else {
										if ((new Object() {
											public String getText() {
												TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
												if (_tf != null) {
													return _tf.getText();
												}
												return "";
											}
										}.getText()).equals("element 0") || (new Object() {
											public String getText() {
												TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
												if (_tf != null) {
													return _tf.getText();
												}
												return "";
											}
										}.getText()).equals("Element 0") || (new Object() {
											public String getText() {
												TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
												if (_tf != null) {
													return _tf.getText();
												}
												return "";
											}
										}.getText()).equals("element zero") || (new Object() {
											public String getText() {
												TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
												if (_tf != null) {
													return _tf.getText();
												}
												return "";
											}
										}.getText()).equals("Element Zero") || (new Object() {
											public String getText() {
												TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
												if (_tf != null) {
													return _tf.getText();
												}
												return "";
											}
										}.getText()).equals("neutronium") || (new Object() {
											public String getText() {
												TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
												if (_tf != null) {
													return _tf.getText();
												}
												return "";
											}
										}.getText()).equals("Neutronium") || (new Object() {
											public String getText() {
												TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
												if (_tf != null) {
													return _tf.getText();
												}
												return "";
											}
										}.getText()).equals("neutrium") || (new Object() {
											public String getText() {
												TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
												if (_tf != null) {
													return _tf.getText();
												}
												return "";
											}
										}.getText()).equals("Neutrium") || (new Object() {
											public String getText() {
												TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
												if (_tf != null) {
													return _tf.getText();
												}
												return "";
											}
										}.getText()).equals("tetraneutron") || (new Object() {
											public String getText() {
												TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
												if (_tf != null) {
													return _tf.getText();
												}
												return "";
											}
										}.getText()).equals("Tetraneutron")) {
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
														ItemStack _setstack = new ItemStack(NeutroniumItem.block);
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
											}.getText()).equals("es") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("ES") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("euroshopper") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("Euroshopper") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("euroshopper energy drink") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("Euroshopper Energy Drink") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("energy drink") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("Energy Drink") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("red bull") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("Red Bull") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("monster") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("Monster") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("monster energy") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("Monster Energy") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("monster energy drink") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("Monster Energy Drink")) {
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
															ItemStack _setstack = new ItemStack(NeutroniumItem.block);
															_setstack.setCount((int) 1);
															ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
														}
														MinecraftForge.EVENT_BUS.unregister(this);
													}
												}.start(world, (int) 40);
												ScpAdditionsModVariables.WorldVariables.get(
														world).Scp294stock = (ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1);
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
												}.getText()).equals("espresso") || (new Object() {
													public String getText() {
														TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
														if (_tf != null) {
															return _tf.getText();
														}
														return "";
													}
												}.getText()).equals("Espresso")) {
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
																ItemStack _setstack = new ItemStack(EspressoItem.block);
																_setstack.setCount((int) 1);
																ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
															}
															MinecraftForge.EVENT_BUS.unregister(this);
														}
													}.start(world, (int) 40);
													ScpAdditionsModVariables.WorldVariables.get(
															world).Scp294stock = (ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1);
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
													}.getText()).equals("estus") || (new Object() {
														public String getText() {
															TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
															if (_tf != null) {
																return _tf.getText();
															}
															return "";
														}
													}.getText()).equals("Estus") || (new Object() {
														public String getText() {
															TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
															if (_tf != null) {
																return _tf.getText();
															}
															return "";
														}
													}.getText()).equals("estus flesk") || (new Object() {
														public String getText() {
															TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
															if (_tf != null) {
																return _tf.getText();
															}
															return "";
														}
													}.getText()).equals("Estus flesk")) {
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
																	ItemStack _setstack = new ItemStack(EstusItem.block);
																	_setstack.setCount((int) 1);
																	ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
																}
																MinecraftForge.EVENT_BUS.unregister(this);
															}
														}.start(world, (int) 40);
														ScpAdditionsModVariables.WorldVariables.get(
																world).Scp294stock = (ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock
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
														}.getText()).equals("Exotic Matter") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("exotic matter") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("Zero Point Energy") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("zero point energy") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("Negative Matter") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("negative matter") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("Gravitons") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("gravitons") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("Higgs Boson") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("higgs boson") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("God Particles") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("god particles") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("Black Hole") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("black hole") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("Black Holes") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("black holes")) {
															if (world instanceof World && !world.isRemote()) {
																((World) world).playSound(null, new BlockPos(x, y, z),
																		(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																				.getValue(new ResourceLocation("scp_additions:scp294emptycup")),
																		SoundCategory.NEUTRAL, (float) 1, (float) 1);
															} else {
																((World) world).playSound(x, y, z,
																		(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																				.getValue(new ResourceLocation("scp_additions:scp294emptycup")),
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
																	if (world instanceof World && !((World) world).isRemote) {
																		((World) world).createExplosion(null, (int) x, (int) y, (int) z, (float) 20,
																				Explosion.Mode.NONE);
																	}
																	{
																		List<Entity> _entfound = world
																				.getEntitiesWithinAABB(Entity.class,
																						new AxisAlignedBB(x - (20 / 2d), y - (20 / 2d), z - (20 / 2d),
																								x + (20 / 2d), y + (20 / 2d), z + (20 / 2d)),
																						null)
																				.stream().sorted(new Object() {
																					Comparator<Entity> compareDistOf(double _x, double _y,
																							double _z) {
																						return Comparator.comparing(
																								(Function<Entity, Double>) (_entcnd -> _entcnd
																										.getDistanceSq(_x, _y, _z)));
																					}
																				}.compareDistOf(x, y, z)).collect(Collectors.toList());
																		for (Entity entityiterator : _entfound) {
																			if (entity instanceof LivingEntity) {
																				((LivingEntity) entity).attackEntityFrom(
																						new DamageSource("gravitons").setDamageBypassesArmor(),
																						(float) 100);
																			}
																		}
																	}
																	MinecraftForge.EVENT_BUS.unregister(this);
																}
															}.start(world, (int) 10);
															ScpAdditionsModVariables.WorldVariables
																	.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																			.get(world).Scp294stock + 1);
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
															}.getText()).equals("eternal champion") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("Eternal Champion") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("blood of eternal champion") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("Blood of Eternal Champion") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("talin warhaft") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("Talin Warhaft") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("champion") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("Champion")) {
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
																			ItemStack _setstack = new ItemStack(ChampionItem.block);
																			_setstack.setCount((int) 1);
																			ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
																		}
																		MinecraftForge.EVENT_BUS.unregister(this);
																	}
																}.start(world, (int) 40);
																ScpAdditionsModVariables.WorldVariables
																		.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																				.get(world).Scp294stock + 1);
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
																}.getText()).equals("fear") || (new Object() {
																	public String getText() {
																		TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																		if (_tf != null) {
																			return _tf.getText();
																		}
																		return "";
																	}
																}.getText()).equals("Fear") || (new Object() {
																	public String getText() {
																		TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																		if (_tf != null) {
																			return _tf.getText();
																		}
																		return "";
																	}
																}.getText()).equals("scare") || (new Object() {
																	public String getText() {
																		TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																		if (_tf != null) {
																			return _tf.getText();
																		}
																		return "";
																	}
																}.getText()).equals("Scare") || (new Object() {
																	public String getText() {
																		TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																		if (_tf != null) {
																			return _tf.getText();
																		}
																		return "";
																	}
																}.getText()).equals("horror") || (new Object() {
																	public String getText() {
																		TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																		if (_tf != null) {
																			return _tf.getText();
																		}
																		return "";
																	}
																}.getText()).equals("Horror") || (new Object() {
																	public String getText() {
																		TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																		if (_tf != null) {
																			return _tf.getText();
																		}
																		return "";
																	}
																}.getText()).equals("terror") || (new Object() {
																	public String getText() {
																		TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																		if (_tf != null) {
																			return _tf.getText();
																		}
																		return "";
																	}
																}.getText()).equals("Terror")) {
																	if (world instanceof World && !world.isRemote()) {
																		((World) world).playSound(null, new BlockPos(x, y, z),
																				(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(
																						new ResourceLocation("scp_additions:scp294pouring")),
																				SoundCategory.NEUTRAL, (float) 1, (float) 1);
																	} else {
																		((World) world).playSound(x, y, z,
																				(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(
																						new ResourceLocation("scp_additions:scp294pouring")),
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
																				ItemStack _setstack = new ItemStack(FearItem.block);
																				_setstack.setCount((int) 1);
																				ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity),
																						_setstack);
																			}
																			MinecraftForge.EVENT_BUS.unregister(this);
																		}
																	}.start(world, (int) 40);
																	ScpAdditionsModVariables.WorldVariables
																			.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																					.get(world).Scp294stock + 1);
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
																	}.getText()).equals("feces") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Feces") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("fecal matter") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Fecal matter") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("shit") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Shit") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("crap") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Crap") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("poo") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Poo") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("poop") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Poop") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("dung") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Dung") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("scat") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Scat") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("turd") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Turd") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("bullshit") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Bullshit") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("horseshit") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Horseshit") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("diarrhea") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Diarrhea")) {
																		if (world instanceof World && !world.isRemote()) {
																			((World) world).playSound(null, new BlockPos(x, y, z),
																					(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																							.getValue(new ResourceLocation(
																									"scp_additions:scp294pouring")),
																					SoundCategory.NEUTRAL, (float) 1, (float) 1);
																		} else {
																			((World) world).playSound(x, y, z,
																					(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																							.getValue(new ResourceLocation(
																									"scp_additions:scp294pouring")),
																					SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
																		}
																		{
																			Entity _ent = entity;
																			if (_ent instanceof ServerPlayerEntity) {
																				Container _current = ((ServerPlayerEntity) _ent).openContainer;
																				if (_current instanceof Supplier) {
																					Object invobj = ((Supplier) _current).get();
																					if (invobj instanceof Map) {
																						((Slot) ((Map) invobj).get((int) (0)))
																								.decrStackSize((int) (1));
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
																					ItemStack _setstack = new ItemStack(FecesItem.block);
																					_setstack.setCount((int) 1);
																					ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity),
																							_setstack);
																				}
																				MinecraftForge.EVENT_BUS.unregister(this);
																			}
																		}.start(world, (int) 40);
																		ScpAdditionsModVariables.WorldVariables
																				.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																						.get(world).Scp294stock + 1);
																		ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																	} else {
																		if ((new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("feces and blood") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("Feces and blood") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("blood and feces") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("Blood and feces") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("Feces and Blood") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("Blood and Feces")) {
																			if (world instanceof World && !world.isRemote()) {
																				((World) world).playSound(null, new BlockPos(x, y, z),
																						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																								.getValue(new ResourceLocation(
																										"scp_additions:scp294pouring")),
																						SoundCategory.NEUTRAL, (float) 1, (float) 1);
																			} else {
																				((World) world).playSound(x, y, z,
																						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																								.getValue(new ResourceLocation(
																										"scp_additions:scp294pouring")),
																						SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
																			}
																			{
																				Entity _ent = entity;
																				if (_ent instanceof ServerPlayerEntity) {
																					Container _current = ((ServerPlayerEntity) _ent).openContainer;
																					if (_current instanceof Supplier) {
																						Object invobj = ((Supplier) _current).get();
																						if (invobj instanceof Map) {
																							((Slot) ((Map) invobj).get((int) (0)))
																									.decrStackSize((int) (1));
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
																						ItemStack _setstack = new ItemStack(FecesAndBloodItem.block);
																						_setstack.setCount((int) 1);
																						ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity),
																								_setstack);
																					}
																					MinecraftForge.EVENT_BUS.unregister(this);
																				}
																			}.start(world, (int) 40);
																			ScpAdditionsModVariables.WorldVariables
																					.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																							.get(world).Scp294stock + 1);
																			ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																		} else {
																			if ((new Object() {
																				public String getText() {
																					TextFieldWidget _tf = (TextFieldWidget) guistate
																							.get("text:scp294input");
																					if (_tf != null) {
																						return _tf.getText();
																					}
																					return "";
																				}
																			}.getText()).equals("gin") || (new Object() {
																				public String getText() {
																					TextFieldWidget _tf = (TextFieldWidget) guistate
																							.get("text:scp294input");
																					if (_tf != null) {
																						return _tf.getText();
																					}
																					return "";
																				}
																			}.getText()).equals("Gin") || (new Object() {
																				public String getText() {
																					TextFieldWidget _tf = (TextFieldWidget) guistate
																							.get("text:scp294input");
																					if (_tf != null) {
																						return _tf.getText();
																					}
																					return "";
																				}
																			}.getText()).equals("gin and tonic") || (new Object() {
																				public String getText() {
																					TextFieldWidget _tf = (TextFieldWidget) guistate
																							.get("text:scp294input");
																					if (_tf != null) {
																						return _tf.getText();
																					}
																					return "";
																				}
																			}.getText()).equals("Gin and Tonic") || (new Object() {
																				public String getText() {
																					TextFieldWidget _tf = (TextFieldWidget) guistate
																							.get("text:scp294input");
																					if (_tf != null) {
																						return _tf.getText();
																					}
																					return "";
																				}
																			}.getText()).equals("tonic and gin") || (new Object() {
																				public String getText() {
																					TextFieldWidget _tf = (TextFieldWidget) guistate
																							.get("text:scp294input");
																					if (_tf != null) {
																						return _tf.getText();
																					}
																					return "";
																				}
																			}.getText()).equals("Tonic and Gin") || (new Object() {
																				public String getText() {
																					TextFieldWidget _tf = (TextFieldWidget) guistate
																							.get("text:scp294input");
																					if (_tf != null) {
																						return _tf.getText();
																					}
																					return "";
																				}
																			}.getText()).equals("gin & tonic") || (new Object() {
																				public String getText() {
																					TextFieldWidget _tf = (TextFieldWidget) guistate
																							.get("text:scp294input");
																					if (_tf != null) {
																						return _tf.getText();
																					}
																					return "";
																				}
																			}.getText()).equals("Gin & Tonic") || (new Object() {
																				public String getText() {
																					TextFieldWidget _tf = (TextFieldWidget) guistate
																							.get("text:scp294input");
																					if (_tf != null) {
																						return _tf.getText();
																					}
																					return "";
																				}
																			}.getText()).equals("tonic & gin") || (new Object() {
																				public String getText() {
																					TextFieldWidget _tf = (TextFieldWidget) guistate
																							.get("text:scp294input");
																					if (_tf != null) {
																						return _tf.getText();
																					}
																					return "";
																				}
																			}.getText()).equals("Tonic & Gin")) {
																				if (world instanceof World && !world.isRemote()) {
																					((World) world).playSound(null, new BlockPos(x, y, z),
																							(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																									.getValue(new ResourceLocation(
																											"scp_additions:scp294pouring")),
																							SoundCategory.NEUTRAL, (float) 1, (float) 1);
																				} else {
																					((World) world).playSound(x, y, z,
																							(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																									.getValue(new ResourceLocation(
																											"scp_additions:scp294pouring")),
																							SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
																				}
																				{
																					Entity _ent = entity;
																					if (_ent instanceof ServerPlayerEntity) {
																						Container _current = ((ServerPlayerEntity) _ent).openContainer;
																						if (_current instanceof Supplier) {
																							Object invobj = ((Supplier) _current).get();
																							if (invobj instanceof Map) {
																								((Slot) ((Map) invobj).get((int) (0)))
																										.decrStackSize((int) (1));
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
																							ItemStack _setstack = new ItemStack(GinItem.block);
																							_setstack.setCount((int) 1);
																							ItemHandlerHelper.giveItemToPlayer(
																									((PlayerEntity) entity), _setstack);
																						}
																						MinecraftForge.EVENT_BUS.unregister(this);
																					}
																				}.start(world, (int) 40);
																				ScpAdditionsModVariables.WorldVariables.get(
																						world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																								.get(world).Scp294stock + 1);
																				ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
																			} else {
																				if ((new Object() {
																					public String getText() {
																						TextFieldWidget _tf = (TextFieldWidget) guistate
																								.get("text:scp294input");
																						if (_tf != null) {
																							return _tf.getText();
																						}
																						return "";
																					}
																				}.getText()).equals("glass") || (new Object() {
																					public String getText() {
																						TextFieldWidget _tf = (TextFieldWidget) guistate
																								.get("text:scp294input");
																						if (_tf != null) {
																							return _tf.getText();
																						}
																						return "";
																					}
																				}.getText()).equals("Glass")) {
																					if (world instanceof World && !world.isRemote()) {
																						((World) world).playSound(null, new BlockPos(x, y, z),
																								(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																										.getValue(new ResourceLocation(
																												"scp_additions:scp294pouring")),
																								SoundCategory.NEUTRAL, (float) 1, (float) 1);
																					} else {
																						((World) world).playSound(x, y, z,
																								(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																										.getValue(new ResourceLocation(
																												"scp_additions:scp294pouring")),
																								SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
																					}
																					{
																						Entity _ent = entity;
																						if (_ent instanceof ServerPlayerEntity) {
																							Container _current = ((ServerPlayerEntity) _ent).openContainer;
																							if (_current instanceof Supplier) {
																								Object invobj = ((Supplier) _current).get();
																								if (invobj instanceof Map) {
																									((Slot) ((Map) invobj).get((int) (0)))
																											.decrStackSize((int) (1));
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
																								ItemStack _setstack = new ItemStack(GlassItem.block);
																								_setstack.setCount((int) 1);
																								ItemHandlerHelper.giveItemToPlayer(
																										((PlayerEntity) entity), _setstack);
																							}
																							MinecraftForge.EVENT_BUS.unregister(this);
																						}
																					}.start(world, (int) 40);
																					ScpAdditionsModVariables.WorldVariables.get(
																							world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																									.get(world).Scp294stock + 1);
																					ScpAdditionsModVariables.WorldVariables.get(world)
																							.syncData(world);
																				} else {
																					if ((new Object() {
																						public String getText() {
																							TextFieldWidget _tf = (TextFieldWidget) guistate
																									.get("text:scp294input");
																							if (_tf != null) {
																								return _tf.getText();
																							}
																							return "";
																						}
																					}.getText()).equals("gold") || (new Object() {
																						public String getText() {
																							TextFieldWidget _tf = (TextFieldWidget) guistate
																									.get("text:scp294input");
																							if (_tf != null) {
																								return _tf.getText();
																							}
																							return "";
																						}
																					}.getText()).equals("Gold")) {
																						if (world instanceof World && !world.isRemote()) {
																							((World) world).playSound(null, new BlockPos(x, y, z),
																									(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																											.getValue(new ResourceLocation(
																													"scp_additions:scp294pouring")),
																									SoundCategory.NEUTRAL, (float) 1, (float) 1);
																						} else {
																							((World) world).playSound(x, y, z,
																									(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																											.getValue(new ResourceLocation(
																													"scp_additions:scp294pouring")),
																									SoundCategory.NEUTRAL, (float) 1, (float) 1,
																									false);
																						}
																						{
																							Entity _ent = entity;
																							if (_ent instanceof ServerPlayerEntity) {
																								Container _current = ((ServerPlayerEntity) _ent).openContainer;
																								if (_current instanceof Supplier) {
																									Object invobj = ((Supplier) _current).get();
																									if (invobj instanceof Map) {
																										((Slot) ((Map) invobj).get((int) (0)))
																												.decrStackSize((int) (1));
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
																									ItemStack _setstack = new ItemStack(
																											GoldCItem.block);
																									_setstack.setCount((int) 1);
																									ItemHandlerHelper.giveItemToPlayer(
																											((PlayerEntity) entity), _setstack);
																								}
																								MinecraftForge.EVENT_BUS.unregister(this);
																							}
																						}.start(world, (int) 40);
																						ScpAdditionsModVariables.WorldVariables.get(
																								world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																										.get(world).Scp294stock + 1);
																						ScpAdditionsModVariables.WorldVariables.get(world)
																								.syncData(world);
																					} else {
																						if ((new Object() {
																							public String getText() {
																								TextFieldWidget _tf = (TextFieldWidget) guistate
																										.get("text:scp294input");
																								if (_tf != null) {
																									return _tf.getText();
																								}
																								return "";
																							}
																						}.getText()).equals("grog") || (new Object() {
																							public String getText() {
																								TextFieldWidget _tf = (TextFieldWidget) guistate
																										.get("text:scp294input");
																								if (_tf != null) {
																									return _tf.getText();
																								}
																								return "";
																							}
																						}.getText()).equals("Grog")) {
																							if (world instanceof World && !world.isRemote()) {
																								((World) world).playSound(null, new BlockPos(x, y, z),
																										(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																												.getValue(new ResourceLocation(
																														"scp_additions:scp294pouring")),
																										SoundCategory.NEUTRAL, (float) 1, (float) 1);
																							} else {
																								((World) world).playSound(x, y, z,
																										(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																												.getValue(new ResourceLocation(
																														"scp_additions:scp294pouring")),
																										SoundCategory.NEUTRAL, (float) 1, (float) 1,
																										false);
																							}
																							{
																								Entity _ent = entity;
																								if (_ent instanceof ServerPlayerEntity) {
																									Container _current = ((ServerPlayerEntity) _ent).openContainer;
																									if (_current instanceof Supplier) {
																										Object invobj = ((Supplier) _current).get();
																										if (invobj instanceof Map) {
																											((Slot) ((Map) invobj).get((int) (0)))
																													.decrStackSize((int) (1));
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
																										ItemStack _setstack = new ItemStack(
																												GrogItem.block);
																										_setstack.setCount((int) 1);
																										ItemHandlerHelper.giveItemToPlayer(
																												((PlayerEntity) entity), _setstack);
																									}
																									MinecraftForge.EVENT_BUS.unregister(this);
																								}
																							}.start(world, (int) 40);
																							ScpAdditionsModVariables.WorldVariables.get(
																									world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																											.get(world).Scp294stock + 1);
																							ScpAdditionsModVariables.WorldVariables.get(world)
																									.syncData(world);
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
