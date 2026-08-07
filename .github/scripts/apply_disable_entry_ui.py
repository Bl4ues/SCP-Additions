from pathlib import Path


def once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


# -----------------------------------------------------------------------------
# Roomba floor catalog
# -----------------------------------------------------------------------------
path = Path("src/main/java/net/mcreator/scpadditions/client/RoombaConfigCenterEnhancements.java")
text = path.read_text(encoding="utf-8")
text = once(
    text,
    "    private record FloorEntry(ResourceLocation id, boolean integrated) {\n    }",
    "    private record FloorEntry(ResourceLocation id, boolean integrated,\n            boolean enabled) {\n    }",
    "roomba floor record",
)

start = text.index("        private void addRemoveButtons(int x, int listY, int listWidth) {")
end = text.index("        private void added(ResourceLocation id) {", start)
replacement = r'''        private void addRemoveButtons(int x, int listY, int listWidth) {
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

'''
text = text[:start] + replacement + text[end:]

old_render = '''                graphics.fill(x, rowY, x + listWidth, rowY + 36,
                        row % 2 == 0 ? ROW : ROW_ALT);
                graphics.fill(x, rowY, x + listWidth, rowY + 1, BORDER);
                graphics.fill(x, rowY + 35, x + listWidth, rowY + 36, BORDER);
                graphics.fill(x, rowY, x + 4, rowY + 36,
                        entry.integrated() ? GOOD : ACCENT);
                drawBlockIcon(graphics, entry.id(), x + 10, rowY + 10);
                graphics.drawString(font, ScpFonts.roboto(blockName(entry.id())),
                        x + 34, rowY + 7, TEXT, false);
                graphics.drawString(font, ScpFonts.roboto(entry.id().toString()),
                        x + 34, rowY + 20, MUTED, false);
                if (entry.integrated()) {
                    String badge = "INTEGRATED";
                    graphics.drawString(font, ScpFonts.roboto(badge),
                            x + listWidth - 16 - font.width(badge),
                            rowY + 14, GOOD, false);
                }'''
new_render = '''                int rowColor = entry.enabled()
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
                        rowY + 14, badgeColor, false);'''
text = once(text, old_render, new_render, "roomba disabled render")
path.write_text(text, encoding="utf-8")


# -----------------------------------------------------------------------------
# Main Configuration Center lists
# -----------------------------------------------------------------------------
path = Path("src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterClient.java")
text = path.read_text(encoding="utf-8")

# Generic ID list editor: SCP-173 targets and hidden effects.
a = text.index("    private static final class IdListScreen extends ConfigScreen {")
b = text.index("    private static final class CodexListScreen extends ConfigScreen {", a)
id_block = r'''    private record IdEntry(String value, boolean enabled) {
    }

    private static final class IdListScreen extends ConfigScreen {
        private final JsonObject root;
        private final String key;
        private final boolean allowTag;
        private final EditBox valueBox;
        private final EditBox searchBox;
        private List<IdEntry> filtered = List.of();
        private int scroll;

        private IdListScreen(Screen parent, JsonObject root, String key,
                String title, boolean allowTag) {
            super(parent, title);
            this.root = root;
            this.key = key;
            this.allowTag = allowTag;
            this.valueBox = new EditBox(Minecraft.getInstance().font,
                    0, 0, 100, 20, Component.literal("Resource ID"));
            this.searchBox = new EditBox(Minecraft.getInstance().font,
                    0, 0, 100, 20, Component.literal("Search"));
        }

        @Override
        protected void init() {
            int w = Math.min(650, width - 18);
            int x = left(width, w) + 12;
            int y = Math.max(8,
                    (height - Math.min(390, height - 16)) / 2) + 38;
            searchBox.setX(x);
            searchBox.setY(y);
            searchBox.setWidth(w - 24);
            searchBox.setHint(Component.literal("Search configured IDs or state"));
            searchBox.setMaxLength(256);
            searchBox.setResponder(value -> { scroll = 0; rebuildRows(); });
            addRenderableWidget(searchBox);
            y += 28;
            valueBox.setX(x);
            valueBox.setY(y);
            valueBox.setWidth(w - 118);
            valueBox.setHint(Component.literal(allowTag
                    ? "namespace:id or #namespace:tag" : "namespace:id"));
            valueBox.setMaxLength(256);
            addRenderableWidget(valueBox);
            addRenderableWidget(Button.builder(Component.literal("Add"),
                    button -> addValue()).bounds(x + w - 106, y, 94, 20).build());
            rebuildRows();
        }

        private String entryValue(JsonElement element) {
            if (element == null || element.isJsonNull()) return "";
            if (element.isJsonPrimitive()) return element.getAsString().trim();
            if (!element.isJsonObject()) return "";
            JsonObject object = element.getAsJsonObject();
            for (String candidate : List.of("id", "entity", "effect", "tag")) {
                if (object.has(candidate) && object.get(candidate).isJsonPrimitive()) {
                    String value = object.get(candidate).getAsString().trim();
                    if (!value.isBlank()) return value;
                }
            }
            return "";
        }

        private boolean entryEnabled(JsonElement element) {
            if (element == null || !element.isJsonObject()) return true;
            return bool(element.getAsJsonObject(), "enabled", true);
        }

        private void addValue() {
            String value = valueBox.getValue().trim();
            if (value.isEmpty()) return;
            String check = allowTag && value.startsWith("#")
                    ? value.substring(1) : value;
            try { new ResourceLocation(check); }
            catch (Exception ignored) { valueBox.setTextColor(BAD); return; }
            JsonArray values = array(root, key);
            for (JsonElement element : values) {
                if (value.equals(entryValue(element))) return;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("id", value);
            entry.addProperty("enabled", true);
            values.add(entry);
            valueBox.setValue("");
            valueBox.setTextColor(TEXT);
            rebuildRows();
        }

        private void rebuildRows() {
            List<IdEntry> values = new ArrayList<>();
            String needle = searchBox.getValue().toLowerCase(Locale.ROOT).trim();
            for (JsonElement element : array(root, key)) {
                String value = entryValue(element);
                if (value.isBlank()) continue;
                boolean enabled = entryEnabled(element);
                String state = enabled ? "enabled" : "disabled";
                if (needle.isEmpty()
                        || value.toLowerCase(Locale.ROOT).contains(needle)
                        || state.contains(needle)) {
                    values.add(new IdEntry(value, enabled));
                }
            }
            values.sort(Comparator.comparing(IdEntry::value,
                    String.CASE_INSENSITIVE_ORDER));
            filtered = values;
            refreshRows();
        }

        private void toggle(IdEntry entry) {
            JsonArray values = array(root, key);
            for (int i = 0; i < values.size(); i++) {
                JsonElement element = values.get(i);
                if (!entry.value().equals(entryValue(element))) continue;
                if (element.isJsonObject()) {
                    element.getAsJsonObject().addProperty("enabled", !entry.enabled());
                } else {
                    JsonObject replacement = new JsonObject();
                    replacement.addProperty("id", entry.value());
                    replacement.addProperty("enabled", !entry.enabled());
                    values.set(i, replacement);
                }
                rebuildRows();
                return;
            }
        }

        private void remove(IdEntry entry) {
            JsonArray values = array(root, key);
            for (int index = values.size() - 1; index >= 0; index--) {
                if (entry.value().equals(entryValue(values.get(index)))) {
                    values.remove(index);
                }
            }
            rebuildRows();
        }

        private void refreshRows() {
            clearWidgets();
            int w = Math.min(650, width - 18);
            int x = left(width, w) + 12;
            int top = Math.max(8,
                    (height - Math.min(390, height - 16)) / 2) + 38;
            searchBox.setX(x); searchBox.setY(top);
            searchBox.setWidth(w - 24); addRenderableWidget(searchBox);
            int y = top + 28;
            valueBox.setX(x); valueBox.setY(y);
            valueBox.setWidth(w - 118); addRenderableWidget(valueBox);
            addRenderableWidget(Button.builder(Component.literal("Add"),
                    button -> addValue()).bounds(x + w - 106, y, 94, 20).build());
            int listY = y + 28;
            int visible = Math.max(4, Math.min(9, (height - 146) / 24));
            scroll = Math.min(scroll, Math.max(0, filtered.size() - visible));
            for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
                IdEntry entry = filtered.get(i);
                int row = i - scroll;
                Component label = Component.literal(entry.value()
                        + (entry.enabled() ? "" : "  [DISABLED]"));
                if (!entry.enabled()) {
                    label = label.copy().withStyle(net.minecraft.ChatFormatting.DARK_GRAY);
                }
                addRenderableWidget(Button.builder(label,
                        button -> valueBox.setValue(entry.value()))
                        .bounds(x, listY + row * 24, w - 178, 20).build());
                addRenderableWidget(Button.builder(Component.literal(
                                entry.enabled() ? "Disable" : "Enable"),
                        button -> toggle(entry))
                        .bounds(x + w - 170, listY + row * 24, 84, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Remove"),
                        button -> remove(entry))
                        .bounds(x + w - 80, listY + row * 24, 68, 20).build());
            }
            int bottom = Math.min(height - 28,
                    listY + visible * 24 + 6);
            addRenderableWidget(Button.builder(Component.literal("Back"),
                    button -> goBack()).bounds(x + w - 92, bottom, 80, 20).build());
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int visible = Math.max(4, Math.min(9, (height - 146) / 24));
            int next = Math.max(0, Math.min(
                    Math.max(0, filtered.size() - visible),
                    scroll + (delta < 0 ? 1 : -1)));
            if (next != scroll) { scroll = next; refreshRows(); return true; }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            renderBackground(graphics);
            int w = Math.min(650, width - 18);
            int h = Math.min(390, height - 16);
            int x = left(width, w);
            int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            int active = 0;
            for (JsonElement element : array(root, key)) {
                if (entryEnabled(element)) active++;
            }
            graphics.drawString(font,
                    active + " active · " + array(root, key).size()
                            + " configured value(s)",
                    x + 12, y + h - 17, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

'''
text = text[:a] + id_block + text[b:]

# New item rules start enabled.
text = once(
    text,
    '''                rule.addProperty("id", id);
                rule.addProperty("type", "MISCELLANEOUS");''',
    '''                rule.addProperty("id", id);
                rule.addProperty("type", "MISCELLANEOUS");
                rule.addProperty("enabled", true);''',
    "item rule enabled default",
)

# Item rule list quick toggle and dimming.
old = '''                addRenderableWidget(Button.builder(Component.literal(compact(id, 45) + "  [" + type + "]"),
                        b -> Minecraft.getInstance().setScreen(new ItemRuleDetailScreen(this, root, rule)))
                        .bounds(x, listY + row * 24, w - 78, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                    removeIdentity(array(root, "item_rules"), rule);
                    removeItemEffects(root, id);
                    rebuildRows();
                }).bounds(x + w - 70, listY + row * 24, 58, 20).build());'''
new = '''                boolean enabled = bool(rule, "enabled", true);
                Component rowLabel = Component.literal(compact(id, 38)
                        + "  [" + type + "]"
                        + (enabled ? "" : "  [DISABLED]"));
                if (!enabled) rowLabel = rowLabel.copy()
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY);
                addRenderableWidget(Button.builder(rowLabel,
                        b -> Minecraft.getInstance().setScreen(
                                new ItemRuleDetailScreen(this, root, rule)))
                        .bounds(x, listY + row * 24, w - 164, 20).build());
                addRenderableWidget(Button.builder(Component.literal(
                                enabled ? "Disable" : "Enable"), b -> {
                            boolean next = !bool(rule, "enabled", true);
                            rule.addProperty("enabled", next);
                            setMatchingItemEffectsEnabled(root, id, next);
                            rebuildRows();
                        }).bounds(x + w - 156, listY + row * 24, 78, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                    removeIdentity(array(root, "item_rules"), rule);
                    removeItemEffects(root, id);
                    rebuildRows();
                }).bounds(x + w - 70, listY + row * 24, 58, 20).build());'''
text = once(text, old, new, "item rule list toggle")
text = once(
    text,
    "    private static void removeItemEffects(JsonObject root, String id) {",
    '''    private static void setMatchingItemEffectsEnabled(JsonObject root,
            String id, boolean enabled) {
        for (JsonElement element : array(root, "item_effects")) {
            if (!element.isJsonObject()) continue;
            JsonObject effect = element.getAsJsonObject();
            if (id.equals(string(effect, "id", ""))) {
                effect.addProperty("enabled", enabled);
            }
        }
    }

    private static void removeItemEffects(JsonObject root, String id) {''',
    "matching item effects helper",
)

# Codex documents.
text = once(
    text,
    '''        document.addProperty("id", "minecraft:paper");
        document.addProperty("category", "Documents");''',
    '''        document.addProperty("id", "minecraft:paper");
        document.addProperty("enabled", true);
        document.addProperty("category", "Documents");''',
    "codex default enabled",
)
old = '''                String label = string(document, "name", "Unnamed") + "  [" + string(document, "category", "Documents") + "]";
                addRenderableWidget(Button.builder(Component.literal(compact(label, 62)), b -> Minecraft.getInstance().setScreen(new CodexDetailScreen(this, root, document)))
                        .bounds(x, listY + row * 24, w - 82, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> { removeIdentity(array(root, "codex_documents"), document); rebuildRows(); })
                        .bounds(x + w - 74, listY + row * 24, 62, 20).build());'''
new = '''                boolean enabled = bool(document, "enabled", true);
                String label = string(document, "name", "Unnamed") + "  ["
                        + string(document, "category", "Documents") + "]"
                        + (enabled ? "" : "  [DISABLED]");
                Component rowLabel = Component.literal(compact(label, 52));
                if (!enabled) rowLabel = rowLabel.copy()
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY);
                addRenderableWidget(Button.builder(rowLabel, b ->
                        Minecraft.getInstance().setScreen(
                                new CodexDetailScreen(this, root, document)))
                        .bounds(x, listY + row * 24, w - 170, 20).build());
                addRenderableWidget(Button.builder(Component.literal(
                                enabled ? "Disable" : "Enable"), b -> {
                            document.addProperty("enabled", !enabled);
                            rebuildRows();
                        }).bounds(x + w - 162, listY + row * 24, 80, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                            removeIdentity(array(root, "codex_documents"), document);
                            rebuildRows();
                        }).bounds(x + w - 74, listY + row * 24, 62, 20).build());'''
text = once(text, old, new, "codex list toggle")

# Context list controls.
old = '''      int rightWidth = rowData.configured() ? 66 : 0;
      int mainWidth = w - 24 - rightWidth
              - (rightWidth > 0 ? 6 : 0);
      addRenderableWidget(new ContextRowButton(x, y, mainWidth, 32,
              rowData, () -> openRow(rowData)));
      if (rowData.configured()) {
          String label = rowData.view().hasIntegratedBase()
                  ? "Reset" : "X";
          addRenderableWidget(Button.builder(Component.literal(label),
                  b -> removeConfigured(rowData))
                  .bounds(x + mainWidth + 6, y, rightWidth, 32)
                  .build());
      }'''
new = '''      int stateWidth = 76;
      int actionWidth = rowData.configured() ? 62 : 0;
      int mainWidth = w - 24 - stateWidth - 6
              - (actionWidth > 0 ? actionWidth + 6 : 0);
      addRenderableWidget(new ContextRowButton(x, y, mainWidth, 32,
              rowData, () -> openRow(rowData)));
      int controlX = x + mainWidth + 6;
      boolean enabled = bool(rowData.rule(), "enabled", true);
      addRenderableWidget(Button.builder(Component.literal(
                      enabled ? "Disable" : "Enable"),
              b -> toggleContextEnabled(rowData))
              .bounds(controlX, y, stateWidth, 32).build());
      controlX += stateWidth + 6;
      if (rowData.configured()) {
          String label = rowData.view().hasIntegratedBase()
                  ? "Reset" : "X";
          addRenderableWidget(Button.builder(Component.literal(label),
                  b -> removeConfigured(rowData))
                  .bounds(controlX, y, actionWidth, 32).build());
      }'''
text = once(text, old, new, "context list controls")
text = once(
    text,
    "        private void removeConfigured(ContextRow row) {",
    '''        private void toggleContextEnabled(ContextRow row) {
  if (!row.configured()) {
      JsonObject tombstone = new JsonObject();
      tombstone.addProperty("type", string(row.rule(), "type", "block"));
      tombstone.addProperty("id", string(row.rule(), "id", ""));
      String interactionId = string(row.rule(), "interactionId",
              string(row.rule(), "interactionKey", ""));
      if (!interactionId.isBlank()) {
          tombstone.addProperty("interactionId", interactionId);
      }
      tombstone.addProperty("enabled", false);
      array(root, "interactions").add(tombstone);
  } else {
      JsonObject rule = row.rule();
      boolean enabled = bool(rule, "enabled", true);
      if (!enabled && isContextDisableTombstone(rule)
              && row.view().hasIntegratedBase()) {
          removeConfigured(row);
          return;
      }
      rule.addProperty("enabled", !enabled);
  }
  scroll = 0;
  rebuildRows();
        }

        private boolean isContextDisableTombstone(JsonObject rule) {
  if (bool(rule, "enabled", true)) return false;
  for (String key : rule.keySet()) {
      if (!List.of("type", "id", "interactionId", "interactionKey",
              "enabled").contains(key)) return false;
  }
  return true;
        }

        private void removeConfigured(ContextRow row) {''',
    "context toggle methods",
)
text = once(
    text,
    '''  Font rowFont = Minecraft.getInstance().font;
  int background = isHoveredOrFocused() ? 0xFF202832 : 0xFF171B22;
  int edge = isHoveredOrFocused() ? ACCENT : 0xFF3C424B;''',
    '''  Font rowFont = Minecraft.getInstance().font;
  boolean enabled = bool(row.rule(), "enabled", true);
  int background = !enabled ? 0xFF11151A
          : isHoveredOrFocused() ? 0xFF202832 : 0xFF171B22;
  int edge = !enabled ? 0xFF30343A
          : isHoveredOrFocused() ? ACCENT : 0xFF3C424B;''',
    "context row disabled palette",
)
text = once(
    text,
    '''  graphics.drawCenteredString(rowFont, type,
          typeX + 29, typeY + 5, TEXT);''',
    '''  graphics.drawCenteredString(rowFont, type,
          typeX + 29, typeY + 5, enabled ? TEXT : 0xFF707680);''',
    "context type dim",
)
text = once(
    text,
    '''  graphics.drawString(rowFont, rowFont.plainSubstrByWidth(main, available),
          textX, getY() + 5, TEXT, false);''',
    '''  graphics.drawString(rowFont, rowFont.plainSubstrByWidth(
                  main + (enabled ? "" : "  [DISABLED]"), available),
          textX, getY() + 5, enabled ? TEXT : 0xFF777E89, false);''',
    "context main dim",
)

# Drink list quick toggle.
old = '''                String label = id + (aliases.isBlank() ? "" : " — “" + aliases + "”") + (bool(drink, "enabled", true) ? "" : " [disabled]");
                addRenderableWidget(Button.builder(Component.literal(compact(label, 67)), b -> Minecraft.getInstance().setScreen(new DrinkDetailScreen(this, drink)))
                        .bounds(x, listY + row * 24, w - 150, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Copy"), b -> duplicate(drink))
                        .bounds(x + w - 142, listY + row * 24, 62, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> { removeIdentity(array(root, "drinks"), drink); rebuildRows(); })
                        .bounds(x + w - 74, listY + row * 24, 62, 20).build());'''
new = '''                boolean enabled = bool(drink, "enabled", true);
                String label = id + (aliases.isBlank() ? "" : " — “" + aliases + "”")
                        + (enabled ? "" : " [DISABLED]");
                Component rowLabel = Component.literal(compact(label, 52));
                if (!enabled) rowLabel = rowLabel.copy()
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY);
                addRenderableWidget(Button.builder(rowLabel, b ->
                        Minecraft.getInstance().setScreen(
                                new DrinkDetailScreen(this, drink)))
                        .bounds(x, listY + row * 24, w - 238, 20).build());
                addRenderableWidget(Button.builder(Component.literal(
                                enabled ? "Disable" : "Enable"), b -> {
                            drink.addProperty("enabled", !enabled);
                            rebuildRows();
                        }).bounds(x + w - 230, listY + row * 24, 80, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Copy"), b -> duplicate(drink))
                        .bounds(x + w - 142, listY + row * 24, 62, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                            removeIdentity(array(root, "drinks"), drink);
                            rebuildRows();
                        }).bounds(x + w - 74, listY + row * 24, 62, 20).build());'''
text = once(text, old, new, "drink list toggle")

# Recipe list quick toggle and dirty tracking.
old = '''                String label = id + "  [" + setting + "]  ‹" + source + "›";
                addRenderableWidget(Button.builder(Component.literal(compact(label, 69)), b -> Minecraft.getInstance().setScreen(new RecipeDetailScreen(this, ref.source(), ref.recipe())))
                        .bounds(x, listY + row * 24, w - 154, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Copy"), b -> duplicate(ref)).bounds(x + w - 146, listY + row * 24, 64, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"), b -> delete(ref)).bounds(x + w - 76, listY + row * 24, 64, 20).build());'''
new = '''                boolean enabled = bool(ref.recipe(), "enabled", true);
                String label = id + "  [" + setting + "]  ‹" + source + "›"
                        + (enabled ? "" : "  [DISABLED]");
                Component rowLabel = Component.literal(compact(label, 53));
                if (!enabled) rowLabel = rowLabel.copy()
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY);
                addRenderableWidget(Button.builder(rowLabel, b ->
                        Minecraft.getInstance().setScreen(new RecipeDetailScreen(
                                this, ref.source(), ref.recipe())))
                        .bounds(x, listY + row * 24, w - 242, 20).build());
                addRenderableWidget(Button.builder(Component.literal(
                                enabled ? "Disable" : "Enable"), b -> {
                            ref.recipe().addProperty("enabled", !enabled);
                            dirty.add(ref.source());
                            rebuildRows();
                        }).bounds(x + w - 234, listY + row * 24, 80, 20).build());
                addRenderableWidget(Button.builder(Component.literal("Copy"),
                        b -> duplicate(ref)).bounds(x + w - 146,
                        listY + row * 24, 64, 20).build());
                addRenderableWidget(Button.builder(Component.literal("X"),
                        b -> delete(ref)).bounds(x + w - 76,
                        listY + row * 24, 64, 20).build());'''
text = once(text, old, new, "recipe list toggle")

path.write_text(text, encoding="utf-8")
