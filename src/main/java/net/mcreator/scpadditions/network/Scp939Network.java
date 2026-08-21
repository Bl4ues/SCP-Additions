package net.mcreator.scpadditions.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.scp939.Scp939BreathSystem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Network facade for SCP-939 interactions. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Scp939Network {
    private static final Map<UUID, PinSnapshot> PIN_STATES =
            new ConcurrentHashMap<>();
    private static boolean registered;

    private Scp939Network() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Scp939Network::register);
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpAdditionsMod.addNetworkMessage(Scp939StatePacket.class,
                Scp939StatePacket::encode, Scp939StatePacket::decode,
                Scp939StatePacket::handle);
        ScpAdditionsMod.addNetworkMessage(Scp939InputPacket.class,
                Scp939InputPacket::encode, Scp939InputPacket::decode,
                Scp939InputPacket::handle);
    }

    public static void sync(ServerPlayer player) {
        if (player == null) return;
        PinSnapshot pin = PIN_STATES.getOrDefault(player.getUUID(),
                PinSnapshot.EMPTY);
        send(player, pin);
    }

    public static void sendPinState(ServerPlayer player, boolean pinned,
            int progress, int failures, int expectedKey, int windowTicks) {
        if (player == null) return;
        PinSnapshot pin = pinned
                ? new PinSnapshot(true, progress, failures, expectedKey,
                        windowTicks)
                : PinSnapshot.EMPTY;
        if (pinned) PIN_STATES.put(player.getUUID(), pin);
        else PIN_STATES.remove(player.getUUID());
        send(player, pin);
    }

    public static void forgetPlayer(UUID playerId) {
        if (playerId != null) PIN_STATES.remove(playerId);
    }

    private static void send(ServerPlayer player, PinSnapshot pin) {
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp939StatePacket(Scp939BreathSystem.isActive(player),
                        Scp939BreathSystem.reserveFraction(player),
                        Scp939BreathSystem.isHolding(player), pin.pinned,
                        pin.progress, pin.failures, pin.expectedKey,
                        pin.windowTicks));
    }

    public static void sendInput(int action, boolean pressed) {
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new Scp939InputPacket(action, pressed));
    }

    private record PinSnapshot(boolean pinned, int progress, int failures,
            int expectedKey, int windowTicks) {
        private static final PinSnapshot EMPTY =
                new PinSnapshot(false, 0, 0, 0, 0);
    }
}
