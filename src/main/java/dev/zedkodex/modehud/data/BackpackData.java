package dev.zedkodex.modehud.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class BackpackData implements INBTSerializable<CompoundTag> {
    public ItemStack stack = ItemStack.EMPTY;

    public boolean worn() {
        return !stack.isEmpty();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        if (!stack.isEmpty()) t.put("stack", stack.save(provider));
        return t;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag t) {
        stack = t.contains("stack") ? ItemStack.parse(provider, t.get("stack")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
    }
}
