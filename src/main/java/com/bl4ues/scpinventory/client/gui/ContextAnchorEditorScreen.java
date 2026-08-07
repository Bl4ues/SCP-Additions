package com.bl4ues.scpinventory.client.gui;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.network.ContextConfigDeletePacket;
import com.bl4ues.scpinventory.network.ContextConfigOpenPacket;
import com.bl4ues.scpinventory.network.ContextConfigSavePacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Polished in-world editor for default and item-specific interactions. */
public final class ContextAnchorEditorScreen extends Screen {
    private static final Gson GSON = new Gson();
    private static final int PANEL_W = 350;
    private static final int PANEL_H = 430;
    private static final int MARGIN = 10;

    private static final int NAVY = 0xF000071F;
    private static final int NAVY_LIGHT = 0xE6141E42;
    private static final int FIELD = 0xFF080D1C;
    private static final int CONTROL = 0xFF111A31;
    private static final int CONTROL_HOVER = 0xFF192744;
    private static final int BORDER = 0xFF46536C;
    private static final int BORDER_HOVER = 0xFF73809A;
    private static final int ACCENT = 0xFFC59A2A;
    private static final int ACCENT_TEXT = 0xFFE5D49A;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFFA9AFBA;
    private static final int SECTION = 0xFFD3D9E4;
    private static final int WARNING = 0xFFFFB35C;
    private static final int DANGER = 0xFFD46060;
    private static final int SUCCESS = 0xFF77C991;

    private final BlockPos pos;
    private final String targetId;
    private final boolean existing;
    private final boolean likelyRightClick;
    private final List<Draft> drafts = new ArrayList<>();

    private EditBox actionBox;
    private EditBox nameBox;
    private EditBox rangeBox;
    private EditBox iconBox;
    private EditBox requiredItemBox;
    private ButtonControl forgetButton;

    private int selectedDraft;
    private Page page = Page.PROMPT;
    private boolean confirmForget;

    public ContextAnchorEditorScreen(ContextConfigOpenPacket packet) {
        super(ScpFonts.roboto("Context Interaction Editor"));
        this.pos = packet.pos();
        this.targetId = packet.blockId();
        this.existing = packet.existing();
        this.likelyRightClick = packet.likelyRightClick();

        Draft base = new Draft();
        base.action = emptyTo(packet.action(), "Use");
        base.name = packet.name() == null ? "" : packet.name();
        base.showName = packet.showName();
        base.range = finite(packet.range(), 2.25D);
        base.allowE = packet.allowE();
        base.allowRightClick = packet.allowRightClick();
        base.allowOffscreen = packet.allowOffscreen();
        base.useItem = cleanUseItem(packet.useItem());
        base.icon = emptyTo(packet.icon(), base.useItem);
        base.requiredItem = clean(packet.requiredItem());
        base.clickFace = emptyTo(packet.clickFace(), "front");
        base.rotateWith = emptyTo(packet.rotateWith(), "none");
        base.anchorX = packet.anchorX();
        base.anchorY = packet.anchorY();
        base.anchorZ = packet.anchorZ();
        drafts.add(base);
        readVariants(packet.variantsJson(), base);
    }

    @Override
    protected void init() {
        actionBox = null;
        nameBox = null;
        rangeBox = null;
        iconBox = null;
        requiredItemBox = null;
        forgetButton = null;

        int left = panelLeft();
        int top = panelTop();
        int innerX = left + 16;
        int innerW = panelWidth() - 32;

        int navY = top + 39;
        addRenderableWidget(new ButtonControl(innerX, navY, 28, 20, "<",
                ButtonStyle.NEUTRAL, () -> switchDraft(-1)));
        addRenderableWidget(new ButtonControl(innerX + 34, navY,
                innerW - 132, 20, draftLabel(), ButtonStyle.NEUTRAL,
                () -> switchDraft(1)));
        addRenderableWidget(new ButtonControl(innerX + innerW - 92, navY,
                28, 20, ">", ButtonStyle.NEUTRAL,
                () -> switchDraft(1)));
        addRenderableWidget(new ButtonControl(innerX + innerW - 58, navY,
                58, 20, "+ Variant", ButtonStyle.PRIMARY,
                this::addVariant));

        int pageY = top + 65;
        addRenderableWidget(new ButtonControl(innerX, pageY, 76, 20,
                page == Page.PROMPT ? "Prompt *" : "Prompt",
                page == Page.PROMPT ? ButtonStyle.PRIMARY
                        : ButtonStyle.NEUTRAL,
                () -> setPage(Page.PROMPT)));
        addRenderableWidget(new ButtonControl(innerX + 82, pageY, 76, 20,
                page == Page.ANCHOR ? "Anchor *" : "Anchor",
                page == Page.ANCHOR ? ButtonStyle.PRIMARY
                        : ButtonStyle.NEUTRAL,
                () -> setPage(Page.ANCHOR)));
        addRenderableWidget(new ButtonControl(innerX + 164, pageY, 70, 20,
                "Duplicate", ButtonStyle.NEUTRAL, this::duplicateDraft));
        ButtonControl remove = new ButtonControl(innerX + 240, pageY,
                innerW - 240, 20, selectedDraft == 0 ? "Default"
                        : "Remove", selectedDraft == 0
                        ? ButtonStyle.NEUTRAL : ButtonStyle.DANGER,
                this::removeDraft);
        remove.active = selectedDraft > 0;
        addRenderableWidget(remove);

        if (page == Page.PROMPT) initPromptPage(innerX, top, innerW);
        else initAnchorPage(innerX, top, innerW);

        int bottomY = top + panelHeight() - 31;
        forgetButton = addRenderableWidget(new ButtonControl(innerX, bottomY,
                76, 22, "Forget", ButtonStyle.DANGER, this::forgetRule));
        addRenderableWidget(new ButtonControl(left + panelWidth() - 176,
                bottomY, 78, 22, "Save", ButtonStyle.PRIMARY, this::save));
        addRenderableWidget(new ButtonControl(left + panelWidth() - 90,
                bottomY, 74, 22, "Cancel", ButtonStyle.NEUTRAL,
                this::onClose));
    }

    private void initPromptPage(int x, int top, int width) {
        Draft draft = current();
        int step = compactLayout() ? 29 : 35;
        int first = top + 106;

        actionBox = configureField(new CenteredEditBox(font, x, first,
                width, 20, ScpFonts.roboto("Action")));
        actionBox.setHint(ScpFonts.roboto("Action text"));
        actionBox.setValue(draft.action);
        addRenderableWidget(actionBox);

        nameBox = configureField(new CenteredEditBox(font, x, first + step,
                width - 92, 20, ScpFonts.roboto("Display name")));
        nameBox.setHint(ScpFonts.roboto("Display name"));
        nameBox.setValue(draft.name.isBlank() ? fallbackName() : draft.name);
        addRenderableWidget(nameBox);
        addRenderableWidget(new ButtonControl(x + width - 84,
                first + step, 84, 20, nameDisplayText(draft),
                ButtonStyle.NEUTRAL, () -> {
            storePromptFields();
            current().showName = !current().showName;
            rebuild();
        }));

        rangeBox = configureField(new CenteredEditBox(font, x,
                first + step * 2, 64, 20, ScpFonts.roboto("Range")));
        rangeBox.setHint(ScpFonts.roboto("Range"));
        rangeBox.setValue(trimDouble(draft.range));
        addRenderableWidget(rangeBox);
        addRenderableWidget(new ButtonControl(x + 72, first + step * 2,
                112, 20, inputText(draft), ButtonStyle.NEUTRAL, () -> {
            storePromptFields();
            cycleInput(current());
            rebuild();
        }));
        addRenderableWidget(new ButtonControl(x + 192, first + step * 2,
                width - 192, 20, "Cycle icon", ButtonStyle.NEUTRAL,
                this::cycleIcon));

        iconBox = configureField(new CenteredEditBox(font, x,
                first + step * 3, width, 20, ScpFonts.roboto("Icon")));
        iconBox.setHint(ScpFonts.roboto(
                "Icon: pickup, card, config, or namespace:path"));
        iconBox.setValue(draft.icon);
        addRenderableWidget(iconBox);

        requiredItemBox = configureField(new CenteredEditBox(font, x,
                first + step * 4, width, 20,
                ScpFonts.roboto("Required item")));
        requiredItemBox.setHint(ScpFonts.roboto(
                "Required item registry ID, blank for any item"));
        requiredItemBox.setValue(draft.requiredItem);
        addRenderableWidget(requiredItemBox);

        int itemButtonsY = first + step * 5;
        addRenderableWidget(new ButtonControl(x, itemButtonsY,
                (width - 8) / 2, 20, "Use held item",
                ButtonStyle.NEUTRAL, this::useHeldItem));
        addRenderableWidget(new ButtonControl(x + (width - 8) / 2 + 8,
                itemButtonsY, (width - 8) / 2, 20, "Clear requirement",
                ButtonStyle.NEUTRAL, () -> {
            if (requiredItemBox != null) requiredItemBox.setValue("");
        }));
    }

    private void initAnchorPage(int x, int top, int width) {
        int step = compactLayout() ? 31 : 38;
        int first = top + 112;

        addRenderableWidget(new ButtonControl(x, first,
                (width - 8) / 2, 20, "Use crosshair",
                ButtonStyle.NEUTRAL, this::anchorFromCrosshair));
        addRenderableWidget(new ButtonControl(x + (width - 8) / 2 + 8,
                first, (width - 8) / 2, 20, "Set 2m ahead",
                ButtonStyle.NEUTRAL, this::anchorAhead));

        int axisY = first + step;
        int gap = 6;
        int axisWidth = (width - gap * 5) / 6;
        addRenderableWidget(axisButton(x, axisY, axisWidth, "X−",
                () -> nudge(-precision(), 0, 0)));
        addRenderableWidget(axisButton(x + axisWidth + gap, axisY,
                axisWidth, "X+", () -> nudge(precision(), 0, 0)));
        addRenderableWidget(axisButton(x + 2 * (axisWidth + gap), axisY,
                axisWidth, "Y−", () -> nudge(0, -precision(), 0)));
        addRenderableWidget(axisButton(x + 3 * (axisWidth + gap), axisY,
                axisWidth, "Y+", () -> nudge(0, precision(), 0)));
        addRenderableWidget(axisButton(x + 4 * (axisWidth + gap), axisY,
                axisWidth, "Z−", () -> nudge(0, 0, -precision())));
        addRenderableWidget(axisButton(x + 5 * (axisWidth + gap), axisY,
                axisWidth, "Z+", () -> nudge(0, 0, precision())));

        addRenderableWidget(new ButtonControl(x, first + step * 2,
                (width - 8) / 2, 20, faceText(current()),
                ButtonStyle.NEUTRAL, () -> {
            current().clickFace = next(current().clickFace, new String[]{
                    "front", "back", "player", "north", "south",
                    "east", "west", "up", "down"});
            rebuild();
        }));
        addRenderableWidget(new ButtonControl(x + (width - 8) / 2 + 8,
                first + step * 2, (width - 8) / 2, 20,
                rotateText(current()), ButtonStyle.NEUTRAL, () -> {
            current().rotateWith = next(current().rotateWith,
                    new String[]{"none", "auto", "facing",
                            "horizontal_facing", "axis"});
            rebuild();
        }));

        addRenderableWidget(new ButtonControl(x, first + step * 3,
                width, 20, offscreenText(current()), ButtonStyle.NEUTRAL,
                () -> {
            current().allowOffscreen = !current().allowOffscreen;
            rebuild();
        }));
    }

    private EditBox configureField(EditBox field) {
        field.setBordered(false);
        field.setTextColor(WHITE);
        field.setTextColorUneditable(MUTED);
        field.setMaxLength(32767);
        field.setFormatter((value, cursor) ->
                ScpFonts.roboto(value).getVisualOrderText());
        return field;
    }

    private ButtonControl axisButton(int x, int y, int width,
            String label, Runnable action) {
        return new ButtonControl(x, y, width, 20, label,
                ButtonStyle.NEUTRAL, action);
    }

    @Override
    public void tick() {
        super.tick();
        if (actionBox != null) actionBox.tick();
        if (nameBox != null) nameBox.tick();
        if (rangeBox != null) rangeBox.tick();
        if (iconBox != null) iconBox.tick();
        if (requiredItemBox != null) requiredItemBox.tick();
        spawnMarkerParticles();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        int left = panelLeft();
        int top = panelTop();
        int panelW = panelWidth();
        int panelH = panelHeight();

        graphics.fill(left, top, left + panelW, top + panelH, NAVY);
        graphics.fill(left, top, left + panelW, top + 31, NAVY_LIGHT);
        graphics.fill(left, top + 30, left + panelW, top + 31, ACCENT);
        outline(graphics, left, top, panelW, panelH, BORDER);

        graphics.drawString(font, ScpFonts.roboto("CONTEXT INTERACTION"),
                left + 16, top + 11, WHITE, false);
        graphics.drawString(font, ScpFonts.roboto(compact(targetId, 48)),
                left + panelW - 16 - font.width(compact(targetId, 48)),
                top + 11, ACCENT_TEXT, false);

        if (page == Page.PROMPT) renderPromptPage(graphics, left, top);
        else renderAnchorPage(graphics, left, top);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPromptPage(GuiGraphics graphics, int left, int top) {
        Draft draft = current();
        if (actionBox != null) drawField(graphics, actionBox);
        if (nameBox != null) drawField(graphics, nameBox);
        if (rangeBox != null) drawField(graphics, rangeBox);
        if (iconBox != null) drawField(graphics, iconBox);
        if (requiredItemBox != null) drawField(graphics, requiredItemBox);

        if (!compactLayout()) {
            int step = 35;
            int first = top + 106;
            graphics.drawString(font, ScpFonts.roboto("Action"),
                    left + 16, first - 10, MUTED, false);
            graphics.drawString(font, ScpFonts.roboto("Display name"),
                    left + 16, first + step - 10, MUTED, false);
            graphics.drawString(font, ScpFonts.roboto("Range and input"),
                    left + 16, first + step * 2 - 10, MUTED, false);
            graphics.drawString(font, ScpFonts.roboto("Prompt icon"),
                    left + 16, first + step * 3 - 10, MUTED, false);
            graphics.drawString(font, ScpFonts.roboto("Required item"),
                    left + 16, first + step * 4 - 10, MUTED, false);
        }

        int statusY = top + (compactLayout() ? 292 : 326);
        String status = requiredItemStatus(draft);
        int statusColor = draft.requiredItem.isBlank() ? MUTED
                : validRequiredItem(draft.requiredItem) ? SUCCESS : WARNING;
        graphics.drawString(font, ScpFonts.roboto(compact(status, 52)),
                left + 16, statusY, statusColor, false);
        if (selectedDraft == 0) {
            graphics.drawString(font, ScpFonts.roboto(
                            "A requirement here makes the default interaction item-only."),
                    left + 16, statusY + 13, MUTED, false);
        } else {
            graphics.drawString(font, ScpFonts.roboto(
                            "This alternate replaces the default while its item is held."),
                    left + 16, statusY + 13, MUTED, false);
        }
        if (!likelyRightClick && draft.allowRightClick) {
            graphics.drawString(font, ScpFonts.roboto(
                            "No native right-click handler was detected for this target."),
                    left + 16, statusY + 26, WARNING, false);
        }
    }

    private void renderAnchorPage(GuiGraphics graphics, int left, int top) {
        Draft draft = current();
        int boxY = top + (compactLayout() ? 246 : 276);
        graphics.fill(left + 16, boxY, left + panelWidth() - 16,
                boxY + 28, FIELD);
        outline(graphics, left + 16, boxY, panelWidth() - 32, 28, BORDER);
        graphics.drawString(font, ScpFonts.roboto("Local anchor"),
                left + 23, boxY + 10, MUTED, false);
        graphics.drawString(font, ScpFonts.roboto(
                        "X " + fmt(draft.anchorX) + "   Y "
                                + fmt(draft.anchorY) + "   Z "
                                + fmt(draft.anchorZ)),
                left + 105, boxY + 10, ACCENT_TEXT, false);
        graphics.drawString(font, ScpFonts.roboto(
                        "Arrows X/Y · PgUp/PgDn Z · wheel Y"),
                left + 16, boxY + 37, MUTED, false);
        graphics.drawString(font, ScpFonts.roboto(
                        "Ctrl: 0.01 · Shift: 0.10 · default: 0.05"),
                left + 16, boxY + 50, MUTED, false);
        graphics.drawString(font, ScpFonts.roboto(
                        "Each variant may use a separate prompt anchor."),
                left + 16, boxY + 70, SECTION, false);
    }

    private static void drawField(GuiGraphics graphics, EditBox field) {
        graphics.fill(field.getX() - 3, field.getY() - 2,
                field.getX() + field.getWidth() + 3,
                field.getY() + field.getHeight() + 2, FIELD);
        outline(graphics, field.getX() - 3, field.getY() - 2,
                field.getWidth() + 6, field.getHeight() + 4, BORDER);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (page == Page.ANCHOR && !isAnyFieldFocused()) {
            double amount = precision();
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                nudge(-amount, 0, 0); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                nudge(amount, 0, 0); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                nudge(0, amount, 0); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                nudge(0, -amount, 0); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
                nudge(0, 0, amount); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
                nudge(0, 0, -amount); return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (page == Page.ANCHOR) {
            nudge(0, delta * precision(), 0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void save() {
        storePromptFields();
        Draft base = drafts.get(0);
        JsonArray variants = new JsonArray();
        for (int index = 1; index < drafts.size(); index++) {
            variants.add(drafts.get(index).toJson());
        }
        ModNetwork.CHANNEL.sendToServer(new ContextConfigSavePacket(
                pos, targetId, base.action, base.name, base.showName,
                base.range, base.allowE, base.allowRightClick,
                base.allowOffscreen, base.useItem, base.icon,
                base.requiredItem, GSON.toJson(variants), base.clickFace,
                base.rotateWith, base.anchorX, base.anchorY, base.anchorZ));
        Minecraft.getInstance().setScreen(null);
    }

    private void forgetRule() {
        if (!confirmForget) {
            confirmForget = true;
            if (forgetButton != null) {
                forgetButton.setMessage(ScpFonts.roboto("Confirm"));
            }
            return;
        }
        ModNetwork.CHANNEL.sendToServer(
                new ContextConfigDeletePacket(pos, targetId));
        Minecraft.getInstance().setScreen(null);
    }

    private void setPage(Page next) {
        if (page == next) return;
        storePromptFields();
        page = next;
        rebuild();
    }

    private void switchDraft(int delta) {
        storePromptFields();
        selectedDraft = Math.floorMod(selectedDraft + delta, drafts.size());
        confirmForget = false;
        rebuild();
    }

    private void addVariant() {
        storePromptFields();
        Draft created = drafts.get(0).copy();
        created.interactionId = uniqueInteractionId("item_variant");
        String held = heldItemId();
        created.requiredItem = held;
        if ("scp_additions:screwdriver".equals(held)) {
            created.action = "Configure";
            created.icon = "config";
        }
        drafts.add(created);
        selectedDraft = drafts.size() - 1;
        page = Page.PROMPT;
        rebuild();
    }

    private void duplicateDraft() {
        storePromptFields();
        Draft copy = current().copy();
        copy.interactionId = uniqueInteractionId(selectedDraft == 0
                ? "variant" : current().interactionId + "_copy");
        drafts.add(copy);
        selectedDraft = drafts.size() - 1;
        rebuild();
    }

    private void removeDraft() {
        if (selectedDraft == 0) return;
        drafts.remove(selectedDraft);
        selectedDraft = Math.max(0,
                Math.min(selectedDraft - 1, drafts.size() - 1));
        rebuild();
    }

    private void cycleIcon() {
        storePromptFields();
        Draft draft = current();
        draft.icon = switch (draft.icon.toLowerCase(Locale.ROOT)) {
            case "pickup", "hand", "default" -> "card";
            case "card" -> "config";
            default -> "pickup";
        };
        draft.useItem = "card".equals(draft.icon) ? "card" : "hand";
        rebuild();
    }

    private void useHeldItem() {
        String held = heldItemId();
        if (held.isBlank()) return;
        requiredItemBox.setValue(held);
        if ("scp_additions:screwdriver".equals(held)
                && iconBox != null && (iconBox.getValue().isBlank()
                || "pickup".equalsIgnoreCase(iconBox.getValue()))) {
            iconBox.setValue("config");
        }
    }

    private void storePromptFields() {
        Draft draft = current();
        if (actionBox != null) draft.action = emptyTo(actionBox.getValue(), "Use");
        if (nameBox != null) draft.name = clean(nameBox.getValue());
        if (rangeBox != null) draft.range = parseRange(rangeBox.getValue(), draft.range);
        if (iconBox != null) {
            draft.icon = emptyTo(iconBox.getValue(), "pickup");
            draft.useItem = "card".equalsIgnoreCase(draft.icon)
                    ? "card" : "hand";
        }
        if (requiredItemBox != null) {
            draft.requiredItem = clean(requiredItemBox.getValue());
        }
    }

    private void cycleInput(Draft draft) {
        if (draft.allowE && draft.allowRightClick) {
            draft.allowRightClick = false;
        } else if (draft.allowE) {
            draft.allowE = false;
            draft.allowRightClick = true;
        } else {
            draft.allowE = true;
            draft.allowRightClick = true;
        }
    }

    private void readVariants(String json, Draft base) {
        if (json == null || json.isBlank()) return;
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonArray()) return;
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                drafts.add(Draft.fromVariant(element.getAsJsonObject(), base));
            }
        } catch (Exception ignored) {
            // Invalid external JSON remains visible through the base rule and
            // is rejected by server-side validation when saved.
        }
    }

    private String uniqueInteractionId(String seed) {
        String base = clean(seed).replaceAll("[^a-zA-Z0-9_./-]", "_");
        if (base.isBlank()) base = "variant";
        String candidate = base;
        int suffix = 2;
        while (containsInteractionId(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private boolean containsInteractionId(String key) {
        for (int i = 1; i < drafts.size(); i++) {
            if (key.equals(drafts.get(i).interactionId)) return true;
        }
        return false;
    }

    private String heldItemId() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return "";
        ItemStack stack = minecraft.player.getMainHandItem();
        if (stack.isEmpty()) stack = minecraft.player.getOffhandItem();
        if (stack.isEmpty()) return "";
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private String requiredItemStatus(Draft draft) {
        String value = requiredItemBox != null
                ? clean(requiredItemBox.getValue()) : draft.requiredItem;
        draft.requiredItem = value;
        if (value.isBlank()) return "Available with any held item.";
        try {
            ResourceLocation id = new ResourceLocation(value);
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null || item == Items.AIR) {
                return "Unknown item: " + value;
            }
            return "Requires " + new ItemStack(item).getHoverName().getString()
                    + " (" + value + ")";
        } catch (Exception ignored) {
            return "Invalid item registry ID: " + value;
        }
    }

    private boolean validRequiredItem(String value) {
        if (value == null || value.isBlank()) return true;
        try {
            ResourceLocation id = new ResourceLocation(value);
            Item item = ForgeRegistries.ITEMS.getValue(id);
            return item != null && item != Items.AIR;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void anchorFromCrosshair() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) {
            setAnchorFromWorld(hit.getLocation());
        }
    }

    private void anchorAhead() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        setAnchorFromWorld(minecraft.player.getEyePosition()
                .add(minecraft.player.getViewVector(1.0F)
                        .normalize().scale(2.0D)));
    }

    private void nudge(double x, double y, double z) {
        setAnchorFromWorld(resolveAnchorWorld().add(x, y, z));
    }

    private void setAnchorFromWorld(Vec3 world) {
        Vec3 centeredWorld = world.subtract(
                Vec3.atLowerCornerOf(pos).add(0.5D, 0.5D, 0.5D));
        Vec3 canonical = inverseRotate(centeredWorld, blockState());
        current().anchorX = round(canonical.x + 0.5D);
        current().anchorY = round(canonical.y + 0.5D);
        current().anchorZ = round(canonical.z + 0.5D);
    }

    private Vec3 resolveAnchorWorld() {
        Draft draft = current();
        Vec3 centered = new Vec3(draft.anchorX - 0.5D,
                draft.anchorY - 0.5D, draft.anchorZ - 0.5D);
        Vec3 rotated = rotate(centered, blockState());
        return Vec3.atLowerCornerOf(pos).add(0.5D, 0.5D, 0.5D)
                .add(rotated);
    }

    private double precision() {
        if (hasControlDown()) return 0.01D;
        if (hasShiftDown()) return 0.10D;
        return 0.05D;
    }

    private void spawnMarkerParticles() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Vec3 world = resolveAnchorWorld();
        minecraft.level.addParticle(ParticleTypes.END_ROD,
                world.x, world.y, world.z, 0.0D, 0.01D, 0.0D);
        minecraft.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                world.x, world.y, world.z, 0.0D, 0.0D, 0.0D);
    }

    private BlockState blockState() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? null
                : minecraft.level.getBlockState(pos);
    }

    private Vec3 rotate(Vec3 local, BlockState state) {
        Direction facing = resolveFacing(state, current().rotateWith);
        if (facing == null || facing == Direction.NORTH) return local;
        return switch (facing) {
            case SOUTH -> new Vec3(-local.x, local.y, -local.z);
            case EAST -> new Vec3(-local.z, local.y, local.x);
            case WEST -> new Vec3(local.z, local.y, -local.x);
            case UP, DOWN -> local;
            default -> local;
        };
    }

    private Vec3 inverseRotate(Vec3 local, BlockState state) {
        Direction facing = resolveFacing(state, current().rotateWith);
        if (facing == null || facing == Direction.NORTH) return local;
        return switch (facing) {
            case SOUTH -> new Vec3(-local.x, local.y, -local.z);
            case EAST -> new Vec3(local.z, local.y, -local.x);
            case WEST -> new Vec3(-local.z, local.y, local.x);
            case UP, DOWN -> local;
            default -> local;
        };
    }

    private Direction resolveFacing(BlockState state, String mode) {
        if (state == null || "none".equalsIgnoreCase(mode)) return null;
        if ("horizontal_facing".equalsIgnoreCase(mode)) {
            return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : null;
        }
        if ("facing".equalsIgnoreCase(mode)) {
            return state.hasProperty(BlockStateProperties.FACING)
                    ? state.getValue(BlockStateProperties.FACING) : null;
        }
        if ("axis".equalsIgnoreCase(mode)) return null;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return null;
    }

    private Draft current() {
        return drafts.get(Math.max(0,
                Math.min(selectedDraft, drafts.size() - 1)));
    }

    private String draftLabel() {
        if (selectedDraft == 0) return "Default interaction";
        Draft draft = current();
        String suffix = draft.requiredItem.isBlank() ? "Any item"
                : registryPath(draft.requiredItem);
        return "Variant " + selectedDraft + " · " + compact(suffix, 18);
    }

    private String fallbackName() {
        String path = targetId.contains(":")
                ? targetId.substring(targetId.indexOf(':') + 1) : targetId;
        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
        }
        return builder.isEmpty() ? targetId : builder.toString();
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private boolean isAnyFieldFocused() {
        return actionBox != null && actionBox.isFocused()
                || nameBox != null && nameBox.isFocused()
                || rangeBox != null && rangeBox.isFocused()
                || iconBox != null && iconBox.isFocused()
                || requiredItemBox != null && requiredItemBox.isFocused();
    }

    private int panelWidth() {
        return Math.max(220, Math.min(PANEL_W, width - MARGIN * 2));
    }

    private int panelHeight() {
        return Math.max(230, Math.min(PANEL_H, height - MARGIN * 2));
    }

    private int panelLeft() {
        return Math.max(MARGIN, width - panelWidth() - MARGIN);
    }

    private int panelTop() {
        return Math.max(MARGIN, (height - panelHeight()) / 2);
    }

    private boolean compactLayout() {
        return panelHeight() < 400;
    }

    private static String inputText(Draft draft) {
        if (draft.allowE && draft.allowRightClick) return "Input: Both";
        return draft.allowE ? "Input: E" : "Input: Right-click";
    }

    private static String offscreenText(Draft draft) {
        return draft.allowOffscreen ? "Off-screen: On" : "Off-screen: Off";
    }

    private static String nameDisplayText(Draft draft) {
        return draft.showName ? "Name: On" : "Name: Off";
    }

    private static String faceText(Draft draft) {
        return "Face: " + draft.clickFace;
    }

    private static String rotateText(Draft draft) {
        return "Rotate: " + draft.rotateWith;
    }

    private static String next(String current, String[] values) {
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(current)) {
                return values[(index + 1) % values.length];
            }
        }
        return values[0];
    }

    private static double parseRange(String value, double fallback) {
        try {
            return Math.max(0.25D, Double.parseDouble(value));
        } catch (Exception ignored) {
            return Math.max(0.25D, fallback);
        }
    }

    private static String registryPath(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private static String cleanUseItem(String value) {
        return "card".equalsIgnoreCase(value) ? "card" : "hand";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String emptyTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static String compact(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static double round(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
    }

    private static String trimDouble(double value) {
        String text = Double.toString(round(value));
        return text.endsWith(".0") ? text.substring(0, text.length() - 2)
                : text;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f",
                Mth.clamp(value, -999.0D, 999.0D));
    }

    private static void outline(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private enum Page { PROMPT, ANCHOR }
    private enum ButtonStyle { PRIMARY, NEUTRAL, DANGER }

    private static final class Draft {
        private String interactionId = "";
        private String action = "Use";
        private String name = "";
        private boolean showName = true;
        private double range = 2.25D;
        private boolean allowE = true;
        private boolean allowRightClick = true;
        private boolean allowOffscreen;
        private String useItem = "hand";
        private String icon = "pickup";
        private String requiredItem = "";
        private String clickFace = "front";
        private String rotateWith = "none";
        private double anchorX = 0.5D;
        private double anchorY = 0.5D;
        private double anchorZ = 0.5D;

        private Draft copy() {
            Draft copy = new Draft();
            copy.interactionId = interactionId;
            copy.action = action;
            copy.name = name;
            copy.showName = showName;
            copy.range = range;
            copy.allowE = allowE;
            copy.allowRightClick = allowRightClick;
            copy.allowOffscreen = allowOffscreen;
            copy.useItem = useItem;
            copy.icon = icon;
            copy.requiredItem = requiredItem;
            copy.clickFace = clickFace;
            copy.rotateWith = rotateWith;
            copy.anchorX = anchorX;
            copy.anchorY = anchorY;
            copy.anchorZ = anchorZ;
            return copy;
        }

        private JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("interactionId", interactionId);
            object.addProperty("range", range);
            object.addProperty("priority", requiredItem.isBlank() ? 40 : 45);
            object.addProperty("useItem", useItem);

            JsonObject text = new JsonObject();
            text.addProperty("action", action);
            text.addProperty("nameMode", name.isBlank() ? "auto" : "manual");
            text.addProperty("name", name);
            text.addProperty("showAction", true);
            text.addProperty("showName", showName);
            object.add("text", text);

            JsonObject input = new JsonObject();
            input.addProperty("allowE", allowE);
            input.addProperty("allowRightClick", allowRightClick);
            if (!requiredItem.isBlank()) {
                input.addProperty("requiredItem", requiredItem);
            }
            object.add("input", input);

            JsonObject visual = new JsonObject();
            visual.addProperty("icon", icon);
            visual.addProperty("allowOffscreen", allowOffscreen);
            object.add("visual", visual);

            JsonObject click = new JsonObject();
            click.addProperty("face", clickFace);
            object.add("click", click);

            JsonObject anchor = new JsonObject();
            JsonArray position = new JsonArray();
            position.add(round(anchorX));
            position.add(round(anchorY));
            position.add(round(anchorZ));
            anchor.add("position", position);
            anchor.addProperty("rotateWith", rotateWith);
            object.add("anchor", anchor);
            return object;
        }

        private static Draft fromVariant(JsonObject object, Draft base) {
            Draft draft = base.copy();
            draft.interactionId = string(object, "interactionId", "variant");
            draft.range = number(object, "range", draft.range);
            draft.useItem = cleanUseItem(string(object, "useItem",
                    draft.useItem));

            JsonObject text = child(object, "text");
            draft.action = string(text, "action",
                    string(object, "action", draft.action));
            draft.name = string(text, "name",
                    string(object, "name", draft.name));
            draft.showName = bool(text, "showName",
                    bool(object, "showName", draft.showName));

            JsonObject input = child(object, "input");
            draft.allowE = bool(input, "allowE",
                    bool(object, "allowE", draft.allowE));
            draft.allowRightClick = bool(input, "allowRightClick",
                    bool(object, "allowRightClick", draft.allowRightClick));
            draft.requiredItem = string(input, "requiredItem",
                    string(object, "requiredItem", draft.requiredItem));

            JsonObject visual = child(object, "visual");
            draft.icon = string(visual, "icon",
                    string(object, "icon", draft.icon));
            draft.allowOffscreen = bool(visual, "allowOffscreen",
                    bool(object, "allowOffscreen", draft.allowOffscreen));

            JsonObject click = child(object, "click");
            draft.clickFace = string(click, "face",
                    string(object, "clickFace", draft.clickFace));

            JsonObject anchor = child(object, "anchor");
            draft.rotateWith = string(anchor, "rotateWith",
                    draft.rotateWith);
            if (anchor.has("position")
                    && anchor.get("position").isJsonArray()) {
                JsonArray position = anchor.getAsJsonArray("position");
                if (position.size() > 0) draft.anchorX = position.get(0).getAsDouble();
                if (position.size() > 1) draft.anchorY = position.get(1).getAsDouble();
                if (position.size() > 2) draft.anchorZ = position.get(2).getAsDouble();
            }
            return draft;
        }

        private static JsonObject child(JsonObject object, String key) {
            return object.has(key) && object.get(key).isJsonObject()
                    ? object.getAsJsonObject(key) : new JsonObject();
        }

        private static String string(JsonObject object, String key,
                String fallback) {
            try {
                return object.has(key) && !object.get(key).isJsonNull()
                        ? object.get(key).getAsString() : fallback;
            } catch (Exception ignored) {
                return fallback;
            }
        }

        private static boolean bool(JsonObject object, String key,
                boolean fallback) {
            try {
                return object.has(key) && !object.get(key).isJsonNull()
                        ? object.get(key).getAsBoolean() : fallback;
            } catch (Exception ignored) {
                return fallback;
            }
        }

        private static double number(JsonObject object, String key,
                double fallback) {
            try {
                return object.has(key) && !object.get(key).isJsonNull()
                        ? object.get(key).getAsDouble() : fallback;
            } catch (Exception ignored) {
                return fallback;
            }
        }
    }

    private static final class CenteredEditBox extends EditBox {
        private CenteredEditBox(Font font, int x, int y, int width,
                int height, Component message) {
            super(font, x, y, width, height, message);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int offset = Math.max(0, (getHeight() - 9) / 2 + 2);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, offset, 0.0F);
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            graphics.pose().popPose();
        }
    }

    private final class ButtonControl extends AbstractButton {
        private final ButtonStyle style;
        private final Runnable action;

        private ButtonControl(int x, int y, int width, int height,
                String label, ButtonStyle style, Runnable action) {
            super(x, y, width, height, ScpFonts.roboto(label));
            this.style = style;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = !active ? 0xFF171B25
                    : isHoveredOrFocused() ? CONTROL_HOVER : CONTROL;
            int edge = style == ButtonStyle.DANGER ? DANGER
                    : style == ButtonStyle.PRIMARY ? ACCENT
                    : isHoveredOrFocused() ? BORDER_HOVER : BORDER;
            int text = !active ? MUTED
                    : style == ButtonStyle.PRIMARY ? ACCENT_TEXT : WHITE;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            outline(graphics, getX(), getY(), getWidth(), getHeight(), edge);
            if (style != ButtonStyle.NEUTRAL) {
                graphics.fill(getX() + 1, getY() + 1, getX() + 4,
                        getY() + getHeight() - 1, edge);
            }
            graphics.drawCenteredString(font, ScpFonts.roboto(getMessage()),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, text);
        }
    }
}
