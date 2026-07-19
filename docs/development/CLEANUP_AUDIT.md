# Cleanup audit policy

The 3.0.6 cleanup is intentionally conservative. A file is removed only after its registration, Java callers, configuration references, model parents, sound definitions, and data-driven uses have been checked.

## Preserved intentionally

- Published registry IDs that are still required for old-world compatibility.
- Animation states used by existing worlds or live door transitions.
- Event subscribers whose registration is implicit through Forge annotations.
- Resource paths loaded by Minecraft convention rather than a direct textual reference.
- Normal, specular, and emissive companion textures that may be discovered by optional rendering integrations.
- The established network packet position reserved for the disabled login-music feature.

## Removed or consolidated

- Pre-3.0 SCP-294 drink item registrations replaced by the configurable NBT-backed cup.
- Their isolated Java item implementations, procedures, models, translations, and obsolete recipes.
- The unregistered SCP-059 and Geiger resource remnants.
- Legacy overlays whose capability flags had no writer.
- Plain Java helpers with no caller and no event-bus role.
- Byte-identical custom model geometry after every explicit parent reference was redirected.
- Byte-identical block/item texture copies only when all explicit references could be redirected safely.
- Processed legacy namespace copies after their registry-facing resources were migrated into `scp_additions`.

## Validation

The cleanup branch must pass:

1. Java 17 and Forge `clean build`;
2. parsing of every source JSON file;
3. old SCP-294 registry-removal and world-remapping checks;
4. missing-resource and duplicate-resource review against the compiled JAR;
5. a comparison of compiled JAR size and entry count against the pre-cleanup baseline.
