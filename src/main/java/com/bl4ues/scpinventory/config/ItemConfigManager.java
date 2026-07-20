package com.bl4ues.scpinventory.config;

import com.bl4ues.scpinventory.network.ItemConfigOpenPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.bl4ues.scpadditions.compat.network.PacketDistributor;
import net.mcreator.scpadditions.config.ConfigFilePersistence;

import java.io.File;
import java.io.FileReader;
import java.util.Locale;

public final class ItemConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/scpinventory/scpinventory.json");
    private static final String NO_STAMINA = "NO_STAMINA";
    private static final String PROTECTED_EYES = "PROTECTED_EYES";

    private ItemConfigManager() {
    }

    public static void openEditor(ServerPlayer player, String idText) {
        if (player == null || !isValidId(idText)) {
            return;
        }

        JsonObject root = loadRoot();
        String type = findItemType(root, idText);
        boolean existing = type != null;
        if (type == null) {
            type = "MISCELLANEOUS";
        }
        boolean noStamina = hasItemEffect(root, idText, NO_STAMINA);
        boolean protectedEyes = hasItemEffect(root, idText, PROTECTED_EYES);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ItemConfigOpenPacket(idText, existing, type, noStamina, protectedEyes));
    }

    public static void saveRule(ServerPlayer player, String idText, String type,
            boolean noStamina, boolean protectedEyes) {
        if (player == null || !isValidId(idText)) {
            return;
        }

        JsonObject root = loadRoot();
        JsonArray rules = array(root, "item_rules");
        removeItemRuleEntries(rules, idText);

        JsonObject rule = new JsonObject();
        rule.addProperty("id", idText);
        rule.addProperty("type", cleanType(type));
        rules.add(rule);

        setItemEffect(root, idText, NO_STAMINA, noStamina);
        setItemEffect(root, idText, PROTECTED_EYES, protectedEyes);
        saveRoot(root);
        ScpInventoryConfig.reloadFromDisk();
        ModNetwork.syncServerConfig(player.server.getPlayerList().getPlayers());
        player.sendSystemMessage(Component.literal("[SCP Inventory] Saved item rule for ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(idText).withStyle(ChatFormatting.AQUA)));
    }

    public static void deleteRule(ServerPlayer player, String idText) {
        if (player == null || !isValidId(idText)) {
            return;
        }

        JsonObject root = loadRoot();
        boolean removed = removeItemRuleEntries(array(root, "item_rules"), idText);
        removed |= removeItemEffects(array(root, "item_effects"), idText);
        saveRoot(root);
        ScpInventoryConfig.reloadFromDisk();
        ModNetwork.syncServerConfig(player.server.getPlayerList().getPlayers());
        player.sendSystemMessage(Component.literal("[SCP Inventory] " + (removed ? "Removed" : "No rule for") + " item rule ").withStyle(removed ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
                .append(Component.literal(idText).withStyle(ChatFormatting.AQUA)));
    }

    private static JsonObject loadRoot() {
        try {
            File dir = CONFIG_FILE.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            if (!CONFIG_FILE.exists()) {
                JsonObject root = new JsonObject();
                root.addProperty("_comment", "SCP Inventory configuration. Edited in-game with the item config editor or config/scpinventory/scpinventory.json.");
                root.add("item_rules", new JsonArray());
                root.add("item_effects", new JsonArray());
                root.add("hidden_status_effects", new JsonArray());
                root.add("codex_documents", new JsonArray());
                saveRoot(root);
                return root;
            }

            JsonElement parsed = JsonParser.parseReader(new FileReader(CONFIG_FILE));
            JsonObject root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            array(root, "item_rules");
            array(root, "item_effects");
            array(root, "hidden_status_effects");
            array(root, "codex_documents");
            return root;
        } catch (Exception ex) {
            ex.printStackTrace();
            JsonObject root = new JsonObject();
            root.add("item_rules", new JsonArray());
            root.add("item_effects", new JsonArray());
            root.add("hidden_status_effects", new JsonArray());
            root.add("codex_documents", new JsonArray());
            return root;
        }
    }

    private static void saveRoot(JsonObject root) {
        try {
            File dir = CONFIG_FILE.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            ConfigFilePersistence.writeWithBackup(CONFIG_FILE.toPath(),
                    GSON.toJson(root) + System.lineSeparator());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static JsonArray array(JsonObject root, String key) {
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            root.add(key, new JsonArray());
        }
        return root.getAsJsonArray(key);
    }

    private static String findItemType(JsonObject root, String idText) {
        for (JsonElement entry : array(root, "item_rules")) {
            String entryId = itemRuleId(entry);
            if (idText.equals(entryId)) {
                return cleanType(itemRuleType(entry));
            }
        }
        return null;
    }

    private static boolean removeItemRuleEntries(JsonArray rules, String idText) {
        boolean removed = false;
        for (int i = rules.size() - 1; i >= 0; i--) {
            if (idText.equals(itemRuleId(rules.get(i)))) {
                rules.remove(i);
                removed = true;
            }
        }
        return removed;
    }

    private static String itemRuleId(JsonElement entry) {
        if (entry == null) {
            return "";
        }
        try {
            if (entry.isJsonPrimitive()) {
                String[] parts = entry.getAsString().split("\\|", 2);
                return parts.length > 0 ? parts[0].trim() : "";
            }
            if (entry.isJsonObject()) {
                JsonObject obj = entry.getAsJsonObject();
                return firstString(obj, "id", "item");
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String itemRuleType(JsonElement entry) {
        if (entry == null) {
            return "MISCELLANEOUS";
        }
        try {
            if (entry.isJsonPrimitive()) {
                String[] parts = entry.getAsString().split("\\|", 2);
                return parts.length > 1 ? parts[1].trim() : "MISCELLANEOUS";
            }
            if (entry.isJsonObject()) {
                JsonObject obj = entry.getAsJsonObject();
                return firstString(obj, "type", "slot");
            }
        } catch (Exception ignored) {
        }
        return "MISCELLANEOUS";
    }

    private static boolean hasItemEffect(JsonObject root, String idText, String effect) {
        String expected = effect.toUpperCase(Locale.ROOT);
        for (JsonElement entry : array(root, "item_effects")) {
            if (!idText.equals(itemEffectId(entry))) {
                continue;
            }
            if (entry.isJsonPrimitive()) {
                String[] parts = entry.getAsString().split("\\|", 2);
                if (parts.length > 1 && expected.equals(parts[1].trim().toUpperCase(Locale.ROOT))) {
                    return true;
                }
            } else if (entry.isJsonObject()) {
                JsonObject obj = entry.getAsJsonObject();
                if (obj.has("effects") && obj.get("effects").isJsonArray()) {
                    for (JsonElement value : obj.getAsJsonArray("effects")) {
                        if (value.isJsonPrimitive() && expected.equals(value.getAsString().trim().toUpperCase(Locale.ROOT))) {
                            return true;
                        }
                    }
                } else if (expected.equals(firstString(obj, "effect").toUpperCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void setItemEffect(JsonObject root, String idText, String effect, boolean enabled) {
        JsonArray effects = array(root, "item_effects");
        removeItemEffect(effects, idText, effect);
        if (!enabled) {
            return;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("id", idText);
        JsonArray effectList = new JsonArray();
        effectList.add(effect);
        obj.add("effects", effectList);
        effects.add(obj);
    }

    private static boolean removeItemEffects(JsonArray effects, String idText) {
        boolean removed = false;
        for (int i = effects.size() - 1; i >= 0; i--) {
            if (idText.equals(itemEffectId(effects.get(i)))) {
                effects.remove(i);
                removed = true;
            }
        }
        return removed;
    }

    private static void removeItemEffect(JsonArray effects, String idText, String effect) {
        String expected = effect.toUpperCase(Locale.ROOT);
        for (int i = effects.size() - 1; i >= 0; i--) {
            JsonElement entry = effects.get(i);
            if (!idText.equals(itemEffectId(entry))) {
                continue;
            }

            if (entry.isJsonPrimitive()) {
                String[] parts = entry.getAsString().split("\\|", 2);
                if (parts.length > 1 && expected.equals(parts[1].trim().toUpperCase(Locale.ROOT))) {
                    effects.remove(i);
                }
                continue;
            }

            if (entry.isJsonObject()) {
                JsonObject obj = entry.getAsJsonObject();
                if (obj.has("effects") && obj.get("effects").isJsonArray()) {
                    JsonArray list = obj.getAsJsonArray("effects");
                    for (int j = list.size() - 1; j >= 0; j--) {
                        JsonElement value = list.get(j);
                        if (value.isJsonPrimitive() && expected.equals(value.getAsString().trim().toUpperCase(Locale.ROOT))) {
                            list.remove(j);
                        }
                    }
                    if (list.isEmpty()) {
                        effects.remove(i);
                    }
                } else if (expected.equals(firstString(obj, "effect").toUpperCase(Locale.ROOT))) {
                    effects.remove(i);
                }
            }
        }
    }

    private static String itemEffectId(JsonElement entry) {
        if (entry == null) {
            return "";
        }
        try {
            if (entry.isJsonPrimitive()) {
                String[] parts = entry.getAsString().split("\\|", 2);
                return parts.length > 0 ? parts[0].trim() : "";
            }
            if (entry.isJsonObject()) {
                return firstString(entry.getAsJsonObject(), "id", "item");
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String cleanType(String type) {
        String value = type == null ? "MISCELLANEOUS" : type.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "MISC", "MISCELLANEOUS", "MISCELLANEOUSLY" -> "MISCELLANEOUS";
            case "HARMFUL", "HARM", "HAZARD", "HAZARDOUS", "DANGEROUS", "CURSED" -> "HARMFUL";
            case "CONSUMABLE", "CONSUME", "USE" -> "CONSUMABLE";
            case "USABLE", "USEABLE", "NON_CONSUMABLE", "NONCONSUMABLE", "RIGHT_CLICK", "RIGHTCLICK" -> "USABLE";
            case "PLACEABLE", "PLACE", "BLOCK", "BUILDING" -> "PLACEABLE";
            case "KEY", "KEYCARD", "KEYRING" -> "KEY";
            case "CODEX", "DOCUMENT", "DOC" -> "MISCELLANEOUS";
            case "COIN", "CURRENCY", "TOKEN", "MONEY" -> "COIN";
            case "AMMO", "AMMUNITION", "BULLET", "BULLETS", "ROUND", "ROUNDS", "MAGAZINE", "MAG" -> "AMMO";
            case "HEAD", "HELMET", "MASK" -> "HEAD";
            case "ACCESSORY", "TRINKET", "RING", "AMULET" -> "ACCESSORY";
            case "ACCESSORYHAND", "ACCESSORY_HAND", "ACCESSORYOFFHAND", "ACCESSORY_OFFHAND", "OFFHAND_ACCESSORY", "OFFHANDACCESSORY" -> "ACCESSORY_HAND";
            case "BODY", "CHEST", "CHESTPLATE", "TORSO" -> "CHEST";
            case "LEGS", "LEGGINGS", "PANTS" -> "LEGS";
            case "FEET", "BOOTS", "SHOES" -> "FEET";
            case "WEAPON", "MAINHAND", "MAIN_HAND", "HAND" -> "WEAPON";
            default -> "MISCELLANEOUS";
        };
    }

    private static String firstString(JsonObject obj, String... keys) {
        if (obj == null) {
            return "";
        }
        for (String key : keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                try {
                    return obj.get(key).getAsString().trim();
                } catch (Exception ignored) {
                }
            }
        }
        return "";
    }

    private static boolean isValidId(String idText) {
        return idText != null && ResourceLocation.tryParse(idText) != null;
    }
}
