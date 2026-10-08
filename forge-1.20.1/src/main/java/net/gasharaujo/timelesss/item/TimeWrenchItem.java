package net.gasharaujo.timelesss.item;

import java.util.List;
import java.util.function.Consumer;
import net.gasharaujo.timelesss.client.TimeWrenchRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class TimeWrenchItem extends Item {
    private static final String DIMENSION = "TimeDimension";
    private static final String POSITION = "TimePosition";

    public TimeWrenchItem(Properties properties) {
        super(properties);
    }

    public static void setTarget(ItemStack stack, String dimension, long position) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(DIMENSION, dimension);
        tag.putLong(POSITION, position);
    }

    public static boolean hasTarget(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(DIMENSION) && tag.contains(POSITION);
    }

    public static String getDimension(ItemStack stack) {
        return hasTarget(stack) ? stack.getTag().getString(DIMENSION) : "";
    }

    public static long getPosition(ItemStack stack) {
        return hasTarget(stack) ? stack.getTag().getLong(POSITION) : 0L;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasTarget(stack)) {
            var pos = net.minecraft.core.BlockPos.of(getPosition(stack));
            tooltip.add(Component.translatable(
                "tooltip.timelesss.wrench_target", getDimension(stack), pos.getX(), pos.getY(), pos.getZ()
            ).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.timelesss.wrench_empty").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final TimeWrenchRenderer renderer = new TimeWrenchRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }
}
