from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java"
RES = ROOT / "src/main/resources"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise RuntimeError(f"Unable to locate {label}")
    return text.replace(old, new, 1)


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, data: dict, *, compact: bool = False) -> None:
    if compact:
        content = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    else:
        content = json.dumps(data, ensure_ascii=False, indent=2)
    write(path, content + "\n")


# ---------------------------------------------------------------------------
# SCP-173 sound registry and natural-spawn cleanup
# ---------------------------------------------------------------------------

sounds_path = JAVA / "net/mcreator/scpadditions/entity/Scp173Sounds.java"
sounds = sounds_path.read_text(encoding="utf-8")
sounds = replace_once(
    sounds,
    '    public static final RegistryObject<SoundEvent> STONE_SCRAP = register("stone_scrap");\n',
    '    public static final RegistryObject<SoundEvent> STONE_SCRAP_LOOP = register("stone_scrap_loop");\n'
    '    public static final RegistryObject<SoundEvent> STONE_SCRAP = register("stone_scrap");\n',
    "SCP-173 scrape sound registrations",
)
sounds = sounds.replace(
    '    public static final RegistryObject<SoundEvent> RATTLE = register("rattle");\n',
    "",
)
write(sounds_path, sounds)

spawn_path = JAVA / "net/mcreator/scpadditions/event/Scp173SpawnEvents.java"
spawn = spawn_path.read_text(encoding="utf-8")
spawn = spawn.replace("import net.minecraft.sounds.SoundSource;\n", "")
spawn = spawn.replace("import net.mcreator.scpadditions.entity.Scp173Sounds;\n", "")
spawn = replace_once(
    spawn,
    "                Scp173Entity spawned = spawn173(level, pos, player, random);\n",
    "                Scp173Entity spawned = spawn173(level, pos, player);\n",
    "natural SCP-173 spawn call",
)
spawn = replace_once(
    spawn,
    "    private static Scp173Entity spawn173(ServerLevel level, BlockPos pos,\n"
    "            ServerPlayer player, RandomSource random) {\n",
    "    private static Scp173Entity spawn173(ServerLevel level, BlockPos pos,\n"
    "            ServerPlayer player) {\n",
    "natural SCP-173 spawn method signature",
)
spawn = spawn.replace(
    "        level.playSound(null, x, y + 0.6D, z,\n"
    "                Scp173Sounds.RATTLE.get(), SoundSource.HOSTILE,\n"
    "                0.72F, 0.96F + random.nextFloat() * 0.08F);\n",
    "",
)
write(spawn_path, spawn)


# ---------------------------------------------------------------------------
# SCP-173 movement loop and turn/stop accents
# ---------------------------------------------------------------------------

movement_event = '''package net.mcreator.scpadditions.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp173Entity;
import net.mcreator.scpadditions.entity.Scp173Sounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Plays the short scrape variants only when SCP-173 turns or stops. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp173MovementScrapeEvents {
    private static final double TRACK_RANGE = 64.0D;
    private static final double TRACK_RANGE_SQR = TRACK_RANGE * TRACK_RANGE;
    private static final double MIN_MOVEMENT_SQR = 0.003D * 0.003D;
    private static final double TURN_DOT_THRESHOLD = 0.9238795325112867D;
    private static final int TURN_SOUND_COOLDOWN_TICKS = 5;
    private static final int STOP_CONFIRM_TICKS = 2;
    private static final long STATE_EXPIRY_TICKS = 200L;

    private static final Map<UUID, MovementState> STATES = new HashMap<>();
    private static final Map<UUID, Integer> LAST_PROCESSED_TICK = new HashMap<>();

    private Scp173MovementScrapeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(TRACK_RANGE);
        for (Scp173Entity scp173 : player.serverLevel().getEntitiesOfClass(
                Scp173Entity.class, area,
                entity -> entity.isAlive()
                        && entity.distanceToSqr(player) <= TRACK_RANGE_SQR)) {
            updateMovementAccent(scp173);
        }

        long gameTime = player.serverLevel().getGameTime();
        if (gameTime % STATE_EXPIRY_TICKS == 0L) {
            STATES.entrySet().removeIf(entry ->
                    gameTime - entry.getValue().lastSeenGameTime
                            > STATE_EXPIRY_TICKS);
            LAST_PROCESSED_TICK.keySet().removeIf(id -> !STATES.containsKey(id));
        }
    }

    private static void updateMovementAccent(Scp173Entity scp173) {
        UUID id = scp173.getUUID();
        if (LAST_PROCESSED_TICK.getOrDefault(id, -1) == scp173.tickCount) {
            return;
        }
        LAST_PROCESSED_TICK.put(id, scp173.tickCount);

        Vec3 current = scp173.position();
        MovementState state = STATES.computeIfAbsent(id,
                ignored -> new MovementState(current));
        state.lastSeenGameTime = scp173.level().getGameTime();

        Vec3 displacement = current.subtract(state.lastPosition);
        state.lastPosition = current;
        Vec3 horizontal = new Vec3(displacement.x, 0.0D, displacement.z);
        boolean moved = scp173.isActivated() && scp173.isScraping()
                && horizontal.lengthSqr() > MIN_MOVEMENT_SQR;

        if (moved) {
            Vec3 direction = horizontal.normalize();
            if (state.moving && state.lastDirection != null
                    && state.lastDirection.dot(direction) < TURN_DOT_THRESHOLD
                    && scp173.tickCount >= state.nextTurnSoundTick) {
                playAccent(scp173);
                state.nextTurnSoundTick = scp173.tickCount
                        + TURN_SOUND_COOLDOWN_TICKS;
            }
            state.moving = true;
            state.stillTicks = 0;
            state.lastDirection = direction;
            return;
        }

        if (!state.moving) return;
        state.stillTicks++;
        boolean confirmedStop = !scp173.isActivated() || !scp173.isScraping()
                || state.stillTicks >= STOP_CONFIRM_TICKS;
        if (!confirmedStop) return;

        state.moving = false;
        state.stillTicks = 0;
        state.lastDirection = null;
        playAccent(scp173);
    }

    private static void playAccent(Scp173Entity scp173) {
        scp173.level().playSound(null, scp173.getX(), scp173.getY() + 0.35D,
                scp173.getZ(), Scp173Sounds.STONE_SCRAP.get(),
                SoundSource.HOSTILE, 0.72F,
                0.94F + scp173.getRandom().nextFloat() * 0.12F);
    }

    private static final class MovementState {
        private Vec3 lastPosition;
        private Vec3 lastDirection;
        private boolean moving;
        private int stillTicks;
        private int nextTurnSoundTick;
        private long lastSeenGameTime;

        private MovementState(Vec3 position) {
            this.lastPosition = position;
        }
    }
}
'''
write(JAVA / "net/mcreator/scpadditions/event/Scp173MovementScrapeEvents.java",
      movement_event)

movement_loop = '''package net.mcreator.scpadditions.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.mcreator.scpadditions.entity.Scp173Entity;
import net.mcreator.scpadditions.entity.Scp173Sounds;

/** Continuous stone scrape attached to a moving SCP-173 instance. */
public final class Scp173MovementLoopSound
        extends AbstractTickableSoundInstance {
    private final Scp173Entity entity;

    public Scp173MovementLoopSound(Scp173Entity entity) {
        super(Scp173Sounds.STONE_SCRAP_LOOP.get(), SoundSource.HOSTILE,
                RandomSource.create());
        this.entity = entity;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.68F;
        this.pitch = 1.0F;
        tick();
    }

    @Override
    public void tick() {
        if (entity == null || entity.isRemoved() || !entity.isAlive()
                || !entity.isActivated() || !entity.isScraping()) {
            stop();
            return;
        }
        this.x = entity.getX();
        this.y = entity.getY() + 0.35D;
        this.z = entity.getZ();
    }

    public void finish() {
        stop();
    }
}
'''
write(JAVA / "net/mcreator/scpadditions/client/Scp173MovementLoopSound.java",
      movement_loop)

movement_loop_events = '''package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp173Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Starts stone_scrap.ogg on movement and stops it when movement ends. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp173MovementLoopSoundEvents {
    private static final double RANGE = 64.0D;
    private static final double MOVEMENT_EPSILON_SQR = 0.003D * 0.003D;
    private static final int STOP_CONFIRM_TICKS = 2;

    private static final Map<Integer, Scp173MovementLoopSound> LOOPS =
            new HashMap<>();
    private static final Map<Integer, MotionState> STATES = new HashMap<>();

    private Scp173MovementLoopSoundEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopEverything();
            return;
        }

        Set<Integer> seen = new HashSet<>();
        AABB area = minecraft.player.getBoundingBox().inflate(RANGE);
        for (Scp173Entity scp173 : minecraft.level.getEntitiesOfClass(
                Scp173Entity.class, area,
                entity -> entity.isAlive() && !entity.isRemoved())) {
            int id = scp173.getId();
            seen.add(id);
            MotionState state = STATES.computeIfAbsent(id,
                    ignored -> new MotionState(scp173.position()));

            Vec3 current = scp173.position();
            Vec3 displacement = current.subtract(state.lastPosition);
            state.lastPosition = current;
            boolean moved = scp173.isActivated() && scp173.isScraping()
                    && new Vec3(displacement.x, 0.0D, displacement.z)
                    .lengthSqr() > MOVEMENT_EPSILON_SQR;

            if (moved) {
                state.stillTicks = 0;
                startLoop(minecraft, scp173);
            } else if (LOOPS.containsKey(id)) {
                state.stillTicks++;
                if (!scp173.isActivated() || !scp173.isScraping()
                        || state.stillTicks >= STOP_CONFIRM_TICKS) {
                    stopOne(LOOPS.remove(id));
                    state.stillTicks = 0;
                }
            }
        }

        prune(seen);
    }

    private static void startLoop(Minecraft minecraft, Scp173Entity entity) {
        Scp173MovementLoopSound existing = LOOPS.get(entity.getId());
        if (existing != null && !existing.isStopped()) return;

        Scp173MovementLoopSound created =
                new Scp173MovementLoopSound(entity);
        LOOPS.put(entity.getId(), created);
        minecraft.getSoundManager().play(created);
    }

    private static void prune(Set<Integer> seen) {
        for (Integer id : new ArrayList<>(LOOPS.keySet())) {
            Scp173MovementLoopSound sound = LOOPS.get(id);
            if (!seen.contains(id) || sound == null || sound.isStopped()) {
                stopOne(sound);
                LOOPS.remove(id);
            }
        }
        STATES.keySet().removeIf(id -> !seen.contains(id));
    }

    private static void stopOne(Scp173MovementLoopSound sound) {
        if (sound != null && !sound.isStopped()) sound.finish();
    }

    private static void stopEverything() {
        LOOPS.values().forEach(Scp173MovementLoopSoundEvents::stopOne);
        LOOPS.clear();
        STATES.clear();
    }

    private static final class MotionState {
        private Vec3 lastPosition;
        private int stillTicks;

        private MotionState(Vec3 lastPosition) {
            this.lastPosition = lastPosition;
        }
    }
}
'''
write(JAVA / "net/mcreator/scpadditions/client/Scp173MovementLoopSoundEvents.java",
      movement_loop_events)


# ---------------------------------------------------------------------------
# Save-game sound module
# ---------------------------------------------------------------------------

gameplay_sounds = '''package net.mcreator.scpadditions.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Sound events for general gameplay feedback. */
public final class GameplaySounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpAdditionsMod.MODID);

    public static final RegistryObject<SoundEvent> SAVE_GAME =
            register("save_game");

    private GameplaySounds() {
    }

    private static RegistryObject<SoundEvent> register(String path) {
        ResourceLocation id = new ResourceLocation(ScpAdditionsMod.MODID, path);
        return REGISTRY.register(path,
                () -> SoundEvent.createVariableRangeEvent(id));
    }
}
'''
write(JAVA / "net/mcreator/scpadditions/sound/GameplaySounds.java",
      gameplay_sounds)

save_events = '''package net.mcreator.scpadditions.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.sound.GameplaySounds;

/** Plays save_game.ogg whenever a player's respawn point is successfully set. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaveGameSoundEvents {
    private SaveGameSoundEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (event.isCanceled() || event.getNewSpawn() == null
                || !(event.getEntity() instanceof ServerPlayer player)
                || !ScpAdditionsModulesConfig.get().audio
                .saveGameSoundEnabled) {
            return;
        }

        player.playNotifySound(GameplaySounds.SAVE_GAME.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
'''
write(JAVA / "net/mcreator/scpadditions/event/SaveGameSoundEvents.java",
      save_events)

mod_path = JAVA / "net/mcreator/scpadditions/ScpAdditionsMod.java"
mod = mod_path.read_text(encoding="utf-8")
mod = replace_once(
    mod,
    "import net.mcreator.scpadditions.scp012.Scp012Module;\n",
    "import net.mcreator.scpadditions.scp012.Scp012Module;\n"
    "import net.mcreator.scpadditions.sound.GameplaySounds;\n",
    "GameplaySounds import",
)
mod = replace_once(
    mod,
    "        Scp173Sounds.REGISTRY.register(bus);\n",
    "        Scp173Sounds.REGISTRY.register(bus);\n"
    "        GameplaySounds.REGISTRY.register(bus);\n",
    "GameplaySounds registry registration",
)
write(mod_path, mod)

config_path = JAVA / "net/mcreator/scpadditions/config/ScpAdditionsModulesConfig.java"
config = config_path.read_text(encoding="utf-8")
config = replace_once(
    config,
    '        @SerializedName("enter_sound_enabled")\n'
    '        public boolean enterSoundEnabled = true;\n',
    '        @SerializedName("enter_sound_enabled")\n'
    '        public boolean enterSoundEnabled = true;\n\n'
    '        @SerializedName("save_game_sound_enabled")\n'
    '        public boolean saveGameSoundEnabled = true;\n',
    "save-game audio module field",
)
write(config_path, config)

bundled_modules_path = ROOT / "config/scpadditions/modules.json"
bundled_modules = load_json(bundled_modules_path)
bundled_modules.setdefault("audio", {})["save_game_sound_enabled"] = True
write_json(bundled_modules_path, bundled_modules)

service_path = JAVA / "net/mcreator/scpadditions/config/ui/ConfigCenterService.java"
service = service_path.read_text(encoding="utf-8")
service = replace_once(
    service,
    '        checkBoolean(root, "audio", "enter_sound_enabled", errors);\n',
    '        checkBoolean(root, "audio", "enter_sound_enabled", errors);\n'
    '        checkBoolean(root, "audio", "save_game_sound_enabled", errors);\n',
    "save-game module validation",
)
service = service.replace(
    '\"audio\":{\"enter_sound_enabled\":true,\"replace_player_hurt_sounds\":true',
    '\"audio\":{\"enter_sound_enabled\":true,\"save_game_sound_enabled\":true,\"replace_player_hurt_sounds\":true',
)
if '\"save_game_sound_enabled\":true' not in service:
    raise RuntimeError("Unable to update ConfigCenterService default modules")
write(service_path, service)

screen_path = JAVA / "net/mcreator/scpadditions/config/ui/Scp079ModulesScreenExtension.java"
screen = screen_path.read_text(encoding="utf-8")
screen = replace_once(
    screen,
    '            new Row("audio", "enter_sound_enabled", "World Entry Sound",\n'
    '                    "Plays enter.ogg after joining or opening a world.", true),\n',
    '            new Row("audio", "enter_sound_enabled", "World Entry Sound",\n'
    '                    "Plays enter.ogg after joining or opening a world.", true),\n'
    '            new Row("audio", "save_game_sound_enabled",\n'
    '                    "Save Game Sound",\n'
    '                    "Plays save_game.ogg when commands, beds, respawn anchors, or facility systems set your respawn point.", true),\n',
    "save-game module UI row",
)
write(screen_path, screen)


# ---------------------------------------------------------------------------
# Resource definitions, subtitles, obsolete rattle assets
# ---------------------------------------------------------------------------

scp_sounds_path = RES / "assets/scp_additions/sounds.json"
scp_sounds = load_json(scp_sounds_path)
scp_sounds.pop("rattle", None)
scp_sounds["stone_scrap_loop"] = {
    "sounds": [
        {
            "name": "scpinventory:stone_scrap",
            "stream": False,
            "volume": 0.75,
        }
    ]
}
scp_sounds["save_game"] = {
    "subtitle": "subtitles.scp_additions.save_game",
    "sounds": [
        {
            "name": "scp_additions:save_game",
            "stream": False,
            "volume": 1.0,
        }
    ],
}
write_json(scp_sounds_path, scp_sounds, compact=True)

inventory_sounds_path = RES / "assets/scpinventory/sounds.json"
inventory_sounds = load_json(inventory_sounds_path)
inventory_sounds.pop("rattle", None)
write_json(inventory_sounds_path, inventory_sounds)

scp_lang_path = RES / "assets/scp_additions/lang/en_us.json"
scp_lang = load_json(scp_lang_path)
scp_lang.pop("subtitles.scp_additions.rattle", None)
scp_lang["subtitles.scp_additions.save_game"] = "Game saved"
write_json(scp_lang_path, scp_lang)

inventory_lang_path = RES / "assets/scpinventory/lang/en_us.json"
inventory_lang = load_json(inventory_lang_path)
inventory_lang.pop("subtitles.scpinventory.rattle", None)
write_json(inventory_lang_path, inventory_lang)

for name in ("rattle1.ogg", "rattle2.ogg", "rattle3.ogg"):
    path = RES / "assets/scpinventory/sounds" / name
    if path.exists():
        path.unlink()


# ---------------------------------------------------------------------------
# Documentation
# ---------------------------------------------------------------------------

changelog_path = ROOT / "CHANGELOG.md"
changelog = changelog_path.read_text(encoding="utf-8")
changelog = replace_once(
    changelog,
    "- Updated SCP-173's sound effects, removed its spawn sound, and changed how its moving sounds trigger;\n",
    "- Reworked SCP-173's movement audio: `stone_scrap.ogg` now loops only while the statue moves, while one of `stone_scrap_1.ogg` through `stone_scrap_5.ogg` plays when it turns or stops; removed the natural-spawn rattle and its unused assets;\n",
    "SCP-173 changelog audio entry",
)
changelog = replace_once(
    changelog,
    "- Reintroduced the world-entry sound and added a General & Modules option to disable it;\n",
    "- Reintroduced the world-entry sound and added a General & Modules option to disable it;\n"
    "- Added a default-enabled **Save Game Sound** module that plays `save_game.ogg` whenever commands, beds, respawn anchors, or facility systems set a player's respawn point;\n",
    "save-game changelog entry",
)
write(changelog_path, changelog)

test_path = ROOT / "docs/migration/TEST_MATRIX.md"
test_matrix = test_path.read_text(encoding="utf-8")
test_matrix = test_matrix.replace(
    "- [ ] scare, horror, rattle, scrape, death and neck-snap sounds load\n",
    "- [ ] scare, horror, continuous scrape, turn/stop scrape variants, death and neck-snap sounds load\n",
)
test_matrix = test_matrix.replace(
    "- [ ] spawn rattle and movement scrape pulses play at the intended cadence\n",
    "- [ ] `stone_scrap.ogg` loops only while SCP-173 moves, and variants 1-5 play only on turns or confirmed stops\n",
)
write(test_path, test_matrix)

# Remove the accidental validation marker from master when this branch is
# fast-forwarded after a successful build.
(ROOT / ".github/scp173-save-sound-validation.txt").unlink(missing_ok=True)

print("Applied SCP-173 movement audio and save-game sound integration.")
