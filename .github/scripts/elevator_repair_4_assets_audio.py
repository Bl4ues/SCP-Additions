import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
java = root / "src/main/java"
res = root / "src/main/resources"


def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError(f"Could not locate {label}")
    return text.replace(old, new, 1)


# Cabin geometry faces the same frame as the station. Locators remain on the
# front/rear axis after removing the authored root quarter-turn.
carriage_path = res / "assets/scp_additions/geo/entity/core_room_elevator_carriage.geo.json"
carriage = json.loads(carriage_path.read_text(encoding="utf-8"))
cabin = carriage["minecraft:geometry"][0]["bones"][0]
if cabin.get("name") != "cabin":
    raise RuntimeError("Unexpected carriage root")
cabin["rotation"] = [0, 0, 0]
cabin["locators"]["cable_attachment_front"] = [0, 53, -7]
cabin["locators"]["cable_attachment_rear"] = [0, 53, 7]
carriage_path.write_text(json.dumps(carriage, separators=(",", ":")) + "\n",
                         encoding="utf-8")

# Shift exactly the four guide bars one Blockbench unit toward the rear.
pulley_path = res / "assets/scp_additions/geo/block/core_room_elevator_pulley.geo.json"
pulley = json.loads(pulley_path.read_text(encoding="utf-8"))
bone = pulley["minecraft:geometry"][0]["bones"][0]
origins = {(-16.5, 0, 13), (12.25, 0, 13),
           (12.25, 0, -14.5), (-16.5, 0, -16)}
shifted = 0
for cube in bone.get("cubes", []):
    if tuple(cube.get("origin", [])) in origins:
        cube["origin"][0] += 1
        shifted += 1
if shifted != 4:
    raise RuntimeError(f"Expected four pulley bars, found {shifted}")
pulley_path.write_text(json.dumps(pulley, separators=(",", ":")) + "\n",
                       encoding="utf-8")

# Preserve partial alpha on the station/cabin and the thin Core Room floor.
client_path = java / "net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorClient.java"
client = client_path.read_text(encoding="utf-8")
needle = "            return RenderType.entityCutoutNoCull(texture);"
positions = []
start = 0
while True:
    found = client.find(needle, start)
    if found < 0:
        break
    positions.append(found)
    start = found + len(needle)
if len(positions) != 3:
    raise RuntimeError(f"Expected three elevator render types, found {len(positions)}")
for index in (2, 0):
    found = positions[index]
    client = (client[:found]
              + "            return RenderType.entityTranslucent(texture);"
              + client[found + len(needle):])
client_path.write_text(client, encoding="utf-8")

screens_path = java / "net/mcreator/scpadditions/init/ScpAdditionsModScreens.java"
screens = screens_path.read_text(encoding="utf-8")
screens = replace_once(screens,
    "ItemBlockRenderTypes.setRenderLayer(CoreRoomElevatorModule.FLOOR.get(), RenderType.cutout());",
    "ItemBlockRenderTypes.setRenderLayer(CoreRoomElevatorModule.FLOOR.get(), RenderType.translucent());",
    "floor render layer")
screens_path.write_text(screens, encoding="utf-8")

state_path = res / "assets/scp_additions/blockstates/core_room_floor.json"
state_path.write_text(json.dumps({"variants": {
    "facing=north": {"model": "scp_additions:block/core_room_floor"},
    "facing=east": {"model": "scp_additions:block/core_room_floor", "y": 90},
    "facing=south": {"model": "scp_additions:block/core_room_floor", "y": 180},
    "facing=west": {"model": "scp_additions:block/core_room_floor", "y": 270}
}}, separators=(",", ":")) + "\n", encoding="utf-8")

# Poll the effective respawn state after vanilla/Forge applies it. This covers
# /spawnpoint, beds, anchors, checkpoint code, and other setRespawnPosition calls.
save_path = java / "net/mcreator/scpadditions/event/SaveGameSoundEvents.java"
save_path.write_text('''package net.mcreator.scpadditions.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.sound.GameplaySounds;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Plays save_game.ogg after a player's effective respawn point changes. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaveGameSoundEvents {
    private static final Map<UUID, SpawnSnapshot> LAST_SPAWNS = new HashMap<>();

    private SaveGameSoundEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_SPAWNS.put(player.getUUID(), snapshot(player));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SPAWNS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)) return;
        SpawnSnapshot current = snapshot(player);
        SpawnSnapshot previous = LAST_SPAWNS.put(player.getUUID(), current);
        if (previous == null || previous.equals(current)
                || current.position() == null
                || !ScpAdditionsModulesConfig.get().audio
                .saveGameSoundEnabled) return;
        player.playNotifySound(GameplaySounds.SAVE_GAME.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static SpawnSnapshot snapshot(ServerPlayer player) {
        BlockPos position = player.getRespawnPosition();
        return new SpawnSnapshot(player.getRespawnDimension(),
                position == null ? null : position.immutable(),
                player.isRespawnForced());
    }

    private record SpawnSnapshot(ResourceKey<Level> dimension,
            @Nullable BlockPos position, boolean forced) {}
}
''', encoding="utf-8")

changelog_path = root / "CHANGELOG.md"
changelog = changelog_path.read_text(encoding="utf-8")
anchor = "- Corrected station, carriage, Pulley, cable, prompt, transparency, z-fighting, and moving-collision alignment."
changelog = replace_once(changelog, anchor, anchor + "\n- Rebuilt thin-floor culling, directional placement, station selection/collision, cabin wall tunneling, SL1-style cabin footsteps, pulley guide alignment, station snapping, translucent windows, and respawn-save audio after in-game validation.", "changelog")
changelog_path.write_text(changelog, encoding="utf-8")
