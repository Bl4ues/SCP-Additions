from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TARGET = ROOT / "src/main/java/net/mcreator/scpadditions/network/ScpAdditionsModVariables.java"

text = TARGET.read_text(encoding="utf-8")

if "import net.minecraft.network.RegistryFriendlyByteBuf;" not in text:
    text = text.replace(
        "import net.minecraft.network.FriendlyByteBuf;\n",
        "import net.minecraft.network.FriendlyByteBuf;\n"
        "import net.minecraft.network.RegistryFriendlyByteBuf;\n",
    )

text = text.replace(
    "\t\tpublic static WorldVariables load(CompoundTag tag) {",
    "\t\tpublic static final SavedData.Factory<WorldVariables> FACTORY =\n"
    "\t\t\t\tnew SavedData.Factory<>(WorldVariables::new, WorldVariables::load);\n\n"
    "\t\tpublic static WorldVariables load(CompoundTag tag, HolderLookup.Provider registries) {",
)
text = text.replace(
    "\t\tpublic static MapVariables load(CompoundTag tag) {",
    "\t\tpublic static final SavedData.Factory<MapVariables> FACTORY =\n"
    "\t\t\t\tnew SavedData.Factory<>(MapVariables::new, MapVariables::load);\n\n"
    "\t\tpublic static MapVariables load(CompoundTag tag, HolderLookup.Provider registries) {",
)

text = text.replace(
    "\t\tpublic CompoundTag save(CompoundTag nbt) {",
    "\t\tpublic CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {",
)

text = text.replace(
    "level.getDataStorage().computeIfAbsent(e -> WorldVariables.load(e), WorldVariables::new, DATA_NAME)",
    "level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME)",
)
text = text.replace(
    "getDataStorage().computeIfAbsent(e -> MapVariables.load(e), MapVariables::new, DATA_NAME)",
    "getDataStorage().computeIfAbsent(FACTORY, DATA_NAME)",
)

old_buffer = '''\t\tpublic static void buffer(SavedDataSyncMessage message, FriendlyByteBuf buffer) {
\t\t\tbuffer.writeInt(message.type);
\t\t\tif (message.data != null)
\t\t\t\tbuffer.writeNbt(message.data.save(new CompoundTag()));
\t\t}'''
new_buffer = '''\t\tpublic static void buffer(SavedDataSyncMessage message, FriendlyByteBuf buffer) {
\t\t\tbuffer.writeInt(message.type);
\t\t\tif (message.data != null) {
\t\t\t\tif (!(buffer instanceof RegistryFriendlyByteBuf registryBuffer)) {
\t\t\t\t\tthrow new IllegalStateException("Saved data sync requires a registry-aware buffer");
\t\t\t\t}
\t\t\t\tbuffer.writeNbt(message.data.save(
\t\t\t\t\t\tnew CompoundTag(), registryBuffer.registryAccess()));
\t\t\t}
\t\t}'''
if old_buffer in text:
    text = text.replace(old_buffer, new_buffer)
elif new_buffer not in text:
    raise RuntimeError("Could not migrate SavedDataSyncMessage.buffer")

TARGET.write_text(text, encoding="utf-8")
print("Migrated SCP Additions SavedData classes to Minecraft 1.21.1")
