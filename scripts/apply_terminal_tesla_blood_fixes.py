from __future__ import annotations

from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one match in {path}, found {count}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


# Keep the client-side SCP Inventory capability authoritative immediately after
# connection lifecycle changes, rather than waiting for the inventory screen to
# request its first snapshot.
replace_once(
    "src/main/java/com/bl4ues/scpinventory/events/InventoryModuleStateEvents.java",
    '''    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ModNetwork.syncModuleState(player);
        ModNetwork.syncServerConfig(player);
        updateDisabledState(player);
    }
''',
    '''    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ModNetwork.syncModuleState(player);
        ModNetwork.syncServerConfig(player);
        updateDisabledState(player);
        syncInventory(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncInventory(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncInventory(player);
        }
    }
''')
replace_once(
    "src/main/java/com/bl4ues/scpinventory/events/InventoryModuleStateEvents.java",
    '''    private static void updateDisabledState(ServerPlayer player) {
''',
    '''    private static void syncInventory(ServerPlayer player) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(
                inventory -> ModNetwork.syncTo(player, inventory));
    }

    private static void updateDisabledState(ServerPlayer player) {
''')

# Send a server-authoritative credential snapshot as part of opening the Tesla
# Terminal. The client capability remains useful for live updates, but opening
# the unrelated Keys panel is no longer a prerequisite for authentication.
replace_once(
    "src/main/java/net/mcreator/scpadditions/block/TeslaTerminalBlockBlock.java",
    '''import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
''',
    '''import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.procedures.TeslaTerminalController;
''')
replace_once(
    "src/main/java/net/mcreator/scpadditions/block/TeslaTerminalBlockBlock.java",
    '''\t\tif (entity instanceof ServerPlayer player) {
\t\t\tboolean teslaOn = world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEON);
\t\t\tboolean manualOverride = world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
''',
    '''\t\tif (entity instanceof ServerPlayer player) {
\t\t\tboolean teslaOn = world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEON);
\t\t\tboolean manualOverride = world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
\t\t\tboolean hasSecurityCredentials = TeslaTerminalController.hasSecurityCredentials(player);
''')
replace_once(
    "src/main/java/net/mcreator/scpadditions/block/TeslaTerminalBlockBlock.java",
    '''\t\t\t\t\tdata.writeBoolean(teslaOn);
\t\t\t\t\tdata.writeBoolean(manualOverride);
\t\t\t\t\treturn new TeslaTerminalMenu(id, inventory, data);
''',
    '''\t\t\t\t\tdata.writeBoolean(teslaOn);
\t\t\t\t\tdata.writeBoolean(manualOverride);
\t\t\t\t\tdata.writeBoolean(hasSecurityCredentials);
\t\t\t\t\treturn new TeslaTerminalMenu(id, inventory, data);
''')
replace_once(
    "src/main/java/net/mcreator/scpadditions/block/TeslaTerminalBlockBlock.java",
    '''\t\t\t\tdata.writeBlockPos(pos);
\t\t\t\tdata.writeBoolean(teslaOn);
\t\t\t\tdata.writeBoolean(manualOverride);
\t\t\t});
''',
    '''\t\t\t\tdata.writeBlockPos(pos);
\t\t\t\tdata.writeBoolean(teslaOn);
\t\t\t\tdata.writeBoolean(manualOverride);
\t\t\t\tdata.writeBoolean(hasSecurityCredentials);
\t\t\t});
''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/world/inventory/TeslaTerminalMenu.java",
    '''\tpublic boolean initialTeslaGatesEnabled = true;
\tpublic boolean initialManualOverride = false;
''',
    '''\tpublic boolean initialTeslaGatesEnabled = true;
\tpublic boolean initialManualOverride = false;
\tpublic boolean initialHasSecurityCredentials = false;
''')
replace_once(
    "src/main/java/net/mcreator/scpadditions/world/inventory/TeslaTerminalMenu.java",
    '''\t\t\tif (extraData.readableBytes() >= 2) {
\t\t\t\tthis.initialTeslaGatesEnabled = extraData.readBoolean();
\t\t\t\tthis.initialManualOverride = extraData.readBoolean();
\t\t\t}
''',
    '''\t\t\tif (extraData.readableBytes() >= 2) {
\t\t\t\tthis.initialTeslaGatesEnabled = extraData.readBoolean();
\t\t\t\tthis.initialManualOverride = extraData.readBoolean();
\t\t\t}
\t\t\tif (extraData.readableBytes() >= 1) {
\t\t\t\tthis.initialHasSecurityCredentials = extraData.readBoolean();
\t\t\t}
''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/client/gui/TeslaTerminalScreen.java",
    '''import com.mojang.blaze3d.systems.RenderSystem;
''',
    '''import com.bl4ues.scpinventory.client.ClientNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
''')
replace_once(
    "src/main/java/net/mcreator/scpadditions/client/gui/TeslaTerminalScreen.java",
    '''\tpublic TeslaTerminalScreen(TeslaTerminalMenu container, Inventory inventory, Component text) {
\t\tsuper(container, inventory, text);
\t\tthis.world = container.world;
\t\tthis.x = container.x;
\t\tthis.y = container.y;
\t\tthis.z = container.z;
\t\tthis.entity = container.entity;
\t\tthis.imageWidth = TEX_W;
\t\tthis.imageHeight = TEX_H;
\t}
''',
    '''\tpublic TeslaTerminalScreen(TeslaTerminalMenu container, Inventory inventory, Component text) {
\t\tsuper(container, inventory, text);
\t\tthis.world = container.world;
\t\tthis.x = container.x;
\t\tthis.y = container.y;
\t\tthis.z = container.z;
\t\tthis.entity = container.entity;
\t\tthis.imageWidth = TEX_W;
\t\tthis.imageHeight = TEX_H;
\t}

\t@Override
\tprotected void init() {
\t\tsuper.init();
\t\tClientNetwork.requestInventorySync();
\t}
''')
replace_once(
    "src/main/java/net/mcreator/scpadditions/client/gui/TeslaTerminalScreen.java",
    '''\tprivate boolean hasCredentialsItem() {
\t\treturn TeslaTerminalController.hasSecurityCredentials(entity);
\t}
''',
    '''\tprivate boolean hasCredentialsItem() {
\t\treturn menu.initialHasSecurityCredentials
\t\t\t\t|| TeslaTerminalController.hasSecurityCredentials(entity);
\t}
''')

# Assign one blood type on first login, preserve it through death, and repair old
# saves where all legacy boolean flags remained false.
Path("src/main/java/net/mcreator/scpadditions/procedures/BloodType1Procedure.java").write_text(
'''package net.mcreator.scpadditions.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

@Mod.EventBusSubscriber
public final class BloodType1Procedure {
    private BloodType1Procedure() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ensureBloodType(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        ensureBloodType(event.getEntity());
    }

    public static void execute(Entity entity) {
        ensureBloodType(entity);
    }

    private static void ensureBloodType(Entity entity) {
        if (entity == null || entity.level().isClientSide()) return;

        entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY)
                .ifPresent(variables -> {
                    if (!hasBloodType(variables)) {
                        clearBloodType(variables);
                        switch (entity.getRandom().nextInt(8)) {
                            case 0 -> variables.Oneg = true;
                            case 1 -> variables.Opos = true;
                            case 2 -> variables.Aneg = true;
                            case 3 -> variables.Apos = true;
                            case 4 -> variables.Bneg = true;
                            case 5 -> variables.Bpos = true;
                            case 6 -> variables.ABneg = true;
                            default -> variables.ABpos = true;
                        }
                    }
                    variables.syncPlayerVariables(entity);
                });
    }

    private static boolean hasBloodType(
            ScpAdditionsModVariables.PlayerVariables variables) {
        return variables.Oneg || variables.Opos
                || variables.Aneg || variables.Apos
                || variables.Bneg || variables.Bpos
                || variables.ABneg || variables.ABpos;
    }

    private static void clearBloodType(
            ScpAdditionsModVariables.PlayerVariables variables) {
        variables.Oneg = false;
        variables.Opos = false;
        variables.Aneg = false;
        variables.Apos = false;
        variables.Bneg = false;
        variables.Bpos = false;
        variables.ABneg = false;
        variables.ABpos = false;
    }
}
''', encoding="utf-8")

replace_once(
    "src/main/java/net/mcreator/scpadditions/network/ScpAdditionsModVariables.java",
    '''\t\t\tPlayerVariables original = ((PlayerVariables) event.getOriginal().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()));
\t\t\tPlayerVariables clone = ((PlayerVariables) event.getEntity().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()));
\t\t\tif (!event.isWasDeath()) {
\t\t\t\tclone.Opos = original.Opos;
\t\t\t\tclone.Oneg = original.Oneg;
\t\t\t\tclone.Apos = original.Apos;
\t\t\t\tclone.Aneg = original.Aneg;
\t\t\t\tclone.Bpos = original.Bpos;
\t\t\t\tclone.Bneg = original.Bneg;
\t\t\t\tclone.ABpos = original.ABpos;
\t\t\t\tclone.ABneg = original.ABneg;
\t\t\t\tclone.PlayerOn1to1 = original.PlayerOn1to1;
''',
    '''\t\t\tPlayerVariables original = ((PlayerVariables) event.getOriginal().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()));
\t\t\tPlayerVariables clone = ((PlayerVariables) event.getEntity().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()));
\t\t\tclone.Opos = original.Opos;
\t\t\tclone.Oneg = original.Oneg;
\t\t\tclone.Apos = original.Apos;
\t\t\tclone.Aneg = original.Aneg;
\t\t\tclone.Bpos = original.Bpos;
\t\t\tclone.Bneg = original.Bneg;
\t\t\tclone.ABpos = original.ABpos;
\t\t\tclone.ABneg = original.ABneg;
\t\t\tif (!event.isWasDeath()) {
\t\t\t\tclone.PlayerOn1to1 = original.PlayerOn1to1;
''')

# Motion-aware Tesla footprints prevent an entity from tunneling through the
# exact visible arc between server ticks. The query margin only finds candidates;
# the final test remains the unexpanded arc swept across the entity's trajectory.
replace_once(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGateVolume.java",
    '''import net.minecraft.world.phys.AABB;
''',
    '''import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
''')
replace_once(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGateVolume.java",
    '''    private static final double ARC_DEPTH_MAX = 1.0D;
''',
    '''    private static final double ARC_DEPTH_MAX = 1.0D;
    private static final double MOTION_QUERY_MARGIN = 2.0D;
''')
replace_once(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGateVolume.java",
    '''    public static boolean intersects(Entity entity, AABB volume) {
        return entity != null && entity.isAlive()
                && entity.getBoundingBox().intersects(volume);
    }
''',
    '''    public static AABB motionCandidates(AABB volume) {
        return volume.inflate(MOTION_QUERY_MARGIN);
    }

    public static boolean intersects(Entity entity, AABB volume) {
        return entity != null && entity.isAlive()
                && entity.getBoundingBox().intersects(volume);
    }

    public static boolean intersectsOrCrossed(Entity entity, AABB volume) {
        if (!entity.isAlive()) return false;
        if (entity.getBoundingBox().intersects(volume)) return true;

        double halfWidth = Math.max(0.01D, entity.getBbWidth() * 0.5D);
        double halfHeight = Math.max(0.01D, entity.getBbHeight() * 0.5D);
        AABB centerPathTarget = volume.inflate(halfWidth, halfHeight,
                halfWidth);
        Vec3 previousCenter = new Vec3(entity.xo,
                entity.yo + halfHeight, entity.zo);
        Vec3 currentCenter = new Vec3(entity.getX(),
                entity.getY() + halfHeight, entity.getZ());
        return centerPathTarget.contains(previousCenter)
                || centerPathTarget.contains(currentCenter)
                || centerPathTarget.clip(previousCenter, currentCenter)
                .isPresent();
    }
''')

Path("src/main/java/net/mcreator/scpadditions/procedures/TeslaGateUpdateTickProcedure.java").write_text(
'''package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.FacilityStructureBreakGuard;
import net.mcreator.scpadditions.facility.Scp079TeslaSuppression;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

import java.util.List;

public class TeslaGateUpdateTickProcedure {
    public static boolean execute(LevelAccessor world, double x, double y,
            double z) {
        BlockPos gatePos = BlockPos.containing(x, y, z);
        if (FacilityStructureBreakGuard.isBeingMined(world, gatePos)) {
            return false;
        }

        boolean manualOverride = world.getLevelData().getGameRules()
                .getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
        boolean teslaGateOn = world.getLevelData().getGameRules()
                .getBoolean(ScpAdditionsModGameRules.TESLAGATEON);
        if (manualOverride && !teslaGateOn
                && world instanceof Level level && !level.isClientSide()) {
            world.getLevelData().getGameRules()
                    .getRule(ScpAdditionsModGameRules.TESLAGATEON)
                    .set(true, level.getServer());
            teslaGateOn = true;
        }
        if (!teslaGateOn && !manualOverride) {
            return false;
        }

        int activationDelay = manualOverride ? 1 : 5;
        ResourceLocation activationSound = new ResourceLocation(
                "scp_additions", manualOverride
                ? "overcharge" : "teslaactivate");
        float activationVolume = manualOverride ? 2.0F : 1.0F;

        AABB detectionVolume = TeslaGateVolume.at(x, y, z);
        List<LivingEntity> occupants = world.getEntitiesOfClass(
                LivingEntity.class,
                TeslaGateVolume.motionCandidates(detectionVolume),
                entity -> TeslaGateVolume.intersectsOrCrossed(entity,
                        detectionVolume));
        if (occupants.isEmpty()) {
            return false;
        }

        AABB lethalVolume = TeslaGateVolume.lethalArcAt(world, gatePos);
        List<LivingEntity> lethalOccupants = occupants.stream()
                .filter(entity -> TeslaGateVolume.intersectsOrCrossed(entity,
                        lethalVolume))
                .toList();
        if (world instanceof ServerLevel server
                && Scp079TeslaSuppression.shouldSuppress(server, gatePos,
                occupants, lethalOccupants, manualOverride)) {
            return false;
        }

        if (world instanceof Level level && !level.isClientSide()) {
            level.playSound(null, gatePos,
                    ForgeRegistries.SOUND_EVENTS.getValue(activationSound),
                    SoundSource.HOSTILE, activationVolume,
                    manualOverride ? 1.25F : 1.0F);
        }
        ScpAdditionsMod.queueServerWork(activationDelay,
                () -> TeslaGateTransitionHelper.transitionIfCurrent(
                        world, x, y, z,
                        ScpAdditionsModBlocks.TESLA_GATE,
                        ScpAdditionsModBlocks.TESLA_ACTIVE));
        return true;
    }
}
''', encoding="utf-8")

replace_once(
    "src/main/java/net/mcreator/scpadditions/block/TeslaGateBlock.java",
    '''public class TeslaGateBlock extends Block implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
''',
    '''public class TeslaGateBlock extends Block implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int SENSOR_INTERVAL_TICKS = 1;
''')
replace_once(
    "src/main/java/net/mcreator/scpadditions/block/TeslaGateBlock.java",
    '''        super.onPlace(state, level, pos, oldState, moving);
        level.scheduleTick(pos, this, 10);
        TeslaGateUpdateTickProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
    }
''',
    '''        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide()) {
            boolean activationQueued = TeslaGateUpdateTickProcedure.execute(
                    level, pos.getX(), pos.getY(), pos.getZ());
            if (!activationQueued && isEnabled(level)) {
                level.scheduleTick(pos, this, SENSOR_INTERVAL_TICKS);
            }
        }
    }
''')
replace_once(
    "src/main/java/net/mcreator/scpadditions/block/TeslaGateBlock.java",
    '''    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TeslaGateUpdateTickProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
        if (level.getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEON)
                || level.getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE)) {
            level.scheduleTick(pos, this, 10);
        }
    }
}
''',
    '''    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        boolean activationQueued = TeslaGateUpdateTickProcedure.execute(
                level, pos.getX(), pos.getY(), pos.getZ());
        if (!activationQueued && isEnabled(level)) {
            level.scheduleTick(pos, this, SENSOR_INTERVAL_TICKS);
        }
    }

    private static boolean isEnabled(Level level) {
        return level.getGameRules().getBoolean(
                ScpAdditionsModGameRules.TESLAGATEON)
                || level.getGameRules().getBoolean(
                ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
    }
}
''')

replace_once(
    "src/main/java/net/mcreator/scpadditions/procedures/TeslaGatePulseHelper.java",
    '''        AABB lethalVolume = TeslaGateVolume.lethalArcAt(world, pos);
        List<LivingEntity> entities = world.getEntitiesOfClass(
                LivingEntity.class, lethalVolume,
                entity -> TeslaGateVolume.intersects(entity, lethalVolume));
''',
    '''        AABB lethalVolume = TeslaGateVolume.lethalArcAt(world, pos);
        List<LivingEntity> entities = world.getEntitiesOfClass(
                LivingEntity.class,
                TeslaGateVolume.motionCandidates(lethalVolume),
                entity -> TeslaGateVolume.intersectsOrCrossed(entity,
                        lethalVolume));
''')

replace_once(
    "CHANGELOG.md",
    '''## Bug Fixes

- Removed the actual source of repeated ceiling-lamp clicks: the old loop file contained embedded power-on and power-off samples, which compounded across nearby lamps;
''',
    '''## Bug Fixes

- Synchronized the SCP Inventory immediately on login, respawn, and dimension changes, and made Tesla Terminal authentication receive a server-authoritative Security Credentials snapshot when opened;
- Changed enabled Tesla Gates from ten-tick polling to continuous sensing without duplicate activation queues, and added swept trajectory checks so running entities cannot tunnel through the unchanged visible discharge arc between ticks;
- Assigned a blood type on first login for new and legacy players and preserved it across death instead of leaving Status as Unknown until the first respawn;
- Removed the actual source of repeated ceiling-lamp clicks: the old loop file contained embedded power-on and power-off samples, which compounded across nearby lamps;
''')

print("Applied terminal, Tesla Gate, and blood type fixes")
