package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.save.SaveDifficultyPolicy;
import net.mcreator.scpadditions.save.SaveGameContext;
import net.mcreator.scpadditions.save.SaveMethod;

import java.util.function.Supplier;

/** Requests a Safe/Thaumiel quicksave at the player's current position. */
public final class QuickSavePacket {
    public static void encode(QuickSavePacket message, FriendlyByteBuf buffer) {
    }

    public static QuickSavePacket decode(FriendlyByteBuf buffer) {
        return new QuickSavePacket();
    }

    public static void handle(QuickSavePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null || !player.isAlive() || player.isSpectator()
                    || !SaveDifficultyPolicy.allowsQuickSave(
                            player.serverLevel().getDifficulty())) {
                return;
            }

            SaveGameContext.run(SaveMethod.QUICK_SAVE, () ->
                    player.setRespawnPosition(
                            player.serverLevel().dimension(),
                            player.blockPosition(), player.getYRot(),
                            true, false));
        });
        context.setPacketHandled(true);
    }
}
