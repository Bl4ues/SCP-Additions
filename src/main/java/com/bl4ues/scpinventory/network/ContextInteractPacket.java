package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.context.ContextInteractionRegistry;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.entity.AbstractScp131Entity;
import net.mcreator.scpadditions.facility.elevator.CoreRoomElevatorCarriageEntity;
import net.mcreator.scpadditions.facility.elevator.CoreRoomElevatorModule;
import net.mcreator.scpadditions.keycard.KeycardReaderInteractionEvents;
import net.mcreator.scpadditions.scp330.Scp330Hands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class ContextInteractPacket {
    private final BlockPos blockPos;
    private final int entityId;
    private final boolean entityTarget;
    private final boolean shiftDown;
    private final boolean controlDown;
    private final String interactionKey;

    public ContextInteractPacket(BlockPos blockPos) {
        this(blockPos, 0, false, false, false, "");
    }

    public ContextInteractPacket(BlockPos blockPos, int entityId,
            boolean entityTarget) {
        this(blockPos, entityId, entityTarget, false, false, "");
    }

    public ContextInteractPacket(BlockPos blockPos, int entityId,
            boolean entityTarget, boolean shiftDown, boolean controlDown) {
        this(blockPos, entityId, entityTarget, shiftDown, controlDown, "");
    }

    public ContextInteractPacket(BlockPos blockPos, int entityId,
            boolean entityTarget, boolean shiftDown, boolean controlDown,
            String interactionKey) {
        this.blockPos = blockPos == null ? BlockPos.ZERO : blockPos;
        this.entityId = entityId;
        this.entityTarget = entityTarget;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.interactionKey = interactionKey == null ? "" : interactionKey;
    }

    public static void encode(ContextInteractPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.entityTarget);
        buf.writeBlockPos(msg.blockPos);
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.shiftDown);
        buf.writeBoolean(msg.controlDown);
        buf.writeUtf(msg.interactionKey, 128);
    }

    public static ContextInteractPacket decode(FriendlyByteBuf buf) {
        boolean entityTarget = buf.readBoolean();
        BlockPos blockPos = buf.readBlockPos();
        int entityId = buf.readInt();
        boolean shiftDown = buf.readBoolean();
        boolean controlDown = buf.readBoolean();
        String interactionKey = buf.readUtf(128);
        return new ContextInteractPacket(blockPos, entityId, entityTarget,
                shiftDown, controlDown, interactionKey);
    }

    public static void handle(ContextInteractPacket msg,
            Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (!ScpAdditionsModulesConfig.customInteractionsEnabledFor(player)
                    || Scp330Hands.isDisabled(player)) {
                return;
            }
            if (msg.entityTarget) {
                handleEntityInteraction(player, msg.entityId, msg.shiftDown,
                        msg.interactionKey);
            } else {
                handleBlockInteraction(player, msg.blockPos, msg.shiftDown,
                        msg.controlDown, msg.interactionKey);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static ContextInteractionRegistry.Rule selectedRule(
            List<ContextInteractionRegistry.Rule> rules, String key) {
        if (rules.isEmpty()) return null;
        if (key != null && !key.isBlank()) {
            for (ContextInteractionRegistry.Rule rule : rules) {
                if (key.equals(rule.interactionKey())) return rule;
            }
        }
        return rules.get(0);
    }

    private static void handleEntityInteraction(ServerPlayer player,
            int entityId, boolean shiftDown, String interactionKey) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Entity entity = level.getEntity(entityId);
        if (entity == null || !entity.isAlive()) return;
        if (entity instanceof AbstractScp131Entity scp131
                && scp131.isFollowingPlayer(player)
                && (interactionKey == null
                || !interactionKey.contains("screwdriver"))) return;

        List<ContextInteractionRegistry.Rule> rules =
                ContextInteractionRegistry.getEntityRules(entity.getType());
        ContextInteractionRegistry.Rule rule = selectedRule(rules,
                interactionKey);
        if (rule == null || !rule.isAvailable(entity)) return;
        InteractionHand hand = rule.matchingHand(player);
        if (hand == null) return;
        Vec3 anchor = rule.resolveEntityAnchor(entity);
        if (player.getEyePosition().distanceTo(anchor) > rule.range() + 0.75D) {
            return;
        }

        if (entity instanceof CoreRoomElevatorCarriageEntity carriage) {
            if (carriage.handleContextInteraction(player,
                    rule.interactionKey())) {
                player.swing(InteractionHand.MAIN_HAND, true);
            }
            return;
        }

        boolean previousShift = player.isShiftKeyDown();
        InteractionResult result;
        player.setShiftKeyDown(shiftDown);
        try {
            result = entity.interact(player, hand);
        } finally {
            player.setShiftKeyDown(previousShift);
        }
        if (result.consumesAction()) {
            player.swing(hand, true);
        }
    }

    private static void handleBlockInteraction(ServerPlayer player,
            BlockPos pos, boolean shiftDown, boolean controlDown,
            String interactionKey) {
        Level level = player.level();
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        List<ContextInteractionRegistry.Rule> rules =
                ContextInteractionRegistry.getBlockRules(state.getBlock());
        ContextInteractionRegistry.Rule rule = selectedRule(rules,
                interactionKey);
        if (rule == null || !rule.isAvailable(level, pos, state)) return;
        InteractionHand hand = rule.matchingHand(player);
        if (hand == null) return;
        Vec3 anchor = rule.resolveBlockAnchor(pos, state);
        if (player.getEyePosition().distanceTo(anchor) > rule.range() + 0.75D) {
            return;
        }

        if (state.getBlock() instanceof CoreRoomElevatorModule.StationBlock station
                && rule.interactionKey().startsWith("elevator_station_")
                && player.level() instanceof ServerLevel serverLevel) {
            InteractionResult result = station.handleContextInteraction(
                    serverLevel, pos, player, rule.interactionKey());
            if (result.consumesAction()) {
                player.swing(InteractionHand.MAIN_HAND, true);
            }
            return;
        }

        if (KeycardReaderInteractionEvents.tryHandleInteraction(player, pos,
                shiftDown, controlDown)) {
            player.swing(InteractionHand.MAIN_HAND, true);
            return;
        }

        boolean doorBefore = isDoorWithOpenState(state);
        boolean wasOpen = doorBefore
                && state.getValue(BlockStateProperties.OPEN);
        BlockHitResult hit = new BlockHitResult(anchor,
                rule.resolveClickFace(state, player), pos, false);
        ItemStack heldItem = player.getItemInHand(hand);
        boolean previousShift = player.isShiftKeyDown();
        InteractionResult result;
        player.setShiftKeyDown(shiftDown);
        try {
            result = player.gameMode.useItemOn(player, level, heldItem,
                    hand, hit);
        } finally {
            player.setShiftKeyDown(previousShift);
        }
        if (result.consumesAction()) {
            player.swing(hand, true);
            playDoorSoundForUser(player, level, pos, state, doorBefore,
                    wasOpen);
        }
    }

    private static boolean isDoorWithOpenState(BlockState state) {
        return state.getBlock() instanceof DoorBlock
                && state.hasProperty(BlockStateProperties.OPEN);
    }

    private static void playDoorSoundForUser(ServerPlayer player, Level level,
            BlockPos pos, BlockState oldState, boolean wasDoor,
            boolean wasOpen) {
        if (!wasDoor) return;
        BlockState newState = level.getBlockState(pos);
        if (!newState.hasProperty(BlockStateProperties.OPEN)) return;
        boolean isOpen = newState.getValue(BlockStateProperties.OPEN);
        if (isOpen == wasOpen) return;
        SoundEvent sound = oldState.is(Blocks.IRON_DOOR)
                ? (isOpen ? SoundEvents.IRON_DOOR_OPEN
                : SoundEvents.IRON_DOOR_CLOSE)
                : (isOpen ? SoundEvents.WOODEN_DOOR_OPEN
                : SoundEvents.WOODEN_DOOR_CLOSE);
        player.playNotifySound(sound, SoundSource.BLOCKS, 1.0F,
                0.9F + player.getRandom().nextFloat() * 0.1F);
    }
}
