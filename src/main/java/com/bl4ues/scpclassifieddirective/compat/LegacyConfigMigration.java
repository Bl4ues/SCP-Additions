package com.bl4ues.scpclassifieddirective.compat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import net.minecraftforge.fml.loading.FMLPaths;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/**
 * Migrates configuration files from every pre-rebrand location without
 * overwriting user-edited files in the current configuration directory.
 */
public final class LegacyConfigMigration {
    private static final List<String> LEGACY_NAMESPACES = List.of(
            "scp_additions", "scp_unity_extra_blocks", "scp_ublocks", "scpinventory");
    private static final List<String> TEXT_EXTENSIONS = List.of(
            ".json", ".toml", ".cfg", ".properties", ".txt", ".csv", ".yaml", ".yml");

    private LegacyConfigMigration() {
    }

    public static void migrate() {
        Path root = FMLPaths.CONFIGDIR.get();
        Path destination = root.resolve(ScpClassifiedDirectiveMod.MODID);
        try {
            Files.createDirectories(destination);
            copyLegacyTree(root.resolve("scpadditions"), destination);
            copyLegacyTree(root.resolve("scpinventory"), destination);

            // A previous migration may already have copied the file before the
            // namespace changed, or the user may have retained an older ID in a
            // current config. Rewrite only resource-location namespace prefixes;
            // unrelated legacy words and user data are deliberately untouched.
            rewriteLegacyResourceIds(destination);
        } catch (IOException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not migrate legacy SCP configuration", exception);
        }
    }

    private static void copyLegacyTree(Path source, Path destination)
            throws IOException {
        if (!Files.isDirectory(source)) return;
        try (var stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path target = destination.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                    continue;
                }
                if (Files.exists(target)) continue;
                Files.createDirectories(target.getParent());
                if (isTextConfig(path)) {
                    Files.writeString(target,
                            rewriteLegacyResourceIds(Files.readString(
                                    path, StandardCharsets.UTF_8)),
                            StandardCharsets.UTF_8);
                } else {
                    Files.copy(path, target,
                            StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private static void rewriteLegacyResourceIds(Path destination)
            throws IOException {
        if (!Files.isDirectory(destination)) return;
        try (var stream = Files.walk(destination)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                if (!isTextConfig(path)) continue;
                String original = Files.readString(path,
                        StandardCharsets.UTF_8);
                String migrated = rewriteLegacyResourceIds(original);
                if (!original.equals(migrated)) {
                    Files.writeString(path, migrated,
                            StandardCharsets.UTF_8);
                }
            }
        }
    }

    private static String rewriteLegacyResourceIds(String contents) {
        String migrated = contents;
        for (String legacyNamespace : LEGACY_NAMESPACES) {
            migrated = migrated.replace(legacyNamespace + ":",
                    ScpClassifiedDirectiveMod.MODID + ":");
        }
        return migrated;
    }

    private static boolean isTextConfig(Path path) {
        String name = path.getFileName().toString()
                .toLowerCase(Locale.ROOT);
        return TEXT_EXTENSIONS.stream().anyMatch(name::endsWith);
    }
}
