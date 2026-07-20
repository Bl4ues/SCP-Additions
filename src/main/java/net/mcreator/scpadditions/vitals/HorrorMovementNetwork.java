package net.mcreator.scpadditions.vitals;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;
import com.bl4ues.scpadditions.compat.network.NetworkRegistry;
import com.bl4ues.scpadditions.compat.network.SimpleChannel;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Small dedicated channel for authoritative horror-sprint input state. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = EventBusSubscriber.Bus.MOD)
public final class HorrorMovementNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ScpAdditionsMod.MODID, "horror_movement"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static final Map<UUID, Boolean> SPRINT_INPUT = new ConcurrentHashMap<>();
    private static boolean registered;

    private HorrorMovementNetwork() {
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(HorrorMovementNetwork::register);
    }

    private static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(0, SprintInputPacket.class,
                SprintInputPacket::encode,
                SprintInputPacket::decode,
                SprintInputPacket::handle);
    }

    public static void sendSprintInput(boolean sprinting) {
        CHANNEL.sendToServer(new SprintInputPacket(sprinting));
    }

    public static boolean isSprintRequested(ServerPlayer player) {
        return player != null && Boolean.TRUE.equals(SPRINT_INPUT.get(player.getUUID()));
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            SPRINT_INPUT.remove(player.getUUID());
        }
    }

    private record SprintInputPacket(boolean sprinting) {
        private static void encode(SprintInputPacket packet, FriendlyByteBuf buffer) {
            buffer.writeBoolean(packet.sprinting);
        }

        private static SprintInputPacket decode(FriendlyByteBuf buffer) {
            return new SprintInputPacket(buffer.readBoolean());
        }

        private static void handle(SprintInputPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                if (packet.sprinting) {
                    SPRINT_INPUT.put(player.getUUID(), true);
                } else {
                    SPRINT_INPUT.remove(player.getUUID());
                }
            });
            context.setPacketHandled(true);
        }
    }
}
