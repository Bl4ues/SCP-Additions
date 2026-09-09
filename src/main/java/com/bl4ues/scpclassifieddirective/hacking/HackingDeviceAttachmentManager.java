package com.bl4ues.scpclassifieddirective.hacking;

import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.inventory.sound.InventoryInteractionSoundFeedback;
import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderLevels;
import com.bl4ues.scpclassifieddirective.network.HackingDeviceNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Server-authoritative persistent mirror for devices physically seated during a hack. */
public final class HackingDeviceAttachmentManager {
    private static final String DATA_NAME = "scp_cd_hacking_device_attachments";

    private HackingDeviceAttachmentManager() {
    }

    public static boolean isCompatibleTarget(Level level, BlockPos pos) {
        return level != null && pos != null
                && isCompatibleTarget(level.getBlockState(pos));
    }

    public static boolean isCompatibleTarget(BlockState state) {
        return state != null && (KeycardReaderLevels.describe(state) != null
                || state.is(ObjectContainmentUnitModule.UNIT.get()));
    }

    public static boolean isAttached(ServerLevel level, BlockPos pos) {
        return level != null && pos != null && data(level).attached.contains(pos);
    }

    public static Set<BlockPos> snapshot(ServerLevel level) {
        return Set.copyOf(data(level).attached);
    }

    public static boolean attach(ServerPlayer player, BlockPos pos,
            InteractionHand hand) {
        if (player == null || pos == null || hand == null
                || !(player.level() instanceof ServerLevel level)
                || !isCompatibleTarget(level, pos)) {
            return false;
        }
        Data data = data(level);
        if (data.attached.contains(pos)) return false;

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ScpClassifiedDirectiveModItems.HACKING_DEVICE.get())
                || !HackingDeviceItem.isReady(held, level)) {
            return false;
        }

        data.attached.add(pos.immutable());
        data.setDirty();
        if (!player.getAbilities().instabuild) held.shrink(1);

        HackingDeviceNetwork.broadcastAttachment(level, pos, true);
        InventoryInteractionSoundFeedback.pickup(player);
        HackingDeviceSessionManager.start(player, pos, hand);
        return true;
    }

    public static boolean detach(ServerPlayer player, ServerLevel level,
            BlockPos pos, InteractionHand preferredHand,
            boolean playReturnCue) {
        return detach(player, level, pos, preferredHand, playReturnCue, 0L, 0L);
    }

    public static boolean detach(ServerPlayer player, ServerLevel level,
            BlockPos pos, InteractionHand preferredHand,
            boolean playReturnCue, long countdownEnd, long readyAt) {
        if (player == null || level == null || pos == null) return false;
        Data data = data(level);
        if (!data.attached.remove(pos)) return false;
        data.setDirty();

        if (player.getAbilities().instabuild) {
            if (preferredHand != null) {
                ItemStack existing = player.getItemInHand(preferredHand);
                if (existing.is(ScpClassifiedDirectiveModItems.HACKING_DEVICE.get())
                        && readyAt > 0L) {
                    HackingDeviceItem.armCooldown(existing, countdownEnd, readyAt);
                }
            }
        } else {
            ItemStack returned = new ItemStack(
                    ScpClassifiedDirectiveModItems.HACKING_DEVICE.get());
            if (readyAt > 0L) {
                HackingDeviceItem.armCooldown(returned, countdownEnd, readyAt);
            }
            if (preferredHand != null
                    && player.getItemInHand(preferredHand).isEmpty()) {
                player.setItemInHand(preferredHand, returned);
            } else if (!player.getInventory().add(returned)) {
                player.drop(returned, false);
            }
        }
        HackingDeviceNetwork.broadcastAttachment(level, pos, false);
        if (playReturnCue) InventoryInteractionSoundFeedback.pickup(player);
        return true;
    }

    /**
     * Removes old pre-session attachments and broken targets. Attachments are now
     * deliberately temporary and may only exist while a live hacking session owns
     * the reader.
     */
    public static void validate(ServerLevel level) {
        if (level == null) return;
        Data data = data(level);
        Set<BlockPos> invalid = new HashSet<>();
        for (BlockPos pos : data.attached) {
            if (!level.hasChunkAt(pos)) continue;
            if (!isCompatibleTarget(level, pos)
                    || !HackingDeviceSessionManager.hasActiveTarget(level, pos)) {
                invalid.add(pos);
            }
        }
        if (invalid.isEmpty()) return;

        for (BlockPos pos : invalid) {
            data.attached.remove(pos);
            ItemStack stack = new ItemStack(
                    ScpClassifiedDirectiveModItems.HACKING_DEVICE.get());
            level.addFreshEntity(new ItemEntity(level,
                    pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, stack));
            HackingDeviceNetwork.broadcastAttachment(level, pos, false);
            HackingDeviceSessionManager.abortTarget(level, pos);
        }
        data.setDirty();
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                Data::load, Data::new, DATA_NAME);
    }

    private static final class Data extends SavedData {
        private final Set<BlockPos> attached = new HashSet<>();

        private Data() {
        }

        private static Data load(CompoundTag tag) {
            Data data = new Data();
            for (long packed : tag.getLongArray("Attached")) {
                data.attached.add(BlockPos.of(packed));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            long[] packed = new long[attached.size()];
            int index = 0;
            for (BlockPos pos : attached) packed[index++] = pos.asLong();
            tag.putLongArray("Attached", packed);
            return tag;
        }
    }
}
