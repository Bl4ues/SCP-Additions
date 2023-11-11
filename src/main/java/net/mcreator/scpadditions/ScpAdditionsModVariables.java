package net.mcreator.scpadditions;

import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.Capability;

import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.world.IServerWorld;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Direction;
import net.minecraft.network.PacketBuffer;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

public class ScpAdditionsModVariables {
	public ScpAdditionsModVariables(ScpAdditionsModElements elements) {
		elements.addNetworkMessage(WorldSavedDataSyncMessage.class, WorldSavedDataSyncMessage::buffer, WorldSavedDataSyncMessage::new,
				WorldSavedDataSyncMessage::handler);
		elements.addNetworkMessage(PlayerVariablesSyncMessage.class, PlayerVariablesSyncMessage::buffer, PlayerVariablesSyncMessage::new,
				PlayerVariablesSyncMessage::handler);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
	}

	private void init(FMLCommonSetupEvent event) {
		CapabilityManager.INSTANCE.register(PlayerVariables.class, new PlayerVariablesStorage(), PlayerVariables::new);
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!event.getPlayer().world.isRemote()) {
			WorldSavedData mapdata = MapVariables.get(event.getPlayer().world);
			WorldSavedData worlddata = WorldVariables.get(event.getPlayer().world);
			if (mapdata != null)
				ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
						new WorldSavedDataSyncMessage(0, mapdata));
			if (worlddata != null)
				ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
						new WorldSavedDataSyncMessage(1, worlddata));
		}
	}

	@SubscribeEvent
	public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!event.getPlayer().world.isRemote()) {
			WorldSavedData worlddata = WorldVariables.get(event.getPlayer().world);
			if (worlddata != null)
				ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
						new WorldSavedDataSyncMessage(1, worlddata));
		}
	}

	public static class WorldVariables extends WorldSavedData {
		public static final String DATA_NAME = "scp_additions_worldvars";
		public double Scp294stock = 0;
		public double coinslot = 0;

		public WorldVariables() {
			super(DATA_NAME);
		}

		public WorldVariables(String s) {
			super(s);
		}

		@Override
		public void read(CompoundNBT nbt) {
			Scp294stock = nbt.getDouble("Scp294stock");
			coinslot = nbt.getDouble("coinslot");
		}

		@Override
		public CompoundNBT write(CompoundNBT nbt) {
			nbt.putDouble("Scp294stock", Scp294stock);
			nbt.putDouble("coinslot", coinslot);
			return nbt;
		}

		public void syncData(IWorld world) {
			this.markDirty();
			if (world instanceof World && !world.isRemote())
				ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.DIMENSION.with(((World) world)::getDimensionKey),
						new WorldSavedDataSyncMessage(1, this));
		}

		static WorldVariables clientSide = new WorldVariables();

		public static WorldVariables get(IWorld world) {
			if (world instanceof ServerWorld) {
				return ((ServerWorld) world).getSavedData().getOrCreate(WorldVariables::new, DATA_NAME);
			} else {
				return clientSide;
			}
		}
	}

	public static class MapVariables extends WorldSavedData {
		public static final String DATA_NAME = "scp_additions_mapvars";
		public boolean Scp914Rough = false;
		public boolean Scp914Coarse = false;
		public boolean Scp914OneToOne = true;
		public boolean Scp914Fine = false;
		public boolean Scp914VeryFine = false;
		public boolean Scp914refining = false;
		public double RandomX = 0;
		public double RandomY = 0;
		public double RandomZ = 0;

		public MapVariables() {
			super(DATA_NAME);
		}

		public MapVariables(String s) {
			super(s);
		}

		@Override
		public void read(CompoundNBT nbt) {
			Scp914Rough = nbt.getBoolean("Scp914Rough");
			Scp914Coarse = nbt.getBoolean("Scp914Coarse");
			Scp914OneToOne = nbt.getBoolean("Scp914OneToOne");
			Scp914Fine = nbt.getBoolean("Scp914Fine");
			Scp914VeryFine = nbt.getBoolean("Scp914VeryFine");
			Scp914refining = nbt.getBoolean("Scp914refining");
			RandomX = nbt.getDouble("RandomX");
			RandomY = nbt.getDouble("RandomY");
			RandomZ = nbt.getDouble("RandomZ");
		}

		@Override
		public CompoundNBT write(CompoundNBT nbt) {
			nbt.putBoolean("Scp914Rough", Scp914Rough);
			nbt.putBoolean("Scp914Coarse", Scp914Coarse);
			nbt.putBoolean("Scp914OneToOne", Scp914OneToOne);
			nbt.putBoolean("Scp914Fine", Scp914Fine);
			nbt.putBoolean("Scp914VeryFine", Scp914VeryFine);
			nbt.putBoolean("Scp914refining", Scp914refining);
			nbt.putDouble("RandomX", RandomX);
			nbt.putDouble("RandomY", RandomY);
			nbt.putDouble("RandomZ", RandomZ);
			return nbt;
		}

		public void syncData(IWorld world) {
			this.markDirty();
			if (world instanceof World && !world.isRemote())
				ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new WorldSavedDataSyncMessage(0, this));
		}

		static MapVariables clientSide = new MapVariables();

		public static MapVariables get(IWorld world) {
			if (world instanceof IServerWorld) {
				return ((IServerWorld) world).getWorld().getServer().getWorld(World.OVERWORLD).getSavedData().getOrCreate(MapVariables::new,
						DATA_NAME);
			} else {
				return clientSide;
			}
		}
	}

	public static class WorldSavedDataSyncMessage {
		public int type;
		public WorldSavedData data;

		public WorldSavedDataSyncMessage(PacketBuffer buffer) {
			this.type = buffer.readInt();
			this.data = this.type == 0 ? new MapVariables() : new WorldVariables();
			this.data.read(buffer.readCompoundTag());
		}

		public WorldSavedDataSyncMessage(int type, WorldSavedData data) {
			this.type = type;
			this.data = data;
		}

		public static void buffer(WorldSavedDataSyncMessage message, PacketBuffer buffer) {
			buffer.writeInt(message.type);
			buffer.writeCompoundTag(message.data.write(new CompoundNBT()));
		}

		public static void handler(WorldSavedDataSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				if (!context.getDirection().getReceptionSide().isServer()) {
					if (message.type == 0)
						MapVariables.clientSide = (MapVariables) message.data;
					else
						WorldVariables.clientSide = (WorldVariables) message.data;
				}
			});
			context.setPacketHandled(true);
		}
	}

	@CapabilityInject(PlayerVariables.class)
	public static Capability<PlayerVariables> PLAYER_VARIABLES_CAPABILITY = null;

	@SubscribeEvent
	public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof PlayerEntity && !(event.getObject() instanceof FakePlayer))
			event.addCapability(new ResourceLocation("scp_additions", "player_variables"), new PlayerVariablesProvider());
	}

	private static class PlayerVariablesProvider implements ICapabilitySerializable<INBT> {
		private final LazyOptional<PlayerVariables> instance = LazyOptional.of(PLAYER_VARIABLES_CAPABILITY::getDefaultInstance);

		@Override
		public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
			return cap == PLAYER_VARIABLES_CAPABILITY ? instance.cast() : LazyOptional.empty();
		}

		@Override
		public INBT serializeNBT() {
			return PLAYER_VARIABLES_CAPABILITY.getStorage().writeNBT(PLAYER_VARIABLES_CAPABILITY, this.instance.orElseThrow(RuntimeException::new),
					null);
		}

		@Override
		public void deserializeNBT(INBT nbt) {
			PLAYER_VARIABLES_CAPABILITY.getStorage().readNBT(PLAYER_VARIABLES_CAPABILITY, this.instance.orElseThrow(RuntimeException::new), null,
					nbt);
		}
	}

	private static class PlayerVariablesStorage implements Capability.IStorage<PlayerVariables> {
		@Override
		public INBT writeNBT(Capability<PlayerVariables> capability, PlayerVariables instance, Direction side) {
			CompoundNBT nbt = new CompoundNBT();
			nbt.putBoolean("Opos", instance.Opos);
			nbt.putBoolean("Oneg", instance.Oneg);
			nbt.putBoolean("Apos", instance.Apos);
			nbt.putBoolean("Aneg", instance.Aneg);
			nbt.putBoolean("Bpos", instance.Bpos);
			nbt.putBoolean("Bneg", instance.Bneg);
			nbt.putBoolean("ABpos", instance.ABpos);
			nbt.putBoolean("ABneg", instance.ABneg);
			nbt.putBoolean("PlayerOn1to1", instance.PlayerOn1to1);
			nbt.putBoolean("PlayerOn1to1_2", instance.PlayerOn1to1_2);
			nbt.putBoolean("PlayerOn1to1_3", instance.PlayerOn1to1_3);
			nbt.putBoolean("PlayerOn1to1_4", instance.PlayerOn1to1_4);
			nbt.putBoolean("PlayerOn1to1_5", instance.PlayerOn1to1_5);
			nbt.putBoolean("PlayerOn1to1_6", instance.PlayerOn1to1_6);
			nbt.putBoolean("PlayerOn1to1_7", instance.PlayerOn1to1_7);
			nbt.putBoolean("PlayerOn1to1_8", instance.PlayerOn1to1_8);
			nbt.putBoolean("PlayerOn1to1_9", instance.PlayerOn1to1_9);
			nbt.putBoolean("PlayerOn1to1_10", instance.PlayerOn1to1_10);
			nbt.putBoolean("PlayerOn1to1_11", instance.PlayerOn1to1_11);
			nbt.putBoolean("scp059infected1", instance.scp059infected1);
			nbt.putBoolean("scp059infected0", instance.scp059infected0);
			nbt.putBoolean("fear", instance.fear);
			nbt.putBoolean("nuclear", instance.nuclear);
			nbt.putBoolean("blackh", instance.blackh);
			return nbt;
		}

		@Override
		public void readNBT(Capability<PlayerVariables> capability, PlayerVariables instance, Direction side, INBT inbt) {
			CompoundNBT nbt = (CompoundNBT) inbt;
			instance.Opos = nbt.getBoolean("Opos");
			instance.Oneg = nbt.getBoolean("Oneg");
			instance.Apos = nbt.getBoolean("Apos");
			instance.Aneg = nbt.getBoolean("Aneg");
			instance.Bpos = nbt.getBoolean("Bpos");
			instance.Bneg = nbt.getBoolean("Bneg");
			instance.ABpos = nbt.getBoolean("ABpos");
			instance.ABneg = nbt.getBoolean("ABneg");
			instance.PlayerOn1to1 = nbt.getBoolean("PlayerOn1to1");
			instance.PlayerOn1to1_2 = nbt.getBoolean("PlayerOn1to1_2");
			instance.PlayerOn1to1_3 = nbt.getBoolean("PlayerOn1to1_3");
			instance.PlayerOn1to1_4 = nbt.getBoolean("PlayerOn1to1_4");
			instance.PlayerOn1to1_5 = nbt.getBoolean("PlayerOn1to1_5");
			instance.PlayerOn1to1_6 = nbt.getBoolean("PlayerOn1to1_6");
			instance.PlayerOn1to1_7 = nbt.getBoolean("PlayerOn1to1_7");
			instance.PlayerOn1to1_8 = nbt.getBoolean("PlayerOn1to1_8");
			instance.PlayerOn1to1_9 = nbt.getBoolean("PlayerOn1to1_9");
			instance.PlayerOn1to1_10 = nbt.getBoolean("PlayerOn1to1_10");
			instance.PlayerOn1to1_11 = nbt.getBoolean("PlayerOn1to1_11");
			instance.scp059infected1 = nbt.getBoolean("scp059infected1");
			instance.scp059infected0 = nbt.getBoolean("scp059infected0");
			instance.fear = nbt.getBoolean("fear");
			instance.nuclear = nbt.getBoolean("nuclear");
			instance.blackh = nbt.getBoolean("blackh");
		}
	}

	public static class PlayerVariables {
		public boolean Opos = false;
		public boolean Oneg = false;
		public boolean Apos = false;
		public boolean Aneg = false;
		public boolean Bpos = false;
		public boolean Bneg = false;
		public boolean ABpos = false;
		public boolean ABneg = false;
		public boolean PlayerOn1to1 = false;
		public boolean PlayerOn1to1_2 = false;
		public boolean PlayerOn1to1_3 = false;
		public boolean PlayerOn1to1_4 = false;
		public boolean PlayerOn1to1_5 = false;
		public boolean PlayerOn1to1_6 = false;
		public boolean PlayerOn1to1_7 = false;
		public boolean PlayerOn1to1_8 = false;
		public boolean PlayerOn1to1_9 = false;
		public boolean PlayerOn1to1_10 = false;
		public boolean PlayerOn1to1_11 = false;
		public boolean scp059infected1 = false;
		public boolean scp059infected0 = false;
		public boolean fear = false;
		public boolean nuclear = false;
		public boolean blackh = false;

		public void syncPlayerVariables(Entity entity) {
			if (entity instanceof ServerPlayerEntity)
				ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) entity),
						new PlayerVariablesSyncMessage(this));
		}
	}

	@SubscribeEvent
	public void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
		if (!event.getPlayer().world.isRemote())
			((PlayerVariables) event.getPlayer().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()))
					.syncPlayerVariables(event.getPlayer());
	}

	@SubscribeEvent
	public void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
		if (!event.getPlayer().world.isRemote())
			((PlayerVariables) event.getPlayer().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()))
					.syncPlayerVariables(event.getPlayer());
	}

	@SubscribeEvent
	public void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!event.getPlayer().world.isRemote())
			((PlayerVariables) event.getPlayer().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()))
					.syncPlayerVariables(event.getPlayer());
	}

	@SubscribeEvent
	public void clonePlayer(PlayerEvent.Clone event) {
		PlayerVariables original = ((PlayerVariables) event.getOriginal().getCapability(PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new PlayerVariables()));
		PlayerVariables clone = ((PlayerVariables) event.getEntity().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()));
		if (!event.isWasDeath()) {
			clone.Opos = original.Opos;
			clone.Oneg = original.Oneg;
			clone.Apos = original.Apos;
			clone.Aneg = original.Aneg;
			clone.Bpos = original.Bpos;
			clone.Bneg = original.Bneg;
			clone.ABpos = original.ABpos;
			clone.ABneg = original.ABneg;
			clone.PlayerOn1to1 = original.PlayerOn1to1;
			clone.PlayerOn1to1_2 = original.PlayerOn1to1_2;
			clone.PlayerOn1to1_3 = original.PlayerOn1to1_3;
			clone.PlayerOn1to1_4 = original.PlayerOn1to1_4;
			clone.PlayerOn1to1_5 = original.PlayerOn1to1_5;
			clone.PlayerOn1to1_6 = original.PlayerOn1to1_6;
			clone.PlayerOn1to1_7 = original.PlayerOn1to1_7;
			clone.PlayerOn1to1_8 = original.PlayerOn1to1_8;
			clone.PlayerOn1to1_9 = original.PlayerOn1to1_9;
			clone.PlayerOn1to1_10 = original.PlayerOn1to1_10;
			clone.PlayerOn1to1_11 = original.PlayerOn1to1_11;
			clone.scp059infected1 = original.scp059infected1;
			clone.scp059infected0 = original.scp059infected0;
			clone.fear = original.fear;
			clone.nuclear = original.nuclear;
			clone.blackh = original.blackh;
		}
	}

	public static class PlayerVariablesSyncMessage {
		public PlayerVariables data;

		public PlayerVariablesSyncMessage(PacketBuffer buffer) {
			this.data = new PlayerVariables();
			new PlayerVariablesStorage().readNBT(null, this.data, null, buffer.readCompoundTag());
		}

		public PlayerVariablesSyncMessage(PlayerVariables data) {
			this.data = data;
		}

		public static void buffer(PlayerVariablesSyncMessage message, PacketBuffer buffer) {
			buffer.writeCompoundTag((CompoundNBT) new PlayerVariablesStorage().writeNBT(null, message.data, null));
		}

		public static void handler(PlayerVariablesSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				if (!context.getDirection().getReceptionSide().isServer()) {
					PlayerVariables variables = ((PlayerVariables) Minecraft.getInstance().player.getCapability(PLAYER_VARIABLES_CAPABILITY, null)
							.orElse(new PlayerVariables()));
					variables.Opos = message.data.Opos;
					variables.Oneg = message.data.Oneg;
					variables.Apos = message.data.Apos;
					variables.Aneg = message.data.Aneg;
					variables.Bpos = message.data.Bpos;
					variables.Bneg = message.data.Bneg;
					variables.ABpos = message.data.ABpos;
					variables.ABneg = message.data.ABneg;
					variables.PlayerOn1to1 = message.data.PlayerOn1to1;
					variables.PlayerOn1to1_2 = message.data.PlayerOn1to1_2;
					variables.PlayerOn1to1_3 = message.data.PlayerOn1to1_3;
					variables.PlayerOn1to1_4 = message.data.PlayerOn1to1_4;
					variables.PlayerOn1to1_5 = message.data.PlayerOn1to1_5;
					variables.PlayerOn1to1_6 = message.data.PlayerOn1to1_6;
					variables.PlayerOn1to1_7 = message.data.PlayerOn1to1_7;
					variables.PlayerOn1to1_8 = message.data.PlayerOn1to1_8;
					variables.PlayerOn1to1_9 = message.data.PlayerOn1to1_9;
					variables.PlayerOn1to1_10 = message.data.PlayerOn1to1_10;
					variables.PlayerOn1to1_11 = message.data.PlayerOn1to1_11;
					variables.scp059infected1 = message.data.scp059infected1;
					variables.scp059infected0 = message.data.scp059infected0;
					variables.fear = message.data.fear;
					variables.nuclear = message.data.nuclear;
					variables.blackh = message.data.blackh;
				}
			});
			context.setPacketHandled(true);
		}
	}
}
