package net.mcreator.scpadditions.facility;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal bootstrap for content migrated from SCP Unity Extra Blocks.
 *
 * Registry IDs intentionally remain under scp_unity_extra_blocks so the copied
 * resource pack resolves without rewriting models, blockstates or textures.
 */
public final class FacilityModule {
    public static final String MODID = "scp_unity_extra_blocks";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    private static final List<RegistryObject<Block>> CREATIVE_BLOCKS = new ArrayList<>();

    public static final RegistryObject<Block> TESLA_BOTTOM = registerStructureBlock("tesla_bottom");
    public static final RegistryObject<Block> TESLA_MID_1 = registerStructureBlock("tesla_mid_1");
    public static final RegistryObject<Block> TESLA_MID_2 = registerStructureBlock("tesla_mid_2");
    public static final RegistryObject<Block> TESLA_BOTTOM_ALT = registerStructureBlock("tesla_bottom_alt");
    public static final RegistryObject<Block> TESLA_TOP_ALT = registerStructureBlock("tesla_top_alt");

    public static final RegistryObject<Block> ARCHIVAL_BOTTOM = registerStructureBlock("archival_bottom");
    public static final RegistryObject<Block> ARCHIVAL_MID = registerStructureBlock("archival_mid");
    public static final RegistryObject<Block> ARCHIVAL_TOP = registerStructureBlock("archival_top");
    public static final RegistryObject<Block> ARCHIVAL_BOT_1 = registerStructureBlock("archival_bot_1");
    public static final RegistryObject<Block> ARCHIVAL_MID_2 = registerStructureBlock("archival_mid_2");

    public static final RegistryObject<Block> OFFICE_BOTTOM = registerStructureBlock("office_bottom");
    public static final RegistryObject<Block> OFFICE_MID = registerStructureBlock("office_mid");
    public static final RegistryObject<Block> OFFICE_TOP = registerStructureBlock("office_top");

    public static final RegistryObject<Block> SKYROOM_BOT_1 = registerStructureBlock("skyroom_bot_1");
    public static final RegistryObject<Block> SKYROOM_BOT_2 = registerStructureBlock("skyroom_bot_2");
    public static final RegistryObject<Block> SKYROOM_MID = registerStructureBlock("skyroom_mid");
    public static final RegistryObject<Block> SKYROOM_TOP_ALT = registerStructureBlock("skyroom_top_alt");
    public static final RegistryObject<Block> SKYROOM_BLOCK = registerStructureBlock("skyroom_block");

    public static final RegistryObject<Block> SECURITY_BOT = registerStructureBlock("security_bot");
    public static final RegistryObject<Block> SECURITY_MID = registerStructureBlock("security_mid");
    public static final RegistryObject<Block> SECURITY_TOP = registerStructureBlock("security_top");

    public static final RegistryObject<CreativeModeTab> FACILITY_MISC = TABS.register("scp_unity_extra_misc", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("item_group.scp_unity_extra_blocks.scp_unity_extra_misc"))
                    .icon(() -> new ItemStack(ARCHIVAL_BOTTOM.get()))
                    .displayItems((parameters, output) -> {
                        if (!ScpAdditionsModulesConfig.get().facility.enabled) {
                            return;
                        }
                        for (RegistryObject<Block> block : CREATIVE_BLOCKS) {
                            output.accept(block.get());
                        }
                    })
                    .withSearchBar()
                    .build());

    private FacilityModule() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
    }

    private static RegistryObject<Block> registerStructureBlock(String name) {
        RegistryObject<Block> block = BLOCKS.register(name, FacilityStructureBlock::new);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CREATIVE_BLOCKS.add(block);
        return block;
    }

    private static final class FacilityStructureBlock extends Block {
        private FacilityStructureBlock() {
            super(BlockBehaviour.Properties.of()
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .sound(SoundType.STONE)
                    .strength(1.0F, 10.0F));
        }

        @Override
        public int getLightBlock(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos) {
            return 15;
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            List<ItemStack> original = super.getDrops(state, builder);
            return original.isEmpty() ? Collections.singletonList(new ItemStack(this)) : original;
        }
    }
}
