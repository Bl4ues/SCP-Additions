package net.mcreator.scpadditions.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Writes configuration files atomically and preserves the previous version as {@code .bak}. */
public final class ConfigFilePersistence {
    private ConfigFilePersistence() {
    }

    public static void writeWithBackup(Path target, String content) throws IOException {
        Path absoluteTarget = target.toAbsolutePath().normalize();
        Path parent = absoluteTarget.getParent();
        if (parent != null) Files.createDirectories(parent);

        if (Files.isRegularFile(absoluteTarget)) {
            Path backup = absoluteTarget.resolveSibling(absoluteTarget.getFileName() + ".bak");
            Files.copy(absoluteTarget, backup, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
        }

        Path temporary = Files.createTempFile(parent, absoluteTarget.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, absoluteTarget, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
