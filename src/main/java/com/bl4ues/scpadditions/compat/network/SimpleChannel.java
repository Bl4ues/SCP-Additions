package com.bl4ues.scpadditions.compat.network;

import java.util.*;
import java.util.function.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class SimpleChannel {
    private static final List<SimpleChannel> CHANNELS = new ArrayList<>();
    private final ResourceLocation channelId;
    private final String version;
    private final CustomPacketPayload.Type<Envelope> payloadType;
    private final Map<Integer, Registration<?>> byId = new LinkedHashMap<>();
    private final Map<Class<?>, Registration<?>> byClass = new LinkedHashMap<>();
    private boolean commonRegistered, clientRegistered;
    private final StreamCodec<RegistryFriendlyByteBuf, Envelope> codec = new StreamCodec<>() {
        @Override public Envelope decode(RegistryFriendlyByteBuf buffer) {
            int id = buffer.readVarInt(); Registration<?> registration = require(id);
            return new Envelope(SimpleChannel.this, id, registration.decode(buffer));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, Envelope envelope) {
            buffer.writeVarInt(envelope.messageId); require(envelope.messageId).encode(envelope.message, buffer);
        }
    };
    SimpleChannel(ResourceLocation channelId, String version) {
        this.channelId=channelId; this.version=version; this.payloadType=new CustomPacketPayload.Type<>(channelId);
        synchronized (CHANNELS) { CHANNELS.add(this); }
    }
    public synchronized <T> void registerMessage(int id, Class<T> type,
            BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf,T> decoder,
            BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
        Registration<T> registration = new Registration<>(id,type,encoder,decoder,handler);
        byId.put(id,registration); byClass.put(type,registration);
    }
    public void send(PacketDistributor.PacketTarget target, Object message) {
        Envelope payload=envelope(message);
        switch (target.kind()) {
            case PLAYER -> ServerPlayNetworking.send((ServerPlayer)target.value(), payload);
            case ALL -> {
                ServerPlayer player=(ServerPlayer)firstServerPlayer();
                if (player != null) for (ServerPlayer targetPlayer: PlayerLookup.all(player.server)) ServerPlayNetworking.send(targetPlayer,payload);
            }
            case DIMENSION -> {
                @SuppressWarnings("unchecked") ResourceKey<Level> key=(ResourceKey<Level>)target.value();
                ServerPlayer player=(ServerPlayer)firstServerPlayer();
                if (player != null) { ServerLevel level=player.server.getLevel(key); if (level != null) for(ServerPlayer p:PlayerLookup.world(level)) ServerPlayNetworking.send(p,payload); }
            }
            case TRACKING_ENTITY -> { for(ServerPlayer p:PlayerLookup.tracking((Entity)target.value())) ServerPlayNetworking.send(p,payload); }
            case TRACKING_ENTITY_AND_SELF -> {
                Entity entity=(Entity)target.value(); for(ServerPlayer p:PlayerLookup.tracking(entity)) ServerPlayNetworking.send(p,payload);
                if(entity instanceof ServerPlayer self) ServerPlayNetworking.send(self,payload);
            }
        }
    }
    private Object firstServerPlayer() {
        for (ServerPlayer player : net.mcreator.scpadditions.fabric.FabricServerContext.players()) return player;
        return null;
    }
    public void sendToServer(Object message) { ClientPlayNetworking.send(envelope(message)); }
    private Envelope envelope(Object message) { Registration<?> registration=byClass.get(message.getClass()); if(registration==null) throw new IllegalStateException("Unregistered packet "+message.getClass()); return new Envelope(this,registration.id,message); }
    private Registration<?> require(int id) { Registration<?> registration=byId.get(id); if(registration==null) throw new IllegalStateException("Unknown packet "+id+" on "+channelId); return registration; }
    public void registerPayload(net.neoforged.neoforge.network.registration.PayloadRegistrar ignored) { registerCommon(); }
    public synchronized void registerCommon() {
        if(commonRegistered) return; commonRegistered=true;
        PayloadTypeRegistry.playC2S().register(payloadType,codec);
        PayloadTypeRegistry.playS2C().register(payloadType,codec);
        ServerPlayNetworking.registerGlobalReceiver(payloadType,(payload,context)->require(payload.messageId).handle(payload.message,new NetworkEvent.Context(context.server(),context.player(),true)));
    }
    public synchronized void registerClient() {
        if(clientRegistered) return; clientRegistered=true;
        ClientPlayNetworking.registerGlobalReceiver(payloadType,(payload,context)->require(payload.messageId).handle(payload.message,new NetworkEvent.Context(context.client(),null,false)));
    }
    public static void registerAllCommon() { synchronized(CHANNELS){ CHANNELS.forEach(SimpleChannel::registerCommon); } }
    public static void registerAllClient() { synchronized(CHANNELS){ CHANNELS.forEach(SimpleChannel::registerClient); } }
    String version() { return version; }
    static List<SimpleChannel> channels() { synchronized(CHANNELS){ return List.copyOf(CHANNELS); } }
    public static final class Envelope implements CustomPacketPayload {
        private final SimpleChannel channel; private final int messageId; private final Object message;
        private Envelope(SimpleChannel channel,int id,Object message){this.channel=channel;this.messageId=id;this.message=message;}
        @Override public Type<? extends CustomPacketPayload> type(){return channel.payloadType;}
    }
    private static final class Registration<T> {
        final int id; final Class<T> type; final BiConsumer<T,FriendlyByteBuf> encoder; final Function<FriendlyByteBuf,T> decoder; final BiConsumer<T,Supplier<NetworkEvent.Context>> handler;
        Registration(int id,Class<T> type,BiConsumer<T,FriendlyByteBuf> encoder,Function<FriendlyByteBuf,T> decoder,BiConsumer<T,Supplier<NetworkEvent.Context>> handler){this.id=id;this.type=type;this.encoder=encoder;this.decoder=decoder;this.handler=handler;}
        void encode(Object message,FriendlyByteBuf buffer){encoder.accept(type.cast(message),buffer);} T decode(FriendlyByteBuf buffer){return decoder.apply(buffer);} void handle(Object message,NetworkEvent.Context context){handler.accept(type.cast(message),()->context);}
    }
}
