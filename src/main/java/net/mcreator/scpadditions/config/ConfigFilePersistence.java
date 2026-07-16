package net.mcreator.scpadditions.config;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Writes configuration files atomically and preserves the previous version as {@code .bak}. */
public final class ConfigFilePersistence {
    private static final int MOVE_ATTEMPTS = 6;

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
        IOException lastMoveFailure = null;
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }

            boolean atomicSupported = true;
            for (int attempt = 0; attempt < MOVE_ATTEMPTS && atomicSupported; attempt++) {
                try {
                    Files.move(temporary, absoluteTarget, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                    return;
                } catch (AtomicMoveNotSupportedException exception) {
                    atomicSupported = false;
                    lastMoveFailure = exception;
                } catch (IOException exception) {
                    lastMoveFailure = exception;
                    pauseBeforeRetry(attempt);
                }
            }

            for (int attempt = 0; attempt < MOVE_ATTEMPTS; attempt++) {
                try {
                    Files.move(temporary, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);
                    return;
                } catch (IOException exception) {
                    lastMoveFailure = exception;
                    pauseBeforeRetry(attempt);
                }
            }

            // Windows may allow rewriting a file while refusing to replace its directory entry.
            // The .bak copy already exists at this point, so this remains recoverable even though
            // the final fallback is not atomic.
            try {
                Files.writeString(absoluteTarget, content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
            } catch (IOException directFailure) {
                if (lastMoveFailure != null) directFailure.addSuppressed(lastMoveFailure);
                throw directFailure;
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void pauseBeforeRetry(int attempt) throws IOException {
        try {
            Thread.sleep(15L * (attempt + 1));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while retrying configuration save", exception);
        }
    }
}
