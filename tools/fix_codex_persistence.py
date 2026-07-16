from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def replace_all_exact(text: str, old: str, new: str, expected: int, label: str) -> str:
    count = text.count(old)
    if count != expected:
        raise RuntimeError(f"{label}: expected {expected} matches, found {count}")
    return text.replace(old, new)


# ---------------------------------------------------------------------------
# ConfigCenterClient: native paper defaults, immediate persistence, save+give.
# ---------------------------------------------------------------------------
path = Path("src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterClient.java")
text = path.read_text(encoding="utf-8")

text = replace_once(text,
'''    private static Screen rootParent;
    private static JsonObject files = new JsonObject();
    private static String homeNotice = "";
''',
'''    private static Screen rootParent;
    private static JsonObject files = new JsonObject();
    private static String homeNotice = "";
    private static boolean returnToCodexAfterSave;
    private static PendingCodexGive pendingCodexGive;
''', "ConfigCenterClient static pending state")

text = replace_once(text,
'''    public static void onSaveResult(ConfigCenterNetwork.SaveResult result) {
        if (result.success()) {
            try {
                JsonElement parsed = JsonParser.parseString(result.snapshot());
                files = parsed.isJsonObject() ? parsed.getAsJsonObject() : files;
            } catch (Exception ignored) {
            }
            homeNotice = result.message();
            Minecraft.getInstance().setScreen(new HomeScreen());
        } else {
            Minecraft.getInstance().setScreen(new MessageScreen(new HomeScreen(), "Configuration Not Saved", result.message(), false));
        }
    }
''',
'''    public static void onSaveResult(ConfigCenterNetwork.SaveResult result) {
        PendingCodexGive give = pendingCodexGive;
        boolean returnToCodex = returnToCodexAfterSave;
        pendingCodexGive = null;
        returnToCodexAfterSave = false;
        if (result.success()) {
            try {
                JsonElement parsed = JsonParser.parseString(result.snapshot());
                files = parsed.isJsonObject() ? parsed.getAsJsonObject() : files;
            } catch (Exception ignored) {
            }
            homeNotice = result.message();
            if (give != null) {
                CodexAssetClient.giveDocument(give.itemId(), give.codexId(), give.displayName());
            }
            if (returnToCodex) {
                HomeScreen home = new HomeScreen();
                InventoryHubScreen hub = new InventoryHubScreen(home);
                Minecraft.getInstance().setScreen(new CodexListScreen(hub, hub.working));
            } else {
                Minecraft.getInstance().setScreen(new HomeScreen());
            }
        } else {
            Minecraft.getInstance().setScreen(new MessageScreen(new HomeScreen(), "Configuration Not Saved", result.message(), false));
        }
    }
''', "ConfigCenterClient save result flow")

text = replace_once(text,
'''    private static void submit(Map<String, JsonObject> changes) {
        JsonObject payload = new JsonObject();
        for (Map.Entry<String, JsonObject> entry : changes.entrySet()) payload.add(entry.getKey(), entry.getValue());
        Minecraft.getInstance().setScreen(new MessageScreen(new HomeScreen(), "Saving Configuration",
                "Validating, writing backups, and reloading on the server...", true));
        ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.SaveRequest(GSON.toJson(payload)));
    }
''',
'''    private static void submit(Map<String, JsonObject> changes) {
        JsonObject payload = new JsonObject();
        for (Map.Entry<String, JsonObject> entry : changes.entrySet()) payload.add(entry.getKey(), entry.getValue());
        Minecraft.getInstance().setScreen(new MessageScreen(new HomeScreen(), "Saving Configuration",
                "Validating, writing backups, and reloading on the server...", true));
        ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.SaveRequest(GSON.toJson(payload)));
    }

    private static void submitCodex(JsonObject inventoryRoot, PendingCodexGive give) {
        returnToCodexAfterSave = true;
        pendingCodexGive = give;
        submit(Map.of(ConfigCenterService.INVENTORY, inventoryRoot));
    }

    private record PendingCodexGive(String itemId, String codexId, String displayName) {
    }
''', "ConfigCenterClient Codex submit helper")

old_add_init = '''            addRenderableWidget(Button.builder(Component.literal("+ Add Document"), b -> openItemPicker(id -> {
                JsonObject document = new JsonObject();
                document.addProperty("id", id);
                document.addProperty("category", "Documents");
                document.addProperty("name", "New Document");
                document.addProperty("image", "");
                document.addProperty("text", "");
                array(root, "codex_documents").add(document);
                Minecraft.getInstance().setScreen(new CodexDetailScreen(this, document));
            }, this)).bounds(x + w - 130, y, 118, 20).build());
'''
new_add_init = '''            addRenderableWidget(Button.builder(Component.literal("+ Paper Document"), b -> {
                JsonObject document = createDefaultCodexDocument();
                array(root, "codex_documents").add(document);
                Minecraft.getInstance().setScreen(new CodexDetailScreen(this, root, document));
            }).bounds(x + w - 130, y, 118, 20).build());
'''
text = replace_once(text, old_add_init, new_add_init, "CodexList first add button")

old_add_refresh = '''            addRenderableWidget(Button.builder(Component.literal("+ Add Document"), b -> openItemPicker(id -> {
                JsonObject document = new JsonObject();
                document.addProperty("id", id);
                document.addProperty("category", "Documents");
                document.addProperty("name", "New Document");
                document.addProperty("image", "");
                document.addProperty("text", "");
                array(root, "codex_documents").add(document);
                Minecraft.getInstance().setScreen(new CodexDetailScreen(this, document));
            }, this)).bounds(x + w - 130, top, 118, 20).build());
'''
new_add_refresh = '''            addRenderableWidget(Button.builder(Component.literal("+ Paper Document"), b -> {
                JsonObject document = createDefaultCodexDocument();
                array(root, "codex_documents").add(document);
                Minecraft.getInstance().setScreen(new CodexDetailScreen(this, root, document));
            }).bounds(x + w - 130, top, 118, 20).build());
'''
text = replace_once(text, old_add_refresh, new_add_refresh, "CodexList refresh add button")

text = replace_once(text,
'''                addRenderableWidget(Button.builder(Component.literal(compact(label, 62)), b -> Minecraft.getInstance().setScreen(new CodexDetailScreen(this, document)))
''',
'''                addRenderableWidget(Button.builder(Component.literal(compact(label, 62)), b -> Minecraft.getInstance().setScreen(new CodexDetailScreen(this, root, document)))
''', "CodexList existing document constructor")

text = replace_once(text,
'''            graphics.drawString(font, "Image and text resource paths are preserved exactly as entered.", x + 12, y + h - 17, MUTED, false);
''',
'''            graphics.drawString(font, "Documents are saved to the server when Save Document is pressed.", x + 12, y + h - 17, MUTED, false);
''', "CodexList footer")

text = replace_once(text,
'''    private static final class CodexDetailScreen extends ConfigScreen {
        private final JsonObject document;
        private final JsonObject edit;
''',
'''    private static JsonObject createDefaultCodexDocument() {
        JsonObject document = new JsonObject();
        document.addProperty("id", "minecraft:paper");
        document.addProperty("category", "Documents");
        document.addProperty("name", "New Document");
        document.addProperty("image", "");
        document.addProperty("text", "");
        return document;
    }

    private static final class CodexDetailScreen extends ConfigScreen {
        private final JsonObject root;
        private final JsonObject document;
        private final JsonObject edit;
''', "Codex default helper and root field")

text = replace_once(text,
'''        private CodexDetailScreen(Screen parent, JsonObject document) {
            super(parent, "Codex Document");
            this.document = document;
''',
'''        private CodexDetailScreen(Screen parent, JsonObject root, JsonObject document) {
            super(parent, "Codex Document");
            this.root = root;
            this.document = document;
''', "CodexDetail constructor")

text = replace_once(text,
'''            Button give = addRenderableWidget(Button.builder(
                    Component.literal("Give Test Item"), b -> giveTestItem())
''',
'''            Button give = addRenderableWidget(Button.builder(
                    Component.literal("Save & Give Test Item"), b -> giveTestItem())
''', "Codex give button label")

text = replace_once(text,
'''        private void giveTestItem() {
            sync();
            if (!uniqueMode) return;
            String id = string(edit, "id", "");
            try { new ResourceLocation(id); }
            catch (Exception ignored) { idBox.setTextColor(BAD); return; }
            CodexAssetClient.giveDocument(id, string(edit, "codex_id", ""),
                    string(edit, "name", "Document"));
        }

        private void save() {
            sync();
            String id = string(edit, "id", "");
            try { new ResourceLocation(id); }
            catch (Exception ignored) { idBox.setTextColor(BAD); return; }
            for (String key : new ArrayList<>(document.keySet())) document.remove(key);
            for (Map.Entry<String, JsonElement> entry : edit.entrySet()) {
                document.add(entry.getKey(), entry.getValue().deepCopy());
            }
            goBack();
        }
''',
'''        private void giveTestItem() {
            sync();
            if (!uniqueMode) return;
            String id = string(edit, "id", "");
            try { new ResourceLocation(id); }
            catch (Exception ignored) { idBox.setTextColor(BAD); return; }
            persistDocument();
            submitCodex(root, new PendingCodexGive(id,
                    string(edit, "codex_id", ""),
                    string(edit, "name", "Document")));
        }

        private void save() {
            sync();
            String id = string(edit, "id", "");
            try { new ResourceLocation(id); }
            catch (Exception ignored) { idBox.setTextColor(BAD); return; }
            persistDocument();
            submitCodex(root, null);
        }

        private void persistDocument() {
            for (String key : new ArrayList<>(document.keySet())) document.remove(key);
            for (Map.Entry<String, JsonElement> entry : edit.entrySet()) {
                document.add(entry.getKey(), entry.getValue().deepCopy());
            }
            if (parent instanceof CodexListScreen list) list.rebuildRows();
        }
''', "Codex save and give persistence")

path.write_text(text, encoding="utf-8")

# ---------------------------------------------------------------------------
# UnityConfigurationUiEvents: stop replacing the native Codex add button.
# ---------------------------------------------------------------------------
path = Path("src/main/java/net/mcreator/scpadditions/client/UnityConfigurationUiEvents.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''        if ("DrinkDetailScreen".equals(name)) installDrinkColorPicker(event, screen);
        if ("ItemRuleDetailScreen".equals(name)) installPlaceableCategory(event, screen);
        if ("CodexListScreen".equals(name)) installPaperDocumentDefault(event, screen);
''',
'''        if ("DrinkDetailScreen".equals(name)) installDrinkColorPicker(event, screen);
        if ("ItemRuleDetailScreen".equals(name)) installPlaceableCategory(event, screen);
''', "Disable reflective Codex add replacement")
path.write_text(text, encoding="utf-8")

# ---------------------------------------------------------------------------
# CodexAssetStorage: route generated test documents straight to Documents.
# ---------------------------------------------------------------------------
path = Path("src/main/java/net/mcreator/scpadditions/config/ui/CodexAssetStorage.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''import com.bl4ues.scpinventory.item.CodexDocumentDefinition;
''',
'''import com.bl4ues.scpinventory.capability.ScpInventoryProvider;
import com.bl4ues.scpinventory.item.CodexDocumentDefinition;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.network.ModNetwork;
''', "CodexAssetStorage inventory imports")
text = replace_once(text,
'''import net.mcreator.scpadditions.config.ui.ConfigCenterService;
''',
'''import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
''', "placeholder impossible") if False else text
# Insert modules import after ForgeRegistries import.
text = replace_once(text,
'''import net.minecraftforge.registries.ForgeRegistries;
''',
'''import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
''', "CodexAssetStorage module import")
text = replace_once(text,
'''        if (displayName != null && !displayName.isBlank()) {
            stack.setHoverName(Component.literal(displayName.trim()));
        }
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        return true;
''',
'''        if (displayName != null && !displayName.isBlank()) {
            stack.setHoverName(Component.literal(displayName.trim()));
        }
        if (ScpAdditionsModulesConfig.get().inventory.enabled
                && ScpItemClassifier.getCodexDocument(stack).isPresent()) {
            boolean[] stored = {false};
            player.getCapability(ScpInventoryProvider.INSTANCE).ifPresent(inventory -> {
                if (inventory.addDocumentItem(stack.copy())) {
                    stored[0] = true;
                    ModNetwork.syncTo(player, inventory);
                }
            });
            if (stored[0]) return true;
        }
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        return true;
''', "Route generated Codex item")
path.write_text(text, encoding="utf-8")

# ---------------------------------------------------------------------------
# CodexAssetClient: remember missing assets and expose loading state.
# ---------------------------------------------------------------------------
path = Path("src/main/java/net/mcreator/scpadditions/client/CodexAssetClient.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''    private static final Set<String> REQUESTED = new HashSet<>();
    private static final Map<String, PendingUpload> UPLOADS = new HashMap<>();
''',
'''    private static final Set<String> REQUESTED = new HashSet<>();
    private static final Set<String> MISSING = new HashSet<>();
    private static final Map<String, PendingUpload> UPLOADS = new HashMap<>();
''', "CodexAssetClient missing set")
text = replace_once(text,
'''        BYTES.put(cacheKey(result.kind(), result.key()), pending.data());
        REQUESTED.remove(cacheKey(result.kind(), result.key()));
''',
'''        String cache = cacheKey(result.kind(), result.key());
        BYTES.put(cache, pending.data());
        REQUESTED.remove(cache);
        MISSING.remove(cache);
''', "Codex upload cache state")
text = replace_all_exact(text,
'''        byte[] data = BYTES.get(cache);
        if (data == null) {
            request(''',
'''        byte[] data = BYTES.get(cache);
        if (data == null) {
            if (MISSING.contains(cache)) return Optional.empty();
            request(''', 2, "Codex asset missing guards")
text = replace_once(text,
'''        REQUESTED.remove(cache);
        if (data.data().length > 0) BYTES.put(cache, data.data());
    }
''',
'''        REQUESTED.remove(cache);
        if (data.data().length > 0) {
            BYTES.put(cache, data.data());
            MISSING.remove(cache);
        } else {
            MISSING.add(cache);
        }
    }

    public static boolean isPending(String kind, String key) {
        return key != null && !key.isBlank()
                && REQUESTED.contains(cacheKey(kind, key));
    }

    public static boolean isMissing(String kind, String key) {
        return key != null && !key.isBlank()
                && MISSING.contains(cacheKey(kind, key));
    }
''', "Codex asset state methods")
path.write_text(text, encoding="utf-8")

# ---------------------------------------------------------------------------
# CodexPanel: no fake page; explicit loading/missing/empty states.
# ---------------------------------------------------------------------------
path = Path("src/main/java/com/bl4ues/scpinventory/client/gui/components/CodexPanel.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''        renderDebugDocumentPage(g, document, definition, areaX, areaY, areaWidth, areaHeight);
    }
''',
'''        renderDocumentPlaceholder(g, definition, areaX, areaY, areaWidth, areaHeight);
    }

    private void renderDocumentPlaceholder(GuiGraphics g,
                                           CodexDocumentDefinition definition,
                                           int areaX, int areaY,
                                           int areaWidth, int areaHeight) {
        String worldKey = definition.getWorldImageKey();
        String message;
        if (!worldKey.isBlank()) {
            if (CodexAssetClient.isMissing("png", worldKey)) {
                message = "Document image unavailable";
            } else {
                message = "Loading document image...";
            }
        } else {
            message = "No document image attached";
        }
        g.fill(areaX, areaY, areaX + areaWidth, areaY + areaHeight, 0x3320262B);
        Component label = ScpFonts.roboto(message);
        int labelX = areaX + Math.max(4, (areaWidth - mc.font.width(label)) / 2);
        int labelY = areaY + Math.max(4, (areaHeight - 8) / 2);
        g.drawString(mc.font, label, labelX, labelY, TEXT_GRAY, false);
    }
''', "Codex image placeholder")
text = replace_once(text,
'''    private Optional<String> readText(CodexDocumentDefinition definition) {
        Optional<String> worldText = CodexAssetClient.getText(
                definition.getWorldTextKey());
        if (worldText.isPresent()) return worldText;
        ResourceLocation textLocation = definition.getTextLocation().orElse(null);
''',
'''    private Optional<String> readText(CodexDocumentDefinition definition) {
        String worldKey = definition.getWorldTextKey();
        Optional<String> worldText = CodexAssetClient.getText(worldKey);
        if (worldText.isPresent()) return worldText;
        if (!worldKey.isBlank()) {
            if (CodexAssetClient.isMissing("text", worldKey)) {
                return Optional.of("The saved document text could not be loaded.");
            }
            return Optional.of("Loading document text...");
        }
        ResourceLocation textLocation = definition.getTextLocation().orElse(null);
''', "Codex text loading state")
path.write_text(text, encoding="utf-8")

# ---------------------------------------------------------------------------
# Changelog: document the fixes.
# ---------------------------------------------------------------------------
path = Path("CHANGELOG.md")
text = path.read_text(encoding="utf-8")
anchor = "## Native configuration center\n"
if anchor not in text:
    raise RuntimeError("CHANGELOG: Native configuration center heading not found")
insert = (
    "- Fixed Codex documents created in the in-game editor not being persisted or listed after reopening the configuration center;\n"
    "- `Save Document` now validates, saves and reloads the inventory configuration immediately, while `Save & Give Test Item` waits for that reload before generating the unique item;\n"
    "- Unique generated Codex items are routed directly to the Documents area when the SCP Inventory module is enabled;\n"
    "- World-scoped Codex PNG/text assets now report loading, missing and empty states instead of displaying the old synthetic document page;\n"
)
text = text.replace(anchor, anchor + insert, 1)
path.write_text(text, encoding="utf-8")
