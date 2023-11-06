package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.world.Explosion;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.DamageSource;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.state.Property;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.container.Container;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.block.BlockState;

import net.mcreator.scpadditions.item.VodkaItem;
import net.mcreator.scpadditions.item.SpiritItem;
import net.mcreator.scpadditions.item.QuantumItem;
import net.mcreator.scpadditions.item.PearCiderItem;
import net.mcreator.scpadditions.item.LagerItem;
import net.mcreator.scpadditions.item.GrimaceShakeItem;
import net.mcreator.scpadditions.item.EthanolItem;
import net.mcreator.scpadditions.item.EmptyCupItem;
import net.mcreator.scpadditions.item.Drink1Item;
import net.mcreator.scpadditions.item.CorrosiveBlackItem;
import net.mcreator.scpadditions.item.CoinItem;
import net.mcreator.scpadditions.item.CoffeeItem;
import net.mcreator.scpadditions.item.CiderItem;
import net.mcreator.scpadditions.item.ChocolateItem;
import net.mcreator.scpadditions.item.ChimItem;
import net.mcreator.scpadditions.item.ChampagneItem;
import net.mcreator.scpadditions.item.CassisFantaItem;
import net.mcreator.scpadditions.item.CarrotItem;
import net.mcreator.scpadditions.item.CarbonItem;
import net.mcreator.scpadditions.item.CactusItem;
import net.mcreator.scpadditions.item.BloodOfChristItem;
import net.mcreator.scpadditions.item.BloodItem;
import net.mcreator.scpadditions.item.BleachItem;
import net.mcreator.scpadditions.item.BeerItem;
import net.mcreator.scpadditions.item.AquaRegiaItem;
import net.mcreator.scpadditions.item.AppleCiderItem;
import net.mcreator.scpadditions.item.AntiEnergyItem;
import net.mcreator.scpadditions.item.AmnesiaItem;
import net.mcreator.scpadditions.item.AloeItem;
import net.mcreator.scpadditions.block.Scp294OutOfRangeBlock;
import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.stream.Collectors;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Comparator;

public class Scp294drinkGiveProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure Scp294drinkGive!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency x for procedure Scp294drinkGive!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency y for procedure Scp294drinkGive!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency z for procedure Scp294drinkGive!");
			return;
		}
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure Scp294drinkGive!");
			return;
		}
		if (dependencies.get("guistate") == null) {
			if (!dependencies.containsKey("guistate"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency guistate for procedure Scp294drinkGive!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		double x = dependencies.get("x") instanceof Integer ? (int) dependencies.get("x") : (double) dependencies.get("x");
		double y = dependencies.get("y") instanceof Integer ? (int) dependencies.get("y") : (double) dependencies.get("y");
		double z = dependencies.get("z") instanceof Integer ? (int) dependencies.get("z") : (double) dependencies.get("z");
		Entity entity = (Entity) dependencies.get("entity");
		HashMap guistate = (HashMap) dependencies.get("guistate");
		if (world instanceof World && !world.isRemote()) {
			((World) world).playSound(null, new BlockPos(x, y, z),
					(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294enter")),
					SoundCategory.NEUTRAL, (float) 1, (float) 1);
		} else {
			((World) world).playSound(x, y, z,
					(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("scp_additions:scp294enter")),
					SoundCategory.NEUTRAL, (float) 1, (float) 1, false);
		}
		if (CoinItem.block == (new Object() {
			public ItemStack getItemStack(int sltid) {
				Entity _ent = entity;
				if (_ent instanceof ServerPlayerEntity) {
					Container _current = ((ServerPlayerEntity) _ent).openContainer;
					if (_current instanceof Supplier) {
						Object invobj = ((Supplier) _current).get();
						if (invobj instanceof Map) {
							return ((Slot) ((Map) invobj).get(sltid)).getStack();
						}
					}
				}
				return ItemStack.EMPTY;
			}
		}.getItemStack((int) (0))).getItem()) {
			if ((new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("air") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Air") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("nothing") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Nothing") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("hl3") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("HL3") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("half life 3") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Half Life 3") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("emptiness") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Emptiness") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("vacuum") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Vacuum") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("cup") || (new Object() {
				public String getText() {
					TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
					if (_tf != null) {
						return _tf.getText();
					}
					return "";
				}
			}.getText()).equals("Cup")) {
				if (world instanceof World && !world.isRemote()) {
					((World) world)
							.playSound(null, new BlockPos(x, y, z),
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
				if (entity instanceof PlayerEntity) {
					ItemStack _setstack = new ItemStack(EmptyCupItem.block);
					_setstack.setCount((int) 1);
					ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
				}
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
				}.getText()).equals("Coffee") || (new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("coffee") || (new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("Black coffee") || (new Object() {
					public String getText() {
						TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
						if (_tf != null) {
							return _tf.getText();
						}
						return "";
					}
				}.getText()).equals("black coffee")) {
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
								ItemStack _setstack = new ItemStack(CoffeeItem.block);
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
					}.getText()).equals("alcohol") || (new Object() {
						public String getText() {
							TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
							if (_tf != null) {
								return _tf.getText();
							}
							return "";
						}
					}.getText()).equals("Alcohol")) {
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
									ItemStack _setstack = new ItemStack(Drink1Item.block);
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
						}.getText()).equals("ethanol") || (new Object() {
							public String getText() {
								TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
								if (_tf != null) {
									return _tf.getText();
								}
								return "";
							}
						}.getText()).equals("Ethanol") || (new Object() {
							public String getText() {
								TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
								if (_tf != null) {
									return _tf.getText();
								}
								return "";
							}
						}.getText()).equals("ethanol liquid") || (new Object() {
							public String getText() {
								TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
								if (_tf != null) {
									return _tf.getText();
								}
								return "";
							}
						}.getText()).equals("Ethanol liquid")) {
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
										ItemStack _setstack = new ItemStack(EthanolItem.block);
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
							}.getText()).equals("spirit") || (new Object() {
								public String getText() {
									TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
									if (_tf != null) {
										return _tf.getText();
									}
									return "";
								}
							}.getText()).equals("Spirit")) {
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
											ItemStack _setstack = new ItemStack(SpiritItem.block);
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
								}.getText()).equals("vodka") || (new Object() {
									public String getText() {
										TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
										if (_tf != null) {
											return _tf.getText();
										}
										return "";
									}
								}.getText()).equals("Vodka")) {
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
												ItemStack _setstack = new ItemStack(VodkaItem.block);
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
									}.getText()).equals("aloe vera") || (new Object() {
										public String getText() {
											TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
											if (_tf != null) {
												return _tf.getText();
											}
											return "";
										}
									}.getText()).equals("Aloe Vera")) {
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
													ItemStack _setstack = new ItemStack(AloeItem.block);
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
										}.getText()).equals("cactus") || (new Object() {
											public String getText() {
												TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
												if (_tf != null) {
													return _tf.getText();
												}
												return "";
											}
										}.getText()).equals("Cactus")) {
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
														ItemStack _setstack = new ItemStack(CactusItem.block);
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
											}.getText()).equals("amnesia") || (new Object() {
												public String getText() {
													TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
													if (_tf != null) {
														return _tf.getText();
													}
													return "";
												}
											}.getText()).equals("Amnesia")) {
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
															ItemStack _setstack = new ItemStack(AmnesiaItem.block);
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
												}.getText()).equals("anti-energy") || (new Object() {
													public String getText() {
														TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
														if (_tf != null) {
															return _tf.getText();
														}
														return "";
													}
												}.getText()).equals("Anti-Energy") || (new Object() {
													public String getText() {
														TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
														if (_tf != null) {
															return _tf.getText();
														}
														return "";
													}
												}.getText()).equals("anti energy") || (new Object() {
													public String getText() {
														TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
														if (_tf != null) {
															return _tf.getText();
														}
														return "";
													}
												}.getText()).equals("Anti Energy")) {
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
																ItemStack _setstack = new ItemStack(AntiEnergyItem.block);
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
													}.getText()).equals("antimatter") || (new Object() {
														public String getText() {
															TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
															if (_tf != null) {
																return _tf.getText();
															}
															return "";
														}
													}.getText()).equals("Antimatter") || (new Object() {
														public String getText() {
															TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
															if (_tf != null) {
																return _tf.getText();
															}
															return "";
														}
													}.getText()).equals("anti-matter") || (new Object() {
														public String getText() {
															TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
															if (_tf != null) {
																return _tf.getText();
															}
															return "";
														}
													}.getText()).equals("Anti-matter") || (new Object() {
														public String getText() {
															TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
															if (_tf != null) {
																return _tf.getText();
															}
															return "";
														}
													}.getText()).equals("void") || (new Object() {
														public String getText() {
															TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
															if (_tf != null) {
																return _tf.getText();
															}
															return "";
														}
													}.getText()).equals("Void")) {
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
																	((World) world).createExplosion(null, (int) x, (int) y, (int) z, (float) 10,
																			Explosion.Mode.NONE);
																}
																{
																	List<Entity> _entfound = world
																			.getEntitiesWithinAABB(Entity.class,
																					new AxisAlignedBB(x - (10 / 2d), y - (10 / 2d), z - (10 / 2d),
																							x + (10 / 2d), y + (10 / 2d), z + (10 / 2d)),
																					null)
																			.stream().sorted(new Object() {
																				Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
																					return Comparator
																							.comparing((Function<Entity, Double>) (_entcnd -> _entcnd
																									.getDistanceSq(_x, _y, _z)));
																				}
																			}.compareDistOf(x, y, z)).collect(Collectors.toList());
																	for (Entity entityiterator : _entfound) {
																		if (entityiterator instanceof LivingEntity) {
																			((LivingEntity) entityiterator).attackEntityFrom(
																					new DamageSource("void").setDamageBypassesArmor(), (float) 100);
																		}
																	}
																}
																MinecraftForge.EVENT_BUS.unregister(this);
															}
														}.start(world, (int) 30);
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
														}.getText()).equals("aqua regia") || (new Object() {
															public String getText() {
																TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																if (_tf != null) {
																	return _tf.getText();
																}
																return "";
															}
														}.getText()).equals("Aqua Regia")) {
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
																		ItemStack _setstack = new ItemStack(AquaRegiaItem.block);
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
															}.getText()).equals("atomic") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("Atomic") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("nuclear") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("Nuclear") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("nuclear fusion") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("Nuclear Fusion") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("nuclear fission") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("Nuclear Fission") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("nuclear reaction") || (new Object() {
																public String getText() {
																	TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																	if (_tf != null) {
																		return _tf.getText();
																	}
																	return "";
																}
															}.getText()).equals("Nuclear Reaction")) {
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
																			((World) world).createExplosion(null, (int) x, (int) y, (int) z,
																					(float) 30, Explosion.Mode.NONE);
																		}
																		{
																			List<Entity> _entfound = world.getEntitiesWithinAABB(Entity.class,
																					new AxisAlignedBB(x - (30 / 2d), y - (30 / 2d), z - (30 / 2d),
																							x + (30 / 2d), y + (30 / 2d), z + (30 / 2d)),
																					null).stream().sorted(new Object() {
																						Comparator<Entity> compareDistOf(double _x, double _y,
																								double _z) {
																							return Comparator.comparing(
																									(Function<Entity, Double>) (_entcnd -> _entcnd
																											.getDistanceSq(_x, _y, _z)));
																						}
																					}.compareDistOf(x, y, z)).collect(Collectors.toList());
																			for (Entity entityiterator : _entfound) {
																				if (entityiterator instanceof LivingEntity) {
																					((LivingEntity) entityiterator).attackEntityFrom(
																							new DamageSource("nuclear").setDamageBypassesArmor(),
																							(float) 100);
																				}
																			}
																		}
																		MinecraftForge.EVENT_BUS.unregister(this);
																	}
																}.start(world, (int) 30);
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
																}.getText()).equals("beer") || (new Object() {
																	public String getText() {
																		TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																		if (_tf != null) {
																			return _tf.getText();
																		}
																		return "";
																	}
																}.getText()).equals("Beer")) {
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
																				ItemStack _setstack = new ItemStack(BeerItem.block);
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
																	}.getText()).equals("lager") || (new Object() {
																		public String getText() {
																			TextFieldWidget _tf = (TextFieldWidget) guistate.get("text:scp294input");
																			if (_tf != null) {
																				return _tf.getText();
																			}
																			return "";
																		}
																	}.getText()).equals("Lager")) {
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
																					ItemStack _setstack = new ItemStack(LagerItem.block);
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
																		}.getText()).equals("black corrosive liquid") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("Black corrosive liquid") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("scp-106") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("SCP-106") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("scp 106") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("SCP 106") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("old man") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("Old man") || (new Object() {
																			public String getText() {
																				TextFieldWidget _tf = (TextFieldWidget) guistate
																						.get("text:scp294input");
																				if (_tf != null) {
																					return _tf.getText();
																				}
																				return "";
																			}
																		}.getText()).equals("106")) {
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
																						ItemStack _setstack = new ItemStack(CorrosiveBlackItem.block);
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
																			}.getText()).equals("bleach") || (new Object() {
																				public String getText() {
																					TextFieldWidget _tf = (TextFieldWidget) guistate
																							.get("text:scp294input");
																					if (_tf != null) {
																						return _tf.getText();
																					}
																					return "";
																				}
																			}.getText()).equals("Bleach")) {
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
																							ItemStack _setstack = new ItemStack(BleachItem.block);
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
																				}.getText()).equals("blood") || (new Object() {
																					public String getText() {
																						TextFieldWidget _tf = (TextFieldWidget) guistate
																								.get("text:scp294input");
																						if (_tf != null) {
																							return _tf.getText();
																						}
																						return "";
																					}
																				}.getText()).equals("Blood")) {
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
																								ItemStack _setstack = new ItemStack(BloodItem.block);
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
																					}.getText()).equals("blood of christ") || (new Object() {
																						public String getText() {
																							TextFieldWidget _tf = (TextFieldWidget) guistate
																									.get("text:scp294input");
																							if (_tf != null) {
																								return _tf.getText();
																							}
																							return "";
																						}
																					}.getText()).equals("Blood of Christ") || (new Object() {
																						public String getText() {
																							TextFieldWidget _tf = (TextFieldWidget) guistate
																									.get("text:scp294input");
																							if (_tf != null) {
																								return _tf.getText();
																							}
																							return "";
																						}
																					}.getText()).equals("blood of jesus") || (new Object() {
																						public String getText() {
																							TextFieldWidget _tf = (TextFieldWidget) guistate
																									.get("text:scp294input");
																							if (_tf != null) {
																								return _tf.getText();
																							}
																							return "";
																						}
																					}.getText()).equals("Blood of Jesus") || (new Object() {
																						public String getText() {
																							TextFieldWidget _tf = (TextFieldWidget) guistate
																									.get("text:scp294input");
																							if (_tf != null) {
																								return _tf.getText();
																							}
																							return "";
																						}
																					}.getText()).equals("blood of jesus christ") || (new Object() {
																						public String getText() {
																							TextFieldWidget _tf = (TextFieldWidget) guistate
																									.get("text:scp294input");
																							if (_tf != null) {
																								return _tf.getText();
																							}
																							return "";
																						}
																					}.getText()).equals("Blood of Jesus Christ")) {
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
																								if (entity instanceof PlayerEntity
																										&& !entity.world.isRemote()) {
																									((PlayerEntity) entity).sendStatusMessage(
																											new StringTextComponent(
																													"SCP-294: \"Hic est enim Calix S\u00E1nguinis mei\""),
																											(false));
																								}
																								if (entity instanceof PlayerEntity) {
																									ItemStack _setstack = new ItemStack(
																											BloodOfChristItem.block);
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
																						}.getText()).equals("grimace") || (new Object() {
																							public String getText() {
																								TextFieldWidget _tf = (TextFieldWidget) guistate
																										.get("text:scp294input");
																								if (_tf != null) {
																									return _tf.getText();
																								}
																								return "";
																							}
																						}.getText()).equals("Grimace") || (new Object() {
																							public String getText() {
																								TextFieldWidget _tf = (TextFieldWidget) guistate
																										.get("text:scp294input");
																								if (_tf != null) {
																									return _tf.getText();
																								}
																								return "";
																							}
																						}.getText()).equals("grimace shake") || (new Object() {
																							public String getText() {
																								TextFieldWidget _tf = (TextFieldWidget) guistate
																										.get("text:scp294input");
																								if (_tf != null) {
																									return _tf.getText();
																								}
																								return "";
																							}
																						}.getText()).equals("Grimace Shake") || (new Object() {
																							public String getText() {
																								TextFieldWidget _tf = (TextFieldWidget) guistate
																										.get("text:scp294input");
																								if (_tf != null) {
																									return _tf.getText();
																								}
																								return "";
																							}
																						}.getText()).equals("Grimace shake")) {
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
																												GrimaceShakeItem.block);
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
																							}.getText()).equals("bose-einstein condensate")
																									|| (new Object() {
																										public String getText() {
																											TextFieldWidget _tf = (TextFieldWidget) guistate
																													.get("text:scp294input");
																											if (_tf != null) {
																												return _tf.getText();
																											}
																											return "";
																										}
																									}.getText()).equals("Bose-Einstein Condensate")
																									|| (new Object() {
																										public String getText() {
																											TextFieldWidget _tf = (TextFieldWidget) guistate
																													.get("text:scp294input");
																											if (_tf != null) {
																												return _tf.getText();
																											}
																											return "";
																										}
																									}.getText()).equals("bose einstein condensate")
																									|| (new Object() {
																										public String getText() {
																											TextFieldWidget _tf = (TextFieldWidget) guistate
																													.get("text:scp294input");
																											if (_tf != null) {
																												return _tf.getText();
																											}
																											return "";
																										}
																									}.getText()).equals("Bose Einstein Condensate")
																									|| (new Object() {
																										public String getText() {
																											TextFieldWidget _tf = (TextFieldWidget) guistate
																													.get("text:scp294input");
																											if (_tf != null) {
																												return _tf.getText();
																											}
																											return "";
																										}
																									}.getText()).equals("quantum gas")
																									|| (new Object() {
																										public String getText() {
																											TextFieldWidget _tf = (TextFieldWidget) guistate
																													.get("text:scp294input");
																											if (_tf != null) {
																												return _tf.getText();
																											}
																											return "";
																										}
																									}.getText()).equals("Quantum Gas")) {
																								if (world instanceof World && !world.isRemote()) {
																									((World) world).playSound(null,
																											new BlockPos(x, y, z),
																											(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																													.getValue(new ResourceLocation(
																															"scp_additions:scp294pouring")),
																											SoundCategory.NEUTRAL, (float) 1,
																											(float) 1);
																								} else {
																									((World) world).playSound(x, y, z,
																											(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																													.getValue(new ResourceLocation(
																															"scp_additions:scp294pouring")),
																											SoundCategory.NEUTRAL, (float) 1,
																											(float) 1, false);
																								}
																								{
																									Entity _ent = entity;
																									if (_ent instanceof ServerPlayerEntity) {
																										Container _current = ((ServerPlayerEntity) _ent).openContainer;
																										if (_current instanceof Supplier) {
																											Object invobj = ((Supplier) _current)
																													.get();
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
																									public void tick(
																											TickEvent.ServerTickEvent event) {
																										if (event.phase == TickEvent.Phase.END) {
																											this.ticks += 1;
																											if (this.ticks >= this.waitTicks)
																												run();
																										}
																									}

																									private void run() {
																										if (entity instanceof PlayerEntity) {
																											ItemStack _setstack = new ItemStack(
																													QuantumItem.block);
																											_setstack.setCount((int) 1);
																											ItemHandlerHelper.giveItemToPlayer(
																													((PlayerEntity) entity),
																													_setstack);
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
																								}.getText()).equals("carbon") || (new Object() {
																									public String getText() {
																										TextFieldWidget _tf = (TextFieldWidget) guistate
																												.get("text:scp294input");
																										if (_tf != null) {
																											return _tf.getText();
																										}
																										return "";
																									}
																								}.getText()).equals("Carbon")) {
																									if (world instanceof World && !world.isRemote()) {
																										((World) world).playSound(null,
																												new BlockPos(x, y, z),
																												(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																														.getValue(
																																new ResourceLocation(
																																		"scp_additions:scp294pouring")),
																												SoundCategory.NEUTRAL, (float) 1,
																												(float) 1);
																									} else {
																										((World) world).playSound(x, y, z,
																												(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																														.getValue(
																																new ResourceLocation(
																																		"scp_additions:scp294pouring")),
																												SoundCategory.NEUTRAL, (float) 1,
																												(float) 1, false);
																									}
																									{
																										Entity _ent = entity;
																										if (_ent instanceof ServerPlayerEntity) {
																											Container _current = ((ServerPlayerEntity) _ent).openContainer;
																											if (_current instanceof Supplier) {
																												Object invobj = ((Supplier) _current)
																														.get();
																												if (invobj instanceof Map) {
																													((Slot) ((Map) invobj)
																															.get((int) (0)))
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

																										public void start(IWorld world,
																												int waitTicks) {
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
																											if (entity instanceof PlayerEntity) {
																												ItemStack _setstack = new ItemStack(
																														CarbonItem.block);
																												_setstack.setCount((int) 1);
																												ItemHandlerHelper.giveItemToPlayer(
																														((PlayerEntity) entity),
																														_setstack);
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
																									}.getText()).equals("cassis fanta")
																											|| (new Object() {
																												public String getText() {
																													TextFieldWidget _tf = (TextFieldWidget) guistate
																															.get("text:scp294input");
																													if (_tf != null) {
																														return _tf.getText();
																													}
																													return "";
																												}
																											}.getText()).equals("Cassis Fanta")) {
																										if (world instanceof World
																												&& !world.isRemote()) {
																											((World) world).playSound(null,
																													new BlockPos(x, y, z),
																													(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																															.getValue(
																																	new ResourceLocation(
																																			"scp_additions:scp294pouring")),
																													SoundCategory.NEUTRAL, (float) 1,
																													(float) 1);
																										} else {
																											((World) world).playSound(x, y, z,
																													(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																															.getValue(
																																	new ResourceLocation(
																																			"scp_additions:scp294pouring")),
																													SoundCategory.NEUTRAL, (float) 1,
																													(float) 1, false);
																										}
																										{
																											Entity _ent = entity;
																											if (_ent instanceof ServerPlayerEntity) {
																												Container _current = ((ServerPlayerEntity) _ent).openContainer;
																												if (_current instanceof Supplier) {
																													Object invobj = ((Supplier) _current)
																															.get();
																													if (invobj instanceof Map) {
																														((Slot) ((Map) invobj)
																																.get((int) (0)))
																																.decrStackSize(
																																		(int) (1));
																														_current.detectAndSendChanges();
																													}
																												}
																											}
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
																												if (entity instanceof PlayerEntity) {
																													ItemStack _setstack = new ItemStack(
																															CassisFantaItem.block);
																													_setstack.setCount((int) 1);
																													ItemHandlerHelper
																															.giveItemToPlayer(
																																	((PlayerEntity) entity),
																																	_setstack);
																												}
																												MinecraftForge.EVENT_BUS
																														.unregister(this);
																											}
																										}.start(world, (int) 40);
																										ScpAdditionsModVariables.WorldVariables.get(
																												world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																														.get(world).Scp294stock + 1);
																										ScpAdditionsModVariables.WorldVariables
																												.get(world).syncData(world);
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
																										}.getText()).equals("carrot juice")
																												|| (new Object() {
																													public String getText() {
																														TextFieldWidget _tf = (TextFieldWidget) guistate
																																.get("text:scp294input");
																														if (_tf != null) {
																															return _tf.getText();
																														}
																														return "";
																													}
																												}.getText()).equals("Carrot juice")
																												|| (new Object() {
																													public String getText() {
																														TextFieldWidget _tf = (TextFieldWidget) guistate
																																.get("text:scp294input");
																														if (_tf != null) {
																															return _tf.getText();
																														}
																														return "";
																													}
																												}.getText()).equals("carrot")
																												|| (new Object() {
																													public String getText() {
																														TextFieldWidget _tf = (TextFieldWidget) guistate
																																.get("text:scp294input");
																														if (_tf != null) {
																															return _tf.getText();
																														}
																														return "";
																													}
																												}.getText()).equals("Carrot")) {
																											if (world instanceof World
																													&& !world.isRemote()) {
																												((World) world).playSound(null,
																														new BlockPos(x, y, z),
																														(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																.getValue(
																																		new ResourceLocation(
																																				"scp_additions:scp294pouring")),
																														SoundCategory.NEUTRAL,
																														(float) 1, (float) 1);
																											} else {
																												((World) world).playSound(x, y, z,
																														(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																.getValue(
																																		new ResourceLocation(
																																				"scp_additions:scp294pouring")),
																														SoundCategory.NEUTRAL,
																														(float) 1, (float) 1, false);
																											}
																											{
																												Entity _ent = entity;
																												if (_ent instanceof ServerPlayerEntity) {
																													Container _current = ((ServerPlayerEntity) _ent).openContainer;
																													if (_current instanceof Supplier) {
																														Object invobj = ((Supplier) _current)
																																.get();
																														if (invobj instanceof Map) {
																															((Slot) ((Map) invobj)
																																	.get((int) (0)))
																																	.decrStackSize(
																																			(int) (1));
																															_current.detectAndSendChanges();
																														}
																													}
																												}
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
																													if (entity instanceof PlayerEntity) {
																														ItemStack _setstack = new ItemStack(
																																CarrotItem.block);
																														_setstack.setCount((int) 1);
																														ItemHandlerHelper
																																.giveItemToPlayer(
																																		((PlayerEntity) entity),
																																		_setstack);
																													}
																													MinecraftForge.EVENT_BUS
																															.unregister(this);
																												}
																											}.start(world, (int) 40);
																											ScpAdditionsModVariables.WorldVariables
																													.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																															.get(world).Scp294stock
																															+ 1);
																											ScpAdditionsModVariables.WorldVariables
																													.get(world).syncData(world);
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
																											}.getText()).equals("champagne")
																													|| (new Object() {
																														public String getText() {
																															TextFieldWidget _tf = (TextFieldWidget) guistate
																																	.get("text:scp294input");
																															if (_tf != null) {
																																return _tf.getText();
																															}
																															return "";
																														}
																													}.getText())
																															.equals("Champagne")) {
																												if (world instanceof World
																														&& !world.isRemote()) {
																													((World) world).playSound(null,
																															new BlockPos(x, y, z),
																															(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																	.getValue(
																																			new ResourceLocation(
																																					"scp_additions:scp294pouring")),
																															SoundCategory.NEUTRAL,
																															(float) 1, (float) 1);
																												} else {
																													((World) world).playSound(x, y, z,
																															(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																	.getValue(
																																			new ResourceLocation(
																																					"scp_additions:scp294pouring")),
																															SoundCategory.NEUTRAL,
																															(float) 1, (float) 1,
																															false);
																												}
																												{
																													Entity _ent = entity;
																													if (_ent instanceof ServerPlayerEntity) {
																														Container _current = ((ServerPlayerEntity) _ent).openContainer;
																														if (_current instanceof Supplier) {
																															Object invobj = ((Supplier) _current)
																																	.get();
																															if (invobj instanceof Map) {
																																((Slot) ((Map) invobj)
																																		.get((int) (0)))
																																		.decrStackSize(
																																				(int) (1));
																																_current.detectAndSendChanges();
																															}
																														}
																													}
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
																														if (entity instanceof PlayerEntity) {
																															ItemStack _setstack = new ItemStack(
																																	ChampagneItem.block);
																															_setstack.setCount(
																																	(int) 1);
																															ItemHandlerHelper
																																	.giveItemToPlayer(
																																			((PlayerEntity) entity),
																																			_setstack);
																														}
																														MinecraftForge.EVENT_BUS
																																.unregister(this);
																													}
																												}.start(world, (int) 40);
																												ScpAdditionsModVariables.WorldVariables
																														.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																																.get(world).Scp294stock
																																+ 1);
																												ScpAdditionsModVariables.WorldVariables
																														.get(world).syncData(world);
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
																												}.getText()).equals("chim")
																														|| (new Object() {
																															public String getText() {
																																TextFieldWidget _tf = (TextFieldWidget) guistate
																																		.get("text:scp294input");
																																if (_tf != null) {
																																	return _tf
																																			.getText();
																																}
																																return "";
																															}
																														}.getText()).equals("Chim")) {
																													if (world instanceof World
																															&& !world.isRemote()) {
																														((World) world).playSound(
																																null,
																																new BlockPos(x, y, z),
																																(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																		.getValue(
																																				new ResourceLocation(
																																						"scp_additions:scp294pouring")),
																																SoundCategory.NEUTRAL,
																																(float) 1, (float) 1);
																													} else {
																														((World) world).playSound(x,
																																y, z,
																																(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																		.getValue(
																																				new ResourceLocation(
																																						"scp_additions:scp294pouring")),
																																SoundCategory.NEUTRAL,
																																(float) 1, (float) 1,
																																false);
																													}
																													{
																														Entity _ent = entity;
																														if (_ent instanceof ServerPlayerEntity) {
																															Container _current = ((ServerPlayerEntity) _ent).openContainer;
																															if (_current instanceof Supplier) {
																																Object invobj = ((Supplier) _current)
																																		.get();
																																if (invobj instanceof Map) {
																																	((Slot) ((Map) invobj)
																																			.get((int) (0)))
																																			.decrStackSize(
																																					(int) (1));
																																	_current.detectAndSendChanges();
																																}
																															}
																														}
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
																															if (entity instanceof PlayerEntity) {
																																ItemStack _setstack = new ItemStack(
																																		ChimItem.block);
																																_setstack.setCount(
																																		(int) 1);
																																ItemHandlerHelper
																																		.giveItemToPlayer(
																																				((PlayerEntity) entity),
																																				_setstack);
																															}
																															MinecraftForge.EVENT_BUS
																																	.unregister(this);
																														}
																													}.start(world, (int) 40);
																													ScpAdditionsModVariables.WorldVariables
																															.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																																	.get(world).Scp294stock
																																	+ 1);
																													ScpAdditionsModVariables.WorldVariables
																															.get(world)
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
																													}.getText()).equals("cider")
																															|| (new Object() {
																																public String getText() {
																																	TextFieldWidget _tf = (TextFieldWidget) guistate
																																			.get("text:scp294input");
																																	if (_tf != null) {
																																		return _tf
																																				.getText();
																																	}
																																	return "";
																																}
																															}.getText()).equals(
																																	"Cider")) {
																														if (world instanceof World
																																&& !world
																																		.isRemote()) {
																															((World) world).playSound(
																																	null,
																																	new BlockPos(x, y,
																																			z),
																																	(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																			.getValue(
																																					new ResourceLocation(
																																							"scp_additions:scp294pouring")),
																																	SoundCategory.NEUTRAL,
																																	(float) 1,
																																	(float) 1);
																														} else {
																															((World) world).playSound(
																																	x, y, z,
																																	(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																			.getValue(
																																					new ResourceLocation(
																																							"scp_additions:scp294pouring")),
																																	SoundCategory.NEUTRAL,
																																	(float) 1,
																																	(float) 1, false);
																														}
																														{
																															Entity _ent = entity;
																															if (_ent instanceof ServerPlayerEntity) {
																																Container _current = ((ServerPlayerEntity) _ent).openContainer;
																																if (_current instanceof Supplier) {
																																	Object invobj = ((Supplier) _current)
																																			.get();
																																	if (invobj instanceof Map) {
																																		((Slot) ((Map) invobj)
																																				.get((int) (0)))
																																				.decrStackSize(
																																						(int) (1));
																																		_current.detectAndSendChanges();
																																	}
																																}
																															}
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
																																if (entity instanceof PlayerEntity) {
																																	ItemStack _setstack = new ItemStack(
																																			CiderItem.block);
																																	_setstack
																																			.setCount(
																																					(int) 1);
																																	ItemHandlerHelper
																																			.giveItemToPlayer(
																																					((PlayerEntity) entity),
																																					_setstack);
																																}
																																MinecraftForge.EVENT_BUS
																																		.unregister(
																																				this);
																															}
																														}.start(world, (int) 40);
																														ScpAdditionsModVariables.WorldVariables
																																.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																																		.get(world).Scp294stock
																																		+ 1);
																														ScpAdditionsModVariables.WorldVariables
																																.get(world)
																																.syncData(world);
																													} else {
																														if ((new Object() {
																															public String getText() {
																																TextFieldWidget _tf = (TextFieldWidget) guistate
																																		.get("text:scp294input");
																																if (_tf != null) {
																																	return _tf
																																			.getText();
																																}
																																return "";
																															}
																														}.getText())
																																.equals("apple cider")
																																|| (new Object() {
																																	public String getText() {
																																		TextFieldWidget _tf = (TextFieldWidget) guistate
																																				.get("text:scp294input");
																																		if (_tf != null) {
																																			return _tf
																																					.getText();
																																		}
																																		return "";
																																	}
																																}.getText()).equals(
																																		"Apple Cider")) {
																															if (world instanceof World
																																	&& !world
																																			.isRemote()) {
																																((World) world)
																																		.playSound(
																																				null,
																																				new BlockPos(
																																						x,
																																						y,
																																						z),
																																				(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																						.getValue(
																																								new ResourceLocation(
																																										"scp_additions:scp294pouring")),
																																				SoundCategory.NEUTRAL,
																																				(float) 1,
																																				(float) 1);
																															} else {
																																((World) world)
																																		.playSound(x,
																																				y, z,
																																				(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																						.getValue(
																																								new ResourceLocation(
																																										"scp_additions:scp294pouring")),
																																				SoundCategory.NEUTRAL,
																																				(float) 1,
																																				(float) 1,
																																				false);
																															}
																															{
																																Entity _ent = entity;
																																if (_ent instanceof ServerPlayerEntity) {
																																	Container _current = ((ServerPlayerEntity) _ent).openContainer;
																																	if (_current instanceof Supplier) {
																																		Object invobj = ((Supplier) _current)
																																				.get();
																																		if (invobj instanceof Map) {
																																			((Slot) ((Map) invobj)
																																					.get((int) (0)))
																																					.decrStackSize(
																																							(int) (1));
																																			_current.detectAndSendChanges();
																																		}
																																	}
																																}
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
																																	if (entity instanceof PlayerEntity) {
																																		ItemStack _setstack = new ItemStack(
																																				AppleCiderItem.block);
																																		_setstack
																																				.setCount(
																																						(int) 1);
																																		ItemHandlerHelper
																																				.giveItemToPlayer(
																																						((PlayerEntity) entity),
																																						_setstack);
																																	}
																																	MinecraftForge.EVENT_BUS
																																			.unregister(
																																					this);
																																}
																															}.start(world, (int) 40);
																															ScpAdditionsModVariables.WorldVariables
																																	.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																																			.get(world).Scp294stock
																																			+ 1);
																															ScpAdditionsModVariables.WorldVariables
																																	.get(world)
																																	.syncData(world);
																														} else {
																															if ((new Object() {
																																public String getText() {
																																	TextFieldWidget _tf = (TextFieldWidget) guistate
																																			.get("text:scp294input");
																																	if (_tf != null) {
																																		return _tf
																																				.getText();
																																	}
																																	return "";
																																}
																															}.getText()).equals(
																																	"pear cider")
																																	|| (new Object() {
																																		public String getText() {
																																			TextFieldWidget _tf = (TextFieldWidget) guistate
																																					.get("text:scp294input");
																																			if (_tf != null) {
																																				return _tf
																																						.getText();
																																			}
																																			return "";
																																		}
																																	}.getText())
																																			.equals("Pear Cider")) {
																																if (world instanceof World
																																		&& !world
																																				.isRemote()) {
																																	((World) world)
																																			.playSound(
																																					null,
																																					new BlockPos(
																																							x,
																																							y,
																																							z),
																																					(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																							.getValue(
																																									new ResourceLocation(
																																											"scp_additions:scp294pouring")),
																																					SoundCategory.NEUTRAL,
																																					(float) 1,
																																					(float) 1);
																																} else {
																																	((World) world)
																																			.playSound(
																																					x,
																																					y,
																																					z,
																																					(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																							.getValue(
																																									new ResourceLocation(
																																											"scp_additions:scp294pouring")),
																																					SoundCategory.NEUTRAL,
																																					(float) 1,
																																					(float) 1,
																																					false);
																																}
																																{
																																	Entity _ent = entity;
																																	if (_ent instanceof ServerPlayerEntity) {
																																		Container _current = ((ServerPlayerEntity) _ent).openContainer;
																																		if (_current instanceof Supplier) {
																																			Object invobj = ((Supplier) _current)
																																					.get();
																																			if (invobj instanceof Map) {
																																				((Slot) ((Map) invobj)
																																						.get((int) (0)))
																																						.decrStackSize(
																																								(int) (1));
																																				_current.detectAndSendChanges();
																																			}
																																		}
																																	}
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
																																		if (entity instanceof PlayerEntity) {
																																			ItemStack _setstack = new ItemStack(
																																					PearCiderItem.block);
																																			_setstack
																																					.setCount(
																																							(int) 1);
																																			ItemHandlerHelper
																																					.giveItemToPlayer(
																																							((PlayerEntity) entity),
																																							_setstack);
																																		}
																																		MinecraftForge.EVENT_BUS
																																				.unregister(
																																						this);
																																	}
																																}.start(world,
																																		(int) 40);
																																ScpAdditionsModVariables.WorldVariables
																																		.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																																				.get(world).Scp294stock
																																				+ 1);
																																ScpAdditionsModVariables.WorldVariables
																																		.get(world)
																																		.syncData(
																																				world);
																															} else {
																																if ((new Object() {
																																	public String getText() {
																																		TextFieldWidget _tf = (TextFieldWidget) guistate
																																				.get("text:scp294input");
																																		if (_tf != null) {
																																			return _tf
																																					.getText();
																																		}
																																		return "";
																																	}
																																}.getText()).equals(
																																		"chocolate")
																																		|| (new Object() {
																																			public String getText() {
																																				TextFieldWidget _tf = (TextFieldWidget) guistate
																																						.get("text:scp294input");
																																				if (_tf != null) {
																																					return _tf
																																							.getText();
																																				}
																																				return "";
																																			}
																																		}.getText())
																																				.equals("Chocolate")
																																		|| (new Object() {
																																			public String getText() {
																																				TextFieldWidget _tf = (TextFieldWidget) guistate
																																						.get("text:scp294input");
																																				if (_tf != null) {
																																					return _tf
																																							.getText();
																																				}
																																				return "";
																																			}
																																		}.getText())
																																				.equals("cocoa")
																																		|| (new Object() {
																																			public String getText() {
																																				TextFieldWidget _tf = (TextFieldWidget) guistate
																																						.get("text:scp294input");
																																				if (_tf != null) {
																																					return _tf
																																							.getText();
																																				}
																																				return "";
																																			}
																																		}.getText())
																																				.equals("Cocoa")
																																		|| (new Object() {
																																			public String getText() {
																																				TextFieldWidget _tf = (TextFieldWidget) guistate
																																						.get("text:scp294input");
																																				if (_tf != null) {
																																					return _tf
																																							.getText();
																																				}
																																				return "";
																																			}
																																		}.getText())
																																				.equals("hot chocolate")
																																		|| (new Object() {
																																			public String getText() {
																																				TextFieldWidget _tf = (TextFieldWidget) guistate
																																						.get("text:scp294input");
																																				if (_tf != null) {
																																					return _tf
																																							.getText();
																																				}
																																				return "";
																																			}
																																		}.getText())
																																				.equals("Hot chocolate")
																																		|| (new Object() {
																																			public String getText() {
																																				TextFieldWidget _tf = (TextFieldWidget) guistate
																																						.get("text:scp294input");
																																				if (_tf != null) {
																																					return _tf
																																							.getText();
																																				}
																																				return "";
																																			}
																																		}.getText())
																																				.equals("hot cocoa")
																																		|| (new Object() {
																																			public String getText() {
																																				TextFieldWidget _tf = (TextFieldWidget) guistate
																																						.get("text:scp294input");
																																				if (_tf != null) {
																																					return _tf
																																							.getText();
																																				}
																																				return "";
																																			}
																																		}.getText())
																																				.equals("Hot cocoa")) {
																																	if (world instanceof World
																																			&& !world
																																					.isRemote()) {
																																		((World) world)
																																				.playSound(
																																						null,
																																						new BlockPos(
																																								x,
																																								y,
																																								z),
																																						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																								.getValue(
																																										new ResourceLocation(
																																												"scp_additions:scp294pouring")),
																																						SoundCategory.NEUTRAL,
																																						(float) 1,
																																						(float) 1);
																																	} else {
																																		((World) world)
																																				.playSound(
																																						x,
																																						y,
																																						z,
																																						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																								.getValue(
																																										new ResourceLocation(
																																												"scp_additions:scp294pouring")),
																																						SoundCategory.NEUTRAL,
																																						(float) 1,
																																						(float) 1,
																																						false);
																																	}
																																	{
																																		Entity _ent = entity;
																																		if (_ent instanceof ServerPlayerEntity) {
																																			Container _current = ((ServerPlayerEntity) _ent).openContainer;
																																			if (_current instanceof Supplier) {
																																				Object invobj = ((Supplier) _current)
																																						.get();
																																				if (invobj instanceof Map) {
																																					((Slot) ((Map) invobj)
																																							.get((int) (0)))
																																							.decrStackSize(
																																									(int) (1));
																																					_current.detectAndSendChanges();
																																				}
																																			}
																																		}
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
																																			if (entity instanceof PlayerEntity) {
																																				ItemStack _setstack = new ItemStack(
																																						ChocolateItem.block);
																																				_setstack
																																						.setCount(
																																								(int) 1);
																																				ItemHandlerHelper
																																						.giveItemToPlayer(
																																								((PlayerEntity) entity),
																																								_setstack);
																																			}
																																			MinecraftForge.EVENT_BUS
																																					.unregister(
																																							this);
																																		}
																																	}.start(world,
																																			(int) 40);
																																	ScpAdditionsModVariables.WorldVariables
																																			.get(world).Scp294stock = (ScpAdditionsModVariables.WorldVariables
																																					.get(world).Scp294stock
																																					+ 1);
																																	ScpAdditionsModVariables.WorldVariables
																																			.get(world)
																																			.syncData(
																																					world);
																																} else {
																																	{
																																		BlockPos _bp = new BlockPos(
																																				x, y,
																																				z);
																																		BlockState _bs = Scp294OutOfRangeBlock.block
																																				.getDefaultState();
																																		BlockState _bso = world
																																				.getBlockState(
																																						_bp);
																																		for (Map.Entry<Property<?>, Comparable<?>> entry : _bso
																																				.getValues()
																																				.entrySet()) {
																																			Property _property = _bs
																																					.getBlock()
																																					.getStateContainer()
																																					.getProperty(
																																							entry.getKey()
																																									.getName());
																																			if (_property != null
																																					&& _bs.get(
																																							_property) != null)
																																				try {
																																					_bs = _bs
																																							.with(_property,
																																									(Comparable) entry
																																											.getValue());
																																				} catch (Exception e) {
																																				}
																																		}
																																		TileEntity _te = world
																																				.getTileEntity(
																																						_bp);
																																		CompoundNBT _bnbt = null;
																																		if (_te != null) {
																																			_bnbt = _te
																																					.write(new CompoundNBT());
																																			_te.remove();
																																		}
																																		world.setBlockState(
																																				_bp,
																																				_bs,
																																				3);
																																		if (_bnbt != null) {
																																			_te = world
																																					.getTileEntity(
																																							_bp);
																																			if (_te != null) {
																																				try {
																																					_te.read(
																																							_bso,
																																							_bnbt);
																																				} catch (Exception ignored) {
																																				}
																																			}
																																		}
																																	}
																																	if (entity instanceof PlayerEntity)
																																		((PlayerEntity) entity)
																																				.closeScreen();
																																	if (world instanceof World
																																			&& !world
																																					.isRemote()) {
																																		((World) world)
																																				.playSound(
																																						null,
																																						new BlockPos(
																																								x,
																																								y,
																																								z),
																																						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																								.getValue(
																																										new ResourceLocation(
																																												"scp_additions:scp294outofrange")),
																																						SoundCategory.NEUTRAL,
																																						(float) 1,
																																						(float) 1);
																																	} else {
																																		((World) world)
																																				.playSound(
																																						x,
																																						y,
																																						z,
																																						(net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS
																																								.getValue(
																																										new ResourceLocation(
																																												"scp_additions:scp294outofrange")),
																																						SoundCategory.NEUTRAL,
																																						(float) 1,
																																						(float) 1,
																																						false);
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
