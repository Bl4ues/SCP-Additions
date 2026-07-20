from __future__ import annotations

from pathlib import Path
import re

ROOT = Path.cwd()
JAVA = ROOT / "src/main/java"
if not JAVA.exists():
    ROOT = Path(__file__).resolve().parents[2]
    JAVA = ROOT / "src/main/java"

blocks_path = JAVA / "net/mcreator/scpadditions/init/ScpAdditionsModBlocks.java"
items_path = JAVA / "net/mcreator/scpadditions/init/ScpAdditionsModItems.java"

blocks = blocks_path.read_text(encoding="utf-8")
items = items_path.read_text(encoding="utf-8")

registered_names = dict(re.findall(
    r"public static final Supplier<Block>\s+(\w+)\s*=\s*REGISTRY\.register\(\"([^\"]+)\"",
    blocks,
))

for field, registry_name in registered_names.items():
    items = items.replace(
        f"block(ScpAdditionsModBlocks.{field})",
        f'block("{registry_name}", ScpAdditionsModBlocks.{field})',
    )

old_helper = '''\tprivate static Supplier<Item> block(Supplier<Block> block) {
\t\treturn REGISTRY.register(BuiltInRegistries.BLOCK.getKey(block.get()).getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
\t}'''
new_helper = '''\tprivate static Supplier<Item> block(String registryName, Supplier<Block> block) {
\t\treturn REGISTRY.register(registryName,
\t\t\t\t() -> new BlockItem(block.get(), new Item.Properties()));
\t}'''
if old_helper not in items and new_helper not in items:
    raise RuntimeError("Could not locate generated block-item registration helper")
items = items.replace(old_helper, new_helper)
items_path.write_text(items, encoding="utf-8")

remaining = re.findall(r"block\(ScpAdditionsModBlocks\.(\w+)\)", items)
if remaining:
    raise RuntimeError("Unmigrated block item registrations: " + ", ".join(remaining))

# NeoForge 21 rejects automatically registered subscriber classes that do not
# actually declare any @SubscribeEvent methods. These classes are either plain
# holders or are registered explicitly elsewhere, so the annotation is invalid
# and unnecessary.
empty_subscribers = (
    JAVA / "net/mcreator/scpadditions/init/ScpAdditionsModGameRules.java",
    JAVA / "net/mcreator/scpadditions/world/features/StructureFeature.java",
    JAVA / "com/bl4ues/scpinventory/client/CoinCounterClient.java",
)
removed_subscribers = 0
for path in empty_subscribers:
    source = path.read_text(encoding="utf-8")
    if "@SubscribeEvent" in source:
        raise RuntimeError(f"Expected an empty subscriber but found event methods in {path}")
    transformed = re.sub(r"^import net\.neoforged\.fml\.common\.EventBusSubscriber;\s*\n", "", source, flags=re.MULTILINE)
    transformed = re.sub(r"^import net\.neoforged\.api\.distmarker\.Dist;\s*\n", "", transformed, flags=re.MULTILINE)
    transformed = re.sub(r"^import net\.neoforged\.fml\.common\.Mod;\s*\n", "", transformed, flags=re.MULTILINE)
    transformed, count = re.subn(r"^@EventBusSubscriber(?:\([^\n]*\))?\s*\n", "", transformed, flags=re.MULTILINE)
    if count:
        removed_subscribers += count
        path.write_text(transformed, encoding="utf-8")

print(
    f"Migrated {len(registered_names)} block item registrations away from early registry access; "
    f"removed {removed_subscribers} invalid empty event subscribers"
)
