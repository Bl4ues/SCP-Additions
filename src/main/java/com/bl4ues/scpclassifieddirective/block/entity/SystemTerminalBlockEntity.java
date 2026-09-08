package com.bl4ues.scpclassifieddirective.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager.DiagnosticSnapshot;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * GeckoLib host plus synchronized display snapshot for the physical SCiPNET
 * diagnostic terminal. Diagnostic values are deliberately snapshot-based: a
 * completed analysis remains visible on the CRT until another analysis or a
 * remote-session cache purge changes the terminal state.
 */
public final class SystemTerminalBlockEntity extends BlockEntity
        implements GeoBlockEntity {
    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    private boolean analyzed;
    private int uncontainedScps;
    private int activeTeslaGates;
    private int registeredTeslaGates;
    private boolean teslaOverride;
    private int connectedDoors;
    private boolean auxiliaryPowerOnline;
    private boolean unusualNetworkActivity;
    private long cachePurgeEndGameTime;

    public SystemTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ScpClassifiedDirectiveModBlockEntities.SCP_079_SYSTEM_CONTROL.get(),
                pos, state);
    }

    public boolean analyzed() {
        return analyzed;
    }

    public int uncontainedScps() {
        return uncontainedScps;
    }

    public int activeTeslaGates() {
        return activeTeslaGates;
    }

    public int registeredTeslaGates() {
        return registeredTeslaGates;
    }

    public boolean teslaOverride() {
        return teslaOverride;
    }

    public int connectedDoors() {
        return connectedDoors;
    }

    public boolean auxiliaryPowerOnline() {
        return auxiliaryPowerOnline;
    }

    public boolean unusualNetworkActivity() {
        return unusualNetworkActivity;
    }

    public int cachePurgeCooldownTicks() {
        if (cachePurgeEndGameTime <= 0L || level == null) return 0;
        long remaining = cachePurgeEndGameTime - level.getGameTime();
        return remaining <= 0L ? 0
                : (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            SystemTerminalBlockEntity terminal) {
        if (level.isClientSide) return;
        if (Math.floorMod(level.getGameTime() + pos.asLong(), 10L) != 0L) {
            return;
        }
        boolean auxiliary = Scp079FacilityAccessManager
                .isAuxiliaryPowerOnline(level);
        int cooldown = Scp079FacilityAccessManager
                .cachePurgeCooldownTicks(level.getServer());
        terminal.updateLiveState(auxiliary, cooldown);
    }

    public void updateSnapshot(DiagnosticSnapshot snapshot,
            boolean analysisComplete) {
        if (snapshot == null) return;
        long end = cooldownEnd(snapshot.cachePurgeCooldownTicks());
        boolean changed = analyzed != analysisComplete
                || uncontainedScps != snapshot.uncontainedScps()
                || activeTeslaGates != snapshot.activeTeslaGates()
                || registeredTeslaGates != snapshot.registeredTeslaGates()
                || teslaOverride != snapshot.teslaOverride()
                || connectedDoors != snapshot.connectedDoors()
                || auxiliaryPowerOnline != snapshot.auxiliaryPowerOnline()
                || unusualNetworkActivity != snapshot.unusualNetworkActivity()
                || cachePurgeEndGameTime != end;
        if (!changed) return;

        analyzed = analysisComplete;
        uncontainedScps = Math.max(0, snapshot.uncontainedScps());
        activeTeslaGates = Math.max(0, snapshot.activeTeslaGates());
        registeredTeslaGates = Math.max(0, snapshot.registeredTeslaGates());
        teslaOverride = snapshot.teslaOverride();
        connectedDoors = Math.max(0, snapshot.connectedDoors());
        auxiliaryPowerOnline = snapshot.auxiliaryPowerOnline();
        unusualNetworkActivity = snapshot.unusualNetworkActivity();
        cachePurgeEndGameTime = end;
        syncChanged();
    }

    private void updateLiveState(boolean auxiliary, int cooldownTicks) {
        long end = cooldownEnd(cooldownTicks);
        boolean nextAnalyzed = cooldownTicks > 0 ? false : analyzed;
        if (auxiliaryPowerOnline == auxiliary
                && cachePurgeEndGameTime == end
                && analyzed == nextAnalyzed) {
            return;
        }
        auxiliaryPowerOnline = auxiliary;
        cachePurgeEndGameTime = end;
        analyzed = nextAnalyzed;
        syncChanged();
    }

    private long cooldownEnd(int remainingTicks) {
        if (remainingTicks <= 0 || level == null) return 0L;
        return level.getGameTime() + remainingTicks;
    }

    private void syncChanged() {
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
        tag.putBoolean("Analyzed", analyzed);
        tag.putInt("UncontainedScps", uncontainedScps);
        tag.putInt("ActiveTeslaGates", activeTeslaGates);
        tag.putInt("RegisteredTeslaGates", registeredTeslaGates);
        tag.putBoolean("TeslaOverride", teslaOverride);
        tag.putInt("ConnectedDoors", connectedDoors);
        tag.putBoolean("AuxiliaryPowerOnline", auxiliaryPowerOnline);
        tag.putBoolean("UnusualNetworkActivity", unusualNetworkActivity);
        tag.putLong("CachePurgeEndGameTime", cachePurgeEndGameTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        analyzed = tag.getBoolean("Analyzed");
        uncontainedScps = Math.max(0, tag.getInt("UncontainedScps"));
        activeTeslaGates = Math.max(0, tag.getInt("ActiveTeslaGates"));
        registeredTeslaGates = Math.max(0,
                tag.getInt("RegisteredTeslaGates"));
        teslaOverride = tag.getBoolean("TeslaOverride");
        connectedDoors = Math.max(0, tag.getInt("ConnectedDoors"));
        auxiliaryPowerOnline = tag.getBoolean("AuxiliaryPowerOnline");
        unusualNetworkActivity = tag.getBoolean("UnusualNetworkActivity");
        cachePurgeEndGameTime = Math.max(0L,
                tag.getLong("CachePurgeEndGameTime"));
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

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        // Static model. GeckoLib is used for geometry, glowmask, and no-cull.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
