package net.mcreator.scpadditions.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.scp939.Scp939BreathSystem;
import net.mcreator.scpadditions.scp939.Scp939PinSystem;

/** Network facade for SCP-939 interactions. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Scp939Network {
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
        Scp939PinSystem.Snapshot pin = Scp939PinSystem.snapshot(player);
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp939StatePacket(Scp939BreathSystem.isActive(player),
                        Scp939BreathSystem.reserveFraction(player),
                        Scp939BreathSystem.isHolding(player), pin.pinned(),
                        pin.progress(), pin.failures(), pin.expectedKey(),
                        pin.windowTicks()));
    }

    public static void sendPinState(ServerPlayer player, boolean pinned,
            int progress, int failures, int expectedKey, int windowTicks) {
        if (player == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Scp939StatePacket(Scp939BreathSystem.isActive(player),
                        Scp939BreathSystem.reserveFraction(player),
                        Scp939BreathSystem.isHolding(player), pinned, progress,
                        failures, expectedKey, windowTicks));
    }

    public static void sendInput(int action, boolean pressed) {
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new Scp939InputPacket(action, pressed));
    }
}
