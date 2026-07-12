package net.mcreator.scpadditions.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Scp914Processor {
    private Scp914Processor() {
    }

    public static void process(LevelAccessor world, double x, double y, double z, Entity user,
                               Scp914RecipeManager.Setting setting) {
        if (!(world instanceof ServerLevel level)) {
            return;
        }

        BlockPos keyPos = BlockPos.containing(x, y, z);
        Scp914RecipeManager.MachineConfig machineConfig = Scp914RecipeManager.machineConfig();
        Direction front = getFacing(level, keyPos);
        ProcessingContext context = createContext(level, keyPos, setting, machineConfig, front);

        if (context.match().isEmpty() && context.players().isEmpty()) {
            return;
        }

        closeDoorsImmediately(level, keyPos);
        playSound(level, keyPos, "scp_additions:scp914refining");
        if (!context.players().isEmpty()) {
            playSound(level, BlockPos.containing(context.intakeCenter()), "scp_additions:scp914inside");
        }
        setRefining(world, true);

        ScpAdditionsMod.queueServerWork(machineConfig.startDelayTicks(), () -> {
            context.match().ifPresent(match -> applyRecipe(level, context.outputCenter(), match));
            for (ServerPlayer player : context.players()) {
                processPlayer(player, context.outputCenter(), setting);
            }
            ScpAdditionsMod.queueServerWork(machineConfig.finishDelayTicks(), () -> setRefining(world, false));
        });
    }

    private static ProcessingContext createContext(ServerLevel level, BlockPos keyPos,
                                                   Scp914RecipeManager.Setting setting,
                                                   Scp914RecipeManager.MachineConfig machineConfig,
                                                   Direction front) {
        Scp914RecipeManager.Offset intakeOffset = normalizeLegacyRangeOffset(machineConfig.intakeOffset());
        Scp914RecipeManager.Offset outputOffset = normalizeLegacyRangeOffset(machineConfig.outputOffset());
        Vec3 intakeCenter = centerOf(keyPos.offset(toWorldOffset(intakeOffset, front)));
        Vec3 outputCenter = centerOf(keyPos.offset(toWorldOffset(outputOffset, front)));
        AABB searchArea = new AABB(intakeCenter, intakeCenter).inflate(machineConfig.searchRadius());

        List<ItemEntity> itemInputs = level.getEntitiesOfClass(ItemEntity.class, searchArea,
                        item -> !item.isRemoved() && !item.getItem().isEmpty())
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(intakeCenter)))
                .toList();

        List<Entity> entityInputs = level.getEntitiesOfClass(Entity.class, searchArea,
                        entity -> !(entity instanceof ItemEntity)
                                && !(entity instanceof ServerPlayer)
                                && !entity.isRemoved())
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(intakeCenter)))
                .toList();

        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, searchArea,
                        player -> !player.isRemoved() && player.isAlive())
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(intakeCenter)))
                .toList();

        Optional<Scp914RecipeManager.RecipeMatch> match =
                Scp914RecipeManager.findRecipe(setting, itemInputs, entityInputs);
        return new ProcessingContext(match, players, intakeCenter, outputCenter);
    }

    private static Scp914RecipeManager.Offset normalizeLegacyRangeOffset(Scp914RecipeManager.Offset offset) {
        if (offset.x() == -4 && offset.y() == 0 && offset.z() == -3) {
            return new Scp914RecipeManager.Offset(-5, 0, -3);
        }
        if (offset.x() == 4 && offset.y() == 0 && offset.z() == -3) {
            return new Scp914RecipeManager.Offset(5, 0, -3);
        }
        return offset;
    }

    private static void processPlayer(ServerPlayer player, Vec3 outputCenter,
                                      Scp914RecipeManager.Setting setting) {
        if (!isAvailable(player)) {
            return;
        }

        player.connection.teleport(outputCenter.x, outputCenter.y, outputCenter.z,
                player.getYRot(), player.getXRot());

        switch (setting) {
            case ROUGH -> {
                hurtWithMessage(player, 18.0F, "scp914rough");
                ScpAdditionsMod.queueServerWork(10, () -> {
                    if (isAvailable(player)) {
                        hurtWithMessage(player, 50.0F, "scp914rough");
                    }
                });
            }
            case COARSE -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        200, 3, false, false));
                hurtWithMessage(player, 18.0F, "scp914coarse");
                ScpAdditionsMod.queueServerWork(200, () -> {
                    if (isAvailable(player)) {
                        hurtWithMessage(player, 50.0F, "scp914coarse");
                    }
                });
            }
            case ONE_TO_ONE -> {
                Scp914SkinManager.assignRandomSkin(player);
                awardMetamorphosisAdvancement(player);
            }
            case FINE -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        200, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP,
                        200, 1, false, false));
                ScpAdditionsMod.queueServerWork(200, () -> {
                    if (isAvailable(player)) {
                        hurtWithMessage(player, 50.0F, "scp914fine");
                    }
                });
            }
            case VERY_FINE -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        300, 5, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP,
                        300, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST,
                        300, 7, false, false));
                ScpAdditionsMod.queueServerWork(300, () -> {
                    if (isAvailable(player)) {
                        hurtWithMessage(player, 80.0F, "scp914veryfine");
                    }
                });
            }
        }
    }

    private static boolean isAvailable(ServerPlayer player) {
        return player != null && !player.isRemoved() && player.isAlive()
                && player.connection != null;
    }

    private static void hurtWithMessage(ServerPlayer player, float amount, String translationKey) {
        DamageSource source = new DamageSource(player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.GENERIC)) {
            @Override
            public Component getLocalizedDeathMessage(LivingEntity entity) {
                return Component.translatable("death.attack." + translationKey,
                        entity.getDisplayName());
            }
        };
        boolean wasAlive = player.isAlive();
        boolean damaged = player.hurt(source, amount);
        if (damaged && wasAlive && player.isDeadOrDying() && !"scp914coarse".equals(translationKey)) {
            player.level().playSound(null, player.blockPosition(),
                    ScpAdditionsModSounds.SCP914DEATH.get(),
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    private static void awardMetamorphosisAdvancement(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Advancement advancement = server.getAdvancements().getAdvancement(
                new ResourceLocation("scp_additions", "scp_914_metamorphosis"));
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
    }

    private static void applyRecipe(ServerLevel level, Vec3 outputCenter,
                                    Scp914RecipeManager.RecipeMatch match) {
        if (level.random.nextFloat() > match.recipe().chance()) {
            consumeInputs(match);
            return;
        }

        ItemStack firstInputStack = match.firstInputStack();
        consumeInputs(match);

        for (Scp914RecipeManager.ItemOutput output :
                Scp914RecipeManager.rollItemOutputs(match.recipe(), level.random)) {
            ItemStack outputStack = Scp914RecipeManager.createItemOutput(
                    output, firstInputStack, match.recipe().copyInputNbt());
            if (!outputStack.isEmpty()) {
                ItemEntity outputEntity = new ItemEntity(level, outputCenter.x,
                        outputCenter.y, outputCenter.z, outputStack);
                outputEntity.setPickUpDelay(10);
                level.addFreshEntity(outputEntity);
            }
        }

        for (Scp914RecipeManager.EntityOutput output : match.recipe().entityOutputs()) {
            Optional<EntityType<?>> type = Scp914RecipeManager.getEntityType(output);
            if (type.isEmpty()) {
                ScpAdditionsMod.LOGGER.warn(
                        "SCP-914 recipe {} points to missing entity output {}",
                        match.recipe().id(), output.entity());
                continue;
            }
            for (int i = 0; i < output.count(); i++) {
                Entity spawned = type.get().spawn(level,
                        BlockPos.containing(outputCenter), MobSpawnType.MOB_SUMMONED);
                if (spawned != null) {
                    spawned.setDeltaMovement(0, 0, 0);
                }
            }
        }
    }

    private static void consumeInputs(Scp914RecipeManager.RecipeMatch match) {
        for (Scp914RecipeManager.ItemUse itemUse : match.itemUses()) {
            ItemStack stack = itemUse.entity().getItem();
            stack.shrink(itemUse.count());
            if (stack.isEmpty()) {
                itemUse.entity().discard();
            } else {
                itemUse.entity().setItem(stack);
            }
        }
        for (Scp914RecipeManager.EntityUse entityUse : match.entityUses()) {
            if (entityUse.consume()) {
                entityUse.entity().discard();
            }
        }
    }

    private static void closeDoorsImmediately(ServerLevel level, BlockPos keyPos) {
        boolean closedAnyDoor = false;
        for (BlockPos pos : BlockPos.betweenClosed(
                keyPos.offset(-8, -4, -8), keyPos.offset(8, 4, 8))) {
            BlockPos target = pos.immutable();
            BlockState state = level.getBlockState(target);
            Block block = state.getBlock();
            if (block == ScpAdditionsModBlocks.SCP_914_INTAKE_DOOR.get()) {
                level.setBlock(target, copyProperties(state,
                        ScpAdditionsModBlocks.SCP_914_INTAKE_DOOR_CLOSED.get()
                                .defaultBlockState()), 3);
                closedAnyDoor = true;
            } else if (block == ScpAdditionsModBlocks.SCP_914_OUTPUT_DOOR.get()) {
                level.setBlock(target, copyProperties(state,
                        ScpAdditionsModBlocks.SCP_914_OUTPUT_DOOR_CLOSED.get()
                                .defaultBlockState()), 3);
                closedAnyDoor = true;
            }
        }

        if (closedAnyDoor) {
            playSound(level, keyPos, "scp_additions:scp914doorclose");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState copyProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Map.Entry<Property<?>, Comparable<?>> entry : from.getValues().entrySet()) {
            Property property = result.getBlock().getStateDefinition()
                    .getProperty(entry.getKey().getName());
            if (property != null) {
                try {
                    result = result.setValue((Property) property, (Comparable) entry.getValue());
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private static Direction getFacing(LevelAccessor world, BlockPos keyPos) {
        BlockState state = world.getBlockState(keyPos);
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return state.getValue(HorizontalDirectionalBlock.FACING);
        }
        return Direction.NORTH;
    }

    private static BlockPos toWorldOffset(Scp914RecipeManager.Offset offset,
                                          Direction front) {
        Direction rightFromViewer = front.getCounterClockWise();
        Vec3i right = rightFromViewer.getNormal();
        Vec3i forward = front.getNormal();
        return new BlockPos(
                right.getX() * offset.x() + forward.getX() * offset.z(),
                offset.y(),
                right.getZ() * offset.x() + forward.getZ() * offset.z());
    }

    private static Vec3 centerOf(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D);
    }

    private static void setRefining(LevelAccessor world, boolean value) {
        ScpAdditionsModVariables.MapVariables.get(world).Scp914refining = value;
        ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
    }

    private static void playSound(Level level, BlockPos pos, String soundId) {
        ResourceLocation id = new ResourceLocation(soundId);
        level.playSound(null, pos, ForgeRegistries.SOUND_EVENTS.getValue(id),
                SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private record ProcessingContext(
            Optional<Scp914RecipeManager.RecipeMatch> match,
            List<ServerPlayer> players,
            Vec3 intakeCenter,
            Vec3 outputCenter) {
    }
}
