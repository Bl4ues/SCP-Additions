from patch_common import ensure_import, replace_once

def patch_context_defaults():
    path = "src/main/java/com/bl4ues/scpinventory/context/ContextConfigManager.java"
    ensure_import(path, "import java.io.InputStream;\n", "import java.io.FileWriter;\n")
    ensure_import(path, "import java.nio.file.Files;\n", "import java.io.InputStream;\n")
    ensure_import(path, "import java.nio.file.StandardCopyOption;\n", "import java.nio.file.Files;\n")
    replace_once(
        path,
"""    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final double SELECT_REACH = 6.0D;""",
"""    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BUNDLED_CONFIG = "config/scpinventory/context_interactions.json";
    private static final double SELECT_REACH = 6.0D;""")
    replace_once(
        path,
"""            if (!file.exists()) {
                file.getParentFile().mkdirs();
                JsonObject root = new JsonObject();
                root.addProperty("_comment", "Context interaction prompts for SCP Inventory. Edited in-game with /scpinventory context or config/scpinventory/context_interactions.json.");
                root.add("interactions", new JsonArray());
                saveRoot(root);
                return root;
            }
            JsonElement parsed = JsonParser.parseReader(new FileReader(file));""",
"""            if (!file.exists()) {
                file.getParentFile().mkdirs();
                copyBundledConfig(file);
            }
            if (!file.exists()) {
                JsonObject root = new JsonObject();
                root.addProperty("_comment", "Context interaction prompts for SCP Inventory. Edited in-game with /scpinventory context or config/scpinventory/context_interactions.json.");
                root.add("interactions", new JsonArray());
                saveRoot(root);
                return root;
            }
            JsonElement parsed = JsonParser.parseReader(new FileReader(file));""")
    replace_once(
        path,
"""    private static void saveRoot(JsonObject root) {""",
"""    private static void copyBundledConfig(File file) {
        try (InputStream stream = ContextConfigManager.class.getClassLoader()
                .getResourceAsStream(BUNDLED_CONFIG)) {
            if (stream != null) {
                Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            ScpInventoryMod.LOGGER.error("Failed to copy bundled context interaction defaults", exception);
        }
    }

    private static void saveRoot(JsonObject root) {""")

def patch_inventory_defaults():
    path = "src/main/java/com/bl4ues/scpinventory/config/ScpInventoryConfig.java"
    ensure_import(path, "import java.io.InputStream;\n", "import java.io.FileWriter;\n")
    ensure_import(path, "import java.nio.file.Files;\n", "import java.io.InputStream;\n")
    ensure_import(path, "import java.nio.file.StandardCopyOption;\n", "import java.nio.file.Files;\n")
    replace_once(
        path,
"""    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/scpinventory/scpinventory.json");""",
"""    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/scpinventory/scpinventory.json");
    private static final String BUNDLED_CONFIG = "config/scpinventory/scpinventory.json";""")
    replace_once(
        path,
"""            if (!CONFIG_FILE.exists()) {
                writeDefaultConfig();
                loaded = true;
                return;
            }

            JsonObject root = JsonParser.parseReader(new FileReader(CONFIG_FILE)).getAsJsonObject();""",
"""            if (!CONFIG_FILE.exists()) {
                writeDefaultConfig();
            }

            JsonObject root = JsonParser.parseReader(new FileReader(CONFIG_FILE)).getAsJsonObject();""")
    replace_once(
        path,
"""    private static void writeDefaultConfig() throws Exception {
        JsonObject root = new JsonObject();""",
"""    private static void writeDefaultConfig() throws Exception {
        try (InputStream stream = ScpInventoryConfig.class.getClassLoader()
                .getResourceAsStream(BUNDLED_CONFIG)) {
            if (stream != null) {
                Files.copy(stream, CONFIG_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        }

        JsonObject root = new JsonObject();""")

def patch_modules_defaults():
    path = "src/main/java/net/mcreator/scpadditions/config/ScpAdditionsModulesConfig.java"
    ensure_import(path, "import java.io.InputStream;\n", "import java.io.Writer;\n")
    ensure_import(path, "import java.nio.file.StandardCopyOption;\n", "import java.nio.file.StandardOpenOption;\n")
    replace_once(
        path,
"""    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("scpadditions").resolve("modules.json");""",
"""    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("scpadditions").resolve("modules.json");
    private static final String BUNDLED_CONFIG = "config/scpadditions/modules.json";""")
    replace_once(
        path,
"""            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {""",
"""            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.notExists(CONFIG_PATH)) {
                copyBundledConfig();
            }
            if (Files.exists(CONFIG_PATH)) {""")
    replace_once(
        path,
"""    private static void writeConfig(Root config) throws IOException {""",
"""    private static void copyBundledConfig() throws IOException {
        try (InputStream stream = ScpAdditionsModulesConfig.class.getClassLoader()
                .getResourceAsStream(BUNDLED_CONFIG)) {
            if (stream != null) {
                Files.copy(stream, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void writeConfig(Root config) throws IOException {""")

def patch_bootstrap():
    path = "src/main/java/net/mcreator/scpadditions/ScpAdditionsMod.java"
    ensure_import(
        path,
        "import com.bl4ues.scpinventory.config.ScpInventoryConfig;\n",
        "import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;\n")
    replace_once(
        path,
"""        ScpAdditionsModulesConfig.load();
        Scp173TargetConfig.load();""",
"""        ScpAdditionsModulesConfig.load();
        ScpInventoryConfig.reload();
        Scp173TargetConfig.load();""")

if __name__ == "__main__":
    patch_context_defaults()
    patch_inventory_defaults()
    patch_modules_defaults()
    patch_bootstrap()
    print("First-run bundled defaults fixed")
