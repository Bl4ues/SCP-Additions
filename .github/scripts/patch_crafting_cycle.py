from pathlib import Path
import re

path = Path("src/main/java/com/bl4ues/scpinventory/client/gui/components/CraftingPanel.java")
text = path.read_text(encoding="utf-8")

if "INGREDIENT_CYCLE_MS" in text:
    raise SystemExit(0)

old_constant = "    private static final long MISSING_FLASH_MS = 950L;\n"
if old_constant not in text:
    raise SystemExit("Missing flash constant was not found")
text = text.replace(
    old_constant,
    old_constant + "    private static final long INGREDIENT_CYCLE_MS = 1000L;\n",
    1,
)

render_pattern = re.compile(
    r"        for \(IngredientGroup group : entry\.ingredients\(\)\) \{.*?"
    r"            materialX \+= neededWidth \+ 4;\n"
    r"        \}",
    re.S,
)
render_replacement = '''        for (IngredientGroup group : entry.ingredients()) {
            String quantity = group.count() > 1 ? "x" + group.count() : "";
            int quantityWidth = quantity.isEmpty()
                    ? 0 : mc.font.width(ScpFonts.roboto(quantity));
            int neededWidth = MATERIAL_ICON_SIZE
                    + (quantity.isEmpty() ? 4 : 3 + quantityWidth);
            if (materialX + neededWidth > materialRight) {
                hiddenGroups++;
                continue;
            }
            ItemStack display = getCyclingIngredientDisplay(group.ingredient());
            if (!display.isEmpty()) {
                graphics.renderItem(display, materialX, materialY);
            }
            if (!group.available()) {
                graphics.fill(materialX, materialY,
                        materialX + MATERIAL_ICON_SIZE,
                        materialY + MATERIAL_ICON_SIZE,
                        flash ? MISSING_RED : DIM_OVERLAY);
                if (flash) drawFrame(graphics, materialX, materialY,
                        MATERIAL_ICON_SIZE, MISSING_RED);
            }
            if (!quantity.isEmpty()) {
                graphics.drawString(mc.font, ScpFonts.roboto(quantity),
                        materialX + MATERIAL_ICON_SIZE + 3, materialY + 7,
                        group.available() ? TEXT_WHITE : 0xFFA06F6F, false);
            }
            materialX += neededWidth + 4;
        }'''
text, count = render_pattern.subn(render_replacement, text, count=1)
if count != 1:
    raise SystemExit(f"Ingredient render loop replacements: {count}")

group_pattern = re.compile(
    r"    private List<IngredientGroup> buildIngredientGroups\(CraftingRecipe recipe,.*?"
    r"    private ItemStack getCurrentResult\(\)",
    re.S,
)
group_replacement = '''    private List<IngredientGroup> buildIngredientGroups(CraftingRecipe recipe,
                                                        List<ItemStack> available) {
        Map<String, MutableIngredientGroup> groups = new LinkedHashMap<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient == null || ingredient.isEmpty()) continue;
            ItemStack display = ScpCraftingRecipeHelper.representative(ingredient);
            if (display.isEmpty()) continue;
            String key = ingredientKey(ingredient);
            MutableIngredientGroup group = groups.computeIfAbsent(key,
                    ignored -> new MutableIngredientGroup(ingredient));
            group.count++;
        }

        List<IngredientGroup> result = new ArrayList<>();
        for (MutableIngredientGroup group : groups.values()) {
            int availableCount = 0;
            for (ItemStack stack : available) {
                if (group.ingredient.test(stack)) availableCount++;
            }
            result.add(new IngredientGroup(group.ingredient, group.count,
                    availableCount >= group.count));
        }
        return result;
    }

    private String ingredientKey(Ingredient ingredient) {
        StringBuilder key = new StringBuilder();
        for (ItemStack option : ingredient.getItems()) {
            if (option == null || option.isEmpty()) continue;
            key.append(BuiltInRegistries.ITEM.getKey(option.getItem()))
                    .append(';');
        }
        return key.toString();
    }

    private ItemStack getCyclingIngredientDisplay(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return ItemStack.EMPTY;
        ItemStack[] options = ingredient.getItems();
        if (options.length == 0) return ItemStack.EMPTY;
        int start = (int) ((System.currentTimeMillis() / INGREDIENT_CYCLE_MS)
                % options.length);
        for (int offset = 0; offset < options.length; offset++) {
            ItemStack option = options[(start + offset) % options.length];
            if (option != null && !option.isEmpty()) return option;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack getCurrentResult()'''
text, count = group_pattern.subn(group_replacement, text, count=1)
if count != 1:
    raise SystemExit(f"Ingredient group method replacements: {count}")

record_pattern = re.compile(
    r"    private record IngredientGroup\(ItemStack display, int count,.*?"
    r"    private static final class MutableIngredientGroup \{.*?"
    r"    \}\n"
    r"\}",
    re.S,
)
record_replacement = '''    private record IngredientGroup(Ingredient ingredient, int count,
                                   boolean available) {
    }

    private static final class MutableIngredientGroup {
        private final Ingredient ingredient;
        private int count;

        private MutableIngredientGroup(Ingredient ingredient) {
            this.ingredient = ingredient;
        }
    }
}'''
text, count = record_pattern.subn(record_replacement, text, count=1)
if count != 1:
    raise SystemExit(f"Ingredient record replacements: {count}")

path.write_text(text, encoding="utf-8")
