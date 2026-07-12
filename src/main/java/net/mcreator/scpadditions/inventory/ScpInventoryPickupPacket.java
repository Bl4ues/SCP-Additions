package net.mcreator.scpadditions.inventory;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Validated one-item manual pickup used by E and right click prompts. */
public final class ScpInventoryPickupPacket {
    private static final double MAX_DISTANCE_SQR = 6.25D;
    private static final long COOLDOWN_TICKS = 4L;
    private static final Map<UUID, Long> LAST_PICKUP = new ConcurrentHashMap<>();

    private final int entityId;

    public ScpInventoryPickupPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(ScpInventoryPickupPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
    }

    public static ScpInventoryPickupPacket decode(FriendlyByteBuf buffer) {
        return new ScpInventoryPickupPacket(buffer.readVarInt());
    }

    public static void handle(ScpInventoryPickupPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.isCreative() || player.isSpectator()
                    || !ScpAdditionsModulesConfig.get().inventory.enabled) {
                return;
            }

            long now = player.serverLevel().getGameTime();
            long last = LAST_PICKUP.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2L);
            if (now - last < COOLDOWN_TICKS) return;

            Entity entity = player.serverLevel().getEntity(message.entityId);
            if (!(entity instanceof ItemEntity itemEntity)
                    || !itemEntity.isAlive()
                    || player.distanceToSqr(itemEntity) > MAX_DISTANCE_SQR) {
                return;
            }

            ItemStack ground = itemEntity.getItem();
            if (ground.isEmpty() || ScpPickupRouter.isUsableSession(ground)
                    || ScpPickupRouter.isInternalMirror(ground)) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE)
                    .ifPresent(inventory -> {
                        ItemStack one = ground.copy();
                        one.setCount(1);
                        int accepted = ScpPickupRouter.accept(inventory, player, one);
                        LAST_PICKUP.put(player.getUUID(), now);
                        if (accepted <= 0) {
                            ScpInventoryNetwork.notifyFull(player);
                            ScpInventoryNetwork.sync(player);
                            return;
                        }

                        player.take(itemEntity, accepted);
                        player.level().playSound(null, player.getX(), player.getY(),
                                player.getZ(), SoundEvents.ITEM_PICKUP,
                                SoundSource.PLAYERS, 0.2F,
                                ((player.getRandom().nextFloat()
                                        - player.getRandom().nextFloat())
                                        * 0.7F + 1.0F) * 2.0F);
                        ground.shrink(accepted);
                        if (ground.isEmpty()) itemEntity.discard();
                        else {
                            itemEntity.setItem(ground);
                            itemEntity.setPickUpDelay(10);
                        }
                        ScpInventoryNetwork.sync(player);
                    });
        });
        context.setPacketHandled(true);
    }
}
