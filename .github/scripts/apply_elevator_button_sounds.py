from __future__ import annotations

import json
from pathlib import Path


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(path: str, old: str, new: str, label: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one occurrence, found {count}")
    write(path, text.replace(old, new, 1))


# Confirm the user-provided assets are present under their exact filenames.
for sound_file in (
        "src/main/resources/assets/scp_additions/sounds/elebuttonpress.ogg",
        "src/main/resources/assets/scp_additions/sounds/elbuttonaccept.ogg"):
    if not Path(sound_file).is_file():
        raise SystemExit(f"Missing elevator sound asset: {sound_file}")


# Register both variable-range sound events and play them from the exact
# station button anchor. Press always plays for a valid button attempt;
# accept plays only when the request is accepted by the elevator manager.
module_path = "src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorModule.java"
replace_once(
    module_path,
    '''    public static final RegistryObject<SoundEvent> ELEVATOR_CABIN_LOOP =
            sound("elevator_cabin_loop");''',
    '''    public static final RegistryObject<SoundEvent> ELEVATOR_CABIN_LOOP =
            sound("elevator_cabin_loop");
    public static final RegistryObject<SoundEvent> ELEVATOR_BUTTON_PRESS =
            sound("elevator_button_press");
    public static final RegistryObject<SoundEvent> ELEVATOR_BUTTON_ACCEPT =
            sound("elevator_button_accept");''',
    "elevator sound registrations")

replace_once(
    module_path,
    '''        public InteractionResult handleContextInteraction(ServerLevel level,
                BlockPos pos, ServerPlayer player, String actionKey) {
            ElevatorFoundation.TravelDirection direction = actionKey != null
                    && actionKey.endsWith("up")
                    ? ElevatorFoundation.TravelDirection.UP
                    : ElevatorFoundation.TravelDirection.DOWN;
            return CoreRoomElevatorManager.requestFromStation(level, pos,
                    direction, player)
                    ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }''',
    '''        public InteractionResult handleContextInteraction(ServerLevel level,
                BlockPos pos, ServerPlayer player, String actionKey) {
            ElevatorFoundation.TravelDirection direction = actionKey != null
                    && actionKey.endsWith("up")
                    ? ElevatorFoundation.TravelDirection.UP
                    : ElevatorFoundation.TravelDirection.DOWN;
            BlockState state = level.getBlockState(pos);
            Vec3 button = CoreRoomElevatorGeometry.stationButtonWorld(pos,
                    state.getValue(FACING),
                    direction == ElevatorFoundation.TravelDirection.UP);
            level.playSound(null, button.x, button.y, button.z,
                    ELEVATOR_BUTTON_PRESS.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0F, 1.0F);
            boolean accepted = CoreRoomElevatorManager.requestFromStation(
                    level, pos, direction, player);
            if (accepted) {
                level.playSound(null, button.x, button.y, button.z,
                        ELEVATOR_BUTTON_ACCEPT.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        1.0F, 1.0F);
            }
            return accepted ? InteractionResult.CONSUME
                    : InteractionResult.FAIL;
        }''',
    "station button sounds")


# Use the same semantics inside the carriage, but emit from the actual selected
# cabin button rather than the entity center.
carriage_path = "src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorCarriageEntity.java"
replace_once(
    carriage_path,
    '''    public boolean handleContextInteraction(ServerPlayer player,
            String actionKey) {
        if (phase() != ElevatorFoundation.Phase.IDLE_OPEN) return false;
        ElevatorFoundation.TravelDirection direction = actionKey != null
                && actionKey.endsWith("up")
                ? ElevatorFoundation.TravelDirection.UP
                : ElevatorFoundation.TravelDirection.DOWN;
        int current = nearestFloorIndex(getY());
        int destination = current + direction.step();
        if (destination < 0 || destination >= floorHeights.length) {
            player.sendSystemMessage(net.minecraft.network.chat.Component
                    .translatable(direction == ElevatorFoundation.TravelDirection.UP
                            ? "message.scp_additions.elevator_no_floor_above"
                            : "message.scp_additions.elevator_no_floor_below"));
            return false;
        }
        queueDestination(destination);
        return true;
    }''',
    '''    public boolean handleContextInteraction(ServerPlayer player,
            String actionKey) {
        ElevatorFoundation.TravelDirection direction = actionKey != null
                && actionKey.endsWith("up")
                ? ElevatorFoundation.TravelDirection.UP
                : ElevatorFoundation.TravelDirection.DOWN;
        Vec3 button = contextAnchor(
                direction == ElevatorFoundation.TravelDirection.UP);
        playElevatorSoundAt(CoreRoomElevatorModule.ELEVATOR_BUTTON_PRESS.get(),
                button, 1.0F);
        if (phase() != ElevatorFoundation.Phase.IDLE_OPEN) return false;
        int current = nearestFloorIndex(getY());
        int destination = current + direction.step();
        if (destination < 0 || destination >= floorHeights.length) {
            player.sendSystemMessage(net.minecraft.network.chat.Component
                    .translatable(direction == ElevatorFoundation.TravelDirection.UP
                            ? "message.scp_additions.elevator_no_floor_above"
                            : "message.scp_additions.elevator_no_floor_below"));
            return false;
        }
        queueDestination(destination);
        playElevatorSoundAt(CoreRoomElevatorModule.ELEVATOR_BUTTON_ACCEPT.get(),
                button, 1.0F);
        return true;
    }''',
    "carriage button sounds")

replace_once(
    carriage_path,
    '''    private void playElevatorSound(net.minecraft.sounds.SoundEvent sound,
            float volume) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.playSound(null, getX(), getY() + 1.0D, getZ(), sound,
                net.minecraft.sounds.SoundSource.BLOCKS, volume, 1.0F);
    }''',
    '''    private void playElevatorSound(net.minecraft.sounds.SoundEvent sound,
            float volume) {
        playElevatorSoundAt(sound,
                new Vec3(getX(), getY() + 1.0D, getZ()), volume);
    }

    private void playElevatorSoundAt(net.minecraft.sounds.SoundEvent sound,
            Vec3 position, float volume) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.playSound(null, position.x, position.y, position.z, sound,
                net.minecraft.sounds.SoundSource.BLOCKS, volume, 1.0F);
    }''',
    "positioned elevator sound helper")


# Define the event IDs separately from the uploaded filenames. This keeps the
# Java API readable while preserving the exact asset names supplied by the user.
sounds_path = Path("src/main/resources/assets/scp_additions/sounds.json")
sounds = json.loads(sounds_path.read_text(encoding="utf-8"))
sounds["elevator_button_press"] = {
    "subtitle": "subtitles.scp_additions.elevator_button_press",
    "sounds": [{
        "name": "scp_additions:elebuttonpress",
        "stream": False,
        "volume": 1.0,
    }],
}
sounds["elevator_button_accept"] = {
    "subtitle": "subtitles.scp_additions.elevator_button_accept",
    "sounds": [{
        "name": "scp_additions:elbuttonaccept",
        "stream": False,
        "volume": 1.0,
    }],
}
sounds_path.write_text(
        json.dumps(sounds, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8")


# Keep subtitle text in the 3.1.0 language patch merged by processResources.
lang_path = Path(
        "src/main/resources/assets/scp_additions/lang/en_us_3_0.json")
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang["subtitles.scp_additions.elevator_button_press"] = \
        "Elevator button clicks"
lang["subtitles.scp_additions.elevator_button_accept"] = \
        "Elevator accepts request"
lang_path.write_text(
        json.dumps(lang, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8")

print("Elevator button sounds applied successfully.")
