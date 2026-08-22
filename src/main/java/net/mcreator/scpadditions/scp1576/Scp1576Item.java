package net.mcreator.scpadditions.scp1576;

import com.bl4ues.scpinventory.event.ScpInventoryMaintenanceEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.mcreator.scpadditions.client.Scp1576ItemRenderer;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/** Hand-wound SCP-1576 item. */
public final class Scp1576Item extends Item implements GeoItem {
    private static final String CONTROLLER = "main";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WINDING = RawAnimation.begin().thenPlay("winding");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Scp1576Item() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("SCP-1576");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("Edisonian Afterlife Communicator"));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return Scp1576Manager.WIND_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(placePos).isAir()) {
            return InteractionResult.FAIL;
        }

        BlockState state = Scp1576Module.PLACED_BLOCK.get().defaultBlockState()
                .setValue(Scp1576PlacedBlock.FACING,
                        player.getDirection().getOpposite());
        if (!state.canSurvive(level, placePos)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!level.setBlock(placePos, state, 11)) {
            return InteractionResult.FAIL;
        }

        BlockEntity blockEntity = level.getBlockEntity(placePos);
        if (!(blockEntity instanceof Scp1576PlacedBlockEntity placed)) {
            level.removeBlock(placePos, false);
            return InteractionResult.FAIL;
        }

        ItemStack held = context.getItemInHand();
        ItemStack stored = held.copy();
        stored.setCount(1);
        placed.setStoredItem(stored);

        boolean clearedUsableMirror = player instanceof ServerPlayer serverPlayer
                && ScpInventoryMaintenanceEvents.discardActiveUsableFromSourceSlot(
                        serverPlayer, -1, stored);
        if (!clearedUsableMirror) {
            held.shrink(1);
        }

        level.gameEvent(player, GameEvent.BLOCK_PLACE, placePos);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
            InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!Scp1576Manager.canStart(stack)) {
            return InteractionResultHolder.fail(stack);
        }

        if (level instanceof ServerLevel serverLevel
                && player instanceof ServerPlayer serverPlayer) {
            if (!Scp1576Manager.beginWinding(serverPlayer, hand, stack)) {
                return InteractionResultHolder.fail(stack);
            }
            triggerAnim(serverPlayer, GeoItem.getOrAssignId(stack, serverLevel),
                    CONTROLLER, "winding");
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity,
            int timeLeft) {
        if (level instanceof ServerLevel serverLevel
                && entity instanceof ServerPlayer player) {
            if (Scp1576Manager.cancelWinding(player)) {
                triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel),
                        CONTROLLER, "idle");
            }
        }
        super.releaseUsing(stack, level, entity, timeLeft);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level,
            LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            Scp1576Manager.completeWinding(player, stack);
        }
        return stack;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private Scp1576ItemRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new Scp1576ItemRenderer();
                return renderer;
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack,
                    LocalPlayer player, HumanoidArm arm, ItemStack itemInHand,
                    float partialTick, float equipProcess, float swingProcess) {
                if (!player.isUsingItem()
                        || !player.getUseItem().is(Scp1576Module.SCP_1576.get())) {
                    return false;
                }

                InteractionHand usedHand = player.getUsedItemHand();
                HumanoidArm usedArm = usedHand == InteractionHand.MAIN_HAND
                        ? player.getMainArm() : player.getMainArm().getOpposite();
                if (arm != usedArm) return false;

                int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                poseStack.translate(side * 0.56F, -0.52F, -0.72F);
                return true;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 0, state -> {
            state.getController().setAnimation(IDLE);
            return PlayState.CONTINUE;
        }).triggerableAnim("winding", WINDING)
                .triggerableAnim("idle", IDLE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
