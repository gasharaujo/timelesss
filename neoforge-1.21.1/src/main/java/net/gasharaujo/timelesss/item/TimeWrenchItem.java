package net.gasharaujo.timelesss.item;

import java.util.List;
import java.util.function.Consumer;
import net.gasharaujo.timelesss.client.TimeWrenchRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class TimeWrenchItem extends Item {
    private static final String DIMENSION = "TimeDimension";
    private static final String POSITION = "TimePosition";

    public TimeWrenchItem(Properties properties) {
        super(properties);
    }

    public static void setTarget(ItemStack stack, String dimension, long position) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(DIMENSION, dimension);
            tag.putLong(POSITION, position);
        });
    }

    private static CompoundTarget target(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return new CompoundTarget(tag.contains(DIMENSION) && tag.contains(POSITION),
            tag.getString(DIMENSION), tag.getLong(POSITION));
    }

    public static boolean hasTarget(ItemStack stack) { return target(stack).present; }
    public static String getDimension(ItemStack stack) { return target(stack).dimension; }
    public static long getPosition(ItemStack stack) { return target(stack).position; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (hasTarget(stack)) {
            var pos = net.minecraft.core.BlockPos.of(getPosition(stack));
            tooltip.add(Component.translatable("tooltip.timelesss.wrench_target", getDimension(stack),
                pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.timelesss.wrench_empty").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final TimeWrenchRenderer renderer = new TimeWrenchRenderer();
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
        });
    }

    private record CompoundTarget(boolean present, String dimension, long position) {}
}
