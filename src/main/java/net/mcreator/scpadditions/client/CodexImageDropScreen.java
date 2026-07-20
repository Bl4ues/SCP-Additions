package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class CodexImageDropScreen extends Screen {
    private static final int MAX_IMAGE_BYTES = 2_500_000;
    private final Screen parent;
    private final boolean hasWorldImage;
    private final Consumer<ImportedImage> callback;
    private final Runnable clearCallback;
    private String status = "Drop one PNG, JPG, or JPEG file anywhere on this screen.";
    private int statusColor = 0xFFA9AFBA;
    private boolean uploading;

    public CodexImageDropScreen(Screen parent, boolean hasWorldImage,
                                Consumer<ImportedImage> callback,
                                Runnable clearCallback) {
        super(ScpFonts.roboto("Import Codex Image"));
        this.parent = parent;
        this.hasWorldImage = hasWorldImage;
        this.callback = callback;
        this.clearCallback = clearCallback;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(500, width - 24);
        int left = (width - panelWidth) / 2;
        int top = Math.max(12, (height - 240) / 2);
        int buttonWidth = (panelWidth - 56) / 2;
        Button remove = addRenderableWidget(Button.builder(
                ScpFonts.roboto("Remove World Image"), b -> {
                    if (clearCallback != null) clearCallback.run();
                }).bounds(left + 20, top + 190, buttonWidth, 22).build());
        remove.active = hasWorldImage;
        addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"),
                b -> Minecraft.getInstance().setScreen(parent))
                .bounds(left + 36 + buttonWidth, top + 190,
                        buttonWidth, 22).build());
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (uploading) return;
        if (paths == null || paths.size() != 1) {
            fail("Drop exactly one image file.");
            return;
        }
        Path path = paths.get(0);
        String name = path.getFileName() == null ? "image.png"
                : path.getFileName().toString();
        String lowerName = name.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".png") && !lowerName.endsWith(".jpg")
                && !lowerName.endsWith(".jpeg")) {
            fail("Only PNG, JPG, and JPEG files are accepted.");
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
                fail("Image must be between 1 byte and 2.5 MB.");
                return;
            }
            int imageWidth;
            int imageHeight;
            try (NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes))) {
                imageWidth = image.getWidth();
                imageHeight = image.getHeight();
            }
            if (imageWidth < 1 || imageHeight < 1
                    || imageWidth > 4096 || imageHeight > 4096) {
                fail("Image dimensions must be between 1 and 4096 pixels.");
                return;
            }
            uploading = true;
            status = "Uploading to this world...";
            statusColor = 0xFFE5D49A;
            CodexAssetClient.upload("image", name, bytes, key -> {
                uploading = false;
                if (callback != null) callback.accept(
                        new ImportedImage(key, imageWidth, imageHeight, name));
            }, message -> {
                uploading = false;
                fail(message);
            });
        } catch (Exception exception) {
            fail("Could not read that image: " + readable(exception));
        }
    }

    private void fail(String message) {
        status = message;
        statusColor = 0xFFD46060;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = Math.min(500, width - 24);
        int left = (width - panelWidth) / 2;
        int top = Math.max(12, (height - 240) / 2);
        graphics.fill(left, top, left + panelWidth, top + 225, 0xFF111317);
        graphics.fill(left, top, left + panelWidth, top + 34, 0xFF24282E);
        graphics.fill(left, top + 33, left + panelWidth, top + 34, 0xFFC59A2A);
        graphics.drawString(font, ScpFonts.montserrat("IMPORT CODEX IMAGE"),
                left + 18, top + 12, 0xFFF7F8FC, false);
        graphics.fill(left + 20, top + 52, left + panelWidth - 20,
                top + 164, 0xFF081022);
        Component drop = ScpFonts.roboto("DROP PNG / JPG / JPEG HERE");
        graphics.drawString(font, drop,
                left + (panelWidth - font.width(drop)) / 2,
                top + 88, 0xFFE5D49A, false);
        Component limit = ScpFonts.roboto(
                "Saved in this world · Maximum 2.5 MB · 4096 × 4096");
        graphics.drawString(font, limit,
                left + (panelWidth - font.width(limit)) / 2,
                top + 108, 0xFFA9AFBA, false);
        Component statusText = ScpFonts.roboto(status);
        graphics.drawString(font, statusText,
                left + (panelWidth - font.width(statusText)) / 2,
                top + 142, statusColor, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override
    public boolean isPauseScreen() { return false; }

    private static String readable(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName() : message;
    }

    public record ImportedImage(String key, int width, int height,
                                String fileName) { }
}
