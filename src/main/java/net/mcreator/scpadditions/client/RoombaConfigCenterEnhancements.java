package net.mcreator.scpadditions.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import java.util.List;

/** Adds Roomba encounter configuration to the existing Configuration Center. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RoombaConfigCenterEnhancements {
    private static final String OLD_HUB_TITLE =
            "Inventory, Equipment & Codex";
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
        if (!className.endsWith("ConfigCenterClient$InventoryHubScreen")) {
            return;
        }

        trySetField(event.getScreen(), "screenTitle", HUB_TITLE);
        try {
            JsonObject working = (JsonObject) getField(
                    event.getScreen(), "working");
            AbstractWidget codex = findWidget(event, "Codex Documents");
            AbstractWidget save = findWidget(event, "Save All");
            AbstractWidget back = findWidget(event, "Back");
            if (codex == null || working == null) {
                return;
            }

            int buttonY = codex.getY() + 32;
            if (save != null) save.setY(save.getY() + 32);
            if (back != null) back.setY(back.getY() + 32);
            event.addListener(Button.builder(Component.literal(
                            "Roomba Spawning"), button ->
                            Minecraft.getInstance().setScreen(
                                    new RoombaSpawningScreen(
                                            event.getScreen(), working)))
                    .bounds(codex.getX(), buttonY,
                            codex.getWidth(), codex.getHeight()).build());
        } catch (ReflectiveOperationException ignored) {
            // Remains non-fatal if the private Configuration Center layout
            // changes in a later version.
        }
    }

    private static void renameWidget(ScreenEvent.Init.Post event,
            String from, String to) {
        AbstractWidget widget = findWidget(event, from);
        if (widget != null) {
            widget.setMessage(Component.literal(to));
        }
    }

    private static AbstractWidget findWidget(ScreenEvent.Init.Post event,
            String label) {
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget
                    && label.equals(widget.getMessage().getString())) {
                return widget;
            }
        }
        return null;
    }

    private static Object getField(Object instance, String name)
            throws ReflectiveOperationException {
        Class<?> type = instance.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void trySetField(Object instance, String name,
            Object value) {
        Class<?> type = instance.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(instance, value);
                return;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
    }

    private static final class RoombaSpawningScreen extends Screen {
        private static final int PANEL = 0xEE111317;
        private static final int HEADER = 0xEE24282E;
        private static final int TEXT = 0xFFE8E8E8;
        private static final int MUTED = 0xFF9FA6AD;
        private static final int GOOD = 0xFF79D58B;
        private static final int BAD = 0xFFFF8B8B;

        private final Screen parent;
        private final JsonObject root;
        private final EditBox blockId;
        private int editingIndex = -1;
        private int scroll;
        private String notice = "";
        private boolean noticeGood;

        private RoombaSpawningScreen(Screen parent, JsonObject root) {
            super(Component.literal("Roomba Spawning"));
            this.parent = parent;
            this.root = root;
            this.blockId = new EditBox(Minecraft.getInstance().font,
                    0, 0, 100, 20, Component.literal("Block ID"));
        }

        @Override
        protected void init() {
            int panelWidth = Math.min(620, width - 20);
            int panelHeight = Math.min(390, height - 20);
            int panelX = Math.max(10, (width - panelWidth) / 2);
            int panelY = Math.max(10, (height - panelHeight) / 2);
            int innerX = panelX + 14;
            int contentWidth = panelWidth - 28;

            blockId.setX(innerX);
            blockId.setY(panelY + 58);
            blockId.setWidth(contentWidth - 128);
            blockId.setMaxLength(128);
            blockId.setHint(Component.literal(
                    "namespace:block_id"));
            addRenderableWidget(blockId);
            addRenderableWidget(Button.builder(Component.literal(
                            editingIndex >= 0 ? "Replace" : "Add Block"),
                    button -> applyEntry())
                    .bounds(innerX + contentWidth - 120,
                            panelY + 58, 120, 20).build());

            rebuildRows(panelX, panelY, panelWidth, panelHeight);
        }

        private void rebuildRows(int panelX, int panelY, int panelWidth,
                int panelHeight) {
            int x = panelX + 14;
            int listY = panelY + 118;
            int width = panelWidth - 28;
            List<String> custom = customIds();
            int visible = Math.max(3,
                    Math.min(8, (panelHeight - 190) / 24));
            int maxScroll = Math.max(0, custom.size() - visible);
            scroll = Math.min(scroll, maxScroll);

            for (int index = scroll;
                    index < Math.min(custom.size(), scroll + visible);
                    index++) {
                int row = index - scroll;
                int actualIndex = index;
                String id = custom.get(index);
                addRenderableWidget(Button.builder(Component.literal(id),
                        button -> beginEdit(actualIndex, id))
                        .bounds(x, listY + row * 24,
                                width - 64, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"),
                        button -> removeEntry(actualIndex))
                        .bounds(x + width - 58, listY + row * 24,
                                58, 20).build());
            }

            addRenderableWidget(Button.builder(Component.literal("Back"),
                    button -> Minecraft.getInstance().setScreen(parent))
                    .bounds(x + width - 84,
                            panelY + panelHeight - 28, 84, 20).build());
        }

        private void rebuildScreen() {
            clearWidgets();
            init();
        }

        private void beginEdit(int index, String id) {
            editingIndex = index;
            blockId.setValue(id);
            notice = "Editing " + id;
            noticeGood = true;
            rebuildScreen();
            blockId.setValue(id);
        }

        private void applyEntry() {
            String value = blockId.getValue().trim();
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id == null || !ForgeRegistries.BLOCKS.containsKey(id)
                    || ForgeRegistries.BLOCKS.getValue(id) == Blocks.AIR) {
                notice = "Unknown block ID";
                noticeGood = false;
                return;
            }
            if (RoombaSpawnConfig.integratedBlocks().contains(id)) {
                notice = "That floor is already integrated by SCP Additions";
                noticeGood = false;
                return;
            }

            JsonArray entries = customArray();
            for (int i = 0; i < entries.size(); i++) {
                if (i != editingIndex
                        && entries.get(i).isJsonPrimitive()
                        && value.equals(entries.get(i).getAsString())) {
                    notice = "That block is already listed";
                    noticeGood = false;
                    return;
                }
            }

            if (editingIndex >= 0 && editingIndex < entries.size()) {
                entries.set(editingIndex, new JsonPrimitive(value));
                notice = "Custom spawn floor updated";
            } else {
                entries.add(value);
                notice = "Custom spawn floor added";
            }
            noticeGood = true;
            editingIndex = -1;
            blockId.setValue("");
            rebuildScreen();
        }

        private void removeEntry(int index) {
            JsonArray entries = customArray();
            if (index >= 0 && index < entries.size()) {
                entries.remove(index);
            }
            editingIndex = -1;
            blockId.setValue("");
            notice = "Custom spawn floor removed";
            noticeGood = true;
            rebuildScreen();
        }

        private JsonArray customArray() {
            if (!root.has(RoombaSpawnConfig.CONFIG_KEY)
                    || !root.get(RoombaSpawnConfig.CONFIG_KEY).isJsonArray()) {
                root.add(RoombaSpawnConfig.CONFIG_KEY, new JsonArray());
            }
            return root.getAsJsonArray(RoombaSpawnConfig.CONFIG_KEY);
        }

        private List<String> customIds() {
            List<String> ids = new ArrayList<>();
            for (JsonElement element : customArray()) {
                if (element.isJsonPrimitive()) {
                    ids.add(element.getAsString());
                }
            }
            return ids;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY,
                double delta) {
            int panelHeight = Math.min(390, height - 20);
            int visible = Math.max(3,
                    Math.min(8, (panelHeight - 190) / 24));
            int max = Math.max(0, customIds().size() - visible);
            int next = Math.max(0, Math.min(max,
                    scroll + (delta < 0 ? 1 : -1)));
            if (next != scroll) {
                scroll = next;
                rebuildScreen();
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            renderBackground(graphics);
            int panelWidth = Math.min(620, width - 20);
            int panelHeight = Math.min(390, height - 20);
            int panelX = Math.max(10, (width - panelWidth) / 2);
            int panelY = Math.max(10, (height - panelHeight) / 2);
            graphics.fill(panelX, panelY, panelX + panelWidth,
                    panelY + panelHeight, PANEL);
            graphics.fill(panelX, panelY, panelX + panelWidth,
                    panelY + 26, HEADER);
            graphics.drawString(font, "Roomba Spawning",
                    panelX + 10, panelY + 9, TEXT, false);
            graphics.drawString(font,
                    "Integrated floors are always available and cannot be removed",
                    panelX + 14, panelY + 34, MUTED, false);

            int integratedY = panelY + 86;
            graphics.drawString(font, "Integrated: ", panelX + 14,
                    integratedY, MUTED, false);
            String integrated = "Blue Floor, Metal Floor, Concrete Floor";
            graphics.drawString(font, integrated, panelX + 72,
                    integratedY, GOOD, false);
            graphics.drawString(font, "Custom spawn floors",
                    panelX + 14, panelY + 104, TEXT, false);
            graphics.drawString(font,
                    "Use Save All in the previous screen to apply changes",
                    panelX + 14, panelY + panelHeight - 24, MUTED, false);
            if (!notice.isBlank()) {
                graphics.drawString(font, notice, panelX + 14,
                        panelY + panelHeight - 42,
                        noticeGood ? GOOD : BAD, false);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
