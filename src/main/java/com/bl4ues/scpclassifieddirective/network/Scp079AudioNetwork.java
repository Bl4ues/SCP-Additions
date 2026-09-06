package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079SpeakerCueSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Audio-only packets for SCP-079's remote host and facility Speaker perception. */
public final class Scp079AudioNetwork {
    private static boolean registered;

    private Scp079AudioNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(RemoteHostSound.class,
                RemoteHostSound::encode, RemoteHostSound::decode,
                RemoteHostSound::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SpeakerCue.class,
                SpeakerCue::encode, SpeakerCue::decode, SpeakerCue::handle);
    }

    public static void sendRemoteHostSound(ServerPlayer player,
            ResourceLocation sound, SoundSource source, float volume,
            float pitch) {
        if (player == null || sound == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new RemoteHostSound(sound,
                        source == null ? SoundSource.BLOCKS : source,
                        volume, pitch));
    }

    public static void sendSpeakerCue(MinecraftServer server,
            ResourceKey<Level> dimension, ResourceLocation sound,
            double x, double y, double z, float volume, float pitch) {
        if (server == null || dimension == null || sound == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.level().dimension().equals(dimension)) continue;
            ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SpeakerCue(sound, x, y, z, volume, pitch));
        }
    }

    public record RemoteHostSound(ResourceLocation sound, SoundSource source,
            float volume, float pitch) {
        private static void encode(RemoteHostSound message,
                FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.sound);
            buffer.writeEnum(message.source);
            buffer.writeFloat(message.volume);
            buffer.writeFloat(message.pitch);
        }

        private static RemoteHostSound decode(FriendlyByteBuf buffer) {
            return new RemoteHostSound(buffer.readResourceLocation(),
                    buffer.readEnum(SoundSource.class), buffer.readFloat(),
                    buffer.readFloat());
        }

        private static void handle(RemoteHostSound message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        Minecraft minecraft = Minecraft.getInstance();
                        if (minecraft.player == null) return;
                        float volume = Mth.clamp(message.volume, 0.0F, 4.0F);
                        float pitch = Mth.clamp(message.pitch, 0.05F, 2.0F);
                        minecraft.getSoundManager().play(new SimpleSoundInstance(
                                message.sound, message.source, volume, pitch,
                                RandomSource.create(), false, 0,
                                SoundInstance.Attenuation.NONE,
                                0.0D, 0.0D, 0.0D, true));
                    }));
            context.setPacketHandled(true);
        }
    }

    public record SpeakerCue(ResourceLocation sound, double x, double y,
            double z, float volume, float pitch) {
        private static void encode(SpeakerCue message, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.sound);
            buffer.writeDouble(message.x);
            buffer.writeDouble(message.y);
            buffer.writeDouble(message.z);
            buffer.writeFloat(message.volume);
            buffer.writeFloat(message.pitch);
        }

        private static SpeakerCue decode(FriendlyByteBuf buffer) {
            return new SpeakerCue(buffer.readResourceLocation(),
                    buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readFloat(), buffer.readFloat());
        }

        private static void handle(SpeakerCue message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> Minecraft.getInstance().getSoundManager().play(
                            new Scp079SpeakerCueSoundInstance(message.sound,
                                    message.x, message.y, message.z,
                                    Mth.clamp(message.volume, 0.0F, 4.0F),
                                    Mth.clamp(message.pitch, 0.05F, 2.0F)))));
            context.setPacketHandled(true);
        }
    }
}
