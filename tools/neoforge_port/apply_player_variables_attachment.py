from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = ROOT / "src/main/java"
TARGET = JAVA_ROOT / "net/mcreator/scpadditions/network/ScpAdditionsModVariables.java"

text = TARGET.read_text(encoding="utf-8")

for obsolete in (
    "import net.minecraftforge.event.AttachCapabilitiesEvent;\n",
    "import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;\n",
    "import net.neoforged.neoforge.capabilities.ICapabilitySerializable;\n",
    "import net.neoforged.neoforge.capabilities.CapabilityToken;\n",
    "import net.neoforged.neoforge.capabilities.CapabilityManager;\n",
    "import net.neoforged.neoforge.capabilities.Capability;\n",
    "import net.minecraft.core.Direction;\n",
    "import net.minecraft.resources.ResourceLocation;\n",
):
    text = text.replace(obsolete, "")

attachment_imports = """import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
"""
if "import net.neoforged.neoforge.attachment.AttachmentType;" not in text:
    text = text.replace(
        "import net.minecraft.client.Minecraft;\n",
        "import net.minecraft.client.Minecraft;\n" + attachment_imports,
    )

text = re.sub(
    r"\n\t@SubscribeEvent\n\tpublic static void init\(RegisterCapabilitiesEvent event\) \{\n"
    r"\t\tevent\.register\(PlayerVariables\.class\);\n\t\}\n",
    "\n",
    text,
)

start_marker = "\tpublic static final Capability<PlayerVariables> PLAYER_VARIABLES_CAPABILITY"
end_marker = "\n\tpublic static class PlayerVariables {"
if start_marker in text:
    start = text.index(start_marker)
    end = text.index(end_marker, start)
    replacement = """\tpublic static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
\t\t\tDeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
\t\t\t\t\tScpAdditionsMod.MODID);

\tpublic static final Supplier<AttachmentType<PlayerVariables>> PLAYER_VARIABLES_ATTACHMENT =
\t\t\tATTACHMENTS.register("player_variables", () -> AttachmentType
\t\t\t\t\t.builder(PlayerVariables::new)
\t\t\t\t\t.serialize(new IAttachmentSerializer<CompoundTag, PlayerVariables>() {
\t\t\t\t\t\t@Override
\t\t\t\t\t\tpublic PlayerVariables read(IAttachmentHolder holder,
\t\t\t\t\t\t\t\tCompoundTag tag, HolderLookup.Provider provider) {
\t\t\t\t\t\t\tPlayerVariables variables = new PlayerVariables();
\t\t\t\t\t\t\tvariables.readNBT(tag);
\t\t\t\t\t\t\treturn variables;
\t\t\t\t\t\t}

\t\t\t\t\t\t@Override
\t\t\t\t\t\tpublic CompoundTag write(PlayerVariables variables,
\t\t\t\t\t\t\t\tHolderLookup.Provider provider) {
\t\t\t\t\t\t\treturn (CompoundTag) variables.writeNBT();
\t\t\t\t\t\t}
\t\t\t\t\t})
\t\t\t\t\t.build());

\tpublic static LazyOptional<PlayerVariables> getPlayerVariables(Entity entity) {
\t\tif (!(entity instanceof Player player) || entity instanceof FakePlayer) {
\t\t\treturn LazyOptional.empty();
\t\t}
\t\treturn LazyOptional.of(() -> player.getData(PLAYER_VARIABLES_ATTACHMENT));
\t}
"""
    text = text[:start] + replacement + text[end:]

GET_CAPABILITY = re.compile(
    r"(?P<expr>[A-Za-z_][A-Za-z0-9_]*"
    r"(?:\.[A-Za-z_][A-Za-z0-9_]*(?:\([^()\n]*\))?)*)"
    r"\.getCapability\((?:ScpAdditionsModVariables\.)?PLAYER_VARIABLES_CAPABILITY,\s*null\)"
)
text = GET_CAPABILITY.sub(
    lambda match: f"ScpAdditionsModVariables.getPlayerVariables({match.group('expr')})",
    text,
)
text = text.replace("\t\t\tevent.getOriginal().revive();\n", "")
TARGET.write_text(text, encoding="utf-8")

changed = 1
for path in JAVA_ROOT.rglob("*.java"):
    source = path.read_text(encoding="utf-8")
    updated = GET_CAPABILITY.sub(
        lambda match: f"ScpAdditionsModVariables.getPlayerVariables({match.group('expr')})",
        source,
    )

    updated = updated.replace(
        "mc.player.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY)",
        "ScpAdditionsModVariables.getPlayerVariables(mc.player)",
    )
    updated = updated.replace(
        "player.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY)",
        "ScpAdditionsModVariables.getPlayerVariables(player)",
    )
    updated = updated.replace(
        "target.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY)",
        "ScpAdditionsModVariables.getPlayerVariables(target)",
    )
    updated = updated.replace(
        "player\n                                .getCapability(PLAYER_VARIABLES_CAPABILITY, null)",
        "ScpAdditionsModVariables.getPlayerVariables(player)",
    )

    if updated != source:
        path.write_text(updated, encoding="utf-8")
        changed += 1

main = JAVA_ROOT / "net/mcreator/scpadditions/ScpAdditionsMod.java"
source = main.read_text(encoding="utf-8")
updated = source
if "ScpAdditionsModVariables.ATTACHMENTS.register(bus);" not in updated:
    updated = updated.replace(
        "        ScpInventoryCapability.REGISTRY.register(bus);\n",
        "        ScpInventoryCapability.REGISTRY.register(bus);\n"
        "        ScpAdditionsModVariables.ATTACHMENTS.register(bus);\n",
    )
if updated != source:
    main.write_text(updated, encoding="utf-8")
    changed += 1

remaining = []
for path in JAVA_ROOT.rglob("*.java"):
    source = path.read_text(encoding="utf-8")
    if "Capability<PlayerVariables> PLAYER_VARIABLES_CAPABILITY" in source:
        remaining.append(str(path.relative_to(ROOT)))

print(f"Updated {changed} files for player variable attachments")
if remaining:
    print("Unconverted player capability declarations:")
    print("\n".join(remaining))
    raise SystemExit(1)
