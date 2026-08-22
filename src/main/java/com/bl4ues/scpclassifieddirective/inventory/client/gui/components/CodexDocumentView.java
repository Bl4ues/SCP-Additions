package com.bl4ues.scpclassifieddirective.inventory.client.gui.components;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.inventory.item.CodexDocumentDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.bl4ues.scpclassifieddirective.client.CodexAssetClient;
import com.bl4ues.scpclassifieddirective.client.DocumentRenderer;
import com.bl4ues.scpclassifieddirective.client.MarkdownTextRenderer;
import com.bl4ues.scpclassifieddirective.document.DocumentData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Shared page and Markdown rendering kept separate from Codex list interaction. */
final class CodexDocumentView {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int TEXT_BODY = 0xF2B2B3B3;

    private CodexDocumentView() { }

    static void renderPage(GuiGraphics g, ItemStack stack,
                           CodexDocumentDefinition definition,
                           int x, int y, int width, int height) {
        if (DocumentData.hasStructuredData(stack)) {
            DocumentRenderer.render(g, stack, x, y, width, height);
            return;
        }
        ResourceLocation texture = CodexAssetClient.getTexture(definition.getWorldImageKey())
                .orElseGet(() -> definition.getImageLocation().orElse(null));
        if (texture == null) {
            placeholder(g, definition, x, y, width, height);
            return;
        }
        int[] fit = fit(definition.getImageWidth(), definition.getImageHeight(), width, height);
        int drawX = x + (width - fit[0]) / 2;
        int drawY = y + (height - fit[1]) / 2;
        g.enableScissor(x, y, x + width, y + height);
        MC.getTextureManager().getTexture(texture).setFilter(true, false);
        g.blit(texture, drawX, drawY, fit[0], fit[1], 0, 0,
                definition.getImageWidth(), definition.getImageHeight(),
                definition.getImageWidth(), definition.getImageHeight());
        MC.getTextureManager().getTexture(texture).setFilter(false, false);
        g.disableScissor();
    }

    static String text(ItemStack stack, CodexDocumentDefinition definition) {
        if (DocumentData.hasStructuredData(stack)) return DocumentData.read(stack).body();
        String worldKey = definition.getWorldTextKey();
        Optional<String> worldText = CodexAssetClient.getText(worldKey);
        if (worldText.isPresent()) return worldText.get();
        if (!worldKey.isBlank()) {
            return CodexAssetClient.isMissing("text", worldKey)
                    ? "The saved document Markdown could not be loaded."
                    : "Loading document Markdown...";
        }
        ResourceLocation location = definition.getTextLocation().orElse(null);
        if (location != null) {
            Optional<String> packaged = MC.getResourceManager().getResource(location).flatMap(resource -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        resource.open(), StandardCharsets.UTF_8))) {
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!result.isEmpty()) result.append('\n');
                        result.append(line);
                    }
                    return Optional.of(result.toString());
                } catch (IOException ignored) {
                    return Optional.empty();
                }
            });
            if (packaged.isPresent()) return packaged.get();
        }
        return definition.getDisplayName(stack)
                + "\n\nNo Markdown transcription configured for this document.";
    }

    static int lineCount(String markdown, int width) {
        return MarkdownTextRenderer.measureLines(markdown, width, 1.0F, MC.font);
    }

    static void renderMarkdown(GuiGraphics g, String markdown, int x, int y,
                               int width, int height, int scrollLines,
                               int lineHeight) {
        g.enableScissor(x, y, x + width, y + height);
        int shiftedY = y - scrollLines * lineHeight;
        MarkdownTextRenderer.renderMonochrome(g, markdown, x, shiftedY,
                shiftedY + height + scrollLines * lineHeight,
                lineHeight, 1.0F, ignored -> width, TEXT_BODY);
        g.disableScissor();
    }

    private static void placeholder(GuiGraphics g, CodexDocumentDefinition definition,
                                    int x, int y, int width, int height) {
        String key = definition.getWorldImageKey();
        String message = key.isBlank() ? "No document image attached"
                : CodexAssetClient.isMissing("image", key)
                ? "Document image unavailable" : "Loading document image...";
        g.fill(x, y, x + width, y + height, 0x3320262B);
        Component label = ScpFonts.roboto(message);
        g.drawString(MC.font, label, x + Math.max(4, (width - MC.font.width(label)) / 2),
                y + Math.max(4, (height - MC.font.lineHeight) / 2), TEXT_GRAY, false);
    }

    private static int[] fit(int sourceWidth, int sourceHeight, int width, int height) {
        float scale = Math.min(width / (float) Math.max(1, sourceWidth),
                height / (float) Math.max(1, sourceHeight));
        return new int[]{Math.max(1, Math.round(sourceWidth * scale)),
                Math.max(1, Math.round(sourceHeight * scale))};
    }
}
