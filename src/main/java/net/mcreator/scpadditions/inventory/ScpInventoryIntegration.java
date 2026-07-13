package net.mcreator.scpadditions.inventory;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.integration.PlayerCurrencyAccess;
import net.mcreator.scpadditions.integration.PlayerItemAccess;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ScpInventoryIntegration {
    private static final PlayerItemAccess.AdditionalItemSource ITEM_SOURCE =
            ScpInventoryAccess::visibleStacks;

    private static final PlayerCurrencyAccess.CurrencyBackend CURRENCY_BACKEND =
            new PlayerCurrencyAccess.CurrencyBackend() {
                @Override
                public boolean accepts(ItemStack stack, Item currency) {
                    return ScpInventoryAccess.acceptsCurrency(stack, currency);
                }

                @Override
                public int count(net.minecraft.world.entity.player.Player player,
                        Item currency) {
                    return ScpInventoryAccess.countCurrency(player, currency);
                }

                @Override
                public ItemStack extractOne(
                        net.minecraft.world.entity.player.Player player,
                        Item currency) {
                    return ScpInventoryAccess.extractCurrency(player, currency);
                }

                @Override
                public int insert(net.minecraft.world.entity.player.Player player,
                        ItemStack stack) {
                    return ScpInventoryAccess.insertCurrency(player, stack);
                }
            };

    private ScpInventoryIntegration() {
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PlayerItemAccess.registerAdditionalSource(ITEM_SOURCE);
            PlayerCurrencyAccess.registerCustomInventoryBackend(CURRENCY_BACKEND);
        });
    }
}
