package net.gasharaujo.timelesss.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

public final class TimeWrenchItem extends Item {
    private static final String DIMENSION = "TimeDimension";
    private static final String POSITION = "TimePosition";

    public TimeWrenchItem(Properties properties) { super(properties); }

    public static void setTarget(ItemStack stack, String dimension, long position) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(DIMENSION, dimension);
            tag.putLong(POSITION, position);
        });
    }

    private static Target target(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return new Target(tag.contains(DIMENSION) && tag.contains(POSITION),
            tag.getStringOr(DIMENSION, ""), tag.getLongOr(POSITION, 0L));
    }

    public static boolean hasTarget(ItemStack stack) { return target(stack).present; }
    public static String getDimension(ItemStack stack) { return target(stack).dimension; }
    public static long getPosition(ItemStack stack) { return target(stack).position; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        if (hasTarget(stack)) {
            var pos = net.minecraft.core.BlockPos.of(getPosition(stack));
            tooltip.accept(Component.translatable("tooltip.timelesss.wrench_target", getDimension(stack),
                pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.accept(Component.translatable("tooltip.timelesss.wrench_empty").withStyle(ChatFormatting.GRAY));
        }
    }

    private record Target(boolean present, String dimension, long position) {}
}
