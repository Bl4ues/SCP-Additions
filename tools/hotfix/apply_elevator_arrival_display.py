from __future__ import annotations

from pathlib import Path
from urllib.request import Request, urlopen
import time

ROOT = Path(__file__).resolve().parents[2]


def replace_once(path: str, old: str, new: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one match in {path}, found {count}: {old[:80]!r}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


def insert_before_last_brace(path: str, block: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    index = text.rfind("\n}")
    if index < 0:
        raise RuntimeError(f"Could not locate final class brace in {path}")
    target.write_text(text[:index] + block + text[index:], encoding="utf-8")


def download_font() -> None:
    destination = ROOT / "src/main/resources/assets/scp_additions/titillium_web_regular.ttf"
    destination.parent.mkdir(parents=True, exist_ok=True)
    url = (
        "https://raw.githubusercontent.com/google/fonts/main/ofl/"
        "titilliumweb/TitilliumWeb-Regular.ttf"
    )
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            request = Request(url, headers={"User-Agent": "SCP-Additions-CI"})
            with urlopen(request, timeout=45) as response:
                payload = response.read()
            if len(payload) < 50_000 or payload[:4] not in (b"\x00\x01\x00\x00", b"OTTO"):
                raise RuntimeError(
                    f"Downloaded Titillium Web payload is invalid ({len(payload)} bytes)"
                )
            destination.write_bytes(payload)
            return
        except Exception as error:  # pragma: no cover - CI network retry
            last_error = error
            time.sleep(2 + attempt * 2)
    raise RuntimeError("Unable to download Titillium Web Regular") from last_error


def patch_fonts() -> None:
    path = "src/main/java/net/mcreator/scpadditions/client/ScpFonts.java"
    replace_once(
        path,
        '    public static final ResourceLocation NOTO_SANS_BOLD =\n'
        '            new ResourceLocation("scp_additions", "noto_sans_bold");\n',
        '    public static final ResourceLocation NOTO_SANS_BOLD =\n'
        '            new ResourceLocation("scp_additions", "noto_sans_bold");\n'
        '    public static final ResourceLocation TITILLIUM_WEB =\n'
        '            new ResourceLocation("scp_additions", "titillium_web");\n',
    )
    replace_once(
        path,
        '    public static MutableComponent scpSign(String text) {\n'
        '        return custom(text, NOTO_SANS_BOLD);\n'
        '    }\n\n'
        '    private static MutableComponent custom(String text, ResourceLocation font) {',
        '    public static MutableComponent scpSign(String text) {\n'
        '        return custom(text, NOTO_SANS_BOLD);\n'
        '    }\n\n'
        '    public static MutableComponent titillium(String text) {\n'
        '        return custom(text, TITILLIUM_WEB);\n'
        '    }\n\n'
        '    private static MutableComponent custom(String text, ResourceLocation font) {',
    )


def patch_protocol_and_network() -> None:
    replace_once(
        "src/main/java/net/mcreator/scpadditions/ScpAdditionsMod.java",
        '    private static final String PROTOCOL_VERSION = "15";',
        '    private static final String PROTOCOL_VERSION = "16";',
    )
    path = "src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java"
    replace_once(
        path,
        '        ScpAdditionsMod.addNetworkMessage(ScpSignSavePacket.class,\n'
        '                ScpSignSavePacket::encode,\n'
        '                ScpSignSavePacket::decode,\n'
        '                ScpSignSavePacket::handle);\n',
        '        ScpAdditionsMod.addNetworkMessage(ScpSignSavePacket.class,\n'
        '                ScpSignSavePacket::encode,\n'
        '                ScpSignSavePacket::decode,\n'
        '                ScpSignSavePacket::handle);\n'
        '        ScpAdditionsMod.addNetworkMessage(\n'
        '                ElevatorArrivalOpenScreenPacket.class,\n'
        '                ElevatorArrivalOpenScreenPacket::encode,\n'
        '                ElevatorArrivalOpenScreenPacket::decode,\n'
        '                ElevatorArrivalOpenScreenPacket::handle);\n'
        '        ScpAdditionsMod.addNetworkMessage(ElevatorArrivalSavePacket.class,\n'
        '                ElevatorArrivalSavePacket::encode,\n'
        '                ElevatorArrivalSavePacket::decode,\n'
        '                ElevatorArrivalSavePacket::handle);\n'
        '        ScpAdditionsMod.addNetworkMessage(\n'
        '                ElevatorArrivalDisplayPacket.class,\n'
        '                ElevatorArrivalDisplayPacket::encode,\n'
        '                ElevatorArrivalDisplayPacket::decode,\n'
        '                ElevatorArrivalDisplayPacket::handle);\n',
    )
    insert_before_last_brace(
        path,
        '\n\n    public static void openElevatorArrivalEditor(ServerPlayer player,\n'
        '            BlockPos stationPos,\n'
        '            net.mcreator.scpadditions.facility.elevator.\n'
        '                    ElevatorArrivalDisplayData data) {\n'
        '        if (player == null) return;\n'
        '        ScpAdditionsMod.PACKET_HANDLER.send(\n'
        '                PacketDistributor.PLAYER.with(() -> player),\n'
        '                new ElevatorArrivalOpenScreenPacket(stationPos, data));\n'
        '    }\n\n'
        '    public static void showElevatorArrival(ServerPlayer player,\n'
        '            net.mcreator.scpadditions.facility.elevator.\n'
        '                    ElevatorArrivalDisplayData data) {\n'
        '        if (player == null || data == null || !data.enabled()) return;\n'
        '        ScpAdditionsMod.PACKET_HANDLER.send(\n'
        '                PacketDistributor.PLAYER.with(() -> player),\n'
        '                new ElevatorArrivalDisplayPacket(data));\n'
        '    }\n',
    )


def patch_station() -> None:
    path = (
        "src/main/java/net/mcreator/scpadditions/facility/elevator/"
        "CoreRoomElevatorModule.java"
    )
    replace_once(
        path,
        'import net.minecraft.nbt.CompoundTag;\n',
        'import net.minecraft.nbt.CompoundTag;\nimport net.minecraft.nbt.Tag;\n',
    )
    replace_once(
        path,
        'import net.mcreator.scpadditions.ScpAdditionsMod;\n',
        'import net.mcreator.scpadditions.ScpAdditionsMod;\n'
        'import net.mcreator.scpadditions.init.UnifiedReaderItems;\n'
        'import net.mcreator.scpadditions.network.ScpEntityNetwork;\n',
    )
    replace_once(
        path,
        '        public InteractionResult use(BlockState state, Level level, BlockPos pos,\n'
        '                Player player, InteractionHand hand, BlockHitResult hit) {\n'
        '            Vec3 upButton = CoreRoomElevatorGeometry.stationButtonWorld(\n',
        '        public InteractionResult use(BlockState state, Level level, BlockPos pos,\n'
        '                Player player, InteractionHand hand, BlockHitResult hit) {\n'
        '            if (player.getItemInHand(hand).is(\n'
        '                    UnifiedReaderItems.SCREWDRIVER.get())) {\n'
        '                if (!level.isClientSide\n'
        '                        && player instanceof ServerPlayer serverPlayer\n'
        '                        && level.getBlockEntity(pos)\n'
        '                        instanceof StationBlockEntity station) {\n'
        '                    ScpEntityNetwork.openElevatorArrivalEditor(\n'
        '                            serverPlayer, pos, station.arrivalDisplay());\n'
        '                }\n'
        '                return InteractionResult.sidedSuccess(level.isClientSide);\n'
        '            }\n'
        '            Vec3 upButton = CoreRoomElevatorGeometry.stationButtonWorld(\n',
    )
    replace_once(
        path,
        '        private DoorVisualState doorState = DoorVisualState.CLOSED;\n'
        '        private int doorTicks = DOOR_TICKS;\n'
        '        private boolean initialized;\n',
        '        private DoorVisualState doorState = DoorVisualState.CLOSED;\n'
        '        private int doorTicks = DOOR_TICKS;\n'
        '        private boolean initialized;\n'
        '        private ElevatorArrivalDisplayData arrivalDisplay =\n'
        '                ElevatorArrivalDisplayData.NONE;\n',
    )
    replace_once(
        path,
        '        public DoorVisualState doorState() {\n'
        '            return doorState;\n'
        '        }\n\n'
        '        public boolean isGateCollisionSolid() {',
        '        public DoorVisualState doorState() {\n'
        '            return doorState;\n'
        '        }\n\n'
        '        public ElevatorArrivalDisplayData arrivalDisplay() {\n'
        '            return arrivalDisplay;\n'
        '        }\n\n'
        '        public void setArrivalDisplay(\n'
        '                ElevatorArrivalDisplayData data) {\n'
        '            arrivalDisplay = data == null\n'
        '                    ? ElevatorArrivalDisplayData.NONE : data;\n'
        '            setChanged();\n'
        '            if (level != null) {\n'
        '                level.sendBlockUpdated(worldPosition, getBlockState(),\n'
        '                        getBlockState(), Block.UPDATE_ALL);\n'
        '            }\n'
        '        }\n\n'
        '        public boolean isGateCollisionSolid() {',
    )
    replace_once(
        path,
        '            tag.putByte("DoorState", (byte) doorState.ordinal());\n'
        '            tag.putInt("DoorTicks", doorTicks);\n',
        '            tag.putByte("DoorState", (byte) doorState.ordinal());\n'
        '            tag.putInt("DoorTicks", doorTicks);\n'
        '            if (arrivalDisplay.enabled()) {\n'
        '                tag.put("ArrivalDisplay", arrivalDisplay.save());\n'
        '            }\n',
    )
    replace_once(
        path,
        '            doorTicks = tag.contains("DoorTicks")\n'
        '                    ? Mth.clamp(tag.getInt("DoorTicks"), 0, DOOR_TICKS)\n'
        '                    : DOOR_TICKS;\n',
        '            doorTicks = tag.contains("DoorTicks")\n'
        '                    ? Mth.clamp(tag.getInt("DoorTicks"), 0, DOOR_TICKS)\n'
        '                    : DOOR_TICKS;\n'
        '            arrivalDisplay = tag.contains("ArrivalDisplay", Tag.TAG_COMPOUND)\n'
        '                    ? ElevatorArrivalDisplayData.load(\n'
        '                            tag.getCompound("ArrivalDisplay"))\n'
        '                    : ElevatorArrivalDisplayData.NONE;\n',
    )


def patch_carriage() -> None:
    path = (
        "src/main/java/net/mcreator/scpadditions/facility/elevator/"
        "CoreRoomElevatorCarriageEntity.java"
    )
    replace_once(
        path,
        'import net.minecraftforge.network.NetworkHooks;\n',
        'import net.minecraftforge.network.NetworkHooks;\n'
        'import net.mcreator.scpadditions.network.ScpEntityNetwork;\n',
    )
    replace_once(
        path,
        '            case LEVELING -> {\n'
        '                if (phaseTicks >= LEVELING_TICKS) {\n'
        '                    playElevatorSound(\n',
        '            case LEVELING -> {\n'
        '                if (phaseTicks >= LEVELING_TICKS) {\n'
        '                    triggerArrivalDisplay(serverLevel);\n'
        '                    playElevatorSound(\n',
    )
    replace_once(
        path,
        '    private void playElevatorSound(net.minecraft.sounds.SoundEvent sound,\n'
        '            float volume) {\n',
        '    private void triggerArrivalDisplay(ServerLevel serverLevel) {\n'
        '        int floor = currentFloorIndex();\n'
        '        if (floor < 0 || floor >= floorHeights.length) return;\n'
        '        BlockPos stationPos = new BlockPos(columnX(),\n'
        '                floorHeights[floor], columnZ());\n'
        '        if (!(serverLevel.getBlockEntity(stationPos)\n'
        '                instanceof CoreRoomElevatorModule.StationBlockEntity station)) {\n'
        '            return;\n'
        '        }\n'
        '        ElevatorArrivalDisplayData data = station.arrivalDisplay();\n'
        '        if (!data.enabled() || data.sectorLabel().isBlank()) return;\n'
        '        AABB passengers = cabinInteriorBox().inflate(0.18D, 0.12D,\n'
        '                0.18D);\n'
        '        for (ServerPlayer player : serverLevel.players()) {\n'
        '            if (!player.isAlive() || player.isSpectator()\n'
        '                    || !passengers.intersects(player.getBoundingBox())) {\n'
        '                continue;\n'
        '            }\n'
        '            ScpEntityNetwork.showElevatorArrival(player, data);\n'
        '        }\n'
        '    }\n\n'
        '    private void playElevatorSound(net.minecraft.sounds.SoundEvent sound,\n'
        '            float volume) {\n',
    )


def patch_editor_state() -> None:
    path = (
        "src/main/java/net/mcreator/scpadditions/client/gui/"
        "ElevatorArrivalEditorScreen.java"
    )
    replace_once(
        path,
        '    private final boolean configured;\n'
        '    private ElevatorArrivalDisplayData.Zone zone;\n'
        '    private ElevatorArrivalDisplayData.FloorType floorType;\n',
        '    private final boolean configured;\n'
        '    private final String initialCustomZone;\n'
        '    private final int initialFloorNumber;\n'
        '    private ElevatorArrivalDisplayData.Zone zone;\n'
        '    private ElevatorArrivalDisplayData.FloorType floorType;\n',
    )
    replace_once(
        path,
        '        this.configured = data != null && data.enabled();\n'
        '        this.zone = initial.zone();\n'
        '        this.floorType = initial.floorType();\n',
        '        this.configured = data != null && data.enabled();\n'
        '        this.initialCustomZone = initial.customZone();\n'
        '        this.initialFloorNumber = initial.floorNumber();\n'
        '        this.zone = initial.zone();\n'
        '        this.floorType = initial.floorType();\n',
    )
    replace_once(
        path,
        '    private ElevatorArrivalDisplayData initialData() {\n'
        '        ElevatorArrivalDisplayData data = ElevatorArrivalDisplayData.EDITOR_DEFAULT;\n'
        '        if (zone == ElevatorArrivalDisplayData.Zone.CUSTOM) {\n'
        '            data = new ElevatorArrivalDisplayData(true, zone, "Custom Zone",\n'
        '                    floorType, 1);\n'
        '        } else {\n'
        '            data = new ElevatorArrivalDisplayData(true, zone, "",\n'
        '                    floorType, 1);\n'
        '        }\n'
        '        return data;\n'
        '    }\n',
        '    private ElevatorArrivalDisplayData initialData() {\n'
        '        return new ElevatorArrivalDisplayData(true, zone,\n'
        '                initialCustomZone, floorType, initialFloorNumber);\n'
        '    }\n',
    )


def patch_docs() -> None:
    replace_once(
        "LICENSE.md",
        '| Montserrat Regular 7.200 | SCP Inventory interface headings and labels | Copyright 2011 The Montserrat Project Authors; originally designed by Julieta Ulanovsky | [SIL Open Font License 1.1](https://github.com/JulietaUla/Montserrat/blob/master/OFL.txt) |\n',
        '| Montserrat Regular 7.200 | SCP Inventory interface headings and labels | Copyright 2011 The Montserrat Project Authors; originally designed by Julieta Ulanovsky | [SIL Open Font License 1.1](https://github.com/JulietaUla/Montserrat/blob/master/OFL.txt) |\n'
        '| Titillium Web Regular | Core Room elevator arrival display | Copyright 2009–2011 Accademia di Belle Arti di Urbino and the Titillium Web project authors | [SIL Open Font License 1.1](https://github.com/google/fonts/blob/main/ofl/titilliumweb/OFL.txt) |\n',
    )
    replace_once(
        "CHANGELOG.md",
        '- Added a modular, animated Core Room elevator based on SCP: Unity, with automatic floor discovery, a moving carriage, landing gates, procedural cables, and one-floor-at-a-time travel;\n',
        '- Added a modular, animated Core Room elevator based on SCP: Unity, with automatic floor discovery, a moving carriage, landing gates, procedural cables, and one-floor-at-a-time travel;\n'
        '- Added optional Screwdriver-configured arrival displays per elevator floor station, with animated sector and level announcements shown to passengers as the doors open;\n',
    )


def main() -> None:
    download_font()
    patch_fonts()
    patch_protocol_and_network()
    patch_station()
    patch_carriage()
    patch_editor_state()
    patch_docs()


if __name__ == "__main__":
    main()
