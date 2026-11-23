package net.gasharaujo.timelesss.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BatteryItem extends Item {
    public BatteryItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    // Remove obsolete charge data so older batteries can merge with fresh stacks.
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity owner, int slot, boolean selected) {
        CompoundTag tag = stack.getTag();
        if (!level.isClientSide() && tag != null && tag.contains("timelesss_resources")) {
            tag.remove("timelesss_resources");
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
    }
}
