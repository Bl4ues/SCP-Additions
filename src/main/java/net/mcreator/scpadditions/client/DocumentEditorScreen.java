package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.network.DocumentNetwork;
import com.bl4ues.scpinventory.network.DocumentSavePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.document.DocumentData;
import net.mcreator.scpadditions.init.DocumentItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creative-only structured document editor.
 *
 * <p>The left panel edits metadata, Markdown, and photographs while the right
 * panel renders the exact document page used by the Codex.</p>
 */
public final class DocumentEditorScreen extends Screen {
    private static final int PANEL_BACKGROUND = 0xF01B2024;
    private static final int PANEL_EDGE = 0xFF657078;
    private static final int HEADER_BACKGROUND = 0xFF242A2F;
    private static final int FIELD_BACKGROUND = 0xFF13181C;
    private static final int FIELD_EDGE = 0xFF4B555C;
    private static final int ROW_BACKGROUND = 0xD9272E33;
    private static final int TEXT_PRIMARY = 0xFFE4E8EA;
    private static final int TEXT_MUTED = 0xFF879097;
    private static final int PRIMARY = 0xFFC59A2A;
    private static final int PRIMARY_TEXT = 0xFFE5D49A;
    private static final int DANGER = 0xFFD46060;
    private static final int PREVIEW_BACKGROUND = 0xF0080B0E;

    private final InteractionHand hand;
    private final String documentId;
    private final List<FieldFrame> fieldFrames = new ArrayList<>();

    private DocumentData.State draft;
    private DocumentData.Template template;
    private String photoKey;
    private int photoWidth;
    private int photoHeight;

    private EditBox title;
    private EditBox category;
    private EditBox caption;
    private final EditBox[] labels = new EditBox[3];
    private final EditBox[] values = new EditBox[3];
    private final LineMarkdownEditor body;

    private boolean templateMenu;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int previewX;
    private int previewW;
    private int templateY;
    private int identityLabelY;
    private int metadataLabelY;
    private int photoLabelY;
    private int toolbarY;
    private int bodyLabelY;
    private int bodyX;
    private int bodyY;
    private int bodyW;
    private int bodyH;
    private int noticeY;
    private String notice = "Markdown: **bold**, *italic*, ---, and [[redacted]].";

    private DocumentEditorScreen(InteractionHand hand, ItemStack stack) {
        super(ScpFonts.roboto("Document Editor"));
        this.hand = hand;
        ItemStack copy = stack.copy();
        DocumentData.ensureInitialized(copy);
        DocumentData.State state = DocumentData.read(copy);
        this.documentId = state.documentId();
        this.draft = state;
        this.template = state.template();
        this.photoKey = state.photoKey();
        this.photoWidth = state.photoWidth();
        this.photoHeight = state.photoHeight();
        this.body = new LineMarkdownEditor(state.body());
    }

    public static void open(InteractionHand hand, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isCreative()) return;
        minecraft.setScreen(new DocumentEditorScreen(hand, stack));
    }

    @Override
    protected void init() {
        fieldFrames.clear();
        calculateLayout();

        int innerLeft = panelX + 18;
        int innerWidth = panelW - 36;
        int y = templateY;

        addRenderableWidget(new EditorButton(innerLeft, y,
                innerWidth - 118, 22,
                Component.literal("Template: " + template.displayName()),
                ButtonStyle.NEUTRAL, this::toggleTemplateMenu));
        addRenderableWidget(new EditorButton(innerLeft + innerWidth - 112, y,
                112, 22, Component.literal("Apply Template"),
                ButtonStyle.PRIMARY, this::applyTemplate));

        int half = (innerWidth - 8) / 2;
        int identityY = identityLabelY + 13;
        title = field(innerLeft, identityY, half,
                "Document title", draft.title());
        category = field(innerLeft + half + 8, identityY, half,
                "Codex category", draft.category());

        int metadataY = metadataLabelY + 13;
        for (int index = 0; index < 3; index++) {
            int rowY = metadataY + index * 27;
            labels[index] = field(innerLeft, rowY, half,
                    "Metadata label " + (index + 1), header(draft, index));
            values[index] = field(innerLeft + half + 8, rowY, half,
                    "Metadata value " + (index + 1), value(draft, index));
        }

        int photoY = photoLabelY + 13;
        addRenderableWidget(new EditorButton(innerLeft, photoY, 112, 22,
                Component.literal(photoKey.isBlank()
                        ? "Add Photograph" : "Replace Photograph"),
                ButtonStyle.NEUTRAL, this::openPhoto));
        EditorButton remove = addRenderableWidget(new EditorButton(
                innerLeft + 118, photoY, 112, 22,
                Component.literal("Remove Photograph"),
                ButtonStyle.DANGER, this::removePhoto));
        remove.active = !photoKey.isBlank();
        caption = field(innerLeft + 236, photoY,
                innerWidth - 236, "Optional photograph caption",
                draft.caption());

        addFormattingButtons(innerLeft, innerWidth);

        List<EditBox> bodyBoxes = body.build(font, bodyX, bodyY, bodyW, bodyH);
        for (EditBox box : bodyBoxes) addRenderableWidget(box);
        EditBox focusedBody = body.focusedWidget();
        if (focusedBody != null) setFocused(focusedBody);

        int buttonY = panelY + panelH - 30;
        int third = (innerWidth - 12) / 3;
        addRenderableWidget(new EditorButton(innerLeft, buttonY, third, 22,
                Component.literal("Save Document"),
                ButtonStyle.PRIMARY, this::save));
        addRenderableWidget(new EditorButton(innerLeft + third + 6, buttonY,
                third, 22, Component.literal("Reset Template"),
                ButtonStyle.NEUTRAL, this::applyTemplate));
        addRenderableWidget(new EditorButton(
                innerLeft + (third + 6) * 2, buttonY,
                innerWidth - (third + 6) * 2, 22,
                Component.literal("Cancel"),
                ButtonStyle.NEUTRAL, this::onClose));

        if (templateMenu) {
            int menuY = templateY + 24;
            int menuWidth = innerWidth - 118;
            suppressFieldsBehindMenu(innerLeft, menuY,
                    menuWidth, templateMenuHeight());
            addTemplateChoices(innerLeft, menuY, menuWidth);
        }
    }

    private void calculateLayout() {
        panelX = 8;
        panelY = 8;
        panelH = Math.max(360, height - 16);
        panelW = Math.max(390,
                Math.min(700, Math.round(width * 0.56F)));
        if (panelW + 300 > width) {
            panelW = Math.max(360, width - 310);
        }

        previewX = panelX + panelW + 10;
        previewW = Math.max(180, width - previewX - 8);

        templateY = panelY + 44;
        identityLabelY = templateY + 31;
        metadataLabelY = identityLabelY + 42;
        photoLabelY = metadataLabelY + 94;
        toolbarY = photoLabelY + 42;
        bodyLabelY = toolbarY + 29;

        bodyX = panelX + 18;
        bodyY = bodyLabelY + 14;
        bodyW = panelW - 36;
        int buttonY = panelY + panelH - 30;
        noticeY = buttonY - 15;
        bodyH = Math.max(66, noticeY - 8 - bodyY);
    }

    private void addFormattingButtons(int x, int width) {
        int quarter = (width - 18) / 4;
        addRenderableWidget(new EditorButton(x, toolbarY,
                quarter, 22, Component.literal("Bold"),
                ButtonStyle.NEUTRAL,
                () -> applyMarkdownWrap("**", "**", "Bold")));
        addRenderableWidget(new EditorButton(x + quarter + 6, toolbarY,
                quarter, 22, Component.literal("Italic"),
                ButtonStyle.NEUTRAL,
                () -> applyMarkdownWrap("*", "*", "Italic")));
        addRenderableWidget(new EditorButton(x + (quarter + 6) * 2,
                toolbarY, quarter, 22, Component.literal("Divider"),
                ButtonStyle.NEUTRAL, this::insertDivider));
        addRenderableWidget(new EditorButton(x + (quarter + 6) * 3,
                toolbarY, width - (quarter + 6) * 3, 22,
                Component.literal("Redact"),
                ButtonStyle.NEUTRAL,
                () -> applyMarkdownWrap("[[", "]]", "Redaction")));
    }

    private void addTemplateChoices(int x, int y, int width) {
        for (DocumentData.Template choice : DocumentData.Template.values()) {
            ButtonStyle style = choice == template
                    ? ButtonStyle.SELECTED : ButtonStyle.NEUTRAL;
            addRenderableWidget(new EditorButton(x, y, width, 22,
                    Component.literal(choice.displayName()), style, () -> {
                captureDraft();
                template = choice;
                templateMenu = false;
                rebuildEditor(false);
            }));
            y += 23;
        }
    }

    private EditBox field(int x, int y, int width,
                          String hint, String value) {
        fieldFrames.add(new FieldFrame(x, y, Math.max(40, width), 22));
        EditBox box = new EditBox(font, x + 5, y + 4,
                Math.max(30, width - 10), 18, Component.literal(hint));
        box.setBordered(false);
        box.setTextColor(TEXT_PRIMARY);
        box.setTextColorUneditable(TEXT_MUTED);
        box.setHint(ScpFonts.roboto(hint));
        box.setMaxLength(DocumentData.MAX_SHORT_TEXT);
        box.setValue(value == null ? "" : value);
        box.setFormatter((text, cursor) ->
                ScpFonts.roboto(text).getVisualOrderText());
        return addRenderableWidget(box);
    }

    private void toggleTemplateMenu() {
        captureDraft();
        templateMenu = !templateMenu;
        rebuildEditor(false);
    }

    private void applyTemplate() {
        body.detach();
        draft = template.createState(documentId);
        photoKey = "";
        photoWidth = 0;
        photoHeight = 0;
        body.setText(draft.body());
        notice = "Applied " + template.displayName() + ".";
        templateMenu = false;
        rebuildEditor(false);
    }

    private void applyMarkdownWrap(String prefix, String suffix,
                                   String label) {
        body.rememberFocus();
        if (!body.wrapFocused(prefix, suffix)) {
            notice = "Select a Markdown line before applying " + label + ".";
            return;
        }
        captureDraft();
        notice = label + " formatting applied.";
        rebuildEditor(false);
    }

    private void insertDivider() {
        body.rememberFocus();
        captureDraft();
        if (!body.divider()) {
            notice = "Select a Markdown line before adding a divider.";
            return;
        }
        notice = "Divider added.";
        rebuildEditor(false);
    }

    private void openPhoto() {
        captureDraft();
        body.detach();
        Minecraft.getInstance().setScreen(new CodexImageDropScreen(
                this, !photoKey.isBlank(), "document_image", imported -> {
            photoKey = imported.key();
            photoWidth = imported.width();
            photoHeight = imported.height();
            draft = withPhoto(draft, photoKey, photoWidth,
                    photoHeight, draft.caption());
            notice = "Photograph imported: " + imported.fileName();
            Minecraft.getInstance().setScreen(this);
            rebuildEditor(false);
        }, () -> {
            photoKey = "";
            photoWidth = 0;
            photoHeight = 0;
            draft = withPhoto(draft, "", 0, 0, draft.caption());
            notice = "Photograph removed.";
            Minecraft.getInstance().setScreen(this);
            rebuildEditor(false);
        }));
    }

    private void removePhoto() {
        captureDraft();
        photoKey = "";
        photoWidth = 0;
        photoHeight = 0;
        draft = withPhoto(draft, "", 0, 0,
                caption == null ? draft.caption() : caption.getValue());
        notice = "Photograph removed.";
        rebuildEditor(false);
    }

    private DocumentData.State currentState() {
        body.sync();
        if (title == null || category == null || caption == null) {
            return draft;
        }
        return DocumentData.sanitize(new DocumentData.State(
                documentId,
                template,
                title.getValue(),
                category.getValue(),
                labels[0].getValue(),
                values[0].getValue(),
                labels[1].getValue(),
                values[1].getValue(),
                labels[2].getValue(),
                values[2].getValue(),
                body.text(),
                photoKey,
                photoWidth,
                photoHeight,
                caption.getValue()));
    }

    private void captureDraft() {
        draft = currentState();
    }

    private static DocumentData.State withPhoto(DocumentData.State state,
                                                String key,
                                                int width,
                                                int height,
                                                String caption) {
        return new DocumentData.State(
                state.documentId(),
                state.template(),
                state.title(),
                state.category(),
                state.header1(),
                state.value1(),
                state.header2(),
                state.value2(),
                state.header3(),
                state.value3(),
                state.body(),
                key,
                width,
                height,
                caption);
    }

    private void save() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isCreative()) {
            notice = "Documents can only be edited in Creative mode.";
            return;
        }

        DocumentData.State state = currentState();
        if (state.title().isBlank() || state.category().isBlank()) {
            notice = "Document title and Codex category are required.";
            return;
        }

        ItemStack held = minecraft.player.getItemInHand(hand);
        if (!DocumentData.isDedicatedItem(held)) {
            notice = "Hold the Document being edited before saving.";
            return;
        }

        DocumentData.write(held, state);
        DocumentNetwork.CHANNEL.sendToServer(new DocumentSavePacket(
                hand, DocumentData.toNetworkTag(state)));
        minecraft.setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        captureDraft();
        if (body.keyPressed(keyCode)) {
            rebuildEditor(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        body.rememberFocus();
        if (templateMenu) {
            if (button != 0) return true;

            int innerLeft = panelX + 18;
            int innerWidth = panelW - 36;
            int menuX = innerLeft;
            int menuY = templateY + 24;
            int menuWidth = innerWidth - 118;
            int menuHeight = templateMenuHeight();

            if (inside(mouseX, mouseY,
                    menuX, menuY, menuWidth, menuHeight)) {
                int relativeY = (int) mouseY - menuY;
                int index = relativeY / 23;
                int withinRow = relativeY % 23;
                DocumentData.Template[] choices =
                        DocumentData.Template.values();
                if (index >= 0 && index < choices.length
                        && withinRow < 22) {
                    captureDraft();
                    template = choices[index];
                    templateMenu = false;
                    rebuildEditor(false);
                }
                return true;
            }

            if (inside(mouseX, mouseY,
                    innerLeft, templateY,
                    innerWidth - 118, 22)) {
                toggleTemplateMenu();
                return true;
            }

            if (inside(mouseX, mouseY,
                    innerLeft + innerWidth - 112,
                    templateY, 112, 22)) {
                templateMenu = false;
                applyTemplate();
                return true;
            }

            captureDraft();
            templateMenu = false;
            rebuildEditor(false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= bodyX && mouseX <= bodyX + bodyW
                && mouseY >= bodyY && mouseY <= bodyY + bodyH
                && body.scroll(delta)) {
            captureDraft();
            rebuildEditor(false);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics);
        drawEditorPanel(graphics);
        drawPreviewPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        DocumentRenderer.render(graphics, previewStack(),
                previewX + 10, panelY + 34,
                previewW - 20, panelH - 44);
    }

    private void drawEditorPanel(GuiGraphics graphics) {
        graphics.fill(panelX, panelY,
                panelX + panelW, panelY + panelH, PANEL_BACKGROUND);
        outline(graphics, panelX, panelY, panelW, panelH, PANEL_EDGE);
        drawDotPattern(graphics, panelX, panelY, panelW, panelH);

        graphics.fill(panelX, panelY,
                panelX + panelW, panelY + 34, HEADER_BACKGROUND);
        graphics.fill(panelX, panelY + 33,
                panelX + panelW, panelY + 34, PRIMARY);
        graphics.drawString(font, ScpFonts.montserrat("DOCUMENT EDITOR"),
                panelX + 18, panelY + 13, TEXT_PRIMARY, false);

        drawSectionLabel(graphics, "DOCUMENT IDENTITY", identityLabelY);
        drawSectionLabel(graphics, "HEADER METADATA", metadataLabelY);
        drawSectionLabel(graphics, "PHOTOGRAPH", photoLabelY);
        drawSectionLabel(graphics, "BODY / MARKDOWN", bodyLabelY);

        for (FieldFrame frame : fieldFrames) {
            drawField(graphics, frame.x(), frame.y(),
                    frame.width(), frame.height());
        }

        for (Map.Entry<EditBox, Integer> entry : body.visible().entrySet()) {
            EditBox box = entry.getKey();
            int rowY = box.getY() - 4;
            graphics.fill(bodyX, rowY,
                    bodyX + bodyW, rowY + 22, ROW_BACKGROUND);
            outline(graphics, bodyX, rowY, bodyW, 22, FIELD_EDGE);
            graphics.fill(bodyX + 29, rowY + 3,
                    bodyX + 30, rowY + 19, 0xFF30383E);
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(Integer.toString(entry.getValue() + 1)),
                    bodyX + 14, rowY + 8, TEXT_MUTED);
        }

        graphics.drawString(font, ScpFonts.roboto(notice),
                panelX + 18, noticeY, TEXT_MUTED, false);
    }

    private void drawPreviewPanel(GuiGraphics graphics) {
        graphics.fill(previewX, panelY,
                previewX + previewW, panelY + panelH, PREVIEW_BACKGROUND);
        outline(graphics, previewX, panelY, previewW, panelH, PANEL_EDGE);
        drawDotPattern(graphics, previewX, panelY, previewW, panelH);
        graphics.fill(previewX, panelY,
                previewX + previewW, panelY + 34, 0xFF101419);
        graphics.fill(previewX, panelY + 33,
                previewX + previewW, panelY + 34, PRIMARY);
        graphics.drawString(font, ScpFonts.montserrat("LIVE PREVIEW"),
                previewX + 18, panelY + 13, TEXT_PRIMARY, false);
    }

    private void drawSectionLabel(GuiGraphics graphics,
                                  String label, int y) {
        graphics.drawString(font, ScpFonts.roboto(label),
                panelX + 18, y, TEXT_MUTED, false);
        int start = panelX + 18 + font.width(ScpFonts.roboto(label)) + 8;
        int end = panelX + panelW - 18;
        if (start < end) {
            graphics.fill(start, y + 4, end, y + 5, 0xFF30383E);
        }
    }

    private ItemStack previewStack() {
        ItemStack stack = new ItemStack(DocumentItems.getDocument());
        DocumentData.write(stack, currentState());
        return stack;
    }

    private void rebuildEditor(boolean capture) {
        if (capture) captureDraft();
        body.detach();
        clearWidgets();
        init();
    }

    private int templateMenuHeight() {
        return Math.max(0,
                DocumentData.Template.values().length * 23 - 1);
    }

    private void suppressFieldsBehindMenu(int x, int y,
                                          int width, int height) {
        hideIfIntersects(title, x, y, width, height);
        hideIfIntersects(category, x, y, width, height);
        hideIfIntersects(caption, x, y, width, height);
        for (EditBox box : labels) {
            hideIfIntersects(box, x, y, width, height);
        }
        for (EditBox box : values) {
            hideIfIntersects(box, x, y, width, height);
        }
        for (EditBox box : body.visible().keySet()) {
            hideIfIntersects(box, x, y, width, height);
        }
    }

    private static void hideIfIntersects(EditBox box,
                                         int x, int y,
                                         int width, int height) {
        if (box == null || !intersects(box.getX(), box.getY(),
                box.getWidth(), box.getHeight(),
                x, y, width, height)) {
            return;
        }
        box.visible = false;
        box.active = false;
    }

    private static boolean inside(double mouseX, double mouseY,
                                  int x, int y,
                                  int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    private static boolean intersects(int x1, int y1,
                                      int width1, int height1,
                                      int x2, int y2,
                                      int width2, int height2) {
        return x1 < x2 + width2 && x1 + width1 > x2
                && y1 < y2 + height2 && y1 + height1 > y2;
    }

    private static void drawField(GuiGraphics graphics, int x, int y,
                                  int width, int height) {
        graphics.fill(x, y, x + width, y + height, FIELD_BACKGROUND);
        outline(graphics, x, y, width, height, FIELD_EDGE);
    }

    private static void drawDotPattern(GuiGraphics graphics,
                                       int x, int y,
                                       int width, int height) {
        for (int dotX = x + 6; dotX < x + width - 4; dotX += 8) {
            for (int dotY = y + 6; dotY < y + height - 4; dotY += 8) {
                graphics.fill(dotX, dotY, dotX + 1, dotY + 1,
                        0x182F383E);
            }
        }
    }

    private static void outline(GuiGraphics graphics, int x, int y,
                                int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1,
                x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y,
                x + width, y + height, color);
    }

    private static String header(DocumentData.State state, int index) {
        return index == 0 ? state.header1()
                : index == 1 ? state.header2() : state.header3();
    }

    private static String value(DocumentData.State state, int index) {
        return index == 0 ? state.value1()
                : index == 1 ? state.value2() : state.value3();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record FieldFrame(int x, int y, int width, int height) {
    }

    private final class EditorButton extends AbstractButton {
        private final ButtonStyle style;
        private final Runnable action;

        private EditorButton(int x, int y, int width, int height,
                             Component label, ButtonStyle style,
                             Runnable action) {
            super(x, y, width, height, label);
            this.style = style;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                                    int mouseY, float partialTick) {
            int background = !active ? 0xFF22282D
                    : isHoveredOrFocused() ? 0xFF56636B : 0xFF343D43;
            int edge = switch (style) {
                case PRIMARY -> isHoveredOrFocused()
                        ? PRIMARY_TEXT : PRIMARY;
                case DANGER -> DANGER;
                case SELECTED -> PRIMARY_TEXT;
                case NEUTRAL -> isHoveredOrFocused()
                        ? 0xFFD8E0E4 : 0xFF667178;
            };
            int text = !active ? TEXT_MUTED
                    : style == ButtonStyle.PRIMARY
                    || style == ButtonStyle.SELECTED
                    ? PRIMARY_TEXT : TEXT_PRIMARY;

            graphics.fill(getX(), getY(),
                    getX() + getWidth(), getY() + getHeight(), background);
            outline(graphics, getX(), getY(),
                    getWidth(), getHeight(), edge);
            if (style != ButtonStyle.NEUTRAL) {
                graphics.fill(getX() + 1, getY() + 1,
                        getX() + 4, getY() + getHeight() - 1, edge);
            }
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(getMessage()),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2,
                    text);
        }
    }

    private enum ButtonStyle {
        NEUTRAL,
        PRIMARY,
        DANGER,
        SELECTED
    }
}
