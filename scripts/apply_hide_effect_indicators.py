from pathlib import Path


def replace(path: str, old: str, new: str) -> None:
    target = Path(path)
    text = target.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}: {old[:160]!r}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


# Module model and bundled defaults.
replace(
    "src/main/java/net/mcreator/scpadditions/config/ScpAdditionsModulesConfig.java",
    "\t\tpublic Toggle hud = new Toggle();\n",
    "\t\tpublic Hud hud = new Hud();\n",
)
replace(
    "src/main/java/net/mcreator/scpadditions/config/ScpAdditionsModulesConfig.java",
    "\t\t\tif (hud == null) hud = new Toggle();\n",
    "\t\t\tif (hud == null) hud = new Hud();\n",
)
replace(
    "src/main/java/net/mcreator/scpadditions/config/ScpAdditionsModulesConfig.java",
    '''\tpublic static class Toggle {
\t\tpublic boolean enabled = true;
\t}

\tpublic static final class Interactions extends Toggle {
''',
    '''\tpublic static class Toggle {
\t\tpublic boolean enabled = true;
\t}

\tpublic static final class Hud extends Toggle {
\t\t@SerializedName("hide_active_effect_indicators")
\t\tpublic boolean hideActiveEffectIndicators = true;
\t}

\tpublic static final class Interactions extends Toggle {
''',
)
replace(
    "config/scpadditions/modules.json",
    '''  "hud": {
    "enabled": true
  },
''',
    '''  "hud": {
    "enabled": true,
    "hide_active_effect_indicators": true
  },
''',
)

# Configuration Center validation, fallback and visible row.
replace(
    "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterService.java",
    '''        checkBoolean(root, "hud", "enabled", errors);
''',
    '''        checkBoolean(root, "hud", "enabled", errors);
        checkBoolean(root, "hud", "hide_active_effect_indicators", errors);
''',
)
replace(
    "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterService.java",
    '''\\"hud\\":{\\"enabled\\":true}''',
    '''\\"hud\\":{\\"enabled\\":true,\\"hide_active_effect_indicators\\":true}''',
)
replace(
    "src/main/java/net/mcreator/scpadditions/config/ui/Scp079ModulesScreenExtension.java",
    '''            new Row("hud", "enabled", "Custom HUD",
                    "Shows the SCP Additions health, stamina and blink presentation.", true),
''',
    '''            new Row("hud", "enabled", "Custom HUD",
                    "Shows the SCP Additions health, stamina and blink presentation.", true),
            new Row("hud", "hide_active_effect_indicators",
                    "Hide Active Effect Indicators",
                    "Hides vanilla status-effect icons from the HUD without changing inventory or SCP Conditions displays.", true),
''',
)

# Synchronize the client-only overlay setting from the server.
replace(
    "src/main/java/com/bl4ues/scpinventory/network/ModNetwork.java",
    '''    private static final String PROTOCOL_VERSION = "10";
''',
    '''    private static final String PROTOCOL_VERSION = "11";
''',
)
replace(
    "src/main/java/com/bl4ues/scpinventory/network/ModNetwork.java",
    '''                        ScpAdditionsModulesConfig.get().audio
                                .disableVanillaMusic));
''',
    '''                        ScpAdditionsModulesConfig.get().audio
                                .disableVanillaMusic,
                        ScpAdditionsModulesConfig.get().hud
                                .hideActiveEffectIndicators));
''',
)

replace(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryModuleStatePacket.java",
    '''        boolean replacePlayerHurtSounds, boolean muteNonPlayerHitSounds,
        boolean disableVanillaMusic) {
''',
    '''        boolean replacePlayerHurtSounds, boolean muteNonPlayerHitSounds,
        boolean disableVanillaMusic,
        boolean hideActiveEffectIndicators) {
''',
)
replace(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryModuleStatePacket.java",
    '''        buffer.writeBoolean(message.disableVanillaMusic);
''',
    '''        buffer.writeBoolean(message.disableVanillaMusic);
        buffer.writeBoolean(message.hideActiveEffectIndicators);
''',
)
replace(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryModuleStatePacket.java",
    '''                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean());
''',
    '''                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean());
''',
)
replace(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryModuleStatePacket.java",
    '''                message.muteNonPlayerHitSounds,
                message.disableVanillaMusic));
''',
    '''                message.muteNonPlayerHitSounds,
                message.disableVanillaMusic,
                message.hideActiveEffectIndicators));
''',
)

replace(
    "src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java",
    '''    private static volatile Boolean serverDisableVanillaMusic;
''',
    '''    private static volatile Boolean serverDisableVanillaMusic;
    private static volatile Boolean serverHideActiveEffectIndicators;
''',
)
replace(
    "src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java",
    '''    public static void updateFromServer(boolean enabled,
''',
    '''    public static boolean hideActiveEffectIndicatorsForClient() {
        Boolean synced = serverHideActiveEffectIndicators;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().hud
                .hideActiveEffectIndicators;
    }

    public static void updateFromServer(boolean enabled,
''',
)
replace(
    "src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java",
    '''            boolean muteNonPlayerHitSounds,
            boolean disableVanillaMusic) {
''',
    '''            boolean muteNonPlayerHitSounds,
            boolean disableVanillaMusic,
            boolean hideActiveEffectIndicators) {
''',
)
replace(
    "src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java",
    '''        serverDisableVanillaMusic = disableVanillaMusic;
''',
    '''        serverDisableVanillaMusic = disableVanillaMusic;
        serverHideActiveEffectIndicators = hideActiveEffectIndicators;
''',
)
replace(
    "src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java",
    '''        serverDisableVanillaMusic = null;
''',
    '''        serverDisableVanillaMusic = null;
        serverHideActiveEffectIndicators = null;
''',
)

# Cancel only the vanilla potion/status-effect icon HUD overlay.
replace(
    "src/main/java/net/mcreator/scpadditions/vitals/client/ClientVitalsEvents.java",
    '''        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (InventoryModuleRuntimeState.hungerDisabledForClient()
''',
    '''        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (InventoryModuleRuntimeState.hideActiveEffectIndicatorsForClient()
                && event.getOverlay().id().equals(
                        VanillaGuiOverlay.POTION_ICONS.id())) {
            event.setCanceled(true);
            return;
        }
        if (InventoryModuleRuntimeState.hungerDisabledForClient()
''',
)

# Changelog.
replace(
    "CHANGELOG.md",
    '''- Made the custom health module hide both the vanilla heart display and armor bar while its replacement HUD is active.
''',
    '''- Made the custom health module hide both the vanilla heart display and armor bar while its replacement HUD is active;
- Added a default-enabled **Hide Active Effect Indicators** module that removes vanilla status-effect icons from the HUD while preserving inventory and SCP Conditions displays.
''',
)

print("Hide Active Effect Indicators module applied successfully.")
