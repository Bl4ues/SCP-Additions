package com.bl4ues.scpinventory.debug;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ScpInventoryDebug {

    private static final Logger LOGGER = LogManager.getLogger(ScpInventoryMod.MODID + "/UsableDebug");
    public static final boolean USABLE_DEBUG = Boolean.getBoolean("scpinventory.debugUsable");

    private ScpInventoryDebug() {
    }

    public static void usable(String message, Object... args) {
        if (!USABLE_DEBUG) {
            return;
        }
        LOGGER.warn("[SCP-USABLE-DEBUG] " + message, args);
    }

    public static String stack(ItemStack stack) {
        if (stack == null) {
            return "null";
        }
        if (stack.isEmpty()) {
            return "empty";
        }
        CompoundTag tag = stack.getTag();
        return stack.getItem()
                + " x" + stack.getCount()
                + " tag=" + (tag == null ? "{}" : tag.toString());
    }

    public static String caller() {
        if (!USABLE_DEBUG) {
            return "disabled";
        }
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        StringBuilder builder = new StringBuilder();
        int written = 0;
        for (StackTraceElement element : trace) {
            String className = element.getClassName();
            if (className.equals(Thread.class.getName()) || className.equals(ScpInventoryDebug.class.getName())) {
                continue;
            }
            if (className.startsWith("java.")) {
                continue;
            }
            if (written > 0) {
                builder.append(" <- ");
            }
            builder.append(className.substring(className.lastIndexOf('.') + 1))
                    .append('#')
                    .append(element.getMethodName())
                    .append(':')
                    .append(element.getLineNumber());
            written++;
            if (written >= 8) {
                break;
            }
        }
        return builder.toString();
    }
}
