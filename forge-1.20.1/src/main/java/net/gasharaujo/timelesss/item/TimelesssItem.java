package net.gasharaujo.timelesss.item;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.gasharaujo.timelesss.client.ClientEvents;
import net.gasharaujo.timelesss.client.TimelesssRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class TimelesssItem extends Item {
    public static final String MULTIPLIER_KEY = "timelesss_multiplier";
    public static final int MIN_MULTIPLIER = 1;
    public static int maxMultiplier() { return net.gasharaujo.timelesss.ResourceConfig.personalMax(); }

    public TimelesssItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide()) net.gasharaujo.timelesss.resource.ResourceInteraction.fillPersonal(player, hand, stack);
            return InteractionResultHolder.pass(stack);
        }

        setMultiplier(stack, getMultiplier(stack));
        if (level.isClientSide()) {
            ClientEvents.openMultiplierScreen(TimelesssLocator.inHand(hand, stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
            TimelesssAccelerator.accelerateBlock(
                context.getPlayer(),
                context.getLevel(),
                context.getClickedPos(),
                context.getClickedFace(),
                context.getItemInHand()
            );
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(
        ItemStack stack,
        Player player,
        LivingEntity target,
        InteractionHand hand
    ) {
        if (!player.level().isClientSide()) {
            TimelesssAccelerator.accelerateEntity(player, target, stack);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        setMultiplier(stack, MIN_MULTIPLIER);
        return stack;
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        @Nullable Level level,
        List<Component> tooltip,
        TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("tooltip.timelesss.multiplier", getMultiplier(stack)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.timelesss.open").withStyle(ChatFormatting.GRAY));
        tooltip.add(net.gasharaujo.timelesss.resource.PersonalResources.status(stack).copy().withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final TimelesssRenderer renderer = new TimelesssRenderer();

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }


    @Override public boolean isBarVisible(ItemStack stack) {
        var type=net.gasharaujo.timelesss.ResourceConfig.personalResource();
        return type!=net.gasharaujo.timelesss.resource.ResourceType.NONE && type!=net.gasharaujo.timelesss.resource.ResourceType.XP;
    }
    @Override public int getBarWidth(ItemStack stack) { return net.gasharaujo.timelesss.resource.PersonalResources.barWidth(stack); }
    @Override public int getBarColor(ItemStack stack) { return 0x55DDCC; }
    @Override public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
        return new net.gasharaujo.timelesss.resource.ResourceCapabilities(stack);
    }

    public static int getMultiplier(ItemStack stack) {
        int value = stack.getOrCreateTag().getInt(MULTIPLIER_KEY);
        if (value == 0) {
            value = MIN_MULTIPLIER;
        }
        return Mth.clamp(value, MIN_MULTIPLIER, maxMultiplier());
    }

    public static void setMultiplier(ItemStack stack, int multiplier) {
        stack.getOrCreateTag().putInt(
            MULTIPLIER_KEY,
            Mth.clamp(multiplier, MIN_MULTIPLIER, maxMultiplier())
        );
    }
}
