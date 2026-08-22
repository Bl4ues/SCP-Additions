package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Dedicated channel so SCP-1576 does not consume or reorder legacy packet IDs. */
public final class Scp1576Network {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "scp1576"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static boolean registered;

    private Scp1576Network() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, Scp1576StatePacket.class,
                Scp1576StatePacket::encode,
                Scp1576StatePacket::decode,
                Scp1576StatePacket::handle);
    }

    public static void sendAll(Scp1576StatePacket packet) {
        if (packet == null) return;
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
