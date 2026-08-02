package net.mcreator.scpadditions.facility;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Iterator;
import java.util.Map;

/** Removes retired standalone notices after all creative contributors run. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RetiredSignCreativeFilter {
    private RetiredSignCreativeFilter() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hideRetiredSigns(
            BuildCreativeModeTabContentsEvent event) {
        Iterator<Map.Entry<ItemStack, CreativeModeTab.TabVisibility>> iterator =
                event.getEntries().iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next().getKey();
            if (stack.is(FacilityModule.SCP_914_USAGE_NOTICE.get().asItem())
                    || stack.is(AreaUnderConstructionSignModule.ITEM.get())) {
                iterator.remove();
            }
        }
    }
}
