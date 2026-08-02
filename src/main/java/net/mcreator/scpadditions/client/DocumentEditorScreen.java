package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.network.DocumentNetwork;
import com.bl4ues.scpinventory.network.DocumentSavePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.document.DocumentData;
import net.mcreator.scpadditions.init.DocumentItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Document fields and Markdown on the left, live rendered page on the right. */
public final class DocumentEditorScreen extends Screen {
    private static final int PANEL = 0xF0111317, HEADER = 0xFF24282E;
    private static final int TEXT = 0xFFE8E8E8, MUTED = 0xFFA9AFBA, ACCENT = 0xFFC59A2A;
    private final InteractionHand hand;
    private final String documentId;
    private DocumentData.State draft;
    private DocumentData.Template template;
    private String photoKey;
    private int photoWidth, photoHeight;
    private EditBox title, category, caption;
    private final EditBox[] labels = new EditBox[3], values = new EditBox[3];
    private LineMarkdownEditor body;
    private boolean templateMenu;
    private final List<Button> templateChoices = new ArrayList<>();
    private int panelX, panelY, panelW, panelH, bodyY, bodyH;
    private String notice = "Markdown: **bold**, *italic*, ---, and [[redacted]].";

    private DocumentEditorScreen(InteractionHand hand, ItemStack stack) {
        super(ScpFonts.roboto("Document Editor"));
        this.hand = hand;
        ItemStack copy = stack.copy();
        DocumentData.ensureInitialized(copy);
        DocumentData.State state = DocumentData.read(copy);
        documentId = state.documentId();
        draft = state;
        template = state.template();
        photoKey = state.photoKey();
        photoWidth = state.photoWidth();
        photoHeight = state.photoHeight();
        body = new LineMarkdownEditor(state.body());
    }

    public static void open(InteractionHand hand, ItemStack stack) {
        Minecraft.getInstance().setScreen(new DocumentEditorScreen(hand, stack));
    }

    @Override protected void init() {
        templateChoices.clear();
        panelX = 8; panelY = 8; panelH = height - 16;
        panelW = Math.max(350, Math.min(650, Math.round(width * 0.55F)));
        if (panelW + 260 > width) panelW = Math.max(340, width - 270);
        int x = panelX + 12, y = panelY + 38, fieldW = panelW - 24;

        addRenderableWidget(Button.builder(Component.literal("Template: " + template.displayName()),
                b -> { templateMenu = !templateMenu; rebuild(); })
                .bounds(x, y, fieldW - 112, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply Template"), b -> applyTemplate())
                .bounds(x + fieldW - 106, y, 106, 20).build());
        y += 27;
        DocumentData.State state = state();
        int half = (fieldW - 6) / 2;
        title = field(x, y, half, "Document title", state.title());
        category = field(x + half + 6, y, half, "Codex category", state.category());
        y += 27;
        for (int i = 0; i < 3; i++) {
            labels[i] = field(x, y, half, "Header " + (i + 1), header(state, i));
            values[i] = field(x + half + 6, y, half, "Header answer " + (i + 1), value(state, i));
            y += 27;
        }
        addRenderableWidget(Button.builder(Component.literal(photoKey.isBlank() ? "Add Photo" : "Replace Photo"),
                b -> openPhoto()).bounds(x, y, 106, 20).build());
        Button remove = addRenderableWidget(Button.builder(Component.literal("Remove Photo"), b -> {
            sync(); photoKey = ""; photoWidth = photoHeight = 0;
            draft = withPhoto(draft, "", 0, 0, caption == null ? "" : caption.getValue());
            notice = "Photo removed."; rebuild();
        }).bounds(x + 112, y, 106, 20).build());
        remove.active = !photoKey.isBlank();
        caption = field(x + 224, y, fieldW - 224, "Optional photo caption", state.caption());
        y += 29;
        toolbar(x, y, fieldW); y += 25;
        bodyY = y + 13; bodyH = Math.max(66, panelY + panelH - 48 - bodyY);
        for (EditBox box : body.build(font, x, bodyY, fieldW, bodyH)) addRenderableWidget(box);
        int by = panelY + panelH - 30, third = (fieldW - 12) / 3;
        addRenderableWidget(Button.builder(Component.literal("Save Document"), b -> save())
                .bounds(x, by, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset Template"), b -> applyTemplate())
                .bounds(x + third + 6, by, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(x + (third + 6) * 2, by, third, 20).build());
        if (templateMenu) addTemplateChoices(x, panelY + 60, fieldW - 112);
    }

    private void toolbar(int x, int y, int width) {
        int q = (width - 18) / 4;
        addRenderableWidget(Button.builder(Component.literal("** Bold **"), b -> { body.wrapFocused("**", "**"); rebuild(); })
                .bounds(x, y, q, 20).build());
        addRenderableWidget(Button.builder(Component.literal("* Italic *"), b -> { body.wrapFocused("*", "*"); rebuild(); })
                .bounds(x + q + 6, y, q, 20).build());
        addRenderableWidget(Button.builder(Component.literal("--- Divider"), b -> { body.divider(); rebuild(); })
                .bounds(x + (q + 6) * 2, y, q, 20).build());
        addRenderableWidget(Button.builder(Component.literal("[[ Redact ]]"), b -> { body.wrapFocused("[[", "]]" ); rebuild(); })
                .bounds(x + (q + 6) * 3, y, width - (q + 6) * 3, 20).build());
    }

    private void addTemplateChoices(int x, int y, int width) {
        for (DocumentData.Template choice : DocumentData.Template.values()) {
            Button button = addRenderableWidget(Button.builder(Component.literal(choice.displayName()), b -> {
                template = choice; templateMenu = false; rebuild();
            }).bounds(x, y, width, 20).build());
            templateChoices.add(button); y += 21;
        }
    }

    private EditBox field(int x, int y, int width, String hint, String value) {
        EditBox box = new EditBox(font, x, y, Math.max(40, width), 20, Component.literal(hint));
        box.setHint(Component.literal(hint)); box.setMaxLength(DocumentData.MAX_SHORT_TEXT); box.setValue(value);
        box.setFormatter((text, cursor) -> ScpFonts.roboto(text).getVisualOrderText());
        return addRenderableWidget(box);
    }

    private void applyTemplate() {
        sync();
        draft = template.createState(documentId);
        body.setText(draft.body());
        photoKey = ""; photoWidth = photoHeight = 0;
        notice = "Applied " + template.displayName() + ".";
        rebuild();
    }

    private void openPhoto() {
        sync();
        Minecraft.getInstance().setScreen(new CodexImageDropScreen(this, !photoKey.isBlank(), "document_image", imported -> {
            photoKey = imported.key(); photoWidth = imported.width(); photoHeight = imported.height();
            draft = withPhoto(draft, photoKey, photoWidth, photoHeight, draft.caption());
            notice = "Photo imported: " + imported.fileName(); Minecraft.getInstance().setScreen(this); rebuild();
        }, () -> {
            photoKey = ""; photoWidth = photoHeight = 0;
            draft = withPhoto(draft, "", 0, 0, draft.caption());
            Minecraft.getInstance().setScreen(this); rebuild();
        }));
    }

    private void sync() {
        body.sync();
        if (title == null) return;
        draft = DocumentData.sanitize(new DocumentData.State(documentId, template,
                title.getValue(), category.getValue(), labels[0].getValue(), values[0].getValue(),
                labels[1].getValue(), values[1].getValue(), labels[2].getValue(), values[2].getValue(),
                body.text(), photoKey, photoWidth, photoHeight, caption.getValue()));
    }

    private DocumentData.State state() {
        sync();
        return draft;
    }

    private static DocumentData.State withPhoto(DocumentData.State state, String key, int width,
                                                int height, String caption) {
        return new DocumentData.State(state.documentId(), state.template(), state.title(), state.category(),
                state.header1(), state.value1(), state.header2(), state.value2(), state.header3(),
                state.value3(), state.body(), key, width, height, caption);
    }

    private void save() {
        DocumentData.State state = state();
        if (state.title().isBlank() || state.category().isBlank()) {
            notice = "Title and Codex category are required."; return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ItemStack held = mc.player.getItemInHand(hand);
            if (DocumentData.isDedicatedItem(held)) DocumentData.write(held, state);
        }
        DocumentNetwork.CHANNEL.sendToServer(new DocumentSavePacket(hand, DocumentData.toNetworkTag(state)));
        mc.setScreen(null);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (body.keyPressed(keyCode)) { rebuild(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= bodyY && mouseY <= bodyY + bodyH
                && body.scroll(delta)) { rebuild(); return true; }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float tick) {
        renderBackground(g);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        g.fill(panelX, panelY, panelX + panelW, panelY + 30, HEADER);
        g.fill(panelX, panelY + 29, panelX + panelW, panelY + 30, ACCENT);
        g.drawString(font, ScpFonts.roboto("DOCUMENT EDITOR"), panelX + 12, panelY + 11, TEXT, false);
        g.drawString(font, ScpFonts.roboto("BODY / MARKDOWN"), panelX + 12, bodyY - 11, MUTED, false);
        for (Map.Entry<EditBox, Integer> entry : body.visible().entrySet())
            g.drawString(font, Integer.toString(entry.getValue() + 1), panelX + 15, entry.getKey().getY() + 6, 0xFF6F7888, false);
        g.drawString(font, ScpFonts.roboto(notice), panelX + 12, panelY + panelH - 43, MUTED, false);
        int px = panelX + panelW + 10, pw = Math.max(100, width - px - 8);
        g.fill(px, panelY, px + pw, panelY + panelH, 0xE8080A0D);
        g.drawString(font, ScpFonts.roboto("LIVE PREVIEW"), px + 10, panelY + 10, MUTED, false);
        super.render(g, mouseX, mouseY, tick);
        DocumentRenderer.render(g, previewStack(), px + 8, panelY + 28, pw - 16, panelH - 36);
    }

    private ItemStack previewStack() {
        ItemStack stack = new ItemStack(DocumentItems.getDocument()); DocumentData.write(stack, state()); return stack;
    }

    private void rebuild() { clearWidgets(); init(); }
    private static String header(DocumentData.State s, int i) { return i == 0 ? s.header1() : i == 1 ? s.header2() : s.header3(); }
    private static String value(DocumentData.State s, int i) { return i == 0 ? s.value1() : i == 1 ? s.value2() : s.value3(); }
    @Override public boolean isPauseScreen() { return false; }
}
