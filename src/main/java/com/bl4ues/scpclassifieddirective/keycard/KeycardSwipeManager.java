package com.bl4ues.scpclassifieddirective.keycard;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import com.bl4ues.scpclassifieddirective.hacking.HackingDeviceAttachmentManager;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.mixin.ObjectContainmentUnitHackInvoker;
import com.bl4ues.scpclassifieddirective.network.KeycardSwipeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns ordinary card use into a short diegetic swipe. The inventory is never
 * altered; the highest available keycard is only represented visually.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KeycardSwipeManager {
    public static final int SWIPE_TICKS = 12;
    public static final int ACCEPT_TICK = SWIPE_TICKS / 2;

    private static final Map<TargetKey, Long> ACTIVE_UNTIL = new HashMap<>();

    private KeycardSwipeManager() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Dedicated tool interactions retain priority over a normal card swipe.
        if (!KeycardReaderInteractionEvents.screwdriver(player).isEmpty()
                || holdsHackingDevice(player)) {
            return;
        }

        KeycardReaderLevels.ReaderDescriptor reader =
                KeycardReaderLevels.describe(state);
        boolean ocu = state.is(ObjectContainmentUnitModule.UNIT.get());
        if (reader == null && !ocu) return;

        if (reader != null && state.getBlock()
                != KeycardReaderLevels.normalBlock(reader.level(), reader.side())) {
            return;
        }
        if (ocu) {
            if (!(level.getBlockEntity(pos)
                    instanceof ObjectContainmentUnitModule.UnitBlockEntity unit)
                    || unit.isOpenForAccess() || unit.isTransitioning()) {
                return;
            }
        }

        int keycardLevel = KeycardAccess.highestLevel(player);
        if (keycardLevel <= 0) return;

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(
                level.isClientSide));
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (HackingDeviceAttachmentManager.isAttached(serverLevel, pos)) return;

        TargetKey key = new TargetKey(serverLevel.dimension(), pos.immutable());
        long now = serverLevel.getGameTime();
        Long activeUntil = ACTIVE_UNTIL.get(key);
        if (activeUntil != null && activeUntil > now) return;
        ACTIVE_UNTIL.put(key, now + SWIPE_TICKS);

        int required = reader != null ? reader.level()
                : ((ObjectContainmentUnitModule.UnitBlockEntity)
                serverLevel.getBlockEntity(pos)).requiredLevel();
        boolean accepted = keycardLevel >= required;
        KeycardSwipeNetwork.broadcast(serverLevel, pos, keycardLevel, ocu);

        KeycardReaderLevels.Side side = reader == null ? null : reader.side();
        int readerLevel = reader == null ? 0 : reader.level();
        ScpClassifiedDirectiveMod.queueServerWork(ACCEPT_TICK,
                () -> resolveMidpoint(serverLevel, pos.immutable(), key,
                        ocu, readerLevel, side, accepted));
        ScpClassifiedDirectiveMod.queueServerWork(SWIPE_TICKS + 1,
                () -> ACTIVE_UNTIL.remove(key));
    }

    private static void resolveMidpoint(ServerLevel level, BlockPos pos,
            TargetKey key, boolean ocu, int readerLevel,
            KeycardReaderLevels.Side side, boolean accepted) {
        if (level == null || pos == null || !level.hasChunkAt(pos)) return;
        if (HackingDeviceAttachmentManager.isAttached(level, pos)) return;

        if (ocu) {
            if (!(level.getBlockEntity(pos)
                    instanceof ObjectContainmentUnitModule.UnitBlockEntity unit)
                    || unit.isOpenForAccess() || unit.isTransitioning()) {
                return;
            }
            ObjectContainmentUnitHackInvoker invoker =
                    (ObjectContainmentUnitHackInvoker) (Object) unit;
            invoker.scpclassifieddirective$playReaderSound(accepted);
            if (accepted) invoker.scpclassifieddirective$startOpening();
            return;
        }

        BlockState state = level.getBlockState(pos);
        KeycardReaderLevels.ReaderDescriptor descriptor =
                KeycardReaderLevels.describe(state);
        if (descriptor == null || descriptor.level() != readerLevel
                || descriptor.side() != side
                || state.getBlock() != KeycardReaderLevels.normalBlock(
                readerLevel, side)) {
            return;
        }

        if (accepted) {
            KeycardReaderLevels.activateAccepted(level, pos);
        } else {
            activateDenied(level, pos, state, readerLevel, side);
        }
    }

    private static boolean activateDenied(ServerLevel level, BlockPos pos,
            BlockState current, int accessLevel, KeycardReaderLevels.Side side) {
        Block wrong = wrongBlock(accessLevel, side);
        if (wrong == null) {
            playDenied(level, pos);
            return false;
        }
        BlockState replacement = wrong.defaultBlockState();
        if (current.hasProperty(HorizontalDirectionalBlock.FACING)
                && replacement.hasProperty(HorizontalDirectionalBlock.FACING)) {
            replacement = replacement.setValue(HorizontalDirectionalBlock.FACING,
                    current.getValue(HorizontalDirectionalBlock.FACING));
        }
        if (current.hasProperty(BlockStateProperties.WATERLOGGED)
                && replacement.hasProperty(BlockStateProperties.WATERLOGGED)) {
            replacement = replacement.setValue(BlockStateProperties.WATERLOGGED,
                    current.getValue(BlockStateProperties.WATERLOGGED));
        }
        return level.setBlock(pos, replacement, Block.UPDATE_ALL);
    }

    private static Block wrongBlock(int level, KeycardReaderLevels.Side side) {
        return switch (level) {
            case 1 -> side == KeycardReaderLevels.Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LEFT_READER_WRONG.get()
                    : ScpClassifiedDirectiveModBlocks.RIGHT_READER_WRONG.get();
            case 2 -> side == KeycardReaderLevels.Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_2_LEFT_READER_WRONG.get()
                    : ScpClassifiedDirectiveModBlocks.LV_2_RIGHT_READER_WRONG.get();
            case 3 -> side == KeycardReaderLevels.Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_3_LEFT_READER_WRONG.get()
                    : ScpClassifiedDirectiveModBlocks.LV_3_RIGHT_READER_WRONG.get();
            case 4 -> side == KeycardReaderLevels.Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_4_LEFT_READER_WRONG.get()
                    : ScpClassifiedDirectiveModBlocks.LV_4_RIGHT_READER_WRONG.get();
            case 5 -> side == KeycardReaderLevels.Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_5_LEFT_READER_WRONG.get()
                    : ScpClassifiedDirectiveModBlocks.LV_5_RIGHT_READER_WRONG.get();
            case 6 -> side == KeycardReaderLevels.Side.LEFT
                    ? ScpClassifiedDirectiveModBlocks.LV_6_LEFT_READER_WRONG.get()
                    : ScpClassifiedDirectiveModBlocks.LV_6_RIGHT_READER_WRONG.get();
            default -> null;
        };
    }

    private static void playDenied(ServerLevel level, BlockPos pos) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                        "accessdenied"));
        if (sound != null) {
            level.playSound(null, pos, sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    private static boolean holdsHackingDevice(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(ScpClassifiedDirectiveModItems.HACKING_DEVICE.get())
                || off.is(ScpClassifiedDirectiveModItems.HACKING_DEVICE.get());
    }

    private record TargetKey(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
