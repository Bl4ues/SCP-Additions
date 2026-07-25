package net.mcreator.scpadditions.client.gui;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.ScpFonts;
import net.mcreator.scpadditions.facility.FacilitySignBlock;
import net.mcreator.scpadditions.facility.FacilitySignClipboard;
import net.mcreator.scpadditions.facility.FacilitySignData;
import net.mcreator.scpadditions.keycard.KeycardReaderInteractionEvents;
import net.mcreator.scpadditions.network.FacilitySignClipboardPacket;
import net.mcreator.scpadditions.network.FacilitySignSavePacket;

import java.util.ArrayList;
import java.util.List;

/** SCP Unity-styled three-entry editor opened with the Screwdriver. */
public final class FacilitySignEditorScreen extends Screen {
    private static final int PANEL_BACKGROUND = 0xF01B2024;
    private static final int PANEL_EDGE = 0xFF657078;
    private static final int ROW_BACKGROUND = 0xD9272E33;
    private static final int FIELD_BACKGROUND = 0xFF13181C;
    private static final int FIELD_EDGE = 0xFF4B555C;
    private static final int TEXT_PRIMARY = 0xFFE4E8EA;
    private static final int TEXT_MUTED = 0xFF879097;
    private static final int ACCENT = 0xFFB8C2C8;
    private static final int PRIMARY = 0xFFC59A2A;
    private static final int PRIMARY_TEXT = 0xFFE5D49A;
    private static final int DANGER = 0xFFD46060;
    private static final int SUCCESS = 0xFF9FC7A8;
    private static final int WARNING = 0xFFD4B37A;

    private final BlockPos signPos;
    private final FacilitySignBlock.SignType type;
    private final List<FacilitySignData.Entry> initialEntries;
    private final List<EditBox> textFields = new ArrayList<>();
    private final List<EditBox> numberFields = new ArrayList<>();

    private FacilitySignClipboard.EntryClipboard copiedEntry;
    private FacilitySignClipboard.SignClipboard copiedSign;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int rowsTop;
    private int rowHeight;
    private int draggingRow = -1;
    private String status = "";
    private long statusExpiresAt;

    private FacilitySignEditorScreen(BlockPos signPos,
            FacilitySignBlock.SignType type,
            List<FacilitySignData.Entry> entries) {
        super(Component.translatable("screen.scp_additions.facility_sign_editor"));
        this.signPos = signPos.immutable();
        this.type = type;
        this.initialEntries = FacilitySignData.normalize(type, entries);
    }

    public static void open(BlockPos signPos, FacilitySignBlock.SignType type,
            List<FacilitySignData.Entry> entries) {
        Minecraft.getInstance().setScreen(
                new FacilitySignEditorScreen(signPos, type, entries));
    }

    @Override
    protected void init() {
        textFields.clear();
        numberFields.clear();
        calculateLayout();
        loadClipboard();

        int innerLeft = panelLeft + 18;
        int innerRight = panelLeft + panelWidth - 18;
        int dragWidth = 22;
        int controlWidth = 29;
        int controlGap = 4;
        int controlsWidth = controlWidth * 3 + controlGap * 2;
        int controlsLeft = innerRight - controlsWidth;
        int counterWidth = 39;
        int numberWidth = type.hasNumbers() ? 48 : 0;
        int textLeft = innerLeft + dragWidth + 7
                + (type.hasNumbers() ? numberWidth + 7 : 0);
        int textWidth = Math.max(80,
                controlsLeft - counterWidth - textLeft - 10);

        for (int index = 0; index < FacilitySignData.ENTRY_COUNT; index++) {
            int rowY = rowsTop + index * rowHeight;
            FacilitySignData.Entry entry = initialEntries.get(index);

            if (type.hasNumbers()) {
                EditBox number = new EditBox(font,
                        innerLeft + dragWidth + 7, rowY + 7,
                        numberWidth, 18, Component.translatable(
                        "screen.scp_additions.facility_sign_number"));
                number.setBordered(false);
                number.setTextColor(TEXT_PRIMARY);
                number.setTextColorUneditable(TEXT_MUTED);
                number.setMaxLength(FacilitySignData.MAX_NUMBER_LENGTH * 2);
                number.setFilter(value -> value.codePointCount(0, value.length())
                        <= FacilitySignData.MAX_NUMBER_LENGTH
                        && value.codePoints().allMatch(Character::isDigit));
                number.setValue(entry.number());
                numberFields.add(addRenderableWidget(number));
            }

            EditBox text = new EditBox(font, textLeft, rowY + 7,
                    textWidth, 18, Component.translatable(
                    "screen.scp_additions.facility_sign_text"));
            text.setBordered(false);
            text.setTextColor(TEXT_PRIMARY);
            text.setTextColorUneditable(TEXT_MUTED);
            text.setMaxLength(type.maxTextLength() * 2);
            text.setFilter(value -> value.codePointCount(0, value.length())
                    <= type.maxTextLength()
                    && value.codePoints().noneMatch(Character::isISOControl));
            text.setValue(entry.text());
            textFields.add(addRenderableWidget(text));

            final int row = index;
            addRenderableWidget(new EditorButton(controlsLeft, rowY + 6,
                    controlWidth, 20, Component.literal("C"),
                    ButtonStyle.NEUTRAL, () -> copyEntry(row)));
            addRenderableWidget(new EditorButton(
                    controlsLeft + controlWidth + controlGap, rowY + 6,
                    controlWidth, 20, Component.literal("P"),
                    ButtonStyle.NEUTRAL, () -> pasteEntry(row)));
            addRenderableWidget(new EditorButton(
                    controlsLeft + (controlWidth + controlGap) * 2, rowY + 6,
                    controlWidth, 20, Component.literal("×"),
                    ButtonStyle.DANGER, () -> clearEntry(row)));
        }

        int toolbarY = rowsTop - 27;
        addRenderableWidget(new EditorButton(innerLeft, toolbarY,
                105, 20, Component.translatable(
                "screen.scp_additions.facility_sign_copy_all"),
                ButtonStyle.NEUTRAL, this::copyEntireSign));
        addRenderableWidget(new EditorButton(innerLeft + 111, toolbarY,
                105, 20, Component.translatable(
                "screen.scp_additions.facility_sign_paste_all"),
                ButtonStyle.NEUTRAL, this::pasteEntireSign));
        addRenderableWidget(new EditorButton(innerLeft + 222, toolbarY,
                75, 20, Component.translatable(
                "screen.scp_additions.facility_sign_clear"),
                ButtonStyle.DANGER, this::clearAll));

        int bottomY = panelTop + panelHeight - 31;
        addRenderableWidget(new EditorButton(innerRight - 175, bottomY,
                82, 22, Component.translatable("gui.done"), this::saveAndClose));
        addRenderableWidget(new EditorButton(innerRight - 87, bottomY,
                82, 22, Component.translatable("gui.cancel"),
                ButtonStyle.NEUTRAL, this::onClose));
    }

    private void calculateLayout() {
        panelWidth = Math.min(640, Math.max(360, width - 24));
        panelHeight = 218;
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(6, (height - panelHeight) / 2);
        rowHeight = 31;
        rowsTop = panelTop + 79;
    }

    private void loadClipboard() {
        if (minecraft == null || minecraft.player == null) return;
        ItemStack screwdriver =
                KeycardReaderInteractionEvents.screwdriver(minecraft.player);
        copiedEntry = FacilitySignClipboard.readEntry(screwdriver);
        copiedSign = FacilitySignClipboard.readSign(screwdriver);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        drawPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (!status.isBlank() && Util.getMillis() < statusExpiresAt) {
            graphics.drawCenteredString(font, ScpFonts.roboto(status),
                    width / 2, panelTop + panelHeight - 26,
                    status.startsWith("No ") ? WARNING : SUCCESS);
        }
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(panelLeft, panelTop,
                panelLeft + panelWidth, panelTop + panelHeight, PANEL_BACKGROUND);
        outline(graphics, panelLeft, panelTop, panelWidth, panelHeight, PANEL_EDGE);

        for (int x = panelLeft + 6; x < panelLeft + panelWidth - 4; x += 8) {
            for (int y = panelTop + 6; y < panelTop + panelHeight - 4; y += 8) {
                graphics.fill(x, y, x + 1, y + 1, 0x242F383E);
            }
        }

        graphics.drawString(font,
                ScpFonts.montserrat(Component.translatable(
                        "screen.scp_additions.facility_sign_editor")),
                panelLeft + 18, panelTop + 13, TEXT_PRIMARY, false);
        graphics.drawString(font, ScpFonts.roboto(Component.translatable(
                        type == FacilitySignBlock.SignType.CORE_ROOM
                                ? "block.scp_additions.core_room_sign"
                                : "block.scp_additions.door_sign")),
                panelLeft + 18, panelTop + 28, TEXT_MUTED, false);

        int innerLeft = panelLeft + 18;
        int innerRight = panelLeft + panelWidth - 18;
        for (int index = 0; index < FacilitySignData.ENTRY_COUNT; index++) {
            int rowY = rowsTop + index * rowHeight;
            int rowColor = draggingRow == index ? 0xEE354047 : ROW_BACKGROUND;
            graphics.fill(innerLeft, rowY + 2, innerRight, rowY + 29, rowColor);
            outline(graphics, innerLeft, rowY + 2,
                    innerRight - innerLeft, 27,
                    draggingRow == index ? ACCENT : 0xFF3D464C);

            graphics.drawCenteredString(font, ScpFonts.roboto("≡"),
                    innerLeft + 11, rowY + 11,
                    draggingRow == index ? TEXT_PRIMARY : TEXT_MUTED);

            for (EditBox field : fieldsForRow(index)) {
                graphics.fill(field.getX() - 4, field.getY() - 3,
                        field.getX() + field.getWidth() + 4,
                        field.getY() + field.getHeight() + 3, FIELD_BACKGROUND);
                outline(graphics, field.getX() - 4, field.getY() - 3,
                        field.getWidth() + 8, field.getHeight() + 6, FIELD_EDGE);
            }

            String count = textFields.get(index).getValue()
                    .codePointCount(0, textFields.get(index).getValue().length())
                    + "/" + type.maxTextLength();
            graphics.drawString(font, ScpFonts.roboto(count),
                    textFields.get(index).getX()
                            + textFields.get(index).getWidth() + 8,
                    rowY + 11, TEXT_MUTED, false);
        }
    }

    private List<EditBox> fieldsForRow(int row) {
        if (!type.hasNumbers()) return List.of(textFields.get(row));
        return List.of(numberFields.get(row), textFields.get(row));
    }

    private List<FacilitySignData.Entry> currentEntries() {
        List<FacilitySignData.Entry> entries = new ArrayList<>();
        for (int index = 0; index < FacilitySignData.ENTRY_COUNT; index++) {
            String number = type.hasNumbers()
                    ? numberFields.get(index).getValue() : "";
            entries.add(new FacilitySignData.Entry(
                    number, textFields.get(index).getValue()));
        }
        return entries;
    }

    private void setEntries(List<FacilitySignData.Entry> entries) {
        for (int index = 0; index < FacilitySignData.ENTRY_COUNT; index++) {
            FacilitySignData.Entry entry = index < entries.size()
                    ? FacilitySignData.sanitize(type, entries.get(index))
                    : FacilitySignData.EMPTY_ENTRY;
            if (type.hasNumbers()) numberFields.get(index).setValue(entry.number());
            textFields.get(index).setValue(entry.text());
        }
    }

    private void copyEntry(int row) {
        List<FacilitySignData.Entry> entries = currentEntries();
        FacilitySignData.Entry clean =
                FacilitySignData.sanitize(type, entries.get(row));
        ItemStack screwdriver = currentScrewdriver();
        FacilitySignClipboard.copyEntry(screwdriver, type, clean);
        copiedEntry = new FacilitySignClipboard.EntryClipboard(type, clean);
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                FacilitySignClipboardPacket.entry(signPos, row, entries));
        setStatus("Entry copied to Screwdriver");
    }

    private void pasteEntry(int row) {
        if (copiedEntry == null) {
            setStatus("No copied entry");
            return;
        }
        FacilitySignData.Entry entry =
                FacilitySignData.sanitize(type, copiedEntry.entry());
        if (type.hasNumbers()) numberFields.get(row).setValue(entry.number());
        textFields.get(row).setValue(entry.text());
        setStatus("Entry pasted");
    }

    private void clearEntry(int row) {
        if (type.hasNumbers()) numberFields.get(row).setValue("");
        textFields.get(row).setValue("");
    }

    private void copyEntireSign() {
        List<FacilitySignData.Entry> entries = currentEntries();
        ItemStack screwdriver = currentScrewdriver();
        FacilitySignClipboard.copySign(screwdriver, type, entries);
        copiedSign = FacilitySignClipboard.readSign(screwdriver);
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                FacilitySignClipboardPacket.sign(signPos, entries));
        setStatus("Sign copied to Screwdriver");
    }

    private void pasteEntireSign() {
        if (copiedSign == null) {
            setStatus("No copied sign");
            return;
        }
        List<FacilitySignData.Entry> adapted = new ArrayList<>();
        for (FacilitySignData.Entry entry : copiedSign.entries()) {
            adapted.add(FacilitySignData.sanitize(type, entry));
        }
        setEntries(adapted);
        setStatus("Sign pasted");
    }

    private void clearAll() {
        setEntries(List.of());
    }

    private void saveAndClose() {
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new FacilitySignSavePacket(signPos, currentEntries()));
        onClose();
    }

    private ItemStack currentScrewdriver() {
        return minecraft != null && minecraft.player != null
                ? KeycardReaderInteractionEvents.screwdriver(minecraft.player)
                : ItemStack.EMPTY;
    }

    private void setStatus(String message) {
        status = message;
        statusExpiresAt = Util.getMillis() + 2200L;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int row = dragHandleAt(mouseX, mouseY);
            if (row >= 0) {
                draggingRow = row;
                setFocused(null);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        if (button == 0 && draggingRow >= 0) {
            int target = Math.max(0, Math.min(FacilitySignData.ENTRY_COUNT - 1,
                    (int) ((mouseY - rowsTop) / rowHeight)));
            if (target != draggingRow) {
                swapRows(draggingRow, target);
                draggingRow = target;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingRow >= 0) {
            draggingRow = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int dragHandleAt(double mouseX, double mouseY) {
        int left = panelLeft + 18;
        if (mouseX < left || mouseX > left + 22
                || mouseY < rowsTop
                || mouseY >= rowsTop + rowHeight * FacilitySignData.ENTRY_COUNT) {
            return -1;
        }
        return (int) ((mouseY - rowsTop) / rowHeight);
    }

    private void swapRows(int first, int second) {
        String firstText = textFields.get(first).getValue();
        textFields.get(first).setValue(textFields.get(second).getValue());
        textFields.get(second).setValue(firstText);
        if (type.hasNumbers()) {
            String firstNumber = numberFields.get(first).getValue();
            numberFields.get(first).setValue(numberFields.get(second).getValue());
            numberFields.get(second).setValue(firstNumber);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void outline(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private final class EditorButton extends AbstractButton {
        private final Runnable action;
        private final ButtonStyle style;

        private EditorButton(int x, int y, int width, int height,
                Component label, Runnable action) {
            this(x, y, width, height, label, ButtonStyle.PRIMARY, action);
        }

        private EditorButton(int x, int y, int width, int height,
                Component label, ButtonStyle style, Runnable action) {
            super(x, y, width, height, label);
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
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = !active ? 0xFF22282D
                    : isHoveredOrFocused() ? 0xFF56636B : 0xFF343D43;
            int edge = switch (style) {
                case PRIMARY -> isHoveredOrFocused() ? PRIMARY_TEXT : PRIMARY;
                case DANGER -> DANGER;
                case NEUTRAL -> isHoveredOrFocused() ? 0xFFD8E0E4 : 0xFF667178;
            };
            int text = !active ? TEXT_MUTED
                    : style == ButtonStyle.PRIMARY ? PRIMARY_TEXT : TEXT_PRIMARY;
            graphics.fill(getX(), getY(),
                    getX() + getWidth(), getY() + getHeight(), background);
            outline(graphics, getX(), getY(), getWidth(), getHeight(), edge);
            if (style != ButtonStyle.NEUTRAL) {
                graphics.fill(getX() + 1, getY() + 1,
                        getX() + 4, getY() + getHeight() - 1, edge);
            }
            graphics.drawCenteredString(font, ScpFonts.roboto(getMessage()),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2,
                    text);
        }
    }

    private enum ButtonStyle {
        NEUTRAL,
        PRIMARY,
        DANGER
    }
}
