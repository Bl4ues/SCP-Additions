package net.mcreator.scpadditions.document;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.init.DocumentItems;

import java.util.Locale;
import java.util.UUID;

/** Persistent, server-sanitized data carried by the dedicated document item. */
public final class DocumentData {
    public static final String ROOT_TAG = "ScpDocument";
    public static final String CODEX_ID_TAG = "ScpCodexId";
    public static final int SCHEMA_VERSION = 2;
    public static final int MAX_SHORT_TEXT = 256;
    public static final int MAX_BODY_TEXT = 65_536;
    public static final int MAX_ASSET_KEY = 160;

    private DocumentData() {
    }

    public static boolean isDedicatedItem(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && DocumentItems.DOCUMENT_ID.equals(
                ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public static boolean hasStructuredData(ItemStack stack) {
        return isDedicatedItem(stack) && stack.hasTag()
                && stack.getTag().contains(ROOT_TAG, Tag.TAG_COMPOUND);
    }

    public static ItemStack createDefaultStack() {
        ItemStack stack = new ItemStack(DocumentItems.getDocument());
        write(stack, Template.BLANK_DOCUMENT.createState(UUID.randomUUID().toString()));
        return stack;
    }

    public static void ensureInitialized(ItemStack stack) {
        if (!isDedicatedItem(stack) || hasStructuredData(stack)) return;
        String existingId = stack.hasTag() ? stack.getTag().getString(CODEX_ID_TAG) : "";
        String id = existingId.isBlank() ? UUID.randomUUID().toString() : existingId;
        State initial = Template.BLANK_DOCUMENT.createState(id);
        if (stack.hasCustomHoverName()) {
            initial = new State(initial.documentId(), initial.template(),
                    stack.getHoverName().getString(), initial.category(),
                    initial.header1(), initial.value1(), initial.header2(),
                    initial.value2(), initial.header3(), initial.value3(),
                    initial.body(), initial.photoKey(), initial.photoWidth(),
                    initial.photoHeight(), initial.caption());
        }
        write(stack, initial);
    }

    public static State read(ItemStack stack) {
        if (!isDedicatedItem(stack)) return Template.BLANK_DOCUMENT.createState("");
        CompoundTag root = stack.getTagElement(ROOT_TAG);
        if (root == null) {
            String id = stack.hasTag() ? stack.getTag().getString(CODEX_ID_TAG) : "";
            String title = stack.hasCustomHoverName() ? stack.getHoverName().getString() : "Blank Document";
            State fallback = Template.BLANK_DOCUMENT.createState(id);
            return new State(id, fallback.template(), title, fallback.category(),
                    fallback.header1(), fallback.value1(), fallback.header2(),
                    fallback.value2(), fallback.header3(), fallback.value3(),
                    fallback.body(), "", 0, 0, "");
        }
        String id = clean(root.getString("DocumentId"), MAX_SHORT_TEXT);
        if (id.isBlank() && stack.hasTag()) id = clean(stack.getTag().getString(CODEX_ID_TAG), MAX_SHORT_TEXT);
        Template template = Template.byId(root.getString("Template"));
        return sanitize(new State(
                id,
                template,
                clean(root.getString("Title"), MAX_SHORT_TEXT),
                clean(root.getString("Category"), MAX_SHORT_TEXT),
                clean(root.getString("Header1"), MAX_SHORT_TEXT),
                clean(root.getString("Value1"), MAX_SHORT_TEXT),
                clean(root.getString("Header2"), MAX_SHORT_TEXT),
                clean(root.getString("Value2"), MAX_SHORT_TEXT),
                clean(root.getString("Header3"), MAX_SHORT_TEXT),
                clean(root.getString("Value3"), MAX_SHORT_TEXT),
                cleanPreserveWhitespace(root.getString("Body"), MAX_BODY_TEXT),
                clean(root.getString("PhotoKey"), MAX_ASSET_KEY),
                Math.max(0, Math.min(4096, root.getInt("PhotoWidth"))),
                Math.max(0, Math.min(4096, root.getInt("PhotoHeight"))),
                clean(root.getString("Caption"), MAX_SHORT_TEXT)
        ));
    }

    public static State read(CompoundTag submittedRoot) {
        if (submittedRoot == null) return Template.BLANK_DOCUMENT.createState("");
        ItemStack temporary = new ItemStack(DocumentItems.getDocument());
        temporary.getOrCreateTag().put(ROOT_TAG, submittedRoot.copy());
        if (submittedRoot.contains("DocumentId")) {
            temporary.getOrCreateTag().putString(CODEX_ID_TAG,
                    submittedRoot.getString("DocumentId"));
        }
        return read(temporary);
    }

    public static void write(ItemStack stack, State state) {
        if (!isDedicatedItem(stack)) return;
        State safe = sanitize(state);
        String id = safe.documentId().isBlank()
                ? UUID.randomUUID().toString() : safe.documentId();
        CompoundTag root = new CompoundTag();
        root.putInt("Version", SCHEMA_VERSION);
        root.putString("DocumentId", id);
        root.putString("Template", safe.template().id());
        root.putString("Title", safe.title());
        root.putString("Category", safe.category());
        root.putString("Header1", safe.header1());
        root.putString("Value1", safe.value1());
        root.putString("Header2", safe.header2());
        root.putString("Value2", safe.value2());
        root.putString("Header3", safe.header3());
        root.putString("Value3", safe.value3());
        root.putString("Body", safe.body());
        root.putString("PhotoKey", safe.photoKey());
        root.putInt("PhotoWidth", safe.photoWidth());
        root.putInt("PhotoHeight", safe.photoHeight());
        root.putString("Caption", safe.caption());
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(ROOT_TAG, root);
        tag.putString(CODEX_ID_TAG, id);
        tag.putString("ScpDocumentCategory", safe.category());
        if (!safe.title().isBlank()) stack.setHoverName(Component.literal(safe.title()));
        else stack.resetHoverName();
    }

    public static CompoundTag toNetworkTag(State state) {
        ItemStack stack = new ItemStack(DocumentItems.getDocument());
        write(stack, state);
        CompoundTag root = stack.getTagElement(ROOT_TAG);
        return root == null ? new CompoundTag() : root.copy();
    }

    public static State sanitize(State state) {
        if (state == null) return Template.BLANK_DOCUMENT.createState("");
        Template template = state.template() == null ? Template.BLANK_DOCUMENT : state.template();
        return new State(
                clean(state.documentId(), MAX_SHORT_TEXT),
                template,
                clean(state.title(), MAX_SHORT_TEXT),
                clean(state.category(), MAX_SHORT_TEXT),
                clean(state.header1(), MAX_SHORT_TEXT),
                clean(state.value1(), MAX_SHORT_TEXT),
                clean(state.header2(), MAX_SHORT_TEXT),
                clean(state.value2(), MAX_SHORT_TEXT),
                clean(state.header3(), MAX_SHORT_TEXT),
                clean(state.value3(), MAX_SHORT_TEXT),
                cleanPreserveWhitespace(state.body(), MAX_BODY_TEXT),
                clean(state.photoKey(), MAX_ASSET_KEY),
                Math.max(0, Math.min(4096, state.photWidth())),
                Math.max(0, Math.min(4096, state.photoHeight())),
                clean(state.caption(), MAX_SHORT_TEXT)
        );
    }

    private static String clean(String value, int max) {
        String cleaned = value == null ? "" : value.replace('\u0000', ' ').trim();
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max);
    }

    private static String cleanPreserveWhitespace(String value, int max) {
        String cleaned = value == null ? "" : value.replace("\r\n", "\n")
                .replace('\r', '\n').replace("\u0000", "");
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max);
    }

    public enum Template {
        SCP_DOCUMENT("scp_document", "SCP Document"),
        FACILITY_DOCUMENT("facility_document", "Facility Document"),
        BLANK_DOCUMENT("blank_document", "Blank Document");

        private final String id;
        private final String displayName;

        Template(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }

        public State createState(String documentId) {
            return switch (this) {
                case SCP_DOCUMENT -> new State(documentId, this,
                        "SCP Document", "SCP Document",
                        "Clearance Level", "0",
                        "Item #", "SCP-000",
                        "Object Class", "Euclid",
                        "**Special Containment Procedures:j* Template Document\n\n"
                                + "**Description:** Template Document",
                        "", 0, 0, "");
                case FACILITY_DOCUMENT -> new State(documentId, this,
                        "Facility Document", "Facility Document",
                        "Clearance Level", "0",
                        "Document #", "000",
                        "Status", "Archived",
                        "**Summary:** Template Document",
                        "", 0, 0, "");
                case BLANK_DOCUMENT -> new State(documentId, this,
                        "Blank Document", "Documents",
                        "", "", "", "", "", "", "",
                        "", 0, 0, "");
            };
        }

        public static Template byId(String id) {
            String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
            for (Template template : values()) {
                if (template.id.equals(normalized)
                        || template.displayName.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return template;
                }
            }
            return BLANK_DOCUMENT;
        }
    }

    public record State(
            String documentId,
            Template template,
            String title,
            String category,
            String header1,
            String value1,
            String header2,
            String value2,
            String header3,
            String value3,
            String body,
            String photoKey,
            int photoWidth,
            int photoHeight,
            String caption) {
    }
}
