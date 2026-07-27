from pathlib import Path


def write(path: str, content: str) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content.rstrip() + "\n", encoding="utf-8")


for obsolete in (
    "src/main/java/net/mcreator/scpadditions/client/WetFloorItemGeoModel.java",
    "src/main/java/net/mcreator/scpadditions/client/WetFloorItemRenderer.java",
):
    Path(obsolete).unlink(missing_ok=True)

write("src/main/java/net/mcreator/scpadditions/facility/WetFloorBlockItem.java", r'''
package net.mcreator.scpadditions.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/** Vanilla-rendered inventory item for the GeckoLib world prop. */
public final class WetFloorBlockItem extends BlockItem {
    public WetFloorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.scp_additions.decorative_prop")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
''')

write("src/main/resources/assets/scp_additions/models/item/wet_floor.json", '''
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "scp_additions:item/wet_floor"
  }
}
''')

changelog = Path("CHANGELOG.md")
text = changelog.read_text(encoding="utf-8")
text = text.replace(
    "model-aware collision, and a matching 3D inventory render;",
    "model-aware collision, and a dedicated inventory/hand texture;",
)
changelog.write_text(text, encoding="utf-8")

print("Refined Wet Floor item rendering")
