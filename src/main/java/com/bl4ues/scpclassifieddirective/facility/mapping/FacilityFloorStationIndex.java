package com.bl4ues.scpclassifieddirective.facility.mapping;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorModule;
import com.bl4ues.scpclassifieddirective.facility.elevator.ElevatorArrivalDisplayData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Keeps a persistent, dimension-aware index of configured Core Room floors. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FacilityFloorStationIndex {
    private FacilityFloorStationIndex() {
    }

    public static void refresh(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        FacilityMappingSavedData data = FacilityMappingSavedData.get(
                level.getServer());
        if (!level.getBlockState(pos).is(CoreRoomElevatorModule.STATION.get())) {
            data.removeStation(tracked(level, pos, "", ""));
            return;
        }
        FacilityFloorStationOption option = read(level, pos);
        data.putStation(tracked(level, pos,
                option == null ? "" : option.longLabel(),
                option == null ? "" : option.shortLabel()));
    }

    public static void refreshLoaded(ServerLevel level) {
        FacilityMappingSavedData data = FacilityMappingSavedData.get(
                level.getServer());
        for (FacilityMappingSavedData.TrackedStation station
                : List.copyOf(data.stations())) {
            if (!station.dimension().equals(level.dimension().location())) continue;
            BlockPos pos = BlockPos.of(station.packedPos());
            if (level.hasChunkAt(pos)) refresh(level, pos);
        }
    }

    public static List<FacilityFloorStationOption> configured(
            ServerLevel level) {
        ResourceLocation dimension = level.dimension().location();
        List<FacilityFloorStationOption> options = new ArrayList<>();
        for (FacilityMappingSavedData.TrackedStation station
                : FacilityMappingSavedData.get(level.getServer()).stations()) {
            if (!station.dimension().equals(dimension)
                    || station.longLabel().isBlank()) continue;
            options.add(new FacilityFloorStationOption(
                    BlockPos.of(station.packedPos()), station.longLabel(),
                    station.shortLabel()));
        }
        options.sort(Comparator.comparingInt(option -> option.pos().getY()));
        return List.copyOf(options);
    }

    public static BlockPos nearest(ServerLevel level, int y) {
        return configured(level).stream()
                .min(Comparator.comparingInt(option ->
                        Math.abs(option.pos().getY() - y)))
                .map(FacilityFloorStationOption::pos).orElse(null);
    }

    public static FacilityMappingSavedData.TrackedStation indexed(
            ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return null;
        ResourceLocation dimension = level.dimension().location();
        long packed = pos.asLong();
        return FacilityMappingSavedData.get(level.getServer()).stations().stream()
                .filter(station -> station.dimension().equals(dimension)
                        && station.packedPos() == packed)
                .findFirst().orElse(null);
    }

    public static boolean contains(ServerLevel level, BlockPos pos) {
        return indexed(level, pos) != null;
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) return;
        rescanChunk(level, chunk);
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getPlacedBlock().is(CoreRoomElevatorModule.STATION.get())) {
            refresh(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getState().is(CoreRoomElevatorModule.STATION.get())) {
            FacilityMappingSavedData.get(level.getServer()).removeStation(
                    tracked(level, event.getPos(), "", ""));
        }
    }

    /**
     * Rescans only the chunk supplied by ChunkEvent.Load.
     *
     * <p>Do not route positions found here through {@link #refresh(ServerLevel,
     * BlockPos)}. ChunkEvent.Load fires while a chunk is still being promoted to
     * a full chunk, and Level#getBlockState can synchronously request that chunk
     * again through ServerChunkCache. That re-entrant chunk request deadlocks the
     * integrated server during spawn preparation. Reading the section state and
     * block entity directly from the already supplied LevelChunk keeps the scan
     * entirely local to the chunk being loaded.</p>
     */
    private static void rescanChunk(ServerLevel level, LevelChunk chunk) {
        FacilityMappingSavedData data = FacilityMappingSavedData.get(
                level.getServer());
        data.removeStationsInChunk(level.dimension().location(),
                chunk.getPos().x, chunk.getPos().z);
        LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length;
                sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()
                    || !section.maybeHas(state -> state.is(
                    CoreRoomElevatorModule.STATION.get()))) continue;
            int baseY = level.getMinBuildHeight() + sectionIndex * 16;
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        BlockState state = section.getBlockState(localX,
                                localY, localZ);
                        if (!state.is(CoreRoomElevatorModule.STATION.get())) continue;
                        BlockPos pos = new BlockPos(
                                chunk.getPos().getMinBlockX() + localX,
                                baseY + localY,
                                chunk.getPos().getMinBlockZ() + localZ);
                        FacilityFloorStationOption option = read(chunk, pos);
                        data.putStation(tracked(level, pos,
                                option == null ? "" : option.longLabel(),
                                option == null ? "" : option.shortLabel()));
                    }
                }
            }
        }
    }

    private static FacilityFloorStationOption read(ServerLevel level,
            BlockPos pos) {
        if (!(level.getBlockEntity(pos)
                instanceof CoreRoomElevatorModule.StationBlockEntity station)) {
            return null;
        }
        return read(pos, station);
    }

    private static FacilityFloorStationOption read(LevelChunk chunk,
            BlockPos pos) {
        BlockEntity blockEntity = chunk.getBlockEntity(pos);
        if (!(blockEntity
                instanceof CoreRoomElevatorModule.StationBlockEntity station)) {
            return null;
        }
        return read(pos, station);
    }

    private static FacilityFloorStationOption read(BlockPos pos,
            CoreRoomElevatorModule.StationBlockEntity station) {
        ElevatorArrivalDisplayData display = station.arrivalDisplay();
        if (display == null || !display.enabled()) return null;
        String zone = display.zone() == ElevatorArrivalDisplayData.Zone.CUSTOM
                ? display.customZone() : display.zone().displayName();
        if (zone == null || zone.isBlank()) return null;
        String longLabel = zone + " - " + display.floorType().displayName()
                + " " + display.floorNumber();
        String shortFloor = display.floorType()
                == ElevatorArrivalDisplayData.FloorType.SUBLEVEL
                ? "SL" + display.floorNumber() : "L" + display.floorNumber();
        return new FacilityFloorStationOption(pos, longLabel,
                abbreviateZone(display, zone) + " - " + shortFloor);
    }

    private static String abbreviateZone(ElevatorArrivalDisplayData display,
            String zone) {
        if (display.zone() != ElevatorArrivalDisplayData.Zone.CUSTOM) {
            return switch (display.zone()) {
                case ENTRANCE_ZONE -> "EZ";
                case LIGHT_CONTAINMENT_ZONE -> "LCZ";
                case HEAVY_CONTAINMENT_ZONE -> "HCZ";
                case SUPER_HEAVY_CONTAINMENT_ZONE -> "SHCZ";
                case CUSTOM -> "CUSTOM";
            };
        }
        StringBuilder abbreviation = new StringBuilder();
        for (String word : zone.strip().split("\\s+")) {
            if (!word.isBlank() && Character.isLetterOrDigit(word.charAt(0))) {
                abbreviation.append(Character.toUpperCase(word.charAt(0)));
                if (abbreviation.length() >= 5) break;
            }
        }
        return abbreviation.isEmpty()
                ? zone.toUpperCase(Locale.ROOT) : abbreviation.toString();
    }

    private static FacilityMappingSavedData.TrackedStation tracked(
            ServerLevel level, BlockPos pos, String longLabel,
            String shortLabel) {
        return new FacilityMappingSavedData.TrackedStation(
                level.dimension().location(), pos.asLong(), longLabel,
                shortLabel);
    }
}
