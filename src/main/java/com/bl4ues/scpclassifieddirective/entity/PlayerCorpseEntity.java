package com.bl4ues.scpclassifieddirective.entity;

import com.bl4ues.scpclassifieddirective.inventory.capability.IScpInventory;
import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpEquipmentSlot;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.network.ScpClassifiedDirectiveModVariables;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import com.bl4ues.scpclassifieddirective.compat.MineZeroDeathCoordinator;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.world.inventory.PlayerCorpseMenu;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-owned physical remnant of a dead player.
 *
 * <p>Besides being a physical target for future recovery/SCP mechanics, the
 * corpse owns the inventory left behind by its player and exposes that state as
 * ordinary container storage. This makes looting use the same server-authority
 * and SCP-storage presentation already used by chests and other containers.</p>
 */
public final class PlayerCorpseEntity extends PathfinderMob
        implements MenuProvider {
    private static final int SETTLE_TICKS = 18;
    private static final int EMPTY_DESPAWN_TICKS = 20 * 60;
    private static final int POSE_VARIANTS = 6;
    private static final int MIN_CONTAINER_SIZE = 9;
    private static final EquipmentSlot[] VISUAL_ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID =
            SynchedEntityData.defineId(PlayerCorpseEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(PlayerCorpseEntity.class,
                    EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> LOGICAL_DEATH =
            SynchedEntityData.defineId(PlayerCorpseEntity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> POSE_VARIANT =
            SynchedEntityData.defineId(PlayerCorpseEntity.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SETTLED =
            SynchedEntityData.defineId(PlayerCorpseEntity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> CUSTOM_SKIN =
            SynchedEntityData.defineId(PlayerCorpseEntity.class,
                    EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM_MODEL =
            SynchedEntityData.defineId(PlayerCorpseEntity.class,
                    EntityDataSerializers.BOOLEAN);

    private SimpleContainer inventory;
    private boolean scpInventoryMode;
    private boolean inventoryDirty;
    private int emptyTicks;

    public PlayerCorpseEntity(EntityType<? extends PlayerCorpseEntity> type,
            Level level) {
        super(type, level);
        bindInventory(new SimpleContainer(MIN_CONTAINER_SIZE));
        setPersistenceRequired();
        setNoAi(true);
        setMaxUpStep(0.0F);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // A corpse does not pathfind, look around, retaliate, or wander.
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER_ID, Optional.empty());
        entityData.define(OWNER_NAME, "");
        entityData.define(LOGICAL_DEATH, false);
        entityData.define(POSE_VARIANT, 0);
        entityData.define(SETTLED, false);
        entityData.define(CUSTOM_SKIN, "");
        entityData.define(SLIM_MODEL, false);
    }

    public void initializeFrom(ServerPlayer player, boolean logicalDeath) {
        if (player == null) return;
        String name = player.getGameProfile().getName();
        entityData.set(OWNER_ID, Optional.of(player.getUUID()));
        entityData.set(OWNER_NAME, name);
        entityData.set(LOGICAL_DEATH, logicalDeath);
        entityData.set(POSE_VARIANT, random.nextInt(POSE_VARIANTS));
        entityData.set(SETTLED, false);
        entityData.set(CUSTOM_SKIN, snapshotCustomSkin(player));
        entityData.set(SLIM_MODEL, resolveSlimModel(player));
        scpInventoryMode = ScpClassifiedDirectiveModulesConfig.get().inventory.enabled;
        emptyTicks = 0;
        setCustomName(Component.literal(name));
        setCustomNameVisible(false);
        moveTo(player.getX(), player.getY(), player.getZ(),
                player.yBodyRot, 0.0F);
        setYBodyRot(player.yBodyRot);
        setYHeadRot(player.yBodyRot);
        yBodyRotO = player.yBodyRot;
        yHeadRotO = player.yBodyRot;

        // Preserve only a restrained fraction of the player's momentum. This
        // gives the fake ragdoll a little physical follow-through without
        // turning bodies into low-friction projectiles.
        Vec3 movement = player.getDeltaMovement();
        double vertical = Math.max(-0.60D,
                Math.min(0.08D, movement.y * 0.35D));
        setDeltaMovement(movement.x * 0.22D, vertical,
                movement.z * 0.22D);
        hasImpulse = true;
    }

    /**
     * Moves the dead player's canonical inventory into this body.
     *
     * <p>Normal deaths respect keepInventory. MineZero logical deaths always
     * move the inventory because the dead spectator must no longer own loot;
     * rollback restores the checkpoint snapshot later.</p>
     */
    public void captureInventoryFrom(ServerPlayer player) {
        if (player == null || level().isClientSide) return;

        clearVisualArmor();
        scpInventoryMode = ScpClassifiedDirectiveModulesConfig.get().inventory.enabled;
        boolean keepInventory = !logicalDeath()
                && player.level().getGameRules()
                .getBoolean(GameRules.RULE_KEEPINVENTORY);
        if (keepInventory) {
            replaceInventory(List.of(), MIN_CONTAINER_SIZE);
            return;
        }

        List<ItemStack> captured = new ArrayList<>();
        int capacityHint = MIN_CONTAINER_SIZE;
        if (scpInventoryMode) {
            IScpInventory scp = player.getCapability(ScpInventoryCapability.INSTANCE)
                    .resolve().orElse(null);
            if (scp != null) {
                // Reserve the complete canonical capacity, not merely the number
                // of occupied stacks. This covers upgraded main inventories,
                // every key slot, equipment, the usable slot and the current
                // document collection. captured.size() remains the final safety
                // net for future inventory sections with no declared maximum.
                capacityHint = scp.getMaxMainSlots()
                        + IScpInventory.MAX_KEY_COUNT
                        + scp.getDocuments().size()
                        + ScpEquipmentSlot.values().length + 1;
                collect(captured, scp.getInventory());
                collect(captured, scp.getKeys());
                collect(captured, scp.getDocuments());
                for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
                    EquipmentSlot visualSlot = visualArmorSlot(slot);
                    if (visualSlot == null) {
                        collect(captured, scp.getEquipment(slot));
                    } else {
                        collectEquippedArmor(captured, visualSlot,
                                scp.getEquipment(slot));
                    }
                }
                collect(captured, scp.getActiveUsable());
                clearScpInventory(scp);
                ModNetwork.syncTo(player, scp);
            } else {
                // Capability attachment should be unconditional, but falling
                // back to vanilla contents is safer than silently deleting a
                // player's inventory if another mod disrupts capability setup.
                Inventory vanilla = player.getInventory();
                capacityHint = vanillaCapacity(vanilla);
                collectVanilla(captured, vanilla);
            }

            // SCP Inventory owns the canonical stacks. Vanilla hand/armor slots
            // may contain temporary mirrors and must not be allowed to drop a
            // second copy after this death event returns.
            player.getInventory().clearContent();
            player.getInventory().setChanged();
        } else {
            Inventory vanilla = player.getInventory();
            capacityHint = vanillaCapacity(vanilla);
            collectVanilla(captured, vanilla);
            player.getInventory().clearContent();
            player.getInventory().setChanged();
        }

        replaceInventory(captured, capacityHint);
    }

    private static void clearScpInventory(IScpInventory scp) {
        if (scp == null) return;
        int capacity = scp.getMaxMainSlots();
        List<ItemStack> emptyMain = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) emptyMain.add(ItemStack.EMPTY);
        scp.setInventory(emptyMain);
        scp.setKeys(List.of());
        scp.setDocuments(List.of());
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            scp.setEquipment(slot, ItemStack.EMPTY);
        }
        scp.setActiveUsable(ItemStack.EMPTY);
        scp.setCoinCount(0);
        // Preserve upgraded main-slot capacity across death. Only contents move
        // to the body; inventory progression is still owned by the player.
        scp.setMaxMainSlots(capacity);
    }

    private static int vanillaCapacity(Inventory inventory) {
        if (inventory == null) return MIN_CONTAINER_SIZE;
        return inventory.items.size() + inventory.armor.size()
                + inventory.offhand.size();
    }

    private void collectVanilla(List<ItemStack> target,
            Inventory inventory) {
        if (inventory == null) return;
        collect(target, inventory.items);
        collectVanillaArmor(target, inventory, 0, EquipmentSlot.FEET);
        collectVanillaArmor(target, inventory, 1, EquipmentSlot.LEGS);
        collectVanillaArmor(target, inventory, 2, EquipmentSlot.CHEST);
        collectVanillaArmor(target, inventory, 3, EquipmentSlot.HEAD);
        collect(target, inventory.offhand);
    }

    private void collectVanillaArmor(List<ItemStack> target,
            Inventory inventory, int index, EquipmentSlot slot) {
        if (index < 0 || index >= inventory.armor.size()) return;
        collectEquippedArmor(target, slot, inventory.armor.get(index));
    }

    private static EquipmentSlot visualArmorSlot(ScpEquipmentSlot slot) {
        if (slot == null) return null;
        return switch (slot) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private void collectEquippedArmor(List<ItemStack> target,
            EquipmentSlot slot, ItemStack stack) {
        if (target == null || slot == null || stack == null || stack.isEmpty()) {
            return;
        }
        ItemStack copy = stack.copy();
        target.add(copy);
        setItemSlot(slot, copy.copy());
    }

    private static void collect(List<ItemStack> target,
            Iterable<ItemStack> stacks) {
        if (target == null || stacks == null) return;
        for (ItemStack stack : stacks) collect(target, stack);
    }

    private static void collect(List<ItemStack> target, ItemStack stack) {
        if (target == null || stack == null || stack.isEmpty()) return;
        target.add(stack.copy());
    }

    private void replaceInventory(List<ItemStack> stacks, int capacityHint) {
        int itemCount = stacks == null ? 0 : stacks.size();
        int required = Math.max(itemCount, Math.max(MIN_CONTAINER_SIZE,
                capacityHint));
        int size = ((required + 8) / 9) * 9;
        SimpleContainer replacement = new SimpleContainer(size);
        if (stacks != null) {
            for (int i = 0; i < stacks.size(); i++) {
                replacement.setItem(i, stacks.get(i).copy());
            }
        }
        bindInventory(replacement);
    }

    private void bindInventory(SimpleContainer replacement) {
        inventory = replacement == null
                ? new SimpleContainer(MIN_CONTAINER_SIZE) : replacement;
        inventory.addListener(container -> {
            if (!level().isClientSide) inventoryDirty = true;
        });
        inventoryDirty = true;
    }

    private void clearVisualArmor() {
        for (EquipmentSlot slot : VISUAL_ARMOR_SLOTS) {
            setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private void syncVisualArmorFromInventory() {
        if (level().isClientSide) return;
        for (EquipmentSlot slot : VISUAL_ARMOR_SLOTS) {
            ItemStack visual = getItemBySlot(slot);
            if (!visual.isEmpty() && !containsMatchingStack(visual)) {
                setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private boolean containsMatchingStack(ItemStack wanted) {
        if (wanted == null || wanted.isEmpty()) return false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stored = inventory.getItem(slot);
            if (!stored.isEmpty()
                    && ItemStack.isSameItemSameTags(stored, wanted)
                    && stored.getCount() >= wanted.getCount()) {
                return true;
            }
        }
        return false;
    }

    private static String snapshotCustomSkin(ServerPlayer player) {
        return player.getCapability(
                        ScpClassifiedDirectiveModVariables.PLAYER_VARIABLES_CAPABILITY)
                .map(variables -> variables.scp914Skin == null
                        ? "" : variables.scp914Skin)
                .orElse("");
    }

    private static boolean resolveSlimModel(ServerPlayer player) {
        try {
            for (Property property : player.getGameProfile().getProperties()
                    .get("textures")) {
                String json = new String(Base64.getDecoder()
                        .decode(property.getValue()), StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonObject textures = root.has("textures")
                        && root.get("textures").isJsonObject()
                        ? root.getAsJsonObject("textures") : null;
                JsonObject skin = textures != null && textures.has("SKIN")
                        && textures.get("SKIN").isJsonObject()
                        ? textures.getAsJsonObject("SKIN") : null;
                if (skin == null) continue;
                JsonObject metadata = skin.has("metadata")
                        && skin.get("metadata").isJsonObject()
                        ? skin.getAsJsonObject("metadata") : null;
                return metadata != null && metadata.has("model")
                        && "slim".equalsIgnoreCase(
                        metadata.get("model").getAsString());
            }
        } catch (RuntimeException ignored) {
            // An invalid/incomplete profile should not be able to prevent a
            // corpse from spawning. Classic is the safest rendering fallback.
        }
        return false;
    }

    public UUID ownerId() {
        return entityData.get(OWNER_ID).orElse(null);
    }

    public String ownerName() {
        return entityData.get(OWNER_NAME);
    }

    public boolean logicalDeath() {
        return entityData.get(LOGICAL_DEATH);
    }

    public int poseVariant() {
        return Math.floorMod(entityData.get(POSE_VARIANT), POSE_VARIANTS);
    }

    public boolean settled() {
        return entityData.get(SETTLED);
    }

    public String customSkin() {
        return entityData.get(CUSTOM_SKIN);
    }

    public boolean slimModel() {
        return entityData.get(SLIM_MODEL);
    }

    public SimpleContainer container() {
        return inventory;
    }

    public int containerSize() {
        return inventory.getContainerSize();
    }

    public boolean scpInventoryMode() {
        return scpInventoryMode;
    }

    @Override
    public Component getDisplayName() {
        String name = ownerName();
        return Component.literal(name == null || name.isBlank()
                ? "Unknown Personnel" : name);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId,
            Inventory playerInventory, Player player) {
        return new PlayerCorpseMenu(containerId, playerInventory, this);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!ScpClassifiedDirectiveModulesConfig.get().deathBodies.enabled
                || serverPlayer.isSpectator()
                || serverPlayer.distanceToSqr(this) > 64.0D) {
            return InteractionResult.PASS;
        }

        NetworkHooks.openScreen(serverPlayer, this, buffer -> {
            buffer.writeInt(getId());
            buffer.writeVarInt(containerSize());
            buffer.writeBoolean(scpInventoryMode());
        });
        return InteractionResult.CONSUME;
    }

    @Override
    public void tick() {
        setNoAi(true);
        setMaxUpStep(0.0F);
        super.tick();

        Vec3 movement = getDeltaMovement();
        if (onGround()) {
            if (!settled() && tickCount < SETTLE_TICKS) {
                setDeltaMovement(movement.x * 0.48D, 0.0D,
                        movement.z * 0.48D);
            } else {
                setDeltaMovement(Vec3.ZERO);
            }
        } else {
            // A body that died in mid-air keeps a little horizontal inertia
            // while gravity does the real vertical settling.
            setDeltaMovement(movement.x * 0.82D, movement.y,
                    movement.z * 0.82D);
        }
        getNavigation().stop();
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());

        if (level().isClientSide) return;
        if (!ScpClassifiedDirectiveModulesConfig.get().deathBodies.enabled) {
            discard();
            return;
        }

        if (inventoryDirty) {
            inventoryDirty = false;
            syncVisualArmorFromInventory();
        }

        if (!settled() && tickCount >= SETTLE_TICKS) {
            entityData.set(SETTLED, true);
            setDeltaMovement(Vec3.ZERO);
        }

        if (logicalDeath() && level() instanceof ServerLevel serverLevel) {
            UUID id = ownerId();
            ServerPlayer owner = id == null ? null
                    : serverLevel.getServer().getPlayerList().getPlayer(id);
            // MineZero rewinds remove bodies created by the discarded timeline.
            if (owner == null || !MineZeroDeathCoordinator.isLogicallyDead(owner)) {
                discard();
            }
        }

        if (inventory.isEmpty()) {
            if (++emptyTicks >= EMPTY_DESPAWN_TICKS) {
                discard();
            }
        } else {
            emptyTicks = 0;
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID id = ownerId();
        if (id != null) tag.putUUID("Owner", id);
        tag.putString("OwnerName", ownerName());
        tag.putBoolean("LogicalDeath", logicalDeath());
        tag.putInt("PoseVariant", poseVariant());
        tag.putBoolean("Settled", settled());
        tag.putString("CustomSkin", customSkin());
        tag.putBoolean("SlimModel", slimModel());
        tag.putBoolean("ScpInventoryMode", scpInventoryMode);
        tag.putInt("CorpseContainerSize", containerSize());
        tag.putInt("EmptyTicks", emptyTicks);

        ListTag items = new ListTag();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Slot", slot);
            stack.save(itemTag);
            items.add(itemTag);
        }
        tag.put("CorpseItems", items);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(OWNER_ID, tag.hasUUID("Owner")
                ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(OWNER_NAME, tag.getString("OwnerName"));
        entityData.set(LOGICAL_DEATH, tag.getBoolean("LogicalDeath"));
        entityData.set(POSE_VARIANT,
                Math.floorMod(tag.getInt("PoseVariant"), POSE_VARIANTS));
        // Legacy bodies predate the flag and should load already settled rather
        // than theatrically collapsing again every time their chunk is opened.
        entityData.set(SETTLED,
                !tag.contains("Settled") || tag.getBoolean("Settled"));
        entityData.set(CUSTOM_SKIN, tag.getString("CustomSkin"));
        entityData.set(SLIM_MODEL,
                tag.contains("SlimModel") && tag.getBoolean("SlimModel"));
        scpInventoryMode = tag.getBoolean("ScpInventoryMode");
        emptyTicks = Math.max(0, tag.getInt("EmptyTicks"));

        int requestedSize = tag.contains("CorpseContainerSize")
                ? tag.getInt("CorpseContainerSize") : MIN_CONTAINER_SIZE;
        int size = Math.max(MIN_CONTAINER_SIZE,
                ((requestedSize + 8) / 9) * 9);
        SimpleContainer loaded = new SimpleContainer(size);
        ListTag items = tag.getList("CorpseItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot >= 0 && slot < size) {
                loaded.setItem(slot, ItemStack.of(itemTag));
            }
        }
        bindInventory(loaded);

        String name = ownerName();
        if (name != null && !name.isBlank()) {
            setCustomName(Component.literal(name));
            setCustomNameVisible(false);
        }
        setNoAi(true);
        setPersistenceRequired();
    }
}
