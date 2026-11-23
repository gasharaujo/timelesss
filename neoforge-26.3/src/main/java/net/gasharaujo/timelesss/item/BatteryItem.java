package net.gasharaujo.timelesss.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

public final class BatteryItem extends Item {
    public BatteryItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    // Remove obsolete charge data so older batteries can merge with fresh stacks.
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains("timelesss_resources")) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove("timelesss_resources"));
        }
    }
}
