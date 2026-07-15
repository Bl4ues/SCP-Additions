package net.mcreator.scpadditions.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

import java.util.function.Supplier;

/** Plays the SCP-1176 music head-relative for only the affected player. */
public final class Scp1176MusicPacket {
    public static void encode(Scp1176MusicPacket message, FriendlyByteBuf buffer) {
    }

    public static Scp1176MusicPacket decode(FriendlyByteBuf buffer) {
        return new Scp1176MusicPacket();
    }

    public static void handle(Scp1176MusicPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> Scp1176MusicPacket::playClient));
        context.setPacketHandled(true);
    }

    private static void playClient() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(ScpAdditionsModSounds.SCP1176.get(), 1.0F, 1.0F));
    }
}
