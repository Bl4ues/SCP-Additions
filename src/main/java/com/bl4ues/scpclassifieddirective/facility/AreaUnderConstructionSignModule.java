package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.intercom.IntercomModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
import com.bl4ues.scpclassifieddirective.facility.speaker.SpeakerModule;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.init.UnifiedReaderItems;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility registration for construction signs placed before the unified
 * SCP Sign template system. The item is intentionally absent from creative
 * menus; existing worlds continue to load without missing-registry damage.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AreaUnderConstructionSignModule {
    public static final String PATH = "area_under_construction_sign";

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS,
                    ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS,
                    ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);

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
        ObjectContainmentUnitModule.register(bus);
        SurveillanceCameraPlaceholderModule.register(bus);
        SpeakerModule.register(bus);
        IntercomModule.register(bus);
    }

    @SubscribeEvent
    public static void addIntegratedFunctionalBlocks(
            BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == FacilityModule.SCP_FACILITY_BLOCKS.get()) {
            event.accept(ObjectContainmentUnitModule.ITEM.get());
            event.accept(SurveillanceCameraPlaceholderModule.ITEM.get());
            event.accept(SpeakerModule.ITEM.get());
            event.accept(IntercomModule.ITEM.get());
        }
    }

    /** Curated sections with retired/duplicate entries removed. */
    public static List<FacilityModule.CreativeSection> creativeSections() {
        List<FacilityModule.CreativeSection> result = new ArrayList<>();
        for (FacilityModule.CreativeSection section :
                FacilityModule.creativeSections()) {
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack stack : section.items()) {
                if (stack.is(ITEM.get())
                        || stack.is(FacilityModule.SCP_914_USAGE_NOTICE.get().asItem())
                        || stack.is(ScpClassifiedDirectiveModBlocks.TESLA_TERMINAL_OFF.get().asItem())) {
                    continue;
                }
                addUnique(items, stack);
                if (stack.is(UnifiedReaderItems.KEYCARD_READER.get())) {
                    addUnique(items,
                            new ItemStack(ObjectContainmentUnitModule.ITEM.get()));
                }
            }
            if (section.sprite().getPath().endsWith("/functionaltab.png")) {
                addUnique(items, new ItemStack(
                        SurveillanceCameraPlaceholderModule.ITEM.get()));
                addUnique(items, new ItemStack(SpeakerModule.ITEM.get()));
                addUnique(items, new ItemStack(IntercomModule.ITEM.get()));
            }
            result.add(new FacilityModule.CreativeSection(section.sprite(),
                    items));
        }
        return List.copyOf(result);
    }

    private static void addUnique(List<ItemStack> items, ItemStack stack) {
        boolean duplicate = items.stream().anyMatch(existing ->
                ItemStack.isSameItemSameTags(existing, stack));
        if (!duplicate) items.add(stack.copy());
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
