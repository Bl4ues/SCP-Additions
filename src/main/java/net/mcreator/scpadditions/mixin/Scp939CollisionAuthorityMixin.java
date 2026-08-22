package net.mcreator.scpadditions.mixin;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.mcreator.scpadditions.entity.Scp939Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives the heavy SCP-939 authority over ordinary entity-body collisions. */
@Mixin(value = Scp939Entity.class, remap = false)
public abstract class Scp939CollisionAuthorityMixin extends PathfinderMob {
    protected Scp939CollisionAuthorityMixin(
            EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** Accesses the entity's private cleanup path before a difficulty despawn. */
    @Invoker(value = "releasePin", remap = false)
    protected abstract void scpadditions$releasePin(boolean kickedOff);

    /**
     * Thaumiel is backed by vanilla Peaceful. SCP-939 is a PathfinderMob rather
     * than Monster, and it is deliberately persistent, so vanilla has no reason
     * to remove an already-spawned specimen when the difficulty changes. Handle
     * that policy explicitly and clean up any pinned player's forced pose first.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true,
            remap = false)
    private void scpadditions$despawnOnThaumiel(CallbackInfo ci) {
        Scp939Entity self = (Scp939Entity) (Object) this;
        if (self.level().isClientSide
                || self.level().getDifficulty() != Difficulty.PEACEFUL) {
            return;
        }

        scpadditions$releasePin(false);
        self.discard();
        ci.cancel();
    }

    /** Body contact from mobs/players must not shove the 939 around. */
    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * Preserve normal 939-to-entity body pushing, except for the victim already
     * underneath it during pin/maul. That pair must be allowed to overlap.
     */
    @Override
    protected void doPush(Entity other) {
        Scp939Entity self = (Scp939Entity) (Object) this;
        byte action = self.getAction();
        if (other instanceof Player
                && (action == Scp939Entity.ACTION_PIN_LAND
                || action == Scp939Entity.ACTION_MAUL)) {
            return;
        }
        other.push(self);
    }
}
