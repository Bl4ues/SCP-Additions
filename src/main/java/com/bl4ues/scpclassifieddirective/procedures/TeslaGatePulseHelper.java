package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.Scp106Entity;
import com.bl4ues.scpclassifieddirective.event.Scp106AudioEvents;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class TeslaGatePulseHelper {
    private static final float LETHAL_DAMAGE = 200.0F;

    private TeslaGatePulseHelper() {
    }

    /**
     * Applies one Tesla Gate discharge to the one-block-thick arc volume.
     * Visual electricity is client-side and deliberately not emitted here.
     */
    public static void damageAt(LevelAccessor world, BlockPos pos) {
        damageAt(world, pos, List.of());
    }

    /**
     * Applies the pulse to entities currently crossing the arc and to entities
     * remembered crossing it while this exact charge was arming. A runner can
     * therefore not outrun the scheduled pulse after committing through the
     * live gate; backing out before the arc plane remains the intended juke.
     */
    public static void damageAt(LevelAccessor world, BlockPos pos,
            Collection<UUID> rememberedCrossers) {
        AABB lethalVolume = TeslaGateVolume.lethalArcAt(world, pos);
        LinkedHashSet<LivingEntity> entities = new LinkedHashSet<>(
                world.getEntitiesOfClass(
                        LivingEntity.class,
                        TeslaGateVolume.motionCandidates(lethalVolume),
                        entity -> TeslaGateVolume.intersectsOrCrossed(entity,
                                lethalVolume)));

        if (world instanceof ServerLevel server && rememberedCrossers != null) {
            for (UUID uuid : rememberedCrossers) {
                if (server.getEntity(uuid) instanceof LivingEntity living
                        && living.isAlive()) {
                    entities.add(living);
                }
            }
        }

        for (LivingEntity living : entities) {
            if (living instanceof Scp106Entity scp106) {
                Scp106AudioEvents.stopChaseFor(scp106);
                scp106.onTeslaGateHit();
                continue;
            }

            living.hurt(new DamageSource(living.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DamageTypes.GENERIC)) {
                @Override
                public Component getLocalizedDeathMessage(
                        LivingEntity messageEntity) {
                    String translateKey = "death.attack.teslagate";
                    if (this.getEntity() == null
                            && this.getDirectEntity() == null) {
                        return messageEntity.getKillCredit() != null
                                ? Component.translatable(
                                        translateKey + ".player",
                                        messageEntity.getDisplayName(),
                                        messageEntity.getKillCredit().getDisplayName())
                                : Component.translatable(translateKey,
                                        messageEntity.getDisplayName());
                    }
                    Component component = this.getEntity() == null
                            ? this.getDirectEntity().getDisplayName()
                            : this.getEntity().getDisplayName();
                    ItemStack itemStack = ItemStack.EMPTY;
                    if (this.getEntity() instanceof LivingEntity sourceLiving) {
                        itemStack = sourceLiving.getMainHandItem();
                    }
                    return !itemStack.isEmpty() && itemStack.hasCustomHoverName()
                            ? Component.translatable(translateKey + ".item",
                                    messageEntity.getDisplayName(), component,
                                    itemStack.getDisplayName())
                            : Component.translatable(translateKey,
                                    messageEntity.getDisplayName(), component);
                }
            }, LETHAL_DAMAGE);
        }
    }

    /** Legacy transient-state bridge retained for old worlds until the old state
     * blocks are removed. New gates use TeslaGateBlockEntity's timed sequence. */
    public static void pulseAndTransition(LevelAccessor world, double x, double y,
            double z, Supplier<? extends Block> expectedBlock,
            Supplier<? extends Block> nextBlock) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (world.getBlockState(pos).getBlock() != expectedBlock.get()) return;

        damageAt(world, pos);
        boolean manualOverride = world.getLevelData().getGameRules()
                .getBoolean(ScpClassifiedDirectiveModGameRules.TESLAGATEMANUALOVERRIDE);
        ScpClassifiedDirectiveMod.queueServerWork(manualOverride ? 1 : 3,
                () -> TeslaGateTransitionHelper.transitionIfCurrent(
                        world, x, y, z, expectedBlock, nextBlock));
    }
}
