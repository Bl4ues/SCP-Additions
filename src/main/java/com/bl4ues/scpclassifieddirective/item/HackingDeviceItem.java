package com.bl4ues.scpclassifieddirective.item;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/** Handheld Chaos Insurgency access-bypass tool. */
public final class HackingDeviceItem extends Item implements GeoItem {
    public static final int PASSAGE_COUNTDOWN_TICKS = 5 * 20;
    public static final int BLINK_INTERVAL_TICKS = 3;
    public static final int BLINK_COUNT = 3;
    public static final int BLINK_TOTAL_TICKS =
            BLINK_INTERVAL_TICKS * BLINK_COUNT * 2;

    private static final String TAG_COUNTDOWN_END = "HackingCountdownEnd";
    private static final String TAG_READY_AT = "HackingReadyAt";

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);

    public HackingDeviceItem() {
        super(new Item.Properties().stacksTo(1));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Hacking Device");
    }

    public static void armCooldown(ItemStack stack, long countdownEnd,
            long readyAt) {
        if (stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof HackingDeviceItem)) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(TAG_COUNTDOWN_END, Math.max(0L, countdownEnd));
        tag.putLong(TAG_READY_AT, Math.max(countdownEnd, readyAt));
    }

    public static boolean isReady(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof HackingDeviceItem)) {
            return false;
        }
        long readyAt = readyAt(stack);
        return readyAt <= 0L || level == null || level.getGameTime() >= readyAt;
    }

    public static boolean isCoolingDown(ItemStack stack, Level level) {
        return !isReady(stack, level);
    }

    public static long countdownEnd(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag()
                ? stack.getTag().getLong(TAG_COUNTDOWN_END) : 0L;
    }

    public static long readyAt(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag()
                ? stack.getTag().getLong(TAG_READY_AT) : 0L;
    }

    /** 5..1 while passage is open, then 0 during the three shutdown flashes. */
    public static int countdownSeconds(ItemStack stack, Level level) {
        if (level == null) return 0;
        long remaining = countdownEnd(stack) - level.getGameTime();
        if (remaining <= 0L) return 0;
        return (int) Math.min(5L, Math.max(1L, (remaining + 19L) / 20L));
    }

    public static boolean isBlinking(ItemStack stack, Level level) {
        if (level == null || isReady(stack, level)) return false;
        long end = countdownEnd(stack);
        return end > 0L && level.getGameTime() >= end;
    }

    /** Three short visible pulses after the countdown reaches zero. */
    public static boolean blinkVisible(ItemStack stack, Level level) {
        if (!isBlinking(stack, level)) return false;
        long elapsed = Math.max(0L, level.getGameTime() - countdownEnd(stack));
        long phase = elapsed / BLINK_INTERVAL_TICKS;
        return phase < BLINK_COUNT * 2L && (phase & 1L) == 0L;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HackingDeviceItemRenderer renderer;

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new HackingDeviceItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
        // Static model. Screen state is rendered independently of the body.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
