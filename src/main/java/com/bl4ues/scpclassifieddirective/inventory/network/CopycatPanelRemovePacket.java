package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.effect.Scp714ExposureManager;
import com.bl4ues.scpclassifieddirective.facility.DoubleWallPanelModule;
import com.bl4ues.scpclassifieddirective.facility.WallPanelModule;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderInteractionEvents;
import com.bl4ues.scpclassifieddirective.scp330.Scp330Hands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-authoritative material removal for the native copycat panels.
 *
 * Kept separate from ContextInteractPacket so a physical Screwdriver click
 * works even when contextual interactions are disabled or the prompt input
 * guard consumes Minecraft's ordinary use key.
 */
public final class CopycatPanelRemovePacket {
    private final BlockPos pos;
    private final String interactionKey;

    public CopycatPanelRemovePacket(BlockPos pos, String interactionKey) {
        this.pos = pos == null ? BlockPos.ZERO : pos.immutable();
        this.interactionKey = interactionKey == null ? "" : interactionKey;
    }

    public static void encode(CopycatPanelRemovePacket msg,
            FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.interactionKey, 128);
    }

    public static CopycatPanelRemovePacket decode(FriendlyByteBuf buf) {
        return new CopycatPanelRemovePacket(buf.readBlockPos(),
                buf.readUtf(128));
    }

    public static void handle(CopycatPanelRemovePacket msg,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || Scp714ExposureManager.isControlsLocked(player)
                    || Scp330Hands.isDisabled(player)) {
                return;
            }
            Level level = player.level();
            if (!level.isLoaded(msg.pos)
                    || player.getEyePosition().distanceTo(
                            Vec3.atCenterOf(msg.pos)) > 4.0D
                    || KeycardReaderInteractionEvents.screwdriver(
                            player).isEmpty()) {
                return;
            }

            BlockState state = level.getBlockState(msg.pos);
            ItemStack returned = ItemStack.EMPTY;

            if (state.is(WallPanelModule.BLOCK.get())
                    && level.getBlockEntity(msg.pos)
                    instanceof WallPanelModule.WallPanelBlockEntity panel) {
                WallPanelModule.Side side = wallSide(msg.interactionKey);
                if (side != null && panel.hasMaterial(side)) {
                    returned = panel.removeMaterial(side);
                }
            } else if (state.is(DoubleWallPanelModule.BLOCK.get())
                    && level.getBlockEntity(msg.pos)
                    instanceof DoubleWallPanelModule.DoubleWallPanelBlockEntity panel) {
                DoubleWallPanelModule.Side side =
                        doubleSide(msg.interactionKey);
                if (side != null && panel.hasMaterial(side)) {
                    returned = panel.removeMaterial(side);
                }
            }

            if (returned.isEmpty()) return;
            if (!player.getInventory().add(returned)) {
                player.drop(returned, false);
            }
            player.swing(screwdriverHand(player), true);
        });
        context.setPacketHandled(true);
    }

    private static WallPanelModule.Side wallSide(String key) {
        if (WallPanelModule.REMOVE_FRONT_INTERACTION.equals(key)) {
            return WallPanelModule.Side.FRONT;
        }
        if (WallPanelModule.REMOVE_BACK_INTERACTION.equals(key)) {
            return WallPanelModule.Side.BACK;
        }
        return null;
    }

    private static DoubleWallPanelModule.Side doubleSide(String key) {
        if (DoubleWallPanelModule.REMOVE_FRONT_INTERACTION.equals(key)) {
            return DoubleWallPanelModule.Side.FRONT;
        }
        if (DoubleWallPanelModule.REMOVE_BACK_INTERACTION.equals(key)) {
            return DoubleWallPanelModule.Side.BACK;
        }
        return null;
    }

    private static InteractionHand screwdriverHand(ServerPlayer player) {
        return player.getMainHandItem().is(
                com.bl4ues.scpclassifieddirective.init.UnifiedReaderItems
                        .SCREWDRIVER.get())
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}
