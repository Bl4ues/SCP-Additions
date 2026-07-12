package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.network.ContextConfigSelectPacket;
import com.bl4ues.scpinventory.network.ItemConfigOpenRequestPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, value = Dist.CLIENT)
public final class ContextConfigClientEvents {
    private ContextConfigClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getKeyCode() != Keybinds.CONTEXT_CONFIG_SELECT.getKey().getValue()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (tryOpenItemConfig(mc)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        while (Keybinds.CONTEXT_CONFIG_SELECT.consumeClick()) {
            if (tryOpenItemConfig(mc)) {
                continue;
            }
            if (mc.screen == null) {
                ModNetwork.CHANNEL.sendToServer(new ContextConfigSelectPacket());
            }
        }
    }

    private static boolean tryOpenItemConfig(Minecraft mc) {
        ItemStack stack = findHoveredScpInventoryStack(mc);
        if (stack.isEmpty()) {
            stack = findHoveredVanillaContainerStack(mc);
        }
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return false;
        }

        ModNetwork.CHANNEL.sendToServer(new ItemConfigOpenRequestPacket(id.toString()));
        return true;
    }

    private static ItemStack findHoveredVanillaContainerStack(Minecraft mc) {
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) {
            return ItemStack.EMPTY;
        }
        try {
            Object result = getSlotUnderMouse(screen);
            if (result instanceof Slot slot && slot.hasItem()) {
                return slot.getItem().copy();
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static Object getSlotUnderMouse(AbstractContainerScreen<?> screen) throws Exception {
        try {
            Method method = AbstractContainerScreen.class.getDeclaredMethod("getSlotUnderMouse");
            method.setAccessible(true);
            return method.invoke(screen);
        } catch (NoSuchMethodException ignored) {
            Field field = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
            field.setAccessible(true);
            return field.get(screen);
        }
    }

    private static ItemStack findHoveredScpInventoryStack(Minecraft mc) {
        if (mc.screen == null || !"com.bl4ues.scpinventory.client.gui.ScpInventoryScreen".equals(mc.screen.getClass().getName())) {
            return ItemStack.EMPTY;
        }
        try {
            Object mode = getField(mc.screen, "mode");
            if (mode == null || !"INVENTORY".equals(mode.toString())) {
                return ItemStack.EMPTY;
            }

            Object itemList = getField(mc.screen, "itemList");
            Object inventoryObj = getField(mc.screen, "inventory");
            if (itemList == null || !(inventoryObj instanceof IScpInventory inventory)) {
                return ItemStack.EMPTY;
            }

            double mouseX = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
            double mouseY = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
            Method getClickedIndex = itemList.getClass().getMethod("getClickedIndex", double.class, double.class);
            Object clicked = getClickedIndex.invoke(itemList, mouseX, mouseY);
            if (!(clicked instanceof Integer index) || index < 0) {
                return ItemStack.EMPTY;
            }

            boolean showingKeys = Boolean.TRUE.equals(getField(mc.screen, "showingKeys"));
            List<ItemStack> stacks = showingKeys ? inventory.getKeys() : inventory.getInventory();
            if (index >= stacks.size()) {
                return ItemStack.EMPTY;
            }
            return stacks.get(index).copy();
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
