package net.mcreator.scpadditions.network;

import net.neoforged.fml.common.EventBusSubscriber;

import com.bl4ues.scpadditions.compat.network.PacketDistributor;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.bl4ues.scpadditions.compat.LazyOptional;
import net.neoforged.neoforge.common.util.FakePlayer;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.function.Supplier;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ScpAdditionsModVariables {
	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		ScpAdditionsMod.addNetworkMessage(SavedDataSyncMessage.class, SavedDataSyncMessage::buffer, SavedDataSyncMessage::new, SavedDataSyncMessage::handler);
		ScpAdditionsMod.addNetworkMessage(PlayerVariablesSyncMessage.class, PlayerVariablesSyncMessage::buffer, PlayerVariablesSyncMessage::new, PlayerVariablesSyncMessage::handler);
	}


	@EventBusSubscriber
	public static class EventBusVariableHandlers {
		@SubscribeEvent
		public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
			if (!event.getEntity().level().isClientSide())
				((PlayerVariables) ScpAdditionsModVariables.getPlayerVariables(event.getEntity()).orElse(new PlayerVariables())).syncPlayerVariables(event.getEntity());
		}

		@SubscribeEvent
		public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
			if (!event.getEntity().level().isClientSide())
				((PlayerVariables) ScpAdditionsModVariables.getPlayerVariables(event.getEntity()).orElse(new PlayerVariables())).syncPlayerVariables(event.getEntity());
		}

		@SubscribeEvent
		public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
			if (!event.getEntity().level().isClientSide())
				((PlayerVariables) ScpAdditionsModVariables.getPlayerVariables(event.getEntity()).orElse(new PlayerVariables())).syncPlayerVariables(event.getEntity());
		}

		@SubscribeEvent
		public static void clonePlayer(PlayerEvent.Clone event) {
			PlayerVariables original = ((PlayerVariables) ScpAdditionsModVariables.getPlayerVariables(event.getOriginal()).orElse(new PlayerVariables()));
			PlayerVariables clone = ((PlayerVariables) ScpAdditionsModVariables.getPlayerVariables(event.getEntity()).orElse(new PlayerVariables()));
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
				clone.scp914Skin = original.scp914Skin;
			}
		}

		@SubscribeEvent
		public static void onStartTracking(PlayerEvent.StartTracking event) {
			if (event.getEntity() instanceof ServerPlayer observer
					&& event.getTarget() instanceof Player target
					&& !observer.level().isClientSide()) {
				ScpAdditionsModVariables.getPlayerVariables(target).ifPresent(variables ->
						ScpAdditionsMod.PACKET_HANDLER.send(
								PacketDistributor.PLAYER.with(() -> observer),
								new PlayerVariablesSyncMessage(target.getId(), variables)));
			}
		}

		@SubscribeEvent
		public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
			if (!event.getEntity().level().isClientSide()) {
				SavedData mapdata = MapVariables.get(event.getEntity().level());
				SavedData worlddata = WorldVariables.get(event.getEntity().level());
				if (mapdata != null)
					ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new SavedDataSyncMessage(0, mapdata));
				if (worlddata != null)
					ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new SavedDataSyncMessage(1, worlddata));
			}
		}

		@SubscribeEvent
		public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
			if (!event.getEntity().level().isClientSide()) {
				SavedData worlddata = WorldVariables.get(event.getEntity().level());
				if (worlddata != null)
					ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new SavedDataSyncMessage(1, worlddata));
			}
		}
	}

	public static class WorldVariables extends SavedData {
		public static final String DATA_NAME = "scp_additions_worldvars";
		public double Scp294stock = 0;
		public double coinslot = 0;

		public static final SavedData.Factory<WorldVariables> FACTORY =
				new SavedData.Factory<>(WorldVariables::new, WorldVariables::load);

		public static WorldVariables load(CompoundTag tag, HolderLookup.Provider registries) {
			WorldVariables data = new WorldVariables();
			data.read(tag);
			return data;
		}

		public void read(CompoundTag nbt) {
			Scp294stock = nbt.getDouble("Scp294stock");
			coinslot = nbt.getDouble("coinslot");
		}

		@Override
		public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
			nbt.putDouble("Scp294stock", Scp294stock);
			nbt.putDouble("coinslot", coinslot);
			return nbt;
		}

		public void syncData(LevelAccessor world) {
			this.setDirty();
			if (world instanceof Level level && !level.isClientSide())
				ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.DIMENSION.with(level::dimension), new SavedDataSyncMessage(1, this));
		}

		static WorldVariables clientSide = new WorldVariables();

		public static WorldVariables get(LevelAccessor world) {
			if (world instanceof ServerLevel level) {
				return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
			} else {
				return clientSide;
			}
		}
	}

	public static class MapVariables extends SavedData {
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

		public static final SavedData.Factory<MapVariables> FACTORY =
				new SavedData.Factory<>(MapVariables::new, MapVariables::load);

		public static MapVariables load(CompoundTag tag, HolderLookup.Provider registries) {
			MapVariables data = new MapVariables();
			data.read(tag);
			return data;
		}

		public void read(CompoundTag nbt) {
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
		public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
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

		public void syncData(LevelAccessor world) {
			this.setDirty();
			if (world instanceof Level && !world.isClientSide())
				ScpAdditionsMod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new SavedDataSyncMessage(0, this));
		}

		static MapVariables clientSide = new MapVariables();

		public static MapVariables get(LevelAccessor world) {
			if (world instanceof ServerLevelAccessor serverLevelAcc) {
				return serverLevelAcc.getLevel().getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
			} else {
				return clientSide;
			}
		}
	}

	public static class SavedDataSyncMessage {
		private final int type;
		private SavedData data;

		public SavedDataSyncMessage(FriendlyByteBuf buffer) {
			this.type = buffer.readInt();
			CompoundTag nbt = buffer.readNbt();
			if (nbt != null) {
				this.data = this.type == 0 ? new MapVariables() : new WorldVariables();
				if (this.data instanceof MapVariables mapVariables)
					mapVariables.read(nbt);
				else if (this.data instanceof WorldVariables worldVariables)
					worldVariables.read(nbt);
			}
		}

		public SavedDataSyncMessage(int type, SavedData data) {
			this.type = type;
			this.data = data;
		}

		public static void buffer(SavedDataSyncMessage message, FriendlyByteBuf buffer) {
			buffer.writeInt(message.type);
			if (message.data != null) {
				if (!(buffer instanceof RegistryFriendlyByteBuf registryBuffer)) {
					throw new IllegalStateException("Saved data sync requires a registry-aware buffer");
				}
				buffer.writeNbt(message.data.save(
						new CompoundTag(), registryBuffer.registryAccess()));
			}
		}

		public static void handler(SavedDataSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				if (!context.getDirection().getReceptionSide().isServer() && message.data != null) {
					if (message.type == 0)
						MapVariables.clientSide = (MapVariables) message.data;
					else
						WorldVariables.clientSide = (WorldVariables) message.data;
				}
			});
			context.setPacketHandled(true);
		}
	}

	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
			DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
					ScpAdditionsMod.MODID);

	public static final Supplier<AttachmentType<PlayerVariables>> PLAYER_VARIABLES_ATTACHMENT =
			ATTACHMENTS.register("player_variables", () -> AttachmentType
					.builder(PlayerVariables::new)
					.serialize(new IAttachmentSerializer<CompoundTag, PlayerVariables>() {
						@Override
						public PlayerVariables read(IAttachmentHolder holder,
								CompoundTag tag, HolderLookup.Provider provider) {
							PlayerVariables variables = new PlayerVariables();
							variables.readNBT(tag);
							return variables;
						}

						@Override
						public CompoundTag write(PlayerVariables variables,
								HolderLookup.Provider provider) {
							return (CompoundTag) variables.writeNBT();
						}
					})
					.build());

	public static LazyOptional<PlayerVariables> getPlayerVariables(Entity entity) {
		if (!(entity instanceof Player player) || entity instanceof FakePlayer) {
			return LazyOptional.empty();
		}
		return LazyOptional.of(() -> player.getData(PLAYER_VARIABLES_ATTACHMENT));
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
		public String scp914Skin = "";

		public void syncPlayerVariables(Entity entity) {
			if (entity instanceof ServerPlayer)
				ScpAdditionsMod.PACKET_HANDLER.send(
						PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
						new PlayerVariablesSyncMessage(entity.getId(), this));
		}

		public Tag writeNBT() {
			CompoundTag nbt = new CompoundTag();
			nbt.putBoolean("Opos", Opos);
			nbt.putBoolean("Oneg", Oneg);
			nbt.putBoolean("Apos", Apos);
			nbt.putBoolean("Aneg", Aneg);
			nbt.putBoolean("Bpos", Bpos);
			nbt.putBoolean("Bneg", Bneg);
			nbt.putBoolean("ABpos", ABpos);
			nbt.putBoolean("ABneg", ABneg);
			nbt.putBoolean("PlayerOn1to1", PlayerOn1to1);
			nbt.putBoolean("PlayerOn1to1_2", PlayerOn1to1_2);
			nbt.putBoolean("PlayerOn1to1_3", PlayerOn1to1_3);
			nbt.putBoolean("PlayerOn1to1_4", PlayerOn1to1_4);
			nbt.putBoolean("PlayerOn1to1_5", PlayerOn1to1_5);
			nbt.putBoolean("PlayerOn1to1_6", PlayerOn1to1_6);
			nbt.putBoolean("PlayerOn1to1_7", PlayerOn1to1_7);
			nbt.putBoolean("PlayerOn1to1_8", PlayerOn1to1_8);
			nbt.putBoolean("PlayerOn1to1_9", PlayerOn1to1_9);
			nbt.putBoolean("PlayerOn1to1_10", PlayerOn1to1_10);
			nbt.putBoolean("PlayerOn1to1_11", PlayerOn1to1_11);
			nbt.putString("scp914Skin", scp914Skin == null ? "" : scp914Skin);
			return nbt;
		}

		public void readNBT(Tag Tag) {
			CompoundTag nbt = (CompoundTag) Tag;
			Opos = nbt.getBoolean("Opos");
			Oneg = nbt.getBoolean("Oneg");
			Apos = nbt.getBoolean("Apos");
			Aneg = nbt.getBoolean("Aneg");
			Bpos = nbt.getBoolean("Bpos");
			Bneg = nbt.getBoolean("Bneg");
			ABpos = nbt.getBoolean("ABpos");
			ABneg = nbt.getBoolean("ABneg");
			PlayerOn1to1 = nbt.getBoolean("PlayerOn1to1");
			PlayerOn1to1_2 = nbt.getBoolean("PlayerOn1to1_2");
			PlayerOn1to1_3 = nbt.getBoolean("PlayerOn1to1_3");
			PlayerOn1to1_4 = nbt.getBoolean("PlayerOn1to1_4");
			PlayerOn1to1_5 = nbt.getBoolean("PlayerOn1to1_5");
			PlayerOn1to1_6 = nbt.getBoolean("PlayerOn1to1_6");
			PlayerOn1to1_7 = nbt.getBoolean("PlayerOn1to1_7");
			PlayerOn1to1_8 = nbt.getBoolean("PlayerOn1to1_8");
			PlayerOn1to1_9 = nbt.getBoolean("PlayerOn1to1_9");
			PlayerOn1to1_10 = nbt.getBoolean("PlayerOn1to1_10");
			PlayerOn1to1_11 = nbt.getBoolean("PlayerOn1to1_11");
			scp914Skin = nbt.getString("scp914Skin");
		}
	}

	public static class PlayerVariablesSyncMessage {
		private final int entityId;
		private final PlayerVariables data;

		public PlayerVariablesSyncMessage(FriendlyByteBuf buffer) {
			this.entityId = buffer.readVarInt();
			this.data = new PlayerVariables();
			CompoundTag nbt = buffer.readNbt();
			if (nbt != null)
				this.data.readNBT(nbt);
		}

		public PlayerVariablesSyncMessage(int entityId, PlayerVariables data) {
			this.entityId = entityId;
			this.data = data;
		}

		public static void buffer(PlayerVariablesSyncMessage message, FriendlyByteBuf buffer) {
			buffer.writeVarInt(message.entityId);
			buffer.writeNbt((CompoundTag) message.data.writeNBT());
		}

		public static void handler(PlayerVariablesSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				if (!context.getDirection().getReceptionSide().isServer()
						&& Minecraft.getInstance().level != null) {
					Entity target = Minecraft.getInstance().level.getEntity(message.entityId);
					if (target instanceof Player player) {
						PlayerVariables variables = ScpAdditionsModVariables.getPlayerVariables(player)
								.orElse(new PlayerVariables());
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
						variables.scp914Skin = message.data.scp914Skin;
					}
				}
			});
			context.setPacketHandled(true);
		}
	}
}
