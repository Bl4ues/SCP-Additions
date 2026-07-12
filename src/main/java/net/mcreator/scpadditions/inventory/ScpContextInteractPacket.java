package net.mcreator.scpadditions.inventory;

import com.bl4ues.scpinventory.context.ContextInteractionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.List;
import java.util.function.Supplier;

/** Server-authoritative execution for configured block/entity context prompts. */
public final class ScpContextInteractPacket {
    private final BlockPos blockPos;
    private final int entityId;
    private final boolean entityTarget;

    public ScpContextInteractPacket(BlockPos blockPos) {
        this(blockPos, 0, false);
    }

    public ScpContextInteractPacket(BlockPos blockPos, int entityId,
            boolean entityTarget) {
        this.blockPos = blockPos == null ? BlockPos.ZERO : blockPos;
        this.entityId = entityId;
        this.entityTarget = entityTarget;
    }

    public static void encode(ScpContextInteractPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.entityTarget);
        buffer.writeBlockPos(message.blockPos);
        buffer.writeVarInt(message.entityId);
    }

    public static ScpContextInteractPacket decode(FriendlyByteBuf buffer) {
        return new ScpContextInteractPacket(buffer.readBlockPosAfterBoolean(), 0, false);
    }

    private static ScpContextInteractPacket decodeCompat(FriendlyByteBuf buffer) {
        boolean entityTarget = buffer.readBoolean();
        BlockPos blockPos = buffer.readBlockPos();
        int entityId = buffer.readVarInt();
        return new ScpContextInteractPacket(blockPos, entityId, entityTarget);
    }

    public static ScpContextInteractPacket read(FriendlyByteBuf buffer) {
        return decodeCompat(buffer);
    }

    public static void handle(ScpContextInteractPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.isSpectator()
                    || !ScpAdditionsModulesConfig.get().inventory.enabled) {
                return;
            }
            if (message.entityTarget) interactEntity(player, message.entityId);
            else interactBlock(player, message.blockPos);
        });
        context.setPacketHandled(true);
    }

    private static void interactEntity(ServerPlayer player, int entityId) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Entity entity = level.getEntity(entityId);
        if (entity == null || !entity.isAlive()) return;

        List<ContextInteractionRegistry.Rule> rules =
                ContextInteractionRegistry.getEntityRules(entity.getType());
        if (rules.isEmpty()) return;
        ContextInteractionRegistry.Rule rule = rules.get(0);
        Vec3 anchor = rule.resolveEntityAnchor(entity);
        if (player.getEyePosition().distanceTo(anchor) > rule.range() + 0.75D)
            return;

        InteractionResult result = entity.interact(player, InteractionHand.MAIN_HAND);
        if (result.consumesAction()) player.swing(InteractionHand.MAIN_HAND, true);
    }

    private static void interactBlock(ServerPlayer player, BlockPos pos) {
        Level level = player.level();
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        List<ContextInteractionRegistry.Rule> rules =
                ContextInteractionRegistry.getBlockRules(state.getBlock());
        if (rules.isEmpty()) return;
        ContextInteractionRegistry.Rule rule = rules.get(0);
        Vec3 anchor = rule.resolveBlockAnchor(pos, state);
        if (player.getEyePosition().distanceTo(anchor) > rule.range() + 0.75D)
            return;

        BlockHitResult hit = new BlockHitResult(anchor,
                rule.resolveClickFace(state, player), pos, false);
        InteractionResult result = state.use(level, player,
                InteractionHand.MAIN_HAND, hit);
        if (result.consumesAction()) player.swing(InteractionHand.MAIN_HAND, true);
    }
}
