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

        writeAtomically(absoluteTarget, content);
    }

    /**
     * Restores a previously captured file without replacing the known-good {@code .bak} copy.
     * Intended only for transactional rollback after a failed configuration save.
     */
    public static void restoreWithoutBackup(Path target, String content) throws IOException {
        writeAtomically(target.toAbsolutePath().normalize(), content);
    }

    private static void writeAtomically(Path absoluteTarget, String content) throws IOException {
        Path parent = absoluteTarget.getParent();
        if (parent != null) Files.createDirectories(parent);

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
