package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persists playable SCP-079 identity without persisting Spectator as the role. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079PlayablePersistence {
    private static final String ROOT = "ScpClassifiedDirectivePlayable079";
    private static final Map<UUID, CompoundTag> PENDING_ORIGINS = new HashMap<>();

    private Scp079PlayablePersistence() { }

    public static void beginAssume(ServerPlayer player) {
        if (player == null || active(player)) return;
        CompoundTag origin = new CompoundTag();
        origin.putString("OriginDimension", player.level().dimension().location().toString());
        origin.putDouble("OriginX", player.getX());
        origin.putDouble("OriginY", player.getY());
        origin.putDouble("OriginZ", player.getZ());
        origin.putFloat("OriginYaw", player.getYRot());
        origin.putFloat("OriginPitch", player.getXRot());
        origin.putInt("OriginGameMode", player.gameMode.getGameModeForPlayer().getId());
        origin.putBoolean("OriginInvulnerable", player.isInvulnerable());
        PENDING_ORIGINS.put(player.getUUID(), origin);
    }

    public static void finishAssume(ServerPlayer player, BlockPos hostPos,
            boolean success) {
        if (player == null) return;
        CompoundTag pending = PENDING_ORIGINS.remove(player.getUUID());
        if (!success) return;
        CompoundTag tag = data(player);
        if (!tag.getBoolean("Active") && pending != null) tag.merge(pending);
        tag.putBoolean("Active", true);
        tag.putString("HostDimension", player.serverLevel().dimension().location().toString());
        tag.putLong("HostPos", hostPos.asLong());
    }

    public static void restoreOriginalAndClear(ServerPlayer player) {
        if (player == null || !active(player)) return;
        CompoundTag tag = data(player);
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("OriginDimension"));
        ServerLevel target = dimension == null ? null : player.server.getLevel(
                ResourceKey.create(Registries.DIMENSION, dimension));
        double x = tag.getDouble("OriginX");
        double y = tag.getDouble("OriginY");
        double z = tag.getDouble("OriginZ");
        float yaw = tag.getFloat("OriginYaw");
        float pitch = tag.getFloat("OriginPitch");
        if (target != null) {
            player.teleportTo(target, x, y, z, yaw, pitch);
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.setInvulnerable(tag.getBoolean("OriginInvulnerable"));
        player.setGameMode(GameType.byId(tag.getInt("OriginGameMode")));
        player.getPersistentData().remove(ROOT);
    }

    public static boolean active(ServerPlayer player) {
        return player != null && player.getPersistentData().contains(ROOT)
                && data(player).getBoolean("Active");
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(player)) return;
        player.server.execute(() -> resume(player));
    }

    private static void resume(ServerPlayer player) {
        if (player == null || player.hasDisconnected() || !active(player)) return;
        CompoundTag tag = data(player);
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("HostDimension"));
        BlockPos hostPos = tag.contains("HostPos") ? BlockPos.of(tag.getLong("HostPos")) : null;
        ServerLevel hostLevel = dimension == null ? null : player.server.getLevel(
                ResourceKey.create(Registries.DIMENSION, dimension));
        if (hostLevel == null || hostPos == null) {
            restoreOriginalAndClear(player);
            return;
        }
        player.teleportTo(hostLevel, hostPos.getX() + 0.5D,
                hostPos.getY() + 0.5D, hostPos.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        if (!Scp079PlayableManager.assume(player, hostPos)) {
            restoreOriginalAndClear(player);
        }
    }

    private static CompoundTag data(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) persistent.put(ROOT, new CompoundTag());
        return persistent.getCompound(ROOT);
    }
}
