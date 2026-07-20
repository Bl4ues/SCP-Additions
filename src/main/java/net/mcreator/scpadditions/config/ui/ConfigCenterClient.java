package net.mcreator.scpadditions.config.ui;

import net.neoforged.fml.common.EventBusSubscriber;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.CodexAssetClient;
import net.mcreator.scpadditions.client.CodexImageDropScreen;
import net.mcreator.scpadditions.client.CodexTextEditorScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Client-only screens for SCP Additions' native configuration center. */
public final class ConfigCenterClient {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int PANEL = 0xEE111317;
    private static final int HEADER = 0xEE24282E;
    private static final int TEXT = 0xFFE8E8E8;
    private static final int MUTED = 0xFF9FA6AD;
    private static final int ACCENT = 0xFF88DDEE;
    private static final int GOOD = 0xFF79D58B;
    private static final int WARN = 0xFFFFC56D;
    private static final int BAD = 0xFFFF8B8B;

    private static Screen rootParent;
    private static JsonObject files = new JsonObject();
    private static String homeNotice = "";
    private static boolean returnToCodexAfterSave;
    private static PendingCodexGive pendingCodexGive;

    private ConfigCenterClient() {
    }

    @EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(ConfigCenterClient::openFromMods)));
        }
    }

    private static Screen openFromMods(Minecraft minecraft, Screen parent) {
        rootParent = parent;
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return new MessageScreen(parent, "SCP Additions Configuration",
                    "Join or open a world to edit server-owned SCP Additions settings.", false);
        }
        ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.OpenRequest());
        return new MessageScreen(parent, "SCP Additions Configuration", "Loading configuration from the server...", true);
    }

    public static void requestOpen(Screen parent) {
        rootParent = parent;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            minecraft.setScreen(new MessageScreen(parent, "SCP Additions Configuration",
                    "Join or open a world to edit server-owned SCP Additions settings.", false));
            return;
        }
        minecraft.setScreen(new MessageScreen(parent, "SCP Additions Configuration", "Loading configuration from the server...", true));
        ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.OpenRequest());
    }

    public static void openSnapshot(String payload) {
        try {
            JsonElement parsed = JsonParser.parseString(payload == null ? "{}" : payload);
            files = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            Minecraft.getInstance().setScreen(new HomeScreen());
        } catch (Exception exception) {
            Minecraft.getInstance().setScreen(new MessageScreen(rootParent, "Configuration Error",
                    "The server returned an unreadable configuration snapshot: " + readable(exception), false));
        }
    }

    public static void onSaveResult(ConfigCenterNetwork.SaveResult result) {
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

    private static void submit(Map<String, JsonObject> changes) {
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

    private static JsonObject file(String key, JsonObject fallback) {
        if (files.has(key) && files.get(key).isJsonObject()) return files.getAsJsonObject(key).deepCopy();
        return fallback.deepCopy();
    }

    private static JsonArray array(JsonObject root, String key) {
        if (!root.has(key) || !root.get(key).isJsonArray()) root.add(key, new JsonArray());
        return root.getAsJsonArray(key);
    }

    private static JsonObject object(JsonObject root, String key) {
        if (!root.has(key) || !root.get(key).isJsonObject()) root.add(key, new JsonObject());
        return root.getAsJsonObject(key);
    }

    private static String string(JsonObject root, String key, String fallback) {
        if (root == null || !root.has(key) || !root.get(key).isJsonPrimitive()) return fallback;
        try { return root.get(key).getAsString(); } catch (Exception ignored) { return fallback; }
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        if (root == null || !root.has(key) || !root.get(key).isJsonPrimitive()) return fallback;
        try { return root.get(key).getAsBoolean(); } catch (Exception ignored) { return fallback; }
    }

    private static int integer(JsonObject root, String key, int fallback) {
        if (root == null || !root.has(key) || !root.get(key).isJsonPrimitive()) return fallback;
        try { return root.get(key).getAsInt(); } catch (Exception ignored) { return fallback; }
    }

    private static double decimal(JsonObject root, String key, double fallback) {
        if (root == null || !root.has(key) || !root.get(key).isJsonPrimitive()) return fallback;
        try { return root.get(key).getAsDouble(); } catch (Exception ignored) { return fallback; }
    }

    private static String compact(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static String readable(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private static int left(int width, int panelWidth) {
        return Math.max(8, (width - panelWidth) / 2);
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height, String title, Font font) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.fill(x, y, x + width, y + 26, HEADER);
        graphics.drawString(font, title, x + 10, y + 9, TEXT, false);
    }

    private abstract static class ConfigScreen extends Screen {
        protected final Screen parent;
        protected final String screenTitle;

        protected ConfigScreen(Screen parent, String title) {
            super(Component.literal(title));
            this.parent = parent;
            this.screenTitle = title;
        }

        protected void goBack() {
            Minecraft.getInstance().setScreen(parent == null ? new HomeScreen() : parent);
        }

        @Override
        public void onClose() {
            goBack();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static final class MessageScreen extends ConfigScreen {
        private final String message;
        private final boolean passive;

        private MessageScreen(Screen parent, String title, String message, boolean passive) {
            super(parent, title);
            this.message = message;
            this.passive = passive;
        }

        @Override
        protected void init() {
            if (!passive) addRenderableWidget(Button.builder(Component.literal("Back"), button -> goBack())
                    .bounds(width / 2 - 50, height / 2 + 36, 100, 20).build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(430, width - 20);
            int x = left(width, w);
            int y = Math.max(12, height / 2 - 65);
            panel(graphics, x, y, w, 130, screenTitle, font);
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(message), w - 24);
            int lineY = y + 42;
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                graphics.drawString(font, line, x + 12, lineY, passive ? MUTED : BAD, false);
                lineY += 11;
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class HomeScreen extends ConfigScreen {
        private HomeScreen() {
            super(rootParent, "SCP Additions Configuration");
        }

        @Override
        protected void init() {
            int w = Math.min(420, width - 20);
            int x = left(width, w) + 14;
            int y = Math.max(10, (height - 300) / 2) + 44;
            int bw = w - 28;
            addRenderableWidget(Button.builder(Component.literal("General & Modules"), button -> Minecraft.getInstance().setScreen(new ModulesScreen(this)))
                    .bounds(x, y, bw, 24).build());
            y += 31;
            addRenderableWidget(Button.builder(Component.literal("Inventory, Equipment & Codex"), button -> Minecraft.getInstance().setScreen(new InventoryHubScreen(this)))
                    .bounds(x, y, bw, 24).build());
            y += 31;
            addRenderableWidget(Button.builder(Component.literal("Contextual Interactions"), button -> Minecraft.getInstance().setScreen(new ContextListScreen(this)))
                    .bounds(x, y, bw, 24).build());
            y += 31;
            addRenderableWidget(Button.builder(Component.literal("SCP-294 Drinks"), button -> Minecraft.getInstance().setScreen(new DrinkListScreen(this)))
                    .bounds(x, y, bw, 24).build());
            y += 31;
            addRenderableWidget(Button.builder(Component.literal("SCP-914 Recipes"), button -> Minecraft.getInstance().setScreen(new RecipeListScreen(this)))
                    .bounds(x, y, bw, 24).build());
            y += 43;
            addRenderableWidget(Button.builder(Component.literal("Reload Snapshot"), button -> requestOpen(rootParent))
                    .bounds(x, y, (bw - 6) / 2, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Done"), button -> Minecraft.getInstance().setScreen(rootParent))
                    .bounds(x + (bw - 6) / 2 + 6, y, (bw - 6) / 2, 20).build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(420, width - 20);
            int h = Math.min(310, height - 20);
            int x = left(width, w);
            int y = Math.max(10, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            graphics.drawString(font, "Server-authoritative JSON editors with validation and automatic .bak backups.",
                    x + 14, y + 30, MUTED, false);
            if (!homeNotice.isBlank()) {
                graphics.drawString(font, compact(homeNotice, 62), x + 14, y + h - 40, GOOD, false);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private record ToggleSpec(String group, String key, String label, String description, boolean fallback) {
    }

    private static final class ModulesScreen extends ConfigScreen {
        private static final List<ToggleSpec> SPECS = List.of(
                new ToggleSpec("inventory", "enabled", "SCP Inventory", "Enables the custom survival-horror inventory.", true),
                new ToggleSpec("inventory", "remember_ui_state", "Remember UI State", "Remembers the selected panel, document and scroll positions until leaving the world.", true),
                new ToggleSpec("interactions", "enabled", "Contextual Interactions", "Enables SCP Unity-style interaction prompts.", true),
                new ToggleSpec("interactions", "disable_in_creative", "Hide Prompts in Creative", "Disables custom prompts for Creative players.", false),
                new ToggleSpec("hud", "enabled", "Custom HUD", "Shows the SCP Additions health, stamina and blink presentation.", true),
                new ToggleSpec("vitals", "custom_health_enabled", "Custom Health", "Enables custom health behavior.", true),
                new ToggleSpec("vitals", "stamina_enabled", "Stamina", "Enables stamina drain and regeneration.", true),
                new ToggleSpec("vitals", "horror_movement_enabled", "Survival-Horror Movement", "Uses slower walking and committed sprinting.", true),
                new ToggleSpec("blink", "enabled", "Blink System", "Enables automatic and manual blinking.", true),
                new ToggleSpec("scp_173", "enabled", "SCP-173", "Enables SCP-173 behavior.", true),
                new ToggleSpec("scp_173", "natural_spawn_enabled", "SCP-173 Natural Spawning", "Allows the configurable natural spawn system.", true)
        );

        private final JsonObject working;
        private final List<Button> toggleButtons = new ArrayList<>();
        private int scroll;

        private ModulesScreen(Screen parent) {
            super(parent, "General & Modules");
            this.working = file(ConfigCenterService.MODULES, new JsonObject());
        }

        @Override
        protected void init() {
            toggleButtons.clear();
            int w = Math.min(560, width - 20);
            int x = left(width, w) + 16;
            int y = Math.max(8, (height - Math.min(380, height - 16)) / 2) + 44;
            int visible = Math.max(4, Math.min(8, (height - 118) / 34));
            int end = Math.min(SPECS.size(), scroll + visible);
            for (int i = scroll; i < end; i++) {
                ToggleSpec spec = SPECS.get(i);
                Button button = addRenderableWidget(Button.builder(Component.literal(toggleLabel(spec)), b -> {
                    JsonObject group = object(working, spec.group());
                    group.addProperty(spec.key(), !bool(group, spec.key(), spec.fallback()));
                    b.setMessage(Component.literal(toggleLabel(spec)));
                }).bounds(x, y + (i - scroll) * 34, w - 32, 22).build());
                toggleButtons.add(button);
            }
            int bottom = Math.min(height - 30, y + visible * 34 + 8);
            addRenderableWidget(Button.builder(Component.literal("Defaults"), b -> resetDefaults())
                    .bounds(x, bottom, 90, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Save & Reload"), b -> submit(Map.of(ConfigCenterService.MODULES, working)))
                    .bounds(x + w - 32 - 198, bottom, 108, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> goBack())
                    .bounds(x + w - 32 - 84, bottom, 84, 20).build());
        }

        private String toggleLabel(ToggleSpec spec) {
            return spec.label() + ": " + (bool(object(working, spec.group()), spec.key(), spec.fallback()) ? "ON" : "OFF");
        }

        private void resetDefaults() {
            for (ToggleSpec spec : SPECS) object(working, spec.group()).addProperty(spec.key(), spec.fallback());
            rebuild();
        }

        private void rebuild() {
            clearWidgets();
            init();
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int visible = Math.max(4, Math.min(8, (height - 118) / 34));
            int max = Math.max(0, SPECS.size() - visible);
            int next = Math.max(0, Math.min(max, scroll + (delta < 0 ? 1 : -1)));
            if (next != scroll) { scroll = next; rebuild(); return true; }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(560, width - 20);
            int h = Math.min(380, height - 16);
            int x = left(width, w);
            int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            int visible = Math.max(4, Math.min(8, (height - 118) / 34));
            int startY = y + 68;
            for (int i = scroll; i < Math.min(SPECS.size(), scroll + visible); i++) {
                graphics.drawString(font, compact(SPECS.get(i).description(), 76), x + 18, startY + (i - scroll) * 34, MUTED, false);
            }
            graphics.drawString(font, "Mouse wheel: scroll options", x + w - 160, y + 31, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class InventoryHubScreen extends ConfigScreen {
        private final JsonObject working;

        private InventoryHubScreen(Screen parent) {
            this(parent, file(ConfigCenterService.INVENTORY, new JsonObject()));
        }

        private InventoryHubScreen(Screen parent, JsonObject working) {
            super(parent, "Inventory, Equipment & Codex");
            this.working = working;
        }

        @Override
        protected void init() {
            int w = Math.min(430, width - 20);
            int x = left(width, w) + 14;
            int y = Math.max(10, (height - 300) / 2) + 44;
            int bw = w - 28;
            addRenderableWidget(Button.builder(Component.literal("Item Categories & Equipment Effects"),
                    b -> Minecraft.getInstance().setScreen(new ItemRulesScreen(this, working)))
                    .bounds(x, y, bw, 24).build());
            y += 32;
            addRenderableWidget(Button.builder(Component.literal("Hidden Status Effects"),
                    b -> Minecraft.getInstance().setScreen(new IdListScreen(this, working, "hidden_status_effects", "Hidden Status Effects", false)))
                    .bounds(x, y, bw, 24).build());
            y += 32;
            addRenderableWidget(Button.builder(Component.literal("SCP-173 Entity Targets"),
                    b -> Minecraft.getInstance().setScreen(new IdListScreen(this, working, "scp_173_targets", "SCP-173 Entity Targets", true)))
                    .bounds(x, y, bw, 24).build());
            y += 32;
            addRenderableWidget(Button.builder(Component.literal("Codex Documents"),
                    b -> Minecraft.getInstance().setScreen(new CodexListScreen(this, working)))
                    .bounds(x, y, bw, 24).build());
            y += 45;
            addRenderableWidget(Button.builder(Component.literal("Save All"), b -> submit(Map.of(ConfigCenterService.INVENTORY, working)))
                    .bounds(x, y, (bw - 6) / 2, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> goBack())
                    .bounds(x + (bw - 6) / 2 + 6, y, (bw - 6) / 2, 20).build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(430, width - 20);
            int h = Math.min(300, height - 20);
            int x = left(width, w);
            int y = Math.max(10, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            graphics.drawString(font, "Edits remain local until Save All is pressed.", x + 14, y + 31, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class ItemRulesScreen extends ConfigScreen {
        private static final List<String> TYPES = List.of("MISCELLANEOUS", "CONSUMABLE", "USABLE", "PLACEABLE", "HARMFUL", "KEY", "COIN", "AMMO", "HEAD", "CHEST", "LEGS", "FEET", "ACCESSORY", "ACCESSORYHAND", "WEAPON");
        private final JsonObject root;
        private final EditBox search;
        private int scroll;
        private List<JsonObject> filtered = List.of();

        private ItemRulesScreen(Screen parent, JsonObject root) {
            super(parent, "Item Categories & Equipment Effects");
            this.root = root;
            JsonArray rules = array(root, "item_rules");
            for (int i = rules.size() - 1; i >= 0; i--) {
                JsonElement element = rules.get(i);
                if (!element.isJsonObject()) continue;
                String configured = string(element.getAsJsonObject(), "type", "");
                if ("CODEX".equalsIgnoreCase(configured)
                        || "DOCUMENT".equalsIgnoreCase(configured)
                        || "DOC".equalsIgnoreCase(configured)) {
                    rules.remove(i);
                }
            }
            this.search = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.literal("Search"));
        }

        @Override
        protected void init() {
            int w = Math.min(650, width - 16);
            int x = left(width, w) + 12;
            int y = Math.max(8, (height - Math.min(400, height - 16)) / 2) + 38;
            search.setX(x);
            search.setY(y);
            search.setWidth(w - 142);
            search.setMaxLength(128);
            search.setHint(Component.literal("Search item id or category"));
            search.setResponder(value -> { scroll = 0; rebuildRows(); });
            addRenderableWidget(search);
            addRenderableWidget(Button.builder(Component.literal("+ Add Item"), b -> openItemPicker(id -> {
                JsonObject rule = new JsonObject();
                rule.addProperty("id", id);
                rule.addProperty("type", "MISCELLANEOUS");
                array(root, "item_rules").add(rule);
                Minecraft.getInstance().setScreen(new ItemRuleDetailScreen(this, root, rule));
            }, this)).bounds(x + w - 130, y, 118, 20).build());
            rebuildRows();
        }

        private void rebuildRows() {
            String needle = search.getValue().trim().toLowerCase(Locale.ROOT);
            List<JsonObject> rows = new ArrayList<>();
            for (JsonElement element : array(root, "item_rules")) {
                if (!element.isJsonObject()) continue;
                JsonObject rule = element.getAsJsonObject();
                String id = string(rule, "id", "");
                String type = string(rule, "type", "MISCELLANEOUS");
                if (needle.isEmpty() || id.toLowerCase(Locale.ROOT).contains(needle) || type.toLowerCase(Locale.ROOT).contains(needle)) rows.add(rule);
            }
            rows.sort(Comparator.comparing(rule -> string(rule, "id", "")));
            filtered = rows;
            rebuildWidgetsOnly();
        }

        private void rebuildWidgetsOnly() {
            clearWidgets();
            addRenderableWidget(search);
            int w = Math.min(650, width - 16);
            int x = left(width, w) + 12;
            int y = Math.max(8, (height - Math.min(400, height - 16)) / 2) + 38;
            addRenderableWidget(Button.builder(Component.literal("+ Add Item"), b -> openItemPicker(id -> {
                JsonObject rule = new JsonObject();
                rule.addProperty("id", id);
                rule.addProperty("type", "MISCELLANEOUS");
                array(root, "item_rules").add(rule);
                Minecraft.getInstance().setScreen(new ItemRuleDetailScreen(this, root, rule));
            }, this)).bounds(x + w - 130, y, 118, 20).build());
            int visible = Math.max(4, Math.min(10, (height - 128) / 24));
            int maxScroll = Math.max(0, filtered.size() - visible);
            scroll = Math.min(scroll, maxScroll);
            int listY = y + 30;
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
                JsonObject rule = filtered.get(i);
                String id = string(rule, "id", "unknown");
                String type = string(rule, "type", "MISCELLANEOUS");
                int row = i - scroll;
                addRenderableWidget(Button.builder(Component.literal(compact(id, 45) + "  [" + type + "]"),
                        b -> Minecraft.getInstance().setScreen(new ItemRuleDetailScreen(this, root, rule)))
                        .bounds(x, listY + row * 24, w - 78, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                    removeIdentity(array(root, "item_rules"), rule);
                    removeItemEffects(root, id);
                    rebuildRows();
                }).bounds(x + w - 70, listY + row * 24, 58, 20).build());
            }
            int bottom = Math.min(height - 28, listY + visible * 24 + 7);
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> goBack()).bounds(x + w - 92, bottom, 80, 20).build());
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int visible = Math.max(4, Math.min(10, (height - 128) / 24));
            int max = Math.max(0, filtered.size() - visible);
            int next = Math.max(0, Math.min(max, scroll + (delta < 0 ? 1 : -1)));
            if (next != scroll) { scroll = next; rebuildWidgetsOnly(); return true; }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(650, width - 16);
            int h = Math.min(400, height - 16);
            int x = left(width, w);
            int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            graphics.drawString(font, filtered.size() + " matching item rule(s)", x + 12, y + h - 18, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class ItemRuleDetailScreen extends ConfigScreen {
        private final JsonObject root;
        private final JsonObject original;
        private final String id;
        private String type;
        private boolean noStamina;
        private boolean protectedEyes;
        private Button typeButton;
        private Button staminaButton;
        private Button eyesButton;

        private ItemRuleDetailScreen(Screen parent, JsonObject root, JsonObject rule) {
            super(parent, "Item Rule");
            this.root = root;
            this.original = rule;
            this.id = string(rule, "id", "minecraft:air");
            this.type = string(rule, "type", "MISCELLANEOUS").toUpperCase(Locale.ROOT);
            this.noStamina = hasItemEffect(root, id, "NO_STAMINA");
            this.protectedEyes = hasItemEffect(root, id, "PROTECTED_EYES");
        }

        @Override
        protected void init() {
            int w = Math.min(430, width - 20);
            int x = left(width, w) + 14;
            int y = Math.max(10, (height - 280) / 2) + 64;
            typeButton = addRenderableWidget(Button.builder(Component.literal("Category: " + type), b -> {
                int index = ItemRulesScreen.TYPES.indexOf(type);
                type = ItemRulesScreen.TYPES.get((Math.max(0, index) + 1) % ItemRulesScreen.TYPES.size());
                b.setMessage(Component.literal("Category: " + type));
            }).bounds(x, y, w - 28, 22).build());
            y += 34;
            staminaButton = addRenderableWidget(Button.builder(Component.literal("NO_STAMINA: " + onOff(noStamina)), b -> {
                noStamina = !noStamina;
                b.setMessage(Component.literal("NO_STAMINA: " + onOff(noStamina)));
            }).bounds(x, y, w - 28, 22).build());
            y += 30;
            eyesButton = addRenderableWidget(Button.builder(Component.literal("PROTECTED_EYES: " + onOff(protectedEyes)), b -> {
                protectedEyes = !protectedEyes;
                b.setMessage(Component.literal("PROTECTED_EYES: " + onOff(protectedEyes)));
            }).bounds(x, y, w - 28, 22).build());
            y += 50;
            addRenderableWidget(Button.builder(Component.literal("Save Rule"), b -> save())
                    .bounds(x, y, (w - 34) / 2, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> goBack())
                    .bounds(x + (w - 34) / 2 + 6, y, (w - 34) / 2, 20).build());
        }

        private void save() {
            original.addProperty("id", id);
            original.addProperty("type", type);
            setItemEffect(root, id, "NO_STAMINA", noStamina);
            setItemEffect(root, id, "PROTECTED_EYES", protectedEyes);
            goBack();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(430, width - 20);
            int x = left(width, w);
            int y = Math.max(10, (height - 280) / 2);
            panel(graphics, x, y, w, 280, screenTitle, font);
            graphics.drawString(font, compact(id, 58), x + 14, y + 34, ACCENT, false);
            graphics.drawString(font, "Category controls where the item is stored.", x + 14, y + 50, MUTED, false);
            graphics.drawString(font, "Equipment effects are evaluated while held, worn, or equipped.", x + 14, y + 166, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static boolean hasItemEffect(JsonObject root, String id, String effect) {
        for (JsonElement element : array(root, "item_effects")) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (!id.equals(string(object, "id", ""))) continue;
            if (object.has("effects") && object.get("effects").isJsonArray()) {
                for (JsonElement value : object.getAsJsonArray("effects")) {
                    if (value.isJsonPrimitive() && effect.equalsIgnoreCase(value.getAsString())) return true;
                }
            }
        }
        return false;
    }

    private static void setItemEffect(JsonObject root, String id, String effect, boolean enabled) {
        JsonArray effects = array(root, "item_effects");
        JsonObject target = null;
        for (JsonElement element : effects) {
            if (element.isJsonObject() && id.equals(string(element.getAsJsonObject(), "id", ""))) {
                target = element.getAsJsonObject();
                break;
            }
        }
        if (target == null && enabled) {
            target = new JsonObject();
            target.addProperty("id", id);
            target.add("effects", new JsonArray());
            effects.add(target);
        }
        if (target == null) return;
        JsonArray list = array(target, "effects");
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).isJsonPrimitive() && effect.equalsIgnoreCase(list.get(i).getAsString())) list.remove(i);
        }
        if (enabled) list.add(effect);
        if (list.size() == 0) removeIdentity(effects, target);
    }

    private static void removeItemEffects(JsonObject root, String id) {
        JsonArray effects = array(root, "item_effects");
        for (int i = effects.size() - 1; i >= 0; i--) {
            if (effects.get(i).isJsonObject() && id.equals(string(effects.get(i).getAsJsonObject(), "id", ""))) effects.remove(i);
        }
    }

    private static void removeIdentity(JsonArray array, JsonObject object) {
        for (int i = array.size() - 1; i >= 0; i--) if (array.get(i) == object) array.remove(i);
    }

    private static final class IdListScreen extends ConfigScreen {
        private final JsonObject root;
        private final String key;
        private final boolean allowTag;
        private final EditBox valueBox;
        private final EditBox searchBox;
        private List<String> filtered = List.of();
        private int scroll;

        private IdListScreen(Screen parent, JsonObject root, String key, String title, boolean allowTag) {
            super(parent, title);
            this.root = root;
            this.key = key;
            this.allowTag = allowTag;
            this.valueBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.literal("Resource ID"));
            this.searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.literal("Search"));
        }

        @Override
        protected void init() {
            int w = Math.min(600, width - 18);
            int x = left(width, w) + 12;
            int y = Math.max(8, (height - Math.min(380, height - 16)) / 2) + 38;
            searchBox.setX(x);
            searchBox.setY(y);
            searchBox.setWidth(w - 24);
            searchBox.setHint(Component.literal("Search configured IDs"));
            searchBox.setMaxLength(256);
            searchBox.setResponder(value -> { scroll = 0; rebuildRows(); });
            addRenderableWidget(searchBox);
            y += 28;
            valueBox.setX(x);
            valueBox.setY(y);
            valueBox.setWidth(w - 118);
            valueBox.setHint(Component.literal(allowTag ? "namespace:id or #namespace:tag" : "namespace:id"));
            valueBox.setMaxLength(256);
            addRenderableWidget(valueBox);
            addRenderableWidget(Button.builder(Component.literal("Add"), b -> addValue())
                    .bounds(x + w - 106, y, 94, 20).build());
            rebuildRows();
        }

        private void addValue() {
            String value = valueBox.getValue().trim();
            if (value.isEmpty()) return;
            String check = allowTag && value.startsWith("#") ? value.substring(1) : value;
            try { ResourceLocation.parse(check); }
            catch (Exception ignored) { valueBox.setTextColor(BAD); return; }
            JsonArray values = array(root, key);
            for (JsonElement element : values) if (element.isJsonPrimitive() && value.equals(element.getAsString())) return;
            values.add(value);
            valueBox.setValue("");
            valueBox.setTextColor(TEXT);
            rebuildRows();
        }

        private void rebuildRows() {
            List<String> values = new ArrayList<>();
            String needle = searchBox.getValue().toLowerCase(Locale.ROOT).trim();
            for (JsonElement element : array(root, key)) {
                if (!element.isJsonPrimitive()) continue;
                String value = element.getAsString();
                if (needle.isEmpty() || value.toLowerCase(Locale.ROOT).contains(needle)) values.add(value);
            }
            values.sort(String::compareToIgnoreCase);
            filtered = values;
            refreshRows();
        }

        private void refreshRows() {
            clearWidgets();
            int w = Math.min(600, width - 18);
            int x = left(width, w) + 12;
            int top = Math.max(8, (height - Math.min(380, height - 16)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(top); searchBox.setWidth(w - 24); addRenderableWidget(searchBox);
            int y = top + 28;
            valueBox.setX(x); valueBox.setY(y); valueBox.setWidth(w - 118); addRenderableWidget(valueBox);
            addRenderableWidget(Button.builder(Component.literal("Add"), b -> addValue()).bounds(x + w - 106, y, 94, 20).build());
            int listY = y + 28;
            int visible = Math.max(4, Math.min(9, (height - 146) / 24));
            scroll = Math.min(scroll, Math.max(0, filtered.size() - visible));
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
                String value = filtered.get(i);
                int row = i - scroll;
                addRenderableWidget(Button.builder(Component.literal(value), b -> valueBox.setValue(value))
                        .bounds(x, listY + row * 24, w - 88, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Remove"), b -> {
                    JsonArray values = array(root, key);
                    for (int index = values.size() - 1; index >= 0; index--) {
                        if (values.get(index).isJsonPrimitive() && value.equals(values.get(index).getAsString())) values.remove(index);
                    }
                    rebuildRows();
                }).bounds(x + w - 80, listY + row * 24, 68, 20).build());
            }
            int bottom = Math.min(height - 28, listY + visible * 24 + 6);
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> goBack()).bounds(x + w - 92, bottom, 80, 20).build());
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int visible = Math.max(4, Math.min(9, (height - 146) / 24));
            int next = Math.max(0, Math.min(Math.max(0, filtered.size() - visible), scroll + (delta < 0 ? 1 : -1)));
            if (next != scroll) { scroll = next; refreshRows(); return true; }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(600, width - 18);
            int h = Math.min(380, height - 16);
            int x = left(width, w);
            int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            graphics.drawString(font, array(root, key).size() + " configured value(s)", x + 12, y + h - 17, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class CodexListScreen extends ConfigScreen {
        private final JsonObject root;
        private final EditBox searchBox;
        private List<JsonObject> filtered = List.of();
        private int scroll;

        private CodexListScreen(Screen parent, JsonObject root) {
            super(parent, "Codex Documents");
            this.root = root;
            this.searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.literal("Search"));
        }

        @Override
        protected void init() {
            int w = Math.min(650, width - 18);
            int x = left(width, w) + 12;
            int y = Math.max(8, (height - Math.min(390, height - 16)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(y); searchBox.setWidth(w - 142);
            searchBox.setHint(Component.literal("Search item, name, or category"));
            searchBox.setMaxLength(256);
            searchBox.setResponder(value -> { scroll = 0; rebuildRows(); });
            addRenderableWidget(searchBox);
            addRenderableWidget(Button.builder(Component.literal("+ Paper Document"), b -> {
                JsonObject document = createDefaultCodexDocument();
                array(root, "codex_documents").add(document);
                Minecraft.getInstance().setScreen(new CodexDetailScreen(this, root, document));
            }).bounds(x + w - 130, y, 118, 20).build());
            rebuildRows();
        }

        private void rebuildRows() {
            String needle = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
            List<JsonObject> rows = new ArrayList<>();
            for (JsonElement element : array(root, "codex_documents")) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String haystack = string(object, "id", "") + " " + string(object, "name", "") + " " + string(object, "category", "");
                if (needle.isEmpty() || haystack.toLowerCase(Locale.ROOT).contains(needle)) rows.add(object);
            }
            rows.sort(Comparator.comparing(object -> string(object, "name", string(object, "id", "")), String.CASE_INSENSITIVE_ORDER));
            filtered = rows;
            refreshRows();
        }

        private void refreshRows() {
            clearWidgets();
            int w = Math.min(650, width - 18);
            int x = left(width, w) + 12;
            int top = Math.max(8, (height - Math.min(390, height - 16)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(top); searchBox.setWidth(w - 142); addRenderableWidget(searchBox);
            addRenderableWidget(Button.builder(Component.literal("+ Paper Document"), b -> {
                JsonObject document = createDefaultCodexDocument();
                array(root, "codex_documents").add(document);
                Minecraft.getInstance().setScreen(new CodexDetailScreen(this, root, document));
            }).bounds(x + w - 130, top, 118, 20).build());
            int listY = top + 30;
            int visible = Math.max(4, Math.min(10, (height - 128) / 24));
            scroll = Math.min(scroll, Math.max(0, filtered.size() - visible));
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
                JsonObject document = filtered.get(i);
                int row = i - scroll;
                String label = string(document, "name", "Unnamed") + "  [" + string(document, "category", "Documents") + "]";
                addRenderableWidget(Button.builder(Component.literal(compact(label, 62)), b -> Minecraft.getInstance().setScreen(new CodexDetailScreen(this, root, document)))
                        .bounds(x, listY + row * 24, w - 82, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> { removeIdentity(array(root, "codex_documents"), document); rebuildRows(); })
                        .bounds(x + w - 74, listY + row * 24, 62, 20).build());
            }
            int bottom = Math.min(height - 28, listY + visible * 24 + 6);
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> goBack()).bounds(x + w - 92, bottom, 80, 20).build());
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int visible = Math.max(4, Math.min(10, (height - 128) / 24));
            int next = Math.max(0, Math.min(Math.max(0, filtered.size() - visible), scroll + (delta < 0 ? 1 : -1)));
            if (next != scroll) { scroll = next; refreshRows(); return true; }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(650, width - 18);
            int h = Math.min(390, height - 16);
            int x = left(width, w);
            int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            graphics.drawString(font, "Documents are saved to the server when Save Document is pressed.", x + 12, y + h - 17, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static JsonObject createDefaultCodexDocument() {
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
        private EditBox idBox;
        private EditBox categoryBox;
        private EditBox nameBox;
        private EditBox imageBox;
        private EditBox textBox;
        private EditBox imageWidthBox;
        private EditBox imageHeightBox;
        private EditBox nbtKeyBox;
        private EditBox nbtValueBox;
        private boolean advanced;
        private boolean uniqueMode;

        private CodexDetailScreen(Screen parent, JsonObject root, JsonObject document) {
            super(parent, "Codex Document");
            this.root = root;
            this.document = document;
            this.edit = document.deepCopy();
            this.advanced = edit.has("image_width") || edit.has("image_height")
                    || edit.has("nbt_key") || edit.has("nbt_value");
            this.uniqueMode = "unique".equalsIgnoreCase(
                    string(edit, "match_mode", "item"));
        }

        @Override
        protected void init() {
            int w = Math.min(700, width - 16);
            int x = left(width, w) + 14;
            int y = Math.max(8, (height - Math.min(470, height - 16)) / 2) + 47;
            int fieldW = w - 28;
            idBox = field(x, y, fieldW, "Item ID", string(edit, "id", "")); y += 31;
            categoryBox = field(x, y, fieldW, "Category",
                    string(edit, "category", "Documents")); y += 31;
            nameBox = field(x, y, fieldW, "Display name",
                    string(edit, "name", "")); y += 31;

            addRenderableWidget(Button.builder(Component.literal(uniqueMode
                            ? "Match Mode: Unique Generated Item"
                            : "Match Mode: Any Matching Item"), b -> {
                        sync();
                        uniqueMode = !uniqueMode;
                        if (uniqueMode && string(edit, "codex_id", "").isBlank()) {
                            edit.addProperty("codex_id", java.util.UUID.randomUUID().toString());
                        }
                        rebuild();
                    }).bounds(x, y, fieldW, 20).build());
            y += 27;

            int resourceW = fieldW - 124;
            imageBox = field(x, y, resourceW, "Packaged image resource",
                    string(edit, "image", ""));
            addRenderableWidget(Button.builder(Component.literal(
                            edit.has("world_image") ? "Replace PNG" : "Import PNG"),
                    b -> openImageImporter())
                    .bounds(x + resourceW + 6, y, 118, 20).build());
            y += 31;
            textBox = field(x, y, resourceW, "Packaged UTF-8 text resource",
                    string(edit, "text", ""));
            addRenderableWidget(Button.builder(Component.literal(
                            edit.has("world_text") ? "Edit Text" : "Write Text"),
                    b -> openTextEditor())
                    .bounds(x + resourceW + 6, y, 118, 20).build());
            y += 32;

            addRenderableWidget(Button.builder(Component.literal(
                            (advanced ? "▼" : "▶")
                                    + " Additional conditions and image sizing"), b -> {
                        sync(); advanced = !advanced; rebuild();
                    }).bounds(x, y, fieldW, 20).build());
            y += 26;
            if (advanced) {
                int half = (fieldW - 6) / 2;
                imageWidthBox = field(x, y, half, "Image width",
                        string(edit, "image_width", ""));
                imageHeightBox = field(x + half + 6, y, half, "Image height",
                        string(edit, "image_height", "")); y += 31;
                nbtKeyBox = field(x, y, half, "NBT key",
                        string(edit, "nbt_key", ""));
                nbtValueBox = field(x + half + 6, y, half, "NBT value",
                        string(edit, "nbt_value", "")); y += 33;
            }

            int buttonY = Math.min(height - 30, y + 4);
            int third = (fieldW - 12) / 3;
            addRenderableWidget(Button.builder(Component.literal("Save Document"),
                    b -> save()).bounds(x, buttonY, third, 20).build());
            Button give = addRenderableWidget(Button.builder(
                    Component.literal("Save & Give Test Item"), b -> giveTestItem())
                    .bounds(x + third + 6, buttonY, third, 20).build());
            give.active = uniqueMode;
            addRenderableWidget(Button.builder(Component.literal("Cancel"),
                    b -> goBack()).bounds(x + (third + 6) * 2,
                    buttonY, third, 20).build());
        }

        private EditBox field(int x, int y, int width, String hint, String value) {
            EditBox box = new EditBox(font, x, y, width, 20, Component.literal(hint));
            box.setHint(Component.literal(hint));
            box.setMaxLength(2048);
            box.setValue(value == null ? "" : value);
            addRenderableWidget(box);
            return box;
        }

        private void sync() {
            if (idBox == null) return;
            setOrRemove(edit, "id", idBox.getValue());
            setOrRemove(edit, "category", categoryBox.getValue());
            setOrRemove(edit, "name", nameBox.getValue());
            setOrRemove(edit, "image", imageBox.getValue());
            setOrRemove(edit, "text", textBox.getValue());
            edit.addProperty("match_mode", uniqueMode ? "unique" : "item");
            if (uniqueMode && string(edit, "codex_id", "").isBlank()) {
                edit.addProperty("codex_id", java.util.UUID.randomUUID().toString());
            }
            if (!uniqueMode) edit.remove("codex_id");
            if (advanced && imageWidthBox != null) {
                setNumberOrRemove(edit, "image_width", imageWidthBox.getValue());
                setNumberOrRemove(edit, "image_height", imageHeightBox.getValue());
                setOrRemove(edit, "nbt_key", nbtKeyBox.getValue());
                setOrRemove(edit, "nbt_value", nbtValueBox.getValue());
            }
        }

        private void openImageImporter() {
            sync();
            Minecraft.getInstance().setScreen(new CodexImageDropScreen(this,
                    edit.has("world_image"), imported -> {
                        edit.addProperty("world_image", imported.key());
                        edit.addProperty("image_width", imported.width());
                        edit.addProperty("image_height", imported.height());
                        Minecraft.getInstance().setScreen(this);
                        rebuild();
                    }, () -> {
                        edit.remove("world_image");
                        Minecraft.getInstance().setScreen(this);
                        rebuild();
                    }));
        }

        private void openTextEditor() {
            sync();
            Minecraft.getInstance().setScreen(new CodexTextEditorScreen(this,
                    string(edit, "world_text", ""), key -> {
                        if (key == null || key.isBlank()) edit.remove("world_text");
                        else edit.addProperty("world_text", key);
                        Minecraft.getInstance().setScreen(this);
                        rebuild();
                    }));
        }

        private void giveTestItem() {
            sync();
            if (!uniqueMode) return;
            String id = string(edit, "id", "");
            try { ResourceLocation.parse(id); }
            catch (Exception ignored) { idBox.setTextColor(BAD); return; }
            persistDocument();
            submitCodex(root, new PendingCodexGive(id,
                    string(edit, "codex_id", ""),
                    string(edit, "name", "Document")));
        }

        private void save() {
            sync();
            String id = string(edit, "id", "");
            try { ResourceLocation.parse(id); }
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

        private void rebuild() { clearWidgets(); init(); }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                           float partialTick) {
            renderBackground(graphics);
            int w = Math.min(700, width - 16);
            int h = Math.min(470, height - 16);
            int x = left(width, w);
            int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            String imageState = edit.has("world_image")
                    ? "World image attached" : "No world image";
            String textState = edit.has("world_text")
                    ? "world text attached" : "no world text";
            graphics.drawString(font, imageState + " · " + textState
                            + " · packaged resources remain optional fallbacks.",
                    x + 14, y + 31, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static void setOrRemove(JsonObject object, String key, String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty()) object.remove(key); else object.addProperty(key, cleaned);
    }

    private static void setNumberOrRemove(JsonObject object, String key, String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty()) { object.remove(key); return; }
        try { object.addProperty(key, Integer.parseInt(cleaned)); }
        catch (NumberFormatException ignored) { object.addProperty(key, cleaned); }
    }

    private static void openItemPicker(Consumer<String> callback, Screen parent) {
        Minecraft.getInstance().setScreen(new ItemPickerScreen(parent, callback));
    }

    private static final class ItemPickerScreen extends ConfigScreen {
        private final Consumer<String> callback;
        private final EditBox searchBox;
        private final List<ResourceLocation> allItems;
        private List<ResourceLocation> filtered;
        private int scroll;

        private ItemPickerScreen(Screen parent, Consumer<String> callback) {
            super(parent, "Choose an Item");
            this.callback = callback;
            this.searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.literal("Search items"));
            this.allItems = BuiltInRegistries.ITEM.getKeys().stream()
                    .filter(id -> BuiltInRegistries.ITEM.getValue(id) != Items.AIR)
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .toList();
            this.filtered = allItems;
        }

        @Override
        protected void init() {
            int w = Math.min(680, width - 16);
            int x = left(width, w) + 12;
            int y = Math.max(8, (height - Math.min(410, height - 16)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(y); searchBox.setWidth(w - 24);
            searchBox.setHint(Component.literal("Search by item name, namespace, or registry path"));
            searchBox.setMaxLength(256);
            searchBox.setResponder(value -> { scroll = 0; filter(); });
            addRenderableWidget(searchBox);
            rebuildRows();
        }

        private void filter() {
            String needle = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
            if (needle.isEmpty()) filtered = allItems;
            else {
                List<ResourceLocation> values = new ArrayList<>();
                for (ResourceLocation id : allItems) {
                    Item item = BuiltInRegistries.ITEM.getValue(id);
                    String name = item == null ? "" : new ItemStack(item).getHoverName().getString();
                    if (id.toString().toLowerCase(Locale.ROOT).contains(needle) || name.toLowerCase(Locale.ROOT).contains(needle)) values.add(id);
                }
                filtered = values;
            }
            rebuildRows();
        }

        private void rebuildRows() {
            clearWidgets();
            int w = Math.min(680, width - 16);
            int x = left(width, w) + 12;
            int top = Math.max(8, (height - Math.min(410, height - 16)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(top); searchBox.setWidth(w - 24); addRenderableWidget(searchBox);
            int listY = top + 30;
            int visible = Math.max(5, Math.min(12, (height - 112) / 24));
            scroll = Math.min(scroll, Math.max(0, filtered.size() - visible));
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
                ResourceLocation id = filtered.get(i);
                Item item = BuiltInRegistries.ITEM.getValue(id);
                String name = item == null ? id.toString() : new ItemStack(item).getHoverName().getString();
                int row = i - scroll;
                addRenderableWidget(Button.builder(Component.literal("    " + compact(name, 42) + "  —  " + compact(id.toString(), 38)), b -> {
                    callback.accept(id.toString());
                }).bounds(x, listY + row * 24, w - 24, 20).build());
            }
            int bottom = Math.min(height - 28, listY + visible * 24 + 5);
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> goBack()).bounds(x + w - 104, bottom, 80, 20).build());
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int visible = Math.max(5, Math.min(12, (height - 112) / 24));
            int next = Math.max(0, Math.min(Math.max(0, filtered.size() - visible), scroll + (delta < 0 ? 1 : -1)));
            if (next != scroll) { scroll = next; rebuildRows(); return true; }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(680, width - 16);
            int h = Math.min(410, height - 16);
            int x = left(width, w);
            int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            int listY = y + 68;
            int visible = Math.max(5, Math.min(12, (height - 112) / 24));
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
                Item item = BuiltInRegistries.ITEM.getValue(filtered.get(i));
                if (item != null) graphics.renderItem(new ItemStack(item), x + 18, listY + (i - scroll) * 24 + 2);
            }
            graphics.drawString(font, filtered.size() + " matching item(s)", x + 12, y + h - 17, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class ContextListScreen extends ConfigScreen {
        private final JsonObject root;
        private final EditBox searchBox;
        private List<JsonObject> filtered = List.of();
        private int scroll;

        private ContextListScreen(Screen parent) {
            super(parent, "Contextual Interactions");
            this.root = file(ConfigCenterService.CONTEXT, new JsonObject());
            this.searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.literal("Search"));
        }

        @Override
        protected void init() {
            int w = Math.min(700, width - 16);
            int x = left(width, w) + 12;
            int y = Math.max(8, (height - Math.min(410, height - 16)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(y); searchBox.setWidth(w - 220);
            searchBox.setHint(Component.literal("Search block/entity id or prompt text"));
            searchBox.setMaxLength(256);
            searchBox.setResponder(value -> { scroll = 0; rebuildRows(); });
            addRenderableWidget(searchBox);
            addRenderableWidget(Button.builder(Component.literal("+ Block"), b -> addRule("block"))
                    .bounds(x + w - 208, y, 94, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+ Entity"), b -> addRule("entity"))
                    .bounds(x + w - 108, y, 96, 20).build());
            rebuildRows();
        }

        private void addRule(String type) {
            JsonObject rule = new JsonObject();
            rule.addProperty("type", type);
            rule.addProperty("id", type.equals("block") ? "minecraft:stone" : "minecraft:pig");
            rule.addProperty("range", 3.0D);
            rule.addProperty("priority", 30);
            rule.addProperty("useItem", "hand");
            JsonObject text = new JsonObject();
            text.addProperty("action", "Use");
            text.addProperty("nameMode", "auto");
            text.addProperty("name", "");
            text.addProperty("showAction", true);
            text.addProperty("showName", true);
            rule.add("text", text);
            JsonObject input = new JsonObject();
            input.addProperty("allowE", true);
            input.addProperty("allowRightClick", true);
            rule.add("input", input);
            JsonObject click = new JsonObject(); click.addProperty("face", "front"); rule.add("click", click);
            JsonObject anchor = new JsonObject();
            JsonArray position = new JsonArray(); position.add(0.5); position.add(0.5); position.add(0.5);
            anchor.add("position", position); anchor.addProperty("rotateWith", "none"); rule.add("anchor", anchor);
            array(root, "interactions").add(rule);
            Minecraft.getInstance().setScreen(new ContextDetailScreen(this, rule));
        }

        private void rebuildRows() {
            String needle = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
            List<JsonObject> rows = new ArrayList<>();
            for (JsonElement element : array(root, "interactions")) {
                if (!element.isJsonObject()) continue;
                JsonObject rule = element.getAsJsonObject();
                JsonObject text = object(rule, "text");
                String haystack = string(rule, "type", "") + " " + string(rule, "id", "") + " " + string(text, "action", "") + " " + string(text, "name", "");
                if (needle.isEmpty() || haystack.toLowerCase(Locale.ROOT).contains(needle)) rows.add(rule);
            }
            rows.sort(Comparator.comparing(rule -> string(rule, "id", ""), String.CASE_INSENSITIVE_ORDER));
            filtered = rows;
            refreshRows();
        }

        private void refreshRows() {
            clearWidgets();
            int w = Math.min(700, width - 16);
            int x = left(width, w) + 12;
            int top = Math.max(8, (height - Math.min(410, height - 16)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(top); searchBox.setWidth(w - 220); addRenderableWidget(searchBox);
            addRenderableWidget(Button.builder(Component.literal("+ Block"), b -> addRule("block")).bounds(x + w - 208, top, 94, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+ Entity"), b -> addRule("entity")).bounds(x + w - 108, top, 96, 20).build());
            int listY = top + 30;
            int visible = Math.max(5, Math.min(11, (height - 132) / 24));
            scroll = Math.min(scroll, Math.max(0, filtered.size() - visible));
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
                JsonObject rule = filtered.get(i);
                int row = i - scroll;
                String label = "[" + string(rule, "type", "?") + "] " + string(rule, "id", "unknown") + " — " + string(object(rule, "text"), "action", "Use");
                addRenderableWidget(Button.builder(Component.literal(compact(label, 73)), b -> Minecraft.getInstance().setScreen(new ContextDetailScreen(this, rule)))
                        .bounds(x, listY + row * 24, w - 82, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> { removeIdentity(array(root, "interactions"), rule); rebuildRows(); })
                        .bounds(x + w - 74, listY + row * 24, 62, 20).build());
            }
            int bottom = Math.min(height - 28, listY + visible * 24 + 5);
            addRenderableWidget(Button.builder(Component.literal("Save & Reload"), b -> submit(Map.of(ConfigCenterService.CONTEXT, root)))
                    .bounds(x, bottom, 118, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> goBack()).bounds(x + w - 92, bottom, 80, 20).build());
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int visible = Math.max(5, Math.min(11, (height - 132) / 24));
            int next = Math.max(0, Math.min(Math.max(0, filtered.size() - visible), scroll + (delta < 0 ? 1 : -1)));
            if (next != scroll) { scroll = next; refreshRows(); return true; }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(700, width - 16);
            int h = Math.min(410, height - 16);
            int x = left(width, w);
            int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            graphics.drawString(font, "The K key remains available for visual anchor placement directly in the world.", x + 12, y + h - 17, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class ContextDetailScreen extends ConfigScreen {
        private final JsonObject original;
        private final JsonObject edit;
        private EditBox idBox;
        private EditBox actionBox;
        private EditBox nameBox;
        private EditBox rangeBox;
        private EditBox priorityBox;
        private EditBox useItemBox;
        private EditBox clickFaceBox;
        private EditBox rotateBox;
        private EditBox anchorXBox;
        private EditBox anchorYBox;
        private EditBox anchorZBox;
        private String type;
        private boolean allowE;
        private boolean allowRightClick;
        private boolean showName;
        private boolean advanced;

        private ContextDetailScreen(Screen parent, JsonObject rule) {
            super(parent, "Context Interaction");
            this.original = rule;
            this.edit = rule.deepCopy();
            this.type = string(edit, "type", "block");
            JsonObject input = object(edit, "input");
            this.allowE = bool(input, "allowE", true);
            this.allowRightClick = bool(input, "allowRightClick", true);
            this.showName = bool(object(edit, "text"), "showName", true);
            this.advanced = edit.has("anchor") || edit.has("click") || edit.has("priority") || edit.has("useItem");
        }

        @Override
        protected void init() {
            int w = Math.min(700, width - 16);
            int x = left(width, w) + 14;
            int y = Math.max(8, (height - Math.min(450, height - 16)) / 2) + 44;
            int fieldW = w - 28;
            addRenderableWidget(Button.builder(Component.literal("Target type: " + type.toUpperCase(Locale.ROOT)), b -> {
                type = type.equals("block") ? "entity" : "block";
                b.setMessage(Component.literal("Target type: " + type.toUpperCase(Locale.ROOT)));
            }).bounds(x, y, 150, 20).build());
            idBox = field(x + 156, y, fieldW - 156, "Target resource ID", string(edit, "id", "")); y += 30;
            actionBox = field(x, y, (fieldW - 6) / 2, "Action text", string(object(edit, "text"), "action", "Use"));
            nameBox = field(x + (fieldW - 6) / 2 + 6, y, (fieldW - 6) / 2, "Manual object name (optional)", string(object(edit, "text"), "name", "")); y += 30;
            rangeBox = field(x, y, (fieldW - 6) / 2, "Interaction range", Double.toString(decimal(edit, "range", 3.0D)));
            priorityBox = field(x + (fieldW - 6) / 2 + 6, y, (fieldW - 6) / 2, "Priority", Integer.toString(integer(edit, "priority", 30))); y += 30;
            addRenderableWidget(Button.builder(Component.literal("E key: " + onOff(allowE)), b -> { allowE = !allowE; b.setMessage(Component.literal("E key: " + onOff(allowE))); })
                    .bounds(x, y, (fieldW - 12) / 3, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Right-click: " + onOff(allowRightClick)), b -> { allowRightClick = !allowRightClick; b.setMessage(Component.literal("Right-click: " + onOff(allowRightClick))); })
                    .bounds(x + (fieldW - 12) / 3 + 6, y, (fieldW - 12) / 3, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Show name: " + onOff(showName)), b -> { showName = !showName; b.setMessage(Component.literal("Show name: " + onOff(showName))); })
                    .bounds(x + 2 * ((fieldW - 12) / 3 + 6), y, (fieldW - 12) / 3, 20).build()); y += 28;
            addRenderableWidget(Button.builder(Component.literal((advanced ? "▼" : "▶") + " Anchor and simulated click details"), b -> { sync(); advanced = !advanced; rebuild(); })
                    .bounds(x, y, fieldW, 20).build()); y += 26;
            if (advanced) {
                useItemBox = field(x, y, (fieldW - 12) / 3, "Use item: hand/held/none", string(edit, "useItem", "hand"));
                clickFaceBox = field(x + (fieldW - 12) / 3 + 6, y, (fieldW - 12) / 3, "Click face", string(object(edit, "click"), "face", "front"));
                rotateBox = field(x + 2 * ((fieldW - 12) / 3 + 6), y, (fieldW - 12) / 3, "Rotate with", string(object(edit, "anchor"), "rotateWith", "none")); y += 30;
                JsonArray pos = anchorPosition(edit);
                anchorXBox = field(x, y, (fieldW - 12) / 3, "Anchor X", pos.get(0).getAsString());
                anchorYBox = field(x + (fieldW - 12) / 3 + 6, y, (fieldW - 12) / 3, "Anchor Y", pos.get(1).getAsString());
                anchorZBox = field(x + 2 * ((fieldW - 12) / 3 + 6), y, (fieldW - 12) / 3, "Anchor Z", pos.get(2).getAsString()); y += 32;
            }
            int buttonY = Math.min(height - 30, y + 3);
            addRenderableWidget(Button.builder(Component.literal("Apply"), b -> save()).bounds(x, buttonY, (fieldW - 6) / 2, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> goBack()).bounds(x + (fieldW - 6) / 2 + 6, buttonY, (fieldW - 6) / 2, 20).build());
        }

        private EditBox field(int x, int y, int width, String hint, String value) {
            EditBox box = new EditBox(font, x, y, width, 20, Component.literal(hint));
            box.setHint(Component.literal(hint)); box.setMaxLength(1024); box.setValue(value == null ? "" : value); addRenderableWidget(box); return box;
        }

        private JsonArray anchorPosition(JsonObject root) {
            JsonObject anchor = object(root, "anchor");
            if (!anchor.has("position") || !anchor.get("position").isJsonArray() || anchor.getAsJsonArray("position").size() != 3) {
                JsonArray pos = new JsonArray(); pos.add(0.5D); pos.add(0.5D); pos.add(0.5D); anchor.add("position", pos);
            }
            return anchor.getAsJsonArray("position");
        }

        private void sync() {
            if (idBox == null) return;
            edit.addProperty("type", type);
            edit.addProperty("id", idBox.getValue().trim());
            edit.addProperty("range", parseDouble(rangeBox.getValue(), 3.0D));
            edit.addProperty("priority", parseInt(priorityBox.getValue(), 30));
            JsonObject text = object(edit, "text");
            text.addProperty("action", actionBox.getValue());
            text.addProperty("name", nameBox.getValue());
            text.addProperty("nameMode", nameBox.getValue().isBlank() ? "auto" : "manual");
            text.addProperty("showAction", true);
            text.addProperty("showName", showName);
            JsonObject input = object(edit, "input"); input.addProperty("allowE", allowE); input.addProperty("allowRightClick", allowRightClick);
            if (advanced && useItemBox != null) {
                edit.addProperty("useItem", useItemBox.getValue().trim());
                object(edit, "click").addProperty("face", clickFaceBox.getValue().trim());
                JsonObject anchor = object(edit, "anchor");
                anchor.addProperty("rotateWith", rotateBox.getValue().trim());
                JsonArray pos = new JsonArray(); pos.add(parseDouble(anchorXBox.getValue(), 0.5)); pos.add(parseDouble(anchorYBox.getValue(), 0.5)); pos.add(parseDouble(anchorZBox.getValue(), 0.5));
                anchor.add("position", pos);
            }
        }

        private void save() {
            sync();
            String id = string(edit, "id", "");
            try { ResourceLocation.parse(id); }
            catch (Exception ignored) { idBox.setTextColor(BAD); return; }
            for (String key : new ArrayList<>(original.keySet())) original.remove(key);
            for (Map.Entry<String, JsonElement> entry : edit.entrySet()) original.add(entry.getKey(), entry.getValue().deepCopy());
            goBack();
        }

        private void rebuild() { clearWidgets(); init(); }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(700, width - 16);
            int h = Math.min(450, height - 16);
            int x = left(width, w);
            int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            graphics.drawString(font, "Use K while looking at the object for live visual anchor positioning.", x + 14, y + 30, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static int parseInt(String text, int fallback) {
        try { return Integer.parseInt(text.trim()); } catch (Exception ignored) { return fallback; }
    }

    private static double parseDouble(String text, double fallback) {
        try { return Double.parseDouble(text.trim()); } catch (Exception ignored) { return fallback; }
    }

    private static final class DrinkListScreen extends ConfigScreen {
        private final JsonObject root;
        private final EditBox searchBox;
        private List<JsonObject> filtered = List.of();
        private int scroll;

        private DrinkListScreen(Screen parent) {
            super(parent, "SCP-294 Drinks");
            this.root = file(ConfigCenterService.DRINKS, new JsonObject());
            this.searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.literal("Search drinks"));
        }

        @Override
        protected void init() {
            int w = Math.min(700, width - 16);
            int x = left(width, w) + 12;
            int y = Math.max(8, (height - Math.min(410, height - 16)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(y); searchBox.setWidth(w - 142);
            searchBox.setHint(Component.literal("Search by drink id or alias"));
            searchBox.setMaxLength(256);
            searchBox.setResponder(value -> { scroll = 0; rebuildRows(); });
            addRenderableWidget(searchBox);
            addRenderableWidget(Button.builder(Component.literal("+ New Drink"), b -> addDrink())
                    .bounds(x + w - 130, y, 118, 20).build());
            rebuildRows();
        }

        private void addDrink() {
            JsonObject drink = new JsonObject();
            drink.addProperty("id", "scp_additions:new_drink");
            drink.addProperty("enabled", true);
            JsonArray aliases = new JsonArray(); aliases.add("new drink"); drink.add("aliases", aliases);
            JsonObject result = new JsonObject(); result.addProperty("item", "scp_additions:cup_of_coffee"); result.addProperty("count", 1); drink.add("result", result);
            drink.addProperty("delay_ticks", 40);
            drink.addProperty("sound", "scp_additions:scp294pouring");
            drink.addProperty("consumes_coin", true);
            drink.addProperty("give_result", true);
            drink.addProperty("drinkable", true);
            drink.addProperty("cup_color", "#FFFFFF");
            drink.add("effects", new JsonArray());
            array(root, "drinks").add(drink);
            Minecraft.getInstance().setScreen(new DrinkDetailScreen(this, drink));
        }

        private void duplicate(JsonObject original) {
            JsonObject copy = original.deepCopy();
            copy.addProperty("id", string(original, "id", "scp_additions:drink") + "_copy");
            array(root, "drinks").add(copy);
            Minecraft.getInstance().setScreen(new DrinkDetailScreen(this, copy));
        }

        private void rebuildRows() {
            String needle = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
            List<JsonObject> rows = new ArrayList<>();
            for (JsonElement element : array(root, "drinks")) {
                if (!element.isJsonObject()) continue;
                JsonObject drink = element.getAsJsonObject();
                StringBuilder haystack = new StringBuilder(string(drink, "id", ""));
                if (drink.has("aliases") && drink.get("aliases").isJsonArray()) for (JsonElement alias : drink.getAsJsonArray("aliases")) if (alias.isJsonPrimitive()) haystack.append(' ').append(alias.getAsString());
                if (needle.isEmpty() || haystack.toString().toLowerCase(Locale.ROOT).contains(needle)) rows.add(drink);
            }
            rows.sort(Comparator.comparing(drink -> string(drink, "id", ""), String.CASE_INSENSITIVE_ORDER));
            filtered = rows;
            refreshRows();
        }

        private void refreshRows() {
            clearWidgets();
            int w = Math.min(700, width - 16);
            int x = left(width, w) + 12;
            int top = Math.max(8, (height - Math.min(410, height - 16)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(top); searchBox.setWidth(w - 142); addRenderableWidget(searchBox);
            addRenderableWidget(Button.builder(Component.literal("+ New Drink"), b -> addDrink()).bounds(x + w - 130, top, 118, 20).build());
            int listY = top + 30;
            int visible = Math.max(5, Math.min(11, (height - 132) / 24));
            scroll = Math.min(scroll, Math.max(0, filtered.size() - visible));
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
                JsonObject drink = filtered.get(i);
                int row = i - scroll;
                String id = string(drink, "id", "unknown");
                String aliases = firstAlias(drink);
                String label = id + (aliases.isBlank() ? "" : " — “" + aliases + "”") + (bool(drink, "enabled", true) ? "" : " [disabled]");
                addRenderableWidget(Button.builder(Component.literal(compact(label, 67)), b -> Minecraft.getInstance().setScreen(new DrinkDetailScreen(this, drink)))
                        .bounds(x, listY + row * 24, w - 150, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Copy"), b -> duplicate(drink))
                        .bounds(x + w - 142, listY + row * 24, 62, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> { removeIdentity(array(root, "drinks"), drink); rebuildRows(); })
                        .bounds(x + w - 74, listY + row * 24, 62, 20).build());
            }
            int bottom = Math.min(height - 28, listY + visible * 24 + 5);
            addRenderableWidget(Button.builder(Component.literal("Save & Reload"), b -> submit(Map.of(ConfigCenterService.DRINKS, root)))
                    .bounds(x, bottom, 118, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> goBack()).bounds(x + w - 92, bottom, 80, 20).build());
        }

        private String firstAlias(JsonObject drink) {
            if (!drink.has("aliases") || !drink.get("aliases").isJsonArray() || drink.getAsJsonArray("aliases").size() == 0) return "";
            JsonElement first = drink.getAsJsonArray("aliases").get(0);
            return first.isJsonPrimitive() ? first.getAsString() : "";
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int visible = Math.max(5, Math.min(11, (height - 132) / 24));
            int next = Math.max(0, Math.min(Math.max(0, filtered.size() - visible), scroll + (delta < 0 ? 1 : -1)));
            if (next != scroll) { scroll = next; refreshRows(); return true; }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(700, width - 16);
            int h = Math.min(410, height - 16);
            int x = left(width, w);
            int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            graphics.drawString(font, "Unknown custom action fields are preserved when a drink is edited.", x + 12, y + h - 17, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class DrinkDetailScreen extends ConfigScreen {
        private final JsonObject original;
        private final JsonObject edit;
        private EditBox idBox;
        private EditBox aliasesBox;
        private EditBox resultBox;
        private EditBox countBox;
        private EditBox colorBox;
        private EditBox delayBox;
        private EditBox soundBox;
        private EditBox actionbarBox;
        private EditBox refuseBox;
        private boolean enabled;
        private boolean consumesCoin;
        private boolean giveResult;
        private boolean drinkable;
        private boolean advanced;

        private DrinkDetailScreen(Screen parent, JsonObject drink) {
            super(parent, "SCP-294 Drink");
            this.original = drink;
            this.edit = drink.deepCopy();
            this.enabled = bool(edit, "enabled", true);
            this.consumesCoin = bool(edit, "consumes_coin", true);
            this.giveResult = bool(edit, "give_result", true);
            this.drinkable = bool(edit, "drinkable", true);
            this.advanced = edit.has("sound") || edit.has("actionbar") || edit.has("refuse_message") || edit.has("dispense_actions") || edit.has("drink_actions") || edit.has("actions");
        }

        @Override
        protected void init() {
            int w = Math.min(730, width - 14);
            int x = left(width, w) + 14;
            int y = Math.max(7, (height - Math.min(470, height - 14)) / 2) + 43;
            int fieldW = w - 28;
            idBox = field(x, y, fieldW, "Drink ID", string(edit, "id", "")); y += 30;
            aliasesBox = field(x, y, fieldW, "Aliases separated by commas", aliases(edit)); y += 30;
            int resultW = fieldW - 126;
            JsonObject result = object(edit, "result");
            resultBox = field(x, y, resultW, "Result item ID", string(result, "item", ""));
            addRenderableWidget(Button.builder(Component.literal("Choose Item"), b -> openItemPicker(id -> {
                resultBox.setValue(id);
                Minecraft.getInstance().setScreen(this);
            }, this)).bounds(x + resultW + 6, y, 120, 20).build()); y += 30;
            int third = (fieldW - 12) / 3;
            countBox = field(x, y, third, "Result count", Integer.toString(integer(result, "count", 1)));
            colorBox = field(x + third + 6, y, third, "Cup color #RRGGBB", string(edit, "cup_color", "#FFFFFF"));
            addRenderableWidget(Button.builder(Component.literal("Effects: " + array(edit, "effects").size()), b -> {
                sync(); Minecraft.getInstance().setScreen(new DrinkEffectsScreen(this, edit));
            }).bounds(x + 2 * (third + 6), y, third, 20).build()); y += 30;
            addRenderableWidget(Button.builder(Component.literal("Enabled: " + onOff(enabled)), b -> { enabled = !enabled; b.setMessage(Component.literal("Enabled: " + onOff(enabled))); })
                    .bounds(x, y, third, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Consumes coin: " + onOff(consumesCoin)), b -> { consumesCoin = !consumesCoin; b.setMessage(Component.literal("Consumes coin: " + onOff(consumesCoin))); })
                    .bounds(x + third + 6, y, third, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Drinkable: " + onOff(drinkable)), b -> { drinkable = !drinkable; b.setMessage(Component.literal("Drinkable: " + onOff(drinkable))); })
                    .bounds(x + 2 * (third + 6), y, third, 20).build()); y += 28;
            addRenderableWidget(Button.builder(Component.literal((advanced ? "▼" : "▶") + " Additional dispensing behavior"), b -> { sync(); advanced = !advanced; rebuild(); })
                    .bounds(x, y, fieldW, 20).build()); y += 26;
            if (advanced) {
                delayBox = field(x, y, third, "Delay ticks", Integer.toString(integer(edit, "delay_ticks", 40)));
                soundBox = field(x + third + 6, y, fieldW - third - 6, "Sound resource ID", string(edit, "sound", "")); y += 30;
                actionbarBox = field(x, y, fieldW, "Action bar text", string(edit, "actionbar", "")); y += 30;
                refuseBox = field(x, y, fieldW, "Refusal message", string(edit, "refuse_message", "")); y += 30;
                addRenderableWidget(Button.builder(Component.literal("Give result item: " + onOff(giveResult)), b -> { giveResult = !giveResult; b.setMessage(Component.literal("Give result item: " + onOff(giveResult))); })
                        .bounds(x, y, fieldW, 20).build()); y += 25;
            }
            int buttonY = Math.min(height - 29, y + 3);
            addRenderableWidget(Button.builder(Component.literal("Apply"), b -> save()).bounds(x, buttonY, (fieldW - 6) / 2, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> goBack()).bounds(x + (fieldW - 6) / 2 + 6, buttonY, (fieldW - 6) / 2, 20).build());
        }

        private EditBox field(int x, int y, int width, String hint, String value) {
            EditBox box = new EditBox(font, x, y, width, 20, Component.literal(hint));
            box.setHint(Component.literal(hint)); box.setMaxLength(4096); box.setValue(value == null ? "" : value); addRenderableWidget(box); return box;
        }

        private String aliases(JsonObject drink) {
            if (!drink.has("aliases") || !drink.get("aliases").isJsonArray()) return "";
            List<String> values = new ArrayList<>();
            for (JsonElement element : drink.getAsJsonArray("aliases")) if (element.isJsonPrimitive()) values.add(element.getAsString());
            return String.join(", ", values);
        }

        private void sync() {
            if (idBox == null) return;
            edit.addProperty("id", idBox.getValue().trim());
            edit.addProperty("enabled", enabled);
            JsonArray aliases = new JsonArray();
            for (String alias : aliasesBox.getValue().split(",")) if (!alias.trim().isEmpty()) aliases.add(alias.trim());
            edit.add("aliases", aliases);
            JsonObject result = object(edit, "result"); result.addProperty("item", resultBox.getValue().trim()); result.addProperty("count", Math.max(1, parseInt(countBox.getValue(), 1)));
            edit.addProperty("cup_color", colorBox.getValue().trim().toUpperCase(Locale.ROOT));
            edit.addProperty("consumes_coin", consumesCoin);
            edit.addProperty("give_result", giveResult);
            edit.addProperty("drinkable", drinkable);
            if (advanced && delayBox != null) {
                edit.addProperty("delay_ticks", Math.max(0, parseInt(delayBox.getValue(), 40)));
                setOrRemove(edit, "sound", soundBox.getValue());
                setOrRemove(edit, "actionbar", actionbarBox.getValue());
                setOrRemove(edit, "refuse_message", refuseBox.getValue());
            }
        }

        private void save() {
            sync();
            try { new ResourceLocation(string(edit, "id", "")); }
            catch (Exception ignored) { idBox.setTextColor(BAD); return; }
            try { new ResourceLocation(string(object(edit, "result"), "item", "")); }
            catch (Exception ignored) { resultBox.setTextColor(BAD); return; }
            if (!string(edit, "cup_color", "").matches("#[0-9A-Fa-f]{6}")) { colorBox.setTextColor(BAD); return; }
            for (String key : new ArrayList<>(original.keySet())) original.remove(key);
            for (Map.Entry<String, JsonElement> entry : edit.entrySet()) original.add(entry.getKey(), entry.getValue().deepCopy());
            goBack();
        }

        private void rebuild() { clearWidgets(); init(); }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(730, width - 14);
            int h = Math.min(470, height - 14);
            int x = left(width, w);
            int y = Math.max(7, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            graphics.drawString(font, "Custom dispense/drink action arrays are preserved even when their fields are not shown here.", x + 14, y + 29, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class DrinkEffectsScreen extends ConfigScreen {
        private final JsonObject drink;
        private int scroll;

        private DrinkEffectsScreen(Screen parent, JsonObject drink) {
            super(parent, "SCP-294 Drink Effects");
            this.drink = drink;
        }

        @Override
        protected void init() {
            rebuild();
        }

        private void rebuild() {
            clearWidgets();
            int w = Math.min(680, width - 16);
            int x = left(width, w) + 12;
            int top = Math.max(8, (height - Math.min(410, height - 16)) / 2) + 42;
            addRenderableWidget(Button.builder(Component.literal("+ Add Effect"), b -> {
                JsonObject effect = new JsonObject(); effect.addProperty("id", "minecraft:speed"); effect.addProperty("duration", 200); effect.addProperty("amplifier", 0); effect.addProperty("visible", true); effect.addProperty("show_icon", true);
                array(drink, "effects").add(effect);
                Minecraft.getInstance().setScreen(new DrinkEffectDetailScreen(this, effect));
            }).bounds(x, top, 120, 20).build());
            JsonArray effects = array(drink, "effects");
            int visible = Math.max(4, Math.min(10, (height - 130) / 24));
            scroll = Math.min(scroll, Math.max(0, effects.size() - visible));
            int listY = top + 30;
            for (int i = scroll; i < Math.min(effects.size(), scroll + visible); i++) {
                JsonElement element = effects.get(i);
                if (!element.isJsonObject()) continue;
                JsonObject effect = element.getAsJsonObject();
                int row = i - scroll;
                String label = string(effect, "id", "unknown") + "  " + integer(effect, "duration", 200) + "t  amplifier " + integer(effect, "amplifier", 0);
                addRenderableWidget(Button.builder(Component.literal(compact(label, 65)), b -> Minecraft.getInstance().setScreen(new DrinkEffectDetailScreen(this, effect)))
                        .bounds(x, listY + row * 24, w - 82, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> { removeIdentity(effects, effect); rebuild(); })
                        .bounds(x + w - 74, listY + row * 24, 62, 20).build());
            }
            int bottom = Math.min(height - 28, listY + visible * 24 + 5);
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> goBack()).bounds(x + w - 92, bottom, 80, 20).build());
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int visible = Math.max(4, Math.min(10, (height - 130) / 24));
            int next = Math.max(0, Math.min(Math.max(0, array(drink, "effects").size() - visible), scroll + (delta < 0 ? 1 : -1)));
            if (next != scroll) { scroll = next; rebuild(); return true; }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(680, width - 16); int h = Math.min(410, height - 16); int x = left(width, w); int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class DrinkEffectDetailScreen extends ConfigScreen {
        private final JsonObject effect;
        private EditBox idBox;
        private EditBox durationBox;
        private EditBox amplifierBox;
        private boolean visible;
        private boolean showIcon;

        private DrinkEffectDetailScreen(Screen parent, JsonObject effect) {
            super(parent, "Potion Effect"); this.effect = effect; this.visible = bool(effect, "visible", true); this.showIcon = bool(effect, "show_icon", true);
        }

        @Override
        protected void init() {
            int w = Math.min(500, width - 20); int x = left(width, w) + 14; int y = Math.max(10, (height - 290) / 2) + 55; int fw = w - 28;
            idBox = new EditBox(font, x, y, fw, 20, Component.literal("Effect resource ID")); idBox.setHint(Component.literal("Effect resource ID")); idBox.setMaxLength(256); idBox.setValue(string(effect, "id", "minecraft:speed")); addRenderableWidget(idBox); y += 32;
            durationBox = new EditBox(font, x, y, (fw - 6) / 2, 20, Component.literal("Duration ticks")); durationBox.setHint(Component.literal("Duration ticks")); durationBox.setValue(Integer.toString(integer(effect, "duration", 200))); addRenderableWidget(durationBox);
            amplifierBox = new EditBox(font, x + (fw - 6) / 2 + 6, y, (fw - 6) / 2, 20, Component.literal("Amplifier")); amplifierBox.setHint(Component.literal("Amplifier")); amplifierBox.setValue(Integer.toString(integer(effect, "amplifier", 0))); addRenderableWidget(amplifierBox); y += 34;
            addRenderableWidget(Button.builder(Component.literal("Particles visible: " + onOff(visible)), b -> { visible = !visible; b.setMessage(Component.literal("Particles visible: " + onOff(visible))); }).bounds(x, y, (fw - 6) / 2, 20).build());
            addRenderableWidget(Button.builder(Component.literal("HUD icon: " + onOff(showIcon)), b -> { showIcon = !showIcon; b.setMessage(Component.literal("HUD icon: " + onOff(showIcon))); }).bounds(x + (fw - 6) / 2 + 6, y, (fw - 6) / 2, 20).build()); y += 42;
            addRenderableWidget(Button.builder(Component.literal("Apply"), b -> save()).bounds(x, y, (fw - 6) / 2, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> goBack()).bounds(x + (fw - 6) / 2 + 6, y, (fw - 6) / 2, 20).build());
        }

        private void save() {
            try { new ResourceLocation(idBox.getValue().trim()); } catch (Exception ignored) { idBox.setTextColor(BAD); return; }
            effect.addProperty("id", idBox.getValue().trim()); effect.addProperty("duration", Math.max(1, parseInt(durationBox.getValue(), 200))); effect.addProperty("amplifier", Math.max(0, parseInt(amplifierBox.getValue(), 0))); effect.addProperty("visible", visible); effect.addProperty("show_icon", showIcon); goBack();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics); int w = Math.min(500, width - 20); int x = left(width, w); int y = Math.max(10, (height - 290) / 2); panel(graphics, x, y, w, 290, screenTitle, font); super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private record RecipeRef(String source, JsonObject recipe) {
    }

    private static final class RecipeListScreen extends ConfigScreen {
        private final LinkedHashMap<String, JsonObject> roots = new LinkedHashMap<>();
        private final Set<String> dirty = new HashSet<>();
        private final EditBox searchBox;
        private List<RecipeRef> filtered = List.of();
        private int scroll;

        private RecipeListScreen(Screen parent) {
            super(parent, "SCP-914 Recipes");
            files.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(ConfigCenterService.RECIPE_PREFIX) && entry.getValue().isJsonObject())
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> roots.put(entry.getKey(), entry.getValue().getAsJsonObject().deepCopy()));
            if (!roots.containsKey(ConfigCenterService.RECIPE_MAIN)) {
                JsonObject main = new JsonObject(); main.addProperty("version", 2); main.add("recipes", new JsonArray()); roots.put(ConfigCenterService.RECIPE_MAIN, main);
            }
            this.searchBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.literal("Search recipes"));
        }

        @Override
        protected void init() {
            int w = Math.min(760, width - 12);
            int x = left(width, w) + 12;
            int y = Math.max(6, (height - Math.min(440, height - 12)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(y); searchBox.setWidth(w - 336);
            searchBox.setHint(Component.literal("Search recipe id, intake, output, or setting"));
            searchBox.setMaxLength(256);
            searchBox.setResponder(value -> { scroll = 0; rebuildRows(); });
            addRenderableWidget(searchBox);
            addRenderableWidget(Button.builder(Component.literal("+ Recipe"), b -> addRecipe()).bounds(x + w - 324, y, 92, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Machine"), b -> Minecraft.getInstance().setScreen(new MachineSettingsScreen(this, roots.get(ConfigCenterService.RECIPE_MAIN), dirty)))
                    .bounds(x + w - 226, y, 92, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Save All"), b -> saveAll()).bounds(x + w - 128, y, 116, 20).build());
            rebuildRows();
        }

        private JsonObject editorRoot() {
            String key = ConfigCenterService.RECIPE_PREFIX + ConfigCenterService.EDITOR_FRAGMENT;
            return roots.computeIfAbsent(key, ignored -> {
                JsonObject root = new JsonObject(); root.addProperty("version", 2); root.add("recipes", new JsonArray()); return root;
            });
        }

        private void addRecipe() {
            String source = ConfigCenterService.RECIPE_PREFIX + ConfigCenterService.EDITOR_FRAGMENT;
            JsonObject recipe = new JsonObject();
            recipe.addProperty("id", "scp_additions:new_914_recipe");
            recipe.addProperty("enabled", true);
            recipe.addProperty("setting", "1_to_1");
            JsonArray inputs = new JsonArray(); JsonObject input = new JsonObject(); input.addProperty("item", "minecraft:cobblestone"); input.addProperty("count", 1); inputs.add(input); recipe.add("item_inputs", inputs);
            JsonArray outputs = new JsonArray(); JsonObject output = new JsonObject(); output.addProperty("item", "minecraft:stone"); output.addProperty("count", 1); outputs.add(output); recipe.add("item_outputs", outputs);
            recipe.addProperty("chance", 1.0D); recipe.addProperty("copy_input_nbt", false);
            editorRoot();
            Minecraft.getInstance().setScreen(new RecipeDetailScreen(this, source, recipe));
        }

        private void duplicate(RecipeRef ref) {
            JsonObject copy = ref.recipe().deepCopy();
            copy.addProperty("id", string(ref.recipe(), "id", "scp_additions:recipe") + "_copy");
            String source = ConfigCenterService.RECIPE_PREFIX + ConfigCenterService.EDITOR_FRAGMENT;
            editorRoot();
            Minecraft.getInstance().setScreen(new RecipeDetailScreen(this, source, copy));
        }

        private void apply(String oldSource, JsonObject original, String newSource, JsonObject replacement) {
            JsonObject oldRoot = roots.get(oldSource);
            if (oldRoot != null) removeIdentity(array(oldRoot, "recipes"), original);
            dirty.add(oldSource);
            JsonObject newRoot = roots.computeIfAbsent(newSource, ignored -> {
                JsonObject root = new JsonObject(); root.addProperty("version", 2); root.add("recipes", new JsonArray()); return root;
            });
            array(newRoot, "recipes").add(replacement);
            dirty.add(newSource);
            rebuildRows();
        }

        private void delete(RecipeRef ref) {
            JsonObject root = roots.get(ref.source());
            if (root != null) removeIdentity(array(root, "recipes"), ref.recipe());
            dirty.add(ref.source());
            rebuildRows();
        }

        private List<String> sourceKeys() {
            List<String> keys = new ArrayList<>(roots.keySet());
            String editor = ConfigCenterService.RECIPE_PREFIX + ConfigCenterService.EDITOR_FRAGMENT;
            if (!keys.contains(editor)) keys.add(editor);
            keys.sort(String::compareToIgnoreCase);
            return keys;
        }

        private void saveAll() {
            if (dirty.isEmpty()) { homeNotice = "No SCP-914 recipe changes to save."; Minecraft.getInstance().setScreen(new HomeScreen()); return; }
            LinkedHashMap<String, JsonObject> changes = new LinkedHashMap<>();
            for (String key : dirty) {
                JsonObject root = roots.get(key);
                if (root != null) changes.put(key, root);
            }
            submit(changes);
        }

        private void rebuildRows() {
            String needle = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
            List<RecipeRef> rows = new ArrayList<>();
            for (Map.Entry<String, JsonObject> entry : roots.entrySet()) {
                for (JsonElement element : array(entry.getValue(), "recipes")) {
                    if (!element.isJsonObject()) continue;
                    JsonObject recipe = element.getAsJsonObject();
                    String haystack = recipeHaystack(recipe);
                    if (needle.isEmpty() || haystack.toLowerCase(Locale.ROOT).contains(needle)) rows.add(new RecipeRef(entry.getKey(), recipe));
                }
            }
            rows.sort(Comparator.comparing(ref -> string(ref.recipe(), "id", ""), String.CASE_INSENSITIVE_ORDER));
            filtered = rows;
            refreshRows();
        }

        private String recipeHaystack(JsonObject recipe) {
            StringBuilder builder = new StringBuilder(string(recipe, "id", "")).append(' ').append(string(recipe, "setting", ""));
            for (String key : List.of("item_inputs", "item_outputs", "weighted_item_outputs", "entity_inputs", "entity_outputs")) {
                if (!recipe.has(key) || !recipe.get(key).isJsonArray()) continue;
                for (JsonElement element : recipe.getAsJsonArray(key)) if (element.isJsonObject()) builder.append(' ').append(string(element.getAsJsonObject(), "item", string(element.getAsJsonObject(), "entity", "")));
            }
            return builder.toString();
        }

        private void refreshRows() {
            clearWidgets();
            int w = Math.min(760, width - 12);
            int x = left(width, w) + 12;
            int top = Math.max(6, (height - Math.min(440, height - 12)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(top); searchBox.setWidth(w - 336); addRenderableWidget(searchBox);
            addRenderableWidget(Button.builder(Component.literal("+ Recipe"), b -> addRecipe()).bounds(x + w - 324, top, 92, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Machine"), b -> Minecraft.getInstance().setScreen(new MachineSettingsScreen(this, roots.get(ConfigCenterService.RECIPE_MAIN), dirty))).bounds(x + w - 226, top, 92, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Save All"), b -> saveAll()).bounds(x + w - 128, top, 116, 20).build());
            int listY = top + 30;
            int visible = Math.max(5, Math.min(12, (height - 130) / 24));
            scroll = Math.min(scroll, Math.max(0, filtered.size() - visible));
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
                RecipeRef ref = filtered.get(i);
                int row = i - scroll;
                String id = string(ref.recipe(), "id", "unknown");
                String setting = string(ref.recipe(), "setting", "?");
                String source = ref.source().equals(ConfigCenterService.RECIPE_MAIN) ? "main" : ref.source().substring(ConfigCenterService.RECIPE_PREFIX.length());
                String label = id + "  [" + setting + "]  ‹" + source + "›";
                addRenderableWidget(Button.builder(Component.literal(compact(label, 69)), b -> Minecraft.getInstance().setScreen(new RecipeDetailScreen(this, ref.source(), ref.recipe())))
                        .bounds(x, listY + row * 24, w - 154, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Copy"), b -> duplicate(ref)).bounds(x + w - 146, listY + row * 24, 64, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> delete(ref)).bounds(x + w - 76, listY + row * 24, 64, 20).build());
            }
            int bottom = Math.min(height - 28, listY + visible * 24 + 5);
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> goBack()).bounds(x + w - 92, bottom, 80, 20).build());
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int visible = Math.max(5, Math.min(12, (height - 130) / 24));
            int next = Math.max(0, Math.min(Math.max(0, filtered.size() - visible), scroll + (delta < 0 ? 1 : -1)));
            if (next != scroll) { scroll = next; refreshRows(); return true; }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(760, width - 12); int h = Math.min(440, height - 12); int x = left(width, w); int y = Math.max(6, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            graphics.drawString(font, filtered.size() + " matching recipe(s) across " + roots.size() + " file(s)" + (dirty.isEmpty() ? "" : " — unsaved changes"), x + 12, y + h - 17, dirty.isEmpty() ? MUTED : WARN, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class MachineSettingsScreen extends ConfigScreen {
        private final JsonObject mainRoot;
        private final Set<String> dirty;
        private EditBox intakeX, intakeY, intakeZ, outputX, outputY, outputZ, radius, startDelay, finishDelay;

        private MachineSettingsScreen(Screen parent, JsonObject mainRoot, Set<String> dirty) {
            super(parent, "SCP-914 Machine Geometry & Timing"); this.mainRoot = mainRoot; this.dirty = dirty;
        }

        @Override
        protected void init() {
            JsonObject machine = object(mainRoot, "machine");
            JsonArray intake = triple(machine, "intake_offset", -5, 0, -3);
            JsonArray output = triple(machine, "output_offset", 5, 0, -3);
            int w = Math.min(620, width - 18); int x = left(width, w) + 14; int y = Math.max(9, (height - 360) / 2) + 58; int fw = w - 28; int third = (fw - 12) / 3;
            intakeX = field(x, y, third, "Intake X", intake.get(0).getAsString()); intakeY = field(x + third + 6, y, third, "Intake Y", intake.get(1).getAsString()); intakeZ = field(x + 2 * (third + 6), y, third, "Intake Z", intake.get(2).getAsString()); y += 40;
            outputX = field(x, y, third, "Output X", output.get(0).getAsString()); outputY = field(x + third + 6, y, third, "Output Y", output.get(1).getAsString()); outputZ = field(x + 2 * (third + 6), y, third, "Output Z", output.get(2).getAsString()); y += 40;
            radius = field(x, y, third, "Search radius", Double.toString(decimal(machine, "search_radius", 1.5D)));
            startDelay = field(x + third + 6, y, third, "Start delay ticks", Integer.toString(integer(machine, "start_delay_ticks", 30)));
            finishDelay = field(x + 2 * (third + 6), y, third, "Finish delay ticks", Integer.toString(integer(machine, "finish_delay_ticks", 160))); y += 48;
            addRenderableWidget(Button.builder(Component.literal("Apply"), b -> save()).bounds(x, y, (fw - 6) / 2, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> goBack()).bounds(x + (fw - 6) / 2 + 6, y, (fw - 6) / 2, 20).build());
        }

        private EditBox field(int x, int y, int width, String hint, String value) { EditBox box = new EditBox(font, x, y, width, 20, Component.literal(hint)); box.setHint(Component.literal(hint)); box.setValue(value); addRenderableWidget(box); return box; }
        private JsonArray triple(JsonObject root, String key, int x, int y, int z) { if (!root.has(key) || !root.get(key).isJsonArray() || root.getAsJsonArray(key).size() != 3) { JsonArray value = new JsonArray(); value.add(x); value.add(y); value.add(z); root.add(key, value); } return root.getAsJsonArray(key); }
        private void save() {
            JsonObject machine = object(mainRoot, "machine");
            JsonArray intake = new JsonArray(); intake.add(parseInt(intakeX.getValue(), -5)); intake.add(parseInt(intakeY.getValue(), 0)); intake.add(parseInt(intakeZ.getValue(), -3)); machine.add("intake_offset", intake);
            JsonArray output = new JsonArray(); output.add(parseInt(outputX.getValue(), 5)); output.add(parseInt(outputY.getValue(), 0)); output.add(parseInt(outputZ.getValue(), -3)); machine.add("output_offset", output);
            machine.addProperty("search_radius", Math.max(0.5D, parseDouble(radius.getValue(), 1.5D)));
            machine.addProperty("start_delay_ticks", Math.max(0, parseInt(startDelay.getValue(), 30)));
            machine.addProperty("finish_delay_ticks", Math.max(0, parseInt(finishDelay.getValue(), 160)));
            dirty.add(ConfigCenterService.RECIPE_MAIN); goBack();
        }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { renderBackground(graphics); int w = Math.min(620, width - 18); int x = left(width, w); int y = Math.max(9, (height - 360) / 2); panel(graphics, x, y, w, 360, screenTitle, font); graphics.drawString(font, "Offsets are relative to the placed SCP-914 controller orientation.", x + 14, y + 33, MUTED, false); graphics.drawString(font, "These values affect every recipe and should be changed only for custom structures.", x + 14, y + 46, WARN, false); super.render(graphics, mouseX, mouseY, partialTick); }
    }

    private record ItemRender(String id, int x, int y) {
    }

    private static final class RecipeDetailScreen extends ConfigScreen {
        private static final List<String> SETTINGS = List.of("rough", "coarse", "1_to_1", "fine", "very_fine");
        private final RecipeListScreen owner;
        private final String originalSource;
        private final JsonObject original;
        private final JsonObject edit;
        private final List<ItemRender> renders = new ArrayList<>();
        private String targetSource;
        private String setting;
        private boolean enabled;
        private boolean weighted;
        private boolean copyNbt;
        private boolean advanced;
        private int intakeOffset;
        private int outputOffset;
        private EditBox idBox;
        private EditBox chanceBox;
        private EditBox actionbarBox;

        private RecipeDetailScreen(RecipeListScreen owner, String source, JsonObject recipe) {
            super(owner, "SCP-914 Recipe");
            this.owner = owner;
            this.originalSource = source;
            this.targetSource = source;
            this.original = recipe;
            this.edit = recipe.deepCopy();
            this.setting = string(edit, "setting", "1_to_1");
            this.enabled = bool(edit, "enabled", true);
            this.weighted = edit.has("weighted_item_outputs") && edit.get("weighted_item_outputs").isJsonArray() && edit.getAsJsonArray("weighted_item_outputs").size() > 0;
            this.copyNbt = bool(edit, "copy_input_nbt", false);
            this.advanced = weighted || edit.has("actionbar") || edit.has("entity_inputs") || edit.has("entity_outputs") || decimal(edit, "chance", 1.0D) != 1.0D;
        }

        @Override
        protected void init() {
            renders.clear();
            int w = Math.min(790, width - 10);
            int x = left(width, w) + 12;
            int panelTop = Math.max(5, (height - Math.min(480, height - 10)) / 2);
            int y = panelTop + 40;
            int fw = w - 24;
            int idW = Math.max(180, fw - 390);
            idBox = new EditBox(font, x, y, idW, 20, Component.literal("Recipe ID")); idBox.setHint(Component.literal("Recipe ID")); idBox.setMaxLength(256); idBox.setValue(string(edit, "id", "")); addRenderableWidget(idBox);
            addRenderableWidget(Button.builder(Component.literal("Setting: " + settingLabel()), b -> { cycleSetting(); b.setMessage(Component.literal("Setting: " + settingLabel())); })
                    .bounds(x + idW + 6, y, 116, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Enabled: " + onOff(enabled)), b -> { enabled = !enabled; b.setMessage(Component.literal("Enabled: " + onOff(enabled))); })
                    .bounds(x + idW + 128, y, 104, 20).build());
            addRenderableWidget(Button.builder(Component.literal("File: " + compact(sourceName(targetSource), 18)), b -> { cycleSource(); b.setMessage(Component.literal("File: " + compact(sourceName(targetSource), 18))); })
                    .bounds(x + idW + 238, y, fw - idW - 238, 20).build());

            int columnsY = y + 38;
            int gap = 12;
            int columnW = (fw - gap) / 2;
            buildItemColumn(x, columnsY, columnW, true);
            buildItemColumn(x + columnW + gap, columnsY, columnW, false);

            int advancedY = columnsY + 154;
            addRenderableWidget(Button.builder(Component.literal((advanced ? "▼" : "▶") + " Additional recipe settings"), b -> { syncHeader(); advanced = !advanced; rebuild(); })
                    .bounds(x, advancedY, fw, 20).build());
            int buttonY;
            if (advanced) {
                int ay = advancedY + 27;
                int half = (fw - 6) / 2;
                chanceBox = new EditBox(font, x, ay, half, 20, Component.literal("Chance 0.0–1.0")); chanceBox.setHint(Component.literal("Chance 0.0–1.0")); chanceBox.setValue(Double.toString(decimal(edit, "chance", 1.0D))); addRenderableWidget(chanceBox);
                addRenderableWidget(Button.builder(Component.literal("Copy input NBT: " + onOff(copyNbt)), b -> { copyNbt = !copyNbt; b.setMessage(Component.literal("Copy input NBT: " + onOff(copyNbt))); })
                        .bounds(x + half + 6, ay, half, 20).build()); ay += 28;
                actionbarBox = new EditBox(font, x, ay, fw, 20, Component.literal("Optional action bar message")); actionbarBox.setHint(Component.literal("Optional action bar message")); actionbarBox.setMaxLength(2048); actionbarBox.setValue(string(edit, "actionbar", "")); addRenderableWidget(actionbarBox); ay += 28;
                addRenderableWidget(Button.builder(Component.literal("Output selection: " + (weighted ? "WEIGHTED (one result is rolled)" : "ALL CONFIGURED OUTPUTS")), b -> { syncHeader(); toggleWeighted(); rebuild(); })
                        .bounds(x, ay, fw, 20).build()); ay += 26;
                buttonY = ay + 8;
            } else {
                buttonY = advancedY + 34;
            }
            buttonY = Math.min(height - 29, buttonY);
            addRenderableWidget(Button.builder(Component.literal("Apply Recipe"), b -> save()).bounds(x, buttonY, (fw - 6) / 2, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> goBack()).bounds(x + (fw - 6) / 2 + 6, buttonY, (fw - 6) / 2, 20).build());
        }

        private void buildItemColumn(int x, int y, int width, boolean intake) {
            JsonArray values = intake ? array(edit, "item_inputs") : outputArray();
            int offset = intake ? intakeOffset : outputOffset;
            int maxVisible = 4;
            addRenderableWidget(Button.builder(Component.literal((intake ? "INTAKE ITEMS" : weighted ? "WEIGHTED RESULTS" : "OUTPUT ITEMS") + "  (" + values.size() + ")"), b -> {})
                    .bounds(x, y, width - 74, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> openItemPicker(id -> {
                JsonObject value = new JsonObject(); value.addProperty("item", id); value.addProperty("count", 1); if (!intake && weighted) value.addProperty("weight", 1); values.add(value); Minecraft.getInstance().setScreen(this);
            }, this)).bounds(x + width - 68, y, 30, 20).build());
            addRenderableWidget(Button.builder(Component.literal("↕"), b -> {
                int max = Math.max(0, values.size() - maxVisible);
                if (intake) intakeOffset = max == 0 ? 0 : (intakeOffset + 1) % (max + 1);
                else outputOffset = max == 0 ? 0 : (outputOffset + 1) % (max + 1);
                rebuild();
            }).bounds(x + width - 34, y, 34, 20).build());
            int rowY = y + 27;
            for (int i = offset; i < Math.min(values.size(), offset + maxVisible); i++) {
                JsonElement element = values.get(i);
                if (!element.isJsonObject()) continue;
                JsonObject value = element.getAsJsonObject();
                int row = i - offset;
                int yy = rowY + row * 29;
                String id = string(value, "item", "minecraft:air");
                renders.add(new ItemRender(id, x + 4, yy + 2));
                int controls = weighted && !intake ? 178 : 128;
                int itemW = Math.max(62, width - controls);
                addRenderableWidget(Button.builder(Component.literal("    " + compact(itemName(id), Math.max(10, itemW / 6))), b -> openItemPicker(selected -> { value.addProperty("item", selected); Minecraft.getInstance().setScreen(this); }, this))
                        .bounds(x, yy, itemW, 22).build());
                int cx = x + itemW + 4;
                addRenderableWidget(Button.builder(Component.literal("−"), b -> { value.addProperty("count", Math.max(1, integer(value, "count", 1) - 1)); rebuild(); })
                        .bounds(cx, yy, 24, 22).build()); cx += 27;
                Button count = addRenderableWidget(Button.builder(Component.literal(Integer.toString(integer(value, "count", 1))), b -> {})
                        .bounds(cx, yy, 30, 22).build()); count.active = false; cx += 33;
                addRenderableWidget(Button.builder(Component.literal("+"), b -> { value.addProperty("count", integer(value, "count", 1) + 1); rebuild(); })
                        .bounds(cx, yy, 24, 22).build()); cx += 27;
                if (weighted && !intake) {
                    addRenderableWidget(Button.builder(Component.literal("W−"), b -> { value.addProperty("weight", Math.max(1, integer(value, "weight", 1) - 1)); rebuild(); })
                            .bounds(cx, yy, 30, 22).build()); cx += 33;
                    addRenderableWidget(Button.builder(Component.literal("W" + integer(value, "weight", 1)), b -> { value.addProperty("weight", integer(value, "weight", 1) + 1); rebuild(); })
                            .bounds(cx, yy, 38, 22).build()); cx += 41;
                }
                addRenderableWidget(Button.builder(Component.literal("X"), b -> { removeIdentity(values, value); if (intake) intakeOffset = Math.max(0, Math.min(intakeOffset, values.size() - maxVisible)); else outputOffset = Math.max(0, Math.min(outputOffset, values.size() - maxVisible)); rebuild(); })
                        .bounds(Math.min(x + width - 28, cx), yy, 28, 22).build());
            }
        }

        private String itemName(String id) {
            try {
                Item item = BuiltInRegistries.ITEM.getValue(ResourceLocation.parse(id));
                return item == null || item == Items.AIR ? id : new ItemStack(item).getHoverName().getString();
            } catch (Exception ignored) { return id; }
        }

        private JsonArray outputArray() {
            String key = weighted ? "weighted_item_outputs" : "item_outputs";
            return array(edit, key);
        }

        private void toggleWeighted() {
            if (weighted) {
                JsonArray old = array(edit, "weighted_item_outputs");
                JsonArray normal = new JsonArray();
                for (JsonElement element : old) {
                    if (!element.isJsonObject()) continue;
                    JsonObject copy = element.getAsJsonObject().deepCopy(); copy.remove("weight"); normal.add(copy);
                }
                edit.add("item_outputs", normal); edit.remove("weighted_item_outputs"); weighted = false;
            } else {
                JsonArray old = array(edit, "item_outputs");
                JsonArray weightedValues = new JsonArray();
                for (JsonElement element : old) {
                    if (!element.isJsonObject()) continue;
                    JsonObject copy = element.getAsJsonObject().deepCopy(); copy.addProperty("weight", 1); weightedValues.add(copy);
                }
                edit.add("weighted_item_outputs", weightedValues); edit.remove("item_outputs"); weighted = true;
            }
            outputOffset = 0;
        }

        private void cycleSetting() {
            int index = SETTINGS.indexOf(setting);
            setting = SETTINGS.get((Math.max(0, index) + 1) % SETTINGS.size());
        }

        private String settingLabel() {
            return switch (setting) {
                case "1_to_1" -> "1:1";
                case "very_fine" -> "Very Fine";
                default -> setting.substring(0, 1).toUpperCase(Locale.ROOT) + setting.substring(1);
            };
        }

        private void cycleSource() {
            List<String> sources = owner.sourceKeys();
            int index = sources.indexOf(targetSource);
            targetSource = sources.get((Math.max(0, index) + 1) % sources.size());
        }

        private String sourceName(String source) {
            return source.equals(ConfigCenterService.RECIPE_MAIN) ? "main config" : source.substring(ConfigCenterService.RECIPE_PREFIX.length());
        }

        private void syncHeader() {
            if (idBox != null) edit.addProperty("id", idBox.getValue().trim());
            edit.addProperty("setting", setting);
            edit.addProperty("enabled", enabled);
            if (advanced && chanceBox != null) {
                edit.addProperty("chance", Math.max(0.0D, Math.min(1.0D, parseDouble(chanceBox.getValue(), 1.0D))));
                edit.addProperty("copy_input_nbt", copyNbt);
                setOrRemove(edit, "actionbar", actionbarBox.getValue());
            } else {
                if (!edit.has("chance")) edit.addProperty("chance", 1.0D);
                edit.addProperty("copy_input_nbt", copyNbt);
            }
        }

        private void save() {
            syncHeader();
            String id = string(edit, "id", "");
            try { ResourceLocation.parse(id); } catch (Exception ignored) { idBox.setTextColor(BAD); return; }
            if (!validItems(array(edit, "item_inputs")) || !validItems(outputArray())) return;
            boolean hasInput = array(edit, "item_inputs").size() > 0 || (edit.has("entity_inputs") && edit.get("entity_inputs").isJsonArray() && edit.getAsJsonArray("entity_inputs").size() > 0);
            boolean hasOutput = outputArray().size() > 0 || (edit.has("entity_outputs") && edit.get("entity_outputs").isJsonArray() && edit.getAsJsonArray("entity_outputs").size() > 0);
            if (!hasInput || !hasOutput) { homeNotice = "A recipe needs at least one intake and one output."; return; }
            if (weighted) edit.remove("item_outputs"); else edit.remove("weighted_item_outputs");
            owner.apply(originalSource, original, targetSource, edit.deepCopy());
            Minecraft.getInstance().setScreen(owner);
        }

        private boolean validItems(JsonArray values) {
            for (JsonElement element : values) {
                if (!element.isJsonObject()) return false;
                try { new ResourceLocation(string(element.getAsJsonObject(), "item", "")); }
                catch (Exception ignored) { return false; }
            }
            return true;
        }

        private void rebuild() { clearWidgets(); init(); }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            int w = Math.min(790, width - 10); int h = Math.min(480, height - 10); int x = left(width, w); int y = Math.max(5, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            graphics.drawString(font, "Choose items by search. Use + to add as many intake or output entries as required.", x + 12, y + 28, MUTED, false);
            for (ItemRender render : renders) {
                try {
                    Item item = BuiltInRegistries.ITEM.getValue(new ResourceLocation(render.id()));
                    if (item != null && item != Items.AIR) graphics.renderItem(new ItemStack(item), render.x(), render.y());
                } catch (Exception ignored) {
                }
            }
            if (advanced) {
                int entityInputs = edit.has("entity_inputs") && edit.get("entity_inputs").isJsonArray() ? edit.getAsJsonArray("entity_inputs").size() : 0;
                int entityOutputs = edit.has("entity_outputs") && edit.get("entity_outputs").isJsonArray() ? edit.getAsJsonArray("entity_outputs").size() : 0;
                if (entityInputs + entityOutputs > 0) graphics.drawString(font, "Preserving " + entityInputs + " entity intake and " + entityOutputs + " entity output rule(s) from JSON.", x + 14, y + h - 42, WARN, false);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }
}
