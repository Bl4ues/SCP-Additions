package net.mcreator.scpadditions.facility;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Persistent entry and whole-sign clipboard stored on the Screwdriver. */
public final class FacilitySignClipboard {
    private static final String ENTRY_TAG = "ScpAdditionsFacilitySignEntry";
    private static final String SIGN_TAG = "ScpAdditionsFacilitySign";
    private static final String TYPE_KEY = "Type";
    private static final String ENTRIES_KEY = "Entries";
    private static final String NUMBER_KEY = "Number";
    private static final String TEXT_KEY = "Text";

    private FacilitySignClipboard() {
    }

    public static void copyEntry(ItemStack screwdriver,
            FacilitySignBlock.SignType type, FacilitySignData.Entry entry) {
        if (screwdriver == null || screwdriver.isEmpty()) return;
        FacilitySignData.Entry clean = FacilitySignData.sanitize(type, entry);
        CompoundTag tag = writeEntry(clean);
        tag.putString(TYPE_KEY, type.serializedName());
        screwdriver.getOrCreateTag().put(ENTRY_TAG, tag);
    }

    public static void copySign(ItemStack screwdriver,
            FacilitySignBlock.SignType type, List<FacilitySignData.Entry> entries) {
        if (screwdriver == null || screwdriver.isEmpty()) return;
        CompoundTag sign = new CompoundTag();
        sign.putString(TYPE_KEY, type.serializedName());
        ListTag list = new ListTag();
        for (FacilitySignData.Entry entry : FacilitySignData.normalize(type, entries)) {
            list.add(writeEntry(entry));
        }
        sign.put(ENTRIES_KEY, list);
        screwdriver.getOrCreateTag().put(SIGN_TAG, sign);
    }

    public static EntryClipboard readEntry(ItemStack screwdriver) {
        if (screwdriver == null || !screwdriver.hasTag()
                || !screwdriver.getTag().contains(ENTRY_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag tag = screwdriver.getTag().getCompound(ENTRY_TAG);
        FacilitySignBlock.SignType type = FacilitySignBlock.SignType.byName(
                tag.getString(TYPE_KEY));
        return new EntryClipboard(type, FacilitySignData.sanitize(type, readEntryTag(tag)));
    }

    public static SignClipboard readSign(ItemStack screwdriver) {
        if (screwdriver == null || !screwdriver.hasTag()
                || !screwdriver.getTag().contains(SIGN_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag tag = screwdriver.getTag().getCompound(SIGN_TAG);
        FacilitySignBlock.SignType type = FacilitySignBlock.SignType.byName(
                tag.getString(TYPE_KEY));
        ListTag list = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        List<FacilitySignData.Entry> entries = new ArrayList<>();
        for (int i = 0; i < Math.min(FacilitySignData.ENTRY_COUNT, list.size()); i++) {
            entries.add(readEntryTag(list.getCompound(i)));
        }
        return new SignClipboard(type, FacilitySignData.normalize(type, entries));
    }

    private static CompoundTag writeEntry(FacilitySignData.Entry entry) {
        CompoundTag tag = new CompoundTag();
        tag.putString(NUMBER_KEY, entry.number());
        tag.putString(TEXT_KEY, entry.text());
        return tag;
    }

    private static FacilitySignData.Entry readEntryTag(CompoundTag tag) {
        return new FacilitySignData.Entry(tag.getString(NUMBER_KEY), tag.getString(TEXT_KEY));
    }

    public record EntryClipboard(FacilitySignBlock.SignType type,
            FacilitySignData.Entry entry) {
    }

    public record SignClipboard(FacilitySignBlock.SignType type,
            List<FacilitySignData.Entry> entries) {
        public SignClipboard {
            entries = List.copyOf(entries);
        }
    }
}
