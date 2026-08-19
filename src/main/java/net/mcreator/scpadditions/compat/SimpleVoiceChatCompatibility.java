package net.mcreator.scpadditions.compat;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.death.DeathSpectateCoordinator;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional Simple Voice Chat bridge for SCP Additions' multiplayer death flow.
 *
 * Living players keep Simple Voice Chat's normal routing. Dead players are
 * isolated from living receivers, share a non-positional dead-only call, and
 * receive the exact voice-chat packets that reach the living player they are
 * currently watching. The watched player's own microphone is added explicitly,
 * since Simple Voice Chat normally never echoes a speaker back to themselves.
 */
@ForgeVoicechatPlugin
public final class SimpleVoiceChatCompatibility implements VoicechatPlugin {
    private static final String PLUGIN_ID = "scp_additions_death_voice";
    private static final int MIN_API_MAJOR = 2;
    private static final int MIN_API_MINOR = 6;
    private static final int MIN_API_PATCH = 20;
    private static final int MICROPHONE_PRIORITY = Integer.MAX_VALUE;
    private static final int ROUTING_PRIORITY = Integer.MIN_VALUE;
    private static final Pattern SEMVER = Pattern.compile(
            "(\\d+)\\.(\\d+)\\.(\\d+)");

    /**
     * API sends synchronously pass through the packet events again. Mark our own
     * forwards so the dead-receiver isolation rule does not cancel them or mirror
     * them recursively.
     */
    private static final ThreadLocal<Boolean> FORWARDING =
            ThreadLocal.withInitial(() -> false);

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        if (!supportedApiInstalled()) {
            ScpAdditionsMod.LOGGER.warn(
                    "Simple Voice Chat was detected, but SCP Additions death voice requires API 2.6.20 or newer; integration will remain disabled");
            return;
        }

        // Register once and consult the host-owned setting at packet time. This
        // allows the Config Center toggle to take effect immediately at runtime.
        registration.registerEvent(MicrophonePacketEvent.class,
                SimpleVoiceChatCompatibility::onMicrophone,
                MICROPHONE_PRIORITY);
        registration.registerEvent(EntitySoundPacketEvent.class,
                SimpleVoiceChatCompatibility::onEntitySound,
                ROUTING_PRIORITY);
        registration.registerEvent(LocationalSoundPacketEvent.class,
                SimpleVoiceChatCompatibility::onLocationalSound,
                ROUTING_PRIORITY);
        registration.registerEvent(StaticSoundPacketEvent.class,
                SimpleVoiceChatCompatibility::onStaticSound,
                ROUTING_PRIORITY);

        ScpAdditionsMod.LOGGER.info(
                "Registered Simple Voice Chat death/spectate integration");
    }

    private static void onMicrophone(MicrophonePacketEvent event) {
        if (!ModCompatibilityConfig.simpleVoiceChatEnabled() || forwarding()) return;
        ServerPlayer sender = minecraftPlayer(event.getSenderConnection());
        if (sender == null) return;

        VoicechatServerApi api = event.getVoicechat();
        if (DeathSpectateCoordinator.isDeadVoiceParticipant(sender)) {
            // Cancel first. Even if another part of the bridge fails, a dead
            // player's microphone must never fall through to living proximity.
            event.cancel();
            StaticSoundPacket packet;
            try {
                packet = event.getPacket().staticSoundPacketBuilder()
                        .channelId(deadCallChannel(sender.getUUID()))
                        .build();
            } catch (RuntimeException | LinkageError exception) {
                ScpAdditionsMod.LOGGER.error(
                        "Could not convert dead-player microphone audio for Simple Voice Chat",
                        exception);
                return;
            }

            for (ServerPlayer dead : DeathSpectateCoordinator
                    .deadVoiceParticipants(sender.server)) {
                if (dead.getUUID().equals(sender.getUUID())) continue;
                sendStatic(api, dead, packet);
            }
            return;
        }

        // The watched player does not hear their own microphone through normal
        // Simple Voice Chat routing, but a dead observer must. Feed it directly
        // as entity audio while preserving sender identity and packet sequence.
        List<ServerPlayer> observers = DeathSpectateCoordinator.observersOf(sender);
        if (observers.isEmpty()) return;

        EntitySoundPacket packet;
        try {
            packet = event.getPacket().entitySoundPacketBuilder()
                    .entityUuid(sender.getUUID())
                    .whispering(event.getPacket().isWhispering())
                    .build();
        } catch (RuntimeException | LinkageError exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Could not mirror the watched player's microphone through Simple Voice Chat",
                    exception);
            return;
        }
        for (ServerPlayer observer : observers) {
            sendEntity(api, observer, packet);
        }
    }

    private static void onEntitySound(EntitySoundPacketEvent event) {
        if (!ModCompatibilityConfig.simpleVoiceChatEnabled() || forwarding()) return;
        ServerPlayer receiver = minecraftPlayer(event.getReceiverConnection());
        if (receiver == null) return;

        if (DeathSpectateCoordinator.isDeadVoiceParticipant(receiver)) {
            // Dead listeners never consume voice from their physical server-side
            // spectator position. Their living soundscape is mirrored below from
            // the actual watched player instead.
            event.cancel();
            return;
        }

        for (ServerPlayer observer : DeathSpectateCoordinator.observersOf(receiver)) {
            sendEntity(event.getVoicechat(), observer, event.getPacket());
        }
    }

    private static void onLocationalSound(LocationalSoundPacketEvent event) {
        if (!ModCompatibilityConfig.simpleVoiceChatEnabled() || forwarding()) return;
        ServerPlayer receiver = minecraftPlayer(event.getReceiverConnection());
        if (receiver == null) return;

        if (DeathSpectateCoordinator.isDeadVoiceParticipant(receiver)) {
            event.cancel();
            return;
        }

        for (ServerPlayer observer : DeathSpectateCoordinator.observersOf(receiver)) {
            sendLocational(event.getVoicechat(), observer, event.getPacket());
        }
    }

    private static void onStaticSound(StaticSoundPacketEvent event) {
        if (!ModCompatibilityConfig.simpleVoiceChatEnabled() || forwarding()) return;
        ServerPlayer receiver = minecraftPlayer(event.getReceiverConnection());
        if (receiver == null) return;

        if (DeathSpectateCoordinator.isDeadVoiceParticipant(receiver)) {
            event.cancel();
            return;
        }

        for (ServerPlayer observer : DeathSpectateCoordinator.observersOf(receiver)) {
            sendStatic(event.getVoicechat(), observer, event.getPacket());
        }
    }

    private static void sendEntity(VoicechatServerApi api, ServerPlayer player,
            EntitySoundPacket packet) {
        VoicechatConnection connection = connection(api, player);
        if (connection == null) return;
        forward(() -> api.sendEntitySoundPacketTo(connection, packet));
    }

    private static void sendLocational(VoicechatServerApi api,
            ServerPlayer player, LocationalSoundPacket packet) {
        VoicechatConnection connection = connection(api, player);
        if (connection == null) return;
        forward(() -> api.sendLocationalSoundPacketTo(connection, packet));
    }

    private static void sendStatic(VoicechatServerApi api, ServerPlayer player,
            StaticSoundPacket packet) {
        VoicechatConnection connection = connection(api, player);
        if (connection == null) return;
        forward(() -> api.sendStaticSoundPacketTo(connection, packet));
    }

    private static VoicechatConnection connection(VoicechatServerApi api,
            ServerPlayer player) {
        if (api == null || player == null) return null;
        VoicechatConnection connection = api.getConnectionOf(player.getUUID());
        return connection != null && connection.isConnected()
                && connection.isInstalled() ? connection : null;
    }

    private static void forward(Runnable action) {
        boolean previous = forwarding();
        FORWARDING.set(true);
        try {
            action.run();
        } catch (RuntimeException | LinkageError exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Could not forward Simple Voice Chat audio into the SCP Additions death feed",
                    exception);
        } finally {
            FORWARDING.set(previous);
        }
    }

    private static boolean forwarding() {
        return Boolean.TRUE.equals(FORWARDING.get());
    }

    private static ServerPlayer minecraftPlayer(VoicechatConnection connection) {
        if (connection == null || connection.getPlayer() == null) return null;
        Object player = connection.getPlayer().getPlayer();
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private static UUID deadCallChannel(UUID speaker) {
        return UUID.nameUUIDFromBytes((ScpAdditionsMod.MODID
                + ":dead_voice:" + speaker).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean supportedApiInstalled() {
        String apiVersion = ModList.get()
                .getModContainerById(SimpleVoiceChatPresence.API_MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("");
        if (versionAtLeast(apiVersion, MIN_API_MAJOR, MIN_API_MINOR,
                MIN_API_PATCH)) {
            return true;
        }

        // Development environments and a few older packaging layouts may expose
        // only the parent voicechat mod. Its version is Minecraft-prefixed, so
        // compare the last semantic triplet (e.g. 1.20.1-2.6.22 -> 2.6.22).
        String voicechatVersion = ModList.get()
                .getModContainerById(SimpleVoiceChatPresence.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("");
        return versionAtLeast(voicechatVersion, MIN_API_MAJOR, MIN_API_MINOR,
                MIN_API_PATCH);
    }

    private static boolean versionAtLeast(String version, int major, int minor,
            int patch) {
        if (version == null || version.isBlank()) return false;
        Matcher matcher = SEMVER.matcher(version);
        int foundMajor = -1;
        int foundMinor = -1;
        int foundPatch = -1;
        while (matcher.find()) {
            try {
                foundMajor = Integer.parseInt(matcher.group(1));
                foundMinor = Integer.parseInt(matcher.group(2));
                foundPatch = Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if (foundMajor < 0) return false;
        if (foundMajor != major) return foundMajor > major;
        if (foundMinor != minor) return foundMinor > minor;
        return foundPatch >= patch;
    }
}
