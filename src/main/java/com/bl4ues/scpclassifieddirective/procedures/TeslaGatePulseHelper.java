package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.List;
import java.util.function.Supplier;

public final class TeslaGatePulseHelper {
    private static final float LETHAL_DAMAGE = 200.0F;

    private TeslaGatePulseHelper() {
    }

    public static void pulseAndTransition(LevelAccessor world, double x, double y,
            double z, Supplier<? extends Block> expectedBlock,
            Supplier<? extends Block> nextBlock) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (world.getBlockState(pos).getBlock() != expectedBlock.get()) {
            return;
        }

        boolean manualOverride = world.getLevelData().getGameRules()
                .getBoolean(ScpClassifiedDirectiveModGameRules.TESLAGATEMANUALOVERRIDE);
        if (manualOverride) {
            emitOverrideParticles(world, x, y, z);
        }

        AABB lethalVolume = TeslaGateVolume.lethalArcAt(world, pos);
        List<LivingEntity> entities = world.getEntitiesOfClass(
                LivingEntity.class,
                TeslaGateVolume.motionCandidates(lethalVolume),
                entity -> TeslaGateVolume.intersectsOrCrossed(entity,
                        lethalVolume));
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

        ScpClassifiedDirectiveMod.queueServerWork(manualOverride ? 1 : 3,
                () -> TeslaGateTransitionHelper.transitionIfCurrent(
                        world, x, y, z, expectedBlock, nextBlock));
    }

    private static void emitOverrideParticles(LevelAccessor world,
            double x, double y, double z) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                x + 0.5D, y + 1.05D, z + 0.5D,
                8, 0.45D, 0.55D, 0.45D, 0.03D);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                x + 0.5D, y + 0.95D, z + 0.5D,
                2, 0.35D, 0.30D, 0.35D, 0.01D);
    }
}
