package com.bl4ues.scpclassifieddirective.block.entity;

import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Small synchronized state mirror used by the physical Tesla terminal screen.
 * The actual authority remains the existing global gamerules/facility power
 * systems; this entity only gives the client renderer a chunk-local snapshot.
 */
public final class TeslaTerminalBlockEntity extends BlockEntity {
    private boolean teslaGatesEnabled = true;
    private boolean manualOverride;
    private boolean auxiliaryPowerOnline = true;

    public TeslaTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ScpClassifiedDirectiveModBlockEntities.TESLA_TERMINAL.get(),
                pos, state);
    }

    public boolean teslaGatesEnabled() {
        return teslaGatesEnabled;
    }

    public boolean manualOverride() {
        return manualOverride;
    }

    public boolean auxiliaryPowerOnline() {
        return auxiliaryPowerOnline;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            TeslaTerminalBlockEntity terminal) {
        if (level.isClientSide) return;
        // Stagger terminals so a room full of them does not poll global state on
        // exactly the same server tick. Human beings do love redundant screens.
        if (Math.floorMod(level.getGameTime() + pos.asLong(), 10L) != 0L) {
            return;
        }
        boolean gates = level.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.TESLAGATEON);
        boolean override = level.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.TESLAGATEMANUALOVERRIDE);
        boolean auxiliary = Scp079FacilityAccessManager
                .isAuxiliaryPowerOnline(level);
        terminal.updateSnapshot(gates, override, auxiliary);
    }

    private void updateSnapshot(boolean gates, boolean override,
            boolean auxiliary) {
        if (teslaGatesEnabled == gates && manualOverride == override
                && auxiliaryPowerOnline == auxiliary) {
            return;
        }
        teslaGatesEnabled = gates;
        manualOverride = override;
        auxiliaryPowerOnline = auxiliary;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state,
                    Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("TeslaGatesEnabled", teslaGatesEnabled);
        tag.putBoolean("ManualOverride", manualOverride);
        tag.putBoolean("AuxiliaryPowerOnline", auxiliaryPowerOnline);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        teslaGatesEnabled = tag.getBoolean("TeslaGatesEnabled");
        manualOverride = tag.getBoolean("ManualOverride");
        auxiliaryPowerOnline = tag.contains("AuxiliaryPowerOnline")
                ? tag.getBoolean("AuxiliaryPowerOnline") : true;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net,
            ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) load(tag);
    }
}
