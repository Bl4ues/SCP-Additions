package net.mcreator.scpadditions.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Registration and creative-tab integration for the construction-area sign. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AreaUnderConstructionSignModule {
    public static final String PATH = "area_under_construction_sign";
    public static final ResourceLocation ID = new ResourceLocation(
            ScpAdditionsMod.MODID, PATH);

    public static final AreaUnderConstructionSignBlock BLOCK =
            new AreaUnderConstructionSignBlock();
    public static final Item ITEM = new ConstructionSignItem(BLOCK,
            new Item.Properties());
    public static final BlockEntityType<AreaUnderConstructionSignBlockEntity>
            BLOCK_ENTITY = BlockEntityType.Builder.of(
                    AreaUnderConstructionSignBlockEntity::new, BLOCK)
                    .build(null);

    private AreaUnderConstructionSignModule() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS,
                helper -> helper.register(ID, BLOCK));
        event.register(ForgeRegistries.Keys.ITEMS,
                helper -> helper.register(ID, ITEM));
        event.register(Registries.BLOCK_ENTITY_TYPE,
                helper -> helper.register(ID, BLOCK_ENTITY));
    }

    @SubscribeEvent
    public static void addToFacilityTab(
            BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().equals(
                FacilityModule.SCP_FACILITY_BLOCKS.getId())) {
            event.accept(ITEM);
        }
    }

    /** Facility sections with the new prop immediately after the 914 notice. */
    public static List<FacilityModule.CreativeSection> creativeSections() {
        List<FacilityModule.CreativeSection> result = new ArrayList<>();
        for (FacilityModule.CreativeSection section :
                FacilityModule.creativeSections()) {
            List<ItemStack> items = new ArrayList<>();
            boolean inserted = false;
            for (ItemStack stack : section.items()) {
                items.add(stack.copy());
                if (stack.is(FacilityModule.SCP_914_USAGE_NOTICE.get().asItem())) {
                    items.add(new ItemStack(ITEM));
                    inserted = true;
                }
            }
            if (!inserted && section.sprite().getPath().endsWith(
                    "/proptab.png")) {
                items.add(new ItemStack(ITEM));
            }
            result.add(new FacilityModule.CreativeSection(section.sprite(),
                    List.copyOf(items)));
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
            return Component.literal("Area Under Construction Sign");
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_additions.decorative_prop")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }
    }
}
