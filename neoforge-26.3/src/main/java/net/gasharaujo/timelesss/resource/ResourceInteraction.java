package net.gasharaujo.timelesss.resource;

import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.ResourceConfig;
import net.gasharaujo.timelesss.TimeNetwork;
import net.gasharaujo.timelesss.block.entity.ResourceBlockEntity;
import net.gasharaujo.timelesss.block.entity.BatteryHubBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ResourceInteraction {
    private ResourceInteraction() {}
    public static void block(Player player, InteractionHand hand, ResourceBlockEntity block) {
        if(!java.util.Objects.equals(block.getOwner(),player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.timelesss.not_owner"));return;
        }
        ItemStack held=player.getItemInHand(hand);
        if(block instanceof BatteryHubBlockEntity hub && held.is(TimelesssMod.BATTERY.get())) {
            if(!hub.insertBattery()) return;
            held.shrink(1);
            if (player instanceof ServerPlayer serverPlayer) {
                TimeNetwork.checkBulkBatteries(serverPlayer);
            }
        } else {
            fill(player, hand, block.resources());
        }
        for(ResourceType type:block.resources().types()) player.sendSystemMessage(Component.translatable("message.timelesss.resource_status",block.resources().amount(type),block.resources().capacity(type),type.name()+" "+ResourceConfig.unit(type)));
    }
    public static boolean fill(Player player, InteractionHand hand, ResourceStorage s) {
        if(s.enabled(ResourceType.FLUID) && !s.validItem(player.getItemInHand(hand))) return net.neoforged.neoforge.transfer.fluid.FluidUtil.interactWithFluidHandler(player, hand, null, new ResourceHandlers(s).fluid(), null);
        ItemStack held=player.getItemInHand(hand);
        if(s.validItem(held)) {
            int moved=(int)s.insert(ResourceType.ITEM,ResourceStorage.itemId(held),held.getCount(),false);
            held.shrink(moved);return moved>0;
        }
        return false;
    }
    public static void fillPersonal(Player player, InteractionHand hand, ItemStack stack) {
        InteractionHand source=hand==InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        fill(player, source, ResourceStorage.item(stack));
    }
}
