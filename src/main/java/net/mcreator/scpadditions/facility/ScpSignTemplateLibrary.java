package net.mcreator.scpadditions.facility;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** World-scoped library of reusable custom sign names and PNG images. */
public final class ScpSignTemplateLibrary extends SavedData {
    private static final String DATA_NAME =
            "scp_additions_scp_sign_templates";
    private static final String ENTRIES_KEY = "Entries";
    private static final String ID_KEY = "Id";
    private static final String NAME_KEY = "Name";
    private static final String IMAGE_KEY = "Image";

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    private ScpSignTemplateLibrary() {
    }

    public static ScpSignTemplateLibrary get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                ScpSignTemplateLibrary::load,
                ScpSignTemplateLibrary::new,
                DATA_NAME);
    }

    private static ScpSignTemplateLibrary load(CompoundTag tag) {
        ScpSignTemplateLibrary library = new ScpSignTemplateLibrary();
        ListTag list = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        int totalBytes = 0;
        for (int index = 0; index < list.size()
                && library.entries.size()
                < ScpSignTemplates.MAX_CUSTOM_TEMPLATES; index++) {
            CompoundTag entryTag = list.getCompound(index);
            String id = ScpSignTemplates.cleanId(entryTag.getString(ID_KEY));
            String name = ScpSignTemplates.cleanName(
                    entryTag.getString(NAME_KEY));
            byte[] image = entryTag.getByteArray(IMAGE_KEY);
            if (!ScpSignTemplates.isCustom(id)
                    || !validImage(image)
                    || totalBytes + image.length
                    > ScpSignTemplates.MAX_TOTAL_IMAGE_BYTES) {
                continue;
            }
            totalBytes += image.length;
            library.entries.put(id, new Entry(id, name,
                    Arrays.copyOf(image, image.length)));
        }
        return library;
    }

    public synchronized List<ScpSignTemplateSummary> summaries() {
        return entries.values().stream()
                .map(Entry::summary)
                .toList();
    }

    public synchronized Entry entry(String id) {
        Entry entry = entries.get(ScpSignTemplates.cleanId(id));
        return entry == null ? null : entry.copy();
    }

    public synchronized boolean contains(String id) {
        return entries.containsKey(ScpSignTemplates.cleanId(id));
    }

    public synchronized Entry create(String requestedName, byte[] image) {
        if (entries.size() >= ScpSignTemplates.MAX_CUSTOM_TEMPLATES
                || !validImage(image)
                || totalImageBytes() + image.length
                > ScpSignTemplates.MAX_TOTAL_IMAGE_BYTES) {
            return null;
        }
        String id = ScpSignTemplates.CUSTOM_PREFIX
                + UUID.randomUUID().toString().replace("-", "");
        Entry entry = new Entry(id,
                uniqueName(ScpSignTemplates.cleanName(requestedName)),
                Arrays.copyOf(image, image.length));
        entries.put(id, entry);
        setDirty();
        return entry.copy();
    }

    public synchronized boolean delete(String id) {
        if (entries.remove(ScpSignTemplates.cleanId(id)) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public synchronized String validateSelection(String id) {
        String clean = ScpSignTemplates.cleanId(id);
        if (ScpSignTemplates.isBuiltIn(clean)) return clean;
        return entries.containsKey(clean)
                ? clean : ScpSignTemplates.INFORMATION;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(ID_KEY, entry.id());
            entryTag.putString(NAME_KEY, entry.name());
            entryTag.put(IMAGE_KEY, new ByteArrayTag(entry.image()));
            list.add(entryTag);
        }
        tag.put(ENTRIES_KEY, list);
        return tag;
    }

    private String uniqueName(String requested) {
        String base = requested;
        String candidate = base;
        int suffix = 2;
        while (hasName(candidate)) {
            String suffixText = " (" + suffix++ + ")";
            int maximumBase = Math.max(1,
                    ScpSignTemplates.MAX_NAME_LENGTH - suffixText.length());
            candidate = base.substring(0,
                    Math.min(base.length(), maximumBase)) + suffixText;
        }
        return candidate;
    }

    private boolean hasName(String name) {
        return entries.values().stream().anyMatch(entry ->
                entry.name().equalsIgnoreCase(name));
    }

    private int totalImageBytes() {
        int total = 0;
        for (Entry entry : entries.values()) total += entry.image().length;
        return total;
    }

    private static boolean validImage(byte[] image) {
        if (image == null || image.length < 24
                || image.length > ScpSignTemplates.MAX_IMAGE_BYTES) {
            return false;
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(image)) {
            BufferedImage decoded = ImageIO.read(input);
            return decoded != null
                    && decoded.getWidth() == ScpSignTemplates.TARGET_WIDTH
                    && decoded.getHeight() == ScpSignTemplates.TARGET_HEIGHT;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    public record Entry(String id, String name, byte[] image) {
        public Entry {
            id = ScpSignTemplates.cleanId(id);
            name = ScpSignTemplates.cleanName(name);
            image = image == null ? new byte[0]
                    : Arrays.copyOf(image, image.length);
        }

        public ScpSignTemplateSummary summary() {
            return new ScpSignTemplateSummary(id, name, true);
        }

        public Entry copy() {
            return new Entry(id, name, image);
        }
    }
}
