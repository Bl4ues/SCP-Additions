package net.mcreator.scpadditions.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import net.mcreator.scpadditions.client.DocumentClientHooks;
import net.mcreator.scpadditions.client.DocumentItemRenderer;
import net.mcreator.scpadditions.document.DocumentData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public final class DocumentItem extends Item {
    public DocumentItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private DocumentItemRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new DocumentItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level,
                                                   Player player,
                                                   InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isCreative()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal(
                        "Documents can only be edited in Creative mode."), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        DocumentData.ensureInitialized(stack);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> DocumentClientHooks.openEditor(
                            hand, stack.copy()));
        }
        return InteractionResultHolder.sidedSuccess(
                stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        String title = DocumentData.read(stack).title();
        return Component.literal(title == null || title.isBlank()
                ? "Document" : title);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        DocumentData.State state = DocumentData.read(stack);
        if (!state.category().isBlank()) {
            tooltip.add(Component.literal(state.category())
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal(
                        "Creative mode: right-click to edit")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
