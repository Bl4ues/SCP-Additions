from pathlib import Path
import re


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def add_import(text: str, import_line: str, after: str) -> str:
    if import_line in text:
        return text
    return replace_once(text, after, after + import_line, f"add import {import_line.strip()}")


# ---------------------------------------------------------------------------
# Dedicated-server safe sound packet dispatch.
# ---------------------------------------------------------------------------
network_dir = Path("src/main/java/net/mcreator/scpadditions/network")
client_dir = Path("src/main/java/net/mcreator/scpadditions/client")

write(network_dir / "ClientPacketExecutor.java", '''package net.mcreator.scpadditions.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common-side bridge for packets whose effects are entirely client-side.
 *
 * The target class is intentionally named as a string. This keeps every
 * net.minecraft.client type out of packet classes that must also be loaded by
 * dedicated servers during channel registration.
 */
public final class ClientPacketExecutor {
    private static final String TARGET =
            "net.mcreator.scpadditions.client.ClientPacketActions";
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();

    private ClientPacketExecutor() {
    }

    public static void run(String action) {
        if (FMLEnvironment.dist != Dist.CLIENT || action == null || action.isBlank()) return;
        try {
            Method method = METHODS.computeIfAbsent(action, ClientPacketExecutor::resolve);
            if (method != null) method.invoke(null);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.error("Could not execute client packet action {}", action, exception);
        }
    }

    private static Method resolve(String action) {
        try {
            return Class.forName(TARGET).getMethod(action);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.error("Could not resolve client packet action {}", action, exception);
            return null;
        }
    }
}
''')

write(client_dir / "ClientPacketActions.java", '''package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Client-only endpoints invoked through the common packet bridge. */
public final class ClientPacketActions {
    private ClientPacketActions() {
    }

    public static void playScareSound() {
        BlinkClient.playScareSound();
    }

    public static void playEnterSound() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(ScpAdditionsModSounds.ENTER.get(), 1.0F, 1.0F));
    }

    public static void playScp1176Music() {
        Scp1176MusicClient.play();
    }
}
''')

packet_template = '''package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class {class_name} {{
    public static void encode({class_name} message, FriendlyByteBuf buffer) {{
    }}

    public static {class_name} decode(FriendlyByteBuf buffer) {{
        return new {class_name}();
    }}

    public static void handle({class_name} message,
                              Supplier<NetworkEvent.Context> supplier) {{
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPacketExecutor.run("{action}"));
        context.setPacketHandled(true);
    }}
}}
'''

for class_name, action in (
        ("ScareSoundPacket", "playScareSound"),
        ("EnterSoundPacket", "playEnterSound"),
        ("Scp1176MusicPacket", "playScp1176Music")):
    write(network_dir / f"{class_name}.java",
          packet_template.format(class_name=class_name, action=action))

# Guard against this classloading regression returning in another registered
# network class. The build step also scans the compiled source tree.


# ---------------------------------------------------------------------------
# Make inventory.enabled authoritative in every continuous routing path.
# ---------------------------------------------------------------------------
path = "src/main/java/com/bl4ues/scpinventory/events/VanillaMirrorSyncHandler.java"
text = read(path)
text = add_import(text,
                  "import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n",
                  "import net.minecraftforge.fml.common.Mod;\n")
needle = '''        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % SYNC_INTERVAL_TICKS != 0) {
'''
replacement = '''        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!ScpAdditionsModulesConfig.get().inventory.enabled) {
            return;
        }

        if (player.tickCount % SYNC_INTERVAL_TICKS != 0) {
'''
text = replace_once(text, needle, replacement, "VanillaMirrorSyncHandler module guard")
write(path, text)

path = "src/main/java/com/bl4ues/scpinventory/event/ScpInventoryMaintenanceEvents.java"
text = read(path)
text = add_import(text,
                  "import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n",
                  "import net.minecraftforge.fml.common.Mod;\n")
text = replace_once(text,
'''    public static boolean activateUsableSession(ServerPlayer player, IScpInventory inventory, int sourceSlot) {
        if (player == null || inventory == null || player.isCreative() || player.isSpectator() || !inventory.isValidMainSlot(sourceSlot)) {
''',
'''    public static boolean activateUsableSession(ServerPlayer player, IScpInventory inventory, int sourceSlot) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled
                || player == null || inventory == null || player.isCreative()
                || player.isSpectator() || !inventory.isValidMainSlot(sourceSlot)) {
''', "usable activation module guard")
text = replace_once(text,
'''    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) {
''',
'''    public static void onItemToss(ItemTossEvent event) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled
                || !(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) {
''', "item toss module guard")
text = replace_once(text,
'''        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isSpectator()) {
''',
'''        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !ScpAdditionsModulesConfig.get().inventory.enabled
                || !(event.player instanceof ServerPlayer player)
                || player.isSpectator()) {
''', "maintenance tick module guard")
write(path, text)

# Output guard events must also become inert when the inventory module is off.
path = "src/main/java/com/bl4ues/scpinventory/event/ScpInventoryUsableOutputGuardEvents.java"
if Path(path).exists():
    text = read(path)
    text = add_import(text,
                      "import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n",
                      "import net.minecraftforge.fml.common.Mod;\n")
    # Insert a guard after every SubscribeEvent method opening. These handlers
    # return void in the current implementation.
    text, count = re.subn(
        r'(public static void on[A-Za-z0-9_]+\([^)]*\) \{\n)',
        r'\1        if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;\n',
        text)
    if count == 0:
        raise RuntimeError("usable output guard: no event methods found")
    write(path, text)

# Reject stale/malicious inventory packets server-side when the module is off.
for packet_name in ("InventoryActionPacket.java", "PickupItemPacket.java", "MainUseActionPacket.java"):
    path = f"src/main/java/com/bl4ues/scpinventory/network/{packet_name}"
    if not Path(path).exists():
        continue
    text = read(path)
    if "ScpAdditionsModulesConfig" not in text:
        # Place the import immediately after the package/import block's first
        # known Minecraft/Forge import.
        marker = "import net.minecraft.server.level.ServerPlayer;\n"
        if marker in text:
            text = text.replace(marker,
                                marker + "import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n",
                                1)
        else:
            text = text.replace("package com.bl4ues.scpinventory.network;\n",
                                "package com.bl4ues.scpinventory.network;\n\nimport net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n",
                                1)
    # Guard all sender assignments without assuming the local Context variable.
    pattern = r'(ServerPlayer\s+\w+\s*=\s*\w+\.getSender\(\);\n)'
    text, count = re.subn(pattern,
                         r'\1            if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;\n',
                         text)
    if count == 0 and "getSender()" in text:
        raise RuntimeError(f"{packet_name}: sender guard could not be inserted")
    write(path, text)

# BlockPickupHandler already respects the module. Keep a static verification so
# this patch fails rather than silently regressing.
pickup = read("src/main/java/com/bl4ues/scpinventory/events/BlockPickupHandler.java")
if "!ScpAdditionsModulesConfig.get().inventory.enabled" not in pickup:
    raise RuntimeError("BlockPickupHandler no longer contains its module guard")


# ---------------------------------------------------------------------------
# Remove the cut 1.0 SCP-079 behavior that overrides terminal choices.
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/procedures/Scp079controloffUpdateTickProcedure.java"
text = read(path)
text, removed = re.subn(
    r'\s*world\.getLevelData\(\)\.getGameRules\(\)\.getRule\(ScpAdditionsModGameRules\.TESLAGATEON\)\.set\(true, world\.getServer\(\)\);',
    '', text)
if removed != 1:
    raise RuntimeError(f"SCP-079 legacy Tesla override: expected one line, removed {removed}")
# Remove now-unused gamerule import if it became unused.
if "ScpAdditionsModGameRules" not in text.replace(
        "import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;", ""):
    text = text.replace("import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;\n", "")
write(path, text)


# ---------------------------------------------------------------------------
# Decontamination: seal the entire checkpoint opening while closed.
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/block/DeconClosedBlock.java"
text = read(path)
# Replace only the collision method, preserving the visual/selection model.
collision_pattern = re.compile(
    r'(@Override\s+public VoxelShape getCollisionShape\(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context\) \{).*?(\n\s*\})',
    re.S)
match = collision_pattern.search(text)
if not match:
    raise RuntimeError("DeconClosedBlock collision method not found")
body = '''@Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
        // The checkpoint model spans three blocks. A narrow model-derived shape
        // left walkable seams beside the closed door, so use a continuous two-
        // block-high barrier across the complete opening while the gas cycle is
        // active.
        return facing.getAxis() == Direction.Axis.Z
                ? Block.box(-16.0D, 0.0D, 6.0D, 32.0D, 32.0D, 10.0D)
                : Block.box(6.0D, 0.0D, -16.0D, 10.0D, 32.0D, 32.0D);
    }'''
text = text[:match.start()] + body + text[match.end():]
# Ensure Direction is imported; FACING is already used by this generated block.
if "import net.minecraft.core.Direction;" not in text:
    text = text.replace("import net.minecraft.core.BlockPos;\n",
                        "import net.minecraft.core.BlockPos;\nimport net.minecraft.core.Direction;\n", 1)
write(path, text)


# ---------------------------------------------------------------------------
# Config validation: unknown module keys should not silently look successful.
# ---------------------------------------------------------------------------
path = "src/main/java/net/mcreator/scpadditions/config/ScpAdditionsReloadCommand.java"
text = read(path)
old = '''        requireObjectIfPresent(root, "inventory", path, errors);
        requireObjectIfPresent(root, "interactions", path, errors);
        requireObjectIfPresent(root, "hud", path, errors);
        requireObjectIfPresent(root, "vitals", path, errors);
        requireObjectIfPresent(root, "blink", path, errors);
        requireObjectIfPresent(root, "scp_173", path, errors);
'''
new = '''        requireObjectIfPresent(root, "inventory", path, errors);
        requireObjectIfPresent(root, "interactions", path, errors);
        requireObjectIfPresent(root, "hud", path, errors);
        requireObjectIfPresent(root, "vitals", path, errors);
        requireObjectIfPresent(root, "blink", path, errors);
        requireObjectIfPresent(root, "scp_173", path, errors);
        validateBooleanMember(root, "inventory", "enabled", path, errors);
        validateBooleanMember(root, "inventory", "remember_ui_state", path, errors);
        rejectUnknownMember(root, "inventory", "disabled", path, errors,
                "Use inventory.enabled: false instead of inventory.disabled.");
'''
text = replace_once(text, old, new, "module validation calls")
# Add small validators before validateInventory.
marker = "    private static void validateInventory(Path path, List<String> errors, List<String> warnings) {\n"
helpers = '''    private static void validateBooleanMember(JsonObject root, String objectKey,
                                              String memberKey, Path path,
                                              List<String> errors) {
        if (!root.has(objectKey) || !root.get(objectKey).isJsonObject()) return;
        JsonObject object = root.getAsJsonObject(objectKey);
        if (object.has(memberKey) && (!object.get(memberKey).isJsonPrimitive()
                || !object.get(memberKey).getAsJsonPrimitive().isBoolean())) {
            errors.add(relative(path) + ": " + objectKey + "." + memberKey
                    + " must be true or false");
        }
    }

    private static void rejectUnknownMember(JsonObject root, String objectKey,
                                            String memberKey, Path path,
                                            List<String> errors, String help) {
        if (!root.has(objectKey) || !root.get(objectKey).isJsonObject()) return;
        if (root.getAsJsonObject(objectKey).has(memberKey)) {
            errors.add(relative(path) + ": unknown field " + objectKey + "."
                    + memberKey + ". " + help);
        }
    }

'''
text = replace_once(text, marker, helpers + marker, "module validation helpers")
write(path, text)


# ---------------------------------------------------------------------------
# Changelog only; README/Wiki are intentionally not rewritten.
# ---------------------------------------------------------------------------
path = "CHANGELOG.md"
text = read(path)
anchor = "## Fixed\n"
if anchor not in text:
    anchor = "## Configuration center\n"
if anchor not in text:
    raise RuntimeError("CHANGELOG insertion point not found")
entries = (
    "- Fixed dedicated servers crashing during network registration because sound packets referenced client-only `SoundInstance` classes;\n"
    "- Fixed `inventory.enabled: false` still allowing background tick handlers and packets to move items into the SCP Inventory;\n"
    "- Removed a cut SCP-079 1.0 behavior that silently forced `teslaGateOn` back to `true`;\n"
    "- Expanded the closed Decontamination Checkpoint collision across the complete doorway to prevent escaping around its sides during a gas cycle;\n"
)
text = text.replace(anchor, anchor + entries, 1)
write(path, text)


# Static source audit: registered packet source must not directly import the
# exact client sound classes that caused the dedicated-server crash.
for path in network_dir.glob("*.java"):
    source = path.read_text(encoding="utf-8")
    if "net.minecraft.client.resources.sounds" in source:
        raise RuntimeError(f"client sound import remains in common network class: {path}")
