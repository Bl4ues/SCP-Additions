package com.bl4ues.scpclassifieddirective.inventory.sound;

import com.bl4ues.scpclassifieddirective.inventory.item.ScpConsumableType;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import com.bl4ues.scpclassifieddirective.inventory.network.ItemInteractionSoundPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

/**
 * Keeps vanilla spatial feedback for observers while letting only the acting
 * client's personal preference replace its own SCP Inventory interaction cue.
 */
public final class InventoryInteractionSoundFeedback {
    private InventoryInteractionSoundFeedback() {
    }

    public static void pickup(ServerPlayer player) {
        if (player == null) return;
        float pitch = ((player.getRandom().nextFloat()
                - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F;
        playForOthers(player, SoundEvents.ITEM_PICKUP, 0.2F, pitch);
        send(player, ItemInteractionSoundPacket.Cue.PICKUP);
    }

    public static void equipped(ServerPlayer player) {
        send(player, ItemInteractionSoundPacket.Cue.EQUIP);
    }

    public static void consumed(ServerPlayer player, ItemStack stack) {
        if (player == null) return;
        ScpConsumableType type = ScpItemClassifier.getConsumableType(stack);
        SoundEvent vanilla = type == ScpConsumableType.DRINK
                ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT;
        float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;
        playForOthers(player, vanilla, 0.8F, pitch);
        send(player, type == ScpConsumableType.DRINK
                ? ItemInteractionSoundPacket.Cue.DRINK
                : ItemInteractionSoundPacket.Cue.FOOD);
    }

    private static void playForOthers(ServerPlayer player, SoundEvent sound,
            float volume, float pitch) {
        // Excluding the source player prevents a custom local cue from layering
        // on top of the vanilla sound, while observers retain normal feedback.
        player.level().playSound(player, player.getX(), player.getY(),
                player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void send(ServerPlayer player,
            ItemInteractionSoundPacket.Cue cue) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ItemInteractionSoundPacket(cue));
    }
}
