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
extra = '- Added serialized capability-key migration so existing SCP Inventory contents and legacy player variables survive the namespace change;\n'
if extra not in text:
    text = text.replace(needle, needle + extra, 1)
changelog.write_text(text, encoding='utf-8')

print('compatibility migration prepared')
