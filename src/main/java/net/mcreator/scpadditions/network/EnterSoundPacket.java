package net.mcreator.scpadditions.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

import java.util.function.Supplier;

public final class EnterSoundPacket {
    public static void encode(EnterSoundPacket message, FriendlyByteBuf buffer) {
    }

    public static EnterSoundPacket decode(FriendlyByteBuf buffer) {
        return new EnterSoundPacket();
    }

    public static void handle(EnterSoundPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> EnterSoundPacket::playClient));
        context.setPacketHandled(true);
    }

    private static void playClient() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(ScpAdditionsModSounds.ENTER.get(), 1.0F, 1.0F));
    }
}
