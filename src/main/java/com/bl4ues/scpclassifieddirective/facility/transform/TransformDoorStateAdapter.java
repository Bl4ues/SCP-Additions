package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorFamily;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorStage;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Shared state adapter for legacy heavy doors and rebuilt Gecko manual doors. */
final class TransformDoorStateAdapter {
    private static final List<DoorFamily> LEGACY = List.of(
            FacilityModule.DEFAULT_DOOR,
            FacilityModule.YELLOW_DOOR,
            FacilityModule.BLACK_DOOR);

    private TransformDoorStateAdapter() {
    }

    static Address address(BlockState state) {
        if (state == null || !FacilityModule.isFacilityDoor(state)) return null;
        if (FacilityGeckoDoorModule.isDoor(state)) {
            FacilityGeckoDoorModule.Family family =
                    FacilityGeckoDoorModule.family(state);
            DoorStage stage = switch (FacilityGeckoDoorModule.phase(state)) {
                case CLOSED -> DoorStage.CLOSED;
                case OPENING -> DoorStage.OPENING;
                case OPEN -> DoorStage.OPEN;
                case CLOSING -> DoorStage.CLOSING;
            };
            return new Address(null, family, stage, 0);
        }
        Block block = state.getBlock();
        for (DoorFamily family : LEGACY) {
            if (block == family.closed().get())
                return new Address(family, null, DoorStage.CLOSED, 0);
            if (block == family.open().get())
                return new Address(family, null, DoorStage.OPEN, 0);
            for (int i = 0; i < family.opening().size(); i++) {
                if (block == family.opening().get(i).get())
                    return new Address(family, null, DoorStage.OPENING, i);
            }
            for (int i = 0; i < family.closing().size(); i++) {
                if (block == family.closing().get(i).get())
                    return new Address(family, null, DoorStage.CLOSING, i);
            }
        }
        return null;
    }

    static int delay(Address address, boolean opening) {
        return address.geckoFamily() != null
                ? FacilityGeckoDoorModule.transitionTicks(opening)
                : Math.max(1, address.legacyFamily().frameDelay());
    }

    static BlockState begin(BlockState current, Address address,
            boolean opening) {
        if (address.geckoFamily() != null)
            return FacilityGeckoDoorModule.beginTransition(current, opening);
        List<net.minecraftforge.registries.RegistryObject<Block>> frames =
                opening ? address.legacyFamily().opening()
                        : address.legacyFamily().closing();
        return frames.isEmpty() ? current
                : copyFacing(current, frames.get(0).get());
    }

    static Advance advance(BlockState current, Address address,
            boolean opening) {
        if (address.geckoFamily() != null) {
            if (opening && address.stage() != DoorStage.OPENING) return null;
            if (!opening && address.stage() != DoorStage.CLOSING) return null;
            return new Advance(
                    FacilityGeckoDoorModule.finishTransition(current, opening),
                    true);
        }
        DoorFamily family = address.legacyFamily();
        if (opening) {
            if (address.stage() != DoorStage.OPENING) return null;
            int nextIndex = address.frame() + 1;
            Block next = nextIndex < family.opening().size()
                    ? family.opening().get(nextIndex).get()
                    : family.open().get();
            return new Advance(copyFacing(current, next),
                    nextIndex >= family.opening().size());
        }
        if (address.stage() != DoorStage.CLOSING) return null;
        int nextIndex = address.frame() + 1;
        Block next = nextIndex < family.closing().size()
                ? family.closing().get(nextIndex).get()
                : family.closed().get();
        return new Advance(copyFacing(current, next),
                nextIndex >= family.closing().size());
    }

    static void playSound(ServerLevel level, Vec3 center, Address address,
            boolean opening) {
        if (address.geckoFamily() != null) {
            FacilityGeckoDoorModule.playTransitionSound(level, center,
                    address.geckoFamily(), opening);
        } else {
            level.playSound(null, center.x, center.y, center.z,
                    (opening ? address.legacyFamily().openingSound()
                            : address.legacyFamily().closingSound()).get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private static BlockState copyFacing(BlockState from, Block target) {
        BlockState next = target.defaultBlockState();
        if (from != null
                && from.hasProperty(HorizontalDirectionalBlock.FACING)
                && next.hasProperty(HorizontalDirectionalBlock.FACING)) {
            next = next.setValue(HorizontalDirectionalBlock.FACING,
                    from.getValue(HorizontalDirectionalBlock.FACING));
        }
        return next;
    }

    record Address(DoorFamily legacyFamily,
            FacilityGeckoDoorModule.Family geckoFamily,
            DoorStage stage, int frame) {
        boolean directUse() {
            return geckoFamily != null || legacyFamily.directUse();
        }

        String id() {
            return geckoFamily != null
                    ? "gecko:" + geckoFamily.id()
                    : legacyFamily.id();
        }
    }

    record Advance(BlockState state, boolean done) {}
}
