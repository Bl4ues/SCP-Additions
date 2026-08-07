package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.RoombaSpawnConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Adds the Roomba floor whitelist editor to the native Configuration Center. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RoombaConfigCenterEnhancements {
    private static final String OLD_HUB_TITLE = "Inventory, Equipment & Codex";
    private static final String HUB_TITLE = "Items, Entities & Codex";

    private RoombaConfigCenterEnhancements() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        String className = event.getScreen().getClass().getName();
        if (className.endsWith("ConfigCenterClient$HomeScreen")) {
            renameWidget(event, OLD_HUB_TITLE, HUB_TITLE);
            return;
        }
        if (!className.endsWith("ConfigCenterClient$InventoryHubScreen")) return;

        trySetField(event.getScreen(), "screenTitle", HUB_TITLE);
        try {
            JsonObject working = (JsonObject) getField(event.getScreen(), "working");
            AbstractWidget codex = findWidget(event, "Codex Documents");
            AbstractWidget save = findWidget(event, "Save All");
            AbstractWidget back = findWidget(event, "Back");
            if (codex == null || working == null) return;

            int buttonY = codex.getY() + 32;
            if (save != null) save.setY(save.getY() + 32);
            if (back != null) back.setY(back.getY() + 32);
            event.addListener(Button.builder(ScpFonts.roboto("Roomba Spawning"),
                            button -> Minecraft.getInstance().setScreen(
                                    new RoombaSpawningScreen(event.getScreen(), working)))
                    .bounds(codex.getX(), buttonY, codex.getWidth(), codex.getHeight())
                    .build());
        } catch (ReflectiveOperationException ignored) {
            // Non-fatal if a future Configuration Center revision changes internals.
        }
    }

    private static void renameWidget(ScreenEvent.Init.Post event, String from, String to) {
        AbstractWidget widget = findWidget(event, from);
        if (widget != null) widget.setMessage(ScpFonts.roboto(to));
    }

    private static AbstractWidget findWidget(ScreenEvent.Init.Post event, String label) {
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget
                    && label.equals(widget.getMessage().getString())) return widget;
        }
        return null;
    }

    private static Object getField(Object instance, String name)
            throws ReflectiveOperationException {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void trySetField(Object instance, String name, Object value) {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(instance, value);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private record FloorEntry(ResourceLocation id, boolean integrated,
            boolean enabled) {
    }

    private abstract static class RoombaScreen extends Screen {
        protected static final int PANEL = 0xFF111317;
        protected static final int HEADER = 0xFF24282E;
        protected static final int ROW = 0xFF081022;
        protected static final int ROW_ALT = 0xFF0D1628;
        protected static final int BORDER = 0xFF46536C;
        protected static final int TEXT = 0xFFF7F8FC;
        protected static final int MUTED = 0xFF9CA3AF;
        protected static final int ACCENT = 0xFFC59A2A;
        protected static final int GOOD = 0xFF79D58B;
        protected static final int BAD = 0xFFD46060;

        protected RoombaScreen(String title) {
            super(ScpFonts.roboto(title));
        }

        protected int panelWidth() {
            return Math.min(760, width - 28);
        }

        protected int panelHeight() {
            return Math.min(450, height - 24);
        }

        protected int panelX() {
            return Math.max(14, (width - panelWidth()) / 2);
        }

        protected int panelY() {
            return Math.max(12, (height - panelHeight()) / 2);
        }

        protected int visibleRows() {
            return Math.max(3, (panelHeight() - 150) / 40);
        }

        protected void drawPanel(GuiGraphics graphics, String title, String subtitle) {
            int x = panelX();
            int y = panelY();
            graphics.fill(x, y, x + panelWidth(), y + panelHeight(), PANEL);
            graphics.fill(x, y, x + panelWidth(), y + 34, HEADER);
            graphics.drawString(font, ScpFonts.montserrat(title),
                    x + 14, y + 10, TEXT, false);
            graphics.drawString(font, ScpFonts.roboto(subtitle),
                    x + 16, y + 39, MUTED, false);
        }

        protected void drawBlockIcon(GuiGraphics graphics, ResourceLocation id,
                int x, int y) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null || block == Blocks.AIR || block.asItem() == Items.AIR) return;
            ItemStack stack = new ItemStack(block.asItem());
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(font, stack, x, y);
        }

        protected String blockName(ResourceLocation id) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            return block == null || block == Blocks.AIR
                    ? id.toString() : block.getName().getString();
        }

        protected StyledButton button(int x, int y, int width, int height,
                String label, boolean primary, boolean danger, Runnable action) {
            StyledButton button = new StyledButton(x, y, width, height,
                    label, primary, danger, action);
            addRenderableWidget(button);
            return button;
        }

        protected static final class StyledButton extends AbstractButton {
            private final Runnable action;
            private final boolean primary;
            private final boolean danger;

            private StyledButton(int x, int y, int width, int height,
                    String label, boolean primary, boolean danger, Runnable action) {
                super(x, y, width, height, ScpFonts.roboto(label));
                this.action = action;
                this.primary = primary;
                this.danger = danger;
            }

            @Override
            public void onPress() {
                action.run();
            }

            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX,
                    int mouseY, float partialTick) {
                Font font = Minecraft.getInstance().font;
                boolean hovered = isHoveredOrFocused();
                int background = !active ? 0xFF1B1E26
                        : hovered ? 0xFF131E36 : ROW;
                int border = danger ? BAD : hovered ? 0xFF73809A : BORDER;
                int stripe = danger ? BAD : primary ? ACCENT
                        : hovered ? 0xFF8D711F : BORDER;
                int textColor = !active ? MUTED
                        : primary && !danger ? 0xFFE5D49A : TEXT;
                int right = getX() + getWidth();
                int bottom = getY() + getHeight();
                graphics.fill(getX(), getY(), right, bottom, background);
                graphics.fill(getX(), getY(), right, getY() + 1, border);
                graphics.fill(getX(), bottom - 1, right, bottom, border);
                graphics.fill(getX(), getY(), getX() + 1, bottom, border);
                graphics.fill(right - 1, getY(), right, bottom, border);
                graphics.fill(getX() + 1, getY() + 1,
                        getX() + (primary || danger || hovered ? 4 : 2),
                        bottom - 1, stripe);
                int textX = getX() + Math.max(5,
                        (getWidth() - font.width(getMessage())) / 2);
                int textY = getY() + Math.max(1, (getHeight() - 8) / 2);
                graphics.drawString(font, getMessage(), textX, textY,
                        textColor, false);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput output) {
                defaultButtonNarrationText(output);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static final class RoombaSpawningScreen extends RoombaScreen {
        private final Screen parent;
        private final JsonObject root;
        private final EditBox searchBox;
        private int scroll;
        private String notice = "";
        private boolean noticeGood;
        private boolean rebuilding;

        private RoombaSpawningScreen(Screen parent, JsonObject root) {
            super("Roomba Spawning");
            this.parent = parent;
            this.root = root;
            this.searchBox = new EditBox(Minecraft.getInstance().font,
                    0, 0, 100, 20, ScpFonts.roboto("Search floors"));
            searchBox.setMaxLength(128);
            searchBox.setHint(ScpFonts.roboto("Search configured floor blocks"));
        }

        @Override
        protected void init() {
            int x = panelX() + 16;
            int y = panelY();
            int listWidth = panelWidth() - 32;
            searchBox.setX(x);
            searchBox.setY(y + 58);
            searchBox.setWidth(listWidth - 142);
            searchBox.setResponder(value -> {
                if (!rebuilding) {
                    scroll = 0;
                    rebuild(true);
                }
            });
            addRenderableWidget(searchBox);
            button(x + listWidth - 134, y + 58, 134, 20,
                    "+ Add Floor", true, false,
                    () -> Minecraft.getInstance().setScreen(
                            new RoombaBlockPickerScreen(this, root)));
            button(x + listWidth - 108, y + panelHeight() - 30,
                    108, 20, "Back", false, false,
                    () -> Minecraft.getInstance().setScreen(parent));
            addRemoveButtons(x, y + 92, listWidth);
        }

        private void addRemoveButtons(int x, int listY, int listWidth) {
            List<FloorEntry> entries = filteredEntries();
            scroll = Math.min(scroll, Math.max(0, entries.size() - visibleRows()));
            int end = Math.min(entries.size(), scroll + visibleRows());
            for (int index = scroll; index < end; index++) {
                FloorEntry entry = entries.get(index);
                int row = index - scroll;
                int rowY = listY + row * 40 + 6;
                int toggleWidth = entry.integrated() ? 82 : 70;
                button(x + listWidth - (entry.integrated() ? toggleWidth : 126),
                        rowY, toggleWidth, 24,
                        entry.enabled() ? "Disable" : "Enable",
                        !entry.enabled(), false,
                        () -> toggleEntry(entry));
                if (!entry.integrated()) {
                    button(x + listWidth - 50, rowY,
                            50, 24, "X", false, true,
                            () -> removeEntry(entry.id()));
                }
            }
        }

        private List<FloorEntry> filteredEntries() {
            String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
            List<FloorEntry> entries = new ArrayList<>();
            for (ResourceLocation id : RoombaSpawnConfig.integratedBlocks()) {
                JsonElement configured = findEntry(id);
                entries.add(new FloorEntry(id, true,
                        configured == null || entryEnabled(configured)));
            }
            for (JsonElement element : customArray()) {
                ResourceLocation id = entryId(element);
                if (id == null || RoombaSpawnConfig.integratedBlocks().contains(id)) {
                    continue;
                }
                entries.add(new FloorEntry(id, false, entryEnabled(element)));
            }
            if (!query.isBlank()) {
                entries.removeIf(entry -> !entry.id().toString()
                        .toLowerCase(Locale.ROOT).contains(query)
                        && !blockName(entry.id()).toLowerCase(Locale.ROOT)
                        .contains(query)
                        && !(entry.enabled() ? "enabled" : "disabled").contains(query));
            }
            return entries;
        }

        private JsonArray customArray() {
            if (!root.has(RoombaSpawnConfig.CONFIG_KEY)
                    || !root.get(RoombaSpawnConfig.CONFIG_KEY).isJsonArray()) {
                root.add(RoombaSpawnConfig.CONFIG_KEY, new JsonArray());
            }
            return root.getAsJsonArray(RoombaSpawnConfig.CONFIG_KEY);
        }

        private ResourceLocation entryId(JsonElement element) {
            if (element == null || element.isJsonNull()) return null;
            String value = "";
            if (element.isJsonPrimitive()) {
                value = element.getAsString();
            } else if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("id") && object.get("id").isJsonPrimitive()) {
                    value = object.get("id").getAsString();
                }
            }
            return ResourceLocation.tryParse(value.trim());
        }

        private boolean entryEnabled(JsonElement element) {
            if (element == null || !element.isJsonObject()) return true;
            JsonObject object = element.getAsJsonObject();
            if (!object.has("enabled")) return true;
            try {
                return object.get("enabled").getAsBoolean();
            } catch (Exception ignored) {
                return true;
            }
        }

        private JsonElement findEntry(ResourceLocation id) {
            if (id == null) return null;
            for (JsonElement element : customArray()) {
                if (id.equals(entryId(element))) return element;
            }
            return null;
        }

        private List<String> customIds() {
            List<String> ids = new ArrayList<>();
            for (JsonElement element : customArray()) {
                ResourceLocation id = entryId(element);
                if (id != null) ids.add(id.toString());
            }
            return ids;
        }

        private Set<ResourceLocation> configuredIds() {
            Set<ResourceLocation> ids = new LinkedHashSet<>(
                    RoombaSpawnConfig.integratedBlocks());
            for (String value : customIds()) {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id != null) ids.add(id);
            }
            return ids;
        }

        private void toggleEntry(FloorEntry entry) {
            JsonArray values = customArray();
            JsonElement existing = findEntry(entry.id());
            boolean next = !entry.enabled();

            if (entry.integrated() && next) {
                // Re-enabling an integrated floor removes the tombstone and
                // resumes following the bundled default in future updates.
                if (existing != null) values.remove(existing);
            } else if (existing != null && existing.isJsonObject()) {
                existing.getAsJsonObject().addProperty("enabled", next);
            } else {
                if (existing != null) values.remove(existing);
                JsonObject state = new JsonObject();
                state.addProperty("id", entry.id().toString());
                state.addProperty("enabled", next);
                values.add(state);
            }
            notice = (next ? "Enabled " : "Disabled ") + blockName(entry.id());
            noticeGood = next;
            rebuild(false);
        }

        private void removeEntry(ResourceLocation id) {
            JsonArray entries = customArray();
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (id.equals(entryId(entries.get(i)))) entries.remove(i);
            }
            notice = "Removed " + blockName(id);
            noticeGood = true;
            rebuild(false);
        }

        private void added(ResourceLocation id) {
            notice = "Added " + blockName(id);
            noticeGood = true;
            searchBox.setValue("");
            scroll = 0;
        }

        private void rebuild(boolean keepFocus) {
            rebuilding = true;
            clearWidgets();
            init();
            rebuilding = false;
            if (keepFocus) {
                setFocused(searchBox);
                searchBox.setFocused(true);
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int max = Math.max(0, filteredEntries().size() - visibleRows());
            int next = Math.max(0, Math.min(max,
                    scroll + (delta < 0 ? 1 : -1)));
            if (next == scroll) return super.mouseScrolled(mouseX, mouseY, delta);
            scroll = next;
            rebuild(false);
            return true;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            renderBackground(graphics);
            drawPanel(graphics, "Roomba Spawning",
                    "Choose which floor blocks allow natural Roomba encounters.");
            int x = panelX() + 16;
            int listY = panelY() + 92;
            int listWidth = panelWidth() - 32;
            List<FloorEntry> entries = filteredEntries();
            int end = Math.min(entries.size(), scroll + visibleRows());
            for (int index = scroll; index < end; index++) {
                FloorEntry entry = entries.get(index);
                int row = index - scroll;
                int rowY = listY + row * 40;
                int rowColor = entry.enabled()
                        ? (row % 2 == 0 ? ROW : ROW_ALT) : 0xFF11151D;
                int rowBorder = entry.enabled() ? BORDER : 0xFF30343C;
                int mainText = entry.enabled() ? TEXT : 0xFF777E89;
                int secondaryText = entry.enabled() ? MUTED : 0xFF555B64;
                graphics.fill(x, rowY, x + listWidth, rowY + 36, rowColor);
                graphics.fill(x, rowY, x + listWidth, rowY + 1, rowBorder);
                graphics.fill(x, rowY + 35, x + listWidth, rowY + 36, rowBorder);
                graphics.fill(x, rowY, x + 4, rowY + 36,
                        entry.enabled() ? (entry.integrated() ? GOOD : ACCENT)
                                : 0xFF50555D);
                drawBlockIcon(graphics, entry.id(), x + 10, rowY + 10);
                if (!entry.enabled()) {
                    graphics.fill(x + 9, rowY + 9, x + 27, rowY + 27,
                            0x88000000);
                }
                graphics.drawString(font, ScpFonts.roboto(blockName(entry.id())),
                        x + 34, rowY + 7, mainText, false);
                graphics.drawString(font, ScpFonts.roboto(entry.id().toString()),
                        x + 34, rowY + 20, secondaryText, false);
                String badge = entry.enabled()
                        ? (entry.integrated() ? "INTEGRATED" : "CUSTOM")
                        : (entry.integrated() ? "INTEGRATED · DISABLED"
                                : "CUSTOM · DISABLED");
                int badgeColor = entry.enabled()
                        ? (entry.integrated() ? GOOD : ACCENT) : 0xFF666B73;
                int controlsReserve = entry.integrated() ? 92 : 136;
                graphics.drawString(font, ScpFonts.roboto(badge),
                        x + listWidth - controlsReserve - font.width(badge),
                        rowY + 14, badgeColor, false);
            }
            if (entries.isEmpty()) {
                graphics.drawCenteredString(font,
                        ScpFonts.roboto("No configured floors match the search."),
                        panelX() + panelWidth() / 2, listY + 28, MUTED);
            }
            int footerY = panelY() + panelHeight() - 24;
            graphics.drawString(font,
                    ScpFonts.roboto(entries.size() + " configured floor(s)"),
                    x, footerY, MUTED, false);
            if (!notice.isBlank()) {
                graphics.drawString(font, ScpFonts.roboto(notice),
                        x, footerY - 15, noticeGood ? GOOD : BAD, false);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }
    }

    private static final class RoombaBlockPickerScreen extends RoombaScreen {
        private final RoombaSpawningScreen parent;
        private final JsonObject root;
        private final EditBox searchBox;
        private int scroll;
        private boolean rebuilding;

        private RoombaBlockPickerScreen(RoombaSpawningScreen parent,
                JsonObject root) {
            super("Add Roomba Spawn Floor");
            this.parent = parent;
            this.root = root;
            this.searchBox = new EditBox(Minecraft.getInstance().font,
                    0, 0, 100, 20, ScpFonts.roboto("Search blocks"));
            searchBox.setMaxLength(128);
            searchBox.setHint(ScpFonts.roboto(
                    "Search block name or namespace:block_id"));
        }

        @Override
        protected void init() {
            int x = panelX() + 16;
            int y = panelY();
            int listWidth = panelWidth() - 32;
            searchBox.setX(x);
            searchBox.setY(y + 58);
            searchBox.setWidth(listWidth);
            searchBox.setResponder(value -> {
                if (!rebuilding) {
                    scroll = 0;
                    rebuild(true);
                }
            });
            addRenderableWidget(searchBox);
            button(x + listWidth - 108, y + panelHeight() - 30,
                    108, 20, "Back", false, false,
                    () -> Minecraft.getInstance().setScreen(parent));
            addButtons(x, y + 92, listWidth);
        }

        private void addButtons(int x, int listY, int listWidth) {
            List<ResourceLocation> entries = candidates();
            scroll = Math.min(scroll, Math.max(0, entries.size() - visibleRows()));
            int end = Math.min(entries.size(), scroll + visibleRows());
            for (int index = scroll; index < end; index++) {
                ResourceLocation id = entries.get(index);
                int row = index - scroll;
                button(x + listWidth - 78, listY + row * 40 + 6,
                        78, 24, "Add", true, false, () -> addEntry(id));
            }
        }

        private List<ResourceLocation> candidates() {
            String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
            Set<ResourceLocation> configured = parent.configuredIds();
            List<ResourceLocation> ids = new ArrayList<>();
            for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
                Block block = ForgeRegistries.BLOCKS.getValue(id);
                if (block == null || block == Blocks.AIR || block.asItem() == Items.AIR
                        || configured.contains(id)) continue;
                String name = block.getName().getString();
                if (!query.isBlank()
                        && !id.toString().toLowerCase(Locale.ROOT).contains(query)
                        && !name.toLowerCase(Locale.ROOT).contains(query)) continue;
                ids.add(id);
            }
            ids.sort(Comparator.comparing(
                            (ResourceLocation id) -> blockName(id).toLowerCase(Locale.ROOT))
                    .thenComparing(ResourceLocation::toString));
            return ids;
        }

        private void addEntry(ResourceLocation id) {
            if (parent.configuredIds().contains(id)) return;
            JsonArray entries;
            if (!root.has(RoombaSpawnConfig.CONFIG_KEY)
                    || !root.get(RoombaSpawnConfig.CONFIG_KEY).isJsonArray()) {
                entries = new JsonArray();
                root.add(RoombaSpawnConfig.CONFIG_KEY, entries);
            } else {
                entries = root.getAsJsonArray(RoombaSpawnConfig.CONFIG_KEY);
            }
            entries.add(new JsonPrimitive(id.toString()));
            parent.added(id);
            Minecraft.getInstance().setScreen(parent);
        }

        private void rebuild(boolean keepFocus) {
            rebuilding = true;
            clearWidgets();
            init();
            rebuilding = false;
            if (keepFocus) {
                setFocused(searchBox);
                searchBox.setFocused(true);
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int max = Math.max(0, candidates().size() - visibleRows());
            int next = Math.max(0, Math.min(max,
                    scroll + (delta < 0 ? 1 : -1)));
            if (next == scroll) return super.mouseScrolled(mouseX, mouseY, delta);
            scroll = next;
            rebuild(false);
            return true;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            renderBackground(graphics);
            drawPanel(graphics, "Add Roomba Spawn Floor",
                    "Search the block registry and add a floor to the whitelist.");
            int x = panelX() + 16;
            int listY = panelY() + 92;
            int listWidth = panelWidth() - 32;
            List<ResourceLocation> entries = candidates();
            int end = Math.min(entries.size(), scroll + visibleRows());
            for (int index = scroll; index < end; index++) {
                ResourceLocation id = entries.get(index);
                int row = index - scroll;
                int rowY = listY + row * 40;
                graphics.fill(x, rowY, x + listWidth, rowY + 36,
                        row % 2 == 0 ? ROW : ROW_ALT);
                graphics.fill(x, rowY, x + listWidth, rowY + 1, BORDER);
                graphics.fill(x, rowY + 35, x + listWidth, rowY + 36, BORDER);
                graphics.fill(x, rowY, x + 4, rowY + 36, ACCENT);
                drawBlockIcon(graphics, id, x + 10, rowY + 10);
                graphics.drawString(font, ScpFonts.roboto(blockName(id)),
                        x + 34, rowY + 7, TEXT, false);
                graphics.drawString(font, ScpFonts.roboto(id.toString()),
                        x + 34, rowY + 20, MUTED, false);
            }
            if (entries.isEmpty()) {
                graphics.drawCenteredString(font,
                        ScpFonts.roboto("No available blocks match the search."),
                        panelX() + panelWidth() / 2, listY + 28, MUTED);
            }
            graphics.drawString(font,
                    ScpFonts.roboto(entries.size() + " available block(s)"),
                    x, panelY() + panelHeight() - 24, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }
    }
}
