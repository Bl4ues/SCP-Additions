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

public final class KeycardReaderCopyLevelPacket {
    private final BlockPos pos;

    public KeycardReaderCopyLevelPacket(BlockPos pos) {
        this.pos = pos.immutable();
    }

    public static void encode(KeycardReaderCopyLevelPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
    }

    public static KeycardReaderCopyLevelPacket decode(FriendlyByteBuf buffer) {
        return new KeycardReaderCopyLevelPacket(buffer.readBlockPos());
    }

    public static void handle(KeycardReaderCopyLevelPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
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

            KeycardReaderInteractionEvents.suppressNextInteraction(player, message.pos);
            screwdriver.getOrCreateTag().putInt(KeycardReaderInteractionEvents.SAVED_LEVEL_TAG,
                    descriptor.level());
            player.displayClientMessage(Component.translatable(
                    "message.scp_additions.keycard_reader_level_copied", descriptor.level())
                    .withStyle(ChatFormatting.GREEN), true);
        });
        context.setPacketHandled(true);
    }
}
