from pathlib import Path
import json

R = Path(__file__).resolve().parents[1]
JAVA = R / 'src/main/java/com/bl4ues/scpclassifieddirective'
MIXINS = R / 'src/main/resources/scp_classified_directive.mixins.json'

# Finish identifier-level branding that cannot be handled by namespace token replacement.
for path in list(JAVA.rglob('*.java')):
    text = path.read_text(encoding='utf-8')
    updated = text.replace('SCPAdditions', 'SCPClassifiedDirective')
    if updated != text:
        path.write_text(updated, encoding='utf-8')
for path in list(JAVA.rglob('*SCPAdditions*.java')):
    path.rename(path.with_name(path.name.replace('SCPAdditions', 'SCPClassifiedDirective')))

# The SavedData filenames are part of existing world persistence, not public branding.
# Keep their historical storage keys intentionally and document why they must not be
# casually renamed by a later cleanup.
variables = JAVA / 'network/ScpClassifiedDirectiveModVariables.java'
variables_text = variables.read_text(encoding='utf-8')
for literal in ('scp_additions_worldvars', 'scp_additions_mapvars'):
    declaration = f'\t\tpublic static final String DATA_NAME = "{literal}";'
    documented = ('\t\t// Legacy persistence key intentionally retained so existing worlds load their saved state.\n'
                  + declaration)
    if declaration in variables_text and documented not in variables_text:
        variables_text = variables_text.replace(declaration, documented, 1)
variables.write_text(variables_text, encoding='utf-8')

# Runtime config migration must migrate the contents too. Merely copying a user's old
# JSON would preserve paths such as scp_additions:item and make those custom rules fail
# against the newly registered namespace.
compat_dir = JAVA / 'compat'
compat_dir.mkdir(parents=True, exist_ok=True)
(compat_dir / 'LegacyConfigMigration.java').write_text('''package com.bl4ues.scpclassifieddirective.compat;\n\nimport java.io.IOException;\nimport java.nio.charset.StandardCharsets;\nimport java.nio.file.Files;\nimport java.nio.file.Path;\nimport java.nio.file.StandardCopyOption;\nimport java.util.List;\nimport java.util.Locale;\nimport net.minecraftforge.fml.loading.FMLPaths;\nimport com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;\n\npublic final class LegacyConfigMigration {\n    private static final List<String> LEGACY_NAMESPACES = List.of(\n            "scp_additions", "scp_unity_extra_blocks", "scp_ublocks", "scpinventory");\n    private static final List<String> TEXT_EXTENSIONS = List.of(\n            ".json", ".toml", ".cfg", ".properties", ".txt", ".csv", ".yaml", ".yml");\n\n    private LegacyConfigMigration() {\n    }\n\n    public static void migrate() {\n        Path root = FMLPaths.CONFIGDIR.get();\n        Path destination = root.resolve(ScpClassifiedDirectiveMod.MODID);\n        try {\n            Files.createDirectories(destination);\n            copyLegacyTree(root.resolve("scpadditions"), destination);\n            copyLegacyTree(root.resolve("scpinventory"), destination);\n        } catch (IOException exception) {\n            ScpClassifiedDirectiveMod.LOGGER.warn("Could not migrate legacy SCP configuration", exception);\n        }\n    }\n\n    private static void copyLegacyTree(Path source, Path destination) throws IOException {\n        if (!Files.isDirectory(source)) return;\n        try (var stream = Files.walk(source)) {\n            for (Path path : stream.toList()) {\n                Path target = destination.resolve(source.relativize(path));\n                if (Files.isDirectory(path)) {\n                    Files.createDirectories(target);\n                    continue;\n                }\n                if (Files.exists(target)) continue;\n                Files.createDirectories(target.getParent());\n                if (isTextConfig(path)) {\n                    String contents = Files.readString(path, StandardCharsets.UTF_8);\n                    for (String legacyNamespace : LEGACY_NAMESPACES) {\n                        contents = contents.replace(legacyNamespace + ":",\n                                ScpClassifiedDirectiveMod.MODID + ":");\n                    }\n                    Files.writeString(target, contents, StandardCharsets.UTF_8);\n                } else {\n                    Files.copy(path, target, StandardCopyOption.COPY_ATTRIBUTES);\n                }\n            }\n        }\n    }\n\n    private static boolean isTextConfig(Path path) {\n        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);\n        return TEXT_EXTENSIONS.stream().anyMatch(name::endsWith);\n    }\n}\n''', encoding='utf-8')

# Forge capabilities are serialized under their attachment ResourceLocation, independently
# from registry missing mappings. Copy legacy keys to the unified namespace during read;
# the next normal save writes only the new key because only the new provider is attached.
mixin_dir = JAVA / 'mixin/compat'
mixin_dir.mkdir(parents=True, exist_ok=True)
(mixin_dir / 'LegacyCapabilityNbtMixin.java').write_text('''package com.bl4ues.scpclassifieddirective.mixin.compat;\n\nimport java.util.List;\nimport net.minecraft.nbt.CompoundTag;\nimport net.minecraft.nbt.Tag;\nimport net.minecraftforge.common.capabilities.CapabilityDispatcher;\nimport org.spongepowered.asm.mixin.Mixin;\nimport org.spongepowered.asm.mixin.injection.At;\nimport org.spongepowered.asm.mixin.injection.Inject;\nimport org.spongepowered.asm.mixin.injection.callback.CallbackInfo;\nimport com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;\n\n@Mixin(value = CapabilityDispatcher.class, remap = false)\npublic abstract class LegacyCapabilityNbtMixin {\n    private static final List<String> LEGACY_NAMESPACES = List.of(\n            "scp_additions", "scp_unity_extra_blocks", "scp_ublocks", "scpinventory");\n\n    @Inject(method = "deserializeNBT", at = @At("HEAD"))\n    private void scpClassifiedDirective$migrateLegacyCapabilityKeys(CompoundTag nbt, CallbackInfo ci) {\n        for (String legacyNamespace : LEGACY_NAMESPACES) {\n            String prefix = legacyNamespace + ":";\n            for (String key : List.copyOf(nbt.getAllKeys())) {\n                if (!key.startsWith(prefix)) continue;\n                String migrated = ScpClassifiedDirectiveMod.MODID + key.substring(legacyNamespace.length());\n                if (nbt.contains(migrated)) continue;\n                Tag value = nbt.get(key);\n                if (value != null) nbt.put(migrated, value.copy());\n            }\n        }\n    }\n}\n''', encoding='utf-8')

mixins = json.loads(MIXINS.read_text(encoding='utf-8'))
entry = 'compat.LegacyCapabilityNbtMixin'
if entry not in mixins['mixins']:
    mixins['mixins'].append(entry)
MIXINS.write_text(json.dumps(mixins, indent=2) + '\n', encoding='utf-8')

changelog = R / 'CHANGELOG.md'
text = changelog.read_text(encoding='utf-8')
needle = '- Added Forge missing-mapping migration so legacy registered world content resolves to the new namespace;\n'
extra = '- Added serialized capability-key migration so existing SCP Inventory contents and legacy player variables survive the namespace change;\n- Legacy configuration files now migrate embedded SCP resource identifiers to the unified namespace while preserving user customizations;\n- Existing world SavedData storage keys remain recognized internally so SCP-294 and SCP-914 state survives the rebrand;\n'
if 'serialized capability-key migration' not in text:
    text = text.replace(needle, needle + extra, 1)
changelog.write_text(text, encoding='utf-8')

print('compatibility migration prepared')
