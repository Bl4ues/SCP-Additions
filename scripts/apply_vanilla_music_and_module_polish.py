from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Missing expected block: {label}")
    return text.replace(old, new, 1)


# Module config and bundled defaults.
path = Path("src/main/java/net/mcreator/scpadditions/config/ScpAdditionsModulesConfig.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''\t\t@SerializedName("mute_non_player_hit_sounds")
\t\tpublic boolean muteNonPlayerHitSounds = false;
''',
'''\t\t@SerializedName("mute_non_player_hit_sounds")
\t\tpublic boolean muteNonPlayerHitSounds = false;

\t\t@SerializedName("disable_vanilla_music")
\t\tpublic boolean disableVanillaMusic = false;
''', "audio config field")
path.write_text(text, encoding="utf-8")

path = Path("config/scpadditions/modules.json")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''    "enter_sound_enabled": true,
    "replace_player_hurt_sounds": true,
    "mute_non_player_hit_sounds": false
''',
'''    "enter_sound_enabled": true,
    "replace_player_hurt_sounds": true,
    "mute_non_player_hit_sounds": false,
    "disable_vanilla_music": false
''', "bundled audio config")
path.write_text(text, encoding="utf-8")

# Client runtime synchronization.
path = Path("src/main/java/com/bl4ues/scpinventory/config/InventoryModuleRuntimeState.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''    private static volatile Boolean serverReplacePlayerHurtSounds;
    private static volatile Boolean serverMuteNonPlayerHitSounds;
''',
'''    private static volatile Boolean serverReplacePlayerHurtSounds;
    private static volatile Boolean serverMuteNonPlayerHitSounds;
    private static volatile Boolean serverDisableVanillaMusic;
''', "runtime state field")
text = replace_once(text,
'''    public static boolean muteNonPlayerHitSoundsForClient() {
        Boolean synced = serverMuteNonPlayerHitSounds;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().audio.muteNonPlayerHitSounds;
    }

    public static void updateFromServer(boolean enabled,
''',
'''    public static boolean muteNonPlayerHitSoundsForClient() {
        Boolean synced = serverMuteNonPlayerHitSounds;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().audio.muteNonPlayerHitSounds;
    }

    public static boolean disableVanillaMusicForClient() {
        Boolean synced = serverDisableVanillaMusic;
        return synced != null ? synced
                : ScpAdditionsModulesConfig.get().audio.disableVanillaMusic;
    }

    public static void updateFromServer(boolean enabled,
''', "runtime state getter")
text = replace_once(text,
'''            boolean replacePlayerHurtSounds,
            boolean muteNonPlayerHitSounds) {
''',
'''            boolean replacePlayerHurtSounds,
            boolean muteNonPlayerHitSounds,
            boolean disableVanillaMusic) {
''', "runtime update signature")
text = replace_once(text,
'''        serverReplacePlayerHurtSounds = replacePlayerHurtSounds;
        serverMuteNonPlayerHitSounds = muteNonPlayerHitSounds;
''',
'''        serverReplacePlayerHurtSounds = replacePlayerHurtSounds;
        serverMuteNonPlayerHitSounds = muteNonPlayerHitSounds;
        serverDisableVanillaMusic = disableVanillaMusic;
''', "runtime update assignment")
text = replace_once(text,
'''        serverReplacePlayerHurtSounds = null;
        serverMuteNonPlayerHitSounds = null;
''',
'''        serverReplacePlayerHurtSounds = null;
        serverMuteNonPlayerHitSounds = null;
        serverDisableVanillaMusic = null;
''', "runtime clear")
path.write_text(text, encoding="utf-8")

path = Path("src/main/java/com/bl4ues/scpinventory/network/InventoryModuleStatePacket.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''public record InventoryModuleStatePacket(boolean enabled,
        boolean reduceScp012VisualEffects, boolean hungerDisabled,
        boolean replacePlayerHurtSounds, boolean muteNonPlayerHitSounds) {
''',
'''public record InventoryModuleStatePacket(boolean enabled,
        boolean reduceScp012VisualEffects, boolean hungerDisabled,
        boolean replacePlayerHurtSounds, boolean muteNonPlayerHitSounds,
        boolean disableVanillaMusic) {
''', "packet record")
text = replace_once(text,
'''        buffer.writeBoolean(message.replacePlayerHurtSounds);
        buffer.writeBoolean(message.muteNonPlayerHitSounds);
''',
'''        buffer.writeBoolean(message.replacePlayerHurtSounds);
        buffer.writeBoolean(message.muteNonPlayerHitSounds);
        buffer.writeBoolean(message.disableVanillaMusic);
''', "packet encode")
text = replace_once(text,
'''        return new InventoryModuleStatePacket(buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean());
''',
'''        return new InventoryModuleStatePacket(buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean());
''', "packet decode")
text = replace_once(text,
'''                message.hungerDisabled, message.replacePlayerHurtSounds,
                message.muteNonPlayerHitSounds));
''',
'''                message.hungerDisabled, message.replacePlayerHurtSounds,
                message.muteNonPlayerHitSounds,
                message.disableVanillaMusic));
''', "packet handle")
path.write_text(text, encoding="utf-8")

path = Path("src/main/java/com/bl4ues/scpinventory/network/ModNetwork.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''    private static final String PROTOCOL_VERSION = "9";
''',
'''    private static final String PROTOCOL_VERSION = "10";
''', "network protocol version")
text = replace_once(text,
'''                        ScpAdditionsModulesConfig.get().audio
                                .muteNonPlayerHitSounds));
''',
'''                        ScpAdditionsModulesConfig.get().audio
                                .muteNonPlayerHitSounds,
                        ScpAdditionsModulesConfig.get().audio
                                .disableVanillaMusic));
''', "module state sync")
path.write_text(text, encoding="utf-8")

# Suppress only Minecraft's own music manager, preserving custom SoundInstances.
path = Path("src/main/java/net/mcreator/scpadditions/client/ModMusicExclusivityClient.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''import net.mcreator.scpadditions.ScpAdditionsMod;
''',
'''import net.mcreator.scpadditions.ScpAdditionsMod;
import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
''', "music runtime import")
text = replace_once(text,
'''        if (event.phase != TickEvent.Phase.END
                || !hasActiveModMusic()) {
            return;
        }
        stopVanillaMusicNow();
''',
'''        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (InventoryModuleRuntimeState.disableVanillaMusicForClient()
                || hasActiveModMusic()) {
            stopVanillaMusicNow();
        }
''', "music tick policy")
path.write_text(text, encoding="utf-8")

# Validation and snapshot defaults.
path = Path("src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterService.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''        checkBoolean(root, "audio", "replace_player_hurt_sounds", errors);
        checkBoolean(root, "audio", "mute_non_player_hit_sounds", errors);
''',
'''        checkBoolean(root, "audio", "replace_player_hurt_sounds", errors);
        checkBoolean(root, "audio", "mute_non_player_hit_sounds", errors);
        checkBoolean(root, "audio", "disable_vanilla_music", errors);
''', "module validation")
text = replace_once(text,
'''\"audio\":{\"enter_sound_enabled\":true,\"replace_player_hurt_sounds\":true,\"mute_non_player_hit_sounds\":false},\"accessibility\"''',
'''\"audio\":{\"enter_sound_enabled\":true,\"replace_player_hurt_sounds\":true,\"mute_non_player_hit_sounds\":false,\"disable_vanilla_music\":false},\"accessibility\"''', "module snapshot defaults")
path.write_text(text, encoding="utf-8")

# Module row and high-visibility ON/OFF state colors.
path = Path("src/main/java/net/mcreator/scpadditions/config/ui/Scp079ModulesScreenExtension.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''            new Row("audio", "enter_sound_enabled", "World Entry Sound",
                    "Plays enter.ogg after joining or opening a world.", true),
            new Row("audio", "replace_player_hurt_sounds",
''',
'''            new Row("audio", "enter_sound_enabled", "World Entry Sound",
                    "Plays enter.ogg after joining or opening a world.", true),
            new Row("audio", "disable_vanilla_music", "Disable Vanilla Music",
                    "Stops Minecraft's ambient soundtrack while preserving SCP Additions music.", false),
            new Row("audio", "replace_player_hurt_sounds",
''', "vanilla music module row")
text = replace_once(text,
'''        private static final int WHITE = 0xFFF7F8FC;
        private static final int MUTED = 0xFF9CA3AF;
        private static final int ROW_HEIGHT = 34;
''',
'''        private static final int WHITE = 0xFFF7F8FC;
        private static final int MUTED = 0xFF9CA3AF;
        private static final int MODULE_ON = 0xFF79D58B;
        private static final int MODULE_OFF = 0xFFFF8B8B;
        private static final int ROW_HEIGHT = 34;
''', "module state colors")
text = replace_once(text,
'''            int textX = left + Math.max(5,
                    (button.getWidth() - font.width(label)) / 2);
            int textY = top + Math.max(1,
                    (button.getHeight() - 8) / 2);
            graphics.drawString(font, label, textX, textY,
                    textColor, false);
''',
'''            int textY = top + Math.max(1,
                    (button.getHeight() - 8) / 2);
            int stateLength = plain.endsWith(": ON") ? 2
                    : plain.endsWith(": OFF") ? 3 : 0;
            if (stateLength > 0) {
                String prefix = plain.substring(0, plain.length() - stateLength);
                String state = plain.substring(plain.length() - stateLength);
                Component prefixLabel = ScpFonts.roboto(prefix);
                Component stateLabel = ScpFonts.roboto(state);
                int totalWidth = font.width(prefixLabel) + font.width(stateLabel);
                int textX = left + Math.max(5,
                        (button.getWidth() - totalWidth) / 2);
                graphics.drawString(font, prefixLabel, textX, textY,
                        textColor, false);
                int stateColor = !button.active ? MUTED
                        : "ON".equals(state) ? MODULE_ON : MODULE_OFF;
                graphics.drawString(font, stateLabel,
                        textX + font.width(prefixLabel), textY,
                        stateColor, false);
            } else {
                int textX = left + Math.max(5,
                        (button.getWidth() - font.width(label)) / 2);
                graphics.drawString(font, label, textX, textY,
                        textColor, false);
            }
''', "module colored label rendering")
path.write_text(text, encoding="utf-8")

# Move only the free-text field backgrounds upward; leave text baselines intact.
path = Path("src/main/java/net/mcreator/scpadditions/client/gui/ScpSignEditorScreen.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''    private static void drawField(GuiGraphics graphics, EditBox field) {
        graphics.fill(field.getX() - 3, field.getY() - 1,
                field.getX() + field.getWidth() + 3,
                field.getY() + field.getHeight() + 1, FIELD_BACKGROUND);
        outline(graphics, field.getX() - 3, field.getY() - 1,
                field.getWidth() + 6, field.getHeight() + 2, FIELD_EDGE);
    }
''',
'''    private static void drawField(GuiGraphics graphics, EditBox field) {
        int frameY = field.getY() - TEXT_FIELD_Y_OFFSET;
        graphics.fill(field.getX() - 3, frameY - 1,
                field.getX() + field.getWidth() + 3,
                frameY + field.getHeight() + 1, FIELD_BACKGROUND);
        outline(graphics, field.getX() - 3, frameY - 1,
                field.getWidth() + 6, field.getHeight() + 2, FIELD_EDGE);
    }
''', "SCP sign text field frames")
path.write_text(text, encoding="utf-8")

# Entering SCP-173 watch mode dismisses the owner's whole SCP-131 group.
path = Path("src/main/java/net/mcreator/scpadditions/entity/AbstractScp131Entity.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''        Scp173Entity scp173 = findNearestScp173();
        if (scp173 != null) {
            wasWatchingScp173 = true;
            runToAndWatch(scp173);
            return;
        }
''',
'''        Scp173Entity scp173 = findNearestScp173();
        if (scp173 != null) {
            if (!wasWatchingScp173 && isFollowing()) {
                dismissFollowersForScp173();
            }
            wasWatchingScp173 = true;
            runToAndWatch(scp173);
            return;
        }
''', "SCP-131 watch transition")
text = replace_once(text,
'''    private void runToAndWatch(Scp173Entity scp173) {
''',
'''    private void dismissFollowersForScp173() {
        if (!isFollowing()) {
            return;
        }
        MinecraftServer server = getServer();
        if (server == null || followOwner == null) {
            stopFollowing();
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(followOwner);
        if (owner != null && stopFollowersFor(owner)) {
            ScpEntityNetwork.showScp131Notice(owner, false);
        } else {
            stopFollowing();
        }
    }

    private void runToAndWatch(Scp173Entity scp173) {
''', "SCP-131 automatic dismiss helper")
path.write_text(text, encoding="utf-8")

# Changelog.
path = Path("CHANGELOG.md")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''## SCP-131

- SCP-131 no longer teleports back to distant owners; moving too far away now dismisses the follower normally.
''',
'''## SCP-131

- SCP-131 no longer teleports back to distant owners; moving too far away now dismisses the follower normally;
- SCP-131 instances now dismiss their player's follower group when they begin watching SCP-173, matching the manual dismiss action.
''', "SCP-131 changelog")
text = replace_once(text,
'''- Added a default-disabled module that removes vanilla attack, critical, and sweep impact sounds against non-player mobs.
''',
'''- Added a default-disabled module that removes vanilla attack, critical, and sweep impact sounds against non-player mobs;
- Added a default-disabled **Disable Vanilla Music** module that suppresses Minecraft's ambient soundtrack without blocking SCP Additions' contextual music.
''', "audio changelog")
text = replace_once(text,
'''- Fine-tuned the SCP Sign typography and Anomaly Trait selector alignment against direct SCP Unity comparisons.
''',
'''- Fine-tuned the SCP Sign typography and Anomaly Trait selector alignment against direct SCP Unity comparisons;
- Corrected the free-text editor field backgrounds without moving their aligned text baselines.
''', "sign editor changelog")
text = replace_once(text,
'''## Accessibility

- Added a dedicated Accessibility screen''',
'''## Accessibility

- Colored module **ON** and **OFF** states green and red for faster visual scanning;
- Added a dedicated Accessibility screen''', "module color changelog")
path.write_text(text, encoding="utf-8")
