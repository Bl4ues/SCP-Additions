package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079SpeakerCueSoundInstance;
import com.bl4ues.scpclassifieddirective.facility.intercom.IntercomWorldSoundBridge;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Audio-only packets for SCP-079, facility Speakers, and Intercom capture. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Scp079AudioNetwork {
    private static boolean registered;

    private Scp079AudioNetwork() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Scp079ActivityPingNetwork.register();
            register();
        });
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(RemoteHostSound.class,
                RemoteHostSound::encode, RemoteHostSound::decode,
                RemoteHostSound::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SpeakerCue.class,
                SpeakerCue::encode, SpeakerCue::decode, SpeakerCue::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(IntercomSoundReport.class,
                IntercomSoundReport::encode, IntercomSoundReport::decode,
                IntercomSoundReport::handle);
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

    /** Reports client-only positional ambience such as fire crackle. */
    public static void reportIntercomSound(ResourceLocation sound,
            Vec3 position, float volume, float pitch) {
        if (sound == null || position == null
                || !Double.isFinite(position.x)
                || !Double.isFinite(position.y)
                || !Double.isFinite(position.z)
                || !Float.isFinite(volume) || !Float.isFinite(pitch)) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new IntercomSoundReport(sound, position.x, position.y,
                        position.z, volume, pitch));
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

    public record IntercomSoundReport(ResourceLocation sound, double x,
            double y, double z, float volume, float pitch) {
        private static void encode(IntercomSoundReport message,
                FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.sound);
            buffer.writeDouble(message.x);
            buffer.writeDouble(message.y);
            buffer.writeDouble(message.z);
            buffer.writeFloat(message.volume);
            buffer.writeFloat(message.pitch);
        }

        private static IntercomSoundReport decode(FriendlyByteBuf buffer) {
            return new IntercomSoundReport(buffer.readResourceLocation(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readFloat());
        }

        private static void handle(IntercomSoundReport message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer reporter = context.getSender();
                if (reporter == null || message.sound == null
                        || !Double.isFinite(message.x)
                        || !Double.isFinite(message.y)
                        || !Double.isFinite(message.z)
                        || !Float.isFinite(message.volume)
                        || !Float.isFinite(message.pitch)) return;
                IntercomWorldSoundBridge.relayClientSound(reporter,
                        message.sound, new Vec3(message.x, message.y, message.z),
                        Mth.clamp(message.volume, 0.0F, 4.0F),
                        Mth.clamp(message.pitch, 0.05F, 2.0F));
            });
            context.setPacketHandled(true);
        }
    }
}
