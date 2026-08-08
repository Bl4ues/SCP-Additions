from pathlib import Path


def read(path):
    return Path(path).read_text(encoding="utf-8")


def write(path, text):
    Path(path).write_text(text, encoding="utf-8")


def replace(path, old, new, count=1):
    text = read(path)
    if old not in text:
        raise SystemExit(f"Missing patch marker in {path}: {old[:160]!r}")
    write(path, text.replace(old, new, count))


def insert_after(path, marker, addition):
    replace(path, marker, marker + addition)


# ---------------------------------------------------------------------------
# Client preference + module declaration
# ---------------------------------------------------------------------------
prefs = "src/main/java/net/mcreator/scpadditions/client/ClientModulePreferences.java"
replace(prefs,
        '            "audio.save_game_sound_enabled",\n',
        '            "audio.save_game_sound_enabled",\n            "audio.custom_item_interaction_sounds",\n')
replace(prefs,
        '        next.audio.saveGameSoundEnabled = bool(audio,\n                "save_game_sound_enabled", next.audio.saveGameSoundEnabled);\n',
        '        next.audio.saveGameSoundEnabled = bool(audio,\n                "save_game_sound_enabled", next.audio.saveGameSoundEnabled);\n        next.audio.customItemInteractionSounds = bool(audio,\n                "custom_item_interaction_sounds",\n                next.audio.customItemInteractionSounds);\n')
replace(prefs,
        '        audio.addProperty("save_game_sound_enabled",\n                value.audio.saveGameSoundEnabled);\n',
        '        audio.addProperty("save_game_sound_enabled",\n                value.audio.saveGameSoundEnabled);\n        audio.addProperty("custom_item_interaction_sounds",\n                value.audio.customItemInteractionSounds);\n')
replace(prefs,
        '    public static boolean mainMenuMusicEnabled() {\n        return current.audio.mainMenuMusicEnabled;\n    }\n',
        '    public static boolean mainMenuMusicEnabled() {\n        return current.audio.mainMenuMusicEnabled;\n    }\n\n    public static boolean customItemInteractionSoundsEnabled() {\n        return current.audio.customItemInteractionSounds;\n    }\n')
replace(prefs,
        '        private boolean saveGameSoundEnabled = true;\n',
        '        private boolean saveGameSoundEnabled = true;\n        private boolean customItemInteractionSounds = true;\n')

modules_java = "src/main/java/net/mcreator/scpadditions/config/ScpAdditionsModulesConfig.java"
replace(modules_java,
        '\t\t@SerializedName("save_game_sound_enabled")\n\t\tpublic boolean saveGameSoundEnabled = true;\n',
        '\t\t@SerializedName("save_game_sound_enabled")\n\t\tpublic boolean saveGameSoundEnabled = true;\n\n\t\t@SerializedName("custom_item_interaction_sounds")\n\t\tpublic boolean customItemInteractionSounds = true;\n')

modules_json = "config/scpadditions/modules.json"
replace(modules_json,
        '    "save_game_sound_enabled": true\n',
        '    "save_game_sound_enabled": true,\n    "custom_item_interaction_sounds": true\n')

modules_screen = "src/main/java/net/mcreator/scpadditions/config/ui/Scp079ModulesScreenExtension.java"
replace(modules_screen,
        '            new Row("audio", "save_game_sound_enabled", "Save Game Sound",\n                    "Plays save_game.ogg when the player\'s respawn point is set.", true),\n',
        '            new Row("audio", "save_game_sound_enabled", "Save Game Sound",\n                    "Plays save_game.ogg when the player\'s respawn point is set.", true),\n            new Row("audio", "custom_item_interaction_sounds",\n                    "Custom Item Interaction Sounds",\n                    "Uses SCP Inventory pickup, consumption, and equipment feedback sounds while the custom inventory is active.", true),\n')

client_scope = "src/main/java/net/mcreator/scpadditions/config/ui/ClientPreferenceModulesUi.java"
replace(client_scope,
        '                if (personal != null) {\n                    button.active = personal || canEdit;\n                }\n',
        '                if (personal != null) {\n                    button.active = (personal || canEdit)\n                            && dependencyAvailable(screen, base);\n                }\n')
replace(client_scope,
        '    private static Boolean scopeForLabel(Map<String, Boolean> scopes,\n            String label) {\n',
        '    private static boolean dependencyAvailable(Screen screen,\n            String label) {\n        if (!"Custom Item Interaction Sounds".equals(label)) return true;\n        try {\n            JsonObject modules = working(screen);\n            if (!modules.has("inventory")\n                    || !modules.get("inventory").isJsonObject()) return true;\n            JsonObject inventory = modules.getAsJsonObject("inventory");\n            return !inventory.has("enabled")\n                    || inventory.get("enabled").getAsBoolean();\n        } catch (Exception ignored) {\n            return true;\n        }\n    }\n\n    private static Boolean scopeForLabel(Map<String, Boolean> scopes,\n            String label) {\n')

service = "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterService.java"
replace(service,
        '        checkBoolean(root, "audio", "save_game_sound_enabled", errors);\n',
        '        checkBoolean(root, "audio", "save_game_sound_enabled", errors);\n        checkBoolean(root, "audio", "custom_item_interaction_sounds", errors);\n')
replace(service,
        '        validateObjectIds(root, "item_rules", "id", errors, warnings, true);\n',
        '        validateObjectIds(root, "item_rules", "id", errors, warnings, true);\n        validateConsumableTypes(root, errors);\n')
replace(service,
        '    private static void validateContext(JsonObject root, List<String> errors, List<String> warnings) {\n',
        '    private static void validateConsumableTypes(JsonObject root,\n            List<String> errors) {\n        if (!root.has("item_rules") || !root.get("item_rules").isJsonArray()) {\n            return;\n        }\n        int index = 0;\n        for (JsonElement element : root.getAsJsonArray("item_rules")) {\n            if (element.isJsonObject()) {\n                JsonObject rule = element.getAsJsonObject();\n                String key = rule.has("consumable_type")\n                        ? "consumable_type"\n                        : rule.has("consume_type") ? "consume_type" : "";\n                if (!key.isBlank()) {\n                    String value = string(rule, key).trim().toUpperCase(java.util.Locale.ROOT);\n                    if (!"FOOD".equals(value) && !"DRINK".equals(value)) {\n                        errors.add("item_rules[" + index + "]." + key\n                                + " must be Food or Drink");\n                    }\n                    String type = string(rule, "type").trim().toUpperCase(java.util.Locale.ROOT);\n                    if (!"CONSUMABLE".equals(type)) {\n                        errors.add("item_rules[" + index + "]." + key\n                                + " is only valid for CONSUMABLE items");\n                    }\n                }\n            }\n            index++;\n        }\n    }\n\n    private static void validateContext(JsonObject root, List<String> errors, List<String> warnings) {\n')

# ---------------------------------------------------------------------------
# Sound registration and assets
# ---------------------------------------------------------------------------
gameplay_sounds = "src/main/java/net/mcreator/scpadditions/sound/GameplaySounds.java"
replace(gameplay_sounds,
        '    public static final RegistryObject<SoundEvent> SAVE_GAME =\n            register("save_game");\n',
        '    public static final RegistryObject<SoundEvent> SAVE_GAME =\n            register("save_game");\n    public static final RegistryObject<SoundEvent> ITEM_PICKUP =\n            register("inventory_pickup");\n    public static final RegistryObject<SoundEvent> ITEM_EAT =\n            register("inventory_eat");\n    public static final RegistryObject<SoundEvent> ITEM_DRINK =\n            register("inventory_drink");\n')

sounds_json = "src/main/resources/assets/scp_additions/sounds.json"
replace(sounds_json,
        '{\n  "teslaactivate": {',
        '''{\n  "inventory_pickup": {\n    "subtitle": "subtitles.scp_additions.inventory_pickup",\n    "sounds": [\n      {"name": "scp_additions:pickup_1", "stream": false, "volume": 1.0},\n      {"name": "scp_additions:pickup_2", "stream": false, "volume": 1.0},\n      {"name": "scp_additions:pickup_3", "stream": false, "volume": 1.0}\n    ]\n  },\n  "inventory_eat": {\n    "subtitle": "subtitles.scp_additions.inventory_eat",\n    "sounds": [\n      {"name": "scp_additions:eat", "stream": false, "volume": 1.0}\n    ]\n  },\n  "inventory_drink": {\n    "subtitle": "subtitles.scp_additions.inventory_drink",\n    "sounds": [\n      {"name": "scp_additions:drink", "stream": false, "volume": 1.0}\n    ]\n  },\n  "teslaactivate": {''')

lang = "src/main/resources/assets/scp_additions/lang/en_us.json"
lang_text = read(lang)
if '"subtitles.scp_additions.inventory_pickup"' not in lang_text:
    if not lang_text.startswith('{\n'):
        raise SystemExit("Unexpected en_us.json format")
    lang_text = lang_text.replace('{\n',
        '{\n  "subtitles.scp_additions.inventory_pickup": "Item picked up",\n'
        '  "subtitles.scp_additions.inventory_eat": "Eating",\n'
        '  "subtitles.scp_additions.inventory_drink": "Drinking",\n', 1)
    write(lang, lang_text)

# ---------------------------------------------------------------------------
# Extensible consumable subtype
# ---------------------------------------------------------------------------
consume_enum = '''package com.bl4ues.scpinventory.item;\n\nimport net.minecraft.core.registries.BuiltInRegistries;\nimport net.minecraft.resources.ResourceLocation;\nimport net.minecraft.world.item.ItemStack;\nimport net.minecraft.world.item.UseAnim;\n\nimport java.util.Locale;\nimport java.util.Optional;\n\n/**\n * Presentation/interaction profile for CONSUMABLE inventory rules. New use\n * families (bandage, pill, injection, etc.) can be added here without changing\n * the inventory category itself.\n */\npublic enum ScpConsumableType {\n    FOOD("Food"),\n    DRINK("Drink");\n\n    private final String displayName;\n\n    ScpConsumableType(String displayName) {\n        this.displayName = displayName;\n    }\n\n    public String displayName() {\n        return displayName;\n    }\n\n    public static Optional<ScpConsumableType> fromConfigToken(String raw) {\n        if (raw == null || raw.isBlank()) return Optional.empty();\n        String value = raw.trim().toUpperCase(Locale.ROOT);\n        return switch (value) {\n            case "FOOD", "EAT", "EATING" -> Optional.of(FOOD);\n            case "DRINK", "DRINKING", "BEVERAGE" -> Optional.of(DRINK);\n            default -> Optional.empty();\n        };\n    }\n\n    public static ScpConsumableType infer(ItemStack stack) {\n        if (stack == null || stack.isEmpty()) return FOOD;\n        if (stack.getUseAnimation() == UseAnim.DRINK) return DRINK;\n\n        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());\n        if (id != null) {\n            String path = id.getPath().toLowerCase(Locale.ROOT);\n            if (path.contains("drink") || path.contains("potion")\n                    || path.contains("coffee") || path.contains("juice")\n                    || path.contains("water") || path.contains("milk")\n                    || path.contains("beverage")) {\n                return DRINK;\n            }\n        }\n        return FOOD;\n    }\n}\n'''
write("src/main/java/com/bl4ues/scpinventory/item/ScpConsumableType.java", consume_enum)

inventory_config = "src/main/java/com/bl4ues/scpinventory/config/ScpInventoryConfig.java"
replace(inventory_config,
        '            String id = firstString(obj, "id", "item");\n            String type = firstString(obj, "type", "slot");\n            if (!id.isBlank() && !type.isBlank()) {\n                values.add(id + "|" + type.toUpperCase(Locale.ROOT));\n            }\n',
        '            String id = firstString(obj, "id", "item");\n            String type = firstString(obj, "type", "slot");\n            if (!id.isBlank() && !type.isBlank()) {\n                String normalizedType = type.toUpperCase(Locale.ROOT);\n                String encoded = id + "|" + normalizedType;\n                if ("CONSUMABLE".equals(normalizedType)) {\n                    String consumableType = firstString(obj,\n                            "consumable_type", "consume_type");\n                    if (!consumableType.isBlank()) {\n                        encoded += "|" + consumableType.toUpperCase(Locale.ROOT);\n                    }\n                }\n                values.add(encoded);\n            }\n')

classifier = "src/main/java/com/bl4ues/scpinventory/item/ScpItemClassifier.java"
replace(classifier,
        '    public static String getDisplayType(ItemStack stack) {\n        return getType(stack).getDisplayName();\n    }\n',
        '    public static String getDisplayType(ItemStack stack) {\n        return getType(stack).getDisplayName();\n    }\n\n    public static ScpConsumableType getConsumableType(ItemStack stack) {\n        if (stack == null || stack.isEmpty()) return ScpConsumableType.FOOD;\n        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());\n        if (stackId != null) {\n            for (String rawRule : ScpInventoryConfig.itemRules()) {\n                Optional<ConfiguredItemRule> rule = parseItemRule(rawRule);\n                if (rule.isPresent()\n                        && rule.get().itemId().equals(stackId)\n                        && rule.get().type() == ScpItemType.CONSUMABLE\n                        && rule.get().consumableType() != null) {\n                    return rule.get().consumableType();\n                }\n            }\n        }\n        return ScpConsumableType.infer(stack);\n    }\n')
replace(classifier,
        '    private static Optional<ConfiguredItemRule> parseItemRule(String rawRule) {\n        if (rawRule == null || rawRule.isBlank()) return Optional.empty();\n        String[] parts = rawRule.split("\\\\|", 2);\n        if (parts.length != 2) return Optional.empty();\n        ResourceLocation configuredId = ResourceLocation.tryParse(parts[0].trim());\n        if (configuredId == null) return Optional.empty();\n        Optional<ScpItemType> type = ScpItemType.fromConfigToken(parts[1]);\n        return type.map(scpItemType -> new ConfiguredItemRule(configuredId, scpItemType));\n    }\n',
        '    private static Optional<ConfiguredItemRule> parseItemRule(String rawRule) {\n        if (rawRule == null || rawRule.isBlank()) return Optional.empty();\n        String[] parts = rawRule.split("\\\\|", 3);\n        if (parts.length < 2) return Optional.empty();\n        ResourceLocation configuredId = ResourceLocation.tryParse(parts[0].trim());\n        if (configuredId == null) return Optional.empty();\n        Optional<ScpItemType> type = ScpItemType.fromConfigToken(parts[1]);\n        ScpConsumableType consumableType = parts.length > 2\n                ? ScpConsumableType.fromConfigToken(parts[2]).orElse(null)\n                : null;\n        return type.map(scpItemType -> new ConfiguredItemRule(\n                configuredId, scpItemType, consumableType));\n    }\n')
replace(classifier,
        '    private record ConfiguredItemRule(ResourceLocation itemId, ScpItemType type) {\n    }\n',
        '    private record ConfiguredItemRule(ResourceLocation itemId,\n            ScpItemType type, ScpConsumableType consumableType) {\n    }\n')

# ---------------------------------------------------------------------------
# Item rule editor packet/persistence
# ---------------------------------------------------------------------------
manager = "src/main/java/com/bl4ues/scpinventory/config/ItemConfigManager.java"
replace(manager,
        'import com.bl4ues.scpinventory.network.ItemConfigOpenPacket;\n',
        'import com.bl4ues.scpinventory.item.ScpConsumableType;\nimport com.bl4ues.scpinventory.network.ItemConfigOpenPacket;\n')
replace(manager,
        'import net.minecraft.core.registries.BuiltInRegistries;\n' if 'import net.minecraft.core.registries.BuiltInRegistries;\n' in read(manager) else 'import net.minecraft.ChatFormatting;\n',
        'import net.minecraft.core.registries.BuiltInRegistries;\nimport net.minecraft.ChatFormatting;\n' if 'import net.minecraft.core.registries.BuiltInRegistries;\n' not in read(manager) else 'import net.minecraft.core.registries.BuiltInRegistries;\n')
# Ensure ItemStack import once.
text = read(manager)
if 'import net.minecraft.world.item.ItemStack;' not in text:
    text = text.replace('import net.minecraft.server.level.ServerPlayer;\n',
                        'import net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.item.ItemStack;\n')
    write(manager, text)
replace(manager,
        '        boolean noStamina = hasItemEffect(root, idText, NO_STAMINA);\n        boolean protectedEyes = hasItemEffect(root, idText, PROTECTED_EYES);\n        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),\n                new ItemConfigOpenPacket(idText, existing, type, noStamina, protectedEyes));\n',
        '        String consumableType = findConsumableType(root, idText);\n        if (consumableType.isBlank()) consumableType = inferConsumableType(idText);\n        boolean noStamina = hasItemEffect(root, idText, NO_STAMINA);\n        boolean protectedEyes = hasItemEffect(root, idText, PROTECTED_EYES);\n        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),\n                new ItemConfigOpenPacket(idText, existing, type, consumableType,\n                        noStamina, protectedEyes));\n')
replace(manager,
        '    public static void saveRule(ServerPlayer player, String idText, String type,\n            boolean noStamina, boolean protectedEyes) {\n',
        '    public static void saveRule(ServerPlayer player, String idText, String type,\n            String consumableType, boolean noStamina, boolean protectedEyes) {\n')
replace(manager,
        '        JsonObject rule = new JsonObject();\n        rule.addProperty("id", idText);\n        rule.addProperty("type", cleanType(type));\n        rules.add(rule);\n',
        '        JsonObject rule = new JsonObject();\n        rule.addProperty("id", idText);\n        String normalizedType = cleanType(type);\n        rule.addProperty("type", normalizedType);\n        if ("CONSUMABLE".equals(normalizedType)) {\n            rule.addProperty("consumable_type",\n                    cleanConsumableType(consumableType));\n        }\n        rules.add(rule);\n')
replace(manager,
        '                String[] parts = entry.getAsString().split("\\\\|", 2);\n                return parts.length > 1 ? parts[1].trim() : "MISCELLANEOUS";\n',
        '                String[] parts = entry.getAsString().split("\\\\|", 3);\n                return parts.length > 1 ? parts[1].trim() : "MISCELLANEOUS";\n')
replace(manager,
        '    private static boolean hasItemEffect(JsonObject root, String idText, String effect) {\n',
        '    private static String findConsumableType(JsonObject root, String idText) {\n        for (JsonElement entry : array(root, "item_rules")) {\n            if (idText.equals(itemRuleId(entry))) {\n                return itemRuleConsumableType(entry);\n            }\n        }\n        return "";\n    }\n\n    private static String itemRuleConsumableType(JsonElement entry) {\n        if (entry == null) return "";\n        try {\n            if (entry.isJsonPrimitive()) {\n                String[] parts = entry.getAsString().split("\\\\|", 3);\n                return parts.length > 2 ? parts[2].trim() : "";\n            }\n            if (entry.isJsonObject()) {\n                return firstString(entry.getAsJsonObject(),\n                        "consumable_type", "consume_type");\n            }\n        } catch (Exception ignored) {\n        }\n        return "";\n    }\n\n    private static String inferConsumableType(String idText) {\n        ResourceLocation id = ResourceLocation.tryParse(idText);\n        if (id == null) return ScpConsumableType.FOOD.name();\n        return BuiltInRegistries.ITEM.getOptional(id)\n                .map(ItemStack::new)\n                .map(ScpConsumableType::infer)\n                .orElse(ScpConsumableType.FOOD).name();\n    }\n\n    private static String cleanConsumableType(String value) {\n        return ScpConsumableType.fromConfigToken(value)\n                .orElse(ScpConsumableType.FOOD).name();\n    }\n\n    private static boolean hasItemEffect(JsonObject root, String idText, String effect) {\n')

open_packet = "src/main/java/com/bl4ues/scpinventory/network/ItemConfigOpenPacket.java"
replace(open_packet,
        '    private final String type;\n    private final boolean noStamina;\n',
        '    private final String type;\n    private final String consumableType;\n    private final boolean noStamina;\n')
replace(open_packet,
        '    public ItemConfigOpenPacket(String itemId, boolean existing, String type,\n            boolean noStamina, boolean protectedEyes) {\n        this.itemId = itemId == null ? "" : itemId;\n        this.existing = existing;\n        this.type = type == null ? "MISCELLANEOUS" : type;\n        this.noStamina = noStamina;\n        this.protectedEyes = protectedEyes;\n    }\n',
        '    public ItemConfigOpenPacket(String itemId, boolean existing, String type,\n            String consumableType, boolean noStamina, boolean protectedEyes) {\n        this.itemId = itemId == null ? "" : itemId;\n        this.existing = existing;\n        this.type = type == null ? "MISCELLANEOUS" : type;\n        this.consumableType = consumableType == null ? "FOOD" : consumableType;\n        this.noStamina = noStamina;\n        this.protectedEyes = protectedEyes;\n    }\n\n    public ItemConfigOpenPacket(String itemId, boolean existing, String type,\n            boolean noStamina, boolean protectedEyes) {\n        this(itemId, existing, type, "FOOD", noStamina, protectedEyes);\n    }\n')
replace(open_packet,
        '        buf.writeUtf(msg.type);\n        buf.writeBoolean(msg.noStamina);\n',
        '        buf.writeUtf(msg.type);\n        buf.writeUtf(msg.consumableType);\n        buf.writeBoolean(msg.noStamina);\n')
replace(open_packet,
        '        return new ItemConfigOpenPacket(buf.readUtf(), buf.readBoolean(), buf.readUtf(),\n                buf.readBoolean(), buf.readBoolean());\n',
        '        return new ItemConfigOpenPacket(buf.readUtf(), buf.readBoolean(),\n                buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readBoolean());\n')
replace(open_packet,
        '    public boolean noStamina() {\n',
        '    public String consumableType() {\n        return consumableType;\n    }\n\n    public boolean noStamina() {\n')

save_packet = "src/main/java/com/bl4ues/scpinventory/network/ItemConfigSavePacket.java"
replace(save_packet,
        '    private final String type;\n    private final boolean noStamina;\n',
        '    private final String type;\n    private final String consumableType;\n    private final boolean noStamina;\n')
replace(save_packet,
        '    public ItemConfigSavePacket(String itemId, String type, boolean noStamina, boolean protectedEyes) {\n        this.itemId = itemId == null ? "" : itemId;\n        this.type = type == null ? "MISCELLANEOUS" : type;\n        this.noStamina = noStamina;\n        this.protectedEyes = protectedEyes;\n    }\n',
        '    public ItemConfigSavePacket(String itemId, String type,\n            String consumableType, boolean noStamina, boolean protectedEyes) {\n        this.itemId = itemId == null ? "" : itemId;\n        this.type = type == null ? "MISCELLANEOUS" : type;\n        this.consumableType = consumableType == null ? "FOOD" : consumableType;\n        this.noStamina = noStamina;\n        this.protectedEyes = protectedEyes;\n    }\n\n    public ItemConfigSavePacket(String itemId, String type,\n            boolean noStamina, boolean protectedEyes) {\n        this(itemId, type, "FOOD", noStamina, protectedEyes);\n    }\n')
replace(save_packet,
        '        buf.writeUtf(msg.type);\n        buf.writeBoolean(msg.noStamina);\n',
        '        buf.writeUtf(msg.type);\n        buf.writeUtf(msg.consumableType);\n        buf.writeBoolean(msg.noStamina);\n')
replace(save_packet,
        '        String type = buf.readUtf();\n        boolean noStamina = buf.readBoolean();\n        boolean protectedEyes = buf.readBoolean();\n        return new ItemConfigSavePacket(itemId, type, noStamina, protectedEyes);\n',
        '        String type = buf.readUtf();\n        String consumableType = buf.readUtf();\n        boolean noStamina = buf.readBoolean();\n        boolean protectedEyes = buf.readBoolean();\n        return new ItemConfigSavePacket(itemId, type, consumableType,\n                noStamina, protectedEyes);\n')
replace(save_packet,
        '            ItemConfigManager.saveRule(player, msg.itemId, msg.type, msg.noStamina, msg.protectedEyes);\n',
        '            ItemConfigManager.saveRule(player, msg.itemId, msg.type,\n                    msg.consumableType, msg.noStamina, msg.protectedEyes);\n')

editor = "src/main/java/com/bl4ues/scpinventory/client/gui/ItemRuleEditorScreen.java"
replace(editor,
        'import com.bl4ues.scpinventory.item.ScpItemType;\n',
        'import com.bl4ues.scpinventory.item.ScpConsumableType;\nimport com.bl4ues.scpinventory.item.ScpItemType;\n')
replace(editor,
        '    private static final int PANEL_H = 228;\n',
        '    private static final int BASE_PANEL_H = 228;\n    private static final int CONSUMABLE_PANEL_H = 276;\n')
replace(editor,
        '    private ScpItemType type;\n    private final EnumSet<EquipmentEffect> effects =\n',
        '    private ScpItemType type;\n    private ScpConsumableType consumableType;\n    private final EnumSet<EquipmentEffect> effects =\n')
replace(editor,
        '    private SingleDropdown<ScpItemType> categoryDropdown;\n    private EffectMultiSelect effectDropdown;\n',
        '    private SingleDropdown<ScpItemType> categoryDropdown;\n    private SingleDropdown<ScpConsumableType> consumableTypeDropdown;\n    private EffectMultiSelect effectDropdown;\n')
replace(editor,
        '        this.type = parseType(packet.type());\n        if (packet.noStamina()) effects.add(EquipmentEffect.NO_STAMINA);\n',
        '        this.type = parseType(packet.type());\n        this.consumableType = ScpConsumableType\n                .fromConfigToken(packet.consumableType())\n                .orElse(ScpConsumableType.FOOD);\n        if (packet.noStamina()) effects.add(EquipmentEffect.NO_STAMINA);\n')
replace(editor,
        '                value -> ScpFonts.roboto(value.getDisplayName()),\n                value -> type = value));\n\n        effectDropdown = addRenderableWidget(new EffectMultiSelect(\n                x, top + 139, width, 22));\n\n        int bottomY = top + PANEL_H - 34;\n',
        '                value -> ScpFonts.roboto(value.getDisplayName()),\n                value -> {\n                    boolean layoutChanged = (type == ScpItemType.CONSUMABLE)\n                            != (value == ScpItemType.CONSUMABLE);\n                    type = value;\n                    if (layoutChanged) {\n                        Minecraft.getInstance().execute(this::rebuildWidgets);\n                    }\n                }));\n\n        int effectY = top + 139;\n        if (type == ScpItemType.CONSUMABLE) {\n            consumableTypeDropdown = addRenderableWidget(new SingleDropdown<>(\n                    x, top + 139, width, 22,\n                    List.of(ScpConsumableType.values()), consumableType,\n                    value -> ScpFonts.roboto(value.displayName()),\n                    value -> consumableType = value));\n            effectY = top + 187;\n        } else {\n            consumableTypeDropdown = null;\n        }\n\n        effectDropdown = addRenderableWidget(new EffectMultiSelect(\n                x, effectY, width, 22));\n\n        int bottomY = top + panelHeight() - 34;\n')
replace(editor,
        '        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, NAVY);\n',
        '        int panelHeight = panelHeight();\n        graphics.fill(left, top, left + PANEL_W, top + panelHeight, NAVY);\n')
replace(editor,
        '        outline(graphics, left, top, PANEL_W, PANEL_H, BORDER);\n',
        '        outline(graphics, left, top, PANEL_W, panelHeight, BORDER);\n')
replace(editor,
        '        graphics.drawString(font,\n                ScpFonts.roboto("EQUIPMENT EFFECTS"),\n                left + 16, top + 126, SECTION, false);\n',
        '        if (type == ScpItemType.CONSUMABLE) {\n            graphics.drawString(font,\n                    ScpFonts.roboto("CONSUMABLE TYPE"),\n                    left + 16, top + 126, SECTION, false);\n            graphics.drawString(font,\n                    ScpFonts.roboto("EQUIPMENT EFFECTS"),\n                    left + 16, top + 174, SECTION, false);\n        } else {\n            graphics.drawString(font,\n                    ScpFonts.roboto("EQUIPMENT EFFECTS"),\n                    left + 16, top + 126, SECTION, false);\n        }\n')
replace(editor,
        '        ModNetwork.CHANNEL.sendToServer(new ItemConfigSavePacket(\n                itemId, type.name(),\n                effects.contains(EquipmentEffect.NO_STAMINA),\n',
        '        ModNetwork.CHANNEL.sendToServer(new ItemConfigSavePacket(\n                itemId, type.name(), consumableType.name(),\n                effects.contains(EquipmentEffect.NO_STAMINA),\n')
replace(editor,
        '    private int panelTop() {\n        return Math.max(MARGIN, (height - PANEL_H) / 2);\n    }\n',
        '    private int panelHeight() {\n        return type == ScpItemType.CONSUMABLE\n                ? CONSUMABLE_PANEL_H : BASE_PANEL_H;\n    }\n\n    private int panelTop() {\n        return Math.max(MARGIN, (height - panelHeight()) / 2);\n    }\n')

legacy_editor = "src/main/java/com/bl4ues/scpinventory/client/gui/ItemConfigScreen.java"
replace(legacy_editor,
        '    private ScpItemType type;\n    private boolean noStamina;\n',
        '    private ScpItemType type;\n    private final String consumableType;\n    private boolean noStamina;\n')
replace(legacy_editor,
        '        this.type = parseType(packet.type());\n        this.noStamina = packet.noStamina();\n',
        '        this.type = parseType(packet.type());\n        this.consumableType = packet.consumableType();\n        this.noStamina = packet.noStamina();\n')
replace(legacy_editor,
        '        ModNetwork.CHANNEL.sendToServer(new ItemConfigSavePacket(\n                itemId, type.name(), noStamina, protectedEyes));\n',
        '        ModNetwork.CHANNEL.sendToServer(new ItemConfigSavePacket(\n                itemId, type.name(), consumableType,\n                noStamina, protectedEyes));\n')

# ---------------------------------------------------------------------------
# Client-directed feedback packet and centralized server feedback
# ---------------------------------------------------------------------------
client_sound = '''package com.bl4ues.scpinventory.client;\n\nimport com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;\nimport com.bl4ues.scpinventory.network.ItemInteractionSoundPacket;\nimport net.minecraft.client.Minecraft;\nimport net.minecraft.client.resources.sounds.SimpleSoundInstance;\nimport net.minecraft.sounds.SoundEvent;\nimport net.minecraft.sounds.SoundEvents;\nimport net.mcreator.scpadditions.client.ClientModulePreferences;\nimport net.mcreator.scpadditions.sound.GameplaySounds;\n\n/** Plays personal SCP Inventory feedback without changing nearby players' audio. */\npublic final class ClientItemInteractionSounds {\n    private ClientItemInteractionSounds() {\n    }\n\n    public static void play(ItemInteractionSoundPacket.Cue cue) {\n        if (cue == null) return;\n        Minecraft minecraft = Minecraft.getInstance();\n        boolean custom = InventoryModuleRuntimeState.isEnabledForClient()\n                && ClientModulePreferences.customItemInteractionSoundsEnabled();\n\n        if (custom) {\n            SoundEvent sound = switch (cue) {\n                case PICKUP, EQUIP -> GameplaySounds.ITEM_PICKUP.get();\n                case FOOD -> GameplaySounds.ITEM_EAT.get();\n                case DRINK -> GameplaySounds.ITEM_DRINK.get();\n            };\n            minecraft.getSoundManager().play(\n                    SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));\n            return;\n        }\n\n        switch (cue) {\n            case PICKUP -> {\n                float pitch = minecraft.player == null ? 1.4F\n                        : ((minecraft.player.getRandom().nextFloat()\n                        - minecraft.player.getRandom().nextFloat())\n                        * 0.7F + 1.0F) * 2.0F;\n                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(\n                        SoundEvents.ITEM_PICKUP, pitch, 0.2F));\n            }\n            case FOOD -> minecraft.getSoundManager().play(\n                    SimpleSoundInstance.forUI(SoundEvents.GENERIC_EAT,\n                            1.0F, 0.8F));\n            case DRINK -> minecraft.getSoundManager().play(\n                    SimpleSoundInstance.forUI(SoundEvents.GENERIC_DRINK,\n                            1.0F, 0.8F));\n            case EQUIP -> {\n                // Equipment was silent before this optional feedback module.\n            }\n        }\n    }\n}\n'''
write("src/main/java/com/bl4ues/scpinventory/client/ClientItemInteractionSounds.java", client_sound)

sound_packet = '''package com.bl4ues.scpinventory.network;\n\nimport com.bl4ues.scpinventory.client.ClientItemInteractionSounds;\nimport net.minecraft.network.FriendlyByteBuf;\nimport net.minecraftforge.network.NetworkEvent;\n\nimport java.util.function.Supplier;\n\n/** Server-authoritative action confirmation with client-personal presentation. */\npublic final class ItemInteractionSoundPacket {\n    public enum Cue {\n        PICKUP,\n        EQUIP,\n        FOOD,\n        DRINK\n    }\n\n    private final Cue cue;\n\n    public ItemInteractionSoundPacket(Cue cue) {\n        this.cue = cue == null ? Cue.PICKUP : cue;\n    }\n\n    public static void encode(ItemInteractionSoundPacket message,\n            FriendlyByteBuf buffer) {\n        buffer.writeEnum(message.cue);\n    }\n\n    public static ItemInteractionSoundPacket decode(FriendlyByteBuf buffer) {\n        return new ItemInteractionSoundPacket(buffer.readEnum(Cue.class));\n    }\n\n    public static void handle(ItemInteractionSoundPacket message,\n            Supplier<NetworkEvent.Context> contextSupplier) {\n        contextSupplier.get().enqueueWork(() ->\n                ClientItemInteractionSounds.play(message.cue));\n        contextSupplier.get().setPacketHandled(true);\n    }\n}\n'''
write("src/main/java/com/bl4ues/scpinventory/network/ItemInteractionSoundPacket.java", sound_packet)

feedback = '''package com.bl4ues.scpinventory.sound;\n\nimport com.bl4ues.scpinventory.item.ScpConsumableType;\nimport com.bl4ues.scpinventory.item.ScpItemClassifier;\nimport com.bl4ues.scpinventory.network.ItemInteractionSoundPacket;\nimport com.bl4ues.scpinventory.network.ModNetwork;\nimport net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.sounds.SoundEvent;\nimport net.minecraft.sounds.SoundEvents;\nimport net.minecraft.sounds.SoundSource;\nimport net.minecraft.world.item.ItemStack;\nimport net.minecraftforge.network.PacketDistributor;\n\n/**\n * Keeps vanilla spatial feedback for observers while letting only the acting\n * client's personal preference replace its own SCP Inventory interaction cue.\n */\npublic final class InventoryInteractionSoundFeedback {\n    private InventoryInteractionSoundFeedback() {\n    }\n\n    public static void pickup(ServerPlayer player) {\n        if (player == null) return;\n        float pitch = ((player.getRandom().nextFloat()\n                - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F;\n        playForOthers(player, SoundEvents.ITEM_PICKUP, 0.2F, pitch);\n        send(player, ItemInteractionSoundPacket.Cue.PICKUP);\n    }\n\n    public static void equipped(ServerPlayer player) {\n        send(player, ItemInteractionSoundPacket.Cue.EQUIP);\n    }\n\n    public static void consumed(ServerPlayer player, ItemStack stack) {\n        if (player == null) return;\n        ScpConsumableType type = ScpItemClassifier.getConsumableType(stack);\n        SoundEvent vanilla = type == ScpConsumableType.DRINK\n                ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT;\n        float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;\n        playForOthers(player, vanilla, 0.8F, pitch);\n        send(player, type == ScpConsumableType.DRINK\n                ? ItemInteractionSoundPacket.Cue.DRINK\n                : ItemInteractionSoundPacket.Cue.FOOD);\n    }\n\n    private static void playForOthers(ServerPlayer player, SoundEvent sound,\n            float volume, float pitch) {\n        // Excluding the source player prevents a custom local cue from layering\n        // on top of the vanilla sound, while observers retain normal feedback.\n        player.level().playSound(player, player.getX(), player.getY(),\n                player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);\n    }\n\n    private static void send(ServerPlayer player,\n            ItemInteractionSoundPacket.Cue cue) {\n        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),\n                new ItemInteractionSoundPacket(cue));\n    }\n}\n'''
write("src/main/java/com/bl4ues/scpinventory/sound/InventoryInteractionSoundFeedback.java", feedback)

network = "src/main/java/com/bl4ues/scpinventory/network/ModNetwork.java"
replace(network,
        '    private static final String PROTOCOL_VERSION = "18";\n',
        '    private static final String PROTOCOL_VERSION = "19";\n')
replace(network,
        '        CHANNEL.registerMessage(id++, PickupItemPacket.class, PickupItemPacket::encode, PickupItemPacket::decode, PickupItemPacket::handle);\n',
        '        CHANNEL.registerMessage(id++, PickupItemPacket.class, PickupItemPacket::encode, PickupItemPacket::decode, PickupItemPacket::handle);\n        CHANNEL.registerMessage(id++, ItemInteractionSoundPacket.class, ItemInteractionSoundPacket::encode, ItemInteractionSoundPacket::decode, ItemInteractionSoundPacket::handle);\n')

pickup_packet = "src/main/java/com/bl4ues/scpinventory/network/PickupItemPacket.java"
replace(pickup_packet,
        'import com.bl4ues.scpinventory.item.ScpPickupRouter;\n',
        'import com.bl4ues.scpinventory.item.ScpPickupRouter;\nimport com.bl4ues.scpinventory.sound.InventoryInteractionSoundFeedback;\n')
replace(pickup_packet,
        '        player.take(itemEntity, acceptedCount);\n        player.level().playSound(\n                null,\n                player.getX(),\n                player.getY(),\n                player.getZ(),\n                SoundEvents.ITEM_PICKUP,\n                SoundSource.PLAYERS,\n                0.2F,\n                ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F\n        );\n',
        '        player.take(itemEntity, acceptedCount);\n        InventoryInteractionSoundFeedback.pickup(player);\n')

inventory_action = "src/main/java/com/bl4ues/scpinventory/network/InventoryActionPacket.java"
replace(inventory_action,
        'import com.bl4ues.scpinventory.item.ScpPickupRouter;\n',
        'import com.bl4ues.scpinventory.item.ScpPickupRouter;\nimport com.bl4ues.scpinventory.sound.InventoryInteractionSoundFeedback;\n')
replace(inventory_action,
        '        player.swing(InteractionHand.MAIN_HAND, true);\n        player.level().playSound(\n                null,\n                player.getX(),\n                player.getY(),\n                player.getZ(),\n                animation == UseAnim.DRINK ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT,\n                SoundSource.PLAYERS,\n                0.8F,\n                0.9F + player.getRandom().nextFloat() * 0.2F\n        );\n\n        HungerSystemEvents.healFromFood(player, usedStack);\n',
        '        player.swing(InteractionHand.MAIN_HAND, true);\n        InventoryInteractionSoundFeedback.consumed(player, usedStack);\n\n        HungerSystemEvents.healFromFood(player, usedStack);\n')
replace(inventory_action,
        '        inventory.setEquipment(targetSlot, newEquipment);\n        syncVanillaEquipmentSlot(player, targetSlot, newEquipment);\n\n        if (!previousEquipment.isEmpty()) inventory.setInventoryItem(slot, previousEquipment);\n',
        '        inventory.setEquipment(targetSlot, newEquipment);\n        syncVanillaEquipmentSlot(player, targetSlot, newEquipment);\n        InventoryInteractionSoundFeedback.equipped(player);\n\n        if (!previousEquipment.isEmpty()) inventory.setInventoryItem(slot, previousEquipment);\n')

main_use = "src/main/java/com/bl4ues/scpinventory/network/MainUseActionPacket.java"
replace(main_use,
        'import com.bl4ues.scpinventory.item.ScpPickupRouter;\n',
        'import com.bl4ues.scpinventory.item.ScpPickupRouter;\nimport com.bl4ues.scpinventory.sound.InventoryInteractionSoundFeedback;\n')
replace(main_use,
        '        player.swing(InteractionHand.MAIN_HAND, true);\n        player.level().playSound(\n                null,\n                player.getX(),\n                player.getY(),\n                player.getZ(),\n                animation == UseAnim.DRINK ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT,\n                SoundSource.PLAYERS,\n                0.8F,\n                0.9F + player.getRandom().nextFloat() * 0.2F\n        );\n\n        HungerSystemEvents.healFromFood(player, usedStack);\n',
        '        player.swing(InteractionHand.MAIN_HAND, true);\n        InventoryInteractionSoundFeedback.consumed(player, usedStack);\n\n        HungerSystemEvents.healFromFood(player, usedStack);\n')

move_packet = "src/main/java/com/bl4ues/scpinventory/network/InventoryMovePacket.java"
replace(move_packet,
        'import com.bl4ues.scpinventory.item.ScpItemClassifier;\n',
        'import com.bl4ues.scpinventory.item.ScpItemClassifier;\nimport com.bl4ues.scpinventory.sound.InventoryInteractionSoundFeedback;\n')
replace(move_packet,
        '        inventory.setEquipment(targetSlot, movingStack);\n        InventoryActionPacket.syncVanillaEquipmentSlot(player, targetSlot, movingStack);\n\n        if (!previousEquipment.isEmpty()) {\n',
        '        inventory.setEquipment(targetSlot, movingStack);\n        InventoryActionPacket.syncVanillaEquipmentSlot(player, targetSlot, movingStack);\n        InventoryInteractionSoundFeedback.equipped(player);\n\n        if (!previousEquipment.isEmpty()) {\n')

# ---------------------------------------------------------------------------
# Changelog
# ---------------------------------------------------------------------------
changelog = "CHANGELOG.md"
replace(changelog,
        '## Audio and presentation\n\n',
        '## Audio and presentation\n\n- Added a default-enabled client-side **Custom Item Interaction Sounds** preference, active only for SCP Inventory actions: custom prompt pickups and equipment use randomized pickup cues, while CONSUMABLE rules can select **Food** or **Drink** feedback in the item-category editor; vanilla interaction paths remain untouched and disabling the preference restores vanilla local pickup/eat/drink feedback;\n')

# Tidy imports that became unused after centralizing sound playback.
for path in [pickup_packet, inventory_action, main_use]:
    text = read(path)
    for unused in [
        'import net.minecraft.sounds.SoundEvents;\n',
        'import net.minecraft.sounds.SoundSource;\n',
    ]:
        text = text.replace(unused, '')
    write(path, text)

print("Custom item interaction sound patch applied.")
