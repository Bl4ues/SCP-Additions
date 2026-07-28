from pathlib import Path


def replace(path: str, old: str, new: str) -> None:
    target = Path(path)
    text = target.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}: {old[:120]!r}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


# Module config and bundled defaults.
replace(
    "src/main/java/net/mcreator/scpadditions/config/ScpAdditionsModulesConfig.java",
    '''\t\t@SerializedName("mute_non_player_hit_sounds")\n\t\tpublic boolean muteNonPlayerHitSounds = false;\n''',
    '''\t\t@SerializedName("mute_non_player_hit_sounds")\n\t\tpublic boolean muteNonPlayerHitSounds = false;\n\n\t\t@SerializedName("disable_vanilla_music")\n\t\tpublic boolean disableVanillaMusic = false;\n''')

replace(
    "config/scpadditions/modules.json",
    '''    "replace_player_hurt_sounds": true,\n    "mute_non_player_hit_sounds": false\n''',
    '''    "replace_player_hurt_sounds": true,\n    "mute_non_player_hit_sounds": false,\n    "disable_vanilla_music": false\n''')

replace(
    "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterService.java",
    '''        checkBoolean(root, "audio", "mute_non_player_hit_sounds", errors);\n''',
    '''        checkBoolean(root, "audio", "mute_non_player_hit_sounds", errors);\n        checkBoolean(root, "audio", "disable_vanilla_music", errors);\n''')
replace(
    "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterService.java",
    '''\"audio\":{\"enter_sound_enabled\":true,\"replace_player_hurt_sounds\":true,\"mute_non_player_hit_sounds\":false}''',
    '''\"audio\":{\"enter_sound_enabled\":true,\"replace_player_hurt_sounds\":true,\"mute_non_player_hit_sounds\":false,\"disable_vanilla_music\":false}''')

# Synchronize the client-only music setting from the server.
replace(
    "src/main/java/com/bl4ues/scpinventory/network/ModNetwork.java",
    '''    private static final String PROTOCOL_VERSION = "9";\n''',
    '''    private static final String PROTOCOL_VERSION = "10";\n''')
replace(
    "src/main/java/com/bl4ues/scpinventory/network/ModNetwork.java",
    '''                        ScpAdditionsModulesConfig.get().audio\n                                .muteNonPlayerHitSounds));\n''',
    '''                        ScpAdditionsModulesConfig.get().audio\n                                .muteNonPlayerHitSounds,\n                        ScpAdditionsModulesConfig.get().audio\n                                .disableVanillaMusic));\n''')

replace(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryModuleStatePacket.java",
    '''        boolean replacePlayerHurtSounds, boolean muteNonPlayerHitSounds) {\n''',
    '''        boolean replacePlayerHurtSounds, boolean muteNonPlayerHitSounds,\n        boolean disableVanillaMusic) {\n''')
replace(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryModuleStatePacket.java",
    '''        buffer.writeBoolean(message.muteNonPlayerHitSounds);\n''',
    '''        buffer.writeBoolean(message.muteNonPlayerHitSounds);\n        buffer.writeBoolean(message.disableVanillaMusic);\n''')
replace(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryModuleStatePacket.java",
    '''                buffer.readBoolean(), buffer.readBoolean());\n''',
    '''                buffer.readBoolean(), buffer.readBoolean(),\n                buffer.readBoolean());\n''')
replace(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryModuleStatePacket.java",
    '''                message.hungerDisabled, message.replacePlayerHurtSounds,\n                message.muteNonPlayerHitSounds));\n''',
    '''                message.hungerDisabled, message.replacePlayerHurtSounds,\n                message.muteNonPlayerHitSounds,\n                message.disableVanillaMusic));\n''')

replace(
    "src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java",
    '''    private static volatile Boolean serverMuteNonPlayerHitSounds;\n''',
    '''    private static volatile Boolean serverMuteNonPlayerHitSounds;\n    private static volatile Boolean serverDisableVanillaMusic;\n''')
replace(
    "src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java",
    '''    public static void updateFromServer(boolean enabled,\n''',
    '''    public static boolean disableVanillaMusicForClient() {\n        Boolean synced = serverDisableVanillaMusic;\n        return synced != null ? synced\n                : ScpAdditionsModulesConfig.get().audio.disableVanillaMusic;\n    }\n\n    public static void updateFromServer(boolean enabled,\n''')
replace(
    "src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java",
    '''            boolean replacePlayerHurtSounds,\n            boolean muteNonPlayerHitSounds) {\n''',
    '''            boolean replacePlayerHurtSounds,\n            boolean muteNonPlayerHitSounds,\n            boolean disableVanillaMusic) {\n''')
replace(
    "src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java",
    '''        serverMuteNonPlayerHitSounds = muteNonPlayerHitSounds;\n''',
    '''        serverMuteNonPlayerHitSounds = muteNonPlayerHitSounds;\n        serverDisableVanillaMusic = disableVanillaMusic;\n''')
replace(
    "src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java",
    '''        serverMuteNonPlayerHitSounds = null;\n''',
    '''        serverMuteNonPlayerHitSounds = null;\n        serverDisableVanillaMusic = null;\n''')

# Suppress only Minecraft's MusicManager. Mod soundtracks use their own sound instances.
replace(
    "src/main/java/net/mcreator/scpadditions/client/ModMusicExclusivityClient.java",
    '''import net.minecraft.client.Minecraft;\n''',
    '''import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;\nimport net.minecraft.client.Minecraft;\n''')
replace(
    "src/main/java/net/mcreator/scpadditions/client/ModMusicExclusivityClient.java",
    '''        if (event.phase != TickEvent.Phase.END\n                || !hasActiveModMusic()) {\n            return;\n        }\n        stopVanillaMusicNow();\n''',
    '''        if (event.phase != TickEvent.Phase.END) {\n            return;\n        }\n        if (InventoryModuleRuntimeState.disableVanillaMusicForClient()\n                || hasActiveModMusic()) {\n            stopVanillaMusicNow();\n        }\n''')

# Add the module to the Unity-styled Configuration Center and color the state suffix.
replace(
    "src/main/java/net/mcreator/scpadditions/config/ui/Scp079ModulesScreenExtension.java",
    '''            new Row("audio", "enter_sound_enabled", "World Entry Sound",\n                    "Plays enter.ogg after joining or opening a world.", true),\n''',
    '''            new Row("audio", "enter_sound_enabled", "World Entry Sound",\n                    "Plays enter.ogg after joining or opening a world.", true),\n            new Row("audio", "disable_vanilla_music", "Disable Vanilla Music",\n                    "Stops Minecraft's ambient soundtrack while preserving SCP Additions music.", false),\n''')
replace(
    "src/main/java/net/mcreator/scpadditions/config/ui/Scp079ModulesScreenExtension.java",
    '''        private static final int MUTED = 0xFF9CA3AF;\n''',
    '''        private static final int MUTED = 0xFF9CA3AF;\n        private static final int ON_COLOR = 0xFF79D58B;\n        private static final int OFF_COLOR = 0xFFFF8B8B;\n''')
replace(
    "src/main/java/net/mcreator/scpadditions/config/ui/Scp079ModulesScreenExtension.java",
    '''            int textX = left + Math.max(5,\n                    (button.getWidth() - font.width(label)) / 2);\n            int textY = top + Math.max(1,\n                    (button.getHeight() - 8) / 2);\n            graphics.drawString(font, label, textX, textY,\n                    textColor, false);\n''',
    '''            int textY = top + Math.max(1,\n                    (button.getHeight() - 8) / 2);\n            boolean toggleState = plain.endsWith(": ON")\n                    || plain.endsWith(": OFF");\n            if (toggleState) {\n                boolean enabled = plain.endsWith(": ON");\n                String state = enabled ? "ON" : "OFF";\n                String prefix = plain.substring(0, plain.length() - state.length());\n                Component prefixComponent = ScpFonts.roboto(prefix);\n                Component stateComponent = ScpFonts.roboto(state);\n                int totalWidth = font.width(prefixComponent)\n                        + font.width(stateComponent);\n                int textX = left + Math.max(5,\n                        (button.getWidth() - totalWidth) / 2);\n                graphics.drawString(font, prefixComponent, textX, textY,\n                        textColor, false);\n                graphics.drawString(font, stateComponent,\n                        textX + font.width(prefixComponent), textY,\n                        enabled ? ON_COLOR : OFF_COLOR, false);\n            } else {\n                int textX = left + Math.max(5,\n                        (button.getWidth() - font.width(label)) / 2);\n                graphics.drawString(font, label, textX, textY,\n                        textColor, false);\n            }\n''')

# Move only the free-text field frame upward; the typed text stays aligned.
replace(
    "src/main/java/net/mcreator/scpadditions/client/gui/ScpSignEditorScreen.java",
    '''    private static void drawField(GuiGraphics graphics, EditBox field) {\n        graphics.fill(field.getX() - 3, field.getY() - 1,\n                field.getX() + field.getWidth() + 3,\n                field.getY() + field.getHeight() + 1, FIELD_BACKGROUND);\n        outline(graphics, field.getX() - 3, field.getY() - 1,\n                field.getWidth() + 6, field.getHeight() + 2, FIELD_EDGE);\n    }\n''',
    '''    private static void drawField(GuiGraphics graphics, EditBox field) {\n        int frameTop = field.getY() - 6;\n        graphics.fill(field.getX() - 3, frameTop,\n                field.getX() + field.getWidth() + 3,\n                frameTop + field.getHeight() + 2, FIELD_BACKGROUND);\n        outline(graphics, field.getX() - 3, frameTop,\n                field.getWidth() + 6, field.getHeight() + 2, FIELD_EDGE);\n    }\n''')

# Starting to watch SCP-173 dismisses the whole follower group for that owner.
replace(
    "src/main/java/net/mcreator/scpadditions/entity/AbstractScp131Entity.java",
    '''        Scp173Entity scp173 = findNearestScp173();\n        if (scp173 != null) {\n            wasWatchingScp173 = true;\n            runToAndWatch(scp173);\n            return;\n        }\n''',
    '''        Scp173Entity scp173 = findNearestScp173();\n        if (scp173 != null) {\n            if (!wasWatchingScp173) {\n                dismissOwnerGroupForScp173();\n            }\n            wasWatchingScp173 = true;\n            runToAndWatch(scp173);\n            return;\n        }\n''')
replace(
    "src/main/java/net/mcreator/scpadditions/entity/AbstractScp131Entity.java",
    '''    private Scp173Entity findNearestScp173() {\n''',
    '''    private void dismissOwnerGroupForScp173() {\n        if (!isFollowing() || followOwner == null) {\n            return;\n        }\n        MinecraftServer server = getServer();\n        ServerPlayer owner = server == null ? null\n                : server.getPlayerList().getPlayer(followOwner);\n        if (owner != null) {\n            stopFollowersFor(owner);\n        } else {\n            stopFollowing();\n        }\n    }\n\n    private Scp173Entity findNearestScp173() {\n''')

# Changelog.
replace(
    "CHANGELOG.md",
    '''- SCP-131 no longer teleports back to distant owners; moving too far away now dismisses the follower normally.\n''',
    '''- SCP-131 no longer teleports back to distant owners; moving too far away now dismisses the follower normally;\n- SCP-131 now dismisses its owner's follower group when it begins watching SCP-173, matching the manual release behavior.\n''')
replace(
    "CHANGELOG.md",
    '''- Added a default-disabled module that removes vanilla attack, critical, and sweep impact sounds against non-player mobs.\n''',
    '''- Added a default-disabled module that removes vanilla attack, critical, and sweep impact sounds against non-player mobs;\n- Added a default-disabled **Disable Vanilla Music** module that suppresses Minecraft's ambient soundtrack without blocking SCP Additions soundtracks;\n- Colored module **ON** and **OFF** states green and red for faster visual scanning.\n''')

print("Final chat tweaks applied successfully.")
