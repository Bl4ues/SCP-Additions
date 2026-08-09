package net.mcreator.scpadditions.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility registration for construction signs placed before the unified
 * SCP Sign template system. The item is intentionally absent from creative
 * menus; existing worlds continue to load without missing-registry damage.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AreaUnderConstructionSignModule {
    public static final String PATH = "area_under_construction_sign";

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS,
                    ScpAdditionsMod.MODID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS,
                    ScpAdditionsMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpAdditionsMod.MODID);

    public static final RegistryObject<AreaUnderConstructionSignBlock> BLOCK =
            BLOCKS.register(PATH, AreaUnderConstructionSignBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register(PATH,
            () -> new ConstructionSignItem(BLOCK.get(),
                    new Item.Properties()));
    public static final RegistryObject<BlockEntityType<
            AreaUnderConstructionSignBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register(PATH, () -> BlockEntityType.Builder.of(
                    AreaUnderConstructionSignBlockEntity::new, BLOCK.get())
                    .build(null));

    private AreaUnderConstructionSignModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    /** Curated sections with both retired standalone sign items removed. */
    public static List<FacilityModule.CreativeSection> creativeSections() {
        List<FacilityModule.CreativeSection> result = new ArrayList<>();
        for (FacilityModule.CreativeSection section :
                FacilityModule.creativeSections()) {
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack stack : section.items()) {
                if (stack.is(ITEM.get()) || stack.is(
                        FacilityModule.SCP_914_USAGE_NOTICE.get().asItem())) {
                    continue;
                }
                items.add(stack.copy());
                if (stack.is(FacilityModule.TV.get().asItem())) {
                    items.add(new ItemStack(
                            TeslaGateTerminalTableModule.ITEM.get()));
                }
            }
            result.add(new FacilityModule.CreativeSection(section.sprite(),
                    items));
        }
        return List.copyOf(result);
    }

    public static List<ItemStack> creativeTabDisplayStacks() {
        List<ItemStack> display = new ArrayList<>();
        for (FacilityModule.CreativeSection section : creativeSections()) {
            for (int index = 0; index < 9; index++) {
                display.add(ItemStack.EMPTY);
            }
            section.items().forEach(stack -> display.add(stack.copy()));
            while (display.size() % 9 != 0) display.add(ItemStack.EMPTY);
        }
        return display;
    }

    public static List<ItemStack> creativeTabIconStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (FacilityModule.CreativeSection section : creativeSections()) {
            for (ItemStack stack : section.items()) {
                boolean duplicate = stacks.stream().anyMatch(existing ->
                        ItemStack.isSameItemSameTags(existing, stack));
                if (!duplicate) stacks.add(stack.copy());
            }
        }
        return List.copyOf(stacks);
    }

    private static final class ConstructionSignItem extends BlockItem {
        private ConstructionSignItem(AreaUnderConstructionSignBlock block,
                Properties properties) {
            super(block, properties);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.literal("Area Under Construction Sign (Legacy)");
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.literal(
                            "Retired: use the configurable SCP Sign instead")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }
    }
}
