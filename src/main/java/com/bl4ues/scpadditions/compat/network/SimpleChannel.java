package com.bl4ues.scpadditions.compat.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Compatibility implementation of Forge 1.20.1 SimpleChannel backed by a
 * single bidirectional NeoForge custom payload per legacy channel.
 */
public final class SimpleChannel {
    private static final List<SimpleChannel> CHANNELS = new ArrayList<>();

    private final ResourceLocation channelId;
    private final String version;
    private final CustomPacketPayload.Type<Envelope> payloadType;
    private final Map<Integer, Registration<?>> registrationsById = new LinkedHashMap<>();
    private final Map<Class<?>, Registration<?>> registrationsByClass = new LinkedHashMap<>();

    private final StreamCodec<RegistryFriendlyByteBuf, Envelope> codec =
            new StreamCodec<>() {
                @Override
                public Envelope decode(RegistryFriendlyByteBuf buffer) {
                    int messageId = buffer.readVarInt();
                    Registration<?> registration = registrationsById.get(messageId);
                    if (registration == null) {
                        throw new IllegalStateException("Unknown legacy packet " + messageId
                                + " on channel " + channelId);
                    }
                    Object message = registration.decode(buffer);
                    return new Envelope(SimpleChannel.this, messageId, message);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, Envelope envelope) {
                    if (envelope.channel != SimpleChannel.this) {
                        throw new IllegalArgumentException("Payload belongs to another channel");
                    }
                    buffer.writeVarInt(envelope.messageId);
                    Registration<?> registration = registrationsById.get(envelope.messageId);
                    if (registration == null) {
                        throw new IllegalStateException("Unknown legacy packet " + envelope.messageId
                                + " on channel " + channelId);
                    }
                    registration.encode(envelope.message, buffer);
                }
            };

    SimpleChannel(ResourceLocation channelId, String version) {
        this.channelId = Objects.requireNonNull(channelId);
        this.version = Objects.requireNonNull(version);
        this.payloadType = new CustomPacketPayload.Type<>(channelId);
        synchronized (CHANNELS) {
            CHANNELS.add(this);
        }
    }

    public synchronized <T> void registerMessage(
            int id,
            Class<T> messageType,
            BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
        if (registrationsById.containsKey(id)) {
            throw new IllegalStateException("Duplicate packet id " + id + " on " + channelId);
        }
        if (registrationsByClass.containsKey(messageType)) {
            throw new IllegalStateException("Duplicate packet type " + messageType.getName()
                    + " on " + channelId);
        }
        Registration<T> registration = new Registration<>(
                id, messageType, encoder, decoder, handler);
        registrationsById.put(id, registration);
        registrationsByClass.put(messageType, registration);
    }

    public void send(PacketDistributor.PacketTarget target, Object message) {
        Envelope envelope = envelope(message);
        switch (target.kind()) {
            case PLAYER -> net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                    (ServerPlayer) target.value(), envelope);
            case ALL -> net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(envelope);
            case DIMENSION -> sendToDimension(target, envelope);
            case TRACKING_ENTITY_AND_SELF ->
                    net.neoforged.neoforge.network.PacketDistributor
                            .sendToPlayersTrackingEntityAndSelf((Entity) target.value(), envelope);
            case TRACKING_ENTITY ->
                    net.neoforged.neoforge.network.PacketDistributor
                            .sendToPlayersTrackingEntity((Entity) target.value(), envelope);
        }
    }

    public void sendToServer(Object message) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(envelope(message));
    }

    private void sendToDimension(PacketDistributor.PacketTarget target, Envelope envelope) {
        @SuppressWarnings("unchecked")
        ResourceKey<Level> dimension = (ResourceKey<Level>) target.value();
        MinecraftServer server = Objects.requireNonNull(
                ServerLifecycleHooks.getCurrentServer(),
                "No active server for dimension packet");
        ServerLevel level = Objects.requireNonNull(
                server.getLevel(dimension),
                "Unknown target dimension " + dimension.location());
        net.neoforged.neoforge.network.PacketDistributor
                .sendToPlayersInDimension(level, envelope);
    }

    private Envelope envelope(Object message) {
        Registration<?> registration = registrationsByClass.get(message.getClass());
        if (registration == null) {
            throw new IllegalStateException("Unregistered packet " + message.getClass().getName()
                    + " on channel " + channelId);
        }
        return new Envelope(this, registration.id, message);
    }

    void registerPayload(PayloadRegistrar registrar) {
        registrar.playBidirectional(payloadType, codec, this::handle);
    }

    private void handle(Envelope envelope, IPayloadContext payloadContext) {
        Registration<?> registration = registrationsById.get(envelope.messageId);
        if (registration == null) {
            throw new IllegalStateException("Unknown legacy packet " + envelope.messageId
                    + " on channel " + channelId);
        }
        registration.handle(envelope.message, payloadContext);
    }

    String version() {
        return version;
    }

    static List<SimpleChannel> channels() {
        synchronized (CHANNELS) {
            return List.copyOf(CHANNELS);
        }
    }

    public static final class Envelope implements CustomPacketPayload {
        private final SimpleChannel channel;
        private final int messageId;
        private final Object message;

        private Envelope(SimpleChannel channel, int messageId, Object message) {
            this.channel = channel;
            this.messageId = messageId;
            this.message = message;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return channel.payloadType;
        }
    }

    private static final class Registration<T> {
        private final int id;
        private final Class<T> type;
        private final BiConsumer<T, FriendlyByteBuf> encoder;
        private final Function<FriendlyByteBuf, T> decoder;
        private final BiConsumer<T, Supplier<NetworkEvent.Context>> handler;

        private Registration(
                int id,
                Class<T> type,
                BiConsumer<T, FriendlyByteBuf> encoder,
                Function<FriendlyByteBuf, T> decoder,
                BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
            this.id = id;
            this.type = type;
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
        }

        private void encode(Object message, FriendlyByteBuf buffer) {
            encoder.accept(type.cast(message), buffer);
        }

        private T decode(FriendlyByteBuf buffer) {
            return decoder.apply(buffer);
        }

        private void handle(Object message, IPayloadContext context) {
            NetworkEvent.Context legacyContext = new NetworkEvent.Context(context);
            handler.accept(type.cast(message), () -> legacyContext);
        }
    }
}
