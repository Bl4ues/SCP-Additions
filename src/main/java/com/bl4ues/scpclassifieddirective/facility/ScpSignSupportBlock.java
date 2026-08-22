package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderInteractionEvents;
import com.bl4ues.scpclassifieddirective.network.ScpEntityNetwork;

import javax.annotation.Nullable;

/** Controller block for the wall-mounted, editable facility sign. */
public final class ScpSignSupportBlock extends AbstractFramedSignBlock {
    public ScpSignSupportBlock() {
        super(FacilityLargePropStructure.Kind.SIGN_SUPPORT);
    }

    @Override
    public String getDescriptionId() {
        return "Facility Sign";
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScpSignSupportBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos)
                        instanceof ScpSignSupportBlockEntity sign) {
            ScpEntityNetwork.openScpSignScreen(serverPlayer, sign);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack screwdriver = KeycardReaderInteractionEvents.screwdriver(player);
        if (screwdriver.isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos)
                        instanceof ScpSignSupportBlockEntity sign) {
            ScpEntityNetwork.openScpSignScreen(serverPlayer, sign);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
