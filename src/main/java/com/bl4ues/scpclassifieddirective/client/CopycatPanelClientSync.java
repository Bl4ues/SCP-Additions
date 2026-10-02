package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.facility.DoubleWallPanelModule;
import com.bl4ues.scpclassifieddirective.facility.WallPanelModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CopycatPanelClientSync {
    private CopycatPanelClientSync() {
    }

    public static void apply(BlockPos pos, CompoundTag tag) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || pos == null || tag == null) return;

        BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
        if (!(blockEntity instanceof WallPanelModule.WallPanelBlockEntity)
                && !(blockEntity instanceof
                DoubleWallPanelModule.DoubleWallPanelBlockEntity)) {
            return;
        }

        blockEntity.load(tag);
        blockEntity.requestModelDataUpdate();

        BlockState state = minecraft.level.getBlockState(pos);
        minecraft.level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        com.bl4ues.scpclassifieddirective.inventory.client.ContextPromptClient
                .clear();
    }
}
