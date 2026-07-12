package net.mcreator.scpadditions.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class Scp914SkinManager {
    private static final Path SKIN_DIRECTORY = FMLPaths.CONFIGDIR.get()
            .resolve("scpadditions")
            .resolve("scp914_skins");
    private static final int LEGACY_SKIN_COUNT = 11;
    private static final String README = """
            SCP-914 1:1 custom skins

            Add Minecraft player skin PNG files to this directory.
            Supported layouts are 64x64 and legacy 64x32.

            When a player passes through SCP-914 on the 1:1 setting, one PNG
            from this directory is selected at random and stored on the player.

            Kleiders Custom Renderer is optional, but it must be installed on a
            client for the selected SCP-914 skin to be rendered.

            In multiplayer, each client must have the same skin filenames in
            this directory. The server synchronizes the selected filename, not
            the image bytes.
            """;

    private Scp914SkinManager() {
    }

    public static void initialize() {
        try {
            Files.createDirectories(SKIN_DIRECTORY);
            copyLegacyDefaults();
            Path readme = SKIN_DIRECTORY.resolve("README.txt");
            if (Files.notExists(readme)) {
                Files.writeString(readme, README, StandardCharsets.UTF_8);
            }
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Failed to initialize SCP-914 skin directory {}", SKIN_DIRECTORY,
                    exception);
        }
    }

    public static void assignRandomSkin(ServerPlayer player) {
        List<String> skins = availableSkinNames();
        if (skins.isEmpty()) {
            ScpAdditionsMod.LOGGER.warn(
                    "SCP-914 1:1 could not change {} because {} contains no PNG skins",
                    player.getScoreboardName(), SKIN_DIRECTORY);
            return;
        }

        String selected = skins.get(player.getRandom().nextInt(skins.size()));
        player.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY)
                .ifPresent(variables -> {
                    clearLegacyFlags(variables);
                    variables.scp914Skin = selected;
                    variables.syncPlayerVariables(player);
                });
    }

    public static Path resolveSkin(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        String safeName = Path.of(fileName).getFileName().toString();
        if (!safeName.equals(fileName)
                || !safeName.toLowerCase(Locale.ROOT).endsWith(".png")) {
            return null;
        }

        Path resolved = SKIN_DIRECTORY.resolve(safeName).normalize();
        return resolved.startsWith(SKIN_DIRECTORY) && Files.isRegularFile(resolved)
                ? resolved : null;
    }

    public static Path skinDirectory() {
        return SKIN_DIRECTORY;
    }

    private static List<String> availableSkinNames() {
        initialize();
        try (Stream<Path> files = Files.list(SKIN_DIRECTORY)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Failed to list SCP-914 skins in {}", SKIN_DIRECTORY, exception);
            return List.of();
        }
    }

    private static void copyLegacyDefaults() {
        for (int index = 1; index <= LEGACY_SKIN_COUNT; index++) {
            String fileName = "skin" + index + ".png";
            Path target = SKIN_DIRECTORY.resolve(fileName);
            if (Files.exists(target)) {
                continue;
            }

            String resource = "assets/scp_additions/textures/entities/" + fileName;
            try (InputStream stream = Scp914SkinManager.class.getClassLoader()
                    .getResourceAsStream(resource)) {
                if (stream != null) {
                    Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception exception) {
                ScpAdditionsMod.LOGGER.warn(
                        "Failed to copy bundled SCP-914 skin {}", fileName, exception);
            }
        }
    }

    private static void clearLegacyFlags(
            ScpAdditionsModVariables.PlayerVariables variables) {
        variables.PlayerOn1to1 = false;
        variables.PlayerOn1to1_2 = false;
        variables.PlayerOn1to1_3 = false;
        variables.PlayerOn1to1_4 = false;
        variables.PlayerOn1to1_5 = false;
        variables.PlayerOn1to1_6 = false;
        variables.PlayerOn1to1_7 = false;
        variables.PlayerOn1to1_8 = false;
        variables.PlayerOn1to1_9 = false;
        variables.PlayerOn1to1_10 = false;
        variables.PlayerOn1to1_11 = false;
    }
}
