from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[0]
if (ROOT / "src/main/java").exists():
    JAVA = ROOT / "src/main/java"
else:
    ROOT = Path(__file__).resolve().parents[2]
    JAVA = ROOT / "src/main/java"


def path(rel: str) -> Path:
    return JAVA / rel


def read(rel: str) -> str:
    return path(rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    p = path(rel)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8")


def add_import(text: str, import_line: str) -> str:
    line = f"import {import_line};"
    if line in text:
        return text
    package_end = text.find(";", text.find("package "))
    return text[:package_end + 1] + "\n\n" + line + text[package_end + 1:]


for java_file in JAVA.rglob("*.java"):
    text = java_file.read_text(encoding="utf-8")
    original = text
    text = text.replace("LegacyItemTags.setTag(LegacyItemTags, ", "LegacyItemTags.setTag(")
    text = re.sub(r"new ResourceLocation\(([^,\n]+)\)", r"ResourceLocation.parse(\1)", text)
    text = text.replace(".alwaysEat()", ".alwaysEdible()")
    if text != original:
        java_file.write_text(text, encoding="utf-8")

rel = "com/bl4ues/scpinventory/network/UseHotbarItemPacket.java"
text = read(rel)
text = add_import(text, "net.minecraft.network.RegistryFriendlyByteBuf")
text = text.replace(
    "buf.writeItem(msg.stack);",
    "ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, msg.stack);",
)
text = text.replace(
    "buf.readBoolean(), buf.readItem())",
    "buf.readBoolean(), ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf))",
)
write(rel, text)

rel = "com/bl4ues/scpinventory/client/gui/ScpInventoryScreen.java"
text = read(rel).replace(
    "return super.mouseScrolled(mouseX, mouseY, scrollY);",
    "return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);",
)
write(rel, text)

rel = "com/bl4ues/scpinventory/client/gui/ContextConfigScreen.java"
text = read(rel).replace(
    "nudge(0, delta * step(), 0);",
    "nudge(0, scrollY * step(), 0);",
)
write(rel, text)

rel = "com/bl4ues/scpinventory/crafting/ScpCraftingRecipeHelper.java"
text = read(rel)
text = add_import(text, "net.minecraft.world.item.crafting.CraftingInput")
text = add_import(text, "net.minecraft.world.item.crafting.RecipeHolder")
if "public static CraftingInput createInput" not in text:
    marker = "    public static Optional<CraftingRecipe> findMatching(Level level, List<ItemStack> grid) {"
    helper = """    public static CraftingInput createInput(List<ItemStack> grid) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(ScpCraftingState.GRID_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < ScpCraftingState.GRID_SIZE; i++) {
            ItemStack stack = grid != null && i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
            stacks.set(i, stack == null ? ItemStack.EMPTY : stack.copy());
        }
        return CraftingInput.of(3, 3, stacks);
    }

"""
    text = text.replace(marker, helper + marker)
text = text.replace(
    "RecipeType.CRAFTING, createContainer(grid), level)",
    "RecipeType.CRAFTING, createInput(grid), level)",
)
text = re.sub(
    r"Optional<\? extends Recipe<\?>> recipe = level\.getRecipeManager\(\)\.byKey\(id\);\n\s*if \(recipe\.isEmpty\(\) \|\| !\(recipe\.get\(\) instanceof CraftingRecipe crafting\)\)",
    "Optional<RecipeHolder<?>> recipe = level.getRecipeManager().byKey(id);\n        if (recipe.isEmpty() || !(recipe.get().value() instanceof CraftingRecipe crafting))",
    text,
)
write(rel, text)

rel = "com/bl4ues/scpinventory/crafting/ScpCraftingKnowledgeEvents.java"
text = read(rel)
text = text.replace(
    "RecipeType.CRAFTING, grid, player.level())",
    "RecipeType.CRAFTING, ScpCraftingRecipeHelper.createInput(grid.getItems()), player.level())",
)
text = text.replace("recipe.getId()", "recipe.id()")
write(rel, text)

rel = "com/bl4ues/scpinventory/crafting/ScpCraftingService.java"
text = read(rel)
text = add_import(text, "net.minecraft.world.item.crafting.CraftingInput")
text = add_import(text, "net.minecraft.world.item.crafting.RecipeHolder")
text = text.replace(
    "TransientCraftingContainer container = ScpCraftingRecipeHelper\n                .createContainer(state.getGrid());\n        Optional<CraftingRecipe> optional = player.level().getRecipeManager()\n                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,\n                        container, player.level());",
    "CraftingInput input = ScpCraftingRecipeHelper.createInput(state.getGrid());\n        Optional<RecipeHolder<CraftingRecipe>> optional = player.level().getRecipeManager()\n                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,\n                        input, player.level());",
)
text = text.replace(
    "CraftingRecipe recipe = optional.get();",
    "RecipeHolder<CraftingRecipe> recipeHolder = optional.get();\n        CraftingRecipe recipe = recipeHolder.value();",
)
text = text.replace(
    "recipe.assemble(container, player.level().registryAccess())",
    "recipe.assemble(input, player.level().registryAccess())",
)
text = text.replace("recipe.getRemainingItems(container)", "recipe.getRemainingItems(input)")
text = text.replace("state.learn(recipe.getId());", "state.learn(recipeHolder.id());")
write(rel, text)

rel = "com/bl4ues/scpinventory/client/gui/components/CraftingPanel.java"
text = read(rel)
text = text.replace(
    "TransientCraftingContainer container = ScpCraftingRecipeHelper\n                .createContainer(ScpCraftingClientState.getGrid());\n        return recipe.get().assemble(container, mc.level.registryAccess());",
    "return recipe.get().assemble(\n                ScpCraftingRecipeHelper.createInput(ScpCraftingClientState.getGrid()),\n                mc.level.registryAccess());",
)
write(rel, text)

rel = "com/bl4ues/scpinventory/crafting/ScpCraftingState.java"
text = read(rel)
text = add_import(text, "net.minecraft.core.HolderLookup")
text = add_import(text, "net.minecraft.nbt.Tag")
text = text.replace(
    "return fromTag(persisted.getCompound(ROOT_KEY));",
    "return fromTag(persisted.getCompound(ROOT_KEY), player.registryAccess());",
)
text = text.replace(
    "persisted.put(ROOT_KEY, toTag(data));",
    "persisted.put(ROOT_KEY, toTag(data, player.registryAccess()));",
)
text = text.replace(
    "public static CompoundTag toTag(Data data) {",
    "public static CompoundTag toTag(Data data, HolderLookup.Provider registries) {",
)
text = text.replace(
    "gridTag.add(stack.isEmpty() ? new CompoundTag() : stack.save(new CompoundTag()));",
    "gridTag.add(saveStack(stack, registries));",
)
text = text.replace(
    "public static Data fromTag(CompoundTag tag) {",
    "public static Data fromTag(CompoundTag tag, HolderLookup.Provider registries) {",
)
text = text.replace(
    "copySingle(ItemStack.of(gridTag.getCompound(i)))",
    "copySingle(ItemStack.parseOptional(registries, gridTag.getCompound(i)))",
)
if "private static CompoundTag saveStack" not in text:
    marker = "    private static ListTag saveIds(Set<ResourceLocation> ids) {"
    helper = """    private static CompoundTag saveStack(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        Tag saved = stack.saveOptional(registries);
        return saved instanceof CompoundTag compound ? compound : new CompoundTag();
    }

"""
    text = text.replace(marker, helper + marker)
write(rel, text)

rel = "com/bl4ues/scpinventory/crafting/ScpCraftingService.java"
text = read(rel).replace(
    "ScpCraftingState.toTag(state)",
    "ScpCraftingState.toTag(state, player.registryAccess())",
)
write(rel, text)

rel = "com/bl4ues/scpinventory/network/RequestCraftingStatePacket.java"
text = read(rel).replace(
    "ScpCraftingState.toTag(\n                                ScpCraftingState.load(player))",
    "ScpCraftingState.toTag(\n                                ScpCraftingState.load(player), player.registryAccess())",
)
write(rel, text)

rel = "com/bl4ues/scpinventory/client/ScpCraftingClientState.java"
text = read(rel)
text = add_import(text, "net.minecraft.client.Minecraft")
text = text.replace(
    "state = ScpCraftingState.fromTag(tag == null ? new CompoundTag() : tag);",
    "Minecraft minecraft = Minecraft.getInstance();\n        if (minecraft.level == null) {\n            state = new ScpCraftingState.Data();\n        } else {\n            state = ScpCraftingState.fromTag(\n                    tag == null ? new CompoundTag() : tag, minecraft.level.registryAccess());\n        }",
)
write(rel, text)

rel = "net/mcreator/scpadditions/item/SCP572Item.java"
text = read(rel)
text = add_import(text, "net.minecraft.world.entity.EquipmentSlotGroup")
text = add_import(text, "net.minecraft.world.item.component.ItemAttributeModifiers")
text = text.replace(
    "super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));",
    """super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON).attributes(
                ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, -2.0D,
                                        AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED,
                                new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4D,
                                        AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .build()));""",
)
text = re.sub(
    r"\n\t@Override\n\tpublic Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers\(EquipmentSlot equipmentSlot\) \{.*?\n\t\}\n",
    "\n",
    text,
    flags=re.S,
)
text = text.replace(
    "public boolean isCorrectToolForDrops(BlockState state)",
    "public boolean isCorrectToolForDrops(ItemStack stack, BlockState state)",
)
write(rel, text)

rel = "net/mcreator/scpadditions/entity/Scp173Entity.java"
text = read(rel)
text = text.replace("        setMaxUpStep(1.05F);\n", "")
text = text.replace(
    ".add(Attributes.ATTACK_DAMAGE, 0.0D);",
    ".add(Attributes.ATTACK_DAMAGE, 0.0D)\n                .add(Attributes.STEP_HEIGHT, 1.05D);",
)
text = text.replace(
    "protected void defineSynchedData() {\n        super.defineSynchedData();\n        entityData.define(SCRAPING, false);\n        entityData.define(MANUAL_YAW, 0.0F);\n        entityData.define(ACTIVATED, false);\n        entityData.define(ROUTINE_SPAWN, false);",
    "protected void defineSynchedData(SynchedEntityData.Builder builder) {\n        super.defineSynchedData(builder);\n        builder.define(SCRAPING, false);\n        builder.define(MANUAL_YAW, 0.0F);\n        builder.define(ACTIVATED, false);\n        builder.define(ROUTINE_SPAWN, false);",
)
text = text.replace(
    "public void lerpTo(double x, double y, double z, float yRot, float xRot, int increments, boolean teleport)",
    "public void lerpTo(double x, double y, double z, float yRot, float xRot, int increments)",
)
text = text.replace(
    "super.lerpTo(x, y, z, yRot, xRot, increments, teleport);",
    "super.lerpTo(x, y, z, yRot, xRot, increments);",
)
write(rel, text)

rel = "net/mcreator/scpadditions/entity/AbstractScp131Entity.java"
text = read(rel)
text = text.replace(
    "protected void defineSynchedData() {\n        super.defineSynchedData();\n        entityData.define(FOLLOWING, false);",
    "protected void defineSynchedData(SynchedEntityData.Builder builder) {\n        super.defineSynchedData(builder);\n        builder.define(FOLLOWING, false);",
)
write(rel, text)

for java_file in JAVA.rglob("*.java"):
    text = java_file.read_text(encoding="utf-8")
    original = text
    text = re.sub(
        r"new MobEffectInstance\((ScpAdditionsModMobEffects\.[A-Z0-9_]+)\.get\(\),",
        r"new MobEffectInstance(\1,",
        text,
    )
    text = re.sub(
        r"getEffect\((ScpAdditionsModMobEffects\.[A-Z0-9_]+)\.get\(\)\)",
        r"getEffect(\1)",
        text,
    )
    if text != original:
        java_file.write_text(text, encoding="utf-8")

rel = "net/mcreator/scpadditions/handler/Scp294DrinkHandler.java"
text = read(rel).replace(
    "                    effect,",
    "                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),",
)
write(rel, text)

for java_file in JAVA.rglob("*.java"):
    text = java_file.read_text(encoding="utf-8")
    original = text
    if "getAdvancement(" in text or "Advancement _adv" in text or "Advancement advancement" in text:
        text = text.replace(
            "import net.minecraft.advancements.Advancement;\n",
            "import net.minecraft.advancements.AdvancementHolder;\n",
        )
        text = re.sub(r"\bAdvancement\s+(_adv|advancement)\b", r"AdvancementHolder \1", text)
        text = text.replace(".getAdvancement(", ".get(")
    if text != original:
        java_file.write_text(text, encoding="utf-8")

for java_file in JAVA.rglob("*.java"):
    text = java_file.read_text(encoding="utf-8")
    if ".hasCustomHoverName()" not in text:
        continue
    text = add_import(text, "net.minecraft.core.component.DataComponents")
    text = re.sub(r"(\w+)\.hasCustomHoverName\(\)", r"\1.has(DataComponents.CUSTOM_NAME)", text)
    java_file.write_text(text, encoding="utf-8")

for java_file in JAVA.rglob("*.java"):
    text = java_file.read_text(encoding="utf-8")
    original = text
    text = re.sub(
        r"(\w+)\.saveWithFullMetadata\(\)",
        r"\1.saveWithFullMetadata(world.registryAccess())",
        text,
    )
    text = text.replace(
        "newBlockEntity.load(blockEntityTag);",
        "newBlockEntity.loadWithComponents(blockEntityTag, world.registryAccess());",
    )
    text = text.replace(
        "_be.load(_bnbt);",
        "_be.loadWithComponents(_bnbt, world.registryAccess());",
    )
    if text != original:
        java_file.write_text(text, encoding="utf-8")

rel = "net/mcreator/scpadditions/item/Scp914AssemblyKitItem.java"
text = read(rel)
text = add_import(text, "net.minecraft.nbt.NbtAccounter")
text = text.replace(
    "NbtIo.readCompressed(input)",
    "NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap())",
)
write(rel, text)

print("Applied NeoForge 1.21.1 gameplay/API migration round 2")
