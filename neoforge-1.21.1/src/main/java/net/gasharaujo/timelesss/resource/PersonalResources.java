package net.gasharaujo.timelesss.resource;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.gasharaujo.timelesss.ResourceConfig;
import net.gasharaujo.timelesss.TimelesssMod;
import net.gasharaujo.timelesss.item.TimelesssItem;
import net.gasharaujo.timelesss.item.TimelesssLocator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PersonalResources {
    private PersonalResources() {}
    public static long xp(Player player) {
        long l=player.experienceLevel;
        long base=l<=16 ? l*l+6*l : l<=31 ? (5*l*l-81*l+720)/2 : (9*l*l-325*l+4440)/2;
        return base + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }
    public static long available(Player player, ItemStack stack) {
        ResourceType type=ResourceConfig.personalResource();
        ResourceStorage s=ResourceStorage.item(stack);
        return switch(type) {
            case NONE -> Long.MAX_VALUE;
            case XP -> xp(player);
            case DURABILITY -> s.capacity(type)-s.amount(type);
            default -> s.amount(type);
        };
    }
    public static boolean canPay(Player player, ItemStack stack) {
        long cost=ResourceConfig.cost(ResourceConfig.personalResource(),TimelesssItem.getMultiplier(stack));
        if(available(player,stack)>=cost && (ResourceConfig.personalResource()!=ResourceType.XP || cost<=Integer.MAX_VALUE)) return true;
        player.sendSystemMessage(Component.translatable("message.timelesss.resource_missing",cost,ResourceConfig.personalResource().name()));
        return false;
    }
    public static void pay(Player player, ItemStack stack) {
        ResourceType type=ResourceConfig.personalResource();
        long cost=ResourceConfig.cost(type,TimelesssItem.getMultiplier(stack));
        ResourceStorage s=ResourceStorage.item(stack);
        if(type==ResourceType.NONE || cost==0) return;
        if(type==ResourceType.XP) player.giveExperiencePoints(-(int)cost);
        else if(type==ResourceType.DURABILITY) {
            s.insert(type,cost,false);
            if(s.amount(type)>=s.capacity(type)) stack.shrink(1);
        } else s.extract(type,cost,false);
        player.getInventory().setChanged();
    }
    public static void recharge(Player player) {
        if(ResourceConfig.personalResource()!=ResourceType.TIME) return;
        Set<ItemStack> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        for(int i=0;i<player.getInventory().getContainerSize();i++) charge(player.getInventory().getItem(i),seen);
        charge(TimelesssLocator.resolve(player,TimelesssLocator.CURIO,0),seen);
    }
    private static void charge(ItemStack stack,Set<ItemStack> seen) {
        if(stack.is(TimelesssMod.TIMELESSS.get()) && seen.add(stack))
            ResourceStorage.item(stack).insert(ResourceType.TIME,ResourceConfig.value(ResourceConfig.TIME_CHARGE),false);
    }
    public static Component status(ItemStack stack) {
        ResourceType type=ResourceConfig.personalResource();
        ResourceStorage s=ResourceStorage.item(stack);
        if(type==ResourceType.NONE) return Component.translatable("tooltip.timelesss.resource_none");
        if(type==ResourceType.XP) return Component.translatable("tooltip.timelesss.resource_xp", ResourceConfig.value(ResourceConfig.XP_COST));
        long value=type==ResourceType.DURABILITY ? s.capacity(type)-s.amount(type) : s.amount(type);
        return Component.translatable("tooltip.timelesss.resource",type.name(),value,
            type==ResourceType.TIME && ResourceConfig.value(ResourceConfig.TIME_LIMIT)==0 ? "∞" : s.capacity(type));
    }
    public static int barWidth(ItemStack stack) {
        ResourceStorage s=ResourceStorage.item(stack);
        long value=s.type()==ResourceType.DURABILITY ? s.capacity(s.type())-s.amount(s.type()) : s.amount(s.type());
        return s.capacity(s.type())<=0 ? 0 : (int)Math.round(13.0 * value / s.capacity(s.type()));
    }
}
