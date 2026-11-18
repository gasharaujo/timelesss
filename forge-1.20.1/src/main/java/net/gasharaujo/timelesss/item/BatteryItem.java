package net.gasharaujo.timelesss.item;

import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

public final class BatteryItem extends Item {
    public BatteryItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(!level.isClientSide()) net.gasharaujo.timelesss.resource.ResourceInteraction.fillPersonal(player,hand,stack);
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack,level.isClientSide());
    }
    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> tooltip, TooltipFlag flag) {
        var s=net.gasharaujo.timelesss.resource.ResourceStorage.item(stack);
        tooltip.add(Component.translatable("tooltip.timelesss.resource",s.type().name(),s.amount(s.type()),s.capacity(s.type())));
    }
    @Override public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
        return new net.gasharaujo.timelesss.resource.ResourceCapabilities(stack);
    }
}
