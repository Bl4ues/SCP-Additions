package net.mcreator.scpadditions.data;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;

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
    private static final List<BundledSkin> BUNDLED_SKINS = List.of(
            new BundledSkin("skin1.png", "skin4.png"),
            new BundledSkin("skin2.png", "skin5.png"),
            new BundledSkin("skin3.png", "skin7.png"),
            new BundledSkin("skin4.png", "skin10.png"),
            new BundledSkin("skin5.png", "skin11.png"));
    private static final List<String> OLD_DEFAULT_NAMES = List.of(
            "skin1.png", "skin2.png", "skin3.png", "skin4.png", "skin5.png",
            "skin6.png", "skin7.png", "skin8.png", "skin9.png", "skin10.png",
            "skin11.png");
    private static final String README = """
            SCP-914 1:1 custom skins

            Add Minecraft player skin PNG files to this directory.
            Supported layouts are 64x64 and legacy 64x32.

            When a player passes through SCP-914 on the 1:1 setting, one PNG
            from this directory is selected at random and stored on the player.

            Bundled defaults: skin1.png, skin2.png, skin3.png, skin4.png, and
            skin5.png. Additional PNG files are treated as custom skins.

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
            migrateOldBundledLayout();
            copyBundledDefaults();
            Files.writeString(SKIN_DIRECTORY.resolve("README.txt"), README,
                    StandardCharsets.UTF_8);
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

    private static void migrateOldBundledLayout() {
        Path readme = SKIN_DIRECTORY.resolve("README.txt");
        boolean oldReadme = false;
        try {
            oldReadme = Files.isRegularFile(readme)
                    && Files.readString(readme, StandardCharsets.UTF_8)
                    .contains("Bundled defaults: skin4.png");
        } catch (Exception ignored) {
        }

        boolean oldMarkers = Files.exists(SKIN_DIRECTORY.resolve("skin7.png"))
                || Files.exists(SKIN_DIRECTORY.resolve("skin10.png"))
                || Files.exists(SKIN_DIRECTORY.resolve("skin11.png"));
        if (!oldReadme && !oldMarkers) return;

        for (String name : OLD_DEFAULT_NAMES) {
            try {
                Files.deleteIfExists(SKIN_DIRECTORY.resolve(name));
            } catch (Exception exception) {
                ScpAdditionsMod.LOGGER.warn(
                        "Failed to remove legacy bundled SCP-914 skin {}", name,
                        exception);
            }
        }
    }

    private static void copyBundledDefaults() {
        for (BundledSkin bundled : BUNDLED_SKINS) {
            Path target = SKIN_DIRECTORY.resolve(bundled.exposedName());
            if (Files.exists(target)) continue;

            String resource = "assets/scp_additions/textures/entities/"
                    + bundled.resourceName();
            try (InputStream stream = Scp914SkinManager.class.getClassLoader()
                    .getResourceAsStream(resource)) {
                if (stream != null) {
                    Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception exception) {
                ScpAdditionsMod.LOGGER.warn(
                        "Failed to copy bundled SCP-914 skin {} as {}",
                        bundled.resourceName(), bundled.exposedName(), exception);
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

    private record BundledSkin(String exposedName, String resourceName) {
    }
}
