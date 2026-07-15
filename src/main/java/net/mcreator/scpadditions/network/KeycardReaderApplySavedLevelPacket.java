package net.mcreator.scpadditions.network;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.keycard.KeycardReaderInteractionEvents;
import net.mcreator.scpadditions.keycard.KeycardReaderLevels;

import java.util.function.Supplier;

public final class KeycardReaderApplySavedLevelPacket {
    private final BlockPos pos;

    public KeycardReaderApplySavedLevelPacket(BlockPos pos) {
        this.pos = pos.immutable();
    }

    public static void encode(KeycardReaderApplySavedLevelPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
    }

    public static KeycardReaderApplySavedLevelPacket decode(FriendlyByteBuf buffer) {
        return new KeycardReaderApplySavedLevelPacket(buffer.readBlockPos());
    }

    public static void handle(KeycardReaderApplySavedLevelPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.distanceToSqr(Vec3.atCenterOf(message.pos)) > 64.0D
                    || !player.level().hasChunkAt(message.pos)) return;
            KeycardReaderLevels.ReaderDescriptor descriptor = KeycardReaderLevels.describe(
                    player.level().getBlockState(message.pos));
            if (descriptor == null) return;

            ItemStack screwdriver = KeycardReaderInteractionEvents.screwdriver(player);
            if (screwdriver.isEmpty()) return;
            int savedLevel = screwdriver.hasTag()
                    ? screwdriver.getTag().getInt(KeycardReaderInteractionEvents.SAVED_LEVEL_TAG) : 0;
            if (savedLevel < 1 || savedLevel > 6) {
                KeycardReaderInteractionEvents.suppressNextInteraction(player, message.pos);
                player.displayClientMessage(Component.translatable(
                        "message.scp_additions.keycard_reader_no_saved_level").withStyle(ChatFormatting.RED), true);
                return;
            }

            KeycardReaderInteractionEvents.suppressNextInteraction(player, message.pos);
            if (descriptor.level() == savedLevel
                    || KeycardReaderLevels.replaceLevel(player.level(), message.pos, savedLevel)) {
                player.displayClientMessage(Component.translatable(
                        "message.scp_additions.keycard_reader_level_applied", savedLevel)
                        .withStyle(ChatFormatting.GREEN), true);
            }
        });
        context.setPacketHandled(true);
    }
}
