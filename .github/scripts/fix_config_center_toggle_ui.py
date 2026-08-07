from pathlib import Path


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


# -----------------------------------------------------------------------------
# ConfigCenterClient: let the contextual row render itself again, use Roboto,
# keep the save notice below the final button row, and improve untranslated names.
# -----------------------------------------------------------------------------
path = Path('src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterClient.java')
text = path.read_text(encoding='utf-8')

text = replace_once(
    text,
    'import com.google.gson.JsonParser;\n',
    'import com.google.gson.JsonParser;\nimport com.bl4ues.scpinventory.client.ScpFonts;\n',
    'ScpFonts import')

text = replace_once(
    text,
    'graphics.drawString(font, compact(homeNotice, 62), x + 14, y + h - 40, GOOD, false);',
    'graphics.drawString(font, compact(homeNotice, 62), x + 14, y + h - 16, GOOD, false);',
    'home notice position')

old = '''    private static String compact(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 3)) + "...";
    }
'''
new = '''    private static String compact(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static String readableResourceId(String raw) {
        if (raw == null || raw.isBlank()) return "Unknown";
        String path = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        StringBuilder out = new StringBuilder();
        for (String part : path.split("[_/.-]+")) {
            if (part.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1));
        }
        return out.isEmpty() ? raw : out.toString();
    }
'''
text = replace_once(text, old, new, 'readable resource helper')

old = '''          if (item != null && item != Items.AIR) {
              return new ItemStack(item).getHoverName().getString();
          }
          return block.getName().getString();
'''
new = '''          if (item != null && item != Items.AIR) {
              String resolved = new ItemStack(item).getHoverName().getString();
              if (!resolved.startsWith("item.") && !resolved.startsWith("block.")) {
                  return resolved;
              }
          }
          String resolved = block.getName().getString();
          return resolved.startsWith("block.")
                  ? readableResourceId(idText) : resolved;
'''
text = replace_once(text, old, new, 'context block display fallback')

old = '''      var entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
      if (entityType != null) {
          return entityType.getDescription().getString();
      }
'''
new = '''      var entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
      if (entityType != null) {
          String resolved = entityType.getDescription().getString();
          return resolved.startsWith("entity.")
                  ? readableResourceId(idText) : resolved;
      }
'''
text = replace_once(text, old, new, 'context entity display fallback')

old = '''  graphics.drawCenteredString(rowFont, type,
          typeX + 29, typeY + 5, enabled ? TEXT : 0xFF707680);
'''
new = '''  graphics.drawCenteredString(rowFont, ScpFonts.roboto(type),
          typeX + 29, typeY + 5, enabled ? TEXT : 0xFF707680);
'''
text = replace_once(text, old, new, 'context type Roboto')

old = '''  graphics.drawString(rowFont, rowFont.plainSubstrByWidth(
                  main + (enabled ? "" : "  [DISABLED]"), available),
          textX, getY() + 5, enabled ? TEXT : 0xFF777E89, false);

  String source = contextSourceLabel(row.view().source());
  int sourceColor = contextSourceColor(row.view().source());
  graphics.drawString(rowFont, source, textX, getY() + 18,
          sourceColor, false);
  int metaX = textX + rowFont.width(source) + 7;
'''
new = '''  String mainVisible = rowFont.plainSubstrByWidth(
          main + (enabled ? "" : "  [DISABLED]"), available);
  graphics.drawString(rowFont, ScpFonts.roboto(mainVisible),
          textX, getY() + 5, enabled ? TEXT : 0xFF777E89, false);

  String source = contextSourceLabel(row.view().source());
  int sourceColor = contextSourceColor(row.view().source());
  Component sourceLabel = ScpFonts.roboto(source);
  graphics.drawString(rowFont, sourceLabel, textX, getY() + 18,
          sourceColor, false);
  int metaX = textX + rowFont.width(sourceLabel) + 7;
'''
text = replace_once(text, old, new, 'context main/source Roboto')

old = '''      graphics.drawString(rowFont,
              rowFont.plainSubstrByWidth(meta.toString(), metaAvailable),
              metaX, getY() + 18, MUTED, false);
'''
new = '''      graphics.drawString(rowFont, ScpFonts.roboto(
                      rowFont.plainSubstrByWidth(meta.toString(), metaAvailable)),
              metaX, getY() + 18, MUTED, false);
'''
text = replace_once(text, old, new, 'context metadata Roboto')

path.write_text(text, encoding='utf-8')


# -----------------------------------------------------------------------------
# UnityConfigurationUiEvents: do not repaint the custom contextual row as a
# generic button; reserve real space for every Enable/Disable button; align the
# changed ID list geometry; show semantic SCP-294 drink results.
# -----------------------------------------------------------------------------
path = Path('src/main/java/net/mcreator/scpadditions/client/UnityConfigurationUiEvents.java')
text = path.read_text(encoding='utf-8')

old = '''            if (listener instanceof EditBox editBox) styleEditBox(editBox);
            if (listener instanceof AbstractButton button) {
                Component current = button.getMessage();
'''
new = '''            if (listener instanceof EditBox editBox) styleEditBox(editBox);
            if (listener instanceof AbstractButton button) {
                if (isSelfRenderedButton(button)) continue;
                Component current = button.getMessage();
'''
text = replace_once(text, old, new, 'skip custom button in render pre')

old = '''            if (!(listener instanceof AbstractButton button) || !button.visible) continue;
            Component label = labelFor(button);
'''
new = '''            if (!(listener instanceof AbstractButton button) || !button.visible
                    || isSelfRenderedButton(button)) continue;
            Component label = labelFor(button);
'''
text = replace_once(text, old, new, 'skip custom button in render post')

text = replace_once(
    text,
    '        if ("ContextListScreen".equals(name)) renderContextRows(graphics, screen, mouseX, mouseY);\n',
    '',
    'remove redundant context overlay')

old = '''    private static void prepareWidget(GuiEventListener listener) {
        if (listener instanceof AbstractSliderButton slider) {
'''
new = '''    private static void prepareWidget(GuiEventListener listener) {
        if (listener instanceof AbstractButton button && isSelfRenderedButton(button)) return;
        if (listener instanceof AbstractSliderButton slider) {
'''
text = replace_once(text, old, new, 'prepare custom button skip')

old = '''    private static boolean isConfigurationScreen(Screen screen) {
'''
new = '''    private static boolean isSelfRenderedButton(AbstractButton button) {
        return button != null && "ContextRowButton".equals(
                button.getClass().getSimpleName());
    }

    private static boolean isConfigurationScreen(Screen screen) {
'''
text = replace_once(text, old, new, 'self rendered helper')

text = replace_once(
    text,
    '            case "IdListScreen" -> { w = Math.min(600, screen.width - 18); h = Math.min(380, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }',
    '            case "IdListScreen" -> { w = Math.min(650, screen.width - 18); h = Math.min(390, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }',
    'id panel geometry')

text = replace_once(
    text,
    '            case "ContextListScreen" -> { w = Math.min(700, screen.width - 16); h = Math.min(410, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }',
    '            case "ContextListScreen" -> { w = Math.min(760, screen.width - 16); h = Math.min(450, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }',
    'context panel geometry')

# First rowRight 150 is the drink list.
text = replace_once(
    text,
    '        int rowRight = x + w - 150;\n',
    '        int rowRight = x + w - 238;\n',
    'drink row right edge')

# Item rules gained an 86px state column.
text = replace_once(
    text,
    '        int rowRight = x + w - 78;\n',
    '        int rowRight = x + w - 164;\n',
    'item rule row right edge')

# Recipe main card must stop before the state button, not before Copy.
text = replace_once(
    text,
    '        int rowRight = x + w - 154;\n',
    '        int rowRight = x + w - 242;\n',
    'recipe row right edge')

old = '''            JsonObject result = childObject(drink, "result");
            String resultName = result == null ? "No item result"
                    : displayNameForItem(string(result, "item", ""));
            int count = result == null ? 1 : integer(result, "count", 1);
            String detail = "→ " + resultName + (count > 1 ? " ×" + count : "");
'''
new = '''            JsonObject result = childObject(drink, "result");
            String resultItem = result == null ? "" : string(result, "item", "");
            String resultName;
            if (!bool(drink, "give_result", true)) {
                resultName = "No cup dispensed";
            } else if ("scp_additions:cup_of_coffee".equals(resultItem)) {
                // The generic cup is only a carrier for the configured drink profile.
                // Show the actual semantic drink here instead of claiming that every
                // SCP-294 entry produces coffee.
                resultName = humanizeId(string(drink, "id", title));
            } else {
                resultName = result == null ? "No item result"
                        : displayNameForItem(resultItem);
            }
            int count = result == null ? 1 : integer(result, "count", 1);
            String detail = "→ " + resultName + (count > 1 ? " ×" + count : "");
'''
text = replace_once(text, old, new, '294 semantic result label')

# Replace stale IdList overlay that still expected List<String> and old geometry.
start = text.index('    private static void renderIdRows(GuiGraphics graphics, Screen screen,')
end = text.index('    private static void renderDrinkEffectRows(', start)
new_method = '''    private static void renderIdRows(GuiGraphics graphics, Screen screen,
                                     int mouseX, int mouseY) {
        List<?> filtered = readList(screen, "filtered");
        int scroll = integerField(screen, "scroll", 0);
        String key = readField(screen, "key", String.class);
        int w = Math.min(650, screen.width - 18);
        int x = (screen.width - w) / 2 + 12;
        int top = Math.max(8, (screen.height - Math.min(390, screen.height - 16)) / 2) + 38;
        int listY = top + 56;
        int visible = Math.max(4, Math.min(9, (screen.height - 146) / 24));
        int rowRight = x + w - 178;
        Font font = Minecraft.getInstance().font;
        boolean effects = "hidden_status_effects".equals(key);

        for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
            Object entry = filtered.get(i);
            String id = entry instanceof String textValue ? textValue
                    : readField(entry, "value", String.class);
            if (id == null || id.isBlank()) continue;
            Boolean enabledField = readField(entry, "enabled", Boolean.class);
            boolean enabled = enabledField == null || enabledField;
            int rowY = listY + (i - scroll) * 24;
            drawSummaryCard(graphics, x, rowY, rowRight, rowY + 20,
                    mouseX, mouseY);
            if (!enabled) {
                graphics.fill(x + 1, rowY + 1, rowRight - 1, rowY + 19,
                        0x66000000);
            }
            String badge = effects ? "EFFECT" : "TARGET";
            drawBadge(graphics, x + 4, rowY + 3, x + 62, rowY + 17,
                    badge, effects ? ENTITY_BADGE : BLOCK_BADGE,
                    enabled ? WHITE : MUTED);
            String display = effects ? displayNameForEffect(id)
                    : id.startsWith("#") ? "Tag: " + humanizeId(id.substring(1))
                    : displayNameForEntity(id);
            if (!enabled) display += "  [Disabled]";
            graphics.enableScissor(x + 66, rowY, rowRight - 5, rowY + 20);
            graphics.drawString(font, ScpFonts.roboto(display), x + 69, rowY + 6,
                    enabled ? WHITE : MUTED, false);
            graphics.disableScissor();
        }
    }

'''
text = text[:start] + new_method + text[end:]

old = '''                coverTextLine(graphics, spec.x() + 10, spec.y() + spec.height() - 44,
                        spec.width() - 20);
                graphics.drawString(font, ScpFonts.roboto(compact(notice, 66)),
                        spec.x() + 14, spec.y() + spec.height() - 40, 0xFF79D58B, false);
'''
new = '''                coverTextLine(graphics, spec.x() + 10, spec.y() + spec.height() - 22,
                        spec.width() - 20);
                graphics.drawString(font, ScpFonts.roboto(compact(notice, 66)),
                        spec.x() + 14, spec.y() + spec.height() - 16, 0xFF79D58B, false);
'''
text = replace_once(text, old, new, 'home notice Unity position')

old = '''    private static String displayNameForBlock(String idText) {
        ResourceLocation id = ResourceLocation.tryParse(idText == null ? "" : idText);
        Block block = id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
        return block == null ? humanizeId(idText) : block.getName().getString();
    }
'''
new = '''    private static String displayNameForBlock(String idText) {
        ResourceLocation id = ResourceLocation.tryParse(idText == null ? "" : idText);
        Block block = id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
        if (block == null) return humanizeId(idText);
        String resolved = block.getName().getString();
        return resolved.startsWith("block.") ? humanizeId(idText) : resolved;
    }
'''
text = replace_once(text, old, new, 'block translation fallback')

old = '''    private static String displayNameForEntity(String idText) {
        ResourceLocation id = ResourceLocation.tryParse(idText == null ? "" : idText);
        net.minecraft.world.entity.EntityType<?> type = id == null
                ? null : ForgeRegistries.ENTITY_TYPES.getValue(id);
        return type == null ? humanizeId(idText) : type.getDescription().getString();
    }
'''
new = '''    private static String displayNameForEntity(String idText) {
        ResourceLocation id = ResourceLocation.tryParse(idText == null ? "" : idText);
        net.minecraft.world.entity.EntityType<?> type = id == null
                ? null : ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) return humanizeId(idText);
        String resolved = type.getDescription().getString();
        return resolved.startsWith("entity.") ? humanizeId(idText) : resolved;
    }
'''
text = replace_once(text, old, new, 'entity translation fallback')

path.write_text(text, encoding='utf-8')


# -----------------------------------------------------------------------------
# Changelog: document the regression fix in the active 3.1.0 section.
# -----------------------------------------------------------------------------
path = Path('CHANGELOG.md')
text = path.read_text(encoding='utf-8')
needle = '- Added reversible **Enable/Disable** controls for compatible multi-entry Configuration Center lists; disabled entries remain visible but dimmed and can be restored without recreating them, including integrated and custom Roomba spawn floors, SCP-173 targets, hidden status effects, item rules, Codex documents, contextual interactions, SCP-294 drinks, and SCP-914 recipes;\n'
replacement = needle + '- Fixed Configuration Center presentation regressions from reversible entry toggles: Contextual Interaction previews and Roboto row styling are restored, Enable/Disable controls remain visibly separated from row summaries, SCP-294 rows describe their actual configured drink profile instead of the generic carrier cup, and save confirmation text no longer overlaps the home controls;\n'
text = replace_once(text, needle, replacement, 'changelog entry')
path.write_text(text, encoding='utf-8')
