from pathlib import Path

ROOT = Path('.')

def patch(path, old, new, count=1):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    found = text.count(old)
    if found != count:
        raise SystemExit(f'{path}: expected {count} occurrence(s), found {found}: {old[:120]!r}')
    p.write_text(text.replace(old, new, count), encoding='utf-8')

# 1) Native Configuration Center screens already draw their own descriptive/footer
# text. Do not draw the legacy reflective body-text pass on top of them.
path = 'src/main/java/net/mcreator/scpadditions/client/UnityConfigurationUiEvents.java'
patch(path,
'''    private static void renderKnownBodyText(GuiGraphics graphics, Screen screen) {
        PanelSpec spec = panelSpec(screen);
''',
'''    private static void renderKnownBodyText(GuiGraphics graphics, Screen screen) {
        // ConfigCenterClient screens already own their body copy and footnotes.
        // Rendering the legacy reflective pass as well produced duplicate text,
        // cover rectangles outside the modern panel and overlapping summaries.
        if (screen.getClass().getName().startsWith(
                "net.mcreator.scpadditions.config.ui.ConfigCenterClient$")) return;
        PanelSpec spec = panelSpec(screen);
''')
patch(path, '    private static boolean renderEntityPreview(',
            '    public static boolean renderEntityPreview(')

# 2) Configuration Center: remove obsolete scroll hint, make the save notice use
# the actual composition instead of the bottom edge, and hard-lock Codex writes
# to an active world.
path = 'src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterClient.java'
patch(path,
'''            graphics.drawString(font, "Mouse wheel: scroll options", x + w - 160, y + 31, MUTED, false);
''', '')
patch(path,
'''            if (!homeNotice.isBlank()) {
                graphics.drawString(font, ScpFonts.roboto(compact(homeNotice, 72)),
                        x + 8, height - 20, GOOD, false);
            }
''',
'''            if (!homeNotice.isBlank()) {
                int noticeY = Math.max(58, Math.round(height * 0.165F));
                graphics.drawString(font, ScpFonts.roboto(compact(homeNotice, 72)),
                        x + 8, noticeY, GOOD, false);
            }
''')
patch(path,
'''    private static void submitCodex(JsonObject inventoryRoot, PendingCodexGive give) {
        returnToCodexAfterSave = true;
        pendingCodexGive = give;
        submit(Map.of(ConfigCenterService.INVENTORY, inventoryRoot));
    }

    private record PendingCodexGive(String itemId, String codexId, String displayName) {
''',
'''    private static void submitCodex(JsonObject inventoryRoot, PendingCodexGive give) {
        if (!codexEditingAvailable()) {
            returnToCodexAfterSave = false;
            pendingCodexGive = null;
            Minecraft.getInstance().setScreen(new MessageScreen(
                    rootParent, "Codex Documents",
                    "Codex editing is available only while a world is open.", false));
            return;
        }
        returnToCodexAfterSave = true;
        pendingCodexGive = give;
        submit(Map.of(ConfigCenterService.INVENTORY, inventoryRoot));
    }

    private static boolean codexEditingAvailable() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.getConnection() != null
                && !(rootParent instanceof net.mcreator.scpadditions.client.CustomMainMenuScreen);
    }

    private record PendingCodexGive(String itemId, String codexId, String displayName) {
''')
patch(path,
'''            addRenderableWidget(Button.builder(Component.literal("Codex Documents"),
                    b -> Minecraft.getInstance().setScreen(new CodexListScreen(this, working)))
                    .bounds(x, y, bw, 24).build());
''',
'''            Button codexButton = addRenderableWidget(Button.builder(
                            Component.literal(codexEditingAvailable()
                                    ? "Codex Documents"
                                    : "Codex Documents (In-World Only)"),
                    b -> Minecraft.getInstance().setScreen(new CodexListScreen(this, working)))
                    .bounds(x, y, bw, 24).build());
            codexButton.active = codexEditingAvailable();
''')

# 3) Put Done in the header instead of letting it hug the lower border.
path = 'src/main/java/net/mcreator/scpadditions/config/ui/ConfigurationHomePolish.java'
patch(path,
'''            if (done != null) place(done, l.infoX, toolY, l.infoWidth, footerHeight);
''',
'''            if (done != null) {
                place(done, l.infoX + l.infoWidth - 118,
                        l.headerY + 4, 118, 30);
            }
''')

# 4) Contextual Interactions: restore block icons and reuse the entity preview
# renderer already used by the SCP-914 editor.
path = 'src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterModernWidgetEvents.java'
patch(path,
'''import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
''',
'''import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
''')
patch(path,
'''import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
''',
'''import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.UnityConfigurationUiEvents;
''')
patch(path,
'''        int textX = tagX + tagWidth;
        int available = Math.max(20, right - textX - 12);
''',
'''        int iconX = tagX + tagWidth;
        int iconY = top + 8;
        boolean hasTargetIcon = renderContextTargetIcon(graphics, rule, iconX, iconY);
        int textX = iconX + (hasTargetIcon ? 20 : 0);
        int available = Math.max(20, right - textX - 12);
''')
patch(path,
'''    private static void polishLegacySummaryRows(GuiGraphics graphics,
''',
'''    private static boolean renderContextTargetIcon(GuiGraphics graphics,
            JsonObject rule, int x, int y) {
        String idText = jsonString(rule, "id", "");
        if (idText.isBlank()) return false;
        try {
            ResourceLocation id = new ResourceLocation(idText);
            String type = jsonString(rule, "type", "");
            if ("block".equalsIgnoreCase(type)) {
                var block = ForgeRegistries.BLOCKS.getValue(id);
                if (block == null || block.asItem() == Items.AIR) return false;
                graphics.renderItem(new ItemStack(block.asItem()), x, y);
                return true;
            }
            if ("entity".equalsIgnoreCase(type)) {
                if (UnityConfigurationUiEvents.renderEntityPreview(graphics, id, x, y)) {
                    return true;
                }
                graphics.renderItem(new ItemStack(Items.SPAWNER), x, y);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static void polishLegacySummaryRows(GuiGraphics graphics,
''')

# 5) Difficulty flyout: same design, less visual mass.
path = 'src/main/java/net/mcreator/scpadditions/client/PauseMenuSettingsPanelClient.java'
patch(path,
'''            int iconSize = Math.min(34, flyout.optionHeight - 10);
''',
'''            int iconSize = Math.min(30, flyout.optionHeight - 10);
''')
patch(path,
'''                    textX, optionY + 9,
''',
'''                    textX, optionY + 7,
''')
patch(path,
'''                    textX, optionY + 23, applyAlpha(MUTED, alpha), false);
''',
'''                    textX, optionY + 20, applyAlpha(MUTED, alpha), false);
''')
patch(path,
'''        int width = Mth.clamp(Math.round(screen.width * 0.19F), 172, 224);
        int optionHeight = Mth.clamp(layout.rowHeight + 12, 44, 52);
        int gap = 4;
        int padding = 6;
''',
'''        int width = Mth.clamp(Math.round(screen.width * 0.165F), 158, 196);
        int optionHeight = Mth.clamp(layout.rowHeight + 8, 40, 46);
        int gap = 3;
        int padding = 5;
''')

# 6) Chair collision: the shape/orientation are finally correct. Shift only the
# authored local X position a small amount to the chair's actual footprint.
path = 'src/main/java/net/mcreator/scpadditions/facility/ArchivistsChairBlock.java'
patch(path,
'''        return box(2.0D + minX, minY, 8.0D + minZ,
                2.0D + maxX, maxY, 8.0D + maxZ);
''',
'''        return box(3.5D + minX, minY, 8.0D + minZ,
                3.5D + maxX, maxY, 8.0D + maxZ);
''')

print('Configuration UI cleanup applied successfully.')
